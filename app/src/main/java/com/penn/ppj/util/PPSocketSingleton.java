package com.penn.ppj.util;

import android.util.Log;

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
                                    Log.v("ppLog", "Socket.EVENT_CONNECT");
                                    try {
                                        JSONObject tmpBody = new JSONObject();
                                        JSONObject _sess = new JSONObject();
                                        _sess.put("userid", PPHelper.currentUserId);

                                        _sess.put("token", PPHelper.token);
                                        _sess.put("tokentimestamp", PPHelper.tokenTimestamp);
                                        tmpBody.put("_sess", _sess);


                                        socket.emit("$init", tmpBody);
                                        Log.v("pplog165", tmpBody.toString());
                                        //socket.disconnect();
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
                                    Log.v("ppLog", "$init from server:" + msg);
                                }
                            })
                    .on(
                            "$kick",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    String msg = args[0].toString();
                                    Log.v("ppLog", "$kick from server:" + msg);
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
                                    Log.v("ppLog", "Socket.EVENT_DISCONNECT");
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
        Log.v("pplog502", "sync");
        PPHelper.networkConnectNeedToRefresh();
    }
}
