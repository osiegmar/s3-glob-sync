package de.siegmar.s3globsync;

import java.nio.file.Path;

public class UpdateFile {

    private final RemoteFile remoteFile;
    private final Path localFile;

    public UpdateFile(final RemoteFile remoteFile, final Path localFile) {
        this.remoteFile = remoteFile;
        this.localFile = localFile;
    }

    public RemoteFile getRemoteFile() {
        return remoteFile;
    }

    public Path getLocalFile() {
        return localFile;
    }

}
