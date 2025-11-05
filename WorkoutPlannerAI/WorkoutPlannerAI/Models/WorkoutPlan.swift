import Foundation

struct WorkoutPlan: Codable, Identifiable {
    let id = UUID()
    let userProfile: UserProfile
    let generatedContent: String
    let createdAt: Date
    let dailyWorkouts: [DailyWorkout]

    init(userProfile: UserProfile, generatedContent: String) {
        self.userProfile = userProfile
        self.generatedContent = generatedContent
        self.createdAt = Date()
        self.dailyWorkouts = WorkoutPlan.parseDailyWorkouts(from: generatedContent)
    }

    private static func parseDailyWorkouts(from content: String) -> [DailyWorkout] {
        return []
    }
}

struct DailyWorkout: Codable, Identifiable {
    let id = UUID()
    let day: Int
    let title: String
    let exercises: [Exercise]
    let warmUp: String
    let coolDown: String
}

struct Exercise: Codable, Identifiable {
    let id = UUID()
    let name: String
    let sets: Int
    let reps: String
    let restPeriod: String
    let videoLink: String?
    let instructions: String
}