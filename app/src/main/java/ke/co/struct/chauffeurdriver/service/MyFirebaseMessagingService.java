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
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.activities.PaymentActivity;
import ke.co.struct.chauffeurdriver.activities.RideAlert;
import ke.co.struct.chauffeurdriver.helper.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMessagingServ";
    private String title, body,ridertoken;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0){
            title = remoteMessage.getData().get("title");
            Map<String,String> payload = remoteMessage.getData();
            body = remoteMessage.getData().get("message");
            Log.d(TAG, "onMessageReceived: ");
            showNotification(body);
            if(title.equals("Ride Request")){
                LatLng rider_location = new Gson().fromJson(remoteMessage.getData().get("location"), LatLng.class);
                LatLng destination = new Gson().fromJson(remoteMessage.getData().get("destination"), LatLng.class);
                String name = remoteMessage.getData().get("name");
                String phone = remoteMessage.getData().get("phone");
                String ridertoken = remoteMessage.getData().get("ridertoken");
                String riderid = remoteMessage.getData().get("riderid");
                String pushid = remoteMessage.getData().get("pushid");
                Intent intent = new Intent(getBaseContext(), RideAlert.class);
                intent.putExtra("lat", rider_location.latitude);
                intent.putExtra("lng", rider_location.longitude);
                intent.putExtra("destlat", destination.longitude);
                intent.putExtra("destlng", destination.longitude);
                intent.putExtra("ridertoken",ridertoken);
                intent.putExtra("rider",riderid);
                intent.putExtra("pushid",pushid);
                intent.putExtra("name",name);
                intent.putExtra("phone",phone);

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            if (title.equals("Total Ride Cost")) {
                String title = payload.get("title");
                String content = payload.get("content");
                String refnum = payload.get("refnum");
                String pushid = payload.get("pushid");
                Intent intent = new Intent(getApplicationContext(), PaymentActivity.class);
                intent.putExtra("title",title);
                intent.putExtra("content",content);
                intent.putExtra("refnum",refnum);
                intent.putExtra("pushid",pushid);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                showNotification(content);
            }
        }
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
