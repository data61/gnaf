#! /bin/bash
# script to download and unpack GNAF and write a SQL script to load it.
set -ex

baseDir=$PWD
scriptDir=$baseDir/src/main/script
dataDir=$baseDir/data
unzipped=$dataDir/unzipped
mkdir -p $dataDir

# update these vars for new release
gnafUrl="https://s3-ap-southeast-2.amazonaws.com/datagovau/AUG16_GNAF%2BEULA_PipeSeparatedValue.zip"
gnafDataDirName="G-NAF AUGUST 2016"

gnafName=${gnafUrl##*/}
gnafName=${gnafName%.zip}
gnafName=${gnafName/'%2B'/+}
gnaf="$unzipped/${gnafName}/G-NAF"
gnafData="$gnaf/$gnafDataDirName"

zip=$dataDir/${gnafName}.zip
[[ -f "$zip" ]] || ( cd $dataDir; wget "$gnafUrl" )

[[ -d "$unzipped" ]] || ( mkdir -p $unzipped; cd $unzipped; unzip $zip )

# Load GNAF into a relational database following https://www.psma.com.au/sites/default/files/g-naf_-_getting_started_guide.pdf

{

# issue message during SQL script execution
progress() {
  echo
  echo "SELECT '$1' AS Progress, CURRENT_TIME() AS Time;"
  echo
}


progress "modified: $gnaf/Extras/GNAF_TableCreation_Scripts/create_tables_ansi.sql"

sed -e 's/DROP TABLE/DROP TABLE IF EXISTS/' -e 's/numeric([0-9])/integer/' "$gnaf/Extras/GNAF_TableCreation_Scripts/create_tables_ansi.sql"

progress "load Authority Code ..."
while read tbl file
do
  echo "INSERT INTO $tbl SELECT * FROM CSVREAD('$gnafData/Authority Code/$file', null, 'fieldSeparator=|');"
done <<-'EoF'
ADDRESS_ALIAS_TYPE_AUT Authority_Code_ADDRESS_ALIAS_TYPE_AUT_psv.psv
ADDRESS_TYPE_AUT Authority_Code_ADDRESS_TYPE_AUT_psv.psv
FLAT_TYPE_AUT Authority_Code_FLAT_TYPE_AUT_psv.psv
GEOCODE_RELIABILITY_AUT Authority_Code_GEOCODE_RELIABILITY_AUT_psv.psv
GEOCODE_TYPE_AUT Authority_Code_GEOCODE_TYPE_AUT_psv.psv
GEOCODED_LEVEL_TYPE_AUT Authority_Code_GEOCODED_LEVEL_TYPE_AUT_psv.psv
LEVEL_TYPE_AUT Authority_Code_LEVEL_TYPE_AUT_psv.psv
LOCALITY_ALIAS_TYPE_AUT Authority_Code_LOCALITY_ALIAS_TYPE_AUT_psv.psv
LOCALITY_CLASS_AUT Authority_Code_LOCALITY_CLASS_AUT_psv.psv
MB_MATCH_CODE_AUT Authority_Code_MB_MATCH_CODE_AUT_psv.psv
PS_JOIN_TYPE_AUT Authority_Code_PS_JOIN_TYPE_AUT_psv.psv
STREET_CLASS_AUT Authority_Code_STREET_CLASS_AUT_psv.psv
STREET_TYPE_AUT Authority_Code_STREET_TYPE_AUT_psv.psv
STREET_LOCALITY_ALIAS_TYPE_AUT Authority_Code_STREET_LOCALITY_ALIAS_TYPE_AUT_psv.psv
STREET_SUFFIX_AUT Authority_Code_STREET_SUFFIX_AUT_psv.psv
EoF
# table name / data file pairs above pasted from g-naf_-_getting_started_guide.pdf referenced above

progress "load Standard ..."
while read tbl file
do
  progress "load $tbl ..."
  name=${file#ACT}
  ls "$gnafData/Standard/"*$name | egrep "/[A-Z]+$name" | while read f # don't load ACT_STREET_LOCALITY_psv.psv into LOCALITY
  do
    echo "INSERT INTO $tbl SELECT * FROM CSVREAD('$f', null, 'fieldSeparator=|');"
  done
done <<-'EoF'
ADDRESS_ALIAS ACT_ADDRESS_ALIAS_psv.psv
ADDRESS_DEFAULT_GEOCODE ACT_ADDRESS_DEFAULT_GEOCODE_psv.psv
ADDRESS_DETAIL ACT_ADDRESS_DETAIL_psv.psv
ADDRESS_MESH_BLOCK_2011 ACT_ADDRESS_MESH_BLOCK_2011_psv.psv
ADDRESS_SITE_GEOCODE ACT_ADDRESS_SITE_GEOCODE_psv.psv
ADDRESS_SITE ACT_ADDRESS_SITE_psv.psv
LOCALITY ACT_LOCALITY_psv.psv
LOCALITY_ALIAS ACT_LOCALITY_ALIAS_psv.psv
LOCALITY_NEIGHBOUR ACT_LOCALITY_NEIGHBOUR_psv.psv
LOCALITY_POINT ACT_LOCALITY_POINT_psv.psv
MB_2011 ACT_MB_2011_psv.psv
PRIMARY_SECONDARY ACT_PRIMARY_SECONDARY_psv.psv
STATE ACT_STATE_psv.psv
STREET_LOCALITY ACT_STREET_LOCALITY_psv.psv
STREET_LOCALITY_ALIAS ACT_STREET_LOCALITY_ALIAS_psv.psv
STREET_LOCALITY_POINT ACT_STREET_LOCALITY_POINT_psv.psv
EoF
# table name / data file pairs above pasted from g-naf_-_getting_started_guide.pdf referenced above

progress "add constraints ..."
sed --regexp-extended --file=$scriptDir/constraint.sed "$gnaf/Extras/GNAF_TableCreation_Scripts/add_fk_constraints.sql"

progress "add an index on STREET_NAME (this is not part of the getting_started_guide)..."
echo "create index STREET_LOCALITY_NAME_IDX on STREET_LOCALITY (STREET_NAME);"

progress "add view (suggested in getting_started_guide) ..."
cat "$gnaf/Extras/GNAF_View_Scripts/address_view.sql"
echo ";"

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


