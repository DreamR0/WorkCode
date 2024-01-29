package com.engine.meeting.cmd.remindertemplate;

import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.*;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.biz.HrmSanyuanAdminBiz;
import com.engine.meeting.util.MeetingNoRightUtil;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DoEditRemindCmd extends AbstractCommonCommand<Map<String, Object>> {

	private String id;
	private String titlemsg;
	private SimpleBizLogger logger;
	private BizLogContext bizLogContext;

	public DoEditRemindCmd(User user, Map<String, Object> params){
		this.user = user;
        this.params = params;
		this.id = params.get("id").toString();
		this.titlemsg = Util.null2String(params.get("titlemsg"));
		this.logger = new SimpleBizLogger();
		this.bizLogContext=new BizLogContext();
	}

	@Override
	public BizLogContext getLogContext() {

		return logger.getBizLogContext();
	}

	/**
	 * 编辑日志的构建
	 */
	public void boforeLog(){
		bizLogContext.setDateObject(new Date());
		bizLogContext.setUserid(user.getUID());
		bizLogContext.setTargetId(Util.null2String(params.get("id")));
		bizLogContext.setUsertype(Util.getIntValue(user.getLogintype()));
		bizLogContext.setTargetName(this.titlemsg);
		bizLogContext.setLogType(BizLogType.MEETING_ENGINE);
		bizLogContext.setBelongType(BizLogSmallType4Meeting.MEETING_ENGINE_REMINDER);
		bizLogContext.setLogSmallType(BizLogSmallType4Meeting.MEETING_ENGINE_REMINDER);
		int type=Util.getIntValue(Util.null2String(params.get("type")));
		String remindType = "";
		if(type==2){
			remindType = SystemEnv.getHtmlLabelName(17586,user.getLanguage());
		}
		if(type==3){
			remindType = SystemEnv.getHtmlLabelName(18845,user.getLanguage());
		}
		if(type==5){
			remindType = SystemEnv.getHtmlLabelName(126016,user.getLanguage());
		}
		if(type==6){
			remindType = SystemEnv.getHtmlLabelName(23042,user.getLanguage());
		}
		bizLogContext.setBelongTypeTargetName(remindType);
		bizLogContext.setParams(params);
		bizLogContext.setClientIp(Util.null2String(params.get(ParamConstant.PARAM_IP)));
		logger.setUser(user);//当前操作人
		logger.setParams(params);//request请求参数
		String mainSql = "select * from meeting_remind_template where id="+bizLogContext.getTargetId();
		logger.setMainSql(mainSql, "id");
		logger.setMainTargetNameColumn("title");
		logger.before(bizLogContext);
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		this.boforeLog();
		Map map=new HashMap();
		if(!HrmUserVarify.checkUserRight("Meeting:Remind",user)) {
			return MeetingNoRightUtil.getNoRightMap();
		}
		int id=Util.getIntValue(params.get("id").toString());
		String titlemsg=Util.null2String(params.get("titlemsg"));
		String desc_n=Util.null2String(params.get("desc_n"));
		String bodymsg=Util.null2String(params.get("bodymsg"));
		bodymsg = bodymsg.replaceAll("\n","<br />");
		ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
		SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
		CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
		boolean mtidetachable = manageDetachComInfo.isUseMtiManageDetach();
		int subid = Util.getIntValue(Util.null2String(params.get("subcompanyid")),0);
		if(mtidetachable){
			if(subid < 0){
				subid = 0;
			}
		}else{
			subid =0;
		}

		ArrayList subcompanylist = null;
		try {
			String subStr = subCompanyComInfo.getRightSubCompany(user.getUID(), "Meeting:Remind",-1);
			subcompanylist = Util.TokenizerString(subStr,",");
		} catch (Exception e) {
			e.printStackTrace();
		}
		//当开启分权,并且未设置机构权限，进入直接返回无权限。
		if (mtidetachable && subcompanylist.size() < 1) {
			return MeetingNoRightUtil.getNoRightMap();
		}
		if( mtidetachable && subid > 0){
			int operatelevel= checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"Meeting:Remind",subid);
			if(operatelevel < 1){
				return MeetingNoRightUtil.getNoRightMap();
			}

		}else if(mtidetachable && subid < 1){
			if(!HrmSanyuanAdminBiz.hasRight(user)){
				return MeetingNoRightUtil.getNoRightMap();
			}
		}
		RecordSet recordSet = new RecordSet();

		bizLogContext.setTargetId(id+"");
		bizLogContext.setOperateType(BizLogOperateType.UPDATE);

		recordSet.executeUpdate("update meeting_remind_template set title='"+titlemsg+"',body='"+bodymsg+"',desc_n='"+desc_n+"' where id= ? and subcompanyid = ?",id, subid);
		map.put("ret", "true");
		return map;
	}

}
