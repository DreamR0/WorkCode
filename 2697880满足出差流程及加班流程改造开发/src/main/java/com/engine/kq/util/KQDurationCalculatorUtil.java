package com.engine.kq.util;

import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.biz.chain.cominfo.ShiftInfoCominfoBean;
import com.engine.kq.biz.chain.duration.NonDayUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonHalfUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonHourUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonWholeUnitSplitChain;
import com.engine.kq.biz.chain.duration.NonWorkDurationChain;
import com.engine.kq.biz.chain.duration.WorkDayUnitSplitChain;
import com.engine.kq.biz.chain.duration.WorkDurationChain;
import com.engine.kq.biz.chain.duration.WorkHalfUnitSplitChain;
import com.engine.kq.biz.chain.duration.WorkHourUnitSplitChain;
import com.engine.kq.biz.chain.duration.WorkWholeUnitSplitChain;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import com.engine.kq.wfset.util.KQFlowUtil;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.report.schedulediff.HrmScheduleDiffUtil;

public class KQDurationCalculatorUtil extends BaseBean {
  private KQLog kqLog = new KQLog();

  private final String resourceid;
  private String fromDate;
  private String toDate;
  private String fromTime;
  private String toTime;
  private String newLeaveType;
  /**
   * 获取最小计算单位
   * 1-按天计算
   * 2-按半天计算
   * 3-按小时计算
   * 4-按整天计算
   */
  private String durationrule;
  /**
   * 1-按工作日计算计算时长
   * 2-按自然日计算计算时长
   */
  private String computingMode;

  /**
   * 加班类型
   */
  private String overtime_type;

  /**
   * 是否统计休息时长
   */
  //==zj
  private String isOpenRest;

  private DurationTypeEnum durationTypeEnum;

  //外部类的构造函数
  private KQDurationCalculatorUtil(DurationParamBuilder build){
    this.resourceid = build.resourceid;
    this.fromDate = build.fromDate;
    this.toDate = build.toDate;
    this.fromTime = build.fromTime;
    this.toTime = build.toTime;
    this.newLeaveType = build.newLeaveType;
    this.durationrule = build.durationrule;
    this.computingMode = build.computingMode;
    this.durationTypeEnum = build.durationTypeEnum;
    this.overtime_type = build.overtime_type;
    //==zj
    this.isOpenRest = build.isOpenRest;
  }

  /**
   * 根据人和指定的日期获取办公时段
   * @param resourceid
   * @param date
   * @param containYesterday
   * @return
   */
  public static ShiftInfoBean getWorkTime(String resourceid, String date,boolean containYesterday){
    User user = User.getUser(Util.getIntValue(resourceid), 0);
    if(user == null){
      return null;
    }
    return getWorkTime(user, date,containYesterday);
  }

  /**
   * 不记录日志的，流程的超时提醒日志太大
   * @param resourceid
   * @param date
   * @param containYesterday
   * @param isLog
   * @return
   */
  public static ShiftInfoBean getWorkTime(String resourceid, String date,boolean containYesterday,boolean isLog){
    User user = User.getUser(Util.getIntValue(resourceid), 0);
    if(user == null){
      return null;
    }
    return getWorkTime(user, date,containYesterday,isLog);
  }

  public static ShiftInfoCominfoBean getShiftInfoCominfoBean(String resourceid, String date){
    KQWorkTime kqWorkTime = new KQWorkTime();
    Map<String, Object> kqWorkTimeMap = new HashMap<>();
    ShiftInfoCominfoBean shiftInfoCominfoBean = kqWorkTime.getShiftInfoCominfoBean(resourceid, date);
    return shiftInfoCominfoBean;
  }

