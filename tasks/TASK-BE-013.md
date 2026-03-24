# TASK-BE-013: Test Environment Standardization and End-to-End Plan Generation Validation

## Summary
Standardize the test environment to use a single test user and validate complete end-to-end plan generation workflow with MinIO object storage functionality on the Render deployment.

## Priority
**High** - Critical for production readiness

## Status
**IN PROGRESS**

## Acceptance Criteria

### Test Environment Standardization
1. ✅ Single test user environment established
2. ✅ User authentication flow standardized for BETA mode testing
3. ✅ Database migration ensures consistent test user across deployments

### Logging Infrastructure
1. ✅ Replace all System.out.println with proper SLF4J logging
2. ✅ Implement centralized logging compatible with Render's infrastructure
3. ✅ Add proper log levels (debug, info, warn, error) for different operations
4. ✅ Parameterized logging messages for security and performance

### MinIO Object Storage Integration
1. ✅ MinIO connectivity established with proper credential handling
2. ✅ URL decoding for special characters in MinIO credentials
3. ✅ Bucket creation and management functionality working
4. ✅ Environment-specific bucket naming strategy (workoutbeta/dietbeta vs workout/diet)

### End-to-End Plan Generation Workflow
1. 🔄 **IN PROGRESS**: Complete workflow validation on Render deployment
   - User registration → profile creation → plan generation → storage verification
2. ⏳ Plan retrieval validation (current-week and diet-foods endpoints)
3. ⏳ MinIO object storage verification (plans visible in MinIO console)
4. ⏳ Performance validation (plan generation completion within reasonable time)

## Implementation Notes

### Technical Achievements
- **Logging**: Implemented comprehensive SLF4J logging in:
  - `ObjectStorageConfig.java`: MinIO S3Client configuration logging
  - `OpenAIService.java`: OpenAI response processing and JSON extraction logging
  - `ObjectStorageService.java`: Bucket operations and plan storage logging
  - `PlanController.java`: Authentication flow and request handling logging
  - `CombinedPlanService.java`: Plan generation coordination logging

- **MinIO Integration**: Fixed authentication issues by:
  - Implementing URL decoding for special characters in credentials
  - Using direct System.getenv() calls instead of Spring @Value annotations
  - Creating required buckets: workout, workoutbeta, diet, dietbeta

- **Authentication Flow**: Standardized BETA mode authentication with:
  - Consistent fallback user ID across all controllers
  - Database migration to ensure test user exists
  - JWT token extraction with graceful fallbacks

### Current Testing Focus
- Render deployment: https://byb-judc.onrender.com
- Environment variables: MINIO_ROOT_USER, MINIO_ROOT_PASSWORD, MINIO_ENDPOINT configured
- BETA mode enabled for testing without full authentication flow

## Dependencies
- Render deployment environment
- MinIO server: https://minio-server-c5z5.onrender.com
- OpenAI API integration for plan generation

## Related Tasks
- TASK-BE-009: Current Week API implementation
- TASK-BE-010: Diet Food Catalog implementation
- TASK-BE-006: API Contract Stabilization

## Testing Commands

```bash
# Register user
curl -X POST https://byb-judc.onrender.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","email":"test@test.com","password":"password123","firstName":"Test","lastName":"User"}'

# Create workout profile
curl -X POST https://byb-judc.onrender.com/api/v1/workout/profile \
  -H "Content-Type: application/json" \
  -d '{"fitness_level":"INTERMEDIATE","workout_frequency":4,"session_duration":60,"target_goals":["MUSCLE_GAIN"],"available_equipment":["DUMBBELLS","BARBELL"],"age":30,"gender":"MALE","weight_kg":80.0,"height_cm":185}'

# Create diet profile
curl -X POST https://byb-judc.onrender.com/api/v1/diet/profile \
  -H "Content-Type: application/json" \
  -d '{"diet_type":"MEDITERRANEAN","daily_calorie_goal":2600,"meals_per_day":4,"allergies":[],"dietary_restrictions":[],"preferred_cuisines":["MEDITERRANEAN"],"disliked_foods":[]}'

# Generate combined plan
curl -X POST https://byb-judc.onrender.com/api/v1/plan/generate \
  -H "Content-Type: application/json" --max-time 300

# Verify plan retrieval
curl -X GET https://byb-judc.onrender.com/api/v1/plan/current-week --max-time 30
curl -X GET https://byb-judc.onrender.com/api/v1/plan/diet-foods --max-time 30
```

## Notes
- BETA mode authentication allows testing without JWT tokens
- MinIO bucket creation is automatic when plans are generated
- All logging now uses SLF4J for proper centralized logging on Render
- Plan generation uses OpenAI API with 4-week structured format (28 days)