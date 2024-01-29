package com.api.browser.service.impl;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.api.browser.bean.BrowserTabBean;
import com.api.browser.bean.ListHeadBean;
import com.api.browser.biz.Dao_Hrm4Ec;
import com.api.browser.biz.Dao_Hrm4EcFactory;
import com.api.browser.biz.HrmComVirtualBean;
import com.api.browser.biz.Tree;
import com.api.browser.biz.TreeNode;
import com.api.browser.util.BoolAttr;
import com.api.customization.qc2607932.util.PermissionUtil;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.blog.util.BlogDiscussShareUtil;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.service.HrmCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.encrypt.biz.EncryptFieldViewScopeConfigComInfo;
import com.engine.hrm.service.impl.BrowserDisplayFieldServiceImpl;
import com.engine.msgcenter.constant.HrmPracticalConstant;
import com.engine.msgcenter.util.HrmPracticalUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.filter.XssUtil;
import weaver.general.*;
import weaver.general.browserData.BrowserManager;
import weaver.hrm.HrmTransMethod;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.cachecenter.CacheLoadQueueManager;
import weaver.hrm.cachecenter.entry.OrgUpOrDownTypeStyle;
import weaver.hrm.cachecenter.entry.SimpleObjtypeInfo;
import weaver.hrm.cachecenter.util.ObjtypeComparor;
import weaver.hrm.common.database.dialect.DialectUtil;
import weaver.hrm.company.CompanyComInfo;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.CompanyVirtualComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.companyvirtual.ResourceVirtualComInfo;
import weaver.hrm.companyvirtual.SubCompanyVirtualComInfo;
import weaver.hrm.group.HrmGroupTreeComInfo;
import weaver.hrm.job.JobActivitiesComInfo;
import weaver.hrm.job.JobCallComInfo;
import weaver.hrm.job.JobGroupsComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.privacy.PrivacyComInfo;
import weaver.hrm.resource.MutilResourceBrowser;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.resource.controller.HrmWorkflowAdvanceManager;
import weaver.hrm.resource.controller.analysis.HrmOrgForwardBean;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.setting.HrmUserSettingComInfo;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.service.BrowserService;
import com.api.browser.util.BrowserConstant;
import com.api.browser.util.ConditionFactory;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.ConditionType;
import com.engine.common.service.WorkflowCommonService;
import com.engine.common.service.impl.WorkflowCommonServiceImpl;
import com.engine.common.util.ParamUtil;

/**
 * 人力资源
 *
 * @author jhy Apr 18, 2017
 */
public class ResourceBrowserService extends BrowserService {
    private List<Map> browserFieldConfig = new ArrayList<>();
    private List<String> displayFields = new ArrayList();
    private String displayFieldsStr = "";
    private List<String> browserFieldFilterKeys = new ArrayList<>(Arrays.asList(
//            "lastnamespan",
            "departmentidspan",
            "subcompanyid1span",
            "orgidspan"
    ));

    private List<String> dataRanageAllSubCompanyIds = new ArrayList<String>();
    private List<String> dataRanageAllDepartmentIds = new ArrayList<String>();
    private int[] allSubComIds = null;//有权限的机构，包含必要的构成树的上级
    private int[] subComIds = null;//有权限的机构，不包含必要的构成树的上级
    private String rightStr= null;//权限字符串
    private String[] allChildSubComIds = null;//人员所属分部的下级分部
    private String[] allChildDeptIds = null;//人员所属部门的下级部门
    private int[] depIds= null;//角色级别是部门的，只能看到自己的部门
    private boolean isExecutionDetach = false;//是否执行力分权
    private Map<String,Object> params = null;
    @Override
    public Map<String, Object> browserAutoComplete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        ResourceComInfo resourceComInfo = new ResourceComInfo();
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        String virtualtype = Util.null2String(request.getParameter("virtualtype"));
        //人力资源 165 分权单人力 166 分权多人力
        String whereClause = Util.null2String(request.getParameter("sqlwhere"));

        WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
        Map<String, Object> params = ParamUtil.request2Map(request);
        this.params = params;

        String agentorbyagentid = Util.null2String(params.get("agentorbyagentid"));
        User finalUser = CacheLoadQueueManager.getFinalUser(user, agentorbyagentid);
        user = finalUser;

        params.put("tableAlias", "t1");
        Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);

        if (Util.null2String(dataRanage.get("sqlWhere")).length() > 0) {
            if (whereClause.length() > 0)
                whereClause += "and";
            whereClause += Util.null2String(dataRanage.get("sqlWhere"));
        }

        if (whereClause.equals("")) {
            whereClause = "t1.departmentid = t2.id";
        } else {
            whereClause += " and t1.departmentid = t2.id";
        }
        if (Util.getIntValue(virtualtype) < -1) {
            whereClause += " and t1.virtualtype = " + virtualtype;
        }

        if (whereClause.indexOf("status") != -1) {
            //包含status 说明外部已控制状态
        } else {
            //只显示在职人员
            if (whereClause.equals("")) {
                whereClause += " (t1.status = 0 or t1.status = 1 or t1.status = 2 or t1.status = 3) ";
            } else {
                whereClause += " and (t1.status = 0 or t1.status = 1 or t1.status = 2 or t1.status = 3) ";
            }
        }

        HttpSession session = request.getSession();
        if (browserType.equals("165") || browserType.equals("166")) {
            int beagenter = Util.getIntValue((String) session.getAttribute("beagenter_" + user.getUID()));
            if (beagenter <= 0) {
                beagenter = user.getUID();
            }
            int fieldid = Util.getIntValue(request.getParameter("fieldid"));
            int isdetail = Util.getIntValue(request.getParameter("viewtype"));
            int isbill = Util.getIntValue(request.getParameter("isbill"), 1);
            if (fieldid != -1) {
                CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
                checkSubCompanyRight.setDetachable(1);
                checkSubCompanyRight.setIsbill(isbill);
                checkSubCompanyRight.setFieldid(fieldid);
                checkSubCompanyRight.setIsdetail(isdetail);
                boolean onlyselfdept = checkSubCompanyRight.getDecentralizationAttr(beagenter, "Resources:decentralization", fieldid, isdetail, isbill);
                boolean isall = checkSubCompanyRight.getIsall();
                String departments = Util.null2String(checkSubCompanyRight.getDepartmentids());
                String subcompanyids = Util.null2String(checkSubCompanyRight.getSubcompanyids());
                if (!isall) {
                    if (onlyselfdept) {
                        if (departments.length() > 0 && !departments.equals("0")) {
                            whereClause += " and t1.departmentid in(" + departments + ")";
                        }
                    } else {
                        if (subcompanyids.length() > 0 && !subcompanyids.equals("0")) {
                            whereClause += " and t1.subcompanyid1 in(" + subcompanyids + ")";
                        }
                    }
                }
            }
        } else if (browserType.equals("160")) {
            String roleids = Util.null2String(request.getParameter("roleid"));
            ArrayList resourcrole = Util.TokenizerString(roleids, "_");
            String roleid = "0";
            int uid = user.getUID();
            if (resourcrole.size() > 0) roleid = "" + resourcrole.get(0);
            int index = roleid.indexOf("a");
            int rolelevel = 0;
            if (index > -1) {
                int roledid_tmp = Util.getIntValue(roleid.substring(0, index), 0);
                String rolelevelStr = roleid.substring(index + 1);

                roleid = "" + roledid_tmp;
                index = rolelevelStr.indexOf("b");
                if (index > -1) {
                    rolelevel = Util.getIntValue(rolelevelStr.substring(0, index), 0);
                    uid = Util.getIntValue(rolelevelStr.substring(index + 1), 0);
                    if (uid <= 0) {
                        uid = user.getUID();
                    }
                } else {
                    rolelevel = Util.getIntValue(rolelevelStr);
                }
            }

            if (roleid.length() == 0) {
                whereClause += " and 1=2 ";
            } else {
                whereClause += " and t1.ID in (select ResourceID from hrmrolemembers a,hrmroles b where a.roleid = b.ID and b.ID=" + roleid + ")";
            }

            if (rolelevel != 0) {
                if (rolelevel == 1) {
                    int subcomid = Util.getIntValue(resourceComInfo.getSubCompanyID("" + uid), 0);
                    whereClause += " and t1.subcompanyid1=" + subcomid + " ";
                } else if (rolelevel == 2) {
                    int subcomid = Util.getIntValue(resourceComInfo.getSubCompanyID("" + uid), 0);
                    int supsubcomid = Util.getIntValue(subCompanyComInfo.getSupsubcomid("" + subcomid), 0);
                    whereClause += " and t1.subcompanyid1=" + supsubcomid + " ";
                } else if (rolelevel == 3) {
                    int departid = Util.getIntValue(resourceComInfo.getDepartmentID("" + uid), 0);
                    whereClause += " and t1.departmentid=" + departid + " ";
                }
            }
        }
        //Added by wcd 2014-11-28 增加分权控制 start
        AppDetachComInfo appDetachComInfo = new AppDetachComInfo();
        String tempSql = appDetachComInfo.getScopeSqlByHrmResourceSearch(String.valueOf(user.getUID()), true, "resource_t1");
        whereClause += (tempSql == null || tempSql.length() == 0) ? "" : (" and " + tempSql);
        //Added by wcd 2014-11-28 增加分权控制 end

        String workflowid = Util.null2String(params.get("bdf_wfid"));
        String nodeid = Util.null2String(params.get("nodeid"));
        String forwardflag = Util.null2String(params.get("forwardflag"));


        HrmOrgForwardBean hrmOrgForwardBean = CacheLoadQueueManager.getForwordBean(workflowid, nodeid, forwardflag, finalUser, false, true);
        whereClause = HrmWorkflowAdvanceManager.resetSqlWhere(hrmOrgForwardBean, whereClause, "t1");

        BrowserManager browserManager = new BrowserManager();
        browserManager.setType(browserType);
        browserManager.setOrderKey("t1.dsporder");
        browserManager.setOrderWay("asc");
        String result = "";
        //加载人力资源浏览按钮显示列定义
        List browserDisplayColumns = loadDisplayColumnConfig();
        if (Util.getIntValue(virtualtype) < -1) {
            result = browserManager.getResult(request, "t1.id,lastname,departmentname", "HrmResourcevirtualview t1,hrmdepartmentvirtual t2", whereClause, PAGENUM, "t1");
        } else {
            result = browserManager.getResult(request, "t1.id,lastname,departmentname", "hrmresource t1,hrmdepartment t2", whereClause, PAGENUM, "t1");
        }

        DepartmentComInfo deptComInfo = new DepartmentComInfo();
        JSONArray arr = (JSONArray) JSON.parse(result);

        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        List<String> ids = new ArrayList();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject userInfo = (JSONObject) arr.get(i);
            ids.add(userInfo.getString("id"));
        }

        // 做广播权限和应用分权的交集
        String radioType = Util.null2String(request.getParameter("radioType"));
        if(HrmPracticalConstant.RADIO_TYPE.equals(radioType)){

            List<String> userIdList = HrmPracticalUtil.getUserIdS(user.getUID(),HrmPracticalConstant.BROAD_CASTER,HrmPracticalConstant.SEND_RANGE);

            List<String> userIdListBak = new ArrayList<>();
            if(CollectionUtils.isEmpty(userIdList))
                ids.clear();
            else {
                for(String userId : userIdList)
                    if(ids.contains(userId))
                        userIdListBak.add(userId);
                    ids = userIdListBak;
            }
        }


        if(ids.size() > 0){
            for(String id : ids){
                Map<String, Object> userInfo = getUserInfo(id, user, false);
                String title = "";
                for(int i = 0; i < browserFieldConfig.size(); i++){
                    Map f = browserFieldConfig.get(i);
                    String fieldName = Util.null2String(f.get("fieldName")) + "span";
                    String value = Util.null2String(userInfo.get(fieldName));

                    if(i == 0)
                        title += value;
                    else{
                        if(title.equals(""))
                            title += value;
                        else if(!value.equals(""))
                            title += "|" + value;
                    }
                }
                userInfo.put("name", userInfo.get("lastname"));
                userInfo.put("title", title);
                users.add(userInfo);
            }
        }
        apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, net.sf.json.JSONArray.fromObject(users));

