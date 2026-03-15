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
            url: "https://github.com/gary-quinn/kmp-ble/releases/download/v0.1.0-alpha05/KmpBle.xcframework.zip",
            checksum: "0130a5a63b139340b1284419c35f5b6eb477e8317e782bc591cf3b76e53d4a51"
        ),
    ]
)
