package com.engine.kq.cmd.reportdetial;

import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.HrmUtil;
import com.engine.kq.biz.KQReportBiz;
import com.engine.kq.util.PageUidFactory;
import weaver.conn.RecordSet;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.Map;

/***
 * 应出勤明细
 */
public class GetSignInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetSignInfoCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{
			String dialogTitle = SystemEnv.getHtmlLabelName(391409,user.getLanguage());
			String resourceId = Util.null2String(params.get("resourceId"));
			String keyWord = Util.null2String(params.get("keyWord"));
			String fromDate = Util.null2String(params.get("fromDate"));
			String toDate = Util.null2String(params.get("toDate"));
			String typeselect =Util.null2String(params.get("typeselect"));
			if(typeselect.length()==0)typeselect = "3";
			if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
				if(typeselect.equals("1")){
					fromDate = TimeUtil.getCurrentDateString();
					toDate = TimeUtil.getCurrentDateString();
				}else{
					fromDate = TimeUtil.getDateByOption(typeselect,"0");
					toDate = TimeUtil.getDateByOption(typeselect,"1");
				}
			}
			String viewScope = Util.null2String(params.get("viewScope"));
			String subCompanyId = Util.null2String(params.get("subCompanyId"));
			String departmentId = Util.null2String(params.get("departmentId"));
			String allLevel = Util.null2String(params.get("allLevel"));
			String isNoAccount = Util.null2String(params.get("isNoAccount"));
			String type = Util.null2String(params.get("type"));
			//人员状态
			String resourceStatus = Util.null2String(params.get("status"));

			String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid,serialid as serialid1, serialid as serialid2," +
													" workbegintime,workendtime,signintime,signouttime, attendanceMins, signMins ";
			String sqlFrom  = "from hrmresource a, kq_format_detail b  ";
			String sqlWhere = " where a.id = b.resourceid and (attendanceMins>0 or signMins>0)";
			String orderby = " kqdate asc, workbegintime asc " ;
			String tableString = "";

			String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"a");
			if(rightSql.length()>0){
				sqlWhere += rightSql;
			}

			if (keyWord.length() > 0){
				sqlWhere += " and lastname = "+keyWord;
			}

			if (fromDate.length() > 0){
				sqlWhere += " and kqdate >= '"+fromDate+"'";
			}

			if (toDate.length() > 0){
				sqlWhere += " and kqdate <= '"+toDate+"'";
			}

			if(subCompanyId.length()>0){
				sqlWhere +=" and a.subcompanyid1 in("+subCompanyId+") ";
			}

			if(departmentId.length()>0){
				sqlWhere +=" and a.departmentid in("+departmentId+") ";
			}

			if (resourceId.length() > 0){
				sqlWhere += " and b.resourceid in ( "+resourceId+")";
			}

			if(resourceStatus.length()>0){
				if (!resourceStatus.equals("8") && !resourceStatus.equals("9")) {
					sqlWhere += " and a.status = "+resourceStatus+ "";
				}else if (resourceStatus.equals("8")) {
					sqlWhere += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
				}
			}

			if(viewScope.equals("4")){//我的下属
				if(allLevel.equals("1")){//所有下属
					sqlWhere+=" and a.managerstr like '%,"+user.getUID()+",%'";
				}else{
					sqlWhere+=" and a.managerid="+user.getUID();//直接下属
				}
			}
			if (!"1".equals(isNoAccount)) {
				sqlWhere += " and a.loginid is not null "+(rs.getDBType().equals("oracle")?"":" and a.loginid<>'' ");
			}

			if (type.equals("signdays")||type.equals("signmins")) {
				sqlWhere += " and signmins >0 ";
			}

			// #1475814-概述：满足考勤报分部部门显示及导出时显示全路径需求
			String transMethodString = HrmUtil.getKqDepartmentTransMethod();

			String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");

			tableString=""+
							"<table pageUid=\""+pageUid+"\" pagesize=\"10\" tabletype=\"none\">"+
							"<sql backfields=\""+backFields+"\" sqlform=\""+Util.toHtmlForSplitPage(sqlFrom)+"\" sqlprimarykey=\"b.id\" sqlorderby=\""+orderby+"\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"/>"+
							"<head>"+
							"				<col width=\"8%\" text=\""+SystemEnv.getHtmlLabelName(413,user.getLanguage())+"\" column=\"lastname\" orderkey=\"lastname\"/>"+
							"				<col width=\"8%\" text=\""+SystemEnv.getHtmlLabelName(714,user.getLanguage())+"\" column=\"workcode\" orderkey=\"workcode\"/>"+
							"				<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(124,user.getLanguage())+"\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\""+transMethodString+"\"/>"+
							"				<col width=\"9%\" text=\""+SystemEnv.getHtmlLabelName(602,user.getLanguage())+"\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\""+user.getLanguage()+"\"/>"+
							"				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(97,user.getLanguage())+"\" column=\"kqdate\" orderkey=\"kqdate\"/>"+
							"				<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(390054,user.getLanguage())+"\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>"+
							"				<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(390495,user.getLanguage())+"\" column=\"serialid1\" orderkey=\"signintime\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignInTime\" otherpara=\"column:signintime+column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>"+
							"				<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(390496,user.getLanguage())+"\" column=\"serialid2\" orderkey=\"signouttime\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignOutTime\" otherpara=\"column:signouttime+column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>"+
							/*"				<col width=\"9%\" text=\""+SystemEnv.getHtmlLabelName(391040,user.getLanguage())+"\" column=\"attendanceMins\" orderkey=\"attendanceMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />"+*/
							"				<col width=\"9%\" text=\""+SystemEnv.getHtmlLabelName(504433,user.getLanguage())+"\" column=\"signMins\" orderkey=\"signMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />"+
							"</head>"+
							"</table>";

			//主要用于 显示定制列以及 表格 每页展示记录数选择
			String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
			Util_TableMap.setVal(sessionkey, tableString);

			retmap.put("dialogTitle",dialogTitle);
			retmap.put("sessionkey", sessionkey);
			retmap.put("status", "1");
		}catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

}
