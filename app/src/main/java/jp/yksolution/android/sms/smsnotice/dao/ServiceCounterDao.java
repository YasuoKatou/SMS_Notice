package jp.yksolution.android.sms.smsnotice.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;
import jp.yksolution.android.sms.smsnotice.entity.ServiceCounterEntity;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * サービス処理状況テーブルDao.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class ServiceCounterDao extends DaoBase {
    private static final String MY_NAME = ServiceCounterDao.class.getSimpleName();

    /** サービス処理状況テーブルDaoのインスタンス. */
    private static final ServiceCounterDao mInstance = new ServiceCounterDao();

    private ServiceCounterDao() {}

    /**
     * サービス処理状況テーブルDaoインスタンスを取得する.
     * @return ログテーブルDao
     */
    public static final ServiceCounterDao getInstance() { return mInstance; }

    /**
     * サービス処理状況に対するクエリーを実行する.
     * @param db SQLiteDatabase
     * @param e EntityBase
     */
    @Override public void execute(SQLiteDatabase db, EntityBase e) {
        ServiceCounterEntity entity = (ServiceCounterEntity)e;
        switch (entity.getProcId()) {
            case ServiceCounterEntity.PROC_ID.ADD_COUNT:
                int count = addByUpdate(db, entity);
                if (count == 0) {
                    addByInsert(db, entity);
                }
                break;
            case ServiceCounterEntity.PROC_ID.LAST_24HOURS:
                this.getLast24HoursLog(db, entity);
                break;
            default:
                Log.e("[" + Thread.currentThread().getName() + "]" + MY_NAME
                        ,"not supported ProcId : " + entity.getProcId());
        }
    }

    /**
     * Updateでカウンタ情報（１０分値）を登録する.
     * @param db SQLiteDatabase
     * @param entity サービス処理状況テーブルエンティティ.
     * @return 更新レコード数
     */
    private int addByUpdate(SQLiteDatabase db, ServiceCounterEntity entity) {
        super.beginTransaction(db);
        ContentValues values = new ContentValues();
        int index10Minute = entity.get10MinuteIndex();
        String colName = String.format("proc_cnt_%d0", index10Minute);
        values.put(colName, entity.getProcCount());
        colName = String.format("proc_max_%d0", index10Minute);
        values.put(colName, entity.getProcMaxTime());
        String[] whereArgs = new String[] {Long.toString(entity.getAggregateTime())};
        int count = 0;
        try {
            count = db.update("t_svc_cnt", values, "count_date = ?", whereArgs);
            db.setTransactionSuccessful();
        } finally {
            long time = super.endTransaction(db);
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"update counter log : " + time + "ms (update:" + count + ")");
        }
        return count;
    }

    /**
     * Insertでカウンタ情報（１０分値）を登録する.
     * @param db SQLiteDatabase
     * @param entity サービス処理状況テーブルエンティティ.
     * @return 更新レコード数
     */
    private void addByInsert(SQLiteDatabase db, ServiceCounterEntity entity) {
        int index10Minute = entity.get10MinuteIndex();
        String colNames = String.format("proc_cnt_%d0,proc_max_%d0", index10Minute, index10Minute);

        StringBuffer insert = new StringBuffer("insert into t_svc_cnt");
        insert.append(" (count_date,")
              .append(colNames)
              .append(") values (?,?,?)");

        long count = 0;
        super.beginTransaction(db);
        try {
            try (SQLiteStatement statement = db.compileStatement(insert.toString())) {
                statement.bindLong(1, entity.getAggregateTime()); ;
                statement.bindLong(2, entity.getProcCount());
                statement.bindLong(3, entity.getProcMaxTime());
                count = statement.executeInsert();
                db.setTransactionSuccessful();
            }
        } finally {
            long time = super.endTransaction(db);
            Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                    ,"insert counter log : " + time + "ms (insert:" + count + ")");
        }
    }

    /**
     * カウンタ情報を取得する.
     * @param db SQLiteDatabase
     * @param entity サービス処理状況テーブルエンティティ.
     */
    private void getLast24HoursLog(SQLiteDatabase db, ServiceCounterEntity entity) {
        super.setStartTime();
        boolean distinct = false;
        String table = "t_svc_cnt";
        String[] columns = new String[]{"count_date"
                , "proc_cnt_00", "proc_max_00", "proc_cnt_10", "proc_max_10", "proc_cnt_20", "proc_max_20"
                , "proc_cnt_30", "proc_max_30", "proc_cnt_40", "proc_max_40", "proc_cnt_50", "proc_max_50"};
        String selection = "count_date >= ?";
        String[] selectionArgs = new String[]{String.valueOf(entity.getAggregateTime())};
        String groupBy = null;
        String having = null;
        String orderBy = "count_date desc";
        String limit = null;
        List<ServiceCounterEntity> list = new ArrayList<>();
        try (Cursor cursor = db.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)) {
            while (cursor.moveToNext()) {
                ServiceCounterEntity logEntity = new ServiceCounterEntity();
                logEntity.setAggregateTime(cursor.getLong(0));
                logEntity.setAggregateData(0, cursor.getInt(1), cursor.getInt(2));
                logEntity.setAggregateData(1, cursor.getInt(3), cursor.getInt(4));
                logEntity.setAggregateData(2, cursor.getInt(5), cursor.getInt(6));
                logEntity.setAggregateData(3, cursor.getInt(7), cursor.getInt(8));
                logEntity.setAggregateData(4, cursor.getInt(9), cursor.getInt(10));
                logEntity.setAggregateData(5, cursor.getInt(11), cursor.getInt(12));
                list.add(logEntity);
            }
        }
        entity.setServiceCounterList(list);
        long time = super.getProcessTime();
        Log.d("[" + Thread.currentThread().getName() + "]" + MY_NAME
                ,"select log time : " + time + "ms");
    }

    /**
     * ログテーブルを作成する.
     * @param db SQLiteDatabase
     */
    public static final void createServiceCounterTable(SQLiteDatabase db) {
        StringBuilder ddl;
        String tag = LogDao.class.getSimpleName();

        // テーブルを作成
        ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS t_svc_cnt (");
        ddl.append("count_date integer primary key")  // yyyy/mm/dd hh:mm:00のシリアル値
                .append(",proc_cnt_00 integer")    // 00分～10分の処理回数
                .append(",proc_max_00 integer")    // 00分～10分の最大処理時間
                .append(",proc_cnt_10 integer")
                .append(",proc_max_10 integer")
                .append(",proc_cnt_20 integer")
                .append(",proc_max_20 integer")
                .append(",proc_cnt_30 integer")
                .append(",proc_max_30 integer")
                .append(",proc_cnt_40 integer")
                .append(",proc_max_40 integer")
                .append(",proc_cnt_50 integer")
                .append(",proc_max_50 integer")
                .append(");");
        Log.d(tag, "CREATE TABLE : " + ddl.toString());
        db.execSQL(ddl.toString());
        Log.d(tag, "CREATE TABLE passed");
    }
}