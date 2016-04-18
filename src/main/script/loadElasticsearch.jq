def filter(pred): map(select(pred)); # filter array keeping only elements matching pred
def d61num: [.prefix, (.number|tostring), .suffix ] | filter(. != "D61_NULL" and . != "-1") | join("");
def d61numLast: if .number == -1 then null else "-" + (.|d61num) end;

# prepend elasticsearch indexing metadata
{ index: { _index: "gnaf", _type: "gnaf", _id: .addressDetailPid } },
# add d61Address.
# Each inner array below gets indexed as a separate Lucene "value" in the "d61Address" field.
# Although Lucene just concatenates all the values into the field there is a big position increment between the values
# ("position_increment_gap": 100 set in gnafMapping.json) that stops phrase searches and shingles (n-grams) matching across values.
setpath(
  ["d61Address"];
  [(
    [
      [ .addressSiteName, .buildingName ],
      [ .flatTypeName, (.flat|d61num),
        .levelTypeName, (.level|d61num),
        ((.numberFirst|d61num) + (.numberLast|d61numLast)), .street.name, .street.typeCode, .street.suffixName
      ], 
      [ .localityName, .stateAbbreviation, .postcode ]
    ]
    + [ .streetVariant[] | [ .name, .typeCode, .suffixName ] ]
    + .stateAbbreviation as $stateAbbreviation | [ .localityVariant[] | [ .localityName, $stateAbbreviation ] ] # append stateAbbreviation for 2-gram
  )[]
  | filter(. != "" and . != null and . != "D61_NULL") | join(" ") # remove null/empty elements from inner arrays and join to convert to string
  ]
)
