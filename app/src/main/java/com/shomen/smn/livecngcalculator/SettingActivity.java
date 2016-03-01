package com.shomen.smn.livecngcalculator;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SettingActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private BasicData basicData ;
    private EditText et_base_Charge,et_p_m_w_charge,et_p_km_charge,et_base_dist;
    private Button btn_set_basic_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        basicData = new BasicData(this);

        if(!basicData.isSet()){
            basicData.insertBaseData(40,2,20,2);
        }
        inItComponent();
        setListenerToComponent();
        setFormData();
        hideSoftKeyboard(et_base_Charge);
    }

    public void hideSoftKeyboard(View view){
        InputMethodManager imm =(InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void inItComponent(){
        et_base_Charge = (EditText) findViewById(R.id.et_b_charge);
        et_base_dist = (EditText) findViewById(R.id.et_b_distance);
        et_p_km_charge = (EditText) findViewById(R.id.et_per_km_charge);
        et_p_m_w_charge = (EditText) findViewById(R.id.et_per_min_w_charge);
        btn_set_basic_data = (Button) findViewById(R.id.btn_set_data);
    }

    private void setListenerToComponent(){
        btn_set_basic_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validate()){
                    Log.d(LOG_TAG,"failed");
                }else{
                    Log.d(LOG_TAG,"succes ");
                    basicData.insertBaseData(Float.parseFloat(et_base_Charge.getText().toString().trim()),
                                            Float.parseFloat(et_base_dist.getText().toString().trim()),
                                            Float.parseFloat(et_p_km_charge.getText().toString().trim()),
                                            Float.parseFloat(et_p_m_w_charge.getText().toString().trim()));

                    finish();
                }
            }
        });
    }

    private void setFormData(){
        et_base_Charge.setText( String.valueOf(basicData.get_BASE_CHARGE()));
        et_base_dist.setText(String.valueOf(basicData.get_BASE_DISTANCE()));
        et_p_km_charge.setText(String.valueOf(basicData.get_PER_KM_CHARGE()));
        et_p_m_w_charge.setText(String.valueOf(basicData.get_PER_MIN_WAIT_CHARGE()));
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
            Float.parseFloat(input);
            return true;
        }catch( Exception e){
            return false;
        }
    }

    public boolean validate() {
        boolean valid = true;

        String b_c = et_base_Charge.getText().toString().trim();
        String b_d = et_base_dist.getText().toString().trim();
        String p_m_w_c = et_p_m_w_charge.getText().toString().trim();
        String p_k_c = et_p_km_charge.getText().toString().trim();

        if (b_c.length() > 4 || !isFloat(b_c)) {
            et_base_Charge.setError("Can't be empty more then 4 character and must have to be a valid number");
            valid = false;
        } else {
            et_base_Charge.setError(null);
        }

        if (b_d.length() > 4 || !isFloat(b_d)) {
            et_base_dist.setError("Can't be empty more then 4 character and must have to be a valid number");
            valid = false;
        } else {
            et_base_dist.setError(null);
        }

        if (p_m_w_c.length() > 4 || !isFloat(p_m_w_c)) {
            et_p_m_w_charge.setError("Can't be empty more then 4 character and must have to be a valid number");
            valid = false;
        } else {
            et_p_m_w_charge.setError(null);
        }

        if (p_k_c.length() > 4 || !isFloat(p_k_c)) {
            et_p_km_charge.setError("Can't be empty more then 4 character and must have to be a valid number");
            valid = false;
        } else {
            et_p_km_charge.setError(null);
        }

        return valid;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}
