package jp.yksolution.android.sms.smsnotice.entity;

import java.util.List;

import jp.yksolution.android.sms.smsnotice.dao.MessageDao;
import jp.yksolution.android.sms.smsnotice.utils.DateTime;

/**
 * メッセージ送信管理テーブルエンティティ.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class MessageEntity extends EntityBase {
    public static class PROC_ID {
        /** 新規メッセージの登録. */
        public static final int NEW_MESSAGE = 1;
        /** 未送信のメッセージを取得. */
        public static final int SELECT_IDLE_MESSAGE = 2;
        /** 再送メッセージを取得. */
        public static final int SELECT_RETRY_MESSAGE = 3;
        /** 送信結果を更新. */
        public static final int SENT_MESSAGE = 4;
        /** 24時間データの取得. */
        public static final int LAST_24HOURS = 91;
    }

    public static enum NOTICE_STATUS {
        IDLE, COMPLETED, RETRY, ERROR
    }
    public void setStatus(String status) {
        if ("IDLE".equals(status)) {
            this.status = NOTICE_STATUS.IDLE;
        } else if ("COMPLETED".equals(status)) {
            this.status = NOTICE_STATUS.COMPLETED;
        } else if ("RETRY".equals(status)) {
            this.status = NOTICE_STATUS.RETRY;
        } else if ("ERROR".equals(status)) {
            this.status = NOTICE_STATUS.ERROR;
        }
    }

    public MessageEntity () {}
    public MessageEntity(int procId) {
        super.mProcId = procId;
        super.mDaoClassName = MessageDao.class.getSimpleName();
    }

    /** id. */
    private Integer id;
    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }

    /** 通知先電話番号. */
    private String phoneNo;
    public String getPhoneNo() { return this.phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

    /** 通知先電話番号の氏名. */
    private String phoneName;
    public String getPhoneName() { return this.phoneName; }
    public void setPhoneName(String phoneName) { this.phoneName = phoneName; }

    /** 通知文 */
    private String message;
    public String getMessage() { return this.message; }
    public void setMessage(String message) { this.message = message; }

    /** 送信状態. */
    private NOTICE_STATUS status;
    public NOTICE_STATUS getStatus() { return this.status; }
    public void setStatus(NOTICE_STATUS status) { this.status = status; }

    /** エラーメッセージ */
    private String errorMessage;
    public String getErrorMessage() { return this.errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /** 再送回数 */
    private int retryCount;
    public int getRetryCount() { return this.retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    /** 基準時刻(リトライを行う時刻を設定する) */
    private long baseTime;
    public long getBaseTime() { return this.baseTime; }
    public void setBaseTime(long baseTime) { this.baseTime = baseTime; }

    /** 作成日時. */
    private long createDate;
    public long getCreateDate() { return this.createDate; }
    public void setCreateDate(long createDate) { this.createDate = createDate; }

    /** 更新日時 */
    private long updateDate;
    public long getUpdateDate() { return this.updateDate; }
    public void setUpdateDate(long updateDate) { this.updateDate = updateDate; }

    /** 既読日時 */
    private long deliveredDate;
    public long getDeliveredDate() { return this.deliveredDate;}
    public void setDeliveredDate(long deliveredDate) { this.deliveredDate = deliveredDate; }

    /** メッセージエンティティ */
    private MessageEntity messageEntity;
    public MessageEntity getMessageEntity() { return this.messageEntity; }
    public void setMessageEntity(MessageEntity messageEntity) { this.messageEntity = messageEntity; }

    private List<MessageEntity> mMessageEntityList;
    public List<MessageEntity> getMessageEntityList() { return this.mMessageEntityList; }
    public void setMessageEntityList(List<MessageEntity> mMessageEntityList) {
        this.mMessageEntityList = mMessageEntityList;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getSimpleName());
        sb.append("(").append("Proc ID : ").append(this.mProcId)
                .append(", id : ").append(this.id)
                .append(", Phone No : ").append(this.phoneNo)
                .append(", Phone Name : ").append(this.phoneName)
                .append(", Message : ").append(this.message)
                .append(", status : ").append(this.status)
                .append(", create date : ").append(DateTime.dateTimeFormat(this.createDate))
                .append(", update date : ").append(DateTime.dateTimeFormat(this.updateDate))
                .append(", error message : ").append(this.errorMessage)
                .append(")");
        return sb.toString();
    }

    public String toTitleString() {
        StringBuffer sb = new StringBuffer("■");
        return sb.append(DateTime.mmddHHMMFormat(this.createDate)).append(" ").append(this.message).toString();
    }

    public String toResultString() {
        StringBuffer sb = new StringBuffer("　");
        sb.append(DateTime.HHMMFormat(this.updateDate)).append(" ").append(this.phoneNo).append(" ");
        switch (this.getStatus()) {
            case IDLE:
                sb.append("未処理");
                break;
            case COMPLETED:
                sb.append("正常終了");
                break;
            case ERROR:
                sb.append("異常終了 [").append(this.errorMessage).append("]");
                break;
            case RETRY:
                sb.append("再試行中");
                break;
        }
        return sb.toString();
    }

    public MessageEntity deepCopy(int procId) {
        MessageEntity clone = new MessageEntity(procId);
        clone.id = this.id;
        clone.phoneNo = this.phoneNo;
        clone.phoneName = this.phoneName;
        clone.message = this.message;
        clone.status = this.status;
        clone.errorMessage = this.errorMessage;
        clone.retryCount = this.retryCount;
        clone.baseTime = this.baseTime;
        clone.createDate = this.createDate;
        clone.updateDate = this.updateDate;
        clone.deliveredDate = this.deliveredDate;
        return clone;
    }
}