#! /bin/bash

# This script simply checks the current data and determines whether there are updates that need to be applied to
# our production environment.

jsonUrl=http://www.data.gov.au/api/3/action/package_show?id=19432f89-dc3a-4ef3-b943-5326ef1dbecc
prodUrl=http://gnaf.nationalmap.nicta.com.au/v2/version

last_modified=$( curl -s $jsonUrl | jq -r '.result.resources[] | select(.format == "ZIP") | .last_modified' )

existing_last_modified=$(curl -s $prodUrl | jq -r '.["gnaf-version"]' || echo None_Found)

echo "Last modified date in production: $existing_last_modified";
echo "Last modified date from data.gov.au: $last_modified";

if [[ "$last_modified" != "$existing_last_modified" ]]; then
    echo "New data found!";
    exit 0
else
    echo "No new data found, exiting with exit code 1";
    exit 1;
fi
