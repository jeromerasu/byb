# Workout AI Service

A Spring Boot microservice for AI-powered workout and diet plan generation using OpenAI integration.

## 🎯 Overview

This service provides RESTful APIs for generating personalized workout and diet plans using artificial intelligence. Built with Spring Boot 3, it features JWT authentication, reactive programming, and comprehensive health monitoring.

## 🔧 Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Build Tool**: Maven 3.9+
- **Security**: Spring Security + JWT
- **Database**: H2 (development), JPA/Hibernate
- **Reactive**: Spring WebFlux
- **AI Integration**: OpenAI API
- **Monitoring**: Spring Actuator

## 📋 Prerequisites

- Java 17+
- Maven 3.9+
- OpenAI API Key

## 🚀 Quick Start

### 1. Environment Setup
```bash
export OPENAI_API_KEY=sk-your-openai-api-key-here
```

### 2. Build & Run
```bash
# Build the application
mvn clean package

# Run the application
java -jar target/workout-ai-service-*.jar
```

### 3. Verify Installation
```bash
# Check health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"},"ping":{"status":"UP"}}}
```

## 📡 API Endpoints

### Authentication
```bash
# Register new user
POST /api/v1/auth/register
Content-Type: application/json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "securepassword"
}

# Login user
POST /api/v1/auth/login
Content-Type: application/json
{
  "usernameOrEmail": "testuser",
  "password": "securepassword"
}

# Validate token
GET /api/v1/auth/validate
Authorization: Bearer <jwt-token>
```

### Workout Plans
```bash
# Generate workout plan
POST /api/v1/workout-plans/generate
Authorization: Bearer <jwt-token>
Content-Type: application/json
{
  "age": 25,
  "equipment": "basic",
  "weekly_frequency": 4
}

# Get saved workout plans
GET /api/v1/workout-plans/saved
Authorization: Bearer <jwt-token>
```

### Diet Plans
```bash
# Generate diet plan
POST /api/v1/diet-plans/generate
Authorization: Bearer <jwt-token>
Content-Type: application/json
{
  "preferred_proteins": ["chicken", "fish"],
  "preferred_carbs": ["rice", "quinoa"],
  "preferred_fats": ["olive oil", "avocado"],
  "diet_goals": "muscle gain",
  "meals_per_day": 3,
  "target_calories": 2500
}

# Get saved diet plans
GET /api/v1/diet-plans/saved
Authorization: Bearer <jwt-token>
```

### Structured plan response contract (v1)
`/api/v1/workout/plan/generate`, `/api/v1/workout/plan/current`, `/api/v1/diet/plan/generate`, `/api/v1/diet/plan/current` now return stable top-level fields for frontend parsing while keeping legacy `plan` object.

Workout response top-level keys:
- `message`, `planTitle`, `storageKey`, `createdAt`
- `title`, `phaseLabel`, `durationMin`, `calories`
- `exercises[]` with: `name`, `prescription`, `muscle`
- `plan` (legacy/full payload)

Diet response top-level keys:
- `message`, `planTitle`, `storageKey`, `createdAt`
- `title`, `phaseLabel`, `calories`, `mealsPerDay`, `dietType`
- `summary` (calories/meals/restrictions/cuisine/shoppingListCount)
- `plan` (legacy/full payload)

If stored or generated plan content is unstructured, backend applies fallback normalization so required keys are always present.

### Health Monitoring
```bash
# Main health check
GET /actuator/health

# Service-specific health
GET /api/v1/workout-plans/health
GET /api/v1/diet-plans/health
```

## 🏗 Project Structure

