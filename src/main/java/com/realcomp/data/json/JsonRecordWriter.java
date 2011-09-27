package com.realcomp.data.json;

import com.realcomp.data.conversion.ConversionException;
import com.realcomp.data.record.Record;
import com.realcomp.data.record.writer.BaseFileWriter;
import com.realcomp.data.schema.SchemaField;
import com.realcomp.data.validation.ValidationException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 *
 * @author krenfro
 */
public class JsonRecordWriter extends BaseFileWriter{

    ObjectMapper jackson;
    
    public JsonRecordWriter(){
        super();
        jackson = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        jackson.getDeserializationConfig().withAnnotationIntrospector(introspector);
        jackson.getSerializationConfig().withAnnotationIntrospector(introspector);
    }

    @Override
    protected void write(Record record, List<SchemaField> fields)
            throws ValidationException, ConversionException, IOException{

        if (count > 0){
            out.write(",".getBytes(charset));
        }
        
        jackson.writeValue(out, record);
    }

        
    
    @Override
    public void open(OutputStream out, Charset charset) throws IOException{
       
        super.open(out, charset);
        out.write("[".getBytes(charset));
    }


    @Override
    public void close(){
        try {
            out.flush();
            out.write("asdfafsdasdf".getBytes(charset));
            out.write("]".getBytes(charset));
            out.flush();
        } 
        catch (IOException ignored) {
        }
        super.close();
    }

    
    @Override
    protected void write(Record record, SchemaField field) 
            throws ValidationException, ConversionException, IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
