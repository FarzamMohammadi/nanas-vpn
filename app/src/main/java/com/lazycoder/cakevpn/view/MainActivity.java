package com.lazycoder.cakevpn.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import com.lazycoder.cakevpn.R;
import com.lazycoder.cakevpn.helpers.downloadManager.DownloadFiles;
import com.lazycoder.cakevpn.interfaces.ChangeServer;
import com.lazycoder.cakevpn.model.Server;

import java.io.File;
import java.util.ArrayList;

import com.lazycoder.cakevpn.Utils;


public class MainActivity extends AppCompatActivity {
    private FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    private Fragment fragment;
    public static File dir = new File(new File(Environment.getExternalStorageDirectory(), "ovpn-files"), "");
    String[] ovpnFileNames = {"1.ovpn", "2.ovpn"};

    private ArrayList<Server> servers; // private = restricted access
    public ArrayList<Server> getServers() {
        return servers;
    }
    public void setServers(ArrayList<Server> servers) {
        this.servers = servers;
    }

    public void createDir(){
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    private void downloadVpnConfigurationFiles(){
        new DownloadFiles().execute(ovpnFileNames);
    }

    public void getPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
            createDir();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissions();

        downloadVpnConfigurationFiles();

        initStart();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        transaction.add(R.id.container, fragment);
        transaction.commit();

        MainFragment.updateActivity(this);
    }

    private void initStart() {
        fragment = new MainFragment();

        setServers(getServerList());
    }

    private ArrayList<Server> getServerList() {

        ArrayList<Server> servers = new ArrayList<>();

        for (String ovpnFileName : ovpnFileNames){
            String filePath = dir.toString() + File.separator + ovpnFileName;
            boolean foundVpnConfigFile = new File(filePath).exists();

            if (foundVpnConfigFile) {
                servers.add(new Server(
                        "japan",
                        Utils.getImgURL(R.drawable.usa_flag),
                        dir.toString() + File.separator + ovpnFileName,
                        "",
                        ""));
            }
        }

        return servers;
    }
}
