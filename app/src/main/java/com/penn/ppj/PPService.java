package com.penn.ppj;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.penn.ppj.util.PPSocketSingleton;

import static com.penn.ppj.util.PPHelper.socketUrl;

/**
 * Created by penn on 27/03/2017.
 */

public class PPService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.v("ppLog", "PPService onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("ppLog", "onStartCommand");
        MyTask myTask = new MyTask();
        myTask.execute();

        return START_STICKY;
    }

    private class MyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.v("pplog162", "testUrl2:" + socketUrl);
            PPSocketSingleton.getInstance(socketUrl);

            return null;
        }
    }

    @Override
    public void onDestroy() {
        Log.v("ppLog", "PPService onDestroy");
        super.onDestroy();
    }
}
