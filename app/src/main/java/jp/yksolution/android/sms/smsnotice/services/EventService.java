package jp.yksolution.android.sms.smsnotice.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Date;

import jp.yksolution.android.sms.smsnotice.R;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * イベント検知サービス.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class EventService extends ServiceBase {
    @Override
    public void onCreate() {
        String className = this.getClass().getSimpleName();
        Log.d(super.getLogTag(className), className + ".onCreate");
        super.create(className);

        // DBアクセスサービスをバインド
        super.bindDbService();

        // メッセージサービスをバインド
        boolean r = bindService(new Intent(getApplicationContext(), MessageService.class)
                , this.mMessageServiceConnection, Context.BIND_AUTO_CREATE);
        if (!r) {
            Log.e(super.getLogTag(this.getClass().getSimpleName()), "bind failre : " + MessageService.class.getSimpleName());
        }
    }

    public static final int EVENT_SERVICE_WHAT_EXECUTE = 1002;
    private boolean mExecuting = false;
    private boolean mStopping = false;
    @Override
    void executeMessage(Message msg) {
        Log.d(super.getLogTag(this.getClass().getSimpleName()), "executeMessage");
        Resources res = getResources();
        final long sleepTime = res.getInteger(R.integer.sleep_01);
        final long cyclicPostTime = res.getInteger(R.integer.cyclic_post_time_01);
        if (msg.what == ServiceBase.SERVICE_WHAT_LOOP_EXIT) {
            // メッセージサービスの終了処理を要求
            mMessageServiceHandler.sendMessage(Message.obtain(mMessageServiceHandler
                    , ServiceBase.SERVICE_WHAT_LOOP_EXIT, null));
            // サービス終了
            this.mExecuting = false;
        } else if (msg.what == EVENT_SERVICE_WHAT_EXECUTE) {
            // サービス（ループ）開始
            if (this.mExecuting) {
                Log.i(super.getLogTag(this.getClass().getSimpleName()), "executeMessage already executing");
                return;
            }
            this.mExecuting = true;
        } else {
            Log.e(super.getLogTag(this.getClass().getSimpleName()), "不明なWHAT : " + msg.what);
            return;
        }
        boolean oneTime = true;
        long count = 0L;                                // 処理回数
        long mxTime = 0L;                               // 最大処理時間
        long time1 = System.currentTimeMillis();        // 現在時刻
        long time3 = time1 + cyclicPostTime;            // 次の定周期メッセージの時刻
        time1 = time1 - (time1 % 10000) + 10000;        // 次の１０秒を設定
        while(this.mExecuting) {
            long time2 = System.currentTimeMillis();
            try {
                Thread.sleep(sleepTime);
//                Log.d(super.getLogTag(this.getClass().getSimpleName())
//                        , super.now() + " : executeMessage in loop");
                // TODO ここに処理を実装する
                if (count == 5 && oneTime) {
                    oneTime = false;
                    String text = "test message : " + DateTime.now();
                    this.mMessageServiceHandler.sendMessage(Message.obtain(
                            this.mMessageServiceHandler, MessageService.MESSAGE_WHAT_SMS_REGIST, text));
                }
                if (time3 < time2) {    // あまり厳密でないが、ソースをシンプルにする方が優先
                    // メッセージサービスに空のメッセージをPOSTする
                    this.mMessageServiceHandler.sendMessage(Message.obtain(
                            this.mMessageServiceHandler, MessageService.MESSAGE_WHAT_SMS_SEND, null));
                    time3 = time2 + cyclicPostTime;
                }
            } catch (Exception ex) {
                Log.e("ERROR", ex.toString());
            }
            long now = System.currentTimeMillis();
            long procTime = now - time2;
            if (mxTime < procTime) mxTime = procTime;
            ++count;            // 処理回数 +1
            if (time1 <= now) {
                // 10秒間の処理をまとめる
                String dateTime = super.now();
//                Log.d(super.getLogTag(this.getClass().getSimpleName())
//                        , dateTime + " : " + String.format("c:%d, m:%d", count, mxTime));
                this.measuredLog(dateTime, count, mxTime);
                time1 = now - (now % 10000) + 10000;
                mxTime = 0L;
                count = 0L;
            }
            time2 = now;
        }
        this.mStopping = false;
        Log.d(super.getLogTag(this.getClass().getSimpleName()), "exit executeMessage");
    }

    private void measuredLog(String dateTime, long count, long mxTime) {
        // ログデータの編集
        LogEntity entity = new LogEntity(LogEntity.LOG_LEVEL.INFO, String.format("c:%d, m:%d", count, mxTime));
        entity.setLogDateTime(dateTime);

        // ログのキュー登録
        super.mDbService.requestMeasured(entity);

        // ログ書込みメッセージ
        super.mDbServiceHandler.sendMessage(Message.obtain(super.mDbServiceHandler
                , DbService.MESSAGE_WHAT_EXEC_QUERY, null));
    }

    @Override
    public void onDestroy() {
        if (this.mMessageService != null) {
            unbindService(mMessageServiceConnection);
            mMessageService = null;
        }
        this.mStopping = true;
        this.mExecuting = false;
        for (int cnt = 0; this.mStopping && (cnt < 3); ++cnt) {
            try {
                Thread.sleep(1000L);
            } catch (Exception ex) {
            }
        }
        if (this.mStopping) {
            Log.i(super.getLogTag(this.getClass().getSimpleName()), "not stopped service");
        } else {
            Log.d(super.getLogTag(this.getClass().getSimpleName()), "stopped service normally");
        }

        super.destroy("Event Service");
    }


    /**
     * メッセージサービス
     */
    protected MessageService mMessageService = null;
    protected Handler mMessageServiceHandler;
    protected ServiceConnection mMessageServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
            mMessageService = (MessageService)binder.getService();
            mMessageServiceHandler = binder.getHadler();
            mMessageServiceHandler.sendMessage(Message.obtain(mMessageServiceHandler
                    , MessageService.MESSAGE_WHAT_INITIALIZE, null));

            Log.i(Thread.currentThread().getName(), "イベントサービスは、メッセージサービスをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDbService = null;
            Log.i(Thread.currentThread().getName(), "イベントサービスは、メッセージサービスから切断されました.");
        }
    };
}