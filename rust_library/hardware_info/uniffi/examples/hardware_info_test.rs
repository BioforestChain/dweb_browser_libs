use hardware_info::get_hardware_info;

fn main() {
  let result = get_hardware_info();
  
  println!("QAQ uuid={}", result.uuid);
}