package com.dbstar.orderdispose.constant;

/**
 * Created by wh on 2017/8/25.
 */

public class URLFilm {
    //后台IP地址
    public static final String IP = "http://192.168.0.205:8080";

    //电影已处理订单
    public static final String OldOrder = "/bar/media/getMediaOldOrder.do";
    //电影未处理订单
    public static final String NewOrder = "/bar/media/getMediaNewOrder.do";
    //电影订单详情
    public static final String OrderItem = "/bar/media/getMediaOrderItem.do";
    //点餐订单详情-订单号
    public static final String NUMBER = "ordersId";
    //标记已经处理完成
    public static final String OrderMark = "/bar/media/getMediaOldOrderStaus.do";
    //订单号
    public static final String OrderMarkID = "ordersId=";
    //标记订单已经处理完毕
    public static final String OrderMarkFLAG = "flag=0";
}
