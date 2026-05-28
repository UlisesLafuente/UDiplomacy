package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.MapVariant;

import java.util.List;

public interface ListMapVariantsUseCase {
    List<MapVariant> execute();
}
