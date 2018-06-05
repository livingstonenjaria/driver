package ke.co.struct.chauffeurdriver.Activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ke.co.struct.chauffeurdriver.MainActivity;
import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.Remote.Common;
import ke.co.struct.chauffeurdriver.Remote.IFCMService;
import ke.co.struct.chauffeurdriver.Remote.MGoogleApi;
import ke.co.struct.chauffeurdriver.helper.DirectionsJSONParser;
import ke.co.struct.chauffeurdriver.model.FCMResponse;
import ke.co.struct.chauffeurdriver.model.Notification;
import ke.co.struct.chauffeurdriver.model.Sender;
import ke.co.struct.chauffeurdriver.model.Token;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTrackingActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    /*-------------------- CODES ----------------------------*/
    private static final String TAG = "DriverTrackingActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int PERMISSION_REQUEST_CODE = 7001;
    private static final int PLAY_SERVICE_RES_REQUEST = 7000;
    private static  int UPDATE_INTERVAL = 5000;
    private static  int FASTEST_INTERVAL = 3000;
    private static  int DISPLACEMENT = 10;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private Boolean mLocationPermissionGranted = false;
    private GeoFire geoFire, nearBy;
    private Marker mCurrent;
    private Circle riderMarker;
    DatabaseReference driverenroute;

    private GoogleMap mMap;
    private Double riderlat, riderlng;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private Polyline direction;

    private String userid,riderid;
    private MGoogleApi mService;
    private IFCMService ifcmService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        userid = Common.auth.getCurrentUser().getUid();
         driverenroute = Common.database.getReference(Common.drivers_enroute);
         geoFire = new GeoFire(driverenroute);
         ifcmService = Common.getFCMService();
         mService = Common.getGoogleApi();
        /*-----------Check Play Services------------------*/
        if (isServicesOK()) {
            getLocationPermission();
        }
        if (getIntent() != null){
            riderlat = getIntent().getDoubleExtra("lat", -1.0);
            riderlng = getIntent().getDoubleExtra("lng", -1.0);
            riderid = getIntent().getStringExtra("rider");
        }
        setUpLocation();
    }
    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving camera to lat: " + latLng.latitude + "lng: " + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        riderMarker = mMap.addCircle(new CircleOptions()
                .center(new LatLng(riderlat, riderlng))
                .radius(50)
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));
        nearBy = new GeoFire(driverenroute);
        GeoQuery geoQuery = nearBy.queryAtLocation(new GeoLocation(riderlat,riderlng),0.05f);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                sendDriverArrivedNotification(riderid);
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(DriverTrackingActivity.this, R.raw.mapstyle));

            if (!success) {

            }
        } catch (Resources.NotFoundException e) {

        }

    }

    private void sendDriverArrivedNotification(String riderid) {
        Token token = new Token(riderid);
        Notification notification = new Notification("Arrived", String.format("Your driver %s has arrived at your location",Common.current_driver.getName()));
        Sender sender = new Sender(token.getToken(), notification);
        ifcmService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success != 1){
                    Toast.makeText(DriverTrackingActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void initMap(){
        Log.d(TAG, "initMap: Initializing map");
        SupportMapFragment  mapFragment  = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: Getting location permission");
        String [] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                initMap();
            } else{
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    public  boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DriverTrackingActivity.this);
        if(available == ConnectionResult.SUCCESS){
            // everything is ok and user can make map requests
            Log.d(TAG, "isServicesOK: Google play services is working");
            return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: An error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DriverTrackingActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else{
            Toast.makeText(this, "Sorry you can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Called");
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if(grantResults.length > 0 ){
                    for(int i = 0;  i < grantResults.length; i++){
                        if(grantResults [i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    initMap();
                }
        }
    }
    /*-----------Set Up Location------------------*/
    private void setUpLocation() {
        Log.d(TAG, "setUpLocation: setup started");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,  new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            if (isServicesOK()){
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }
    /*-----------Location Requests------------------*/
    private void createLocationRequest() {
        Log.d(TAG, "createLocationRequest: Requesting locations");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }
    /*----------- Google API Client------------------*/
    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    /*-----------Show Location in Map------------------*/
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.mLastLocation != null) {
            Log.d(TAG, "displayLocation: displaying location");

                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();

                // Update location in firebase
                geoFire.setLocation(userid, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mCurrent != null) {
                            mCurrent.remove();
                            mCurrent = mMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.car))
                                    .position(new LatLng(latitude,longitude)));
                            moveCamera(new LatLng(latitude,longitude), DEFAULT_ZOOM);
                            Common.rotateMarker(mCurrent, -360, mMap);
                        }else{
                            mCurrent = mMap.addMarker(new MarkerOptions()
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.car))
                                    .position(new LatLng(latitude,longitude)));
                            moveCamera(new LatLng(latitude,longitude), DEFAULT_ZOOM);
                            Common.rotateMarker(mCurrent, -360, mMap);
                        }
                    }
                });
                if (direction != null)
                    direction.remove();

            getDirection();
        }
        else{
            Log.d(TAG, "displayLocation: Cannot get location");
        }
    }
    /*-----------Get directions to rider------------------*/

    private void getDirection() {
        LatLng currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+riderlat+","+riderlng+"&"+
                    "key="+getResources().getString(R.string.google_maps_API);
            Log.d(TAG, "getDirection:  "+requestApi);
            mService.getPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        new ParserTask().execute(response.body().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(DriverTrackingActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "startLocationUpdates: start loc updates");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>>{
        ProgressDialog mDialog = new ProgressDialog(DriverTrackingActivity.this);
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage("Waiting....");
            mDialog.show();
        }
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();
            ArrayList points = null;
            PolylineOptions polylineOptions = null;
            for (int i = 0; i < lists.size(); i++)
            {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();
                List<HashMap<String,String>> path = lists.get(i);
                for (int j = 0; j < path.size(); j++){
                    HashMap<String,String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat,lng);
                    points.add(position);
                }
                polylineOptions.addAll(points);
                polylineOptions.width(5);
                polylineOptions.jointType(JointType.ROUND);
                polylineOptions.endCap(new RoundCap());
                polylineOptions.startCap(new RoundCap());
                polylineOptions.color(Color.BLACK);
                polylineOptions.geodesic(true);
            }
            direction = mMap.addPolyline(polylineOptions);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            if(mCurrent != null && riderMarker != null){
                //the include method will calculate the min and max bound.
                builder.include(mCurrent.getPosition());
                builder.include(riderMarker.getCenter());

                LatLngBounds bounds = builder.build();

                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.20); // offset from edges of the map 10% of screen

                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

                mMap.animateCamera(cu);

            }

        }
    }
}
