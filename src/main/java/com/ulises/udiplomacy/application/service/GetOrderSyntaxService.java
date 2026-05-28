package com.ulises.udiplomacy.application.service;

import com.ulises.udiplomacy.application.port.input.GetOrderSyntaxUseCase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetOrderSyntaxService implements GetOrderSyntaxUseCase {

    @Override
    public Map<String, Object> execute() {
        Map<String, Object> syntax = new LinkedHashMap<>();
        syntax.put("format", "UNIT_TYPE PROVINCE ACTION [TARGET] [AUXILIARY]");
        syntax.put("unitTypes", List.of("ARMY (or A)", "FLEET (or F)"));

        Map<String, Object> hold = new LinkedHashMap<>();
        hold.put("syntax", "ARMY LON H");
        hold.put("description", "Unit holds position");
        syntax.put("HOLD", hold);

        Map<String, Object> move = new LinkedHashMap<>();
        move.put("syntax", "ARMY LON - YOR");
        move.put("description", "Unit moves from source to target");
        syntax.put("MOVE", move);

        Map<String, Object> support = new LinkedHashMap<>();
        support.put("syntax", "ARMY LON S YOR");
        support.put("description", "Unit in LON supports unit in YOR (target is optional for support-hold)");
        syntax.put("SUPPORT (hold)", support);

        Map<String, Object> supportMove = new LinkedHashMap<>();
        supportMove.put("syntax", "ARMY LON S YOR - WAL");
        supportMove.put("description", "Unit in LON supports unit in YOR moving to WAL");
        syntax.put("SUPPORT (move)", supportMove);

        Map<String, Object> convoy = new LinkedHashMap<>();
        convoy.put("syntax", "FLEET ENG C ARMY LON - PIC");
        convoy.put("description", "Fleet in ENG conveys army from LON to PIC");
        syntax.put("CONVOY", convoy);

        Map<String, Object> retreat = new LinkedHashMap<>();
        retreat.put("syntax", "ARMY PAR R BRE");
        retreat.put("description", "Dislodged unit retreats to specified province");
        syntax.put("RETREAT", retreat);

        Map<String, Object> disband = new LinkedHashMap<>();
        disband.put("syntax", "ARMY PAR D");
        disband.put("description", "Disband a unit (used during retreat or build phase)");
        syntax.put("DISBAND", disband);

        Map<String, Object> build = new LinkedHashMap<>();
        build.put("syntax", "ARMY LON B EDI");
        build.put("description", "Build a new unit at a home supply center");
        syntax.put("BUILD", build);

        return syntax;
    }
}
