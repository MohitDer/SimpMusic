 

package com.maxrave.simpmusic.adapter;

public class GuideModel {

    private String title;
    private String message;
    private int image;

    public GuideModel(String title, String message, int image) {
        this.title = title;
        this.message = message;
        this.image = image;
    }

    public String getNumber() {
        return title;
    }

    public void setNumber(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }
}
