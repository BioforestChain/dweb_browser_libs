use hyper::body::Bytes;
use hyper::header::{HeaderName, HeaderValue, CONTENT_TYPE};
use hyper::HeaderMap;
use lazy_static::lazy_static;
use multer::{Constraints, Multipart};
use std::collections::HashMap;
use std::convert::Infallible;
use std::sync::atomic::{AtomicI32, Ordering};
use std::sync::Mutex;
use tokio::sync::mpsc::{unbounded_channel, UnboundedSender};
use tokio_stream::wrappers::UnboundedReceiverStream;

#[macro_use]
extern crate log;

#[cfg(target_os = "android")]
extern crate android_logger;
#[cfg(target_os = "android")]
use android_logger::{Config, FilterBuilder};
#[cfg(target_os = "android")]
use log::LevelFilter;

#[cfg(target_os = "android")]
fn init_log() {
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Trace) // limit log level
            .with_tag("mytag") // logs will show under mytag tag
            .with_filter(
                // configure messages for specific crate
                FilterBuilder::new()
                    .parse("debug,hello::crate=error")
                    .build(),
            ),
    );
}
#[cfg(not(target_os = "android"))]
fn init_log() {}

lazy_static! {
    static ref MULTIPART_HASHMAP: Mutex<HashMap<i32, UnboundedSender<Result<Bytes, Infallible>>>> =
        Mutex::new(HashMap::new());
    static ref ACC_ID: AtomicI32 = AtomicI32::new(0);
}

fn get_boundary(headers: HashMap<String, String>) -> Option<String> {
    let mut header_map = HeaderMap::new();
    for (key, value) in headers {
        header_map.append(
            HeaderName::from_bytes(key.as_bytes()).unwrap(),
            HeaderValue::from_str(value.as_str()).unwrap(),
        );
    }

    header_map
        .get(CONTENT_TYPE)
        .and_then(|ct| ct.to_str().ok())
        .and_then(|ct| multer::parse_boundary(ct).ok())
}

#[tokio::main]
async fn process_multipart_open(boundary: String, consumer: Box<dyn MultipartConsumer>) {
    init_log();
    let (tx, rx) = unbounded_channel::<Result<Bytes, Infallible>>();

    let id = ACC_ID.load(Ordering::Relaxed) + 1;
    ACC_ID.store(id, Ordering::Relaxed);

    let receiver_stream = Box::pin(UnboundedReceiverStream::new(rx));
    {
        let mut map = MULTIPART_HASHMAP.lock().unwrap();
        map.insert(id, tx);
    }
    consumer.on_open(id);

    let constraints = Constraints::new();

    let mut multipart = Multipart::with_constraints(receiver_stream, boundary, constraints);

    let mut field_index = 0;

    // Iterate over the fields, `next_field` method will return the next field if available.
    while let Some(mut field) = multipart.next_field().await.unwrap() {
        // Get the field name.
        let name = match field.name() {
            Some(name) => Some(name.to_string()),
            None => None,
        };

        // Get the field's filename if provided in "Content-Disposition" header.
        let file_name = match field.file_name() {
            Some(file_name) => Some(file_name.to_string()),
            None => None,
        };

        // Get the "Content-Type" header as `mime::Mime` type.
        let content_type = match field.content_type() {
            Some(mime) => Some(mime.essence_str().to_string()),
            None => None,
        };

        info!(
            "Name: {:?}, FileName: {:?}, Content-Type: {:?}",
            name, file_name, content_type
        );
        consumer.on_field_start(name, file_name, content_type, field_index);

        let mut field_bytes_len = 0;
        while let Some(field_chunk) = field.chunk().await.unwrap() {
            field_bytes_len += field_chunk.len();
            consumer.on_field_chunk(field_index, field_chunk.to_vec());
        }

        info!("Field Bytes Length: {:?}", field_bytes_len);
        consumer.on_field_end(field_index);

        field_index += 1;
    }

    consumer.on_close(id);

    process_multipart_close(id);
}

fn process_multipart_write(id: i32, chunk: Vec<u8>) {
    let map = MULTIPART_HASHMAP.lock().unwrap();
    let tx = map.get(&id).unwrap();

    tx.send(Ok(Bytes::copy_from_slice(&chunk))).unwrap();
}

fn process_multipart_close(id: i32) {
    MULTIPART_HASHMAP.lock().unwrap().remove(&id);
}

pub trait MultipartConsumer: Send + Sync + std::fmt::Debug {
    fn on_open(&self, id: i32);
    fn on_field_start(
        &self,
        name: Option<String>,
        file_name: Option<String>,
        content_type: Option<String>,
        field_index: i32,
    );
    fn on_field_chunk(&self, field_index: i32, chunk: Vec<u8>);
    fn on_field_end(&self, field_index: i32);
    fn on_close(&self, id: i32);
}

uniffi::include_scaffolding!("multipart");
