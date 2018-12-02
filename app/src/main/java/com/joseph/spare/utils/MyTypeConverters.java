package com.joseph.spare.utils;


import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.joseph.spare.domain.User;

public class MyTypeConverters {
    @TypeConverter
    public String arrayStringTypeConverter(String[] strings){
        return  new Gson().toJson(strings);
    }
    @TypeConverter
    public String[] arrayStringTypeConverter(String strings){
        return  new Gson().fromJson(strings,String[].class);
    }

//    @TypeConverter
//    public <T> String  listStringTypeConverter(List<T> list){
//        return new Gson().toJson(list);
//    }
//    @TypeConverter
//    public  ArrayList listStringTypeConverter(String string){
//        return new Gson().fromJson(string,ArrayList.class);
//    }

    @TypeConverter
    public User userConverter(String string){
        return new Gson().fromJson(string,User.class);
    }
    @TypeConverter
    public String userConverter(User user){
        return new Gson().toJson(user);
    }

}
