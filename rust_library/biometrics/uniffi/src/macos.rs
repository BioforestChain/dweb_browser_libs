pub fn check_support_biometrics() -> i8 {
    biometrics_macos::check_support_biometrics(None)
}

pub fn biometrics_result_content(reason: &str) -> (bool, String) {
    biometrics_macos::biometrics_result_content(None, reason)
}
