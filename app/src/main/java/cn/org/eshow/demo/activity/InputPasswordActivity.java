package cn.org.eshow.demo.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cn.org.eshow.demo.R;
import cn.org.eshow.demo.bean.Enum_CodeType;
import cn.org.eshow.demo.bean.Switch_CodeType;
import cn.org.eshow.demo.common.CommonActivity;
import cn.org.eshow.demo.common.Global;
import cn.org.eshow.demo.common.SharedPrefUtil;
import cn.org.eshow.demo.network.ESResponseListener;
import cn.org.eshow.demo.network.NetworkInterface;
import cn.org.eshow.demo.view.LoginAutoCompleteEdit;
import cn.org.eshow.framwork.fragment.AbProgressDialogFragment;
import cn.org.eshow.framwork.util.AbDialogUtil;
import cn.org.eshow.framwork.util.AbStrUtil;
import cn.org.eshow.framwork.util.AbToastUtil;
import cn.org.eshow.framwork.util.AbViewUtil;

/**
 * 找回密码的修改密码页面
 * Created by daikting on 16/2/24.
 */
@EActivity(R.layout.activity_findpassword)
public class InputPasswordActivity extends CommonActivity {
    private Context mContext = InputPasswordActivity.this;
    public static final String INTENT_TEL = "UserName";
    @ViewById(R.id.rlBack)
    RelativeLayout mRlMenu;
    @ViewById(R.id.material_back_button)
    MaterialMenuView mMaterialBackButton;
    @ViewById(R.id.tvTitle)
    TextView mTvTitle;

    @ViewById(R.id.tvReminder)
    TextView mTvReminder;
    @ViewById(R.id.etTel)
    LoginAutoCompleteEdit etCode;
    @ViewById(R.id.etPassword)
    LoginAutoCompleteEdit mEtPassword;
    @ViewById(R.id.llAgainPassword)
    LinearLayout llAgainPassword;
    @ViewById(R.id.etRePassword)
    LoginAutoCompleteEdit etRePassword;
    @ViewById(R.id.btnVoice)
    Button btnVoice;
    @ViewById(R.id.btnSubmit)
    Button btnSubmit;
    //页面跳转的intent标识
    private Enum_CodeType intentExtra = Enum_CodeType.REGISTER;

