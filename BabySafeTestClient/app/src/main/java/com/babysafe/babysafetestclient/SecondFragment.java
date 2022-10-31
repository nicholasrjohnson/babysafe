package com.babysafe.babysafetestclient;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.babysafe.babysafetestclient.databinding.FragmentSecondBinding;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private static HttpClient client;
    private final static String url = "http://10.0.0.56:8000/babysafe/image";

    private static class GetImage implements Callable<byte[]> {
        @Override
        public byte[] call() {
            byte[] imageBytes = new byte[0];
            try {
                HttpResponse response = client.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    InputStream stream = entity.getContent();
                    long contentLength = entity.getContentLength();
                    char[] jsonChars = new char[(int) contentLength];
                    InputStreamReader reader = new InputStreamReader(stream);
                    long hasRead = 0;
                    while (hasRead < contentLength)
                        hasRead += reader.read(jsonChars, (int) hasRead, (int) (contentLength - hasRead));
                    JSONObject obj = new JSONObject(new String(jsonChars));
                    String image = obj.getString("image");
                    imageBytes = Base64.getDecoder().decode(image);
                    response.getEntity().getContent().close();
                    reader.close();
                } else {
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (IOException e) {
                System.out.println("There has been an IO exception: " + e);
            } catch (JSONException e) {
                System.out.println("There has been a Json exception: " + e);
            }

            return imageBytes;
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        client = HttpClients.createDefault();
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

        binding.buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = new byte[0];
                GetImage func = new GetImage();
                try {
                    Future<byte[]> imageFuture = Executors.newCachedThreadPool().submit(func);
                    bytes = imageFuture.get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    e.printStackTrace();
                }
                binding.imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        client = null;
    }
}