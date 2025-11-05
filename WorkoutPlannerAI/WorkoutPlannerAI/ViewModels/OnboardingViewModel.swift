import Foundation
import SwiftUI

@MainActor
class OnboardingViewModel: ObservableObject {
    @Published var userProfile = UserProfile(
        age: 25,
        gender: .male,
        workoutDuration: .medium,
        availableEquipment: []
    )

    @Published var isGenerating = false
    @Published var generatedPlan: WorkoutPlan?
    @Published var errorMessage: String?

    private let openAIService = OpenAIService()

    func canProceed(from step: Int) -> Bool {
        switch step {
        case 0: return userProfile.age >= 13
        case 1: return true
        case 2: return true
        case 3: return !userProfile.availableEquipment.isEmpty
        default: return false
        }
    }

    func generateWorkoutPlan() {
        isGenerating = true
        errorMessage = nil

        Task {
            do {
                let content = try await openAIService.generateWorkoutPlan(for: userProfile)
                let plan = WorkoutPlan(userProfile: userProfile, generatedContent: content)

                await MainActor.run {
                    self.generatedPlan = plan
                    self.isGenerating = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isGenerating = false
                }
            }
        }
    }
}