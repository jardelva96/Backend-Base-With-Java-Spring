package com.retail.inventory.service;

import com.retail.inventory.entity.InventoryItem;
import com.retail.inventory.kafka.InventoryEventProducer;
import com.retail.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        inventoryItem = new InventoryItem(1L, 100, "Warehouse A");
        inventoryItem.setId(1L);
    }

    @Test
    void getAllInventory_ShouldReturnListOfItems() {
        List<InventoryItem> items = Arrays.asList(inventoryItem);
        when(inventoryRepository.findAll()).thenReturn(items);

        List<InventoryItem> result = inventoryService.getAllInventory();

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getQuantity());
        verify(inventoryRepository, times(1)).findAll();
    }

    @Test
    void getInventoryById_ShouldReturnItem() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        Optional<InventoryItem> result = inventoryService.getInventoryById(1L);

        assertTrue(result.isPresent());
        assertEquals(100, result.get().getQuantity());
        verify(inventoryRepository, times(1)).findById(1L);
    }

    @Test
    void createInventoryItem_ShouldSaveItemAndSendEvent() {
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        InventoryItem result = inventoryService.createInventoryItem(inventoryItem);

        assertNotNull(result);
        assertEquals(100, result.getQuantity());
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
        verify(inventoryEventProducer, times(1)).sendInventoryCreatedEvent(any(InventoryItem.class));
    }

    @Test
    void updateInventoryQuantity_ShouldUpdateQuantityAndSendEvent() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        InventoryItem result = inventoryService.updateInventoryQuantity(1L, 150);

        assertEquals(150, result.getQuantity());
        verify(inventoryRepository, times(1)).findById(1L);
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
        verify(inventoryEventProducer, times(1)).sendInventoryUpdatedEvent(any(InventoryItem.class));
    }

    @Test
    void restockInventory_ShouldIncreaseQuantityAndSendEvent() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        InventoryItem result = inventoryService.restockInventory(1L, 50);

        assertEquals(150, result.getQuantity());
        verify(inventoryRepository, times(1)).findById(1L);
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
        verify(inventoryEventProducer, times(1)).sendInventoryRestockedEvent(any(InventoryItem.class), eq(50));
    }

    @Test
    void reserveInventory_ShouldDecreaseQuantityAndSendEvent() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        InventoryItem result = inventoryService.reserveInventory(1L, 30);

        assertEquals(70, result.getQuantity());
        verify(inventoryRepository, times(1)).findByProductId(1L);
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
        verify(inventoryEventProducer, times(1)).sendInventoryReservedEvent(any(InventoryItem.class), eq(30));
    }

    @Test
    void reserveInventory_ShouldThrowExceptionWhenInsufficientStock() {
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventoryItem));

        assertThrows(RuntimeException.class, () -> {
            inventoryService.reserveInventory(1L, 150);
        });

        verify(inventoryRepository, times(1)).findByProductId(1L);
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
        verify(inventoryEventProducer, never()).sendInventoryReservedEvent(any(InventoryItem.class), anyInt());
    }
}
