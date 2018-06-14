package ke.co.struct.chauffeurdriver.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.HashMap;
import java.util.Map;

import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.model.DataMessage;
import ke.co.struct.chauffeurdriver.remote.Common;
import ke.co.struct.chauffeurdriver.remote.IFCMService;
import ke.co.struct.chauffeurdriver.remote.MGoogleApi;
import ke.co.struct.chauffeurdriver.model.Driver;
import ke.co.struct.chauffeurdriver.model.FCMResponse;
import ke.co.struct.chauffeurdriver.model.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideAlert extends AppCompatActivity {
    private MaterialEditText txtAddress, txtTime, txtDistance, txtName;
    MediaPlayer mediaPlayer;
    private static final String TAG = "RideAlert";
    private MGoogleApi mService;
    private IFCMService ifcmService;
    private Button btnAccept, btnDecline;
    private String riderid, phone, name, pushid;
    private Double lat,lng,destlat,destlng;
    private Driver driver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_alert);
        txtTime = findViewById(R.id.txtTime);
        txtName = findViewById(R.id.riderName);
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
            name = getIntent().getStringExtra("name");
            phone = getIntent().getStringExtra("phone");
            pushid = getIntent().getStringExtra("pushid");
            destlat = getIntent().getDoubleExtra("destlat", -1.0);
            destlng = getIntent().getDoubleExtra("destlng", -1.0);
            txtName.setText(name);
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
                intent.putExtra("name",name);
                intent.putExtra("phone",phone);
                intent.putExtra("pushid", pushid);
                intent.putExtra("destlat", destlat);
                intent.putExtra("destlng", destlng);
                startActivity(intent);
                finish();
            }
        });
    }

    private void sendDriverDetails(String riderid) {
        driver = Common.current_driver;
        Token token = new Token(riderid);
        JSONObject details = new JSONObject();
        try {
                details.put("name",driver.getName());
                details.put("phone", driver.getPhone());
                details.put("carType", driver.getCarType());
                details.put("carimg", driver.getCarimg());
                details.put("licPlate", driver.getLicPlate());
                details.put("ProfileImageUrl", driver.getProfileImageUrl());
                Log.d(TAG, "driverinfo: " + details);

        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Bundle bundle = new Bundle();
//        bundle.putString("json", details.toString());
        Map<String,String> content = new HashMap<>();
        content.put("title","Accepted");
        content.put("message",String.format("%s has accepted your request",Common.current_driver.getName()));
        content.put("name",driver.getName());
        content.put("phone", driver.getPhone());
        content.put("carType", driver.getCarType());
        content.put("carimg", driver.getCarimg());
        content.put("licPlate", driver.getLicPlate());
        content.put("ProfileImageUrl", driver.getProfileImageUrl());
        DataMessage dataMessage = new DataMessage(token.getToken(), content);
        ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
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
        Map<String,String> content = new HashMap<>();
        content.put("title","Cancelled");
        content.put("message","Driver has cancelled your request");
        DataMessage dataMessage = new DataMessage(token.getToken(), content);
        ifcmService.sendMessage(dataMessage)
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
//        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
//        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
//        mediaPlayer.start();
        super.onResume();
    }
}
