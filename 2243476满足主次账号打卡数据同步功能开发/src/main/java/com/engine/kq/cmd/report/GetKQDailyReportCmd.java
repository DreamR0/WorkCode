package com.engine.kq.cmd.report;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.util.PageUidFactory;
import java.math.BigDecimal;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.util.*;

public class GetKQDailyReportCmd extends AbstractCommonCommand<Map<String, Object>> {

  private KQLog kqLog = new KQLog();

	public GetKQDailyReportCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		RecordSet rs = new RecordSet();
		String sql = "";
		try{
			String pageUid = PageUidFactory.getHrmPageUid("KQDailyReport");

			SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
		  DepartmentComInfo departmentComInfo = new DepartmentComInfo();
			ResourceComInfo resourceComInfo = new ResourceComInfo();
			JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
			KQWorkTime kqWorkTime = new KQWorkTime();
			ShiftManagementToolKit shiftManagementToolKit = new ShiftManagementToolKit();
			KQReportBiz kqReportBiz = new KQReportBiz();
			KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();

			String rightSql = new KQReportBiz().getReportRight("2",""+user.getUID(),"a");

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
      //人员状态
      String status = Util.null2String(jsonObj.get("status"));
			String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));
			String departmentId = Util.null2String(jsonObj.get("departmentId"));
			String resourceId = Util.null2String(jsonObj.get("resourceId"));
			String allLevel = Util.null2String(jsonObj.get("allLevel"));
			String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));
			String viewScope = Util.null2String(jsonObj.get("viewScope"));
			if(typeselect.length()==0)typeselect = "3";
			int pageIndex = Util.getIntValue(Util.null2String(jsonObj.get("pageIndex")), 1);
			int pageSize =  KQReportBiz.getPageSize(Util.null2String(jsonObj.get("pageSize")),pageUid,user.getUID());
			int count = 0;
			int pageCount = 0;
			int isHavePre = 0;
			int isHaveNext = 0;

			List<Map<String, Object>> leaveRules = kqLeaveRulesBiz.getAllLeaveRules();
			List<Object> columns = new ArrayList();
			Map<String,Object> column = null;
			List<Object> datas = new ArrayList();
			Map<String,Object> data = null;
			Map<String,Object> mapChildColumnInfo = null;
			List<Object> childColumns = null;
			KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();
			while (kqReportFieldComInfo.next()){
				if(Util.null2String(kqReportFieldComInfo.getParentid()).length()>0)continue;
				boolean isDaily = kqReportFieldComInfo.getReportType().equals("daily");
				if(!kqReportFieldComInfo.getReportType().equals("all") && !isDaily)continue;
				if("leave".equalsIgnoreCase(kqReportFieldComInfo.getFieldname())&&leaveRules.size()==0){
					continue;
				}
				column = new HashMap();
				column.put("title", SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(), user.getLanguage()));
				column.put("unit", KQReportBiz.getUnitType(kqReportFieldComInfo, user));
				column.put("dataIndex", kqReportFieldComInfo.getFieldname());
				column.put("key", kqReportFieldComInfo.getFieldname());
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
				if(kqReportFieldComInfo.getReportType1().equals("daily")){
          column.put("isdaily", "1");
        }
				column.put("showDetial",kqReportFieldComInfo.getShowDetial());
				columns.add(column);
			}

