package cn.org.eshow.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.org.eshow.activity.LoginActivity_;
import cn.org.eshow.common.Global;
import cn.org.eshow.common.SharedPrefUtil;
import cn.org.eshow.view.ConfirmDialog;
import cn.org.eshow_structure.util.ESLogUtil;

/**
 * 用于通知界面更新、界面操作、退出程序等的通知接收
 * Created by daikting on 16/3/15.
 */
public class UpdateViewReveiver extends BroadcastReceiver
{
    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();

        if(action.equals(Global.EShow_Broadcast_Action.ACTION_EXIT)){
            ESLogUtil.d(context, "接收到广播通知，Global.EShow_Broadcast_Action.ACTION_EXIT，应用退出");
            try {
                final Activity activity = (Activity) context;
                ConfirmDialog.OnCustomDialogListener onCustomDialogListener = new ConfirmDialog.OnCustomDialogListener() {
                    @Override
                    public void OnCustomDialogConfim(String str) {
                        SharedPrefUtil.logout(context);
                        LoginActivity_.intent(context).start();
                        activity.finish();
                    }

                    @Override
                    public void OnCustomDialogCancel(String str) {

                    }
                };
                ConfirmDialog confirmDialog = new ConfirmDialog(context,"退出","确定退出？","确定","取消",onCustomDialogListener);
                confirmDialog.show();
            }catch (Exception e){
                e.printStackTrace();
            }

        }else if (action.equals(Global.EShow_Broadcast_Action.ACTION_EXIT_REPASSWORD)) {//修改密码成功后退出
            ESLogUtil.d(context, "接收到广播通知，修改密码成功后退出，重启登录界面");
            try {
                final Activity activity = (Activity) context;
                ConfirmDialog.OnCustomDialogListener onCustomDialogListener = new ConfirmDialog.OnCustomDialogListener() {
                    @Override
                    public void OnCustomDialogConfim(String str) {
                        SharedPrefUtil.logout(context);
                        LoginActivity_.intent(context).start();
                        activity.finish();
                    }

                    @Override
                    public void OnCustomDialogCancel(String str) {
                        SharedPrefUtil.logout(context);
                        LoginActivity_.intent(context).start();
                        activity.finish();
                    }
                };
                ConfirmDialog confirmDialog = new ConfirmDialog(context,"退出","由于密码被修改，需要重新登录！","确定",null,onCustomDialogListener);
                confirmDialog.show();
            }catch (Exception e){
                e.printStackTrace();
            }

        }else if(action.equals(Global.EShow_Broadcast_Action.ACTION_MISSING_TOKEN)){
            ESLogUtil.d(context, "接收到广播通知，登录过期，重启登录界面");
            //token过期
            try {
                final Activity activity = (Activity) context;
                ConfirmDialog.OnCustomDialogListener onCustomDialogListener = new ConfirmDialog.OnCustomDialogListener() {
                    @Override
                    public void OnCustomDialogConfim(String str) {
                        SharedPrefUtil.logout(context);
                        LoginActivity_.intent(context).start();
                        activity.finish();
                    }

                    @Override
                    public void OnCustomDialogCancel(String str) {
                        SharedPrefUtil.logout(context);
                        LoginActivity_.intent(context).start();
                        activity.finish();
                    }
                };
                ConfirmDialog confirmDialog = new ConfirmDialog(context,"退出","由于登录已过期，需要重新登录！","确定",null,onCustomDialogListener);
                confirmDialog.show();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}