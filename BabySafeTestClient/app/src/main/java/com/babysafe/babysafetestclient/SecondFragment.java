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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private static HttpClient client;
    private final static String url = "http://10.0.0.56:8000";

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
                try {
                    HttpResponse response = client.execute(new HttpGet(url));
                    StatusLine statusLine = response.getStatusLine();
                    int MAX_SIZE = 1024 * 1000 * 10;
                    byte[] jsonByteArray = new byte[MAX_SIZE];
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        int size = response.getEntity().getContent().read(jsonByteArray);
                        byte[] fullJsonByteArray = new byte[size];
                        for (int i = 0; i <fullJsonByteArray.length; i++) fullJsonByteArray[i] = jsonByteArray[i];
                        JSONObject obj = new JSONObject(fullJsonByteArray.toString());
                        byte[] image = (byte[]) obj.get("image");
                        binding.imageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
                    }
                    else
                    {
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                }
                catch (IOException e) {
                   System.out.println("There has been an IO exception: " + e);
                }
                catch (JSONException e)
                {
                   System.out.println("There has been a Json exception: " + e);
                }
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