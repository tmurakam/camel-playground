package org.tmurakam.camel;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;

/**
 * Active MQ route test.
 *
 * You need to run ActiveMQ locally to test this.
 */
public class ActiveMqRoutes {
    public static final String QUEUE = "activemq:queue:queue1";
    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String BROKER_USERNAME = "admin";
    public static final String BROKER_PASSWORD = "admin";
    public static int CONCURRENT_CONSUMERS = 1;
    public static boolean ASYNC_CONSUMER = true;


    public static void createRoutes(CamelContext context) throws Exception {
        configureActiveMq(context);
        context.addRoutes(new ActiveMqProducerRoute());
        context.addRoutes(new ActiveMqConsumerRoute());
    }


    /**
     * Configure ActiveMQ component
     * @param context
     */
    static void configureActiveMq(CamelContext context) {
        ActiveMQConnectionFactory f = new ActiveMQConnectionFactory(BROKER_URL);
        f.setUserName(BROKER_USERNAME);
        f.setPassword(BROKER_PASSWORD);

        ActiveMQConfiguration config = new ActiveMQConfiguration();
        config.setConnectionFactory(f);

        ActiveMQComponent component = new ActiveMQComponent();
        component.setConfiguration(config);
        component.setAsyncConsumer(ASYNC_CONSUMER);  // This is required for 'threads'
        component.setAcknowledgementMode(AUTO_ACKNOWLEDGE);

        context.addComponent("activemq", component);
    }

    /**
     * Producer route. Send message periodically.
     */
    public static class ActiveMqProducerRoute extends RouteBuilder {
        private int counter = 0;

        @Override
        public void configure() throws Exception {
            from("timer:foo?period=1000")
                    .process(exchange -> {
                        counter++;
                        exchange.getIn().setHeader("seq", counter);
                        log.info("producer: {}", counter);
                    })
                    .to(QUEUE);
        }
    }

    /**
     * Consumer route.
     */
    public static class ActiveMqConsumerRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from(QUEUE + "?concurrentConsumers=" + CONCURRENT_CONSUMERS)
                    .process(exchange -> {
                        log.info("enqueue: {}", getSeq(exchange));
                    })

                    // Use threads
                    .threads()
                    .poolSize(5).maxPoolSize(5).maxQueueSize(100)

                    // slow processor
                    .process(exchange -> {
                        log.info("recv start: {}", getSeq(exchange));
                        Thread.sleep(10 * 1000);
                        log.info("recv finish: {}", getSeq(exchange));
                    });
        }

        private String getSeq(Exchange exchange) {
            return exchange.getIn().getHeader("seq").toString();
        }
    }
}
