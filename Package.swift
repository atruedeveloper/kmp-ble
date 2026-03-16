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
            url: "https://github.com/gary-quinn/kmp-ble/releases/download/v0.1.0-alpha08/KmpBle.xcframework.zip",
            checksum: "09d3c0ccaac0d9950b95f2fb35fb7f77431cb1875ae854585a5dd575581a35db"
        ),
    ]
)
