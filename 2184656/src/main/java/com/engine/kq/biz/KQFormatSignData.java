package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.engine.kq.cmd.attendanceButton.ButtonStatusEnum;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.UtilKQ;
import com.google.common.collect.Lists;
import java.io.PrintWriter;
import java.io.StringWriter;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import java.util.*;

/**
 * 获取班次打卡数据
 */
public class KQFormatSignData extends BaseBean {
  private KQLog kqLog = new KQLog();

  public List<Object> getSignInfo(String userId, TimeScopeEntity signTimeScope,
      TimeScopeEntity workTimeScope, String kqDate, String preDate,
      String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo) {
    return getSignInfo(userId, signTimeScope, workTimeScope, kqDate, preDate, nextDate, kqTimesArrayComInfo,
        Lists.newArrayList(), "");
  }
  /***
   * 获取班次打卡数据
   * @param userId
   * @param signTimeScope
   * @param workTimeScope
   * @param kqDate
   * @param preDate
   * @param nextDate
   * @param kqTimesArrayComInfo
   * @return
   */
  public List<Object> getSignInfo(String userId, TimeScopeEntity signTimeScope,
      TimeScopeEntity workTimeScope, String kqDate, String preDate,
      String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo,ArrayList<String> hostIps,String uuid) {
    kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid+" in ");
    List<Object> lsCheckInfo = new ArrayList<>();
    try{
      Map<String, Object> checkInfo = null;
      String base_sql = "";
      RecordSet rs = new RecordSet();
      String dbtype = rs.getDBType();

      //获取工作上下班时间
      String workBeginDateTime = workTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
      workBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(workTimeScope.getBeginTime())+":00";
      kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid+"::workBeginDateTime::"+workBeginDateTime);

