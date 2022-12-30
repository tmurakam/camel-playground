package org.tmurakam.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
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

        //camelContext.addRoutes(helloRoute());
        //camelContext.addRoutes(asyncExceptionRoute());
        //camelContext.addRoutes(new ThreadsQueueTestRoute());
        ActiveMqRoutes.createRoutes(camelContext);

        // start is not blocking
        camelContext.start();

        Thread.sleep(60 * 60 * 1000);
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
}
