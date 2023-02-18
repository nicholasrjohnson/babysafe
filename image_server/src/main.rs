use warp::{Filter};
use std::collections::HashMap;
use parking_lot::RwLock;
use serde::{Serialize, Deserialize};
use base64;
use std::sync::Arc;
use std::str;
use opencv::{
    prelude::*,
    videoio,
    core::Vector,
    core::Scalar,
    core::in_range,
    core::bitwise_and,
};

type Payload = HashMap<String, String>;

#[derive(Debug, Serialize, Deserialize)]
struct Item {
    name: String,
    data: String,
}

#[derive(Clone)]
struct ToJson {
    message: Arc<RwLock<Payload>>
}

impl ToJson {
    fn new() -> Self {
        ToJson {
            message: Arc::new(RwLock::new(HashMap::new())),
        }
    }
}

#[tokio::main]
async fn main() {
    let  payload = ToJson::new();

    payload.message.write().insert("test".to_string(), "some_data".to_string());

    let payload_filter = warp::any().map(move || payload.clone());

    let get_image = warp::get()
        .and(warp::path("babysafe"))
        .and(warp::path("image"))
        .and(warp::path::end())
        .and(payload_filter.clone())
        .and_then(get_image);

    let routes = get_image;
    
    warp::serve(routes)
    .run(([10, 0, 0, 54], 8000))
    .await;
}

async fn get_image(
    payload: ToJson
    ) -> Result<impl warp::Reply, warp::Rejection> {
        let mut cam = videoio::VideoCapture::new(0, videoio::CAP_ANY).unwrap();
        let mut frame = Mat::default();
        let mut result = HashMap::new();
        let r = payload.message.read();

        for (key,value) in r.iter() {
            result.insert(key, value);
        }

        cam.read(&mut frame).unwrap();
        let image_string = "image".to_string();


        //let hsv_encoded_image = &mut Vector::<u8>::new(); 

        //cvt_color(&frame,hsv_encoded_image,opencv::imgproc::COLOR_BGR2HLS, 0).unwrap(); 

        
        let mask  = &mut Mat::default();

        let params = &mut Vector::<i32>::new();
        let encoded_image = &mut Vector::<u8>::new();

        let red_image = &mut Mat::default();

        let lower_bound = Scalar::from((0 as f64, 0 as f64, 100 as f64));
        let upper_bound = Scalar::from((100 as f64, 100 as f64, 255 as f64));

        in_range(&frame, &lower_bound, &upper_bound, mask).unwrap();

        bitwise_and(&frame, &frame, red_image, mask).unwrap();

        let flag = opencv::imgcodecs::imencode(".bmp", red_image , encoded_image, params).unwrap();
        let s = base64::encode(encoded_image);

        match flag {
            true => { result.insert(&image_string, &s); 
            },
            false => { println!("cannot be encoded");
            },
        };

        Ok(warp::reply::json(
                &result
        ))
}
