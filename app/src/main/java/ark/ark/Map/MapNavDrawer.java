package ark.ark.Map;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import ark.ark.Authentication.ARK_auth;
import ark.ark.Groups.CurrentUser;
import ark.ark.Groups.Friend;
import ark.ark.Groups.Group;
import ark.ark.PermissionUtils;
import ark.ark.R;
import ark.ark.UserLocation.LocationSingleton;

/**
 * Map + Nav Drawer Class
 */
public class MapNavDrawer extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        Observer {

    private GoogleMap mMap;
    private UiSettings uiSettings;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Marker mWaypoint = null;
    private Geocoder geocoder;
    private MapWaypoint undobk;
    String otheremail;
    String useremail;
    private LocationSingleton mCurrentLocation;
    private BottomSheet bs;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mylocation;
    private HashMap<String, Marker> mGroup;
    private HashMap<Integer, String> idToEmail = new HashMap<>();
    private CurrentUser curruser;
    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;

    // Stuff for Zengster for navigation drawer
    private ImageView profilePicture, headerImage;
    private TextView currentUserName, currentUserGroup;
    private View drawerHeader;
    boolean isPopulated;
    int numGroupMembers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_nav_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                initiateDrawer();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Assorted drawer code
        drawerHeader = navigationView.getHeaderView(0);
        currentUserName = (TextView) drawerHeader.findViewById(R.id.userEmail);
        currentUserGroup = (TextView) drawerHeader.findViewById(R.id.activeGroup);
        profilePicture = (ImageView) drawerHeader.findViewById(R.id.profileImg);
        initiateDrawer();


        // Geocoder
        geocoder = new Geocoder(this);

        // Bottom Sheet Initialize
        View bs_view = findViewById(R.id.bottom_sheet);
        bs = new BottomSheet(bs_view, geocoder);

        bs_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bs.is_collapsed()) {
                    bs.set_expanded();
                } else {
                    bs.set_collapsed();
                }
            }
        });

        bs.get_bsb().setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (bs.is_hidden()) {
                    changeFAB(fab, R.drawable.map_marker_radius, R.color.colorPrimaryDark);
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });

        // Map Initiate
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bs.is_collapsed()) {
                    bs.set_expanded();
                } else if (mWaypoint != null) {
                    bs.set_place_mode(findViewById(android.R.id.content),
                            (MapWaypoint) mWaypoint.getTag(), get_location());
                    bs.set_collapsed();
                } else {
                    selectPlace(view);
                }
            }
        });

        //Google Places Initialise
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);
        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        //Initiate RequestQueue
        // Instantiate the RequestQueue.
        final TextView mTextView = (TextView) findViewById(R.id.textView2);
        RequestQueue queue = Volley.newRequestQueue(this);
        useremail = ARK_auth.fetchUserEmail(this);
        mTextView.setText(useremail);

