package jp.yksolution.android.sms.smsnotice;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import jp.yksolution.android.sms.smsnotice.dao.LogDao;
import jp.yksolution.android.sms.smsnotice.dao.MessageDao;

/**
 * データベースの管理<br/>
 * テーブルの追加・削除・変更を行う
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class DbHelper extends SQLiteOpenHelper {
    public static final String TAG = DbHelper.class.getSimpleName();
    /** データベース名 : sms_noticer */
    public static final String DB_NAME = "sms_noticer";
    /** データベースのバージョン : 1 */
    public static final int DB_VERSION = 1;

    private Context mContext;
    public DbHelper(final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
    }

    /**
     * アプリケーションを新規登録したときに呼ばれ、必要なテーブル等を作成する.
     * @param db SQLiteDatabase
     */
    @Override public void onCreate(final SQLiteDatabase db) {
        Log.i(TAG, "onCreate version : " + db.getVersion());
        LogDao.createLogTable(db);
        MessageDao.createMessageTable(db);
    }

    /**
     * データベースのバージョンが変更されたときによばれ、必要なテーブルの変更を行う.
     * @param db SQLiteDatabase
     * @param oldVer 変更前のバージョン
     * @param newVer 変更後のバージョン
     */
    @Override public void onUpgrade(final SQLiteDatabase db, final int oldVer, final int newVer) {
        Log.d(TAG, "onUpgrade version : " + db.getVersion());
        Log.d(TAG, "onUpgrade oldVersion : " + oldVer);
        Log.d(TAG, "onUpgrade newVersion : " + newVer);
//        if ((oldVer == 1) && (newVer == 2)) {
//            MessageDao.createMessageTable(db);
//        }
    }
}