package de.siegmar.s3globsync;

import java.nio.file.Path;
import java.util.List;

public interface RemoteRepository {

    List<RemoteFile> list();

    void create(Path path, String name, final FileMetadata fileMetadata);

    boolean update(final RemoteFile remoteFile, Path path, String name, final FileMetadata fileMetadata);

    void delete(RemoteFile remoteFile);

}
