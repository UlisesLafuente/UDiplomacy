package com.ulises.udiplomacy.domain.game;

import com.ulises.udiplomacy.domain.game.enums.OrderResult;

import java.util.Collections;
import java.util.Map;

public record ResolutionResult(
        DislodgementResult dislodgementResult,
        Map<Order, OrderResult> orderResults
) {
    public ResolutionResult {
        dislodgementResult = dislodgementResult;
        orderResults = Collections.unmodifiableMap(orderResults);
    }
}
