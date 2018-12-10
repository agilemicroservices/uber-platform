package org.agilemicroservices.mds;

import javax.jms.Message;


public interface AuthHandler {

    void handleMessage(Message message) throws Exception;
}