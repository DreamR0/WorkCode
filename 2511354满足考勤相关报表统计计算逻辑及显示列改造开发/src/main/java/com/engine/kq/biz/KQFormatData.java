package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.Util_DataMap;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.cmd.attendanceButton.ButtonStatusEnum;
import com.engine.kq.entity.KQShiftRuleEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.FlowReportTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.UtilKQ;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import weaver.common.DateUtil;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.general.BaseBean;
import weaver.general.InitServer;
import weaver.general.Util;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 考勤数据格式化
 */
public class KQFormatData extends BaseBean {
  private String today = DateUtil.getCurrentDate();
  private KQLog kqLog = new KQLog();
  private boolean writeLog = false;
  private LinkedHashMap<String,Object> logInfo = new LinkedHashMap<>();

  /***
   * 该方法不允许直接调用
   * @param userId
   * @param kqDate
   * @return
   */
  public Map<String, Object> formatKqDate(String userId, String kqDate) {
    List<List<Object>> lsParam = new ArrayList<>();
    //非工作日处理
    List<Object> nonlsParam = null;
    Map<String, Object> resultMap = new HashMap<>();
    BatchRecordSet bRs = new BatchRecordSet();
    KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      kqLog.info("formatKqDate in userId=" + userId + "kqDate==" + kqDate);
      if (DateUtil.timeInterval(kqDate, today) < 0) {//今天之后的无需处理
        kqLog.info("今天之后的无需处理的数据：resourceid=="+userId+"kqdate=="+kqDate+"today=="+today);
        return resultMap;
      }
      String uuid = UUID.randomUUID().toString();
      KQFormatFreeData kqFormatFreeData = new KQFormatFreeData();
      KQWorkTime kqWorkTime = new KQWorkTime();
      kqWorkTime.setIsFormat(true);
      String kqDateNext = DateUtil.addDate(kqDate, 1);

      KQFlowDataBiz kqFlowDataBiz = new KQFlowDataBiz.FlowDataParamBuilder().resourceidParam(userId).fromDateParam(kqDate).toDateParam(kqDateNext).build();
      Map<String, Object> workFlowInfo = new HashMap<>();//userid|date--工作流程
      kqFlowDataBiz.getAllFlowData(workFlowInfo,false);
      WorkTimeEntity workTime = kqWorkTime.getWorkTime(userId, kqDate);
      kqLog.info("userId:"+userId+":kqDate:"+kqDate+":formatKqDate workTime=" + JSONObject.toJSONString(workTime)+"::uuid::"+uuid);
      kqLog.info("userId:"+userId+":kqDate:"+kqDate+":formatKqDate workFlowInfo=" + JSONObject.toJSONString(workFlowInfo)+"::uuid::"+uuid);

      if(this.writeLog) {
        logInfo.put("userId",userId);
        logInfo.put("kqDate",kqDate);
        logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(16253,weaver.general.ThreadVarLanguage.getLang())+"",workTime);
        logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(126871,weaver.general.ThreadVarLanguage.getLang())+"",workFlowInfo);
      }

      new KQFormatBiz().delFormatData(userId, kqDate);

      String excludecount = Util.null2String(kqGroupComInfo.getExcludecount(workTime.getGroupId()));//是否参与考勤报表统计
      if (workTime.getIsExclude()) {//无需考勤人员没有异常状态
        if(!excludecount.equals("1")){
          kqLog.info("无需考勤人员没有异常状态 workTime.getIsExclude()="+workTime.getIsExclude()+"excludecount=="+excludecount);
          return resultMap;
        }
      }
      if( Util.null2String(workTime.getGroupId()).length()==0){
        //没有考勤组不需格式化
        return resultMap;
      }
      if (workTime == null || workTime.getWorkMins() == 0) {
        kqLog.info("workTime == null || workTime.getWorkMins() == 0 插入空记录");
        nonlsParam = new ArrayList<>();
        formatNonWork(userId, kqDate,nonlsParam,workTime, workFlowInfo);

        if(!nonlsParam.isEmpty()){
          sql = " insert into kq_format_detail(resourceid,kqdate,groupid,serialnumber,signindate,signintime,signinid,signoutdate,signouttime,signoutid,leaveMins,leaveinfo,evectionMins,outMins)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
          rs.executeUpdate(sql, nonlsParam);

          sql = " insert into kq_format_total(resourceid,kqdate,subcompanyid,departmentid,jobtitle,groupid,serialid,workdays," +
                  " workmins,attendancedays,attendancemins,signdays,signmins,belate,belatemins,gravebelate,gravebelatemins,leaveeearly,leaveearlymins," +
                  " graveleaveearly,graveleaveearlymins,absenteeism,absenteeismmins,forgotcheck, forgotcheckMins,leaveMins,evectionMins,outMins)" +
                  " select a.resourceid,kqdate,b.subcompanyid1,b.departmentid,b.jobtitle,groupid,serialid," +
                  " case when sum(workmins)>0 then 1 end as workdays, sum(workmins) as workmins," +
                  " 0 as attendancedays, sum(attendanceMins) as attendanceMins," +
                  " 0 as signdays, sum(signmins) as signmins," +
                  " sum(case when belatemins> 0 then 1 else 0 end) as belate,sum(belatemins) as belatemins," +
                  " sum(case when graveBeLateMins> 0 then 1 else 0 end) as graveBeLate,sum(graveBeLateMins) as graveBeLateMins," +
                  " sum(case when leaveearlymins> 0 then 1 else 0 end) as leaveearly,sum(leaveearlymins) as leaveearlymins," +
                  " sum(case when graveLeaveEarlyMins> 0 then 1 else 0 end) as graveLeaveEarly,sum(graveLeaveEarlyMins) as graveLeaveEarlyMins," +
                  " sum(case when absenteeismmins> 0 then 1 else 0 end) as absenteeism,sum(absenteeismmins) as absenteeismmins," +
                  " sum(case when forgotcheckmins> 0 then 1 else 0 end) as forgotcheck,sum(forgotcheckmins) as forgotcheckmins, " +
                  " sum(leaveMins) as leaveMins, sum(evectionMins) as evectionMins, sum(outMins) as outMins" +
                  " from kq_format_detail a, hrmresource b" +
                  " where a.resourceid = b.id and resourceid =? and kqdate=?" +
                  " group by resourceid,kqdate,b.subcompanyid1,b.departmentid,b.jobtitle,groupid,serialid,workmins";
          rs.executeUpdate(sql, userId, kqDate);
        }
      }else{
        Map<String,Object> definedFieldInfo = new KQFormatBiz().getDefinedField();
        String definedField = "";
        String definedParam = "";
        String definedParamSum = "";
        if (workTime.getKQType().equals("3")) {//自由工时
          lsParam.addAll(kqFormatFreeData.format(userId, kqDate, workFlowInfo));
        } else {
          definedField = Util.null2String(definedFieldInfo.get("definedField"));
          definedParam = Util.null2String(definedFieldInfo.get("definedParam"));
          definedParamSum = Util.null2String(definedFieldInfo.get("definedParamSum"));
          lsParam.addAll(format(userId, kqDate, workTime, workFlowInfo,uuid));
        }

        if (lsParam.size() > 0) {
          sql = " insert into kq_format_detail( " +
                  " resourceid,kqdate,groupid,serialid,serialnumber,workbegindate,workbegintime,workenddate,workendtime,workmins," +
                  " signindate,signintime,signinid,signoutdate,signouttime,signoutid,signMins," +
                  " attendanceMins,belatemins,graveBeLateMins,leaveearlymins,graveLeaveEarlyMins,absenteeismmins,forgotcheckMins," +
                  " leaveMins,leaveinfo,evectionMins,outMins,forgotbeginworkcheckmins,otherinfo"+(definedField.length()>0?","+definedField+"":"")+") " +
                  " values(?,?,?,?,?,?,?,?,?,?, ?,?,?,?,?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?"+(definedField.length()>0?","+definedParam+"":"")+")";
          for (int i = 0; i < lsParam.size(); i++) {
            List<Object> param = lsParam.get(i);
            boolean isok = rs.executeUpdate(sql, param);
            kqLog.info("插入记录:userId:"+userId+":kqDate:"+kqDate+":param:"+JSON.toJSONString(param)+":isok:"+isok+"::uuid::"+uuid);
          }

          sql = " insert into kq_format_total(resourceid,kqdate,subcompanyid,departmentid,jobtitle,groupid,serialid,workdays,workmins," +
                  " attendancedays,attendancemins,signdays,signmins,belate,belatemins,gravebelate,gravebelatemins,leaveeearly,leaveearlymins,graveleaveearly," +
                  " graveleaveearlymins,absenteeism,absenteeismmins,forgotcheck,forgotcheckmins," +
                  " leaveMins,evectionMins,outMins,forgotbeginworkcheck,forgotbeginworkcheckmins"+(definedField.length()>0?","+definedField+"":"")+") " +
                  " select a.resourceid,kqdate,b.subcompanyid1,b.departmentid,b.jobtitle,groupid,serialid," +
                  " case when sum(workmins)>0 then 1 end as workdays, sum(workmins) as workmins," +
                  " cast(sum(attendanceMins)AS decimal(10, 2))/sum(workmins) as attendancedays, sum(attendanceMins) as attendanceMins," +
                  " cast(sum(signmins)AS decimal(10, 2))/sum(workmins) as signdays, sum(signmins) as signmins," +
                  " sum(case when belatemins> 0 then 1 else 0 end) as belate,sum(belatemins) as belatemins," +
                  " sum(case when graveBeLateMins> 0 then 1 else 0 end) as graveBeLate,sum(graveBeLateMins) as graveBeLateMins," +
                  " sum(case when leaveearlymins> 0 then 1 else 0 end) as leaveearly,sum(leaveearlymins) as leaveearlymins," +
                  " sum(case when graveLeaveEarlyMins> 0 then 1 else 0 end) as graveLeaveEarly,sum(graveLeaveEarlyMins) as graveLeaveEarlyMins, " +
                  " sum(case when absenteeismmins> 0 then 1 else 0 end) as absenteeism,sum(absenteeismmins) as absenteeismmins," +
                  " sum(case when forgotcheckmins> 0 then 1 else 0 end) as forgotcheck,sum(forgotcheckmins) as forgotcheckmins,sum(leaveMins) as leaveMins," +
                  " sum(evectionMins) as evectionMins,sum(outMins) as outMins, " +
                  " sum(case when forgotbeginworkcheckmins> 0 then 1 else 0 end) as forgotbeginworkcheck,sum(forgotbeginworkcheckmins) as forgotbeginworkcheckmins " +
                  (definedField.length()>0?","+definedParamSum+"":"")+
                  " from kq_format_detail a, hrmresource b" +
                  " where a.resourceid = b.id and resourceid = ? and kqdate=?" +
                  " group by resourceid,kqdate,b.subcompanyid1,b.departmentid,b.jobtitle,groupid,serialid";
          rs.executeUpdate(sql, userId, kqDate);
        }
      }
    }catch (Exception e) {
      writeLog(e);
      kqLog.info(e);
    }
    return resultMap;
  }

  public List<List<Object>> format(String userId, String kqDate, WorkTimeEntity workTime,
                                   Map<String, Object> workFlowInfo, String uuid) {
    List<List<Object>> lsParam = new ArrayList<>();
    List<Object> params = null;
    try {
      KQSettingsComInfo kqSettingsComInfo = new KQSettingsComInfo();
      String nosign_is_absent = Util.null2String(kqSettingsComInfo.getMain_val("nosign_is_absent"),"1");
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      KQFormatShiftRule kqFormatShiftRule = new KQFormatShiftRule();
      String preDate = DateUtil.addDate(kqDate, -1);//上一天日期
      String nextDate = DateUtil.addDate(kqDate, 1);//下一天日期
      String dateKey = userId + "|" + kqDate;
      String nextDateKey = userId + "|" + nextDate;
      ArrayList<String> hostIps = InitServer.getRealIp();
      kqLog.info("format in >>>>>userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
      boolean oneSign = false;
      List<TimeScopeEntity> lsSignTime = new ArrayList<>();
      List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
      List<TimeScopeEntity> lsRestTime = new ArrayList<>();
      List<Object> workFlow = null;

      if (workTime != null) {
        lsSignTime = workTime.getSignTime();//允许打卡时间
        lsWorkTime = workTime.getWorkTime();//工作时间
        lsRestTime = workTime.getRestTime();//休息时段时间
        oneSign = lsWorkTime!=null&&lsWorkTime.size()==1;
      }

      int[] dayMins = new int[2880];//一天所有分钟数
      Arrays.fill(dayMins, -1);
//一天4次打卡单独做判断，如果是上午下班打卡和下午上班打卡时间重叠，那么上午的下班卡取最早的，下午的上班卡取最晚的。用shiftCount是否等于-1判断，-1就走标准不重叠。2就表示重叠走新的逻辑
      int shiftCount = lsWorkTime == null ? 0 : lsWorkTime.size();
      int shiftI = 0;
      String signEndDateTimeZero = "";
      for (int i = 0; lsWorkTime != null && i < lsWorkTime.size(); i++) {
        shiftI = i;
        TimeScopeEntity signTimeScope = lsSignTime.get(i);
        TimeScopeEntity workTimeScope = lsWorkTime.get(i);
        String signBeginDateTime = signTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
        if(signTimeScope.isBeginTimePreAcross()){
          signBeginDateTime = preDate;
        }
        signBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime())+":00";
        String signEndDateTime = signTimeScope.getEndTimeAcross() ? nextDate : kqDate;
        signEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime())+":59";
        if (shiftCount == 2 && shiftI == 0) {
          signEndDateTimeZero = signEndDateTime;
        }
        if (shiftCount == 2 && shiftI == 1) {
          shiftCount = signBeginDateTime.compareTo(signEndDateTimeZero) <= 0 ? shiftCount : -1;
        }
      }
      for (int i = 0; lsWorkTime != null && i < lsWorkTime.size(); i++) {
        shiftI = i;        
		params = new ArrayList<>();
        TimeScopeEntity signTimeScope = lsSignTime.get(i);
        TimeScopeEntity workTimeScope = lsWorkTime.get(i);
        //TimeScopeEntity restTimeScope = lsRestTime.isEmpty()?null:lsRestTime.get(i);
        String workBeginTime = Util.null2String(workTimeScope.getBeginTime());
        String ori_workBeginTime = workBeginTime;
        int workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
        boolean workBenginTimeAcross = workTimeScope.getBeginTimeAcross();
        String workEndTime = Util.null2String(workTimeScope.getEndTime());
        String ori_workEndTime = workEndTime;
        int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
        boolean workEndTimeAcross = workTimeScope.getEndTimeAcross();
        int workMins = workTimeScope.getWorkMins();

        String workBeginDate = workBenginTimeAcross ? nextDate : kqDate;
        String workEndDate = workEndTimeAcross ? nextDate : kqDate;

        Arrays.fill(dayMins, workBeginIdx, workEndIdx, 1);//工作时段标识 1

        int beginIdx = 0;
        int endIdx = 0;
        int checkIn = 0;
        int checkOut = 0;
        String signInId = "";
        String signInDate = "";
        String signInTime = "";
        String signOutId = "";
        String signOutDate = "";
        String signOutTime = "";
        int earlyInMins = 0;//早到分钟数
        int lateOutMins = 0;//晚走分钟数
        int signMins = 0;//签到签退时长
        int tmpAttendanceMins = 0;//出勤分钟数（流程抵扣来的）
        int attendanceMins = 0;
        int beLateMins = 0;
        int graveBeLateMins = 0;
        int leaveEarlyMins = 0;
        int graveLeaveEarlyMins = 0;
        int absenteeismMins = 0;
        int leaveMins = 0;//请假时长
        Map<String,Object> leaveInfo = new HashMap<>();//请假信息
        Map<String,Object> otherinfo = new HashMap<>();//存一些用得到的信息
        int evectionMins = 0;//出差时长
        int outMins = 0;//公出时长
        int otherMins = 0;//异常流程时长
        int forgotCheckMins = 0;
        int forgotBeginWorkCheckMins = 0;//上班漏签
        int signInTimeIndx = -1;
        int flowSignInTimeIndx = -1;
        int signInTimeOutdx = -1;
        //用来计算实际打卡时长用的
        int signInTimeIndx4Sign = -1;
        int signInTimeOutdx4Sign = -1;

        String signBeginDateTime = signTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
        if(signTimeScope.isBeginTimePreAcross()){
          signBeginDateTime = preDate;
        }
        signBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime())+":00";
        String signEndDateTime = signTimeScope.getEndTimeAcross() ? nextDate : kqDate;
        signEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime())+":59";

        String workBeginDateTime = workTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
        workBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(workTimeScope.getBeginTime())+":00";
        String workEndDateTime = workTimeScope.getEndTimeAcross() ? nextDate : kqDate;
        workEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(workTimeScope.getEndTime())+":00";

        kqLog.info("signBeginDateTime" + signBeginDateTime+"::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
        kqLog.info("signEndDateTime" + signEndDateTime+"::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
        kqLog.info("workBeginDateTime" + workBeginDateTime+"::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
        kqLog.info("workEndDateTime" + workEndDateTime+"::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);

        Map<String, String> shifRuleMap = Maps.newHashMap();
        if(oneSign){
          //个性化设置只支持一天一次上下班
          ShiftInfoBean shiftInfoBean = new ShiftInfoBean();
          shiftInfoBean.setSplitDate(kqDate);
          shiftInfoBean.setShiftRuleMap(workTime.getShiftRuleInfo());
          shiftInfoBean.setSignTime(lsSignTime);
          shiftInfoBean.setWorkTime(lsWorkTime);
          List<String> logList = Lists.newArrayList();
          KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean, userId, shifRuleMap,logList);
          if(!shifRuleMap.isEmpty()){
            if(!logList.isEmpty()){
              otherinfo.put("logList", logList);
            }
            otherinfo.put("shiftRule", shifRuleMap);
            if(shifRuleMap.containsKey("shift_beginworktime")){
              String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
              if(shift_beginworktime.length() > 0){
                workBeginTime = Util.null2String(shift_beginworktime);
                workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
                workTimeScope.setBeginTime(workBeginTime);
                workTimeScope.setBeginTimeAcross(workBeginIdx>=1440?true:false);
              }
            }
            if(shifRuleMap.containsKey("shift_endworktime")){
              String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
              if(shift_endworktime.length() > 0){
                workEndTime = Util.null2String(shift_endworktime);
                workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
                workTimeScope.setEndTime(workEndTime);
                workTimeScope.setEndTimeAcross(workEndIdx>=1440?true:false);
              }
            }
          }
          kqLog.info("个性化之后 signBeginDateTime" + signBeginDateTime);
          kqLog.info("个性化之后 signEndDateTime" + signEndDateTime);
          kqLog.info("个性化之后 workBeginDateTime" + workBeginDateTime);
          kqLog.info("个性化之后 workEndDateTime" + workEndDateTime);
        }
        List<Object> lsCheckInfo = new KQFormatSignData().getSignInfo(userId,signTimeScope,workTimeScope,kqDate,preDate,nextDate,kqTimesArrayComInfo,hostIps,uuid,shiftCount,shiftI);
        kqLog.info("lsCheckInfo" + JSONObject.toJSONString(lsCheckInfo)+"::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
        if(this.writeLog) {
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005297,weaver.general.ThreadVarLanguage.getLang())+"",signBeginDateTime);
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005298,weaver.general.ThreadVarLanguage.getLang())+"",signEndDateTime);
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(1940,weaver.general.ThreadVarLanguage.getLang())+"",workBeginDateTime);
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005299,weaver.general.ThreadVarLanguage.getLang())+"",workEndDateTime);
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005300,weaver.general.ThreadVarLanguage.getLang())+"",lsCheckInfo);
        }
        for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
          Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(j);
          String signStatus = Util.null2String(checkInfo.get("signStatus"));
          String signId = Util.null2String(checkInfo.get("signId"));
          String signDate = Util.null2String(checkInfo.get("signDate"));
          String signTime = Util.null2String(checkInfo.get("signTime"));
          String deduct_signintime = Util.null2String(checkInfo.get("deduct_signintime"));
          String deduct_signofftime = Util.null2String(checkInfo.get("deduct_signofftime"));
          String flow_signInTime = "";
          String flow_signOutTime = "";
          if(kqDate.compareTo(signDate) < 0)endIdx+=1440;
          if (signTime.length() > 8) {
            signTime = signTime.substring(0, 8);
          }
          if (checkInfo.get("signType").equals("1")) {//签到
            checkIn++;
            //如果流程抵扣存在，打卡时长也存在，那么相互比较得到出勤时长和打卡时长 暂不这样处理，还是按照漏签的逻辑来处理
            if(signTime.length() > 0){
              signInTimeIndx4Sign = kqTimesArrayComInfo.getArrayindexByTimes(signTime);
            }
            signInId = signId;
            signInDate = signDate;
            signInTime = signTime;
            signInTimeIndx = kqTimesArrayComInfo.getArrayindexByTimes(signInTime);
            if(deduct_signintime.length() > 0){
              if(signTime.length() > 0){
                if(deduct_signintime.compareTo(signTime) < 0){
                  flow_signInTime = deduct_signintime;
                }
              }else{
                flow_signInTime = deduct_signintime;
              }
            }
            if(flow_signInTime.length() > 0){
              flowSignInTimeIndx = kqTimesArrayComInfo.getArrayindexByTimes(flow_signInTime);
            }
            if(kqDate.compareTo(signDate) < 0) {
              signInTimeIndx += 1440;
              flowSignInTimeIndx += 1440;
            }else if(kqDate.compareTo(signDate) > 0){
              signInTimeIndx -= 1440;
              signInTimeIndx  = signInTimeIndx < 0 ? 0 : signInTimeIndx;
              flowSignInTimeIndx -= 1440;
              flowSignInTimeIndx  = flowSignInTimeIndx < 0 ? 0 : flowSignInTimeIndx;
            }
            if(oneSign){
              if(workBeginIdx>signInTimeIndx) {
                earlyInMins = workBeginIdx-signInTimeIndx;
              }
            }
          } else if (checkInfo.get("signType").equals("2")) {//签退
            checkOut++;
            //如果流程抵扣存在，打卡时长也存在，那么相互比较得到出勤时长和打卡时长 暂不这样处理，还是按照漏签的逻辑来处理
            if(signTime.length() > 0){
              signInTimeOutdx4Sign = kqTimesArrayComInfo.getArrayindexByTimes(signTime);
            }
            signOutId = signId;
            signOutDate = signDate;
            signOutTime = signTime;
            signInTimeOutdx = kqTimesArrayComInfo.getArrayindexByTimes(signOutTime);
            if(deduct_signofftime.length() > 0){
              if(signTime.length() > 0){
                if(deduct_signofftime.compareTo(signTime) > 0){
                  flow_signOutTime = deduct_signofftime;
                }
              }else{
                flow_signOutTime = deduct_signofftime;
              }
            }
            if(flow_signOutTime.length() > 0){
              signInTimeOutdx = kqTimesArrayComInfo.getArrayindexByTimes(flow_signOutTime);
            }
            if(kqDate.compareTo(signDate) < 0){
              signInTimeOutdx+=1440;
            }else if(kqDate.compareTo(signDate) > 0){
              signInTimeOutdx -= 1440;
              signInTimeOutdx  = signInTimeOutdx < 0 ? 0 : signInTimeOutdx;
            }
            if(oneSign){
              if(signInTimeOutdx>workEndIdx) {
                lateOutMins = signInTimeOutdx-workEndIdx;
              }
            }
          }
          if (checkInfo.get("signType").equals("1")) {//签到
            if(signTime.length() > 0){
              String signMinTime = signTime.substring(0,5)+":00";
              endIdx = kqTimesArrayComInfo.getArrayindexByTimes(signTime);
              if(signTime.compareTo(signMinTime) > 0){
                //如果签到时间是带秒的且是迟到，那么签到时间多一秒和多一分钟是一样的
                endIdx += 1;
                signInTimeIndx = signInTimeIndx + 1;//如果是带秒的打卡数据不应该影响流程抵扣的数据的下标
              }
              if(kqDate.compareTo(signDate) < 0){
                endIdx+=1440;
              }else if(kqDate.compareTo(signDate) > 0){
                endIdx -= 1440;
                endIdx  = endIdx < 0 ? 0 : endIdx;
              }
              if (endIdx > workBeginIdx) {
                if(flow_signInTime.length() > 0){
                  if(flowSignInTimeIndx > workBeginIdx){
                    //增加一个判断，流程抵扣打卡如果开启了并且有抵扣上班打卡，那么也就不是迟到了
                    Arrays.fill(dayMins, workBeginIdx, endIdx, 2);//迟到时段标识 2
                  }
                }else{
                  Arrays.fill(dayMins, workBeginIdx, endIdx, 2);//迟到时段标识 2
                }
              }
            }
          } else if (checkInfo.get("signType").equals("2")) {//签退
            if(signTime.length() > 0){
              beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(signTime);
              if(StringUtils.isNotBlank(signDate) && signDate.compareTo(kqDate) > 0){
                beginIdx+=1440;
              }else if(kqDate.compareTo(signDate) > 0){
                beginIdx -= 1440;
                beginIdx  = beginIdx < 0 ? 0 : beginIdx;
              }
              if (workEndIdx > beginIdx) {
                if(flow_signOutTime.length() > 0){
                  if (workEndIdx > signInTimeOutdx) {
                    //增加一个判断，流程抵扣打卡如果开启了并且有抵扣下班打卡，那么也就不是早退了
                    Arrays.fill(dayMins, beginIdx, workEndIdx, 3);//早退时段标识 3
                  }
                }else{

                  Arrays.fill(dayMins, beginIdx, workEndIdx, 3);//早退时段标识 3
                }
              }
            }
          }
        }

        //打卡时长=签退时间-签到时间(有签到签退才计算)
        if(checkIn==1&&checkOut==1){
          if(signInTimeIndx4Sign > -1 && signInTimeOutdx4Sign > -1){
            if(DateUtil.dayDiff(signInDate,signOutDate)==0){//同一天签到和签退
              signMins=signInTimeOutdx4Sign - signInTimeIndx4Sign;
            }else if(DateUtil.dayDiff(signInDate,signOutDate)==1) {//第一天签到，第二天签退
              if(signInTimeOutdx4Sign<signInTimeIndx4Sign){
                signMins = 1440 + signInTimeOutdx4Sign - signInTimeIndx4Sign;
              }else{
                signMins = signInTimeOutdx4Sign - signInTimeIndx4Sign;
              }
            }
          }else{
            signMins=0;
          }
          if(signMins<0){
            signMins=0;
          }
        }

        if (checkIn == 0 && checkOut == 0) {//旷工(无签到无签退)
          if (workEndIdx > workBeginIdx) {
            Arrays.fill(dayMins, workBeginIdx, workEndIdx, 4);//旷工时段标识 4
          }
        }

        if (checkOut == 0 && checkIn > 0) {//漏签(有签到无签退)
          if(signInTimeIndx > -1){
            if (workEndIdx > signInTimeIndx) {
              //漏签就是从本次时段内的打卡到下班点
              //上班漏签应该是从签到到签到结束时间，不过这里可以不用管，只是一个次数
              Arrays.fill(dayMins, signInTimeIndx, workEndIdx, 6);//上班漏签时段标识 6
            } else {
              //签到晚于本次时段结束时间，也算漏签
              forgotCheckMins++;
            }
          }else if(flowSignInTimeIndx > -1){
            if (workEndIdx > flowSignInTimeIndx) {
              //漏签就是从本次时段内的打卡到下班点
              //上班漏签应该是从签到到签到结束时间，不过这里可以不用管，只是一个次数
              Arrays.fill(dayMins, flowSignInTimeIndx, workEndIdx, 6);//上班漏签时段标识 6
            } else {
              //签到晚于本次时段结束时间，也算漏签
              forgotCheckMins++;
            }
          }
        }

        if (checkIn == 0 && checkOut > 0) {//漏签(有签退无签到)
          if(signInTimeOutdx > 0){
            if(workBeginIdx < signInTimeOutdx) {
              //下班漏签应该是从签退到签退开始时间，不过这里可以不用管，只是一个次数
              Arrays.fill(dayMins, workBeginIdx, signInTimeOutdx, 66);//下班漏签时段标识 66，66呼应前面的漏签的6
            }else{
              //这种数据理论上不会存在，也记下吧
              forgotBeginWorkCheckMins++;
            }
          }
        }

        if (workFlowInfo.get(dateKey) != null) {
          workFlow = (List<Object>) workFlowInfo.get(dateKey);
        }

        for (int j = 0; workFlow != null && j < workFlow.size(); j++) {
          Map<String, Object> data = (Map<String, Object>) workFlow.get(j);
          String flowType = Util.null2String(data.get("flowtype"));
          String newLeaveType = Util.null2String(data.get("newleavetype"));
          String signtype = Util.null2String(data.get("signtype"));
          String serial = Util.null2String(data.get("serial"));
          String requestId = Util.null2String(data.get("requestId"));
          beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("begintime")));
          endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("endtime")));
          if (beginIdx >= endIdx) {
            continue;
          }

          if(flowType.equals(FlowReportTypeEnum.EVECTION.getFlowType())){
            Arrays.fill(dayMins, beginIdx, endIdx, 7);//出差抵扣时段标识 7
          }else if(flowType.equals(FlowReportTypeEnum.OUT.getFlowType())){
            Arrays.fill(dayMins, beginIdx, endIdx, 8);//公出抵扣时段标识 8
          }else if(flowType.equalsIgnoreCase(FlowReportTypeEnum.LEAVE.getFlowType())){
            if (endIdx > beginIdx) {
              Arrays.fill(dayMins, beginIdx, endIdx, 5);//流程抵扣时段标识 5
              int tmpBeginIdx = beginIdx;
              int tmpEndIdx = endIdx;
              Integer val = 0;
              if(leaveInfo.get(newLeaveType)==null){
                leaveInfo.put(newLeaveType,val);
              }else{
                val = (Integer) leaveInfo.get(newLeaveType);
              }

              if(beginIdx<workBeginIdx)tmpBeginIdx=workBeginIdx;
              if(endIdx>workEndIdx)tmpEndIdx=endIdx;
              if(tmpEndIdx>tmpBeginIdx){
                leaveInfo.put(newLeaveType,val+(tmpEndIdx-tmpBeginIdx));
              }
            }
          }else{
            if (endIdx > beginIdx) {
              Arrays.fill(dayMins, beginIdx, endIdx, 99);//异常流程抵扣时段标识99
            }
          }
        }


        if (workEndTimeAcross && false) {//跨天需要加入一天的流程
          workFlow = null;
          if (workFlowInfo.get(nextDateKey) != null) {
            workFlow = (List<Object>) workFlowInfo.get(nextDateKey);
          }

          for (int j = 0; workFlow != null && j < workFlow.size(); j++) {
            Map<String, Object> data = (Map<String, Object>) workFlow.get(j);
            String flowType = Util.null2String(data.get("flowtype"));
            beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("begintime")))+1440;
            endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("endtime")))+1440;
            if(endIdx>=2880){
              endIdx = 2880;
            }
            if(flowType.equals(FlowReportTypeEnum.EVECTION.getFlowType())){
              Arrays.fill(dayMins, beginIdx, endIdx, 7);//出差抵扣时段标识 7
            }else if(flowType.equals(FlowReportTypeEnum.OUT.getFlowType())){
              Arrays.fill(dayMins, beginIdx, endIdx, 8);//公出抵扣时段标识 8
            }else if(flowType.equalsIgnoreCase(FlowReportTypeEnum.LEAVE.getFlowType())){
              if (endIdx > beginIdx) {
                Arrays.fill(dayMins, beginIdx, endIdx, 5);//流程抵扣时段标识 5

              }
            }else{
              if (endIdx > beginIdx) {
                Arrays.fill(dayMins, beginIdx, endIdx, 99);//异常流程抵扣时段标识99
              }
            }
          }
        }

        if(lsRestTime != null && !lsRestTime.isEmpty()){
          for(int k = 0 ; k < lsRestTime.size(); k++){
            TimeScopeEntity restTimeScope = lsRestTime.get(k);
            if (restTimeScope!=null) {
              String restBeginTime = Util.null2String(restTimeScope.getBeginTime());
              String restEndTime = Util.null2String(restTimeScope.getEndTime());
              beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(restBeginTime);
              endIdx = kqTimesArrayComInfo.getArrayindexByTimes(restEndTime);
              if (endIdx > beginIdx) {
                Arrays.fill(dayMins, beginIdx, endIdx, -11);//休息时间
              }
            }
          }
          int all_rest_cnt = kqTimesArrayComInfo.getCnt(dayMins, workBeginIdx, workEndIdx, -11);
          if(all_rest_cnt >= 0){
            workMins = workMins-(all_rest_cnt);
          }
        }

        for (int j = workBeginIdx; j < workEndIdx; j++) {
          switch (dayMins[j]) {
            case 1:
              tmpAttendanceMins++;
              break;
            case 2:
              beLateMins++;
              break;
            case 3:
              leaveEarlyMins++;
              break;
            case 4:
              absenteeismMins++;
              break;
            case 5:
              leaveMins++;
              break;
            case 6:
              forgotCheckMins++;
              break;
            case 7:
              evectionMins++;
              break;
            case 8:
              outMins++;
              break;
            case 66:
              forgotBeginWorkCheckMins++;
              break;
            case 99:
              otherMins++;
              break;
            default:
              break;
          }
        }

        if(forgotCheckMins == 1 && beLateMins==0 && tmpAttendanceMins==0){//forgotCheckMins==1表示下班后漏签，不是迟到，流程已完全抵扣异常
          forgotCheckMins = 0;
        }

        KQShiftRuleEntity kqShiftRuleEntity = new KQShiftRuleEntity();
        kqShiftRuleEntity.setUserId(userId);
        kqShiftRuleEntity.setKqDate(kqDate);
        kqShiftRuleEntity.setBelatemins(beLateMins);
        kqShiftRuleEntity.setLeaveearlymins(leaveEarlyMins);
        kqShiftRuleEntity.setAbsenteeismmins(absenteeismMins);
        kqShiftRuleEntity.setForgotcheckmins(forgotCheckMins);
        kqShiftRuleEntity.setForgotBeginWorkCheckMins(forgotBeginWorkCheckMins);
        kqShiftRuleEntity.setEarlyInMins(earlyInMins);
        kqShiftRuleEntity.setLateOutMins(lateOutMins);
        kqLog.info("人性化规则处理前数据" + JSONObject.toJSONString(kqShiftRuleEntity));
        if(this.writeLog) {
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005301,weaver.general.ThreadVarLanguage.getLang())+"",kqShiftRuleEntity);
        }
        //人性化规则
        kqShiftRuleEntity = kqFormatShiftRule.doShiftRule(workTime,kqShiftRuleEntity);
        kqLog.info("人性化规则处理后数据" + JSONObject.toJSONString(kqShiftRuleEntity));
        if(this.writeLog) {
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005302,weaver.general.ThreadVarLanguage.getLang())+"",kqShiftRuleEntity);
        }
        beLateMins = kqShiftRuleEntity.getBelatemins();
        graveBeLateMins = kqShiftRuleEntity.getGravebelatemins();
        leaveEarlyMins = kqShiftRuleEntity.getLeaveearlymins();
        graveLeaveEarlyMins = kqShiftRuleEntity.getGraveleaveearlymins();
        absenteeismMins = kqShiftRuleEntity.getAbsenteeismmins();
        forgotCheckMins = kqShiftRuleEntity.getForgotcheckmins();
        forgotBeginWorkCheckMins = kqShiftRuleEntity.getForgotBeginWorkCheckMins();

        boolean isondutyfreecheck =false;
        boolean isoffdutyfreecheck =false;
        Map<String, String> model_ShiftRule = kqFormatShiftRule.getModel_ShiftRule(i, workTime.getSerialId());
        Iterator iter = model_ShiftRule.entrySet().iterator();
        while (iter.hasNext()) {
          Map.Entry entry = (Map.Entry) iter.next();
          String key = Util.null2String(entry.getKey());
          String value = Util.null2String(entry.getValue());
          if(key.equals("start")&&value.equals("1")){
            isondutyfreecheck = true;
          }
          if(key.equals("end")&&value.equals("1")){
            isoffdutyfreecheck = true;
          }
        }
        boolean beforeBegin = !new KQFormatBiz().needCal(workBeginDate,workBeginTime);
        if(beforeBegin) {//还未到上班时间，不用计算任何状态
          kqLog.writeLog("还未到上班时间，不用计算任何状态");
          beLateMins = 0;
          graveBeLateMins = 0;
          leaveEarlyMins = 0;
          graveLeaveEarlyMins = 0;
          absenteeismMins = 0;
          forgotCheckMins = 0;
          forgotBeginWorkCheckMins = 0;
        }else if(!new KQFormatBiz().needCal(workEndDate,workEndTime)) {//还未到下班时间
          kqLog.writeLog("还未到上班时间");
          leaveEarlyMins = 0;
          graveLeaveEarlyMins = 0;
          forgotCheckMins = 0;
        }

        if (workTime.getIsExclude()) {//无需考勤人员没有异常状态
          beLateMins = 0;
          graveBeLateMins = 0;
          leaveEarlyMins = 0;
          graveLeaveEarlyMins = 0;
          absenteeismMins = 0;
          forgotCheckMins = 0;
          forgotBeginWorkCheckMins = 0;
        }
        //允许下班不打卡   ,如果上班也没有打卡，那么算漏签
        if (isoffdutyfreecheck) {
          if (checkIn == 0) {//(无签到),就会有上班漏签，如果有下班卡，那么上班漏签
            if(forgotBeginWorkCheckMins==0&&absenteeismMins>0){
              forgotBeginWorkCheckMins= absenteeismMins ;
            }
          }
          absenteeismMins = 0;
          forgotCheckMins = 0;
        }
        //允许上班不打卡，如果下班也没有打卡，那么算漏签
        if(isondutyfreecheck){
          if (checkOut == 0) {//(无签退)
            if(forgotCheckMins==0&&absenteeismMins>0){
              forgotCheckMins= absenteeismMins ;
            }
          }
          absenteeismMins = 0;
          forgotBeginWorkCheckMins = 0;
        }
        if(isondutyfreecheck&&isoffdutyfreecheck){
          absenteeismMins = 0;
          forgotCheckMins = 0;
          forgotBeginWorkCheckMins = 0;
        }
        //计算实际出勤时间(出差公出算出勤)=应出勤-旷工-请假-迟到-早退
        attendanceMins = workMins - absenteeismMins-leaveMins-beLateMins-graveBeLateMins-leaveEarlyMins-graveLeaveEarlyMins;
        // 如果没有开启"漏签是否算实际出勤"开关，则漏签不算实际出勤时长
        if("0".equals(nosign_is_absent)) {
          attendanceMins = attendanceMins-forgotCheckMins-forgotBeginWorkCheckMins;
        }
        if(beforeBegin || attendanceMins < 0) {//还未到上班时间，不用计算任何状体
          attendanceMins = 0;
        }
        kqLog.info("实际出勤计算公式" + "实际出勤=应出勤- 旷工-请假-迟到-早退  userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
        kqLog.info("实际出勤计算结果" + attendanceMins + "=" + workMins + "- " + absenteeismMins + "-" + leaveMins + "-" + (beLateMins + graveBeLateMins) + "-" + (leaveEarlyMins - graveLeaveEarlyMins)+" userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid);
        if(this.writeLog) {
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005303,weaver.general.ThreadVarLanguage.getLang())+"",""+weaver.systeminfo.SystemEnv.getHtmlLabelName(130566,weaver.general.ThreadVarLanguage.getLang())+"="+weaver.systeminfo.SystemEnv.getHtmlLabelName(132056,weaver.general.ThreadVarLanguage.getLang())+"- "+weaver.systeminfo.SystemEnv.getHtmlLabelName(20085,weaver.general.ThreadVarLanguage.getLang())+"-"+weaver.systeminfo.SystemEnv.getHtmlLabelName(670,weaver.general.ThreadVarLanguage.getLang())+"-"+weaver.systeminfo.SystemEnv.getHtmlLabelName(20081,weaver.general.ThreadVarLanguage.getLang())+"-"+weaver.systeminfo.SystemEnv.getHtmlLabelName(20082,weaver.general.ThreadVarLanguage.getLang())+"");
          logInfo.put(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005304,weaver.general.ThreadVarLanguage.getLang())+"",attendanceMins+"="+workMins+"- "+absenteeismMins+"-"+leaveMins+"-"+(beLateMins+graveBeLateMins)+"-"+(leaveEarlyMins-graveLeaveEarlyMins));
        }
        //判断当天考勤状态
