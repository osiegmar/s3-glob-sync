package de.siegmar.s3globsync;

import java.nio.file.Path;
import java.util.List;

public class DummyRemoteRepository implements RemoteRepository {

    private final String prefix;

    public DummyRemoteRepository(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public List<RemoteFile> list() {
        return List.of(
            new RemoteFile(prefix  + "robots.txt", "robots.txt", 3213, ""),
            new RemoteFile(prefix + "foo.txt", "robots.txt", 123, "")
        );
    }

    @Override
    public void create(final Path path, final String name, final FileMetadata fileMetadata) {
        System.out.println("Create " + buildRemotePath(name) + " (from " + path + ")");
    }

    private String buildRemotePath(final String name) {
        return prefix != null ? prefix + name : name;
    }

    @Override
    public boolean update(final RemoteFile remoteFile, final Path path, final String name, final FileMetadata fileMetadata) {
        System.out.println("Update " + buildRemotePath(name) + " (from " + path + ")");
        return false;
    }

    @Override
    public void delete(final RemoteFile remoteFile) {
        System.out.println("Delete " + remoteFile);
    }

}
