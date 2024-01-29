package com.engine.kq.wfset.util;

import com.api.customization.qc2476717.Util.ReplaceCardUtil;
import com.engine.kq.biz.KQAttFlowFieldsSetBiz;
import com.engine.kq.biz.KQFormatData;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQOverTimeRuleCalBiz;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.KQOvertimeRulesDetailEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;

public class KQFlowCardUtil {
  private KQLog kqLog = new KQLog();


  /**
   * 补卡流程数据 生成签到签退数据并更新考勤报表
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQCardAction(Map<String, String> sqlMap,
      List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
      int requestId, ResourceComInfo rci) throws Exception{

    String[] cardFields = KQAttFlowFieldsSetBiz.cardFields;

    KQFormatData kqFormatData = new KQFormatData();
    Map<String,String> result = new HashMap<>();
    RecordSet rs1 = new RecordSet();
    RecordSet rs2 = new RecordSet();

    String resourceId = "";
    String detail_scheduletime = "";
    String detail_signtype = "";
    String detail_signdate = "";
    String detail_signtime = "";

    String userId = "";
    String userType = "1";//默认给1
    String signType = "";
    String signDate = "";
    String signTime = "";
    String isInCom = "1";
//        String belongdate = "";

//        String serialid = "";
//        String worksection = "";
//        String belongtime = "";
    String signfrom = "";

    String signStatus = "";
//        String workdatesection = "";

    if(!sqlMap.isEmpty()){
      for(Map.Entry<String,String> me : sqlMap.entrySet()){
        String concort = "###" ;
        String key = me.getKey();
        String value = me.getValue();

        String wfId =  key.split(concort)[3] ;
        int usedetail =  Util.getIntValue(key.split(concort)[2], 0);
        String tableDetailName=  key.split(concort)[1] ;
        String tableName=  key.split(concort)[0] ;
        String idVal = "";
        String id = "dataId";
        if(usedetail == 1){
          id = "detailId";
        }
        rs1.execute(value);
        List<String> belongDateList = Lists.newArrayList();
        List<String> overtimeList = Lists.newArrayList();
        Map<String,List<String>> formatMap = Maps.newHashMap();
        Map<String,List<String>> overtimeMap = Maps.newHashMap();
        while (rs1.next()) {
          idVal = Util.null2s(rs1.getString(id), "");
          String signSource = "card:|wfid|"+wfId+"|requestid|"+requestId+"|detailId|"+idVal;

          String f_resourceId = cardFields[0];
          String f_detail_signdate = cardFields[3];
          String f_detail_scheduletime = cardFields[4];
          String f_detail_signtype = cardFields[6];
          String f_detail_signtime = cardFields[7];
          resourceId = Util.null2s(rs1.getString(f_resourceId), "");
          detail_scheduletime = Util.null2s(rs1.getString(f_detail_scheduletime), "");
          detail_signtype = Util.null2s(rs1.getString(f_detail_signtype), "");
          detail_signdate = Util.null2s(rs1.getString(f_detail_signdate), "");
          detail_signtime = Util.null2s(rs1.getString(f_detail_signtime), "");
          userId = resourceId;

          signType = "0".equalsIgnoreCase(detail_signtype)?"1":"2";
          signDate = detail_signdate;
          signTime = detail_signtime+":00";
          signfrom = signSource;

          //qc2476717 这里把流程中签到信息封装下
          Map<String,String> cardMap  = new HashMap<>();
          String signTypeC = "";
          if ("1".equals(signType)){
            signTypeC = "上班卡";
          }else {
            signTypeC = "下班卡";
          }
          cardMap.put("signType",signTypeC);
          cardMap.put("signDate",signDate);
          cardMap.put("signTime",detail_signtime);
          cardMap.put("signfrom",signfrom);
          cardMap.put("isInCom",isInCom);
          cardMap.put("userId",userId);

          //qc2476717 执行补卡插入
          User user = new User(1);
          ReplaceCardUtil replaceCardUtil = new ReplaceCardUtil();
         boolean isSucess =  replaceCardUtil.insertCard(user,cardMap);
         new BaseBean().writeLog("补卡插入结果："+isSucess);
          if(!isSucess){
            result.put("status", "-1");
            result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(390273,weaver.general.ThreadVarLanguage.getLang())+"action"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005361,weaver.general.ThreadVarLanguage.getLang())+"");
            return result;
          }

          StringBuffer sql = new StringBuffer("insert into HrmScheduleSign(userId,userType,signType,signDate,signTime,isInCom,signfrom) values(")
              .append(userId).append(",'").append(userType).append("','")
              .append(signType).append("','").append(signDate).append("','")
              .append(signTime).append("','").append(isInCom).append("','")
              .append(signfrom).append("'").append(" )");
          boolean isOk = rs2.execute(sql.toString());



          if(!isOk){
            result.put("status", "-1");
            result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(390273,weaver.general.ThreadVarLanguage.getLang())+"action"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005361,weaver.general.ThreadVarLanguage.getLang())+"");
            return result;
          }
          kqLog.info("handleKQCardAction:userId:"+userId+":signDate:"+signDate+":signTime:"+signTime);
          String cardKey = resourceId+"_"+signDate+"_"+signTime;
          if(!overtimeList.contains(cardKey)){
            overtimeList.add(cardKey);
          }
        }
        for (int i = 0; i < overtimeList.size(); i++) {
          String cardKey = overtimeList.get(i);
          List<String> cardKeys = Util.splitString2List(cardKey, "_");
          if(cardKeys.size() == 3){
            String resourceid = cardKeys.get(0);
            String date = cardKeys.get(1);
            String time = cardKeys.get(2);
            String belongDate = getBelongDate(resourceId, date, time);
            String belongKey = resourceid+"_"+belongDate;
            if(!belongDateList.contains(belongKey)){
              belongDateList.add(belongKey);
            }
          }
        }
        KQOverTimeRuleCalBiz kqOvertimeCalBiz = new KQOverTimeRuleCalBiz();
        if(!belongDateList.isEmpty()){
          for (String keys :belongDateList) {
            List<String> belongdateKey = Util.splitString2List(keys, "_");
            if(belongdateKey.size() == 2){
              String resourceid = belongdateKey.get(0);
              String belongDate = belongdateKey.get(1);
              kqLog.info("handleKQCardAction:resourceid:"+resourceid+":belongDate:"+belongDate);
              new KQFormatData().formatKqDate(resourceid,belongDate);
              String tomorrow = DateUtil.addDate(belongDate,1);
              new KQFormatData().formatKqDate(resourceid,tomorrow);
              String preDay = DateUtil.addDate(belongDate, -1);
              new KQFormatData().formatKqDate(resourceid, preDay);			  
              kqOvertimeCalBiz.buildOvertime(resourceid, preDay, preDay, "补卡流程生成加班#card,preDay,requestId:" + requestId);			  
              kqOvertimeCalBiz.buildOvertime(resourceid, belongDate, belongDate, "补卡流程生成加班#card,belongDate,requestId:"+requestId);

            }

          }
        }

      }
    }

    return result;
  }

  /**
   * 判断打卡归属日期
   * @param userid
   * @param date
   * @param time
   * @throws Exception
   */
  public String getBelongDate(String userid, String date, String time) throws Exception{
    KQOverTimeRuleCalBiz kqOverTimeRuleCalBiz = new KQOverTimeRuleCalBiz();
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String preDay = DateUtil.addDate(date, -1);//昨天
    String nextDay = DateUtil.addDate(date, 1);//明天
    Map<String,Integer> changeTypeMap = Maps.newHashMap();
    Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap = Maps.newHashMap();
    Map<String, List<String[]>> restTimeMap = Maps.newHashMap();
    Map<String,Integer> computingModeMap = Maps.newHashMap();
    //先获取一些前提数据，加班規則和假期規則
    kqOverTimeRuleCalBiz.getOverTimeDataMap(userid, preDay, nextDay, dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,computingModeMap);

    String change_key = date+"_"+userid;
    String preChange_key = preDay+"_"+userid;
    String nextChange_key = nextDay+"_"+userid;
    int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
    int preChangeType = Util.getIntValue(""+changeTypeMap.get(preChange_key),-1);
    int nextChangeType = Util.getIntValue(""+changeTypeMap.get(nextChange_key),-1);
    String changeType_key = date+"_"+changeType;
    String preChangeType_key = preDay+"_"+preChangeType;
    String nextChangeType_key = nextDay+"_"+nextChangeType;
    KQOvertimeRulesDetailEntity curKqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
    KQOvertimeRulesDetailEntity preKqOvertimeRulesDetailEntity = overRulesDetailMap.get(preChangeType_key);
    KQOvertimeRulesDetailEntity nextKqOvertimeRulesDetailEntity = overRulesDetailMap.get(preChangeType_key);

    boolean isInRange = checkInWortTimeRange(userid, date,date, time,curKqOvertimeRulesDetailEntity);
    if(isInRange){
      return date;
    }
    isInRange = checkInWortTimeRange(userid, preDay,date, time, preKqOvertimeRulesDetailEntity);
    if(isInRange){
      return preDay;
    }
    isInRange = checkInWortTimeRange(userid, nextDay,date, time, nextKqOvertimeRulesDetailEntity);
    if(isInRange){
      return nextDay;
    }
    return date;

  }