//        for (int i = 0; i < arr.size(); i++) {
//            JSONObject userInfo = (JSONObject) arr.get(i);
//            String resourceid = userInfo.getString("id");
//            userInfo.put("lastname", resourceComInfo.getLastname(resourceid));
//            userInfo.put("jobtitlename", MutilResourceBrowser.getJobTitlesname(resourceid));
//            userInfo.put("icon", resourceComInfo.getMessagerUrls(resourceid));
//            userInfo.put("type", "resource");
//            userInfo.put("departmentid", resourceComInfo.getDepartmentID(resourceid));
//            userInfo.put("departmentname", deptComInfo.getDepartmentmark(resourceComInfo.getDepartmentID(resourceid)));
//            String subcompanyid = deptComInfo.getSubcompanyid1(resourceComInfo.getDepartmentID(resourceid));
//            String parentsubcompanyid = subCompanyComInfo.getSupsubcomid(subcompanyid);
//            userInfo.put("subcompanyid", subcompanyid);
//            userInfo.put("subcompanyname", subCompanyComInfo.getSubcompanyname(subcompanyid));
//            userInfo.put("supsubcompanyid", parentsubcompanyid);
//            userInfo.put("supsubcompanyname", subCompanyComInfo.getSubcompanyname(parentsubcompanyid));
//            userInfo.put("title", userInfo.get("lastname") + "|" + userInfo.get("departmentname"));
//        }
//        apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, arr);
        return apidatas;
    }

    @Override
    public Map<String, Object> getBrowserConditionInfo(Map<String, Object> params) throws Exception {

        String workflowid = Util.null2String(params.get("bdf_wfid"));
        String nodeid = Util.null2String(params.get("nodeid"));
        String forwardflag = Util.null2String(params.get("forwardflag"));
        String agentorbyagentid = Util.null2String(params.get("agentorbyagentid"));
        String bdShare = Util.null2String(params.get("bdShare")); //微博模块 单独加的条件
        User finalUser = CacheLoadQueueManager.getFinalUser(user, agentorbyagentid);
        user = finalUser;





        HrmOrgForwardBean hrmOrgForwardBean = CacheLoadQueueManager.getForwordBean(workflowid, nodeid, forwardflag, finalUser, false, true);

        Map<String, Object> apidatas = new HashMap<String, Object>();
        List<SearchConditionItem> conditions = new ArrayList<SearchConditionItem>();
        apidatas.put(BrowserConstant.BROWSER_RESULT_CONDITIONS, conditions);
        ConditionFactory conditionFactory = new ConditionFactory(user);
        //姓名
        conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 413, "lastname", true));


        //微博模块 单独条件返回
        if(!"".equals(bdShare)){
            conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 141, "subcompanyid", "194"));
            //部门
            conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 124, "departmentid", "57"));
            return apidatas;
        }


        //组织维度
        CompanyComInfo companyComInfo = new CompanyComInfo();
        CompanyVirtualComInfo companyVirtualComInfo = new CompanyVirtualComInfo();
        List<SearchConditionOption> comOptions = new ArrayList<SearchConditionOption>();
        if (companyComInfo.getCompanyNum() > 0) {
            companyComInfo.setTofirstRow();
            while (companyComInfo.next()) {
                comOptions.add(new SearchConditionOption(companyComInfo.getCompanyid(), companyComInfo.getCompanyname(), true));
            }
        }
        companyVirtualComInfo.setTofirstRow();
        while (companyVirtualComInfo.next()) {
            if ("1".equals(companyVirtualComInfo.getCancel())){
                continue;
            }
            comOptions.add(new SearchConditionOption(companyVirtualComInfo.getCompanyid(), companyVirtualComInfo.getVirtualType()));
        }
        SearchConditionItem searchConditionItem = conditionFactory.createCondition(ConditionType.SELECT, 34069, "virtualtype", comOptions);
        searchConditionItem.setValue(companyComInfo.getCompanyid());
        conditions.add(conditionFactory.createCondition(ConditionType.SELECT, 34069, "virtualtype", comOptions));
        //状态
        //流程状态
        List<SearchConditionOption> statuOptions = new ArrayList<SearchConditionOption>();
        conditions.add(conditionFactory.createCondition(ConditionType.SELECT, 602, "status", statuOptions));
        String conditionType = Util.null2String(params.get("changeType"));   //人员状态变更类型
        if (conditionType.length() == 0) {

            RemindSettings settings = new ChgPasswdReminder().getRemindSettings();
            String checkUnJob = Util.null2String(settings.getCheckUnJob(), "0");//非在职人员信息查看控制 启用后，只有有“离职人员查看”权限的用户才能检索非在职人员
            if ("1".equals(checkUnJob)) {
                if (HrmUserVarify.checkUserRight("hrm:departureView", user)) {
                    statuOptions.add(new SearchConditionOption("9", SystemEnv.getHtmlLabelName(332, user.getLanguage())));
                }
            } else {
                statuOptions.add(new SearchConditionOption("9", SystemEnv.getHtmlLabelName(332, user.getLanguage())));
            }
            statuOptions.add(new SearchConditionOption("8", SystemEnv.getHtmlLabelName(1831, user.getLanguage()), true));
            statuOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, user.getLanguage())));
            statuOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
            statuOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
            statuOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(15844, user.getLanguage())));
            if ("1".equals(checkUnJob)) {
                if (HrmUserVarify.checkUserRight("hrm:departureView", user)) {
                    statuOptions.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(6094, user.getLanguage())));
                    statuOptions.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(6091, user.getLanguage())));
                    statuOptions.add(new SearchConditionOption("6", SystemEnv.getHtmlLabelName(6092, user.getLanguage())));
                    statuOptions.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(2245, user.getLanguage())));
                }
            } else {
                statuOptions.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(6094, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(6091, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("6", SystemEnv.getHtmlLabelName(6092, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(2245, user.getLanguage())));
            }
        } else {
            statuOptions.add(new SearchConditionOption("9", SystemEnv.getHtmlLabelName(332, user.getLanguage()), true));
            if (conditionType.equals("hrmtry")) {
                statuOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
            } else if (conditionType.equals("retire")) {
                statuOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
            } else if (conditionType.equals("rehire")) {
                statuOptions.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(6094, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("5", SystemEnv.getHtmlLabelName(6091, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("6", SystemEnv.getHtmlLabelName(6092, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(2245, user.getLanguage())));
            } else if (conditionType.equals("redeploy") || conditionType.equals("fire") || conditionType.equals("extend")) {
                statuOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(15844, user.getLanguage())));
            } else if (conditionType.equals("hire")) {
                statuOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(15844, user.getLanguage())));
            } else if (conditionType.equals("dismiss")) {
                statuOptions.add(new SearchConditionOption("8", SystemEnv.getHtmlLabelName(1831, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
                statuOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(15844, user.getLanguage())));
            }
        }

        //分部
        conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 141, "subcompanyid", "194"));
        //部门
        conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 124, "departmentid", "57"));
        //岗位
        conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 6086, "jobtitle"));
        //角色
        conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 122, "roleid", "267"));

        List<BrowserTabBean> tabs = new ArrayList<BrowserTabBean>();
        tabs.add(new BrowserTabBean("1", SystemEnv.getHtmlLabelName(24515, user.getLanguage())));
        tabs.add(new BrowserTabBean("2", SystemEnv.getHtmlLabelName(18511, user.getLanguage())));
        tabs.add(new BrowserTabBean("3", SystemEnv.getHtmlLabelName(15089, user.getLanguage())));
        tabs.add(new BrowserTabBean("4", SystemEnv.getHtmlLabelName(18770, user.getLanguage())));
        tabs.add(new BrowserTabBean("5", SystemEnv.getHtmlLabelName(81554, user.getLanguage())));
        //tabs.add(new BrowserTabBean("6", SystemEnv.getHtmlLabelName(1340, user.getLanguage())));

        return apidatas;
    }

    private static final ThreadLocal<HrmOrgForwardBean> localHrmOrgForwardBean = new ThreadLocal();

    @Override
    public Map<String, Object> getBrowserData(Map<String, Object> params) throws Exception {
        this.params = params;
        String cmd = Util.null2String(params.get("cmd"));
        //
        String workflowid = Util.null2String(params.get("bdf_wfid"));
        String workplan = Util.null2String(params.get("workplan"));
        String nodeid = Util.null2String(params.get("nodeid"));
        String forwardflag = Util.null2String(params.get("forwardflag"));
        String agentorbyagentid = Util.null2String(params.get("agentorbyagentid"));
        User finalUser = CacheLoadQueueManager.getFinalUser(user, agentorbyagentid);
        String filterids = Util.null2String(params.get("filterids"));
        String bdShare = Util.null2String(params.get("bdShare")); //微博模块单独条件
        String virtualtype = Util.null2String(params.get("virtualtype"));
        this.rightStr = Util.null2String(params.get("rightStr"));
        String from = Util.null2String(params.get("from"));
        // 公文模块
        String odoc = Util.null2String(params.get("odoc"));
        String classification = Util.null2String(params.get("classification"));

        user = finalUser;

        HrmOrgForwardBean hrmOrgForwardBean = CacheLoadQueueManager.getForwordBean(workflowid, nodeid, forwardflag, finalUser, false);
        localHrmOrgForwardBean.set(hrmOrgForwardBean);
        String sqlwhere = Util.null2String(params.get("sqlwhere"));
        if (hrmOrgForwardBean != null) {
            sqlwhere = HrmWorkflowAdvanceManager.wrapUnqinueSqlwhere(sqlwhere, workflowid, nodeid, forwardflag);
            sqlwhere = HrmWorkflowAdvanceManager.resetSqlWhere(hrmOrgForwardBean, sqlwhere, "hr");
            params.put("sqlwhere", sqlwhere);

        }
        if(!"".equals(filterids)){
            sqlwhere += " hr.id not in ("+filterids+")";
            params.put("sqlwhere", sqlwhere);
        }

        //微博模块 单独条件
        if(!"".equals(bdShare)&&!cmd.equals("resource")){
            cmd = "bdShare";
        }
        //微博模块 单独条件
        if(!"".equals(bdShare)&&cmd.equals("resource")){
            cmd = "bdShareUsers";
        }


        if("email".equals(from)){
            sqlwhere += " hr.email is not null " + (new RecordSet().getDBType().equals("oracle") ? "" : " and hr.email<>'' ");
            params.put("sqlwhere", sqlwhere);
        }

        if(classification.length()>0){//根据资源密集过滤人员
            sqlwhere += " hr.classification in (select seclevel from UserClassification where Optionalresourceseclevel like '%"+classification+"%')";
            params.put("sqlwhere", sqlwhere);
        }

        Map<String, Object> result = null;
        WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
        Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);

        generateRange(dataRanage, virtualtype);

        //加载人力资源浏览按钮显示列定义
        List browserDisplayColumns = loadDisplayColumnConfig();

        if (cmd.equals("newly")) {
            if (Util.null2String(dataRanage.get("sqlWhere")).length() > 0) {
                result = getSearchList(params);
            } else {
                String isDelete = Util.null2String(params.get("isDelete"));
                if (isDelete.equals("1")) {
                    String resourceid = Util.null2String(params.get("resourceid"));
                    RecordSet rs = new RecordSet();
                    String sql = "delete from hrmresourceselectrecord where resourceid=" + user.getUID();
                    if (resourceid.length() > 0) {
                        sql += " and selectid=" + resourceid;
                    }
                    rs.executeUpdate(sql);
                    result = new HashMap<>();
                    result.put("datas", new ArrayList<Object>());
                    result.put("count", 0);
                } else {
                    result = getList(params);
                }
            }
        } else if (cmd.equals("search")) {
            result = getSearchList(params);
        } else if (cmd.equals("role")) {
            result = getRoleList(params);
        } else if (cmd.equals("underling")) {
            result = "".equals(workplan) ? getUnderlingList(params) : getUnderlingList4WorkPlan(params);
        } else if (cmd.equals("branch")) {
            result = "".equals(workplan) ? getbranchList(params) : getbranchList4WorkPlan(params);
        } else if (cmd.equals("addList")) {
            result = addList(params);
        } else if (cmd.equals("companyvirtual")) {
            result = getcompanyvirtual(params);
        } else if (cmd.equals("v2resourcetree")) {
            // 公文流程模块 qc1081999 只返回当前人员所在的分部
            if ("1".equals(odoc) && !user.isAdmin()) {
                dataRanageAllSubCompanyIds = new ArrayList<>();
                ResourceComInfo resourceComInfo = new ResourceComInfo();
                String subCompanyID = resourceComInfo.getSubCompanyID(user.getUID() + "");
                dataRanageAllSubCompanyIds.add(subCompanyID);
            }
            result = getv2resource(params);
        } else if (cmd.equals("v2grouptree")) {
            result = getV2grouptree(params);
        } else if (cmd.equals("status")) {
            result = gethrmstatus(params);
        } else if (cmd.equals("allResource")) {
            result = getAllResourceList(params);
        } else if (cmd.equals("getResourceListByLetter")) {
            result = getResourceListByLetter(params);
        } else if (cmd.equals("resource")) {
            result = getOrgResource(params);
        } else if(cmd.equals("bdShare")){ //微博单独条件
            result = getBlogHrmResource(params);
        } else if(cmd.equals("bdShareUsers")){
            result = getBlogSelectedHrmResource(params);
        }

        if(browserDisplayColumns.size() > 0)//抛出显示列定义
            result.put("columns", browserDisplayColumns);
        if (result.get("datas") != null) {
            result.put("mobileshowtemplate", getMobileShowTemplate());
            //em7人力资源浏览按钮list模板
            result.put("mobiletemplate",getMobileTemplate());
        }
        String mobileResourceId = Util.null2String(params.get("mobileResourceId"));//手机端参数
        if (!mobileResourceId.equals("")) {
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            result.put("lastname", resourceComInfo.getLastname(mobileResourceId));
        }
        localHrmOrgForwardBean.remove();
        return result;
    }

    private Map<String, Object> getList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);

        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String lastname = Util.null2String(params.get("lastname"));
            String min = Util.null2String(params.get("min"));
            String max = Util.null2String(params.get("max"));
            String sqlwhere = Util.null2String(params.get("sqlwhere"));
            String virtualtype = Util.null2String(params.get("virtualtype"));
            String ids = user.getUID() + "";
            String belongtoshow = new HrmUserSettingComInfo().getBelongtoshowByUserId(user.getUID()+ "");
            if (belongtoshow.equals("1") && "0".equals(user.getAccount_type())) {
                String belongtoids = user.getBelongtoids();
                ids = ids + "," + belongtoids;
            }

            // 暂时先写死sql语句，后期拓展
            String newlistsql = "select   distinct  hr.id as id, hrsd.id as dsporder0" + displayFieldsStr + ",dsporder,ROW_NUMBER() OVER(order by hrsd.id desc ,dsporder asc,hr.id asc)rn from " + (!"".equals(virtualtype) && Util.getIntValue(virtualtype) < 0 ? "HrmResourceVirtualView" : "hrmresource") + " hr, HrmResourceSelectRecord  hrsd  where  (status =0 or status = 1 or status = 2 or status = 3)  and loginid is not null  and loginid<>''  and hr.id = selectid and resourceid in (" + ids+")";
            if (DialectUtil.isMySql(rs.getDBType())) {
                newlistsql = "select   distinct  hr.id as id, hrsd.id as dsporder0" + displayFieldsStr + ",dsporder from  " + (!"".equals(virtualtype) && Util.getIntValue(virtualtype) < 0 ? "HrmResourceVirtualView" : "hrmresource") + " hr, HrmResourceSelectRecord  hrsd  where  (status =0 or status = 1 or status = 2 or status = 3)  and loginid is not null  and loginid<>''  and hr.id = selectid and resourceid in (" + ids+")";
            }

            if (!"".equals(lastname)) {
                newlistsql += "and( lastname like '%" + lastname + "%' or pinyinlastname like '%" + lastname +"%' or mobile like '%" + lastname + "%' or workcode like '%" + lastname + "%') ";
            }

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                newlistsql += " " + dataRanageResourceIds;
            }

            if (!"".equals(sqlwhere)) {
                newlistsql += "  and " + sqlwhere;
            }

            if (adci.isUseAppDetach()) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                newlistsql += tempstr;
            }
            newlistsql = getSearchSQL(params, newlistsql, user);

            String orderby = " order by dsporder0 desc, dsporder ";

            List<String> getList = new ArrayList<String>();
            getList.add("id");

            getList.addAll(getDisplayFields());
//            getList.add("lastname");
//            getList.add("pinyinlastname");
//            getList.add("departmentid");
//            getList.add("subcompanyid1");
//            getList.add("workcode");
            List<Map<String, String>> getRList = d.getNewlyList(newlistsql, min, max, orderby, getList);
            getRList = EditGetMap(getRList);
            int count = d.getCount(newlistsql);
            jo.put("status", true);
            if (null != getRList && 0 < getRList.size()) {
                for (int i = 0; i < getRList.size(); i++) {
                    getRList.get(i).put("icon", rci.getMessagerUrls(getRList.get(i).get("id")));
                    getRList.get(i).put("requestParams", rci.getUserIconInfoStr(getRList.get(i).get("id"),user));
                }

                jo.put("datas", getRList);
                jo.put("count", count);
            } else {
                jo.put("datas", new ArrayList<Object>());
                jo.put("count", 0);
            }

//            getRList = EditGetMap(getRList);
        } catch (Exception e) {
            writeLog(e);
        }
        return jo;
    }

    private Map<String, Object> getSearchList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);

        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String virtual = Util.null2String(params.get("virtualtype"));
            String min = Util.null2String(params.get("min"));
            String max = Util.null2String(params.get("max"));
            String sql = Util.null2String(params.get("sqlwhere"));
            if(!"".equals(sql) && sql.startsWith(XssUtil.__RANDOM__)){//防范SQL注入
                sql = new XssUtil().get(sql);
            }

            String ids = user.getUID() + "";
            if ("".equals(ids)) {
                jo = getWrongCode(403, jo);
                return jo;
            }
            String sqlwhere = " where 1=1 ";
            String cmd = Util.null2String(params.get("cmd"));
//            if("newly".equals(cmd)) {
//                sqlwhere += " and exists( select * from HrmResourceSelectRecord hrsd where hrsd.selectid= HR.id)";
//            }
            if (sql.length() != 0) sqlwhere += "  and " + sql;
            String mobileResourceId = Util.null2String(params.get("mobileResourceId"));
            if (!mobileResourceId.equals("")) {
                String alllevel = Util.null2String(params.get("alllevel"));
                ids = mobileResourceId;
                if (alllevel.equals("1")) {
                    // 显示所有下属
                    sqlwhere += " and hr.managerstr like '%," + ids + ",%'";
                } else {
                    sqlwhere += " and hr.managerid = " + ids;
                }
            }

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }

            String sqlfrom = "HrmResource hr ";
            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlfrom = "HrmResourceVirtualView hr ";
            }

            String backfields = " hr.id as id, " + String.join(",", this.getDisplayFields()) + ", dsporder ";

            if("newly".equals(cmd)){
                backfields = " hr.id as id,hrsd.id as dsporder0, " + String.join(",", this.getDisplayFields()) + ", dsporder ";
            }
            String lastsql =null;

            if("newly".equals(cmd)){
                //2020/02/06mwl修复浏览按钮最近页签数据显示不准确的bug
                String sqlfrom2= ",HrmResourceSelectRecord hrsd";
                sqlwhere += " AND hr.id = selectid and hrsd.resourceid="+user.getUID();

                lastsql = "select Distinct "
                        + backfields
                        + ",ROW_NUMBER() OVER(order by hrsd.id desc, dsporder asc,lastname asc)rn from "
                        + sqlfrom +sqlfrom2+ sqlwhere;
            }else{
                lastsql = "select Distinct "
                        + backfields
                        + ",ROW_NUMBER() OVER(order by dsporder asc,lastname asc)rn from "
                        + sqlfrom + sqlwhere;
            }

            lastsql = getSearchSQL(params, lastsql, user);

            String orderby = " order by dsporder ";
            if("newly".equals(cmd)) {
                orderby = " order by dsporder0 desc, dsporder ";
            }
            List<String> getList = new ArrayList<String>();
            getList.add("id");
            getList.addAll(getDisplayFields());
//            getList.add("lastname");
//            getList.add("pinyinlastname");
//            getList.add("jobtitle");
//            getList.add("dsporder");
//            getList.add("workcode");
            List<Map<String, String>> getRList = d.getNewlyList(lastsql, min, max, orderby, getList);
            int count = d.getCount(lastsql);
            jo.put("status", true);
            if (null != getRList && 0 < getRList.size()) {
                getRList = EditGetMap(getRList);
                jo.put("datas", getRList);
                jo.put("count", count);
            } else {
                jo.put("datas", new ArrayList<Object>());
                jo.put("count", 0);
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return jo;
    }

    private Map<String, Object> getRoleList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);

        String rolesname = Util.null2String(params.get("rolesname"));
        String rolesmark = Util.null2String(params.get("rolesmark"));
        String sqlwhere = Util.null2String(params.get("sqlwhere"));

        String ids = user.getUID() + "";
        if ("".equals(ids)) {
            jo = getWrongCode(403, jo);
            return jo;
        }

        int ishead = 0;
        if (!sqlwhere.equals(""))
            ishead = 1;
        if (!rolesname.equals("")) {
            if (ishead == 0) {
                ishead = 1;
                sqlwhere += " where rolesname like '%" + Util.fromScreen2(rolesname, user.getLanguage()) + "%' ";
            } else
                sqlwhere += " and rolesname like '%" + Util.fromScreen2(rolesname, user.getLanguage()) + "%' ";
        }
        if (!rolesmark.equals("")) {
            if (ishead == 0) {
                ishead = 1;
                sqlwhere += " where rolesmark like '%" + Util.fromScreen2(rolesmark, user.getLanguage()) + "%' ";
            } else
                sqlwhere += " and rolesmark like '%" + Util.fromScreen2(rolesmark, user.getLanguage()) + "%' ";
        }

        if (!rolesmark.equals("")) {
            if (ishead == 0) {
                ishead = 1;
                sqlwhere += " where rolesmark like '%" + Util.fromScreen2(rolesmark, user.getLanguage()) + "%' ";
            } else
                sqlwhere += " and rolesmark like '%" + Util.fromScreen2(rolesmark, user.getLanguage()) + "%' ";
        }

        WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
        params.put("tableAlias", "hr");
        Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
        String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
        if (dataRanageResourceIds.length() > 0) {
            if (ishead == 0) {
                ishead = 1;
                sqlwhere += " where 1=1 " + dataRanageResourceIds;
            } else {
                sqlwhere += " " + dataRanageResourceIds;
            }
        }

        // 暂时先写死sql语句，后期拓展
        String pageUid = "0068f8a8-958d-4fef-a62c-ed728cc974c8";
        String tableString = "<table instanceid='BrowseTable' tabletype='none' pageUid =\""
                + pageUid
                + "\">"
                + "<sql backfields=\""
                + "id,rolesmark,rolesname"// backfields
                + "\" sqlform=\""
                + Util.toHtmlForSplitPage("  HrmRoles ")
                + "\" sqlwhere=\""
                + sqlwhere
                + "\"  sqlorderby=\""
                + "rolesmark"
                + "\"  sqlprimarykey=\"id\" sqlsortway=\"Desc\"/>"
                + "<head>"
                + "	<col width=\"0%\" hide=\"true\" text=\"\" column=\"id\"/>"
                + "   <col width=\"30%\"  text=\""
                + "id"
                + "\" orderkey=\"id\" column=\"id\"/>"
                + "   <col width=\"30%\"  text=\""
                + SystemEnv.getHtmlLabelName(15068, user.getLanguage())
                + "\" display=\"true\" orderkey=\"rolesname\" column=\"rolesname\"/>"
                + "   <col width=\"35%\"  text=\""
                + SystemEnv.getHtmlLabelName(85, user.getLanguage())
                + "\" display=\"true\" orderkey=\"rolesmark\" column=\"rolesmark\"/>"
                + "</head>" + "</table>";
        String sessionkey = Util.getEncrypt(Util.getRandom());
        Util_TableMap.setVal(sessionkey, tableString);
        return jo;
    }

    private Map<String, Object> getUnderlingList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceVirtualComInfo rv = new ResourceVirtualComInfo();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String virtual = Util.null2String(params.get("virtualtype"));
            String min = Util.null2String(params.get("min"));
            String max = Util.null2String(params.get("max"));

            String ids = user.getUID() + "";
            if ("".equals(ids)) {
                jo = getWrongCode(403, jo);
                return jo;
            }

            String whereClause = Util.null2String(params.get("sqlwhere"));
            String sqlwhere = " where  1=1 ";
            if (whereClause.length() != 0) sqlwhere += " and " + whereClause + "  ";

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }

            String sqlfrom = "HrmResource hr ";
            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlfrom = "HrmResourceVirtualView hr ";
            }
            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                String departmentid = rv.getDepartmentid(virtual, "" + ids);
                if (null != departmentid && !"".equals(departmentid)) {
                    sqlwhere += " and hr.departmentid = " + departmentid + " and virtualtype=" + virtual;
                } else {
                    sqlwhere += " and hr.departmentid = " + user.getUserDepartment() + " and virtualtype=" + virtual;
                }
            } else {
                sqlwhere += "  and hr.departmentid = '" + user.getUserDepartment() + "'";
            }

            if (adci.isUseAppDetach() && (Util.getIntValue(virtual) >= 0 || Util.getIntValue(virtual) == -10000)) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sqlwhere += tempstr;
            }

            String backfields = " hr.id as id," + String.join(",", this.getDisplayFields()) + ", dsporder ";
            String lastsql = "select "
                    + backfields
                    + ",ROW_NUMBER() OVER(order by dsporder asc,lastname asc)rn from "
                    + sqlfrom + sqlwhere;

            lastsql = getSearchSQL(params, lastsql, user);
            // 拼接插入参数;
            List<String> getList = new ArrayList<String>();
            getList.add("id");
            getList.addAll(getDisplayFields());
