package com.workoutplanner.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserializes an Integer field that may be sent as either a scalar integer
 * or a JSON array (in which case the array length is used as the value).
 *
 * This handles the case where the frontend sends sets as an array of set
 * objects (e.g. [{reps:10,weight:50},{reps:10,weight:50}]) but the backend
 * stores it as a count (2).
 */
public class IntegerFromArraySizeDeserializer extends StdDeserializer<Integer> {

    public IntegerFromArraySizeDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return p.getIntValue();
        }
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        if (p.currentToken() == JsonToken.START_ARRAY) {
            int count = 0;
            while (p.nextToken() != JsonToken.END_ARRAY) {
                // skip nested objects/values
                if (p.currentToken() == JsonToken.START_OBJECT) {
                    p.skipChildren();
                }
                count++;
            }
            return count;
        }
        // fallback: try to parse as string
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            if (text.isEmpty()) return null;
            return Integer.parseInt(text);
        }
        return (Integer) ctx.handleUnexpectedToken(Integer.class, p);
    }
}
