# Prime JSON
A Java library and suite of command-line tools that facilitate data transformations.


This readme serves as a brief introduction to Prime and its high-level features.  You are encouraged
to consult the wiki for more complete documentation.  (TODO)

There are _many_ tools available for parsing and manipulating data from files.  If you simply need to extract
some arbitrary fields from a CSV file, Prime is likely _not_ the best tool.  However, if you need to read values
from a file that arrives every week and has undergone 3 layout changes in the past year, then Prime is
a very good choice.


## Maven

The release artifacts are in the Maven Central Repository.

```xml
<dependency>
    <groupId>com.real-comp</groupId>
    <artifactId>prime-json</artifactId>
    <version>0.5.0</version>
</dependency>      
```

## Data Types

Prime supports the following data types:

* String
* Float
* Integer
* Double
* Long
* Boolean
* List
* Map


## Record

A Record is the internal data model for Prime and is a fancy Java Map&lt;String,Object&gt;.  Values in Records are retrieved by
_name_ and the type of the stored values can be coerced.

Here is an example of a Record with two String fields.  Notice the age field is being coerced from a String to an Integer.
```java
Record record = new Record();
record.put("name", "Bob");
record.put("age", "21");
assertEquals(21, record.getInt("age"));
```

Using internal Lists and Maps, a Record can represent a relational data model.  Here we have a simple Map of address
information that is added to a Record.  Using the _name1.name2_ convention, values can be resolved from deep
within a Record.

```java
Map<String,String> address = new HashMap<>();
address.put("street", "1234 Main St");
address.put("city", "Austin");
address.put("state", "TX");

Record record = new Record();
record.put("name, "Bob");
record.put("location", address);
assertEquals("Austin", record.get("location.city");

```


## Schema

A schema describes the format of a file (CSV,TAB,FIXED,JSON,...) with optional validators and converters.  A schema is typically serialized
to XML.  Schemas are fairly powerful and can handle:

* Header records and other format specific features
* Multiple formats within a single file
* Validations of data in individual fields
* Conversion of data in individual fields, or all fields.


For a very simple CSV file:
```csv
name,faction,rank
Optimus Prime,Autobot,Prime
Bumblebee,Autobot,Car
Megatron,Decepticon,Leader
Starscream,Decepticon,Commander
```

We have a very simple Schema:
```xml
<rc:schema
   xmlns:rc="http://www.real-comp.com/realcomp-data/schema/file-schema/1.2"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.real-comp.com/realcomp-data/schema/file-schema/1.2 http://www.real-comp.com/realcomp-data/schema/file-schema/1.2/file-schema.xsd">

    <format type="CSV" header="true"/>

    <fields>
        <field name="name"/>
        <field name="faction"/>
        <field name="rank"/>
    </fields>
</rc:schema>

```
note: The XML namespaces reference realcomp-data, the old internal name of this project, instead of _prime_.  This will be fixed at some point.  The realcomp-data schemas will continue to be hosted for backward compatibility.

## Validators

Validation of data in a file (or other source), is an important step to ensure
 the quality of your data and detecting format changes over time.
 
Prime ships with many useful validators, and you can add your own.

| Validator | Description | Example |
| --------- | ----------- | ------- |
| LengthValidator   | validate that a field has a specific length | &lt;validateLength min="1" max="10"/&gt; |
| RequiredValidator | validate that a field is populated | &lt;required/&gt; |
| LongRangeValidator | validate that a field has a value in a specific range. | &lt;validateLongRange min="0" max="100"/&gt; |
| RegexValidator    | validate that a field matches a regular expression. | &lt;validateRegex regex="[0-9]{4}"/&gt; | 
  
