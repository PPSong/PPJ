package com.penn.ppj;

import android.app.ProgressDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;

import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.messageEvent.LoginRegisterProgressEvent;
import com.penn.ppj.messageEvent.ToggleToolBarEvent;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPPagerAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        PPPagerAdapter adapter = new PPPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new LoginFragment(), getString(R.string.login));
        adapter.addFragment(new RegisterFragment(), getString(R.string.register));
        binding.mainViewPager.setAdapter(adapter);

        binding.mainTabLayout.setupWithViewPager(binding.mainViewPager);
    }
}
