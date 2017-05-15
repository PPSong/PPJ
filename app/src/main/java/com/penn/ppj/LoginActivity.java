package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPPagerAdapter;

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
