package org.agilemicroservices.mds.interceptorchain;


public class InterceptorChain {
    private static final ServiceInvocationInterceptor HEAD_INTERCEPTOR = (arguments, context) -> context.invoke(arguments);
    private final Node head = new Node(HEAD_INTERCEPTOR);


    public void add(ServiceInvocationInterceptor interceptor) {
        head.add(new Node(interceptor));
    }

    public ServiceInvocationResult doInvoke(Object message) {
        return head.doInvoke(message);
    }


    private class Node implements ServiceInvocationContext {
        private ServiceInvocationInterceptor interceptor;
        private Node next;


        private Node(ServiceInvocationInterceptor interceptor) {
            this.interceptor = interceptor;
        }


        private void add(Node node) {
            if (null == next) {
                next = node;
            } else {
                next.add(node);
            }
        }


        private ServiceInvocationResult doInvoke(Object message) {
            return interceptor.doInvoke(message, this);
        }

        @Override
        public ServiceInvocationResult invoke(Object message) {
            return next.doInvoke(message);
        }
    }
}
