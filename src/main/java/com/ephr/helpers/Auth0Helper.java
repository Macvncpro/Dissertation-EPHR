package com.ephr.helpers;

public class Auth0Helper {
    private static final String DOMAIN = System.getenv("AUTH0_DOMAIN");
    private static final String CLIENT_ID = System.getenv("AUTH0_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("AUTH0_CLIENT_SECRET");

    public static String getDomain() {
        return DOMAIN;
    }

    public static String getClientId() {
        return CLIENT_ID;
    }

    public static String getClientSecret() {
        return CLIENT_SECRET;
    }

}