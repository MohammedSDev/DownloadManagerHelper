package co.digital.downloadmanagerhelper;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by clickapps on 4/12/17.
 */

public class DownloadManagerHelper {

    private static final int MY_PERMISSIONS_CODE = 1044;
    private static DownloadManagerHelper INSTANE = null;
    private DownloadManager mDownloadManager;
    private BroadcastReceiver mDownloadReceiver;
    private BroadcastReceiver mDownloadClickReceiver;

    //builder
    private Activity context;
    private DownloadMHelperCallback mCallback;
    private OnNotifiClickCallback mNotifClickedCallback;
    private Builder builder;
    private Map<Integer,Long> mTaskIds; /*<taskId,downloadId>*/

    public static DownloadManagerHelper getInstance(Activity context) {
        if (INSTANE == null) {
            synchronized (DownloadManagerHelper.class) {
                if (INSTANE == null)
                    INSTANE = new DownloadManagerHelper(context);
            }
        }else {
//            INSTANE.context = context;
        }

        return INSTANE;
    }

    private DownloadManagerHelper(Activity context) {
        this.context = context;
        this.mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        mTaskIds = new HashMap<>();

    }


    /**
     * enqueue new download in Download load  after check
     *
     * @param builder DownloadManagerHelper.Builder
     */
    public void startNewDownload(Builder builder) {
        mCallback = builder.mCallback;
        this.builder = builder;
        mNotifClickedCallback = builder.mOnNotifiClickCallback;

        if (!checkNetworkPermissionsIsOk()) {
            if (mCallback != null)
                mCallback.onError("Network Permissions required",builder.mTaskID);
            return;
        } else if (!checkConnection()) {
            if (mCallback != null)
                mCallback.onError("Oops... No Internet connection",builder.mTaskID);
            return;
        } else if (builder.mPublicDir && !checkWritePermissionIsOk()) {
            requestRuntimeWritePermission();
            return;
        }


        DownloadManager.Request buildReq = builder.build();
        if (buildReq == null) {
            if (mCallback != null)
                mCallback.onError("context & UrlPath params are required",builder.mTaskID);
        } else {
            long enqueueId = mDownloadManager.enqueue(buildReq);
            mTaskIds.put(builder.mTaskID,enqueueId);
            //clear builder
            this.builder = null;
        }
    }


    /*Status methods*/

    public int getPercentStatue(long downloadId) {
        Cursor cursor = checkStatus(downloadId);
        if (cursor == null)
            return -1;
        double total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        double downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        double l = (downloaded / total) * 100;
        return (int) l;

    }

    /**
     * GET DOWNLOAD CURSOR FROM DOWNLOAD MANAGER BY download_id
     */
    private Cursor checkStatus(long downloadId) {

        DownloadManager.Query ImageDownloadQuery = new DownloadManager.Query();
        //set the query filter to our previously Enqueued download
        ImageDownloadQuery.setFilterById(downloadId);

        //Query the download manager about downloads that have been requested.
        Cursor cursor = mDownloadManager.query(ImageDownloadQuery);
        if (cursor.moveToFirst()) {
            return cursor;
        } else
            return null;
    }


    /**
     * CHECK DOWNLOAD STATUE AND CURRENT PERCENT
     */
    public DownloadState getDownloadStatus(long downloadId) {
        DownloadState state = new DownloadState();

        Cursor cursor = checkStatus(downloadId);
        if (cursor == null) {
            state.setStatus("Download file not found");
            return state;
        }
        //column for download  status
        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);
        //column for reason code if the download failed or paused
        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);
        //get the download filename
