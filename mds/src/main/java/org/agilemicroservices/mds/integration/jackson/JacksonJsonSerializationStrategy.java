package org.agilemicroservices.mds.integration.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agilemicroservices.mds.integration.SerializationStrategy;

import java.io.IOException;


// TODO move into client project and use globally
public class JacksonJsonSerializationStrategy implements SerializationStrategy {
    public static final JacksonJsonSerializationStrategy INSTANCE = new JacksonJsonSerializationStrategy();
    // TODO optimize
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T deserialize(String str, Class<T> clazz) {
        try {
            return objectMapper.readValue(str, clazz);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
