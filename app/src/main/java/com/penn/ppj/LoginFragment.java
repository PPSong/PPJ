package com.penn.ppj;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.FragmentLoginBinding;
import com.penn.ppj.util.PPHelper;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_login, container, false);

        setup();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
        super.onDestroyView();
    }

    private void setup() {
        //登录按钮监控
        Observable<Object> loginButtonObservable = RxView.clicks(binding.loginButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(loginButtonObservable
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                login();
                            }
                        }
                )
        );
    }

    private void login() {
        PPHelper.login(binding.usernameTextInputEditText.getText().toString(), binding.passwordTextInputEditText.getText().toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                Intent intent = new Intent(getContext(), MainActivity.class);
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
