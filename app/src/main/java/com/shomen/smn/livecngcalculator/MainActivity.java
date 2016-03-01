package com.shomen.smn.livecngcalculator;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener,
        GoogleMap.OnMapClickListener,GoogleMap.OnMapLongClickListener,GoogleMap.OnMarkerClickListener,
        View.OnClickListener{

    private final String TAG = this.getClass().getSimpleName();

    private final double CHITTAGONG_LAT = 22.348079;
    private final double CHITTAGONG_LN = 91.812229;

    private GoogleMap mMap;
    private TextView tv_dist,tv_cost,tv_btn_state,tv_more;
    private EditText et_wail_min;
    private ImageView iv_curr_loc;
    private AutoCompleteTextView auto_comp_tv_search;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private FrameLayout left_drawer;
    private Button btn_service,btn_live_path_dist;
    private ProgressDialog pDialog;

    private static final int ERROR_DIALOG_REQUEST = 9001;
    private GoogleApiClient googleApiClient;
    protected Location lastLocation = null;
    protected LocationRequest locationRequest;

    private List<LatLng> listOfMarkerPoints = new ArrayList<>();
    private PolylineOptions polyLineOptions = new PolylineOptions();
    private List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
    private JSONObject g_response = new JSONObject();

    private double mDistance = 0;

    private long duration = 300;
    private int loc_btn_counter = 0;

    private BackgroundService backgroundService;
    private boolean isBound = false;
    private Timer timer;
    private MyTimerTask myTimerTask;
    private BasicData basicData ;

    private InternalFileController rw ;

    private JsonParser parser = new JsonParser();

    private AutoCompleteAdapter mAdapter;

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,IBinder service) {
            BackgroundService.ServiceBinder binder = (BackgroundService.ServiceBinder) service;
            backgroundService = binder.getService();
            isBound = true;
            Log.d(TAG, "service connected");

            timer = new Timer();
            myTimerTask = new MyTimerTask();
            timer.schedule(myTimerTask, 2000, 2000);

        }

        public void onServiceDisconnected(ComponentName className) {
            isBound = false;
            Log.d(TAG,"service disconnected");
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"inside OnCreate ");

        if( playServicesOk() ){

            basicData = new BasicData(this);
            if(basicData.isSet() == false){
                basicData.insertBaseData(40,2,20,2);
            }
            setContentView(R.layout.activity_map_sliding_drawer);
            initMap();
            inItComponent();
            setListenerToComponents();
            buildGoogleApiClient();
            inItViewData();
            rw = new InternalFileController(this);

        }else{
            setContentView(R.layout.activity_main);
        }

    }

    private void inItComponent(){
        tv_dist = (TextView) findViewById(R.id.dt_tv_km_in_text);
        tv_cost = (TextView) findViewById(R.id.dt_tv_cost_in_tk);
        tv_more = (TextView) findViewById(R.id.tv_more);
        et_wail_min = (EditText) findViewById(R.id.dt_et_waiting);
        iv_curr_loc = (ImageView) findViewById(R.id.btn_current_location);

        left_drawer = (FrameLayout) findViewById(R.id.left_drawer);

        btn_live_path_dist = (Button) findViewById(R.id.btn_live_path_dist);
        btn_live_path_dist.setEnabled(false);
        tv_btn_state = (TextView) findViewById(R.id.tv_btn_state);
        btn_service = (Button) findViewById(R.id.button3);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_cast_dark,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };


        if (isMyServiceRunning(BackgroundService.class)) {
            Toast.makeText(this,"service running ",Toast.LENGTH_SHORT).show();
            tv_btn_state.setText("Stop Live Track");
            btn_service.setBackgroundResource(R.drawable.btn_state);
            btn_service.setText("Stop Live Track");

        } else {
            Toast.makeText(this,"service not running ",Toast.LENGTH_SHORT).show();
            tv_btn_state.setText("Start Live Track");
            btn_service.setBackgroundResource(R.drawable.btn_state);
            btn_service.setText("Start Live Track");
        }

        serviceButtonActivator();

        auto_comp_tv_search = (AutoCompleteTextView) findViewById(R.id.auto_comp_tv_search);
        mAdapter = new AutoCompleteAdapter( this );
        auto_comp_tv_search.setAdapter(mAdapter);

        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading...");
        pDialog.setCancelable(false);

    }

    private void setListenerToComponents(){
        iv_curr_loc.setOnClickListener(this);
        et_wail_min.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG, "beforeTextChanged");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "afterTextChanged");
                if (s.length() < 5)
                    calculate();
                else {
                    s.clear();
                    Toast.makeText(MainActivity.this, "Value must be less than 5 character", Toast.LENGTH_SHORT).show();
                }
            }
        });
        et_wail_min.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                et_wail_min.setFocusable(true);
                return false;
            }
        });

        left_drawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        btn_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMyServiceRunning(BackgroundService.class)) {
                    Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                    unbindService(myConnection);
                    isBound = false;
                    stopService(intent);
                    timer.cancel();
                    BackgroundService.IS_SERVICE_RUNNING = false;
                    btn_service.setText("Start Live Track");
                    btn_service.setBackgroundResource(R.drawable.btn_state);
                } else {
                    Intent intent = new Intent(MainActivity.this, BackgroundService.class);
                    intent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startService(intent);
                    isBound = bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

                    BackgroundService.IS_SERVICE_RUNNING = true;
                    btn_service.setText("Stop Live Track");
                    btn_service.setBackgroundResource(R.drawable.btn_state);
                    btn_live_path_dist.setEnabled(true);
                }

            }
        });

        btn_live_path_dist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateLivePathDistance();
            }
        });

        tv_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MoreActivity.class);
                startActivity(intent);
            }
        });

        auto_comp_tv_search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AutoCompletePlaceModel place = (AutoCompletePlaceModel) parent.getItemAtPosition(position);
                findPlaceById(place.getId());
                auto_comp_tv_search.setText("");
            }
        });

    }

    private void findPlaceById( String id ) {
        if( TextUtils.isEmpty(id) || googleApiClient == null || !googleApiClient.isConnected() )
            return;

        Places.GeoDataApi.getPlaceById(googleApiClient, id ) .setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (places.getStatus().isSuccess()) {
                    Place place = places.get(0);
                    Toast.makeText(MainActivity.this,"place lastLocation "+place.getLatLng().toString(),Toast.LENGTH_SHORT).show();
                    mAdapter.clear();
                    goToLocation(place.getLatLng());
                }
                places.release();
            }
        });
    }

    private void serviceButtonActivator(){
        if(listOfMarkerPoints.size()>=2){
            btn_service.setEnabled(true);
        }else {
            btn_service.setEnabled(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(googleApiClient != null)
            googleApiClient.connect();
        Log.d(TAG, "inside onStart ");

        if(isMyServiceRunning(BackgroundService.class)){
            Intent intent = new Intent(MainActivity.this, BackgroundService.class);
            bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
            isBound = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "inside onResume");

        if(isMyServiceRunning(BackgroundService.class)){

            cleanAll();

            JSONObject obj = rw.readFile();
            try {
                g_response = obj.getJSONObject("google_data");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            listOfMarkerPoints.addAll(parser.parseFileData_U(obj));

            serviceButtonActivator();
            btn_live_path_dist.setEnabled(true);

            routes.addAll(parser.parseFileData_G(obj));

            Log.d(TAG, "inside onResume ll" + listOfMarkerPoints.toString());
            Log.d(TAG, "inside onResume rout " + routes.toString());

            addMarkerOnMap();
            getRouteData();

            goToLocation(listOfMarkerPoints.get(0));
        }
    }

    public boolean playServicesOk(){

        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if( isAvailable == ConnectionResult.SUCCESS ){
            return  true;

        }else if( GooglePlayServicesUtil.isUserRecoverableError(isAvailable)){

            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else {
            Toast.makeText(this,"Can't connect to mapping services.",Toast.LENGTH_SHORT).show();
        }

        return  false;
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi( Places.PLACE_DETECTION_API )
                .build();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        startLocationUpdates();
    }

    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    private boolean initMap() {
        if (mMap == null) {
            SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mMap = mapFrag.getMap();
            mMap.setOnMapClickListener(this);
            mMap.setOnMarkerClickListener(this);
            mMap.setOnMapLongClickListener(this);

            goToLocation(new LatLng(CHITTAGONG_LAT, CHITTAGONG_LN));

        }
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG," google api is connected ");

        if( mAdapter != null )
            mAdapter.setGoogleApiClient(googleApiClient);

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastLocation != null) {
//            Toast.makeText(this, "Lat Long :"+lastLocation.getLatitude()+" "+lastLocation.getLongitude(), Toast.LENGTH_LONG).show();
//            goToLocation(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()));
            createLocationRequest();
        } else {
//            Toast.makeText(this, "No lastLocation detected. Make sure lastLocation is enabled on the device.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient != null)
            googleApiClient.disconnect();
        Log.d(TAG, "inside onPause");
        if(isMyServiceRunning(BackgroundService.class)){
            saveIntoFile();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "inside onStop");
        if(isMyServiceRunning(BackgroundService.class)){
            unbindService(myConnection);
            isBound = false;
            timer.cancel();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(googleApiClient != null)
            googleApiClient.disconnect();
        Log.d(TAG, "inside onDestroy");
    }

    @Override
    public void finish() {
        super.finish();
        Log.d(TAG, "inside finish");
    }

    private void drawRoute(List<LatLng> points){

        polyLineOptions.addAll(points);
        polyLineOptions.width(5);
        polyLineOptions.color(Color.BLUE);
        mMap.addPolyline(polyLineOptions);
    }

    private void addMarkerOnMap(){
        for(int i=0;i<listOfMarkerPoints.size();i++){
            MarkerOptions options = new MarkerOptions()
                    .anchor(.5f, .5f)
                    .position(listOfMarkerPoints.get(i));
            if(i>1){
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }
            mMap.addMarker(options);
        }
    }

    private void goToLocation(LatLng ll){
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 15);
        mMap.animateCamera(update);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "lastLocation changed");
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if(isMyServiceRunning(BackgroundService.class)){
            Toast.makeText(this,"Live tracking is on please stop it",Toast.LENGTH_SHORT).show();
            return;
        }
        storeMarkerAndPointsToList(latLng);

    }

    private void storeMarkerAndPointsToList(LatLng latLng){
        mMap.clear();
        listOfMarkerPoints.add(latLng);
        Log.d(TAG, "size of  listOfMarkerPoints" + "  " + listOfMarkerPoints.size());
        addMarkerOnMap();
        serviceButtonActivator();

        /*for(int i=0;i<listOfMarkerPoints.size();i++){
            MarkerOptions options = new MarkerOptions()
                    .anchor(.5f, .5f)
                    .position(listOfMarkerPoints.get(i));
            if(i>1){
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }
            mMap.addMarker(options);
        }*/
        if(listOfMarkerPoints.size()>=2){
            requestData(getDirectionsUrl(listOfMarkerPoints.get(0), listOfMarkerPoints.get(1)));
        }
    }

    public void requestData(String uri){

        if(!isInternetConnected(this)){
            Toast.makeText(this,"Please activate internet",Toast.LENGTH_SHORT).show();
            return;
        }
        pDialog.show();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
            uri,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Success " + response.toString());
                    try {
                        g_response = response;
                        routes = parser.parse(response);
                        getRouteData();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    pDialog.hide();

                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.d(TAG + error.getMessage());
                    Log.d(TAG, "Error: " + error.getMessage() );
                    Toast.makeText(MainActivity.this,"Something weired happened please try again....",Toast.LENGTH_SHORT).show();
                    pDialog.hide();
                }
            });

/*        int socketTimeout = 10000;//10 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(policy);*/

        AppController.getInstance().addToRequestQueue(request, uri);

        return;
    }

    private void inItViewData(){
        et_wail_min.setText("0");
        tv_cost.setText("0.0 tk");
        tv_dist.setText("0.0 km");
    }

    private void getRouteData(){

        List<LatLng> points = new ArrayList<>();

        for(int i=0;i<routes.size();i++){
            points = new ArrayList<LatLng>();
            polyLineOptions = new PolylineOptions();

            // Fetching i-th route
            List<HashMap<String, String>> path = routes.get(i);

            // Fetching all the points in i-th route
            Log.d(TAG, "variable route :"+routes.toString());
            for(int j=0;j<path.size();j++){
                HashMap<String,String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);
                points.add(position);
            }

        }
        mDistance = Math.round(parser.getDistance()*100.0)/100000.0;
        inItViewData();
        calculate();
        drawRoute(points);
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        String sensor = "sensor=false";

        String waypoints = "";
        for(int i=2;i<listOfMarkerPoints.size();i++){
            LatLng point  =  listOfMarkerPoints.get(i);
            if(i==2)
                waypoints = "waypoints=";
            waypoints += point.latitude + "," + point.longitude + "|";
        }

        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+waypoints;

        String output = "json";

        String url = Constants.URL.GOOGLE_API_URL+output+"?"+parameters;
        Log.d(TAG, "url :" + url);

        return url;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if(isMyServiceRunning(BackgroundService.class)){
            Toast.makeText(this,"Live tracking is on please stop it first to start a new session",Toast.LENGTH_SHORT).show();
            return true;
        }

        if(listOfMarkerPoints.get(0).equals(marker.getPosition())||listOfMarkerPoints.get(1).equals(marker.getPosition())){
            Toast.makeText(this," source or destination can't be removed individualy, to clear the map press and hold the map ",Toast.LENGTH_SHORT).show();
        }else{
            removeWayPoint( marker);
        }
        return true;
    }

    private void removeWayPoint(Marker marker){
        mMap.clear();
        listOfMarkerPoints.remove(marker.getPosition());
        serviceButtonActivator();
        addMarkerOnMap();
        requestData(getDirectionsUrl(listOfMarkerPoints.get(0), listOfMarkerPoints.get(1)));
    }

    private void calculate(){
        double cost = 0;
        if(mDistance <= basicData.get_BASE_DISTANCE())
            cost = basicData.get_BASE_CHARGE();
        else
            cost = (mDistance-basicData.get_BASE_DISTANCE())*basicData.get_PER_KM_CHARGE()+basicData.get_BASE_CHARGE();

        if(!et_wail_min.getText().toString().isEmpty() && isInteger(et_wail_min.getText().toString().trim())){
            cost = cost+Integer.parseInt(et_wail_min.getText().toString().trim())*basicData.get_PER_MIN_WAIT_CHARGE();
        }
        tv_dist.setText(new DecimalFormat("##.##").format(mDistance) + " km");
        tv_cost.setText((new DecimalFormat("##.##").format(cost) + " tk"));
    }

    public boolean isInteger( String input ){
        try{
            Integer.parseInt( input );
            return true;
        }catch( Exception e){
            return false;
        }
    }

    public boolean isFloat( String input ){
        try{
            Float.parseFloat( input );
            return true;
        }catch( Exception e){
            return false;
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if(isMyServiceRunning(BackgroundService.class)){
            Toast.makeText(this,"Currently live tracking is on please stop live tracking first",Toast.LENGTH_SHORT).show();
        }else{
            cleanAll();
            serviceButtonActivator();
        }
    }

    @Override
    public void onClick(View v) {

        if(isMyServiceRunning(BackgroundService.class)){
            Toast.makeText(this,"Live tracking is on please stop it",Toast.LENGTH_SHORT).show();
            return;
        }

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        loc_btn_counter++;
        if(lastLocation == null){
            if(loc_btn_counter > 3)
                Toast.makeText(this, "No Location detected. Make sure gps is enabled on the device.It may take a while depending on your device's GPS", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, "No Location detected. Make sure gps is enabled on the device.", Toast.LENGTH_LONG).show();

        }else{
            loc_btn_counter = 0;
            cleanAll();
            goToLocation(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
            storeMarkerAndPointsToList(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
        }

        return;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    drawLiveTracePath();
                }
            });
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void saveIntoFile(){
        Log.d(TAG,"saveIntoFile method called");

        try {
            JSONObject data = new JSONObject();
            JSONObject user_data = new JSONObject();
            JSONArray user_lat = new JSONArray();
            JSONArray user_lon = new JSONArray();
            for(LatLng m:listOfMarkerPoints){
                user_lat.put(m.latitude) ;
                user_lon.put(m.longitude);
            }
            user_data.put("user_lat",user_lat);
            user_data.put("user_lon",user_lon);
            data.put("user_data",user_data);
            data.put("google_data",g_response);
            Log.d(TAG,"inside saveintoFile "+data.toString());
            rw.createFile(data);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "saveIntoFile method called something wrong happened "+e.toString());
        }
    }

    private void drawLiveTracePath(){
       /* LatLng dest = new LatLng(22.347983,91.833822);
        LatLng source = new LatLng(22.348716,91.814291);

        PolylineOptions polyOpt = new PolylineOptions().color(Color.GREEN).width(5);
        Log.d(TAG,"inside drawlivetrace size of m "+m.size());
        polyOpt.add(source);
        polyOpt.add(dest);*/
        Log.d(TAG, "inside drawLivePath..size " + backgroundService.getLatitudesFromService().size());
        PolylineOptions polyOpt = new PolylineOptions().color(Color.GREEN).width(5);
        for(int i =0; i< backgroundService.getLatitudesFromService().size(); i++){
            polyOpt.add(new LatLng(backgroundService.getLatitudesFromService().get(i), backgroundService.getLongitudesFromService().get(i))) ;
        }
        mMap.addPolyline(polyOpt);
    }

    private void cleanAll(){
        mMap.clear();
        listOfMarkerPoints.clear();
        routes.clear();
        mDistance = 0.0;
        inItViewData();
    }

    private void calculateLivePathDistance(){
        //22.360643, 91.828505
/*        LatLng dest = new LatLng(22.359037,91.826124);
        LatLng source = new LatLng(22.359954,91.825855);
        float results[] = new float[1];
        Location.distanceBetween(source.latitude,source.longitude,dest.latitude,dest.longitude,results);
        Log.d(TAG,"insdie calculateLivePathDist result =  "+results[0]);*/

        if(backgroundService.getLatitudesFromService().size()>0){
            float results[] = new float[1];
            float totalDist = 0.000f;

            for (int i=0;i< backgroundService.getLatitudesFromService().size()-1;i++){
                LatLng s = new LatLng(backgroundService.getLatitudesFromService().get(i), backgroundService.getLongitudesFromService().get(i));
                LatLng e = new LatLng(backgroundService.getLatitudesFromService().get(i+1), backgroundService.getLongitudesFromService().get(i+1));

                if(s.latitude == e.latitude && s.longitude == e.longitude){
                    Log.d(TAG, "inside calculateLivePathD s lat ln is equals to e lat ln" );
                }else{
                    Location.distanceBetween(s.latitude,s.longitude,e.latitude,e.longitude,results);

                    totalDist = totalDist+results[0];
                    String v = new DecimalFormat("#.###").format(results[0]);
                    Log.d(TAG, "inside calculateLivePathDist result[0] =  " + results[0]+" after formatting "+v);
                }
            }
            String totalDist_str = new DecimalFormat("##.###").format(totalDist/1000);
            Toast.makeText(this,"currently you have visited "+totalDist_str+" km",Toast.LENGTH_SHORT).show();

        }
    }

    private static boolean isInternetConnected (Context ctx) {
        ConnectivityManager connectivityMgr = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        // Check if wifi or mobile network is available or not. If any of them is
        // available or connected then it will return true, otherwise false;
        if (wifi != null) {
            if (wifi.isConnected()) {
                return true;
            }
        }
        if (mobile != null) {
            if (mobile.isConnected()) {
                return true;
            }
        }
        return false;
    }

}
