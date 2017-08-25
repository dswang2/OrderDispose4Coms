package com.dbstar.orderdispose.utils;

/**
 * Created by wh on 2017/1/5.
 */
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
public class ToastUtils {
    public static Toast sToast;
    /**
     * 展示一个安全的土司
     * @param activity
     * @param msg
     */
    public static void showSafeToast(final Activity activity,final String msg){
        if(Thread.currentThread().getName().equals("main")){
            if(sToast == null) {
                sToast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);  //正常执行
            }
            else {
                sToast.setText(msg);  //用于覆盖前面未消失的提示信息
            }
            sToast.show();
        }else{
            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(sToast == null) {
                        sToast = Toast.makeText(activity, msg, Toast.LENGTH_SHORT);  //正常执行
                    }
                    else {
                        sToast.setText(msg);  //用于覆盖前面未消失的提示信息
                    }
                    sToast.show();
                }
            });
        }

    }

    /**
     * 展示一个Toast，会覆盖之前的Toast
     * @param context
     * @param msg
     */
    public static void showSafeToast(final Context context,final String msg){
            if(sToast == null) {
                sToast = Toast.makeText(context, msg, Toast.LENGTH_LONG);  //正常执行
            }
            else {
                sToast.setText(msg);  //用于覆盖前面未消失的提示信息
            }
            sToast.show();


    }
}