package com.engine.kq.cmd.myattendance;

import com.alibaba.fastjson.JSON;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQAttFlowSetBiz;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.util.KQTransMethod;
import com.engine.kq.util.TransMethod;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.util.*;

public class GetDetailList4MobileCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetDetailList4MobileCmd(Map<String, Object> params, User user) {
        this.user = user;

        //==zj  判断主账号有没有考勤组
        KqUtil kqUtil = new KqUtil();
        String userId = Util.null2String(user.getUID());  //获取主账号userid
        String today = DateUtil.getCurrentDate();         //获取当前日期

        Boolean flag = kqUtil.mainAccountCheck(userId, today);
        new BaseBean().writeLog("==zj==(我的考勤移动端)" + flag);
        if (!flag){
            //如果主账号没有考勤组，再看子账号是否有考勤组
            List<Map> list = kqUtil.childAccountCheck(userId, today);
            new BaseBean().writeLog("==zj==(我的考勤移动端-maps)" + JSON.toJSONString(list));
            if (list.size()  == 1){
                //如果只有一个生效考勤组
                Map<String,String> map = list.get(0);
                int userid = Integer.parseInt(map.get("userid"));
                this.user= new User(userid);
            }
        }
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String kqtype = Util.null2String(params.get("kqtype"));
            if (kqtype.equalsIgnoreCase("realWorkdays")) {
                resultMap = getRealWorkdaysList();
            } else if (kqtype.equalsIgnoreCase("absent")) {
                resultMap = getAbsentList();
            } else if (kqtype.equalsIgnoreCase("beLate") || kqtype.equalsIgnoreCase("graveBeLate")) {
                if (kqtype.equalsIgnoreCase("graveBeLate")) {
                    params.put("tabKey", "2");
                }
                resultMap = getBeLateList();
            } else if (kqtype.equalsIgnoreCase("leaveEarly") || kqtype.equalsIgnoreCase("graveLeaveEarly")) {
                if (kqtype.equalsIgnoreCase("graveLeaveEarly")) {
                    params.put("tabKey", "2");
                }
                resultMap = getLeaveEearlyList();
            } else if (kqtype.equalsIgnoreCase("noSign")) {
                resultMap = getForgotCheckList();
            } else if (kqtype.startsWith("leaveType_")
                    || kqtype.equalsIgnoreCase("evection")
                    || kqtype.equalsIgnoreCase("outDays")) {
                resultMap = getLeaveList();
            } else if (kqtype.equalsIgnoreCase("overTime")) {
                resultMap = getOverTimeList();
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取实际出勤明细
     *
     * @return
     */
    private Map<String, Object> getRealWorkdaysList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            String signType = Util.null2String(params.get("signType"));//打卡类型
            //由于手机端无法一次性获取所有得数据，需要分批次获取
            int index = Util.getIntValue((String) params.get("index"), 0);//从第几个开始获取
            int pageSize = Util.getIntValue((String) params.get("pageSize"), 10);//一次获取多少个

            TransMethod transMethod = new TransMethod();
            List<Object> dataList = new ArrayList<Object>();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();

            RecordSet recordSet = new RecordSet();
            String backFields = " a.id, b.resourceid,a.departmentid, a.lastname, a.workcode, a.status, a.dsporder, kqdate, serialid,serialid as serialid1, serialid as serialid2," +
                    " workbegintime,workendtime,signintime,signouttime, attendanceMins, signMins ";
            String sqlFrom = "from hrmresource a, kq_format_detail b  ";
            String sqlWhere = " where a.id = b.resourceid and (attendanceMins>0 or signMins>0)";
            String orderBy = " kqdate asc, workbegintime asc ";
            if (!"".equals(resourceId)) {
                sqlWhere += " and resourceId=" + resourceId;
            }
            if (!"".equals(searchDateFrom)) {
                sqlWhere += " and kqdate>='" + searchDateFrom + "' ";
            }
            if (!"".equals(searchDateTo)) {
                sqlWhere += " and kqdate<='" + searchDateTo + "' ";
            }
            if ("1".equals(signType)) {
                if (recordSet.getDBType().equalsIgnoreCase("sqlserver") || recordSet.getDBType().equalsIgnoreCase("mysql")) {
                    sqlWhere += " and (signInTime is not null and signInTime<>'') ";
                } else {
                    sqlWhere += " and (signInTime is not null) ";
                }
            } else if ("2".equals(signType)) {
                if (recordSet.getDBType().equalsIgnoreCase("sqlserver") || recordSet.getDBType().equalsIgnoreCase("mysql")) {
                    sqlWhere += " and (signOutTime is not null and signOutTime<>'') ";
                } else {
                    sqlWhere += " and (signOutTime is not null) ";
                }
            }
            String pageSql = "select * from (select tmp.*,rownum rn from ( select" + backFields + sqlFrom + sqlWhere + " order by " + orderBy + ") tmp where rownum<=" + (pageSize + index) + ") c where rn>=" + (index + 1);
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                pageSql = "select t.* from ( select " + backFields + ",ROW_NUMBER() OVER( order by " + orderBy + ") rn " + sqlFrom + sqlWhere + ") t where 1=1 and rn>=" + (index + 1) + " and rn<=" + (pageSize + index);
            } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + index + "," + pageSize;
            }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + pageSize + " offset " + index;
            }
            recordSet.executeQuery(pageSql);
            if (pageSize > recordSet.getCounts()) {
                resultMap.put("isEnd", true);
            } else {
                resultMap.put("isEnd", false);
            }
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");
                String serialId = recordSet.getString("serialId");
                String serialNumber = recordSet.getString("serialNumber");
                String workBeginTime = recordSet.getString("workBeginTime");
                String workEndTime = recordSet.getString("workEndTime");
                String signInDate = recordSet.getString("signInDate");
                String signInTime = recordSet.getString("signInTime");
                String signOutDate = recordSet.getString("signOutDate");
                String signOutTime = recordSet.getString("signOutTime");
                String signMins = recordSet.getString("signMins");
                String attendanceMins = recordSet.getString("attendanceMins");

                String serialInfo = transMethod.getSerailName(serialId, workBeginTime + "+" + workEndTime);
                String signInTimeShow = transMethod.getReportDetialSignInTime(serialId, signInTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());
                String signOutTimeShow = transMethod.getReportDetialSignOutTime(serialId, signOutTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());

                itemList = new ArrayList<Map<String, Object>>();
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", "");
                itemMap.put("key", "kqDate");
                itemMap.put("value", kqDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390054, user.getLanguage()));
                itemMap.put("key", "serialInfo");
                itemMap.put("value", serialInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390495, user.getLanguage()));
                itemMap.put("key", "signInTime");
                itemMap.put("value", signInTimeShow);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390496, user.getLanguage()));
                itemMap.put("key", "signOutTime");
                itemMap.put("value", signOutTimeShow);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(391040, user.getLanguage()));
                itemMap.put("key", "attendanceMins");
                itemMap.put("value", transMethod.getReportDetialMinToHour(attendanceMins));
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(504433, user.getLanguage()));
                itemMap.put("key", "signMins");
                itemMap.put("value", transMethod.getReportDetialMinToHour(signMins));
                itemList.add(itemMap);

                dataList.add(itemList);
            }
            resultMap.put("data", dataList);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(10000816, Util.getIntValue(user.getLanguage())));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513174, user.getLanguage()).replace("{lastName}", lastName));
            }
        } catch (Exception e) {
            writeLog(e);
            resultMap.put("api_status", false);
            resultMap.put("message", e.getMessage());
        }
        return resultMap;
    }

    /**
     * 获取旷工明细
     *
     * @return
     */
    private Map<String, Object> getAbsentList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            //由于手机端无法一次性获取所有得数据，需要分批次获取
            int index = Util.getIntValue((String) params.get("index"), 0);//从第几个开始获取
            int pageSize = Util.getIntValue((String) params.get("pageSize"), 10);//一次获取多少个


            TransMethod transMethod = new TransMethod();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
            List<Object> dataList = new ArrayList<Object>();

            String backFields = " * ";
            String sqlFrom = " from kq_format_detail  ";
            String sqlWhere = " where absenteeismMins>0 ";
            String orderBy = " kqdate asc, workbegintime asc ";
            if (!"".equals(resourceId)) {
                sqlWhere += " and resourceId=" + resourceId;
            }
            if (!"".equals(searchDateFrom)) {
                sqlWhere += " and kqDate>='" + searchDateFrom + "' ";
            }
            if (!"".equals(searchDateTo)) {
                sqlWhere += " and kqDate<='" + searchDateTo + "' ";
            }
            RecordSet recordSet = new RecordSet();
            String pageSql = "select * from (select tmp.*,rownum rn from ( select" + backFields + sqlFrom + sqlWhere + " order by " + orderBy + ") tmp where rownum<=" + (pageSize + index) + ") c where rn>=" + (index + 1);
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                pageSql = "select t.* from ( select " + backFields + ",ROW_NUMBER() OVER( order by " + orderBy + ") rn " + sqlFrom + sqlWhere + ") t where 1=1 and rn>=" + (index + 1) + " and rn<=" + (pageSize + index);
            } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + index + "," + pageSize;
            }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + pageSize + " offset " + index;
            }
            recordSet.executeQuery(pageSql);
            if (pageSize > recordSet.getCounts()) {
                resultMap.put("isEnd", true);
            } else {
                resultMap.put("isEnd", false);
            }
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");
                String serialId = recordSet.getString("serialId");
                String workBeginTime = recordSet.getString("workBeginTime");
                String workEndTime = recordSet.getString("workEndTime");
                String signInTime = recordSet.getString("signInTime");
                String signOutTime = recordSet.getString("signOutTime");

                String serialInfo = transMethod.getSerailName(serialId, workBeginTime + "+" + workEndTime);
                String signInfo = transMethod.getReportDetialSignTime(serialId, signInTime + "+" + signOutTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());
                String absenteeismMins = transMethod.getReportDetialMinToHour(recordSet.getString("absenteeismMins"));

                itemList = new ArrayList<Map<String, Object>>();
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", "");
                itemMap.put("key", "kqDate");
                itemMap.put("value", kqDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390054, user.getLanguage()));
                itemMap.put("key", "serialInfo");
                itemMap.put("value", serialInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(18949, user.getLanguage()));
                itemMap.put("key", "signInfo");
                itemMap.put("value", signInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(391416, user.getLanguage()));
                itemMap.put("key", "absenteeismMins");
                itemMap.put("value", absenteeismMins);
                itemList.add(itemMap);

                dataList.add(itemList);
            }
            resultMap.put("data", dataList);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(10000817, Util.getIntValue(user.getLanguage())));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513179, user.getLanguage()).replace("{lastName}", lastName));
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取迟到或者严重迟到的明细
     *
     * @return
     */
    private Map<String, Object> getBeLateList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            String tabKey = Util.null2String(params.get("tabKey"));//是迟到还是严重迟到
            //由于手机端无法一次性获取所有得数据，需要分批次获取
            int index = Util.getIntValue((String) params.get("index"), 0);//从第几个开始获取
            int pageSize = Util.getIntValue((String) params.get("pageSize"), 10);//一次获取多少个


            TransMethod transMethod = new TransMethod();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
            List<Object> dataList = new ArrayList<Object>();

            String backFields = " * ";
            String sqlFrom = " from kq_format_detail ";
            String sqlWhere = "where 1=1 ";
            String orderBy = " kqdate asc, workbegintime asc, resourceId asc ";
            if (!"".equals(resourceId)) {
                sqlWhere += " and resourceId=" + resourceId;
            }
            if (!"".equals(searchDateFrom)) {
                sqlWhere += " and kqDate>='" + searchDateFrom + "' ";
            }
            if (!"".equals(searchDateTo)) {
                sqlWhere += " and kqDate<='" + searchDateTo + "' ";
            }
            if ("2".equals(tabKey)) {
                sqlWhere += " and graveBeLateMins>0 ";
            } else {
                sqlWhere += " and beLateMins>0 ";
            }
            RecordSet recordSet = new RecordSet();
            String pageSql = "select * from (select tmp.*,rownum rn from ( select" + backFields + sqlFrom + sqlWhere + " order by " + orderBy + ") tmp where rownum<=" + (pageSize + index) + ") c where rn>=" + (index + 1);
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                pageSql = "select t.* from ( select " + backFields + ",ROW_NUMBER() OVER( order by " + orderBy + ") rn " + sqlFrom + sqlWhere + ") t where 1=1 and rn>=" + (index + 1) + " and rn<=" + (pageSize + index);
            } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + index + "," + pageSize;
            } else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + pageSize + " offset " + index;
            }
            recordSet.executeQuery(pageSql);
            if (pageSize > recordSet.getCounts()) {
                resultMap.put("isEnd", true);
            } else {
                resultMap.put("isEnd", false);
            }
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");
                String serialId = recordSet.getString("serialId");
                String workBeginTime = recordSet.getString("workBeginTime");
                String workEndTime = recordSet.getString("workEndTime");
                String signInTime = recordSet.getString("signInTime");
                String signOutTime = recordSet.getString("signOutTime");

                String serialInfo = transMethod.getSerailName(serialId, workBeginTime + "+" + workEndTime);
                String signInfo = transMethod.getReportDetialSignTime(serialId, signInTime + "+" + signOutTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());
                String beLateMins = transMethod.getReportDetialMinToHour(recordSet.getString("beLateMins"));
                String graveBeLateMins = transMethod.getReportDetialMinToHour(recordSet.getString("graveBeLateMins"));

                itemList = new ArrayList<Map<String, Object>>();
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", "");
                itemMap.put("key", "kqDate");
                itemMap.put("value", kqDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390054, user.getLanguage()));
                itemMap.put("key", "serialInfo");
                itemMap.put("value", serialInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(18949, user.getLanguage()));
                itemMap.put("key", "signInfo");
                itemMap.put("value", signInfo);
                itemList.add(itemMap);
                if ("2".equals(tabKey)) {
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("label", SystemEnv.getHtmlLabelName(10000818, Util.getIntValue(user.getLanguage())));
                    itemMap.put("key", "graveBeLateMins");
                    itemMap.put("value", graveBeLateMins);
                    itemList.add(itemMap);
                } else {
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("label", SystemEnv.getHtmlLabelName(10000819, Util.getIntValue(user.getLanguage())));
                    itemMap.put("key", "beLateMins");
                    itemMap.put("value", beLateMins);
                    itemList.add(itemMap);
                }
                dataList.add(itemList);
            }
            resultMap.put("data", dataList);
            if ("2".equals(tabKey)) {
                if (resourceId.equals("" + user.getUID())) {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(10000820, Util.getIntValue(user.getLanguage())));
                } else {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(513176, user.getLanguage()).replace("{lastName}", lastName));
                }
            } else {
                if (resourceId.equals("" + user.getUID())) {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(10000821, Util.getIntValue(user.getLanguage())));
                } else {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(513175, user.getLanguage()).replace("{lastName}", lastName));
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取早退或者严重早退的明细
     *
     * @return
     */
    private Map<String, Object> getLeaveEearlyList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            String tabKey = Util.null2String(params.get("tabKey"));
            //由于手机端无法一次性获取所有得数据，需要分批次获取
            int index = Util.getIntValue((String) params.get("index"), 0);//从第几个开始获取
            int pageSize = Util.getIntValue((String) params.get("pageSize"), 10);//一次获取多少个


            TransMethod transMethod = new TransMethod();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
            List<Object> dataList = new ArrayList<Object>();

            String backFields = " * ";
            String sqlFrom = " from kq_format_detail ";
            String sqlWhere = "where 1=1 ";
            String orderBy = " kqdate asc, workbegintime asc, resourceId asc ";
            if (!"".equals(resourceId)) {
                sqlWhere += " and resourceId=" + resourceId;
            }
            if (!"".equals(searchDateFrom)) {
                sqlWhere += " and kqDate>='" + searchDateFrom + "' ";
            }
            if (!"".equals(searchDateTo)) {
                sqlWhere += " and kqDate<='" + searchDateTo + "' ";
            }
            if ("2".equals(tabKey)) {
                sqlWhere += " and graveLeaveEarlyMins>0 ";
            } else {
                sqlWhere += " and leaveEarlyMins>0 ";
            }
            RecordSet recordSet = new RecordSet();
            String pageSql = "select * from (select tmp.*,rownum rn from ( select" + backFields + sqlFrom + sqlWhere + " order by " + orderBy + ") tmp where rownum<=" + (pageSize + index) + ") c where rn>=" + (index + 1);
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                pageSql = "select t.* from ( select " + backFields + ",ROW_NUMBER() OVER( order by " + orderBy + ") rn " + sqlFrom + sqlWhere + ") t where 1=1 and rn>=" + (index + 1) + " and rn<=" + (pageSize + index);
            } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + index + "," + pageSize;
            } else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + pageSize + " offset " + index;
            }
            recordSet.executeQuery(pageSql);
            if (pageSize > recordSet.getCounts()) {
                resultMap.put("isEnd", true);
            } else {
                resultMap.put("isEnd", false);
            }
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");
                String serialId = recordSet.getString("serialId");
                String workBeginTime = recordSet.getString("workBeginTime");
                String workEndTime = recordSet.getString("workEndTime");
                String signInTime = recordSet.getString("signInTime");
                String signOutTime = recordSet.getString("signOutTime");
                String serialInfo = transMethod.getSerailName(serialId, workBeginTime + "+" + workEndTime);
                String signInfo = transMethod.getReportDetialSignTime(serialId, signInTime + "+" + signOutTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());
                String leaveEarlyMins = transMethod.getReportDetialMinToHour(recordSet.getString("leaveEarlyMins"));
                String graveLeaveEarlyMins = transMethod.getReportDetialMinToHour(recordSet.getString("graveLeaveEarlyMins"));

                itemList = new ArrayList<Map<String, Object>>();
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", "");
                itemMap.put("key", "kqDate");
                itemMap.put("value", kqDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390054, user.getLanguage()));
                itemMap.put("key", "serialInfo");
                itemMap.put("value", serialInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(18949, user.getLanguage()));
                itemMap.put("key", "signInfo");
                itemMap.put("value", signInfo);
                itemList.add(itemMap);
                if ("2".equals(tabKey)) {
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("label", SystemEnv.getHtmlLabelName(10000822, Util.getIntValue(user.getLanguage())));
                    itemMap.put("key", graveLeaveEarlyMins);
                    itemMap.put("value", graveLeaveEarlyMins);
                    itemList.add(itemMap);
                } else {
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("label", SystemEnv.getHtmlLabelName(10000823, Util.getIntValue(user.getLanguage())));
                    itemMap.put("key", leaveEarlyMins);
                    itemMap.put("value", leaveEarlyMins);
                    itemList.add(itemMap);
                }
                dataList.add(itemList);
            }
            resultMap.put("data", dataList);
            if ("2".equals(tabKey)) {
                if (resourceId.equals("" + user.getUID())) {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(10000824, Util.getIntValue(user.getLanguage())));
                } else {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(513177, user.getLanguage()).replace("{lastName}", lastName));
                }
            } else {
                if (resourceId.equals("" + user.getUID())) {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(10000825, Util.getIntValue(user.getLanguage())));
                } else {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(513178, user.getLanguage()).replace("{lastName}", lastName));
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取漏签的明细
     *
     * @return
     */
    private Map<String, Object> getForgotCheckList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            //由于手机端无法一次性获取所有得数据，需要分批次获取
            int index = Util.getIntValue((String) params.get("index"), 0);//从第几个开始获取
            int pageSize = Util.getIntValue((String) params.get("pageSize"), 10);//一次获取多少个

            TransMethod transMethod = new TransMethod();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
            List<Object> dataList = new ArrayList<Object>();

            String backFields = " * ";
            String sqlFrom = " from kq_format_detail ";
            String sqlWhere = "where 1=1 and (forgotCheckMins>0 or forgotBeginWorkCheckMins>0) ";
            String orderBy = " kqdate asc, workbegintime asc ";
            if (!"".equals(resourceId)) {
                sqlWhere += " and resourceId=" + resourceId;
            }
            if (!"".equals(searchDateFrom)) {
                sqlWhere += " and kqDate>='" + searchDateFrom + "' ";
            }
            if (!"".equals(searchDateTo)) {
                sqlWhere += " and kqDate<='" + searchDateTo + "' ";
            }
            RecordSet recordSet = new RecordSet();
            String pageSql = "select * from (select tmp.*,rownum rn from ( select" + backFields + sqlFrom + sqlWhere + " order by " + orderBy + ") tmp where rownum<=" + (pageSize + index) + ") c where rn>=" + (index + 1);
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                pageSql = "select t.* from ( select " + backFields + ",ROW_NUMBER() OVER( order by " + orderBy + ") rn " + sqlFrom + sqlWhere + ") t where 1=1 and rn>=" + (index + 1) + " and rn<=" + (pageSize + index);
            } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + index + "," + pageSize;
            }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                pageSql = "select " + backFields + sqlFrom + sqlWhere + " order by " + orderBy + " limit " + pageSize + " offset " + index;
            }
            recordSet.executeQuery(pageSql);
            if (pageSize > recordSet.getCounts()) {
                resultMap.put("isEnd", true);
            } else {
                resultMap.put("isEnd", false);
            }
            while (recordSet.next()) {
                String kqDate = recordSet.getString("kqDate");
                String serialId = recordSet.getString("serialId");
                String workBeginTime = recordSet.getString("workBeginTime");
                String workEndTime = recordSet.getString("workEndTime");
                String signInTime = recordSet.getString("signInTime");
                String signOutTime = recordSet.getString("signOutTime");
                String forgotCheckMins = recordSet.getString("forgotcheckmins");
                String forgotBeginWorkCheckMins = recordSet.getString("forgotBeginWorkCheckMins");
                int forgotCheck = 0;
                if (Util.getIntValue(forgotCheckMins, 0) > 0) {
                    forgotCheck++;
                }
                if (Util.getIntValue(forgotBeginWorkCheckMins, 0) > 0) {
                    forgotCheck++;
                }

                String serialInfo = transMethod.getSerailName(serialId, workBeginTime + "+" + workEndTime);
                String signInInfo = transMethod.getReportDetialSignInTime(serialId, signInTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());
                String signOutInfo = transMethod.getReportDetialSignOutTime(serialId, signOutTime + "+" + kqDate + "+" + resourceId + "+" + user.getLanguage());

                itemList = new ArrayList<Map<String, Object>>();
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", "");
                itemMap.put("key", "kqDate");
                itemMap.put("value", kqDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390054, user.getLanguage()));
                itemMap.put("key", "serialInfo");
                itemMap.put("value", serialInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390495, user.getLanguage()));
                itemMap.put("key", "signInInfo");
                itemMap.put("value", signInInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(390496, user.getLanguage()));
                itemMap.put("key", "signOutInfo");
                itemMap.put("value", signOutInfo);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(34099, user.getLanguage()));
                itemMap.put("key", "forgotCheck");
                itemMap.put("value", forgotCheck);
                itemList.add(itemMap);
                dataList.add(itemList);
            }
            resultMap.put("data", dataList);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(10000826, Util.getIntValue(user.getLanguage())));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513180, user.getLanguage()).replace("{lastName}", lastName));
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取出差、公出、请假的明细
     *
     * @return
     */
    private Map<String, Object> getLeaveList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            String kqType = Util.null2String(params.get("kqtype"));

            Map<String, Object> paramsMap = new HashMap<String, Object>();
            paramsMap.put("typeselect", "6");
            paramsMap.put("tabKey", "1");
            paramsMap.put("resourceId", resourceId);
            paramsMap.put("fromDate", searchDateFrom);
            paramsMap.put("toDate", searchDateTo);
            paramsMap.put("isMyKQ", "1");
            if (kqType.equalsIgnoreCase("evection")) {//出差
                paramsMap.put("kqtype", KqSplitFlowTypeEnum.EVECTION.getFlowtype());
                if (resourceId.equals("" + user.getUID())) {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(10000827, Util.getIntValue(user.getLanguage())));
                } else {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(513182, user.getLanguage()).replace("{lastName}", lastName));
                }
            } else if (kqType.equalsIgnoreCase("outDays")) {//公出
                paramsMap.put("kqtype", KqSplitFlowTypeEnum.OUT.getFlowtype());
                if (resourceId.equals("" + user.getUID())) {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(10000828, Util.getIntValue(user.getLanguage())));
                } else {
                    resultMap.put("title", SystemEnv.getHtmlLabelName(513181, user.getLanguage()).replace("{lastName}", lastName));
                }
            } else if (kqType.startsWith("leaveType_")) {
                String[] typeInfo = Util.splitString(kqType, "_");
                if (typeInfo[0].equals("leaveType")) {
                    paramsMap.put("kqtype", KqSplitFlowTypeEnum.LEAVE.getFlowtype());
                    paramsMap.put("newleavetype", typeInfo[1]);
                    if (resourceId.equals("" + user.getUID())) {
                        resultMap.put("title", SystemEnv.getHtmlLabelName(10000829, Util.getIntValue(user.getLanguage())));
                    } else {
                        resultMap.put("title", SystemEnv.getHtmlLabelName(513184, user.getLanguage()).replace("{lastName}", lastName));
                    }
                }
            }

            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();

            KQAttFlowSetBiz kqAttFlowSetBiz = new KQAttFlowSetBiz();
            Map<String, String> sqlMap = kqAttFlowSetBiz.getFLowSql(paramsMap, user);
            String sqlFrom = sqlMap.get("from");
            String sqlWhere = sqlMap.get("where");

            String sql = "select * " + sqlFrom + sqlWhere;
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String requestId = recordSet.getString("requestId");
                String requestName = recordSet.getString("requestName");
                int durationRule = Util.getIntValue(recordSet.getString("durationrule"));
                String unitName = "";
                if (durationRule == 3 || durationRule == 5 || durationRule == 6) {
                    unitName = SystemEnv.getHtmlLabelName(389326, user.getLanguage());
                } else {
                    unitName = SystemEnv.getHtmlLabelName(389325, user.getLanguage());
                }
                double duration = Util.getDoubleValue(recordSet.getString("duration"), 0.00);
                double backDuration = Util.getDoubleValue(recordSet.getString("backDuraion"), 0.00);
                double period = duration - backDuration;
                if (period <= 0) {
                    continue;
                }

                itemMap = new HashMap<String, Object>();
                itemMap.put("label", requestName);
                itemMap.put("period", String.format("%.2f", period) + unitName);
                itemMap.put("requestId", requestId);
                itemList.add(itemMap);
            }
            resultMap.put("data", itemList);
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    /**
     * 获取加班的明细
     *
     * @return
     */
    private Map<String, Object> getOverTimeList() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String resourceId = Util.null2String(params.get("resourceId"));
            if ("".equals(resourceId)) {
                resourceId = "" + user.getUID();
            }
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String searchDateSelect = Util.null2String(params.get("searchDateselect"));//日期范围
            String searchDateFrom = Util.null2String(params.get("searchDatefrom"));//开始日期
            String searchDateTo = Util.null2String(params.get("searchDateto"));//结束日期
            if (!searchDateSelect.equals("") && !searchDateSelect.equals("0") && !searchDateSelect.equals("6")) {
                searchDateFrom = TimeUtil.getDateByOption(searchDateSelect, "0");
                searchDateTo = TimeUtil.getDateByOption(searchDateSelect, "1");
            }
            String computingMode = Util.null2s((String) params.get("computingMode"), "0");

            Map<String, Object> data4ComputingMode = new TreeMap<String, Object>(
                    new Comparator<String>() {
                        @Override
                        public int compare(String obj1, String obj2) {
                            // 降序排序
                            return obj2.compareTo(obj1);
                        }
                    }
            );
            if ("0".equalsIgnoreCase(computingMode) || "1".equalsIgnoreCase(computingMode)) {
                Map<String, Object> data4ComputingMode1 = getOvertime4ComputingMode1(resourceId, searchDateFrom, searchDateTo);
                data4ComputingMode.putAll(data4ComputingMode1);
            }
            if ("0".equalsIgnoreCase(computingMode) || "2".equalsIgnoreCase(computingMode)) {
                Map<String, Object> data4ComputingMode2 = getOvertime4ComputingMode2(resourceId, searchDateFrom, searchDateTo);
                data4ComputingMode.putAll(data4ComputingMode2);
            }
            if ("0".equalsIgnoreCase(computingMode) || "3".equalsIgnoreCase(computingMode)) {
                Map<String, Object> data4ComputingMode3 = getOvertime4ComputingMode3(resourceId, searchDateFrom, searchDateTo);
                data4ComputingMode.putAll(data4ComputingMode3);
            }
            if ("0".equalsIgnoreCase(computingMode) || "4".equalsIgnoreCase(computingMode)) {
              Map<String, Object> data4ComputingMode4 = getOvertime4ComputingMode4(resourceId, searchDateFrom, searchDateTo);
              data4ComputingMode.putAll(data4ComputingMode4);
            }

            List<Object> dataList = new ArrayList<Object>();
            for (Map.Entry entry : data4ComputingMode.entrySet()) {
                dataList.add(entry.getValue());
            }

            resultMap.put("data", dataList);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(10000830, Util.getIntValue(user.getLanguage())));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513185, user.getLanguage()).replace("{lastName}", lastName));
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    private Map<String, Object> getOvertime4ComputingMode1(String resourceId, String searchDateFrom, String searchDateTo) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String paidLeaveEnable = Util.null2String(params.get("paidLeaveEnable"));

            RecordSet recordSet = new RecordSet();
            String dbType = recordSet.getDBType();
            String overtimeTable = "";
            String belongDateWhere = "";
            if (searchDateFrom.length() > 0 && searchDateTo.length() > 0) {
                belongDateWhere += " and ( belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' " +
                        " or belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' " +
                        " or '" + searchDateFrom + "' between belongdate and belongdate " +
                        " or '" + searchDateTo + "' between belongdate and belongdate) ";
            }
            String paidLeaveEnableWhere = "";
            if ("0".equalsIgnoreCase(paidLeaveEnable) || "1".equalsIgnoreCase(paidLeaveEnable)) {
                paidLeaveEnableWhere += " and paidLeaveEnable=" + paidLeaveEnable;
            }
            if ("oracle".equalsIgnoreCase(dbType)||"postgresql".equalsIgnoreCase(dbType)) {
                overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,to_char(sum(duration_min)) duration_min from kq_flow_overtime where computingmode=1 " +
                        (belongDateWhere.length() == 0 ? "" : belongDateWhere) + (paidLeaveEnableWhere.length() == 0 ? "" : paidLeaveEnableWhere) + " group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate ";
            } else if ("mysql".equalsIgnoreCase(dbType)) {
                overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,CONVERT (sum(duration_min), CHAR ) duration_min from kq_flow_overtime where computingmode=1 " +
                        (belongDateWhere.length() == 0 ? "" : belongDateWhere) + (paidLeaveEnableWhere.length() == 0 ? "" : paidLeaveEnableWhere) + " group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate ";
            } else {
                overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,convert(varchar,sum(cast(duration_min as NUMERIC)))  duration_min  from kq_flow_overtime where computingmode=1 " +
                        (belongDateWhere.length() == 0 ? "" : belongDateWhere) + (paidLeaveEnableWhere.length() == 0 ? "" : paidLeaveEnableWhere) + " group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate ";
            }

            String backFields = " * ";
            String fromSql = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from (" + overtimeTable + ") a left join hrmresource b on a.resourceid = b.id) f ";
            String sqlWhere = " 1=1 ";
            if (resourceId.length() > 0) {
                sqlWhere += " and resourceId in(" + resourceId + ")";
            }
            if ("0".equalsIgnoreCase(paidLeaveEnable) || "1".equalsIgnoreCase(paidLeaveEnable)) {
                sqlWhere += " and paidLeaveEnable=" + paidLeaveEnable;
            }
            String orderBy = " computingMode ";
            String sql = "select " + backFields + " from " + fromSql + " where " + sqlWhere + " order by " + orderBy;
            resultMap = executeSQL(sql, "1");
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    private Map<String, Object> getOvertime4ComputingMode2(String resourceId, String searchDateFrom, String searchDateTo) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String paidLeaveEnable = Util.null2String(params.get("paidLeaveEnable"));

            String belongDateWhere = "";
            if (searchDateFrom.length() > 0 && searchDateTo.length() > 0) {
                belongDateWhere += " and ( belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' or belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' "
                        + " or '" + searchDateFrom + "' between belongdate and belongdate or '" + searchDateTo + "' between belongdate and belongdate) ";
            }
            String paidLeaveEnableWhere = "";
            if ("0".equalsIgnoreCase(paidLeaveEnable) || "1".equalsIgnoreCase(paidLeaveEnable)) {
                paidLeaveEnableWhere += " and paidLeaveEnable=" + paidLeaveEnable;
            }
            String overtimeTable = " select requestid,resourceid,fromdate,fromtime,todate,totime,computingmode,paidLeaveEnable,expiringdate,duration_min from kq_flow_overtime where computingmode = 2  " +
                    (belongDateWhere.length() == 0 ? "" : belongDateWhere) +
                    (paidLeaveEnableWhere.length() == 0 ? "" : paidLeaveEnableWhere);

            String backFields = " * ";
            String fromSql = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from (" + overtimeTable + ") a left join hrmresource b on a.resourceid = b.id) f ";
            String sqlWhere = " 1=1 ";
            String orderBy = " computingMode ";
            if (resourceId.length() > 0) {
                sqlWhere += " and resourceId in(" + resourceId + ")";
            }
            String sql = "select " + backFields + " from " + fromSql + " where " + sqlWhere + " order by " + orderBy;
            resultMap = executeSQL(sql, "2");
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    private Map<String, Object> getOvertime4ComputingMode3(String resourceId, String searchDateFrom, String searchDateTo) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String paidLeaveEnable = Util.null2String(params.get("paidLeaveEnable"));

            String belongDateWhere = "";
            if (searchDateFrom.length() > 0 && searchDateTo.length() > 0) {
                belongDateWhere += " and ( belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' or belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' "
                        + " or '" + searchDateFrom + "' between belongdate and belongdate or '" + searchDateTo + "' between belongdate and belongdate) ";
            }
            String paidLeaveEnableWhere = "";
            if ("0".equalsIgnoreCase(paidLeaveEnable) || "1".equalsIgnoreCase(paidLeaveEnable)) {
                paidLeaveEnableWhere += " and paidLeaveEnable=" + paidLeaveEnable;
            }
            String overtimeTable = " select requestid,resourceid,fromdate,fromtime,todate,totime,computingmode,paidLeaveEnable,expiringdate,duration_min from kq_flow_overtime where computingmode = 3  " +
                    (belongDateWhere.length() == 0 ? "" : belongDateWhere) +
                    (paidLeaveEnableWhere.length() == 0 ? "" : paidLeaveEnableWhere);

            String backFields = " * ";
            String fromSql = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from (" + overtimeTable + ") a left join hrmresource b on a.resourceid = b.id) f ";
            String sqlWhere = " 1=1 ";
            String orderBy = " computingmode ";
            if (resourceId.length() > 0) {
                sqlWhere += " and resourceId in(" + resourceId + ")";
            }
            String sql = "select " + backFields + " from " + fromSql + " where " + sqlWhere + " order by " + orderBy;
            resultMap = executeSQL(sql, "3");
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    private Map<String, Object> getOvertime4ComputingMode4(String resourceId, String searchDateFrom, String searchDateTo) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String paidLeaveEnable = Util.null2String(params.get("paidLeaveEnable"));

            String belongDateWhere = "";
            if (searchDateFrom.length() > 0 && searchDateTo.length() > 0) {
                belongDateWhere += " and ( belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' or belongdate between '" + searchDateFrom + "' and '" + searchDateTo + "' "
                        + " or '" + searchDateFrom + "' between belongdate and belongdate or '" + searchDateTo + "' between belongdate and belongdate) ";
            }
            String paidLeaveEnableWhere = "";
            if ("0".equalsIgnoreCase(paidLeaveEnable) || "1".equalsIgnoreCase(paidLeaveEnable)) {
                paidLeaveEnableWhere += " and paidLeaveEnable=" + paidLeaveEnable;
            }
            String overtimeTable = " select requestid,resourceid,fromdate,fromtime,todate,totime,computingmode,paidLeaveEnable,expiringdate,duration_min from kq_flow_overtime where computingmode = 4  " +
                    (belongDateWhere.length() == 0 ? "" : belongDateWhere) +
                    (paidLeaveEnableWhere.length() == 0 ? "" : paidLeaveEnableWhere);

            String backFields = " * ";
            String fromSql = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from (" + overtimeTable + ") a left join hrmresource b on a.resourceid = b.id) f ";
            String sqlWhere = " 1=1 ";
            String orderBy = " computingmode ";
            if (resourceId.length() > 0) {
                sqlWhere += " and resourceId in(" + resourceId + ")";
            }
            String sql = "select " + backFields + " from " + fromSql + " where " + sqlWhere + " order by " + orderBy;
            resultMap = executeSQL(sql, "4");
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }

    private Map<String, Object> executeSQL(String sql, String computingMode) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            String computingModeShow = "";
            if ("1".equals(computingMode)) {
                computingModeShow += SystemEnv.getHtmlLabelName(500382, user.getLanguage());//需审批，以加班流程为准
            } else if ("2".equals(computingMode)) {
                computingModeShow += SystemEnv.getHtmlLabelName(500383, user.getLanguage());//需审批，以打卡为准，但是不能超过加班流程时长
            } else if ("3".equals(computingMode)) {
                computingModeShow += SystemEnv.getHtmlLabelName(390837, user.getLanguage());//无需审批，根据打卡时间计算加班时长
            } else if ("4".equals(computingMode)) {
              computingModeShow += SystemEnv.getHtmlLabelName(524827, user.getLanguage());//打卡审批交集
            }

            KQTransMethod kqTransMethod = new KQTransMethod();
            TransMethod transMethod = new TransMethod();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
            List<Object> dataList = new ArrayList<Object>();

            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                String requestId = recordSet.getString("requestId");
                String resourceId = recordSet.getString("resourceId");
                String fromDate = recordSet.getString("fromdate");
                String fromTime = recordSet.getString("fromtime");
                String toDate = recordSet.getString("todate");
                String toTime = recordSet.getString("toTime");
                String duration_min = transMethod.getDuration_minByUnit(recordSet.getString("duration_min"));
                String paidLeaveEnable = transMethod.getPaidLeaveEnable(recordSet.getString("PaidLeaveEnable"), "" + user.getLanguage());

                String key = requestId + "|" + resourceId + "|" + fromDate + "|" + fromTime + "|" + toDate + "|" + toTime;

                itemList = new ArrayList<Map<String, Object>>();
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(740, user.getLanguage()));
                itemMap.put("key", "fromDate");
                itemMap.put("value", fromDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(742, user.getLanguage()));
                itemMap.put("key", "fromTime");
                itemMap.put("value", fromTime);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(741, user.getLanguage()));
                itemMap.put("key", "toDate");
                itemMap.put("value", toDate);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(743, user.getLanguage()));
                itemMap.put("key", "toTime");
                itemMap.put("value", toTime);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(509604, user.getLanguage()));
                itemMap.put("key", "duration_min");
                itemMap.put("value", duration_min);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(125804, user.getLanguage()));
                itemMap.put("key", "paidLeaveEnable");
                itemMap.put("value", paidLeaveEnable);
                itemList.add(itemMap);
                itemMap = new HashMap<String, Object>();
                itemMap.put("label", SystemEnv.getHtmlLabelName(26408, user.getLanguage()));
                itemMap.put("key", "computingMode");
                itemMap.put("value", computingModeShow);
                itemList.add(itemMap);

                resultMap.put(key, itemList);
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }
}
