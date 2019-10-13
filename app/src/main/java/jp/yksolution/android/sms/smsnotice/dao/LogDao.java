package jp.yksolution.android.sms.smsnotice.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;

public class LogDao extends DaoBase {
    private static final String MY_NAME = LogDao.class.getSimpleName();

    private static final LogDao thisInstance = new LogDao();
    private final String mSQL_Insert;
    /**
     * 空のコンストラクタを使用禁止にする
     */
    private LogDao () {
        this.mSQL_Insert = this.editInsert();
    }
    public static final LogDao getInstance() { return thisInstance; }

    private LogEntity entity = null;
    public void setEntity(LogEntity entity) {
        this.entity = entity;
    }

    private static String editInsert() {
        StringBuffer insert = new StringBuffer("insert into t_log");
        insert.append(" (create_date,_level,contents) values")
              .append(" (?,?,?)");
        return insert.toString();
    }

    @Override public void execute(SQLiteDatabase db, EntityBase e) {
        LogEntity entity = (LogEntity)e;
        switch (entity.getProcId()) {
            case LogEntity.PROC_ID.INSERT:
                this.appendLog(db, entity);
                break;
            case LogEntity.PROC_ID.LOG_LIST:
                this.makeLogList(db, entity);
                break;
            default:
                Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME
                        ,"not supported ProcId : " + entity.getProcId());
        }
    }

    private void appendLog(SQLiteDatabase db, LogEntity entity) {
        super.beginTransaction(db);
        try {
            try (SQLiteStatement statement = db.compileStatement(this.mSQL_Insert)) {
                statement.bindString(1, entity.getLogDateTime());
                statement.bindString(2, entity.getLogLevel().toString());
                statement.bindString(3, entity.getLogContents());
                statement.executeInsert();
                db.setTransactionSuccessful();
            }
        } finally {
            long time = super.endTransaction(db);
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"insert log time : " + time + "ms");
        }
    }

    private void makeLogList(SQLiteDatabase db, LogEntity entity) {
        super.setStartTime();
        boolean distinct = false;
        String table = "t_log";
        String[] columns = new String[]{"create_date", "_level", "contents"};
        String selection = null;
        String[] selectionArgs = null;
        String groupBy = null;
        String having = null;
        String orderBy = "create_date desc";
        String limit = "0,50";
        List<LogEntity> list = new ArrayList<>();
        try (Cursor cursor = db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)) {
            while (cursor.moveToNext()) {
                LogEntity logEntity = new LogEntity();
                logEntity.setLogDateTime(cursor.getString(0));
                logEntity.setLogLevel(LogEntity.getLogLevel(cursor.getString(1)));
                logEntity.setLogContents(cursor.getString(2));
                list.add(logEntity);
            }
        }
        entity.setLogList(list);
        long time = super.getProcessTime();
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                ,"select log time : " + time + "ms");
    }

    public static final void createLogTable(SQLiteDatabase db) {
        StringBuilder ddl;
        String tag = LogDao.class.getSimpleName();

        // テーブルを作成
        ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS t_log (");
        ddl.append("create_date text")
                .append(",_level text")
                .append(",contents text")
                .append(");");
        Log.d(tag, "CREATE TABLE : " + ddl.toString());
        db.execSQL(ddl.toString());
        Log.d(tag, "CREATE TABLE passed");

        // インデックスを作成
        ddl = new StringBuilder("create index t_log_idx1 on t_log(create_date);");
        Log.d(tag, "CREATE INDEX : " + ddl.toString());
        db.execSQL(ddl.toString());
        Log.d(tag, "CREATE INDEX passed");
    }
}