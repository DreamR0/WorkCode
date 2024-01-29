package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQCardLogBiz;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQSettingsBiz;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.google.common.collect.Maps;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.engine.portal.biz.constants.ModuleConstants;
import com.engine.portal.biz.nonstandardfunction.SysModuleInfoBiz;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

/**
 * 获取考勤组的基本信息的
 */
public class GetButtonBaseInfoCmd extends AbstractCommonCommand<Map<String, Object>> {
  public KQLog kqLog = new KQLog();
  private DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
  private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
  private Map<String,Object> logMap = Maps.newHashMap();
  private Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();

	public GetButtonBaseInfoCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
    kqLog.info(user.getLastname()+":GetButtonBaseInfoCmd:params:"+params);

    //==zj  判断主账号有没有考勤组
      KqUtil kqUtil = new KqUtil();
      String userId = Util.null2String(user.getUID());  //获取主账号userid
      String today = DateUtil.getCurrentDate();         //获取当前日期

      Boolean flag = kqUtil.mainAccountCheck(userId, today);
      new BaseBean().writeLog("==zj==(获取打卡界面)" + flag);
      if (!flag){
        //如果主账号没有考勤组，再看子账号是否有考勤组
        List<Map> list = kqUtil.childAccountCheck(userId, today);
        new BaseBean().writeLog("==zj==(获取打卡界面-maps)" + JSON.toJSONString(list));
        if (list.size()  == 1){
          //如果只有一个生效考勤组
          Map<String,String> map = list.get(0);
          int userid = Integer.parseInt(map.get("userid"));
          User user = new User(userid);
          this.user = user;
        }else if (list.size() > 1){
          //如果有多个生效考勤组
          Map<String, Object> retmap = new HashMap<String, Object>();
          RecordSet rs = new RecordSet();
          String tips = "";
          String sql = "select * from "+new BaseBean().getPropValue("qc2280379","tipsTableName")+" where "+new BaseBean().getPropValue("qc2280379","tipsFrom")+"= '考勤打卡'";
          new BaseBean().writeLog("==zj==(考勤打卡提示信息)" + sql);
          rs.executeQuery(sql);
          if (rs.next()){
            tips = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2280379","tipsMessage")));
          }
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          retmap.put("userid", user.getUID());
          retmap.put("message",tips);
          return retmap;
        }
      }



      logMap.put("lastname", user.getLastname());
    logMap.put("params", params);
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{

      KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
      KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();

      String ismobile = Util.null2String(params.get("ismobile"));

      String lastname = user.getLastname();
      String messagerurl = resourceComInfo.getMessagerUrls(""+user.getUID());
      String shortname = "";

      boolean USERICONLASTNAME = Util.null2String(new BaseBean().getPropValue("Others" , "USERICONLASTNAME")).equals("1");
      if(USERICONLASTNAME&&(messagerurl.indexOf("icon_w_wev8.jpg")>-1||messagerurl.indexOf("icon_m_wev8.jpg")>-1||messagerurl.indexOf("dummyContact.png")>-1)){
        shortname = User.getLastname(Util.null2String(Util.formatMultiLang(lastname, ""+user.getLanguage())));
      }

      String curDate = DateUtil.getCurrentDate();
      LocalDateTime now = LocalDateTime.now();
      String now_date = now.format(dateFormatter);
      String now_time = now.format(fullTimeFormatter);
      GetButtonsCmd getButtonsCmd = new GetButtonsCmd(params, user);
      LocalDateTime now_zone = getButtonsCmd.getZoneOfClientDateTime(now_date,now_time,logMap);
      if(now_zone != null){
        curDate = now_zone.format(dateFormatter);
      }
      KQWorkTime kqWorkTime = new KQWorkTime();
      WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(user.getUID()+"", curDate);
      String userinfo = "#userid#"+user.getUID()+"#getUserSubCompany1#"+user.getUserSubCompany1()+"#getUserSubCompany1#"+user.getUserDepartment()
          +"#getJobtitle#"+user.getJobtitle();
      workTimeEntityLogMap.put("resourceid", userinfo);
      workTimeEntityLogMap.put("splitDate", curDate);
      workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);

