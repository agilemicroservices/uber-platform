package org.agilemicroservices.mds;

import org.agilemicroservices.mds.integration.SerializationStrategy;
import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.agilemicroservices.mds.util.ReflectionUtil;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.IdleStrategy;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.*;
import java.util.concurrent.TimeUnit;


class ServiceContainer implements Container {
    private List<AgentRunner> agentRunners = new ArrayList<>();
    private Collection<Session> sessions;


    ServiceContainer(Object service, Connection connection, Map<String, SerializationStrategy> formatToSerializerMap,
                     TransactionStrategy transactionStrategy, AuthHandler authHandler, int batchSize) {
        Map<String, List<DispatchAgent>> scopeToDispatchersMap = new HashMap<>();
        Map<String, Session> scopeToSessionMap = new HashMap<>();

        // create dispatcher for each message-driven method
        ReflectionUtil.forEachServiceMethod(service.getClass(), serviceMethod -> {
            String scope = serviceMethod.getScope();
            Session session = getOrCreateSession(scope, connection, scopeToSessionMap);
            DispatchAgent dispatcher = new DispatchAgent(
                    service,
                    serviceMethod,
                    session,
                    formatToSerializerMap.get(serviceMethod.getFormat()),
                    transactionStrategy,
                    authHandler,
                    batchSize);

            addDispatcher(dispatcher, scope, scopeToDispatchersMap);
        });
        sessions = scopeToSessionMap.values();

        // create and schedule a single composite agent for all agents in each scope
        for (String o : scopeToDispatchersMap.keySet()) {
            List<DispatchAgent> dispatchers = scopeToDispatchersMap.get(o);
            CompositeAgent agent = new CompositeAgent(dispatchers);
            IdleStrategy idleStrategy = Configuration.createDefaultIdleStrategy();
            AgentRunner runner = new AgentRunner(idleStrategy, Throwable::printStackTrace, null, agent);
            agentRunners.add(runner);

            new Thread(runner).start();
        }
    }


    private void addDispatcher(DispatchAgent dispatcher, String scope,
                               Map<String, List<DispatchAgent>> scopeToDispatchersMap) {
        List<DispatchAgent> dispatchers = scopeToDispatchersMap.get(scope);
        if (null == dispatchers) {
            dispatchers = new ArrayList<>();
            scopeToDispatchersMap.put(scope, dispatchers);
        }

        dispatchers.add(dispatcher);
    }

    private Session getOrCreateSession(String scope, Connection connection, Map<String, Session> scopeToSessionMap) {
        Session session = scopeToSessionMap.get(scope);
        if (null == session) {
            try {
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
            } catch (JMSException e) {
                throw new IllegalStateException(e);
            }
            scopeToSessionMap.put(scope, session);
        }
        return session;
    }


    @Override
    public void close() {
        agentRunners.forEach(AgentRunner::close);
        sessions.forEach(session -> {
            try {
                session.close();
            } catch (JMSException e) {
                // TODO log
                e.printStackTrace();
            }
        });
    }
}