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
import jp.yksolution.android.sms.smsnotice.entity.ServiceCounterEntity;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * イベント検知サービス.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class EventService extends ServiceBase {
    private static final String MY_NAME = EventService.class.getSimpleName();
    @Override
    public void onCreate() {
        Log.d(super.getLogTag(MY_NAME), MY_NAME + ".onCreate");
        super.create(MY_NAME);

        // DBアクセスサービスをバインド
        super.bindDbService();

        // メッセージサービスをバインド
        boolean r = bindService(new Intent(getApplicationContext(), MessageService.class)
                , this.mMessageServiceConnection, Context.BIND_AUTO_CREATE);
        if (!r) {
            Log.e(super.getLogTag(MY_NAME), "bind failre : " + MessageService.class.getSimpleName());
        }
    }

    private static final String TITLE = "監視サービス";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.startCommand(MY_NAME, TITLE, startId);
        super.appendLog(new LogEntity(LogEntity.LOG_LEVEL.INFO, "Event Service start."));
        return START_STICKY;
    }

    public static final int EVENT_SERVICE_WHAT_EXECUTE = 1002;
    private boolean mExecuting = false;
    private boolean mStopping = false;
    @Override
    void executeMessage(Message msg) {
        Log.d(super.getLogTag(MY_NAME), "executeMessage");
        if (msg.what == ServiceBase.SERVICE_WHAT_LOOP_EXIT) {
            // メッセージサービスの終了処理を要求
            mMessageServiceHandler.sendMessage(Message.obtain(mMessageServiceHandler
                    , ServiceBase.SERVICE_WHAT_LOOP_EXIT, null));
            // サービス終了
            this.mExecuting = false;
        } else if (msg.what == EVENT_SERVICE_WHAT_EXECUTE) {
            // サービス開始
            if (this.mExecuting) {
                Log.i(super.getLogTag(MY_NAME), "executeMessage already executing");
                return;
            }
            this.mExecuting = true;
            new Thread(new EventSensorTask()).start();
        } else {
            Log.e(super.getLogTag(MY_NAME), "不明なWHAT : " + msg.what);
            return;
        }
    }

    private class EventSensorTask implements Runnable {
        @Override
        public void run() {
            Resources res = getResources();
            final long sleepTime = res.getInteger(R.integer.sleep_01);
            final long cyclicPostTime = res.getInteger(R.integer.cyclic_post_time_01);
            resetAggregateDto(System.currentTimeMillis(), true);
            while (mExecuting) {
//                Log.d("[" + Thread.currentThread().getName() + "]EventSensorTask","run");
                try {
                    Thread.sleep(sleepTime);
                    // TODO ここに処理を実装する
                } catch (Exception ex) {
                    Log.e("[" + Thread.currentThread().getName() + "]EventSensorTask", ex.toString());
                }
                mAggregateDto.time1 = aggregateCount();
            }
            EventService.this.appendLog(new LogEntity(LogEntity.LOG_LEVEL.INFO, "Event Service end."));
        }
/*
        boolean oneTime = true;
//        long time3 = time1 + cyclicPostTime;            // 次の定周期メッセージの時刻
//        time1 = time1 - (time1 % 10000) + 10000;        // 次の１０秒を設定
        while(this.mExecuting) {
            long time2 = System.currentTimeMillis();
            try {
                Thread.sleep(sleepTime);
//                Log.d(super.getLogTag(MY_NAME)
//                        , super.now() + " : executeMessage in loop");
//                if (count == 5 && oneTime) {
//                    oneTime = false;
//                    String text = "test message : " + DateTime.now();
//                    this.mMessageServiceHandler.sendMessage(Message.obtain(
//                            this.mMessageServiceHandler, MessageService.MESSAGE_WHAT_SMS_REGIST, text));
//                }
//                if (time3 < time2) {    // あまり厳密でないが、ソースをシンプルにする方が優先
//                    // メッセージサービスに空のメッセージをPOSTする
//                    this.mMessageServiceHandler.sendMessage(Message.obtain(
//                            this.mMessageServiceHandler, MessageService.MESSAGE_WHAT_SMS_SEND, null));
//                    time3 = time2 + cyclicPostTime;
//                }
            } catch (Exception ex) {
                Log.e(super.getLogTag(MY_NAME), ex.toString());
            }
//            long now = System.currentTimeMillis();
//            long procTime = now - time2;
//            if (mxTime < procTime) mxTime = procTime;
//            ++count;            // 処理回数 +1
//            if (time1 <= now) {
//                // 10秒間の処理をまとめる
//                String dateTime = super.now();
////                Log.d(super.getLogTag(MY_NAME)
////                        , dateTime + " : " + String.format("c:%d, m:%d", count, mxTime));
//                this.measuredLog(dateTime, count, mxTime);
//                time1 = now - (now % 10000) + 10000;
//                mxTime = 0L;
//                count = 0L;
//            }
//            time2 = now;
        }
        this.mStopping = false;
        Log.d(super.getLogTag(this.getClass().getSimpleName()), "exit executeMessage");
 */
    }

    private static class AggregateDto {
        private long time1;         // 処理開始前の日時（処理時間計算用）
        private long next10Minute;  // 次の10分後の日時
        private long count;         // 処理回数
        private long mxTime;        // 最大処理時間
        private int timeIndex;
        private ServiceCounterEntity entity;
    }
    private AggregateDto mAggregateDto = new AggregateDto();
    private void resetAggregateDto(long now, boolean newEntity) {
        this.mAggregateDto.count = 0L;      // 処理回数
        this.mAggregateDto.mxTime = 0L;     // 最大処理時間
        this.mAggregateDto.time1 = now;     // 処理開始日時
        if (newEntity) {
            this.mAggregateDto.entity = new ServiceCounterEntity();
            this.mAggregateDto.entity.setAggregateTime(DateTime.roudDownMinute(now));   // 直近の00分00秒
            this.mAggregateDto.entity.setDaoCallback(null);     // 更新結果は、不要
        }
        this.mAggregateDto.timeIndex = DateTime.get10MibuteIndex(now);
        this.mAggregateDto.next10Minute = DateTime.next10Mibute(now);               // 次の10分の開始日時
    }

    private long aggregateCount() {
        final long now = System.currentTimeMillis();    // 処理終了後の日時
        this.mAggregateDto.count += 1;      // 処理回数
        final long procTime = now - this.mAggregateDto.time1;
        if (this.mAggregateDto.mxTime < procTime) {
            // 最大処理時間の更新
            this.mAggregateDto.mxTime = procTime;
        }
//        Log.d(super.getLogTag(MY_NAME), "now : "
//                + DateTime.dateTimeFormat(now) + ", next : " + DateTime.dateTimeFormat(this.mAggregateDto.next10Minute));

        if (now < this.mAggregateDto.next10Minute) {
            // 次の10分が先にある場合、次のループを処理する
            return now;
        }

        // 10分の区切り
        // 10分間のカウントをエンティティに保存
        this.mAggregateDto.entity.setAggregateData(
                this.mAggregateDto.timeIndex
              , (int)this.mAggregateDto.count
              , (int)this.mAggregateDto.mxTime);
        Log.d(DateTime.dateTimeFormat(now)+ " : " + super.getLogTag(MY_NAME)
                , "change 10 numite : " + this.mAggregateDto.entity.toString());
        // 10分間のカウントの結果をＤＢに保存
        this.mAggregateDto.entity.setProcId(ServiceCounterEntity.PROC_ID.ADD_COUNT);
        super.mDbService.requestMeasured(this.mAggregateDto.entity);    // ログのキュー登録
        super.mDbServiceHandler.sendMessage(Message.obtain(super.mDbServiceHandler
                , DbService.MESSAGE_WHAT_EXEC_QUERY, null));        // 書込みメッセージ

        // 集計Dtoを初期化する
        boolean newEntity = (this.mAggregateDto.entity.getAggregateTime() != DateTime.roudDownMinute(now));
        this.resetAggregateDto(now, newEntity);

        return now;
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
            Log.i(super.getLogTag(MY_NAME), "not stopped service");
        } else {
            Log.d(super.getLogTag(MY_NAME), "stopped service normally");
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