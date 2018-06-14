package ke.co.struct.chauffeurdriver;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import ke.co.struct.chauffeurdriver.activities.DriverEarnings;
import ke.co.struct.chauffeurdriver.remote.Common;
import ke.co.struct.chauffeurdriver.remote.MGoogleApi;
import ke.co.struct.chauffeurdriver.activities.SettingsActivity;
import ke.co.struct.chauffeurdriver.service.MyFirebaseInstanceIdService;
import ke.co.struct.chauffeurdriver.model.Driver;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static ke.co.struct.chauffeurdriver.remote.Common.auth;

public class DriverHomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback {
    /*-------------------- CODES ----------------------------*/
    private static final String TAG = "DriverHomeActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int PERMISSION_REQUEST_CODE = 7001;
    private static final int PLAY_SERVICE_RES_REQUEST = 7000;
    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;
    private static final float DEFAULT_ZOOM = 15f;
    private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    /*----------------------Firebase and Geofire -----------------*/
    private GeoFire geoFire;
    private String userid;
    DatabaseReference drivers, onlineRef, currentUserRef;

    /*------------------Map Variables-----------------------*/
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationCallback locationCallback;
    private ToggleButton location_switch;
    private Boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Marker mCurrent;
    private PolylineOptions polylineOptions, bluePolylineOptions;
    private Polyline bluePolyline, greenPolyline;
    private MGoogleApi mService;
    private List<LatLng> polyLineList;
    private float v;
    private double lat, lng;
    private LatLng startPosition, endPosition, currentPosition;

    /*----------------Variables-----------------*/

