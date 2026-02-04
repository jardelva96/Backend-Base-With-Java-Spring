package com.retail.inventory.kafka;

import com.retail.inventory.entity.InventoryItem;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventProducer {
    private static final String TOPIC = "inventory-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendInventoryCreatedEvent(InventoryItem item) {
        String message = String.format("CREATED:InventoryItem[id=%d, productId=%d, quantity=%d]",
                item.getId(), item.getProductId(), item.getQuantity());
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendInventoryUpdatedEvent(InventoryItem item) {
        String message = String.format("UPDATED:InventoryItem[id=%d, productId=%d, quantity=%d]",
                item.getId(), item.getProductId(), item.getQuantity());
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendInventoryRestockedEvent(InventoryItem item, Integer additionalQuantity) {
        String message = String.format("RESTOCKED:InventoryItem[id=%d, productId=%d, quantity=%d, added=%d]",
                item.getId(), item.getProductId(), item.getQuantity(), additionalQuantity);
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendInventoryReservedEvent(InventoryItem item, Integer reservedQuantity) {
        String message = String.format("RESERVED:InventoryItem[id=%d, productId=%d, remainingQuantity=%d, reserved=%d]",
                item.getId(), item.getProductId(), item.getQuantity(), reservedQuantity);
        kafkaTemplate.send(TOPIC, message);
    }

    public void sendInventoryDeletedEvent(Long itemId) {
        String message = String.format("DELETED:InventoryItem[id=%d]", itemId);
        kafkaTemplate.send(TOPIC, message);
    }
}
