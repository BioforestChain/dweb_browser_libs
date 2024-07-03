import Foundation
import Security
import SwiftRs

@objc open class KeyChainGenericStore: NSObject {

  private let service: String
  private let queue: DispatchQueue

  // 初始化方法
  @objc public init(service: String) {
    self.service = service
    self.queue = DispatchQueue(label: "KeyChainGenericStore.queue/" + service)
  }

  /// 写入数据
  @objc public func saveItem(account: String, data: Data) {
    return queue.sync {
      // 1. 尝试更新数据
      let updateQuery: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
      ]
      let updateAttributes: [String: Any] = [
        kSecValueData as String: data
      ]
      let updateStatus = SecItemUpdate(
        updateQuery as CFDictionary, updateAttributes as CFDictionary)

      // 2. 如果更新失败，说明数据不存在，则添加数据
      if updateStatus == errSecItemNotFound {
        let addQuery: [String: Any] = [
          kSecClass as String: kSecClassGenericPassword,
          kSecAttrService as String: service,
          kSecAttrAccount as String: account,
          kSecValueData as String: data,
        ]
        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)

        if addStatus != errSecSuccess {
          // 处理添加数据错误
          print("添加数据失败！")
        }
      } else if updateStatus != errSecSuccess {
        // 处理更新数据错误
        print("更新数据失败！")
      }
    }
  }

  /// 判定指定建值是否存在
  @objc public func hasItem(account: String) -> Bool {
    return queue.sync {
      let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
        kSecReturnData as String: false,
        kSecReturnAttributes as String: false,
        kSecReturnRef as String: false,
        kSecMatchLimit as String: kSecMatchLimitOne,
      ]

      var result: AnyObject?
      let status = SecItemCopyMatching(query as CFDictionary, &result)

      return status == errSecSuccess
    }
  }

  /// 读取数据
  @objc public func loadItem(account: String) -> Data? {
    return queue.sync {
      let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
        kSecReturnData as String: true,
        kSecMatchLimit as String: kSecMatchLimitOne,
      ]

      var result: AnyObject?
      let status = SecItemCopyMatching(query as CFDictionary, &result)

      if status == errSecSuccess {
        return result as? Data
      } else {
        // 处理错误
        print("读取数据失败！")
        return nil
      }
    }
  }

  /// 删除数据
  @objc public func deleteItem(account: String) -> Bool {
    return queue.sync {
      let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecAttrAccount as String: account,
      ]

      let status = SecItemDelete(query as CFDictionary)

      if status != errSecSuccess {
        // 处理错误
        print("删除数据失败！")
      }
      return status == errSecSuccess
    }
  }

  /// 罗列所有的建值
  @objc public func getAllAccounts() -> [String] {
    return queue.sync {
      let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service,
        kSecMatchLimit as String: kSecMatchLimitAll,
        kSecReturnAttributes as String: true,
      ]

      var result: AnyObject?
      let status = SecItemCopyMatching(query as CFDictionary, &result)

      var accounts: [String] = []
      if status == errSecSuccess {
        if let items = result as? [[String: Any]] {
          for item in items {
            if let account = item[kSecAttrAccount as String] as? String {
              accounts.append(account)
            }
          }
        }
      } else {
        // 处理错误
        print("获取所有账户失败！")
      }

      return accounts
    }
  }

}

@_cdecl("create_key_chain_generic_store")
func createKeyChainGenericStore(service: SRString) -> KeyChainGenericStore {
 return KeyChainGenericStore(service: service.toString())
}
@_cdecl("store_save_item")
func storeSaveItem(store: KeyChainGenericStore, account: SRString, data: SRData) {
 return store.saveItem(account: account.toString(), data: Data(data.toArray()))
}
@_cdecl("store_has_item")
func storeHasItem(store: KeyChainGenericStore, account: SRString) -> Bool {
 return store.hasItem(account: account.toString())
}
@_cdecl("store_load_item")
func storeLoadItem(store: KeyChainGenericStore, account: SRString) -> SRData? {
 if let data = store.loadItem(account: account.toString()) {
   return SRData(data.map { $0 })
 }
 return nil
}
@_cdecl("store_delete_item")
func storeDeleteItem(store: KeyChainGenericStore, account: SRString) -> Bool {
 return store.deleteItem(account: account.toString())
}

class AccountArray: NSObject {
 var data: SRArray<SRString>

 init(_ data: [SRString]) {
   self.data = SRArray(data)
 }
}

@_cdecl("store_get_all_accounts")
func storeGetAllAccounts(store: KeyChainGenericStore) -> AccountArray {
 return AccountArray(store.getAllAccounts().map({ SRString($0) }))
}
