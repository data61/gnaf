#! /bin/bash

concat() {
  if [[ -n "$2" ]]; then echo "$1-$2"; else echo "$1"; fi
}

# search="src/main/script/searchEs.js"
search="src/main/script/searchLucene.js"

for opt1 in "" "--localityAlias"
do
  filePrefix=`concat "address" "${opt1#--}"` 
  for opt2 in "" "--numberAdornments" "--unit" "--level" "--streetAlias"
  do
    file=`concat "$filePrefix" "${opt2#--}"`.json
    echo "options: $opt1 $opt2 ..."
    # sample size turns out smaller than requested because some locations have no addresses
    # and others have no addresses with the requested characteristics (haven't figured out how to filter them out cheaply)
    if false
    then
      time java -jar target/scala-2.11/gnaf-test_2.11-0.1-SNAPSHOT-one-jar.jar --sampleSize 200 $opt1 $opt2 > $file
      node $search $file > stats${file#address} &
    else
      node $search $file > stats${file#address}
    fi
  done
done
wait # for node to complete

node src/main/script/summary.js stats*.json
