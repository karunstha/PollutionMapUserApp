package com.halo.pmapu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.halo.pmapu.data.AdapterPred;
import com.halo.pmapu.data.ModelPred;
import com.halo.pmapu.helper.AppConstants;
import com.halo.pmapu.helper.GPSUtils;
import com.halo.pmapu.helper.ModelData;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Activity_Home extends AppCompatActivity {

    DatabaseReference dbRef;

    FirebaseDatabase database = FirebaseDatabase.getInstance();

    RecyclerView recyclerView;
    ArrayList<ModelPred> modelPredArrayList = new ArrayList<>();
    AdapterPred adapterPred;

    Query query;

    TextView tv_value, tv_place;

    FloatingActionButton fab_map;

    ArrayList<ModelData> modelDataArrayList = new ArrayList<>();
    ArrayList<ModelData> modelDataArrayListAI = new ArrayList<>();

    SimpleDateFormat sdf = new SimpleDateFormat("HH",Locale.getDefault());

    GraphView graph;

    RelativeLayout rellay_top;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
        String date = df.format(c);

        dbRef = database.getReference("data/" + date + "/");

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView_pred);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        adapterPred = new AdapterPred(modelPredArrayList, this, recyclerView);
        recyclerView.setAdapter(adapterPred);

        tv_value = findViewById(R.id.tv_value);
        tv_place = findViewById(R.id.tv_location);

        rellay_top = findViewById(R.id.rellay1);

        fab_map = findViewById(R.id.fab_map);
        fab_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Activity_Home.this, Activity_Map.class);
                intent.putExtra("latitude", currLocation.getLatitude());
                intent.putExtra("longitude", currLocation.getLongitude());
                startActivity(intent);
            }
        });

        graph = (GraphView) findViewById(R.id.graph);

//        graph.getGridLabelRenderer().setLabelFormatter(new LabelFormatter() {
//            @Override
//            public String formatLabel(double value, boolean isValueX) {
//                if (isValueX) {
//                    return sdf.format(7);
//                }
//                return null;
//            }
//
//            @Override
//            public void setViewport(Viewport viewport) {
//
//            }
//        });

