package org.tmurakam.camel;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

public class ActiveMqRoutes {
    public static final String QUEUE = "activemq:queue:queue1";

    public static void createRoutes(CamelContext context) throws Exception {
        configureActiveMq(context);
        context.addRoutes(new ActiveMqProducerRoute());
        context.addRoutes(new ActiveMqConsumerRoute());
    }


    static void configureActiveMq(CamelContext context) {
        ActiveMQConnectionFactory f = new ActiveMQConnectionFactory("tcp://localhost:61616");
        f.setUserName("admin");
        f.setPassword("admin");

        ActiveMQConfiguration config = new ActiveMQConfiguration();
        config.setConnectionFactory(f);

        ActiveMQComponent component = new ActiveMQComponent();
        component.setConfiguration(config);

        context.addComponent("activemq", component);
    }

    public static class ActiveMqProducerRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("timer:foo?period=1000")
                    .to(QUEUE)
                    .log("activemq sent");
        }
    }

    public static class ActiveMqConsumerRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from(QUEUE)
                    .log("activemq recv");
        }
    }
}
