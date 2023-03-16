package com.lazycoder.cakevpn.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.lazycoder.cakevpn.CheckInternetConnection;
import com.lazycoder.cakevpn.R;
import com.lazycoder.cakevpn.SharedPreference;
import com.lazycoder.cakevpn.databinding.FragmentMainBinding;
import com.lazycoder.cakevpn.interfaces.ChangeServer;
import com.lazycoder.cakevpn.model.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements View.OnClickListener, ChangeServer {

    private Server currentServer;
    private ArrayList<Server> servers;
    private CheckInternetConnection connection;

    private OpenVPNThread vpnThread = new OpenVPNThread();
    private OpenVPNService vpnService = new OpenVPNService();
    boolean vpnStarted = false;
    int numberOfConnectionRetries = 0;
    private SharedPreference preference;

    private FragmentMainBinding binding;

    String connect = "مادرجانم ین دکمه رو بزن که وسل شه";
    String connected = "مادرجانم وصل شود";
    String connecting = "مادرجانم درحاله وصل شودنه";
    String disconnect = "مادرجانم ین دکمه رو بزن که قطع بشه";
    String disconnected = "مادرجانم قطع شد";
    String noInternetConnection = "مادرجانم اینترنت قته، لطفآ به ون وسل شو";
    String unableToConnectToAnyServer = "";
    String defaultGreeting = "سلام مادر جون";
    String morningGreeting = "صبحت بخیر مادر جون";
    String afternoonGreeting = "عصرت بخیر مادر جون";
    String eveningGreeting = "عصرت بخیر مادر جون";

    private static WeakReference<MainActivity> mActivityRef;
    public static void updateActivity(MainActivity activity) {
        mActivityRef = new WeakReference<MainActivity>(activity);
    }

    /**
     * Receive broadcast message
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);

        View view = binding.getRoot();
        initializeAll();

        return view;
    }

    /**
     * Initialize all variable and object
     */
    private void initializeAll() {
        preference = new SharedPreference(getContext());

        MainActivity mainAcitivity = mActivityRef.get();

        servers = mainAcitivity.getServers();
        currentServer = servers.get(0);

        connection = new CheckInternetConnection();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.vpnBtn.setOnClickListener(this);

        try{
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH");

            int currentHour = Integer.parseInt(sdf.format(calendar.getTime())); //24hr format

            if (currentHour >= 3 && currentHour <= 12){
                binding.logTv.setText(morningGreeting);
            }
            else if (currentHour >= 12 && currentHour <= 18){
                binding.logTv.setText(afternoonGreeting);
            }
            else {
                binding.logTv.setText(eveningGreeting);
            }
        }
        catch (Exception e){
            binding.logTv.setText(defaultGreeting);
        }

        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(getActivity().getCacheDir());
    }

    /**
     * @param v: click listener view
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.vpnBtn:
                prepareVpn();
        }
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private void prepareVpn() {
        if (!vpnStarted) {
            if (getInternetStatus()) {

                // Checking permission for network monitor
                Intent intent = VpnService.prepare(getContext());

                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else startVpn();

                status("connecting");
            } else {
                showToast(noInternetConnection);
            }

        } else if (stopVpn()) {
            showToast(disconnected);
        }
    }

    /**
     * Stop vpn
     * @return boolean: VPN status
     */
    public boolean stopVpn() {
        try {
            vpnThread.stop();

            status("connect");
            vpnStarted = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Taking permission for network access
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //Permission granted, start the VPN
            startVpn();
        } else {
            showToast("Permission Deny !! ");
        }
    }

    /**
     * Internet connection status.
     */
    public boolean getInternetStatus() {
        return connection.netCheck(getContext());
    }

    /**
     * Get service status
     */
    public void isServiceRunning() {
        setStatus(vpnService.getStatus());
    }

    /**
     * Start the VPN
     */
    private void startVpn() {
        numberOfConnectionRetries = 0;

        try {
            File ovpnFile = new File(currentServer.getOvpn());
            BufferedReader br = new BufferedReader(new FileReader(ovpnFile));
            String config = "";
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }

            br.readLine();
            OpenVpnApi.startVpn(getContext(), config, currentServer.getCountry(), currentServer.getOvpnUserName(), currentServer.getOvpnUserPassword());

            String newConnectionAttemptMsg = "Now Connecting to" + currentServer.getOvpn();
            Log.i("New Server", newConnectionAttemptMsg);

            // Update log
            binding.logTv.setText(connecting);
            vpnStarted = true;

        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }
    /**
     * Status change with corresponding vpn connection status
     * @param connectionState
     */
    public void setStatus(String connectionState) {
        if (connectionState != null) {
            switch (connectionState) {
                case "DISCONNECTED":
                    status("connect");
                    vpnStarted = false;
                    vpnService.setDefaultStatus();
                    binding.logTv.setText(disconnected);
                    break;
                case "CONNECTED":
                    vpnStarted = true;// it will use after restart this activity
                    status("connected");
                    binding.logTv.setText(connected);
                    break;
                case "WAIT":
                    binding.logTv.setText(connecting);
                    break;
                case "AUTH":
                    binding.logTv.setText(connecting);
                    break;
                case "RECONNECTING":
                    if (numberOfConnectionRetries >= 1){
                        useNextAvailableVpn();
                        return;
                    }

                    numberOfConnectionRetries+=1;

                    status("connecting");
                    binding.logTv.setText(connecting);
                    break;
                case "NONETWORK":
                    binding.logTv.setText(noInternetConnection);
                    break;
            }
        }
    }

    /**
     * Change button background color and text
     * @param status: VPN current status
     */
    public void status(String status) {

        if (status.equals("connect")) {
            binding.vpnBtn.setText(connect);
        } else if (status.equals("connecting")) {
            binding.vpnBtn.setText(connecting);
        } else if (status.equals("connected")) {

            binding.vpnBtn.setText(disconnect);

        } else if (status.equals("tryDifferentServer")) {
            //TODO: Should automatically move to the next server
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected);
            binding.vpnBtn.setText("Try Different\nServer");
        } else if (status.equals("loading")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button);
            binding.vpnBtn.setText(connecting);
        } else if (status.equals("invalidDevice")) {
            //TODO: Should automatically move to the next server
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected);
            binding.vpnBtn.setText("Invalid Device");
        } else if (status.equals("authenticationCheck")) {
            binding.vpnBtn.setBackgroundResource(R.drawable.button_connecting);
            binding.vpnBtn.setText(connecting);
        }
    }

    private void resetServerBackToFirstInTheList(){
        Server firstServerInTheList = servers.get(0);

        currentServer = firstServerInTheList;

        stopVpn();
    }

    private void useNextAvailableVpn(){
        int currentServerIndex = servers.indexOf(currentServer);

        if (currentServerIndex + 1 == servers.size()){
            resetServerBackToFirstInTheList();

            binding.logTv.setText(unableToConnectToAnyServer);
        }

        Server nextAvailableServer = servers.get(currentServerIndex+1);

        newServer(nextAvailableServer);
    }

    /**
     * Show toast message
     * @param message: toast message
     */
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Change server when user select new server
     * @param server ovpn server details
     */
    @Override
    public void newServer(Server server) {
        this.currentServer = server;

        // Stop previous connection
        if (vpnStarted) {
            stopVpn();
        }

        prepareVpn();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));

        if (currentServer == null) {
            currentServer = preference.getServer();
        }
        super.onResume();
    }

    /**
     * Save current selected server on local shared preference
     */
    @Override
    public void onStop() {
        if (currentServer != null) {
            preference.saveServer(currentServer);
        }

        super.onStop();
    }
}
