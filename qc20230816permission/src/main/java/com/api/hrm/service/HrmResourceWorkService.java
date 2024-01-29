package com.api.hrm.service;

import java.text.DecimalFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc20230816.permission.util.CheckSelectUtil;
import com.api.customization.qc20230816.permission.util.CheckSetUtil;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.*;
import com.api.browser.bean.SearchConditionItem;
import com.cloudstore.dev.api.util.TextUtil;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.docs.docs.CustomFieldManager;
import weaver.encrypt.EncryptUtil;
import weaver.file.FileUpload;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.contract.ContractTypeComInfo;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.definedfield.HrmFieldManager;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.AllManagers;
import weaver.hrm.resource.CustomFieldTreeManager;
import weaver.hrm.resource.HrmListValidate;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SysMaintenanceLog;
import weaver.systeminfo.SystemEnv;
import weaver.hrm.tools.HrmDateCheck;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import com.api.browser.util.ConditionType;
import weaver.file.ImageFileManager;

/***
 * 人员卡片
 * @author lvyi
 *
 */
public class HrmResourceWorkService extends BaseBean {
    private static final char separator = Util.getSeparator();
    private String today = DateUtil.getCurrentDate();
    private DecimalFormat df = new DecimalFormat("0.00");

    //qc20230816 自定义修改权限
    private String isOpen = new BaseBean().getPropValue("qc20230816permission","isopen");
    /**
     * 查看人员工作信息
     *
     * @param request
     * @param response
     * @return
     */
    public String getResourceWorkView(HttpServletRequest request, HttpServletResponse response) {
        User user = HrmUserVarify.getUser(request, response);
        HrmResourceBaseService hrmResourceBaseService = new HrmResourceBaseService();
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            String id = Util.null2String(request.getParameter("id"));
            if (id.length() == 0) {
                id = "" + user.getUID();
            }
            boolean isSelf = false;
            if (id.equals("" + user.getUID())) {
                isSelf = true;
            }
            //工作信息
            Map<String, Object> buttons = new Hashtable<String, Object>();
            if (id.equals("")) id = String.valueOf(user.getUID());
            String status = "";
            String subcompanyid = "", departmentId = "";
            RecordSet rs = new RecordSet();
            HttpSession session = request.getSession(true);
            rs.executeSql("select subcompanyid1, status, departmentId from hrmresource where id = " + id);
            if (rs.next()) {
                status = Util.toScreen(rs.getString("status"), user.getLanguage());
                subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
                departmentId = StringUtil.vString(rs.getString("departmentId"));
                if (subcompanyid == null || subcompanyid.equals("") || subcompanyid.equalsIgnoreCase("null"))
                    subcompanyid = "-1";
                
            }

            int operatelevel = -1;
            //人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
            int hrmdetachable = 0;
            ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
            if (session.getAttribute("hrmdetachable") != null) {
                hrmdetachable = Util.getIntValue(String.valueOf(session.getAttribute("hrmdetachable")), 0);
            } else {
                boolean isUseHrmManageDetach = ManageDetachComInfo.isUseHrmManageDetach();
                if (isUseHrmManageDetach) {
                    hrmdetachable = 1;
                    session.setAttribute("detachable", "1");
                    session.setAttribute("hrmdetachable", String.valueOf(hrmdetachable));
                } else {
                    hrmdetachable = 0;
                    session.setAttribute("detachable", "0");
                    session.setAttribute("hrmdetachable", String.valueOf(hrmdetachable));
                }
            }
            if (hrmdetachable == 1) {
                CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
                operatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", Integer.parseInt(subcompanyid));
            } else {
                if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentId)) {
                    operatelevel = 2;
                }
            }
            //工作信息修改按钮权限放开
            CheckSetUtil checkSetUtil = new CheckSetUtil();
            Boolean setButtonWork = checkSetUtil.isSetButtonWork(request, response, user);
            if (user.getUID() == 1){
                setButtonWork = true;
            }
            if (((isSelf&&hrmResourceBaseService.isHasModify(3)) || operatelevel > 0 || "1".equals(isOpen)) && !status.equals("10") && setButtonWork) {
                buttons.put("hasEdit", true);
                buttons.put("hasSave", true);
            }

            HrmListValidate HrmListValidate = new HrmListValidate();
            AllManagers AllManagers = new AllManagers();
            boolean isManager = false;
            AllManagers.getAll(id);
            if (id.equals("" + user.getUID())) {
                isSelf = true;
            }
            while (AllManagers.next()) {
                String tempmanagerid = AllManagers.getManagerID();
                if (tempmanagerid.equals("" + user.getUID())) {
                    isManager = true;
                }
            }
            //qc20230816 放开工作信息查看权限
            if ((isSelf || isManager || operatelevel >= 0 /*|| "1".equals(isOpen)*/) && HrmListValidate.isValidate(12)) {
            result.put("buttons", buttons);
            Map<String, Object> tmp = getFormFields(request, response, false);
            result.put("conditions", tmp.get("conditions"));
            result.put("tables", tmp.get("tables"));
            result.put("id", id);
            }
