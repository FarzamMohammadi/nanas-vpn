package com.lazycoder.cakevpn.helpers.downloadManager;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public class DownloadFiles extends AsyncTask<String, String, String> {
    private String blobUrl = "";

    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(String[] fileNames) {
        int count;

        for(String fileName : fileNames){
            try {
                URL url = new URL(blobUrl + fileName);
                URLConnection connection = url.openConnection();
                connection.connect();

                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);
                String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

                File file = new File(baseDir, "ovpn-files" + File.separator + fileName);

                FileOutputStream output = new FileOutputStream(file);

                byte data[] = new byte[1024];

                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.write(data, 0, count);
                output.flush();

                output.close();
                input.close();

            } catch (Exception e) {
                if (e.getClass().getCanonicalName() == ArrayIndexOutOfBoundsException.class.getCanonicalName()){
                    Log.i("Successful Download", blobUrl + fileName);
                    continue;
                }

                Log.e("Unable to Download", e.getMessage());
            }
        }

        return null;
    }

}