  /**
   * 直接根据user来获取
   * @param user
   * @param date
   * @param containYesterday
   * @param isLog
   * @return
   */
  public static ShiftInfoBean getWorkTime(User user, String date,boolean containYesterday,boolean isLog){
    KQWorkTime kqWorkTime = new KQWorkTime();
    Map<String, Object> kqWorkTimeMap = new HashMap<>();
    kqWorkTimeMap = kqWorkTime.getWorkDuration(""+user.getUID(), date,containYesterday,isLog);
    boolean isfree = "1".equalsIgnoreCase(Util.null2String(kqWorkTimeMap.get("isfree")));
    if(isfree){
      ShiftInfoBean shiftInfoBean = new ShiftInfoBean();
      shiftInfoBean.setIsfree(true);
      String signStart = Util.null2String(kqWorkTimeMap.get("signStart"));
      String workMins = Util.null2String(kqWorkTimeMap.get("workMins"));
      shiftInfoBean.setFreeSignStart(signStart);
      shiftInfoBean.setFreeWorkMins(workMins);
      shiftInfoBean.setSplitDate(date);
      if(signStart.length() > 0 && workMins.length() > 0){
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime signLocalTime = LocalTime.parse(signStart, dateTimeFormatter);
        shiftInfoBean.setFreeSignEnd(signLocalTime.plusMinutes(Util.getIntValue(workMins)).format(dateTimeFormatter));
        shiftInfoBean.setFreeSignMiddle(signLocalTime.plusMinutes(Util.getIntValue(workMins)/2).format(dateTimeFormatter));
      }

      return shiftInfoBean;
    }else{
      if(kqWorkTimeMap.get("shiftInfoBean") != null){
        ShiftInfoBean shiftInfoBean = (ShiftInfoBean)kqWorkTimeMap.get("shiftInfoBean");
        return shiftInfoBean;
      }else{
        return null;
      }
    }
  }

  /**
   * 直接根据user来获取
   * @param user
   * @param date
   * @param containYesterday
   * @return
   */
  public static ShiftInfoBean getWorkTime(User user, String date,boolean containYesterday){
    return getWorkTime(user, date, containYesterday, true);
  }

  public static Map<String, Object> getWorkButton(String resourceid, String date, boolean containYesterday){
    User user = User.getUser(Util.getIntValue(resourceid), 0);
    return getWorkButton(user,date,containYesterday);
  }

  public static Map<String, Object> getWorkButton(User user, String date, boolean containYesterday){
    KQWorkTime kqWorkTime = new KQWorkTime();
    Map<String, Object> kqWorkTimeMap = new HashMap<>();
    kqWorkTimeMap = kqWorkTime.getWorkButton(""+user.getUID(), date,containYesterday);

    return kqWorkTimeMap;
  }

  /**
   * 根据传入的用户和时段返回非工作时长
   * @return
   */
  public Map<String,Object> getNonWorkDuration(){

    Map<String,Object> durationMap = new HashMap<>();
    try{
      double D_Duration = 0.0;
      double Min_Duration = 0.0;
      //公众假日加班时长
      double D_Pub_Duration = 0.0;
      double D_Pub_Mins = 0.0;
      //工作日加班时长
      double D_Work_Duration = 0.0;
      double D_Work_Mins = 0.0;
      //休息日加班时长
      double D_Rest_Duration = 0.0;
      double D_Rest_Mins = 0.0;

      SplitBean splitBean = new SplitBean();
      splitBean.setFromDate(fromDate);
      splitBean.setFromTime(fromTime);
      splitBean.setToDate(toDate);
      splitBean.setToTime(toTime);
      splitBean.setResourceId(resourceid);
      splitBean.setFromdatedb(fromDate);
      splitBean.setTodatedb(toDate);
      splitBean.setFromtimedb(fromTime);
      splitBean.setTotimedb(toTime);
      splitBean.setDurationrule(durationrule);
      splitBean.setComputingMode(computingMode);
      splitBean.setDurationTypeEnum(DurationTypeEnum.OVERTIME);
      splitBean.setOvertime_type(overtime_type);
      //==zj
      splitBean.setIsOpenRest(isOpenRest);

      List<SplitBean> splitBeans = new ArrayList<>();

      NonWorkDurationChain hourUnitSplitChain = new NonHourUnitSplitChain(splitBeans);
      NonWorkDurationChain dayUnitSplitChain = new NonDayUnitSplitChain(splitBeans);
      NonWorkDurationChain halfUnitSplitChain = new NonHalfUnitSplitChain(splitBeans);
      NonWorkDurationChain wholeUnitSplitChain = new NonWholeUnitSplitChain(splitBeans);

      //设置执行链
      hourUnitSplitChain.setDurationChain(dayUnitSplitChain);
      dayUnitSplitChain.setDurationChain(halfUnitSplitChain);
      halfUnitSplitChain.setDurationChain(wholeUnitSplitChain);
      //把初始数据设置进去
      hourUnitSplitChain.handleDuration(splitBean);

      //每一天的流程时长都在这里了，搞吧
      for(SplitBean sb : splitBeans){
//   * 1-公众假日、2-工作日、3-休息日
        int changeType = sb.getChangeType();
        double durations = Util.getDoubleValue(sb.getDuration(), 0.0);
        double durationMins = sb.getD_Mins();
        if(1 == changeType){
          D_Pub_Duration += durations;
          D_Pub_Mins += durationMins;
        }
        if(2 == changeType){
          D_Work_Duration += durations;
          D_Work_Mins += durationMins;
        }
        if(3 == changeType){
          D_Rest_Duration += durations;
          D_Rest_Mins += durationMins;
        }
      }
      Min_Duration = D_Pub_Mins+D_Work_Mins+D_Rest_Mins;

      if("3".equalsIgnoreCase(durationrule) || "5".equalsIgnoreCase(durationrule) || "6".equalsIgnoreCase(durationrule)){
        double d_hour = Min_Duration/60.0;
        durationMap.put("duration", KQDurationCalculatorUtil.getDurationRound(""+d_hour));
      }else {
        double oneDayHour = KQFlowUtil.getOneDayHour(DurationTypeEnum.OVERTIME,"");
        double d_day = Min_Duration/(oneDayHour * 60);
        durationMap.put("duration", KQDurationCalculatorUtil.getDurationRound(""+d_day));
      }

      durationMap.put("min_duration", KQDurationCalculatorUtil.getDurationRound(""+Min_Duration));

    }catch (Exception e){
      e.printStackTrace();
    }
    return durationMap;
  }

