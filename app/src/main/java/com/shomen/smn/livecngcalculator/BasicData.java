package com.shomen.smn.livecngcalculator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;


public class BasicData {

    private Context context;
    private SharedPreferences sharedPreferences;
    private final String BASIC_PREFS = "LIVE_CNG_COST_CALCULATOR";
    private SharedPreferences.Editor editor;

    private final String IS_SET = "is_set";
    private final String PER_MIN_WAIT_CHARGE = "per_min_wait_charge";
    private final String BASE_DISTANCE = "base_distance";
    private final String PER_KM_CHARGE = "per_km_charge";
    private final String BASE_CHARGE = "base_charge";


    public BasicData(Context context) {
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(BASIC_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void insertBaseData(float base_charge,float base_dist,float per_km_charge,float per_min_wait_charge){
        editor.putBoolean(IS_SET, true);
        editor.putFloat(BASE_CHARGE, base_charge);
        editor.putFloat(BASE_DISTANCE,base_dist);
        editor.putFloat(PER_KM_CHARGE, per_km_charge);
        editor.putFloat(PER_MIN_WAIT_CHARGE,per_min_wait_charge);
        editor.commit();
    }

    public void clearBaseData(){
        editor.clear();
        editor.commit();
    }

    public float get_PER_KM_CHARGE(){return sharedPreferences.getFloat(PER_KM_CHARGE,0);}

    public float get_BASE_DISTANCE(){return sharedPreferences.getFloat(BASE_DISTANCE,0);}

    public float get_PER_MIN_WAIT_CHARGE() {
        return sharedPreferences.getFloat(PER_MIN_WAIT_CHARGE,0);
    }

    public float get_BASE_CHARGE(){return sharedPreferences.getFloat(BASE_CHARGE,0);}

    public boolean isSet(){
        return sharedPreferences.getBoolean(IS_SET, false);
    }

    public void set_PER_KM_CHARGE(float per_km_charge){
        editor.putFloat(PER_MIN_WAIT_CHARGE,per_km_charge);
    }

    public void set_PER_MIN_WAIT_CHARGE(float per_min_wait_charge) {
        editor.putFloat(PER_MIN_WAIT_CHARGE,per_min_wait_charge);
    }

    public void set_BASE_CHARGE(float base_charge){
        editor.putFloat(BASE_CHARGE, base_charge);
    }

    public void set_BASE_DISTANCE(float base_dist){
        editor.putFloat(BASE_DISTANCE, base_dist);
    }

}
