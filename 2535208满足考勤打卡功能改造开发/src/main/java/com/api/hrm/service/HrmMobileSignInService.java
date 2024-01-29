package com.api.hrm.service;

import com.alibaba.fastjson.JSON;
import com.engine.kq.log.KQLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.api.browser.bean.BrowserTabBean;
import com.api.browser.bean.SearchConditionItem;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.bean.SelectOption;
import com.api.hrm.bean.WeaRadioGroup;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.api.hrm.util.PageUidFactory;
import com.cloudstore.dev.api.util.Util_TableMap;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.mobile.signin.SignInManager;
import weaver.hrm.passwordprotection.manager.HrmResourceManager;
import weaver.hrm.resource.ResourceComInfo;
import weaver.mobile.sign.HrmSign;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.mobile.sign.MobileSign;
import weaver.systeminfo.SystemEnv;

/***
 * 外勤签到
 * @author lvyi
 *
 */
public class HrmMobileSignInService extends BaseBean {

    /**
     * 权限判断
     *
     * @param request
     * @param response
     * @return
     */
    public boolean hasRight(HttpServletRequest request, HttpServletResponse response) {
        User user = HrmUserVarify.getUser(request, response);
        String resourceId = Util.null2String(request.getParameter("resourceId"));
        if (resourceId.length() == 0) {
            resourceId = "" + user.getUID();
        }
        String currentUserId = "" + user.getUID();

        RecordSet rs = new RecordSet();
        String departmentId = "";
        rs.executeQuery("select subcompanyid1, departmentId from hrmresource where id = ?",currentUserId);
        if (rs.next()) {
            departmentId = StringUtil.vString(rs.getString("departmentId"));
        }

        boolean hasRight = false;
        List<String> ids = Arrays.asList(new HrmResourceManager().getSubResourceIds(currentUserId).split(","));
        List<String> idList = Arrays.asList(resourceId.split(","));
        if (idList != null && idList.size() > 0) {
            for (String _subId : idList) {
                if (_subId.equals(currentUserId) || ids.indexOf(_subId) != -1) {
                    hasRight = true;
                    break;
                }
            }
        }
        hasRight = resourceId.equals(currentUserId) || hasRight || HrmUserVarify.checkUserRight("MobileSignInfo:Manage", user, departmentId);
        return hasRight;
    }

