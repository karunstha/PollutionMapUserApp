package com.halo.pmapu;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.halo.pmapu.helper.ModelData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class Activity_Map extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnCameraIdleListener {

    ArrayList<ModelData> modelDataArrayList = new ArrayList<>();

    float zoom = 15;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    FloatingActionButton fab_currLocation;

    DatabaseReference dbRef;

    FirebaseDatabase database = FirebaseDatabase.getInstance();

    Query query;

    double currLat;
    double currLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        currLat = getIntent().getExtras().getDouble("latitude");
        currLong = getIntent().getExtras().getDouble("longitude");

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
        String date = df.format(c);
        dbRef = database.getReference("data/" + date + "/");

        FragmentManager fmanager = getSupportFragmentManager();
        Fragment fragment = fmanager.findFragmentById(R.id.fragment_mapMyLocation);
        SupportMapFragment googleMapFrag = (SupportMapFragment) fragment;
        if (googleMapFrag != null) {
            googleMapFrag.getMapAsync(this);
        }else {
            Toast.makeText(getApplicationContext(),
                    "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                    .show();
        }

        buildGoogleApiClient();

        fab_currLocation = (FloatingActionButton) findViewById(R.id.fab_currLocation);
        fab_currLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setToLastKnownLoc();
            }
        });
    }

    public void setToLastKnownLoc() {

        fab_currLocation.setImageResource(R.drawable.ic_my_location);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(currLat,
                        currLong)).zoom(zoom).build();
        googleMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
    }

    @Override
    protected void onResume() {
        super.onResume();

        mGoogleApiClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        Toast.makeText(getApplicationContext(), "buildGoogleApiClient", Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
        }

        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.setOnCameraIdleListener(this);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(currLat,
                        currLong)).zoom(zoom).build();
        googleMap.moveCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));

        this.googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                CameraPosition cameraPosition = googleMap.getCameraPosition();
                zoom = cameraPosition.zoom;
            }
        });

        fetchDataFromFirebase();
    }

    private Bitmap getBitmap(int drawableRes) {
        Drawable drawable = getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getApplicationContext(), "onConnected", Toast.LENGTH_SHORT).show();

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000 * 5);
        mLocationRequest.setFastestInterval(1000);

//        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        //    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onCameraIdle() {

    }

    public void fetchDataFromFirebase() {

        query = dbRef;

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ArrayList<ModelData> modelDataArrayList1 = new ArrayList<>();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String _id = postSnapshot.getKey();
                        String location = postSnapshot.child("Location").getValue().toString();
                        String locationName = postSnapshot.child("LocationName").getValue().toString();
                        String time = postSnapshot.child("Time").getValue().toString();
                        String value = postSnapshot.child("Value").getValue().toString();

                        ModelData modelData = new ModelData(_id, locationName, location, time, value);
                        modelDataArrayList1.add(modelData);
                    }

                    Collections.sort(modelDataArrayList1, new Comparator<ModelData>() {

                        @Override
                        public int compare(ModelData o1, ModelData o2) {
                            try {
                                return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(o2.getTime()).compareTo(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(o1.getTime()));
                            } catch (ParseException e) {
                                return 0;
                            }
                        }
                    });

                    for (int i = 0; i < modelDataArrayList1.size(); i++) {
                        String li = modelDataArrayList1.get(i).getLocationName();
                        if (modelDataArrayList.size() == 0) {
                            modelDataArrayList.add(modelDataArrayList1.get(i));
                        } else {
                            boolean cha = false;
                            for (int j = 0; j < modelDataArrayList.size(); j++) {
                                String lj = modelDataArrayList.get(j).getLocationName();
                                if (lj.equals(li)) {
                                    cha = true;
                                }
                            }
                            if (!cha) {
                                modelDataArrayList.add(modelDataArrayList1.get(i));
                            }
                        }
                    }

                    for (int k = 0; k < modelDataArrayList.size(); k++) {
                        String[] locationn = modelDataArrayList.get(k).getLocation().split(", ");
                        double lat = Double.parseDouble(locationn[0]);
                        double longg = Double.parseDouble(locationn[1]);

                        float value = Float.parseFloat(modelDataArrayList.get(k).getValue());

                        Bitmap drawableBitmap = getBitmap(R.drawable.marker_red);
                        if (value > 500) {
                            drawableBitmap = getBitmap(R.drawable.marker_red);
                        } else if (value > 300) {
                            drawableBitmap = getBitmap(R.drawable.marker_orange);
                        } else if (value > 150) {
                            drawableBitmap = getBitmap(R.drawable.marker_yellow);
                        } else if (value > 0) {
                            drawableBitmap = getBitmap(R.drawable.marker_green);
                        }

                        googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, longg))
                                .title(modelDataArrayList.get(k).getLocationName())
                                .snippet("Value: " + modelDataArrayList.get(k).getValue())
                                .icon(BitmapDescriptorFactory.fromBitmap(drawableBitmap)));
                        }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
