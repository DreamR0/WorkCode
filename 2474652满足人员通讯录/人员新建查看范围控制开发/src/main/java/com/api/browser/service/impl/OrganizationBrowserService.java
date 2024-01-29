package com.api.browser.service.impl;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.*;
import com.api.browser.service.BrowserService;
import com.api.browser.util.*;
import com.api.customization.qc2474652.Util.HrmVarifyCustom;
import com.engine.common.service.HrmCommonService;
import com.engine.common.service.WorkflowCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.common.service.impl.WorkflowCommonServiceImpl;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.biz.OrganizationShowSetBiz;
import com.engine.hrm.service.impl.BrowserDisplayFieldServiceImpl;
import com.engine.hrm.util.HrmUtil;
import com.google.common.collect.Lists;
import org.eclipse.persistence.jpa.jpql.model.INewValueStateObjectBuilder;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.common.Tools;
import weaver.hrm.company.CompanyComInfo;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.CompanyVirtualComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.companyvirtual.SubCompanyVirtualComInfo;
import weaver.hrm.definedfield.HrmDeptFieldManagerE9;
import weaver.hrm.definedfield.HrmFieldComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import weaver.systeminfo.systemright.OrganizationUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.*;

public class OrganizationBrowserService extends BrowserService {

    private AppDetachComInfo adci = null;
    private boolean isDetachBrower = false;
    private Map<String, Object> params = null;
    private int[] subcomids = null;//必要的上级分部（用于展示完整树）
    private int[] subcomids1 = null;//有权限的机构，不包含必要的构成树的上级
    private ArrayList UserDepts = new ArrayList();//必要的上级部门（用于展示完整树）
    private ArrayList UserDepts1 = new ArrayList(); //有权限的部门，不包含必要的上级部门
    private List<String> dataRanageSubCompanyIds = null;
    private List<String> dataRanageSuperSubCompanyIds = null;
    private List<String> dataRanageDepartmentIds = null;
    private List<String> dataRanageSuperDepartmentIds = null;
    private String dataRanageSqlWhere = null;
    private List<String> dataRanageAllSubCompanyIds = new ArrayList<String>();
    private List<String> dataRanageAllDepartmentIds = new ArrayList<String>();

    private List<Map> browserFieldConfig = new ArrayList<>();
    private Map<String, Integer> fieldLabelMap = new HashMap<>();
    private List<String> browserFieldFilterKeys = new ArrayList<>(Arrays.asList(
//            "departmentname",
            "subcompanyid1",
            "supdepid",
            "supOrgInfo",
//            "subcompanyname",
            "supsubcomid"
    ));

    private String rightStr= null;//权限字符串
    private String[] allChildSubComIds = null;//人员所属分部的下级分部
    private String[] allChildDeptIds = null;//人员所属部门的下级部门
    private boolean isExecutionDetach = false;//是否执行力分权
    @Override
    public Map<String, Object> getBrowserData(Map<String, Object> params) throws Exception {
        //分权单部门--浏览框ID=167
        //分权多部门--浏览框ID=168
        //分权单分部--浏览框ID=169
        //分权多分部--浏览框ID=170
        this.params = params;
        //qc2474652 这里根据指定浏览框进行判断
        if (user.getUID() != 1){
            if ("1".equals(Util.null2String(params.get("custom")))){
                //是否来自指定页面浏览框
                this.browserType = "4";
            }
        }


        if (this.browserType.equals("167") || this.browserType.equals("168")
                || this.browserType.equals("169") || this.browserType.equals("170")) {
            this.isDetachBrower = true;
            this.getSubCompanyTreeListByDecRight();//加载分权数据
        }
        this.rightStr = Util.null2String(params.get("rightStr"));
        if(this.rightStr.equals("Execution:PerformanceSet") || this.rightStr.equals("Execution:ReportSet")){
            ManageDetachComInfo ma=new ManageDetachComInfo();
            this.isExecutionDetach = ma.isUseExecutionManageDetach();
            this.checkBlogAppLoadSub(this.rightStr);
            this.isDetachBrower = true;
            this.getSubCompanyTreeListByDecRight();//加载分权数据
        }

        WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
        Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, Util.getIntValue(this.browserType));
        dataRanageSubCompanyIds = (List<String>) dataRanage.get("subCompanyIds");
        dataRanageSuperSubCompanyIds = (List<String>) dataRanage.get("superSubCompanyIds");
        dataRanageDepartmentIds = (List<String>) dataRanage.get("departmentIds");
        dataRanageSuperDepartmentIds = (List<String>) dataRanage.get("superDeptIds");
        dataRanageSqlWhere = Util.null2String(dataRanage.get("sqlWhere"));
        if (this.dataRanageSubCompanyIds != null && !this.dataRanageSubCompanyIds.isEmpty()) {
            this.dataRanageAllSubCompanyIds.addAll(this.dataRanageSubCompanyIds);
        }
        if (this.dataRanageSuperSubCompanyIds != null && !this.dataRanageSuperSubCompanyIds.isEmpty()) {
            this.dataRanageAllSubCompanyIds.addAll(this.dataRanageSuperSubCompanyIds);
        }
        if (this.dataRanageDepartmentIds != null && !this.dataRanageDepartmentIds.isEmpty()) {
            this.dataRanageAllDepartmentIds.addAll(this.dataRanageDepartmentIds);
        }
        if (this.dataRanageSuperDepartmentIds != null && !this.dataRanageSuperDepartmentIds.isEmpty()) {
            this.dataRanageAllDepartmentIds.addAll(this.dataRanageSuperDepartmentIds);
        }

        List columns = loadDisplayColumnConfig(isDepartmentBrowser() ? 2 : 3);

        String list = Util.null2String(params.get("list"));
        if ("1".equals(list)) {
            return getOrgListData(params);
        }

        String id = Util.null2String(params.get("id"));

        // 虚拟组织
        String virtualCompanyid = Util.null2String(params.get("virtualCompanyid"));
        if(virtualCompanyid.length()==0){
            virtualCompanyid = Util.null2String(params.get("virtualtype"));
        }
        if(virtualCompanyid.equals("1")){
            virtualCompanyid = "";
        }
        // 是否加载部门
        boolean isDepartmentBrowser = this.isDepartmentBrowser();

        if(!"".equals(dataRanageSqlWhere)){
            RecordSet rs = new RecordSet();
            String sql = "select id from " + (isDepartmentBrowser ? "Hrmdepartmentallview" : "HRMSUBCOMPANYALLVIEW") + " where 1 = 1 " + dataRanageSqlWhere;
            rs.executeQuery(sql);
            if(!rs.next()){
                if(isDepartmentBrowser){
                    this.dataRanageSubCompanyIds.add(null);
                    this.dataRanageSuperSubCompanyIds.add(null);
                    this.dataRanageAllSubCompanyIds.add(null);
                    this.dataRanageDepartmentIds.add(null);
                    this.dataRanageSuperDepartmentIds.add(null);
                    this.dataRanageAllDepartmentIds.add(null);
                }else{
                    this.dataRanageSubCompanyIds.add(null);
                    this.dataRanageSuperSubCompanyIds.add(null);
                    this.dataRanageAllSubCompanyIds.add(null);
                }
            }
        }

        new BaseBean().writeLog("==zj==(id)" + JSON.toJSONString(id));