//            getList.add("lastname");
//            getList.add("pinyinlastname");
//            getList.add("jobtitle");
//            getList.add("dsporder");
//            getList.add("workcode");
            List<Map<String, String>> getRList = d.getNewlyList(lastsql, min, max, getList);
            int count = d.getCount(lastsql);
            jo.put("status", true);
            if (null != getRList && 0 < getRList.size()) {
                getRList = EditGetMap(getRList);
                jo.put("datas", getRList);
                jo.put("count", count);
            } else {
                jo.put("datas", new ArrayList<Object>());
                jo.put("count", 0);
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return jo;
    }

    private Map<String, Object> getUnderlingList4WorkPlan(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceVirtualComInfo rv = new ResourceVirtualComInfo();
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String virtual = Util.null2String(params.get("virtualtype"));
            String min = Util.null2String(params.get("min"));
            String max = Util.null2String(params.get("max"));

            String ids = user.getUID() + "";
            if ("".equals(ids)) {
                jo = getWrongCode(403, jo);
                return jo;
            }

            String whereClause = Util.null2String(params.get("sqlwhere"));
            String alllevel = Util.null2String(params.get("alllevel"));
            String sqlwhere = " where  1=1 ";
            if (whereClause.length() != 0) sqlwhere += " and " + whereClause + "  ";

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }

            String sqlfrom = "HrmResource hr ";
            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlfrom = "HrmResourceVirtualView hr ";
            }
            if (!"".equals(alllevel) && 1 == Integer.valueOf(alllevel)) {
                ArrayList<String> childDepts = new ArrayList<>();
                childDepts.add(user.getUserDepartment() + "");
                departmentComInfo.getAllChildDeptByDepId(childDepts, user.getUserDepartment() + "");
                sqlwhere += " and " + Util.getSubINClause(String.join(",", childDepts), "hr.departmentid", "IN");
            } else {
                sqlwhere += "  and hr.departmentid = '" + user.getUserDepartment() + "'";
            }

            if (adci.isUseAppDetach() && (Util.getIntValue(virtual) >= 0 || Util.getIntValue(virtual) == -10000)) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sqlwhere += tempstr;
            }

            String backfields = " hr.id as id," + String.join(",", this.getDisplayFields()) + ", dsporder ";
            String lastsql = "select "
                    + backfields
                    + ",ROW_NUMBER() OVER(order by dsporder asc,lastname asc)rn from "
                    + sqlfrom + sqlwhere;

            lastsql = getSearchSQL(params, lastsql, user);
            // 拼接插入参数;
            List<String> getList = new ArrayList<String>();
            getList.add("id");
            getList.addAll(getDisplayFields());
            List<Map<String, String>> getRList = d.getNewlyList(lastsql, min, max, getList);
            int count = d.getCount(lastsql);
            jo.put("status", true);
            if (null != getRList && 0 < getRList.size()) {
                getRList = EditGetMap(getRList);
                jo.put("datas", getRList);
                jo.put("count", count);
            } else {
                jo.put("datas", new ArrayList<Object>());
                jo.put("count", 0);
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return jo;
    }

    private String getSearchSQL(Map<String, Object> params, String lastsql, User user) throws Exception {
        try {
            AppDetachComInfo adci = new AppDetachComInfo();
            RecordSet rs = new RecordSet();

            String lastname = Util.null2String(params.get("lastname"));
            String virtualtype = Util.null2String(params.get("virtualtype"));
            String subcompanyid = Util.null2String(params.get("subcompany"));
            if (subcompanyid.length() == 0) {
                subcompanyid = Util.null2String(params.get("subcompanyid"));
            }
            String departmentid = Util.null2String(params.get("department"));
            if (departmentid.length() == 0) {
                departmentid = Util.null2String(params.get("departmentid"));
            }
            String jobtitle = Util.null2String(params.get("jobtitle"));
            String isNoAccount = Util.null2String(params.get("isNoAccount"));// 是否显示无账号人员
            String roleid = Util.null2String(params.get("role"));// 角色
            if (roleid.length() == 0) {
                roleid = Util.null2String(params.get("roleid"));
            }
            if (!"".equals(lastname)) {
                lastsql += "and( lastname like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or pinyinlastname like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%'" + " or workcode like '%" + lastname + "%' or mobile like '%" + lastname + "%') ";
            }
            // 状态值
            String status = Util.null2String(params.get("status"));
            String fromHrmStatusChange = Util.null2String(params.get("fromHrmStatusChange"));
            if (status.equals("-1"))
                status = "";
            if (!status.equals("") && !status.equals("9") && !status.equals("8")) {
                lastsql += " and status =" + status + " ";
            }
            if (fromHrmStatusChange.equals("")) {
                if (status.equals("") || status.equals("8")) {
                    lastsql += " and (status =0 or status = 1 or status = 2 or status = 3) ";
                }
            }
            // 维度开始
            if (!fromHrmStatusChange.equals("")) {
                if (Util.getIntValue(virtualtype) < -1) {
                    lastsql = lastsql.replace("hr.subcompanyid1", "hr.hrmsubcompanyid1");
                    lastsql = lastsql.replace("hr.departmentid", "hr.hrmdepartmentid");
                }
            }
            if (Util.getIntValue(virtualtype) < -1) {
                lastsql += " and virtualtype=" + virtualtype;
            }

            // 分部
            if (subcompanyid.equals("0"))
                subcompanyid = "";
            if (departmentid.equals("0"))
                departmentid = "";

            if (!departmentid.equals("")) {
                lastsql += " and departmentid in(" + departmentid + ") ";
            }
            if (departmentid.equals("") && !subcompanyid.equals("")) {
                lastsql += " and subcompanyid1 in(" + subcompanyid + ") ";
            }

            // 是否显示全部
            if (!"1".equals(isNoAccount)) {
                lastsql += " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
            }

            //增加应用分权控制
            if (adci.isUseAppDetach()) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                lastsql += tempstr;
            }

            /*
             * 执行力管理分权增加控制,未开启，查询部门（包括下级）人员，分部（包括下级）人员，
             * */
            if(this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet")){
                String appdetawhere = checkBlogAppLoadSub(this.rightStr);
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (appdetawhere) : "");
                lastsql += tempstr;
            }

            //增加广播类型控制
            String radioType = Util.null2String(params.get("radioType"));
            if(HrmPracticalConstant.RADIO_TYPE.equals(radioType)){

                List<String> userIdList = HrmPracticalUtil.getUserIdS(user.getUID(),HrmPracticalConstant.BROAD_CASTER,HrmPracticalConstant.SEND_RANGE);

                if(userIdList != null){
                    lastsql += " and hr.ID in (" + StringUtils.join(userIdList, ",") + ")";
                }
            }


            // 角色
            if (!roleid.equals("")) {
                lastsql += " and    hr.ID in (select t1.ResourceID from hrmrolemembers t1,hrmroles t2 where t1.roleid = t2.ID and t2.ID=" + roleid + " ) ";
            }
            // 岗位
            if (!jobtitle.equals("")) {
                lastsql += " and jobtitle in(select id from HrmJobTitles where jobtitlename like '%" + Util.fromScreen2(jobtitle, user.getLanguage()) + "%') ";
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return lastsql;
    }

    private Map<String, Object> getbranchList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        try {
            AppDetachComInfo adci = new AppDetachComInfo();
            RecordSet rs = new RecordSet();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }
            String virtual = Util.null2String(params.get("virtualtype"));
            String min = Util.null2String(params.get("min"));
            String max = Util.null2String(params.get("max"));
            String lastname = Util.null2String(params.get("lastname"));
            String isNoAccount = Util.null2String(params.get("isNoAccount"));// 是否显示无账号人员
            String alllevel = Util.null2String(params.get("alllevel"));// 是否显示子成员
            String ids = user.getUID() + "";
            String mobileResourceId = Util.null2String(params.get("mobileResourceId"));
            if (!mobileResourceId.equals("")) {
                ids = mobileResourceId;
            }
            if ("".equals(ids)) {
                jo = getWrongCode(403, jo);
                return jo;
            }

            String whereClause = Util.null2String(params.get("sqlwhere"));
            String sqlwhere = " where  1=1 ";
            if (whereClause.length() != 0) sqlwhere += " and " + whereClause + "  ";
            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }
            String sqlfrom = "HrmResource hr ";
            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlfrom = "HrmResourceVirtualView hr ";
            }
            // if (!"".equals(lastname)) {
            //     sqlwhere += "and( lastname like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or pinyinlastname like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or mobile like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or workcode like '%" + Util.fromScreen2(lastname, user.getLanguage())+ "%') ";
            // }
            // sqlwhere += " and (status =0 or status = 1 or status = 2 or status = 3) ";
            if (!isNoAccount.equals("1")) {
                sqlwhere += " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
            }

            //增加应用分权控制
            if (adci.isUseAppDetach() && (Util.getIntValue(virtual) >= 0 || Util.getIntValue(virtual) == -10000)) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sqlwhere += tempstr;
            }

            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlwhere += " and virtualtype=" + virtual;

            }

            if (alllevel.equals("1")) {
                // 显示所有下属
                sqlwhere += " and (hr.managerstr like '%," + ids + ",%'";
            } else {
                sqlwhere += " and (hr.managerid = " + ids + " ";
            }
            //qczj 显示当前分部和所有下级分部
            PermissionUtil permissionUtil = new PermissionUtil();
            String departmentChilds = permissionUtil.getDepartmentChilds(user);
            new BaseBean().writeLog("==zj==(subcompanyChilds)" + JSON.toJSONString(departmentChilds));
            if (!"".equals(departmentChilds)){
                sqlwhere +=  " or hr.subcompanyid1 in ("+departmentChilds+") )";
            }else {
                sqlwhere += " ) ";
            }

            String backfields = " hr.id as id," + String.join(",", this.getDisplayFields()) + ", dsporder";

            String lastsql = "select "
                    + backfields
                    + ",ROW_NUMBER() OVER(order by dsporder asc,lastname asc)rn from "
                    + sqlfrom + sqlwhere;
            lastsql = getSearchSQL(params, lastsql, user);

            new BaseBean().writeLog("==zj==(lastsql)" + JSON.toJSONString(lastsql));
            // 拼接插入参数;
            List<String> getList = new ArrayList<String>();
            getList.add("id");
            getList.addAll(getDisplayFields());
//            getList.add("lastname");
//            getList.add("pinyinlastname");
//            getList.add("jobtitle");
//            getList.add("dsporder");
//            getList.add("workcode");
            List<Map<String, String>> getRList = d.getNewlyList(lastsql, min, max, getList);
            int count = d.getCount(lastsql);
            jo.put("status", true);
            if (null != getRList && 0 < getRList.size()) {
                getRList = EditGetMap(getRList);
                jo.put("datas", getRList);
                jo.put("count", count);
            } else {

                jo.put("datas", new ArrayList<Object>());
                jo.put("count", 0);
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return jo;
    }

    private Map<String, Object> getbranchList4WorkPlan(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        try {
            AppDetachComInfo adci = new AppDetachComInfo();
            RecordSet rs = new RecordSet();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }
            String virtual = Util.null2String(params.get("virtualtype"));
            String min = Util.null2String(params.get("min"));
            String max = Util.null2String(params.get("max"));
            String lastname = Util.null2String(params.get("lastname"));
            String isNoAccount = Util.null2String(params.get("isNoAccount"));// 是否显示无账号人员
            String alllevel = Util.null2String(params.get("alllevel"));// 是否显示子成员
            String ids = user.getUID() + "";
            String mobileResourceId = Util.null2String(params.get("mobileResourceId"));
            if (!mobileResourceId.equals("")) {
                ids = mobileResourceId;
            }
            if ("".equals(ids)) {
                jo = getWrongCode(403, jo);
                return jo;
            }

            String whereClause = Util.null2String(params.get("sqlwhere"));
            String sqlwhere = " where  1=1 ";
            if (whereClause.length() != 0) sqlwhere += " and " + whereClause + "  ";
            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }
            String sqlfrom = "HrmResource hr ";
            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlfrom = "HrmResourceVirtualView hr ";
            }
            // if (!"".equals(lastname)) {
            //     sqlwhere += "and( lastname like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or pinyinlastname like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or mobile like '%" + Util.fromScreen2(lastname, user.getLanguage()) + "%' or workcode like '%" + Util.fromScreen2(lastname, user.getLanguage())+ "%') ";
            // }
            // sqlwhere += " and (status =0 or status = 1 or status = 2 or status = 3) ";
            if (!isNoAccount.equals("1")) {
                sqlwhere += " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
            }

            //增加应用分权控制
            if (adci.isUseAppDetach() && (Util.getIntValue(virtual) >= 0 || Util.getIntValue(virtual) == -10000)) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sqlwhere += tempstr;
            }

            if (!"".equals(virtual) && -1 > Integer.valueOf(virtual)) {
                sqlwhere += " and virtualtype=" + virtual;

            }

            if (alllevel.equals("1")) {
                // 显示所有下属
                sqlwhere += " and hr.managerstr like '%," + ids + ",%'";
            } else {
                sqlwhere += " and hr.managerid = " + ids + " ";
            }


            String backfields = " hr.id as id," + String.join(",", this.getDisplayFields()) + ", dsporder";

            String lastsql = "select "
                    + backfields
                    + ",ROW_NUMBER() OVER(order by dsporder asc,lastname asc)rn from "
                    + sqlfrom + sqlwhere;
            lastsql = getSearchSQL(params, lastsql, user);

            // 拼接插入参数;
            List<String> getList = new ArrayList<String>();
            getList.add("id");
            getList.addAll(getDisplayFields());
