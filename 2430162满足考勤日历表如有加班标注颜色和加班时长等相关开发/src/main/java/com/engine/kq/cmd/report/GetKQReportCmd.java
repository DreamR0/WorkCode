package com.engine.kq.cmd.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.customization.qc2238872.kqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.util.PageUidFactory;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.math.BigDecimal;
import java.util.*;

public class GetKQReportCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetKQReportCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		new BaseBean().writeLog("==zj==(考勤汇总报表获取进入)");
		try{
			String pageUid = PageUidFactory.getHrmPageUid("KQReport");

			SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
		  DepartmentComInfo departmentComInfo = new DepartmentComInfo();
			ResourceComInfo resourceComInfo = new ResourceComInfo();
			JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
			KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
			KQReportBiz kqReportBiz = new KQReportBiz();

			JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
			String attendanceSerial = Util.null2String(jsonObj.get("attendanceSerial"));
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
			//人员状态
			String status = Util.null2String(jsonObj.get("status"));
			String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));
			String departmentId = Util.null2String(jsonObj.get("departmentId"));
			String resourceId = Util.null2String(jsonObj.get("resourceId"));
			String allLevel = Util.null2String(jsonObj.get("allLevel"));
			String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));
			String viewScope = Util.null2String(jsonObj.get("viewScope"));
			String isFromMyAttendance = Util.null2String(jsonObj.get("isFromMyAttendance"));//是否是来自我的考勤的请求，如果是，不加载考勤报表权限共享的限制，不然我的考勤会提示无权限
			Boolean isMyAttendance = false;
			if ("1".equals(isFromMyAttendance)){
				isMyAttendance = true;
			}
			int pageIndex = Util.getIntValue(Util.null2String(jsonObj.get("pageIndex")), 1);
			int pageSize =  KQReportBiz.getPageSize(Util.null2String(jsonObj.get("pageSize")),pageUid,user.getUID());
			int count = 0;
			int pageCount = 0;
			int isHavePre = 0;
			int isHaveNext = 0;


			String rightSql = kqReportBiz.getReportRight("1",""+user.getUID(),"a");
			if(isFromMyAttendance.equals("1")){
				rightSql = "";
			}

			List<Map<String, Object>> leaveRules = kqLeaveRulesBiz.getAllLeaveRules();
			List<Object> columns = new ArrayList();
			Map<String,Object> column = null;
			List<Object> datas = new ArrayList();
			Map<String,Object> data = null;
			Map<String,Object> mapChildColumnInfo = null;
			List<Object> childColumns = null;
			KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();

			new BaseBean().writeLog("==zj==(获取考勤汇总报表字段集合)" + JSON.toJSONString(kqReportFieldComInfo));
			while (kqReportFieldComInfo.next()){
				if(Util.null2String(kqReportFieldComInfo.getParentid()).length()>0)continue;
				if(kqReportFieldComInfo.getFieldname().equals("kqCalendar"))continue;
				if(KQReportFieldComInfo.cascadekey2fieldname.keySet().contains(kqReportFieldComInfo.getFieldname()))continue;
				if(!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("month"))continue;
				if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0)continue;

				column = new HashMap();
				column.put("title", SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(), user.getLanguage()));
				column.put("unit", KQReportBiz.getUnitType(kqReportFieldComInfo, user));
				column.put("dataIndex", kqReportFieldComInfo.getFieldname());
				column.put("type", kqReportFieldComInfo.getFieldname());
				column.put("key", kqReportFieldComInfo.getFieldname());
				column.put("isSystem", kqReportFieldComInfo.getIsSystem());
				mapChildColumnInfo = this.getChildColumnsInfo(kqReportFieldComInfo.getFieldname(),user);
				childColumns = (List<Object>)mapChildColumnInfo.get("childColumns");
				if(childColumns.size()>0) {//跨列width取子列的width
					column.put("rowSpan", 1);
					column.put("width", mapChildColumnInfo.get("sumChildColumnWidth"));
					column.put("children", childColumns);
				}else{
					column.put("rowSpan", 3);
					column.put("width", Util.getIntValue(kqReportFieldComInfo.getWidth()));
				}
				column.put("showDetial",kqReportFieldComInfo.getShowDetial());
				columns.add(column);
				columns.addAll(this.getCascadeKeyColumnsInfo(kqReportFieldComInfo.getCascadekey(),user));
			}

			boolean isEnd = false;
			Calendar cal = DateUtil.getCalendar();
			String today = DateUtil.getCurrentDate();
