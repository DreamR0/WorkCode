package com.customization.qc2238872;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.util.KQDurationCalculatorUtil;
import org.joda.time.DateTimeConstants;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.prefs.BackingStoreException;

public class kqUtil {


    /**
     * 返回分钟数
     *
     * @param Mins
     * @return
     */
    public String setFieldvalue(String Mins) {
        if ("0.0".equals(Mins)) {
            return "00";
        } else {
            return KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(Mins) * 60.0)));

        }
    }

    /**
     * 计算每天早班晚班次数
     *
     * @param attendanceMins
     * @return
     */
    public Double shiftCount(int attendanceMins) {
        double shiftCount = 0.0;
        new BaseBean().writeLog("==zj==(计算出勤时长--分钟)" + attendanceMins);
        double attendanceHours = attendanceMins / 60;      //出勤时长
        new BaseBean().writeLog("==zj==(计算出勤时长--小时)" + attendanceHours);
        if (attendanceHours >= 8 && attendanceHours < 12) {
            shiftCount = 0.5;
            return shiftCount;
        } else if (attendanceHours >= 12) {
            shiftCount = 1;
            return shiftCount;
        } else {
            return shiftCount;
        }
    }
    /**
     * 计算除了调休之外的所有考勤时长
     */
  public String leaveCount(Map<String,Object> flowData,String id,String fromDate,String toDate,String earlyShift,String nightSift){
      KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
      List<Map<String, Object>> allLeaveRules = kqLeaveRulesBiz.getAllLeaveRules();
      Map<String, Object> leaveRule = null;
      String value = "0";
      double sum = 0;               //平时加班
      double leaveCount = 0;        //假期时长
      double valueD = 0;
      for(int i=0;allLeaveRules!=null&&i<allLeaveRules.size();i++){
          leaveRule = (Map<String, Object>)allLeaveRules.get(i);
          new BaseBean().writeLog("==zj==(leaveRule)" + JSON.toJSONString(leaveRule));
          //如果是调休就跳出当前循环
          String isTiaoxiu = leaveRule.get("id").toString();
          new BaseBean().writeLog("==zj==(判断标识)" + isTiaoxiu);
          new BaseBean().writeLog("==zj==(调休判断)" + KQLeaveRulesBiz.isTiaoXiu(isTiaoxiu));

          if (KQLeaveRulesBiz.isTiaoXiu(isTiaoxiu)){
              continue;
          }
          String flowType = Util.null2String("leaveType_"+leaveRule.get("id"));
          String leaveData = Util.null2String(flowData.get(id+"|"+flowType));
          String flowLeaveBackType = Util.null2String("leavebackType_"+leaveRule.get("id"));
          String leavebackData = Util.null2s(Util.null2String(flowData.get(id+"|"+flowLeaveBackType)),"0.0");
          String b_flowLeaveData = "";
          String flowLeaveData = "";
          try{
              //以防止出现精度问题
              if(leaveData.length() == 0){
                  leaveData = "0.0";
              }
              if(leavebackData.length() == 0){
                  leavebackData = "0.0";
              }
              BigDecimal b_leaveData = new BigDecimal(leaveData);
              BigDecimal b_leavebackData = new BigDecimal(leavebackData);
              b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
              if(Util.getDoubleValue(b_flowLeaveData, -1) < 0){
                  b_flowLeaveData = "0.0";
              }
          }catch (Exception e){

              new BaseBean().writeLog("==zj==(计算所有请假时长错误)" + e);
          }
          //考虑下冻结的数据
          if(b_flowLeaveData.length() > 0){
              flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
          }else{
              flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData,0.0)-Util.getDoubleValue(leavebackData,0.0)));
          }

          //这里对所有请假类型时长按小时来换算  1--天   2--按半天  3--按小时  4--按整天  5--按半小时  6--按整小时
          String unitType = leaveRule.get("unitType").toString();
          new BaseBean().writeLog("==zj==(请假单位)" + unitType);
          if ("1".equals(unitType) || "2".equals(unitType) || "4".equals(unitType)){
                //如果是天，半天，或整天就直接*8
              valueD = Util.getDoubleValue(flowLeaveData) * 8.0;
          }

          if ("5".equals(unitType)){
              //如果单位为半小时
              valueD = Util.getDoubleValue(flowLeaveData) * 2.0;
          }

          if ("3".equals(unitType) || "6".equals(unitType)){
              //如果单位为小时
              valueD = Util.getDoubleValue(flowLeaveData);
          }
          new BaseBean().writeLog("==zj==(请假类型时长)" + flowType + " | "+valueD);
          //计算出所有假期时长
          leaveCount += valueD;
          //计算每个月应出勤天数
          Map<String, Object> result = workDaysCount(fromDate, toDate);
          int workDays = Integer.parseInt(Util.null2String(result.get("workDays")));
          //获取白班，夜班个数
          new BaseBean().writeLog("==zj==(获取白班、夜班个数)" + earlyShift +" | " + nightSift);
          if (earlyShift == null || "".equals(earlyShift)){
              earlyShift="0.0";
          }
          if (nightSift == null || "".equals(nightSift)){
              nightSift="0.0";
          }
          double earlyShiftCount = Double.parseDouble(earlyShift);
          double nightShiftCount = Double.parseDouble(nightSift);
          //平时加班= （白班个数+夜班个数）*12 - 每个月应出勤天数*8+假期时长（除调休之外）
          new BaseBean().writeLog("==zj==(平时加班计算公式) sum=("+earlyShiftCount +"+"+nightShiftCount+") * 12"+" - ("+workDays+" * 8 + "+leaveCount);
          sum = (earlyShiftCount + nightShiftCount) * 12 - workDays * 8 + leaveCount;

          //如果计算小于0,则显示为0
          if (sum<0){
              sum = 0.0;
          }
          value = KQDurationCalculatorUtil.getDurationRound(Util.null2String(sum));
          new BaseBean().writeLog("计算平时加班时长" + value);
      }

        return value;
  }

    /**
     * 这是计算应出勤天数,和应出勤时长
     */

    public Map<String,Object> workDaysCount(String fromDate,String toDate){
        RecordSet rs = new RecordSet();
        Map<String,Object> result = new HashMap<String,Object>();
        String sql = "";
        int otherDays = 0;       //其它类型天数
        int workDays = 0;       //应出勤天数
        int workDaysSum = 0;    //当前范围内天数
        int workMins = 0;       //应工作时长
        String groupSetId = new BaseBean().getPropValue("qc2238872","groupSetId"); //获取考勤组id
        new BaseBean().writeLog("==zj==（节假日获取考勤组id）"+ groupSetId);

        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sds= new SimpleDateFormat("yyyy-MM");
        Date nowDateD = new Date(System.currentTimeMillis());

        try {
            Date fromDateD = formatter.parse(fromDate);
            Date toDateD = formatter.parse(toDate);
            String nowDate = formatter.format(nowDateD);

            //如果开始日期在当前日期之后
            if (fromDateD.after(nowDateD)){
                result.put("workDays",0);
                return result;
            }

                if (toDateD.before(nowDateD)){
                    //如果结束日期在当前日期之前
                    sql = "select count(*) as count from kq_HolidaySet where holidayDate between '"+fromDate+"' and '"+toDate+"' and changeType  in(1,3) and groupid="+groupSetId;
                    rs. executeQuery(sql);
                    new BaseBean().writeLog("==zj==(结束日期在当前日期之前sql)" +sql);
                    if (rs.next()){
                        //获得范围其它类型天数
                        otherDays = rs.getInt("count");
                        //获得范围内天数
                        workDaysSum = fun(fromDate,toDate);
                        workDaysSum += 1;
                        //获得范围内几个周六日
                        int weekCount = weekCount(fromDate, toDate, groupSetId);
                        //应出勤天数 = 范围内天数 - 范围其它类型天数
                        workDays = workDaysSum - otherDays - weekCount;

                        new BaseBean().writeLog("==zj==(获得应出勤天数1)" + workDays + "="+workDaysSum+" - "+otherDays+" - "+weekCount);
                        if (workDays > 0){
                            workMins = workDays * 8;
                        }
                    }
                    result.put("workDays",workDays);
                    result.put("workMins",workMins);
                }

                if (toDateD.after(nowDateD)){
                    //如果结束日期在当前日期之后
                    sql = "select count(*) as count from kq_HolidaySet where holidayDate between '"+fromDate+"' and '"+nowDate+"' and changeType  in(1,3) and groupid="+groupSetId;
                    rs. executeQuery(sql);
                    new BaseBean().writeLog("==zj==(结束日期在当前日期之后sql)" +sql);
                    if (rs.next()){
                        //获得范围其它类型天数
                        otherDays = rs.getInt("count");
                        //获得范围内天数
                        workDaysSum = fun(fromDate,nowDate);
                        workDaysSum += 1;
                        //获得范围内几个周六日
                        int weekCount = weekCount(fromDate, nowDate, groupSetId);
                        //应出勤天数 = 范围内天数 - 范围其它类型天数 - 周六日
                        workDays = workDaysSum - otherDays - weekCount;
                        new BaseBean().writeLog("==zj==(获得应出勤天数2)" + workDays + "="+workDaysSum+" - "+otherDays+" - "+weekCount);

                        if (workDays > 0){
                            workMins = workDays * 8;
                        }
                    }
                    result.put("workDays",workDays);
                    result.put("workMins",workMins);
                }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==计算应出勤天数错误" + e);
        }

        return result;
    }

    /**
     * 获取范围内天数
     */
    public int fun(String s1,String s2){
        //指定日期格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        //按照指定格式转化为LocalDate对象
        LocalDate time1 = LocalDate.parse(s1,dateTimeFormatter);
        LocalDate time2 = LocalDate.parse(s2,dateTimeFormatter);
        //调方法计算两个LocalDate的天数差
        long between = ChronoUnit.DAYS.between(time1, time2);
        return (int)between;
    }

    /**
     * 计算时间范围内有几个周六日
     * @param fromdate
     * @param todate
     * @return
     */
    public int weekCount(String fromdate,String todate,String groupSetId){
        RecordSet rs = new RecordSet();
        String date = fromdate;
        int  weekNum =  0;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");

        try{
            while(date.compareTo(todate) <= 0){
                Date d =f.parse(date);
                new BaseBean().writeLog("==zj==(当前计算日期)" + d);
                int week = dateToWeek(d);
                new BaseBean().writeLog("==zj==(返回星期几)" + week);

                //如果为6或7则为周六日
                if ( week == 6 || week ==7 ){
                    weekNum++;
                    String dates = f.format(d);     //将日期转为String类型
                    String sql = "select  * from kq_HolidaySet where holidayDate = '" + dates + "' and groupid="+groupSetId;
                    new BaseBean().writeLog("==zj==(周六日计算sql)" + sql);
                    rs.executeQuery(sql);
                    if (rs.next()){
                        int changeType = rs.getInt("changeType");
                        new BaseBean().writeLog("==zj==(周日情况下是否是节假日)" + changeType);
                        if (changeType ==1 || changeType ==3){
                            weekNum--;
                        }
                        if (changeType == 2){
                            //如果周六日有调配工作日则不算休息日
                            weekNum --;
                        }
                    }

                }

                date = DateUtil.addDate(date,1);
            }


        }catch (Exception e){
            new BaseBean().writeLog("==zj==(计算周六日报错)" + e);
        }
        new BaseBean().writeLog("==z==(一共几个周六日)" + weekNum);
        return weekNum;
    }

    /**
     * 计算当前日期为周几
     * @param date
     * @return
     */
    public  int dateToWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        //因为数组下标从0开始，而返回的是数组的内容，是数组{1,2,3,4,5,6,7}中用1~7来表示，所以要减1
        int week = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (week == 0) {
            week = 7;
        }
        return week;

    }

}