//			String today = DateUtil.getCurrentDate();
//			if(DateUtil.compDate(today, toDate)>0){//结束如期不大于今天
//				toDate = today;
//        if(DateUtil.compDate(today, fromDate)>0){
//          fromDate = today;
//        }
//			}
      String forgotBeginWorkCheck_field = " b.forgotbeginworkcheck ";

      if(rs.getDBType().equalsIgnoreCase("oracle")&&!Util.null2String(rs.getOrgindbtype()).equals("dm")&&!Util.null2String(rs.getOrgindbtype()).equals("st")&&!Util.null2String(rs.getOrgindbtype()).equals("jc")) {
        forgotBeginWorkCheck_field = " nvl(b.forgotBeginWorkCheck,0)  ";
      }else if((rs.getDBType()).equalsIgnoreCase("mysql")){
        forgotBeginWorkCheck_field = " ifnull(b.forgotBeginWorkCheck,0) ";
      }else {
        forgotBeginWorkCheck_field = " isnull(b.forgotBeginWorkCheck,0) ";
      }

			String backFields = " a.id,a.lastname,a.subcompanyid1 as subcompanyid,a.departmentid, a.workcode,b.jobtitle,a.dsporder," +
													" b.kqdate, b.workdays,b.workMins,b.serialid, b.attendancedays,b.attendanceMins," +
													" b.beLate,b.beLateMins,b.graveBeLate,b.graveBeLateMins,b.leaveEearly,b.leaveEarlyMins," +
													" b.signdays,b.signmins, "+
													" b.graveLeaveEarly,b.graveLeaveEarlyMins,b.absenteeism ,b.absenteeismMins ,(b.forgotCheck+"+forgotBeginWorkCheck_field+") forgotCheck ";
			String sqlFrom = " from hrmresource a, kq_format_total b where a.id= b.resourceid and b.kqdate >='"+fromDate+"' and b.kqdate <='"+toDate+"'";
			String sqlWhere = rightSql;
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

			sql = " select count(*) as c from ( select 1 as c "+sqlFrom+sqlWhere+") t";
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

			String orderBy = " order by a.dsporder asc, a.lastname asc, b.kqdate asc ";
			String descOrderBy = " order by a.dsporder desc, a.lastname desc, b.kqdate desc ";
			sql = backFields + sqlFrom  + sqlWhere + orderBy;

			if (pageIndex > 0 && pageSize > 0) {
				if (rs.getDBType().equals("oracle")) {
					sql = " select " + sql;
					sql = "select * from ( select row_.*, rownum rownum_ from ( " + sql + " ) row_ where rownum <= "
									+ (pageIndex * pageSize) + ") where rownum_ > " + ((pageIndex - 1) * pageSize);
				} else if (rs.getDBType().equals("mysql")) {
					sql = " select " + sql;
					sql = "select t1.* from (" + sql + ") t1 limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
				}
				else if (rs.getDBType().equals("postgresql")) {
					sql = " select " + sql;
					sql = "select t1.* from (" + sql + ") t1 limit " + pageSize+ " offset " + ((pageIndex - 1) * pageSize);
				}
				else {
					orderBy = " order by dsporder asc, lastname asc, kqdate asc ";
					descOrderBy = " order by dsporder desc, lastname desc, kqdate desc ";
					if (pageIndex > 1) {
						int topSize = pageSize;
						if (pageSize * pageIndex > count) {
							topSize = count - (pageSize * (pageIndex - 1));
						}
						sql = " select top " + topSize + " * from ( select top  " + topSize + " * from ( select top "
										+ (pageIndex * pageSize) + sql+" ) tbltemp1 " + descOrderBy + ") tbltemp2 " + orderBy;
					} else {
						sql = " select top " + pageSize + sql;
					}
				}
			} else {
				sql = " select " + sql;
			}
      Map<String,Object> flowData = kqReportBiz.getDailyFlowData(params,user);

			// #1475814-概述：满足考勤报分部部门显示及导出时显示全路径
			String fullPathMainKey = "show_full_path";
			KQSettingsComInfo kqSettingsComInfo = new KQSettingsComInfo();
			String isShowFullPath = Util.null2String(kqSettingsComInfo.getMain_val(fullPathMainKey),"0");

			new BaseBean().writeLog("==zj==(考勤日报表)" + sql);
			rs.execute(sql);
			while (rs.next()) {
				String id = rs.getString("id");
				String kqdate = rs.getString("kqdate");
				WorkTimeEntity workTime = kqWorkTime.getWorkTime(id, kqdate);
				data = new HashMap<>();
				kqReportFieldComInfo.setTofirstRow();
				while (kqReportFieldComInfo.next()) {
					if (!Util.null2String(kqReportFieldComInfo.getIsdataColumn()).equals("1")) continue;
					if (!kqReportFieldComInfo.getReportType().equals("all") && !kqReportFieldComInfo.getReportType().equals("daily"))
						continue;
					String fieldName = kqReportFieldComInfo.getFieldname();
					String fieldValue = "";
					if (fieldName.equals("subcompany")) {
						String fieldValueID = rs.getString("subcompanyid");
						fieldValue = "1".equals(isShowFullPath) ?
								SubCompanyComInfo.getSubcompanyRealPath(fieldValueID, "/", "0") :
								subCompanyComInfo.getSubCompanyname(fieldValueID);

						//fieldValue = subCompanyComInfo.getSubCompanyname(fieldValueID);

						if (fieldValue.length() == 0) {
							fieldValueID = resourceComInfo.getSubCompanyID(id);

							fieldValue = "1".equals(isShowFullPath) ?
									SubCompanyComInfo.getSubcompanyRealPath(fieldValueID, "/", "0") :
									subCompanyComInfo.getSubCompanyname(fieldValueID);

							// fieldValue = subCompanyComInfo.getSubCompanyname(fieldValueID);
						}
						data.put(fieldName + "Id", fieldValueID);
						data.put(fieldName, fieldValue);
					} else if (fieldName.equals("department")) {
						String fieldValueID = rs.getString("departmentid");
						fieldValue = "1".equals(isShowFullPath) ?
								departmentComInfo.getDepartmentRealPath(fieldValueID, "/", "0") :
								departmentComInfo.getDepartmentname(fieldValueID);
						//fieldValue = departmentComInfo.getDepartmentname(fieldValueID);

						if (fieldValue.length() == 0) {
							fieldValueID = resourceComInfo.getDepartmentID(id);

							fieldValue = "1".equals(isShowFullPath) ?
									departmentComInfo.getDepartmentRealPath(fieldValueID, "/", "0") :
									departmentComInfo.getDepartmentname(fieldValueID);

							// fieldValue = departmentComInfo.getDepartmentname(fieldValueID);
						}
						data.put(fieldName + "Id", fieldValueID);
						data.put(fieldName, fieldValue);
					} else if (fieldName.equals("jobtitle")) {
						String fieldValueID = rs.getString("jobtitle");
						fieldValue = jobTitlesComInfo.getJobTitlesname(rs.getString("jobtitle"));
						if (fieldValue.length() == 0) {
							fieldValueID = resourceComInfo.getJobTitle(id);
							fieldValue = jobTitlesComInfo.getJobTitlesname(fieldValueID);
						}
						data.put(fieldName + "Id", fieldValueID);
						data.put(fieldName, fieldValue);
					} else if (kqReportFieldComInfo.getParentid().equals("overtime") || kqReportFieldComInfo.getParentid().equals("overtime_nonleave")
									|| kqReportFieldComInfo.getParentid().equals("overtime_4leave") || fieldName.equals("businessLeave") || fieldName.equals("officialBusiness")) {
						if (fieldName.equals("overtimeTotal")) {
							double workingDayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id + "|" + kqdate + "|workingDayOvertime_4leave")));
							workingDayOvertime_4leave = workingDayOvertime_4leave < 0 ? 0 : workingDayOvertime_4leave;
							double restDayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id + "|" + kqdate + "|restDayOvertime_4leave")));
							restDayOvertime_4leave = restDayOvertime_4leave < 0 ? 0 : restDayOvertime_4leave;
							double holidayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id + "|" + kqdate + "|holidayOvertime_4leave")));
							holidayOvertime_4leave = holidayOvertime_4leave < 0 ? 0 : holidayOvertime_4leave;

							double workingDayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id + "|" + kqdate + "|workingDayOvertime_nonleave")));
							workingDayOvertime_nonleave = workingDayOvertime_nonleave < 0 ? 0 : workingDayOvertime_nonleave;
							double restDayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id + "|" + kqdate + "|restDayOvertime_nonleave")));
							restDayOvertime_nonleave = restDayOvertime_nonleave < 0 ? 0 : restDayOvertime_nonleave;
							double holidayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id + "|" + kqdate + "|holidayOvertime_nonleave")));
							holidayOvertime_nonleave = holidayOvertime_nonleave < 0 ? 0 : holidayOvertime_nonleave;

							fieldValue = KQDurationCalculatorUtil.getDurationRound(String.valueOf(workingDayOvertime_4leave + restDayOvertime_4leave + holidayOvertime_4leave +
											workingDayOvertime_nonleave + restDayOvertime_nonleave + holidayOvertime_nonleave));
						} else {
							fieldValue = KQDurationCalculatorUtil.getDurationRound(Util.null2String(flowData.get(id + "|" + kqdate + "|" + fieldName)));
						}
						data.put(fieldName, fieldValue);
					} else if (fieldName.equals("serialid")) {
						fieldValue = Util.null2String(rs.getString(fieldName));
						if (fieldValue.length()>0) {//弹性工作制没有班次
							data.put("serialid", shiftManagementToolKit.getShiftOnOffWorkSections(fieldValue, user.getLanguage()));
						}
					}else {
						fieldValue = Util.null2String(rs.getString(fieldName));
						if (kqReportFieldComInfo.getUnittype().equals("2") && fieldValue.length() > 0) {
							fieldValue = KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(fieldValue) / 60.0)));
						}
						data.put(fieldName, fieldValue);
					}
				}
        data.putAll(this.getSignDetailInfo(id, kqdate));

				//请假
				List<Map<String, Object>> allLeaveRules = KQLeaveRulesBiz.getAllLeaveRules();
				Map<String, Object> leaveRule = null;
				for (int i = 0; allLeaveRules != null && i < allLeaveRules.size(); i++) {
					leaveRule = (Map<String, Object>) allLeaveRules.get(i);
					String flowType = Util.null2String("leaveType_" + leaveRule.get("id"));
					String leaveData = Util.null2String(flowData.get(id + "|" + kqdate + "|" + flowType));
					String flowLeaveBackType = Util.null2String("leavebackType_" + leaveRule.get("id"));
					String leavebackData = Util.null2s(Util.null2String(flowData.get(id + "|" + kqdate + "|" + flowLeaveBackType)), "0.0");
					String b_flowLeaveData = "";
					String flowLeaveData = "";
					try {
						//以防止出现精度问题
						if (leaveData.length() == 0) {
							leaveData = "0.0";
						}
						if (leavebackData.length() == 0) {
							leavebackData = "0.0";
						}
						BigDecimal b_leaveData = new BigDecimal(leaveData);
						BigDecimal b_leavebackData = new BigDecimal(leavebackData);
						b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
            if(Util.getDoubleValue(b_flowLeaveData, -1) < 0){
              b_flowLeaveData = "0.0";
            }
				
					} catch (Exception e) {
						kqLog.info("GetKQReportCmd:leaveData" + leaveData + ":leavebackData:" + leavebackData + ":" + e);
					}

					//考虑下冻结的数据
					if (b_flowLeaveData.length() > 0) {
						flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
					} else {
						flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData, 0.0) - Util.getDoubleValue(leavebackData, 0.0)));
					}
					data.put(flowType, flowLeaveData);
				}

				data.put("resourceId", id);
				data.put("kqdate", kqdate);
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
		}
		return retmap;
	}


	private String getUnitType(String unitType, User user){
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
		return unitTypeName;
	}

	private Map<String,Object> getChildColumnsInfo(String parentid, User user){
		Map<String,Object> returnMap = new HashMap<>();
		List<Object> lsChildColumns = new ArrayList<>();
		Map column = null;
		int sumChildColumnWidth = 0;
		if(parentid.equals("leave")){
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
        	if(!kqReportFieldComInfo.getReportType().equals("daily"))continue;
          column = new HashMap();
          column.put("title", SystemEnv.getHtmlLabelNames(kqReportFieldComInfo.getFieldlabel(), user.getLanguage()));
					column.put("unit", KQReportBiz.getUnitType(kqReportFieldComInfo, user));
          column.put("width", Util.getIntValue(kqReportFieldComInfo.getWidth()));
          column.put("dataIndex", kqReportFieldComInfo.getFieldname());
          column.put("key", kqReportFieldComInfo.getFieldname());
          column.put("rowSpan", 2);
          column.put("colSpan", 1);
          column.put("isdaily", kqReportFieldComInfo.getReportType1().equals("daily")?"1":"0");
          sumChildColumnWidth+=Util.getIntValue(kqReportFieldComInfo.getWidth());
          lsChildColumns.add(column);
        }
      }
    }
		returnMap.put("childColumns",lsChildColumns);
		returnMap.put("sumChildColumnWidth",sumChildColumnWidth);
		return returnMap;
	}

	public Map<String, Object> getSignDetailInfo(String resourceId, String kqDate){
		Map<String, Object> data = new HashMap<>();
		Map<String,Object> signStatusInfo = null;
		RecordSet rs = new RecordSet();
		String sql = "";
		KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
		try{
			sql = " select kqdate,resourceid,serialid,serialnumber,workbegindate,workbegintime, " +
							" workenddate,workendtime,workmins,signindate,signintime,signoutdate,signouttime, \n" +
							" attendanceMins,belatemins,graveBeLateMins,leaveearlymins,graveLeaveEarlyMins,absenteeismmins,forgotcheckMins,forgotBeginWorkCheckMins," +
							" leaveMins,leaveInfo,evectionMins,outMins,signinid,signoutid \n" +
							" from kq_format_detail b \n" +
							" where resourceid = " + resourceId + " and kqdate ='" + kqDate + "' \n" +
							" order by serialnumber \n";
			rs.execute(sql);
			while (rs.next()) {
				String resourceid = Util.null2String(rs.getString("resourceid"));
				String kqdate = Util.null2String(rs.getString("kqdate"));
				String serialid = Util.null2String(rs.getString("serialid"));
				int serialnumber  = rs.getInt("serialnumber")+1;
				String workbegindate = Util.null2String(rs.getString("workbegindate")).trim();
				String workbegintime = Util.null2String(rs.getString("workbegintime")).trim();
				String workenddate = Util.null2String(rs.getString("workenddate")).trim();
				String workendtime = Util.null2String(rs.getString("workendtime")).trim();
				int workMins = rs.getInt("workMins");
				String signintime = Util.null2String(rs.getString("signintime")).trim();
				String signouttime = Util.null2String(rs.getString("signouttime")).trim();
				int attendanceMins = rs.getInt("attendanceMins");
				String beLateMins = Util.null2String(rs.getString("beLateMins")).trim();
				String graveBeLateMins = Util.null2String(rs.getString("graveBeLateMins")).trim();
				String leaveEarlyMins= Util.null2String(rs.getString("leaveEarlyMins")).trim();
				String graveLeaveEarlyMins= Util.null2String(rs.getString("graveLeaveEarlyMins")).trim();
				String absenteeismMins= Util.null2String(rs.getString("absenteeismMins")).trim();
				String forgotCheckMins = Util.null2String(rs.getString("forgotcheckMins")).trim();
				String forgotBeginWorkCheckMins = Util.null2String(rs.getString("forgotBeginWorkCheckMins")).trim();
        String signinid = Util.null2String(rs.getString("signinid")).trim();
        String signoutid = Util.null2String(rs.getString("signoutid")).trim();
				int leaveMins = rs.getInt("leaveMins");
				String leaveInfo = Util.null2String(rs.getString("leaveInfo"));
				int evectionMins = rs.getInt("evectionMins");
				int outMins = rs.getInt("outMins");


				if(serialid.length()>0){
					if (workbegintime.length() > 0) {
						signStatusInfo = new HashMap();
						signStatusInfo.put("workdate",workbegindate);
						signStatusInfo.put("worktime",workbegintime);
						signStatusInfo.put("beLateMins",beLateMins);
            signStatusInfo.put("forgotBeginWorkCheckMins",forgotBeginWorkCheckMins);
						signStatusInfo.put("graveBeLateMins",graveBeLateMins);
						signStatusInfo.put("absenteeismMins",absenteeismMins);
						signStatusInfo.put("leaveMins",leaveMins);
						signStatusInfo.put("leaveInfo",leaveInfo);
						signStatusInfo.put("evectionMins",evectionMins);
						signStatusInfo.put("outMins",outMins);

						data.put("signintime"+serialnumber, signintime.length()==0?SystemEnv.getHtmlLabelName(25994, user.getLanguage()):signintime);
						data.put("signinstatus"+serialnumber, KQReportBiz.getSignStatus(signStatusInfo,user,"on"));
					}

					if (workendtime.length() > 0) {
						signStatusInfo = new HashMap();
						signStatusInfo.put("workdate",workenddate);
						signStatusInfo.put("worktime",kqTimesArrayComInfo.turn48to24Time(workendtime));
						signStatusInfo.put("leaveEarlyMins",leaveEarlyMins);
						signStatusInfo.put("graveLeaveEarlyMins",graveLeaveEarlyMins);
						signStatusInfo.put("forgotCheckMins",forgotCheckMins);
						signStatusInfo.put("forgotBeginWorkCheckMins",forgotBeginWorkCheckMins);
						signStatusInfo.put("absenteeismMins",absenteeismMins);
						signStatusInfo.put("leaveMins",leaveMins);
						signStatusInfo.put("leaveInfo",leaveInfo);
						signStatusInfo.put("evectionMins",evectionMins);
						signStatusInfo.put("outMins",outMins);

						data.put("signouttime"+serialnumber, signouttime.length()==0?SystemEnv.getHtmlLabelName(25994, user.getLanguage()):signouttime);
						data.put("signoutstatus"+serialnumber, KQReportBiz.getSignStatus(signStatusInfo,user,"off"));
					}
				}else{
					if(workMins>0){
					//弹性工时打卡时间取自签到签退数据
					}
          signStatusInfo = new HashMap();
          signStatusInfo.put("leaveMins",leaveMins);
          signStatusInfo.put("leaveInfo",leaveInfo);
          signStatusInfo.put("evectionMins",evectionMins);
          signStatusInfo.put("outMins",outMins);

          if(signinid.length() > 0){
            data.put("signintime"+serialnumber, signintime.length()==0?SystemEnv.getHtmlLabelName(25994, user.getLanguage()):signintime);
            data.put("signinstatus"+serialnumber, KQReportBiz.getSignStatus(signStatusInfo,user,"on"));
            if(signoutid.length() > 0){
              data.put("signouttime"+serialnumber, signouttime.length()==0?SystemEnv.getHtmlLabelName(25994, user.getLanguage()):signouttime);
              data.put("signoutstatus"+serialnumber, KQReportBiz.getSignStatus(signStatusInfo,user,"off"));
            }
          }else{
            data.put("signinstatus"+serialnumber, KQReportBiz.getSignStatus(signStatusInfo,user,"on"));
          }
				}
			}
		}catch (Exception e){
			writeLog(e);
		}
		return data;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

}
