package org.agilemicroservices.mds.util;

import org.agilemicroservices.mds.annotation.JmsInbound;
import org.agilemicroservices.mds.annotation.JmsOutbound;

import java.lang.reflect.Method;


public final class ServiceMethod {
    private boolean isOneWay = true;
    private DestinationUri inboundUri;
    private DestinationUri outboundUri;
    private Method method;
    private Class<?> messageClass;
    private String scope;
    private String format;


    ServiceMethod(Method method) {
        this.method = method;
        if (method.getParameterTypes().length > 0) {
            messageClass = method.getParameterTypes()[0];
        }

        JmsInbound inbound = method.getAnnotation(JmsInbound.class);
        JmsOutbound outbound = method.getAnnotation(JmsOutbound.class);

        isOneWay = method.getReturnType() == void.class;

        inboundUri = new DestinationUri(inbound.value());
        if (null != outbound) {
            outboundUri = new DestinationUri(outbound.value());
        }

        scope = inbound.scope();
        format = inbound.format();
    }


    public boolean isOneWay() {
        return isOneWay;
    }

    public DestinationUri getInboundUri() {
        return inboundUri;
    }

    public DestinationUri getOutboundUri() {
        return outboundUri;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getMessageClass() {
        return messageClass;
    }

    public String getScope() {
        return scope;
    }

    public String getFormat() {
        return format;
    }


    @Override
    public String toString() {
        return "(" + (messageClass == null ? "" : messageClass.getSimpleName()) + ")" +
                "->" +
                method.getDeclaringClass().getSimpleName() +
                '.' +
                method.getName() +
                "; scope=" +
                scope;
    }
}
