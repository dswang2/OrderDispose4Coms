package com.dbstar.orderdispose.constant;

/**
 * Created by wh on 2016/12/26.
 */
public class URLShopping {

    //后台IP地址
    public static final String IP = "http://192.168.0.205:8080";

    //点餐已处理订单
    public static final String OldOrder = "/bar/media/getOldOrder.do";
    //点餐未处理订单
    public static final String NewOrder = "/bar/media/getNewOrder.do";
    //点餐订单详情
    public static final String OrderItem = "/bar/media/getOrderItem.do";
    //点餐订单详情-订单号
    public static final String NUMBER = "number";
    //标记已经处理完成
    public static final String OrderMark = "/bar/media/updateMark.do";
    //订单号
    public static final String OrderMarkID = "id";
    //标记订单已经处理完毕
    public static final String OrderMarkFLAG = "flag=0";

}
