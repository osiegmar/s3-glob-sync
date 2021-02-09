package de.siegmar.s3globsync;

import java.nio.file.Path;
import java.util.List;

public interface RemoteRepository {

    List<RemoteFile> list();

    void create(Path path, String name, final String acl, final String cacheControl);

    boolean update(final RemoteFile remoteFile, Path path, String name, final String acl, final String cacheControl);

    void delete(RemoteFile remoteFile);

}
