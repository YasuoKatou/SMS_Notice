package jp.yksolution.android.sms.smsnotice.contacts;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class MyContacts {
    private final static String TAG = MyContacts.class.getSimpleName();
    private final Context context;
    public MyContacts(Context context) {
        this.context = context;
    }

    public static class Entity {
        private final String id;
        private String phoneNo;
        private String name;
        private String note;

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        private String company;

        public Entity(String id) {
            this.id = id;
        }

        public void setPhoneNo(String phoneNo) {
            this.phoneNo = phoneNo;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getId() {
            return id;
        }

        public String getPhoneNo() {
            return phoneNo;
        }

        public String getName() {
            return name;
        }

        public String getNote() {
            return note;
        }

        @Override
        public String toString() {
            StringBuilder string = new StringBuilder();
            string.append("name : ").append(this.name);
            string.append(", phone No : ").append(this.phoneNo);
            string.append(", campany : ").append(this.company);
            string.append(", note : ").append(this.note);
            return string.toString();
        }

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