package com.penn.ppj.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.ppj.LoginActivity;
import com.penn.ppj.MomentDetailActivity;
import com.penn.ppj.PPApplication;
import com.penn.ppj.PPService;
import com.penn.ppj.R;
import com.penn.ppj.model.Geo;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Message;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.ppj.PPApplication.getContext;

/**
 * Created by penn on 13/05/2017.
 */

public class PPHelper {
    public static final int TIMELINE_MINE_PAGE_SIZE = 20;
    public static final String APP_NAME = "PPJ";
    public static final String AUTH_BODY_KEY = "AUTH_BODY_KEY";

    public static final String qiniuBase = "http://7xu8w0.com1.z0.glb.clouddn.com/";

    public static String currentUserId;
    public static String token;
    public static long tokenTimestamp;

    public static String currentUserNickname;
    public static String currentUserAvatar;
    public static String socketUrl;
    public static String baiduAk;

    public static void clear() {
        currentUserId = null;
        token = null;
        tokenTimestamp = 0;
        currentUserNickname = null;
        currentUserAvatar = null;
        socketUrl = null;
        baiduAk = null;
        PPRetrofit.authBody = null;
        removePrefItem(AUTH_BODY_KEY);

        CurUser.clear();
        PPSocketSingleton.close();
        PPApplication.getContext().stopService(new Intent(PPApplication.getContext(), PPService.class));
        Intent intent = new Intent(PPApplication.getContext(), LoginActivity.class);
        PPApplication.getContext().startActivity(intent);
    }

