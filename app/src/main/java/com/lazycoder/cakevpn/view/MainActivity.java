package com.lazycoder.cakevpn.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import com.lazycoder.cakevpn.R;
import com.lazycoder.cakevpn.adapter.ServerListRVAdapter;
import com.lazycoder.cakevpn.helpers.downloadManager.DownloadFiles;
import com.lazycoder.cakevpn.interfaces.ChangeServer;
import com.lazycoder.cakevpn.interfaces.NavItemClickListener;
import com.lazycoder.cakevpn.model.Server;

import java.io.File;
import java.util.ArrayList;

import com.lazycoder.cakevpn.Utils;


public class MainActivity extends AppCompatActivity implements NavItemClickListener {
    private FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    private Fragment fragment;
    private RecyclerView serverListRv;
    private ArrayList<Server> serverLists;
    private ServerListRVAdapter serverListRVAdapter;
    private DrawerLayout drawer;
    private ChangeServer changeServer;

    public static final String TAG = "CakeVPN";
    public static File dir = new File(new File(Environment.getExternalStorageDirectory(), "ovpn-files"), "");
    String[] ovpnFileNames = {"1.ovpn", "2.ovpn", "3.ovpn", "4.ovpn", "5.ovpn", "6.ovpn", "7.ovpn", "8.ovpn", "9.ovpn", "10.ovpn"};

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

        initializeAll();

        downloadVpnConfigurationFiles();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        transaction.add(R.id.container, fragment);
        transaction.commit();

        // Server List recycler view initialize
        if (serverLists != null) {
            serverListRVAdapter = new ServerListRVAdapter(serverLists, this);
            serverListRv.setAdapter(serverListRVAdapter);
        }

    }

    /**
     * Initialize all object, listener etc
     */
    private void initializeAll() {
        drawer = findViewById(R.id.drawer_layout);

        fragment = new MainFragment();
        serverListRv = findViewById(R.id.serverListRv);
        serverListRv.setHasFixedSize(true);

        serverListRv.setLayoutManager(new LinearLayoutManager(this));

        serverLists = getServerList();
        changeServer = (ChangeServer) fragment;

    }

    /**
     * Close navigation drawer
     */
    public void closeDrawer(){
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            drawer.openDrawer(GravityCompat.END);
        }
    }

    /**
     * Generate server array list
     */
    private ArrayList getServerList() {

        ArrayList<Server> servers = new ArrayList<>();

        for (String ovpnFileName : ovpnFileNames){
            servers.add(new Server(
                    "",
                    Utils.getImgURL(R.drawable.usa_flag),
                    dir.toString() + ovpnFileName,
                    "",
                    ""));
        }

        return servers;
    }

    /**
     * On navigation item click, close drawer and change server
     * @param index: server index
     */
    @Override
    public void clickedItem(int index) {
        closeDrawer();
        changeServer.newServer(serverLists.get(index));
    }
}
