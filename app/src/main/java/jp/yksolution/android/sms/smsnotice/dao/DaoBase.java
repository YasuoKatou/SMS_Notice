package jp.yksolution.android.sms.smsnotice.dao;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.MessageEntity;

/**
 * Dao基底クラス.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public abstract class DaoBase {
//    private final static String TAG = DaoBase.class.getSimpleName();

    abstract public void execute(SQLiteDatabase db, EntityBase entty);

    /** 処理開始日時. */
    protected long mStartTime;
    /**
     * 処理開始日時を取得する.
     */
    protected void setStartTime() {
        this.mStartTime = System.currentTimeMillis();
    }
    /**
     * 処理開始日時を取得し、トランザクションを開始する.
     * @param db SQLiteDatabase
     */
    protected void beginTransaction(SQLiteDatabase db) {
        this.setStartTime();
        db.beginTransaction();
    }

    /**
     * 処理時間を取得する.
     * @return 処理時間（単位：msec）
     */
    protected long getProcessTime() {
        return System.currentTimeMillis() - this.mStartTime;
    }

    /**
     * トランザクションを終了し、処理時間（単位：msec）を取得する.
     * @param db SQLiteDatabase
     * @return 処理時間（単位：msec）
     */
    protected long endTransaction(SQLiteDatabase db) {
        db.endTransaction();
        return this.getProcessTime();
    }
}