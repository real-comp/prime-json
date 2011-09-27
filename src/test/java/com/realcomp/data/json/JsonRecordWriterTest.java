package com.realcomp.data.json;

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
        record.put("b", 1);
        record.put("c", 1.1f);
        record.put("d", 2);
        record.put("e", 2.2d);
        record.put("f", true);
        
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
            writer.write("asdf");
            //writer.write(getRecord());
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            writer.close();
        }
        
    }

}
