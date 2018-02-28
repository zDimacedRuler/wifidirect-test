package com.example.bishakh.wifidirecttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    LinearLayout mainlayout;

    IntentFilter mIntentFilter;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    WifiP2pManager.ConnectionInfoListener mListener;

    WifiP2pManager.PeerListListener myPeerListListener =  new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            Log.v("DEBUG", "Device list:");
            Collection<WifiP2pDevice> deviceList = wifiP2pDeviceList.getDeviceList();
            int devicesCount = deviceList.size();
            int iii = 0;
            final Button deviceButtons[] = new Button[devicesCount];
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
                mainlayout.addView(deviceButtons[iii]);
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainlayout = new LinearLayout(getApplicationContext());
        setContentView(mainlayout);

        Button discoverButton = new Button(getApplicationContext());
        discoverButton.setText("Discover Peers");
        mainlayout.addView(discoverButton);
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
                try {

                    Log.v("DEBUG", "Connection Group Owner address: " + wifiP2pInfo.groupOwnerAddress.toString());
                }catch (Exception e){}

                Log.v("DEBUG", "Group Formed: " + wifiP2pInfo.groupFormed);

                String ip = getDottedDecimalIP(getLocalIPAddress());

                Log.v("DEBUG", "My IP: " + ip);


            }
        };
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




}
