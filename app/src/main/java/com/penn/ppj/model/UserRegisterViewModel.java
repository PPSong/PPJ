package com.penn.ppj.model;

/**
 * Created by penn on 24/05/2017.
 */

public class UserRegisterViewModel {
    public String username;
    public String password;
    public String verifyCode;

    public boolean validate() {
        String mobileRegex = "^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8}$";
        if (!(username.matches(mobileRegex))) {
            return false;
        }

        return true;
    }
}
