package com.engine.kq.cmd.balanceofleaverp;

import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldUtil;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.*;
import com.engine.kq.util.KQTransMethod;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 员工假期余额报表--获取查询列表
 */
public class GetSearchListCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetSearchListCmd(Map<String, Object> params, User user) {
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
        /**
         * 分页控件返回的值
         * currentPage：当前页数
         * pageSize：每页多少条数据
         */
        int currentPage = Util.getIntValue((String) params.get("currentPage"), 1);
        int pageSize = Util.getIntValue((String) params.get("pageSize"), 10);
        /**
         * 时间范围选择的年份
         * dateScope：5-本年、8-上一年
         * selectedYear：指定年份
         */
        String dateScope = Util.null2String(params.get("dateScope"));
        String selectedYear = Util.null2String(params.get("selectedYear"));
        if (dateScope.equals("5") || dateScope.equals("8")) {
            selectedYear = TimeUtil.getDateByOption(dateScope, "0").substring(0, 4);
        }
        /**
         * 数据范围
         * dataScope：0-总部、1-分部、2-分部、3-人员、4-我的下属
         * subcomId：指定分部ID
         * deptId：指定部门ID
         * resourceId：指定人员ID
         * allLevel：是否包含下级下属：0-不包含、1-包含
         */
        String dataScope = Util.null2String(params.get("dataScope"));
        String subcomId = Util.null2String(params.get("subcomId"));
        String deptId = Util.null2String(params.get("deptId"));
        String resourceId = Util.null2String(params.get("resourceId"));
        String allLevel = Util.null2String(params.get("allLevel"));
        //人员状态
        String resourceStatus = Util.null2String(params.get("status"));
        /**
         * isNoAccount：是否显示无账号人员：true-显示、false-不显示
         */
        String isNoAccount = Util.null2String(params.get("isNoAccount"));

