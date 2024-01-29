package com.engine.kq.cmd.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.Util_DataMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.util.UtilKQ;
import weaver.common.DateUtil;
import weaver.conn.BatchRecordSet;
import weaver.conn.RecordSet;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.*;

public class FormatReportCmd extends AbstractCommonCommand<Map<String, Object>> {

	public FormatReportCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		BatchRecordSet bRs = new BatchRecordSet();
		RecordSet rs = new RecordSet();
		String sql = "";
		String delSql = "";
		try{
			String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"a");
			JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
			String fromDate = Util.null2String(jsonObj.get("fromDate"));
			String toDate = Util.null2String(jsonObj.get("toDate"));
			String typeselect =Util.null2String(jsonObj.get("typeselect"));
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

			String today = DateUtil.getCurrentDate();
			if(DateUtil.compDate(today, fromDate)>0){//开始如期不大于今天
				fromDate = today;
			}
			if(DateUtil.compDate(today, toDate)>0){//结束如期不大于今天
				toDate = today;
			}
			List<String> lsDate = new ArrayList<>();
			Calendar cal = DateUtil.getCalendar();

			if (DateUtil.timeInterval(toDate, fromDate) > 0)
			{
				retmap.put("status", "-1");
				retmap.put("message", ""+ SystemEnv.getHtmlLabelName(10005334,weaver.general.ThreadVarLanguage.getLang())+"");
				writeLog("考勤格式开始时间不能大于结束时间 fromDate==" + fromDate + "toDate==" + toDate);
				return retmap;
			}

//			String kqformat_error = Util.null2String(Util_DataMap.getVal("kqformat_error"));
//			if(kqformat_error.length()>0) {
//				retmap.put("status", "-1");
//				retmap.put("message", SystemEnv.getHtmlLabelNames(kqformat_error, user.getLanguage()));
//				Util_DataMap.clearVal("kqformat_error");
//				return retmap;
//			}

			boolean isEnd = false;
			for(String date=fromDate; !isEnd;) {
				if(date.equals(toDate)) isEnd = true;
				lsDate.add(date);
				cal.setTime(DateUtil.parseToDate(date));
				date = DateUtil.getDate(cal.getTime(), 1);
			}

			String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));
			String departmentId = Util.null2String(jsonObj.get("departmentId"));
			String resourceId = Util.null2String(jsonObj.get("resourceId"));
			String allLevel = Util.null2String(jsonObj.get("allLevel"));
			String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));
			String viewScope = Util.null2String(jsonObj.get("viewScope"));


			List<List<Object>> paramInsert = new ArrayList<>();
			List<Object> params = null;

			Map paramsObj = new HashMap();
			paramsObj.put("isNoAccount",isNoAccount);
			sql = " select distinct a.id from hrmresource a , ("+new KQGroupBiz().getGroupMemberSql(paramsObj)+") b where a.id=b.resourceid "+rightSql;
			String sqlWhere = " ";
			if(subCompanyId.length()>0){
				sqlWhere +=" and a.subcompanyid1 in("+subCompanyId+") ";
			}

			if(departmentId.length()>0){
				sqlWhere +=" and a.departmentid in("+departmentId+") ";
			}

			if(resourceId.length()>0){
				sqlWhere +=" and a.id in("+resourceId+") ";
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

//			if(rs.getDBType().equals("sqlserver")||rs.getDBType().equals("mysql")){
//				delSql = "delete t1  from kq_format_detail t1 inner join hrmresource a on t1.resourceid = a.id where t1.kqdate>=? and t1.kqdate<=? "+sqlWhere;
//				rs.executeUpdate(delSql,fromDate,toDate);
//				delSql = "delete t1  from kq_format_total t1 inner join hrmresource a on t1.resourceid = a.id where t1.kqdate>=? and t1.kqdate<=? "+sqlWhere;
//				rs.executeUpdate(delSql,fromDate,toDate);
//			}else{
//				delSql = "delete kq_format_detail t1 where exists ( select 1 from hrmresource a where t1.resourceid = a.id "+sqlWhere+") and t1.kqdate>=? and t1.kqdate<=? ";
//				rs.executeUpdate(delSql,fromDate,toDate);
//				delSql = "delete kq_format_total t1 where exists ( select 1 from hrmresource a where t1.resourceid = a.id "+sqlWhere+") and t1.kqdate>=? and t1.kqdate<=? ";
//				rs.executeUpdate(delSql,fromDate,toDate);
//			}

			sql = sql +sqlWhere;
			rs.executeQuery(sql);
			while(rs.next()){
				String resourceid = rs.getString("id");
				for(int i=0;i<lsDate.size();i++) {
					params = new ArrayList<>();
					params.add(resourceid);
					params.add(lsDate.get(i));
					paramInsert.add(params);
				}
			}
			new KQFormatBiz().format(paramInsert);
			retmap.put("status", "1");
			retmap.put("message", 	SystemEnv.getHtmlLabelName(30700, user.getLanguage()));
			return retmap;
		}catch (Exception e){
			writeLog(e);
		}
		return retmap;
	}


	@Override
	public BizLogContext getLogContext() {
		return null;
	}
}
