package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.OrderType;

import java.util.Objects;
import java.util.Optional;

public final class Order {
    private final OrderType type;
    private final Unit unit;
    private final Territory source;
    private final Territory target;
    private final Territory auxiliary;

    public Order(OrderType type, Unit unit, Territory source, Territory target, Territory auxiliary) {
        this.type = type;
        this.unit = unit;
        this.source = source;
        this.target = target;
        this.auxiliary = auxiliary;
    }

    public OrderType type() { return type; }
    public Unit unit() { return unit; }
    public Territory source() { return source; }
    public Optional<Territory> target() { return Optional.ofNullable(target); }
    public Optional<Territory> auxiliary() { return Optional.ofNullable(auxiliary); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return type == order.type
                && Objects.equals(unit, order.unit)
                && Objects.equals(source, order.source)
                && Objects.equals(target, order.target)
                && Objects.equals(auxiliary, order.auxiliary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, unit, source, target, auxiliary);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(unit.unitType()).append(" ").append(source.provinceName());
        switch (type) {
            case HOLD -> sb.append(" H");
            case MOVE -> sb.append(" - ").append(target.provinceName());
            case SUPPORT -> {
                sb.append(" S ");
                auxiliary().ifPresent(a -> sb.append(a.provinceName()));
            }
            case CONVOY -> {
                sb.append(" C ");
                auxiliary().ifPresent(a -> sb.append(a.provinceName()));
            }
            case RETREAT -> sb.append(" R ").append(target.provinceName());
            case BUILD -> sb.append(" B ").append(target.provinceName());
            case DISBAND -> sb.append(" D");
        }
        return sb.toString();
    }
}
