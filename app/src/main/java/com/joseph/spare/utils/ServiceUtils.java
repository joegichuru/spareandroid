package com.joseph.spare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.joseph.spare.domain.Item;
import com.joseph.spare.domain.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database service and synchronisation
 */
public class ServiceUtils {
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static String uniqueId = null;

    public static synchronized String getUUID(Context context) {
        String str;
        synchronized (ServiceUtils.class) {
            if (uniqueId == null) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_UNIQUE_ID, 0);
                uniqueId = sharedPreferences.getString(PREF_UNIQUE_ID, null);
                if (uniqueId == null) {
                    uniqueId = UUID.randomUUID().toString();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PREF_UNIQUE_ID, uniqueId);
                    editor.apply();
                }
            }
            Log.i("UUID", uniqueId);
            str = uniqueId;
        }
        return str;
    }

    public static void saveLoggedUser(String userId, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userid", userId);
        editor.apply();
    }

    public static void saveToken(String token, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.apply();
    }

    public static String getLoggedUserId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("userid", null);
    }



    public static void clearLogin(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (sharedPreferences.contains("userid")) editor.remove("userid");

        editor.apply();
    }

    public static String getToken(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("token", null);
    }




    public static String formatAmount(long amount) {
        String formattedPrice = String.valueOf(amount);
        if (amount < 2147483647L) {
            formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(amount);
        }
        return formattedPrice;
    }

    public static String validateSafaricomPhoneNumbers(String inputPhoneNumber) {
        Pattern pattern = Pattern.compile("^(?:254|\\+254|0)?(7(?:(?:[12][0-6])|(?:0[0-8])|(?:9[0-8]))[0-9]{6})$");
        Matcher matcher = pattern.matcher(inputPhoneNumber);
        if (matcher.matches()) {
            return "254" + matcher.group(1);
        }
        return null;
    }

    public static String validateAirtelPhoneNumbers(String inputPhoneNumber) {
        Pattern pattern = Pattern.compile("^(?:254|\\+254|0)?(7(?:(?:[3][0-9])|(?:5[0-6])|(8[5-9]))[0-9]{6})$");
        Matcher matcher = pattern.matcher(inputPhoneNumber);
        if (matcher.matches()) {
            return "254" + matcher.group(1);
        }
        return null;
    }

    public static String validateEquitelNumber(String inputPhoneNumber) {
        Pattern pattern = Pattern.compile("^(?:254|\\+254|0)?(76[34][0-9]{6})$");
        Matcher matcher = pattern.matcher(inputPhoneNumber);
        if (matcher.matches()) {
            return "254" + matcher.group(1);
        }
        return null;
    }

    public static String validSupportedPhone(String inputPhone) {
        String match = validateSafaricomPhoneNumbers(inputPhone);
        if (match != null) {
            return match;
        }
        match = validateAirtelPhoneNumbers(inputPhone);
        if (match != null) {
            return match;
        }
        match = validateEquitelNumber(inputPhone);
        if (match != null) {
            return match;
        }
        return null;
    }

    public static void updateLocation(float lat, float lng, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("lat", lat);
        editor.putFloat("lng", lng);
        editor.apply();
    }

    public static Map<String,Float> findLocation(Context context){
        Map<String,Float> map=new HashMap<>();
        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(context);
        map.put("lat",sharedPreferences.getFloat("lat",23.0f));
        map.put("lng",sharedPreferences.getFloat("lng",23.0f));
        return map;
    }

    public static User updateUser(JSONObject user_data, User user) {
        return null;
    }

    public static User parseUser(JSONObject jsonObject,User user) throws JSONException {
        Gson gson = new Gson();
        User user1 = gson.fromJson(jsonObject.toString(), User.class);
        if(user!=null){
            user1.setAccessToken(user.getAccessToken());
        }

        return user1;
    }

    public static List<Item> parseItems(JSONArray jsonArray) throws JSONException {
        List<Item> items = new ArrayList<>();
        Gson gson = new Gson();
        for (int i = 0; i < jsonArray.length(); i++) {
            Item item = new Item();
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            item = gson.fromJson(jsonObject.toString(), Item.class);

            items.add(item);
        }
        Collections.reverse(items);
        return items ;
    }
    public static void saveFcmToken(String token,Context context){
        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("fcmtoken",token);
        editor.apply();
    }

    public static String getFcmToken(Context context){
        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("fcmtoken",null);
    }

    public static String tranform(long likes) {
        if(likes>=1000){
            return likes/1000+" k+";
        }else {
            return String.valueOf(likes);
        }
    }
}
