package com.gbs_soft.galib_web.helper;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gbs_soft.galib_web.controller.MyControl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ChromeClient extends WebChromeClient {

    Activity activity;
    static MyHelper myHelper;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    protected FrameLayout mFullscreenContainer;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

    public ChromeClient(MyHelper myHelper) {
        ChromeClient.myHelper = myHelper;
    }

    public ChromeClient(Activity activity) {
        this.activity = activity;
    }

    public ChromeClient() {
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (newProgress >= 100) {
            // Page loading finish
            /*  progressBar.setVisibility(View.GONE);*/
            myHelper.finishLoading();
        } else {
            /*progressBar.setVisibility(View.VISIBLE);*/
            myHelper.loading();
        }
    }

    public Bitmap getDefaultVideoPoster() {
        if (mCustomView == null) {
            return null;
        }
        return BitmapFactory.decodeResource(activity.getResources(), 2130837573);
    }

    public void onHideCustomView() {
        ((FrameLayout) activity.getWindow().getDecorView()).removeView(this.mCustomView);
        this.mCustomView = null;
        activity.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
        activity.setRequestedOrientation(this.mOriginalOrientation);
        this.mCustomViewCallback.onCustomViewHidden();
        this.mCustomViewCallback = null;
    }

    public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
        if (this.mCustomView != null) {
            onHideCustomView();
            return;
        }
        this.mCustomView = paramView;
        this.mOriginalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
        this.mOriginalOrientation = activity.getRequestedOrientation();
        this.mCustomViewCallback = paramCustomViewCallback;
        ((FrameLayout) activity.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
        activity.getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    //============================================
    //============================================

    /*-- handling input[type="file"] requests for android API 21+ --*/
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

        if(file_permission() && Build.VERSION.SDK_INT >= 21) {
            MyControl.file_path = filePathCallback;
            Intent takePictureIntent = null;
            Intent takeVideoIntent = null;

            boolean includeVideo = false;
            boolean includePhoto = false;

            /*-- checking the accept parameter to determine which intent(s) to include --*/

            paramCheck:
            for (String acceptTypes : fileChooserParams.getAcceptTypes()) {
                String[] splitTypes = acceptTypes.split(", ?+");
                /*-- although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values --*/
                for (String acceptType : splitTypes) {
                    switch (acceptType) {
                        case "*/*":
                            includePhoto = true;
                            includeVideo = true;
                            break paramCheck;
                        case "image/*":
                            includePhoto = true;
                            break;
                        case "video/*":
                            includeVideo = true;
                            break;
                    }
                }
            }

            if (fileChooserParams.getAcceptTypes().length == 0) {

                /*-- no `accept` parameter was specified, allow both photo and video --*/

                includePhoto = true;
                includeVideo = true;
            }

            if (includePhoto) {
                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = create_image();
                        takePictureIntent.putExtra("PhotoPath", MyControl.cam_file_data);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        MyControl.cam_file_data = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        MyControl.cam_file_data = null;
                        takePictureIntent = null;
                    }
                }
            }

            if (includeVideo) {
                takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (takeVideoIntent.resolveActivity(activity.getPackageManager()) != null) {
                    File videoFile = null;
                    try {
                        videoFile = create_video();
                    } catch (IOException ex) {
                        Log.e(TAG, "Video file creation failed", ex);
                    }
                    if (videoFile != null) {
                        MyControl.cam_file_data = "file:" + videoFile.getAbsolutePath();
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                    } else {
                        MyControl.cam_file_data = null;
                        takeVideoIntent = null;
                    }
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType(MyControl.file_type);


            Intent[] intentArray;
            if (takePictureIntent != null && takeVideoIntent != null) {
                intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
            } else if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else if (takeVideoIntent != null) {
                intentArray = new Intent[]{takeVideoIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            activity.startActivityForResult(chooserIntent, MyControl.file_req_code);
            return true;
        } else {
            return false;
        }
    }

    public boolean file_permission(){


        /*
        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);
            return false;
        }else{
            return true;
        }
         */


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
                Log.v("WebBrowser", "Permission Requested..");
                return false;
            } else return true;

        }





        else if(Build.VERSION.SDK_INT >=23){

            if(ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(
                        activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                Log.v("WebBrowser", "Permission Requested..");
                return false;
            }else return true;

        }

        else return true;


    }

    private File create_image() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    private File create_video() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".3gp", sd_directory);
    }


} // ChromeClient End Here =============
