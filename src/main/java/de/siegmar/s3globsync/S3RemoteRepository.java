package de.siegmar.s3globsync;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Md5Utils;

public class S3RemoteRepository implements RemoteRepository {

    private static final Logger LOG = LoggerFactory.getLogger(S3RemoteRepository.class);

    private final String bucket;
    private final String prefix;
    private final boolean compareCachePolicy;
    private final boolean dryrun;
    private final S3Client s3;

    public S3RemoteRepository(final String bucket, final String prefix, final boolean compareCachePolicy, final boolean dryrun) {
        this.bucket = bucket;
        this.prefix = prefix;
        this.compareCachePolicy = compareCachePolicy;
        this.dryrun = dryrun;
        s3 = S3Client.builder().build();
    }

    @Override
    public List<RemoteFile> list() {
        final List<RemoteFile> ret = new ArrayList<>();

        final ListObjectsV2Iterable response = s3.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build());
        for (final ListObjectsV2Response listObjectsV2Response : response) {
            for (final S3Object content : listObjectsV2Response.contents()) {
                final String baseName = content.key().substring(prefix.length());
                ret.add(new RemoteFile(content.key(), baseName, content.size(), content.eTag()));
            }
        }

        return ret;
    }

    @Override
    public void create(final Path path, final String name, final String cacheControl) {
        if (!dryrun) {
            LOG.info("Create {} (from {})", prefix + name, path);
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(prefix + name).cacheControl(cacheControl).build(), path);
        } else {
            LOG.info("DRY-RUN -- Create {} (from {})", prefix + name, path);
        }
    }

    @Override
    public boolean update(final RemoteFile remoteFile, final Path path, final String name, final String cacheControl) {
        try {
            if (fileChanged(remoteFile, path, cacheControl)) {
                if (!dryrun) {
                    LOG.info("Update {} (from {})", prefix + name, path);
                    final PutObjectRequest req = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(prefix + name)
                        .cacheControl(cacheControl)
                        .build();

                    s3.putObject(req, path);
                } else {
                    LOG.info("DRY-RUN -- Update {} (from {})", prefix + name, path);
                }

                return true;
            } else {
                LOG.debug("Skip Update {} (file unchanged)", prefix + name);
                return false;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean fileChanged(final RemoteFile remoteFile, final Path path, final String cacheControl) throws IOException {
        final long fileSize = Files.size(path);
        if (fileSize != remoteFile.getSize()) {
            LOG.debug("File size of {} changed (old: {} / new: {})",
                remoteFile.getName(), remoteFile.getSize(), fileSize);
            return true;
        }

        final String md5Hash = '"' + BinaryUtils.toHex(Md5Utils.computeMD5Hash(path.toFile())) + '"';
        final boolean contentHashChanged = !remoteFile.getContentHash().equals(md5Hash);
        if (contentHashChanged) {
            LOG.debug("File content of {} changed (old: {} / new: {})",
                remoteFile.getName(), remoteFile.getContentHash(), md5Hash);
            return true;
        }

        if (compareCachePolicy) {
            LOG.debug("Check cache policy change for {}", remoteFile.getName());
            final HeadObjectResponse headObjectResponse = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(remoteFile.getName()).build());
            if (!Objects.equals(headObjectResponse.cacheControl(), cacheControl)) {
                LOG.debug("Cache-Control header for {} changed (old: {} / new: {})",
                    remoteFile.getName(), headObjectResponse.cacheControl(), cacheControl);
                return true;
            }
        }

        return false;
    }

    @Override
    public void delete(final RemoteFile remoteFile) {
        if (!dryrun) {
            LOG.info("Delete {}", remoteFile);
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(remoteFile.getName()).build());
        } else {
            LOG.info("DRY-RUN -- Delete {}", remoteFile);
        }
    }

}
