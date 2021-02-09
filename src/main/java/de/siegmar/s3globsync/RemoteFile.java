package de.siegmar.s3globsync;

import java.util.StringJoiner;

public class RemoteFile {

    private final String name;
    private final String baseName;
    private final long size;
    private final String contentHash;

    public RemoteFile(final String name, final String baseName, final long size, final String contentHash) {
        this.name = name;
        this.baseName = baseName;
        this.size = size;
        this.contentHash = contentHash;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public long getSize() {
        return size;
    }

    public String getContentHash() {
        return contentHash;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RemoteFile.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("contentHash='" + contentHash + "'")
            .toString();
    }

}
