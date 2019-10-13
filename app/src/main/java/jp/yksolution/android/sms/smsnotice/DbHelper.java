package jp.yksolution.android.sms.smsnotice;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import jp.yksolution.android.sms.smsnotice.dao.LogDao;
import jp.yksolution.android.sms.smsnotice.dao.MessageDao;

public class DbHelper extends SQLiteOpenHelper {
    public static final String TAG = DbHelper.class.getSimpleName();
    public static final String DB_NAME = "sms_noticer";
    public static final int DB_VERSION = 1;

    private Context mContext;
    public DbHelper(final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
    }

    @Override public void onCreate(final SQLiteDatabase db) {
        Log.i(TAG, "onCreate version : " + db.getVersion());
        LogDao.createLogTable(db);
        MessageDao.createMessageTable(db);
    }

    @Override public void onUpgrade(final SQLiteDatabase db, final int oldVer, final int newVer) {
        Log.d(TAG, "onUpgrade version : " + db.getVersion());
        Log.d(TAG, "onUpgrade oldVersion : " + oldVer);
        Log.d(TAG, "onUpgrade newVersion : " + newVer);
//        if ((oldVer == 1) && (newVer == 2)) {
//            MessageDao.createMessageTable(db);
//        }
    }
}