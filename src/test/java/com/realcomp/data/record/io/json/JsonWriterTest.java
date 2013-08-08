package com.realcomp.data.record.io.json;

import com.realcomp.data.record.Record;
import com.realcomp.data.record.io.IOContext;
import com.realcomp.data.record.io.IOContextBuilder;
import com.realcomp.data.record.io.RecordWriterFactory;
import com.realcomp.data.schema.Schema;
import com.realcomp.data.schema.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author krenfro
 */
public class JsonWriterTest {


    static{
        RecordWriterFactory.registerWriter("JSON", JsonWriter.class.getName());
    }


    public JsonWriterTest() {
    }


    private Record getSampleRecord(){

        Record record = new Record();
        record.put("zip", "78717");
        record.put("address", "8665 EPHRAIM RD");
        record.put("userId", "73783");
        record.put("orderId", "299");
        record.put("product", "ALLSTATE AUTO SPECIFIC");
        record.put("source", "relevate");
        record.put("usedDate", "2011-09-21");
        return record;
    }

    private Record getComplexRecord(){
        Record record = new Record();
        record.put("s", "test string");
        record.put("i", 1);
        record.put("f", 1.2f);
        record.put("d", 1.3434d);
        record.put("l", 7474l);
        record.put("b", true);

        ArrayList list = new ArrayList();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add(1);
        list.add(2);
        list.add(3);
        record.put("list", list);

        Map<String,Object> map = new HashMap<>();
        map.put("entry", "a");
        map.put("entryNumber", 1);
        record.put("map", map);

        return record;
    }



    @Test
    public void testNoRecords() throws Exception{

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOContext ctx = new IOContextBuilder().out(out).build();

        //write the Record to json string.
        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        writer.close();

        assertEquals("[]", new String(out.toByteArray()));
    }


    @Test
    public void testOneRecord() throws Exception{

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOContext ctx = new IOContextBuilder().out(out).build();

        //write the Record to json string.
        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        Record record = new Record();
        record.put("a", 1);
        writer.write(record);
        writer.close();

        assertEquals("[{\"a\":1}]", new String(out.toByteArray()));
    }

    @Test
    public void testTwoRecords() throws Exception{

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOContext ctx = new IOContextBuilder().out(out).build();

        //write the Record to json string.
        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        Record record = new Record();
        record.put("a", 1);
        writer.write(record);
        writer.write(record);
        writer.close();

        assertEquals("[{\"a\":1}\n,{\"a\":1}]", new String(out.toByteArray()));
    }


    @Test
    public void testWrite() throws Exception{

        IOContext ctx = new IOContextBuilder()
                .out(new ByteArrayOutputStream())
                .build();

        //write the Record to json string.
        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        writer.write(getSampleRecord());
        writer.close();


        byte[] ba = ((ByteArrayOutputStream) ctx.getOut()).toByteArray();

        String json = new String(ba);

        //read the json string back into a Record
        JsonReader reader = new JsonReader();
        ctx = new IOContextBuilder(ctx)
                .in(new ByteArrayInputStream(json.getBytes()))
                .build();

        reader.open(ctx);
        Record record = reader.read();
        reader.close();

        //make sure the read record matches the original Record
        assertEquals(record, getSampleRecord());
    }



    @Test
    public void testWriteTypes() throws Exception{

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOContext ctx = new IOContextBuilder()
                .out(out)
                .build();

        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        writer.write(getComplexRecord());
        writer.close();
        String json = new String(out.toByteArray());

        //should be close to:
        //{"f":1.2,"d":1.3434,"b":true,"s":"test string","list":["a","b","c",1,2,3],"l":7474,"i":1, "map":{"entryNumber":1,"entry":"a"}}
//        {"l":7474,"f":1.2,"d":1.3434,"list":["a","b","c",1,2,3],"map.entry":"a","s":"test string","i":1,"b":true,"map.entryNumber":1}



        assertTrue(Pattern.compile("\"f\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"d\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"b\"[ ]*:[ ]*true").matcher(json).find());
        assertTrue(Pattern.compile("\"l\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"i\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"s\"[ ]*:[ ]*\"test string\"").matcher(json).find());
        System.out.println(json);

        assertTrue(Pattern.compile("\"list\"[ ]*:[ ]*\\[[0-9a-zA-Z,\"\\. ]+\\]").matcher(json).find());
        assertTrue(Pattern.compile("\"map\"[ ]*:[ ]*\\{[0-9a-zA-Z,\"\\.: ]+\\}").matcher(json).find());

    }



    @Test
    public void testPrettyPrint() throws Exception{

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOContext ctx = new IOContextBuilder()
                .attribute("pretty", "true")
                .out(out)
                .build();

        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        writer.write(getComplexRecord());
        writer.close();
        String json = new String(out.toByteArray());

        assertTrue(Pattern.compile("\"f\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"d\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"b\"[ ]*:[ ]*true").matcher(json).find());
        assertTrue(Pattern.compile("\"l\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"i\"[ ]*:[ ]*[0-9\\.]+").matcher(json).find());
        assertTrue(Pattern.compile("\"s\"[ ]*:[ ]*\"test string\"").matcher(json).find());
        assertTrue(Pattern.compile("\"list\"[ ]*:[ ]*\\[[0-9a-zA-Z,\"\\. ]+\\]").matcher(json).find());

        //not matching very much of the map here...
        assertTrue(Pattern.compile("\"map\"[ ]*:[ ]*\\{").matcher(json).find());
    }



    @Test
    public void testWriteWithSchema() throws Exception{

        Schema schema = SchemaFactory.buildSchema(this.getClass().getResourceAsStream("sample.schema"));
        IOContext ctx = new IOContextBuilder()
                .schema(schema)
                .in(this.getClass().getResourceAsStream("sample.json"))
                .build();

        JsonReader reader = new JsonReader();
        reader.open(ctx);

        Record record = reader.read();

        //add field that is not in schema
        record.put("skip", "not in schema");

        //change a field so the upper-case operation is run.
        record.put("source", "relevate");//lower-case

        //write the Record to json string.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx = new IOContextBuilder(ctx).out(out).build();
        JsonWriter writer = new JsonWriter();
        writer.open(ctx);
        writer.write(record);
        writer.close();
        String json = new String(out.toByteArray());

        System.out.println(json);

        //note upper case operation ran
        assertTrue(Pattern.compile("\"source\"[ ]*:[ ]*\"RELEVATE\"").matcher(json).find());

        //the skip field should have been removed
        assertFalse(Pattern.compile("\"skip\"").matcher(json).find());

    }


}
