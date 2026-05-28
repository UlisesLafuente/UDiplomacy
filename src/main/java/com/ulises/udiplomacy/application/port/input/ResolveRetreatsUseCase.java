package com.ulises.udiplomacy.application.port.input;

import java.util.List;

public interface ResolveRetreatsUseCase {
    void execute(String gameId, List<String> rawOrders);
}
