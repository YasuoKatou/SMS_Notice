package jp.yksolution.android.sms.smsnotice.dao;

import android.os.Handler;

import jp.yksolution.android.sms.smsnotice.entity.EntityBase;

public interface DaoCallback {
    /**
     * DBアクセスの終了を処理する.
     * @param entity Entityオブジェクト
     */
    void finishedDbAccess(EntityBase entity);
}