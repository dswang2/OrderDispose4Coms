package com.dbstar.orderdispose.constant;

/**
 * Created by wh on 2017/1/5.
 */
public class Constant {

    //订单类型：点菜
    public static final String ORDER_TYPE_MEAL = "0";

    //订单类型：购物
    public static final String ORDER_TYPE_SHOPING = "1";

    //购物电话
    public static final String PHONE_NUMBER_SHOPING = "8188";

    //订餐电话
    public static final String PHONE_NUMBER_MEAL = "1188";

    //最大打印次数
    public static final int PRINT_MAX_COUNT = 10;

    public static final String AUTO_PRINT = "auto_print";
    public static final String VOICE_ENABLE = "voice_enable";
    public static final String PRINT_COUNT = "print_count";

    //轮询服务消息类型
    //网络状态正常
    public static final int MSG_NET_OK = 0;
    //网络状态错误
    public static final int MSG_NET_ERR = 1;
    //有新的订单产生
    public static final int MSG_NEW_ORDER = 2;

    //后台服务轮询新订单的时间间隔：10秒钟
    public static final int CHECK_TIME = 1 * 1 * 10 * 1000;

    public static final String SERVICE_IP = "service_ip";
}
