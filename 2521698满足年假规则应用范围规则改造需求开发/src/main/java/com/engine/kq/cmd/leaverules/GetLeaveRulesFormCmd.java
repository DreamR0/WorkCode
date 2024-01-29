package com.engine.kq.cmd.leaverules;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import weaver.conn.RecordSet;
import weaver.filter.XssUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 假期规则--获取新建编辑的表单
 */
public class GetLeaveRulesFormCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetLeaveRulesFormCmd(Map<String, Object> params, User user) {
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
        try {
            boolean canEdit = HrmUserVarify.checkUserRight("KQLeaveRulesEdit:Edit", user);//是否具有编辑权限
            if (!canEdit) {
                resultMap.put("status", "-1");
                resultMap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return resultMap;
            }
            /**假期类型的相关信息：*/
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();

            /*新建假期规则时选择完请假类型后会再次请求此表单接口*/
            /*规则对应的假期类型的ID*/
            String ruleId = Util.null2String(params.get("typeId"));

            /*是否启用：0-未启用、1-启用*/
            int isEnable = 1;

            /*最小请假单位：1-按天请假、2-按半天请假、3-按小时请假、4-按整天请假*/
            int minimumUnit = 1;

            /******************************************************************************/

            /**假期规则详情的相关信息：*/

            /*是否是编辑*/
            boolean isEdit = false;

            /*假期规则详情的ID*/
            String ruleDetailId = Util.null2String(params.get("ruleId"));

            /*假期规则名称*/
            String ruleName = "";

            /*应用范围：0-总部、1-分部*/
            int scopeType = 0;

            /*应用范围为分部时，选择的分部ID*/
            String scopeValue = "";

            //qc2521698 年假界定判定日期
            String scopeDate = "";

            /*余额发放方式：1-手动发放、2-按司龄自动发放、3-按工龄自动发放、4-每年自动发放固定天数、5-加班时长自动计入余额、6-按工龄+司龄自动发放*/
            int distributionMode = 1;

            //年假基数计算方式：
            // 0(精确计算)-假期基数发放日期和假期基数变动日期均为每年的01月01号(但是假期基数是结合 司龄/工龄 变化前后的值按天数比例折算出来的)、
            // 1(按最少的余额计算)-假期基数发放日期和假期基数变动日期均为每年的01月01号、
            // 2(按最多的余额计算)-假期基数发放日期发放日期为每年的01月01号，假期基数的变动日期为每年的 入职日期/参加工作日期
            int calcMethod = 1;

            //是否折算：0-不折算、1-四舍五入、2-向上取整、3-向下取整、4-向上取0.5的倍数、向下取0.5的倍数
            int convertMode = 1;

            //折算后的小数点位数
            int decimalDigit = 2;

            /*每人发放小时(天)数(当余额发放方式为是每年发放固定天数时有效)*/
            double annualAmount = 0;

            //法定年假规则：0-工龄、1-司龄、2-工龄+司龄 (当余额发放方式为按工龄+司龄自动发放时有效)
            String legalKey = "0";

            //福利年假规则：0-工龄、1司龄、2-工龄+司龄 (当余额发放方式为按工龄+司龄自动发放时有效)
            String welfareKey = "1";

            /*扣减优先级：1-法定年假、2-福利年假(当余额发放方式为按工龄+司龄自动发放时有效)*/
            int priority = 1;

            //有效期规则：0-永久有效、1-按自然年（1月1日-12月31日）、2-按入职日期起12个月、3-自定义次年失效日期、4、按天数失效、5-按季度失效、6-按月数失效
            int validityRule = 0;

            /*失效日期--月（当有效期规则选择自定义次年失效日期时有效）*/
            String expirationMonth = "1";

            /*失效日期--日（当有效期规则选择自定义次年失效日期时有效）*/
            String expirationDay = "1";

            //有效天数（当有效期规则选择自定义有效天数时)
            String effectiveDays = "30";

            //有效月数（当有效期规则选择自定义有效月数时）
            String effectiveMonths = "1";

            /*允许延长有效期：0-不允许、1-允许*/
            int extensionEnable = 0;

            /*允许超过有效期天数*/
            int extendedDays = 90;

            /*释放规则：0-不限制、1-按天释放、2-按月释放*/
            int releaseRule = 0;

            //是否需要排除次账号：0--不排除，即次账号正常享受年假、1--排除，即次账号不能享受年假
            int excludeSubAccount = 1;

            //转正之前是否允许发放假期余额：0-不允许、1-允许
            int beforeFormal = 1;

            /*入职时长--年假*/
            Map<String, Object> entryMap = new HashMap<String, Object>();
            List entryList = new ArrayList();

            /*工龄--年假*/
            Map<String, Object> workingAgeMap = new HashMap<String, Object>();
            List workingAgeList = new ArrayList();

            /*入职时长+工龄混合--年假*/
            Map<String, Object> mixedModeMap = new HashMap<String, Object>();
            List mixedModeList = new ArrayList();

            if (!ruleDetailId.equals("")) {
                String sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete != 1) and id=" + ruleDetailId;
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql);
                if (recordSet.next()) {
                    isEdit = true;

                    ruleName = recordSet.getString("ruleName");
                    ruleId = recordSet.getString("ruleId");
                    scopeType = recordSet.getInt("scopeType");
                    scopeValue = recordSet.getString("scopeValue");
                    distributionMode = Util.getIntValue(recordSet.getString("distributionMode"), 1);
                    annualAmount = Util.getDoubleValue(recordSet.getString("annualAmount"), 0);
                    legalKey = "" + Util.getIntValue(recordSet.getString("legalKey"), 0);
                    welfareKey = "" + Util.getIntValue(recordSet.getString("welfareKey"), 1);
                    priority = Util.getIntValue(recordSet.getString("priority"), 1);
                    validityRule = Util.getIntValue(recordSet.getString("validityRule"), 0);
                    effectiveDays = Util.null2s(recordSet.getString("effectiveDays"), "30");
                    effectiveMonths = Util.null2s(recordSet.getString("effectiveMonths"),"1");
                    expirationMonth = Util.null2s(recordSet.getString("expirationMonth"), "1");
                    expirationDay = Util.null2s(recordSet.getString("expirationDay"), "1");
                    extensionEnable = Util.getIntValue(recordSet.getString("extensionEnable"), 0);
                    extendedDays = Util.getIntValue(recordSet.getString("extendedDays"), 90);
                    releaseRule = Util.getIntValue(recordSet.getString("releaseRule"), 0);
                    calcMethod = Util.getIntValue(recordSet.getString("calcMethod"), 1);
                    convertMode = Util.getIntValue(recordSet.getString("convertMode"), 1);
                    excludeSubAccount = Util.getIntValue(recordSet.getString("excludeSubAccount"), 1);
                    beforeFormal = Util.getIntValue(recordSet.getString("beforeFormal"), 1);
                    scopeDate =Util.null2String(recordSet.getString("scopeDate"));
                }

