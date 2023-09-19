package com.tequre.wallet.utils;

import java.nio.charset.Charset;
import java.util.Base64;

public class WebUtils {

    public static String authorization(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder()
                .encode(auth.getBytes(Charset.forName("US-ASCII")));
        return "Basic " + new String(encodedAuth);
    }
}
