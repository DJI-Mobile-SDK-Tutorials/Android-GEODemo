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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.flyzone.FlyZoneInformation;
import dji.common.flightcontroller.flyzone.FlyZoneState;
import dji.common.flightcontroller.flyzone.SubFlyZoneInformation;
import dji.common.flightcontroller.flyzone.SubFlyZoneShape;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.midware.data.forbid.FlyForbidProtocol;
import dji.midware.data.model.P3.DataOsdGetPushCommon;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getName();

    private GoogleMap mMap;
    private ArrayList<Integer> unlockableIds = new ArrayList<Integer>();

    protected TextView mConnectStatusTextView;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnUnlock;
    private Button btnGetUnlock;
    private Button btnGetSurroundNFZ;
    private Button btnUpdateLocation;

    private TextView loginStatusTv;
    private TextView flyZonesTv;

    private Marker marker;
    private FlightController mFlightController = null;

    private MarkerOptions markerOptions = new MarkerOptions();
    private LatLng latLng;
    private double droneLocationLat = 181, droneLocationLng = 181;
    private ArrayList<Integer> unlockFlyZoneIds = new ArrayList<Integer>();
    private final int limitFillColor = Color.HSVToColor(120, new float[] {0, 1, 1});
    private final int limitCanUnlimitFillColor = Color.argb(40, 0xFF, 0xFF, 0x00);
    private FlyfrbBasePainter painter = new FlyfrbBasePainter();

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

        DJISDKManager.getInstance().getFlyZoneManager()
                .setFlyZoneStateCallback(new FlyZoneState.Callback() {
                    @Override
                    public void onUpdate(FlyZoneState status) {
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
        loginAccount();
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

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void updateTitleBar() {
        if (mConnectStatusTextView == null) return;
        boolean ret = false;
        BaseProduct product = GEODemoApplication.getProductInstance();
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
                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;
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
        btnUpdateLocation.setOnClickListener(this);

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {

                loginStatusTv.setText(UserAccountManager.getInstance().getUserAccountState().name());

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

                UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                        new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                            @Override
                            public void onSuccess(final UserAccountState userAccountState) {
                                showToast(userAccountState.name());
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loginStatusTv.setText(userAccountState.name());
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

                UserAccountManager.getInstance().logoutOfDJIUserAccount(new CommonCallbacks.CompletionCallback() {
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
                                            DJISDKManager.getInstance().getFlyZoneManager().unlockFlyZones(unlockFlyZoneIds, new CommonCallbacks.CompletionCallback() {
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

                DJISDKManager.getInstance().getFlyZoneManager().getUnlockedFlyZones(new CommonCallbacks.CompletionCallbackWith<List<FlyZoneInformation>>(){
                    @Override
                        public void onSuccess(final List<FlyZoneInformation> flyZoneInformations) {
                        showToast("Get Unlock NFZ success");
                        showSurroundFlyZonesInTv(flyZoneInformations);
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
                if (latLng != null) {

                    //Create MarkerOptions object
                    final MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
                    marker = mMap.addMarker(markerOptions);
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
                break;

        }
    }

    private void initFlightController() {

        if (isFlightControllerSupported()) {
            mFlightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState
                                             djiFlightControllerCurrentState) {
                    if (mMap != null) {
                        droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                        droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
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
                    if (pos != null) {

                        //Create MarkerOptions object
                        final MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(pos);
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

                        marker = mMap.addMarker(markerOptions);
                    }

                }
            }
        });
    }

    private boolean isFlightControllerSupported() {
        return DJISDKManager.getInstance().getProduct() != null &&
                DJISDKManager.getInstance().getProduct() instanceof Aircraft &&
                ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController() != null;
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

        DJISDKManager.getInstance().getFlyZoneManager().getFlyZonesInSurroundingArea(new CommonCallbacks.CompletionCallbackWith<ArrayList<FlyZoneInformation>>() {
            @Override
            public void onSuccess(ArrayList<FlyZoneInformation> flyZones) {
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

    private void showSurroundFlyZonesInTv(final List<FlyZoneInformation> flyZones) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuffer sb = new StringBuffer();
                for (FlyZoneInformation flyZone : flyZones) {
                    if (flyZone != null && flyZone.getCategory() != null){

                        sb.append("FlyZoneId: ").append(flyZone.getFlyZoneID()).append("\n");
                        sb.append("Category: ").append(flyZone.getCategory().name()).append("\n");
                        sb.append("Latitude: ").append(flyZone.getCoordinate().getLatitude()).append("\n");
                        sb.append("Longitude: ").append(flyZone.getCoordinate().getLongitude()).append("\n");
                        sb.append("FlyZoneType: ").append(flyZone.getFlyZoneType().name()).append("\n");
                        sb.append("Radius: ").append(flyZone.getRadius()).append("\n");
                        if(flyZone.getShape() != null){
                            sb.append("Shape: ").append(flyZone.getShape().name()).append("\n");
                        }
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

    private void updateFlyZonesOnTheMap(final ArrayList<FlyZoneInformation> flyZones) {
        if (mMap == null) {
            return;
        }
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMap.clear();
                if (latLng != null) {

                    //Create MarkerOptions object
                    final MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

                    marker = mMap.addMarker(markerOptions);
                }
                for (FlyZoneInformation flyZone : flyZones) {

                    //print polygon
                    if(flyZone.getSubFlyZones() != null){
                        SubFlyZoneInformation[] polygonItems = flyZone.getSubFlyZones();
                        int itemSize = polygonItems.length;
                        for(int i = 0; i != itemSize; ++i) {
                            if(polygonItems[i].getShape() == SubFlyZoneShape.POLYGON) {
                                DJILog.d("updateFlyZonesOnTheMap", "sub polygon points " + i + " size: " + polygonItems[i].getVertices().size());
                                DJILog.d("updateFlyZonesOnTheMap", "sub polygon points " + i + " category: " + flyZone.getCategory().value());
                                DJILog.d("updateFlyZonesOnTheMap", "sub polygon points " + i + " limit height: " + polygonItems[i].getMaxFlightHeight());
                                addPolygonMarker(polygonItems[i].getVertices(), flyZone.getCategory().value(), polygonItems[i].getMaxFlightHeight());
                            }
                            else if (polygonItems[i].getShape() == SubFlyZoneShape.CYLINDER){
                                LocationCoordinate2D tmpPos = polygonItems[i].getCenter();
                                double subRadius = polygonItems[i].getRadius();
                                DJILog.d("updateFlyZonesOnTheMap", "sub circle points " + i + " coordinate: " + tmpPos.getLatitude() + "," + tmpPos.getLongitude());
                                DJILog.d("updateFlyZonesOnTheMap", "sub circle points " + i + " radius: " + subRadius);

                                CircleOptions circle = new CircleOptions();
                                circle.radius(subRadius);
                                circle.center(new LatLng(tmpPos.getLatitude(),
                                        tmpPos.getLongitude()));
                                switch (flyZone.getCategory()) {
                                    case WARNING:
                                        circle.strokeColor(Color.GREEN);
                                        break;
                                    case ENHANCED_WARNING:
                                        circle.strokeColor(Color.BLUE);
                                        break;
                                    case AUTHORIZATION:
                                        circle.strokeColor(Color.YELLOW);
                                        unlockableIds.add(flyZone.getFlyZoneID());
                                        break;
                                    case RESTRICTED:
                                        circle.strokeColor(Color.RED);
                                        break;

                                    default:
                                        break;
                                }
                                mMap.addCircle(circle);
                            }
                        }
                    }
                    else {
                        CircleOptions circle = new CircleOptions();
                        circle.radius(flyZone.getRadius());
                        circle.center(new LatLng(flyZone.getCoordinate().getLatitude(), flyZone.getCoordinate().getLongitude()));
                        switch (flyZone.getCategory()) {
                            case WARNING:
                                circle.strokeColor(Color.GREEN);
                                break;
                            case ENHANCED_WARNING:
                                circle.strokeColor(Color.BLUE);
                                break;
                            case AUTHORIZATION:
                                circle.strokeColor(Color.YELLOW);
                                unlockableIds.add(flyZone.getFlyZoneID());
                                break;
                            case RESTRICTED:
                                circle.strokeColor(Color.RED);
                                break;

                            default:
                                break;
                        }
                        mMap.addCircle(circle);
                    }
                }

            }
        });

    }


    /**
     * 新版多边形限飞区使用
     * @param area_level
     */
    private void addPolygonMarker(List<LocationCoordinate2D> polygonPoints, int area_level, int height) {
        if(polygonPoints == null) {
            return;
        }

        ArrayList<LatLng> points = new ArrayList<>();

        for (LocationCoordinate2D point : polygonPoints) {
            points.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }
        int fillColor = limitFillColor;
        if(painter.getmHeightToColor().get(height) != null) {
            fillColor = painter.getmHeightToColor().get(height);
        }
        else if(area_level == FlyForbidProtocol.LevelType.CAN_UNLIMIT.value()) {
            fillColor = limitCanUnlimitFillColor;
        } else if(area_level == FlyForbidProtocol.LevelType.STRONG_WARNING.value() || area_level == FlyForbidProtocol.LevelType.WARNING.value()) {
            fillColor = getResources().getColor(R.color.gs_home_fill);
        }
        Polygon plg = mMap.addPolygon(new PolygonOptions().addAll(points)
                .strokeColor(painter.getmColorTransparent())
                .fillColor(fillColor));

    }

}
