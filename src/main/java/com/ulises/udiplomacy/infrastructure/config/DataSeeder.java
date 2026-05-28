package com.ulises.udiplomacy.infrastructure.config;

import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.domain.game.MapVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_MAP_ID = "europe-classic";

    private final MapVariantRepository repository;
    private final ResourceLoader resourceLoader;

    public DataSeeder(MapVariantRepository repository, ResourceLoader resourceLoader) {
        this.repository = repository;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        if (repository.existsById(DEFAULT_MAP_ID)) {
            log.info("Default map '{}' already seeded", DEFAULT_MAP_ID);
            return;
        }

        try (InputStream is = resourceLoader.getResource("classpath:europe-classic.json").getInputStream()) {
            String json = new String(is.readAllBytes());
            MapVariant variant = new MapVariant(
                    DEFAULT_MAP_ID, "Classic Diplomacy Europe",
                    json, null, Instant.now());
            repository.save(variant);
            log.info("Seeded default map '{}'", DEFAULT_MAP_ID);
        } catch (Exception e) {
            log.warn("Could not seed default map: {}", e.getMessage());
        }
    }
}
