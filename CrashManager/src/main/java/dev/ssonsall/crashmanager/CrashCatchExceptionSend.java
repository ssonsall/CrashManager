package dev.ssonsall.crashmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Properties;

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
public class CrashCatchExceptionSend {
    private Context context;
    private String cathcedException;
    private String combinedLog;
    public CrashCatchExceptionSend(Context context) {
        this.context = context;
        this.cathcedException = "";
    }

    public void sendMailCatchException(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        cathcedException = sw.toString();
        cathcedException = "[Catched]\n" + cathcedException;

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
        String deviceId = CrashValue.getInstance().getSharedPreferences().getString("deviceId","");

        combinedLog = "Crash Time : " + crashTime + newLine
                + "Package Name : " + appPackageName + newLine
                + "Version Code : " + versionCode + newLine
                + "Version Name : " + versionName + newLine
                + "Device Id : " + deviceId + newLine + newLine
                + "**Crash Log Begin**" + newLine
                + cathcedException
                + "**Crash Log Finish**";

        if (checkInternetConnect(context)) {
            SendEmail sendEmail = new SendEmail();
            try {
                sendEmail.execute(combinedLog);
            } catch (Exception e) {
            }
        } else {
            SharedPreferences sharedPreferences = CrashValue.getInstance().getSharedPreferences();
            String savedLogs = sharedPreferences.getString("crashLogs", "");
            combinedLog += "//";
            savedLogs += combinedLog;
            SharedPreferences.Editor editor = CrashValue.getInstance().getEditor();
            editor.putString("crashLogs", savedLogs);
            editor.apply();
        }
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
                message.setFrom(new InternetAddress(CrashValue.getInstance().getFromAccountName()));
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
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!result) {
                SharedPreferences sharedPreferences = CrashValue.getInstance().getSharedPreferences();
                String savedLogs = sharedPreferences.getString("crashLogs", "");
                combinedLog += "//";
                savedLogs += combinedLog;
                SharedPreferences.Editor editor = CrashValue.getInstance().getEditor();
                editor.putString("crashLogs", savedLogs);
                editor.apply();
            }
        }
    }
}
