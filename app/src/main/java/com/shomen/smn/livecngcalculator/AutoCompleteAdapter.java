package com.shomen.smn.livecngcalculator;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoCompleteAdapter extends ArrayAdapter<AutoCompletePlaceModel> {

    private GoogleApiClient mGoogleApiClient;
    private final String LOGT_TAG = this.getClass().getSimpleName();

    private LatLngBounds bounds = new LatLngBounds( new LatLng( 39.906374, -105.122337 ), new LatLng( 39.949552, -105.068779 ) );
    private List<Integer> filterTypes = new ArrayList<Integer>();

    public AutoCompleteAdapter( Context context ) {
        super(context, 0);
        filterTypes.add(Place.TYPE_ESTABLISHMENT);
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        ViewHolder holder;
        Log.d(LOGT_TAG,"inside getView method....");
        if( convertView == null ) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from( getContext() ).inflate(R.layout.single_search_item, parent, false  );
            holder.text = (TextView) convertView.findViewById(R.id.tv_search_item );
            convertView.setTag( holder );
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText( getItem( position ).getDescription() );

        return convertView;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
    }

    private class ViewHolder {
        TextView text;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                Log.d(LOGT_TAG,"inside performFiltering method....");

                if( mGoogleApiClient == null || !mGoogleApiClient.isConnected() ) {
                    Toast.makeText( getContext(), "Can't connect please try again.", Toast.LENGTH_SHORT ).show();
                    return null;
                }
                clear();
                Log.d(LOGT_TAG,"data "+constraint.toString());
                displayPredictiveResults(constraint.toString());

                return null;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                Log.d(LOGT_TAG,"inside publishResults method.");
                notifyDataSetChanged();
            }
        };
    }

    private void displayPredictiveResults( String query ) {
        Log.d(LOGT_TAG,"inside displayPredictiveResults method.");

        if(query.isEmpty() || query.length()<2)
            return;

        Places.GeoDataApi.getAutocompletePredictions( mGoogleApiClient, query, bounds,
            AutocompleteFilter.create(filterTypes) )
            .setResultCallback(
                new ResultCallback<AutocompletePredictionBuffer>() {
                    @Override
                    public void onResult(AutocompletePredictionBuffer buffer) {

                        if (buffer == null)
                            return;

                        if (buffer.getStatus().isSuccess()) {
                            for (AutocompletePrediction prediction : buffer) {
                                add(new AutoCompletePlaceModel(prediction.getPlaceId(),
                                        prediction.getDescription()));
                            }
                            Log.d(LOGT_TAG, "google data " + buffer.toString());
                        }else{
                            Toast.makeText(getContext(),"ERROR :"+buffer.getStatus().toString(),Toast.LENGTH_SHORT).show();
                        }

                        buffer.release();
                    }
                }, 60, TimeUnit.SECONDS);
    }
}
