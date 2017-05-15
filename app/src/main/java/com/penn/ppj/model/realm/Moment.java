package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.MomentStatus;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class Moment extends RealmObject {
    @PrimaryKey
    private String id; //createTime_creatorUserId for local new moment
    private long createTime;
    private String status;
    private Pic pic;
    private String avatar;

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

    public MomentStatus getStatus() {
        return MomentStatus.valueOf(status);
    }

    public void setStatus(MomentStatus status) {
        this.status = status.toString();
    }

    public Pic getPic() {
        return pic;
    }

    public void setPic(Pic pic) {
        this.pic = pic;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
