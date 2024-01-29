package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.EMManager;
import com.customization.qc2443677.SubmitRequestUtil;
import com.customization.qc2443677.checkUtil;
import com.customization.qc2443677.signDataUtil;
import com.customization.qc2443677.signRequestUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQCardLogBiz;
import com.engine.kq.biz.KQFormatBiz;
import com.engine.kq.biz.KQGroupBiz;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.util.SplitActionUtil;
import com.engine.workflow.constant.PAResponseCode;
import com.engine.workflow.entity.publicApi.PAResponseEntity;
import com.engine.workflow.entity.publicApi.ReqOperateRequestEntity;
import com.google.common.collect.Maps;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * 外勤签到签退
 */
public class PunchOutButtonCmd extends AbstractCommonCommand<Map<String, Object>> {
  private HttpServletRequest request;
  public KQLog kqLog = new KQLog();
  private Map<String,Object> logMap = Maps.newHashMap();
  private Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();

  public PunchOutButtonCmd(HttpServletRequest request,Map<String, Object> params, User user) {
    this.request = request;
    this.user = user;
    this.params = params;
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String, String> resultMap = new HashMap<String, String>();
    Map<String, Object> retmap = new HashMap<String, Object>();
    signRequestUtil signRequestUtil = new signRequestUtil();  //创建流程
    checkUtil checkUtil = new checkUtil();    //检查流程
    signDataUtil signDataUtil = new signDataUtil(); //获取打卡所需要数据
    try{
      //这里获取其它打卡数据
      new BaseBean().writeLog("==zj==(外勤打卡params)" + JSON.toJSONString(params));
      Map<String,Object> otherParams = new HashMap<String,Object>();
      otherParams = signDataUtil.getsignDate(user,request,params);
      //qc2443677 先判断该打卡是否在班次范围内
      if (!checkUtil.checkSignTime(user)){
        //如果不在班次范围内，就不允许打卡
        retmap.put("status", "-1");
        retmap.put("message", "打卡时间不在今日班次打卡时间范围内");
      }
      //qc2443677 先判断是否有正在审批中的外勤打卡流程
      Boolean isHave = checkUtil.checkSignWf(user);
      if(isHave){
        retmap.put("status", "-1");
        retmap.put("message", "有审批中的外勤流程，不允许再次打外勤卡");
        return retmap;
      }
      //qc2443677 判断是否有退回的外勤打卡流程，如果有，就在退回流程上更新数据
      JSONArray mainTable = signRequestUtil.setMainTable(request,user, params,otherParams); //设置主表数据
      resultMap = checkUtil.checkReject(user, otherParams,mainTable);
      new BaseBean().writeLog("==zj==(resultMap)" + JSON.toJSONString(resultMap));
      if("1".equals(resultMap.get("code"))){
        /*ReqOperateRequestEntity requestParam = new ReqOperateRequestEntity();
        SubmitRequestUtil submitRequestUtil = new SubmitRequestUtil(user,requestParam);
        int rejectRequestid = Integer.parseInt(resultMap.get("requestid"));
        PAResponseEntity resultEntity = submitRequestUtil.submitRequest(rejectRequestid);*/
          retmap.put("status", "1");
          retmap.put("message", "打卡流程提交成功");
          return retmap;
      }else if ("-1".equals(resultMap.get("code"))){
        retmap.put("status", "-1");
        retmap.put("message", "打卡流程更新失败");
        return retmap;
      } else {
        //qc2443677 如果没有退回流程这里触发外勤打卡审批流程
        String wfId = new BaseBean().getPropValue("qc2443677","workflowId");  //配置对应流程wfID
        new BaseBean().writeLog("==zj==(wfId)" + wfId);
        new BaseBean().writeLog("==zj==(otherParams)"+JSON.toJSONString(otherParams));
         mainTable = signRequestUtil.setMainTable(request,user, params,otherParams); //设置主表数据
        int requestid = signRequestUtil.createRequest( request,user, wfId, mainTable);
        new BaseBean().writeLog("==zj==(创建流程requestid)" + JSON.toJSONString(requestid));
        if (requestid == 0){
          retmap.put("status", "-1");
          retmap.put("message", "创建流程失败");
        }else {
          retmap.put("status", "1");
          retmap.put("message", "打卡审批流程已提交");
        }
      }
      /*insertSign(retmap);*/
    }catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      writeLog(e);
    }

    KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "punchOutButton");
    return retmap;
  }

  /**
   * 检验ip是否在考勤组设置的范围要求内
   */
  private boolean checkIsInIp() {
    KQGroupBiz kqGroupBiz = new KQGroupBiz();
    String clientAddress = Util.getIpAddr(request);
    return kqGroupBiz.getIsInScope(user.getUID()+"", clientAddress);
  }

  private void insertSign(Map<String, Object> retmap) {
    logMap.put("lastname", user.getLastname());
    logMap.put("params", params);
//    signSection: 2019-03-20 08:30#2019-03-20 18:30

    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    String serialid = Util.null2String(params.get("serialid"));
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
    boolean belongdateIsNull = belongdate.length()==0;
    String islastsign = Util.null2String(params.get("islastsign"));

    String isPunchOpen = Util.null2String(params.get("isPunchOpen"));

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
    String address = Util.null2String(params.get("address"));
    String ismobile = Util.null2String(params.get("ismobile"));
    String remark = Util.null2String(params.get("remark"));
    String attachment = Util.null2String(params.get("fileids"));
    //区分是来自于钉钉还是EM7
    String browser = Util.null2String(params.get("browser"));
    //客户
    String crm = Util.null2String(params.get("crm"));
    //是否开启外勤签到转考勤
    String outsidesign = "";
    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    KQGroupEntity kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
    String kqGroupEntityInfo = kqGroupEntity != null ? JSON.toJSONString(kqGroupEntity): "";
    logMap.put("kqGroupEntityInfo", kqGroupEntityInfo);
    if (kqGroupEntity != null) {
      outsidesign = kqGroupEntity.getOutsidesign();
    }
    kqLog.info(user.getLastname()+":params:"+params+":outsidesign:"+outsidesign);

    int userId = user.getUID();
    String signfrom = "e9_mobile_out";
    DateTimeFormatter allFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    LocalDateTime localTime = LocalDateTime.now();
    String signTime =localTime.format(dateTimeFormatter);
    String signDate = localTime.format(dateFormatter);

    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(user.getUID()+"", signDate);
    String userinfo = "#userid#"+user.getUID()+"#getUserSubCompany1#"+user.getUserSubCompany1()+"#getUserSubCompany1#"+user.getUserDepartment()
        +"#getJobtitle#"+user.getJobtitle();
    workTimeEntityLogMap.put("resourceid", userinfo);
    workTimeEntityLogMap.put("splitDate", signDate);
    workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);

    //处理多时区
    String timeZoneConversion = Util.null2String(new weaver.general.BaseBean().getPropValue("weaver_timezone_conversion","timeZoneConversion")).trim();
    logMap.put("timeZoneConversion", timeZoneConversion);
    if("1".equals(timeZoneConversion)) {
      DateTransformer dateTransformer=new DateTransformer();
      String[] zone_localTime = dateTransformer.getLocaleDateAndTime(signDate,signTime);
      if(zone_localTime != null && zone_localTime.length == 2){
        signDate = zone_localTime[0];
        signTime = zone_localTime[1];
      }
    }
    String timeZone = Util.null2String(TimeZoneVar.getTimeZone(),"");

    String mobile_sign_sql = "insert into mobile_sign(operater,operate_type,operate_date,operate_time,longitude,latitude,address,remark,attachment,crm,timezone) "
        + " values(?,?,?,?,?,?,?,?,?,?,?) ";
    rs1.executeUpdate(mobile_sign_sql, userId,signfrom,signDate,signTime,longitude,latitude,address,remark,attachment,crm,timeZone);

    logMap.put("outsidesign", outsidesign);
    if("1".equalsIgnoreCase(outsidesign)){

      JSONObject jsonObject = null;
      String deviceInfo = Util.null2String(params.get("deviceInfo"));
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

      if("DingTalk".equalsIgnoreCase(browser)){
        signfrom = "DingTalk_out";
      }else if("Wechat".equalsIgnoreCase(browser)){
        signfrom = "Wechat_out";
        String weChat_deviceid = Util.null2String(request.getSession().getAttribute(EMManager.DeviceId));
        kqLog.info("EMManager.DeviceId:"+EMManager.DeviceId+":weChat_deviceid:"+weChat_deviceid);
        logMap.put("weChat_deviceid", weChat_deviceid);

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
      //自由班制处理
      String isfree = Util.null2String(params.get("isfree"));

      String userType = user.getLogintype();
      String signType = "on".equalsIgnoreCase(type) ? "1" : "2";
      String clientAddress = Util.getIpAddr(request);
      boolean isInIp = true;

      String isInCom = isInIp ? "1" : "0";

      String datetime_timezone = signDate+" "+signTime;
      LocalDateTime nowDateTime = LocalDateTime.parse(datetime_timezone,allFormatter);
      kqLog.info("timeZone:"+timeZone+":signDate:"+signDate+":signTime:"+signTime+":nowDateTime:"+nowDateTime);

      boolean isInScope = true;
      if(signsection != null && signsection.length() > 0){
        List<String> signsectionList = Util.TokenizerString(signsection, ",");
        for(int i = 0 ; i < signsectionList.size() ; i++){
          String signsections = Util.null2String(signsectionList.get(i));
          String[] signsection_arr = signsections.split("#");
          if(signsection_arr != null && signsection_arr.length == 2){
            String canStart = signsection_arr[0];
            String canEnd = signsection_arr[1];
            LocalDateTime startSignDateTime = LocalDateTime.parse(canStart,allFormatter);
            LocalDateTime endSignDateTime = LocalDateTime.parse(canEnd,allFormatter);
            if(nowDateTime.isBefore(startSignDateTime) || nowDateTime.isAfter(endSignDateTime)){
              isInScope = false;
            }else{
              isInScope = true;
              break;
            }
          }
        }
      }
      if(!isInScope){
        //外勤的不在范围内也不管，全部计入考勤表
//        retmap.put("status", "1");
//        retmap.put("message", SystemEnv.getHtmlLabelName(503597 , user.getLanguage()));
//        return ;
      }
      if(belongdate.length() == 0){
        belongdate = signDate;
      }

      String punchSql = "insert into HrmScheduleSign(userId,userType,signType,signDate,signTime,clientAddress,isInCom,timeZone,belongdate,signfrom,longitude,latitude,addr,deviceInfo) "+
          " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      boolean isok = rs.executeUpdate(punchSql,userId,userType,signType,signDate,signTime,clientAddress,isInCom,
          timeZone,belongdate,signfrom,longitude,latitude,address,deviceInfo);

      logMap.put("punchSql", punchSql);
      logMap.put("punchSql_isok", isok);
      //同步更新考勤数据到考勤报表
      if(belongdateIsNull){
        //外勤签到没有归属日期，遇到跨天班次打卡可能归属前一天，需要格式化前一天考勤
        kqLog.info("PunchOutButtonCmd:userId:"+userId+":belongdate:"+DateUtil.getYesterday());
        new KQFormatBiz().formatDate(""+userId,DateUtil.getYesterday());
      }
      kqLog.info("PunchOutButtonCmd:userId:"+userId+":belongdate:"+(belongdate.length() == 0 ? DateUtil.getCurrentDate() : belongdate));
      if(belongdate.length()==0){
        //外勤签到没有归属日期，遇到跨天班次打卡可能归属前一天，需要格式化前一天考勤
        new KQFormatBiz().formatDate(""+userId,DateUtil.getYesterday());
      }
      new KQFormatBiz().formatDate(""+userId,(belongdate.length() == 0 ? DateUtil.getCurrentDate() : belongdate));
      //外勤签到转的考勤 处理加班规则
      SplitActionUtil.pushOverTimeTasksAll(belongdate,belongdate,""+userId);
    }
    retmap.put("status", "1");
    retmap.put("signdate", signDate);
    retmap.put("signtime", signTime);
    logMap.put("retmap", retmap);
  }

  @Override
  public BizLogContext getLogContext() {
    return null;
  }

}
