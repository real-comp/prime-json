package com.realcomp.prime.record.io.json;

import com.realcomp.prime.record.Record;
import com.realcomp.prime.record.io.IOContext;
import com.realcomp.prime.record.io.IOContextBuilder;
import com.realcomp.prime.record.io.RecordReaderFactory;
import com.realcomp.prime.schema.SchemaFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class JsonReaderTest {

    static{
        RecordReaderFactory.registerReader("JSON", JsonReader.class.getName());
    }

    public JsonReaderTest() {
    }


    public String getJsonTestString1(){
        return "{\"attributes\":{\"valueDescription\":\"2011 Preliminary\",\"buildDate\":\"20111031\"},\"rawAddress\":{\"address\":[\"1028 HWY 3\"],\"state\":\"TX\",\"city\":\"LA MARQUE\",\"zip\":\"77568\",\"fips\":\"48267\"},\"exemptions\":[],\"owners\":[{\"name\":{\"first\":\"FELIPE\",\"last\":\"ATONAL\",\"salutation\":\"FELIPE\"},\"rawAddress\":{\"address\":[\"1028 HWY 3 LOT 17\"],\"state\":\"TX\",\"city\":\"LA MARQUE\",\"zip\":\"77568\"},\"percentOwnership\":100.0}],\"landSegments\":[],\"improvements\":[{\"description\":\"MOBILE HOME\",\"stories\":1.0,\"details\":[{\"description\":\"OUT BUILDINGS\"},{\"type\":\"MAIN_AREA\",\"description\":\"MAIN AREA\",\"sqft\":952.0}],\"sketchCommands\":\"NV!NV\"}],\"deedDate\":\"20051019\",\"agriculturalValue\":0,\"cadGeographicId\":\"101131\",\"cadPropertyId\":\"M279227\",\"landValue\":0,\"legalDescription\":\"SHADY OAKS MHPK-LM, SPACE 17, SERIAL # JE3107A, TITLE # 00146824, LABEL # TEX0122548, LIFESTYLE BAYSHORE 1980 14X68 CRM/TAN/BRN\",\"subdivision\":\"\",\"totalImprovementSqft\":952,\"totalLandAcres\":0.0,\"totalValue\":7810}";
    }


    private Record getSimpleRecord(){

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
        record.put("zip", "78717");
        record.put("address", "8665 EPHRAIM RD");
        record.put("userId", "73783");
        record.put("orderId", "299");
        record.put("product", "ALLSTATE AUTO SPECIFIC");
        record.put("source", "relevate");
        record.put("usedDate", "2011-09-21");
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        record.put("list", list);
        return record;
    }


    @Test
    public void testSimpleSerialization() throws Exception{

        ObjectMapper jackson = new ObjectMapper();
        Record expected = getSimpleRecord();
        String json = jackson.writeValueAsString(expected);
        System.out.println(json);
        Record actual = jackson.readValue(json, Record.class);
        assertEquals(expected, actual);

        expected = getComplexRecord();
        json = jackson.writeValueAsString(expected);
        System.out.println(json);
        actual = jackson.readValue(json, Record.class);
        assertEquals(expected, actual);

    }

    @Test
    public void testReadFromString() throws Exception{

        IOContext ctx = new IOContextBuilder()
                .in(new ByteArrayInputStream(getJsonTestString1().getBytes())).build();
        JsonReader reader = new JsonReader();
        reader.open(ctx);
        Record record = reader.read();
        assertNotNull(record);
        assertEquals(1, reader.getCount());
        assertEquals(7810, record.get("totalValue"));
        reader.close();
    }


    @Test
    public void testReadFromFile() throws Exception{

        IOContext ctx = new IOContextBuilder()
                .in(this.getClass().getResourceAsStream("sample.json")).build();
        JsonReader reader = new JsonReader();
        reader.open(ctx);

        Record record = reader.read();
        assertNotNull(record);
        assertEquals(1, reader.getCount());
        assertEquals("relevate", record.get("source"));

        assertNull(reader.read());
        assertNull(reader.read());
        reader.close();


        Record sample = getSimpleRecord();
        assertEquals(record, sample);


        reader = new JsonReader();
        ctx = new IOContextBuilder(ctx)
                .in(this.getClass().getResourceAsStream("multiRecordSample.json")).build();

        reader.open(ctx);
        record = reader.read();
        assertNotNull(record);
        assertEquals(1, reader.getCount());
        assertEquals("8665 EPHRAIM RD", record.get("address"));

        record = reader.read();
        assertNotNull(record);
        assertEquals(2, reader.getCount());
        assertEquals("8666 EPHRAIM RD", record.get("address"));


        assertNull(reader.read());
        reader.close();
    }



    @Test
    public void testReadMultipleFromFile() throws Exception{

        IOContext ctx = new IOContextBuilder()
                .in(this.getClass().getResourceAsStream("sample2.json")).build();
        JsonReader reader = new JsonReader();
        reader.open(ctx);

        Record record = reader.read();
        assertNotNull(record);
        assertEquals(1, reader.getCount());
        assertEquals("relevate", record.get("source"));

        assertNotNull(reader.read());
        assertEquals(2, reader.getCount());
        assertNull(reader.read());
        reader.close();

    }

   
    @Test
    public void testWithSchema() throws Exception{

        IOContext ctx = new IOContextBuilder()
                .schema(SchemaFactory.buildSchema(this.getClass().getResourceAsStream("sample.schema")))
                .in(this.getClass().getResourceAsStream("sample.json"))
                .build();


        JsonReader reader = new JsonReader();
        reader.open(ctx);

        Record record = reader.read();
        assertNotNull(record);
        assertEquals(1, reader.getCount());
        assertEquals("RELEVATE", record.get("source")); //upper-case converter

        assertEquals(null, record.get("asdf"));
        assertEquals("78717", record.get("doesnotexistinjson"));

        assertNull(reader.read());
        assertNull(reader.read());
        reader.close();

    }



}
