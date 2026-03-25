# API Contract Documentation

## Unified Plan Response Contracts

This document defines the stable, typed response contracts for workout and diet plan endpoints.

## Workout Plan Endpoints

### POST /api/v1/workout/plan/generate

Generates a new workout plan for the authenticated user.

**Sample Response:**
```json
{
  "message": "Workout plan generated successfully",
  "planTitle": "Workout Plan - 2026-03-17",
  "storageKey": "workout-plan-abc123",
  "createdAt": "2026-03-17T17:30:00",

  // Stable frontend contract fields
  "title": "30-Day Personalized Workout Plan",
  "phaseLabel": "Base Phase",
  "durationMin": 45,
  "calories": 300,
  "exercises": [
    {
      "name": "Push-ups",
      "prescription": "3 sets of 10 reps",
      "muscle": "chest"
    },
    {
      "name": "Bodyweight Squats",
      "prescription": "3 sets of 12 reps",
      "muscle": "quadriceps"
    },
    {
      "name": "Plank",
      "prescription": "3 sets of 30 seconds",
      "muscle": "core"
    }
  ],

  // NEW: 30-day structured plan format
  "plan": {
    "title": "30-Day Personalized Workout Plan",
    "type": "30_DAY_STRUCTURED",
    "version": "2.0",
    "generatedAt": "2026-03-17T17:30:00",
    "totalWeeks": 4,
    "totalDays": 28,
    "fitnessLevel": "BEGINNER",
    "targetGoals": ["WEIGHT_LOSS"],
    "availableEquipment": ["BODYWEIGHT"],
    "sessionDuration": 45,

    "weeks": {
      "week_1": {
        "weekNumber": 1,
        "focus": "Foundation & Form",
        "intensity": "Low",
        "day_1": {
          "dayNumber": 1,
          "weekNumber": 1,
          "isWorkoutDay": true,
          "focus": "Upper Body",
          "exercises": [
            {
              "name": "Push-ups",
              "sets": 3,
              "reps": 10,
              "weight_lbs": 0,
              "weight_type": "bodyweight",
              "rest_seconds": 60,
              "instructions": "Start in plank position, lower chest to floor, push back up",
              "muscle_groups": ["chest", "shoulders", "triceps"]
            },
            {
              "name": "Wall Push-ups",
              "sets": 3,
              "reps": 11,
              "weight_lbs": 0,
              "weight_type": "bodyweight",
              "rest_seconds": 45,
              "instructions": "Perform with proper form",
              "muscle_groups": ["full_body"]
            }
          ],
          "estimatedDuration": 45,
          "estimatedCalories": 270
        },
        "day_2": {
          "dayNumber": 2,
          "weekNumber": 1,
          "isWorkoutDay": false,
          "exercises": [],
          "restDay": true,
          "recommendedActivity": "Light stretching or walking"
        },
        "day_3": {
          "dayNumber": 3,
          "weekNumber": 1,
          "isWorkoutDay": true,
          "focus": "Lower Body",
          "exercises": [...],
          "estimatedDuration": 45,
          "estimatedCalories": 270
        }
        // ... day_4 through day_7
      },
      "week_2": {
        "weekNumber": 2,
        "focus": "Building Strength",
        "intensity": "Moderate",
        // ... day_1 through day_7
      },
      "week_3": {
        "weekNumber": 3,
        "focus": "Increasing Intensity",
        "intensity": "High",
        // ... day_1 through day_7
      },
      "week_4": {
        "weekNumber": 4,
        "focus": "Peak Performance",
        "intensity": "Peak",
        // ... day_1 through day_7
      }
    },

    "summary": {
      "totalWorkoutDays": 12,
      "totalRestDays": 16,
      "avgDurationPerSession": 45,
      "estimatedTotalCalories": 3240,
      "primaryMuscleGroups": ["chest", "back", "legs", "shoulders", "core", "arms"],
      "recommendedEquipment": ["BODYWEIGHT"]
    }
  }
}
```

### GET /api/v1/workout/plan/current

Retrieves the user's current workout plan.

**Sample Response:**
```json
{
  "message": "Current workout plan retrieved",
  "planTitle": "Workout Plan - 2026-02-27",
  "storageKey": "workout-plan-abc123",
  "createdAt": "2026-02-27T17:30:00",

  // Stable frontend contract fields
  "title": "Personalized Workout Plan",
  "phaseLabel": "Base Phase",
  "durationMin": 45,
  "calories": 300,
  "exercises": [
    {
      "name": "Push-ups",
      "prescription": "3 sets of 8-12 reps",
      "muscle": "chest"
    }
  ],

  // Legacy payload for backward compatibility
  "plan": {
    // Full legacy structure preserved
  }
}
```

## Diet Plan Endpoints

### POST /api/v1/diet/plan/generate

Generates a new diet plan for the authenticated user.

