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
    core::in_range,
    core::bitwise_and;
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
    .run(([10, 0, 0, 56], 8000))
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

        let encoded_image = &mut Vector::<u8>::new();

        let flag = opencv::imgcodecs::imencode(".jpg", &frame, encoded_image, &Vector::<i32>::new()).unwrap();


        let mask  = &mut Vector::<u8>::new();

        let mut lower_bound = Vector::<i32>::new();
            
        let mut upper_bound = Vector::<i32>::new(); 

        lower_bound.push(20);
        lower_bound.push(20);
        lower_bound.push(200);

        upper_bound.push(0);
        upper_bound.push(0);
        upper_bound.push(255);

        in_range(encoded_image, &lower_bound, &upper_bound, mask).unwrap();

        let red_image = &mut Vector::<u8>::new();

        bitwise_and(&encoded_image, &encoded_image, red_image, mask);

        let image_vector = red_image.as_slice();

        let s = base64::encode(image_vector);

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
