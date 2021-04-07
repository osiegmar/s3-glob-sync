# S3 Glob Sync

> :warning: **This project is in very early stage of development!**

Utility to publish files to a S3 bucket while maintaining file specific metadata.


## Distribution

* GitHub: https://github.com/osiegmar/s3-glob-sync
* Docker Hub: https://hub.docker.com/r/osiegmar/s3-glob-sync


## Overview of CLI arguments

```
Usage: s3-glob-sync [-h] [--compare-cache-policy] [--delete-orphaned] [--dry-run] [--version]
                    --bucket=<bucket> [--default-acl=<defaultAcl>]
                    [--default-cache-policy=<defaultCachePolicy>] [--prefix=<prefix>]
                    [--wait-before-delete=<waitBeforeDelete>] [--exclude=<excludes>]...
                    [--glob=<glob> ([--cache-policy=<cachePolicy>] [--acl=<acl>])]... <path>
      <path>                 Local directory to sync with S3
      --bucket=<bucket>      the S3 bucket
      --compare-cache-policy if changes in the cache-policy should be checked and corrected
      --default-acl=<defaultAcl>
                             the default ACL to use
      --default-cache-policy=<defaultCachePolicy>
                             the default cache policy to use
      --delete-orphaned      delete files on S3 which are not present locally
      --dry-run              if a dry-run should be performed (no real manipulations)
      --exclude=<excludes>   files (glob) to be excluded
      --glob=<glob>          file (glob) specific settings
  -h, --help                 display this help message
      --prefix=<prefix>      the S3 prefix (e.g. 'preview/') to use
      --version              print version information and exit
      --wait-before-delete=<waitBeforeDelete>
                             Milliseconds to wait before deleting files
Arguments of --glob:
      --acl=<acl>            acl for this file glob (e.g. 'public-read')
      --cache-policy=<cachePolicy>
                             cache policy specific for this file glob (e.g. 'public,
                               max-age=86400')
```
