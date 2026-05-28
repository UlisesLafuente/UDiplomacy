package com.ulises.udiplomacy.infrastructure.persistence.mongodb;

import com.ulises.udiplomacy.domain.game.*;
import com.ulises.udiplomacy.domain.game.enums.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Document(collection = "games")
public class MongoGameEntity {
    @Id
    private String id;
    private String state;
    private MongoMapData mapData;
    private MongoTurnData currentTurn;
    private List<MongoTurnData> turnHistory;
    private List<String> nations;
    private List<MongoOrderData> orderPool;
    private String winner;

    public MongoGameEntity() {}

    public MongoGameEntity(Game game) {
        this.id = game.gameId();
        this.state = game.state().name();
        this.mapData = new MongoMapData(game.gameMap());
        this.currentTurn = new MongoTurnData(game.currentTurn());
        this.turnHistory = game.turnHistory().stream().map(MongoTurnData::new).toList();
        this.nations = game.nations().stream().map(Nation::name).toList();
        this.orderPool = game.orderPool().orders().stream().map(MongoOrderData::new).toList();
        this.winner = game.winner().map(Nation::name).orElse(null);
    }

    public Game toDomain() {
        GameMap gameMap = mapData != null ? mapData.toDomain() : null;
        Set<Nation> nationSet = new HashSet<>();
        if (nations != null) {
            nations.forEach(n -> nationSet.add(new Nation(n)));
        }
        Game game = new Game(id, gameMap, nationSet);

        GameState restoredState = state != null ? GameState.valueOf(state) : GameState.WAITING;
        Turn restoredCurrentTurn = currentTurn != null ? currentTurn.toDomain() : null;
        List<Turn> restoredHistory = turnHistory != null
                ? turnHistory.stream().map(MongoTurnData::toDomain).toList() : List.of();
        OrderPool restoredPool = new OrderPool();
        if (orderPool != null) {
            orderPool.forEach(d -> restoredPool.add(d.toDomain()));
        }
        Nation restoredWinner = winner != null ? new Nation(winner) : null;

        game.restore(restoredState, restoredCurrentTurn, restoredHistory,
                restoredPool, null, restoredWinner);
        return game;
    }

    record MongoMapData(String id, String name, List<MongoProvinceData> provinces) {
        MongoMapData(GameMap gm) {
            this(gm.id(), gm.name(),
                    gm.provinces().values().stream().map(MongoProvinceData::new).toList());
        }
        GameMap toDomain() {
            return new GameMap(id, name,
                    provinces.stream().map(MongoProvinceData::toDomain).toList());
        }
    }

    record MongoProvinceData(String name, String type, String homeNation,
                              boolean supplyCenter, List<String> coasts,
                              Map<String, String> adjacencies) {
        MongoProvinceData(Province p) {
            this(p.name(), p.type().name(),
                    p.homeNation().map(Nation::name).orElse(null),
                    p.isSupplyCenter(),
                    p.coasts().stream().map(Coast::name).toList(),
                    p.adjacencies().entrySet().stream()
                            .collect(HashMap::new, (m, e) -> m.put(e.getKey(),
                                    e.getValue() != null ? e.getValue().name() : null), Map::putAll));
        }
        Province toDomain() {
            List<Coast> coastList = coasts != null
                    ? coasts.stream().map(Coast::new).toList() : List.of();
            Map<String, Coast> adjMap = adjacencies != null
                    ? adjacencies.entrySet().stream()
                    .collect(HashMap::new, (m, e) -> m.put(e.getKey(),
                            e.getValue() != null ? new Coast(e.getValue()) : null), Map::putAll)
                    : Map.of();
            return new Province(name, ProvinceType.valueOf(type),
                    homeNation != null ? new Nation(homeNation) : null,
                    supplyCenter, coastList, adjMap);
        }
    }

    record MongoTurnData(String season, int year, String phase,
                          List<MongoUnitData> units,
                          List<MongoOrderData> orderPool,
                          List<MongoOrderData> resolvedOrders,
                          List<String> resolvedResults) {
        MongoTurnData(Turn t) {
            this(t.season().name(), t.year(), t.phase().name(),
                    t.units().stream().map(MongoUnitData::new).toList(),
                    t.orderPool().orders().stream().map(MongoOrderData::new).toList(),
                    t.resolvedOrders().stream().map(MongoOrderData::new).toList(),
                    t.resolvedOrders().stream()
                            .map(o -> t.resolvedResults().getOrDefault(o,
                                    com.ulises.udiplomacy.domain.game.enums.OrderResult.SUCCESS).name())
                            .toList());
        }
        Turn toDomain() {
            Season s = Season.valueOf(season);
            Phase p = Phase.valueOf(phase);
            List<Unit> unitList = units != null
                    ? units.stream().map(MongoUnitData::toDomain).toList() : List.of();
            OrderPool pool = new OrderPool();
            if (orderPool != null) {
                orderPool.forEach(d -> pool.add(d.toDomain()));
            }
            List<Order> resolved = resolvedOrders != null
                    ? resolvedOrders.stream().map(MongoOrderData::toDomain).toList() : List.of();
            Map<Order, OrderResult> results = new HashMap<>();
            if (resolvedResults != null && resolved.size() == resolvedResults.size()) {
                for (int i = 0; i < resolved.size(); i++) {
                    results.put(resolved.get(i), OrderResult.valueOf(resolvedResults.get(i)));
                }
            }
            return new Turn(s, year, p, unitList, pool, resolved, results);
        }
    }

    record MongoUnitData(String unitType, String nation, String province, String coast) {
        MongoUnitData(Unit u) {
            this(u.unitType().name(), u.nation().name(),
                    u.location().provinceName(),
                    u.location().coast().map(Coast::name).orElse(null));
        }
        Unit toDomain() {
            Territory territory = coast != null
                    ? new Territory(province, new Coast(coast))
                    : new Territory(province);
            return new Unit(UnitType.valueOf(unitType),
                    nation != null ? new Nation(nation) : null,
                    territory);
        }
    }

    record MongoOrderData(String type, String unitType, String unitNation,
                           String sourceProvince, String sourceCoast,
                           String targetProvince, String targetCoast,
                           String auxProvince, String auxCoast) {
        MongoOrderData(Order o) {
            this(o.type().name(),
                    o.unit().unitType().name(), o.unit().nation() != null ? o.unit().nation().name() : null,
                    o.source().provinceName(), o.source().coast().map(Coast::name).orElse(null),
                    o.target().map(Territory::provinceName).orElse(null),
                    o.target().flatMap(Territory::coast).map(Coast::name).orElse(null),
                    o.auxiliary().map(Territory::provinceName).orElse(null),
                    o.auxiliary().flatMap(Territory::coast).map(Coast::name).orElse(null));
        }
        Order toDomain() {
            Territory source = sourceCoast != null
                    ? new Territory(sourceProvince, new Coast(sourceCoast))
                    : new Territory(sourceProvince);
            Territory target = targetProvince != null
                    ? (targetCoast != null
                        ? new Territory(targetProvince, new Coast(targetCoast))
                        : new Territory(targetProvince))
                    : null;
            Territory aux = auxProvince != null
                    ? (auxCoast != null
                        ? new Territory(auxProvince, new Coast(auxCoast))
                        : new Territory(auxProvince))
                    : null;
            Unit stubUnit = new Unit(UnitType.valueOf(unitType),
                    unitNation != null ? new Nation(unitNation) : null,
                    source);
            return new Order(OrderType.valueOf(type), stubUnit, source, target, aux);
        }
    }
}