//        int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
//        String filename = cursor.getString(filenameIndex);


        switch (status) {
            case DownloadManager.STATUS_FAILED:
                state.setStatus("STATUS_FAILED");
                switch (reason) {
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        state.setReason("ERROR_CANNOT_RESUME");
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        state.setReason("ERROR_DEVICE_NOT_FOUND");
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        state.setReason("ERROR_FILE_ALREADY_EXISTS");
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        state.setReason("ERROR_FILE_ERROR");
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        state.setReason("ERROR_HTTP_DATA_ERROR");
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        state.setReason("ERROR_INSUFFICIENT_SPACE");
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        state.setReason("ERROR_TOO_MANY_REDIRECTS");
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        state.setReason("ERROR_UNHANDLED_HTTP_CODE");
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        state.setReason("ERROR_UNKNOWN");
                        break;
                }
                break;
            case DownloadManager.STATUS_PAUSED:
                state.setStatus("STATUS_PAUSED");
                switch (reason) {
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        state.setReason("PAUSED_QUEUED_FOR_WIFI");
                        break;
                    case DownloadManager.PAUSED_UNKNOWN:
                        state.setReason("PAUSED_UNKNOWN");
                        break;
                    case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                        state.setReason("PAUSED_WAITING_FOR_NETWORK");
                        break;
                    case DownloadManager.PAUSED_WAITING_TO_RETRY:
                        state.setReason("PAUSED_WAITING_TO_RETRY");
                        break;
                }
                state.setPercent(getPercentStatue(downloadId));
                break;
            case DownloadManager.STATUS_PENDING:
                state.setStatus("STATUS_PENDING");
                state.setPercent(getPercentStatue(downloadId));
                break;
            case DownloadManager.STATUS_RUNNING:
                state.setStatus("STATUS_RUNNING");
                state.setPercent(getPercentStatue(downloadId));
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                state.setStatus("STATUS_SUCCESSFUL");
                state.setPercent(100);
                break;
        }

        return state;
    }

    /**
     * GET DOWNLOAD ID USING TASK ID
     * */
    public long getDownloadId(int taskId){
//        if (taskId != 0)
        Long aLong = mTaskIds.get(taskId);
        if (aLong == null)
            aLong = -1l;
        return aLong;
    }

    /**
     * GET TASK ID USING DOWNLOAD ID
     * */
    private int getTaskId(long id){
        for (Map.Entry<Integer, Long> item : mTaskIds.entrySet()){
            if (item.getValue() == id) {
                return item.getKey();
            }
        }
        return -1;
    }

    /**
     * CANCEL UN COMPLETED DOWNLOAD OPERATION
     */
    public void cancel(long id) {
        int xx = mDownloadManager.remove(id);
        Log.d("mud", "cancel: id: " + xx);
    }

    /**
     * GET URI FOR FILE DOWNLOADING.
     */
    public Uri getDownloadUri(long id) {
        return mDownloadManager.getUriForDownloadedFile(id);
    }

    @Deprecated
    public Uri showFileContent(long id) {
        Uri uri = mDownloadManager.getUriForDownloadedFile(id);
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setData(uri);
//        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

       /* Intent shareIntent = new Intent(); //work fine
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image*//*");*/

        Log.d("mud", "showFileContent: fileProvider: " + context.getPackageName() + ".fileprovider");
        Uri uriForFile =
                null;
        try {
            uriForFile = FileProvider.getUriForFile(context
                    , context.getPackageName() + ".fileprovider"
                    , new File(getFilePath(context, uri)));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
//        context.grantUriPermission(context.getPackageName(),uriForFile,Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_VIEW);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
//        shareIntent.setData(uriForFile);
        shareIntent.setDataAndType(uriForFile, "image/*");

//        shareIntent.setType("image/*");

//        Intent intent = Intent.createChooser(target, "open download file");
        Intent intent = Intent.createChooser(shareIntent, "open download file");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "can't open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return uri;
    }


    /*Listener init*/

    /**
     * INITIALIZE CALLBACKS.SHOULD CALL ON onStart ACTIVITY/FRAGMENT CALLBACK
     * */
    public static void initOnStart(Activity context){
        getInstance(context).registerReceivers();
    }

    /**
     * * UN INITIALIZE CALLBACKS.SHOULD CALL ON onStop ACTIVITY/FRAGMENT CALLBACK
     * */
    public static void unInitOnStop(Activity context){
        getInstance(context).unRegisterReceivers();
    }

    /**
     * REGISTER ON DOWNLOAD COMPLETE OR CANCEL & ON NOTIFICATION DOWNLOAD CLICKED BROADCAST RECEIVERS
     * */
    private void registerReceivers() {


        if (mDownloadReceiver == null) {
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

            mDownloadReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    //check if the broadcast message is for our enqueued download
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                    if (mCallback != null) {
                        DownloadState downloadStatus = getDownloadStatus(id);
                        if (downloadStatus != null && downloadStatus.getStatus().equalsIgnoreCase("STATUS_SUCCESSFUL")) {
                            Uri downloadUri = getDownloadUri(id);
                            String path = null;
                            try {
                                path = getFilePath(context, downloadUri);
                            } catch (URISyntaxException e) {
                                e.printStackTrace();
                            }
                            mCallback.onSuccess(id, path,getTaskId(id));
                        } else {
                            mCallback.onError("Download canceled",getTaskId(id));
                        }
                    }

                }
            };
            context.registerReceiver(mDownloadReceiver, filter);
        }

        //user click on download progress
        if (mDownloadClickReceiver == null) {
            IntentFilter filterClick = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);

            mDownloadClickReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {

                    //check if the broadcast message is for our enqueued download
                    long[] ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
                    if (ids != null && ids.length > 0) {
                        if (mNotifClickedCallback != null)
                            mNotifClickedCallback.onNotificationClicked(ids);
                    }
                }
            };


            context.registerReceiver(mDownloadClickReceiver, filterClick);
        }
    }

    /**
     * * UN REGISTER ON DOWNLOAD COMPLETE OR CANCEL & ON NOTIFICATION DOWNLOAD CLICKED BROADCAST RECEIVERS
     * */
    public void unRegisterReceivers() {
        if (context != null) {
            if (mDownloadReceiver != null)
                context.unregisterReceiver(mDownloadReceiver);
            if (mDownloadClickReceiver != null)
                context.unregisterReceiver(mDownloadClickReceiver);

            mDownloadReceiver = null;
            mDownloadClickReceiver = null;
        }
    }


    /*Validation methods*/

    /**
     * check required network permissions
     */
    private boolean checkNetworkPermissionsIsOk() {
        PackageManager pm = context.getPackageManager();
        if (pm.checkPermission(Manifest.permission.INTERNET, context.getPackageName())
                != PackageManager.PERMISSION_GRANTED
                || pm.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE, context.getPackageName())
                != PackageManager.PERMISSION_GRANTED) {

            return false;
        } else {
            return true;
        }
    }

    /**
     * check device is connected or connecting
     */
    private boolean checkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;

    }

    /**
     * CHECK PERMISSION GRANTED
     */
    private boolean checkWritePermissionIsOk() {
        PackageManager pm = context.getPackageManager();
        return pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context.getPackageName()) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }



    /*Utils methods*/

    /**
     * REQUEST RUNTIME PERMISSION
     */
    private void requestRuntimeWritePermission() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                new AlertDialog.Builder(context)
                        .setTitle("Write Permission required")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //request the permission.
                                ActivityCompat.requestPermissions(context,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_CODE);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //nothing,just clear builder field
                                if (mCallback != null)
                                    mCallback.onError("User prevent write permission ",builder.mTaskID);
                                DownloadManagerHelper.this.builder = null;
                            }
                        })
                        .show();

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_CODE);
            }
        }
    }


    /**
     * ON REQUEST PERMISSIONS CALLBACK
     * this should be call in same 'this.context' activity -> onRequestPermissionsResult
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // re start last attempt new download
                startNewDownload(builder);

            } else {

                // permission denied,clear builder field
                if (mCallback != null)
                    mCallback.onError("User prevent write permission ",builder.mTaskID);
                this.builder = null;
            }
        }
    }


    /**
     * GET STRING PATH FROM URI
     */
    private String getFilePath(Context context, Uri uri) throws URISyntaxException {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    //class
    public static class Builder {

        private Context context;
        private String mTitle;
        private String mDescription;
        private String mFileName;
        private String mUrl;
        private int mTaskID;
        private boolean mPublicDir;
        private DMHNotificationVisibility mVisibility;
        private DownloadMHelperCallback mCallback;
        private OnNotifiClickCallback mOnNotifiClickCallback;
        private Map<String, String> mHeader;

        public Builder(Context context) {
            this.context = context;
        }


        public Builder setTitle(String mTitle) {
            this.mTitle = mTitle;
            return this;
        }

        public Builder setDescription(String mDescription) {
            this.mDescription = mDescription;
            return this;
        }

        public Builder setFileName(String mFileName) {
            this.mFileName = mFileName;
            return this;
        }

        public Builder setUrl(String mUrl) {
            this.mUrl = mUrl;
            return this;
        }

        public Builder setCallback(DownloadMHelperCallback mDownloadMHelperCallbackListener) {
            this.mCallback = mDownloadMHelperCallbackListener;
            return this;
        }

        public Builder setOnNotifiClickCallback(OnNotifiClickCallback mOnNotifiClickCallbackListener) {
            this.mOnNotifiClickCallback = mOnNotifiClickCallbackListener;
            return this;
        }

        public Builder setHeader(Map<String, String> mHeader) {
            this.mHeader = mHeader;
            return this;
        }

        public Builder setVisibility(DMHNotificationVisibility mVisibility) {
            this.mVisibility = mVisibility;
            return this;
        }

        public Builder setPublicDir(boolean mPublicDir) {
            this.mPublicDir = mPublicDir;
            return this;
        }

        public Builder setTaskID(int mTaskID) {
            this.mTaskID = mTaskID;
            return this;
        }

        private DownloadManager.Request build() {
            if (context == null) {
                return null;
            }

            if (isEmpty(mUrl)) {
                return null;
            }

            if (mTaskID == 0) {
                return null;
            }

            Uri uri;
            try {
                uri = Uri.parse(mUrl);
            } catch (IllegalArgumentException e) {
                return null;
            }


            if (isEmpty(mFileName)) {
//                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(uri));
//                String extension = MimeTypeMap.getFileExtensionFromUrl(new File(uri.getPath()).getAbsolutePath());
                mFileName = mUrl.substring(mUrl.lastIndexOf("/") + 1);
            }else {
                if (mFileName.lastIndexOf(".") == -1 || mFileName.substring(mFileName.lastIndexOf(".")).length() < 4)
                    mFileName = mFileName + "." + mUrl.substring(mUrl.lastIndexOf(".") + 1);
            }
            DownloadManager.Request req = new DownloadManager.Request(uri);

            req.setTitle(mTitle);
            req.setDescription(mDescription);
            if (!mPublicDir) {
                req.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, mFileName);
            } else {
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mFileName);
            }

//        req.setVisibleInDownloadsUi(false);
            if (mHeader != null && mHeader.size() > 0) {
                Map.Entry<String, String> item;
                while (mHeader.entrySet().iterator().hasNext()) {
                    item = mHeader.entrySet().iterator().next();
                    req.addRequestHeader(item.getKey(), item.getValue());
                }
            }
            if (mVisibility != null)
                req.setNotificationVisibility(mVisibility.getVal());


            return req;

        }


        // validation
        private boolean isEmpty(String text) {
            return (text == null || text.equalsIgnoreCase("null") || text.length() <= 0);
        }


    }

    /**
     * NOTIFICATION VISIBILITY TYPES
     */
    public enum DMHNotificationVisibility {
        VISIBLE_TILL_COMPLETE {
            @Override
            public int getVal() {
                return DownloadManager.Request.VISIBILITY_VISIBLE;

            }
        }, VISIBLE_TILL_AND_AFTER_COMPLETE {
            @Override
            public int getVal() {
                return DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
            }
        }, HIDE {
            @Override
            public int getVal() {
                return DownloadManager.Request.VISIBILITY_HIDDEN;
            }
        };

        private int val;

        public abstract int getVal();
    }
}