    public static int getStatusBarAddActionBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            result += TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return result;
    }

    public static JsonElement ppFromString(String json, String path, PPValueType type) {
        JsonElement jsonElement = ppFromString(json, path);
        if (jsonElement == null) {
            switch (type) {
                case ARRAY:
                    return new JsonArray();
                case INT:
                    return new JsonPrimitive(0);
                case STRING:
                    return new JsonPrimitive("");
                default:
                    return null;
            }
        }

        return jsonElement;
    }

    public static JsonElement ppFromString(String json, String path) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement item = parser.parse(json);
            if (path == null || path.length() == 0 || Pattern.matches("\\.+", path)) {
                //Log.v("ppLog", "解析整个json String");
                return item;
            }
            String[] seg = path.split("\\.");
            for (int i = 0; i < seg.length; i++) {
                if (i > 0) {
                    //Log.v("ppLog", "解析完毕:" + seg[i - 1]);
                    //Log.v("ppLog", "-------");
                }
                //Log.v("ppLog", "准备解析:" + seg[i]);
                if (seg[i].length() == 0) {
                    //""情况
                    //Log.v("ppLog", "解析空字符串的path片段, 停止继续解析");
                    return null;
                }
                if (item != null) {
                    //当前path片段item不为null
                    //Log.v("ppLog", "当前path片段item不为null");
                    if (item.isJsonArray()) {
                        //当前path片段item为数组
                        //Log.v("ppLog", "当前path片段item为数组");
                        String regex = "\\d+";
                        if (Pattern.matches("\\d+", seg[i])) {
                            //当前path片段描述为数组格式
                            //Log.v("ppLog", "当前path片段描述为数组格式");
                            item = item.getAsJsonArray().get(Integer.parseInt(seg[i]));
                        } else {
                            //当前path片段描述不为数组格式
                            //Log.v("ppLog", "当前path片段描述不为数组格式");
                            //Log.v("ppLog", "path中间片段描述错误:" + seg[i] + ", 停止继续解析");
                            return null;
                        }
                    } else if (item.isJsonObject()) {
                        //当前path片段item为JsonObject
                        //Log.v("ppLog", "当前path片段item为JsonObject");
                        item = item.getAsJsonObject().get(seg[i]);
                    } else {
                        //当前path片段item为JsonPrimitive
                        //Log.v("ppLog", "当前path片段item为JsonPrimitive");
                        //Log.v("ppLog", "path中间片段取值为JsonPrimitive, 停止继续解析");
                        return null;
                    }
                } else {
                    //当前path片段item为null
                    //Log.v("ppLog", "当前path片段item为null");
                    Log.v("ppLog", path + ":path中间片段取值为null, 停止继续解析");
                    return null;
                }
            }
            return item;
        } catch (Exception e) {
            Log.v("ppLog", "Json解析错误" + e);
            return null;
        }
    }

    public static PPWarn ppWarning(String jServerResponse) {
        int code = ppFromString(jServerResponse, "code").getAsInt();
        if (code != 1) {
            return new PPWarn(jServerResponse);
        } else {
            return null;
        }
    }

    public static void initRealm(Context context, String phone) {
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name(phone + ".realm")
                .build();
        //清除当前用户的数据文件, 测试用
        boolean clearData = true;
        if (clearData) {
            Realm.deleteRealm(config);
        }

        Realm.setDefaultConfiguration(config);
    }

    public static Observable<String> login(final String phone, final String pwd) {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", phone)
                .put("pwd", pwd);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("user.login", jBody.getJSONObject());

        return apiResult
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        initRealm(getContext(), phone);

                        try (Realm realm = Realm.getDefaultInstance()) {

                            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();

                            realm.beginTransaction();

                            if (currentUser == null) {
                                currentUser = new CurrentUser();
                                currentUser.setUserId(ppFromString(s, "data.userid").getAsString());
                            }

                            currentUser.setToken(ppFromString(s, "data.token").getAsString());
                            currentUser.setTokenTimestamp(ppFromString(s, "data.tokentimestamp").getAsLong());

                            realm.copyToRealmOrUpdate(currentUser);

                            realm.commitTransaction();

                            //设置PPRetrofit authBody
                            String authBody = new JSONObject()
                                    .put("userid", currentUser.getUserId())
                                    .put("token", currentUser.getToken())
                                    .put("tokentimestamp", currentUser.getTokenTimestamp())
                                    .toString();
                            PPRetrofit.authBody = authBody;
                            PPHelper.setPrefStringValue(AUTH_BODY_KEY, phone + "," + pwd);
                            currentUserId = currentUser.getUserId();
                            token = currentUser.getToken();
                            tokenTimestamp = currentUser.getTokenTimestamp();
                        }

                        return PPRetrofit.getInstance().api("user.startup", null);
                    }
                })
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        //pptodo connect to rongyun
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.beginTransaction();

                            CurrentUser currentUser = realm.where(CurrentUser.class)
                                    .findFirst();

                            String tmpAk = ppFromString(s, "data.settings.geo.ak_browser").getAsString();

                            currentUser.setPhone(ppFromString(s, "data.userInfo.phone").getAsString());
                            currentUser.setNickname(ppFromString(s, "data.userInfo.nickname").getAsString());
                            currentUser.setGender(ppFromString(s, "data.userInfo.gender").getAsInt());
                            currentUser.setBirthday(ppFromString(s, "data.userInfo.birthday").getAsLong());
                            currentUser.setHead(ppFromString(s, "data.userInfo.head").getAsString());
                            currentUser.setBaiduApiUrl(ppFromString(s, "data.settings.geo.api").getAsString());
                            currentUser.setBaiduAkBrowser(tmpAk);
                            currentUser.setSocketHost(ppFromString(s, "data.settings.socket.host").getAsString());
                            currentUser.setSocketPort(ppFromString(s, "data.settings.socket.port").getAsInt());

                            //pptodo get im_unread_count_int
                            RealmList<Pic> pics = currentUser.getPics();
                            JsonArray tmpArr = ppFromString(s, "data.userInfo.params.more.pics", PPValueType.ARRAY).getAsJsonArray();
                            for (int i = 0; i < tmpArr.size(); i++) {
                                Pic pic = new Pic();
                                pic.setKey("profile_pic" + i);
                                pic.setStatus(PicStatus.NET);
                                pics.add(pic);
                            }

                            currentUserNickname = currentUser.getNickname();
                            currentUserAvatar = currentUser.getHead();
                            socketUrl = currentUser.getSocketHost() + ":" + currentUser.getSocketPort();

                            //设置baiduAk
                            baiduAk = tmpAk;
                            realm.commitTransaction();
                        }

                        Intent intent = new Intent(PPApplication.getContext(), new PPService().getClass());
                        PPApplication.getContext().startService(intent);

                        networkConnectNeedToRefresh();
                        return "OK";
                    }
                });
    }

    public static void error(String error) {
        Log.v("pplog", error);
        Toasty.error(getContext(), error, Toast.LENGTH_LONG, true).show();
    }


    public static String get80ImageUrl(String imageName) {
        return getImageUrl(imageName, 80);
    }

    public static String get180ImageUrl(String imageName) {
        return getImageUrl(imageName, 180);
    }

    public static String get800ImageUrl(String imageName) {
        return getImageUrl(imageName, 800);
    }

    private static String getImageUrl(String imageName, int size) {
        //容错
        if (TextUtils.isEmpty(imageName)) {
            //pptodo 可以使用默认表示空白的图片
            return "";
        }

        //pptodo 要添加是local图片的情况
        if (imageName.startsWith("http")) {
            return imageName;
        } else {
            //pptodo 如果是"", 返回默认图片
            String result = qiniuBase + imageName + "?imageView2/1/w/" + size + "/h/" + size + "/interlace/1/";
            return result;
        }
    }

    public static int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 180);
        return noOfColumns;
    }

    public static int calculateHeadMaxOffset(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        float height_16_9 = dpWidth * 9 / 16;
        float height_4_3 = dpWidth * 3 / 4;
        int result = (int) ((height_4_3 - height_16_9) * displayMetrics.density);

        return result;
    }

    public static int calculateHeadHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        int height_4_3 = (int) dpWidth * 3 / 4;

        return height_4_3;
    }

    public static int calculateMomentOverHeight() {
        Context context = PPApplication.getContext();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        int result = (int) ((dpWidth / calculateNoOfColumns(context)) * 9 / 16);

        return result;
    }

    public static void likeButtonAppear(Context context, View floatingActionButton, int topMargin) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) floatingActionButton.getLayoutParams();
        params.topMargin = topMargin;
        floatingActionButton.setLayoutParams(params);

        floatingActionButton.startAnimation(AnimationUtils.loadAnimation(context, R.anim.appear));
    }

    public static Geo getLatestGeo() {
        //pptodo implement it
        return Geo.getDefaultGeo();
    }

    public static void setPrefStringValue(String key, String value) {
        getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static String getPrefStringValue(String key, String defaultValue) {
        return getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static void removePrefItem(String key) {
        getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().remove(AUTH_BODY_KEY).apply();
    }

    public static void getNewMoment(long mostNewCreateTime) {
        final int pageSize = 20;

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("after", mostNewCreateTime)
                .put("sort", "1");

        final Observable<String> apiResult = PPRetrofit.getInstance().api("timeline.mine", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                long newStartTime = processMoment(s, pageSize);
                                if (newStartTime != -1) {
                                    getNewMoment(newStartTime);
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                            }
                        }
                );
    }

    public static void getNewMessage(long mostNewCreateTime) {
        final int pageSize = 20;

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("after", mostNewCreateTime)
                .put("sort", "1");

        final Observable<String> apiResult = PPRetrofit.getInstance().api("message.list", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                long newStartTime = processMessage(s, pageSize);
                                if (newStartTime != -1) {
                                    getNewMessage(newStartTime);
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                            }
                        }
                );
    }

    //包含所有网络获得链接后需要更新的事情
    public static void networkConnectNeedToRefresh() {
        //我的moment

        long mostNewMomentCreateTime = 0;
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Moment> moments = realm.where(Moment.class).findAllSorted("createTime", Sort.DESCENDING);
            if (moments.size() > 0) {
                mostNewMomentCreateTime = moments.get(0).getCreateTime();
            }
        }
        getNewMoment(mostNewMomentCreateTime);

        //我的notification
        long mostNewMessageCreateTime = 0;
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Message> messages = realm.where(Message.class).findAllSorted("createTime", Sort.DESCENDING);
            if (messages.size() > 0) {
                mostNewMessageCreateTime = messages.get(0).getCreateTime();
            }
        }
        getNewMessage(mostNewMessageCreateTime);

        //我的follow
        PPJSONObject jBodyFollow = new PPJSONObject();
        jBodyFollow
                .put("before", "0");

        final Observable<String> apiResultFollow = PPRetrofit.getInstance()
                .api("friend.myFollows", jBodyFollow.getJSONObject());

        apiResultFollow
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                Log.v("pplog", "s1:" + s);

                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.beginTransaction();

                                    JsonArray ja = PPHelper.ppFromString(s, "data").getAsJsonArray();
                                    long now = System.currentTimeMillis();

                                    for (int i = 0; i < ja.size(); i++) {
                                        RelatedUser relatedUser = new RelatedUser();
                                        relatedUser.setType(RelatedUserType.FOLLOW);
                                        relatedUser.setUserId(PPHelper.ppFromString(s, "data." + i + ".id").getAsString());
                                        relatedUser.setId();
                                        relatedUser.setNickname(PPHelper.ppFromString(s, "data." + i + ".nickname").getAsString());
                                        relatedUser.setAvatar(PPHelper.ppFromString(s, "data." + i + ".head").getAsString());
                                        relatedUser.setCreateTime(PPHelper.ppFromString(s, "data." + i + ".time").getAsLong());
                                        relatedUser.setLastVisitTime(now);

                                        realm.insertOrUpdate(relatedUser);
                                    }

                                    //把服务器上已删除的user从本地删掉
                                    realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FOLLOW.toString()).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

                                    realm.commitTransaction();
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                            }
                        }
                );

        //我的fan
        PPJSONObject jBodyFan = new PPJSONObject();
        jBodyFan
                .put("before", "0");

        final Observable<String> apiResultFan = PPRetrofit.getInstance()
                .api("friend.myFans", jBodyFan.getJSONObject());

        apiResultFan
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                Log.v("pplog", "s2:" + s);

                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.beginTransaction();

                                    JsonArray ja = PPHelper.ppFromString(s, "data").getAsJsonArray();
                                    long now = System.currentTimeMillis();

                                    for (int i = 0; i < ja.size(); i++) {
                                        RelatedUser relatedUser = new RelatedUser();
                                        relatedUser.setType(RelatedUserType.FAN);
                                        relatedUser.setUserId(PPHelper.ppFromString(s, "data." + i + ".id").getAsString());
                                        relatedUser.setId();
                                        relatedUser.setNickname(PPHelper.ppFromString(s, "data." + i + ".nickname").getAsString());
                                        relatedUser.setAvatar(PPHelper.ppFromString(s, "data." + i + ".head").getAsString());
                                        relatedUser.setCreateTime(PPHelper.ppFromString(s, "data." + i + ".time").getAsLong());
                                        relatedUser.setLastVisitTime(now);

                                        realm.insertOrUpdate(relatedUser);
                                    }

                                    //把服务器上已删除的user从本地删掉
                                    realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FAN.toString()).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

                                    realm.commitTransaction();
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                            }
                        }
                );

        //我的friend
        PPJSONObject jBodyFriend = new PPJSONObject();
        jBodyFriend
                .put("needInfo", "1");

        final Observable<String> apiResultFriend = PPRetrofit.getInstance()
                .api("friend.mine", jBodyFriend.getJSONObject());

        apiResultFriend
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                Log.v("pplog", "s3:" + s);

                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.beginTransaction();

                                    JsonArray ja = PPHelper.ppFromString(s, "data.list").getAsJsonArray();
                                    long now = System.currentTimeMillis();

                                    for (int i = 0; i < ja.size(); i++) {
                                        RelatedUser relatedUser = new RelatedUser();
                                        relatedUser.setType(RelatedUserType.FRIEND);
                                        relatedUser.setUserId(PPHelper.ppFromString(s, "data.list." + i + ".id").getAsString());
                                        relatedUser.setId();
                                        relatedUser.setNickname(PPHelper.ppFromString(s, "data.list." + i + ".nickname").getAsString());
                                        relatedUser.setAvatar(PPHelper.ppFromString(s, "data.list." + i + ".head").getAsString());
                                        relatedUser.setCreateTime(PPHelper.ppFromString(s, "data.list." + i + ".time").getAsLong());
                                        relatedUser.setLastVisitTime(now);

                                        realm.insertOrUpdate(relatedUser);
                                    }

                                    //把服务器上已删除的user从本地删掉
                                    realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FRIEND.toString()).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

                                    realm.commitTransaction();
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                error(throwable.toString());
                            }
                        }
                );
    }

    public static void setImageViewResource(final ImageView imageView, String pic, int size) {
        if (TextUtils.isEmpty(pic)) {
            Log.v("pplog", "isEmpty");
            return;
        }

        if (size == 800) {
            Log.v("pplog", "800");
            String picUrl = get800ImageUrl(pic);
            Picasso.with(getContext())
                    .load(picUrl)
                    .error(R.mipmap.ic_launcher)
                    //          .into(imageView);
                    .into(new Target() {
                        //pptodo 改进取色方案
                        @Override
                        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                    /* Save the bitmap or do something with it here */
                            Palette p = Palette.from(bitmap).generate();
                            //Set it in the ImageView
                            imageView.setImageBitmap(bitmap);
                            imageView.setBackground(new ColorDrawable(p.getVibrantColor(getContext().getResources().getColor(R.color.colorPrimaryDark))));
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {
                        }
                    });
        } else if (size == 180) {
            Log.v("pplog", "180");
            String picUrl = get180ImageUrl(pic);
            Picasso.with(getContext())
                    .load(picUrl)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
        } else {
            //default
            Log.v("pplog", "80");
            String picUrl = get80ImageUrl(pic);
            Picasso.with(getContext())
                    .load(picUrl)
                    .error(R.mipmap.ic_launcher)
                    .into(imageView);
        }
    }

    public static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    public static void refreshMoment(String id) {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("id", id);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.detail", jBody.getJSONObject());

        apiResult.subscribe(
                new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) throws Exception {
                        Log.v("pplog200", s);

                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        long createTime = PPHelper.ppFromString(s, "data.createTime").getAsLong();

                        try (Realm realm = Realm.getDefaultInstance()) {
                            Moment moment = new Moment();
                            moment.setKey(createTime + "_" + PPHelper.ppFromString(s, "data._creator.id").getAsString());
                            moment.setId(PPHelper.ppFromString(s, "data._id").getAsString());
                            moment.setUserId(PPHelper.ppFromString(s, "data._creator.id").getAsString());
                            moment.setCreateTime(createTime);
                            moment.setStatus(MomentStatus.NET);
                            moment.setAvatar(PPHelper.ppFromString(s, "data._creator.head").getAsString());

                            Pic pic = new Pic();
                            pic.setKey(PPHelper.ppFromString(s, "data.pics.0").getAsString());
                            pic.setStatus(PicStatus.NET);
                            moment.setPic(pic);

                            realm.beginTransaction();

                            realm.insertOrUpdate(moment);

                            realm.commitTransaction();
                        }
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        error(throwable.toString());
                    }
                }
        );
    }

    private static long processMoment(String s, int pageSize) {
        try (Realm realm = Realm.getDefaultInstance()) {

            realm.beginTransaction();

            Log.v("pplog", s);
            JsonArray ja = PPHelper.ppFromString(s, "data.timeline").getAsJsonArray();

            for (int i = 0; i < ja.size(); i++) {
                long createTime = PPHelper.ppFromString(s, "data.timeline." + i + "._info.createTime").getAsLong();

                Moment moment = new Moment();
                moment.setKey(createTime + "_" + PPHelper.ppFromString(s, "data.timeline." + i + "._info._creator.id").getAsString());
                moment.setId(PPHelper.ppFromString(s, "data.timeline." + i + ".id").getAsString());
                moment.setUserId(PPHelper.ppFromString(s, "data.timeline." + i + "._info._creator.id").getAsString());
                moment.setCreateTime(createTime);
                moment.setStatus(MomentStatus.NET);
                moment.setAvatar(PPHelper.ppFromString(s, "data.timeline." + i + "._info._creator.head").getAsString());

                Pic pic = new Pic();
                pic.setKey(PPHelper.ppFromString(s, "data.timeline." + i + "._info.pics.0").getAsString());
                pic.setStatus(PicStatus.NET);
                moment.setPic(pic);

                realm.insertOrUpdate(moment);
            }

            realm.commitTransaction();

            if (ja.size() < pageSize) {
                return -1;
            } else {
                return PPHelper.ppFromString(s, "data.timeline." + (ja.size() - 1) + "._info.createTime").getAsLong();
            }
        }
    }

    private static long processMessage(String s, int pageSize) {
        long newestCreateTime = 0;

        try (Realm realm = Realm.getDefaultInstance()) {

            realm.beginTransaction();

            Log.v("pplog", "pptest1:" + s);

            JsonArray ja = PPHelper.ppFromString(s, "data.list").getAsJsonArray();

            Log.v("pplog", "pptest2:" + s);

            for (int i = 0; i < ja.size(); i++) {
                Message message = parseMessage(PPHelper.ppFromString(s, "data.list." + i).getAsJsonObject().toString());

                realm.insertOrUpdate(message);

                if (i == (ja.size() - 1)) {
                    newestCreateTime = message.getCreateTime();
                }
            }

            realm.commitTransaction();

            if (ja.size() < pageSize) {
                return -1;
            } else {
                return newestCreateTime;
            }
        }
    }

    private static Message parseMessage(String s) {
        Log.v("pplog", "parseMessage:" + s);
        Message message = new Message();
        message.setId(PPHelper.ppFromString(s, "id").getAsString());
        int type = PPHelper.ppFromString(s, "type").getAsInt();
        message.setType(type);
        message.setRead(PPHelper.ppFromString(s, "read").getAsInt() == 1 ? true : false);
        message.setCreateTime(PPHelper.ppFromString(s, "createTime").getAsLong());
        String content = "";

        Log.v("pplog", "parseMessage type:" + type);

        //nickname, avatar, content
        switch (type) {
            case 8:
            case 9:
            case 10:
                message.setNickname(PPHelper.ppFromString(s, "params.targetUser.nickname").getAsString());
                message.setAvatar(PPHelper.ppFromString(s, "params.targetUser.head").getAsString());
                content = PPHelper.ppFromString(s, "content").getAsString();
                message.setContent(TextUtils.isEmpty(content) ? PPApplication.getContext().getResources().getString(R.string.empty) : content);
                return message;

            case 15:
                message.setNickname(PPHelper.ppFromString(s, "params.targetUser.nickname").getAsString());
                message.setAvatar(PPHelper.ppFromString(s, "params.targetUser.head").getAsString());
                content = PPHelper.ppFromString(s, "content").getAsString();
                message.setContent(TextUtils.isEmpty(content) ? PPApplication.getContext().getResources().getString(R.string.empty) : content);
                return message;

            case 16:
                message.setNickname(PPHelper.ppFromString(s, "params.targetUser.nickname").getAsString());
                message.setAvatar(PPHelper.ppFromString(s, "params.targetUser.head").getAsString());
                content = PPHelper.ppFromString(s, "content").getAsString();
                message.setContent(TextUtils.isEmpty(content) ? PPApplication.getContext().getResources().getString(R.string.empty) : content);
                return message;

            default:
                Log.v("pplog", "未处理:" + type);
                message.setNickname(PPHelper.currentUserNickname);
                message.setAvatar(PPHelper.currentUserAvatar);
                content = "未处理:" + s;
                message.setContent(TextUtils.isEmpty(content) ? PPApplication.getContext().getResources().getString(R.string.empty) : content);
                return message;
        }
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
