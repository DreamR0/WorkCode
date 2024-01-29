package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.chain.cominfo.ShiftInfoCominfoBean;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;

import java.util.*;

public class KQWorkTime extends BaseBean {
  private KQLog kqLog = new KQLog();
  private boolean isFormat = false;

  public WorkTimeEntity getWorkTime(String userId) {
    return getWorkTime(userId, null);
  }

  /**
   * 判断是否是工作日
   * @param userId
   * @param workdate
   * @return
   */
  public boolean isWorkDay(String userId, String workdate) {
    boolean isWorkDay = false;
    if(!KQHolidaySetBiz.isHoliday(userId,workdate)) {//不是节假日，且有班次
      Map<String, Object> serialInfo = getSerialInfo( userId,  workdate,  false);
      if(!serialInfo.isEmpty()){
        if(Util.null2String(serialInfo.get("kqType")).equals("3")){
          Map<String, Object> result = (Map<String, Object>) serialInfo.get(workdate);
          if(result!=null && result.size()>0 && Util.null2String(result.get("signStart")).length()>0 && Util.null2String(result.get("workMins")).length()>0){
            isWorkDay = true;
          }
        }else{
          isWorkDay = Util.getIntValue(Util.null2String(serialInfo.get(workdate)))>0;
        }
      }
    }
    return isWorkDay;
  }

