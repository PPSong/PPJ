package com.penn.ppj;

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

import com.google.gson.JsonArray;
import com.penn.ppj.messageEvent.InitLoadingEvent;
import com.penn.ppj.messageEvent.ToggleToolBarEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.databinding.ActivityMainBinding;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.CurUser;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPPagerAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;

import static com.penn.ppj.util.PPHelper.AUTH_BODY_KEY;
import static com.penn.ppj.util.PPRetrofit.authBody;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int DASHBOARD = 0;
    private static final int NEARBY = 1;

    private ActivityMainBinding binding;
    private boolean inInitLoading = false;

    private Menu menu;

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
        adapter.addFragment(new DashboardFragment(), "Category 1");
        adapter.addFragment(new NearbyFragment(), "Category 2");
        binding.mainViewPager.setAdapter(adapter);
        //有几个tab就设几防止page自己重新刷新
        binding.mainViewPager.setOffscreenPageLimit(2);

        initLoading();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        setupMenuIcon();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_out:
                loginOut();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void LoginEvent(UserLoginEvent event) {
        setupMenuIcon();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void LoginEvent(UserLogoutEvent event) {
        setupMenuIcon();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void InitLoadingEvent(InitLoadingEvent event) {
        Log.v("pplog", "InitLoadingEvent");
        initLoading();
    }

    private void createMoment() {

    }

    private void initLoading() {
        synchronized (MainActivity.class) {
            if (inInitLoading) {
                return;
            }

            inInitLoading = true;

            try (Realm realm = Realm.getDefaultInstance()) {
                CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();

                if (currentUser.isInitLoadingFinished()) {
                    inInitLoading = false;
                    return;
                }

                long earliestCreateTime = currentUser.getEarliestMomentCreateTime();

                PPJSONObject jBody = new PPJSONObject();
                jBody
                        .put("before", earliestCreateTime);

                final Observable<String> apiResult = PPRetrofit.getInstance().api("timeline.mine", jBody.getJSONObject());

                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(@NonNull String s) throws Exception {
                                        PPWarn ppWarn = PPHelper.ppWarning(s);

                                        if (ppWarn != null) {
                                            throw new Exception(ppWarn.msg);
                                        }

                                        processMoment(s);
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
        }
    }

    private void processMoment(String s) {
        try (Realm realm = Realm.getDefaultInstance()) {
            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();

            realm.beginTransaction();

            Log.v("pplog", s);
            JsonArray ja = PPHelper.ppFromString(s, "data.timeline").getAsJsonArray();

            for (int i = 0; i < ja.size(); i++) {
                long createTime = PPHelper.ppFromString(s, "data.timeline." + i + "._info.createTime").getAsLong();

                Moment moment = new Moment();
                moment.setId(PPHelper.ppFromString(s, "data.timeline." + i + ".id").getAsString());
                moment.setCreateTime(createTime);
                moment.setStatus(MomentStatus.NET);
                moment.setAvatar(PPHelper.ppFromString(s, "data.timeline." + i + "._info._creator.head").getAsString());

                Pic pic = new Pic();
                pic.setKey(PPHelper.ppFromString(s, "data.timeline." + i + "._info.pics.0").getAsString());
                pic.setStatus(PicStatus.NET);
                moment.setPic(pic);

                realm.insertOrUpdate(moment);
                currentUser.setEarliestMomentCreateTime(createTime);
            }

            if (ja.size() < PPHelper.TIMELINE_MINE_PAGE_SIZE) {
                currentUser.setInitLoadingFinished(true);
            }

            realm.commitTransaction();

            inInitLoading = false;
            //继续触发initLoading
            EventBus.getDefault().post(new InitLoadingEvent());
        }
    }

    private void loginOut() {
        if (CurUser.logined()) {
            CurUser.clear();
            EventBus.getDefault().post(new UserLoginEvent());
        } else {
            CurUser.getInstance();
            EventBus.getDefault().post(new UserLogoutEvent());
        }
    }

    private void setupMenuIcon() {
        if (CurUser.logined()) {
            menu.findItem(R.id.login_out).setIcon(getResources().getDrawable(R.drawable.ic_exit_to_app_black_24dp));
        } else {
            menu.findItem(R.id.login_out).setIcon(getResources().getDrawable(R.drawable.ic_person_black_24dp));
        }
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
