package com.penn.ppj;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.FragmentLoginBinding;
import com.penn.ppj.messageEvent.LoginRegisterProgressEvent;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPWarn;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.penn.ppj.R.string.login;

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

        binding.passwordTextInputEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login();
                    return true;
                }
                return false;
            }
        });

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
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
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
        PPHelper.showProgressDialog(getContext(), getString(R.string.login) + "...", null);
        PPHelper.login(binding.usernameTextInputEditText.getText().toString(), binding.passwordTextInputEditText.getText().toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(new Action() {
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
                })
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
                                Log.v("pplog", "error:" + throwable.toString());
                            }
                        }
                );
    }
}
