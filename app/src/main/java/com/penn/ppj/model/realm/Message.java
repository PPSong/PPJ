package com.penn.ppj.model.realm;

import com.penn.ppj.ppEnum.MessageType;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static android.R.attr.type;
import static com.penn.ppj.util.PPHelper.ppFromString;

/**
 * Created by penn on 02/05/2017.
 */

public class Message extends RealmObject {
    @PrimaryKey
    private String id;
    private long createTime;
    private boolean read;
    private String messageType;
    private String avatar;
    private int type;
    private String body;

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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MessageType getMessageType() {
        return MessageType.valueOf(messageType);
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType.toString();
    }
}