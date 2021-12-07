package edu.ucsb.ece150.locationplus;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    private Geofence mGeofence;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mPendingIntent = null;
    private Toolbar mToolbar;
    private GnssStatus.Callback mGnssStatusCallback;
    private GoogleMap mMap;
    private LocationManager mLocationManager;

    private LatLng cur_LatLng = new LatLng(0, 0);
    private LatLng geoFence_LatLng;

    private boolean check_centered = false;
    private boolean check_info = false;
    private boolean check_geoFence = false;

    private ArrayList<Satellite> satelliteArray;
    private ArrayList<String> satelliteNameArray;
    private ArrayAdapter adapter;
    private ListView satListView;
    private View mapView;
    private FloatingActionButton b;
    private Marker marker_geoFence;
    private Circle circle_geoFence;
    private AlertDialog.Builder builder_dial;
    private String PREFS_NAME = "A";
    private Marker m;



    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Set up Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up Geofencing Client
        mGeofencingClient = LocationServices.getGeofencingClient(MapsActivity.this);

        // Set up Satellite List
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        satelliteArray = new ArrayList<Satellite>();
        satelliteNameArray = new ArrayList<String>();

        mapView = findViewById(R.id.map);
        satListView = findViewById(R.id.satelliteList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, satelliteNameArray);


        mGnssStatusCallback = new GnssStatus.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                // [TODO] Implement behavior when the satellite status is updated
                satelliteArray.clear();
                satelliteNameArray.clear();
                int size = status.getSatelliteCount();
                for (int i = 0; i < size; i++) {
                    satelliteArray.add(new Satellite(
                            status.getAzimuthDegrees(i),
                            status.getElevationDegrees(i),
                            status.getCarrierFrequencyHz(i),
                            status.getCn0DbHz(i),
                            status.getConstellationType(i),
                            status.getSvid(i)
                    ));
                    satelliteNameArray.add("Satellite " + i);
                }
                adapter.notifyDataSetChanged();
                satListView.setAdapter(adapter);

            }
        };

        // [TODO] Additional setup for viewing satellite information (lists, adapters, etc.)

        // Set up Toolbar
        mToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        satListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                builder_dial = new AlertDialog.Builder(MapsActivity.this);
                builder_dial.setMessage(satelliteArray.get(position).toString())
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                // Create and show the alert dialog
                AlertDialog alert = builder_dial.create();
                alert.setTitle("Satellite" + position);
                alert.show();
            }
        });

        // Set up floating action button
        b = findViewById(R.id.button);
        b.setVisibility(View.GONE);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGeofencingClient.removeGeofences(getGeofencePendingIntent());
                marker_geoFence.remove();
                circle_geoFence.remove();


                b.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // [TODO] Implement behavior when Google Maps is ready
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Permission", Toast.LENGTH_SHORT).show();
            return;
        }

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        cur_LatLng = new LatLng(bestLocation.getLatitude(), bestLocation.getLongitude());
        m = mMap.addMarker(new MarkerOptions().position(cur_LatLng)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (check_centered)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(cur_LatLng));
            }
        });

        if(check_geoFence){
            marker_geoFence = mMap.addMarker(new MarkerOptions().position(geoFence_LatLng)
                    .title("Destination")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            circle_geoFence = mMap.addCircle(new CircleOptions()
                    .center(geoFence_LatLng)
                    .radius(50)
                    .strokeColor(Color.argb(40, Color.red(Color.RED), Color.green(Color.RED), Color.blue(Color.RED)))
                    .fillColor(Color.argb(40, Color.red(Color.RED), Color.green(Color.RED), Color.blue(Color.RED))));
            mGeofence = new Geofence.Builder()
                    .setRequestId("Destination")
                    .setCircularRegion(geoFence_LatLng.latitude, geoFence_LatLng.longitude, 100)
                    .setExpirationDuration(50000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build();
        }
        if(check_info){
            mapView.setVisibility(View.GONE);
        }
        // [TODO] In addition, add a listener for long clicks (which is the starting point for
        // creating a Geofence for the destination and listening for transitions that indicate
        // arrival)

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {

                if(!check_geoFence) {
                    marker_geoFence = mMap.addMarker(new MarkerOptions().position(latLng)
                            .title("Destination")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    circle_geoFence = mMap.addCircle(new CircleOptions()
                            .center(latLng)
                            .radius(50)
                            .strokeColor(Color.argb(40,Color.red(Color.RED),Color.green(Color.RED),Color.blue(Color.RED)))
                            .fillColor(Color.argb(40,Color.red(Color.RED),Color.green(Color.RED),Color.blue(Color.RED))));
                    mGeofence = new Geofence.Builder()
                            .setRequestId("Destination")
                            .setCircularRegion(latLng.latitude, latLng.longitude, 100)
                            .setExpirationDuration(50000)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build();

                    mGeofencingClient.addGeofences(getGeofenceRequest(), getGeofencePendingIntent());
                }
                builder_dial = new AlertDialog.Builder(MapsActivity.this);
                builder_dial.setTitle("Confirm Destination");

                String message = "Set position (" + latLng.latitude + "◦ ," + latLng.longitude + "◦) as your destination?";
                builder_dial.setMessage(message);
                builder_dial.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(check_geoFence) {
                            marker_geoFence.remove();
                            circle_geoFence.remove();
                            marker_geoFence = mMap.addMarker(new MarkerOptions().position(latLng)
                                    .title("Destination")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            circle_geoFence = mMap.addCircle(new CircleOptions()
                                    .center(latLng)
                                    .radius(50)
                                    .strokeColor(Color.argb(40, Color.red(Color.RED), Color.green(Color.RED),Color.blue(Color.RED)))
                                    .fillColor(Color.argb(40,Color.red(Color.RED),Color.green(Color.RED),Color.blue(Color.RED))));
                            mGeofence = new Geofence.Builder()
                                    .setRequestId("Destination")
                                    .setCircularRegion(latLng.latitude, latLng.longitude, 100)
                                    .setExpirationDuration(50000)
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                    .build();
                            mGeofencingClient.addGeofences(getGeofenceRequest(), getGeofencePendingIntent());

                        }

                        check_geoFence = true;
                        b.setVisibility(View.VISIBLE);
                    }
                });

                builder_dial.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(!check_geoFence){
                            b.setVisibility(View.GONE);
                        }else{
                            marker_geoFence.remove();
                            circle_geoFence.remove();
                        }
                    }
                });

                AlertDialog alert = builder_dial.create();
                alert.show();

                return;
            }
        });
        check_geoFence = false;

        GeofenceBroadcastReceiver updateGeoFenceReceiver;

        IntentFilter filter = new IntentFilter();
        filter.addAction("Remove Geofence");
        updateGeoFenceReceiver = new GeofenceBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null){
                    marker_geoFence.remove();
                    circle_geoFence.remove();
                    check_geoFence = false;
                }
            }
        };
        registerReceiver(updateGeoFenceReceiver, filter);
    }

    @Override
    public void onLocationChanged(Location location) {
        // [TODO] Implement behavior when a location update is received
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Permission", Toast.LENGTH_SHORT).show();
            return;
        }
        Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        cur_LatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        m.remove();
        m = mMap.addMarker(new MarkerOptions().position(cur_LatLng)
                .title(" Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    /*
     * The following three methods onProviderDisabled(), onProviderEnabled(), and onStatusChanged()
     * do not need to be implemented -- they must be here because this Activity implements
     * LocationListener.
     *
     * You may use them if you need to.
     */
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private GeofencingRequest getGeofenceRequest() {
        // [TODO] Set the initial trigger (i.e. what should be triggered if the user is already
        // inside the Geofence when it is created)

        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)   // <--  Add triggers here
                .addGeofence(mGeofence)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if(mPendingIntent != null)
            return mPendingIntent;

        Intent intent = new Intent(MapsActivity.this, GeofenceBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(MapsActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() throws SecurityException {
        super.onStart();

        // [TODO] Ensure that necessary permissions are granted (look in AndroidManifest.xml to
        // see what permissions are needed for this app)

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // [TODO] Data recovery

        SharedPreferences mPreference = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        check_geoFence = mPreference.getBoolean("check_geofence", false);
        check_centered = mPreference.getBoolean("check_centered", false);
        check_info = mPreference.getBoolean("check_info", false);
        if (check_geoFence) {
            geoFence_LatLng = new LatLng(mPreference.getFloat("lng", 0), mPreference.getFloat("lat", 0));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // [TODO] Data saving
        SharedPreferences mPreference = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = mPreference.edit();

        editor.putBoolean("check_centered",check_centered);
        editor.putBoolean("check_info",check_info);
        editor.putBoolean("check_geofence",check_geoFence);

        if (check_geoFence){
            editor.putFloat("lng", (float)geoFence_LatLng.longitude);
            editor.putFloat("lat", (float)geoFence_LatLng.latitude);
        }
        editor.apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStop() {
        super.onStop();

        mLocationManager.removeUpdates(this);
        mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.center:
                if (check_centered) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(cur_LatLng));
                    Toast.makeText(this, "Auto-Center Deactivated", Toast.LENGTH_SHORT).show();
                }
                else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(cur_LatLng));
                    Toast.makeText(this, "Auto-Center Activated", Toast.LENGTH_SHORT).show();
                }
                check_centered = !check_centered;
                break;
            // action with ID action_settings was selected
            case R.id.info:
                if(check_info){
                    Toast.makeText(this, "Open Satellite List", Toast.LENGTH_SHORT).show();
                    mapView.setVisibility(View.VISIBLE);
                }else{
                    Toast.makeText(this, "Close Satellite List", Toast.LENGTH_SHORT).show();
                    mapView.setVisibility(View.GONE);
                }
                check_info = !check_info;
                break;
            default:
                break;
        }

        return true;
    }
}
