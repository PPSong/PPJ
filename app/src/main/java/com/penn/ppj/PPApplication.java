package com.penn.ppj;

import android.app.Application;
import android.content.Context;
import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPHelper;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by penn on 15/05/2017.
 */

public class PPApplication extends Application {
    private static Context appContext;

    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = new MyLocationListener();

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        Realm.init(appContext);

        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);
        //注册监听函数

        initLocation();

        mLocationClient.start();
    }

    public static Context getContext() {
        return appContext;
    }

    @BindingAdapter({"bind:imageUrl"})
    public static void setImageViewResource(final ImageView imageView, String pic) {
//        Log.v("pplog", "imageUrl");
        PPHelper.setImageViewResource(imageView, pic, 800);
    }

    @BindingAdapter({"bind:imageData"})
    public static void setImageViewDataResource(final ImageView imageView, byte[] pic) {
//        Log.v("pplog", "imageData");
        if (pic == null) {
            imageView.setImageResource(android.R.color.transparent);
            return;
        }
        Bitmap bmp = BitmapFactory.decodeByteArray(pic, 0, pic.length);
        imageView.setImageBitmap(bmp);
    }

    @BindingAdapter({"bind:avatarImageUrl"})
    public static void setAvatarImageViewResource(final ImageView imageView, String pic) {
//        Log.v("pplog", "avatarImageUrl");
        PPHelper.setImageViewResource(imageView, pic, 80);
    }

    @BindingAdapter({"bind:imagePicUrl"})
    public static void setImagePicViewResource(final ImageView imageView, Pic pic) {
        if (pic.getStatus() == PicStatus.NET) {
//            Log.v("pplog", "imagePicUrl");
            PPHelper.setImageViewResource(imageView, pic.getKey(), 180);
        } else {
            byte[] data = pic.getThumbLocalData();
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            imageView.setImageBitmap(bmp);
        }
    }

    @BindingAdapter({"bind:pp_reference_time"})
    public static void setTimeAgo(final RelativeTimeTextView relativeTimeTextView, long time) {
//        Log.v("pplog", "setTimeAgo");
        if (time > 0) {
            relativeTimeTextView.setReferenceTime(time);
        }
//        Log.v("pplog", "setTimeAgo 0");
    }

    @BindingAdapter({"bind:mapImageUrl"})
    public static void setMapImage(final ImageView imageView, String geo) {
//        Log.v("pplog", "mapImageUrl");
        PPHelper.setMapImage(imageView, geo);
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备

        option.setCoorType("bd09ll");
        //可选，默认gcj02，设置返回的定位结果坐标系

        int span = 10000;
        option.setScanSpan(span);
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要

        option.setOpenGps(true);
        //可选，默认false,设置是否使用gps

        option.setLocationNotify(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果

        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”

        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到

        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死

        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集

        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要

        mLocationClient.setLocOption(option);
    }

    class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            //获取定位结果
            StringBuffer sb = new StringBuffer(256);

            sb.append("time : ");
            sb.append(location.getTime());    //获取定位时间

            sb.append("\nerror code : ");
            sb.append(location.getLocType());    //获取类型类型

            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());    //获取纬度信息

            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());    //获取经度信息

            if (location.getLocType() != BDLocation.TypeOffLineLocation) {
                PPHelper.setPrefStringValue(PPHelper.LATEST_GEO, String.format("%.9f", location.getLongitude()) + "," + String.format("%.9f", location.getLatitude()));
                PPHelper.setPrefStringValue(PPHelper.LATEST_ADDRESS, location.getAddrStr());
            }

            sb.append("\nradius : ");
            sb.append(location.getRadius());    //获取定位精准度

            if (location.getLocType() == BDLocation.TypeGpsLocation) {

                // GPS定位结果
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());    // 单位：公里每小时

                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());    //获取卫星数

                sb.append("\nheight : ");
                sb.append(location.getAltitude());    //获取海拔高度信息，单位米

                sb.append("\ndirection : ");
                sb.append(location.getDirection());    //获取方向信息，单位度

                sb.append("\naddr : ");
                sb.append(location.getAddrStr());    //获取地址信息

                sb.append("\ndescribe : ");
                sb.append("gps定位成功");

            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {

                // 网络定位结果
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());    //获取地址信息

                sb.append("\noperationers : ");
                sb.append(location.getOperators());    //获取运营商信息

                sb.append("\ndescribe : ");
                sb.append("网络定位成功");

            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {

                // 离线定位结果
                sb.append("\ndescribe : ");
                sb.append("离线定位成功，离线定位结果也是有效的");

            } else if (location.getLocType() == BDLocation.TypeServerError) {

                sb.append("\ndescribe : ");
                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");

            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {

                sb.append("\ndescribe : ");
                sb.append("网络不同导致定位失败，请检查网络是否通畅");

            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {

                sb.append("\ndescribe : ");
                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");

            }

            sb.append("\nlocationdescribe : ");
            sb.append(location.getLocationDescribe());    //位置语义化信息

            List<Poi> list = location.getPoiList();    // POI数据
            if (list != null) {
                sb.append("\npoilist size = : ");
                sb.append(list.size());
                for (Poi p : list) {
                    sb.append("\npoi= : ");
                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
                }
            }

//            Log.i("BaiduLocationApiDem", sb.toString());
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
}
