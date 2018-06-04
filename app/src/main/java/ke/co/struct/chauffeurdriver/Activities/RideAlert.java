package ke.co.struct.chauffeurdriver.Activities;

import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import ke.co.struct.chauffeurdriver.MainActivity;
import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.Remote.Common;
import ke.co.struct.chauffeurdriver.Remote.IFCMService;
import ke.co.struct.chauffeurdriver.Remote.MGoogleApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideAlert extends AppCompatActivity {
    private TextView txtAddress, txtTime, txtDistance;
    MediaPlayer mediaPlayer;
    private static final String TAG = "RideAlert";
    private MGoogleApi mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_alert);
        txtTime = findViewById(R.id.txtTime);
        txtAddress = findViewById(R.id.txtAddress);
        txtDistance = findViewById(R.id.txtDistance);
        mediaPlayer = MediaPlayer.create(this, R.raw.drivernotification);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if (getIntent() != null){
            double lat = getIntent().getDoubleExtra("lat", -1.0);
            double lng = getIntent().getDoubleExtra("lng", -1.0);
            getDirection(lat, lng);
        }
    }
    private void getDirection(double lat, double lng) {
        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+ Common.mLastLocation.getLatitude() +","+Common.mLastLocation.getLongitude()+"&"+
                    "destination="+lat+","+lng+"&"+
                    "key="+getResources().getString(R.string.google_maps_API);
            Log.d(TAG, "getDirection:  "+requestApi);
            mService.getPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray routes = jsonObject.getJSONArray("routes");
                        JSONObject object = routes.getJSONObject(0);
                        JSONArray legs = object.getJSONArray("legs");
                        JSONObject legsobject = legs.getJSONObject(0);
                        JSONObject distance = legsobject.getJSONObject("distance");
                        txtDistance.setText(distance.getString("text"));

                        //Time
                        JSONObject time = legsobject.getJSONObject("duration");
                        txtTime.setText(time.getString("text"));

                        //Address
                        String address = legsobject.getString("end_address");
                        txtAddress.setText(address);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(RideAlert.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mediaPlayer.start();
        super.onResume();
    }
}
