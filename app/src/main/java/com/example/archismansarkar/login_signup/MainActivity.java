package com.example.archismansarkar.login_signup;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;


public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class
            .getSimpleName();
    public int track = 0;
    int count = 0;
    int layer = 0;
    int layer_back_trig = 0;

    Button login1, signup1, login, signup;
    Button alive_next, alive_skip, first_next, first_skip, second_next, second_skip, final_done;
    SeekBar fanSpeed;
    EditText uname_login, password_login, uname_signup, password_signup, repassword_signup, hardwareID_signup;
    ImageView imageView, imageView1, alive;
    ImageButton tubelight;
    GPSActivity gps;
    ImageSwitcher fanAlive;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    // blechat - characteristics for HM-10 serial
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    String[] data_parsed;
    String layout_position;
    private String username_init;
    private String password_init;
    private String user_login_init;
    String TL_state = "TL_OFF";
    String FAN_state = "FAN_OFF";
    boolean tubelight_state = false;

    boolean connecting = false;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                connecting = false;
                Toast.makeText(getApplicationContext(),"Welcome back to your Home!",Toast.LENGTH_SHORT).show();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                if (track==0){start();}
                mConnected = false;
                connecting = false;
                connecting = mBluetoothLeService.connect("74:DA:EA:B2:39:E9");
                Toast.makeText(getApplicationContext(), "BT disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.

                // blechat
                // set serial chaacteristics
                setupSerial();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] rxBytes = characteristicRX.getValue();
                String btData = new String(rxBytes);
                Toast.makeText(getApplicationContext(),"Text from Hardware: "+btData,Toast.LENGTH_SHORT).show();
                btData = btData.substring(7);
                //sendSerial(btData);
                //mConnection.sendTextMessage(btData);
            }
        }
    };

    private final WebSocketConnection mConnection = new WebSocketConnection();


    public void start() {

        final String wsuri = "ws://192.168.1.10:80";
        //final String wsuri = "ws://192.168.8.100:80";

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {

                @Override
                public void onOpen() {
                    track = 1;
                    mConnection.sendTextMessage("LOGI-"+username_init+"-"+password_init);
                    mConnection.sendTextMessage("ENQ-" + username_init);

                }

                @Override
                public void onTextMessage(String payload) {
                    data_parsed = payload.split("-");
                    int size = data_parsed.length;

                    if (new String("VERIFY").equals(data_parsed[0])){
                        if(new String("True").equals(data_parsed[1])){
                            if((size>2)&& new String("STATUS").equals(data_parsed[2])){

                                TL_state = data_parsed[3];
                                FAN_state = data_parsed[4];

                                if(new String("TL_ON").equals(data_parsed[3])){tubelight_state = false;tubelight.performClick();}
                                else if (new String("TL_OFF").equals(data_parsed[3])){tubelight_state = true;tubelight.performClick();}

                                if(new String("FAN_OFF").equals(data_parsed[4])){fanAlive.setImageResource(R.drawable.fan);fanSpeed.setProgress(0);}
                                else if(new String("FAN_ON_1").equals(data_parsed[4])){fanAlive.setImageResource(R.drawable.fan_one);fanSpeed.setProgress(1);}
                                else if(new String("FAN_ON_2").equals(data_parsed[4])){fanAlive.setImageResource(R.drawable.fan_two);fanSpeed.setProgress(2);}
                                else if(new String("FAN_ON_3").equals(data_parsed[4])){fanAlive.setImageResource(R.drawable.fan_three);fanSpeed.setProgress(3);}
                                else if(new String("FAN_ON_4").equals(data_parsed[4])){fanAlive.setImageResource(R.drawable.fan_four);fanSpeed.setProgress(4);}
                                else if(new String("FAN_ON_5").equals(data_parsed[4])){fanAlive.setImageResource(R.drawable.fan_five);fanSpeed.setProgress(5);}
                            }


                            if (count == 0) {
                                user_layer_interface();
                                Toast.makeText(getApplicationContext(), "You are connected to your room via the Web!!!", Toast.LENGTH_SHORT).show();
                                count = 1;

                            }
                        }
                        else if (new String("False").equals(data_parsed[1])){
                            Toast.makeText(getApplicationContext(), "Authentication Error!!!", Toast.LENGTH_SHORT).show();
                        }
                    }

                }

                @Override
                public void onClose(int code, String reason) {
                    track = 0;
                }
            });
        } catch (WebSocketException e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                if (mConnected == false && connecting == false){connecting = mBluetoothLeService.connect("74:DA:EA:B2:39:E9");}
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        SharedPreferences logged_data = getSharedPreferences("LayoutData", Context.MODE_PRIVATE);
        layout_position = logged_data.getString("layout","");

        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        username_init = prefs.getString("username","");
        password_init = prefs.getString("password","");

        if (new String("startup_page").equals(layout_position))startup_page();
        else intro_page();

    }

    @Override
    protected void onResume() {
        super.onResume();
//        counter = 0;
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);

        }
        //      if((mConnected)&&(counter==0)) {
        //        counter++;sendSerial();
        //  }
    }

    @Override
    protected void onPause() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        //if((mConnected)&&(counter==0)) {
        //  sendSerial();
        //}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onBackPressed(){
        if(layer == 1){layer_back_trig++;
            if(layer_back_trig>2){
                Toast.makeText(getApplicationContext(), "Exited", Toast.LENGTH_LONG).show();
                layer_back_trig=0;
                finish();
            }
        }
        else if((layer == 2) || (layer == 3)){startup_page();}
        else if(layer == 4){layer_back_trig++;
            if(layer_back_trig>2){
                Toast.makeText(getApplicationContext(), "Exited", Toast.LENGTH_LONG).show();
                layer_back_trig=0;
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ((layer == 1)||(layer == 2)||(layer == 3)){
            getMenuInflater().inflate(R.menu.normal_menu, menu);//Menu Resource, Menu
            return true;}
        else if (layer == 4){
            getMenuInflater().inflate(R.menu.final_menu, menu);
            return true;}
        else return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();    //remove all items
        if ((layer == 1)||(layer == 2)||(layer == 3)){
            getMenuInflater().inflate(R.menu.normal_menu, menu);//Menu Resource, Menu
            return true;}
        else if (layer == 4){
            getMenuInflater().inflate(R.menu.final_menu, menu);
            return true;}
        else return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item1:
                    String user_logout = "LOGO-"+username_init;
                    if(track==1)mConnection.sendTextMessage(user_logout);
                    SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("username", "");
                    editor.putString("password", "");
                    editor.commit();
                    count = 0;
                    Toast.makeText(getApplicationContext(),"Logged out!!!", Toast.LENGTH_SHORT).show();
                    startup_page();
                    return true;
                case R.id.item2:
                    Toast.makeText(getApplicationContext(), "Exited", Toast.LENGTH_LONG).show();
                    finish();
                    return true;
                case R.id.item3:
                    Toast.makeText(getApplicationContext(), "Exited", Toast.LENGTH_LONG).show();
                    finish();
                    return true;
                case R.id.item4:
                    Toast.makeText(getApplicationContext(), "Connecting to Server!!!", Toast.LENGTH_LONG).show();
                    start();
                    return true;
                case R.id.item5:
                    Toast.makeText(getApplicationContext(), "Hi, this is Archisman Sarkar \n I am the developer of this application \n You can reach me out at: archidehex@gmail.com", Toast.LENGTH_LONG).show();
                    return true;
                case R.id.item6:
                    Toast.makeText(getApplicationContext(), "It is an Automation Company \n Originated at IIT Kharagpur!!!", Toast.LENGTH_LONG).show();
                    return true;
                case R.id.item7:
                    Toast.makeText(getApplicationContext(), "Hi, this is Archisman Sarkar \n" +
                            " I am the developer of this application \n" +
                            " You can reach me out at: archidehex@gmail.com", Toast.LENGTH_LONG).show();
                    return true;
                case R.id.item8:
                    Toast.makeText(getApplicationContext(), "It is an Automation Company \n" +
                            " Originated at IIT Kharagpur!!!", Toast.LENGTH_LONG).show();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void setupSerial() {

        // blechat - set serial characteristics
        String uuid;
        String unknownServiceString = "Unknown Service";

        for (BluetoothGattService gattService : mBluetoothLeService
                .getSupportedGattServices()) {
            // HashMap<String, String> currentServiceData = new HashMap<String,
            // String>();
            uuid = gattService.getUuid().toString();

            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {

                // get characteristic when UUID matches RX/TX UUID
                characteristicTX = gattService
                        .getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
                characteristicRX = gattService
                        .getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

                mBluetoothLeService.setCharacteristicNotification(
                        characteristicRX, true);

                break;

            } // if

        } // for


    }

    public void intro_page(){
        setContentView(R.layout.alivehome);

        alive_next = (Button) findViewById(R.id.alive_next);
        alive_skip = (Button) findViewById(R.id.alive_skip);


        alive_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setContentView(R.layout.intro);
                first_next = (Button) findViewById(R.id.first_next);
                first_skip = (Button) findViewById(R.id.first_skip);

                first_next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {setContentView(R.layout.intro_second);
                        second_next = (Button) findViewById(R.id.second_next);
                        second_skip = (Button) findViewById(R.id.second_skip);

                        second_next.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {setContentView(R.layout.intro_last);
                                final_done = (Button) findViewById(R.id.final_done);

                                final_done.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {shared_layout_cache();startup_page();}
                                });
                            }
                        });

                        second_skip.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {shared_layout_cache(); startup_page(); }
                        });
                    }
                });

                first_skip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {shared_layout_cache(); startup_page(); }
                });
            }
        });

        alive_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {shared_layout_cache(); startup_page(); }
        });
    }

    public void startup_page(){
        setContentView(R.layout.startup_select);
        layer_back_trig =0;
        layer = 1;
        imageView1 = (ImageView) findViewById(R.id.imageView1);
        imageView1.setImageResource(R.mipmap.ic_launcher);
        start();

        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        username_init = prefs.getString("username","");
        password_init = prefs.getString("password","");
        user_login_init = "LOGI-" + username_init + "-" + password_init;

        login1 = (Button) findViewById(R.id.login1);
        signup1 = (Button) findViewById(R.id.signup1);

        login1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(track == 0){
                    Toast.makeText(getApplicationContext(), "Not connected to the Server!!!", Toast.LENGTH_SHORT).show();
                    start();
                    Toast.makeText(getApplicationContext(), "Trying to connect to the server!!!", Toast.LENGTH_SHORT).show();
                }
                else if(track ==1) {
                    setContentView(R.layout.activity_main);
                    layer = 2;
                    Toast.makeText(getApplicationContext(), "Good to see you back!!!", Toast.LENGTH_SHORT).show();
                    login = (Button) findViewById(R.id.login);

                    uname_login = (EditText) findViewById(R.id.uname_login);
                    password_login = (EditText) findViewById(R.id.password_login);

                    login.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if (!new String("").equals(uname_login.getText().toString())) {
                                username_init = uname_login.getText().toString();
                                SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("username", uname_login.getText().toString());
                                editor.putString("password", password_login.getText().toString());
                                editor.commit();

                                String user_login = "LOGI-" + uname_login.getText().toString() + "-" + password_login.getText().toString();
                                if (track == 1) mConnection.sendTextMessage(user_login);
                                //callGPS();
                                if (track == 1) mConnection.sendTextMessage("ENQ-" + username_init);
                            }
                            else if (new String("").equals(uname_login.getText().toString())){
                                Toast.makeText(getApplicationContext(), "Please enter an username!!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        signup1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(track == 0){
                    Toast.makeText(getApplicationContext(), "Not connected to the Server!!!", Toast.LENGTH_SHORT).show();
                    start();
                    Toast.makeText(getApplicationContext(), "Trying to connect to the server!!!", Toast.LENGTH_SHORT).show();
                }
                else if(track ==1) {
                    setContentView(R.layout.signup);
                    layer = 3;
                    Toast.makeText(getApplicationContext(), "Welcome to Alive Home automation!!!", Toast.LENGTH_SHORT).show();
                    signup = (Button) findViewById(R.id.signup);

                    uname_signup = (EditText) findViewById(R.id.uname_signup);
                    password_signup = (EditText) findViewById(R.id.password_signup);
                    repassword_signup = (EditText) findViewById(R.id.repassword_signup);
                    hardwareID_signup = (EditText) findViewById(R.id.hardwareID_signup);

                    signup.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if ((!new String("").equals(uname_signup.getText().toString()))&&(!new String("").equals(hardwareID_signup.getText().toString()))) {
                                username_init = uname_signup.getText().toString();
                                SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("username", uname_signup.getText().toString());
                                editor.putString("password", password_signup.getText().toString());
                                editor.commit();

                                String user_signup = "NUS-" + uname_signup.getText().toString() + "-" + password_signup.getText().toString() + "-" + repassword_signup.getText().toString() + "-" + hardwareID_signup.getText().toString();
                                if (track == 1) mConnection.sendTextMessage(user_signup);
                                //callGPS();
                                if (track == 1) mConnection.sendTextMessage("ENQ-" + username_init);
                            }
                            else if ((new String("").equals(uname_signup.getText().toString()))||(new String("").equals(hardwareID_signup.getText().toString()))){
                                Toast.makeText(getApplicationContext(), "Username and HardwareID cannot be left unattended!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    public void shared_layout_cache(){
        SharedPreferences logged_data = getSharedPreferences("LayoutData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = logged_data.edit();
        editor.putString("layout", "startup_page");
        editor.commit();
    }


    public void user_layer_interface(){

        setContentView(R.layout.user_layout);
        layer_back_trig = 0;
        layer = 4;
        callGPS();

        alive = (ImageView) findViewById(R.id.alive);
        alive.setImageResource(R.mipmap.ic_launcher);

        fanAlive = (ImageSwitcher) findViewById(R.id.fanAlive);
        fanAlive.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
                imageView.setImageResource(R.drawable.fan);
                return imageView;
            }
        });

        tubelight = (ImageButton) findViewById(R.id.tubelight);
        tubelight.setImageResource(R.drawable.bulboff);

        tubelight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callGPS();
                if(tubelight_state == true) {
                    TL_state = "TL_OFF";
                    tubelight.setImageResource(R.drawable.bulboff);
                    if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);
                    sendSerialBLE(TL_state+"-"+FAN_state);
                    tubelight_state = false;
                }
                else if (tubelight_state == false){
                    TL_state = "TL_ON";
                    tubelight.setImageResource(R.drawable.bulbon);
                    if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);
                    sendSerialBLE(TL_state+"-"+FAN_state);
                    tubelight_state = true;
                }
            }
        });

        fanSpeed = (SeekBar) findViewById(R.id.fanSpeed);

        fanSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                callGPS();
                progressChanged = progress;
                if (progress == 0){fanAlive.setImageResource(R.drawable.fan);FAN_state = "FAN_OFF";if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);sendSerialBLE(TL_state+"-"+FAN_state);}
                else if (progressChanged == 1){fanAlive.setImageResource(R.drawable.fan_one);FAN_state = "FAN_ON_1";if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);sendSerialBLE(TL_state+"-"+FAN_state);}
                else if (progressChanged == 2){fanAlive.setImageResource(R.drawable.fan_two);FAN_state = "FAN_ON_2";if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);sendSerialBLE(TL_state+"-"+FAN_state);}
                else if (progressChanged == 3){fanAlive.setImageResource(R.drawable.fan_three);FAN_state = "FAN_ON_3";if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);sendSerialBLE(TL_state+"-"+FAN_state);}
                else if (progressChanged == 4){fanAlive.setImageResource(R.drawable.fan_four);FAN_state = "FAN_ON_4";if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);sendSerialBLE(TL_state+"-"+FAN_state);}
                else if (progressChanged == 5){fanAlive.setImageResource(R.drawable.fan_five);FAN_state = "FAN_ON_5";if(track==1)mConnection.sendTextMessage("CTRL-"+username_init+"-"+TL_state+"-"+FAN_state);sendSerialBLE(TL_state+"-"+FAN_state);}
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    private void callGPS(){
        // create class object
        gps = new GPSActivity(MainActivity.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            String latitudeS = String.valueOf(latitude);
            String longitudeS = String.valueOf(longitude);
            if(track==1)mConnection.sendTextMessage("LOCATION-"+username_init+"-"+latitudeS+"-"+longitudeS);
            //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }

    private void sendSerialBLE(String message) {
        Log.d(TAG, "Sending: " + message);
        final byte[] tx = message.getBytes();
        if (mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
        } // if
    }

}
