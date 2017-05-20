package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.widget.CompoundButton;

import com.google.gson.JsonArray;
import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.databinding.ActivityUserHomePageBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.UserHomePage;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmChangeListener;

import static com.penn.ppj.R.id.liked;

public class UserHomePageActivity extends AppCompatActivity {

    private ActivityUserHomePageBinding binding;

    private Realm realm;

    private String userId;

    private UserHomePage userHomePage;

    private RealmChangeListener<UserHomePage> userHomePageChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_home_page);

        binding.followToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TransitionManager.beginDelayedTransition(binding.mainLinearLayout);
            }
        });

        //like按钮监控
        Observable<Object> followButtonObservable = RxView.clicks(binding.followToggleButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        followButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                boolean followed = binding.followToggleButton.isChecked();
                                Log.v("pplog", "follow2:" + followed);
                                followOrUnfollowMoment(followed);
                            }
                        }
                );

        realm = Realm.getDefaultInstance();

        userId = getIntent().getStringExtra("userId");

        UserHomePage tmpUserHomePage = realm.where(UserHomePage.class).equalTo("userId", userId).findFirst();

        if (tmpUserHomePage != null) {
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.beginTransaction();
                tmpUserHomePage.setLastVisitTime(System.currentTimeMillis());
                realm.commitTransaction();
            }
        }

        userHomePageChangeListener = new RealmChangeListener<UserHomePage>() {
            @Override
            public void onChange(UserHomePage element) {
                binding.setData(element);
            }
        };

        setupBindingData(tmpUserHomePage);

        getServerUserHomePage(userId);

        showUserMoments(userId);
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void followOrUnfollowMoment(boolean follow) {
        String api = follow ? "friend.follow" : "friend.unFollow";

        Log.v("pplog253", "api:" + api + ",userId:" + userId);

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", userId)
                .put("isFree", "true");

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api(api, jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                getServerUserHomePage(userId);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                restoreLocalUserHomePage();
                            }
                        }
                );
    }

    private void restoreLocalUserHomePage() {
        binding.setData(userHomePage);
    }

    private void setupBindingData(UserHomePage userHomePage) {
        if (userHomePage == null) {
            return;
        }

        synchronized (this) {
            if (this.userHomePage == null) {
                this.userHomePage = userHomePage;
                this.userHomePage.addChangeListener(userHomePageChangeListener);
                binding.setData(this.userHomePage);
            }
        }
    }

    private void getServerUserHomePage(final String userId) {
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("target", userId);

        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("friend.isFollowed", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("target", userId);

        final Observable<String> apiResult2 = PPRetrofit.getInstance().api("user.info", jBody2.getJSONObject());

        Observable
                .zip(
                        apiResult1, apiResult2, new BiFunction<String, String, String[]>() {

                            @Override
                            public String[] apply(@NonNull String s, @NonNull String s2) throws Exception {
                                String[] result = {s, s2};

                                return result;
                            }
                        }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String[]>() {
                            @Override
                            public void accept(@NonNull String[] result) throws Exception {
                                //更新本地UserHomePage
                                PPWarn ppWarn = PPHelper.ppWarning(result[0]);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPWarn ppWarn2 = PPHelper.ppWarning(result[1]);

                                if (ppWarn2 != null) {
                                    throw new Exception(ppWarn2.msg);
                                }

                                processUserHomePage(result[0], result[1]);
                                Log.v("pplog", "getServerUserHomePage:" + result[0]);

                                if (userHomePage == null) {
                                    setupBindingData(realm.where(UserHomePage.class).equalTo("userId", userId).findFirst());
                                }
                            }
                        }
                        ,
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                Log.v("pplog", throwable.toString());
                            }
                        }
                );
    }

    private void processUserHomePage(String isFollowed, String userInfo) {
        //构造userHomePage
        long now = System.currentTimeMillis();

        UserHomePage userHomePage = new UserHomePage();
        userHomePage.setFollowed(PPHelper.ppFromString(isFollowed, "data.follow").getAsInt() == 0 ? false : true);
        userHomePage.setUserId(PPHelper.ppFromString(userInfo, "data.profile.id").getAsString());
        userHomePage.setNickname(PPHelper.ppFromString(userInfo, "data.profile.nickname").getAsString());
        userHomePage.setAvatar(PPHelper.ppFromString(userInfo, "data.profile.head").getAsString());
        userHomePage.setLastVisitTime(now);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            realm.insertOrUpdate(userHomePage);

            realm.commitTransaction();
        }
    }

    private void showUserMoments(String userId) {
        FriendMomentBottomSheetFragment friendMomentBottomSheetFragment = FriendMomentBottomSheetFragment.newInstance(userId);
        friendMomentBottomSheetFragment.show(getSupportFragmentManager(), friendMomentBottomSheetFragment.getTag());
    }
}