//            if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
//                buttons.put("hasEdit", true);
//                buttons.put("hasSave", true);
//            }

        } catch (Exception e) {
            writeLog(e);
        }


        Map<String, Object> retmap = new HashMap<String, Object>();
        retmap.put("result", result);
        return JSONObject.toJSONString(retmap);
    }

    /**
     * 人员个人信息表单字段
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> getFormFields(HttpServletRequest request, HttpServletResponse response, boolean isAdd) {
        User user = HrmUserVarify.getUser(request, response);
        Map<String, Object> result = new HashMap<String, Object>();
        List<Object> lsGroup = new ArrayList<Object>();
        Map<String, Object> groupitem = null;
        List<Object> itemlist = null;
        try {
            String id = Util.null2String(request.getParameter("id"));
            int viewAttr = Util.getIntValue(request.getParameter("viewAttr"), 1);
            if (isAdd) viewAttr = 2;

            if (id.length() == 0 && !isAdd ) {
                id = "" + user.getUID();
            }

            HrmListValidate HrmListValidate = new HrmListValidate();
            int scopeId = 3;
            HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
            CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
            hfm.getHrmData(Util.getIntValue(id));
            cfm.getCustomData(Util.getIntValue(id));
            String workstartdate = Util.null2String(hfm.getHrmData("workstartdate"));//参加工作日期
            String companystartdate = Util.null2String(hfm.getHrmData("companystartdate"));//入职日期
            CheckSetUtil checkSetUtil = new CheckSetUtil();
            CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
            while (HrmFieldGroupComInfo.next()) {
                int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
                if (grouptype != scopeId) continue;
                int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
                int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
                hfm.getCustomFields(groupid);

                groupitem = new HashMap<String, Object>();
                itemlist = new ArrayList<Object>();
                groupitem.put("title", SystemEnv.getHtmlLabelName(grouplabel, user.getLanguage()));
                groupitem.put("defaultshow", true);
                if (groupid == 5) {
                    HrmListValidate hrmListValidate = new HrmListValidate();
                    groupitem.put("hide", !hrmListValidate.isValidate(47));
                } else {
                    groupitem.put("hide", !Util.null2String(HrmFieldGroupComInfo.getIsShow()).equals("1"));
                }
                groupitem.put("items", itemlist);
                lsGroup.add(groupitem);
                while (hfm.next()) {
                    int tmpviewattr = viewAttr;
                    String fieldName = hfm.getFieldname();
                    //qc20230816 查看工作信息过滤字段
                    int selectUserId = Integer.parseInt(id);
                    if (tmpviewattr == 1 && user.getUID()!=1 && "1".equals(isOpen) && !checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,2,selectUserId))continue;
                    String cusFieldname = "";
                    String fieldValue = "";
                    if (hfm.isBaseField(fieldName)) {
                        fieldValue = hfm.getHrmData(fieldName);
                        if (isAdd){
                            fieldValue = "";
                        }
                    } else {
                        fieldValue = Util.null2String(new EncryptUtil().decrypt("cus_fielddata","field" + hfm.getFieldid(),"HrmCustomFieldByInfoType",""+scopeId,cfm.getData("field" + hfm.getFieldid()),viewAttr==2,true));
                        if (isAdd){
                            fieldValue = "";
                        }
                        cusFieldname = "customfield" + hfm.getFieldid();
                        if (isAdd) cusFieldname = "customfield_3_" + hfm.getFieldid();
                    }

//                    if (fieldName.equals("workyear")) {//工龄司龄改为程序计算
//                        if (workstartdate.length() > 0) {
//                            fieldValue = df.format(DateUtil.dayDiff(workstartdate, today) / 365.0);
//                        } else {
//                            fieldValue = "";
//                        }
//                    }
//                    if (fieldName.equals("companyworkyear")) {//工龄司龄改为程序计算
//                        if (companystartdate.length() > 0) {
//                            fieldValue = df.format(DateUtil.dayDiff(companystartdate, today) / 365.0);
//                        } else {
//                            fieldValue = "";
//                        }
//                    }
                    if (!hfm.isUse() || (viewAttr != 1 && (fieldName.equals("workyear") || fieldName.equals("companyworkyear")))) {
                        HrmFieldBean hrmFieldBean = new HrmFieldBean();
                        hrmFieldBean.setFieldname(cusFieldname.length() > 0 ? cusFieldname : fieldName);
                        hrmFieldBean.setFieldhtmltype("1");
                        hrmFieldBean.setType("1");
                        hrmFieldBean.setFieldvalue(fieldValue);
                        hrmFieldBean.setIsFormField(true);
                        SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                        Map<String, Object> otherParams = new HashMap<String, Object>();
                        otherParams.put("hide", true);
                        searchConditionItem.setOtherParams(otherParams);
                        itemlist.add(searchConditionItem);
                        continue;
                    }
                    org.json.JSONObject hrmFieldConf = hfm.getHrmFieldConf(fieldName);
                    HrmFieldBean hrmFieldBean = new HrmFieldBean();
                    hrmFieldBean.setFieldid((String) hrmFieldConf.get("id"));
                    hrmFieldBean.setFieldname(cusFieldname.length() > 0 ? cusFieldname : fieldName);
                    hrmFieldBean.setFieldlabel(hfm.getLable());
                    hrmFieldBean.setFieldhtmltype((String) hrmFieldConf.get("fieldhtmltype"));
                    hrmFieldBean.setType((String) hrmFieldConf.get("type"));
                    hrmFieldBean.setDmlurl((String) hrmFieldConf.get("dmlurl"));
                    hrmFieldBean.setIssystem("" + (Integer) hrmFieldConf.get("issystem"));
                    hrmFieldBean.setFieldvalue(fieldValue);
                    hrmFieldBean.setIsFormField(true);
                    if (viewAttr == 2 && ((String) hrmFieldConf.get("ismand")).equals("1")) {
                        tmpviewattr = 3;
                        if (hrmFieldBean.getFieldhtmltype().equals("3")) {
                            hrmFieldBean.setRules("required|string");
//                            if (hrmFieldBean.getType().equals("2")||hrmFieldBean.getType().equals("161")||hrmFieldBean.getType().equals("162")) {
//                                hrmFieldBean.setRules("required|string");
//                            } else {
//                                hrmFieldBean.setRules("required|integer");
//                            }
                        } else if (hrmFieldBean.getFieldhtmltype().equals("4") ||
                                hrmFieldBean.getFieldhtmltype().equals("5")) {
                            hrmFieldBean.setRules("required|integer");
						} else if (hrmFieldBean.getFieldhtmltype().equals("1") && hrmFieldBean.getType().equals("2")) {
							hrmFieldBean.setRules("required|integer");
                        } else {
                            hrmFieldBean.setRules("required|string");
                        }
                    }
                    SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                    if (searchConditionItem == null) continue;
                    if (searchConditionItem.getBrowserConditionParam() != null) {
                        searchConditionItem.getBrowserConditionParam().setViewAttr(tmpviewattr);
                    }
                    searchConditionItem.setViewAttr(tmpviewattr);
                    //qc20230816 编辑工作信息过滤字段
                    if (tmpviewattr==2 && user.getUID()!=1 && "1".equals(isOpen)){
                        int setUserId = Integer.parseInt(id);
                        checkSetUtil.searchConditionItem(user.getUID(),fieldName,2,searchConditionItem,setUserId);
                    }
                    itemlist.add(searchConditionItem);
                }
                if (itemlist.size() == 0) lsGroup.remove(groupitem);
            }
            result.put("conditions", lsGroup);

            //明细信息
            List<Object> lsTable = new ArrayList<Object>();
            List<HrmFieldBean> titles = null;
            Map<String, Object> table = null;
            Map<String, Object> maptab = null;
            HrmFieldBean hrmFieldBean = null;
            List<Map<String, Object>> columns = null;
            List<Map<String, Object>> datas = null;
            Map<String, Object> data = null;
            result.put("tables", lsTable);

            HrmFieldDetailComInfo HrmFieldDetailComInfo = new HrmFieldDetailComInfo();
            HrmFieldManager hrmFieldManager = new HrmFieldManager();
            LinkedHashMap<String, List<HrmFieldBean>> detialTable = HrmFieldDetailComInfo.getDetialTable("" + scopeId, viewAttr, "20%");
            Iterator<Map.Entry<String, List<HrmFieldBean>>> entries = detialTable.entrySet().iterator();
            int idx = 49;
            while (entries.hasNext()) {
                Map.Entry<String, List<HrmFieldBean>> entry = entries.next();
                String tablename = entry.getKey();
                idx++;
                if (viewAttr == 2 && tablename.equals("HrmContract".toUpperCase())) continue;
                if (viewAttr == 2 && tablename.equals("HrmStatusHistory".toUpperCase())) continue;

                if (isAdd && (tablename.equals("HrmContract".toUpperCase()) || tablename.equals("HrmStatusHistory".toUpperCase()))) {
                    continue;
                }
                titles = entry.getValue();
                table = new HashMap<String, Object>();
                columns = HrmFieldUtil.getHrmDetailTable(titles, null, user);
                table.put("columns", columns);
                if (tablename.equals("HrmContract".toUpperCase()) || tablename.equals("HrmStatusHistory".toUpperCase())) {
                    table.put("editDisable", true);
                    datas = new ArrayList<Map<String, Object>>();
                    RecordSet rs = new RecordSet();
                    String sql = "";
                    if (tablename.equals("HrmContract".toUpperCase())) {
                        sql += " select * from " + tablename + " where ContractMan = " + id + " order by id ";
                    } else {
                        sql += " select * from " + tablename + " where resourceid = " + id + " order by changedate ";
                    }
                    rs.executeSql(sql);
                    while (rs.next()) {
                        data = new HashMap<String, Object>();
                        for (HrmFieldBean fieldInfo : titles) {
                            //必要的字段转换
                            String fieldname = getFieldName(tablename.toLowerCase(), fieldInfo.getFieldname());
                            String fieldvalue = "";
                            if (fieldname.equals("olddepid")) {
                                int type = Util.getIntValue(rs.getString("type_n"));
                                String olddepid = "oldjobtitleid";
                                if (type == 4) {
                                    olddepid = "newjobtitleid";
                                }
                                fieldvalue = new JobTitlesComInfo().getJobTitlesname(rs.getString(olddepid));
                            } else {
                                fieldvalue = rs.getString(fieldname);
                            }

                            if (fieldname.equals("contracttypeid")) {
                                fieldvalue = new ContractTypeComInfo().getContractTypename(fieldvalue);
                            } else if (fieldname.equals("operator")) {
                                fieldvalue = new ResourceComInfo().getLastname("" + Util.getIntValue(rs.getString("operator"), 0));
                            } else if (fieldname.equals("type_n")) {
                                fieldvalue = getHrmHistoryStatus(rs.getInt("type_n"), user);
                            }
                            data.put(fieldInfo.getFieldname(), fieldvalue);
                        }
                        datas.add(data);
                    }
                } else {
                    datas = new ArrayList<Map<String, Object>>();
                    RecordSet rs = new RecordSet();
                    String sql = "select * from " + tablename + " where resourceid = " + id + " order by id";
                    rs.executeSql(sql);
                    while (rs.next()) {
                        data = new HashMap<String, Object>();
                        for (HrmFieldBean fieldInfo : titles) {
                            //必要的字段转换
                            String fieldname = getFieldName(tablename.toLowerCase(), fieldInfo.getFieldname());
                            String fieldvalue = "";
                            if(!isAdd){
                                fieldvalue = rs.getString(fieldname);
                            }

                            if (fieldInfo.getFieldhtmltype().equals("1") && fieldInfo.getType().equals("1")) {
                                data.put(fieldInfo.getFieldname(), TextUtil.toBase64ForMultilang(fieldvalue));
                            } else if (FieldFactory.getFieldhtmltype(fieldInfo.getFieldhtmltype(), fieldInfo.getType()) == FieldType.BROWSER) {
                                String fieldid = Util.null2String(fieldInfo.getFieldid());//字段id
                                String fieldhtmltype = Util.null2String(fieldInfo.getFieldhtmltype());//字段类型
                                String type = Util.null2String(fieldInfo.getType());//字段二级类型（浏览框--单人力）
                                String dmlurl = Util.null2String(fieldInfo.getDmlurl());
                                String fieldshowname = hrmFieldManager.getFieldvalue(user, dmlurl, Util.getIntValue(fieldid), Util.getIntValue(fieldhtmltype), Util.getIntValue(type), fieldvalue, 0);
                                data.put(fieldInfo.getFieldname(), fieldvalue);
                                data.put(fieldInfo.getFieldname() + "span", fieldshowname);
                            } else {
                                data.put(fieldInfo.getFieldname(), fieldvalue);
                            }
                        }
                        datas.add(data);
                    }
                }

                table.put("datas", datas);
                table.put("rownum", tablename.toLowerCase() + "num");
                maptab = new Hashtable<String, Object>();
                String tablabel = HrmResourceDetailTab.HrmResourceDetailTabInfo.get(tablename.toUpperCase());
                maptab.put("tabname", SystemEnv.getHtmlLabelNames(tablabel, user.getLanguage()));
                maptab.put("hide", !HrmListValidate.isValidate(idx));
                maptab.put("tabinfo", table);
                lsTable.add(maptab);
            }

            //自定义信息
            RecordSet RecordSet = new RecordSet();
            CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();
            LinkedHashMap<String, String> ht = new LinkedHashMap<String, String>();
            RecordSet.executeSql("select id, formlabel,viewtype from cus_treeform where parentid=" + scopeId + " order by scopeorder");
            while (RecordSet.next()) {
                titles = new ArrayList<HrmFieldBean>();
                int subId = RecordSet.getInt("id");
                CustomFieldManager cfm2 = new CustomFieldManager("HrmCustomFieldByInfoType", subId);
                cfm2.getCustomFields();
                CustomFieldTreeManager.getMutiCustomData("HrmCustomFieldByInfoType", subId, Util.getIntValue(id, 0));
                int colcount1 = cfm2.getSize();
                int rowcount = 0;
                while (cfm2.next()) {
                    if (!cfm2.isUse()) continue;
                    rowcount++;
                }
                if (rowcount == 0) continue;
                cfm2.beforeFirst();
                if (colcount1 != 0) {
                    ht.put("cus_list_" + subId, RecordSet.getString("formlabel"));
                    int col = 0;
                    while (cfm2.next()) {
                        if (!cfm2.isUse()) continue;
                        col++;
                    }
                    cfm2.beforeFirst();
                    while (cfm2.next()) {
                        if (!cfm2.isUse()) continue;
                        int tmpviewattr = viewAttr;
                        //创建表头
                        String fieldname = "customfield" + cfm2.getId() + "_" + subId;
                        if (isAdd) fieldname = "customfield_3_" + cfm2.getId() + "_" + subId;
                        hrmFieldBean = new HrmFieldBean();
                        hrmFieldBean.setFieldid("" + cfm2.getId());
                        hrmFieldBean.setFieldname(fieldname);
                        hrmFieldBean.setFieldlabel(cfm2.getLable());
                        hrmFieldBean.setFieldhtmltype(cfm2.getHtmlType());
                        hrmFieldBean.setType("" + cfm2.getType());
                        hrmFieldBean.setDmlurl(cfm2.getDmrUrl());
                        if (viewAttr == 2 && cfm2.isMand()) {
                            tmpviewattr = 3;
                            hrmFieldBean.setRules("required|string");
                        }
                        hrmFieldBean.setViewAttr(tmpviewattr);

                        hrmFieldBean.setWidth("80%");
                        titles.add(hrmFieldBean);
                    }
                    table = new HashMap<String, Object>();
                    columns = HrmFieldUtil.getHrmDetailTable(titles, null, user);
                    table.put("columns", columns);

                    datas = new ArrayList<Map<String, Object>>();
                    cfm2.beforeFirst();
                    while (CustomFieldTreeManager.nextMutiData()) {
                        data = new HashMap<String, Object>();
                        while (cfm2.next()) {
                            if (!cfm2.isUse()) continue;
                            int fieldid = cfm2.getId();  //字段id
                            int type = cfm2.getType();
                            String dmlurl = cfm2.getDmrUrl();
                            int fieldhtmltype = Util.getIntValue(cfm2.getHtmlType());

                            String fieldname = "customfield" + cfm2.getId() + "_" + subId;
                            if (isAdd) fieldname = "customfield_3_" + cfm2.getId() + "_" + subId;
                            String fieldvalue = "";
                            if(!isAdd){
                                fieldvalue = Util.null2String(CustomFieldTreeManager.getMutiData("field" + fieldid));
                            }
                            data.put(fieldname, fieldvalue);
                            if (cfm2.getHtmlType().equals("1") && cfm2.getType() == 1) {
                                data.put(fieldname, TextUtil.toBase64ForMultilang(Util.null2String(fieldvalue)));
                            } else if (cfm2.getHtmlType().equals("3")) {
                                String fieldshowname = hfm.getFieldvalue(user, dmlurl, fieldid, fieldhtmltype, type, fieldvalue, 0);
                                data.put(fieldname, fieldvalue);
                                data.put(fieldname + "span", fieldshowname);
                            } else if (cfm2.getHtmlType().equals("4")) {
                                data.put(fieldname, fieldvalue.equals("1"));
                            } else if(cfm2.getHtmlType().equals("6")){
                                List<Object> filedatas = new ArrayList<Object>();
                                if(Util.null2String(fieldvalue).length()>0) {
                                    Map<String, Object> filedata = null;
                                    String[] tmpIds = Util.splitString(Util.null2String(fieldvalue), ",");
                                    for (int i = 0; i < tmpIds.length; i++) {
                                        String fileid = tmpIds[i];
                                        ImageFileManager manager = new ImageFileManager();
                                        manager.getImageFileInfoById(Util.getIntValue(fileid));
                                        String filename = manager.getImageFileName();
                                        String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
                                        filedata = new HashMap<String, Object>();
                                        filedata.put("acclink", "/weaver/weaver.file.FileDownload?fileid=" + fileid);
                                        filedata.put("fileExtendName", extname);
                                        filedata.put("fileid", fileid);
                                        filedata.put("filelink", "/spa/document/index2file.jsp?imagefileId=" + fileid + "#/main/document/fileView");
                                        filedata.put("filename", filename);
                                        filedata.put("filesize", manager.getImgsize());
                                        filedata.put("imgSrc", "");
                                        filedata.put("isImg", "");
                                        filedata.put("loadlink", "/weaver/weaver.file.FileDownload?fileid=" + fileid + "&download=1");
                                        filedata.put("showDelete", viewAttr==2);
                                        filedata.put("showLoad", "true");
                                        filedatas.add(filedata);
                                    }
                                }
                                data.put(fieldname, filedatas);
                            }
                        }
                        cfm2.beforeFirst();
                        datas.add(data);
                    }
                    table.put("datas", datas);
                    table.put("rownum", "nodesnum_" + subId);
                    maptab = new Hashtable<String, Object>();
                    RecordSet rs = new RecordSet();
                    String formlabel = "";
                    rs.executeSql("select id, formlabel from cus_treeform where parentid=" + scopeId + " and id=" + subId + " order by scopeorder");
                    if (rs.next()) {
                        formlabel = rs.getString("formlabel");
                    }
                    maptab.put("hide", RecordSet.getInt("viewtype") != 1);
                    maptab.put("tabname", formlabel);
                    maptab.put("tabinfo", table);
                    lsTable.add(maptab);
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return result;
    }

    /***
     * 新建人员工作信息
     * @param request
     * @param response
     * @return
     */
    public Map<String, String> addResourceWork(String id, HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> retmap = new HashMap<String, String>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            RecordSet rs = new RecordSet();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();

            SimpleBizLogger logger = new SimpleBizLogger();
            Map<String, Object> params = ParamUtil.request2Map(request);
            BizLogContext bizLogContext = new BizLogContext();
            bizLogContext.setLogType(BizLogType.HRM);//模块类型
            bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
            bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(15688, user.getLanguage()));
            bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_WORK);//当前小类型
            bizLogContext.setOperateType(BizLogOperateType.ADD);
            bizLogContext.setParams(params);//当前request请求参数
            logger.setUser(user);//当前操作人
            String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType", 3, "b");
            String mainSql = "select a.*" + (cusFieldNames.length() > 0 ? "," + cusFieldNames : "") + " from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=3 where a.id=" + id;
            logger.setMainSql(mainSql, "id");//主表sql
            logger.setMainPrimarykey("id");//主日志表唯一key
            logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
            logger.before(bizLogContext);//写入操作前日志

            String para = "";
            FileUpload fu = new FileUpload(request);
            //String id = Util.null2String(fu.getParameter("id"));
            String usekind = Util.fromScreen3(fu.getParameter("usekind"), user.getLanguage());
            String startdate = Util.fromScreen3(fu.getParameter("startdate"), user.getLanguage());
            String probationenddate = Util.fromScreen3(fu.getParameter("probationenddate"), user.getLanguage());
            String enddate = Util.fromScreen3(fu.getParameter("enddate"), user.getLanguage());

            para = "" + id + separator + usekind + separator + startdate + separator + probationenddate + separator + enddate;
            rs.executeProc("HrmResourceWorkInfo_Insert", para);
            //更新参加工作日期、入职日期、工龄、司龄
            updateWorkInfo(request, id, user);
            int userid = user.getUID();
            String userpara = "" + userid + separator + today;
            rs.executeProc("HrmResource_ModInfo", "" + id + separator + userpara);

            para = "" + id + separator + "0";
            int edurownum = Util.getIntValue(fu.getParameter("hrmeducationinfonum"), 0);
            for (int i = 0; i < edurownum; i++) {
                String school = Util.fromScreen3(fu.getParameter("school_" + i), user.getLanguage());
                String speciality = Util.fromScreen3(fu.getParameter("speciality_" + i), user.getLanguage());
                String edustartdate = Util.fromScreen3(fu.getParameter("edustartdate_" + i), user.getLanguage());
                String eduenddate = Util.fromScreen3(fu.getParameter("eduenddate_" + i), user.getLanguage());
                String educationlevel = Util.fromScreen3(fu.getParameter("educationlevel_" + i), user.getLanguage());
                String studydesc = Util.fromScreen3(fu.getParameter("studydesc_" + i), user.getLanguage());

                String info = school + speciality + edustartdate + eduenddate + educationlevel + studydesc;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + edustartdate + separator + eduenddate + separator + school + separator + speciality +
                            separator + educationlevel + separator + studydesc;
                    rs.executeProc("HrmEducationInfo_Insert", para);
                }
            }

            int lanrownum = Util.getIntValue(fu.getParameter("hrmlanguageabilitynum"), 0);
            for (int i = 0; i < lanrownum; i++) {
                String language = Util.fromScreen3(fu.getParameter("language_" + i), user.getLanguage());
                String level = Util.fromScreen3(fu.getParameter("level_n_" + i), user.getLanguage());
                String memo = Util.fromScreen3(fu.getParameter("memo_" + i), user.getLanguage());
                String info = language + memo;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + language + separator + level + separator + memo;
                    rs.executeProc("HrmLanguageAbility_Insert", para);
                }
            }

            int workrownum = Util.getIntValue(fu.getParameter("hrmworkresumenum"), 0);
            for (int i = 0; i < workrownum; i++) {
                String company = Util.fromScreen3(fu.getParameter("company_" + i), user.getLanguage());
                String workstartdate = Util.fromScreen3(fu.getParameter("workstartdate_" + i), user.getLanguage());
                String workenddate = Util.fromScreen3(fu.getParameter("workenddate_" + i), user.getLanguage());
                String jobtitle = Util.fromScreen3(fu.getParameter("jobtitle_" + i), user.getLanguage());
                String workdesc = Util.fromScreen3(fu.getParameter("workdesc_" + i), user.getLanguage());
                String leavereason = Util.fromScreen3(fu.getParameter("leavereason_" + i), user.getLanguage());

                String info = company + workstartdate + workenddate + jobtitle + workdesc + leavereason;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + workstartdate + separator + workenddate + separator + company + separator + jobtitle +
                            separator + workdesc + separator + leavereason;
                    rs.executeProc("HrmWorkResume_Insert", para);
                }
            }

            int trainrownum = Util.getIntValue(fu.getParameter("hrmtrainbeforeworknum"), 0);
            for (int i = 0; i < trainrownum; i++) {
                String trainname = Util.fromScreen3(fu.getParameter("trainname_" + i), user.getLanguage());
                String trainstartdate = Util.fromScreen3(fu.getParameter("trainstartdate_" + i), user.getLanguage());
                String trainenddate = Util.fromScreen3(fu.getParameter("trainenddate_" + i), user.getLanguage());
                String trainresource = Util.fromScreen3(fu.getParameter("trainresource_" + i), user.getLanguage());
                String trainmemo = Util.fromScreen3(fu.getParameter("trainmemo_" + i), user.getLanguage());

                String info = trainname + trainstartdate + trainenddate + trainresource + trainmemo;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + trainname + separator + trainresource + separator + trainstartdate + separator + trainenddate +
                            separator + trainmemo;

                    rs.executeProc("HrmTrainBeforeWork_Insert", para);
                }
            }

            int rewardrownum = Util.getIntValue(fu.getParameter("hrmrewardbeforeworknum"), 0);
            for (int i = 0; i < rewardrownum; i++) {
                String rewardname = Util.fromScreen3(fu.getParameter("rewardname_" + i), user.getLanguage());
                String rewarddate = Util.fromScreen3(fu.getParameter("rewarddate_" + i), user.getLanguage());
                String rewardmemo = Util.fromScreen3(fu.getParameter("rewardmemo_" + i), user.getLanguage());
                String info = rewardname + rewarddate + rewardmemo;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + rewardname + separator + rewarddate + separator + rewardmemo;

                    rs.executeProc("HrmRewardBeforeWork_Insert", para);
                }
            }

            int cerrownum = Util.getIntValue(fu.getParameter("hrmcertificationnum"), 0);
            for (int i = 0; i < cerrownum; i++) {
                String cername = Util.fromScreen3(fu.getParameter("cername_" + i), user.getLanguage());
                String cerstartdate = Util.fromScreen3(fu.getParameter("cerstartdate_" + i), user.getLanguage());
                String cerenddate = Util.fromScreen3(fu.getParameter("cerenddate_" + i), user.getLanguage());
                String cerresource = Util.fromScreen3(fu.getParameter("cerresource_" + i), user.getLanguage());

                String info = cername + cerstartdate + cerenddate + cerresource;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + cerstartdate + separator + cerenddate + separator + cername + separator + cerresource;

                    rs.executeProc("HrmCertification_Insert", para);
                }
            }

            // 工作信息不需要清理缓存 ResourceComInfo.removeResourceCache();
            //处理自定义字段 add by wjy
            CustomFieldTreeManager.editCustomDataE9Add("HrmCustomFieldByInfoType", 3, fu, Util.getIntValue(id, 0));
            CustomFieldTreeManager.editMutiCustomDataeE9Add("HrmCustomFieldByInfoType", 3, fu, Util.getIntValue(id, 0));

            rs.execute("update HrmResource set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);
            rs.execute("update HrmResourceManager set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);

            LogUtil.writeBizLog(logger.getBizLogContexts());

            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog("新建人员工作信息错误：" + e);
        }
        return retmap;
    }


    /**
     * 编辑人员工作信息
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> editResourceWork(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            //qc20230816 放开修改权限
            if (!HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user) && !"1".equals(isOpen)) {
                retmap.put("status", "-1");
                retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
                return retmap;
            }
            RecordSet rs = new RecordSet();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();

            String para = "";
            FileUpload fu = new FileUpload(request);
            String id = Util.null2String(fu.getParameter("id"));
            String usekind = Util.fromScreen3(fu.getParameter("usekind"), user.getLanguage());
            String startdate = Util.fromScreen3(fu.getParameter("startdate"), user.getLanguage());
            String probationenddate = Util.fromScreen3(fu.getParameter("probationenddate"), user.getLanguage());
            String enddate = Util.fromScreen3(fu.getParameter("enddate"), user.getLanguage());

            SimpleBizLogger logger = new SimpleBizLogger();
            Map<String, Object> params = ParamUtil.request2Map(request);
            BizLogContext bizLogContext = new BizLogContext();
            bizLogContext.setLogType(BizLogType.HRM);//模块类型
            bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
            bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(15688, user.getLanguage()));
            bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_WORK);//当前小类型
            bizLogContext.setParams(params);//当前request请求参数
            logger.setUser(user);//当前操作人
            String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType", 3, "b");
            String mainSql = "select a.*" + (cusFieldNames.length() > 0 ? "," + cusFieldNames : "") + " from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=3 where a.id=" + id;
            logger.setMainSql(mainSql, "id");//主表sql
            logger.setMainPrimarykey("id");//主日志表唯一key
            logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
            logger.before(bizLogContext);//写入操作前日志

            para = "" + id + separator + usekind + separator + startdate + separator + probationenddate + separator + enddate;
            rs.executeProc("HrmResourceWorkInfo_Insert", para);
            //更新参加工作日期、入职日期、工龄、司龄
            updateWorkInfo(request, id, user);

            int userid = user.getUID();
            String userpara = "" + userid + separator + today;
            rs.executeProc("HrmResource_ModInfo", "" + id + separator + userpara);

            String contractid = "";
            String sqltype = "select * from hrmcontract where exists (select * from HrmContractType where hrmcontract.contracttypeid=HrmContractType.id and HrmContractType.ishirecontract=1) and contractman=" + id + " order by contractenddate desc,contractstartdate desc";
            rs.executeSql(sqltype);
            if (rs.next()) {
                contractid = rs.getString("id");
                //根据qc212896 人事卡片合同日期修改的时候 同步更新合同类型为入职合同中 最新的一个合同的日期
                sqltype = "update HrmContract set contractstartdate = '" + startdate + "',contractenddate = '" + enddate + "',proenddate = '" + probationenddate + "' where id = " + contractid;
                rs.execute(sqltype);
            }

            String sql = "delete from HrmLanguageAbility where resourceid = " + id;
            rs.executeSql(sql);
            int lanrownum = Util.getIntValue(fu.getParameter("hrmlanguageabilitynum"), 0);
            for (int i = 0; i < lanrownum; i++) {
                String language = Util.fromScreen3(fu.getParameter("language_" + i), user.getLanguage());
                String level = Util.fromScreen3(fu.getParameter("level_n_" + i), user.getLanguage());
                String memo = Util.fromScreen3(fu.getParameter("memo_" + i), user.getLanguage());
                String info = language + memo;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + language + separator + level + separator + memo;
                    rs.executeProc("HrmLanguageAbility_Insert", para);
                }
            }

            sql = "delete from HrmEducationInfo where resourceid = " + id;
            rs.executeSql(sql);
            int edurownum = Util.getIntValue(fu.getParameter("hrmeducationinfonum"), 0);
            for (int i = 0; i < edurownum; i++) {
                String school = Util.fromScreen3(fu.getParameter("school_" + i), user.getLanguage());
                String speciality = Util.fromScreen3(fu.getParameter("speciality_" + i), user.getLanguage());
                String edustartdate = Util.fromScreen3(fu.getParameter("edustartdate_" + i), user.getLanguage());
                String eduenddate = Util.fromScreen3(fu.getParameter("eduenddate_" + i), user.getLanguage());
                String educationlevel = Util.fromScreen3(fu.getParameter("educationlevel_" + i), user.getLanguage());
                String studydesc = Util.fromScreen3(fu.getParameter("studydesc_" + i), user.getLanguage());
                String info = school + speciality + edustartdate + eduenddate + studydesc;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + edustartdate + separator + eduenddate + separator + school + separator + speciality +
                            separator + educationlevel + separator + studydesc;
                    rs.executeProc("HrmEducationInfo_Insert", para);
                }
            }

            int workrownum = Util.getIntValue(fu.getParameter("hrmworkresumenum"));
            sql = "delete from HrmWorkResume where resourceid = " + id;
            rs.executeSql(sql);
            for (int i = 0; i < workrownum; i++) {
                String company = Util.fromScreen3(fu.getParameter("company_" + i), user.getLanguage());
                String workstartdate = Util.fromScreen3(fu.getParameter("workstartdate_" + i), user.getLanguage());
                String workenddate = Util.fromScreen3(fu.getParameter("workenddate_" + i), user.getLanguage());
                String jobtitle = Util.fromScreen3(fu.getParameter("jobtitle_" + i), user.getLanguage());
                String workdesc = Util.fromScreen3(fu.getParameter("workdesc_" + i), user.getLanguage());
                String leavereason = Util.fromScreen3(fu.getParameter("leavereason_" + i), user.getLanguage());
                String info = company + workstartdate + workenddate + jobtitle + workdesc + leavereason;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + workstartdate + separator + workenddate + separator + company + separator + jobtitle +
                            separator + workdesc + separator + leavereason;
                    rs.executeProc("HrmWorkResume_Insert", para);
                }
            }

            int trainrownum = Util.getIntValue(fu.getParameter("hrmtrainbeforeworknum"), 0);
            sql = "delete from HrmTrainBeforeWork where resourceid = " + id;
            rs.executeSql(sql);
            for (int i = 0; i < trainrownum; i++) {
                String trainname = Util.fromScreen3(fu.getParameter("trainname_" + i), user.getLanguage());
                String trainstartdate = Util.fromScreen3(fu.getParameter("trainstartdate_" + i), user.getLanguage());
                String trainenddate = Util.fromScreen3(fu.getParameter("trainenddate_" + i), user.getLanguage());
                String trainresource = Util.fromScreen3(fu.getParameter("trainresource_" + i), user.getLanguage());
                String trainmemo = Util.fromScreen3(fu.getParameter("trainmemo_" + i), user.getLanguage());
                String info = trainname + trainstartdate + trainenddate + trainresource + trainmemo;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + trainname + separator + trainresource + separator + trainstartdate + separator + trainenddate +
                            separator + trainmemo;
                    rs.executeProc("HrmTrainBeforeWork_Insert", para);
                }
            }

            int cerrownum = Util.getIntValue(fu.getParameter("hrmcertificationnum"), 0);
            sql = "delete from HrmCertification where resourceid = " + id;
            rs.executeSql(sql);
            for (int i = 0; i < cerrownum; i++) {
                String cername = Util.fromScreen3(fu.getParameter("cername_" + i), user.getLanguage());
                String cerstartdate = Util.fromScreen3(fu.getParameter("cerstartdate_" + i), user.getLanguage());
                String cerenddate = Util.fromScreen3(fu.getParameter("cerenddate_" + i), user.getLanguage());
                String cerresource = Util.fromScreen3(fu.getParameter("cerresource_" + i), user.getLanguage());

                String info = cername + cerstartdate + cerenddate + cerresource;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + cerstartdate + separator + cerenddate + separator + cername + separator + cerresource;
                    rs.executeProc("HrmCertification_Insert", para);
                }
            }

            int rewardrownum = Util.getIntValue(fu.getParameter("hrmrewardbeforeworknum"), 0);
            sql = "delete from HrmRewardBeforeWork where resourceid = " + id;
            rs.executeSql(sql);
            for (int i = 0; i < rewardrownum; i++) {
                String rewardname = Util.fromScreen3(fu.getParameter("rewardname_" + i), user.getLanguage());
                String rewarddate = Util.fromScreen3(fu.getParameter("rewarddate_" + i), user.getLanguage());
                String rewardmemo = Util.fromScreen3(fu.getParameter("rewardmemo_" + i), user.getLanguage());
                String info = rewardname + rewarddate + rewardmemo;
                if (!info.trim().equals("")) {
                    para = "" + id + separator + rewardname + separator + rewarddate + separator + rewardmemo;
                    rs.executeProc("HrmRewardBeforeWork_Insert", para);
                }
            }

            /*工作信息中有参加工作日期以及入职日期，所以需要刷新人员缓存*/
            ResourceComInfo.updateResourceInfoCache(id);
            //处理自定义字段 add by wjy
            CustomFieldTreeManager.editCustomData("HrmCustomFieldByInfoType", 3, fu, Util.getIntValue(id, 0));

            CustomFieldTreeManager.setIsE9(true);
            CustomFieldTreeManager.editMutiCustomData("HrmCustomFieldByInfoType", 3, fu, Util.getIntValue(id, 0));

            rs.execute("update HrmResource set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);
            rs.execute("update HrmResourceManager set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);

            LogUtil.writeBizLog(logger.getBizLogContexts());

            HrmFaceCheckManager.sync(id, HrmFaceCheckManager.getOptUpdate(), "hrm_e9_HrmResourceWorkService_editResourceWork", HrmFaceCheckManager.getOaResource());


            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog("编辑人员工作信息错误：" + e);
            retmap.put("status", "-1");
        }
        return retmap;
    }

    public String getFieldName(String tablename, String fieldname) {
        if (tablename.equals("hrmeducationinfo")) {
            if (fieldname.equals("edustartdate")) {
                fieldname = "startdate";
            } else if (fieldname.equals("eduenddate")) {
                fieldname = "enddate";
            }
        } else if (tablename.equals("hrmworkresume")) {
            if (fieldname.equals("workstartdate")) {
                fieldname = "startdate";
            } else if (fieldname.equals("workenddate")) {
                fieldname = "enddate";
            }
        } else if (tablename.equals("hrmcertification")) {
            if (fieldname.equals("cername")) {
                fieldname = "certname";
            } else if (fieldname.equals("cerstartdate")) {
                fieldname = "datefrom";
            } else if (fieldname.equals("cerenddate")) {
                fieldname = "dateto";
            } else if (fieldname.equals("cerresource")) {
                fieldname = "awardfrom";
            }
        }
        return fieldname;
    }

    public String getHrmHistoryStatus(int type, User user) {
        String result = "";
        if (type == 1) {
            result = SystemEnv.getHtmlLabelName(6094, user.getLanguage());
        } else if (type == 2) {
            result = SystemEnv.getHtmlLabelName(6088, user.getLanguage());
        } else if (type == 3) {
            result = SystemEnv.getHtmlLabelName(6089, user.getLanguage());
        } else if (type == 4) {
            result = SystemEnv.getHtmlLabelName(6090, user.getLanguage());
        } else if (type == 5) {
            result = SystemEnv.getHtmlLabelName(6091, user.getLanguage());
        } else if (type == 6) {
            result = SystemEnv.getHtmlLabelName(6092, user.getLanguage());
        } else if (type == 7) {
            result = SystemEnv.getHtmlLabelName(6093, user.getLanguage());
        } else if (type == 8) {
            result = SystemEnv.getHtmlLabelName(15710, user.getLanguage());
        } else if (type == 9) {
            result = SystemEnv.getHtmlLabelName(32009, user.getLanguage());
        }
        return result;
    }

    public void updateWorkInfo(HttpServletRequest request, String id, User user) {
        try {
            RecordSet rs = new RecordSet();
            String sql = "";
            String companystartdate = Util.fromScreen3(request.getParameter("companystartdate"), user.getLanguage());
            String workstartdate = Util.fromScreen3(request.getParameter("workstartdate"), user.getLanguage());

            List<String> lsParams = new ArrayList<>();
            lsParams.add(companystartdate.length() == 0 ? null : companystartdate);
            lsParams.add(workstartdate.length() == 0 ? null : workstartdate);
            lsParams.add(id);
            sql = " update hrmresource set companystartdate=?,workstartdate=? where id=?";
            rs.executeUpdate(sql, lsParams);
            HrmDateCheck hrmDateCheck = new HrmDateCheck();
            hrmDateCheck.calWorkInfo(id);
        } catch (Exception e) {
            writeLog(e);
        }
    }

}
