package org.agilemicroservices.mds;


import javax.jms.Session;


public class OnewayMessageDrivenServiceFactory implements MessageDrivenServiceFactory {
    private Class<? extends MessageDrivenService> serviceClass;


    public OnewayMessageDrivenServiceFactory(Class<? extends MessageDrivenService> serviceClass) {
        this.serviceClass = serviceClass;
    }


    @Override
    public MessageDrivenService create(Session session) {
        try {
            MessageDrivenService service = serviceClass.newInstance();
            service.setSession(session);
            return service;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate service.", e);
        }
    }
}
