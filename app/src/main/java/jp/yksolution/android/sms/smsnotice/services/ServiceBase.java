package jp.yksolution.android.sms.smsnotice.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import jp.yksolution.android.sms.smsnotice.R;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

public abstract class ServiceBase extends Service {
    private Looper mServiceLooper;
    protected ServiceHandler mServiceHandler;
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
//            Log.d("[" + Thread.currentThread().getName() + "]" + this.getClass().getSimpleName()
//                    , "handleMessage : " + ((msg == null) ? "null" : msg.toString()));
            // 拡張クラスのメッセージハンドラーを呼び出す
            ServiceBase.this.executeMessage(msg);
        }
    }

    public static final int SERVICE_WHAT_LOOP_EXIT = 9999;
    abstract void executeMessage(Message msg);

    protected final void create(String threadName) {
        String className = ServiceBase.class.getSimpleName();
        Log.d(getLogTag(className), className + ".create");
        HandlerThread thread = new HandlerThread(threadName + "Thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        this.mServiceLooper = thread.getLooper();
        this.mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    protected void bindDbService() {
        // DBサービスをバインド
        bindService(new Intent(getApplicationContext(), DbService.class), this.mDbServiceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void startCommand(String channelId, String title, int startId) {
        if (Build.VERSION.SDK_INT > 25) {
            NotificationManager notificationManager =
                    (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if(notificationManager != null) {
                NotificationChannel channel =
                        new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
                Notification notification = new Notification.Builder(getApplicationContext(), channelId)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentText("実行中")
                        .build();
                startForeground(startId, notification);
            }
        }
    }

    public class LocalBinder extends Binder {
        public Handler getHadler() {
            return mServiceHandler;
        }
        public Service getService() {
            return ServiceBase.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void destroy(String serviceName) {
        if (this.mDbService != null) {
            unbindService(this.mDbServiceConnection);
            this.mDbService = null;
        }

        if (Build.VERSION.SDK_INT > 25) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        }
    }

    private List<LogEntity> mLogList = new ArrayList<>();
    protected void appendLog(LogEntity logEntity) {
        if (this.mDbService == null) {
            //まだ、バインドされていないので
            //自クラス内に一時的に退避しておく
            //バインド後、DBサービスのキューに登録する
            synchronized (this.mLogList) {
                this.mLogList.add(logEntity);
            }
            return;
        }
        // キューの設定
        this.mDbService.requestLog(logEntity);
        // DB処理を開始
        this.mDbServiceHandler.sendMessage(Message.obtain(this.mDbServiceHandler
                , DbService.MESSAGE_WHAT_EXEC_QUERY, null));
    }

    /**
     * DB アクセスサービス（共通サービス）
     */
    protected DbService mDbService;
    protected Handler mDbServiceHandler;
    protected ServiceConnection mDbServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
            mDbService = (DbService)binder.getService();
            mDbServiceHandler = binder.getHadler();

            // バインド待ちのキューを処理する
            boolean logEmpty = mLogList.isEmpty();
            synchronized (mLogList) {
                while (!mLogList.isEmpty()) {
                    mDbService.requestLog(mLogList.remove(0));
                }
            }
            if (!logEmpty) {
                mDbServiceHandler.sendMessage(Message.obtain(mDbServiceHandler
                        , DbService.MESSAGE_WHAT_EXEC_QUERY, null));
            }

            Log.i(Thread.currentThread().getName(), "DBサービスをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDbService = null;
            Log.i(Thread.currentThread().getName(), "DBサービスから切断されました.");
        }
    };

    protected String getLogTag(String str) {
        return "[" + Thread.currentThread().getName() + "]" + str;
    }

    /**
     * 現在時刻を取得する.
     * @return 日時文字列.
     */
    protected String now() { return DateTime.now(); }
}