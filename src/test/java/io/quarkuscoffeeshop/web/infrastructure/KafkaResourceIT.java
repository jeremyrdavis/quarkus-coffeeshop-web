package io.quarkuscoffeeshop.web.infrastructure;

import io.quarkuscoffeeshop.domain.EventType;
import io.quarkuscoffeeshop.domain.Item;
import io.quarkuscoffeeshop.domain.OrderInEvent;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class KafkaResourceIT extends BaseTestContainersIT{

    Logger logger = LoggerFactory.getLogger(KafkaResourceIT.class);

    static final String CONSUMER_TOPIC = "orders";

    static final String PRODUCER_TOPIC = "orders";

    WebTarget target;

    public KafkaResourceIT() {
        super(PRODUCER_TOPIC, CONSUMER_TOPIC);
    }

    @Test
    public void testSingleUpdate() {

        ArrayList<InboundSseEvent> events = new ArrayList<>();

        Client client = ClientBuilder.newClient();
        target = client.target("http://localhost:8081/dashboard/stream");
        SseEventSource msgEventSource = SseEventSource.target(target).build();
        msgEventSource.register(event -> {
            logger.info(event.readData());
            events.add(event);
        });
        msgEventSource.open();
        assertTrue(msgEventSource.isOpen());

        OrderInEvent orderEvent = new OrderInEvent(
                EventType.BEVERAGE_ORDER_IN,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "Minnie",
                Item.CAPPUCCINO);

        ProducerRecord<String, String> producerRecord = new ProducerRecord(
                PRODUCER_TOPIC,
                orderEvent.orderId,
                jsonb.toJson(orderEvent).toString());

        kafkaProducer.send(producerRecord);

        await().atLeast(5, TimeUnit.SECONDS).until(() -> events.size() >= 1);
        assertTrue(true);
//        logger.info(events.get(0).toString());
    }
}
