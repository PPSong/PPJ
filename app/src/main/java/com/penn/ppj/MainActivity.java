package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import com.penn.ppj.messageEvent.MomentPublishEvent;
import com.penn.ppj.messageEvent.ToggleToolBarEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.databinding.ActivityMainBinding;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PPValueType;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.CurUser;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPPagerAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPSocketSingleton;
import com.penn.ppj.util.PPWarn;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

import static android.R.attr.key;
import static android.os.Build.VERSION_CODES.M;
import static com.penn.ppj.util.PPHelper.AUTH_BODY_KEY;
import static com.penn.ppj.util.PPHelper.ppWarning;
import static com.penn.ppj.util.PPRetrofit.authBody;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int DASHBOARD = 0;
    private static final int NEARBY = 1;
    private static final int NOTIFICATION = 2;

    private static final int CREATE_MOMENT = 1001;

    private ActivityMainBinding binding;

    private Menu menu;

    private Configuration config = new Configuration.Builder().build();

    private UploadManager uploadManager = new UploadManager(config);

    private DashboardFragment dashboardFragment;

    private NearbyFragment nearbyFragment;

    private NotificationFragment notificationFragment;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_MOMENT && resultCode == RESULT_OK) {
            binding.mainViewPager.setCurrentItem(DASHBOARD);
            dashboardFragment.binding.mainRecyclerView.scrollToPosition(0);
            uploadMoment();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) binding.mainToolbar.getLayoutParams();
        layoutParams.height = PPHelper.getStatusBarAddActionBarHeight(this);
        binding.mainToolbar.setLayoutParams(layoutParams);

        setSupportActionBar(binding.mainToolbar);

        binding.mainFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createMoment();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, binding.mainToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.main_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        PPPagerAdapter adapter = new PPPagerAdapter(getSupportFragmentManager());
        dashboardFragment = new DashboardFragment();
        nearbyFragment = new NearbyFragment();
        notificationFragment = new NotificationFragment();
        adapter.addFragment(dashboardFragment, "C1");
        adapter.addFragment(nearbyFragment, "C2");
        adapter.addFragment(notificationFragment, "C3");
        binding.mainViewPager.setAdapter(adapter);
        //有几个tab就设几防止page自己重新刷新
        binding.mainViewPager.setOffscreenPageLimit(3);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        this.menu = menu;
//        getMenuInflater().inflate(R.menu.main, menu);
//        setupMenuIcon();
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.login_out:
//                loginOut();
//                return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    //-----helper-----
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void ToggleToolBarEvent(ToggleToolBarEvent event) {
        if (event.show) {
            binding.mainToolbar.animate()
                    .translationY(0)
                    .setDuration(100L)
                    .setInterpolator(new LinearInterpolator());
        } else {
            binding.mainToolbar.animate()
                    .translationY(PPHelper.getStatusBarAddActionBarHeight(this) * -1)
                    .setDuration(100L)
                    .setInterpolator(new LinearInterpolator());
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void MomentPublishEvent(MomentPublishEvent event) {
        PPHelper.refreshMoment(event.id);
    }

    private Observable<String> uploadSingleImage(final byte[] data, final String key, final String token) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                uploadManager.put(data, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success:" + key);
                                    emitter.onNext(key);
                                    emitter.onComplete();
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                    Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable(info.error.toString()));
                                    emitter.onError(apiError);
                                }
                                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                            }
                        }, null);
            }
        });
    }

    private void uploadMoment() {
        final String needUploadMomentId;
        final byte[] imageData;
        String address;
        String geo;
        String content;
        long createTime;

        try (Realm realm = Realm.getDefaultInstance()) {
            MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("status", MomentStatus.PREPARE.toString()).findFirst();

            needUploadMomentId = momentCreating.getId();
            imageData = momentCreating.getPic();
            address = momentCreating.getAddress();
            geo = momentCreating.getGeo();
            content = momentCreating.getContent();
            createTime = momentCreating.getCreateTime();

            realm.beginTransaction();
            //修改momentCreating放入Moment状态
            momentCreating.setStatus(MomentStatus.LOCAL);

            realm.commitTransaction();
        }

        //申请上传图片的token
        final String key = needUploadMomentId + "_0";
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("type", "public")
                .put("filename", key);

        final Observable<String> requestToken = PPRetrofit.getInstance().api("system.generateUploadToken", jBody.getJSONObject());

        //上传moment
        JSONArray jsonArrayPics = new JSONArray();

        jsonArrayPics.put(key);

        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("pics", jsonArrayPics)
                .put("address", address)
                .put("geo", geo)
                .put("content", content)
                .put("createTime", createTime);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("moment.publish", jBody1.getJSONObject());

        requestToken
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, ObservableSource<String>>() {
                            @Override
                            public ObservableSource<String> apply(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg + ":" + key);
                                }
                                String token = PPHelper.ppFromString(s, "data.token").getAsString();
                                return uploadSingleImage(imageData, key, token);
                            }
                        }
                )
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        return apiResult1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {

                                Log.v("pplog", "publish ok:" + s);

                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg);
                                }

                                String uploadedMomentId = PPHelper.ppFromString(s, "data.id").getAsString();

                                try (Realm realm = Realm.getDefaultInstance()) {
                                    MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("id", needUploadMomentId).findFirst();

                                    realm.beginTransaction();
                                    //删除momentCreating
                                   // momentCreating.setStatus(MomentStatus.NET);
                                    momentCreating.deleteFromRealm();

                                    realm.commitTransaction();

                                    //通知更新本地Moment中对应moment
                                    EventBus.getDefault().post(new MomentPublishEvent(uploadedMomentId));
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    MomentCreating momentCreating = realm.where(MomentCreating.class).equalTo("id", needUploadMomentId).findFirst();

                                    realm.beginTransaction();
                                    //修改momentCreating放入Moment状态
                                    momentCreating.setStatus(MomentStatus.FAILED);

                                    realm.commitTransaction();
                                }
                                PPHelper.error(throwable.toString());
                            }
                        }
                );
    }

    private void createMoment() {
        Intent intent = new Intent(this, CreateMomentActivity.class);
        startActivityForResult(intent, CREATE_MOMENT);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.test) {
            // Handle the camera action
            startRealmModelsActivity();
        } else if (id == R.id.dashboard) {
            binding.mainViewPager.setCurrentItem(DASHBOARD);
        } else if (id == R.id.nearby) {
            binding.mainViewPager.setCurrentItem(NEARBY);
        } else if (id == R.id.notification) {
            binding.mainViewPager.setCurrentItem(NOTIFICATION);
        } else if (id == R.id.exit) {
            PPHelper.clear();
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //-----ppTest-----
    private void startRealmModelsActivity() {
        Realm realm = Realm.getDefaultInstance();
        RealmConfiguration configuration = realm.getConfiguration();
        realm.close();
        RealmBrowser.startRealmModelsActivity(this, configuration);
    }
}
