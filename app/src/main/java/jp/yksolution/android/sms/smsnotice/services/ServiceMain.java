package jp.yksolution.android.sms.smsnotice.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import jp.yksolution.android.sms.smsnotice.entity.LogEntity;

public class ServiceMain extends ServiceBase {
    @Override
    public void onCreate() {
        String className = this.getClass().getSimpleName();
        Log.d(super.getLogTag(className), className + ".onCreate");
        super.create(className);
        // DBアクセスサービスをバインド
        super.bindDbService();
        // イベントサービスをバインド
        boolean r = bindService(new Intent(getApplicationContext(), EventService.class)
                , this.mEventServiceConnection, Context.BIND_AUTO_CREATE);
        if (!r) {
            Log.e(super.getLogTag(this.getClass().getSimpleName()), "bind failre : " + EventService.class.getSimpleName());
        }
    }

    private static final String CHANNEL_ID = ServiceMain.class.getSimpleName();
    private static final String TITLE = "監視サービス";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.startCommand(CHANNEL_ID, TITLE, startId);
        super.appendLog(new LogEntity(LogEntity.LOG_LEVEL.INFO, "Service Main start."));
        return START_STICKY;
    }

    public static final int SERVIE_MAIN_WHAT_EXECUTE = 1001;
    private boolean mExecuting = false;
    private boolean mStopping = false;
    @Override
    public void executeMessage(Message msg) {
        Log.d(super.getLogTag(this.getClass().getSimpleName())
                , "executeMessage : " + ((msg == null) ? "null" : msg.toString()));

        if (msg.what == ServiceBase.SERVICE_WHAT_LOOP_EXIT) {
            // イベントサービスを終了させる
            this.mEventServiceHandler.sendMessage(Message.obtain(this.mEventServiceHandler
                    , ServiceBase.SERVICE_WHAT_LOOP_EXIT, null));
            // サービス終了
            this.mExecuting = false;
        } else if (msg.what == SERVIE_MAIN_WHAT_EXECUTE) {
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
        while(this.mExecuting) {
            try {
                Thread.sleep(1000L);
//                Log.d(super.getLogTag(this.getClass().getSimpleName())
//                        , super.now() + " : executeMessage in loop");
            } catch (Exception ex) {
            }
        }
        this.mStopping = false;
        Log.d(super.getLogTag(this.getClass().getSimpleName()), "exit executeMessage");
    }

    @Override
    public void onDestroy() {
        // イベントサービスをアンバインド
        if (this.mEventService != null) {
            unbindService(this.mEventServiceConnection);
            this.mEventService = null;
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

        super.appendLog(new LogEntity(LogEntity.LOG_LEVEL.INFO, "Service Main end."));
        super.destroy("Service Main");
    }

    /**
     * イベントサービス
     */
    protected EventService mEventService;
    protected Handler mEventServiceHandler;
    protected ServiceConnection mEventServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
            mEventService = (EventService)binder.getService();
            mEventServiceHandler = binder.getHadler();
            mEventServiceHandler.sendMessage(Message.obtain(mEventServiceHandler
                    , EventService.EVENT_SERVICE_WHAT_EXECUTE, null));

            Log.i(Thread.currentThread().getName(), "サービスメインは、イベントサービスをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDbService = null;
            Log.i(Thread.currentThread().getName(), "サービスメインは、イベントサービスから切断されました.");
        }
    };
}