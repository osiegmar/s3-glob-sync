# S3 Glob sync

> :warning: **This project is in very early stage of development!**

GitHub action to sync static files to an S3 bucket while maintaining file specific metadata.

## Example usage

```
name: Upload Website

on:
  push:
    branches:
    - master

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - uses: osiegmar/s3-glob-sync@v0
      with:
        args: --bucket=my_bucket dist
      env:
        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
```

## Advanced example usage

```
name: Upload Website

on:
  push:
    branches:
    - master

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - uses: osiegmar/s3-glob-sync@v0
      with:
        args: >
          --bucket=my_bucket
          --compare-cache-policy
          --delete-orphaned
          --exclude="**/.gitkeep"
          --default-acl="public-read"
          --default-cache-policy="no-store"
          --prefix="preview/"
          --glob="static/**" --cache-policy="public, max-age=604800, immutable"
          --glob="robots.txt" --cache-policy="public, max-age=86400, must-revalidate"
          --glob="**/*.html" --cache-policy="public, no-cache"
          dist
      env:
        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        AWS_REGION: 'us-west-1' # optional: defaults to us-east-1
```

## Overview of CLI arguments

```
Usage: s3-glob-sync [-h] [--compare-cache-policy] [--delete-orphaned]
                    [--dryrun] --bucket=<bucket> [--default-acl=<defaultAcl>]
                    [--default-cache-policy=<defaultCachePolicy>]
                    [--prefix=<prefix>]
                    [--wait-before-delete=<waitBeforeDelete>]
                    [--exclude=<excludes>]... [--glob=<glob>
                    --cache-policy=<cachePolicy> --acl=<acl>]... <path>
      <path>                 Local directory to sync with S3
      --acl=<acl>            acl for this file glob
      --bucket=<bucket>      the S3 bucket
      --cache-policy=<cachePolicy>
                             cache policy specific for this file glob
      --compare-cache-policy if changes in the cache-policy should be checked
                               and corrected
      --default-acl=<defaultAcl>
                             the default ACL to use
      --default-cache-policy=<defaultCachePolicy>
                             the default cache policy to use
      --delete-orphaned      delete files on S3 which are not present locally
      --dryrun               if a dry run should be performed (no real
                               manipulations)
      --exclude=<excludes>   files (glob) to be excluded
      --glob=<glob>          file (glob) specific settings
  -h, --help                 display this help message
      --prefix=<prefix>      the S3 prefix (e.g. 'preview/') to use
      --wait-before-delete=<waitBeforeDelete>
                             Milliseconds to wait before deleting files
```
