package jp.yksolution.android.sms.smsnotice.entity;

import java.util.List;

import jp.yksolution.android.sms.smsnotice.dao.LogDao;

public class LogEntity extends EntityBase {
    public static enum LOG_LEVEL {
        ERROR, INFO, DEBUG, TRACE
    }
    public static class PROC_ID {
        /** 処理ID：ログ登録. */
        public static final int INSERT = 1;
        /** 処理ID：ログ取得（５０件）. */
        public static final int LOG_LIST = 2;
    }

    /** ログレベル. */
    private LOG_LEVEL logLevel;
    /** ログデータ. */
    private String logContents;
    /** ログ日時. */
    private String logDateTime;

    /**
     * ログ登録コンストラクタ.
     * @param logLevel ログレベル.
     * @param contents ログの内容.
     */
    public LogEntity(LOG_LEVEL logLevel, String contents) {
        this(PROC_ID.INSERT);
        this.logDateTime = super.now();
        this.logLevel = logLevel;
        this.logContents = contents;
    }

    public LogEntity(int procId) {
        super.mProcId = procId;
        super.mDaoClassName = LogDao.class.getSimpleName();
    }

    public LogEntity() {
    }

    public LOG_LEVEL getLogLevel() {
        return this.logLevel;
    }

    public void setLogLevel(LOG_LEVEL logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogContents() {
        return this.logContents;
    }

    public void setLogContents(String logContents) {
        this.logContents = logContents;
    }

    public String getLogDateTime() {
        return this.logDateTime;
    }

    public void setLogDateTime(String logDateTime) {
        this.logDateTime = logDateTime;
    }

    /** ログ一覧. */
    private List<LogEntity> mLogList;
    public List<LogEntity> getLogList() { return this.mLogList; }
    public void setLogList(List<LogEntity> list) { this.mLogList = list; }

    public String toValues() {
        StringBuffer vals = new StringBuffer();
        return vals.append((this.logDateTime == null) ? "null" : this.logDateTime).append("(String)")
                    .append((" "))
                    .append((this.logLevel == null) ? "null" : this.logLevel.toString()).append("(String)")
                    .append((" "))
                    .append((this.logContents == null) ? "null" : this.logContents).append(("(String)"))
                    .toString();
    }

    public static LOG_LEVEL getLogLevel(String value) {
        if (LogEntity.LOG_LEVEL.ERROR.toString().equals(value)) {
            return LogEntity.LOG_LEVEL.ERROR;
        } else if (LogEntity.LOG_LEVEL.INFO.toString().equals(value)) {
            return LogEntity.LOG_LEVEL.INFO;
        } else if (LogEntity.LOG_LEVEL.DEBUG.toString().equals(value)) {
            return LogEntity.LOG_LEVEL.DEBUG;
        } else if (LogEntity.LOG_LEVEL.TRACE.toString().equals(value)) {
            return LogEntity.LOG_LEVEL.TRACE;
        }
        return null;
    }

    public String toLogViewString() {
        String logLevel = (this.logLevel == null) ? " " : this.logLevel.toString().substring(0, 1);
        StringBuffer str = new StringBuffer();
        str.append("[").append(logLevel).append("] ");
        if (this.logDateTime == null) {
            str.append("??/?? ??:??:??");
        } else {
            str.append(this.logDateTime.substring(5)); //月～秒までを抽出
        }
        str.append(" ");
        str.append((this.logContents == null) ? "" : this.logContents);
        return str.toString();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getSimpleName());
        sb.append("(").append("Proc ID : ").append(this.mProcId)
          .append(", Date Time : ").append(this.logDateTime)
          .append(", Log Level : ").append(this.logLevel)
          .append(", Content : ").append(this.logContents)
          .append(")");
        return sb.toString();
    }
}