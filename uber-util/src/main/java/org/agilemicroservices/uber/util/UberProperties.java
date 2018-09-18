package org.agilemicroservices.uber.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class UberProperties {
    public static final UberProperties INSTANCE = new UberProperties();
    private Map<String, String> properties = new HashMap<>();

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public Set<String> nameSet() {
        return properties.keySet();
    }

    public Map<String, String> toMap() {
        return properties;
    }
}
