package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.customization.qc2505403.IpCheckUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.KQShiftRuleEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.util.SplitActionUtil;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
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

	public PunchButtonCmd(HttpServletRequest request,Map<String, Object> params, User user) {
		this.request = request;
	  this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try{
          insertSign(retmap);
    }catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
	}

  /**
   * 检验ip是否在考勤组设置的范围要求内
   * @param ismobile
   */
  private boolean checkIsInIp(String ismobile) {
    //qc2505403 这里修改下，只有pc端才要查看ip地址，移动端直接返回true
    new BaseBean().writeLog("==zj==(ismobile)" + JSON.toJSONString(ismobile));
   if("1".equalsIgnoreCase(ismobile)){
      return true;
    }
    KQGroupBiz kqGroupBiz = new KQGroupBiz();
    String clientAddress = Util.getIpAddr(request);
    new BaseBean().writeLog("==zj==(clientAddress)" + JSON.toJSONString(clientAddress));
    kqLog.info("PunchButtonCmd:clientAddress:"+clientAddress);
    return kqGroupBiz.getIsInScope(user.getUID()+"", clientAddress);
  }

  public void insertSign(Map<String, Object> retmap) throws Exception{
    kqLog.info(user.getLastname()+":PunchButtonCmd:params:"+params);
	  RecordSet rs = new RecordSet();
    //应上班 工作时间点
    String time = Util.null2String(params.get("time"));
    //应上班 工作时间 带日期
    String datetime = Util.null2String(params.get("datetime"));
    //允许打卡时段 带日期
    String signSectionTime = Util.null2String(params.get("signSectionTime"));

    //打卡所属worksection的对应的点
    String type = Util.null2String(params.get("type"));
    //所属打卡日期
    String belongdate = Util.null2String(params.get("belongdate"));
    String islastsign = Util.null2String(params.get("islastsign"));

    String workmins = Util.null2String(params.get("workmins"));
    //针对非工作时段  签退的时候记录的签到数据 用于计算加班
    String signInTime4Out = Util.null2String(params.get("signInTime4Out"));
    //允许打卡的范围
    String signsection = Util.null2String(params.get("signSection"));

    //手机打卡部分
    String longitude = Util.null2String(params.get("longitude"));
    String latitude = Util.null2String(params.get("latitude"));
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

    KQFormatData kqFormatData = new KQFormatData();
    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    LocalTime localTime = LocalTime.now();
    String signTime =localTime.format(dateTimeFormatter);
    String signDate = LocalDate.now().format(dateFormatter);

    int userId = user.getUID();
    String userType = user.getLogintype();
    String signType = "on".equalsIgnoreCase(type) ? "1" : "2";
    String clientAddress = Util.getIpAddr(request);
    boolean isInIp = checkIsInIp(ismobile);
    if(!isInIp){
      retmap.put("message",  SystemEnv.getHtmlLabelName(20157,user.getLanguage()));
      retmap.put("isInIp",  "0");
    }
    String isInCom = isInIp ? "1" : "0";

    //是否是考勤例外人员
    boolean isExclude = false;

    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    String groupid = kqGroupMemberComInfo.getKQGroupId(user.getUID()+"",signDate);
    KQGroupEntity kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
    if (("," + kqGroupEntity.getExcludeid() + ",").indexOf("," + user.getUID() + ",")>-1) {//排除人员无需计算考勤时间
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

    String timeZone = Util.null2String(TimeZoneVar.getTimeZone(),"");

    LocalDateTime nowDateTime = LocalDateTime.now();

    if("1".equalsIgnoreCase(signType) && signSectionTime.length() > 0 ){
      LocalDateTime startWorkDateTime = LocalDateTime.parse(signSectionTime,fullFormatter);
      if(nowDateTime.isBefore(startWorkDateTime)){
        Duration duration = Duration.between(nowDateTime, startWorkDateTime);
        retmap.put("status", "1");
        retmap.put("message",  "最早签到开始时间还差"+duration.toMinutes()+"分钟");
        isInCom = "0";
      }

    }else if("2".equalsIgnoreCase(signType) && signSectionTime.length() > 0 ){
      LocalDateTime endWorkDateTime = LocalDateTime.parse(signSectionTime,fullFormatter);
      if(nowDateTime.isAfter(endWorkDateTime)){
        Duration duration = Duration.between(endWorkDateTime, nowDateTime);
        retmap.put("status", "1");
        retmap.put("message",  "最晚签退时间已经结束"+duration.toMinutes()+"分钟");
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
              retmap.put("message",  SystemEnv.getHtmlLabelName(500510, user.getLanguage()));
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
            retmap.put("message", SystemEnv.getHtmlLabelName(500510, user.getLanguage()));
            isInCom = "0";
          }
        }
      }

      if("DingTalk".equalsIgnoreCase(browser)){
        signfrom = "DingTalk";
      }else if("Wechat".equalsIgnoreCase(browser)){
        signfrom = "Wechat";
      }
    }
    String signStatus = "";

    if(!"1".equalsIgnoreCase(isfree) && datetime.length() > 0){
      signStatus = getSignStatus(signType,datetime,user.getUID()+"",signDate);
    }

    if(isExclude){
      signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
    }

    String punchSql = "insert into HrmScheduleSign(userId,userType,signType,signDate,signTime,clientAddress,isInCom,timeZone,belongdate,signfrom,longitude,latitude,addr) "+
        " values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
    boolean isOk = rs.executeUpdate(punchSql,userId,userType,signType,signDate,signTime,clientAddress,isInCom,
        timeZone,belongdate,signfrom,longitude,latitude,addr);
    if(!isOk){
      retmap.put("status", "1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      return ;
    }
    kqLog.info(user.getLastname()+":PunchButtonCmd:punchSql:"+punchSql+":isOk:"+isOk);

    //打卡提醒处理
    String remindSql = "insert into hrmschedulesign_remind(userId,signType,signDate,signTime,belongdate) values(?,?,?,?,?)";
    isOk = rs.executeUpdate(remindSql, userId,signType,signDate,signTime,belongdate);
    kqLog.info(user.getLastname()+":PunchButtonCmd:remindSql:"+remindSql+":isOk:"+isOk);

    //同步更新考勤数据到考勤报表
    new KQFormatBiz().formatDate(""+userId,(belongdate.length() == 0 ? DateUtil.getCurrentDate() : belongdate));
    //点击签退的时候，可能存在加班数据生成的情况
    if("2".equalsIgnoreCase(signType)){
      SplitActionUtil.pushOverTimeTasksAll(belongdate, belongdate, ""+userId);
    }


    retmap.put("status", "1");
    retmap.put("signdate", signDate);
    retmap.put("signtime", signTime);
    retmap.put("kqstatus", signStatus);

    if(!"1".equalsIgnoreCase(signStatus) && !"2".equalsIgnoreCase(signStatus)){
      if("".equalsIgnoreCase(Util.null2String(retmap.get("message")))){
        retmap.put("success", "1");
        retmap.put("message",  SystemEnv.getHtmlLabelNames("512596", user.getLanguage()));
      }
    }
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
      if(rs.getDBType().equals("oracle")){
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
  public String  getSignStatus(String signType,String datetime,String userid,String workdate) {
    DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String signStatus = "";

    LocalDateTime nowDateTime = LocalDateTime.now();
    KQFormatShiftRule kqFormatShiftRule = new KQFormatShiftRule();
    //签到的话
    if("1".equalsIgnoreCase(signType)){
      LocalDateTime startWorkDateTime = LocalDateTime.parse(datetime,fullFormatter);
      //yz 添加允许迟到时间验证
      KQWorkTime kqWorkTime = new KQWorkTime();
      WorkTimeEntity workTime = kqWorkTime.getWorkTime(userid,workdate);
//      Map<String, Object> shiftRuleInfo = workTime.getShiftRuleInfo();
//      if (shiftRuleInfo != null && !shiftRuleInfo.isEmpty()) {
//        kqLog.info("workTime:"+ JSON.toJSONString(workTime));
//        String permitlatestatus = Util.null2String(shiftRuleInfo.get("permitlatestatus"));
//        if ("1".equals(permitlatestatus)) {
//          String permitlateminutes = Util.null2String(workTime.getShiftRuleInfo().get("permitlateminutes"));
//          startWorkDateTime = startWorkDateTime.plus(Util.getIntValue(permitlateminutes,0), ChronoUnit.MINUTES);//minus 减；plus 加
//        }
//      }
      //打卡时间比上班时间晚，迟到了
      if(nowDateTime.isAfter(startWorkDateTime)){
        Duration duration = Duration.between(startWorkDateTime, nowDateTime);
        int beLateMins = (int)duration.toMinutes();
        if(nowDateTime.getSecond()>0)beLateMins++;
        KQShiftRuleEntity kqShiftRuleEntity = new KQShiftRuleEntity();
        kqShiftRuleEntity.setUserId(userid);
        kqShiftRuleEntity.setKqDate(workdate);
        kqShiftRuleEntity.setBelatemins(beLateMins);
        kqLog.info("打卡人性化规则处理前数据"+ JSONObject.toJSONString(kqShiftRuleEntity));
        kqShiftRuleEntity = kqFormatShiftRule.doShiftRule(workTime,kqShiftRuleEntity);
        kqLog.info("人性化规则处理后数据"+JSONObject.toJSONString(kqShiftRuleEntity));
        if(kqShiftRuleEntity.getBelatemins()>0) {
          signStatus = ButtonStatusEnum.BELATE.getStatusCode();
        }else{
          signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
        }
      }else{
        signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
      }
    }else if("2".equalsIgnoreCase(signType)){
      LocalDateTime endWorkDateTime = LocalDateTime.parse(datetime,fullFormatter);
      //签退的话

      //yz 添加允许早退时间验证
      KQWorkTime kqWorkTime = new KQWorkTime();
      WorkTimeEntity workTime = kqWorkTime.getWorkTime(userid,workdate);
//      Map<String, Object> shiftRuleInfo = workTime.getShiftRuleInfo();
//      if (shiftRuleInfo != null && !shiftRuleInfo.isEmpty()) {
//        kqLog.info("workTime:"+ JSON.toJSONString(workTime));
//        String permitleaveearlystatus = Util.null2String(shiftRuleInfo.get("permitleaveearlystatus"));
//        if ("1".equals(permitleaveearlystatus)) {
//          String permitleaveearlyminutes = Util.null2String(workTime.getShiftRuleInfo().get("permitleaveearlyminutes"));
//          endWorkDateTime = endWorkDateTime.minus(Util.getIntValue(permitleaveearlyminutes,0), ChronoUnit.MINUTES);//minus 减；plus 加
//        }
//      }
      //打卡时间比下班班时间晚，早退了
      if(nowDateTime.isBefore(endWorkDateTime)){
        Duration duration = Duration.between(nowDateTime, endWorkDateTime);
        int leaveEarlyMins = (int)duration.toMinutes();
        KQShiftRuleEntity kqShiftRuleEntity = new KQShiftRuleEntity();
        kqShiftRuleEntity.setUserId(userid);
        kqShiftRuleEntity.setKqDate(workdate);
        kqShiftRuleEntity.setEarlyInMins(kqFormatShiftRule.getEarlyInMins(userid,workdate));
        kqShiftRuleEntity.setLeaveearlymins(leaveEarlyMins);
        kqLog.info("打卡人性化规则处理前数据"+ JSONObject.toJSONString(kqShiftRuleEntity));
        kqShiftRuleEntity = kqFormatShiftRule.doShiftRule(workTime,kqShiftRuleEntity);
        kqLog.info("人性化规则处理后数据"+JSONObject.toJSONString(kqShiftRuleEntity));
        if(kqShiftRuleEntity.getLeaveearlymins()>0) {
          signStatus = ButtonStatusEnum.LEAVEERALY.getStatusCode();
        }else{
          signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
        }
      }else{
        signStatus = ButtonStatusEnum.NORMAL.getStatusCode();
      }
    }else{
      writeLog(user.getLastname()+LocalDateTime.now()+":竟然没有传:"+signType);
      return "";
    }
    return signStatus;
  }

  @Override
	public BizLogContext getLogContext() {
		return null;
	}

}
