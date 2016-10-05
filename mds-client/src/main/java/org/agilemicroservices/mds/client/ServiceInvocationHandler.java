package org.agilemicroservices.mds.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agilemicroservices.mds.util.TypeResolver;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.IdleStrategy;

import javax.jms.Message;
import javax.jms.TextMessage;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


class ServiceInvocationHandler implements InvocationHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private AgentRunner agentRunner;
    private AtomicLong lastCorrelationId = new AtomicLong();
    private Queue<ServiceInvocation> handlerToAgentQueue;
    private ReceiveAgent receiveAgent;


    ServiceInvocationHandler(Class<?> serviceInterface, ProxyManager proxyManager) {
        receiveAgent = new ReceiveAgent(serviceInterface, proxyManager);
        javax.jms.Queue replyToQueue = receiveAgent.getReplyToQueue();

        SenderAgent senderAgent = new SenderAgent(serviceInterface, proxyManager, replyToQueue);
        handlerToAgentQueue = senderAgent.getHandlerToAgentQueue();

        // TODO use a real error handler, tune idle strategy
        // SleepingIdleStrategy idleStrategy = new SleepingIdleStrategy(TimeUnit.MILLISECONDS.toNanos(4));
        IdleStrategy idleStrategy = new BackoffIdleStrategy(20, 50, TimeUnit.MILLISECONDS.toNanos(4),
                TimeUnit.MILLISECONDS.toNanos(25));
        CompositeAgent compositeAgent = new CompositeAgent(receiveAgent, senderAgent);
        agentRunner = new AgentRunner(idleStrategy, Throwable::printStackTrace, null, compositeAgent);
        new Thread(agentRunner).start();
    }

    public void close() {
        agentRunner.close();
    }

    // TODO define representation for null as argument value carried in message
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("hashCode")) {
            return -1;
        } else if (method.getName().equals("equals")) {
            return false;
        } else if (method.getName().equals("toString")) {
            return this.toString();
        }

        long correlationId = lastCorrelationId.incrementAndGet();

        Class<?> returnValueClass = method.getReturnType();
        Future<Message> future = null;
        if (returnValueClass != void.class) {
            future = receiveAgent.newFuture(correlationId);
        }

        String str = null;
        if (null != args && args.length > 0) {
            try {
                str = OBJECT_MAPPER.writeValueAsString(args[0]);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(e);
            }
        }

        ServiceInvocation invocation = new ServiceInvocation(correlationId, method, str);
        handlerToAgentQueue.add(invocation);

        Object returnValue = null;
        if (null != future) {
            // TODO make timeout configurable
            Message message = future.get(30, TimeUnit.SECONDS);
            String responseText = ((TextMessage) message).getText();
            // TODO support all standard collection types
            if (List.class.isAssignableFrom(returnValueClass)) {
                Class<?> elementClass = TypeResolver.resolveRawArgument(method.getGenericReturnType(), returnValueClass);
                JavaType resultType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass);
                returnValue = OBJECT_MAPPER.readValue(responseText, resultType);
            } else {
                returnValue = OBJECT_MAPPER.readValue(responseText, returnValueClass);
            }
        }

        return returnValue;
    }
}