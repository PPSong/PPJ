package com.penn.ppj;

import android.app.Application;
import android.content.Context;
import android.databinding.BindingAdapter;
import android.util.Log;
import android.widget.ImageView;

import com.penn.ppj.util.PPHelper;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by penn on 15/05/2017.
 */

public class PPApplication extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    public static Context getContext() {
        return appContext;
    }

    @BindingAdapter({"bind:imageUrl"})
    public static void setImageViewResource(final ImageView imageView, String pic) {
        PPHelper.setImageViewResource(imageView, pic, 800);
    }

    @BindingAdapter({"bind:avatarImageUrl"})
    public static void setAvatarImageViewResource(final ImageView imageView, String pic) {
        Log.v("pploge", "setAvatarImageViewResource");
        PPHelper.setImageViewResource(imageView, pic, 80);
    }
}