        try {
            /**
             * 获取考勤报表权限共享设置
             */
            KQReportBiz kqReportBiz = new KQReportBiz();
            String rightStr = kqReportBiz.getReportRight("4", "" + user.getUID(), "a");

            /**
             * ==zj 获取该账号的子账号id
             */
            if (!user.isAdmin()){
                KqUtil kqUtil = new KqUtil();
                String childId = kqUtil.getChildKq(user.getUID());
                if (!"".equals(childId)){
                    resourceId += ","+childId;
                }
            }


            /**
             * 拼凑查询结果列表的表头
             */
            List<Map<String, Object>> columnsList = new ArrayList<Map<String, Object>>();
            List<HrmFieldBean> hrmFieldBeanList = new ArrayList<HrmFieldBean>();
            HrmFieldBean hrmFieldBean = null;
            String[] tempArr = new String[]{"lastName,413", "subcom,141", "dept,124", "jobtitle,6086","workcode,714", "companyStartDate,1516"};
            for (int i = 0; i < tempArr.length; i++) {
                String[] fieldInfo = tempArr[i].split(",");
                hrmFieldBean = new HrmFieldBean();
                hrmFieldBean.setFieldname(fieldInfo[0]);
                hrmFieldBean.setFieldlabel(fieldInfo[1]);
                hrmFieldBean.setFieldhtmltype("1");
                hrmFieldBean.setType("1");
                hrmFieldBean.setViewAttr(1);
                hrmFieldBean.setIsFormField(true);
                hrmFieldBeanList.add(hrmFieldBean);
            }

            /**********************************************************************************************************/

            /**获取假期类型的相关设置*/

            /*请假类型的ID*/
            String leaveRulesId = "";

            /*假期类型的名称，用作查询结果列表的表头显示*/
            String leaveName = "";

            /*最小请假单位：1-按天请假、2-按半天请假、3-按小时请假、4-按整天请假*/
            int minimumUnit = 1;

            /*请假单位的显示名称是天还是小时*/
            String unitName = "";

            /*是否开启假期余额，没有开启假期余额时需要提示“不限制余额*/
            int balanceEnable = 0;

            /**********************************************************************************************************/

            /*是否是混合模式(福利年假+法定年假)*/
            boolean isMixMode = false;

            //是否是调休
            boolean isTiaoXiu = false;

            /**********************************************************************************************************/

            /*获取当前日期*/
            String currentDate = DateUtil.getCurrentDate();

            Map<String, BigDecimal> balanceMap = new HashMap<String, BigDecimal>();
            KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
            rulesComInfo.setTofirstRow();
            while (rulesComInfo.next()) {
                if (rulesComInfo.getIsEnable().equals("0")) {
                    continue;//此假期类型没有启用
                }

                /*获取假期类型的设置 start*/
                leaveRulesId = rulesComInfo.getId();
                leaveName = Util.formatMultiLang(rulesComInfo.getLeaveName(), "" + user.getLanguage());
                minimumUnit = Util.getIntValue(rulesComInfo.getMinimumUnit(), 1);
                if (minimumUnit == 1 || minimumUnit == 2 || minimumUnit == 4) {
                    unitName = SystemEnv.getHtmlLabelName(389325, user.getLanguage());//(天)
                } else {
                    unitName = SystemEnv.getHtmlLabelName(389326, user.getLanguage());//(小时)
                }
                String showName = leaveName + unitName;
                balanceEnable = Util.getIntValue(rulesComInfo.getBalanceEnable(), 0);

                isMixMode = KQLeaveRulesBiz.isMixMode(leaveRulesId);
                isTiaoXiu = KQLeaveRulesBiz.isTiaoXiu(leaveRulesId);
                /*获取假期类型的设置 end*/

                hrmFieldBean = new HrmFieldBean();
                hrmFieldBean.setFieldname(rulesComInfo.getId());
                hrmFieldBean.setFieldlabelname(showName);
                hrmFieldBean.setFieldhtmltype("1");
                hrmFieldBean.setType("1");
                hrmFieldBean.setViewAttr(1);
                hrmFieldBean.setIsFormField(true);
                hrmFieldBeanList.add(hrmFieldBean);

                /*判断该假期规则是否开启了假期余额，若没有开启，则无需查找该假期的余额，直接遍历下一个假期*/
                if (balanceEnable == 0) {
                    continue;
                }
                //如果是调休，获取假期余额的方式有些不同
                if(isTiaoXiu){
                    params.put("leaveRulesId",leaveRulesId);
                    params.put("showAll","false");
                    Map<String, BigDecimal> _balanceMap = KQBalanceOfLeaveBiz.getRestAmountMapByDis5(params,user);
                    balanceMap.putAll(_balanceMap);
                    continue;
                }

                /*查询指定年份指定假期的余额数据，并放入集合中，用于后续拼凑查询结果数据*/
                KQTransMethod transMethod = new KQTransMethod();
                RecordSet recordSet = new RecordSet();
                String sql = "select a.id hrmResourceId,a.companyStartDate,a.workStartDate,b.* from HrmResource a left join kq_balanceOfLeave b on a.id=b.resourceId " +
                        "and belongYear='" + selectedYear + "' and b.leaveRulesId=" + leaveRulesId + " where 1=1 ";
                if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                    sql = "select a.id hrmResourceId,a.companyStartDate,a.workStartDate,b.*,ROW_NUMBER() OVER(order by dspOrder,a.id) as rn  from HrmResource a left join kq_balanceOfLeave b on a.id=b.resourceId " +
                            "and belongYear='" + selectedYear + "' and b.leaveRulesId=" + leaveRulesId + " where 1=1 ";
                } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                    sql = "select a.id hrmResourceId,a.companyStartDate,a.workStartDate,b.* from HrmResource a left join kq_balanceOfLeave b on a.id=b.resourceId " +
                            "and belongYear='" + selectedYear + "' and b.leaveRulesId=" + leaveRulesId + " where 1=1 ";
                }
                if (dataScope.equals("0")) {
                    //总部
                } else if (dataScope.equals("1")) {
                    sql += " and a.subcompanyId1 in (" + subcomId + ") ";
                } else if (dataScope.equals("2")) {
                    sql += " and a.departmentId in (" + deptId + ") ";
                } else if (dataScope.equals("3")) {
                    sql += " and a.id in (" + resourceId + ")";
                } else if (dataScope.equals("4")) {
                    if (allLevel.equals("1")) {
                        sql += " and (a.id=" + user.getUID() + " or a.managerStr like '%," + user.getUID() + ",%' )";
                    } else {
                        sql += " and (a.id=" + user.getUID() + " or a.managerid = " + user.getUID() + ")";
                    }
                }
                if (isNoAccount.equals("false")) {
                    if (recordSet.getDBType().equalsIgnoreCase("sqlserver")
                            || recordSet.getDBType().equalsIgnoreCase("mysql")) {
                        sql += " and (loginId is not null and loginId<>'')";
                    }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                        sql += " and (loginId is not null and loginId<>'')";
                    } else {
                        sql += " and (loginId is not null)";
                    }
                }

                if(resourceStatus.length()>0){
                  if (!resourceStatus.equals("8") && !resourceStatus.equals("9")) {
                    sql += " and a.status = "+resourceStatus+ "";
                  }else if (resourceStatus.equals("8")) {
                    sql += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
                  }
                }
                //考勤报表共享设置
                if (!rightStr.equals("") && !dataScope.equals("4")) {
                    sql += rightStr;
                }
                if (!recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                    sql += " order by dspOrder,hrmResourceId ";
                }

                String pageSql = "select * from (select tmp.*,rownum rn from (" + sql + ") tmp where rownum<=" + (pageSize * currentPage) + ") where rn>=" + (pageSize * (currentPage - 1) + 1);
                if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                    pageSql = "select t.* from (" + sql + ") t where 1=1 and rn>=" + (pageSize * (currentPage - 1) + 1) + " and rn<=" + (pageSize * currentPage);
                } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                    pageSql = sql + " limit " + (currentPage - 1) * pageSize + "," + pageSize;
                }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                    pageSql = sql + " limit " +pageSize + " offset " +  (currentPage - 1) * pageSize;
                }
                recordSet.executeQuery(pageSql);
                while (recordSet.next()) {
                    String hrmResourceId = recordSet.getString("hrmResourceId");
                    //所属年份
                    String belongYear = recordSet.getString("belongYear");
                    //失效日期
                    String effectiveDate = recordSet.getString("effectiveDate");
                    //失效日期
                    String expirationDate = recordSet.getString("expirationDate");
                    /*判断假期余额的有效期*/
                    boolean status = KQBalanceOfLeaveBiz.getBalanceStatus(leaveRulesId, hrmResourceId, belongYear, currentDate,effectiveDate,expirationDate);
                    if (!status) {
                        continue;
                    }
                    BigDecimal baseAmount = new BigDecimal(Util.getDoubleValue(recordSet.getString("baseAmount"), 0.00));//假期基数
                    BigDecimal usedAmount = new BigDecimal(Util.getDoubleValue(recordSet.getString("usedAmount"), 0.00));//已用假期
                    BigDecimal extraAmount = new BigDecimal(Util.getDoubleValue(recordSet.getString("extraAmount"), 0.00));//额外假期
                    BigDecimal baseAmount2 = new BigDecimal(Util.getDoubleValue(recordSet.getString("baseAmount2"), 0.00));//用于混合模式时：福利年假基数
                    BigDecimal usedAmount2 = new BigDecimal(Util.getDoubleValue(recordSet.getString("usedAmount2"), 0.00));//用于混合模式时：已用福利年假
                    BigDecimal extraAmount2 = new BigDecimal(Util.getDoubleValue(recordSet.getString("extraAmount2"), 0.00));//用于混合模式时：额外福利年假

                    BigDecimal restAmount = new BigDecimal(0);
                    if (isMixMode) {
                        /*释放规则*/
                        baseAmount = KQBalanceOfLeaveBiz.getCanUseAmount(hrmResourceId, leaveRulesId, belongYear, baseAmount, "legal", currentDate);
                        baseAmount2 = KQBalanceOfLeaveBiz.getCanUseAmount(hrmResourceId, leaveRulesId, belongYear, baseAmount2, "welfare", currentDate);

                        restAmount = baseAmount.add(extraAmount).subtract(usedAmount).add(baseAmount2).add(extraAmount2).subtract(usedAmount2);
                    } else {
                        /*释放规则*/
                        baseAmount = KQBalanceOfLeaveBiz.getCanUseAmount(hrmResourceId, leaveRulesId, belongYear, baseAmount, "", currentDate);

                        restAmount = baseAmount.add(extraAmount).subtract(usedAmount);
                    }
                    balanceMap.put(hrmResourceId + "_" + leaveRulesId, restAmount);
                }
            }

            /**
             * 用于拼凑查询结果列表的表数据
             */
            SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
            List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
            Map<String, Object> dataMap = new HashMap<String, Object>();
            RecordSet recordSet = new RecordSet();
            String sql = "select * from HrmResource a where 1=1 ";
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                sql = "select *,ROW_NUMBER() OVER(order by dspOrder,id) as rn from HrmResource a where 1=1 ";
            }
            if (dataScope.equals("0")) {
                //总部
            } else if (dataScope.equals("1")) {
                sql += " and a.subcompanyId1 in (" + subcomId + ") ";
            } else if (dataScope.equals("2")) {
                sql += " and a.departmentId in (" + deptId + ") ";
            } else if (dataScope.equals("3")) {
                sql += " and a.id in (" + resourceId + ")";
            } else if (dataScope.equals("4")) {
                if (allLevel.equals("1")) {
                    sql += " and (a.id=" + user.getUID() + " or a.managerStr like '%," + user.getUID() + ",%' )";
                } else {
                    sql += " and (a.id=" + user.getUID() + " or a.managerid = " + user.getUID() + ")";
                }
            }
            if (isNoAccount.equals("false")) {
                if (recordSet.getDBType().equalsIgnoreCase("sqlserver")
                        || recordSet.getDBType().equalsIgnoreCase("mysql")) {
                    sql += " and (a.loginId is not null and a.loginId<>'')";
                }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                    sql += " and (a.loginId is not null and a.loginId<>'')";
                } else {
                    sql += " and (a.loginId is not null)";
                }
            }

            if(resourceStatus.length()>0){
              if (!resourceStatus.equals("8") && !resourceStatus.equals("9")) {
                sql += " and a.status = "+resourceStatus+ "";
              }else if (resourceStatus.equals("8")) {
                sql += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
              }
            }
            //考勤报表共享设置
            if (!rightStr.equals("") && !dataScope.equals("4")) {
                sql += rightStr;
            }
            if (!recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                sql += " order by dspOrder,id ";
            }
            String pageSql = "select * from (select tmp.*,rownum rn from (" + sql + ") tmp where rownum<=" + (pageSize * currentPage) + ") where rn>=" + (pageSize * (currentPage - 1) + 1);
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")) {
                pageSql = "select t.* from (" + sql + ") t where 1=1 and rn>=" + (pageSize * (currentPage - 1) + 1) + " and rn<=" + (pageSize * currentPage);
            } else if (recordSet.getDBType().equalsIgnoreCase("mysql")) {
                pageSql = sql + " limit " + (currentPage - 1) * pageSize + "," + pageSize;
            }else if (recordSet.getDBType().equalsIgnoreCase("postgresql")) {
                pageSql = sql + " limit " +pageSize  + " offset " + (currentPage - 1) * pageSize;
            }
            recordSet.executeQuery(pageSql);

            // #1473334-概述：满足考勤报分部部门显示及导出时显示全路径
            String fullPathMainKey = "show_full_path";
            KQSettingsComInfo kqSettingsComInfo = new KQSettingsComInfo();
            String isShowFullPath = Util.null2String(kqSettingsComInfo.getMain_val(fullPathMainKey),"0");
            String departmentNameTmp = "";
            String subCompanyNameTmp = "";

            new BaseBean().writeLog("==zj==(员工假期余额sql)" + pageSql);
            while (recordSet.next()) {
                dataMap = new HashMap<String, Object>();

                String id = recordSet.getString("id");
                String lastName = Util.formatMultiLang(recordSet.getString("lastName"), "" + user.getLanguage());
                String departmentId = recordSet.getString("departmentId");
                String subcompanyId = recordSet.getString("subcompanyId1");
                String jobtitleId = recordSet.getString("jobtitle");
                String workcode = recordSet.getString("workcode");
                String companyStartdate = recordSet.getString("companyStartdate");

                dataMap.put("id", id);
                dataMap.put("lastName", lastName);

                // 根据开关决定是否显示分部部门全路径
                subCompanyNameTmp = "1".equals(isShowFullPath) ?
                        SubCompanyComInfo.getSubcompanyRealPath(subcompanyId, "/", "0") :
                        subCompanyComInfo.getSubCompanyname(subcompanyId);

                departmentNameTmp =  "1".equals(isShowFullPath) ?
                        departmentComInfo.getDepartmentRealPath(departmentId, "/", "0") :
                        departmentComInfo.getDepartmentname(departmentId);

                dataMap.put("subcom", Util.formatMultiLang(subCompanyNameTmp, "" + user.getLanguage()));
                dataMap.put("dept", Util.formatMultiLang(departmentNameTmp, "" + user.getLanguage()));

                // dataMap.put("subcom", Util.formatMultiLang(subCompanyComInfo.getSubcompanyname(subcompanyId), "" + user.getLanguage()));
                // dataMap.put("dept", Util.formatMultiLang(departmentComInfo.getDepartmentname(departmentId), "" + user.getLanguage()));
                dataMap.put("jobtitle", Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitleId), "" + user.getLanguage()));
                dataMap.put("workcode", workcode);
                dataMap.put("companyStartDate", companyStartdate);
                dataMap.put("subcomId", subcompanyId);
                dataMap.put("deptId", departmentId);
                dataMap.put("jobtitleId", jobtitleId);

                rulesComInfo.setTofirstRow();
                while (rulesComInfo.next()) {
                    if (rulesComInfo.getIsEnable().equals("0")) {
                        continue;
                    }
                    /*该假期没有开启余额限制，显示不限制余额*/
                    if (rulesComInfo.getBalanceEnable().equals("0")) {
                        dataMap.put(rulesComInfo.getId(), SystemEnv.getHtmlLabelName(389731, user.getLanguage()));//不限制余额
                        continue;
                    }
                    BigDecimal restAmount = balanceMap.get(id + "_" + rulesComInfo.getId());
                    dataMap.put(rulesComInfo.getId(), restAmount != null ? restAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0");
                }
                dataList.add(dataMap);
            }

            columnsList = HrmFieldUtil.getHrmDetailTable(hrmFieldBeanList, null, user);
            resultMap.put("columns", columnsList);
            resultMap.put("datas", dataList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}
