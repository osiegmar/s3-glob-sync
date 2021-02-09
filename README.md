# s3-glob-sync

**This project is in very early stage of development!**

Example use:

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
    - uses: actions/checkout@master
    - uses: osiegmar/s3-glob-sync@master
      with:
        args: dist
      env:
        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        AWS_REGION: 'us-west-1' # optional: defaults to us-east-1
```


Usage:
```
Usage: s3-glob-sync [-h] [--compare-cache-policy] [--delete-orphaned]
                    [--dryrun] --bucket=<bucket>
                    [--default-cache-policy=<defaultCachePolicy>]
                    [--prefix=<prefix>] [--region=<region>]
                    [--wait-before-delete=<waitBeforeDelete>]
                    [--exclude=<excludes>]... [--glob=<glob>
                    --cache-policy=<cachePolicy>]... <path>
      <path>                 Local directory to sync with S3
      --bucket=<bucket>      the S3 bucket
      --cache-policy=<cachePolicy>
                             cache policy specific for this file glob
      --compare-cache-policy if changes in the cache-policy should be checked
                               and corrected
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
