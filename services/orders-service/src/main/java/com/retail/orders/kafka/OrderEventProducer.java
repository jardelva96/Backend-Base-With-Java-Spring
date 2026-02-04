package com.retail.orders.kafka;

import com.retail.orders.entity.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {
    private static final String TOPIC = "order-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(Order order) {
        String message = String.format("CREATED:Order[id=%d, customerId=%d, productId=%d, quantity=%d, status=%s]",
                order.getId(), order.getCustomerId(), order.getProductId(), order.getQuantity(), order.getStatus());
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendOrderStatusChangedEvent(Order order) {
        String message = String.format("STATUS_CHANGED:Order[id=%d, status=%s]",
                order.getId(), order.getStatus());
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendOrderCancelledEvent(Long orderId) {
        String message = String.format("CANCELLED:Order[id=%d]", orderId);
        kafkaTemplate.send(TOPIC, message);
    }
}
