package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.EMManager;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQCardLogBiz;
import com.engine.kq.biz.KQFormatBiz;
import com.engine.kq.biz.KQGroupBiz;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQLoactionComInfo;
import com.engine.kq.biz.KQShiftRuleInfoBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.timer.KQQueue;
import com.engine.kq.timer.KQTaskBean;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.wfset.util.SplitActionUtil;
import com.google.common.collect.Maps;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.dateformat.DateTransformer;
import weaver.dateformat.TimeZoneVar;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

/**
 * 考勤的 签到签退
 */
public class PunchButtonCmd extends AbstractCommonCommand<Map<String, Object>> {
  public KQLog kqLog = new KQLog();
  private HttpServletRequest request;
  private Map<String,Object> logMap = Maps.newHashMap();
  private Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();

	public PunchButtonCmd(HttpServletRequest request,Map<String, Object> params, User user) {
		this.request = request;
	  this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try{
          //==zj  判断主账号有没有考勤组
          KqUtil kqUtil = new KqUtil();
          String userId = Util.null2String(user.getUID());  //获取主账号userid
          String today = DateUtil.getCurrentDate();         //获取当前日期

          Boolean flag = kqUtil.mainAccountCheck(userId, today);
          new BaseBean().writeLog("==zj==(点击打卡)" + flag);
          if (!flag){
            //如果主账号没有考勤组，再看子账号是否有考勤组
            List<Map> list = kqUtil.childAccountCheck(userId, today);
            new BaseBean().writeLog("==zj==(点击打卡-maps)" + JSON.toJSONString(list));
            if (list.size()  == 1){
              //如果只有一个生效考勤组
              Map<String,String> map = list.get(0);
              int userid = Integer.parseInt(map.get("userid"));
              User user = new User(userid);
              this.user = user;
            }
          }

      insertSign(retmap);

    }catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
    kqLog.info(user.getLastname()+":PunchButtonCmd:retmap:"+retmap);
    KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "punchButton");

