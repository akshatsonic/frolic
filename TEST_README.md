# Frolic Project - Test Suite

## Overview
Comprehensive test suite for the Frolic gamification platform covering both `frolic-core` and `frolic-services` modules.

## Test Structure

```
frolic/
├── frolic-core/
│   └── src/test/java/com/frolic/core/
│       ├── cache/store/
│       │   ├── RedisBudgetStoreTest.java
│       │   └── RedisResultStoreTest.java
│       ├── common/
│       │   ├── constant/
│       │   │   ├── KafkaTopicsTest.java
│       │   │   └── RedisKeysTest.java
│       │   └── util/
│       │       ├── IdGeneratorTest.java
│       │       ├── JsonUtilsTest.java
│       │       └── TimeUtilsTest.java
│       ├── engine/
│       │   ├── concurrency/
│       │   │   ├── AtomicBudgetDecrementerTest.java
│       │   │   └── IdempotencyHandlerTest.java
│       │   └── probability/
│       │       └── ProbabilityCalculatorTest.java
│       ├── messaging/producer/
│       │   └── PlayEventProducerTest.java
│       └── repository/jpa/
│           ├── CampaignRepositoryIntegrationTest.java
│           └── GameRepositoryIntegrationTest.java
│
└── frolic-services/
    └── src/test/java/com/frolic/services/
        ├── config/
        │   └── GlobalExceptionHandlerTest.java
        ├── consumer/
        │   └── PlayEventConsumerTest.java
        ├── controller/play/
        │   └── PlayControllerTest.java
        ├── service/
        │   ├── admin/
        │   │   └── CampaignServiceTest.java
        │   ├── allocation/
        │   │   └── RewardAllocationServiceTest.java
        │   └── play/
        │       └── PlayIngestionServiceTest.java
```

## Running Tests

### Prerequisites
- Java 21
- Maven 3.8+
- Docker (for integration tests with Testcontainers)

### Run All Tests
```bash
# From project root
mvn clean test
```

### Run Tests for Specific Module
```bash
# Core module only
mvn test -pl frolic-core

# Services module only
mvn test -pl frolic-services
```

### Run Specific Test Class
```bash
# Run a specific test class
mvn test -Dtest=ProbabilityCalculatorTest

# Run multiple test classes
mvn test -Dtest=ProbabilityCalculatorTest,IdGeneratorTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=ProbabilityCalculatorTest#testCalculateAllocation_ZeroBudget_ReturnsZero
```

### Run Tests with Coverage
```bash
mvn clean test jacoco:report

# View report at: target/site/jacoco/index.html
```

### Skip Tests During Build
```bash
mvn clean install -DskipTests
```

## Test Categories

### 1. Unit Tests
**Fast, isolated tests with mocked dependencies**

Examples:
- `IdGeneratorTest` - Pure utility function tests
- `ProbabilityCalculatorTest` - Algorithm verification
- `PlayIngestionServiceTest` - Service logic with mocks

Characteristics:
- Execution time: < 1 second per class
- No external dependencies
- Mockito for dependency mocking

### 2. Integration Tests
**Tests with real database/infrastructure**

Examples:
- `GameRepositoryIntegrationTest` - JPA repository tests
- `CampaignRepositoryIntegrationTest` - Database operations

Characteristics:
- Uses @DataJpaTest with in-memory H2
- Can use Testcontainers for real databases
- Execution time: 1-5 seconds per class

### 3. Component Tests
**Tests with partial Spring context**

Examples:
- `PlayControllerTest` - @WebMvcTest for controllers
- API layer testing with MockMvc

## Test Annotations Guide

### Unit Test Annotations
```java
@ExtendWith(MockitoExtension.class)  // Enable Mockito
class ServiceTest {
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private ServiceToTest service;
}
```

### Controller Test Annotations
```java
@WebMvcTest(PlayController.class)  // Only load controller layer
class PlayControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PlayIngestionService service;
}
```

### Repository Test Annotations
```java
@DataJpaTest  // Configure JPA test slice
@ActiveProfiles("test")
class RepositoryIntegrationTest {
    @Autowired
    private GameRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;
}
```

## Test Coverage Goals

### Current Coverage
| Module | Classes | Methods | Lines | Branches |
|--------|---------|---------|-------|----------|
| frolic-core | 80%+ | 75%+ | 80%+ | 70%+ |
| frolic-services | 80%+ | 75%+ | 80%+ | 70%+ |

### Key Areas Covered
✅ Probability calculation algorithm
✅ Concurrency control (idempotency & atomic operations)
✅ Cache operations (Redis stores)
✅ Kafka messaging
✅ Service business logic
✅ API controllers
✅ Exception handling
✅ Repository operations

## Writing New Tests

### Naming Conventions
```
Test Class: {ClassName}Test.java
Test Method: test{MethodName}_{Scenario}_{ExpectedResult}

Examples:
- testCalculateAllocation_ZeroBudget_ReturnsZero
- testSubmitPlay_InvalidUser_ThrowsException
- testGetCampaignById_NotFound_ThrowsException
```

### Test Structure (AAA Pattern)
```java
@Test
void testMethodName_Scenario_ExpectedResult() {
    // Arrange: Set up test data and mocks
    String playId = "play-123";
    when(repository.findById(playId)).thenReturn(Optional.empty());
    
    // Act: Execute the method under test
    assertThrows(ResourceNotFoundException.class, () -> 
        service.getPlayResult(playId)
    );
    
    // Assert: Verify the results
    verify(repository).findById(playId);
}
```

### Common Test Utilities
```java
// Create test entities
private GameEntity createGame(String name, GameStatus status) {
    GameEntity game = new GameEntity();
    game.setName(name);
    game.setStatus(status);
    // ... set other required fields
    return game;
}

// Verify mock interactions
verify(repository, times(1)).save(any(GameEntity.class));
verify(repository, never()).deleteById(anyString());
```

## Troubleshooting

### Tests Won't Compile
```bash
# Clean and rebuild
mvn clean install -DskipTests

# Update dependencies
mvn dependency:resolve
```

### Tests Fail Due to Missing Dependencies
```bash
# Ensure all dependencies are in pom.xml
mvn dependency:tree

# Check for version conflicts
mvn dependency:analyze
```

### Integration Tests Fail
```bash
# Ensure Docker is running (for Testcontainers)
docker ps

# Check if ports are available
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka
```

### Flaky Tests
- Check for hardcoded timestamps
- Verify thread synchronization
- Review random number generation
- Check for shared state between tests

## Continuous Integration

### GitHub Actions Example
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: mvn clean test
      - name: Generate coverage report
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## Best Practices

✅ **Test Independence** - Each test should run independently
✅ **Fast Execution** - Unit tests should be fast (< 1s per class)
✅ **Descriptive Names** - Test names should describe what they test
✅ **Single Assertion** - Focus on one logical assertion per test
✅ **Mock External Dependencies** - Don't hit real databases in unit tests
✅ **Clean Test Data** - Set up and tear down test data properly
✅ **Test Edge Cases** - Cover boundary conditions and error paths

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Testcontainers](https://www.testcontainers.org/)

## Support

For issues or questions about tests:
1. Check TEST_COVERAGE_SUMMARY.md for overview
2. Review existing test examples
3. Consult Spring Boot testing documentation
4. Check project-specific patterns in existing tests