    private Handler handler;
    private int index, next;
    android.app.Dialog waitingDialog;
    EditText old_pwd, new_pwd, repeat_pwd;
    private String error;
    private CoordinatorLayout homeRoot;
    private CircleImageView profile_pic;
    private TextView driver_name, driver_email;
    private static Bundle bundle = new Bundle();
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/PTMono.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
        setContentView(R.layout.activity_driver_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        userid = auth.getCurrentUser().getUid();
        updateFirebaseToken();
        driverinfo();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = navigationView.getHeaderView(0);
        driver_name = navigationHeaderView.findViewById(R.id.driver_name);
        driver_email = navigationHeaderView.findViewById(R.id.driver_email);
        profile_pic = navigationHeaderView.findViewById(R.id.profile_pic);

        location_switch = findViewById(R.id.location_switch);
        homeRoot = findViewById(R.id.homeRoot);
        polyLineList = new ArrayList<>();
        mService = Common.getGoogleApi();
        drivers = Common.database.getReference(Common.drivers_available);
        geoFire = new GeoFire(drivers);
        handler = new Handler();

        /*-----------Check Play Services------------------*/
        if (isServicesOK()) {
            getLocationPermission();
        }
        /*------------------Presence System-----------------*/
        onlineRef = Common.database.getReference().child(".info/connected");
        currentUserRef = Common.database.getReference(Common.drivers_available).child(userid);
        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        if (mLocationPermissionGranted) {
            location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        // getDeviceLocation();
                        SharedPreferences sharedPreferences = PreferenceManager
                                .getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("toggleButton", location_switch.isChecked());
                        editor.commit();
                        if (ActivityCompat.checkSelfPermission(DriverHomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DriverHomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        buildLocationRequests();
                        buildLocationCallback();
                        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
                        Common.database.goOnline();
                        displayLocation();

                    } else {
                        SharedPreferences sharedPreferences = PreferenceManager
                                .getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("toggleButton", location_switch.isChecked());
                        editor.commit();
                        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        Common.database.goOffline();
                        if (mCurrent != null) {
                            mCurrent.remove();
                        }
                        if (mMap != null) {
                            mMap.clear();
                        }
                        if (handler != null){
                            handler.removeCallbacks(drawPathRunnable);
                        }
                    }
                }
            });
        }
        if(savedInstanceState !=null ){

                location_switch.setChecked(savedInstanceState.getBoolean("ToggleButtonState", false));

        }
        setUpLocation();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.driver_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (id == R.id.nav_earnings) {
                    Intent intent = new Intent(DriverHomeActivity.this,DriverEarnings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                }
//                else if (id == R.id.nav_history) {
//
//                }
                else if (id == R.id.nav_settings) {
                    Intent intent = new Intent(DriverHomeActivity.this, SettingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);

                } else if (id == R.id.nav_help) {

                } else if (id == R.id.nav_changepwd) {
                    showDialogChangePwd();

                } else if (id == R.id.nav_logout) {
                    signOut();
                }
            }
        }, 200);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showDialogChangePwd() {
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(DriverHomeActivity.this);
        alertdialog.setTitle(R.string.change_password);
        alertdialog.setMessage(R.string.please_fill_all);

        LayoutInflater inflater = this.getLayoutInflater();
        View layout_pwd = inflater.inflate(R.layout.layout_change_pwd, null);
        old_pwd = layout_pwd.findViewById(R.id.change_pwd);
        new_pwd = layout_pwd.findViewById(R.id.new_pwd);
        repeat_pwd = layout_pwd.findViewById(R.id.repeat_pwd);
        alertdialog.setView(layout_pwd);
        alertdialog.setPositiveButton(getResources().getString(R.string.change_password), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                waitingDialog = new SpotsDialog(DriverHomeActivity.this, getString(R.string.changing_pwd));
                waitingDialog.setCancelable(false);
                waitingDialog.show();
                String pwd = old_pwd.getText().toString().trim();
                final String newpwd = new_pwd.getText().toString().trim();
                String repeatpwd = repeat_pwd.getText().toString().trim();
                if (!TextUtils.isEmpty(pwd) && !TextUtils.isEmpty(newpwd) && !TextUtils.isEmpty(repeatpwd)) {
                    if (newpwd.equals(repeatpwd)) {
                        String mail = Common.auth.getCurrentUser().getEmail();
                        AuthCredential authCredential = EmailAuthProvider.getCredential(mail, pwd);
                        Common.auth.getCurrentUser().reauthenticate(authCredential).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                waitingDialog.dismiss();
                                if (task.isSuccessful()) {
                                    Common.auth.getCurrentUser().updatePassword(newpwd)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    waitingDialog.dismiss();
                                                    if (task.isSuccessful()) {
                                                        Snackbar.make(homeRoot, "Password changed successfully", Snackbar.LENGTH_LONG).show();
                                                    } else {
                                                        Toast.makeText(DriverHomeActivity.this, R.string.failed_to_update_pwd, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            waitingDialog.dismiss();
                                            error = e.getMessage();
                                            erroralert(error);
                                        }
                                    });
                                } else {
                                    Toast.makeText(DriverHomeActivity.this, R.string.failed_to_authenticate, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                waitingDialog.dismiss();
                                error = e.getMessage();
                                erroralert(error);
                            }
                        });

                    } else {
                        waitingDialog.dismiss();
                        error = getString(R.string.password_mismatch);
                        erroralert(error);
                    }
                } else {
                    waitingDialog.dismiss();
                    error = getString(R.string.please_fill_all);
                    erroralert(error);
                }
            }
        });
        alertdialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertdialog.show();
    }

    private void signOut() {
        auth.signOut();
        Intent intent = new Intent(DriverHomeActivity.this, LandingPage.class);
        startActivity(intent);
        finish();
    }

    /*----------------Get driver Information----------------*/
    private void driverinfo() {
        Common.database.getReference().child("Users").child("Drivers").child(userid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Common.current_driver = dataSnapshot.getValue(Driver.class);
                        if (!TextUtils.isEmpty(Common.current_driver.getName())) {
                            driver_name.setText(Common.current_driver.getName());
                        }
                        if (!TextUtils.isEmpty(Common.auth.getCurrentUser().getEmail())) {
                            driver_email.setText(Common.auth.getCurrentUser().getEmail());
                        }

                        if (Common.current_driver.getProfileImageUrl() != null && !TextUtils.isEmpty(Common.current_driver.getProfileImageUrl())) {
                            Picasso.get()
                                    .load(Common.current_driver.getProfileImageUrl())
                                    .resize(200, 200)
                                    .centerCrop()
                                    .into(profile_pic);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


    }


    private void updateFirebaseToken() {
        MyFirebaseInstanceIdService myFirebaseInstanceIdService = new MyFirebaseInstanceIdService();
        myFirebaseInstanceIdService.updateTokenToServer(FirebaseInstanceId.getInstance().getToken());
    }

    /*-----------Get directions to rider------------------*/

    private void getDirection() {
        currentPosition = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
        String requestApi = null;
        try {
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&" +
                    "destination=" + "&" +
                    "key=" + getResources().getString(R.string.google_maps_API);
            Log.d(TAG, "getDirection:  " + requestApi);
            mService.getPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polyLineList = decodePoly(polyline);
                        }
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (LatLng latLng : polyLineList)
                            builder.include(latLng);
                        LatLngBounds bounds = builder.build();
                        CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                        mMap.animateCamera(mCameraUpdate);

                        bluePolylineOptions = new PolylineOptions();
                        bluePolylineOptions.color(R.color.colorPurple);
                        bluePolylineOptions.width(5);
                        bluePolylineOptions.startCap(new RoundCap());
                        bluePolylineOptions.endCap(new RoundCap());
                        bluePolylineOptions.jointType(JointType.ROUND);
                        bluePolylineOptions.addAll(polyLineList);
                        bluePolyline = mMap.addPolyline(bluePolylineOptions);

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(R.color.colorGreen);
                        polylineOptions.width(5);
                        polylineOptions.startCap(new RoundCap());
                        polylineOptions.endCap(new RoundCap());
                        polylineOptions.jointType(JointType.ROUND);
                        greenPolyline = mMap.addPolyline(polylineOptions);

                        mMap.addMarker(new MarkerOptions()
                                .position(polyLineList.get(polyLineList.size() - 1))
                                .title("PickUp Location"));

                        //Animation
                        ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0, 100);
                        polyLineAnimator.setDuration(2000);
                        polyLineAnimator.setInterpolator(new LinearInterpolator());
                        polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                List<LatLng> points = bluePolyline.getPoints();
                                int percentValue = (int) animation.getAnimatedValue();
                                int size = points.size();
                                int newpoints = (int) (size * (percentValue / 100.0f));
                                List<LatLng> p = points.subList(0, newpoints);
                                greenPolyline.setPoints(p);
                            }
                        });
                        polyLineAnimator.start();
                        if (mCurrent != null) {
                            mCurrent.remove();
                        }

                        mCurrent = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.car)));

                        index = -1;
                        next = 1;
                        handler.postDelayed(drawPathRunnable, 3000);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(DriverHomeActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*-----------Set Up Location------------------*/
    private void setUpLocation() {
        Log.d(TAG, "setUpLocation: setup startted");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            if (isServicesOK()) {
                buildLocationRequests();
                buildLocationCallback();
                if (location_switch.isChecked()) {
                    displayLocation();
                }
            }
        }
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    Common.mLastLocation = location;
                }
                displayLocation();
            }
        };
    }

    private void buildLocationRequests() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /*-----------Show Location in Map------------------*/
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        Common.mLastLocation = location;
                        if (Common.mLastLocation != null) {
                            Log.d(TAG, "displayLocation: displaying location");
                            if (location_switch.isChecked()) {
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
                                                    .position(new LatLng(latitude, longitude)));
                                            moveCamera(new LatLng(latitude, longitude), DEFAULT_ZOOM);
                                            Common.rotateMarker(mCurrent, -360, mMap);
                                        } else {
                                            mCurrent = mMap.addMarker(new MarkerOptions()
                                                    .icon(BitmapDescriptorFactory
                                                            .fromResource(R.mipmap.car))
                                                    .position(new LatLng(latitude, longitude)));
                                            moveCamera(new LatLng(latitude, longitude), DEFAULT_ZOOM);
                                            Common.rotateMarker(mCurrent, -360, mMap);
                                        }
                                    }
                                });

                            }
                        } else {
                            Log.d(TAG, "displayLocation: Cannot get location");
                        }
                    }
                });

    }


