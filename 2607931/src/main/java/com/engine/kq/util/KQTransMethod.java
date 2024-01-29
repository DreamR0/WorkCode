package com.engine.kq.util;

import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.*;
import com.google.common.collect.Lists;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import weaver.workflow.workflow.WorkflowRequestComInfo;

/**
 * 列表的显示转换方法
 */
public class KQTransMethod {

    /***************************************假期的相关方法***************************************/
    /**
     * 判断假期类型能否进行编辑、删除、查看日志
     * (被流程引用过的假期类型不可删除,新建过假期规则的假期类型不可删除、未启用的假期类型不可编辑)
     *
     * @param ruleId 假期类型的ID
     * @param param  canEdit:canDelete:canLog
     * @return
     */
    public ArrayList<String> getLeaveRulesOperate(String ruleId, String param) {
        ArrayList<String> resultList = new ArrayList<String>();
        String[] allParam = param.split(":");
        String canEdit = allParam[0];
        String canDelete = allParam[1];
        String canLog = allParam[2];
        boolean leaveTypeUsed = false;
        boolean isEnable = true;
        boolean hasLeaveRules = false;
        try {
            String sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=?";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql, ruleId);
            if (recordSet.next()) {
                hasLeaveRules = true;
            }

            leaveTypeUsed = KQFlowDataBiz.leaveTypeUsed(ruleId);//判断是否有发起过该请假类型的请假流程
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            isEnable = rulesComInfo.getIsEnable(ruleId).equals("1");//判断该请假是否启用
        } catch (Exception e) {
            e.printStackTrace();
        }
        resultList.add(isEnable ? canEdit : "false");//是否可以编辑
        resultList.add((leaveTypeUsed || hasLeaveRules) ? "false" : canDelete);//是否可以删除
        resultList.add(canLog);//是否可以查看日志
        return resultList;
    }

