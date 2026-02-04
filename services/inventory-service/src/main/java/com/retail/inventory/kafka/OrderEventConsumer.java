package com.retail.inventory.kafka;

import com.retail.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final InventoryService inventoryService;

    public OrderEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void consumeOrderEvent(String message) {
        logger.info("Received order event: {}", message);
        
        try {
            if (message.startsWith("CREATED:")) {
                logger.info("Processing order creation event for inventory update");
                // Parse message to extract productId and quantity
                // Example message: "CREATED:Order[id=1, customerId=100, productId=1, quantity=2, status=PENDING]"
                Long productId = extractProductId(message);
                Integer quantity = extractQuantity(message);
                if (productId != null && quantity != null) {
                    inventoryService.reserveInventory(productId, quantity);
                    logger.info("Reserved {} units of product {} for order", quantity, productId);
                }
            } else if (message.startsWith("CANCELLED:")) {
                logger.info("Processing order cancellation event - inventory restoration would happen here");
                // In a real system, we would need to store the original order details to restore inventory
            }
        } catch (Exception e) {
            logger.error("Error processing order event: {}", message, e);
        }
    }

    private Long extractProductId(String message) {
        try {
            int start = message.indexOf("productId=") + 10;
            int end = message.indexOf(",", start);
            if (end == -1) end = message.indexOf("]", start);
            return Long.parseLong(message.substring(start, end).trim());
        } catch (Exception e) {
            logger.error("Failed to extract productId from message: {}", message);
            return null;
        }
    }

    private Integer extractQuantity(String message) {
        try {
            int start = message.indexOf("quantity=") + 9;
            int end = message.indexOf(",", start);
            if (end == -1) end = message.indexOf("]", start);
            return Integer.parseInt(message.substring(start, end).trim());
        } catch (Exception e) {
            logger.error("Failed to extract quantity from message: {}", message);
            return null;
        }
    }
}
