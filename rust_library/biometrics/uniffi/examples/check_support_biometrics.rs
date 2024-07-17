use biometrics::check_support_biometrics;

fn main() {
  let result = check_support_biometrics();
  println!("result: {:?}", result);
  assert_eq!(0, result);
}