    /**
     * 判断假期类型是否可以勾选，即是否可以删除
     * (被流程引用过的假期类型不可删除、新建过假期规则的假期类型不可删除)
     *
     * @param ruleId 假期类型ID
     * @return
     */
    public String getLeaveRulesCheckbox(String ruleId) {
        boolean leaveTypeUsed = false;
        boolean hasLeaveRules = false;
        try {
            String sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=?";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql, ruleId);
            if (recordSet.next()) {
                hasLeaveRules = true;
            }

            leaveTypeUsed = KQFlowDataBiz.leaveTypeUsed(ruleId);//判断是否有发起过该请假类型的请假流程
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(!leaveTypeUsed && !hasLeaveRules);
    }

    /**
     * 获取最小请假单位
     * 1-按天请假
     * 2-按半天请假
     * 3-按小时请假
     * 4-按整天请假
     *
     * @param minimumUnit
     * @param languageId
     * @return
     */
    public String getMinimumUnitName4Browser(String minimumUnit, String languageId) {
        String minimumUnitName = "";
        int language = Util.getIntValue(languageId, 7);
        switch (minimumUnit) {
            case "1":
                minimumUnitName = SystemEnv.getHtmlLabelName(1925, language);
                break;
            case "2":
                minimumUnitName = SystemEnv.getHtmlLabelName(128559, language);
                break;
            case "3":
                minimumUnitName = SystemEnv.getHtmlLabelName(391, language);
                break;
            case "4":
                minimumUnitName = SystemEnv.getHtmlLabelName(390728, language);
                break;
            case "5":
              minimumUnitName = SystemEnv.getHtmlLabelName(124952, language);
              break;
            case "6":
              minimumUnitName = SystemEnv.getHtmlLabelName(529675, language);
              break;
            default:
                minimumUnitName = "";
                break;
        }
        return minimumUnitName;
    }

    /**
     * 获取最小请假单位
     * 1-按天请假
     * 2-按半天请假
     * 3-按小时请假
     * 4-按整天请假
     *
     * @param minimumUnit
     * @param languageId
     * @return
     */
    public String getMinimumUnitName(String minimumUnit, String languageId) {
        String minimumUnitName = "";
        int language = Util.getIntValue(languageId, 7);
        switch (minimumUnit) {
            case "1":
                minimumUnitName = SystemEnv.getHtmlLabelName(388885, language);
                break;
            case "2":
                minimumUnitName = SystemEnv.getHtmlLabelName(388886, language);
                break;
            case "3":
                minimumUnitName = SystemEnv.getHtmlLabelName(388887, language);
                break;
            case "4":
                minimumUnitName = SystemEnv.getHtmlLabelName(389680, language);
                break;
            case "5":
              minimumUnitName = SystemEnv.getHtmlLabelName(514712, language);
              break;
            case "6":
              minimumUnitName = SystemEnv.getHtmlLabelName(514786, language);
              break;
            default:
                minimumUnitName = "";
                break;
        }
        return minimumUnitName;
    }

    /**
     * 计算请假时长方式
     * 1-按工作日计算请假时长
     * 2-按自然日计算请假时长
     *
     * @param computingMode
     * @param languageId
     * @return
     */
    public String getComputingModeName(String computingMode, String languageId) {
        String computingModeName = "";
        int language = Util.getIntValue(languageId, 7);
        switch (computingMode) {
            case "1":
                computingModeName = SystemEnv.getHtmlLabelName(388889, language);
                break;
            case "2":
                computingModeName = SystemEnv.getHtmlLabelName(388890, language);
                break;
            default:
                computingModeName = "";
                break;
        }
        return computingModeName;
    }

    /**
     * 获取余额规则
     * 1-手动发放
     * 2-按司龄自动发放
     * 3-按工龄自动发放
     * 4-每年自动发放固定天数
     * 5-加班时长自动计入余额
     * 6-按工龄+司龄自动发放
     *
     * @param distributionMode
     * @return
     */
    public String getDistributionModName(String distributionMode, String languageId) {
        int language = Util.getIntValue(languageId, 7);

        String distributionModeName = "";
        switch (distributionMode) {
            case "1":
                distributionModeName = SystemEnv.getHtmlLabelName(388947, language);
                break;
            case "2":
                distributionModeName = SystemEnv.getHtmlLabelName(390374, language);
                break;
            case "3":
                distributionModeName = SystemEnv.getHtmlLabelName(388949, language);
                break;
            case "4":
                distributionModeName = SystemEnv.getHtmlLabelName(388950, language);
                break;
            case "5":
                distributionModeName = SystemEnv.getHtmlLabelName(388951, language);
                break;
            case "6":
                distributionModeName = SystemEnv.getHtmlLabelName(390822, language);
                break;
            case "7":
                distributionModeName = SystemEnv.getHtmlLabelName(514025, language);
                break;
            default:
                distributionModeName = "";
                break;
        }
        return distributionModeName;
    }

    /**
     * 获取应用范围的显示名称
     *
     * @param scopeType
     * @param languageId
     * @return
     */
    public String getScopeTypeName(String scopeType, String languageId) {
        int language = Util.getIntValue(languageId, 7);
        String scopeTypeName = "";
        if (scopeType.equals("0")) {
            scopeTypeName = SystemEnv.getHtmlLabelName(140, language);
        } else if (scopeType.equals("1")) {
            scopeTypeName = SystemEnv.getHtmlLabelName(33553, language);
        }
        return scopeTypeName;
    }

    /**
     * 获取分部的名称
     *
     * @param organizationType
     * @param organizationId
     * @return
     */
    public String getOrganizationIdName(String organizationId, String organizationType) {
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        String showName = "";
        if (organizationType.equals("1")) {
            showName = subCompanyComInfo.getSubCompanyname(organizationId);
        }
        return showName;
    }

    /**
     * 获取应用范围的显示名称
     *
     * @param organizationType
     * @param languageId
     * @return
     */
    public String getOrganizationTypeName(String organizationType, String languageId) {
        int language = Util.getIntValue(languageId, 7);
        String showName = "";
        if (organizationType.equals("0")) {
            showName = SystemEnv.getHtmlLabelName(140, language);
        } else {
            showName = SystemEnv.getHtmlLabelName(141, language);
        }
        return showName;
    }

    /**
     * 获取释放规则的显示名称
     *
     * @param releaseRule
     * @param languageId
     * @return
     */
    public String getReleaseRuleName(String releaseRule, String languageId) {
        String releaseRuleName = "";
        int language = Util.getIntValue(languageId, 7);
        if (releaseRule.equals("0")) {
            releaseRuleName = SystemEnv.getHtmlLabelName(32499, language);
        } else if (releaseRule.equals("1")) {
            releaseRuleName = SystemEnv.getHtmlLabelName(390280, language);
        } else if (releaseRule.equals("2")) {
            releaseRuleName = SystemEnv.getHtmlLabelName(390281, language);
        }
        return releaseRuleName;
    }

    /**
     * 获取有效期规则的显示名称
     *
     * @param validityRule
     * @param languageId
     * @return
     */
    public String getValidityRuleShow(String validityRule, String languageId) {
        String validityRuleShow = "";
        int language = Util.getIntValue(languageId, 7);
        if (validityRule.equals("0")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(22135, language);
        } else if (validityRule.equals("1")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(388953, language);
        } else if (validityRule.equals("2")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(388954, language);
        } else if (validityRule.equals("3")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(389739, language);
        } else if (validityRule.equals("4")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(508428, language);
        } else if (validityRule.equals("5")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(513525, language);
        } else if (validityRule.equals("6")) {
            validityRuleShow = SystemEnv.getHtmlLabelName(515135, language);
        }
        return validityRuleShow;
    }

    /**
     * 获取是否启用假期余额的显示名称
     *
     * @param balanceEnable 是否显示假期余额
     * @param languageId    系统语言
     * @return
     */
    public String getBalanceEnableShow(String balanceEnable, String languageId) {
        String balanceEnableShow = "";
        try {
            int language = Util.getIntValue(languageId, 7);
            if (balanceEnable.equals("1")) {
                balanceEnableShow = SystemEnv.getHtmlLabelName(18095, language);
            } else {
                balanceEnableShow = SystemEnv.getHtmlLabelName(32386, language);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balanceEnableShow;
    }

    /**
     * 获取假期类型的显示名称
     *
     * @param typeId     假期类型的ID(对应于数据库表kq_leaveRules的主键ID)
     * @param languageId 系统语言
     * @return
     */
    public String getLeaveTypesName(String typeId, String languageId) {
        String leaveTypesName = "";
        try {
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            leaveTypesName = Util.formatMultiLang(rulesComInfo.getLeaveName(typeId), languageId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return leaveTypesName;
    }

    /***************************************员工假期余额的相关方法***************************************/

    /**
     * 为了前端校验时取值，故将分页控件的值做一下转换显示
     *
     * @param value
     * @return
     */
    public String getOriginalShow(String value) {
        BigDecimal bigDecimal = new BigDecimal(Util.null2s(value.trim(), "0"));
        return bigDecimal.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 获取当前可用的假期余额(假期余额基数*释放比例)
     *
     * @param balanceId 假期余额表的主键ID
     * @param otherPara 指定的人员ID+指定的假期规则ID+指定的所属年份+假期基数+是计算法定年假还是计算福利年假('neither'-非混合模式、legal-法定年假、welfare-福利年假)
     * @return
     */
    public String getCanUseAmount(String balanceId, String otherPara) {
        BigDecimal canUseAmount = new BigDecimal(0);
        try {
            String otherParaArr[] = otherPara.split("\\+", 5);
            String resourceId = otherParaArr[0];
            String leaveRulesId = otherParaArr[1];
            String belongYear = otherParaArr[2];
            BigDecimal baseAmount = new BigDecimal(Util.null2s(otherParaArr[3].trim(), "0"));
            String legalOrWelfare = Util.null2String(otherParaArr[4]);

            /*获取当前日期*/
            String date = DateUtil.getCurrentDate();

            canUseAmount = KQBalanceOfLeaveBiz.getCanUseAmount(resourceId, leaveRulesId, belongYear, baseAmount, legalOrWelfare, date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return canUseAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 获取剩余的假期余额(假期余额基数*释放比例+额外余额-已用余额)
     *
     * @param balanceId 假期余额表的主键ID
     * @param otherPara 指定的人员ID+指定的假期规则ID+指定的所属年份+假期基数+额外假期余额+已休假期余额+是计算法定年假还是计算福利年假(neither-非混合模式、legal-法定年假、welfare-福利年假)
     * @return
     */
    public String getRestAmount(String balanceId, String otherPara) {
        String restAmount = "0.00";
        try {
            String otherParaArr[] = otherPara.split("\\+", 7);
            String resourceId = otherParaArr[0];
            String leaveRulesId = otherParaArr[1];
            String belongYear = otherParaArr[2];
            double baseAmount = Util.getDoubleValue(otherParaArr[3], 0.00);
            double extraAmount = Util.getDoubleValue(otherParaArr[4], 0.00);
            double usedAmount = Util.getDoubleValue(otherParaArr[5], 0.00);
            String legalOrWelfare = Util.null2s(otherParaArr[6], "");

            String str = resourceId + "+" + leaveRulesId + "+" + belongYear + "+" + baseAmount + "+" + legalOrWelfare;
            double canUseAmount = Util.getDoubleValue(getCanUseAmount(balanceId, str), 0.00);

            restAmount = String.format("%.2f", canUseAmount + extraAmount - usedAmount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return restAmount;
    }

    /**
     * 判断假期余额是否有效
     *
     * @param leaveRulesId
     * @param otherParams
     * @return
     */
    public String getBalanceStatusShow(String leaveRulesId, String otherParams) {
        String statusShow = "";
        try {
            String resourceId = otherParams.split("\\+", 5)[0];
            String belongYear = otherParams.split("\\+", 5)[1];
            String languageId = otherParams.split("\\+", 5)[2];
            String effectiveDate = otherParams.split("\\+", 5)[3];
            String expirationDate = otherParams.split("\\+", 5)[4];

            /*获取今天的日期、此刻的时间*/
            Calendar today = Calendar.getInstance();
            String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            boolean status = KQBalanceOfLeaveBiz.getBalanceStatus(leaveRulesId, resourceId, belongYear, currentDate, effectiveDate, expirationDate);
            if (status) {
                statusShow = SystemEnv.getHtmlLabelName(2246, Util.getIntValue(languageId, 7));
            } else {
                statusShow = SystemEnv.getHtmlLabelName(2245, Util.getIntValue(languageId, 7));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusShow;
    }

    public String getTotalAmountShow(String baseAmount, String otherParams) {
        String extraAmount = otherParams;
        BigDecimal _baseAmount = new BigDecimal(Util.null2s(baseAmount.trim(), "0"));
        BigDecimal _extraAmount = new BigDecimal(Util.null2s(extraAmount.trim(), "0"));
        BigDecimal _totalAmount = _baseAmount.add(_extraAmount);
        String totalAmount = _totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return totalAmount;
    }

    public String getTiaoxiuamountShow(String tiaoxiuamount) {
        BigDecimal _tiaoxiuamount = new BigDecimal(Util.null2s(tiaoxiuamount, "0"));
        String totalAmount = _tiaoxiuamount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return totalAmount;
    }

    /**
     * 获取已经失效的调休值(针对单条记录而言的，不是汇总的)
     *
     * @param invalidAmount
     * @param otherParams
     * @return
     */
    public String getInvalidAmountShow(String invalidAmount, String otherParams) {
        /*获取当前日期，当前时间*/
        Calendar today = Calendar.getInstance();
        String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-" +
                Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
//      tiaoxiuamount
        String[] otherParamArr = otherParams.split("\\+", 5);
        BigDecimal baseAmount = new BigDecimal(Util.null2s(otherParamArr[0].trim(), "0"));//基数
        BigDecimal extraAmount = new BigDecimal(Util.null2s(otherParamArr[1].trim(), "0"));//额外
      BigDecimal tiaoxiuAmount = new BigDecimal(Util.null2s(otherParamArr[4].trim(), "0"));//加班生成的调休
        BigDecimal totalAmount = baseAmount.add(extraAmount).add(tiaoxiuAmount);
      BigDecimal usedAmount = new BigDecimal(Util.null2s(otherParamArr[2].trim(), "0"));//已休
      String expirationDate = Util.null2String(otherParamArr[3]).trim();
        if ("".equals(expirationDate) || expirationDate.compareTo(currentDate) >= 0) {
            invalidAmount = "0.00";
        } else {
            invalidAmount = totalAmount.subtract(usedAmount).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        return invalidAmount;
    }

    /**
     * 获取剩余的调休值(针对单条记录而言的，不是汇总的)
     *
     * @param restAmount
     * @param otherParam
     * @return
     */
    public String getRestAmountShow(String restAmount, String otherParam) {
        /*获取当前日期，当前时间*/
        Calendar today = Calendar.getInstance();
        String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-" +
                Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
        String[] otherParamArr = otherParam.split("\\+", 5);
        BigDecimal baseAmount = new BigDecimal(Util.null2s(otherParamArr[0].trim(), "0"));//基数
        BigDecimal extraAmount = new BigDecimal(Util.null2s(otherParamArr[1].trim(), "0"));//额外
      BigDecimal usedAmount = new BigDecimal(Util.null2s(otherParamArr[2].trim(), "0"));//已休
      BigDecimal tiaoxiuamount = new BigDecimal(Util.null2s(otherParamArr[4].trim(), "0"));//加班生成调休
        BigDecimal totalAmount = baseAmount.add(extraAmount).add(tiaoxiuamount);
        String expirationDate = Util.null2String(otherParamArr[3]).trim();
        if ("".equals(expirationDate) || expirationDate.compareTo(currentDate) >= 0) {
            restAmount = totalAmount.subtract(usedAmount).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } else {
            restAmount = "0.00";
        }

        return restAmount;
    }

    /**
     * 获取加班类型
     *
     * @param overtimeType
     * @param language
     * @return
     */
    public String getOvertimeTypeShow(String overtimeType, String language) {
        if (overtimeType.equals("3")) {
            //打卡生成的调休
            return SystemEnv.getHtmlLabelName(509982, Util.getIntValue(language, 7));
        } else if (overtimeType.equals("4")) {
            //加班流程生成的调休
            return SystemEnv.getHtmlLabelName(509981, Util.getIntValue(language, 7));
        } else if (overtimeType.equals("7")) {
            //Excel导入生成的调休
            return SystemEnv.getHtmlLabelName(509980, Util.getIntValue(language, 7));
        } else {
            //原有的调休
            return SystemEnv.getHtmlLabelName(509983, Util.getIntValue(language, 7));
        }
    }

    public String getAllTotalAmount(String allTotalAmount, String otherParams) {
        String[] otherParamArr = otherParams.split("\\+", 6);
        BigDecimal allBaseAmountB = new BigDecimal(Util.null2s(otherParamArr[0].trim(), "0"));
        BigDecimal allExtraAmountB = new BigDecimal(Util.null2s(otherParamArr[1].trim(), "0"));
        BigDecimal allBaseAmountC = new BigDecimal(Util.null2s(otherParamArr[2].trim(), "0"));
        BigDecimal allExtraAmountC = new BigDecimal(Util.null2s(otherParamArr[3].trim(), "0"));
        BigDecimal alltiaoxiuamountB = new BigDecimal(Util.null2s(otherParamArr[4].trim(), "0"));
        BigDecimal alltiaoxiuamountC = new BigDecimal(Util.null2s(otherParamArr[5].trim(), "0"));
        allTotalAmount = allBaseAmountB.add(allExtraAmountB).add(allBaseAmountC).add(allExtraAmountC)
            .add(alltiaoxiuamountB).add(alltiaoxiuamountC).setScale(2, RoundingMode.HALF_UP).toPlainString();
        return allTotalAmount;
    }

    public String getAllUsedAmount(String allUsedAmount, String otherParams) {
        String[] otherParamArr = otherParams.split("\\+", 2);
        BigDecimal allUsedAmountB = new BigDecimal(Util.null2s(otherParamArr[0].trim(), "0"));
        BigDecimal allUsedAmountC = new BigDecimal(Util.null2s(otherParamArr[1].trim(), "0"));
        allUsedAmount = allUsedAmountB.add(allUsedAmountC).setScale(2, RoundingMode.HALF_UP).toString();
        return allUsedAmount;
    }

    public String getAllInvalidAmount(String allInvalidAmount, String otherParams) {
        String[] otherParamArr = otherParams.split("\\+", 4);
        BigDecimal allBaseAmountC = new BigDecimal(Util.null2s(otherParamArr[0].trim(), "0"));
        BigDecimal allExtraAmountC = new BigDecimal(Util.null2s(otherParamArr[1].trim(), "0"));
        BigDecimal alltiaoxiuAmountC = new BigDecimal(Util.null2s(otherParamArr[3].trim(), "0"));
        BigDecimal allUsedAmountC = new BigDecimal(Util.null2s(otherParamArr[2].trim(), "0"));
        allInvalidAmount = allBaseAmountC.add(allExtraAmountC).add(alltiaoxiuAmountC).subtract(allUsedAmountC).setScale(2, RoundingMode.HALF_UP).toPlainString();
        return allInvalidAmount;
    }

    public String getAllRestAmount(String allRestAmount, String otherParams) {
        String[] otherParamArr = otherParams.split("\\+", 4);
        BigDecimal allBaseAmountB = new BigDecimal(Util.null2s(otherParamArr[0].trim(), "0"));
        BigDecimal allExtraAmountB = new BigDecimal(Util.null2s(otherParamArr[1].trim(), "0"));
        BigDecimal allUsedAmountB = new BigDecimal(Util.null2s(otherParamArr[2].trim(), "0"));
        BigDecimal alltiaoxiuamountB = new BigDecimal(Util.null2s(otherParamArr[3].trim(), "0"));
        allRestAmount = allBaseAmountB.add(allExtraAmountB).add(alltiaoxiuamountB).subtract(allUsedAmountB).setScale(2, RoundingMode.HALF_UP).toPlainString();
        return allRestAmount;
    }

    /**
     * 获取明细列的转换显示
     *
     * @param detaiShow
     * @param language
     * @return
     */
    public String getDetailShow(String detaiShow, String language) {
        return SystemEnv.getHtmlLabelName(17463, Util.getIntValue(language, 7));
    }

    /**
     * 获取有效期的显示
     *
     * @param expirationDate
     * @param language
     * @return
     */
    public String getExpirationDateShow(String expirationDate, String language) {
        String expirationDateShow = "";
        if ("2222-12-31".equals(expirationDate)) {
            expirationDateShow = SystemEnv.getHtmlLabelName(10000846, Util.getIntValue(Util.getIntValue(language,7)));
        } else {
            expirationDateShow = expirationDate;
        }
        return expirationDateShow;
    }

    /**
     * 获取员工假期余额明细操作日志中对象的显示名称
     *
     * @param id
     * @param params
     * @return
     */
    public String getTargetName4BalanceDetail(String id, String params) {
        String targetName = "";
        try {
            String[] paramArr = params.split("\\+");
            String resourceId = paramArr[0];
            String belongYear = paramArr[1];
            String belongMonth = paramArr[2];
            String languageId = paramArr[3];

            //人力资源缓存类
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            String lastName = Util.formatMultiLang(resourceComInfo.getResourcename(resourceId), languageId);
            targetName = SystemEnv.getHtmlLabelName(512690, Util.getIntValue(languageId, 7));
            targetName = targetName.replace("${lastName}", lastName).replace("${belongYear}", belongYear).replace("${belongMonth}", belongMonth).replace("${ID}", id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetName;
    }

    /**
     * 员工假期余额的右键日志对象名称
     *
     * @param id
     * @param otherParams
     * @return
     */
    public String getTargetName4Balance(String id, String otherParams) {
        String targetName = "";
        try {
            String[] otherParaArr = otherParams.split("\\+");
            String resourceId = otherParaArr[0];
            String leaveRulesId = otherParaArr[1];

            ResourceComInfo resourceComInfo = new ResourceComInfo();
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            String lastName = resourceComInfo.getResourcename(resourceId);
            String ruleName = rulesComInfo.getLeaveName(leaveRulesId);
            targetName = lastName + "-" + ruleName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetName;
    }

    /***************************************加班的相关方法***************************************/

    /**
     * 获取加班规则的具体的规则内容
     *
     * @param ruleId 加班规则的ID
     * @return
     */
    public String getRuleContent(String ruleId, String languageId) {
        if (ruleId.equals("")) {
            return "";
        }
        int language = Util.getIntValue(languageId, 7);
        /*需要显示的内容*/
        String resultStr = "";
        /*日期类型：1-工作日、2-周末、3-节假日*/
        int dayType = 0;
        /*是否允许加班：0-不允许、1-允许*/
        int overtimeEnable = 0;
        /*加班方式：1-需审批，以审批单为准、2-需审批，以打卡为准，但是不能超过审批时长、3-无需审批，根据打卡时间计算加班时长*/
        int computingMode = 0;
        List<String> dayTypeList = Lists.newArrayList();
        String sql = "select * from kq_OvertimeRulesDetail where ruleId=" + ruleId + " order by id";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        while (recordSet.next()) {
            dayType = recordSet.getInt("dayType");
            overtimeEnable = recordSet.getInt("overtimeEnable");
            computingMode = recordSet.getInt("computingMode");

            if(dayTypeList.contains((""+dayType))){
              continue;
            }else{
              dayTypeList.add((""+dayType));
            }

            if (dayType == 2) {//工作日
                resultStr += SystemEnv.getHtmlLabelName(28387, language) + "：";
                if (overtimeEnable == 0) {
                    resultStr += SystemEnv.getHtmlLabelName(389562, language);//不允许加班
                } else if (computingMode == 1) {
                    resultStr += SystemEnv.getHtmlLabelName(500382, language);//需审批，以加班流程为准
                } else if (computingMode == 2) {
                    resultStr += SystemEnv.getHtmlLabelName(500383, language);//需审批，以打卡为准，但是不能超过加班流程时长
                } else if (computingMode == 3) {
                    resultStr += SystemEnv.getHtmlLabelName(390837, language);//无需审批，根据打卡时间计算加班时长
                } else if (computingMode == 4) {
                  resultStr += SystemEnv.getHtmlLabelName(524827, language);//流程和打卡取交集
                }
            } else if (dayType == 3) {//休息日
                resultStr += "<br>" + SystemEnv.getHtmlLabelName(458, language) + "：";
                if (overtimeEnable == 0) {
                    resultStr += SystemEnv.getHtmlLabelName(389562, language);//不允许加班
                } else if (computingMode == 1) {
                    resultStr += SystemEnv.getHtmlLabelName(500382, language);//需审批，以加班流程为准
                } else if (computingMode == 2) {
                    resultStr += SystemEnv.getHtmlLabelName(500383, language);//需审批，以打卡为准，但是不能超过加班流程时长
                } else if (computingMode == 3) {
                    resultStr += SystemEnv.getHtmlLabelName(390837, language);//无需审批，根据打卡时间计算加班时长
                } else if (computingMode == 4) {
                  resultStr += SystemEnv.getHtmlLabelName(524827, language);//流程和打卡取交集
                }
            } else if (dayType == 1) {//节假日
                resultStr += "<br>" + SystemEnv.getHtmlLabelName(28386, language) + "：";
                if (overtimeEnable == 0) {
                    resultStr += SystemEnv.getHtmlLabelName(389562, language);//不允许加班
                } else if (computingMode == 1) {
                    resultStr += SystemEnv.getHtmlLabelName(500382, language);//需审批，以加班流程为准
                } else if (computingMode == 2) {
                    resultStr += SystemEnv.getHtmlLabelName(500383, language);//需审批，以打卡为准，但是不能超过加班流程时长
                } else if (computingMode == 3) {
                    resultStr += SystemEnv.getHtmlLabelName(390837, language);//无需审批，根据打卡时间计算加班时长
                } else if (computingMode == 4) {
                  resultStr += SystemEnv.getHtmlLabelName(524827, language);//流程和打卡取交集
                }
            }
        }
    System.out.println("getRuleContent resultStr:::"+resultStr);
        return resultStr;
    }

    /**
     * 获取加班规则的应用范围列的显示名称
     *
     * @param groupIds
     * @param languageId
     * @return
     */
    public String getGroupName(String groupIds, String languageId) {
        String groupName = "";
        int language = Util.getIntValue(languageId, 7);
        if (groupIds.equals("")) {
            groupName = SystemEnv.getHtmlLabelName(165, language);
            return groupName;
        }
        KQGroupComInfo kqGroupComInfo = new KQGroupComInfo();
        String[] groupIdArr = groupIds.split(",");
        groupName = kqGroupComInfo.getGroupname(groupIdArr[0]);
        if (groupIdArr.length > 1) {
            groupName = SystemEnv.getHtmlLabelName(516131, language).replace("{groupname}", groupName).replace("{count}", "" + groupIdArr.length);
        }
        return groupName;
    }

    /**
     * 获取加班计算单位的日志的对象名
     *
     * @param targetId
     * @param otherParams
     * @return
     */
    public String getTargetName4OvertimeUnit(String targetId, String otherParams) {
        String targetName = "";
        String languageId = otherParams.split("\\+")[0];
        targetName = SystemEnv.getHtmlLabelName(505608, Util.getIntValue(languageId, 7));
        return targetName;
    }


    /***************************************节假日设置的相关方法***************************************/

    /**
     * 根据changeType获取显示名
     *
     * @param changeType
     * @return
     */
    public String getChangeTypeName(String changeType, String languageId) {
        int language = Util.getIntValue(languageId, 7);
        String changeTypeName = "";
        if (changeType.equals("1")) {
            changeTypeName = SystemEnv.getHtmlLabelName(16478, language);
        } else if (changeType.equals("2")) {
            changeTypeName = SystemEnv.getHtmlLabelName(16751, language);
        } else if (changeType.equals("3")) {
            changeTypeName = SystemEnv.getHtmlLabelName(16752, language);
        }
        return changeTypeName;
    }

    /**
     * 获取对应工作日的显示名称
     *
     * @param relatedDay 对应工作日：0-星期一、1-星期二、2-星期三、3-星期四、4-星期五、5-星期六、6-星期日
     * @return
     */
    public String getRelatedDayName(String relatedDay, String languageId) {
        String relatedDayName = "";
        int language = Util.getIntValue(languageId, 7);
        switch (relatedDay) {
            case "6":
                relatedDayName = SystemEnv.getHtmlLabelName(398, language);
                break;
            case "0":
                relatedDayName = SystemEnv.getHtmlLabelName(392, language);
                break;
            case "1":
                relatedDayName = SystemEnv.getHtmlLabelName(393, language);
                break;
            case "2":
                relatedDayName = SystemEnv.getHtmlLabelName(394, language);
                break;
            case "3":
                relatedDayName = SystemEnv.getHtmlLabelName(395, language);
                break;
            case "4":
                relatedDayName = SystemEnv.getHtmlLabelName(396, language);
                break;
            case "5":
                relatedDayName = SystemEnv.getHtmlLabelName(397, language);
                break;
            default:
                relatedDayName = "";
                break;
        }
        return relatedDayName;
    }

    /**
     * 获取节假日设置的日志的对象名
     *
     * @param targetId
     * @param otherParams
     * @return
     */
    public String getTargetName4HolidaySet(String targetId, String otherParams) {
        String targetName = "";
        try {
            String groupId = otherParams.split("\\+")[0];
            String holidayDate = otherParams.split("\\+")[1];
            String languageId = otherParams.split("\\+")[2];

            KQGroupComInfo groupComInfo = new KQGroupComInfo();
            String groupName = Util.formatMultiLang(groupComInfo.getGroupname(groupId), languageId);
            targetName = groupName + " " + holidayDate;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetName;
    }

    /***************************************原始打卡记录的相关方法***************************************/

    /**
     * 获取考勤日期显示
     *
     * @param belongDate
     * @param languageId
     * @return
     */
    public String getBelongDateShow(String belongDate, String languageId) {
        String belongDateShow = belongDate + " ";
        int index = DateUtil.getWeek(belongDate);
        if (index == 1) {
            belongDateShow += getRelatedDayName("0", languageId);
        } else if (index == 2) {
            belongDateShow += getRelatedDayName("1", languageId);
        } else if (index == 3) {
            belongDateShow += getRelatedDayName("2", languageId);
        } else if (index == 4) {
            belongDateShow += getRelatedDayName("3", languageId);
        } else if (index == 5) {
            belongDateShow += getRelatedDayName("4", languageId);
        } else if (index == 6) {
            belongDateShow += getRelatedDayName("5", languageId);
        } else if (index == 7) {
            belongDateShow += getRelatedDayName("6", languageId);
        }
        return belongDateShow;
    }

    /**
     * 获取考勤时间显示
     *
     * @param belongTime
     * @param belongDate
     * @return
     */
    public String getBelongTimeShow(String belongTime, String belongDate, String languageId) {
        String belongTimeShow = belongDate + " " + new KQTimesArrayComInfo().turn48to24Time(belongTime);
        int language = Util.getIntValue(languageId, 7);
        if (belongTime.equals("free")) {
            belongTimeShow = belongDate + " " + SystemEnv.getHtmlLabelName(500375, language);
        }
        return belongTimeShow;
    }

    /**
     * 获取打卡时间显示
     *
     * @param signDate
     * @param signTime
     * @return
     */
    public String getSignDateShow(String signDate, String signTime) {
      String tmpsignDate = signDate.replaceAll("-", "/");
        return tmpsignDate + " " + signTime;
    }

    public String getSignDateShowNew(String signDate, String signTime) {
        String tmpsignDate = signDate.replaceAll("-", "/");
        return tmpsignDate + "  " + signTime;
    }

    /**
     * 获取原始打卡记录的数据来源列的显示名称
     *
     * @param signFrom
     * @return
     */
    public String getSignFromShow(String signFrom, String languageId) {
        String signFromShow = "";
        int language = Util.getIntValue(languageId, 7);
        if (signFrom.equalsIgnoreCase("e9pc")) {
            signFromShow = SystemEnv.getHtmlLabelName(503612, language);
        } else if (signFrom.equalsIgnoreCase("e9mobile")) {
            signFromShow = SystemEnv.getHtmlLabelName(503613, language);
        } else if (signFrom.equalsIgnoreCase("e9e")) {
            signFromShow = SystemEnv.getHtmlLabelName(503614, language);
        } else if (signFrom.equalsIgnoreCase("e9ewx")) {
            signFromShow = SystemEnv.getHtmlLabelName(503615, language);
        } else if (signFrom.startsWith("card")) {
            signFromShow = SystemEnv.getHtmlLabelName(503616, language);
        } else if (signFrom.equalsIgnoreCase("e9_mobile_out")) {
            signFromShow = SystemEnv.getHtmlLabelName(82634, language);
        } else if (signFrom.equalsIgnoreCase("importExcel")) {
            signFromShow = SystemEnv.getHtmlLabelName(503617, language);
        } else if (signFrom.equalsIgnoreCase("EMSyn")) {
            signFromShow = SystemEnv.getHtmlLabelName(503618, language);
        }else if (signFrom.equalsIgnoreCase("EMSyn_out")) {
            signFromShow = SystemEnv.getHtmlLabelName(517845, language);
        } else if (signFrom.equalsIgnoreCase("OutDataSourceSyn")) {
            signFromShow = SystemEnv.getHtmlLabelName(503619, language);
        } else if (signFrom.equalsIgnoreCase("DingTalk")) {
            signFromShow = SystemEnv.getHtmlLabelName(506318, language);
        } else if (signFrom.equalsIgnoreCase("DingTalk_out")) {
            signFromShow = SystemEnv.getHtmlLabelName(506319, language);
        } else if (signFrom.equalsIgnoreCase("Wechat_out")) {
            signFromShow = SystemEnv.getHtmlLabelName(506652, language);
        } else if (signFrom.equalsIgnoreCase("Wechat")) {
            signFromShow = SystemEnv.getHtmlLabelName(506653, language);
        } else if (signFrom.equalsIgnoreCase("auto_card")) {
            signFromShow = SystemEnv.getHtmlLabelName(521191, language);
        } else if (signFrom.equalsIgnoreCase("null")) {
            signFromShow = "";
        } else {
            signFromShow = signFrom;
        }
        return signFromShow;
    }

    /***************************************考勤报表权限共享的相关方法***************************************/

    /**
     * 获取考勤报表名称的显示
     *
     * @param reportName
     * @param languageId
     * @return
     */
    public String getReportNameShow(String reportName, String languageId) {
        String reportNameShow = "";
        int language = Util.getIntValue(languageId, 7);
        if (reportName.equals("0")) {
            reportNameShow = SystemEnv.getHtmlLabelName(332, language);
        } else if (reportName.equals("1")) {
            reportNameShow = SystemEnv.getHtmlLabelName(390351, language);
        } else if (reportName.equals("2")) {
            reportNameShow = SystemEnv.getHtmlLabelName(390352, language);
        } else if (reportName.equals("3")) {
            reportNameShow = SystemEnv.getHtmlLabelName(390248, language);
        } else if (reportName.equals("4")) {
            reportNameShow = SystemEnv.getHtmlLabelName(389441, language);
        }
        return reportNameShow;
    }

    /**
     * 获取共享级别的显示
     *
     * @param shareLevel 0-分部、1-部门、2-人员、3-岗位、4-所有人
     * @param languageId
     * @return
     */
    public String getShareLevelShow(String shareLevel, String languageId) {
        String shareLevelShow = "";
        int language = Util.getIntValue(languageId, 7);
        if (shareLevel.equals("0")) {
            shareLevelShow = SystemEnv.getHtmlLabelName(33553, language);
        } else if (shareLevel.equals("1")) {
            shareLevelShow = SystemEnv.getHtmlLabelName(27511, language);
        } else if (shareLevel.equals("2")) {
            shareLevelShow = SystemEnv.getHtmlLabelName(179, language);
        } else if (shareLevel.equals("3")) {
            shareLevelShow = SystemEnv.getHtmlLabelName(6086, language);
        } else if (shareLevel.equals("4")) {
            shareLevelShow = SystemEnv.getHtmlLabelName(1340, language);
        }
        return shareLevelShow;
    }

    /**
     * 获取共享对象的显示
     *
     * @param id
     * @param languageId
     * @return
     */
    public String getReportShareShow(String id, String languageId) {
        String showName = "";
        try {
            int language = Util.getIntValue(languageId, 7);
            String sql = "select * from kq_ReportShare where id=" + id;
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            if (recordSet.next()) {
                String shareLevel = recordSet.getString("shareLevel");
                String subcomId = Util.null2String(recordSet.getString("subcomId"));
                String deptId = Util.null2String(recordSet.getString("deptId"));
                String jobtitleId = Util.null2String(recordSet.getString("jobtitleId"));
                String userId = Util.null2String(recordSet.getString("userId"));
                String resourceType = Util.null2String(recordSet.getString("resourcetype"));
                String subcomIdCustom = Util.null2String(recordSet.getString("subcomIdCustom"));
                String deptIdCustom = Util.null2String(recordSet.getString("deptIdCustom"));

                if (shareLevel.equals("0")) {
                    SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
                    //如果对象类型是1024
                    if (resourceType.equals("1024")){
                        showName = subCompanyComInfo.getSubcompanynames(subcomIdCustom);
                    }else {
                        showName = subCompanyComInfo.getSubcompanynames(subcomId);
                    }
                } else if (shareLevel.equals("1")) {
                    DepartmentComInfo departmentComInfo = new DepartmentComInfo();
                    //如果对象类型是1024
                    if (resourceType.equals("1024")){
                        showName = departmentComInfo.getDepartmentNames(deptIdCustom);
                    }else {
                        showName = departmentComInfo.getDepartmentNames(deptId);
                    }
                } else if (shareLevel.equals("2")) {
                    ResourceComInfo resourceComInfo = new ResourceComInfo();
                    List<String> tempList = Util.TokenizerString(userId, ",");
                    for (int i = 0; i < tempList.size(); i++) {
                        showName += "," + resourceComInfo.getResourcename(tempList.get(i));
                    }
                    showName = showName.startsWith(",") ? showName.substring(1) : showName;
                } else if (shareLevel.equals("3")) {
                    JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
                    List<String> tempList = Util.TokenizerString(jobtitleId, ",");
                    for (int i = 0; i < tempList.size(); i++) {
                        showName += "," + jobTitlesComInfo.getJobTitlesname(tempList.get(i));
                    }
                    showName = showName.length() > 0 ? showName.substring(1) : showName;
                } else if (shareLevel.equals("4")) {
                    showName = SystemEnv.getHtmlLabelName(1340, language);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return showName;
    }

    /**
     * 获取考勤报表权限共享日志的对象名
     *
     * @param targetId
     * @param otherParams
     * @return
     */
    public String getTargetName4ReportShare(String targetId, String otherParams) {
        String targetName = "";
        try {
            String reportName = otherParams.split("\\+")[0];
            String resourceType = otherParams.split("\\+")[1];
            String resourceId = otherParams.split("\\+", 4)[2];
            String languageId = otherParams.split("\\+", 4)[3];

            switch (reportName) {
                case "1":
                    targetName += SystemEnv.getHtmlLabelName(16559, Util.getIntValue(languageId, 7));
                    break;
                case "2":
                    targetName += SystemEnv.getHtmlLabelName(82801, Util.getIntValue(languageId, 7));
                    break;
                case "3":
                    targetName += SystemEnv.getHtmlLabelName(390248, Util.getIntValue(languageId, 7));
                    break;
                case "4":
                    targetName += SystemEnv.getHtmlLabelName(389441, Util.getIntValue(languageId, 7));
                    break;
            }

            ResourceComInfo resourceComInfo = new ResourceComInfo();
            targetName += " " + Util.formatMultiLang(resourceComInfo.getLastnames(resourceId), languageId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return targetName;
    }

    /***************************************公出出差规则的相关方法***************************************/

    /**
     * 获取出差公出规则的日志的对象名
     *
     * @param targetId
     * @param otherParams
     * @return
     */
    public String getTargetName4TravelRules(String targetId, String otherParams) {
        String targetName = "";
        String languageId = otherParams.split("\\+")[0];
        targetName = SystemEnv.getHtmlLabelName(390004, Util.getIntValue(languageId, 7));
        return targetName;
    }

    /***************************************一键已用考勤的相关方法***************************************/

    /**
     * 获取目前新考勤的请假类型表的ID字段的下一个值
     * 考虑到历史数据迁移，过去的请假类型表和目前的请假类型表不是同一张，所以新考勤请假类型表kq_leaveRules的id(非自增)对应于旧的老考勤请假类型表HrmLeaveTypeColor的Id，并作为主键使用
     *
     * @return
     */
    public int getNextId() {
        int nextId = 0;
        int maxId = 0;
        String maxColorIdSql = "select max(id) maxId from kq_leaveRules";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(maxColorIdSql);
        if (recordSet.next()) {
            maxId = Util.getIntValue(recordSet.getString("maxId"), 1);
        }
        nextId = maxId <= 0 ? 1 : (maxId + 1);
        return nextId;
    }

    /***************************************正式系统会用到的相关方法***************************************/

    /**
     * 获取人员的年假计算开始日期
     *
     * @param companyStartDate
     * @param resourceId
     * @return
     */
    public String getCompanyStartDate(String companyStartDate, String resourceId) {
        return companyStartDate;
    }

    /**
     * 获取考勤流程校验规则的 校验规则
     * @return
     */
    public String get_rules(String rule_type_uuid,String otherparam) {
      List<String> otherparams = Util.splitString2List(otherparam, "+");
      if (otherparams.size() == 4 && rule_type_uuid.length() > 0) {
        String ruleid = otherparams.get(0);
        String rule_table = otherparams.get(1);
        String key_fieldname = otherparams.get(2);
        String fieldname = "";
        RecordSet rs = new RecordSet();
        if(key_fieldname.length() > 0){
          String sql = "select * from kq_att_checkrule_fields where rule_type_uuid=? and key_fieldname=? ";
          rs.executeQuery(sql, rule_type_uuid,key_fieldname);
          if(rs.next()){
            fieldname = rs.getString("fieldname");
          }else{
            fieldname = key_fieldname;
          }
        }else{
          return "";
        }
        String lanid = otherparams.get(3);
        String sql = "select * from "+rule_table+" where id = ? ";
        rs.executeQuery(sql,ruleid);
        if(rs.next()){
          String val = rs.getString(fieldname);
          if("check_rule".equalsIgnoreCase(key_fieldname)){
            if("kq_att_frequency_rule".equalsIgnoreCase(rule_table)){
              //次数校验需要单独处理
              String val_count = rs.getString(fieldname+"_count");
              String frequencyContent = getFrequencyContent(val,Util.getIntValue(lanid));
              String content = frequencyContent + SystemEnv.getHtmlLabelName(516776, Util.getIntValue(Util.getIntValue(lanid)))+val_count+SystemEnv.getHtmlLabelName(18083, Util.getIntValue(Util.getIntValue(lanid)));
              return content;
            }else{
              if("1".equalsIgnoreCase(val)){
                return SystemEnv.getHtmlLabelName(516104, Util.getIntValue(Util.getIntValue(lanid)));
              }else{
                return SystemEnv.getHtmlLabelName(516103, Util.getIntValue(Util.getIntValue(lanid)));
              }
            }
          }else if("check_level".equalsIgnoreCase(key_fieldname)){
            if("0".equalsIgnoreCase(val)){
              return SystemEnv.getHtmlLabelName(32137, Util.getIntValue(Util.getIntValue(lanid)));
            }else if("1".equalsIgnoreCase(val)){
              return SystemEnv.getHtmlLabelName(32138, Util.getIntValue(Util.getIntValue(lanid)));
            }else if("2".equalsIgnoreCase(val)){
              return SystemEnv.getHtmlLabelName(26009, Util.getIntValue(Util.getIntValue(lanid)));
            }else {
              return "";
            }
          }else if("check_message".equalsIgnoreCase(key_fieldname)){
            return val;
          }else {
            return "";
          }
        }
      }
      return "";
    }

    public String getCheckRuleName(String labelids, String lanid) {
      return SystemEnv.getHtmlLabelNames(labelids,Util.getIntValue(lanid,7));
    }

  public String getFrequencyContent(String val, int lanid) {
    if("0".equalsIgnoreCase(val)){
      return SystemEnv.getHtmlLabelName(539, Util.getIntValue(lanid));
    }else if("1".equalsIgnoreCase(val)){
      return SystemEnv.getHtmlLabelName(545, Util.getIntValue(lanid));
    }else if("2".equalsIgnoreCase(val)){
      return SystemEnv.getHtmlLabelName(541, Util.getIntValue(lanid));
    }else if("3".equalsIgnoreCase(val)){
      return SystemEnv.getHtmlLabelName(543, Util.getIntValue(lanid));
    }else if("4".equalsIgnoreCase(val)){
      return SystemEnv.getHtmlLabelName(546, Util.getIntValue(lanid));
    }
    return "";
  }

  /**
   * 获取设备信息
   *
   * @param deviceinfo
   * @return
   */
  public String getDeviceinfo(String deviceinfo) {
    String info = "";
    try{
      if(deviceinfo.length() > 0){
        JSONObject jsonObject = JSONObject.parseObject(deviceinfo);
        if(jsonObject != null && !jsonObject.isEmpty()){
          String deviceId = Util.null2String(jsonObject.get("deviceId"));
          if(deviceId.length() > 0){
            info = deviceId;
          }
        }
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    return info;
  }

  /**
   * 判断是否在地理范围内
   *
   * @param isincom
   * @param otherParam
   * @return
   */
  public String getIsincom(String isincom,String otherParam) {
    String info = "";
    try{
      if(isincom.length() > 0){
        int lan = Util.getIntValue(otherParam);
        if("1".equalsIgnoreCase(isincom)){
          info = SystemEnv.getHtmlLabelName(163, lan);
        }else{
          info = SystemEnv.getHtmlLabelName(161, lan);
        }
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    return info;
  }

  public String getWorkFlowUrl4mobile(String requestid) {
    WorkflowRequestComInfo workflowRequestComInfo = new WorkflowRequestComInfo();
    String flowName = workflowRequestComInfo.getRequestName(requestid);
    if (StringUtil.isNotNull(flowName)) {
      String url = weaver.general.GCONST.getContextPath()+"/spa/workflow/static4mobileform/index.html#/req?requestid=" + requestid;
      String a_href = "<a href='javascript:void(0)' onclick=\"window.showHoverWindow('" + url + "','/req',false)\"  >" + flowName + "</a>&nbsp;";
      return a_href;
    } else {
      return "";
    }
  }

  public String getWorkFlowUrl(String requestid) {
    WorkflowRequestComInfo workflowRequestComInfo = new WorkflowRequestComInfo();
    String flowName = workflowRequestComInfo.getRequestName(requestid);
    if (StringUtil.isNotNull(flowName)) {
      return "<a href='"+weaver.general.GCONST.getContextPath()+"/spa/workflow/index_form.jsp#/main/workflow/req?requestid=" + requestid + "'target='_blank'>" + flowName + "</a>&nbsp;";
    } else {
      return "";
    }
  }

    public String getLabelName(String id, String otherPara) {
        String otherParaArr[] = otherPara.split("\\+", 5);
        String labelId = otherParaArr[0];
        String LanId = otherParaArr[1];
        return SystemEnv.getHtmlLabelNames(labelId, LanId);
    }
}
