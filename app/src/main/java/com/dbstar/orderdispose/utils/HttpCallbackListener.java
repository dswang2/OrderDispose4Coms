package com.dbstar.orderdispose.utils;

public interface HttpCallbackListener {

    void onFinish(String response);

    void onError(Exception e);

}