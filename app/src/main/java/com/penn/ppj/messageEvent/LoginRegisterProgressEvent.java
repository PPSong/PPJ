package com.penn.ppj.messageEvent;

/**
 * Created by penn on 13/05/2017.
 */

public class LoginRegisterProgressEvent {
    public String type;
    public boolean show;

    public LoginRegisterProgressEvent(String type, boolean show) {
        this.type = type;
        this.show = show;
    }
}
