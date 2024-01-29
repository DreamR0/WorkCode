package com.api.doc.detail.service;


import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.engine.common.entity.EncryptFieldEntity;
import com.engine.doc.util.DocEncryptUtil;
import com.engine.encrypt.biz.EncryptFieldConfigComInfo;
import org.apache.commons.lang.StringEscapeUtils;
import weaver.encrypt.EncryptUtil;
import weaver.general.GCONST;
import weaver.system.SystemComInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.api.doc.detail.util.*;
import com.api.doc.mobile.systemDoc.bean.DocoutShareManager;
import com.engine.doc.bean.DocMenuBean;
import com.engine.doc.util.DocMenuManager;
import com.engine.doc.util.IWebOfficeConf;
import org.apache.commons.lang3.StringUtils;

import com.api.browser.bean.BrowserBean;
import com.api.browser.bean.BrowserValueInfo;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.service.Browser;
import com.api.browser.service.BrowserValueInfoService;
import com.api.browser.service.impl.DocDummyService;
import com.api.browser.util.ConditionType;
import com.api.doc.detail.bean.CustomType;
import com.api.doc.detail.bean.DocParam;
import com.api.doc.detail.bean.DocParamForm;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.api.doc.edit.util.EditConfigUtil;
import com.api.doc.search.bean.RightMenu;
import com.api.doc.search.util.BrowserType;
import com.api.doc.search.util.DbType;
import com.api.doc.search.util.DocCondition;
import com.api.doc.search.util.DocSptm;
import com.api.doc.search.util.PatternUtil;
import com.api.doc.search.util.RightMenuType;
import com.engine.doc.entity.DocApproveWfDetailBean;
import com.engine.doc.util.CheckPermission;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.odoc.util.OdocRequestdocUtil;
import com.google.common.collect.Lists;

import weaver.docs.util.DocTriggerUtils;
import weaver.conn.RecordSet;
import weaver.cpt.capital.CapitalComInfo;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.docs.DocDetailLog;
import weaver.docs.category.DocTreeDocFieldComInfo;
import weaver.docs.category.SecCategoryApproveWfManager;
import weaver.docs.category.SecCategoryComInfo;
import weaver.docs.category.SecCategoryDocPropertiesComInfo;
import weaver.docs.category.security.MultiAclManager;
import weaver.docs.docs.CustomFieldManager;
import weaver.docs.docs.DocApproveWfManager;
import weaver.docs.docs.DocCoder;
import weaver.docs.docs.DocComInfo;
import weaver.docs.docs.DocIdUpdate;
import weaver.docs.docs.DocImageManager;
import weaver.docs.docs.DocManager;
import weaver.docs.docs.DocViewer;
import weaver.docs.docs.util.DesUtils;
import weaver.docs.share.DocShareUtil;
import weaver.file.FileManage;
import weaver.file.FileUpload;
import weaver.file.ImageFileManager;
import weaver.fullsearch.util.SearchUpdateType;
import weaver.fullsearch.util.SearchUpdateUtil;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.proj.Maint.ProjectInfoComInfo;
import weaver.soa.workflow.request.MainTableInfo;
import weaver.soa.workflow.request.Property;
import weaver.soa.workflow.request.RequestInfo;
import weaver.soa.workflow.request.RequestService;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.language.LanguageComInfo;
import weaver.systeminfo.setting.HrmUserSettingComInfo;
import weaver.workflow.field.BrowserComInfo;
import weaver.workflow.workflow.WorkflowVersion;

public class DocSaveService {

    public static String DOC_CONTENT = "doccontent";
    public static String IS_CREATE = "iscreate";

    public static String CUSTOM_FIELD = "customfield";

    /**
     * 获取文档属性
     *
     * @author wangqs
     */
    public Map<String, Object> getParams(int docid, int secid, User user, Map<String, String> otherParams) throws Exception {
        return getParams(docid, secid, user, otherParams, false);
    }

    public Map<String, Object> getParams(int docid, int secid, User user,
                                         Map<String, String> otherParams, boolean isMobile) throws Exception {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        if (docid == 0 && secid == 0) {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(19327, user
                    .getLanguage()));
            return apidatas;
        }

        otherParams = otherParams == null ? new HashMap<String, String>()
                : otherParams;

        ResourceComInfo rci = new ResourceComInfo();
        RecordSet rs = new RecordSet();
        Map<String, String> doc = new HashMap<String, String>();
        Map<String, String> customFileds = new HashMap<String, String>();
        int docstatus = -100;
        String maindoc= "";
        String maindocsubject= "";
        String themeshowpic="";
        if (docid > 0) {
            rs.execute("select *from DocDetail where id=" + docid);
            if (!rs.next()) {
                apidatas.put("status", -1);
                apidatas.put("msg", SystemEnv.getHtmlLabelName(23230, user
                        .getLanguage()));
                return apidatas;
            }else{
                docstatus = Util.getIntValue(rs.getString("docstatus"),-100);
                maindoc = Util.null2String(rs.getString("maindoc"));
                themeshowpic = Util.null2String(rs.getString("themeshowpic"));
                maindocsubject = DocSecretLevelUtil.getDocName(maindoc);
            }
            for (String column : rs.getColumnName()) {
                doc.put(column.toUpperCase(), Util.null2String(rs
                        .getString(column)));
            }
            secid = Util.getIntValue(doc.get("SECCATEGORY"), 0);

            //自定义属性
            rs.execute("select *from cus_fielddata where scope='DocCustomFieldBySecCategory' and scopeid=" + secid + " and id=" + docid);
            if (rs.next()) {
                for (String column : rs.getColumnName()) {
                    customFileds.put(column.toUpperCase(), Util.null2String(rs.getString(column)));
                }
            }
        }

        String hasasset = "";
        String assetlabel = "";
        String hasitems = "";
        String itemlabel = "";
        String hashrmres = "";
        String hrmreslabel = "";
        String hascrm = "";
        String crmlabel = "";
        String hasproject = "";
        String projectlabel = "";
        String hasfinance = "";
        String financelabel = "";
        String isSetShare = "";

        String replyable = "";
        String readoptercanprint = "";
        String publishable = "";
        boolean canShowDocMain = false; // 是否有摘要
		boolean allowscheduledrelease = false; // 允许定时发布
        boolean docMainShow = false; // 是否显示摘要(发布类型为主页时)

        String doctype = Util.null2String(otherParams.get("doctype")); // 文档类型；
        doctype = doctype.isEmpty() ? ".html" : doctype;

        if (docid > 0)
            doctype = "." + doc.get("DOCEXTENDNAME");

        /**
         * html显示模板  1
         * html编辑模板  2
         * word显示模板  3
         * word编辑模板  4
         * excel编辑模板  6
         * wps显示模板  7
         * wps编辑模板  8
         * */
        int tempMouldType = 0;//2：html编辑模版
        if (doctype.equals(".html") || doctype.isEmpty()) {
            tempMouldType = docid > 0 ? 1 : 2;
        } else if (doctype.equals(".doc") || doctype.equals(".docx")) {
            tempMouldType = docid > 0 ? 3 : 4;//4：WORD编辑模版
        } else if (doctype.equals(".xls") || doctype.equals(".xlsx")) {
            tempMouldType = docid > 0 ? 0 : 6;//6：EXCEL编辑模版
        } else if (doctype.equals(".wps")) {
            tempMouldType = docid > 0 ? 7 : 8;//8：WPS文字编辑模版
        } else if (doctype.equals(".et")) {
            tempMouldType = 10;//10：et表格编辑模版
        }

        rs.execute("select *from DocSecCategory where id=" + secid);

        if (rs.next()) {
            hasasset = Util.toScreen(rs.getString("hasasset"), user
                    .getLanguage());
            assetlabel = Util.toScreen(rs.getString("assetlabel"), user
                    .getLanguage());
            hasitems = Util.toScreen(rs.getString("hasitems"), user
                    .getLanguage());
            itemlabel = Util.toScreenToEdit(rs.getString("itemlabel"), user
                    .getLanguage());
            hashrmres = Util.toScreen(rs.getString("hashrmres"), user
                    .getLanguage());
            hrmreslabel = Util.toScreenToEdit(rs.getString("hrmreslabel"), user
                    .getLanguage());
            hascrm = Util.toScreen(rs.getString("hascrm"), user.getLanguage());
            crmlabel = Util.toScreenToEdit(rs.getString("crmlabel"), user
                    .getLanguage());
            hasproject = Util.toScreen(rs.getString("hasproject"), user
                    .getLanguage());
            projectlabel = Util.toScreenToEdit(rs.getString("projectlabel"),
                    user.getLanguage());
            hasfinance = Util.toScreen(rs.getString("hasfinance"), user
                    .getLanguage());
            financelabel = Util.toScreenToEdit(rs.getString("financelabel"),
                    user.getLanguage());
            isSetShare = Util.null2String(rs.getString("isSetShare"));

            replyable = Util.null2String(rs.getString("replyable"));
            readoptercanprint = Util.null2String(rs
                    .getString("readoptercanprint"));
            publishable = Util.null2String(rs.getString("publishable"));
			try{
				allowscheduledrelease="1".equals(Util.null2String(rs.getString("puboperation")));
			}catch(Exception e){}
        }


        /** 文档属性 start * */
		RecordSet docPropCacheRs = new RecordSet();
		docPropCacheRs.executeSql("select * from DocSecCategoryDocProperty where secCategoryId= "+secid+"  order by viewindex ");
        List<DocParam> params = new ArrayList<DocParam>();

        /**是否开启回复*/
        if (replyable.equals("1")) {
            SystemEnv.getHtmlLabelName(18641, user.getLanguage());
        }
        //remindinput

        if (readoptercanprint.equals("2") && docid > 0) {
            String value = "";
            DocParamForm docParamForm = new DocParamForm();

            DocParam docParam = new DocParam();
            docParam.setLabel(SystemEnv.getHtmlLabelName(19462, user.getLanguage()));
            docParam.setColumn("2");
            String defaultValue = doc.get("READOPTERCANPRINT") != null ? doc
                    .get("READOPTERCANPRINT")
                    : "0";
            docParamForm = DocParamUtil.getDocParam(
                    DocParamItem.READOPTER_CANPRINT, user, defaultValue);
            docParamForm.setViewAttr(2);
            docParam.setValue("");
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        } else if (readoptercanprint.equals("1")) {
            //readoptercanprint=1

        } else if (readoptercanprint.equals("0")) {
            //readoptercanprint=0

        }

        DocComInfo dci = new DocComInfo();
        DepartmentComInfo dept = new DepartmentComInfo();
        DocSptm ds = new DocSptm();
        /**
         * 浏览按钮
         * */
        RecordSet rsBrowser = new RecordSet();
        rsBrowser.executeQuery("select type,clazz from wf_browser_config");
        Map<String, String> broserMap = new HashMap<String, String>();
        while (rsBrowser.next()) {
            broserMap.put(rsBrowser.getString("type"), rsBrowser.getString("clazz"));
        }
        
