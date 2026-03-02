# Structured Storage Organization

## Overview

The storage system now uses a hierarchical folder structure to organize workout and diet plans, separating main plans from detailed metadata for better organization and scalability.

## Folder Structure

### Workout Plans
```
workout/
├── {userId}/
│   └── weeklyplan/
│       └── {weekNumber}/
│           ├── plan.json          # Main workout plan
│           └── exercises/
│               ├── exercise_1.json
│               ├── exercise_2.json
│               └── exercise_N.json
```

### Diet Plans
```
diet/
├── {userId}/
│   └── weeklyplan/
│       └── {weekNumber}/
│           ├── plan.json          # Main diet plan
│           └── meals/
│               ├── day_1_Monday.json
│               ├── day_2_Tuesday.json
│               └── day_N_DayName.json
```

## Examples

### Example Workout Structure
```
workout/
├── user123/
│   └── weeklyplan/
│       ├── week8/
│       │   ├── plan.json
│       │   └── exercises/
│       │       ├── exercise_1.json    # Push-ups
│       │       ├── exercise_2.json    # Squats
│       │       └── exercise_3.json    # Plank
│       └── week9/
│           ├── plan.json
│           └── exercises/
│               ├── exercise_1.json
│               └── exercise_2.json
```

### Example Diet Structure
```
diet/
├── user123/
│   └── weeklyplan/
│       └── week8/
│           ├── plan.json
│           └── meals/
│               ├── day_1_Monday.json
│               ├── day_2_Tuesday.json
│               ├── day_3_Wednesday.json
│               ├── day_4_Thursday.json
│               ├── day_5_Friday.json
│               ├── day_6_Saturday.json
│               └── day_7_Sunday.json
```

## File Contents

### Main Plan Files (plan.json)

**Workout Plan:**
```json
{
  "title": "Personalized Workout Plan",
  "fitnessLevel": "BEGINNER",
  "frequency": 3,
  "duration": 45,
  "targetGoals": ["strength", "endurance"],
  "equipment": ["bodyweight"],
  "exercisesStoredSeparately": true,
  "exercisesLocation": "exercises/",
  "generatedAt": "2026-03-02T14:15:00"
}
```

**Diet Plan:**
```json
{
  "title": "Personalized Diet Plan",
  "dietType": "BALANCED",
  "dailyCalories": 2000,
  "mealsPerDay": 3,
  "restrictions": ["gluten-free"],
  "preferredCuisines": ["mediterranean"],
  "mealsStoredSeparately": true,
  "mealsLocation": "meals/",
  "generatedAt": "2026-03-02T14:15:00"
}
```

### Metadata Files

**Exercise Metadata (exercise_1.json):**
```json
{
  "name": "Push-ups",
  "sets": "3 sets of 8-12 reps",
  "type": "strength",
  "muscle": "chest",
  "equipment": "bodyweight",
  "difficulty": "beginner",
  "instructions": "Start in plank position..."
}
```

**Meal Metadata (day_1_Monday.json):**
```json
{
  "day": "Monday",
  "meals": [
    {
      "type": "Breakfast",
      "foods": ["Scrambled eggs", "Whole grain toast"],
      "estimatedCalories": 500,
      "prepTime": "10 minutes"
    },
    {
      "type": "Lunch",
      "foods": ["Grilled chicken", "Brown rice", "Steamed broccoli"],
      "estimatedCalories": 700,
      "prepTime": "25 minutes"
    },
    {
      "type": "Dinner",
      "foods": ["Baked salmon", "Sweet potato", "Asparagus"],
      "estimatedCalories": 800,
      "prepTime": "30 minutes"
    }
  ],
  "totalCalories": 2000,
  "macros": {
    "protein": "125g",
    "carbs": "225g",
    "fats": "67g"
  }
}
```

## Benefits of This Structure

### 1. **Scalability**
- Individual exercise/meal files allow for detailed metadata without bloating main plan
- Easy to add new metadata fields to specific exercises/meals
- Better performance when loading partial data

### 2. **Organization**
- Clear separation between plan overview and detailed components
- User-specific folders prevent data mixing
- Week-based organization enables plan versioning

### 3. **Flexibility**
- Can load full plan with all details or just plan summary
- Individual exercise/meal files can be updated independently
- Easy to implement features like exercise history or meal modifications

### 4. **Traceability**
- Week-based folders provide natural versioning
- Clear audit trail of plan generation over time
- Easy to compare plans across different weeks

## Storage Key Format

The system returns storage keys in the format:
- Workout: `workout/{userId}/weeklyplan/{weekNumber}`
- Diet: `diet/{userId}/weeklyplan/{weekNumber}`

These keys point to the week directory containing both the main plan and metadata folders.

## Week Number Generation

Currently uses a simple algorithm based on day of year:
```java
int dayOfYear = LocalDateTime.now().getDayOfYear();
String weekNumber = String.valueOf((dayOfYear / 7) + 1);
```

This can be enhanced to use:
- Actual ISO week numbers
- User-specific week cycles
- Custom week definitions based on plan start dates

## Implementation Details

### Storage Services
- **LocalFileStorageService**: Uses local file system with same structure
- **ObjectStorageService**: Uses S3/MinIO with same structure
- Both services implement identical folder hierarchy

### Plan Retrieval
1. Load main `plan.json` file
2. Check if metadata is stored separately
3. If yes, load all files from metadata folder
4. Combine into complete plan object
5. Remove internal metadata flags before returning

### Plan Storage
1. Extract exercises/meals from main plan
2. Store main plan (without details) as `plan.json`
3. Store each exercise/meal as separate file in metadata folder
4. Return week directory storage key