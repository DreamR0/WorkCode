package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2563653.util.CheckUtil;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.KQOvertimeRulesDetailEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;
import weaver.wechat.util.DateUtil;

import java.math.BigDecimal;
import java.util.*;

import static com.engine.kq.cmd.attendanceEvent.GetOverTimeWorkDurationCmd.threadLocal;

/**
 * 加班规则的相关接口
 */
public class KQOvertimeRulesBiz {
  //qc2563653 保存用户id
  private static String userId  ;

  private static KQLog logger = new KQLog();//用于记录日志信息

  private static KQOvertimeRulesDetailEntity kqOvertimeRulesDetail = new KQOvertimeRulesDetailEntity();//加班规则实体类

  /**
   * 根据指定人员ID以及指定日期判断这一天是工作日还是休息日还是节假日
   *
   * @param resourceId 指定人员ID
   *                   根据指定人员ID获取考勤组ID，根据考勤组得到对应日期是工作日还是休息日
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日(这里综合考虑考勤组和节假日设置，节假日设置的优先级最高)
   * @return 1-节假日、2-工作日、3-休息日、-1-数据异常，无效数据
   */
  public static int getChangeType(String resourceId, String date) {
    int changeType = -1;

    /*获取考勤组的ID，因为考勤组有有效期，所以需要传入日期*/
    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    String groupId = kqGroupMemberComInfo.getKQGroupId(resourceId, date);

    /*该人员不存在于任意一个考勤组中，请为其设置考勤组*/
    if(groupId.equals("")){
      logger.writeLog("该人员不存在于任意一个考勤组中，请为其设置考勤组。resourceId=" + resourceId + ",date=" + date);
    }

    changeType = KQHolidaySetBiz.getChangeType(groupId, date);
    if (changeType != 1 && changeType != 2 && changeType != 3) {
      KQWorkTime kqWorkTime = new KQWorkTime();
      changeType = kqWorkTime.isWorkDay(resourceId, date) ? 2 : 3;
    }
    return changeType;
  }

  /**
   * 判断是否允许加班
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return 0-不允许加班、1-允许加班
   */
  public static int getOvertimeEnable(String resourceId, String date) {
    int overtimeEnable = 0;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    overtimeEnable = kqOvertimeRulesDetail.getOvertimeEnable();
    return overtimeEnable;
  }

  /**
   * 获取加班方式计算
   * 1-需审批，以审批单为准
   * 2-需审批，以打卡为准，但是不能超过审批时长
   * 3-无需审批，根据打卡时间计算加班时长
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return -1：接口异常，返回的无效数据。
   */
  public static int getComputingMode(String resourceId, String date) {
    int computingMode = -1;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    computingMode = kqOvertimeRulesDetail.getComputingMode();
    return computingMode;
  }

  /**
   * 获取加班起算时间
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return -1：接口异常，返回的无效数据。
   */
  public static int getStartTime(String resourceId, String date) {
    int startTime = -1;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    startTime = kqOvertimeRulesDetail.getStartTime();
    return startTime;
  }

  /**
   * 获取最小加班时长
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return -1：接口异常，返回的无效数据。
   */
  public static int getMinimumLen(String resourceId, String date) {
    int minimumUnit = -1;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    minimumUnit = kqOvertimeRulesDetail.getMinimumLen();
    return minimumUnit;
  }

  /**
   * 是否允许加班补偿
   * 0-不允许加班转调休、1-允许加班转调休
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return
   */
  public static int getPaidLeaveEnable(String resourceId, String date) {
    int paidLeaveEnable = 0;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    paidLeaveEnable = kqOvertimeRulesDetail.getPaidLeaveEnable();
    return paidLeaveEnable;
  }

  /**
   * 获取加班时长转调休时长比例中的分母
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return -1：接口异常，返回的无效数据。
   */
  public static double getLenOfOvertime(String resourceId, String date) {
    double lenOfOvertime = -1;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    lenOfOvertime = kqOvertimeRulesDetail.getLenOfOvertime();
    BigDecimal bigDecimal = new BigDecimal(lenOfOvertime);
    lenOfOvertime = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    return lenOfOvertime;
  }

  /**
   * 获取加班时长转调休时长比例中的分子
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return -1：接口异常，返回的无效数据。
   */
  public static double getLenOfLeave(String resourceId, String date) {
    double lenOfLeave = -1;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    lenOfLeave = kqOvertimeRulesDetail.getLenOfLeave();
    BigDecimal bigDecimal = new BigDecimal(lenOfLeave);
    lenOfLeave = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    return lenOfLeave;
  }

  /**
   * 判断是否需要排除休息时间
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return 0-不需要排除休息时间、1-需要排除休息时间
   */
  public static int getHasRestTime(String resourceId, String date) {
    int hasRestTime = 0;
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    hasRestTime = kqOvertimeRulesDetail.getHasRestTime();
    return hasRestTime;
  }

