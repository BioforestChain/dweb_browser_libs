use std::io::Cursor;

pub fn zstd_decompress(data: Vec<u8>) -> Vec<u8> {
    return zstd::stream::decode_all(Cursor::new(data)).unwrap();
}
pub fn zstd_compress(data: Vec<u8>, level: i32) -> Vec<u8> {
    return zstd::stream::encode_all(Cursor::new(data), level).unwrap();
}

uniffi::include_scaffolding!("compression");
