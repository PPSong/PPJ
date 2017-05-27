package com.penn.ppj;

import android.database.DatabaseUtils;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TImage;
import com.jph.takephoto.model.TResult;
import com.penn.ppj.databinding.ActivityCreateMomentBinding;
import com.penn.ppj.messageEvent.MomentPublishEvent;
import com.penn.ppj.model.Geo;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPHelper;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;

import static com.jakewharton.rxbinding2.widget.RxTextView.textChanges;

public class CreateMomentActivity extends AppCompatActivity {

    private ActivityCreateMomentBinding binding;
    private Realm realm;
    private MomentCreating momentCreating;
    private RealmChangeListener<MomentCreating> changeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_moment);

        changeListener = new RealmChangeListener<MomentCreating>() {
            @Override
            public void onChange(MomentCreating element) {
                //使用isValid()来避免对已删除的记录进行操作
                if (element.isValid()) {
                    binding.setData(element);
                    validatePublish();
                }
            }
        };

        realm = Realm.getDefaultInstance();

        setupBindingData();

        setup();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                View view = getCurrentFocus();
                PPHelper.hideKeyboard(ev, view, this);//调用方法判断是否需要隐藏键盘
                break;

            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void setup() {
        //发布按钮监控
        Observable<Object> publishButtonObservable = RxView.clicks(binding.publishButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        publishButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                publishMoment();
                            }
                        }
                );

        Observable<CharSequence> contentObservable = RxTextView.textChanges(binding.contentTextInputEditText)
                .debounce(200, TimeUnit.MILLISECONDS);

        contentObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<CharSequence>() {
                            @Override
                            public void accept(@NonNull CharSequence charSequence) throws Exception {
                                realm.beginTransaction();
                                momentCreating.setContent("" + charSequence);
                                realm.commitTransaction();
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                            }
                        }
                );

        getCurLocation();
    }

    private void setupBindingData() {
        momentCreating = realm.where(MomentCreating.class).equalTo("status", MomentStatus.PREPARE.toString()).findFirst();
        Log.v("pplog", "setupBindingData start");
        if (momentCreating == null) {
            realm.beginTransaction();

            MomentCreating newOne = new MomentCreating();
            newOne.setId();
            newOne.setStatus(MomentStatus.PREPARE);

            realm.copyToRealm(newOne);

            realm.commitTransaction();

            momentCreating = realm.where(MomentCreating.class).equalTo("status", MomentStatus.PREPARE.toString()).findFirst();
        }

        momentCreating.addChangeListener(changeListener);
        binding.setData(momentCreating);
        binding.contentTextInputEditText.setText(momentCreating.getContent());
    }

    private void getCurLocation() {
        realm.beginTransaction();
        momentCreating.setGeo(PPHelper.getLatestGeoString());
        momentCreating.setAddress(PPHelper.getLatestAddress());
        realm.commitTransaction();
        //pptodo use baidu api to get real address
    }

    private void validatePublish() {
        String result = momentCreating.validatePublish();
        if (!result.equalsIgnoreCase("OK")) {
            binding.publishButton.setEnabled(false);
            //PPHelper.error(result);
            Log.v("pplog", result);
        } else {
            binding.publishButton.setEnabled(true);
        }
    }

    private void publishMoment() {
        long now = System.currentTimeMillis();

        //把momentCreating放入Moment
        Moment moment = new Moment();
        moment.setKey(momentCreating.getId());
        moment.setUserId(PPHelper.currentUserId);
        moment.setCreateTime(momentCreating.getCreateTime());
        moment.setStatus(MomentStatus.LOCAL);
        Pic pic = new Pic();
        pic.setKey(momentCreating.getId() + "_" + 0);
        pic.setStatus(PicStatus.LOCAL);
        pic.setLocalData(momentCreating.getPic());
        moment.setPic(pic);

        realm.beginTransaction();

        realm.copyToRealm(moment);

        realm.commitTransaction();

        binding.contentTextInputEditText.setText("");

        Log.v("pplog", "time:" + (System.currentTimeMillis() - now));

        setResult(RESULT_OK);
        finish();
    }
}
