package org.xiaobai.prettylike.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;
import org.xiaobai.prettylike.listener.thumb.msg.ThumbEvent;

/**
 * Pulsar DEMO
 */
@Slf4j
@Component
public class PulsarBootHelloWorld {

    ApplicationRunner runner(PulsarTemplate<ThumbEvent> pulsarTemplate){
        ThumbEvent thumbEvent = new ThumbEvent();
        for (int i = 0; i < 10000; i++) {
            try {
                pulsarTemplate.send("thumb-topic", thumbEvent);
            }catch (Exception e){
                log.error("发送消息异常:", e);
            }
            // ThreadUtil.sleep(500);
            log.info("Pulsar boot-hello-pulsar-topic" + (i + "----------------------------"));
        }
        return e -> {};
    }

    @PulsarListener(subscriptionName = "hello-pulsar-sub", topics = "hello-pulsar-topic")
    void listen(String message) {
        System.out.println("Message Received: " + message);
    }
}
