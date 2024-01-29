package com.engine.kq.cmd.myattendance;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2476717.Util.ReplaceCardUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import org.apache.commons.lang3.StringUtils;
import weaver.common.DateUtil;
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
public class GetHrmKQReportInfoCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetHrmKQReportInfoCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
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
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            /**
             * 判断是否有权限
             */
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"), 0);
            int departmentId = Util.getIntValue((String) params.get("departmentId"), 0);
            String resourceId = Util.null2String(params.get("resourceId"));
            if (resourceId.equals("")) {
                resourceId = "" + user.getUID();
            }
            String lastName = resourceComInfo.getResourcename(resourceId);
            if (resourceId.equals("" + user.getUID())) {
                resultMap.put("title", SystemEnv.getHtmlLabelName(513095, user.getLanguage()));
            } else {
                resultMap.put("title", SystemEnv.getHtmlLabelName(515562, user.getLanguage()).replace("{name}", lastName));
            }
            if (subCompanyId == 0 || departmentId == 0) {
                subCompanyId = Util.getIntValue(resourceComInfo.getSubCompanyID(resourceId), 0);
                departmentId = Util.getIntValue(resourceComInfo.getDepartmentID(resourceId), 0);
            }
            if (!resourceId.equals("" + user.getUID()) && !isIndirectSuperId(resourceId, user.getUID() + "", resourceComInfo) && !HrmUserVarify.checkUserRight("HrmResource:Absense", user, departmentId)) {
                resultMap.put("status", "-1");
                resultMap.put("hasRight", false);
                return resultMap;
            }
            String fromDate = Util.null2String(params.get("fromDate"));
            String toDate = Util.null2String(params.get("toDate"));

            Calendar today = Calendar.getInstance();
            /**今年*/
            String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
            /**上一年*/
            String lastYearDate = Util.add0(today.get(Calendar.YEAR) - 1, 4) + "-12-31";

            /**
             * 拼凑前端所属数据格式
             */
            Map<String, Object> groupMap = new HashMap<String, Object>();
            List<Object> groupItemList = new ArrayList<Object>();
            Map<String, Object> groupItem = new HashMap<String, Object>();
            List<Object> itemList = new ArrayList<Object>();
            Map<String, Object> itemMap = new HashMap<String, Object>();
            List<Object> itemList5 = new ArrayList<Object>();
            Map<String, Object> groupItem5 = new HashMap<String, Object>();
            /**
             * 出勤统计
             * workDays：应出勤天数
             * realWorkDays：实际出勤天数
             * leaveDays：缺勤天数
             */
            double workDays = 0.00;
            double realWorkDays = 0.00;
            double leaveDays = 0.00;
            /**
             * beLate：迟到次数
             * graveBeLate；严重迟到次数
             * leaveEearly：早退次数
             * graveLeaveEarly：严重迟到次数
             * absenteeism：旷工次数
             * forgotCheck：漏签次数
             */
            int beLate = 0;
            int graveBeLate = 0;
            int leaveEearly = 0;
            int graveLeaveEarly = 0;
            int absenteeism = 0;
            int forgotCheck = 0;
            /**
             * or_minimumUnit：最小加班单位
             * or_unitName：(小时)/(天)
             * tr_minimumUnit：最小出差单位
             * tr_unitName：(小时)/(天)
             * er_minimumUnit：最小公出单位
             * er_unitName：(小时)/(天)
             */
            int or_minimumUnit = KQOvertimeRulesBiz.getMinimumUnit();//最小加班单位
            String or_unitName = (or_minimumUnit == 3 || or_minimumUnit == 5 || or_minimumUnit == 6) ? SystemEnv.getHtmlLabelName(389326, user.getLanguage()) : SystemEnv.getHtmlLabelName(389325, user.getLanguage());
            KQTravelRulesComInfo kqTravelRulesComInfo = new KQTravelRulesComInfo();
            int tr_minimumUnit = Util.getIntValue(kqTravelRulesComInfo.getMinimumUnit("1"));//最小出差单位
            String tr_unitName = tr_minimumUnit == 3 ? SystemEnv.getHtmlLabelName(389326, user.getLanguage()) : SystemEnv.getHtmlLabelName(389325, user.getLanguage());
            KQExitRulesComInfo kqExitRulesComInfo = new KQExitRulesComInfo();
            int er_minimumUnit = Util.getIntValue(kqExitRulesComInfo.getMinimumUnit("1"));//最小公出单位
            String er_unitName = er_minimumUnit == 3 ? SystemEnv.getHtmlLabelName(389326, user.getLanguage()) : SystemEnv.getHtmlLabelName(389325, user.getLanguage());
            /**
             * overtimeTotal：加班时长
             * businessLeave：出差时长
             * officialBusiness：公出时长
             */
            double overtimeTotal = 0.00;//加班时长
            double businessLeave = 0.00;//出差时长
            double officialBusiness = 0.00;//公出时长
            /**
             * 记录请假时长(考虑到人员调整部门之后，考勤汇总报表会存在两条记录，用于累计请假时长)
             */
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            Map<String, Object> leaveMap = new HashMap<String, Object>();
            /**
             * 获取考勤的相关统计数据，数据来源于考勤汇总报表，在service层调用了考勤汇总报表的接口重新封装了数据传入此接口
             * 调用的考勤汇总报表的接口为：com/engine/kq/cmd/report/GetKQReportCmd.java
             */
            List<Map<String, Object>> datasList = new ArrayList<Map<String, Object>>();
            Map<String, Object> datasMap = new HashMap<>();

            datasList = (List<Map<String, Object>>) params.get("datas");
            new BaseBean().writeLog("==zj==(移动端我的考勤)" + JSON.toJSONString(datasList));
            for (int i = 0; datasList != null && i < datasList.size(); i++) {
                datasMap = (Map<String, Object>) datasList.get(i);

                workDays += Util.getDoubleValue((String) datasMap.get("workdays"), 0.00);//应出勤天数
                realWorkDays += Util.getDoubleValue((String) datasMap.get("attendancedays"), 0.00);//实际出勤天数
                leaveDays = workDays - realWorkDays;//缺勤天数

                beLate += Util.getIntValue((String) datasMap.get("beLate"), 0);//迟到次数
                graveBeLate += Util.getIntValue((String) datasMap.get("graveBeLate"), 0);//严重迟到次数
                leaveEearly += Util.getIntValue((String) datasMap.get("leaveEearly"), 0);//早退次数
                graveLeaveEarly += Util.getIntValue((String) datasMap.get("graveLeaveEarly"), 0);//严重迟到次数
                absenteeism += Util.getIntValue((String) datasMap.get("absenteeism"), 0);//旷工次数
                forgotCheck += Util.getIntValue((String) datasMap.get("forgotCheck"), 0);//漏签次数

                overtimeTotal += Util.getDoubleValue((String) datasMap.get("overtimeTotal"), 0.00);//加班时长
                businessLeave += Util.getDoubleValue((String) datasMap.get("businessLeave"), 0.00);//出差时长
                officialBusiness += Util.getDoubleValue((String) datasMap.get("officialBusiness"), 0.00);//公出时长

                rulesComInfo.setTofirstRow();
                while (i == 0 && rulesComInfo.next()) {
                    double value = Util.getDoubleValue(String.valueOf(leaveMap.get("leaveType_" + rulesComInfo.getId())), 0.00);
                    value += Util.getDoubleValue((String) datasMap.get("leaveType_" + rulesComInfo.getId()), 0.00);//请假时长
                    leaveMap.put("leaveType_" + rulesComInfo.getId(), value);
                }
            }

            /**统计应出勤天数*/
            KQWorkTime kqWorkTime = new KQWorkTime();
            if (!toDate.equals("") && toDate.compareTo(currentDate) > 0 && Util.getIntValue(resourceComInfo.getStatus(resourceId), 0) < 4) {
                workDays = 0.00;
                if (!fromDate.equals("") && fromDate.compareTo(currentDate) > 0) {
                    //查询没有到的月份，缺勤天数以及出勤天数应该都为0
                    realWorkDays = 0.00;
                    leaveDays = 0.00;
                }

                boolean isEnd = false;
                for (String date = fromDate; !isEnd; ) {
                    if (date.compareTo(toDate)>=0) {
                        isEnd = true;
                    }

                    boolean isWorkDay = kqWorkTime.isWorkDay(resourceId, date);
                    if (isWorkDay) {
                        workDays += 1.00;
                    }

                    date = DateUtil.getDate(date, 1);
                }
            }

            /**********************************************************************************************************/

            itemList = new ArrayList<Object>();
            itemMap = new HashMap<String, Object>();
            itemMap.put("id", "leaveDays");
            itemMap.put("name", SystemEnv.getHtmlLabelName(506345, user.getLanguage()));//未出勤
            itemMap.put("title", SystemEnv.getHtmlLabelName(506345, user.getLanguage()));
            itemMap.put("type", "ABSENT");
            itemMap.put("value", String.format("%.2f", leaveDays));
            itemList.add(itemMap);

            itemMap = new HashMap<String, Object>();
            itemMap.put("id", "realWorkDays");
            itemMap.put("name", SystemEnv.getHtmlLabelName(130566, user.getLanguage()));//实际出勤
            itemMap.put("title", SystemEnv.getHtmlLabelName(130566, user.getLanguage()));
            itemMap.put("type", "REALWORKDAYS");
            itemMap.put("value", String.format("%.2f", realWorkDays));
            itemList.add(itemMap);

            groupItem = new HashMap<String, Object>();
            groupItem.put("id", "workDays");
            groupItem.put("name", SystemEnv.getHtmlLabelName(513089, user.getLanguage()));//应出勤(天)
            groupItem.put("title", SystemEnv.getHtmlLabelName(16732, user.getLanguage()));
            groupItem.put("value", String.format("%.2f", workDays));
            groupItem.put("items", itemList);
            resultMap.put("groupitem1", groupItem);

            /**********************************************************************************************************/

            itemList = new ArrayList<Object>();
            itemMap = new HashMap<String, Object>();
            itemMap.put("name", "evection");
            itemMap.put("title", SystemEnv.getHtmlLabelName(20084, user.getLanguage()) + tr_unitName);//出差
            itemMap.put("type", "EVECTION");
            itemMap.put("value",  String.format("%.2f",businessLeave));
            itemList.add(itemMap);

            //qc2476717 这里把移动端指定的数据隐藏
            int isMobile = Util.getIntValue(Util.null2String(params.get("ismobile")),0);
            new BaseBean().writeLog("==zj==(isMobile)" + isMobile);
            Boolean isShow = true;
            if(1 == isMobile){
                isShow = false;
            }

            //qc2476717 这里把移动端指定的数据隐藏
            if (isShow){
                itemMap = new HashMap<String, Object>();
                itemMap.put("name", "outDays");
                itemMap.put("title", SystemEnv.getHtmlLabelName(24058, user.getLanguage()) + er_unitName);//公出
                itemMap.put("type", "OUTDAYS");
                itemMap.put("value",String.format("%.2f",officialBusiness));
                itemList.add(itemMap);
            }


            itemMap = new HashMap<String, Object>();
            itemMap.put("name", "overTimes");
            itemMap.put("title", SystemEnv.getHtmlLabelName(6151, user.getLanguage()) + or_unitName);//加班
            itemMap.put("type", "OVERTIME");
            itemMap.put("value", String.format("%.2f",overtimeTotal));
            itemList.add(itemMap);

            groupItem = new HashMap<String, Object>();
            groupItem.put("items", itemList);
            resultMap.put("groupitem2", groupItem);

            /**********************************************************************************************************/

            itemList = new ArrayList<Object>();
            itemMap = new HashMap<String, Object>();
            itemMap.put("name", "beLate");
            itemMap.put("title", SystemEnv.getHtmlLabelName(34089, user.getLanguage()));//迟到
            itemMap.put("type", "BELATE");
            itemMap.put("value", beLate);
            itemList.add(itemMap);

            //qc2476717 这里把移动端指定的数据隐藏
            if (isShow){
                itemMap = new HashMap<String, Object>();
                itemMap.put("name", "graveBeLate");
                itemMap.put("title", SystemEnv.getHtmlLabelName(535131, user.getLanguage()));//严重迟到
                itemMap.put("type", "graveBeLate");
                itemMap.put("value", graveBeLate);
                itemList.add(itemMap);
            }


            itemMap = new HashMap<String, Object>();
            itemMap.put("name", "leaveEarly");
            itemMap.put("title", SystemEnv.getHtmlLabelName(34098, user.getLanguage()));//早退
            itemMap.put("type", "LEAVEEARLY");
            itemMap.put("value", leaveEearly);
            itemList.add(itemMap);

            //qc2476717 这里把移动端指定的数据隐藏
            if (isShow){
                itemMap = new HashMap<String, Object>();
                itemMap.put("name", "graveLeaveEarly");
                itemMap.put("title", SystemEnv.getHtmlLabelName(535132, user.getLanguage()));//严重早退
                itemMap.put("type", "graveLeaveEarly");
                itemMap.put("value", graveLeaveEarly);
                itemList.add(itemMap);
            }

            //qc2476717 这里添加补卡数据
            new BaseBean().writeLog("==zj==(我的考勤日期范围)" + fromDate + "    |    " + toDate);
            ReplaceCardUtil replaceCardUtil = new ReplaceCardUtil();
            String remindCardValue = replaceCardUtil.getRemindCard(user, fromDate, toDate);
            //这里对补卡详情地址进行拼接
            String detailBegin = "";
            String detailEnd = "";
            String belongDate = fromDate.substring(0,7);
            //这里判断是否使用测试地址还是正式地址
            String isOpen = new BaseBean().getPropValue("qc2476717","isOpen");
            if ("0".equals(isOpen)){
                //使用测试环境地址
                 detailBegin = new BaseBean().getPropValue("qc2476717","remindAdress");
                 detailEnd = "&id="+user.getUID()+"&date="+belongDate+new BaseBean().getPropValue("qc2476717","remindAdressEnd");
            }else {
                detailBegin = new BaseBean().getPropValue("qc2476717","remindAdress1");
                detailEnd = "&id="+user.getUID()+"&date="+belongDate+new BaseBean().getPropValue("qc2476717","remindAdressEnd1");
            }

            itemMap = new HashMap<String, Object>();
                itemMap.put("name", "remindCard");
                itemMap.put("title", "补卡（次）");//补卡
                itemMap.put("type", "remindCard");
                itemMap.put("value",remindCardValue);
                itemMap.put("userid",user.getUID());
                itemMap.put("remindCardDetail",detailBegin+detailEnd);
                itemList.add(itemMap);



            itemMap = new HashMap<String, Object>();
            itemMap.put("name", "absentFromWork");
            itemMap.put("title", SystemEnv.getHtmlLabelName(10000344, user.getLanguage()));//旷工
            itemMap.put("type", "ABSENT");
            itemMap.put("value", absenteeism);
            itemList.add(itemMap);

            //qc2476717 移动端就隐藏
            if (isShow){
                itemMap = new HashMap<String, Object>();
                itemMap.put("name", "noSign");
                itemMap.put("title", SystemEnv.getHtmlLabelName(34099, user.getLanguage()));//漏签
                itemMap.put("type", "noSign");
                itemMap.put("value", forgotCheck);
                itemList.add(itemMap);
            }


            groupItem = new HashMap<String, Object>();
            groupItem.put("items", itemList);
            resultMap.put("groupitem3", groupItem);

            /**********************************************************************************************************/

            rulesComInfo.setTofirstRow();
            while (rulesComInfo.next()) {
                if (!rulesComInfo.getIsEnable().equals("1") || !rulesComInfo.getBalanceEnable().equals("1")) {
                    continue;
                }
                String scopeType = rulesComInfo.getScopeType();
                if (scopeType.equals("1")) {
                    String scopeValue = rulesComInfo.getScopeValue();
                    List<String> scopeValueList = Util.TokenizerString(scopeValue, ",");
                    if (scopeValueList.indexOf("" + resourceComInfo.getSubCompanyID(resourceId)) < 0) {
                        continue;
                    }
                }

                String allRestAmount = KQBalanceOfLeaveBiz.getRestAmount("" + resourceId, rulesComInfo.getId(), currentDate, true, true);
                String currentRestAmount = KQBalanceOfLeaveBiz.getRestAmount("" + resourceId, rulesComInfo.getId(), currentDate, true, false);
                String beforeRestAmount = String.format("%.2f", Util.getDoubleValue(allRestAmount, 0) - Util.getDoubleValue(currentRestAmount, 0));
                itemList = new ArrayList<Object>();
                if(KQSettingsBiz.show_split_balance()) {
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("title", (KQUnitBiz.isLeaveHour(rulesComInfo.getMinimumUnit())) ? SystemEnv.getHtmlLabelName(513286, user.getLanguage()) : SystemEnv.getHtmlLabelName(513287, user.getLanguage()));
                    itemMap.put("value", beforeRestAmount);
                    itemList.add(itemMap);

                    itemMap = new HashMap<String, Object>();
                    itemMap.put("title", (KQUnitBiz.isLeaveHour(rulesComInfo.getMinimumUnit())) ? SystemEnv.getHtmlLabelName(504395, user.getLanguage()) : SystemEnv.getHtmlLabelName(504394, user.getLanguage()));
                    itemMap.put("value", currentRestAmount);
                    itemList.add(itemMap);

                    itemMap = new HashMap<String, Object>();
                    itemMap.put("title", (KQUnitBiz.isLeaveHour(rulesComInfo.getMinimumUnit())) ? SystemEnv.getHtmlLabelName(513288, user.getLanguage()) : SystemEnv.getHtmlLabelName(513289, user.getLanguage()));
                    itemMap.put("value", allRestAmount);
                    itemList.add(itemMap);
                }else{
                    itemMap = new HashMap<String, Object>();
                    itemMap.put("title", (KQUnitBiz.isLeaveHour(rulesComInfo.getMinimumUnit())) ? SystemEnv.getHtmlLabelName(513288, user.getLanguage()) : SystemEnv.getHtmlLabelName(513289, user.getLanguage()));
                    itemMap.put("value", allRestAmount);
                    itemList.add(itemMap);
                }

                groupItem = new HashMap<String, Object>();
                groupItem.put("color", "#25C6DA");
                groupItem.put("icon", "icon-Human-resources-adjustment");
                groupItem.put("title", rulesComInfo.getLeaveName());
                groupItem.put("item", itemList);
                groupItemList.add(groupItem);
            }
            groupMap.put("items", groupItemList);
            groupMap.put("title", SystemEnv.getHtmlLabelName(132058, user.getLanguage()));
            resultMap.put("groupitem4", groupMap);

            /**********************************************************************************************************/

            rulesComInfo.setTofirstRow();
            while (rulesComInfo.next()) {
                double value = Util.getDoubleValue(String.valueOf(leaveMap.get("leaveType_" + rulesComInfo.getId())), 0.00);//请假时长
                if (value > 0) {
                    String leaveName = Util.formatMultiLang(rulesComInfo.getLeaveName(), "" + user.getLanguage());
                    int le_minimumUnit = Util.getIntValue(rulesComInfo.getMinimumUnit(), 1);//最小请假单位
                    String le_unitName = (le_minimumUnit == 3 || le_minimumUnit == 5 || le_minimumUnit == 6) ? SystemEnv.getHtmlLabelName(389326, user.getLanguage()) : SystemEnv.getHtmlLabelName(389325, user.getLanguage());

                    itemMap = new HashMap<String, Object>();
                    itemMap.put("name", "leaveType_" + rulesComInfo.getId());
                    itemMap.put("title", leaveName + le_unitName);
                    itemMap.put("type", "leaveType_" + rulesComInfo.getId());
                    itemMap.put("value", Util.toDecimalDigits("" + value, 2));
                    itemList5.add(itemMap);
                }
            }
            groupItem5.put("items", itemList5);
            resultMap.put("groupitem5", groupItem5);
            resultMap.put("status", "1");
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }
}
