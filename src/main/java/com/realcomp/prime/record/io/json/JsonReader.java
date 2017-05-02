package com.realcomp.prime.record.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.realcomp.prime.Operation;
import com.realcomp.prime.conversion.ConversionException;
import com.realcomp.prime.record.Record;
import com.realcomp.prime.record.io.BaseRecordReaderWriter;
import com.realcomp.prime.record.io.IOContext;
import com.realcomp.prime.record.io.RecordReader;
import com.realcomp.prime.schema.Field;
import com.realcomp.prime.schema.SchemaException;
import com.realcomp.prime.transform.TransformContext;
import com.realcomp.prime.transform.ValueSurgeon;
import com.realcomp.prime.validation.ValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The JSON format is rich enough that a Schema is <i>not</i> required to parse a Record.
 * If a schema <i>is</i> specified, only the fields specified in the schema will appear in the Record.
 * <p/>
 * Will read a single JSON object or an array of JSON objects.
 *
 * @author krenfro
 */
public class JsonReader extends BaseRecordReaderWriter implements RecordReader{

    protected JsonFactory jsonFactory;
    protected JsonParser jsonParser;
    protected ValueSurgeon surgeon;
    protected TransformContext transformContext;

    public JsonReader(){
        super();
        format.putDefault("type", "JSON");
        format.putDefault("pretty", "false");
        format.putDefault("singleObject", "false");
        jsonFactory = new JsonFactory();
        surgeon = new ValueSurgeon();
        transformContext = new TransformContext();
    }

    @Override
    public Record read() throws IOException, ValidationException, ConversionException, SchemaException{
        if (!beforeFirstOperationsRun){
            executeBeforeFirstOperations();
            beforeFirstOperationsRun = true;
        }
        Record record = null;
        moveToNextObject();
        Map map = parseMap();

        if (map != null){
            if (schema == null){
                record = new Record(map);
            }
            else{
                /* Since a schema is defined, only put the fields defined in the schema into the final record.
                 * The operations should be able to find values in the more complete Record parsed from
                 * the raw json.  Create a temporary Record from the parsed json, and use that for the
                 * field creation.
                 */
                record = new Record();
                Record temp = new Record(map);
                transformContext.setRecord(temp);
                for (Field field : schema.classify(temp)){
                    transformContext.setKey(field.getName());
                    Object value = surgeon.operate(getOperations(field), transformContext);
                    if (value != null){
                        //Write the results of the operations to both the final Record, and the
                        // temporary Record for subsequent field creation.
                        record.put(field.getName(), field.getType().coerce(value));
                        temp.put(field.getName(), field.getType().coerce(value));
                    }
                }
            }
            count++;
        }
        else{
            executeAfterLastOperations();
        }

        return record;
    }

    private List<Operation> getOperations(Field field){
        assert (field != null);
        assert (schema != null);
        List<Operation> operations = new ArrayList<>();
        if (schema.getBeforeOperations() != null){
            operations.addAll(schema.getBeforeOperations());
        }
        operations.addAll(field.getOperations());
        if (schema.getAfterOperations() != null){
            operations.addAll(schema.getAfterOperations());
        }
        return operations;
    }

    private void moveToNextObject() throws IOException{
        JsonToken token = jsonParser.nextToken();
        if (token == JsonToken.START_ARRAY){
            //more than one record in the input stream
            moveToNextObject();
        }
        else if (token == JsonToken.START_OBJECT){
            //ready - probably only one json object in the input stream
        }
    }

    private Map parseMap() throws IOException{
        Map map = null;
        if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT){

            map = new HashMap();
            while (jsonParser.nextToken() != JsonToken.END_OBJECT){

                JsonToken token = jsonParser.getCurrentToken();

                if (token == JsonToken.START_ARRAY){
                    //nested list
                    map.put(jsonParser.getCurrentName(), parseList());
                }
                else if (token == JsonToken.START_OBJECT){
                    //nested map
                    map.put(jsonParser.getCurrentName(), parseMap());
                }
                else if (token == JsonToken.VALUE_TRUE){
                    map.put(jsonParser.getCurrentName(), Boolean.TRUE);
                }
                else if (token == JsonToken.VALUE_FALSE){
                    map.put(jsonParser.getCurrentName(), Boolean.FALSE);
                }
                else if (token == JsonToken.VALUE_STRING){
                    //TODO: charset being used!
                    map.put(jsonParser.getCurrentName(), jsonParser.getText());
                }
                else if (token == JsonToken.VALUE_NUMBER_FLOAT){
                    try{
                        map.put(jsonParser.getCurrentName(), Float.valueOf(jsonParser.getFloatValue()));
                    }
                    catch (JsonParseException ex){
                        map.put(jsonParser.getCurrentName(), Double.valueOf(jsonParser.getDoubleValue()));
                    }
                }
                else if (token == JsonToken.VALUE_NUMBER_INT){
                    try{
                        map.put(jsonParser.getCurrentName(), Integer.valueOf(jsonParser.getIntValue()));
                    }
                    catch (JsonParseException ex){
                        map.put(jsonParser.getCurrentName(), Long.valueOf(jsonParser.getLongValue()));
                    }
                }
                else if (token == JsonToken.VALUE_NULL){
                    //skip
                }
            }
        }

        return map;
    }

    private List parseList() throws IOException{
        List list = null;
        if (jsonParser.getCurrentToken() == JsonToken.START_ARRAY){

            list = new ArrayList();
            while (jsonParser.nextToken() != JsonToken.END_ARRAY){

                JsonToken token = jsonParser.getCurrentToken();

                if (token == JsonToken.START_ARRAY){
                    //nested list
                    list.add(parseList());
                }
                else if (token == JsonToken.START_OBJECT){
                    //nested map
                    list.add(parseMap());
                }
                else if (token == JsonToken.VALUE_TRUE){
                    list.add(Boolean.TRUE);
                }
                else if (token == JsonToken.VALUE_FALSE){
                    list.add(Boolean.FALSE);
                }
                else if (token == JsonToken.VALUE_NULL){
                    //skip
                }
                else if (token == JsonToken.VALUE_STRING){
                    //TODO: charset being used!
                    list.add(jsonParser.getText());
                }
                else if (token == JsonToken.VALUE_NUMBER_FLOAT){
                    //TODO: charset being used!
                    list.add(Float.valueOf(jsonParser.getFloatValue()));
                }
                else if (token == JsonToken.VALUE_NUMBER_INT){
                    //TODO: charset being used!
                    list.add(Integer.valueOf(jsonParser.getIntValue()));
                }
            }
        }

        return list;
    }

    @Override
    public void open(IOContext context) throws IOException, SchemaException{
        super.open(context);
        if (context.getIn() == null){
            throw new IllegalArgumentException("Invalid IOContext. No InputStream specified");
        }
        jsonParser = jsonFactory.createJsonParser(context.getIn());
        transformContext.setValidationExceptionThreshold(context.getValidationExeptionThreshold());
        transformContext.setSchema(schema);
    }

    @Override
    public void close(boolean closeIOContext) throws IOException{
        super.close(closeIOContext);
        if (jsonParser != null){
            try{
                jsonParser.close();
            }
            catch (IOException ex){
                Logger.getLogger(JsonReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
