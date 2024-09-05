use std::vec;

use resvg::{
    render, tiny_skia,
    usvg::{self, Size},
};
pub struct RenderOptions {
    pub width: f32,
    pub height: f32,
    pub layer_limit_size: Option<f32>,
    pub fit_mode: FitMode,
}

pub enum FitMode {
    Fill,
    Contain,
}

// 不能返回 Option<Vec<u8>>，kotlin-uniffi 有 bug
pub fn svg_to_png(svg_data: Vec<u8>, options: Option<RenderOptions>) -> Vec<u8> {
    let mut output_width = 0;
    let mut output_height = 0;
    let mut fit_mode = FitMode::Contain;
    let mut layer_limit_size = 5242880.0; // 默认上限是5mb
    let tree = {
        let mut opt = usvg::Options::default();

        opt.fontdb_mut().load_system_fonts();

        match options {
            Some(render_options) => {
                fit_mode = render_options.fit_mode;
                match Size::from_wh(render_options.width, render_options.height) {
                    Some(new_size) => {
                        let pixmap_size = new_size.to_int_size();
                        output_width = pixmap_size.width();
                        output_height = pixmap_size.height();

                        opt.default_size = new_size;
                    }
                    None => {}
                }
                match render_options.layer_limit_size {
                    Some(limit_size) => {
                        layer_limit_size = limit_size;
                    }
                    None => {}
                }
            }
            None => {}
        }

        usvg::Tree::from_data(&svg_data, &opt).unwrap()
    };

    let tree_size = tree.size();
    let pixmap_size = tree_size.to_int_size();
    let pixmap_width = pixmap_size.width();
    let pixmap_height = pixmap_size.height();
    if output_width == 0 || output_height == 0 {
        output_width = pixmap_width;
        output_height = pixmap_height;
    }

    let mut transform = tiny_skia::Transform::default();
    if output_width != pixmap_width || output_height != pixmap_height {
        match fit_mode {
            FitMode::Fill => {
                let sx = output_width as f32 / pixmap_width as f32;
                let sy = output_height as f32 / pixmap_height as f32;
                transform = transform.post_scale(sx, sy);
            }
            FitMode::Contain => {
                let aspect_ratio = pixmap_width as f32 / pixmap_height as f32;
                let output_ratio = output_width as f32 / output_height as f32;
                let sx: f32;
                let sy: f32;
                if aspect_ratio > output_ratio {
                    sx = output_width as f32 / pixmap_width as f32;
                    sy = sx;
                    output_height = (output_width as f32 / aspect_ratio) as u32;
                } else {
                    sy = output_height as f32 / pixmap_height as f32;
                    sx = sy;
                    output_width = (output_height as f32 * aspect_ratio) as u32;
                }
                transform = transform.post_scale(sx, sy);
            }
        }
    }

    let mut pixmap = tiny_skia::Pixmap::new(output_width, output_height).unwrap();

    let layer_bounding_box = tree.root().abs_layer_bounding_box();
    let layer_bounding_width = layer_bounding_box.width();
    let layer_bounding_height = layer_bounding_box.height();
    // 判断一下是否需要过量的绘制成本，超过5mb的，就不画了
    if layer_bounding_width * layer_bounding_height > layer_limit_size {
        // 实际上要进行绘制的东西太多了，这里无法进行绘制，所以返回空
        return Vec::new();
    }

    render(&tree, transform, &mut pixmap.as_mut());
    return pixmap.encode_png().unwrap();
}

uniffi::include_scaffolding!("resvg_render");
