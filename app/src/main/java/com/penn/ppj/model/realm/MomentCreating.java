package com.penn.ppj.model.realm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.penn.ppj.model.Geo;
import com.penn.ppj.ppEnum.MomentStatus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class MomentCreating extends RealmObject {
    @PrimaryKey
    private String id;
    private long createTime;
    private byte[] pic;
    private String content;
    private String geo; //lon,lat
    private String address;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public byte[] getPic() {
        return pic;
    }

    public void setPic(String picFilePath) {
        if (picFilePath == null) {
            pic = null;
            return;
        }

        File file = new File(picFilePath);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.v("pplog", "MomentCreating.setPic error:" + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.v("pplog", "MomentCreating.setPic error:" + e.toString());
            e.printStackTrace();
        }

        this.pic = bytes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String validatePublish() {
        if (pic == null || pic.length == 0) {
            return "图片不能为空";
        }

        if (TextUtils.isEmpty(content)) {
            return "内容不能为空";
        }

        if (content.length() > 70) {
            return "内容不能超过70个字";
        }

        if (TextUtils.isEmpty(geo)) {
            return "没有获取地理位置";
        }

        if (TextUtils.isEmpty(address)) {
            return "没有获取地址";
        }

        return "OK";
    }

    public void clear() {
        setCreateTime(0);
        setPic(null);
        setContent(null);
        setGeo(null);
        setAddress(null);
    }
}
