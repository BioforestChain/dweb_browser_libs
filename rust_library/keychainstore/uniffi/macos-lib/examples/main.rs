use keychainstore_macos::KeyChainGenericStore;

fn main() {
    let store = KeyChainGenericStore::new("qaq.com");
    store.save_item("qaq", "hi~".as_bytes());
    store.save_item(&"qaq1212".repeat(100), "hixxxxxxxx333~".as_bytes());
    store.save_item("qaq22", "hi333~".as_bytes());
    println!("数据写入成功");
    println!(
        "数据加载成功:{:?}",
        String::from_utf8(store.load_item("qaq").unwrap_or_default()).unwrap()
    );
    for key in store.get_all_accounts() {
        println!("读取数据:{}", key);
        println!(
            "成功:{}",
            store
                .load_item(key.as_str())
                .map(|v| String::from_utf8(v).unwrap())
                .unwrap()
        );
        if store.delete_item(key.as_str()) {
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
