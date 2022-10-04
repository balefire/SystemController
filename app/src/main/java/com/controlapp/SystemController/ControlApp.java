package com.controlapp.SystemController;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

public class ControlApp extends Application {
    private static ControlApp instance;
    private final static String TAG = "[FJY Application]";
    private Handler handler = null;

    public void setHandler(Handler handler) {
        Log.v(TAG, "Set Handler [ " + handler + " ]");
        this.handler = handler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public Handler getHandler() {
        return handler;
    }

    public static ControlApp getInstance() {
        return instance;
    }
}
