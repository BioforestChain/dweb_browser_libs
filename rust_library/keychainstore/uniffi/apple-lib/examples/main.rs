use keychainstore_macos::*;

fn main() {
    let service = "qaq.com";
    save_item(service, "qaq", "hi~".as_bytes());
    save_item(service, &"qaq1212".repeat(100), "hixxxxxxxx333~".as_bytes());
    save_item(service, "qaq22", "hi333~".as_bytes());
    println!("数据写入成功");
    println!(
        "数据加载成功:{:?}",
        String::from_utf8(load_item(service, "qaq").unwrap_or_default()).unwrap()
    );
    for key in get_all_accounts(service) {
        println!("读取数据:{}", key);
        println!(
            "成功:{}",
            load_item(service, key.as_str())
                .map(|v| String::from_utf8(v).unwrap())
                .unwrap()
        );
        if delete_item(service, key.as_str()) {
            println!("删除成功:{}", key);
        } else {
            println!("删除失败:{}", key);
        }
    }
}

// use keychainstore_macos::{add, add_str};

// fn main() {
//     println!("okk {}", add(1, 2));
//     println!("okk str {}", add_str("Hello", "World"));
// }
