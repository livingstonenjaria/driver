package ke.co.struct.chauffeurdriver.Service;

import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import ke.co.struct.chauffeurdriver.Activities.RideAlert;

public class MyFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        LatLng rider_location = new Gson().fromJson(remoteMessage.getNotification().getBody(), LatLng.class);
        Intent intent = new Intent(getBaseContext(), RideAlert.class);
        intent.putExtra("lat", rider_location.latitude);
        intent.putExtra("lng", rider_location.longitude);
        intent.putExtra("rider",remoteMessage.getNotification().getTitle());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void sendnotification(LatLng rider_location) {

    }
}
