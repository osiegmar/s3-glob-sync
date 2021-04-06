module de.siegmar.s3globsync {
    requires info.picocli;
    requires software.amazon.awssdk.services.s3;
    requires software.amazon.awssdk.utils;
    requires org.slf4j;

    opens de.siegmar.s3globsync to info.picocli;
}
