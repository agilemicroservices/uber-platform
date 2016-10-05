package org.agilemicroservices.mds;

import org.agilemicroservices.mds.integration.Transaction;
import org.agilemicroservices.mds.integration.SerializationStrategy;
import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.agrona.concurrent.Agent;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.function.Consumer;


class ConsumerAgent implements Agent {
    // TODO tune batch size
    // private static final int DEFAULT_MAX_MESSAGE_LIMIT = 25_000;

    private TransactionStrategy transactionManager;
    private Transaction transaction;
    private Consumer<?> consumer;
    private Session session;
    private MessageConsumer jmsConsumer;
    private SerializationStrategy serializer;
    private Class<?> messageClass;
    private int batchSize;


    ConsumerAgent(Consumer<?> consumer, Class<?> messageClass, Session session, MessageConsumer jmsConsumer,
                  SerializationStrategy serializer, TransactionStrategy transactionManager, int batchSize) {
        this.consumer = consumer;
        this.messageClass = messageClass;
        this.session = session;
        this.jmsConsumer = jmsConsumer;
        this.serializer = serializer;
        this.transactionManager = transactionManager;
        this.batchSize = batchSize;
    }


    @SuppressWarnings("unchecked")
    @Override
    public int doWork() throws Exception {
        int workCount = 0;

        for (; workCount < batchSize; workCount++) {
            Message message = jmsConsumer.receiveNoWait();
            if (null == message) {
                break;
            }

            if (null == transaction) {
                transaction = transactionManager.begin();
            }

            String str = ((TextMessage) message).getText();
            Object obj = serializer.deserialize(str, messageClass);
            ((Consumer) consumer).accept(obj);
        }

        if (workCount > 0) {
            if (consumer instanceof BatchAware) {
                ((BatchAware) consumer).endOfBatch();
            }
            // TODO move into transaction manager
            session.commit();
            transactionManager.commit(transaction);
            transaction = null;
        }

        return workCount;
    }

    @Override
    public void onClose() {
        // TODO support @PreDestruct?
        // service.onDestroy();
    }

    @Override
    public String roleName() {
        return "Container - " + consumer.getClass().getName();
    }

}
