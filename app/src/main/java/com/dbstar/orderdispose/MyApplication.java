package com.dbstar.orderdispose;

import android.app.Application;
import android.content.SharedPreferences;

import com.dbstar.crashcanary.CrashCanary;
import com.dbstar.orderdispose.constant.Constant;
import com.dbstar.orderdispose.constant.URL;
import com.dbstar.orderdispose.constant.URLFilm;
import com.dbstar.orderdispose.constant.URLShopping;

/**
 * Created by wh on 2017/1/5.
 */
public class MyApplication extends Application {
    private int ordersCode;
    private boolean isPrintAuto;
    private boolean isVoiceEnable;
    private int print_count;
    private String serviceIP;
    private SharedPreferences sp;
    private String ordersType;

    @Override
    public void onCreate() {
        super.onCreate();

        CrashCanary.install(this);

        //从sp获取数据
        sp = this.getSharedPreferences("config", MODE_PRIVATE);
        setIsPrintAuto(sp.getBoolean(Constant.AUTO_PRINT,false));
        setIsVoiceEnable(sp.getBoolean(Constant.VOICE_ENABLE, false));
        setPrint_count(sp.getInt(Constant.PRINT_COUNT,1));
        setServiceIP(sp.getString(Constant.SERVICE_IP,""));
        setOrdersType(sp.getString(Constant.ORDERS_TYPE,Constant.ORDER_TYPE_SHOPPING));
    }

    public void setOrdersType(String ordersType) {
        this.ordersType = ordersType;
        setUrl();
    }

    public String getOrdersType(){
        return ordersType;
    }

    public String getServiceIP() {
        return "http://"+serviceIP+":8080";
    }

    public void setServiceIP(String serviceIP) {
        this.serviceIP = serviceIP;
    }

    public boolean isPrintAuto() {
        return isPrintAuto;
    }

    public void setIsPrintAuto(boolean isPrintAuto) {
        this.isPrintAuto = isPrintAuto;
    }

    public boolean isVoiceEnable() {
        return isVoiceEnable;
    }

    public void setIsVoiceEnable(boolean isVoiceEnable) {
        this.isVoiceEnable = isVoiceEnable;
    }

    public int getPrint_count() {
        return print_count;
    }

    public void setPrint_count(int print_count) {
        this.print_count = print_count;
    }

    public int getOrdersCode() {
        return ordersCode;
    }

    public void setOrdersCode(int ordersCode) {
        this.ordersCode = ordersCode;
    }

    private void setUrl() {
        switch (getOrdersType()){
            case Constant.ORDER_TYPE_FILM:
                URL.IP = URLFilm.IP;
                URL.OldOrder = URLFilm.OldOrder;
                URL.NewOrder = URLFilm.NewOrder;
                URL.OrderItem = URLFilm.OrderItem;
                URL.NUMBER = URLFilm.NUMBER;
                URL.OrderMark = URLFilm.OrderMark;
                URL.OrderMarkID = URLFilm.OrderMarkID;
                URL.OrderMarkFLAG = URLFilm.OrderMarkFLAG;
                break;
            case Constant.ORDER_TYPE_SHOPPING:
            case Constant.ORDER_TYPE_SERVICE:
            case Constant.ORDER_TYPE_MEAL_SHOPING_SERVICE:
                URL.IP = URLShopping.IP;
                URL.OldOrder = URLShopping.OldOrder;
                URL.NewOrder = URLShopping.NewOrder;
                URL.OrderItem = URLShopping.OrderItem;
                URL.NUMBER = URLShopping.NUMBER;
                URL.OrderMark = URLShopping.OrderMark;
                URL.OrderMarkID = URLShopping.OrderMarkID;
                URL.OrderMarkFLAG = URLShopping.OrderMarkFLAG;
                break;
        }
    }
}
