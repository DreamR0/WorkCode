package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.kq.report.biz.KqSignCusBiz;
import com.api.integration.Base;
import com.engine.kq.bean.KQHrmScheduleSign;
import com.engine.kq.biz.*;
import com.engine.kq.biz.chain.cominfo.HalfShiftComIndex;
import com.engine.kq.biz.chain.cominfo.ShiftInfoCominfoBean;
import com.engine.kq.biz.chain.duration.WorkHalfUnitSplitChain;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.enums.FlowReportTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import weaver.common.DateUtil;
import weaver.general.BaseBean;
import weaver.general.Util;

/**
 * 考勤班次个性化设置
 */
public class KQShiftRuleInfoBiz {

  public static void getShiftRuleInfo(ShiftInfoBean shiftInfoBean, String resourceId,Map<String,String> shifRuleMap,List<String> logList) {
    getShiftRuleInfo(shiftInfoBean, resourceId, shifRuleMap, true,logList);
  }

  public static void getShiftRuleInfo(ShiftInfoBean shiftInfoBean, String resourceId,Map<String,String> shifRuleMap) {
    getShiftRuleInfo(shiftInfoBean, resourceId, shifRuleMap, true);
  }

  public static void getShiftRuleInfo(ShiftInfoBean shiftInfoBean, String resourceId,Map<String,String> shifRuleMap,boolean need_lateoutlatein) {
    getShiftRuleInfo(shiftInfoBean, resourceId, shifRuleMap, need_lateoutlatein,
        Lists.newArrayList());
  }

  private static Map<String,Object> flowingData = null;//流程表单数据