  public boolean checkInWortTimeRange(String userid, String checkDate, String date, String time,
      KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity) throws Exception{
    boolean isInRange = false;
    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String preDay = DateUtil.addDate(checkDate, -1);//昨天
    String nextDay = DateUtil.addDate(checkDate, 1);//明天

    KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(userid, checkDate);
    List<TimeScopeEntity> lsSignTime = new ArrayList<>();
    List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
    lsSignTime = workTime.getSignTime();//允许打卡时间
    if(lsSignTime != null && !lsSignTime.isEmpty()) {
      TimeScopeEntity firstSignTimeScope = lsSignTime.get(0);
      TimeScopeEntity lastSignTimeScope = lsSignTime.get(lsSignTime.size()-1);
      String beginSignScope = "";
      String endSignScope = "";

      String signBeginTime = firstSignTimeScope.getBeginTime();
      String signEndTime = lastSignTimeScope.getEndTime();
      beginSignScope = checkDate+" "+signBeginTime;
      endSignScope = checkDate+" "+signEndTime;
      //上班允许打卡是否跨到前一天
      boolean beginTimePreAcross = firstSignTimeScope.isBeginTimePreAcross();
      if(beginTimePreAcross){
        beginSignScope = preDay+" "+signBeginTime;
      }
      boolean signEndTimeAcross = lastSignTimeScope.getEndTimeAcross();
      if(signEndTimeAcross){
        endSignScope = nextDay+" "+signEndTime;
      }
      if(beginSignScope.length() > 0){
        beginSignScope = beginSignScope+":00";
      }
      if(endSignScope.length() > 0){
        endSignScope = endSignScope+":59";
      }
      if(kqOvertimeRulesDetailEntity != null){
        int has_cut_point = kqOvertimeRulesDetailEntity.getHas_cut_point();
        if(has_cut_point == 1){
          //如果设置了打卡归属
          String cut_point = kqOvertimeRulesDetailEntity.getCut_point();
          String cut_point_datettime = nextDay+" "+cut_point+":59";
          if(endSignScope.compareTo(cut_point_datettime) < 0){
            endSignScope = cut_point_datettime;
          }
        }
      }
      String datetime = date+" "+time;
      if(beginSignScope.length() > 0 && endSignScope.length() > 0){
        LocalDateTime local_beginSignScope = LocalDateTime.parse(beginSignScope,fullFormatter);
        LocalDateTime local_endSignScope = LocalDateTime.parse(endSignScope,fullFormatter);
        LocalDateTime datetime_endSignScope = LocalDateTime.parse(datetime,fullFormatter);
        if((datetime_endSignScope.isAfter(local_beginSignScope) || datetime_endSignScope.equals(local_beginSignScope))
            && (datetime_endSignScope.isBefore(local_endSignScope) || datetime_endSignScope.equals(local_endSignScope))){
          isInRange = true;
        }
      }
    }
    return isInRange;
  }


}
