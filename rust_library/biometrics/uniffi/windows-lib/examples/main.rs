use biometrics_win::{check_support_biometrics, biometrics_result_content};

fn main() {
    // let result = check_support_biometrics();

    let result = biometrics_result_content("test".to_string());
    println!("result: {}", result);
}