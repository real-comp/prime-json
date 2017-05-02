package com.realcomp.prime.record.io.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.realcomp.prime.DataType;
import com.realcomp.prime.Operation;
import com.realcomp.prime.conversion.ConversionException;
import com.realcomp.prime.record.Record;
import com.realcomp.prime.record.io.BaseRecordReaderWriter;
import com.realcomp.prime.record.io.IOContext;
import com.realcomp.prime.record.io.RecordWriter;
import com.realcomp.prime.schema.Field;
import com.realcomp.prime.schema.FieldList;
import com.realcomp.prime.schema.SchemaException;
import com.realcomp.prime.transform.TransformContext;
import com.realcomp.prime.transform.Transformer;
import com.realcomp.prime.transform.ValueSurgeon;
import com.realcomp.prime.validation.ValidationException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JsonWriter extends BaseRecordReaderWriter implements RecordWriter{

    private static final Logger logger = Logger.getLogger(JsonWriter.class.getName());

    protected JsonFactory jsonFactory;
    protected JsonGenerator json;
    protected Transformer transformer;
    protected TransformContext xCtx;
    protected ValueSurgeon surgeon;

    public JsonWriter(){

        super();
        format.putDefault("pretty", "false");
        format.putDefault("type", "JSON");
        format.putDefault("singleObject", "false");

        jsonFactory = new JsonFactory();
        jsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        transformer = new Transformer();
        xCtx = new TransformContext();
        surgeon = new ValueSurgeon();
    }

    @Override
    public void write(Record record) throws IOException, ValidationException, ConversionException, SchemaException{

        if (record == null){
            throw new IllegalArgumentException("record is null");
        }
        if (!beforeFirstOperationsRun){
            executeBeforeFirstOperations();
            beforeFirstOperationsRun = true;
        }
        if (schema != null){
            //modify the record, performing all operations and keeping only the fields defined in the schema
            FieldList fields = schema.classify(record);
            transform(record, fields);
            filterFields(record, fields);
        }
        writeJson(record.asSimpleMap());
        if (!isSingleObject()){
            json.writeRaw("\n");
        }

        count++;
    }

    protected void transform(Record record, FieldList fields)
            throws ConversionException, ValidationException, SchemaException{

        assert (record != null);
        assert (schema != null);
        transformer.setBefore(schema.getBeforeOperations());
        transformer.setAfter(schema.getAfterOperations());
        transformer.setFields(fields);
        xCtx.setRecord(record);
        transformer.transform(xCtx);
    }

    protected void filterFields(Record record, FieldList fields){
        Set<String> filter = new HashSet<>();
        Set<String> keep = new HashSet<>();
        for (Field field : fields){
            keep.add(field.getName());
        }
        filter.addAll(record.keySet());
        filter.removeAll(keep);
        for (String f : filter){
            record.remove(f);
        }
    }


    private void writeJson(Map<String, Object> map)  throws ValidationException, ConversionException, IOException{
        json.writeStartObject();
        for (Map.Entry<String, Object> entry : map.entrySet()){
            writeJson(entry.getKey(), entry.getValue());
        }
        json.writeEndObject();
    }


    private void writeJson(String name, Object value)
            throws IOException, ValidationException, ConversionException{

        if (value == null){
            json.writeFieldName(name);
            json.writeNull();
        }
        else{
            DataType type = DataType.getDataType(value);
            switch (type){
                case MAP:
                    json.writeFieldName(name);
                    writeJson((Map<String, Object>) value);
                    break;
                case LIST:
                    json.writeArrayFieldStart(name);
                    writeJson(value, type);
                    json.writeEndArray();
                    break;
                default:
                    json.writeFieldName(name);
                    writeJson(value, type);
            }
        }
    }


    private void writeJson(Object value, DataType type)
            throws IOException, ValidationException, ConversionException{

        assert (value != null);
        assert (type != null);

        switch (type){
            case STRING:
                json.writeString((String) value);
                break;
            case INTEGER:
                json.writeNumber((Integer) value);
                break;
            case LONG:
                json.writeNumber((Long) value);
                break;
            case FLOAT:
                json.writeNumber((Float) value);
                break;
            case DOUBLE:
                json.writeNumber((Double) value);
                break;
            case BOOLEAN:
                json.writeBoolean((Boolean) value);
                break;
            case MAP:
                writeJson((Map<String, Object>) value);
                break;
            case LIST:
                for (Object entry : (List) value){
                    writeJson(entry, DataType.getDataType(entry));
                }
                break;
        }
    }


    @Override
    public void close(boolean closeIOContext) throws IOException{

        try{
            executeAfterLastOperations();
        }
        catch (ValidationException | ConversionException ex){
            logger.log(Level.WARNING, null, ex);
        }

        if (json != null){
            try{
                if (isSingleObject()){
                    json.writeEndArray();
                }

                json.close();
            }
            catch (IOException ex){
                logger.log(Level.WARNING, null, ex);
            }
        }

        super.close(closeIOContext);
    }

    @Override
    public void open(IOContext context) throws IOException, SchemaException{
        super.open(context);
        if (context.getOut() == null){
            throw new IllegalArgumentException("Invalid IOContext. No OutputStream specified");
        }
        json = jsonFactory.createJsonGenerator(context.getOut(), JsonEncoding.UTF8);
        if (isPretty()){
            json.setPrettyPrinter(new DefaultPrettyPrinter());
        }
        else{
            json.setPrettyPrinter(new MinimalPrettyPrinter(""));
        }
        if (isSingleObject()){
            json.writeStartArray();
        }
    }


    @Override
    protected void executeAfterLastOperations() throws ValidationException, ConversionException{
        if (context != null && schema != null){
            List<Operation> operations = schema.getAfterLastOperations();
            if (operations != null && !operations.isEmpty()){
                xCtx.setRecordCount(this.getCount());
                surgeon.operate(operations, xCtx);
            }
        }
    }

    @Override
    protected void executeBeforeFirstOperations() throws ValidationException, ConversionException{
        if (context != null && schema != null){
            List<Operation> operations = schema.getBeforeFirstOperations();
            if (operations != null && !operations.isEmpty()){
                xCtx.setRecordCount(this.getCount());
                surgeon.operate(operations, xCtx);
            }
        }
    }


    public boolean isPretty(){
        return Boolean.parseBoolean(format.get("pretty"));
    }

    public boolean isSingleObject(){
        return Boolean.parseBoolean(format.get("singleObject"));
    }

}
