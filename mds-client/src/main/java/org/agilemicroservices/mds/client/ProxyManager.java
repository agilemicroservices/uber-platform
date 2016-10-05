package org.agilemicroservices.mds.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ProxyManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyManager.class);

    private ConcurrentMap<Class<?>, Object> serviceInterfaceToProxyMap = new ConcurrentHashMap<>();
    private ConnectionFactory connectionFactory;
    private Connection connection;


    public ProxyManager(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        connect();
    }


    // TODO reinstate reconnect once agent threads are spawned from executor and interrupted correctly by wildfly
    // TODO support timeout
    private void connect() {
        closeQuietly(connection);

//        do {
            try {
                connection = connectionFactory.createConnection();
                connection.start();
            } catch (Throwable t) {
                LOGGER.error("Failed to connect to JMS broker.", t);
                closeQuietly(connection);
                throw new IllegalStateException("Unable to connect.", t);

//                // TODO make sleep time configurable
//                try {
//                    Thread.sleep(250);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    throw new IllegalStateException("Interrupted attempting to reconnect.", e);
//                }
            }
//        } while (null == connection);
    }

    private void closeQuietly(Connection connection) {
        if (null != connection) {
            try {
                connection.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }


    @Override
    public void close() {
        serviceInterfaceToProxyMap.values().forEach(o -> {
            ServiceInvocationHandler handler = (ServiceInvocationHandler) Proxy.getInvocationHandler(o);
            handler.close();
        });
    }


    // TODO support timeout
    synchronized Session createSession() {
        Session session = null;

        do {
            try {
                LOGGER.debug("Creating JMS session.");
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
            } catch (JMSException e) {
                LOGGER.error("Session creation failed.", e);
                connect();
            }
        } while (null == session);

        return session;
    }


    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> serviceInterface) {
        Object proxy = serviceInterfaceToProxyMap.get(serviceInterface);
        if (null == proxy) {
            synchronized (this) {
                proxy = serviceInterfaceToProxyMap.get(serviceInterface);
                if (null == proxy) {
                    //Session session = createSession();
                    ServiceInvocationHandler handler = new ServiceInvocationHandler(serviceInterface, this);
                    proxy = Proxy.newProxyInstance(ProxyManager.class.getClassLoader(), new Class<?>[]{serviceInterface},
                            handler);
                    serviceInterfaceToProxyMap.put(serviceInterface, proxy);

                    LOGGER.debug("Created client messaging proxy; interface={}.", serviceInterface.getName());
                }
            }
        }
        return (T) proxy;
    }
}