**Sample Response:**
```json
{
  "message": "Diet plan generated successfully",
  "planTitle": "Diet Plan - 2026-02-27",
  "storageKey": "diet-plan-def456",
  "createdAt": "2026-02-27T17:30:00",

  // Stable frontend contract fields
  "title": "Personalized Diet Plan",
  "phaseLabel": "Nutrition Base",
  "calories": 2000,
  "proteinG": 125,
  "carbsG": 225,
  "fatsG": 67,
  "mealsPerDay": 3,
  "dietType": "BALANCED",
  "summary": {
    "calories": 2000,
    "mealsPerDay": 3,
    "dietType": "BALANCED",
    "restrictions": ["gluten-free"],
    "preferredCuisines": ["mediterranean"],
    "shoppingListCount": 15
  },

  // Legacy payload for backward compatibility
  "plan": {
    "title": "Personalized Diet Plan",
    "dietType": "BALANCED",
    "dailyCalories": 2000,
    "mealsPerDay": 3,
    "weeklyPlan": [
      {
        "day": "Monday",
        "meals": [
          {
            "type": "Breakfast",
            "foods": ["Scrambled eggs", "Whole grain toast"],
            "estimatedCalories": 500
          }
        ]
      }
    ],
    "generatedAt": "2026-02-27T17:30:00"
  }
}
```

### GET /api/v1/diet/plan/current

Retrieves the user's current diet plan.

**Sample Response:**
```json
{
  "message": "Current diet plan retrieved",
  "planTitle": "Diet Plan - 2026-02-27",
  "storageKey": "diet-plan-def456",
  "createdAt": "2026-02-27T17:30:00",

  // Stable frontend contract fields
  "title": "Personalized Diet Plan",
  "phaseLabel": "Nutrition Base",
  "calories": 2000,
  "proteinG": 125,
  "carbsG": 225,
  "fatsG": 67,
  "mealsPerDay": 3,
  "dietType": "BALANCED",
  "summary": {
    "calories": 2000,
    "mealsPerDay": 3,
    "dietType": "BALANCED",
    "restrictions": [],
    "preferredCuisines": [],
    "shoppingListCount": 0
  },

  // Legacy payload for backward compatibility
  "plan": {
    // Full legacy structure preserved
  }
}
```

## Contract Guarantees

### Workout Plan Response
- **title**: Always present, defaults to "Personalized Workout Plan"
- **phaseLabel**: Always present, defaults to "Base Phase"
- **durationMin**: Always present, positive integer, defaults to 45
- **calories**: Always present, positive integer, calculated from exercises
- **exercises[]**: Always present array, with fallback exercises if empty
- **plan**: Legacy payload always preserved for backward compatibility

### Diet Plan Response
- **title**: Always present, defaults to "Personalized Diet Plan"
- **phaseLabel**: Always present, defaults to "Nutrition Base"
- **calories**: Always present, positive integer, defaults to 2000
- **proteinG**: Always present, positive integer, calculated from calories (25%)
- **carbsG**: Always present, positive integer, calculated from calories (45%)
- **fatsG**: Always present, positive integer, calculated from calories (30%)
- **mealsPerDay**: Always present, positive integer, defaults to 3
- **dietType**: Always present, defaults to "BALANCED"
- **summary**: Always present object with nutrition overview
- **plan**: Legacy payload always preserved for backward compatibility

## Error Responses

All endpoints return structured error responses:

```json
{
  "timestamp": "2026-02-27T17:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Workout profile not found",
  "path": "/api/v1/workout/plan/generate"
}
```

## Schema Validation & AI Content Safety

### 30-Day Workout Plan Validation
All workout plans are validated against the 30-day schema structure:
- **Required**: `weeks.week_1` through `weeks.week_4`
- **Required**: Each week contains `day_1` through `day_7`
- **Required**: Each day has `exercises[]` array (can be empty for rest days)
- **Required**: Each exercise must include: `name`, `sets`, `reps`, `weight_type`
- **Optional**: `weight_lbs`, `rest_seconds`, `instructions`, `muscle_groups`

### Exercise Field Requirements
```json
{
  "name": "string (required) - Exercise name",
  "sets": "integer (required) - Number of sets",
  "reps": "integer (required) - Repetitions or seconds",
  "weight_type": "string (required) - bodyweight|time_seconds|weight",
  "weight_lbs": "number (optional) - Weight in pounds, 0 for bodyweight",
  "rest_seconds": "integer (optional) - Rest between sets",
  "instructions": "string (optional) - Exercise instructions",
  "muscle_groups": "array (optional) - Target muscle groups"
}
```

### AI Content Validation Process
1. **JSON Structure Check**: Ensures AI response is valid JSON object/array
2. **Schema Validation**: Validates 30-day workout plan structure
3. **Content Repair**: Automatically repairs malformed plans with safe fallback
4. **Fallback Generation**: Provides safe exercise templates if AI content invalid

### Validation Error Handling
```json
{
  "message": "Plan generated with fallback content due to validation errors",
  "planTitle": "Safe Workout Plan - 2026-03-17",
  "validationWarning": "AI content was invalid, using safe template",
  "plan": {
    "type": "30_DAY_STRUCTURED",
    "version": "2.0",
    "weeks": {
      // Safe fallback structure with basic exercises
    }
  }
}
```

## Migration Notes

- Frontend can immediately use the stable top-level fields (`title`, `phaseLabel`, etc.)
- New 30-day structured format available in `plan.weeks` object
- All AI-generated content is validated and repaired automatically
- Non-JSON or schema-invalid AI responses are replaced with safe templates
- Response envelopes are deterministic and frontend-safe
- All responses include metadata (`message`, `planTitle`, `storageKey`, `createdAt`)
- Fallback logic ensures responses are always well-formed even if AI generation fails