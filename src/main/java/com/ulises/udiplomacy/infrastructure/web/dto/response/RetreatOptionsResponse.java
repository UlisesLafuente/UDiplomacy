package com.ulises.udiplomacy.infrastructure.web.dto.response;

import java.util.List;

public record RetreatOptionsResponse(List<DislodgedUnitOptions> units) {
    public record DislodgedUnitOptions(String type, String nation, String province,
                                        List<String> retreatOptions) {}
}
