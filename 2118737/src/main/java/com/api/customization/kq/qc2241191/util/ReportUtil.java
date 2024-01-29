package com.api.customization.kq.qc2241191.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.*;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.util.PageUidFactory;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.resource.ResourceComInfo;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportUtil {



    public List<Object> getReport(Map<String, Object> params, User user,String fromDate,String toDate){
        List<Object> datas = new ArrayList();
        RecordSet rs = new RecordSet();
        String sql = "";

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

            String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));
            String departmentId = Util.null2String(jsonObj.get("departmentId"));
            String resourceId = Util.null2String(jsonObj.get("resourceId"));
            String allLevel = Util.null2String(jsonObj.get("allLevel"));
            String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));
            String viewScope = Util.null2String(jsonObj.get("viewScope"));
            String isFromMyAttendance = Util.null2String(jsonObj.get("isFromMyAttendance"));//是否是来自我的考勤的请求，如果是，不加载考勤报表权限共享的限制，不然我的考勤会提示无权限
            int pageIndex = 0 /*Util.getIntValue(Util.null2String(jsonObj.get("pageIndex")), 1)*/;
            int pageSize = 0 /*KQReportBiz.getPageSize(Util.null2String(jsonObj.get("pageSize")),pageUid,user.getUID())*/;
            int count = 0;

            String rightSql = kqReportBiz.getReportRight("1",""+user.getUID(),"a");
            if(isFromMyAttendance.equals("1")){
                rightSql = "";
            }

            List<Map<String, Object>> leaveRules = kqLeaveRulesBiz.getAllLeaveRules();
            Map<String,Object> data = null;
            KQReportFieldComInfo kqReportFieldComInfo = new KQReportFieldComInfo();

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
                    " sum(b.workdays) as workdays,sum(b.attendancedays) as attendancedays," +
                    " sum(b.forgotCheck)+"+forgotBeginWorkCheck_field+" as forgotCheck "+(definedFieldSum.length()>0?","+definedFieldSum+"":"");

            if(rs.getDBType().equals("oracle")){
                backFields = 	"/*+ index(kq_format_total IDX_KQ_FORMAT_TOTAL_KQDATE) */ "+backFields;
            }
            String sqlFrom = " from hrmresource a, kq_format_total b where a.id= b.resourceid and b.kqdate >='"+fromDate+"' and b.kqdate <='"+toDate+"'";
            String sqlWhere = rightSql;
            String groupBy = " group by a.id,a.lastname,a.workcode,a.dsporder,b.resourceid,a.subcompanyid1,a.departmentid,a.jobtitle ";
            if(subCompanyId.length()>0){
                sqlWhere +=" and b.subcompanyid in("+subCompanyId+") ";
            }

            if(departmentId.length()>0){
                sqlWhere +=" and b.departmentid in("+departmentId+") ";
            }

            if(resourceId.length()>0){
                sqlWhere +=" and b.resourceid in("+resourceId+") ";
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

            sql = " select count(*) as c from ( select 1 as c "+sqlFrom+sqlWhere+groupBy+") t";
            rs.execute(sql);
            if (rs.next()){
                count = rs.getInt("c");
            }

            String orderBy = " order by t.dsporder asc, t.lastname asc ";
            String descOrderBy = " order by t.dsporder desc, t.lastname desc ";

            sql = backFields + sqlFrom  + sqlWhere + groupBy;

            if (pageIndex > 0 && pageSize > 0) {
                if (rs.getDBType().equals("oracle")) {
                    sql = " select * from (select " + sql+") t "+orderBy;
                    sql = "select * from ( select row_.*, rownum rownum_ from ( " + sql + " ) row_ where rownum <= "
                            + (pageIndex * pageSize) + ") where rownum_ > " + ((pageIndex - 1) * pageSize);
                } else if (rs.getDBType().equals("mysql")) {
                    sql = " select * from (select " + sql+") t "+orderBy;
                    sql = "select t1.* from (" + sql + ") t1 limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
                } else {
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
            Map<String,Object> flowData = kqReportBiz.getFlowData(params,user);
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

                    //如果是absenteeism（旷工）跳出
                    if (fieldName.equals("absenteeism"))continue;
                    //如果是absenteeismMins（旷工时长）跳出
                    if (fieldName.equals("absenteeismMins"))continue;
                    //如果是attendanceMins（出勤分钟）跳出
                    if (fieldName.equals("attendanceMins"))continue;
                    //如果是attendanceSerial（出勤序列号）跳出
                    if (fieldName.equals("attendanceSerial"))continue;
                    //如果是beLate（迟到）跳出
                    if (fieldName.equals("beLate"))continue;
                    //如果是beLateMins（迟到分钟）跳出
                    if (fieldName.equals("beLateMins"))continue;
                    //如果是graveBeLate（严重迟到）跳出
                    if (fieldName.equals("graveBeLate"))continue;
                    //如果是graveBeLateMins（严重迟到分钟）跳出
                    if (fieldName.equals("graveBeLateMins"))continue;
                    //如果是graveLeaveEarly（严重早退）跳出
                    if (fieldName.equals("graveLeaveEarly"))continue;
                    //如果是graveLeaveEarlyMins（严重早退分钟）跳出
                    if (fieldName.equals("graveLeaveEarlyMins"))continue;
                    //如果是holidayOvertime_4leave 跳出
                    if (fieldName.equals("holidayOvertime_4leave"))continue;
                    //如果是holidayOvertime_nonleave 跳出
                    if (fieldName.equals("holidayOvertime_nonleave"))continue;
                    //如果是leaveEarlyMins（早退分钟） 跳出
                    if (fieldName.equals("leaveEarlyMins"))continue;
                    //如果是leaveEearly（早退） 跳出
                    if (fieldName.equals("leaveEearly"))continue;
                    //如果是overtimeTotal（加班总计） 跳出
                    if (fieldName.equals("overtimeTotal"))continue;
                    //如果是restDayOvertime_4leave（休息日加班） 跳出
                    if (fieldName.equals("restDayOvertime_4leave"))continue;
                    //如果是restDayOvertime_nonleave（休息日加班） 跳出
                    if (fieldName.equals("restDayOvertime_nonleave"))continue;
                    //如果是workingDayOvertime_4leave（工作日加班） 跳出
                    if (fieldName.equals("workingDayOvertime_4leave"))continue;
                    //如果是workingDayOvertime_nonleave（工作日加班） 跳出
                    if (fieldName.equals("workingDayOvertime_nonleave"))continue;
                    //如果是workmins（工作时长） 跳出
                    if (fieldName.equals("workmins"))continue;
                    //如果是forgotCheck 跳出
                    if (fieldName.equals("forgotCheck"))continue;
                    //如果是signmins 跳出
                    if (fieldName.equals("signmins"))continue;
                    //如果是leave 跳出
                    if (fieldName.equals("leave"))continue;
                    //如果是jobtitle 跳出
                    if (fieldName.equals("jobtitle"))continue;
                    //如果是workcode 跳出
                    if (fieldName.equals("workcode"))continue;

                    if(fieldName.equals("subcompany")){
                        String tmpSubcompanyId = Util.null2String(rs.getString("subcompanyid"));
                        if(tmpSubcompanyId.length()==0){
                            tmpSubcompanyId =  Util.null2String(resourceComInfo.getSubCompanyID(id));
                        }
                        data.put("subcompanyId",tmpSubcompanyId);
                        fieldValue = subCompanyComInfo.getSubCompanyname(tmpSubcompanyId);
                    }else if(fieldName.equals("department")){
                        String tmpDepartmentId = Util.null2String(rs.getString("departmentid"));
                        if(tmpDepartmentId.length()==0){
                            tmpDepartmentId =  Util.null2String(resourceComInfo.getDepartmentID(id));
                        }
                        data.put("departmentId",tmpDepartmentId);
                        fieldValue = departmentComInfo.getDepartmentname(tmpDepartmentId);
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
                    } else {
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
                        new BaseBean().writeLog("GetKQReportCmd:leaveData"+leaveData+":leavebackData:"+leavebackData+":"+e);
                    }

                    //考虑下冻结的数据
                    if(b_flowLeaveData.length() > 0){
                        flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
                    }else{
                        flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData,0.0)-Util.getDoubleValue(leavebackData,0.0)));
                    }
                    data.put(flowType,flowLeaveData);
                }
                //给每条数据生成考勤年月
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                String date = sdf.format(sdf.parse(fromDate));
                data.put("attendancedate",date);
                datas.add(data);
            }
        }catch (Exception e){
            new BaseBean().writeLog(e);
        }
        return datas;
    }
}
