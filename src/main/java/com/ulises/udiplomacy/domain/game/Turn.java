package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.OrderResult;
import com.ulises.udiplomacy.domain.game.enums.Phase;
import com.ulises.udiplomacy.domain.game.enums.Season;

import java.util.*;

public final class Turn {
    private final Season season;
    private final int year;
    private final Phase phase;
    private final List<Unit> units;
    private final OrderPool orderPool;
    private final List<Order> resolvedOrders;
    private final Map<Order, OrderResult> resolvedResults;

    public Turn(Season season, int year, Phase phase, List<Unit> units) {
        this(season, year, phase, units, new OrderPool(), List.of(), Map.of());
    }

    public Turn(Season season, int year, Phase phase, List<Unit> units,
                OrderPool orderPool, List<Order> resolvedOrders) {
        this(season, year, phase, units, orderPool, resolvedOrders, Map.of());
    }

    public Turn(Season season, int year, Phase phase, List<Unit> units,
                OrderPool orderPool, List<Order> resolvedOrders,
                Map<Order, OrderResult> resolvedResults) {
        this.season = season;
        this.year = year;
        this.phase = phase;
        this.units = List.copyOf(units);
        this.orderPool = orderPool;
        this.resolvedOrders = List.copyOf(resolvedOrders);
        this.resolvedResults = Map.copyOf(resolvedResults);
    }

    public Season season() { return season; }
    public int year() { return year; }
    public Phase phase() { return phase; }
    public List<Unit> units() { return units; }
    public OrderPool orderPool() { return orderPool; }
    public List<Order> resolvedOrders() { return resolvedOrders; }
    public Map<Order, OrderResult> resolvedResults() { return resolvedResults; }

    public Turn withPhase(Phase newPhase) {
        return new Turn(season, year, newPhase, units, new OrderPool(), List.of(), Map.of());
    }

    public Turn withOrdersResolved(List<Order> resolved, Map<Order, OrderResult> results) {
        return new Turn(season, year, phase, units, orderPool, resolved, results);
    }

    public Turn withUnits(List<Unit> newUnits) {
        return new Turn(season, year, phase, newUnits, orderPool, resolvedOrders, resolvedResults);
    }

    public Turn withOrderPool(OrderPool pool) {
        return new Turn(season, year, phase, units, pool, resolvedOrders, resolvedResults);
    }

    public Turn next(Season nextSeason, int nextYear) {
        return new Turn(nextSeason, nextYear, Phase.ORDERS, units);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Turn turn)) return false;
        return year == turn.year
                && season == turn.season
                && phase == turn.phase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(season, year, phase);
    }
}
