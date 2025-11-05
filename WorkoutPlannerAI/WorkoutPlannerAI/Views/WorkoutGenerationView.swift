import SwiftUI

struct WorkoutGenerationView: View {
    @ObservedObject var viewModel: OnboardingViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            VStack(spacing: 30) {
                Spacer()

                VStack(spacing: 20) {
                    ProgressView()
                        .scaleEffect(1.5)

                    Text("Generating Your Workout Plan")
                        .font(.title2)
                        .fontWeight(.semibold)

                    Text("Our AI is creating a personalized 7-day workout plan based on your preferences...")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Spacer()

                if let errorMessage = viewModel.errorMessage {
                    VStack(spacing: 15) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.largeTitle)
                            .foregroundColor(.orange)

                        Text("Generation Failed")
                            .font(.headline)

                        Text(errorMessage)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)

                        Button("Try Again") {
                            viewModel.generateWorkoutPlan()
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                }
            }
            .navigationTitle("Creating Plan")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .onChange(of: viewModel.generatedPlan) { plan in
            if plan != nil {
                dismiss()
            }
        }
    }
}