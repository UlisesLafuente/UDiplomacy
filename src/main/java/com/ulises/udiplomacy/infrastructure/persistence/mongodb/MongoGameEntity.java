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

    // --- MongoMapData ---

    static class MongoMapData {
        private String id;
        private String name;
        private List<MongoProvinceData> provinces;

        public MongoMapData() {}

        MongoMapData(GameMap gm) {
            this.id = gm.id();
            this.name = gm.name();
            this.provinces = gm.provinces().values().stream().map(MongoProvinceData::new).toList();
        }

        GameMap toDomain() {
            return new GameMap(id, name,
                    provinces.stream().map(MongoProvinceData::toDomain).toList());
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<MongoProvinceData> getProvinces() { return provinces; }
        public void setProvinces(List<MongoProvinceData> provinces) { this.provinces = provinces; }
    }

    // --- MongoProvinceData ---

    static class MongoProvinceData {
        private String name;
        private String type;
        private String homeNation;
        private boolean supplyCenter;
        private List<String> coasts;
        private Map<String, String> adjacencies;

        public MongoProvinceData() {}

        MongoProvinceData(Province p) {
            this.name = p.name();
            this.type = p.type().name();
            this.homeNation = p.homeNation().map(Nation::name).orElse(null);
            this.supplyCenter = p.isSupplyCenter();
            this.coasts = p.coasts().stream().map(Coast::name).toList();
            this.adjacencies = p.adjacencies().entrySet().stream()
                    .collect(HashMap::new, (m, e) -> m.put(e.getKey(),
                            e.getValue() != null ? e.getValue().name() : null), Map::putAll);
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

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHomeNation() { return homeNation; }
        public void setHomeNation(String homeNation) { this.homeNation = homeNation; }
        public boolean isSupplyCenter() { return supplyCenter; }
        public void setSupplyCenter(boolean supplyCenter) { this.supplyCenter = supplyCenter; }
        public List<String> getCoasts() { return coasts; }
        public void setCoasts(List<String> coasts) { this.coasts = coasts; }
        public Map<String, String> getAdjacencies() { return adjacencies; }
        public void setAdjacencies(Map<String, String> adjacencies) { this.adjacencies = adjacencies; }
    }

    // --- MongoTurnData ---

    static class MongoTurnData {
        private String season;
        private int year;
        private String phase;
        private List<MongoUnitData> units;
        private List<MongoOrderData> orderPool;
        private List<MongoOrderData> resolvedOrders;
        private List<String> resolvedResults;

        public MongoTurnData() {}

        MongoTurnData(Turn t) {
            this.season = t.season().name();
            this.year = t.year();
            this.phase = t.phase().name();
            this.units = t.units().stream().map(MongoUnitData::new).toList();
            this.orderPool = t.orderPool().orders().stream().map(MongoOrderData::new).toList();
            this.resolvedOrders = t.resolvedOrders().stream().map(MongoOrderData::new).toList();
            this.resolvedResults = t.resolvedOrders().stream()
                    .map(o -> t.resolvedResults().getOrDefault(o,
                            OrderResult.SUCCESS).name())
                    .toList();
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

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }
        public List<MongoUnitData> getUnits() { return units; }
        public void setUnits(List<MongoUnitData> units) { this.units = units; }
        public List<MongoOrderData> getOrderPool() { return orderPool; }
        public void setOrderPool(List<MongoOrderData> orderPool) { this.orderPool = orderPool; }
        public List<MongoOrderData> getResolvedOrders() { return resolvedOrders; }
        public void setResolvedOrders(List<MongoOrderData> resolvedOrders) { this.resolvedOrders = resolvedOrders; }
        public List<String> getResolvedResults() { return resolvedResults; }
        public void setResolvedResults(List<String> resolvedResults) { this.resolvedResults = resolvedResults; }
    }

    // --- MongoUnitData ---

    static class MongoUnitData {
        private String unitType;
        private String nation;
        private String province;
        private String coast;

        public MongoUnitData() {}

        MongoUnitData(Unit u) {
            this.unitType = u.unitType().name();
            this.nation = u.nation().name();
            this.province = u.location().provinceName();
            this.coast = u.location().coast().map(Coast::name).orElse(null);
        }

        Unit toDomain() {
            Territory territory = coast != null
                    ? new Territory(province, new Coast(coast))
                    : new Territory(province);
            return new Unit(UnitType.valueOf(unitType),
                    nation != null ? new Nation(nation) : null,
                    territory);
        }

        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public String getNation() { return nation; }
        public void setNation(String nation) { this.nation = nation; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCoast() { return coast; }
        public void setCoast(String coast) { this.coast = coast; }
    }

    // --- MongoOrderData ---

    static class MongoOrderData {
        private String type;
        private String unitType;
        private String unitNation;
        private String sourceProvince;
        private String sourceCoast;
        private String targetProvince;
        private String targetCoast;
        private String auxProvince;
        private String auxCoast;

        public MongoOrderData() {}

        MongoOrderData(Order o) {
            this.type = o.type().name();
            this.unitType = o.unit().unitType().name();
            this.unitNation = o.unit().nation() != null ? o.unit().nation().name() : null;
            this.sourceProvince = o.source().provinceName();
            this.sourceCoast = o.source().coast().map(Coast::name).orElse(null);
            this.targetProvince = o.target().map(Territory::provinceName).orElse(null);
            this.targetCoast = o.target().flatMap(Territory::coast).map(Coast::name).orElse(null);
            this.auxProvince = o.auxiliary().map(Territory::provinceName).orElse(null);
            this.auxCoast = o.auxiliary().flatMap(Territory::coast).map(Coast::name).orElse(null);
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

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        public String getUnitNation() { return unitNation; }
        public void setUnitNation(String unitNation) { this.unitNation = unitNation; }
        public String getSourceProvince() { return sourceProvince; }
        public void setSourceProvince(String sourceProvince) { this.sourceProvince = sourceProvince; }
        public String getSourceCoast() { return sourceCoast; }
        public void setSourceCoast(String sourceCoast) { this.sourceCoast = sourceCoast; }
        public String getTargetProvince() { return targetProvince; }
        public void setTargetProvince(String targetProvince) { this.targetProvince = targetProvince; }
        public String getTargetCoast() { return targetCoast; }
        public void setTargetCoast(String targetCoast) { this.targetCoast = targetCoast; }
        public String getAuxProvince() { return auxProvince; }
        public void setAuxProvince(String auxProvince) { this.auxProvince = auxProvince; }
        public String getAuxCoast() { return auxCoast; }
        public void setAuxCoast(String auxCoast) { this.auxCoast = auxCoast; }
    }
}
