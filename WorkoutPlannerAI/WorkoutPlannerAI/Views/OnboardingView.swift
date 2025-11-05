import SwiftUI

struct OnboardingView: View {
    @StateObject private var viewModel = OnboardingViewModel()
    @State private var currentStep = 0

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                ProgressView(value: Double(currentStep), total: 4)
                    .padding(.horizontal)

                Spacer()

                Group {
                    switch currentStep {
                    case 0:
                        AgeQuestionView(age: $viewModel.userProfile.age)
                    case 1:
                        GenderQuestionView(gender: $viewModel.userProfile.gender)
                    case 2:
                        WorkoutDurationView(duration: $viewModel.userProfile.workoutDuration)
                    case 3:
                        EquipmentSelectionView(equipment: $viewModel.userProfile.availableEquipment)
                    default:
                        EmptyView()
                    }
                }
                .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading)))

                Spacer()

                HStack {
                    if currentStep > 0 {
                        Button("Back") {
                            withAnimation {
                                currentStep -= 1
                            }
                        }
                        .buttonStyle(.bordered)
                    }

                    Spacer()

                    Button(currentStep == 3 ? "Generate Plan" : "Next") {
                        withAnimation {
                            if currentStep == 3 {
                                viewModel.generateWorkoutPlan()
                            } else {
                                currentStep += 1
                            }
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!viewModel.canProceed(from: currentStep))
                }
                .padding()
            }
            .navigationTitle("Workout Setup")
            .navigationBarTitleDisplayMode(.large)
        }
        .sheet(isPresented: $viewModel.isGenerating) {
            WorkoutGenerationView(viewModel: viewModel)
        }
    }
}

struct AgeQuestionView: View {
    @Binding var age: Int

    var body: some View {
        VStack(spacing: 20) {
            Text("How old are you?")
                .font(.title2)
                .fontWeight(.semibold)

            Picker("Age", selection: $age) {
                ForEach(13...80, id: \.self) { age in
                    Text("\(age) years old").tag(age)
                }
            }
            .pickerStyle(.wheel)
            .frame(height: 150)
        }
    }
}

struct GenderQuestionView: View {
    @Binding var gender: UserProfile.Gender

    var body: some View {
        VStack(spacing: 20) {
            Text("What's your gender?")
                .font(.title2)
                .fontWeight(.semibold)

            VStack(spacing: 15) {
                ForEach(UserProfile.Gender.allCases, id: \.self) { option in
                    Button(action: {
                        gender = option
                    }) {
                        HStack {
                            Text(option.rawValue)
                                .foregroundColor(gender == option ? .white : .primary)
                            Spacer()
                            if gender == option {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.white)
                            }
                        }
                        .padding()
                        .background(gender == option ? Color.blue : Color.gray.opacity(0.2))
                        .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

struct WorkoutDurationView: View {
    @Binding var duration: UserProfile.WorkoutDuration

    var body: some View {
        VStack(spacing: 20) {
            Text("How long can you typically workout?")
                .font(.title2)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)

            VStack(spacing: 15) {
                ForEach(UserProfile.WorkoutDuration.allCases, id: \.self) { option in
                    Button(action: {
                        duration = option
                    }) {
                        HStack {
                            Text(option.rawValue)
                                .foregroundColor(duration == option ? .white : .primary)
                            Spacer()
                            if duration == option {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.white)
                            }
                        }
                        .padding()
                        .background(duration == option ? Color.blue : Color.gray.opacity(0.2))
                        .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

struct EquipmentSelectionView: View {
    @Binding var equipment: [UserProfile.Equipment]

    var body: some View {
        VStack(spacing: 20) {
            Text("What equipment do you have access to?")
                .font(.title2)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)

            Text("Select all that apply")
                .font(.caption)
                .foregroundColor(.secondary)

            ScrollView {
                VStack(spacing: 15) {
                    ForEach(UserProfile.Equipment.allCases, id: \.self) { option in
                        Button(action: {
                            if equipment.contains(option) {
                                equipment.removeAll { $0 == option }
                            } else {
                                equipment.append(option)
                            }
                        }) {
                            HStack {
                                Text(option.rawValue)
                                    .foregroundColor(equipment.contains(option) ? .white : .primary)
                                Spacer()
                                if equipment.contains(option) {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.white)
                                }
                            }
                            .padding()
                            .background(equipment.contains(option) ? Color.blue : Color.gray.opacity(0.2))
                            .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}