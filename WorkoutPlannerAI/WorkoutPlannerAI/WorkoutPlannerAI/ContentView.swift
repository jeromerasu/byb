//
//  ContentView.swift
//  WorkoutPlannerAI
//
//  Created by Jerome Reyes on 11/6/25.
//

import SwiftUI
import Combine

struct ContentView: View {
    @EnvironmentObject var dataManager: DataManager
    @State private var showingQuestionnaire = false
    @State private var savedPlans: [WorkoutPlan] = []

    var body: some View {
        NavigationView {
            VStack {
                if savedPlans.isEmpty {
                    EmptyStateView {
                        showingQuestionnaire = true
                    }
                } else {
                    SavedPlansView(plans: savedPlans) { plan in
                        dataManager.deleteWorkoutPlan(id: plan.id)
                        loadSavedPlans()
                    }
                }
            }
            .navigationTitle("WorkoutPlannerAI")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    NavigationLink(destination: SettingsView()) {
                        Image(systemName: "gearshape")
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showingQuestionnaire = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingQuestionnaire) {
                QuestionnaireView()
            }
            .onAppear {
                loadSavedPlans()
            }
        }
    }

    private func loadSavedPlans() {
        savedPlans = dataManager.fetchWorkoutPlans()
    }
}

struct EmptyStateView: View {
    let onCreatePlan: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            VStack(spacing: 16) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)

                Text("Welcome to WorkoutPlannerAI")
                    .font(.title)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)

                Text("Create personalized workout plans using AI based on your age, available equipment, and schedule.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            Button("Create Your First Plan") {
                onCreatePlan()
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)

            Spacer()
        }
        .padding()
    }
}

struct SavedPlansView: View {
    let plans: [WorkoutPlan]
    let onDelete: (WorkoutPlan) -> Void
    @State private var selectedPlan: WorkoutPlan?
    @State private var showingPlanDetail = false

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(plans) { plan in
                    PlanCardView(plan: plan) {
                        selectedPlan = plan
                        showingPlanDetail = true
                    } onDelete: {
                        onDelete(plan)
                    }
                }
            }
            .padding()
        }
        .sheet(isPresented: $showingPlanDetail) {
            if let plan = selectedPlan {
                WorkoutPlanView(workoutPlan: plan)
            }
        }
    }
}

struct PlanCardView: View {
    let plan: WorkoutPlan
    let onTap: () -> Void
    let onDelete: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(plan.title)
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)

                    Spacer()

                    Menu {
                        Button("Delete", role: .destructive) {
                            onDelete()
                        }
                    } label: {
                        Image(systemName: "ellipsis")
                            .foregroundColor(.secondary)
                    }
                }

                HStack(spacing: 16) {
                    Label("Age \(plan.userProfile.age)", systemImage: "person")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Label("\(plan.weeklySchedule.count) days", systemImage: "calendar")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Spacer()
                }

                HStack {
                    Image(systemName: plan.userProfile.equipment.iconName)
                        .foregroundColor(.blue)
                    Text(plan.userProfile.equipment.displayName)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    ContentView()
        .environmentObject(DataManager.shared)
}
