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
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.1.10/KmpBle.xcframework.zip",
            checksum: "982a3e3e893973bcb29480cc7f1df3c32999e6d64ea9f13eab72fafbb944c92a"
        ),
    ]
)
