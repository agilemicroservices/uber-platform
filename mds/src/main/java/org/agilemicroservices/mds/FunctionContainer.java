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
import java.util.function.Function;

class FunctionContainer implements Container {
    private IdleStrategy backoffIdleStrategy = Configuration.createDefaultIdleStrategy();
    private Session session;
    private Destination inboundDestination;
    private AgentRunner agentRunner;


    FunctionContainer(Function<?, ?> function, Session session, Destination inboundDestination,
                             TransactionStrategy transactionManager, int batchSize) {
        this.session = session;
        this.inboundDestination = inboundDestination;

        MessageConsumer jmsConsumer;
        try {
            jmsConsumer = session.createConsumer(inboundDestination);
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to create JMS consumer.", e);
        }

        Class<?>[] parameterTypes = TypeResolver.resolveRawArguments(Function.class, function.getClass());
        Class<?> messageClass = parameterTypes[0];

        FunctionAgent agent = new FunctionAgent(function, messageClass, session, jmsConsumer,
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
