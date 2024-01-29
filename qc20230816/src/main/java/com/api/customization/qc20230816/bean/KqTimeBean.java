package com.api.customization.qc20230816.bean;

public class KqTimeBean {
    private String userId = "0"; //人员id
    private String kqDate = ""; //考勤日期
    private Double tmpAttendanceDays = 0.0;//出勤天数
    private int tmpAttendanceMinsCustom = 0;//出勤时长
    private int tmpBeLateMins = 0 ; //迟到时长
    private int tmpLeaveEarlyMins  = 0;//早退时长
    private int tmpAbsenteeismMins  = 0;//旷工时长
    private int tmpLeaveMins = 0;//请假时长
    private int tmpForgotCheckMins = 0;//下班漏签时长
    private int tmpForgotBeginWorkCheckMins = 0;//上班漏签时长
    private int tmpEvectionMins = 0;//出差时长
    private int tmpOutMins = 0;//外出时长
    private int realWorksMins = 0;//上班工时
    private String kqstatus = "";//记录上下午
    private Boolean isAbsenteeism = false; //半天是否旷工
    private Double absenteeismDays = 0.0;//记录旷工天数

    public String getKqstatus() {
        return kqstatus;
    }

    public void setKqstatus(String kqstatus) {
        this.kqstatus = kqstatus;
    }

    public int getTmpForgotCheckMins() {
        return tmpForgotCheckMins;
    }

    public void setTmpForgotCheckMins(int tmpForgotCheckMins) {
        this.tmpForgotCheckMins = tmpForgotCheckMins;
    }

    public int getTmpForgotBeginWorkCheckMins() {
        return tmpForgotBeginWorkCheckMins;
    }

    public void setTmpForgotBeginWorkCheckMins(int tmpForgotBeginWorkCheckMins) {
        this.tmpForgotBeginWorkCheckMins = tmpForgotBeginWorkCheckMins;
    }

    public int getTmpEvectionMins() {
        return tmpEvectionMins;
    }

    public void setTmpEvectionMins(int tmpEvectionMins) {
        this.tmpEvectionMins = tmpEvectionMins;
    }

    public int getTmpOutMins() {
        return tmpOutMins;
    }

    public void setTmpOutMins(int tmpOutMins) {
        this.tmpOutMins = tmpOutMins;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getKqDate() {
        return kqDate;
    }

    public void setKqDate(String kqDate) {
        this.kqDate = kqDate;
    }

    public int getRealWorksMins() {
        return realWorksMins;
    }

    public void setRealWorksMins(int realWorksMins) {
        this.realWorksMins = realWorksMins;
    }

    public Double getTmpAttendanceDays() {
        return tmpAttendanceDays;
    }

    public void setTmpAttendanceDays(Double tmpAttendanceDays) {
        this.tmpAttendanceDays = tmpAttendanceDays;
    }

    public int getTmpAttendanceMinsCustom() {
        return tmpAttendanceMinsCustom;
    }

    public void setTmpAttendanceMinsCustom(int tmpAttendanceMinsCustom) {
        this.tmpAttendanceMinsCustom = tmpAttendanceMinsCustom;
    }

    public int getTmpBeLateMins() {
        return tmpBeLateMins;
    }

    public void setTmpBeLateMins(int tmpBeLateMins) {
        this.tmpBeLateMins = tmpBeLateMins;
    }

    public int getTmpLeaveEarlyMins() {
        return tmpLeaveEarlyMins;
    }

    public void setTmpLeaveEarlyMins(int tmpLeaveEarlyMins) {
        this.tmpLeaveEarlyMins = tmpLeaveEarlyMins;
    }

    public int getTmpAbsenteeismMins() {
        return tmpAbsenteeismMins;
    }

    public void setTmpAbsenteeismMins(int tmpAbsenteeismMins) {
        this.tmpAbsenteeismMins = tmpAbsenteeismMins;
    }

    public int getTmpLeaveMins() {
        return tmpLeaveMins;
    }

    public void setTmpLeaveMins(int tmpLeaveMins) {
        this.tmpLeaveMins = tmpLeaveMins;
    }

    public Boolean getAbsenteeism() {
        return isAbsenteeism;
    }

    public void setAbsenteeism(Boolean absenteeism) {
        isAbsenteeism = absenteeism;
    }

    public Double getAbsenteeismDays() {
        return absenteeismDays;
    }

    public void setAbsenteeismDays(Double absenteeismDays) {
        this.absenteeismDays = absenteeismDays;
    }
}
