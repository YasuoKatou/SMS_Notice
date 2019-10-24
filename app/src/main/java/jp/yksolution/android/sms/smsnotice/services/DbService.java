package jp.yksolution.android.sms.smsnotice.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import jp.yksolution.android.sms.smsnotice.DbHelper;
import jp.yksolution.android.sms.smsnotice.dao.DaoBase;
import jp.yksolution.android.sms.smsnotice.dao.LogDao;
import jp.yksolution.android.sms.smsnotice.dao.MessageDao;
import jp.yksolution.android.sms.smsnotice.dao.ServiceCounterDao;
import jp.yksolution.android.sms.smsnotice.entity.EntityBase;

/**
 * DBアクセスサービス
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class DbService extends ServiceBase {
//    private final static String TAG = DbService.class.getSimpleName();
    public static final int MESSAGE_WHAT_EXEC_QUERY = 1;

    private static final int MAX_REQUEST = 5;
    private final Object[] mLock = new Object[MAX_REQUEST];
    private final List<EntityBase>[] mDbReq = new List[MAX_REQUEST];

    private DbHelper mDbHelper;
    private SQLiteDatabase db;

    @Override
    public void onCreate() {
        String className = this.getClass().getSimpleName();
        Log.d(super.getLogTag(className), className + ".onCreate");

        for (int idx = 0; idx < MAX_REQUEST; ++idx) {
            this.mDbReq[idx] = new ArrayList<>();
            this.mLock[idx] = new Object();
        }
        this.mDbHelper = new DbHelper(this);
        this.db = this.mDbHelper.getWritableDatabase();

        super.create(className);
    }

    @Override
    public void executeMessage(Message msg) {
//        Log.d("[" + Thread.currentThread().getName() + "]" + this.getClass().getSimpleName()
//                , "executeMessage : " + ((msg == null) ? "null" : msg.toString()));

        EntityBase entity;
        do {
            entity = null;
            for (int priority = 0; priority < this.mDbReq.length; ++priority) {
                entity = this.getRequest(priority);
                if (entity != null) {
                    DaoBase dao = this.getDao(entity);
                    try {
                        dao.execute(this.db, entity);
                        entity.setResult(EntityBase.RESULT.NORMAL_COMPLETED);
                    } catch (Exception ex) {
                        Log.e(Thread.currentThread().getName()
                                , "Dao.execute 失敗\n" + entity.toString() + "\n" + ex.toString());
                        entity.setResult(EntityBase.RESULT.ABNORMAL_COMPLETED);
                    }
                    try {
                        entity.finished();
                    } catch (Exception ex) {
                        Log.e(Thread.currentThread().getName(), "Dao.finished 失敗\n" + ex.toString());
                    }
                    break;
                }
            }
        } while (entity != null);
    }

    /**
     * DBにアクセスするDaoインスタンスを取得する.
     * @param entity Entityオブジェクト
     * @return Daoインスタンス
     */
    private DaoBase getDao(EntityBase entity) {
        String daoClassName = entity.getDaoClassName();
        if ("LogDao".equals(daoClassName)) {
            return LogDao.getInstance();
        } else if ("MessageDao".equals(daoClassName)) {
            return MessageDao.getInstance();
        } else if ("ServiceCounterDao".equals(daoClassName)) {
            return ServiceCounterDao.getInstance();
        }
        Log.d("[" + Thread.currentThread().getName() + "]" + this.getClass().getSimpleName()
                , "unsupportted dao class name : " + ((daoClassName == null) ? "null" : daoClassName));
        return null;
    }

    private EntityBase getRequest(int priority) {
        synchronized(mLock[priority]) {
            if (this.mDbReq[priority].size() > 0) {
                return mDbReq[priority].remove(0);
            } else {
                return null;
            }
        }
    }

    /**
     * 優先順位１のDBアクセスリクエストを設定する.
     * @param entity Entityオブジェクト
     */
    public void requestBusiness(EntityBase entity) {synchronized(mLock[0]) { mDbReq[0].add(entity); }}

    /**
     * 優先順位２のDBアクセスリクエストを設定する.
     * @param entity Entityオブジェクト
     */
    public void requestDBAccess2(EntityBase entity) {synchronized(mLock[1]) { mDbReq[1].add(entity); }}

    /**
     * 優先順位３（処理の計測）のDBアクセスリクエストを設定する.
     * @param entity Entityオブジェクト
     */
    public void requestMeasured(EntityBase entity) {synchronized(mLock[2]) { mDbReq[2].add(entity); }}

    /**
     * 優先順位４のDBアクセスリクエストを設定する.
     * @param entity Entityオブジェクト
     */
    public void requestDBAccess4(EntityBase entity) {synchronized(mLock[3]) { mDbReq[3].add(entity); }}

    /**
     * 優先順位５（ログ）のDBアクセスリクエストを設定する.
     * @param entity Entityオブジェクト
     */
    public void requestLog(EntityBase entity) {synchronized(mLock[4]) { mDbReq[4].add(entity); }}

    @Override
    public void onDestroy() {
        Log.d(Thread.currentThread().getName() + "[" + this.getClass().getSimpleName() + "]","onDestroy");

        // DBを閉じる
        this.db.close();
    }
}