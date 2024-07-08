import XCTest

@testable import keychainstore_swift

final class keychainstore_swiftTests: XCTestCase {
  func testExample() throws {
    // XCTest Documentation
    // https://developer.apple.com/documentation/xctest

    // Defining Test Cases and Test Methods
    // https://developer.apple.com/documentation/xctest/defining_test_cases_and_test_methods

    let service = "XXX.myapp"
    // 存储数据
    let data = "Hello, Keychain!123".data(using: .utf8)!
    saveItem(service: service, account: "myAcco222unt", data: data)
    saveItem(service: service, account: "myAccount2", data: data)
    saveItem(service: service, account: "myAccount3", data: data)
    print("数据写入完成")
    print(getAllAccounts(service: service))
    print(hasItem(service: service, account: "myAccount"))
    print(hasItem(service: service, account: "myAccountxxxx"))

    for account in getAllAccounts(service: service) {
      // 读取数据
      if let loadedData = loadItem(service: service, account: account) {
        let loadedString = String(data: loadedData, encoding: .utf8)
        print("读取到的数据：\(loadedString ?? "")")
        // 删除数据
        if deleteItem(service: service, account: account) {
          print("数据删除成功: \(account)")
        } else {
          print("数据删除失败: \(account)")
        }
      } else {
        print("没有读取到的数据：\(account)")
      }
    }
  }
}
