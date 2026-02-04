package com.retail.inventory.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void consumeOrderEvent(String message) {
        logger.info("Received order event: {}", message);
        
        // Process order event to update inventory
        if (message.startsWith("CREATED:")) {
            logger.info("Processing order creation event for inventory update");
        } else if (message.startsWith("CANCELLED:")) {
            logger.info("Processing order cancellation event for inventory restoration");
        }
    }
}
