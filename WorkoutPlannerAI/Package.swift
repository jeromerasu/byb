// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "WorkoutPlannerAI",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "WorkoutPlannerAI",
            targets: ["WorkoutPlannerAI"]),
    ],
    dependencies: [
        .package(url: "https://github.com/MacPaw/OpenAI", from: "0.2.4")
    ],
    targets: [
        .target(
            name: "WorkoutPlannerAI",
            dependencies: ["OpenAI"]),
        .testTarget(
            name: "WorkoutPlannerAITests",
            dependencies: ["WorkoutPlannerAI"]),
    ]
)