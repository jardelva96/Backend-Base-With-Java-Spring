package com.retail.inventory.service;

import com.retail.inventory.entity.InventoryItem;
import com.retail.inventory.kafka.InventoryEventProducer;
import com.retail.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryService(InventoryRepository inventoryRepository, InventoryEventProducer inventoryEventProducer) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    public List<InventoryItem> getAllInventory() {
        return inventoryRepository.findAll();
    }

    public Optional<InventoryItem> getInventoryById(Long id) {
        return inventoryRepository.findById(id);
    }

    public Optional<InventoryItem> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Transactional
    public InventoryItem createInventoryItem(InventoryItem item) {
        InventoryItem savedItem = inventoryRepository.save(item);
        inventoryEventProducer.sendInventoryCreatedEvent(savedItem);
        return savedItem;
    }

    @Transactional
    public InventoryItem updateInventoryQuantity(Long id, Integer quantity) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));

        item.setQuantity(quantity);
        InventoryItem updatedItem = inventoryRepository.save(item);
        inventoryEventProducer.sendInventoryUpdatedEvent(updatedItem);
        return updatedItem;
    }

    @Transactional
    public InventoryItem restockInventory(Long id, Integer additionalQuantity) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));

        item.setQuantity(item.getQuantity() + additionalQuantity);
        item.setLastRestockedAt(LocalDateTime.now());
        InventoryItem updatedItem = inventoryRepository.save(item);
        inventoryEventProducer.sendInventoryRestockedEvent(updatedItem, additionalQuantity);
        return updatedItem;
    }

    @Transactional
    public InventoryItem reserveInventory(Long productId, Integer quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found for product: " + productId));

        if (item.getQuantity() < quantity) {
            throw new com.retail.inventory.exception.InsufficientInventoryException(
                    "Insufficient inventory for product: " + productId + ". Available: " + item.getQuantity() + ", Requested: " + quantity);
        }

        item.setQuantity(item.getQuantity() - quantity);
        InventoryItem updatedItem = inventoryRepository.save(item);
        inventoryEventProducer.sendInventoryReservedEvent(updatedItem, quantity);
        return updatedItem;
    }

    @Transactional
    public void deleteInventoryItem(Long id) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));
        inventoryRepository.delete(item);
        inventoryEventProducer.sendInventoryDeletedEvent(id);
    }
}
