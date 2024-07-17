use biometrics::biometrics_result_content;

fn main() {
  println!("biometrics_result_content start");
  let result = biometrics_result_content("test".to_string());
  // assert_eq!(true, result.success);
  println!("biometrics_result_content end");
  println!("result: {:?}", result);
}