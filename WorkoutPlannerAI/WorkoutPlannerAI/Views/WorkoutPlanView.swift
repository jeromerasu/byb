import SwiftUI

struct WorkoutPlanView: View {
    let workoutPlan: WorkoutPlan
    @State private var selectedDay = 0

    var body: some View {
        NavigationView {
            VStack {
                if workoutPlan.dailyWorkouts.isEmpty {
                    RawContentView(content: workoutPlan.generatedContent)
                } else {
                    StructuredPlanView(
                        dailyWorkouts: workoutPlan.dailyWorkouts,
                        selectedDay: $selectedDay
                    )
                }
            }
            .navigationTitle("Your Workout Plan")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Share") {
                    }
                }
            }
        }
    }
}

struct RawContentView: View {
    let content: String

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Your Personalized Workout Plan")
                    .font(.title2)
                    .fontWeight(.bold)
                    .padding(.horizontal)

                Text(content)
                    .font(.body)
                    .lineSpacing(4)
                    .padding(.horizontal)
                    .textSelection(.enabled)
            }
            .padding(.vertical)
        }
    }
}

struct StructuredPlanView: View {
    let dailyWorkouts: [DailyWorkout]
    @Binding var selectedDay: Int

    var body: some View {
        VStack {
            DaySelector(selectedDay: $selectedDay, totalDays: dailyWorkouts.count)

            ScrollView {
                if selectedDay < dailyWorkouts.count {
                    DailyWorkoutDetailView(workout: dailyWorkouts[selectedDay])
                }
            }
        }
    }
}

struct DaySelector: View {
    @Binding var selectedDay: Int
    let totalDays: Int

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(0..<totalDays, id: \.self) { day in
                    Button(action: {
                        selectedDay = day
                    }) {
                        VStack(spacing: 4) {
                            Text("Day")
                                .font(.caption2)
                                .foregroundColor(selectedDay == day ? .white : .secondary)
                            Text("\(day + 1)")
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(selectedDay == day ? .white : .primary)
                        }
                        .frame(width: 50, height: 60)
                        .background(selectedDay == day ? Color.blue : Color.gray.opacity(0.2))
                        .cornerRadius(12)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
    }
}

struct DailyWorkoutDetailView: View {
    let workout: DailyWorkout

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                Text(workout.title)
                    .font(.title2)
                    .fontWeight(.bold)

                if !workout.warmUp.isEmpty {
                    WarmUpCoolDownCard(title: "Warm-up", content: workout.warmUp)
                }
            }

            LazyVStack(spacing: 12) {
                ForEach(workout.exercises) { exercise in
                    ExerciseCard(exercise: exercise)
                }
            }

            if !workout.coolDown.isEmpty {
                WarmUpCoolDownCard(title: "Cool-down", content: workout.coolDown)
            }
        }
        .padding()
    }
}

struct ExerciseCard: View {
    let exercise: Exercise

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(exercise.name)
                        .font(.headline)
                        .fontWeight(.semibold)

                    HStack {
                        Label("\(exercise.sets) sets", systemImage: "repeat")
                        Spacer()
                        Label(exercise.reps, systemImage: "number")
                        Spacer()
                        Label(exercise.restPeriod, systemImage: "clock")
                    }
                    .font(.caption)
                    .foregroundColor(.secondary)
                }

                Spacer()

                if let videoLink = exercise.videoLink, !videoLink.isEmpty {
                    Button(action: {
                        if let url = URL(string: videoLink) {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        Image(systemName: "play.circle.fill")
                            .font(.title2)
                            .foregroundColor(.red)
                    }
                }
            }

            if !exercise.instructions.isEmpty {
                Text(exercise.instructions)
                    .font(.body)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(12)
    }
}

struct WarmUpCoolDownCard: View {
    let title: String
    let content: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .fontWeight(.semibold)

            Text(content)
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color.blue.opacity(0.1))
        .cornerRadius(12)
    }
}