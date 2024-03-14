use biometrics_macos::{check_support_biometrics, biometrics_result_content};

fn main() {
  let (success, _) = biometrics_result_content(None, "test".to_string());
  println!("result: {:?}", success);
}
