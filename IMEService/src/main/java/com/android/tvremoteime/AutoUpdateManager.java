package com.android.tvremoteime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.view.WindowManager;

import com.android.tvremoteime.http.HTTPGet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
/**
 * Created by kingt on 2018/4/11.
 */
public class AutoUpdateManager {
    private static String TAG = "AutoUpdateManager";
    private Context context;
    private Handler handler;
    private File localFile = null;
    // HA Edition: 通过 GitHub Releases API 检查更新
    // API 返回最新 release 的 JSON，包含 tag_name / assets[].browser_download_url / body
    private static String VERSION_URL = "https://api.github.com/repos/tiejiang29/TVRemoteIME-HA/releases/latest";
    public AutoUpdateManager(Context context, Handler handler){
        this.context = context;
        this.handler = handler;
        this.localFile = new File(context.getExternalCacheDir(), context.getString(R.string.app_name) + ".apk");
        this.startUpdateThread();
    }

    private void startUpdateThread(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    JSONObject versionObj = getServerVersionObj();
                    if(versionObj != null && needUpdate(versionObj)){
                        if(downloadInstallAPK(versionObj)){
                            // GitHub release: tag_name 作为 versionName, body 作为更新说明
                            String message = versionObj.has("body") ? versionObj.getString("body") : "";
                            String versionName = versionObj.has("tag_name") ? versionObj.getString("tag_name") : AppPackagesHelper.getCurrentPackageVersion(context);
                            StringBuilder msg = new StringBuilder();
                            msg.append("发现新版本：").append(versionName);
                            if(!TextUtils.isEmpty(message)){
                                msg.append("\r\n更新内容：\r\n\r\n").append(message);
                            }
                            final String content = msg.toString();
                            final boolean forced = false;  // GitHub release 不强制更新

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle(context.getString(R.string.app_name) + "版本更新提示").setIcon(R.drawable.ic_launcher);
                                    builder.setMessage(content);
                                    if (!forced) {
                                        builder.setPositiveButton("稍后更新", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                    }
                                    builder.setNegativeButton("马上更新", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            AppPackagesHelper.installPackage(localFile, context);
                                            dialog.dismiss();
                                        }
                                    });
                                    try {
                                        AlertDialog dialog = builder.create();
                                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                        dialog.setCanceledOnTouchOutside(false);
                                        dialog.show();
                                    } catch (Exception ex) {
                                        AppPackagesHelper.installPackage(localFile, context);
                                    }
                                }
                            });
                        }
                    }
                }catch (Exception e){
                    Log.e(TAG, "startUpdateThread", e);
                }
            }
        });
        thread.start();
    }

    private int getCurrentPackageVersion(){
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }catch (PackageManager.NameNotFoundException e){}
        return -1;
    }

    private JSONObject getServerVersionObj(){
        try{
            // 添加 Accept header 让 GitHub 返回 JSON
            String jsonData = HTTPGet.readStringWithAccept(VERSION_URL, "application/vnd.github+json");
            if (!TextUtils.isEmpty(jsonData)) {
                if(Environment.needDebug) Environment.debug(TAG, "getServerVersionObj:\r\n" + jsonData);
                return new JSONObject(jsonData);
            }
        } catch (Exception e) {
            Log.e(TAG, "getServerVersionObj", e);
        }
        return null;
    }

    private boolean needUpdate(JSONObject versionObj){
        int version = getCurrentPackageVersion();
        if(version == -1 || versionObj == null) return false;

        try {
            // GitHub release: tag_name 格式如 "v1.4.2-ha" 或 "1.4.2-ha"
            // 我们用 release 的 name 或 tag_name 解析 versionCode，但更可靠的是用 release.id
            // 简单策略：如果 tag_name 与当前 versionName 不同，就提示更新
            // 这里保持原有逻辑：要求 release 里有 versionCode 字段
            // 由于 GitHub Release 不直接提供 versionCode，我们用 tag_name 比较法：
            // 如果 tag_name != 当前 versionName 且能下载到 APK，就提示更新
            String currentVersionName = AppPackagesHelper.getCurrentPackageVersion(context);
            String tag = versionObj.has("tag_name") ? versionObj.getString("tag_name") : "";
            // 移除前缀 v
            if (tag.startsWith("v") || tag.startsWith("V")) tag = tag.substring(1);
            // 比较 versionName
            return !tag.isEmpty() && !tag.equals(currentVersionName) && versionObj.has("assets");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean needDownloadAPK(JSONObject versionObj) {
        try {
            if(! this.localFile.exists()) return true;

            PackageManager pm = context.getPackageManager();
            PackageInfo packInfo = pm.getPackageArchiveInfo(this.localFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (packInfo == null) return true;
            int localVersion = packInfo.versionCode;
            if(Environment.needDebug) Environment.debug(TAG, "needDownloadAPK: localVersion = " + localVersion);
            // 拿 GitHub release 的最新 id 作为版本标识
            int releaseId = versionObj.getInt("id");
            return localVersion < releaseId;  // release id 一般随版本递增
        }catch (Exception e){
            return true;
        }
    }

    private boolean downloadInstallAPK(JSONObject versionObj){
        try {
            if(!needDownloadAPK(versionObj)){
                return true;
            }

            // GitHub Release: assets 数组里找 .apk 文件
            org.json.JSONArray assets = versionObj.getJSONArray("assets");
            String url = null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getString("name");
                if (name.endsWith(".apk")) {
                    url = asset.getString("browser_download_url");
                    break;
                }
            }
            if(TextUtils.isEmpty(url)) return false;
            if(Environment.needDebug) Environment.debug(TAG, "downloadInstallAPK starting: " + url);
            boolean flag = HTTPGet.downloadFile(url, this.localFile);
            if(Environment.needDebug) Environment.debug(TAG, "downloadInstallAPK finished. result: " + flag);
            return flag;
        } catch (Exception e) {
            Log.e(TAG, "downloadInstallAPK", e);
        }
        return false;
    }
}
