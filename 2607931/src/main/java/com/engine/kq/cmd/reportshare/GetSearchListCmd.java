package com.engine.kq.cmd.reportshare;

import com.alibaba.fastjson.JSON;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import weaver.general.BaseBean;
import weaver.general.PageIdConst;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.Map;

/**
 * 考勤报表共享设置--获取查询结果
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

        boolean canDelete = HrmUserVarify.checkUserRight("KQ:ReportShare", user);
        boolean canLog = HrmUserVarify.checkUserRight("KQ:ReportShare", user);

        String resourceName = Util.null2String(params.get("resourceName"));//对象姓名
        String reportName = Util.null2String(params.get("reportName"));//报表名称

        String backFields = " * ";
        String sqlFrom = " from kq_ReportShare ";
        String sqlWhere = " where 1=1 ";
        if (!resourceName.equals("")) {
            sqlWhere += " and (resourceId in (select id from HrmResource where lastname like '%" + resourceName + "%') " +
                    " or resourceId in (select id from HrmResourceManager where lastname like '%" + resourceName + "%'))";
        }
        if (!reportName.equals("") && !reportName.equals("0")) {
            sqlWhere += " and reportName='" + reportName + "'";
        }
        String orderBy = " id ";

        new BaseBean().writeLog("==zj==(考勤共享报表列表sql)" + backFields+sqlFrom+sqlWhere);
        String pageUid = "340e42fa-a02c-fb7c-9c0a-5b3738d6c284";
        String operateString = "";
        operateString = "<operates width=\"20%\">";
        operateString += "<popedom transmethod=\"weaver.hrm.common.SplitPageTagOperate.getBasicOperate\" otherpara=\"" + canDelete + ":" + canLog + "\"></popedom> ";
        operateString += "  <operate href=\"javascript:doDel()\"   text=\"" + SystemEnv.getHtmlLabelName(91, user.getLanguage()) + "\"  index=\"1\"/>";
        operateString += "  <operate href=\"javascript:onLog()\"   text=\"" + SystemEnv.getHtmlLabelName(83, user.getLanguage()) + "\"  index=\"2\"/>";
        operateString += "</operates>";
        String tableString = "" +
                "<table pageId=\"KQ:ReportShareList\" pageUid=\"" + pageUid + "\" tabletype=\"checkbox\" pagesize=\"" + PageIdConst.getPageSize("Kq:ReportShareList", user.getUID(), PageIdConst.HRM) + "\" >" +
                "<sql backfields=\"" + backFields + "\" sqlform=\"" + sqlFrom + "\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"  sqlorderby=\"" + orderBy + "\"  sqlprimarykey=\"id\" sqlsortway=\"asc\" sqlisdistinct=\"false\"/>"
                + operateString +
                "   <head>" +
                "       <col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(15517, user.getLanguage()) + "\" column=\"reportName\" orderkey=\"reportName\" transmethod=\"com.engine.kq.util.KQTransMethod.getReportNameShow\" otherpara=\"" + user.getLanguage() + "\" />" +
                "       <col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(106, user.getLanguage()) + "\" column=\"resourceId\" orderkey=\"resourceId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getResourcename\" otherpara=\"column:resourcetype+column:reportname+column:id\" />" +
                "       <col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(3005, user.getLanguage()) + "\" column=\"shareLevel\" orderkey=\"shareLevel\" transmethod=\"com.engine.kq.util.KQTransMethod.getShareLevelShow\" otherpara=\"" + user.getLanguage() + "\"  />" +
                "       <col width=\"20%\" text=\"" + SystemEnv.getHtmlLabelName(19467, user.getLanguage()) + "\" column=\"id\" orderkey=\"id\" transmethod=\"com.engine.kq.util.KQTransMethod.getReportShareShow\" otherpara=\"" + user.getLanguage() + "\"  />" +
                "   </head>" +
                "</table>";
        String sessionKey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
        Util_TableMap.setVal(sessionKey, tableString);
        resultMap.put("sessionkey", sessionKey);
        return resultMap;
    }
}