//        if (useremail == "user1@user1.com") {
//            otheremail = "user2@user2.com";
//        } else {
//            otheremail = "user1@user1.com";
//        }
//        String url = "http://52.65.97.117/locations/show?email=" + otheremail;
//
//        // Request a json response from the provided URL.
//        JsonObjectRequest jsObjRequest = new JsonObjectRequest
//                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
//
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        try {
//                            JSONObject loc = response.getJSONObject("location");
//                            set_person_marker(new LatLng(loc.getDouble("lat"), loc.getDouble("lng")), otheremail);
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                }, new Response.ErrorListener() {
//
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        // TODO Auto-generated method stub
//
//                    }
//                });
//        // Add the request to the RequestQueue.
//        queue.add(jsObjRequest);

        curruser = CurrentUser.getInstance();
        curruser.addObserver(this);

        mGroup = new HashMap<>();

        mCurrentLocation = LocationSingleton.getInstance();
        mCurrentLocation.addObserver(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }


    private void initiateDrawer() {
        currentUserName.setText(CurrentUser.getInstance().getEmail());
        currentUserGroup.setText(CurrentUser.getInstance().getActiveGroup().getName());

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        final Menu menu = navigationView.getMenu();
        numGroupMembers = 0;
        int j = 0;

        Context context = getApplicationContext();
        CharSequence text = "Group member list refreshed!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);

        if(isPopulated == true) {
            for(int key: idToEmail.keySet()) {
                menu.removeItem(key);
            }
            toast.show();
        }

        for(Friend tempFriend: CurrentUser.getInstance().getActiveGroup().getFriends().values()) {
            menu.add(0, j, 0, tempFriend.getEmail()).setIcon(R.drawable.ic_person_black_24dp);
            idToEmail.put(j, tempFriend.getEmail());
            numGroupMembers++;
            j++;
            isPopulated = true;
        }

        j = 0;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (bs.is_expanded()) {
            bs.set_collapsed();
        } else if (bs.is_collapsed()) {
            bs.set_hidden();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_nav_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Context context = getApplicationContext();
        CharSequence text = "Hello toast!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Context context = getApplicationContext();
        CharSequence text = "HashMap search successful.";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);

        //noinspection SimplifiableIfStatement
        for(int friendID: idToEmail.keySet()) {
            if (id == friendID) {
                LatLng moveCamera = new LatLng(
                        mGroup.get(idToEmail.get(id)).getPosition().latitude,
                        mGroup.get(idToEmail.get(id)).getPosition().longitude
                );
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(moveCamera, 15));
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        uiSettings = mMap.getUiSettings();
        uiSettings.setCompassEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        enableMyLocation();

        Group group = curruser.getActiveGroup();
        if (group != null){
            if (group.getFriends() != null){
                for(Map.Entry<String, Friend> entry : group.getFriends().entrySet()){
                    LatLng loc = new LatLng(entry.getValue().getLocation().getLatitude(),
                            entry.getValue().getLocation().getLongitude());
                    set_person_marker(loc,entry.getValue().getEmail());
                }
            }
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (bs.is_expanded()) {
            bs.set_collapsed();
        } else {
            fab.setImageResource(R.drawable.map_marker_radius);
            bs.set_hidden();
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if (marker.getTag() instanceof MapWaypoint) {
            bs.set_place_mode(findViewById(android.R.id.content), (MapWaypoint) marker.getTag(),
                    get_location());
            changeFAB(fab, R.drawable.map_marker_radius, R.color.colorPrimaryDark);
        } else {
            if (mWaypoint != null){
                bs.set_person_mode(findViewById(android.R.id.content), marker, get_location(),
                        (MapWaypoint) mWaypoint.getTag(),(String) marker.getTag());
            }
            else{
                bs.set_person_mode(findViewById(android.R.id.content), marker, get_location(),
                        null, (String) marker.getTag());
            }

            changeFAB(fab, R.drawable.ic_person_black_24dp, R.color.cyan);
        }

        bs.set_collapsed();
        return true;
    }

    /**
     * Uses Google Places API to select a waypoint.
     * @param view
     */
    public void selectPlace(View view) {
        int PLACE_PICKER_REQUEST = 1;

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
            // Start the Intent by requesting a result, identified by a request code.
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);

        } catch (GooglePlayServicesRepairableException e) {
            //TODO
        } catch (GooglePlayServicesNotAvailableException e) {
            //TODO
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int PLACE_PICKER_REQUEST = 1;
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                setWaypoint(place);
            }
        }
    }

    /**
     * Sets a waypoint for the group
     * @param place
     */
    public void setWaypoint(Place place) {

        if (mWaypoint != null) {
            mWaypoint.remove();
            mWaypoint = null;
        }

        add_waypoint_marker(place.getLatLng(), useremail + "'s Hotspot",
                new MapWaypoint(useremail + "'s Hotspot", place.getLatLng(),
                        place.getName().toString(), place.getAddress().toString()));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
        bs.set_collapsed();
    }

    public void add_waypoint_marker(LatLng pos, String title, MapWaypoint mw) {
        mWaypoint = mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(title)
        );
        mWaypoint.setTag(mw);

        bs.set_place_mode(findViewById(android.R.id.content), mw, get_location());
    }

    /**
     * Deletes the current waypoint
     * @param view
     */
    public void deleteWaypoint(View view) {
        if (mWaypoint != null) {
            undobk = (MapWaypoint) mWaypoint.getTag();
            mWaypoint.remove();
            mWaypoint = null;
        }
        hide_fab();

        bs.set_hidden();

        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.bottom_sheet_layout),
                R.string.waypointremoved, Snackbar.LENGTH_LONG);
        mySnackbar.setAction(R.string.undo_string, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                add_waypoint_marker(undobk.getLocation(), undobk.getTitle(), undobk);

            }
        }).addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar mySnackbar, int event) {
                show_fab();
            }
        });
        mySnackbar.show();
    }


    public void set_person_marker(LatLng lat, String name) {
        Marker mPerson;
        Drawable d = getResources().getDrawable(R.drawable.ic_person_black_24dp);
        mPerson = mMap.addMarker(new MarkerOptions()
                .position(lat)
                .title(name +  "'s Location.")
                .icon(BitmapDescriptorFactory.fromBitmap(drawableToBitmap(d)))
        );
        mPerson.setTag(name);
        mGroup.put(name,mPerson);
    }

    public void changeFAB(FloatingActionButton fab, int icon, int colour) {
        fab.setImageResource(icon);
    }

    @Override
    public void update(Observable o, Object data) {
        HashMap<String, Friend> friendslist = null;
        Location location = get_location();

        if (curruser.getActiveGroup() != null) {
            friendslist = curruser.getActiveGroup().getFriends();
        }
        if (o == curruser){
            if (friendslist != null) {
                update_position(friendslist);
            }
        }
        if (o == mCurrentLocation){
            if (bs.is_place_mode()) {
                bs.set_distance_waypoint(findViewById(android.R.id.content),
                        new LatLng(location.getLatitude(), location.getLongitude()),
                        mWaypoint.getPosition()
                );
            }
        }

        if(bs.is_user_mode()){
            LatLng loc = null;
            if (friendslist != null){
                loc = new LatLng(friendslist.get(bs.get_active_user())
                        .getLocation().getLatitude(), friendslist.get(bs.get_active_user())
                        .getLocation().getLongitude());
            }
            if (mWaypoint != null){
                bs.set_distance_person(findViewById(android.R.id.content),
                        location, loc, (MapWaypoint) mWaypoint.getTag());
            }
            else{
                bs.set_distance_person(findViewById(android.R.id.content),
                        location, loc, null);
            }

        }




    }

    private void update_position(HashMap<String,Friend> f){
        Marker mPerson;
        Drawable d = getResources().getDrawable(R.drawable.ic_person_black_24dp);
        for(Map.Entry<String, Marker> entry : mGroup.entrySet()){
            LatLng currloc = new LatLng(f.get(entry.getValue()).getLocation().getLatitude(),
                    f.get(entry.getValue()).getLocation().getLongitude());
            if (!(entry.getValue().getPosition().equals(currloc))){
                entry.getValue().remove();
                mPerson = mMap.addMarker(new MarkerOptions()
                        .position(entry.getValue().getPosition())
                        .title(entry.getValue().getTitle())
                        .icon(BitmapDescriptorFactory.fromBitmap(drawableToBitmap(d)))
                );
                mPerson.setTag(null);
                mGroup.put(entry.getKey(),mPerson);
            }
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Location get_location() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mylocation = location;
                        }
                    }
                });
        return mylocation;
    }

    public void hide_fab(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
    }

    public void show_fab(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
    }
}
