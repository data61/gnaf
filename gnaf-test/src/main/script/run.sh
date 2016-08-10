#! /bin/bash

prepend() {
  if [[ -n "$2" ]]; then echo "$1-$2"; else echo "$1"; fi
}

for opt1 in "" "--localityAlias"
do
  out1=`prepend "address" "${opt1#--}"` 
  for opt2 in "" "--numberAdornments" "--unitLevel" "--streetAlias"
  do
    echo "options: $opt1 $opt2 ..."
    out2=`prepend "$out1" "${opt2#--}"`.json
    # sample size turns out smaller than requested because some locations have no addresses
    # and others have no addresses with the requested characteristics (haven't figured out how to filter them out cheaply)
    if false
    then
      time java -jar target/scala-2.11/gnaf-test_2.11-0.1-SNAPSHOT-one-jar.jar --sampleSize 200 $opt1 $opt2 > $out2
      node src/main/script/search.js $out2 > stats${out2#address} &
    else
      node src/main/script/search.js $out2 > stats${out2#address}
    fi
  done
done
wait # for node to complete

node src/main/script/summary.js stats*.json