  /**
   * 获取休息时段
   * startType：0-开始时间为本日、1-开始时间为次日
   * startTime：开始时间
   * endType：0-结束时间为本日、1-结束时间为次日
   * endTime：结束时间
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param date       指定日期
   *                   根据日期判断是工作日还是节假日还是休息日
   * @return
   */
  public static List<String[]> getRestTimeList(String resourceId, String date) {
    List<String[]> restTimeList = new ArrayList<String[]>();

    /**
     * 因为休息时段可以设置次日的数据，所以获取某一天的休息时段的时候，需要先考虑上一个日期所设置的次日的休息时段
     */
    String lastDay = DateUtil.addDay(date, -1, "yyyy-MM-dd");
    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, lastDay);
    int overtimeEnable = kqOvertimeRulesDetail.getOvertimeEnable();
    int hasRestTime = kqOvertimeRulesDetail.getHasRestTime();
    if (overtimeEnable == 1 && hasRestTime == 1) {
      int dayType = kqOvertimeRulesDetail.getDayType();
      int ruleId = kqOvertimeRulesDetail.getRuleId();

      String sql = "select * from kq_OvertimeRestTime where ruleId=" + ruleId + " and dayType=" + dayType;
      RecordSet recordSet = new RecordSet();
      recordSet.executeQuery(sql);
      while (recordSet.next()) {
        String startType = recordSet.getString("startType");
        String startTime = recordSet.getString("startTime");
        String endType = recordSet.getString("endType");
        String endTime = recordSet.getString("endTime");

        if (startType.equals("0") && endType.equals("1")) {
          String[] str = new String[]{"00:00", endTime};
          restTimeList.add(str);
        } else if (startType.equals("1") && endType.equals("1")) {
          String[] str = new String[]{startTime, endTime};
          restTimeList.add(str);
        }
      }
    }

    kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, date);
    overtimeEnable = kqOvertimeRulesDetail.getOvertimeEnable();
    hasRestTime = kqOvertimeRulesDetail.getHasRestTime();
    if (overtimeEnable == 1 && hasRestTime == 1) {
      int dayType = kqOvertimeRulesDetail.getDayType();
      int ruleId = kqOvertimeRulesDetail.getRuleId();

      String sql = "select * from kq_OvertimeRestTime where ruleId=" + ruleId + " and dayType=" + dayType;
      RecordSet recordSet = new RecordSet();
      recordSet.executeQuery(sql);
      while (recordSet.next()) {
        String startType = recordSet.getString("startType");
        String startTime = recordSet.getString("startTime");
        String endType = recordSet.getString("endType");
        String endTime = recordSet.getString("endTime");

        if (startType.equals("0") && endType.equals("0")) {
          String[] str = new String[]{startTime, endTime};
          restTimeList.add(str);
        } else if (startType.equals("0") && endType.equals("1")) {
          String[] str = new String[]{startTime, "24:00"};
          restTimeList.add(str);
        }
      }
    }
    return restTimeList;
  }

  /**
   * 获取加班规则的明细
   *
   * @param resourceId 指定人员
   *                   根据人员ID获取人员所在的考勤组ID，找出对应的加班规则ID
   * @param changeType 日期类型：1-节假日、2-工作日、3-休息日
   * @return
   */
  private static KQOvertimeRulesDetailEntity getOvertimeRulesDetail(String resourceId, String date) {
    KQOvertimeRulesDetailEntity kqOvertimeRulesDetail = new KQOvertimeRulesDetailEntity();
    try {
      /*获取考勤组的ID，因为考勤组有有效期，所以需要传入日期*/
      KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
      String groupIds = kqGroupMemberComInfo.getKQGroupId(resourceId, date);
      if (groupIds.equals("")) {
        /*该人员不存在于任意一个考勤组中，请为其设置考勤组*/
        logger.writeLog("该人员不存在于任意一个考勤组中，请为其设置考勤组。resourceId=" + resourceId + ",date=" + date);
      }

      int changeType = getChangeType(resourceId, date);
      /*获取当前日期的日期类型错误*/
      if (changeType != 1 && changeType != 2 && changeType != 3) {
        logger.writeLog("获取当前日期的日期类型错误。resourceId=" + resourceId + ",date=" + date + ",changeType=" + changeType);
      }

      int overtimeRuleId = 0;//加班规则的ID
      RecordSet recordSet = new RecordSet();
      String sql = "select id from kq_OvertimeRules where (isDelete is null or isDelete !=1) ";
      if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
        sql += " and ','+groupIds+',' like '%," + groupIds + ",%'";
      } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
        sql += " and concat(',',groupIds,',') like '%," + groupIds + ",%'";
      } else {
        sql += " and ','||groupIds||',' like '%," + groupIds + ",%'";
      }
      recordSet.executeQuery(sql);
      if (recordSet.next()) {
        overtimeRuleId = recordSet.getInt("id");

        sql = "select * from kq_OvertimeRulesDetail where ruleId=" + overtimeRuleId + " and dayType=" + changeType;
        recordSet.executeQuery(sql);
        if (recordSet.next()) {
          int ruleId = recordSet.getInt("ruleId");
          int dayType = recordSet.getInt("dayType");
          int overtimeEnable = recordSet.getInt("overtimeEnable");
          int computingMode = recordSet.getInt("computingMode");
          int startTime = recordSet.getInt("startTime");
          int minimumLen = recordSet.getInt("minimumLen");
          int paidLeaveEnable = recordSet.getInt("paidLeaveEnable");
          double lenOfOvertime = Util.getDoubleValue(recordSet.getString("lenOfOvertime"), 1.00);
          double lenOfLeave = Util.getDoubleValue(recordSet.getString("lenOfLeave"), 1.00);
          int hasRestTime = Util.getIntValue(recordSet.getString("hasRestTime"));

          kqOvertimeRulesDetail.setRuleId(ruleId);
          kqOvertimeRulesDetail.setDayType(dayType);
          kqOvertimeRulesDetail.setOvertimeEnable(overtimeEnable);
          kqOvertimeRulesDetail.setComputingMode(computingMode);
          kqOvertimeRulesDetail.setStartTime(startTime);
          kqOvertimeRulesDetail.setMinimumLen(minimumLen);
          kqOvertimeRulesDetail.setPaidLeaveEnable(paidLeaveEnable);
          kqOvertimeRulesDetail.setLenOfOvertime(lenOfOvertime);
          kqOvertimeRulesDetail.setLenOfLeave(lenOfLeave);
          kqOvertimeRulesDetail.setHasRestTime(hasRestTime);
        }
      } else {
        logger.writeLog("该人员所属的考勤组没有设置过任何加班规则，请为其设置加班规则。resourceId=" + resourceId + ",date="+date+",changeType=" + changeType);
      }
    } catch (Exception e) {
      logger.writeLog("根据人员ID获取加班规则的规则内容出错。resourceId=" + resourceId + ",date=" + date);
      e.printStackTrace();
    }
    return  kqOvertimeRulesDetail;
  }

  /**
   * 新建加班规则
   *
   * @param params
   * @return
   */
  public static int addOvertimeRules(Map<String, Object> params, User user) {
    /*是否保存成功*/
    boolean flag = false;

    /*新插入的加班规则的ID*/
    int ruleId = -1;
    try {
      boolean canAdd = HrmUserVarify.checkUserRight("KQOvertimeRulesAdd:Add", user);//是否具有新建权限
      if (!canAdd) {
        logger.info(user.getLastname() + "暂时没有权限!");
        return -1;
      }

      /*加班规则名称*/
      String name = Util.null2String(params.get("name"));
      /*考勤组的ID*/
      String groupIds = Util.null2String(params.get("groupIds"));

      /*工作日是否允许加班：0-不允许、1-允许*/
      int overtimeEnable2 = Util.getIntValue((String) params.get("overtimeEnable2"), 1);
      /*工作日的加班方式：1-需审批，以审批单为准、2-需审批，以打卡为准，但是不能超过审批时长、3-无需审批，根据打卡时间计算加班时长*/
      int computingMode2 = Util.getIntValue((String) params.get("computingMode2"), 1);
      /*工作日下班多少分钟后开始计算加班*/
      int startTime2 = Util.getIntValue((String) params.get("startTime2"), 30);
      /*工作日最小加班时长*/
      int minimumLen2 = Util.getIntValue((String) params.get("minimumLen2"), 30);
      /*工作日是否允许加班转调休*/
      int paidLeaveEnable2 = Util.getIntValue((String) params.get("paidLeaveEnable2"), 0);
      /*工作日加班转调休比例中的加班时长*/
      int lenOfOvertime2 = Util.getIntValue((String) params.get("lenOfOvertime2"), 1);
      /*工作日加班转调休比例中的调休时长*/
      int lenOfLeave2 = Util.getIntValue((String) params.get("lenOfLeave2"), 1);

      /*休息日是否允许加班：0-不允许、1-允许*/
      int overtimeEnable3 = Util.getIntValue((String) params.get("overtimeEnable3"), 1);
      /*休息日的加班方式：1-需审批，以审批单为准、2-需审批，以打卡为准，但是不能超过审批时长、3-无需审批，根据打卡时间计算加班时长*/
      int computingMode3 = Util.getIntValue((String) params.get("computingMode3"), 1);
      /*休息日的最小加班时长*/
      int minimumLen3 = Util.getIntValue((String) params.get("minimumLen3"), 30);
      /*休息日是否允许加班转调休*/
      int paidLeaveEnable3 = Util.getIntValue((String) params.get("paidLeaveEnable3"), 0);
      /*休息日加班转调休比例中的加班时长*/
      int lenOfOvertime3 = Util.getIntValue((String) params.get("lenOfOvertime3"), 1);
      /*休息日加班转调休比例中的调休时长*/
      int lenOfLeave3 = Util.getIntValue((String) params.get("lenOfLeave3"), 1);
      /*休息日是否有休息时间*/
      int hasRestTime3 = Util.getIntValue((String) params.get("hasRestTime3"), 0);

      /*节假日是否允许加班：0-不允许、1-允许*/
      int overtimeEnable1 = Util.getIntValue((String) params.get("overtimeEnable1"), 1);
      /*节假日的加班方式：1-需审批，以审批单为准、2-需审批，以打卡为准，但是不能超过审批时长、3-无需审批，根据打卡时间计算加班时长*/
      int computingMode1 = Util.getIntValue((String) params.get("computingMode1"), 1);
      /*节假日的最小加班时长*/
      int minimumLen1 = Util.getIntValue((String) params.get("minimumLen1"), 30);
      /*节假日是否允许加班转调休*/
      int paidLeaveEnable1 = Util.getIntValue((String) params.get("paidLeaveEnable1"), 0);
      /*节假日加班转调休比例中的加班时长*/
      int lenOfOvertime1 = Util.getIntValue((String) params.get("lenOfOvertime1"), 1);
      /*节假日加班转调休比例中的调休时长*/
      int lenOfLeave1 = Util.getIntValue((String) params.get("lenOfLeave1"), 1);
      /*节假日是否有休息时间*/
      int hasRestTime1 = Util.getIntValue((String) params.get("hasRestTime1"), 0);

      int paidLeaveEnableType2 = Util.getIntValue((String) params.get("paidLeaveEnableType2"), 2);
      int paidLeaveEnableType3 = Util.getIntValue((String) params.get("paidLeaveEnableType3"), 2);
      int paidLeaveEnableType1 = Util.getIntValue((String) params.get("paidLeaveEnableType1"), 2);
      int paidLeaveEnableFlowType2 = Util.getIntValue((String) params.get("paidLeaveEnableFlowType2"), 1);
      int paidLeaveEnableFlowType3 = Util.getIntValue((String) params.get("paidLeaveEnableFlowType3"), 1);
      int paidLeaveEnableFlowType1 = Util.getIntValue((String) params.get("paidLeaveEnableFlowType1"), 1);

      /*获取今天的日期*/
      Calendar today = Calendar.getInstance();
      String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
          + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
          + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
      String tomorrowDate = weaver.common.DateUtil.addDate(currentDate, 1);

      /*校验休息日排除休息时间的设置是否正确*/
      if (hasRestTime3 == 1) {
        int restTimeLen3 = Util.getIntValue((String) params.get("restTimeLen3"), 0);

        List<String> fromList = new ArrayList<String>();
        List<String> toList = new ArrayList<String>();
        for (int i = 0; i < restTimeLen3; i++) {
          String startType = "0";
          String startTime = Util.getIntValues((String) params.get("startTime3_" + i));
          String endType = "0";
          String endTime = Util.getIntValues((String) params.get("endTime3_" + i));

          String fromDateTime = (startType.equals("0") ? currentDate : tomorrowDate) + " " + startTime + ":00";
          String toDateTime = (endType.equals("0") ? currentDate : tomorrowDate) + " " + endTime + ":00";
          if (fromDateTime.compareTo(toDateTime) > 0) {
            logger.info("结束时间必须大于开始时间，fromDateTime=" + fromDateTime + "，toDateTime=" + toDateTime);
            return -1;
          }
          fromList.add(fromDateTime);
          toList.add(toDateTime);
        }
        /*利用冒泡法对日期时间进行排序*/
        for (int i = 0; i < fromList.size(); i++) {
          for (int j = 0; j < fromList.size() - i - 1; j++) {
            if (fromList.get(j).compareTo(fromList.get(j + 1)) > 0) {
              String str = fromList.get(j);
              fromList.set(j, fromList.get(j + 1));
              fromList.set(j + 1, str);

              str = toList.get(j);
              toList.set(j, toList.get(j + 1));
              toList.set(j + 1, str);
            }
          }
        }
        /*根据排序后的数据判断是否时间段有重叠*/
        for (int i = 0; i < fromList.size() - 1; i++) {
          if (fromList.get(i + 1).compareTo(toList.get(i)) < 0) {
            logger.info("休息时段不能交叉");
            return -1;
          }
        }
      }

      /*校验休息日排除休息时间的设置是否正确*/
      if (hasRestTime1 == 1) {
        int restTimeLen1 = Util.getIntValue((String) params.get("restTimeLen1"), 0);

        List<String> fromList = new ArrayList<String>();
        List<String> toList = new ArrayList<String>();
        for (int i = 0; i < restTimeLen1; i++) {
          String startType = "0";
          String startTime = Util.getIntValues((String) params.get("startTime1_" + i));
          String endType = "0";
          String endTime = Util.getIntValues((String) params.get("endTime1_" + i));

          String fromDateTime = (startType.equals("0") ? currentDate : tomorrowDate) + " " + startTime + ":00";
          String toDateTime = (endType.equals("0") ? currentDate : tomorrowDate) + " " + endTime + ":00";
          if (fromDateTime.compareTo(toDateTime) > 0) {
            logger.info("结束时间必须大于开始时间：fromDateTime=" + fromDateTime + "，toDateTime=" + toDateTime);
            return -1;
          }
          fromList.add(fromDateTime);
          toList.add(toDateTime);
        }
        /*利用冒泡法对日期时间进行排序*/
        for (int i = 0; i < fromList.size(); i++) {
          for (int j = 0; j < fromList.size() - i - 1; j++) {
            if (fromList.get(j).compareTo(fromList.get(j + 1)) > 0) {
              String str = fromList.get(j);
              fromList.set(j, fromList.get(j + 1));
              fromList.set(j + 1, str);

              str = toList.get(j);
              toList.set(j, toList.get(j + 1));
              toList.set(j + 1, str);
            }
          }
        }
        /*根据排序后的数据判断是否时间段有重叠*/
        for (int i = 0; i < fromList.size() - 1; i++) {
          if (fromList.get(i + 1).compareTo(toList.get(i)) < 0) {
            logger.info("休息时段不能交叉");
            return -1;
          }
        }
      }

      String sql = "insert into kq_OvertimeRules(name,groupIds,isDelete) values(?,?,0)";
      RecordSet recordSet = new RecordSet();
      flag = recordSet.executeUpdate(sql, name, groupIds);
      if (!flag) {
        logger.info("加班规则保存失败");//保存失败
        return -1;
      }

      sql = "select max(id) maxId from kq_OvertimeRules";
      recordSet.executeQuery(sql);
      if (recordSet.next()) {
        ruleId = recordSet.getInt("maxId");
      }

      if (ruleId != 0) {
        sql = "insert into kq_OvertimeRulesDetail(ruleId,dayType,overtimeEnable,computingMode,startTime,minimumLen,paidLeaveEnable,lenOfOvertime,lenOfLeave,hasRestTime,paidleaveenabletype,paidleaveenableflowtype) " +
            "values(?,?,?,?,?,?,?,?,?,?,?,?)";
        /**
         * 保存工作日加班规则
         */
        flag = recordSet.executeUpdate(sql, ruleId, 2, overtimeEnable2, computingMode2, startTime2, minimumLen2, paidLeaveEnable2, lenOfOvertime2, lenOfLeave2, 0,paidLeaveEnableType2,paidLeaveEnableFlowType2);
        if (!flag) {
          logger.info("工作日加班规则保存失败");//保存失败
          return -1;
        }
        /**
         * 保存休息日加班规则
         */
        flag = recordSet.executeUpdate(sql, ruleId, 3, overtimeEnable3, computingMode3, 0, minimumLen3, paidLeaveEnable3, lenOfOvertime3, lenOfLeave3, hasRestTime3,paidLeaveEnableType3,paidLeaveEnableFlowType3);
        if (!flag) {
          logger.info("休息日加班规则保存失败");//保存失败
          return -1;
        }

        if (hasRestTime3 == 1) {
          int restTimeLen3 = Util.getIntValue((String) params.get("restTimeLen3"), 0);

          for (int i = 0; i < restTimeLen3; i++) {
            String startType = "0";
            String startTime = Util.getIntValues((String) params.get("startTime3_" + i));
            String endType = "0";
            String endTime = Util.getIntValues((String) params.get("endTime3_" + i));

            String restTimeSql = "insert into kq_OvertimeRestTime(ruleId,dayType,startType,startTime,endType,endTime) values(?,?,?,?,?,?)";
            flag = recordSet.executeUpdate(restTimeSql, ruleId, 3, startType, startTime, endType, endTime);
            if (!flag) {
              logger.info("休息日加班规则休息时段保存失败");//保存失败
              return -1;
            }
          }
        }
        /**
         * 保存节假日加班规则
         */
        flag = recordSet.executeUpdate(sql, ruleId, 1, overtimeEnable1, computingMode1, 0, minimumLen1, paidLeaveEnable1, lenOfOvertime1, lenOfLeave1, hasRestTime1,paidLeaveEnableType1,paidLeaveEnableFlowType1);
        if (!flag) {
          logger.info("节假日加班规则保存失败");
          return -1;
        }

        if (hasRestTime1 == 1) {
          int restTimeLen1 = Util.getIntValue((String) params.get("restTimeLen1"), 0);

          for (int i = 0; i < restTimeLen1; i++) {
            String startType = "0";
            String startTime = Util.getIntValues((String) params.get("startTime1_" + i));
            String endType = "0";
            String endTime = Util.getIntValues((String) params.get("endTime1_" + i));

            String restTimeSql = "insert into kq_OvertimeRestTime(ruleId,dayType,startType,startTime,endType,endTime) values(?,?,?,?,?,?)";
            flag = recordSet.executeUpdate(restTimeSql, ruleId, 1, startType, startTime, endType, endTime);
            if (!flag) {
              logger.info("节假日加班规则休息时段保存失败");
              return -1;
            }
          }
        }
      }
    } catch (Exception e) {
      ruleId = -1;
    }
    return ruleId;
  }

  /************************************************加班单位的相关接口************************************************/
  /**
   * 获取最小加班单位
   * 1-按天加班
   * 2-按半天加班
   * 3-按小时加班
   * 4-按整天加班
   * 5-按半小时加班
   * 6-按整小时加班
   * @return -1：接口异常，返回的无效数据。
   */
  public static int getMinimumUnit() {
    userId  = threadLocal.get();

    Map<String, Object> reusltMap = new HashMap<>();
    new BaseBean().writeLog("==zj==(userid-thread)" + JSON.toJSONString(userId));
    int minimumUnit = -1;
    //qc2563653 如果有维护建模表根据建模表设置
    if (!"1".equals(userId) && userId != null/*&& false*/){
      CheckUtil checkUtil = new CheckUtil();
       reusltMap = checkUtil.getOverSet(userId);
      new BaseBean().writeLog("==zj==(reusltMap1)" + JSON.toJSONString(reusltMap));
    }


    if (reusltMap.size() > 0){
      minimumUnit = (int) reusltMap.get("minimumUnit");
    }else {
      String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
      RecordSet recordSet = new RecordSet();
      recordSet.executeQuery(sql);
      if (recordSet.next()) {
        minimumUnit = recordSet.getInt("minimumUnit");
      }
    }

    return minimumUnit;
  }

  /**
   * 获取最小加班单位
   * 1-按天加班
   * 2-按半天加班
   * 3-按小时加班
   * 4-按整天加班
   * 5-按半小时加班
   * 6-按整小时加班
   * @return -1：接口异常，返回的无效数据。
   */
  public static Map<String,String> getMinimumUnitAndConversion() {
    userId  = threadLocal.get();

    Map<String, Object> reusltMap = new HashMap<>();
    Map<String,String> map = Maps.newHashMap();
    int minimumUnit = -1;
    int overtimeConversion = -1;
    //qc2563653 加班单位设置走建模表
    if (!"1".equals(userId) && userId != null/*&& false*/) {
      CheckUtil checkUtil = new CheckUtil();
       reusltMap = checkUtil.getOverSet(userId);
      new BaseBean().writeLog("==zj==(reusltMap2)" + JSON.toJSONString(reusltMap));
    }

    //qc2563653 如果有建模配置走配置，没有走标准
    if (reusltMap.size() > 0) {
      minimumUnit =(int) reusltMap.get("minimumUnit");
      overtimeConversion = (int) reusltMap.get("overtimeConversion");
      map.put("minimumUnit", ""+minimumUnit);
      map.put("overtimeConversion", ""+overtimeConversion);
    } else {
      String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
      RecordSet recordSet = new RecordSet();
      recordSet.executeQuery(sql);
      if (recordSet.next()) {
        minimumUnit = recordSet.getInt("minimumUnit");
        overtimeConversion = recordSet.getInt("overtimeConversion");
        map.put("minimumUnit", ""+minimumUnit);
        map.put("overtimeConversion", ""+overtimeConversion);
      }
    }
    return map;
  }


  /**
   * 获取加班单位的日折算时长
   *
   * @return -1：接口异常，返回的无效数据。
   */
  public static double getHoursToDay() {
    userId  = threadLocal.get();
new BaseBean().writeLog("==zj==(getHoursToDay)" + JSON.toJSONString(userId));

    Map<String, Object> reusltMap = new HashMap<>();
    double hoursToDay = -1.00;

    //qc2563653 如果有维护建模表根据建模表设置
    if (!"1".equals(userId) && userId != null /*&& false*/){
      CheckUtil checkUtil = new CheckUtil();
       reusltMap = checkUtil.getOverSet(userId);
      new BaseBean().writeLog("==zj==(reusltMap3)" + JSON.toJSONString(reusltMap));
    }

    if (reusltMap.size() > 0){
      hoursToDay =(double) reusltMap.get("hoursToDay");
    } else {
      String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
      RecordSet recordSet = new RecordSet();
      recordSet.executeQuery(sql);
      if (recordSet.next()) {
        hoursToDay = recordSet.getDouble("hoursToDay");
      }
    }

    BigDecimal bigDecimal = new BigDecimal(hoursToDay);
    return bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
  }

  /**
   * 获取半天显示类型，是时间还是下拉框
   * 半天单位 时间选择方式：1-下拉框选择 、2-具体时间
   * @return
   */
  public static String getTimeselection() {
    int timeselection = 1;
    String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
    RecordSet recordSet = new RecordSet();
    recordSet.executeQuery(sql);
    if (recordSet.next()) {
      timeselection = recordSet.getInt("timeselection");
    }
    if(timeselection == 1 || timeselection == 2){
      return ""+timeselection;
    }else{
      return "1";
    }
  }

  /**
   * 获取加班所有需要的数据
   * @param resourceId
   * @param date
   * @param changeTypeMap
   * @return
   */
  public static void getOverTimeData(String resourceId, String date,
      Map<String,Integer> changeTypeMap,Map<String,KQOvertimeRulesDetailEntity> overRulesDetailMap,Map<String,List<String[]>> restTimeMap,Map<String,Integer> computingModeMap) {
    int changeType = -1;

    /*获取考勤组的ID，因为考勤组有有效期，所以需要传入日期*/
    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    String groupId = Util.null2s(kqGroupMemberComInfo.getKQGroupId(resourceId, date),"");

    /*该人员不存在于任意一个考勤组中，请为其设置考勤组*/
    if(groupId.length() == 0){
      logger.writeLog("该人员不存在于任意一个考勤组中，请为其设置考勤组。resourceId=" + resourceId + ",date=" + date);
      return ;
    }

    changeType = KQHolidaySetBiz.getChangeType(groupId, date);
    if (changeType != 1 && changeType != 2 && changeType != 3) {
      KQWorkTime kqWorkTime = new KQWorkTime();
      changeType = kqWorkTime.isWorkDay(resourceId, date) ? 2 : 3;
    }
    String change_key = date+"_"+resourceId;
    changeTypeMap.put(change_key, changeType);

    List<String[]> restTimeList = new ArrayList<String[]>();
    int overtimeRuleId = 0;//加班规则的ID
    RecordSet recordSet = new RecordSet();
    String sql = "select id from kq_OvertimeRules where (isDelete is null or isDelete !=1) ";
    if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
      sql += " and ','+groupIds+',' like '%," + groupId + ",%'";
    } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
      sql += " and concat(',',groupIds,',') like '%," + groupId + ",%'";
    } else {
      sql += " and ','||groupIds||',' like '%," + groupId + ",%'";
    }
    recordSet.executeQuery(sql);
    if (recordSet.next()) {
      overtimeRuleId = recordSet.getInt("id");

      sql = "select * from kq_OvertimeRulesDetail where ruleId=" + overtimeRuleId + " and dayType=" + changeType;
      recordSet.executeQuery(sql);
      if (recordSet.next()) {
        int id = recordSet.getInt("id");
        int ruleId = recordSet.getInt("ruleId");
        int dayType = recordSet.getInt("dayType");
        int overtimeEnable = recordSet.getInt("overtimeEnable");
        int computingMode = recordSet.getInt("computingMode");
        int startTime = recordSet.getInt("startTime");
        int minimumLen = recordSet.getInt("minimumLen");
        int paidLeaveEnable = recordSet.getInt("paidLeaveEnable");
        double lenOfOvertime = Util.getDoubleValue(recordSet.getString("lenOfOvertime"), 1.00);
        double lenOfLeave = Util.getDoubleValue(recordSet.getString("lenOfLeave"), 1.00);
        int hasRestTime = Util.getIntValue(recordSet.getString("hasRestTime"));
        int before_startTime = recordSet.getInt("before_startTime");

        int paidLeaveEnableType = recordSet.getInt("paidLeaveEnableType");
        int paidLeaveEnableDefaultType = recordSet.getInt("paidLeaveEnableDefaultType");
        int paidLeaveEnableFlowType = recordSet.getInt("paidLeaveEnableFlowType");
        int restTimeType = recordSet.getInt("restTimeType");
        int has_cut_point = Util.getIntValue(Util.null2s(recordSet.getString("has_cut_point"),"0"),0);
        if(paidLeaveEnableType <= 0){
          paidLeaveEnableType = 1;
        }
        if(paidLeaveEnableDefaultType <= 0){
          paidLeaveEnableDefaultType = 1;
        }
        if(paidLeaveEnableFlowType <= 0){
          paidLeaveEnableFlowType = 1;
        }
        if(paidLeaveEnableType == 1 && paidLeaveEnable == 0){
          //如果就没有开启关联调休，那么默认的加班转调休方式就是第一种
          paidLeaveEnableDefaultType = 1;
        }
        if(restTimeType <= 0){
          restTimeType = 1;
        }
        String cut_point = Util.null2s(recordSet.getString("cut_point"),"");
        String key = date+"_"+changeType;

        KQOvertimeRulesDetailEntity kqOvertimeRulesDetail = new KQOvertimeRulesDetailEntity();
        kqOvertimeRulesDetail.setRuleId(ruleId);
        kqOvertimeRulesDetail.setDayType(dayType);
        kqOvertimeRulesDetail.setOvertimeEnable(overtimeEnable);
        kqOvertimeRulesDetail.setComputingMode(computingMode);
        kqOvertimeRulesDetail.setStartTime(startTime);
        kqOvertimeRulesDetail.setMinimumLen(minimumLen);
        kqOvertimeRulesDetail.setPaidLeaveEnable(paidLeaveEnable);
        kqOvertimeRulesDetail.setLenOfOvertime(lenOfOvertime);
        kqOvertimeRulesDetail.setLenOfLeave(lenOfLeave);
        kqOvertimeRulesDetail.setHasRestTime(hasRestTime);
        kqOvertimeRulesDetail.setBefore_startTime(before_startTime);
        kqOvertimeRulesDetail.setCut_point(cut_point);
        kqOvertimeRulesDetail.setHas_cut_point(has_cut_point);
        kqOvertimeRulesDetail.setId(id);
        kqOvertimeRulesDetail.setPaidLeaveEnableType(paidLeaveEnableType);
        kqOvertimeRulesDetail.setPaidLeaveEnableDefaultType(paidLeaveEnableDefaultType);
        kqOvertimeRulesDetail.setPaidLeaveEnableFlowType(paidLeaveEnableFlowType);
        kqOvertimeRulesDetail.setRestTimeType(restTimeType);
        overRulesDetailMap.put(key, kqOvertimeRulesDetail);
        computingModeMap.put(key, computingMode);

        /**
         * 因为休息时段可以设置次日的数据，所以获取某一天的休息时段的时候，需要先考虑上一个日期所设置的次日的休息时段
         */
        String lastDay = DateUtil.addDay(date, -1, "yyyy-MM-dd");
        KQOvertimeRulesDetailEntity pre_kqOvertimeRulesDetail = getOvertimeRulesDetail(resourceId, lastDay);
        int pre_overtimeEnable = pre_kqOvertimeRulesDetail.getOvertimeEnable();
        int pre_hasRestTime = pre_kqOvertimeRulesDetail.getHasRestTime();
        if (pre_overtimeEnable == 1 && pre_hasRestTime == 1) {
          int pre_dayType = pre_kqOvertimeRulesDetail.getDayType();
          int pre_ruleId = pre_kqOvertimeRulesDetail.getRuleId();

          sql = "select * from kq_OvertimeRestTime where ruleId=" + pre_ruleId + " and dayType=" + pre_dayType;
          RecordSet recordSet1 = new RecordSet();
          recordSet1.executeQuery(sql);
          while (recordSet1.next()) {
            String startType = recordSet1.getString("startType");
            String startTime1 = recordSet1.getString("startTime");
            String endType = recordSet1.getString("endType");
            String endTime = recordSet1.getString("endTime");

            if (startType.equals("0") && endType.equals("1")) {
              String[] str = new String[]{"00:00", endTime};
              restTimeList.add(str);
            } else if (startType.equals("1") && endType.equals("1")) {
              String[] str = new String[]{startTime1, endTime};
              restTimeList.add(str);
            }
          }
        }

        overtimeEnable = kqOvertimeRulesDetail.getOvertimeEnable();
        hasRestTime = kqOvertimeRulesDetail.getHasRestTime();
        restTimeType = kqOvertimeRulesDetail.getRestTimeType();
        if (overtimeEnable == 1 && hasRestTime == 1) {
          if(restTimeType == 1){
            sql = "select * from kq_OvertimeRestTime where ruleId=" + ruleId + " and dayType=" + dayType;
            RecordSet recordSet1 = new RecordSet();
            recordSet1.executeQuery(sql);
            while (recordSet1.next()) {
              String startType = recordSet1.getString("startType");
              String startTime1 = recordSet1.getString("startTime");
              String endType = recordSet1.getString("endType");
              String endTime = recordSet1.getString("endTime");

              if (startType.equals("0") && endType.equals("0")) {
                String[] str = new String[]{startTime1, endTime};
                restTimeList.add(str);
              } else if (startType.equals("0") && endType.equals("1")) {
                String[] str = new String[]{startTime1, "24:00"};
                restTimeList.add(str);
              }
            }
          }else{
            sql = "select * from kq_OvertimeRestlength where ruleId=" + ruleId + " and dayType=" + dayType+" order by dsporder ";
            RecordSet recordSet1 = new RecordSet();
            recordSet1.executeQuery(sql);
            while (recordSet1.next()) {
              String overlength = Util.null2String(recordSet1.getString("overlength"));
              String cutlength = Util.null2String(recordSet1.getString("cutlength"));
              if(overlength.length() > 0 && cutlength.length() > 0){
                String[] str = new String[]{overlength, cutlength};
                restTimeList.add(str);
              }
            }
          }
        }
        restTimeMap.put(key, restTimeList);
      }


    } else {
      logger.writeLog("该人员所属的考勤组没有设置过任何加班规则，请为其设置加班规则。resourceId=" + resourceId + ",date="+date+",changeType=" + changeType);
    }

    return ;
  }

  /**
   * 按加班时长范围设置转调休时长
   * 按照加班时长从大到小排序 取到最大的满足了，那么调休就直接按照这个来转调休了
   */
  public Map<String,List<String>> getBalanceLengthDetailMap(int ruleDetailid){
    Map<String,List<String>> balanceLengthDetailMap = Maps.newHashMap();
    RecordSet recordSet = new RecordSet();
    List<String> overtimelengthList = Lists.newArrayList();
    List<String> balancelengthList = Lists.newArrayList();
    String sql = "select * from kq_OvertimeBalanceLengthDetail where rulesdetailid= ? order by  overtimelength desc ";
    recordSet.executeQuery(sql, ruleDetailid);
    while (recordSet.next()){
      String overtimelength = recordSet.getString("overtimelength");
      String balancelength = recordSet.getString("balancelength");
      overtimelengthList.add(overtimelength);
      balancelengthList.add(balancelength);
    }
    if(!overtimelengthList.isEmpty() && !balancelengthList.isEmpty()){
      balanceLengthDetailMap.put("overtimelengthList", overtimelengthList);
      balanceLengthDetailMap.put("balancelengthList", balancelengthList);
      return balanceLengthDetailMap;
    }
    return balanceLengthDetailMap;
  }

  /**
   * 按加班的时间段设置转调休时长
   */
  public Map<String,List<String>> getBalanceTimeDetailMap(int ruleDetailid){
    Map<String,List<String>> balanceTimethDetailMap = Maps.newHashMap();
    RecordSet recordSet = new RecordSet();
    List<String> idList = Lists.newArrayList();
    List<String> timepointList = Lists.newArrayList();
    List<String> lenOfOvertimeList = Lists.newArrayList();
    List<String> lenOfLeaveList = Lists.newArrayList();
    String sql = "select * from kq_OvertimeBalanceTimeDetail where rulesdetailid= ? order by dsporder ";
    recordSet.executeQuery(sql, ruleDetailid);
    while (recordSet.next()){
      String id = recordSet.getString("id");
      String timepoint = recordSet.getString("timepoint");
      String lenOfOvertime = recordSet.getString("lenOfOvertime");
      String lenOfLeave = recordSet.getString("lenOfLeave");
      idList.add(id);
      timepointList.add(timepoint);
      lenOfOvertimeList.add(lenOfOvertime);
      lenOfLeaveList.add(lenOfLeave);
    }
    if(!timepointList.isEmpty() && !lenOfOvertimeList.isEmpty() && !lenOfLeaveList.isEmpty()){
      balanceTimethDetailMap.put("idList", idList);
      balanceTimethDetailMap.put("timepointList", timepointList);
      balanceTimethDetailMap.put("lenOfOvertimeList", lenOfOvertimeList);
      balanceTimethDetailMap.put("lenOfLeaveList", lenOfLeaveList);
      return balanceTimethDetailMap;
    }
    return balanceTimethDetailMap;
  }
}
