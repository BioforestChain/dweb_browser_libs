use biometrics_macos::check_support_biometrics;

fn main() {
  let result = check_support_biometrics(None);
  println!("result: {}", result);
}
