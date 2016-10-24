package ryancheng.okhttp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Create time: 2016/10/14.
 */

public class DownloadActivity extends AppCompatActivity {
    EditText editText1;
    EditText editText2;
    EditText editText3;
    Button button;
    Button button2;
    TextView textView;
    private Task task;
    static final String ROOT = "http://u.namibox.com/static/d/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        editText1 = (EditText) findViewById(R.id.edittext1);
        editText2 = (EditText) findViewById(R.id.edittext2);
        editText3 = (EditText) findViewById(R.id.edittext3);
        button = (Button) findViewById(R.id.btn);
        button2 = (Button) findViewById(R.id.btn2);
        textView = (TextView) findViewById(R.id.text);
        editText1.setText("tape1a");
        editText2.setText("tape1a_001002");
        editText3.setText("20160521161038");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unzip();
            }
        });
    }

    private void unzip() {
        button2.setEnabled(false);
        new ZipTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class ZipTask extends WeakAsyncTask<Void, String, Void, DownloadActivity> {

        ZipTask(DownloadActivity downloadActivity) {
            super(downloadActivity);
        }

        @Override
        protected Void doInBackground(DownloadActivity downloadActivity, Void... params) {
            File file = new File(Environment.getExternalStorageDirectory(), "tape1a_000001.zip.20161018091541.20161018145911.zip");
            File dir = new File(Environment.getExternalStorageDirectory(), "unzip_test");
            try {
                publishProgress("开始解压");
                unzip2(file, dir, false);
                publishProgress("完成解压");
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("错误：" + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(DownloadActivity downloadActivity, String... values) {
            downloadActivity.print(values[0]);
        }

        @Override
        protected void onPostExecute(DownloadActivity downloadActivity, Void aVoid) {
            downloadActivity.button2.setEnabled(true);
        }
    }

    private void download() {
        textView.setText("");
        button.setEnabled(false);
        task = new Task(this);
        String grade = editText1.getText().toString();
        String bookid = editText2.getText().toString();
        String time = editText3.getText().toString();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, grade, bookid, time);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (task != null) {
            task.cancel(true);
        }
    }

    private void print(String text) {
        textView.append('\n' + text);
    }

    private static class Task extends WeakAsyncTask<String, String, String, DownloadActivity> {

        Task(DownloadActivity downloadActivity) {
            super(downloadActivity);
        }

        private File getDir(String bookid) {
            File dir = new File(Environment.getExternalStorageDirectory(), "namibox/main/books/" + bookid);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir;
        }

        private File getFile(String bookid) {
            return new File(getDir(bookid), "all");
        }

        @Override
        protected String doInBackground(DownloadActivity downloadActivity, String... params) {
            String bookid = params[1];
            String grade = params[0];
            String time = params[2];
            String url = ROOT + grade + "/" + bookid + ".zip?" + time;
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36 Namibox/Android/3.0.3")
                    .build();
            try {
                Response response = okHttpClient.newCall(request).execute();
                InputStream is = response.body().byteStream();
                byte[] buffer = new byte[8192];
                int count;
                long current = 0;
                long total = response.body().contentLength();
                publishProgress("开始下载:" + url);
                File file = getFile(bookid);
                FileOutputStream os = new FileOutputStream(file);
                int percent = 0;
                while (!isCancelled() && (count = is.read(buffer)) > 0) {
                    os.write(buffer, 0, count);
                    current += count;
                    int newpercent = (int) (100 * current / total);
                    if (percent != newpercent) {
                        percent = newpercent;
                        publishProgress("下载中(" + current + "/" + total + ")" + percent + "%");
                    }
                }
                publishProgress("开始解压");
                unzip2(file, file.getParentFile(), true);
                publishProgress("完成解压");
                if (response.body() != null) {
                    response.body().close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("出错：" + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(DownloadActivity downloadActivity, String... values) {
            downloadActivity.print(values[0]);
        }

        @Override
        protected void onPostExecute(DownloadActivity downloadActivity, String s) {
            downloadActivity.button.setEnabled(true);
        }
    }
    public static void unzip2(File zip, File extractTo, boolean md5Name) throws IOException {
        ZipFile archive = new ZipFile(zip);
        Enumeration<? extends ZipEntry> e = archive.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            File file = new File(extractTo, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                File newFile = md5Name ? new File(file.getParentFile(), MD5Util.md5(file.getName())) : file;
                InputStream in = null;
                BufferedOutputStream out = null;
                Log.e("unzip", "file=" +newFile);
                try {
                    in = archive.getInputStream(entry);
                    out = new BufferedOutputStream(new FileOutputStream(newFile));
                    inputStreamToOutputStream(in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    public static void inputStreamToOutputStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int count;
        while ((count = is.read(buffer)) > 0) {
            os.write(buffer, 0, count);
        }
    }
}
