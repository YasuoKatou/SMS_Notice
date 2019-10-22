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

    /**
     * 日時の分以下を切り捨てる.
     * @param tm 日時
     * @return 切り捨て後の日時
     */
    public static long roudDownMinute(long tm) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tm - (tm % 1000));      // ミリ秒の端数を切り捨てておく
        cal.set(Calendar.SECOND, 0);                // 秒を切り捨て
        cal.set(Calendar.MINUTE, 0);                // 分を切り捨て
        return cal.getTimeInMillis();
    }

    /**
     * 日時の分以下を１０分単位に切り捨てる.
     * @param tm 日時
     * @return 切り捨て後の日時
    public static long roundDownMibute10(long tm) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tm - (tm % 1000));      // ミリ秒の端数を切り捨てておく
        cal.set(Calendar.SECOND, 0);                // 秒を切り捨て
        int minute = cal.get(Calendar.MINUTE) / 10 * 10;
        cal.set(Calendar.MINUTE, minute);           // 10分を切り捨て
        return cal.getTimeInMillis();
    }
     */

    /**
     * 日時から１０分単位のインデックスを取得する.
     * @param tm 日時
     * @return 10分インデックス
     */
    public static int get10MibuteIndex(long tm) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tm);
        return cal.get(Calendar.MINUTE) / 10;
    }

    /**
     * 次の１０分を計算する
     * @param tm 日時
     * @return 次の１０分日時
     */
    public static long next10Mibute(long tm) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(tm - (tm % 1000));      // ミリ秒の端数を切り捨てておく
        cal.set(Calendar.SECOND, 0);                // 秒を切り捨て
        int minute = cal.get(Calendar.MINUTE) / 10 * 10;
        cal.set(Calendar.MINUTE, minute);           // 10分を切り捨て
        cal.add(Calendar.MINUTE, 10);       // 次の10分
        return cal.getTimeInMillis();
    }
}