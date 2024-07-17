use std::borrow::Borrow;

use swift_rs::{swift, Bool, Int, SRObject, SRString};

/// 参考 https://github.com/caoimhebyrne/localauthentication-rs 项目

/// The set of available local authentication policies.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum LAPolicy {
    /// User authentication with biometry.
    DeviceOwnerAuthenticationWithBiometrics = 1,

    /// User authentication with Apple Watch.
    DeviceOwnerAuthenticationWithWatch = 3,

    /// User authentication with either biometry or Apple Watch.
    DeviceOwnerAuthenticationWithBiometricsOrWatch = 4,

    /// User authentication with biometry, Apple Watch, or the device passcode.
    DeviceOwnerAuthentication = 2,

    /// User authentication with wrist detection on watchOS.
    DeviceOwnerAuthenticationWithWristDetection = 5,
}

#[repr(C)]
pub(crate) struct LAContext {
    interactionNotAllowed: Bool,
}

#[repr(C)]
#[derive(Debug)]
pub(crate) struct BiometricsResult {
    success: bool,
    message: String,
}

/// Init LAContext
// swift!(pub(crate) fn lacontext_new() -> SRObject<LAContext>);

/// Assesses whether authentication can proceed for a given policy.
swift!(pub(crate) fn lacontext_canEvaluatePolicy(policy: Int) -> Bool);

/// Evaluates the specified policy.
swift!(pub(crate) fn lacontext_evaluatePolicy(policy: Int, reason: &SRString) -> SRString);

impl From<i8> for LAPolicy {
    fn from(value: i8) -> Self {
        match value {
            1 => Self::DeviceOwnerAuthenticationWithBiometrics,
            2 => Self::DeviceOwnerAuthentication,
            3 => Self::DeviceOwnerAuthenticationWithWatch,
            4 => Self::DeviceOwnerAuthenticationWithBiometricsOrWatch,
            _ => Self::DeviceOwnerAuthenticationWithWristDetection,
        }
    }
}

/// LAPolicy 转为 Int 类型
impl From<LAPolicy> for Int {
    fn from(value: LAPolicy) -> Self {
        match value {
            LAPolicy::DeviceOwnerAuthenticationWithBiometrics => 1,
            LAPolicy::DeviceOwnerAuthentication => 2,
            LAPolicy::DeviceOwnerAuthenticationWithWatch => 3,
            LAPolicy::DeviceOwnerAuthenticationWithBiometricsOrWatch => 4,
            LAPolicy::DeviceOwnerAuthenticationWithWristDetection => 5,
        }
    }
}

/// LAPolicy 转为 i8 类型
impl From<LAPolicy> for i8 {
    fn from(value: LAPolicy) -> Self {
        match value {
            LAPolicy::DeviceOwnerAuthenticationWithBiometrics => 1,
            LAPolicy::DeviceOwnerAuthentication => 2,
            LAPolicy::DeviceOwnerAuthenticationWithWatch => 3,
            LAPolicy::DeviceOwnerAuthenticationWithBiometricsOrWatch => 4,
            LAPolicy::DeviceOwnerAuthenticationWithWristDetection => 5,
        }
    }
}

pub fn check_support_biometrics(policy: Option<i8>) -> i8 {
    let la_policy = policy.unwrap_or(LAPolicy::DeviceOwnerAuthentication.into());

    if unsafe { lacontext_canEvaluatePolicy(la_policy.into()) } {
        0
    } else {
        -1
    }
}

pub fn biometrics_result_content(policy: Option<i8>, reason: String) -> (bool, String) {
    // let context = unsafe { lacontext_new() };
    let la_policy = policy.unwrap_or(LAPolicy::DeviceOwnerAuthentication.into());
    let localized_reason: SRString = reason.as_str().into();
    let result_rs = unsafe { lacontext_evaluatePolicy(la_policy.into(), &localized_reason) };
    let result = result_rs.as_str();
    if result == "success" {
        return (true, "".into());
    }
    let message = result.replace("error:", "");
    return (false, message);
}
