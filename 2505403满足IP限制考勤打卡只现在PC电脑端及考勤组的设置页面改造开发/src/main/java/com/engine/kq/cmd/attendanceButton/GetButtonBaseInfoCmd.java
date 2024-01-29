package com.engine.kq.cmd.attendanceButton;

import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQGroupComInfo;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.entity.KQGroupEntity;
import java.util.HashMap;
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

	public GetButtonBaseInfoCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
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
      String groupid = kqGroupMemberComInfo.getKQGroupId(user.getUID()+"",curDate);
      String groupname = "";
      String locationshowaddress = Util.null2String(kqGroupComInfo.getLocationshowaddress(groupid));//是否记录统一地址
      String locationfacecheck = Util.null2String(kqGroupComInfo.getLocationfacecheck(groupid));//办公地点启用人脸识别拍照打卡
      String wififacecheck = Util.null2String(kqGroupComInfo.getWififacecheck(groupid));//wifi启用人脸识别拍照打卡

      boolean isAdmin = user.isAdmin();
      if(isAdmin){
        retmap.put("showbutton", "0");
        retmap.put("status", "1");
        retmap.put("userid", user.getUID());
        return retmap;
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
        retmap.put("userid", user.getUID());
        return retmap;
      }
      if("2".equalsIgnoreCase(groupSignType)){
        if("1".equalsIgnoreCase(ismobile)){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          retmap.put("userid", user.getUID());
          return retmap;
        }
      }
      if("3".equalsIgnoreCase(groupSignType)){
        if(!"1".equalsIgnoreCase(ismobile)){
          retmap.put("showbutton", "0");
          retmap.put("status", "1");
          retmap.put("userid", user.getUID());
          return retmap;
        }
      }

      if(groupid.length() > 0){
        groupname = SystemEnv.getHtmlLabelNames("524,129047,390221,18435", user.getLanguage())+kqGroupComInfo.getGroupname(groupid);
      }else{
        groupname = SystemEnv.getHtmlLabelNames("390221,18435,390220", user.getLanguage());
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
      retmap.put("wififacecheck", wififacecheck);
      retmap.put("date", curDate);
      retmap.put("timemillis", System.currentTimeMillis());

			retmap.put("status", "1");
		}catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
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
