#! /bin/bash
# script to run the whole thing
# executable documentation
# you might not want to run all this each time

set -ex
version=`sed 's/.*"\(.*\)"/\1/' version.sbt`
scalaVersion=2.11

# optional recovery from gnaf-extractor connection timeout after successful population of database
if [[ "$1" != "skip" ]]; then

# === Delete/Create database ===

if [[ -f ~/gnaf.mv.db ]]; then
  rm -f ~/gnaf-old.mv.db
  mv ~/gnaf{,-old}.mv.db
fi
# rm -rf gnaf-db/data/unzipped

# create SQL load script
( cd gnaf-db; src/main/script/createGnafDb.sh;)

if [[ $? -eq 5 ]]; then echo "no new data found, cancelling build"; exit 0; fi

# build scala projects
# 1. above gnaf-db/src/main/script/createGnafDb.sh creates gnaf-db/target/generated/version.json
# 2. this version.json file is included in the gnaf-search jar by gnaf-search/build.sbt (so we need to build after running the above script)
# 3. h2 (used below) is downloaded by the build if necessary, so we need build before running h2
sbt one-jar

# run h2 with postgres protocol, remembering its PID
# get h2 version
h2ver=$( sed --quiet --regexp-extended '/com.h2database/s/.*"h2"[^"]*"([^"]*)".*/\1/p' gnaf-db/build.sbt )
echo $h2ver

java -Xmx3G -jar ~/.ivy2/cache/com.h2database/h2/jars/h2-${h2ver}.jar -web -pg &
H2_PID=$!
sleep 10

# set psql gnaf password to gnaf
[[ -r ~/.pgpass ]] && grep -q gnaf ~/.pgpass || {
  echo "localhost:5435:~/gnaf:gnaf:gnaf" >> ~/.pgpass
  chmod 600 ~/.pgpass
}

# run load script using Postgres client, takes about 90 minutes with a SSD
# see gnaf-db/README.md for an alternative method using the h2 client
psql --host=localhost --port=5435 --username=gnaf --dbname=~/gnaf < gnaf-db/data/createGnafDb.sql

# attempt to avoid gnaf-extractor failing below with: java.sql.SQLTimeoutException: Timeout after 10000ms of waiting for a connection
sleep 10

# stop h2
kill $H2_PID
wait

fi

# === Extract JSON address data and load into Lucene ===

# takes about 23 min
time java -Xmx3G -jar gnaf-extractor/target/scala-${scalaVersion}/gnaf-extractor_${scalaVersion}-${version}-one-jar.jar | gzip > addresses.gz

# takes about 13 min
time zcat addresses.gz | java -jar gnaf-indexer/target/scala-${scalaVersion}/gnaf-indexer_${scalaVersion}-${version}-one-jar.jar

#
#
## === demo gnaf-search and gnaf-test ===
#
#java -jar gnaf-search/target/scala-${scalaVersion}/gnaf-search_${scalaVersion}-${version}-one-jar.jar &
#SEARCH_PID=$!
#sleep 15 # we could wait for it to log a message
#
#echo "gnaf-search: swagger.json ..."
#curl http://localhost:9040/api-docs/swagger.json
#curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{
#  "addr": "137-~45 CHEVALLUM SCHOOL ROAD PALMWOODS QLD 4555",
#  "numHits": 3,
#  "fuzzy": {
#    "maxEdits": 2,
#    "minLength": 5,
#    "prefixLength": 2
#  }
#}' 'http://localhost:9040/search'
#echo
#
## takes about 12 min
## gnaf-search must be running
## gnaf-db-service must not be running (both use the gnaf database in embedded mode, to run at the same time they would need
## to use different databases or not use embedded mode).
#echo "gnaf-test ..."
#cd gnaf-test
#npm install
#time src/main/script/run.sh
#cd ..
#
## === demo gnaf-db-service ===
#
#java -jar gnaf-db-service/target/scala-${scalaVersion}/gnaf-db-service_${scalaVersion}-${version}-one-jar.jar &
#DB_PID=$!
#sleep 15
#
#echo "gnaf-db-service: swagger.json ..."
#curl http://localhost:9000/api-docs/swagger.json
#echo "get geocode types and descriptions ..."
#curl 'http://localhost:9000/gnaf/geocodeType'
#echo "get type of address e.g. RURAL, often missing, for an addressDetailPid ..."
#curl 'http://localhost:9000/gnaf/addressType/GANSW716635201'
#echo "get all geocodes for an addressDetailPid, almost always 1, sometimes 2, never more ..."
#curl 'http://localhost:9000/gnaf/addressGeocode/GASA_414912543'
#echo
#
#
## === demo gnaf-contrib ===
#
#java -jar gnaf-contrib/target/scala-${scalaVersion}/gnaf-contrib_${scalaVersion}-${version}-one-jar.jar &
#CONTRIB_PID=$!
#sleep 15
#
#echo "gnaf-contrib: swagger.json ..."
#curl http://localhost:9010/api-docs/swagger.json
#echo "add contributed geocode for an addressSite ..."
#curl -XPOST 'http://localhost:9010/contrib/' -H 'Content-Type:application/json' -d '{
#  "contribStatus":"Submitted","addressSitePid":"712279621","geocodeTypeCode":"EM",
#  "longitude":149.1213974,"latitude":-35.280994199999995,"dateCreated":0,"version":0
#}'
#echo "list contributed geocodes for an addressSite ..."
#curl 'http://localhost:9010/contrib/712279621'
## there are also delete and update methods
#
## === Stop JSON web services ===
#
#kill $SEARCH_PID
#kill $DB_PID
#kill $CONTRIB_PID
#wait
