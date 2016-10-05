package org.agilemicroservices.mds;

import org.agilemicroservices.mds.integration.jackson.JacksonJsonSerializationStrategy;
import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.agilemicroservices.mds.util.TypeResolver;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.util.function.Consumer;


class ConsumerContainer implements Container {
    private IdleStrategy backoffIdleStrategy = Configuration.createDefaultIdleStrategy();
    private Session session;
    private Destination inboundDestination;
    private AgentRunner agentRunner;


    ConsumerContainer(Consumer<?> consumer, Session session, Destination inboundDestination,
                      TransactionStrategy transactionManager, int batchSize) {
        this.session = session;
        this.inboundDestination = inboundDestination;

        MessageConsumer jmsConsumer;
        try {
            jmsConsumer = session.createConsumer(inboundDestination);
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to create JMS consumer.", e);
        }

        Class<?> messageClass = TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());

        ConsumerAgent agent = new ConsumerAgent(consumer, messageClass, session, jmsConsumer,
                JacksonJsonSerializationStrategy.INSTANCE, transactionManager, batchSize);
        agentRunner = new AgentRunner(backoffIdleStrategy, Throwable::printStackTrace, null, agent);
        new Thread(agentRunner).start();
    }


    @Override
    public void close() {
        agentRunner.close();
        try {
            session.close();
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }
}