    public Map<String, Object> getSearchCondition(Map<String, Object> params, User user) {
        Map<String, Object> resultMap = new HashMap<String, Object>();


        /**
         * 考勤类型
         * 全部、外勤签到、移动端考勤
         */
        List<Map<String, Object>> groupList = new ArrayList<Map<String, Object>>();
        Map<String, Object> groupItem = new HashMap<String, Object>();
        List<Object> itemList = new ArrayList<Object>();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        SearchConditionItem searchConditionItem = null;
        HrmFieldBean hrmFieldBean = null;

        groupItem.put("title", SystemEnv.getHtmlLabelName(1889, user.getLanguage()));
        groupItem.put("defaultshow", true);

        WeaRadioGroup weaRadioGroup = new WeaRadioGroup();
        List<Object> options = new ArrayList<Object>();//WeaRadioGroup组件内的options参数
        List<String> domkey = new ArrayList<String>();//WeaRadioGroup组件内的domkey参数
        Map<String, Object> selectLinkageDatas = new HashMap<String, Object>();//WeaRadioGroup组件内的selectLinkageDatas参数
        Map<String, Object> linkMap = new HashMap<String, Object>();//selectLinkageDatas参数内部Map数据

        weaRadioGroup.setLabel(SystemEnv.getHtmlLabelName(16070, user.getLanguage()));

        options.add(new SelectOption("0", SystemEnv.getHtmlLabelName(332, user.getLanguage()), true));//全部
        options.add(new SelectOption("1", SystemEnv.getHtmlLabelName(82634, user.getLanguage())));//外勤签到
        options.add(new SelectOption("2", SystemEnv.getHtmlLabelName(386502, user.getLanguage())));//移动端考勤
        weaRadioGroup.setOptions(options);

        domkey.add("kqtype");
        weaRadioGroup.setDomkey(domkey);

        linkMap.put("conditionType", "DATEPICKER");
        linkMap.put("viewAttr", 3);
        linkMap.put("format", "YYYY");
        domkey = new ArrayList<String>();
        domkey.add("selectedYear");
        linkMap.put("domkey", domkey);
        selectLinkageDatas.put("6", linkMap);
        weaRadioGroup.setSelectLinkageDatas(selectLinkageDatas);

        weaRadioGroup.setLabelcol(4);
        weaRadioGroup.setFieldcol(20);
        itemList.add(weaRadioGroup);

        /**
         * 数据范围
         * 总部、分部、部门、人员、我的下属(管理员没有我的下属)
         * //如果人员是否在考勤报表权限共享设置里，则表示此人拥有权限，否则，没有权限
         */
        boolean hasRight = false;
//        String sql = "select * from kq_ReportShare where resourceId=" + user.getUID() + " and reportName=4";
//        RecordSet recordSet = new RecordSet();
//        recordSet.executeQuery(sql);
//        hasRight = recordSet.getCounts() > 0;

        RecordSet recordSet = new RecordSet();
        String sql ="";

        hasRight = HrmUserVarify.checkUserRight("MobileSignInfo:Manage", user);//是否有 移动签到情况管理 权限

        //获取人员的权限级别  rightLevel  0-部门；1-分部；2-总部
        String rightLevel = HrmUserVarify.getRightLevel("MobileSignInfo:Manage", user);
        /*判断当前登录人员是否有下级*/
        boolean hasSubordinate = false;
        sql = "select count(*) from HrmResource where managerId=" + user.getUID();
        recordSet.executeQuery(sql);
        if (recordSet.next()) {
            hasSubordinate = recordSet.getInt(1) > 0;
        }

        weaRadioGroup = new WeaRadioGroup();
        options = new ArrayList<Object>();//WeaRadioGroup组件内的options参数
        domkey = new ArrayList<String>();//WeaRadioGroup组件内的domkey参数
        selectLinkageDatas = new HashMap<String, Object>();//WeaRadioGroup组件内的selectLinkageDatas参数

        weaRadioGroup.setLabel(SystemEnv.getHtmlLabelName(34216, user.getLanguage()));//数据范围

        if (hasRight) {
            if(rightLevel.equals("2")){
                options.add(new SelectOption("0", SystemEnv.getHtmlLabelName(140, user.getLanguage()), hasRight && user.isAdmin()));//总部
            }
            if(rightLevel.equals("2")||rightLevel.equals("1")){
                options.add(new SelectOption("1", SystemEnv.getHtmlLabelName(33553, user.getLanguage())));//分部
            }
            if(rightLevel.equals("2")||rightLevel.equals("1")||rightLevel.equals("0")){
                options.add(new SelectOption("2", SystemEnv.getHtmlLabelName(27511, user.getLanguage())));//部门
            }
            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("subcomId");//分部
            hrmFieldBean.setFieldlabel("33553");
            hrmFieldBean.setFieldhtmltype("3");
            hrmFieldBean.setType("194");
            hrmFieldBean.setViewAttr(3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|string");
            List<BrowserTabBean> browserTabBeanList = searchConditionItem.getBrowserConditionParam().getTabs();
            for (BrowserTabBean browserTabBean : browserTabBeanList) {
                if (browserTabBean.getKey().equals("2")) {
                    browserTabBean.setSelected(true);
                }
            }
            selectLinkageDatas.put("1", searchConditionItem);

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("deptId");//部门
            hrmFieldBean.setFieldlabel("27511");
            hrmFieldBean.setFieldhtmltype("3");
            hrmFieldBean.setType("57");
            hrmFieldBean.setViewAttr(3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|string");
            browserTabBeanList = searchConditionItem.getBrowserConditionParam().getTabs();
            for (BrowserTabBean browserTabBean : browserTabBeanList) {
                if (browserTabBean.getKey().equals("2")) {
                    browserTabBean.setSelected(true);
                }
            }
            selectLinkageDatas.put("2", searchConditionItem);
        }

        options.add(new SelectOption("3", SystemEnv.getHtmlLabelName(30042, user.getLanguage()), (!hasRight || !user.isAdmin())));//人员
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("resourceId");//人员
        hrmFieldBean.setFieldlabel("30042");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("17");
        hrmFieldBean.setViewAttr(hasRight ? 3 : 1);
        if (!hasRight || !user.isAdmin()) {
            hrmFieldBean.setFieldvalue("" + user.getUID());
        }
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        if (!hasRight) {
            Map<String, Object> OtherParamsMap = new HashMap<String, Object>();
            OtherParamsMap.put("hasBorder", true);
            searchConditionItem.setOtherParams(OtherParamsMap);
        }
        searchConditionItem.setRules("required|string");
        selectLinkageDatas.put("3", searchConditionItem);

        if (!user.isAdmin() && hasSubordinate) {
            options.add(new SelectOption("4", SystemEnv.getHtmlLabelName(15089, user.getLanguage())));//我的下属

            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("allLevel");//包含下级下属
            hrmFieldBean.setFieldlabel("389995");
            hrmFieldBean.setFieldhtmltype("4");
            hrmFieldBean.setType("1");
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            selectLinkageDatas.put("4", searchConditionItem);
        }

        weaRadioGroup.setOptions(options);
        weaRadioGroup.setSelectLinkageDatas(selectLinkageDatas);

        domkey.add("dataScope");
        weaRadioGroup.setDomkey(domkey);

        weaRadioGroup.setLabelcol(4);
        weaRadioGroup.setFieldcol(20);
        itemList.add(weaRadioGroup);

        groupItem.put("items", itemList);
        groupList.add(groupItem);
        resultMap.put("condition", groupList);
        return resultMap;
    }


    /**
     * 外勤签到时间视图
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> getMobileSignInTimeData(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();

        try {
            User user = HrmUserVarify.getUser(request, response);
            Map<String, String> dateScope = getDate(request);
            CustomerInfoComInfo customerInfoComInfo = new CustomerInfoComInfo();

            ResourceComInfo resourceComInfo = new ResourceComInfo();
            List<Object> lsTimeData = new ArrayList<Object>();
            Map<String, Object> date = null;
            List<Object> lsitems = null;
            Map<String, Object> item = null;

            SignInManager signIn = new SignInManager();
            String resourceId = Util.null2String(request.getParameter("resourceId"));
            String signtype = Util.null2String(request.getParameter("signtype"));
            String cmd = Util.null2String(request.getParameter("cmd"));
            if (signtype.equals("1")) {
                signtype = "";
            } else {
                signtype = "2";
            }

            String fromDate = dateScope.get("fromDate");
            String toDate = dateScope.get("toDate");
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }

            if (!hasRight(request, response)) {
                retmap.put("status", "-1");
                retmap.put("hasRight", false);
                return retmap;
            }

            Map datas = signIn.getData(resourceId, fromDate + " 00:00:00", toDate + " 23:59:59", 1, 1000, signtype);

          List signs = null;
            if (datas != null && !datas.isEmpty()) {
                signs = (List) datas.get("list");
                signs = signs == null ? new ArrayList() : signs;
            }
            String operDate = "";
            MobileSign mSign = null;
            if(fromDate.compareTo(toDate)>0){
                retmap.put("status", "-1");
                retmap.put("api_errormsg", "fromDate="+fromDate+",toDate="+toDate);
                return retmap;
            }
            int num = 0;
            boolean isEnd = false;
            boolean hasResult = false;
            for (String startDate = toDate; !isEnd; ) {
                num++;
                date = new HashMap<String, Object>();
                lsTimeData.add(date);
                date.put("date", startDate + " " + com.engine.portal.util.DateUtil.getDayWeekOfDate1(DateUtil.parseToDate(startDate)));
//                date.put("date", startDate + " " + getWeekDay(DateUtil.getWeek(startDate), user));
                lsitems = new ArrayList<Object>();
                date.put("items", lsitems);

                if (startDate.equals(fromDate) || num >33) {
                    isEnd = true;
                }

                for (int i = 0; i < signs.size(); i++) {
                    mSign = (MobileSign) signs.get(i);
                    operDate = mSign.getOperateDate();
                    if (!operDate.equals(startDate)) continue;
                    if (!hasResult) hasResult = true;

                    item = new HashMap<String, Object>();
                    lsitems.add(item);

                    item.put("name", resourceComInfo.getLastname(mSign.getOperaterId()));
                    item.put("time", mSign.getOperateTime());
                    item.put("signTitle", signIn.getShowName(mSign.getOperateType(),user));
                    item.put("information", mSign.getRemark());
                    List<String> pics = new ArrayList<String>();
                    String[] ids = mSign.getAttachmentIds().split(",");
                    int idSize = ids == null ? 0 : ids.length;
                    for (int x = 0; x < idSize; x++) {
                        if (Util.null2String(ids[x]).length() == 0) continue;
                        pics.add("/weaver/weaver.file.FileDownload?fileid=" + ids[x]);
                    }
                    item.put("pics", pics);
                    item.put("x", mSign.getLongitude());
                    item.put("y", mSign.getLatitude());
                    item.put("title", mSign.getAddress());
                    List<String> crmList = new ArrayList<>();
                    String crm = Util.null2String(mSign.getCrm());
                    if(!"".equals(crm)){
                        List<String> crmIds = Util.splitString2List(crm, ",");
                        for(String key : crmIds) {
                            crmList.add(customerInfoComInfo.getCustomerInfoname(key));
                        }
                    }
                    item.put("crm", crmList);
                }
                startDate = DateUtil.addDate(startDate, -1);
            }
            if (cmd.equals("mapData") && !hasResult) {
                lsTimeData.clear();
            }
            retmap.put("result", lsTimeData);
            retmap.put("status", "1");
        } catch (Exception e) {
            retmap.put("api_status", false);
            retmap.put("api_errormsg", e.getMessage());
            e.printStackTrace();
        }
        return retmap;
    }


    public Map<String, String> getDate(HttpServletRequest request) {
        Map<String, String> retmap = new HashMap<String, String>();

        String cmd = Util.null2String(request.getParameter("cmd"));
        String fromDate = Util.null2String(request.getParameter("fromDate"));
        String toDate = Util.null2String(request.getParameter("toDate"));
        if (cmd.equals("timeData") || cmd.equals("mapData")) {
            if (fromDate.length() == 7) {
                fromDate = fromDate + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }
        } else if (cmd.equals("detialData")) {
            String typeselect = Util.null2String(request.getParameter("typeselect"));
            if (!typeselect.equals("") && !typeselect.equals("0") && !typeselect.equals("6")) {
                fromDate = TimeUtil.getDateByOption(typeselect, "0");
                toDate = TimeUtil.getDateByOption(typeselect, "1");
            }
        }

        retmap.put("fromDate", fromDate);
        retmap.put("toDate", toDate);
        return retmap;
    }

    public Map<String, Object> getMobileSignInList(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        
        try {
            User user = HrmUserVarify.getUser(request, response);
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            Map<String, String> dateScope = getDate(request);
//            String resourceId = Util.null2String(request.getParameter("resourceId"));
//            String signtype = Util.null2String(request.getParameter("signtype"));

            /**
             * 数据范围
             * dataScope：0-总部、1-分部、2-分部、3-人员、4-我的下属
             * kqtype: 0-全部 、1-外勤签到、2-移动端签到
             * subcomId：指定分部ID
             * deptId：指定部门ID
             * resourceId：指定人员ID
             * allLevel：是否包含下级下属：0-不包含、1-包含
             */
            String kqtype = Util.null2String(request.getParameter("kqtype"));
            String dataScope = Util.null2String(request.getParameter("dataScope"));
            String resourceId = Util.null2String(request.getParameter("resourceId"));
            String deptId = Util.null2String(request.getParameter("deptId"));
            String subcomId = Util.null2String(request.getParameter("subcomId"));
            String allLevel = Util.null2String(request.getParameter("allLevel"));
            String fromDate = dateScope.get("fromDate");
            String toDate = dateScope.get("toDate");
            if (resourceId.length() == 0) {
                resourceId = "" + user.getUID();
            }
            
            if (!hasRight(request, response)) {
                String pageUid = PageUidFactory.getHrmPageUid("HrmMobileSignInList");
                String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
                String backfields = " uniqueid,id,operater,operate_type,operate_date,operate_time,LONGITUDE,LATITUDE,address,remark,attachment,signtype,canViewSignImg,crm ";
                String fromSql = " ";
                String sqlWhere = " where 1=2 ";
                String orderby = " operate_date desc,operate_time desc ";
                String tableString =
                        " <table pageId=\"" + pageUid + "\" pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"none\">" +
                                " 	<sql backfields=\"" + backfields + "\" sqlform=\"" + Util.toHtmlForSplitPage(fromSql) + "\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"  sqlorderby=\"" + orderby + "\"  sqlprimarykey=\"uniqueid\" sqlsortway=\"Asc\" sqlisdistinct=\"true\"/>" +
                                "	<head>" +
                                "	  <col width=\"12%\" text=\"" + SystemEnv.getHtmlLabelName(33563, user.getLanguage()) + "\" column=\"operate_date\" orderkey=\"operate_date\"/>" +
                                "		<col width=\"12%\" text=\"" + SystemEnv.getHtmlLabelName(20035, user.getLanguage()) + "\" column=\"operate_time\" orderkey=\"operate_time\"/>" +
                                "		<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(125530, user.getLanguage()) + "\" column=\"operater\" orderkey=\"operater\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getLastname\"/>" +
                                "		<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" transmethod=\"com.api.hrm.util.HrmTransMethod.getSubCompanyNameByResourceId\" otherpara=\"column:operater\"/>" +
                                "		<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(124, user.getLanguage()) + "\"  column=\"departmentid\"  transmethod=\"com.api.hrm.util.HrmTransMethod.getDepartmentNameByResourceId\" otherpara=\"column:operater\"/>" +
                                "		<col width=\"21%\" text=\"" + SystemEnv.getHtmlLabelName(125531, user.getLanguage()) + "\" column=\"address\" orderkey=\"address\"/>" +
                                "		<col width=\"21%\" text=\"" + SystemEnv.getHtmlLabelName(136, user.getLanguage()) + "\" column=\"crm\" orderkey=\"crm\" transmethod=\"com.api.hrm.util.HrmTransMethod.getCrm\"/>" +
                                "		<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(454, user.getLanguage()) + "\" column=\"remark\" orderkey=\"remark\" transmethod=\"com.api.hrm.util.HrmTransMethod.getSignRemark\" otherpara=\"column:signtype+" + user.getLanguage() + "\"/>" +
                                "	  <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(25996, user.getLanguage()) + "\" column=\"operate_type\" orderkey=\"operate_type\" transmethod=\"com.api.hrm.util.HrmTransMethod.getSignTypeShowName\" otherpara=\"" + user.getLanguage() + "\"/>" +
                                "	  <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(503320, user.getLanguage()) + "\" column=\"canViewSignImg\" transmethod=\"com.api.hrm.util.HrmTransMethod.canViewSignImg\" />" +
                                "	</head>" +
                                " </table>";
                Util_TableMap.setVal(sessionkey, tableString);
                retmap.put("sessionkey", sessionkey);
                retmap.put("status", "-1");
                retmap.put("hasRight", false);
                return retmap;
            }

            //搜索结果  对权限级别的范围进行控制
            String _level = "";
            String rightSql = "";
            String rightresourceId = "";
            String rightdeptId = "";
            String rightsubcomId = "";
            if(!hasRight(request, response)){
                rightresourceId = ""+user.getUID();
                rightSql += "  id in (" + rightresourceId + ")";
            } else {
                _level = StringUtil.vString(HrmUserVarify.getRightLevel("MobileSignInfo:Manage", user));
                if(user.isAdmin()){
                }else {
                    if (_level.equals("0")) {
                        rightdeptId = "" + user.getUserDepartment();
                        rightSql += "  departmentId in (" + rightdeptId + ") ";
                    } else if (_level.equals("1")) {
                        rightsubcomId = "" + user.getUserSubCompany1();
                        rightSql += "  subcompanyId1 in (" + rightsubcomId + ") ";
                    }
                }
            }


            String currentUserId = Util.null2String(user.getUID());
            String subids = Util.null2String(new HrmResourceManager().getSubResourceIds(currentUserId));
            if(!subids.equals("")){
                if(!rightSql.equals("")){
                    rightSql = " ((" + rightSql+ ")" + " or (id in (" + subids + ")))";
                }
            }

            String beginQueryDate = "";
            String endQueryDate = "";
            if (fromDate.length() > 0) {
                beginQueryDate = fromDate + " 00:00:00";
            }
            if (toDate.length() > 0) {
                endQueryDate = toDate + " 23:59:59";
            }
            String resourceSql = "select id from HrmResource where status in (0,1,2,3,5)";
//            if (!"".equals(resourceId)) {
//                resourceSql += " and id in (" + resourceId + ")";
//            }
            if (dataScope.equals("0")) {
                //总部
            } else if (dataScope.equals("1")) {
                resourceSql += " and subcompanyId1 in (" + subcomId + ") ";
            } else if (dataScope.equals("2")) {
                resourceSql += " and departmentId in (" + deptId + ") ";
            } else if (dataScope.equals("3")) {
                resourceSql += " and id in (" + resourceId + ")";
            } else if (dataScope.equals("4")) {
                if (allLevel.equals("1")) {
                    resourceSql += " and ( managerStr like '%," + user.getUID() + ",%' )";
                } else {
                    resourceSql += " and ( managerid = " + user.getUID() + ")";
                }
            }

            if(!"".equals(rightSql)){
                resourceSql = resourceSql + " and " +rightSql;
            }

            String backfields = " uniqueid,id,operater,operate_type,operate_date,operate_time,LONGITUDE,LATITUDE,address,remark,attachment,signtype,canViewSignImg,crm ";
            String fromSql = " ";
            String sqlWhere = " where 1=1 ";
            String orderby = " operate_date desc,operate_time desc ";


            String hrmSignSql = HrmSign.CreateHrmSignSql4E9(resourceSql, beginQueryDate, endQueryDate);
            String mobileSignSql = MobileSign.CreateMobileSignSql(resourceSql, beginQueryDate, endQueryDate);
            
            String UNIONsql = "";
            if ("2".equals(kqtype)) {
                UNIONsql = hrmSignSql;
            } else if ("1".equals(kqtype)) {
                UNIONsql = mobileSignSql;
            } else {
                UNIONsql = hrmSignSql + " UNION " + mobileSignSql;
            }

            fromSql = "( " + UNIONsql + " ) t";

            String pageUid = PageUidFactory.getHrmPageUid("HrmMobileSignInList");
            //System.out.println("SELECT " + backfields + " from " + fromSql + " " + sqlWhere + " ORDER BY " + orderby);
            new KQLog().info("getMobileSignInList>>>11 SELECT " + backfields + " from " + fromSql + " " + sqlWhere + " ORDER BY " + orderby);
            String tableString =
                    " <table pageId=\"" + pageUid + "\" pageUid=\"" + pageUid + "\" pagesize=\"10\" tabletype=\"none\">" +
                            " 	<sql backfields=\"" + backfields + "\" sqlform=\"" + Util.toHtmlForSplitPage(fromSql) + "\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"  sqlorderby=\"" + orderby + "\"  sqlprimarykey=\"uniqueid\" sqlsortway=\"Asc\" sqlisdistinct=\"true\"/>" +
                            "	<head>" +
                            "	  <col width=\"12%\" text=\"" + SystemEnv.getHtmlLabelName(33563, user.getLanguage()) + "\" column=\"operate_date\" orderkey=\"operate_date\"/>" +
                            "		<col width=\"12%\" text=\"" + SystemEnv.getHtmlLabelName(20035, user.getLanguage()) + "\" column=\"operate_time\" orderkey=\"operate_time\"/>" +
                            "		<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(125530, user.getLanguage()) + "\" column=\"operater\" orderkey=\"operater\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getLastname\"/>" +
                            "		<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" transmethod=\"com.api.hrm.util.HrmTransMethod.getSubCompanyNameByResourceId\" otherpara=\"column:operater\"/>" +
                            "		<col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(124, user.getLanguage()) + "\"  column=\"departmentid\"  transmethod=\"com.api.hrm.util.HrmTransMethod.getDepartmentNameByResourceId\" otherpara=\"column:operater\"/>" +
                            "		<col width=\"21%\" text=\"" + SystemEnv.getHtmlLabelName(125531, user.getLanguage()) + "\" column=\"address\" orderkey=\"address\"/>" +
                            "		<col width=\"21%\" text=\"" + SystemEnv.getHtmlLabelName(136, user.getLanguage()) + "\" column=\"crm\" orderkey=\"crm\" transmethod=\"com.api.hrm.util.HrmTransMethod.getCrm\"/>" +
                            "		<col width=\"15%\" text=\"" + SystemEnv.getHtmlLabelName(454, user.getLanguage()) + "\" column=\"remark\" orderkey=\"remark\" transmethod=\"com.api.hrm.util.HrmTransMethod.getSignRemark\" otherpara=\"column:signtype+" + user.getLanguage() + "\"/>" +
                            "	  <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(25996, user.getLanguage()) + "\" column=\"operate_type\" orderkey=\"operate_type\" transmethod=\"com.api.hrm.util.HrmTransMethod.getSignTypeShowName\" otherpara=\"" + user.getLanguage() + "\"/>" +
                            "	  <col width=\"10%\" text=\"" + SystemEnv.getHtmlLabelName(503320, user.getLanguage()) + "\" column=\"canViewSignImg\" transmethod=\"com.api.hrm.util.HrmTransMethod.canViewSignImg\" />" +
                            "	</head>" +
                            " </table>";
            
            //主要用于 显示定制列以及 表格 每页展示记录数选择
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            Util_TableMap.setVal(sessionkey, tableString);
            retmap.put("status", "1");
            retmap.put("sessionkey", sessionkey);
        } catch (Exception e) {
            retmap.put("api_status", false);
            retmap.put("api_errormsg", e.getMessage());
            e.printStackTrace();
        }
        
        return retmap;
    }

