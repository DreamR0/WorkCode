package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import java.io.PrintWriter;
import java.io.StringWriter;
import weaver.common.DateUtil;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.*;

/**
 * 考勤数据格式化
 */
public class KQFormatBiz extends BaseBean {
  private String today = DateUtil.getCurrentDate();
  protected KQLog kqLog = new KQLog();

  /**
   * 考勤报表格式化
   *
   * @param lsFormatParams
   */
  public void format(List<List<Object>> lsFormatParams) {
    BatchRecordSet bRs = new BatchRecordSet();
    String sql = "";
    List<Object> params = null;
    try {
      //==zj 不走线程格式化
      if (KQSettingsBiz.getKqformatthread() && false) {
        sql = " insert into kq_format_pool (resourceid, kqdate) values (?,?)";
        if (KQSettingsBiz.getKqformatAccurate()){
          sql = " insert into kq_format_pool (resourceid, kqdate, exectime) values (?,?,?)";
          lsFormatParams = processFormatParams(lsFormatParams);
        }
        bRs.executeBatchSql(sql, lsFormatParams);
      } else {
        String resourceid = "";
        String kqdate = "";
        for (int i = 0; lsFormatParams != null && i < lsFormatParams.size(); i++) {
          params = lsFormatParams.get(i);
          resourceid = Util.null2String(params.get(0));
          kqdate = Util.null2String(params.get(1));
          new BaseBean().writeLog("==zj==(方法进入)");
          new KQFormatData().formatKqDate(resourceid, kqdate);
        }
      }
    } catch (Exception e) {
      writeLog(" KQFormatData.formatKqDate lsFormatParams >>>>>>>>>" + e);
    }
  }

  public void formatDateByKQDate(String kqdate) {
    String sql = "";
    RecordSet rs = new RecordSet();
    List<List<Object>> lsFormatParams = new ArrayList<>();
    List<Object> formatParams = null;
    try {
      if (DateUtil.timeInterval(kqdate, today) < 0) {
        kqLog.info("今天之后的无需处理的数据：kqdate==" + kqdate + "today==" + today);
        return;//今天之后的无需处理
      }

      sql = " SELECT distinct resourceid FROM ( " +
        new KQGroupBiz().getGroupMemberSql() + ") t ";
      rs.executeQuery(sql);
      while (rs.next()) {
        String resourceid = rs.getString("resourceid");
        if(Util.null2String(kqdate).length()!=10)return;
        formatParams = new ArrayList<>();
        formatParams.add(resourceid);
        formatParams.add(kqdate);
        lsFormatParams.add(formatParams);
      }
      this.format(lsFormatParams);
    } catch (Exception e) {
      writeLog(e);
      kqLog.info(e);
    }
  }

  public void formatDateByGroupId(String groupid, String kqdate) {
    String sql = "";
    RecordSet rs = new RecordSet();
    List<List<Object>> lsFormatParams = new ArrayList<>();
    List<Object> formatParams = null;
    try {
      if (DateUtil.timeInterval(kqdate, today) < 0) {
        kqLog.info("今天之后的无需处理的数据：groupid==" + groupid + "kqdate==" + kqdate + "today==" + today);
        return;//今天之后的无需处理
      }
      KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
      KQGroupBiz kqGroupBiz = new KQGroupBiz();
      String kqtype = kqGroupComInfo.getKqtype(groupid);
      if (kqtype.equals("2")) {//排班
        sql = "select resourceid, kqdate from kq_shiftschedule where groupid=" + groupid + " and kqdate='" + kqdate + "' and (isdelete is null or isdelete <> '1') ";
      } else {
        sql = "select resourceid,'" + kqdate + "' from (" + kqGroupBiz.getGroupMemberSql(groupid) + ") t ";
      }
      rs.executeQuery(sql);
      while (rs.next()) {
        String resourceid = rs.getString("resourceid");
        if(Util.null2String(kqdate).length()!=10)return;
        formatParams = new ArrayList<>();
        formatParams.add(resourceid);
        formatParams.add(kqdate);
        lsFormatParams.add(formatParams);
      }
      this.format(lsFormatParams);
    } catch (Exception e) {
      writeLog(e);
      kqLog.info(e);
    }
  }

