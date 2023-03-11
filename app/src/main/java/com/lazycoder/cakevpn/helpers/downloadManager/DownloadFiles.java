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

//    /**
//     * Before starting background thread Show Progress Bar Dialog
//     * */
//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//        showDialog(progress_bar_type);
//    }

    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(String[] fileNames) {
        int count;

        for(String fileName : fileNames)
        try {
            URL url = new URL(blobUrl+fileName);
            URLConnection connection = url.openConnection();
            connection.connect();

            // progress bar
            int lenghtOfFile = connection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(),
                    8192);
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

            File file = new File(baseDir, "ovpn-files" + File.separator + fileName);

            // Output stream
            FileOutputStream output = new FileOutputStream(file);

            byte data[] = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

        } catch (Exception e) {
           Log.e("Error: ", e.getMessage());
        }

        return null;
    }

//    /**
//     * Updating progress bar
//     * */
//    protected void onProgressUpdate(String... progress) {
//        // setting progress percentage
//        pDialog.setProgress(Integer.parseInt(progress[0]));
//    }

//    /**
//     * After completing background task Dismiss the progress dialog
//     * **/
//    @Override
//    protected void onPostExecute(String file_url) {
//        // dismiss the dialog after the file was downloaded
//        dismissDialog(progress_bar_type);
//
//    }

}