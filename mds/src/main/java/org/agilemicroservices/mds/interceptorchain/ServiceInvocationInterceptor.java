package org.agilemicroservices.mds.interceptorchain;


public interface ServiceInvocationInterceptor {

    ServiceInvocationResult doInvoke(Object message, ServiceInvocationContext context);
}