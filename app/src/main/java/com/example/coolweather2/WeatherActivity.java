package com.example.coolweather2;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather2.gson.Forecast;
import com.example.coolweather2.gson.Weather;
import com.example.coolweather2.util.HttpUtil;
import com.example.coolweather2.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public static final String TAG = "WeatherActivity";

    private TextView cityName; //地区名称

    private TextView updateTime; //更新时间

    private TextView degreeText; //当前气温

    private TextView weatherInfo; //天气情况

    private TextView dateText; //日期

    private TextView infoText; //日期对应的天气

    private TextView maxTemp; //最高气温

    private TextView minTemp; //最低气温

    private TextView aqiText; //AQI指数

    private TextView pmText; //PM2.5指数

    private TextView comfortText; //舒适度

    private TextView carWashText; //洗车指数

    private TextView sportText; //运动建议

    private LinearLayout forecastLayout;

    private ScrollView scrollView;

    private String mWeatherId; //天气代号

    private ImageView imageView; //背景图片

    public SwipeRefreshLayout swipeRefreshLayout;

    public DrawerLayout drawerLayout;

    private Button navButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 透明状态栏
        if(Build.VERSION.SDK_INT >= 21){
            View view = getWindow().getDecorView();
            view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        initViews();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String str = sp.getString("weather_response",null);
        if( str != null ){
            // 有缓存时直接显示缓存
            Weather weather = Utility.handleWeatherResponse(str);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            // 没有缓存就去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            // 请求数据是一个耗时操作，因此先把外层的ScrollView隐藏，
            // 以便达到较好的体验
            scrollView.setVisibility(View.INVISIBLE);
            requestWeatherInfo(mWeatherId);
        }
        // 加载背景图片
        String imgUrl = sp.getString("bing_img_url",null);
        if( imgUrl != null ){
            Glide.with(this).load(imgUrl).into(imageView);
        } else {
            loadBingPic();
        }
    }

    private void initViews() {
        cityName = (TextView) findViewById(R.id.title_city);
        updateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfo = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pmText = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        imageView = (ImageView) findViewById(R.id.background_image);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeatherInfo(mWeatherId);
            }
        });
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_btn);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * 请求天气信息
     */
    public void requestWeatherInfo(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid="
                + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String weatherStr = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(weatherStr);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if( weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this)
                                    .edit();
                            editor.putString("weather_response",weatherStr);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        // 每次获取天气信息时，重新加载背景图片
        loadBingPic();
    }

    /**
     * 显示天气信息
     *
     * @param weather 包含天气信息的实体类
     */
    private void showWeatherInfo(Weather weather) {
        String cityNameStr = weather.basic.cityName;
        String updateTimeStr = weather.basic.update.updateTime.split(" ")[1];
        String degreeTextStr = weather.now.temperature+"℃";
        cityName.setText(cityNameStr);
        updateTime.setText(updateTimeStr);
        degreeText.setText(degreeTextStr);
        weatherInfo.setText(weather.now.more.info);
        forecastLayout.removeAllViews(); //先移除所有View
        for(Forecast forecast : weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout, false);
            dateText = (TextView) view.findViewById(R.id.date_text);
            infoText = (TextView) view.findViewById(R.id.info_text);
            maxTemp = (TextView) view.findViewById(R.id.max_text);
            minTemp = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxTemp.setText(forecast.temperature.max+"℃");
            minTemp.setText(forecast.temperature.min+"℃");
            forecastLayout.addView(view);
        }
        if(weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pmText.setText(weather.aqi.city.pm25);
        }
        String comfortContent = "舒适度："+weather.suggestion.comfort.info;
        String carWashContent = "洗车指数："+weather.suggestion.carWash.info;
        String sportContent = "运动建议："+weather.suggestion.sport.info;
        comfortText.setText(comfortContent);
        carWashText.setText(carWashContent);
        sportText.setText(sportContent);
        scrollView.setVisibility(View.VISIBLE);
    }

    /**
     * 加载Bing图片作为背景图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String imgUrl = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this)
                        .edit();
                editor.putString("bing_img_url",imgUrl);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            Glide.with(WeatherActivity.this)
                                    .load(imgUrl).into(imageView);
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {

            }
        });
    }


}
