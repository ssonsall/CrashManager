package dev.ssonsall.crashmanager;

import android.content.SharedPreferences;

import java.util.ArrayList;

/*
* Created by 김준훤 on 2020-10-25 (025) 오후 10:59
*/
public class CrashValue {
    private String deviceId;
    private String sharedPreferencesKey;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private String fromAccountName = "";
    private String fromAccoutPW = "";
    private ArrayList<String> toList;

    private CrashValue() {
        this.toList = new ArrayList<>();
        this.fromAccountName = "";
        this.fromAccoutPW = "";
    }

    private static CrashValue instance = new CrashValue();


    public static CrashValue getInstance() {
        return instance;
    }

    public void setFromAccountName(String fromAccountName) {
        this.fromAccountName = fromAccountName;
    }

    public void setFromAccoutPW(String fromAccoutPW) {
        this.fromAccoutPW = fromAccoutPW;
    }

    public void addToAccount(String toAccountName) {
        toList.add(toAccountName);
    }

    public String getFromAccountName() {
        return fromAccountName;
    }

    public String getFromAccoutPW() {
        return fromAccoutPW;
    }

    public ArrayList<String> getToList() {
        return toList;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSharedPreferencesKey() {
        return sharedPreferencesKey;
    }

    public void setSharedPreferencesKey(String sharedPreferencesKey) {
        this.sharedPreferencesKey = sharedPreferencesKey;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public SharedPreferences.Editor getEditor() {
        return editor;
    }

    public void setEditor(SharedPreferences.Editor editor) {
        this.editor = editor;
    }
}
