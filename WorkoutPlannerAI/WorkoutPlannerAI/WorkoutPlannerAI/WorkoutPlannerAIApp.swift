//
//  WorkoutPlannerAIApp.swift
//  WorkoutPlannerAI
//
//  Created by Jerome Reyes on 11/6/25.
//

import SwiftUI

@main
struct WorkoutPlannerAIApp: App {
    let dataManager = DataManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, dataManager.context)
                .environmentObject(dataManager)
        }
    }
}
