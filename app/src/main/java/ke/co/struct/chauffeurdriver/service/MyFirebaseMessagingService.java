package ke.co.struct.chauffeurdriver.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.activities.RideAlert;
import ke.co.struct.chauffeurdriver.helper.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private String title, body;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        title = remoteMessage.getNotification().getTitle();
        body = remoteMessage.getNotification().getBody();
        LatLng rider_location = new Gson().fromJson(remoteMessage.getNotification().getBody(), LatLng.class);
        Intent intent = new Intent(getBaseContext(), RideAlert.class);
        intent.putExtra("lat", rider_location.latitude);
        intent.putExtra("lng", rider_location.longitude);
        intent.putExtra("rider",remoteMessage.getNotification().getTitle());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private  void showNotificationsAPI26(String message){
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(),0,new Intent(),PendingIntent.FLAG_ONE_SHOT);
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/drivernotification");
        NotificationHelper notificationHelper = new NotificationHelper(getBaseContext());
        Notification.Builder builder = notificationHelper.getChauffeurNotification(title,message,pendingIntent,sound);
        notificationHelper.getManager().notify(1,builder.build());

    }
    private void showNotification(String body) {
        Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + getPackageName() + "/raw/drivernotification");
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(),0,new Intent(),PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder builder = new Notification.Builder(getBaseContext());
        builder.setSmallIcon(R.drawable.ic_car)
                .setWhen(System.currentTimeMillis())
                .setSound(sound)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1,builder.build());
    }

}
