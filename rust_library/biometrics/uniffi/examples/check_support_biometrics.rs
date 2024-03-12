use biometrics::check_support_biometrics;

fn main() {
  let result = check_support_biometrics();
  assert_eq!(0, result);
}
