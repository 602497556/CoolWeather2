package com.example.coolweather2.util;

import android.text.TextUtils;

import com.example.coolweather2.db.City;
import com.example.coolweather2.db.County;
import com.example.coolweather2.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ZENGJIE on 2016/12/14.
 */
public class Utility {

    /**
     * 解析并处理返回的省级数据
     */
    public static boolean handleProvinceResponse(String response){
        // response为null和""时，TextUtils.isEmpty都返回true
        if( !TextUtils.isEmpty(response) ){
            try {
                JSONArray array = new JSONArray(response);
                for( int i=0; i<array.length(); i++){
                    Province province = new Province();
                    JSONObject obj = array.getJSONObject(i);
                    province.setProvinceCode(obj.getInt("id"));
                    province.setProvinceName(obj.getString("name"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    /**
     * 解析并处理返回的市级数据
     */
    public static boolean handleCityResponse(String response,int provinceId){
        if( !TextUtils.isEmpty(response) ){
            try {
                JSONArray allCities = new JSONArray(response);
                for( int i=0; i<allCities.length(); i++ ){
                    City city = new City();
                    JSONObject obj = allCities.getJSONObject(i);
                    city.setCityCode(obj.getInt("id"));
                    city.setCityName(obj.getString("name"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析并处理返回的县级数据
     */
    public static boolean handleCountyResponse(String response,int cityId){
        if( !TextUtils.isEmpty(response) ){
            try {
                JSONArray allCounties = new JSONArray(response);
                for( int i=0; i<allCounties.length(); i++ ){
                    County county = new County();
                    JSONObject obj = allCounties.getJSONObject(i);
                    county.setCountyName(obj.getString("name"));
                    county.setWeatherId(obj.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


}
