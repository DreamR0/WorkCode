package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.cmd.attendanceButton.ButtonStatusEnum;
import com.engine.kq.entity.KQShiftRuleEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import weaver.common.DateUtil;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.*;

/**
 * 格式化数据 人性化规则
 */
public class KQFormatShiftRule extends BaseBean {
  private KQLog kqLog = new KQLog();

  public KQShiftRuleEntity doShiftRule(WorkTimeEntity workTime, KQShiftRuleEntity kqShiftRuleEntity) {
    Map<String, Object> shiftRuleInfo = workTime.getShiftRuleInfo();
    //人性化处理
    if (shiftRuleInfo != null && shiftRuleInfo.size() > 0) {
      Map<String, Object> ruleDetail = (Map<String, Object>) shiftRuleInfo.get("ruleDetail");
      //允许迟到分钟数
      boolean usepermitlateminutes = Util.null2String(shiftRuleInfo.get("permitlatestatus")).equals("1");
      int permitlateminutes = Util.getIntValue(Util.null2String(shiftRuleInfo.get("permitlateminutes")), 0);
      boolean enableexcludelate = Util.null2String(shiftRuleInfo.get("enableexcludelate")).equals("1");
      //允许早退分钟数
      boolean usepermitleaveearlyminutes = Util.null2String(shiftRuleInfo.get("permitleaveearlystatus")).equals("1");
      int permitleaveearlyminutes = Util.getIntValue(Util.null2String(shiftRuleInfo.get("permitleaveearlyminutes")), 0);
      boolean enableexcludeleaveearly = Util.null2String(shiftRuleInfo.get("enableexcludeleaveearly")).equals("1");

      //严重迟到分钟数
      boolean useseriouslateminutes = Util.null2String(shiftRuleInfo.get("seriouslatestatus")).equals("1");
      int seriouslateminutes = Util.getIntValue(Util.null2String(shiftRuleInfo.get("seriouslateminutes")), 0);
      //严重早退分钟数
      boolean useseriousleaveearlyminutes = Util.null2String(shiftRuleInfo.get("seriousleaveearlystatus")).equals("1");
      int seriousleaveearlyminutes = Util.getIntValue(Util.null2String(shiftRuleInfo.get("seriousleaveearlyminutes")), 0);
      //迟到多少钟数算旷工
      boolean uselateabsentminutes = Util.null2String(shiftRuleInfo.get("lateabsentstatus")).equals("1");
      int lateabsentminutes = Util.getIntValue(Util.null2String(shiftRuleInfo.get("lateabsentminutes")), 0);
      //早退多少钟数算旷工
      boolean useleaveearlyabsentminutes = Util.null2String(shiftRuleInfo.get("leaveearlyabsentstatus")).equals("1");
      int leaveearlyabsentminutes = Util.getIntValue(Util.null2String(shiftRuleInfo.get("leaveearlyabsentminutes")), 0);
      //允许下班不打卡
      boolean isoffdutyfreecheck = Util.null2String(shiftRuleInfo.get("isoffdutyfreecheck")).equals("1");

      String userId = kqShiftRuleEntity.getUserId();
      String kqDate = kqShiftRuleEntity.getKqDate();
      int beLateMins = kqShiftRuleEntity.getBelatemins();
      int graveBeLateMins = 0;
      int leaveEarlyMins = kqShiftRuleEntity.getLeaveearlymins();
      int graveLeaveEarlyMins = 0;
      int absenteeismMins = kqShiftRuleEntity.getAbsenteeismmins();
      int forgotcheckMins = kqShiftRuleEntity.getForgotcheckmins();
      int forgotBeginWorkCheckMins = kqShiftRuleEntity.getForgotBeginWorkCheckMins();
      int earlyInMins = kqShiftRuleEntity.getEarlyInMins();
      int lateOutMins = kqShiftRuleEntity.getLateOutMins();

      List<Object> earlyinearlyout = null;//早到早走规则
      List<Object> lateinlateout = null;//晚到晚走规则
      List<Object> lateoutlatein = null;//晚走晚到规则

      //这里个性化没法处理流程数据，逻辑改为在前面直接虚拟改掉了上下班时间
      if (ruleDetail != null && ruleDetail.size() > 0 && false) {//处理人性化设置其他规则
        earlyinearlyout = (List<Object>) ruleDetail.get("earlyinearlyout");
        lateinlateout = (List<Object>) ruleDetail.get("lateinlateout");
        lateoutlatein = (List<Object>) ruleDetail.get("lateoutlatein");

        if (earlyinearlyout != null && earlyinearlyout.size() > 0 && leaveEarlyMins > 0) {
          for (int i = 0; earlyInMins > 0 && i < earlyinearlyout.size(); i++) {
            Map<String, Object> rule = (Map<String, Object>) earlyinearlyout.get(i);
            if (Util.null2String(rule.get("enable")).equals("1")) {
              int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//早到时间
              int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//允许早走时间
              if (Util.null2String(rule.get("enablesame")).equals("1")) {
                if(earlyInMins>postponetime){
                  leaveEarlyMins -= postponetime;
                  if (leaveEarlyMins < 0) leaveEarlyMins = 0;
                }else{
                  leaveEarlyMins -= earlyInMins;
                  if (leaveEarlyMins < 0) leaveEarlyMins = 0;
                }
                break;
              }else{
                if (earlyInMins >= advancetime) {
                  leaveEarlyMins -= postponetime;
                  if (leaveEarlyMins < 0) leaveEarlyMins = 0;
                  break;
                }
              }
            }
          }
        }

        if (lateinlateout != null && lateinlateout.size() > 0 && beLateMins > 0) {
          for (int i = 0; lateOutMins > 0 && i < lateinlateout.size(); i++) {
            Map<String, Object> rule = (Map<String, Object>) lateinlateout.get(i);
            if (Util.null2String(rule.get("enable")).equals("1")) {
              int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//晚到时间
              int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//需要晚走时间
              if (Util.null2String(rule.get("enablesame")).equals("1")) {
                if(lateOutMins>postponetime){
                  beLateMins -= postponetime;
                  if (beLateMins < 0) beLateMins = 0;
                }else{
                  beLateMins -= lateOutMins;
                  if (beLateMins < 0) beLateMins = 0;
                }
                break;
              }else{
                if (lateOutMins >= postponetime) {
                  beLateMins -= advancetime;
                  if (beLateMins < 0) beLateMins = 0;
                  break;
                }
              }
            }
          }
        }

        if (lateoutlatein != null && lateoutlatein.size() > 0 && beLateMins > 0) {
          int preDayLateOutMins = getPreDayLateOutMins(userId, kqDate);
          for (int i = 0; preDayLateOutMins > 0 && i < lateoutlatein.size(); i++) {
            Map<String, Object> rule = (Map<String, Object>) lateoutlatein.get(i);
            if (Util.null2String(rule.get("enable")).equals("1")) {
              int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//晚走时间
              int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//允许晚到时间

              if (Util.null2String(rule.get("enablesame")).equals("1")) {
                if(preDayLateOutMins>postponetime){
                  beLateMins -= postponetime;
                  if (beLateMins < 0) beLateMins = 0;
                }else{
                  beLateMins -= preDayLateOutMins;
                  if (beLateMins < 0) beLateMins = 0;
                }
                break;
              }else{
                if (preDayLateOutMins >= advancetime) {
                  beLateMins -= postponetime;
                  if (beLateMins < 0) beLateMins = 0;
                  break;
                }
              }
            }
          }
        }
      }

      if (beLateMins > 0) {//迟到人性化设置
        if (usepermitlateminutes) {//允许迟到分钟数
          if(permitlateminutes >= beLateMins) {
            beLateMins = 0;
          }else if(enableexcludelate){
            beLateMins = beLateMins - permitlateminutes;
          }
        }

        if (uselateabsentminutes && beLateMins >= lateabsentminutes) {//旷工
          absenteeismMins += beLateMins;
          beLateMins = 0;
          //leaveEarlyMins = 0;
          forgotcheckMins=0 ;
        } else if (useseriouslateminutes && beLateMins >= seriouslateminutes) {//严重迟到
          graveBeLateMins += beLateMins;
          //TODO  这有个问题，严重迟到了，还要不要算迟到？这两个是同时存在的吗？
          beLateMins = 0;
        }
      }

      if (leaveEarlyMins > 0) {//早退人性化设置
        if (usepermitleaveearlyminutes) {//允许早退分钟数
          if(permitleaveearlyminutes >= leaveEarlyMins){
            leaveEarlyMins = 0;
          }else if (enableexcludeleaveearly) {
            leaveEarlyMins = leaveEarlyMins - permitleaveearlyminutes;
          }
        }
        if (useleaveearlyabsentminutes && leaveEarlyMins >= leaveearlyabsentminutes) {//旷工
          absenteeismMins += leaveEarlyMins;
          //beLateMins = 0;
          leaveEarlyMins = 0;
          forgotcheckMins=0 ;
          forgotBeginWorkCheckMins = 0;
        } else if (useseriousleaveearlyminutes && leaveEarlyMins >= seriousleaveearlyminutes) {//严重早退
          graveLeaveEarlyMins += leaveEarlyMins;
          //TODO  这有个问题，严重早退了，还要不要算早退？这两个是同时存在的吗？
          leaveEarlyMins = 0;
        }
      }
      //允许下班不打卡
      if (isoffdutyfreecheck) {
        forgotcheckMins = 0;
      }

      kqShiftRuleEntity.setBelatemins(beLateMins);
      kqShiftRuleEntity.setGravebelatemins(graveBeLateMins);
      kqShiftRuleEntity.setLeaveearlymins(leaveEarlyMins);
      kqShiftRuleEntity.setGraveleaveearlymins(graveLeaveEarlyMins);
      kqShiftRuleEntity.setAbsenteeismmins(absenteeismMins);
      kqShiftRuleEntity.setForgotcheckmins(forgotcheckMins);
      kqShiftRuleEntity.setForgotBeginWorkCheckMins(forgotBeginWorkCheckMins);
    }
    return kqShiftRuleEntity;
  }

