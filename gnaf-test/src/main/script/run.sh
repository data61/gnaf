#! /bin/bash

version=`sed 's/.*"\(.*\)"/\1/' ../version.sbt`
scalaVersion=2.11

concat() {
  if [[ -n "$2" ]]; then echo "$1-$2"; else echo "$1"; fi
}

# search="src/main/script/searchEs.js"
search="src/main/script/searchLucene.js"

time for opt1 in "" "--localityAlias"
do
  filePrefix=`concat "address" "${opt1#--}"` 
  for opt2 in "" "--numberAdornments" "--unit" "--level" "--streetAlias"
  do
    file=`concat "$filePrefix" "${opt2#--}"`.json
    echo "options: $opt1 $opt2 ..."
    # sample size turns out smaller than requested because some locations have no addresses
    # and others have no addresses with the requested characteristics (haven't figured out how to filter them out cheaply)
    if true
    then
      time java -jar target/scala-${scalaVersion}/gnaf-test_${scalaVersion}-${version}-one-jar.jar --sampleSize 200 $opt1 $opt2 > $file
      wait # for previous node process
      node $search $file > stats${file#address} &
    else
      # re-run with same test data as before
      node $search $file > stats${file#address}
    fi
  done
done
wait # for last node process

node src/main/script/summary.js stats*.json
