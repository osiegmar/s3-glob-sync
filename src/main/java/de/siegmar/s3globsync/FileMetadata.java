package de.siegmar.s3globsync;

public class FileMetadata {

    private final String cachePolicy;
    private final String acl;

    public FileMetadata(final String cachePolicy, final String acl) {
        this.cachePolicy = cachePolicy;
        this.acl = acl;
    }

    public String getCachePolicy() {
        return cachePolicy;
    }

    public String getAcl() {
        return acl;
    }

}
