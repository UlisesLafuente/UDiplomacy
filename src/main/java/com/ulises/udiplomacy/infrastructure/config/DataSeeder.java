package com.ulises.udiplomacy.infrastructure.config;

import com.ulises.udiplomacy.application.port.output.MapVariantRepository;
import com.ulises.udiplomacy.application.port.output.PasswordEncoder;
import com.ulises.udiplomacy.application.port.output.UserRepository;
import com.ulises.udiplomacy.domain.game.MapVariant;
import com.ulises.udiplomacy.domain.user.Role;
import com.ulises.udiplomacy.domain.user.User;
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

    private final MapVariantRepository mapRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResourceLoader resourceLoader;

    public DataSeeder(MapVariantRepository mapRepository, UserRepository userRepository,
                      PasswordEncoder passwordEncoder, ResourceLoader resourceLoader) {
        this.mapRepository = mapRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        try { seedDefaultMap(); } catch (Exception e) {
            log.warn("Could not seed default map: {}", e.getMessage());
        }
        try { seedAdminUser(); } catch (Exception e) {
            log.warn("Could not seed admin user: {}", e.getMessage());
        }
    }

    private void seedDefaultMap() {
        if (mapRepository.existsById(DEFAULT_MAP_ID)) {
            log.info("Default map '{}' already seeded", DEFAULT_MAP_ID);
            return;
        }

        try (InputStream is = resourceLoader.getResource("classpath:europe-classic.json").getInputStream()) {
            String json = new String(is.readAllBytes());
            MapVariant variant = new MapVariant(
                    DEFAULT_MAP_ID, "Classic Diplomacy Europe",
                    json, null, Instant.now());
            mapRepository.save(variant);
            log.info("Seeded default map '{}'", DEFAULT_MAP_ID);
        } catch (Exception e) {
            log.warn("Could not seed default map: {}", e.getMessage());
        }
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            log.info("Admin user already seeded");
            return;
        }

        String hashedPassword = passwordEncoder.encode("diplomacy");
        User admin = new User(UUID.randomUUID().toString(), "admin", hashedPassword, Role.ADMIN);
        userRepository.save(admin);
        log.info("Seeded admin user");
    }
}
