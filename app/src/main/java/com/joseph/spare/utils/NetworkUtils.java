package com.joseph.spare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Single;

public class NetworkUtils {
    private static OkHttpClient httpClient = new OkHttpClient.Builder().
            connectTimeout(2, TimeUnit.MINUTES).retryOnConnectionFailure(true).build();


    private static NetworkUtils instance;
    private ApiEndpoints apiEndpointsService = new Retrofit.Builder().baseUrl(Constants.BASE_URL).addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create())).
                    client(httpClient).build().create(ApiEndpoints.class);

    private NetworkUtils() {
    }

    public static NetworkUtils getInstance() {
        if (instance == null) {
            instance = new NetworkUtils();
        }
        return instance;
    }

    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo networkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public Single<ResponseBody> passwordResetRequest(String email) {
        return apiEndpointsService.passwordResetRequest(email);
    }


    public Single<ResponseBody> changePassword(String currentPassword, String newPassword, String accessToken) {
        accessToken = "Bearer " + accessToken;
        return apiEndpointsService.changePassword(currentPassword, newPassword, accessToken);
    }

    public Single<ResponseBody> updateAccount(RequestBody name, RequestBody email, MultipartBody.Part image, String accessToken) {
        accessToken = "Bearer " + accessToken;
        return apiEndpointsService.updateAccountAll(name, email, image, accessToken);
    }

    public Single<ResponseBody> updateAccount(RequestBody name, RequestBody email, String accessToken) {
        accessToken = "Bearer " + accessToken;
        return apiEndpointsService.updateAccount(name, email, accessToken);
    }

    public Observable<ResponseBody> emailLogin(String userEmail, String userPassword) {
        return apiEndpointsService.emailLogin(userEmail, userPassword);
    }

    public Observable<ResponseBody> register(String name, String email, String password) {
        return apiEndpointsService.register(name, email, password);
    }

    public Single<ResponseBody> findItems(String token) {
        token = "Bearer " + token;
        return apiEndpointsService.findAll(token);
    }

    public Single<ResponseBody> findNearby(double lat, double lng, String token) {
        token = "Bearer " + token;
        return apiEndpointsService.findNearby(lat, lng, token);
    }

    public Single<ResponseBody> postNew(RequestBody name, RequestBody price, RequestBody duration,
                                        RequestBody type, RequestBody category, RequestBody description,
                                        RequestBody city, RequestBody lat, RequestBody lng, RequestBody amenities,
                                        RequestBody email, RequestBody phone, RequestBody website, RequestBody bathrooms,
                                        RequestBody bedrooms, RequestBody area, MultipartBody.Part image, String token
    ) {
        token = "Bearer " + token;
        return apiEndpointsService.postNew(name, price, duration, type, category, description, city, lat, lng, amenities, email, phone, website, bathrooms, bedrooms, area, image, token);
    }

    public Single<ResponseBody> findByUser(String userId, String token) {
        token = "Bearer " + token;
        return apiEndpointsService.findAllByUser(userId, token);
    }

    public Single<ResponseBody> like(String itemId, String token) {
        token = "Bearer " + token;
        return apiEndpointsService.like(itemId, token);
    }

    public Single<ResponseBody> sendFcmToken(String fcmToken,String token){
        token="Bearer "+token;
        return apiEndpointsService.fcmToken(fcmToken,token);
    }
    public Observable<ResponseBody> search(String query,String accessToken){
        accessToken="Bearer "+accessToken;
        return apiEndpointsService.search(query,accessToken);
    }

    public Single<ResponseBody> viewItem(String itemId,String accessToken){
        accessToken="Bearer "+accessToken;
        return apiEndpointsService.view(itemId,accessToken);
    }
}
