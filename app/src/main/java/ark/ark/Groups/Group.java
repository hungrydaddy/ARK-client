package ark.ark.Groups;

import android.location.Location;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.SquareCap;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import ark.ark.Map.MapNavDrawer;
import ark.ark.Map.MapWaypoint;

/**
 * Created by khtin on 20/09/2017.
 */

public class Group {
    private static int nGroups = 0;
    private String groupName;
    private String ownerEmail;
    private MapWaypoint waypoint;

    private String groupID;
    private HashMap<String, Friend> friendsInGroup = new HashMap<>();

    public Group(String id, String ownerEmail) {
        nGroups += 1;
        groupName = "";
        groupID = id;
        this.ownerEmail = ownerEmail;
    }

    public HashMap<String, Friend> getFriends() {
        return friendsInGroup;
    }

    public Friend getFriend(String email) { return friendsInGroup.get(email); }

    public String getId() {
        return groupID;
    }

    public String getName() { return groupName; }

    public void setName(String name){
        this.groupName = name;
    }

    public String getOwner() { return ownerEmail; }

    /*
    @param
    dict: HashMap of email and location of users
     */
    /*
    public void setLocations(HashMap<String, Location> dict) {

        for (String email : dict.keySet()) {
            friendsInGroup.get(email).setLocation(dict.get(email));
        }
    }

    public void updateFriends(List<Friend> friends) {
        for (Friend friend : friends) {
            if (!friendsInGroup.containsKey(friend.getEmail())) {
                friendsInGroup.put(friend.getEmail(), friend);
            }
        }
    }
    */

    public void updateFriend(Friend friend) {
        if (!friendsInGroup.containsKey(friend.getEmail()) && !friend.getEmail().equals(ownerEmail)) {
            friendsInGroup.put(friend.getEmail(), friend);
        }
    }

    public void setLocation(String email, Location location) {
        if (friendsInGroup.containsKey(email)){
            friendsInGroup.get(email).setLocation(location);
        }

    }

    public void setWaypoint(Double lat, Double lng, String creator, String placename,
                            String placeaddress, Boolean active){

            String title = creator + "'s Waypoint.";
            LatLng loc = new LatLng(lat, lng);
            waypoint = new MapWaypoint(title, loc, placename, placeaddress, true);
            if (!active){
                waypoint.set_active(active);
            }
    }

    public void deleteWaypoint(){
        Log.d("Waypointdeleted", "waypointnulled");
        waypoint.set_active(false);
    }

    public MapWaypoint getWaypoint(){ return waypoint;}

    @Override
    public String toString() {
        return friendsInGroup.values().toString();
    }

}