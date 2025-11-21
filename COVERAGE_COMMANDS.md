# Code Coverage Commands - Quick Reference

## Generate Coverage Report

### Basic Command
```bash
mvn clean test jacoco:report
```

### View Reports
- **Frolic-Core**: `frolic-core/target/site/jacoco/index.html`
- **Frolic-Services**: `frolic-services/target/site/jacoco/index.html`

## What's Excluded from Coverage

✅ **Automatically Excluded:**
- Repository interfaces (`**/repository/jpa/**`)
- Entity classes (`**/repository/entity/**`)
- DTOs (`**/dto/**`)
- Enums (`**/enums/**`)
- Constants (`**/constant/**`)
- Configuration classes (`**/config/**`)
- Main application class

## Coverage Goals (After Exclusions)

- Line Coverage: **80%+**
- Branch Coverage: **75%+**
- Method Coverage: **85%+**

## Advanced Commands

### Run Tests Only
```bash
mvn test
```

### Generate Coverage + Check Thresholds
```bash
mvn clean test jacoco:check
```

### Aggregate Report (Both Modules)
```bash
mvn clean test
mvn jacoco:report-aggregate
```

## CI/CD Integration

### GitHub Actions
```yaml
- name: Run tests with coverage
  run: mvn clean test jacoco:report

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ./frolic-core/target/site/jacoco/jacoco.xml,./frolic-services/target/site/jacoco/jacoco.xml
```

## Troubleshooting

### Coverage Report Empty
- Ensure tests ran: `mvn clean test`
- Check JaCoCo executed: Look for `jacoco.exec` in `target/`

### Module Not Showing Coverage
- Verify tests exist in `src/test/java`
- Check plugin is in parent POM
- Ensure module builds successfully

## What IS Covered

Focus areas in coverage report:
- ✅ Service layer business logic
- ✅ Engine components (probability, concurrency)
- ✅ Cache stores (Redis operations)
- ✅ Utility classes
- ✅ Controllers
- ✅ Messaging producers/consumers
- ✅ Custom exception handlers
