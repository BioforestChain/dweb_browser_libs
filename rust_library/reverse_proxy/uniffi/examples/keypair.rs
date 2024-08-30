use rcgen::{
    generate_simple_self_signed, Certificate, CertificateParams, KeyPair, PKCS_ECDSA_P256_SHA256,
};
fn main() {
    let subject_alt_names = vec!["localhost.dweb".to_string()];
    let mut params = CertificateParams::new(subject_alt_names);
    // {
    let key_pair = KeyPair::generate(params.alg).unwrap();
    let der = key_pair.serialized_der();
    println!("KEYPAIR DER: {:?}", der);
    let pem = key_pair.serialize_pem();
    println!("KEYPAIR PEM: {:?}", pem);
    // }
    // {
    //     let pem = "-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgecODpk61S6BZnYqq\nqiuntoAdX6aGxVtfJIugukIX3OKhRANCAASeBORnD2cXzDx/I3yKxnbHrlmUDQrE\nJGPKAUDquJl3axpUJS+hD8dm7Wyl4JpDZgCn0Fbq/DUFGFkHaEpNmSn8\n-----END PRIVATE KEY-----\n";
    let key_pair = KeyPair::from_pem(&pem).unwrap();
    let der = key_pair.serialized_der();
    println!("KEYPAIR DER: {:?}", der);
    let pem = key_pair.serialize_pem();
    println!("KEYPAIR PEM: {:?}", pem);
    // }
    params.key_pair = Some(key_pair);
}
