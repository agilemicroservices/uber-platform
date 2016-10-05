package org.agilemicroservices.mds.client;

import org.agrona.collections.Long2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


class ReceiveAgent implements Agent {
    // TODO set to a sane value
    private static final int MAX_MESSAGE_LIMIT = 100;
    private Long2ObjectHashMap<ReceiveFuture> correlationIdToFutureMap = new Long2ObjectHashMap<>();
    private MessageConsumer consumer;
    private String roleName;
    private Queue replyToQueue;


    public ReceiveAgent(Class<?> serviceInterface, ProxyManager proxyManager) {
        roleName = "Receiver - " + serviceInterface.getName();
        try {
            Session session = proxyManager.createSession();
            replyToQueue = session.createTemporaryQueue();
            consumer = session.createConsumer(replyToQueue);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Invokes a handler upon receipt of a message or on timeout.
     *
     * @param correlationId the correlation ID of the expected message.
     */
    Future<Message> newFuture(long correlationId) {
        ReceiveFuture future = new ReceiveFuture();
        ReceiveFuture existing = correlationIdToFutureMap.put(correlationId, future);
        if (null != existing) {
            throw new IllegalArgumentException("The correlation ID " + correlationId + " is already used.");
        }
        return future;
    }


    @Override
    public int doWork() throws Exception {
        int workCount = 0;

        while (workCount < MAX_MESSAGE_LIMIT) {
            Message message = consumer.receiveNoWait();
            if (null == message) {
                break;
            }
            workCount++;
            dispatchMessage(message);
        }

        return workCount;
    }

    private void dispatchMessage(Message message) {
        long correlationId = -1;
        try {
            String str = message.getJMSCorrelationID();
            correlationId = Long.parseLong(str);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }

        ReceiveFuture future = correlationIdToFutureMap.get(correlationId);
        if (null != future) {
            future.setMessage(message);
        } else {
            // TODO log unexpected message
        }
    }

    @Override
    public void onClose() {
        try {
            consumer.close();
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String roleName() {
        return roleName;
    }

    public Queue getReplyToQueue() {
        return replyToQueue;
    }


    private class ReceiveFuture implements Future<Message> {
        private IdleStrategy idleStrategy = new BackoffIdleStrategy(20, 50, TimeUnit.MILLISECONDS.toNanos(4),
                TimeUnit.MILLISECONDS.toNanos(25));
        // private IdleStrategy idleStrategy = new SleepingIdleStrategy(TimeUnit.MILLISECONDS.toNanos(4));
        private volatile Message message;


        private void setMessage(Message message) {
            this.message = message;
        }


        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return null != message;
        }

        @Override
        public Message get() throws InterruptedException, ExecutionException {
            while (null == message) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                idleStrategy.idle();
            }
            return message;
        }

        @Override
        public Message get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            long thresholdMillis = System.currentTimeMillis() + unit.toMillis(timeout);
            while (null == message) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (System.currentTimeMillis() > thresholdMillis) {
                    throw new TimeoutException("Timed out awaiting response message.");
                }
                idleStrategy.idle();
            }
            return message;
        }
    }
}
