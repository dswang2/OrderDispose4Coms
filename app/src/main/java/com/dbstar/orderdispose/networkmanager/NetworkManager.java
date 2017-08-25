/*
 * Copyright (C) 2015 The Android Open Source Project.
 *
 *        yinglovezhuzhu@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dbstar.orderdispose.networkmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 网络状态管理类
 * Created by yinglovezhuzhu@gmail.com on 2015/7/23.
 */
public class NetworkManager {

    private Context mContext;

    //系统：网络管理类
    private ConnectivityManager mConnectivityManager;

    //设置一个网络状态改变监听：网络状态改变时，调用监听器的 notifyNetworkChanged() 方法
    private final NetworkObservable mNetworkObservable = new NetworkObservable();

    //网络连接状态
    private boolean mNetworkConnected = false;

    //系统：网络连接数据
    private NetworkInfo mCurrentNetwork = null;

    //是否已经初始化
    private boolean mInitialized = false;

    private static NetworkManager mInstance = null;

    private NetworkManager() {

    }

    public static NetworkManager getInstance() {
        //当两个并发线程访问同一个对象object中的这个synchronized(this)同步代码块时，一个时间内只能有一个线程得到执行
        //这是为了保证 mInstance对象 的唯一
        synchronized (NetworkManager.class) {
            if(null == mInstance) {
                mInstance = new NetworkManager();
            }
            return mInstance;
        }
    }

    public void initialized(Context context) {
        if(!mInitialized) {
            mContext = context.getApplicationContext();
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            // 注册一个广播接受者，接收系统发出的 CONNECTIVITY_ACTION 广播
            mContext.registerReceiver(new NetworkReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            mInitialized = true;
        }

        // 获得网络状态的初始参数
        mCurrentNetwork = mConnectivityManager.getActiveNetworkInfo();
        // 网络状态
        mNetworkConnected = null != mCurrentNetwork && mCurrentNetwork.isConnected();
    }

    /**
     * 网络是否连接
     * @return
     */
    public boolean isNetworkConnected() {
        return mNetworkConnected;
    }

    /**
     * 获取当前网络信息
     * @return 当前网络信息，如果有网络连接，则为null
     */
    public NetworkInfo getCurrentNetwork() {
        return mCurrentNetwork;
    }

    /**
     * 注册一个网络状态观察者
     * @param observer
     */
    // 这个 NetworkObserver observer 由 MainActivity 传递过来
    public void registerNetworkObserver(NetworkObserver observer) {
        synchronized (mNetworkObservable) {
            mNetworkObservable.registerObserver(observer);
        }
    }

    /**
     * 反注册一个网络状态观察者
     * @param observer
     */
    public void unregisterNetworkObserver(NetworkObserver observer) {
        synchronized (mNetworkObservable) {
            mNetworkObservable.unregisterObserver(observer);
        }
    }

    /**
     * 反注册所有的观察者，建议这个只在退出程序时做清理用
     */
    public void unregisterAllObservers() {
        synchronized (mNetworkObservable) {
            mNetworkObservable.unregisterAll();
        }
    }



    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(null == intent) {
                return;
            }
            if(!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                return;
            }

            NetworkInfo lastNetwork = mCurrentNetwork;
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            mNetworkConnected = !noConnectivity;
            if(mNetworkConnected) {
                mCurrentNetwork = mConnectivityManager.getActiveNetworkInfo();
            } else {
                mCurrentNetwork = null;
                // 没有网络连接，直接返回
            }

            mNetworkObservable.notifyNetworkChanged(mNetworkConnected, mCurrentNetwork, lastNetwork);
        }
    }


}
