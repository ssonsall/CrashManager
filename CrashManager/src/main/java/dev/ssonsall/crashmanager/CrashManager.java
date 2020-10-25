package dev.ssonsall.crashmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/*
 * Created by 김준훤 on 2020-10-25 (025) 오후 10:59
 */
public class CrashManager implements Thread.UncaughtExceptionHandler {
    private Context context;
    private String deviceId;
    private String sharedPreferencesKey;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public CrashManager(Context context) {
        this.context = context;
        this.sharedPreferencesKey = "crashmanager-8c636754-0668-4646-9360-147c3f223e7a";
        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.sharedPreferences = context.getSharedPreferences(sharedPreferencesKey, context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
        this.deviceId = sharedPreferences.getString("deviceId", "");
        if (TextUtils.isEmpty(deviceId) || deviceId.equals("")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");
            long currentTimeLong = System.currentTimeMillis();
            String extensionDeviceId = format.format(currentTimeLong);
            deviceId = UUID.randomUUID().toString() + "-" + extensionDeviceId;
            editor.putString("deviceId", deviceId);
            editor.commit();
        }

        CrashValue crashValue = CrashValue.getInstance();
        crashValue.setDeviceId(sharedPreferences.getString("deviceId", ""));
        crashValue.setSharedPreferencesKey(sharedPreferencesKey);
        crashValue.setSharedPreferences(sharedPreferences);
        crashValue.setEditor(editor);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        //앱이 Crash로 종료되기 전 수행할 로직.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        final String stackTrace = sw.toString();

        SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
        long currentTimeLong = System.currentTimeMillis();
        String crashTime = format.format(currentTimeLong);
        String appPackageName = context.getPackageName();
        PackageInfo pInfo = null;
        int versionCode = -1;
        String versionName = "";
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (pInfo != null) {
            versionCode = pInfo.versionCode;
            versionName = pInfo.versionName;
        }

        String newLine = "\n";
        String combinedLog = "Crash Time : " + crashTime + newLine
                + "Package Name : " + appPackageName + newLine
                + "Version Code : " + versionCode + newLine
                + "Version Name : " + versionName + newLine
                + "Device Id : " + deviceId + newLine + newLine
                + "**Crash Log Begin**" + newLine
                + stackTrace
                + "**Crash Log Finish**";

        if (checkInternetConnect(context)) {
            SendEmail sendEmail = new SendEmail();
            try {
                Boolean result = false;
                result = sendEmail.execute(combinedLog).get();
                if (!result) {
                    saveFailSendMailException(combinedLog);
                }
            } catch (Exception e) {
                saveFailSendMailException(combinedLog);
            }
        } else {
            saveFailSendMailException(combinedLog);
        }
        uncaughtExceptionHandler.uncaughtException(thread, throwable);
    }

    private void saveFailSendMailException(String stackTrace) {
        String savedLogs = sharedPreferences.getString("crashLogs", "");
        stackTrace += "//";
        savedLogs += stackTrace;
        editor.putString("crashLogs", savedLogs);
        editor.commit();
    }

    private boolean checkInternetConnect(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        } else {
            return false;
        }
        return false;
    }

    class SendEmail extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(CrashValue.getInstance().getFromAccountName(), CrashValue.getInstance().getFromAccoutPW());
                        }
                    });

            try {
                CrashValue crashValue = CrashValue.getInstance();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(crashValue.getFromAccountName()));
                int toListSize = crashValue.getToList().size();
                InternetAddress[] internetAddressesecipients = new InternetAddress[toListSize];
                for (int i = 0; i < toListSize; i++) {
                    internetAddressesecipients[i] = new InternetAddress(crashValue.getToList().get(i));
                }
                SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                long currentTimeLong = System.currentTimeMillis();
                String crashTime = format.format(currentTimeLong);
                message.setRecipients(Message.RecipientType.TO, internetAddressesecipients);
                message.setSubject("[App Crash]" + crashTime + " -> " + context.getPackageName());
                message.setText(strings[0]);
                Transport.send(message);
                return true;
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setFromAccountAndPw(String fromAccount, String fromPw) {
        CrashValue.getInstance().setFromAccountName(fromAccount);
        CrashValue.getInstance().setFromAccoutPW(fromPw);
    }

    public void addToAccount(String toAccount) {
        CrashValue.getInstance().addToAccount(toAccount);
    }
}
