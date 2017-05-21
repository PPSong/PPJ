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

        String authBody = PPHelper.getPrefStringValue(PPHelper.AUTH_BODY_KEY, "");
        if (!TextUtils.isEmpty(authBody)) {
            String[] tmpArr = authBody.split(",");
            String phone = tmpArr[0];
            String pwd = tmpArr[1];
            Log.v("pplog301", "getPrefStringValue");
            PPHelper.showProgressDialog(this, getString(R.string.login) + "...", null);
            PPHelper.login(phone, pwd)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    new Handler().postDelayed(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    PPHelper.hideProgressDialog();
                                                }
                                            },
                                            1000
                                    );
                                }
                            }
                    )
                    .subscribe(
                            new Consumer<String>() {
                                @Override
                                public void accept(@NonNull String s) throws Exception {
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
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
