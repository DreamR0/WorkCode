package com.engine.kq.cmd.myattendance;

import com.alibaba.fastjson.JSON;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.wfset.util.KQSignUtil;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取签到签退信息
 */
public class GetHrmKQSignInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetHrmKQSignInfoCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        new BaseBean().writeLog("=zj==(签到移动端)" + params.get("isMobile"));
        if ("true".equals(Util.null2String(params.get("isMobile")))){
            //==zj  判断主账号有没有考勤组
            KqUtil kqUtil = new KqUtil();
            String userId = Util.null2String(user.getUID());  //获取主账号userid
            String today = DateUtil.getCurrentDate();         //获取当前日期

            Boolean flag = kqUtil.mainAccountCheck(userId, today);
            new BaseBean().writeLog("==zj==(签到移动端)" + flag);
            if (!flag){
                //如果主账号没有考勤组，再看子账号是否有考勤组
                List<Map> list = kqUtil.childAccountCheck(userId, today);
                new BaseBean().writeLog("==zj==(签到移动端-maps)" + JSON.toJSONString(list));
                if (list.size()  == 1){
                    //如果只有一个生效考勤组
                    Map<String,String> map = list.get(0);
                    int userid = Integer.parseInt(map.get("userid"));
                    this.user= new User(userid);
                }
            }
        }
        try {
            String date = Util.null2String(params.get("date"));
            String resourceId = Util.null2String(params.get("resourceId"));
            if (resourceId.equals("")) {
                resourceId = String.valueOf(user.getUID());
            }
            new BaseBean().writeLog("==zj==(移动端签到签退数据)"  + resourceId);
            String isMobile = Util.null2String(params.get("isMobile"));//是否是移动端的【我的考勤】

            /**判断是否显示补打卡数据*/
            boolean showCard = false;
            String settingSql = "select * from KQ_SETTINGS where main_key='showSignFromCard'";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(settingSql);
            if (recordSet.next()) {
                String main_val = recordSet.getString("main_val");
                showCard = main_val.equals("1");
            }

            Map<String, Object> signStatusInfo = new HashMap<String, Object>();

            List<Object> dataList = new ArrayList<>();
            Map<String, Object> dataMap = new HashMap<>();
            List<Object> dataItemList = new ArrayList<>();
            Map<String, Object> dataItemMap = new HashMap<>();

            String serialId = "";
            String sql = "SELECT a.RESOURCEID, a.KQDATE, a.GROUPID, a.SERIALID, a.SERIALNUMBER, a.WORKBEGINDATE,\n " +
                    "a.WORKBEGINTIME, a.WORKENDDATE, a.WORKENDTIME, a.WORKMINS, a.SIGNINDATE, a.SIGNINTIME,\n " +
                    "a.SIGNINID, a.SIGNOUTDATE, a.SIGNOUTTIME, a.SIGNOUTID, a.ATTENDANCEMINS, a.BELATEMINS,\n " +
                    "a.GRAVEBELATEMINS, a.LEAVEEARLYMINS, a.GRAVELEAVEEARLYMINS, a.ABSENTEEISMMINS, a.LEAVEMINS,\n " +
                    "a.EVECTIONMINS, a.OUTMINS, a.FORGOTCHECKMINS, a.SIGNMINS, a.LEAVEINFO, a.FORGOTBEGINWORKCHECKMINS,\n " +
                    "'signIn' signType,a.signInDate signDate,a.signInTime signTime,b.clientAddress,b.addr\n " +
                    "FROM KQ_FORMAT_DETAIL a LEFT JOIN HrmScheduleSign b ON a.signInId=b.id " +
                    "WHERE kqDate='" + date + "' AND resourceId=" + resourceId + "\n ";
            if (!showCard) {
                sql += " AND (signFrom is null or signFrom not like 'card%') ";
            }
            sql += "UNION ALL\n " +
                    "SELECT a.RESOURCEID, a.KQDATE, a.GROUPID, a.SERIALID, a.SERIALNUMBER, a.WORKBEGINDATE,\n " +
                    "a.WORKBEGINTIME, a.WORKENDDATE, a.WORKENDTIME, a.WORKMINS, a.SIGNINDATE, a.SIGNINTIME,\n " +
                    "a.SIGNINID, a.SIGNOUTDATE, a.SIGNOUTTIME, a.SIGNOUTID, a.ATTENDANCEMINS, a.BELATEMINS,\n " +
                    "a.GRAVEBELATEMINS, a.LEAVEEARLYMINS, a.GRAVELEAVEEARLYMINS, a.ABSENTEEISMMINS, a.LEAVEMINS,\n " +
                    "a.EVECTIONMINS, a.OUTMINS, a.FORGOTCHECKMINS, a.SIGNMINS, a.LEAVEINFO, a.FORGOTBEGINWORKCHECKMINS,\n " +
                    "'signOut' signType,a.signOutDate signDate,a.signOutTime signTime,b.clientAddress,b.addr\n " +
                    "FROM KQ_FORMAT_DETAIL a LEFT JOIN HrmScheduleSign b ON a.signOutId=b.id " +
                    "WHERE kqDate='" + date + "' AND resourceId=" + resourceId + "\n ";
            if (!showCard) {
                sql += " AND (signFrom is null or signFrom not like 'card%') ";
            }
            sql += "ORDER BY kqDate,resourceId,serialNumber,signType ";
            new BaseBean().writeLog("==zj==（查询sql）"  + sql);
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String signType = recordSet.getString("signType");
                String signDate = recordSet.getString("signDate");
                String signTime = recordSet.getString("signTime");
                String clientAddress = recordSet.getString("clientAddress");
                String addr = recordSet.getString("addr");

                serialId = Util.null2String(recordSet.getString("serialId"));
                int serialNumber = recordSet.getInt("serialnumber") + 1;
                String workBeginTime = Util.null2String(recordSet.getString("workbegintime")).trim();
                String workEndTime = Util.null2String(recordSet.getString("workendtime")).trim();
                int workMins = recordSet.getInt("workMins");
                int attendanceMins = recordSet.getInt("attendanceMins");
                String beLateMins = Util.null2String(recordSet.getString("beLateMins")).trim();
                String graveBeLateMins = Util.null2String(recordSet.getString("graveBeLateMins")).trim();
                String leaveEarlyMins = Util.null2String(recordSet.getString("leaveEarlyMins")).trim();
                String graveLeaveEarlyMins = Util.null2String(recordSet.getString("graveLeaveEarlyMins")).trim();
                String absenteeismMins = Util.null2String(recordSet.getString("absenteeismMins")).trim();
                String forgotCheckMins = Util.null2String(recordSet.getString("forgotcheckMins")).trim();
                String forgotBeginWorkCheckMins = Util.null2String(recordSet.getString("forgotBeginWorkCheckMins")).trim();
                int leaveMins = recordSet.getInt("leaveMins");
                String leaveInfo = Util.null2String(recordSet.getString("leaveInfo"));
                int evectionMins = recordSet.getInt("evectionMins");
                int outMins = recordSet.getInt("outMins");

                signStatusInfo = new HashMap<String, Object>();
                signStatusInfo.put("worktime", workBeginTime);
                signStatusInfo.put("absenteeismMins", absenteeismMins);
                signStatusInfo.put("leaveMins", leaveMins);
                signStatusInfo.put("leaveInfo", leaveInfo);
                signStatusInfo.put("evectionMins", evectionMins);
                signStatusInfo.put("outMins", outMins);
                String signStatus = "";
                boolean needWorkFlow = false;//是否需要提交考勤异常流程
                if ("signIn".equals(signType)) {
                    signStatusInfo.put("beLateMins", beLateMins);
                    signStatusInfo.put("graveBeLateMins", graveBeLateMins);
                    signStatusInfo.put("forgotBeginWorkCheckMins", forgotBeginWorkCheckMins);
                    signStatus = "on";
                    if (Util.getDoubleValue(beLateMins, 0) > 0 || Util.getDoubleValue(graveBeLateMins, 0) > 0 || Util.getDoubleValue(forgotBeginWorkCheckMins, 0) > 0 || Util.getDoubleValue(absenteeismMins, 0) > 0) {
                        needWorkFlow = true;
                    }
                } else {
                    signStatusInfo.put("leaveEarlyMins", leaveEarlyMins);
                    signStatusInfo.put("graveLeaveEarlyMins", graveLeaveEarlyMins);
                    signStatusInfo.put("forgotCheckMins", forgotCheckMins);
                    signStatusInfo.put("forgotBeginWorkCheckMins", forgotBeginWorkCheckMins);
                    signStatus = "off";
                    if (Util.getDoubleValue(leaveEarlyMins, 0) > 0 || Util.getDoubleValue(graveLeaveEarlyMins, 0) > 0 || Util.getDoubleValue(forgotCheckMins, 0) > 0 || Util.getDoubleValue(absenteeismMins, 0) > 0) {
                        needWorkFlow = true;
                    }
                }
                if (!resourceId.equals("" + user.getUID())) {
                    needWorkFlow = false;
                }
                String status = KQReportBiz.getSignStatus2(signStatusInfo, user, signStatus);
//                if (serialId.equalsIgnoreCase("")) {
//                    continue;
//                }
                if ("signIn".equals(signType)) {
                    //签到 start
                    String signDateTime = signDate + " " + signTime;
                    if (signTime.trim().length() <= 0) {
                        signDateTime = SystemEnv.getHtmlLabelName(25994, user.getLanguage());
                    }

                    dataMap = new HashMap<>();
                    dataItemList = new ArrayList<Object>();

                    dataItemMap = new HashMap<String, Object>();
                    dataItemMap.put("title", SystemEnv.getHtmlLabelName(512504, user.getLanguage()));
                    dataItemMap.put("value", signDateTime);
                    dataItemList.add(dataItemMap);

                    dataItemMap = new HashMap<String, Object>();
                    dataItemMap.put("title", SystemEnv.getHtmlLabelName(20032, user.getLanguage()) + "IP");
                    dataItemMap.put("value", clientAddress);
                    dataItemList.add(dataItemMap);

                    dataItemMap = new HashMap<String, Object>();
                    dataItemMap.put("title", SystemEnv.getHtmlLabelName(125531, user.getLanguage()));
                    dataItemMap.put("value", addr);
                    dataItemList.add(dataItemMap);

                    dataMap.put("item", dataItemList);
                    dataMap.put("status", status);
                    dataMap.put("needWorkFlow", needWorkFlow);
                    dataList.add(dataMap);
                    //签到 end
                } else {
                    //签退 start
                    String signDateTime = signDate + " " + signTime;
                    if (signTime.trim().length() <= 0) {
                        signDateTime = SystemEnv.getHtmlLabelName(25994, user.getLanguage());
                    }

                    dataMap = new HashMap<>();
                    dataItemList = new ArrayList<Object>();

                    dataItemMap = new HashMap<String, Object>();
                    dataItemMap.put("title", SystemEnv.getHtmlLabelName(512505, user.getLanguage()));
                    dataItemMap.put("value", signDateTime);
                    dataItemList.add(dataItemMap);

                    dataItemMap = new HashMap<String, Object>();
                    dataItemMap.put("title", SystemEnv.getHtmlLabelName(20033, user.getLanguage()) + "IP");
                    dataItemMap.put("value", clientAddress);
                    dataItemList.add(dataItemMap);

                    dataItemMap = new HashMap<String, Object>();
                    dataItemMap.put("title", SystemEnv.getHtmlLabelName(132060, user.getLanguage()));
                    dataItemMap.put("value", addr);
                    dataItemList.add(dataItemMap);

                    dataMap.put("item", dataItemList);
                    dataMap.put("status", status);
                    dataMap.put("needWorkFlow", needWorkFlow);
                    dataList.add(dataMap);
                    //签退 end
                }
            }

            Map<String, Object> signDateMap = new HashMap<String, Object>();
            signDateMap.put("date", date);
            signDateMap.put("signInfo", dataList);
            //班次时间
            ShiftManagementToolKit shiftManagementToolKit = new ShiftManagementToolKit();
