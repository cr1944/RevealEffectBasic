package ryancheng.okhttp;

import java.util.ArrayList;
import java.util.List;

/**
 * Create time: 2016/10/9.
 */

public class UploadData {
    public String clientip = "";
    public String carrierName = "";
    public List<String> clientdns = new ArrayList<>();
    public List<String> dnsparser = new ArrayList<>();
    public List<Request> request_info = new ArrayList<>();

    public static class Request {
        public String requesturl = "";
        public String requestheader = "";
        public String responseheader = "";
        public String responsebody = "";
    }
}