    private String userName = "";
    AbProgressDialogFragment progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @AfterViews
    void init() {
        AbViewUtil.scaleContentView((LinearLayout) findViewById(R.id.llParent));

        intentExtra = (Enum_CodeType) getIntent().getSerializableExtra(InputTelActivity_.INTENT_ISREGISTER);
        userName = getIntent().getStringExtra(INTENT_TEL);
        new Switch_CodeType(intentExtra) {
            @Override
            public void onRegister() {
                InputPasswordActivity.this.setTitle("注册");
                llAgainPassword.setVisibility(View.GONE);
            }

            @Override
            public void onFindPassword() {
                InputPasswordActivity.this.setTitle("找回密码");
                llAgainPassword.setVisibility(View.VISIBLE);
                mEtPassword.setHint("请输入新密码");
                etRePassword.setHint("请再次输入新密码");
            }

            @Override
            public void onRePassword() {
                InputPasswordActivity.this.setTitle("重置密码");
                llAgainPassword.setVisibility(View.VISIBLE);
                mEtPassword.setHint("请输入新密码");
                etRePassword.setHint("请再次输入新密码");
            }
        };
        mTvTitle.setText(getTitle());
        mMaterialBackButton.setState(MaterialMenuDrawable.IconState.ARROW);

        String tempTel = userName;
        if (tempTel.length() == 11) {
            String telHead = tempTel.substring(0, 3);
            String telEnd = tempTel.substring(7, 11);
            tempTel = telHead + "****" + telEnd;
        }
        mTvReminder.setText("已将短信发送至您的手机  " + tempTel);

        checkVoiceState();

        mEtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mEtPassword.getText().toString().length() >= 6) {
                    btnSubmit.setClickable(true);
                    btnSubmit.setBackgroundResource(R.drawable.btn_long);
                } else {
                    btnSubmit.setClickable(false);
                    btnSubmit.setBackgroundResource(R.drawable.btn_long_pressed);
                }
            }
        });

    }

    @Click(R.id.rlBack)
    void onBack() {
        finish();
    }

    @Click(R.id.btnVoice)
    void onVoice() {
        ESResponseListener responseListener = new ESResponseListener(mContext) {
            @Override
            public void onBQSucess(String esMsg, JSONObject resultJson) {
                AbToastUtil.showToast(mContext, "请求语音播报验证码成功，请注意来电！");
                SharedPrefUtil.setSendCodeTime(mContext);
                checkVoiceState();
            }

            @Override
            public void onBQNoData() {

            }

            @Override
            public void onBQNotify(String bqMsg) {
                AbToastUtil.showToast(mContext, bqMsg);
            }

            @Override
            public void onStart() {
                progressDialog = AbDialogUtil.showProgressDialog(mContext, Global.LOADING_PROGRESSBAR_ID, "请求数据中...");
            }

            @Override
            public void onFinish() {
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode, String content, Throwable error) {
                progressDialog.dismiss();
                AbToastUtil.showToast(mContext, "请求失败，错误码：" + statusCode);
            }
        };
        NetworkInterface.voice(mContext, userName, intentExtra, responseListener);
    }

    @Click(R.id.btnSubmit)
    void onSubmit() {
        final String code = etCode.getText().toString();
        final String password = mEtPassword.getText().toString();

        if (AbStrUtil.isEmpty(code)) {
            AbToastUtil.showToast(mContext, "请输入验证码！");
            return;
        }

        if (AbStrUtil.isEmpty(password)) {
            AbToastUtil.showToast(mContext, "请输入密码！");
            return;
        }

        if (AbStrUtil.strLength(password) > 20) {
            AbToastUtil.showToast(mContext, "密码长度过长！");
            return;
        }

        if(llAgainPassword.isShown()){
            String againPassword = etRePassword.getText().toString();
            if(!againPassword.equals(password)){
                AbToastUtil.showToast(mContext, "两次输入的密码不一样！");
                return;
            }
        }
        final ESResponseListener responseListener = new ESResponseListener(mContext) {
            @Override
            public void onBQSucess(String esMsg, JSONObject resultJson) {
                new Switch_CodeType(intentExtra) {
                    @Override
                    public void onRegister() {
                        AbToastUtil.showToast(mContext, "注册成功！");
                        LoginActivity_.intent(mContext).extra("userName", userName).start();
                        finish();
                    }

                    @Override
                    public void onFindPassword() {
                        AbToastUtil.showToast(mContext, "密码修改成功！");
                        finish();
                    }

                    @Override
                    public void onRePassword() {
                        Intent intent = new Intent(Global.EShow_Broadcast_Action.ACTION_EXIT_REPASSWORD);
                        sendBroadcast(intent);
                        AbToastUtil.showToast(mContext, "密码修改成功！");
                        finish();
                    }
                };
            }

            @Override
            public void onBQNoData() {

            }

            @Override
            public void onBQNotify(String bqMsg) {
                AbToastUtil.showToast(mContext, bqMsg);
            }

            @Override
            public void onStart() {
                progressDialog = AbDialogUtil.showProgressDialog(mContext, Global.LOADING_PROGRESSBAR_ID, "请求数据中...");
            }

            @Override
            public void onFinish() {
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode, String content, Throwable error) {
                progressDialog.dismiss();
                AbToastUtil.showToast(mContext, "请求失败，错误码：" + statusCode);
            }
        };

        new Switch_CodeType(intentExtra) {
            @Override
            public void onRegister() {
                NetworkInterface.regist(mContext, userName, code, password, responseListener);
            }

            @Override
            public void onFindPassword() {
                NetworkInterface.rePassword(mContext, userName, code, password, responseListener);
            }

            @Override
            public void onRePassword() {
                NetworkInterface.rePassword(mContext, userName, code, password, responseListener);
            }
        };


    }

    //倒计时
    private Timer countDownTimer ;
    //倒计时
    private TimerTask countDownTask;
    //倒计时
    private Handler countDownHandler = new Handler() {
        public void handleMessage(Message msg) {
            long time = (Long) msg.obj;

            switch (msg.what) {
                case 0:
                    btnVoice.setBackgroundResource(R.drawable.btn_long_pressed);
                    btnVoice.setText(time + "S");
                    btnVoice.setClickable(false);
                    break;
                case 1:
                    btnVoice.setBackgroundResource(R.drawable.btn_long);
                    btnVoice.setText("语音播报");
                    btnVoice.setClickable(true);

                    if (countDownTask != null) {
                        countDownTask.cancel();
                        countDownTask = null;
                    }

                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        countDownTimer = null;
                    }
                    break;
            }
        }
    };

    /**
     * 检测语音播报按钮状态
     */
    private void checkVoiceState() {
        long sendTime = SharedPrefUtil.getSendCodeTime(mContext);
        long currentTime = System.currentTimeMillis();
        long time = (Global.SEND_CODE_CYCLE - (currentTime - sendTime)) / 1000;
        if (time > 0) {//还没有达到再次发送短信验证码的时间限制
            btnVoice.setClickable(false);
            btnVoice.setBackgroundResource(R.drawable.btn_long_pressed);
            btnVoice.setText(time + "S");
            countDownTimer = new Timer();
            countDownTask= new TimerTask() {
                @Override
                public void run() {
                    long sendTime = SharedPrefUtil.getSendCodeTime(mContext);
                    long currentTime = System.currentTimeMillis();
                    long time = (Global.SEND_CODE_CYCLE - (currentTime - sendTime)) / 1000;

                    Message msg = new Message();
                    if (time > 0) {//还没有达到再次发送短信验证码的时间限制
                        msg.what = 0;
                    } else {
                        msg.what = 1;
                    }
                    msg.obj = time;
                    countDownHandler.sendMessage(msg);
                }
            };
            countDownTimer.schedule(countDownTask, 0, 1000);//开始倒计时
        } else {
            btnVoice.setClickable(true);
            btnVoice.setBackgroundResource(R.drawable.btn_long);
            btnVoice.setText("语音播报");
        }
    }
}
