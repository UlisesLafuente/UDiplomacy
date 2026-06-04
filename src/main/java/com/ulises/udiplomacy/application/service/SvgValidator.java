package com.ulises.udiplomacy.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SvgValidator {

    private static final Pattern DATA_CODE_PATTERN = Pattern.compile("data-code=\"([A-Z0-9]+)\"");

    public void validate(String svgContent, String mapJson) {
        List<String> errors = new ArrayList<>();

        if (svgContent == null || svgContent.isBlank()) {
            errors.add("SVG content is required");
        }
        if (mapJson == null || mapJson.isBlank()) {
            errors.add("Map JSON is required");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        Set<String> svgCodes = extractSvgCodes(svgContent);
        Set<String> jsonCodes = extractJsonCodes(mapJson);

        if (svgCodes.isEmpty()) {
            errors.add("No data-code=\"XXX\" attributes found in SVG. "
                    + "Each province path must have a data-code attribute matching the JSON province name.");
        }

        Set<String> missingInSvg = new HashSet<>(jsonCodes);
        missingInSvg.removeAll(svgCodes);
        if (!missingInSvg.isEmpty()) {
            errors.add("Provinces present in JSON but missing data-code in SVG: "
                    + String.join(", ", missingInSvg.stream().sorted().toList()));
        }

        if (svgCodes.isEmpty() && jsonCodes.isEmpty()) {
            errors.add("No provinces found in JSON. The 'provinces' array must not be empty.");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Map variant validation failed: " + String.join("; ", errors));
        }
    }

    Set<String> extractSvgCodes(String svgContent) {
        Set<String> codes = new HashSet<>();
        Matcher matcher = DATA_CODE_PATTERN.matcher(svgContent);
        while (matcher.find()) {
            codes.add(matcher.group(1));
        }
        return codes;
    }

    @SuppressWarnings("unchecked")
    Set<String> extractJsonCodes(String mapJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> root = mapper.readValue(mapJson, new TypeReference<>() {});
            List<Map<String, Object>> provinces = (List<Map<String, Object>>) root.get("provinces");
            if (provinces == null) return Set.of();
            return provinces.stream()
                    .map(p -> (String) p.get("name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid map JSON: " + e.getMessage(), e);
        }
    }
}
