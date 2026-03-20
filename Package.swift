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
            url: "https://github.com/atruedeveloper/kmp-ble/releases/download/v0.1.8/KmpBle.xcframework.zip",
            checksum: "1da7c22cdf797d34e496aa89ad161d646abb82de20c007f955fbce7230c3193c"
        ),
    ]
)
