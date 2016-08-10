#! /bin/bash

set -ex
GNAF=$PWD

DIR=tmp
# DIR=/srv/gnaf/data # for http://gnaf.it.csiro.au/ (no space in user home dir)

if false
then

# run Scala program, takes about 25min with a SSD
rm -f gnaf-indexer.log
mkdir -p $DIR
time java -Xmx3G -jar target/scala-2.11/gnaf-indexer_2.11-0.1-SNAPSHOT-one-jar.jar | gzip > $DIR/out.gz
mv gnaf-indexer.log $DIR

(
  cd $DIR
  
  # transform output of Scala program to suit Elasticsearch 'bulk' API, takes about 15min with a SSD (was 32min using jq)
  time zcat out.gz | node $GNAF/src/main/script/loadElasticsearch.js > bulk

  # split 'bulk' file into chunks not too big for a POST request
  rm -f chunk-???
  split -l10000 -a3 bulk chunk-
)

fi

# backup old index? (for cluster.name: neilsElasSrch set in elasticsearch.yml)
# tar cvfz index1.tar.gz -C /var/lib/elasticsearch/neilsElasSrch/ nodes

# delete any old index
curl -XDELETE 'localhost:9200/gnaf/'

# create new index with custom field mappings
curl -XPUT 'localhost:9200/gnaf/' --data-binary @src/main/script/gnafMapping.json

# load the chunks using the Elasticsearch 'bulk' API 
for i in $DIR/chunk-???
do
  echo $i
  curl -s -XPOST localhost:9200/_bulk --data-binary @$i
done

echo "all done"


