package com.api.customization.qc20230816.util;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc20230816.bean.KqTimeBean;
import com.engine.kq.biz.*;
import com.engine.kq.entity.KQGroupEntity;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;

import java.util.*;

public class KqFormatUtil {

    /**
     * 这里面处理下上下午时间段
     * @param dayMins
     */
    public List<Integer> attendanceHalf(String userId,String kqDate,int[]dayMins,int workBeginIdx,int workEndIdx){
        new BaseBean().writeLog("==zj==(班次时间)" + kqDate + " | " +  workBeginIdx + "  " + workEndIdx);
        ArrayList<Integer> dayTimes = new ArrayList<>();
        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
        String halfcalpoint = "";   //半天分界时间点
        int halfcalpointIdx = 0;
        RecordSet rs = new RecordSet();
        KQWorkTime kt = new KQWorkTime();


        try {
            String serialIds = kt.getSerialIds(userId, kqDate);     //获取当天班次id
            //获取当前人员日期的班次信息,半天计算规则 0：总工作时长的一般  1：自定义规则
            String halfSql = "select * from kq_ShiftManagement where id = " + serialIds + " and halfcalrule <> 0";
            new BaseBean().writeLog("==zj==(halfSql)" + JSON.toJSONString(halfSql));
            rs.executeQuery(halfSql);
            if (rs.next()){
                halfcalpoint = Util.null2String(rs.getString("halfcalpoint"));
            }
            //检查半天分界时间点

            if (halfcalpoint.length() > 0){
                if ("00:00".equals(halfcalpoint)){
                    halfcalpointIdx = kqTimesArrayComInfo.getArrayindexByTimes("24:00");

                }else if ("01:00".equals(halfcalpoint)){
                    halfcalpointIdx = kqTimesArrayComInfo.getArrayindexByTimes("01:00");
                    halfcalpointIdx += 1440;
                }else {
                    halfcalpointIdx = kqTimesArrayComInfo.getArrayindexByTimes(halfcalpoint);
                }

            }else {
                //没有就按照标准，不分上下午
                dayTimes.add(workBeginIdx);
                dayTimes.add(workEndIdx);
                return dayTimes;
            }
            //将上下午时间段封装下
            new BaseBean().writeLog("==zj==(halfcalpointIdx)" + JSON.toJSONString(halfcalpoint));
            if (halfcalpointIdx != 0){
                dayTimes.add(workBeginIdx);
                dayTimes.add(halfcalpointIdx);
                dayTimes.add(halfcalpointIdx);
                dayTimes.add(workEndIdx);
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(上下午出勤时长计算错误)" + JSON.toJSONString(e));
        }
       return dayTimes;
    }

    /**
     * 计算上下午出勤天数,对上下午数据处理
     * @param kqTimeBean
     * @return
     */
    public KqTimeBean KqTimeBeanCal(KqTimeBean kqTimeBean){
        int tmpBeLateMins = kqTimeBean.getTmpBeLateMins();  //迟到
        int tmpLeaveEarlyMins = kqTimeBean.getTmpLeaveEarlyMins();  //早退
        int tmpAbsenteeismMins = kqTimeBean.getTmpAbsenteeismMins(); //旷工
        int tmpLeaveMins = kqTimeBean.getTmpLeaveMins(); //请假
        int tmpForgotBeginWorkCheckMins = kqTimeBean.getTmpForgotBeginWorkCheckMins();//上班漏签
        int tmpForgotCheckMins = kqTimeBean.getTmpForgotCheckMins();                  //下班漏签

        Boolean isAbsenteeism = false;  //是否旷工
        double attendanceDays = 0.5;    //出勤天数
        int absenteeismMins = 0;   //旷工时长
        double absenteeismDays = 0.0;//旷工天数

        //如果迟到或早退大于30分钟，或者旷工。扣除所在半天时长
        if (tmpBeLateMins >= 30 || tmpLeaveEarlyMins >= 30 || tmpAbsenteeismMins > 0 || tmpForgotBeginWorkCheckMins >0 || tmpForgotCheckMins > 0){
            isAbsenteeism = true;
            attendanceDays = 0.0;
            absenteeismMins = 240;
            absenteeismDays = 0.5;

           /* kqTimeBean.setAbsenteeism(true);    //标记为旷工
            kqTimeBean.setTmpAttendanceDays(0.0);//出勤天数设置为0.0
            kqTimeBean.setTmpAbsenteeismMins(240);//旷工时长设置为4小时 -- 240分钟*/
        }else if (tmpLeaveMins > 0){
            attendanceDays = 0.0;
        }
       /* //这里在判断当天是否有请假时长
        if (tmpLeaveMins > 0){
            //根据需求请假半天都扣除4小时  --  240分钟
            kqTimeBean.setTmpAttendanceDays(0.0);   //出勤天数 - 0.5天
        }*/

        kqTimeBean.setAbsenteeism(isAbsenteeism);
        kqTimeBean.setTmpAttendanceDays(attendanceDays);
        kqTimeBean.setTmpAbsenteeismMins(absenteeismMins);
        kqTimeBean.setAbsenteeismDays(absenteeismDays);

        return kqTimeBean;
    }

    /**
     * 这里计算上班工时
     * @param workMins
     * @return
     */
    public int workMinsCustomCal(int workMins, KqTimeBean kqTimeBeanAM, KqTimeBean kqTimeBeanPM, Map<String,Object> leaveInfo){
        String leaveType = "";//假期类型
        Boolean absenteeismAM = kqTimeBeanAM.getAbsenteeism(); //上午旷工
        Boolean absenteeismPM = kqTimeBeanPM.getAbsenteeism(); //下午旷工
        Boolean tmpLeaveMinsAM = kqTimeBeanAM.getTmpLeaveMins() > 0 ? true:false; //上午请假
        Boolean tmpLeaveMinsPM = kqTimeBeanPM.getTmpLeaveMins() > 0 ? true:false; //下午请假
        String leaveName = "";
        int realWorkMins = 480 ; //实际工作时长

        if (!absenteeismAM && !absenteeismPM && !tmpLeaveMinsAM && !tmpLeaveMinsPM){
            //如果当天不存在任何旷工或请假，按照当天班次时长计算工作工时
            realWorkMins = workMins;
            return realWorkMins;
        }

        //如果上下午存在旷工情况
        if (absenteeismAM || absenteeismPM){
            if (absenteeismAM && absenteeismPM){
                //如果上下午都旷工，当天工作时长为0
                realWorkMins = 0;
            }else {
                //因为某半天存在旷工，则当天工作时长为8小时，并扣除半天
                realWorkMins -= 240;
            }
        }

        //如果上下午存在请假时长
        if(tmpLeaveMinsAM || tmpLeaveMinsPM){
            if (tmpLeaveMinsAM && tmpLeaveMinsPM){
                //如果上下午都请假，当天工作时长为0
                realWorkMins = 0;
            }else {
                //因为某半天存在请假，则当天工作时长为8小时，并扣除半天
                realWorkMins -= 240;
            }

            //如果请假类型为哺乳假，工时就加1小时，不超过当天班次工作时长
            String sql = "";
            RecordSet rs = new RecordSet();

            Iterator<String> leaveKeys = leaveInfo.keySet().iterator();
            while (leaveKeys.hasNext()){
                sql = "select leavename from kq_LeaveRules where id='"+leaveKeys.next()+"'";
                rs.executeQuery(sql);
                if (rs.next()){
                    leaveName = Util.null2String(rs.getString(sql));
                    if (leaveName.contains("哺乳")){
                        //如果该假期为哺乳类型
                        //不超过当前班次工作时长
                        realWorkMins  =realWorkMins + 60 ;
                        realWorkMins = realWorkMins > workMins? workMins:realWorkMins;
                    }
                }

            }
        }
        return realWorkMins;
    }



    /**
     * 按0.5天向上取整
     * @param days
     * @return
     */
    public String halfDayCount(String days){
        //乘2，向上取整，除以2
        double halfDays = Double.parseDouble(days);
        Double floor = Math.ceil(halfDays * 2);
        return Util.null2String(floor/2);
    }

    /**
     *获取班次名称
     * @param serialId
     * @return
     */
    public String getSerialName(int serialId,String text,Boolean isExport){
        String serialName = "";
        RecordSet rs = new RecordSet();
        String sql = "select serial from KQ_SHIFTMANAGEMENT where id = "+serialId;
        rs.executeQuery(sql);
        if (rs.next()){
             serialName = Util.null2String(rs.getString("serial"));
        }
        /*signouttime = isWrap?"\r\n"+signouttime:"<br/>"+signouttime;*/

        text = isExport?serialName+"\r\n" + text:serialName+"<br/>"+text;

        return text;
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
            groupMemberComInfo.setIsFormat(false);
            KQFixedSchedulceComInfo kqFixedSchedulceComInfo = new KQFixedSchedulceComInfo();
            kqFixedSchedulceComInfo.setFormat(false);
            KQShiftScheduleComInfo kqShiftScheduleComInfo = new KQShiftScheduleComInfo();
            kqShiftScheduleComInfo.setFormat(false);
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
                    if( serialid.length()>0 && Util.getIntValue(serialid) > 0){
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
                if(serialid.length()>0  && Util.getIntValue(serialid) > 0){
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
                    if (weekDay.contains(""+dayOfweek)) {//当前天
                        map = new HashMap<>();
                        map.put("signStart", signStart);
                        map.put("workMins", workMins);
                        map.put("calmethod", calmethod);
                        serialInfo.put(workdate, map);
                    }
                }
            }
        } catch (Exception e) {
        }
        return serialInfo;
    }


