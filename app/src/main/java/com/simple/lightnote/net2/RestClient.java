package com.simple.lightnote.net2;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author yangpeiyong
 */
public class RestClient {
    private static final String BASE_URL= "http://gank.avosapps.com/api/data/";
    private static final String BASE_URL_2= "http://gank.io/api/data/";

    private static RestClient instance = new RestClient();
    private  static ApiService api;

    private RestClient(){
        OkHttpClient okHttpClient = new OkHttpClient();
       // okHttpClient.networkInterceptors().add(new AddHeaderInterceptor());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_2)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ApiService.class);

    }

    public static ApiService api(){
        return api;
    }
}
