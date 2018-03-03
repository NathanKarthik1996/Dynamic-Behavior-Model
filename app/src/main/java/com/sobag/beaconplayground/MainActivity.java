package com.sobag.beaconplayground;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconType;
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconUtils;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconDevice;
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconDistanceDescriptor;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;


public class MainActivity extends ActionBarActivity
{

    // ------------------------------------------------------------------------
    // members
    // ------------------------------------------------------------------------
    public TextView txtView;
    private static final String LOG_TAG = "MainActivity";
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private Handler scanHandler = new Handler();
    private int scan_interval_ms = 5000;
    private boolean isScanning = false;
    //data d;
    String[] descriptors={"NEAR","IMMEDIATE","FAR"};
    int[][] counter;
    int thresh2=0;
    ArrayList<String> map=new ArrayList<String>();
    String s2;
    int count=0;
    int thresh=0;
    int cut=4,max_range=10;
    int piece_size=max_range/cut;
    int[] pieces=new int[cut+1];
    String last;
    String showUrl = "http://192.168.43.82:81/retail/showdata.php";
    String descript;
    //HashMap<String, Integer> meMap=new HashMap<String, Integer>();
    int i=0;
    // ------------------------------------------------------------------------
    // default stuff...
    // ------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(thresh==0) {
            thresh++;
            String insertUrl = "http://192.168.0.16/tutorial/insertStudent.php";
            //String showUrl = "http://192.168.1.3:81/retail/showdata.php";
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            // TextView result;
            // makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
            //   Log.i(LOG_TAG,"INSIDE");
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    showUrl, new Response.Listener<JSONObject>() {
                // toasting("s");
                @Override
                public void onResponse(JSONObject response) {
                    System.out.println(response.toString());
                    //   Toast.makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
                    //       toasting("inside response");
                    try {

                        // txtView.setText("");
//                            i++;

                        Log.i(LOG_TAG, "INSIDE");
                        JSONArray students = response.getJSONArray("infos");
                        System.out.println("INside");
                        for (int i = 0; i < students.length(); i++) {
                            JSONObject student = students.getJSONObject(i);
                            //             toasting("inside loop");
                            String firstname = student.getString("UUID");
                            //String lastname = student.getString("data");
                            //String age = student.getString("age");
                            System.out.println("INside");

                            map.add(firstname);
                        }
                        //                     toasting("" + map.size());
                        // txtView.append("===\n");
                        //                      }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.append(error.getMessage());

                }
            });
            requestQueue.add(jsonObjectRequest);

        }

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        scanHandler.post(scanRunnable);
        // init BLE

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------
    // public usage
    // ------------------------------------------------------------------------

    private Runnable scanRunnable = new Runnable()
    {
        @Override
        public void run() {
            if (isScanning)
            {
                if (btAdapter != null)
                {
                    btAdapter.stopLeScan(leScanCallback);
                }
            }
            else
            {
                if (btAdapter != null)
                {
                    btAdapter.startLeScan(leScanCallback);
                }
            }

            isScanning = !isScanning;

            scanHandler.postDelayed(this, scan_interval_ms);
        }
    };

    // ------------------------------------------------------------------------
    // Inner classes
    // ------------------------------------------------------------------------

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
            //int thresh=0;
            int startByte = 2;
            boolean patternFound = false;
            Double distance=0.0;

            for(int l=0, y=0;l<=max_range&&y<=cut;l+=piece_size,y++)
            {
                pieces[y]=l;
            }
            final BluetoothLeDevice deviceLe = new BluetoothLeDevice(device, rssi, scanRecord, System.currentTimeMillis());
            if (BeaconUtils.getBeaconType(deviceLe) == BeaconType.IBEACON) {
                final IBeaconDevice iBeacon = new IBeaconDevice(deviceLe);
                distance=iBeacon.getAccuracy();
                IBeaconDistanceDescriptor s=iBeacon.getDistanceDescriptor();
                descript=s.toString();
                // toasting(""+descript);
                toasting(""+distance);
            }
            while (startByte <= 5)
            {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15)
                { //Identifies correct data length
                    patternFound = true;
                    break;
                }
                startByte++;
            }
            if(thresh2==0) {
                counter = new int[map.size()][cut];
                for (int i = 0; i < map.size(); i++) {
                    for(int j=0;j<cut;j++){

                        counter[i][j] = 0;
                    }
                }
                thresh2++;
            }
            if (patternFound)
            {
                //Convert to hex String
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //UUID detection
                String uuid =  hexString.substring(0,8) + "-" +
                        hexString.substring(8,12) + "-" +
                        hexString.substring(12,16) + "-" +
                        hexString.substring(16,20) + "-" +
                        hexString.substring(20,32);
                if(!uuid.equals(s2)){
                    i=0;
                }
                // major
                final int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

                // minor
                final int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);
                txtView=(TextView) findViewById(R.id.text_id);
                //txtView.setText("UUID: " +uuid + "\nmajor: " +major +"\nminor:" +minor);
                Log.i(LOG_TAG,"UUID: " +uuid + "\\nmajor: " +major +"\\nminor" +minor);
                //    Toast.makeText(this,uuid, Toast.LENGTH_LONG).show();
                // db.connect(uuid);
                RequestQueue requestQueue= Volley.newRequestQueue(getApplicationContext());
                s2=uuid;
                // queri2(uuid,requestQueue,distance);
                queri2(uuid,requestQueue,distance);
                // queri3(uuid,requestQueue,descript);

            }

        }
    };

    /**
     * bytesToHex method
     */
    public void toasting(String s)
    {
        makeText(this, s, LENGTH_SHORT).show();
    }
    /* public void queri(final String uuid, RequestQueue requestQueue)
     {
         String insertUrl = "http://192.168.1.7/tutorial/insertStudent.php";
         String showUrl = "http://192.168.1.6/retail/showdata.php";
         // TextView result;
        // makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
      //   Log.i(LOG_TAG,"INSIDE");
         JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                 showUrl, new Response.Listener<JSONObject>() {
            // toasting("s");
             @Override
             public void onResponse(JSONObject response) {
                 System.out.println(response.toString());
              //   Toast.makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
          //       toasting("inside response");
                 try {
                     if(i==0) {
                         txtView.setText("");
                         i++;
                         Log.i(LOG_TAG, "INSIDE");
                         JSONArray students = response.getJSONArray("infos");
                         System.out.println("INside");
                         for (int i = 0; i < students.length(); i++) {
                             JSONObject student = students.getJSONObject(i);
                             //             toasting("inside loop");
                             String firstname = student.getString("UUID");
                             String lastname = student.getString("Data");
                             String age = student.getString("data2");
                             System.out.println("INside");

                           if(uuid.equals(firstname)) {

                               int xy=map.indexOf(uuid);
                               Log.i(LOG_TAG,"YEAAHHH BITCHH"+counter.length);
                               if(counter[xy]==0) {
                                   txtView.append(lastname + " \n");
                                 //  Log.i(LOG_TAG, firstname + " " + lastname + age+" \n");
                                  counter[xy]++;
                                   Log.i(LOG_TAG, "PUSHHHHHHHH"+counter[xy]);
                               }
                               else{
                                   txtView.append(age + " \n");
                                   Log.i(LOG_TAG, "INSIDE THE SECOND ONE NIGGA");
                               }
                               }
                           }
                        // txtView.append("===\n");
                     }
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }

             }
         }, new Response.ErrorListener() {
             @Override
             public void onErrorResponse(VolleyError error) {
                 System.out.append(error.getMessage());

             }
         });
         requestQueue.add(jsonObjectRequest);
       /*  toasting("" + map.size());
         for(String s:map) {
             toasting(s);
         }
     }*/
    public void queri2(final String uuid, RequestQueue requestQueue, final Double distance)
    {
        String insertUrl = "http://192.168.1.7/tutorial/insertStudent.php";
       // String showUrl = "http://192.168.1.3:81/retail/showdata.php";
        //toasting("olla");
        // TextView result;
        // makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
         //  Log.i(LOG_TAG,"INSIDE");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                showUrl, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response.toString());
                //  Toast.makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
                //       toasting("inside response");
                try {
                    //if(i==0) {

                    //  }
                    //i++;
                    String[] items=new String[cut*2];
                    Log.i(LOG_TAG, "INSIDE");
                    //   toasting("in bitch");
                    JSONArray students = response.getJSONArray("infos");
                    System.out.println("INside");
                    for (int i = 0; i < students.length(); i++) {
                        JSONObject student = students.getJSONObject(i);
                        //             toasting("inside loop");
                        String firstname = student.getString("UUID");

                        //String lastname = student.getString("Data");
                        //String age = student.getString("data2");
                        System.out.println("INside");

                        if(uuid.equals(firstname))
                        {

                            int xy = map.indexOf(uuid);
                            //Log.i(LOG_TAG, "es" + xy);
                            for(int j=0;j<cut*2;j++)
                            {
                                int ilo=j+1;
                                items[j]=student.getString("data"+ilo);
                                Log.i(LOG_TAG, items[j]);
                                //  toasting(items[j]);
                            }

                            for (int pos = 0; pos <cut; pos++) {
                                Log.i(LOG_TAG, ""+cut);
                                if (distance >= pieces[pos] && distance <= pieces[pos+1 ]) {
                                    Log.i(LOG_TAG, "inside the fif");
                                    if (counter[xy][pos]==0){
                                        Log.i(LOG_TAG, "inside counter" );
                                        txtView.setText(items[pos]);
                                        last = items[pos];
                                        counter[xy][pos]++;

                                    }
                                    else{
                                        if(!last.equals(items[pos])) {
                                            txtView.setText(items[pos + cut]);
                                            last = items[pos + cut];
                                        }
                                    }
                                }
                            }

                        }
                        //     }
                        // txtView.append("===\n");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.append(error.getMessage());

            }
        });
        requestQueue.add(jsonObjectRequest);
      /*  toasting("" + map.size());
        for(String s:map) {
            toasting(s);
        }*/
    }
    public void queri3(final String uuid, RequestQueue requestQueue, final String Description)
    {
        String insertUrl = "http://192.168.1.7/tutorial/insertStudent.php";
        //String showUrl = "http://192.168.1.3:81/retail/showdata.php";
        //toasting("olla");
        // TextView result;
        // makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
        //   Log.i(LOG_TAG,"INSIDE");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                showUrl, new Response.Listener<JSONObject>() {
            // toasting("s");
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response.toString());
                //   Toast.makeText(this, "Read SMS permission granted", LENGTH_SHORT).show();
                //       toasting("inside response");
                try {
                    //if(i==0) {

                    //  }
                    //i++;
                    String[] items=new String[cut*2];
                    Log.i(LOG_TAG, "INSIDE");
                    //   toasting("in bitch");
                    JSONArray students = response.getJSONArray("infos");
                    System.out.println("INside");
                    for (int i = 0; i < students.length(); i++) {
                        JSONObject student = students.getJSONObject(i);
                        //             toasting("inside loop");
                        String firstname = student.getString("UUID");

                        //String lastname = student.getString("Data");
                        //String age = student.getString("data2");
                        System.out.println("INside");

                        if(uuid.equals(firstname))
                        {

                            int xy = map.indexOf(uuid);
                            //Log.i(LOG_TAG, " BITCHes" + xy);
                            for(int j=0;j<cut*2;j++)
                            {
                                int ilo=j+1;
                                items[j]=student.getString("data"+ilo);
                                Log.i(LOG_TAG, items[j]);
                                //  toasting(items[j]);
                            }

                           /* for (int pos = 0; pos <cut; pos++) {
                                Log.i(LOG_TAG, ""+cut);
                                if (distance >= pieces[pos] && distance <= pieces[pos+1 ]) {
                                    Log.i(LOG_TAG, "inside the fif");
                                    if (counter[xy][pos]==0){
                                        Log.i(LOG_TAG, "inside counter" );
                                        txtView.setText(items[pos]);
                                        last = items[pos];
                                        counter[xy][pos]++;

                                    }
                                    else{
                                        if(!last.equals(items[pos])) {
                                            txtView.setText(items[pos + cut]);
                                            last = items[pos + cut];
                                        }
                                    }
                                }
                            }*/
                            if(Description.equalsIgnoreCase("near")) {
                                if (counter[xy][0] == 0) {
                                    Log.i(LOG_TAG, "inside counter");
                                    txtView.setText(items[0]);
                                    last = items[0];
                                    counter[xy][0]++;
                                } else {
                                    if (!last.equals(items[0])) {
                                        txtView.setText(items[0 + cut]);
                                        last = items[0 + cut];

                                    }
                                }
                            }
                            else if(Description.equalsIgnoreCase("immediate"))
                            {
                                if (counter[xy][1] == 0) {
                                    Log.i(LOG_TAG, "inside counter");
                                    txtView.setText(items[1]);
                                    last = items[1];
                                    counter[xy][1]++;
                                } else {
                                    if (!last.equals(items[1])) {
                                        txtView.setText(items[1 + cut]);
                                        last = items[1 + cut];

                                    }
                                }
                            }
                            else
                            {
                                if (counter[xy][2] == 0) {
                                    Log.i(LOG_TAG, "inside counter");
                                    txtView.setText(items[2]);
                                    last = items[2];
                                    counter[xy][2]++;
                                } else {
                                    if (!last.equals(items[2])) {
                                        txtView.setText(items[2 + cut]);
                                        last = items[2 + cut];

                                    }
                                }
                            }

                        }
                        //     }
                        // txtView.append("===\n");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.append(error.getMessage());

            }
        });
        requestQueue.add(jsonObjectRequest);
      /*  toasting("" + map.size());
        for(String s:map) {
            toasting(s);
        }*/
    }
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
