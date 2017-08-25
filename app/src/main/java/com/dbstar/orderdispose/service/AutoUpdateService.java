package com.dbstar.orderdispose.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dbstar.orderdispose.MyApplication;
import com.dbstar.orderdispose.bean.Order;
import com.dbstar.orderdispose.constant.Constant;
import com.dbstar.orderdispose.constant.URL;
import com.dbstar.orderdispose.networkmanager.NetworkManager;
import com.dbstar.orderdispose.utils.HttpUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by wh on 2016/12/26.
 */
public class AutoUpdateService extends Service {

    private MyApplication application;
    private Handler handler;
    private static final String TAG = "dsw_AutoUpdateService";
    private MediaPlayer mediaPlayer;
    private AlarmManager manager;
    private PendingIntent pi;
    private List<Order.OrderBean> datas = new ArrayList<Order.OrderBean>();

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        application = (MyApplication) getApplication();

        //绑定方式不会直接开 onStartCommand
        manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 1 * 1 * 10 * 1000; // 这是1分钟的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        pi = PendingIntent.getService(this, 0, i, 0);
        //manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);


        //初始化 mediaPlayer
        new Thread() {
            @Override
            public void run() {

                AssetManager assetManager = AutoUpdateService.this.getAssets();
                AssetFileDescriptor fileDescriptor = null;
                try {
                    //新订单语音提示：提示音
                    fileDescriptor = assetManager.openFd("order.mp3");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer = new MediaPlayer();

                try {
                    mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                            fileDescriptor.getStartOffset(),
                            fileDescriptor.getLength());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                super.run();
            }
        }.start();
    }

    @Override
    public void onDestroy() {

        if(manager!=null && pi!=null){
            manager.cancel(pi);
        }
        super.onDestroy();
    }

    private OnMessageListener onMessageListener;
    public void setOnMessageListener(OnMessageListener onMessageListener) {
        this.onMessageListener = onMessageListener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand.");
        //访问网络，提醒更新
        updateOrder();

        //AlarmManager 轮询
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = Constant.CHECK_TIME; // 轮询新订单的时间间隔
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        //manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    private void updateOrder() {

        //定时访问网络如何？
        NetworkManager.getInstance().initialized(this);
        if(!NetworkManager.getInstance().isNetworkConnected()){
            //无网络连接
            //打开对话框
            if(onMessageListener!=null){;
                onMessageListener.onUpdate(Constant.MSG_NET_ERR);
            }
            return;
        }else {
            if(onMessageListener!=null){;
                onMessageListener.onUpdate(Constant.MSG_NET_OK);
            }
        }
        getUnHandleOrderList();
    }


    private void getUnHandleOrderList() {

        datas.clear();

        try {
            HttpUtil.sendOkHttpRequest(application.getServiceIP() + URL.NewOrder, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();

                    Order order =  null;
                    //解析访问网络获取到的 json数据 ，打印出来
                    try {
                        order = new Gson().fromJson(json, Order.class);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    Log.d(TAG, "onResponse: " + order);

                    if (order != null) {
                        datas.addAll(order.getData());
                    }

                    getUnHandleMovieOrderList();

                }

                @Override
                public void onFailure(Call call, IOException e) {
                    getUnHandleMovieOrderList();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //获取未处理电影订单
    private void getUnHandleMovieOrderList() {
        try {
            HttpUtil.sendOkHttpRequest(application.getServiceIP() + "/bar/media/getMediaNewOrder.do", new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    Order order =  null;
                    //解析访问网络获取到的 json数据 ，打印出来
                    try {
                        order = new Gson().fromJson(json, Order.class);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    Log.d(TAG, "onResponse: " + order);

                    if (order != null) {
                        datas.addAll(order.getData());
                    }


                    int preSize = application.getOrderListSize();
                    int nextSize = datas.size();



                    Log.d(TAG, "onResponse: preSize = " + preSize + " nextSize = " + nextSize);
                    // 新的订单列表 长度 大于原有的列表，视为有新消息
                    if (preSize < nextSize) {
                        if(application.isVoiceEnable() && mediaPlayer!=null){
                            mediaPlayer.start();
                        }
                        //打开对话框
                        if(onMessageListener!=null){;
                            onMessageListener.onUpdate(Constant.MSG_NEW_ORDER);
                        }
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {

                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new AutoUpdateServiceBinder();
    }

    public class AutoUpdateServiceBinder extends Binder {
        /**
         * 获取当前Service的实例
         * @return
         */
        public AutoUpdateService getService(){
            return AutoUpdateService.this;
        }
    }

}
