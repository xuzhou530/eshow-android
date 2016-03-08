package com.bangqu.eshow.demo.network;

import android.content.Context;

import com.bangqu.eshow.demo.bean.Enum_VoiceType;
import com.bangqu.eshow.http.ESRequestParams;

/**
 * 网络接口
 * Created by daikting on 15/10/12.
 */
public class NetworkInterface {
    /**
     * 登录
     *
     * @param context
     * @param userName
     * @param password
     * @param responseListener
     */
    public static void login(Context context, String userName, String password, ESResponseListener responseListener) {
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("user.username", userName);
        abRequestParams.put("user.password", password);
        new ESHttpUtil(context).post("user/login", abRequestParams, responseListener);
    }

    /**
     * 注册
     * @param context
     * @param userName
     * @param responseListener
     */
    public static void register(Context context,String userName,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("user.username", userName);
        new ESHttpUtil(context).post("user/check", abRequestParams, responseListener);
    }

    /**
     * 语音播报
     * @param context
     * @param mobile
     * @param enum_voiceType
     * @param responseListener
     */
    public static void voice(Context context,String mobile,Enum_VoiceType enum_voiceType,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("mobile", mobile);
        abRequestParams.put("type",enum_voiceType.toString());
        new ESHttpUtil(context).post("code/voice", abRequestParams, responseListener);
    }
    /**
     * 获取七牛token
     *
     * @param context
     * @param responseListener
     */
    public static void getQiniuToken(Context context, ESResponseListener responseListener) {
        ESRequestParams abRequestParams = new ESRequestParams();
        new ESHttpUtil(context).post("qiniu/token", abRequestParams, responseListener);
    }

    /**
     * 获取七牛Key
     */
    public static void getQiniuKey(Context context, ESResponseListener responseListener) {
        ESRequestParams abRequestParams = new ESRequestParams();
        new ESHttpUtil(context).post("qiniu/key", abRequestParams, responseListener);
    }

}
