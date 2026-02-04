package com.retail.orders.service;

import com.retail.orders.entity.Order;
import com.retail.orders.kafka.OrderEventProducer;
import com.retail.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(100L, 1L, 2, new BigDecimal("199.98"));
        order.setId(1L);
        order.setStatus(Order.OrderStatus.PENDING);
    }

    @Test
    void getAllOrders_ShouldReturnListOfOrders() {
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.getAllOrders();

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getCustomerId());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getOrderById_ShouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.getOrderById(1L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getCustomerId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void createOrder_ShouldSaveOrderAndSendEvent() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.createOrder(order);

        assertNotNull(result);
        assertEquals(100L, result.getCustomerId());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderCreatedEvent(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatusAndSendEvent() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED);

        assertEquals(Order.OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderStatusChangedEvent(any(Order.class));
    }

    @Test
    void cancelOrder_ShouldCancelOrderAndSendEvent() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.cancelOrder(1L);

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventProducer, times(1)).sendOrderCancelledEvent(1L);
    }

    @Test
    void getOrdersByCustomerId_ShouldReturnFilteredOrders() {
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByCustomerId(100L)).thenReturn(orders);

        List<Order> result = orderService.getOrdersByCustomerId(100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getCustomerId());
        verify(orderRepository, times(1)).findByCustomerId(100L);
    }
}
