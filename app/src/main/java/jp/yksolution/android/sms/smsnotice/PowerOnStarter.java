package jp.yksolution.android.sms.smsnotice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import jp.yksolution.android.sms.smsnotice.services.EventService;

/**
 * Android端末の再起動で呼ばれるクラス.
 * @author Y.Katou (YKSolution)
 * @since 0.0.1
 */
public class PowerOnStarter extends BroadcastReceiver {
    /**
     * Android端末の再起動でサービスの起動を行う.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, EventService.class));
    }
}