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
    },
    {
      "name": "Bodyweight Squats",
      "prescription": "3 sets of 10-15 reps",
      "muscle": "legs"
    }
  ],

  // Legacy payload for backward compatibility
  "plan": {
    "title": "Personalized Workout Plan",
    "fitnessLevel": "BEGINNER",
    "frequency": 3,
    "duration": 45,
    "workoutDays": [
      {
        "day": 1,
        "focus": "Upper Body",
        "exercises": [...],
        "duration": 45
      }
    ],
    "generatedAt": "2026-02-27T17:30:00"
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

## Migration Notes

- Frontend can immediately use the stable top-level fields (`title`, `phaseLabel`, etc.)
- Legacy `plan` field remains available during migration period
- All responses include metadata (`message`, `planTitle`, `storageKey`, `createdAt`)
- Fallback logic ensures responses are always well-formed even if AI generation fails