#! /bin/bash
# script to download and unpack GNAF and write a SQL script to load it.
set -ex

baseDir=$PWD
scriptDir=$baseDir/src/main/script
dataDir=$baseDir/data
mkdir -p $dataDir

# JSON URL from near top-right of: http://www.data.gov.au/dataset/geocoded-national-address-file-g-naf
jsonUrl=http://www.data.gov.au/api/3/action/package_show?id=19432f89-dc3a-4ef3-b943-5326ef1dbecc
# get data URL for current version from JSON
curl -sL $jsonUrl > meta.json
dataUrl=$( jq -r '.result.resources[] | select((.format == "ZIP") and (.name | test("GDA2020"))) | .url' meta.json )
last_modified=$( jq -r '.result.resources[] | select((.format == "ZIP") and (.name | test("GDA2020"))) | .last_modified' meta.json )

# download ZIP data file unless already done
zip=$dataDir/${dataUrl##*/}
[[ -f "$zip" ]] || ( cd $dataDir; wget --no-clobber "$dataUrl" )

unzipped=$dataDir/unzipped
# get dir path where the zip file's */Extras/ will be extracted (contains release month so releases don't clobber each other)
# get path from zip, discard leading info up to time and following spaces, keep the rest apart from the trailing /
# maybe a bit too brittle?
# 'unzip -l' prints 
#      8868  2020-08-16 22:14   G-NAF/Extras/GNAF_TableCreation_Scripts/create_tables_sqlserver.sql
# and we extract 'G-NAF'
gnafExtras="$unzipped/$( unzip -l "$zip" '*/Extras/*' | sed -rn 's~^.*[0-9][0-9]:[0-9][0-9] *(.*/Extras)/.+$~\1~p' | head -1 )"
# unzip unless $gnafExtras already exists
[[ -d "$gnafExtras" ]] || ( mkdir -p $unzipped; cd $unzipped; unzip $zip )
# get dir path parent of Standard/
# 'unzip -l' prints 
#   3357974  2020-08-16 22:22   G-NAF/G-NAF AUGUST 2020/Standard/WA_ADDRESS_ALIAS_psv.psv
# and we extract 'G-NAF/G-NAF AUGUST 2020'
gnafData="$unzipped/$( unzip -l "$zip" '*/Standard/*' | sed -rn 's~^.*[0-9][0-9]:[0-9][0-9] *(.*)/Standard/.+~\1~p' | head -1 )"

mkdir -p target/generated
cat > target/generated/version.json <<EoF
{
  "git-commit": "$( git rev-parse HEAD )",
  "sbt-version": "$( sed --regexp-extended 's/.*:=\s*"([^"]+)"/\1/' ../version.sbt )",
  "gnaf-version": "$last_modified"
}
EoF

# echo ${gnafData##*/G-NAF } -- old method for determining version

# Load GNAF into a relational database following https://www.psma.com.au/sites/default/files/g-naf_-_getting_started_guide.pdf

{

# issue message during SQL script execution
progress() {
  echo
  echo "SELECT '$1' AS Progress, CURRENT_TIME() AS Time;"
  echo
}


progress "modified: $gnafExtras/GNAF_TableCreation_Scripts/create_tables_ansi.sql"

sed -e 's/DROP TABLE/DROP TABLE IF EXISTS/' -e 's/numeric([0-9])/integer/' "$gnafExtras/GNAF_TableCreation_Scripts/create_tables_ansi.sql"

progress "load Authority Code ..."
while read tbl
do
  echo "INSERT INTO ${tbl} SELECT * FROM CSVREAD('$gnafData/Authority Code/Authority_Code_${tbl}_psv.psv', null, 'fieldSeparator=|');"
done <<-'EoF'
ADDRESS_ALIAS_TYPE_AUT
ADDRESS_TYPE_AUT
FLAT_TYPE_AUT
GEOCODE_RELIABILITY_AUT
GEOCODE_TYPE_AUT
GEOCODED_LEVEL_TYPE_AUT
LEVEL_TYPE_AUT
LOCALITY_ALIAS_TYPE_AUT
LOCALITY_CLASS_AUT
MB_MATCH_CODE_AUT
PS_JOIN_TYPE_AUT
STREET_CLASS_AUT
STREET_TYPE_AUT
STREET_LOCALITY_ALIAS_TYPE_AUT
STREET_SUFFIX_AUT
EoF
# table names pasted from g-naf_-_getting_started_guide.pdf referenced above

progress "load Standard ..."
while read tbl
do
  progress "load ${tbl} ..."
  # A-Z mess matches 2 and 3 char state abreviations (note * would try to load {state}_STREET_LOCALITY_psv.psv into LOCALITY)
  ls -1 "${gnafData}"/Standard/{[A-Z][A-Z],[A-Z][A-Z][A-Z]}_${tbl}_psv.psv | while read f
  do
    echo "INSERT INTO ${tbl} SELECT * FROM CSVREAD('$f', null, 'fieldSeparator=|');"
  done
done <<-'EoF'
ADDRESS_ALIAS
ADDRESS_DEFAULT_GEOCODE
ADDRESS_DETAIL
ADDRESS_MESH_BLOCK_2011
ADDRESS_SITE_GEOCODE
ADDRESS_SITE
LOCALITY
LOCALITY_ALIAS
LOCALITY_NEIGHBOUR
LOCALITY_POINT
MB_2011
PRIMARY_SECONDARY
STATE
STREET_LOCALITY
STREET_LOCALITY_ALIAS
STREET_LOCALITY_POINT
EoF
# table names pasted from g-naf_-_getting_started_guide.pdf referenced above

progress "add constraints ..."
sed --regexp-extended --file=$scriptDir/constraint.sed "$gnafExtras/GNAF_TableCreation_Scripts/add_fk_constraints.sql"

progress "add an index on STREET_NAME (this is not part of the getting_started_guide)..."
echo "create index STREET_LOCALITY_NAME_IDX on STREET_LOCALITY (STREET_NAME);"

# progress "add view (suggested in getting_started_guide) ..."
# commented out as not useful/too slow
# cat "$gnafExtras/GNAF_View_Scripts/address_view.sql"
# echo ";"

progress "Create READONLY user ..."
cat <<-'EoF'
CREATE USER READONLY PASSWORD 'READONLY';
GRANT SELECT ON SCHEMA PUBLIC TO READONLY;
EoF

} | sed 's/REM/--/' > $dataDir/createGnafDb.sql

cat <<-'EoF'

Start H2 database engine with: java -jar h2*.jar -web -pg
Create an empty database by connecting to a new dburl e.g. jdbc:h2:file:~/gnaf (specify 'gnaf' as the username and password).
In the SQL input area enter: RUNSCRIPT FROM 'data/createGnafDb.sql'
or paste in the content of this file (to get progress feedback lacking with RUNSCRIPT).
After an hour (with SSD) you should have a GNAF database.
EoF


