package jp.yksolution.android.sms.smsnotice.entity;

import android.text.format.DateFormat;

import java.util.Date;

/**
 * サービス処理状況テーブルエンティティ.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class ServiceCounterEntity extends EntityBase {
    public static class PROC_ID {
        /** 新規登録. */
        public static final int APPEND = 1;
        /** 10分単位データの更新. */
        public static final int ADD_COUNT = 2;
    }

    /** 集計開始日時（yyyy/mm/dd hh:00:00を示すシリアル値）. */
    private long aggregateTime;
    public long getAggregateTime() { return this.aggregateTime; }
    public void setAggregateTime(long aggregateTime) { this.aggregateTime = aggregateTime; }

    /** 処理回数（１０分×６回）. */
    private Integer[] mProcCount = new Integer[]{null, null, null, null, null, null};
    /** 最大処理時間（１０分×６回）. */
    private Integer[] mProcMaxTime = new Integer[]{null, null, null, null, null, null};
    public void setAggregateData(int minuteIndex, int procCount, int maxTime) {
        this.mProcCount[minuteIndex] = Integer.valueOf(procCount);
        this.mProcMaxTime[minuteIndex] = Integer.valueOf(maxTime);
    }

    /**
     * 処理回数（１時間分）を取得する.
     * @return  処理回数一覧
     */
    public Integer[] getProcCount() { return this.mProcCount; }

    /**
     * 最大処理時間（１時間分）を取得する..
     * @return 大処理時間一覧
     */
    public Integer[] getProcMaxTime() { return this.mProcMaxTime; }

    @Override
    public String toString() {
        String dateTime = DateFormat.format("MM/dd kk:mm:ss", new Date(this.aggregateTime)).toString();
        StringBuilder sb = new StringBuilder(dateTime);
        for (int index = 0; index < this.mProcCount.length; ++index) {
            Integer val = this.mProcCount[index];
            sb.append(",").append((val == null) ? "-" : val.intValue());
            val = this.mProcMaxTime[index];
            sb.append(",[").append((val == null) ? "-" : val.intValue()).append("]");
        }
        return sb.toString();
    }
}