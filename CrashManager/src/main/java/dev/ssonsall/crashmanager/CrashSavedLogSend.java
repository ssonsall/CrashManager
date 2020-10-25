package dev.ssonsall.crashmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

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
public class CrashSavedLogSend {
    private Context context;

    public CrashSavedLogSend(Context context) {
        this.context = context;
    }

    public void sendMailSavedCrashLog() {
        SharedPreferences sharedPreferences = CrashValue.getInstance().getSharedPreferences();
        String savedLogs = sharedPreferences.getString("crashLogs", "");
        if (savedLogs.equals("")) {
        } else {
            String[] splitLogs = savedLogs.split("//");
            String newLine = "\n";
            String combinedLog = "";
            for (String log : splitLogs) {
                combinedLog += log + newLine + newLine;
            }

            //메일전송
            //Internet 검사
            if (checkInternetConnect(context)) {
                SendEmail sendEmail = new SendEmail();
                try {
                    //쌓인 로그를 보내는 작업은 동기처리가 필요없어서 비동기처리함
                    sendEmail.execute(combinedLog);
                } catch (Exception e) {
                }
            }
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
            if (result) {
                SharedPreferences sharedPreferences = CrashValue.getInstance().getSharedPreferences();
                SharedPreferences.Editor editor = CrashValue.getInstance().getEditor();
                editor.putString("crashLogs", "");
                editor.apply();
            }
        }
    }
}