    public String getWeekDay(int weekday, User user) {
        String result = "";
        switch (weekday) {
            case 1:
                result = SystemEnv.getHtmlLabelName(392, user.getLanguage());
                break;
            case 2:
                result = SystemEnv.getHtmlLabelName(393, user.getLanguage());
                break;
            case 3:
                result = SystemEnv.getHtmlLabelName(394, user.getLanguage());
                break;
            case 4:
                result = SystemEnv.getHtmlLabelName(395, user.getLanguage());
                break;
            case 5:
                result = SystemEnv.getHtmlLabelName(396, user.getLanguage());
                break;
            case 6:
                result = SystemEnv.getHtmlLabelName(397, user.getLanguage());
                break;
            case 7:
                result = SystemEnv.getHtmlLabelName(398, user.getLanguage());
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * 获取外勤签到的图片的ID
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> getSignImgIds(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String signImgIds = "";
        try {
            User user = HrmUserVarify.getUser(request, response);

            String id = Util.null2String(request.getParameter("id"));
            if (id.indexOf("sign") > -1) {
                id = id.substring(4);
                String sql = "select * from Mobile_Sign where id=?";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql, id);
                if (recordSet.next()) {
                    signImgIds = recordSet.getString("attachment");
                }
            }
            //qc2535208 考勤打卡图片
            if (id.indexOf("hrm") > -1) {
                id = id.substring(3);
                String sql = "select * from HrmScheduleSign where id=?";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(sql, id);
                if (recordSet.next()) {
                    signImgIds = recordSet.getString("attachment");
                }
            }


            List<String> signImgIdList = Util.TokenizerString(signImgIds, ",");
            for (int i = 0; i < signImgIdList.size(); i++) {
               String str = DocDownloadCheckUtil.checkPermission(signImgIdList.get(i), user);

               signImgIdList.set(i, str);
            }
            if (signImgIdList != null && signImgIdList.size() > 0) {
                signImgIds = String.join(",", signImgIdList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        resultMap.put("signImgIds", signImgIds);
        return resultMap;
    }
}