                if (distributionMode == 2 || distributionMode == 7) {
                    sql = "select * from kq_EntryToLeave where leaveRulesId = ? order by lowerLimit,upperLimit";
                    recordSet.executeQuery(sql, ruleDetailId);
                    int lowerLimit = 0;//入职年限下限
                    int upperLimit = 0;//入职年限上限
                    double amount = 0;//假期天数
                    while (recordSet.next()) {
                        lowerLimit = recordSet.getInt("lowerLimit");
                        upperLimit = recordSet.getInt("upperLimit");
                        amount = Util.getDoubleValue(recordSet.getString("amount"), 0);

                        entryMap = new HashMap<String, Object>();
                        entryMap.put("timePoint", lowerLimit);
                        entryMap.put("amount", String.format("%.2f", amount));
                        entryList.add(entryMap);
                    }
                    resultMap.put("detailRule", entryList);
                }

                if (distributionMode == 3) {
                    sql = "select * from kq_WorkingAgeToLeave where leaveRulesId = ? order by lowerLimit,upperLimit";
                    recordSet.executeQuery(sql, ruleDetailId);
                    int lowerLimit = 0;//工龄下限
                    int upperLimit = 0;//工龄上限
                    double amount = 0;//假期天数
                    while (recordSet.next()) {
                        lowerLimit = recordSet.getInt("lowerLimit");
                        upperLimit = recordSet.getInt("upperLimit");
                        amount = Util.getDoubleValue(recordSet.getString("amount"), 0);

                        workingAgeMap = new HashMap<String, Object>();
                        workingAgeMap.put("timePoint", lowerLimit);
                        workingAgeMap.put("amount", String.format("%.2f", amount));
                        workingAgeList.add(workingAgeMap);
                    }
                    resultMap.put("detailRule", workingAgeList);
                }

