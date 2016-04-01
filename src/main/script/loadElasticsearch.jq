# define our own filters
def d61get: if . == "D61_NULL" then "" else . end;
def d61space: if . == "" then . else " " + . end;
def d61num: (.prefix|d61get) + (.number|tostring) + (.suffix|d61get);
def d61numLast: if .number == -1 then "" else " - " + (.|d61num) end;

# prepend elasticsearch indexing metadata
{ index: { _index: "gnaf", _type: "gnaf", _id: .addressDetailPid } },
# copy of input object with a new property d61SugStreet added to populate a suggester
setpath( ["d61SugStreet"]; { "input": ((.numberFirst|d61num) + (.numberLast|d61numLast) + " " + .street.name + " " + .street.typeCode + (.street.suffixName|d61get|d61space)) } )
