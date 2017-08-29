package com.realcomp.prime.util;

import com.realcomp.prime.conversion.ConversionException;
import com.realcomp.prime.record.Record;
import com.realcomp.prime.record.io.IOContext;
import com.realcomp.prime.record.io.IOContextBuilder;
import com.realcomp.prime.record.io.json.JsonReader;
import com.realcomp.prime.schema.Field;
import com.realcomp.prime.schema.FieldList;
import com.realcomp.prime.schema.Schema;
import com.realcomp.prime.schema.SchemaException;
import com.realcomp.prime.schema.xml.XStreamFactory;
import com.realcomp.prime.validation.ValidationException;
import com.thoughtworks.xstream.XStream;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Schema generator for json files.
 */
public class JsonSchemaGenerator{

    private static final Logger logger = Logger.getLogger(SchemaGenerator.class.getName());

    private long limit = Long.MAX_VALUE;

    public long getLimit(){
        return limit;
    }

    public void setLimit(long limit){
        this.limit = limit;
    }

    public void generate(InputStream in, OutputStream out) throws IOException{
        LinkedHashSet<String> fieldNames = getFieldNames(in);
        Schema schema = buildSchema(fieldNames);
        writeSchema(schema, out);
    }

    protected LinkedHashSet<String> getFieldNames(InputStream in) throws IOException{
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        try (JsonReader reader = new JsonReader();
             IOContext ctx = new IOContextBuilder().in(in).build()){

            try{
                reader.open(ctx);
                Record record = reader.read();
                while (record != null && reader.getCount() < limit){
                    fields.addAll(record.keySet());
                    record = reader.read();
                }
            }
            catch (SchemaException | ConversionException | ValidationException e){
                throw new IOException(e);
            }
        }
        return fields;
    }

    public Schema generate(File file) throws IOException{
        try (InputStream in = new FileInputStream(file)){
            LinkedHashSet<String> fieldNames = getFieldNames(in);
            return buildSchema(fieldNames);
        }
    }

    public Schema generate(Record record) throws IOException{
        LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
        fieldNames.addAll(record.keySet());
        return buildSchema(fieldNames);
    }


    protected String toXml(Schema schema){
        XStream xstream = XStreamFactory.build(true);
        StringWriter temp = new StringWriter();
        xstream.toXML(schema, temp);
        return temp.getBuffer().toString();
    }

    protected void writeSchema(Schema schema, OutputStream out) throws IOException{

        String xml = toXml(schema);
        xml = clean(xml);
        out.write(xml.getBytes());
    }

    protected Schema buildSchema(LinkedHashSet<String> fieldNames){
        Schema schema = new Schema();
        Map<String, String> format = new HashMap<>();
        format.put("type", "json");
        schema.setFormat(format);

        int count = 1;
        FieldList fieldList = new FieldList();
        for (String fieldName : fieldNames){
            fieldList.add(new Field(fieldName.isEmpty() ? "FIELD" + count : fieldName));
            count++;
        }
        schema.addFieldList(fieldList);
        return schema;
    }

    protected String clean(String dirty){
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        header = header.concat("<rc:schema\n");
        header = header.concat("   xmlns:rc=\"http://www.real-comp.com/realcomp-data/schema/file-schema/1.2\"\n");
        header = header.concat("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        header =
                header.concat("   xsi:schemaLocation=\"http://www.real-comp.com/realcomp-prime/schema/file-schema/1.2 http://www.real-comp.com/realcomp-prime/schema/file-schema/1.2/file-schema.xsd\">\n");

        String clean = header.concat(dirty.replace("<schema>", ""));
        clean = clean.replace("</schema>", "</rc:schema>");
        clean = clean.replaceAll(" length=\"0\"", "");
        clean = clean.replaceAll(" classifier=\".*\"", "");
        clean = clean.concat("\n");
        return clean;
    }


    private static void printHelp(OptionParser parser){
        try{
            parser.printHelpOn(System.err);
        }
        catch (IOException ignored){
        }
    }

    public static void main(String[] args){

        OptionParser parser = new OptionParser(){
            {
                accepts("limit", "max number of input records to examine").withRequiredArg().describedAs("count");
                accepts("in", "input file (default: STDIN)").withRequiredArg().describedAs("file");
                accepts("out", "output file (default: STDOUT)").withRequiredArg().describedAs("file");
                acceptsAll(Arrays.asList("h", "?", "help"), "help");
            }
        };

        int result = 0;

        try{
            OptionSet options = parser.parse(args);
            if (options.has("?")){
                printHelp(parser);
            }
            else{
                JsonSchemaGenerator generator = new JsonSchemaGenerator();

                if (options.has("limit")){
                    generator.setLimit(Long.parseLong((String) options.valueOf("limit")));
                }

                InputStream in =  options.has("in")
                        ? new BufferedInputStream(new FileInputStream((String) options.valueOf("in")))
                        : new BufferedInputStream(System.in);
                OutputStream out = options.has("out")
                        ? new BufferedOutputStream(new FileOutputStream((String) options.valueOf("out")))
                        : new BufferedOutputStream(System.out);

                generator.generate(in, out);
                result = 0;
                in.close();
                out.close();
            }
        }
        catch (IOException ex){
            logger.severe(ex.getMessage());
        }
        catch (OptionException ex){
            logger.severe(ex.getMessage());
            printHelp(parser);
        }

        System.exit(result);
    }
}