  /**
   * 根据传入的用户和时段返回工作时长
   * @return
   */
  public Map<String,Object> getWorkDuration(){

    Map<String,Object> durationMap = new HashMap<>();
    try{
      if(!isValidate(fromDate,toDate,fromTime,toTime)){
        durationMap.put("duration", "0.0");
        return durationMap;
      }
      if(durationTypeEnum != DurationTypeEnum.COMMON_CAL){
        kqLog.info("getWorkDuration:"+durationTypeEnum.getDurationType()+":fromDate:"+fromDate+":toDate:"+toDate+":fromTime:"+fromTime+":toTime:"+toTime+":durationrule:"+durationrule+":computingMode:"+computingMode);
      }
      //如果是加班
      if(durationTypeEnum ==DurationTypeEnum.OVERTIME){
        return getNonWorkDuration();
      }
      //时长
      double D_Duration = 0.0;
      //分钟数
      double Min_Duration = 0.0;

      SplitBean splitBean = new SplitBean();
      splitBean.setFromDate(fromDate);
      splitBean.setFromTime(fromTime);
      splitBean.setToDate(toDate);
      splitBean.setToTime(toTime);
      splitBean.setResourceId(resourceid);
      splitBean.setFromdatedb(fromDate);
      splitBean.setTodatedb(toDate);
      splitBean.setFromtimedb(fromTime);
      splitBean.setTotimedb(toTime);
      splitBean.setDurationrule(durationrule);
      splitBean.setDurationTypeEnum(durationTypeEnum);
      splitBean.setComputingMode(computingMode);
      splitBean.setNewLeaveType(newLeaveType);
      if("2".equalsIgnoreCase(computingMode)){
        double oneDayHour = KQFlowUtil.getOneDayHour(durationTypeEnum,newLeaveType);
        splitBean.setOneDayHour(oneDayHour);
        if(durationTypeEnum == DurationTypeEnum.LEAVE){
          //只有自然日 请假才有这个排除节假日、休息日的功能
          splitBean.setFilterholidays(KQLeaveRulesBiz.getFilterHolidays(splitBean.getNewLeaveType()));
        }
      }
      if(durationTypeEnum ==DurationTypeEnum.LEAVE || durationTypeEnum ==DurationTypeEnum.LEAVEBACK){
        if(newLeaveType.length() > 0){
          KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
          String conversion = kqLeaveRulesComInfo.getConversion(newLeaveType);
          splitBean.setConversion(conversion);
        }
      }

      List<SplitBean> splitBeans = new ArrayList<>();

      WorkDurationChain hourUnitSplitChain = new WorkHourUnitSplitChain(splitBeans);
      WorkDurationChain dayUnitSplitChain = new WorkDayUnitSplitChain(splitBeans);
      WorkDurationChain halfUnitSplitChain = new WorkHalfUnitSplitChain(splitBeans);
      WorkDurationChain wholeUnitSplitChain = new WorkWholeUnitSplitChain(splitBeans);

      //设置执行链
      hourUnitSplitChain.setDurationChain(dayUnitSplitChain);
      dayUnitSplitChain.setDurationChain(halfUnitSplitChain);
      halfUnitSplitChain.setDurationChain(wholeUnitSplitChain);
      //把初始数据设置进去
      hourUnitSplitChain.handleDuration(splitBean);

      //每一天的流程时长都在这里了，搞吧
      for(SplitBean sb : splitBeans){
        double durations = Util.getDoubleValue(sb.getDuration(), 0.0);
        double min_durations = sb.getD_Mins();
        D_Duration += durations;
        Min_Duration += min_durations;
      }

      durationMap.put("duration", KQDurationCalculatorUtil.getDurationRound(""+D_Duration));
      durationMap.put("min_duration", KQDurationCalculatorUtil.getDurationRound(""+Min_Duration));

    }catch (Exception e){
      e.printStackTrace();
    }
    return durationMap;
  }

