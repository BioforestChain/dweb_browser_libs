pub(crate) mod consts;
use consts::BiometricsResult;

pub fn check_support_biometrics() -> i8 {
    biometrics_win::check_support_biometrics()
}

pub fn biometrics_result_content(reason: String) -> (bool, String) {
    biometrics_win::biometrics_result_content(reason)
}
