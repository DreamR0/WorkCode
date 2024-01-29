package com.engine.kq.cmd.leaverules;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import com.engine.kq.biz.KQLeaveRulesDetailComInfo;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.systeminfo.SystemEnv;

import java.util.*;

/**
 * 假期规则--编辑
 */
public class EditLeaveRulesCmd extends AbstractCommonCommand<Map<String, Object>> {

    private SimpleBizLogger logger;

    public EditLeaveRulesCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
        this.logger = new SimpleBizLogger();

        int ruleDetailId = Util.getIntValue((String) params.get("ruleId"));
        BizLogContext bizLogContext = new BizLogContext();
        bizLogContext.setLogType(BizLogType.HRM_ENGINE);//模块类型
        bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_KQ_LEAVERULES);//所属大类型
        bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_KQ_LEAVERULES);//当前小类型
        bizLogContext.setParams(params);//当前request请求参数
        logger.setUser(user);//当前操作人
        String mainSql = "select * from kq_LeaveRulesDetail where id=" + ruleDetailId;
        logger.setMainSql(mainSql, "id");//主表sql
        logger.setMainPrimarykey("id");//主日志表唯一key
        logger.setMainTargetNameColumn("rulename");

        SimpleBizLogger.SubLogInfo subLogInfo1 = logger.getNewSubLogInfo();
        String subSql1 = "select * from kq_EntryToLeave where leaveRulesId=" + ruleDetailId;
        subLogInfo1.setSubSql(subSql1, "id");
        logger.addSubLogInfo(subLogInfo1);

        SimpleBizLogger.SubLogInfo subLogInfo2 = logger.getNewSubLogInfo();
        String subSql2 = "select * from kq_WorkingAgeToLeave where leaveRulesId=" + ruleDetailId;
        subLogInfo2.setSubSql(subSql2, "id");
        logger.addSubLogInfo(subLogInfo2);

        SimpleBizLogger.SubLogInfo subLogInfo3 = logger.getNewSubLogInfo();
        String subSql3 = "select * from kq_MixModeToLegalLeave where leaveRulesId=" + ruleDetailId;
        subLogInfo3.setSubSql(subSql3, "id");
        logger.addSubLogInfo(subLogInfo3);

        SimpleBizLogger.SubLogInfo subLogInfo4 = logger.getNewSubLogInfo();
        String subSql4 = "select * from kq_MixModeToLegalLeave where leaveRulesId=" + ruleDetailId;
        subLogInfo4.setSubSql(subSql4, "id");
        logger.addSubLogInfo(subLogInfo4);

        logger.before(bizLogContext);
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public List<BizLogContext> getLogContexts() {
        return logger.getBizLogContexts();
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            boolean canEdit = HrmUserVarify.checkUserRight("KQLeaveRulesEdit:Edit", user);//是否具有新建权限
            if (!canEdit) {
                resultMap.put("status", "-1");
                resultMap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return resultMap;
            }
            /*假期类型的ID*/
            int ruleId = Util.getIntValue((String) params.get("typeId"));

            /*假期规则详情的ID*/
            int ruleDetailId = Util.getIntValue((String) params.get("ruleId"));

            /*假期规则名称*/
            String ruleName = Util.null2String(params.get("ruleName"));

            /*应用范围：0-总部、1-分部*/
            int scopeType = Util.getIntValue((String) params.get("scopeType"));

            /*应用范围为分部时，选择的分部ID*/
            String scopeValue = Util.null2String(params.get("scopeValue"));

            /*qc2521698 年假界定判定日期*/
            String scopeDate = Util.null2String(params.get("scopeDate"));

            /*余额发放方式：1-手动发放、2-按司龄自动发放、3-按工龄自动发放、4-每年自动发放固定天数、	5-加班时长自动计入余额*/
            int distributionMode = Util.getIntValue((String) params.get("distributionMode"), 1);

            /*每人发放小时(天)数(当余额发放方式为每年自动发放固定天数时有效)*/
            double annualAmount = Util.getDoubleValue((String) params.get("annualAmount"), 0);

            /*扣减优先级：1-法定年假、2-福利年假*/
            int priority = Util.getIntValue((String) params.get("priority"), 1);

            //法定年假规则：0-工龄、1-司龄、2-工龄+司龄 (当余额发放方式为按工龄+司龄自动发放时有效)
            String legalKey = Util.null2s((String) params.get("legalKey"), "0");

            //福利年假规则：0-工龄、1-工龄、2-工龄+司龄 (当余额发放方式为按工龄+司龄自动发放时有效)
            String welfareKey = Util.null2s((String) params.get("welfareKey"), "1");

            /*有效期规则：0-不限制、1-按自然月(1月1日-12月31日)、2-按入职日期起12个月、3-自定义次年失效日期、4-自定义有效天数*/
            int validityRule = Util.getIntValue((String) params.get("validityRule"), 1);

            /*有效期天数：（当有效期天数选择按天数失效的时候有效）*/
            int effectiveDays = Util.getIntValue((String) params.get("effectiveDays"), 30);

            //有效月数（当有效期规则选择自定义有效月数时）
            int effectiveMonths = Util.getIntValue((String) params.get("effectiveMonths"), 1);

            /*失效日期--月（当有效期规则选择3-自定义次年失效日期时有效）*/
            String expirationMonth = Util.null2String(params.get("expirationMonth"));

            /*失效日期--日（当有效期规则选择3-自定义次年失效日期时有效）*/
            String expirationDay = Util.null2String(params.get("expirationDay"));

            /*允许延长有效期：0-不允许、1-允许*/
            int extensionEnable = Util.getIntValue((String) params.get("extensionEnable"), 0);

            /*允许超过有效期天数*/
            int extendedDays = Util.getIntValue((String) params.get("extendedDays"), 0);

            /*释放规则：0-不限制、1-按天释放、2-按月释放*/
            int releaseRule = Util.getIntValue((String) params.get("releaseRule"), 0);

            /*假期基数计算方式：0-精确计算、1-按最少的假期余额计算、2-按最多的假期余额计算*/
            int calcMethod = Util.getIntValue((String) params.get("calcMethod"), 0);

            /*是否折算：0-不折算、1-四舍五入、2-向上取整、3-向下取整、4-向上取0.5的倍数、5-向下取0.5的倍数*/
            int convertMode = Util.getIntValue("" + params.get("convertMode"), 1);

            /*次账号是否发放假期余额：0-不发放、1-发放*/
            int excludeSubAccount = Util.getIntValue("" + params.get("excludeSubAccount"), 1);

            /*转正之前是否发放假期余额：0-不发放、1-发放*/
            int beforeFormal = Util.getIntValue("" + params.get("beforeFormal"), 1);

            if (scopeType == 1 && scopeValue.equals("")) {
                resultMap.put("status", "-1");
                resultMap.put("message", SystemEnv.getHtmlLabelName(388858, user.getLanguage()));//参数有误
                return resultMap;
            }
            if (distributionMode == 5) {
                releaseRule = 0;
                /*两个不同的假期类型下不能同时存在 加班时长自动计入余额 的余额发放方式*/
                String sql = "select * from kq_LeaveRules where (isDelete is null or isDelete<>1) and id<>? and id in (select ruleId from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and distributionMode=5)";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql, ruleId);
                if (recordSet.next()) {
                    resultMap.put("status", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(505664, user.getLanguage()));//两个不同的假期类型下不能同时存在 加班时长自动计入余额 的余额发放方式
                    return resultMap;
                }
            } else if (distributionMode == 6) {
                /*同一假期类型下 按司龄+工龄自动发放 不能与其他余额发放方式共存*/
                String sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=? and distributionMode<>6";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql, ruleId);
                if (recordSet.next()) {
                    resultMap.put("status", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(505665, user.getLanguage()));//同一假期类型下 按司龄+工龄自动发放 不能与其他余额发放方式共存
                    return resultMap;
                }
            } else {
                /*同一个假期类型下其他余额发放方式不能与 按司龄+工龄自动发放 共存*/
                String sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=? and distributionMode=6";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql, ruleId);
                if (recordSet.next()) {
                    resultMap.put("status", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(505666, user.getLanguage()));//同一个假期类型下其他余额发放方式不能与 按司龄+工龄自动发放 共存
                    return resultMap;
                }

                //同一个假期类型下其他余额发放方式不能与 加班时长自动计入余额 共存
                sql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=? and distributionMode=5";
                recordSet.executeQuery(sql, ruleId);
                if (recordSet.next()) {
                    resultMap.put("status", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(510357, user.getLanguage()));//同一个假期类型下其他余额发放方式不能与 加班时长自动计入余额 共存
                    return resultMap;
                }
            }

            String searchSql = "select * from kq_LeaveRulesDetail where (isDelete is null or isDelete<>1) and ruleId=? and id<>?";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(searchSql, ruleId, ruleDetailId);
            while (recordSet.next()) {
                int scopeTypeTemp = Util.getIntValue(recordSet.getString("scopeType"), 0);
                String scopeValueTemp = recordSet.getString("scopeValue");

                if (scopeType == 0 && scopeTypeTemp == 0) {
                    resultMap.put("status", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(505667, user.getLanguage()));//该假期类型下已经新建过总部的假期规则，请勿重复新建
                    return resultMap;
                }
                if (scopeType == 1 && scopeTypeTemp == 1) {
                    List<String> scopeValueTempList = Util.TokenizerString(scopeValueTemp, ",");
                    List<String> scopeValueList = Util.TokenizerString(scopeValue, ",");
                    for (String temp : scopeValueList)
                        if (scopeValueTempList.contains(temp)) {
                            SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
                            String subcomName = subCompanyComInfo.getSubCompanyname(temp);

                            resultMap.put("status", "-1");
                            resultMap.put("message", SystemEnv.getHtmlLabelName(505668, user.getLanguage()).replace("$", subcomName));//该假期类型下已经新建过分部的假期规则，请勿重复新建
                            return resultMap;
                        }
                }
            }
            boolean flag = false;//数据是否更新成功
            //qc2521698添加一个scopeDate字段
            String sql = "update kq_LeaveRulesDetail set ruleId=?,ruleName=?,scopeType=?,scopeValue=?," +
                    "distributionMode=?,annualAmount=?,legalKey=?,welfareKey=?,priority=?,validityRule=?,effectiveDays=?,effectiveMonths=?,expirationMonth=?,expirationDay=?,extensionEnable=?,extendedDays=?,releaseRule=?,calcMethod=?,convertMode=?,excludeSubAccount=?,beforeFormal=?,scopeDate=? where id=?";
            flag = recordSet.executeUpdate(sql, ruleId, ruleName, scopeType, scopeValue,
                    distributionMode, annualAmount, legalKey, welfareKey, priority, validityRule, effectiveDays, effectiveMonths, expirationMonth, expirationDay, extensionEnable, extendedDays, releaseRule, calcMethod, convertMode, excludeSubAccount, beforeFormal, scopeDate,ruleDetailId);
            if (!flag) {
                resultMap.put("sign", "-1");
                resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                return resultMap;
            }

            if ((distributionMode == 2 || distributionMode == 7) && ruleDetailId != 0) {
                sql = "delete from kq_EntryToLeave where leaveRulesId=?";
                flag = recordSet.executeUpdate(sql, ruleDetailId);
                if (!flag) {
                    resultMap.put("sign", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                    return resultMap;
                }
                int lowerLimit = 0;//司龄下限
                int upperLimit = 0;//司龄上限
                String data = Util.null2String(params.get("detailRule"));
                JSONArray jsonArray = JSONArray.parseArray(data);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int timePoint = jsonObject.getIntValue("timePoint");
                    double amount = jsonObject.getDoubleValue("amount");

                    lowerLimit = i == 0 ? 0 : timePoint;
                    upperLimit = i == jsonArray.size() - 1 ? 9999 : jsonArray.getJSONObject(i + 1).getIntValue("timePoint");

                    sql = "insert into kq_EntryToLeave(leaveRulesId,lowerLimit,upperLimit,amount) values(?,?,?,?)";
                    flag = recordSet.executeUpdate(sql, ruleDetailId, lowerLimit, upperLimit, amount);
                    if (!flag) {
                        resultMap.put("sign", "-1");
                        resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                        return resultMap;
                    }
                }
            }

            if (distributionMode == 3 && ruleDetailId != 0) {
                sql = "delete from kq_WorkingAgeToLeave where leaveRulesId=?";
                flag = recordSet.executeUpdate(sql, ruleDetailId);
                if (!flag) {
                    resultMap.put("sign", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                    return resultMap;
                }
                int lowerLimit = 0;//工龄下限
                int upperLimit = 0;//工龄上限
                String data = Util.null2String(params.get("detailRule"));
                JSONArray jsonArray = JSONArray.parseArray(data);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int timePoint = jsonObject.getIntValue("timePoint");
                    double amount = jsonObject.getDoubleValue("amount");

                    lowerLimit = i == 0 ? 0 : timePoint;
                    upperLimit = i == jsonArray.size() - 1 ? 9999 : jsonArray.getJSONObject(i + 1).getIntValue("timePoint");

                    sql = "insert into kq_WorkingAgeToLeave(leaveRulesId,lowerLimit,upperLimit,amount) values(?,?,?,?)";
                    flag = recordSet.executeUpdate(sql, ruleDetailId, lowerLimit, upperLimit, amount);
                    if (!flag) {
                        resultMap.put("sign", "-1");
                        resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                        return resultMap;
                    }
                }
            }

            if (distributionMode == 6 && ruleDetailId != 0) {
                sql = "delete from kq_MixModeToLegalLeave where leaveRulesId=?";
                flag = recordSet.executeUpdate(sql, ruleDetailId);
                if (!flag) {
                    resultMap.put("sign", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                    return resultMap;
                }

                sql = "delete from kq_MixModeToWelfareLeave where leaveRulesId=?";
                flag = recordSet.executeUpdate(sql, ruleDetailId);
                if (!flag) {
                    resultMap.put("sign", "-1");
                    resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                    return resultMap;
                }
                String mixModeData = Util.null2String(params.get("legalRule"));
                JSONArray jsonArray = JSONArray.parseArray(mixModeData);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int workYear = jsonObject.getIntValue("workYear");
                    int entryTime = jsonObject.getIntValue("entryTime");
                    double legalAmount = jsonObject.getDoubleValue("legalAmount");

                    sql = "insert into kq_MixModeToLegalLeave(leaveRulesId,limit1,limit2,amount) values(?,?,?,?)";
                    flag = recordSet.executeUpdate(sql, ruleDetailId, workYear, entryTime, legalAmount);
                    if (!flag) {
                        resultMap.put("sign", "-1");
                        resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                        return resultMap;
                    }
                }

                mixModeData = Util.null2String(params.get("welfareRule"));
                jsonArray = JSONArray.parseArray(mixModeData);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int workYear = jsonObject.getIntValue("workYear");
                    int entryTime = jsonObject.getIntValue("entryTime");
                    double welfareAmount = jsonObject.getDoubleValue("welfareAmount");

                    sql = "insert into kq_MixModeToWelfareLeave(leaveRulesId,limit1,limit2,amount) values(?,?,?,?)";
                    flag = recordSet.executeUpdate(sql, ruleDetailId, workYear, entryTime, welfareAmount);
                    if (!flag) {
                        resultMap.put("sign", "-1");
                        resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
                        return resultMap;
                    }
                }
            }
            if (flag) {
                resultMap.put("sign", "1");
                resultMap.put("message", SystemEnv.getHtmlLabelName(83551, user.getLanguage()));//保存成功
            }
        } catch (Exception e) {
            writeLog(e);
            resultMap.put("sign", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(84544, user.getLanguage()));//保存失败
        } finally {
            KQLeaveRulesDetailComInfo detailComInfo = new KQLeaveRulesDetailComInfo();
            detailComInfo.removeCache();
        }
        return resultMap;
    }
}
