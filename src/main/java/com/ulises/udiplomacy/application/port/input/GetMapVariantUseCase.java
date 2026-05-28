package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.MapVariant;

public interface GetMapVariantUseCase {
    MapVariant execute(String id);
}
