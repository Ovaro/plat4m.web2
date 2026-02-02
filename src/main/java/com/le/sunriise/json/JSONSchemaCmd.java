package com.le.sunriise.json;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.le.sunriise.mnyobject.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONSchemaCmd {

    private static final Logger log = LoggerFactory.getLogger(JSONSchemaCmd.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();

        JsonSchema jsonSchema = null;
        try {
            jsonSchema = mapper.generateJsonSchema(Category.class);
            System.out.println(jsonSchema.toString());
        } catch (JsonMappingException e) {
            log.warn("" + e);
        } finally {}
    }
}
