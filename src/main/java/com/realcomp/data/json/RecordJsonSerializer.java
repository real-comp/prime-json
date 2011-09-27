package com.realcomp.data.json;

import com.realcomp.data.record.Record;
import java.io.IOException;
import java.util.Map;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 *
 * @author krenfro
 */
public class RecordJsonSerializer extends JsonSerializer<Record>{


    @Override
    public void serialize(Record record, JsonGenerator jgen, SerializerProvider provider) 
            throws IOException, JsonProcessingException {
 
        jgen.writeStartObject();
        for (Map.Entry<String,Object> entry: record.entrySet())
            jgen.writeObjectField(entry.getKey(), entry.getValue());
        jgen.writeEndObject();
    }

    @Override
    public Class<Record> handledType() {
        return Record.class;
    }
    
}
