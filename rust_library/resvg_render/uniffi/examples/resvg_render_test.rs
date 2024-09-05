use std::env;

use resvg_render::{svg_to_png, FitMode, RenderOptions};

fn main() {
    let mut name = "test".to_string();
    let args: Vec<String> = env::args().collect();
    for arg in &args[1..] {
        println!("QAQ {arg}");
        name = arg.into()
    }
    let svg_data = std::fs::read(format!("./assets/{name}.svg")).unwrap();
    match std::fs::metadata("./output") {
        Ok(_) => {
            std::fs::remove_dir_all("./output").unwrap();
        }
        Err(_) => {}
    }
    std::fs::create_dir("./output").unwrap();
    {
        let png_data = svg_to_png(svg_data.clone(), None);
        std::fs::write(format!("./output/{name}.png"), png_data).unwrap();
    }
    {
        let png_data = svg_to_png(
            svg_data.clone(),
            Some(RenderOptions {
                width: 996.0,
                height: 500.0,
                fit_mode: FitMode::Contain,
            }),
        );
        std::fs::write(format!("./output/{name}.contain.png"), png_data).unwrap();
    }
    {
        let png_data = svg_to_png(
            svg_data.clone(),
            Some(RenderOptions {
                width: 996.0,
                height: 500.0,
                fit_mode: FitMode::Fill,
            }),
        );
        std::fs::write(format!("./output/{name}.fill.png"), png_data).unwrap();
    }
}
