package jp.yksolution.android.sms.smsnotice.entity;

import android.os.Handler;
import android.os.Message;

import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * エンティティの基底クラス.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class EntityBase {
    public static final int MESSAGE_WHAT_QUERY_FINISHED = 8001;

    /** このエンティティを処理するDaoクラス名. */
    protected String mDaoClassName;
    public String getDaoClassName() { return this.mDaoClassName; }
    protected void setDaoClassName(String name) { this.mDaoClassName = name; }

    /** 処理(insert, select #1, select #2 ...)ID. */
    protected int mProcId;
    public final int getProcId() { return this.mProcId; }
    public final void setProcId(int procId) { this.mProcId = procId; }

    /** DBアクセス終了後に送信するメッセージ. */
    private Handler mMessageHandler = null;
    public final Handler getCallbackHandler() { return this.mMessageHandler; }
    public final void setCallbackHandler(Handler mh) { this.mMessageHandler = mh; }

    public final void finished() throws Exception {
        if (this.mMessageHandler != null) {
            this.mMessageHandler.sendMessage(Message.obtain(this.mMessageHandler
                    , MESSAGE_WHAT_QUERY_FINISHED, this));
//        } else {
//            Log.d(Thread.currentThread().getName(), "no Dao Callback");
        }
    }

    /** 処理結果. */
    public enum RESULT {NORMAL_COMPLETED, ABNORMAL_COMPLETED}
    private RESULT mResult;
    public final void setResult(RESULT result) { this.mResult = result; }
    public final RESULT getResult() { return this.mResult; }

    protected void deepCopy(EntityBase entity) {
        entity.mProcId = this.mProcId;
        entity.mDaoClassName = this.mDaoClassName;
        entity.mMessageHandler = this.mMessageHandler;
        entity.mResult = this.mResult;
    }

    /**
     * 現在時刻を取得する.
     * @return 日時文字列.
     */
    protected String now() { return DateTime.now(); }
}