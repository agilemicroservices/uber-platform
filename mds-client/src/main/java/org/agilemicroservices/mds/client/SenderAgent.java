package org.agilemicroservices.mds.client;

import org.agilemicroservices.mds.util.DestinationUri;
import org.agilemicroservices.mds.util.ReflectionUtil;
import org.agilemicroservices.mds.util.ServiceMethod;
import org.agrona.concurrent.Agent;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


class SenderAgent implements Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderAgent.class);
    private static final int DEFAULT_MAX_MESSAGE_LIMIT = 1000;

    private ProxyManager proxyManager;
    private Class<?> serviceInterface;
    private Map<Method, MessageProducer> producers = new HashMap<>();
    private Session session;
    // TODO set to sane limit
    private ConcurrentLinkedQueue<ServiceInvocation> handlerToAgentQueue = new ConcurrentLinkedQueue<>();
    private String name;
    private javax.jms.Queue replyToQueue;


    SenderAgent(Class<?> serviceInterface, ProxyManager proxyManager, javax.jms.Queue replyToQueue) {
        name = "Sender - " + serviceInterface.getName();
        this.serviceInterface = serviceInterface;
        this.proxyManager = proxyManager;
        this.session = proxyManager.createSession();
        this.replyToQueue = replyToQueue;

        refreshProducers();
    }


    private void refreshProducers() {
        ReflectionUtil.forEachServiceMethod(serviceInterface, this::createProducer);
    }

    private void createProducer(ServiceMethod serviceMethod) {
        Destination destination;
        try {
            DestinationUri inboundUri = serviceMethod.getInboundUri();
            if (inboundUri.getScheme().equals(DestinationUri.QUEUE_SCHEME)) {
                destination = ActiveMQJMSClient.createQueue(inboundUri.getName());
            } else if (inboundUri.getScheme().equals(DestinationUri.TOPIC_SCHEME)) {
                destination = ActiveMQJMSClient.createTopic(inboundUri.getName());
            } else {
                throw new IllegalArgumentException("Unsupported destination URI scheme '" + inboundUri.getScheme() + "'.");
            }

            MessageProducer producer = session.createProducer(destination);
            producers.put(serviceMethod.getMethod(), producer);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }

    private void recreateProducer(Method method, MessageProducer producer) {
        try {
            producer.close();
        } catch (Throwable t) {
            // ignore
        }

        try {
            MessageProducer newProducer = session.createProducer(producer.getDestination());
            producers.put(method, newProducer);
        } catch (JMSException e) {
            recreateSession();
        }
    }

    private void recreateSession() {
        try {
            session.close();
        } catch (Throwable t) {
            // ignore
        }

        session = proxyManager.createSession();
        refreshProducers();
    }


    // TODO define representation for null as argument value carried in message
    private void send(ServiceInvocation invocation) {
        MessageProducer producer = null;
        try {
            TextMessage message = session.createTextMessage(invocation.getBody());
            message.setJMSCorrelationID(String.valueOf(invocation.getCorrelationId()));
            message.setJMSReplyTo(replyToQueue);
            producer = producers.get(invocation.getMethod());
            if (null == producer) {
                LOGGER.error("No producer for method {}.", invocation.getMethod());
            } else {
                producer.send(message);
                // TODO commit in batch
                session.commit();

                LOGGER.debug("Sent message for invocation, method={},destination={},correlationId={}.",
                        invocation.getMethod().getName(),
                        producer.getDestination(),
                        invocation.getCorrelationId());
            }
        } catch (JMSException e) {
            LOGGER.error("Failed to send message,method=" + invocation.getMethod().getName() + ".", e);
            if (null != producer) {
                recreateProducer(invocation.getMethod(), producer);
            }
        }
    }

    @Override
    public int doWork() throws Exception {
        int workCount = 0;
        for (; workCount < DEFAULT_MAX_MESSAGE_LIMIT; workCount++) {
            ServiceInvocation invocation = handlerToAgentQueue.poll();
            if (null == invocation) {
                break;
            }
            send(invocation);
        }

        LOGGER.trace("Completed duty cycle, workCount={}.", workCount);

        return workCount;
    }

    @Override
    public String roleName() {
        return name;
    }

    public Queue<ServiceInvocation> getHandlerToAgentQueue() {
        return handlerToAgentQueue;
    }
}