    /**
     * 获取上下午考勤状态
     * @param kqTimeBean
     * @return
     */
    public String getKqStatus(KqTimeBean kqTimeBean){
        String status = "";
        Boolean isHave = false;

        //请假按半天计算，所以只要出现请假时长，半天就是请假
        if (kqTimeBean.getTmpLeaveMins() > 0 && !isHave){
            //请假
            status = "leave";
            isHave = true;
        }

        //如果出现旷工时长就是半天旷工
        if (kqTimeBean.getTmpAbsenteeismMins() > 0 && !isHave){
            status = "absenteeism";
            isHave = true;
        }

        //考勤异常
      if ((kqTimeBean.getTmpBeLateMins() > 0 && kqTimeBean.getTmpBeLateMins() < 30) && !isHave){
          //迟到时长小于30分钟，算迟到
          status = "belate";
          isHave = true;
      }else if ((kqTimeBean.getTmpLeaveEarlyMins() > 0 && kqTimeBean.getTmpLeaveEarlyMins() < 30) && !isHave){
            //早退时长小于30分钟，算早退
            status = "leaveEarly";
            isHave = true;
        }



        if (kqTimeBean.getTmpEvectionMins() > 0 && !isHave){
            //出差
            status = "evection";
            isHave = true;
        }

        if (kqTimeBean.getTmpOutMins() > 0 && !isHave){
            //公出
            status = "out";
            isHave = true;
        }

        //只要出现漏签就记为旷工
        if (kqTimeBean.getTmpForgotCheckMins() > 0 && !isHave){
            status = "absenteeism";
            isHave = true;
        }

        //只要出现漏签就记为旷工
        if (kqTimeBean.getTmpForgotBeginWorkCheckMins() > 0 && !isHave){
            status = "absenteeism";
            isHave = true;
        }

        //状态正常
        if (!isHave){
            status = "normal";
        }


        return status;
    }

}
