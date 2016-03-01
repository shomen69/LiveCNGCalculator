package com.shomen.smn.livecngcalculator;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class InternalFileController {

    private final String TAG = this.getClass().getSimpleName();
    private Context context;

    public InternalFileController(Context context) {
        this.context = context;
    }

    public String getFileDirectory(){
        File f = context.getFilesDir();
        String path = f.getAbsolutePath();
        return  path;
    }

    public void createFile(JSONObject data) {

        String text = data.toString();

        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput("paths", context.MODE_PRIVATE);
            Log.d(TAG,"inside creatFile data "+text);
            fos.write(text.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public JSONObject readFile() {
        Log.d(TAG,"inside readFile ");

        FileInputStream fis = null;
        try {
            fis = context.openFileInput("paths");
            BufferedInputStream bis = new BufferedInputStream(fis);
            StringBuffer b = new StringBuffer();
            while (bis.available() != 0) {
                char c = (char) bis.read();
                b.append(c);
            }
            bis.close();
            fis.close();

            JSONObject data = new JSONObject(b.toString());
            Log.d(TAG,"d"+data.toString());
            return data;
        } catch (FileNotFoundException e) {
            Log.d(TAG,"Error "+e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"Error " + e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG,"Error " + e.toString());
        }
        return null;

    }

}
