# Claude Code Configuration and Testing Guide

## 🏗️ Environment Configuration

### 3-Stage Environment Setup

The application uses three distinct environments with different storage strategies:

#### 1. Test Profile (Local Development)
- **Command**: `mvn spring-boot:run -Dspring-boot.run.profiles=test`
- **Storage**: Local file storage (`storage.use-local=true`)
- **Path**: `./test-storage`
- **Database**: H2 in-memory
- **Purpose**: Local development without external dependencies
- **Port**: 8083

#### 2. Beta Profile (Staging/Testing)
- **Profile**: `beta`
- **Storage**: MinIO object storage (`storage.use-local=false`)
- **Bucket**: `workout-plans-beta`
- **Purpose**: Testing with production-like MinIO setup
- **Environment**: Sandbox/Testing

#### 3. Production Profile
- **Profile**: `prod` (default on Render)
- **Storage**: MinIO object storage (`storage.use-local=false`)
- **Bucket**: `workout-plans`
- **Purpose**: Full production deployment
- **Environment**: Production

## 🔧 MinIO Environment Variables

For both beta and production deployments, configure these in Render:

```bash
MINIO_ENDPOINT=https://minio-server-c5z5.onrender.com
MINIO_ROOT_USER=your-access-key
MINIO_ROOT_PASSWORD=your-secret-key
```

Optional variables:
- `MINIO_BUCKET_NAME` (default: workout-plans or workout-plans-beta)
- `MINIO_REGION` (default: us-east-1)
- `MINIO_AUTO_CREATE_BUCKET` (default: true)

## 🧪 Testing Guidelines

### Local Testing
**IMPORTANT**: Always use the test profile for local testing to use local file storage:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true
```

This avoids MinIO connection issues during local development.

### API Testing Flow
1. User registration: `POST /api/v1/auth/register`
2. Workout profile: `POST /api/v1/workout/profile`
3. Diet profile: `POST /api/v1/diet/profile`
4. Plan generation: `POST /api/v1/plan/generate`
5. Current week: `GET /api/v1/plan/current-week`
6. Diet foods: `GET /api/v1/plan/diet-foods`

## 📂 Storage Structure

### Object Storage Path Structure
- Workout plans: `/{userId}/month-{n}/workout-plan.json`
- Diet plans: `/{userId}/month-{n}/diet-plan.json`

### Local Storage Structure (Test Environment)
- Base: `./test-storage/`
- Workout: `./test-storage/workout/{userId}/weeklyplan/week{n}/`
- Diet: `./test-storage/diet/{userId}/weeklyplan/week{n}/`

## 🔐 Authentication

### BETA Mode
- Enabled in test and beta profiles
- Allows testing without full authentication
- Falls back to hardcoded user IDs when needed

### JWT Configuration
- Secret: Configurable via `JWT_SECRET` environment variable
- Expiration: 24 hours (86400000 ms)
- Refresh: 7 days (604800000 ms)

## 🚀 Deployment Commands

### Local Development
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test -Dmaven.test.skip=true
```

### Testing with MinIO (if available locally)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=beta
```

### Build and Compile
```bash
mvn clean compile
mvn test-compile
```

## 🔍 Troubleshooting

### Common Issues
1. **Storage failures on Render**: Ensure MinIO environment variables are set
2. **403 Authentication errors**: Check BETA mode configuration and JWT tokens
3. **JSON parsing errors**: Verify OpenAI API key and response format
4. **Local testing failures**: Always use test profile for local development

### Debug Commands
```bash
# Check application health
curl -s https://byb-judc.onrender.com/actuator/health

# Test user registration
curl -X POST http://localhost:8083/api/v1/auth/register -H "Content-Type: application/json" -d '{
  "username":"testuser",
  "email":"test@test.com",
  "password":"password123",
  "firstName":"Test",
  "lastName":"User"
}'
```

## 📋 Important Notes

- **Never use local storage for deployed environments** - Only test profile should use local storage
- **MinIO is required for beta/prod** - Ensure proper credentials are configured
- **Use test profile for all local development** - Avoids external dependencies
- **Plan generation requires both workout and diet profiles** - Create both before generating plans