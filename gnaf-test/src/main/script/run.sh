#! /bin/bash

version=`sed 's/.*"\(.*\)"/\1/' ../version.sbt`
scalaVersion=2.11

search="src/main/script/searchLucene.js" # "src/main/script/searchEs.js"
url="http://localhost:9040/bulkSearch"   # "http://localhost:9200/gnaf/_msearch"
skip="false"

while getopts "u:sh" opt
do
  case $opt in
    u) url=$OPTARG ;;
    s) skip="true" ;;
    h|"?") cat <<EOF
usage: $0 -u url -s -h
  -u url to set the search service endpoint (default $url)
  -s to skip generation of test address data (it must already exist in the current directory)
  -h for this help
EOF
	exit 1 ;;
  esac
done

concat() {
  if [[ -n "$2" ]]; then echo "$1-$2"; else echo "$1"; fi
}

for opt1 in "" "--localityAlias"
do
  filePrefix=`concat "address" "${opt1#--}"` 
  for opt2 in "" "--numberAdornments" "--unit" "--level" "--streetAlias"
  do
    file=`concat "$filePrefix" "${opt2#--}"`.json
    echo "options: $opt1 $opt2 ..."
    # sample size turns out smaller than requested because some locations have no addresses
    # and others have no addresses with the requested characteristics (haven't figured out how to filter them out cheaply)
    if [[ "$skip" != "true" ]]
    then
      time java -jar target/scala-${scalaVersion}/gnaf-test_${scalaVersion}-${version}-one-jar.jar --sampleSize 200 $opt1 $opt2 > $file
      wait # for previous node process
      node $search $url $file > stats${file#address} &
    else
      # re-run with same test data as before
      node $search $url $file > stats${file#address}
    fi
  done
done
wait # for last node process

node src/main/script/summary.js stats*.json
