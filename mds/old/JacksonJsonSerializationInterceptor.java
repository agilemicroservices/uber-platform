package org.agilemicroservices.mds;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;


public class JacksonJsonSerializationInterceptor implements ServiceInvocationInterceptor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private Class<?> messageClass;


    public JacksonJsonSerializationInterceptor(Class<?> messageClass) {
        this.messageClass = messageClass;
    }


    @Override
    public ServiceInvocationResult doInvoke(Object message, ServiceInvocationContext context) {
        try {
            String str = ((TextMessage) message).getText();
            Object deserialized = OBJECT_MAPPER.readValue(str, messageClass);
            return context.invoke(deserialized);
        } catch (JMSException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
