package com.realcomp.data.record.io.json;

import com.realcomp.data.DataType;
import com.realcomp.data.Operation;
import com.realcomp.data.conversion.ConversionException;
import com.realcomp.data.record.Record;
import com.realcomp.data.record.io.BaseRecordReaderWriter;
import com.realcomp.data.record.io.IOContext;
import com.realcomp.data.record.io.RecordWriter;
import com.realcomp.data.schema.Field;
import com.realcomp.data.schema.FieldList;
import com.realcomp.data.schema.SchemaException;
import com.realcomp.data.transform.TransformContext;
import com.realcomp.data.transform.Transformer;
import com.realcomp.data.transform.ValueSurgeon;
import com.realcomp.data.validation.ValidationException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.codehaus.jackson.util.MinimalPrettyPrinter;

/**
 *
 * @author krenfro
 */
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
        format.putDefault("singleObject", "true");

        jsonFactory = new JsonFactory();
        jsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        transformer = new Transformer();
        xCtx = new TransformContext();
        surgeon = new ValueSurgeon();
    }

    @Override
    public void write(Record record) throws IOException, ValidationException, ConversionException, SchemaException {

        if (record == null)
            throw new IllegalArgumentException("record is null");

        if (!beforeFirstOperationsRun){
            executeBeforeFirstOperations();
            beforeFirstOperationsRun = true;
        }

        if (context.getSchema() != null){
            //modify the record, performing all operations and keeping only the fields defined in the schema
            FieldList fields = context.getSchema().classify(record);
            transform(record, fields);
            filterFields(record, fields);
        }

        if (count > 0){
            json.writeRaw("\n");
        }
        writeJson(record.asSimpleMap());

        count++;
    }

    protected void transform(Record record, FieldList fields)
            throws ConversionException, ValidationException, SchemaException{

        assert(record != null);
        assert(context.getSchema() != null);

        transformer.setBefore(context.getSchema().getBeforeOperations());
        transformer.setAfter(context.getSchema().getAfterOperations());
        transformer.setFields(fields);
        xCtx.setRecord(record);
        transformer.transform(xCtx);
    }

    protected void filterFields(Record record, FieldList fields){

        Set<String> filter = new HashSet<>();
        Set<String> keep = new HashSet<>();
        for (Field field: fields)
            keep.add(field.getName());

        filter.addAll(record.keySet());
        filter.removeAll(keep);

        for (String f: filter){
            record.remove(f);
        }
    }


    private void writeJson(Map<String,Object> map)
            throws ValidationException, ConversionException, IOException{

        json.writeStartObject();

        for (Map.Entry<String,Object> entry: map.entrySet()){
            writeJson(entry.getKey(), entry.getValue());
        }

        json.writeEndObject();
    }

    /**
     *
     * @param map
     * @return true if the map is not null and contains a list or a map.
     */
    private boolean isDeepMap(Map<String,Object> map){

        boolean deep = true;
        if (map != null) {
            for (Object entry : (List) map) {
                if (entry != null) {
                    DataType entryType = DataType.getDataType(entry);
                    if (entryType == DataType.LIST || entryType == DataType.MAP) {
                        deep = false;
                        break;
                    }
                }
            }
        }
        return deep;
    }

    private void writeJson(String name, Object value)
            throws IOException, ValidationException, ConversionException{


        if (value == null){
            json.writeFieldName(name);
            json.writeNull();
        }
        else{

            DataType type = DataType.getDataType(value);
            switch(type){
                case MAP:
                    json.writeFieldName(name);
                    writeJson((Map<String,Object>) value);
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

        assert(value != null);
        assert(type != null);

        switch(type){
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
                writeJson((Map<String,Object>) value);
                break;
            case LIST:
                for (Object entry: (List) value){
                    writeJson(entry, DataType.getDataType(entry));
                }
                break;
        }

    }


    @Override
    public void close(boolean closeIOContext) {

        try {
            executeAfterLastOperations();
        }
        catch (ValidationException | ConversionException ex) {
            logger.log(Level.WARNING, null, ex);
        }

        if (json != null){
            try {
                if (isSingleObject()){
                    json.writeEndArray();
                }

                json.close();
            }
            catch (IOException ex) {
                logger.log(Level.WARNING, null, ex);
            }
        }

        super.close(closeIOContext);
    }

    @Override
    public void open(IOContext context) throws IOException, SchemaException {

        super.open(context);
        if (context.getOut() == null)
            throw new IllegalArgumentException("Invalid IOContext. No OutputStream specified");

        json = jsonFactory.createJsonGenerator(context.getOut(), JsonEncoding.UTF8);
        if (isPretty()){
            json.setPrettyPrinter(new DefaultPrettyPrinter());
        }


        if (isSingleObject()){
            json.writeStartArray();
        }
    }


    @Override
    protected void executeAfterLastOperations() throws ValidationException, ConversionException{

        if (context != null && context.getSchema() != null){
            List<Operation> operations = context.getSchema().getAfterLastOperations();
            if (operations != null && !operations.isEmpty()){
                xCtx.setRecordCount(this.getCount());
                surgeon.operate(operations, xCtx);
            }
        }


    }

    @Override
    protected void executeBeforeFirstOperations() throws ValidationException, ConversionException{

         if (context != null &&context.getSchema() != null){
            List<Operation> operations = context.getSchema().getBeforeFirstOperations();
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
