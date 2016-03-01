package com.shomen.smn.livecngcalculator;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonParser {

	private final String TAG = this.getClass().getSimpleName();

	double distance;
	double duration;

	public List<List<HashMap<String,String>>> parse(JSONObject jObject){
		Log.d(TAG,"jb "+jObject.toString());

        distance = 0;
        duration = 0;

		List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String,String>>>();
		JSONArray jRoutes = null;
		JSONArray jLegs = null;
		JSONArray jSteps = null;
		JSONObject jDistance = null;
		JSONObject jDuration = null;

		try {

			jRoutes = jObject.getJSONArray("routes");

			/** Traversing all routes */
			for(int i=0;i<jRoutes.length();i++){
				jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
				List path = new ArrayList<HashMap<String, String>>();

				/** Traversing all legs */
				for(int j=0;j<jLegs.length();j++){

					/** Getting distance from the json data */
					jDistance = ((JSONObject) jLegs.get(j)).getJSONObject("distance");
					Log.d(TAG, jDistance.toString());
					distance = distance+jDistance.getDouble("value");

					/** Getting duration from the json data */
					jDuration = ((JSONObject) jLegs.get(j)).getJSONObject("duration");
					Log.d(TAG, jDuration.toString());
					duration = duration+jDuration.getDouble("value");


					jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

					/** Traversing all steps */
					for(int k=0;k<jSteps.length();k++){
						String polyline = "";
						polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
						List<LatLng> list = decodePoly(polyline);

						/** Traversing all points */
						for(int l=0;l<list.size();l++){
							HashMap<String, String> hm = new HashMap<String, String>();
							hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
							hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
							path.add(hm);
						}
					}
					routes.add(path);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}catch (Exception e){
		}
		return routes;
	}

	public double getDistance(){
		return distance;
	}

	public Double getDuration(){
		return duration;
	}

	/**
	 * Method to decode polyline points 
	 * Courtesy : jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java 
	 * */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

	public List<List<HashMap<String,String>>> parseFileData_G(JSONObject obj){

		List<List<HashMap<String,String>>> data = new ArrayList<>();

		Log.d(TAG,"obj "+obj.toString());
		try {
			data.addAll(parse(obj.getJSONObject("google_data")));

		} catch (JSONException e) {
			e.printStackTrace();
			Log.d(TAG, "inside parseFileData_G something weired happen "+e.toString());
		}
		return data;
	}

	public List<LatLng> parseFileData_U(JSONObject obj){
		List<LatLng> ll = new ArrayList<>();
		Log.d(TAG,"obj "+obj.toString());
		try {
			JSONArray arr_lat = obj.getJSONObject("user_data").getJSONArray("user_lat");
			Log.d(TAG,"arr_lat "+arr_lat.toString()) ;
			JSONArray arr_lon =  obj.getJSONObject("user_data").getJSONArray("user_lon");
			Log.d(TAG,"arr_lon "+arr_lon.toString()) ;
			for(int i=0 ; i<arr_lat.length();i++) {
				ll.add(new LatLng(arr_lat.getDouble(i),arr_lon.getDouble(i)));
			}
			Log.d(TAG,"list ll "+ll.toString()) ;

		} catch (JSONException e) {
			e.printStackTrace();
			Log.d(TAG, "Error " + e.toString()) ;
		}
		return ll;
	}
}