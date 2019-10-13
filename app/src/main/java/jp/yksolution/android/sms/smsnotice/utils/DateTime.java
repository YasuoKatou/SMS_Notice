package jp.yksolution.android.sms.smsnotice.utils;

import android.text.format.DateFormat;

import java.util.Calendar;

public class DateTime {
    public static String now() {
        return DateFormat.format("yyyy/MM/dd kk:mm:ss", Calendar.getInstance()).toString();
    }
}