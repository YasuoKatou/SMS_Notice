package jp.yksolution.android.sms.smsnotice.dao;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.MessageEntity;

public abstract class DaoBase {
//    private final static String TAG = DaoBase.class.getSimpleName();

    abstract public void execute(SQLiteDatabase db, EntityBase entty);

    private long mStartTime;
    protected void setStartTime() {
        this.mStartTime = System.currentTimeMillis();
    }
    protected void beginTransaction(SQLiteDatabase db) {
        this.setStartTime();
        db.beginTransaction();
    }
    protected long getProcessTime() {
        return System.currentTimeMillis() - this.mStartTime;
    }
    protected long endTransaction(SQLiteDatabase db) {
        db.endTransaction();
        return this.getProcessTime();
    }
}