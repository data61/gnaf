# gnaf-common

## Introduction

[Slick](http://slick.typesafe.com/) provides "Functional Relational Mapping for Scala".
This project provides a Slick mapping for the GNAF database created by the gnaf-createdb project.
The mappings are not tied to any particular relational database.

## Generate Slick bindings

To generate Slick mappings for the database ~/gnaf.mv.db, from the top level gnaf directory:

    sbt
    > project gnafCommon
    > console
    slick.codegen.SourceCodeGenerator.main(
        Array("slick.driver.H2Driver", "org.h2.Driver", "jdbc:h2:file:~/gnaf", "generated", "au.csiro.data61.gnaf.common.db", "gnaf", gnaf")
    )

This generates code in: `generated/au/csiro/data61/gnaf/common/db/Tables.scala`.
The source file `src/main/scala/au/csiro/data61/gnaf/common/db/GnafTables.scala` is a very minor modification of this generated code.

