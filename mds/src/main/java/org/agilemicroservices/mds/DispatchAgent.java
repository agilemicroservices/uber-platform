package org.agilemicroservices.mds;

import org.agilemicroservices.mds.integration.SerializationStrategy;
import org.agilemicroservices.mds.integration.Transaction;
import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.agilemicroservices.mds.util.DestinationUri;
import org.agilemicroservices.mds.util.ServiceMethod;
import org.agrona.concurrent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;


class DispatchAgent implements Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatchAgent.class);
    // TODO tune
    // private static final int DEFAULT_MAX_MESSAGE_LIMIT = 1;

    private SerializationStrategy serializationStrategy;
    private TransactionStrategy transactionStrategy;
    private Object receiver;
    private ServiceMethod serviceMethod;
    private Session session;
    private MessageConsumer messageConsumer;
    private int batchSize;


    DispatchAgent(Object receiver, ServiceMethod serviceMethod, Session session,
                  SerializationStrategy serializationStrategy, TransactionStrategy transactionStrategy, int batchSize) {
        this.receiver = receiver;
        this.serviceMethod = serviceMethod;
        this.session = session;
        this.serializationStrategy = serializationStrategy;
        this.transactionStrategy = transactionStrategy;
        this.batchSize = batchSize;

        Destination destination = createDestination(serviceMethod.getInboundUri());
        try {
            messageConsumer = session.createConsumer(destination);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }


    private Destination createDestination(DestinationUri uri) {
        Destination destination;
        try {
            if (uri.isQueue()) {
                destination = session.createQueue(uri.getName());
            } else if (uri.isTopic()) {
                destination = session.createTopic(uri.getName());
            } else {
                throw new IllegalArgumentException("Invalid scheme " + uri.getScheme());
            }
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to create JMS destination '" + uri.getName() + "'.");
        }

        return destination;
    }

    @Override
    public void onClose() {
        try {
            messageConsumer.close();
        } catch (JMSException e) {
            // TODO log
            e.printStackTrace();
        }
    }


    //@Override
    public int oldDoWork() throws Exception {
        int workCount = 0;

        Transaction transaction = null;
        for (; workCount < batchSize; workCount++) {
            Message message = messageConsumer.receiveNoWait();
            if (null == message) {
                break;
            }

            if (null == transaction) {
                transaction = transactionStrategy.begin();
            }

            Object argument;
            if (serviceMethod.getMessageClass().isAssignableFrom(message.getClass())) {
                argument = message;
            } else {
                String requestText = ((TextMessage) message).getText();
                argument = serializationStrategy.deserialize(requestText, serviceMethod.getMessageClass());
            }

            Object returnValue;
            try {
                returnValue = serviceMethod.getMethod().invoke(receiver, argument);
            } catch (Throwable t) {
                LOGGER.error("Exception invoking service.", t);
                if (null != transaction) {
                    try {
                        transactionStrategy.rollback(transaction);
                    } catch (Throwable innerT) {
                        LOGGER.error("Unable to rollback database transaction.", innerT);
                    }

                    try {
                        session.rollback();
                    } catch (Throwable innerT) {
                        LOGGER.error("Unable to rollback JMS transaction.", innerT);
                    }
                }

                return workCount;
            }

            if (!serviceMethod.isOneWay()) {
                Destination destination = message.getJMSReplyTo();
                if (null != destination) {
                    String responseText = serializationStrategy.serialize(returnValue);
                    TextMessage response = session.createTextMessage(responseText);
                    response.setJMSCorrelationID(message.getJMSCorrelationID());

                    // TODO avoid creation/disposal on every invocation
                    MessageProducer producer = session.createProducer(destination);
                    producer.send(response);
                    producer.close();
                }
            }
        }

        if (workCount > 0) {
            if (receiver instanceof BatchAware) {
                ((BatchAware) receiver).endOfBatch();
            }
            // TODO move into transaction manager
            session.commit();
        }

        if (null != transaction) {
            transactionStrategy.commit(transaction);
        }

        return workCount;
    }


    @Override
    public int doWork() {
        int workCount;

        try {
            workCount = performTransaction();
            if (workCount > 0) {
                session.commit();
                LOGGER.debug("Committed JMS session, service={}.", serviceMethod);
            }
        } catch (Throwable t) {
            LOGGER.error("Exception invoking " + serviceMethod + ".", t);
            try {
                session.rollback();
            } catch (JMSException e) {
                LOGGER.error("Unable to rollback session, service=" + serviceMethod + ".", e);
            }
            // avoid throttling the agent when exception occurs on first message
            workCount = 1;
        }

        return workCount;
    }


    private int performTransaction() throws Throwable {
        int workCount = 0;

        Transaction transaction = null;
        try {
            for (; workCount < batchSize; workCount++) {
                Message message = messageConsumer.receiveNoWait();
                if (null == message) {
                    break;
                }

                if (null == transaction) {
                    LOGGER.debug("Beginning database transaction, service={}.", serviceMethod);
                    transaction = transactionStrategy.begin();
                }

                Object returnValue;
                if (null == serviceMethod.getMessageClass()) {
                    returnValue = serviceMethod.getMethod().invoke(receiver);
                } else {
                    Object argument = getArgument(message);
                    returnValue = serviceMethod.getMethod().invoke(receiver, argument);
                }

                if (!serviceMethod.isOneWay()) {
                    Destination destination = message.getJMSReplyTo();
                    if (null != destination) {
                        String responseText = serializationStrategy.serialize(returnValue);
                        TextMessage response = session.createTextMessage(responseText);
                        response.setJMSCorrelationID(message.getJMSCorrelationID());

                        // TODO avoid creation/disposal on every invocation
                        MessageProducer producer = session.createProducer(destination);
                        producer.send(response);
                        producer.close();
                    }
                }
            }

            if (workCount > 0 && receiver instanceof BatchAware) {
                ((BatchAware) receiver).endOfBatch();
            }
        } catch (Throwable t) {
            LOGGER.debug("Transaction failed, rolling back, service={}.", serviceMethod);
            if (null != transaction) {
                try {
                    transactionStrategy.rollback(transaction);
                } catch (Throwable t2) {
                    LOGGER.error("Unable to rollback database transaction, service=" + serviceMethod + ".", t2);
                }
            }
            throw t;
        }

        if (null != transaction) {
            transactionStrategy.commit(transaction);
            LOGGER.debug("Committed database transaction, service={}.", serviceMethod);
        }

        return workCount;
    }


    private Object getArgument(Message message) throws JMSException {
        Class<?> clazz = serviceMethod.getMessageClass();
        if (clazz.isAssignableFrom(message.getClass())) {
            return message;
        } else {
            String str = ((TextMessage) message).getText();
            return serializationStrategy.deserialize(str, clazz);
        }
    }


    @Override
    public String roleName() {
        return "Dispatcher - " + serviceMethod;
    }
}
