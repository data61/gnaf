#! /bin/bash

set -ex

# sbt oneJar

mkdir -p tmp
# time java -Xmx3G -jar target/scala-2.11/gnaf_2.11-0.1-SNAPSHOT-one-jar.jar > tmp/out
# mv gnaf.log tmp

# curl -XDELETE 'localhost:9200/gnaf/'

# curl -XPUT 'localhost:9200/gnaf/' --data-binary @src/main/resources/gnafMapping.json

(
  cd tmp
  
  #time jq -c '
  #{ index: { _index: "gnaf", _type: "gnaf", _id: .addressDetailPid } },
  #. ' out > bulk

  rm -f x???
  split -l10000 -a3 bulk
  for i in x???
  do
    echo $i
    curl -s -XPOST localhost:9200/_bulk --data-binary @$i
  done
)


