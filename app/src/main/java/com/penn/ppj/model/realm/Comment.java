package com.penn.ppj.model.realm;

import android.util.Log;
import android.view.View;

import com.penn.ppj.ppEnum.CommentStatus;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.util.PPHelper;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 15/05/2017.
 */

public class Comment extends RealmObject {
    @PrimaryKey
    private String key;//createTime_creatorUserId
    private String id;
    private String userId;
    private String momentId;
    private long createTime;
    private String nickname;
    private String avatar;
    private String content;
    private String status;
    private long lastVisitTime;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getMomentId() {
        return momentId;
    }

    public void setMomentId(String momentId) {
        this.momentId = momentId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CommentStatus getStatus() {
        return CommentStatus.valueOf(status);
    }

    public void setStatus(CommentStatus status) {
        this.status = status.toString();
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    public int deletable() {
        return (getUserId().equals(PPHelper.currentUserId) && getStatus() == CommentStatus.NET) ? View.VISIBLE : View.INVISIBLE;
    }
}