//			if(DateUtil.compDate(today, toDate)>0){//结束日期不大于今天
//				toDate = today;
//				if(DateUtil.compDate(today, fromDate)>0){//结束日期不大于今天
//          fromDate = today;
//        }
//			}

			childColumns = new ArrayList<>();
			for(String date=fromDate; !isEnd;) {
				if(date.equals(toDate)) isEnd = true;
				column = new HashMap();
				column.put("title", DateUtil.geDayOfMonth(date));
				column.put("dataIndex", date);
				column.put("key", date);
				column.put("type", date);
				column.put("rowSpan", 1);
				column.put("width", 90);
				column.put("isCalendar", 1);
				childColumns.add(column);
				cal.setTime(DateUtil.parseToDate(date));
				date = DateUtil.getDate(cal.getTime(), 1);
			}

			column = new HashMap();
			column.put("title", SystemEnv.getHtmlLabelName(386476, user.getLanguage()));
			column.put("dataIndex", "kqCalendar");
			column.put("key", "kqCalendar");
			if(childColumns.size()>0) {//跨列width取子列的width
				column.put("rowSpan", 1);
				column.put("width", childColumns.size()*90);
				column.put("children", childColumns);
			}
			columns.add(column);

			String forgotBeginWorkCheck_field = " sum(b.forgotBeginWorkCheck) ";

      if(rs.getDBType().equalsIgnoreCase("oracle")) {
        forgotBeginWorkCheck_field = " sum(nvl(b.forgotBeginWorkCheck,0))  ";
      }else if((rs.getDBType()).equalsIgnoreCase("mysql")){
        forgotBeginWorkCheck_field = " sum(ifnull(b.forgotBeginWorkCheck,0)) ";
      }else {
        forgotBeginWorkCheck_field = " sum(isnull(b.forgotBeginWorkCheck,0)) ";
      }

			Map<String,Object> definedFieldInfo = new KQFormatBiz().getDefinedField();
			String definedFieldSum = Util.null2String(definedFieldInfo.get("definedFieldSum"));

			String backFields = " a.id,a.lastname,a.workcode,a.dsporder,b.resourceid,a.subcompanyid1 as subcompanyid,a.departmentid,a.jobtitle," +
													" sum(b.workdays) as workdays,sum(b.workMins) as workMins,sum(b.attendancedays) as attendancedays," +
													" sum(b.attendanceMins) as attendanceMins,sum(b.beLate) as beLate,sum(b.beLateMins) as beLateMins, " +
													" sum(b.graveBeLate) as graveBeLate, sum(b.graveBeLateMins) as graveBeLateMins,sum(b.leaveEearly) as leaveEearly," +
													" sum(b.leaveEarlyMins) as leaveEarlyMins, sum(b.graveLeaveEarly) as graveLeaveEarly, " +
												  " sum(b.graveLeaveEarlyMins) as graveLeaveEarlyMins,sum(b.absenteeism) as absenteeism, " +
													"sum(b.earlyshift) as earlyshift,"+
													"sum(b.nightshift) as nightshift,"+
													" sum(b.signdays) as signdays,sum(b.signmins) as signmins, "+
													" sum(b.absenteeismMins) as absenteeismMins, sum(b.forgotCheck)+"+forgotBeginWorkCheck_field+" as forgotCheck "+(definedFieldSum.length()>0?","+definedFieldSum+"":"");

			if(rs.getDBType().equals("oracle")){
				backFields = 	"/*+ index(kq_format_total IDX_KQ_FORMAT_TOTAL_KQDATE) */ "+backFields;
			}
			String sqlFrom = " from hrmresource a, kq_format_total b where a.id= b.resourceid and b.kqdate >='"+fromDate+"' and b.kqdate <='"+toDate+"'";
			String sqlWhere = rightSql;
			String groupBy = " group by a.id,a.lastname,a.workcode,a.dsporder,b.resourceid,a.subcompanyid1,a.departmentid,a.jobtitle ";
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

      if(status.length()>0){
        if (!status.equals("8") && !status.equals("9")) {
          sqlWhere += " and a.status = "+status+ "";
        }else if (status.equals("8")) {
          sqlWhere += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
        }
      }

			sql = " select count(*) as c from ( select 1 as c "+sqlFrom+sqlWhere+groupBy+") t";
			rs.execute(sql);
			if (rs.next()){
				count = rs.getInt("c");
			}

			if (count <= 0) {
				pageCount = 0;
			}

			pageCount = count / pageSize + ((count % pageSize > 0) ? 1 : 0);

			isHaveNext = (pageIndex + 1 <= pageCount) ? 1 : 0;

			isHavePre = (pageIndex - 1 >= 1) ? 1 : 0;

			String orderBy = " order by t.dsporder asc, t.lastname asc ";
			String descOrderBy = " order by t.dsporder desc, t.lastname desc ";

			//默认排序设置 start，有性能问题，先取消，后面再看看有没有好的方式
