package com.penn.ppj.util;

import static com.penn.ppj.util.PPHelper.ppFromString;

/**
 * Created by penn on 08/04/2017.
 */

public class PPWarn {
    public int code;
    public String msg;

    public PPWarn(String string) {
        code = ppFromString(string, "code").getAsInt();
        msg = ppFromString(string, "msg").getAsString();
    }
}
