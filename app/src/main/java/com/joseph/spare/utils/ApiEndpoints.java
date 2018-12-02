package com.joseph.spare.utils;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;
import rx.Single;

public interface ApiEndpoints {
    @POST("login")
    @FormUrlEncoded
    @Headers(value = {"Accept:application/json"})
    Observable<ResponseBody> emailLogin(@Field("email") String email,
                                        @Field("password") String password
    );

    @POST("auth/signup")
    @Headers(value = {"Accept:application/json"})
    @FormUrlEncoded
    Observable<ResponseBody> register(@Field("name") String name, @Field("email") String email,
                                      @Field("password") String password);

    @POST("auth/password/reset")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> passwordResetRequest(@Query("email") String email);


    @POST("auth/password/update")
    @Headers(value = {"Accept:application/json"})
    @FormUrlEncoded
    Single<ResponseBody> changePassword(@Field("oldPassword") String currentPassword,
                                        @Field("newPassword") String newPassword,
                                        @Header("Authorization") String token);

    @GET("items")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findAll(@Header("Authorization") String token);

    @GET("items/u/{userId}")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findAllByUser(@Path("userId") String userId, @Header("Authorization") String token);

    @GET("items")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findAllByPage(@Query("page") long page, @Header("Authorization") String token);

    @GET("items/{itemId}")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findOne(@Path("itemId") String itemId, @Header("Authorization") String token);

    @GET("items/nearby")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findNearby(@Query("lat") double lat, @Query("lng") double lng, @Header("Authorization") String token);

    @GET("items/{itemId}/comments/{page}")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findCommentsPaged(@Path("itemId") String itemId, @Path("page") long page, @Header("Authorization") String token);

    @GET("items/{itemId}/comments")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> findComments(@Path("itemId") String itemId, @Header("Authorization") String token);


    @POST("items/{itemId}/comment")
    @Headers(value = {"Accept:application/json"})
    @FormUrlEncoded
    Single<ResponseBody> comment(@Path("itemId") String itemId, @Field("comment") String comment, @Header("Authorization") String token);

    @POST("items/{itemId}/like")
    @Headers(value = {"Accept:application/json"})
    Single<ResponseBody> like(@Path("itemId") String itemId, @Header("Authorization") String token);


    @Multipart
    @POST("auth/update/all")
    Single<ResponseBody> updateAccountAll(@Part("name") RequestBody name,
                                          @Part("email") RequestBody email,
                                          @Part MultipartBody.Part image,
                                          @Header("Authorization") String token);

    @POST("auth/update")
    @Multipart
    Single<ResponseBody> updateAccount(@Part("name") RequestBody name,
                                       @Part("email") RequestBody email,
                                       @Header("Authorization") String token);

    @Multipart
    @POST("items/post")
    Single<ResponseBody> postNew(@Part("name") RequestBody name, @Part("price") RequestBody price, @Part("duration") RequestBody duration,
                                 @Part("type") RequestBody type, @Part("category") RequestBody category, @Part("description") RequestBody description,
                                 @Part("city") RequestBody city, @Part("lat") RequestBody lat, @Part("lng") RequestBody lng, @Part("amenities") RequestBody amenities,
                                 @Part("email") RequestBody email, @Part("phone") RequestBody phone, @Part("website") RequestBody website, @Part("bathrooms") RequestBody bathrooms,
                                 @Part("bedrooms") RequestBody bedrooms, @Part("area") RequestBody area, @Part MultipartBody.Part image, @Header("Authorization") String token
    );

    @POST("service/fcm-token")
    @FormUrlEncoded
    Single<ResponseBody> fcmToken(@Field("fcmtoken") String fcmToken, @Header("Authorization") String token);

    @GET("items/search")
    Observable<ResponseBody> search(@Query("q") String query, @Header("Authorization") String token);

    @POST("items/{itemId}/view")
    Single<ResponseBody> view(@Path("itemId") String itemId,
                              @Header("Authorization") String token);
}
