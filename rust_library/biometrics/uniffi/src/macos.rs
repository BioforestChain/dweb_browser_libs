pub fn check_support_biometrics(policy: Option<i8>) -> i8 {
    biometrics_swift::check_support_biometrics(policy)
}

pub fn biometrics_result_content(policy: Option<i8>, reason: String) -> i8 {
    biometrics_swift::biometrics_result_content(policy, reason)
}