      String groupid = workTimeEntity.getGroupId();
      logMap.put("groupid", groupid);
      String groupname = "";
      String locationshowaddress = Util.null2String(kqGroupComInfo.getLocationshowaddress(groupid));//是否记录统一地址
      String locationfacecheck = Util.null2String(kqGroupComInfo.getLocationfacecheck(groupid));//办公地点启用人脸识别拍照打卡
      String locationfacechecktype = Util.null2String(kqGroupComInfo.getLocationfacechecktype(groupid));//办公地点启用人脸识别拍照打卡方式
      String wififacecheck = Util.null2String(kqGroupComInfo.getWififacecheck(groupid));//wifi启用人脸识别拍照打卡
      String wififacechecktype = Util.null2String(kqGroupComInfo.getWififacechecktype(groupid));//wifi启用人脸识别拍照打卡方式
      logMap.put("locationshowaddress", locationshowaddress);
      logMap.put("locationfacecheck", locationfacecheck);
      logMap.put("locationfacechecktype", locationfacechecktype);
      logMap.put("wififacecheck", wififacecheck);
      logMap.put("wififacechecktype", wififacechecktype);

      boolean isAdmin = user.isAdmin();
      logMap.put("isAdmin", isAdmin);
      if(isAdmin){
        retmap.put("showbutton", "0");
        retmap.put("status", "1");
        retmap.put("userid", user.getUID());
        KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttonBaseInfo");
        return retmap;
      }
//   * 1、PC和移动端均可打卡
//          * 2、仅PC可打卡
//          * 3、仅移动端可打卡
//          * 4、无需打卡
      String groupSignType = getSignType();
      logMap.put("groupSignType", groupSignType);
      //无需打卡
      if("4".equalsIgnoreCase(groupSignType)){
        retmap.put("showbutton", "0");
        retmap.put("status", "1");
        retmap.put("userid", user.getUID());
        KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttonBaseInfo");
        return retmap;
      }
      if("2".equalsIgnoreCase(groupSignType)){
        if("1".equalsIgnoreCase(ismobile)){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          retmap.put("userid", user.getUID());
          KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttonBaseInfo");
          return retmap;
        }
      }
      if("3".equalsIgnoreCase(groupSignType)){
        if(!"1".equalsIgnoreCase(ismobile)){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          retmap.put("userid", user.getUID());
          KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttonBaseInfo");
          return retmap;
        }
      }

      if(groupid.length() > 0){
        groupname = SystemEnv.getHtmlLabelName(10000801, Util.getIntValue(user.getLanguage()))+kqGroupComInfo.getGroupname(groupid);
      }else{
        groupname = SystemEnv.getHtmlLabelName(10000799, Util.getIntValue(user.getLanguage()));
		    retmap.put("showbutton", "0");
      }

      retmap.put("hasCrmModule", SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Crm));
      
      retmap.put("userid", user.getUID());
      retmap.put("lastname", lastname);
      retmap.put("shortname", shortname);
      retmap.put("messagerurl", messagerurl);
      retmap.put("groupname", groupname);
      retmap.put("locationshowaddress", locationshowaddress);
      retmap.put("locationfacecheck", locationfacecheck);
      retmap.put("locationfacechecktype", locationfacechecktype);
      retmap.put("wififacecheck", wififacecheck);
      retmap.put("wififacechecktype", wififacechecktype);
      retmap.put("date", curDate);
      retmap.put("timemillis", System.currentTimeMillis());
      retmap.put("wificheck",Util.null2String(kqGroupComInfo.getWificheck(groupid)).equals("1"));
      retmap.put("locationcheck",Util.null2String(kqGroupComInfo.getLocationcheck(groupid)).equals("1"));
      retmap.put("locationcheckscope",Util.null2String(kqGroupComInfo.getLocationcheckscope(groupid)));

      String user_last_map = get_user_last_map(user.getUID()+"");
      logMap.put("user_last_map", user_last_map);
      if(user_last_map.length() > 0){
        retmap.put("user_last_map", user_last_map);
      }

      retmap.put("isFirstLocation", KQSettingsBiz.isFirstLocation()?"1":"0");
			retmap.put("status", "1");
		}catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
    kqLog.info(user.getLastname()+":GetButtonBaseInfoCmd:retmap:"+retmap);
    logMap.put("result", retmap);
    KQCardLogBiz.logCardInfo(user.getUID()+"", logMap, workTimeEntityLogMap, "buttonBaseInfo");
		return retmap;
	}

  /**
   * 获取当前用户选择的地图
   * @param userid
   */
  public static String get_user_last_map(String userid) {
    RecordSet rs = new RecordSet();
    String sql = "select * from user_last_map where userid = ? ";
    rs.executeQuery(sql, userid);
    if(rs.next()){
      return rs.getString("last_map");
    }
    return "";
  }

  /**
   * 获取考勤组的考勤方式
   * 1、PC和移动端均可打卡
   * 2、仅PC可打卡
   * 3、仅移动端可打卡
   */
  private String getSignType() {
    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    KQGroupEntity kqGroupEntity  = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
    if(kqGroupEntity == null){
      return "";
    }
    return kqGroupEntity.getSignintype();
  }

  @Override
	public BizLogContext getLogContext() {
		return null;
	}

}
