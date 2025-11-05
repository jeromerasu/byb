import Foundation

struct UserProfile: Codable {
    let id = UUID()
    var age: Int
    var gender: Gender
    var workoutDuration: WorkoutDuration
    var availableEquipment: [Equipment]

    enum Gender: String, CaseIterable, Codable {
        case male = "Male"
        case female = "Female"
        case other = "Other"
    }

    enum WorkoutDuration: String, CaseIterable, Codable {
        case short = "15-30 minutes"
        case medium = "30-45 minutes"
        case long = "45-60 minutes"
        case extended = "60+ minutes"
    }

    enum Equipment: String, CaseIterable, Codable {
        case bodyweight = "Bodyweight only"
        case dumbbells = "Dumbbells"
        case barbell = "Barbell"
        case kettlebells = "Kettlebells"
        case resistanceBands = "Resistance bands"
        case pullupBar = "Pull-up bar"
        case gym = "Full gym access"
        case cardioMachine = "Cardio machines"
    }
}

extension UserProfile {
    func generatePrompt() -> String {
        let equipmentList = availableEquipment.map { $0.rawValue }.joined(separator: ", ")

        return """
        Create a detailed 7-day workout plan for:
        - Age: \(age) years old
        - Gender: \(gender.rawValue)
        - Workout duration: \(workoutDuration.rawValue) per session
        - Available equipment: \(equipmentList)

        Please provide:
        1. Daily workout routine with specific exercises
        2. Sets and reps for each exercise
        3. Rest periods between exercises
        4. Include YouTube video links for exercise demonstrations when possible
        5. Progressive difficulty throughout the week
        6. Include warm-up and cool-down routines

        Format the response as a structured weekly plan with clear day-by-day breakdown.
        """
    }
}