package com.example.bishakh.wifidirecttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    LinearLayout mainlayout;
    LinearLayout neighbourlayout;
    TextView logText;
    ScrollView scrollView;

    IntentFilter mIntentFilter;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    WifiP2pManager.ConnectionInfoListener mListener;

    // PeerListListener
    WifiP2pManager.PeerListListener myPeerListListener =  new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            Log.v("DEBUG", "Device list:");
            Collection<WifiP2pDevice> deviceList = wifiP2pDeviceList.getDeviceList();
            int devicesCount = deviceList.size();
            int iii = 0;
            final Button deviceButtons[] = new Button[devicesCount];
            neighbourlayout.removeAllViews();
            for (WifiP2pDevice device:deviceList){
                Log.v("DEBUG", "Device: " + device.deviceAddress);
                deviceButtons[iii] = new Button(getApplicationContext());
                deviceButtons[iii].setText(device.deviceAddress);
                deviceButtons[iii].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button b = (Button)view;
                        Log.v("DEBUG", "Try to connect: " + b.getText());
                        //obtain a peer from the WifiP2pDeviceList
                        //WifiP2pDevice device;
                        final WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = b.getText().toString();
                        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                //success logic
                                Log.v("DEBUG", "Connection Success: " + config.deviceAddress);
                                Toast.makeText(getApplicationContext(), "Connection Success: " + config.deviceAddress,
                                        Toast.LENGTH_LONG).show();
                                logText.setText(logText.getText() + "\nConnection Success: " + config.deviceAddress);
                                mManager.requestConnectionInfo (mChannel,
                                        mListener);

                            }

                            @Override
                            public void onFailure(int reason) {
                                //failure logic
                                Log.v("DEBUG", "Connection Failed: " + config.deviceAddress);

                            }
                        });

                    }
                });
                neighbourlayout.addView(deviceButtons[iii]);
            }

        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scrollView = new ScrollView(getApplicationContext());
        mainlayout = new LinearLayout(getApplicationContext());
        mainlayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(mainlayout);
        neighbourlayout = new LinearLayout(getApplicationContext());
        neighbourlayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(scrollView);

        logText = new TextView(getApplicationContext());
        logText.setTextColor(Color.BLUE);
        Button discoverButton = new Button(getApplicationContext());
        final Button printInfoButton = new Button(getApplicationContext());
        printInfoButton.setText("Print Info");
        printInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printInfo();
            }
        });
        discoverButton.setText("Discover Peers");
        mainlayout.addView(printInfoButton);
        mainlayout.addView(discoverButton);
        mainlayout.addView(neighbourlayout);
        mainlayout.addView(logText);
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.v("DEBUG", "discovery success");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.v("DEBUG", "discovery failed: " + reasonCode);

                    }
                });
            }
        });

        mListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                logText.setText(logText.getText() + "\n" +
                        "INFO:");
                try {

                    Log.v("DEBUG", "Connection Group Owner address: " + wifiP2pInfo.groupOwnerAddress.toString());
                    logText.setText(logText.getText() + "\nConnection Group Owner address: " + wifiP2pInfo.groupOwnerAddress.toString());
                }catch (Exception e){}

                Log.v("DEBUG", "Group Formed: " + wifiP2pInfo.groupFormed);
                logText.setText(logText.getText() + "\nGroup Formed: " + wifiP2pInfo.groupFormed);

                if(wifiP2pInfo.isGroupOwner){
                    Log.v("DEBUG", "I am group owner");
                    logText.setText(logText.getText() + "\nI am group owner");
                }


                try {
                   String ip = getDottedDecimalIP(getLocalIPAddress());

                   Log.v("DEBUG", "My IP: " + ip);
                   logText.setText(logText.getText() + "\nMy IP: " + ip);
               }catch (Exception e){

               }

            }
        };


        logText.setText(logText.getText() + "\nMy MAC address: " + getMacAddr());

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this, myPeerListListener);

        registerReceiver(mReceiver, mIntentFilter);

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }


    private byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return inetAddress.getAddress();
                        }
                        //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
                    }
                }
            }
        } catch (SocketException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return null;
    }

    private String getDottedDecimalIP(byte[] ipAddr) {
        //convert to dotted decimal notation:
        String ipAddrStr = "";
        for (int i=0; i<ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i]&0xFF;
        }
        return ipAddrStr;
    }

    private static String getMacAddr() {
        String macAddresses = "";
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                //if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    continue;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                macAddresses = macAddresses + ", " + nif.getName() + " : " +  res1.toString();
            }
        } catch (Exception ex) {
            //handle exception
        }
        return macAddresses;
    }

    private void printInfo(){
        mManager.requestConnectionInfo (mChannel,
                mListener);
    }




}
