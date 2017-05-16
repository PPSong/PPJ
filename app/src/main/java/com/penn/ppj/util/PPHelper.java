package com.penn.ppj.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.ppj.PPApplication;
import com.penn.ppj.model.Geo;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.ppEnum.PicStatus;

import org.json.JSONObject;

import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

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

                        initRealm(PPApplication.getContext(), phone);

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
                        return "OK";
                    }
                });
    }

    public static void error(String error) {
        Log.v("pplog", error);
        Toasty.error(PPApplication.getContext(), error, Toast.LENGTH_LONG, true).show();
    }


    public static String get80ImageUrl(String imageName) {
        return getImageUrl(imageName, 80);
    }

    public static String get800ImageUrl(String imageName) {
        return getImageUrl(imageName, 800);
    }

    private static String getImageUrl(String imageName, int size) {
        //容错
        if (TextUtils.isEmpty(imageName)){
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

    public static Geo getLatestGeo() {
        //pptodo implement it
        return Geo.getDefaultGeo();
    }

    public static void setPrefStringValue(String key, String value) {
        PPApplication.getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static String getPrefStringValue(String key, String defaultValue) {
        return PPApplication.getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static void removePrefItem(String key) {
        PPApplication.getContext().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE).edit().remove(AUTH_BODY_KEY).apply();
    }

}