      //获取工作上下班时间
      String workEndDateTime = workTimeScope.getEndTimeAcross() ? nextDate : kqDate;
      workEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(workTimeScope.getEndTime())+":00";
      kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid+"::workEndDateTime::"+workEndDateTime);

      Map<String,String> flow_deduct_card_map = getflowDeductCardSql(userId,kqDate,workTimeScope.getBeginTime(),workTimeScope.getEndTime(),signTimeScope);
      new BaseBean().writeLog("==zj==(flow_deduct_card_map)" + JSON.toJSONString(flow_deduct_card_map));

      kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid+"::flow_deduct_card_map::"+flow_deduct_card_map);

      List<Map<String,String>> sqlConditions = getCanSignInfo(signTimeScope,kqDate,preDate,nextDate,kqTimesArrayComInfo);
      new BaseBean().writeLog("==zj==(sqlConditions)" + JSON.toJSONString(sqlConditions));
      base_sql = signSignSql(rs);
      new KQLog().info("sqlConditions:(userId:"+userId+":base_sql"+base_sql+":::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid+"::flow_deduct_card_map::"+flow_deduct_card_map);
      if(sqlConditions != null && !sqlConditions.isEmpty()){
        for(Map<String,String> sqlMap : sqlConditions){
          String sql = "";
          String orderSql = "";
          int idx = 0;
          String signBeginDateTime = Util.null2String(sqlMap.get("signBeginDateTime"));
          String signEndDateTime = Util.null2String(sqlMap.get("signEndDateTime"));
          String type = Util.null2String(sqlMap.get("type"));
          if(type.length() > 0){
            if("signoff".equalsIgnoreCase(type)){
              orderSql = " order by signdate desc, signtime desc ";
            }else if("signin".equalsIgnoreCase(type)){
              orderSql = " order by signdate asc, signtime asc ";
            }
            if("oracle".equalsIgnoreCase(dbtype)){
              sql = "select * from ("+base_sql+" "+orderSql+") a where rownum=1";
            }else if("mysql".equalsIgnoreCase(dbtype)){
              sql = "select * from ("+base_sql+" "+orderSql+") a limit 0,1";
            }
            else if("postgresql".equalsIgnoreCase(dbtype)){
              sql = "select * from ("+base_sql+" "+orderSql+") a limit 1 offset 0";
            }
            else if("sqlserver".equalsIgnoreCase(dbtype)){
              sql = "select top 1 * from ("+base_sql+") a "+" "+orderSql;
            }else{
              sql = "select * from ("+base_sql+" "+orderSql+") a where rownum=1";
            }
          }else{
            orderSql = " order by signdate asc, signtime asc ";
            sql = base_sql+" "+orderSql;
          }
          new BaseBean().writeLog("==zj==(获取签到数据)"+ sql + " | " + userId + " | " +signBeginDateTime +" | " + signEndDateTime);
          rs.executeQuery(sql, userId, signBeginDateTime, signEndDateTime);
          new KQLog().info("getSignInfo:(userId:"+userId+":signBeginDateTime:"+
              signBeginDateTime+":signEndDateTime:"+signEndDateTime+"):sql"+sql+":counts:"+rs.getCounts()+":::userId" + userId + "kqDate==" + kqDate+":hostIps:"+hostIps+":uuid::"+uuid+"::flow_deduct_card_map::"+flow_deduct_card_map);
          while (rs.next()) {
            String signId = Util.null2String(rs.getString("id"));
            String signdate = Util.null2String(rs.getString("signdate"));
            String signtime = Util.null2String(rs.getString("signtime"));

            checkInfo = new HashMap<>();
            checkInfo.put("signId", signId);//签到签退标识
            checkInfo.put("signDate", signdate);//签到签退日期
            checkInfo.put("signTime", signtime);//签到签退时间
            String signDateTime = signdate + " " + signtime;
            idx++;
            if(type.length() > 0){
              if("signin".equalsIgnoreCase(type)){
                checkInfo.put("signType", "1");
                if(workBeginDateTime.length()>0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                }
                if(!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())){
                  if(flow_deduct_card_map.containsKey("signin")){
                    String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
                    if(deduct_signintime.length() > 0){
                      signDateTime = signdate + " " + deduct_signintime;
                      checkInfo.put("deduct_signintime", deduct_signintime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                    }
                  }
                }
                lsCheckInfo.add(checkInfo);
              }else {
                checkInfo.put("signType", "2");
                if(workEndDateTime.length()>0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                }
                if(!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())){
                  if(flow_deduct_card_map.containsKey("signoff")){
                    String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
                    if(deduct_signofftime.length() > 0){
                      signDateTime = signdate + " " + deduct_signofftime;
                      checkInfo.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                    }
                  }
                }
                lsCheckInfo.add(checkInfo);
              }
            }else{
              if(idx==1){//第一条算签到
                checkInfo.put("signType", "1");
                if(workBeginDateTime.length()>0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                }
                if(!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())){
                  if(flow_deduct_card_map.containsKey("signin")){
                    String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
                    if(deduct_signintime.length() > 0){
                      signDateTime = signdate + " " + deduct_signintime;
                      checkInfo.put("deduct_signintime", deduct_signintime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                    }
                  }
                }
                lsCheckInfo.add(checkInfo);
              }else if(idx==rs.getCounts()){//最后一条算签退
                checkInfo.put("signType", "2");
                if(workEndDateTime.length()>0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                }
                if(!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())){
                  if(flow_deduct_card_map.containsKey("signoff")){
                    String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
                    if(deduct_signofftime.length() > 0){
                      signDateTime = signdate + " " + deduct_signofftime;
                      checkInfo.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                    }
                  }
                }
                lsCheckInfo.add(checkInfo);
              }
            }
          }
        }
      }
      //如果签到，签退不成对，流程抵扣异常存在，那么需要判断下是不是可以补足
      new BaseBean().writeLog("==zj==(是不是可以补足)" + lsCheckInfo.size());
      if(lsCheckInfo.size() < 2 && !flow_deduct_card_map.isEmpty()){
        if(lsCheckInfo.isEmpty()){
          if(flow_deduct_card_map.containsKey("signin")){
            String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
            if(deduct_signintime.length() > 0){
              checkInfo = new HashMap<>();
              String[] workBeginDateTimes = workBeginDateTime.split(" ");
              String tmp_workBeginDate = workBeginDateTimes[0];
              String tmp_workBeginTime = workBeginDateTimes[1];
              checkInfo.put("signType", "1");
              checkInfo.put("signDate", tmp_workBeginDate);//签到签退日期
              checkInfo.put("signTime", "");//签到签退时间
              checkInfo.put("deduct_signintime", tmp_workBeginTime);//流程抵扣作为打卡时间
              checkInfo.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
              lsCheckInfo.add(checkInfo);
            }
          }
          if(flow_deduct_card_map.containsKey("signoff")){
            String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
            if(deduct_signofftime.length() > 0){
              checkInfo = new HashMap<>();
              String[] workEndDateTimes = workEndDateTime.split(" ");
              String tmp_workEndDate = workEndDateTimes[0];
              String tmp_workEndTime = workEndDateTimes[1];
              checkInfo.put("signType", "2");
              checkInfo.put("signDate", tmp_workEndDate);//签到签退日期
              checkInfo.put("signTime", "");//签到签退时间
              checkInfo.put("deduct_signofftime", tmp_workEndTime);//流程抵扣作为打卡时间
              checkInfo.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
              lsCheckInfo.add(checkInfo);
            }
          }
        }else{
          Map<String, Object> checkCardMap = (Map<String, Object>) lsCheckInfo.get(0);
          if(!checkCardMap.isEmpty()){
            String signType = Util.null2String(checkCardMap.get("signType"));
            if("1".equalsIgnoreCase(signType)){
              //如果签到数据有了，检测下是不是有签退的流程抵扣打卡
              if(flow_deduct_card_map.containsKey("signoff")){
                String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
                if(deduct_signofftime.length() > 0){
                  checkInfo = new HashMap<>();
                  String[] workEndDateTimes = workEndDateTime.split(" ");
                  String tmp_workEndDate = workEndDateTimes[0];
                  String tmp_workEndTime = workEndDateTimes[1];
                  checkInfo.put("signType", "2");
                  checkInfo.put("signDate", tmp_workEndDate);//签到签退日期
                  checkInfo.put("signTime", "");//签到签退时间
                  checkInfo.put("deduct_signofftime", tmp_workEndTime);//流程抵扣作为打卡时间
                  checkInfo.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
                  lsCheckInfo.add(checkInfo);
                }
              }
            }else{
              if(flow_deduct_card_map.containsKey("signin")){
                String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
                if(deduct_signintime.length() > 0){
                  checkInfo = new HashMap<>();
                  String[] workBeginDateTimes = workBeginDateTime.split(" ");
                  String tmp_workBeginDate = workBeginDateTimes[0];
                  String tmp_workBeginTime = workBeginDateTimes[1];
                  checkInfo.put("signType", "1");
                  checkInfo.put("signDate", tmp_workBeginDate);//签到签退日期
                  checkInfo.put("signTime", "");//签到签退时间
                  checkInfo.put("deduct_signintime", tmp_workBeginTime);//流程抵扣作为打卡时间
                  checkInfo.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
                  lsCheckInfo.add(checkInfo);
                }
              }
            }
          }
        }
      }
    }catch (Exception e){

      kqLog.info("报表错:getSignInfo:");
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
    }

    //writeLog(sql,userId +"=="+ signBeginDateTime+"=="+signEndDateTime);
    return lsCheckInfo;
  }

  /**
   * 根据上下班时间获取到是否有流程抵扣打卡的数据
   * @param userId
   * @param kqDate
   * @param workBeginDateTime
   * @param workEndDateTime
   */
  public Map<String,String> getflowDeductCardSql(String userId, String kqDate, String workBeginDateTime, String workEndDateTime,TimeScopeEntity signTimeScope) {
    Map<String,String> flow_deduct_card_map = new HashMap<>();
    RecordSet rs = new RecordSet();
    TimeSignScopeEntity timeSignScopeEntity = signTimeScope.getTimeSignScopeEntity(); //获取班次时间范围
    String beginTimeEnd = workBeginDateTime;    //默认为流程开始时间
    String endTimeStart = workEndDateTime;      //默认为流程结束时间


    String flow_deduct_card_sql = "select * from kq_flow_deduct_card where 1=1 and (isclear is null or isclear<>1) ";
    if(userId.length() > 0){
      flow_deduct_card_sql += " and resourceid="+userId;
    }
    if(kqDate.length() > 0){
      flow_deduct_card_sql += " and belongDate='"+kqDate+"'";
    }

    new BaseBean().writeLog("==zj==(timeSignScopeEntity)" + JSON.toJSONString(timeSignScopeEntity));
    if (timeSignScopeEntity!=null){
       beginTimeEnd = timeSignScopeEntity.getBeginTimeEnd();  //获取上班打卡结束时间
       endTimeStart = timeSignScopeEntity.getEndTimeStart();  //获取下班打卡开始时间
    }
    if(workBeginDateTime.length() > 0){
      flow_deduct_card_sql += " and workBeginTime='"+workBeginDateTime+"'";
    }
    if(workEndDateTime.length() > 0){
      flow_deduct_card_sql += " and workEndTime='"+workEndDateTime+"'";
    }

    rs.executeQuery(flow_deduct_card_sql);
    //查看打卡中间表
    new BaseBean().writeLog("==zj==(查询打卡中间表)" + flow_deduct_card_sql);
    while (rs.next()){
      String signtype = rs.getString("signtype");
      if("1".equalsIgnoreCase(signtype)){
        flow_deduct_card_map.put("signin",workBeginDateTime);
      }
      if("2".equalsIgnoreCase(signtype)){
        flow_deduct_card_map.put("signoff",workEndDateTime);
      }
    }
    return flow_deduct_card_map;
  }

  /**
   * 根据打卡的范围生成上班，下班数据获取的sql
   * @param signTimeScope
   * @param kqDate
   * @param preDate
   * @param nextDate
   * @param kqTimesArrayComInfo
   * @return
   */
  public List<Map<String,String>> getCanSignInfo(TimeScopeEntity signTimeScope, String kqDate, String preDate, String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo) {

    List<Map<String,String>> sqlConditions = new ArrayList<>();
    Map<String,String> conditionMap = new HashMap<>();

    TimeSignScopeEntity timeSignScopeEntity = signTimeScope.getTimeSignScopeEntity();

    String signBeginDateTime = "";
    String signEndDateTime = "";
    signBeginDateTime = signTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
    if(signTimeScope.isBeginTimePreAcross()){
      signBeginDateTime = preDate;
    }
    signBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime())+":00";

    signEndDateTime = signTimeScope.getEndTimeAcross() ? nextDate : kqDate;
    signEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime())+":59";

    if(timeSignScopeEntity == null){
      //没有设置 签到最晚时间和签退最早时间
      conditionMap = new HashMap<>();
      conditionMap.put("signBeginDateTime", signBeginDateTime);
      conditionMap.put("signEndDateTime", signEndDateTime);
      sqlConditions.add(conditionMap);
    }else{
      String beginTimeEnd = timeSignScopeEntity.getBeginTimeEnd();
      boolean beginTimeEndAcross = timeSignScopeEntity.isBeginTimeEndAcross();

      String endTimeStart = timeSignScopeEntity.getEndTimeStart();
      boolean endTimeStartAcross = timeSignScopeEntity.isEndTimeStartAcross();

      if(beginTimeEnd.length() > 0){
        //如果设置了 上班结束时间
        if(endTimeStart.length() > 0){
          //设置了下班开始时间
          String signBeginDateEndTime = "";
          String signEndDateStartTime = "";
          signBeginDateEndTime = beginTimeEndAcross ? nextDate : kqDate;
          signBeginDateEndTime+=" "+kqTimesArrayComInfo.turn48to24Time(beginTimeEnd)+":59";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signBeginDateTime);
          conditionMap.put("signEndDateTime", signBeginDateEndTime);
          conditionMap.put("type", "signin");
          sqlConditions.add(conditionMap);

          signEndDateStartTime = endTimeStartAcross ? nextDate : kqDate;
          signEndDateStartTime+=" "+kqTimesArrayComInfo.turn48to24Time(endTimeStart)+":00";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signEndDateStartTime);
          conditionMap.put("signEndDateTime", signEndDateTime);
          conditionMap.put("type", "signoff");
          sqlConditions.add(conditionMap);
        }else{
          //没有设置下班开始时间
          String signBeginDateEndTime = "";
          String signEndDateStartTime = "";
          signBeginDateEndTime = beginTimeEndAcross ? nextDate : kqDate;
          signBeginDateEndTime+=" "+kqTimesArrayComInfo.turn48to24Time(beginTimeEnd)+":59";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signBeginDateTime);
          conditionMap.put("signEndDateTime", signBeginDateEndTime);
          conditionMap.put("type", "signin");
          sqlConditions.add(conditionMap);

          //如果设置了上班结束时间，相当于下班开始时间也被限定了
          String endTimeByBeginTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(beginTimeEnd)+1);
          signEndDateStartTime = beginTimeEndAcross ? nextDate : kqDate;
          signEndDateStartTime+=" "+kqTimesArrayComInfo.turn48to24Time(endTimeByBeginTime)+":00";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signEndDateStartTime);
          conditionMap.put("signEndDateTime", signEndDateTime);
          conditionMap.put("type", "signoff");
          sqlConditions.add(conditionMap);
        }
      }else if(endTimeStart.length() > 0){
        //如果没有设置上班结束时间，设置了下班开始时间
        String signBeginDateEndTime = "";
        String signEndDateStartTime = "";

        //如果设置了下班开始时间，相当于上班结束时间也被限定了
        String BeginTimeByendTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(endTimeStart)-1);
        signBeginDateEndTime = endTimeStartAcross ? nextDate : kqDate;
        signBeginDateEndTime+=" "+kqTimesArrayComInfo.turn48to24Time(BeginTimeByendTime)+":59";

        conditionMap = new HashMap<>();
        conditionMap.put("signBeginDateTime", signBeginDateTime);
        conditionMap.put("signEndDateTime", signBeginDateEndTime);
        conditionMap.put("type", "signin");
        sqlConditions.add(conditionMap);

        signEndDateStartTime = endTimeStartAcross ? nextDate : kqDate;
        signEndDateStartTime+=" "+kqTimesArrayComInfo.turn48to24Time(endTimeStart)+":00";

        conditionMap = new HashMap<>();
        conditionMap.put("signBeginDateTime", signEndDateStartTime);
        conditionMap.put("signEndDateTime", signEndDateTime);
        conditionMap.put("type", "signoff");
        sqlConditions.add(conditionMap);
      }
    }
    return sqlConditions;
  }

  public String signSignSql(RecordSet rs){
    String sql = "";
    if(rs.getDBType().equals("oracle")){
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate||' '||signtime >= ? and signdate||' '||signtime <= ? " +
          " ";
    }else if("sqlserver".equals(rs.getDBType())){
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate + ' ' + signtime >= ? and signdate + ' ' + signtime <= ? " +
          " ";
    }else{
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and concat(signdate,' ',signtime) >= ? and concat(signdate,' ',signtime) <= ? " +
          " ";
    }
    return sql;
  }

  public List<Object> getSignInfoForAll(String userId, String signBeginDateTime, String signEndDateTime, String workBeginDateTime, String workEndDateTime) {
    List<Object> lsCheckInfo = new ArrayList<>();
    Map<String, Object> checkInfo = null;
    String sql = "";
    RecordSet rs = new RecordSet();

    int idx = 0;
    if(rs.getDBType().equals("oracle")){
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate||' '||signtime >= ? and signdate||' '||signtime <= ? " +
          " order by signdate asc, signtime asc ";
    }else if("sqlserver".equals(rs.getDBType())){
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate + ' ' + signtime >= ? and signdate + ' ' + signtime <= ? " +
          " order by signdate asc, signtime asc ";
    }else{
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and concat(signdate,' ',signtime) >= ? and concat(signdate,' ',signtime) <= ? " +
          " order by signdate asc, signtime asc ";
    }
    rs.executeQuery(sql, userId, signBeginDateTime, signEndDateTime);
    //writeLog(sql,userId +"=="+ signBeginDateTime+"=="+signEndDateTime);
    while (rs.next()) {
      String signId = Util.null2String(rs.getString("id"));
      String signdate = Util.null2String(rs.getString("signdate"));
      String signtime = Util.null2String(rs.getString("signtime"));

      checkInfo = new HashMap<>();
      checkInfo.put("signId", signId);//签到签退标识
      checkInfo.put("signDate", signdate);//签到签退日期
      checkInfo.put("signTime", signtime);//签到签退时间
      String signDateTime = signdate + " " + signtime;

      idx++;
      if(idx%2==1){
        checkInfo.put("signType", "1");
      }else{
        checkInfo.put("signType", "2");
      }
      lsCheckInfo.add(checkInfo);
    }
    return lsCheckInfo;
  }

  public List<Object> getNonWorkSignInfo(String userId, String preDate, String kqDate,
      List<TimeScopeEntity> pre_lsSignTime,
      List<TimeScopeEntity> next_lsSignTime) {
    List<Object> lsCheckInfo = new ArrayList<>();
    Map<String, Object> checkInfo = null;
    RecordSet rs = new RecordSet();
    String pre_Worktime4Today = "";
    if(!pre_lsSignTime.isEmpty()){
      TimeScopeEntity pre_signTimeScope = pre_lsSignTime.get(pre_lsSignTime.size()-1);
      if(pre_signTimeScope.getEndTimeAcross()){
        pre_Worktime4Today = pre_signTimeScope.getEndTime();
      }
    }
    String next_Worktime4Today = "";
    if(!next_lsSignTime.isEmpty()){
      TimeScopeEntity next_signTimeScope = next_lsSignTime.get(next_lsSignTime.size()-1);
      if(next_signTimeScope.isBeginTimePreAcross()){
        next_Worktime4Today = next_signTimeScope.getBeginTime();
      }
    }
    String sql = "select * from hrmschedulesign where userid="+userId+" and signdate = '"+kqDate+"' ";
    if(pre_Worktime4Today.length() > 0){
      if(pre_Worktime4Today.length() == 5){
        pre_Worktime4Today += ":59";
      }
      sql += " and signtime > '"+pre_Worktime4Today+"'";
    }
    if(next_Worktime4Today.length() > 0){
      if(next_Worktime4Today.length() == 5){
        next_Worktime4Today += ":00";
      }
      sql += " and signtime < '"+next_Worktime4Today+"'";
    }
    sql += " order by signdate asc,signtime asc ";
    rs.executeQuery(sql);
    int idx = 0;
    while (rs.next()){
      String signId = Util.null2String(rs.getString("id"));
      String signdate = Util.null2String(rs.getString("signdate"));
      String signtime = Util.null2String(rs.getString("signtime"));

      checkInfo = new HashMap<>();
      checkInfo.put("signId", signId);//签到签退标识
      checkInfo.put("signDate", signdate);//签到签退日期
      checkInfo.put("signTime", signtime);//签到签退时间
      idx++;
      if(idx==1){//第一条算签到
        checkInfo.put("signType", "1");
        lsCheckInfo.add(checkInfo);
      }else if(idx==rs.getCounts()){//最后一条算签退
        checkInfo.put("signType", "2");
        lsCheckInfo.add(checkInfo);
      }
    }
    return lsCheckInfo;
  }

}