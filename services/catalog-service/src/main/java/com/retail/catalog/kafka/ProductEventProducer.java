package com.retail.catalog.kafka;

import com.retail.catalog.entity.Product;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventProducer {
    private static final String TOPIC = "product-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProductEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductCreatedEvent(Product product) {
        String message = String.format("CREATED:Product[id=%d, name=%s, price=%s]",
                product.getId(), product.getName(), product.getPrice());
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendProductUpdatedEvent(Product product) {
        String message = String.format("UPDATED:Product[id=%d, name=%s, price=%s]",
                product.getId(), product.getName(), product.getPrice());
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendProductDeletedEvent(Long productId) {
        String message = String.format("DELETED:Product[id=%d]", productId);
        kafkaTemplate.send(TOPIC, message);
    }
}