//            String serialInfo = shiftManagementToolKit.getShiftOnOffWorkSections(serialId, user.getLanguage());
            //班次信息--加入自由班制的判断
            String serialInfo = shiftManagementToolKit.getShiftOnOffWorkSections(serialId, user.getLanguage(),date,resourceId);
            signDateMap.put("serialInfo", SystemEnv.getHtmlLabelName(24803, user.getLanguage()) + "：" + serialInfo);
            resultMap.put("data", signDateMap);
            new BaseBean().writeLog("GetHrmKQSignInfoCmd>>isMobile="+isMobile+";date="+date);
            //只有移动端的【我的考勤】才需要显示流程相关的数据
            if ("true".equals(isMobile)) {
                //获取考勤流程的相关数据
                List<Map<String, Object>> workflowList = new ArrayList<Map<String, Object>>();
                //请假
                workflowList = getFlowData(KqSplitFlowTypeEnum.LEAVE.getFlowtype());
                if (workflowList == null) {
                    workflowList = new ArrayList<Map<String, Object>>();
                }
                //出差
                workflowList.addAll(getFlowData(KqSplitFlowTypeEnum.EVECTION.getFlowtype()));
                if (workflowList == null) {
                    workflowList = new ArrayList<Map<String, Object>>();
                }
                //公出
                workflowList.addAll(getFlowData(KqSplitFlowTypeEnum.OUT.getFlowtype()));
                if (workflowList == null) {
                    workflowList = new ArrayList<Map<String, Object>>();
                }
                //加班
                workflowList.addAll(getFlowData(KqSplitFlowTypeEnum.OVERTIME.getFlowtype()));
                resultMap.put("workflowInfo", workflowList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    public List<Map<String, Object>> getFlowData(int flowType) {
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            String date = Util.null2String(params.get("date"));
            if ("".equals(date)) {
                date = DateUtil.getCurrentDate();
            }
            String firstDateOfMonth = DateUtil.getFirstDayOfMonth(date);
            String lastDateOfMonth = DateUtil.getLastDayOfMonth(date);

            /**获取销假*/
            List<String> leaveBackList = getLeaveDate();

            Map<String, Object> dataMap = new HashMap<String, Object>();
            Map<String, Object> workflowMap = new HashMap<String, Object>();
            List<Map<String, Object>> workflowList = new ArrayList<Map<String, Object>>();

            boolean showOnHoliday = true;//非工作日是否显示流程（出差、公出、请假）
            KQExitRulesComInfo exitRulesComInfo = new KQExitRulesComInfo();
            KQTravelRulesComInfo travelRulesComInfo = new KQTravelRulesComInfo();
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            KQWorkTime kqWorkTime = new KQWorkTime();

            if (flowType == 2) {
                showOnHoliday = exitRulesComInfo.getComputingMode("1").equals("2");
            } else if (flowType == 1) {
                showOnHoliday = travelRulesComInfo.getComputingMode("1").equals("2");
            }

            Map<String, Object> paramsMap = new HashMap<String, Object>();
            paramsMap.put("typeselect", "6");
            paramsMap.put("tabKey", "1");
            paramsMap.put("kqtype", "" + flowType);
            paramsMap.put("resourceId", resourceId);
            paramsMap.put("fromDate", firstDateOfMonth);
            paramsMap.put("toDate", lastDateOfMonth);
            paramsMap.put("isMyKQ", "1");

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
                String requestName = recordSet.getString("requestName");
                int durationRule = Util.getIntValue(recordSet.getString("durationrule"));
                String unitName = "";
                if (durationRule == 3 || durationRule == 5 || durationRule == 6) {
                    unitName = SystemEnv.getHtmlLabelName(389326, user.getLanguage());
                } else {
                    unitName = SystemEnv.getHtmlLabelName(389325, user.getLanguage());
                }
                String fromDateTemp = recordSet.getString("fromDate");
                String fromTimeTemp = recordSet.getString("fromTime");
                String toDateTemp = recordSet.getString("toDate");
                String toTimeTemp = recordSet.getString("toTime");
                String leaveRulesId = "";
                String period = SystemEnv.getHtmlLabelName(506047, user.getLanguage()) + ":" + String.format("%.2f", duration) + unitName;
                if (flowType == 0) {
                    backDuration = Util.getDoubleValue(recordSet.getString("backDuraion"), 0.00);
                    if (backDuration > 0) {
                        period = SystemEnv.getHtmlLabelName(506047, user.getLanguage()) + ":" + String.format("%.2f", duration) + "(" + SystemEnv.getHtmlLabelName(24473, user.getLanguage()) + ":" + String.format("%.2f", backDuration) + ")" + unitName;
                    }
                    leaveRulesId = recordSet.getString("newleavetype");
                    showOnHoliday = rulesComInfo.getComputingMode(leaveRulesId).equals("2");
                }

                boolean isEnd = false;
                for (String dateTemp = fromDateTemp; !isEnd; ) {
                    if (dateTemp.equals(toDateTemp)) {
                        isEnd = true;
                    }
                    if (!leaveBackList.contains(resourceId + "|" + date + "|" + requestId)) {
                        workflowList = (ArrayList<Map<String, Object>>) dataMap.get(resourceId + "|" + dateTemp);
                        if (null == workflowList) {
                            workflowList = new ArrayList<Map<String, Object>>();
                        }
                        workflowMap = new HashMap<String, Object>();
                        workflowMap.put("title", requestName);
                        workflowMap.put("period", period);
                        workflowMap.put("requestId", requestId);
                        workflowList.add(workflowMap);

                        boolean isWorkDay = kqWorkTime.isWorkDay(resourceId, date);
                        if (!(!isWorkDay && !showOnHoliday)) {
                            dataMap.put(resourceId + "|" + dateTemp, workflowList);
                        }
                    }

                    dateTemp = DateUtil.getDate(dateTemp, 1);
                }
            }
            if (dataMap.get(resourceId + "|" + date) != null) {
                resultList = (ArrayList<Map<String, Object>>) dataMap.get(resourceId + "|" + date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }

    /**
     * 获取请完假之后又销掉的日期
     *
     * @return
     */
    private List<String> getLeaveDate() {
        List<String> resultList = new ArrayList<String>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            String date = Util.null2String(params.get("date"));
            if ("".equals(date)) {
                date = DateUtil.getCurrentDate();
            }
            String firstDateOfMonth = DateUtil.getFirstDayOfMonth(date);
            String lastDateOfMonth = DateUtil.getLastDayOfMonth(date);

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
            if (firstDateOfMonth.length() > 0 && lastDateOfMonth.length() > 0) {
                sql += " and " + leaveTableName + ".belongdate between'" + firstDateOfMonth + "' and '" + lastDateOfMonth + "' ";
            }
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String requestId = recordSet.getString("requestId");
                String belongDate = recordSet.getString("belongDate");
                double duration = Util.getDoubleValue(recordSet.getString("duration"), 0.00);
                double backDuration = Util.getDoubleValue(recordSet.getString("backDuration"), 0.00);
                if (backDuration >= duration) {
                    resultList.add(resourceId + "|" + belongDate + "|" + requestId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }
}
