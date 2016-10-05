package org.agilemicroservices.mds;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MessageDrivenServiceContainer implements Container {
    // TODO set to sane value
    private final IdleStrategy backoffIdleStrategy = new SleepingIdleStrategy(TimeUnit.MILLISECONDS.toNanos(15));
    private final MessageDrivenServiceFactory factory;
    private final Session session;
    private final Destination inboundDestination;
    private final List<AgentRunner> activeInstances = new ArrayList<>();


    public MessageDrivenServiceContainer(MessageDrivenServiceFactory factory, Session session,
                                         Destination inboundDestination, int initialInstanceCount) {
        this.factory = factory;
        this.session = session;
        this.inboundDestination = inboundDestination;

        for (int i = 0; i < initialInstanceCount; i++) {
            createInstance();
        }
    }


    private void createInstance() {
        MessageDrivenService instance = factory.create(session);
        MessageConsumer consumer;
        try {
            consumer = session.createConsumer(inboundDestination);
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to create JMS consumer.", e);
        }

        MessageDrivenServiceAgent agent = new MessageDrivenServiceAgent(instance, session, consumer);
        AgentRunner runner = new AgentRunner(backoffIdleStrategy, Throwable::printStackTrace, null, agent);
        new Thread(runner).start();
        activeInstances.add(runner);
    }

    private void destroyInstance() {
        AgentRunner runner = activeInstances.remove(0);
        runner.close();
    }


    @Override
    public void destroy() {
        while (!activeInstances.isEmpty()) {
            destroyInstance();
        }
    }


    public int getInstanceCount() {
        return activeInstances.size();
    }

    public void setInstanceCount(int instanceCount) {
        // TODO implement
    }
}
