package com.dji.geodemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIFlyZoneInformation;
import dji.common.flightcontroller.FlyForbidStatus;
import dji.common.flightcontroller.UserAccountStatus;
import dji.common.util.DJICommonCallbacks;
import dji.midware.data.model.P3.DataOsdGetPushCommon;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.flightcontroller.DJIFlyZoneManager;
import dji.sdk.products.DJIAircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getName();

    private GoogleMap mMap;

    protected TextView mConnectStatusTextView;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnUnlock;
    private Button btnGetUnlock;
    private Button btnGetSurroundNFZ;
    private Button btnSetEnableGeoSystem;
    private Button btnGetEnableGeoSystem;
    private Button btnUpdateLocation;

    private TextView loginStatusTv;
    private TextView flyZonesTv;

    private Marker marker;
    private DJIFlightController mFlightController = null;

    private MarkerOptions markerOptions = new MarkerOptions();
    private LatLng latLng;
    private double droneLocationLat = 181, droneLocationLng = 181;
    private ArrayList<Integer> unlockFlyZoneIds = new ArrayList<Integer>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK works well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.VIBRATE,
                            android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.CHANGE_WIFI_STATE, android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                            android.Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        initUI();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DJIFlyZoneManager.getInstance().setFlyForbidStatusUpdatedCallback(new DJIFlyZoneManager.FlyForbidStatusUpdatedCallback() {
            @Override
            public void onFlyForbidStatusUpdated(FlyForbidStatus status) {
                showToast(status.name());
            }
        });
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateTitleBar();
        initFlightController();
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    private void updateTitleBar() {
        if (mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = GEODemoApplication.getProductInstance();
        if (product != null) {
            if (product.isConnected()) {
                //The product is connected
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        mConnectStatusTextView.setText(GEODemoApplication.getProductInstance().getModel() + " Connected");
                    }
                });
                ret = true;
            } else {
                if (product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                mConnectStatusTextView.setText("only RC Connected");
                            }
                        });
                        ret = true;
                    }
                }
            }
        }

        if (!ret) {
            // The product or the remote controller are not connected.

            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mConnectStatusTextView.setText("Disconnected");
                }
            });
        }
    }

    private void initUI() {

        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

        btnLogin = (Button) findViewById(R.id.geo_login_btn);
        btnLogout = (Button) findViewById(R.id.geo_logout_btn);
        btnUnlock = (Button) findViewById(R.id.geo_unlock_nfzs_btn);
        btnGetUnlock = (Button) findViewById(R.id.geo_get_unlock_nfzs_btn);
        btnGetSurroundNFZ = (Button) findViewById(R.id.geo_get_surrounding_nfz_btn);
        btnSetEnableGeoSystem = (Button) findViewById(R.id.geo_set_geo_enabled_btn);
        btnGetEnableGeoSystem = (Button) findViewById(R.id.geo_get_geo_enabled_btn);
        btnUpdateLocation = (Button) findViewById(R.id.geo_update_location_btn);

        loginStatusTv = (TextView) findViewById(R.id.login_status);
        loginStatusTv.setTextColor(Color.BLACK);
        flyZonesTv = (TextView) findViewById(R.id.fly_zone_tv);
        flyZonesTv.setTextColor(Color.BLACK);

        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnUnlock.setOnClickListener(this);
        btnGetUnlock.setOnClickListener(this);
        btnGetSurroundNFZ.setOnClickListener(this);
        btnSetEnableGeoSystem.setOnClickListener(this);
        btnGetEnableGeoSystem.setOnClickListener(this);
        btnUpdateLocation.setOnClickListener(this);

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                loginStatusTv.setText(DJIFlyZoneManager.getInstance().getCurrentUserAccountStatus().name());
            }
        });

    }

    public void showToast(final String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.geo_login_btn:
                DJIFlyZoneManager.getInstance().LogIntoDJIUserAccount(this, new DJICommonCallbacks.DJICompletionCallbackWith<UserAccountStatus>() {
                    @Override
                    public void onSuccess(final UserAccountStatus userAccountStatus) {
                        showToast(userAccountStatus.name());
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loginStatusTv.setText(userAccountStatus.name());
                            }
                        });

                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast(error.getDescription());
                    }
                });
                break;

            case R.id.geo_logout_btn:

                DJIFlyZoneManager.getInstance().logoutOfDJIUserAccount(new DJICommonCallbacks.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null == error) {
                            showToast("logoutOfDJIUserAccount Success");

                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loginStatusTv.setText("NotLoggedin");
                                }
                            });

                        } else {
                            showToast(error.getDescription());
                        }
                    }
                });

                break;

            case R.id.geo_unlock_nfzs_btn:

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText input = new EditText(this);
                input.setHint("Enter Fly Zone ID");
                input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                builder.setView(input);
                builder.setTitle("Unlock Fly Zones");
                builder.setItems(new CharSequence[]
                                {"Continue", "Unlock", "Cancel"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                switch (which) {
                                    case 0:
                                        if (TextUtils.isEmpty(input.getText())) {
                                            dialog.dismiss();
                                        } else {
                                            String value1 = input.getText().toString();
                                            unlockFlyZoneIds.add(Integer.parseInt(value1));
                                        }
                                        break;
                                    case 1:
                                        if (TextUtils.isEmpty(input.getText())) {
                                            dialog.dismiss();
                                        } else {
                                            String value2 = input.getText().toString();
                                            unlockFlyZoneIds.add(Integer.parseInt(value2));
                                            DJIFlyZoneManager.getInstance().unlockNFZs(unlockFlyZoneIds, new DJICommonCallbacks.DJICompletionCallback() {
                                                @Override
                                                public void onResult(DJIError error) {

                                                    unlockFlyZoneIds.clear();
                                                    if (error == null) {
                                                        showToast("unlock NFZ Success!");
                                                    } else {
                                                        showToast(error.getDescription());
                                                    }
                                                }
                                            });
                                        }
                                        break;
                                    case 2:
                                        dialog.dismiss();
                                        break;
                                }
                            }
                        });

                builder.show();
                break;

            case R.id.geo_get_unlock_nfzs_btn:

                DJIFlyZoneManager.getInstance().getUnlockedNFZs(new DJICommonCallbacks.DJICompletionCallbackWith<ArrayList<DJIFlyZoneInformation>>() {
                    @Override
                    public void onSuccess(ArrayList<DJIFlyZoneInformation> djiFlyZoneInformations) {
                        showToast("Get Unlock NFZ success");
                        showSurroundFlyZonesInTv(djiFlyZoneInformations);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        showToast(djiError.getDescription());
                    }
                });

                break;

            case R.id.geo_get_surrounding_nfz_btn:
                printSurroundFlyZones();
                break;

            case R.id.geo_update_location_btn:
                latLng = new LatLng(DataOsdGetPushCommon.getInstance().getLatitude(),
                        DataOsdGetPushCommon.getInstance().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
                break;

            case R.id.geo_set_geo_enabled_btn:

                final AlertDialog.Builder setGEObuilder = new AlertDialog.Builder(this);
                setGEObuilder.setTitle("Set GEO Enable");
                setGEObuilder.setItems(new CharSequence[]
                                {"Enable", "Disable", "Cancel"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // The 'which' argument contains the index position
                                // of the selected item
                                switch (which) {
                                    case 0:
                                        DJIFlyZoneManager.getInstance().setGEOSystemEnabled(true, new DJICommonCallbacks.DJICompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (null == djiError) {
                                                    showToast("set GEO Enabled Success");
                                                } else {
                                                    showToast(djiError.getDescription());
                                                }
                                            }
                                        });
                                        break;
                                    case 1:
                                        DJIFlyZoneManager.getInstance().setGEOSystemEnabled(false, new DJICommonCallbacks.DJICompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (null == djiError) {
                                                    showToast("set GEO Disable Success");
                                                } else {
                                                    showToast(djiError.getDescription());
                                                }
                                            }
                                        });
                                        break;
                                    case 2:
                                        dialog.dismiss();
                                        break;
                                }
                            }
                        });

                setGEObuilder.show();
                break;

            case R.id.geo_get_geo_enabled_btn:

                DJIFlyZoneManager.getInstance().getGEOSystemEnabled(new DJICommonCallbacks.DJICompletionCallbackWith<Boolean>() {

                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        showToast("GEO System Enable");
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast(error.getDescription());
                    }
                });
                break;
        }
    }


    private void initFlightController() {

        if (isFlightControllerSupported()) {
            mFlightController = ((DJIAircraft) DJISDKManager.getInstance().getDJIProduct()).getFlightController();

            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(final DJIFlightControllerCurrentState state) {

                    if (mMap != null) {
                        droneLocationLat = state.getAircraftLocation().getLatitude();
                        droneLocationLng = state.getAircraftLocation().getLongitude();
                        updateDroneLocation();
                    }
                }
            });
        }
    }

    public static boolean checkGpsCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void updateDroneLocation(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (marker != null) {
                    marker.remove();
                }
                if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {

                    LatLng pos = new LatLng(droneLocationLat, droneLocationLng);

                    //Create MarkerOptions object
                    final MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(pos);
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
                    marker = mMap.addMarker(markerOptions);
                }
            }
        });
    }

    private boolean isFlightControllerSupported() {
        return DJISDKManager.getInstance().getDJIProduct() != null &&
                DJISDKManager.getInstance().getDJIProduct() instanceof DJIAircraft &&
                ((DJIAircraft) DJISDKManager.getInstance().getDJIProduct()).getFlightController() != null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng paloAlto = new LatLng(37.453671, -122.118101);

        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(paloAlto));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            return;
        }

        printSurroundFlyZones();
    }

    private void printSurroundFlyZones() {

        DJIFlyZoneManager.getInstance().getFlyZonesInSurroundingAre(new DJICommonCallbacks.DJICompletionCallbackWith<ArrayList<DJIFlyZoneInformation>>() {
            @Override
            public void onSuccess(ArrayList<DJIFlyZoneInformation> flyZones) {
                showToast("get surrounding Fly Zone Success!");
                updateFlyZonesOnTheMap(flyZones);
                showSurroundFlyZonesInTv(flyZones);
            }

            @Override
            public void onFailure(DJIError error) {
                showToast(error.getDescription());
            }
        });
    }

    private void showSurroundFlyZonesInTv(final ArrayList<DJIFlyZoneInformation> flyZones) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuffer sb = new StringBuffer();
                for (DJIFlyZoneInformation flyZone : flyZones) {
                    if (flyZone != null && flyZone.getCategory() != null){

                        sb.append("FlyZoneId: ").append(flyZone.getFlyZoneId()).append("\n");
                        sb.append("Category: ").append(flyZone.getCategory().name()).append("\n");
                        sb.append("Latitude: ").append(flyZone.getLatitude()).append("\n");
                        sb.append("Longitude: ").append(flyZone.getLongitude()).append("\n");
                        sb.append("FlyZoneType: ").append(flyZone.getFlyZoneType().name()).append("\n");
                        sb.append("Radius: ").append(flyZone.getRadius()).append("\n");
                        sb.append("Shape: ").append(flyZone.getShape().name()).append("\n");
                        sb.append("StartTime: ").append(flyZone.getStartTime()).append("\n");
                        sb.append("EndTime: ").append(flyZone.getEndTime()).append("\n");
                        sb.append("UnlockStartTime: ").append(flyZone.getUnlockStartTime()).append("\n");
                        sb.append("UnlockEndTime: ").append(flyZone.getUnlockEndTime()).append("\n");
                        sb.append("Name: ").append(flyZone.getName()).append("\n");
                        sb.append("\n");
                    }
                }
                flyZonesTv.setText(sb.toString());
            }
        });
    }

    private void updateFlyZonesOnTheMap(final ArrayList<DJIFlyZoneInformation> flyZones) {
        if (mMap == null) {
            return;
        }
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMap.clear();
                for (DJIFlyZoneInformation flyZone : flyZones) {
                    CircleOptions circle = new CircleOptions();
                    circle.radius(flyZone.getRadius());
                    circle.center(new LatLng(flyZone.getLatitude(), flyZone.getLongitude()));
                    switch (flyZone.getCategory()) {
                        case Warning:
                            circle.strokeColor(Color.GREEN);
                            break;
                        case EnhancedWarning:
                            circle.strokeColor(Color.BLUE);
                            break;
                        case Authorization:
                            circle.strokeColor(Color.YELLOW);
                            break;
                        case Restricted:
                            circle.strokeColor(Color.RED);
                            break;

                        default:
                            break;
                    }
                    mMap.addCircle(circle);
                }

                if (latLng != null) {

                    //Create MarkerOptions object
                    final MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

                    marker = mMap.addMarker(markerOptions);
                }

            }
        });

    }

}
