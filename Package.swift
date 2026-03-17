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
            url: "https://github.com/gary-quinn/kmp-ble/releases/download/v0.1.0/KmpBle.xcframework.zip",
            checksum: "b1eff477c95ecc4464f2e3418bcced80051be35baed29a441c9e3eb791e0911f"
        ),
    ]
)
