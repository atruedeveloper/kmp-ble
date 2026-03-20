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
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.1.8-alpha2/KmpBle.xcframework.zip",
            checksum: "d4406c2991b4ccb13f6389356f3578f9a4ceb5ef7e5d30a622f8c19f8e19e493"
        ),
    ]
)
