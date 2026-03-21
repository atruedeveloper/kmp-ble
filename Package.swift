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
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.1.9/KmpBle.xcframework.zip",
            checksum: "2faa45dbd9727a6719bae37b4fee39ac08e724c609cff6eadb6669e444d760ba"
        ),
    ]
)
