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

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TImage;
import com.jph.takephoto.model.TResult;
import com.penn.ppj.databinding.ActivityCreateMomentBinding;
import com.penn.ppj.model.Geo;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.util.PPHelper;

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
import static com.penn.ppj.R.id.imageView;

public class CreateMomentActivity extends TakePhotoActivity {

    private ActivityCreateMomentBinding binding;
    private Realm realm;
    private MomentCreating momentCreating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_moment);

        realm = Realm.getDefaultInstance();

        momentCreating = realm.where(MomentCreating.class).findFirst();
        binding.setData(momentCreating);
        binding.contentTextInputEditText.setText(momentCreating.getContent());

        momentCreating.addChangeListener(new RealmChangeListener<MomentCreating>() {
            @Override
            public void onChange(MomentCreating element) {
                binding.setData(element);
                validatePublish();
            }
        });

        setup();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void setup() {
        //拍照按钮监控
        Observable<Object> takePhotoButtonObservable = RxView.clicks(binding.takePhotoImageButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        takePhotoButtonObservable
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                takePhoto();
                            }
                        }
                );

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

    private void getCurLocation() {
        Geo geo = PPHelper.getLatestGeo();
        realm.beginTransaction();
        momentCreating.setGeo("" + geo.lon + "," + geo.lat);
        momentCreating.setAddress("address:" + geo.lon + "," + geo.lat);
        realm.commitTransaction();
        //pptodo use baidu api to get real address
    }

    private void takePhoto() {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        Uri imageUri = Uri.fromFile(file);

        CompressConfig config = new CompressConfig.Builder()
                .setMaxSize(1024 * 1024)
                .setMaxPixel(1024)
                .create();

        TakePhoto takePhoto = getTakePhoto();

        takePhoto.onEnableCompress(config, true);

        takePhoto.onPickFromCapture(imageUri);
    }

    @Override
    public void takeFail(TResult result, String msg) {
        PPHelper.error(msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);

        realm.beginTransaction();
        momentCreating.setPic(result.getImages().get(0).getCompressPath());
        realm.commitTransaction();
    }

    private void validatePublish() {
        String result = momentCreating.validatePublish();
        if (result != "OK") {
            binding.publishButton.setEnabled(false);
            //PPHelper.error(result);
            Log.v("pplog", result);
        } else {
            binding.publishButton.setEnabled(true);
        }
    }

    private void publishMoment() {
        realm.beginTransaction();

        //把momentCreating放入Moment

        momentCreating.clear();
        realm.commitTransaction();
        binding.contentTextInputEditText.setText("");
        finish();
    }
}