//        graph.getGridLabelRenderer().setHumanRounding(false);
        graph.getGridLabelRenderer().setNumHorizontalLabels(12);
        graph.getGridLabelRenderer().setGridColor(getResources().getColor(R.color.secondaryText));
        graph.getGridLabelRenderer().setHorizontalLabelsColor(getResources().getColor(android.R.color.white));
        graph.getGridLabelRenderer().setVerticalLabelsColor(getResources().getColor(android.R.color.white));

        location();

        regressionCalc();

    }

    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private boolean isGPS = false;

    Location currLocation;
    String locationName;

    public void location() {

        Log.d("Locationnn", "Location Log Starts Here");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                currLocation = location;
                getAddressFromLocation(currLocation);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("Locationnn", "Last Known Location:");
                Log.d("Locationnn", latitude + ", " + longitude);
            }
        });

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(1000); // 1 second

        new GPSUtils(Activity_Home.this).turnGPSOn(new GPSUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {

                        currLocation = location;
                        getAddressFromLocation(currLocation);
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        Log.d("Locationnn", "Live Location:");
                        Log.d("Locationnn", latitude + ", " + longitude);
//                        if (mFusedLocationClient != null) {
//                            mFusedLocationClient.removeLocationUpdates(locationCallback);
//                        }
                    }
                }
            }
        };

        getLocation();

    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(Activity_Home.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(Activity_Home.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Activity_Home.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);

        } else {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }

    public void fetchDataFromFirebase(final String locationN) {

        query = dbRef.orderByChild("LocationName").equalTo(locationN);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String _id = postSnapshot.getKey();
                        String location = postSnapshot.child("Location").getValue().toString();
                        String locationName = postSnapshot.child("LocationName").getValue().toString();
                        String time = postSnapshot.child("Time").getValue().toString();
                        String value = postSnapshot.child("Value").getValue().toString();

                        ModelData modelData = new ModelData(_id, locationN, location, time, value);
                        modelDataArrayList.add(modelData);
                    }

                    Collections.sort(modelDataArrayList, new Comparator<ModelData>() {

                        @Override
                        public int compare(ModelData o1, ModelData o2) {
                            try {
                                return new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).parse(o1.getTime()).compareTo(new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).parse(o2.getTime()));
                            } catch (ParseException e) {
                                return 0;
                            }
                        }
                    });

                    for (int i = 0; i < modelDataArrayList.size(); i++) {
                        String ti = modelDataArrayList.get(i).getTime();
                        String t[] = ti.split(":");
                        if (modelDataArrayListAI.size() == 0) {
                            ModelData m = modelDataArrayList.get(i);
                            ModelData md = new ModelData(m.getId(), m.getLocationName(), m.getLocation(), t[0], m.getValue());
                            modelDataArrayListAI.add(md);
                        } else {
                            boolean cha = false;
                            for (int j = 0; j < modelDataArrayListAI.size(); j++) {
                                String tj = modelDataArrayListAI.get(j).getTime();
                                if (tj.equals(t[0])) {
                                    cha = true;
                                }
                            }
                            if (!cha) {
                                ModelData m = modelDataArrayList.get(i);
                                ModelData md = new ModelData(m.getId(), m.getLocationName(), m.getLocation(), t[0], m.getValue());
                                modelDataArrayListAI.add(md);
                            }
                        }
                    }

                    List<DataPoint> dataPoints = new ArrayList<>(modelDataArrayListAI.size());

                    for (int k = 0; k < modelDataArrayListAI.size(); k++) {
                        ModelData m = modelDataArrayListAI.get(k);
                        dataPoints.add(new DataPoint(Integer.parseInt(m.getTime()), Float.parseFloat(m.getValue())));
                    }

                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints.toArray(new
                            DataPoint[dataPoints.size()]));
                    graph.removeAllSeries();
                    graph.addSeries(series);

                    ModelData m = modelDataArrayList.get(modelDataArrayList.size() - 1);
                    String value = m.getValue();
                    tv_value.setText(String.format(Locale.getDefault(), "%.1f", Float.parseFloat(value)));

                    float val = Float.parseFloat(value);
                    int statusBarColor = ContextCompat.getColor(Activity_Home.this, R.color.colorPrimaryDark);
                    if (val > 500) {
                        rellay_top.setBackgroundDrawable(ContextCompat.getDrawable(Activity_Home.this, R.drawable.rectangle1_red));
                        statusBarColor = ContextCompat.getColor(Activity_Home.this, R.color.red);
                    } else if (val > 300) {
                        rellay_top.setBackgroundDrawable(ContextCompat.getDrawable(Activity_Home.this, R.drawable.rectangle1_orange));
                        statusBarColor = ContextCompat.getColor(Activity_Home.this, R.color.orange);
                    } else if (val > 150) {
                        rellay_top.setBackgroundDrawable(ContextCompat.getDrawable(Activity_Home.this, R.drawable.rectangle1_yellow));
                        statusBarColor = ContextCompat.getColor(Activity_Home.this, R.color.yellow);
                    } else if (val > 0) {
                        rellay_top.setBackgroundDrawable(ContextCompat.getDrawable(Activity_Home.this, R.drawable.rectangle1_green));
                        statusBarColor = ContextCompat.getColor(Activity_Home.this, R.color.green);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Window window = getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(statusBarColor);
                    }

                    tv_place.setText(m.getLocationName());

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void getAddressFromLocation(final Location location) {
        final Context context = Activity_Home.this;
        Thread thread = new Thread() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String result = null;
                try {
                    List<Address> list = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    if (list != null && list.size() > 0) {
                        Address address = list.get(0);
                        // sending back first address line and locality
                        result = address.getFeatureName();
                    }
                } catch (IOException e) {
                    Log.e("LocationName", "Impossible to connect to Geocoder", e);
                } finally {
                    locationName = result;
                    if (locationName != null) {
                        if (!locationName.equals("Unnamed Road") && locationName.length() != 0) {
                            fetchDataFromFirebase(locationName);
                        }
                    }
                }
            }
        };
        thread.start();
    }

    public void regressionCalc() {

        for (int h = 4; h <= 20; h++) {
            int timeC = h;
            int x[] = new int[17];
            double y[] = new double[17];
            int timeR[] = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 10};
            double Pol[] = {156.5,160.5,166.5,174.5,175.5,165,161.5,157.5,152.5,152,156.5,157.5,163.5,163.5,157.5,157.5,158.5};

            if (timeC >= 4 && timeC <= 7) {
                for (int j = 0; j <= 3; j++) {
                    x[j] = timeR[j];
                    y[j] = Pol[j];
                }
                int x2[] = new int[4];
                double xy[] = new double[4];
                int sumX = 0;
                float sumY = 0;
                float sumX2 = 0;
                float sumXY = 0;
                for (int i = 0; i <= 3; i++) {
                    sumX += x[i];
                    sumY += y[i];
                    x2[i] = x[i] * x[i];
                    xy[i] = x[i] * y[i];
                    sumXY += xy[i];
                    sumX2 += x2[i];
                }
                double b = (4 * sumXY - sumX * sumY) / (4 * sumX2 - sumX * sumX);
                double a = sumY / 4 - b * sumX / 4;
                double ans = a + b * timeC;
                populateRecycler(timeC + ":00", String.format(Locale.getDefault(),"%.1f", ans));
            }
            if (timeC >= 8 && timeC <= 14) {
                for (int j = 4; j <= 10; j++) {
                    x[j] = timeR[j];
                    y[j] = Pol[j];
                }
                int x2[] = new int[11];
                double xy[] = new double[11];
                int sumX = 0;
                double sumY = 0;
                double sumX2 = 0;
                double sumXY = 0;
                for (int i = 4; i <= 10; i++) {
                    sumX += x[i];
                    sumY += y[i];
                    x2[i] = x[i] * x[i];
                    xy[i] = x[i] * y[i];
                    sumXY += xy[i];
                    sumX2 += x2[i];
                }
                double b = (7 * sumXY - sumX * sumY) / (7 * sumX2 - sumX * sumX);
                double a = sumY / 7 - b * sumX / 7;
                double ans = a + b * timeC;
                populateRecycler(timeC + ":00", String.format(Locale.getDefault(),"%.1f", ans));
            }
            if (timeC >= 15 && timeC <= 20) {
                for (int j = 11; j <= 16; j++) {
                    x[j] = timeR[j];
                    y[j] = Pol[j];
                }
                int x2[] = new int[17];
                double xy[] = new double[17];
                int sumX = 0;
                double sumY = 0;
                double sumX2 = 0;
                double sumXY = 0;
                for (int i = 11; i <= 16; i++) {
                    sumX += x[i];
                    sumY += y[i];
                    x2[i] = x[i] * x[i];
                    xy[i] = x[i] * y[i];
                    sumXY += xy[i];
                    sumX2 += x2[i];
                }
                double b = (6 * sumXY - sumX * sumY) / (6 * sumX2 - sumX * sumX);
                double a = sumY / 6 - b * sumX / 6;
                double ans = a + b * timeC;
                populateRecycler(timeC + ":00", String.format(Locale.getDefault(), "%.1f", ans));
            }
        }

    }

    public void populateRecycler(String time, String value) {

        ModelPred modelPred = new ModelPred(value, time);
        modelPredArrayList.add(modelPred);

        runOnUiThread(new Runnable() {
            public void run() {
                adapterPred.notifyDataSetChanged();
            }
        });

    }

}
