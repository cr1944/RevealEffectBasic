package ryancheng.okhttp;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParsingMode;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextView textView;
    Button button1, button2;
    EditText editText;
    private TestTask testTask;
    private UploadData uploadData;
    private boolean downloading;
    private int index;

    private static final String[] URLS = {
            "http://www.applexe.com/ios",
            "http://cc.xtgreat.com/cm.gif?dspid=11114",
            "https://www.idescout.com/secure/downloads/SQLScout-2.0.5.zip",
            "http://v.namibox.com/material_video/v_002644/v_002644.m3u8?pm3u8/0&e=1476233345&token=yN-wMdOklbPBGDK9HQ9rauvZshy1EfeAFPA8kWLq:wdL17rSnEM5wGDlJ5t5vIZkdbsE=",
            "http://v.namibox.com/u7wPKxRMeOWP3oNt3QCX7_8Ot1w=/ljxaKNboxSx3FiCrZG3XjRC1Vk8h/000000.ts?e=1476111835&token=yN-wMdOklbPBGDK9HQ9rauvZshy1EfeAFPA8kWLq:Ht2Tn4LaO3eJJpGpY2bu9bL9qxI"
    };

    private static final String[] TYPES = {
            "false",
            "false",
            "true",
            "false"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        button1 = (Button) findViewById(R.id.btn);
        button2 = (Button) findViewById(R.id.btn2);
        editText = (EditText) findViewById(R.id.edit);
        editText.setText(URLS[0]);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloading) {
                    cancel();
                } else {
                    download();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 1, "切换地址").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            index++;
            if (index >= URLS.length) {
                index = 0;
            }
            editText.setText(URLS[index]);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (testTask != null) {
            testTask.cancel(true);
        }
    }

    private void download() {
        downloading = true;
        button1.setText("停止诊断");
        clean();
        if (testTask != null) {
            testTask.cancel(true);
        }
        uploadData = null;
        testTask = new TestTask(this);
        testTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URLS[index], TYPES[index]);
    }

    private void done(UploadData data) {
        downloading = false;
        button1.setText("开始诊断");
        uploadData = data;
        button2.setEnabled(true);
    }

    private void cancel() {
        if (testTask != null) {
            testTask.cancel(true);
            testTask = null;
        }
    }

    private static class TestTask extends WeakAsyncTask<String, String, UploadData, MainActivity> {

        TestTask(MainActivity activity) {
            super(activity);
        }

        @Override
        protected void onPreExecute(MainActivity activity) {
            if (activity != null) {
                activity.print("*********诊断开始********");
            }
        }

        @Override
        protected UploadData doInBackground(MainActivity activity, String... params) {
            UploadData data = new UploadData();
            String url = params[0];
            boolean m3u8 = Boolean.parseBoolean(params[1]);
            checkNet(activity, data, url);
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .followRedirects(false)
                    .build();
            checkDownload(okHttpClient, data, url, m3u8);
            if (m3u8) {
                try {
                    File file = getFile(url);
                    if (file.exists()) {
                        publishProgress(">>>>>开始解析文件");
                        InputStream inputStream = new FileInputStream(file);
                        PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8, ParsingMode.LENIENT);
                        Playlist playlist = parser.parse();
                        if (playlist != null && playlist.hasMediaPlaylist() && playlist.getMediaPlaylist().hasTracks()) {
                            HttpUrl httpUrl = HttpUrl.parse(url);
                            for (int i = 0; i < 3; i++) {
                                TrackData trackData = playlist.getMediaPlaylist().getTracks().get(i);
                                publishProgress("\n>>>>>解析ts文件：" + i);
                                String tsUrl = httpUrl.scheme() + "://" + httpUrl.host() + trackData.getUri();
                                checkDownload(okHttpClient, data, tsUrl, false);
                            }
                        }
                        publishProgress("<<<<<解析文件完成");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    publishProgress("<<<<<解析文件出错：" + e);
                }
            }
            return data;
        }

        private File getFile(String url) {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    MD5Util.md5(url));
        }

        private void checkDownload(OkHttpClient okHttpClient, UploadData data, String url, boolean m3u8) {
            UploadData.Request r = new UploadData.Request();
            data.request_info.add(r);
            r.requesturl = url;
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36")
                    .build();
            try {
                publishProgress("\n>>>>>请求地址:\n" + url);
                publishProgress("请求头:");
                r.requestheader = request.headers().toString();
                publishProgress(r.requestheader);
                Response response = okHttpClient.newCall(request).execute();
                publishProgress("响应头:");
                r.responseheader = response.headers().toString();
                publishProgress(r.responseheader);
                r.responsebody = response.toString();
                if (response.isRedirect()) {
                    publishProgress(">>>>>重定向>>>>>");
                    String redirect_url = response.header("Location");
                    checkDownload(okHttpClient, data, redirect_url, false);
                    return;
                } else if (!response.isSuccessful()) {
                    publishProgress("错误，code=" + response.code());
                    return;
                }
                InputStream is = response.body().byteStream();
                byte[] buffer = new byte[128];
                int count;
                long current = 0;
                long total = response.body().contentLength();
                publishProgress(">>>>>开始请求数据");
                File file = getFile(url);
                FileOutputStream os = new FileOutputStream(file);
                while (!isCancelled() && (count = is.read(buffer)) > 0 && (m3u8 || current < 1024)) {
                    os.write(buffer, 0, count);
                    current += count;
                }
                publishProgress("<<<<<成功请求数据(" + current + "/" + total + ")");
                if (response.body() != null) {
                    response.body().close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("异常:" + e.toString());
            }
        }

        private void checkNet(Context c, UploadData data, String url) {
            boolean _isNetConnected;
            if (Utils.isNetworkConnected(c)) {
                _isNetConnected = true;
                publishProgress("当前是否联网:" + "是");
            } else {
                _isNetConnected = false;
                publishProgress("当前是否联网:" + "否");
            }
            String _netType = Utils.getNetWorkType(c);
            publishProgress("当前网络类型:" + _netType);
            if (_isNetConnected) {
                if (Utils.NETWORKTYPE_WIFI.equals(_netType)) { // wifi：获取本地ip和网关，其他类型：只获取ip
                    data.clientip = Utils.getLocalIpByWifi(c);
                } else {
                    data.clientip = Utils.getLocalIpBy3G();
                }
            } else {
                data.clientip = "127.0.0.1";
            }
            publishProgress("本地IP:" + data.clientip);
            data.carrierName = Utils.getMobileOperator(c);
            publishProgress("运营商:" + data.carrierName);
            String _dns1, _dns2;
            if (_isNetConnected) {
                _dns1 = Utils.getLocalDns("dns1");
                if (!TextUtils.isEmpty(_dns1)) {
                    data.clientdns.add(_dns1);
                }
                _dns2 = Utils.getLocalDns("dns2");
                if (!TextUtils.isEmpty(_dns2)) {
                    data.clientdns.add(_dns2);
                }
            } else {
                _dns1 = _dns2 = "0.0.0.0";
                data.clientdns.add(_dns1);
            }
            publishProgress("本地DNS:" + _dns1 + "," + _dns2);
            if (_isNetConnected) {
                HttpUrl httpUrl = HttpUrl.parse(url);
                if (httpUrl != null)
                    data.dnsparser = parseDomain(httpUrl.host());
            }
        }

        private List<String> parseDomain(String _dormain) {
            int len;
            String ipString = "";
            List<String> _remoteIpList = new ArrayList<>();
            Map<String, Object> map = Utils.getDomainIp(_dormain);
            String useTime = (String) map.get("useTime");
            InetAddress[] _remoteInet = (InetAddress[]) map.get("remoteInet");
            String timeShow;
            if (Integer.parseInt(useTime) > 5000) {// 如果大于1000ms，则换用s来显示
                timeShow = " (" + Integer.parseInt(useTime) / 1000 + "s)";
            } else {
                timeShow = " (" + useTime + "ms)";
            }
            if (_remoteInet != null) {// 解析正确
                len = _remoteInet.length;
                for (int i = 0; i < len; i++) {
                    _remoteIpList.add(_remoteInet[i].getHostAddress());
                    ipString += _remoteInet[i].getHostAddress() + ",";
                }
                ipString = ipString.substring(0, ipString.length() - 1);
                publishProgress("DNS解析结果:" + ipString + timeShow);
            } else {// 解析不到，判断第一次解析耗时，如果大于10s进行第二次解析
                if (Integer.parseInt(useTime) > 10000) {
                    map = Utils.getDomainIp(_dormain);
                    useTime = (String) map.get("useTime");
                    _remoteInet = (InetAddress[]) map.get("remoteInet");
                    if (Integer.parseInt(useTime) > 5000) {// 如果大于1000ms，则换用s来显示
                        timeShow = " (" + Integer.parseInt(useTime) / 1000 + "s)";
                    } else {
                        timeShow = " (" + useTime + "ms)";
                    }
                    if (_remoteInet != null) {
                        len = _remoteInet.length;
                        for (int i = 0; i < len; i++) {
                            _remoteIpList.add(_remoteInet[i].getHostAddress());
                            ipString += _remoteInet[i].getHostAddress() + ",";
                        }
                        ipString = ipString.substring(0, ipString.length() - 1);
                        publishProgress("DNS解析结果:" + ipString + timeShow);
                    } else {
                        publishProgress("DNS解析结果:" + "解析失败" + timeShow);
                    }
                } else {
                    publishProgress("DNS解析结果:" + "解析失败" + timeShow);
                }
            }
            return _remoteIpList;
        }

        @Override
        protected void onProgressUpdate(MainActivity activity, String... values) {
            if (activity != null) {
                activity.print(values[0]);
            }
        }

        @Override
        protected void onCancelled(MainActivity activity, UploadData data) {
            if (activity != null) {
                activity.print("*********终止********");
                activity.done(data);
            }
        }

        @Override
        protected void onPostExecute(MainActivity activity, UploadData data) {
            if (activity != null) {
                activity.print("*********诊断结束********");
                activity.done(data);
            }
        }
    }

    private void print(String text) {
        textView.append('\n' + text);
    }

    private void clean() {
        textView.setText("");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void upload(View view) {
        if (uploadData == null) {
            toast("没有可上报的数据");
            return;
        }
        button2.setText("正在上传");
        button2.setEnabled(false);
        button1.setEnabled(false);
        new UploadTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uploadData);
    }

    private void uploadDone(boolean success) {
        button2.setText("提交报告");
        button2.setEnabled(true);
        button1.setEnabled(true);
        toast(success ? "提交成功" : "提交失败");
    }

    private static class UploadTask extends WeakAsyncTask<UploadData, String, Boolean, MainActivity> {

        UploadTask(MainActivity activity) {
            super(activity);
        }

        @Override
        protected Boolean doInBackground(MainActivity activity, UploadData... params) {
            UploadData uploadData = params[0];
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            String data = new Gson().toJson(uploadData);
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), data);
            Request request = new Request.Builder()
                    .url("http://w.namibox.com/api/app/checkdownload")
                    .header("User-Agent", "okhttp3.0")
                    .post(body)
                    .build();
            try {
                Response response = okHttpClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    String s = response.body().string();
                    Result result = Utils.parseJsonString(s, Result.class);
                    if (result != null && result.errcode == 0) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(MainActivity activity, Boolean result) {
            if (activity != null) {
                activity.uploadDone(result);
            }
        }
    }
}
