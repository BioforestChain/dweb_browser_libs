#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>


typedef struct RustBuffer
{
    int64_t capacity;
    int64_t len;
    uint8_t *_Nullable data;
} RustBuffer;

typedef struct RustBufferByReference
{
    int64_t capacity;
    int64_t len;
    uint8_t *_Nullable data;
} RustBufferByReference;

typedef struct ForeignBytes
{
    int32_t len;
    const uint8_t *_Nullable data;
} ForeignBytes;

typedef struct UniffiRustCallStatus {
  int8_t code;
  RustBuffer errorBuf;
} UniffiRustCallStatus;

// Public interface members begin here.


// Contains loading, initialization code,
// and the FFI Function declarations.

typedef void (*UniffiRustFutureContinuationCallback)(int64_t, int8_t
    );

typedef void (*UniffiForeignFutureFree)(int64_t
    );

typedef void (*UniffiCallbackInterfaceFree)(int64_t
    );

typedef struct UniffiForeignFuture {
    int64_t handle;
    UniffiForeignFutureFree free;
} UniffiForeignFuture;

typedef struct UniffiForeignFutureStructU8 {
    int8_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructU8;

typedef void (*UniffiForeignFutureCompleteU8)(int64_t, UniffiForeignFutureStructU8
    );

typedef struct UniffiForeignFutureStructI8 {
    int8_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructI8;

typedef void (*UniffiForeignFutureCompleteI8)(int64_t, UniffiForeignFutureStructI8
    );

typedef struct UniffiForeignFutureStructU16 {
    int16_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructU16;

typedef void (*UniffiForeignFutureCompleteU16)(int64_t, UniffiForeignFutureStructU16
    );

typedef struct UniffiForeignFutureStructI16 {
    int16_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructI16;

typedef void (*UniffiForeignFutureCompleteI16)(int64_t, UniffiForeignFutureStructI16
    );

typedef struct UniffiForeignFutureStructU32 {
    int32_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructU32;

typedef void (*UniffiForeignFutureCompleteU32)(int64_t, UniffiForeignFutureStructU32
    );

typedef struct UniffiForeignFutureStructI32 {
    int32_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructI32;

typedef void (*UniffiForeignFutureCompleteI32)(int64_t, UniffiForeignFutureStructI32
    );

typedef struct UniffiForeignFutureStructU64 {
    int64_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructU64;

typedef void (*UniffiForeignFutureCompleteU64)(int64_t, UniffiForeignFutureStructU64
    );

typedef struct UniffiForeignFutureStructI64 {
    int64_t returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructI64;

typedef void (*UniffiForeignFutureCompleteI64)(int64_t, UniffiForeignFutureStructI64
    );

typedef struct UniffiForeignFutureStructF32 {
    float returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructF32;

typedef void (*UniffiForeignFutureCompleteF32)(int64_t, UniffiForeignFutureStructF32
    );

typedef struct UniffiForeignFutureStructF64 {
    double returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructF64;

typedef void (*UniffiForeignFutureCompleteF64)(int64_t, UniffiForeignFutureStructF64
    );

typedef struct UniffiForeignFutureStructPointer {
    void * returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructPointer;

typedef void (*UniffiForeignFutureCompletePointer)(int64_t, UniffiForeignFutureStructPointer
    );

typedef struct UniffiForeignFutureStructRustBuffer {
    RustBuffer returnValue;
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructRustBuffer;

typedef void (*UniffiForeignFutureCompleteRustBuffer)(int64_t, UniffiForeignFutureStructRustBuffer
    );

typedef struct UniffiForeignFutureStructVoid {
    UniffiRustCallStatus callStatus;
} UniffiForeignFutureStructVoid;

typedef void (*UniffiForeignFutureCompleteVoid)(int64_t, UniffiForeignFutureStructVoid
    );

int8_t uniffi_keychainstore_fn_func_keychain_delete_item(RustBuffer scope, RustBuffer key, UniffiRustCallStatus *_Nonnull out_status
);
RustBuffer uniffi_keychainstore_fn_func_keychain_get_item(RustBuffer scope, RustBuffer key, UniffiRustCallStatus *_Nonnull out_status
);
int8_t uniffi_keychainstore_fn_func_keychain_has_item(RustBuffer scope, RustBuffer key, UniffiRustCallStatus *_Nonnull out_status
);
RustBuffer uniffi_keychainstore_fn_func_keychain_item_keys(RustBuffer scope, UniffiRustCallStatus *_Nonnull out_status
);
int8_t uniffi_keychainstore_fn_func_keychain_set_item(RustBuffer scope, RustBuffer key, RustBuffer value, UniffiRustCallStatus *_Nonnull out_status
);
int8_t uniffi_keychainstore_fn_func_keychain_support_enum_keys(UniffiRustCallStatus *_Nonnull out_status
    
);
RustBuffer ffi_keychainstore_rustbuffer_alloc(int64_t size, UniffiRustCallStatus *_Nonnull out_status
);
RustBuffer ffi_keychainstore_rustbuffer_from_bytes(ForeignBytes bytes, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rustbuffer_free(RustBuffer buf, UniffiRustCallStatus *_Nonnull out_status
);
RustBuffer ffi_keychainstore_rustbuffer_reserve(RustBuffer buf, int64_t additional, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_u8(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_u8(int64_t handle
);
void ffi_keychainstore_rust_future_free_u8(int64_t handle
);
int8_t ffi_keychainstore_rust_future_complete_u8(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_i8(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_i8(int64_t handle
);
void ffi_keychainstore_rust_future_free_i8(int64_t handle
);
int8_t ffi_keychainstore_rust_future_complete_i8(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_u16(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_u16(int64_t handle
);
void ffi_keychainstore_rust_future_free_u16(int64_t handle
);
int16_t ffi_keychainstore_rust_future_complete_u16(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_i16(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_i16(int64_t handle
);
void ffi_keychainstore_rust_future_free_i16(int64_t handle
);
int16_t ffi_keychainstore_rust_future_complete_i16(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_u32(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_u32(int64_t handle
);
void ffi_keychainstore_rust_future_free_u32(int64_t handle
);
int32_t ffi_keychainstore_rust_future_complete_u32(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_i32(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_i32(int64_t handle
);
void ffi_keychainstore_rust_future_free_i32(int64_t handle
);
int32_t ffi_keychainstore_rust_future_complete_i32(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_u64(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_u64(int64_t handle
);
void ffi_keychainstore_rust_future_free_u64(int64_t handle
);
int64_t ffi_keychainstore_rust_future_complete_u64(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_i64(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_i64(int64_t handle
);
void ffi_keychainstore_rust_future_free_i64(int64_t handle
);
int64_t ffi_keychainstore_rust_future_complete_i64(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_f32(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_f32(int64_t handle
);
void ffi_keychainstore_rust_future_free_f32(int64_t handle
);
float ffi_keychainstore_rust_future_complete_f32(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_f64(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_f64(int64_t handle
);
void ffi_keychainstore_rust_future_free_f64(int64_t handle
);
double ffi_keychainstore_rust_future_complete_f64(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_pointer(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_pointer(int64_t handle
);
void ffi_keychainstore_rust_future_free_pointer(int64_t handle
);
void * ffi_keychainstore_rust_future_complete_pointer(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_rust_buffer(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_rust_buffer(int64_t handle
);
void ffi_keychainstore_rust_future_free_rust_buffer(int64_t handle
);
RustBuffer ffi_keychainstore_rust_future_complete_rust_buffer(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
void ffi_keychainstore_rust_future_poll_void(int64_t handle, UniffiRustFutureContinuationCallback callback, int64_t callbackData
);
void ffi_keychainstore_rust_future_cancel_void(int64_t handle
);
void ffi_keychainstore_rust_future_free_void(int64_t handle
);
void ffi_keychainstore_rust_future_complete_void(int64_t handle, UniffiRustCallStatus *_Nonnull out_status
);
int16_t uniffi_keychainstore_checksum_func_keychain_delete_item(void
    
);
int16_t uniffi_keychainstore_checksum_func_keychain_get_item(void
    
);
int16_t uniffi_keychainstore_checksum_func_keychain_has_item(void
    
);
int16_t uniffi_keychainstore_checksum_func_keychain_item_keys(void
    
);
int16_t uniffi_keychainstore_checksum_func_keychain_set_item(void
    
);
int16_t uniffi_keychainstore_checksum_func_keychain_support_enum_keys(void
    
);
int32_t ffi_keychainstore_uniffi_contract_version(void
    
);