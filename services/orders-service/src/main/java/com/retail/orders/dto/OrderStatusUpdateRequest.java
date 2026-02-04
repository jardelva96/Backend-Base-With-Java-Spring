package com.retail.orders.dto;

import com.retail.orders.entity.Order;

public class OrderStatusUpdateRequest {
    private Order.OrderStatus status;

    public OrderStatusUpdateRequest() {
    }

    public OrderStatusUpdateRequest(Order.OrderStatus status) {
        this.status = status;
    }

    public Order.OrderStatus getStatus() {
        return status;
    }

    public void setStatus(Order.OrderStatus status) {
        this.status = status;
    }
}
