package org.tmurakam.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;

@Slf4j
public class ThreadsQueueTestRoute extends RouteBuilder {
    private int counter = 0;

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
}
