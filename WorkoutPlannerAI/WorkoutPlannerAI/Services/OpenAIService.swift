import Foundation
import OpenAI

class OpenAIService: ObservableObject {
    private let openAI: OpenAI

    init() {
        guard let apiKey = Bundle.main.object(forInfoDictionaryKey: "OPENAI_API_KEY") as? String else {
            fatalError("OpenAI API key not found in Info.plist")
        }
        self.openAI = OpenAI(apiToken: apiKey)
    }

    func generateWorkoutPlan(for userProfile: UserProfile) async throws -> String {
        let prompt = userProfile.generatePrompt()

        let query = ChatQuery(
            messages: [
                Chat(role: .system, content: """
                You are a certified personal trainer and fitness expert. Create detailed, safe, and effective workout plans.
                Always include proper form instructions and safety considerations.
                When suggesting exercises, include YouTube video links using this format: [Exercise Name](https://youtube.com/watch?v=VIDEO_ID) where you provide actual working YouTube links for exercise demonstrations.
                """),
                Chat(role: .user, content: prompt)
            ],
            model: .gpt4,
            maxTokens: 2000,
            temperature: 0.7
        )

        let result = try await openAI.chats(query: query)

        guard let content = result.choices.first?.message.content?.string else {
            throw OpenAIError.noResponse
        }

        return content
    }
}

enum OpenAIError: LocalizedError {
    case noResponse
    case apiKeyMissing

    var errorDescription: String? {
        switch self {
        case .noResponse:
            return "No response received from OpenAI"
        case .apiKeyMissing:
            return "OpenAI API key is missing"
        }
    }
}