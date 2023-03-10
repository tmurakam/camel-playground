package org.tmurakam.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


/**
 * A basic example running as public static void main.
 */
@EnableAutoConfiguration
@Service
@Slf4j
public class CamelBasic {
    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ActiveMqRoutes activeMqRoutes;

    @PostConstruct
    public void postConstruct() throws Exception {
        log.info("started");

        //camelContext.addRoutes(helloRoute());
        //camelContext.addRoutes(asyncExceptionRoute());
        //camelContext.addRoutes(new ThreadsQueueTestRoute());
        activeMqRoutes.createRoutes();
    }

    RouteBuilder helloRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:foo")
                        .log("Hello Camel");
            }
        };
    }

    RouteBuilder asyncExceptionRoute() {
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
}
