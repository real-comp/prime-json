package com.realcomp.data.json;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.File;
import com.realcomp.data.schema.SchemaField;
import com.realcomp.data.schema.FileSchema;
import com.realcomp.data.conversion.ConversionException;
import com.realcomp.data.schema.SchemaException;
import com.realcomp.data.validation.ValidationException;
import java.io.IOException;
import com.realcomp.data.record.Record;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author krenfro
 */
public class JsonRecordWriterTest {
    
    public JsonRecordWriterTest() {
    }
    
    
    protected Record getRecord(){
        
        Record record = new Record();
        record.put("a", "asdf");
        
        List<String> list = new ArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        record.put("b", list);
        
        
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("asdf","9834939");
        map.put("true", Boolean.valueOf(true));
        record.put("map", map);
        
        return record;
        
    }
    
    public FileSchema getSchema() throws SchemaException{
        
        FileSchema schema = new FileSchema();
        schema.addField(new SchemaField("a"));
        return schema;
    }
    
    @Test
    public void testWriter() throws IOException, ValidationException, ConversionException, SchemaException{
        
        JsonRecordWriter writer = new JsonRecordWriter();
        writer.setSchema(getSchema());
        
        try{
            writer.open(System.out);
            Record record = getRecord();
            writer.write(record);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            writer.close();
        }
        
    }

}
