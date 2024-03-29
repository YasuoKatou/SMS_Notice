package jp.yksolution.android.sms.smsnotice.services;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Map;

import jp.yksolution.android.sms.smsnotice.R;
import jp.yksolution.android.sms.smsnotice.contacts.MyContacts;
import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;
import jp.yksolution.android.sms.smsnotice.entity.MessageEntity;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * SMS送信サービス.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class MessageService extends ServiceBase {
    private static final String MY_NAME = MessageService.class.getSimpleName();
    private static final String SENT = "SMS_SENT";
    private static final String DELIVERED = "SMS_DELIVERED";

    private static enum SMS_STATUS {
            NOT_YET, RETRY, ERROR
    }

    @Override
    public void onCreate() {
        Log.d(super.getLogTag(MY_NAME), MY_NAME + ".onCreate");
        super.create(MY_NAME);
        // DBアクセスサービスをバインド
        super.bindDbService();

        // SMS送信結果
        this.mSentIntent = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        this.mDeliveryIntent = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

        // SMS送信先一覧         ← 2019/10/13 SMS送信要求化発生した都度、最新を取得するに変更
//        this.mContactMap = this.getContacts();
//        if (this.mContactMap != null) {
//            for (MyContacts.Entity entity : this.mContactMap.values()) {
//                Log.d(super.getLogTag(this.getClass().getSimpleName()), "contacts : " + entity.toString());
//            }
//        }
    }

    @Override
    public void onDestroy() {
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME,"onDestroy");

        super.destroy("Service Main");
    }

    private void registMessage(String message) {
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME,"registMessage");
        Map<String, MyContacts.Entity> contactMap = this.getContacts();
        if (contactMap == null) {
            Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME,"no contacts");
            return;
        }

        // 通知先分メッセージ送信レコードを登録する
        long now = System.currentTimeMillis();
        int contantNum = contactMap.values().size();
        int cnt = 0;
        for (MyContacts.Entity contact : contactMap.values()) {
            if (contact.getNote() == null) {
                --contantNum;
                continue;
            }
            if (contact.getNote().indexOf("Alert") < 0) {
                --contantNum;
                continue;
            }
            MessageEntity request = new MessageEntity(MessageEntity.PROC_ID.NEW_MESSAGE);
            request.setPhoneNo(contact.getPhoneNo());
            request.setPhoneName(contact.getName());
            request.setMessage(message);
            request.setStatus(MessageEntity.NOTICE_STATUS.IDLE);
            request.setCreateDate(now);
            request.setRetryCount(0);
            request.setBaseTime(now + cnt);
            request.setCallbackHandler((++cnt == contantNum) ? super.mServiceHandler : null);
            super.mDbService.requestBusiness(request);
        }
        // DBサービスをキックする
        this.kickDbService();
    }

    private void kickDbService() {
        super.mDbServiceHandler.sendMessage(Message.obtain(super.mDbServiceHandler
                , DbService.MESSAGE_WHAT_EXEC_QUERY, null));
    }

    /**
     * DBアクセスの終了を処理する.
     * このメソッドは、ＤＢサービスのスレッドから呼ばれる。
     * SMSの送信は、このサービスのスレッドで実施する
     * @param e オブジェクト
     */
    private void finishedDbAccess(Object e) {
//        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME, "finishedDbAccess");
        boolean kick = false;
        if (e instanceof MessageEntity) {
            MessageEntity entity = (MessageEntity)e;
            if (entity.getProcId() == MessageEntity.PROC_ID.NEW_MESSAGE) {
                // SMS送信要求の登録完了のとき、未送信メッセージの取得を行う
                MessageEntity request = new MessageEntity(MessageEntity.PROC_ID.SELECT_IDLE_MESSAGE);
                request.setCallbackHandler(super.mServiceHandler);
                super.mDbService.requestBusiness(request);
                kick = true;
            } else if (entity.getProcId() == MessageEntity.PROC_ID.SELECT_IDLE_MESSAGE) {
                // 未送信のメッセージの取得完了
                entity = entity.getMessageEntity();
                if (entity != null) {
                    // 未送信のメッセージを送信する
                    this.sendSMS(entity);
                } else {
                    // 未送信のメッセージがないとき、リトライ中のメッセージを取得
                    MessageEntity request = new MessageEntity(MessageEntity.PROC_ID.SELECT_RETRY_MESSAGE);
                    request.setCallbackHandler(super.mServiceHandler);
                    super.mDbService.requestBusiness(request);
                    kick = true;
                }
            } else if (entity.getProcId() == MessageEntity.PROC_ID.SELECT_RETRY_MESSAGE) {
                // リトライ中のメッセージの取得完了
                entity = entity.getMessageEntity();
                if (entity != null) {
                    // リトライ中のメッセージを送信する
                    this.sendSMS(entity);
                } else {
                    // リトライ中のメッセージなし
                    Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                            ,"no more sned Message");
                }
            } else {
                Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME + ":ERROR"
                        , "no supported process id : " + entity.getProcId());
            }
        } else {
            Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME + ":ERROR"
                    , "no supported entity class : " + e.toString());
        }
        if (kick) {
            // DBサービスをキックする
            this.kickDbService();
        }
    }

    public static final int MESSAGE_WHAT_INITIALIZE = 2001;
    public static final int MESSAGE_WHAT_SMS_REGIST = 2002;
    public static final int MESSAGE_WHAT_SMS_SEND = 2003;
    @Override
    public void executeMessage(Message msg) {
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                , "executeMessage : " + ((msg == null) ? "null" : msg.toString()));
        switch (msg.what) {
            case MESSAGE_WHAT_INITIALIZE:
                this.addBroadcastReceiver();
                break;
            case MESSAGE_WHAT_SMS_REGIST:
                this.registMessage((String)msg.obj);
                break;
            case MESSAGE_WHAT_SMS_SEND:
                Object o = msg.obj;
                if (o instanceof MessageEntity) {
                    // SMS 送信
                    this.sendSMS((MessageEntity)o);
                } else {
                    // イベントサービスからの定周期メッセージ（msg.obj = null）
                    Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                            , "cyclic message");
                    // 未送信メッセージをＤＢから取得
                    MessageEntity request = new MessageEntity(MessageEntity.PROC_ID.SELECT_IDLE_MESSAGE);
                    request.setCallbackHandler(MessageService.super.mServiceHandler);
                    MessageService.super.mDbService.requestBusiness(request);
                    // DBサービスをキックする
                    this.kickDbService();
                }
                break;
            case ServiceBase.SERVICE_WHAT_LOOP_EXIT:
                this.removeBroadcastReceiver();
                break;
            case EntityBase.MESSAGE_WHAT_QUERY_FINISHED:
                this.finishedDbAccess(msg.obj);
                break;
            default:
                Log.e(super.getLogTag(MY_NAME), "不明なWHAT : " + msg.what);
        }
    }

    private MessageEntity mProcessingMessageEntity = null;

    private PendingIntent mSentIntent = null;
    private PendingIntent mDeliveryIntent = null;
    /**
     * 指定の電話番号にメッセージを送信する
     * @param entity メッセージエンティティ.
     */
    private void sendSMS(MessageEntity entity) {
        if (mProcessingMessageEntity != null) {
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    , "sms busy... then wait...");
            return;
        }
        this.mProcessingMessageEntity = entity;
        String phoneNo = entity.getPhoneNo();
        String text    = entity.getMessage();

        SmsManager smsManager = SmsManager.getDefault();
        String scAddress = null;                // サービスセンターアドレス（null = デフォルト値）
        PendingIntent sentIntent = null;        // 送信の成否
        PendingIntent deliveryIntent = null;    // 受信者に届いたか否か
        smsManager.sendTextMessage(phoneNo, scAddress, text, this.mSentIntent, this.mDeliveryIntent);
        Log.i("[" + Thread.currentThread().getName() + "]" + MY_NAME, phoneNo + " : " + text);
    }

    private class SentReceiver extends BroadcastReceiver {
        /**
         * 正常終了でメッセージ送信リクエストを更新する
         */
        private void updateSmsTable() {
            MessageEntity entity = mProcessingMessageEntity.deepCopy(MessageEntity.PROC_ID.SENT_MESSAGE);
            entity.setStatus(MessageEntity.NOTICE_STATUS.COMPLETED);
            entity.setUpdateDate(System.currentTimeMillis());
            entity.setErrorMessage(null);        // nullは、更新なし
            entity.setCallbackHandler(null);     // 更新結果は不要
            MessageService.super.mDbService.requestBusiness(entity);

            // ログの登録
            StringBuilder sb = new StringBuilder("SMS Send");
            sb.append(" : ").append(entity.getPhoneNo())
              .append(", ").append(entity.getMessage());
            LogEntity logEntity = new LogEntity(LogEntity.LOG_LEVEL.INFO, sb.toString());
            MessageService.super.mDbService.requestLog(logEntity);
        }

        /**
         * メッセージ送信リクエストを異常で更新する
         * @param errMsg エラーメッセージ
         */
        private void updateSmsTable(String errMsg) {
            Resources res = getResources();
            final int maxRetry = res.getInteger(R.integer.retry_max_02);
            final long waitTime = res.getInteger(R.integer.retry_timer_02);
            MessageEntity.NOTICE_STATUS stat;
            long baseTime;
            int callCount = mProcessingMessageEntity.getRetryCount() + 1;
            LogEntity.LOG_LEVEL logLevel;
            if (callCount < maxRetry) {
                stat = MessageEntity.NOTICE_STATUS.RETRY;
                baseTime = System.currentTimeMillis() + waitTime;
                logLevel = LogEntity.LOG_LEVEL.WARN;
            } else {
                stat = MessageEntity.NOTICE_STATUS.ERROR;
                baseTime = mProcessingMessageEntity.getBaseTime();
                logLevel = LogEntity.LOG_LEVEL.ERROR;
            }
            MessageEntity entity = mProcessingMessageEntity.deepCopy(MessageEntity.PROC_ID.SENT_MESSAGE);
            entity.setStatus(stat);
            entity.setErrorMessage(errMsg);
            entity.setRetryCount(callCount);
            entity.setBaseTime(baseTime);
            entity.setUpdateDate(System.currentTimeMillis());
            entity.setCallbackHandler(null);     // 更新結果は不要
            MessageService.super.mDbService.requestBusiness(entity);

            // エラーログの登録
            LogEntity logEntity = new LogEntity(logLevel, "SMS Send : " + errMsg);
            MessageService.super.mDbService.requestLog(logEntity);
            Log.d(DateTime.now() + " retry time", DateTime.dateTimeFormat(baseTime));
        }

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String tag = "[" + Thread.currentThread().getName() + "]" + SENT;
            Log.d(tag, "SMS sent\narg0" + arg0.toString() + "\narg1" + arg1.toString());
            int resultCode = getResultCode();
            switch (resultCode) {
                case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                    Log.d(tag, "SMS sent");
                    this.updateSmsTable();
                    break;
                case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                    Log.d(tag, "we reached the sending queue limit");
                    this.updateSmsTable("[" + resultCode + "] : we reached the sending queue limit");
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                        Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                    Log.e(tag, "Generic failure cause");
                    this.updateSmsTable("[" + resultCode + "] : Generic failure cause");
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
//                        Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                    Log.e(tag, "service is currently unavailable");
                    this.updateSmsTable("[" + resultCode + "] : service is currently unavailable");
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
//                        Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                    Log.e(tag, "no pdu provided");
                    this.updateSmsTable("[" + resultCode + "] : no pdu provided");
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
//                        Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                    Log.e(tag, "radio was explicitly turned off");
                    this.updateSmsTable("[" + resultCode + "] : radio was explicitly turned off");
                    break;
                default:
                    Log.e(tag, "unknown error code : " + getResultCode());
                    this.updateSmsTable("[" + resultCode + "] : unknown error code");
                    break;
            }
            // SMS送信要求の取得（未送信を優先して取得する）
            MessageEntity request = new MessageEntity(MessageEntity.PROC_ID.SELECT_IDLE_MESSAGE);
            request.setCallbackHandler(MessageService.super.mServiceHandler);
            MessageService.super.mDbService.requestBusiness(request);
            // DBサービスをキックする
            kickDbService();
            mProcessingMessageEntity = null;
        }
    }
    private class DeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String tag = "[" + Thread.currentThread().getName() + "]" + DELIVERED;
            Log.d(tag, "SMS sent\narg0" + arg0.toString() + "\narg1" + arg1.toString());
            String now = DateTime.now();
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                    Log.d(tag, "SMS delivered");
                    break;
                case Activity.RESULT_CANCELED:
