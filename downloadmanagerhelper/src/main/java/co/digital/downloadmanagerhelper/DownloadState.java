package co.digital.downloadmanagerhelper;

/**
 * Created by clickapps on 4/12/17.
 */

public class DownloadState {


    private String status;
    private String reason;
    private int percent;


    //Getter


    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public int getPercent() {
        return percent;
    }


    //Setter


    public void setStatus(String status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}

