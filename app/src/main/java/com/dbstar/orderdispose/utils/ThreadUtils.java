package com.dbstar.orderdispose.utils;

import android.os.Handler;

public class ThreadUtils {
	
	/** 接收一个runable，并在子线程执行 */
	public static void runInChildThread(Runnable runnable) {
		new Thread(runnable).start();
	}

	public static Handler mHandler = new Handler();
	
	/** 接收一个runable，并在主线程执行 */
	public static void runInUIThread(Runnable runnable) {
		mHandler.post(runnable);

	}

}
