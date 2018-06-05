package ke.co.struct.chauffeurdriver.Remote;

import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import ke.co.struct.chauffeurdriver.model.Driver;

public class Common {
    public static final String drivers_available = "driversavailable";
    public static final String drivers_enroute = "driversenroute";
    public static final String notifications = "notifications";
    public static final String baseUrl = "https://maps.googleapis.com";
    public static final String fcmURL = "https://fcm.googleapis.com";
    public static Driver current_driver;
    public static FirebaseAuth auth = FirebaseAuth.getInstance();
    public static FirebaseDatabase database = FirebaseDatabase.getInstance();
    public static Location mLastLocation = null;
    public  static MGoogleApi getGoogleApi(){
        return RetrofitClient.getClient(baseUrl).create(MGoogleApi.class);
    }
    public  static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }
    public static void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation =  mCurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float)elapsed/duration);
                float rot = t * i + (1 - t) *startRotation;
                mCurrent.setRotation(-rot > 180?rot/2:rot);
                if (t<1.0){
                    handler.postDelayed(this, 16);
                }
            }
        });
    }
}
