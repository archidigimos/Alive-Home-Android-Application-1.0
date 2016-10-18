package com.example.archismansarkar.login_signup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    MainActivity reconnect_app = new MainActivity();


    @Override
    public void onReceive(final Context context, final Intent intent) {

        String status = NetworkUtil.getConnectivityStatusString(context);

        if (new String("Wifi enabled").equals(status)) {
            Toast.makeText(context, "Connected to internet!! Please connect to SERVER from menu if possible!!", Toast.LENGTH_LONG).show();
        }
        else if (new String("Not connected to Internet").equals(status)){
            Toast.makeText(context, "Not connected to Internet! Please try reconnecting to the required SSID from settings or wait for BT to connect!!!", Toast.LENGTH_LONG).show();
        }
    }
}
