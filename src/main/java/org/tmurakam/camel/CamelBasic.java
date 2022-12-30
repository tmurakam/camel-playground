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

    public static void main(String[] args) throws Exception {
        log.info("started");

        // create a CamelContext
        CamelContext camel = new DefaultCamelContext();

        camel.addRoutes(route1());

        // start is not blocking
        camel.start();

        Thread.sleep(60 * 60 * 1000);
    }

    static RouteBuilder route1() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:foo?delay=0&period=10000")
                        .log("Hello Camel");
            }
        };
    }
}
