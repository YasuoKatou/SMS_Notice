package jp.yksolution.android.sms.smsnotice.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.LogEntity;

/**
 * ログテーブルDao.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class LogDao extends DaoBase {
    private static final String MY_NAME = LogDao.class.getSimpleName();

    /** ログテーブルDaoのインスタンス. */
    private static final LogDao mInstance = new LogDao();
    /** ログ登録(insert)クエリー文字列. */
    private final String mSQL_Insert;
    /**
     * 空のコンストラクタを使用禁止にする
     */
    private LogDao () { this.mSQL_Insert = this.editInsert(); }

    /**
     * ログテーブルDaoインスタンスを取得する.
     * @return ログテーブルDao
     */
    public static final LogDao getInstance() { return mInstance; }
//
//    private LogEntity entity = null;
//    public void setEntity(LogEntity entity) {
//        this.entity = entity;
//    }

    /**
     * ログ登録(insert)クエリー文字列を編集する.
     * @return ログ登録(insert)クエリー文字列
     */
    private static String editInsert() {
        StringBuffer insert = new StringBuffer("insert into t_log");
        insert.append(" (create_date,_level,contents) values")
              .append(" (?,?,?)");
        return insert.toString();
    }

    /**
     * ログテーブルに対するクエリーを実行する.
     * @param db SQLiteDatabase
     * @param e EntityBase
     */
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

    /**
     * ログをinsertするクエリーを実行する.
     * @param db SQLiteDatabase
     * @param entity LogEntity
     */
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

    /**
     * ログの一覧を取得するクエリーを実行する.
     * @param db SQLiteDatabase
     * @param entity LogEntity
     */
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

    /**
     * ログテーブルを作成する.
     * @param db SQLiteDatabase
     */
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