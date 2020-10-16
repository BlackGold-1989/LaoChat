package com.laodev.chatapp.vmeet.bean;

import android.graphics.drawable.Drawable;

public class Intro {

    Drawable img ;
    String title;

    public Intro(Drawable img, String title) {
        this.img = img;
        this.title = title;
    }

    public Drawable getImg() {
        return img;
    }

    public void setImg(Drawable img) {
        this.img = img;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
