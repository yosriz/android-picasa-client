package com.pluscubed.picasaclient;


import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.pluscubed.picasaclient.signin.GoogleSignIn;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class PicasaClient2 {

    private static boolean DEBUG = BuildConfig.DEBUG || Log.isLoggable("pluscubed.picasaclient", Log.DEBUG);
    private static final String BASE_API_URL = "https://picasaweb.google.com/data/feed/api/user/";
    private static final int TIMEOUT_SEC = 30;

    private final AppCompatActivity activity;
    private final GoogleSignIn googleSignIn;

    public PicasaClient2(AppCompatActivity activity) {
        this.activity = activity;
        googleSignIn = new GoogleSignIn();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        googleSignIn.onActivityResult(requestCode, resultCode, data);
    }

    public Single<PicasaApiService> createPicasaApiService() {
        return googleSignIn.getToken(activity)
                .map(token -> new PicasaApiService(createRetrofit(token)));
    }

    private PicasaApi createRetrofit(String authToken) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    if (originalRequest.body() != null || originalRequest.header("Authorization") != null) {
                        return chain.proceed(originalRequest);
                    }

                    HttpUrl jsonUrl = originalRequest.url().newBuilder()
                            .addQueryParameter("alt", "json")
                            .build();

                    Request authorizedRequest = originalRequest.newBuilder()
                            .url(jsonUrl)
                            .header("Authorization", "Bearer " + authToken)
                            .header("Gdata-version", "2")
                            .build();
                    return chain.proceed(authorizedRequest);
                });
        if (DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            clientBuilder.addInterceptor(loggingInterceptor);
        }
        clientBuilder.connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(clientBuilder.build())
                .build();

        return retrofit.create(PicasaApi.class);
    }

}
