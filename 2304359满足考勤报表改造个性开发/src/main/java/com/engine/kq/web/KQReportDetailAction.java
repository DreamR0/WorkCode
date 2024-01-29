package com.engine.kq.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.engine.common.util.ParamUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.kq.biz.KQReportFieldComInfo;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.service.KQReportDetailService;
import com.engine.kq.service.impl.KQReportDetailServiceImpl;
import com.engine.kq.util.TransMethod;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * 考勤报表
 */
public class KQReportDetailAction extends BaseBean {

	private KQReportDetailService getService(User user) {
		return (KQReportDetailService) ServiceUtil.getService(KQReportDetailServiceImpl.class, user);
	}

	/**
	 * 获取考勤报表明细Tabs
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/getTabs")
	@Produces(MediaType.TEXT_PLAIN)
	public String getTabs(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		Map<String, Object> apidatas = new HashMap<String, Object>();
		try {
			User user = HrmUserVarify.getUser(request, response);
			apidatas = getService(user).getTabs(ParamUtil.request2Map(request), user);
		} catch (Exception e) {
			apidatas.put("status", "-1");
			writeLog(e);
		}
		return JSONObject.toJSONString(apidatas);
	}

	/**
	 * 获取考勤报表明细
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/getKQReportDetail")
	@Produces(MediaType.TEXT_PLAIN)
	public String getKQReportDetail(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		Map<String, Object> apidatas = new HashMap<String, Object>();
		Map<String,Object> params =ParamUtil.request2Map(request) ;
		params.put("isNoAccount","1") ;
		RecordSet rs = new RecordSet();
		String sql = "";
		try {
			User user = HrmUserVarify.getUser(request, response);
			String type = Util.null2String(request.getParameter("type"));
			String reportType = Util.null2String(request.getParameter("reportType"));

			if(KQReportFieldComInfo.cascadekey2fieldname.keySet().contains(type)){
				type=KQReportFieldComInfo.cascadekey2fieldname.get(type);
			}
			if(reportType.equals("month")) {
				sql = "select formula from kq_report_field where fieldname = ? ";
				rs.executeQuery(sql, type);
				if(rs.next()){
					String formula = Util.null2String(rs.getString("formula"));
					if(formula.indexOf("beLateMins")>-1){
						type = "beLate";
						params.put("type",type);
					}else if(formula.indexOf("leaveEarlyMins")>-1){
						type = "leaveEearly";
						params.put("type",type);
					}
					params.put("formula",formula);
				}
			}

			if(type.equals("workdays")||type.equals("workmins")){
				apidatas = getService(user).getWorkDayInfo(params, user);
			}else if(type.equals("attendancedays")||type.equals("attendanceMins")||
							  type.equals("signdays")||type.equals("signmins")){
				apidatas = getService(user).getSignInfo(params, user);
			}else if(type.equals("beLate")||type.equals("beLateMins")||
							type.equals("graveBeLate")||type.equals("graveBeLateMins")){
				apidatas = getService(user).getBeLateInfo(params, user);
			}else if(type.equals("leaveEearly")||type.equals("leaveEarlyMins")||
							type.equals("graveLeaveEarly")||type.equals("graveLeaveEarlyMins")){
				apidatas = getService(user).getLeaveEearlyInfo(params, user);
			}else if(type.equals("absenteeism")||type.equals("absenteeismMins")){
				apidatas = getService(user).getAbsenteeismInfo(params, user);
			}else if(type.equals("forgotCheck")){
				apidatas = getService(user).getForgotCheckInfo(params, user);
			}else if(type.equals("leave")||type.startsWith("leaveType_")||type.equals("overtimeTotal")||
							type.equals("businessLeave")||type.equals("officialBusiness")||
							type.equals("leaveDeduction")){
				apidatas = getService(user).getLeaveInfo(params, user);
			}
		} catch (Exception e) {
			apidatas.put("status", "-1");
			writeLog(e);
		}
		return JSONObject.toJSONString(apidatas);
	}

	/**
	 * 获取考勤报表明细信息
	 * @param request
	 * @param response
	 * @return
	 */
	@POST
	@Path("/getDailyDetialInfo")
	@Produces(MediaType.TEXT_PLAIN)
	public String getDailyDetialInfo(@Context HttpServletRequest request, @Context HttpServletResponse response) {
		Map<String, Object> apidatas = new HashMap<String, Object>();
		try {
			User user = HrmUserVarify.getUser(request, response);
			apidatas = getService(user).getDailyDetialInfo(ParamUtil.request2Map(request), user);
		} catch (Exception e) {
			apidatas.put("status", "-1");
			writeLog(e);
		}
		return JSONObject.toJSONString(apidatas);
	}
}
