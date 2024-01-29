package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.api.cusomization.qc2721227.util.KqCustomUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.bean.KQHrmScheduleSign;
import com.engine.kq.biz.*;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.google.common.collect.Maps;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.dateformat.DateTransformer;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取签到签退按钮
 * 以及考勤流程数据
 * 以及上次签到时间
 */
public class GetButtonsCmd extends AbstractCommonCommand<Map<String, Object>> {

  private KQLog kqLog = new KQLog();
  private String curDate = DateUtil.getCurrentDate();
  private LocalDateTime now = LocalDateTime.now();
  private DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
  private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
  private Map<String,Object> logMap = Maps.newHashMap();
  private Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();

  public GetButtonsCmd(Map<String, Object> params, User user) {
    this.user = user;
    this.params = params;
    String now_date = now.format(dateFormatter);
    String now_time = now.format(fullTimeFormatter);
    LocalDateTime now_zone = getZoneOfClientDateTime(now_date,now_time, logMap);
    if(now_zone != null){
      setNow(now_zone);
      setCurDate(now_zone.format(dateFormatter));
    }
  }

  /**
   * 一个完整的timelines里map的样子
   * {
   * across: "0" 是否是次日
   * active: "1" 是否是允许签到签退的范围
   * belongdate: "2018-12-27" 根据班次获取的所属日期
   * canSignTime: "2018-12-27 00:00:00" 允许签到签退的时间范围
   * date: "2018-12-27" 签到签退的日期
   * datetime: "2018-12-27 08:30:00" 签到签退日期+上下班时间
   * isYellow: "1" 当前时间是否已经是迟到或者早退
   * isacross: "0" 当前时间对应的班次是否是跨天班次
   * islastsign: "0" 是否是最后一次签退
   * min: "0" 允许打卡时段控制
   * needSign: "1" 是否可以签到签退，在active的基础上增加了hrmschedulesign签到签退数据的判断
   * pre: "0" 是否是昨日
   * serialid: "521" 班次id
   * signsection: "2018-12-27 00:00:00#2018-12-27 14:00:00" 允许签到签退的范围
   * time: "08:30" 上班时间/下班时间
   * type: "on" 对应的是签到还是签退的状态
   * workdatesection: "2018-12-27 08:30:00#2018-12-27 11:30:00" 上下班日期时段
   * worksection: "08:30-11:30" 上下班时间
   * }
   * @param commandContext
   * @return
   */
  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    logMap.put("lastname", user.getLastname());
    logMap.put("params", params);
    kqLog.info(user.getLastname()+":GetButtonsCmd:params:"+params);
    Map<String, Object> retmap = new HashMap<String, Object>();
    RecordSet rs = new RecordSet();
    String sql = "";
    try{
      curDate = getCurDate();
      //真正的考勤时间线
      List<Object> timelineList = new ArrayList<>();
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

      KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
      KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      KQReportBiz kqReportBiz = new KQReportBiz();

      String lastname = user.getLastname();
      String messagerurl = resourceComInfo.getMessagerUrls(""+user.getUID());
      String shortname = "";
      //自由切换班次
      String serialid = Util.null2String(params.get("serialid"));
      String ismobile = Util.null2String(params.get("ismobile"));

      boolean USERICONLASTNAME = Util.null2String(new BaseBean().getPropValue("Others" , "USERICONLASTNAME")).equals("1");
      if(USERICONLASTNAME&&(messagerurl.indexOf("icon_w_wev8.jpg")>-1||messagerurl.indexOf("icon_m_wev8.jpg")>-1||messagerurl.indexOf("dummyContact.png")>-1)){
        shortname = User.getLastname(Util.null2String(Util.formatMultiLang(lastname, ""+user.getLanguage())));
      }

      String groupname = "";
      KQWorkTime kqWorkTime = new KQWorkTime();
      WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(user.getUID()+"", curDate);
      String userinfo = "#userid#"+user.getUID()+"#getUserSubCompany1#"+user.getUserSubCompany1()+"#getUserSubCompany1#"+user.getUserDepartment()
              +"#getJobtitle#"+user.getJobtitle();
      workTimeEntityLogMap.put("resourceid", userinfo);
      workTimeEntityLogMap.put("splitDate", curDate);
      workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);
      String groupid = workTimeEntity.getGroupId();
      logMap.put("groupid", groupid);
      String outsidesign = "";
      KQGroupEntity kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
      String kqGroupEntityInfo = kqGroupEntity != null ? JSON.toJSONString(kqGroupEntity): "";
      logMap.put("kqGroupEntityInfo", kqGroupEntityInfo);
      if (kqGroupEntity != null) {
        outsidesign = kqGroupEntity.getOutsidesign();
      }else{
        groupname = SystemEnv.getHtmlLabelName(10000799, Util.getIntValue(user.getLanguage()));
        retmap.put("showbutton", "0");
        retmap.put("status", "1");
        retmap.put("groupname", groupname);
        KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
        return retmap;
      }

      boolean isAdmin = user.isAdmin();
      if(isAdmin){
        retmap.put("showbutton", "0");
        retmap.put("status", "1");
        KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
        return retmap;
      }

      //==z 如果是周末，节假日无需显示打卡按钮
      KqCustomUtil kqCustomUtil = new KqCustomUtil();
      if (kqCustomUtil.isOpen()){
        int changeType = KQOvertimeRulesBiz.getChangeType(user.getUID()+"", curDate);//获取当天类型
        if (changeType != 2 ){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          retmap.put("message",  "当前为周末或者没有班次，不需要打卡");
          new BaseBean().writeLog("==zj==(retmap)" + JSON.toJSONString(retmap));
          KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
          return retmap;
        }
      }


//   * 1、PC和移动端均可打卡
//          * 2、仅PC可打卡
//          * 3、仅移动端可打卡
//          * 4、无需打卡
      String groupSignType = getSignType();
      //无需打卡
      if("4".equalsIgnoreCase(groupSignType)){
        retmap.put("showbutton", "0");
        retmap.put("status", "1");
        KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
        return retmap;
      }
      if("2".equalsIgnoreCase(groupSignType)){
        if("1".equalsIgnoreCase(ismobile)){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
          return retmap;
        }
      }
      if("3".equalsIgnoreCase(groupSignType)){
        if(!"1".equalsIgnoreCase(ismobile)){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
          return retmap;
        }
      }

      String yesterday = LocalDate.parse(curDate).minusDays(1).format(dateFormatter);
      String nextday = LocalDate.parse(curDate).plusDays(1).format(dateFormatter);
      List<Object> yesterdayLineButton = new ArrayList<>();
      List<Object> todayLineButton = new ArrayList<>();
      List<Object> restLineButton = new ArrayList<>();

      //弹性工作制
      List<Object> freeLineButton = new ArrayList<>();

      //先获取考勤工作时段的按钮 上下班时间
      Map<String, Object> todayLineMap = KQDurationCalculatorUtil.getWorkButton(user, curDate,true);
      //明天的班次可能会存在上班开始时间在前一天的情况，这个也需要考虑
      Map<String, Object> nextLineMap = KQDurationCalculatorUtil.getWorkButton(user, nextday,true);
      boolean isfree = "1".equalsIgnoreCase(Util.null2String(todayLineMap.get("isfree")));
      logMap.put("todayLineMap", todayLineMap);
      logMap.put("nextLineMap", nextLineMap);

      if(isfree){
        do4FreeLine(user,freeLineButton,todayLineMap);
        if(freeLineButton != null && !freeLineButton.isEmpty()){
          timelineList.addAll(freeLineButton);
          retmap.put("lastname", lastname);
          retmap.put("shortname", shortname);
          retmap.put("messagerurl", messagerurl);
          retmap.put("groupname", groupname+SystemEnv.getHtmlLabelName(10000800, Util.getIntValue(user.getLanguage())));
          retmap.put("outsidesign", outsidesign);
          retmap.put("date", curDate);
          retmap.put("timeline", timelineList);
          retmap.put("status", "1");
          KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
          return retmap;
        }
      }
      //每次打开考勤按钮的时候，都只能有一个活动按钮
      List<String> activeList = new ArrayList<>();

