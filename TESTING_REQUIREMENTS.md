# BACKEND MANDATORY TESTING REQUIREMENTS

No backend task is complete until it passes local testing verification.

## 1) Required local test profile
Create `src/main/resources/application-test.properties` with at minimum:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

spring.flyway.enabled=false
storage.use-local=true
storage.local.path=./test-storage

server.port=8083
beta.mode=true
```

## 2) Dependency requirement
Ensure H2 runtime dependency is present.

## 3) Mandatory execution steps
1. Application startup:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true
```
Must start cleanly with H2/local storage.

2. Endpoint accessibility:
```bash
curl -X POST http://localhost:8083/api/[your-endpoint] -H "Content-Type: application/json" -v
```
Must be reachable (no connection refused/404).

3. Auth setup when required:
- Register test user
- Use returned token for authenticated endpoint tests

4. Required seed/setup data:
- Create workout/diet profiles or other required entities before endpoint test

5. Response validation:
- Proper JSON response
- Expected schema
- No implementation 500s

## 4) Completion evidence required in every backend task
- Local testing setup created
- Startup/db init confirmation
- Endpoint accessibility confirmation
- Response validation confirmation
- Exact commands used
- Key note: tested locally without external API dependency

## Failure criteria (task fails)
- App won’t start locally
- Endpoint unreachable / connection refused
- 500s from missing dependencies/implementation
- Cannot create required test data
- Cannot verify endpoint request/response cycle

## Success criteria (task passes)
- App starts with test profile
- Implemented endpoints reachable
- Response schema correct
- Full request/response cycle verified locally
- No external API dependency for basic verification
