package org.agilemicroservices.mds.integration.spring;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;


public class UberPropertySourceFactory implements PropertySourceFactory {
    private static final String CLASS_NAME = "org.agilemicroservices.uber.util.UberProperties";

    @Override
    @SuppressWarnings("unchecked")
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        Map<String, Object> properties;

        // reference UberProperties via reference to avoid direct reference, avoiding inclusion in dependent shadow jars
        try {
            Class<?> uberPropertiesClass = Class.forName(CLASS_NAME);
            Field instanceField = uberPropertiesClass.getField("INSTANCE");
            Object uberProperties = instanceField.get(null);
            Method toMapMethod = uberPropertiesClass.getMethod("toMap");
            properties = (Map<String,Object>) toMapMethod.invoke(uberProperties);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(CLASS_NAME + " class not on classpath.", e);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Invalid UberProperties class.", e);
        }

        return new MapPropertySource(name, properties);
    }
}