//			String orderBySql = "select * from kq_report_order where userId=? and sort=1 order by orders";
//			rs.executeQuery(orderBySql, user.getUID());
//			if (rs.getCounts() <= 0) {
//				orderBySql = "select * from kq_report_order where userId=0 and sort=1 order by orders";
//				rs.executeQuery(orderBySql);
//			}
//			while (rs.next()) {
//				String dataIndex = rs.getString("dataIndex");
//				String ascOrDesc = rs.getString("ascOrDesc");
//				String ascOrDesc1 = (ascOrDesc.equals("")||ascOrDesc.equals("asc"))?"desc":"asc";
//				if (dataIndex.equals("organization")) {
//					orderBy += ",showOrderOfDeptTree " + ascOrDesc + ",dept_id " + ascOrDesc;
//					descOrderBy += ",showOrderOfDeptTree " + ascOrDesc1 + ",dept_id " + ascOrDesc1;
//				}
//				if (dataIndex.equalsIgnoreCase("dspOrder") || dataIndex.equalsIgnoreCase("lastName")) {
//					orderBy += "," + dataIndex + " " + ascOrDesc + ",id " + ascOrDesc;
//					descOrderBy += "," + dataIndex + " " + ascOrDesc1 + ",id " + ascOrDesc1;
//				} else if (dataIndex.equalsIgnoreCase("deptShowOrder") || dataIndex.equalsIgnoreCase("deptName")) {
//					orderBy += "," + dataIndex + " " + ascOrDesc + ",dept_id " + ascOrDesc;
//					descOrderBy += "," + dataIndex + " " + ascOrDesc1 + ",dept_id " + ascOrDesc1;
//				} else if (dataIndex.equalsIgnoreCase("subcomShowOrder") || dataIndex.equalsIgnoreCase("subcomName")) {
//					orderBy += "," + dataIndex + " " + ascOrDesc + ",subcom_id " + ascOrDesc;
//					descOrderBy += "," + dataIndex + " " + ascOrDesc1 + ",subcom_id " + ascOrDesc1;
//				}
//			}
//			orderBy = orderBy.startsWith(",") ? orderBy.substring(1) : orderBy;
//			descOrderBy = descOrderBy.startsWith(",") ? descOrderBy.substring(1) : descOrderBy;
//			orderBy = orderBy.equals("") ? " t.dspOrder,t.id " : orderBy;
//			descOrderBy = descOrderBy.equals("") ? " t.dspOrder,t.id " : descOrderBy;
//			orderBy = "order by "+orderBy;

			sql = backFields + sqlFrom  + sqlWhere + groupBy;

			if (pageIndex > 0 && pageSize > 0) {
				if (rs.getDBType().equals("oracle")) {
					sql = " select * from (select " + sql+") t "+orderBy;
					sql = "select * from ( select row_.*, rownum rownum_ from ( " + sql + " ) row_ where rownum <= "
						+ (pageIndex * pageSize) + ") where rownum_ > " + ((pageIndex - 1) * pageSize);
				} else if (rs.getDBType().equals("mysql")) {
					sql = " select * from (select " + sql+") t "+orderBy;
					sql = "select t1.* from (" + sql + ") t1 limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
				}
				else if (rs.getDBType().equals("postgresql")) {
					sql = " select * from (select " + sql+") t "+orderBy;
					sql = "select t1.* from (" + sql + ") t1 limit " +pageSize + " offset " + ((pageIndex - 1) * pageSize);
				}
				else {
					orderBy = " order by dsporder asc, lastname asc ";
					descOrderBy = " order by dsporder desc, lastname desc ";
					if (pageIndex > 1) {
						int topSize = pageSize;
						if (pageSize * pageIndex > count) {
							topSize = count - (pageSize * (pageIndex - 1));
						}
						sql = " select top " + topSize + " * from ( select top  " + topSize + " * from ( select top "
							+ (pageIndex * pageSize) + sql + orderBy+ " ) tbltemp1 " + descOrderBy + ") tbltemp2 " + orderBy;
					} else {
						sql = " select top " + pageSize + sql+orderBy;
					}
				}
			} else {
				sql = " select " + sql;
			}


			// #1475814-概述：满足考勤报分部部门显示及导出时显示全路径
			String fullPathMainKey = "show_full_path";
			KQSettingsComInfo kqSettingsComInfo = new KQSettingsComInfo();
			String isShowFullPath = Util.null2String(kqSettingsComInfo.getMain_val(fullPathMainKey),"0");

			String show_card_source = Util.null2String(kqSettingsComInfo.getMain_val("show_card_source"),"0");//是否显示打卡数据，以及打卡数据来源
			params.put("show_card_source",show_card_source);
			KQOvertimeRulesBiz kqOvertimeRulesBiz = new KQOvertimeRulesBiz();
			int uintType = kqOvertimeRulesBiz.getMinimumUnit();//当前加班单位
			double hoursToDay = kqOvertimeRulesBiz.getHoursToDay();//当前天跟小时计算关系
			params.put("uintType",uintType);
			params.put("hoursToDay",hoursToDay);
			Map<String,Object> flowData = kqReportBiz.getFlowData(params,user);
			new BaseBean().writeLog("==zj==(考勤报表获取sql)" +sql);

			rs.execute(sql);
			while (rs.next()) {
				data = new HashMap<>();
				kqReportFieldComInfo.setTofirstRow();
				String id = rs.getString("id");
				data.put("resourceId",id);
				while (kqReportFieldComInfo.next()){
					if(!Util.null2String(kqReportFieldComInfo.getIsdataColumn()).equals("1"))continue;
					if(!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("month"))continue;
					if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0){
						continue;
					}
					String fieldName = kqReportFieldComInfo.getFieldname();
					String fieldValue = "";
					if(fieldName.equals("subcompany")){
						String tmpSubcompanyId = Util.null2String(rs.getString("subcompanyid"));
						if(tmpSubcompanyId.length()==0){
							tmpSubcompanyId =  Util.null2String(resourceComInfo.getSubCompanyID(id));
						}
						data.put("subcompanyId",tmpSubcompanyId);

						fieldValue = "1".equals(isShowFullPath) ?
								SubCompanyComInfo.getSubcompanyRealPath(tmpSubcompanyId, "/", "0") :
								subCompanyComInfo.getSubCompanyname(tmpSubcompanyId);

						// fieldValue = subCompanyComInfo.getSubCompanyname(tmpSubcompanyId);
					}else if(fieldName.equals("department")){
						String tmpDepartmentId = Util.null2String(rs.getString("departmentid"));
						if(tmpDepartmentId.length()==0){
							tmpDepartmentId =  Util.null2String(resourceComInfo.getDepartmentID(id));
						}
						data.put("departmentId",tmpDepartmentId);

						fieldValue = "1".equals(isShowFullPath) ?
								departmentComInfo.getDepartmentRealPath(tmpDepartmentId, "/", "0") :
								departmentComInfo.getDepartmentname(tmpDepartmentId);

						// fieldValue = departmentComInfo.getDepartmentname(tmpDepartmentId);
					}else if(fieldName.equals("jobtitle")){
						String tmpJobtitleId = Util.null2String(rs.getString("jobtitle"));
						if(tmpJobtitleId.length()==0){
							tmpJobtitleId =  Util.null2String(resourceComInfo.getJobTitle(id));
						}
						data.put("jobtitleId",tmpJobtitleId);
						fieldValue = jobTitlesComInfo.getJobTitlesname(tmpJobtitleId);
					}else if(fieldName.equals("attendanceSerial")){
						List<String> serialIds = null;
						if(attendanceSerial.length()>0){
							serialIds = Util.splitString2List(attendanceSerial,",");
						}
						for(int i=0;serialIds!=null&&i<serialIds.size();i++){
							data.put(serialIds.get(i), kqReportBiz.getSerialCount(id,fromDate,toDate,serialIds.get(i)));
						}
					}else if(kqReportFieldComInfo.getParentid().equals("overtime")||kqReportFieldComInfo.getParentid().equals("overtime_nonleave")
              ||kqReportFieldComInfo.getParentid().equals("overtime_4leave")||fieldName.equals("businessLeave") || fieldName.equals("officialBusiness")){
						if(fieldName.equals("overtimeTotal")){
							double workingDayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|workingDayOvertime_4leave")));
							workingDayOvertime_4leave = workingDayOvertime_4leave<0?0:workingDayOvertime_4leave;
							double restDayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|restDayOvertime_4leave")));
							restDayOvertime_4leave = restDayOvertime_4leave<0?0:restDayOvertime_4leave;
							double holidayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|holidayOvertime_4leave")));
							holidayOvertime_4leave = holidayOvertime_4leave<0?0:holidayOvertime_4leave;

              double workingDayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|workingDayOvertime_nonleave")));
              workingDayOvertime_nonleave = workingDayOvertime_nonleave<0?0:workingDayOvertime_nonleave;
              double restDayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|restDayOvertime_nonleave")));
              restDayOvertime_nonleave = restDayOvertime_nonleave<0?0:restDayOvertime_nonleave;
              double holidayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|holidayOvertime_nonleave")));
              holidayOvertime_nonleave = holidayOvertime_nonleave<0?0:holidayOvertime_nonleave;

							fieldValue = KQDurationCalculatorUtil.getDurationRound(String.valueOf(workingDayOvertime_4leave+restDayOvertime_4leave+holidayOvertime_4leave+
                  workingDayOvertime_nonleave+restDayOvertime_nonleave+holidayOvertime_nonleave));
						}else if(fieldName.equals("businessLeave") || fieldName.equals("officialBusiness")){
              String businessLeaveData = Util.null2s(Util.null2String(flowData.get(id+"|"+fieldName)),"0.0");
              String backType = fieldName+"_back";
              String businessLeavebackData = Util.null2s(Util.null2String(flowData.get(id+"|"+backType)),"0.0");
              String businessLeave = "";
              try{
                //以防止出现精度问题
                if(businessLeaveData.length() == 0){
                  businessLeaveData = "0.0";
                }
                if(businessLeavebackData.length() == 0){
                  businessLeavebackData = "0.0";
                }
                BigDecimal b_businessLeaveData = new BigDecimal(businessLeaveData);
                BigDecimal b_businessLeavebackData = new BigDecimal(businessLeavebackData);
                businessLeave = b_businessLeaveData.subtract(b_businessLeavebackData).toString();
                if(Util.getDoubleValue(businessLeave, -1) < 0){
                  businessLeave = "0.0";
                }
              }catch (Exception e){
              }
              fieldValue = KQDurationCalculatorUtil.getDurationRound(businessLeave);
            }else{
							fieldValue = KQDurationCalculatorUtil.getDurationRound(Util.null2String(flowData.get(id+"|"+fieldName)));
						}
					}else if(("leaveEarlyMinsD".equals(fieldName) || "beLateMinsD".equals(fieldName)) && !isMyAttendance){

							new BaseBean().writeLog("==zj==(是否来自我的考勤-leaveEarlyMinsD-beLateMinsD)" + isMyAttendance);
							//==zj 这里获取自定义字段的值,早班分钟数，晚班分钟数
							if ("leaveEarlyMinsD".equals(fieldName)){
								fieldValue = Util.null2String(rs.getString("leaveEarlyMins"));
							}
							if ("beLateMinsD".equals(fieldName)){
								fieldValue = Util.null2String(rs.getString("beLateMins"));
							}
							new BaseBean().writeLog("==zj==(获取早班分钟数或者晚班分钟数修改)" + fieldValue);
							if ("".equals(fieldValue) || fieldValue == null){
								fieldValue = "0";
							}


					}else if(("workdays".equals(fieldName) || "workmins".equals(fieldName)) &&  !isMyAttendance){
						//==zj 这里修改应出勤天数和应工作时长，按照节假日设置显示（如果来自我的考勤就会显示冲突，所以做下区分）
						new BaseBean().writeLog("==zj==(是否来自我的考勤-workdays-workmins)" + isMyAttendance);
							kqUtil kqUtil = new kqUtil();
							new BaseBean().writeLog("==zj==(查询开始时间和结束时间)" + fromDate + " | " + toDate);
							Map<String, Object> result = kqUtil.workDaysCount(fromDate, toDate);
							new BaseBean().writeLog("==zj==(查询结果)" + JSON.toJSONString(result));

							if ("workdays".equals(fieldName)){
								fieldValue =Util.null2String( result.get("workDays"));
							}

							if ("workmins".equals(fieldName)){
								fieldValue = Util.null2String(result.get("workMins"));
							}


					}else if(("overtimePeace".equals(fieldName)) &&  !isMyAttendance){
						new BaseBean().writeLog("==zj==(是否来自我的考勤-overtimePeace)" + isMyAttendance);
							//==zj 这里对平时加班字段进行赋值
							String earlyshift = rs.getString("earlyshift");
							String nightshift = rs.getString("nightshift");
							kqUtil kqUtil = new kqUtil();

							//平时加班计算
							fieldValue = kqUtil.leaveCount(flowData,id,fromDate,toDate,earlyshift,nightshift);
							new BaseBean().writeLog("==zj==(平时加班汇总)" + fieldValue);

					}else {
						fieldValue = Util.null2String(rs.getString(fieldName));
						if(Util.null2String(kqReportFieldComInfo.getUnittype()).length()>0) {
							if(fieldValue.length() == 0){
								fieldValue="0";
							}else{
								if (kqReportFieldComInfo.getUnittype().equals("2")) {
									fieldValue = KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(fieldValue) / 60.0)));
								}
							}
						}
					}
					data.put(fieldName,fieldValue);
				}

				//请假
				List<Map<String, Object>> allLeaveRules = kqLeaveRulesBiz.getAllLeaveRules();
				Map<String, Object> leaveRule = null;
				for(int i=0;allLeaveRules!=null&&i<allLeaveRules.size();i++){
					leaveRule = (Map<String, Object>)allLeaveRules.get(i);
					String flowType = Util.null2String("leaveType_"+leaveRule.get("id"));
					String leaveData = Util.null2String(flowData.get(id+"|"+flowType));
					String flowLeaveBackType = Util.null2String("leavebackType_"+leaveRule.get("id"));
					String leavebackData = Util.null2s(Util.null2String(flowData.get(id+"|"+flowLeaveBackType)),"0.0");
					String b_flowLeaveData = "";
          String flowLeaveData = "";
					try{
            //以防止出现精度问题
            if(leaveData.length() == 0){
              leaveData = "0.0";
            }
            if(leavebackData.length() == 0){
              leavebackData = "0.0";
            }
            BigDecimal b_leaveData = new BigDecimal(leaveData);
            BigDecimal b_leavebackData = new BigDecimal(leavebackData);
            b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
            if(Util.getDoubleValue(b_flowLeaveData, -1) < 0){
              b_flowLeaveData = "0.0";
            }
          }catch (Exception e){
					  writeLog("GetKQReportCmd:leaveData"+leaveData+":leavebackData:"+leavebackData+":"+e);
          }

          //考虑下冻结的数据
          if(b_flowLeaveData.length() > 0){
            flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
          }else{
            flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData,0.0)-Util.getDoubleValue(leavebackData,0.0)));
          }
					data.put(flowType,flowLeaveData);
				}

				Map<String,Object> detialDatas = kqReportBiz.getDetialDatas(id,fromDate,toDate,user,flowData,false,uintType,show_card_source);