  public int getEarlyInMins(String userId, String kqDate){
    int earlyInMins = 0;
    boolean oneSign = false;
    String preDate = DateUtil.addDate(kqDate, -1);//上一天日期
    String nextDate = DateUtil.addDate(kqDate, 1);//下一天日期
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(userId, kqDate);
    List<TimeScopeEntity> lsSignTime = new ArrayList<>();
    List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
    if (workTime != null) {
      lsSignTime = workTime.getSignTime();//允许打卡时间
      lsWorkTime = workTime.getWorkTime();//工作时间
      oneSign = lsWorkTime != null && lsWorkTime.size() == 1;
    }

    if (oneSign) {
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      TimeScopeEntity signTimeScope = lsSignTime.get(0);
      TimeScopeEntity workTimeScope = lsWorkTime.get(0);
      int workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workTimeScope.getBeginTime());

      List<Object> lsCheckInfo = new KQFormatSignData().getSignInfo(userId,signTimeScope,workTimeScope,kqDate,preDate,nextDate,kqTimesArrayComInfo);
      for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
        Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(j);
        String signDate = Util.null2String(checkInfo.get("signDate"));
        String signTime = Util.null2String(checkInfo.get("signTime"));
        String deduct_signintime = Util.null2String(checkInfo.get("deduct_signintime"));
        if (checkInfo.get("signType").equals("1")) {//签到
          //有签到但是没有signTime是因为开启了流程抵扣打卡
          if(null == signTime || signTime.length()<5){
            continue;
          }else{
            writeLog("signDate:"+signDate+",signTime="+signTime+",checkInfo="+checkInfo.toString());
          }
          String signMinTime = signTime.substring(0,5)+":00";
          boolean signInWorkBeginTime = false;
          if(signTime.compareTo(signMinTime) > 0){
            //如果签到时间是带秒的且是迟到，那么签到时间多一秒和多一分钟是一样的
            signInWorkBeginTime = true;
          }
          String signInTime = signTime;
          int signInTimeIndx = kqTimesArrayComInfo.getArrayindexByTimes(signInTime);
          String flow_signInTime = "";
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
            signInTimeIndx = kqTimesArrayComInfo.getArrayindexByTimes(flow_signInTime);
          }
          if(kqDate.compareTo(signDate) < 0)signInTimeIndx+=1440;
          if(signInWorkBeginTime){
            signInTimeIndx = signInTimeIndx + 1;
          }
          if(workBeginIdx>signInTimeIndx) {
            earlyInMins = workBeginIdx-signInTimeIndx;
          }
        }
      }
    }
    return earlyInMins;
  }

  public int getLateOutMins(String userId, String kqDate){
    int lateOutMins = 0;
    boolean oneSign = false;
    String preDate = DateUtil.addDate(kqDate, -1);//上一天日期
    String nextDate = DateUtil.addDate(kqDate, 1);//下一天日期
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(userId, kqDate);
    List<TimeScopeEntity> lsSignTime = new ArrayList<>();
    List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
    if (workTime != null) {
      lsSignTime = workTime.getSignTime();//允许打卡时间
      lsWorkTime = workTime.getWorkTime();//工作时间
      oneSign = lsWorkTime != null && lsWorkTime.size() == 1;
    }

    if (oneSign) {
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      TimeScopeEntity signTimeScope = lsSignTime.get(0);
      TimeScopeEntity workTimeScope = lsWorkTime.get(0);
      int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workTimeScope.getEndTime());

      List<Object> lsCheckInfo = new KQFormatSignData().getSignInfo(userId,signTimeScope,workTimeScope,kqDate,preDate,nextDate,kqTimesArrayComInfo);
      for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
        Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(j);
        String signDate = Util.null2String(checkInfo.get("signDate"));
        String signTime = Util.null2String(checkInfo.get("signTime"));
        String deduct_signofftime = Util.null2String(checkInfo.get("deduct_signofftime"));
        if (checkInfo.get("signType").equals("2")) {//签退
          String signOutTime = signTime;
          int signInTimeOutdx = kqTimesArrayComInfo.getArrayindexByTimes(signOutTime);
          String flow_signOutTime = signOutTime;
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
          if(kqDate.compareTo(signDate) < 0)signInTimeOutdx+=1440;
          if(signInTimeOutdx>workEndIdx) {
            lateOutMins = signInTimeOutdx-workEndIdx;
          }
        }
      }
    }
    return lateOutMins;
  }

  public int getPreDayLateOutMins(String userId, String kqDate) {
    return getPreDayLateOutMins(userId, kqDate, Lists.newArrayList());
  }
  /**
   * 前一天晚走分钟数
   */
  public int getPreDayLateOutMins(String userId, String kqDate,List<String> logList) {
    int preDayLateOutMins = 0;

    boolean oneSign = false;
    KQWorkTime kqWorkTime = new KQWorkTime();
    //前一天是非工作日，往前取到工作日
    String tmpKQPreDate = DateUtil.addDate(kqDate, -1);
    for(int i=0;i<31;i++){
      WorkTimeEntity workTime = kqWorkTime.getWorkTime(userId, tmpKQPreDate);
      if(workTime.getWorkMins()>0){
        kqDate = DateUtil.addDate(tmpKQPreDate, 1);
        break;
      }
      tmpKQPreDate = DateUtil.addDate(tmpKQPreDate, -1);
    }
    String kqPreDate =  DateUtil.addDate(kqDate, -1);;
    String kqPrePreDate = DateUtil.addDate(kqDate, -2);
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(userId, kqPreDate);
    List<TimeScopeEntity> lsSignTime = new ArrayList<>();
    List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
    if (workTime != null) {
      lsSignTime = workTime.getSignTime();//允许打卡时间
      lsWorkTime = workTime.getWorkTime();//工作时间
      oneSign = lsWorkTime != null && lsWorkTime.size() == 1;
    }

    if (oneSign) {
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      TimeScopeEntity signTimeScope = lsSignTime.get(0);
      TimeScopeEntity workTimeScope = lsWorkTime.get(0);
      String signBeginDateTime = signTimeScope.getBeginTimeAcross() ? kqPrePreDate : kqPreDate;
      signBeginDateTime += " " + kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime()) + ":00";
      String signEndDateTime = signTimeScope.getEndTimeAcross() ? kqDate : kqPreDate;
      signEndDateTime += " " + kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime()) + ":00";
      String workBeginDateTime = workTimeScope.getBeginTimeAcross() ? kqPrePreDate : kqPreDate;
      workBeginDateTime += " " + kqTimesArrayComInfo.turn48to24Time(workTimeScope.getBeginTime()) + ":00";
      String workEndDateTime = workTimeScope.getEndTimeAcross() ? kqDate : kqPreDate;
      workEndDateTime += " " + kqTimesArrayComInfo.turn48to24Time(workTimeScope.getEndTime()) + ":00";

      Map<String, String> shifRuleMap = Maps.newHashMap();
      String shif_workEndTime = "";
      getPre_ShiftRuleInfo(kqPreDate,workTime,lsSignTime,lsWorkTime,userId,shifRuleMap,logList);
      if(!shifRuleMap.isEmpty()){
        if(shifRuleMap.containsKey("shift_endworktime")){
          String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
          if(shift_endworktime.length() > 0){
            shif_workEndTime = Util.null2String(shift_endworktime);
          }
        }
      }

      List<Object> lsCheckInfo = new KQFormatSignData().getSignInfo(userId,signTimeScope,workTimeScope,kqPreDate,kqPrePreDate,kqDate,kqTimesArrayComInfo);
      Map<String, Object> checkInfo = null;
      if (lsCheckInfo.size() == 2) {
        for(int i = 0 ;i < lsCheckInfo.size() ;i++){
			checkInfo = (Map<String, Object>) lsCheckInfo.get(i);
			String signType = Util.null2String(checkInfo.get("signType"));
			if("2".equalsIgnoreCase(signType)){
				break;
			}
		}
        String signDate = Util.null2String(checkInfo.get("signDate"));
        String signTime = Util.null2String(checkInfo.get("signTime"));
        String workEndTime = Util.null2String(workTimeScope.getEndTime());
        int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
        if(shif_workEndTime.length() > 0){
          workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(shif_workEndTime);
        }
        if (!kqPreDate.equals(signDate)) { //跨天
          signTime = kqTimesArrayComInfo.turn24to48Time(signTime);
        }
        int signOutTimeIdx = kqTimesArrayComInfo.getArrayindexByTimes(signTime);
        if (signOutTimeIdx > workEndIdx) {
          preDayLateOutMins = signOutTimeIdx - workEndIdx;
          logList.add("签退时间是:"+(signDate+" "+kqTimesArrayComInfo.turn48to24Time(signTime))+",晚走了"+preDayLateOutMins+"分钟");
        }
      }else if(!lsCheckInfo.isEmpty()){
        for(int i = 0 ;i < lsCheckInfo.size() ;i++){
          Map<String, Object> checkInfoMap = (Map<String, Object>) lsCheckInfo.get(i);
          String signType = Util.null2String(checkInfoMap.get("signType"));
          if("2".equalsIgnoreCase(signType)){
            String signDate = Util.null2String(checkInfoMap.get("signDate"));
            String signTime = Util.null2String(checkInfoMap.get("signTime"));
            String workEndTime = Util.null2String(workTimeScope.getEndTime());
            int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
            if(shif_workEndTime.length() > 0){
              workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(shif_workEndTime);
            }
            if (!kqPreDate.equals(signDate)) { //跨天
              signTime = kqTimesArrayComInfo.turn24to48Time(signTime);
            }
            int signOutTimeIdx = kqTimesArrayComInfo.getArrayindexByTimes(signTime);
            if (signOutTimeIdx > workEndIdx) {
              preDayLateOutMins = signOutTimeIdx - workEndIdx;
              logList.add(",签退时间是:"+(signDate+" "+signTime)+",晚走了"+preDayLateOutMins+"分钟");
            }
          }
        }
      }
    }
    return preDayLateOutMins;
  }

  /**
   * 获取前一天的弹性时间
   * @param kqPreDate
   * @param workTime
   * @param lsSignTime
   * @param lsWorkTime
   * @param userId
   * @param logList
   */
  public void getPre_ShiftRuleInfo(String kqPreDate, WorkTimeEntity workTime,
      List<TimeScopeEntity> lsSignTime, List<TimeScopeEntity> lsWorkTime, String userId,
      Map<String, String> shifRuleMap,List<String> logList) {
    ShiftInfoBean shiftInfoBean = new ShiftInfoBean();
    shiftInfoBean.setSplitDate(kqPreDate);
    shiftInfoBean.setShiftRuleMap(workTime.getShiftRuleInfo());
    shiftInfoBean.setSignTime(lsSignTime);
    shiftInfoBean.setWorkTime(lsWorkTime);
    KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean, userId, shifRuleMap,false,logList);
  }
}