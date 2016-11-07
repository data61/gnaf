#! /bin/bash

# Note: to count total searches performed:
# for i in address*.json; do jq '.|length' $i; done | awk '{ sum += $1 } END { print sum*6 }'

# Note: searchLucene.js performs 6 searches for each address:
# { fuz, noFuz } * { query, queryTypo, queryPostcodeBeforeState }

version=`sed 's/.*"\(.*\)"/\1/' ../version.sbt`
scalaVersion=2.11

search="src/main/script/searchLucene.js" # "src/main/script/searchEs.js"
url="http://localhost:9040/bulkSearch"   # "http://localhost:9200/gnaf/_msearch"
skip="false"
sampleSize=200
addrDir=.
statsDir=.

while getopts "u:n:sh" opt
do
  case $opt in
    a) addrDir=$OPTARG ;;
    o) statsDir=$OPTARG ;;
    u) url=$OPTARG ;;
    n) sampleSize=$OPTARG ;;
    s) skip="true" ;;
    h|"?") cat <<EOF
usage: $0 -u url -s -h
  -u url to set the search service endpoint, blank to skip search (default $url)
  -n sampleSize passed to gnaf-test (default $sampleSize)
  -s to skip generation of test address data (it must already exist in the current directory)
  -a directory for address JSON files (default $addrDir)
  -o directory for stats JSON files (default $statsDir)
  -h for this help
EOF
	exit 1 ;;
  esac
done

for opt in "" "--numberAdornments" "--flat" "--flat --noFlatType" "--level" "--streetAlias" "--localityAlias"
do
    echo "options: $opt ..."
    name=${opt//--/}
    name=${name// /-}
    [[ -n "$name" ]] && name=-${name}
    afile=$addrDir/address${name}.json
    sfile=$statsDir/stats${name}.json
    # sample size turns out smaller than requested because some locations have no addresses
    # and others have no addresses with the requested characteristics (haven't figured out how to filter them out cheaply)
    if [[ "$skip" != "true" ]]
    then
      time java -jar target/scala-${scalaVersion}/gnaf-test_${scalaVersion}-${version}-one-jar.jar --sampleSize $sampleSize $opt > $afile
      wait # for previous node process
      [[ -n "$url" ]] && node $search $url $afile > $sfile &
    else
      # re-run with same test data as before
      node $search $url $afile > $sfile
    fi
done
wait # for last node process

node src/main/script/summary.js $statsDir/stats*.json
