# gnaf-lucene

## Introduction

This project produces a library of common code for indexing and searching G-NAF with [Lucene](https://lucene.apache.org/)
and is used by `gnaf-indexer` and `gnaf-search`.

## Search Strategy

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

Analysis of results using `gnaf-test` has shown that Lucene's default scoring based on language models doesn't work well with address data.
`AddressSimilarity` is used to override this behaviour:

- length norm is disabled so that multiple aliases are not penalized
- term frequency is disabled so that a matching street and locality name isn't unduly rewarded
- document frequency is disabled so that common street names are not penalized

For a street address with no street number, a "d61_no_num" token is indexed to represent the missing number.

### Searching

Bigram term matches are boosted by a factor of 3 to reward correct ordering.
"d61_no_num" is added to the query boosted by 0.1 so that a matching street number will score much higher, but otherwise a street with
no number will be preferred over one with a spurious number.
Input tokenization and filtering is as discussed above (under Indexing) and scoring is provided by `AddressSimilarity` also as above.

#### Suggested preprocessing for client applications

People often use "2 / 12 BLAH STREET" for "UNIT 2 12 BLAH STREET" (which corresponds the indexed format).
Bigrams will provide a high score for "2 12 BLAH" but not for "2 / 12 BLAH", so "/" in the input should be replaced with a space.
Similarly any commas in the input should also be replaced with a space.

The only useful non-alphanumeric characters are '-' as a number range separator and some non-alphanumeric characters that may appear
in names such as "-" and "'".
