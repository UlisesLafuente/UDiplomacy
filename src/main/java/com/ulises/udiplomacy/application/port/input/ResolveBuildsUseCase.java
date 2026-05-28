package com.ulises.udiplomacy.application.port.input;

import java.util.List;

public interface ResolveBuildsUseCase {
    void execute(String gameId, List<String> rawOrders);
}
