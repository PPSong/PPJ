package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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

import com.penn.ppj.messageEvent.ToggleToolBarEvent;
import com.penn.ppj.messageEvent.UserLoginEvent;
import com.penn.ppj.messageEvent.UserLogoutEvent;
import com.penn.ppj.databinding.ActivityMainBinding;
import com.penn.ppj.util.CurUser;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPPagerAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int DASHBOARD = 0;
    private static final int NEARBY = 1;

    private ActivityMainBinding binding;

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
        binding.mainViewPager.setCurrentItem(NEARBY);
        setupMenuIcon();
    }

    private void createMoment() {

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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
