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
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.1.8-alpha1/KmpBle.xcframework.zip",
            checksum: "e55b2d6e5460ad9560089b34f9d7966a9cf1a022699a68cf9899b4ecdb81845a"
        ),
    ]
)