      if(!todayLineMap.isEmpty()){
        if(todayLineMap.get("pre_timelineList") != null){
          do4YesterdayLine(todayLineMap,yesterdayLineButton,kqReportBiz,timelineList,yesterday,activeList);
        }
        if(todayLineMap.get("timelineList") != null){
          do4TodayLine(todayLineMap,todayLineButton,kqReportBiz,timelineList,activeList);
        }else{
          //节假日或者休息日需要默认给客户拼接一个timelineList
          retmap.put("isrest", "1");
          //kqLog.info("yesterdayLineButton:"+yesterdayLineButton);
          if(yesterdayLineButton.isEmpty()){
            List<Object> yesterdayLine = (List<Object>)todayLineMap.get("pre_timelineList");
            //kqLog.info("yesterdayLine:"+yesterdayLine);
            if(yesterdayLine != null){
              Map<String,Object> offTimelineMap = (Map<String,Object>)yesterdayLine.get(yesterdayLine.size()-1);
              //kqLog.info("offTimelineMap:"+offTimelineMap);
              //kqLog.info("signAcross:"+Util.null2String(offTimelineMap.get("signAcross")).equalsIgnoreCase("1"));
              if(offTimelineMap.containsKey("signAcross") && Util.null2String(offTimelineMap.get("signAcross")).equalsIgnoreCase("1")){
                Map<String,Object> offTimelineMapTmp = reBuildYesTimeMap(offTimelineMap,yesterday,"off");
                //kqLog.info("offTimelineMapTmp:"+offTimelineMapTmp);
                yesterdayLineButton.add(offTimelineMapTmp);
              }
            }
          }
          do4RestLine(""+user.getUID(),restLineButton,yesterdayLineButton,nextLineMap);
          timelineList.addAll(restLineButton);
        }
      }else{
        //节假日或者休息日需要默认给客户拼接一个timelineList
        retmap.put("isrest", "1");
        do4RestLine(""+user.getUID(),restLineButton, null,nextLineMap);
        timelineList.addAll(restLineButton);
      }
      if(!nextLineMap.isEmpty()){
        List<Object> nextdayLineButton = new ArrayList<>();
        do4NextdayLine(nextLineMap,nextdayLineButton,kqReportBiz,timelineList,nextday,activeList);
      }

      if(groupid.length() > 0){
        groupname = SystemEnv.getHtmlLabelName(10000801, Util.getIntValue(user.getLanguage()))+kqGroupComInfo.getGroupname(groupid);
      }else{
        groupname = SystemEnv.getHtmlLabelName(10000799, Util.getIntValue(user.getLanguage()));
      }

      retmap.put("lastname", lastname);
      retmap.put("shortname", shortname);
      retmap.put("messagerurl", messagerurl);
      retmap.put("groupname", groupname);
      retmap.put("outsidesign", outsidesign);
      retmap.put("date", curDate);
      retmap.put("timeline", timelineList);
      retmap.put("status", "1");
      retmap.put("now", now.format(fullFormatter));

