package jp.yksolution.android.sms.smsnotice.entity;

import jp.yksolution.android.sms.smsnotice.dao.MessageDao;

/**
 * メッセージ送信管理テーブルエンティティ.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class MessageEntity extends EntityBase {
    public static class PROC_ID {
        public static final int NEW_MESSAGE = 1;
        public static final int SELECT_MESSAGE = 2;
        public static final int SENT_MESSAGE = 3;
    }

    public static enum NOTICE_STATUS {
        IDLE, COMPLETED, RETRY, ERROR
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
    private int baseTime;
    public int getBaseTime() { return this.baseTime; }
    public void setBaseTime(int baseTime) { this.baseTime = baseTime; }

    /** 作成日時. */
    private String createDate;
    public String getCreateDate() { return this.createDate; }
    public void setCreateDate(String createDate) { this.createDate = createDate; }

    /** 更新日時 */
    private String updateDate;
    public String getUpdateDate() { return this.updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    /** 既読日時 */
    private String deliveredDate;
    public String getDeliveredDate() { return this.deliveredDate;}
    public void setDeliveredDate(String deliveredDate) { this.deliveredDate = deliveredDate; }

    /** メッセージエンティティ */
    private MessageEntity messageEntity;
    public MessageEntity getMessageEntity() { return this.messageEntity; }
    public void setMessageEntity(MessageEntity messageEntity) { this.messageEntity = messageEntity; }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getSimpleName());
        sb.append("(").append("Proc ID : ").append(this.mProcId)
                .append(", id : ").append(this.id)
                .append(", Phone No : ").append(this.phoneNo)
                .append(", Phone Name : ").append(this.phoneName)
                .append(", Message : ").append(this.message)
                .append(", status : ").append(this.status)
                .append(", create date : ").append(this.createDate)
                .append(", update date : ").append(this.updateDate)
                .append(", error message : ").append(this.errorMessage)
                .append(")");
        return sb.toString();
    }
}