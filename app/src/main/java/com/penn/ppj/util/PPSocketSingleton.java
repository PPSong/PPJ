package com.penn.ppj.util;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.penn.ppj.MainActivity;
import com.penn.ppj.PPApplication;
import com.penn.ppj.R;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.penn.ppj.util.PPHelper.LOGIN_INFO;
import static com.penn.ppj.util.PPHelper.getPrefStringValue;
import static com.penn.ppj.util.PPHelper.ppWarning;

/**
 * Created by penn on 03/05/2017.
 */

public class PPSocketSingleton {
    private static PPSocketSingleton instance;

    private static Socket socket;

    private PPSocketSingleton(String url) {
        Log.v("ppLog", "PPSocketSingleton");
        Log.v("pplog160", "socketUrl:" + url);
        try {
            //pptodo 需要改进, 如果从sharepreference中去取而不是写死
            socket = IO.socket("http://jbapp.magicfish.cn:80");

            socket
                    .on(
                            Socket.EVENT_CONNECT,
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    Log.v("ppLog1000", "Socket.EVENT_CONNECT");
                                    try {
                                        JSONObject tmpBody = new JSONObject();
                                        JSONObject _sess = new JSONObject();

                                        String loginInfo = PPHelper.getPrefStringValue(PPHelper.LOGIN_INFO, "");

                                        if (TextUtils.isEmpty(loginInfo)) {
                                            close();
                                        } else {
                                            String[] tmpStrArr = loginInfo.split(",");
                                            _sess.put("userid", "" + tmpStrArr[0]);

                                            _sess.put("token", "" + tmpStrArr[1]);
                                            _sess.put("tokentimestamp", "" + tmpStrArr[2]);
                                            tmpBody.put("_sess", _sess);


                                            socket.emit("$init", tmpBody);
                                            Log.v("pplog165", tmpBody.toString());
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                            })
                    .on(
                            "$init",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    String msg = args[0].toString();
                                    Log.v("ppLog1000", "$init from server:" + msg);
                                }
                            })
                    .on(
                            "$kick",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    String msg = args[0].toString();
                                    Log.v("ppLog1000", "$kick from server:" + msg);
                                }
                            })
                    .on(
                            "sync",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    sync();
                                }
                            })
                    .on(
                            Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    Log.v("ppLog1000", "Socket.EVENT_DISCONNECT");
                                }

                            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        socket.close();
        instance = null;
    }

    public static PPSocketSingleton getInstance(String url) {
        if (instance == null) {
            instance = new PPSocketSingleton(url);
        }
        Log.v("pplog162", "testUrl3:");
        Log.v("pplog162", "testUrl4:");

        return instance;
    }

    private void sync() {
        //send notification
// Sets an ID for the notification
        int mNotificationId = 001;
// Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) PPApplication.getContext().getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, getNotification("test"));
        Log.v("pplog1000", "sync");
        PPHelper.networkConnectNeedToRefresh();
    }

    private Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(PPApplication.getContext());
        builder.setContentTitle("Scheduled Notification");
        builder.setContentText(content);
        builder.setSmallIcon(R.drawable.ic_location_on_black_24dp);
        return builder.build();
    }
}