  /**
   * 校验是传入的参数数据是否正常
   * @return false 表示数据有误
   */
  private boolean isValidate(String fromDate,String toDate,String fromTime,String toTime) {

    if(fromDate.length() == 0 || toDate.length() == 0){
      return false;
    }
    if(fromTime.length() == 0 || toTime.length() == 0){
      return false;
    }

    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    String fromDateTime = fromDate+" "+fromTime+":00";
    String toDateTime = toDate+" "+toTime+":00";

    LocalDateTime localFromDateTime = LocalDateTime.parse(fromDateTime,fullFormatter);
    LocalDateTime localToDateTime = LocalDateTime.parse(toDateTime,fullFormatter);

    if(localFromDateTime.isAfter(localToDateTime) || localFromDateTime.isEqual(localToDateTime)){
      return false;
    }
    return true;
  }

  /**
   * 得到排除非工作时间的时长
   * @param fromDate
   * @param fromTime
   * @param toDate
   * @param toTime
   * @param resourceid
   * @return
   */
  public Map<String,Object> getTotalWorkingDurations(String fromDate,String fromTime,String toDate,String toTime,String resourceid){
    KQDurationCalculatorUtil kqDurationCalculatorUtil =new DurationParamBuilder(resourceid).
            fromDateParam(fromDate).toDateParam(toDate).fromTimeParam(fromTime).toTimeParam(toTime).durationRuleParam("1")
            .computingModeParam("1").durationTypeEnumParam(DurationTypeEnum.COMMON_CAL).build();

    Map<String,Object> durationMap = kqDurationCalculatorUtil.getWorkDuration();
    return durationMap;
  }

  /**
   * 得到排除非工作时间的天数
   * @param fromDate
   * @param fromTime
   * @param toDate
   * @param toTime
   * @param resourceid
   * @return
   */
  public String getTotalWorkingDays(String fromDate,String fromTime,String toDate,String toTime,String resourceid){
    Map<String,Object> durationMap = getTotalWorkingDurations(fromDate,fromTime,toDate,toTime,resourceid);

    String duration4day = Util.null2s(Util.null2String(durationMap.get("duration")),"0");
    return KQDurationCalculatorUtil.getDurationRound(duration4day);
  }

  /**
   * 得到排除非工作时间的小时
   * @param fromDate
   * @param fromTime
   * @param toDate
   * @param toTime
   * @param resourceid
   * @return
   */
  public String getTotalWorkingHours(String fromDate,String fromTime,String toDate,String toTime,String resourceid){
    Map<String,Object> durationMap = getTotalWorkingDurations(fromDate,fromTime,toDate,toTime,resourceid);
    String duration4min = Util.null2s(Util.null2String(durationMap.get("min_duration")),"0");
    double duration4hour = Util.getDoubleValue(duration4min)/60.0;

    return KQDurationCalculatorUtil.getDurationRound(duration4hour+"");
  }

  /**
   * 得到排除非工作时间的分钟
   * @param fromDate
   * @param fromTime
   * @param toDate
   * @param toTime
   * @param resourceid
   * @return
   */
  public String getTotalWorkingMins(String fromDate,String fromTime,String toDate,String toTime,String resourceid){
    Map<String,Object> durationMap = getTotalWorkingDurations(fromDate,fromTime,toDate,toTime,resourceid);
    String duration4min = Util.null2s(Util.null2String(durationMap.get("min_duration")),"0");

    return KQDurationCalculatorUtil.getDurationRound(duration4min+"");
  }

  /**
   * 得到非工作时间的天数
   * @param fromDate
   * @param fromTime
   * @param toDate
   * @param toTime
   * @param resourceid
   * @return
   */
  public String getTotalNonWorkingDays(String fromDate,String fromTime,String toDate,String toTime,String resourceid){
    KQDurationCalculatorUtil kqDurationCalculatorUtil =new DurationParamBuilder(resourceid).
            fromDateParam(fromDate).toDateParam(toDate).fromTimeParam(fromTime).toTimeParam(toTime).computingModeParam("1").
            durationRuleParam("1").durationTypeEnumParam(DurationTypeEnum.OVERTIME).build();
    Map<String,Object> durationMap = kqDurationCalculatorUtil.getNonWorkDuration();

    String duration = Util.null2String(durationMap.get("duration"));
    return KQDurationCalculatorUtil.getDurationRound(duration);
  }

