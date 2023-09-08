package com.gbs_soft.galib_web.helper;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MyWebDownloader {

    static MyHelper myHelper;

    public MyWebDownloader(MyHelper myHelper) {
        MyWebDownloader.myHelper = myHelper;
    }



    public MyWebDownloader() {
    }

    public static void WebDownloader(WebView webView, Activity activity){
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(final String url, final String userAgent, String contentDisposition, String mimetype, long contentLength) {


                if (Build.VERSION.SDK_INT >=33 ){

                    if(ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
                            &&
                            ContextCompat.checkSelfPermission(activity,
                                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED
                            &&
                            ContextCompat.checkSelfPermission(activity,
                                    Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED
                    ) {

                        ActivityCompat.requestPermissions(
                                activity, new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO
                                }, 1);

                        Log.v("WebBrowser", "Permission is revoked");
                        myHelper.finishLoading();


                    }

                    else {
                        Log.v("WebBrowser", "Permission is granted");
                        downloadDialog(url, userAgent, contentDisposition, mimetype, activity);
                    }

                } // end IF


                //Checking runtime permission for devices above Marshmallow.
                else if (Build.VERSION.SDK_INT >= 23) {
                    if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        Log.v("WebBrowser", "Permission is granted");
                        downloadDialog(url, userAgent, contentDisposition, mimetype, activity);

                    } else {
                        Log.v("WebBrowser", "Permission is revoked");
                       /* progressBar.setVisibility(View.GONE);*/
                        myHelper.finishLoading();
                        //requesting permissions.
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

                    }
                } else {
                    //Code for devices below API 23 or Marshmallow
                    Log.v("WebBrowser", "Permission is granted");
                    downloadDialog(url, userAgent, contentDisposition, mimetype, activity);
                    /*progressBar.setVisibility(View.GONE);*/
                    myHelper.finishLoading();

                }


            }
        });
    }

    public static void downloadDialog(final String url, final String userAgent, String contentDisposition, String mimetype, Activity activity) {
        //getting filename from url.
        final String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
        //alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        //title of alertdialog
        builder.setTitle("Downloading");
        //message of alertdialog
        builder.setMessage("We are trying to download: " + ' ' + filename);
        //if Yes button clicks.

        builder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //DownloadManager.Request created with url.
                /*progressBar.setVisibility(View.GONE);*/
                myHelper.finishLoading();
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                //cookie
                String cookie = CookieManager.getInstance().getCookie(url);
                //Add cookie and User-Agent to request
                request.addRequestHeader("Cookie", cookie);
                request.addRequestHeader("User-Agent", userAgent);
                //file scanned by MediaScannar
                request.allowScanningByMediaScanner();
                //Download is visible and its progress, after completion too.
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                //DownloadManager created
                DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                //Saving files in Download folder
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                //download enqued
                downloadManager.enqueue(request);


                BroadcastReceiver onComplete = new BroadcastReceiver() {
                    public void onReceive(Context ctxt, Intent intent) {
                        Toast.makeText(activity, "Download Complete", Toast.LENGTH_SHORT).show();
                    }
                };

                activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            }
        });

        builder.setNegativeButton("NOT NOW", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //cancel the dialog if Cancel clicks
                /*progressBar.setVisibility(View.GONE);*/
                myHelper.finishLoading();
                dialog.cancel();
                myHelper.webGoBack();
            }

        });
        //alertdialog shows.
        builder.show();

    } // downloadDialog End Here ==========================

} // MyWebDownloader End Here =====================
