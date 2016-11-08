# gnaf-lucene

## Introduction

This project produces a library of common code for indexing and searching G-NAF with [Lucene](https://lucene.apache.org/)
and is used by `gnaf-indexer` and `gnaf-search`.

## Search Techniques

### Indexing

The following G-NAF data is formatted into an array of strings (one array element per bullet point):

- site name, building name (commas not included)
- unit/flat,
- level,
- street (number ranges are formatted with a minus separator and no space e.g. "2-4 Reed Street South"),
- locality, state abbreviation, postcode;

plus:

- one array element for each street alias; and
- one array element for each locality alias: locality alias, state abbreviation, postcode

These strings are indexed into the same Lucene field using `WhitespaceTokenizer`, `LowerCaseFilter` and `ShingleFilter` producing unigram and bigram tokens.
Bigrams provide a reward for terms appearing in the above order.
A PositionIncrementGap is used to prevent bigrams going across string boundaries so that only ordering within each string is rewarded, not between them.

A case where this indexing scheme doesn't work well is a user query for "2 17 SMITH STREET". We understand the 2 represents a unit/flat number, because if it was a level number it would need some text to indicate that. The 2 and the 17 appear in separate array elements so "2 17" will not produce a bigram match. The "2" will only score as a unigram match to any "2" e.g. possibly a level or street number. In the case that an address has a flat number and a street number but no level, a flat number/street number bigram is added to the index specifically to handle queries of this form.

A search for a street address with no flat specified should score a match to the street address with no flat higher than one with a spurious match to a flat. More generally it is desirable to add a slight boost (less than the score increment for a correct match) to results with missing data for: site/building, flat, level, and street number. This is facilitated by adding a MISSING_DATA_TOKEN to the field F_MISSING_DATA for each missing data element from this list.

### Searching

Query tokenization and filtering is as discussed above (under Indexing).
Bigram term matches are boosted by a factor of 3 to reward correct ordering.
MISSING_DATA_TOKEN is added to the query boosted by 0.05 to slightly boost results for each missing data element.

### Scoring

Analysis of results using `gnaf-test` has shown that Lucene's default scoring based on language models doesn't work well with address data.

`AddressSimilarity` is used to override the default scoring:

- length norm is disabled so that multiple aliases are not penalized
- term frequency is disabled so that a matching street and locality name isn't unduly rewarded
- document frequency is disabled so that common street names are not penalized

`MissingDataSimilarity` overrides the scoring for the field F_MISSING_DATA:

- length norm is disabled so that multiple tokens are not penalized
- term frequency is enabled so that multiple tokens score more
- document frequency is disabled (it's a constant as we only have one unique token)

#### Suggested preprocessing for client applications

People often use "2 / 12 BLAH STREET" for "UNIT 2 12 BLAH STREET" (which corresponds the indexed format).
Bigrams will provide a high score for "2 12 BLAH" but not for "2 / 12 BLAH", so "/" in the input should be replaced with a space.
Similarly any commas in the input should also be replaced with a space.

The only useful non-alphanumeric characters are '-' as a number range separator and some non-alphanumeric characters that may appear
in names such as "-" and "'".