  private static boolean isCountLateoutlatein = true;//是否计算晚到晚走
  /**
   * 根据班次获取个性化设置 这个方法改逻辑的话 ，记得要把do4ShiftRule也一起改
   * @param shiftInfoBean
   * @param resourceId
   * @param need_lateoutlatein 是否需要处理晚走晚到
   */
  public static void getShiftRuleInfo(ShiftInfoBean shiftInfoBean, String resourceId,Map<String,String> shifRuleMap,boolean need_lateoutlatein,List<String> logList) {
    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter minFormatter = DateTimeFormatter.ofPattern("HH:mm");
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
    List<Object> earlyinearlyout = null;//早到早走规则
    List<Object> lateinlateout = null;//晚到晚走规则
    List<Object> lateoutlatein = null;//晚走晚到规则
    new BaseBean().writeLog("=====hb=========shiftInfoBean:"+(shiftInfoBean == null ? null : JSONObject.toJSONString(shiftInfoBean)));
    if(shiftInfoBean == null){
      return ;
    }

    Map<String, Object> shiftRuleMap = shiftInfoBean.getShiftRuleMap();
    if (shiftRuleMap != null && !shiftRuleMap.isEmpty() && shiftRuleMap.containsKey("ruleDetail")) {//处理人性化设置其他规则
      //允许迟到分钟数
      boolean usepermitlateminutes = Util.null2String(shiftRuleMap.get("permitlatestatus")).equals("1");
      //和蔡志军讨论，优先满足弹性，即如果晚到了，开启了弹性，那么就先按照弹性来处理，不满足弹性的部分再按照允许迟到来处理
      usepermitlateminutes = false;
      int permitlateminutes = Util.getIntValue(Util.null2String(shiftRuleMap.get("permitlateminutes")), 0);
//      //允许早退分钟数
//      boolean usepermitleaveearlyminutes = Util.null2String(shiftRuleMap.get("permitleaveearlystatus")).equals("1");
//      int permitleaveearlyminutes = Util.getIntValue(Util.null2String(shiftRuleMap.get("permitleaveearlyminutes")), 0);

      Map<String, Object> ruleDetail = (Map<String, Object>) shiftRuleMap.get("ruleDetail");
      if(ruleDetail != null && !ruleDetail.isEmpty()){
        earlyinearlyout = (List<Object>) ruleDetail.get("earlyinearlyout");
        lateinlateout = (List<Object>) ruleDetail.get("lateinlateout");
        lateoutlatein = (List<Object>) ruleDetail.get("lateoutlatein");
        String splitDate = shiftInfoBean.getSplitDate();
        String preDate = DateUtil.addDate(splitDate, -1);//上一天日期
        String nextDate = DateUtil.addDate(splitDate, 1);//下一天日期
        KQWorkTime kqWorkTime = new KQWorkTime();
        //如果设置了个性化规则，需要先获取下打卡数据
        List<TimeScopeEntity> lsSignTime = new ArrayList<>();
        List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
        List<TimeScopeEntity> lsRestTime = new ArrayList<>();

        lsSignTime = shiftInfoBean.getSignTime();//允许打卡时间
        lsWorkTime = shiftInfoBean.getWorkTime();//工作时间

        //只有一次签到签退才有个性化的这些设置
        if(lsWorkTime != null && lsWorkTime.size() > 0){
          //早到早走后，引起下班时间变化后的时间
          LocalDateTime early_localEndDateTime = null;
          //早到早走后，引起上班时间变化后的时间
          LocalDateTime early_localBeginDateTime = null;
          //晚到晚走后，引起下班时间变化后的时间
          LocalDateTime late_localEndDateTime = null;
          //晚到晚走后，引起上班时间变化后的时间
          LocalDateTime late_localBeginDateTime = null;
          //晚走晚到后，引起上班时间变化后的时间
          LocalDateTime latein_localBeginDateTime = null;

          TimeScopeEntity signTimeScope = lsSignTime.get(0);
          TimeScopeEntity workTimeScope = lsWorkTime.get(0);

          String signInDate = "";
          String signInTime = "";
          String signOutDate = "";
          String signOutTime = "";
          String workbeigintime = workTimeScope.getBeginTime();
          boolean beginTimeAcross = workTimeScope.getBeginTimeAcross();
          String workbegindate = splitDate;
          if(beginTimeAcross){
            workbegindate = nextDate;
            workbeigintime = kqTimesArrayComInfo.turn48to24Time(workbeigintime);
          }
          String workbegindatetime = workbegindate+" "+workbeigintime+":00";
          LocalDateTime localBeginDateTime = LocalDateTime.parse(workbegindatetime,fullFormatter);

          String workendtime = workTimeScope.getEndTime();
          boolean endTimeAcross = workTimeScope.getEndTimeAcross();
          String workenddate = splitDate;
          if(endTimeAcross){
            workenddate = nextDate;
            workendtime = kqTimesArrayComInfo.turn48to24Time(workendtime);
          }
          String workenddatetime = workenddate+" "+workendtime+":00";
          LocalDateTime localEndDateTime = LocalDateTime.parse(workenddatetime,fullFormatter);

          List<Object> lsCheckInfo = new KQFormatSignData().getSignInfo(resourceId,signTimeScope,workTimeScope,splitDate,preDate,nextDate,kqTimesArrayComInfo);
          if(lsCheckInfo.isEmpty() && false){//新增人性化规则支持流程抵扣后，不需要此验证
            //如果当天没有打卡数据，判断下是否存在昨天有打卡数据，晚走晚到的情况
            if (lateoutlatein != null && lateoutlatein.size() > 0) {
            }else{
              return ;
            }
          }


          new BaseBean().writeLog("==zj==(KQShiftRuleInfoBiz--lsCheckInfo)" + JSON.toJSONString(lsCheckInfo));

           if(lsCheckInfo.size() > 1){
             for(int i = 0 ; i < lsCheckInfo.size() ; i++){
               Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(i);
               if (checkInfo.get("signType").equals("1")) {
                 //签到
                 signInDate = Util.null2String(checkInfo.get("signDate"));
                 signInTime = Util.null2String(checkInfo.get("signTime"));
                 //==zj 处理触发晚到晚走的签到时间
                 //多个判断不为空
                 if (signInTime.length() >= 5){
                   signInTime = signInTime.substring(0,5)+":00";
                   new BaseBean().writeLog("==zj==(晚到晚走signtime)" +signInTime);
                 }

               }
               if (checkInfo.get("signType").equals("2")) {
                 //签退
                 signOutDate = Util.null2String(checkInfo.get("signDate"));
                 signOutTime = Util.null2String(checkInfo.get("signTime"));
               }
             }
           }else if(lsCheckInfo.size() == 1){
             Map<String, Object> checkInfo = (Map<String, Object>) lsCheckInfo.get(0);
             if (checkInfo.get("signType").equals("1")) {
               //签到
               signInDate = Util.null2String(checkInfo.get("signDate"));
               signInTime = Util.null2String(checkInfo.get("signTime"));
             }
             if (checkInfo.get("signType").equals("2")) {
               //签退
               signOutDate = Util.null2String(checkInfo.get("signDate"));
               signOutTime = Util.null2String(checkInfo.get("signTime"));
             }
           }



          if (need_lateoutlatein && lateoutlatein != null && lateoutlatein.size() > 0) {

            String log_str = "";
            KQFormatShiftRule kqFormatShiftRule = new KQFormatShiftRule();
            int preDayLateOutMins = kqFormatShiftRule.getPreDayLateOutMins(resourceId, splitDate,logList);
            if(!logList.isEmpty()){
              for(int i = 0 ; i < logList.size() ; i++){
                if(i == 0){
                  log_str += ""+logList.get(i);
                }else{
                  log_str += ","+logList.get(i);
                }
              }
              logList.clear();
            }
            for (int i = 0; preDayLateOutMins > 0 && i < lateoutlatein.size(); i++) {
              Map<String, Object> rule = (Map<String, Object>) lateoutlatein.get(i);
              if (Util.null2String(rule.get("enable")).equals("1")) {
                int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//晚走时间
                int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//允许晚到时间

                boolean enablesame = Util.null2String(rule.get("enablesame")).equals("1");
                if(log_str.length() > 0){
                  log_str += ","+(" "+"开启了晚走晚到,"+(enablesame?"相对时间,前一天晚走，第二天可以晚到，弹性限额"+postponetime+"分钟":"绝对时间"+",前一天晚走"+advancetime+"分钟,第二天上班可以晚到"+postponetime+"分钟"));
                }else{
                  log_str += ""+(" "+"开启了晚走晚到,"+(enablesame?"相对时间,前一天晚走，第二天可以晚到，弹性限额"+postponetime+"分钟":"绝对时间"+",前一天晚走"+advancetime+"分钟,第二天上班可以晚到"+postponetime+"分钟"));
                }
                if (enablesame) {
                  if(preDayLateOutMins >= advancetime){
                    //如果昨日的晚走时长已经完全大于了设置的晚走时长,那么就可以晚到整个postponetime
                    latein_localBeginDateTime = localBeginDateTime.plusMinutes(postponetime);
                  }else{
                    latein_localBeginDateTime = localBeginDateTime.plusMinutes(preDayLateOutMins);
                  }
                  break;
                }else{
                  if (preDayLateOutMins >= advancetime) {
                    //如果昨日的晚走时长已经完全大于了设置的晚走时长,那么就可以晚到整个postponetime
                    latein_localBeginDateTime = localBeginDateTime.plusMinutes(postponetime);
                    break;
                  }else{
                    logList.clear();
                  }
                }
              }else{
                logList.clear();
              }
            }
            if(latein_localBeginDateTime != null){
              shifRuleMap.put("shift_type", "lateoutlatein");
              String beginDate = latein_localBeginDateTime.format(dateFormatter);
              String beginmin = latein_localBeginDateTime.format(minFormatter);
              // 前一天的晚到影响了今天的早到或者晚到
              workbegindatetime = latein_localBeginDateTime.format(fullFormatter);
              localBeginDateTime = LocalDateTime.parse(workbegindatetime,fullFormatter);
              log_str += ","+("满足了晚走晚到,所以今天的上班时间变化了,变成了:"+workbegindatetime+"-"+workenddatetime);

              shifRuleMap.put("shift_begindate", beginDate);
              if(beginDate.compareTo(splitDate) > 0){
                shifRuleMap.put("shift_beginworktime", kqTimesArrayComInfo.turn24to48Time(beginmin));
              }else{
                shifRuleMap.put("shift_beginworktime", beginmin);
              }
            }else{
              log_str = "";
            }
            if(log_str.length() > 0){
              logList.add(log_str);
            }else{
              logList.clear();
            }
          }
          String log_str = "";
          long signIn_before_mins = 0;
          long signIn_after_mins = 0;
          long signOut_before_mins = 0;
          long signOut_after_mins = 0;

          //针对流程的特殊处理
          String flowFromDate = "";
          String flowToDate = "";
          String flowFromTime = "";
          String flowToTime = "";

          String dateKey = resourceId + "|" + splitDate;
          new BaseBean().writeLog("==========hb========6datekey:"+dateKey);
          String kqDateNext = DateUtil.addDate(splitDate, 1);
          KQFlowDataBiz kqFlowDataBiz = new KQFlowDataBiz.FlowDataParamBuilder().resourceidParam(resourceId).fromDateParam(splitDate).toDateParam(kqDateNext).build();
          Map<String,Object> workFlowInfo = new HashMap<>();//格式为 userid | date -流程信息
          List<Object> workFlow = null;
          if (true){        //后续开关加在这里
            kqFlowDataBiz.getAllFlowData(workFlowInfo,false);
            if (workFlowInfo.get(dateKey) != null){
              workFlow = (List<Object>) workFlowInfo.get(dateKey);
              new BaseBean().writeLog("==========hb========5workFlow:"+JSON.toJSONString(workFlow));
            }
          }
          Map<String, String> dateTimeMap = new HashMap<>();
          List<String> fromTimeList = new ArrayList<>();
          //将流程表单数据加入到流程数据中进行计算
          if (flowingData!=null && flowingData.size()>0){
            if (workFlow==null) {
              workFlow = new ArrayList<>();
            }
            workFlow.add(flowingData);
          }
          for (int j = 0; workFlow != null && j < workFlow.size(); j++){
            Map<String,Object> data = (Map<String, Object>) workFlow.get(j);
            String fromtime = Util.null2String(data.get("begintime"));
            String totime = Util.null2String(data.get("endtime"));
            String flowType = Util.null2String(data.get("flowtype"));
            String params = splitDate+"#"+fromtime+"#"+splitDate+"#"+totime;
            new BaseBean().writeLog("==========hb========flowType:"+flowType);
            if(flowType.equals(FlowReportTypeEnum.LEAVE.getFlowType()) ||
                    flowType.equals(FlowReportTypeEnum.OUT.getFlowType()) ||
                    flowType.equals(FlowReportTypeEnum.EVECTION.getFlowType())) {//流程类型为 请假、公出、出差
              isCountLateoutlatein = data.get("nolateoutlatein") ==null;//表单数据跨天的情况下，是否进行晚到晚走
              dateTimeMap.put(fromtime, params);
              fromTimeList.add(fromtime);
            }
          }
          if (fromTimeList.size() > 0){
            Collections.sort(fromTimeList, new Comparator<String>() {
              @Override
              public int compare(String o1, String o2) {
                return o1.compareTo(o2);
              }
            });
            if(!dateTimeMap.isEmpty()) {
              String flowDateTimeReal = dateTimeMap.get(fromTimeList.get(0));
              flowFromDate = flowDateTimeReal.split("#")[0];
              flowToDate = flowDateTimeReal.split("#")[2];
              flowFromTime = flowDateTimeReal.split("#")[1];
              flowToTime = flowDateTimeReal.split("#")[3];
            }
          }
          String flowFromDateTime = "";
          String flowToDateTime = "";
          if(!"".equals(flowFromDate) && !"".equals(flowToDate) && !"".equals(flowFromTime) && !"".equals(flowToTime)) {
            if (!splitDate.equals(flowFromDate) && !splitDate.equals(flowToDate)) {
              return;
            }
            long flowIn_after_mins = 0;
            // 如果流程开始时间在打卡时间之前，验证流程
            String signDateTime = signInDate+" "+signInTime;

            if (splitDate.equals(flowFromDate)){
              flowFromDateTime = splitDate+" "+flowFromTime+":00";
              if (flowFromDateTime.compareTo(workbegindatetime) >= 0){//流程开始时间早于打卡时间，晚于上班时间，按流程走

              }else if (flowFromDateTime.compareTo(workbegindatetime) <0 ){// 流程开始时间早于上班时间,不走弹性
                return;
              }else if(splitDate.equals(flowToDate)){
                flowFromDateTime = "";
              }
            } else if (splitDate.equals(flowToDate)){
              flowToDateTime = splitDate+" "+flowToTime+":00";
              if (flowToDateTime.compareTo(workbegindatetime) > 0){// 流程结束时间在上班时间之后，不走弹性
                return;
              }
            }
          }
          boolean isFromFlow = false;

          if(signInDate.length() > 0 && signInTime.length() > 0){
            //判断下签到和上班时间，是否存在早到或者晚到的情况的情况
            String fromDateTime = signInDate+" "+signInTime;
            String toDateTime = workbegindatetime;
            // 如果流程上的时间在打卡时间之前并且是在上班时间之后，则按流程进行班次变化
            if(!"".equals(flowFromDateTime) && flowFromDateTime.compareTo(fromDateTime) <= 0 && flowFromDateTime.compareTo(workbegindatetime) >= 0) {
              fromDateTime = flowFromDateTime;
              signInTime = flowFromTime+":00";
              isFromFlow = true;
            }
            long signIn_mins = Duration.between(LocalDateTime.parse(fromDateTime, fullFormatter), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();
            if(latein_localBeginDateTime != null){
              signIn_mins = Duration.between(LocalDateTime.parse(fromDateTime, fullFormatter), latein_localBeginDateTime).toMinutes();
            }
            boolean need_plus_one_min = false;
            new BaseBean().writeLog("fromDateTime="+fromDateTime+">>>toDateTime="+toDateTime);
            if(signInTime.length() > 6){
              String signMinTime = signInTime.substring(0,5)+":00";
              if(signInTime.compareTo(signMinTime) > 0){
                //如果签到时间是带秒的且是迟到，那么签到时间多一秒和多一分钟是一样的
                need_plus_one_min = true;
                String tmpfromDateTime = signInDate+" "+signMinTime;
                if(need_plus_one_min){
                  signIn_mins = Duration.between(LocalDateTime.parse(tmpfromDateTime, fullFormatter).plusMinutes(1), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();
                  if(latein_localBeginDateTime != null){
                    signIn_mins = Duration.between(LocalDateTime.parse(tmpfromDateTime, fullFormatter).plusMinutes(1), latein_localBeginDateTime).toMinutes();
                  }
                }
              }
            }
            if(signIn_mins > 0){
              log_str = (splitDate+" 上班时间是:"+toDateTime+",签到时间是:"+fromDateTime+",早到"+signIn_mins+"分钟");
              //确实是早到了
              signIn_before_mins = signIn_mins;
            }else if(signIn_mins < 0){
              //这属于是晚到了
              signIn_after_mins = Math.abs(signIn_mins);
              if(isFromFlow) {
                log_str = (splitDate+" "+"上班时间是:"+toDateTime+",流程开始时间是:"+fromDateTime+",晚到"+signIn_after_mins+"分钟");
              } else {
                log_str = (splitDate+" "+"上班时间是:"+toDateTime+",签到时间是:"+fromDateTime+",晚到"+signIn_after_mins+"分钟");
              }
              if (usepermitlateminutes) {//允许迟到分钟数
                signIn_after_mins = signIn_after_mins - permitlateminutes;
              }
            }
          }else { // 没有签到卡的情况下，只看流程
            new BaseBean().writeLog("==========hb========1flowFromDateTime:"+flowFromDateTime+"=workbegindatetime:"+workbegindatetime);
            if (!"".equals(flowFromDateTime) && flowFromDateTime.compareTo(workbegindatetime) >= 0){
              isFromFlow = true;
              String toDateTime = workbegindatetime;
              long flowIn_mins = Duration.between(LocalDateTime.parse(flowFromDateTime, fullFormatter), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();
              if(latein_localBeginDateTime != null){
                flowIn_mins = Duration.between(LocalDateTime.parse(flowFromDateTime, fullFormatter), latein_localBeginDateTime).toMinutes();
              }
              boolean need_plus_one_min = false;
              new BaseBean().writeLog("fromDateTime="+flowFromDateTime+">>>toDateTime="+toDateTime);
              String flowMinTime = flowFromTime.substring(0,5)+":00";
              if(flowFromTime.compareTo(flowMinTime) > 0){
                //如果签到时间是带秒的且是迟到，那么签到时间多一秒和多一分钟是一样的
                need_plus_one_min = true;
                String tmpfromDateTime = signInDate+" "+flowMinTime;
                if(need_plus_one_min){
                  flowIn_mins = Duration.between(LocalDateTime.parse(tmpfromDateTime, fullFormatter).plusMinutes(1), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();
                  if(latein_localBeginDateTime != null){
                    flowIn_mins = Duration.between(LocalDateTime.parse(tmpfromDateTime, fullFormatter).plusMinutes(1), latein_localBeginDateTime).toMinutes();
                  }
                }
              }
              if(flowIn_mins > 0){
                log_str = (splitDate+" 上班时间是:"+toDateTime+",流程开始时间是:"+flowFromDateTime+",早到"+flowIn_mins+"分钟");
                //确实是早到了
                signIn_before_mins = flowIn_mins;
              }else if(flowIn_mins < 0){
                //这属于是晚到了
                signIn_after_mins = Math.abs(flowIn_mins);
                log_str = (splitDate+" "+"上班时间是:"+toDateTime+",流程开始时间是:"+flowFromDateTime+",晚到"+signIn_after_mins+"分钟");
                if (usepermitlateminutes) {//允许迟到分钟数
                  signIn_after_mins = signIn_after_mins - permitlateminutes;
                }
              }
            }
          }

          if(signOutDate.length() > 0 && signOutTime.length() > 0){
            //判断下签退和下班时间，是否存在晚走的情况
            String fromDateTime = workenddatetime;
            String toDateTime = signOutDate+" "+signOutTime;
            long signOut_mins = Duration.between(LocalDateTime.parse(fromDateTime, fullFormatter), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();

            if(signOut_mins > 0){
              //这属于是晚走了
              signOut_after_mins = signOut_mins;
            }else if(signOut_mins < 0){
              //这属于是早退了
              signOut_before_mins = Math.abs(signOut_mins);
            }
          }

          if (earlyinearlyout != null && earlyinearlyout.size() > 0 && signIn_before_mins > 0) {
            //必须有早到时间才能继续下面的判断
            for (int i = 0; i < earlyinearlyout.size(); i++) {
              Map<String, Object> rule = (Map<String, Object>) earlyinearlyout.get(i);
              if (Util.null2String(rule.get("enable")).equals("1")) {
                int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//早到时间
                int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//允许早走时间
                boolean enablesame = Util.null2String(rule.get("enablesame")).equals("1");
                if(log_str.length() > 0){
                  log_str += ","+(" "+"开启了早到早走,"+(enablesame?"相对时间,早到可以早走，弹性限额"+postponetime+"分钟":"绝对时间"+",上班早到"+advancetime+"分钟,下班可以早走"+postponetime+"分钟"));
                }else{
                  log_str += ""+(" "+"开启了早到早走,"+(enablesame?"相对时间,早到可以早走，弹性限额"+postponetime+"分钟":"绝对时间"+",上班早到"+advancetime+"分钟,下班可以早走"+postponetime+"分钟"));
                }
                if (enablesame) {
                  //使用相对时间
                  if(signIn_before_mins >= advancetime){
                    //如果打卡的早到时长已经完全大于了设置的早到时长,那么就可以早走整个postponetime
                    early_localEndDateTime = localEndDateTime.minusMinutes(postponetime);
                    early_localBeginDateTime = localBeginDateTime.minusMinutes(advancetime);
                  }else{
                    early_localEndDateTime = localEndDateTime.minusMinutes(signIn_before_mins);
                    early_localBeginDateTime = localBeginDateTime.minusMinutes(signIn_before_mins);
                  }
                  break;
                }else{
                  if(signIn_before_mins >= advancetime){
                    //如果打卡的早到时长已经完全大于了设置的早到时长,那么就可以早走整个postponetime
                    early_localEndDateTime = localEndDateTime.minusMinutes(postponetime);
                    early_localBeginDateTime = localBeginDateTime.minusMinutes(advancetime);
                    break;
                  }
                }
              }
            }

            String new_beginDateTime = "";
            String new_endDateTime = "";
            if(early_localBeginDateTime != null){
              shifRuleMap.put("shift_type", "earlyinearlyout");
              String beginDate = early_localBeginDateTime.format(dateFormatter);
              String beginmin = early_localBeginDateTime.format(minFormatter);
              shifRuleMap.put("shift_begindate", beginDate);
              if(beginDate.compareTo(splitDate) > 0){
                shifRuleMap.put("shift_beginworktime", kqTimesArrayComInfo.turn24to48Time(beginmin));
              }else{
                shifRuleMap.put("shift_beginworktime", beginmin);
              }
              new_beginDateTime = beginDate+" "+beginmin;
            }
            if(early_localEndDateTime != null){
              shifRuleMap.put("shift_type", "earlyinearlyout");
              String endDate = early_localEndDateTime.format(dateFormatter);
              String endmin = early_localEndDateTime.format(minFormatter);
              shifRuleMap.put("shift_enddate", endDate);
              if(endDate.compareTo(splitDate) > 0){
                shifRuleMap.put("shift_endworktime", kqTimesArrayComInfo.turn24to48Time(endmin));
              }else{
                shifRuleMap.put("shift_endworktime", endmin);
              }
              new_endDateTime = endDate+" "+endmin;
            }
            if(new_beginDateTime.length() > 0 && new_endDateTime.length() > 0){
              log_str += ","+"满足了早到,所以"+splitDate+"的上班时间和下班时间变化了,上班时间变成了:"+new_beginDateTime+",下班时间变成了:"+new_endDateTime;
            }else{
              log_str += ",不满足规则,所以"+splitDate+"的上班时间和下班时间未发生变化,上班时间还是:"+workbegindatetime+",下班时间还是:"+workenddatetime;
            }
            if(log_str.length() > 0){
              logList.add(log_str);
            }

          }

          boolean isLateinlateout = false;
          new BaseBean().writeLog("==============hb==========lateinlateout:"+JSON.toJSONString(lateinlateout));
          if (lateinlateout != null && lateinlateout.size() > 0) {
             new BaseBean().writeLog("===========hb============signIn_after_mins:"+signIn_after_mins);
            if(signIn_after_mins > 0){
              isLateinlateout = true;
              //必须有晚到时间才能继续下面的判断
              for (int i = 0; i < lateinlateout.size(); i++) {
                Map<String, Object> rule = (Map<String, Object>) lateinlateout.get(i);
                if (Util.null2String(rule.get("enable")).equals("1")) {
                  int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//晚到时间
                  int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//需要晚走时间
                  boolean enablesame = Util.null2String(rule.get("enablesame")).equals("1");
                  if(isFromFlow) {
                    if(log_str.length() > 0){
                      log_str += ","+(" "+"开启了晚到晚走,"+(enablesame?"相对时间,晚到可以晚走，弹性限额"+postponetime+"分钟":"绝对时间"+",流程开始时间晚到"+advancetime+"分钟,下班需要晚走"+postponetime+"分钟"));
                    }else{
                      log_str += ""+(" "+"开启了晚到晚走,"+(enablesame?"相对时间,晚到可以晚走，弹性限额"+postponetime+"分钟":"绝对时间"+",流程开始时间晚到"+advancetime+"分钟,下班需要晚走"+postponetime+"分钟"));
                    }
                  } else {
                    if(log_str.length() > 0){
                      log_str += ","+(" "+"开启了晚到晚走,"+(enablesame?"相对时间,晚到可以晚走，弹性限额"+postponetime+"分钟":"绝对时间"+",上班晚到"+advancetime+"分钟,下班需要晚走"+postponetime+"分钟"));
                    }else{
                      log_str += ""+(" "+"开启了晚到晚走,"+(enablesame?"相对时间,晚到可以晚走，弹性限额"+postponetime+"分钟":"绝对时间"+",上班晚到"+advancetime+"分钟,下班需要晚走"+postponetime+"分钟"));
                    }
                  }

                  if (enablesame) {
                    //使用相对时间
                    new BaseBean().writeLog("===========hb========signIn_after_mins:"+signIn_after_mins+"==advancetime"+advancetime);
                    if(signIn_after_mins <= advancetime){
                      //如果打卡的晚到时长必须要小于等于设置的晚到时长，才算满足，晚到多久就可以晚走多久
                      late_localEndDateTime = localEndDateTime.plusMinutes(signIn_after_mins);
                      late_localBeginDateTime = localBeginDateTime.plusMinutes(signIn_after_mins);
                      break;
                    }else{
                      new BaseBean().writeLog("==============hb==========KQSettingsBiz.is_lateinlateout_outrule():"+KQSettingsBiz.is_lateinlateout_outrule());
                      if(KQSettingsBiz.is_lateinlateout_outrule()){
                        late_localEndDateTime = localEndDateTime.plusMinutes(advancetime);
                        late_localBeginDateTime = localBeginDateTime.plusMinutes(advancetime);
                        break;
                      }
                    }
                  }else{
                    if (signIn_after_mins <= advancetime) {
                      //如果打卡的晚到时长已经完全小于了设置的晚到时长,那么需要晚走整个postponetime
                      late_localEndDateTime = localEndDateTime.plusMinutes(postponetime);
                      late_localBeginDateTime = localBeginDateTime.plusMinutes(advancetime);
                      break;
                    }else{
                      if(KQSettingsBiz.is_lateinlateout_outrule()){
                        late_localEndDateTime = localEndDateTime.plusMinutes(postponetime);
                        late_localBeginDateTime = localBeginDateTime.plusMinutes(advancetime);
                        break;
                      }
                    }
                  }
                }
              }
            }
            if(isLateinlateout){
              String new_beginDateTime = "";
              String new_endDateTime = "";
              new BaseBean().writeLog("============hb=============late_localBeginDateTime:"+late_localBeginDateTime);
              if(late_localBeginDateTime != null){
                shifRuleMap.put("shift_type", "lateinlateout");
                String beginDate = late_localBeginDateTime.format(dateFormatter);
                String beginmin = late_localBeginDateTime.format(minFormatter);
                shifRuleMap.put("shift_begindate", beginDate);
                if(beginDate.compareTo(splitDate) > 0){
                  shifRuleMap.put("shift_beginworktime", kqTimesArrayComInfo.turn24to48Time(beginmin));
                }else{
                  shifRuleMap.put("shift_beginworktime", beginmin);
                }
                new_beginDateTime = beginDate+" "+beginmin;
              }
              if(late_localEndDateTime != null){
                shifRuleMap.put("shift_type", "lateinlateout");
                String endDate = late_localEndDateTime.format(dateFormatter);
                String endmin = late_localEndDateTime.format(minFormatter);
                shifRuleMap.put("shift_enddate", endDate);
                if(endDate.compareTo(splitDate) > 0){
                  shifRuleMap.put("shift_endworktime", kqTimesArrayComInfo.turn24to48Time(endmin));
                }else{
                  shifRuleMap.put("shift_endworktime", endmin);
                }
                new_endDateTime = endDate+" "+endmin;
              }
              if(new_beginDateTime.length() > 0 && new_endDateTime.length() > 0){
                log_str += ",满足了晚到,所以"+splitDate+"的上班时间和下班时间变化了,上班时间变成了:"+new_beginDateTime+",下班时间变成了:"+new_endDateTime;
              }else{
                log_str += ",不满足规则,所以"+splitDate+"的上班时间和下班时间未发生变化,上班时间还是:"+workbegindatetime+",下班时间还是:"+workenddatetime;
              }
              if(log_str.length() > 0){
                logList.add(log_str);
              }
            }
          }
        }
      }
    }
  }

  public void rest_workLongTimeIndex(ShiftInfoBean shiftInfoBean,
      SplitBean splitBean, List<int[]> real_workLongTimeIndex,
      KQTimesArrayComInfo kqTimesArrayComInfo,
      List<String> real_allLongWorkTime){
    boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
    if(!is_flow_humanized){
      return ;
    }
    //个性化设置只支持一次打卡的
    flowingData = KqSignCusBiz.splitFlowToShiftRuleData(splitBean,shiftInfoBean.getSplitDate());
    Map<String,String> shifRuleMap = Maps.newHashMap();
    KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean,splitBean.getResourceId(),shifRuleMap);
    if(!shifRuleMap.isEmpty()){
      if(shifRuleMap.containsKey("shift_beginworktime")){
        String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
        if(shift_beginworktime.length() > 0){
          int[] workLongTimeIndex_arr = real_workLongTimeIndex.get(0);
          workLongTimeIndex_arr[0] = kqTimesArrayComInfo.getArrayindexByTimes(shift_beginworktime);
          if(real_allLongWorkTime != null){
            real_allLongWorkTime.set(0,shift_beginworktime);
          }
        }
      }
      if(shifRuleMap.containsKey("shift_endworktime")){
        String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
        if(shift_endworktime.length() > 0){
          int[] workLongTimeIndex_arr = real_workLongTimeIndex.get(0);
          workLongTimeIndex_arr[1] = kqTimesArrayComInfo.getArrayindexByTimes(shift_endworktime);
          if(real_allLongWorkTime != null){
            real_allLongWorkTime.set(1,shift_endworktime);
          }
        }
      }
      reset_halfIndex(shiftInfoBean,real_allLongWorkTime,kqTimesArrayComInfo);
    }

  }

  public void reset_halfIndex(ShiftInfoBean shiftInfoBean, List<String> real_allLongWorkTime, KQTimesArrayComInfo kqTimesArrayComInfo) {
    try{
      List<String[]> real_workAcrossTime = shiftInfoBean.getWorkAcrossTime();
      List<int[]> shift_halfWorkIndex = shiftInfoBean.getHalfWorkIndex();
      List<int[]> real_shift_halfWorkIndex = Lists.newArrayList();

      List<Integer> eachWorkMins = Lists.newArrayList();
      List<Integer> tmp_eachWorkMins = Lists.newArrayList();
      List<int[]> workLongTimeIndex = shiftInfoBean.getWorkLongTimeIndex();
      List<int[]> restLongTimeIndex = shiftInfoBean.getRestLongTimeIndex();
      String allLongWorkBeginTime = real_allLongWorkTime.get(0);
      String allLongWorkEndTime = real_allLongWorkTime.get(1);

      String[] real_workAcrossTimes = new String[2];
      real_workAcrossTimes[0] = allLongWorkBeginTime;
      real_workAcrossTimes[1] = allLongWorkEndTime;
      if(!workLongTimeIndex.isEmpty()){
        real_workAcrossTime.set(0,real_workAcrossTimes);
      }

      int[] real_workLongTimeIndex = new int[2];
      real_workLongTimeIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(allLongWorkBeginTime);
      real_workLongTimeIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(allLongWorkEndTime);
      if(!workLongTimeIndex.isEmpty()){
        workLongTimeIndex.set(0, real_workLongTimeIndex);
      }

      int allLongWorkBeginTime_Index = kqTimesArrayComInfo.getArrayindexByTimes(allLongWorkBeginTime);
      int allLongWorkEndTime_Index = kqTimesArrayComInfo.getArrayindexByTimes(allLongWorkEndTime);
      tmp_eachWorkMins.add(allLongWorkBeginTime_Index);
      int real_workmins = allLongWorkEndTime_Index-allLongWorkBeginTime_Index;
      for(int k = 0 ; k < restLongTimeIndex.size() ; k++){
        int[] rests = restLongTimeIndex.get(k);
        if(rests != null && rests.length == 2){
          real_workmins = real_workmins - (rests[1]-rests[0]);
          tmp_eachWorkMins.add(rests[0]);
          tmp_eachWorkMins.add(rests[1]);
        }
      }
      tmp_eachWorkMins.add(allLongWorkEndTime_Index);
      for(int j = 0 ; j < tmp_eachWorkMins.size() ; ){
        int end_index = tmp_eachWorkMins.get(j+1);
        int begin_index = tmp_eachWorkMins.get(j);
        int tmp = end_index-begin_index;
        if(tmp > 0){
          eachWorkMins.add(end_index-begin_index);
        }
        j = j + 2;
      }

      Map<String,Object> workTimeMap = Maps.newHashMap();
      workTimeMap.put("halfcalrule", shiftInfoBean.getHalfcalrule());
      workTimeMap.put("halfcalpoint", shiftInfoBean.getHalfcalpoint());
      workTimeMap.put("halfcalpoint2cross", shiftInfoBean.getHalfcalpoint2cross());
      workTimeMap.put("workmins", real_workmins);

      HalfShiftComIndex halfShiftComIndex = new HalfShiftComIndex(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005308,weaver.general.ThreadVarLanguage.getLang())+"",workTimeMap);
      ShiftInfoCominfoBean shiftInfoCominfoBean = new ShiftInfoCominfoBean();
      shiftInfoCominfoBean.setAllLongWorkTime(real_allLongWorkTime);
      shiftInfoCominfoBean.setRestLongTimeIndex(shiftInfoBean.getRestLongTimeIndex());
      shiftInfoCominfoBean.setWorkAcrossTime(real_workAcrossTime);
      shiftInfoCominfoBean.setEachWorkMins(eachWorkMins);
      shiftInfoCominfoBean.setHalfWorkIndex(real_shift_halfWorkIndex);
      shiftInfoCominfoBean.setWorkLongTimeIndex(workLongTimeIndex);
      halfShiftComIndex.handleHalfTime(shiftInfoCominfoBean);
      if(!real_shift_halfWorkIndex.isEmpty()){
        shiftInfoBean.setHalfWorkIndex(real_shift_halfWorkIndex);
      }
    }catch (Exception e){
    }
  }


  /**
   * 开启了个性化设置之后，对加班数据的影响
   * @param ruleDetail
   * @param signInTimeBean
   * @param signOutTimeBean
   * @param allWorkTime
   * @param splitDate
   * @param nextday
   * @param resourceid
   */
  public static Map<String, String> do4ShiftRule(Map<String, Object> ruleDetail,
      KQHrmScheduleSign signInTimeBean, KQHrmScheduleSign signOutTimeBean,
      List<String> allWorkTime, String splitDate, String nextday, String resourceid) {
    Map<String,String> shifRuleMap = Maps.newHashMap();
    boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
    if(!is_flow_humanized){
      return shifRuleMap;
    }
    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter minFormatter = DateTimeFormatter.ofPattern("HH:mm");
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

    List<Object> earlyinearlyout = null;//早到早走规则
    List<Object> lateinlateout = null;//晚到晚走规则
    List<Object> lateoutlatein = null;//晚走晚到规则
    String begintime = allWorkTime.get(0);
    String endtime = allWorkTime.get(allWorkTime.size()-1);
    boolean isEndAcross = false;
    if(begintime.compareTo(endtime) > 0){
      //结束时间跨天了
      isEndAcross = true;
    }
    //早到早走后，引起下班时间变化后的时间
    LocalDateTime early_localEndDateTime = null;
    //早到早走后，引起上班时间变化后的时间
    LocalDateTime early_localBeginDateTime = null;
    //晚到晚走后，引起下班时间变化后的时间
    LocalDateTime late_localEndDateTime = null;
    //晚到晚走后，引起上班时间变化后的时间
    LocalDateTime late_localBeginDateTime = null;
    //晚走晚到后，引起上班时间变化后的时间
    LocalDateTime latein_localBeginDateTime = null;

    earlyinearlyout = (List<Object>) ruleDetail.get("earlyinearlyout");
    lateinlateout = (List<Object>) ruleDetail.get("lateinlateout");
    lateoutlatein = (List<Object>) ruleDetail.get("lateoutlatein");

    String workbegindatetime = splitDate+" "+begintime+":00";
    LocalDateTime localBeginDateTime = LocalDateTime.parse(workbegindatetime,fullFormatter);
    String workenddatetime = (isEndAcross ? nextday : splitDate)+" "+endtime+":00";
    LocalDateTime localEndDateTime = LocalDateTime.parse(workenddatetime,fullFormatter);

    long signIn_before_mins = 0;
    long signIn_after_mins = 0;
    long signOut_before_mins = 0;
    long signOut_after_mins = 0;
    if(signInTimeBean != null){
      String signInDate = signInTimeBean.getSigndate();
      String signInTime = signInTimeBean.getSigntime();
      //判断下签到和上班时间，是否存在早到或者晚到的情况的情况
      String fromDateTime = signInDate+" "+signInTime;
      String toDateTime = workbegindatetime;
      long signIn_mins = Duration.between(LocalDateTime.parse(fromDateTime, fullFormatter), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();

      if(signIn_mins > 0){
        //确实是早到了
        signIn_before_mins = signIn_mins;
      }else if(signIn_mins < 0){
        //这属于是晚到了
        signIn_after_mins = Math.abs(signIn_mins);
      }
    }
    if(signOutTimeBean != null){
      String signOutDate = signOutTimeBean.getSigndate();
      String signOutTime = signOutTimeBean.getSigntime();
      //判断下签退和下班时间，是否存在晚走的情况
      String fromDateTime = workenddatetime;
      String toDateTime = signOutDate+" "+signOutTime;
      long signOut_mins = Duration.between(LocalDateTime.parse(fromDateTime, fullFormatter), LocalDateTime.parse(toDateTime, fullFormatter)).toMinutes();

      if(signOut_mins > 0){
        //这属于是晚走了
        signOut_after_mins = signOut_mins;
      }else if(signOut_mins < 0){
        //这属于是早退了
        signOut_before_mins = Math.abs(signOut_mins);
      }
    }

    if (earlyinearlyout != null && earlyinearlyout.size() > 0 && signIn_before_mins > 0) {
      //必须有早到时间才能继续下面的判断
      for (int i = 0; i < earlyinearlyout.size(); i++) {
        Map<String, Object> rule = (Map<String, Object>) earlyinearlyout.get(i);
        if (Util.null2String(rule.get("enable")).equals("1")) {
          int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//早到时间
          int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//允许早走时间
          if (Util.null2String(rule.get("enablesame")).equals("1")) {
            //使用相对时间
            if(signIn_before_mins >= advancetime){
              //如果打卡的早到时长已经完全大于了设置的早到时长,那么就可以早走整个postponetime
              early_localEndDateTime = localEndDateTime.minusMinutes(postponetime);
              early_localBeginDateTime = localBeginDateTime.minusMinutes(advancetime);
            }else{
              early_localEndDateTime = localEndDateTime.minusMinutes(signIn_before_mins);
              early_localBeginDateTime = localBeginDateTime.minusMinutes(signIn_before_mins);
            }
            break;
          }else{
            if(signIn_before_mins >= advancetime){
              //如果打卡的早到时长已经完全大于了设置的早到时长,那么就可以早走整个postponetime
              early_localEndDateTime = localEndDateTime.minusMinutes(postponetime);
              early_localBeginDateTime = localBeginDateTime.minusMinutes(advancetime);
              break;
            }
          }
        }
      }
    }

    if (lateinlateout != null && lateinlateout.size() > 0 && signIn_after_mins > 0) {
      //必须有晚到时间才能继续下面的判断
      for (int i = 0; i < lateinlateout.size(); i++) {
        Map<String, Object> rule = (Map<String, Object>) lateinlateout.get(i);
        if (Util.null2String(rule.get("enable")).equals("1")) {
          int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//晚到时间
          int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//需要晚走时间
          if (Util.null2String(rule.get("enablesame")).equals("1")) {
            //使用相对时间
            if(signIn_after_mins <= advancetime){
              //如果打卡的晚到时长必须要小于等于设置的晚到时长，才算满足，晚到多久就可以晚走多久
              late_localEndDateTime = localEndDateTime.plusMinutes(signIn_after_mins);
              late_localBeginDateTime = localBeginDateTime.plusMinutes(signIn_after_mins);
            }
            break;
          }else{
            if (signIn_after_mins <= advancetime) {
              //如果打卡的晚到时长已经完全小于了设置的晚到时长,那么需要晚走整个postponetime
              late_localEndDateTime = localEndDateTime.plusMinutes(postponetime);
              late_localBeginDateTime = localBeginDateTime.plusMinutes(advancetime);
              break;
            }
          }
        }
      }
    }


    if (lateoutlatein != null && lateoutlatein.size() > 0) {

      LocalDateTime tmp_localBeginDateTime = localBeginDateTime;
      if(early_localBeginDateTime != null){
        tmp_localBeginDateTime = early_localBeginDateTime;
      }
      if(late_localBeginDateTime != null){
        tmp_localBeginDateTime = late_localBeginDateTime;
      }
      KQFormatShiftRule kqFormatShiftRule = new KQFormatShiftRule();
      int preDayLateOutMins = kqFormatShiftRule.getPreDayLateOutMins(resourceid, splitDate);
      for (int i = 0; preDayLateOutMins > 0 && i < lateoutlatein.size(); i++) {
        Map<String, Object> rule = (Map<String, Object>) lateoutlatein.get(i);
        if (Util.null2String(rule.get("enable")).equals("1")) {
          int advancetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("advancetime"))))).intValue();//晚走时间
          int postponetime = new Double((Util.getDoubleValue(Util.null2String(rule.get("postponetime"))))).intValue();//允许晚到时间

          if (Util.null2String(rule.get("enablesame")).equals("1")) {
            if(preDayLateOutMins >= advancetime){
              //如果昨日的晚走时长已经完全大于了设置的晚走时长,那么就可以晚到整个postponetime
              latein_localBeginDateTime = tmp_localBeginDateTime.plusMinutes(postponetime);
            }else{
              latein_localBeginDateTime = tmp_localBeginDateTime.plusMinutes(preDayLateOutMins);
            }
            break;
          }else{
            if (preDayLateOutMins >= advancetime) {
              //如果昨日的晚走时长已经完全大于了设置的晚走时长,那么就可以晚到整个postponetime
              latein_localBeginDateTime = tmp_localBeginDateTime.plusMinutes(postponetime);
              break;
            }
          }
        }
      }
    }


    if(early_localEndDateTime != null){
      shifRuleMap.put("shift_type", "earlyinearlyout");
      String endDate = early_localEndDateTime.format(dateFormatter);
      String endmin = early_localEndDateTime.format(minFormatter);
      if(endDate.compareTo(splitDate) > 0){
        shifRuleMap.put("shift_endworktime", kqTimesArrayComInfo.turn24to48Time(endmin));
      }else{
        shifRuleMap.put("shift_endworktime", endmin);
      }
    }
    if(late_localEndDateTime != null){
      shifRuleMap.put("shift_type", "lateinlateout");
      String endDate = late_localEndDateTime.format(dateFormatter);
      String endmin = late_localEndDateTime.format(minFormatter);
      if(endDate.compareTo(splitDate) > 0){
        shifRuleMap.put("shift_endworktime", kqTimesArrayComInfo.turn24to48Time(endmin));
      }else{
        shifRuleMap.put("shift_endworktime", endmin);
      }
    }
    if(early_localBeginDateTime != null){
      shifRuleMap.put("shift_type", "earlyinearlyout");
      String beginDate = early_localBeginDateTime.format(dateFormatter);
      String beginmin = early_localBeginDateTime.format(minFormatter);
      if(beginDate.compareTo(splitDate) > 0){
        shifRuleMap.put("shift_beginworktime", kqTimesArrayComInfo.turn24to48Time(beginmin));
      }else{
        shifRuleMap.put("shift_beginworktime", beginmin);
      }
    }
    if(late_localBeginDateTime != null){
      shifRuleMap.put("shift_type", "lateinlateout");
      String beginDate = late_localBeginDateTime.format(dateFormatter);
      String beginmin = late_localBeginDateTime.format(minFormatter);
      if(beginDate.compareTo(splitDate) > 0){
        shifRuleMap.put("shift_beginworktime", kqTimesArrayComInfo.turn24to48Time(beginmin));
      }else{
        shifRuleMap.put("shift_beginworktime", beginmin);
      }
    }
    if(latein_localBeginDateTime != null){
      shifRuleMap.put("shift_type", "lateoutlatein");
      String beginDate = latein_localBeginDateTime.format(dateFormatter);
      String beginmin = latein_localBeginDateTime.format(minFormatter);
      if(beginDate.compareTo(splitDate) > 0){
        shifRuleMap.put("shift_beginworktime", kqTimesArrayComInfo.turn24to48Time(beginmin));
      }else{
        shifRuleMap.put("shift_beginworktime", beginmin);
      }
    }
    return shifRuleMap;
  }

}
