package com.example.coolweather2.db;

import org.litepal.crud.DataSupport;

/**
 * Created by Administrator on 2016/12/13.
 */
public class Province extends DataSupport {

    /**
     * 代表表中的主键，是每个实体类中都应该有的字段
     */
    private int id;

    private String provinceName; //省份名

    private int provinceCode; //省份代号

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}
