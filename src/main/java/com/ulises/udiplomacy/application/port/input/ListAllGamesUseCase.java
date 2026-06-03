package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.user.GameReference;

import java.util.List;

public interface ListAllGamesUseCase {
    List<GameReference> execute();
}
