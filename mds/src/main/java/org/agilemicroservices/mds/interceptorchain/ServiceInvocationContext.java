package org.agilemicroservices.mds.interceptorchain;


public interface ServiceInvocationContext {

    ServiceInvocationResult invoke(Object message);
}