/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hk.mcc.utils.applog2es;

import java.util.Date;

/**
 *
 * @author cc
 */
public class AppLog {
    private Date logTime;
    private String server;
    private String level;
    private String code;
    private String className;
    private String tid;
    private String userId;
    private String ecid;
    private String application;
    private String message;

    public AppLog() {
    }

    
    
    public AppLog(Date logTime, String server, String level, String code, String className, String tid, String userId, String ecid, String application, String message) {
        this.logTime = logTime;
        this.server = server;
        this.level = level;
        this.code = code;
        this.className = className;
        this.tid = tid;
        this.userId = userId;
        this.ecid = ecid;
        this.application = application;
        this.message = message;
    }

    public String getServer() {
        return server;
    }

    public String getLevel() {
        return level;
    }

    public String getCode() {
        return code;
    }

    public String getClassName() {
        return className;
    }

    public String getTid() {
        return tid;
    }

    public String getUserId() {
        return userId;
    }

    public String getEcid() {
        return ecid;
    }

    public String getApplication() {
        return application;
    }

    public Date getLogTime() {
        return logTime;
    }

    public void setLogTime(Date logTime) {
        this.logTime = logTime;
    }


    public void setServer(String server) {
        this.server = server;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEcid(String ecid) {
        this.ecid = ecid;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    
    
    @Override
    public String toString() {
        return "AppLog{" + "logTime=" + logTime + ", server=" + server + ", level=" + level + ", code=" + code + ", className=" + className + ", tid=" + tid + ", userId=" + userId + ", ecid=" + ecid + ", application=" + application + ", message=" + message + '}';
    }
    
    
}
