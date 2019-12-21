import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class FTPResponse {
    private int responseCode = -1;
    private List<String> listFile = new ArrayList<String>();;
    private String message;

    public FTPResponse() {
        super();
    }

    public FTPResponse(String st) {
        Gson gson = new Gson();

        FTPResponse response = gson.fromJson(st, FTPResponse.class);
        this.setMessage(response.getMessage());
        this.setListFile(response.getListFile());
        this.setResponseCode(response.getResponseCode());
    }

    public List<String> getListFile() {
        return listFile;
    }

    public void setListFile(List<String> listFile) {
        this.listFile = listFile;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        String toStr = "";
        if (responseCode != -1) {
         toStr += responseCode + " ";
        }
        toStr += this.getMessage();
        if (!listFile.isEmpty()) {
            // toStr += listFile.toString();
            for(String fi : listFile) {
                toStr += "\n" + fi;
            }
        }
        return toStr;
    }

    public static FTPResponse permissionDenied() {
        FTPResponse response = new FTPResponse();
        response.setResponseCode(FTPResponseCode.PERMISSION_DENIED);
        response.setMessage(FTPMessage.PERMISSION_DENIED);
        return response;
    }
}
