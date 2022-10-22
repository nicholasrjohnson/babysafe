use std::{
    thread,
    net::{TcpStream}
}'


use opencv::{
    prelude::*,
    videoio,
};

mod main;

use crate::main::Client;

pub async fn runVideo(client: Client, mut stream: TcpStream) -> thread {
    let builder = thread::Builder::new()
        .name("video".to_string());

    let handler = builder.spawn(|| {
        println!("sending video");
        let mut cam = videoio::VideoCapture::new(0, videoio::CAP_ANY)?;
        let _ = sender.send(Ok(Message::text("pong")));

        // send the video to the client
    }).unwrap();
    
   return handler; 
}

