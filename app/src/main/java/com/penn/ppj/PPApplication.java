package com.penn.ppj;

import android.app.Application;
import android.content.Context;
import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.PicStatus;
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
        Log.v("pplog", "imageUrl");
        PPHelper.setImageViewResource(imageView, pic, 800);
    }

    @BindingAdapter({"bind:imageData"})
    public static void setImageViewDataResource(final ImageView imageView, byte[] pic) {
        Log.v("pplog", "imageData");
        if (pic == null) {
            imageView.setImageResource(android.R.color.transparent);
            return;
        }
        Bitmap bmp = BitmapFactory.decodeByteArray(pic, 0, pic.length);
        imageView.setImageBitmap(bmp);
    }

    @BindingAdapter({"bind:avatarImageUrl"})
    public static void setAvatarImageViewResource(final ImageView imageView, String pic) {
        Log.v("pplog", "avatarImageUrl");
        PPHelper.setImageViewResource(imageView, pic, 80);
    }

    @BindingAdapter({"bind:imagePicUrl"})
    public static void setImagePicViewResource(final ImageView imageView, Pic pic) {
        if (pic.getStatus() == PicStatus.NET) {
            Log.v("pplog", "imagePicUrl");
            PPHelper.setImageViewResource(imageView, pic.getKey(), 180);
        }
        //pptodo load local image
    }

    @BindingAdapter({"bind:pp_reference_time"})
    public static void setTimeAgo(final RelativeTimeTextView relativeTimeTextView, long time) {
        Log.v("pplog", "setTimeAgo");
        if (time > 0) {
            relativeTimeTextView.setReferenceTime(time);
        }
        Log.v("pplog", "setTimeAgo 0");
    }
}
