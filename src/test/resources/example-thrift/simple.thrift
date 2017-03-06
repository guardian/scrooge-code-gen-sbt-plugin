namespace   java  simple.test
#@namespace scala simple.test

include "included.thrift"
include "alsoincluded.thrift"

struct SimpleStruct {
  1: string name
  2: optional i32 age
  3: list<string> strings
}

struct OtherSimpleStruct {
  1: double number
}

struct HasNested {
  1: string name
  2: SimpleStruct nested
  3: included.IncludedStruct otherFile
}
