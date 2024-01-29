package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.engine.kq.bean.KQHrmScheduleSign;
import com.engine.kq.cmd.attendanceButton.ButtonStatusEnum;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.UtilKQ;
import com.engine.kq.wfset.util.KQSignUtil;
import com.google.common.collect.Lists;

import java.awt.image.BandedSampleModel;
import java.io.PrintWriter;
import java.io.StringWriter;

import weaver.common.DateUtil;
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

  public List<Object> getSignInfo(String userId, TimeScopeEntity signTimeScope,
                                  TimeScopeEntity workTimeScope, String kqDate, String preDate,
                                  String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo, ArrayList<String> hostIps, String uuid) {
    return getSignInfo(userId, signTimeScope, workTimeScope, kqDate, preDate, nextDate, kqTimesArrayComInfo, hostIps, uuid, 0, 0);

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
                                  String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo, ArrayList<String> hostIps, String uuid, int shiftCount, int shiftI) {
    kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate + ":hostIps:" + hostIps + ":uuid::" + uuid + " in ");
    List<Object> lsCheckInfo = new ArrayList<>();
    String count4NoonStartDateTime = "";
    String count4NoonEndDateTime = "";
    String signEndDateTime4Afternoon = "";
    try {
      Map<String, Object> checkInfo = null;
      String base_sql = "";
      RecordSet rs = new RecordSet();
      String dbtype = rs.getDBType();

      //获取工作上下班时间
      String workBeginDateTime = workTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
      workBeginDateTime += " " + kqTimesArrayComInfo.turn48to24Time(workTimeScope.getBeginTime()) + ":00";
      kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate + ":hostIps:" + hostIps + ":uuid::" + uuid + "::workBeginDateTime::" + workBeginDateTime);

      //获取工作上下班时间
      String workEndDateTime = workTimeScope.getEndTimeAcross() ? nextDate : kqDate;
      workEndDateTime += " " + kqTimesArrayComInfo.turn48to24Time(workTimeScope.getEndTime()) + ":00";
      kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate + ":hostIps:" + hostIps + ":uuid::" + uuid + "::workEndDateTime::" + workEndDateTime);

      Map<String, String> flow_deduct_card_map = getflowDeductCardSql(userId, kqDate, workTimeScope.getBeginTime(), workTimeScope.getEndTime());

      kqLog.info("in getSignInfo ::userId" + userId + "kqDate==" + kqDate + ":hostIps:" + hostIps + ":uuid::" + uuid + "::flow_deduct_card_map::" + flow_deduct_card_map);

      List<Map<String, String>> sqlConditions = getCanSignInfo(signTimeScope, kqDate, preDate, nextDate, kqTimesArrayComInfo, workTimeScope, shiftCount, shiftI);
      base_sql = signSignSql(rs);
      new KQLog().info("sqlConditions:(userId:" + userId + ":base_sql" + base_sql + ":::userId" + userId + "kqDate==" + kqDate + ":hostIps:" + hostIps + ":uuid::" + uuid + "::flow_deduct_card_map::" + flow_deduct_card_map);
      if (sqlConditions != null && !sqlConditions.isEmpty()) {
        if (shiftCount == 2) {
          String fieldName = "";
          //==zj==
          String afterTime="";
          if (shiftI == 0) {
            fieldName = "signEndDateTime";
          } else if (shiftI == 1) {
            fieldName = "signBeginDateTime";
          }
          Map<String, String> sqlMap = sqlConditions.get(0);
          afterTime = Util.null2String(sqlMap.get("signEndDateTime"));//获取下班时间


          count4NoonStartDateTime = Util.null2String(sqlMap.get(fieldName));
          if (count4NoonStartDateTime.length() == 19) {
            count4NoonStartDateTime = count4NoonStartDateTime.substring(0, 17) + "00";
          }
          sqlMap = sqlConditions.get(0);
          count4NoonEndDateTime = Util.null2String(sqlMap.get(fieldName));
          if (sqlConditions.size() >= 2) {
            sqlMap = sqlConditions.get(1);
            count4NoonEndDateTime = Util.null2String(sqlMap.get(fieldName));
          }
        }

        for (Map<String, String> sqlMap : sqlConditions) {
          String sql = "";
          String orderSql = "";
          int idx = 0;
          String signBeginDateTime = Util.null2String(sqlMap.get("signBeginDateTime"));
          String signEndDateTime = Util.null2String(sqlMap.get("signEndDateTime"));
          signEndDateTime4Afternoon = workEndDateTime;
          String type = Util.null2String(sqlMap.get("type"));
          new BaseBean().writeLog("==zj==(type)" + type);
          if (type.length() > 0) {
            if ("signoff".equalsIgnoreCase(type)) {
              orderSql = " order by signdate desc, signtime desc ";
            } else if ("signin".equalsIgnoreCase(type)) {
              orderSql = " order by signdate asc, signtime asc ";
            }
            if ("oracle".equalsIgnoreCase(dbtype)) {
              sql = "select * from (" + base_sql + " " + orderSql + ") a where rownum=1";
            } else if ("mysql".equalsIgnoreCase(dbtype)) {
              sql = "select * from (" + base_sql + " " + orderSql + ") a limit 0,1";
            } else if ("postgresql".equalsIgnoreCase(dbtype)) {
              sql = "select * from (" + base_sql + " " + orderSql + ") a limit 1 offset 0";
            } else if ("sqlserver".equalsIgnoreCase(dbtype)) {
              sql = "select top 1 * from (" + base_sql + ") a " + " " + orderSql;
            } else {
              sql = "select * from (" + base_sql + " " + orderSql + ") a where rownum=1";
            }
          } else {
            orderSql = " order by signdate asc, signtime asc ";
            sql = base_sql + " " + orderSql;
          }
          rs.executeQuery(sql, userId, signBeginDateTime, signEndDateTime);
          new KQLog().info("getSignInfo:(userId:" + userId + ":signBeginDateTime:" +
                  signBeginDateTime + ":signEndDateTime:" + signEndDateTime + "):sql" + sql + ":counts:" + rs.getCounts() + ":::userId" + userId + "kqDate==" + kqDate + ":hostIps:" + hostIps + ":uuid::" + uuid + "::flow_deduct_card_map::" + flow_deduct_card_map);
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
            if (type.length() > 0) {
              if ("signin".equalsIgnoreCase(type)) {
                checkInfo.put("signType", "1");
                if (workBeginDateTime.length() > 0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                }
                if (!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())) {
                  if (flow_deduct_card_map.containsKey("signin")) {
                    String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
                    if (deduct_signintime.length() > 0) {
                      signDateTime = signdate + " " + deduct_signintime;
                      checkInfo.put("deduct_signintime", deduct_signintime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                    }
                  }
                }
                lsCheckInfo.add(checkInfo);
              } else {
                checkInfo.put("signType", "2");
                if (workEndDateTime.length() > 0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                }
                if (!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())) {
                  if (flow_deduct_card_map.containsKey("signoff")) {
                    String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
                    if (deduct_signofftime.length() > 0) {
                      signDateTime = signdate + " " + deduct_signofftime;
                      checkInfo.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                    }
                  }
                }

                lsCheckInfo.add(checkInfo);
              }
            } else {
              if (idx == 1) {//第一条算签到
                checkInfo.put("signType", "1");
                if (workBeginDateTime.length() > 0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                }
                if (!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())) {
                  if (flow_deduct_card_map.containsKey("signin")) {
                    String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
                    if (deduct_signintime.length() > 0) {

                      signDateTime = signdate + " " + deduct_signintime;
                      checkInfo.put("deduct_signintime", deduct_signintime);//流程抵扣作为打卡时间
                      checkInfo.put("signStatus", UtilKQ.getSignStatus("1", signDateTime, workBeginDateTime));
                    }
                  }
                }
                lsCheckInfo.add(checkInfo);
              } else if (idx == rs.getCounts()) {//最后一条算签退
                checkInfo.put("signType", "2");
                if (workEndDateTime.length() > 0) {
                  checkInfo.put("signStatus", UtilKQ.getSignStatus("2", signDateTime, workEndDateTime));
                }
                if (!Util.null2String(checkInfo.get("signStatus")).equalsIgnoreCase(ButtonStatusEnum.NORMAL.getStatusCode())) {
                  if (flow_deduct_card_map.containsKey("signoff")) {
                    String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
                    if (deduct_signofftime.length() > 0) {
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
      if (lsCheckInfo.size() < 2 && !flow_deduct_card_map.isEmpty()) {
        if (lsCheckInfo.isEmpty()) {
          if (flow_deduct_card_map.containsKey("signin")) {
            String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
            if (deduct_signintime.length() > 0) {
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
          if (flow_deduct_card_map.containsKey("signoff")) {
            String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
            if (deduct_signofftime.length() > 0) {
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
        } else {
          Map<String, Object> checkCardMap = (Map<String, Object>) lsCheckInfo.get(0);
          if (!checkCardMap.isEmpty()) {
            String signType = Util.null2String(checkCardMap.get("signType"));
            if ("1".equalsIgnoreCase(signType)) {
              //如果签到数据有了，检测下是不是有签退的流程抵扣打卡
              if (flow_deduct_card_map.containsKey("signoff")) {
                String deduct_signofftime = Util.null2String(flow_deduct_card_map.get("signoff"));
                if (deduct_signofftime.length() > 0) {
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
            } else {
              if (flow_deduct_card_map.containsKey("signin")) {
                String deduct_signintime = Util.null2String(flow_deduct_card_map.get("signin"));
                if (deduct_signintime.length() > 0) {
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
      // 上午的签退取中间时段的第一次打卡
      if (shiftCount == 2 && shiftI == 0) {
        String noonSignTimeSql = KQSignUtil.buildSignSql(count4NoonStartDateTime, count4NoonEndDateTime);
        String baseSql = "select * from hrmschedulesign where 1=1 and isInCom='1' and userid=" + userId + " ";
        String sql = baseSql;
        sql += " and " + noonSignTimeSql + " order by signdate, signtime";
        rs.executeQuery(sql);
        if (rs.next()) {
          String signId = Util.null2String(rs.getString("id"));
          String signDate = Util.null2String(rs.getString("signdate"));
          String signTime = Util.null2String(rs.getString("signtime"));
          String signDateTime = signDate + " " + signTime;
          for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
            Map<String, Object> checkInfoInner = (Map<String, Object>) lsCheckInfo.get(j);
            if (checkInfoInner.get("signType").equals("2")) {//签退
              String signDateInner = Util.null2String(checkInfoInner.get("signDate"));
              String signTimeInner = Util.null2String(checkInfoInner.get("signTime"));
              String deduct_signofftime = Util.null2String(checkInfoInner.get("deduct_signofftime"));
              if (!"".equals(signTimeInner)) {
                String signDateTimeInner = signDateInner + " " + signTimeInner;
                if (signDateTime.compareTo(signDateTimeInner) < 0) {
                  checkInfoInner.put("signId", signId);//签到签退标识
                  checkInfoInner.put("signType", "2");
                  checkInfoInner.put("signDate", signDateInner);//签到签退日期
                  checkInfoInner.put("signTime", signTime);//签到签退时间
                  checkInfoInner.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
                  checkInfoInner.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
                }
              }

              new BaseBean().writeLog("==zj==(checkInfoInner（完成）)" + JSON.toJSONString(checkInfoInner));
            }
          }
        }
      } else if (shiftCount == 2 && shiftI == 1) { // 下午的签到取中间时段的第二次打卡
        String noonSignTimeSql = KQSignUtil.buildSignSql(count4NoonStartDateTime, count4NoonEndDateTime);
        String baseSql = "select * from hrmschedulesign where 1=1 and isInCom='1' and userid=" + userId + " ";
        String sql = baseSql;
        sql += " and " + noonSignTimeSql + " order by signdate, signtime";
        new BaseBean().writeLog("==zj==（下午签到时间打卡sql）" + sql);
        rs.executeQuery(sql);
        int count = 0;
        /*int counts = rs.getCounts();
        if(counts==1){
          lsCheckInfo.clear();
        }*/
        //中午没第二次打卡，去下午上班时间到签到时间结束时间找
        boolean relatedFlag = false;//false表示中午时段没有第二次打卡
        while (rs.next()) {
          if (count == 0) {
            count++;
            continue;
          }
          String signId = Util.null2String(rs.getString("id"));
          String signTime = Util.null2String(rs.getString("signtime"));
          new BaseBean().writeLog("==zj==(lsCheckInfo)" + JSON.toJSONString(lsCheckInfo));
          for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
            Map<String, Object> checkInfoInner = (Map<String, Object>) lsCheckInfo.get(j);
            new BaseBean().writeLog("==zj==(下午checkInfoInner)" + checkInfoInner);
            if (checkInfoInner.get("signType").equals("1")) {//签退
              String signDateInner = Util.null2String(checkInfoInner.get("signDate"));
              String signTimeInner = Util.null2String(checkInfoInner.get("signTime"));
              String deduct_signofftime = Util.null2String(checkInfoInner.get("deduct_signofftime"));
              checkInfoInner.put("signId", signId);//签到签退标识
              checkInfoInner.put("signType", "1");
              checkInfoInner.put("signDate", signDateInner);//签到签退日期
              checkInfoInner.put("signTime", signTime);//签到签退时间
              checkInfoInner.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
              checkInfoInner.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
              relatedFlag = true;
              }
            }
            break;
          }


        //==zj==下班卡取距离下班之后最近的
        /*if (checkInfoInner.get("signType").equals("2")){//签退
          new BaseBean().writeLog("==zj==(下午下班签退初始化)" + JSON.toJSONString(checkInfoInner));
          String signDate = "";
          String signDateTime = "";
          sql = "select * from hrmschedulesign where 1=1 and isInCom='1' and userid=" + userId +
                  " and signDate+' '+ signTime>='" + signEndDateTime4Afternoon + "' order by signdate,signtime";
          new BaseBean().writeLog("==zj==（下班卡范围sql）" + sql);
          rs.executeQuery(sql);
          if (rs.next()){
            signId = Util.null2String(rs.getString("id"));
            signDate = Util.null2String(rs.getString("signdate"));
            signTime = Util.null2String(rs.getString("signtime"));
            signDateTime = signDate+" "+signTime;
          }

          String signDateInner = Util.null2String(checkInfoInner.get("signDate"));
          String signTimeInner = Util.null2String(checkInfoInner.get("signTime"));
          String deduct_signofftime = Util.null2String(checkInfoInner.get("deduct_signofftime"));

          if(!"".equals(signTimeInner)) {
            String signDateTimeInner = signDateInner+" "+signTimeInner;
            if(signDateTime.compareTo(signDateTimeInner) < 0) {
              checkInfoInner.put("signId", signId);//签到签退标识
              checkInfoInner.put("signType", "2");
              checkInfoInner.put("signDate", signDateInner);//签到签退日期
              checkInfoInner.put("signTime", signTime);//签到签退时间
              checkInfoInner.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
              checkInfoInner.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
            }
          }*/

          new BaseBean().writeLog("==zj==(下午下班签退处理完成)" + JSON.toJSONString(checkInfoInner));
        }

        if (!relatedFlag) {
          noonSignTimeSql = KQSignUtil.buildSignSql(count4NoonEndDateTime,signEndDateTime4Afternoon);
          baseSql = "select * from hrmschedulesign where 1=1 and isInCom='1' and userid="+userId+" ";
          sql = baseSql;
          sql += " and "+noonSignTimeSql + " order by signdate, signtime";
          rs.executeQuery(sql);
          boolean flag1 = false;
          while (rs.next()) {
            flag1 = true;
            String signId = Util.null2String(rs.getString("id"));
            String signTime = Util.null2String(rs.getString("signtime"));
            for (int j = 0; lsCheckInfo != null && j < lsCheckInfo.size(); j++) {
              Map<String, Object> checkInfoInner = (Map<String, Object>) lsCheckInfo.get(j);
              if (checkInfoInner.get("signType").equals("1")) {//签到
                String signDateInner = Util.null2String(checkInfoInner.get("signDate"));
                String signTimeInner = Util.null2String(checkInfoInner.get("signTime"));
                String deduct_signofftime = Util.null2String(checkInfoInner.get("deduct_signofftime"));
                checkInfoInner.put("signId", signId);//签到签退标识
                checkInfoInner.put("signType", "1");
                checkInfoInner.put("signDate", signDateInner);//签到签退日期
                checkInfoInner.put("signTime", signTime);//签到签退时间
                checkInfoInner.put("deduct_signofftime", deduct_signofftime);//流程抵扣作为打卡时间
                checkInfoInner.put("signStatus", ButtonStatusEnum.NORMAL.getStatusCode());
              }
            }
            if (lsCheckInfo != null && 1 < lsCheckInfo.size()) {
              Map<String, Object> checkInfoInner = (Map<String, Object>) lsCheckInfo.get(0);
              Map<String, Object> checkInfoInner2 = (Map<String, Object>) lsCheckInfo.get(1);
              if (checkInfoInner.get("signTime").equals(checkInfoInner2.get("signTime"))) {
                lsCheckInfo.remove(1);
              }
            }
            break;
          }
          if (!flag1) {
            for (int j = 0; lsCheckInfo != null && j <= lsCheckInfo.size(); j++) {
              lsCheckInfo.remove(j);
            }
          }
        }
      }
    } catch (Exception e) {

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
   *
   * @param userId
   * @param kqDate
   * @param workBeginDateTime
   * @param workEndDateTime
   */
  public Map<String, String> getflowDeductCardSql(String userId, String kqDate, String workBeginDateTime, String workEndDateTime) {
    Map<String, String> flow_deduct_card_map = new HashMap<>();
    RecordSet rs = new RecordSet();
    String flow_deduct_card_sql = "select * from kq_flow_deduct_card where 1=1 and (isclear is null or isclear<>1) ";
    if (userId.length() > 0) {
      flow_deduct_card_sql += " and resourceid=" + userId;
    }
    if (kqDate.length() > 0) {
      flow_deduct_card_sql += " and belongDate='" + kqDate + "'";
    }
    if (workBeginDateTime.length() > 0) {
      flow_deduct_card_sql += " and workBeginTime='" + workBeginDateTime + "'";
    }
    if (workEndDateTime.length() > 0) {
      flow_deduct_card_sql += " and workEndTime='" + workEndDateTime + "'";
    }
    rs.executeQuery(flow_deduct_card_sql);
    while (rs.next()) {
      String signtype = rs.getString("signtype");
      if ("1".equalsIgnoreCase(signtype)) {
        flow_deduct_card_map.put("signin", workBeginDateTime);
      }
      if ("2".equalsIgnoreCase(signtype)) {
        flow_deduct_card_map.put("signoff", workEndDateTime);
      }
    }
    return flow_deduct_card_map;
  }


  /**
   * 根据打卡的范围生成上班，下班数据获取的sql
   *
   * @param signTimeScope
   * @param kqDate
   * @param preDate
   * @param nextDate
   * @param kqTimesArrayComInfo
   * @return
   */
  public List<Map<String, String>> getCanSignInfo(TimeScopeEntity signTimeScope, String kqDate, String preDate, String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo) {
    return getCanSignInfo(signTimeScope, kqDate, preDate, nextDate, kqTimesArrayComInfo, null);
  }

  public List<Map<String, String>> getCanSignInfo(TimeScopeEntity signTimeScope, String kqDate, String preDate, String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo, TimeScopeEntity workTimeScope) {
    return getCanSignInfo(signTimeScope, kqDate, preDate, nextDate, kqTimesArrayComInfo, workTimeScope, 0, 0);
  }

  public List<Map<String, String>> getCanSignInfo(TimeScopeEntity signTimeScope, String kqDate, String preDate, String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo, TimeScopeEntity workTimeScope, int shiftCount, int shiftI) {

    List<Map<String, String>> sqlConditions = new ArrayList<>();
    Map<String, String> conditionMap = new HashMap<>();

    TimeSignScopeEntity timeSignScopeEntity = signTimeScope.getTimeSignScopeEntity();

    String signBeginDateTime = "";
    String signEndDateTime = "";
    signBeginDateTime = signTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
    if (signTimeScope.isBeginTimePreAcross()) {
      signBeginDateTime = preDate;
    }
    signBeginDateTime += " " + kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime()) + ":00";

    signEndDateTime = signTimeScope.getEndTimeAcross() ? nextDate : kqDate;

//    if (workTimeScope != null && workTimeScope.getEndTimeAcross() && signTimeScope.getEndTimeAcross() ) {
//      if(workTimeScope.getEndTime().compareTo(signTimeScope.getEndTime())>=0){
//        signEndDateTime = DateUtil.addDate(kqDate, 2);//下下一天日期;
//      }
//    }
    signEndDateTime += " " + kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime()) + ":59";


    if (timeSignScopeEntity == null) {
      //没有设置 签到最晚时间和签退最早时间
      conditionMap = new HashMap<>();
      conditionMap.put("signBeginDateTime", signBeginDateTime);
      conditionMap.put("signEndDateTime", signEndDateTime);
      sqlConditions.add(conditionMap);
    } else {
      String beginTimeEnd = timeSignScopeEntity.getBeginTimeEnd();
      boolean beginTimeEndAcross = timeSignScopeEntity.isBeginTimeEndAcross();

      String endTimeStart = timeSignScopeEntity.getEndTimeStart();
      boolean endTimeStartAcross = timeSignScopeEntity.isEndTimeStartAcross();

      if (beginTimeEnd.length() > 0) {
        //如果设置了 上班结束时间
        if (endTimeStart.length() > 0) {
          //设置了下班开始时间
          String signBeginDateEndTime = "";
          String signEndDateStartTime = "";
          signBeginDateEndTime = beginTimeEndAcross ? nextDate : kqDate;
          signBeginDateEndTime += " " + kqTimesArrayComInfo.turn48to24Time(beginTimeEnd) + ":59";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signBeginDateTime);
          conditionMap.put("signEndDateTime", signBeginDateEndTime);
          conditionMap.put("type", "signin");
          sqlConditions.add(conditionMap);

          signEndDateStartTime = endTimeStartAcross ? nextDate : kqDate;
          signEndDateStartTime += " " + kqTimesArrayComInfo.turn48to24Time(endTimeStart) + ":00";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signEndDateStartTime);
          conditionMap.put("signEndDateTime", signEndDateTime);
          conditionMap.put("type", "signoff");
          sqlConditions.add(conditionMap);
        } else {
          //没有设置下班开始时间
          String signBeginDateEndTime = "";
          String signEndDateStartTime = "";
          signBeginDateEndTime = beginTimeEndAcross ? nextDate : kqDate;
          signBeginDateEndTime += " " + kqTimesArrayComInfo.turn48to24Time(beginTimeEnd) + ":59";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signBeginDateTime);
          conditionMap.put("signEndDateTime", signBeginDateEndTime);
          conditionMap.put("type", "signin");
          sqlConditions.add(conditionMap);

          //如果设置了上班结束时间，相当于下班开始时间也被限定了
          String endTimeByBeginTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(beginTimeEnd) + 1);
          signEndDateStartTime = beginTimeEndAcross ? nextDate : kqDate;
          signEndDateStartTime += " " + kqTimesArrayComInfo.turn48to24Time(endTimeByBeginTime) + ":00";

          conditionMap = new HashMap<>();
          conditionMap.put("signBeginDateTime", signEndDateStartTime);
          conditionMap.put("signEndDateTime", signEndDateTime);
          conditionMap.put("type", "signoff");
          sqlConditions.add(conditionMap);
        }
      } else if (endTimeStart.length() > 0) {
        //如果没有设置上班结束时间，设置了下班开始时间
        String signBeginDateEndTime = "";
        String signEndDateStartTime = "";

        //如果设置了下班开始时间，相当于上班结束时间也被限定了
        String BeginTimeByendTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(endTimeStart) - 1);
        signBeginDateEndTime = endTimeStartAcross ? nextDate : kqDate;
        signBeginDateEndTime += " " + kqTimesArrayComInfo.turn48to24Time(BeginTimeByendTime) + ":59";

        conditionMap = new HashMap<>();
        conditionMap.put("signBeginDateTime", signBeginDateTime);
        conditionMap.put("signEndDateTime", signBeginDateEndTime);
        conditionMap.put("type", "signin");
        sqlConditions.add(conditionMap);

        signEndDateStartTime = endTimeStartAcross ? nextDate : kqDate;
        signEndDateStartTime += " " + kqTimesArrayComInfo.turn48to24Time(endTimeStart) + ":00";

        conditionMap = new HashMap<>();
        conditionMap.put("signBeginDateTime", signEndDateStartTime);
        conditionMap.put("signEndDateTime", signEndDateTime);
        conditionMap.put("type", "signoff");
        sqlConditions.add(conditionMap);
      }
    }
    return sqlConditions;
  }

  public String signSignSql(RecordSet rs) {
    String sql = "";
    if (rs.getDBType().equals("oracle")) {
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate||' '||signtime >= ? and signdate||' '||signtime <= ? " +
              " ";
    } else if ("sqlserver".equals(rs.getDBType())) {
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate + ' ' + signtime >= ? and signdate + ' ' + signtime <= ? " +
              " ";
    } else {
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
    if (rs.getDBType().equals("oracle")) {
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate||' '||signtime >= ? and signdate||' '||signtime <= ? " +
              " order by signdate asc, signtime asc ";
    } else if ("sqlserver".equals(rs.getDBType())) {
      sql = " select id,signdate,signtime from hrmschedulesign where isincom=1 and userid = ? and signdate + ' ' + signtime >= ? and signdate + ' ' + signtime <= ? " +
              " order by signdate asc, signtime asc ";
    } else {
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
      if (idx % 2 == 1) {
        checkInfo.put("signType", "1");
      } else {
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
    if (!pre_lsSignTime.isEmpty()) {
      TimeScopeEntity pre_signTimeScope = pre_lsSignTime.get(pre_lsSignTime.size() - 1);
      if (pre_signTimeScope.getEndTimeAcross()) {
        pre_Worktime4Today = pre_signTimeScope.getEndTime();
      }
    }
    String next_Worktime4Today = "";
    if (!next_lsSignTime.isEmpty()) {
      TimeScopeEntity next_signTimeScope = next_lsSignTime.get(next_lsSignTime.size() - 1);
      if (next_signTimeScope.isBeginTimePreAcross()) {
        next_Worktime4Today = next_signTimeScope.getBeginTime();
      }
    }
    String sql = "select * from hrmschedulesign where userid=" + userId + " and signdate = '" + kqDate + "' ";
    if (pre_Worktime4Today.length() > 0) {
      if (pre_Worktime4Today.length() == 5) {
        pre_Worktime4Today += ":59";
      }
      sql += " and signtime > '" + pre_Worktime4Today + "'";
    }
    if (next_Worktime4Today.length() > 0) {
      if (next_Worktime4Today.length() == 5) {
        next_Worktime4Today += ":00";
      }
      sql += " and signtime < '" + next_Worktime4Today + "'";
    }
    sql += " order by signdate asc,signtime asc ";
    rs.executeQuery(sql);
    int idx = 0;
    while (rs.next()) {
      String signId = Util.null2String(rs.getString("id"));
      String signdate = Util.null2String(rs.getString("signdate"));
      String signtime = Util.null2String(rs.getString("signtime"));

      checkInfo = new HashMap<>();
      checkInfo.put("signId", signId);//签到签退标识
      checkInfo.put("signDate", signdate);//签到签退日期
      checkInfo.put("signTime", signtime);//签到签退时间
      idx++;
      if (idx == 1) {//第一条算签到
        checkInfo.put("signType", "1");
        lsCheckInfo.add(checkInfo);
      } else if (idx == rs.getCounts()) {//最后一条算签退
        checkInfo.put("signType", "2");
        lsCheckInfo.add(checkInfo);
      }
    }
    return lsCheckInfo;
  }

}