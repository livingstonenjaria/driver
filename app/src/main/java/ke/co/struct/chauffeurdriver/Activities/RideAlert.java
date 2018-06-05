package ke.co.struct.chauffeurdriver.Activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
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
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import ke.co.struct.chauffeurdriver.MainActivity;
import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.Remote.Common;
import ke.co.struct.chauffeurdriver.Remote.IFCMService;
import ke.co.struct.chauffeurdriver.Remote.MGoogleApi;
import ke.co.struct.chauffeurdriver.model.Driver;
import ke.co.struct.chauffeurdriver.model.FCMResponse;
import ke.co.struct.chauffeurdriver.model.Notification;
import ke.co.struct.chauffeurdriver.model.Sender;
import ke.co.struct.chauffeurdriver.model.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideAlert extends AppCompatActivity {
    private TextView txtAddress, txtTime, txtDistance;
    MediaPlayer mediaPlayer;
    private static final String TAG = "RideAlert";
    private MGoogleApi mService;
    private IFCMService ifcmService;
    private Button btnAccept, btnDecline;
    private String riderid;
    private Double lat,lng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_alert);
        txtTime = findViewById(R.id.txtTime);
        txtAddress = findViewById(R.id.txtAddress);
        txtDistance = findViewById(R.id.txtDistance);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);

        mediaPlayer = MediaPlayer.create(this, R.raw.drivernotification);
        mediaPlayer.setLooping(false);
        mediaPlayer.start();

        ifcmService = Common.getFCMService();
        mService = Common.getGoogleApi();
        if (getIntent() != null){
             lat = getIntent().getDoubleExtra("lat", -1.0);
             lng = getIntent().getDoubleExtra("lng", -1.0);
            riderid = getIntent().getStringExtra("rider");
            getDirection(lat, lng);
        }
        btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(riderid)){
                    cancelBooking(riderid);
                }
            }
        });
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDriverDetails(riderid);
                Intent intent = new Intent(RideAlert.this, DriverTrackingActivity.class);
                intent.putExtra("lat",lat);
                intent.putExtra("lng",lng);
                intent.putExtra("rider",riderid);
                startActivity(intent);
                finish();
            }
        });
    }

    private void sendDriverDetails(String riderid) {
        Token token = new Token(riderid);
        String details = new Gson().toJson(Driver.class);
        Notification notification = new Notification("Accepted", details);
        Sender sender = new Sender(token.getToken(), notification);
        ifcmService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success != 1){
                    Toast.makeText(RideAlert.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void cancelBooking(String riderid) {
        Token token = new Token(riderid);
        Notification notification = new Notification("Cancelled", "Driver has cancelled your request");
        Sender sender = new Sender(token.getToken(), notification);
        ifcmService.sendMessage(sender)
                .enqueue(new retrofit2.Callback<FCMResponse>() {
                    @Override
                    public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                        if (response.body().success == 1){
                            Toast.makeText(RideAlert.this, "Cancelled", Toast.LENGTH_SHORT).show();
                            finish();
                        }else{
                            Toast.makeText(RideAlert.this, "Failed to cancel", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<FCMResponse> call, Throwable t) {
                        Log.e(TAG, "onFailure:  "+t.getMessage() );
                    }
                });
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
