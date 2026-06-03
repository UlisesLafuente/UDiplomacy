package com.ulises.udiplomacy.infrastructure.config;

import com.ulises.udiplomacy.application.port.input.*;
import com.ulises.udiplomacy.application.port.output.*;
import com.ulises.udiplomacy.application.service.*;
import com.ulises.udiplomacy.infrastructure.events.EventPublisherAdapter;
import com.ulises.udiplomacy.infrastructure.map.MapLoader;
import com.ulises.udiplomacy.infrastructure.persistence.mongodb.MapVariantRepositoryAdapter;
import com.ulises.udiplomacy.infrastructure.persistence.mongodb.MongoGameRepositoryAdapter;
import com.ulises.udiplomacy.infrastructure.persistence.mongodb.SpringDataMongoGameRepository;
import com.ulises.udiplomacy.infrastructure.persistence.mongodb.SpringDataMongoMapVariantRepository;
import com.ulises.udiplomacy.infrastructure.persistence.postgres.projection.GameProjectionRepositoryAdapter;
import com.ulises.udiplomacy.infrastructure.persistence.postgres.projection.SpringDataJpaGameProjectionRepository;
import com.ulises.udiplomacy.infrastructure.persistence.postgres.user.JpaUserRepositoryAdapter;
import com.ulises.udiplomacy.infrastructure.persistence.postgres.user.SpringDataJpaUserRepository;
import com.ulises.udiplomacy.domain.game.services.ConflictResolver;
import com.ulises.udiplomacy.domain.game.services.OrderParser;
import com.ulises.udiplomacy.infrastructure.web.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class BeanConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    public GameRepository gameRepository(SpringDataMongoGameRepository springRepo) {
        return new MongoGameRepositoryAdapter(springRepo);
    }

    @Bean
    public UserRepository userRepository(SpringDataJpaUserRepository springRepo) {
        return new JpaUserRepositoryAdapter(springRepo);
    }

    @Bean
    public GameProjectionRepository projectionRepository(SpringDataJpaGameProjectionRepository springRepo) {
        return new GameProjectionRepositoryAdapter(springRepo);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        var springEncoder = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return springEncoder.encode(rawPassword);
            }
            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return springEncoder.matches(rawPassword, encodedPassword);
            }
        };
    }

    @Bean
    public EventPublisher eventPublisher(GameProjectionRepository projectionRepository) {
        return new EventPublisherAdapter(projectionRepository);
    }

    @Bean
    public OrderParser orderParser() {
        return new OrderParser();
    }

    @Bean
    public ConflictResolver conflictResolver() {
        return new ConflictResolver();
    }

    @Bean
    public MapLoader mapLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        return new MapLoader(resourceLoader, objectMapper);
    }

    @Bean
    public MapVariantRepository mapVariantRepository(SpringDataMongoMapVariantRepository springRepo) {
        return new MapVariantRepositoryAdapter(springRepo);
    }

    @Bean
    public CreateGameUseCase createGameUseCase(GameRepository gameRepository,
                                                GameProjectionRepository projectionRepository,
                                                MapVariantRepository mapVariantRepository,
                                                UserRepository userRepository,
                                                ObjectMapper objectMapper) {
        return new CreateGameService(gameRepository, projectionRepository,
                mapVariantRepository, userRepository, objectMapper);
    }

    @Bean
    public GetGameUseCase getGameUseCase(GameRepository gameRepository) {
        return new GetGameService(gameRepository);
    }

    @Bean
    public SubmitOrderUseCase submitOrderUseCase(GameRepository gameRepository, OrderParser orderParser) {
        return new SubmitOrderService(gameRepository, orderParser);
    }

    @Bean
    public ExecuteOrdersUseCase executeOrdersUseCase(GameRepository gameRepository,
                                                      ConflictResolver conflictResolver,
                                                      EventPublisher eventPublisher,
                                                      GameProjectionRepository projectionRepository) {
        return new ExecuteOrdersService(gameRepository, conflictResolver, eventPublisher, projectionRepository);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository,
                                                    PasswordEncoder passwordEncoder) {
        return new RegisterUserService(userRepository, passwordEncoder);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(UserRepository userRepository,
                                                            PasswordEncoder passwordEncoder,
                                                            JwtTokenProvider tokenProvider) {
        return new AuthenticateUserService(userRepository, passwordEncoder, tokenProvider);
    }

    @Bean
    public ListUserGamesUseCase listUserGamesUseCase(GameProjectionRepository projectionRepository) {
        return new ListUserGamesService(projectionRepository);
    }

    @Bean
    public ListAllGamesUseCase listAllGamesUseCase(GameProjectionRepository projectionRepository) {
        return new ListAllGamesService(projectionRepository);
    }

    @Bean
    public SaveGameUseCase saveGameUseCase(GameRepository gameRepository, EventPublisher eventPublisher) {
        return new SaveGameService(gameRepository, eventPublisher);
    }

    @Bean
    public DeleteGameUseCase deleteGameUseCase(GameRepository gameRepository,
                                                 GameProjectionRepository projectionRepository) {
        return new DeleteGameService(gameRepository, projectionRepository);
    }

    @Bean
    public GetPendingDislodgedUnitsUseCase getPendingDislodgedUnitsUseCase(GameRepository gameRepository) {
        return new GetPendingDislodgedUnitsService(gameRepository);
    }

    @Bean
    public GetBuildOptionsUseCase getBuildOptionsUseCase(GameRepository gameRepository) {
        return new GetBuildOptionsService(gameRepository);
    }

    @Bean
    public ResolveRetreatsUseCase resolveRetreatsUseCase(GameRepository gameRepository,
                                                          OrderParser orderParser,
                                                          ConflictResolver conflictResolver) {
        return new ResolveRetreatsService(gameRepository, orderParser, conflictResolver);
    }

    @Bean
    public ResolveBuildsUseCase resolveBuildsUseCase(GameRepository gameRepository,
                                                      OrderParser orderParser,
                                                      EventPublisher eventPublisher) {
        return new ResolveBuildsService(gameRepository, orderParser, eventPublisher);
    }

    @Bean
    public UndoLastTurnUseCase undoLastTurnUseCase(GameRepository gameRepository) {
        return new UndoLastTurnService(gameRepository);
    }

    @Bean
    public AdvancePhaseUseCase advancePhaseUseCase(GameRepository gameRepository) {
        return new AdvancePhaseService(gameRepository);
    }

    @Bean
    public RemoveOrderUseCase removeOrderUseCase(GameRepository gameRepository) {
        return new RemoveOrderService(gameRepository);
    }

    @Bean
    public RewindGameUseCase rewindGameUseCase(GameRepository gameRepository) {
        return new RewindGameService(gameRepository);
    }

    @Bean
    public GetGameHistoryUseCase getGameHistoryUseCase(GameRepository gameRepository) {
        return new GetGameHistoryService(gameRepository);
    }

    @Bean
    public GetOrderSyntaxUseCase getOrderSyntaxUseCase() {
        return new GetOrderSyntaxService();
    }

    @Bean
    public ListMapVariantsUseCase listMapVariantsUseCase(MapVariantRepository repository) {
        return new ListMapVariantsService(repository);
    }

    @Bean
    public GetMapVariantUseCase getMapVariantUseCase(MapVariantRepository repository) {
        return new GetMapVariantService(repository);
    }

    @Bean
    public SvgValidator svgValidator() {
        return new SvgValidator();
    }

    @Bean
    public CreateMapVariantUseCase createMapVariantUseCase(MapVariantRepository repository,
                                                            SvgValidator svgValidator) {
        return new CreateMapVariantService(repository, svgValidator);
    }

    @Bean
    public GetMapVariantSvgUseCase getMapVariantSvgUseCase(MapVariantRepository repository) {
        return new GetMapVariantSvgService(repository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository repository) {
        return new ListUsersService(repository);
    }

    @Bean
    public UpdateUserRoleUseCase updateUserRoleUseCase(UserRepository repository) {
        return new UpdateUserRoleService(repository);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepository repository) {
        return new DeleteUserService(repository);
    }
}
