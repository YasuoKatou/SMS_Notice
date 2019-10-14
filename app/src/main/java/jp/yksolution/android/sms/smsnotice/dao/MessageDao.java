package jp.yksolution.android.sms.smsnotice.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.MessageEntity;

/**
 * メッセージ送信管理テーブルDao.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class MessageDao extends DaoBase {
    private static final String MY_NAME = MessageDao.class.getSimpleName();

    /** メッセージ送信管理テーブルDaoのインスタンス. */
    private static final MessageDao thisInstance = new MessageDao();
    /** メッセージ送信管理テーブル登録(insert)クエリー文字列. */
    private final String mSQL_Insert;
    /**
     * 空のコンストラクタを使用禁止にする
     */
    private MessageDao () {
        this.mSQL_Insert = this.editInsert();
    }

    /**
     * メッセージ送信管理テーブルDaoインスタンスを取得する.
     * @return メッセージ送信管理テーブルDao
     */
    public static final MessageDao getInstance() { return thisInstance; }

    /**
     * メッセージ送信管理テーブル登録(insert)クエリー文字列を編集する.
     * @return メッセージ送信管理テーブル登録(insert)クエリー文字列
     */
    private static String editInsert() {
        StringBuffer insert = new StringBuffer("insert into t_msg");
        insert.append(" (phone_no,phone_name,message,status,create_date) values")
                .append(" (?,?,?,?,?)");
        return insert.toString();
    }

    /**
     * メッセージ送信管理テーブルに対するクエリーを実行する.
     * @param db SQLiteDatabase
     * @param e EntityBase
     */
    @Override
    public void execute(SQLiteDatabase db, EntityBase e) {
        MessageEntity entity = (MessageEntity)e;
//        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
//                ,"execute sql : " + entity.toString());
        switch (entity.getProcId()) {
            case MessageEntity.PROC_ID.NEW_MESSAGE:
                this.appendMessage(db, entity);
                break;
            case MessageEntity.PROC_ID.SELECT_MESSAGE:
                this.selectSendMessage(db, entity);
                break;
            case MessageEntity.PROC_ID.SENT_MESSAGE:
                this.updateSendStatus(db, entity);
                break;
            default:
                Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME
                        ,"not supported ProcId : " + entity.getProcId());
        }
    }

    /**
     * メッセージ送信管理テーブルにinsertするクエリーを実行する.
     * @param db SQLiteDatabase
     * @param entity MessageEntity
     */
    private void appendMessage(SQLiteDatabase db, MessageEntity entity) {
        super.beginTransaction(db);
        try {
            try (SQLiteStatement statement = db.compileStatement(this.mSQL_Insert)) {
                statement.bindString(1, entity.getPhoneNo());
                statement.bindString(2, entity.getPhoneName());
                statement.bindString(3, entity.getMessage());
                statement.bindString(4, entity.getStatus().toString());
                statement.bindString(5, entity.getCreateDate());
                statement.executeInsert();
                db.setTransactionSuccessful();
            }
        } finally {
            long time = super.endTransaction(db);
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"insert log time : " + time + "ms");
        }
    }

    /**
     * メッセージ送信管理テーブルから未送信のデータを１レコード取得する.
     * @param db SQLiteDatabase
     * @param entity MessageEntity
     */
    private void selectSendMessage(SQLiteDatabase db, MessageEntity entity) {
        super.setStartTime();
        boolean distinct = false;
        String table = "t_msg";
        String[] columns = new String[]{"id", "phone_no", "phone_name", "message"};
        String selection = "status = ?";
        String[] selectionArgs = new String[] {MessageEntity.NOTICE_STATUS.IDLE.toString()};
        String groupBy = null;
        String having = null;
        String orderBy = "create_date desc";
//        String orderBy = "create_date";
        String limit = "0,1";
        MessageEntity sendSMS = null;
        try (Cursor cursor = db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)) {
            if (cursor.moveToNext()) {
                sendSMS = new MessageEntity();
                sendSMS.setId(cursor.getInt(0));
                sendSMS.setPhoneNo(cursor.getString(1));
                sendSMS.setPhoneName(cursor.getString(2));
                sendSMS.setMessage(cursor.getString(3));
            }
        }
        entity.setMessageEntity(sendSMS);
        long time = super.getProcessTime();
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                ,"select send message : " + time + "ms");
        if (sendSMS == null) {
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"no more send message");
        } else {
            Log.d("next send SMS message", sendSMS.toString());
        }
    }

    /**
     * メッセージ送信管理テーブルの送信結果を更新する.
     * @param db SQLiteDatabase
     * @param entity MessageEntity
     */
    private void updateSendStatus(SQLiteDatabase db, MessageEntity entity) {
        super.beginTransaction(db);
        ContentValues values = new ContentValues();
        values.put("status", entity.getStatus().toString());
        values.put("update_date", entity.getUpdateDate());
        String errMsg = entity.getErrorMessage();
        if (errMsg != null) values.put("err_msg", errMsg);
        try {
            db.update("t_msg", values, "id = " + entity.getId(), null);
            db.setTransactionSuccessful();
        } finally {
            long time = super.endTransaction(db);
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"update message send status : " + time + "ms");
        }
    }

    /**
     * メッセージ送信管理テーブル作成する.
     * @param db SQLiteDatabase
     */
    public static final void createMessageTable(SQLiteDatabase db) {
        StringBuilder ddl;
        String tag = LogDao.class.getSimpleName();

        // テーブルを作成
        ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS t_msg (");
        ddl.append("id integer primary key")     // ID
           .append(",phone_no text not null")    // 通知先電話番号
           .append(",phone_name text not null")  // 通知先氏名
           .append(",message text not null")     // 通知内容
           .append(",status text not null")      // 送信状態
           .append(",err_msg text")              // エラーメッセージ
           .append(",call_count integer")        // 再送回数
           .append(",base_time int")             // 基準時間
           .append(",create_date text not null") // 作成日時
           .append(",update_date text")          // 更新日時
           .append(",delivered text")            // 既読日時
           .append(");");
        Log.d(tag, "CREATE TABLE : " + ddl.toString());
        db.execSQL(ddl.toString());
        Log.d(tag, "CREATE TABLE passed");

        // インデックスを作成
        ddl = new StringBuilder("create index t_msg_idx1 on t_msg(status, create_date);");
        Log.d(tag, "CREATE INDEX : " + ddl.toString());
        db.execSQL(ddl.toString());
        Log.d(tag, "CREATE INDEX passed");
    }
}