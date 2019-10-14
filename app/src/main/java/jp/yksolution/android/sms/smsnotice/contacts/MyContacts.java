package jp.yksolution.android.sms.smsnotice.contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 電話帳データ取得クラス
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class MyContacts {
    private final static String TAG = MyContacts.class.getSimpleName();
    private final Context context;
    public MyContacts(Context context) {
        this.context = context;
    }

    /**
     * 電話帳データエンティティ.
     */
    public static class Entity {
        /** id. */
        private final String id;
        public Entity(String id) { this.id = id; }
        public String getId() { return id; }

        /** 電話番号 */
        private String phoneNo;
        public String getPhoneNo() { return this.phoneNo; }
        public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

        /** 氏名. */
        private String name;
        public String getName() { return this.name; }
        public void setName(String name) { this.name = name; }

        /** メモ. */
        private String note;
        public String getNote() { return this.note; }
        public void setNote(String note) { this.note = note; }

        /** 会社名. */
        private String company;
        public String getCompany() { return this.company; }
        public void setCompany(String company) { this.company = company; }

        @Override
        public String toString() {
            StringBuilder string = new StringBuilder();
            string.append("name : ").append(this.name);
            string.append(", phone No : ").append(this.phoneNo);
            string.append(", campany : ").append(this.company);
            string.append(", note : ").append(this.note);
            return string.toString();
        }

        /**
         * 一覧出力用
         * @return エンティティ情報
         */
        public String toListString() {
            StringBuilder string = new StringBuilder(this.phoneNo);
            string.append(" (").append(this.name).append((")"));
            return string.toString();
        }
    }
    private final String[] projection = new String[]{
              ContactsContract.Data.CONTACT_ID
            , ContactsContract.Data.MIMETYPE
            , ContactsContract.CommonDataKinds.StructuredName.DATA1
            , ContactsContract.CommonDataKinds.Phone.NUMBER
            , ContactsContract.CommonDataKinds.Organization.COMPANY
            , ContactsContract.CommonDataKinds.Note.DATA1
    };

    public Map<String, Entity> editContacts() {
        Map<String, Entity> entityMap = new HashMap<>();
        try {
            Cursor cursor = this.context.getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI
                    , projection
                    , null, null, null);
            while (cursor.moveToNext()) {
                int index = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
                String contactId = cursor.getString(index);
                if (contactId == null) {
                    Log.e(TAG, ContactsContract.Data.MIMETYPE + " is null");
                    continue;
                }

                Entity entity = entityMap.get(contactId);
                if(entity == null) {
                    entity = new Entity(contactId);
                    entityMap.put(contactId, entity);
//                    Log.i(TAG,"new contact_id : " + contactId);
                }

                index = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
                String mimeType = cursor.getString(index);
                if (mimeType.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
                    index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DATA1);
                    entity.setName(cursor.getString(index));
                } else if (mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                    index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    entity.setPhoneNo(cursor.getString(index));
                } else if (mimeType.equals(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)) {
                    index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY);
                    entity.setCompany(cursor.getString(index));
                } else if (mimeType.equals(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)) {
                    index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.DATA1);
                    entity.setNote(cursor.getString(index));
                } else {
                    Log.i(TAG, "passed" + mimeType);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception : " + ex.toString());
        }

        return entityMap;
    }
}