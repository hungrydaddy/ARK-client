package ark.ark.Groups;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;

import ark.ark.Authentication.ARK_auth;
import ark.ark.Map.MapWaypoint;
import ark.ark.Profile.LoginActivity;
import ark.ark.R;
import ark.ark.ToastUtils;

/**
 * Created by khtin on 22/09/2017.
 */


/**
 * Utility class for requests for CurrentUser that handles requests to server
 */
public class UserRequestsUtil {

    /*
    Updates the groups the user is in and then puts locations into the active group
     */
    public static void initialiseCurrentUser(final Context context) {
        CurrentUser mUser = CurrentUser.getInstance();
        mUser.setIsInitialising();
        // logHead is the heading to add to the log for this function
        String logHead = "initialiseCurrentUser";

        Log.d(logHead,"function start");

        if (mUser.getEmail() != null) {
            RequestQueue queue = Volley.newRequestQueue(context);
            String server = "52.65.97.117";

            String path = "/group/show?";
            String requestURL = "http://" + server + path +"email="+ mUser.getEmail();

            Log.d(logHead,requestURL);

            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestURL,
                    new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            // after getting response, try reading the json
                            CurrentUser mUser = CurrentUser.getInstance();
//                            ToastUtils.showToast(response, context);
                            Log.i("Init Curr User",response);
                            try {
                                JSONObject res = new JSONObject(response);

                                if (res.getString("success").equals("ok")) {
                                    for (int i = 0; i < res.getJSONArray("groups").length(); i++) {
                                        Group g = new Group(res.getJSONArray("groups").getString(i), mUser.getEmail());
                                        mUser.addGroup(g);
                                        updateCurrentUserGroupName(g.getId(),context);
                                    }
                                    updateActiveGroupLocations(context);
                                    updateActiveGroupWaypoint(context);


                                } else {
                                    ToastUtils.showToast(res.getString("msg"), context);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ToastUtils.showToast("exception", context);
                                Log.d("initcurr", e.getMessage());

                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error handling
                    ToastUtils.showToast("Sorry, cannot connect to the server.", context);
                }
            });

            queue.add(stringRequest);
        } else {
//            ToastUtils.showToast("Location doesn't exist", context);
        }
    }

    public static void updateActiveGroupLocations(final Context context) {
        CurrentUser mUser = CurrentUser.getInstance();
        final Double defaultlat = -37.797044;
        final Double defaultlong = 144.961212;
        Log.d("UpdateGroupLoc","Called");
        if (mUser.getEmail() != null && mUser.getActiveGroup() != null) {
            RequestQueue queue = Volley.newRequestQueue(context);
            String server = "52.65.97.117";
            String path = "/group/locations?";
            String requestURL = "http://" + server + path +"group_id="+ mUser.getActiveGroup().getId();
            //ToastUtils.showToast(requestURL, context);

            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestURL,
                    new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            // after getting response, try reading the json
                            CurrentUser mUser = CurrentUser.getInstance();
                            //ToastUtils.showToast(response, context);
                            try {
                                JSONObject res = new JSONObject(response);

                                if (res.getString("success").equals("ok")) {
                                    JSONArray friendList = res.getJSONArray("groups");
                                    for (int i = 0; i < friendList.length(); i++) {
                                        JSONObject friend = friendList.getJSONObject(i);
                                        String email = friend.getString("email");
                                        Double lat, lng;
                                        if(!friend.isNull("location")){
                                            lat = friend.getJSONObject("location").getDouble("lat");
                                            lng = friend.getJSONObject("location").getDouble("lng");
                                        }
                                        else{
                                            lat = defaultlat;
                                            lng = defaultlong;
                                        }


                                        Location loc = new Location("Server");
                                        loc.setLatitude(lat);
                                        loc.setLongitude(lng);

                                        Friend f = new Friend(email);
                                        mUser.getActiveGroup().updateFriend(f);
                                        mUser.setActiveGroupLocation(email, loc);

                                        //ToastUtils.showToast(email, context);
                                    }

                                    if(!mUser.isInitiated()){
                                        mUser.setGroupInitiated();
                                        mUser.setIsInitiated();
                                    }
                                    //ToastUtils.showToast(mUser.getActiveGroup().toString(), context);


                                } else {
                                    ToastUtils.showToast(res.getString("msg"), context);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ToastUtils.showToast("exception", context);
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error handling
                    ToastUtils.showToast("Sorry, cannot connect to the server.", context);
                }
            });

            queue.add(stringRequest);
        } else {
//            ToastUtils.showToast("Location doesn't exist", context);
        }
    }

    public static void updateActiveGroupWaypoint(final Context context) {
        Log.d("UpdateWaypoint","Called");
        CurrentUser mUser = CurrentUser.getInstance();

        if (mUser.getEmail() != null && mUser.getActiveGroup() != null) {
            RequestQueue queue = Volley.newRequestQueue(context);
            String server = "52.65.97.117";

            String path = "/waypoint/show?";
            String requestURL = "http://" + server + path +"group_id="+ mUser.getActiveGroup().getId();
            //ToastUtils.showToast(requestURL, context);

            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestURL,
                    new Response.Listener<String>() {

                        @Override
                        public void onResponse(String response) {
                            // after getting response, try reading the json
                            CurrentUser mUser = CurrentUser.getInstance();
                            //ToastUtils.showToast(response, context);
                            try {
                                JSONObject res = new JSONObject(response);

                                if (res.getString("success").equals("ok")) {
                                    //ToastUtils.showToast("inside success", context);
                                    JSONObject waypoint = res.getJSONObject("waypoint").getJSONObject("waypoint");
                                    String name = res.getJSONObject("waypoint").getString("creator_nickname");
                                    Double lat = waypoint.getDouble("lat");
                                    Double lng = waypoint.getDouble("lng");
                                    String placename = waypoint.getString("place_name");
                                    String placeaddress = waypoint.getString("place_address");
                                    Boolean active = waypoint.getBoolean("active");

                                    mUser.updateActiveGroupWaypoint(lat, lng, name, placename,
                                            placeaddress, active);
                                    //ToastUtils.showToast(email, context);
                                    //ToastUtils.showToast(mUser.getActiveGroup().toString(), context);


                                } else {
                                    ToastUtils.showToast(res.getString("msg"), context);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ToastUtils.showToast(e.getMessage(), context);
                            }
                            if(!mUser.isInitiated()){
                                mUser.setIsInitiated();
                                mUser.setWaypointisInitiated();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error handling
                    ToastUtils.showToast("Sorry, cannot connect to the server.", context);
                }
            });

            queue.add(stringRequest);
        } else {
//            ToastUtils.showToast("Location doesn't exist", context);
        }
    }



    public static void sendActiveWaypointToServer(final Context context){
        CurrentUser mUser = CurrentUser.getInstance();
        RequestQueue queue = Volley.newRequestQueue(context);
        MapWaypoint waypoint = mUser.getActiveGroup().getWaypoint();
        String server = "52.65.97.117";
        String path = "/waypoint/create?";
        String requestURL;
        if (waypoint.getActive()==true){
            String place_name = "";
            String place_details = "";
            try {
                place_name = URLEncoder.encode(waypoint.getNam(),"UTF-8");
                place_details = URLEncoder.encode(waypoint.getDetails(),"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


            requestURL = "http://" + server + path +"email="+ mUser.getEmail()
                    +"&lat="+waypoint.getLocation().latitude+
                    "&lng="+waypoint.getLocation().longitude+
                    "&group_id="+mUser.getActiveGroup().getId()+
                    "&place_name="+ place_name+
                    "&place_address="+place_details+
                    "&active=true";
        }
        else{
            requestURL = "http://" + server + path +"email="+ mUser.getEmail()
                    +"&lat=0"+
                    "&lng=0"+
                    "&group_id="+mUser.getActiveGroup().getId()+
                    "&place_name=0"+
                    "&place_address=0"+
                    "&active=false";
        }
        //ToastUtils.showToast(requestURL, context);
        requestURL = requestURL.replaceAll("\\s","%20");
        Log.d("REQUESTURL:", requestURL);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, requestURL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // after getting response, try reading the json
                            try {
                                JSONObject res = new JSONObject(response);
                                if (res.getString("success").equals("ok")) {
                                    // iterate through the direct list to populate the convo list

                                    //ToastUtils.showToast("Congratulations! Your location is updated!", getApplicationContext());

                                } else {
                                    ToastUtils.showToast(res.getString("msg"), context);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ToastUtils.showToast(e.getMessage(), context);
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error handling
                    ToastUtils.showToast("Sorry, cannot connect to the server.", context);
                }
        });

        queue.add(stringRequest);

        // Request a string response from the requestURL.


    }


    public static void updateCurrentUserGroupName(String gID, final Context context){
        String groupID = gID;

        RequestQueue queue = Volley.newRequestQueue(context);

        String server ="52.65.97.117";
        String path = "/groupProfile/show?";

        String requestURL = "http://" + server + path +"group_id="+groupID;


        // Request a string response from the requestURL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // after getting response, try reading the json
                        try {
                            JSONObject res = new JSONObject(response);
                            if (res.getString("success").equals("ok")) {

                                //get data from res object

                                String group_name = res.getJSONObject("profile").getString("group_name");
                                String group_id = res.getJSONObject("profile").getString("group_id");

                                //ToastUtils.showToast(group_name, context);

                                if(CurrentUser.getInstance().getAllGroups().containsKey(group_id)){
                                    CurrentUser.getInstance().getAllGroups().get(group_id).setName(group_name);
                                }

                            } else {
                                ToastUtils.showToast(res.getString("msg"),context);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            ToastUtils.showToast("exception",context);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // error handling
                ToastUtils.showToast("Sorry, cannot connect to the server.",context);
            }
        });

        queue.add(stringRequest);


    }

}
