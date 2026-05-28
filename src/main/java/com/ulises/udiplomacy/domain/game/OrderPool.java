package com.ulises.udiplomacy.domain.game;

import java.util.*;

public final class OrderPool {
    private final List<Order> orders;

    public OrderPool() {
        this.orders = new ArrayList<>();
    }

    public OrderPool(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
    }

    public void add(Order order) {
        orders.add(order);
    }

    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public int size() { return orders.size(); }

    public Order remove(int index) {
        return orders.remove(index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderPool orderPool)) return false;
        return Objects.equals(orders, orderPool.orders);
    }

    @Override
    public int hashCode() { return Objects.hashCode(orders); }
}