      kqLog.info(user.getLastname()+":GetButtonsCmd:timelineList:"+timelineList);
    }catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      writeLog(e);
      kqLog.info("考勤按钮报错",e);
    }
    logMap.put("retmap", retmap);
    KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttons");
    return retmap;
  }

  /**
   * 当日的考勤按钮处理
   * @param todayLineMap
   * @param todayLineButton
   * @param kqReportBiz
   * @param timelineList
   * @param activeList
   */
  private void do4TodayLine(Map<String, Object> todayLineMap, List<Object> todayLineButton,
                            KQReportBiz kqReportBiz, List<Object> timelineList,
                            List<String> activeList) {

    List<Object> todayLine = (List<Object>)todayLineMap.get("timelineList");
    if(todayLine != null && !todayLine.isEmpty()){
      getWorkTimeButton(false, todayLine,todayLineButton, false);
      //获取当天的考勤数据
//      List<Object> reports = kqReportBiz.getKqDateInfo(""+user.getUID(),curDate,curDate,true);
      List<Object> reports = new ArrayList<>();
      getSignButton(user.getUID()+"",todayLineButton,reports,curDate);
      timelineList.addAll(todayLineButton);
    }
  }

  /**
   * 明日的跨天到今天的考勤按钮处理
   * 只有一种情况，那就是明日的班次设置的开始打卡时间在今天
   * 所以只有上班时间有这个情况要处理
   * @param nextLineMap
   * @param nextdayLineButton
   * @param kqReportBiz
   * @param timelineList
   * @param nextday
   * @param activeList
   */
  private void do4NextdayLine(Map<String, Object> nextLineMap,
                              List<Object> nextdayLineButton, KQReportBiz kqReportBiz,
                              List<Object> timelineList, String nextday, List<String> activeList) {

    List<Object> nextdayLine = (List<Object>)nextLineMap.get("timelineList");
    if(nextdayLine != null && !nextdayLine.isEmpty()){
      Map<String,Object> onTimelineMap = (Map<String, Object>) nextdayLine.get(0);
      if(onTimelineMap.containsKey("sign_preAcross")){
        boolean is_sign_preAcross = "1".equalsIgnoreCase(Util.null2String(onTimelineMap.get("sign_preAcross")));
        if(!is_sign_preAcross){
          return ;
        }
      }
      getWorkTimeButton(false, nextdayLine,nextdayLineButton,true);
      List<Object> reports = new ArrayList<>();
      getSignButton(user.getUID()+"",nextdayLineButton,reports,nextday);
      timelineList.addAll(nextdayLineButton);
    }
  }

  /**
   * 昨日跨天的考勤按钮处理
   * @param yesterdayLineMap
   * @param yesterdayLineButton
   * @param kqReportBiz
   * @param timelineList
   * @param yesterday
   * @param activeList
   */
  private void do4YesterdayLine(Map<String, Object> yesterdayLineMap,
                                List<Object> yesterdayLineButton, KQReportBiz kqReportBiz,
                                List<Object> timelineList, String yesterday, List<String> activeList) {

    List<Object> yesterdayLine = (List<Object>)yesterdayLineMap.get("pre_timelineList");
    if(yesterdayLine != null && !yesterdayLine.isEmpty()){
      getWorkTimeButton(true, yesterdayLine,yesterdayLineButton, false);
      //获取前一天的考勤数据
//          List<Object> reports = kqReportBiz.getKqDateInfo(""+user.getUID(),yesterday,yesterday,true);
      List<Object> reports = new ArrayList<>();
      getSignButton(user.getUID()+"",yesterdayLineButton,reports,yesterday);
      timelineList.addAll(yesterdayLineButton);
    }
  }

  /**
   * 针对自由工作制的人员单独处理
   * @param user
   * @param freeLineButton
   * @param todayLineMap
   */
  private void do4FreeLine(User user, List<Object> freeLineButton,
                           Map<String, Object> todayLineMap) {
    String resourceId = ""+user.getUID();
    if(todayLineMap != null && !todayLineMap.isEmpty()){
      LocalTime curTime = now.toLocalTime();

      String signStart = Util.null2String(todayLineMap.get("signStart"));//签到开始时间
      String workMins = Util.null2String(todayLineMap.get("workMins"));//工作时长
      LocalTime signStartLocal = LocalTime.parse(signStart+":00", fullTimeFormatter);
      boolean canSign = false;
      if(curTime.isAfter(signStartLocal)){
        canSign = true;
      }

      String signDateTimeSql = " signtime >= '"+signStart+":00"+"'";

      KQScheduleSignBiz kqScheduleSignBiz = new KQScheduleSignBiz.KQScheduleSignParamBuilder().resourceidParam(resourceId)
              .userTypeParam(user.getLogintype()).signDateParam(curDate).signDateTimeSqlParam(signDateTimeSql).build();
      List<KQHrmScheduleSign> kqHrmScheduleSigns = kqScheduleSignBiz.getFreeScheduleSignInfo();
      if(kqHrmScheduleSigns != null && !kqHrmScheduleSigns.isEmpty()) {
        int i = 0 ;
        for(KQHrmScheduleSign sign : kqHrmScheduleSigns){
          String signtype = sign.getSigntype();
          String signtime = sign.getSigntime();
          String signdate = sign.getSigndate();
          String signStatus = sign.getSignstatus();
          String addr = sign.getAddr();

          if(i%2 == 0){
            //签到
            freeLineButton.add(reBuildFreeButton("on", curDate, "0", "0",signtime,addr));
          }else{
//            签退
            freeLineButton.add(reBuildFreeButton("off", curDate, "0", "0",signtime,addr));
          }

          i++;
        }
        //i是加1之后的结果
        if(i%2 == 0){
          //如果是成对存在的，那么需要再多出来俩，支持多次签到签退
          freeLineButton.add(reBuildFreeButton("on", curDate, "1", canSign?"1":"0","",""));
          freeLineButton.add(reBuildFreeButton("off", curDate, "0", "0","",""));
        }else{
          //如果不是成对存在，一定是签到多，签退少，当前处于需要签退的状态
          freeLineButton.add(reBuildFreeButton("off", curDate, "1", "1","",""));
        }

      }else{
        //如果没有考勤数据，那么就是先签到再签退
        freeLineButton.add(reBuildFreeButton("on", curDate, "1", canSign?"1":"0","",""));
        freeLineButton.add(reBuildFreeButton("off", curDate, "0", "0","",""));
      }
    }

  }

  private Map<String,Object> reBuildFreeButton(String type,String date,String active,String needSign,String signTime,String position){
    Map<String,Object> freetimelineMap = new HashMap<>();
    freetimelineMap.put("type", type);
    freetimelineMap.put("belongdate", date);
    freetimelineMap.put("date", date);
    freetimelineMap.put("active", active);
    freetimelineMap.put("needSign", needSign);
    freetimelineMap.put("signTime", signTime);
    freetimelineMap.put("position", position);
    freetimelineMap.put("time", "");
    freetimelineMap.put("isfree", "1");

    return freetimelineMap;
  }

  /**
   * 真对非工作时间的签到签退
   * @param resourceId
   * @param restLineButton
   * @param yesterdayLineButton
   * @param nextLineMap
   */
  private void do4RestLine(String resourceId, List<Object> restLineButton,
                           List<Object> yesterdayLineButton,
                           Map<String, Object> nextLineMap) {
    boolean hasSignIn = false;
    boolean hasSignOut = false;
    boolean isActive = true;
    Map<String,Object> timelineOnMap = new HashMap<>();
    timelineOnMap.put("type", "on");
    timelineOnMap.put("time", "");
    timelineOnMap.put("isfirstsign", "1");
    timelineOnMap.put("belongdate", curDate);
    timelineOnMap.put("date", curDate);
    Map<String,Object> timelineOffMap = new HashMap<>();
    timelineOffMap.put("type", "off");
    timelineOffMap.put("time", "");
    timelineOffMap.put("islastsign", "1");
    timelineOffMap.put("belongdate", curDate);
    timelineOffMap.put("date", curDate);
    String yesterday = LocalDate.parse(curDate).minusDays(1).format(dateFormatter);
    String canSignTime = "";
    String next_canSignTime = "";
    String signDateTimeSql = "";
    //kqLog.info("do4RestLine::yesterdayLineButton"+yesterdayLineButton);
    if(yesterdayLineButton != null && !yesterdayLineButton.isEmpty()){
      Map<String,Object> yesterdayMap = (Map<String,Object>)yesterdayLineButton.get(yesterdayLineButton.size()-1);
      //kqLog.info("do4RestLine::yesterdayMap"+yesterdayMap);
      if(yesterdayMap != null){
        canSignTime = Util.null2String(yesterdayMap.get("canSignTime"));
        kqLog.info("do4RestLine::canSignTime"+canSignTime);
        if(canSignTime != null && canSignTime.length() > 0){
          LocalTime localTime = LocalTime.parse(canSignTime, timeFormatter);
          localTime = localTime.plusMinutes(1);
          canSignTime = localTime.format(timeFormatter);
        }
      }
    }
    if(!nextLineMap.isEmpty()){
      List<Object> nextdayLine = (List<Object>)nextLineMap.get("timelineList");
      if(nextdayLine != null && !nextdayLine.isEmpty()){
        Map<String,Object> onTimelineMap = (Map<String, Object>) nextdayLine.get(0);
        if(onTimelineMap.containsKey("sign_preAcross")){
          boolean is_sign_preAcross = "1".equalsIgnoreCase(Util.null2String(onTimelineMap.get("sign_preAcross")));
          if(is_sign_preAcross){
            int tMin = Util.getIntValue(Util.null2String(onTimelineMap.get("min")));
            String time = Util.null2String(onTimelineMap.get("time"));
            String nextday = LocalDate.parse(curDate).plusDays(1).format(dateFormatter);
            String next_date_canSignTime = nextday + " "+ time + ":00";
            if(tMin > 0) {
              LocalDateTime tmpLocalDateTime = LocalDateTime.parse(next_date_canSignTime, fullFormatter).minusMinutes(tMin);
              next_canSignTime = tmpLocalDateTime.format(timeFormatter);
            }
          }
        }
      }

    }
    if(canSignTime.length() > 0){
      signDateTimeSql = " signtime >= '"+canSignTime+":00"+"'";
      timelineOnMap.put("signSectionTime",curDate+" "+canSignTime+":00");
      timelineOnMap.put("signSection",curDate+" "+canSignTime+":00"+"#"+curDate+" "+"23:59:59");
      timelineOnMap.put("canSignTime","00:00:00");
      timelineOffMap.put("signSectionTime",curDate+" "+"23:59:59");
      timelineOffMap.put("canSignTime","23:59:59");
      timelineOffMap.put("signSection",curDate+" "+canSignTime+":00"+"#"+curDate+" "+"23:59:59");
      if(next_canSignTime.length() > 0){
        signDateTimeSql += " and signtime <= '"+next_canSignTime+":59"+"'";
        String offSignSectionTime = curDate+" "+next_canSignTime+":59";
        LocalDateTime offLocalDateTime = LocalDateTime.parse(offSignSectionTime,fullFormatter);
        if(now.isAfter(offLocalDateTime)){
          isActive = false;
        }
        timelineOffMap.put("signSectionTime",curDate+" "+next_canSignTime+":59");
        timelineOffMap.put("canSignTime",next_canSignTime+":59");
        timelineOffMap.put("signSection",curDate+" "+canSignTime+":00"+"#"+curDate+" "+next_canSignTime+":59");
      }
      String onSignSectionTime = curDate+" "+canSignTime+":00";
      LocalDateTime onLocalDateTime = LocalDateTime.parse(onSignSectionTime,fullFormatter);
      if(now.isBefore(onLocalDateTime)){
        isActive = false;
      }
    }else{
      timelineOnMap.put("signSectionTime",curDate+" "+"00:00:00");
      timelineOnMap.put("canSignTime","00:00:00");
      timelineOnMap.put("signSection",curDate+" "+"00:00:00"+":00"+"#"+curDate+" "+"23:59:59");
      timelineOffMap.put("signSectionTime",curDate+" "+"23:59:59");
      timelineOffMap.put("canSignTime","23:59:59");
      timelineOffMap.put("signSection",curDate+" "+"00:00:00"+":00"+"#"+curDate+" "+"23:59:59");
      if(next_canSignTime.length() > 0){
        String offSignSectionTime = curDate+" "+next_canSignTime+":59";
        LocalDateTime offLocalDateTime = LocalDateTime.parse(offSignSectionTime,fullFormatter);
        if(now.isAfter(offLocalDateTime)){
          isActive = false;
        }
        signDateTimeSql += " signtime <= '"+next_canSignTime+":59"+"'";
        timelineOffMap.put("signSectionTime",curDate+" "+next_canSignTime+":59");
        timelineOffMap.put("canSignTime",next_canSignTime+":59");
        timelineOnMap.put("signSection",curDate+" "+"00:00:00"+":00"+"#"+curDate+" "+next_canSignTime+":59");
        timelineOffMap.put("signSection",curDate+" "+"00:00:00"+":00"+"#"+curDate+" "+next_canSignTime+":59");
      }
    }

    KQScheduleSignBiz kqScheduleSignBiz = new KQScheduleSignBiz.KQScheduleSignParamBuilder().resourceidParam(resourceId)
            .userTypeParam(user.getLogintype()).signDateParam(curDate).signDateTimeSqlParam(signDateTimeSql).build();
    Map<String,KQHrmScheduleSign> signMap = kqScheduleSignBiz.getScheduleSignInfo();
    if(signMap != null && !signMap.isEmpty()) {
      KQHrmScheduleSign signInTimeBean = signMap.get("signin");
      KQHrmScheduleSign signOutTimeBean =  signMap.get("signout");
      if(signInTimeBean != null){
        String signtimeTmp = Util.null2String(signInTimeBean.getSigntime());
        timelineOnMap.put("signTime", signtimeTmp);
        timelineOnMap.put("position", Util.null2String(signInTimeBean.getAddr()));
        hasSignIn = true;
      }
      if(signOutTimeBean != null){
        String signtimeTmp = Util.null2String(signOutTimeBean.getSigntime());
        timelineOffMap.put("signTime", signtimeTmp);
        timelineOffMap.put("position", Util.null2String(signOutTimeBean.getAddr()));
        hasSignOut = true;
      }
      if(hasSignIn){
        //有签到 就记录下
        timelineOffMap.put("signInTime4Out", timelineOnMap.get("signTime"));
      }
    }
    getActiveRestSign(isActive, hasSignIn, hasSignOut, timelineOnMap, timelineOffMap);
    restLineButton.add(timelineOnMap);
    restLineButton.add(timelineOffMap);
  }

  /**
   * 获取考勤组的考勤方式
   * 1、PC和移动端均可打卡
   * 2、仅PC可打卡
   * 3、仅移动端可打卡
   */
  private String getSignType() {
    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    KQGroupEntity kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
    if(kqGroupEntity == null){
      return "";
    }
    return kqGroupEntity != null ? kqGroupEntity.getSignintype(): "";
  }

  /**
   * 根据工作时段来获取对应工作时段下的签到签退数据
   * @param resourceId
   * @param timelineList
   * @param reports
   */
  private void getSignButton(String resourceId,List<Object> timelineList,List<Object> reports,String kqDate) {
    if(!timelineList.isEmpty()){
      //获取当前时间
      LocalDateTime now = getNow();

      //因为工作时段是成对存在的，所以timelineList一定是两两一对的复数[0,1] [2,3] [4,5]
      int count = timelineList.size();
      // 一天4次打卡单独做判断，如果是上午下班打卡和下午上班打卡时间重叠，那么上午的下班卡取最早的，下午的上班卡取最晚的。用shiftCount是否等于-1判断，-1就走标准不重叠。2就表示重叠走新的逻辑
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      KQWorkTime kqWorkTime = new KQWorkTime();
      WorkTimeEntity workTime=kqWorkTime.getWorkTime(resourceId, kqDate);
      String preDate = DateUtil.addDate(kqDate, -1);//上一天日期
      String nextDate = DateUtil.addDate(kqDate, 1);//下一天日期
      List<TimeScopeEntity> lsSignTime = new ArrayList<>();
      List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
      if (workTime != null) {
        lsSignTime = workTime.getSignTime();//允许打卡时间
        lsWorkTime = workTime.getWorkTime();//工作时间
      }
      int shiftCount = lsWorkTime == null ? 0 : lsWorkTime.size();
      int shiftI = 0;
      String signEndDateTimeZero = "";
      for (int i = 0; lsWorkTime != null && i < lsWorkTime.size(); i++) {
        shiftI = i;
        TimeScopeEntity signTimeScope = lsSignTime.get(i);
        String signBeginDateTime = signTimeScope.getBeginTimeAcross() ? nextDate : kqDate;
        if(signTimeScope.isBeginTimePreAcross()){
          signBeginDateTime = preDate;
        }
        signBeginDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getBeginTime())+":00";
        String signEndDateTime = signTimeScope.getEndTimeAcross() ? nextDate : kqDate;
        signEndDateTime+=" "+kqTimesArrayComInfo.turn48to24Time(signTimeScope.getEndTime())+":59";
        if (shiftCount == 2 && shiftI == 0) {
          signEndDateTimeZero = signEndDateTime;
        }
        if (shiftCount == 2 && shiftI == 1) {
          shiftCount = signBeginDateTime.compareTo(signEndDateTimeZero) <= 0 ? shiftCount : -1;
        }
      }
      int workCnt = 0;
      List<String> recordLastButtomInfo = new ArrayList<>();
      String count4NoonStartDateTime = "";
      String count4NoonEndDateTime = "";
      for(int i = 0 ; i < count ; ){
        //成对出现的上下班时间
        int on_i = i;
        int off_i = i+1;
        Map<String,Object> timelineOnMap = (Map<String,Object>)timelineList.get(on_i);
        Map<String,Object> timelineOffMap = (Map<String,Object>)timelineList.get(off_i);
        if(timelineOnMap != null && !timelineOnMap.isEmpty() && timelineOffMap != null && !timelineOffMap.isEmpty()){
          String onTime = Util.null2String(timelineOnMap.get("time"));
          String belongdate = Util.null2String(timelineOnMap.get("belongdate"));
          //上班开始打卡时间
          String onSignSectionTime = Util.null2String(timelineOnMap.get("signSectionTime"));
          //上班结束打卡时间
          String signSectionEndTime = Util.null2String(timelineOnMap.get("signSectionEndTime"));
          String onDateTime = Util.null2String(timelineOnMap.get("datetime"));
          String onIsacross = Util.null2String(timelineOnMap.get("isacross"));

          String offTime = Util.null2String(timelineOffMap.get("time"));
          //下班开始打卡时间
          String signSectionBeginTime = Util.null2String(timelineOffMap.get("signSectionBeginTime"));
          //下班结束打卡时间
          String offSignSectionTime = Util.null2String(timelineOffMap.get("signSectionTime"));
          String offDateTime = Util.null2String(timelineOffMap.get("datetime"));
          String offIsacross = Util.null2String(timelineOffMap.get("isacross"));
          if(onSignSectionTime.length() == 0 || offSignSectionTime.length() == 0){
            kqLog.info(user.getLastname()+":"+curDate+":onSignSectionTime is null:"+onSignSectionTime+":offSignSectionTime is null:"+offSignSectionTime);
            continue;
          }
          if(signSectionEndTime.length() == 0 || signSectionBeginTime.length() == 0){
          }
          boolean isOn = false;
          if(i == (count-2) && shiftCount == 4&&count4NoonEndDateTime.equals(onSignSectionTime)){
            isOn = true;
          }
          LocalDateTime onLocalDateTime = LocalDateTime.parse(onSignSectionTime,fullFormatter);
          LocalDateTime onLocalDateEndTime = null;
          if(signSectionEndTime.length() > 0){
            onLocalDateEndTime = LocalDateTime.parse(signSectionEndTime,fullFormatter);
          }

          LocalDateTime offLocalDateBeginTime = null;
          if(signSectionBeginTime.length() > 0){
            offLocalDateBeginTime = LocalDateTime.parse(signSectionBeginTime,fullFormatter);
          }
          LocalDateTime offLocalDateTime = LocalDateTime.parse(offSignSectionTime,fullFormatter);

          LocalDateTime onworkDateTime = LocalDateTime.parse(onDateTime,fullFormatter);
          LocalDateTime offworkDateTime = LocalDateTime.parse(offDateTime,fullFormatter);

          if("1".equalsIgnoreCase(onIsacross)){
            onTime = new KQTimesArrayComInfo().turn24to48Time(onTime);
            timelineOffMap.put("belongtime", onTime);
          }
          if("1".equalsIgnoreCase(offIsacross)){
            offTime = new KQTimesArrayComInfo().turn24to48Time(offTime);
            timelineOffMap.put("belongtime", offTime);
          }
          String worksection = onTime+"-"+offTime;
          String signSection = onSignSectionTime+"#"+offSignSectionTime;
          timelineOnMap.put("signSection", signSection);
          timelineOnMap.put("signSectionEndTime", signSectionEndTime);
          timelineOffMap.put("signSection", signSection);
          timelineOffMap.put("signSectionBeginTime", signSectionBeginTime);

          if(i == (count-2)){
            //如果当前工作时段是最后一个工作时段
            timelineOnMap.put("islastsign", "1");
            timelineOffMap.put("islastsign", "1");
          }else{
            timelineOnMap.put("islastsign", "0");
            timelineOffMap.put("islastsign", "0");
          }
          if(i == 0){
            timelineOnMap.put("isfirstsign", "1");
          }else{
            timelineOnMap.put("isfirstsign", "0");
          }

          boolean hasSignIn = false;
          boolean hasSignOut = false;
          //是否在这个整体的签到签退范围内
          boolean isActive = false;
          //签到是否在允许的打卡范围内
          boolean isOnActive = false;
          //签推是否在允许的打卡范围内
          boolean isOffActive = false;
          if((now.isAfter(onLocalDateTime) && now.isBefore(offLocalDateTime))||(now.isEqual(onLocalDateTime))||(now.isEqual(offLocalDateTime))){
            if(isOn&&i == (count-2) && shiftCount == 4){   //如果当前工作时段是最后一个工作时段
              if(now.isAfter(onLocalDateTime) && now.isBefore(offworkDateTime)||(now.isEqual(onLocalDateTime))||(now.isEqual(offworkDateTime))) {
                if(recordLastButtomInfo.size() > 0 && "true".equals(recordLastButtomInfo.get(0))) {
                  isActive = true;
                }
              } else {
                isActive = true;
              }
            } else {
              isActive = true;
            }
          }

          if(now.isAfter(onworkDateTime)){
            timelineOnMap.put("isYellow", "1");
          }else{
            timelineOnMap.put("isYellow", "0");
          }
          if(now.isBefore(offworkDateTime)){
            timelineOffMap.put("isYellow", "1");
          }else{
            timelineOffMap.put("isYellow", "0");
          }
          if(onSignSectionTime.length() == 0 || offSignSectionTime.length() == 0){
            timelineOnMap.put("active", "0");
            timelineOffMap.put("active", "0");
            timelineOnMap.put("needSign", "0");
            timelineOffMap.put("needSign", "0");
            kqLog.info("考勤按钮报错："+user.getLastname()+"::"+curDate+":onSignSectionTime:"+onSignSectionTime+":offSignSectionTime:"+offSignSectionTime);
            return ;
          }
          //针对签到签退数据获取用的
          String sign_signSectionTime = "";
          String sign_signSectionEndTime = "";
          String sign_signSectionBeginTime = "";
          String sign_offSignSectionTime = "";
          if(signSectionEndTime.length() == 0 && signSectionBeginTime.length() == 0){
            //如果没设置上班后，下班前打卡
            sign_signSectionTime = onSignSectionTime;
            sign_offSignSectionTime = offSignSectionTime;
          }else{
            if(signSectionEndTime.length() > 0){
              if(signSectionBeginTime.length() > 0){
                //如果上班后，下班前打卡范围都做了控制
                if(isOn&&shiftCount == 4) {    // 针对一天4次卡的二开
                  if(onLocalDateTime.isAfter(offworkDateTime)) {
                    if((now.isAfter(onLocalDateTime) && now.isBefore(offworkDateTime)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(offworkDateTime))){
                      isOnActive = true;
                    }
                  } else {
                    if((now.isAfter(onLocalDateTime) && now.isBefore(onLocalDateEndTime)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(onLocalDateEndTime))){
                      isOnActive = true;
                    }
                  }
                } else {
                  if((now.isAfter(onLocalDateTime) && now.isBefore(onLocalDateEndTime)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(onLocalDateEndTime))){
                    isOnActive = true;
                  }
                }
                if((now.isAfter(offLocalDateBeginTime) && now.isBefore(offLocalDateTime)) ||(now.isEqual(offLocalDateBeginTime))||(now.isEqual(offLocalDateTime))){
                  isOffActive = true;
                }
                String onSignSection = onLocalDateTime.format(fullFormatter)+"#"+onLocalDateEndTime.format(fullFormatter);
                String offSignSection = offLocalDateBeginTime.format(fullFormatter)+"#"+offLocalDateTime.format(fullFormatter);
                timelineOnMap.put("signSection", onSignSection);
                timelineOffMap.put("signSection", offSignSection);
                sign_signSectionTime = onSignSectionTime;
                sign_signSectionEndTime = signSectionEndTime;
                sign_signSectionBeginTime = signSectionBeginTime;
                sign_offSignSectionTime = offSignSectionTime;

              }else{
                //如果只是上班后打卡范围做了控制
                if(isOn&&shiftCount == 4) {    // 针对一天4次卡的二开
                  if(onLocalDateTime.isAfter(offworkDateTime)) {
                    if((now.isAfter(onLocalDateTime) && now.isBefore(offworkDateTime)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(offworkDateTime))){
                      isOnActive = true;
                    }
                  } else {
                    if((now.isAfter(onLocalDateTime) && now.isBefore(onLocalDateEndTime)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(onLocalDateEndTime))){
                      isOnActive = true;
                    }
                  }
                } else {
                  if((now.isAfter(onLocalDateTime) && now.isBefore(onLocalDateEndTime)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(onLocalDateEndTime))){
                    isOnActive = true;
                  }
                }
                LocalDateTime tmp = LocalDateTime.parse(onLocalDateEndTime.plusMinutes(1).format(datetimeFormatter)+":00",fullFormatter);
                String tmp_datetime = tmp.format(fullFormatter);
                if((now.isAfter(tmp) && now.isBefore(offLocalDateTime)) ||(now.isEqual(tmp))||(now.isEqual(offLocalDateTime))){
                  isOffActive = true;
                }
                String onSignSection = onLocalDateTime.format(fullFormatter)+"#"+onLocalDateEndTime.format(fullFormatter);
                String offSignSection = tmp.format(fullFormatter)+"#"+offLocalDateTime.format(fullFormatter);
                timelineOnMap.put("signSection", onSignSection);
                timelineOffMap.put("signSection", offSignSection);
                sign_signSectionTime = onSignSectionTime;
                sign_signSectionEndTime = signSectionEndTime;
                sign_signSectionBeginTime = tmp_datetime;
                sign_offSignSectionTime = offSignSectionTime;

              }
            }else if(signSectionBeginTime.length() > 0){
              //如果只是下班前打卡范围做了控制
              LocalDateTime tmp = LocalDateTime.parse(offLocalDateBeginTime.minusMinutes(1).format(datetimeFormatter)+":59",fullFormatter);
              String tmp_datetime = tmp.format(fullFormatter);
              if((now.isAfter(onLocalDateTime) && now.isBefore(tmp)) ||(now.isEqual(onLocalDateTime))||(now.isEqual(tmp))){
                isOnActive = true;
              }
              if((now.isAfter(offLocalDateBeginTime) && now.isBefore(offLocalDateTime)) ||(now.isEqual(offLocalDateBeginTime))||(now.isEqual(offLocalDateTime))){
                isOffActive = true;
              }
              String onSignSection = onLocalDateTime.format(fullFormatter)+"#"+tmp_datetime;
              String offSignSection = offLocalDateBeginTime.format(fullFormatter)+"#"+offLocalDateTime.format(fullFormatter);
              timelineOnMap.put("signSection", onSignSection);
              timelineOffMap.put("signSection", offSignSection);
              sign_signSectionTime = onSignSectionTime;
              sign_signSectionEndTime = tmp_datetime;
              sign_signSectionBeginTime = signSectionBeginTime;
              sign_offSignSectionTime = offSignSectionTime;
            }

            timelineOnMap.put("signSectionEndTime", sign_signSectionEndTime);
            timelineOffMap.put("signSectionBeginTime", sign_signSectionBeginTime);
          }
          if(shiftCount == 4 && i == 0) {
            count4NoonStartDateTime = sign_signSectionEndTime.length()>0?sign_signSectionEndTime:offDateTime;
            count4NoonEndDateTime = sign_offSignSectionTime;
          }
          KQScheduleSignBiz kqScheduleSignBiz = new KQScheduleSignBiz.KQScheduleSignParamBuilder().resourceidParam(resourceId).shiftCountParam(shiftCount).shiftIParam(i)
                  .count4NoonStartDateTimeParam(count4NoonStartDateTime).count4NoonEndDateTimeParam(count4NoonEndDateTime)
                  .userTypeParam(user.getLogintype()).signSectionTimeParam(sign_signSectionTime)
                  .signSectionEndTimeParam(sign_signSectionEndTime).signSectionBeginTimeParam(sign_signSectionBeginTime)
                  .offSignSectionTimeParam(sign_offSignSectionTime).build();

          Map<String,KQHrmScheduleSign> signMap = kqScheduleSignBiz.getScheduleSignInfoWithCardRange();
          if(signMap != null && !signMap.isEmpty()){
            KQHrmScheduleSign signInTimeBean = signMap.get("signin");
            KQHrmScheduleSign signOutTimeBean =  signMap.get("signout");
            if(signInTimeBean != null){
              String signdate = Util.null2String(signInTimeBean.getSigndate());
              String signtimeTmp = Util.null2String(signInTimeBean.getSigntime());
              String signfrom = Util.null2String(signInTimeBean.getSignfrom());
              timelineOnMap.put("signfrom", signfrom);
              timelineOnMap.put("signbelong", "today");
              timelineOnMap.put("signbelongspan", SystemEnv.getHtmlLabelName(15537, user.getLanguage()));
              if(signdate.compareTo(curDate) < 0){
                timelineOnMap.put("signbelong", "yesterday");
                timelineOnMap.put("signbelongspan", SystemEnv.getHtmlLabelName(82640, user.getLanguage()));
              }
              timelineOnMap.put("signTime", signtimeTmp);
              timelineOnMap.put("position", Util.null2String(signInTimeBean.getAddr()));
              hasSignIn = true;
            }
            if(signOutTimeBean != null){
              String signdate = Util.null2String(signOutTimeBean.getSigndate());
              String signtimeTmp = Util.null2String(signOutTimeBean.getSigntime());
              String signfrom = Util.null2String(signOutTimeBean.getSignfrom());
              timelineOffMap.put("signfrom", signfrom);
              timelineOffMap.put("signbelong", SystemEnv.getHtmlLabelName(15537, user.getLanguage()));
              timelineOffMap.put("signbelongspan", SystemEnv.getHtmlLabelName(15537, user.getLanguage()));
              if(signdate.compareTo(curDate) < 0){
                timelineOffMap.put("signbelong", SystemEnv.getHtmlLabelName(82640, user.getLanguage()));
                timelineOffMap.put("signbelongspan", SystemEnv.getHtmlLabelName(82640, user.getLanguage()));
              }
              timelineOffMap.put("signTime", signtimeTmp);
              timelineOffMap.put("position", Util.null2String(signOutTimeBean.getAddr()));
              hasSignOut = true;
            }
          }
          //根据考勤工作时段获取相应时段下的签到签退数据
          if(!reports.isEmpty()){
            updateStatusByReport(reports,belongdate, timelineOnMap, timelineOffMap,worksection);
          }
          if(signSectionEndTime.length() == 0 && signSectionBeginTime.length() == 0){
            getActiveRestSign(isActive, hasSignIn, hasSignOut, timelineOnMap, timelineOffMap);
          }else{
            getActiveSign(isActive, hasSignIn, hasSignOut, timelineOnMap, timelineOffMap,isOnActive,isOffActive,shiftCount, i);
          }
          String lastButtomInfo = ""+hasSignOut;
          recordLastButtomInfo.add(lastButtomInfo);
        }

        i = i + 2;
        workCnt++;
      }
    }

  }

  /**
   * 根据考勤报表数据，更新打卡界面状态
   * @param reports
   * @param belongdate
   * @param timelineOnMap
   * @param timelineOffMap
   * @param worksection
   */
  public void updateStatusByReport(List<Object> reports, String belongdate,
                                   Map<String, Object> timelineOnMap,
                                   Map<String, Object> timelineOffMap, String worksection) {
    List<Object> checkInfoList = new ArrayList<>();
    for(int i = 0 ; i < reports.size() ; i++){
      Map<String, Object> reportData = (Map<String, Object>) reports.get(i);
      if(reportData != null && !reportData.isEmpty()){
        String kqdate = Util.null2String(reportData.get("kqdate"));
        if(belongdate.equalsIgnoreCase(kqdate)){
          checkInfoList = (List<Object>) reportData.get("checkInfo");
          if(checkInfoList != null && !checkInfoList.isEmpty()){
            for(int j = 0 ; j < checkInfoList.size() ; j++){
              Map<String, Object> kqData = (Map<String, Object>) checkInfoList.get(j);
              String workbegintime = Util.null2String(kqData.get("workbegintime"));
              String workendtime = Util.null2String(kqData.get("workendtime"));
              if(worksection.equalsIgnoreCase((workbegintime+"-"+workendtime))){
                //如果有考勤数据，再根据考勤数据同步更新下
                setStatusByReport(kqData,timelineOnMap,timelineOffMap);
                break;
              }
            }
          }
        }
      }
    }

  }

  /**
   * 根据考勤报表数据返回签到签退按钮状态
   * @param reportData
   * @param timelineOnMap
   * @param timelineOffMap
   */
  private void setStatusByReport(Map<String, Object> reportData, Map<String, Object> timelineOnMap, Map<String, Object> timelineOffMap) {

    String status = Util.null2String(reportData.get("status"));
    if(status.contains(ButtonStatusEnum.ABSENT.getStatusCode())){
      //旷工 旷工大于一切
      timelineOnMap.put("status", ButtonStatusEnum.ABSENT.getStatusCode());
      timelineOffMap.put("status", ButtonStatusEnum.ABSENT.getStatusCode());
      return ;
    }
    if(status.contains(ButtonStatusEnum.NORMAL.getStatusCode())){
      timelineOnMap.put("status", ButtonStatusEnum.NORMAL.getStatusCode());
      timelineOffMap.put("status", ButtonStatusEnum.NORMAL.getStatusCode());
      return ;
    }

    if(status.contains(ButtonStatusEnum.BELATE.getStatusCode())){
      //迟到
      timelineOnMap.put("status", ButtonStatusEnum.BELATE.getStatusCode());
    }
    if(status.contains(ButtonStatusEnum.LEAVEERALY.getStatusCode())){
      //早退
      timelineOffMap.put("status", ButtonStatusEnum.LEAVEERALY.getStatusCode());
    }
    if(status.contains(ButtonStatusEnum.NOSIGN.getStatusCode())){
      //漏签
      timelineOffMap.put("status", ButtonStatusEnum.NOSIGN.getStatusCode());
    }
  }

  private void getActiveRestSign(boolean isActive, boolean hasSignIn, boolean hasSignOut,
                                 Map<String, Object> timelineOnMap, Map<String, Object> timelineOffMap) {

    if(isActive){
      if(hasSignIn){
        if(hasSignOut){
          //签到了，签退了
          timelineOnMap.put("active", "0");
          timelineOffMap.put("active", "0");

          timelineOnMap.put("needSign", "0");
          timelineOffMap.put("needSign", "0");
          timelineOffMap.put("reSign", "1");

        }else{
          //签到了，未签退
          timelineOnMap.put("active", "0");
          timelineOffMap.put("active", "1");

          timelineOnMap.put("needSign", "0");
          timelineOffMap.put("needSign", "1");
        }
      }else{
        if(hasSignOut){
          //未签到，签退了  理论上是不可能的

        }else{
          //未签到未签退
          timelineOnMap.put("active", "1");
          timelineOffMap.put("active", "0");

          timelineOnMap.put("needSign", "1");
          timelineOffMap.put("needSign", "0");
        }
      }

    }else{
      //如果都不在考勤范围内肯定不能签到签退
      timelineOnMap.put("active", "0");
      timelineOffMap.put("active", "0");

      timelineOnMap.put("needSign", "0");
      timelineOffMap.put("needSign", "0");

    }
  }
  /**
   * 生成时间轴上 需要签到和活动考勤点的标识
   * @param isActive
   * @param hasSignIn
   * @param hasSignOut
   * @param timelineOnMap
   * @param timelineOffMap
   * @param isOnActive
   * @param isOffActive
   */
  private void getActiveSign(boolean isActive, boolean hasSignIn, boolean hasSignOut,
                             Map<String, Object> timelineOnMap, Map<String, Object> timelineOffMap,
                             boolean isOnActive, boolean isOffActive, int count, int i) {

    if(isActive){
      if(hasSignIn){
        if(hasSignOut){
          //签到了，签退了
          timelineOnMap.put("active", "0");
          timelineOnMap.put("needSign", "0");

          timelineOffMap.put("active", "0");
          timelineOffMap.put("needSign", "0");
          if(i == 0 && count == 4) {   //如果当前工作时段是最后一个工作时段, 上午时段有过签退就不能再更新签退了，因为第二次打卡是下午的上班卡

          } else {
            if (isOffActive) {
              timelineOffMap.put("reSign", "1");
            }
          }
        }else{
          //签到了，未签退
          timelineOnMap.put("active", "0");
          timelineOnMap.put("needSign", "0");

          if(isOffActive){
            timelineOffMap.put("needSign", "1");
            timelineOffMap.put("active", "1");
          }else{
            timelineOffMap.put("needSign", "0");
            timelineOffMap.put("active", "0");
          }
        }
      }else{
        if(hasSignOut){
          //未签到，签退了
          if(isOnActive){
            timelineOnMap.put("active", "1");
            timelineOnMap.put("needSign", "1");
          }else{
            timelineOnMap.put("active", "0");
          }

          timelineOffMap.put("active", "0");
          timelineOffMap.put("needSign", "0");

          if(i == 0 && count == 4) {   //如果当前工作时段是最后一个工作时段, 上午时段有过签退就不能再更新签退了，因为第二次打卡是下午的上班卡

          } else {
            if(isOffActive){
              timelineOffMap.put("reSign", "1");
            }
          }
        }else{
          //未签到未签退
          if(isOnActive){
            timelineOnMap.put("active", "1");
            timelineOnMap.put("needSign", "1");
          }else{
            timelineOnMap.put("active", "0");
          }
          if(isOffActive && !isOnActive){
            timelineOffMap.put("needSign", "1");
            timelineOffMap.put("active", "1");
          }else{
            timelineOffMap.put("active", "0");
          }

        }
      }

    }else{
      //如果都不在考勤范围内肯定不能签到签退
      timelineOnMap.put("active", "0");
      timelineOffMap.put("active", "0");

      timelineOnMap.put("needSign", "0");
      timelineOffMap.put("needSign", "0");

    }
  }

  /**
   * 先根据工作时段把考勤timeline基本信息加载一下
   * @param isYesterday
   * @param timeLine
   * @param timeLineButton
   * @param isNextday 是否是明日的
   */
  private void getWorkTimeButton(boolean isYesterday, List<Object> timeLine,
                                 List<Object> timeLineButton, boolean isNextday) {
    boolean show_yes_button = show_yes_button();
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String yesterday = LocalDate.parse(curDate).minusDays(1).format(dateFormatter);
    String nextday = LocalDate.parse(curDate).plusDays(1).format(dateFormatter);
    String next_nextday = LocalDate.parse(nextday).plusDays(1).format(dateFormatter);

    for(int i = 0 ; i < timeLine.size() ;){
      Map<String,Object> onTimelineMap = (Map<String,Object>)timeLine.get(i);
      Map<String,Object> offTimelineMap = (Map<String,Object>)timeLine.get(i+1);
      if(isYesterday){
        //如果允许打卡时间也跨天，那么也需要显示出来按钮
        if(Util.null2String(offTimelineMap.get("signAcross")).equalsIgnoreCase("1")){
          Map<String,Object> onTimelineMapTmp = reBuildYesTimeMap(onTimelineMap,yesterday,"on");
          Map<String,Object> offTimelineMapTmp = reBuildYesTimeMap(offTimelineMap,yesterday,"off");
          if(!show_yes_button && offTimelineMapTmp.containsKey("signSectionTime")){
            String signSectionTime = Util.null2s(Util.null2String(offTimelineMapTmp.get("signSectionTime")),"");
            if(signSectionTime.length() > 0 && now.isAfter(LocalDateTime.parse(signSectionTime,fullFormatter))){
              i = i + 2;
              continue;
            }
          }
          timeLineButton.add(onTimelineMapTmp);
          timeLineButton.add(offTimelineMapTmp);
        }
      }else if(isNextday){
        if(onTimelineMap.containsKey("sign_preAcross")){
          if("1".equalsIgnoreCase(Util.null2String(onTimelineMap.get("sign_preAcross")))){
            Map<String,Object> onTimelineMapTmp = reBuildNextTimeMap(onTimelineMap,nextday,next_nextday,"on");
            Map<String,Object> offTimelineMapTmp = reBuildNextTimeMap(offTimelineMap,nextday,next_nextday,"off");
            timeLineButton.add(onTimelineMapTmp);
            timeLineButton.add(offTimelineMapTmp);
          }
        }
      }else {
        Map<String,Object> onTimelineMapTmp = reBuildTimeMap(onTimelineMap,nextday,"on");
        Map<String,Object> offTimelineMapTmp = reBuildTimeMap(offTimelineMap,nextday,"off");
        timeLineButton.add(onTimelineMapTmp);
        timeLineButton.add(offTimelineMapTmp);
      }
      i = i + 2;
    }

  }

  /**
   * 是否显示昨日失效的考勤按钮
   * @return
   */
  public boolean show_yes_button() {
    boolean show_yes_button = true;
    RecordSet rs = new RecordSet();
    String settingSql = "select * from KQ_SETTINGS where main_key='show_yes_button'";
    rs.executeQuery(settingSql);
    if(rs.next()){
      String main_val = rs.getString("main_val");
      if(!"1".equalsIgnoreCase(main_val)){
        show_yes_button = false;
      }
    }
    return show_yes_button;
  }

  /**
   * 针对前一天的工作时间 根据跨天判断重构一遍数据
   * @param timelineMap
   * @param yesterday
   * @param isOnOff
   */
  private Map<String,Object> reBuildYesTimeMap(Map<String, Object> timelineMap, String yesterday,
                                               String isOnOff) {

    Map<String,Object> lineMap = new HashMap<>();

    String isacross = Util.null2String(timelineMap.get("isacross"));
    String min = Util.null2String(timelineMap.get("min"));
    String min_next = Util.null2String(timelineMap.get("min_next"));
    String time = Util.null2String(timelineMap.get("time"));
    String workmins = Util.null2String(timelineMap.get("workmins"));
//    打卡时段设置 一定是要开启的
    String isPunchOpen = "1";
    String signAcross = Util.null2String(timelineMap.get("signAcross"));
    String signAcross_next = Util.null2String(timelineMap.get("signAcross_next"));
    int tMin = Util.getIntValue(min,0);
    int tMin_next = Util.getIntValue(min_next,-1);

    lineMap.put("type", timelineMap.get("type"));
    lineMap.put("serialid", timelineMap.get("serialid"));
    lineMap.put("isacross", isacross);
    lineMap.put("time", time);
    lineMap.put("belongtime", time);
    lineMap.put("min", min);
    lineMap.put("min_next", min_next);
    lineMap.put("workmins", workmins);
    lineMap.put("isPunchOpen", isPunchOpen);
    lineMap.put("signAcross", signAcross);
    lineMap.put("signAcross_next", signAcross_next);

    if("on".equalsIgnoreCase(isOnOff)){
      if("1".equalsIgnoreCase(isacross)){
        //上班时间，跨天
        lineMap.put("date", curDate);//打卡日期
        lineMap.put("belongdate", yesterday);//工作时段所属日期
        lineMap.put("pre", "0");//是否是昨日
        lineMap.put("across", "0");//是否是次日
        lineMap.put("datetime", curDate+" "+time+":00");//应打卡日期+时间

        String canSignTime = curDate + " "+ time + ":00";
        if(tMin > 0) {
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = curDate + " "+ time + ":59";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin_next);
          lineMap.put("signSectionEndTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignEndTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }else{
        //上班时间，不跨天
        lineMap.put("date", yesterday);
        lineMap.put("belongdate", yesterday);
        lineMap.put("pre", "1");
        lineMap.put("across", "0");
        lineMap.put("datetime", yesterday+" "+time+":00");//应打卡日期+时间
        String canSignTime = yesterday + " "+ time + ":00";

        if(tMin > 0) {
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = yesterday + " "+ time + ":59";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin_next);
          lineMap.put("signSectionEndTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignEndTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }
    }else{
      if("1".equalsIgnoreCase(isacross)){
        //下班时间，跨天
        lineMap.put("date", curDate);
        lineMap.put("belongdate", yesterday);
        lineMap.put("pre", "0");
        lineMap.put("across", "0");
        lineMap.put("datetime", curDate+" "+time+":00");//应打卡日期+时间
        String canSignTime = curDate + " "+ time + ":59";
        if(tMin > 0) {
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = curDate + " "+ time + ":00";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin_next);
          lineMap.put("signSectionBeginTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignBeginTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }else{
        //下班时间，不跨天
        lineMap.put("date", yesterday);
        lineMap.put("belongdate", yesterday);
        lineMap.put("pre", "1");
        lineMap.put("across", "0");
        lineMap.put("datetime", yesterday+" "+time+":00");//应打卡日期+时间
        String canSignTime = yesterday + " "+ time + ":59";
        //如果开启了打卡时段设置
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = yesterday + " "+ time + ":00";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin_next);
          lineMap.put("signSectionBeginTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignBeginTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }

    }
    return lineMap;
  }

  /**
   * 针对今天的工作时间 根据跨天判断重构一遍数据
   * @param timelineMap
   * @param nextday
   * @param isOnOff
   */
  private Map<String,Object> reBuildTimeMap(Map<String, Object> timelineMap, String nextday,
                                            String isOnOff) {
    Map<String,Object> lineMap = new HashMap<>();

    String isacross = Util.null2String(timelineMap.get("isacross"));
    String min = Util.null2String(timelineMap.get("min"));
    String time = Util.null2String(timelineMap.get("time"));
    String min_next = Util.null2String(timelineMap.get("min_next"));
    String workmins = Util.null2String(timelineMap.get("workmins"));
//    打卡时段设置 一定是要开启的
    String isPunchOpen = "1";
    String signAcross = Util.null2String(timelineMap.get("signAcross"));
    String signAcross_next = Util.null2String(timelineMap.get("signAcross_next"));
    int tMin = Util.getIntValue(min,0);
    int tMin_next = Util.getIntValue(min_next,-1);

    lineMap.put("type", timelineMap.get("type"));
    lineMap.put("serialid", timelineMap.get("serialid"));
    lineMap.put("isacross", isacross);
    lineMap.put("time", time);
    lineMap.put("belongtime", time);
    lineMap.put("min", min);
    lineMap.put("min_next", min_next);
    lineMap.put("workmins", workmins);
    lineMap.put("isPunchOpen", isPunchOpen);
    lineMap.put("signAcross", signAcross);
    lineMap.put("signAcross_next", signAcross_next);

    if("on".equalsIgnoreCase(isOnOff)){
      if("1".equalsIgnoreCase(isacross)){
        //上班时间，跨天
        lineMap.put("date", nextday);
        lineMap.put("belongdate", curDate);
        lineMap.put("pre", "0");
        lineMap.put("across", "1");
        lineMap.put("datetime", nextday+" "+time+":00");//应打卡日期+时间
        String canSignTime = nextday + " "+ time + ":00";
        //如果开启了打卡时段设置
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = nextday + " "+ time + ":59";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin_next);
          lineMap.put("signSectionEndTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignEndTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }else{
        //上班时间，不跨天
        lineMap.put("date", curDate);
        lineMap.put("belongdate", curDate);
        lineMap.put("pre", "0");
        lineMap.put("across", "0");
        lineMap.put("datetime", curDate+" "+time+":00");//应打卡日期+时间
        String canSignTime = curDate + " "+ time + ":00";
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = curDate + " "+ time + ":59";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin_next);
          lineMap.put("signSectionEndTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignEndTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }
    }else{
      if("1".equalsIgnoreCase(isacross)){
        //下班时间，跨天
        lineMap.put("date", nextday);
        lineMap.put("belongdate", curDate);
        lineMap.put("pre", "0");
        lineMap.put("across", "1");
        lineMap.put("datetime", nextday+" "+time+":00");//应打卡日期+时间
        String canSignTime = nextday + " "+ time + ":59";
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = nextday + " "+ time + ":00";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin_next);
          lineMap.put("signSectionBeginTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignBeginTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }else{
        //下班时间，不跨天
        lineMap.put("date", curDate);
        lineMap.put("belongdate", curDate);
        lineMap.put("pre", "0");
        lineMap.put("across", "0");
        lineMap.put("datetime", curDate+" "+time+":00");//应打卡日期+时间
        String canSignTime = curDate + " "+ time + ":59";
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = curDate + " "+ time + ":00";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin_next);
          lineMap.put("signSectionBeginTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignBeginTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }

    }
    return lineMap;
  }


  /**
   * 针对明日的工作时间 根据跨天判断重构一遍数据
   * @param timelineMap
   * @param nextday
   * @param next_nextday
   * @param isOnOff
   */
  private Map<String,Object> reBuildNextTimeMap(Map<String, Object> timelineMap, String nextday, String next_nextday,
                                                String isOnOff) {
    Map<String,Object> lineMap = new HashMap<>();

    String isacross = Util.null2String(timelineMap.get("isacross"));
    String min = Util.null2String(timelineMap.get("min"));
    String time = Util.null2String(timelineMap.get("time"));
    String min_next = Util.null2String(timelineMap.get("min_next"));
    String workmins = Util.null2String(timelineMap.get("workmins"));
//    打卡时段设置 一定是要开启的
    String isPunchOpen = "1";
    String signAcross = Util.null2String(timelineMap.get("signAcross"));
    String signAcross_next = Util.null2String(timelineMap.get("signAcross_next"));
    int tMin = Util.getIntValue(min,0);
    int tMin_next = Util.getIntValue(min_next,-1);

    lineMap.put("type", timelineMap.get("type"));
    lineMap.put("serialid", timelineMap.get("serialid"));
    lineMap.put("isacross", isacross);
    lineMap.put("time", time);
    lineMap.put("belongtime", time);
    lineMap.put("min", min);
    lineMap.put("min_next", min_next);
    lineMap.put("workmins", workmins);
    lineMap.put("isPunchOpen", isPunchOpen);
    lineMap.put("signAcross", signAcross);
    lineMap.put("signAcross_next", signAcross_next);

//    因为当前是明日的班次，所以这个across就都搞成1，前端的话就显示次日
    if("on".equalsIgnoreCase(isOnOff)){
      if("1".equalsIgnoreCase(isacross)){
        //上班时间，跨天
        lineMap.put("date", next_nextday);
        lineMap.put("belongdate", nextday);
        lineMap.put("across", "1");
        lineMap.put("datetime", next_nextday+" "+time+":00");//应打卡日期+时间
        String canSignTime = next_nextday + " "+ time + ":00";
        //如果开启了打卡时段设置
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = next_nextday + " "+ time + ":59";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin_next);
          lineMap.put("signSectionEndTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignEndTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }else{
        //上班时间，不跨天
        lineMap.put("date", nextday);
        lineMap.put("belongdate", nextday);
        lineMap.put("pre", "0");
        lineMap.put("across", "1");
        lineMap.put("datetime", nextday+" "+time+":00");//应打卡日期+时间
        String canSignTime = nextday + " "+ time + ":00";
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = nextday + " "+ time + ":59";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin_next);
          lineMap.put("signSectionEndTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignEndTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }
    }else{
      if("1".equalsIgnoreCase(isacross)){
        //下班时间，跨天
        lineMap.put("date", next_nextday);
        lineMap.put("belongdate", nextday);
        lineMap.put("pre", "0");
        lineMap.put("across", "1");
        lineMap.put("datetime", next_nextday+" "+time+":00");//应打卡日期+时间
        String canSignTime = next_nextday + " "+ time + ":59";
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = next_nextday + " "+ time + ":00";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin_next);
          lineMap.put("signSectionBeginTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignBeginTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }else{
        //下班时间，不跨天
        lineMap.put("date", nextday);
        lineMap.put("belongdate", nextday);
        lineMap.put("pre", "0");
        lineMap.put("across", "1");
        lineMap.put("datetime", nextday+" "+time+":00");//应打卡日期+时间
        String canSignTime = nextday + " "+ time + ":59";
        if(tMin > 0){
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).plusMinutes(tMin);
          lineMap.put("signSectionTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
        if(tMin_next > -1) {
          canSignTime = nextday + " "+ time + ":00";
          LocalDateTime tmpLocalDateTime = LocalDateTime.parse(canSignTime, fullFormatter).minusMinutes(tMin_next);
          lineMap.put("signSectionBeginTime",tmpLocalDateTime.format(fullFormatter));
          lineMap.put("canSignBeginTime",tmpLocalDateTime.toLocalTime().format(timeFormatter));
        }
      }

    }
    return lineMap;
  }

  public String getCurDate() {
    return curDate;
  }

  public void setCurDate(String curDate) {
    this.curDate = curDate;
  }

  public void setNow(LocalDateTime now){
    this.now = now;
  }

  public LocalDateTime getNow() {
    return now;
  }

  public LocalDateTime getZoneOfClientDateTime(String now_date, String now_time,
                                               Map<String, Object> logMap){
    LocalDateTime now_zone = null;
    //处理多时区
    String timeZoneConversion = Util.null2String(new BaseBean().getPropValue("weaver_timezone_conversion","timeZoneConversion")).trim();
    logMap.put("timeZoneConversion", timeZoneConversion);
    if("1".equals(timeZoneConversion)) {
      DateTransformer dateTransformer=new DateTransformer();
      DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      String[] zone_localTime = dateTransformer.getLocaleDateAndTime(now_date,now_time);
      kqLog.info((user != null ? user.getLastname() : "")+"::"+"now_date:"+now_date+":now_time:"+now_time+"::"
              +(zone_localTime !=null ? JSON.toJSONString(zone_localTime): ""));
      logMap.put("zone_localTime", (zone_localTime !=null ? JSON.toJSONString(zone_localTime): ""));
      if(zone_localTime != null && zone_localTime.length == 2){
        now_date = zone_localTime[0];
        now_time = zone_localTime[1];
      }
      String dateTime = now_date+" "+now_time;
      now_zone = LocalDateTime.parse(dateTime,fullFormatter);
    }
    logMap.put("now_zone", now_zone);
    return now_zone;
  }

  @Override
  public BizLogContext getLogContext() {
    return null;
  }

}