Here is our schema with a validation defined:
 ```xml
 <rc:schema
    xmlns:rc="http://www.real-comp.com/realcomp-data/schema/file-schema/1.2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.real-comp.com/realcomp-data/schema/file-schema/1.2 http://www.real-comp.com/realcomp-data/schema/file-schema/1.2/file-schema.xsd">
 
     <format type="CSV" header="true"/>
 
     <fields>
         <field name="name">
             <required/>  <!-- our validator -->
         </field>
         <field name="faction"/>
         <field name="rank"/>
     </fields>
 </rc:schema>
 
 ```

## Converters

Converters describe modifications that should be performed on your data.

Prime ships with _many_ useful converters, and you can add your own.  
Here are _some_ of the build-in converters.

| Converter | Description | Example |
| --------- | ----------- | ------- |
| Append      | append a value to a field | &lt;append value="!"/&gt; |
| Trim        | trim leading and trailing whitespace | &lt;trim/&gt; | 
| UpperCase   | convert lower-case letters to upper-case | &lt;upperCase/&gt; |
| Concat      | combine fields | &lt;concat fields="name,rank"/&gt; |
| CurrentDate | emit the current date | &lt;currentDate format="short"/&gt; |
| LeftPad     | pad a field with a character to a length | &lt;leftPad with=" " length="100"/&gt; |
| Default     | provide a default value for a field | &lt;default value="Autobots Roll Out!"/&gt; |
| Divide      | divide a numeric value | &lt;divide divisor="100"/&gt;|
| Replace     | replace one or more characters | &lt;replace regex="," replacement="-"/&gt; |
| Sequence    | a monotonically increasing sequence number | &lt;sequence/&gt; |

There are many more.

Here is our example schema again with a few converters defined:
 ```xml
 <rc:schema
    xmlns:rc="http://www.real-comp.com/realcomp-data/schema/file-schema/1.2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.real-comp.com/realcomp-data/schema/file-schema/1.2 http://www.real-comp.com/realcomp-data/schema/file-schema/1.2/file-schema.xsd">
 
     <format type="CSV" header="true"/>
 
     <fields>
         <field name="name">
             <required/>
             <trim/>
             <upperCase/>             
         </field>
         <field name="faction"/>
         <field name="rank">
             <default value="none"/>
         </field>
     </fields>
 </rc:schema>
 
 ```
 
 
## Formats

Prime supports CSV, TAB, and FIXED length files by default.

JSON support is provided by https://github.com/real-comp/prime-json

X-BASE (dbf) support is provided by https://github.com/real-comp/prime-xbase

Support for other file formats can be added.

Usually, Prime reads and writes to files, but in general any Input/OutputStream can be used
 as an input/output source.

## Command-Line Utilities

Armed with a basic understanding of Prime, I will highlight two utilities that are useful.


### Validation
Validates a file against a schema.  As a general best practice - validate your source data against the 
schema.  Detecting format issues early is a good thing.
 
See src/main/resources for the input.schema, output.schema, and test.csv files used in this example.
  
```bash
java -cp prime-0.5.0-jar-with-dependencies.jar com.realcomp.prime.util.Validate --in test.csv --is input.schema  
```

Problems will be reported on STDERR.
The exit code will be 0 if the file validated, else 1.

Note: Utilities works with STDIN/STDOUT.
```bash
cat test.csv | java -cp prime-0.5.0-jar-with-dependencies.jar com.realcomp.prime.util.Validate  --is input.schema  
```

### Reformat
Often, you will be tasked to convert a source file to a different format.  The 
Reformat utility is your friend here.

Armed with the input.schema and output.schema:

```bash
cat test.csv | java -cp prime-0.5.0-jar-with-dependencies.jar com.realcomp.prime.util.Reformat --is input.schema --os output.schema
"id","name","rank","faction"
"1","OPTIMUS PRIME","Prime","GOOD"
"2","BUMBLEBEE","Car","GOOD"
"3","MEGATRON","Leader","BAD"
"4","STARSCREAM","Commander","BAD"
```
Problems will be reported on STDERR.
The exit code will be 0 if the file conversion was successful, else 1.


