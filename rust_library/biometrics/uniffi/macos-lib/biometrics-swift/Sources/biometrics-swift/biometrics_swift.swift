// The Swift Programming Language
// https://docs.swift.org/swift-book

import LocalAuthentication
import SwiftRs

@_cdecl("lacontext_canEvaluatePolicy")
func canEvaluatePolicy(policy: LAPolicy) -> Bool {
  return LAContext().canEvaluatePolicy(policy, error: nil)
}

/// 目前不能直接返回对象，所以
@_cdecl("lacontext_evaluatePolicy")
func evaluatePolicy(policy: LAPolicy, reason: SRString) -> SRString {
  let reason = reason.toString()

  let semaphore = DispatchSemaphore(value: 0)
  var result = "error:unknown"

  LAContext().evaluatePolicy(policy, localizedReason: reason) { success, error in
    if success && error == nil {
      result = "success"
    } else {
      result = "error:\(error?.localizedDescription ?? "")"
    }
    semaphore.signal()
  }

  semaphore.wait()
  return SRString(result)
}
