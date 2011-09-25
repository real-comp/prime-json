package com.realcomp.data.json;

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
        record.put("String", "asdf");
        record.put("Integer", 1);
        record.put("Float", 1.1f);
        record.put("Long", 2);
        record.put("Double", 2.2d);
        record.put("Boolean", true);
        
        return record;
        
    }
    
    public FileSchema getSchema() throws SchemaException{
        
        FileSchema schema = new FileSchema();
        schema.addField(new SchemaField("String"));
        return schema;
    }
    
    @Test
    public void testWriter() throws IOException, ValidationException, ConversionException, SchemaException{
        
        JsonRecordWriter writer = new JsonRecordWriter();
        writer.setSchema(getSchema());
        
        writer.open(System.out);
        writer.write(getRecord());
        writer.close();
        
    }

}
