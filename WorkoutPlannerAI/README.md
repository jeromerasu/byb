# WorkoutPlanner AI

An iOS app that generates personalized workout plans using OpenAI's GPT-4.

## Features

- **Personalized Questionnaire**: Collects user preferences (age, gender, workout duration, available equipment)
- **AI-Powered Generation**: Uses OpenAI SDK to create custom 7-day workout plans
- **Video Integration**: Includes YouTube video links for exercise demonstrations
- **Clean UI**: Modern SwiftUI interface with smooth animations

## Setup

1. **Install Dependencies**:
   ```bash
   swift package resolve
   ```

2. **Add OpenAI API Key**:
   - Open `Info.plist`
   - Replace `YOUR_OPENAI_API_KEY_HERE` with your actual OpenAI API key

3. **Run the App**:
   - Open in Xcode
   - Build and run on simulator or device

## Project Structure

```
WorkoutPlannerAI/
├── Models/
│   ├── UserProfile.swift      # User preferences and prompt generation
│   └── WorkoutPlan.swift      # Workout plan data structures
├── Views/
│   ├── ContentView.swift      # Main app navigation
│   ├── OnboardingView.swift   # User questionnaire
│   ├── WorkoutGenerationView.swift  # AI generation loading
│   └── WorkoutPlanView.swift  # Display generated plan
├── ViewModels/
│   └── OnboardingViewModel.swift  # Onboarding logic
├── Services/
│   └── OpenAIService.swift    # OpenAI API integration
└── WorkoutPlannerAIApp.swift  # App entry point
```

## Usage Flow

1. **Welcome Screen**: Introduction to the app
2. **Questionnaire**: 4-step user preference collection
   - Age selection
   - Gender selection
   - Workout duration preference
   - Available equipment selection
3. **AI Generation**: Real-time workout plan creation
4. **Plan Display**: Interactive 7-day workout plan with video links

## Requirements

- iOS 15.0+
- Xcode 14.0+
- Swift 5.7+
- OpenAI API key

## API Cost Considerations

- Typical plan generation: ~1,500-3,000 tokens
- Cost per plan: $0.01-0.05
- Recommend implementing caching for similar user profiles

## Next Steps

- Add plan saving/history
- Implement progress tracking
- Add nutrition plan generation
- Include Apple HealthKit integration