package org.tmurakam.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * A basic example running as public static void main.
 */
@Slf4j
public final class CamelBasic {
    private static CamelContext camelContext;

    public static void main(String[] args) throws Exception {
        log.info("started");

        // create a CamelContext
        camelContext = new DefaultCamelContext();

        //camel.addRoutes(helloRoute());
        //camel.addRoutes(asyncExceptionRoute());
        camelContext.addRoutes(threadsQueueTestRoute());

        // start is not blocking
        camelContext.start();

        Thread.sleep(60 * 60 * 1000);
    }

    static void configureActiveMq() {
        ActiveMQConnectionFactory f = new ActiveMQConnectionFactory("tcp://localhost:61616");
        f.setUserName("admin");
        f.setPassword("admin");

        ActiveMQConfiguration config = new ActiveMQConfiguration();
        config.setConnectionFactory(f);

        ActiveMQComponent component = new ActiveMQComponent();
        component.setConfiguration(config);

        camelContext.addComponent("activemq", component);
    }

    static RouteBuilder helloRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:foo")
                        .log("Hello Camel");
            }
        };
    }

    static RouteBuilder asyncExceptionRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:foo?delay=0&period=10000")
                        .onException(Exception.class)
                            .log("Handle exception")
                            .handled(true)
                        .end()

                        // throw exception on async threads
                        .threads(10)
                        .log("In async threads")
                        .throwException(new RuntimeException("ex1"))
                        .log("Should not reach here");
            }
        };
    }

    static RouteBuilder threadsQueueTestRoute() {
        return new RouteBuilder() {
            int counter = 0;
            
            @Override
            public void configure() throws Exception {
                from("timer:foo?period=10")
                        .onException(Exception.class)
                            .log("Handle exception")
                            .handled(true)
                        .end()

                        .setExchangePattern(ExchangePattern.InOnly)
                        .process(exchange -> {
                            counter++;
                            exchange.setExchangeId(Integer.toString(counter));
                            log.info("Consume: exchange id = {}", exchange.getExchangeId());
                        })

                        // Create threads = 1, queue size = 10
                        .threads()
                        .poolSize(1)
                        .maxPoolSize(1)
                        .maxQueueSize(10)

                        // Create slow processor. This will 'block' the input.
                        .process(exchange -> {
                            String id = exchange.getExchangeId();
                            log.info("Start Processor: id = {}", id);
                            Thread.sleep(3 * 1000);
                            log.info("Finish Processor: id = {}", id);
                        });
            }
        };
    }
}
