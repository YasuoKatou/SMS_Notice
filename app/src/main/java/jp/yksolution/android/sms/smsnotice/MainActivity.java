package jp.yksolution.android.sms.smsnotice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jp.yksolution.android.sms.smsnotice.contacts.MyContacts;
import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;
import jp.yksolution.android.sms.smsnotice.entity.MessageEntity;
import jp.yksolution.android.sms.smsnotice.entity.ServiceCounterEntity;
import jp.yksolution.android.sms.smsnotice.services.DbService;
import jp.yksolution.android.sms.smsnotice.services.EventService;
import jp.yksolution.android.sms.smsnotice.services.MessageService;
import jp.yksolution.android.sms.smsnotice.services.ServiceBase;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * メインアクティビティ.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final String SERVICE_CLASS_NAME = EventService.class.getSimpleName();
    private Handler mDBResultHadler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.refreshButton();

        // DBの作成／更新を行う
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.close();

        // DBサービスをバインド
        bindService(new Intent(getApplicationContext(), DbService.class), this.mDbServiceConnection, Context.BIND_AUTO_CREATE);

        // 電話帳のアクセス状態を表示
        this.checkContactsButton();

        // SMS送信の許可状態を表示
        this.checkSMSSendButton();

        if (this.inServiceing(SERVICE_CLASS_NAME)) {
            // イベントサービスが実行中の場合、イベントサービスをバインド
            bindService(new Intent(getApplicationContext(), EventService.class), this.mEventServiceConnection, Context.BIND_AUTO_CREATE);
        }

        /**
         * ＤＢサービスからのクエリー終了通知を処理する.
         */
        mDBResultHadler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case EntityBase.MESSAGE_WHAT_QUERY_FINISHED:
                        finishedDbAccess(msg.obj);
                        break;
                    default:
                        Log.e(TAG, "不明なWHAT : " + msg.what);
                }
            }
        };
    }

    private void checkContactsButton() {
        int color;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // 電話帳へのアクセス権がある場合
            color = ContextCompat.getColor(this, R.color.status_allow);
        } else {
            // 電話帳へのアクセス権がない場合
            color = ContextCompat.getColor(this, R.color.status_deny);
        }
        ((Button)findViewById(R.id.btnSearchContacts)).setBackgroundColor(color);
    }

    private void checkSMSSendButton() {
        int color;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // SMS送信が可能な場合
            color = ContextCompat.getColor(this, R.color.status_allow);
        } else {
            // SMS送信が不可の場合
            color = ContextCompat.getColor(this, R.color.status_deny);
        }
        ((Button)findViewById(R.id.btnSmsSend)).setBackgroundColor(color);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (view.getId()) {
            case R.id.btnStartStopService:
                if (this.inServiceing(SERVICE_CLASS_NAME)) {
                    // サービス実行中の場合、サービスを停止する
                    this.stopService();
                } else {
                    // サービス停止の場合、サービスを起動する
                    this.startService();
                }
                this.refreshButton();
                break;
            case R.id.btnRefreshServiceStatus:
                this.refreshButton();
//                this.refreshAppList(SimpleService03.class.getSimpleName());
                break;
            case R.id.btnSearchContacts:
                this.searchContacts();
                break;
            case R.id.btnSmsSend:
                this.allowSmsSend();
                break;
            case R.id.btnTestSMS:
                this.mEventService.sendTestMesssage();
                break;
            case R.id.btnApLog:
                this.showLog();
                break;
            case R.id.btnProcLog:
                this.showProcLog();
                break;
            case R.id.btnSmsLog:
                this.showSmsLog();
                break;
        }
    }

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private void searchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // パーミッションがない場合
            // パーミッションの必要性に関する説明を表示するかどうかを確認
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                // 説明が不要の場合はパーミッションをリクエスト
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
            return;
        }
        List<String> contactsList = new ArrayList<>();
        MyContacts contacts = new MyContacts(this);
        Map<String, MyContacts.Entity> contactMap = contacts.editContacts();
        if (contactMap != null) {
            for (MyContacts.Entity entity : contactMap.values()) {
                contactsList.add(entity.toListString());
            }
            ListView lv = (ListView) findViewById(R.id.lstApp);
            lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contactsList));
        }
    }

    private static final int MY_PERMISSIONS_REQUEST_SMS_SEND = 101;
    private void allowSmsSend() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
         || (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)) {
            // パーミッションがない場合
            // パーミッションの必要性に関する説明を表示するかどうかを確認
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                // 説明が不要の場合はパーミッションをリクエスト
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS}
                , MY_PERMISSIONS_REQUEST_SMS_SEND);
            }
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                Toast.makeText(this, "電話帳へのアクセスが許可されました.", Toast.LENGTH_SHORT).show();
                // リストビューに電話帳の一覧を表示
                this.searchContacts();
            } else {
                Toast.makeText(this, "電話帳へのアクセスが許可されませんでした", Toast.LENGTH_SHORT).show();
            }
            // ボタンの背景色を変更
            this.checkContactsButton();
        } else if (requestCode == MY_PERMISSIONS_REQUEST_SMS_SEND) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                Toast.makeText(this, "SMSへの送受信が許可されました.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMSへの送受信が許可されませんでした.", Toast.LENGTH_SHORT).show();
            }
            // ボタンの背景色を変更
            this.checkSMSSendButton();
        }
    }

    private void showLog() {
        if (this.mDbService == null) {
            Toast.makeText(this, "DBアクセスサービスがバインドされていない", Toast.LENGTH_SHORT).show();
            return;
        }
        // クエリーパラメータの編集
        LogEntity entity = new LogEntity(LogEntity.PROC_ID.LOG_LIST);
        // クエリーリクエストを登録する
        entity.setCallbackHandler(this.mDBResultHadler);
        this.mDbService.requestLog(entity);
        // ＤＢサービスを開始する
        this.kickDBService();
    }

    private void showProcLog() {
        if (this.mDbService == null) {
            Toast.makeText(this, "DBアクセスサービスがバインドされていない", Toast.LENGTH_SHORT).show();
            return;
        }
        // クエリーパラメータの編集
        ServiceCounterEntity entity = new ServiceCounterEntity(ServiceCounterEntity.PROC_ID.LAST_24HOURS);
        entity.setAggregateTime(DateTime.before24Hour());
        // クエリーリクエストを登録する
        entity.setCallbackHandler(this.mDBResultHadler);
        this.mDbService.requestLog(entity);
        // ＤＢサービスを開始する
        this.kickDBService();
    }

    private void showSmsLog() {
        if (this.mDbService == null) {
            Toast.makeText(this, "DBアクセスサービスがバインドされていない", Toast.LENGTH_SHORT).show();
            return;
        }
        // クエリーパラメータの編集
        MessageEntity entity = new MessageEntity(MessageEntity.PROC_ID.LAST_24HOURS);
        entity.setBaseTime(DateTime.before24Hour());
        // クエリーリクエストを登録する
        entity.setCallbackHandler(this.mDBResultHadler);
        this.mDbService.requestLog(entity);
        // ＤＢサービスを開始する
        this.kickDBService();
    }

    private void kickDBService() {
        Object n = this.mDbServiceHandler.sendMessage(Message.obtain(this.mDbServiceHandler
                , DbService.MESSAGE_WHAT_EXEC_QUERY, null));
    }

    private void finishedDbAccess(Object o) {
        if (o instanceof LogEntity) {
            List<LogEntity> list = ((LogEntity) o).getLogList();
            if ((list == null) || (list.isEmpty())) {
                Toast.makeText(this, "ログが登録されていません.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> contactsList = new ArrayList<>();
            for (LogEntity item : list) {
                contactsList.add(item.toLogViewString());
            }
            ListView lv = (ListView) findViewById(R.id.lstApp);
            lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contactsList));
        } else if (o instanceof ServiceCounterEntity) {
            List<ServiceCounterEntity> serviceCounterlist = ((ServiceCounterEntity)o).getServiceCounterList();
            if ((serviceCounterlist == null) || (serviceCounterlist.isEmpty())) {
                Toast.makeText(this, "処理カウンタが登録されていません.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> list = new ArrayList<>();
            for (ServiceCounterEntity item : serviceCounterlist) {
//                Log.d("result : ", item.toString());
                list.add(item.toString());
            }
            ListView lv = (ListView) findViewById(R.id.lstApp);
            lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));
        } else if (o instanceof MessageEntity) {
            List<MessageEntity> messageList = ((MessageEntity)o).getMessageEntityList();
            if ((messageList == null) || (messageList.isEmpty())) {
                Toast.makeText(this, "メッセージが登録されていません.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> list = new ArrayList<>();
            String title = null;
            long dateTime = 0;
            for (MessageEntity item : messageList) {
//                Log.d("result : ", item.toString());
                if ((dateTime != item.getCreateDate()) && !item.getMessage().equals(title)) {
                    list.add(item.toTitleString());
                    title = item.getMessage();
                    dateTime = item.getCreateDate();
                }
                list.add(item.toResultString());
            }
            ListView lv = (ListView) findViewById(R.id.lstApp);
            lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list));
        } else {
            Log.e(TAG, "not supported entity : " + ((o == null) ? "null" : o.getClass().getName()));
        }
    }

    private void startService() {
        Intent intent = new Intent(getApplicationContext(), EventService.class);
        if (Build.VERSION.SDK_INT <= 25) {
            startService(intent);
        } else {
            startForegroundService(intent);
        }
//        Log.d("[" + Thread.currentThread().getName() + "]" + this.getClass().getSimpleName()
//                , "アプリは、イベントサービスを起動しました");
        Toast.makeText(this, "サービスを起動しました", Toast.LENGTH_SHORT).show();
        this.refreshButton();

        // イベントサービスをバインド
        bindService(new Intent(getApplicationContext(), EventService.class), this.mEventServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopService() {
        Log.d("[" + Thread.currentThread().getName() + "]" + MainActivity.class.getSimpleName()
                , "イベントサービスを停止します");

        // イベントサービスを停止する
        this.mEventServiceHandler.sendMessage(Message.obtain(this.mEventServiceHandler
                , ServiceBase.SERVICE_WHAT_LOOP_EXIT, null));

        // アンバインド
        unbindService(this.mEventServiceConnection);

        // サービス停止
        stopService(new Intent(getApplicationContext(), EventService.class));
        Toast.makeText(this, "サービスを停止しました", Toast.LENGTH_SHORT).show();

        this.refreshButton();
    }

    private void refreshButton() {
        Button button = findViewById(R.id.btnStartStopService);
        if (this.inServiceing(SERVICE_CLASS_NAME)) {
            // サービス実行中の場合、停止を表示
            button.setText(R.string.btnText_0112);
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.status_running));
        } else {
            // サービス停止の場合、起動を表示
            button.setText(R.string.btnText_0111);
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.status_stopped));
        }
    }

    protected boolean inServiceing(String targetClassName) {
        final Context _context = getApplicationContext();
        ActivityManager _am = (ActivityManager) _context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo item : _am.getRunningServices(Integer.MAX_VALUE)) {
            if (item.service.getShortClassName().endsWith(targetClassName)) {
                Log.d("[" + Thread.currentThread().getName() + "]" + MainActivity.class.getSimpleName()
                        , "イベントサービスが実行中");
                return true;
            }
        }
        Log.d("[" + Thread.currentThread().getName() + "]" + MainActivity.class.getSimpleName()
                , "イベントサービスは、停止しています");
        return false;
    }

    /**
     * DB アクセスサービス
     */
    private DbService mDbService;
    private Handler mDbServiceHandler;
    private ServiceConnection mDbServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
            mDbService = (DbService)binder.getService();
            mDbServiceHandler = binder.getHadler();
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "アプリは、DBサービスをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDbService = null;
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "アプリは、DBサービスから切断されました.");
        }
    };

    /**
     * イベントアクセスサービス
     */
    private EventService mEventService;
    private Handler mEventServiceHandler;
    private ServiceConnection mEventServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
            mEventService = (EventService)binder.getService();
            mEventServiceHandler = binder.getHadler();
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "アプリは、イベントサービスをバインドしました");
            // イベントサービスを開始する
            mEventServiceHandler.sendMessage(Message.obtain(mEventServiceHandler
                    , EventService.EVENT_SERVICE_WHAT_EXECUTE, null));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mEventService = null;
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "アプリは、イベントサービスから切断されました.");
        }
    };
}