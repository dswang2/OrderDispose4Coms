package com.dbstar.orderdispose;

import android.app.Application;
import android.content.SharedPreferences;

import com.dbstar.crashcanary.CrashCanary;
import com.dbstar.orderdispose.constant.Constant;

/**
 * Created by wh on 2017/1/5.
 */
public class MyApplication extends Application {
    private int orderListSize;
    private boolean isPrintAuto;
    private boolean isVoiceEnable;
    private int print_count;
    private String serviceIP;
    private SharedPreferences sp;

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

    public int getOrderListSize() {
        return orderListSize;
    }

    public void setOrderListSize(int orderListSize) {
        this.orderListSize = orderListSize;
    }
}