  public Map<String, Object> getWorkButton(String userId, String workdate, boolean containYesterday) {
    Map<String, Object> result = new HashMap<>();
    try {
      KQShiftManagementComInfo kQShiftManagementComInfo = new KQShiftManagementComInfo();
      Map<String,Object> serialInfo = getSerialInfo(userId, workdate, containYesterday);
      kqLog.info("考勤组获取成员所在的班次 getWorkButton:serialInfo:"+ serialInfo);
      String kqType = Util.null2String(serialInfo.get("kqType"));
      if(serialInfo!=null&&serialInfo.size()>0){
        if("3".equalsIgnoreCase(kqType)){
          //自由班制的单独处理
          result = (Map<String, Object>) serialInfo.get(workdate);
          if(result != null && !result.isEmpty()){
            result.put("isfree", "1");
          }else{
            result = new HashMap<>();
          }
        }else{
          result = kQShiftManagementComInfo.getWorkButton(workdate,serialInfo,containYesterday);
          kqLog.info("考勤组获取成员所在的班次 getWorkButton:result:"+ JSON.toJSONString(result));
        }
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return result;
  }

  /**
   *
   * 获取今天所含班次的考勤时间（包含今天和昨天的）
   * @param userId
   * @param workdate
   * @param containYesterday
   * @param isLog 是否记录日志
   * @return
   */
  public Map<String, Object> getWorkDuration(String userId, String workdate,boolean containYesterday,boolean isLog) {
    Map<String, Object> result = new HashMap<>();
    try {
      Map<String,Object> workTimeMap = null;
      KQShiftManagementComInfo kQShiftManagementComInfo = new KQShiftManagementComInfo();
      Map<String,Object> serialInfo = getSerialInfo(userId, workdate, true);
      if(isLog){
        kqLog.info("考勤组获取成员所在的班次 getWorkDuration:"+serialInfo);
      }
      if(serialInfo!=null&&serialInfo.size()>0){
        String kqType = Util.null2String(serialInfo.get("kqType"));
        if("3".equalsIgnoreCase(kqType)){
          //自由班制的单独处理
          result = (Map<String, Object>) serialInfo.get(workdate);
          if(result != null && !result.isEmpty()){
            result.put("isfree", "1");
          }else{
            result = new HashMap<>();
          }
        }else{
          workTimeMap = kQShiftManagementComInfo.getWorkDuration(workdate,serialInfo,containYesterday);
          if(workTimeMap!=null){
            if(isLog) {
              kqLog.info(
                  "考勤组获取成员所在的班次 getWorkDuration:workTimeMap:" + JSON.toJSONString(workTimeMap));
            }
            result.put("shiftInfoBean",workTimeMap.get("shiftInfoBean"));
          }
        }
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return result;
  }

  /**
   * 获取今天所含班次的考勤时间（包含今天和昨天的）
   * @param userId
   * @param workdate
   * @param containYesterday
   * @return
   */
  public Map<String, Object> getWorkDuration(String userId, String workdate,boolean containYesterday) {
    return getWorkDuration(userId,workdate,containYesterday,true);
  }

  /**
   * 获取今天所含班次的考勤时间（今天的）
   * @param userId
   * @param workdate
   * @return
   */
  public ShiftInfoCominfoBean getShiftInfoCominfoBean(String userId, String workdate) {
    ShiftInfoCominfoBean shiftInfoCominfoBean = null;
    try {
      Map<String,Object> workTimeMap = null;
      KQShiftManagementComInfo kQShiftManagementComInfo = new KQShiftManagementComInfo();
      Map<String,Object> serialInfo = getSerialInfo(userId, workdate, false);

      if(serialInfo!=null&&serialInfo.size()>0){
        shiftInfoCominfoBean = kQShiftManagementComInfo.getShiftInfoCominfoBean(workdate,serialInfo);
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return shiftInfoCominfoBean;
  }

  /**
   * 获取考勤时间
   * @param userId
   * @param workdate
   * @return
   */
  public WorkTimeEntity getWorkTime(String userId, String workdate) {
    WorkTimeEntity workTimeEntity = new WorkTimeEntity();
    try {
      KQShiftManagementComInfo kQShiftManagementComInfo = new KQShiftManagementComInfo();
      ShiftManagementToolKit shiftManagementToolKit = new ShiftManagementToolKit();
      Map<String,Object> serialInfo = getSerialInfo(userId, workdate, false);
      workTimeEntity.setIsExclude(Util.null2String(serialInfo.get("isExclude")).equals("1"));
      workTimeEntity.setGroupId(Util.null2String(serialInfo.get("groupId")));
      if(serialInfo!=null&&serialInfo.size()>0) {
        String kqType = Util.null2String(serialInfo.get("kqType"));
        if(kqType.equals("3")){
          Map<String,Object> map = (Map<String,Object>)serialInfo.get(workdate);
          workTimeEntity.setGroupId(Util.null2String(serialInfo.get("groupId")));
          workTimeEntity.setGroupName(Util.null2String(serialInfo.get("groupName")));
          workTimeEntity.setKQType(Util.null2String(kqType));
          workTimeEntity.setIsExclude(Util.null2String(serialInfo.get("isExclude")).equals("1"));
          if(map!=null) {
            workTimeEntity.setSignStart(Util.null2String(map.get("signStart")));
            workTimeEntity.setWorkMins(Util.getIntValue(Util.null2String(map.get("workMins"))));
            workTimeEntity.setCalmethod(Util.null2String(map.get("calmethod")));
          }
        }else{
          int serialid = Util.getIntValue(Util.null2String(serialInfo.get(workdate)), 0);
          if (serialid > 0){
            Map<String,Object> dateWorkTimeMap = kQShiftManagementComInfo.getWorkTimeMap(workdate, serialInfo);
            workTimeEntity.setGroupId(Util.null2String(serialInfo.get("groupId")));
            workTimeEntity.setGroupName(Util.null2String(serialInfo.get("groupName")));
            workTimeEntity.setKQType(kqType);
            workTimeEntity.setSerialId(""+serialid);
            workTimeEntity.setShiftRuleInfo(ShiftManagementToolKit.getShiftRuleInfo(""+serialid,true));
            workTimeEntity.setSignTime(formatTimeScope((List<Object>)dateWorkTimeMap.get("signTime"),false));
            workTimeEntity.setWorkTime(formatTimeScope((List<Object>)dateWorkTimeMap.get("workTime"),true));
            workTimeEntity.setRestTime(formatTimeScope((List<Object>)dateWorkTimeMap.get("restTime"),false));
            workTimeEntity.setWorkMins(Util.getIntValue(Util.null2String(dateWorkTimeMap.get("workMins"))));
            workTimeEntity.setIsAcross(Util.null2String(dateWorkTimeMap.get("isAcross")));
          }
        }
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return workTimeEntity;
  }

  public List<TimeScopeEntity> formatTimeScope(List<Object> timeScope, boolean needWorkMins){
    List<TimeScopeEntity> timeScopes = new ArrayList<>();
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
    TimeScopeEntity timeScopeEntity = null;
    for(int i=0;timeScope!=null && i<timeScope.size();i++){
      Map<String,Object> obj = (Map<String,Object>)timeScope.get(i);
      String bengintime_end = Util.null2String(obj.get("bengintime_end"));
      String bengintime_end_across = Util.null2String(obj.get("bengintime_end_across"));
      String endtime_start = Util.null2String(obj.get("endtime_start"));
      String endtime_start_across = Util.null2String(obj.get("endtime_start_across"));
      String bengintime_pre_across = Util.null2String(obj.get("bengintime_pre_across"));
      timeScopeEntity = new TimeScopeEntity();
      timeScopeEntity.setBeginTime(Util.null2String(obj.get("bengintime")));
      timeScopeEntity.setBeginTimeAcross(Util.null2String(obj.get("bengintime_across")).equals("1"));//标记是否跨天
      timeScopeEntity.setEndTime(Util.null2String(obj.get("endtime")));
      timeScopeEntity.setEndTimeAcross(Util.null2String(obj.get("endtime_across")).equals("1"));//标记是否跨天
      timeScopeEntity.setBeginTimePreAcross("1".equalsIgnoreCase(bengintime_pre_across));

      if(needWorkMins) {
        int workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(timeScopeEntity.getBeginTime());
        int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(timeScopeEntity.getEndTime());
        timeScopeEntity.setWorkMins(workEndIdx - workBeginIdx);
      }
      if((bengintime_end != null && bengintime_end.length() >0) || (endtime_start != null && endtime_start.length() > 0)){
        TimeSignScopeEntity timeSignScopeEntity = new TimeSignScopeEntity();
        timeSignScopeEntity.setBeginTimeEnd(bengintime_end);
        timeSignScopeEntity.setBeginTimeEndAcross("1".equalsIgnoreCase(bengintime_end_across));
        timeSignScopeEntity.setEndTimeStart(endtime_start);
        timeSignScopeEntity.setEndTimeStartAcross("1".equalsIgnoreCase(endtime_start_across));
        timeSignScopeEntity.setBeginTimePreAcross("1".equalsIgnoreCase(bengintime_pre_across));
        timeScopeEntity.setTimeSignScopeEntity(timeSignScopeEntity);
      }
      timeScopes.add(timeScopeEntity);
    }
    return timeScopes;
  }

  /**
   * 获取当天班次
   * @param userId
   * @param workdate
   * @return
   */
  public String getSerialIds(String userId, String workdate) {
    Map<String, Object> serialInfo = getSerialInfo( userId,  workdate,  false);
    return serialInfo!=null?Util.null2String(serialInfo.get(workdate)):"";
  }

  /**
   * 获取班次信息 获取顺序 工作日调整、排班、固定班和周期班
   * @param userId
   * @param workdate
   * @param containYesterday
   * @return
   */
  public Map<String,Object> getSerialInfo(String userId, String workdate, boolean containYesterday) {
    Map<String, Object> serialInfo = new HashMap<>();
    String preworkdate = "";
    try {
      KQGroupMemberComInfo groupMemberComInfo = new KQGroupMemberComInfo();
      groupMemberComInfo.setIsFormat(this.isFormat);
      KQFixedSchedulceComInfo kqFixedSchedulceComInfo = new KQFixedSchedulceComInfo();
      kqFixedSchedulceComInfo.setFormat(this.isFormat);
      KQShiftScheduleComInfo kqShiftScheduleComInfo = new KQShiftScheduleComInfo();
      kqShiftScheduleComInfo.setFormat(this.isFormat);
      KQGroupEntity kqGroupEntity = groupMemberComInfo.getUserKQGroupInfo(userId,workdate);
      ResourceComInfo resourceComInfo = new ResourceComInfo();

      preworkdate = DateUtil.addDate(workdate,-1);
      if(containYesterday){
        Map<String, Object> pre_serialInfo = getSerialInfo(userId, preworkdate, false);
        if(pre_serialInfo != null && !pre_serialInfo.isEmpty()){
          if(pre_serialInfo.containsKey(preworkdate)){
            serialInfo.put(preworkdate,pre_serialInfo.get(preworkdate));//获取前一天的班次
          }
        }
      }

      if(kqGroupEntity==null){//不在考勤组内
        return serialInfo;
      }


      //无需考勤人员需要计算考勤时间，但不计算异常状态
//      if (("," + kqGroupEntity.getExcludeid() + ",").indexOf("," + userId + ",")>-1) {//排除人员无需计算考勤时间
//        return serialInfo;
//      }

      if (("," + kqGroupEntity.getExcludeid() + ",").indexOf("," + userId + ",")>-1) {//排除人员无需计算考勤时间
        serialInfo.put("isExclude","1");
      }

      String begindate = Util.null2String(resourceComInfo.getCreatedate(userId)).trim();
      String companyStartDate = Util.null2String(resourceComInfo.getCompanyStartDate(userId)).trim();
      if(companyStartDate.length()!=10){
        companyStartDate = "";
      }
      if(companyStartDate.length()>0 && companyStartDate.indexOf("-")>0){
        begindate=companyStartDate;
      }
      if(begindate.length()>0 && DateUtil.compDate(begindate,workdate)<0 ){//人员入职日期前无需计算考勤，如果没有入职日期，已创建日期为准
//        kqLog.writeLog("getSerialInfo 入职日期不满足条件:userId:"+userId+":workdate:"+workdate+":companyStartDate:"+companyStartDate+":begindate:"+begindate+":DateUtil.compDate(begindate,workdate):"+DateUtil.compDate(begindate,workdate));
        return serialInfo;
      }

      String endDate = Util.null2String(resourceComInfo.getEndDate(userId));

      String status = Util.null2String(resourceComInfo.getStatus(userId));
      if(status.equals("0")||status.equals("1")||status.equals("2")||status.equals("3")){
        //在职
      }else{
        //其他状态
        if(endDate.length()>0  && DateUtil.compDate(endDate,workdate)>0){//人员合同结束日期无需计算考勤
//          kqLog.writeLog("getSerialInfo 人员合同结束日期不满足条件:userId:"+userId+":workdate:"+workdate+":endDate:"+endDate+":status:"+status+":DateUtil.compDate(endDate,workdate):"+DateUtil.compDate(endDate,workdate));
          return serialInfo;
        }
      }

      String groupid = kqGroupEntity.getId();
      String groupname = kqGroupEntity.getGroupname();
      String kqtype = kqGroupEntity.getKqtype();
      int dayOfweek = DateUtil.getWeek(workdate)-1;
      int preDayOfweek = DateUtil.getWeek(preworkdate)-1;
      boolean preDayIsHoliday = KQHolidaySetBiz.isHoliday(userId,preworkdate);
      boolean isHoliday = KQHolidaySetBiz.isHoliday(userId,workdate);
      String serialid = "";

      if(!kqtype.equals("2")){//处理调配工作日(除排班外)
        if(KQHolidaySetBiz.getChangeType(groupid,preworkdate)==2){
          preDayOfweek = KQHolidaySetBiz.getRelatedDay(userId,preworkdate);
        }

        if(KQHolidaySetBiz.getChangeType(groupid,workdate)==2){
          dayOfweek = KQHolidaySetBiz.getRelatedDay(userId,workdate);
        }
      }
      serialInfo.put("groupId",groupid);
      serialInfo.put("groupName",groupname);
      serialInfo.put("kqType",kqtype);
      serialInfo.put("isHoliday",isHoliday);
      if (kqtype.equals("1")) {//固定班
//          if(containYesterday && !serialInfo.containsKey(preworkdate)) {
//            serialid = Util.null2String(kqFixedSchedulceComInfo.getSerialid(groupid,preDayOfweek));
//            if(!preDayIsHoliday&&serialid.length()>0 && Util.getIntValue(serialid) > 0){
//              serialInfo.put(preworkdate,serialid);//获取前一天的班次
//            }
//          }
          if(!serialInfo.containsKey(workdate)){
            serialid = Util.null2String(kqFixedSchedulceComInfo.getSerialid(groupid,dayOfweek));
            if( !isHoliday&&serialid.length()>0 && Util.getIntValue(serialid) > 0){
              serialInfo.put(workdate, serialid);//获取当天的班次
            }
          }
      } else if (kqtype.equals("2")) {//排班
        //先取排班设置里的班次
//        serialid = Util.null2String(kqShiftScheduleComInfo.getSerialId(userId,preworkdate));
//        if(containYesterday && serialid.length()>0 && !preDayIsHoliday && Util.getIntValue(serialid) > 0){
//          serialInfo.put(preworkdate,Util.null2String(kqShiftScheduleComInfo.getSerialId(userId,preworkdate)));//获取前一天的班次
//        }
        serialid = Util.null2String(kqShiftScheduleComInfo.getSerialId(userId,workdate));
        if(serialid.length()>0 && !isHoliday && Util.getIntValue(serialid) > 0){
          serialInfo.put(workdate,Util.null2String(kqShiftScheduleComInfo.getSerialId(userId,workdate)));//获取当天的班次
        }
      } else if (kqtype.equals("3")) {//自由班
        List weekDay = Util.splitString2List(kqGroupEntity.getWeekday(), ",");
        String signStart = Util.null2String(kqGroupEntity.getSignstart());//签到开始时间
        int workMins = Util.getIntValue(Util.getIntValues(""+Util.getDoubleValue(Util.null2String(kqGroupEntity.getWorkhour()))*60));//工作时长
        if(signStart.length()>0 && workMins>0) {
          String calmethod = Util.null2s(kqGroupEntity.getCalmethod(),"2");
          Map<String, Object> map = null;
          if (weekDay.contains(""+preDayOfweek) && !preDayIsHoliday) {//前一天
            map = new HashMap<>();
            map.put("signStart", signStart);
            map.put("workMins", workMins);
            map.put("calmethod", calmethod);
            serialInfo.put(preworkdate, map);
          }
          if (weekDay.contains(""+dayOfweek) && !isHoliday) {//当前天
            map = new HashMap<>();
            map.put("signStart", signStart);
            map.put("workMins", workMins);
            map.put("calmethod", calmethod);
            serialInfo.put(workdate, map);
          }
        }
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return serialInfo;
  }


  public void setIsFormat(boolean isFormat){
    this.isFormat = isFormat;
  }
}