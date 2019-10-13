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
import java.util.List;
import java.util.Map;

import jp.yksolution.android.sms.smsnotice.contacts.MyContacts;
import jp.yksolution.android.sms.smsnotice.dao.DaoCallback;
import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;
import jp.yksolution.android.sms.smsnotice.services.DbService;
import jp.yksolution.android.sms.smsnotice.services.MessageService;
import jp.yksolution.android.sms.smsnotice.services.ServiceBase;
import jp.yksolution.android.sms.smsnotice.services.ServiceMain;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DaoCallback {
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final String SERVICE_CLASS_NAME = ServiceMain.class.getSimpleName();
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mHandler = new Handler();
        this.refreshButton();

        // DBの作成／行使を行う
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.close();

        // DBサービスをバインド
        bindService(new Intent(getApplicationContext(), DbService.class), this.mDbServiceConnection, Context.BIND_AUTO_CREATE);
        // メッセージサービスをバインド
        bindService(new Intent(getApplicationContext(), MessageService.class), this.mMessageServiceConnection, Context.BIND_AUTO_CREATE);

        // 電話帳のアクセス状態を表示
        this.checkContactsButton();

        // SMS送信の許可状態を表示
        this.checkSMSSendButton();
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
                    // サービス実行中の場合、停止
                    this.stopService();
                } else {
                    // サービス停止の場合、起動
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
            case R.id.btnShowLog:
//                this.mMessageServiceHandler.sendMessage(Message.obtain(this.mMessageServiceHandler
//                        , MessageService.MESSAGE_WHAT_SMS_SEND, null));
                this.showLog();
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
        /*
        // 参考にしたサイト：https://www.memory-lovers.blog/entry/2017/09/17/094145
        //取得するカラムをは、名前とIDと誕生日
//        String[] projection = new String[]{
//                  ContactsContract.Contacts.DISPLAY_NAME
//                , ContactsContract.CommonDataKinds.Event.CONTACT_ID
//                , ContactsContract.CommonDataKinds.Event.START_DATE
//        };
        String[] projection = new String[]{
                  ContactsContract.Data.CONTACT_ID
                , ContactsContract.Data.MIMETYPE
                , ContactsContract.CommonDataKinds.StructuredName.DATA1
                , ContactsContract.CommonDataKinds.Phone.NUMBER
                , ContactsContract.CommonDataKinds.Note.DATA1
                , ContactsContract.CommonDataKinds.Organization.COMPANY
        };
//        String selection = "((" + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "') or ("
//                         + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'))";
        String selection = ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'";
        try {
            Cursor cursor = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI                     // and use selection
//                    ContactsContract.RawContacts.CONTENT_URI              // selection null
//                    ContactsContract.Contacts.CONTENT_URI              // selection null
//                    , null
                    , projection
                    , null
//                    , selection
                    , null, null);
            while (cursor.moveToNext()) {
////                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
//                String name = cursor.getString(nameIndex);
//                Log.i(ContactsContract.CommonDataKinds.Phone.LABEL, name);
////                Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
                for (String colmnName : cursor.getColumnNames()) {
                    String val = cursor.getString(cursor.getColumnIndex(colmnName));
                    Log.i(colmnName, (val == null) ? "null" : val);
                }
            }
        } catch (Exception ex) {
            Log.e("Exception", ex.toString());
        }
        */
        List<String> contactsList = new ArrayList<>();
        Map<String, MyContacts.Entity> contactMap = this.mMessageService.getContacts();
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
        LogEntity logEntity = new LogEntity(LogEntity.PROC_ID.LOG_LIST);
        logEntity.setDaoCallback(this);
        this.mDbService.requestLog(logEntity);
        Object n = this.mDbServiceHandler.sendMessage(Message.obtain(this.mDbServiceHandler
                , DbService.MESSAGE_WHAT_EXEC_QUERY, null));
    }
    public void finishedDbAccess(EntityBase entity) {
        if (entity instanceof LogEntity) {
            List<LogEntity> list = ((LogEntity)entity).getLogList();
            if ((list == null) || (list.isEmpty())) {
                Toast.makeText(this, "ログが登録されていません.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> contactsList = new ArrayList<>();
            for (LogEntity item : list) {
                contactsList.add(item.toLogViewString());
            }
            this.mHandler.post(new DBResult(this, contactsList));
        } else {
            Log.e(TAG, "not supported entity : " + ((entity == null) ? "null" : entity.getClass().getName()));
        }
    }

    private class DBResult extends Thread {
        private final List<String> mList;
        private final MainActivity mActivity;
        private DBResult(MainActivity activity, List<String> list) {
            this.mActivity = activity;
            this.mList = list;
        }
        @Override
        public void run() {
            ListView lv = (ListView) findViewById(R.id.lstApp);
            lv.setAdapter(new ArrayAdapter<String>(this.mActivity, android.R.layout.simple_list_item_1, this.mList));
        }
    }

    private void startService() {
        Intent intent = new Intent(getApplicationContext(), ServiceMain.class);
        if (Build.VERSION.SDK_INT <= 25) {
            startService(intent);
//        Toast.makeText(this, "MainActivity start service", Toast.LENGTH_SHORT).show();
        } else {
            startForegroundService(intent);
        }
        Log.d("[" + Thread.currentThread().getName() + "]" + this.getClass().getSimpleName()
                , "サービスメインを起動しました");
        this.refreshButton();

        // サービスメインをバインド
        bindService(new Intent(getApplicationContext(), ServiceMain.class), this.mServiceMainConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopService() {
        Log.d("[" + Thread.currentThread().getName() + "]" + MainActivity.class.getSimpleName()
                , "サービスメインを停止します");


        // サービスメインを終了させる
        this.mServiceMainHandler.sendMessage(Message.obtain(this.mServiceMainHandler
                , ServiceBase.SERVICE_WHAT_LOOP_EXIT, null));
        // アンバインド
//        unbindService(this.mMessageServiceConnection);
        unbindService(this.mServiceMainConnection);
//        unbindService(this.mDbServiceConnection);

        // サービス停止
        stopService(new Intent(getApplicationContext(), ServiceMain.class));
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
                        , "サービスメインが実行中");
                return true;
            }
        }
        Log.d("[" + Thread.currentThread().getName() + "]" + MainActivity.class.getSimpleName()
                , "サービスメインは、停止しています");
        return false;
    }
/*
    protected void refreshAppList(String targetClassName) {
        List<String> classList = new ArrayList<>();
        final Context _context = getApplicationContext();
        ActivityManager _am = (ActivityManager) _context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo item : _am.getRunningServices(Integer.MAX_VALUE)) {
//            classList.add(item.service.getClassName());
            if (item.service.getShortClassName().endsWith(targetClassName)) {
                classList.add(item.service.getShortClassName());
            }
        }
        ListView lv = (ListView) findViewById(R.id.lstApp);
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, classList));
    }
*/

    /**
     * サービスメイン
     */
//    protected ServiceMain mServiceMain = null;
    private Handler mServiceMainHandler;
    private ServiceConnection mServiceMainConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
//            mServiceMain = (ServiceMain)binder.getService();
            mServiceMainHandler = binder.getHadler();
            // サービスの処理を開始する
            mServiceMainHandler.sendMessage(Message.obtain(mServiceMainHandler
                    , ServiceMain.SERVIE_MAIN_WHAT_EXECUTE, null));
            Log.i(Thread.currentThread().getName(), "メインアクティビティは、サービスメインをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
//            mServiceMainHadle = null;
            Log.i(Thread.currentThread().getName(), "メインアクティビティは、サービスメインから切断されました.");
        }
    };

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
                    , "メインアクティビティは、DBサービスをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDbService = null;
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "メインアクティビティは、DBサービスから切断されました.");
        }
    };

    /**
     * メッセージサービス
     */
    private MessageService mMessageService;
    private Handler mMessageServiceHandler;
    private ServiceConnection mMessageServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceBase.LocalBinder binder = (ServiceBase.LocalBinder)service;
            mMessageService = (MessageService)binder.getService();
            mMessageServiceHandler = binder.getHadler();
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "メインアクティビティは、メッセージサービスをバインドしました");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessageService = null;
            Log.i("[" + Thread.currentThread().getName() + "]"
                    , "メインアクティビティは、メッセージサービスから切断されました.");
        }
    };
}