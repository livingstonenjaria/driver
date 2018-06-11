package ke.co.struct.chauffeurdriver.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import ke.co.struct.chauffeurdriver.R;

public class NotificationHelper extends ContextWrapper {
    private static final String CHAUFFEUR_CHANNEL_ID = "ke.co.struct.chauffeurdriver.CHAUFFEUR";
    private static final String CHAUFFEUR_CHANNEL_NAME = "CHAUFFEUR";
    private NotificationManager manager;
    public NotificationHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannels();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel chauffeurChannel = new NotificationChannel(CHAUFFEUR_CHANNEL_ID,CHAUFFEUR_CHANNEL_NAME,NotificationManager.IMPORTANCE_DEFAULT);
        chauffeurChannel.enableLights(true);
        chauffeurChannel.enableVibration(true);
        chauffeurChannel.setLightColor(Color.GRAY);
        chauffeurChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(chauffeurChannel);
    }

    public NotificationManager getManager() {
        if (manager == null){
            manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getChauffeurNotification(String title, String content, PendingIntent pendingIntent, Uri soundUri){
        return new Notification.Builder(getApplicationContext(),CHAUFFEUR_CHANNEL_ID)
                .setContentText(content)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_car);
    }
}
