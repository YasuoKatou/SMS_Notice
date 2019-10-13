package jp.yksolution.android.sms.smsnotice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import jp.yksolution.android.sms.smsnotice.services.ServiceMain;

public class PowerOnStarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ServiceMain.class));
    }
}