```
src/
├── main/
│   ├── java/com/workoutplanner/
│   │   ├── config/           # Configuration classes
│   │   │   ├── PasswordConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebClientConfig.java
│   │   ├── controller/       # REST controllers
│   │   │   ├── AuthController.java
│   │   │   ├── DietPlanController.java
│   │   │   ├── UserController.java
│   │   │   └── WorkoutPlanController.java
│   │   ├── dto/              # Data transfer objects
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── OpenAIRequest.java
│   │   │   ├── OpenAIResponse.java
│   │   │   └── RegisterRequest.java
│   │   ├── exception/        # Exception handling
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── model/            # Entity models
│   │   │   ├── DietPlan.java
│   │   │   ├── DietProfile.java
│   │   │   ├── Equipment.java
│   │   │   ├── User.java
│   │   │   ├── UserProfile.java
│   │   │   └── WorkoutPlan.java
│   │   ├── repository/       # Data repositories
│   │   │   ├── DietPlanRepository.java
│   │   │   ├── UserRepository.java
│   │   │   └── WorkoutPlanRepository.java
│   │   ├── security/         # Security components
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── service/          # Business logic
│   │   │   ├── DietAIService.java
│   │   │   ├── DietPlanService.java
│   │   │   ├── JwtService.java
│   │   │   ├── OpenAIService.java
│   │   │   ├── UserService.java
│   │   │   └── WorkoutPlanService.java
│   │   └── WorkoutAiServiceApplication.java
│   └── resources/
│       └── application.yml   # Configuration
└── test/                     # Unit tests
```

## ⚙️ Configuration

### Environment Variables
- `OPENAI_API_KEY` - OpenAI API key (required)
- `JWT_SECRET` - JWT signing secret (optional, auto-generated)
- `JWT_EXPIRATION` - Token expiration in ms (default: 24h)
- `SPRING_PROFILES_ACTIVE` - Active profile (dev/prod)
- `BETA_MODE` / `beta.mode` - Set to `true` to disable auth for local dev testing only

### Dev Mode (Frontend + Backend local testing)
For rapid mobile iteration, you can run backend in dev-auth-bypass mode:

```bash
export BETA_MODE=true
mvn spring-boot:run
```

⚠️ Never use this in production. Keep `BETA_MODE=false` (default) in deployed environments.

### Application Properties (application.yml)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:workoutdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

openai:
  api:
    key: ${OPENAI_API_KEY}
    url: https://api.openai.com/v1/chat/completions

jwt:
  expiration: 86400000  # 24 hours
```

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Manual API Testing
```bash
# Health check
curl http://localhost:8080/actuator/health

# Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123"}'

# Generate workout (replace with actual token)
curl -X POST http://localhost:8080/api/v1/workout-plans/generate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"age":25,"equipment":"basic","weekly_frequency":4}'
```

## 🐳 Docker

### Build Image
```bash
# From project root
docker build -t workout-ai-service:latest .

# From this directory
docker build -f ../Dockerfile -t workout-ai-service:latest ../
```

### Run Container
```bash
docker run -e OPENAI_API_KEY=sk-your-key -p 8080:8080 workout-ai-service:latest
```

## 🔧 Development

### IDE Setup
- **IntelliJ IDEA**: Project files included in `.idea/`
- **Java 17**: Required for compilation
- **Maven Integration**: For dependency management

### Common Commands
```bash
# Clean build
mvn clean compile

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package without tests
mvn clean package -DskipTests

# Run specific test
mvn test -Dtest=OpenAIServiceTest
```

## 🚨 Troubleshooting

### Common Issues

1. **OpenAI API Key Error**
   ```
   Error: OpenAI API key is not properly configured
   Solution: Set OPENAI_API_KEY environment variable
   ```

2. **Port Already in Use**
   ```
   Error: Port 8080 already in use
   Solution: Kill process or change port: --server.port=8081
   ```

3. **JWT Token Issues**
   ```
   Error: Invalid or expired token
   Solution: Re-authenticate or check token format
   ```

### Health Checks
```bash
# Database health
curl http://localhost:8080/actuator/health/db

# Application health
curl http://localhost:8080/actuator/health

# Custom service health
curl http://localhost:8080/api/v1/workout-plans/health
```

## 📊 Monitoring

### Available Endpoints
- `/actuator/health` - Overall health
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application info

### Logging
Configured for structured logging with different levels per environment.

## 🔒 Security Features

- **JWT Authentication**: Stateless token-based auth
- **Password Hashing**: BCrypt encryption
- **Input Validation**: Comprehensive request validation
- **CORS Configuration**: Cross-origin resource sharing
- **Security Headers**: XSS protection, content type options

## 🤝 Contributing

1. Follow Spring Boot conventions
2. Write unit tests for new features
3. Update documentation for API changes
4. Use proper error handling
5. Follow Java coding standards

---

**Part of WorkoutPlannerAI - Built with Spring Boot 3 and OpenAI**