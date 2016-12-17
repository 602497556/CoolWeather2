package com.example.coolweather2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather2.db.City;
import com.example.coolweather2.db.County;
import com.example.coolweather2.db.Province;
import com.example.coolweather2.util.HttpUtil;
import com.example.coolweather2.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2016/12/14.
 */
public class ChooseAreaFragment extends Fragment {


    private TextView titleText; //显示地区

    private Button btnBack; //返回键

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();
    //存放查询得到的省级数据
    private List<Province> provinces = new ArrayList<>();
    //存放市级数据
    private List<City> cities = new ArrayList<>();
    //存放县级数据
    private List<County> counties = new ArrayList<>();

    private Province selectedProvince; //选中的省份

    private City selectedCity; //选中的市

    private ProgressDialog progressDialog;

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private int currentLevel;


    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        btnBack = (Button) view.findViewById(R.id.title_btn);
        listView = (ListView) view.findViewById(R.id.lv_area);
        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if( currentLevel == LEVEL_PROVINCE ){
                    selectedProvince = provinces.get(position);
                    queryCities();
                } else if( currentLevel == LEVEL_CITY ){
                    selectedCity = cities.get(position);
                    queryCounties();
                } else if( currentLevel == LEVEL_COUNTY ){
                    String weatherId = counties.get(position).getWeatherId();
                    // 判断碎片依附于哪个Activity，执行不同的逻辑
                    if( getActivity() instanceof MainActivity ){
                        Intent intent = new Intent(getContext(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if( getActivity() instanceof WeatherActivity ){
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeatherInfo(weatherId);
                    }
                }
            }
        });
        //设置返回箭头的点击事件
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( currentLevel == LEVEL_COUNTY ){
                    queryCities();
                } else if( currentLevel == LEVEL_CITY ){
                    queryProvinces();
                }
            }
        });
        //查询省份
        queryProvinces();
    }

    /**
     * 查询全国所有省的数据,优先从数据库查询，没有再通过网络请求
     */
    private void queryProvinces() {
        titleText.setText("中国");
        btnBack.setVisibility(View.GONE);
        provinces = DataSupport.findAll(Province.class);
        if( provinces.size() > 0 ){
            //清除已有的数据
            dataList.clear();
            for( Province p : provinces ){
                dataList.add(p.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     * 查询某省份下所有的市,优先从数据库查询，没有再通过网络请求
     */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        btnBack.setVisibility(View.VISIBLE);
        cities = DataSupport.where(" provinceId = ? ",
                String.valueOf(selectedProvince.getId()))
                .find(City.class);
        if( cities.size() > 0 ){
            dataList.clear();
            for( City city : cities ){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }
    }

    /**
     * 查询某市下所有的县,优先从数据库查询，没有再通过网络请求
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        btnBack.setVisibility(View.VISIBLE);
        counties = DataSupport.where("cityId = ?",
                String.valueOf(selectedCity.getId()))
                .find(County.class);
        if( counties.size() > 0 ){
            dataList.clear();
            for( County county : counties ){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode +
                    "/" + cityCode;
            queryFromServer(address,"county");
        }
    }



    /**
     * 通过网络请求查询省市县数据
     *
     * @param address 地址
     * @param type 类型
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest( address, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean result = false;
                String rsp = response.body().string();
                if( "province".equals(type) ){
                    result = Utility.handleProvinceResponse(rsp);
                } else if( "city".equals(type) ){
                    result = Utility.handleCityResponse(rsp,selectedProvince.getId());
                } else if( "county".equals(type) ){
                    result = Utility.handleCountyResponse(rsp,selectedCity.getId());
                }
                if( result ){
                    //注意这里需要操作UI，所有要运行在主线程
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if( "province".equals(type) ){
                                queryProvinces();
                            } else if( "city".equals(type) ){
                                queryCities();
                            } else if( "county".equals(type) ){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog(); //注意它需要运行在主线程
                        Toast.makeText(getContext(),"加载失败",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    private void closeProgressDialog() {
        if( progressDialog != null ){
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }


}
