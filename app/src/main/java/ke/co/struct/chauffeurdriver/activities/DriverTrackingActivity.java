package ke.co.struct.chauffeurdriver.activities;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.model.DataMessage;
import ke.co.struct.chauffeurdriver.model.GetPlace;
import ke.co.struct.chauffeurdriver.remote.Common;
import ke.co.struct.chauffeurdriver.remote.IFCMService;
import ke.co.struct.chauffeurdriver.remote.MGoogleApi;
import ke.co.struct.chauffeurdriver.helper.DirectionsJSONParser;
import ke.co.struct.chauffeurdriver.model.FCMResponse;
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
    private Boolean rideStarted = false;
    private GeoFire geoFire, nearBy;
    private Marker mCurrent;
    private Circle riderMarker;
    DatabaseReference driverenroute;

    private GoogleMap mMap;
    private Double riderlat, riderlng;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Double trip = 0.0;
    private Polyline direction;

    private String userid,riderid,phone,pushid;
    private MGoogleApi mService;
    private IFCMService ifcmService;
    private FloatingActionButton fabcall, fabcancel;
    private Button startride,endride;
    private Double  lat,lng;
    String startAddress, endAddress, actualPickup;



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
            lat = getIntent().getDoubleExtra("destlat", -1.0);
            lng = getIntent().getDoubleExtra("destlng", -1.0);
            riderid = getIntent().getStringExtra("rider");
            phone = getIntent().getStringExtra("phone");
            pushid = getIntent().getStringExtra("pushid");
            getRiderRequestInfo(pushid,riderid);

        }
        fabcall = findViewById(R.id.fabcall);
        fabcancel = findViewById(R.id.fabcancel);
        startride = findViewById(R.id.ridestart);
        endride = findViewById(R.id.rideend);

        fabcall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callRider();
            }
        });
        fabcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRide();
            }
        });
        startride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lat = Common.mLastLocation.getLatitude();
                lng = Common.mLastLocation.getLongitude();
                startRide();
                startride.setVisibility(View.GONE);
                endride.setVisibility(View.VISIBLE);
                fabcall.setVisibility(View.GONE);
                fabcancel.setVisibility(View.GONE);
            }
        });
        endride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endRide();
                endride.setVisibility(View.GONE);
                mMap.clear();
            }
        });
        setUpLocation();
    }

    private void startRide() {
        if(riderMarker !=null ){
            riderMarker.remove();
        }
        rideStarted = true;

        DatabaseReference rideStart = Common.database.getReference().child("ridestarted").child(pushid).child(userid).child(riderid);
        HashMap<String,Object> map = new HashMap<>();
        map.put("destination",endAddress);
        map.put("setpickUp",startAddress);
        map.put("actualpickUp", actualPickup);
        rideStart.updateChildren(map);
//        DatabaseReference driverEnrouteRef = Common.database.getReference().child("driversenroute");
//        GeoFire geoFire = new GeoFire(driverEnrouteRef);
//        geoFire.removeLocation(userid);
//        DatabaseReference matchConsent = Common.database.getReference("matchConsent").child(userid).child(riderid);
//        matchConsent.removeValue();
        erasepolylines();
        getDirection(lat,lng);
    }

    private void erasepolylines() {
            if (direction != null) {
                direction.remove();
            }

    }

    private void cancelRide() {
    }

    private void callRider() {
        if (phone != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
            startActivity(intent);
        }
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
//                sendDriverArrivedNotification(riderid);
                startride.setVisibility(View.VISIBLE);
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
        Map<String,String> content = new HashMap<>();
        content.put("title","Arrived");
        content.put("message",String.format("Your driver %s has arrived at your location",Common.current_driver.getName()));
        DataMessage dataMessage = new DataMessage(token.getToken(), content);
        ifcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
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

            getDirection(riderlat, riderlng);
        }
        else{
            Log.d(TAG, "displayLocation: Cannot get location");
        }
    }
    /*-----------Get directions to rider------------------*/

    private void getDirection(Double lat, Double lng) {
        LatLng currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+lat+","+lng+"&"+
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
        if (Common.mLastLocation != null){
            getLocationInfo(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
        }
        if (rideStarted.equals(true)){
            Common.trip += Common.mLastLocation.distanceTo(location)/1000;
            DatabaseReference rideStarted = Common.database.getReference().child("rideOngoing");
            GeoFire geoRide = new GeoFire(rideStarted);
            geoRide.setLocation(pushid,new GeoLocation(Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude()));
        }
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
    private void getRiderRequestInfo(String pushId, String riderId){
        DatabaseReference riderLoc = Common.database.getReference().child("riderRequest").child(pushId).child(riderId);
        riderLoc.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0)
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("riderLocation") != null) {
                        startAddress = map.get("riderLocation").toString();
                    }
                    if (map.get("riderDestination") != null) {
                        endAddress = map.get("riderDestination").toString();
                    }
//                    if (map.get("paymentMethod") != null) {
//                        mPayment = map.get("paymentMethod").toString();
//                        paymentMethod.setText(mPayment);
//                    }
//                    if (map.get("destinationLat") != null) {
//                        destinationLat = Double.parseDouble(map.get("destinationLat").toString());
//                    }
//                    if (map.get("destinationLng") != null) {
//                        destinationLng =  Double.parseDouble(map.get("destinationLng").toString());
//                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
//        DatabaseReference riderLocCoords = Common.database.getReference().child("riderRequest").child(pushId).child(riderid).child("l");
//        riderLocCoords.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0)
//                {
//                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
//
//                    if (map.get("0") != null) {
//                        rider_lat = Double.parseDouble(map.get("0").toString());
//
//                    }
//                    if (map.get("1") != null) {
//                        rider_lng = Double.parseDouble(map.get("1").toString());
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(DatabaseError databaseError) {}
//        });
    }
       /*-----------------Current Location information-----------*/
    public void getLocationInfo(Double newLat, Double newLng) {
        String placeUrl;
        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?");
        googlePlaceUrl.append("latlng=" + newLat + "," + newLng);
        //googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key=" + "AIzaSyBvfzFgDQZQjThxa3MgCUczuKTreOWsAkk");
        //googlePlaceUrl.append("&key=" + "AIzaSyAJIa-FpquMtKsHGpDJ9uOtHlXK9wa9XVI");
        placeUrl = googlePlaceUrl.toString();
        //String to place our result in
        String result;

        //Instantiate new instance of our class
        GetPlace getRequest = new GetPlace();

        //Perform the doInBackground method, passing in our url
        try {
            result = getRequest.execute(placeUrl).get();
            getAddress(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public  void getAddress(String jsonResponse){
        try {
            if (jsonResponse != null) {
                JSONObject ret = new JSONObject(jsonResponse);
                JSONObject location = ret.getJSONArray("results").getJSONObject(0);
                String location_string = location.getString("formatted_address");
                String address = location_string;
                if (address!=null){
                    actualPickup = address;
                }
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
    private  void endRide(){
        rideStarted = false;
        DatabaseReference rideComplete = Common.database.getReference().child("rideCompleted");
        HashMap map = new HashMap();
        map.put("actualPickup", startAddress);
        map.put("requestLocation",startAddress);
        map.put("desired_destination",endAddress);
        map.put("dropOff",actualPickup);
        map.put("driver",userid);
        map.put("rider",riderid);
        map.put("distance",Common.trip);
        map.put("drivername",Common.current_driver.getName());
        rideComplete.child(pushid).updateChildren(map);
//        DatabaseReference complete = Common.database.getReference().child("completedRidecordinates");
//        GeoFire geoFire = new GeoFire(complete);
//        geoFire.setLocation(pushid, new GeoLocation(Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude()));
//        DatabaseReference rideEnd = Common.database.getReference().child("rideOngoing").child(pushid);
//        rideEnd.removeValue();
        recordRideHistory();
    }
    /*---------------------finish ride end---------------------*/

    /*---------------------Record History --------------------*/
    private void recordRideHistory(){
        DatabaseReference driverHistoryRef = Common.database.getReference().child("Users").child("Drivers").child(userid).child("rideHistory");
        DatabaseReference riderHistoryRef = Common.database.getReference().child("Users").child("Riders").child(userid).child("rideHistory");
        DatabaseReference rideHistory = Common.database.getReference().child("rideHistory");
        String requestID = rideHistory.push().getKey();
        driverHistoryRef.child(pushid).setValue(true);
        riderHistoryRef.child(pushid).setValue(true);
        HashMap map = new HashMap();
        map.put("driver",userid);
        map.put("rider",riderid);
        map.put("drivername",Common.current_driver.getName());
//        map.put("riderRating",mRating);
        map.put("from",startAddress);
        map.put("to",endAddress);
        map.put("pickupLat",riderlat);
        map.put("pickupLng",riderlng);
//        map.put("dropoffLat",);
//        map.put("dropoffLng",dropoffLng);
        map.put("timestamp",getCurrentTimestamp());
        map.put("status","Ride Completed Successfully");
        rideHistory.child(pushid).updateChildren(map);
    }

    private Long getCurrentTimestamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }
    /*----------------end of record history--------------------*/

    /*----------------display ride complete dialog-----------------------*/
    private void diplayRideCompleteDialog(String content) {

    }
    /*-----------------end display dialog--------------------------------*/



    /*---------------------------------------------------------------------------------------------------------------------------------------------------*/

}