//                        Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                    Log.d(tag, "SMS not delivered");
                    break;
            }
        }
    }

    private SentReceiver mSentReceiver = new SentReceiver();
    private DeliveredReceiver mDeliveredReceiver = new DeliveredReceiver();
    private void removeBroadcastReceiver() {
        try {
            unregisterReceiver(this.mSentReceiver);
        } catch (Exception ex) {
            Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"removeBroadcastReceiver : " + ex.toString());
        }
        try {
            unregisterReceiver(this.mDeliveredReceiver);
        } catch (Exception ex) {
            Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"removeBroadcastReceiver : " + ex.toString());
        }
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME,"removeBroadcastReceiver");
    }
    private void addBroadcastReceiver() {
        registerReceiver(this.mSentReceiver, new IntentFilter(SENT));
        registerReceiver(this.mDeliveredReceiver, new IntentFilter(DELIVERED));
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME,"addBroadcastReceiver");
    }

//    private Map<String, MyContacts.Entity> mContactMap = null;
    public Map<String, MyContacts.Entity> getContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // 電話帳へのアクセスが許可されていない
            return null;
        }
        long time1 = System.currentTimeMillis();
        MyContacts contacts = new MyContacts(this);
        Map<String, MyContacts.Entity> contactMap = contacts.editContacts();
        long time = System.currentTimeMillis() - time1;
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
        , "Contact List process time : " + time + "ms, members : " + contactMap.values().size());
        return contactMap;
    }
}