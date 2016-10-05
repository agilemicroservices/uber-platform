package org.agilemicroservices.mds.integration;


public interface SerializationStrategy {

    String serialize(Object value);

    <T> T deserialize(String str, Class<T> clazz);
}
