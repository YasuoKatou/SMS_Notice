package jp.yksolution.android.sms.smsnotice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

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
        Log.d("PowerOnStarter", "onReceive");
        Intent svcIntent = new Intent(context, EventService.class);
        if (Build.VERSION.SDK_INT <= 25) {
            Log.d("PowerOnStarter", "startService");
            context.startService(svcIntent);
        } else {
            Log.d("PowerOnStarter", "startForegroundService");
            context.startForegroundService(svcIntent);
        }
    }
}