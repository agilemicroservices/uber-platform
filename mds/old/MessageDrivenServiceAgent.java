package org.agilemicroservices.mds;

import org.agrona.concurrent.Agent;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;


class MessageDrivenServiceAgent implements Agent {
    private static final int DEFAULT_MAX_MESSAGE_LIMIT = 100;
    private MessageDrivenService service;
    private Session session;
    private MessageConsumer consumer;


    MessageDrivenServiceAgent(MessageDrivenService service, Session session, MessageConsumer consumer) {
        this.service = service;
        this.session = session;
        this.consumer = consumer;
    }


    @Override
    public int doWork() throws Exception {
        int workCount = 0;

        for (; workCount < DEFAULT_MAX_MESSAGE_LIMIT; workCount++) {
            Message message = consumer.receiveNoWait();
            if (null == message) {
                break;
            }
            service.onMessage(message);
        }

        if (workCount > 0) {
            service.endOfBatch();
            session.commit();
        }

        return workCount;
    }

    @Override
    public void onClose() {
        service.onDestroy();
    }

    @Override
    public String roleName() {
        return "Container - " + service.getClass().getSimpleName();
    }
}
