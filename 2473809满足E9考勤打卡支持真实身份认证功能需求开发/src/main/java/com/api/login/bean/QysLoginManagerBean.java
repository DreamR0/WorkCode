package com.api.login.bean;

public class QysLoginManagerBean {
    private String userid;
    private String username;
    private String langid;
    private boolean isSecondAuth;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLangid() {
        return langid;
    }

    public void setLangid(String langid) {
        this.langid = langid;
    }

    public boolean isSecondAuth() {
        return isSecondAuth;
    }

    public void setSecondAuth(boolean secondAuth) {
        isSecondAuth = secondAuth;
    }
}
