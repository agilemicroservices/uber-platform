package org.agilemicroservices.mds.interceptorchain;


public interface ServiceInvocationResult {

    /**
     * @return <code>true</code> if the underlying invocation completed without throwing an exception,
     * <code>false</code> otherwise.
     */
    boolean isSuccess();

    boolean hasReturnValue();

    /**
     * @return the value returned by the message handler.
     * @throws IllegalStateException if <code>hasReturnValue()</code> returns <code>false</code>.
     */
    Object getReturnValue();

    /**
     * @return the exception thrown by the message handler.
     * @throws IllegalStateException if <code>isSuccess()</code> returns <code>true</code>.
     */
    Throwable getCause();
}