    return retmap;
	}

  /**
   * 检验ip是否在考勤组设置的范围要求内
   * @param ismobile
   */
  private boolean checkIsInIp(String ismobile) {
//    if("1".equalsIgnoreCase(ismobile)){
//      return true;
//    }
    KQGroupBiz kqGroupBiz = new KQGroupBiz();
    String clientAddress = Util.getIpAddr(request);
    kqLog.info("PunchButtonCmd:clientAddress:"+clientAddress);
    return kqGroupBiz.getIsInScopeV4V6(user.getUID()+"", clientAddress,ismobile);
  }

  public void insertSign(Map<String, Object> retmap) throws Exception{
    logMap.put("lastname", user.getLastname());
    logMap.put("params", params);
    kqLog.info(user.getLastname()+":PunchButtonCmd:params:"+params);
	  RecordSet rs = new RecordSet();
    String deviceInfo = Util.null2String(params.get("deviceInfo"));
    JSONObject jsonObject = null;
    if(deviceInfo.length() > 0){
      jsonObject = JSON.parseObject(deviceInfo);
      JSONObject jsonObject1 = new JSONObject();
      Set<Entry<String, Object>> jsonSet =  jsonObject.entrySet();
      for(Entry<String, Object> js : jsonSet){
        String key = js.getKey();
        String value = Util.null2String(js.getValue());
        jsonObject1.put(key, value);
      }
      if(!jsonObject1.isEmpty()){
        deviceInfo = jsonObject1.toJSONString();
      }
    }
    //应上班 工作时间点
    String time = Util.null2String(params.get("time"));
    //应上班 工作时间 带日期
    String datetime = Util.null2String(params.get("datetime"));
    //允许打卡时段 带日期
    String signSectionTime = Util.null2String(params.get("signSectionTime"));
    //上传照片
    String attachment = Util.null2String(params.get("fileids"));

    //打卡所属worksection的对应的点
    String type = Util.null2String(params.get("type"));
    //所属打卡日期
    String belongdate = Util.null2String(params.get("belongdate"));
    belongdate = belongdate.length() == 0 ? DateUtil.getCurrentDate() : belongdate;
    String islastsign = Util.null2String(params.get("islastsign"));
    String isfirstsign = Util.null2String(params.get("isfirstsign"));

    String workmins = Util.null2String(params.get("workmins"));
    //针对非工作时段  签退的时候记录的签到数据 用于计算加班
    String signInTime4Out = Util.null2String(params.get("signInTime4Out"));
    //允许打卡的范围
    String signsection = Util.null2String(params.get("signSection"));

    //手机打卡部分
    String longitude = Util.null2String(params.get("longitude"));
    String latitude = Util.null2String(params.get("latitude"));
    double d_longitude = Util.getDoubleValue(longitude);
    double d_latitude = Util.getDoubleValue(latitude);
    if(d_latitude <= 0){
      latitude = "";
    }
    if(d_longitude <= 0){
      longitude = "";
    }
    //wifi用的
    String mac = Util.null2String(params.get("mac"));
    String sid = Util.null2String(params.get("sid"));
    String addr = Util.null2String(params.get("position"));
    String ismobile = Util.null2String(params.get("ismobile"));
    //区分是来自于钉钉还是EM7
    String browser = Util.null2String(params.get("browser"));
    //自由班制处理
    String isfree = Util.null2String(Util.null2String(params.get("isfree")),"0");

    //上班打卡 允许最晚打卡时间
    String signSectionEndTime = Util.null2String(params.get("signSectionEndTime"));
    //下班打卡 允许最早打卡时间
    String signSectionBeginTime = Util.null2String(params.get("signSectionBeginTime"));

    String locationshowaddress = Util.null2String(params.get("locationshowaddress"));
    if(locationshowaddress.equals("1")){//记录统一地址
      String locationid = Util.null2String(params.get("locationid"));//办公地点id
      if(locationid.length()>0){//如果开启统一显示，就用配置的地址
        KQLoactionComInfo kqLoactionComInfo = new KQLoactionComInfo();
        addr = kqLoactionComInfo.getLocationname(locationid);
      }
    }

    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    LocalTime localTime = LocalTime.now();
    String signTime =localTime.format(dateTimeFormatter);
    String signDate = LocalDate.now().format(dateFormatter);

    String timeZone = Util.null2String(TimeZoneVar.getTimeZone(),"");
    //处理多时区
    String timeZoneConversion = Util.null2String(new weaver.general.BaseBean().getPropValue("weaver_timezone_conversion","timeZoneConversion")).trim();
    logMap.put("timeZoneConversion", timeZoneConversion);
    if("1".equals(timeZoneConversion)) {
      DateTransformer dateTransformer=new DateTransformer();
      String[] zone_localTime = dateTransformer.getLocaleDateAndTime(signDate,signTime);
      kqLog.info(user.getLastname()+":TimeZoneVar.getTimeZone():"+TimeZoneVar.getTimeZone()+":zone_localTime:"+JSON.toJSONString(zone_localTime));
      if(zone_localTime != null && zone_localTime.length == 2){
        signDate = zone_localTime[0];
        signTime = zone_localTime[1];
      }
    }

    int userId = user.getUID();
    String userType = user.getLogintype();
    String signType = "on".equalsIgnoreCase(type) ? "1" : "2";
    String clientAddress = Util.getIpAddr(request);
    boolean isInIp = checkIsInIp(ismobile);
    logMap.put("clientAddress", clientAddress);

    if(!isInIp){
      retmap.put("message",  SystemEnv.getHtmlLabelName(20157,user.getLanguage()));
      retmap.put("isInIp",  "0");
    }
    String isInCom = isInIp ? "1" : "0";

    //是否是考勤例外人员
    boolean isExclude = false;

    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(user.getUID()+"", signDate);
    String userinfo = "#userid#"+user.getUID()+"#getUserSubCompany1#"+user.getUserSubCompany1()+"#getUserSubCompany1#"+user.getUserDepartment()
        +"#getJobtitle#"+user.getJobtitle();
    workTimeEntityLogMap.put("resourceid", userinfo);
    workTimeEntityLogMap.put("splitDate", signDate);
    workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);
    String groupid = workTimeEntity.getGroupId();
    logMap.put("groupid", groupid);
    KQGroupEntity kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
    String kqGroupEntityInfo = kqGroupEntity != null ? JSON.toJSONString(kqGroupEntity): "";
    logMap.put("kqGroupEntityInfo", kqGroupEntityInfo);
    if (kqGroupEntity != null && ("," + kqGroupEntity.getExcludeid() + ",").indexOf("," + user.getUID() + ",")>-1) {//排除人员无需计算考勤时间
      isExclude = true;
    }

    String[] signsections = signsection.split("#");
    if(!"1".equalsIgnoreCase(isfree)){
      if(signsections != null && signsections.length == 2){
        //判断是未签到直接签退
        String signedMsg = signedMsg(userId+"", signType, user,signsections,signSectionBeginTime,signSectionEndTime);
        if(signedMsg.length() > 0){
          retmap.put("status", "1");
          retmap.put("message",  signedMsg);
          isInCom = "0";
        }
      }
    }

    String datetime_timezone = signDate+" "+signTime;
    LocalDateTime nowDateTime = LocalDateTime.parse(datetime_timezone,fullFormatter);
    kqLog.info("timeZone:"+timeZone+":signDate:"+signDate+":signTime:"+signTime+":nowDateTime:"+nowDateTime);

    if("1".equalsIgnoreCase(signType) && signSectionTime.length() > 0 ){
      LocalDateTime startWorkDateTime = LocalDateTime.parse(signSectionTime,fullFormatter);
      if(nowDateTime.isBefore(startWorkDateTime)){
        Duration duration = Duration.between(nowDateTime, startWorkDateTime);
        retmap.put("status", "1");
        retmap.put("message",  ""+ SystemEnv.getHtmlLabelName(10005326,weaver.general.ThreadVarLanguage.getLang())+""+duration.toMinutes()+""+ SystemEnv.getHtmlLabelName(15049,weaver.general.ThreadVarLanguage.getLang())+"");
        isInCom = "0";
      }

    }else if("2".equalsIgnoreCase(signType) && signSectionTime.length() > 0 ){
      LocalDateTime endWorkDateTime = LocalDateTime.parse(signSectionTime,fullFormatter);
      if(nowDateTime.isAfter(endWorkDateTime)){
        Duration duration = Duration.between(endWorkDateTime, nowDateTime);
        retmap.put("status", "1");
        retmap.put("message",  ""+ SystemEnv.getHtmlLabelName(10005327,weaver.general.ThreadVarLanguage.getLang())+""+duration.toMinutes()+""+ SystemEnv.getHtmlLabelName(15049,weaver.general.ThreadVarLanguage.getLang())+"");
        isInCom = "0";
      }
    }

    //记录下是来自于E9的pc端签到
    String signfrom = "e9pc";
    if("1".equalsIgnoreCase(ismobile)){
      signfrom = "e9mobile";
      boolean needLocationRange = false;
      boolean needWifiRange = false;
      boolean isLocationRange = false;
      boolean isWifiRange = false;
      KQGroupBiz kqGroupBiz = new KQGroupBiz();
      Map<String,Object> locationMap = kqGroupBiz.checkLocationScope(userId+"",longitude,latitude);
      logMap.put("locationMap", locationMap);
      String locationNeedCheck = Util.null2String(locationMap.get("needCheck"));
      boolean locationInScope = Boolean.parseBoolean(Util.null2String(locationMap.get("inScope")));
      if("1".equalsIgnoreCase(locationNeedCheck)){
        needLocationRange = true;
        if(locationInScope){
          isLocationRange = true;
        }
      }
      String wifiNeedCheck = "";
      Map<String,Object> wifiMap = kqGroupBiz.checkWifiScope(userId+"", sid, mac);
      logMap.put("wifiMap", wifiMap);
      wifiNeedCheck = Util.null2String(wifiMap.get("needCheck"));
      boolean wifiInScope = Boolean.parseBoolean(Util.null2String(wifiMap.get("inScope")));
      if("1".equalsIgnoreCase(wifiNeedCheck)){
        needWifiRange = true;
        if(wifiInScope){
          isWifiRange = true;
        }
      }
      if(needLocationRange){
        if(isLocationRange){
        }else{
          if(needWifiRange){
            if(isWifiRange){
            }else{
              //地理位置开启，而且不在范围内，且开启wifi验证,不在范围内
              retmap.put("message",  SystemEnv.getHtmlLabelName(507524, user.getLanguage()));
              isInCom = "0";
            }
          }else {
            //地理位置开启，而且不在范围内，且未开启wifi验证
            retmap.put("message",  SystemEnv.getHtmlLabelName(500510, user.getLanguage()));
            isInCom = "0";
          }
        }
      }else{
        if(needWifiRange) {
          if (isWifiRange) {
          } else {
            //地理位置未开启，且开启wifi验证,不在范围内
            retmap.put("message", SystemEnv.getHtmlLabelName(507524, user.getLanguage()));
            isInCom = "0";
          }
        }
      }

      if("DingTalk".equalsIgnoreCase(browser)){
        signfrom = "DingTalk";
      }else if("Wechat".equalsIgnoreCase(browser)){
        signfrom = "Wechat";
        String weChat_deviceid = Util.null2String(request.getSession().getAttribute(EMManager.DeviceId));
        logMap.put("weChat_deviceid", weChat_deviceid);
        kqLog.info("EMManager.DeviceId:"+EMManager.DeviceId+":weChat_deviceid:"+weChat_deviceid);
        if(weChat_deviceid.length() > 0){
          //微信打卡的设备号需要单独处理
          if(jsonObject != null){
            jsonObject.put("deviceId", weChat_deviceid);
          }else{
            jsonObject = new JSONObject();
            jsonObject.put("deviceId", weChat_deviceid);
          }
          if(!jsonObject.isEmpty()){
            deviceInfo = jsonObject.toJSONString();
          }
        }
      }
    }
    String signStatus = "";

    if(!"1".equalsIgnoreCase(isfree) && datetime.length() > 0){
      signStatus = getSignStatus(signType,datetime,user.getUID()+"",belongdate,nowDateTime);
      logMap.put("signStatus", signStatus);
    }

    if(isExclude){
      signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
    }

    String punchSql = "insert into HrmScheduleSign(userId,userType,signType,signDate,signTime,clientAddress,isInCom,timeZone,belongdate,signfrom,longitude,latitude,addr,deviceInfo) "+
        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    boolean isOk = rs.executeUpdate(punchSql,userId,userType,signType,signDate,signTime,clientAddress,isInCom,
        timeZone,belongdate,signfrom,longitude,latitude,addr,deviceInfo);
    if(!isOk){
      retmap.put("status", "1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      return ;
    }
    logMap.put("punchSql", punchSql);
    logMap.put("punchSql_isOk", isOk);
    kqLog.info(user.getLastname()+":PunchButtonCmd:punchSql:"+punchSql+":isOk:"+isOk);

    //打卡提醒处理
    String remindSql = "insert into hrmschedulesign_remind(userId,signType,signDate,signTime,belongdate) values(?,?,?,?,?)";
    isOk = rs.executeUpdate(remindSql, userId,signType,signDate,signTime,belongdate);
    kqLog.info(user.getLastname()+":PunchButtonCmd:remindSql:"+remindSql+":isOk:"+isOk);

    //同步更新考勤数据到考勤报表
    new KQFormatBiz().formatDate(""+userId,(belongdate.length() == 0 ? DateUtil.getCurrentDate() : belongdate));
    //点击签退的时候，可能存在加班数据生成的情况
    if("2".equalsIgnoreCase(signType)){
      if("1".equalsIgnoreCase(islastsign)){
        List<KQTaskBean> tasks = new ArrayList<>();
        List<KQTaskBean> after_tasks = new ArrayList<>();
        SplitActionUtil.pushOverTimeTasks(belongdate, belongdate, ""+userId,tasks);
        if(!tasks.isEmpty()){
          for(KQTaskBean kqTaskBean : tasks){
            after_tasks.add(kqTaskBean);
          }
        }
        logMap.put("after_tasks", after_tasks);
        if(!after_tasks.isEmpty()){
          KQQueue.writeTasks(after_tasks);
        }
      }
    }
    if("1".equalsIgnoreCase(signType)){
      if("1".equalsIgnoreCase(isfirstsign)){
        List<KQTaskBean> tasks = new ArrayList<>();
        List<KQTaskBean> before_tasks = new ArrayList<>();
        SplitActionUtil.pushOverTimeTasks(belongdate, belongdate, ""+userId,tasks);
        if(!tasks.isEmpty()){
          for(KQTaskBean kqTaskBean : tasks){
            kqTaskBean.setTasktype("punchcard");
            before_tasks.add(kqTaskBean);
          }
        }
        logMap.put("before_tasks", before_tasks);
        if(!before_tasks.isEmpty()){
          KQQueue.writeTasks(before_tasks);
        }
      }
    }

    String reSignStatus = reSignStatus(user.getUID()+"",signType,nowDateTime,belongdate);
    if(Util.null2String(reSignStatus,"").length() > 0){
      signStatus = reSignStatus;
    }

    retmap.put("status", "1");
    retmap.put("signdate", signDate);
    retmap.put("signtime", signTime);
    retmap.put("kqstatus", signStatus);

    if(!"1".equalsIgnoreCase(signStatus) && !"2".equalsIgnoreCase(signStatus)){
      if("".equalsIgnoreCase(Util.null2String(retmap.get("message")))){
        retmap.put("success", "1");
        retmap.put("message",  SystemEnv.getHtmlLabelName(512596, Util.getIntValue(user.getLanguage())));
      }
    }
    logMap.put("retmap", retmap);
  }

  public String reSignStatus(String userid, String signType, LocalDateTime nowDateTime,
      String workdate) {
    String signStatus = "";

    String shift_begindateworktime = "";
    String shift_enddateworktime = "";
    ShiftInfoBean shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(userid, workdate,false);
    if(shiftInfoBean == null){
      return signStatus;
    }
    Map<String,String> shifRuleMap = Maps.newHashMap();
    KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean,userid,shifRuleMap);
    KQTimesArrayComInfo arrayComInfo = new KQTimesArrayComInfo();
    if(!shifRuleMap.isEmpty()) {
      DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      String datetime = "";
      if (shifRuleMap.containsKey("shift_beginworktime")) {
        String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
        if (shift_beginworktime.length() > 0) {
          int shift_beginworktime_index = -1;
          shift_beginworktime_index = arrayComInfo.getArrayindexByTimes(shift_beginworktime);
          datetime = workdate+" "+shift_beginworktime+":00";
          if(shift_beginworktime_index >= 1440){
            //跨天了
            datetime = DateUtil.addDate(workdate, 1)+" "+arrayComInfo.turn48to24Time(shift_beginworktime)+":00";
          }
        }
      }
      if (shifRuleMap.containsKey("shift_endworktime")) {
        String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
        if (shift_endworktime.length() > 0) {
          int shift_endworktime_index = -1;
          shift_endworktime_index = arrayComInfo.getArrayindexByTimes(shift_endworktime);
          datetime = workdate+" "+shift_endworktime+":00";
          if(shift_endworktime_index >= 1440) {
            //跨天了
            datetime = DateUtil.addDate(workdate, 1)+" "+arrayComInfo.turn48to24Time(shift_endworktime)+":00";
          }
        }
      }

      if (datetime.length() > 0) {
        if("1".equalsIgnoreCase(signType)) {
          LocalDateTime startWorkDateTime = LocalDateTime.parse(datetime, fullFormatter);
          //打卡时间比上班时间晚，迟到了
          if (nowDateTime.isAfter(startWorkDateTime)) {
            signStatus = ButtonStatusEnum.BELATE.getStatusCode();
          } else {
            signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
          }
        } else if ("2".equalsIgnoreCase(signType)) {
          LocalDateTime endWorkDateTime = LocalDateTime.parse(datetime, fullFormatter);
          //签退的话
          if (nowDateTime.isBefore(endWorkDateTime)) {
            signStatus = ButtonStatusEnum.LEAVEERALY.getStatusCode();
          } else {
            signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
          }
        } else {
          writeLog(user.getLastname() + nowDateTime + ":竟然没有传:" + signType);
          return "";
        }
      }
    }
    return signStatus;
  }

  /**
   * 上班前打卡目前是 视作前一天的跨天加班
   * @param resourceid
   * @param pre_splitDate
   * @param signtime
   * @param pre_bengintime
   * @param signdate
   */
  public void doBeforeAcrossOvertime(String resourceid,String pre_splitDate,String signtime,String pre_bengintime,String signdate) {

    KQTaskBean kqTaskBean = new KQTaskBean();
    kqTaskBean.setResourceId(resourceid);
    kqTaskBean.setTaskDate(pre_splitDate);
    kqTaskBean.setLastWorkTime(signtime);
    if(pre_bengintime.length() == 5){
      kqTaskBean.setTaskSignTime(pre_bengintime+":00");
    }else{
      kqTaskBean.setTaskSignTime(pre_bengintime);
    }
    kqTaskBean.setSignDate(signdate);
    kqTaskBean.setSignEndDate(signdate);
    kqTaskBean.setTimesource("before");
    KQQueue.writeTask(kqTaskBean);
  }

  /**
   * 校验是否已经签到过
   * @param userid
   * @param signtype
   * @param curUser
   * @param signsections
   * @param signSectionBeginTime 下班打卡 允许最早打卡时间
   * @param signSectionEndTime 上班打卡 允许最晚打卡时间
   * @return
   */
  public String signedMsg(String userid, String signtype, User curUser, String[] signsections,
      String signSectionBeginTime, String signSectionEndTime) throws Exception{
    String signedMsg = "";
    RecordSet rs = new RecordSet();
    boolean hasSigned = false;
    String onSignSectionTime = signsections[0];
    String offSignSectionTime = signsections[1];
    if(onSignSectionTime.length() > 0 && offSignSectionTime.length() > 0){
      String hasSign = "select 1 from hrmschedulesign where 1 = 1 and isInCom = '1' and userid = ? ";

      StringBuffer sql = new StringBuffer();
      if(rs.getDBType().equals("oracle")||rs.getDBType().equals("postgresql")){
        sql.append(" AND signDate||' '||signTime>=? ");
        sql.append(" AND signDate||' '||signTime<=? ");
      }else if(rs.getDBType().equals("mysql")){
        sql.append(" AND concat(signDate,' ',signTime)>=? ");
        sql.append(" AND concat(signDate,' ',signTime)<=? ");
      }else{
        sql.append(" AND signDate+' '+signTime>=? ");
        sql.append(" AND signDate+' '+signTime<=? ");
      }
      hasSign += sql.toString();
      rs.executeQuery(hasSign, userid,onSignSectionTime,offSignSectionTime);
      if(rs.next()){
        hasSigned = true;
      }
      if("1".equalsIgnoreCase(signtype)){
        if(signSectionBeginTime.length() > 0 || signSectionEndTime.length() > 0){
        }else{
          if(hasSigned){
            signedMsg = SystemEnv.getHtmlLabelName(129706, curUser.getLanguage());
          }
        }
      }else if("2".equalsIgnoreCase(signtype)){
        if(signSectionBeginTime.length() > 0 || signSectionEndTime.length() > 0){
        }else{
          if(!hasSigned){
            signedMsg = SystemEnv.getHtmlLabelName(501301, curUser.getLanguage());
          }
        }
      }
    }
    return signedMsg;
  }
  /**
   * 在签到签退的时候先根据打卡数据
   * 粗步 得到打卡状态 正常，迟到，早退
   * @return
   */
  public String  getSignStatus(String signType, String datetime, String userid, String workdate,
      LocalDateTime nowDateTime) {
    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String signStatus = "";

    //签到的话
    if("1".equalsIgnoreCase(signType)){
      LocalDateTime startWorkDateTime = LocalDateTime.parse(datetime,fullFormatter);
      //打卡时间比上班时间晚，迟到了
      if(nowDateTime.isAfter(startWorkDateTime)){
        signStatus = ButtonStatusEnum.BELATE.getStatusCode();
      }else{
        signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
      }
    }else if("2".equalsIgnoreCase(signType)){
      LocalDateTime endWorkDateTime = LocalDateTime.parse(datetime,fullFormatter);
      //签退的话
      if(nowDateTime.isBefore(endWorkDateTime)){
        signStatus = ButtonStatusEnum.LEAVEERALY.getStatusCode();
      }else{
        signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
      }
    }else{
      writeLog(user.getLastname()+nowDateTime+":竟然没有传:"+signType);
      return "";
    }
    return signStatus;
  }

  @Override
	public BizLogContext getLogContext() {
		return null;
	}

}