//        if (beLateMins > 0) {
//          status = ButtonStatusEnum.BELATE.getStatusCode();
//        } else if (leaveEarlyMins > 0) {
//          status = ButtonStatusEnum.LEAVEERALY.getStatusCode();
//        } else if (absenteeismMins > 0) {
//          status = ButtonStatusEnum.ABSENT.getStatusCode();
//        } else if (forgotCheckMins > 0) {
//          status = ButtonStatusEnum.NOSIGN.getStatusCode();
//        } else {
//          status = ButtonStatusEnum.NORMAL.getStatusCode();
//        }

        String groupid = Util.null2String(workTime.getGroupId());
        String serialid = Util.null2String(workTime.getSerialId());
        //qc2511354
        int deduction = 0;
       if (forgotCheckMins > 0 ){
          deduction += 240;
       }
       if (forgotBeginWorkCheckMins > 0){
          deduction += 240;
       }

        attendanceMins = attendanceMins - deduction;


        params.add(userId);
        params.add(kqDate);
        params.add(groupid.length() == 0 ? null : groupid);
        params.add(serialid.length() == 0 ? null : serialid);
        params.add(i);
        params.add(workBeginDate);
        params.add(kqTimesArrayComInfo.turn48to24Time(ori_workBeginTime));
        params.add(workEndDate);
        params.add(kqTimesArrayComInfo.turn48to24Time(ori_workEndTime));
        params.add(workMins);
        params.add(signInDate);
        params.add(signInTime);
        params.add(signInId.length() == 0 ? null : signInId);
        params.add(signOutDate);
        params.add(signOutTime);
        params.add(signOutId.length() == 0 ? null : signOutId);
        kqLog.info("format in >>>>>userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid
                +":signInDate:"+signInDate+":signInTime::"+signInTime+":signOutDate:"+signOutDate+":signOutTime::"+signOutTime);
        params.add(signMins);
        params.add(attendanceMins < 0 ? 0 : attendanceMins);
        params.add(beLateMins);
        params.add(graveBeLateMins);
        params.add(leaveEarlyMins);
        params.add(graveLeaveEarlyMins);
        params.add(absenteeismMins);
        params.add(forgotCheckMins);
        params.add(leaveMins);
        params.add(JSONObject.toJSONString(leaveInfo));
        params.add(evectionMins);
        params.add(outMins);
        params.add(forgotBeginWorkCheckMins);
        params.add(JSONObject.toJSONString(otherinfo));

        Map<String,Object> definedFieldInfo = new KQFormatBiz().getDefinedField();
        String[] definedFields = Util.splitString(Util.null2String(definedFieldInfo.get("definedField")),",");
        KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
        for (int tmpIdx = 0; tmpIdx<definedFields.length; tmpIdx++) {
          String fieldname = definedFields[tmpIdx];
//          System.out.println("fieldname=="+fieldname);
          String fieldid = KQReportFieldComInfo.field2Id.get(fieldname);
          String formula = kqReportFieldComInfo.getFormula(fieldid);
          if(formula.length()==0)continue;
          String expression = formula;
          Pattern pattern = Pattern.compile("\\$\\{[^}]+\\}");
          Matcher matcher = pattern.matcher(expression);
          Map<String, Object> env = new HashMap<>();
          String keyname = "";
          while (matcher.find()) {
            String key = matcher.group(0);
            keyname = key.substring(2, key.length() - 1).trim();
            expression = matcher.replaceAll(keyname);
            env.put(keyname, keyname.equals("beLateMins")?beLateMins:leaveEarlyMins);
          }
          Expression compiledExp = AviatorEvaluator.compile(expression,true);
          String value = Util.null2String(compiledExp.execute(env));
          params.add(value);
          if(value.equals("1")) {
            params.add(keyname.equals("beLateMins") ? beLateMins : leaveEarlyMins);
          }else{
            params.add("0");
          }
        }
        kqLog.info("format in >>>>>userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid
                +":params:"+JSON.toJSONString(params));
        lsParam.add(params);
      }
    } catch (Exception e) {
      writeLog(e);
      kqLog.info(e);
    }
    return lsParam;
  }

  public void setWriteLog(boolean writeLog) {
    this.writeLog = writeLog;
  }

  public Map<String,Object> getLogInfo() {
    return logInfo;
  }

  public void formatDateByKQDate(String kqdate) {
    KQFormatBiz kqFormatBiz = new KQFormatBiz();
    kqFormatBiz.formatDateByKQDate(kqdate);
  }

  public void formatDateByGroupId(String groupid, String kqdate) {
    KQFormatBiz kqFormatBiz = new KQFormatBiz();
    kqFormatBiz.formatDateByGroupId(groupid, kqdate);
  }

  public void formatDate(String resourceid, String kqdate) {
    KQFormatBiz kqFormatBiz = new KQFormatBiz();
    kqFormatBiz.formatDate(resourceid, kqdate);
  }

  public void delFormatData(String resourceid, String kqdate) {
    KQFormatBiz kqFormatBiz = new KQFormatBiz();
    kqFormatBiz.delFormatData(resourceid, kqdate);
  }

  /**
   * 非工作日格式化考勤报表
   * @param userId
   * @param kqDate
   * @param nonlsParam
   * @param workTime
   * @param workFlowInfo
   */
  public void formatNonWork(String userId, String kqDate, List<Object> nonlsParam, WorkTimeEntity workTime, Map<String, Object> workFlowInfo) {
    String signInId = "";
    String signInDate = "";
    String signInTime = "";
    String signOutId = "";
    String signOutDate = "";
    String signOutTime = "";
    int beginIdx = 0;
    int endIdx = 0;
    int leaveMins = 0;//请假时长
    Map<String,Object> leaveInfo = new HashMap<>();//请假信息
    int evectionMins = 0;//出差时长
    int outMins = 0;//公出时长
    int otherMins = 0;//异常流程时长
    int[] dayMins = new int[2880];//一天所有分钟数
    List<Object> workFlow = null;
    String dateKey = userId + "|" + kqDate;
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

    String preDate = DateUtil.addDate(kqDate, -1);//上一天日期
    String nextDate = DateUtil.addDate(kqDate, 1);//下一天日期
    WorkTimeEntity pre_workTime = new KQWorkTime().getWorkTime(userId, preDate);
    List<TimeScopeEntity> pre_lsSignTime = new ArrayList<>();

    if (pre_workTime != null) {
      pre_lsSignTime = pre_workTime.getSignTime();//允许打卡时间
      pre_lsSignTime = pre_lsSignTime != null ? pre_lsSignTime : new ArrayList<>();
    }
    WorkTimeEntity next_workTime = new KQWorkTime().getWorkTime(userId, nextDate);
    List<TimeScopeEntity> next_lsSignTime = new ArrayList<>();

    if (next_workTime != null) {
      next_lsSignTime = next_workTime.getSignTime();//允许打卡时间
      next_lsSignTime = next_lsSignTime != null ? next_lsSignTime : new ArrayList<>();
    }

    List<Object> lsCheckInfo = new KQFormatSignData().getNonWorkSignInfo(userId,preDate,kqDate,pre_lsSignTime,next_lsSignTime);

    for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
      Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(j);
      String signStatus = Util.null2String(checkInfo.get("signStatus"));
      String signId = Util.null2String(checkInfo.get("signId"));
      String signDate = Util.null2String(checkInfo.get("signDate"));
      String signTime = Util.null2String(checkInfo.get("signTime"));
      if (checkInfo.get("signType").equals("1")) {//签到
        signInId = signId;
        signInDate = signDate;
        signInTime = signTime;
      } else if (checkInfo.get("signType").equals("2")) {//签退
        signOutId = signId;
        signOutDate = signDate;
        signOutTime = signTime;
      }
    }

    if (workFlowInfo.get(dateKey) != null) {
      workFlow = (List<Object>) workFlowInfo.get(dateKey);
    }

    for (int j = 0; workFlow != null && j < workFlow.size(); j++) {
      Map<String, Object> data = (Map<String, Object>) workFlow.get(j);
      String flowType = Util.null2String(data.get("flowtype"));
      String newLeaveType = Util.null2String(data.get("newleavetype"));
      beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("begintime")));
      endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("endtime")));
      if(flowType.equals(FlowReportTypeEnum.EVECTION.getFlowType())){
        Arrays.fill(dayMins, beginIdx, endIdx, 7);//出差抵扣时段标识 7
      }else if(flowType.equals(FlowReportTypeEnum.OUT.getFlowType())){
        Arrays.fill(dayMins, beginIdx, endIdx, 8);//公出抵扣时段标识 8
      }else if(flowType.equalsIgnoreCase(FlowReportTypeEnum.LEAVE.getFlowType())){
        if (endIdx > beginIdx) {
          Arrays.fill(dayMins, beginIdx, endIdx, 5);//流程抵扣时段标识 5
          int tmpBeginIdx = beginIdx;
          int tmpEndIdx = endIdx;
          Integer val = 0;
          if(leaveInfo.get(newLeaveType)==null){
            leaveInfo.put(newLeaveType,val);
          }else{
            val = (Integer) leaveInfo.get(newLeaveType);
          }
          if(tmpEndIdx>tmpBeginIdx){
            leaveInfo.put(newLeaveType,val+(tmpEndIdx-tmpBeginIdx));
          }
        }
      }else{
        if (endIdx > beginIdx) {
          Arrays.fill(dayMins, beginIdx, endIdx, 99);//异常流程抵扣时段标识99
        }
      }
    }

    for (int j = 0; j < 2880; j++) {
      switch (dayMins[j]) {
        case 5:
          leaveMins++;
          break;
        case 7:
          evectionMins++;
          break;
        case 8:
          outMins++;
          break;
        case 99:
          otherMins++;
          break;
        default:
          break;
      }
    }

    nonlsParam.add(userId);
    nonlsParam.add(kqDate);
    nonlsParam.add(workTime.getGroupId());
    nonlsParam.add(0);
    nonlsParam.add(signInDate);
    nonlsParam.add(signInTime);
    nonlsParam.add(signInId.length() == 0 ? null : signInId);
    nonlsParam.add(signOutDate);
    nonlsParam.add(signOutTime);
    nonlsParam.add(signOutId.length() == 0 ? null : signOutId);
    nonlsParam.add(leaveMins);
    nonlsParam.add(JSONObject.toJSONString(leaveInfo));
    nonlsParam.add(evectionMins);
    nonlsParam.add(outMins);
  }
}
