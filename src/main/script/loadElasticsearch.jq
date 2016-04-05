# define our own filters
def d61Filter: map(select(. != "D61_NULL" and . != "-1" and . != "" and . != null));
def d61num: [.prefix, (.number|tostring), .suffix ] | d61Filter | join("");
def d61numLast: if .number == -1 then null else "-" + (.|d61num) end;
def line1: [ .addressSiteName, .buildingName ] | d61Filter | join(" ");
def line2: [
  .flatTypeName, (.flat|d61num),
  .levelTypeName, (.level|d61num),
  ((.numberFirst|d61num) + (.numberLast|d61numLast)), .street.name, .street.typeCode, .street.suffixName
  ] | d61Filter | join(" ");
def line3: [ .localityName, .stateAbbreviation, .postcode ] | d61Filter | join(" ");

# prepend elasticsearch indexing metadata
{ index: { _index: "gnaf", _type: "gnaf", _id: .addressDetailPid } },
# add d61SugStreet to populate a suggester
(
setpath( ["d61SugStreet"]; {
  "input": [ ((.numberFirst|d61num) + (.numberLast|d61numLast)), .street.name, .street.typeCode, .street.suffixName ] | d61Filter | join(" "),
  "payload" : { addressDetailPid, localityName, stateAbbreviation, postcode }
} ) | setpath( ["d61Address"]; [ line1, line2, line3 ] | d61Filter | join(", ") )
)