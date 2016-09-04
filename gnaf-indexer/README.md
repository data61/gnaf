# gnaf-indexer

## Introduction

This project loads JSON address data from `gnaf-extractor` into a [Lucene](https://lucene.apache.org/) index.
Originally Elasticsearch was used, but it was found that significant tweaks to scoring were required for good results
and this was easiest achieved in raw Lucene (which also provided significant speed improvements).

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overridden with environment variables.
The index directory can also be set with a command line option (overriding the above, use `--help` for details).

## Running and Usage

See `gnaf/src/main/script/run.sh`.
