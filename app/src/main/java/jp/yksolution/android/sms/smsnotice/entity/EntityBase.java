package jp.yksolution.android.sms.smsnotice.entity;

import android.util.Log;

import jp.yksolution.android.sms.smsnotice.dao.DaoCallback;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

public class EntityBase {
    /** このエンティティを処理するDaoクラス名. */
    protected String mDaoClassName;
    public String getDaoClassName() { return this.mDaoClassName; }
    protected void setDaoClassName(String name) { this.mDaoClassName = name; }

    /** 処理(insert, select #1, select #2 ...)ID. */
    protected int mProcId;
    public final int getProcId() { return this.mProcId; }
    public final void setProcId(int procId) { this.mProcId = procId; }

    /** DBアクセス終了後に呼び出すメソッド. */
    private DaoCallback mDaoCallback = null;
    public final DaoCallback getDaoCallback() { return this.mDaoCallback; }
    public final void setDaoCallback(DaoCallback cb) { this.mDaoCallback = cb; }

    public final void finished() throws Exception {
        if (this.mDaoCallback != null) {
            this.mDaoCallback.finishedDbAccess(this);
//        } else {
//            Log.d(Thread.currentThread().getName(), "no Dao Callback");
        }
    }

    /** 処理結果. */
    public enum RESULT {NORMAL_COMPLETED, ABNORMAL_COMPLETED}
    private RESULT mResult;
    public final void setResult(RESULT result) { this.mResult = result; }
    public final RESULT getResult() { return this.mResult; }

    /**
     * 現在時刻を取得する.
     * @return 日時文字列.
     */
    protected String now() { return DateTime.now(); }
}