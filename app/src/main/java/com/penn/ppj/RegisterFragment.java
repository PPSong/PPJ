package com.penn.ppj;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxRadioGroup;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.ppj.databinding.FragmentRegisterBinding;
import com.penn.ppj.messageEvent.ToggleToolBarEvent;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function4;
import io.reactivex.functions.Function5;
import io.reactivex.functions.Function6;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.realm.Realm;
import io.realm.Sort;


public class RegisterFragment extends Fragment {
    private static BehaviorSubject<String> birthdayString = BehaviorSubject.create();

    private FragmentRegisterBinding binding;

    private Realm realm;

    private Observable<String> phoneInputObservable;

    private Observable<String> passwordInputObservable;

    private Observable<String> nicknameInputObservable;

    private Observable<String> sexInputObservable;

    private Observable<String> agreeInputObservable;

    private Observable<Object> randomNicknameButtonObservable;

    private Observable<String> verifyCodeInputObservable;

    private Observable<Object> requestVerifyCodeButtonObservable;

    private Observable<Object> signUpButtonObservable;

    private Observable<Long> timeLeftObservable;

    public RegisterFragment() {
        // Required empty public constructor
    }

    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_register, container, false);
        View view = binding.getRoot();
        //end common

        setup();

        return view;
    }

    private void setup() {
        //手机输入监控
        phoneInputObservable = RxTextView.textChanges(binding.phoneInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isPhoneValid(getContext(), charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.phoneInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //密码输入监控
        passwordInputObservable = RxTextView.textChanges(binding.passwordInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isPasswordValid(getContext(), charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.passwordInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //验证码输入监控
        verifyCodeInputObservable = RxTextView.textChanges(binding.verifyCodeInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isVerifyCodeValid(getContext(), charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.verifyCodeInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //获取验证码密码按钮监控
        requestVerifyCodeButtonObservable = RxView.clicks(binding.requestVerifyCodeButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        requestVerifyCodeButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                requestVerifyCode();
                            }
                        }
                );

        //昵称输入监控
        nicknameInputObservable = RxTextView.textChanges(binding.nicknameInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isNicknameValid(getContext(), charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.nicknameInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //性别输入监控
        sexInputObservable = RxRadioGroup.checkedChanges(binding.sexInput)
                .skip(1)
                .map(
                        new Function<Integer, String>() {

                            @Override
                            public String apply(Integer integer) throws Exception {
                                String error = "";
                                Log.v("ppLog", "RxRadioGroup:" + integer);
                                if (integer < 0) {
                                    error = getString(R.string.error_field_required);
                                }
                                return error;
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.sexInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //同意守则勾选监控
        agreeInputObservable = RxCompoundButton.checkedChanges(binding.agreeCheck)
                .skip(1)
                .map(
                        new Function<Boolean, String>() {

                            @Override
                            public String apply(Boolean aBoolean) throws Exception {
                                String error = "";
                                if (!aBoolean) {
                                    error = getString(R.string.must_agree);
                                }
                                return error;
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                Log.v("ppLog", "RxCompoundButton");
                                binding.agreeInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //注册按钮是否可用

        Observable
                .combineLatest(
                        verifyCodeInputObservable,
                        phoneInputObservable,
                        passwordInputObservable,
                        nicknameInputObservable,
                        sexInputObservable,
                        agreeInputObservable,
                        new Function6<String, String, String, String, String, String, Boolean>() {

                            @Override
                            public Boolean apply(String s, String s2, String s3, String s4, String s5, String s6) throws Exception {
                                return TextUtils.isEmpty(s) && TextUtils.isEmpty(s2) && TextUtils.isEmpty(s3) && TextUtils.isEmpty(s4) && TextUtils.isEmpty(s5) && TextUtils.isEmpty(s6);
                            }
                        }
                )
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                binding.signUpButton.setEnabled(aBoolean);
                            }
                        }
                );

        //获取随机昵称按钮监控
        randomNicknameButtonObservable = RxView.clicks(binding.getRandomNicknameButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        randomNicknameButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                requestRandomNickname();
                            }
                        }
                );

        //注册按钮监控
        signUpButtonObservable = RxView.clicks(binding.signUpButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        signUpButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                register();
                            }
                        }
                );

        //控制获取验证码倒计时
        timeLeftObservable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                .doOnNext(
                        new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                Log.v("ppLog", "doOnNext" + aLong);
                            }
                        }
                )
                .takeWhile(
                        new Predicate<Long>() {
                            @Override
                            public boolean test(Long aLong) throws Exception {
                                Log.v("ppLog", "takeWhile");
                                return aLong <= PPHelper.REQUEST_VERIFY_CODE_INTERVAL;
                            }
                        }
                );
    }

    private void register() {
        PPHelper.showProgressDialog(getContext(), getString(R.string.register) + "...", null);
        final String tmpPhone = binding.phoneInput.getText().toString();
        final String tmpPassword = binding.passwordInput.getText().toString();

        PPJSONObject jBody = new PPJSONObject();

        int sex = binding.sexInput.getCheckedRadioButtonId() == R.id.male_radio ? 1 : 2;

        jBody
                .put("phone", tmpPhone)
                .put("pwd", tmpPassword)
                .put("gender", sex)
                .put("checkCode", binding.verifyCodeInput.getText().toString())
                .put("nickname", binding.nicknameInput.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.register", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, Observable<String>>() {
                            @Override
                            public Observable<String> apply(String s) throws Exception {
                                Log.v("ppLog", "pp test:" + s);

                                //如果有错误通过throw new Exception传递到subscribe的onError
                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPJSONObject jBody = new PPJSONObject();

                                jBody
                                        .put("phone", tmpPhone)
                                        .put("pwd", tmpPassword);

                                final Observable<String> apiResult = PPRetrofit.getInstance().api("user.login", jBody.getJSONObject());
                                return apiResult;
                            }
                        }
                )
                .flatMap(
                        new Function<String, ObservableSource<String>>() {

                            @Override
                            public ObservableSource<String> apply(@NonNull String s) throws Exception {
                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                return PPHelper.login(tmpPhone, tmpPassword);
                            }
                        }
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPHelper.hideProgressDialog();
                                getActivity().finish();
//                                Intent intent = new Intent(getContext(), MainActivity.class);
//                                startActivity(intent);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.hideProgressDialog();
                                PPHelper.error(throwable.toString());
                            }
                        }
                );
    }

    private void requestRandomNickname() {
        PPJSONObject jBody = new PPJSONObject();

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.randomNickName", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                Log.v("ppLog", "get result:" + s);

                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    PPHelper.error(ppWarn.msg);

                                    return;
                                }

                                String nickname = PPHelper.ppFromString(s, "data.nickname").getAsString();
                                binding.nicknameInput.setText(nickname);
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                PPHelper.error(t1.toString());
                                Log.v("pplog", t1.toString());
                            }
                        }
                );
    }

    private void requestVerifyCode() {
        binding.requestVerifyCodeButton.setEnabled(false);
        timeLeftObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        if (aLong == PPHelper.REQUEST_VERIFY_CODE_INTERVAL) {
                            binding.requestVerifyCodeButton.setEnabled(true);
                            binding.requestVerifyCodeButton.setText(getString(R.string.get_random));
                        } else {
                            binding.requestVerifyCodeButton.setText("" + (PPHelper.REQUEST_VERIFY_CODE_INTERVAL - aLong));
                        }
                    }
                });


        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", binding.phoneInput.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.sendRegisterCheckCode", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                Log.v("ppLog511", "get result:" + s);

                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    PPHelper.error(ppWarn.msg);

                                    return;
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                PPHelper.error(t1.toString());
                                Log.v("pploge", t1.toString());
                            }
                        }
                );
    }
}
