use mix_compression::{zstd_compress, zstd_decompress};
use rand::Rng;

fn main() {
    let size: usize = 100 * 1024;
    let mut rng = rand::thread_rng();
    let source: Vec<u8> = (0..size).map(|_| rng.r#gen()).collect();
    let source = [source.clone(), source.clone(), source.clone()].concat();

    for level in 1..13 {
        let data = zstd_compress(source.clone(), level);
        println!("source len:{}", source.len());
        println!("compress len:{}", data.len());
        assert!(data.len() <= source.len());
        let result = zstd_decompress(data);
        assert_eq!(source, result);
    }
}
