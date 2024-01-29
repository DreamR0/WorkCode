package com.api.hrm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2607931.util.SaveUtil;
import com.api.hrm.bean.FieldItem;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.ConditionType;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateAuditType;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.util.HrmWeakPasswordUtil;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import ln.LN;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.filter.XssUtil;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.PasswordUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.definedfield.HrmFieldManager;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.passwordprotection.manager.HrmResourceManager;
import weaver.hrm.privacy.PrivacyBaseComInfo;
import weaver.hrm.privacy.PrivacyComInfo;
import weaver.hrm.privacy.UserPrivacyComInfo;
import weaver.hrm.resource.HrmListValidate;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
//import weaver.interfaces.email.CoreMailAPI;
import weaver.interfaces.hrm.HrmServiceManager;
import org.apache.commons.lang3.StringUtils;
import weaver.license.PluginUserCheck;
import weaver.rsa.security.RSA;
import weaver.rtx.OrganisationCom;
import weaver.rtx.OrganisationComRunnable;
import weaver.systeminfo.SysMaintenanceLog;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

/***
 * 新建人员
 * @author lvyi
 *
 */
public class HrmResourceAddService extends BaseBean {

    public boolean hasRightSystemInfo(HttpServletRequest request, HttpServletResponse response) {
        User user = HrmUserVarify.getUser(request, response);
        boolean isRight = false;
        try {
            HrmListValidate HrmListValidate = new HrmListValidate();
            isRight = HrmListValidate.isValidate(15) && HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user);
        } catch (Exception e) {
            writeLog(e);
        }
        return isRight;
    }

    /**
     * 新建人员表单Simple
     *
     * @param request
     * @param response
     * @return
     */
    public String getHrmResourceAddSimpleForm(HttpServletRequest request, HttpServletResponse response) {
        User user = HrmUserVarify.getUser(request, response);

        //姓名、性别、部门、岗位、直接上级、状态、照片、办公地点、移动电话、办公室电话、电子邮件+必填的自定义字段
        String fromfields = ",workcode,lastname,sex,accounttype,belongto,departmentid,jobtitle,managerid,status,resourceimageid,locationid,mobile," +
                "telephone,email,";
        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String passwordComplexity = settings.getPasswordComplexity();
        int minpasslen = settings.getMinPasslen();
        boolean flagaccount = GCONST.getMOREACCOUNTLANDING();
        List<Object> lsGroup = new ArrayList<Object>();
        Map<String, Object> groupitem = null;
        List<Object> itemlist = null;
        try {
            PrivacyComInfo pc = new PrivacyComInfo();
            PrivacyBaseComInfo privacyBaseComInfo = new PrivacyBaseComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            Map<String, String> mapShowTypes = pc.getMapShowTypes();
            //总体的大开关的默认设置
            Map<String, String> mapBaseShowTypeDefaults = privacyBaseComInfo.getMapShowTypeDefaults();

            //基本信息
            int scopeId = -1;
            HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);

            while (HrmFieldGroupComInfo.next()) {
                int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
                if (grouptype != scopeId) continue;
                int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
                int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
                hfm.getCustomFields(groupid);
                if (hfm.getGroupCount() == 0 || groupid == 3) continue;

                groupitem = new HashMap<String, Object>();
                itemlist = new ArrayList<Object>();
                groupitem.put("title", SystemEnv.getHtmlLabelName(grouplabel, user.getLanguage()));
                groupitem.put("defaultshow", true);
                groupitem.put("items", itemlist);
                lsGroup.add(groupitem);
                while (hfm.next()) {
                    int tmpviewattr = 2;
                    String fieldName = hfm.getFieldname();
                    String cusFieldname = "";
                    if (hfm.isBaseField(fieldName)) {
                        if (fromfields.indexOf("," + fieldName + ",") == -1) continue;
                    } else {
                        cusFieldname = "customfield_0_" + hfm.getFieldid();
                        if (!hfm.isMand()) continue;
                    }
                    if (!hfm.isUse()) {
                        continue;
                    }

                    if (fieldName.equals("loginid") || fieldName.equals("jobactivity") || fieldName.equals("departmentvirtualids"))
                        continue;
                    if (fieldName.equals("accounttype") && !flagaccount) continue;
                    org.json.JSONObject hrmFieldConf = hfm.getHrmFieldConf(fieldName);
                    HrmFieldBean hrmFieldBean = new HrmFieldBean();
                    hrmFieldBean.setFieldid((String) hrmFieldConf.get("id"));
                    hrmFieldBean.setFieldname(cusFieldname.length() > 0 ? cusFieldname : fieldName);
                    hrmFieldBean.setFieldlabel(hfm.getLable());
                    hrmFieldBean.setFieldhtmltype((String) hrmFieldConf.get("fieldhtmltype"));
                    hrmFieldBean.setType((String) hrmFieldConf.get("type"));
                    hrmFieldBean.setDmlurl((String) hrmFieldConf.get("dmlurl"));
                    hrmFieldBean.setIssystem("" + (Integer) hrmFieldConf.get("issystem"));
                    hrmFieldBean.setIsFormField(true);
                    if (hrmFieldBean.getFieldname().equals("lastname")) {
                        hrmFieldBean.setMultilang(true);
                    } else {
                        hrmFieldBean.setMultilang(false);
                    }
                    if (((String) hrmFieldConf.get("ismand")).equals("1")) {
                        tmpviewattr = 3;
                    }

                    if (fieldName.equals("belongto") && flagaccount) {
                        tmpviewattr = 3;
                    }

                    if (fieldName.equals("mobile") || fieldName.equals("email")) {
                        hrmFieldBean.setMultilang(false);
                    }

                    if (fieldName.equals("departmentid")) {
                        hrmFieldBean.setFieldvalue(Util.null2String(request.getParameter("departmentid")));
                    }

                    SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                    if (searchConditionItem == null) continue;
                    if(fieldName.equals("sex")){
                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
                        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(28473, user.getLanguage()), true));
                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(28474, user.getLanguage())));
                        searchConditionItem.setOptions(options);
                    }
                    if(fieldName.equals("accounttype")){
                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
                        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(17746, user.getLanguage()), true));
                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(17747, user.getLanguage())));
                        searchConditionItem.setOptions(options);
                    }
                    if (fieldName.equals("belongto") && flagaccount) {
                        XssUtil xssUtil = new XssUtil();
                        String accountSql = "(accounttype=0 or accounttype=null or accounttype is null)";
                        searchConditionItem.getBrowserConditionParam().getDataParams().put("sqlwhere", xssUtil.put(accountSql));
                        searchConditionItem.getBrowserConditionParam().getCompleteParams().put("sqlwhere", xssUtil.put(accountSql));
                        searchConditionItem.getBrowserConditionParam().getDestDataParams().put("sqlwhere", xssUtil.put(accountSql));
                    }
                    if (fieldName.equals("status")) {
                        List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
                        statusOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, user.getLanguage()), true));
                        statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
                        statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
                        searchConditionItem.setOptions(statusOptions);
                    }
                    if (searchConditionItem.getBrowserConditionParam() != null) {
                        searchConditionItem.getBrowserConditionParam().setViewAttr(tmpviewattr);
                        if (hrmFieldBean.getFieldname().equals("departmentid")) {
                            searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "HrmResourceAdd:Add");
                            searchConditionItem.getBrowserConditionParam().getCompleteParams().put("rightStr", "HrmResourceAdd:Add");
                        }
                    }
                    if (!passwordComplexity.equals("0") && !searchConditionItem.getConditionType().equals(ConditionType.DATE)) {
                        searchConditionItem.setLabelcol(6);
                        searchConditionItem.setFieldcol(12);
                    }
                    searchConditionItem.setViewAttr(tmpviewattr);
                    itemlist.add(searchConditionItem);

                    if (fieldName.equals("jobtitle")) {//岗位后加入上下级关系
                        hrmFieldBean = new HrmFieldBean();
                        hrmFieldBean.setFieldname("managerid");
                        hrmFieldBean.setFieldlabel("15709");
                        hrmFieldBean.setFieldhtmltype("3");
                        hrmFieldBean.setType("1");
                        hrmFieldBean.setIssystem("1");
                        hrmFieldBean.setIsFormField(true);
                        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                        if (searchConditionItem == null) continue;
                        if (searchConditionItem.getBrowserConditionParam() != null) {
                            searchConditionItem.getBrowserConditionParam().setViewAttr(2);
                        }
                        if (!passwordComplexity.equals("0") && !searchConditionItem.getConditionType().equals(ConditionType.DATE)) {
                            searchConditionItem.setLabelcol(6);
                            searchConditionItem.setFieldcol(12);
                        }
                        searchConditionItem.setViewAttr(2);
                        itemlist.add(searchConditionItem);
                    }
