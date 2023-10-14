package com.odysseusinc.arachne.datanode.environment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.function.Function;
import org.springframework.core.serializer.support.SerializationFailedException;

public class SerializationUtils {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException(e.getMessage(), e);
        }
    }

    public static <T> Function<String,T> deserialize(Class<T> deserializableClass) {
        return value -> {
            try {
                return objectMapper.readValue(value, deserializableClass);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(e.getMessage(), e);
            }
        };
    }

    public static <T> Function<String,T> deserialize(TypeReference<T> deserializableClass) {
        return value -> {
            try {
                return objectMapper.readValue(value, deserializableClass);
            } catch (JsonProcessingException e) {
                throw new SerializationFailedException(e.getMessage(), e);
            }
        };
    }

    public static <T> Function<URL,T> deserializeByUrl(Class<T> deserializableClass) {
        return value -> {
            try {
                return objectMapper.readValue(value, deserializableClass);
            } catch (IOException e) {
                throw new SerializationFailedException(e.getMessage(), e);
            }
        };
    }

    public static JsonParser traverse(JsonNode jsonNode) throws IOException {
        JsonParser parser = jsonNode.traverse();
        parser.nextToken();
        return parser;
    }

}