        if ("".equals(id)) {
            Map<String, Object> apidatas = new HashMap<String, Object>();
            // 虚拟组织
            if (virtualCompanyid.length() == 0) {
                virtualCompanyid = Util.null2String(params.get("virtualtype"));
            }
            if (virtualCompanyid.equals("1")) {
                virtualCompanyid = "";
            }
            // 是否加载部门
            // 是否加载所有下级数据
            boolean isLoadAllSub = "1".equals(Util.null2String(params.get("isLoadAllSub")));

            CompanyComInfo companyComInfo = null;
            CompanyVirtualComInfo companyVirtualComInfo = null;
            // 加载顶级分部一级分部
            try {
                companyComInfo = new CompanyComInfo();
                companyVirtualComInfo = new CompanyVirtualComInfo();
                companyVirtualComInfo.setUser(user);
            } catch (Exception e) {
            }
            List<OrgBean> companys = new ArrayList<OrgBean>();

            // 加载虚拟组织列表
            if ("".equals(virtualCompanyid) && companyVirtualComInfo.getCompanyNum() > 0) {
                OrgBean companyInfo = null;
                if (companyComInfo.getCompanyNum() > 0) {
                    companyComInfo.setTofirstRow();
                    while (companyComInfo.next()) {
                        companyInfo = new OrgBean();
                        companyInfo.setCompanyid(companyComInfo.getCompanyid());
                        companyInfo.setName(companyComInfo.getCompanyname());
                        companyInfo.setIsVirtual("0");
                        companys.add(companyInfo);
                    }
                }

                companyVirtualComInfo.setTofirstRow();
                while (companyVirtualComInfo.next()) {
                    if(HrmUtil.isHideDefaultDimension(Util.null2String(companyVirtualComInfo.getCompanyid()))){
                        continue;
                    }

                    companyInfo = new OrgBean();
                    companyInfo.setCompanyid(companyVirtualComInfo.getCompanyid());
                    companyInfo.setName(companyVirtualComInfo.getVirtualType());
                    companyInfo.setIsVirtual("1");
                    companys.add(companyInfo);
                }
            }
            if (isDetachBrower) companys.clear();//分权没有虚拟组织
            apidatas.put("companys", companys);
            String companyname = "";
            if("".equals(virtualCompanyid))
                companyname = companyComInfo.getCompanyname("1");
            else
                companyname = companyVirtualComInfo.getVirtualType(virtualCompanyid);
            OrgBean root = new OrgBean();
            root.setIcon(BrowserTreeNodeIcon.COMPANY_ICON.toString());
            if ("".equals(virtualCompanyid)) {
                new BaseBean().writeLog("==zj==(virtualCompanyid)" + virtualCompanyid);
                root.setId("0");
                root.setCompanyid("1");
                root.setName(companyname);
                root.setType("0");
                root.setIsVirtual("0");
                // 加载下级分部
                loadSubCompanys(root, isDepartmentBrowser, isLoadAllSub, user);
            } else {
                new BaseBean().writeLog("==zj==(root)");
                // 虚拟组织
                root.setId("0");
                root.setCompanyid(virtualCompanyid);
                root.setName(companyname);
                root.setType("0");
                root.setIsVirtual("1");
                loadVirtualSubCompanyInfo(root, isDepartmentBrowser, isLoadAllSub, user);
            }

            apidatas.put(BrowserConstant.BROWSER_RESULT_TYPE, BrowserDataType.TREE_DATA.getTypeid());
            apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, root);


//            String name = isDepartmentBrowser ? "departmentname" : "subcompanyname";
//
//            loadDisplayColumnConfig(1);

//            List<ListHeadBean> tableHeadColumns = new ArrayList<ListHeadBean>();
//            tableHeadColumns.add(new ListHeadBean("id", BoolAttr.TRUE).setIsPrimarykey(BoolAttr.TRUE));
//            tableHeadColumns.add(new ListHeadBean(name, "", 1, BoolAttr.TRUE));
//            tableHeadColumns.add(new ListHeadBean("orgInfo", ""));
//            tableHeadColumns.add(new ListHeadBean("orgWholePathspan", "", 2, BoolAttr.TRUE));
            apidatas.put(BrowserConstant.BROWSER_RESULT_COLUMN, columns);
            new BaseBean().writeLog("==zj==(apidatas)" + JSON.toJSONString(apidatas));
            return apidatas;
        } else {
            return getTreeNodeData(params);
        }
    }

    /**
     * 加载下级分部
     *
     * @param parentOrg
     * @param isDepartmentBrowser
     * @param isLoadAllSub
     * @param user
     */
    private void loadSubCompanys(OrgBean parentOrg, boolean isDepartmentBrowser, boolean isLoadAllSub, User user) {
        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(params.get("fromModule"));
        SubCompanyComInfo rs = null;
        try {
            rs = new SubCompanyComInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        rs.setTofirstRow();

        if (isDepartmentBrowser) {
            // loadsubDepartments
            loadSubDepartments(parentOrg, null, isLoadAllSub);
        }

        List<BrowserTreeNode> subOrgs = null;
        if (parentOrg.getSubs() != null) {
            subOrgs = parentOrg.getSubs();
        } else {
            subOrgs = new ArrayList<BrowserTreeNode>();
        }

        String subcompanyid1 = Util.null2String(params.get("subcompanyid1"));
        String allParentIds = "";
        String allChildIds = "";
        if (subcompanyid1.length() > 0 && !subcompanyid1.equals("0")) {
            try {
                allParentIds = new SubCompanyComInfo().getAllSupCompany(subcompanyid1);
                if (allParentIds.length() > 0) allParentIds += ",";
                allParentIds += subcompanyid1;
            } catch (Exception e) {
                writeLog(e);
            }
        }
        if (subcompanyid1.length() > 0 && !subcompanyid1.equals("0")) {
            String[] subids = subcompanyid1.split(",");
            for (String subid:subids) {
                try {
                    allChildIds += new SubCompanyComInfo().getAllChildSubcompanyId(subid,allChildIds);
                    if (allChildIds.length() > 0) allChildIds += ",";
                    allChildIds += subid;
                } catch (Exception e) {
                    writeLog(e);
                }
            }
        }

        while (rs.next()) {
            String supsubcomid = rs.getSupsubcomid();
            if ("1".equals(rs.getCompanyiscanceled())) {
                continue;
            }
            if (subcompanyid1.length() > 0 && allChildIds.indexOf(Util.null2String(rs.getSubCompanyid()))<0) {
                continue;
            }

            if (supsubcomid.equals(""))
                supsubcomid = "0";
            if (!supsubcomid.equals(parentOrg.getId()))
                continue;

            String id = rs.getSubCompanyid();

            String name = rs.getSubCompanyname();
            String checksubname = Util.null2String(params.get("subcompanyname"));

            //qc2474652 分部显示由建模表配置
            if (user.getUID() != 1) {
                if ("1".equals(Util.null2String(params.get("custom")))) {
                    Boolean isHave = false;
                    HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
                    String[] subCompany = hrmVarifyCustom.getSubCompanyTree(user);
                    for (int i = 0; i < subCompany.length; i++) {
                        if (id.equals(subCompany[i])) {
                            isHave = true;
                            break;
                        }
                    }
                    if (!isHave) {
                        continue;
                    }
                }
            }
            if (!"".equals(checksubname)) {
                if(name.indexOf(checksubname) == -1){
                    continue;
                }
            }

//            if (allParentIds.length() > 0) {
//                if (("," + allParentIds + ",").indexOf("," + id + ",") == -1) {
//                    continue;
//                }
//            }

            OrgBean orgBean = new OrgBean();
            orgBean.setId(id);
            orgBean.setName(name);
            orgBean.setPid(parentOrg.getId());
            orgBean.setType("1");
            orgBean.setIsVirtual("0");
            orgBean.setCanClick(!isDepartmentBrowser);
            orgBean.setIcon(BrowserTreeNodeIcon.SUBCOMPANY_ICON.toString());
            orgBean.setOrgWholePathspan(getOrgSubcomWholePath(id, fromModule));
            if (orgBean.getOrgWholePathspan().equals("")) {
                orgBean.setOrgWholePathspan(name);
            }
            if (!isDepartmentBrowser) {
                orgBean.setShadowInfo(getAllParentsOrg(id, "0"));
            }

            if (this.dataRanageSubCompanyIds != null && this.dataRanageSubCompanyIds.size() > 0||dataRanageSqlWhere.length()>0) {
                boolean canSelect = false;
                if (this.dataRanageSubCompanyIds.contains(id)) {
                    canSelect = true;
                }
                if (!isDepartmentBrowser) {
                    orgBean.setCanClick(canSelect);
                }

                if (this.dataRanageAllSubCompanyIds.contains(id) ) {

                }else{
                    continue;
                }
            }
            //排除没有授权的机构
            if (this.isDetachBrower) {
                boolean canSelect = false;
                orgBean.setCanClick(canSelect);
                for (int i = 0; i < subcomids1.length; i++) {
                    if (id.equals(String.valueOf(subcomids1[i]))) {
                        canSelect = true;
                    }
                    if (!isDepartmentBrowser) {
                        orgBean.setCanClick(canSelect);
                    }
                }
            }

            int rightLevel = this.checkDetach("com", id);
            if (rightLevel==-1) {//检查机构分权和应用分权
                continue;
            }else{
                if(orgBean.getCanClick()){
                    orgBean.setCanClick(rightLevel==2);
                }
            }

//            if (!orgBean.getCanClick()) {
//                validOrgIsParent(orgBean, isDepartmentBrowser, user);//如果无子节点，且只读，不显示
//                if (!orgBean.getIsParent()) {
//                    continue;
//                }
//            }
            if(!isDepartmentBrowser) {
                List<String> displayKeys = new ArrayList<>();
                SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
                for (Map m : browserFieldConfig) {
                    String fieldName = Util.null2String(m.get("fieldName"));
                    generateDisplayKeys(displayKeys, fieldName);
                    String fullPath = Util.null2String(m.get("fullPath"));
                    String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                    if (fullPathDelimiter.equals(""))
                        fullPathDelimiter = "|";
                    else
                        fullPathDelimiter = "" + fullPathDelimiter + "";
                    String orderType = Util.null2String(m.get("orderType"));
                    switch (fieldName) {
                        case "subcompanyname":
                            orgBean.setSubcompanyname(subCompanyComInfo.getSubCompanyname(id));
                            break;
                        case "supsubcomid":
                            orgBean.setSupsubcomid(getSupSubName(id, fullPath, fullPathDelimiter, orderType));
                            break;
                        case "url":
                            orgBean.setUrl(getSubCompanyUrl(id));
                            break;
                        case "subcompanycode":
                            orgBean.setSubcompanycode(getSubCompanyCode(id));
                            break;
                    }
                }
                if(displayKeys.size() > 0) orgBean.setDisplayKeys(displayKeys);
            }
            subOrgs.add(orgBean);

            parentOrg.setIsParent(true);

            // 加载下级分部
            if (isLoadAllSub) {
                loadSubCompanys(orgBean, isDepartmentBrowser, isLoadAllSub, user);
            } else {
                // 更新当前节点isParent状态
                validOrgIsParent(orgBean, isDepartmentBrowser, user);
            }
        }
        parentOrg.setSubs(subOrgs);
    }

    /**
     * 不加载下级数据时，判断当前节点是否有下级分部
     *
     * @param parentOrg
     * @param isDepartmentBrowser
     * @param user
     */
    private void validOrgIsParent(OrgBean parentOrg, boolean isDepartmentBrowser, User user) {
        SubCompanyComInfo rs = null;
        try {
            rs = new SubCompanyComInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        rs.setTofirstRow();

        if (isDepartmentBrowser) {
            validOrgIsParent(parentOrg, null);
        }

        while (rs.next()) {
            String supsubcomid = rs.getSupsubcomid();
            if (supsubcomid.equals(""))
                supsubcomid = "0";
            if (!supsubcomid.equals(parentOrg.getId()))
                continue;

            if ("1".equals(rs.getCompanyiscanceled())) {
                continue;
            }

            String id = rs.getSubCompanyid();
            if (this.checkDetach("com", id)==-1) {//检查机构分权和应用分权
                continue;
            }
            parentOrg.setIsParent(true);
            break;
        }
    }

    /**
     * 不加载下级数据时,且开启加载部门，判断当前节点是否有下级部门
     *
     * @param pSubCompany
     * @param pDepartment
     */
    private void validOrgIsParent(OrgBean pSubCompany, OrgBean pDepartment) {
        DepartmentComInfo rsDepartment = null;
        try {
            rsDepartment = new DepartmentComInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        rsDepartment.setTofirstRow();
        String pdepartmentId = pDepartment == null ? "0" : pDepartment.getId();
        String subcompanyid = pSubCompany.getId();
        if ("".equals(subcompanyid)) {
            subcompanyid = rsDepartment.getSubcompanyid1(pdepartmentId);
        }

        while (rsDepartment.next()) {
            String supdepid = rsDepartment.getDepartmentsupdepid();
            if (pdepartmentId.equals("0") && supdepid.equals("")) {
                supdepid = "0";
            }
            if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(pdepartmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && pdepartmentId
                    .equals("0")))))
                continue;

            if ("1".equals(rsDepartment.getDeparmentcanceled())) {
                continue;
            }

            String id = rsDepartment.getDepartmentid();
            if (this.checkDetach("dept", id)==-1) {
                continue;
            }
            pSubCompany.setIsParent(true);
            if (pDepartment != null) {
                pDepartment.setIsParent(true);
            }
            break;
        }
    }

    /**
     * 加载下级部门
     *
     * @param pSubCompany
     * @param pDepartment
     * @param isLoadAllSub
     */
    private void loadSubDepartments(OrgBean pSubCompany, OrgBean pDepartment, boolean isLoadAllSub) {
        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(params.get("fromModule"));
        DepartmentComInfo rsDepartment = null;
        try {
            rsDepartment = new DepartmentComInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String pdepartmentId = pDepartment == null ? "0" : pDepartment.getId();
        String subcompanyid = Util.null2String(pSubCompany.getId());
        if ("".equals(subcompanyid)) {
            subcompanyid = rsDepartment.getSubcompanyid1(pdepartmentId);
        }
        String pId = pDepartment == null ? pSubCompany.getId() : pDepartment.getId();

        List<BrowserTreeNode> subOrgs = null;
        if (pDepartment == null && pSubCompany.getSubs() != null) {
            subOrgs = pSubCompany.getSubs();
        } else {
            subOrgs = new ArrayList<BrowserTreeNode>();
        }
        rsDepartment.setTofirstRow();
        while (rsDepartment.next()) {
            List<String> displayKeys = new ArrayList<>();
            String supdepid = rsDepartment.getDepartmentsupdepid();
            if ("1".equals(rsDepartment.getDeparmentcanceled())) {
                continue;
            }
            if (pdepartmentId.equals("0") && supdepid.equals("")) {
                supdepid = "0";
            }
            if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(pdepartmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && pdepartmentId
                    .equals("0")))))
                continue;

            String id = rsDepartment.getDepartmentid();
            String name = rsDepartment.getDepartmentmark();
            String subcompanyid1 = rsDepartment.getSubcompanyid1();
            //qc2474652 这里把非建模表配置的部门过滤掉
            if (user.getUID() != 1){
                if ("1".equals(Util.null2String(params.get("custom")))){
                    HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
                    Boolean isDep = false;
                    ArrayList<String> depIds = hrmVarifyCustom.getDepartment(user);
                    for (int i = 0; i < depIds.size(); i++) {
                        if (id.equals(depIds.get(i))) {
                            isDep = true;
                            break;
                        }
                    }

                    if (!isDep) {
                        continue;
                    }
                }
            }



            //执行力平台，未开启管理分权，部门级别的，只能查看本部门以及下级部门，不能查看分部下所有部门
            if (!this.checkExecAppSetting(rightStr, id)) {
                continue;
            }
            OrgBean orgBean = new OrgBean();
            orgBean.setId(id);
            orgBean.setPid(pId);
            orgBean.setName(name);
            orgBean.setType("2");
            orgBean.setIsVirtual("0");
            orgBean.setCanClick(true);
            orgBean.setShadowInfo(getAllParentsOrg(id, "1"));
            orgBean.setPsubcompanyid(pSubCompany.getId());
            orgBean.setIcon(BrowserTreeNodeIcon.DEPARTMENT_ICON.toString());
            orgBean.setOrgWholePathspan(getOrgDeptWholePath(id, fromModule));
            if (orgBean.getOrgWholePathspan().equals("")) {
                orgBean.setOrgWholePathspan(name);
            }

            pSubCompany.setIsParent(true);
            if (pDepartment != null) {
                pDepartment.setIsParent(true);
            }

            if(this.dataRanageSqlWhere.length()>0){
                //受控
                boolean canselect = false;
                if (this.dataRanageDepartmentIds.contains(id)) {
                    canselect = true;
                }else if(this.dataRanageSubCompanyIds != null && this.dataRanageSubCompanyIds.size() > 0 && this.dataRanageSubCompanyIds.contains(subcompanyid1)) {
                    canselect = true;
                }
                orgBean.setCanClick(canselect);

                if(this.dataRanageAllDepartmentIds.contains(id)){

                }else if(this.dataRanageAllSubCompanyIds.contains(subcompanyid1)){
                    if(!canselect){
                        continue;
                    }
                }else{
                    continue;
                }
            }

          /*  HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
            UserDepts1 = hrmVarifyCustom.getDepartment(user, UserDepts1);
            new BaseBean().writeLog("==zj==(UserDepts1和id)" + JSON.toJSONString(UserDepts1) +  "     |   " + id);*/
            if (this.isDetachBrower) {//分权浏览框
                boolean canselect = false;
                if ((UserDepts1.size() == 0 || UserDepts.contains(id))) {
                    if (UserDepts1.size() == 0 || UserDepts1.contains(id)) {
                        canselect = true;
                    }
                } else {
                    continue;
                }
                orgBean.setCanClick(canselect);
            }

            if (!orgBean.getCanClick()) {
                validOrgIsParent(pSubCompany, orgBean);//如果无子节点，且只读，不显示
                if (!orgBean.getIsParent()) {
                    continue;
                }
            }

            int rightLevel = this.checkDetach("dept", id);
            if (rightLevel==-1) {//检查机构分权和应用分权
                continue;
            }else{
                if(orgBean.getCanClick()){
                    orgBean.setCanClick(rightLevel==2);
                }
            }
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            for (Map m : browserFieldConfig) {
                String fieldName = Util.null2String(m.get("fieldName"));
                generateDisplayKeys(displayKeys, fieldName);
                String fullPath = Util.null2String(m.get("fullPath"));
                String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                if (fullPathDelimiter.equals(""))
                    fullPathDelimiter = "|";
                else
                    fullPathDelimiter = "" + fullPathDelimiter + "";
                String orderType = Util.null2String(m.get("orderType"));
                switch (fieldName) {
                    case "departmentname":
                        orgBean.setDepartmentname(departmentComInfo.getDepartmentmark(id));
                        break;
                    case "subcompanyid1":
                        orgBean.setSubcompanyid1(getSubComNameByDept(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "supdepid":
                        orgBean.setSupdepid(getSupDeptName(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "departmentcode":
                        orgBean.setDepartmentcode(getDeptCode(id));
                        break;
                    case "supOrgInfo":
                        String supOrgInfo = "";
                        try{
                            supOrgInfo = getSupOrgInfo(id, fullPathDelimiter, orderType);
                        }catch (Exception ex){
                            writeLog(ex);
                            ex.printStackTrace();
                        }
                        orgBean.setSupOrgInfo(supOrgInfo);
                        break;
                }
            }
            if(displayKeys.size() > 0) orgBean.setDisplayKeys(displayKeys);



            subOrgs.add(orgBean);

            if (isLoadAllSub) {
                loadSubDepartments(pSubCompany, orgBean, isLoadAllSub);
            } else {
                validOrgIsParent(pSubCompany, orgBean);
            }

            //qc2474652 这里如果是最底层部门，不需要再加载子部门
            if (user.getUID() != 1){
                if ("1".equals(Util.null2String(params.get("custom")))){
                    HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
                    String departments = hrmVarifyCustom.getDepartment(user, false);
                    if (departments.contains(id)){
                        //判断是否有子部门
                        if (hrmVarifyCustom.isParent(user,id,true)){
                            orgBean.setIsParent(true);
                        }else {
                            orgBean.setIsParent(false);
                        }
                    }
                }
            }
        }

        if (pDepartment == null) {
            pSubCompany.setSubs(subOrgs);
        } else {
            pDepartment.setSubs(subOrgs);
        }
    }

    /**
     * 加载虚拟分部
     *
     * @param parentOrg
     * @param isDepartmentBrowser
     * @param isLoadAllSub
     * @param user
     */
    private void loadVirtualSubCompanyInfo(OrgBean parentOrg, boolean isDepartmentBrowser, boolean isLoadAllSub, User user) {
        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(params.get("fromModule"));
        SubCompanyVirtualComInfo rs = null;
        try {
            rs = new SubCompanyVirtualComInfo();
            if ("".equals(Util.null2String(parentOrg.getCompanyid()))) {
                parentOrg.setCompanyid(rs.getCompanyid(parentOrg.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        rs.setTofirstRow();

        // 加载虚拟部门
        if (isDepartmentBrowser) {
            loadVirtualSubDepartments(parentOrg, null, isLoadAllSub);
        }

        List<BrowserTreeNode> subOrgs = null;
        if (parentOrg.getSubs() != null) {
            subOrgs = parentOrg.getSubs();
        } else {
            subOrgs = new ArrayList<BrowserTreeNode>();
        }

        while (rs.next()) {
            String id = rs.getSubCompanyid();
            String supsubcomid = rs.getSupsubcomid();
            String comid = rs.getCompanyid();

            if (this.checkDetach("com", id)==-1) {//检查机构分权和应用分权
                continue;
            }

            if ("1".equals(rs.getCompanyiscanceled())) {
                continue;
            }
            if (!comid.equals(parentOrg.getCompanyid()))
                continue;
            if (supsubcomid.equals(""))
                supsubcomid = "0";
            if (!supsubcomid.equals(parentOrg.getId()))
                continue;

            String name = rs.getSubCompanyname();
            String checksubname = Util.null2String(params.get("subcompanyname"));
            if (!"".equals(checksubname)) {
                if(name.indexOf(checksubname) == -1){
                    continue;
                }
            }
            OrgBean virtualOrgBean = new OrgBean();
            virtualOrgBean.setId(id);
            virtualOrgBean.setPid(parentOrg.getId());
            virtualOrgBean.setName(name);
            virtualOrgBean.setCompanyid(parentOrg.getCompanyid());
            virtualOrgBean.setType("1");
            virtualOrgBean.setIsVirtual("1");
            virtualOrgBean.setCanClick(!isDepartmentBrowser);
            virtualOrgBean.setIcon(BrowserTreeNodeIcon.SUBCOMPANY_ICON.toString());
            virtualOrgBean.setOrgWholePathspan(getOrgSubcomWholePath(id, fromModule));
            if (virtualOrgBean.getOrgWholePathspan().equals("")) {
                virtualOrgBean.setOrgWholePathspan(name);
            }
            if (!isDepartmentBrowser) {
                virtualOrgBean.setShadowInfo(getAllParentsOrg(id, "0+" + parentOrg.getCompanyid()));
            }
            parentOrg.setIsParent(true);

            if (this.dataRanageSubCompanyIds != null && this.dataRanageSubCompanyIds.size() > 0 && this.dataRanageSubCompanyIds.contains(id)) {
                if (!isDepartmentBrowser) {
                    virtualOrgBean.setCanClick(true);
                }
                if(dataRanageAllSubCompanyIds.contains(id)){

                }else{
                    continue;
                }
            }
            if(!isDepartmentBrowser) {
                List<String> displayKeys = new ArrayList<>();
                SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
                for (Map m : browserFieldConfig) {
                    String fieldName = Util.null2String(m.get("fieldName"));
                    generateDisplayKeys(displayKeys, fieldName);
                    String fullPath = Util.null2String(m.get("fullPath"));
                    String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                    if (fullPathDelimiter.equals(""))
                        fullPathDelimiter = "|";
                    else
                        fullPathDelimiter = "" + fullPathDelimiter + "";
                    String orderType = Util.null2String(m.get("orderType"));
                    switch (fieldName) {
                        case "subcompanyname":
                            virtualOrgBean.setSubcompanyname(subCompanyComInfo.getSubCompanyname(id));
                            break;
                        case "supsubcomid":
                            virtualOrgBean.setSupsubcomid(getSupSubName(id, fullPath, fullPathDelimiter, orderType));
                            break;
                        case "url":
                            virtualOrgBean.setUrl(getSubCompanyUrl(id));
                            break;
                        case "subcompanycode":
                            virtualOrgBean.setSubcompanycode(getSubCompanyCode(id));
                            break;
                    }
                }
                if(displayKeys.size() > 0) virtualOrgBean.setDisplayKeys(displayKeys);
            }
            subOrgs.add(virtualOrgBean);

            if (isLoadAllSub) {
                loadVirtualSubCompanyInfo(virtualOrgBean, isDepartmentBrowser, isLoadAllSub, user);
            } else {
                validVirtualOrgIsParent(virtualOrgBean, isDepartmentBrowser, user);
            }
        }
        parentOrg.setSubs(subOrgs);
    }

    /**
     * 加载虚拟部门
     *
     * @param pVCompany
     * @param pVDepartment
     * @param isLoadAllSub
     */
    private void loadVirtualSubDepartments(OrgBean pVCompany, OrgBean pVDepartment, boolean isLoadAllSub) {
        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(params.get("fromModule"));
        DepartmentVirtualComInfo rsDepartment = null;
        try {
            rsDepartment = new DepartmentVirtualComInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String departmentId = pVDepartment == null ? "0" : pVDepartment.getId();
        String subcompanyid = Util.null2String(pVCompany.getId());
        if ("".equals(subcompanyid)) {
            subcompanyid = rsDepartment.getSubcompanyid1(departmentId);
        }

        String pId = pVDepartment == null ? pVCompany.getId() : pVDepartment.getId();
        List<BrowserTreeNode> subOrgs = null;
        if (pVDepartment == null && pVCompany.getSubs() != null) {
            subOrgs = pVCompany.getSubs();
        } else {
            subOrgs = new ArrayList<BrowserTreeNode>();
        }
        rsDepartment.setTofirstRow();
        while (rsDepartment.next()) {
            if ("1".equals(rsDepartment.getDeparmentcanceled())) {
                continue;
            }
            if (departmentId.equals(rsDepartment.getDepartmentid()))
                continue;
            String supdepid = rsDepartment.getDepartmentsupdepid();
            if (departmentId.equals("0") && supdepid.equals(""))
                supdepid = "0";
            if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(departmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && departmentId.equals("0")))))
                continue;

            String id = rsDepartment.getDepartmentid();
            String name = rsDepartment.getDepartmentmark();

            if (this.checkDetach("dept", id)==-1) {//检查机构分权和应用分权
                continue;
            }

            OrgBean virtualOrgBean = new OrgBean();
            virtualOrgBean.setId(id);
            virtualOrgBean.setPid(pId);
            virtualOrgBean.setName(name);
            virtualOrgBean.setType("2");
            virtualOrgBean.setIsVirtual("1");
            virtualOrgBean.setCanClick(true);
            virtualOrgBean.setPsubcompanyid(pVCompany.getId());
            virtualOrgBean.setShadowInfo(getAllParentsOrg(id, "0+" + pVCompany.getCompanyid()));
            virtualOrgBean.setIcon(BrowserTreeNodeIcon.DEPARTMENT_ICON.toString());
            virtualOrgBean.setOrgWholePathspan(getOrgDeptWholePath(id, fromModule));
            if (virtualOrgBean.getOrgWholePathspan().equals("")) {
                virtualOrgBean.setOrgWholePathspan(name);
            }
            pVCompany.setIsParent(true);
            if (pVDepartment != null) {
                pVDepartment.setIsParent(true);
            }
            if (this.dataRanageDepartmentIds != null && this.dataRanageDepartmentIds.size() > 0) {
                boolean canselect = false;
                if (this.dataRanageDepartmentIds.size() == 0 || this.dataRanageDepartmentIds.contains(id)) {
                    canselect = true;
                }
                virtualOrgBean.setCanClick(canselect);
            }else if (this.dataRanageSubCompanyIds != null && this.dataRanageSubCompanyIds.size() > 0 && !virtualOrgBean.getCanClick()) {
                boolean canselect = false;
                if (this.dataRanageSubCompanyIds.contains(subcompanyid)) {
                    canselect = true;
                }
                virtualOrgBean.setCanClick(canselect);
            }

            if((this.dataRanageAllDepartmentIds != null && this.dataRanageAllDepartmentIds.size() > 0)||
              (this.dataRanageAllSubCompanyIds != null && this.dataRanageAllSubCompanyIds.size() > 0)) {
                if (this.dataRanageAllDepartmentIds.contains(id) || this.dataRanageAllSubCompanyIds.contains(subcompanyid)) {

                } else {
                    continue;
                }
            }
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            List<String> displayKeys = new ArrayList<>();
            for (Map m : browserFieldConfig) {
                String fieldName = Util.null2String(m.get("fieldName"));
                generateDisplayKeys(displayKeys, fieldName);
                String fullPath = Util.null2String(m.get("fullPath"));
                String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                if (fullPathDelimiter.equals(""))
                    fullPathDelimiter = "|";
                else
                    fullPathDelimiter = "" + fullPathDelimiter + "";
                String orderType = Util.null2String(m.get("orderType"));
                switch (fieldName) {
                    case "departmentname":
                        virtualOrgBean.setDepartmentname(departmentComInfo.getDepartmentmark(id));
                        break;
                    case "subcompanyid1":
                        virtualOrgBean.setSubcompanyid1(getSubComNameByDept(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "supdepid":
                        virtualOrgBean.setSupdepid(getSupDeptName(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "departmentcode":
                        virtualOrgBean.setDepartmentcode(getDeptCode(id));
                        break;
                    case "supOrgInfo":
                        String supOrgInfo = "";
                        try{
                            supOrgInfo = departmentComInfo.getAllParentDepartmentBlankNames(id, departmentComInfo.getSubcompanyid1(id), fullPathDelimiter, orderType, false);
                        }catch (Exception ex){
                            writeLog(ex);
                            ex.printStackTrace();
                        }
                        virtualOrgBean.setSupOrgInfo(supOrgInfo);
                        break;
                }
            }
            if(displayKeys.size() > 0) virtualOrgBean.setDisplayKeys(displayKeys);
            subOrgs.add(virtualOrgBean);

            if (isLoadAllSub) {
                loadVirtualSubDepartments(pVCompany, virtualOrgBean, isLoadAllSub);
            } else {
                validVirtualOrgIsParent(pVCompany, virtualOrgBean);
            }
        }

        if (pVDepartment == null) {
            pVCompany.setSubs(subOrgs);
        } else {
            pVDepartment.setSubs(subOrgs);
        }
    }

    private void generateDisplayKeys(List<String> list, String key){
        if(key.equals("departmentname") || key.equals("subcompanyname") || !browserFieldFilterKeys.contains(key))
            list.add(key);
    }

    /**
     * @param parentOrg
     * @param isDepartmentBrowser
     * @param user
     */
    private void validVirtualOrgIsParent(OrgBean parentOrg, boolean isDepartmentBrowser, User user) {
        SubCompanyVirtualComInfo rs = null;
        try {
            rs = new SubCompanyVirtualComInfo();
            if ("".equals(Util.null2String(parentOrg.getCompanyid()))) {
                parentOrg.setCompanyid(rs.getCompanyid(parentOrg.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        rs.setTofirstRow();

        // 加载虚拟部门
        if (isDepartmentBrowser) {
            validVirtualOrgIsParent(parentOrg, null);
        }
        while (rs.next()) {
            String id = rs.getSubCompanyid();
            String supsubcomid = rs.getSupsubcomid();
            String comid = rs.getCompanyid();

            if (!comid.equals(parentOrg.getCompanyid()))
                continue;
            if (supsubcomid.equals(""))
                supsubcomid = "0";
            if (!supsubcomid.equals(parentOrg.getId()))
                continue;

            if (this.checkDetach("com", id)==-1) {//检查机构分权和应用分权
                continue;
            }
            parentOrg.setIsParent(true);
            break;
        }
    }

    /**
     * @param pVCompany
     * @param pVDepartment
     */
    public void validVirtualOrgIsParent(OrgBean pVCompany, OrgBean pVDepartment) {
        DepartmentVirtualComInfo rsDepartment = null;
        try {
            rsDepartment = new DepartmentVirtualComInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        rsDepartment.setTofirstRow();

        String departmentId = pVDepartment == null ? "0" : pVDepartment.getId();
        String subcompanyid = pVCompany.getId();

        if ("".equals(subcompanyid)) {
            subcompanyid = rsDepartment.getSubcompanyid1(departmentId);
        }

        while (rsDepartment.next()) {
            if (departmentId.equals(rsDepartment.getDepartmentid()))
                continue;
            String supdepid = rsDepartment.getDepartmentsupdepid();
            if (departmentId.equals("0") && supdepid.equals(""))
                supdepid = "0";
            if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(departmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && departmentId.equals("0")))))
                continue;

            String id = rsDepartment.getDepartmentid();
            if (this.checkDetach("dept", id)==-1) {
                continue;
            }

            pVCompany.setIsParent(true);
            if (pVDepartment != null) {
                pVDepartment.setIsParent(true);
            }

            break;
        }

    }

    /***
     * 获取下级机构
     * @param params
     * @return
     */
    public Map<String, Object> getTreeNodeData(Map<String, Object> params) {
        Map<String, Object> apidatas = new HashMap<String, Object>();

        String type = Util.null2String(params.get("type"));
        String id = Util.null2String(params.get("id"));
        String psubcompanyid = Util.null2String(params.get("psubcompanyid"));
        String isVirtual = Util.null2String(params.get("isVirtual"));
        // 是否加载部门
        boolean isDepartmentBrowser = this.isDepartmentBrowser();
        OrgBean psubOrgBean = new OrgBean();
        List<BrowserTreeNode> result = null;
        if ("0".equals(type)) {
            String companyid = Util.null2String(params.get("companyid"));
            psubOrgBean.setId(id);
            psubOrgBean.setCompanyid(companyid);

            if ("0".equals(isVirtual)) {
                loadSubCompanys(psubOrgBean, isDepartmentBrowser, false, user);
            } else {
                loadVirtualSubCompanyInfo(psubOrgBean, isDepartmentBrowser, false, user);
            }
            result = psubOrgBean.getSubs();
        } else if ("1".equals(type)) {
            psubOrgBean.setId(id);
            psubOrgBean.setType(type);
            psubOrgBean.setIsVirtual(isVirtual);
            if ("0".equals(isVirtual)) {
                loadSubCompanys(psubOrgBean, isDepartmentBrowser, false, user);
            } else {
                loadVirtualSubCompanyInfo(psubOrgBean, isDepartmentBrowser, false, user);
            }
            result = psubOrgBean.getSubs();
        } else if ("2".equals(type)) {
            psubOrgBean.setId(psubcompanyid);

            OrgBean pdepOrgBean = new OrgBean();
            pdepOrgBean.setId(id);
            pdepOrgBean.setIsVirtual(isVirtual);
            if ("0".equals(isVirtual)) {
                loadSubDepartments(psubOrgBean, pdepOrgBean, false);
            } else {
                loadVirtualSubDepartments(psubOrgBean, pdepOrgBean, false);
            }
            result = pdepOrgBean.getSubs();
        }
        apidatas.put(BrowserConstant.BROWSER_RESULT_TYPE, BrowserDataType.TREE_DATA.getTypeid());
        apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, result);
        return apidatas;
    }

    //按列表显示数据
    private Map<String, Object> getOrgListData(Map<String, Object> params) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        boolean isDepartmentBrowser = this.isDepartmentBrowser();//"1".equals(Util.null2String(params.get("isDepartmentBrowser")));
        String virtualCompanyid = Util.null2String(params.get("virtualCompanyid"));
        if (virtualCompanyid.length() == 0) {
            virtualCompanyid = Util.null2String(params.get("virtualtype"));
        }
        if (virtualCompanyid.equals("1")) {
            virtualCompanyid = "";
        }
        String backfields = "";
        String fromSql = "";
        String sqlwhere = " where 1 = 1 ";
        String orderby = "";
        String sqlprimarykey = "id";
        List<SplitTableColBean> cols = new ArrayList<SplitTableColBean>();
        cols.add(new SplitTableColBean("true", "id"));
        String otherpara = (isDepartmentBrowser ? "1" : "0") + "+" + virtualCompanyid;

        //组织字段显示层级设置（部分模块的组织字段需要显示全路径）
        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(params.get("fromModule"));

        if (isDepartmentBrowser) {
//            loadDisplayColumnConfig(2);
            backfields = "id,departmentmark as departmentname,id as orgInfo,id as orgWholePath";
            for (int i = 1; i < browserFieldConfig.size(); i++) {
                Map m = browserFieldConfig.get(i);
                String fieldName = Util.null2String(m.get("fieldName"));
                if(!"departmentname".equals(fieldName))
                    backfields += ",id as " + fieldName;
            }
            if ("".equals(virtualCompanyid)) {
                fromSql = "HrmDepartment";
                orderby = "subcompanyid1, HrmDepartment.supdepid, showorder, departmentname";
            } else {
                fromSql = "HrmDepartmentVirtual";
                orderby = "showorder,id";
            }

            String departmentname = Util.null2String(params.get("departmentname"));
            String subcompanyid1 = Util.null2String(params.get("subcompanyid1"));
            String departmentcode = Util.null2String(params.get("departmentcode"));
            String supdepid = Util.null2String(params.get("supdepid"));

            //封存不显示
            sqlwhere += " and (canceled is null or canceled ='' or canceled ='0' )";

            if (!"".equals(virtualCompanyid)) {
                sqlwhere += " and virtualtype =" + virtualCompanyid;
            }

            if (!"".equals(departmentname)) {
                sqlwhere += " and departmentname like '%" + departmentname + "%'";
            }

            if (!"".equals(subcompanyid1)) {
                sqlwhere += " and subcompanyid1 = " + subcompanyid1;
            }

            if (!"".equals(departmentcode)) {
                sqlwhere += " and departmentcode like '%" + departmentcode + "%'";
            }

            if (!"".equals(supdepid)) {
                sqlwhere += " and supdepid = " + supdepid;
            }

            if ("".equals(virtualCompanyid)) {
                String canselectids = "";
//				for(int i=0; this.dataRanageDepartmentIds!=null && i<this.dataRanageDepartmentIds.size(); i++){
//					if(canselectids.length()>0)canselectids+=",";
//					canselectids+=this.dataRanageDepartmentIds.get(i);
//				}
//				if(!"".equals(canselectids)){
//					sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "id")+")" ;
//				}
//
//				canselectids = "";
//				for(int i=0; this.dataRanageSubCompanyIds!=null && i<this.dataRanageSubCompanyIds.size(); i++){
//					if(canselectids.length()>0)canselectids+=",";
//					canselectids+=this.dataRanageSubCompanyIds.get(i);
//				}
//				if(!"".equals(canselectids)){
//					sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "subcompanyid1")+")" ;
//				}

                if (Util.null2String(this.dataRanageSqlWhere).length() > 0) {
                    sqlwhere += this.dataRanageSqlWhere;
                }

                if (this.isDetachBrower) {
                    canselectids = "";
                    for (int i = 0; i < this.UserDepts1.size(); i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.UserDepts1.get(i);
                    }
                    if (!"".equals(canselectids)) {
                        sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "id") + ")";
                    }

                    canselectids = "";
                    for (int i = 0; i < this.subcomids1.length; i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.subcomids1[i];
                    }
                    if (!"".equals(canselectids)) {
                        sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "subcompanyid1") + ")";
                    }

                    if (this.UserDepts1.size() == 0 && this.subcomids1.length == 0) {
                        sqlwhere += " and 1=2 ";
                    }

                    boolean isUseHrmManageDetach = new ManageDetachComInfo().isUseHrmManageDetach();
                    if (!isUseHrmManageDetach) {
                        String rightStr = Util.null2String(this.params.get("rightStr"));
                        if (rightStr.length() > 0) {
                            String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                            int departmentID = user.getUserDepartment();
                            int subcompanyID = user.getUserSubCompany1();
                            if (rightLevel.equals("2")) {
                                //总部级别的，什么也不返回
                            } else if (rightLevel.equals("1")) { //分部级别的
                                sqlwhere += " and subcompanyid1=" + subcompanyID;
                            } else if (rightLevel.equals("0")) { //部门级别
                                sqlwhere += " and id=" + departmentID;
                            }
                        }
                    }
                } else {
                    // 检查应用分权
                    if (adci == null) {
                        adci = new AppDetachComInfo(user);
                    }
                    if(!adci.isNotNeedToDoAppDetach()) {
                        String ids = Util.null2String(adci.getAlllowdepartmentstr());
                        String subcompanyids = Util.null2String(adci.getAlllowsubcompanystr());
//                    String viewids = Util.null2String(adci.getAlllowdepartmentviewstr());
//                    if (viewids.length() > 0) {
//                        if (ids.length() > 0) ids += ",";
//                        ids += viewids;
//                    }
                        String tempWhere = "";
                        if (subcompanyids.length() > 0) {
                            tempWhere += Util.getSubINClause(subcompanyids, "subcompanyid1", "in");
                        }
                        if (ids.length() > 0) {
                            if (tempWhere.length() > 0) {
                                tempWhere += " or ";
                            }
                            tempWhere += Util.getSubINClause(ids, "id", "in");
                        }
                        if (tempWhere.length() > 0) {
                            sqlwhere += " and (" + tempWhere + ")";
                        }else{
                            if(adci.isNotCheckUserAppDetach()){

                            }else {
                                sqlwhere += " and 1=2 ";
                            }
                        }
                    }
                }
            }else{
                if(Util.null2String(this.dataRanageSqlWhere).length()>0){
                    sqlwhere += this.dataRanageSqlWhere;
                }
            }

            getDeptFields(5, 7);

            String deptInfoPara = "";
            for (Map m : browserFieldConfig) {
                String fieldName = Util.null2String(m.get("fieldName"));
                String fullPath = Util.null2String(m.get("fullPath"));
                String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                String orderType = Util.null2String(m.get("orderType"));

                if (fullPathDelimiter.equals(""))
                    fullPathDelimiter = "|";
                SplitTableColBean tableColBean = null;
                deptInfoPara = "column:id+" +
                        fieldName + "+" +
                        fullPath + "+" +
                        fullPathDelimiter + "+" +
                        orderType + "+" + otherpara;
                switch (fieldName) {
                    case "departmentname":
                        cols.add(new SplitTableColBean("40%", SystemEnv.getHtmlLabelName(124, user.getLanguage()), "departmentname", "departmentname", 1).setIsInputCol(BoolAttr.TRUE));
                        break;
                    default:
                        cols.add(new SplitTableColBean("60%", SystemEnv.getHtmlLabelName(fieldLabelMap.get(fieldName), user.getLanguage()), fieldName, null, "com.api.browser.service.impl.OrganizationBrowserService.getDeptInfo", deptInfoPara));
                        break;
                }
            }
            SplitTableColBean col = new SplitTableColBean("true", "orgInfo");
            col.setTransmethod("com.api.browser.service.impl.OrganizationBrowserService.getAllParentsOrg");
            col.setOtherpara(otherpara);
            col.setShowType(2);
            col.setTransMethodForce("true");
            cols.add(col);

            col = new SplitTableColBean("true", "orgWholePath");
            col.setTransmethod("com.api.browser.service.impl.OrganizationBrowserService.getOrgDeptWholePath");
            col.setOtherpara(fromModule);
            col.setShowType(2);
            col.setTransMethodForce("true");
            col.setIsInputCol(BoolAttr.TRUE);
            cols.add(col);
//			cols.add(new SplitTableColBean("40%", SystemEnv.getHtmlLabelName(124 , user.getLanguage()), "departmentname", "departmentname",1).setIsInputCol(BoolAttr.TRUE));
//			cols.add(new SplitTableColBean("60%", SystemEnv.getHtmlLabelName(15772 , user.getLanguage()), "orgInfo", null, "com.api.browser.service.impl.OrganizationBrowserService.getAllParentsOrg",otherpara));
//			cols.add(new SplitTableColBean("0%", SystemEnv.getHtmlLabelName(506187 , user.getLanguage()), "orgWholePath", null, "com.api.browser.service.impl.OrganizationBrowserService.getOrgDeptWholePath",fromModule,2));
        } else {
//            loadDisplayColumnConfig(3);
            backfields = "id,subcompanyname, id as orgInfo,id as orgWholePath";
            for (int i = 1; i < browserFieldConfig.size(); i++) {
                Map m = browserFieldConfig.get(i);
                String fieldName = Util.null2String(m.get("fieldName"));
                if(!"subcompanyname".equals(fieldName))
                    backfields += ",id as " + fieldName;
            }
            if ("".equals(virtualCompanyid)) {
                fromSql = "hrmsubcompany";
                orderby = "supsubcomid, showorder,subcompanyname";
            } else {
                orderby = "supsubcomid,showorder,subcompanyname";
                fromSql = "HrmSubCompanyVirtual";
            }

            String subcompanyname = Util.null2String(params.get("subcompanyname"));
            String supsubcomid = Util.null2String(params.get("supsubcomid"));
            //封存不显示
            sqlwhere += " and (canceled is null or canceled ='' or canceled ='0' )";

            if (!"".equals(virtualCompanyid)) {
                sqlwhere += " and virtualtypeid =" + virtualCompanyid;
            }

            if (!"".equals(subcompanyname)) {
                sqlwhere += " and subcompanyname like '%" + subcompanyname + "%'";
            }
            if (!"".equals(supsubcomid)) {
                sqlwhere += " and supsubcomid = " + supsubcomid;
            }

            if ("".equals(virtualCompanyid)) {
                String canselectids = "";
//				for(int i=0; this.dataRanageSubCompanyIds!=null && i<this.dataRanageSubCompanyIds.size(); i++){
//					if(canselectids.length()>0)canselectids+=",";
//					canselectids+=this.dataRanageSubCompanyIds.get(i);
//				}
//				if(!"".equals(canselectids)){
//					sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "id") +")";
//				}
                if (Util.null2String(this.dataRanageSqlWhere).length() > 0) {
                    sqlwhere += this.dataRanageSqlWhere;
                }
                if (this.isDetachBrower) {
                    canselectids = "";
                    for (int i = 0; i < this.subcomids1.length; i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.subcomids1[i];
                    }
                    if (!"".equals(canselectids)) {
                        sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "id") + ")";
                    } else {
                        sqlwhere += " and 1=2 ";
                    }
                } else {
                    // 检查应用分权
                    if ("".equals(virtualCompanyid)) {
                        if (adci == null) {
                            adci = new AppDetachComInfo(user);
                        }
                        if(!adci.isNotNeedToDoAppDetach()) {
                            String ids = Util.null2String(adci.getAlllowsubcompanystr());
//                        String viewids = Util.null2String(adci.getAlllowsubcompanyviewstr());
//                        if (viewids.length() > 0) {
//                            if (ids.length() > 0) ids += ",";
//                            ids += viewids;
//                        }
                            if (ids.length() > 0) {
                                sqlwhere += " and (" + Util.getSubINClause(ids, "id", "in") + ")";
                            } else {
                                if(adci.isNotCheckUserAppDetach()){

                                }else {
                                    sqlwhere += " and 1=2 ";
                                }
                            }
                        }
                    }
                }
            }else{
                if (Util.null2String(this.dataRanageSqlWhere).length() > 0) {
                    sqlwhere += this.dataRanageSqlWhere;
                }
            }

            getDeptFields(4, 6);
            String deptInfoPara = "";
            for (Map m : browserFieldConfig) {
                String fieldName = Util.null2String(m.get("fieldName"));
                String fullPath = Util.null2String(m.get("fullPath"));
                String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                String orderType = Util.null2String(m.get("orderType"));

                if (fullPathDelimiter.equals(""))
                    fullPathDelimiter = "|";
                deptInfoPara = "column:id+" +
                        fieldName + "+" +
                        fullPath + "+" +
                        fullPathDelimiter + "+" +
                        orderType + "+" + otherpara;
                switch (fieldName) {
                    case "subcompanyname":
                        cols.add(new SplitTableColBean("40%", SystemEnv.getHtmlLabelName(141, user.getLanguage()), "subcompanyname", "subcompanyname", 1).setIsInputCol(BoolAttr.TRUE));
                        break;
                    default:
                        cols.add(new SplitTableColBean("60%", SystemEnv.getHtmlLabelName(fieldLabelMap.get(fieldName), user.getLanguage()), fieldName, null, "com.api.browser.service.impl.OrganizationBrowserService.getSubcompanyInfo", deptInfoPara));
                        break;
                }
            }

            SplitTableColBean col = new SplitTableColBean("true", "orgInfo");
            col.setTransmethod("com.api.browser.service.impl.OrganizationBrowserService.getAllParentsOrg");
            col.setOtherpara(otherpara);
            col.setShowType(2);
            col.setTransMethodForce("true");
            cols.add(col);

            col = new SplitTableColBean("true", "orgWholePath");
            col.setTransmethod("com.api.browser.service.impl.OrganizationBrowserService.getOrgSubcomWholePath");
            col.setOtherpara(fromModule);
            col.setShowType(2);
            col.setTransMethodForce("true");
            col.setIsInputCol(BoolAttr.TRUE);
            cols.add(col);
//			cols.add(new SplitTableColBean("40%", SystemEnv.getHtmlLabelName(141 , user.getLanguage()), "subcompanyname", "subcompanyname",1).setIsInputCol(BoolAttr.TRUE));
//			cols.add(new SplitTableColBean("60%", SystemEnv.getHtmlLabelName(22753 , user.getLanguage()), "orgInfo", null, "com.api.browser.service.impl.OrganizationBrowserService.getAllParentsOrg",otherpara));
//			cols.add(new SplitTableColBean("0%", SystemEnv.getHtmlLabelName(506187 , user.getLanguage()), "orgWholePath", null, "com.api.browser.service.impl.OrganizationBrowserService.getOrgSubcomWholePath",fromModule,2));
        }
        //qc2474652 这里树形列表在过滤一下
        if (user.getUID() != 1 && "1".equals(Util.null2String(params.get("custom")))){
            HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
            String department = hrmVarifyCustom.getDepartment(user, false);
            String customWhere = " and id in("+department+") ";
            sqlwhere += customWhere;
        }
        apidatas.putAll(SplitTableUtil.makeListDataResult(new SplitTableBean(backfields, fromSql, sqlwhere, orderby, sqlprimarykey,"ASC", cols)));
        return apidatas;
    }

    public String getAllParentsOrg(String id, String para) {
        String[] paraArr = para.split("\\+");
        boolean isDept = "1".equals(paraArr[0]);
        boolean isVirtual = !"".equals(paraArr.length > 1 ? Util.null2String(paraArr[1]) : "");
        String pname = getPName("", id, isDept, isVirtual);
        if (isVirtual) {
            CompanyVirtualComInfo companyVirtualComInfo = new CompanyVirtualComInfo();
            pname = ("".equals(pname) ? "" : pname + "/") + companyVirtualComInfo.getVirtualType(Util.null2String(paraArr[1]));
        } else {
            CompanyComInfo companyComInfo = new CompanyComInfo();
            pname = ("".equals(pname) ? "" : pname + "/") + companyComInfo.getCompanyname("1");
        }
        return pname;
    }


    public String getPName(String pname, String id, boolean isDept, boolean isVirtual) {
        if ("0".equals(id) || "".equals(id)) return pname;
        String _pname = "";
        String _pid = "";
        if (isDept) {
            if (isVirtual) {
                DepartmentVirtualComInfo deptVirComInfo = new DepartmentVirtualComInfo();
                _pname = deptVirComInfo.getDepartmentmark(id);
                isDept = !"0".equals(Util.null2s(deptVirComInfo.getDepartmentsupdepid(id), "0"));
                _pid = isDept ? deptVirComInfo.getDepartmentsupdepid(id) : deptVirComInfo.getSubcompanyid1(id);
            } else {
                DepartmentComInfo deptComInfo = new DepartmentComInfo();
                _pname = deptComInfo.getDepartmentmark(id);
                isDept = !"0".equals(Util.null2s(deptComInfo.getDepartmentsupdepid(id), "0"));
                _pid = isDept ? deptComInfo.getDepartmentsupdepid(id) : deptComInfo.getSubcompanyid1(id);
            }
        } else {
            if (isVirtual) {
                SubCompanyVirtualComInfo subVirComInfo = new SubCompanyVirtualComInfo();
                _pname = subVirComInfo.getSubCompanyname(id);
                _pid = Util.null2String(subVirComInfo.getSupsubcomid(id));
            } else {
                SubCompanyComInfo subComInfo = new SubCompanyComInfo();
                _pname = subComInfo.getSubCompanyname(id);
                _pid = Util.null2String(subComInfo.getSupsubcomid(id));
            }
        }
        pname = ("".equals(pname) ? "" : pname + "/") + _pname;
        return getPName(pname, _pid, isDept, isVirtual);
    }

    private String getDepartmentName(String name, String deptId, String fullPath, String fullPathDelimiter, String orderType) {
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();

        String str = "";
        try {
            boolean virtualFlag = Integer.parseInt(deptId) < 0;
            if (virtualFlag) {
                if (fullPath.equals("1")) {
                    String supSubIds = departmentVirtualComInfo.getAllSupdepid(deptId);
                    String[] ids = supSubIds.split("\\,");
                    int i = 0;
                    for (String id : ids) {
                        if (i == 0)
                            str += departmentVirtualComInfo.getDepartmentmark(id);
                        else
                            str += fullPathDelimiter + departmentVirtualComInfo.getDepartmentmark(id);
                        i++;
                    }
                } else
                    str = departmentVirtualComInfo.getDepartmentmark(deptId);
            } else {
                if (fullPath.equals("1")) {
                    str = departmentComInfo.getDepartmentRealPath(deptId, fullPathDelimiter, orderType);
                } else
                    str = name;
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getDepartmentName(String name, String deptId, String fullPath, String fullPathDelimiter, String orderType, String virtualCompanyid, boolean includeSelf) {
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();

        String str = "";
        try {
            boolean virtualFlag = Integer.parseInt(deptId) < 0;
            if (virtualFlag) {
                if (fullPath.equals("1")) {
                    String supSubIds = departmentVirtualComInfo.getAllSupdepid(deptId);
                    String[] ids = supSubIds.split("\\,");
                    int i = 0;
                    for (String id : ids) {
                        if(!includeSelf && id.equals(deptId))
                            continue;
                        if (i == 0)
                            str += departmentVirtualComInfo.getDepartmentmark(id);
                        else
                            str += fullPathDelimiter + departmentVirtualComInfo.getDepartmentmark(id);
                        i++;
                    }
                } else
                    str = departmentVirtualComInfo.getDepartmentmark(deptId);
            } else {
                if (fullPath.equals("1")) {
                    str = departmentComInfo.getDepartmentRealPath(deptId, fullPathDelimiter, orderType, includeSelf);
                } else
                    str = name;
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getSubComNameByDept(String deptId, String fullPath, String fullPathDelimiter, String orderType) {
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();

        String str = "";
        try {
            String subComponyId = "";
            boolean virtualFlag = Integer.parseInt(deptId) < 0;
            if (virtualFlag) {
                subComponyId = departmentVirtualComInfo.getSubcompanyid1(deptId);
                if (fullPath.equals("1")) {
                    String supSubIds = subComponyId + "," + subCompanyVirtualComInfo.getAllSupCompany(subComponyId);
                    String[] ids = supSubIds.split("\\,");
                    List<String> list = new ArrayList<String>(Arrays.asList(ids));
                    if ("0".equals(orderType))
                        Collections.reverse(list);
                    int i = 0;
                    for (String id : list) {
                        if (i == 0)
                            str += subCompanyVirtualComInfo.getSubCompanyname(id);
                        else
                            str += fullPathDelimiter + subCompanyVirtualComInfo.getSubCompanyname(id);
                        i++;
                    }
                } else
                    str = subCompanyVirtualComInfo.getSubCompanyname(subComponyId);
            } else {
                subComponyId = departmentComInfo.getSubcompanyid1(deptId);
                if (fullPath.equals("1")) {
                    str = subCompanyComInfo.getSubcompanyRealPath(subComponyId, fullPathDelimiter, orderType);
                } else
                    str = subCompanyComInfo.getSubCompanyname(subComponyId);
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getSupDeptName(String deptId, String fullPath, String fullPathDelimiter, String orderType) {
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();

        String str = "";
        try {
            String departmentId = "";
            boolean virtualFlag = Integer.parseInt(deptId) < 0;
            if (virtualFlag) {
                departmentId = departmentVirtualComInfo.getDepartmentsupdepid(deptId);
                if (fullPath.equals("1")) {
                    String supSubIds = departmentVirtualComInfo.getAllSupDepartment(deptId);
                    String[] ids = supSubIds.split("\\,");
                    List<String> list = new ArrayList<>(Arrays.asList(ids));
                    if ("0".equals(orderType))
                        Collections.reverse(list);
                    int i = 0;
                    for (String id : list) {
                        if (i == 0)
                            str += departmentVirtualComInfo.getDepartmentmark(id);
                        else
                            str += fullPathDelimiter + departmentVirtualComInfo.getDepartmentmark(id);
                        i++;
                    }
                } else
                    str = departmentVirtualComInfo.getDepartmentmark(departmentId);
            } else {
                departmentId = departmentComInfo.getDepartmentsupdepid(deptId);
                if (fullPath.equals("1")) {
                    str = departmentComInfo.getDepartmentRealPath(departmentId, fullPathDelimiter, orderType);
                } else
                    str = departmentComInfo.getDepartmentmark(departmentId);
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getDeptCode(String deptId) {
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();

        String str = "";
        try {
            boolean virtualFlag = Integer.parseInt(deptId) < 0;
            if (virtualFlag) {
                str = departmentVirtualComInfo.getDepartmentCode(deptId);
            } else {
                str = departmentComInfo.getDepartmentCode(deptId);
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    public String getDeptInfo(String id, String para) {
        String[] paraArr = para.split("\\+");
        int paraLen = paraArr.length;
        String deptId = paraArr[0];
        String fieldName = paraArr[1];
        String fullPath = paraArr[2];
        String fullPathDelimiter = paraArr[3];
        String orderType = paraArr[4];
        String virtualCompanyid = "";
        if (paraLen > 6) virtualCompanyid = paraArr[6];

        String str = "";

        switch (fieldName) {
            case "departmentname":
                str = getDepartmentName(id, deptId, fullPath, fullPathDelimiter, orderType);
                break;
            case "subcompanyid1":
                str = getSubComNameByDept(deptId, fullPath, fullPathDelimiter, orderType);
                break;
            case "supdepid":
                str = getSupDeptName(deptId, fullPath, fullPathDelimiter, orderType);
                break;
            case "departmentcode":
                str = getDeptCode(deptId);
                break;
            case "supOrgInfo":
                str = getSupOrgInfo(id, fullPathDelimiter, orderType);
                break;
        }

        return str;
    }

    private String getSupOrgInfo(String deptId, String fullPathDelimiter, String orderType){
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
        CompanyComInfo companyComInfo = new CompanyComInfo();
        CompanyVirtualComInfo companyVirtualComInfo = new CompanyVirtualComInfo();

        String str = "";
        String subCompanyId = "";
        try {
            boolean virtualFlag = Integer.parseInt(deptId) < 0;
            String pname = "";
            if (virtualFlag) {
                String virtualCompanyId = departmentVirtualComInfo.getVirtualtype(deptId);
                pname = companyVirtualComInfo.getVirtualType(virtualCompanyId);
                subCompanyId = departmentVirtualComInfo.getSubcompanyid1(deptId);
                str = departmentVirtualComInfo.getAllParentDepartmentBlankNames(deptId, subCompanyId, fullPathDelimiter, orderType, false);
//                if("".equals(orderType) || "0".equals(orderType)){
//                    str = pname + fullPathDelimiter + str;
//                }else{
//                    str += fullPathDelimiter + pname;
//                }
            } else {
                pname = companyComInfo.getCompanyname("1");
                subCompanyId = departmentComInfo.getSubcompanyid1(deptId);
                str = departmentComInfo.getAllParentDepartmentBlankNames(deptId, subCompanyId, fullPathDelimiter, orderType, false);
//                if("".equals(orderType) || "0".equals(orderType)){
//                    str = pname + fullPathDelimiter + str;
//                }else{
//                    str += fullPathDelimiter + pname;
//                }
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getSubCompanyName(String name, String subComId, String fullPath, String fullPathDelimiter, String orderType) {
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();

        String str = "";
        try {
            boolean virtualFlag = Integer.parseInt(subComId) < 0;
            if (virtualFlag) {
                if (fullPath.equals("1")) {
                    String supSubIds = subComId + "," + subCompanyVirtualComInfo.getAllSupCompany(subComId);
                    String[] ids = supSubIds.split("\\,");
                    int i = 0;
                    for (String id : ids) {
                        if (i == 0)
                            str += subCompanyVirtualComInfo.getSubCompanyname(id);
                        else
                            str += fullPathDelimiter + subCompanyVirtualComInfo.getSubCompanyname(id);
                        i++;
                    }
                } else
                    str = name;
            } else {
                if (fullPath.equals("1")) {
                    str = subCompanyComInfo.getSubcompanyRealPath(subComId, fullPathDelimiter, orderType);
                } else
                    str = name;
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getSupSubName(String subComId, String fullPath, String fullPathDelimiter, String orderType) {
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();

        String str = "";
        try {
            String subComponyId = "";
            boolean virtualFlag = Integer.parseInt(subComId) < 0;
            if (virtualFlag) {
                subComponyId = subCompanyVirtualComInfo.getSupsubcomid(subComId);
                if (fullPath.equals("1")) {
                    String supSubIds = subCompanyVirtualComInfo.getAllSupCompany(subComId);
                    String[] ids = supSubIds.split("\\,");
                    List<String> list = new ArrayList<>(Arrays.asList(ids));
                    if ("0".equals(orderType))
                        Collections.reverse(list);
                    int i = 0;
                    for (String id : list) {
                        if (i == 0)
                            str += subCompanyVirtualComInfo.getSubCompanyname(id);
                        else
                            str += fullPathDelimiter + subCompanyVirtualComInfo.getSubCompanyname(id);
                        i++;
                    }
                } else
                    str = subCompanyVirtualComInfo.getSubCompanyname(subComponyId);
            } else {
                subComponyId = subCompanyComInfo.getSupsubcomid(subComId);
                if (fullPath.equals("1")) {
                    str = subCompanyComInfo.getSubcompanyRealPath(subComponyId, fullPathDelimiter, orderType);
                } else
                    str = subCompanyComInfo.getSubCompanyname(subComponyId);
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getSubCompanyUrl(String subComId) {
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();

        String str = "";
        try {
            boolean virtualFlag = Integer.parseInt(subComId) < 0;
            if (virtualFlag) {
                str = subCompanyComInfo.getUrl(subComId);
            } else {
                str = subCompanyComInfo.getUrl(subComId);
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    private String getSubCompanyCode(String subComId) {
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();

        String str = "";
        try {
            boolean virtualFlag = Integer.parseInt(subComId) < 0;
            if (virtualFlag) {
                str = subCompanyComInfo.getSubCompanyCode(subComId);
            } else {
                str = subCompanyComInfo.getSubCompanyCode(subComId);
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return str;
    }

    public String getSubcompanyInfo(String id, String para) {
        String[] paraArr = para.split("\\+");
        int paraLen = paraArr.length;
        String subComId = paraArr[0];
        String fieldName = paraArr[1];
        String fullPath = paraArr[2];
        String fullPathDelimiter = paraArr[3];
        String orderType = paraArr[4];
        String virtualCompanyid = "";
        if (paraLen > 6) virtualCompanyid = paraArr[6];

        String str = "";

        switch (fieldName) {
            case "subcompanyname":
                str = getSubCompanyName(id, subComId, fullPath, fullPathDelimiter, orderType);
                break;
            case "supsubcomid":
                str = getSupSubName(subComId, fullPath, fullPathDelimiter, orderType);
                break;
            case "url":
                str = getSubCompanyUrl(subComId);
                break;
            case "subcompanycode":
                str = getSubCompanyCode(subComId);
                break;
        }

        return str;
    }

    @Override
    public Map<String, Object> getBrowserConditionInfo(Map<String, Object> params) throws Exception {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        List<SearchConditionItem> conditions = new ArrayList<SearchConditionItem>();
        apidatas.put(BrowserConstant.BROWSER_RESULT_CONDITIONS, conditions);
        ConditionFactory conditionFactory = new ConditionFactory(user);
        boolean  isFormmode=Util.null2String(params.get("isFormmode")).equals("1");//qc:958065

        // 是否显示虚拟维度-客户维度
        boolean showCustomerDimension = Util.null2String(this.getPropValue("hrm_virtual_dimension", "showCustomerDimension")).equals("1");
        // 是否显示虚拟维度-公文维度
        boolean showDocumentDimension = Util.null2String(this.getPropValue("hrm_virtual_dimension", "showDocumentDimension")).equals("1");

        //部门
        if ("4".equals(browserType) || "57".equals(browserType) || "167".equals(browserType) || "168".equals(browserType)) {
            conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 15390, "departmentname", true));
            conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 141, "subcompanyid1", "164"));
            conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 15391, "departmentcode"));
            conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 15772, "supdepid", "4"));
        } else if(isFormmode&&"169".equals(browserType)){
            conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 1878, "subcompanyname", true));
        } else{
            conditions.add(conditionFactory.createCondition(ConditionType.INPUT, 1878, "subcompanyname", true));
            conditions.add(conditionFactory.createCondition(ConditionType.BROWSER, 22753, "supsubcomid", "164"));
        }

        boolean hideVirtualOrg = Util.null2String(params.get("hideVirtualOrg")).equals("true");
        if (!hideVirtualOrg) {
            //组织维度
            CompanyComInfo companyComInfo = new CompanyComInfo();
            CompanyVirtualComInfo companyVirtualComInfo = new CompanyVirtualComInfo();
            List<SearchConditionOption> comOptions = new ArrayList<SearchConditionOption>();
            if (companyComInfo.getCompanyNum() > 0) {
                companyComInfo.setTofirstRow();
                while (companyComInfo.next()) {
                    comOptions.add(new SearchConditionOption(companyComInfo.getCompanyid(), companyComInfo.getCompanyname()));
                }
            }
            companyVirtualComInfo.setTofirstRow();
            while (companyVirtualComInfo.next()) {
                if ("1".equals(companyVirtualComInfo.getCancel())){
                    continue;
                }
                // -20001=公文交换维度  -10000=客户维度
                String companyid = Util.null2String(companyVirtualComInfo.getCompanyid());
                if("-20001".equals(companyid) && !showDocumentDimension){
                    continue;
                }else if("-10000".equals(companyid) && !showCustomerDimension){
                    continue;
                }

                comOptions.add(new SearchConditionOption(companyVirtualComInfo.getCompanyid(), companyVirtualComInfo.getVirtualType()));
            }
            conditions.add(conditionFactory.createCondition(ConditionType.SELECT, 34069, "virtualCompanyid", comOptions));
        }
        return apidatas;
    }

    @Override
    public Map<String, Object> getMultBrowserDestData(Map<String, Object> params) throws Exception {
        this.params = params;
        if (this.browserType.equals("167") || this.browserType.equals("168")
                || this.browserType.equals("169") || this.browserType.equals("170")) {
            this.isDetachBrower = true;
            this.getSubCompanyTreeListByDecRight();//加载分权数据
        }

        WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
        Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, Util.getIntValue(this.browserType));
        dataRanageSubCompanyIds = (List<String>) dataRanage.get("subCompanyIds");
        dataRanageSuperSubCompanyIds = (List<String>) dataRanage.get("superSubCompanyIds");
        dataRanageDepartmentIds = (List<String>) dataRanage.get("departmentIds");
        dataRanageSuperDepartmentIds = (List<String>) dataRanage.get("superDeptIds");
        dataRanageSqlWhere = Util.null2String(dataRanage.get("sqlWhere"));

        if (this.dataRanageSubCompanyIds != null && !this.dataRanageSubCompanyIds.isEmpty()) {
            this.dataRanageAllSubCompanyIds.addAll(this.dataRanageSubCompanyIds);
        }
        if (this.dataRanageSuperSubCompanyIds != null && !this.dataRanageSuperSubCompanyIds.isEmpty()) {
            this.dataRanageAllSubCompanyIds.addAll(this.dataRanageSuperSubCompanyIds);
        }
        if (this.dataRanageDepartmentIds != null && !this.dataRanageDepartmentIds.isEmpty()) {
            this.dataRanageAllDepartmentIds.addAll(this.dataRanageDepartmentIds);
        }
        if (this.dataRanageSuperDepartmentIds != null && !this.dataRanageSuperDepartmentIds.isEmpty()) {
            this.dataRanageAllDepartmentIds.addAll(this.dataRanageSuperDepartmentIds);
        }

        Map<String, Object> apidatas = new HashMap<String, Object>();
        SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();
        DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        //组织字段显示层级设置（部分模块的组织字段需要显示全路径）
        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(params.get("fromModule"));
        String alllevel = Util.null2String(params.get("alllevel"));

        String selectids = Util.null2String(params.get(BrowserConstant.BROWSER_MULT_DEST_SELECTIDS));//选中节点的id
        String types = Util.null2String(params.get("types"));//选中节点的类型

        //虚拟组织id，用于sql查询
        String virtualCompany = Util.null2String(params.get("virtualCompanyid"));
        //兼容原有虚拟组织逻辑
        String virtualCompanyid = virtualCompany;
        if (virtualCompanyid.equals("1")) {
            virtualCompanyid = "";
        }

        //是否是根节点全选
        boolean selectedAll = false;
        //是否是部门浏览按钮
        boolean isDepartmentBrowser = this.isDepartmentBrowser();
        //数据源
        List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
        //组织字段参数
        String otherpara = (isDepartmentBrowser ? "1" : "0") + "+" + virtualCompanyid;

        //浏览按钮显示列定义
        List<ListHeadBean> displayColumns = new ArrayList<ListHeadBean>();
        RecordSet rs = new RecordSet();
        String backFields = "";
        String sqlFrom = "";
        String sqlWhere = " 1 = 1 and (canceled is null or canceled = '' or canceled = '0') ";
        if(rs.getDBType().equalsIgnoreCase("postgresql"))
            sqlWhere = " 1 = 1 and (canceled is null  or canceled = '0') ";
        String sqlOrderBy = " order by showorder ";
        String name = "";//TODO

        List<String> allSubcompanyids = new ArrayList<>();
        List<String> allChildren = new ArrayList<>();
        String idCondition = "";

        if (isDepartmentBrowser) {
            //加载部门浏览按钮显示字段定义
            displayColumns = loadDisplayColumnConfig(2);

            name = "departmentname";
            backFields = " id, departmentname, id as otherParam ";
            sqlFrom = " hrmdepartmentallview ";
            List<String> list = Util.splitString2List(types, ",");
            if (list.contains("subCom|0"))//如果包含id为0的分部，即选中了根节点
                selectedAll = true;
            if (!selectedAll) {
                if(!"".equals(types)) {
                    for (String type : list) {
                        List<String> kv = Util.splitString2List(type, "|");
                        String k = kv.get(0);
                        String v = kv.get(1);
                        if ("0".equals(v))//遇到根节点跳过
                            continue;
                        boolean virtualFlag = Integer.parseInt(v) < 0;
                        switch (k) {
                            case "subcom":
                                allSubcompanyids.add(v);
                                if ("1".equals(alllevel)) {
                                    List<String> subids =
                                            !virtualFlag
                                                    ? Util.splitString2List(subCompanyComInfo.getAllChildSubcompanyId(v, ""), ",")
                                                    : Util.splitString2List(subCompanyVirtualComInfo.getAllChildSubcompanyId(v, ""), ",");
                                    allSubcompanyids.addAll(subids);
                                }
                                break;
                            case "dept":
                                allChildren.add(v);
                                if ("1".equals(alllevel)) {
                                    List<String> deptids =
                                            !virtualFlag
                                                    ? Util.splitString2List(departmentComInfo.getAllChildDepartId(v, ""), ",")
                                                    : Util.splitString2List(departmentVirtualComInfo.getAllChildDepartId(v, ""), ",");
                                    allChildren.addAll(deptids);
                                }
                                break;
                        }
                    }
                }else{
                    List<String> arr = Util.splitString2List(selectids, ",");
                    for(String id : arr){
                        allChildren.add(id);
                    }
                }
                if(allChildren.size() > 0)
                    idCondition = Util.getSubINClause(formatIds(allChildren), "id", "IN");
                if(allSubcompanyids.size() > 0){
                    if(!"".equals(idCondition))
                        idCondition += " or " + Util.getSubINClause(formatIds(allSubcompanyids), "subcompanyid1", "IN");
                    else
                        idCondition = Util.getSubINClause(formatIds(allSubcompanyids), "subcompanyid1", "IN");
                }
                if(!"".equals(idCondition))
                    sqlWhere += " and (" + idCondition + ") ";
            }else{
                sqlWhere += " and VIRTUALTYPE = " + virtualCompany;
            }
            if ("".equals(virtualCompanyid)) {
                String canselectids = "";

                if (Util.null2String(this.dataRanageSqlWhere).length() > 0) {
                    sqlWhere += this.dataRanageSqlWhere;
                }

                if (this.isDetachBrower) {
                    canselectids = "";
                    for (int i = 0; i < this.UserDepts1.size(); i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.UserDepts1.get(i);
                    }
                    if (!"".equals(canselectids)) {
                        sqlWhere += " and (" + Tools.getOracleSQLIn(canselectids, "id") + ")";
                    }

                    canselectids = "";
                    for (int i = 0; i < this.subcomids1.length; i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.subcomids1[i];
                    }
                    if (!"".equals(canselectids)) {
                        sqlWhere += " and (" + Tools.getOracleSQLIn(canselectids, "subcompanyid1") + ")";
                    }

                    if (this.UserDepts1.size() == 0 && this.subcomids1.length == 0) {
                        sqlWhere += " and 1=2 ";
                    }

                    boolean isUseHrmManageDetach = new ManageDetachComInfo().isUseHrmManageDetach();
                    if (!isUseHrmManageDetach) {
                        String rightStr = Util.null2String(this.params.get("rightStr"));
                        if (rightStr.length() > 0) {
                            String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                            int departmentID = user.getUserDepartment();
                            int subcompanyID = user.getUserSubCompany1();
                            if (rightLevel.equals("2")) {
                                //总部级别的，什么也不返回
                            } else if (rightLevel.equals("1")) { //分部级别的
                                sqlWhere += " and subcompanyid1=" + subcompanyID;
                            } else if (rightLevel.equals("0")) { //部门级别
                                sqlWhere += " and id=" + departmentID;
                            }
                        }
                    }
                } else {
                    // 检查应用分权
                    if (adci == null) adci = new AppDetachComInfo(user);
                    String ids = Util.null2String(adci.getAlllowdepartmentstr());
                    String subcompanyids = Util.null2String(adci.getAlllowsubcompanystr());
                    String viewids = Util.null2String(adci.getAlllowdepartmentviewstr());
                    if (viewids.length() > 0) {
                        if (ids.length() > 0) ids += ",";
                        ids += viewids;
                    }
                    String tempWhere = "";
                    if (subcompanyids.length() > 0) {
                        tempWhere += Util.getSubINClause(subcompanyids, "subcompanyid1", "in");
                    }
                    if (ids.length() > 0) {
                        if (tempWhere.length() > 0) {
                            tempWhere += " or ";
                        }
                        tempWhere += Util.getSubINClause(ids, "id", "in");
                    }
                    if (tempWhere.length() > 0) {
                        sqlWhere += " and (" + tempWhere + ")";
                    }
                }
            }else{
                sqlWhere += " and virtualtype = " + virtualCompanyid;
            }
        } else {
            //加载分部浏览按钮显示字段定义
            displayColumns = loadDisplayColumnConfig(3);

            name = "subcompanyname";
            backFields = " id, subcompanyname, id as otherParam ";
            sqlFrom = " hrmsubcompanyallview ";

            List<String> ids = Util.splitString2List(selectids, ",");
            for (String id : ids) {
            	  if(Util.null2String(id).length()==0){
                    continue;
                }
                if("0".equals(id))//遇到根节点跳过
                    continue;
                allSubcompanyids.add(id);
                boolean virtualFlag = Integer.parseInt(id) < 0;
                if("1".equals(alllevel)){
                    List<String> subids =
                        !virtualFlag
                        ? Util.splitString2List(subCompanyComInfo.getAllChildSubcompanyId(id, ""), ",")
                        : Util.splitString2List(subCompanyVirtualComInfo.getAllChildSubcompanyId(id, ""), ",");
                    allSubcompanyids.addAll(subids);
                }
            }
            if(allSubcompanyids.size() > 0)
                sqlWhere += " and (" + Util.getSubINClause(formatIds(allSubcompanyids), "id", "IN") + ") ";
            if ("".equals(virtualCompanyid)) {
                String canselectids = "";
                if (Util.null2String(this.dataRanageSqlWhere).length() > 0) {
                    sqlWhere += this.dataRanageSqlWhere;
                }
                if (this.isDetachBrower) {
                    canselectids = "";
                    for (int i = 0; i < this.subcomids1.length; i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.subcomids1[i];
                    }
                    if (!"".equals(canselectids)) {
                        sqlWhere += " and (" + Tools.getOracleSQLIn(canselectids, "id") + ")";
                    } else {
                        sqlWhere += " and 1=2 ";
                    }
                } else {
                    // 检查应用分权
                    if ("".equals(virtualCompanyid)) {
                        if (adci == null) adci = new AppDetachComInfo(user);
                        String ids1 = Util.null2String(adci.getAlllowsubcompanystr());
                        String viewids = Util.null2String(adci.getAlllowsubcompanyviewstr());
                        if (viewids.length() > 0) {
                            if (ids1.length() > 0) ids1 += ",";
                            ids1 += viewids;
                        }
                        if (ids1.length() > 0) {
                            sqlWhere += " and (" + Util.getSubINClause(ids1, "id", "in") + ")";
                        }
                    }
                }
            }else {
                sqlWhere += " and virtualtypeid = " + virtualCompanyid;
            }
        }


        String querySql = "select" + backFields + "from" + sqlFrom + "where" + sqlWhere +sqlOrderBy;

        rs.executeSql(querySql);
        while (rs.next()) {
            Map<String, Object> item = new HashMap<String, Object>();
            datas.add(item);
            String id = Util.null2String(rs.getString("id"));
            item.put("id", id);
            String deptName = rs.getString(name);
            item.put(name, deptName);
            if (Util.getIntValue(id) < 0) {
                if (isDepartmentBrowser) {
                    virtualCompanyid = departmentVirtualComInfo.getVirtualtype(id);
                } else {
                    virtualCompanyid = subCompanyVirtualComInfo.getVirtualtypeid(id);
                }
                otherpara = (isDepartmentBrowser ? "1" : "0") + "+" + virtualCompanyid;
            }
            item.put("orgInfo", getAllParentsOrg(id, otherpara));
            if (isDepartmentBrowser) {
                item.put("orgWholePathspan", getOrgDeptWholePath(id, fromModule));
            } else {
                item.put("orgWholePathspan", getOrgSubcomWholePath(id, fromModule));
            }
            for (Map m : browserFieldConfig) {
                String fieldName = Util.null2String(m.get("fieldName"));
                String fullPath = Util.null2String(m.get("fullPath"));
                String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                if (fullPathDelimiter.equals(""))
                    fullPathDelimiter = "|";
                else
                    fullPathDelimiter = "" + fullPathDelimiter + "";
                String orderType = Util.null2String(m.get("orderType"));
                switch (fieldName) {
                    case "departmentname":
                        item.put(fieldName, deptName);//getDepartmentName(deptName, id, fullPath, fullPathDelimiter, virtualCompanyid));
                        break;
                    case "subcompanyid1":
                        item.put(fieldName, getSubComNameByDept(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "supdepid":
                        item.put(fieldName, getSupDeptName(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "departmentcode":
                        item.put(fieldName, getDeptCode(id));
                        break;
                    case "supOrgInfo":
                        item.put(fieldName, getSupOrgInfo(id, fullPathDelimiter, orderType));
//                        item.put(fieldName, departmentComInfo.getAllParentDepartmentBlankNames(id, departmentComInfo.getSubcompanyid1(id), fullPathDelimiter, orderType, false));
                        break;
                    case "subcompanyname":
                        item.put(fieldName, deptName);//getSubCompanyName(deptName, id, fullPath, fullPathDelimiter, virtualCompanyid));
                        break;
                    case "supsubcomid":
                        item.put(fieldName, getSupSubName(id, fullPath, fullPathDelimiter, orderType));
                        break;
                    case "url":
                        item.put(fieldName, getSubCompanyUrl(id));
                        break;
                    case "subcompanycode":
                        item.put(fieldName, getSubCompanyCode(id));
                        break;
                }
            }
        }

        List<ListHeadBean> tableHeadColumns = new ArrayList<ListHeadBean>();
//        tableHeadColumns.add(new ListHeadBean("id", BoolAttr.TRUE).setIsPrimarykey(BoolAttr.TRUE));
        tableHeadColumns.addAll(displayColumns);
        tableHeadColumns.add(new ListHeadBean("orgWholePathspan", "", 2, BoolAttr.FALSE));

        apidatas.put(BrowserConstant.BROWSER_RESULT_COLUMN, tableHeadColumns);
        apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, datas);
        apidatas.put(BrowserConstant.BROWSER_RESULT_TYPE, BrowserDataType.LIST_ALL_DATA.getTypeid());
        return apidatas;
    }

//	@Override
//	public Map<String, Object> getMultBrowserDestData(Map<String, Object> params) throws Exception {
//		Map<String, Object> apidatas = new HashMap<String, Object>();
//		SubCompanyVirtualComInfo subCompanyVirtualComInfo  =  new SubCompanyVirtualComInfo();
//		DepartmentVirtualComInfo departmentVirtualComInfo  =  new DepartmentVirtualComInfo();
//		DepartmentComInfo departmentComInfo = new DepartmentComInfo();
//		SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
//		//组织字段显示层级设置（部分模块的组织字段需要显示全路径）
//		//workflow--工作流程、model--表单建模
//		String fromModule = Util.null2String(params.get("fromModule"));
//		String alllevel = Util.null2String(params.get("alllevel"));
//		String types = Util.null2String(params.get("types"));
//
//		String selectids  = Util.null2String(params.get(BrowserConstant.BROWSER_MULT_DEST_SELECTIDS));
//		String virtualCompanyid = Util.null2String(params.get("virtualCompanyid"));
//		if(virtualCompanyid.equals("1")){
//			virtualCompanyid = "";
//		}
//		boolean isDepartmentBrowser  =  this.isDepartmentBrowser();
//		List<Map<String,Object>> datas  = new ArrayList<Map<String,Object>>();
//		String otherpara = (isDepartmentBrowser?"1":"0")+"+"+virtualCompanyid;
//		String querySql = "select ";
//		String name  = "";
//		String idCondition = selectids;
//		List<String> allSubcompanyids = new ArrayList<>();
//		String subCompanyidCondition = "";
//		List allChildren = new ArrayList();
//		boolean selectedAll = false;
//		//加载浏览按钮显示列定义
//		List<ListHeadBean> displayColumns = new ArrayList<ListHeadBean>();
//		if(isDepartmentBrowser){
//			displayColumns = loadDisplayColumnConfig(2);
//			querySql += "id,departmentname,id as otherParam";
//			querySql += " from hrmdepartmentallview";
//			name = "departmentname";
//			List<String> list = Util.splitString2List(types, ",");
//			if(list.contains("subCom|0"))
//				selectedAll = true;
//			for (String row : list) {
//				List<String> vals = Util.splitString2List(row, "|");
//				switch (vals.get(0)) {
//					case "subcom":
//						allSubcompanyids.add(vals.get(1));
//						if ("1".equals(alllevel)) {
//							if ("".equals(virtualCompanyid))
//								allSubcompanyids.addAll(Util.splitString2List(subCompanyComInfo.getAllChildSubcompanyId(vals.get(1), ""), ","));
//							else
//								allSubcompanyids.addAll(Util.splitString2List(subCompanyVirtualComInfo.getAllChildSubcompanyId(vals.get(1), ""), ","));
//						}
//						break;
//					case "dept":
//						allChildren.add(vals.get(1));
//						if ("1".equals(alllevel)) {
//							if ("".equals(virtualCompanyid))
//								allChildren.addAll(Util.splitString2List(departmentComInfo.getAllChildDepartId(vals.get(1), ""), ","));
//							else
//								allChildren.addAll(Util.splitString2List(departmentVirtualComInfo.getAllChildDepartId(vals.get(1), ""), ","));
//						}
//						break;
//				}
//			}
//		}else{
//			displayColumns = loadDisplayColumnConfig(3);
//			querySql += " id,subcompanyname,id as otherParam";
//			querySql += " from hrmsubcompanyallview";
//			name = "subcompanyname";
//			if("".equals(alllevel)){
//				List<String> ids = Util.splitString2List(selectids, ",");
//				for(String id : ids){
//					allChildren.add(id);
//
//					boolean virtualFlag = Integer.parseInt(id) < 0;
////					if("".equals(virtualCompanyid)) {
//					if(!virtualFlag){
//						allChildren.addAll(Util.splitString2List(subCompanyComInfo.getAllChildSubcompanyId(id, ""), ","));
//					}else
//						allChildren.addAll(Util.splitString2List(subCompanyVirtualComInfo.getAllChildSubcompanyId(id, ""), ","));
//				}
//			}
//		}
//		if(allChildren.size() > 0){
//			idCondition = formatIds(allChildren);
//		}
//		if(allSubcompanyids.size() > 0){
//			subCompanyidCondition = formatIds(allSubcompanyids);
//		}
//		querySql += " where 1 = 1 and (canceled is null or canceled = '' or canceled = '0') ";
//
//		if(!selectedAll) {
//			String idWhereCase = "";
//			if (!"".equals(idCondition)) {
//				idWhereCase = Util.getSubINClause(idCondition, "id", "IN");
//			}
//			if (!"".equals(subCompanyidCondition)) {
//				if (!"".equals(idWhereCase))
//					idWhereCase += " or " + Util.getSubINClause(subCompanyidCondition, "subcompanyid1", "IN");
//				else
//					idWhereCase += Util.getSubINClause(subCompanyidCondition, "subcompanyid1", "IN");
//			}
//			if (!"".equals(idWhereCase))
//				querySql += " and (" + idWhereCase + ")";
//		}else{
//			querySql += " and VIRTUALTYPE = " + virtualCompanyid;
//		}
//		RecordSet rs  = new RecordSet();
//		rs.executeSql(querySql);
//		while(rs.next()){
//			Map<String,Object> item = new HashMap<String,Object>();
//			datas.add(item);
//			String id  = Util.null2String(rs.getString("id"));
//			item.put("id",id);
//			String deptName = rs.getString(name);
//			item.put(name, deptName);
//			if(Util.getIntValue(id)<0){
//				if(isDepartmentBrowser){
//					virtualCompanyid = departmentVirtualComInfo.getVirtualtype(id);
//				}else{
//					virtualCompanyid = subCompanyVirtualComInfo.getVirtualtypeid(id);
//				}
//				otherpara = (isDepartmentBrowser?"1":"0")+"+"+virtualCompanyid;
//			}
//			item.put("orgInfo", getAllParentsOrg(id,otherpara));
//			if(isDepartmentBrowser){
//				item.put("orgWholePathspan",getOrgDeptWholePath(id,fromModule));
//			}else{
//				item.put("orgWholePathspan",getOrgSubcomWholePath(id,fromModule));
//			}
//			for (Map m : browserFieldConfig) {
//				String fieldName = Util.null2String(m.get("fieldName"));
//				String fullPath = Util.null2String(m.get("fullPath"));
//				String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
//				if(fullPathDelimiter.equals(""))
//					fullPathDelimiter = "|";
//				else
//					fullPathDelimiter = "" + fullPathDelimiter + "";
//				String orderType = Util.null2String(m.get("orderType"));
//				switch(fieldName){
//					case "departmentname":
//						item.put(fieldName, deptName);//getDepartmentName(deptName, id, fullPath, fullPathDelimiter, virtualCompanyid));
//						break;
//					case "subcompanyid1":
//						item.put(fieldName, getSubComNameByDept(id, fullPath, fullPathDelimiter, orderType, virtualCompanyid));
//						break;
//					case "supdepid":
//						item.put(fieldName, getSupDeptName(id, fullPath, fullPathDelimiter, orderType, virtualCompanyid));
//						break;
//					case "departmentcode":
//						item.put(fieldName, getDeptCode(id, virtualCompanyid));
//						break;
//					case "subcompanyname":
//						item.put(fieldName, deptName);//getSubCompanyName(deptName, id, fullPath, fullPathDelimiter, virtualCompanyid));
//						break;
//					case "supsubcomid":
//						item.put(fieldName, getSupSubName(id, fullPath, fullPathDelimiter, orderType, virtualCompanyid));
//						break;
//					case "url":
//						item.put(fieldName, getSubCompanyUrl(id, virtualCompanyid));
//						break;
//					case "subcompanycode":
//						item.put(fieldName, getSubCompanyCode(id, virtualCompanyid));
//						break;
//				}
//			}
//		}
//
//		List<ListHeadBean> tableHeadColumns =  new ArrayList<ListHeadBean>();
//		tableHeadColumns.add(new ListHeadBean("id",BoolAttr.TRUE).setIsPrimarykey(BoolAttr.TRUE));
//		tableHeadColumns.addAll(displayColumns);
////		tableHeadColumns.add(new ListHeadBean(name,"",1,BoolAttr.TRUE));
////		tableHeadColumns.add(new ListHeadBean("orgInfo",""));
//		tableHeadColumns.add(new ListHeadBean("orgWholePathspan","",2,BoolAttr.TRUE));
//
//		apidatas.put(BrowserConstant.BROWSER_RESULT_COLUMN, tableHeadColumns);
////		apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, BrowserBaseUtil.sortDatas(datas,idCondition,"id"));
//		apidatas.put(BrowserConstant.BROWSER_RESULT_DATA, datas);
//		apidatas.put(BrowserConstant.BROWSER_RESULT_TYPE, BrowserDataType.LIST_ALL_DATA.getTypeid());
//		return apidatas;
//	}

    /**
     * @return
     */
    private boolean isDepartmentBrowser() {
        return "4".equals(browserType) || "57".equals(browserType) || "167".equals(browserType) || "168".equals(browserType);
    }

    /**
     * Description 装载当前用户具有访问权限的机构列表，用于分权管理
     */
    private void getSubCompanyTreeListByDecRight() throws Exception {
        CheckSubCompanyRight newCheck = new CheckSubCompanyRight();
        String rightStr = Util.null2String(this.params.get("rightStr"));
        if (this.browserType.equals("167") || this.browserType.equals("168")) {
            if ("".equals(rightStr)) {
                rightStr = "Departments:decentralization";
            }
        } else if (this.browserType.equals("169") || this.browserType.equals("170")) {
            if ("".equals(rightStr)) {
                rightStr = "Subcompanys:decentralization";
            }
        }
        int beagenter = Util.getIntValue(Util.null2String(this.params.get("beagenter_" + user.getUID())), 0);
        if (beagenter <= 0) {
            beagenter = user.getUID();
        }
        int isdetail = Util.getIntValue(Util.null2String(this.params.get("viewtype")), 0);
        int isbill = Util.getIntValue(Util.null2String(this.params.get("isbill")), 0);
        int fieldid = Util.getIntValue(Util.null2String(this.params.get("fieldid")), 0);
        boolean onlyselfdept = newCheck.getDecentralizationAttr(beagenter, rightStr, fieldid, isdetail, isbill);
        //boolean isall=newCheck.getIsall();

        String isruledesign = Util.null2String(this.params.get("isruledesign"));
        boolean isadmin = false;
        String adminsql = "select * from HrmResourceManager where id = " + beagenter;
        RecordSet recordSet = new RecordSet();
        recordSet.executeSql(adminsql);
        if (recordSet.next()) {
            isadmin = true;
        }

        if (isadmin && "true".equals(isruledesign)) {
            beagenter = 1;
        }
        if (fieldid <= 0) { //主要针对分权分部 保持原样
            ManageDetachComInfo me = new ManageDetachComInfo();
            if (rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet")) {
                UserDepts = newCheck.getDepartmentPathByDec(user.getUID(), onlyselfdept);
                subcomids = newCheck.getSubComPathByDecUserRightId(user.getUID(), rightStr, 1);
                subcomids1 = newCheck.getSubComByUserRightId(user.getUID(), rightStr, 1);
                boolean exec = me.isUseExecutionManageDetach();
                if (!exec) {
                    String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                    switch (rightLevel) {
                        case "0":
                            if(this.browserType.equals("194")){
                                subcomids = new int[0];
                                subcomids1 = new int[0];
                                break;
                            }
                            break;
                        case "1":
                            int num3 = subcomids.length + allChildSubComIds.length;
                            int num4 = subcomids1.length + allChildSubComIds.length;
                            int intlist3[] = new int[num3];
                            int intlist4[] = new int[num4];
                            for (int i = 0; i < subcomids.length; i++) {
                                intlist3[i] = subcomids[i];
                            }
                            for (int i = 0; i < subcomids1.length; i++) {
                                intlist4[i] = subcomids1[i];
                            }
                            for (int i = 0; i < allChildSubComIds.length; i++) {
                                if (Util.getIntValue(allChildSubComIds[i]) > 0) {
                                    intlist3[subcomids.length + i] = Util.getIntValue(allChildSubComIds[i]);
                                    intlist4[subcomids1.length + i] = Util.getIntValue(allChildSubComIds[i]);
                                }
                            }
                            subcomids = new int[num3];
                            subcomids1 = new int[num4];
                            subcomids = intlist3;
                            subcomids1 = intlist4;
                            break;
                        default:
                            break;
                    }
                }else{
                    getUserDeptes();
                }
              } else if (rightStr.equals("blog:newappSetting")) {
                UserDepts = newCheck.getDepartmentPathByDec(user.getUID(), onlyselfdept);
                subcomids = newCheck.getSubComPathByDecUserRightId(user.getUID(), rightStr, -1);
                subcomids1 = newCheck.getSubComByUserRightId(user.getUID(), rightStr, 1);
                boolean isUseBlogManageDetach = me.isUseBlogManageDetach();
                if (!isUseBlogManageDetach) {
                    String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                    int subcompanyID = user.getUserSubCompany1();
                    String subcompids = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(subcompanyID), String.valueOf(subcompanyID));
                    subcompids = ("," + subcompids + ",").replaceAll("," + subcompanyID + ",", "");
                    if (rightLevel.equals("1")) { //分部级别
                        try {
                            subcompids = subcompids.trim().startsWith(",")?subcompids.trim().substring(1):subcompids.trim();
                            subcompids = subcompids.trim().endsWith(",")?subcompids.trim().substring(0,subcompids.trim().length()-1):subcompids.trim();
                            String[] ids = Util.splitString(subcompids, ",");
                            int num1 = subcomids.length + ids.length;
                            int num2 = subcomids1.length + ids.length;
                            int intlist1[] = new int[num1];
                            int intlist2[] = new int[num2];
                            for (int i = 0; i < subcomids.length; i++) {
                                intlist1[i] = subcomids[i];
                            }
                            for (int i = 0; i < subcomids1.length; i++) {
                                intlist2[i] = subcomids1[i];
                            }
                            for (int i = 0; i < ids.length; i++) {
                                if (Util.getIntValue(ids[i]) > 0) {
                                    intlist1[subcomids.length + i] = Util.getIntValue(ids[i]);
                                    intlist2[subcomids1.length + i] = Util.getIntValue(ids[i]);
                                }
                            }
                            subcomids = new int[num1];
                            subcomids1 = new int[num2];
                            subcomids = intlist1;
                            subcomids1 = intlist2;
                        } catch (Exception e) {
                            writeLog(e);
                        }
                    }
                }
            } else{
                UserDepts = newCheck.getDepartmentPathByDec(user.getUID(), onlyselfdept);
                subcomids = newCheck.getSubComPathByDecUserRightId(user.getUID(), rightStr, 0);
                subcomids1 = newCheck.getSubComByUserRightId(user.getUID(), rightStr, 1);
                boolean isUseHrmManageDetach = me.isUseHrmManageDetach();
                if (!isUseHrmManageDetach) {
                    if (rightStr.length() > 0) {
                        String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                        int departmentID = user.getUserDepartment();
                        if (rightLevel.equals("0")) { //部门级别
                            UserDepts1.add("" + departmentID);
                        }
                    }
                }
            }
        } else {
            OrganizationUtil ou = new OrganizationUtil();

            ou.selectData(user.getUID(), fieldid + "", isdetail, isbill, rightStr);
            ArrayList subcList = ou.getSubcomList();
            //if(onlyselfdept) subcList = new ArrayList();//流程设置了本部门，会带上本分部，这个不对，这里重置一下
            ArrayList depats_list = ou.getDepatList();
            ArrayList subcomids_list = ou.getNecessarySupcomList(); //必要的上级分部
            UserDepts = ou.getNecessarySupDepatList();//必要的上级部门

            subcomids1 = new int[subcList.size()];
            for (int i = 0; i < subcList.size(); i++) {
                String idtmp = (String) subcList.get(i);
                subcomids1[i] = Util.getIntValue(idtmp);
                if (!subcomids_list.contains(idtmp)) {
                    subcomids_list.add(idtmp);
                }
            }

            subcomids = new int[subcomids_list.size()];
            for (int i = 0; i < subcomids_list.size(); i++) {
                subcomids[i] = Util.getIntValue((String) subcomids_list.get(i));
            }

            UserDepts1 = depats_list;
            for (int i = 0; i < depats_list.size(); i++) {
                String id = depats_list.get(i) + "";
                if (!UserDepts.contains(id)) {
                    UserDepts.add(id);
                }
            }
            new BaseBean().writeLog("==zj==(数据查看)" + JSON.toJSONString(UserDepts) + "   |   " + JSON.toJSONString(subcomids1) + "  |   " + JSON.toJSONString(subcomids)  +  "  |   " + JSON.toJSONString(UserDepts1));
//      	System.out.println("subcomids:"+subcomids_list);
//      	System.out.println("subcomids1:"+subcList);
//      	System.out.println("UserDepts:"+UserDepts);
//      	System.out.println("UserDepts1:"+UserDepts1);
//      	System.out.println("onlyselfdept:"+onlyselfdept);
        }
//	  	System.out.println("UserDepts:"+UserDepts);
//	  	System.out.println("subcomids:"+JSONObject.toJSONString(subcomids));
//	  	System.out.println("subcomids1:"+JSONObject.toJSONString(subcomids1));
        //System.out.println("userId:"+userId+"   fieldid:"+fieldid+"   isdetail:"+isdetail+"  isbill:"+isbill);

//	  	System.out.println("subcomids:"+JSONObject.toJSONString(subcomids));
//	  	System.out.println("subcomids1:"+JSONObject.toJSONString(subcomids1));
//	  	System.out.println("UserDepts:"+JSONObject.toJSONString(UserDepts));
//	  	System.out.println("UserDepts1:"+JSONObject.toJSONString(UserDepts1));
    }

    /**
     * 检查机构分权和应用分权
     *
     * @param type
     * @param id
     * @return result -1无权  1查看  2有权
     */
    private int checkDetach(String type, String id) {
        int rightLevel = 2;
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        if (type.equals("com")) {
            //检查浏览数据定义
            if (this.dataRanageAllSubCompanyIds != null && !this.dataRanageAllSubCompanyIds.isEmpty()) {
                rightLevel = -1;
                if (this.dataRanageAllSubCompanyIds.contains(id)) {
                    rightLevel =2;
                }
                if(rightLevel==-1)return rightLevel;
            }

            if (Util.getIntValue(id) > 0) {
                if (this.isDetachBrower) {//检查机构分权
                    rightLevel = -1;
                    for (int i = 0; i < subcomids.length; i++) {
                        if (id.equals(String.valueOf(subcomids[i]))) {
                            rightLevel = 1;
                            break;
                        }
                    }

                    for (int i = 0; i < subcomids1.length; i++) {
                        if (id.equals(String.valueOf(subcomids1[i]))) {
                            rightLevel = 2;
                            break;
                        }
                    }
                } else {//检查应用分权
                    if (adci == null) {
                        adci = new AppDetachComInfo(user);
                    }
                    if (adci.isUseAppDetach()) {
                        int result = adci.checkUserAppDetach(id, "2");
                        if ( result == 0) {
                            rightLevel = -1;
                        } else if ( result == 1) {
                            rightLevel = 2;
                        } else if ( result == 2) {
                            rightLevel = 1;
                        }
                    }
                }
            }
        } else if (type.equals("dept")) {
            String subcompanyid = departmentComInfo.getSubcompanyid1(id);

            //检查浏览数据定义
            if (this.dataRanageAllDepartmentIds != null && !this.dataRanageAllDepartmentIds.isEmpty()) {
                rightLevel =-1;
                if (this.dataRanageAllDepartmentIds.contains(id)) {
                    rightLevel =2;
                } else {
                    if (this.dataRanageSubCompanyIds.contains(subcompanyid)) {
                        rightLevel =2;
                    }
                }
                if(rightLevel==-1)return rightLevel;
            }

            if (Util.getIntValue(id) > 0) {
                if (this.isDetachBrower) {//检查机构分权
                    rightLevel = -1;
                    if (this.UserDepts.size() > 0 && this.UserDepts.indexOf(id) != -1) {
                        rightLevel = 1;
                    }
                    if (this.subcomids.length > 0) {
                        for (int i = 0; i < subcomids.length; i++) {
                            if (subcompanyid.equals(String.valueOf(subcomids[i]))) {
                                rightLevel =1;
                            }
                        }
                    }

                    if (this.UserDepts1.size() > 0 && this.UserDepts1.indexOf(id) != -1) {
                        rightLevel =2;
                    }

                    if (this.subcomids1.length > 0) {
                        for (int i = 0; i < subcomids1.length; i++) {
                            if (subcompanyid.equals(String.valueOf(subcomids1[i]))) {
                                rightLevel =2;
                            }
                        }
                    }
                } else {// 检查应用分权
                    if (adci == null) {
                        adci = new AppDetachComInfo(user);
                    }
                    if (adci.isUseAppDetach()) {
                        int result = adci.checkUserAppDetach(id, "3");
                        if ( result == 0) {
                            rightLevel = -1;
                        } else if ( result == 1) {
                            rightLevel = 2;
                        } else if ( result == 2) {
                            rightLevel = 1;
                        }
                    }
                }
            }
        }
        return  rightLevel;
    }
    /**
     * 生成执行力分权未开启时的角色查看分部部门id
     * @param rightStr
     * @return
     */
    private void checkBlogAppLoadSub(String rightStr) {
        if (!this.isExecutionDetach) {
            //检查执行力管理分权
            if (HrmUserVarify.checkUserRight(rightStr, user) && (rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet"))) {
                HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
                int rolelevel = hrmCommonService.getMaxRoleLevel(this.user.getUID(), rightStr);
                switch (rolelevel) {
                    case 0:
                        int userdepart = this.user.getUserDepartment();
                        String detidss = null;
                        try {
                            detidss = DepartmentComInfo.getAllChildDepartId(String.valueOf(userdepart), String.valueOf(userdepart));
                        } catch (Exception e) {
                            writeLog(e);
                        }
                        String[] result0 = detidss.split(",");
                        this.allChildDeptIds = result0;
                        break;
                    case 1:
                        int usersubcom = this.user.getUserSubCompany1();
                        String idss = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(usersubcom), String.valueOf(usersubcom));
                        String[] result1 = idss.split(",");
                        this.allChildSubComIds = result1;
                        break;
                    default:
                        break;
                }
            }
        }
    }
    private class OrgBean extends BrowserTreeNode {
        private String companyid;
        private String isVirtual;
        private String psubcompanyid;
        private String orgWholePathspan;//组织字段层架显示
        private List<String> displayKeys;

        private String departmentname;
        private String subcompanyid1;
        private String supdepid;
        private String departmentcode;
        private String supOrgInfo;
        private String subcompanyname;
        private String supsubcomid;
        private String url;
        private String subcompanycode;

        public String getCompanyid() {
            return companyid;
        }

        public void setCompanyid(String companyid) {
            this.companyid = companyid;
        }


        public String getIsVirtual() {
            return isVirtual;
        }

        public void setIsVirtual(String isVirtual) {
            this.isVirtual = isVirtual;
        }

        public String getPsubcompanyid() {
            return psubcompanyid;
        }

        public void setPsubcompanyid(String psubcompanyid) {
            this.psubcompanyid = psubcompanyid;
        }

        public String getOrgWholePathspan() {
            return orgWholePathspan;
        }

        public void setOrgWholePathspan(String orgWholePathspan) {
            this.orgWholePathspan = orgWholePathspan;
        }

        public List<String> getDisplayKeys() {
            return displayKeys;
        }

        public void setDisplayKeys(List<String> displayKeys) {
            this.displayKeys = displayKeys;
        }

        public String getDepartmentname() {
            return departmentname;
        }

        public void setDepartmentname(String departmentname) {
            this.departmentname = departmentname;
        }

        public String getSubcompanyid1() {
            return subcompanyid1;
        }

        public void setSubcompanyid1(String subcompanyid1) {
            this.subcompanyid1 = subcompanyid1;
        }

        public String getSupdepid() {
            return supdepid;
        }

        public void setSupdepid(String supdepid) {
            this.supdepid = supdepid;
        }

        public String getDepartmentcode() {
            return departmentcode;
        }

        public void setDepartmentcode(String departmentcode) {
            this.departmentcode = departmentcode;
        }

        public String getSupOrgInfo() {
            return supOrgInfo;
        }

        public void setSupOrgInfo(String supOrgInfo) {
            this.supOrgInfo = supOrgInfo;
        }

        public String getSubcompanyname() {
            return subcompanyname;
        }

        public void setSubcompanyname(String subcompanyname) {
            this.subcompanyname = subcompanyname;
        }

        public String getSupsubcomid() {
            return supsubcomid;
        }

        public void setSupsubcomid(String supsubcomid) {
            this.supsubcomid = supsubcomid;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSubcompanycode() {
            return subcompanycode;
        }

        public void setSubcompanycode(String subcompanycode) {
            this.subcompanycode = subcompanycode;
        }
    }

    @Override
    public Map<String, Object> browserAutoComplete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        List<Map<String, String>> datas = new ArrayList<Map<String, String>>();
        RecordSet rs = new RecordSet();
        ManageDetachComInfo mdc = new ManageDetachComInfo();
        CheckSubCompanyRight cscr = new CheckSubCompanyRight();
        AppDetachComInfo appDetachComInfo = new AppDetachComInfo();
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
        HttpSession session = request.getSession();

        String whereClause = Util.null2String(request.getParameter("whereClause"));
        if (whereClause.length() == 0) {
            whereClause = Util.null2String(request.getParameter("sqlwhere"));
        }
        //接口中开发的条件
        String spellSqlWhere = Util.null2String(request.getAttribute(BrowserConstant.SPELL_SQL_WHERE));
        if (!"".equals(spellSqlWhere)) {
            whereClause += spellSqlWhere;
        }
        if (whereClause.length() == 0) {
            whereClause = " where 1=1 ";
        }

        //workflow--工作流程、model--表单建模
        String fromModule = Util.null2String(request.getParameter("fromModule"));

        // 部门
        if (this.browserType.equals("4") || this.browserType.equals("57") || this.browserType.equals("167") || this.browserType.equals("168")) {
            loadDisplayColumnConfig(2);
            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            Map<String, Object> params = ParamUtil.request2Map(request);
            params.put("tableAlias", "hrmdepartment");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, Util.getIntValue(this.browserType));
            //解决部门浏览按钮加了浏览数据定义之后无法模糊搜索
            String dataDefineSql = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataDefineSql.length() > 0) {
                if (!dataDefineSql.trim().startsWith("and")) {
                    if (whereClause.length() > 0) {
                        whereClause += " and ";
                    }
                }
                whereClause += dataDefineSql;
            }

            if (whereClause.length() > 0)
                whereClause += "and";
            if(rs.getDBType().equalsIgnoreCase("postgresql"))
                whereClause += " hrmdepartment.subcompanyid1=hrmsubcompany.id and (hrmdepartment.canceled = '0' or hrmdepartment.canceled is null) ";
            else
                whereClause += " hrmdepartment.subcompanyid1=hrmsubcompany.id and (hrmdepartment.canceled = '0' or hrmdepartment.canceled is null or hrmdepartment.canceled = '') ";
            if (this.browserType.equals("167") || this.browserType.equals("168")) {
                String rightStr = Util.null2String(request.getParameter("rightStr"));
                 if(rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet")){
                    boolean isExecManageDetach = mdc.isUseExecutionManageDetach();
                    if (isExecManageDetach) {
                        String companyid = "";
                        int[] companyids = cscr.getSubComByUserRightId(user.getUID(), rightStr, 1);
                        for (int i = 0; companyids != null && i < companyids.length; i++) {
                            companyid += "," + companyids[i];
                        }
                        if (companyid.length() > 0) {
                            companyid = companyid.substring(1);
                            whereClause += " and ( hrmdepartment.subcompanyid1 in(" + companyid + "))";
                        } else {
                            whereClause += " and ( hrmdepartment.subcompanyid1 in(0))";// 分权而且没有选择机构权限
                        }
                    } else {
                        String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                        int subcompanyID = user.getUserSubCompany1();
                        int departmentID = user.getUserDepartment();
                        if (rightLevel.equals("2")) {
                            // 总部级别的，什么也不返回
                        } else if (rightLevel.equals("1")) { // 分部级别的
                            String subcompids = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(subcompanyID), String.valueOf(subcompanyID));
                            whereClause += " and hrmdepartment.subcompanyid1 in（" + subcompids+")";
                        } else if (rightLevel.equals("0")) { // 部门级别
                            String departmentids = DepartmentComInfo.getAllChildDepartId(String.valueOf(departmentID), String.valueOf(departmentID));
                            whereClause += " and hrmdepartment.id in(" + departmentids+")";
                        }
                    }
                }
                rs.executeSql("select detachable from SystemSet");
                int detachable = 0;
                if (rs.next()) {
                    detachable = rs.getInt("detachable");
                }

                boolean isUseHrmManageDetach = mdc.isUseHrmManageDetach();
                if (isUseHrmManageDetach) {
                    detachable = 1;
                } else {
                    detachable = 0;
                }
                if (detachable == 1) {
                    if (rightStr.length() > 0) {
                        String companyid = "";

                        int[] companyids = cscr.getSubComByUserRightId(user.getUID(), rightStr, 1);
                        for (int i = 0; companyids != null && i < companyids.length; i++) {
                            companyid += "," + companyids[i];
                        }
                        if (companyid.length() > 0) {
                            companyid = companyid.substring(1);
                            whereClause += " and ( hrmdepartment.subcompanyid1 in(" + companyid + "))";
                        } else {
                            whereClause += " and ( hrmdepartment.subcompanyid1 in(0))";// 分权而且没有选择机构权限
                        }
                    }
                } else {
                    if (rightStr.length() > 0) {
                        String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                        int departmentID = user.getUserDepartment();
                        int subcompanyID = user.getUserSubCompany1();
                        if (rightLevel.equals("2")) {
                            // 总部级别的，什么也不返回
                        } else if (rightLevel.equals("1")) { // 分部级别的
                            whereClause += " and hrmdepartment.subcompanyid1=" + subcompanyID;
                        } else if (rightLevel.equals("0")) { // 部门级别
                            whereClause += " and hrmdepartment.id=" + departmentID;
                        }
                    }
                }
                int beagenter = Util.getIntValue((String) session.getAttribute("beagenter_" + user.getUID()));
                if (beagenter <= 0) {
                    beagenter = user.getUID();
                }
                int fieldid = Util.getIntValue(request.getParameter("fieldid"));
                int isdetail = Util.getIntValue(request.getParameter("isdetail"));
                int isbill = Util.getIntValue(request.getParameter("isbill"), 1);
                if (fieldid != -1) {
                    cscr.setDetachable(1);
                    cscr.setIsbill(isbill);
                    cscr.setFieldid(fieldid);
                    cscr.setIsdetail(isdetail);
                    boolean onlyselfdept = cscr.getDecentralizationAttr(beagenter, "Departments:decentralization", fieldid, isdetail, isbill);
                    boolean isall = cscr.getIsall();
                    String departments = Util.null2String(cscr.getDepartmentids());
                    String subcompanyids = Util.null2String(cscr.getSubcompanyids());
                    if (!isall) {
                        if (onlyselfdept) {
                            if (departments.length() > 0 && !departments.equals("0")) {
                                whereClause += " and hrmdepartment.id in(" + departments + ")";
                            }
                        } else {
                            if (subcompanyids.length() > 0 && !subcompanyids.equals("0")) {
                                whereClause += " and subcompanyid1 in(" + subcompanyids + ")";
                            }
                        }
                    }
                }
            }
            // Added by wcd 2014-11-28 增加分权控制 start
            String tempSql = appDetachComInfo.getScopeSqlByHrmResourceSearch(String.valueOf(user.getUID()), true, "department");
            whereClause += (whereClause == null || whereClause.length() == 0) ? tempSql : (tempSql.equals("") ? " " : " and " + tempSql);

            // Added by wcd 2014-11-28 增加分权控制 end

            // 虚拟组织
            String virtualCompanyid = Util.null2String(request.getParameter("virtualCompanyid"));
            if (virtualCompanyid.length() == 0) {
                virtualCompanyid = Util.null2String(request.getParameter("virtualtype"));
            }

            if (!"".equals(virtualCompanyid)) {
                whereClause += " and hrmdepartment.virtualtype =" + virtualCompanyid;
            }

            String subcompanyid1 = Util.null2String(request.getParameter("subcompanyid1"));
            if (!"".equals(subcompanyid1)) {
                whereClause += " and hrmdepartment.subcompanyid1 =" + subcompanyid1;
            }

            String qStr = Util.null2String(request.getParameter("q"));
            if (!"".equals(qStr)) {
                whereClause += " and (departmentname like '%" + qStr + "%' ";
                if ("oracle".equalsIgnoreCase(rs.getDBType()) || "mysql".equals(rs.getDBType())) {
                    whereClause += " or hrmdepartment.ecology_pinyin_search like '%" + qStr.toLowerCase() + "%'";
                } else if ("sqlserver".equals(rs.getDBType())) {
                    whereClause += " or hrmdepartment.ecology_pinyin_search like '%" + qStr.toLowerCase() + "%'";
                }
                else if ("postgresql".equalsIgnoreCase(rs.getDBType()) ) {
                    whereClause += " or hrmdepartment.ecology_pinyin_search like '%" + qStr.toLowerCase() + "%'";
                }
                whereClause += ")";
            }

            String backfields = " hrmdepartment.id,departmentmark as name,subcompanyname as title ";
            String sqlfrom = " from hrmdepartment , hrmsubcompany ";
            String sqlwhere = whereClause.trim().startsWith("where") ? whereClause : " where " + whereClause;
            String orderby = " order by hrmdepartment.showorder asc ";
            if (this.browserType.equals("167") || this.browserType.equals("168")||Util.null2String(request.getParameter("show_virtual_org")).equals("-1")) {
            } else {
                sqlfrom = " from hrmdepartmentallView hrmdepartment, hrmsubcompany ";

            }
            String sql = " select " + backfields + sqlfrom + sqlwhere + orderby;

            List<String> ids = Lists.newArrayList();
            rs.executeQuery(sql);
            while (rs.next()) {
                Map<String, String> item = new HashMap<String, String>();
                String id = rs.getString("id");
                if(!ids.isEmpty() && ids.contains(id)){
                  continue;
                }else{
                  ids.add(id);
                }
                item.put("id", id);
                String name = rs.getString("name");
                String deptPath = getOrgDeptWholePath(id, fromModule);
                item.put("name", deptPath);
//				String title = rs.getString("name");
//				if(Util.null2String(rs.getString("title")).length()>0){
//					title+= "|" + Util.null2String(rs.getString("title"));
//				}
//				item.put("title", title);
                String title = "";
                for (int i = 0; i < browserFieldConfig.size(); i++) {
                    Map m = browserFieldConfig.get(i);
                    String fieldName = Util.null2String(m.get("fieldName"));
                    String fullPath = Util.null2String(m.get("fullPath"));
                    String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                    if (fullPathDelimiter.equals(""))
                        fullPathDelimiter = "|";
                    else
                        fullPathDelimiter = "" + fullPathDelimiter + "";
                    String orderType = Util.null2String(m.get("orderType"));
                    String value = "";
                    switch (fieldName) {
                        case "departmentname":
                            value = name;//getDepartmentName(name, id, fullPath, fullPathDelimiter, virtualCompanyid);
                            break;
                        case "subcompanyid1":
                            value = getSubComNameByDept(id, fullPath, fullPathDelimiter, orderType);
                            break;
                        case "supdepid":
                            value = getSupDeptName(id, fullPath, fullPathDelimiter, orderType);
                            break;
                        case "departmentcode":
                            value = getDeptCode(id);
                            break;
                        case "supOrgInfo":
                            value = getSupOrgInfo(id, fullPathDelimiter, orderType);//departmentComInfo.getAllParentDepartmentBlankNames(id, departmentComInfo.getSubcompanyid1(id), fullPathDelimiter, orderType, false);
                            break;
                    }
                    if (i == 0)
                        title += value;
                    else {
                        if (title.equals(""))
                            title += value;
                        else if (!value.equals(""))
                            title += "|" + value;
                    }
                }
                item.put("title", title);
                datas.add(item);
            }
            apidatas.put("datas", datas);
        } else if (this.browserType.equals("164") || this.browserType.equals("194") || this.browserType.equals("169") || this.browserType.equals("170")) {// 分部
            loadDisplayColumnConfig(3);
            WorkflowCommonService WorkflowCommonService = new WorkflowCommonServiceImpl();
            Map<String, Object> params = ParamUtil.request2Map(request);
            params.put("tableAlias", "hrmsubcompany");
            Map<String, Object> dataRanage = WorkflowCommonService.getDataDefinitionDataRanageSet(params, user, Util.getIntValue(this.browserType));
            //解决分部浏览按钮加了浏览数据定义之后无法模糊搜索
            String dataDefineSql = Util.null2String(dataRanage.get("sqlWhere"));
            if (dataDefineSql.length() > 0) {
                if (!dataDefineSql.trim().startsWith("and")) {
                    if (whereClause.length() > 0) {
                        whereClause += " and ";
                    }
                }
                whereClause += dataDefineSql;
            }
            // Added by wcd 2014-11-28 增加分权控制
            String tempSql = appDetachComInfo.getScopeSqlByHrmResourceSearch(String.valueOf(user.getUID()), true, "subcompany");
            whereClause += (whereClause == null || whereClause.length() == 0) ? tempSql : (tempSql.equals("") ? " " : " and " + tempSql);

            rs.executeSql("select detachable from SystemSet");
            int detachable = 0;
            if (rs.next()) {
                detachable = rs.getInt("detachable");
            }
            ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
            CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
            boolean isUseHrmManageDetach = ManageDetachComInfo.isUseHrmManageDetach();
            if (isUseHrmManageDetach) {
                detachable = 1;
            } else {
                detachable = 0;
            }

            if (this.browserType.equals("169") || this.browserType.equals("170")|| this.browserType.equals("194")) {
                String rightStr = Util.null2String(request.getParameter("rightStr"));
                if (rightStr.equals("blog:newappSetting")) {
                    boolean isUseBlogManageDetach = ManageDetachComInfo.isUseBlogManageDetach();
                    if (isUseBlogManageDetach) {
                        String companyid = "";
                        int[] companyids = CheckSubCompanyRight.getSubComByUserRightId(user.getUID(), rightStr, 1);
                        for (int i = 0; companyids != null && i < companyids.length; i++) {
                            companyid += "," + companyids[i];
                        }
                        if (companyid.length() > 0) {
                            companyid = companyid.substring(1);
                            whereClause += " and ( id in(" + companyid + "))";
                        } else {
                            whereClause += " and ( id in(0))";//分权而且没有选择机构权限
                        }
                    } else {
                        String rightLevel = HrmUserVarify.getRightLevel("blog:newappSetting", user);
                        int subcompanyID = user.getUserSubCompany1();
                        if (rightLevel.equals("2")) {
                            //总部级别的，什么也不返回
                        } else if (rightLevel.equals("1")) { //分部级别的
                            String subcompids = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(subcompanyID), String.valueOf(subcompanyID));
                            if (whereClause.length() > 0)
                                whereClause += " and id in (" + subcompids + ")";
                            else
                                whereClause += " id in (" + subcompids + ")";
                        } else if (rightLevel.equals("0")) { //部门级别的
                            if (whereClause.length() > 0)
                                whereClause += " and id=" + subcompanyID;
                            else
                                whereClause += " id=" + subcompanyID;
                        }
                    }
                }else if(rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet")){
                    boolean isExecManageDetach = ManageDetachComInfo.isUseExecutionManageDetach();
                    if (isExecManageDetach) {
                        String companyid = "";
                        int[] companyids = CheckSubCompanyRight.getSubComByUserRightId(user.getUID(), rightStr, 1);
                        for (int i = 0; companyids != null && i < companyids.length; i++) {
                            companyid += "," + companyids[i];
                        }
                        if (companyid.length() > 0) {
                            companyid = companyid.substring(1);
                            whereClause += " and ( id in(" + companyid + "))";
                        } else {
                            whereClause += " and ( id in(0))";//分权而且没有选择机构权限
                        }
                    } else {
                        String rightLevel = HrmUserVarify.getRightLevel(rightStr, user);
                        int subcompanyID = user.getUserSubCompany1();
                        if (rightLevel.equals("2")) {
                            //总部级别的，什么也不返回
                        } else if (rightLevel.equals("1")) { //分部级别的
                            String subcompids = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(subcompanyID), String.valueOf(subcompanyID));
                            if (whereClause.length() > 0)
                                whereClause += " and id in (" + subcompids + ")";
                            else
                                whereClause += " id in (" + subcompids + ")";
                        }
                    }
                }
                if (detachable == 1) {
                    if (rightStr.length() > 0) {
                        String companyid = "";
                        int[] companyids = CheckSubCompanyRight.getSubComByUserRightId(user.getUID(), rightStr, 1);
                        for (int i = 0; companyids != null && i < companyids.length; i++) {
                            companyid += "," + companyids[i];
                        }
                        if (companyid.length() > 0) {
                            companyid = companyid.substring(1);
                            whereClause += " and ( id in(" + companyid + "))";
                        } else {
                            whereClause += " and ( id in(0))";//分权而且没有选择机构权限
                        }
                    }

                } else {
                    String rightLevel = HrmUserVarify.getRightLevel("HrmResourceEdit:Edit", user);
                    int subcompanyID = user.getUserSubCompany1();
                    if (rightLevel.equals("2")) {
                        //总部级别的，什么也不返回
                    } else if (rightLevel.equals("1")) { //分部级别的
                        if (whereClause.length() > 0)
                            whereClause += " and id=" + subcompanyID;
                        else
                            whereClause += " id=" + subcompanyID;
                    }
                }
            }
            if (whereClause.length() > 0)
                whereClause += " and (canceled = '0' or canceled is null or canceled = '')  ";
            else
                whereClause += " (canceled = '0' or canceled is null or canceled = '')  ";

            // 如果开启分权
//			rs.executeSql("select detachable from SystemSet");
//			int detachable = 0;
//			if (rs.next()) {
//				detachable = rs.getInt("detachable");
//			}

            // 虚拟组织
            String virtualCompanyid = Util.null2String(request.getParameter("virtualCompanyid"));
            if (virtualCompanyid.length() == 0) {
                virtualCompanyid = Util.null2String(request.getParameter("virtualtype"));
            }

            if (!"".equals(virtualCompanyid)) {
                whereClause += " and virtualtypeid =" + virtualCompanyid;
            }

            String qStr = Util.null2String(request.getParameter("q"));
            if (!"".equals(qStr)) {
                whereClause += " and (subcompanyname like '%" + qStr + "%' ";
                if ("oracle".equalsIgnoreCase(rs.getDBType()) || "mysql".equals(rs.getDBType())) {
                    whereClause += " or ecology_pinyin_search like '%" + qStr.toLowerCase() + "%'";
                } else if ("sqlserver".equals(rs.getDBType())) {
                    whereClause += " or ecology_pinyin_search like '%" + qStr.toLowerCase() + "%'";
                }else if ("postgresql".equals(rs.getDBType())) {
                    whereClause += " or ecology_pinyin_search like '%" + qStr.toLowerCase() + "%'";
                }
                whereClause += ")";
            }

            String backfields = " id,subcompanyname as name,supsubcomid ";
            String sqlfrom = " from hrmsubcompany ";
            String sqlwhere = whereClause.trim().startsWith("where") ? whereClause : " where " + whereClause;
            String orderby = " order by hrmsubcompany.showorder asc ";
            if (this.browserType.equals("169") || this.browserType.equals("170") || Util.null2String(request.getParameter("show_virtual_org")).equals("-1")) {
            } else {
                sqlfrom = " from hrmsubcompany";

            }
            String sql = " select " + backfields + sqlfrom + sqlwhere + orderby;
            rs.executeQuery(sql);
            while (rs.next()) {
                Map<String, String> item = new HashMap<String, String>();
                String id = rs.getString("id");
                item.put("id", id);
                String name = rs.getString("name");
                String subcomPath = getOrgSubcomWholePath(id, fromModule);
                item.put("name", subcomPath);
//				String title = rs.getString("name");
//				if(subCompanyComInfo.getSubCompanyname(rs.getString("supsubcomid")).length()>0){
//					title+= "|" + subCompanyComInfo.getSubCompanyname(rs.getString("supsubcomid"));
//				}
//				item.put("title", title);
                String title = "";
                for (int i = 0; i < browserFieldConfig.size(); i++) {
                    Map m = browserFieldConfig.get(i);
                    String fieldName = Util.null2String(m.get("fieldName"));
                    String fullPath = Util.null2String(m.get("fullPath"));
                    String fullPathDelimiter = Util.null2String(m.get("fullPathDelimiter"));
                    if (fullPathDelimiter.equals(""))
                        fullPathDelimiter = "|";
                    else
                        fullPathDelimiter = "" + fullPathDelimiter + "";
                    String orderType = Util.null2String(m.get("orderType"));
                    String value = "";
                    switch (fieldName) {
                        case "subcompanyname":
                            value = name;//getSubCompanyName(name, id, fullPath, fullPathDelimiter, virtualCompanyid);
                            break;
                        case "supsubcomid":
                            value = getSupSubName(id, fullPath, fullPathDelimiter, orderType);
                            break;
                        case "url":
                            value = getSubCompanyUrl(id);
                            break;
                        case "subcompanycode":
                            value = getSubCompanyCode(id);
                            break;
                    }

                    if (i == 0)
                        title += value;
                    else {
                        if (title.equals(""))
                            title += value;
                        else if (!value.equals(""))
                            title += "|" + value;
                    }
                }
                item.put("title", title);
                datas.add(item);
            }
            apidatas.put("datas", datas);
        } else {
            apidatas = super.browserAutoComplete(request, response);
        }
        return apidatas;
    }

    /**
     * 加载人力浏览按钮显示列定义
     *
     * @return
     */
    private List loadDisplayColumnConfig(int type) {
        List<ListHeadBean> columns = new ArrayList();
        try {
            columns.add(new ListHeadBean("id", BoolAttr.TRUE).setIsPrimarykey(BoolAttr.TRUE));
            browserFieldConfig = new BrowserDisplayFieldServiceImpl().getConfig(type);
            int i = 0;
            for (Map m : browserFieldConfig) {
                String fieldName = String.valueOf(m.get("fieldName")) + "";
                if (i == 0)
                    columns.add(new ListHeadBean(fieldName, "", 1, ("departmentname".equalsIgnoreCase(fieldName)||"subcompanyname".equalsIgnoreCase(fieldName))?BoolAttr.TRUE:BoolAttr.FALSE));
                else
                    columns.add(new ListHeadBean(fieldName, "", 0, ("departmentname".equalsIgnoreCase(fieldName)||"subcompanyname".equalsIgnoreCase(fieldName))?BoolAttr.TRUE:BoolAttr.FALSE));
                i++;
            }
        } catch (Exception ex) {
            writeLog(ex);
        }
        return columns;
    }

    /**
     * 组织字段层级显示--分部
     *
     * @param id         分部ID
     * @param fromModule 模块：workflow--工作流程、model--表单建模
     * @return
     */
    public String getOrgSubcomWholePath(String id, String fromModule) {
        String subcomWholePath = "";
        try {
            OrganizationShowSetBiz organizationShowSetBiz = new OrganizationShowSetBiz();
            subcomWholePath = organizationShowSetBiz.getSubcompanyShow(fromModule, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subcomWholePath;
    }

    /**
     * 组织字段层级显示--部门
     *
     * @param id         部门ID
     * @param fromModule 模块：workflow--工作流程、model--表单建模
     * @return
     */
    public String getOrgDeptWholePath(String id, String fromModule) {
        String deptWholePath = "";
        try {
            OrganizationShowSetBiz organizationShowSetBiz = new OrganizationShowSetBiz();
            deptWholePath = organizationShowSetBiz.getDepartmentShow(fromModule, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deptWholePath;
    }

    private List getDeptFields(int scopeId, int groupId) {
        List<String> deptFieldsDef = new ArrayList<>(Arrays.asList(
                "departmentname",
                "supdepid",
                "subcompanyid1",
                "departmentcode",
                "supOrgInfo"
        ));
        List<String> subCompFieldsDef = new ArrayList<>(Arrays.asList(
                "subcompanyname",
                "supsubcomid",
                "url",
                "subcompanycode"
        ));

        List<Object> list = new ArrayList<>();
        Map<String, Object> fieldInfo = null;
        List keys = (groupId == 6 ? subCompFieldsDef : deptFieldsDef);

        try {
            HrmFieldComInfo HrmFieldComInfo = new HrmFieldComInfo();
            HrmDeptFieldManagerE9 hfm = new HrmDeptFieldManagerE9(scopeId);
            List lsGroup = hfm.getLsGroup();
            for (int i = 0; lsGroup != null && i < lsGroup.size(); i++) {
                String groupid = (String) lsGroup.get(i);
                List lsField = hfm.getLsField(groupid);

                if (lsField.size() == 0)
                    continue;
                for (int j = 0; lsField != null && j < lsField.size(); j++) {
                    String fieldid = (String) lsField.get(j);
                    String fieldname = HrmFieldComInfo.getFieldname(fieldid);
                    if (!keys.contains(fieldname))
                        continue;
                    int label = Integer.parseInt(HrmFieldComInfo.getLabel(fieldid));
                    if (fieldname.equals("departmentname"))
                        label = 124;
                    else if (fieldname.equals("subcompanyid1") || fieldname.equals("subcompanyname"))
                        label = 141;
                    fieldInfo = new HashMap<>();
                    fieldInfo.put("id", fieldname);
                    fieldInfo.put("labelId", label);
                    fieldInfo.put("displayName", SystemEnv.getHtmlLabelName(label, user.getLanguage()));
                    list.add(fieldInfo);
                    this.fieldLabelMap.put(fieldname, label);
                }
            }
            if(groupId != 6){
                this.fieldLabelMap.put("supOrgInfo", 511637);
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return list;
    }

    private String formatIds(List<String> ids) {
        List<String> list = new ArrayList<>();
        for (String id : ids) {
            if (list.indexOf(id) < 0)
                list.add(id);
        }
        return String.join(",", list);
    }

    private String checkDetach(String sqlwhere, boolean isDepartmentBrowser, String virtualCompanyid){
        if(isDepartmentBrowser){

        }else{
            if ("".equals(virtualCompanyid)) {
                String canselectids = "";
                if (Util.null2String(this.dataRanageSqlWhere).length() > 0) {
                    sqlwhere += this.dataRanageSqlWhere;
                }
                if (this.isDetachBrower) {
                    canselectids = "";
                    for (int i = 0; i < this.subcomids1.length; i++) {
                        if (canselectids.length() > 0) canselectids += ",";
                        canselectids += this.subcomids1[i];
                    }
                    if (!"".equals(canselectids)) {
                        sqlwhere += " and (" + Tools.getOracleSQLIn(canselectids, "id") + ")";
                    } else {
                        sqlwhere += " and 1=2 ";
                    }
                } else {
                    // 检查应用分权
                    if ("".equals(virtualCompanyid)) {
                        if (adci == null) adci = new AppDetachComInfo(user);
                        String ids = Util.null2String(adci.getAlllowsubcompanystr());
                        String viewids = Util.null2String(adci.getAlllowsubcompanyviewstr());
                        if (viewids.length() > 0) {
                            if (ids.length() > 0) ids += ",";
                            ids += viewids;
                        }
                        if (ids.length() > 0) {
                            sqlwhere += " and (" + Util.getSubINClause(ids, "id", "in") + ")";
                        }
                    }
                }
            }
        }
        return sqlwhere;
    }
    //开启管理分权后，没有部门数据，执行力开启分权后，需要看到对应分部下的部门。返回对应部门数据
    public void  getUserDeptes() {
        String subcomidstemp = "0";
        for (int i = 0; i < subcomids1.length; i++) {
             subcomidstemp += "," + subcomids1[i];
        }
        RecordSet rs = new RecordSet();
        rs.executeQuery("select id from hrmdepartment where subcompanyid1 in (" + subcomidstemp + ")");
        while (rs.next()){
            UserDepts.add(rs.getString("id"));
            UserDepts1.add(rs.getString("id"));
        }

    }
    /**
     * 检查微博管理分权未开启时的角色成员权限：702505
     * @param type
     * @param id
     * @return
     */
    private boolean checkExecAppSetting(String rightStr, String id) {
        boolean hasRight = true;
        if (!this.isExecutionDetach) {
            if (HrmUserVarify.checkUserRight(rightStr, user)) {
                if (rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet")) {
                    hasRight = false;
                    HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
                    int rolelevel = hrmCommonService.getMaxRoleLevel(this.user.getUID(), rightStr);
                    switch (rolelevel) {
                        case 0:
                            if (this.allChildDeptIds != null && this.allChildDeptIds.length > 0) {
                                for (int i = 0; i < this.allChildDeptIds.length; i++) {
                                    if (id.equals(this.allChildDeptIds[i])) {
                                        hasRight = true;
                                        break;
                                    }
                                }
                            }
                            break;
                        default:
                            hasRight = true;
                            break;
                    }
                }
            }
        }
        return hasRight;
    }

}
