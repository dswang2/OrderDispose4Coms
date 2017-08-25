package com.dbstar.crashcanary;


import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.dbstar.crashcanary.model.CrashCause;
import com.dbstar.crashcanary.model.CrashLogs;
import com.dbstar.crashcanary.ui.CrashInfoActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashCanary implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashCanary";
    private Context mContext;

    private String nameString;
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public static CrashCanary install(Application context) {
        return new CrashCanary(context);
    }

    private CrashCanary(Context context) {
        mContext = context;
        nameString = getClass().getSimpleName();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable ex) {
        CrashLogs logs = getCrashLogs(ex);
        saveCrashInfo2File(ex);
        CrashInfoActivity.actionStart(mContext, logs);
        exitApp();
    }

    private String saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();

        sb.append(result);

        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = nameString + "-" + time + "-" + timestamp
                    + ".log";
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {

                //String path = "/storage/sdcard/crash/";
                String path = Environment.getExternalStorageDirectory().getPath() ;
                path += File.separator + "crashlog" + File.separator ;
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                Log.e(TAG, "path + fileName");
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }

    @NonNull
    private CrashLogs getCrashLogs(Throwable ex) {
        StringWriter strWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(strWriter);
        ex.printStackTrace(printWriter);

        CrashLogs logs = new CrashLogs();
        CrashCause cause = new CrashCause(strWriter.toString());
        logs.add(cause);

        Throwable nextCause = ex.getCause();
        while (nextCause != null) {
            nextCause.printStackTrace(printWriter);
            nextCause = nextCause.getCause();

            logs.add(new CrashCause(strWriter.toString()));
        }
        printWriter.close();
        return logs;
    }

    private void exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}
