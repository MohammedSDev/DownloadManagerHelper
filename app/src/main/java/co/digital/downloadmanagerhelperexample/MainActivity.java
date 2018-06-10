package co.digital.downloadmanagerhelperexample;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import java.io.File;

import co.digital.downloadmanagerhelper.DownloadMHelperCallback;
import co.digital.downloadmanagerhelper.DownloadManagerHelper;
import co.digital.downloadmanagerhelper.DownloadState;
import co.digital.downloadmanagerhelper.OnNotifiClickCallback;

public class MainActivity extends AppCompatActivity
        implements DownloadMHelperCallback, OnNotifiClickCallback {

    TextView tv;
    TextView tvS;
    TextView tvC;

    private DownloadManagerHelper helper;
    long l;
    private final String TAG = "mud";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        helper = DownloadManagerHelper.getInstance(this);
        helper.startNewDownload(new DownloadManagerHelper.Builder(null));

        tv = (TextView) findViewById(R.id.textview);
        tvS = (TextView) findViewById(R.id.textview_state);
        tvC = (TextView) findViewById(R.id.textview_cancel);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DownloadManagerHelper.Builder builder = new DownloadManagerHelper.Builder(MainActivity.this)
                        .setTitle("Title")
                        .setDescription("Desc")
                        .setFileName("filename")
                        .setUrl("http://download.quranicaudio.com/quran/abu_bakr_ash-shatri_tarawee7/025.mp3")
                        .setCallback(MainActivity.this)
                        .setOnNotifiClickCallback(MainActivity.this)
                        .setPublicDir(true)
                        .setTaskID(102)
                        .setVisibility(DownloadManagerHelper.DMHNotificationVisibility.VISIBLE_TILL_AND_AFTER_COMPLETE);
                helper.startNewDownload(builder);
            }
        });

//        statue
        tvS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long downloadId = helper.getDownloadId(102);
                Log.d(TAG, "onClick: downlpad Id:  " + downloadId);
                DownloadState downloadStatus = helper.getDownloadStatus(downloadId);
                Log.d(TAG, "onClick: status: " + downloadStatus.getStatus());
                Log.d(TAG, "onClick: reason: " + downloadStatus.getReason());
                Log.d(TAG, "onClick: percent: " + downloadStatus.getPercent());
            }
        });

//        cancel
        tvC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                long downloadId = helper.getDownloadId(102);
                Log.d(TAG, "onClick: downlpad Id:  " + downloadId);
                helper.cancel(downloadId);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DownloadManagerHelper.getInstance(this).onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    public void onSuccess(long id,String path,int taskId) {
        Log.d(TAG, "onSuccess: id: "+ id);
        Log.d(TAG, "onSuccess: path: "+ path);
        Log.d(TAG, "onSuccess: taskId: "+ taskId);

        //test
        String s = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(path);
        intent.setDataAndType(Uri.fromFile(file), s);
        startActivity(Intent.createChooser(intent,"play mp3"));

        Log.d(TAG, "onSuccess: s: " + s);
    }


    @Override
    public void onError(String s, int taskId) {
        Log.d(TAG, "onError: "+ s);
        Log.d(TAG, "onError:taskId: "+ taskId);
    }

    @Override
    public void onNotificationClicked(long[] ids) {
        for (long l :ids) {
            Log.d(TAG, "onNotificationClicked: ids: " + l);
        }
    }




    /**** LifeCycle****/
    @Override
    protected void onStart() {
        super.onStart();
        DownloadManagerHelper.initOnStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DownloadManagerHelper.unInitOnStop(this);
    }



    /****End*/


}

/*http://download.quranicaudio.com/quran/abu_bakr_ash-shatri_tarawee7/002.mp3*/