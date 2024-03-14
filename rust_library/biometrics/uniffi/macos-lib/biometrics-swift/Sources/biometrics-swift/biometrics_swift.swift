// The Swift Programming Language
// https://docs.swift.org/swift-book

import LocalAuthentication
import SwiftRs

class BiometricsResult: NSObject {
    var success: Bool
    var message: String

    init(_ success: Bool, _ message: String) {
        self.success = success
        self.message = message
    }
}

@_cdecl("lacontext_new")
func newLAContext() -> LAContext {
    return LAContext()
}

@_cdecl("lacontext_canEvaluatePolicy")
func canEvaluatePolicy(context: LAContext, policy: LAPolicy) -> Bool {
    return context.canEvaluatePolicy(policy, error: nil)
}

@_cdecl("lacontext_evaluatePolicy")
func evaluatePolicy(context: LAContext, policy: LAPolicy, reason: SRString) -> BiometricsResult {
    let reason = reason.toString()

    let semaphore = DispatchSemaphore(value: 0)
    var result = BiometricsResult(false, "")
    var didEvaluate = false

    context.evaluatePolicy(policy, localizedReason: reason) { success, error in
        result.success = success && error == nil
        result.message = error?.localizedDescription ?? ""
        semaphore.signal()
    }

    semaphore.wait()
    return result
}
