package org.agilemicroservices.mds;

import javax.jms.Session;


public interface SessionAware {

    void setSession(Session session);
}
