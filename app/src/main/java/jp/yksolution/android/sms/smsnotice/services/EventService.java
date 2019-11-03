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
    @Override
    void executeMessage(Message msg) {
        Log.d(super.getLogTag(MY_NAME), "executeMessage");
        if (msg.what == ServiceBase.SERVICE_WHAT_LOOP_EXIT) {
            // サービス終了
            this.stopSensorThread();
            // メッセージサービスの終了処理を要求
            mMessageServiceHandler.sendMessage(Message.obtain(mMessageServiceHandler
                    , ServiceBase.SERVICE_WHAT_LOOP_EXIT, null));
            super.appendLog(new LogEntity(LogEntity.LOG_LEVEL.INFO, "Event Service stop."));
        } else if (msg.what == EVENT_SERVICE_WHAT_EXECUTE) {
            // サービス開始
            this.executeSensorTask();
        } else {
            Log.e(super.getLogTag(MY_NAME), "不明なWHAT : " + msg.what);
            return;
        }
    }

    private Handler mEventSensorHandler = null;
    private Runnable mEventSensorThread = null;
    private class EventSensorThread implements Runnable {
        private final long sleepTime;
        private final long cyclicPostTime;
        public EventSensorThread() {
            Resources res = getResources();
            this.sleepTime = res.getInteger(R.integer.sleep_01);
            this.cyclicPostTime = res.getInteger(R.integer.cyclic_post_time_01);
            // 次の定周期メッセージの時刻を設定
            long now = System.currentTimeMillis();
            this.nextCyclicPostTime = now + this.cyclicPostTime;

            resetAggregateDto(now, true);
        }

        /** 次の定周期メッセージの時刻. */
        private long nextCyclicPostTime;
        @Override
        public void run() {
//            Log.d("[" + Thread.currentThread().getName() + "]EventSensorTask","run");
            // ---------------------------
            // TODO ここに処理を実装する

            mAggregateDto.time1 = aggregateCount();
            // ---------------------------
            // 定周期メッセージの送信
            if (this.nextCyclicPostTime < mAggregateDto.time1) {
                // 次の定周期メッセージの時刻を設定
                this.nextCyclicPostTime = mAggregateDto.time1 + this.cyclicPostTime;
                // 定周期メッセージの送信
                mMessageServiceHandler.sendMessage(Message.obtain(mMessageServiceHandler
                        , MessageService.MESSAGE_WHAT_SMS_SEND, null));
            }
            // wait
            mEventSensorHandler.postDelayed(mEventSensorThread, this.sleepTime);
        }
    }

    private void stopSensorThread() {
        if ((this.mEventSensorHandler != null) && (this.mEventSensorThread != null)) {
            this.mEventSensorHandler.removeCallbacks(this.mEventSensorThread);
            this.mEventSensorHandler = null;
            this.mEventSensorThread  = null;
        }
    }

    private void executeSensorTask() {
        if (this.mEventSensorThread != null) {
            Log.i(super.getLogTag(MY_NAME), "already executing sensor thread");
            return;
        }
        this.mEventSensorThread = new EventSensorThread();
        this.mEventSensorHandler = new Handler();
        this.mEventSensorHandler.post(mEventSensorThread);
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
            this.mAggregateDto.entity = new ServiceCounterEntity(ServiceCounterEntity.PROC_ID.ADD_COUNT);
            this.mAggregateDto.entity.setAggregateTime(DateTime.roudDownMinute(now));   // 直近の00分00秒
            this.mAggregateDto.entity.setCallbackHandler(null);     // 更新結果は、不要
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
        this.stopSensorThread();

        super.destroy("Event Service");
    }

    public void sendTestMesssage() {
        String text = "test message : " + DateTime.now();
        this.mMessageServiceHandler.sendMessage(Message.obtain(
                this.mMessageServiceHandler, MessageService.MESSAGE_WHAT_SMS_REGIST, text));
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