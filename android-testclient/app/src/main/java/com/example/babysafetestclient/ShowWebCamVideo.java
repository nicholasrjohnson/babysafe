package com.example.babysafetestclient;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.babysafetestclient.databinding.ShowVideoBinding;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ShowWebCamVideo extends Fragment {
    MediaController controller;
    private final String server = "127.0.0.2";
    private final int port = 8000;
    private ShowVideoBinding binding;
    private Context context;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = ShowVideoBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getActivity().getApplicationContext();
        binding.buttonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(ShowWebCamVideo.this)
                        .navigate(R.id.action_Go_To_Connect);
            }
        });

        controller = new MediaController(context);

        controller.setMediaPlayer(binding.videoView);
        binding.videoView.setMediaController(controller);
        binding.videoView.requestFocus();

        Connection connection = new Connection(binding);
        
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class Connection implements Runnable {
        private ShowVideoBinding binding;

        public Connection(ShowVideoBinding _binding) {
            this.binding = _binding;
        }

        @Override
        public void run() {
            File bufferFile = null;
            boolean flag = false;
            Socket sock = null;
            DataInputStream inStream = null;
            DataOutputStream outputStream = null;
            BufferedOutputStream videoOutput = null;
            byte[] buffer = new byte[16384];
            int totalRead = 0;
            boolean started = false;

            try {
                bufferFile = File.createTempFile("buffvid", "mp4");
                bufferFile.deleteOnExit();
                videoOutput = new BufferedOutputStream(new FileOutputStream(bufferFile));

                sock = new Socket(server, port);
                flag = sock.isConnected();
                if (flag) {
                    inStream = new DataInputStream(sock.getInputStream());
                    outputStream = new DataOutputStream(sock.getOutputStream());
                }
            } catch (UnknownHostException e) {
                flag = false;
            } catch (IOException e) {
                flag = false;
            }

            while (flag == true) {
                try {

                    outputStream.writeUTF("babysafe");

                    int available = inStream.available();

                    if (available > 0) {
                        try {
                            videoOutput.write(buffer, 0, available);
                            videoOutput.flush();
                            totalRead += available;
                            if (totalRead > 12000 && !started) {
                                this.binding.videoView.setVideoPath(bufferFile.getAbsolutePath());
                                this.binding.videoView.start();
                                started = true;
                            }
                        } catch (IOException e) {
                        }
                    }

                    // send heartbeat

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {

                } catch (IOException e) {

                }
            }

            try {
                inStream.close();
                outputStream.close();
                videoOutput.close();
                sock.close();
            } catch (IOException e) {
            }
        }
    }
}