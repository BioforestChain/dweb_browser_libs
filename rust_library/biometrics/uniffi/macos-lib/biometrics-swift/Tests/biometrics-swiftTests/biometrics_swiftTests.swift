import SwiftRs
import XCTest

@testable import biometrics_swift

final class biometrics_swiftTests: XCTestCase {
  func testExample() throws {
    let result = evaluatePolicy(
      policy: .deviceOwnerAuthentication,
      reason: SRString("qaq\nqaq")
    )

    print(result.toString())
  }
}
