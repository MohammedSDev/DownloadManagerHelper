package co.digital.downloadmanagerhelper;

/**
 * Created by clickapps on 4/12/17.
 */

public interface DownloadMHelperCallback {

    void onSuccess(long id,String filePath,int taskId);
    void onError(String s,int taskId);
}