//					if(fieldName.equals("mobile")){
//					  String mobileShowSet = Util.null2String(settings.getMobileShowSet());
//					  String mobileShowType = Util.null2String(settings.getMobileShowType());
//					  String mobileShowTypeDefault = Util.null2String(settings.getMobileShowTypeDefault());
//					  if(mobileShowSet.equals("1")){
//							hrmFieldBean = new HrmFieldBean();
//							hrmFieldBean.setFieldname("mobileshowtype");
//							hrmFieldBean.setFieldlabel("32684");
//							hrmFieldBean.setFieldhtmltype("5");
//							hrmFieldBean.setType("1");
//							hrmFieldBean.setIssystem("1");
//							hrmFieldBean.setIsFormField(true);
//							searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
//							if(searchConditionItem==null)continue;
//							if(!passwordComplexity.equals("0")&&!searchConditionItem.getConditionType().equals(ConditionType.DATE)){
//						  	searchConditionItem.setLabelcol(6);
//						  	searchConditionItem.setFieldcol(12);
//							}
//							searchConditionItem.setViewAttr(2);
//				  		List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
//				  		if(mobileShowType.indexOf("1")!=-1){
//				  			statusOptions.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(2161,user.getLanguage()),mobileShowTypeDefault.equals("1")));
//						 	}if(mobileShowType.indexOf("2")!=-1){
//						 		statusOptions.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(32670,user.getLanguage()),mobileShowTypeDefault.equals("2")));
//						 	}if(mobileShowType.indexOf("3")!=-1){
//						 		statusOptions.add(new SearchConditionOption("3",SystemEnv.getHtmlLabelName(32671,user.getLanguage()),mobileShowTypeDefault.equals("3")));
//						 	}
//							searchConditionItem.setOptions(statusOptions);
//							itemlist.add(searchConditionItem);
//					  }
//					}

                    if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                        String mobileShowSet = Util.null2String(mapShowSets.get(fieldName));
                        String mobileShowType = Util.null2String(mapShowTypes.get(fieldName));
                        String baseMobileShowTypeDefaults = Util.null2String(mapBaseShowTypeDefaults.get(fieldName));

                        if (mobileShowSet.equals("1")) {
                            hrmFieldBean = new HrmFieldBean();
                            hrmFieldBean.setFieldname(fieldName + "showtype");
                            hrmFieldBean.setFieldlabelname(SystemEnv.getHtmlLabelNames(hfm.getLable(), user.getLanguage()) + SystemEnv.getHtmlLabelName(385571, user.getLanguage()));
                            hrmFieldBean.setFieldhtmltype("5");
                            hrmFieldBean.setType("1");
                            hrmFieldBean.setIssystem("1");
                            hrmFieldBean.setIsFormField(true);
                            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                            if (searchConditionItem == null) continue;
                            List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
                            if (mobileShowType.indexOf("1") != -1) {
                                if (baseMobileShowTypeDefaults.indexOf("1") != -1) {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage())));
                                }
                            }
                            if (mobileShowType.indexOf("2") != -1) {
                                if (baseMobileShowTypeDefaults.indexOf("2") != -1) {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage())));
                                }
                            }
                            if (mobileShowType.indexOf("3") != -1) {
                                if (baseMobileShowTypeDefaults.indexOf("3") != -1) {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage())));
                                }
                            }
                            searchConditionItem.setOptions(statusOptions);
                            itemlist.add(searchConditionItem);
                        }
                    }
                }
                if (itemlist.size() == 0) lsGroup.remove(groupitem);
            }

            RecordSet rs = new RecordSet();
            String sql = "select * from hrm_formfield where fieldname=? ";

            String[] fields = new String[]{"companystartdate,1516,3,2","workstartdate,23519,3,2"};
            SearchConditionItem searchConditionItem = null;
            HrmFieldBean hrmFieldBean = null;
            itemlist = new ArrayList<Object>();
            groupitem = new HashMap<String, Object>();
            groupitem.put("title", SystemEnv.getHtmlLabelName(15688, user.getLanguage()));
            groupitem.put("defaultshow", true);
            for (int i = 0; i < fields.length; i++) {
                String[] fieldinfo = fields[i].split(",");
                hrmFieldBean = new HrmFieldBean();
                hrmFieldBean.setFieldname(fieldinfo[0]);
                hrmFieldBean.setFieldlabel(fieldinfo[1]);
                hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
                hrmFieldBean.setType(fieldinfo[3]);
                hrmFieldBean.setIsFormField(true);
                hrmFieldBean.setMultilang(false);
                boolean isUse = false;
                boolean isMand = false;
                rs.executeQuery(sql,hrmFieldBean.getFieldname());
                if(rs.next()){
                    if(rs.getInt("isUse")==1){
                        isUse = true;
                    }
                    if(rs.getInt("isMand")==1){
                        isMand = true;
                    }
                }
                if(!isUse)continue;
                searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                searchConditionItem.setViewAttr(isMand?3:2);
                itemlist.add(searchConditionItem);
            }
            if(!itemlist.isEmpty()){
                lsGroup.add(groupitem);
                groupitem.put("items", itemlist);
            }


            //登录名、密码、密码确认、安全级别
            if (hasRightSystemInfo(request, response)) {
                String[] fields2 = new String[]{"loginid,412,1,1", "password,409,1,4", "password1,501,1,4", "seclevel,683,1,2","validatecode,22910,1,1"};
                SearchConditionItem searchConditionItem2 = null;
                HrmFieldBean hrmFieldBean2 = null;
                itemlist = new ArrayList<Object>();
                groupitem = new HashMap<String, Object>();
                groupitem.put("title", SystemEnv.getHtmlLabelName(15804, user.getLanguage()));
                groupitem.put("defaultshow", true);
                lsGroup.add(groupitem);
                for (int i = 0; i < fields2.length; i++) {
                    String[] fieldinfo = fields2[i].split(",");
                    hrmFieldBean2 = new HrmFieldBean();
                    hrmFieldBean2.setFieldname(fieldinfo[0]);
                    hrmFieldBean2.setFieldlabel(fieldinfo[1]);
                    hrmFieldBean2.setFieldhtmltype(fieldinfo[2]);
                    hrmFieldBean2.setType(fieldinfo[3]);
                    hrmFieldBean2.setIsFormField(true);
                    hrmFieldBean2.setMultilang(false);
                    if(hrmFieldBean2.getFieldname().equals("validatecode")){
                        hrmFieldBean2.setViewAttr(3);
                        hrmFieldBean2.setRules("required|string");
                    }
                    searchConditionItem2 = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean2, user);
                    if ("seclevel".equalsIgnoreCase(hrmFieldBean2.getFieldname())) {
                        searchConditionItem2.setMin("-999");
                        searchConditionItem2.setMax("999");
                    }
                    if (fieldinfo[3].equals("4")) {
                        Map<String, Object> otherParams = new HashMap<String, Object>();
                        otherParams.put("type", "password");
                        if (fieldinfo[0].equals("password")) {
                            String title = "";
                            if (passwordComplexity.equals("1")) {
                                title = SystemEnv.getHtmlLabelName(24080, user.getLanguage());
                            } else if (passwordComplexity.equals("2")) {
                                title = SystemEnv.getHtmlLabelName(24081, user.getLanguage());
                            } else if (passwordComplexity.equals("3")) {
                                title = SystemEnv.getHtmlLabelName(512563, user.getLanguage());
                            }
                            otherParams.put("tip", title);
                            otherParams.put("tipLength", "100");
                            if (!passwordComplexity.equals("0")) {
                                otherParams.put("passwordStrength", true);
                                otherParams.put("passwordStrengthIdx", 1);
                            }
                        }
                        searchConditionItem2.setOtherParams(otherParams);
                    }
                    if (!passwordComplexity.equals("0") && !searchConditionItem2.getConditionType().equals(ConditionType.DATE)) {
                        searchConditionItem2.setLabelcol(6);
                        searchConditionItem2.setFieldcol(12);
                    }
                    itemlist.add(searchConditionItem2);
                }
                groupitem.put("items", itemlist);
            }

        } catch (Exception e) {
            writeLog(e);
        }
        Map<String, Object> retmap = new HashMap<String, Object>();
        retmap.put("condition", lsGroup);
        retmap.put("passwordComplexity", passwordComplexity);
        retmap.put("minpasslen", minpasslen);
        //是否开启了RSA加密
        String openRSA = Util.null2String(Prop.getPropValue("openRSA","isrsaopen"));
        retmap.put("openRSA",openRSA);
        retmap.put("hasJobTitlesAdd", HrmUserVarify.checkUserRight("HrmJobTitlesAdd:Add", user));
        return JSONObject.toJSONString(retmap);
    }

    /**
     * 新建人员表单
     *
     * @param request
     * @param response
     * @return
     */
    public String getHrmResourceAddForm(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();
            String passwordComplexity = settings.getPasswordComplexity();
            int minpasslen = settings.getMinPasslen();
            int needdynapass = settings.getNeeddynapass();
            String needusbHt = settings.getNeedusbHt();
            String needusbDt = settings.getNeedusbDt();
            String needCL = settings.getNeedCL();
            HrmResourceBaseService HrmResourceBaseService = new HrmResourceBaseService();
            HrmResourcePersonalService HrmResourcePersonalService = new HrmResourcePersonalService();
            HrmResourceWorkService HrmResourceWorkService = new HrmResourceWorkService();

            HrmListValidate HrmListValidate = new HrmListValidate();
            retmap.put("basecondition", HrmResourceBaseService.getFormFields(request, response, true));
            result = HrmResourcePersonalService.getFormFields(request, response, true);
            if (HrmListValidate.isValidate(11)) {
                retmap.put("personalcondition", result != null ? result.get("conditions") : null);
				if(result!=null){
                    List<Map<String,Object>> tables = (List<Map<String,Object>>) result.get("tables") ;
                    for(Map<String,Object> tableInfo : tables){
                        Map<String,Object> table   = (Map<String,Object>)tableInfo.get("tabinfo") ;
                        if(table!=null) {
                            List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
                            if (columns != null) {
                                for (Map<String, Object> c : columns) {
                                    String key = (String) c.get("key");
                                    if ("company".equals(key) || "jobtitle".equals(key)) {
                                        c.put("key", "person_" + key);
                                        c.put("dataIndex", "person_" + key);
                                        List<FieldItem> coms = (List<FieldItem>) c.get("com") ;
                                        if(coms != null){
                                            for(FieldItem com : coms){
                                                com.setKey("person_"+key);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
				
                retmap.put("personaltables", result != null ? result.get("tables") : null);
            }
            if (HrmListValidate.isValidate(12)) {
                result = HrmResourceWorkService.getFormFields(request, response, true);
                retmap.put("workcondition", result != null ? result.get("conditions") : null);
                retmap.put("worktables", result != null ? result.get("tables") : null);
            }

            //系统信息
            if (hasRightSystemInfo(request, response)) {
                List<Object> lsGroup = new ArrayList<Object>();
                Map<String, Object> groupitem = null;
                List<Object> itemlist = null;
                //登录名、密码、密码确认、辅助校验方式、系统语言、电子邮件、安全级别
                String[] fields = new String[]{"loginid,412,1,1", "password,409,1,4",
                        "password1,501,1,4", "userUsbType,81629,5,1", "usbstate,602,5,1",
                        "tokenKey,32897,1,1", "serial,21597,1,1", "seclevel,683,1,2","validatecode,22910,1,1"};//,"systemlanguage,16066,3,259","email,477,1,1"

                HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
                SearchConditionItem searchConditionItem = null;
                HrmFieldBean hrmFieldBean = null;
                itemlist = new ArrayList<Object>();
                groupitem = new HashMap<String, Object>();
                groupitem.put("title", SystemEnv.getHtmlLabelName(15804, user.getLanguage()));
                groupitem.put("defaultshow", true);
                for (int i = 0; i < fields.length; i++) {
                    String[] fieldinfo = fields[i].split(",");
                    hrmFieldBean = new HrmFieldBean();
                    hrmFieldBean.setFieldname(fieldinfo[0]);
                    hrmFieldBean.setFieldlabel(fieldinfo[1]);
                    hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
                    hrmFieldBean.setType(fieldinfo[3]);
                    hrmFieldBean.setIsFormField(true);
                    hrmFieldBean.setMultilang(false);
                    if(hrmFieldBean.getFieldname().equals("validatecode")){
                        hrmFieldBean.setViewAttr(3);
                        hrmFieldBean.setRules("required|string");
                    }
                    if (hrmFieldBean.getFieldname().equals("userUsbType")) {
                        if (needdynapass == 1 || "1".equals(needusbHt) || "1".equals(needusbDt)|| "1".equals(needCL)) {
                        } else {
                            continue;
                        }
                    }
                    searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                    if ("seclevel".equalsIgnoreCase(hrmFieldBean.getFieldname())) {
                        searchConditionItem.setMin("-999");
                        searchConditionItem.setMax("999");
                    }
                    if (fieldinfo[3].equals("4")) {
                        Map<String, Object> otherParams = new HashMap<String, Object>();
                        otherParams.put("type", "password");
                        if (fieldinfo[0].equals("password")) {
                            String title = "";
                            if (passwordComplexity.equals("1")) {
                                title = SystemEnv.getHtmlLabelName(24080, user.getLanguage());
                            } else if (passwordComplexity.equals("2")) {
                                title = SystemEnv.getHtmlLabelName(24081, user.getLanguage());
                            } else if (passwordComplexity.equals("3")) {
                                title = SystemEnv.getHtmlLabelName(512563, user.getLanguage());
                            }
                            otherParams.put("tip", title);
                            otherParams.put("tipLength", "100");
                            if (!passwordComplexity.equals("0")) {
                                otherParams.put("passwordStrengthIdx", 2);
                                otherParams.put("passwordStrength", true);
                            }
                        }
                        searchConditionItem.setOtherParams(otherParams);
                    } else if (hrmFieldBean.getFieldhtmltype().equals("5")) {
                        List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
                        if (hrmFieldBean.getFieldname().equals("userUsbType")) {
                            statusOptions.add(new SearchConditionOption("", "", true));
                            if (needdynapass == 1) {
                                statusOptions.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(32511, user.getLanguage())));
                            }
                            if ("1".equals(needusbHt)) {
                                statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21589, user.getLanguage())));
                            }
                            if ("1".equals(needusbDt)) {
                                statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32896, user.getLanguage())));
                            }
                            if ("1".equals(needCL)) {
                                statusOptions.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(388414, user.getLanguage())));
                            }
                            searchConditionItem.setOptions(statusOptions);
                        } else if (hrmFieldBean.getFieldname().equals("usbstate")) {
                            statusOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), true));
                            statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18096, user.getLanguage())));
                            statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21384, user.getLanguage())));
                            searchConditionItem.setOptions(statusOptions);
                        }
                    }
                    if (!passwordComplexity.equals("0") && !searchConditionItem.getConditionType().equals(ConditionType.DATE)) {
                        searchConditionItem.setLabelcol(6);
                        searchConditionItem.setFieldcol(12);
                    }
                    itemlist.add(searchConditionItem);

                }
                groupitem.put("items", itemlist);
                lsGroup.add(groupitem);
                retmap.put("systeminfocondition", lsGroup);
                retmap.put("passwordComplexity", passwordComplexity);
                retmap.put("minpasslen", minpasslen);
            }
            retmap.put("hrmId", user.getUID());
            retmap.put("hasJobTitlesAdd", HrmUserVarify.checkUserRight("HrmJobTitlesAdd:Add", user));
            //是否开启了RSA加密
            String openRSA = Util.null2String(Prop.getPropValue("openRSA","isrsaopen"));
            retmap.put("openRSA",openRSA);
        } catch (Exception e) {
            retmap.put("api_status", false);
            retmap.put("api_errormsg", e.getMessage());
            e.printStackTrace();
        }
        return JSONObject.toJSONString(retmap);
    }

    /**
     * 保存新建人员simple
     *
     * @param request
     * @param response
     * @return
     */
    public String saveSimple(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        RecordSet rs = new RecordSet();
        String sql = "";
        try {
            User user = HrmUserVarify.getUser(request, response);
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            boolean canEdit = HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user);
            if (!canEdit) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(22620, user.getLanguage()));
                return JSONObject.toJSONString(retmap);
            }

            String loginid = Util.null2String(request.getParameter("loginid"));
            String accounttype = Util.null2s(Util.fromScreen3(request.getParameter("accounttype"), user.getLanguage()), "0");
            if(accounttype.equals("1"))loginid="";//次账号没有loginid
            boolean canSave = false;
            LN LN = new LN();
            int ckHrmnum = LN.CkHrmnum();
            if(loginid.length()>0) {
                if (ckHrmnum < 0) {//只有License检查人数小于规定人数，才能修改。防止客户直接修改数据库数据
                    canSave = true;
                }
            }else{
                canSave = true;
            }
            if (!canSave) {
                retmap.put("status", "-1");
                retmap.put("message", 	SystemEnv.getHtmlLabelName(84760, user.getLanguage()));
                return JSONObject.toJSONString(retmap);
            }

           /*验证码是否正确 start*/
            String validatecode = Util.null2String(request.getParameter("validatecode"));
            String validateRand = Util.null2String((String) request.getSession(true).getAttribute("validateRand"));
            request.getSession(true).removeAttribute("validateRand");
            if (!validateRand.toLowerCase().equals(validatecode.trim().toLowerCase()) || (loginid.length()>0&&"".equals(validatecode.trim().toLowerCase()))) {
                retmap.put("message", SystemEnv.getHtmlLabelName(10000304, Util.getIntValue(user.getLanguage())));
                retmap.put("status", "-1");
                return JSONObject.toJSONString(retmap);
            }
            /*验证码是否正确 end*/

            if (!loginid.equals("") && "0".equals(accounttype)) {
                sql = "select count(1) from hrmresourceallview where loginid='" + loginid + "' ";
                rs.executeSql(sql);
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(15094, user.getLanguage()));
                        return JSONObject.toJSONString(retmap);
                    }
                }
            }

            String departmentid = Util.null2String(request.getParameter("departmentid"));
            String subcompanyid = departmentComInfo.getSubcompanyid1(departmentid);
            if (!loginid.equals("") && !subcompanyid.equals("0") && new HrmResourceManager().noMore(subcompanyid)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(81926, user.getLanguage()));
                return JSONObject.toJSONString(retmap);
            }

            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            HrmResourceBaseService HrmResourceBaseService = new HrmResourceBaseService();
            Map<String, String> result = HrmResourceBaseService.addResourceBase(request, response);

            String addResourceBaseCode = result.get("status") ;
            if("-1".equals(addResourceBaseCode)){
                retmap.put("status", "-1");
                retmap.put("message", result.get("message"));
                return JSONObject.toJSONString(retmap) ;
            }


            int id = Util.getIntValue(result.get("id"));
            String password = Util.null2String(request.getParameter("password"));
            //是否开启了RSA加密
            String openRSA = Util.null2String(Prop.getPropValue("openRSA","isrsaopen"));
            List<String> passwordList = new ArrayList<String>();
            if("1".equals(openRSA)){
                passwordList.add(password);

                RSA rsa = new RSA();
                List<String> resultList = rsa.decryptList(request,passwordList);
                password = resultList.get(0);
            }
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();

            //判断是否开启了【启用初始密码】
            String defaultPasswordEnable = Util.null2String(settings.getDefaultPasswordEnable());
            //【初始密码】
            String defaultPassword = Util.null2String(settings.getDefaultPassword());
            //如果管理员设置的密码为空。并且开启了【启用初始密码】，且初始密码不为空，则默认取初始密码作为密码
            if (password.equals("") && defaultPasswordEnable.equals("1") && !defaultPassword.equals("")) {
                password = defaultPassword;
            }

            //判断是否开启了【禁止弱密码保存】
            String weakPasswordDisable = Util.null2s(settings.getWeakPasswordDisable(), "0");
            if (weakPasswordDisable.equals("1")) {
                if (password.equals("")) {//密码为空的情况
                } else {
                    //判断是否为弱密码
                    HrmWeakPasswordUtil hrmWeakPasswordUtil = new HrmWeakPasswordUtil();
                    if (hrmWeakPasswordUtil.isWeakPsd(password)) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(515420, user.getLanguage()));
                        return JSONObject.toJSONString(retmap);
                    }
                }
            }

            int seclevel = Util.getIntValue(request.getParameter("seclevel"), 0);
            if (id > 0) {//保存系统信息
                SimpleBizLogger logger = new SimpleBizLogger();
                Map<String, Object> params = ParamUtil.request2Map(request);
                BizLogContext bizLogContext = new BizLogContext();
                bizLogContext.setLogType(BizLogType.HRM);//模块类型
                bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
                bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_SYSTEM);//当前小类型
                bizLogContext.setOperateType(BizLogOperateType.ADD);
                bizLogContext.setOperateAuditType(BizLogOperateAuditType.WARNING);
                bizLogContext.setParams(params);//当前request请求参数
                logger.setUser(user);//当前操作人
                String mainSql = "select * from hrmresource where id=" + id;
                logger.setMainSql(mainSql, "id");//主表sql
                logger.setMainPrimarykey("id");//主日志表唯一key
                logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
                logger.before(bizLogContext);//写入操作前日志

                String workstartdate = Util.null2String(request.getParameter("workstartdate"));//参加工作日期
                String companystartdate = Util.null2String(request.getParameter("companystartdate"));//入职日期
                String dsporder = Util.fromScreen3(request.getParameter("dsporder"), user.getLanguage());
                if (dsporder.length() == 0) dsporder = "" + id;
                if (accounttype.equals("0")) {
                    
					String encrptPassword = "" ;
                    String salt = "" ;
                    if(StringUtils.isNotBlank(password)){
                        String[] encrypts = PasswordUtil.encrypt(password);
                        encrptPassword = encrypts[0] ;
                        salt = encrypts[1] ;
                    }
					
                    sql = " update hrmresource set loginid='" + loginid + "', password='" + encrptPassword + "'," +
                            "seclevel=" + seclevel + ",dsporder=" + dsporder +
                            ",salt='" + salt + "',workstartdate='" + workstartdate + "',companystartdate='" + companystartdate + "' where id = " + id;
                } else {
                    sql = " update hrmresource set seclevel=" + seclevel + ",dsporder=" + dsporder +
                            " ,workstartdate='" + workstartdate + "',companystartdate='" + companystartdate + "' where id = " + id;
                }
                HrmResourceWorkService hrmResourceWorkService =new  HrmResourceWorkService();
                String  workinfoid =String.valueOf(id);
                hrmResourceWorkService.updateWorkInfo(request,workinfoid, user);

                rs.executeSql(sql);
				
				HrmFaceCheckManager.setUserPassowrd(id+"",password);
                HrmFaceCheckManager.sync(id+"",HrmFaceCheckManager.getOptUpdate(),"HrmResourceAddService_saveSimple_update",HrmFaceCheckManager.getOaResource());

                LogUtil.writeBizLog(logger.getBizLogContexts());
            }

            //同步RTX端的用户信息.
            new OrganisationCom().checkUser(id);
            new Thread(new OrganisationComRunnable("user", "add", "" + id)).start();
            ResourceComInfo.updateResourceInfoCache("" + id);
            new PluginUserCheck().clearPluginUserCache("messager");
            //OA与第三方接口单条数据同步方法开始
            new HrmServiceManager().SynInstantHrmResource("" + id, "1");
            //OA与第三方接口单条数据同步方法结束
            //BBS集成相关
            String bbsLingUrl = new BaseBean().getPropValue(GCONST.getConfigFile(), "ecologybbs.linkUrl");
            if (!password.equals("0")) {
                if (!bbsLingUrl.equals("")) {
                    new Thread(new weaver.bbs.BBSRunnable(loginid, password)).start();
                }
            }

            //修改人员实时同步到CoreMail邮件系统
            //CoreMailAPI coremailapi = CoreMailAPI.getInstance();
            //coremailapi.synUser(""+id);

            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog("保存新建人员simple错误：" + e);
            retmap.put("status", "-1");
            retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
        }
        return JSONObject.toJSONString(retmap);
    }

	
	 public static ThreadLocal<Boolean> saveStatusThreadLocal = new ThreadLocal<>() ;

    /**
     * 保存新建人员
     *
     * @param request
     * @param response
     * @return
     */
    public String save(HttpServletRequest request, HttpServletResponse response) {
		saveStatusThreadLocal.set(true);
        Map<String, Object> retmap = new HashMap<String, Object>();
        RecordSet rs = new RecordSet();
        RecordSet rs1 = new RecordSet();
        RecordSet temprs = new RecordSet();
        String sql = "";
        String sql1 = "";
        try {
            User user = HrmUserVarify.getUser(request, response);
            boolean canEdit = HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user);
            if (!canEdit) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(22620, user.getLanguage()));
                return JSONObject.toJSONString(retmap);
            }
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            HrmResourceBaseService HrmResourceBaseService = new HrmResourceBaseService();
            HrmResourcePersonalService HrmResourcePersonalService = new HrmResourcePersonalService();
            HrmResourceWorkService HrmResourceWorkService = new HrmResourceWorkService();

            String loginid = Util.null2String(request.getParameter("loginid"));
            String accounttype = Util.null2s(Util.fromScreen3(request.getParameter("accounttype"), user.getLanguage()), "0");
            if(accounttype.equals("1"))loginid="";//次账号没有loginid
            boolean canSave = false;
            LN LN = new LN();
            int ckHrmnum = LN.CkHrmnum();
			if(loginid.length()>0) {
                if (ckHrmnum < 0) {//只有License检查人数小于规定人数，才能修改。防止客户直接修改数据库数据
                    canSave = true;
                }
            }else{
                canSave = true;
            }

            if (!canSave) {
                retmap.put("status", "-1");
                retmap.put("message", 	SystemEnv.getHtmlLabelName(84760, user.getLanguage()));
                return JSONObject.toJSONString(retmap);
            }

            /*验证码是否正确 start*/
            String validatecode = Util.null2String(request.getParameter("validatecode"));
            String validateRand = Util.null2String((String) request.getSession(true).getAttribute("validateRand"));
            request.getSession(true).removeAttribute("validateRand");
            if(user.getUID()==1){
                //esb 同步需要，超级管理员暂不限制
            }else {
                if (!validateRand.toLowerCase().equals(validatecode.trim().toLowerCase()) || (loginid.length() > 0 && "".equals(validatecode.trim().toLowerCase()))) {
                    retmap.put("message", SystemEnv.getHtmlLabelName(10000304, Util.getIntValue(user.getLanguage())));
                    retmap.put("status", "-1");
                    return JSONObject.toJSONString(retmap);
                }
            }
            /*验证码是否正确 end*/

            if (!loginid.equals("") && ("0".equals(accounttype) || StringUtils.isBlank(accounttype))) {
                sql = "select count(1) from HrmResource where loginid='" + loginid + "' ";
                rs.executeSql(sql);
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(15094, user.getLanguage()));
                        return JSONObject.toJSONString(retmap);
                    }
                }
                //管理员表判断
                sql1 = "select count(1) from HrmResourceManager where loginid='" + loginid + "' ";
                rs1.executeSql(sql1);
                if (rs1.next()) {
                    if (rs1.getInt(1) > 0) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(15094, user.getLanguage()));
                        return JSONObject.toJSONString(retmap);
                    }
                }
            }

            String certificatenum = Util.null2String(request.getParameter("certificatenum"));/*证件号码*/
            certificatenum=certificatenum.trim();
            if(!certificatenum.equals("")){
              if(!accounttype.equals("1")) {
                temprs.executeSql("select id from HrmResource where certificatenum='"+certificatenum+"' and (accounttype != '1' or accounttype is null)");
                if(temprs.next()){
                  retmap.put("status", "-1");
                  retmap.put("message", ""+ SystemEnv.getHtmlLabelName(83521,weaver.general.ThreadVarLanguage.getLang())+"");
                  return JSONObject.toJSONString(retmap);
                }
              }
            }



            String departmentid = Util.null2String(request.getParameter("departmentid"));
            String subcompanyid = departmentComInfo.getSubcompanyid1(departmentid);
            if (!loginid.equals("") && !subcompanyid.equals("0") && new HrmResourceManager().noMore(subcompanyid)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(81926, user.getLanguage()));
                return JSONObject.toJSONString(retmap);
            }

            Map<String, String> result = HrmResourceBaseService.addResourceBase(request, response);

            String addResourceBaseCode = result.get("status") ;
            if("-1".equals(addResourceBaseCode)){
                retmap.put("status", "-1");
                retmap.put("message", result.get("message"));
                return JSONObject.toJSONString(retmap) ;
            }

            int id = Util.getIntValue(result.get("id"));
            if (Util.getIntValue(result.get("status")) > 0 && id > 0) {
                ResourceComInfo ResourceComInfo = new ResourceComInfo();
                HrmResourcePersonalService.addResourcePersonal("" + id, request, response);
				saveStatusThreadLocal.remove();
                HrmResourceWorkService.addResourceWork("" + id, request, response);

                SimpleBizLogger logger = new SimpleBizLogger();
                Map<String, Object> params = ParamUtil.request2Map(request);
                BizLogContext bizLogContext = new BizLogContext();
                bizLogContext.setLogType(BizLogType.HRM);//模块类型
                bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
                bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_SYSTEM);//当前小类型
                bizLogContext.setOperateType(BizLogOperateType.ADD);
                bizLogContext.setOperateAuditType(BizLogOperateAuditType.WARNING);
                bizLogContext.setParams(params);//当前request请求参数
                logger.setUser(user);//当前操作人
                String mainSql = "select * from hrmresource where id=" + id;
                logger.setMainSql(mainSql, "id");//主表sql
                logger.setMainPrimarykey("id");//主日志表唯一key
                logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
                logger.before(bizLogContext);//写入操作前日志

                //保存系统信息
                String password = Util.null2String(request.getParameter("password"));
                //判断是否开启了RSA加密
                String openRSA = Util.null2String(Prop.getPropValue("openRSA","isrsaopen"));
                List<String> passwordList = new ArrayList<String>();
                if("1".equals(openRSA)){
                    passwordList.add(password);

                    RSA rsa = new RSA();
                    List<String> resultList = rsa.decryptList(request,passwordList);
                    password = resultList.get(0);
                }

                ChgPasswdReminder reminder = new ChgPasswdReminder();
                RemindSettings settings = reminder.getRemindSettings();

                //判断是否开启了【启用初始密码】
                String defaultPasswordEnable = Util.null2String(settings.getDefaultPasswordEnable());
                //【初始密码】
                String defaultPassword = Util.null2String(settings.getDefaultPassword());
                //如果管理员设置的密码为空。并且开启了【启用初始密码】，且初始密码不为空，则默认取初始密码作为密码
                if (password.equals("") && defaultPasswordEnable.equals("1") && !defaultPassword.equals("")) {
                    password = defaultPassword;
                }

                String systemlanguage = Util.null2String(request.getParameter("systemlanguage"));
                if (systemlanguage.equals("") || systemlanguage.equals("0")) systemlanguage = "7";
                String email = Util.null2String(request.getParameter("email"));
                int seclevel = Util.getIntValue(request.getParameter("seclevel"), 0);
                String userUsbType = Util.null2String(request.getParameter("userUsbType"));//辅助校验
                String usbstate = Util.null2String(request.getParameter("usbstate"));//是否启用
                String tokenkey = Util.null2String(request.getParameter("tokenKey"));//动态令牌序列号
                String serial = Util.null2String(request.getParameter("serial"));//海泰key秘钥
                String needdynapass = "";
                String needusb = "";
                StringBuffer upSql = new StringBuffer();
                if (userUsbType.equals("4")) {
                    needdynapass = "1";
                    needusb = "0";
                } else if (userUsbType.equals("2") || userUsbType.equals("3")) {
                    needdynapass = "0";
                    needusb = "1";
                } else {
                    needdynapass = "0";
                    needusb = "0";
                }
                if (!"".equals(usbstate) && !"".equals(userUsbType)) {
                    upSql.append(" ,needusb = ").append(needusb)
                            .append(" ,usbstate = ").append(usbstate)
                            .append(" ,needdynapass = ").append(needdynapass)
                            .append(" ,userUsbType = ").append(userUsbType);
                    if (userUsbType.equals("2")) {
                        upSql.append(" ,serial='" + (serial.equals("0") ? "" : serial) + "' ");
                    } else if (userUsbType.equals("3")) {
                        upSql.append(" ,tokenkey='" + tokenkey + "' ");
                    }
                }

                String belongto = Util.fromScreen3(request.getParameter("belongto"), user.getLanguage());
                if (accounttype.equals("1") && loginid.equalsIgnoreCase("")) {
                    rs.executeSql("select loginid from HrmResource where id =" + belongto);
                    if (rs.next()) {
                        loginid = rs.getString(1);
                    }
                    if (!loginid.equals("")) {
                        loginid = loginid + (id + 1);
                    }
                }

                if (!accounttype.equals("1") && !loginid.equalsIgnoreCase("")) {
                    String encrptPassword = "" ;
                    String salt = "" ;
                    if(StringUtils.isNotBlank(password)){
                        String[] encrypts = PasswordUtil.encrypt(password);
                        encrptPassword = encrypts[0] ;
                        salt = encrypts[1] ;
                    }
                    upSql.append(" ,loginid='" + loginid + "' ");
                    upSql.append(" ,password='" + encrptPassword + "',salt='" + salt + "' ");
                }

                String dsporder = Util.fromScreen3(request.getParameter("dsporder"), user.getLanguage());
                if (dsporder.length() == 0) dsporder = "" + id;
                sql = " update hrmresource set systemlanguage=" + systemlanguage + ",email='" + email + "',seclevel=" + seclevel +
                        ",dsporder=" + dsporder +
                        upSql.toString() +
                        " where id = " + id;
                rs.executeSql(sql);
                LogUtil.writeBizLog(logger.getBizLogContexts());
                ResourceComInfo.updateResourceInfoCache("" + id);

                HrmFaceCheckManager.setUserPassowrd(id+"",password);
                HrmFaceCheckManager.sync(id+"",HrmFaceCheckManager.getOptUpdate(),"HrmResourceAddService_save_update",HrmFaceCheckManager.getOaResource());
            }
            //qczj 这里对新建人员进行排班初始化
            SaveUtil saveUtil = new SaveUtil();
            RecordSet saveRS = new RecordSet();
            String fromDate = "";
            String toDate = "";
            String serialid = "";
            String groupId = Util.null2String(request.getParameter(new BaseBean().getPropValue("qc2607931","kqgroupFieldid")));
            String saveSql = "select * from "+new BaseBean().getPropValue("qc2607931","shiftTableName")+" where kqz="+groupId+"";
            new BaseBean().writeLog("==zj==(初始化排班sql1)" + JSON.toJSONString(saveSql));
            saveRS.executeQuery(saveSql);
            if (saveRS.next()){
                fromDate = Util.null2String(saveRS.getString("kssj"));//开始时间
                toDate = Util.null2String(saveRS.getString("jssj"));//结束时间
                serialid = Util.null2String(saveRS.getString("bcid"));//班次id
            }
            //初始化考勤组
            new BaseBean().writeLog("==zj==(初始化排班数据1)" + id + " | " + fromDate + " | " + toDate + " | "+serialid + " | " + groupId);
            saveUtil.saveGroupMember(String.valueOf(id),groupId);
            //初始化排班
            saveUtil.saveShiftschedule(String.valueOf(id),fromDate,toDate,serialid,groupId);
            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog("保存新建人员simple错误：" + e);
            retmap.put("status", "-1");
            retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
        }
        return JSONObject.toJSONString(retmap);
    }
}
