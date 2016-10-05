package org.agilemicroservices.mds;

import org.agilemicroservices.mds.integration.Transaction;
import org.agilemicroservices.mds.integration.SerializationStrategy;
import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.agrona.concurrent.Agent;

import javax.jms.*;
import java.util.function.Function;


class FunctionAgent implements Agent {
    // TODO tune batch size
    // private static final int DEFAULT_MAX_MESSAGE_LIMIT = 25_000;

    private TransactionStrategy transactionManager;
    private Transaction transaction;
    private Function<?, ?> function;
    private Session session;
    private MessageConsumer jmsConsumer;
    private SerializationStrategy serializer;
    private Class<?> messageClass;
    private int batchSize;


    FunctionAgent(Function<?, ?> function, Class<?> messageClass, Session session, MessageConsumer jmsConsumer,
                  SerializationStrategy serializer, TransactionStrategy transactionManager, int batchSize) {
        this.function = function;
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

            String requestText = ((TextMessage) message).getText();
            Object argument = serializer.deserialize(requestText, messageClass);
            Object returnValue = ((Function) function).apply(argument);

            Destination destination = message.getJMSReplyTo();
            if (null != destination) {
                String responseText = serializer.serialize(returnValue);
                TextMessage response = session.createTextMessage(responseText);
                response.setJMSCorrelationID(message.getJMSCorrelationID());
                MessageProducer producer = session.createProducer(destination);
                producer.send(response);
                producer.close();
            }
        }

        if (workCount > 0) {
            if (function instanceof BatchAware) {
                ((BatchAware) function).endOfBatch();
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
        return "Container - " + function.getClass().getName();
    }

}
