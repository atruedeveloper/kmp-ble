// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KmpBle",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "KmpBle", targets: ["KmpBle"]),
    ],
    targets: [
        .binaryTarget(
            name: "KmpBle",
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.3.0/KmpBle.xcframework.zip",
            checksum: "aeb0f0c85aded7638d2ad50c717a6ac9cf6643438c5ee3d1aa38aed1373b7c28"
        ),
    ]
)