//    private void getDeviceLocation() {
//        Log.d(TAG, "getDeviceLocation: getting the device location");
//        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        try {
//            if (mLocationPermissionGranted) {
//                Task location = mFusedLocationProviderClient.getLastLocation();
//                location.addOnCompleteListener(new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if (task.isSuccessful()) {
//                            Log.d(TAG, "onComplete: found location");
//                            Location currentLocation = (Location) task.getResult();
//                            mLastLocation = (Location) task.getResult();
//                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);
//                        } else {
//                            Log.d(TAG, "onComplete: current location is null");
//                            Toast.makeText(DriverHomeActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//            }
//        } catch (SecurityException e) {
//            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
//        }
//    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving camera to lat: " + latLng.latitude + "lng: " + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(DriverHomeActivity.this, R.raw.mapstyle));

            if (!success) {

            }
        } catch (Resources.NotFoundException e) {

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildLocationRequests();
        buildLocationCallback();
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        location_switch.setChecked(sharedPreferences.getBoolean("toggleButton", false));
    }
    private void initMap(){
        Log.d(TAG, "initMap: Initializing map");
        SupportMapFragment mapFragment  = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
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
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DriverHomeActivity.this);
        if(available == ConnectionResult.SUCCESS){
            // everything is ok and user can make map requests
            Log.d(TAG, "isServicesOK: Google play services is working");
            return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: An error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DriverHomeActivity.this, available, ERROR_DIALOG_REQUEST);
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

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if (index<polyLineList.size()-1){
                index++;
                next = index+1;
            }
            if (index< polyLineList.size()-1){
                startPosition = polyLineList.get(index);
                endPosition = polyLineList.get(next);
            }
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v = valueAnimator.getAnimatedFraction();
                    lng = v*endPosition.longitude+(1-v)*startPosition.longitude;
                    lat = v*endPosition.latitude+(1-v)*startPosition.latitude;
                    LatLng newPos = new LatLng(lat,lng);
                    mCurrent.setPosition(newPos);
                    mCurrent.setAnchor(0.5f,0.5f);
                    mCurrent.setRotation(getBearing(startPosition,newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(newPos)
                            .zoom(15.5f)
                            .build()
                    ));
                }
            });
            valueAnimator.start();
            handler.postDelayed(this, 3000);
        }
    };

    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);
        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }
    private void erroralert(String message){
        Log.d(TAG, "erroralert: Dialog");
        android.support.v7.app.AlertDialog.Builder builder;
        builder = new android.support.v7.app.AlertDialog.Builder(DriverHomeActivity.this);
        builder.setTitle("Whoa! there's an error")
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_error);
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        location_switch.setChecked(bundle.getBoolean("ToggleButtonState",false));
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        location_switch.setChecked(sharedPreferences.getBoolean("toggleButton", false));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        location_switch.setChecked(bundle.getBoolean("ToggleButtonState",false));
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        location_switch.setChecked(sharedPreferences.getBoolean("toggleButton", false));
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("toggleButton", location_switch.isChecked());
        editor.commit();
        bundle.putBoolean("ToggleButtonState", location_switch.isChecked());
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("toggleButton", location_switch.isChecked());
        editor.commit();
        bundle.putBoolean("ToggleButtonState", location_switch.isChecked());
    }

    @Override
    protected void onStart() {
        super.onStart();
        location_switch.setChecked(bundle.getBoolean("ToggleButtonState",false));
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        location_switch.setChecked(sharedPreferences.getBoolean("toggleButton", false));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ToggleButtonState", location_switch.isChecked());
    }
}
