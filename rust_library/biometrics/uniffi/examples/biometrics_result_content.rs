use biometrics::biometrics_result_content;

fn main() {
  let result = biometrics_result_content("test".to_string());
  assert_eq!(true, result.success);
}