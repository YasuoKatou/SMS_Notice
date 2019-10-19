package jp.yksolution.android.sms.smsnotice.utils;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;

/**
 * 時刻処理ツール群.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class DateTime {
    /**
     * 現在日時を文字列で取得する.
     * @return 現在日時
     */
    public static String now() {
        return DateFormat.format("yyyy/MM/dd kk:mm:ss", Calendar.getInstance()).toString();
    }
    public static String dateTimeFormat(long msec) {
        return DateFormat.format("yyyy/MM/dd kk:mm:ss", new Date(msec)).toString();
    }
}