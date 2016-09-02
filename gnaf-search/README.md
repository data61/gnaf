# gnaf-search

## Introduction

This project provides a JSON web service to search the [Lucene](https://lucene.apache.org/) index created by `gnaf-indexer`.

## Configuration

Configuration is in [application.conf](src/main/resources/application.conf) and most settings can be overridden with environment variables.
Command line options take precedence over the above, use `--help` for details.

## Running and Usage

See `gnaf/src/main/script/run.sh`.
