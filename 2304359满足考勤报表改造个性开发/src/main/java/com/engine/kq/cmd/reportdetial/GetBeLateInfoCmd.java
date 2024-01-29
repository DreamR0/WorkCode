package com.engine.kq.cmd.reportdetial;

import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQReportBiz;
import com.engine.kq.enums.FlowReportTypeEnum;
import com.engine.kq.enums.ReportColumnEnum;
import com.engine.kq.util.PageUidFactory;
import com.engine.kq.util.TransMethod;
import weaver.conn.RecordSet;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * 迟到相关信息
 */
public class GetBeLateInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetBeLateInfoCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		//qc2304359
		TransMethod.setIsHave(true);
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{
			String dialogTitle = SystemEnv.getHtmlLabelName(20088, user.getLanguage());
			String tabKey = Util.null2String(params.get("tabKey"));
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
			String formula = Util.null2String(params.get("formula"));

			String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid, serialid as serialid1," +
							" workbegintime,workendtime, signintime,signouttime, beLateMins, graveBeLateMins ";
			String sqlFrom  = "from hrmresource a, kq_format_detail b  ";
			String sqlWhere = " where a.id = b.resourceid";
			String orderby = " kqdate asc, workbegintime asc, a.id asc " ;
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

			if(tabKey.equals("2")){
				sqlWhere += " and graveBeLateMins>0 ";
			}else{
				sqlWhere += " and beLateMins>0 ";
			}

			if(formula.length()>0){
				formula = formula.replace("${beLateMins}>","").replace("${beLateMins}<=","").replace("?1:0","").replace(" && ","-");
				String[] tmpValue = Util.splitString(formula,"-");
				sqlWhere += " and beLateMins>"+tmpValue[0]+" and beLateMins<= "+tmpValue[1];
			}

			String pageUid = PageUidFactory.getHrmPageUid("KQReportDetialList");

			tableString=""+
							"<table pageUid=\""+pageUid+"\" pagesize=\"10\" tabletype=\"none\">"+
							"<sql backfields=\""+backFields+"\" sqlform=\""+Util.toHtmlForSplitPage(sqlFrom)+"\" sqlprimarykey=\"b.id\" sqlorderby=\""+orderby+"\" sqlsortway=\"asc\" sqldistinct=\"true\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"/>"+
							"<head>"+
							"				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(413,user.getLanguage())+"\" column=\"lastname\" orderkey=\"lastname\"/>"+
							"				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(714,user.getLanguage())+"\" column=\"workcode\" orderkey=\"workcode\"/>"+
							"				<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(124,user.getLanguage())+"\" column=\"departmentid\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>"+
							"				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(602,user.getLanguage())+"\" column=\"status\" orderkey=\"status\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\""+user.getLanguage()+"\"/>"+
							"				<col width=\"15%\" text=\""+SystemEnv.getHtmlLabelName(97,user.getLanguage())+"\" column=\"kqdate\" orderkey=\"kqdate\"/>"+
							"				<col width=\"20%\" text=\""+SystemEnv.getHtmlLabelName(390054,user.getLanguage())+"\" column=\"serialid\" orderkey=\"serialid\" transmethod=\"com.engine.kq.util.TransMethod.getSerailName\" otherpara=\"column:workbegintime+column:workendtime\"/>"+
							"				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(18949,user.getLanguage())+"\" column=\"serialid1\" orderkey=\"serialid1\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialSignTime\" otherpara=\"column:signintime++column:kqdate+column:resourceid+"+user.getLanguage()+"\"/>";
							if(tabKey.equals("2")){
								tableString += "				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(391413,user.getLanguage())+"\" column=\"graveBeLateMins\" orderkey=\"graveBeLateMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />";
							}else{
								tableString += "				<col width=\"10%\" text=\""+SystemEnv.getHtmlLabelName(391413,user.getLanguage())+"\" column=\"beLateMins\" orderkey=\"beLateMins\" transmethod=\"com.engine.kq.util.TransMethod.getReportDetialMinToHour\" />";
							}
			tableString += "</head>"+
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
