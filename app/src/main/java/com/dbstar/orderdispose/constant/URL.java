package com.dbstar.orderdispose.constant;

/**
 * Created by wh on 2016/12/26.
 */
public class URL {

    //后台IP地址
    public static final String IP = "http://192.168.0.205:8080";
//    public static final String IP = "http://192.168.0.254:8080";
//    public static final String IP = "http://116.231.55.196:9877";
//    192.168.0.254

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

    //购物已处理订单
    public static final String ShoppingOldOrder = "/bar/media/getShoppingOldOrder.do";

    //购物未处理订单
    public static final String ShoppingNewOrder = "/bar/media/getShoppingNewOrder.do\n";

    //购物详情
    public static final String ShoppingOrderItem = "/bar/media/getShoppingOrderItem.do";

    //一键送物已处理订单
    public static final String AccessoriesOldOrder = "/bar/media/getAccessoriesOldOrder.do";

    //一键送物未处理订单
    public static final String AccessoriesNewOrder = "/bar/media/getAccessoriesNewOrder.do";


    //一键送物详情
    public static final String AccessoriesOrderItem = "/bar/media/getAccessoriesOrderItem.do";

}
