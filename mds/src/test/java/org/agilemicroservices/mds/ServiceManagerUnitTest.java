package org.agilemicroservices.mds;

import org.agilemicroservices.mds.annotation.JmsInbound;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


public class ServiceManagerUnitTest {
    private static final Queue QUEUE = ActiveMQJMSClient.createQueue("test");
    private static final Topic TOPIC = ActiveMQJMSClient.createTopic("test");

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private ServiceManager serviceManager;


    @Before
    public void setUp() throws Exception {
        session = mock(Session.class);

        connection = mock(Connection.class);
        doReturn(session).when(connection).createSession(anyBoolean(), anyInt());

        connectionFactory = mock(ConnectionFactory.class);
        doReturn(connection).when(connectionFactory).createConnection();

        serviceManager = new ServiceManager(connectionFactory, null);
    }

    @After
    public void tearDown() {
        serviceManager.close();
    }


    @Test
    public void shouldCreateConsumerForUnannotatedConsumer() throws Exception {
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        doReturn(messageConsumer).when(session).createConsumer(eq(QUEUE));

        serviceManager.register(QUEUE, (String o) -> {
        });

        verify(session).createConsumer(QUEUE);
    }

    @Test
    public void shouldCreateConsumerForUnannotatedFunction() throws Exception {
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        doReturn(messageConsumer).when(session).createConsumer(eq(QUEUE));

        serviceManager.register(QUEUE, (String o) -> null);

        verify(session).createConsumer(QUEUE);
    }

    @Test
    public void shouldRegisterAnnotatedConsumer() throws Exception {
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        doReturn(messageConsumer).when(session).createConsumer(eq(QUEUE));

        serviceManager.register(new AnnotatedConsumer());

        verify(session).createConsumer(QUEUE);
    }

    @Test
    public void shouldRegisterAnnotatedFunction() {

    }

    @Test
    public void shouldRegisterFunctionCompatible() {
    }

    @Test
    public void shouldRegisterObjectWithMultipleMethods() throws Exception {
        MessageConsumer queueConsumer = mock(MessageConsumer.class);
        doReturn(queueConsumer).when(session).createConsumer(eq(QUEUE));
        MessageConsumer topicConsumer = mock(MessageConsumer.class);
        doReturn(topicConsumer).when(session).createConsumer(eq(TOPIC));

        TestImpl impl = new TestImpl();

        serviceManager.register(impl);

        verify(session).createConsumer(QUEUE);
        verify(session).createConsumer(TOPIC);
    }


    public class TestImpl {

        @JmsInbound("queue:test")
        public void queue(String message) {
        }

        @JmsInbound("topic:test")
        public void topic(String message) {
        }
    }


    public static class AnnotatedConsumer implements Consumer<String> {

        @Override
        @JmsInbound("queue:test")
        public void accept(String s) {

        }
    }


    public static class AnnotatedFunction implements Function<String, String> {

        @Override
        public String apply(String s) {
            return null;
        }
    }
}
