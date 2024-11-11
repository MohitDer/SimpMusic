package com.maxrave.simpmusic.adapter;

public class LangModel {

    int flag;
    String lang;

    String code;

    public LangModel(int flag, String lang, String code) {
        this.flag = flag;
        this.lang = lang;
        this.code = code;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
