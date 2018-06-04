package ke.co.struct.chauffeurdriver.Remote;

import android.location.Location;

public class Common {
    public static final String drivers_available = "driversavailable";
    public static final String notifications = "notifications";
    public static final String baseUrl = "https://maps.googleapis.com";
    public static final String fcmURL = "https://fcm.googleapis.com";
    public static Location mLastLocation = null;
    public  static MGoogleApi getGoogleApi(){
        return RetrofitClient.getClient(baseUrl).create(MGoogleApi.class);
    }
    public  static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }
}
