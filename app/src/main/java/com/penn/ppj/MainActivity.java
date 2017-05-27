package com.penn.ppj;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoFragmentActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TResult;
import com.penn.ppj.messageEvent.MomentDeleteEvent;
import com.penn.ppj.messageEvent.MomentPublishEvent;
import com.penn.ppj.messageEvent.ToggleToolBarEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.databinding.ActivityMainBinding;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.MyProfile;
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
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;


import de.jonasrottmann.realmbrowser.RealmBrowser;
import es.dmoral.toasty.Toasty;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmResults;

import static android.R.attr.breadCrumbShortTitle;
import static android.R.attr.key;
import static android.app.Activity.RESULT_OK;
import static android.os.Build.VERSION_CODES.M;
import static com.penn.ppj.R.id.item_touch_helper_previous_elevation;
import static com.penn.ppj.R.id.main_nav_view;

import static com.penn.ppj.util.PPHelper.AUTH_BODY_KEY;
import static com.penn.ppj.util.PPHelper.currentUserAvatar;
import static com.penn.ppj.util.PPHelper.ppWarning;
import static com.penn.ppj.util.PPHelper.uploadSingleImage;
import static com.penn.ppj.util.PPRetrofit.authBody;

public class MainActivity extends TakePhotoFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int DASHBOARD = 0;
    private static final int NEARBY = 1;
    private static final int NOTIFICATION = 2;

    private static final int CREATE_MOMENT = 1001;

    private ActivityMainBinding binding;

    private static final int PP_ACCESS_COARSE_LOCATION = 1000;

    private Menu menu;

    private DashboardFragment dashboardFragment;

    private NearbyFragment nearbyFragment;

    private NotificationFragment notificationFragment;

    private BehaviorSubject<Integer> scrollDirection = BehaviorSubject.<Integer>create();

    private MyProfile myProfile;

    private Realm realm = Realm.getDefaultInstance();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("pplog561", "requestCode:" + requestCode + ",resultCode:" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_MOMENT && resultCode == RESULT_OK) {

            Log.v("pplog580", "onActivityResult:" + data.getStringExtra("momentCreatingId"));

            binding.mainViewPager.setCurrentItem(DASHBOARD);
            dashboardFragment.binding.mainRecyclerView.scrollToPosition(0);
            PPHelper.uploadMoment(data.getStringExtra("momentCreatingId"));
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        realm.close();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("pplog520", "mainActivity onCreate");
        EventBus.getDefault().register(this);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().setStatusBarColor(Color.TRANSPARENT);
//        }
//
//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) binding.mainToolbar.getLayoutParams();
        layoutParams.height = PPHelper.getStatusBarAddActionBarHeight(this);
        Log.v("pplog", "getStatusBarAddActionBarHeight:" + PPHelper.getStatusBarAddActionBarHeight(this));
        binding.mainToolbar.setLayoutParams(layoutParams);

        binding.mainToolbar.setPadding(0, PPHelper.getStatusBarHeight(this), 0, 0);

        //setSupportActionBar(binding.mainToolbar);

        binding.cameraFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.mainFloatingActionMenu.close(true);
                takePhoto(true);
            }
        });

        binding.photoFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.mainFloatingActionMenu.close(true);
                takePhoto(false);

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, binding.mainToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(main_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View hView = navigationView.getHeaderView(0);
        final ImageView avatarImageView = (ImageView) hView.findViewById(R.id.imageView);

        myProfile = realm.where(MyProfile.class).equalTo("userId", PPHelper.currentUserId).findFirst();
        myProfile.addChangeListener(new RealmChangeListener<MyProfile>() {
            @Override
            public void onChange(MyProfile element) {
                Picasso.with(MainActivity.this)
                        .load(PPHelper.get80ImageUrl(element.getAvatar()))
                        .into(avatarImageView);
            }
        });

        Picasso.with(this)
                .load(PPHelper.get80ImageUrl(currentUserAvatar))
                .into(avatarImageView);

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MyProfileActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

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

        binding.mainToolbar.setTitle(getString(R.string.dashboard));

        binding.mainViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case DASHBOARD:
                        binding.mainToolbar.setTitle(getString(R.string.dashboard));
                        break;
                    case NEARBY:
                        binding.mainToolbar.setTitle(getString(R.string.nearby));
                        break;
                    case NOTIFICATION:
                        binding.mainToolbar.setTitle(getString(R.string.notification));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, binding.mainDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        requestPermission(this);
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
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void ToggleToolBarEvent(ToggleToolBarEvent event) {
//        if (event.show) {
//            binding.mainToolbar.animate()
//                    .translationY(0)
//                    .setDuration(100L)
//                    .setInterpolator(new LinearInterpolator());
//        } else {
//            binding.mainToolbar.animate()
//                    .translationY(PPHelper.getStatusBarAddActionBarHeight(this) * -1)
//                    .setDuration(100L)
//                    .setInterpolator(new LinearInterpolator());
//        }
//    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void MomentPublishEvent(MomentPublishEvent event) {
        PPHelper.refreshMoment(event.id);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void MomentDeleteEvent(MomentDeleteEvent event) {
        Log.v("pplog508", "MomentDeleteEvent:" + event.id);
        PPHelper.removeMoment(event.id);
    }

    private void requestPermission(Activity activity) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PP_ACCESS_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PP_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    PPHelper.error("无此权限程序不能正常运行!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
            Toasty.info(this, getString(R.string.exit_tips)).show();
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

    private void takePhoto(boolean camera) {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        Uri imageUri = Uri.fromFile(file);

        CompressConfig config = new CompressConfig.Builder()
                .setMaxSize(1024 * 1024)
                .setMaxPixel(1024)
                .create();

        TakePhoto takePhoto = getTakePhoto();

        takePhoto.onEnableCompress(config, true);

        if (camera) {
            takePhoto.onPickFromCapture(imageUri);
        } else {
            takePhoto.onPickFromGallery();
        }
    }

    @Override
    public void takeFail(TResult result, String msg) {
        PPHelper.error(msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            realm.where(MomentCreating.class).equalTo("status", MomentStatus.PREPARE.toString()).findAll().deleteAllFromRealm();

            MomentCreating newOne = new MomentCreating();
            newOne.setId();
            Log.v("pplog580", "newOneOne:" + newOne.getId());
            newOne.setStatus(MomentStatus.PREPARE);
            newOne.setPic(result.getImages().get(0).getCompressPath());

            realm.copyToRealm(newOne);

            realm.commitTransaction();
        }

        Intent intent = new Intent(this, CreateMomentActivity.class);
        startActivityForResult(intent, CREATE_MOMENT);
    }

    //-----ppTest-----
    private void startRealmModelsActivity() {
        Realm realm = Realm.getDefaultInstance();
        RealmConfiguration configuration = realm.getConfiguration();
        realm.close();
        RealmBrowser.startRealmModelsActivity(this, configuration);
    }
}
