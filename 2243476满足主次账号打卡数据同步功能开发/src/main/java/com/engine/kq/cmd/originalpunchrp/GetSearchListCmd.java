package com.engine.kq.cmd.originalpunchrp;

import com.cloudstore.dev.api.util.Util_TableMap;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.biz.KQReportBiz;
import java.util.HashMap;
import java.util.Map;

import com.engine.kq.biz.KQSettingsComInfo;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.PageIdConst;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

/**
 * 原始打卡记录报表--获取查询结果列表
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
        try {
            String showCol = Util.null2String(params.get("showCol"));//展示列
            String dateScope = Util.null2String(params.get("dateScope"));//日期范围
            String startDate = Util.null2String(params.get("startDate"));//开始日期
            String endDate = Util.null2String(params.get("endDate"));//结束日期
            if (!dateScope.equals("") && !dateScope.equals("0") && !dateScope.equals("6")) {
                startDate = TimeUtil.getDateByOption(dateScope, "0");
                endDate = TimeUtil.getDateByOption(dateScope, "1");
            }
            //人员状态
            String status = Util.null2String(params.get("status"));
            String dataScope = Util.null2String(params.get("dataScope"));//数据范围
            String subcomId = Util.null2String(params.get("subcomId"));//分部ID
            String deptId = Util.null2String(params.get("deptId"));//部门ID
            String resourceId = Util.null2String(params.get("resourceId"));//人员ID
            String allLevel = Util.null2String(params.get("allLevel"));//包含下级下属
            String isNoaccount = Util.null2String(params.get("isNoAccount"));//是否显示无账号人员

            /**
             * 获取考勤报表权限共享设置
             */
            KQReportBiz kqReportBiz = new KQReportBiz();
            String rightStr = kqReportBiz.getReportRight("3", "" + user.getUID(), "b");

            String backFields = " a.*,b.subcompanyId1,b.departmentId,b.jobtitle,b.workcode ";
            String sqlFrom = " from HrmScheduleSign a,HrmResource b ";
            String sqlWhere = " where a.userId=b.id ";
            /*原始打卡记录是否显示补打卡流程的数据*/
            String showCardOnOriginalpunchRp = "0";//0-表示不显示、1-表示显示
            String kqSettingSql = "select * from kq_settings where main_key='showCardOnOriginalpunchRp'";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(kqSettingSql);
            if(recordSet.next()){
                showCardOnOriginalpunchRp = recordSet.getString("main_val");
            }
            if(!showCardOnOriginalpunchRp.equals("1")){
                sqlWhere += " and (signfrom is null or signfrom='' or signfrom not like 'card%') ";
            }
            if (!startDate.equals("")) {
                sqlWhere += " and (signDate is not null and signDate>='" + startDate + "') ";
            }
            if (!endDate.equals("")) {
                sqlWhere += " and (signDate is not null and signDate<='" + endDate + "') ";
            }
            if (dataScope.equals("1") && !subcomId.equals("")) {
                sqlWhere += " and b.subcompanyId1 in (" + subcomId + ")";
            }
            if (dataScope.equals("2") && !deptId.equals("")) {
                sqlWhere += " and b.departmentId in (" + deptId + ")";
            }

            //==zj resourceid加入次账号id
            new BaseBean().writeLog("==zj==(该用户是否是管理员)"+user.isAdmin());
            if (!user.isAdmin()){
                KqUtil kqUtil = new KqUtil();
                String childKq = kqUtil.getChildKq(user.getUID());
                new BaseBean().writeLog("==zj==(原始打卡人员id拼接)" +childKq);
                if (!"".equals(childKq)){
                    resourceId += ","+childKq;
                }
                new BaseBean().writeLog("==zj==(原始打卡记录人员id)" + resourceId);
            }

            if (dataScope.equals("3") && !resourceId.equals("")) {
                sqlWhere += " and userId in (" + resourceId + ")";
            }
            if (dataScope.equals("4")) {
                if (allLevel.equals("1")) {
                    sqlWhere += " and (b.id=" + user.getUID() + " or b.managerStr like '%," + user.getUID() + ",%' )";
                } else {
                    sqlWhere += " and (b.id=" + user.getUID() + " or b.managerid = " + user.getUID() + ")";
                }
            }
            if (isNoaccount.equals("false")) {
                if (recordSet.getDBType().equalsIgnoreCase("sqlserver")
                        || recordSet.getDBType().equalsIgnoreCase("mysql")) {
                    sqlWhere += " and (loginId is not null and loginId<>'')";
                } else {
                    sqlWhere += " and (loginId is not null)";
                }
            }

            if(status.length()>0){
              if (!status.equals("8") && !status.equals("9")) {
                sqlWhere += " and b.status = "+status+ "";
              }else if (status.equals("8")) {
                sqlWhere += " and (b.status = 0 or b.status = 1 or b.status = 2 or b.status = 3) ";
              }
            }

            //考勤报表共享设置
            if (!rightStr.equals("") && !dataScope.equals("4")) {
                sqlWhere += rightStr;
            }
            String orderBy = " userId,signDate,signTime ";

            // #1473334-概述：满足考勤报分部部门显示及导出时显示全路径
            String fullPathMainKey = "show_full_path";
            KQSettingsComInfo kqSettingsComInfo = new KQSettingsComInfo();
            String isShowFullPath = Util.null2String(kqSettingsComInfo.getMain_val(fullPathMainKey),"0");
            String transMethodString = "";

            String colString = "";

            // 根据开关，决定是否显示部门分部全路径
            if (showCol.indexOf("subcom") > -1) {
                if("1".equals(isShowFullPath)){
                    transMethodString = "com.engine.hrm.util.HrmUtil.showSubCompanyFullPath";
                }else {
                    transMethodString = "weaver.hrm.company.SubCompanyComInfo.getSubCompanyname";
                }
                colString += "<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyId1\" transmethod=\""+transMethodString+"\"/>";
            }

            if (showCol.indexOf("dept") > -1) {
                if("1".equals(isShowFullPath)){
                    transMethodString = "com.engine.hrm.util.HrmUtil.showDepartmentFullPath";
                }else {
                    transMethodString = "weaver.hrm.company.DepartmentComInfo.getDepartmentName";
                }
                colString += "<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(27511, user.getLanguage()) + "\" column=\"departmentId\" transmethod=\""+transMethodString+"\"/>";
            }

            if (showCol.indexOf("jobtitle") > -1) {
                colString += "<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(6086, user.getLanguage()) + "\" column=\"jobtitle\" transmethod=\"weaver.hrm.job.JobTitlesComInfo.getJobTitlesname\"/>";
            }
            if (showCol.indexOf("workcode") > -1) {
                colString += "<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(714, user.getLanguage()) + "\" column=\"workcode\" />";
            }
            String isincom_String = "";

          boolean has_incom = false;
          String sql = "select * from kq_settings  where main_key='show_incom' ";
          recordSet.executeQuery(sql);
          if(recordSet.next()){
            String main_val = recordSet.getString("main_val");
            if("1".equalsIgnoreCase(main_val)){
              has_incom = true;
            }
          }
          if(has_incom){
            isincom_String = "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(520084, user.getLanguage()) + "\" column=\"isincom\" otherpara=\"" + user.getLanguage() + "\"  transmethod=\"com.engine.kq.util.KQTransMethod.getIsincom\" />" ;
          }
          new BaseBean().writeLog("==zj=(原始打卡记录sql)"+backFields+sqlFrom+sqlWhere+orderBy);

            String pageUid = "e85dea76-8ac5-c0b1-9221-71742fe87e03";
            String tableString = "" +
                    "<table pageId=\"KQ:originalPunchRpList\" pageUid=\"" + pageUid + "\" tabletype=\"none\" pagesize=\"" + PageIdConst.getPageSize("KQ:originalPunchRpList", user.getUID(), PageIdConst.HRM) + "\" >" +
                    "   <sql backfields=\"" + backFields + "\" sqlform=\"" + sqlFrom + "\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"  sqlorderby=\"" + orderBy + "\"  sqlprimarykey=\"id\" sqlsortway=\"desc\" sqlisdistinct=\"false\"/>" +
                    "   <head>" +
                    "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(25034, user.getLanguage()) + "\" column=\"userId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getResourcename\" orderkey=\"userId\"/>" +
                    colString +
                    "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(18949, user.getLanguage()) + "\" column=\"signDate\" transmethod=\"com.engine.kq.util.KQTransMethod.getSignDateShowNew\" otherpara=\"column:signTime\" />" +
                    "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(390501, user.getLanguage()) + "\" column=\"addr\" />" +
                    "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(32531, user.getLanguage()) + "\" column=\"clientAddress\" />" +
                    "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(28006, user.getLanguage()) + "\" column=\"signFrom\" otherpara=\"" + user.getLanguage() + "\" transmethod=\"com.engine.kq.util.KQTransMethod.getSignFromShow\" />" +
                "       <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(515803, user.getLanguage()) + "\" column=\"deviceinfo\" transmethod=\"com.engine.kq.util.KQTransMethod.getDeviceinfo\" />" +
                isincom_String +
                "   </head>" +
                    "</table>";
            String sessionKey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionKey, tableString);
            resultMap.put("sessionkey", sessionKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}
