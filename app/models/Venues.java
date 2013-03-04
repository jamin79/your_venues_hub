package models;

/**
 * Created with IntelliJ IDEA.
 * User: IfieB
 * Date: 11/02/2013
 * Time: 22:15
 * To change this template use File | Settings | File Templates.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import play.Logger;
import play.data.validation.Constraints.Required;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import controllers.MorphiaObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

import javax.validation.Valid;
import java.net.URI ;


@Entity("venues")
public class Venues {

    @Id
    public ObjectId id;
    @Required
    public String name;
    @Required
    public String about;

    @Valid
    @Embedded
    public Address address;

    @Valid
    @Required
    @Embedded
    public Contact contact;

    public String venueId;

    //@Required
    //@Embedded
    //private List<Rooms> rooms;
    //@Required
    //private List<HireType> hireType;
    //public String parkingInfo;

    public static List<Venues> all() {
        if (MorphiaObject.datastore != null) {
            return MorphiaObject.datastore.find(Venues.class).asList();
        } else {
            return new ArrayList<Venues>();
        }
    }

    public static void create(Venues venue) {
        String _address = venue.address.line1 + " " + venue.address.line2 + " " + venue.address.postCode;
        Float[] latlog = getLongLatGoogle(_address);
        venue.address.latitude = latlog[0];
        venue.address.longitude = latlog[1];
        //venue.venueId = venue.id.toStringMongod();
        MorphiaObject.datastore.save(venue);
    }

    public static void delete(String idToDelete) {
        Venues toDelete = MorphiaObject.datastore.find(Venues.class).field("_id").equal(new ObjectId(idToDelete)).get();
        if (toDelete != null) {
            Logger.info("toDelete: " + toDelete);
            MorphiaObject.datastore.delete(toDelete);
        } else {
            Logger.debug("ID Not Found: " + idToDelete);
        }
    }

    public static void deleteByName(String nameToDelete) {
        Venues toDelete = MorphiaObject.datastore.find(Venues.class).field("name").equal(nameToDelete).get();
        if (toDelete != null) {
            Logger.info("toDelete: " + toDelete);
            MorphiaObject.datastore.delete(toDelete);
        } else {
            Logger.debug("Name Not Found: " + nameToDelete);
        }
    } //deleteByName

    public static Venues getVenue(String id) {
        Venues venue = MorphiaObject.datastore.find(Venues.class).field("_id").equal(new ObjectId(id)).get();
        if (venue != null) {
            Logger.info("returning: " + venue);
            return venue;
        } else {
            Logger.debug("ID Not Found: " + id);
        }
        return null;
    }



    public static Float[] getLongLat(String address)  {
        float latValue = 0.0F;
        float longValue = 0.0F;
        try {
            HttpClient client = new DefaultHttpClient();
            URIBuilder builder = new URIBuilder();
            builder.setScheme("http").setHost("nominatim.openstreetmap.org").setPath("/search")
                    .setParameter("q", address)
                    .setParameter("countrycodes", "GB")
                    .setParameter("format", "json");
            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            HttpResponse response = client.execute(request);

            int code = response.getStatusLine().getStatusCode();
            Logger.debug("Status code was "+ Integer.toString(code));

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

            String json = rd.readLine();
            Logger.debug(json);
            Object obj = JSONValue.parse(json);
            JSONArray _array = (JSONArray)obj;
            Logger.debug("size is " + Integer.toString( _array.size()));
            for(int i = 0; i < _array.size(); i++) {
                JSONObject result = (JSONObject)_array.get(i);
                String latitude = (String) result.get("lat");
                latValue = Float.parseFloat(latitude);
                String longitude = (String) result.get("lon");
                longValue = Float.parseFloat(longitude);
                Logger.debug(String.format("lat:%s long:%s", latitude, longitude));
                break;
            }
        }  catch (Exception ex) {
            Logger.debug(ex.getMessage());
        }
        return new Float[] { latValue, longValue }  ;
    }

    public static Float[] getLongLatGoogle(String address)  {
        float latValue = 0.0F;
        float longValue = 0.0F;
        Geocoder geocoder = new Geocoder();
        GeocoderRequest geocoderRequest = new GeocoderRequestBuilder().setAddress(address).getGeocoderRequest();
        GeocodeResponse geocoderResponse = geocoder.geocode(geocoderRequest);
        List<GeocoderResult> results = geocoderResponse.getResults();
        latValue = results.get(0).getGeometry().getLocation().getLat().floatValue();
        longValue = results.get(0).getGeometry().getLocation().getLng().floatValue();
        return new Float[] { latValue, longValue }  ;
    }


}
