package org.agilemicroservices.mds.client;

import java.lang.reflect.Method;


class ServiceInvocation {
    private long correlationId;
    private Method method;
    private String body;


    public ServiceInvocation(long correlationId, Method method, String body) {
        this.correlationId = correlationId;
        this.method = method;
        this.body = body;
    }


    public long getCorrelationId() {
        return correlationId;
    }

    public Method getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }
}