  public void formatDate(String resourceid, String kqdate) {
    List<List<Object>> lsFormatParams = new ArrayList<>();
    List<Object> formatParams = null;
    try {
      if (DateUtil.timeInterval(kqdate, today) < 0) {
        kqLog.info("今天之后的无需处理的数据：resourceid==" + resourceid + "kqdate==" + kqdate + "today==" + today);
        return;//今天之后的无需处理
      }
      if(Util.null2String(kqdate).length()!=10)return;
      formatParams = new ArrayList<>();
      formatParams.add(resourceid);
      formatParams.add(kqdate);
      lsFormatParams.add(formatParams);
      this.format(lsFormatParams);
    } catch (Exception e) {
      writeLog(e);
      kqLog.info(e);
    }
  }

  public void delFormatData(String resourceid, String kqdate) {
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      sql = " delete from kq_format_detail where resourceid =? and kqdate = ? ";//删除非工作日数据
      rs.executeUpdate(sql, resourceid, kqdate);

      sql = " delete from kq_format_total where resourceid =? and kqdate = ? ";//删除非工作日数据
      rs.executeUpdate(sql, resourceid, kqdate);
    } catch (Exception e) {
      writeLog(e);
      kqLog.info(e);
    }
  }

  public void clearFormatPool() {
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      //删除三天前的数据格式化数据
      if (rs.getDBType().equals("sqlserver")) {
        sql = " delete from kq_format_pool where status = 1  and datediff(day,created,getdate()) > 1";
      } else if (rs.getDBType().equals("mysql")) {
        sql = " delete from kq_format_pool where status = 1  and datediff(now(),created) > 1";
      }
      else if (rs.getDBType().equals("postgresql")) {
        sql = " delete from kq_format_pool where status = 1  and datediff(now(),created) > 1";
      }
      else if (rs.getOrgindbtype().equals("st")) {
        sql = " delete from kq_format_pool where status = 1  and to_number(trunc(sysdate) - trunc(created)) > 1";
      } else {
        sql = " delete from kq_format_pool where status = 1  and trunc(sysdate) - trunc(created) > 1";
      }
      rs.executeUpdate(sql);
    } catch (Exception e) {
      writeLog(e);
    }
  }

  public Map<String,Object> getDefinedField(){
    Map<String,Object> retMap = new HashMap<>();
    String definedField = "";
    String definedFieldSum = "";
    String definedParam = "";
    String definedParamSum = "";
    KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
    while (kqReportFieldComInfo.next()) {
      if (!Util.null2String(kqReportFieldComInfo.getIsenable()).equals("1")) continue;
      if (Util.null2String(kqReportFieldComInfo.getIsSystem()).equals("1")) continue;
      if(KQReportFieldComInfo.cascadekey2fieldname.keySet().contains(kqReportFieldComInfo.getFieldname()))continue;

      if(definedField.length()>0)definedField+=",";
      definedField+=kqReportFieldComInfo.getFieldname();

      if(definedFieldSum.length()>0)definedFieldSum+=",";
      definedFieldSum+="sum("+kqReportFieldComInfo.getFieldname()+") as "+kqReportFieldComInfo.getFieldname();

      if(definedParam.length()>0)definedParam+=",";
      definedParam+="?";

      if(definedParamSum.length()>0)definedParamSum+=",";
      definedParamSum+="sum("+kqReportFieldComInfo.getFieldname()+")";

      String[] cascadekeys = Util.splitString(Util.null2String(kqReportFieldComInfo.getCascadekey()),",");
      for(int i=0;cascadekeys!=null&&i<cascadekeys.length;i++){
        String fieldname = Util.null2String(cascadekeys[i]);
        if(fieldname.length()==0)continue;

        if(definedField.length()>0)definedField+=",";
        definedField+=fieldname;

        if(definedFieldSum.length()>0)definedFieldSum+=",";
        definedFieldSum+="sum("+fieldname+") as "+fieldname;

        if(definedParam.length()>0)definedParam+=",";
        definedParam+="?";

        if(definedParamSum.length()>0)definedParamSum+=",";
        definedParamSum+="sum("+fieldname+")";
      }
    }
    retMap.put("definedField",definedField);
    retMap.put("definedFieldSum",definedFieldSum);
    retMap.put("definedParam",definedParam);
    retMap.put("definedParamSum",definedParamSum);
    return retMap;
  }

  public boolean needCal(String workDate, String workTime){
    boolean needCalForgotCheckMins = true;
    if (KQSettingsBiz.getKqformatAccurate()) {
      workTime = new KQTimesArrayComInfo().turn48to24Time(workTime);
      if (workDate.length() > 0 && workTime.length() > 0) {
        String currentFullTime = DateUtil.getFullDate();
        String endTime = workDate + " " + workTime;
        if (DateUtil.timeInterval(currentFullTime, endTime) > 0) {
          //当前时间之后的状态无效计算
          needCalForgotCheckMins = false;
        }
        kqLog.writeLog("currentFullTime："+currentFullTime+"wroktime:"+endTime+"needCalForgotCheckMins:"+needCalForgotCheckMins);
      }
    }
    return needCalForgotCheckMins;
  }

  private List<List<Object>> processFormatParams(List<List<Object>> lsFormatParams) {
    List<List<Object>> lsFormatParamsTmp = new ArrayList<>();
    try {
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      KQWorkTime kqWorkTime = new KQWorkTime();
      List<Object> formatParams = null;
      for(int i=0;i<lsFormatParams.size();i++){
        formatParams = lsFormatParams.get(i);
        String resourceId = Util.null2String(formatParams.get(0));
        String kqDate = Util.null2String(formatParams.get(1));

        formatParams = new ArrayList<>();
        formatParams.add(resourceId);
        formatParams.add(kqDate);
        formatParams.add(new java.sql.Timestamp(DateUtil.getCalendar(DateUtil.getFullDate()).getTimeInMillis()));
        lsFormatParamsTmp.add(formatParams);

        String nextDate = DateUtil.addDate(kqDate, 1);//下一天日期
        WorkTimeEntity workTime = kqWorkTime.getWorkTime(resourceId, kqDate);
        List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
        if (workTime != null) {
          lsWorkTime = workTime.getWorkTime();//工作时间
          for (int j = 0; lsWorkTime != null && j < lsWorkTime.size(); j++) {
            TimeScopeEntity workTimeScope = lsWorkTime.get(j);
            String workBeginDateTime = workTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
            workBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(workTimeScope.getBeginTime())+":00:00";
            String workEndDateTime = workTimeScope.getEndTimeAcross() ? nextDate : kqDate;
            workEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(workTimeScope.getEndTime())+":00:00";

            formatParams = new ArrayList<>();
            formatParams.add(resourceId);
            formatParams.add(kqDate);
            formatParams.add(new java.sql.Timestamp(DateUtil.getCalendar(workBeginDateTime).getTimeInMillis()));
            lsFormatParamsTmp.add(formatParams);

            formatParams = new ArrayList<>();
            formatParams.add(resourceId);
            formatParams.add(kqDate);
            formatParams.add(new java.sql.Timestamp(DateUtil.getCalendar(workEndDateTime).getTimeInMillis()));
            lsFormatParamsTmp.add(formatParams);
          }
        }else{
          formatParams = new ArrayList<>();
          formatParams.add(resourceId);
          formatParams.add(kqDate);
          lsFormatParamsTmp.add(formatParams);
        }
      }

    }catch (Exception e) {
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
    }
    return lsFormatParamsTmp;
  }
}