  /**
   * 得到非工作时间的小时
   * @param fromDate
   * @param fromTime
   * @param toDate
   * @param toTime
   * @param resourceid
   * @return
   */
  public String getTotalNonWorkingHours(String fromDate,String fromTime,String toDate,String toTime,String resourceid){
    KQDurationCalculatorUtil kqDurationCalculatorUtil =new DurationParamBuilder(resourceid).
            fromDateParam(fromDate).toDateParam(toDate).fromTimeParam(fromTime).toTimeParam(toTime).computingModeParam("1").
            durationRuleParam("3").durationTypeEnumParam(DurationTypeEnum.OVERTIME).build();
    Map<String,Object> durationMap = kqDurationCalculatorUtil.getNonWorkDuration();

    String duration = Util.null2String(durationMap.get("duration"));
    return KQDurationCalculatorUtil.getDurationRound(duration);
  }

  /**
   * 考勤通用精度 2
   * @param duration
   * @return
   */
  public static String getDurationRound(String duration){
    if(HrmScheduleDiffUtil.isFromFlow()){
      return Util.round(duration,5) ;
    }
    return Util.round(duration, 2);
  }

  /**
   * 考勤流程中间表精度 5
   * @param duration
   * @return
   */
  public static String getDurationRound5(String duration){
    return Util.round(duration, 5);
  }

  /**
   * 针对可能存在的多种参数类型 创建参数静态内部类Builder
   */
  public static class DurationParamBuilder {

    //必选变量 人员看怎么都是需要的
    private final String resourceid;

    //可选变量
    private String fromDate = "";
    private String toDate = "";
    private String fromTime = "";
    private String toTime = "";
    /**
     * 请假用的请假类型
     */
    private String newLeaveType = "";
    /**
     * 单位
     * 1-按天出差
     * 2-按半天出差
     * 3-按小时出差
     * 4-按整天出差
     */
    private String durationrule = "";
    /**
     * 时长计算方式
     * 1-按照工作日计算请假时长
     * 2-按照自然日计算请假时长
     */
    private String computingMode = "";

    /**
     * 加班类型
     */
    private String overtime_type = "";

    /**
     * 是否统计休息时长
     */
    //==zj
    private String isOpenRest = "";

    /**
     * 哪种类型的时长计算，请假还是出差还是公出还是加班
     */
    private DurationTypeEnum durationTypeEnum;

    public DurationParamBuilder(String resourceid) {
      this.resourceid = resourceid;
      //初始化的时候需要把其他参数先清空下
      this.fromDate = "";
      this.toDate = "";
      this.fromTime = "";
      this.toTime = "";
      this.newLeaveType = "";
      this.durationrule = "";
      this.computingMode = "";
      this.overtime_type = "";
      this.isOpenRest = "";
    }

    //成员方法返回其自身，所以可以链式调用
    public DurationParamBuilder fromDateParam(final String fromDate) {
      this.fromDate = fromDate;
      return this;
    }

    public DurationParamBuilder toDateParam(final String toDate) {
      this.toDate = toDate;
      return this;
    }

    public DurationParamBuilder fromTimeParam(final String fromTime) {
      this.fromTime = fromTime;
      return this;
    }

    public DurationParamBuilder toTimeParam(final String toTime) {
      this.toTime = toTime;
      return this;
    }

    public DurationParamBuilder newLeaveTypeParam(final String newLeaveType) {
      this.newLeaveType = newLeaveType;
      return this;
    }

    public DurationParamBuilder durationRuleParam(final String durationrule) {
      this.durationrule = durationrule;
      return this;
    }
    public DurationParamBuilder computingModeParam(final String computingMode) {
      this.computingMode = computingMode;
      return this;
    }
    public DurationParamBuilder overtime_typeParam(final String overtime_type) {
      this.overtime_type = overtime_type;
      return this;
    }
    //==zj
    public DurationParamBuilder isOpenRest_typeParam(final String isOpenRest) {
      this.isOpenRest = isOpenRest;
      return this;
    }

    public DurationParamBuilder durationTypeEnumParam(final DurationTypeEnum durationTypeEnum) {
      this.durationTypeEnum = durationTypeEnum;
      return this;
    }

    //Builder的build方法，返回外部类的实例
    public KQDurationCalculatorUtil build() {
      return new KQDurationCalculatorUtil(this);
    }
  }

}
