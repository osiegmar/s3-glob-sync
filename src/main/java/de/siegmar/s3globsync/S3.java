package de.siegmar.s3globsync;

import static picocli.CommandLine.ArgGroup;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

@Command(name = "s3-glob-sync", usageHelpWidth = 120, usageHelpAutoWidth = true,
         version = "0.1")
public class S3 implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(S3.class);

    @Option(names = "--bucket", required = true, description = "the S3 bucket")
    String bucket;

    @Option(names = "--prefix", description = "the S3 prefix (e.g. 'preview/') to use")
    String prefix = "";

    @Option(names = "--default-cache-policy", defaultValue = "no-store", description = "the default cache policy to use")
    String defaultCachePolicy;

    @Option(names = "--default-acl", defaultValue = "public-read", description = "the default ACL to use")
    String defaultAcl;

    @Option(names = "--exclude", description = "files (glob) to be excluded")
    List<String> excludes = new ArrayList<>();

    @Option(names = "--compare-cache-policy", description = "if changes in the cache-policy should be checked and corrected")
    boolean compareCachePolicy;

    @Option(names = "--dry-run", description = "if a dry-run should be performed (no real manipulations)")
    boolean dryRun;

    @Option(names = "--delete-orphaned", defaultValue = "true", description = "delete files on S3 which are not present locally")
    boolean deleteOrphaned;

    @Option(names = "--wait-before-delete", defaultValue = "5000", description = "Milliseconds to wait before deleting files")
    int waitBeforeDelete;

    @ArgGroup(exclusive = false, multiplicity = "0..*")
    List<GlobGroup> globGroups = new ArrayList<>();

    @Parameters(description = "Local directory to sync with S3")
    Path path;

    @Option(names = "--version", versionHelp = true, description = "print version information and exit")
    boolean versionRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Override
    public Integer call() throws Exception {
        //final RemoteRepository repository = new DummyRemoteRepository(prefix);
        final RemoteRepository repository = new S3RemoteRepository(bucket, prefix, compareCachePolicy, dryRun);

        final List<Path> localFilesToSync = scanLocal();
        final List<RemoteFile> remoteFiles = repository.list();
        final List<RemoteFile> deleteFiles = new ArrayList<>(remoteFiles);

        List<Path> createFiles = new ArrayList<>();
        List<UpdateFile> updateFiles = new ArrayList<>();

        for (final Path localFile : localFilesToSync) {
            final String relativeFilename = path.relativize(localFile).toString();

            final Optional<RemoteFile> first = remoteFiles.stream()
                .filter(p -> p.getBaseName().equals(relativeFilename))
                .findFirst();

            if (first.isPresent()) {
                final RemoteFile remoteFile = first.get();
                updateFiles.add(new UpdateFile(remoteFile, localFile));
                deleteFiles.remove(remoteFile);
            } else {
                createFiles.add(localFile);
            }
        }

        // Determine all CREATEs (local files missing remote)
        for (final Path createFile : createFiles) {
            final FileMetadata fileMetadata = findMetadata(createFile);
            repository.create(createFile, path.relativize(createFile).toString(), fileMetadata);
        }

        // Determine all UPDATEs (local files with different remote state)
        boolean fileUpdates = false;
        for (final UpdateFile updateFile : updateFiles) {
            final FileMetadata fileMetadata = findMetadata(updateFile.getLocalFile());
            fileUpdates |= repository.update(updateFile.getRemoteFile(), updateFile.getLocalFile(), path.relativize(updateFile.getLocalFile()).toString(), fileMetadata);
        }

        // Determine all DELETEs (remote files with missing local)
        if (deleteOrphaned) {
            if (deleteFiles.isEmpty()) {
                LOG.debug("Nothing to delete");
            } else {
                // Wait a moment to not disturb current deliveries
                if (!dryRun && fileUpdates) {
                    LOG.info("Wait {} milliseconds before deleting files (prevent failed access to stale references)", waitBeforeDelete);
                    Thread.sleep(waitBeforeDelete);
                }

                for (final RemoteFile remoteFile : deleteFiles) {
                    repository.delete(remoteFile);
                }
            }
        }

        return 0;
    }

    private List<Path> scanLocal() throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk
                .filter(Files::isRegularFile)
                .filter(f -> !isExcluded(path.relativize(f)))
                .collect(Collectors.toList());
        }
    }

    private boolean isExcluded(final Path f) {
        for (String exclude : excludes) {
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + exclude);
            if (pathMatcher.matches(f)) {
                LOG.debug("Ignore {} (excluded by glob {})", f, exclude);
                return true;
            }
        }
        return false;
    }

    private FileMetadata findMetadata(final Path createFile) {
        for (final GlobGroup globGroup : globGroups) {
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globGroup.glob);
            if (pathMatcher.matches(path.relativize(createFile))) {
                return new FileMetadata(
                    Optional.ofNullable(globGroup.globControl.cachePolicy).orElse(defaultCachePolicy),
                    Optional.ofNullable(globGroup.globControl.acl).orElse(defaultAcl));
            }
        }

        return new FileMetadata(defaultCachePolicy, defaultAcl);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new S3()).execute(args));
    }

    static class GlobGroup {

        @Option(names = "--glob", required = true, description = "file (glob) specific settings")
        String glob;

        @ArgGroup(exclusive = false, multiplicity = "1", heading = "Arguments of --glob:%n")
        GlobControl globControl;

    }

    static class GlobControl {

        @Option(names = "--cache-policy", description = "cache policy specific for this file glob (e.g. 'public, max-age=86400')")
        String cachePolicy;

        @Option(names = "--acl", description = "acl for this file glob (e.g. 'public-read')")
        String acl;

    }

}
