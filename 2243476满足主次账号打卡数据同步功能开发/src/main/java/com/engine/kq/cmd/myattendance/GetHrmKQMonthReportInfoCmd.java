package com.engine.kq.cmd.myattendance;

import com.alibaba.fastjson.JSON;
import com.api.hrm.bean.KQReportType;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.wfset.util.KQSignUtil;
import com.engine.kq.wfset.util.SplitActionUtil;
import com.engine.kq.wfset.util.SplitSelectSet;
import org.apache.commons.lang3.StringUtils;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.util.*;

/**
 * 前台--人事--我的考勤
 */
public class GetHrmKQMonthReportInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetHrmKQMonthReportInfoCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    private static boolean isIndirectSuperId(String id,String checkSuperId,ResourceComInfo info){

        int loopBreakTimes = 20 ;
        for(String loopId = id;
            isRightResourceId(loopId) && (loopBreakTimes--)>0 ;
            loopId = info.getManagerID(loopId)){
            if(isDirectSuperId(loopId,checkSuperId,info)) return true ;
        }
        return false ;
    }

    private static boolean isDirectSuperId(String id,String checkSuperId,ResourceComInfo info){
        String superId = Util.null2String(info.getManagerID(id)) ;
        return isRightResourceId(superId) && superId.equals(checkSuperId) ;
    }

    private static boolean isRightResourceId(String id){
        return StringUtils.isNotBlank(id) && !"0".equals(id) ;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?
            /**
             * 若未指定查看人员，则默认为当前登录人员
             */

            //==zj 如果来自移动端就显示为当前唯一生效考勤组的信息
            if ("1".equals(Util.null2String(params.get("ismobile")))){
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
                    }
                }
            }

            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            if (subCompanyId == 0 || departmentId == 0) {
                subCompanyId = Util.getIntValue(resourceComInfo.getSubCompanyID(resourceId), 0);
                departmentId = Util.getIntValue(resourceComInfo.getDepartmentID(resourceId), 0);
            }
            String lastName = resourceComInfo.getResourcename(resourceId);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513535, user.getLanguage()));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(515563, user.getLanguage()).replace("{name}", lastName));
            }
            /**
             * 判断是否有权限查看
             */
            //==zj 这里多个判断，如果查询账号为当前登录账号的子账号，解除权限。
            KqUtil kqUtil = new KqUtil();
            new BaseBean().writeLog("==zj==(权限判断)" + params.get("ismobile"));
            Boolean result = false;
            if ("1".equals(Util.null2String(params.get("ismobile")))){

            }else {
                result =   !kqUtil.isChildAccount(user.getUID(),resourceId);
            }
            if (!resourceId.equals("" + user.getUID()) && !isIndirectSuperId(resourceId, user.getUID() + "", resourceComInfo) && !HrmUserVarify.checkUserRight("HrmResource:Absense", user, departmentId) && result) {
                resultMap.put("status", "-1");
                resultMap.put("hasRight", false);
                return resultMap;
            }

            /**
             * 获取指定日期范围的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            /**判断是否在考勤日历上显示签到签退数据*/
            boolean showSign = true;
            String settingSql = "select * from KQ_SETTINGS where main_key='showSignOnCalendar'";
            RecordSet rs = new RecordSet();
            rs.executeQuery(settingSql);
            if (rs.next()) {
                String main_val = rs.getString("main_val");
                showSign = main_val.equals("1");
            }

            //签到签退明细
            Map<String,Object> signInfoMap = getSignInfo();
            List<Map<String, Object>> signInfoList = new ArrayList<Map<String, Object>>();
            //休息明细(统计工作日)
            List<String> isNotRestList = new ArrayList<String>();
            //迟到明细
            List<String> beLateList = new ArrayList<String>();
            //早退明细
            List<String> leaveEarlyList = new ArrayList<String>();
            //旷工明细
            List<String> absentList = new ArrayList<String>();
            //漏签明细
            List<String> noSignList = new ArrayList<String>();
            //请假明细
            Map<String, Object> leaveMap = getFlowData(KqSplitFlowTypeEnum.LEAVE.getFlowtype());
            //出差明细
            Map<String, Object> evectionMap = getFlowData(KqSplitFlowTypeEnum.EVECTION.getFlowtype());
            //公出明细
            Map<String, Object> outDaysMap = getFlowData(KqSplitFlowTypeEnum.OUT.getFlowtype());
            //加班明细
            Map<String, Object> overtimeMap = getFlowData(KqSplitFlowTypeEnum.OVERTIME.getFlowtype());
            //调配工作日
            List<String> workdayList = new ArrayList<String>();
            //调配休息日、公众假日
            List<String> holidayList = new ArrayList<String>();

            //获取节假日
            List<Map<String, Object>> tempList = KQHolidaySetBiz.getHolidaySetListByScope(resourceId, fromDate, toDate);
            for (Map<String, Object> tempMap : tempList) {
                String date = Util.null2String(tempMap.get("date"));
                int changeType = Util.getIntValue((String) tempMap.get("type"), 1);
                if (changeType == 1 || changeType == 3) {
                    holidayList.add(date);
                } else if (changeType == 2) {
                    workdayList.add(date);
                }
            }

            String sql = "select * from kq_format_detail where resourceId=" + resourceId + " and KQDate>='" + fromDate + "' and KQDate<='" + toDate + "' order by kqDate";
            RecordSet recordSet = new RecordSet();
            RecordSet detailRs = new RecordSet();
            recordSet.executeQuery(sql);
            KQWorkTime kqWorkTime = new KQWorkTime();
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");
                String serialId = recordSet.getString("serialId");

                WorkTimeEntity workTime = kqWorkTime.getWorkTime(resourceId,kqDate);
                if (!isNotRestList.contains(kqDate) && !serialId.equals("")) {
                    isNotRestList.add(kqDate);
                }else if(Util.null2String(workTime.getKQType()).equals("3")){
                    if(workTime!=null&&Util.getIntValue(workTime.getWorkMins())>0) {
                        isNotRestList.add(kqDate);
//                        groupid = Util.null2String(workTime.getGroupId());
//                        signstart = Util.null2String(workTime.getSignStart());//签到开始时间
//                        workMins = Util.getIntValue(Util.null2String(workTime.getWorkMins()));//工作时长
                    }
                }

                int beLate = recordSet.getInt("beLateMins");//迟到
                int graveBeLate = recordSet.getInt("graveBeLateMins");//严重迟到
                int leaveEarly = recordSet.getInt("leaveEarlyMins");//早退
                int graveLeaveEarly = recordSet.getInt("graveLeaveEarlyMins");//严重早退
                int absenteeism = recordSet.getInt("absenteeismMins");//旷工
                int forgotCheck = recordSet.getInt("forgotCheckMins");//下班打卡漏签
                int forgotBeginWorkCheckMins = recordSet.getInt("forgotBeginWorkCheckMins");//上班打卡漏签
                if (absenteeism > 0) {//旷工
                    absentList.add(kqDate);
                } else {
                    if (beLate > 0 || graveBeLate > 0) { //迟到
                        beLateList.add(kqDate);
                    }
                    if (leaveEarly > 0 || graveLeaveEarly > 0) {//早退
                        leaveEarlyList.add(kqDate);
                    }
                    if (forgotCheck > 0 || forgotBeginWorkCheckMins > 0) {//漏签
                        noSignList.add(kqDate);
                    }
                }
            }

            Map<String, Object> year = new HashMap<String, Object>();
            Map<String, Object> month = new HashMap<String, Object>();
            Map<String, Object> day = new HashMap<String, Object>();
            List<Object> types = new ArrayList<Object>();
            List<Map<String, Object>> lsWorkflow = new ArrayList<Map<String, Object>>();
            Map<String, Object> workflow = new HashMap<String, Object>();

            boolean isEnd = false;
            for (String date = fromDate; !isEnd; ) {
                String tmpMonth = "" + Util.getIntValue(date.split("-")[1]);
                String tmpDay = "" + Util.getIntValue(date.split("-")[2]);
                if (date.compareTo(toDate) >= 0) {
                    isEnd = true;
                }
                /*如果是按年份查看*/
                if (type.equals("1")) {
                    if (!year.containsKey(tmpMonth)) {
                        month = new HashMap<String, Object>();
                        year.put(tmpMonth, month);
                    }
                }

                day = new HashMap<String, Object>();
                month.put(tmpDay, day);
                if (isNotRestList.contains(date)) {
                    day.put("isWorkDay", true);
                } else {
                    day.put("isWorkDay", false);
                }
                if (type.equals("1")) {
                    day.put("tip", "");
                }
                if (holidayList.contains(date)) {
                    day.put("tip", SystemEnv.getHtmlLabelName(125806, user.getLanguage()));
                    day.put("isWorkDay", false);
                } else if (workdayList.contains(date)) {
                    day.put("tip", SystemEnv.getHtmlLabelName(125807, user.getLanguage()));
                    day.put("isWorkDay", true);
                }
                day.put("date", date);
                types = new ArrayList<Object>();
                if (type.equals("2")) {
                    day.put("types", types);
                }
                lsWorkflow = new ArrayList<Map<String, Object>>();
                /*只有月视图才显示流程数据、只有月视图才把签到签退信息显示在日历上*/
                if (type.equals("2") && showSign) {
                    signInfoList = (List)signInfoMap.get(date);
                    if (signInfoList != null && signInfoList.size() > 0) {
                        day.put("signInfo", signInfoList);
                    }
                }
                /*只有月视图才显示流程数据*/
                if (type.equals("2")) {
                    day.put("workflow", lsWorkflow);
                }
                boolean isNormal = true;
                //迟到明细
                if (beLateList.contains(date)) {
                    if (!types.contains(KQReportType.BELATE)) {
                        types.add(KQReportType.BELATE);
                        isNormal = false;
                    }
                }

                //旷工明细
                if (absentList.contains(date)) {
                    if (!types.contains(KQReportType.ABSENT)) {
                        types.add(KQReportType.ABSENT);
                        isNormal = false;
                    }
                }

                //早退明细
                if (leaveEarlyList.contains(date)) {
                    if (!types.contains(KQReportType.LEAVEEARLY)) {
                        types.add(KQReportType.LEAVEEARLY);
                        isNormal = false;
                    }
                }

                //漏签明细
                if (noSignList.contains(date)) {
                    if (!types.contains(KQReportType.NOSIGN)) {
                        types.add(KQReportType.NOSIGN);
                        isNormal = false;
                    }
                }

                //请假明细
                if (leaveMap.containsKey(resourceId + "|" + date)) {
                    if (!types.contains(KQReportType.LEAVE)) {
                        types.add(KQReportType.LEAVE);
                        isNormal = false;
                    }
                    tempList = (List<Map<String, Object>>) leaveMap.get(resourceId + "|" + date);
                    for (Map<String, Object> tempMap : tempList) {
                        lsWorkflow.add(tempMap);
                    }
                }

                //加班明细
                if (!Util.null2String(overtimeMap.get(resourceId + "|" + date)).equals("")) {
                    if (!types.contains(KQReportType.OVERTIME)) {
                        types.add(KQReportType.OVERTIME);
                    }
                    tempList = (List<Map<String, Object>>) overtimeMap.get(resourceId + "|" + date);
                    for (Map<String, Object> tempMap : tempList) {
                        lsWorkflow.add(tempMap);
                    }
                }

                //外出明细
                if (evectionMap.containsKey(resourceId + "|" + date)) {
                    if (!types.contains(KQReportType.EVECTION)) {
                        types.add(KQReportType.EVECTION);
                        isNormal = false;
                    }
                    tempList = (List<Map<String, Object>>) evectionMap.get(resourceId + "|" + date);
                    for (Map<String, Object> tempMap : tempList) {
                        lsWorkflow.add(tempMap);
                    }
                }

                //公出明细
                if (outDaysMap.containsKey(resourceId + "|" + date)) {
                    if (!types.contains(KQReportType.OUTDAYS)) {
                        types.add(KQReportType.OUTDAYS);
                        isNormal = false;
                    }
                    tempList = (List<Map<String, Object>>) outDaysMap.get(resourceId + "|" + date);
                    for (Map<String, Object> tempMap : tempList) {
                        lsWorkflow.add(tempMap);
                    }
                }

                if (isNotRestList.contains(date) && DateUtil.isInDateRange(date, fromDate, nowdate)) {
                    if (isNormal) {
                        types.add(KQReportType.NORMAL);
                    }
                }

                if (type.equals("1")) {
                    day.put("type", types.size() > 0 ? types.get(0) : "NORMAL");
                }
                date = DateUtil.getDate(date, 1);
            }
            if (type.equals("1")) {
                resultMap.put("result", year);
            } else {
                resultMap.put("result", month);
            }
            resultMap.put("status", "1");
        } catch (Exception e) {
            resultMap.put("api_status", false);
            resultMap.put("api_errormsg", e.getMessage());
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取指定人员在指定时间范围内的流程集合
     *
     * @return
     */
    public Map<String, Object> getFlowData(int flowType) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            /**获取销假*/
            List<String> leaveBackList = getLeaveDate();

            KQExitRulesComInfo exitRulesComInfo = new KQExitRulesComInfo();
            KQTravelRulesComInfo travelRulesComInfo = new KQTravelRulesComInfo();
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            KQTimeSelectionComInfo kqTimeSelectionComInfo = new KQTimeSelectionComInfo();
            KQWorkTime kqWorkTime = new KQWorkTime();

            boolean showOnHoliday = true;//非工作日是否显示流程（出差、公出、请假）

            String timeselection = "1";
            String selectiontype = "";
            String typeName = "";
            KQReportType workflowType = null;
            switch (flowType) {
                case 3://加班
                    typeName = SystemEnv.getHtmlLabelName(6151, user.getLanguage());
                    workflowType = KQReportType.OVERTIME;
                    selectiontype = ""+KqSplitFlowTypeEnum.OVERTIME.getFlowtype();
                    timeselection = KQOvertimeRulesBiz.getTimeselection();
                    break;
                case 2://公出
                    typeName = SystemEnv.getHtmlLabelName(24058, user.getLanguage());
                    workflowType = KQReportType.OUTDAYS;
                    showOnHoliday = exitRulesComInfo.getComputingMode("1").equals("2");
                    selectiontype = ""+KqSplitFlowTypeEnum.OUT.getFlowtype();
                    timeselection = KQExitRulesBiz.getTimeselection();
                    break;
                case 1://出差
                    typeName = SystemEnv.getHtmlLabelName(20084, user.getLanguage());
                    workflowType = KQReportType.EVECTION;
                    showOnHoliday = travelRulesComInfo.getComputingMode("1").equals("2");
                    selectiontype = ""+KqSplitFlowTypeEnum.EVECTION.getFlowtype();
                    timeselection = KQTravelRulesBiz.getTimeselection();
                    break;
                case 0://请假
                    typeName = SystemEnv.getHtmlLabelName(670, user.getLanguage());
                    workflowType = KQReportType.LEAVE;
                    selectiontype = ""+KqSplitFlowTypeEnum.LEAVE.getFlowtype();
                    break;
                default:
                    break;
            }

            /**
             * 获取数据集合
             */
            List<Map<String, Object>> workflowList = new ArrayList<Map<String, Object>>();
            Map<String, Object> workflowMap = new HashMap<String, Object>();

            Map<String, Object> paramsMap = new HashMap<String, Object>();
            paramsMap.put("typeselect", "6");
            paramsMap.put("tabKey", "1");
            paramsMap.put("kqtype", "" + flowType);
            paramsMap.put("resourceId", resourceId);
            paramsMap.put("fromDate", fromDate);
            paramsMap.put("toDate", toDate);
            paramsMap.put("isMyKQ", "1");
            paramsMap.put("isNoAccount","1");

            KQAttFlowSetBiz kqAttFlowSetBiz = new KQAttFlowSetBiz();
            Map<String, String> sqlMap = kqAttFlowSetBiz.getFLowSql(paramsMap, user);
            String sqlFrom = sqlMap.get("from");
            String sqlWhere = sqlMap.get("where");

            String sql = "select * " + sqlFrom + sqlWhere;
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String requestId = recordSet.getString("requestId");
                double duration = Util.getDoubleValue(recordSet.getString("duration"), 0.00);
                double backDuration = 0.00;
                int durationrule = recordSet.getInt("durationrule");
                String fromDateTemp = recordSet.getString("fromDate");
                String fromTimeTemp = recordSet.getString("fromTime");
                String toDateTemp = recordSet.getString("toDate");
                String toTimeTemp = recordSet.getString("toTime");
                String leaveRulesId = "";
                String filterholidays = "";
                String count = String.format("%.2f", duration) + "(" + ((durationrule == 3||durationrule == 5||durationrule == 6) ? SystemEnv.getHtmlLabelName(391, user.getLanguage()) : SystemEnv.getHtmlLabelName(1925, user.getLanguage())) + ")";
                if(flowType == 3){
                  count = String.format("%.2f", duration) + "(" + ((durationrule == 3||durationrule == 5||durationrule == 6) ? SystemEnv.getHtmlLabelName(391, user.getLanguage()) : SystemEnv.getHtmlLabelName(1925, user.getLanguage())) + ")";
                }
                if (flowType == 0) {
                    backDuration = Util.getDoubleValue(recordSet.getString("backDuraion"), 0.00);
                    if (backDuration > 0) {
                        count = String.format("%.2f", duration) + "(" + (SystemEnv.getHtmlLabelName(24473, user.getLanguage()) + ":" + String.format("%.2f", backDuration)) + ")" + ((durationrule == 3||durationrule == 5||durationrule == 6) ? SystemEnv.getHtmlLabelName(391, user.getLanguage()) : SystemEnv.getHtmlLabelName(1925, user.getLanguage()));
                    }
                    leaveRulesId = recordSet.getString("newleavetype");
                    String computingMode = rulesComInfo.getComputingMode(leaveRulesId);
                    if("2".equalsIgnoreCase(computingMode)){
                      //按照自然日计算
                      showOnHoliday = true;
                      filterholidays= rulesComInfo.getFilterHolidays(leaveRulesId);
                    } else {

                        showOnHoliday = false;
                    }
                    typeName = Util.formatMultiLang(rulesComInfo.getLeaveName(leaveRulesId), "" + user.getLanguage());
                }
                String start = fromDateTemp + " " + fromTimeTemp;
                String end = toDateTemp + " " + toTimeTemp;
                if (2 == durationrule || 4 == durationrule) {
                    if (2 == durationrule) {
                      if(leaveRulesId.length() > 0){
                        timeselection = rulesComInfo.getTimeSelection(leaveRulesId);
                      }
                      if("1".equalsIgnoreCase(timeselection)){
                        Map<String,String> half_map = kqTimeSelectionComInfo.getTimeselections(selectiontype,leaveRulesId,durationrule+"");
                        String cus_am = SystemEnv.getHtmlLabelName(16689, user.getLanguage());
                        String cus_pm = SystemEnv.getHtmlLabelName(16690, user.getLanguage());
                        if(half_map != null && !half_map.isEmpty()){
                          cus_am = Util.null2String(half_map.get("half_on"));
                          cus_pm = Util.null2String(half_map.get("half_off"));
                        }
                        if (fromTimeTemp.equalsIgnoreCase(SplitSelectSet.forenoon_start)) {
                          start = fromDateTemp + " " + cus_am;
                        } else if (fromTimeTemp.equalsIgnoreCase(SplitSelectSet.forenoon_end)) {
                          start = fromDateTemp + " " + cus_pm;
                        }

                        if (toTimeTemp.equalsIgnoreCase(SplitSelectSet.afternoon_start)) {
                          end = toDateTemp + " " + cus_am;
                        } else if (toTimeTemp.equalsIgnoreCase(SplitSelectSet.afternoon_end)) {
                          end = toDateTemp + " " + cus_pm;
                        }
                      }
                    }
                    if (4 == durationrule) {
                        if (fromTimeTemp.equalsIgnoreCase(SplitSelectSet.forenoon_start)) {
                            start = fromDateTemp + " " + SystemEnv.getHtmlLabelName(390728, user.getLanguage());
                        }

                        if (toTimeTemp.equalsIgnoreCase(SplitSelectSet.afternoon_end)) {
                            end = toDateTemp + " " + SystemEnv.getHtmlLabelName(390728, user.getLanguage());
                        }
                    }
                }

                boolean isEnd = false;
                for (String date = fromDateTemp; !isEnd; ) {
                    if (date.equals(toDateTemp)) {
                        isEnd = true;
                    }
					boolean isWorkDay = kqWorkTime.isWorkDay(resourceId, date);
					if(!isWorkDay && !showOnHoliday) {
                        date = DateUtil.getDate(date, 1);
                        continue;
                    }
                    if (!leaveBackList.contains(resourceId + "|" + date + "|" + requestId)) {
                        workflowList = (ArrayList<Map<String, Object>>) resultMap.get(resourceId + "|" + date);
                        if (null == workflowList) {
                            workflowList = new ArrayList<Map<String, Object>>();
                        }
                        workflowMap = new HashMap<String, Object>();
                        workflowMap.put("count", count);
                        workflowMap.put("start", start);
                        workflowMap.put("end", end);
                        workflowMap.put("type", typeName);
                        workflowMap.put("workflowtype", workflowType);
                        workflowMap.put("requestId", requestId);
                        workflowMap.put("title", "<a href=javaScript:openFullWindowHaveBarForWFList('/workflow/request/ViewRequestForwardSPA.jsp?requestid=" + requestId + "&ismonitor=1&isovertime=0'," + requestId + ");>" + typeName + "</a>");
                        workflowList.add(workflowMap);

                        if (!(!isWorkDay && !showOnHoliday)) {
                          SplitActionUtil splitActionUtil = new SplitActionUtil();
                          boolean is_filterholidays = splitActionUtil.check_filterholidays(filterholidays, resourceId, date, 1);
                          if(!is_filterholidays){
                            resultMap.put(resourceId + "|" + date, workflowList);
                          }
                        }
                    }

                    date = DateUtil.getDate(date, 1);
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取请完假之后又销掉的日期
     *
     * @return
     */
    private List<String> getLeaveDate() {
        List<String> resultList = new ArrayList<String>();
        try {
            String nowdate = DateUtil.getCurrentDate();
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);//指定查看的人员的所属分部
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);//指定查看的人员的所属部门
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID
            String status = Util.null2String(params.get("status"));//?

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }

            String leaveBackTableName = KqSplitFlowTypeEnum.LEAVEBACK.getTablename();
            String leaveTableName = KqSplitFlowTypeEnum.LEAVE.getTablename();
            String sql = " select requestId,resourceId,belongDate,duration,durationRule," +
                    " (select sum(duration) from " + leaveBackTableName + " where " + leaveBackTableName + ".leavebackrequestid=" + leaveTableName + ".requestid " +
                    " and " + leaveBackTableName + ".newleavetype=" + leaveTableName + ".newleavetype " +
                    " and " + leaveBackTableName + ".belongdate=" + leaveTableName + ".belongDate ) as backDuration " +
                    " from " + leaveTableName + " where 1=1 ";
            if (!resourceId.equals("")) {
                sql += " and " + leaveTableName + ".resourceId=" + resourceId;
            }
            if (fromDate.length() > 0 && toDate.length() > 0) {
                sql += " and " + leaveTableName + ".belongdate between'" + fromDate + "' and '" + toDate + "' ";
            }
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String requestId = recordSet.getString("requestId");
                String belongDate = recordSet.getString("belongDate");
                double duration = Util.getDoubleValue(recordSet.getString("duration"), 0.00);
                if (duration <= 0) {
                    continue;
                }
                double backDuration = Util.getDoubleValue(recordSet.getString("backDuration"), 0.00);
                if (backDuration >= duration) {
                    resultList.add(resourceId + "|" + belongDate + "|" + requestId);
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultList;
    }

    /**
     * 获取某段时间内的所有签到签退信息
     *
     * @return
     */
    private Map<String, Object> getSignInfo() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String type = Util.null2String(params.get("type"));//是查看一年的数据还是一月的数据：1-年、2-月
            String typevalue = Util.null2String(params.get("typevalue"));//指定的年份或者指定的月份
            String fromDate = Util.null2String(params.get("fromDate"));//指定日期起点
            String toDate = Util.null2String(params.get("toDate"));//指定日期终点
            String resourceId = Util.null2String(params.get("resourceId"));//指定查看的人员ID

            /**
             * 若未指定查看人员，则默认为当前登录人员
             */
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            /**
             * 获取指定日期的起点和终点
             */
            if (type.equals("1")) {//年
                if (typevalue.length() == 0 || typevalue.length() != 4) {
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            } else if (type.equals("2")) {//月
                if (typevalue.length() == 0) {
                    typevalue = DateUtil.getYear() + "-" + DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }
            /**
             * 判断是否显示补打卡数据
             */
            RecordSet recordSet = new RecordSet();
            boolean showCard = false;
            String settingSql = "select * from KQ_SETTINGS where main_key='showSignFromCard'";
            recordSet.executeQuery(settingSql);
            if (recordSet.next()) {
                String main_val = recordSet.getString("main_val");
                showCard = main_val.equals("1");
            }

            SignInfo signInfo = null;
            Map<String, SignInfo> signInfoMap = new LinkedHashMap<String, SignInfo>();

            String sql = "SELECT a.kqdate,a.resourceId,a.serialNumber,a.serialId,'signIn' signType,a.signInDate signDate,a.signInTime signTime,b.clientAddress,b.addr FROM KQ_FORMAT_DETAIL a LEFT JOIN HrmScheduleSign b ON a.signInId=b.id WHERE 1=1 AND resourceId=" + resourceId + "\n ";
            if (!showCard) {
                sql += " AND (signFrom is null or signFrom not like 'card%') ";
            }
            if (!"".equals(fromDate)) {
                sql += " AND kqDate>='" + fromDate + "' ";
            }
            if (!"".equals(toDate)) {
                sql += " AND kqDate<='" + toDate + "' ";
            }
            sql += " UNION ALL\n " +
                    "SELECT a.kqdate,a.resourceId,a.serialNumber,a.serialId,'signOut' signType,a.signOutDate signDate,a.signOutTime signTime,b.clientAddress,b.addr FROM KQ_FORMAT_DETAIL a LEFT JOIN HrmScheduleSign b ON a.signOutId=b.id WHERE 1=1 AND resourceId=" + resourceId + "\n ";
            if (!showCard) {
                sql += " AND (signFrom is null or signFrom not like 'card%') ";
            }
            if (!"".equals(fromDate)) {
                sql += " AND kqDate>='" + fromDate + "' ";
            }
            if (!"".equals(toDate)) {
                sql += " AND kqDate<='" + toDate + "' ";
            }
            sql += "ORDER BY kqDate,resourceId,serialNumber,signType ";
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String kqDate = Util.null2String(recordSet.getString("kqDate"));
                String signType = Util.null2String(recordSet.getString("signType"));
                String signDate = Util.null2String(recordSet.getString("signDate"));
                String signTime = Util.null2String(recordSet.getString("signTime"));
                String serialId = Util.null2String(recordSet.getString("serialId"));
                int serialNumber = Util.getIntValue(recordSet.getString("serialNumber"), 0);


                signInfo = signInfoMap.get(kqDate);
                if (signInfo == null) {
                    signInfo = new SignInfo();
                }
                signInfo.setSerialId("" + serialId);
                if ("signIn".equals(signType)) {
                    if (serialNumber == 2) {
                        signInfo.setNeedSign3(true);
                        signInfo.setSignInTime3(signTime);
                    } else if (serialNumber == 1) {
                        signInfo.setNeedSign2(true);
                        signInfo.setSignInTime2(signTime);
                    } else {
                        signInfo.setNeedSign1(true);
                        signInfo.setSignInTime1(signTime);
                    }
                } else {
                    if (serialNumber == 2) {
                        signInfo.setNeedSign3(true);
                        signInfo.setSignOutTime3(signTime);
                    } else if (serialNumber == 1) {
                        signInfo.setNeedSign2(true);
                        signInfo.setSignOutTime2(signTime);
                    } else {
                        signInfo.setNeedSign1(true);
                        signInfo.setSignOutTime1(signTime);
                    }
                }
                signInfoMap.put(kqDate,signInfo);
            }

            Map<String, Object> itemMap = null;
            List<Map<String, Object>> itemList = null;
            for (Map.Entry entry : signInfoMap.entrySet()) {
                String kqDate = (String) entry.getKey();
                signInfo = (SignInfo) entry.getValue();
                String serialId = Util.null2String(signInfo.getSerialId());
                boolean needSign1 = signInfo.isNeedSign1();
                String signInTime1 = Util.null2String(signInfo.getSignInTime1());
                String signOutTime1 = Util.null2String(signInfo.getSignOutTime1());
                boolean needSign2 = signInfo.isNeedSign2();
                String signInTime2 = Util.null2String(signInfo.getSignInTime2());
                String signOutTime2 = Util.null2String(signInfo.getSignOutTime2());
                boolean needSign3 = signInfo.isNeedSign3();
                String signInTime3 = Util.null2String(signInfo.getSignInTime3());
                String signOutTime3 = Util.null2String(signInfo.getSignOutTime3());

                itemList = new ArrayList<Map<String, Object>>();

                if (serialId.equals("") || serialId.equals("0")) {
                    if (signInTime1.equals("") && signOutTime1.equals("")) {
                        continue;
                    } else {
                        itemMap = new HashMap<String, Object>();
                        itemMap.put("title", SystemEnv.getHtmlLabelName(512504, user.getLanguage()));
                        if (signInTime1.equals("")) {
                            itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                        } else {
                            itemMap.put("signTime", signInTime1);
                        }
                        itemList.add(itemMap);

                        itemMap = new HashMap<String, Object>();
                        itemMap.put("title", SystemEnv.getHtmlLabelName(512505, user.getLanguage()));
                        if (signOutTime1.equals("")) {
                            itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                        } else {
                            itemMap.put("signTime", signOutTime1);
                        }
                        itemList.add(itemMap);
                        resultMap.put(kqDate,itemList);
                    }
                } else {
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("title", SystemEnv.getHtmlLabelName(512504, user.getLanguage()));
                    if (signInTime1.equals("")) {
                        itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                    } else {
                        itemMap.put("signTime", signInTime1);
                    }
                    itemList.add(itemMap);

                    itemMap = new HashMap<String, Object>();
                    itemMap.put("title", SystemEnv.getHtmlLabelName(512505, user.getLanguage()));
                    if (signOutTime1.equals("")) {
                        itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                    } else {
                        itemMap.put("signTime", signOutTime1);
                    }
                    itemList.add(itemMap);

                    if (needSign2) {
                        itemMap = new HashMap<String, Object>();
                        itemMap.put("title", SystemEnv.getHtmlLabelName(512504, user.getLanguage()));
                        if (signInTime2.equals("")) {
                            itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                        } else {
                            itemMap.put("signTime", signInTime2);
                        }
                        itemList.add(itemMap);

                        itemMap = new HashMap<String, Object>();
                        itemMap.put("title", SystemEnv.getHtmlLabelName(512505, user.getLanguage()));
                        if (signOutTime2.equals("")) {
                            itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                        } else {
                            itemMap.put("signTime", signOutTime2);
                        }
                        itemList.add(itemMap);
                    }

                    if (needSign3) {
                        itemMap = new HashMap<String, Object>();
                        itemMap.put("title", SystemEnv.getHtmlLabelName(512504, user.getLanguage()));
                        if (signInTime3.equals("")) {
                            itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                        } else {
                            itemMap.put("signTime", signInTime3);
                        }
                        itemList.add(itemMap);

                        itemMap = new HashMap<String, Object>();
                        itemMap.put("title", SystemEnv.getHtmlLabelName(512505, user.getLanguage()));
                        if (signOutTime3.equals("")) {
                            itemMap.put("signTime", SystemEnv.getHtmlLabelName(25994, user.getLanguage()));
                        } else {
                            itemMap.put("signTime", signOutTime3);
                        }
                        itemList.add(itemMap);
                    }
                    resultMap.put(kqDate,itemList);
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    class SignInfo {
        private String serialId;
        private boolean needSign1 = false;
        private String signInTime1;
        private String signOutTime1;
        private boolean needSign2 = false;
        private String signInTime2;
        private String signOutTime2;
        private boolean needSign3 = false;
        private String signInTime3;
        private String signOutTime3;

        public SignInfo() {
        }

        public String getSerialId() {
            return serialId;
        }

        public void setSerialId(String serialId) {
            this.serialId = serialId;
        }

        public boolean isNeedSign1() {
            return needSign1;
        }

        public void setNeedSign1(boolean needSign1) {
            this.needSign1 = needSign1;
        }

        public String getSignInTime1() {
            return signInTime1;
        }

        public void setSignInTime1(String signInTime1) {
            this.signInTime1 = signInTime1;
        }

        public String getSignOutTime1() {
            return signOutTime1;
        }

        public void setSignOutTime1(String signOutTime1) {
            this.signOutTime1 = signOutTime1;
        }

        public boolean isNeedSign2() {
            return needSign2;
        }

        public void setNeedSign2(boolean needSign2) {
            this.needSign2 = needSign2;
        }

        public String getSignInTime2() {
            return signInTime2;
        }

        public void setSignInTime2(String signInTime2) {
            this.signInTime2 = signInTime2;
        }

        public String getSignOutTime2() {
            return signOutTime2;
        }

        public void setSignOutTime2(String signOutTime2) {
            this.signOutTime2 = signOutTime2;
        }

        public boolean isNeedSign3() {
            return needSign3;
        }

        public void setNeedSign3(boolean needSign3) {
            this.needSign3 = needSign3;
        }

        public String getSignInTime3() {
            return signInTime3;
        }

        public void setSignInTime3(String signInTime3) {
            this.signInTime3 = signInTime3;
        }

        public String getSignOutTime3() {
            return signOutTime3;
        }

        public void setSignOutTime3(String signOutTime3) {
            this.signOutTime3 = signOutTime3;
        }
    }
}
