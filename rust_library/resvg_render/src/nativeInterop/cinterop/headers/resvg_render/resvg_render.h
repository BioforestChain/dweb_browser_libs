#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

// The following structs are used to implement the lowest level
// of the FFI, and thus useful to multiple uniffied crates.
// We ensure they are declared exactly once, with a header guard, UNIFFI_SHARED_H.
#ifdef UNIFFI_SHARED_H
    // We also try to prevent mixing versions of shared uniffi header structs.
    // If you add anything to the #else block, you must increment the version suffix in UNIFFI_SHARED_HEADER_V4
    #ifndef UNIFFI_SHARED_HEADER_V4
        #error Combining helper code from multiple versions of uniffi is not supported
    #endif // ndef UNIFFI_SHARED_HEADER_V4
#else
#define UNIFFI_SHARED_H
#define UNIFFI_SHARED_HEADER_V4

// ⚠️ Attention: If you change this #else block (ending in `#endif // def UNIFFI_SHARED_H`) you *must* ⚠️
// ⚠️ increment the version suffix in all instances of UNIFFI_SHARED_HEADER_V4 in this file.           ⚠️

typedef struct RustBuffer
{
    int32_t capacity;
    int32_t len;
    uint8_t *_Nullable data;
} RustBuffer;

typedef struct ForeignBytes
{
    int32_t len;
    const uint8_t *_Nullable data;
} ForeignBytes;
typedef struct RustCallStatus {
    int8_t code;
    RustBuffer errorBuf;
} RustCallStatus;


typedef int32_t (*ForeignCallback)(uint64_t, int32_t, const uint8_t *_Nonnull, int32_t, RustBuffer *_Nonnull);

typedef void (*UniFfiRustFutureContinuation)(uint64_t, int16_t);

// ⚠️ Attention: If you change this #else block (ending in `#endif // def UNIFFI_SHARED_H`) you *must* ⚠️
// ⚠️ increment the version suffix in all instances of UNIFFI_SHARED_HEADER_V4 in this file.           ⚠️
#endif // def UNIFFI_SHARED_H

RustBuffer uniffi_resvg_render_fn_func_svg_to_png(RustBuffer svg_data_, RustBuffer box_size_, RustCallStatus *_Nonnull out_status);
RustBuffer ffi_resvg_render_rustbuffer_alloc(int32_t size_, RustCallStatus *_Nonnull out_status);
RustBuffer ffi_resvg_render_rustbuffer_from_bytes(ForeignBytes bytes_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rustbuffer_free(RustBuffer buf_, RustCallStatus *_Nonnull out_status);
RustBuffer ffi_resvg_render_rustbuffer_reserve(RustBuffer buf_, int32_t additional_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_continuation_callback_set(UniFfiRustFutureContinuation _Nonnull callback_);
void ffi_resvg_render_rust_future_poll_u8(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_u8(void* handle_);
void ffi_resvg_render_rust_future_free_u8(void* handle_);
uint8_t ffi_resvg_render_rust_future_complete_u8(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_i8(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_i8(void* handle_);
void ffi_resvg_render_rust_future_free_i8(void* handle_);
int8_t ffi_resvg_render_rust_future_complete_i8(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_u16(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_u16(void* handle_);
void ffi_resvg_render_rust_future_free_u16(void* handle_);
uint16_t ffi_resvg_render_rust_future_complete_u16(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_i16(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_i16(void* handle_);
void ffi_resvg_render_rust_future_free_i16(void* handle_);
int16_t ffi_resvg_render_rust_future_complete_i16(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_u32(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_u32(void* handle_);
void ffi_resvg_render_rust_future_free_u32(void* handle_);
uint32_t ffi_resvg_render_rust_future_complete_u32(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_i32(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_i32(void* handle_);
void ffi_resvg_render_rust_future_free_i32(void* handle_);
int32_t ffi_resvg_render_rust_future_complete_i32(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_u64(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_u64(void* handle_);
void ffi_resvg_render_rust_future_free_u64(void* handle_);
uint64_t ffi_resvg_render_rust_future_complete_u64(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_i64(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_i64(void* handle_);
void ffi_resvg_render_rust_future_free_i64(void* handle_);
int64_t ffi_resvg_render_rust_future_complete_i64(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_f32(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_f32(void* handle_);
void ffi_resvg_render_rust_future_free_f32(void* handle_);
float ffi_resvg_render_rust_future_complete_f32(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_f64(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_f64(void* handle_);
void ffi_resvg_render_rust_future_free_f64(void* handle_);
double ffi_resvg_render_rust_future_complete_f64(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_pointer(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_pointer(void* handle_);
void ffi_resvg_render_rust_future_free_pointer(void* handle_);
void*_Nonnull ffi_resvg_render_rust_future_complete_pointer(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_rust_buffer(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_rust_buffer(void* handle_);
void ffi_resvg_render_rust_future_free_rust_buffer(void* handle_);
RustBuffer ffi_resvg_render_rust_future_complete_rust_buffer(void* handle_, RustCallStatus *_Nonnull out_status);
void ffi_resvg_render_rust_future_poll_void(void* handle_, size_t uniffi_callback_);
void ffi_resvg_render_rust_future_cancel_void(void* handle_);
void ffi_resvg_render_rust_future_free_void(void* handle_);
void ffi_resvg_render_rust_future_complete_void(void* handle_, RustCallStatus *_Nonnull out_status);
uint16_t uniffi_resvg_render_checksum_func_svg_to_png(void);
uint32_t ffi_resvg_render_uniffi_contract_version(void);
