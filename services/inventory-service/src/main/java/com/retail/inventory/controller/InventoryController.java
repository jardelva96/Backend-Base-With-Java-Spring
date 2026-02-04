package com.retail.inventory.controller;

import com.retail.inventory.entity.InventoryItem;
import com.retail.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getInventoryById(@PathVariable Long id) {
        return inventoryService.getInventoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryItem> getInventoryByProductId(@PathVariable Long productId) {
        return inventoryService.getInventoryByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<InventoryItem> createInventoryItem(@Valid @RequestBody InventoryItem item) {
        InventoryItem createdItem = inventoryService.createInventoryItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<InventoryItem> updateQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer quantity = body.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().build();
            }
            InventoryItem updatedItem = inventoryService.updateInventoryQuantity(id, quantity);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<InventoryItem> restockInventory(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer additionalQuantity = body.get("additionalQuantity");
            if (additionalQuantity == null) {
                return ResponseEntity.badRequest().build();
            }
            InventoryItem updatedItem = inventoryService.restockInventory(id, additionalQuantity);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/product/{productId}/reserve")
    public ResponseEntity<InventoryItem> reserveInventory(@PathVariable Long productId, @RequestBody Map<String, Integer> body) {
        try {
            Integer quantity = body.get("quantity");
            if (quantity == null) {
                return ResponseEntity.badRequest().build();
            }
            InventoryItem updatedItem = inventoryService.reserveInventory(productId, quantity);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable Long id) {
        try {
            inventoryService.deleteInventoryItem(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