        int kk = 0;
        boolean secretFlag = CheckPermission.isOpenSecret();
        while (docPropCacheRs.next()) {
            int docPropLabelid = Util.getIntValue(docPropCacheRs.getString("labelid")); // 自定义属性
            // 标签id
			String docPropCustomName ="";
			if(user.getLanguage()==7){
				docPropCustomName = Util.null2String(docPropCacheRs.getString("customName"));
			}else if(user.getLanguage()==8){
				docPropCustomName = Util.null2String(docPropCacheRs.getString("customNameEng"));
			}else if(user.getLanguage()==9){
				docPropCustomName = Util.null2String(docPropCacheRs.getString("customNameTran"));
			}
            int docPropIsCustom = Util.getIntValue(docPropCacheRs.getString("isCustom"));; // 是否自定义
            int docPropType = Util.getIntValue(docPropCacheRs.getString("type")); // 属性类型
            int docPropMustInput = Util.getIntValue(docPropCacheRs.getString("mustInput")); // 必填

            String docPropScope = Util.null2String(docPropCacheRs.getString("scope"));
            int docPropScopeId = Util.getIntValue(docPropCacheRs.getString("scopeid"));
            int docPropFieldid =  Util.getIntValue(docPropCacheRs.getString("fieldid"));

            boolean mouldEdit = true;

            if (Util.getIntValue(docPropCacheRs.getString("secCategoryId")) != secid)
                continue; // 是否是该目录
            if (Util.getIntValue(docPropCacheRs.getString("visible")) == 0)
                continue; // 是否显示
            if (docPropLabelid == MultiAclManager.MAINCATEGORYLABEL
                    || docPropLabelid == MultiAclManager.SUBCATEGORYLABEL)
                continue;

            if (docPropType == 1 || docPropType == 11 || docPropType == 13
                    || docPropType == 14 || docPropType == 15
                    || docPropType == 16 || docPropType == 17
                    || docPropType == 18 || docPropType == 20)
                continue;
            if(docPropType == 26 && !secretFlag){ //文档密级
				continue;
			}

            String label = "";
            if (!docPropCustomName.equals("")&&(!"0".equals(docPropCustomName))) {
                label = docPropCustomName;
            } else if (docPropIsCustom != 1) {
                label = SystemEnv.getHtmlLabelName(docPropLabelid, user
                        .getLanguage());
            }else if(docPropIsCustom == 1){
                    rs.executeQuery("select fieldlabel from cus_formdict where id="+docPropFieldid);
                    if(rs.next())
                        label = Util.null2String(rs.getString("fieldlabel"));
                    if(label.equals(""))label = "field"+docPropFieldid;

            }

            String value = "";
            DocParamForm docParamForm = new DocParamForm();
            boolean isFull = Util.getIntValue(docPropCacheRs.getString("columnWidth")) == 2;

            DocParam docParam = new DocParam();
            docParam.setLabel(label);
            docParam.setColumn(isFull ? "2" : "1");

            boolean fillIn = true;

            switch (docPropType) {
                case 1: // 1 文档标题
                    break;
                case 2: // 2 文档编号
                    value = doc.get("DOCCODE");
                    break;
                case 3: // 发布
                    if (!publishable.trim().equals("")
                            && !publishable.trim().equals("0")) {
                        canShowDocMain = true;
                        String defaultValue = doc.get("DOCPUBLISHTYPE") != null ? doc
                                .get("DOCPUBLISHTYPE")
                                : "2";
                        docMainShow = "2".equals(defaultValue);

                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();

                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(58, user.getLanguage())));//文档
                        options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(227, user.getLanguage()), true));//主页
                        options.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(229, user.getLanguage())));//标题

                        docParamForm = DocParamUtil.getDocParam(
                                DocParamItem.DOC_PUBLISH_TYPE, user, defaultValue, options);
                    } else {
                        value = SystemEnv.getHtmlLabelName(58, user.getLanguage());
                    }
                    break;
                case 4:// 4 文档版本
                    value = docid > 0 ? dci.getEditionView(docid) : value;
                    break;
                case 5:// 5 文档状态
                    value = docid > 0 ? dci.getStatusView(docid, user) : SystemEnv
                            .getHtmlLabelName(220, user.getLanguage());
                    break;
                case 6:// 6 主目录
                    break;
                case 7:// 7 分目录
                    break;
                case 8:// 8 子目录
                    SecCategoryComInfo scci = new SecCategoryComInfo();
                    if (docid > 0) {
                        value = scci.getAllParentName("" + secid, true);
                        value = Util.replace(value, "&amp;quot;", "\"", 0);
                        value = Util.replace(value, "&quot;", "\"", 0);
                        value = Util.replace(value, "&lt;", "<", 0);
                        value = Util.replace(value, "&gt;", ">", 0);
                        value = Util.replace(value, "&apos;", "'", 0);
                    } else {
                        docParamForm = DocParamUtil.getDocParam(
                                DocParamItem.SEC_CATEGORY, user, secid + "");
                        BrowserBean browserBean = new BrowserBean(
                                BrowserType.CATEGORY);
                        browserBean.setHasAdvanceSerach(false);
                        browserBean.setQuickSearchName("categoryname");
                        browserBean.setTitle(SystemEnv.getHtmlLabelNames(
                                DocCondition.SEC_CATEGORY.getLanguage(), user
                                        .getLanguage()));
                        browserBean.setValue(secid + "");
                        browserBean.setViewAttr(3);

                        List<Map<String, Object>> categorys = new ArrayList<Map<String, Object>>();
                        Map<String, Object> category = new HashMap<String, Object>();
                        category.put("id", secid + "");
                        String name = scci.getAllParentName("" + secid, true);
                        name = Util.replace(name, "&amp;quot;", "\"", 0);
                        name = Util.replace(name, "&quot;", "\"", 0);
                        name = Util.replace(name, "&lt;", "<", 0);
                        name = Util.replace(name, "&gt;", ">", 0);
                        name = Util.replace(name, "&apos;", "'", 0);
                        category.put("name", name);
                        categorys.add(category);
                        browserBean.setReplaceDatas(categorys);
                        browserBean.getDataParams().put(IS_CREATE, "1");
                        browserBean.getCompleteParams().put(IS_CREATE, "1");
                        docParamForm.setBrowserConditionParam(browserBean);
                        //docParamForm.setMustInput(true);
                        //docParamForm.setViewAttr(3);

                        docPropMustInput = 1;
                    }
                    break;
                case 9:// 9 部门

                    String deptid = docid > 0 ? doc.get("DOCDEPARTMENTID") : (user
                            .getUserDepartment() + "");

                    String deptname = dept.getDepartmentname(deptid);

                    if (deptname != null && !deptname.isEmpty()) {
					/*value = "<a href='/hrm/company/HrmDepartmentDsp.jsp?id="
							+ deptid + "' target='_blank'>"
							+ Util.toScreen(deptname, user.getLanguage())
							+ "</a>";*/
                        value = ds.getDepartmentLink(deptid, deptname);
                        if(isMobile){
                            value = deptname;
                        }
                    }
                    break;
                case 10:// 10 模版
                    int docmouldid = 0;
                    List<String> selectMouldList = new ArrayList<String>();
                    int selectMouldType = 0;
                    int selectDefaultMould = 0;

                    boolean isTemporaryDoc = !""
                            .equals(doc.get("INVALIDATIONDATE")) && docid > 0;

                    //if ("1".equals(doc.get("DOCTYPE")) || "html".equals(doctype)) {
                    if (tempMouldType > 0) {
                        weaver.docs.mouldfile.DocMouldComInfo dmcEdit = new weaver.docs.mouldfile.DocMouldComInfo();  //获取编辑模板
                        weaver.docs.mould.DocMouldComInfo dmcShow = new weaver.docs.mould.DocMouldComInfo();  //获取编辑模板

                        rs.execute("select t1.* from DocSecCategoryMould t1 where t1.secCategoryId =" + secid +
                                " and t1.mouldType=" + tempMouldType + " order by t1.id");
                        while (rs.next()) {
                            String moduleid = rs.getString("mouldId");
                            String modulebind = rs.getString("mouldBind");
                            int isDefault = Util.getIntValue(rs.getString("isDefault"), 0);

                            if (isTemporaryDoc) {//失效文档
                                if (Util.getIntValue(modulebind, 1) == 3) {//临时文档绑定
                                    selectMouldType = 3;
                                    selectDefaultMould = Util.getIntValue(moduleid);
                                    selectMouldList = new ArrayList<String>();
                                    selectMouldList.add(moduleid);
                                    mouldEdit = false;
                                    break;
                                } else if (Util.getIntValue(modulebind, 1) == 1 && isDefault == 1) {	//可选择、默认
                                    if (selectMouldType == 0) {
                                        selectMouldType = 1;
                                        selectDefaultMould = Util.getIntValue(moduleid);
                                    }
                                    selectMouldList.add(moduleid);
                                } else {	//可选择、正常文档绑定
                                    if (Util.getIntValue(modulebind, 1) != 2) //不是正常文档绑定
                                        selectMouldList.add(moduleid);	//可选择
                                }

                            } else {	//新建、正常文档
                                if (Util.getIntValue(modulebind, 1) == 2) {	//正常文档绑定
                                    selectMouldType = 2;
                                    selectDefaultMould = Util.getIntValue(moduleid);
                                    selectMouldList = new ArrayList<String>();
                                    selectMouldList.add(moduleid);
                                    mouldEdit = false;
                                    break;
                                } else if (Util.getIntValue(modulebind, 1) == 1 && isDefault == 1) {//可选择、默认
                                    selectMouldType = 1;
                                    selectDefaultMould = Util.getIntValue(moduleid);
                                    selectMouldList.add(moduleid);
                                } else {	//可选择、临时文档绑定
                                    if (Util.getIntValue(modulebind, 1) != 3)	//可选择
                                        selectMouldList.add(moduleid);
                                }
                            }
                        }

					/*if (docmouldid <= 0) {
						MouldManager mouldManager = new MouldManager();
						docmouldid = mouldManager.getDefaultMouldId();
					}*/

                        if (selectMouldType > 0) {
                            docmouldid = selectDefaultMould;
                        }

                        if (doc.get("SELECTEDPUBMOULDID") != null && Util.getIntValue(doc.get("SELECTEDPUBMOULDID")) > 0) {
                            docmouldid = Util.getIntValue(doc.get("SELECTEDPUBMOULDID"));
                        }

                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();

                        if (selectMouldType < 2) {
                            options.add(new SearchConditionOption("-1", ""));
                        }

                        for (int i = 0; i < selectMouldList.size(); i++) {
                            String moduleid = selectMouldList.get(i);
                            String modulename = docid > 0 ? dmcShow.getDocMouldname(moduleid) : dmcEdit.getDocMouldname(moduleid);
                            String mType = docid > 0 ? dmcShow.getDocMouldType(moduleid) : dmcEdit.getDocMouldType(moduleid);
                            String mouldTypeName = "";
                            if ((mType.equals("") || mType.equals("0")
                                    || mType.equals("1")) && doctype.equals(".html")) {
                                mouldTypeName = "HTML";
                            } else if (mType.equals("2") && (doctype.equals(".doc") || doctype.equals("docx"))) {
                                mouldTypeName = "WORD";
                            } else if (mType.equals("3") && (doctype.equals(".xls") || doctype.equals("xlsx"))) {
                                mouldTypeName = "EXCEL";
                            } else if (mType.equals("4") && doctype.equals(".wps")) {
                                mouldTypeName = SystemEnv.getHtmlLabelName(22359, user.getLanguage());
                            } else if (mType.equals("5") && doctype.equals(".et")) {
                                mouldTypeName = SystemEnv.getHtmlLabelName(24545, user.getLanguage());
                            } else {
                                continue;
                            }
                            boolean isselect = false;
                            if (docmouldid == Util.getIntValue(moduleid))
                                isselect = true;
                            options.add(new SearchConditionOption(moduleid,
                                    modulename + "(" + mouldTypeName + ")",
                                    isselect));
                        }
                        String defalut = "";
                        for (SearchConditionOption op : options) {
                            if (op.isSelected()) {
                                defalut = op.getKey();
                            }
                        }
                        docParamForm = DocParamUtil.getDocParam(
                                DocParamItem.DOC_MOULD, user, defalut, options);
                    }
                    break;
                case 11:// 11 语言
                    if (docid > 0) {
                        LanguageComInfo lci = new LanguageComInfo();
                        value = lci.getLanguagename(doc.get("DOCLANGURAGE"));
                    }
                    break;
                case 12:// 12 关键字
                    docParamForm = DocParamUtil.getDocParam(DocParamItem.KEYWORD,
                            user, doc.get("KEYWORD"));
                    break;
                case 13: // 13 创建
                    if (docid > 0) {
                        value = DocDetailService.getUserLink(doc
                                        .get("DOCCREATERID"), doc.get("DOCCREATERTYPE"),
                                user);
                        value += "&nbsp;" + doc.get("DOCCREATEDATE") + "&nbsp;"
                                + doc.get("DOCCREATETIME");
                        if (isMobile){
                            value=rci.getResourcename(doc.get("DOCCREATERID"))+" "+doc.get("DOCCREATEDATE")+" "+doc.get("DOCCREATETIME");
                        }
                    }
                    break;
                case 14:// 14 修改
                    if (docid > 0) {
                        value = DocDetailService.getUserLink(doc
                                .get("DOCLASTMODUSERID"), doc
                                .get("DOCLASTMODUSERTYPE"), user);
                        value += "&nbsp;" + doc.get("DOCLASTMODDATE") + "&nbsp;"
                                + doc.get("DOCLASTMODTIME");
                        if(isMobile){
                            value=rci.getResourcename(doc.get("DOCLASTMODUSERID"))+" "+doc.get("DOCLASTMODDATE")+" "+doc.get("DOCLASTMODTIME");
                        }
                    }
                    break;
                case 15:// 15 批准
                    if (docid > 0
                            && Util.getIntValue(doc.get("DOCAPPROVEUSERID"), 0) != 0) {
                        value = DocDetailService.getUserLink(doc
                                .get("DOCAPPROVEUSERID"), doc
                                .get("DOCAPPROVEUSERTYPE"), user);
                        value += "&nbsp;" + doc.get("DOCAPPROVEDATE") + "&nbsp;"
                                + doc.get("DOCAPPROVETIME");
                        if(isMobile){
                            value=rci.getResourcename(doc.get("DOCAPPROVEUSERID"))+" "+ doc.get("DOCAPPROVEDATE")+" "+doc.get("DOCAPPROVETIME");
                        }
                    }
                    break;
                case 16:// 16 失效
                    if (docid > 0
                            && Util.getIntValue(doc.get("DOCINVALUSERID"), 0) != 0) {
                        value = DocDetailService.getUserLink(doc
                                        .get("DOCINVALUSERID"),
                                doc.get("DOCINVALUSERTYPE"), user);
                        value += "&nbsp;" + doc.get("DOCINVALDATE") + "&nbsp;"
                                + doc.get("DOCINVALTIME");
                        if(isMobile){
                            value=rci.getResourcename(doc.get("DOCINVALUSERID"))+" "+ doc.get("DOCINVALDATE")+" "+doc.get("DOCINVALTIME");
                        }
                    }
                    break;
                case 17:// 17 归档
                    if (docid > 0
                            && Util.getIntValue(doc.get("DOCARCHIVEUSERID"), 0) != 0) {
                        value = DocDetailService.getUserLink(doc
                                .get("DOCARCHIVEUSERID"), doc
                                .get("DOCARCHIVEUSERTYPE"), user);
                        value += "&nbsp;" + doc.get("DOCARCHIVEDATE") + "&nbsp;"
                                + doc.get("DOCARCHIVETIME");
                        if(isMobile){
                            value=rci.getResourcename(doc.get("DOCARCHIVEUSERID"))+" "+ doc.get("DOCARCHIVEDATE")+" "+doc.get("DOCARCHIVETIME");
                        }
                    }
                    break;
                case 18:// 18 作废
                    if (docid > 0
                            && Util.getIntValue(doc.get("DOCCANCELUSERID"), 0) != 0) {
                        value = DocDetailService.getUserLink(doc
                                .get("DOCCANCELUSERID"), doc
                                .get("DOCCANCELUSERTYPE"), user);
                        value += "&nbsp;" + doc.get("DOCCANCELDATE") + "&nbsp;"
                                + doc.get("DOCCANCELTIME");
                        if(isMobile){
                            value= rci.getResourcename(doc.get("DOCCANCELUSERID"))+" "+ doc.get("DOCCANCELDATE")+" "+doc.get("DOCCANCELTIME");
                        }
                    }
                    break;
                case 19:// 19 主文档
                    docParamForm = DocParamUtil.getDocParam(DocParamItem.MAIN_DOC,
                            user, "-1");
                    BrowserBean browserBean = new BrowserBean(
                            BrowserType.SIMPLE_DOC);
                    docParamForm.setBrowserConditionParam(browserBean);
                    docParamForm.setSecretLimit(true);

                    BrowserBean mainDoc = new BrowserBean(BrowserType.SIMPLE_DOC);
                    mainDoc.setValue((maindoc.isEmpty()?"-1":maindoc));
                    mainDoc.setPageSize(10);
                    List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
                    Map<String, Object> _doc = new HashMap<String, Object>();
                    docs.add(_doc);
                    _doc.put("id", maindoc.isEmpty()?"-1":maindoc);
                    _doc.put("name",maindoc.isEmpty()?SystemEnv.getHtmlLabelName(390563, user
                            .getLanguage()):maindocsubject);//当前文档
                    mainDoc.setReplaceDatas(docs);
                    docParamForm.setBrowserConditionParam(mainDoc);
                    break;
                case 20:// 20 被引用列表
                    break;
                case 21:// 21 文档所有者
                    if (docid > 0 || user.getType() != 0) {
                        value = DocDetailService.getUserLink(doc.get("OWNERID"),
                                doc.get("OWNERTYPE"), user);
                        if(isMobile){
                            value= rci.getResourcename(doc.get("OWNERID"));
                        }
                    } else {
                        docParamForm = DocParamUtil.getDocParam(
                                DocParamItem.OWNER_ID, user, user.getUID() + "");
                        BrowserBean bb = new BrowserBean(BrowserType.USER);
                        bb.setValue(user.getUID() + "");
                        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
                        Map<String, Object> _user = new HashMap<String, Object>();
                        users.add(_user);
                        _user.put("id", user.getUID() + "");
                        _user.put("name", rci.getResourcename(user.getUID() + ""));
                        bb.setReplaceDatas(users);
                        docParamForm.setBrowserConditionParam(bb);
                    }
                    break;
                case 22:// 22 失效时间
                    docParamForm = DocParamUtil.getDocParam(
                            DocParamItem.INVALIDATION_DATE, user, doc
                                    .get("INVALIDATIONDATE"));
                    break;
                case 24:// 24 虚拟目录
                    String dummyIds = "";
                    if (docid > 0) {
                        String strSql = "select catelogid from DocDummyDetail where docid="
                                + docid;
                        rs.execute(strSql);
                        while (rs.next()) {
                            dummyIds += "," + Util.null2String(rs.getString(1));
                        }
                    } else {
                        String strSql = "select defaultDummyCata from DocSecCategory where id="
                                + secid;
                        rs.execute(strSql);
                        if (rs.next()) {
                            dummyIds = Util.null2String(rs.getString(1));
                        }
                    }

                    dummyIds = dummyIds.startsWith(",") ? dummyIds.substring(1)
                            : dummyIds;
                    dummyIds = dummyIds.endsWith(",") ? dummyIds.substring(0,
                            dummyIds.length() - 1) : dummyIds;
                    String _ids = "";
                    for (String s : dummyIds.split(",")) {
                        if (Util.getIntValue(s, -1) > 0) {
                            _ids += "," + s;
                        }
                    }
                    _ids = _ids.length() > 0 ? _ids.substring(1) : _ids;
                    dummyIds = _ids;

                    docParamForm = DocParamUtil.getDocParam(
                            DocParamItem.TREE_DOC_FIELD_ID, user, dummyIds);
                    BrowserBean bb = new BrowserBean(BrowserType.DUMMY);
                    bb.setIsSingle(false);
                    bb.setIsMultCheckbox(true);
                    bb.setHasAdvanceSerach(false);
                    bb.setTitle(SystemEnv.getHtmlLabelNames(
                            DocCondition.TREE_DOC_FIELD_ID.getLanguage(), user
                                    .getLanguage()));
                    bb.setValue(dummyIds);

                    if (!dummyIds.isEmpty()) {
                        List<Map<String, Object>> dummyCates = new ArrayList<Map<String, Object>>();
                        DocTreeDocFieldComInfo dtdfc = new DocTreeDocFieldComInfo();
                        for (String s : dummyIds.split(",")) {
                            Map<String, Object> _dummyCate = new HashMap<String, Object>();
                            dummyCates.add(_dummyCate);
                            if(isMobile){
                                DocDummyService docDummyService = new DocDummyService();
                                _dummyCate.put("id", s);
                                _dummyCate.put("name", docDummyService.getDummyNameById(s));
                            }else {
                                _dummyCate.put("id", s);
                                _dummyCate.put("name", dtdfc.getMultiTreeDocFieldNameOther(s));
                            }

                        }

                        bb.setReplaceDatas(dummyCates);

                    }
                    docParamForm.setBrowserConditionParam(bb);
                    break;
                case 25:// 可打印份数

                    docParamForm = DocParamUtil.getDocParam(
                            DocParamItem.CAN_PRINTED_NUM, user, doc
                                    .get("CANPRINTEDNUM"));

                    break;
                case 26 : //密级等级
                	HrmClassifiedProtectionBiz hcpb = new HrmClassifiedProtectionBiz();
                	String defauleValue = doc.get(DocParamItem.SECRET_LEVEL.getName().toUpperCase());
                	if(docid > 0 && docstatus>0 && docstatus!=9){
                	    value = DocSecretLevelUtil.takeSecretLevelDefaultValue(Util.null2s(defauleValue,DocManager.DEFAILT_SECRET_LEVEL+""),user,docid+"",true);
//                		value = hcpb.getResourceSecLevelShowName(Util.getIntValue(defauleValue,DocManager.DEFAILT_SECRET_LEVEL) + "", user.getLanguage() + "");
                	}else{
                		String _secretLevel = otherParams.get("_secretLevel");
                		String moudleFrom = Util.null2String(otherParams.get("moudleFrom"));
	                	List<SearchConditionOption> secretOptions = hcpb.getResourceOptionListByUser(user);
	                	int viewAttr = 2;
	                	if(!moudleFrom.isEmpty() && Util.getIntValue(_secretLevel) > -1){
	                		boolean flag = false;
	                		viewAttr = 1;
	                		mouldEdit = false;
                			for(SearchConditionOption secretOption : secretOptions){
                				if(_secretLevel.equals(secretOption.getKey())){
                					secretOption.setSelected(true);
                					defauleValue = _secretLevel;
                					flag = true;
                					break;
                				}
                			}
                			if(!flag && secretOptions.size() > 0){
                				secretOptions.get(0).setSelected(true);
                				defauleValue = secretOptions.get(0).getKey();
                			}
                		}else if(((docstatus<=0 && docstatus!=-100) || docstatus==9) && docid>0){
                            boolean flag = false;
                            _secretLevel = Util.getIntValues(DocSecretLevelUtil.takeSecretLevelbyDocid(docid+""));
                            viewAttr = 2;
                            mouldEdit = false;
                            for(SearchConditionOption secretOption : secretOptions){
                                if(_secretLevel.equals(secretOption.getKey())){
                                    secretOption.setSelected(true);
                                    defauleValue = _secretLevel;
                                    flag = true;
                                    break;
                                }
                            }
                            if(!flag && secretOptions.size() > 0){
                                secretOptions.get(0).setSelected(true);
                                defauleValue = secretOptions.get(0).getKey();
                            }
                        }else{
                			if((defauleValue == null || defauleValue.isEmpty()) && secretOptions.size() > 0){
                				secretOptions.get(0).setSelected(true);
                				defauleValue = secretOptions.get(0).getKey();
                			}
                		}
	                	docParamForm = DocParamUtil.getDocParam(
	                            DocParamItem.SECRET_LEVEL, user, defauleValue,secretOptions);
	                	docParamForm.setViewAttr(viewAttr);
                	}
					break;
                case 27:// 可打印份数
                    docParamForm = DocParamUtil.getDocParam(
                            DocParamItem.DOC_UPLOAD_PIC, user, doc
                                    .get("THEMESHOWPIC"));
                    
                    docParamForm.setCodeValue(DocDownloadCheckUtil.EncodeFileid(doc.get("THEMESHOWPIC"),user));
                    
					break;
                case 0: // 自定义
                    CustomFieldManager cfm = new CustomFieldManager(docPropScope, docPropScopeId);
                    String dbname = "";
                    //if (docid > 0) {
                        cfm.getCustomFields();
                        while (cfm.next()) {
                            if (cfm.getId() == docPropFieldid) {
                                dbname = cfm.getFieldDBName();
                                break;
                            }
                        }
                    //}
                    cfm.getCustomFields(docPropFieldid);
                    String _value = Util.null2String(customFileds.get(dbname.toUpperCase()));
                    _value = Util.StringReplace(_value, "\n", "<br>");
                    _value = PatternUtil.formatJson2Js(_value);
                    if (cfm.next()) {
                        if (cfm.getHtmlType().equals("1")) {
                        	
                        	_value = Util.null2String(new EncryptUtil().decrypt(DocEncryptUtil.CUS_FIELDDATA,dbname,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM,""+secid,_value,true,true));
                        	
                            if(isMobile){
                                if (cfm.getType() == 1) {    //字符串
                                    docParamForm = new DocParamForm(ConditionType.INPUT, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                } else if (cfm.getType() == 2) {  // 整数
                                    docParamForm = new DocParamForm(ConditionType.INPUT, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                    // docParamForm.setCustomType(CustomType.INPUT);
                                } else if (cfm.getType() == 3) { //浮点数
                                    docParamForm = new DocParamForm(ConditionType.INPUT, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                    // docParamForm.setCustomType(CustomType.INPUT);
                                    String flength = cfm.getFieldDbType().substring(cfm.getFieldDbType().indexOf(",")+1,cfm.getFieldDbType().indexOf(")"));
                                    docParamForm.setFloatPoint(Util.getIntValue(flength,3));   //浮点数增加小数位数
                                }
                            } else {
                                if (cfm.getType() == 1) {    //字符串
                                    docParamForm = new DocParamForm(ConditionType.INPUT, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                } else if (cfm.getType() == 2) {  // 整数
                                    docParamForm = new DocParamForm(ConditionType.CUSTOM, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                    docParamForm.setCustomType(CustomType.INPUT_INT);
                                } else if (cfm.getType() == 3) { //浮点数
                                    docParamForm = new DocParamForm(ConditionType.CUSTOM, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                    docParamForm.setCustomType(CustomType.INPUT_FLOAT);
                                    String flength = cfm.getFieldDbType().substring(cfm.getFieldDbType().indexOf(",")+1,cfm.getFieldDbType().indexOf(")"));
                                    docParamForm.setFloatPoint(Util.getIntValue(flength,3));   //浮点数增加小数位数
                                }
                            }
                        } else if (cfm.getHtmlType().equals("2")) { //文本
                        	_value = Util.null2String(new EncryptUtil().decrypt(DocEncryptUtil.CUS_FIELDDATA,dbname,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM,""+secid,_value,true,true));
                            //docParamForm = new DocParamForm(ConditionType.INPUT,new String[]{CUSTOM_FIELD + cfm.getId()},_value);
                            docParamForm = new DocParamForm(ConditionType.TEXTAREA, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                        } else if (cfm.getHtmlType().equals("3")) {

                            docParamForm = new DocParamForm();
                            docParamForm.setDomkey(new String[]{CUSTOM_FIELD + cfm.getId()});
                            docParamForm.setDefaultValue(_value);

                            BrowserBean customBrowser = new BrowserBean();

                            BrowserComInfo bci = new BrowserComInfo();
                            String fieldtype = String.valueOf(cfm.getType());
                            String url = bci.getBrowserurl(fieldtype); // 浏览按钮弹出页面的url
                            boolean isSingle = false;
                            String paraName = "selectids";
                            if ("135".equals(fieldtype)) {
                                paraName = "projectids";
                            }
                            //文档浏览按钮和流程浏览按钮，选择数据时要受到密级的限制
                            if("16".equals(fieldtype) || "9".equals(fieldtype) || "152".equals(fieldtype) || "37".equals(fieldtype)){
                                docParamForm.setSecretLimit(true);
                            }
                            String _url = url;
                            try {
                                _url = url.substring(url.indexOf("?url") + 4);
                            } catch (Exception e) {
                            }
                            if (_url.indexOf("?") != -1) {
                                url += "&" + paraName + "=";
                            } else {
                                url += "?" + paraName + "=";
                            }
                            String fielddbtype = Util.null2String(cfm.getFieldDbType());
                            if (fielddbtype.equals("int") || fielddbtype.equals("integer")) {
                                isSingle = true;
                            }
                            customBrowser.setIsSingle(isSingle);

                            //已实现的自定义浏览按钮
                            if (broserMap.get(fieldtype) != null) {
                                customBrowser.setType(fieldtype);
                                //customBrowser.getDataParams().put("type", fielddbtype);  //QC611162 暂时不传该参数
                                customBrowser.setViewAttr(docPropMustInput == 1 ? 3 : 2);
                                
                                if("161".equals(fieldtype) || "162".equals(fieldtype)){
                                	
                                	fielddbtype = "browser." + fielddbtype.replace("browser.", "");
                                	
                                	customBrowser.getDataParams().put("type", fielddbtype);
                                	customBrowser.getDataParams().put("fielddbtype",fielddbtype);
                                	customBrowser.getCompleteParams().put("type", fielddbtype);
                                	customBrowser.getCompleteParams().put("fielddbtype", fielddbtype);

                                	customBrowser.getConditionDataParams().put("type", fielddbtype);
                                	customBrowser.getConditionDataParams().put("fielddbtype", fielddbtype);
                                	
                                	customBrowser.getDestDataParams().put("type", fielddbtype);
                                	customBrowser.getDestDataParams().put("fielddbtype", fielddbtype);

                                    docParamForm.setIsMultCheckbox(true);//161或者162设置为最新多选
                                }
                                
                                try {
                                    Browser browser = (Browser) Class.forName(broserMap.get(fieldtype)).newInstance();
                                    browser.setBrowserType(fieldtype);
                                    browser.setUser(user);
                                    //Map<String,Object> selectid = new HashMap<String,Object>();
                                    //selectid.put(BrowserConstant.BROWSER_MULT_DEST_SELECTIDS, _value);
                                    //Map<String,Object> selectData = browser.getMultBrowserDestData(selectid);

                                    BrowserValueInfoService browserValue = new BrowserValueInfoService();
                                    //String fieldType,String fielddbtype,int fieldid,String fieldValue, int languageId, String modeId, String dataId
                                    List<BrowserValueInfo> browserValueInfos = browserValue.getBrowserValueInfo(fieldtype, fielddbtype, 0, _value, 7, "", "");
                                    
                                    List<Map<String, Object>> nameList = new ArrayList<Map<String, Object>>();
                                    List<Map<String, Object>> nameList2 = new ArrayList<Map<String, Object>>();
                                    if (browserValueInfos != null && browserValueInfos.size() > 0) {
                                        for (BrowserValueInfo browserValueInfo : browserValueInfos) {
                                            Map<String, Object> name = new HashMap<String, Object>();
                                            Map<String, Object> name2 = new HashMap<String, Object>();
                                            name.put("id", browserValueInfo.getId());
                                            name.put("name", browserValueInfo.getName());
                                            nameList.add(name);
                                            
                                            name2.put("id",browserValueInfo.getId());
                                            name2.put("name", browserValueInfo.getName());
                                            nameList2.add(name2);
                                        }
                                    }
                                    customBrowser.setReplaceDatas(nameList);
                                    //customBrowser.setAppendDatas(nameList2);

                                } catch (Exception e) {

                                }

                                docParamForm.setConditionType(ConditionType.BROWSER);
                                docParamForm.setBrowserConditionParam(customBrowser);
                                break;
                            }

                            _value = !"".equals(_value) && docid > 0 ?  _value : "";
                            if (fieldtype.equals("2")) {  //日期

                                docParamForm = new DocParamForm(ConditionType.DATEPICKER, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                            }else if(fieldtype.equals("290")){
                                docParamForm = new DocParamForm(ConditionType.DATEPICKER, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                docParamForm.setFormat("yyyy-MM-dd HH:mm");
                                Map<String,String> showTime = new HashMap<>();
                                showTime.put("format","HH:mm");
                                docParamForm.setShowTime(showTime);
                                docParamForm.setMode("datetime");
                            }else if(fieldtype.equals("402")){
                                docParamForm = new DocParamForm(ConditionType.DATEPICKER, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                Map<String,String> showTime = new HashMap<>();
                                if(!isMobile){
                                    docParamForm.setFormat("yyyy");
                                }else{
                                    docParamForm.setFormat("");
                                }
                                showTime.put("format","yyyy");
//                                docParamForm.setShowTime(showTime);
                                docParamForm.setMode("year");
                                docParamForm.setPlaceholder(SystemEnv.getHtmlLabelName(526306, user
                                        .getLanguage()));
                            }else if(fieldtype.equals("403")){
                                docParamForm = new DocParamForm(ConditionType.DATEPICKER, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                                Map<String,String> showTime = new HashMap<>();
                                if(!isMobile){
                                    docParamForm.setFormat("yyyy-MM");
                                }else{
                                    docParamForm.setFormat("");
                                }
                                showTime.put("format","yyyy-MM");
//                                docParamForm.setShowTime(showTime);
                                docParamForm.setMode("month");
                                docParamForm.setPlaceholder(SystemEnv.getHtmlLabelName(126137, user
                                        .getLanguage()));
                            }else { //时间
                                docParamForm = new DocParamForm(ConditionType.TIMEPICKER, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                            }

                        } else if (cfm.getHtmlType().equals("4")) {
                            //checkbox
                            docParamForm = new DocParamForm(ConditionType.CHECKBOX, new String[]{CUSTOM_FIELD + cfm.getId()}, _value);
                        } else if (cfm.getHtmlType().equals("5")) {
                            cfm.getSelectItem(cfm.getId());  //select
                            if(docid<=0){
                                rs.execute("select * from cus_selectitem where doc_isdefault=1 and fieldid=" + cfm.getId());
                                if (rs.next()){
                                    _value=Util.null2String(rs.getString("selectvalue"));
                                }
                            }

                            List<SearchConditionOption> _options = new ArrayList<SearchConditionOption>();
                            _options.add(new SearchConditionOption("", "", _value.isEmpty()));
                            while (cfm.nextSelect()) {
                                _options.add(new SearchConditionOption(cfm.getSelectValue(), cfm.getSelectName(), _value.equals(cfm.getSelectValue())));
                            }
                            docParamForm = new DocParamForm(ConditionType.SELECT, new String[]{CUSTOM_FIELD + cfm.getId()}, _value, _options);
                        }
                    }
                    break;

                default:
                    fillIn = false;
                    break;
            }

            if (!fillIn)
                continue;

            value = PatternUtil.formatJson2Js(value);

            docParamForm.setMustInput(docPropMustInput == 1);
            if (docPropMustInput == 1) {
                docParamForm.setViewAttr(3);
            } else if (docParamForm.getDomkey() != null) {
                docParamForm.setViewAttr(mouldEdit ? 2 : 1);  //mouldEdit-模板是否允许编辑
            } else {
                docParamForm = new DocParamForm(ConditionType.INPUT, new String[]{"_docParam" + kk}, value);
                kk++;
                docParamForm.setViewAttr(1);
            }

            docParam.setValue("");
            docParam.setParamForm(docParamForm);

            params.add(docParam);
        }
        //移动端需要的属性
        if (isMobile) {
            //附件上传信息
            Map<String, String> upload = EditConfigUtil.getFileUpload(user.getLanguage(), null, 0, secid);
            String uploadSize = PatternUtil.formatJson2Js(upload.get("maxUploadSize"));
            String uploadType = PatternUtil.formatJson2Js(upload.get("limitType"));
            uploadType = (uploadType.equals("")) ? "*.*" :uploadType;
            //附件大小
            DocParam uploadSizeParm = getDocParm(DocParamItem.UPLOAD_SIZE, user, uploadSize, 19998, false, 1, true);
            params.add(uploadSizeParm);
            //附件类型
            DocParam uploadTypeParm = getDocParm(DocParamItem.UPLOAD_TYPE, user, uploadType, 387953, false, 1, true);
            params.add(uploadTypeParm);
        }

        /** 文档属性 end * */

        /** * 摘要 start ** */

        if (canShowDocMain) {

            String doccontent = "";
            if (docid > 0) {
                if (doc.get("DOCCONTENT") == null) {
                    rs.executeQuery("select doccontent from DocDetailContent where docid=" + docid);
                    if (rs.next()) {
                        doccontent = rs.getString("doccontent");
                    }
                } else {
                    doccontent = doc.get("DOCCONTENT");
                }
            }

            String docMain = "";
            int tmppos = doccontent.indexOf("!@#$%^&*");
            if (tmppos != -1) {
                docMain = doccontent.substring(0, tmppos);

                docMain = Util.replace(docMain, "quot;", "\"", 0);
                docMain = Util.replace(docMain, "&quot;", "\"", 0);
                docMain = Util.replace(docMain, "&lt;", "<", 0);
                docMain = Util.replace(docMain, "&gt;", ">", 0);
                docMain = Util.replace(docMain, "&apos;", "'", 0);
                docMain = Util.replace(docMain, "&amp;", "&", 0);
            }

            DocParam docParam = new DocParam();
            docParam.setLabel(SystemEnv.getHtmlLabelName(341, user
                    .getLanguage()));
            docParam.setColumn("2");
            DocParamForm docParamForm = DocParamUtil.getDocParam(
                    DocParamItem.DOC_MAIN, user, PatternUtil.formatJson2Js(docMain));
            docParamForm.setMustInput(true);
            docParamForm.setViewAttr(3);
            docParamForm.setShow(docMainShow);
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        }
        /** * 摘要 end ** */

        /** * 类型 start ** */

        // 人力资源
        if (!hashrmres.trim().equals("") && !hashrmres.trim().equals("0")) {

            DocParam docParam = new DocParam();
            docParam.setLabel(!hrmreslabel.trim().equals("") ? hrmreslabel
                    : SystemEnv.getHtmlLabelName(179, user.getLanguage()));
            docParam.setColumn("1");
            DocParamForm docParamForm = DocParamUtil.getDocParam(
                    DocParamItem.DOC_HRM, user, doc.get("HRMRESID"));
            docParamForm.setMustInput(hashrmres.equals("2"));
            docParamForm.setViewAttr(hashrmres.equals("2") ? 3 : 2);
            docParamForm.setShow(true);
            BrowserBean bb = new BrowserBean(BrowserType.HRM);
            bb.setViewAttr(docParamForm.getViewAttr());

            if (doc.get("HRMRESID") != null && !doc.get("HRMRESID").isEmpty()) {
                List<Map<String, Object>> hrms = new ArrayList<Map<String, Object>>();
                Map<String, Object> hrm = new HashMap<String, Object>();
                hrm.put("id", doc.get("HRMRESID"));
                hrm.put("name", rci.getResourcename(doc.get("HRMRESID")));
                hrms.add(hrm);
                bb.setReplaceDatas(hrms);
            }

            docParamForm.setBrowserConditionParam(bb);
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        }
        // 资产
        if (!hasasset.trim().equals("0") && !hasasset.trim().equals("")) {
            DocParam docParam = new DocParam();
            docParam.setLabel(!assetlabel.trim().equals("") ? assetlabel
                    : SystemEnv.getHtmlLabelName(535, user.getLanguage()));
            docParam.setColumn("1");
            DocParamForm docParamForm = DocParamUtil.getDocParam(
                    DocParamItem.DOC_ASSET, user, doc.get("ASSETID"));
            docParamForm.setMustInput(hasasset.equals("2"));
            docParamForm.setViewAttr(hasasset.equals("2") ? 3 : 2);
            docParamForm.setShow(true);
            BrowserBean bb = new BrowserBean(BrowserType.ASSET);
            bb.setViewAttr(docParamForm.getViewAttr());

            if (doc.get("ASSETID") != null && !doc.get("ASSETID").isEmpty()) {
                List<Map<String, Object>> assets = new ArrayList<Map<String, Object>>();
                Map<String, Object> asset = new HashMap<String, Object>();
                asset.put("id", doc.get("ASSETID"));
                CapitalComInfo cci = new CapitalComInfo();
                asset.put("name", cci.getCapitalname(doc.get("ASSETID")));
                assets.add(asset);
                bb.setReplaceDatas(assets);
            }

            docParamForm.setBrowserConditionParam(bb);
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        }
        // 客户
        if (!hascrm.trim().equals("0") && !hascrm.trim().equals("")) {
            DocParam docParam = new DocParam();
            docParam.setLabel(!crmlabel.trim().equals("") ? crmlabel
                    : SystemEnv.getHtmlLabelName(147, user.getLanguage()));
            docParam.setColumn("1");
            DocParamForm docParamForm = DocParamUtil.getDocParam(
                    DocParamItem.DOC_CRM, user, doc.get("CRMID"));
            docParamForm.setMustInput(hascrm.equals("2"));
            docParamForm.setViewAttr(hascrm.equals("2") ? 3 : 2);
            docParamForm.setShow(true);
            BrowserBean bb = new BrowserBean(BrowserType.CRM);
            bb.setViewAttr(docParamForm.getViewAttr());

            if (doc.get("CRMID") != null && !doc.get("CRMID").isEmpty()) {
                List<Map<String, Object>> crms = new ArrayList<Map<String, Object>>();
                Map<String, Object> crm = new HashMap<String, Object>();
                crm.put("id", doc.get("CRMID"));
                CustomerInfoComInfo cici = new CustomerInfoComInfo();
                crm.put("name", cici.getCustomerInfoname(doc.get("CRMID")));
                crms.add(crm);
                bb.setReplaceDatas(crms);
            }

            docParamForm.setBrowserConditionParam(bb);
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        }
        // 项目
        if (!hasproject.trim().equals("0") && !hasproject.trim().equals("")) {
            DocParam docParam = new DocParam();
            docParam.setLabel(!projectlabel.trim().equals("") ? projectlabel
                    : SystemEnv.getHtmlLabelName(101, user.getLanguage()));
            docParam.setColumn("1");
            DocParamForm docParamForm = DocParamUtil.getDocParam(
                    DocParamItem.DOC_PROJECT, user, doc.get("PROJECTID"));
            docParamForm.setMustInput(hasproject.equals("2"));
            docParamForm.setViewAttr(hasproject.equals("2") ? 3 : 2);
            docParamForm.setShow(true);
            BrowserBean bb = new BrowserBean(BrowserType.PROJECT);
            bb.setViewAttr(docParamForm.getViewAttr());

            if (doc.get("PROJECTID") != null && !doc.get("PROJECTID").isEmpty()) {
                List<Map<String, Object>> projects = new ArrayList<Map<String, Object>>();
                Map<String, Object> project = new HashMap<String, Object>();
                project.put("id", doc.get("PROJECTID"));
                ProjectInfoComInfo pici = new ProjectInfoComInfo();
                project.put("name", pici.getProjectInfoname(doc.get("PROJECTID")));
                projects.add(project);
                bb.setReplaceDatas(projects);
            }

            docParamForm.setBrowserConditionParam(bb);
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        }
        /** * 类型 end ** */
	    /** * 定时发布日期 start ** */
		allowscheduledrelease=false;
		if (allowscheduledrelease) {
			RecordSet rslog=new RecordSet();
			
            DocParam docParam = new DocParam();
            docParam.setLabel(SystemEnv.getHtmlLabelName(524676, user.getLanguage()));//定时发布日期			
            docParam.setColumn("1");			
            DocParamForm docParamForm = DocParamUtil.getDocParam(DocParamItem.SCHEDULEDRELEASE_DATE, user, doc.get("SCHEDULEDRELEASEDATE"));
			docParamForm.setMustInput(true);
            docParamForm.setViewAttr(3);
            docParamForm.setShow(true);
            docParam.setParamForm(docParamForm);
            params.add(docParam);
        }
		/** * 定时发布日期 end ** */
        apidatas.put("status", 1);
        apidatas.put("data", params);
        apidatas.put("themeshowpic", themeshowpic);
        return apidatas;
    }

    private DocParam getDocParm(DocParamItem docParamItem, User user, String defaultValue, int labelid, boolean mustInput, int viewAttr, boolean show) {
        DocParam docParam = new DocParam();
        docParam.setLabel(SystemEnv.getHtmlLabelName(labelid, user.getLanguage()));
        docParam.setColumn("1");
        DocParamForm docParamForm = DocParamUtil.getDocParam(
                docParamItem, user, defaultValue);
        docParamForm.setMustInput(mustInput);
        docParamForm.setViewAttr(viewAttr);
        docParamForm.setShow(show);
        docParam.setParamForm(docParamForm);
        return docParam;

    }

    /**
     * 获取html编辑模板
     */
    public String getMouldContent(User user, int mouldid) {
        //Map<String,Object> apidatas = new HashMap<String,Object>();

        //apidatas.put("status", 1);
        //Map<String,String> data = new HashMap<String,String>();
        //data.put("doccontent","");

        if (mouldid > 0) {
            try {
                weaver.docs.mouldfile.MouldManager mouldManager = new weaver.docs.mouldfile.MouldManager();
                mouldManager.setId(mouldid);
                mouldManager.getMouldInfoById();
                String mouldtext = mouldManager.getMouldText();
                
                int tmppos = mouldtext.indexOf("<title>");
		        while (tmppos != -1) {
		            int endpos = mouldtext.lastIndexOf("</title>", tmppos);
		            if ((endpos - tmppos) > 1) {
		                String tmpcontent = mouldtext.substring(0, tmppos);
		                tmpcontent += tmpcontent.substring(endpos + 8);
		                mouldtext = tmpcontent;
		            } else if(endpos < 0){
		            	mouldtext = mouldtext.replace("<title>","");
		            }
		            tmppos = mouldtext.indexOf("<title>");
		        }
                
                mouldManager.closeStatement();
                return PatternUtil.formatJson2Js(mouldtext);
                //data.put("doccontent", PatternUtil.formatJson2Js(mouldtext));
            } catch (Exception e) {
            }
        }
        //apidatas.put("data", data);

        return "";
    }
    
    public int getHtmlMouldIdBySec(int secid){
    	if(secid > 0){
    		RecordSet rs = new RecordSet();
    		rs.executeQuery("select mouldId,mouldBind,isDefault from DocSecCategoryMould where secCategoryId = ? and mouldType=2 order by id ",secid);
    		weaver.docs.mouldfile.DocMouldComInfo dmcEdit = new weaver.docs.mouldfile.DocMouldComInfo();  //获取编辑模板
    		int selectMouldType = 0;
			int mouldid = 0;
    		while(rs.next()){
				String moduleid=rs.getString("mouldId");
				String mType = dmcEdit.getDocMouldType(moduleid);
				String modulebind = rs.getString("mouldBind");
				int isDefault = Util.getIntValue(rs.getString("isDefault"),0);
				if(Util.getIntValue(modulebind,1)==2){
				    selectMouldType = 2;
				    mouldid = Util.getIntValue(moduleid);
			    } else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
				    if(selectMouldType==0){
				        selectMouldType = 1;
				        mouldid = Util.getIntValue(moduleid);
				    }
			    }
			}
    		return mouldid;
    	}
    	return 0;
    	
    }

    public Map<String, Object> doSave(User user, Map<String, String> paramMap, String clientip) throws Exception {
        return doSave(user,paramMap,clientip,null);
    }
    /**
     * 新增、修改文档
     *
     * @author wangqs
     */
    public Map<String, Object> doSave(User user, Map<String, String> paramMap, String clientip,HttpServletRequest request) throws Exception {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        int docid = Util.getIntValue(paramMap.get("docid"), 0);
        if (docid == 0) {
            docid = Util.getIntValue(paramMap.get("id"), 0);
        }
		if(docid>0){
			String docids = DocShareUtil.docRightFilter(user,docid + "");
			if(docid != Util.getIntValue(docids)){
				apidatas.put("status", -1);
				apidatas.put("api_status", true);
				apidatas.put("msg", SystemEnv.getHtmlLabelName( 384301, user
						.getLanguage()));//暂无权限查看此文档
				return apidatas;
			}
		}
        //是否是office
        boolean isOffice = "1".equals(paramMap.get("isOffice"));
        boolean saveBJCA = "1".equals(paramMap.get("saveBJCA"));

        RecordSet rs = new RecordSet();
        DocManager dm = new DocManager();


        if (docid <= 0 && Util.getIntValue(paramMap.get(DocParamItem.SEC_CATEGORY.getName())) <= 0) {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(500495,user.getLanguage()));//"目录不能为空!"
            return apidatas;
        }

		String f_weaver_belongto_userid = paramMap.get("f_weaver_belongto_userid");
		//String f_weaver_belongto_usertype = paramMap.get("f_weaver_belongto_usertype");
		if(f_weaver_belongto_userid != null && Util.getIntValue(f_weaver_belongto_userid) > 0){
			HrmUserSettingComInfo userSetting=new HrmUserSettingComInfo();
			String belongtoshow = userSetting.getBelongtoshowByUserId(user.getUID()+"");
			String belongtoids = "," + user.getBelongtoids() + ",";
			String account_type = user.getAccount_type();
			if(belongtoshow.equals("1") && account_type.equals("0") &&
					belongtoids.contains("," + f_weaver_belongto_userid + ",")){
				user = new User(Util.getIntValue(f_weaver_belongto_userid));
			}
		}

        // 是否是新增
        boolean isadd = docid <= 0;

        boolean needAddNewEdition = false;
        int olddocid = docid;
        int oldsecid = 0;
        int docEditionId = -1;
        int docEdition = -1;
        String oldstatus = "";
        String secretValidity="";
        String fileids = Util.null2String(paramMap.get("fileids"));
        String mainimagefile = Util.null2s(paramMap.get("mainimagefile"),"");
        String deleteaccessory = "";
        if(!mainimagefile.isEmpty()){ //判断主附件是否是最新版本，不是的话更新为最新版本
            String versionSql = "select imagefileid from docimagefile where  id = (select id from docimagefile where imagefileid = ?) order by imagefileid desc";
            RecordSet versionrs = new RecordSet();
            String value2 = "";
            versionrs.executeQuery(versionSql,mainimagefile);
            if(versionrs.next()){
                mainimagefile = Util.null2s(versionrs.getString("imagefileid"),mainimagefile);
            }
        }
//        if(!fileids.isEmpty()){ //处理附件，判断所有的附件id是否是最新版本，不是最新版本就更新为最新版本
//            String fileidvalue = "";
//            String versionSql = "select imagefileid from docimagefile where  id = (select id from docimagefile where imagefileid = ?) order by imagefileid desc";
//            RecordSet versionrs = new RecordSet();
//            String value2 = "";
//            for (String fileid : fileids.split(",")){
//                versionrs.executeQuery(versionSql,fileid);
//                if(versionrs.next()){
//                    value2 = Util.null2String(versionrs.getString("imagefileid"));
//                    if(!value2.isEmpty()){
//                        fileidvalue = fileidvalue.isEmpty()?value2:(fileidvalue+","+value2);
//                    }
//                }
//            }
//            if(!fileidvalue.isEmpty()) fileids = fileidvalue;
//        }
        //处理新建或者编辑的时候，未设置主文档，则在此处设置
        if (!fileids.isEmpty() && mainimagefile.isEmpty()) {
            fileids = PatternUtil.trimSplit(fileids, ",");
            boolean mainFileFlag = false;
            for (String fileid : fileids.split(",")) {
                if(ImageConvertUtil.canViewOnlineByFileid(fileid) && !mainFileFlag){
                    mainFileFlag = true;
                    DocDetailUtil.setMainImagefile2imagefile(docid+"",fileid);
                }
            }
        }else{
            //如果有设置主文档，则更新主文档的值
            DocDetailUtil.setMainImagefile2imagefile(docid+"",mainimagefile);
        }
        String oldDoctype = "1";
        if (!isadd) { // 是编辑
            DocDetailLog log = new DocDetailLog();
            log.resetParameter();
            log.setDocId(docid);
            log.setDocSubject(paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
            log.setOperateType("20");
            log.setOperateUserid(user.getUID());
            log.setUsertype(user.getLogintype());
            log.setClientAddress(clientip);
            log.setDocCreater(user.getUID());
            log.setCreatertype(user.getLogintype());
            log.setDocLogInfo();

            rs.executeQuery("select d.docstatus,d.seccategory,d.docEditionId,d.docEdition,d.docdepartmentid,d.ishistory,d.doccreaterid from DocDetail d where d.id=" + docid);
            rs.next();
            //编辑状态保存文档首先判断权限是否满足
            //判读是否具备失效权限
            int docdepartmentid = rs.getInt("docdepartmentid");	//文档所属部门
            int ishistory = rs.getInt("ishistory");	//是否是历史文档
            String docstatus= rs.getString("docstatus");
            oldDoctype = Util.null2s(rs.getString("doctype"),"1");
			String docCreaterId = Util.null2String(rs.getString("doccreaterid"));
            DocViewPermission docViewPermission = new DocViewPermission();
            Map<String,Boolean> rightMap = docViewPermission.getShareLevel(docid, user, false);
            boolean canEdit = rightMap.get(DocViewPermission.EDIT);
			int requestId = Util.getIntValue(paramMap.get("requestid"), 0);
            if(requestId > 0 || canEdit && (Util.getIntValue(docstatus,3) < 3 || docstatus.equals("4") || docstatus.equals("7")) && (ishistory!=1))
                canEdit = true;
            else
                canEdit = false;

			if(!canEdit){
				docViewPermission.hasEditRightFromOtherMould(docid,rightMap,user,paramMap);
				canEdit = rightMap.get(docViewPermission.EDIT);
			}
            if(!canEdit&&!("-1".equals(docstatus)&&(user.getUID()+"").equals(docCreaterId))){
                apidatas.put("status",-1);
                apidatas.put("msg",SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
                return apidatas;
            }


            oldstatus = Util.null2String(rs.getString("docstatus"));
            int secid = rs.getInt("seccategory");
            docEditionId = rs.getInt("docEditionId");
            docEdition = rs.getInt("docEdition");
            oldsecid = secid;
            SecCategoryComInfo scc = new SecCategoryComInfo();
            //如果不是正文执行以下操作
            if (!Util.null2String(paramMap.get("isflowdoc")).equals("1")) {

                if (!saveBJCA && scc.isEditionOpen(secid) && ((Util.getIntValue(oldstatus) > 0 && !"4".equals(oldstatus)) || (docEditionId > 0 && docEdition > 0))) {
                    needAddNewEdition = true;
                    DocIdUpdate docIdUpdate = new DocIdUpdate();
                    docid = docIdUpdate.getDocNewId();
                }
                //复制文档信息生成新的版本
                if (needAddNewEdition) {

                    if (docEditionId <= 0) {//之前一个版本为非版本管理
                        docEditionId = dm.getNextEditionId(rs);
                        docEdition = 1;
                        rs.execute("update DocDetail set docEditionId=" + docEditionId + ",docEdition=" + docEdition + " where id=" + olddocid);
                        rs.execute("update docdetail set docstatus = 7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition <= " + docEdition + " and doceditionid > 0 and doceditionid = " + docEditionId);
                        docEdition = 2;
                    } else if (docEdition <= 0) {//之前一个版本没有设置版本号  如为审批状态
                        DocComInfo dc = new DocComInfo();
                        docEdition = dc.getEdition(docEditionId) + 1;
                        rs.execute("update DocDetail set docEditionId=" + docEditionId + ",docEdition=" + docEdition + " where id=" + olddocid);
                        rs.execute("update docdetail set docstatus = 7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition <= " + docEdition + " and doceditionid > 0 and doceditionid = " + docEditionId);
                    } else if (docEditionId > 0 && docEdition > 0&&scc.needApprove(secid)) {
                        DocComInfo dc = new DocComInfo();
                        docEdition = dc.getEdition(docEditionId) + 1;
                        rs.execute("update docdetail set docstatus = 7,ishistory = 1 where id not in(" + docid+","+olddocid + ") and docedition > 0 and docedition <= " + docEdition + " and doceditionid > 0 and doceditionid = " + docEditionId);
                    }else if (docEditionId > 0 && docEdition > 0&&!scc.needApprove(secid)) {
                        DocComInfo dc = new DocComInfo();
                        docEdition = dc.getEdition(docEditionId) + 1;
                        rs.execute("update docdetail set docstatus = 7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition <= " + docEdition + " and doceditionid > 0 and doceditionid = " + docEditionId);
                    }

                    String delsql = "select imagefileid from DocImageFile where docfiletype <> '1' and docfiletype <> '11' and isextfile='1' and docid=" + olddocid;
                    if (!fileids.isEmpty()) {
                        fileids = PatternUtil.trimSplit(fileids, ",");
                        if (PatternUtil.isAllNumber(fileids)) {
                            delsql += " and not id in(select id from DocImageFile where imagefileid in(" + fileids + "))"; //提交的附件id和已存在，则该附件及其历史版本不处理
                        }
                    }
                    rs.executeQuery(delsql);
                    while (rs.next()) {
                        deleteaccessory += "," + rs.getString("imagefileid");
                    }
                    deleteaccessory = deleteaccessory.contains(",") ? deleteaccessory.substring(1) : deleteaccessory;
                    dm.resetParameter();
                    dm.setId(docid);
                    dm.setSeccategory(secid);
                    dm.setDocEdition(docEdition);
                    dm.setDocEditionId(docEditionId);
                    dm.setDocsubject(paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
                    dm.setUserid(user.getUID());
                    dm.setUsertype(user.getLogintype());
                    dm.setDoccreaterid(user.getUID());
                    dm.setDocCreaterType(user.getLogintype());
                    dm.addNewDocForEdition(olddocid, deleteaccessory);
                } else {
                    //rs.executeQuery("select * from DocImageFile where docfiletype <> '1' and docfiletype <> '11' and isextfile='1' and docid =" + olddocid);
                    String delsql = "select imagefileid from DocImageFile where docfiletype <> '1' and docfiletype <> '11' and isextfile='1' and docid=" + docid;
                    if (!fileids.isEmpty()) {
                        fileids = PatternUtil.trimSplit(fileids, ",");
                        if (PatternUtil.isAllNumber(fileids)) {
                            //delsql += " and imagefileid not in(" + fileids + ")";
                            delsql += " and id not in(select id from DocImageFile where imagefileid in(" + fileids + "))"; //提交的附件id和已存在，则该附件及其历史版本不处理
                        }
                    }

                    rs.executeQuery(delsql);
                    DocAccService accService = new DocAccService();
                    String delids = "";
                    while (rs.next()) {
                        delids += "," + rs.getInt("imagefileid");
                        accService.deleteAcc(rs.getInt("imagefileid"), true);
                    }
                    if (delids.contains(",")) {
                        delids = delids.substring(1);
                        rs.execute("delete from DocImagefile where imagefileid in(" + delids + ")");
                    }
                }
            }
        }
        int accessorycount = 0;
        List<String> isExistedImageFileIds = new ArrayList<String>();
        boolean addExtraHighlight = "true".equals(paramMap.get("addExtraHighlight"));
        DocSensitiveWordsUtil docSensitiveWordsUtil = new DocSensitiveWordsUtil();
        boolean enableOfficeSensitiveWordValidate =docSensitiveWordsUtil.enableOfficeSensitiveWordValidate();
        boolean enableAutoRemoveHighlightAfterValidateSensitiveWord = docSensitiveWordsUtil.enableAutoRemoveHighlightAfterValidateSensitiveWord();
        if(!enableOfficeSensitiveWordValidate) {
            enableAutoRemoveHighlightAfterValidateSensitiveWord = false;
        }
        if (isadd) {
            if (isOffice) {
                docid = Util.getIntValue(paramMap.get("off_docid"));
                if(docid <= 0){	//新版永中新建office文档（只生成附件）
                	int off_imagefileid = Util.getIntValue(paramMap.get("off_imagefileid"));
                	DocIdUpdate docIdUpdate = new DocIdUpdate();
                    docid = docIdUpdate.getDocNewId();
                    String off_name = paramMap.get("off_name");
                    if(off_imagefileid <=0 && off_name != null && !off_name.isEmpty()){
                    	off_imagefileid = getFileIdByName(off_name,paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
                    }
					if(addExtraHighlight && enableAutoRemoveHighlightAfterValidateSensitiveWord && off_imagefileid > 0) {
                        WordProcessUtil wordProcessUtil = new WordProcessUtil();
                        wordProcessUtil.removeHighlightInPlace(off_imagefileid);
                    }
                    if (off_imagefileid > 0) {
                        DocAccService accService = new DocAccService();
                        accService.buildRelForDoc(off_imagefileid, docid, false);
                    }
                }
            } else {
                DocIdUpdate docIdUpdate = new DocIdUpdate();
                docid = docIdUpdate.getDocNewId();
            }
        } else {
            //isextfile = '1' and docid = " + this.id + " and docfiletype <> '1'   and docfiletype <> '11'
            rs.execute("select * from DocImageFile where docfiletype <> '1' and docfiletype <> '11' and isextfile='1' and docid =" + olddocid);
            while (rs.next()) {
                isExistedImageFileIds.add(Util.null2String(rs.getString("imagefileid")));
            }
            //accessorycount = isExistedImageFileIds.size();

            if (isOffice && !saveBJCA) {
                int off_imagefileid = Util.getIntValue(paramMap.get("off_imagefileid"));
                String off_name = paramMap.get("off_name");
                if(off_imagefileid <=0 && off_name != null && !off_name.isEmpty()){
                	off_imagefileid = getFileIdByName(off_name,paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
                }
                if(addExtraHighlight && enableAutoRemoveHighlightAfterValidateSensitiveWord && off_imagefileid > 0) {
                    WordProcessUtil wordProcessUtil = new WordProcessUtil();
                    wordProcessUtil.removeHighlightInPlace(off_imagefileid);
                }
                if (off_imagefileid > 0) {
                    RecordSet recordSet = new RecordSet();
                    recordSet.executeQuery("select imagefileid from docimagefile where docid =  "+docid+" and (isextfile <> '1' or isextfile is null) and docfiletype <> '1' and docfiletype<>'11' order by id desc");
                    int docimagefileid = 0;
                    if(recordSet.next()){
                        docimagefileid = Util.getIntValue(recordSet.getString("imagefileid"));
                    }
                    DocAccService accService = new DocAccService();
                    accService.buildRelForDoc(off_imagefileid, docid, false,docimagefileid);

                    String date = TimeUtil.getCurrentDateString();
                    String time = TimeUtil.getOnlyCurrentTimeString() ;
                    String versionDetail = user.getUsername()+date+" "+time+""+ SystemEnv.getHtmlLabelName(10004433,weaver.general.ThreadVarLanguage.getLang())+"";
                    rs.executeUpdate("update docimagefile set versiondetail = ? where docid = ? and imagefileid = ?",versionDetail,docid,off_imagefileid);
                }
                /*if (!needAddNewEdition && !Util.null2String(paramMap.get("isflowdoc")).equals("1")) {  //office文档，不开启版本，删除旧文件,，并且不是公文流程
                	rs.execute("select imagefileid from DocImageFile where docid =" + olddocid + " and (isextfile <> '1' or isextfile is null)");
                    while (rs.next()) {
                        int imagefileid = rs.getInt("imagefileid");
                        if(off_imagefileid > 0 && off_imagefileid == imagefileid)
                        	continue;
                        rs.execute("delete from DocImageFile where docid =" + olddocid + " and imagefileid=" + imagefileid);
                        DocAccService dac = new DocAccService();
                        dac.deleteAcc(imagefileid, true);
                    }
                }*/
            }
        }


        /** * 处理附件 start ** */
        if (!fileids.isEmpty()) {
            fileids = PatternUtil.trimSplit(fileids, ",");
            String delids = "";
            DocAccService accService = new DocAccService();
            for (String fileid : fileids.split(",")) {
                if (Util.getIntValue(fileid, -1) <= 0)
                    continue;
                if (isExistedImageFileIds.indexOf(Util.null2String(fileid)) > -1) {
                    accessorycount++;
                    continue;
                }
                delids += "," + fileid;
                int flag = accService.buildRelForDoc(Util.getIntValue(fileid, 0), docid);
                if (flag == 1) {
                    accessorycount++;
                    DocImageManager dim = new DocImageManager();
                    dim.resetParameter();
                    dim.setDocid(docid);
                    dim.setImagefileid(fileid);
                    dim.setOperateuserid(user.getUID());
                    dim.setVersionDetail(SystemEnv.getHtmlLabelName(128095,user.getLanguage()));
                    dim.updateDocImageOperaInfo();
                }
            }

            // 删除临时表数据
            if (!delids.isEmpty()) {
                delids = delids.substring(1);
                String delidsStr = delids.contains(",") ? (" in (" + delids + ")") : (" = " + delids);
                rs.execute("delete from imagefiletemp where imagefileid " + delidsStr);
            }
        }
        /** * 处理附件 end ** */


        // 用户基本属性
        int doccreaterid = user.getUID();
        String docCreaterType = user.getLogintype();

        String docMain = "";
        String dummyIds = "";

        Map<String, String> columnMap = new HashMap<String, String>();

        for (DocParamItem docParam : DocParamItem.values()) {
            String column = docParam.getName();
            if (paramMap.get(column) == null||StringUtils.isBlank(Util.null2String(paramMap.get(column))))
                continue;
            String value = paramMap.get(column);

            // 特殊处理属性
            if (docParam == DocParamItem.DOC_MAIN) { // 摘要
                docMain = value;
                continue;
            } else if (docParam == DocParamItem.TREE_DOC_FIELD_ID) { // 虚拟目录
                dummyIds = value;
                continue;
            } else if (docParam == DocParamItem.MAIN_DOC) {//主文档
                if (Util.getIntValue(value, 0) == -1) {
                    value = docid + "";
                } else {
                    value = Util.getIntValue(value, -1) + "";
                }
            }else if(docParam == DocParamItem.SECRET_LEVEL) {
                if(Util.null2s(value,"").isEmpty()){
                    value = DocManager.DEFAILT_SECRET_LEVEL+"";
                }
            }else if (docParam == DocParamItem.DOC_MOULD && isadd) { //模板(新建选择的编辑模板)
                continue;
            }

            value = Util.toHtml2(value);
            if (docParam.getDbType() == DbType.INT) {
                value = Util.getIntValue(value, 0) + "";
            } else {
                if(DocParamItem.DOC_SUBJECT == docParam) {
                    value = interceptString4Word(value, docParam.getLength());
                } else {
                    value = interceptString(value, docParam.getLength());
                }
                value = value.replace("'", "''");
                value = "'" + value + "'";
            }
            columnMap.put(docParam.getName().toUpperCase(), value);
        }

        String docStatus = "";

        String operate = paramMap.get("operate");
        int secid = Util.getIntValue(paramMap.get(DocParamItem.SEC_CATEGORY.getName()));

        secid = secid <= 0 ? oldsecid : secid;

        if ("draft".equals(operate)) {
            docStatus = "-1";
        } else if (Util.null2String(paramMap.get("isflowdoc")).equals("1")) { //来源公文流程
            int requestid = Util.getIntValue(paramMap.get("requestid"),-1);
            if(requestid>0){
                docStatus = getDocStatusByRequestid(requestid);
            }else{
                docStatus = "9";
            }
        } else {
            SecCategoryComInfo scc = new SecCategoryComInfo();
            boolean blnOsp = false;
            if (isadd || Util.getIntValue(oldstatus) <= 0) {  //是新建或者草稿
                blnOsp = scc.isSetShare(secid);  //是否弹出默认提示
            }

            if (scc.needApprove(secid)) {//如果需要审批
                docStatus = blnOsp ? "-3" : "3";
            } else if (scc.needPubOperation(secid)) {//如果需要发布
                docStatus = blnOsp ? "-6" : "6";
            } else {//生效/正常
                docStatus = blnOsp ? "-1" : "1";

                if (scc.isEditionOpen(secid)) {//如果版本管理开启
                    if (isadd) {
                        docEditionId = dm.getNextEditionId(rs);
                        docEdition = 1;
                    }
                    columnMap.put("DOCEDITIONID", docEditionId + "");
                    columnMap.put("DOCEDITION", docEdition + "");
                }
            }

            apidatas.put("blnOsp", blnOsp ? "1" : "0");
        }

        if ("1".equals(docStatus)) {
            //生效人、类型、日期、时间
            columnMap.put("DOCVALIDUSERID", user.getUID() + "");
            columnMap.put("DOCVALIDUSERTYPE", user.getType() + "");
            columnMap.put("DOCVALIDDATE", "'" + TimeUtil.getCurrentDateString() + "'");
            columnMap.put("DOCVALIDTIME", "'" + TimeUtil.getOnlyCurrentTimeString() + "'");
        }

        //文档状态   //只有在正文新建的时候，置为流程草稿状态
        if (docStatus.equals("9")){
            if(Util.null2String(paramMap.get("isflowdoc")).equals("1")&&isadd)
                columnMap.put("DOCSTATUS", docStatus);
        }else{
            columnMap.put("DOCSTATUS", docStatus);
        }
        //文档类型
        if (isOffice) {
            if(saveBJCA){
                columnMap.put("DOCTYPE", "12");
            }else{
                columnMap.put("DOCTYPE", "2");
            }
            String strSql = "select imagefilename from docimagefile where (isextfile <> '1' or isextfile is null) and docid=" + docid;
            rs.execute(strSql);
            if (rs.next()) {
                String imageFileName = Util.null2String(rs.getString(1));
                int tempPos = imageFileName.lastIndexOf(".");
                if (tempPos != -1) {
                    columnMap.put("DOCEXTENDNAME", "'" + imageFileName.substring(tempPos + 1) + "'");
                }
            }
        } else {
            columnMap.put("DOCTYPE", oldDoctype);
            if (!isadd) { // 是编辑文件类型不做修改
                String strSql = "select imagefilename from docimagefile where (isextfile <> '1' or isextfile is null) and docid=" + docid;
                rs.execute(strSql);
                if (rs.next()) {
                    String imageFileName = Util.null2String(rs.getString(1));
                    if(imageFileName.toLowerCase().endsWith("pdf")) {
                        columnMap.put("DOCTYPE", "12");
                        int tempPos = imageFileName.lastIndexOf(".");
                        if (tempPos != -1) {
                            columnMap.put("DOCEXTENDNAME", "'" + imageFileName.substring(tempPos + 1) + "'");
                        }
                    }
                }
            } else {
                columnMap.put("DOCEXTENDNAME", "'html'");
            }
        }

        if(isadd || docStatus.equals("9") || Util.getIntValue(docStatus)<=0 ){
            String secretLevel = Util.null2String(paramMap.get("secretLevel"));
            if(!"".equals(secretLevel)){
                columnMap.put("SECRETLEVEL", secretLevel);
            }
        }
        secretValidity = Util.null2String(paramMap.get("secretValidity"));
        if(!secretValidity.isEmpty()){
            columnMap.put("SECRETVALIDITY", "'"+secretValidity+"'");
        }
        //附件数
        columnMap.put("ACCESSORYCOUNT", accessorycount + "");
        //所有者类型
        columnMap.put("OWNERTYPE", "1");
        if(isadd){
            //文档编号
            DocCoder dc = new DocCoder();
            String docCode = dc.getDocCoder("" + secid);
            columnMap.put("DOCCODE", "'" + docCode + "'");
        }

        /**目录相关 start */

        //是否可打印
        int readOpterCanPrint = 0;
        //是否可订阅
        int orderable = 0;

        rs.execute("select readoptercanprint,orderable from DocSecCategory where id=" + secid);
        if (rs.next()) {
            readOpterCanPrint = Util.getIntValue(rs.getString("readOpterCanPrint"), 0);
            orderable = Util.getIntValue(rs.getString("orderable"), 0);
        }
        if(columnMap.get("READOPTERCANPRINT") == null){
        	columnMap.put("READOPTERCANPRINT", readOpterCanPrint + "");
        }
        columnMap.put("ORDERABLE", orderable + "");


        /**目录相关 end */


        //所属部门
        int docdepartmentid = 0;
        if (Util.getIntValue(paramMap.get(DocParamItem.OWNER_ID.getName())) > 0) {
            try {
                ResourceComInfo hrc = new ResourceComInfo();
                docdepartmentid = Util.getIntValue(hrc.getDepartmentID(paramMap.get(DocParamItem.OWNER_ID.getName())));
                columnMap.put("DOCDEPARTMENTID", docdepartmentid + "");
            } catch (Exception e) {
            }
        }else if(isadd){  //后台目录属性没有开启时
        	 columnMap.put("OWNERID", user.getUID() + "");
             columnMap.put("DOCDEPARTMENTID", user.getUserDepartment() + "");
        }
        columnMap.put("THEMESHOWPIC", "\'" + Util.null2String(paramMap.get("themeshowpic")) + "\'");
        /** * 摘要start ** */
        String doccontent = paramMap.get(DOC_CONTENT);
        doccontent = replaceContent(docid, doccontent, needAddNewEdition);
        if ("2".equals(paramMap.get(DocParamItem.DOC_PUBLISH_TYPE.getName()))) {
            doccontent = Util.toHtml2(Util.encodeAnd(docMain)) + "!@#$%^&*"
                    + doccontent;
        }
        if (!"".equals(doccontent)) {
            String reg = "fileid=([0-9a-zA-Z]+)";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(doccontent);
            if (matcher.find()) {
                String docfileid = matcher.group();
                docfileid = "'"+docfileid.replace("fileid=","")+"'";
                columnMap.put("DOCFIRSTPICID",docfileid);
                columnMap.put("HASFIRSTPIC","1");
            }
        }
        /** * 摘要end ** */

        /** 基本信息start * */
        addBaseInfos(columnMap, user, docid + "", isadd);
        if(Util.null2String(paramMap.get("isflowdoc")).equals("1")){
            columnMap.remove("CHECKOUTSTATUS");
            columnMap.remove("CHECKOUTUSERID");
            columnMap.remove("CHECKOUTUSERTYPE");
            columnMap.remove("CHECKOUTDATE");
            columnMap.remove("CHECKOUTTIME");

        }
        /** 基本信息end * */

        String dbType = rs.getDBType();
        String sqlKey = "";
        String sqlValue = "";

        /** docdetail sql组装 start * */
//        String regexp = "\'";
        for (String key : columnMap.keySet()) {
            if (isadd) {
                if (!Util.null2String(columnMap.get(key)).equals("") && !key.equals("UPLOADSIZE")&& !key.equals("UPLOADTYPE")){//前端传入的多余的参数
                    sqlKey += "," + key;
                    sqlValue += "," + columnMap.get(key);
                }

            } else {
                sqlKey += "," + key + "=" + columnMap.get(key);
            }
        }

        sqlKey = sqlKey.substring(1);
        sqlValue = sqlValue.length() > 0 ? sqlValue.substring(1) : sqlValue;

        /** docdetail sql组装 end * */

        /** 文档正文 start * */
        if ("oracle".equals(dbType) || "mysql".equals(dbType)) {
        	
        	RecordSet rsContent = new RecordSet();
        	
        	//rsContent.writeLog("^^^^^^^("+doccontent.length()+")^^^^^^^docContent=" + doccontent);
        	
        	if (isadd) {
        		rsContent.executeUpdate("insert into DocDetailContent (doccontent,docid) values(?,?)", doccontent,docid);
        	}else{
        		rsContent.executeUpdate("update DocDetailContent set doccontent=? where docid=?", doccontent,docid);
        	}
        	
        } else {
            doccontent = doccontent.replace("'", "''");
            if (isadd) {
                sqlKey += ",doccontent";
                sqlValue += ",'" + doccontent + "'";
            } else {
                sqlKey += ",doccontent='" + doccontent + "'";
            }
        }
        /** 文档正文 end * */

        String sql = "";
        if (isadd) {
            sqlKey = "id," + sqlKey;
            sqlValue = docid + "," + sqlValue;
            sql = "insert into DocDetail(" + sqlKey + ") values(" + sqlValue
                    + ")";
        } else {
            sql = "update DocDetail set " + sqlKey + " where id=" + docid;
            new BaseBean().writeLog("docsaveservice---updSql = "+sql);
        }

        rs.execute(sql);

        // 1621135wgs 更新 接收正文转存pdf字段 文档的修改时间
        int requestid = Util.getIntValue(paramMap.get("requestid"), 0);
        new MutiFileChangeModDateTime().updateRelationDoc(requestid, null, "save");
        // 1621135wgs

        // 执行触发器docdetail_getpinyin
        DocTriggerUtils docTriggerUtils = new DocTriggerUtils();
        if(isadd) {
            docTriggerUtils.docdetail_getpinyin(rs);
        } else {
            docTriggerUtils.docdetail_getpinyin(docid,rs);
        }
        /**
         * 自定义字段
         * */
        
        boolean costomAdd = isadd;
        if (!costomAdd) {
            RecordSet customRs = new RecordSet();
            customRs.executeQuery("select 1 from cus_fielddata where id=" + docid + " and scope='DocCustomFieldBySecCategory' and scopeid='" + secid + "'");
            if (!customRs.next()) {
                costomAdd = true;
            }
        }
        
        CustomFieldManager cfm = new CustomFieldManager("DocCustomFieldBySecCategory", secid);
        cfm.getCustomFields();

        String customColumns = "";
        String customValues = "";
        EncryptUtil encryptUtil = new EncryptUtil();
        while (cfm.next()) {
            String fieldId = cfm.getId() + "";
            for (String key : paramMap.keySet()) {
                if (key.startsWith(CUSTOM_FIELD)) {
                    String fileid = key.replace(CUSTOM_FIELD, "");
                    if (Util.getIntValue(fileid, -1) == -1)
                        continue;
                }
            }
            String value = paramMap.get(CUSTOM_FIELD + fieldId);
            if (value != null) {

                boolean isString = false;
                if (cfm.getFieldDbType().startsWith("text") || cfm.getFieldDbType().startsWith("char")
                        || cfm.getFieldDbType().startsWith("varchar") || cfm.getFieldDbType().startsWith("browser")) {
                    isString = true;
                }

                String column = cfm.getFieldName(fieldId);
                rs.writeLog(DocSaveService.class.getName(),"  colum="+column+" secid="+secid+" value="+value);
                EncryptFieldEntity encryptFieldEntity  = new EncryptFieldConfigComInfo().getFieldEncryptConfig("cus_fielddata", column,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM,secid+"");
                if(encryptFieldEntity!=null && encryptFieldEntity.getIsEncrypt().equals("1")){
                    rs.writeLog(DocSaveService.class.getName(),"  colum="+column+" secid="+secid+" value="+value);
                    //是否需要加密
                    value = Util.null2String(encryptUtil.encryt("cus_fielddata", column, "HrmCustomFieldByInfoType",secid+"",value, value));
                }
                if (costomAdd) {
                    customColumns += "," + column;
                    if (value.isEmpty()) {
                        customValues += "," + (isString ? "''" : "null");
                    } else {
                        customValues += ",'" + value.replace("'", "''") + "'";
                    }

                } else {
                    if (value.isEmpty()) {
                        customColumns += "," + column + "=" + (isString ? "''" : "null");
                    } else {
                        customColumns += "," + column + "='" + value.replace("'", "''") + "'";
                    }

                }
            }
        }

        
        String customSql = "";
        if (costomAdd) {
            customColumns = "scope,scopeid,id" + customColumns;
            customValues = "'DocCustomFieldBySecCategory'," + secid + "," + docid + customValues;
            customSql = "insert into cus_fielddata(" + customColumns + ") values(" + customValues + ")";
        } else {
            if (!customColumns.isEmpty()) {
                customColumns = customColumns.substring(1);
                customSql = "update cus_fielddata set " + customColumns +
                        " where id=" + docid + " and scope='DocCustomFieldBySecCategory' and scopeid='" + secid + "'";
            }
        }
        if (!customSql.isEmpty()) {
            RecordSet customRs = new RecordSet();
            customRs.setNoAutoEncrypt(true);
            customRs.execute(customSql);
        }


        /** * 虚拟目录start ** */
        dummyIds = Util.null2String(dummyIds);
        dummyIds = dummyIds.startsWith(",") ? dummyIds.substring(1) : dummyIds;
        dummyIds = dummyIds.endsWith(",") ? dummyIds.substring(0, dummyIds
                .length() - 1) : dummyIds;
        if (PatternUtil.isAllNumber(dummyIds) || "".equals(dummyIds)) {
            String importdate = TimeUtil.getCurrentDateString();
            String importtime = TimeUtil.getOnlyCurrentTimeString();
            //DocTreeDocFieldComInfo ddfc = new DocTreeDocFieldComInfo();
            if (!isadd) {
                rs.execute("delete from DocDummyDetail where docid=" + docid);
            }
            
            if(!dummyIds.isEmpty()){
	            for (String dummyId : dummyIds.split(",")) {
	                //if (!ddfc.isHaveSameOne(dummyId, docid)) {
	                String strSql = "insert into DocDummyDetail(catelogid,docid,importdate,importtime) values ("
	                        + dummyId
	                        + ","
	                        + docid
	                        + ",'"
	                        + importdate
	                        + "','"
	                        + importtime + "')";
	                rs.execute(strSql);
	                //}
	            }
            }

        }
        /** * 虚拟目录end ** */

        /** * 阅读日志start ** */
        if (isadd) {
            char flag = Util.getSeparator();
            rs.executeProc("docReadTag_AddByUser", "" + docid + flag
                    + doccreaterid + flag + docCreaterType);
            if (docCreaterType == "" || docCreaterType == null) {
                rs.execute("update docreadtag set readcount=0 where docid="
                        + docid + " and userid=" + doccreaterid
                        + " and usertype= 1 ");
            } else {
                rs.execute("update docreadtag set readcount=0 where docid="
                        + docid + " and userid=" + doccreaterid
                        + " and usertype=" + docCreaterType);
            }
            rs.execute("update docdetail set sumReadCount=0 where id=" + docid);

            DocDetailLog log = new DocDetailLog();
            log.resetParameter();
            log.setDocId(docid);
            log.setDocSubject(paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
            log.setOperateType("1");
            log.setOperateUserid(user.getUID());
            log.setUsertype(user.getLogintype());
            log.setClientAddress(clientip);
            log.setDocCreater(user.getUID());
            log.setCreatertype(user.getLogintype());
            log.setDocLogInfo();
        } else {
            DocDetailLog log = new DocDetailLog();
            log.resetParameter();
            log.setDocId(docid);
            log.setDocSubject(paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
            log.setOperateType("2");
            log.setOperateUserid(user.getUID());
            log.setUsertype(user.getLogintype());
            log.setClientAddress(clientip);
            log.setDocCreater(user.getUID());
            log.setCreatertype(user.getLogintype());
            log.setDocLogInfo();

            DocComInfo dci = new DocComInfo();
            dci.updateDocInfoCache("" + docid);

        }
        /** * 阅读日志end ** */

        /** * 回复提醒  主附件 start ** */
        String replysql = "update docdetail set replyremind = ? where id = ?";
        String replyremind = Util.null2s(paramMap.get("replyremind"),"0");
        RecordSet replyrs = new RecordSet();
        replyrs.executeUpdate(replysql,replyremind,docid);
        replyrs.next();
        /** * 回复提醒 主附件end ** */

        if (isadd) {
            dm.resetParameter();
            dm.setId(docid);
            dm.setSeccategory(secid);
            dm.setUserid(user.getUID());
            dm.setDocCreaterType(user.getLogintype());
            dm.setDocdepartmentid(docdepartmentid);
            dm.AddShareInfo();

            DocViewer DocViewer = new DocViewer();
            DocViewer.setDocShareByDoc(docid + "");
        }


        copyDocFile(secid,docid);
        apidatas.put("status", 1);
        apidatas.put("api_status", true);
        apidatas.put("docid", docid);
		String currentblnOsp=Util.null2String(apidatas.get("blnOsp"));
		if(false && !"1".equals(currentblnOsp)){
			rs.executeQuery("select d.docstatus,d.scheduledreleasedate,d.docEditionId,d.docEdition from DocDetail d where  d.id=" + docid);
			String currentDocStatus="";
			String scheduledreleasedate="";
			if (rs.next()) {
				currentDocStatus=Util.null2String(rs.getString("docstatus"));
				scheduledreleasedate=Util.null2String(rs.getString("scheduledreleasedate"));
				int currentDocdocEditionId=Util.getIntValue(rs.getString("docEditionId"),-1);
				int currentDocdocEdition=Util.getIntValue(rs.getString("docEdition"),-1);
				if ("6".equals(currentDocStatus)&&!"".equals(scheduledreleasedate)) {
					String currentDate=TimeUtil.getCurrentDateString();
					String currentTime=TimeUtil.getOnlyCurrentTimeString();
					if(scheduledreleasedate.compareTo(currentDate)<=0){
						SecCategoryComInfo scc2 = new SecCategoryComInfo();
						if (scc2.isEditionOpen(secid)) {//如果版本管理开启
							if (isadd) {
								docEditionId = dm.getNextEditionId(rs);
								docEdition = 1;
								rs.executeSql("UPDATE DocDetail SET doceditionid=" +docEditionId+",docEdition="+docEdition+" where id="+docid);
							}else{
								if(currentDocdocEditionId>0 && currentDocdocEdition<=0){
									DocComInfo dc = new DocComInfo();
									currentDocdocEdition = dc.getEdition(currentDocdocEditionId) + 1;
									rs.executeSql("UPDATE DocDetail SET docEdition="+currentDocdocEdition+" where id="+docid);
								}								
							}							
						}
						rs.executeSql(
								" UPDATE DocDetail SET " +
								" docstatus = 2" +						
								",docpubuserid = " + user.getUID() +
								",docPubUserType = '" + user.getLogintype() + "'" +                     
								",docpubdate = '" + currentDate + "'" +
								",docpubtime = '" + currentTime + "'" +
								",docvaliduserid = " + user.getUID() +
								",docValidUserType = '" + user.getLogintype() + "'" +                     
								",docvaliddate = '" + currentDate + "'" +
								",docvalidtime = '" + currentTime + "'" +
								" WHERE ID = " + docid
						);
					}
				}
			}
		}
		
        //时间戳,保证页面刷新
        Calendar c = Calendar.getInstance();

        String params = "";
        if (apidatas.get("blnOsp") != null) {
            params += "&blnOsp=" + apidatas.get("blnOsp");
        }

        params += "&__random=" + c.getTimeInMillis();

        params += "&" + DocSptm.DOC_ROOT_FLAG + "=1";

        DocViewPermission dvp = new DocViewPermission();
        params += dvp.getMoudleParams(paramMap);

        apidatas.put("link", DocSptm.DOC_DETAIL_LINK + "?id=" + docid + params + DocSptm.DOC_DETAIL_ROUT);
        BaseBean bb = new BaseBean();
        bb.writeLog("docid---->"+docid+"docsaveService------->link",DocSptm.DOC_DETAIL_LINK + "?id=" + docid + params + DocSptm.DOC_DETAIL_ROUT);
		apidatas.put("docid",docid+"");
        apidatas.put("docsubject",paramMap.get(DocParamItem.DOC_SUBJECT.getName()));
        boolean success = true;
        if ("3".equals(docStatus)) {
            success = approveWorkflow(docid, oldstatus, "1", user);
        }

        if (success) {
            changeSend(docid);
            senMessage(docid, docStatus, user,request);
        }

        if (Util.null2String(paramMap.get("isflowdoc")).equals("1")) {
            OdocRequestdocUtil oru = new OdocRequestdocUtil();
            oru.changeStatusByWorkflowId(user, paramMap);
        }
        
        SearchUpdateUtil.updateIndexLog(SearchUpdateType.DOC,docid);

        return apidatas;
    }

    private void changeSend(int docid){
        int updatedocsend = 0;
        int editionIsOpen = 0;
        RecordSet findRs = new RecordSet();
        findRs.executeSql("select updatedocsend,editionIsOpen from docseccategory where id in (select seccategory from docdetail where id = "+docid+")");
        if(findRs.next()){
            updatedocsend =findRs.getInt(1);
            editionIsOpen  =Util.getIntValue(findRs.getString("editionIsOpen"),0);
        }
        if(updatedocsend>0 && editionIsOpen!=1) {
            RecordSet sendRs = new RecordSet();
            sendRs.executeUpdate("update sendtoalltemp set status = 0 where docid = ?", docid);
            sendRs.next();
        }
    }
    /**
	 * 根据附件名称获取附件id（用于iwebchina上传获取id）
	 * */
	public int getFileIdByName(String filename,String imagefilename){
		RecordSet rs = new RecordSet();
		rs.executeQuery("select imagefileid from ImageFile where imagefilename=?",filename);
		if(rs.next()){
			int imagefileid = rs.getInt("imagefileid");

			String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";

			rs.executeUpdate("update ImageFile set imagefilename=? where imagefileid=?",imagefilename + extname,imagefileid);
			return imagefileid;
		}
		return 0;
	}

    /**
     * 草稿提交
     */
    public Map<String, Object> doSubmit(int docid, User user, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();

        if (docid <= 0) {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(19711, user.getLanguage()));
            return apidatas;
        }

		/*String f_weaver_belongto_userid = request.getParameter("f_weaver_belongto_userid");
		//String f_weaver_belongto_usertype = paramMap.get("f_weaver_belongto_usertype");
		if(f_weaver_belongto_userid != null && Util.getIntValue(f_weaver_belongto_userid) > 0){
			HrmUserSettingComInfo userSetting=new HrmUserSettingComInfo();
			String belongtoshow = userSetting.getBelongtoshowByUserId(user.getUID()+"");
			String belongtoids = "," + user.getBelongtoids() + ",";
			String account_type = user.getAccount_type();
			if(belongtoshow.equals("1") && account_type.equals("0") &&
					belongtoids.contains("," + f_weaver_belongto_userid + ",")){
				user = new User(Util.getIntValue(f_weaver_belongto_userid));
			}
		}*/

        DocViewPermission dvp = new DocViewPermission();

        Map<String, String> params = new HashMap<String, String>();
        params.get("useNew");

        RecordSet rs = new RecordSet();
        boolean hasRight = false;
        try {
            hasRight = dvp.getDocEdit(user, docid, params, request, response);
        } catch (Exception e) {
            rs.writeLog(e);
        }
		
		String dstatus = "";
		String docCreaterId = "";
		rs.executeQuery("select d.docstatus,d.doccreaterid from DocDetail d where d.id=?", docid);
		if(rs.next()){
			dstatus = Util.null2String(rs.getString("docstatus"));
			docCreaterId = Util.null2String(rs.getString("doccreaterid"));
		}

        if (!hasRight&&!(("-1".equals(dstatus) || "-6".equals(dstatus))&&(user.getUID()+"").equals(docCreaterId))) {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
            return apidatas;
        }

        rs.executeQuery("select d.seccategory,d.docstatus,d.doceditionid,d.docEdition,d.scheduledreleasedate from DocDetail d,DocSecCategory c where d.seccategory=c.id and d.id=" + docid);

        int secid = 0;
        int oldstatus = 0;
        int docEditionId = -1;
        int docEdition = -1;
		String scheduledreleasedate="";
        if (rs.next()) {
            secid = rs.getInt("seccategory");
            oldstatus = rs.getInt("docstatus");
            docEditionId = rs.getInt("doceditionid");
            docEdition = rs.getInt("docEdition");
			scheduledreleasedate=Util.null2String(rs.getString("scheduledreleasedate"));
        } else {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(19711, user.getLanguage()));
            return apidatas;
        }


        SecCategoryComInfo scc = new SecCategoryComInfo();
        boolean blnOsp = false;
        if (oldstatus <= 0) {  //是新建或者草稿
            blnOsp = scc.isSetShare(secid);  //是否弹出默认提示
        }

        String docStatus = "";
        if (scc.needApprove(secid)) {//如果需要审批
            docStatus = blnOsp ? "-3" : "3";
        } else if (scc.needPubOperation(secid)) {//如果需要发布
            docStatus = blnOsp ? "-6" : "6";
        } else {//生效/正常
            docStatus = blnOsp ? "-1" : "1";
        }
        
        String sql = "update DocDetail set docstatus=" + docStatus;

        if ("1".equals(docStatus)) {
            //生效人、类型、日期、时间
            sql += ",DOCVALIDUSERID=" + user.getUID();
            sql += ",DOCVALIDUSERTYPE=" + user.getType();
            sql += ",DOCVALIDDATE='" + TimeUtil.getCurrentDateString() + "'";
            sql += ",DOCVALIDTIME='" + TimeUtil.getOnlyCurrentTimeString() + "'";
        }
        
        if (scc.isEditionOpen(secid)) {//如果版本管理开启
            if (docEditionId <= 0) {
            	DocManager dm = new DocManager();
            	try{
            		docEditionId = dm.getNextEditionId(rs);
            	}catch(Exception e){
            	}
                docEdition = 1;
                sql += ",DOCEDITIONID=" + docEditionId;
                sql += ",DOCEDITION=" + docEdition;
            }else if(docEdition <= 0){
            	DocComInfo dc = new DocComInfo();
            	docEdition = dc.getEdition(docEditionId) + 1;
            	sql += ",DOCEDITION=" + docEdition;
            }
        }
        
        sql += " where id=" + docid;
        rs.executeUpdate(sql);
		if (false && "6".equals(docStatus)&&!"".equals(scheduledreleasedate)) {
			String currentDate=TimeUtil.getCurrentDateString();
			String currentTime=TimeUtil.getOnlyCurrentTimeString();
			if(scheduledreleasedate.compareTo(currentDate)<=0){
				rs.executeSql(
						" UPDATE DocDetail SET " +
						" docstatus = 2" +						
						",docpubuserid = " + user.getUID() +
						",docPubUserType = '" + user.getLogintype() + "'" +                     
						",docpubdate = '" + currentDate + "'" +
						",docpubtime = '" + currentTime + "'" +
						",docvaliduserid = " + user.getUID() +
						",docValidUserType = '" + user.getLogintype() + "'" +                     
						",docvaliddate = '" + currentDate + "'" +
						",docvalidtime = '" + currentTime + "'" +
						" WHERE ID = " + docid
				);
			}
		}

        //时间戳,保证页面刷新
        Calendar c = Calendar.getInstance();

        apidatas.put("status", 1);
        apidatas.put("api_status", true);

        apidatas.put("docid", docid);

        String param = "";
        if (blnOsp) {
            apidatas.put("blnOsp", "1");
            param += "&blnOsp=" + apidatas.get("blnOsp");
        }

        param += "&__random=" + c.getTimeInMillis();

        param += "&" + DocSptm.DOC_ROOT_FLAG + "=1";

        param += dvp.getMoudleParams(request);

        apidatas.put("link", DocSptm.DOC_DETAIL_LINK + "?id=" + docid + param + DocSptm.DOC_DETAIL_ROUT);

        boolean success = true;
        if ("3".equals(docStatus)) {
            success = approveWorkflow(docid, oldstatus + "", "1", user);
        }

        if (success) {
            changeSend(docid);
            senMessage(docid, docStatus, user,request);
        }


        return apidatas;
    }

    /**
     * 触发流程
     *
     * @author wangqs
     * @params docid
     * @params oldstatus
     * @params approveType   1-生效，2-失效
     * @params user
     */
    public boolean approveWorkflow(int docid, String oldstatus, String approveType, User user) {
        RecordSet rs = new RecordSet();
        RecordSet rs2 = new RecordSet();
        rs.execute("select c.approveWorkflowId,c.isOpenApproveWf,d.docsubject,d.docstatus,c.id secid from DocSecCategory c,DocDetail d where d.id=" + docid + " and d.seccategory=c.id");


        int workflowId = 0;
        int secid = 0;
        String isOpenApproveWf = "";
        String currentStatus = "";
        if (rs.next()) {
            workflowId = rs.getInt("approveWorkflowId");
            isOpenApproveWf = Util.null2String(rs.getString("isOpenApproveWf"));

            currentStatus = rs.getString("docstatus");
            secid = rs.getInt("secid");

        }
        if (oldstatus == null || oldstatus.isEmpty()) {
            oldstatus = "-3";
        }
        SecCategoryComInfo scci = new SecCategoryComInfo();
        if (!scci.needApprove(secid, Util.getIntValue(approveType, 1)) && "3".equals(currentStatus)) {
            rs2.execute("update DocDetail set docStatus=-3 where id=" + docid);
            return false;
        }

        if ("1".equals(isOpenApproveWf)) {  //审批工作流
            DocApproveWfManager docApproveWfManager = new DocApproveWfManager();
            boolean secretFlag = CheckPermission.isOpenSecret();
        	if(secretFlag){
        		DocManager dm = new DocManager();
        		try{
        			dm.resetParameter();
        			dm.setId(docid);
        			dm.getDocInfoById();
        			docApproveWfManager.setSecLevel(dm.getDocSecretLevel() + "");
        		}catch(Exception e){
        			
        		}
        	}

            String flag = "";
            if ("3".equals(oldstatus)) {  //已在审批状态
                flag = docApproveWfManager.updateApprovedWf(docid, user);
            } else {
                flag = docApproveWfManager.approveWf(docid, approveType, user);
            }
            if ("false".equals(flag)) {  //失败
                if ("1".equals(approveType)) {
                    rs2.execute("update DocDetail set docStatus=0 where id=" + docid);
                    rs2.execute("update docdetail set docstatus=7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition < (select docedition from DocDetail where id=" + docid + ") and doceditionid > 0 and doceditionid = (select docEditionId from DocDetail where id=" + docid + ")");
                } else {
                    rs2.execute("update DocDetail set docStatus=2 where id=" + docid);
                }
                return false;
            }
        } else if ("2".equals(isOpenApproveWf) && !"3".equals(oldstatus) && workflowId > 0) { //批准工作流、并且不在审批状态
            String _workflowId = WorkflowVersion.getActiveVersionWFID(workflowId+"");

            String userid = user.getUID() + "";
            String docsubject = rs.getString("docsubject");
            RequestInfo ri = new RequestInfo();
            ri.setIsNextFlow("1"); //是否创建后流转流程. 0:不流转. 1:流转
            ri.setDescription(docsubject);
            ri.setWorkflowid(_workflowId);
            ri.setCreatorid("" + userid);
            ri.setRequestlevel("0");
            ri.setRemindtype("0");
            MainTableInfo mi = new MainTableInfo();

			/*List<Property> propertys = this.getDocWfRef(secid + "",workflowId + "",approveType,user,docid);
			if(propertys != null){
				for(Property p : propertys){
					mi.addProperty(p);
				}
			}*/

            ri.setMainTableInfo(mi);

            String requestid1 = "";
            try {
            	boolean secretFlag = CheckPermission.isOpenSecret();
            	if(secretFlag){
            		DocManager dm = new DocManager();
            		dm.resetParameter();
            		dm.setId(docid);
            		dm.getDocInfoById();
            		ri.setSecLevel(dm.getDocSecretLevel() + "");
            	}
                requestid1 = new RequestService().createRequest(ri);
            } catch (Exception e) {
                rs.writeLog(e);
            }

            if (Util.getIntValue(requestid1, 0) > 0) {

                rs2.execute("update bill_Approve set approveid=" + docid + ",docid=" + docid + ",approvetype=9,gopage='/docs/docs/DocApprove.jsp?id=',status=0 where REQUESTID=" + requestid1);
				
				rs2.execute("update workflow_requestbase set docids='" + docid + "' where REQUESTID=" + requestid1);

                rs2.execute("select userid from workflow_currentoperator where ((isremark = '0' and (takisremark is null or takisremark=0)) or isremark = '1' or isremark = '5' or  isremark = '8' or isremark = '9' or isremark = '7')  and islasttimes = 1 and requestid=" + requestid1);

                String usertypes = "";
                String resourceids = "";
                while (rs2.next()) {
                    resourceids += "," + rs2.getString("userid");
                    usertypes += ",0";
                }

                if (!usertypes.isEmpty()) {
                    resourceids = resourceids.substring(1);
                    usertypes = usertypes.substring(1);

                    DocShareUtil.addDocShare(user, docid + "", usertypes, resourceids, 1);
                }

            } else {
                rs2.writeLog("^^^^^^^^(" + docid + ")触发批准失败(" + requestid1 + ")^^^^^^");
                rs2.execute("update DocDetail set docStatus=0 where id=" + docid);
                rs2.execute("update docdetail set docstatus=7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition < (select docedition from DocDetail where id=" + docid + ") and doceditionid > 0 and doceditionid = (select docEditionId from DocDetail where id=" + docid + ")");
                return false;
            }
        }

        if (!"3".equals(currentStatus)) {
            rs2.execute("update DocDetail set docStatus=3,approveType=" + Util.getIntValue(approveType, 1) + " where id=" + docid);
        } else {
            rs2.execute("update DocDetail set approveType=" + Util.getIntValue(approveType, 1) + " where id=" + docid);
        }


        return true;
    }


    public List<Property> getDocWfRef(String id, String approveWfId, String approveType, User user, int docid) {
        List<Property> propertys = new ArrayList<Property>();

        RecordSet rs = new RecordSet();
        rs.executeQuery("select * from DocDetail where id=?", docid);
        if (!rs.next()) {
            return propertys;
        }

        try {
            Map data = new SecCategoryApproveWfManager().getApproveWfTRList(id, approveType, approveWfId, user.getLanguage(), false);
            List<DocApproveWfDetailBean> list = Lists.newArrayList();
            if (data != null) {
                List approveWfFieldList = (List) data.get("approveWfFieldList");
                // Map approveWfDetailIdMap = (Map) data.get("approveWfDetailIdMap");
                Map docPropertyFieldIdMap = (Map) data.get("docPropertyFieldIdMap");
                Map docPropertyNameMap = (Map) data.get("docPropertyNameMap");
                for (int i = 0; i < approveWfFieldList.size(); i++) {
                    Map approveWfFieldMap = (Map) approveWfFieldList.get(i);
                    String approveWfFieldId = Util.null2String(approveWfFieldMap.get("approveWfFieldId"));
                    String approveWfFieldName = Util.null2String(approveWfFieldMap.get("approveWfFieldName"));
                    String approveWfFieldHtmlType = Util.null2String(approveWfFieldMap.get("approveWfFieldHtmlType"));
                    String approveWfFieldType = Util.null2String(approveWfFieldMap.get("approveWfFieldType"));
                    String approveWfFieldFieldname = Util.null2String(approveWfFieldMap.get("approveWfFieldFieldname"));


                    if ("3".equals(approveWfFieldHtmlType) && "9".equals(approveWfFieldType)) {  //单文档字段


                    }


                    //获得子流程字段对应的子流程设置明细id
                    // String approveWfDetailId = Util.null2String(approveWfDetailIdMap.get(approveWfFieldId));

                    //获得流程字段对应的文档属性页字段id
                    String docPropertyFieldId = Util.null2String(docPropertyFieldIdMap.get(approveWfFieldId));
                    //获得流程字段对应的文档属性页字段名称
                    // String docPropertySpan = "";

                    if (!docPropertyFieldId.equals("")) {
                        //String docPropertyName = Util.null2String(docPropertyNameMap.get(docPropertyFieldId));
                        // if (docPropertyName == null) {
                        //    docPropertyName = "";
                        //}
                        //docPropertySpan = docPropertyName;
                        int type = Util.getIntValue(docPropertyFieldId);
                        if (type > 0) {
                            SecCategoryDocPropertiesComInfo propComInfo = new SecCategoryDocPropertiesComInfo();
                            propComInfo.addDefaultDocProperties(Util.getIntValue(id));
                            type = Util.getIntValue(propComInfo.getType(), 0);
                        }
                        String value = "";
                        DocComInfo dci = new DocComInfo();
                        switch (type) {
                            case -2:   //创建人
                                value = rs.getString("doccreaterid");
                                break;
                            case -3:   //当前操作者
                                break;
                            case -4:   //共享范围
                                break;
                            case 1: //1 文档标题
                                value = docid + "";
                                break;
                            case 2: //2 文档编号
                                value = rs.getString("doccode");
                                break;
                            case 3: //发布
                                if ("2".equals(rs.getString("DOCPUBLISHTYPE"))) {
                                    value = SystemEnv.getHtmlLabelName(227, user.getLanguage());
                                } else if ("3".equals(rs.getString("DOCPUBLISHTYPE"))) {
                                    value = SystemEnv.getHtmlLabelName(229, user.getLanguage());
                                } else {
                                    value = SystemEnv.getHtmlLabelName(58, user.getLanguage());
                                }
                                break;
                            case 4://4 文档版本
                                value = dci.getEditionView(docid);
                                break;
                            case 5://5 文档状态
                                value = dci.getStatusView(docid, user);
                                break;
                            case 6://6 主目录
                                break;
                            case 7://7 分目录
                                break;
                            case 8://8 子目录
                                value = rs.getString("seccategory");
                                break;
                            case 9://9 部门
                                value = rs.getString("docdepartmentid");
                                break;
                            case 10://10 模版
                                value = rs.getString("selectedpubmouldid");
                                break;
                            case 11://11 语言
                                LanguageComInfo lci = new LanguageComInfo();
                                value = lci.getLanguagename(rs.getString("doclangurage"));
                                break;
                            case 12://12 关键字
                                value = rs.getString("keyword");
                                break;
                            case 13:    //13 创建
                                value = rs.getString("doccreaterid");
                                break;
                            case 14://14 修改
                                value = rs.getString("doclastmoduserid");
                                break;
                            case 15://15 批准
                                value = rs.getString("docapproveuserid");
                                break;
                            case 16://16 失效
                                value = rs.getString("docinvaluserid");
                                break;
                            case 17://17 归档
                                value = rs.getString("docarchiveuserid");
                                break;
                            case 18://18 作废
                                value = rs.getString("doccanceluserid");
                                break;
                            case 19://19 主文档
                                value = rs.getString("maindoc");
                                if (value == null || value.equals("") || value.equals("0") || value.equals("-1")) {
                                    value = docid + "";
                                }
                                break;
                            case 20://20 被引用列表
                                break;
                            case 21://21 文档所有者
                                value = rs.getString("ownerid");
                                break;
                            case 22://22 失效时间
                                value = rs.getString("invalidationdate");
                                break;
                            case 24://24 虚拟目录
                                String strSql = "select catelogid from DocDummyDetail where docid=" + docid;
                                RecordSet rs2 = new RecordSet();
                                rs2.executeQuery(strSql);
                                String dummyIds = "";
                                while (rs.next()) {
                                    dummyIds += "," + Util.null2String(rs.getString(1));
                                }
                                if (!dummyIds.isEmpty()) {
                                    value = dummyIds.substring(1);
                                }
                                break;
                            case 25://可打印份数
                                value = rs.getString("canprintednum");
                                break;
                            default:
                                break;

                        }

                        Property property = new Property();
                        property.setName(approveWfFieldFieldname);
                        property.setValue(value);
                        propertys.add(property);
                    }
                    // list.add(new DocApproveWfDetailBean(approveWfDetailId, approveWfFieldName, docPropertyFieldId, docPropertySpan, approveWfFieldId));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return propertys;
    }

    public void senMessage(int docId, String docstatus, User user){
        senMessage(docId, docstatus, user,null);
    }

    public void senMessage(int docId, String docstatus, User user,HttpServletRequest request) {

        //设置推送提醒 start
        try {
            DesUtils desUtils = new DesUtils();
            String docidcode = desUtils.encrypt(docId+"");
            if(("1".equals(docstatus) || "2".equals(docstatus) || "5".equals(docstatus)) && DocoutShareManager.hasjsonFile(docidcode)){
                Map<String,Object> params = new HashMap<>();
                params.put("docid",docidcode);
                new DocoutShareManager().createJsonFile(user,params);
            }
            Map<String,Object> sendParams = new HashMap<>();
            if(request != null){
	            int port = request.getServerPort();
	            String url = request.getScheme()+"://"+request.getServerName();
	            if(port>0){
	                url = url + ":" + port;
	            }
	            sendParams.put("requesturl",url);
            }
           SendMsgForNewDocThread sendThread = new SendMsgForNewDocThread(user,docId,sendParams);
           sendThread.start();
        } catch (Exception e) {
            BaseBean bb = new BaseBean();
            bb.writeLog(e);
        }
        //设置推送提醒 end
    }
	    /**
     * QC1025422 批量阅读提醒和全部阅读提醒
     * @param docId 文档id
     * @param userids 要提醒的用户ids:  15,12,10,9
     * @param user
     * @param request
     */
    public void senMessage2Users(int docId,String userids,User user,HttpServletRequest request) {

        //设置推送提醒 start
        try {
            Map<String,Object> sendParams = new HashMap<>();
            if(request != null){
                int port = request.getServerPort();
                String url = request.getScheme()+"://"+request.getServerName();
                if(port>0){
                    url = url + ":" + port;
                }
                sendParams.put("requesturl",url);
            }
            sendParams.put("userids",userids);
            SendMsgForNewDocThread sendThread = new SendMsgForNewDocThread(user,docId,sendParams);
            sendThread.start();
        } catch (Exception e) {
            BaseBean bb = new BaseBean();
            bb.writeLog(e);
        }
        //设置推送提醒 end
    }

    public Map<String, Object> afterSave(User user, int docid, Map<String, String> paramMap) {
        return afterSave(user,docid,paramMap,null);
    }

    /**
     * 弹出默认共享之后,修改文档状态、触发流程等
     *
     * @author wangqs
     */
    public Map<String, Object> afterSave(User user, int docid, Map<String, String> paramMap,HttpServletRequest request) {
        Map<String, Object> apidatas = new HashMap<String, Object>();

        if (docid == 0) {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(19711, user.getLanguage()));
            return apidatas;
        }

        String f_weaver_belongto_userid = paramMap.get("f_weaver_belongto_userid");
        //String f_weaver_belongto_usertype = paramMap.get("f_weaver_belongto_usertype");
        if (f_weaver_belongto_userid != null && Util.getIntValue(f_weaver_belongto_userid) > 0) {
            user = new User(Util.getIntValue(f_weaver_belongto_userid));
        }

        RecordSet rs = new RecordSet();

        rs.execute("select d.docstatus,d.seccategory,d.scheduledreleasedate,d.docEditionId,d.docEdition from DocDetail d where d.id=" + docid);

        if (!rs.next()) {
            apidatas.put("status", -1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(23230, user.getLanguage()));
            return apidatas;
        }

        int secid = rs.getInt("seccategory");
        int docStatus = rs.getInt("docstatus");
        String oldstatus = docStatus + "";
		String scheduledreleasedate=Util.null2String(rs.getString("scheduledreleasedate"));
		int currentDocdocEditionId=Util.getIntValue(rs.getString("docEditionId"),-1);
		int currentDocdocEdition=Util.getIntValue(rs.getString("docEdition"),-1);
        SecCategoryComInfo scc = new SecCategoryComInfo();

        String valiSql = "";

        if (scc.needApprove(secid)) {//如果需要审批
            // docStatus = blnOsp ? "-3" : "3";
            docStatus = 3;
        } else if (scc.needPubOperation(secid)) {//如果需要发布
            // docStatus = blnOsp ? "-6" : "6";
            docStatus = 6;
        } else {
            docStatus = 1;
            valiSql = ",docvaliduserid=" + user.getUID() +
                    ",DocValidUserType=" + user.getType() +
                    ",docvaliddate='" + TimeUtil.getCurrentDateString() + "'" +
                    ",docvalidtime='" + TimeUtil.getOnlyCurrentTimeString() + "'";

        }

        rs.execute("update DocDetail set docstatus=" + docStatus + valiSql + " where id=" + docid);
		//rs.writeLog("DocSaveService--------afterSave----------docid="+docid+";docStatus="+docStatus+";scheduledreleasedate="+scheduledreleasedate);
		if (false && docStatus==6&&!"".equals(scheduledreleasedate)) {
			String currentDate=TimeUtil.getCurrentDateString();
			String currentTime=TimeUtil.getOnlyCurrentTimeString();
			//rs.writeLog("DocSaveService--------afterSave----------docid="+docid+";docStatus="+docStatus+";scheduledreleasedate="+scheduledreleasedate+";time="+(scheduledreleasedate.compareTo(currentDate)));
			if(scheduledreleasedate.compareTo(currentDate)<=0){
				if (scc.isEditionOpen(secid)) {//如果版本管理开启
					if (currentDocdocEditionId<=0) {
						try{
							DocManager dm = new DocManager();
							currentDocdocEditionId = dm.getNextEditionId(rs);
							currentDocdocEdition = 1;
							rs.executeSql("UPDATE DocDetail SET doceditionid=" +currentDocdocEditionId+",docEdition="+currentDocdocEdition+" where id="+docid);
						}catch(Exception e){};
					}else{
						if(currentDocdocEditionId>0 && currentDocdocEdition<=0){
							DocComInfo dc = new DocComInfo();
							currentDocdocEdition = dc.getEdition(currentDocdocEditionId) + 1;
							rs.executeSql("UPDATE DocDetail SET docEdition="+currentDocdocEdition+" where id="+docid);
							rs.executeSql("update docdetail set docstatus = 7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition <= " + currentDocdocEdition + " and doceditionid > 0 and doceditionid = " + currentDocdocEditionId);
						}								
					}							
				}
				rs.executeSql(
						" UPDATE DocDetail SET " +
						" docstatus = 2" +						
						",docpubuserid = " + user.getUID() +
						",docPubUserType = '" + user.getLogintype() + "'" +                     
						",docpubdate = '" + currentDate + "'" +
						",docpubtime = '" + currentTime + "'" +
						",docvaliduserid = " + user.getUID() +
						",docValidUserType = '" + user.getLogintype() + "'" +                     
						",docvaliddate = '" + currentDate + "'" +
						",docvalidtime = '" + currentTime + "'" +
						" WHERE ID = " + docid
				);
			}
		}

        String params = "";

        //时间戳,保证页面刷新
        Calendar c = Calendar.getInstance();
        params += "&__random=" + c.getTimeInMillis();

        params += "&" + DocSptm.DOC_ROOT_FLAG + "=1";

        DocViewPermission dvp = new DocViewPermission();
        params += dvp.getMoudleParams(paramMap);

        apidatas.put("status", 1);
        apidatas.put("link", DocSptm.DOC_DETAIL_LINK + "?id=" + docid + params + DocSptm.DOC_DETAIL_ROUT);

        boolean success = true;
        if (docStatus == 3) {
            success = approveWorkflow(docid, oldstatus, "1", user);
        }

        if (success) {
            changeSend(docid);
            senMessage(docid, docStatus + "", user,request);
        }

        return apidatas;
    }

    /**
     * 增加基本信息
     *
     * @author wangqs
     */
    public void addBaseInfos(Map<String, String> columnMap,
                             User user, String docid, boolean isadd) {
        String date = "'" + TimeUtil.getCurrentDateString() + "'";
        String time = "'" + TimeUtil.getOnlyCurrentTimeString() + "'";

        addBaseInfo(columnMap, "DOCREPLYABLE", "1");
        addBaseInfo(columnMap, "ISREPLY", "0");
        addBaseInfo(columnMap, "ISHISTORY", "0");
        addBaseInfo(columnMap, "REPLYDOCID", "0");
        addBaseInfo(columnMap, "DOCLASTMODUSERID", user.getUID() + "");
        addBaseInfo(columnMap, "DOCLASTMODUSERTYPE", user.getLogintype());
        addBaseInfo(columnMap, "DOCLASTMODDATE", date);
        addBaseInfo(columnMap, "DOCLASTMODTIME", time);
        addBaseInfo(columnMap, "DOCLANGURAGE", user.getLanguage() + "");

        addBaseInfo(columnMap, "CHECKOUTSTATUS", "'0'");
        addBaseInfo(columnMap, "CHECKOUTUSERID", "0");
        addBaseInfo(columnMap, "CHECKOUTUSERTYPE", "''");
        addBaseInfo(columnMap, "CHECKOUTDATE", "''");
        addBaseInfo(columnMap, "CHECKOUTTIME", "''");

        if (isadd) {
            addBaseInfo(columnMap, "DOCCREATERID", user.getUID() + "");
            addBaseInfo(columnMap, "DOCCREATERTYPE", user.getLogintype());
            addBaseInfo(columnMap, "USERTYPE", user.getLogintype());
            addBaseInfo(columnMap, "DOCCREATEDATE", date);
            addBaseInfo(columnMap, "DOCCREATETIME", time);

            addBaseInfo(columnMap, "DOCAPPROVEUSERID", "0");
            addBaseInfo(columnMap, "DOCAPPROVEUSERTYPE", "''");
            addBaseInfo(columnMap, "DOCAPPROVEDATE", "''");
            addBaseInfo(columnMap, "DOCAPPROVETIME", "''");
            addBaseInfo(columnMap, "DOCARCHIVEUSERID", "0");
            addBaseInfo(columnMap, "DOCARCHIVEUSERTYPE", "''");
            addBaseInfo(columnMap, "DOCARCHIVEDATE", "''");
            addBaseInfo(columnMap, "DOCARCHIVETIME", "''");

            addBaseInfo(columnMap, "REPLAYDOCCOUNT", "0");

            //物品
            addBaseInfo(columnMap, "ITEMID", "0");
            addBaseInfo(columnMap, "ITEMMAINCATEGORYID", "0");
            //财务
            addBaseInfo(columnMap, "FINANCEID", "0");

        }

        // parentids

    }

    public void addBaseInfo(Map<String, String> columnMap,
                            String key, String value) {
        columnMap.put(key.toUpperCase(), value);
    }

    
    /**
     * 修改密级(供其他模块调用)
     * */
    public boolean updateDocSecretLevel(int docid,String secretLevel,User user){
    	RecordSet rs = new RecordSet();
    	rs.writeLog("^^^^^^^^^^^^^^^^^^^修改文档密级("+docid+"):" + secretLevel);
    	String sql = "update DocDetail set secretLevel=? where id=?";
    	
    	boolean flag = rs.executeUpdate(sql, secretLevel,docid);
    	
    	return flag;
    }

    public int accForDoc(int secid,int fileid,User user) throws Exception{
    	return accForDoc(secid,fileid,DocManager.DEFAILT_SECRET_LEVEL,user);
    }
    public int accForDoc(int secid,int fileid,int secretLevel,User user) throws Exception{
        return accForDoc(secid,fileid,secretLevel,user,null);
    }
    public int accForDoc(int secid, int fileid, int secretLevel, User user,HttpServletRequest request) throws Exception {
        return accForDoc(secid,fileid,secretLevel,user,request,"");
    }
    public int accForDoc(int secid,int fileid,User user,String operateType) throws Exception{
        return accForDoc(secid,fileid,DocManager.DEFAILT_SECRET_LEVEL,user,null,operateType);
    }
    /**
     * 附件生成文档
     *
     * @author wangqs
     */
    public int accForDoc(int secid, int fileid, int secretLevel, User user,HttpServletRequest request,String operateType) throws Exception {

        if (secid <= 0 || fileid <= 0 || user == null) {
            return -1;
        }

        DocIdUpdate docIdUpdate = new DocIdUpdate();
        RecordSet rs = new RecordSet();

        // 判断目录设置
        SecCategoryComInfo scc = new SecCategoryComInfo();
        String docStatus = "";

        //boolean blnOsp = scc.isSetShare(secid);  //是否弹出默认提示


        rs.execute("select a.imagefilename,b.docid from ImageFile  a left join docimagefile b on a.imagefileid=b.imagefileid " +
                " where a.imagefileid=" + fileid);
        rs.next();
        String olddocid = Util.null2String(rs.getString("docid"));
        if (!olddocid.isEmpty()) {
            return Util.getIntValue(olddocid, 0);
        }

        int docid = docIdUpdate.getDocNewId();
        String fileFullName = Util.null2String(rs.getString("imagefilename"));
        String docextendname = fileFullName.contains(".") ? fileFullName.substring(fileFullName.lastIndexOf(".") + 1) : "";
        String docsubject = fileFullName.contains(".") ? fileFullName.substring(0, fileFullName.lastIndexOf(".")) : fileFullName;

        DocManager dm = new DocManager();
        dm.resetParameter();
        dm.setId(docid);
        dm.setSecretLevel(secretLevel);
        dm.setSeccategory(secid);
        dm.setDocsubject(docsubject);
        dm.setDocextendname("html");
        
        rs.executeQuery("select defaultDummyCata from DocSecCategory where id=?",secid);
        if(rs.next()){
        	String dummyIds = Util.null2String(rs.getString("defaultDummyCata"));
        	dummyIds = dummyIds.startsWith(",") ? dummyIds.substring(1) : dummyIds;
            dummyIds = dummyIds.endsWith(",") ? dummyIds.substring(0, dummyIds.length() - 1) : dummyIds;
            if (PatternUtil.isAllNumber(dummyIds) || "".equals(dummyIds)) {
            	dm.setDummycata(dummyIds);
            }
        }
        
        String formatdate = TimeUtil.getCurrentDateString();
        String formattime = TimeUtil.getOnlyCurrentTimeString();

        if (scc.needApprove(secid)) {//如果需要审批
            docStatus = "3";//blnOsp ? "-3" : "3";
            dm.setApproveType(1);
        } else if (scc.needPubOperation(secid)) {//如果需要发布
            docStatus = "6";//blnOsp ? "-6" : "6";
        } else {//生效/正常
            docStatus = "1";//blnOsp ? "-1" : "1";
            dm.setDocValidUserId(user.getUID());
            dm.setDocValidUserType(user.getType() + "");
            dm.setDocValidDate(formatdate);
            dm.setDocValidTime(formattime);
        }

        dm.setDocstatus(docStatus);
        dm.setAccessorycount(1);
        dm.setDoccreaterid(user.getUID());

        dm.setDoccreatedate(formatdate);
        dm.setDoccreatetime(formattime);
        dm.setDoclastmoddate(formatdate);
        dm.setDoclastmodtime(formattime);
        dm.setDoclastmoduserid(user.getUID());
        dm.setUserid(user.getUID());
        dm.setOwnerid(user.getUID());
        dm.setOwnerType("" + user.getLogintype());
        dm.setDocdepartmentid(user.getUserDepartment());

        dm.setDoclangurage(user.getLanguage());
        dm.setUsertype("" + user.getLogintype());

        //是否可打印
        int readOpterCanPrint = 0;
        //是否可订阅
        String orderable = "";
        rs.execute("select readoptercanprint,orderable from DocSecCategory where id=" + secid);
        if (rs.next()) {
            readOpterCanPrint = Util.getIntValue(rs.getString("readOpterCanPrint"), 0);
            orderable = Util.null2String(rs.getString("orderable"));
        }

        dm.setReadOpterCanPrint(readOpterCanPrint);
        dm.setOrderable(orderable);
        dm.setDocLastModUserType(user.getLogintype());
        dm.setMainDoc(docid);

        dm.AddDocInfo();
        dm.AddShareInfo();

        DocViewer docViewer = new DocViewer();
        docViewer.setDocShareByDoc("" + docid);

        DocImageManager imgManger = new DocImageManager();
        imgManger.resetParameter();
        imgManger.setImagefilename(fileFullName);
        String ext = docextendname;
        if (ext.equalsIgnoreCase("doc")) {
            imgManger.setDocfiletype("3");
        } else if (ext.equalsIgnoreCase("xls")) {
            imgManger.setDocfiletype("4");
        } else if (ext.equalsIgnoreCase("ppt")) {
            imgManger.setDocfiletype("5");
        } else if (ext.equalsIgnoreCase("wps")) {
            imgManger.setDocfiletype("6");
        } else if (ext.equalsIgnoreCase("docx")) {
            imgManger.setDocfiletype("7");
        } else if (ext.equalsIgnoreCase("xlsx")) {
            imgManger.setDocfiletype("8");
        } else if (ext.equalsIgnoreCase("pptx")) {
            imgManger.setDocfiletype("9");
        } else if(ext.equalsIgnoreCase("et")){
        	imgManger.setDocfiletype("10");
        }else {
            imgManger.setDocfiletype("2");
        }
        imgManger.setDocid(docid);
        imgManger.setImagefileid(fileid);
        imgManger.setIsextfile("1");
        if("upload".equals(operateType)){
            imgManger.setOperateuserid(user.getUID());
            imgManger.setVersionDetail("128095");
        }
        imgManger.AddDocImageInfo();
        copyDocFile(secid,docid);
        boolean success = true;
        if ("3".equals(docStatus)) {
            success = approveWorkflow(docid, "", "1", user);
        }

        if (success) {
            changeSend(docid);
            senMessage(docid, docStatus + "", user,request);
        }

        return docid;
    }

    /**
     * 处理文档内容
     *
     * @author wangqs
     */
    public String replaceContent(int docid, String doccontent, boolean copyForVersion) {
        // 存储文档的内容
        doccontent = Util.htmlFilter4UTF8(doccontent);
        // 替换文档内容中可能存在的 textarea|input|form 标签
        String rex_html = "<(textarea|input|form)[^>]*>";
        String rex_html2 = "</(textarea|form)>";

        Pattern p_script = Pattern.compile(rex_html,
                Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_script.matcher(doccontent);
        doccontent = m_html.replaceAll("");

        p_script = Pattern.compile(rex_html2,
                Pattern.CASE_INSENSITIVE);
        m_html = p_script.matcher(doccontent);
        doccontent = m_html.replaceAll("");

        doccontent = this.updateDocImageFileOfPic(docid, doccontent, copyForVersion);

        String moduleimages[] = null;// fu.getParameters("moduleimages");

        // 如果文档中图片的路径是全路径则将其改为相对路径使其不含服务器的路径。
        if (moduleimages != null && moduleimages.length > 0) {
            int tmppos = doccontent
                    .indexOf("/weaver/weaver.file.FileDownload?fileid=");
            while (tmppos != -1) {
                int startpos = doccontent.lastIndexOf("\"", tmppos);
                if ((tmppos - startpos) > 1) {
                    String tmpcontent = doccontent.substring(0, startpos + 1);
                    tmpcontent += doccontent.substring(tmppos);
                    doccontent = tmpcontent;
                    tmppos = doccontent.indexOf(
                            "/weaver/weaver.file.FileDownload?fileid=",
                            tmppos + 1);
                } else
                    tmppos = doccontent.indexOf(
                            "/weaver/weaver.file.FileDownload?fileid=",
                            tmppos + 1);
            }
        }
        return doccontent;
    }

    /**
     * 获取标题 及右键菜单
     *
     * @author wangqs
     */
    public Map<String, Object> getBasicInfo(int docid, int secid, User user, Map<String, String> params) {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        BaseBean bb = new BaseBean();
        List<RightMenu> rightMenus = new ArrayList<RightMenu>();
        DocMenuManager docMenuManager = new DocMenuManager();
        Map<String,DocMenuBean> showRightMenuMap = docMenuManager.getDocMenuSet();
        int language = user.getLanguage();
        String docsubject = "";
        if(showRightMenuMap.get(DocMenuManager.SUBMIT).getIsopen()==1){
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_SUBMIT, "", showRightMenuMap.get(DocMenuManager.SUBMIT).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.SUBMIT).getColumname()));
        }
        if(showRightMenuMap.get(DocMenuManager.DRAFT).getIsopen()==1){
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_DRAFT, "", showRightMenuMap.get(DocMenuManager.DRAFT).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DRAFT).getColumname()));
        }

        if (docid > 0) { //编辑
            RecordSet rs = new RecordSet();
            rs.execute("select docsubject from DocDetail where id=" + docid);
            if (rs.next()) {
                docsubject = Util.null2String(rs.getString("docsubject"));
                docsubject = docsubject.replaceAll("\n", "");
                docsubject = docsubject.replaceAll("&lt;", "<");
                docsubject = docsubject.replaceAll("&gt;", ">");
                docsubject = docsubject.replaceAll("&quot;", "\"");
                docsubject = docsubject.replaceAll("&#8226;", "·");
            }
        }
        //if (params != null && "1".equals(params.get("isIE")) && "1".equals(params.get("isCreate"))) {

        String officeType = Util.null2String(params.get("officeType"));

        boolean createOffice = false;
        if("1".equals(params.get("isIE"))){
        	createOffice = true;
        }
        boolean yozhongFlag = ImageConvertUtil.canEditForYozo("doc",user);
        if(!yozhongFlag){
        	yozhongFlag = ImageConvertUtil.canEditForWps("doc",user);
        }
        if(yozhongFlag && (docid > 0 || secid > 0)){
        	Map<String,String> customData = new HashMap<String,String>();
        	int maxUploadFileSize = 0;
        	int minUploadFileSize = 0;
        	RecordSet rs = new RecordSet();
        	String filetype = "";
        	if(docid > 0){
        		rs.executeQuery("select a.maxUploadFileSize,a.minUploadFileSize,b.docextendname from DocSecCategory a,DocDetail b where a.id=b.seccategory and b.id=?",docid);
        		if(rs.next()){
        			maxUploadFileSize = rs.getInt("maxUploadFileSize");
                    minUploadFileSize = rs.getInt("minUploadFileSize");
        			filetype = rs.getString("docextendname");
        		}
        	}else if(secid > 0){
        		rs.executeQuery("select a.maxUploadFileSize,a.minUploadFileSize from DocSecCategory a where a.id=?",secid);
        		if(rs.next()){
        			maxUploadFileSize = rs.getInt("maxUploadFileSize");
                    minUploadFileSize = rs.getInt("minUploadFileSize");
        		}
        		filetype = Util.null2String(params.get("currentType")).replace(".","");
        	}
        	filetype = filetype.toLowerCase();
        //	if("doc".equals(filetype) || "docx".equals(filetype) || "xls".equals(filetype) || "xlsx".equals(filetype)){
            if(showRightMenuMap.get(DocMenuManager.OPENLOCAL).getIsopen()==1){
	        	RightMenu menu = new RightMenu(language, RightMenuType.BTN_OPEN_LOCAL_FILE, "", showRightMenuMap.get(DocMenuManager.OPENLOCAL).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.OPENLOCAL).getColumname());
	        	customData.put("maxUploadSize",maxUploadFileSize + "");
	        	customData.put("mixUploadSize",minUploadFileSize + "");
	        	//ImageConvertUtil icu = new ImageConvertUtil();
	        	//customData.put("uploadUrl",icu.getYozoClient() + "/uploadFile.do");
	        	String _filetype = "";
	        		
	        	DocAccService accService = new DocAccService();
	        	if("doc".equals(filetype) || "docx".equals(filetype) || "xls".equals(filetype) || "xlsx".equals(filetype)){
	        		_filetype = accService.getReplaceType(filetype);
	        	}else{
	        		_filetype = accService.getReplaceType("docx") + "," + accService.getReplaceType("xlsx");
	        	}
				customData.put("limitType",_filetype);
				menu.setCustomData(customData);
	        	rightMenus.add(0,menu);
            }
        //	}
        }
        if(!createOffice){
        	createOffice = yozhongFlag;
        }
        boolean iweboffice = false;
        if(!createOffice){
        	String agent = params.get("agent");
        	iweboffice = IWebOfficeConf.canIwebOffice(agent,null);
        	createOffice = iweboffice;
        }
        if (params != null && "1".equals(params.get("isCreate")) && createOffice && showRightMenuMap.get(DocMenuManager.HTML).getIsopen()==1) {
            RightMenu htmlMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_HTML, "", showRightMenuMap.get(DocMenuManager.HTML).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.HTML).getColumname());
            String currentType = params.get("currentType");
            if (!"".equals(currentType)) {
                if(showRightMenuMap.get(DocMenuManager.HTML).getColumname().isEmpty()){
                    htmlMenu.setMenuName("HTML" + htmlMenu.getMenuName());
                }
                htmlMenu.setParams("");
                rightMenus.add(htmlMenu); // HTML文档
            }
       /*     //brs
            boolean isOpenWps = false;	//是否开启WPS
            try{
                RecordSet wps = new RecordSet();
                wps.executeQuery("select isopen from wps_config where type='wps'");
                if (wps.next()){
                    isOpenWps = wps.getInt("isopen")==1;
                }
            }catch (Exception e){
                new BaseBean().writeLog("brs -Exception- "+e);
            }
            if (!isOpenWps){
                yozhongFlag = false;
            }*/
            //==zj
            boolean isOpenWps = false;
            int uid = user.getUID();        //获取用户id
            new BaseBean().writeLog("==zj==（菜单--userid）" + uid);
            String subcompanyName = "";		//分部id
            String subcompanylist = "";		//分部wps权限数组

            try{
                RecordSet rs = new RecordSet();
                rs.executeQuery("select isopen from wps_config where type='wps'");
                if (rs.next()){
                    int isOpen = rs.getInt("isopen");
                    //如果开关是开启状态
                    if (isOpen == 1){
                        String hrmsql = "select id from hrmsubcompany where id = (select subcompanyid1 from Hrmresource where id =" +  uid + ")";
                        rs.executeQuery(hrmsql);
                        if (rs.next()){
                            subcompanyName = Util.null2String(rs.getString("id"));      //获取当前用户的分部id
                        }
                        new BaseBean().writeLog("==zj==(菜单--用户分部id)" + subcompanyName);
                        String wpssql = "select subcompanyname from uf_wps";
                        rs.executeQuery(wpssql);
                        if (rs.next()){
                            subcompanylist = Util.null2String(rs.getString("subcompanyname"));
                            new BaseBean().writeLog("==zj==(菜单--wps分部)" + subcompanylist);
                            String result[] = subcompanylist.split(",");
                            for (int i = 0; i < result.length; i++) {
                                if (subcompanyName.equals(result[i]) && !"".equals(subcompanyName)){
                                    isOpenWps = true;
                                    break;
                                }
                            }
                        }
                        new BaseBean().writeLog("==zj==（菜单--用户是否开启wps）" + isOpenWps);
                    }
                }
            }catch (Exception e){
                    new BaseBean().writeLog("==zj==(菜单)" + e);
            }
            if (!isOpenWps){
                yozhongFlag = false;
            }
            //--
            if(!yozhongFlag){
	            if (!".doc".equals(currentType) && showRightMenuMap.get(DocMenuManager.DOC).getIsopen()==1) {
	                RightMenu docMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_DOC, "", showRightMenuMap.get(DocMenuManager.DOC).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DOC).getColumname());
                    if(showRightMenuMap.get(DocMenuManager.DOC).getColumname().isEmpty()){
                        docMenu.setMenuName("DOC" + docMenu.getMenuName());
                    }
	                docMenu.setParams("&isOffice=1&officeType=.doc");
	                rightMenus.add(docMenu); // DOC文档
	            }
	            if (!".docx".equals(currentType) && showRightMenuMap.get(DocMenuManager.DOCX).getIsopen()==1) {
	                RightMenu docxMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_DOCX, "", showRightMenuMap.get(DocMenuManager.DOCX).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DOCX).getColumname());
                    if(showRightMenuMap.get(DocMenuManager.DOCX).getColumname().isEmpty()){
                        docxMenu.setMenuName("DOCX" + docxMenu.getMenuName());
                    }
	                docxMenu.setParams("&isOffice=1&officeType=.docx");
	                rightMenus.add(docxMenu); // DOCX文档
	            }
	            if (!".xls".equals(currentType) && showRightMenuMap.get(DocMenuManager.XLS).getIsopen()==1) {
	                RightMenu xlsMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_XLS, "", showRightMenuMap.get(DocMenuManager.XLS).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.XLS).getColumname());
                    if(showRightMenuMap.get(DocMenuManager.XLS).getColumname().isEmpty()){
                        xlsMenu.setMenuName("XLS" + xlsMenu.getMenuName());
                    }
	                xlsMenu.setParams("&isOffice=1&officeType=.xls");
	                rightMenus.add(xlsMenu); // XLS文档
	            }
	            if (!".xlsx".equals(currentType) && showRightMenuMap.get(DocMenuManager.XLSX).getIsopen()==1) {
	                RightMenu xlsxMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_XLSX, "", showRightMenuMap.get(DocMenuManager.XLSX).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.XLSX).getColumname());
                    if(showRightMenuMap.get(DocMenuManager.XLSX).getColumname().isEmpty()){
                        xlsxMenu.setMenuName("XLSX" + xlsxMenu.getMenuName());
                    }
	                xlsxMenu.setParams("&isOffice=1&officeType=.xlsx");
	                rightMenus.add(xlsxMenu); // XLSX文档
	            }
	            if("1".equals(params.get("isIE")) || iweboffice  || IWebOfficeConf.canIwebOffice() && showRightMenuMap.get(DocMenuManager.WPS).getIsopen()==1){	//IE支持、iwebchina、iweb2015
		            if (!".wps".equals(currentType)) {
		                RightMenu wpsMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_WPS, "", showRightMenuMap.get(DocMenuManager.WPS).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.WPS).getColumname());
                        if(showRightMenuMap.get(DocMenuManager.WPS).getColumname().isEmpty()){
                            wpsMenu.setMenuName("WPS" + wpsMenu.getMenuName());
                        }
		                wpsMenu.setParams("&isOffice=1&officeType=.wps");
		                rightMenus.add(wpsMenu); // WPS文档
		            }
	            }
            }else{
            	String newDoc = SystemEnv.getHtmlLabelName(82, user.getLanguage());
            	if (!".xls".equals(currentType) && !".xlsx".equals(currentType) && showRightMenuMap.get(DocMenuManager.CREATEEXCEL).getIsopen()==1) {
            		RightMenu excelMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_XLSX, "", showRightMenuMap.get(DocMenuManager.CREATEEXCEL).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.CREATEEXCEL).getColumname());
            		if(showRightMenuMap.get(DocMenuManager.CREATEEXCEL).getColumname().isEmpty()){
                        excelMenu.setMenuName(newDoc + "EXCEL" + excelMenu.getMenuName());
                    }
            		excelMenu.setParams("&isOffice=1&officeType=.xlsx");
            		rightMenus.add(0,excelMenu); 
            	}
            	if (!".doc".equals(currentType) && !".docx".equals(currentType) && showRightMenuMap.get(DocMenuManager.CREATEWORD).getIsopen()==1) {
            		 RightMenu wordMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_DOCX, "", showRightMenuMap.get(DocMenuManager.CREATEWORD).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.CREATEWORD).getColumname());
                	 if(showRightMenuMap.get(DocMenuManager.CREATEWORD).getColumname().isEmpty()){
                         wordMenu.setMenuName(newDoc + "WORD" + wordMenu.getMenuName());
                     }
 	                 wordMenu.setParams("&isOffice=1&officeType=.docx");
 	                 rightMenus.add(0,wordMenu); 
            	}
            }
        }
        DocSensitiveWordsUtil sensitiveWordsUtil = new DocSensitiveWordsUtil();
        if(sensitiveWordsUtil.enableOfficeSensitiveWordValidate() && sensitiveWordsUtil.canProcessOfficeType(officeType)) {
            if(rightMenus != null) {
                DocMenuBean docMenuBean = showRightMenuMap.get(DocMenuManager.SENSITIVEWORD_VALIDATE);
                boolean isOpenMenu = docMenuBean == null ? true : docMenuBean.getIsopen() == 1;
                if(isOpenMenu) {
                    boolean isTopMenu = docMenuBean == null ? true : docMenuBean.getQuickmenu() == 1;
                    String menuCustomName = docMenuBean == null ? "检查敏感词" : docMenuBean.getColumname();
                    RightMenu rightMenu = new RightMenu(language, RightMenuType.BTN_SENSITIVEWORD_VALIDATE, "", isTopMenu, menuCustomName);
                    List<RightMenu> newMenus = new ArrayList<>();
                    newMenus.add(rightMenu);
                    newMenus.addAll(rightMenus);
                    rightMenus = newMenus;
            	}
            }
        }
        dataMap.put("rightMenus", rightMenus);
        dataMap.put(DocParamItem.DOC_SUBJECT.getName(), StringEscapeUtils.unescapeHtml(docsubject));

        return dataMap;
    }

    /**
     * 截取不大于指定字节长度的字符串
     *
     * @param initString 初始字符串
     * @param byteLength 指定字节长度
     * @return String 不大于指定字节长度的字符串
     */
    private static String interceptString(String initString, int byteLength) {
        try {

            String returnString = "";
            int counterOfDoubleByte = 0;
            byte[] b = initString.getBytes("UTF-8");
            if (b.length <= byteLength)
                return initString;
            for (int i = 0; i < byteLength; i++) {
                if (b[i] < 0)
                    counterOfDoubleByte++;
            }

            if (counterOfDoubleByte % 2 == 0) {
                returnString = new String(b, 0, byteLength, "UTF-8");
            } else {
                returnString = new String(b, 0, byteLength - 1, "UTF-8");
            }

            List<String> noInterceptStringList = new ArrayList<String>();
            noInterceptStringList.add("&lt;");
            noInterceptStringList.add("&gt;");
            noInterceptStringList.add("&quot;");
            noInterceptStringList.add("<br>");

            List<String> partList = new ArrayList<String>();
            List<String> allList = new ArrayList<String>();

            String noInterceptString = null;
            for (int i = 0; i < noInterceptStringList.size(); i++) {
                noInterceptString = Util
                        .null2String((String) noInterceptStringList.get(i));
                for (int j = 1; j < noInterceptString.length(); j++) {
                    partList.add(noInterceptString.substring(0, j));
                    allList.add(noInterceptString);
                }
            }

            String partString = null;
            String allString = null;
            int lastIndex = 0;

            for (int i = 0; i < partList.size(); i++) {
                partString = (String) partList.get(i);
                allString = (String) allList.get(i);

                if (returnString.endsWith(partString)) {
                    lastIndex = returnString.lastIndexOf(partString);
                    if (lastIndex == initString.indexOf(allString, lastIndex)) {
                        returnString = initString.substring(0, lastIndex);
                        break;
                    }
                }
            }

            return returnString;

        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * 截取不大于指定字节长度的字符串
     * 会按照字符遍历字符串，而不是双字节直接截取，双字节直接截取容易造成结尾字符乱码
     * @param initString 初始字符串
     * @param byteLength 指定字节长度
     * @return String 不大于指定字节长度的字符串
     */
    private static String interceptString4Word(String initString, int byteLength) {
        try {

            String returnString = "";
            int counterOfDoubleByte = 0;
            StringBuilder stringBuilder = new StringBuilder();

            for (int i =0 ; i < initString.length(); i++) {
                String tempChar = initString.substring(i, i+1);
                int tempLenght = tempChar.getBytes("UTF-8").length;
                if((counterOfDoubleByte + tempLenght) > byteLength) {
                    stringBuilder.append("...");
                    break;
                }
                counterOfDoubleByte = counterOfDoubleByte + tempLenght;
                stringBuilder.append(tempChar);
            }

            returnString = stringBuilder.toString();

            List<String> noInterceptStringList = new ArrayList<String>();
            noInterceptStringList.add("&lt;");
            noInterceptStringList.add("&gt;");
            noInterceptStringList.add("&quot;");
            noInterceptStringList.add("<br>");

            List<String> partList = new ArrayList<String>();
            List<String> allList = new ArrayList<String>();

            String noInterceptString = null;
            for (int i = 0; i < noInterceptStringList.size(); i++) {
                noInterceptString = Util
                        .null2String((String) noInterceptStringList.get(i));
                for (int j = 1; j < noInterceptString.length(); j++) {
                    partList.add(noInterceptString.substring(0, j));
                    allList.add(noInterceptString);
                }
            }

            String partString = null;
            String allString = null;
            int lastIndex = 0;

            for (int i = 0; i < partList.size(); i++) {
                partString = (String) partList.get(i);
                allString = (String) allList.get(i);

                if (returnString.endsWith(partString)) {
                    lastIndex = returnString.lastIndexOf(partString);
                    if (lastIndex == initString.indexOf(allString, lastIndex)) {
                        returnString = initString.substring(0, lastIndex);
                        break;
                    }
                }
            }

            return returnString;

        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * 更新文档图片对应关系
     *
     * @param docid      文档id
     * @param doccontent 文档内容
     */
    private String updateDocImageFileOfPic(int docid, String doccontent, boolean copyForVersion) {

        DocImageManager imgManger = new DocImageManager();
        RecordSet rs = new RecordSet();

        List<String> htmlimagefileidlist = new ArrayList<String>();
        int tmppos = doccontent.indexOf("/weaver/weaver.file.FileDownload?fileid=");
        int imagefileid = 0;

        List<Map<String, String>> replaceimagefileidlist = new ArrayList<Map<String, String>>();

        while (tmppos != -1) {
            int imagefileidbeginpos = tmppos + "/weaver/weaver.file.FileDownload?fileid=".length();
            int imagefileidendpos1 = doccontent.indexOf("\"", tmppos);
            int imagefileidendpos2 = doccontent.indexOf("&", tmppos);
            //int imagefileidendpos=imagefileidendpos1<imagefileidendpos2?imagefileidendpos1:imagefileidendpos2;
            int imagefileidendpos = imagefileidendpos1;
            if (imagefileidendpos1 > imagefileidendpos2 && imagefileidendpos2 > tmppos) {
                imagefileidendpos = imagefileidendpos2;
            }

            if (imagefileidendpos > imagefileidbeginpos) {
            	String idStr = doccontent.substring(imagefileidbeginpos, imagefileidendpos);
                imagefileid = Util.getIntValue(idStr);
                
                if(imagefileid <= 0){
                	imagefileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileidstr(idStr));
                }

                rs.writeLog("^^^^^^^(idStr="+idStr+")(imagefileid="+imagefileid+")^^^^^^^^^");
                
                if (copyForVersion) {
                    int imageid = ImageFileManager.copyImageFile(imagefileid);
                    if (imageid > 0) {
                        htmlimagefileidlist.add("" + imageid);
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("oldid", idStr);
                        map.put("newid", imageid + "");
                        replaceimagefileidlist.add(map);
                    }
                } else {
                    if (imagefileid > 0) {
                        htmlimagefileidlist.add("" + imagefileid);
                    }
                }
            }
            tmppos = doccontent.indexOf("/weaver/weaver.file.FileDownload?fileid=", tmppos + 1);
        }

        rs.writeLog("^^^^^^^^^^^^^^^^replaceimagefileidlist=" + replaceimagefileidlist);
        for (Map<String, String> map : replaceimagefileidlist) {
            String oldid = map.get("oldid");
            String newid = map.get("newid");
            doccontent = doccontent.replace("/weaver/weaver.file.FileDownload?fileid=" + oldid,
                    "/weaver/weaver.file.FileDownload?fileid=" + newid);
        }

        /*** if(copyForVersion){ //生成版本时，拷贝附件时，会把图片也复制一份,在此删除掉这些图片
         rs.execute("select imagefileid from docimagefile where docid=" + docid + " and docfiletype=1");
         DocAccService accService = new DocAccService();
         boolean hasImg = false;
         while(rs.next()){
         hasImg = true;
         accService.deleteAcc(rs.getInt("imagefileid"), true);
         }
         if(hasImg)
         rs.execute("delete docimagefile where docid=" + docid + " and docfiletype=1");
         }
         ***/

        List<String> existsfileidlist = new ArrayList<String>();
        if (!copyForVersion) {
            rs.executeQuery("select imagefileid from docimagefile where docfiletype='1' and docid=" + docid);
            while (rs.next()) {
                existsfileidlist.add(rs.getString("imagefileid"));
            }
        }

        String delids = "";
        for (String fileid : htmlimagefileidlist) {
            if (Util.getIntValue(fileid, 0) == 0)
                continue;
            if (existsfileidlist.contains(fileid))
                continue;
            rs.executeQuery("select imagefilename from imagefile where imagefileid=" + fileid);
            if (!rs.next())
                continue;
            String filename = Util.null2String(rs.getString("imagefilename"));

            imgManger.resetParameter();
            imgManger.setDocid(docid);
            imgManger.setImagefileid(Util.getIntValue(fileid, 0));
            imgManger.setImagefilename(filename);
            imgManger.setDocfiletype("1");

            imgManger.AddDocImageInfo();
            delids += "," + fileid;
        }

        // 删除临时表数据
        if (!delids.isEmpty()) {
            delids = delids.substring(1);
            String delidsStr = delids.contains(",") ? (" in (" + delids + ")") : (" = " + delids);
            rs.execute("delete from imagefiletemp where imagefileid " + delidsStr);
        }

        delids = "";
        for (String fileid : existsfileidlist) {
            if (htmlimagefileidlist.contains(fileid))
                continue;
            delids += "," + fileid;
        }
        rs.writeLog("^^^^^^^^^^^^delids=" + delids);
        // 删除编辑被删掉的图片
        if (!delids.isEmpty()) {
            delids = delids.substring(1);
            String delidsStr = delids.contains(",") ? (" in (" + delids + ")") : (" = " + delids);
            rs.executeQuery("select imagefileid from DocImageFile where imagefileid" + delidsStr + " and docid!=" + docid);

            delids = "," + delids + ",";
            while (rs.next()) {
                delids = delids.replace("," + rs.getString("imagefileid") + ",", ",");
            }
			rs.writeLog("^^^^^^^^^^^^delids2=" + delids);
            DocAccService accService = new DocAccService();
            for (String delid : delids.split(",")) {
                if (delid.isEmpty())
                    continue;
				String delDocImSql="delete from DocImageFile where imagefileid = ? and docid= ?";
				rs.executeUpdate(delDocImSql,delid,docid);
                accService.deleteAcc(Util.getIntValue(delid, 0), true);
            }
        }


        return doccontent;

    }

     /**
     * 移动附件到对应目录
     * @param secid
     * @param docid
     */
    public static void copyDocFile(int secid,int docid) {

        int seccategoryid = secid;
        String sql = "select t.filesavepath from DocSecCategory t where t.id=?"; //根据目录id找到目录所属机构id
        RecordSet rs = new RecordSet();
        RecordSet rs1 = new RecordSet();
        rs.executeQuery(sql, "" + seccategoryid);
        if (rs.next()) {
            String targetpath = Util.null2String(rs.getString("filesavepath"));
            if (!"".equals(targetpath)) {
			 	targetpath = Util.StringReplace(targetpath , "\\" , "#$^123") ;
                targetpath = Util.StringReplace(targetpath , "/" , "#$^123") ;
                targetpath = Util.StringReplace(targetpath , "#$^123" , File.separator ) ;
                moveFile(targetpath,""+docid);

            } else {
				 SystemComInfo syscominfo = new SystemComInfo();
                String ddefaultpath = syscominfo.getFilesystem();
                if("".equals(ddefaultpath)){
                    ddefaultpath = GCONST.getRootPath();
                    ddefaultpath = ddefaultpath + "filesystem" + File.separatorChar;
                }
                moveFile(ddefaultpath,""+docid);
            }
        }
    }

    public static int occurTimes(String string, String a) {
        int pos = -2;
        int n = 0;

        while (pos != -1) {
            if (pos == -2) {
                pos = -1;
            }
            pos = string.indexOf(a, pos + 1);
            if (pos != -1) {
                n++;
            }
        }
        return n;
    }

    public static void moveFile(String targetpath,String docid) {
            RecordSet rs1 = new RecordSet();
            RecordSet rs = new RecordSet();
            String sql="select * from docimagefile a inner join imagefile b on  b.imagefileid=a.imagefileid where docid=?";
            rs.executeQuery(sql,docid);
            try{
                while(rs.next()){
                    String resourcepath=rs.getString("filerealpath");
                    if(!resourcepath.startsWith(targetpath)||(resourcepath.startsWith(targetpath)&&occurTimes(resourcepath.substring(targetpath.length()+1),File.separator)!=2)){
                        String createdir = FileUpload.getCreateDir(targetpath+File.separator);
                        String fileName = UUID.randomUUID().toString();
                        if (resourcepath.lastIndexOf(".") > -1) {
                            fileName += resourcepath.substring(resourcepath.lastIndexOf("."));
                        }
                        boolean flag = FileManage.createDir(createdir);
                        String new_fileRealPath =createdir+fileName;
                        if (flag) {
                            FileManage.copy(resourcepath, new_fileRealPath);
                            rs1.execute("update imagefile set filerealpath='" + new_fileRealPath + "' where imagefileid=" + rs.getString("imagefileid"));
                            FileManage.DeleteFile(resourcepath);
                        }
                    }
                }
            }catch(Exception ex){
                rs.writeLog("file move fail："+ex);
                ex.printStackTrace();
            }
     }
    
    /**
     * 开启涉密系统时，初始化流程相关资源，仅在开启涉密系统时调用一次,其他地方不要调用！！！
     * @return
     */
    public boolean initDocumentSecLevel(){
    	String defaultLevel = new HrmClassifiedProtectionBiz().getDefaultResourceSecLevel();
        RecordSet rs = new RecordSet();
        rs.executeUpdate("update DocDetail set secretLevel=? ",defaultLevel);
        return true;
    }

    /**
     * 文档是否签出
     */
    public Map<String, Object> isCheckOut(int docid, User user) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        apidatas.put("api_status", 1);

        String checkOutStatus = "";
        int checkOutUserId = 0;
        String checkOutUserType = "";

        RecordSet rs = new RecordSet();
        rs.executeQuery("select checkOutStatus,checkOutUserId,checkOutUserType,checkOutDate,checkOutTime from DocDetail where id=?", docid);
        if (rs.next()) {
            checkOutStatus = rs.getString("checkOutStatus");
            checkOutUserId = rs.getInt("checkOutUserId");
            checkOutUserType = rs.getString("checkOutUserType");
        }

        if (checkOutStatus != null && (checkOutStatus.equals("1") || checkOutStatus.equals("2")) && !(checkOutUserId == user.getUID() && checkOutUserType != null && checkOutUserType.equals(user.getLogintype()))) {

            String checkOutUserName = "";
            if (checkOutUserType != null && checkOutUserType.equals("2")) {
                try {
                    CustomerInfoComInfo cici = new CustomerInfoComInfo();
                    checkOutUserName = cici.getCustomerInfoname("" + checkOutUserId);
                } catch (Exception e) {
                }
            } else {
                try {
                    ResourceComInfo rci = new ResourceComInfo();
                    checkOutUserName = rci.getResourcename("" + checkOutUserId);
                } catch (Exception e) {
                }
            }
            //
            String checkOutMessage = SystemEnv.getHtmlLabelName(19695, user.getLanguage());
            checkOutMessage+= SystemEnv.getHtmlLabelName(19690, user.getLanguage()) + "：" + checkOutUserName;

            apidatas.put("api_status", 0);
            apidatas.put("msg", checkOutMessage);
        }

        return apidatas;
    }


    public void checkOut(int docid, String docsubject, int doccreaterid, String doccreatertype, String ipAddress, User user) {
        String checkOutDate = TimeUtil.getCurrentDateString();
        String checkOutTime = TimeUtil.getOnlyCurrentTimeString();
        RecordSet rs = new RecordSet();
        rs.executeUpdate("update  DocDetail set checkOutStatus=?,checkOutUserId=?,checkOutUserType=?,checkOutDate=?,checkOutTime=? where id=?",
                "1", user.getUID(), user.getLogintype(), checkOutDate, checkOutTime, docid);

        if (docsubject == null || docsubject.isEmpty() || doccreaterid <= 0 || doccreatertype == null || doccreatertype.isEmpty()) {
            rs.executeQuery("select docSubject,doccreaterid,doccreatertype from DocDetail where id=?", docid);
            if (rs.next()) {
                docsubject = rs.getString("docSubject");
                doccreaterid = rs.getInt("doccreaterid");
                doccreatertype = rs.getString("doccreatertype");
            } else {
                return;
            }
        }

        try {
            DocDetailLog ddl = new DocDetailLog();
            ddl.resetParameter();
            ddl.setDocId(docid);
            ddl.setDocSubject(docsubject);
            ddl.setOperateType("18");
            ddl.setOperateUserid(user.getUID());
            ddl.setUsertype(user.getLogintype());
            ddl.setClientAddress(ipAddress);
            ddl.setDocCreater(doccreaterid);
            ddl.setCreatertype(doccreatertype);
            ddl.setDocLogInfo();
        } catch (Exception e) {
        }
    }

    public Map<String, Object> checkIn(int docid, String ipAddress, User user) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        apidatas.put("api_status", 1);
        RecordSet rs = new RecordSet();
        rs.executeQuery("select docsubject,doccreaterid,doccreatertype from DocDetail where id=?", docid);
        String docsubject = "";
        int doccreaterid = 0;
        String doccreatertype = "";
        if (rs.next()) {
            docsubject = rs.getString("docsubject");
            doccreaterid = rs.getInt("doccreaterid");
            doccreatertype = rs.getString("doccreatertype");
        } else {
            apidatas.put("api_status", 0);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(23230, user.getLanguage()));
            return apidatas;
        }
        rs.executeUpdate("update  DocDetail set checkOutStatus=?,checkOutUserId=?,checkOutUserType=?,checkOutDate=?,checkOutTime=? where id=?",
                "0", 0, "", "", "", docid);

        try {
            DocDetailLog ddl = new DocDetailLog();
            ddl.resetParameter();
            ddl.setDocId(docid);
            ddl.setDocSubject(docsubject);
            ddl.setOperateType("20");
            ddl.setOperateUserid(user.getUID());
            ddl.setUsertype(user.getLogintype());
            ddl.setClientAddress(ipAddress);
            ddl.setDocCreater(doccreaterid);
            ddl.setCreatertype(doccreatertype);
            ddl.setDocLogInfo();
        } catch (Exception e) {
        }
        return apidatas;
    }

    /**
     * 根据正文是否存为流程草稿，或者文档新建的初始状态
     * @param requestid
     * @return
     */
    public String getDocStatusByRequestid(int requestid){
        String docStatus = "";
        String isWorkFlowDraft = "";
        RecordSet rs = new RecordSet();
        rs.executeQuery("select isWorkFlowDraft from workflow_createdoc where workflowid=(select workflowid from workflow_requestbase where requestid=?)",requestid);
        if(rs.next()){
            isWorkFlowDraft = Util.null2String(rs.getString("isWorkFlowDraft"));
            if(isWorkFlowDraft.equals("1")){
                docStatus = "9";
            }else{
                docStatus = "1";
            }
        }
        return docStatus;
    }

}
