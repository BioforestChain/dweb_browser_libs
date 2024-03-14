pub fn check_support_biometrics() -> i8 {
    biometrics_win::check_support_biometrics()
}

pub fn biometrics_result_content(reason: String) -> i8 {
    biometrics_win::biometrics_result_content(reason)
}
