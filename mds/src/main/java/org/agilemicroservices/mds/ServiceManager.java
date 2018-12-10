package org.agilemicroservices.mds;

import org.agilemicroservices.mds.integration.SerializationStrategy;
import org.agilemicroservices.mds.integration.TransactionStrategy;
import org.agilemicroservices.mds.integration.jackson.JacksonJsonSerializationStrategy;

import javax.jms.*;
import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * <code>ServiceManager</code> manages registered services and provides methods for registering new services.
 * <p>
 * The following example illustrates a simple function annotated for registration with a service manager.  The function
 * returns the string <code>Hello </code> followed by the argument value.
 * <pre><code>
 *     public class HelloFunction implements Function&lt;String, String&gt; {
 *
 *        {@literal @}JmsInbound("queue:example.hello")
 *         public String apply(String name) {
 *             return "Hello " + name;
 *         }
 *     }
 * </code></pre>
 * The following example illustrates creating a service manager and registering a function.
 * <pre><code>
 *     ConnectionFactory factory = ...
 *     ServiceManager manager = new ServiceManager(factory);
 *     manager.register(new HelloFunction());
 * </code></pre>
 */
// TODO improve name
// TODO register method should accept Object
public class ServiceManager {
    private static final AuthHandler DEFAULT_AUTH_HANDLER = message -> {};
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final Map<String, SerializationStrategy> DEFAULT_SERIALIZER_MAP =
            Collections.singletonMap("json", new JacksonJsonSerializationStrategy());

    private ArrayList<Container> containers = new ArrayList<>();
    private Map<String, SerializationStrategy> formatToSerializerMap;
    private AuthHandler authHandler;
    private TransactionStrategy transactionManager;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private int batchSize;


    public ServiceManager(ConnectionFactory connectionFactory, TransactionStrategy transactionManager) {
        this(connectionFactory, transactionManager, DEFAULT_BATCH_SIZE);
    }

    public ServiceManager(ConnectionFactory connectionFactory, TransactionStrategy transactionManager, int batchSize) {
        this(connectionFactory, transactionManager, DEFAULT_SERIALIZER_MAP, batchSize);
    }

    public ServiceManager(ConnectionFactory connectionFactory, TransactionStrategy transactionManager,
                          Map<String, SerializationStrategy> formatToSerializerMap) {
        this(connectionFactory, transactionManager, formatToSerializerMap, DEFAULT_BATCH_SIZE);
    }

    public ServiceManager(ConnectionFactory connectionFactory, TransactionStrategy transactionManager,
                          Map<String, SerializationStrategy> formatToSerializerMap, int batchSize) {
        this(connectionFactory, transactionManager, formatToSerializerMap, DEFAULT_AUTH_HANDLER, batchSize);
    }

    public ServiceManager(ConnectionFactory connectionFactory, TransactionStrategy transactionManager,
                          Map<String, SerializationStrategy> formatToSerializerMap,
                          AuthHandler authHandler) {
        this(connectionFactory, transactionManager, formatToSerializerMap, authHandler, DEFAULT_BATCH_SIZE);
    }

    public ServiceManager(ConnectionFactory connectionFactory, TransactionStrategy transactionManager,
                          Map<String, SerializationStrategy> formatToSerializerMap,
                          AuthHandler authHandler, int batchSize) {
        this.connectionFactory = connectionFactory;
        this.transactionManager = transactionManager;
        this.formatToSerializerMap = formatToSerializerMap;
        this.authHandler = authHandler;
        this.batchSize = batchSize;

        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException e) {
            throw new IllegalStateException("Unable to create JMS connection.", e);
        }
    }


    public void close() {
        containers.forEach(Container::close);
        try {
            connection.close();
        } catch (JMSException e) {
            // TODO should this just be logged and not thrown?
            throw new IllegalStateException(e);
        }
    }


    /**
     * Registers an annotated <code>Consumer</code>, <code>Function</code> or interface composed of multiple methods.
     *
     * @param obj the annotated object to be registered.
     */
    public void register(Object obj) {
        ServiceContainer container = new ServiceContainer(obj, connection, formatToSerializerMap, transactionManager,
                authHandler, batchSize);
        containers.add(container);
    }

    /**
     * Register an unannotated <code>Consumer</code>.
     *
     * @param inboundDestination
     * @param consumer
     */
    // TODO rework to match register(Object)
    // TODO make destination argument optional, read from annotations
    public void register(Destination inboundDestination, Consumer<?> consumer) {
        Session session;
        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }

        ConsumerContainer container = new ConsumerContainer(consumer, session, inboundDestination, transactionManager,
                batchSize);
        containers.add(container);
    }

    /**
     * Register an unannotated <code>Function</code>.
     *
     * @param inboundDestination
     * @param function
     */
    // TODO rework to match register(Object)
    // TODO make destination argument optional, read from annotations
    public void register(Destination inboundDestination, Function<?, ?> function) {
        Session session;
        try {
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }

        FunctionContainer container = new FunctionContainer(function, session, inboundDestination, transactionManager,
                batchSize);
        containers.add(container);
    }
}