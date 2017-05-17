package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.PicStatus;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 09/04/2017.
 */

public class Pic extends RealmObject {
    @PrimaryKey
    private String key; //netFileName or createTime_userId_index
    private String status;
    private byte[] localData;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public PicStatus getStatus() {
        return PicStatus.valueOf(status);
    }

    public void setStatus(PicStatus status) {
        this.status = status.toString();
    }

    public byte[] getLocalData() {
        return localData;
    }

    public void setLocalData(byte[] localData) {
        this.localData = localData;
    }
}
