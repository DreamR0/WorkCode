package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQCardLogBiz;
import com.engine.kq.biz.KQGroupBiz;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

/**
 * 获取外勤签到签退按钮
 */
public class GetOutButtonsCmd extends AbstractCommonCommand<Map<String, Object>> {

  private KQLog kqLog = new KQLog();
  private String curDate = DateUtil.getCurrentDate();
  private Map<String,Object> logMap = Maps.newHashMap();
  private Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();
  
	public GetOutButtonsCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
      //==zj  判断主账号有没有考勤组
      KqUtil kqUtil = new KqUtil();
      String userIdMain = Util.null2String(user.getUID());  //获取主账号userid
      String today = DateUtil.getCurrentDate();         //获取当前日期

      Boolean flag = kqUtil.mainAccountCheck(userIdMain, today);
      new BaseBean().writeLog("==zj==(GetOutButtonsCmd)" + flag);
      if (!flag){
        //如果主账号没有考勤组，再看子账号是否有考勤组
        List<Map> list = kqUtil.childAccountCheck(userIdMain, today);
        new BaseBean().writeLog("==zj==(GetOutButtonsCmd-maps)" + JSON.toJSONString(list));
        if (list.size()  == 1){
          //如果只有一个生效考勤组
          Map<String,String> map = list.get(0);
          int userid = Integer.parseInt(map.get("userid"));
          User user = new User(userid);
          this.user = user;
        }
      }


    logMap.put("lastname", user.getLastname());
    logMap.put("params", params);
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{

      kqLog.info("GetOutButtonsCmd:"+user.getLastname()+"::"+params);
      String userId = user.getUID()+"";
      //position用的
      String longitude = Util.null2String(params.get("longitude"));
      String latitude = Util.null2String(params.get("latitude"));
      //wifi用的
      String networkType = Util.null2String(params.get("networkType"));
      String mac = Util.null2String(params.get("mac"));
      String sid = Util.null2String(params.get("sid"));

      KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
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
      kqLog.info("GetOutButtonsCmd:"+user.getLastname()+":groupid:"+groupid+":kqGroupEntity:"+(kqGroupEntity != null ? JSON.toJSONString(kqGroupEntity): ""));
      if (kqGroupEntity != null) {
        outsidesign = kqGroupEntity.getOutsidesign();
      }
      String rangekey = "";
      String rangeid = "";
      String rangename = "";
      boolean isRange = false;
      boolean isLocationRange = false;
      boolean isWifiRange = false;

      KQGroupBiz kqGroupBiz = new KQGroupBiz();
      Map<String,Object> locationMap = kqGroupBiz.checkLocationScope(userId,longitude,latitude);
      String locationNeedCheck = Util.null2String(locationMap.get("needCheck"));
      boolean locationInScope = Boolean.parseBoolean(Util.null2String(locationMap.get("inScope")));
      logMap.put("locationMap", locationMap);
      if("1".equalsIgnoreCase(locationNeedCheck)){
        if(locationInScope){
          Map<String,Object> loactionInfo = (Map<String, Object>) locationMap.get("loactionInfo");
          isLocationRange = true;
          rangeid = Util.null2String(loactionInfo.get("id"));
          rangename = Util.null2String(loactionInfo.get("locationname"));
          rangekey = "position";
        }else{
          kqLog.info(user.getLastname()+":办公地点不在考勤范围内:GetOutButtonsCmd:userId:"+userId+":longitude:"+longitude+":latitude:"+latitude);
        }
      }
      //优先返回地点信息
      String wifiNeedCheck = "";
      logMap.put("isLocationRange", isLocationRange);
      kqLog.info("GetOutButtonsCmd:"+user.getLastname()+":isLocationRange:"+isLocationRange+":locationMap:"+ JSONObject.toJSONString(locationMap));
      if(!isLocationRange){
        if("wifi".equalsIgnoreCase(networkType)){
          Map<String,Object> wifiMap = kqGroupBiz.checkWifiScope(userId, sid, mac);
          logMap.put("wifiMap", wifiMap);
          wifiNeedCheck = Util.null2String(wifiMap.get("needCheck"));
          boolean wifiInScope = Boolean.parseBoolean(Util.null2String(wifiMap.get("inScope")));
          if("1".equalsIgnoreCase(wifiNeedCheck)){
            if(wifiInScope){
              Map<String,Object> wifiInfo = (Map<String, Object>) wifiMap.get("wifiInfo");
              isWifiRange = true;
              rangename = Util.null2String(wifiInfo.get("wifiname"));
              rangekey = "wifi";
            }else{
              kqLog.info(user.getLastname()+":wifi不在考勤范围内:GetOutButtonsCmd:userId:"+userId+":sid:"+sid+":mac:"+mac);
            }
          }
        }
      }

      isRange = isLocationRange || isWifiRange;
      if("1".equalsIgnoreCase(locationNeedCheck) && !isLocationRange){
        retmap.put("failkey", "position");
      }else if("1".equalsIgnoreCase(wifiNeedCheck) && !isWifiRange){
        retmap.put("failkey", "wifi");
      }

      retmap.put("outsidesign", outsidesign);
      retmap.put("isrange", isRange ? "1" : "0");
      retmap.put("rangekey", rangekey);
      retmap.put("locationid", rangeid);
      retmap.put("rangename", rangename);
			retmap.put("status", "1");

		}catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
    logMap.put("retmap", retmap);
      kqLog.info(user.getLastname()+":GetOutButtonsCmd:retmap:"+retmap);
    KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "outButtons");
		return retmap;
	}

  @Override
	public BizLogContext getLogContext() {
		return null;
	}

}
