package ryancheng.okhttp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Create time: 2016/10/24.
 */

public class AttackActivity extends AppCompatActivity {
    TextView textView;
    Button button1;
    static String[] email_list = {
            "@163.com",
            "@gmail.com",
            "@hotmail.com",
            "@126.com",
            "@qq.com",
            "@sina.com",
            "@tom.com",
            "@yahoo.com.cn",
            "@sohu.com"
    };
    //static String root = "http://www.applexe.com";
    //static String origin_url = "http://www.applexe.com/ios";
    //static String host = "www.applexe.com";
    static String root = "http://apple-china-id.top";
    static String origin_url = "http://apple-china-id.top";
    static String host = "apple-china-id.top";

    static String token_url = root + "/Home/Token/";
    static String post_url = root + "/Home/Save";
    static String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
    boolean started;
    TestTask[] testTask = new TestTask[1];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack);
        textView = (TextView) findViewById(R.id.text);
        button1 = (Button) findViewById(R.id.btn);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (started) {
                    stop();
                } else {
                    start();
                }
            }
        });
        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clean();
            }
        });
    }

    void start() {
        started = true;
        button1.setText("停止");
        clean();
        for (int i = 0; i < testTask.length; i++) {
            testTask[i] = new TestTask(this, i);
            testTask[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    void stop() {
        for (TestTask t : testTask) {
            if (t != null) {
                t.cancel(true);
            }
        }
    }

    private void print(int id, String text) {
        textView.append("[" + id + "]" + text + '\n');
    }

    private void clean() {
        textView.setText("");
    }

    private void done() {
        started = false;
        button1.setText("开始");
    }

    private static class TestTask extends WeakAsyncTask<Void, String, Void, AttackActivity> {
        OkHttpClient okHttpClient;
        PersistentCookieStore cookieStore;
        String refer;
        int id;
        int total;

        TestTask(AttackActivity activity, int id) {
            super(activity);
            this.id = id;
        }

        @Override
        protected void onPreExecute(AttackActivity activity) {
            if (activity != null) {
                activity.print(id, "*********开始********");
            }
        }

        @Override
        protected Void doInBackground(AttackActivity activity, Void... params) {
            try {
                Thread.sleep(id * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cookieStore = new PersistentCookieStore(activity);
            okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(5, TimeUnit.SECONDS)
                    .followRedirects(false)
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            for (Cookie cookie : cookies) {
                                cookieStore.add(url, cookie);
                            }
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            return cookieStore.get(url);
                        }
                    })
                    .build();
            get(origin_url);
            Random random = new Random();
            //while (!isCancelled()) {
                publishProgress(">>>>>>>>>>>>写入第" + total + "条数据");
                String tokenString = get(token_url);
                String u = randomString(random, 10);
                String p = randomString(random, 10) + email_list[random.nextInt(email_list.length)];
                if (tokenString != null) {
                    Pattern pattern = Pattern.compile("value=\"(.*?)\"");
                    Matcher matcher = pattern.matcher(tokenString);
                    if (matcher.find()) {
                        String token = matcher.group(1);
                        RequestBody formBody = new FormBody.Builder()
                                .add("u", u)
                                .add("p", p)
                                .add("hiddenToken", token)
                                .build();
                        publishProgress("u: " + u + ", p: " + p + ", token: " + token);
                        String result = post(post_url, formBody);
                        publishProgress(result);
                    }
                }
                total++;
            //}
            return null;
        }

        static String randomString(Random random, int len) {
            //将所有的大小写字母和0-9数字存入字符串中
            String str = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ0123456789";
            StringBuilder stringBuffer = new StringBuilder();
            for (int j = 0; j < len; j++) {
                //先随机生成初始定义的字符串 str 的某个索引，以获取相应的字符
                int index = random.nextInt(str.length());
                char c = str.charAt(index);
                stringBuffer.append(c);
            }
            return stringBuffer.toString();
        }

        private String get(String url) {
            Request.Builder rb = new Request.Builder();
            rb.url(url);
            rb.header("User-Agent", ua);
            rb.header("Host", host);
            rb.header("Origin", root);
            rb.header("X-Requested-With", "XMLHttpRequest");
            if (!TextUtils.isEmpty(refer))
                rb.header("Referer", refer);
            Request request = rb.build();
            try {
//                publishProgress("\n>>>>>请求地址:\n" + url);
//                publishProgress("请求头:");
//                publishProgress(request.headers().toString());
                Response response = okHttpClient.newCall(request).execute();
//                publishProgress("响应头:");
//                publishProgress(response.headers().toString());
                if (response.isRedirect()) {
                    publishProgress(">>>>>重定向>>>>>");
                    String redirect_url = response.header("Location");
                    refer = root + redirect_url;
                    return get(refer);
                }
                String r = response.body().string();
                if (response.body() != null) {
                    response.body().close();
                }
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                //publishProgress("异常:" + e.toString());
            }
            return null;
        }

        private String post(String url, RequestBody body) {
            Request.Builder rb = new Request.Builder();
            rb.url(url);
            rb.post(body);
            rb.header("User-Agent", ua);
            rb.header("Host", host);
            rb.header("Origin", root);
            rb.header("X-Requested-With", "XMLHttpRequest");
            if (!TextUtils.isEmpty(refer))
                rb.header("Referer", refer);
            Request request = rb.build();
            try {
//                publishProgress("\n>>>>>请求地址:\n" + url);
//                publishProgress("请求头:");
//                publishProgress(request.headers().toString());
                Response response = okHttpClient.newCall(request).execute();
//                publishProgress("响应头:");
//                publishProgress(response.headers().toString());
                String r = response.body().string();
                if (response.body() != null) {
                    response.body().close();
                }
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("异常:" + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(AttackActivity activity, String... values) {
            if (activity != null) {
                activity.print(id, values[0]);
            }
        }

        @Override
        protected void onCancelled(AttackActivity activity, Void data) {
            if (activity != null) {
                activity.print(id, "*********终止,共写入：" + total + "条数据********");
                activity.done();
            }
        }

        @Override
        protected void onPostExecute(AttackActivity activity, Void data) {
            if (activity != null) {
                activity.print(id, "*********结束,共写入：" + total + "条数据********");
                activity.done();
            }
        }
    }
}
