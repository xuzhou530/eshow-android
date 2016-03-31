package com.bangqu.eshow.demo.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.bangqu.eshow.demo.R;
import com.bangqu.eshow.demo.common.CommonActivity;
import com.bangqu.eshow.demo.common.Global;
import com.bangqu.eshow.util.ESLogUtil;
import com.bangqu.eshow.util.ESViewUtil;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 网页支付页面
 *
 * @author
 */
public class PayWebViewActivity extends CommonActivity implements OnClickListener {
    public static final String INTENT_URL_TAG = "intent.url";
    private RelativeLayout rlBack;
    private MaterialMenuView mMaterialBackButton;
    private TextView tvTitle;

    private WebView webView;
    private String title = "";
    private Context mContext;
    ProgressBar pbCash;
    private String webUrl = "";
    private static final String tag = "PayWebViewActivity";

    /**
     * 开发者需要填一个服务端 CHARGE_URL 该 CHARGE_URL 是用来请求支付需要的 charge 。务必确保，CHARGE_URL
     * 能返回 json 格式的 charge 对象。 服务端生成 charge 的方式可以参考 ping++ 官方文档，地址
     * https://pingxx.com/guidance/server/import
     * <p/>
     * 【 http://218.244.151.190/demo/charge 】是 ping++ 为了方便开发者体验 sdk 而提供的一个临时 url
     * 。 改 url 仅能调用【模拟支付控件】，开发者需要改为自己服务端的 url 。
     */
    private String charge_url;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view_location_layout);
        ESViewUtil.scaleContentView((LinearLayout) findViewById(R.id.llParent));
        mContext = this;
        webUrl = getIntent().getStringExtra(PayWebViewActivity.INTENT_URL_TAG);
        title = "支付订单";
        findView();
        fillDate();

    }

    private void findView() {

        rlBack = (RelativeLayout) findViewById(R.id.rlBack);
        rlBack.setOnClickListener(this);
        mMaterialBackButton = (MaterialMenuView) findViewById(R.id.material_back_button);
        mMaterialBackButton.setState(MaterialMenuDrawable.IconState.ARROW);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvTitle.setText(title);

        webView = (WebView) findViewById(R.id.wvtBangQu);
        pbCash = (ProgressBar) findViewById(R.id.pbCash);
        pbCash.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void fillDate() {
        webView.loadUrl(webUrl);
        webView.getSettings().setJavaScriptEnabled(true);// 开启jacascript
//        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);// 支持通过JS打开新窗口
//        webView.getSettings().setSupportZoom(true);
//        webView.getSettings().setAllowFileAccess(true);
//        webView.getSettings().setLoadsImagesAutomatically(true);// 支持自动加载图片
//
//        webView.requestFocusFromTouch();
//        webView.getSettings().setBuiltInZoomControls(true);// 设置支持缩放
//        webView.getSettings().setDefaultZoom(ZoomDensity.FAR);// 屏幕自适应网页，如果没有这个在低分辨率手机上显示会异常
        ESLogUtil.d(mContext, "webUrl:" + webUrl);
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        pbCash.setVisibility(View.VISIBLE);
        /**
         * 设置 js 交互类
         */
        webView.addJavascriptInterface(new JSInterface(), "PINGPP_ANDROID_SDK");
    }

    public class WebChromeClient extends android.webkit.WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            pbCash.setProgress(newProgress);
            super.onProgressChanged(view, newProgress);
        }
    }

    // 内部类
    public class MyWebViewClient extends WebViewClient {

        // 如果页面中链接，如果希望点击链接继续在当前browser中响应，
        // 而不是新开Android的系统browser中响应该链接，必须覆盖 webview的WebViewClient对象。

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {// 拦截无资源的请求

            if (url != null && url.startsWith("uppay://")) {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
            return false;
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            pbCash.setVisibility(View.VISIBLE);
        }

        public void onPageFinished(WebView view, String url) {
            pbCash.setVisibility(View.GONE);
        }

        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            pbCash.setVisibility(View.GONE);

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlBack:
                finish();
                break;
            default:
                break;
        }
    }

    /**
     * JS回调java
     */
    class JSInterface {
        @JavascriptInterface
        // JS代码调用接口，调用方法：PINGPP_ANDROID_SDK.callPay(channel, amount);
        public void callPay(String url) {
            url = url.substring(1, url.length());
            charge_url = Global.SERVER_URL + url;
            ESLogUtil.d(mContext, String.valueOf(url));
            new PaymentTask().execute(new PaymentRequest(url));

        }
    }


    /**
     * http 请求
     *
     * @param url
     * @param json
     * @return
     * @throws IOException
     */
    private static String postJson(String url, String json) throws IOException {
        MediaType type = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(type, json);
        Request request = new Request.Builder().url(url).post(body).build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    /**
     * 请求支付凭据
     *
     * @author sunkai
     */
    class PaymentTask extends AsyncTask<PaymentRequest, Void, String> {
        @Override
        protected String doInBackground(PaymentRequest... pr) {
            PaymentRequest paymentRequest = pr[0];
            String data = null;
            String json = new Gson().toJson(paymentRequest);
            try {
                // 向支付请求文件（例如 pay.php）请求数据
                ESLogUtil.d(mContext, "charge_url:" + charge_url);
                data = postJson(charge_url, json);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            ESLogUtil.d(mContext, data);
            Intent intent = new Intent();
            String packageName = getPackageName();
            ComponentName componentName = new ComponentName(packageName, packageName + ".wxapi.WXPayEntryActivity");
            intent.setComponent(componentName);
            intent.putExtra(com.pingplusplus.android.PaymentActivity.EXTRA_CHARGE, data);
            startActivityForResult(intent, 1);
        }
    }

    /**
     * 请求 Charge 的参数类
     *
     * @author sunkai
     */
    class PaymentRequest {
        String channel;
        int amount;

        public PaymentRequest(String url) {
            Map<String, Object> map = urlSplit(url);
            this.channel = map.get("channel").toString();
            this.amount = Integer.valueOf(map.get("amount").toString());
        }
    }

    public Map<String, Object> urlSplit(String data) {
        StringBuffer strbuf = new StringBuffer();
        StringBuffer strbuf2 = new StringBuffer();
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < data.length(); i++) {

            if (data.substring(i, i + 1).equals("=")) {

                for (int n = i + 1; n < data.length(); n++) {
                    if (data.substring(n, n + 1).equals("&") || n == data.length() - 1) {
                        map.put(strbuf.toString(), strbuf2);
                        strbuf = new StringBuffer("");
                        strbuf2 = new StringBuffer("");
                        i = n;
                        break;
                    }
                    strbuf2.append(data.substring(n, n + 1));
                }
                continue;
            }
            strbuf.append(data.substring(i, i + 1));
        }

        return map;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 支付页面返回处理
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getExtras().getString("pay_result");

				/*
                 * 处理返回值 "success" - 支付成功 "fail" - 支付失败 "cancel" - 用户取消
				 * "invalid" - 未安装支付控件
				 */
                if (result.equalsIgnoreCase("success")) {
                    ESLogUtil.d(mContext, "支付成功跳转");
                    String sucessUrl = "http://api.eshow.org.cn/pingpay/pay.jsp";
                    webView.loadUrl(sucessUrl);
                }

                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "User canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}