package com.penn.ppj.util;

/**
 * Created by penn on 14/05/2017.
 */

public class CurUser {
    private static CurUser instance;

    private CurUser() {
    }

    public static CurUser getInstance() {
        if (instance == null) {
            instance = new CurUser();
        }
        return instance;
    }

    public static boolean logined() {
        return !(instance == null);
    }

    public static void clear() {
        instance = null;
    }
}
