package com.ulises.udiplomacy.application.port.input;

import com.ulises.udiplomacy.domain.game.MapVariant;

public interface CreateMapVariantUseCase {
    MapVariant execute(String name, String mapJson, String svgContent);
}
