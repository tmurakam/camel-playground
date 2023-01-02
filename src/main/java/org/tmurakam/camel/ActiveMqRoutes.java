package org.tmurakam.camel;

import com.atomikos.jms.AtomikosConnectionFactoryBean;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.activemq.ActiveMQConfiguration;
import org.apache.camel.model.RouteDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.jta.JtaTransactionManager;

import static javax.jms.Session.*;

/**
 * Active MQ route test.
 *
 * You need to run ActiveMQ locally to test this.
 */
@Service
public class ActiveMqRoutes {
    public static final String QUEUE = "activemq:queue:queue1";
    public static final String BROKER_URL = "tcp://localhost:61616";
    public static final String BROKER_USERNAME = "admin";
    public static final String BROKER_PASSWORD = "admin";
    public static int CONCURRENT_CONSUMERS = 5;
    public static boolean ASYNC_CONSUMER = true;
    public static boolean ENABLE_TX = false;  // Change this to true to test transaction

    @Autowired
    private CamelContext camelContext;
    @Autowired
    private JtaTransactionManager txManager;


    public void createRoutes() throws Exception {
        configureActiveMq();
        camelContext.addRoutes(new ActiveMqProducerRoute());
        camelContext.addRoutes(new ActiveMqConsumerRoute());
    }

    /**
     * Configure ActiveMQ component
     */
    private void configureActiveMq() {
        ActiveMQConfiguration config = new ActiveMQConfiguration();

        if (ENABLE_TX) {
            ActiveMQXAConnectionFactory f = new ActiveMQXAConnectionFactory(BROKER_URL);
            f.setUserName(BROKER_USERNAME);
            f.setPassword(BROKER_PASSWORD);

            AtomikosConnectionFactoryBean af = new AtomikosConnectionFactoryBean();
            af.setUniqueResourceName("af");
            af.setPoolSize(20);
            af.setMaxPoolSize(40);
            af.setXaConnectionFactory(f);

            config.setConnectionFactory(af);
            config.setUsePooledConnection(true);
            config.setTransactionManager(txManager);
            config.setTransacted(true);
        } else {
            ActiveMQConnectionFactory f = new ActiveMQConnectionFactory(BROKER_URL);
            f.setUserName(BROKER_USERNAME);
            f.setPassword(BROKER_PASSWORD);

            config.setConnectionFactory(f);
            config.setUsePooledConnection(true);
            config.setTransacted(false);
        }

        ActiveMQComponent component = new ActiveMQComponent();
        component.setConfiguration(config);
        component.setAsyncConsumer(ASYNC_CONSUMER);  // This is required for 'threads'. Note: This does not work with transaction
        if (ENABLE_TX) {
            component.setAcknowledgementMode(SESSION_TRANSACTED);
        } else {
            component.setAcknowledgementMode(AUTO_ACKNOWLEDGE);
        }

        camelContext.addComponent("activemq", component);
    }

    /**
     * Producer route. Send message periodically.
     */
    private static class ActiveMqProducerRoute extends RouteBuilder {
        private int counter = 0;

        @Override
        public void configure() {
            RouteDefinition route = from("timer:foo?period=1000");

            Processor p = exchange -> {
                counter++;
                exchange.getIn().setHeader("seq", counter);
                log.info("producer: {}", counter);
            };
            
            if (ENABLE_TX) {
                route
                        .transacted()
                        .process(p)
                        .to(QUEUE);
            } else {
                route
                        .process(p)
                        .to(QUEUE);
            }
        }
    }

    /**
     * Consumer route.
     */
    private static class ActiveMqConsumerRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            RouteDefinition route = from(QUEUE + "?concurrentConsumers=" + CONCURRENT_CONSUMERS);

            Processor slowProcessor = (exchange) -> {
                log.info("recv start: {}", getSeq(exchange));
                Thread.sleep(3 * 1000);
                log.info("recv finish: {}", getSeq(exchange));
            };

            if (ENABLE_TX) {
                route
                        .transacted()
                        .process(slowProcessor);
            } else {
                route
                        .threads().poolSize(5).maxPoolSize(5).maxQueueSize(100)
                        .process(slowProcessor);
            }
        }

        private String getSeq(Exchange exchange) {
            return exchange.getIn().getHeader("seq").toString();
        }
    }
}