                if (distributionMode == 6) {
                    sql = "select * from kq_MixModeToLegalLeave where leaveRulesId=? order by id ";
                    recordSet.executeQuery(sql, ruleDetailId);
                    double limit1 = 0;//工龄下限
                    double limit2 = 0;//司龄下限
                    double amount = 0;//法定年假天数or福利年假天数
                    while (recordSet.next()) {
                        limit1 = Util.getDoubleValue(recordSet.getString("limit1"), 0);
                        limit2 = Util.getDoubleValue(recordSet.getString("limit2"), 0);
                        amount = Util.getDoubleValue(recordSet.getString("amount"), 0);

                        mixedModeMap = new HashMap<String, Object>();
                        if (legalKey.equals("0") || legalKey.equals("2")) {
                            mixedModeMap.put("workYear", limit1);
                        }
                        if (legalKey.equals("1") || legalKey.equals("2")) {
                            mixedModeMap.put("entryTime", limit2);
                        }
                        mixedModeMap.put("legalAmount", String.format("%.2f", amount));
                        mixedModeList.add(mixedModeMap);
                    }
                    resultMap.put("legalRule", mixedModeList);
                    resultMap.put("legalKey", legalKey);

                    mixedModeList = new ArrayList();
                    sql = "select * from kq_MixModeToWelfareLeave where leaveRulesId=? order by id ";
                    recordSet.executeQuery(sql, ruleDetailId);
                    while (recordSet.next()) {
                        limit1 = Util.getDoubleValue(recordSet.getString("limit1"), 0);
                        limit2 = Util.getDoubleValue(recordSet.getString("limit2"), 0);
                        amount = Util.getDoubleValue(recordSet.getString("amount"), 0);

                        mixedModeMap = new HashMap<String, Object>();
                        if (welfareKey.equals("0") || welfareKey.equals("2")) {
                            mixedModeMap.put("workYear", limit1);
                        }
                        if (welfareKey.equals("1") || welfareKey.equals("2")) {
                            mixedModeMap.put("entryTime", limit2);
                        }
                        mixedModeMap.put("welfareAmount", String.format("%.2f", amount));
                        mixedModeList.add(mixedModeMap);
                    }
                    resultMap.put("welfareRule", mixedModeList);
                    resultMap.put("welfareKey", welfareKey);
                }
            }

            if (isEdit) {
                isEnable = Util.getIntValue(rulesComInfo.getIsEnable(ruleId), 1);
            }

            List<Map<String, Object>> groupList = new ArrayList<Map<String, Object>>();
            Map<String, Object> groupItem = new HashMap<String, Object>();
            List<Object> itemList = new ArrayList<Object>();
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            SearchConditionItem searchConditionItem = null;
            HrmFieldBean hrmFieldBean = null;

            /****************************************************基本信息****************************************************/

            groupItem.put("title", SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
            groupItem.put("defaultshow", true);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("typeId");//假期类型
            hrmFieldBean.setFieldlabel("129811");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(ruleId);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 || isEdit ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
            if (!isEdit) {
                rulesComInfo.setTofirstRow();
                while (rulesComInfo.next()) {
                    if (!rulesComInfo.getIsEnable().equals("1") || !rulesComInfo.getBalanceEnable().equals("1")) {
                        continue;
                    }
                    optionsList.add(new SearchConditionOption(rulesComInfo.getId(), Util.formatMultiLang(rulesComInfo.getLeaveName(), "" + user.getLanguage()), ruleId.equals(rulesComInfo.getId())));
                }
            } else {
                optionsList.add(new SearchConditionOption(ruleId, Util.formatMultiLang(rulesComInfo.getLeaveName(ruleId), "" + user.getLanguage()), true));
            }
            searchConditionItem.setOptions(optionsList);
            searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(505298, user.getLanguage()));//只能选择启用状态下开启了假期余额的假期类型，并且编辑假期规则时不能变更假期类型
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("ruleName");//规则名称
            hrmFieldBean.setFieldlabel("19829");
            hrmFieldBean.setFieldhtmltype("1");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(ruleName);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            /*应用范围是否能够选择总部，如果已经新建过总部的假期规则，则无法新建总部的假期规则*/
            boolean canSelectCom = true;
            /*已经新建过某分部的假期规则，则无法继续新建该分部的假期规则*/
            String selectedSubcomIds = "";
            /*新建假期规则的时候选择完假期类型后重亲请求了此接口*/
            if (!ruleId.equals("") && !isEdit) {
                String sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=?";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql, ruleId);
                while (recordSet.next()) {
                    int scopeTypeTemp = Util.getIntValue(recordSet.getString("scopeType"), 0);
                    String scopeValueTemp = recordSet.getString("scopeValue");

                    if (scopeTypeTemp == 0) {
                        canSelectCom = false;
                    }
                    if (scopeTypeTemp == 1) {
                        selectedSubcomIds += "," + scopeValueTemp;
                    }
                }
            }
            selectedSubcomIds = selectedSubcomIds.length() > 0 ? selectedSubcomIds.substring(1) : "";

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("scopeType");//此规则适用范围
            hrmFieldBean.setFieldlabel("19374");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 || !canSelectCom ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(140, user.getLanguage()), scopeType == 0 || !canSelectCom));
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(33553, user.getLanguage()), scopeType == 1));
            searchConditionItem.setOptions(optionsList);
            //对于一个请假类型，各分部能够且仅能够设置一个属于本分部的假期规则，如果未设置本分部的假期规则，默认取总部的假期规则，如果总部也未设置，则假期基数视作0.00
            searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(505299, user.getLanguage()));
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("scopeValue");//分部
            hrmFieldBean.setFieldlabel("33553");
            hrmFieldBean.setFieldhtmltype("3");
            hrmFieldBean.setType("170");
            hrmFieldBean.setFieldvalue(scopeValue);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "KQLeaveRulesAdd:Add");
            if (selectedSubcomIds.length() > 0) {
                XssUtil xssUtil = new XssUtil();
                String sqlWhere = " id not in (" + selectedSubcomIds + ") ";
                searchConditionItem.getBrowserConditionParam().getDataParams().put("sqlWhere", xssUtil.put(sqlWhere));
                searchConditionItem.getBrowserConditionParam().getCompleteParams().put("sqlWhere", xssUtil.put(sqlWhere));
                searchConditionItem.getBrowserConditionParam().getDestDataParams().put("sqlWhere", xssUtil.put(sqlWhere));
            }
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            //qc2521698 多一个年假界定判断日期
            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("scopeDate");
            hrmFieldBean.setFieldlabel(new BaseBean().getPropValue("qc2521698","lableName"));
            hrmFieldBean.setFieldhtmltype("3");
            hrmFieldBean.setType("2");
            hrmFieldBean.setFieldvalue(scopeDate);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            groupItem.put("items", itemList);
            groupList.add(groupItem);

            /****************************************************发放规则****************************************************/

            groupItem = new HashMap<String, Object>();
            itemList = new ArrayList<Object>();
            groupItem.put("title", SystemEnv.getHtmlLabelName(508539, user.getLanguage()));
            groupItem.put("defaultshow", true);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("distributionMode");//余额发放方式
            hrmFieldBean.setFieldlabel("388946");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr((isEnable == 0 || (isEdit && distributionMode == 6)) ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(388947, user.getLanguage()), distributionMode == 1));
            optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(390374, user.getLanguage()), distributionMode == 2));
            optionsList.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(388949, user.getLanguage()), distributionMode == 3));
            optionsList.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(390323, user.getLanguage()), distributionMode == 4));
            optionsList.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(388951, user.getLanguage()), distributionMode == 5));
            SearchConditionOption searchConditionOption = new SearchConditionOption("6", SystemEnv.getHtmlLabelName(390822, user.getLanguage()), distributionMode == 6);
            searchConditionOption.setDisabled((isEdit && distributionMode != 6) ? true : false);
            optionsList.add(searchConditionOption);
            optionsList.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(514025, user.getLanguage()), distributionMode == 7));
            optionsList.add(new SearchConditionOption("8", SystemEnv.getHtmlLabelName(536880, user.getLanguage()), distributionMode == 8));
            searchConditionItem.setOptions(optionsList);
            searchConditionItem.setHelpfulTip("★" + SystemEnv.getHtmlLabelName(10000815,user.getLanguage()) + "★" + SystemEnv.getHtmlLabelName(501107, user.getLanguage()));
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            List<String> distributionModeTips = new ArrayList<String>();
            distributionModeTips.add(SystemEnv.getHtmlLabelName(389735, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511045, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(500952, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511045, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(500953, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511046, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(389736, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511045, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(389737, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511045, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(500954, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511046, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(514026, user.getLanguage()) + " <font color=\"#FF0000\"><b>" + SystemEnv.getHtmlLabelName(511045, user.getLanguage()) + "</b></font>");
            distributionModeTips.add(SystemEnv.getHtmlLabelName(536881, user.getLanguage()));
            resultMap.put("distributionMode", distributionModeTips);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("calcMethod");//假期基数计算方式
            hrmFieldBean.setFieldlabel("501121");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(505302, user.getLanguage()), calcMethod == 0));
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(505303, user.getLanguage()), calcMethod == 1));
            optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(505304, user.getLanguage()), calcMethod == 2));
            searchConditionItem.setOptions(optionsList);
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            List<String> calcMethodTips = new ArrayList<String>();
            //以入职日期(或参加工作日期)为分隔点将一年划分为上半年和下半年，全年可用假期天数=上半年天数/全年总天数*上半年司龄(或工龄)对应的假期天数+下半年天数/全年总天数*下半年司龄(或工龄)对应的假期天数。每年1月1日自动发放假期天数。
            calcMethodTips.add(SystemEnv.getHtmlLabelName(505305, user.getLanguage()));
            //每年1月1日计算员工的司龄(或工龄)，取对应的假期天数，于1月1日自动发放。
            calcMethodTips.add(SystemEnv.getHtmlLabelName(505306, user.getLanguage()));
            //每年1月1日计算员工的司龄(或工龄)，取对应的假期天数，于1月1日自动发放。若一年中员工司龄(或工龄)增加后，对应的假期天数也随之增加，则自动补发增加的假期天数。
            calcMethodTips.add(SystemEnv.getHtmlLabelName(505307, user.getLanguage()));
            resultMap.put("calcMethod", calcMethodTips);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("priority");//扣减优先级
            hrmFieldBean.setFieldlabel("2093");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(129819, user.getLanguage()), priority == 1));
            optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(132046, user.getLanguage()), priority == 2));
            searchConditionItem.setOptions(optionsList);
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("annualAmount");//每人发放小时数
            hrmFieldBean.setFieldlabel("503237");
            hrmFieldBean.setFieldhtmltype("1");
            hrmFieldBean.setType("2");
            hrmFieldBean.setFieldvalue(annualAmount);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|numeric");
            searchConditionItem.setPrecision(2);
            searchConditionItem.setMin("0");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("convertMode");//是否折算
            hrmFieldBean.setFieldlabel("508419");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(508423, user.getLanguage()), convertMode == 0));
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(389654, user.getLanguage()), convertMode == 1));
            optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(508424, user.getLanguage()), convertMode == 2));
            optionsList.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(508425, user.getLanguage()), convertMode == 3));
            optionsList.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(508426, user.getLanguage()), convertMode == 4));
            optionsList.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(508427, user.getLanguage()), convertMode == 5));
            searchConditionItem.setOptions(optionsList);
            //当假期基数计算方式选择【精确计算】时，不能选择【不折算】
            searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(510491, user.getLanguage()));
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            List<String> convertModeTips = new ArrayList<String>();
            //指不考虑是否是入职当年还是初始获得年（即年初01月01日的时候计算工龄或者司龄，得出对应的假期基数为0。但是当这一年的工龄或者司龄增加后，对应的假期基数就不再是0了，这样的年份称作初始获得年），通过工龄或司龄计算出对应的假期基数，不做任何扣减折算
            convertModeTips.add(SystemEnv.getHtmlLabelName(510131, user.getLanguage()));
            //通过工龄或司龄计算出对应的假期基数后（若是入职当年或初始获得年会进行扣减折算），折算后四舍五入保留两位小数
            convertModeTips.add(SystemEnv.getHtmlLabelName(510132, user.getLanguage()));
            //通过工龄或司龄计算出对应的假期基数后（若是入职当年或初始获得年会进行扣减折算），折算后的数值取整，例如：折算后的基数为3.21，取整后为4.0
            convertModeTips.add(SystemEnv.getHtmlLabelName(510133, user.getLanguage()));
            //通过工龄或司龄计算出对应的假期基数后（若是入职当年或初始获得年会进行扣减折算），折算后的数值取整，例如：折算后的基数为3.21，取整后为3.0
            convertModeTips.add(SystemEnv.getHtmlLabelName(510134, user.getLanguage()));
            //通过工龄或司龄计算出对应的假期基数后（若是入职当年或初始获得年会进行扣减折算），折算后的数值取0.5的倍数，例如：折算后的基数为3.21，最终为3.5
            convertModeTips.add(SystemEnv.getHtmlLabelName(510135, user.getLanguage()));
            //通过工龄或司龄计算出对应的假期基数后（若是入职当年或初始获得年会进行扣减折算），折算后的数值取0.5的倍数，例如：折算后的基数为3.21，最终后为3.0
            convertModeTips.add(SystemEnv.getHtmlLabelName(510136, user.getLanguage()));
            resultMap.put("convertModeTips", convertModeTips);

            groupItem.put("items", itemList);
            groupList.add(groupItem);

            /****************************************************有效期****************************************************/

            groupItem = new HashMap<String, Object>();
            itemList = new ArrayList<Object>();
            groupItem.put("title", SystemEnv.getHtmlLabelName(15030, user.getLanguage()));
            groupItem.put("defaultshow", true);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("validityRule");//有效期规则
            hrmFieldBean.setFieldlabel("388952");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(22135, user.getLanguage()), validityRule == 0));
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(388953, user.getLanguage()), validityRule == 1));
            optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(388954, user.getLanguage()), validityRule == 2));
            optionsList.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(389739, user.getLanguage()), validityRule == 3));
            optionsList.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(508428, user.getLanguage()), validityRule == 4));
            optionsList.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(513525, user.getLanguage()), validityRule == 5));
            optionsList.add(new SearchConditionOption("6", SystemEnv.getHtmlLabelName(515135, user.getLanguage()), validityRule == 6));
            optionsList.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(536941, user.getLanguage()), validityRule == 7));
            searchConditionItem.setOptions(optionsList);
            searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(515354, user.getLanguage()));
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("effectiveDays");//有效月数
            hrmFieldBean.setFieldlabel("132356");
            hrmFieldBean.setFieldhtmltype("1");
            hrmFieldBean.setType("2");
            hrmFieldBean.setFieldvalue(effectiveDays);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|integer");
            searchConditionItem.setMin("1");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("effectiveMonths");//有效月数
            hrmFieldBean.setFieldlabel("515174");
            hrmFieldBean.setFieldhtmltype("1");
            hrmFieldBean.setType("2");
            hrmFieldBean.setFieldvalue(effectiveMonths);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|integer");
            searchConditionItem.setMin("1");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("expirationMonth");//失效日期--月
            hrmFieldBean.setFieldlabel("390103");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(expirationMonth);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            for (int i = 1; i <= 12; i++) {
                if(user.getLanguage()==8){
                    optionsList.add(new SearchConditionOption("" + i, i+"", Util.getIntValue(expirationMonth, 1) == i));
                }else {
                optionsList.add(new SearchConditionOption("" + i, i + SystemEnv.getHtmlLabelName(383373, user.getLanguage()), Util.getIntValue(expirationMonth, 1) == i));
                }
            }
            searchConditionItem.setOptions(optionsList);
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("expirationDay");//失效日期--日
            hrmFieldBean.setFieldlabel("390103");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(expirationDay);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            for (int i = 1; i <= 31; i++) {
                if(user.getLanguage()==8){
                    optionsList.add(new SearchConditionOption("" + i, i+"", Util.getIntValue(expirationMonth, 1) == i));
                }else {
                    optionsList.add(new SearchConditionOption("" + i, i + SystemEnv.getHtmlLabelName(390, user.getLanguage()), Util.getIntValue(expirationDay, 1) == i));
                }
            }
            searchConditionItem.setOptions(optionsList);
            searchConditionItem.setRules("required|string");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("extensionEnable");//允许延长有效期
            hrmFieldBean.setFieldlabel("388955");
            hrmFieldBean.setFieldhtmltype("4");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(extensionEnable);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 2);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            Map<String, Object> otherParamsMap = new HashMap<String, Object>();
            otherParamsMap.put("display", "switch");
            searchConditionItem.setOtherParams(otherParamsMap);
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("extendedDays");//允许延长的天数
            hrmFieldBean.setFieldlabel("389198");
            hrmFieldBean.setFieldhtmltype("1");
            hrmFieldBean.setType("2");
            hrmFieldBean.setFieldvalue(extendedDays);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|integer");
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            groupItem.put("items", itemList);
            groupList.add(groupItem);

            /****************************************************其他设置****************************************************/

            groupItem = new HashMap<String, Object>();
            itemList = new ArrayList<Object>();
            groupItem.put("title", SystemEnv.getHtmlLabelName(20824, user.getLanguage()));
            groupItem.put("defaultshow", true);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("releaseRule");//释放规则
            hrmFieldBean.setFieldlabel("389093");
            hrmFieldBean.setFieldhtmltype("5");
            hrmFieldBean.setType("1");
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            optionsList = new ArrayList<SearchConditionOption>();
            optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(32499, user.getLanguage()), releaseRule == 0));
            optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(127263, user.getLanguage()), releaseRule == 1));
            optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(127262, user.getLanguage()), releaseRule == 2));
            searchConditionItem.setOptions(optionsList);
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(510129, user.getLanguage()));
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("excludeSubAccount");//次账号发放假期余额
            hrmFieldBean.setFieldlabel("510174");
            hrmFieldBean.setFieldhtmltype("4");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(excludeSubAccount);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 2);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            otherParamsMap = new HashMap<String, Object>();
            otherParamsMap.put("display", "switch");
            searchConditionItem.setOtherParams(otherParamsMap);
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("beforeFormal");//转正之前发放假期余额
            hrmFieldBean.setFieldlabel("510175");
            hrmFieldBean.setFieldhtmltype("4");
            hrmFieldBean.setType("1");
            hrmFieldBean.setFieldvalue(beforeFormal);
            hrmFieldBean.setIsFormField(true);
            hrmFieldBean.setViewAttr(isEnable == 0 ? 1 : 2);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            otherParamsMap = new HashMap<String, Object>();
            otherParamsMap.put("display", "switch");
            searchConditionItem.setOtherParams(otherParamsMap);
            if (hrmFieldBean.getViewAttr() == 1) {
                Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
                OtherParamsMap.put("hasBorder", true);
                searchConditionItem.setOtherParams(OtherParamsMap);
            }
            itemList.add(searchConditionItem);

            groupItem.put("items", itemList);
            groupList.add(groupItem);
            resultMap.put("condition", groupList);
            resultMap.put("isEnable", "" + isEnable);
            if (isEdit) {
                minimumUnit = Util.getIntValue(rulesComInfo.getMinimumUnit(ruleId), 1);

                String unitName = "";//单位名称，天/小时
                if (minimumUnit == 1 || minimumUnit == 2 || minimumUnit == 4) {
                    unitName = SystemEnv.getHtmlLabelName(1925, user.getLanguage());//天
                } else {
                    unitName = SystemEnv.getHtmlLabelName(391, user.getLanguage());//小时
                }
                resultMap.put("unitName", unitName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}
