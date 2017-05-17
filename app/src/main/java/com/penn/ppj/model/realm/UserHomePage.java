package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.RelatedUserType;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static android.R.attr.type;

/**
 * Created by penn on 15/05/2017.
 */

public class UserHomePage extends RealmObject {
    @PrimaryKey
    private String userId;
    private String nickname;
    private String avatar;
    private boolean isFollowed;
    private long lastVisitTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isFollowed() {
        return isFollowed;
    }

    public void setFollowed(boolean followed) {
        isFollowed = followed;
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }
}
