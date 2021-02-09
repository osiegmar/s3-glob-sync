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

@Command(name = "s3-glob-sync")
public class S3 implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(S3.class);
    private static final String ACL = "public-read";

    @Option(names = "--bucket", required = true, description = "the S3 bucket")
    String bucket;

    @Option(names = "--prefix", description = "the S3 prefix (e.g. 'preview/') to use")
    String prefix = "";

    @Option(names = "--default-cache-policy", description = "the default cache policy to use")
    String defaultCachePolicy = "no-store";

    @Option(names = "--exclude", description = "files (glob) to be excluded")
    List<String> excludes = new ArrayList<>();

    @Option(names = "--compare-cache-policy", description = "if changes in the cache-policy should be checked and corrected")
    boolean compareCachePolicy;

    @Option(names = "--dryrun", description = "if a dry run should be performed (no real manipulations)")
    boolean dryrun;

    @Option(names = "--delete-orphaned", description = "delete files on S3 which are not present locally")
    boolean deleteOrphaned = true;

    @Option(names = "--wait-before-delete", description = "Milliseconds to wait before deleting files")
    int waitBeforeDelete = 5_000;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @ArgGroup(exclusive = false, multiplicity = "0..*")
    List<GlobGroup> globGroups = new ArrayList<>();

    @Parameters(description = "Local directory to sync with S3")
    Path path;

    @Override
    public Integer call() throws Exception {
        //final RemoteRepository repository = new DummyRemoteRepository(prefix);
        final RemoteRepository repository = new S3RemoteRepository(bucket, prefix, compareCachePolicy, dryrun);

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
            repository.create(createFile, path.relativize(createFile).toString(), ACL, findCacheControl(createFile));
        }

        // Determine all UPDATEs (local files with different remote state)
        boolean fileUpdates = false;
        for (final UpdateFile updateFile : updateFiles) {
            fileUpdates |= repository.update(updateFile.getRemoteFile(), updateFile.getLocalFile(), path.relativize(updateFile.getLocalFile()).toString(), ACL, findCacheControl(updateFile.getLocalFile()));
        }

        // Determine all DELETEs (remote files with missing local)
        if (deleteOrphaned) {
            if (deleteFiles.isEmpty()) {
                LOG.debug("Nothing to delete");
            } else {
                // Wait a moment to not disturb current deliveries
                if (!dryrun && fileUpdates) {
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

    private String findCacheControl(final Path createFile) {
        for (final GlobGroup globGroup : globGroups) {
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globGroup.glob);
            if (pathMatcher.matches(path.relativize(createFile))) {
                return globGroup.cachePolicy;
            }
        }

        return defaultCachePolicy;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new S3()).execute(args));
    }

    static class GlobGroup {

        @Option(names = "--glob", required = true, description = "file (glob) specific settings")
        String glob;

        @Option(names = "--cache-policy", required = true, description = "cache policy specific for this file glob")
        String cachePolicy;

    }

}
