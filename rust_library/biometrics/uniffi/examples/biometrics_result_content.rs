use biometrics::biometrics_result_content;

fn main() {
  let result = biometrics_result_content();
  assert_eq!(0, result)
}