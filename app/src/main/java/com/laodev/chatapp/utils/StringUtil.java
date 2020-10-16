package com.laodev.chatapp.utils;

import android.text.Html;
import android.text.Spanned;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StringUtil {

    public static Spanned getSearchText(String target, String search) {
        if (!target.toLowerCase().contains(search.toLowerCase())) {
            return Html.fromHtml(target);
        } else {
            String[] spilts = target.toLowerCase().split(search.toLowerCase());
            StringBuilder result = new StringBuilder(target.substring(0, spilts[0].length()));
            int beginIndex = result.length();
            for (int i = 1; i < spilts.length; i++) {
                String searchKey = target.substring(beginIndex, beginIndex + search.length());
                searchKey = "<span style='background-color: yellow;'>" + searchKey + "</span>";
                result.append(searchKey);
                beginIndex = beginIndex + search.length();
                result.append(target.substring(beginIndex, beginIndex + spilts[i].length()));
                beginIndex = beginIndex + spilts[i].length();
            }
            if (beginIndex < target.length()) {
                String searchKey = target.substring(beginIndex, beginIndex + search.length());
                searchKey = "<span style='background-color: yellow;'>" + searchKey + "</span>";
                result.append(searchKey);
            }
            return Html.fromHtml(result.toString());
        }
    }

    public static String getUploadImageName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        return "IMG_" + currentDateandTime + ".jpg";
    }

    public static String getUploadVideoName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        return "VIO_" + currentDateandTime + ".mp4";
    }

}