//        new KQLog().info("id:"+id+":detialDatas:"+detialDatas);
				isEnd = false;
				for(String date=fromDate; !isEnd;) {
					if(date.equals(toDate)) isEnd = true;
					if(DateUtil.compDate(today, date)>0){
						data.put(date,"");
					}else{
//            new KQLog().info("id:date:"+(id+"|"+date)+":detialDatas.get:"+detialDatas.get(id+"|"+date));
						data.put(date,detialDatas.get(id+"|"+date)==null?SystemEnv.getHtmlLabelName(26593, user.getLanguage()):detialDatas.get(id+"|"+date));
					}
					cal.setTime(DateUtil.parseToDate(date));
					date = DateUtil.getDate(cal.getTime(), 1);
				}
				datas.add(data);
			}

			List<Object> lsHolidays = KQHolidaySetBiz.getHolidaySetListByScope(""+user.getUID(),fromDate,toDate);
			retmap.put("holidays", lsHolidays);

			retmap.put("columns",columns);
			retmap.put("datas",datas);
			retmap.put("pagesize", pageSize);
			retmap.put("pageindex", pageIndex);
			retmap.put("count", count);
			retmap.put("pagecount", pageCount);
			retmap.put("ishavepre", isHavePre);
			retmap.put("ishavenext", isHaveNext);
		}catch (Exception e){
			writeLog(e);
			new BaseBean().writeLog("==zj==(获取考勤汇总报表报错信息)" + e);
		}
		return retmap;
	}

	private Map<String,Object> getChildColumnsInfo(String parentid, User user){
		Map<String,Object> returnMap = new HashMap<>();
		List<Object> lsChildColumns = new ArrayList<>();
		Map column = null;
		int sumChildColumnWidth = 0;
		if(parentid.equals("attendanceSerial")){//考勤班次
			KQShiftManagementComInfo kqShiftManagementComInfo = new KQShiftManagementComInfo();
			JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
			List<String> serialIds = null;
			if(Util.null2String(jsonObj.get("attendanceSerial")).length()>0){
				serialIds = Util.splitString2List(Util.null2String(jsonObj.get("attendanceSerial")),",");
			}
			for(int i=0;serialIds!=null&&i<serialIds.size();i++){
				column = new HashMap();
				column.put("title", kqShiftManagementComInfo.getSerial(serialIds.get(i)));
				column.put("unit", "");
				column.put("width", 65);
				column.put("dataIndex", serialIds.get(i));
				column.put("key", serialIds.get(i));
        column.put("rowSpan", 2);
				column.put("colSpan", 1);
				sumChildColumnWidth+=65;
				lsChildColumns.add(column);
			}
		}else if(parentid.equals("leave")){
			KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
			List<Map<String, Object>> leaveRules = kqLeaveRulesBiz.getAllLeaveRules();
			for(int i=0;leaveRules!=null&&i<leaveRules.size();i++){
				Map<String, Object> leaveRule = leaveRules.get(i);
				String id = "leaveType_"+Util.null2String(leaveRule.get("id"));
				String name = Util.null2String(leaveRule.get("name"));
				String unitType = Util.null2String(leaveRule.get("unitType"));
				column = new HashMap();
				column.put("title", name);
				column.put("unit", KQUnitBiz.isLeaveHour(unitType) ?SystemEnv.getHtmlLabelName(391, user.getLanguage()):SystemEnv.getHtmlLabelName(1925, user.getLanguage()));
				column.put("width", 65);
				column.put("dataIndex", id);
				column.put("key", id);
				column.put("rowSpan", 2);
				column.put("colSpan", 1);
				column.put("showDetial","1");
				sumChildColumnWidth+=65;
				lsChildColumns.add(column);
			}
		}else if(parentid.equals("overtime")){
      String[] overtimeChild = {"overtime_nonleave","overtime_4leave","overtimeTotal"};
      for(int i=0;i<overtimeChild.length;i++){
        String id = overtimeChild[i];
        column = new HashMap();
        String fieldlabel = "";
        column.put("unit", "");
        if("overtime_nonleave".equalsIgnoreCase(id)){
          fieldlabel = "125805";
        }else if("overtime_4leave".equalsIgnoreCase(id)){
          fieldlabel = "125804";
        }else{
          fieldlabel = "523";
          column.put("showDetial","1");
          String unitType = (KQOvertimeRulesBiz.getMinimumUnit()==3 || KQOvertimeRulesBiz.getMinimumUnit()==5 ||KQOvertimeRulesBiz.getMinimumUnit()==6)?"2":"1";
          String unitTypeName = "";
          if(Util.null2String(unitType).length()>0){
            if(unitType.equals("1")){
              unitTypeName=SystemEnv.getHtmlLabelName(1925, user.getLanguage());
            }else if(unitType.equals("2")){
              unitTypeName=SystemEnv.getHtmlLabelName(391, user.getLanguage());
            }else if(unitType.equals("3")){
              unitTypeName=SystemEnv.getHtmlLabelName(18083, user.getLanguage());
            }
          }
          column.put("unit", unitTypeName);
        }
        column.put("title", SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()));
        column.put("dataIndex", id);
        column.put("key", id);
        column.put("rowSpan", 1);
        Map<String,Object> mapChildColumnInfo = getChildColumnsInfo(id, user);
        int childWidth = 65;
        List<Object> childColumns = (List<Object>)mapChildColumnInfo.get("childColumns");
        if(childColumns.size()>0) {//跨列width取子列的width
          column.put("children", childColumns);
          childWidth = Util.getIntValue(Util.null2String(mapChildColumnInfo.get("sumChildColumnWidth")),65);
        }
        column.put("width", childWidth+"");
        sumChildColumnWidth+=childWidth;
        lsChildColumns.add(column);
      }
    }else{
			KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
			while (kqReportFieldComInfo.next()){
				if(kqReportFieldComInfo.getParentid().equals(parentid)) {
					if(!kqReportFieldComInfo.getReportType().equals("month"))continue;
					column = new HashMap();
					column.put("title", SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(), user.getLanguage()));
					column.put("unit", KQReportBiz.getUnitType(kqReportFieldComInfo, user));
					column.put("width", Util.getIntValue(kqReportFieldComInfo.getWidth()));
					column.put("dataIndex", kqReportFieldComInfo.getFieldname());
					column.put("key", kqReportFieldComInfo.getFieldname());
					column.put("rowSpan", 1);
					column.put("colSpan", 1);
					column.put("showDetial",kqReportFieldComInfo.getShowDetial());
					sumChildColumnWidth+=Util.getIntValue(kqReportFieldComInfo.getWidth());
					lsChildColumns.add(column);
				}
			}
		}
		returnMap.put("childColumns",lsChildColumns);
		returnMap.put("sumChildColumnWidth",sumChildColumnWidth);
		return returnMap;
	}

	private List<Object>  getCascadeKeyColumnsInfo(String cascadeKey, User user){
		List<Object> lsChildColumns = new ArrayList<>();
		if(Util.null2String(cascadeKey).length()==0){
			return lsChildColumns;
		}
		Map<String,Object> column = null;
		List<String> lsCascadeKey = Util.splitString2List(cascadeKey,",");
		KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
		for(int i=0;i<lsCascadeKey.size();i++){
			kqReportFieldComInfo.setTofirstRow();
			while (kqReportFieldComInfo.next()) {
				if(!kqReportFieldComInfo.getReportType().equals("month"))continue;
				if (kqReportFieldComInfo.getFieldname().equals(lsCascadeKey.get(i))){
					column = new HashMap();
					column.put("title", SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(), user.getLanguage()));
					column.put("unit", KQReportBiz.getUnitType(kqReportFieldComInfo, user));
					column.put("width", Util.getIntValue(kqReportFieldComInfo.getWidth()));
					column.put("dataIndex", kqReportFieldComInfo.getFieldname());
					column.put("key", kqReportFieldComInfo.getFieldname());
					column.put("rowSpan", 1);
					column.put("colSpan", 1);
					column.put("showDetial",kqReportFieldComInfo.getShowDetial());
					column.put("isSystem", kqReportFieldComInfo.getIsSystem());
					lsChildColumns.add(column);
				}
			}
		}
		return lsChildColumns;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

}
