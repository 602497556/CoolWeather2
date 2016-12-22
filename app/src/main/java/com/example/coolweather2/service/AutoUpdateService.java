package com.example.coolweather2.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather2.gson.Weather;
import com.example.coolweather2.util.HttpUtil;
import com.example.coolweather2.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2016/12/17.
 */
public class AutoUpdateService extends Service {

    private static final String TAG = "AutoUpdateService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 我被执行了！！！");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: 我也执行了！！！");
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int time = 8 * 60 * 60 * 1000; // 8小时的毫秒数
        // SystemClock.elapsedRealtime为系统开机到现在所走的毫秒数
        // System.currentTimeMillis为1970/1/1零点到现在所走的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + time;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi); // 这句话有什么用？
        // 之前是用的AlarmManager.ELAPSED_REALTIME导致不能后台更新数据
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气信息
     */
    private void updateWeather() {
        Log.d( TAG,"AutoUpdateService中的updateWeather执行了！！！");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = sp.getString("weather_response",null);
        if(weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;

            String weatherUrl = "http://guolin.tech/api/weather?cityid="
                    + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if( weather != null && "ok".equals(weather.status) ){
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather_response",responseText);
                        editor.apply();
                    }
                }
                @Override
                public void onFailure(Call call, IOException e) {

                }
            });
        }
    }

    /**
     * 更新必应每日一图
     */
    private void updateBingPic() {
        Log.d( TAG,"AutoUpdateService中的updateBingPic执行了！！！");
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String imgUrl = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(AutoUpdateService.this)
                        .edit();
                editor.putString("bing_img_url",imgUrl);
                editor.apply();
            }
            @Override
            public void onFailure(Call call, IOException e) {

            }
        });
    }


}
