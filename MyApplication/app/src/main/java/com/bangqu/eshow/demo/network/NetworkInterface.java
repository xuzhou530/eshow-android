package com.bangqu.eshow.demo.network;

import android.content.Context;

import com.bangqu.eshow.demo.bean.Enum_CodeType;
import com.bangqu.eshow.demo.bean.Enum_ThirdType;
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
        new MyHttpUtil(context).post("user/login", abRequestParams, responseListener);
    }

    /**
     * 注册、找回密码时发送短信验证码
     * @param context
     * @param userName
     * @param responseListener
     */
    public static void sendCode(Context context,String userName,Enum_CodeType type,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("user.username", userName);
        abRequestParams.put("type",type.toString());
        new MyHttpUtil(context).post("user/check", abRequestParams, responseListener);
    }

    /**
     * 注册
     * @param context
     * @param userName
     * @param code
     * @param password
     * @param responseListener
     */
    public static void regist(Context context,String userName,String code,String password,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("user.username", userName);
        abRequestParams.put("code",code);
        abRequestParams.put("user.password",password);
        new MyHttpUtil(context).post("user/signup", abRequestParams, responseListener);
    }

    /**
     * 重置密码
     * @param context
     * @param userName
     * @param code
     * @param password
     * @param responseListener
     */
    public static void rePassword(Context context,String userName,String code,String password,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("user.username", userName);
        abRequestParams.put("code",code);
        abRequestParams.put("user.password",password);
        new MyHttpUtil(context).post("user/password", abRequestParams, responseListener);
    }

    /**
     * 语音播报
     * @param context
     * @param mobile
     * @param enum_codeType
     * @param responseListener
     */
    public static void voice(Context context,String mobile,Enum_CodeType enum_codeType,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("mobile", mobile);
        abRequestParams.put("type",enum_codeType.toString());
        new MyHttpUtil(context).post("code/voice", abRequestParams, responseListener);
    }

    /**
     * 获取七牛token
     *
     * @param context
     * @param responseListener
     */
    public static void getQiniuToken(Context context, ESResponseListener responseListener) {
        ESRequestParams abRequestParams = new ESRequestParams();
        new MyHttpUtil(context).post("qiniu/token", abRequestParams, responseListener);
    }

    /**
     * 获取七牛Key
     */
    public static void getQiniuKey(Context context, ESResponseListener responseListener) {
        ESRequestParams abRequestParams = new ESRequestParams();
        new MyHttpUtil(context).post("qiniu/key", abRequestParams, responseListener);
    }

    /**
     * 第三放登录
     * @param context
     * @param token
     * @param type 平台类型
     * @param responseListener
     */
    public static void thirdLogin(Context context,String token,Enum_ThirdType type,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("thirdParty.platform", type.toString());
        abRequestParams.put("thirdParty.username", token);
        new MyHttpUtil(context).post("user/third", abRequestParams, responseListener);
    }

    /**
     * 绑定第三方账号到手机号
     * @param context
     * @param userName
     * @param token
     * @param responseListener
     */
    public static void thirdBound(Context context,String userName,String token,ESResponseListener responseListener){
        ESRequestParams abRequestParams = new ESRequestParams();
        abRequestParams.put("user.username", userName);
        abRequestParams.put("thirdParty.username",token);
        new MyHttpUtil(context).post("user/mobile", abRequestParams, responseListener);
    }

    /**
     * 请求高德地图获取周边的小区、写字楼、学校
     * @param context
     * @param location
     * @param types
     * @param responseListener
     */
    public static void placeAround(Context context,String location,String types,AMapResponseListener responseListener){
        ESRequestParams esRequestParams = new ESRequestParams();
        esRequestParams.put("key", "b9410630740ff6a90c303b4bfdfef1ef");
        esRequestParams.put("location",location);
        esRequestParams.put("output","json");
        esRequestParams.put("radius","1000");
        esRequestParams.put("types",types);
        new AMapHttpUtil(context).post("v3/place/around",esRequestParams,responseListener);
    }
}
