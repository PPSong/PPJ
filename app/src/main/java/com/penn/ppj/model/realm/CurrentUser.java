package com.penn.ppj.model.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 09/04/2017.
 */

public class CurrentUser extends RealmObject {
    @PrimaryKey
    private String userId;
    private String token;
    private long tokenTimestamp;
    private String phone;
    private String nickname;
    private int gender;
    private long birthday;
    private String head;
    private String baiduApiUrl;
    private String baiduAkBrowser;
    private String socketHost;
    private int socketPort;
    private RealmList<Pic> pics;

    private String latestHash;
    private String earliestHash;
    private boolean initLoadingFinished;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getTokenTimestamp() {
        return tokenTimestamp;
    }

    public void setTokenTimestamp(long tokenTimestamp) {
        this.tokenTimestamp = tokenTimestamp;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public long getBirthday() {
        return birthday;
    }

    public void setBirthday(long birthday) {
        this.birthday = birthday;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getBaiduApiUrl() {
        return baiduApiUrl;
    }

    public void setBaiduApiUrl(String baiduApiUrl) {
        this.baiduApiUrl = baiduApiUrl;
    }

    public String getBaiduAkBrowser() {
        return baiduAkBrowser;
    }

    public void setBaiduAkBrowser(String baiduAkBrowser) {
        this.baiduAkBrowser = baiduAkBrowser;
    }

    public String getSocketHost() {
        return socketHost;
    }

    public void setSocketHost(String socketHost) {
        this.socketHost = socketHost;
    }

    public int getSocketPort() {
        return socketPort;
    }

    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
    }

    public RealmList<Pic> getPics() {
        return pics;
    }

    public void setPics(RealmList<Pic> pics) {
        this.pics = pics;
    }

    public String getLatestHash() {
        return latestHash;
    }

    public void setLatestHash(String latestHash) {
        this.latestHash = latestHash;
    }

    public String getEarliestHash() {
        return earliestHash;
    }

    public void setEarliestHash(String earliestHash) {
        this.earliestHash = earliestHash;
    }

    public boolean isInitLoadingFinished() {
        return initLoadingFinished;
    }

    public void setInitLoadingFinished(boolean initLoadingFinished) {
        this.initLoadingFinished = initLoadingFinished;
    }

}