//            getList.add("lastname");
//            getList.add("pinyinlastname");
//            getList.add("jobtitle");
//            getList.add("dsporder");
//            getList.add("workcode");
            List<Map<String, String>> getRList = d.getNewlyList(lastsql, min, max, getList);
            int count = d.getCount(lastsql);
            jo.put("status", true);
            if (null != getRList && 0 < getRList.size()) {
                getRList = EditGetMap(getRList);
                jo.put("datas", getRList);
                jo.put("count", count);
            } else {

                jo.put("datas", new ArrayList<Object>());
                jo.put("count", 0);
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return jo;
    }

    private List<Map<String, String>> EditGetMap(List<Map<String, String>> getRList) {
        try {
            PrivacyComInfo pc = new PrivacyComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();

            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            SubCompanyComInfo scc = new SubCompanyComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();
            AccountType accountType = new AccountType();
            JobActivitiesComInfo jobActivitiesComInfo = new JobActivitiesComInfo();
            HrmTransMethod hrmTransMethod = new HrmTransMethod();
            JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
            EncryptFieldViewScopeConfigComInfo encryptFieldViewScopeConfigComInfo = new EncryptFieldViewScopeConfigComInfo();
            if (null != getRList && 0 < getRList.size()) {
                for (int i = 0; i < getRList.size(); i++) {
                    String userid = getRList.get(i).get("id");
                    String departmentid = rci.getDepartmentID(userid);
                    String subcomid = rsDepartment.getSubcompanyid1(departmentid);
                    String supsubcomid = scc.getSupsubcomid(subcomid);
                    getRList.get(i).put("img", rci.getMessagerUrls(userid));
                    getRList.get(i).put("messagerurl", rci.getMessagerUrls(userid));
                    getRList.get(i).put("requestParams", rci.getUserIconInfoStr(userid,user));
                    getRList.get(i).put("lastname", Util.formatMultiLang(getRList.get(i).get("lastname")));
                    getRList.get(i).put("pinyinlastname", Util.formatMultiLang(getRList.get(i).get("pinyinlastname")));
                    getRList.get(i).put("jobtitlename", mrb.getJobTitlesname(userid));
                    getRList.get(i).put("departmentid", departmentid);
                    getRList.get(i).put("departmentname", rsDepartment.getDepartmentmark(departmentid));
                    getRList.get(i).put("subcompanyid", subcomid);
                    getRList.get(i).put("subcompanyname", scc.getSubCompanyname(subcomid));
                    getRList.get(i).put("supsubcompanyid", supsubcomid);
                    getRList.get(i).put("supsubcompanyname", scc.getSubCompanyname(supsubcomid));
                    getRList.get(i).put("email", rci.getEmail(userid));
                    for(Map m : browserFieldConfig){
                        String fieldName = Util.null2String(m.get("fieldName"));
                        String fullPath = Util.null2String(m.get("fullPath"));
                        String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                        if(fullPathDelimiter.equals(""))
                            fullPathDelimiter = "|";
                        else
                            fullPathDelimiter = "" + fullPathDelimiter + "";
                        String orderType = Util.null2String(m.get("orderType"));
                        String key = fieldName + "span";
                        switch(fieldName){
                            case "departmentid":
                                String fullDeptPath = rsDepartment.getDepartmentRealPath(departmentid, fullPathDelimiter, orderType);
                                getRList.get(i).put(key, fullPath.equals("1") ? fullDeptPath : rsDepartment.getDepartmentmark(departmentid));
                                break;
                            case "subcompanyid1":
                                getRList.get(i).put(key, fullPath.equals("1") ? scc.getSubcompanyRealPath(subcomid, fullPathDelimiter, orderType) : scc.getSubCompanyname(subcomid));
                                break;
                            case "accounttype":
                                getRList.get(i).put(key, accountType.getAccountType(rci.getAccountType(userid), user.getLanguage() + ""));
                                break;
                            case "sex":
                                getRList.get(i).put(key, rci.getSexName(Util.null2String(getRList.get(i).get("sex")),""+user.getLanguage()));
                                break;
                            case "managerid":
                                getRList.get(i).put(key, rci.getResourcename(rci.getManagerID(userid)));
                                break;
                            case "assistantid":
                                getRList.get(i).put(key, rci.getResourcename(getRList.get(i).get("assistantid")));
                                break;
                            case "jobtitle":
                                getRList.get(i).put(key, mrb.getJobTitlesname(userid));
                                break;
                            case "status":
                                getRList.get(i).put(key, ResourceComInfo.getStatusName(Util.null2String(getRList.get(i).get("status")), user.getLanguage()+""));
                                break;
                            case "jobactivity":
                                getRList.get(i).put(key, Util.null2String(jobActivitiesComInfo.getJobActivitiesname(jobTitlesComInfo.getJobactivityid(rci.getJobTitle(userid)))));
                                break;
                            case "orgid":
                                getRList.get(i).put(key, rsDepartment.getAllParentDepartmentBlankNames(departmentid, subcomid, "|"));
                                break;
                            case "jobcall":
                                getRList.get(i).put(key, Util.null2String(new JobCallComInfo().getJobCallname(rci.getJobCall(userid))));
                                break;
                            case "jobGroupId":
                                String jobtitleid = rci.getJobTitle(userid) ;
                                String jobActivityId = new JobTitlesComInfo().getJobactivityid(jobtitleid);
                                String groupId = jobActivitiesComInfo.getJobgroupid(jobActivityId);
                                getRList.get(i).put(key, Util.null2String(new JobGroupsComInfo().getJobGroupsname(groupId)));
                                break;
                            case "joblevel":
                                getRList.get(i).put(key, Util.null2String(rci.getJoblevel(userid)));
                                break;
                            case "jobactivitydesc":
                                getRList.get(i).put(key, Util.null2String(rci.getJobActivityDesc(userid)));
                                break;
                            case "workroom":
                                getRList.get(i).put(key, Util.null2String(rci.getWorkroom(userid)));
                                break;
                            default:
                                String value = "";
                                switch(fieldName){
                                    case "loginid":
                                        value = rci.getLoginID(userid);
                                        break;
                                    case "workcode":
                                        value = rci.getWorkcode(userid);
                                        break;
                                    case "lastname":
                                        value = rci.getLastname(userid);
                                        break;
                                    case "locationid":
                                        value = rci.getLocationid(userid);
                                        break;
                                    case "mobile":
                                        value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","mobile")? EncryptConfigBiz.getDecryptData(rci.getMobile(userid)):rci.getMobile(userid);
                                        break;
                                    case "telephone":
                                        value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","telephone")? EncryptConfigBiz.getDecryptData(rci.getTelephone(userid)):rci.getTelephone(userid);
                                        break;
                                    case "mobilecall":
                                        value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","mobilecall")? EncryptConfigBiz.getDecryptData(rci.getMobileCall(userid)):rci.getMobileCall(userid);
                                        break;
                                    case "fax":
                                        value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","fax")? EncryptConfigBiz.getDecryptData(rci.getFax(userid)):rci.getFax(userid);
                                        break;
                                    case "email":
                                        value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","email")? EncryptConfigBiz.getDecryptData(rci.getEmail(userid)):rci.getEmail(userid);
                                        break;
                                    case "managerid":
                                        value = rci.getManagerID(userid);
                                        break;
                                    case "assistantid":
                                        value = rci.getAssistantID(userid);
                                        break;
                                }
                                if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                                    getRList.get(i).put(key, pc.getSearchContent(Util.null2String(value), (userid + "+" + user.getUID() + "+" + fieldName)));
                                } else {
                                    getRList.get(i).put(key, hrmTransMethod.getDefineContent(Util.null2String(value), fieldName + ":" + user.getLanguage()));
                                }
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return getRList;
    }

    private Map<String, Object> addList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);

        try {
            AppDetachComInfo adci = new AppDetachComInfo();
            RecordSet rs = new RecordSet();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String ids = Util.null2String(params.get("ids"));
            String id = Util.null2String(params.get("id"));
            if ("".equals(id)) {
                int ida = user.getUID();
                if (0 == ida) {
                    jo = getWrongCode(402, jo);
                    return jo;
                }
                id = String.valueOf(ida);
            }
            if ("".equals(ids)) {
                jo = getWrongCode(403, jo);
            } else {
                String[] idslist = ids.split(",");
                Map<String, String> getMap = new HashMap<String, String>();
                getMap.put("resourceid", id);

                for (int i = 0; i < idslist.length; i++) {
                    getMap.put("selectid", idslist[i]);
                    if (i > 20)
                        break;
                    d.deleteObjectSql("HrmResourceSelectRecord", getMap);
                    d.insertObjectSql("HrmResourceSelectRecord", getMap);
                }
                d.deleteRecord(id);
                jo.put("status", true);
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return jo;
    }

    private Map<String, Object> getcompanyvirtual(Map<String, Object> params) {
        JSONObject jo = new JSONObject();
        jo.put("status", false);
        try {
            String workplan = Util.null2String(params.get("workplan"));
            String type = Util.null2String(params.get("type"));

            if("".equals(workplan) || ("1".equals(workplan) && "v2resourcetree".equals(type))) {
                AppDetachComInfo adci = new AppDetachComInfo();
                RecordSet rs = new RecordSet();
                Dao_Hrm4Ec d = null;
                if (!"oracle".equals(rs.getDBType())) {
                    d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
                } else {
                    d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
                }
                HrmComVirtualBean hb = new HrmComVirtualBean();
                hb.setId(1);
                hb.setShoworder("0");
                int language = 7;
                if (user != null) {
                    language = user.getLanguage();
                }
                hb.setVirtualdesc(SystemEnv.getHtmlLabelName(83179, language));
                hb.setVirtualtype(SystemEnv.getHtmlLabelName(83179, language));
                List<HrmComVirtualBean> getList = new ArrayList<HrmComVirtualBean>();
                getList.add(hb);
                getList.addAll(d.gethrmcompanyvirtualList());
                jo.put("status", true);
                jo.put("datas", getList);
            }else{
                if(!"".equals(type))
                    jo.put("status", true);
                List<HrmComVirtualBean> list = new ArrayList<HrmComVirtualBean>();
                HrmComVirtualBean hb = null;
                switch (type){
                    case "underling":
                        hb = new HrmComVirtualBean();
                        hb.setId(0);
                        hb.setShoworder("0");
                        hb.setVirtualdesc(SystemEnv.getHtmlLabelName(21837, user.getLanguage()));
                        hb.setVirtualtype(SystemEnv.getHtmlLabelName(21837, user.getLanguage()));
                        list.add(hb);

                        hb = new HrmComVirtualBean();
                        hb.setId(1);
                        hb.setShoworder("1");
                        hb.setVirtualdesc(SystemEnv.getHtmlLabelName(522006, user.getLanguage()));
                        hb.setVirtualtype(SystemEnv.getHtmlLabelName(522006, user.getLanguage()));
                        list.add(hb);
                        break;
                    case "branch":
                        hb = new HrmComVirtualBean();
                        hb.setId(0);
                        hb.setShoworder("0");
                        hb.setVirtualdesc(SystemEnv.getHtmlLabelName(81863, user.getLanguage()));
                        hb.setVirtualtype(SystemEnv.getHtmlLabelName(81863, user.getLanguage()));
                        list.add(hb);

                        hb = new HrmComVirtualBean();
                        hb.setId(1);
                        hb.setShoworder("1");
                        hb.setVirtualdesc(SystemEnv.getHtmlLabelName(17494, user.getLanguage()));
                        hb.setVirtualtype(SystemEnv.getHtmlLabelName(17494, user.getLanguage()));
                        list.add(hb);
                        break;
                }
                jo.put("datas", list);
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return jo;

    }

    private Map<String, Object> getv2resource(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);

        HrmOrgForwardBean hrmOrgForwardBean = localHrmOrgForwardBean.get();
        if (hrmOrgForwardBean != null) hrmOrgForwardBean.checkInited(true);

        try {
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            CompanyComInfo cci = new CompanyComInfo();
            CompanyVirtualComInfo cvci = new CompanyVirtualComInfo();
            SubCompanyVirtualComInfo scvc = new SubCompanyVirtualComInfo();

            if (null == user) {
                jo = getWrongCode(402, jo);
                return jo;
            }
            String id = Util.null2String(params.get("id"));
            String isNoAccount = Util.null2String(params.get("isNoAccount"));
            String sqlwhere = Util.null2String(params.get("sqlwhere"));
            String selectedids = Util.null2String(params.get("selectedids"));
            String virtualtype = Util.null2String(params.get("virtualtype"));
            String type = Util.null2String(params.get("type"));
            String alllevel = Util.null2String(params.get("alllevel"));
            String cmd = Util.null2String(params.get("cmd"));

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }

            List<String> selectList = new ArrayList<String>();
            if (selectedids.length() > 0) {
                String[] tmp_selectedids = selectedids.split(",");
                for (String selectedid : tmp_selectedids) {
                    selectList.add(selectedid);
                }
            }
            if(this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet")){
                this.getSubCompanyTreeListByRight(user.getUID(),this.rightStr);//加载分权数据
            }
            //todo
            String resultString = "";
            TreeNode envelope = new TreeNode();
            if (cmd.equals("getNum")) {
                String nodeids = Util.null2String(params.get("nodeids"));
                resultString = getResourceNumJson(nodeids, selectList, sqlwhere);
                jo.put("status", true);
                jo.put("datas", resultString);
            } else {
                if (id.equals("")) {
                    // 初始化
                    String companyname = "";
                    TreeNode root = new TreeNode();
                    if(Util.getIntValue(virtualtype, 1) >= 0)
                        companyname = cci.getCompanyname("1");
                    else
                        companyname = cvci.getVirtualType(virtualtype);
                    root.setLastname(companyname);
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        root.setId(virtualtype);
                        root.setNodeid("com_" + virtualtype + "x");
                    } else {
                        root.setNodeid("com_" + 1 + "x");
                        root.setId("0");
                    }

                    root.setOpen("true");
                    root.setTarget("_self");
                    root.setIcon("/images/treeimages/global_wev8.gif");
                    root.setType("com");
                    getV2SubCompanyTreeList(root, "0", virtualtype, selectList, isNoAccount, user, sqlwhere);

                    jo.put("status", true);
                    jo.put("datas", root);
                } else if (type.equals("com") || type.equals("subcom")) {
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        virtualtype = scvc.getCompanyid(id);
                    }

                    getV2SubCompanyTreeList(envelope, id, virtualtype, selectList, isNoAccount, user, sqlwhere);
                    ArrayList<TreeNode> lsChild = envelope.getChildren();

                    jo.put("status", true);
                    jo.put("datas", lsChild);
                } else if (type.equals("dept")) {
                    String subId = "";
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        DepartmentVirtualComInfo DepartmentComInfo = new DepartmentVirtualComInfo();
                        subId = DepartmentComInfo.getSubcompanyid1(id);
                    } else {
                        DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
                        subId = DepartmentComInfo.getSubcompanyid1(id);
                    }

                    getV2DepartTreeList(envelope, subId, id, selectList, isNoAccount, user, sqlwhere, virtualtype);
                    ArrayList<TreeNode> lsChild = envelope.getChildren();
                    /**
                     * 浏览按钮自定义显示字段
                     */
                    List<String> ids = new ArrayList();
                    for(TreeNode tn : lsChild){
                        if(tn.getType().equals("resource"))
                            ids.add(tn.getId());
                    }
                    Map<String, Map<String, Object>> map = new HashMap();
                    if(ids.size() > 0){
                        for(String resourceId : ids){
                            Map<String, Object> userInfo = getUserInfo(resourceId, user, false);
                            map.put(resourceId, userInfo);
                        }
                    }
                    JSONArray jsonArray = (JSONArray)JSONArray.toJSON(lsChild);
                    for(int i = 0; i < jsonArray.size(); i++){
                        JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                        String k = jsonObject.getString("id");
                        String loopType = jsonObject.getString("type") ;
                        if(map.containsKey(k) && "resource".equals(loopType)) {
                            jsonObject.putAll(map.get(k));
//                            jsonObject.put("columnControled", true);
                        }
                    }
                    jo.put("status", true);
                    jo.put("datas", jsonArray);
                }
            }

        } catch (Exception e) {
            writeLog(e);
        }
        return jo;
    }

    private TreeNode getV2DepartTreeList(TreeNode departTreeList, String subId, String departmentId, List selectedids, String isNoAccount, User user, String sqlwhere, String vituraltype) throws Exception {
        AppDetachComInfo adci = new AppDetachComInfo(user);

        if (!"".equals(vituraltype) && !"1".equals(vituraltype)) {
            if (Util.getIntValue(departmentId) < 0) {
                getResourceTreeListV2(departTreeList, departmentId, selectedids, isNoAccount, user, sqlwhere, vituraltype);
            }

            DepartmentVirtualComInfo rsDepartment = new DepartmentVirtualComInfo();
            rsDepartment.setTofirstRow();
            while (rsDepartment.next()) {
                if (departmentId.equals(rsDepartment.getDepartmentid())) {
                    continue;
                }
                String supdepid = rsDepartment.getDepartmentsupdepid();
                if (departmentId.equals("0") && supdepid.equals("")) {
                    supdepid = "0";
                }
                if (!(rsDepartment.getSubcompanyid1().equals(subId) && (supdepid.equals(departmentId)
                        || (!rsDepartment.getSubcompanyid1(supdepid).equals(subId) && departmentId.equals("0"))))) {
                    continue;
                }
                String id = rsDepartment.getDepartmentid();

                if (this.dataRanageAllDepartmentIds != null && !this.dataRanageAllDepartmentIds.isEmpty()) {
                    if (!this.dataRanageAllDepartmentIds.contains(id)) {
                        continue;
                    }
                }

                //if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initDepartment(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                //    continue;
                //}


                String name = Util.formatMultiLang(rsDepartment.getDepartmentmark());
                String canceled = rsDepartment.getDeparmentcanceled();

                if (adci.isUseAppDetach()) {
                    if ((Util.getIntValue(vituraltype) >= 0 || Util.getIntValue(vituraltype) == -10000) && adci.checkUserAppDetach(id, "3") == 0) {
                        continue;
                    }
                }

                TreeNode departmentNode = new TreeNode();
                departmentNode.setLastname(name);
                departmentNode.setNocheck("Y");
                departmentNode.setId(id);
                departmentNode.setNodeid("dept_" + id + "x");
                departmentNode.setType("dept");
                departmentNode.setIcon("/images/treeimages/subCopany_Colse_wev8.gif");
                if (hasChild("dept", id)) {
                    departmentNode.setIsParent("true");
                }
                if (!"1".equals(canceled)) {
                    departTreeList.AddChildren(departmentNode);
                }
            }
        } else {
            if (departmentId.length() > 0) {
                getResourceTreeListV2(departTreeList, departmentId, selectedids, isNoAccount, user, sqlwhere, "");
            }
            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            rsDepartment.setTofirstRow();
            while (rsDepartment.next()) {
                String id = rsDepartment.getDepartmentid();

                if (this.dataRanageAllDepartmentIds != null && !this.dataRanageAllDepartmentIds.isEmpty()) {
                    if (!this.dataRanageAllDepartmentIds.contains(id)) {
                        continue;
                    }
                }
                if((this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet"))&&!this.checkBlogAppSetting("dept",this.rightStr,id)){
                    continue;
                }

                if (departmentId.equals(rsDepartment.getDepartmentid())) {
                    continue;
                }
                String supdepid = rsDepartment.getDepartmentsupdepid();

                String name = Util.formatMultiLang(rsDepartment.getDepartmentmark());
                String canceled = rsDepartment.getDeparmentcanceled();
                if (departmentId.equals("0") && supdepid.equals("")) {
                    supdepid = "0";
                }
                if (!(rsDepartment.getSubcompanyid1().equals(subId) && (supdepid.equals(departmentId)
                        || (!rsDepartment.getSubcompanyid1(supdepid).equals(subId) && departmentId.equals("0"))))) {
                    continue;
                }

                if (adci.isUseAppDetach()) {
                    if ((Util.getIntValue(vituraltype) >= 0 ) && adci.checkUserAppDetach(id, "3") == 0) {
                        continue;
                    }
                }

                if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initDepartment(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                    continue;
                }

                TreeNode departmentNode = new TreeNode();
                departmentNode.setLastname(name);

                departmentNode.setNocheck("Y");
                departmentNode.setId(id);
                departmentNode.setNodeid("dept_" + id + "x");
                departmentNode.setType("dept");
                departmentNode.setIcon("/images/treeimages/subCopany_Colse_wev8.gif");
                if (hasChild("dept", id)) {
                    departmentNode.setIsParent("true");
                }
                if (!"1".equals(canceled)) {
                    departTreeList.AddChildren(departmentNode);
                }
            }
        }

        return departTreeList;
    }

    private TreeNode getResourceTreeListV2(TreeNode resourceTreeList, String departmentId, List selectedids, String isNoAccount, User user, String sqlwhere, String vituraltype) throws Exception {
        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();
            SubCompanyComInfo scc = new SubCompanyComInfo();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            rsDepartment.setTofirstRow();
            if (!"".equals(vituraltype)) {
                String sql = " select hr.id, hr.loginid, hr.account, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle,hr.workcode, hr.mobile"
                        + " from hrmresource hr " + "where 1=1 ";
                if (sqlwhere.length() > 0) {
                    if (sqlwhere.trim().startsWith("and")) {
                        sqlwhere = " " + sqlwhere;
                    } else {
                        sqlwhere = " and " + sqlwhere;
                    }
                }

                sqlwhere += " and hr.status in (0,1,2,3)";
                if (!isNoAccount.equals("1")) {
                    sqlwhere += " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
                }
                if (adci.isUseAppDetach()) {
                    String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                    String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                    sqlwhere += tempstr;
                }
                if (sqlwhere.length() > 0)
                    sql += sqlwhere;
                sql += "and exists (select * from hrmresourcevirtual where hr.id = resourceid and departmentid="
                        + departmentId + ") order by hr.dsporder, hr.id ";

                resourceTreeList = d.getHrmResourceV2(resourceTreeList, sql, isNoAccount, selectedids);
                if (null != resourceTreeList) {
                    int j = resourceTreeList.getChildren().size();
                    for (int i = 0; i < j; i++) {
                        String id = resourceTreeList.getChildren().get(i).getId();
                        String subcomid = rsDepartment.getSubcompanyid1(rci.getDepartmentID(id));
                        String supsubcomid = scc.getSupsubcomid(subcomid);
                        resourceTreeList.getChildren().get(i).setDepartmentname(rsDepartment.getDepartmentmark(rci.getDepartmentID(id)));
                        resourceTreeList.getChildren().get(i).setSubcompanyname(scc.getSubCompanyname(subcomid));
                        resourceTreeList.getChildren().get(i).setSupsubcompanyname(scc.getSubCompanyname(supsubcomid));
                        resourceTreeList.getChildren().get(i).setIcon(rci.getMessagerUrls(id));
                        resourceTreeList.getChildren().get(i).setRequestParams(rci.getUserIconInfoStr(id,user));
                        resourceTreeList.getChildren().get(i).setJobtitlename(mrb.getJobTitlesname(id));
                    }
                }
            } else {
                String sql = "select hr.id, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle, loginid, account ,hr.workcode,hr.mobile,hr.departmentid"
                        + " from hrmresource hr, hrmdepartment t2 "
                        + " where hr.departmentid=t2.id and t2.id=" + departmentId;
                if (sqlwhere.length() > 0) {
                    if(sqlwhere.trim().startsWith("and id in"))
                        sqlwhere = sqlwhere.replaceFirst("id in", "hr.id in");
                    if (sqlwhere.trim().startsWith("and")) {
                        sqlwhere = " " + sqlwhere;
                    } else {
                        sqlwhere = " and " + sqlwhere;
                    }
                }
                sqlwhere += " and hr.status in (0,1,2,3)";
                if (!isNoAccount.equals("1")) {
                    sqlwhere += " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
                }
                if (adci.isUseAppDetach()) {
                    String appdetawhere;
                    appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");

                    String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                    sqlwhere += tempstr;
                }
                if (sqlwhere.length() > 0)
                    sql += sqlwhere;
                sql += " order by hr.dsporder ";
                resourceTreeList = d.getHrmResourceV2(resourceTreeList, sql, isNoAccount, selectedids);
                if (null != resourceTreeList) {
                    int j = resourceTreeList.getChildren().size();
                    for (int i = 0; i < j; i++) {
                        String id = resourceTreeList.getChildren().get(i).getId();
                        String subcomid = rsDepartment.getSubcompanyid1(rci.getDepartmentID(id));
                        String supsubcomid = scc.getSupsubcomid(subcomid);
                        resourceTreeList.getChildren().get(i).setDepartmentname(rsDepartment.getDepartmentmark(rci.getDepartmentID(id)));
                        resourceTreeList.getChildren().get(i).setSubcompanyname(scc.getSubCompanyname(subcomid));
                        resourceTreeList.getChildren().get(i).setSupsubcompanyname(scc.getSubCompanyname(supsubcomid));
                        resourceTreeList.getChildren().get(i).setIcon(rci.getMessagerUrls(id));
                        resourceTreeList.getChildren().get(i).setRequestParams(rci.getUserIconInfoStr(id,user));
                        resourceTreeList.getChildren().get(i).setJobtitlename(mrb.getJobTitlesname(id));
                    }
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return resourceTreeList;
    }

    private TreeNode getV2SubCompanyTreeList(TreeNode companyTreeList, String subId, String virtualtype, List selectedids, String isNoAccount, User user, String sqlwhere) throws Exception {
        getV2DepartTreeList(companyTreeList, subId, "0", selectedids, isNoAccount, user, sqlwhere, virtualtype);
        AppDetachComInfo adci = new AppDetachComInfo(user);

        if (null != virtualtype && !"".equals(virtualtype) && !"1".equals(virtualtype)) {
            SubCompanyVirtualComInfo scvc = new SubCompanyVirtualComInfo();
            scvc.setTofirstRow();
            while (scvc.next()) {
                String id = scvc.getSubCompanyid();

                if (this.dataRanageAllSubCompanyIds != null && !this.dataRanageAllSubCompanyIds.isEmpty()) {
                    if (!this.dataRanageAllSubCompanyIds.contains(id)) {
                        continue;
                    }
                }

                String supsubcomid = scvc.getSupsubcomid();
                String tmp_virtualtype = scvc.getCompanyid();
                if (null != virtualtype && !"".equals(virtualtype) && !"1".equals(virtualtype)) {
                    if (!virtualtype.equals(tmp_virtualtype)) continue;
                }

                if (supsubcomid.equals(""))
                    supsubcomid = "0";
                if (!supsubcomid.equals(subId))
                    continue;

                if (adci.isUseAppDetach()) {
                    if ((Util.getIntValue(virtualtype) >= 0 || Util.getIntValue(virtualtype) == -10000) && adci.checkUserAppDetach(id, "2") == 0) {
                        continue;
                    }
                }

                //if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initSubCompany(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                //    writeLog(">>>>>>>");
				//	continue;
                //}

                String name = Util.formatMultiLang(scvc.getSubCompanyname());
                String canceled = scvc.getCompanyiscanceled();

                TreeNode subCompanyNode = new TreeNode();
                subCompanyNode.setLastname(name);
                subCompanyNode.setId(id);
                subCompanyNode.setNodeid("subcom_" + id + "x");
                subCompanyNode.setPid(subId);
                subCompanyNode.setIcon("/images/treeimages/Home_wev8.gif");
                subCompanyNode.setNocheck("N");
                subCompanyNode.setType("subcom");
                if (hasChild("subcompany", id)) {
                    subCompanyNode.setIsParent("true");
                }

                if (!"1".equals(canceled)) {
                    companyTreeList.AddChildren(subCompanyNode);
                }
            }
        } else {
            SubCompanyComInfo scvc = new SubCompanyComInfo();
            scvc.setTofirstRow();
            while (scvc.next()) {
                String id = scvc.getSubCompanyid();

                if (this.dataRanageAllSubCompanyIds != null && !this.dataRanageAllSubCompanyIds.isEmpty()) {
                    if (!this.dataRanageAllSubCompanyIds.contains(id)) {
                        continue;
                    }
                }
                //todo
                if((this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet"))&&!this.checkBlogAppSetting("com",this.rightStr,id)){
                    continue;
                }

                String supsubcomid = scvc.getSupsubcomid();
                String tmp_virtualtype = scvc.getCompanyid();
                if (null != virtualtype && !"".equals(virtualtype)
                        && !"1".equals(virtualtype)) {
                    if (!virtualtype.equals(tmp_virtualtype))
                        continue;
                }

                if (supsubcomid.equals(""))
                    supsubcomid = "0";
                if (!supsubcomid.equals(subId))
                    continue;

                if (adci.isUseAppDetach()) {
                    if ((Util.getIntValue(virtualtype) >= 0 || Util.getIntValue(virtualtype) == -10000) && adci.checkUserAppDetach(id, "2") == 0) {
                        continue;
                    }
                }

                if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initSubCompany(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                    continue;
                }

                String name = Util.formatMultiLang(scvc.getSubCompanyname());
                String canceled = scvc.getCompanyiscanceled();

                TreeNode subCompanyNode = new TreeNode();
                subCompanyNode.setLastname(name);
                subCompanyNode.setId(id);
                subCompanyNode.setNodeid("subcom_" + id + "x");
                subCompanyNode.setPid(subId);
                subCompanyNode.setIcon("/images/treeimages/Home_wev8.gif");
                subCompanyNode.setNocheck("N");
                subCompanyNode.setType("subcom");
                if (hasChild("subcompany", id)) {
                    subCompanyNode.setIsParent("true");
                }

                if (!"1".equals(canceled))
                    companyTreeList.AddChildren(subCompanyNode);
            }
        }

        return companyTreeList;
    }

    private String getResourceNumJson(String nodeids, List<String> selectedids, String sqlwhere) throws Exception {
        JSONArray jsonArr = new JSONArray();
        try {
            RecordSet rs = new RecordSet();
            ResourceComInfo rci = new ResourceComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();
            SubCompanyComInfo scc = new SubCompanyComInfo();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String[] arr_nodeid = Util.TokenizerString2(nodeids, ",");
            for (String nodeid : arr_nodeid) {

                String type = nodeid.split("_")[0];
                String id = nodeid.split("_")[1];
                String sql = "select count(*) from HrmResourceVirtualView hr where hr.status in (0,1,2,3) and 1=1 ";
                JSONObject jo = d.getResourceNumJson(type, id, sql, sqlwhere,
                        selectedids);
                if (null != jo) {
                    jo.put("nodeid", nodeid);
                    jsonArr.add(jo);
                }
            }

        } catch (Exception e) {
            writeLog(e);
        }

        return jsonArr.toString();
    }

    private Map<String, Object> getV2grouptree(Map<String, Object> params) throws Exception {
        Map<String, Object> jo = new JSONObject();
        ResourceComInfo resourceComInfo = new ResourceComInfo();
        jo.put("status", false);

        try {
            if (user == null) {
                jo = getWrongCode(402, jo);
                return jo;
            }
            boolean hasRight = HrmUserVarify.checkUserRight("CustomGroup:Edit", user);
            jo.put("hasRight", hasRight ? 1 : 0);
            MutilResourceBrowser mrb = new MutilResourceBrowser();
            String id = Util.null2String(params.get("id"));
            String alllevel = Util.null2String(params.get("alllevel"));
            String selectedids = Util.null2String(params.get("selectedids"));
            String isNoAccount = Util.null2String(params.get("isNoAccount"));
            String virtualtype = Util.null2String(params.get("virtualtype"));
            String cmd = Util.null2String(params.get("cmd"));
            String sqlwhere = Util.null2String(params.get("sqlwhere"));
            selectedids = mrb.getExcludeSqlWhere(selectedids, alllevel, isNoAccount, user, sqlwhere);
            String groupName = Util.null2String(params.get("groupName"));
            String showNum = Util.null2String(params.get("showNum"));

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }

            List<String> selectList = new ArrayList<String>();
            if (selectedids.length() > 0) {
                String[] tmp_selectedids = selectedids.split(",");
                for (String selectedid : tmp_selectedids) {
                    selectList.add(selectedid);
                }
            }
            JSONArray jsonArr = new JSONArray();
            String resultStr = "";
            if (cmd.equals("getComDeptResource")) {
                String comdeptnodeids = Util.null2String(params.get("comdeptnodeids"));
                String[] groupids = Util.TokenizerString2(comdeptnodeids, ",");
                for (String groupid : groupids) {
                    getV2ResourceByGroupid(jsonArr, user, groupid, selectList, isNoAccount, sqlwhere, virtualtype);
                }
                resultStr = JSON.toJSONString(jsonArr);
                jo.put("status", true);
                jo.put("datas", resultStr);
            } else if (cmd.equals("getAll")) {
                jsonArr = getV2ResourceByGroupid(jsonArr, user, "-1", selectList, isNoAccount, sqlwhere, virtualtype);
                resultStr = jsonArr.toString();
                jo.put("status", true);
                jo.put("datas", resultStr);
            } else if (cmd.equals("getNum")) {
                String nodeids = Util.null2String(params.get("nodeids"));
                resultStr = getV2ResourceJson(nodeids, selectList, user, isNoAccount, sqlwhere);
                jo.put("status", true);
                jo.put("datas", resultStr);
            } else {
                if (id.length() == 0) {
                    // init
                    TreeNode a = getV2GroupTree(user, selectList, isNoAccount, sqlwhere, showNum);
                    if (!groupName.equals("")) {
                        a = getV2GroupTree(user, selectList, isNoAccount, sqlwhere, groupName, showNum);
                    }
                    jo.put("status", true);
                    jo.put("datas", a);
                } else {
                    List<TreeNode> a = getV2ResourceTree(id, selectList, isNoAccount, user, sqlwhere, virtualtype);
                    /**
                     * 浏览按钮自定义显示字段
                     */
                    List<String> ids = new ArrayList();
                    for(TreeNode tn : a){
                        if(tn.getType().equals("resource"))
                            ids.add(tn.getId());
                    }
                    Map<String, Map<String, Object>> map = new HashMap();
                    if(ids.size() > 0){
                        for(String resourceId : ids){
                            Map<String, Object> userInfo = getUserInfo(resourceId, user, true);
                            map.put(resourceId, userInfo);
                        }
                    }
                    JSONArray jsonArray = (JSONArray)JSONArray.toJSON(a);
                    for(int i = 0; i < jsonArray.size(); i++){
                        JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                        String k = jsonObject.getString("id");
                        if(map.containsKey(k)) {
                            jsonObject.putAll(map.get(k));
//                            jsonObject.put("columnControled", true);
                        }
                    }
                    jo.put("status", true);
                    jo.put("datas", jsonArray);

                }
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return jo;
    }

    private Map<String, Object> gethrmstatus(Map<String, Object> params) throws Exception {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", true);

        List<Map<String, String>> getMapList = new ArrayList();
        for (int i = 0; i < 5; i++) {
            Map<String, String> getMap = new HashMap();
            switch (i) {
                case 0:
                    getMap.put("id", "9");
                    getMap.put("order", "0");
                    getMap.put("name", ""+ SystemEnv.getHtmlLabelName(82857,weaver.general.ThreadVarLanguage.getLang())+"");
                    break;
                case 1:
                    getMap.put("id", "8");
                    getMap.put("order", "1");
                    getMap.put("name", ""+ SystemEnv.getHtmlLabelName(1831,weaver.general.ThreadVarLanguage.getLang())+"");
                    break;
                case 2:
                    getMap.put("id", "0");
                    getMap.put("order", "2");
                    getMap.put("name", ""+ SystemEnv.getHtmlLabelName(15710,weaver.general.ThreadVarLanguage.getLang())+"");
                    break;
                case 3:
                    getMap.put("id", "1");
                    getMap.put("order", "3");
                    getMap.put("name", ""+ SystemEnv.getHtmlLabelName(15711,weaver.general.ThreadVarLanguage.getLang())+"");
                    break;
                case 4:
                    getMap.put("id", "2");
                    getMap.put("order", "4");
                    getMap.put("name", ""+ SystemEnv.getHtmlLabelName(480,weaver.general.ThreadVarLanguage.getLang())+"");
                    break;
            }
            getMapList.add(getMap);
        }
        if (1 == user.getUID()) {
            for (int i = 0; i < 5; i++) {
                Map<String, String> getMap = new HashMap();
                switch (i) {
                    case 0:
                        getMap.put("id", "3");
                        getMap.put("order", "5");
                        getMap.put("name", ""+ SystemEnv.getHtmlLabelName(15844,weaver.general.ThreadVarLanguage.getLang())+"");
                        break;
                    case 1:
                        getMap.put("id", "4");
                        getMap.put("order", "6");
                        getMap.put("name", ""+ SystemEnv.getHtmlLabelName(6094,weaver.general.ThreadVarLanguage.getLang())+"");
                        break;
                    case 2:
                        getMap.put("id", "5");
                        getMap.put("order", "7");
                        getMap.put("name", ""+ SystemEnv.getHtmlLabelName(6091,weaver.general.ThreadVarLanguage.getLang())+"");
                        break;
                    case 3:
                        getMap.put("id", "6");
                        getMap.put("order", "8");
                        getMap.put("name", ""+ SystemEnv.getHtmlLabelName(6092,weaver.general.ThreadVarLanguage.getLang())+"");
                        break;
                    case 4:
                        getMap.put("id", "7");
                        getMap.put("order", "9");
                        getMap.put("name", ""+ SystemEnv.getHtmlLabelName(2245,weaver.general.ThreadVarLanguage.getLang())+"");
                        break;
                }
                getMapList.add(getMap);
            }
        }
        jo.put("datas", getMapList);
        return jo;
    }

    private List<TreeNode> getV2ResourceTree(String groupid, List<String> selectedids, String isNoAccount, User user, String sqlwhere, String virtualtype) throws Exception {
        TreeNode root = new TreeNode();
        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            SubCompanyComInfo scc = new SubCompanyComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }
            String fromSql = "hrmresource";
            if (Util.getIntValue(virtualtype) < -1)
                fromSql = "HrmResourcevirtualview";
            String sql = "select hr.id, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle, hr.loginid, hr.account ,hr.workcode, hr.mobile"
                    + " from "
                    + fromSql
                    + " hr, HrmGroupMembers t2 "
                    + " where hr.id= userid and groupid in (" + groupid + ")";
            if (Util.getIntValue(virtualtype) < -1)
                sql += " and hr.virtualtype=" + virtualtype;

            if (sqlwhere.length() > 0) {
                if(sqlwhere.trim().startsWith("and id in"))
                    sqlwhere = sqlwhere.replaceFirst("id in", "hr.id in");
                if (sqlwhere.trim().startsWith("and")) {
                    sqlwhere = " " + sqlwhere;
                } else {
                    sqlwhere = " and " + sqlwhere;
                }
            }
            sqlwhere += " and status in(0,1,2,3) ";
            if(Util.null2String(params.get("from")).equals("ResourceBrowserDecService")) {

            }else{
                if (adci.isUseAppDetach()) {
                    String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                    String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                    sqlwhere += tempstr;
                }
            }
            /*
             * 执行力管理分权增加控制,未开启，查询部门（包括下级）人员，分部（包括下级）人员，
             * */
            if(this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet")){
                String appdetawhere = checkBlogAppLoadSub(this.rightStr);
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (appdetawhere) : "");
                sqlwhere += tempstr;
            }
            if (sqlwhere.length() > 0)
                sql += sqlwhere;
            String noAccountSql = "";
            if (!isNoAccount.equals("1")) {
                noAccountSql = " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
            }
            sql += noAccountSql;

            sql += " order by t2.dsporder";
            root = d.getHrmResourceV2(root, sql, isNoAccount, selectedids);
            if (null != root) {
                int j = root.getChildren().size();
                for (int i = 0; i < j; i++) {
                    String id = root.getChildren().get(i).getId();
                    String subcomid = rsDepartment.getSubcompanyid1(rci.getDepartmentID(id));
                    String supsubcomid = scc.getSupsubcomid(subcomid);
                    root.getChildren().get(i).setDepartmentname(
                            rsDepartment.getDepartmentmark(rci.getDepartmentID(id)));
                    root.getChildren().get(i).setSubcompanyname(scc.getSubCompanyname(subcomid));
                    root.getChildren().get(i).setSupsubcompanyname(
                            scc.getSubCompanyname(supsubcomid));
                    root.getChildren().get(i).setIcon(rci.getMessagerUrls(id));
                    root.getChildren().get(i).setRequestParams(rci.getUserIconInfoStr(id,user));
                    root.getChildren().get(i).setJobtitlename(mrb.getJobTitlesname(id));
                    if(Util.null2String(params.get("from")).equals("ResourceBrowserDecService")) {
                        root.getChildren().get(i).setCanClick("true");
                        root.getChildren().get(i).setIsImgIcon(false);
                        root.getChildren().get(i).setName(rci.getLastname(id));
                    }
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return root.getChildren();
    }

    private TreeNode getV2GroupTree(User user, List<String> selectList, String isNoAccount, String sqlwhere, String showNum) throws Exception {
        TreeNode root = new TreeNode();
        try {
            RecordSet rs = new RecordSet();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            root.setId("-1");
            root.setNodeid("group_-1x");
            root.setLastname(SystemEnv.getHtmlLabelName(17620, user.getLanguage()));
            root.setName(root.getLastname());
            root.setOpen("true");
            root.setType("group");
            root.setIcon("/images/treeimages/Home_wev8.gif");
            root.setCanClick("false");
            root.setDisabledCheck(true);
            root.setIsParent("true");

            // 公共组
            TreeNode node = new TreeNode();
            node.setId("-2");
            node.setNodeid("group_-2x");
            node.setLastname(SystemEnv.getHtmlLabelName(17619, user.getLanguage()));
            node.setName(node.getLastname());
            node.setOpen("true");
            node.setIcon("/images/treeimages/Home_wev8.gif");
            node.setType("group");
            node.setCanClick("false");
            node.setIsPrivate("false");
            node.setDisabledCheck(true);
            node = d.getV2GroupList(node, user, isNoAccount, sqlwhere, showNum);
            if(node.getChildren().size()>0){
                node.setIsParent("true");
            }
            root.AddChildren(node);

            // 私人组
            TreeNode prinode = new TreeNode();
            prinode.setId("-3");
            prinode.setNodeid("group_-3x");
            prinode.setLastname(SystemEnv.getHtmlLabelName(17618, user.getLanguage()));
            prinode.setName(prinode.getLastname());
            prinode.setOpen("true");
            prinode.setType("group");
            prinode.setIcon("/images/treeimages/Home_wev8.gif");
            prinode.setCanClick("false");
            prinode.setIsPrivate("true");
            prinode.setDisabledCheck(true);
            prinode = d.getV2ServiceList(prinode, user, isNoAccount, sqlwhere, showNum);
            if(prinode.getChildren().size()>0){
                prinode.setIsParent("true");
            }
            root.AddChildren(prinode);
        } catch (Exception e) {
            writeLog(e);
        }
        return root;
    }

    private TreeNode getV2GroupTree(User user, List<String> selectList, String isNoAccount, String sqlwhere,String groupName, String showNum) throws Exception {
        TreeNode root = new TreeNode();
        try {
            RecordSet rs = new RecordSet();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            root.setId("-1");
            root.setNodeid("group_-1x");
            root.setLastname(SystemEnv.getHtmlLabelName(17620, user.getLanguage()));
            root.setName(root.getLastname());
            root.setOpen("true");
            root.setType("group");
            root.setIcon("/images/treeimages/Home_wev8.gif");
            root.setCanClick("false");

            // 公共组
            TreeNode node = new TreeNode();
            node.setId("-2");
            node.setNodeid("group_-2x");
            node.setLastname(SystemEnv.getHtmlLabelName(17619, user.getLanguage()));
            node.setName(node.getLastname());
            node.setOpen("true");
            node.setIcon("/images/treeimages/Home_wev8.gif");
            node.setType("group");
            node.setCanClick("false");
            node.setIsPrivate("false");
            node = d.getV2GroupList(node, user, isNoAccount, sqlwhere, groupName, showNum);

            root.AddChildren(node);

            // 私人组
            TreeNode prinode = new TreeNode();
            prinode.setId("-3");
            prinode.setNodeid("group_-3x");
            prinode.setLastname(SystemEnv.getHtmlLabelName(17618, user.getLanguage()));
            prinode.setName(prinode.getLastname());
            prinode.setOpen("true");
            prinode.setType("group");
            prinode.setIcon("/images/treeimages/Home_wev8.gif");
            prinode.setCanClick("false");
            prinode.setIsPrivate("true");
            prinode = d.getV2ServiceList(prinode, user, isNoAccount, sqlwhere, groupName, showNum);
            root.AddChildren(prinode);
        } catch (Exception e) {
            writeLog(e);
        }
        return root;
    }

    private String getV2ResourceJson(String nodeids, List<String> selectList, User user, String isNoAccount, String sqlwhere) throws Exception {
        String[] arr_groupid = Util.TokenizerString2(nodeids, ",");
        JSONArray jsonArr = new JSONArray();
        try {
            RecordSet rs = new RecordSet();
            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            for (String groupid : arr_groupid) {
                String nodeid = groupid;
                if (groupid.equals("-1") || groupid.equals("-2") || groupid.equals("-3")) {
                    // 包含子节点
                    if (groupid.equals("-1")) {
                        groupid = "";
                        groupid = d.getAllGroupId(user, groupid);
                    } else if (groupid.equals("-2")) {
                        groupid = d.getPubGroupId(user, groupid);
                    } else if (groupid.equals("-3")) {
                        groupid = d.getPriGroupId(user, groupid);
                    }

                    JSONObject jo = d.getV2Resource(sqlwhere, groupid, nodeid);
                    if (null != jo) {
                        jsonArr.add(jo);
                    }
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return jsonArr.toString();
    }

    private JSONArray getV2ResourceByGroupid(JSONArray jsonArr, User user, String groupid, List<String> selectedids, String isNoAccount, String sqlwhere, String virtualtype) throws Exception {
        String sql = "";
        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            SubCompanyComInfo scc = new SubCompanyComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }
            // 包含子节点
            if (groupid.equals("-1")) {
                groupid = "";
                groupid = d.getAllGroupId(user, groupid);
            } else if (groupid.equals("-2")) {
                groupid = d.getPubGroupId(user, groupid);
            } else if (groupid.equals("-3")) {
                groupid = d.getPriGroupId(user, groupid);
            }

            String fromSql = "hrmresource";
            if (Util.getIntValue(virtualtype) < -1)
                fromSql = "HrmResourcevirtualview";
            sql = "select hr.id, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle "
                    + " from "
                    + fromSql
                    + " hr, HrmGroupMembers t2 "
                    + " where hr.id= userid and groupid in (" + groupid + ")";
            if (Util.getIntValue(virtualtype) < -1)
                sql += " and hr.virtualtype=" + virtualtype;
            String ids = "";
            for (int i = 0; selectedids != null && i < selectedids.size(); i++) {
                if (ids.length() > 0)
                    ids += ",";
                ids += selectedids.get(i);
            }
            if (ids.length() > 0)
                sql += " and userid not in (" + ids + ")";
            if (sqlwhere.length() > 0) {
                if(sqlwhere.trim().startsWith("and id in"))
                    sqlwhere = sqlwhere.replaceFirst("id in", "hr.id in");
                if (sqlwhere.trim().startsWith("and")) {
                    sqlwhere = " " + sqlwhere;
                } else {
                    sqlwhere = " and " + sqlwhere;
                }
            }
            sqlwhere += " and status in(0,1,2,3) ";
            if (adci.isUseAppDetach()) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sqlwhere += tempstr;
            }
            if (sqlwhere.length() > 0)
                sql += sqlwhere;

            String noAccountSql = "";
            if (!isNoAccount.equals("1")) {
                noAccountSql = " and loginid is not null " + (rs.getDBType().equals("oracle") ? "" : " and loginid<>'' ");
            }
            sql += noAccountSql;

            sql += " order by hr.dsporder";

            /**
             * 浏览按钮自定义显示字段
             */
            List<String> resourceIds = new ArrayList();
            jsonArr = d.getV2ResList(sql, jsonArr, ids);
            if (null != jsonArr && 0 < jsonArr.size()) {
                for (int i = 0; i < jsonArr.size(); i++) {
                    JSONObject ja = jsonArr.getJSONObject(i);
                    ja.put("jobtitlename", mrb.getJobTitlesname(ja.getString("id")));
                    ja.put("messagerurl", rci.getMessagerUrls(ja.getString("id")));
                    String id = ja.getString("id");
                    resourceIds.add(id);
                    String subcomid = rsDepartment.getSubcompanyid1(rci.getDepartmentID(id));
                    String supsubcomid = scc.getSupsubcomid(subcomid);
                    ja.put("departmentid", rci.getDepartmentID(id));
                    ja.put("departmentname", rsDepartment.getDepartmentmark(rci.getDepartmentID(id)));
                    ja.put("subcompanyid", subcomid);
                    ja.put("subcompanyname", scc.getSubCompanyname(subcomid));
                    ja.put("supsubcompanyid", supsubcomid);
                    ja.put("supsubcompanyname", scc.getSubCompanyname(supsubcomid));
                    ja.put("requestParams", rci.getUserIconInfoStr(id,user));
                }
            }
            Map<String, Map<String, Object>> map = new HashMap();
            if(resourceIds.size() > 0){
                for(String resourceId : resourceIds){
                    Map<String, Object> userInfo = getUserInfo(resourceId, user, true);
                    map.put(resourceId, userInfo);
                }
            }
            for (int i = 0; i < jsonArr.size(); i++) {
                JSONObject ja = jsonArr.getJSONObject(i);
                String id = ja.getString("id");
                if(map.containsKey(id))
                    ja.putAll(map.get(id));
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return jsonArr;
    }

    private List<Tree> getDepartTreeList(List<Tree> otl, String subId, String departmentId, String selectedids, String isNoAccount, User user, String sqlwhere) {
        DepartmentComInfo rsDepartment = new DepartmentComInfo();
        AppDetachComInfo adci = new AppDetachComInfo();
        rsDepartment.setTofirstRow();
        if (departmentId.length() > 0) {
            // 获取人员
            getResourceTreeList(otl, departmentId, selectedids, isNoAccount,
                    user, sqlwhere);
        }
        while (rsDepartment.next()) {
            if (departmentId.equals(rsDepartment.getDepartmentid()))
                continue;
            String supdepid = rsDepartment.getDepartmentsupdepid();
            if (departmentId.equals("0") && supdepid.equals(""))
                supdepid = "0";
            if (!(rsDepartment.getSubcompanyid1().equals(subId) && (supdepid
                    .equals(departmentId) || (!rsDepartment.getSubcompanyid1(
                    supdepid).equals(subId) && departmentId.equals("0")))))
                continue;

            String id = rsDepartment.getDepartmentid();
            String name = rsDepartment.getDepartmentmark();
            String canceled = rsDepartment.getDeparmentcanceled();
            Tree newtree = new Tree();
            newtree.setId("dept_" + id);
            newtree.setPid("dept_" + supdepid);
            newtree.setLastname(name);
            newtree.setNocheck("Y");
            newtree.setIcon("/images/treeimages/subCopany_Colse_wev8.gif");
            if (hasChild("dept", id)) {
                newtree.setIsParent("true");
            }
            if (!"1".equals(canceled))
                otl.add(newtree);
        }

        return otl;
    }

    private List<Tree> getResourceTreeList(List<Tree> otl, String departmentId, String selectedids, String isNoAccount, User user, String sqlwhere) {
        try {
            RecordSet rs = new RecordSet();
            AppDetachComInfo adci = new AppDetachComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            SubCompanyComInfo scc = new SubCompanyComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();

            Dao_Hrm4Ec d = null;
            if (!"oracle".equals(rs.getDBType())) {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcSqlServer");
            } else {
                d = Dao_Hrm4EcFactory.getInstance().getDao("Dao_Hrm4EcOracle");
            }

            String sql = "select hr.id, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle, loginid, account ,hr.departmentid"
                    + "from hrmresource hr, hrmdepartment t2 "
                    + "where hr.departmentid=t2.id and t2.id=" + departmentId;
            if (sqlwhere.length() > 0) {
                if(sqlwhere.trim().startsWith("and id in"))
                    sqlwhere = sqlwhere.replaceFirst("id in", "hr.id in");
                if (sqlwhere.trim().startsWith("and")) {
                    sqlwhere = " " + sqlwhere;
                } else {
                    sqlwhere = " and " + sqlwhere;
                }
            }
            sqlwhere += " and hr.status in (0,1,2,3)";
            if (adci.isUseAppDetach()) {
                String appdetawhere;
                appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sqlwhere += tempstr;
            }
            if (sqlwhere.length() > 0)
                sql += sqlwhere;
            sql += " order by hr.dsporder ";
            List<Tree> RSotl = d.getHrmResource(sql);
            if (null != RSotl && 0 < RSotl.size()) {
                for (int i = 0; i < RSotl.size(); i++) {
                    RSotl.get(i).setIcon(rci.getMessagerUrls(RSotl.get(i).getId()));
                    RSotl.get(i).setRequestParams(rci.getUserIconInfoStr(RSotl.get(i).getId(),user));
                }
            }
            otl.addAll(RSotl);
        } catch (Exception e) {
            writeLog(e);
        }
        return otl;
    }

    /**
     * 指定节点下是否有子节点
     *
     * @param id 节点id
     * @return boolean
     * @throws Exception
     */
    private boolean hasChild(String type, String id) {
        boolean hasChild = false;
        try {
            ResourceComInfo rci = new ResourceComInfo();
            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            SubCompanyComInfo scc = new SubCompanyComInfo();
            SubCompanyVirtualComInfo scvc = new SubCompanyVirtualComInfo();
            DepartmentVirtualComInfo dvci = new DepartmentVirtualComInfo();
            ResourceVirtualComInfo rvci = new ResourceVirtualComInfo();
            if (Util.getIntValue(id) < 0) {
                if (type.equals("subcompany")) {
                    scvc.setTofirstRow();
                    while (scvc.next()) {
                        if (scvc.getSupsubcomid().equals(id) && !"1".equals(scvc.getCompanyiscanceled()))
                            hasChild = true;
                    }

                    dvci.setTofirstRow();
                    while (dvci.next()) {
                        if (dvci.getSubcompanyid1().equals(id) && !"1".equals(dvci.getDeparmentcanceled())) {
                            hasChild = true;
                        }
                    }
                } else if (type.equals("dept")) {

                    dvci.setTofirstRow();
                    while (dvci.next()) {
                        String str = dvci.getSubcompanyid1(id);
                        if (dvci.getSubcompanyid1().equals(str) && dvci.getDepartmentsupdepid().equals(id) && !"1".equals(dvci.getDeparmentcanceled()))
                            hasChild = true;
                    }
                    if (!hasChild) {
                        RecordSet rs = new RecordSet();
                        rs.executeSql("select count(*) from HrmResourceVirtualView t1 where t1.status in (0,1,2,3) and t1.departmentid=" + id);
                        if (rs.next()) {
                            if (rs.getInt(1) > 0) {
                                hasChild = true;
                            }
                        }
                    }
                }
            } else {
                if (type.equals("subcompany")) {
                    scc.setTofirstRow();
                    while (scc.next()) {
                        if (scc.getSupsubcomid().equals(id) && !"1".equals(scc.getCompanyiscanceled()))
                            hasChild = true;
                    }

                    rsDepartment.setTofirstRow();
                    while (rsDepartment.next()) {
                        if (rsDepartment.getSubcompanyid1().equals(id) && !"1".equals(rsDepartment.getDeparmentcanceled())) {
                            hasChild = true;
                        }
                    }
                } else if (type.equals("dept")) {
                    rsDepartment.setTofirstRow();
                    while (rsDepartment.next()) {
                        String str = rsDepartment.getSubcompanyid1(id);
                        if (rsDepartment.getSubcompanyid1().equals(str) && rsDepartment.getDepartmentsupdepid().equals(id) && !"1".equals(rsDepartment.getDeparmentcanceled()))
                            hasChild = true;
                    }
                    if (!hasChild) {
                        rci.setTofirstRow();
                        while (rci.next()) {
                            String str = rci.getDepartmentID();
                            if (str.equals(id))
                                hasChild = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return hasChild;
    }

    private Map<String, Object> getWrongCode(int errid, Map<String, Object> jo) {
        jo.put("error", errid);
        jo.put("status", "false");
        switch (errid) {
            case 402:
                jo.put("msg", ""+ SystemEnv.getHtmlLabelName(2011,weaver.general.ThreadVarLanguage.getLang())+"");
                break;
            case 403:
                jo.put("msg", ""+ SystemEnv.getHtmlLabelName(501597,weaver.general.ThreadVarLanguage.getLang())+"");
                break;
            default:
                jo.put("msg", ""+ SystemEnv.getHtmlLabelName(382011,weaver.general.ThreadVarLanguage.getLang())+"");
                break;
        }
        return jo;
    }

    private List getMobileTemplate(){
        List<Object> mobileShowTemplate = new ArrayList<>();
        int i = 0;
        for(Map m : browserFieldConfig){
            Map c = new HashMap();
            String fieldName = String.valueOf(m.get("fieldName"));
            c.put("dataIndex", fieldName + "span");
            c.put("showType", i % 2);
            c.put("hide", "false");
            if(i == 0)
                c.put("primary", "true");
            mobileShowTemplate.add(c);
            i++;
        }
        return mobileShowTemplate;
    }

    private List<Object> getMobileShowTemplate() {
        List<Object> mobileShowTemplate = new ArrayList<>();
        Map<String, Object> col = null;
        List<Object> colConfigs = null;
        Map<String, Object> colRow = null;
        List<Object> colRowConfigs = null;
        Map<String, Object> cell = null;

        col = new HashMap<>();
        col.put("key", "col1");
        colConfigs = new ArrayList();
        col.put("configs", colConfigs);
        colRow = new HashMap<>();
        colRow.put("key", "col1_row1");
        colRowConfigs = new ArrayList();
        colRow.put("configs", colRowConfigs);
        colConfigs.add(colRow);
        cell = new HashMap<>();
        cell.put("key", "lastname");
        colRowConfigs.add(cell);

        colRow = new HashMap<>();
        colRow.put("key", "col1_row2");
        colRowConfigs = new ArrayList();
        colRow.put("configs", colRowConfigs);
        colConfigs.add(colRow);
        cell = new HashMap<>();
        cell.put("key", "subcompanyname");
        colRowConfigs.add(cell);
        mobileShowTemplate.add(col);

        col = new HashMap<>();
        col.put("key", "col2");
        colConfigs = new ArrayList();
        col.put("configs", colConfigs);
        colRow = new HashMap<>();
        colRow.put("key", "col2_row1");
        colRowConfigs = new ArrayList();
        colRow.put("configs", colRowConfigs);
        colConfigs.add(colRow);
        cell = new HashMap<>();
        cell.put("key", "jobtitlename");
        colRowConfigs.add(cell);

        colRow = new HashMap<>();
        colRow.put("key", "col2_row2");
        colRowConfigs = new ArrayList();
        colRow.put("configs", colRowConfigs);
        colConfigs.add(colRow);
        cell = new HashMap<>();
        cell.put("key", "departmentname");
        colRowConfigs.add(cell);
        mobileShowTemplate.add(col);

        return mobileShowTemplate;
    }

    private Map<String, Object> getAllResourceList(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        try {
            List<Map<String, String>> lsRow = new ArrayList<>();
            Map<String, String> row = new HashMap<>();
            HrmGroupTreeComInfo hrmgrpcominfo = new HrmGroupTreeComInfo();
            String[] allresourceArray = hrmgrpcominfo.getResourceAll(user, params, false);
            if (allresourceArray != null && allresourceArray.length > 0) {
                jo.put("status", true);
                jo.put("datas", allresourceArray[0]);
                if (allresourceArray[0] != null) {
                    jo.put("count", allresourceArray[0].split(",").length);
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return jo;
    }

    private Map<String, Object> getResourceListByLetter(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        String sql = "";
        RecordSet rs = new RecordSet();
        try {
            List<Map<String, String>> lsRow = new ArrayList<>();
            Map<String, String> row = null;
            AppDetachComInfo adci = new AppDetachComInfo();
            DepartmentComInfo rsDepartment = new DepartmentComInfo();
            ResourceComInfo rci = new ResourceComInfo();
            SubCompanyComInfo scc = new SubCompanyComInfo();
            MutilResourceBrowser mrb = new MutilResourceBrowser();
            String letter = Util.null2String(params.get("letter")).toLowerCase();
            List<String> lsLetter = Util.splitString2List(Util.null2String(params.get("letter")).toLowerCase(), ",");
            String sqlwhere = Util.null2String((String) params.get("sqlwhere"));
            if (StringUtils.isNotBlank(sqlwhere)) {
                sqlwhere = " and " + sqlwhere;
            }

            sql = " select id" + displayFieldsStr + " from hrmresource hr where status in(0,1,2,3) " + sqlwhere;

            if (letter.equals("#")) {
                if (rs.getDBType().toLowerCase().equals("oracle")) {
                    sql += " and (lower(substr(pinyinlastname,0,1)) not in ('a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z')) ";
                } else {
                    sql += " and (lower(substring(pinyinlastname,1,1)) not in ('a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z')) ";
                }
            } else {
                String tmpSql = "";
                for (int i = 0; lsLetter != null && i < lsLetter.size(); i++) {
                    if (i == 0) {
                        sql += " and ( ";
                    }
                    if (tmpSql.length() > 0) tmpSql += " or ";
                    tmpSql += " pinyinlastname like '" + lsLetter.get(i) + "%' ";
                }
                if (tmpSql.length() > 0) {
                    sql += tmpSql + ")";
                }
            }
            if (adci.isUseAppDetach()) {
                String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "", true, "resource_hr");
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
                sql += tempstr;
            }
            sql += " order by dsporder, pinyinlastname ";
            this.writeLog("gecy getResourceListByLetter>>>" + sql);
            rs.executeQuery(sql);
            while (rs.next()) {
                String userid = rs.getString("id");
//                String subcomid = rsDepartment.getSubcompanyid1(rci.getDepartmentID(userid));
//                String supsubcomid = scc.getSupsubcomid(subcomid);
                row = new HashMap<>();
                row.put("id", userid);
                for(String f : getDisplayFields()){
                    row.put(f, rs.getString(f));
                }
//                row.put("img", rci.getMessagerUrls(userid));
//                row.put("messagerurl", rci.getMessagerUrls(userid));
//                row.put("lastname", rci.getLastname(userid));
//                row.put("pinyinlastname", rci.getPinyinlastname(userid));
//                row.put("jobtitlename", mrb.getJobTitlesname(userid));
//                row.put("departmentid", rci.getDepartmentID(userid));
//                row.put("departmentname", rsDepartment.getDepartmentmark(rci.getDepartmentID(userid)));
//                row.put("subcompanyid", subcomid);
//                row.put("subcompanyname", scc.getSubCompanyname(subcomid));
//                row.put("supsubcompanyid", supsubcomid);
//                row.put("supsubcompanyname", scc.getSubCompanyname(supsubcomid));
                lsRow.add(row);
            }

            lsRow = EditGetMap(lsRow);
            jo.put("status", true);
            jo.put("datas", lsRow);
        } catch (Exception e) {
            writeLog(e);
        }
        return jo;
    }

    private Map<String, Object> getOrgResource(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        String types = Util.null2String(params.get("types"));
        String alllevel = Util.null2String(params.get("alllevel")).length() == 0 ? "0" : Util.null2String(params.get("alllevel"));
        String isNoAccount = Util.null2String(params.get("isNoAccount")).length() == 0 ? "0" : Util.null2String(params.get("isNoAccount"));
        if ("".equals(types))
            return result;
        String[] typeArr = types.split(",");
        List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
        try {
            MutilResourceBrowser mrb = new MutilResourceBrowser();
            ResourceComInfo rcomInfo = new ResourceComInfo();
            DepartmentComInfo deptComInfo = new DepartmentComInfo();
            SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
            String whereClause = Util.null2String(params.get("sqlwhere"));
            String resourceids = "";

            if (whereClause.length() > 0) whereClause = " and " + whereClause;

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias","hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            if (Util.null2String(dataRanage.get("sqlWhere")).length() > 0) {
                if (whereClause.length() > 0) {
                    if (!whereClause.trim().startsWith("and")) whereClause += "and";
                }
                whereClause += Util.null2String(dataRanage.get("sqlWhere"));
            }
            /*
             * 执行力管理分权增加控制,未开启，查询部门（包括下级）人员，分部（包括下级）人员，
             * */
            if(this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet")){
                String appdetawhere = checkBlogAppLoadSub(this.rightStr);
                String tempstr = (appdetawhere != null && !"".equals(appdetawhere) ? (appdetawhere) : "");
                whereClause += tempstr;
            }
            this.writeLog("gecy getOrgResource>>>>" + whereClause + ";;" + user.getUID() + ";;" + types);

            for (String typeInfo : typeArr) {
                String[] typeInfoArr = typeInfo.split("\\|");
                List<Map> records = new ArrayList<>();
                if (typeInfoArr.length > 1) {
                    String type = typeInfoArr[0];
                    String id = typeInfoArr[1];
                    if (type.equals("subcom") || type.equals("dept") || type.equals("com")) {
                        // 部门
                        String nodeid = type + "_" + id;
                        if (Integer.parseInt(id) < 0) {
                            // 虚拟
                            resourceids = mrb.getComDeptResourceVirtualIds(nodeid, alllevel, isNoAccount, user, whereClause);
                        } else {
                            resourceids = mrb.getComDeptResourceIds(nodeid, alllevel, isNoAccount, user, whereClause);
                        }
                    } else if (type.equals("group")) {// 自定义组
                        resourceids = mrb.getGroupResourceIds(id, isNoAccount, user, whereClause);
                    }

                    String[] resourceidArr = Util.TokenizerString2(resourceids, ",");
                    List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
                    List<String> ids = new ArrayList();
                    for (String resourceid : resourceidArr) {
                        if ("".equals(Util.null2String(resourceid)))
                            continue;
                        ids.add(resourceid);
                    }

                    if(ids.size() > 0){
                        for(String resourceId : ids){
                            users.add(getUserInfo(resourceId, user, true));
                        }
                    }

                    Map<String, Object> typeresult = new HashMap<String, Object>();
                    typeresult.put("id", id);
                    typeresult.put("type", type);
                    typeresult.put("users", users);
                    typeresult.put("nodeid", type + "_" + id + "x");
                    datas.add(typeresult);
                } else {
                    if ("".equals(Util.null2String(typeInfo)))
                        continue;
                    datas.add(getUserInfo(typeInfo, user, true));
                }
            }
        } catch (Exception e) {
            writeLog(e);
            e.printStackTrace();
        }
        result.put("status", true);
        result.put("datas", datas);
        return result;
    }

      //微博单独返回人员 由微博模块维护
      private Map<String, Object> getBlogHrmResource(Map<String, Object> params) {

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();

        int pageSize = Util.getIntValue((String) params.get("pageSize"),10) ;
        int current = Util.getIntValue((String) params.get("current"),10) ;
        String conditon = StringUtils.EMPTY ;
        conditon= BlogDiscussShareUtil.getShareSqlWhereForBrw(this.user.getUID()+"", (String) params.get("lastname"));

        RecordSet rs = new RecordSet();
        String sql = "";

          if(rs.getDBType().equals("mysql")||rs.getDBType().equals("sqlserver")){

              sql = " select id from hrmresource hr where (status =0 or status = 1 or status = 2 or status = 3) and (loginid is not null and loginid <> '') and seclevel>=0 "+conditon;
          }else{

              sql = " select id from hrmresource hr where (status =0 or status = 1 or status = 2 or status = 3) and (loginid is not null ) and seclevel>=0 "+conditon;
          }
            sql += "  order by dsporder ";

          rs.executeQuery(sql);
          writeLog("blogshare ------------ "+sql);
          int count = rs.getCounts();

          SplitPageParaBean spp = new SplitPageParaBean();
          SplitPageUtil spu = new SplitPageUtil();

          String backfields = "id";
          spp.setBackFields(backfields);
          spp.setSqlFrom("hrmresource hr");

          if(rs.getDBType().equals("mysql")||rs.getDBType().equals("sqlserver")){
              spp.setSqlWhere(" where (status =0 or status = 1 or status = 2 or status = 3)  and (loginid is not null and loginid <> '') and seclevel>=0 "+conditon);
          }else{
              spp.setSqlWhere(" where (status =0 or status = 1 or status = 2 or status = 3)  and (loginid is not null) and seclevel>=0 "+conditon);
          }

          spp.setPrimaryKey("id");
          spp.setSqlOrderBy("dsporder");
          spu.setSpp(spp);

          rs = spu.getCurrentPageRsNew(current, pageSize);
        try {
            while (rs.next()) {
                    users.add(getUserInfo(rs.getString("id"), user, true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.put("status", true);
        result.put("datas", users);
          result.put("count",count);
        return result;

    }


    //微博单独返回人员 由微博模块维护
    private Map<String, Object> getBlogSelectedHrmResource(Map<String, Object> params) {

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();


        String selectIds = Util.null2String( params.get("selectids"));

        if(selectIds.equals("")){
            result.put("status", true);
            result.put("datas", users);
        }

        RecordSet rs = new RecordSet();
        String sql = "";

        sql = " select id from hrmresource hr where (status =0 or status = 1 or status = 2 or status = 3)  and seclevel>=0 and id in ("+selectIds+")";
        sql += "  order by dsporder ";

        try {
            rs.executeSql(sql);
            while (rs.next()) {
                users.add(getUserInfo(rs.getString(1), user, true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.put("status", true);
        result.put("datas", users);
        return result;

    }

    private Map<String, Object> getUserInfo(String userid, ResourceComInfo rcomInfo, DepartmentComInfo deptComInfo, SubCompanyComInfo subCompanyComInfo) throws Exception {
        Map<String, Object> userInfo = new HashMap<String, Object>();
        userInfo.put("id", userid);
        userInfo.put("lastname", rcomInfo.getLastname(userid));
        userInfo.put("pinyinlastname", rcomInfo.getLastname(userid)); //rcomInfo.getPinyinlastname(userid)
        userInfo.put("jobtitlename", MutilResourceBrowser.getJobTitlesname(userid));
        userInfo.put("icon", rcomInfo.getMessagerUrls(userid));
        userInfo.put("requestParams", rcomInfo.getUserIconInfoStr(userid,user));
        userInfo.put("type", "resource");
        userInfo.put("nodeid", "resource_" + userid + "x");
        userInfo.put("departmentid", rcomInfo.getDepartmentID(userid));
        userInfo.put("departmentname", deptComInfo.getDepartmentmark(rcomInfo.getDepartmentID(userid)));
        String subcompanyid = deptComInfo.getSubcompanyid1(rcomInfo.getDepartmentID(userid));
        String parentsubcompanyid = subCompanyComInfo.getSupsubcomid(subcompanyid);
        userInfo.put("subcompanyid", subcompanyid);
        userInfo.put("subcompanyname", subCompanyComInfo.getSubcompanyname(subcompanyid));
        userInfo.put("supsubcompanyid", parentsubcompanyid);
        userInfo.put("supsubcompanyname", subCompanyComInfo.getSubcompanyname(parentsubcompanyid));
        return userInfo;
    }


    private Map<String, Object> getUserInfo(String userid, User user, Boolean allFields) throws Exception{
        MutilResourceBrowser mrb = new MutilResourceBrowser();
        ResourceComInfo rcomInfo = new ResourceComInfo();
        DepartmentComInfo deptComInfo = new DepartmentComInfo();
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        JobActivitiesComInfo jobActivitiesComInfo = new JobActivitiesComInfo();
        JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
        String jobactivityId = jobTitlesComInfo.getJobactivityid(rcomInfo.getJobTitle(userid));
        PrivacyComInfo pc = new PrivacyComInfo();
        Map<String, String> mapShowSets = pc.getMapShowSets();
        AccountType accountType = new AccountType();
        HrmTransMethod hrmTransMethod = new HrmTransMethod();
        EncryptFieldViewScopeConfigComInfo encryptFieldViewScopeConfigComInfo = new EncryptFieldViewScopeConfigComInfo();
        Map<String, Object> userInfo = new HashMap<String, Object>();
        userInfo.put("id", userid);
        userInfo.put("lastname", rcomInfo.getLastname(userid));
        userInfo.put("pinyinlastname", rcomInfo.getLastname(userid)); //rcomInfo.getPinyinlastname(userid)
        userInfo.put("jobtitlename", MutilResourceBrowser.getJobTitlesname(userid));
        userInfo.put("icon", rcomInfo.getMessagerUrls(userid));
        userInfo.put("type", "resource");
        userInfo.put("email", rcomInfo.getEmail(userid));
        userInfo.put("nodeid", "resource_" + userid + "x");
        String departmentid = rcomInfo.getDepartmentID(userid);
        userInfo.put("departmentid", rcomInfo.getDepartmentID(userid));
        userInfo.put("departmentname", deptComInfo.getDepartmentmark(rcomInfo.getDepartmentID(userid)));
        String subcompanyid  = deptComInfo.getSubcompanyid1(rcomInfo.getDepartmentID(userid));
        String parentsubcompanyid  = subCompanyComInfo.getSupsubcomid(subcompanyid);
        userInfo.put("subcompanyid", subcompanyid);
        userInfo.put("subcompanyname", subCompanyComInfo.getSubcompanyname(subcompanyid));
        userInfo.put("supsubcompanyid", parentsubcompanyid);
        userInfo.put("supsubcompanyname", subCompanyComInfo.getSubcompanyname(parentsubcompanyid));
        userInfo.put("requestParams", rcomInfo.getUserIconInfoStr(userid,user));

        try {
            List<String> displayKeys = new ArrayList<>();
            for (Map m : browserFieldConfig) {
                String fieldName = Util.null2String(m.get("fieldName"));
                String fullPath = Util.null2String(m.get("fullPath"));
                String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                if(fullPathDelimiter.equals(""))
                    fullPathDelimiter = "|";
                else
                    fullPathDelimiter = "" + fullPathDelimiter + "";
                String orderType = Util.null2String(m.get("orderType"));
                String key = fieldName + "span";
                generateDisplayKeys(displayKeys, key, allFields);
                switch (fieldName) {
                    case "departmentid":
                        String fullDeptPath = deptComInfo.getDepartmentRealPath(departmentid, fullPathDelimiter, orderType);
                        userInfo.put(key, fullPath.equals("1") ? fullDeptPath : deptComInfo.getDepartmentmark(departmentid));
                        break;
                    case "subcompanyid1":
                        userInfo.put(key, fullPath.equals("1") ? subCompanyComInfo.getSubcompanyRealPath(subcompanyid, fullPathDelimiter, orderType) : subCompanyComInfo.getSubCompanyname(subcompanyid));
                        break;
                    case "accounttype":
                        userInfo.put(key, accountType.getAccountType(rcomInfo.getAccountType(userid), user.getLanguage()+""));
                        break;
                    case "sex":
                        userInfo.put(key, rcomInfo.getSexName(Util.null2String(rcomInfo.getSexs(userid)),""+user.getLanguage()));
                        break;
                    case "managerid":
                        userInfo.put(key, rcomInfo.getResourcename(rcomInfo.getManagerID(userid)));
                        break;
                    case "assistantid":
                        userInfo.put(key, rcomInfo.getResourcename(rcomInfo.getAssistantID(userid)));
                        break;
                    case "jobtitle":
                        userInfo.put(key, mrb.getJobTitlesname(userid));
                        break;
                    case "status":
                        userInfo.put(key, ResourceComInfo.getStatusName(Util.null2String(rcomInfo.getStatus(userid)), user.getLanguage() + ""));
                        break;
                    case "jobactivity":
                        userInfo.put(key, Util.null2String(jobActivitiesComInfo.getJobActivitiesname(jobactivityId)));//hrmTransMethod.getJobActivitiesname("", rcomInfo.getJobTitle(userid)));
                        break;
                    case "orgid":
                        userInfo.put(key, deptComInfo.getAllParentDepartmentBlankNames(departmentid, subcompanyid, "|"));
                        break;
                    case "jobcall":
                        userInfo.put(key, Util.null2String(new JobCallComInfo().getJobCallname(rcomInfo.getJobCall(userid))));
                        break;
                    case "jobGroupId":
                        String jobtitleid = rcomInfo.getJobTitle(userid) ;
                        String jobActivityId = new JobTitlesComInfo().getJobactivityid(jobtitleid);
                        String groupId = jobActivitiesComInfo.getJobgroupid(jobActivityId);
                        userInfo.put(key, Util.null2String(new JobGroupsComInfo().getJobGroupsname(groupId)));
                        break;
                    case "joblevel":
                        userInfo.put(key, Util.null2String(rcomInfo.getJoblevel(userid)));
                        break;
                    case "jobactivitydesc":
                        userInfo.put(key, Util.null2String(rcomInfo.getJobActivityDesc(userid)));
                        break;
                    case "workroom":
                        userInfo.put(key, Util.null2String(rcomInfo.getWorkroom(userid)));
                        break;
                    default:

                        String value = "";
                        switch(fieldName){
                            case "loginid":
                                value = rcomInfo.getLoginID(userid);
                                break;
                            case "workcode":
                                value = rcomInfo.getWorkcode(userid);
                                break;
                            case "lastname":
                                value = rcomInfo.getLastname(userid);
                                break;
                            case "locationid":
                                value = rcomInfo.getLocationid(userid);
                                break;
                            case "mobile":
                                value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","mobile")? EncryptConfigBiz.getDecryptData(rcomInfo.getMobile(userid)):rcomInfo.getMobile(userid);
                                break;
                            case "telephone":
                                value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","telephone")? EncryptConfigBiz.getDecryptData(rcomInfo.getTelephone(userid)):rcomInfo.getTelephone(userid);
                                break;
                            case "mobilecall":
                                value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","mobilecall")? EncryptConfigBiz.getDecryptData(rcomInfo.getMobileCall(userid)):rcomInfo.getMobileCall(userid);
                                break;
                            case "fax":
                                value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","fax")? EncryptConfigBiz.getDecryptData(rcomInfo.getFax(userid)):rcomInfo.getFax(userid);
                                break;
                            case "email":
                                value = encryptFieldViewScopeConfigComInfo.fieldCanView(""+user.getUID(),"hrmresource","email")? EncryptConfigBiz.getDecryptData(rcomInfo.getEmail(userid)):rcomInfo.getEmail(userid);
                                break;
                            case "managerid":
                                value = rcomInfo.getManagerID(userid);
                                break;
                            case "assistantid":
                                value = rcomInfo.getAssistantID(userid);
                                break;
                        }
                        if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                            userInfo.put(key, pc.getSearchContent(Util.null2String(value), (userid + "+" + user.getUID() + "+" + fieldName)));
                        } else {
                            userInfo.put(key, hrmTransMethod.getDefineContent(Util.null2String(value), fieldName + ":" + user.getLanguage()));
                        }
                        break;
                }
            }
            if(displayKeys.size() > 0) userInfo.put("displayKeys", displayKeys);
        }catch (Exception e){
            e.printStackTrace();
        }
        return userInfo;
    }

    /**
     * 加载人力浏览按钮显示列定义
     * @return
     */
    private List loadDisplayColumnConfig() {
        List columns = new ArrayList();
        displayFields.clear();
        displayFields.add("lastname");
        displayFields.add("pinyinlastname");
        displayFields.add("departmentid");
        displayFields.add("subcompanyid1");
        displayFields.add("workcode");
        displayFields.add("mobile");
        try {
            browserFieldConfig = new BrowserDisplayFieldServiceImpl().getConfig(1);
            int i = 0;
            for (Map m : browserFieldConfig) {
                Map c = new HashMap();
                String fieldName = String.valueOf(m.get("fieldName"));
                if (!fieldName.equals("jobactivity") && !fieldName.equals("orgid") && !this.displayFields.contains(fieldName))
                    displayFields.add(fieldName);
                c.put("dataIndex", fieldName + "span");
                c.put("showType", i > 1 ? "0" : "1");
                c.put("hide", "false");
                columns.add(c);
                i++;
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        displayFieldsStr = ", " + String.join(",", getDisplayFields());

        if(Util.null2String(params.get("from")).equals("ResourceBrowserDecService")) {
            List<ListHeadBean> tableHeadColumns = new ArrayList<ListHeadBean>();
            tableHeadColumns.add(new ListHeadBean("id", BoolAttr.TRUE).setIsPrimarykey(BoolAttr.TRUE));
            tableHeadColumns.add(new ListHeadBean("lastname", SystemEnv.getHtmlLabelName(413, user.getLanguage()), 1, BoolAttr.TRUE));
            tableHeadColumns.add(new ListHeadBean("jobtitlename", "", 1));
            tableHeadColumns.add(new ListHeadBean("departmentname", ""));
            columns = tableHeadColumns;
        }
        return columns;
    }

    private List<String> getDisplayFields(){
        List<String> list = new ArrayList<>();
        for(String f : displayFields){
            if(f.toLowerCase().equals("jobgroupid") || f.toLowerCase().equals("jobactivity")){
                continue;
            }
            list.add(f);
        }
        return list;
    }

    private void generateDisplayKeys(List<String> list, String key, Boolean allFields){
        if(allFields == true || key.equals("lastnamespan") || !browserFieldFilterKeys.contains(key))
            list.add(key);
    }

    private void generateRange(Map<String, Object> dataRanage, String virtualtype){
        try {
            String sqlWhere = Util.null2String(dataRanage.get("sqlWhere"));
            if (sqlWhere.length() > 0) {
                SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();
                SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
                DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
                DepartmentComInfo departmentComInfo = new DepartmentComInfo();

                RecordSet rs = new RecordSet();
                Set<String> subCompanyIds = new HashSet<>();
                Set<String> departmentIds = new HashSet<>();
                Set<String> allSubCompanyIds = new HashSet<>();
                Set<String> allDepartmentIds = new HashSet<>();

                String table = "HRMRESOURCEALLVIEW";
                if(Util.getIntValue(virtualtype, 1) < 0){
                    table = "HRMRESOURCEVIRTUALVIEW";
                }

                String sql = "select subcompanyid1, departmentid from " + table + " where 1 = 1" + sqlWhere;
                rs.executeQuery(sql);
                while (rs.next()) {
                    String subcompanyId = Util.null2String(rs.getString("subcompanyid1"));
                    String departmentId = Util.null2String(rs.getString("departmentid"));
                    if (!"".equals(subcompanyId)) {
                        subCompanyIds.add(subcompanyId);
                    }
                    if (!"".equals(subcompanyId)) {
                        departmentIds.add(departmentId);
                    }
                }

                Iterator iterator = subCompanyIds.iterator();
                while (iterator.hasNext()) {
                    String ids = "";
                    String v = (String)iterator.next();
                    allSubCompanyIds.add(v);
                    if (Integer.parseInt(v) < 0) {
                        ids = subCompanyVirtualComInfo.getAllSupCompany(v);
                    }else{
                        ids = subCompanyComInfo.getAllSupCompany(v);
                    }
                    String [] idArr = Util.splitString(ids, ",");
                    for(String id : idArr){
                        allSubCompanyIds.add(id);
                    }
                }

                iterator = allSubCompanyIds.iterator();
                while (iterator.hasNext()) {
                    String v = (String)iterator.next();
                    if(!"".equals(v))
                        dataRanageAllSubCompanyIds.add(v);
                }
                if(allSubCompanyIds.size() == 0)
                    dataRanageAllSubCompanyIds.add("");

                iterator = departmentIds.iterator();
                while (iterator.hasNext()) {
                    String ids = "";
                    String v = (String)iterator.next();
                    allDepartmentIds.add(v);
                    if (Integer.parseInt(v) < 0) {
                        ids = departmentVirtualComInfo.getAllSupDepartment(v);
                    }else{
                        ids = departmentComInfo.getAllSupDepartment(v);
                    }
                    String [] idArr = Util.splitString(ids, ",");
                    for(String id : idArr){
                        allDepartmentIds.add(id);
                    }
                }

                iterator = allDepartmentIds.iterator();
                while (iterator.hasNext()) {
                    String v = (String)iterator.next();
                    if(!"".equals(v))
                        dataRanageAllDepartmentIds.add(v);
                }
                if(allDepartmentIds.size() == 0)
                    dataRanageAllDepartmentIds.add("");
            }
        }catch (Exception ex){
            ex.printStackTrace();
            writeLog(ex);
        }
    }

    public void getSubCompanyTreeListByRight(int userId, String rightStr) {
        ManageDetachComInfo ma = new ManageDetachComInfo();
        this.isExecutionDetach = ma.isUseExecutionManageDetach();
        CheckSubCompanyRight newCheck = new CheckSubCompanyRight();
        newCheck.setShowCanceled(false);
        allSubComIds = newCheck.getSubComPathByUserRightId(userId, rightStr, 1);
        subComIds = newCheck.getSubComByUserRightId(userId, rightStr);
        depIds = newCheck.geDeptPathByUserRightId(userId, rightStr, 1);
        if (!this.isExecutionDetach) {
            //未分权
            String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
            int subcompanyID = user.getUserSubCompany1();
            int departmentID = user.getUserDepartment();
            if (rightLevel.equals("2")) {
                // 总部级别的，什么也不返回
            } else if (rightLevel.equals("1")) { // 分部级别的
                String subcompids = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(subcompanyID), String.valueOf(subcompanyID));
                String[] result1 = subcompids.split(",");
                this.allChildSubComIds = result1;
            } else if (rightLevel.equals("0")) { // 部门级别
                String departmentids = "";
                try {
                    departmentids = DepartmentComInfo.getAllChildDepartId(String.valueOf(departmentID), String.valueOf(departmentID));
                } catch (Exception e) {
                    writeLog(e);
                }
                String[] result0 = departmentids.split(",");
                this.allChildDeptIds = result0;
            }
        }
    }
    /**
     * 生成执行力分权未开启时的角色查看分部部门id
     * @param rightStr
     * @return
     */
    private String checkBlogAppLoadSub(String rightStr) {
        String whereClause = "";
        ManageDetachComInfo ma=new ManageDetachComInfo();
        boolean isExecutionDetach = ma.isUseExecutionManageDetach();
        if (!isExecutionDetach) {
            //未分权
            String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
            int subcompanyID = user.getUserSubCompany1();
            int departmentID = user.getUserDepartment();
            if (rightLevel.equals("2")) {
                // 总部级别的，什么也不返回
            } else if (rightLevel.equals("1")) { // 分部级别的
                String subcompids = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(subcompanyID), String.valueOf(subcompanyID));
                whereClause += " and hr.subcompanyid1 in（" + subcompids+")";
            } else if (rightLevel.equals("0")) { // 部门级别
                String departmentids = "0";
                try {
                    departmentids = DepartmentComInfo.getAllChildDepartId(String.valueOf(departmentID), String.valueOf(departmentID));
                } catch (Exception e) {
                    writeLog(e);
                }
                whereClause += " and hr.departmentid in(" + departmentids+")";
            }
        }else{
            //开启管理分权执行力
            CheckSubCompanyRight cscr = new CheckSubCompanyRight();
            String companyid = "";
            int[] companyids = cscr.getSubComByUserRightId(user.getUID(), rightStr, 1);
            for (int i = 0; companyids != null && i < companyids.length; i++) {
                companyid += "," + companyids[i];
            }
            if (companyid.length() > 0) {
                companyid = companyid.substring(1);
                whereClause += " and ( hr.subcompanyid1 in(" + companyid + "))";
            } else {
                whereClause += " and ( hr.subcompanyid1 in(0))";// 分权而且没有选择机构权限
            }

        }
        return whereClause;
    }
    /*
     * 未开启执行力分权，人员组织结构页签，查询控制
     * */
    private boolean checkBlogAppSetting(String type, String rightStr, String id) {
        boolean hasRight = false;
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        if (!this.isExecutionDetach) {
            String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
            switch (rightLevel) {
                case "0":
                    if (type.equals("com")) {
                        if (!hasRight && this.allSubComIds != null && this.allSubComIds.length > 0) {
                            for (int i = 0; i < allSubComIds.length; i++) {
                                if (id.equals(String.valueOf(allSubComIds[i]))) {
                                    hasRight = true;
                                }
                            }
                        }
                    } else {
                        if (this.allChildDeptIds != null && this.allChildDeptIds.length > 0) {
                            for (int i = 0; i < this.allChildDeptIds.length; i++) {
                                if (id.equals(this.allChildDeptIds[i])) {
                                    hasRight = true;
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case "1":
                    if (type.equals("com")) {
                        if (this.allSubComIds != null && this.allSubComIds.length > 0) {
                            for (int i = 0; i < this.allSubComIds.length; i++) {
                                if (id.equals(String.valueOf(this.allSubComIds[i]))) {
                                    hasRight = true;
                                    break;
                                }
                            }
                        }
                        if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
                            for (int i = 0; i < this.allChildSubComIds.length; i++) {
                                if (id.equals(this.allChildSubComIds[i])) {
                                    hasRight = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        String subcompanyid = departmentComInfo.getSubcompanyid1(id);
                        if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
                            for (int i = 0; i < this.allChildSubComIds.length; i++) {
                                if (subcompanyid.equals(this.allChildSubComIds[i])) {
                                    hasRight = true;
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case "2":
                    hasRight = true;
                    break;
                default:
                    hasRight = true;
                    break;
            }
        } else {
            if (type.equals("com")) {
                if (this.allSubComIds != null && this.allSubComIds.length > 0) {
                    for (int i = 0; i < this.allSubComIds.length; i++) {
                        if (id.equals(String.valueOf(this.allSubComIds[i]))) {
                            hasRight = true;
                            break;
                        }
                    }
                }
                if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
                    for (int i = 0; i < this.allChildSubComIds.length; i++) {
                        if (id.equals(String.valueOf(this.allChildSubComIds[i]))) {
                            hasRight = true;
                            break;
                        }
                    }
                }
            } else {
                String subcompanyid = departmentComInfo.getSubcompanyid1(id);
                hasRight = false;
                if (this.depIds != null && this.depIds.length > 0) {
                    for (int i = 0; i < depIds.length; i++) {
                        if (id.equals(String.valueOf(depIds[i]))) {
                            hasRight = true;
                            break;
                        }
                    }
                }
                if (!hasRight && this.subComIds != null && this.subComIds.length > 0) {
                    for (int i = 0; i < subComIds.length; i++) {
                        if (subcompanyid.equals(String.valueOf(subComIds[i]))) {
                            hasRight = true;
                            break;
                        }
                    }
                }
                if (this.allChildDeptIds != null && this.allChildDeptIds.length > 0) {
                    for (int i = 0; i < this.allChildDeptIds.length; i++) {
                        if (id.equals(String.valueOf(this.allChildDeptIds[i]))) {
                            hasRight = true;
                            break;
                        }
                    }
                }
            }
        }
        return hasRight;
    }
    public static void main(String[] args) {
        ResourceBrowserService ResourceBrowserService = new ResourceBrowserService();
        System.out.println("aaa=" + JSONObject.toJSONString(ResourceBrowserService.getMobileShowTemplate(), SerializerFeature.DisableCircularReferenceDetect));
    }

    private Map<String, Object> getv2resourceBySub(Map<String, Object> params) {
        Map<String, Object> jo = new HashMap<>();
        jo.put("status", false);
        jo.put("type", 6);

        HrmOrgForwardBean hrmOrgForwardBean = localHrmOrgForwardBean.get();
        if (hrmOrgForwardBean != null) hrmOrgForwardBean.checkInited(true);

        try {
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            ResourceVirtualComInfo resourceVirtualComInfo = new ResourceVirtualComInfo();
            DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
            CompanyComInfo cci = new CompanyComInfo();
            SubCompanyVirtualComInfo scvc = new SubCompanyVirtualComInfo();
            SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
            AppDetachComInfo adci = new AppDetachComInfo(user);

            if (null == user) {
                jo = getWrongCode(402, jo);
                return jo;
            }
            String id = Util.null2String(params.get("id"));
            String isNoAccount = Util.null2String(params.get("isNoAccount"));
            String sqlwhere = Util.null2String(params.get("sqlwhere"));
            String selectedids = Util.null2String(params.get("selectedids"));
            String virtualtype = Util.null2String(params.get("virtualtype"));
            String type = Util.null2String(params.get("type"));
            String alllevel = Util.null2String(params.get("alllevel"));
            String cmd = Util.null2String(params.get("cmd"));

            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            params.put("tableAlias", "hr");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, 1);
            String dataRanageResourceIds = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataRanageResourceIds.length() > 0) {
                sqlwhere += " " + dataRanageResourceIds;
            }

            List<String> selectList = new ArrayList<String>();
            if (selectedids.length() > 0) {
                String[] tmp_selectedids = selectedids.split(",");
                for (String selectedid : tmp_selectedids) {
                    selectList.add(selectedid);
                }
            }
            String resultString = "";
            TreeNode envelope = new TreeNode();
            if (cmd.equals("getNum")) {
                String nodeids = Util.null2String(params.get("nodeids"));
                resultString = getResourceNumJson(nodeids, selectList, sqlwhere);
                jo.put("status", true);
                jo.put("datas", resultString);
            } else {
                if (id.equals("")) {
                    // 初始化
                    TreeNode root = new TreeNode();
                    String companyname = cci.getCompanyname("1");
                    root.setLastname(companyname);
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        root.setId(virtualtype);
                        root.setNodeid("com_" + virtualtype + "x");
                    } else {
                        root.setNodeid("com_" + 1 + "x");
                        root.setId("0");
                    }

                    root.setOpen("true");
                    root.setTarget("_self");
                    root.setIcon("/images/treeimages/global_wev8.gif");
                    root.setType("com");

                    //添加本分部
                    String subId = String.valueOf(user.getUserSubCompany1());
                    String name = subCompanyComInfo.getSubCompanyname(subId);
                    String canceled = subCompanyComInfo.getCompanyiscanceled(subId);
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        String deptId = resourceVirtualComInfo.getDepartmentid(virtualtype, "" + user.getUID());
                        subId = departmentVirtualComInfo.getSubcompanyid1(deptId);
                        name = scvc.getSubCompanyname(subId);
                        canceled = scvc.getCompanyiscanceled(subId);
                    }
                    boolean canAdd = true;
                    if (subId == null || subId.equals("") || subId.equals("0")) {
                        jo.put("status", true);
                        jo.put("datas", root);
                        return jo;
                    }

                    if (adci.isUseAppDetach()) {
                        if (adci.checkUserAppDetach(subId, "2") == 0) {
                            canAdd = false;
                        }
                    }

                    if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initSubCompany(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                        canAdd = false;
                    }

                    TreeNode subCompanyNode = new TreeNode();
                    subCompanyNode.setLastname(name);
                    subCompanyNode.setId(subId);
                    subCompanyNode.setNodeid("subcom_" + subId + "x");
                    subCompanyNode.setPid("0");
                    subCompanyNode.setIcon("/images/treeimages/Home_wev8.gif");
                    subCompanyNode.setNocheck("N");
                    subCompanyNode.setType("subcom");
                    if (hasChild("subcompany", subId)) {
                        subCompanyNode.setIsParent("true");
                    }
                    if ("1".equals(canceled)) {
                        canAdd = false;
                    }
                    if (canAdd) {
                        root.AddChildren(subCompanyNode);
                    }
                    jo.put("status", true);
                    jo.put("datas", root);
                } else if (type.equals("subcom")) {
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        virtualtype = scvc.getCompanyid(id);
                    }

                    getV2SubCompanyTreeListBySub(envelope, id, virtualtype, selectList, isNoAccount, user, sqlwhere);
                    ArrayList<TreeNode> lsChild = envelope.getChildren();

                    jo.put("status", true);
                    jo.put("datas", lsChild);
                } else if (type.equals("dept")) {
                    String subId = "";
                    if (!"".equals(virtualtype) && !"1".equals(virtualtype)) {
                        DepartmentVirtualComInfo DepartmentComInfo = new DepartmentVirtualComInfo();
                        subId = DepartmentComInfo.getSubcompanyid1(id);
                    } else {
                        DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
                        subId = DepartmentComInfo.getSubcompanyid1(id);
                    }

                    getV2DepartTreeList(envelope, subId, id, selectList, isNoAccount, user, sqlwhere, virtualtype);
                    ArrayList<TreeNode> lsChild = envelope.getChildren();
                    /**
                     * 浏览按钮自定义显示字段
                     */
                    List<String> ids = new ArrayList();
                    for (TreeNode tn : lsChild) {
                        if (tn.getType().equals("resource"))
                            ids.add(tn.getId());
                    }
                    Map<String, Map<String, Object>> map = new HashMap();
                    if (ids.size() > 0) {
                        for (String resourceId : ids) {
                            Map<String, Object> userInfo = getUserInfo(resourceId, user, null);
                            map.put(resourceId, userInfo);
                        }
                    }
                    JSONArray jsonArray = (JSONArray) JSONArray.toJSON(lsChild);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        String k = jsonObject.getString("id");
                        String loopType = jsonObject.getString("type");
                        if (map.containsKey(k) && "resource".equals(loopType)) {
                            jsonObject.putAll(map.get(k));
//                            jsonObject.put("columnControled", true);
                        }
                    }
                    jo.put("status", true);
                    jo.put("datas", jsonArray);
                }
            }

        } catch (Exception e) {
            writeLog(e);
        }
        return jo;
    }

    private TreeNode getV2SubCompanyTreeListBySub(TreeNode companyTreeList, String subId, String virtualtype, List selectedids, String isNoAccount, User user, String sqlwhere) throws Exception {
        getV2DepartTreeList(companyTreeList, subId, "0", selectedids, isNoAccount, user, sqlwhere, virtualtype);
        AppDetachComInfo adci = new AppDetachComInfo(user);
        String alllowsubcompanystr = Util.null2String(adci.getAlllowsubcompanystr());
        String alllowsubcompanyviewstr = Util.null2String(adci.getAlllowsubcompanyviewstr());
        if (null != virtualtype && !"".equals(virtualtype) && !"1".equals(virtualtype)) {
            SubCompanyVirtualComInfo scvc = new SubCompanyVirtualComInfo();
            ResourceVirtualComInfo resourceVirtualComInfo = new ResourceVirtualComInfo();
            String subComId = resourceVirtualComInfo.getSubcompanyid(virtualtype, "" + user.getUID());
            if (subComId == null || subComId.equals("")) {
                return companyTreeList;
            }
            String allShowSubComIds = "";
            try {
                allShowSubComIds = scvc.getSubCompanyTreeStr(subComId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<String> allShowSubComIdList = Util.TokenizerString(allShowSubComIds, ",");
            if (allShowSubComIdList == null) {
                allShowSubComIdList = new ArrayList<String>();
            }
            allShowSubComIdList.add(subComId);
            scvc.setTofirstRow();
            while (scvc.next()) {
                String id = scvc.getSubCompanyid();
                String supsubcomid = scvc.getSupsubcomid();
                String tmp_virtualtype = scvc.getCompanyid();
                if (null != virtualtype && !"".equals(virtualtype) && !"1".equals(virtualtype)) {
                    if (!virtualtype.equals(tmp_virtualtype)) continue;
                }

                if (supsubcomid.equals(""))
                    supsubcomid = "0";
                if (!supsubcomid.equals(subId))
                    continue;

                if (adci.isUseAppDetach()) {
                    if (adci.checkUserAppDetach(id, "2") == 0) {
                        continue;
                    }
                }

                if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initSubCompany(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                    continue;
                }

                if (!allShowSubComIdList.contains(id)) {
                    continue;
                }

                String name = Util.formatMultiLang(scvc.getSubCompanyname());
                String canceled = scvc.getCompanyiscanceled();

                TreeNode subCompanyNode = new TreeNode();
                subCompanyNode.setLastname(name);
                subCompanyNode.setId(id);
                subCompanyNode.setNodeid("subcom_" + id + "x");
                subCompanyNode.setPid(subId);
                subCompanyNode.setIcon("/images/treeimages/Home_wev8.gif");
                subCompanyNode.setNocheck("N");
                subCompanyNode.setType("subcom");
                if (hasChild("subcompany", id)) {
                    subCompanyNode.setIsParent("true");
                }

                if (!"1".equals(canceled)) {
                    companyTreeList.AddChildren(subCompanyNode);
                }
            }
        } else {
            SubCompanyComInfo scvc = new SubCompanyComInfo();
            String subComId = "" + user.getUserSubCompany1();
            String allShowSubComIds = scvc.getAllChildSubcompanyId(subComId, subComId);
            List<String> allShowSubComIdList = Util.TokenizerString(allShowSubComIds, ",");
            if (allShowSubComIdList == null) {
                allShowSubComIdList = new ArrayList<String>();
            }
            allShowSubComIdList.add(subComId);
            scvc.setTofirstRow();
            while (scvc.next()) {
                String id = scvc.getSubCompanyid();
                String supsubcomid = scvc.getSupsubcomid();
                String tmp_virtualtype = scvc.getCompanyid();
                if (null != virtualtype && !"".equals(virtualtype)
                  && !"1".equals(virtualtype)) {
                    if (!virtualtype.equals(tmp_virtualtype))
                        continue;
                }

                if (supsubcomid.equals(""))
                    supsubcomid = "0";
                if (!supsubcomid.equals(subId))
                    continue;

                if (adci.isUseAppDetach()) {
                    if (adci.checkUserAppDetach(id, "2") == 0) {
                        continue;
                    }
                }

                if (!ObjtypeComparor.isSourceHasTarget(localHrmOrgForwardBean.get(), SimpleObjtypeInfo.initSubCompany(id, OrgUpOrDownTypeStyle.ORG_ALL_DOWN))) {
                    continue;
                }

                if (!allShowSubComIdList.contains(id)) {
                    continue;
                }

                String name = Util.formatMultiLang(scvc.getSubCompanyname());
                String canceled = scvc.getCompanyiscanceled();

                TreeNode subCompanyNode = new TreeNode();
                subCompanyNode.setLastname(name);
                subCompanyNode.setId(id);
                subCompanyNode.setNodeid("subcom_" + id + "x");
                subCompanyNode.setPid(subId);
                subCompanyNode.setIcon("/images/treeimages/Home_wev8.gif");
                subCompanyNode.setNocheck("N");
                subCompanyNode.setType("subcom");
                if (hasChild("subcompany", id)) {
                    subCompanyNode.setIsParent("true");
                }

                if (!"1".equals(canceled))
                    companyTreeList.AddChildren(subCompanyNode);
            }
        }

        return companyTreeList;
    }
}
