package org.agilemicroservices.mds;

import javax.jms.Session;


@FunctionalInterface
public interface MessageDrivenServiceFactory {

    MessageDrivenService create(Session session);
}
