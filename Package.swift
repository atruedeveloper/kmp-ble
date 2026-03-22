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
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.2.2/KmpBle.xcframework.zip",
            checksum: "844c3376fb03be60f5e21b50be36139ff9b4d88324d4e724f5ccc6c847677da1"
        ),
    ]
)
