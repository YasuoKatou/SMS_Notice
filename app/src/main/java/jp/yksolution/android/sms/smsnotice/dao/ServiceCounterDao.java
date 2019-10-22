package jp.yksolution.android.sms.smsnotice.dao;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;

/**
 * サービス処理状況テーブルDao.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class ServiceCounterDao extends DaoBase {

    /** サービス処理状況テーブルDaoのインスタンス. */
    private static final ServiceCounterDao mInstance = new ServiceCounterDao();
    /** サービス処理状況登録(insert)クエリー文字列. */
    private final String mSQL_Insert;

    private ServiceCounterDao() { mSQL_Insert = this.editInsert(); }

    /**
     * サービス処理状況登録(insert)クエリー文字列を編集する.
     * @return サービス処理状況登録(insert)クエリー文字列
     */
    private static String editInsert() {
        StringBuffer insert = new StringBuffer("insert into t_log");
        insert.append(" (count_date")
              .append(",proc_cnt_00,proc_max_00")
              .append(",proc_cnt_10,proc_max_10")
              .append(",proc_cnt_20,proc_max_20")
              .append(",proc_cnt_30,proc_max_30")
              .append(",proc_cnt_40,proc_max_40")
              .append(",proc_cnt_50,proc_max_50")
              .append(") values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
        return insert.toString();
    }

    /**
     * サービス処理状況に対するクエリーを実行する.
     * @param db SQLiteDatabase
     * @param e EntityBase
     */
    @Override public void execute(SQLiteDatabase db, EntityBase e) {

    }

    /**
     * ログテーブルを作成する.
     * @param db SQLiteDatabase
     */
    public static final void createServiceCounterTable(SQLiteDatabase db) {
        StringBuilder ddl;
        String tag = LogDao.class.getSimpleName();

        // テーブルを作成
        ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS t_svc_cnt (");
        ddl.append("count_date integer primary key")  // yyyy/mm/dd hh:mm:00のシリアル値
                .append(",proc_cnt_00 integer")    // 00分～10分の処理回数
                .append(",proc_max_00 integer")    // 00分～10分の最大処理時間
                .append(",proc_cnt_10 integer")
                .append(",proc_max_10 integer")
                .append(",proc_cnt_20 integer")
                .append(",proc_max_20 integer")
                .append(",proc_cnt_30 integer")
                .append(",proc_max_30 integer")
                .append(",proc_cnt_40 integer")
                .append(",proc_max_40 integer")
                .append(",proc_cnt_50 integer")
                .append(",proc_max_50 integer")
                .append(");");
        Log.d(tag, "CREATE TABLE : " + ddl.toString());
        db.execSQL(ddl.toString());
        Log.d(tag, "CREATE TABLE passed");
    }
}