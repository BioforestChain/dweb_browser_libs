import SwiftRs
import XCTest

@testable import smartscan_swift

final class smartscanTests: XCTestCase {
    func testText() {
        text()
    }

    func testExample() throws {
        fileChooser(type: "image")
    }
}
