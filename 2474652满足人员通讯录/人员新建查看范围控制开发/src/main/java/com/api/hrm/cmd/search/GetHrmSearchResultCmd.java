package com.api.hrm.cmd.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2474652.Util.HrmVarifyCustom;
import com.api.hrm.util.PageUidFactory;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.BizLogContext;
import com.engine.common.entity.EncryptShareSettingEntity;
import com.engine.common.enums.EncryptMould;
import com.engine.core.interceptor.CommandContext;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.general.BaseBean;
import weaver.general.PageIdConst;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.common.Tools;
import weaver.hrm.common.database.dialect.DbDialectFactory;
import weaver.hrm.common.database.dialect.DialectUtil;
import weaver.hrm.common.database.dialect.IDbDialectSql;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.companyvirtual.SubCompanyVirtualComInfo;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.definedfield.HrmFieldManager;
import weaver.hrm.privacy.PrivacyComInfo;
import weaver.hrm.resource.HrmListValidate;
import weaver.hrm.resource.ResourceBelongtoComInfo;
import weaver.hrm.tools.HrmValidate;
import weaver.rdeploy.portal.PortalUtil;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 获取查询结果
 */
public class GetHrmSearchResultCmd extends AbstractCommonCommand<Map<String, Object>> {

    private static String DATE_SELECT = "select";
    private static String DATE_FROM = "from";
    private static String DATE_TO = "to";

    private static HttpServletRequest request;

    public GetHrmSearchResultCmd(Map<String, Object> params, User user, HttpServletRequest request) {
        this.user = user;
        this.params = params;
        this.request = request;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            RecordSet RecordSet = new RecordSet();
            Set<Map.Entry<String, Object>> paramSet = params.entrySet();
            for(Map.Entry me : paramSet){
              String key = Util.null2s(Util.null2String(me.getKey()),"");
              String val = Util.null2s(Util.null2String(me.getValue()),"");
              if(val.length() > 0 && key.length() > 0){
                //安全处理sql拼接 %;
                params.put(key ,val.replace("%",""));
              }
            }
            //满足QC1071823导入/导出需求 只显示标准字段并剔除隐私设置字段(电话号码、邮箱等)
            boolean isExport = "1".equals(Util.null2String(params.get("isExport")));
            String groupId = Util.null2String(params.get("groupId"));//常用组ID（来自左侧常用组树）
            String superid = Util.null2String(params.get("superid"));
            String needRealPath = Util.null2String(params.get("needRealPath"));
            String keyfield = Util.null2String(params.get("keyfield"));
            String tabkey = Util.fromScreen2((String) params.get("tabkey"), user.getLanguage());
            String showAllLevel = Util.fromScreen2((String) params.get("showAllLevel"), user.getLanguage());//是否显示下级机构、(我的下属页签)是否显示下级下属
            String resourcename = Util.fromScreen2((String) params.get("resourcename"), user.getLanguage());
            if (resourcename.length() == 0) {
                resourcename = Util.fromScreen2((String) params.get("lastname"), user.getLanguage());
            }
            resourcename = resourcename.trim();
            String jobtitle = Util.fromScreen((String) params.get("jobtitle"), user.getLanguage());
            String activitydesc = Util.fromScreen2((String) params.get("activitydesc"), user.getLanguage());
            String jobgroup = Util.fromScreen((String) params.get("jobgroup"), user.getLanguage());
            String jobactivity = Util.fromScreen((String) params.get("jobactivity"), user.getLanguage());
            String costcenter = Util.fromScreen((String) params.get("costcenter"), user.getLanguage());
            String resourcetype = Util.fromScreen((String) params.get("resourcetype"), user.getLanguage());
            String status = Util.fromScreen((String) params.get("status"), user.getLanguage());
            String subcompanyid1 = Util.fromScreen((String) params.get("subcompanyid1"), user.getLanguage());
            if (subcompanyid1.length() == 0) {
                subcompanyid1 = Util.fromScreen((String) params.get("subcompany"), user.getLanguage());
            }
            String departmentid = Util.fromScreen((String) params.get("departmentid"), user.getLanguage());
            if (departmentid.length() == 0) {
                departmentid = Util.fromScreen((String) params.get("department"), user.getLanguage());
            }
            String location = Util.fromScreen((String) params.get("location"), user.getLanguage());
            String manager = Util.fromScreen((String) params.get("manager"), user.getLanguage());
            String assistant = Util.fromScreen((String) params.get("assistant"), user.getLanguage());
            String roles = Util.fromScreen((String) params.get("roles"), user.getLanguage());
            String seclevel = Util.fromScreen((String) params.get("seclevel"), user.getLanguage());
            String seclevelto = Util.fromScreen((String) params.get("seclevelto"), user.getLanguage());
            String joblevel = Util.fromScreen((String) params.get("joblevel"), user.getLanguage());
            String joblevelto = Util.fromScreen((String) params.get("joblevelto"), user.getLanguage());
            String workroom = Util.fromScreen2((String) params.get("workroom"), user.getLanguage());
            String telephone = Util.fromScreen((String) params.get("telephone"), user.getLanguage());
            String startdate = Util.fromScreen((String) params.get("startdate" + DATE_FROM), user.getLanguage());
            String startdateto = Util.fromScreen((String) params.get("startdate" + DATE_TO), user.getLanguage());
            String startdateselect = Util.fromScreen((String) params.get("startdate" + DATE_SELECT), user.getLanguage());
            if (!startdateselect.equals("") && !startdateselect.equals("0") && !startdateselect.equals("6")) {
                startdate = TimeUtil.getDateByOption(startdateselect, "0");
                startdateto = TimeUtil.getDateByOption(startdateselect, "1");
            }
            String enddate = Util.fromScreen((String) params.get("enddate" + DATE_FROM), user.getLanguage());
            String enddateto = Util.fromScreen((String) params.get("enddate" + DATE_TO), user.getLanguage());
            String enddateselect = Util.fromScreen((String) params.get("enddate" + DATE_SELECT), user.getLanguage());
            if (!enddateselect.equals("") && !enddateselect.equals("0") && !enddateselect.equals("6")) {
                enddate = TimeUtil.getDateByOption(enddateselect, "0");
                enddateto = TimeUtil.getDateByOption(enddateselect, "1");
            }
            //人员查询增加入职日期条件
            String companystartdate = Util.fromScreen((String) params.get("companystartdate" + DATE_FROM), user.getLanguage());
            String companystartdateto = Util.fromScreen((String) params.get("companystartdate" + DATE_TO), user.getLanguage());
            String companystartdateselect = Util.fromScreen((String) params.get("companystartdate" + DATE_SELECT), user.getLanguage());
            if (!companystartdateselect.equals("") && !companystartdateselect.equals("0") && !companystartdateselect.equals("6")) {
                companystartdate = TimeUtil.getDateByOption(companystartdateselect, "0");
                companystartdateto = TimeUtil.getDateByOption(companystartdateselect, "1");
            }
            String companyworkyear = Util.fromScreen((String) params.get("companyworkyear"), user.getLanguage());
            String companyworkyearto = Util.fromScreen((String) params.get("companyworkyearto"), user.getLanguage());

            //人员查询增加参加工作日期条件
            String workstartdate = Util.fromScreen((String) params.get("workstartdate" + DATE_FROM), user.getLanguage());
            String workstartdateto = Util.fromScreen((String) params.get("workstartdate" + DATE_TO), user.getLanguage());
            String workstartdateselect = Util.fromScreen((String) params.get("workstartdate" + DATE_SELECT), user.getLanguage());
            if (!workstartdateselect.equals("") && !workstartdateselect.equals("0") && !workstartdateselect.equals("6")) {
                workstartdate = TimeUtil.getDateByOption(workstartdateselect, "0");
                workstartdateto = TimeUtil.getDateByOption(workstartdateselect, "1");
            }
            String workyear = Util.fromScreen((String) params.get("workyear"), user.getLanguage());
            String workyearto = Util.fromScreen((String) params.get("workyearto"), user.getLanguage());

            String contractdate = Util.fromScreen((String) params.get("contractdate" + DATE_FROM), user.getLanguage());
            String contractdateto = Util.fromScreen((String) params.get("contractdate" + DATE_TO), user.getLanguage());
            String contractdateselect = Util.fromScreen((String) params.get("contractdate" + DATE_SELECT), user.getLanguage());
            if (!contractdateselect.equals("") && !contractdateselect.equals("0") && !contractdateselect.equals("6")) {
                contractdate = TimeUtil.getDateByOption(contractdateselect, "0");
                contractdateto = TimeUtil.getDateByOption(contractdateselect, "1");
            }
            String birthday = Util.fromScreen((String) params.get("birthday" + DATE_FROM), user.getLanguage());
            String birthdayto = Util.fromScreen((String) params.get("birthday" + DATE_TO), user.getLanguage());
            String birthdayselect = Util.fromScreen((String) params.get("birthday" + DATE_SELECT), user.getLanguage());
            if (!birthdayselect.equals("") && !birthdayselect.equals("0") && !birthdayselect.equals("6")) {
                birthday = TimeUtil.getDateByOption(birthdayselect, "0");
                birthdayto = TimeUtil.getDateByOption(birthdayselect, "1");
            }
            String age = Util.fromScreen((String) params.get("age"), user.getLanguage());
            String ageto = Util.fromScreen((String) params.get("ageto"), user.getLanguage());
            String sex = Util.fromScreen((String) params.get("sex"), user.getLanguage());
            int accounttype = Util.getIntValue((String) params.get("accounttype"), -1);
            String resourceidfrom = Util.fromScreen((String) params.get("resourceidfrom"), user.getLanguage());
            String resourceidto = Util.fromScreen((String) params.get("resourceidto"), user.getLanguage());
            String workcode = Util.fromScreen((String) params.get("workcode"), user.getLanguage());
            String jobcall = Util.fromScreen((String) params.get("jobcall"), user.getLanguage());
            String mobile = Util.fromScreen((String) params.get("mobile"), user.getLanguage());
            String mobilecall = Util.fromScreen((String) params.get("mobilecall"), user.getLanguage());
            String fax = Util.fromScreen((String) params.get("fax"), user.getLanguage());
            String email = Util.fromScreen((String) params.get("email"), user.getLanguage());
            String folk = Util.fromScreen2((String) params.get("folk"), user.getLanguage());
            String nativeplace = Util.fromScreen2((String) params.get("nativeplace"), user.getLanguage());
            String regresidentplace = Util.fromScreen2((String) params.get("regresidentplace"), user.getLanguage());
            String maritalstatus = Util.fromScreen((String) params.get("maritalstatus"), user.getLanguage());
            String certificatenum = Util.fromScreen((String) params.get("certificatenum"), user.getLanguage());
            String tempresidentnumber = Util.fromScreen((String) params.get("tempresidentnumber"), user.getLanguage());
            String residentplace = Util.fromScreen2((String) params.get("residentplace"), user.getLanguage());
            String homeaddress = Util.fromScreen2((String) params.get("homeaddress"), user.getLanguage());
            String healthinfo = Util.fromScreen((String) params.get("healthinfo"), user.getLanguage());
            String heightfrom = Util.fromScreen((String) params.get("height"), user.getLanguage());
            String heightto = Util.fromScreen((String) params.get("heightto"), user.getLanguage());
            String weightfrom = Util.fromScreen((String) params.get("weight"), user.getLanguage());
            String weightto = Util.fromScreen((String) params.get("weightto"), user.getLanguage());
            String educationlevel = Util.fromScreen((String) params.get("educationlevel"), user.getLanguage());
            String educationlevelto = Util.fromScreen((String) params.get("educationlevelto"), user.getLanguage());
            String degree = Util.fromScreen2((String) params.get("degree"), user.getLanguage());
            String usekind = Util.fromScreen((String) params.get("usekind"), user.getLanguage());
            String policy = Util.fromScreen2((String) params.get("policy"), user.getLanguage());
            String bememberdatefrom = Util.fromScreen((String) params.get("bememberdate" + DATE_FROM), user.getLanguage());
            String bememberdateto = Util.fromScreen((String) params.get("bememberdate" + DATE_TO), user.getLanguage());
            String bememberdateselect = Util.fromScreen((String) params.get("bememberdate" + DATE_SELECT), user.getLanguage());
            if (!bememberdateselect.equals("") && !bememberdateselect.equals("0") && !bememberdateselect.equals("6")) {
                bememberdatefrom = TimeUtil.getDateByOption(bememberdateselect, "0");
                bememberdateto = TimeUtil.getDateByOption(bememberdateselect, "1");
            }
            String bepartydatefrom = Util.fromScreen((String) params.get("bepartydate" + DATE_FROM), user.getLanguage());
            String bepartydateto = Util.fromScreen((String) params.get("bepartydate" + DATE_TO), user.getLanguage());
            String bepartydateselect = Util.fromScreen((String) params.get("bepartydate" + DATE_SELECT), user.getLanguage());
            if (!bepartydateselect.equals("") && !bepartydateselect.equals("0") && !bepartydateselect.equals("6")) {
                bepartydatefrom = TimeUtil.getDateByOption(bepartydateselect, "0");
                bepartydateto = TimeUtil.getDateByOption(bepartydateselect, "1");
            }
            String islabouunion = Util.fromScreen((String) params.get("islabouunion"), user.getLanguage());
            String bankid1 = Util.fromScreen((String) params.get("bankid1"), user.getLanguage());
            String accountid1 = Util.fromScreen((String) params.get("accountid1"), user.getLanguage());
            String accumfundaccount = Util.fromScreen((String) params.get("accumfundaccount"), user.getLanguage());
            String loginid = Util.fromScreen((String) params.get("loginid"), user.getLanguage());
            String systemlanguage = Util.fromScreen((String) params.get("systemlanguage"), user.getLanguage());
            String classification = Util.null2String(params.get("classification"));//人员密级
            String datefield1 = Util.toScreenToEdit((String) params.get("datefield1"), user.getLanguage());
            String datefield1to = Util.toScreenToEdit((String) params.get("datefieldto1"), user.getLanguage());
            String datefield1select = Util.fromScreen((String) params.get("datefield1select"), user.getLanguage());
            if (!datefield1select.equals("") && !datefield1select.equals("0") && !datefield1select.equals("6")) {
                datefield1 = TimeUtil.getDateByOption(datefield1select, "0");
                datefield1to = TimeUtil.getDateByOption(datefield1select, "1");
            }
            String datefield2 = Util.toScreenToEdit((String) params.get("datefield2"), user.getLanguage());
            String datefield2to = Util.toScreenToEdit((String) params.get("datefieldto2"), user.getLanguage());
            String datefield2select = Util.fromScreen((String) params.get("datefield2select"), user.getLanguage());
            if (!datefield2select.equals("") && !datefield2select.equals("0") && !datefield2select.equals("6")) {
                datefield2 = TimeUtil.getDateByOption(datefield2select, "0");
                datefield2to = TimeUtil.getDateByOption(datefield2select, "1");
            }
            String datefield3 = Util.toScreenToEdit((String) params.get("datefield3"), user.getLanguage());
            String datefield3to = Util.toScreenToEdit((String) params.get("datefieldto3"), user.getLanguage());
            String datefield3select = Util.fromScreen((String) params.get("datefield3select"), user.getLanguage());
            if (!datefield3select.equals("") && !datefield3select.equals("0") && !datefield3select.equals("6")) {
                datefield3 = TimeUtil.getDateByOption(datefield3select, "0");
                datefield3to = TimeUtil.getDateByOption(datefield3select, "1");
            }
            String datefield4 = Util.toScreenToEdit((String) params.get("datefield4"), user.getLanguage());
            String datefield4to = Util.toScreenToEdit((String) params.get("datefieldto4"), user.getLanguage());
            String datefield4select = Util.fromScreen((String) params.get("datefield4select"), user.getLanguage());
            if (!datefield4select.equals("") && !datefield4select.equals("0") && !datefield4select.equals("6")) {
                datefield4 = TimeUtil.getDateByOption(datefield4select, "0");
                datefield4to = TimeUtil.getDateByOption(datefield4select, "1");
            }
            String datefield5 = Util.toScreenToEdit((String) params.get("datefield5"), user.getLanguage());
            String datefield5to = Util.toScreenToEdit((String) params.get("datefieldto5"), user.getLanguage());
            String datefield5select = Util.fromScreen((String) params.get("datefield5select"), user.getLanguage());
            if (!datefield5select.equals("") && !datefield5select.equals("0") && !datefield5select.equals("6")) {
                datefield5 = TimeUtil.getDateByOption(datefield5select, "0");
                datefield5to = TimeUtil.getDateByOption(datefield5select, "1");
            }
            String numberfield1 = Util.toScreenToEdit((String) params.get("numberfield1"), user.getLanguage());
            String numberfield1to = Util.toScreenToEdit((String) params.get("numberfieldto1"), user.getLanguage());
            String numberfield2 = Util.toScreenToEdit((String) params.get("numberfield2"), user.getLanguage());
            String numberfield2to = Util.toScreenToEdit((String) params.get("numberfieldto2"), user.getLanguage());
            String numberfield3 = Util.toScreenToEdit((String) params.get("numberfield3"), user.getLanguage());
            String numberfield3to = Util.toScreenToEdit((String) params.get("numberfieldto3"), user.getLanguage());
            String numberfield4 = Util.toScreenToEdit((String) params.get("numberfield4"), user.getLanguage());
            String numberfield4to = Util.toScreenToEdit((String) params.get("numberfieldto4"), user.getLanguage());
            String numberfield5 = Util.toScreenToEdit((String) params.get("numberfield5"), user.getLanguage());
            String numberfield5to = Util.toScreenToEdit((String) params.get("numberfieldto5"), user.getLanguage());
            String tff01name = Util.toScreenToEdit((String) params.get("textfield1"), user.getLanguage());
            String tff02name = Util.toScreenToEdit((String) params.get("textfield2"), user.getLanguage());
            String tff03name = Util.toScreenToEdit((String) params.get("textfield3"), user.getLanguage());
            String tff04name = Util.toScreenToEdit((String) params.get("textfield4"), user.getLanguage());
            String tff05name = Util.toScreenToEdit((String) params.get("textfield5"), user.getLanguage());
            String bff01name = Util.toScreenToEdit((String) params.get("tinyintfield1"), user.getLanguage());
            String bff02name = Util.toScreenToEdit((String) params.get("tinyintfield2"), user.getLanguage());
            String bff03name = Util.toScreenToEdit((String) params.get("tinyintfield3"), user.getLanguage());
            String bff04name = Util.toScreenToEdit((String) params.get("tinyintfield4"), user.getLanguage());
            String bff05name = Util.toScreenToEdit((String) params.get("tinyintfield5"), user.getLanguage());

            String createdatefrom = Util.fromScreen((String) params.get("createdatefrom"), user.getLanguage());
            String createdateto = Util.fromScreen((String) params.get("createdateto"), user.getLanguage());
            String createdateselect = Util.fromScreen((String) params.get("createdateselect"), user.getLanguage());
            if (!createdateselect.equals("") && !createdateselect.equals("0") && !createdateselect.equals("6")) {
                createdatefrom = TimeUtil.getDateByOption(createdateselect, "0");
                createdateto = TimeUtil.getDateByOption(createdateselect, "1");
            }

            String virtualtype = Util.null2String(params.get("virtualtype"));
            if (virtualtype.length() == 0) {
                virtualtype = Util.null2String(params.get("virtualCompanyid"));
            }
            String belongto = Util.fromScreen((String) params.get("belongto"), user.getLanguage());
            String nodetype = Util.fromScreen((String) params.get("type"), user.getLanguage());
            if (nodetype.equals("0") && virtualtype.length() == 0) {//总部
                virtualtype = Util.fromScreen((String) params.get("id"), user.getLanguage());
            }

            if (nodetype.equals("1") && subcompanyid1.length() == 0) {//分部
                subcompanyid1 = Util.fromScreen((String) params.get("id"), user.getLanguage());
            }

            if (nodetype.equals("2") && departmentid.length() == 0) {//部门
                departmentid = Util.fromScreen((String) params.get("id"), user.getLanguage());
            }

            int scopeId = 0;
            Map<String, String> customFieldMapBase = new HashMap<String, String>();
            if (request != null) {
                Enumeration<String> en = request.getParameterNames();
                while (en.hasMoreElements()) {
                    String paramName = en.nextElement();
                    if (paramName.indexOf("column_" + scopeId + "_") > -1 && !paramName.endsWith("_")) {
                        if (Util.splitString(paramName, "_")[2].indexOf("select") > -1) {
                            String dateselect = Util.fromScreen2((String) params.get(paramName), user.getLanguage());
                            if (!dateselect.equals("") && !dateselect.equals("0") && !dateselect.equals("6")) {
                                customFieldMapBase.put(Util.splitString(paramName, "_")[2].replace("select", "from"), TimeUtil.getDateByOption(dateselect, "0"));
                                customFieldMapBase.put(Util.splitString(paramName, "_")[2].replace("select", "to"), TimeUtil.getDateByOption(dateselect, "1"));
                                continue;
                            } else {
                                continue;
                            }
                        }
                        customFieldMapBase.put(Util.splitString(paramName, "_")[2], Util.fromScreen2((String) params.get(paramName), user.getLanguage()));
                    }
                }
            }

            scopeId = 1;
            Map<String, String> customFieldMapPersonal = new HashMap<String, String>();
            if (request != null) {
                Enumeration<String> en = request.getParameterNames();
                while (en.hasMoreElements()) {
                    String paramName = (String) en.nextElement();
                    if (paramName.indexOf("column_" + scopeId + "_") > -1 && !paramName.endsWith("_")) {
                        if (Util.splitString(paramName, "_")[2].indexOf("select") > -1) {
                            String dateselect = Util.fromScreen2((String) params.get(paramName), user.getLanguage());
                            if (!dateselect.equals("") && !dateselect.equals("0") && !dateselect.equals("6")) {
                                customFieldMapPersonal.put(Util.splitString(paramName, "_")[2].replace("select", "from"), TimeUtil.getDateByOption(dateselect, "0"));
                                customFieldMapPersonal.put(Util.splitString(paramName, "_")[2].replace("select", "to"), TimeUtil.getDateByOption(dateselect, "1"));
                                continue;
                            } else {
                                continue;
                            }
                        }
                        customFieldMapPersonal.put(Util.splitString(paramName, "_")[2], Util.fromScreen2((String) params.get(paramName), user.getLanguage()));
                    }
                }
            }

            scopeId = 3;
            Map<String, String> customFieldMapWork = new HashMap<String, String>();
            if (request != null) {
                Enumeration<String> en = request.getParameterNames();
                while (en.hasMoreElements()) {
                    String paramName = (String) en.nextElement();
                    if (paramName.indexOf("column_" + scopeId + "_") > -1 && !paramName.endsWith("_")) {
                        if (Util.splitString(paramName, "_")[2].indexOf("select") > -1) {
                            String dateselect = Util.fromScreen2((String) params.get(paramName), user.getLanguage());
                            if (!dateselect.equals("") && !dateselect.equals("0") && !dateselect.equals("6")) {
                                customFieldMapWork.put(Util.splitString(paramName, "_")[2].replace("select", "from"), TimeUtil.getDateByOption(dateselect, "0"));
                                customFieldMapWork.put(Util.splitString(paramName, "_")[2].replace("select", "to"), TimeUtil.getDateByOption(dateselect, "1"));
                                continue;
                            } else {
                                continue;
                            }
                        }
                        customFieldMapWork.put(Util.splitString(paramName, "_")[2], Util.fromScreen2((String) params.get(paramName), user.getLanguage()));
                    }
                }
            }

            boolean isoracle = RecordSet.getDBType().toLowerCase().equals("oracle");
            String from = Util.fromScreen((String) params.get("from"), user.getLanguage());

            String searchFrom = "";//搜索来做哪里：quick表示来自于快捷搜索；

            String strResult = " ";
            int ishead = 0;

            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-" +
                    Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                    Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            String birthbyage = "";
            String birthbyageto = "";

            int tempyear = Util.getIntValue(Util.add0(today.get(Calendar.YEAR), 4));

            if (!age.equals("")) {
                birthbyage = (tempyear - Util.getIntValue(age)) + "-" +
                        Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                        Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
            }

            if (!ageto.equals("")) {
                birthbyageto = (tempyear - Util.getIntValue(ageto)) + "-" +
                        Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-" +
                        Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);
            }


            if (tabkey.equals("default_123")) {//根据tabkey的值来查询同分部的人员
                if (ishead == 0) {
                    ishead = 1;
                  strResult = "where ";
                    RecordSet rs = new RecordSet();

                    String sql = " WITH allsub(id) as ( SELECT id FROM hrmsubcompany where id= "+user.getUserSubCompany1()+
                            " union ALL select a.id FROM hrmsubcompany a,allsub b where a.supsubcomid = b.id) select * from allsub ";
                    rs.execute(sql);
                    Integer index = 0;
                    while (rs.next()) {
                        if (index == 0) {
                            strResult += " subcompanyid1= "+rs.getString("id");
                            index++;
                        } else {
                            strResult += " or subcompanyid1= "+rs.getString("id");

                        }
                    }

                } else {
                    strResult += " and 1=1 ";
                    RecordSet rs = new RecordSet();

                    String sql = " WITH allsub(id) as ( SELECT id FROM hrmsubcompany where id= "+user.getUserSubCompany1()+
                            " union ALL select a.id FROM hrmsubcompany a,allsub b where a.supsubcomid = b.id) select * from allsub ";
                    rs.execute(sql);
                    while (rs.next()) {
                        strResult += " or subcompanyid1= "+rs.getString("id");
                    }
                }
            } else if (tabkey.equals("default_1")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where departmentid = " + user.getUserDepartment();
                } else {
                    strResult += " and departmentid = " + user.getUserDepartment();
                }
            } else if (tabkey.equals("default_2")) {
                if (showAllLevel.equals("1")) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where managerstr like '%," + user.getUID() + ",%'";
                    } else {
                        strResult += " and managerstr like '%," + user.getUID() + ",%'";
                    }
                } else {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where managerid = " + user.getUID();
                    } else {
                        strResult += " and managerid = " + user.getUID();
                    }
                }
            }else if (tabkey.equals("default_4")) {
                ResourceBelongtoComInfo resourceBelongtoComInfo = new ResourceBelongtoComInfo();
                List lsUser = resourceBelongtoComInfo.getBelongtousers(superid);

                String ids = "";
                for (int i = 0; lsUser != null && i < lsUser.size(); i++) {
                    User user = (User) lsUser.get(i);
                    Integer idnew =  user.getUID();
                    if (ids.length() > 0)
                        ids += ",";
                    ids += idnew;
                }
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where id in (" + ids + ")";
                } else {
                    strResult += " and id in (" + ids + ")";
                }

            }

            if (!resourcename.equals("")) {
                String encodeResourceName = resourcename;//weaver.common.StringUtil.string2Unicode(resourcename);
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where";
                } else {
                    strResult += " and";
                }
                String mysqlEscope = "/";
                if ("hrmResource".equals(searchFrom)) {
                    if (DialectUtil.isMySql()) {
                        strResult += " (lastname like '%" + Util.StringReplace(resourcename, "_", mysqlEscope + "_")
                                + "%' escape '" + mysqlEscope + "' or pinyinlastname like '%" + Util.StringReplace(resourcename.toLowerCase(), "_", mysqlEscope + "_")
                                + "%' escape '" + mysqlEscope + "'  or lastname like '%" + Util.StringReplace(encodeResourceName, "_", mysqlEscope + "_")
                                + "%' or pinyinlastname like '%" + Util.StringReplace(encodeResourceName.toLowerCase(), "_", mysqlEscope + "_") + "%')";
                    } else {
                        strResult += " (lastname like '%" + Util.StringReplace(resourcename, "_", "\\_")
                                + "%' escape '\\' or pinyinlastname like '%" + Util.StringReplace(resourcename.toLowerCase(), "_", "\\_")
                                + "%' escape '\\'  or lastname like '%" + Util.StringReplace(encodeResourceName, "_", "\\_")
                                + "%' or pinyinlastname like '%" + Util.StringReplace(encodeResourceName.toLowerCase(), "_", "\\_") + "%')";
                    }

                } else {
                    if (DialectUtil.isMySql()) {
                        strResult += " (lastname like '%" + Util.StringReplace(resourcename, "_", mysqlEscope + "_") + "%' escape '" + mysqlEscope + "' or pinyinlastname like '%"
                                + Util.StringReplace(resourcename.toLowerCase(), "_", mysqlEscope + "_")
                                + "%' escape '" + mysqlEscope + "'  or mobile like '%" + Util.StringReplace(resourcename, "_", mysqlEscope + "_")
                                + "%' escape '" + mysqlEscope + "' or lastname like '%" + Util.StringReplace(encodeResourceName, "_", "_")
                                + "%' or pinyinlastname like '%" + Util.StringReplace(encodeResourceName.toLowerCase(), "_", mysqlEscope + "_")
                                + "%' or workcode like '%" + Util.StringReplace(encodeResourceName.toLowerCase(), "_", mysqlEscope + "_")
                                + "%' or mobile like '%" + Util.StringReplace(encodeResourceName, "_", mysqlEscope + "_") + "%')";
                    } else {
                        strResult += " (lastname like '%" + Util.StringReplace(resourcename, "_", "\\_")
                                + "%' escape '\\' or pinyinlastname like '%" + Util.StringReplace(resourcename.toLowerCase(), "_", "\\_")
                                + "%' escape '\\'  or mobile like '%" + Util.StringReplace(resourcename, "_", "\\_")
                                + "%' escape '\\' or lastname like '%" + Util.StringReplace(encodeResourceName, "_", "\\_")
                                + "%' or pinyinlastname like '%" + Util.StringReplace(encodeResourceName.toLowerCase(), "_", "\\_")
                                + "%' or workcode like '%" + Util.StringReplace(encodeResourceName.toLowerCase(), "_", "\\_")
                                + "%' or mobile like '%" + Util.StringReplace(encodeResourceName, "_", "\\_") + "%')";
                    }
                }
            }

            if (!belongto.equals("") && !belongto.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where belongto = " + belongto + " ";
                } else {
                    strResult += " and belongto =" + belongto + " ";
                }
            }

            if (!jobtitle.equals("") && !jobtitle.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where jobtitle in(select id from HrmJobTitles where jobtitlename like '%" + jobtitle + "%')";
                } else {
                    strResult += " and jobtitle in(select id from HrmJobTitles where jobtitlename like '%" + jobtitle + "%')";
                }
            }

            if (!activitydesc.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where jobactivitydesc like '%" + activitydesc + "%' ";
                } else {
                    strResult += " and jobactivitydesc like '%" + activitydesc + "%' ";
                }
            }

            if (!jobgroup.equals("") && !jobgroup.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where jobtitle in( select id from HrmJobTitles where jobactivityid in( select id from HrmJobActivities where jobgroupid = " + jobgroup + ")) ";
                } else {
                    strResult += " and jobtitle in( select id from HrmJobTitles where jobactivityid in( select id from HrmJobActivities where jobgroupid = " + jobgroup + ")) ";
                }
            }

            if (!jobactivity.equals("") && !jobactivity.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where jobtitle  in(select id from HrmJobTitles where jobactivityid = " + jobactivity + ") ";
                } else {
                    strResult += " and jobtitle  in(select id from HrmJobTitles where jobactivityid = " + jobactivity + ") ";
                }
            }

            if (!costcenter.equals("") && !costcenter.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where costcenterid = " + costcenter;
                } else {
                    strResult += " and costcenterid = " + costcenter;
                }
            }

            if (!resourcetype.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where resourcetype = '" + resourcetype + "' ";
                } else {
                    strResult += " and resourcetype  = '" + resourcetype + "' ";
                }
            }

            if (!subcompanyid1.equals("0") && !subcompanyid1.equals("")) {
                try {
                    String subcompanyAll;
                    if (subcompanyid1.startsWith("-")) {//多维
                        subcompanyAll = "";

                        String[] sub = subcompanyid1.split(",");
                        if (sub != null) {
                            for (int cx = 0; cx < sub.length; cx++) {
                                String subid_ = sub[cx];
                                if (subid_ != null && !"".equals(subid_) && showAllLevel.equals("1")) {
                                    subcompanyAll = SubCompanyVirtualComInfo.getAllChildSubcompanyId(subid_, subcompanyAll);
                                }
                            }
                        }
                        if (!"".equals(subcompanyAll)) {
                            subcompanyAll = subcompanyid1 + subcompanyAll.trim();
                        } else {
                            subcompanyAll = subcompanyid1;
                        }
                        if (ishead == 0) {
                            ishead = 1;
                            strResult = " where EXISTS (SELECT * FROM hrmresourcevirtual WHERE hrmresource.id = resourceid AND hrmresourcevirtual.subcompanyid IN ( " + subcompanyAll + " ) )";
                        } else {
                            strResult += " and EXISTS (SELECT * FROM hrmresourcevirtual WHERE hrmresource.id = resourceid AND hrmresourcevirtual.subcompanyid IN ( " + subcompanyAll + " ) )";
                        }
                    } else {
                        subcompanyAll = "";

                        String[] sub = subcompanyid1.split(",");
                        if (sub != null) {
                            for (int cx = 0; cx < sub.length; cx++) {
                                String subid_ = sub[cx];
                                if (subid_ != null && !"".equals(subid_) && showAllLevel.equals("1")) {
                                    subcompanyAll = SubCompanyComInfo.getAllChildSubcompanyId(subid_, subcompanyAll);
                                }
                            }
                        }
                        if (!"".equals(subcompanyAll)) {
                            subcompanyAll = subcompanyid1 + subcompanyAll.trim();
                        } else {
                            subcompanyAll = subcompanyid1;
                        }
                        String tmpSql = Tools.getOracleSQLIn(subcompanyAll, "subcompanyid1");
                        if (ishead == 0) {
                            ishead = 1;
                            strResult = " where (" + tmpSql + ")";
                        } else {
                            strResult += " and (" + tmpSql + ")";
                        }
                    }
                } catch (Exception e) {
                    writeLog(e.getMessage());
                }
            }

            if (!departmentid.equals("") && !departmentid.equals("0")) {
                try {
                    //组织图表的查询人员还是根据传入的分部，部门来显示
                    if ("hrmorg".equalsIgnoreCase(from)) {
                        if (departmentid.startsWith("-")) {//多维

                            String tmpSql = Tools.getOracleSQLIn(departmentid, "hrmresourcevirtual.departmentid");
                            if (ishead == 0) {
                                ishead = 1;
                                strResult = " where EXISTS (SELECT * FROM hrmresourcevirtual WHERE hrmresource.id = resourceid AND  ( " + tmpSql + " ) )";
                            } else {
                                strResult += " and EXISTS (SELECT * FROM hrmresourcevirtual WHERE hrmresource.id = resourceid AND   ( " + tmpSql + " ) )";
                            }
                        } else {
                            String tmpSql = Tools.getOracleSQLIn(departmentid, "departmentid");
                            if (ishead == 0) {
                                ishead = 1;
                                strResult = " where (" + tmpSql + ")";
                            } else {
                                strResult += " and (" + tmpSql + ")";
                            }
                        }
                    } else {
                        String departmentAll;
                        if (departmentid.startsWith("-")) {//多维
                            departmentAll = "";

                            String[] dep = departmentid.split(",");
                            if (dep != null) {
                                for (int cx = 0; cx < dep.length; cx++) {
                                    String depid_ = dep[cx];
                                    if (depid_ != null && !"".equals(depid_) && showAllLevel.equals("1")) {
                                        departmentAll = DepartmentVirtualComInfo.getAllChildDepartId(depid_, departmentAll);
                                    }
                                }
                            }
                            if (!"".equals(departmentAll)) {
                                departmentAll = departmentid + departmentAll.trim();
                            } else {
                                departmentAll = departmentid;
                            }
                            if (ishead == 0) {
                                ishead = 1;
                                strResult = " where EXISTS (SELECT * FROM hrmresourcevirtual WHERE hrmresource.id = resourceid AND hrmresourcevirtual.departmentid IN ( " + departmentAll + " ) )";
                            } else {
                                strResult += " and EXISTS (SELECT * FROM hrmresourcevirtual WHERE hrmresource.id = resourceid AND hrmresourcevirtual.departmentid IN ( " + departmentAll + " ) )";
                            }
                        } else {

                            departmentAll = "";

                            String[] dep = departmentid.split(",");
                            if (dep != null) {
                                for (int cx = 0; cx < dep.length; cx++) {
                                    String depid_ = dep[cx];
                                    if (depid_ != null && !"".equals(depid_) && showAllLevel.equals("1")) {
                                        departmentAll = DepartmentComInfo.getAllChildDepartId(depid_, departmentAll);
                                    }
                                }
                            }
                            if (!"".equals(departmentAll)) {
                                departmentAll = departmentid + departmentAll.trim();
                            } else {
                                departmentAll = departmentid;
                            }
                            String tmpSql = Tools.getOracleSQLIn(departmentAll, "departmentid");
                            if (ishead == 0) {
                                ishead = 1;
                                strResult = " where (" + tmpSql + ")";
                            } else {
                                strResult += " and (" + tmpSql + ")";
                            }
                        }
                    }
                } catch (Exception e) {
                    writeLog(e.getMessage());
                }
            }

            if (!groupId.equals("")) {//常用组查询条件（通讯录不需要增加权限查看，有公共组的共享范围就放出可查看人员）
                boolean cansave = true;
          //      boolean cansave = HrmUserVarify.checkUserRight("CustomGroup:Edit", user);
                String conditionSql = "";
                if(!cansave){
                    conditionSql += " and groupid in (SELECT id FROM HrmGroup WHERE owner=" + user.getUID() + " AND TYPE=0 AND (canceled IS NULL OR canceled !='1'))";
                }else{
                    conditionSql += " and groupid in (SELECT id FROM HrmGroup WHERE ((owner=" + user.getUID() + " AND TYPE=0) OR TYPE=1) AND (canceled IS NULL OR canceled !='1'))";
                }
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where EXISTS (select 1 from HrmGroupMembers where hrmResource.id=userid and groupid=" + groupId + conditionSql + ") ";
                } else {
                    strResult += " and EXISTS (select 1 from HrmGroupMembers where hrmResource.id=userid and groupid=" + groupId + conditionSql + ") ";
                }
            }

            if (!location.equals("") && !location.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where locationid = " + location;
                } else {
                    strResult += " and locationid = " + location;
                }
            }

            if (!manager.equals("") && !manager.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where managerid = " + manager;
                } else {
                    strResult += " and managerid = " + manager;
                }
            }

            if (!assistant.equals("") && !assistant.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where assistantid = " + assistant;
                } else {
                    strResult += " and assistantid = " + assistant;
                }
            }

            if (!roles.equals("") && !roles.equals("0")) {
                String roleSql = " SELECT distinct resourceid FROM ( " +
                        " SELECT a.id AS resourceid, b.roleid , b.rolelevel FROM HrmResource a, HrmRoleMembers b " +
                        " WHERE (a.id=b.resourceid and b.resourcetype=1) " +
                        " UNION ALL " +
                        " SELECT a.id AS resourceid, b.roleid , b.rolelevel FROM HrmResourceManager a, HrmRoleMembers b " +
                        " WHERE (a.id=b.resourceid and b.resourcetype IN(7,8)) " +
                        " UNION ALL " +
                        " SELECT a.id AS resourceid, b.roleid , b.rolelevel FROM HrmResource a, HrmRoleMembers b " +
                        " WHERE (a.subcompanyid1 = b.resourceid AND a.seclevel>=b.seclevelfrom AND a.seclevel<=b.seclevelto AND b.resourcetype=2) " +
                        " UNION ALL " +
                        " SELECT a.id AS resourceid, b.roleid , b.rolelevel FROM HrmResource a, HrmRoleMembers b " +
                        " WHERE (a.departmentid = b.resourceid AND a.seclevel>=b.seclevelfrom AND a.seclevel<=b.seclevelto AND b.resourcetype=3) " +
                        " UNION ALL " +
                        " SELECT a.id AS resourceid, b.roleid , b.rolelevel FROM HrmResource a, HrmRoleMembers b " +
                        " WHERE  (a.jobtitle = b.resourceid AND b.resourcetype=5 AND (b.jobtitlelevel=1 OR (b.jobtitlelevel=2 AND a.subcompanyid1 IN(b.subdepid)) OR (b.jobtitlelevel=3 AND a.departmentid IN(b.subdepid))))) t " +
                        " WHERE roleid in ( " + roles + ")";
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where HrmResource.id in( " + roleSql + ")";
                } else {
                    strResult += " and HrmResource.id in( " + "+roleSql+" + ")";
                }
            }

            if (!seclevel.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where seclevel >= " + seclevel;
                } else {
                    strResult += " and seclevel >= " + seclevel;
                }
            }

            if (!seclevelto.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where seclevel <= " + seclevelto;
                } else {
                    strResult += " and seclevel <= " + seclevelto;
                }
            }

            if (!joblevel.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where joblevel >= " + joblevel;
                } else {
                    strResult += " and joblevel >= " + joblevel;
                }
            }

            if (!joblevelto.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where joblevel <= " + joblevelto;
                } else {
                    strResult += " and joblevel <= " + joblevelto;
                }
            }

            if (!workroom.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where workroom like '%" + workroom + "%'";
                } else {
                    strResult += " and workroom like '%" + workroom + "%'";
                }
            }

            if (!telephone.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where telephone like '%" + telephone + "%'";
                } else {
                    strResult += " and telephone like '%" + telephone + "%'";
                }
            }

            if (!startdate.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where startdate >= '" + startdate + "'";
                } else {
                    strResult += " and startdate >= '" + startdate + "'";
                }
            }

            if (!startdateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where startdate <= '" + startdateto + "' and startdate<>''";
                    } else {
                        strResult += " and startdate <= '" + startdateto + "' and startdate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where startdate <= '" + startdateto + "' and startdate is not null";
                    } else {
                        strResult += " and startdate <= '" + startdateto + "' and startdate is not null";
                    }
                }
            }

            //拼接入职日期查询条件
            if (!companystartdate.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where companystartdate >= '" + companystartdate + "'";
                } else {
                    strResult += " and companystartdate >= '" + companystartdate + "'";
                }
            }

            if (!companystartdateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where companystartdate <= '" + companystartdateto + "' and companystartdate<>''";
                    } else {
                        strResult += " and companystartdate <= '" + companystartdateto + "' and companystartdate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where companystartdate <= '" + companystartdateto + "' and companystartdate is not null";
                    } else {
                        strResult += " and companystartdate <= '" + companystartdateto + "' and companystartdate is not null";
                    }
                }
            }

            //拼接参加工作查询条件
            if (!workstartdate.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where workstartdate >= '" + workstartdate + "'";
                } else {
                    strResult += " and workstartdate >= '" + workstartdate + "'";
                }
            }

            if (!companyworkyear.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where companyworkyear >= " + companyworkyear;
                } else {
                    strResult += " and companyworkyear >= " + companyworkyear;
                }
            }

            if (!companyworkyearto.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where companyworkyear <= " + companyworkyearto;
                } else {
                    strResult += " and companyworkyear <= " + companyworkyearto;
                }
            }

            if (!workstartdateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where workstartdate <= '" + workstartdateto + "' and workstartdate<>''";
                    } else {
                        strResult += " and workstartdate <= '" + workstartdateto + "' and workstartdate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where workstartdate <= '" + workstartdateto + "' and workstartdate is not null";
                    } else {
                        strResult += " and workstartdate <= '" + workstartdateto + "' and workstartdate is not null";
                    }
                }
            }

            if (!workyear.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where workyear >= " + workyear;
                } else {
                    strResult += " and workyear >= " + workyear;
                }
            }

            if (!workyearto.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where workyear <= " + workyearto;
                } else {
                    strResult += " and workyear <= " + workyearto;
                }
            }

            if (!enddate.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where enddate >= '" + enddate + "'";
                } else {
                    strResult += " and enddate >= '" + enddate + "'";
                }
            }
            if (!enddateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where enddate <= '" + enddateto + "' and enddate<>''";
                    } else {
                        strResult += " and enddate <= '" + enddateto + "' and enddate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where enddate <= '" + enddateto + "' and enddate is not null";
                    } else {
                        strResult += " and enddate <= '" + enddateto + "' and enddate is not null";
                    }
                }
            }


            if (!contractdate.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where probationenddate >= '" + contractdate + "'";
                } else {
                    strResult += " and probationenddate >= '" + contractdate + "'";
                }
            }
            if (!contractdateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where probationenddate <= '" + contractdateto + "' and probationenddate<>''";
                    } else {
                        strResult += " and probationenddate <= '" + contractdateto + "' and probationenddate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where probationenddate <= '" + contractdateto + "' and probationenddate is not null";
                    } else {
                        strResult += " and probationenddate <= '" + contractdateto + "' and probationenddate is not null";
                    }
                }
            }

            if (!createdatefrom.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where createdate >= '" + createdatefrom + "'";
                } else {
                    strResult += " and createdate >= '" + createdatefrom + "'";
                }
            }
            if (!createdateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where createdate <= '" + createdateto + "' and createdate<>''";
                    } else {
                        strResult += " and createdate <= '" + createdateto + "' and createdate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where createdate <= '" + createdateto + "' and createdate is not null";
                    } else {
                        strResult += " and createdate <= '" + createdateto + "' and createdate is not null";
                    }
                }
            }

            if (!birthday.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where birthday >= '" + birthday + "' and birthday<>''";
                    } else {
                        strResult += " and birthday >= '" + birthday + "' and birthday<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where birthday >= '" + birthday + "' and birthday is not null";
                    } else {
                        strResult += " and birthday >= '" + birthday + "' and birthday is not null";
                    }
                }
            }

            if (!birthdayto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where birthday <= '" + birthdayto + "' and birthday<>''";
                    } else {
                        strResult += " and birthday <= '" + birthdayto + "' and birthday<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where birthday <= '" + birthdayto + "' and birthday is not null";
                    } else {
                        strResult += " and birthday <= '" + birthdayto + "' and birthday is not null";
                    }
                }
            }

            if (!birthbyage.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where birthday <= '" + birthbyage + "' and birthday<>''";
                    } else {
                        strResult += " and birthday <= '" + birthbyage + "' and birthday<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where birthday <= '" + birthbyage + "' and birthday is not null";
                    } else {
                        strResult += " and birthday <= '" + birthbyage + "' and birthday is not null";
                    }
                }
            }

            if (!birthbyageto.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where birthday >= '" + birthbyageto + "' ";
                } else {
                    strResult += " and birthday >= '" + birthbyageto + "' ";
                }
            }

            if (!sex.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where sex = '" + sex + "'";
                } else {
                    strResult += " and sex = '" + sex + "'";
                }
            }

            if (accounttype != -1) {
                if (ishead == 0) {
                    ishead = 1;
                    if (accounttype == 0)
                        strResult = " where (accounttype=0 OR accounttype IS NULL)";
                    else
                        strResult = " where accounttype=1";
                } else {
                    if (accounttype == 0)
                        strResult += " and (accounttype=0 OR accounttype IS NULL)";
                    else
                        strResult += " and accounttype=1";
                }
            }

            if (!resourceidfrom.equals("") && !resourceidfrom.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where id >= " + resourceidfrom;
                } else {
                    strResult += " and id >= " + resourceidfrom;
                }
            }

            if (!resourceidto.equals("") && !resourceidto.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where id <= " + resourceidto;
                } else {
                    strResult += " and id <= " + resourceidto;
                }
            }

            if (!workcode.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where workcode like '%" + workcode + "%' ";
                } else {
                    strResult += " and workcode like '%" + workcode + "%' ";
                }
            }

            if (!jobcall.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where jobcall = " + jobcall + " ";
                } else {
                    strResult += " and jobcall = " + jobcall + " ";
                }
            }

            if (!mobile.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where mobile like '%" + mobile + "%' ";
                } else {
                    strResult += " and mobile like '%" + mobile + "%' ";
                }
            }

            if (!mobilecall.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where mobilecall like '%" + mobilecall + "%' ";
                } else {
                    strResult += " and mobilecall like '%" + mobilecall + "%' ";
                }
            }

            if (!fax.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where fax like '%" + fax + "%' ";
                } else {
                    strResult += " and fax like '%" + fax + "%' ";
                }
            }

            if (!email.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where email like '%" + email + "%' ";
                } else {
                    strResult += " and email like '%" + email + "%' ";
                }
            }

            if (!folk.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where folk like '%" + folk + "%' ";
                } else {
                    strResult += " and folk like '%" + folk + "%' ";
                }
            }

            if (!nativeplace.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where nativeplace like '%" + nativeplace + "%' ";
                } else {
                    strResult += " and nativeplace like '%" + nativeplace + "%' ";
                }
            }

            if (!regresidentplace.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where regresidentplace like '%" + regresidentplace + "%' ";
                } else {
                    strResult += " and regresidentplace like '%" + regresidentplace + "%' ";
                }
            }

            if (!maritalstatus.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where maritalstatus = '" + maritalstatus + "' ";
                } else {
                    strResult += " and maritalstatus = '" + maritalstatus + "' ";
                }
            }

            if (!certificatenum.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where certificatenum like '%" + certificatenum + "%' ";
                } else {
                    strResult += " and certificatenum like '%" + certificatenum + "%' ";
                }
            }

            if (!tempresidentnumber.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where tempresidentnumber like '%" + tempresidentnumber + "%' ";
                } else {
                    strResult += " and tempresidentnumber like '%" + tempresidentnumber + "%' ";
                }
            }

            if (!residentplace.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where residentplace like '%" + residentplace + "%' ";
                } else {
                    strResult += " and residentplace like '%" + residentplace + "%' ";
                }
            }

            if (!homeaddress.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where homeaddress like '%" + homeaddress + "%' ";
                } else {
                    strResult += " and homeaddress like '%" + homeaddress + "%' ";
                }
            }

            if (!healthinfo.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where healthinfo = '" + healthinfo + "' ";
                } else {
                    strResult += " and healthinfo = '" + healthinfo + "' ";
                }
            }

            if (!heightfrom.equals("") && !heightfrom.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where height >= " + heightfrom;
                } else {
                    strResult += " and height >= " + heightfrom;
                }
            }

            if (!heightto.equals("") && !heightto.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where height <= " + heightto;
                } else {
                    strResult += " and height <= " + heightto;
                }
            }

            if (!weightfrom.equals("") && !weightfrom.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where weight >= " + weightfrom;
                } else {
                    strResult += " and weight >= " + weightfrom;
                }
            }

            if (!weightto.equals("") && !weightto.equals("0")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where weight <= " + weightto;
                } else {
                    strResult += " and weight <= " + weightto;
                }
            }

            if (!educationlevel.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where educationlevel = " + educationlevel;
                } else {
                    strResult += " and educationlevel = " + educationlevel;
                }
            }

            if (!educationlevelto.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where educationlevel <= " + educationlevelto;
                } else {
                    strResult += " and educationlevel <= " + educationlevelto;
                }
            }

            if (!degree.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where degree like '%" + degree + "%' ";
                } else {
                    strResult += " and degree like '%" + degree + "%' ";
                }
            }

            if (!usekind.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where usekind = " + usekind + " ";
                } else {
                    strResult += " and usekind = " + usekind + " ";
                }
            }

            if (!policy.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where policy like '%" + policy + "%' ";
                } else {
                    strResult += " and policy like '%" + policy + "%' ";
                }
            }


            if (!bememberdatefrom.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where bememberdate >= '" + bememberdatefrom + "'";
                } else {
                    strResult += " and bememberdate >= '" + bememberdatefrom + "'";
                }
            }


            if (!bememberdateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where bememberdate <= '" + bememberdateto + "' and bememberdate<>''";
                    } else {
                        strResult += " and bememberdate <= '" + bememberdateto + "' and bememberdate<>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where bememberdate <= '" + bememberdateto + "' and bememberdate is not null";
                    } else {
                        strResult += " and bememberdate <= '" + bememberdateto + "' and bememberdate is not null";
                    }
                }
            }

            if (!datefield1.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield1 >= '" + datefield1 + "'";
                } else {
                    strResult += " and datefield1 >= '" + datefield1 + "'";
                }
            }
            if (!datefield2.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield2 >= '" + datefield2 + "'";
                } else {
                    strResult += " and datefield2 >= '" + datefield2 + "'";
                }
            }
            if (!datefield3.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield3 >= '" + datefield3 + "'";
                } else {
                    strResult += " and datefield3 >= '" + datefield3 + "'";
                }
            }
            if (!datefield4.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield4 >= '" + datefield4 + "'";
                } else {
                    strResult += " and datefield4 >= '" + datefield4 + "'";
                }
            }
            if (!datefield5.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield5 >= '" + datefield5 + "'";
                } else {
                    strResult += " and datefield5 >= '" + datefield5 + "'";
                }
            }
            if (!datefield1to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield1 <= '" + datefield1to + "'";
                } else {
                    strResult += " and datefield1 <= '" + datefield1to + "'";
                }
            }
            if (!datefield2to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield2 <= '" + datefield2to + "'";
                } else {
                    strResult += " and datefield2 <= '" + datefield2to + "'";
                }
            }
            if (!datefield3to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield3 <= '" + datefield3to + "'";
                } else {
                    strResult += " and datefield3 <= '" + datefield3to + "'";
                }
            }
            if (!datefield4to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield4 <= '" + datefield4to + "'";
                } else {
                    strResult += " and datefield4 <= '" + datefield4to + "'";
                }
            }
            if (!datefield5to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where datefield5 <= '" + datefield5to + "'";
                } else {
                    strResult += " and datefield5 <= '" + datefield5to + "'";
                }
            }
            if (!numberfield1.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield1 >= " + numberfield1;
                } else {
                    strResult += " and numberfield1 >= " + numberfield1;
                }
            }

            if (!numberfield1to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield1 <= " + numberfield1to;
                } else {
                    strResult += " and numberfield1 <= " + numberfield1to;
                }
            }
            if (!numberfield2.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield2 >= " + numberfield2;
                } else {
                    strResult += " and numberfield2 >= " + numberfield2;
                }
            }

            if (!numberfield2to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield2 <= " + numberfield2to;
                } else {
                    strResult += " and numberfield2 <= " + numberfield2to;
                }
            }
            if (!numberfield3.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield3 >= " + numberfield3;
                } else {
                    strResult += " and numberfield3 >= " + numberfield3;
                }
            }

            if (!numberfield3to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield3 <= " + numberfield3to;
                } else {
                    strResult += " and numberfield3 <= " + numberfield3to;
                }
            }
            if (!numberfield4.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield4 >= " + numberfield4;
                } else {
                    strResult += " and numberfield4 >= " + numberfield4;
                }
            }

            if (!numberfield4to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield4 <= " + numberfield4to;
                } else {
                    strResult += " and numberfield4 <= " + numberfield4to;
                }
            }
            if (!numberfield5.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield5 >= " + numberfield5;
                } else {
                    strResult += " and numberfield5 >= " + numberfield5;
                }
            }

            if (!numberfield5to.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where numberfield5 <= " + numberfield5to;
                } else {
                    strResult += " and numberfield5 <= " + numberfield5to;
                }
            }
            if (!tff01name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where textfield1 like '%" + tff01name + "%' ";
                } else {
                    strResult += " and textfield1 like '%" + tff01name + "%' ";
                }
            }
            if (!tff02name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where textfield2 like '%" + tff02name + "%' ";
                } else {
                    strResult += " and textfield2 like '%" + tff02name + "%' ";
                }
            }
            if (!tff03name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where textfield3 like '%" + tff03name + "%' ";
                } else {
                    strResult += " and textfield3 like '%" + tff03name + "%' ";
                }
            }
            if (!tff04name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where textfield4 like '%" + tff04name + "%' ";
                } else {
                    strResult += " and textfield4 like '%" + tff04name + "%' ";
                }
            }
            if (!tff05name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where textfield5 like '%" + tff05name + "%' ";
                } else {
                    strResult += " and textfield5 like '%" + tff05name + "%' ";
                }
            }
            if (!bff01name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where tinyintfield1 = " + bff01name + " ";
                } else {
                    strResult += " and tinyintfield1 = " + bff01name + " ";
                }
            }
            if (!bff02name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where tinyintfield2 = " + bff02name + "  ";
                } else {
                    strResult += " and tinyintfield2 = " + bff02name + "  ";
                }
            }
            if (!bff03name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where tinyintfield3 = " + bff03name + "  ";
                } else {
                    strResult += " and tinyintfield3 = " + bff03name + "  ";
                }
            }
            if (!bff04name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where tinyintfield4 = " + bff04name + "  ";
                } else {
                    strResult += " and tinyintfield4 = " + bff04name + "  ";
                }
            }
            if (!bff05name.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where tinyintfield5 = " + bff05name + "  ";
                } else {
                    strResult += " and tinyintfield5 = " + bff05name + "  ";
                }
            }

            if (!bepartydatefrom.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where bepartydate >= '" + bepartydatefrom + "'";
                } else {
                    strResult += " and bepartydate >= '" + bepartydatefrom + "'";
                }
            }
	/*
			if(!bepartydateto.equals("")){
				if(ishead==0){
					ishead=1;
					strResult = " where bepartydate <= '"+bepartydateto+"'  and bepartydate<>''";
				}else{
					strResult += " and bepartydate <= '"+bepartydateto+"'  and bepartydate<>''";
				}
			}
	*/
            if (!bepartydateto.equals("")) {
                if (!isoracle) {
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where bepartydate <= '" + bepartydateto + "' and bepartydate <>''";
                    } else {
                        strResult += " and bepartydate <= '" + bepartydateto + "' and bepartydate <>''";
                    }
                } else { //如果数据库为oracle的话
                    if (ishead == 0) {
                        ishead = 1;
                        strResult = " where bepartydate <= '" + bepartydateto + "' and bepartydate is not null";
                    } else {
                        strResult += " and bepartydate <= '" + bepartydateto + "' and bepartydate is not null";
                    }
                }
            }

            if (!islabouunion.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where islabouunion = '" + islabouunion + "' ";
                } else {
                    strResult += " and islabouunion = '" + islabouunion + "' ";
                }
            }

            if (!bankid1.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where bankid1 = " + bankid1 + " ";
                } else {
                    strResult += " and bankid1 = " + bankid1 + " ";
                }
            }

            if (!accountid1.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where accountid1 like '%" + accountid1 + "%' ";
                } else {
                    strResult += " and accountid1 like '%" + accountid1 + "%' ";
                }
            }

            if (!accumfundaccount.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where accumfundaccount like '%" + accumfundaccount + "%' ";
                } else {
                    strResult += " and accumfundaccount like '%" + accumfundaccount + "%' ";
                }
            }

            if (!loginid.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where loginid like '%" + loginid + "%' ";
                } else {
                    strResult += " and loginid like '%" + loginid + "%' ";
                }
            }

            if (!systemlanguage.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where systemlanguage = " + systemlanguage + " ";
                } else {
                    strResult += " and systemlanguage = " + systemlanguage + " ";
                }
            }

            if (!status.equals("") && !status.equals("8") && !status.equals("9")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where status = " + status + " ";
                } else {
                    strResult += " and status = " + status + " ";
                }
            }

            if (status.equals("8") || status.equals("")) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where (status = 0 or status = 1 or status = 2 or status = 3) ";
                } else {
                    strResult += " and (status = 0 or status = 1 or status = 2 or status = 3) ";
                }
            }


            if (Util.getIntValue(virtualtype) < -1) {
                if (ishead == 0) {
                    ishead = 1;
                    strResult = " where EXISTS ( SELECT * FROM   hrmresourcevirtual WHERE  hrmresource.id = hrmresourcevirtual.resourceid AND hrmresourcevirtual.virtualtype = " + virtualtype + " )";
                } else {
                    strResult += " and EXISTS ( SELECT * FROM   hrmresourcevirtual WHERE  hrmresource.id = hrmresourcevirtual.resourceid AND hrmresourcevirtual.virtualtype = " + virtualtype + " )";
                }
            }

            if (customFieldMapBase.size() > 0) {
                Set fieldidSet = customFieldMapBase.keySet();
                for (Iterator iter = fieldidSet.iterator(); iter.hasNext(); ) {
                    String fieldid = (String) iter.next();
                    String value = (String) customFieldMapBase.get(fieldid);
                    String fieldhtmltype = "";
                    String fielddbtype = "";
                    String type = "";
                    if (value.equals("")) continue;
                    if (ishead == 0) {
                        ishead = 1;
                        if (fieldid.indexOf("from") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("from"));
                            strResult = " where t0_field" + realid + ">='" + value + "'";
                        } else if (fieldid.indexOf("to") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("to"));
                            strResult = " where t0_field" + realid + "<='" + value + "'";
                        } else {
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select fieldhtmltype, fielddbtype, type from cus_formdict where id = " + fieldid);
                            if (rs.next()) {
                                fieldhtmltype = rs.getString("fieldhtmltype");
                                fielddbtype = rs.getString("fielddbtype");
                                type = rs.getString("type");
                            }
                            if ("1".equals(fieldhtmltype) || "2".equals(fieldhtmltype) || "text".equals(fielddbtype)) {
                                strResult = " where t0_field" + fieldid + " like '%" + value + "%'";
                            } else if (("3".equals(fieldhtmltype) && fielddbtype.toLowerCase().equals("text")) || "161".equals(type) || "162".equals(type)) {
                                strResult = " where 1=1 ";
                                strResult += " and ( 1=2 ";
                                //浏览框 id为逗号隔开的形式
                                String[] vals = value.split(",");
                                for (String val : vals) {
                                    if (val.length() == 0) continue;
                                    if (isoracle) {
                                        strResult += " or ','||cast(t0_field" + fieldid + " as VARCHAR2(4000))||',' like '%," + val + ",%'";
                                    } else if (DialectUtil.isMySql()) {
                                        IDbDialectSql dialect = DbDialectFactory.get(rs.getDBType());
                                        String castCondition = dialect.castToChar("t0_field" + fieldid, 4000);
                                        String dot = "','";
                                        String t0FieldConcat = dialect.concatStr(dot, castCondition, dot);
                                        strResult += " or " + t0FieldConcat + " like '%," + val + ",%'";
                                    } else
                                        strResult += " or ','+cast(t0_field" + fieldid + " as VARCHAR(4000))+',' like '%," + val + ",%'";
                                }
                                strResult += " ) ";
                            } else {
                                strResult = " where t0_field" + fieldid + "='" + value + "'";
                            }
                        }
                    } else {
                        if (fieldid.indexOf("from") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("from"));
                            strResult += " and t0_field" + realid + ">='" + value + "'";
                        } else if (fieldid.indexOf("to") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("to"));
                            strResult += " and t0_field" + realid + "<='" + value + "'";
                        } else {
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select fieldhtmltype, fielddbtype, type from cus_formdict where id = " + fieldid);
                            if (rs.next()) {
                                fieldhtmltype = rs.getString("fieldhtmltype");
                                fielddbtype = rs.getString("fielddbtype");
                                type = rs.getString("type");
                            }
                            if ("1".equals(fieldhtmltype) || "2".equals(fieldhtmltype) || "text".equals(fielddbtype)) {
                                strResult += " and t0_field" + fieldid + " like '%" + value + "%'";
                            } else if (("3".equals(fieldhtmltype) && fielddbtype.toLowerCase().equals("text")) || "161".equals(type) || "162".equals(type)) {
                                strResult += " and ( 1=2 ";
                                //浏览框 id为逗号隔开的形式
                                String[] vals = value.split(",");
                                for (String val : vals) {
                                    if (val.length() == 0) continue;
                                    if (isoracle) {
                                        strResult += " or ','||cast(t0_field" + fieldid + " as VARCHAR(4000))||',' like '%," + val + ",%'";
                                    } else if (DialectUtil.isMySql()) {
                                        IDbDialectSql dialect = DbDialectFactory.get(rs.getDBType());
                                        String castCondition = dialect.castToChar("t0_field" + fieldid, 4000);
                                        String dot = "','";
                                        String t0FieldConcat = dialect.concatStr(dot, castCondition, dot);
                                        strResult += " or " + t0FieldConcat + " like '%," + val + ",%'";
                                    } else {
                                        strResult += " or ','+cast(t0_field" + fieldid + " as NVARCHAR)+',' like '%," + val + ",%'";
                                    }
                                }
                                strResult += " ) ";
                            } else {
                                strResult += " and t0_field" + fieldid + "='" + value + "'";
                            }
                        }
                    }
                }

            }

            if (customFieldMapPersonal.size() > 0) {
                Set fieldidSet = customFieldMapPersonal.keySet();
                for (Iterator iter = fieldidSet.iterator(); iter.hasNext(); ) {
                    String fieldid = (String) iter.next();
                    String value = (String) customFieldMapPersonal.get(fieldid);
                    String fieldhtmltype = "";
                    String fielddbtype = "";
                    String type = "";
                    if (value.equals("")) continue;
                    if (ishead == 0) {
                        ishead = 1;
                        if (fieldid.indexOf("from") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("from"));
                            strResult = " where t1_field" + realid + ">='" + value + "'";
                        } else if (fieldid.indexOf("to") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("to"));
                            strResult = " where t1_field" + realid + "<='" + value + "'";
                        } else {
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select fieldhtmltype, fielddbtype, type from cus_formdict where id = " + fieldid);
                            if (rs.next()) {
                                fieldhtmltype = rs.getString("fieldhtmltype");
                                fielddbtype = rs.getString("fielddbtype");
                                type = rs.getString("type");
                            }
                            if ("1".equals(fieldhtmltype) || "2".equals(fieldhtmltype) || "text".equals(fielddbtype)) {
                                strResult = " where t1_field" + fieldid + " like '%" + value + "%'";
                            } else if (("3".equals(fieldhtmltype) && fielddbtype.toLowerCase().equals("text")) || "161".equals(type) || "162".equals(type)) {
                                strResult += " and ( 1=2 ";
                                //浏览框 id为逗号隔开的形式
                                String[] vals = value.split(",");
                                for (String val : vals) {
                                    if (val.length() == 0) continue;
                                    if (isoracle) {
                                        strResult += " or ','||cast(t1_field" + fieldid + " as VARCHAR(4000))||',' like '%," + val + ",%'";
                                    } else if (DialectUtil.isMySql()) {
                                        IDbDialectSql dialect = DbDialectFactory.get(rs.getDBType());
                                        String castCondition = dialect.castToChar("t1_field" + fieldid, 4000);
                                        String dot = "','";
                                        String t0FieldConcat = dialect.concatStr(dot, castCondition, dot);
                                        strResult += " or " + t0FieldConcat + " like '%," + val + ",%'";
                                    } else {
                                        strResult += " or ','+cast(t1_field" + fieldid + " as NVARCHAR)+',' like '%," + val + ",%'";
                                    }
                                }
                                strResult += " ) ";
                            } else {
                                strResult = " where t1_field" + fieldid + "='" + value + "'";
                            }
                        }
                    } else {
                        if (fieldid.indexOf("from") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("from"));
                            strResult += " and t1_field" + realid + ">='" + value + "'";
                        } else if (fieldid.indexOf("to") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("to"));
                            strResult += " and t1_field" + realid + "<='" + value + "'";
                        } else {
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select fieldhtmltype, fielddbtype, type from cus_formdict where id = " + fieldid);
                            if (rs.next()) {
                                fieldhtmltype = rs.getString("fieldhtmltype");
                                fielddbtype = rs.getString("fielddbtype");
                                type = rs.getString("type");
                            }
                            if ("1".equals(fieldhtmltype) || "2".equals(fieldhtmltype) || "text".equals(fielddbtype)) {
                                strResult += " and t1_field" + fieldid + " like '%" + value + "%'";
                            } else if (("3".equals(fieldhtmltype) && fielddbtype.toLowerCase().equals("text")) || "161".equals(type) || "162".equals(type)) {
                                strResult += " and ( 1=2 ";
                                //浏览框 id为逗号隔开的形式
                                String[] vals = value.split(",");
                                for (String val : vals) {
                                    if (val.length() == 0) continue;
                                    if (isoracle) {
                                        strResult += " or ','||cast(t1_field" + fieldid + " as VARCHAR(4000))||',' like '%," + val + ",%'";
                                    } else if (DialectUtil.isMySql()) {
                                        IDbDialectSql dialect = DbDialectFactory.get(rs.getDBType());
                                        String castCondition = dialect.castToChar("t1_field" + fieldid, 4000);
                                        String dot = "','";
                                        String t0FieldConcat = dialect.concatStr(dot, castCondition, dot);
                                        strResult += " or " + t0FieldConcat + " like '%," + val + ",%'";
                                    } else {
                                        strResult += " or ','+cast(t1_field" + fieldid + " as NVARCHAR)+',' like '%," + val + ",%'";
                                    }
                                }
                                strResult += " ) ";
                            } else {
                                strResult += " and t1_field" + fieldid + "='" + value + "'";
                            }
                        }
                    }
                }

            }

            if (customFieldMapWork.size() > 0) {
                Set fieldidSet = customFieldMapWork.keySet();
                for (Iterator iter = fieldidSet.iterator(); iter.hasNext(); ) {
                    String fieldid = (String) iter.next();
                    String value = (String) customFieldMapWork.get(fieldid);
                    String fieldhtmltype = "";
                    String fielddbtype = "";
                    String type = "";
                    if (value.equals("")) continue;
                    if (ishead == 0) {
                        ishead = 1;
                        if (fieldid.indexOf("from") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("from"));
                            strResult = " where t3_field" + realid + ">='" + value + "'";
                        } else if (fieldid.indexOf("to") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("to"));
                            strResult = " where t3_field" + realid + "<='" + value + "'";
                        } else {
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select fieldhtmltype, fielddbtype, type from cus_formdict where id = " + fieldid);
                            if (rs.next()) {
                                fieldhtmltype = rs.getString("fieldhtmltype");
                                fielddbtype = rs.getString("fielddbtype");
                                type = rs.getString("type");
                            }
                            if ("1".equals(fieldhtmltype) || "2".equals(fieldhtmltype) || "text".equals(fielddbtype)) {
                                strResult = " where t3_field" + fieldid + " like '%" + value + "%'";
                            } else if (("3".equals(fieldhtmltype) && fielddbtype.toLowerCase().equals("text")) || "161".equals(type) || "162".equals(type)) {
                                strResult = " where 1=1 ";
                                strResult += " and ( 1=2 ";
                                //浏览框 id为逗号隔开的形式
                                String[] vals = value.split(",");
                                for (String val : vals) {
                                    if (val.length() == 0) continue;
                                    if (isoracle) {
                                        strResult += " or ','||cast(t3_field" + fieldid + " as VARCHAR(4000))||',' like '%," + val + ",%'";
                                    } else if (DialectUtil.isMySql()) {
                                        IDbDialectSql dialect = DbDialectFactory.get(rs.getDBType());
                                        String castCondition = dialect.castToChar("t3_field" + fieldid, 4000);
                                        String dot = "','";
                                        String t0FieldConcat = dialect.concatStr(dot, castCondition, dot);
                                        strResult += " or " + t0FieldConcat + " like '%," + val + ",%'";
                                    } else {
                                        strResult += " or ','+cast(t3_field" + fieldid + " as NVARCHAR)+',' like '%," + val + ",%'";
                                    }
                                }
                                strResult += " ) ";
                            } else {
                                strResult = " where t3_field" + fieldid + "='" + value + "'";
                            }
                        }
                    } else {
                        if (fieldid.indexOf("from") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("from"));
                            strResult += " and t3_field" + realid + ">='" + value + "'";
                        } else if (fieldid.indexOf("to") > 0) {
                            String realid = fieldid.substring(0, fieldid.indexOf("to"));
                            strResult += " and t3_field" + realid + "<='" + value + "'";
                        } else {
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select fieldhtmltype, fielddbtype, type from cus_formdict where id = " + fieldid);
                            if (rs.next()) {
                                fieldhtmltype = rs.getString("fieldhtmltype");
                                fielddbtype = rs.getString("fielddbtype");
                                type = rs.getString("type");
                            }
                            if ("1".equals(fieldhtmltype) || "2".equals(fieldhtmltype) || "text".equals(fielddbtype)) {
                                strResult += " and t3_field" + fieldid + " like '%" + value + "%'";
                            } else if (("3".equals(fieldhtmltype) && fielddbtype.toLowerCase().equals("text")) || "161".equals(type) || "162".equals(type)) {
                                strResult += " and ( 1=2 ";
                                //浏览框 id为逗号隔开的形式
                                String[] vals = value.split(",");
                                for (String val : vals) {
                                    if (val.length() == 0) continue;
                                    if (isoracle) {
                                        strResult += " or ','||cast(t3_field" + fieldid + " as VARCHAR(4000))||',' like '%," + val + ",%'";
                                    } else if (DialectUtil.isMySql()) {
                                        IDbDialectSql dialect = DbDialectFactory.get(rs.getDBType());
                                        String castCondition = dialect.castToChar("t3_field" + fieldid, 4000);
                                        String dot = "','";
                                        String t0FieldConcat = dialect.concatStr(dot, castCondition, dot);
                                        strResult += " or " + t0FieldConcat + " like '%," + val + ",%'";
                                    } else {
                                        strResult += " or ','+cast(t3_field" + fieldid + " as NVARCHAR)+',' like '%," + val + ",%'";
                                    }
                                }
                                strResult += " ) ";
                            } else {
                                strResult += " and t3_field" + fieldid + "='" + value + "'";
                            }
                        }
                    }
                }

            }

            if (ishead == 0) {
                ishead = 1;
                strResult = " where status != 10";
            } else {
                strResult += " and status != 10";
            }

            if (!classification.equals("")) {
                strResult += " and classification in (" + classification + ")";
            }

            boolean flagaccount = weaver.general.GCONST.getMOREACCOUNTLANDING();//账号类型
            //默认显示的字段 其余都隐藏
            List<String> lsFieldShow = new ArrayList<String>();
            lsFieldShow.add("id");
            lsFieldShow.add("lastname");
            if (!PortalUtil.isuserdeploy()) {
                lsFieldShow.add("subcompanyid1");
            }
            lsFieldShow.add("departmentid");
            lsFieldShow.add("managerid");
            lsFieldShow.add("telephone");
            lsFieldShow.add("mobile");
            //判断是否有管理员同步过显示列定制
            RecordSet recordSet = new RecordSet();
            String showColSql = "select * from cloudstore_defcol where pageUid='920acb49-9262-4553-a9e5-dae1e17ebc89' and userId=0 and display=0 ";
            recordSet.executeQuery(showColSql);
            if (recordSet.getCounts() > 0) {
                lsFieldShow = new ArrayList<String>();
                lsFieldShow.add("id");
            }
            while (recordSet.next()) {
                String dataIndex = recordSet.getString("dataIndex").toLowerCase();
                if (!lsFieldShow.contains(dataIndex)) {
                    lsFieldShow.add(dataIndex);
                }
            }
            writeLog("GetHrmSearchResultCmd>>>lsFieldShow="+ JSON.toJSONString(lsFieldShow));
            String tempstr = strResult;
            AppDetachComInfo adci = new AppDetachComInfo();
            String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID() + "");
            if (!"".equals(tempstr))
                tempstr += (appdetawhere != null && !"".equals(appdetawhere) ? (" and " + appdetawhere) : "");
            else tempstr = appdetawhere;

            String backfields = "*";
            String sqlWhere = " " + tempstr;
            String fromSql = "";
            String tableString = "";
            String operateString = "";
            String orderby = "";
            //默认排序设置 start
            String orderBySql = "select * from HrmSearchOrderByCol where userId=? and sort=1 order by orders";
            recordSet.executeQuery(orderBySql, user.getUID());
            if (recordSet.getCounts() <= 0) {
                orderBySql = "select * from HrmSearchOrderByCol where userId=0 and sort=1 order by orders";
                recordSet.executeQuery(orderBySql);
            }
            while (recordSet.next()) {
                String dataIndex = recordSet.getString("dataIndex");
                String ascOrDesc = recordSet.getString("ascOrDesc");
                if (dataIndex.equals("organization")) {
                    orderby += ",dept.showOrderOfDeptTree " + ascOrDesc;
                }
                if (dataIndex.equalsIgnoreCase("hrmresource.dspOrder") || dataIndex.equalsIgnoreCase("hrmresource.lastName")) {
                    orderby += "," + dataIndex + " " + ascOrDesc;
                } else if (dataIndex.equalsIgnoreCase("dept.deptShowOrder") || dataIndex.equalsIgnoreCase("dept.deptName")) {
                    orderby += "," + dataIndex + " " + ascOrDesc;
                } else if (dataIndex.equalsIgnoreCase("subcom.subcomShowOrder") || dataIndex.equalsIgnoreCase("subcom.subcomName")) {
                    orderby += "," + dataIndex + " " + ascOrDesc;
                }
            }
            orderby = orderby.startsWith(",") ? orderby.substring(1) : orderby;
            orderby = orderby.equals("") ? " hrmresource.dspOrder,hrmresource.id " : orderby;
            //默认排序设置 end
            HrmListValidate HrmListValidate = new HrmListValidate();

            int[] scopeIds;
            String pageId = "";
            String pageUid = "";
            if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                scopeIds = new int[]{-1, 1, 3};
                pageId = PageIdConst.HRM_ResourceSearchResultByManager;
                pageUid = PageUidFactory.getHrmPageUid("SearchResourceManager");
            } else {
                scopeIds = new int[]{-1};
                pageId = PageIdConst.HRM_ResourceSearchResult;
                pageUid = PageUidFactory.getHrmPageUid("SearchResource");
            }

            String tabletype = "none";
            if (HrmValidate.hasEmessage(user)) {
                tabletype = "checkbox";
            }

            PrivacyComInfo pc = new PrivacyComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();

            String popedomOtherpara = String.valueOf(HrmValidate.hasEmessage(user) && HrmListValidate.isValidate(33)) + "_" +//发消息
                    String.valueOf(HrmListValidate.isValidate(19)) + "_" +//发送邮件
                    String.valueOf(HrmListValidate.isValidate(31)) + "_" +//发送短信
                    String.valueOf(String.valueOf(HrmListValidate.isValidate(32))) + "_" +//新建日程
                    String.valueOf(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) + "_" +
                    String.valueOf(user.getUID());
            operateString = "<operates width=\"8%\">";
            operateString += " 	   <popedom async=\"false\" transmethod=\"weaver.hrm.HrmTransMethod.getHrmSearchOperate\" otherpara=\"" + popedomOtherpara + "\"></popedom> ";
            operateString += "     <operate href=\"javascript:sendEmessage();\" text=\"" + SystemEnv.getHtmlLabelName(127379, user.getLanguage()) + "\"  index=\"0\"/>";
            operateString += "	   <operate href=\"javascript:sendMail();\" text=\"" + SystemEnv.getHtmlLabelName(2051, user.getLanguage()) + "\" index=\"1\"/>";
            operateString += "     <operate href=\"javascript:sendSmsMessage();\" text=\"" + SystemEnv.getHtmlLabelName(16635, user.getLanguage()) + "\" index=\"2\"/>";
            operateString += "     <operate href=\"javascript:doAddWorkPlanByHrm();\" text=\"" + SystemEnv.getHtmlLabelName(18481, user.getLanguage()) + "\" index=\"3\"/>";
            //operateString += "     <operate href=\"javascript:addCoWork();\" text=\"" + SystemEnv.getHtmlLabelName(18034, user.getLanguage()) + "\" index=\"3\"/>";
            operateString += "     <operate href=\"javascript:jsHrmResourceSystemView();\" text=\"" + SystemEnv.getHtmlLabelName(15804, user.getLanguage()) + "\" index=\"4\"/>";
            operateString += "</operates>";
            String checkboxpopedompara = "";
            if ("checkbox".equals(tabletype)) {
                checkboxpopedompara += " <checkboxpopedom showmethod=\"weaver.hrm.HrmTransMethod.getEmessageCheckbox\"  id=\"checkbox\"  popedompara=\"column:id+" + user.getUID() + "\" />";
            }

            LinkedHashMap<String, String> ht = new LinkedHashMap<String, String>();
            HrmFieldManager hfm = null;
            HrmFieldGroupComInfo hrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            String[] backfield = new String[]{"id as t1_id", "id as t2_id", "id as t3_id"};

            for (int i = 0; i < scopeIds.length; i++) {
                scopeId = scopeIds[i];
                hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
                hfm.getCustomFields();
                while (hfm.next()) {
                    if (!"1".equals(hrmFieldGroupComInfo.getIsShow(hfm.getGroupid() + "")) || !hfm.isUse()) continue;
                    if (hfm.getHtmlType().equals("6")) continue;//屏蔽附件上传
                    String fieldNameTemp = hfm.getFieldname();
                    if (fieldNameTemp.equals("loginid")) {
                        if (!HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) continue;
                    }
                    if (hfm.isBaseField(fieldNameTemp)) {
//                      if (hfm.getFieldname().indexOf("textfield") != -1) {
//                        ht.put("t" + i + "_" + hfm.getFieldname(), hfm.getLable());
//                      }else{
                        //导入导出不需要显示隐私设置字段
                        if (isExport && mapShowSets != null && mapShowSets.get(fieldNameTemp) != null) {
                            continue;
                        }
                        ht.put(fieldNameTemp, hfm.getLable());
//                      }
                        continue;
                    }else if(isExport){
                        continue;
                    }
                    if (backfield[i].length() > 0) backfield[i] += ",";
                    if (fieldNameTemp.indexOf("field") != -1) {
                        backfield[i] += fieldNameTemp + " as t" + i + "_" + fieldNameTemp;
                    } else {
                        backfield[i] += fieldNameTemp;
                    }
                    if (ht.get(fieldNameTemp) == null) {
                      ht.put("t" + i + "_" + fieldNameTemp, hfm.getLable());
                    }
                }
            }
            //增加后台未开放定义，但是需要显示的列
            if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                ht.put("seclevel", "683");
            }
            fromSql = " from HrmResource "
                    + " left join (SELECT " + backfield[0] + " FROM cus_fielddata WHERE scope='HrmCustomFieldByInfoType' AND scopeId=-1 AND seqorder in (SELECT MAX(t.seqorder) FROM (SELECT seqorder,id FROM cus_fielddata WHERE scope = 'HrmCustomFieldByInfoType' AND scopeid=-1) t GROUP BY t.id)) t1 on hrmresource.id=t1_id "
                    + " left join (SELECT " + backfield[1] + " FROM cus_fielddata WHERE scope='HrmCustomFieldByInfoType' AND scopeId=1 AND seqorder in (SELECT MAX(t.seqorder) FROM (SELECT seqorder,id FROM cus_fielddata WHERE scope = 'HrmCustomFieldByInfoType' AND scopeid=1) t GROUP BY t.id)) t2 on hrmresource.id=t2_id "
                    + " left join (SELECT " + backfield[2] + " FROM cus_fielddata WHERE scope='HrmCustomFieldByInfoType' AND scopeId=3 AND seqorder in (SELECT MAX(t.seqorder) FROM (SELECT seqorder,id FROM cus_fielddata WHERE scope = 'HrmCustomFieldByInfoType' AND scopeid=3) t GROUP BY t.id)) t3 on hrmresource.id=t3_id "
                    + " left join (SELECT id AS dept_id,departmentname AS deptName,showOrder AS deptShowOrder,showOrderOfTree AS showOrderOfDeptTree FROM HrmDepartment) dept on hrmresource.departmentid = dept_id "
                    + " left join (SELECT id AS subcom_id,subcompanyname AS subcomName,showOrder AS subcomShowOrder,showOrderOfTree AS showOrderOfSubComTree FROM HrmSubCompany) subcom on hrmresource.subcompanyid1 = subcom_id ";
            tableString = "";

            String[] realPathArr = null;
            if (needRealPath.length() > 0) {
                realPathArr = needRealPath.split(",");
            }
            StringBuffer colString = new StringBuffer();
            Iterator iter = ht.entrySet().iterator();
            int count = 0;
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String fieldname = ((String) entry.getKey()).toLowerCase();
                String fieldlabel = (String) entry.getValue();
                String display = lsFieldShow.contains(fieldname) ? "true" : "false";
                if (fieldname.equals("dept_id") || fieldname.equals("deptName") || fieldname.equals("deptShowOrder")
                        || fieldname.equals("subcom_id") || fieldname.equals("subcomName") || fieldname.equals("subcomShowOrder")) {
                    //这几个字段只是参与排序的字段，不是显示列
                    continue;
                }
                if (fieldname.equals("accounttype") && flagaccount) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\" text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"accounttype\" tablename=\"hrmresource\" transmethod=\"weaver.general.AccountType.getAccountType\" otherpara=\"" + user.getLanguage() + "\" />");
                } else if (fieldname.equals("lastname")) {
                    colString.append("<col width=\"7%\" labelid=\"547\"  text=\"" + SystemEnv.getHtmlLabelName(547, user.getLanguage()) + "\" column=\"id\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.resource.ResourceComInfo.isOnline\" />");
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\" text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"lastname\" tablename=\"hrmresource\" orderkey=\"lastname\"/>");
                } else if (fieldname.equals("sex")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"sex\" tablename=\"hrmresource\" orderkey=\"sex\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getSexName\" otherpara=\"" + user.getLanguage() + "\" />");
                } else if (fieldname.equals("departmentid")) {
                    String display4Dept = lsFieldShow.contains("departmentid") ? "true" : "false";
                    String display4SubCom = lsFieldShow.contains("subcompanyid1") ? "true" : "false";
                    String showDepartmentFullPath = Util.null2String(Prop.getInstance().getPropValue("Others", "showDepartmentFullPath"));// 是否显示全路径
                    String showSubCompanyFullPath = Util.null2String(Prop.getInstance().getPropValue("Others", "showSubCompanyFullPath"));// 是否显示全路径
                    String showDepartmentFullName = Util.null2String(Prop.getInstance().getPropValue("Others", "showDepartmentFullName"));// 是否显示全称
                    String showSubCompanyFullName = Util.null2String(Prop.getInstance().getPropValue("Others", "showSubCompanyFullName"));//是否显示全称

                    if (virtualtype.startsWith("-")) {
                        if("1".equals(showDepartmentFullPath)){
                            colString.append("<col width=\"10%\" display=\"" + display4Dept + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"departmentid\" tablename=\"hrmresource\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.companyvirtual.ResourceVirtualComInfo.getDepartmentRealPath\" otherpara=\"column:id+" + virtualtype + "\"/>");
                        }else{
                            colString.append("<col width=\"10%\" display=\"" + display4Dept + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"departmentid\" tablename=\"hrmresource\" orderkey=\"departmentid\" transmethod=\"weaver.hrm.companyvirtual.ResourceVirtualComInfo.getDepartmentName\" otherpara=\"column:id+" + virtualtype + "\"/>");
                        }
                        if("1".equals(showSubCompanyFullPath)){
                            colString.append("<col width=\"10%\" display=\"" + display4SubCom + "\" labelid=\"141\"  text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" orderkey=\"subcompanyid1\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.companyvirtual.ResourceVirtualComInfo.getSubcompanyRealPath\" otherpara=\"column:id+" + virtualtype + "\"/>");
                        }else{
                            colString.append("<col width=\"10%\" display=\"" + display4SubCom + "\" labelid=\"141\"  text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" orderkey=\"subcompanyid1\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.companyvirtual.ResourceVirtualComInfo.getSubCompanyName\" otherpara=\"column:id+" + virtualtype + "\"/>");
                        }
                    } else {
                        if (realPathArr != null && Arrays.asList(realPathArr).contains("department") || "1".equals(showDepartmentFullPath)) {//部门全路径
                            colString.append("<col width=\"10%\" display=\"" + display4Dept + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentRealPath\"/>");
                        }else if("1".equals(showDepartmentFullName)){
                            colString.append("<col width=\"10%\" display=\"" + display4Dept + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentname\"/>");
                        }else{
                            colString.append("<col width=\"10%\" display=\"" + display4Dept + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"departmentid\" orderkey=\"departmentid\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.company.DepartmentComInfo.getDepartmentmark\"/>");
                        }

                        if (!PortalUtil.isuserdeploy()) {
                            if (realPathArr != null && Arrays.asList(realPathArr).contains("subcompany") || "1".equals(showSubCompanyFullPath)) {//分部全路径
                                colString.append("<col width=\"10%\" display=\"" + display4SubCom + "\" labelid=\"141\"  text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" orderkey=\"subcompanyid1\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.company.SubCompanyComInfo.getSubcompanyRealPath\"/>");
                            } else if (showSubCompanyFullName.equals("1")) {
                                colString.append("<col width=\"10%\" display=\"" + display4SubCom + "\" labelid=\"141\"  text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" orderkey=\"subcompanyid1\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.company.SubCompanyComInfo.getSubCompanydesc\"/>");
                            } else {
                                colString.append("<col width=\"10%\" display=\"" + display4SubCom + "\" labelid=\"141\"  text=\"" + SystemEnv.getHtmlLabelName(141, user.getLanguage()) + "\" column=\"subcompanyid1\" orderkey=\"subcompanyid1\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.company.SubCompanyComInfo.getSubCompanyname\"/>");
                            }
                        }
                    }
                } else if (fieldname.equals("managerid")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"managerid\" orderkey=\"managerid\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.resource.ResourceComInfo.filterResourcename\" otherpara=\"" + keyfield + "\"/>");
                } else if (fieldname.equals("jobtitle")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\"  text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"jobtitle\" orderkey=\"jobtitle\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.job.JobTitlesComInfo.getJobTitlesname\"/>");
                } else if (fieldname.equals("status")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\" text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"status\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getStatusName\" otherpara=\"" + user.getLanguage() + "\"/>");
                } else if (fieldname.equals("jobactivity")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"1915\" text=\"" + SystemEnv.getHtmlLabelName(1915, Util.getIntValue(user.getLanguage())) + "\" column=\"jobactivity\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.HrmTransMethod.getJobActivitiesname\" otherpara=\"column:jobtitle\"/>");
                } else if (fieldname.equals("jobgroupid")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"805\" text=\"" + SystemEnv.getHtmlLabelName(805, Util.getIntValue(user.getLanguage())) + "\" column=\"jobGroupId\" tablename=\"hrmresource\" transmethod=\"weaver.hrm.HrmTransMethod.getJobGroupName\" otherpara=\"column:jobtitle\"/>");
                } else if (fieldname.equals("seclevel")) {
                    colString.append("<col width=\"10%\" display=\"" + display + "\" labelid=\"683\" text=\"" + SystemEnv.getHtmlLabelName(683, Util.getIntValue(user.getLanguage())) + "\" column=\"seclevel\" tablename=\"hrmresource\" />");
                } else {
                    String width = "10%";
                    if (fieldname.equals("telephone")) {
                        width = "15%";
                    }
                    if (mapShowSets != null && mapShowSets.get(fieldname) != null) {
                        colString.append("<col width=\"" + width + "\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\"   text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"" + fieldname + "\" tablename=\"hrmresource\" orderkey=\"" + fieldname + "\" transmethod=\"weaver.hrm.privacy.PrivacyComInfo.getSearchContent\" otherpara=\"column:id+" + user.getUID() + "+" + fieldname + "\" />");

                    } else {
                        colString.append("<col width=\"" + width + "\" display=\"" + display + "\" labelid=\"" + fieldlabel + "\"   text=\"" + SystemEnv.getHtmlLabelNames(fieldlabel, user.getLanguage()) + "\" column=\"" + fieldname + "\" tablename=\"hrmresource\" orderkey=\"" + fieldname + "\" transmethod=\"weaver.hrm.HrmTransMethod.getDefineContent\" otherpara=\"" + fieldname + ":" + user.getLanguage() + "\"/>");
                    }
                }
            }
            colString.append("<col width=\"10%\" display=\"false\" labelid=\"15513\" text=\"" + SystemEnv.getHtmlLabelName(15513, user.getLanguage()) + "\" column=\"dsporder\" tablename=\"hrmresource\"  />");
            String display = lsFieldShow.contains("orgid") ? "true" : "false";

            colString.append("<col width=\"10%\" display=\""+display+"\" labelid=\"376\"  text=\"" + SystemEnv.getHtmlLabelName(376, user.getLanguage()) + "\" column=\"orgid\" tablename=\"hrmresource\"  orderkey=\"departmentid\" transmethod=\"com.api.hrm.util.HrmTransMethod.getAllParentDepartmentNames\" otherpara=\"column:departmentid\" otherpara2=\"column:subcompanyid1\"/>");
            if (HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit", user)) {
                //工资账号户名
                colString.append("<col width=\"10%\" hide=\"true\" labelid=\"83353\"  text=\"" + SystemEnv.getHtmlLabelName(83353, user.getLanguage()) + "\" column=\"accountname\" tablename=\"hrmresource\"  orderkey=\"accountname\" />");
                //工资银行
                colString.append("<col width=\"10%\" hide=\"true\" labelid=\"15812\"  text=\"" + SystemEnv.getHtmlLabelName(15812, user.getLanguage()) + "\" column=\"bankid1\" tablename=\"hrmresource\"  orderkey=\"bankid1\" transmethod=\"weaver.hrm.HrmTransMethod.getBankName\" />");
                //工资账号
                colString.append("<col width=\"10%\" hide=\"true\" labelid=\"16016\"  text=\"" + SystemEnv.getHtmlLabelName(16016, user.getLanguage()) + "\" column=\"accountid1\" tablename=\"hrmresource\"  orderkey=\"accountid1\"/>");
                //公积金帐户
                colString.append("<col width=\"10%\" hide=\"true\" labelid=\"16085\"  text=\"" + SystemEnv.getHtmlLabelName(16085, user.getLanguage()) + "\" column=\"accumfundaccount\" tablename=\"hrmresource\"  orderkey=\"accumfundaccount\"/>");
            }
            EncryptShareSettingEntity encryptShareSettingEntity = EncryptConfigBiz.getEncryptShareSetting(EncryptMould.HRM.getCode(),"ADDRESSBOOK");
            if(isExport){
                operateString= "";
                operateString = "<operates width=\"20%\">";
                operateString+=" <popedom  ></popedom> ";
                operateString+="     <operateqc text=\""+ SystemEnv.getHtmlLabelName(17416,user.getLanguage())+"\" index=\"0\"/>";
                operateString+="</operates>";
                tabletype="none";
                checkboxpopedompara = " <checkboxpopedom  id=\"checkbox\" showmethod=\"weaver.general.KnowledgeTransMethod.getDirCheckbox\" popedompara = \"column:id\" />";

            }

            //qc2474652这里对于人员通讯录数据进行过滤，只能显示人员在建模表维护的部门
            String isCustom = Util.null2String(params.get("custom"));
            HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
            if (user.getUID() != 1){
                if ("1".equals(isCustom)){
                    Boolean isHave = hrmVarifyCustom.checkUserRight(user);
                    if (isHave){
                        String department = hrmVarifyCustom.getDepartment(user,false);
                        //qc2374652 这里把用户所在的部门也加入到过滤条件中
                        department += ","+user.getUserDepartment();
                        String sqlCustom =" and dept_id in("+department+") ";
                        sqlWhere += sqlCustom;
                    }else {
                        sqlWhere += " and 1=2 ";
                    }

                }
            }

            String sql = "SELECT " + backfields + " " + fromSql + " " + sqlWhere + " ORDER BY " + orderby;
            new BaseBean().writeLog("==zj==(人员通讯录展示)" + JSON.toJSONString(sql));
            tableString = " <table tabletype=\"" + tabletype + "\" pageId=\"" + pageId + "\" pageUid=\"" + pageUid + "\" exportRight=\"HrmResourceInfo:Import\" isEncryptShare=\""+encryptShareSettingEntity.getIsEnable()+"\" pagesize=\"" + PageIdConst.getPageSize(pageId, user.getUID(), PageIdConst.HRM) + "\" >" +
                    checkboxpopedompara +
                    "	   <sql backfields=\"" + backfields + "\" sqlform=\"" + fromSql + "\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlWhere) + "\"  sqlorderby=\"" + orderby + "\"  sqlprimarykey=\"hrmresource.id\" sqlsortway=\"Asc\" sqlisdistinct=\"false\"/>" +
                    operateString +
                    "			<head>"
                    + colString.toString()
                    + "			</head>"
                    + " </table>";

            writeLog("#$sql is _________________"+sql);
            String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
            if (request != null) {
                request.getSession().setAttribute("HrmResourceSearchResultExcelSql", sql);
            }
            Util_TableMap.setVal(sessionkey, tableString);
            retmap.put("sessionkey", sessionkey);
        } catch (Exception e) {
            retmap.put("api_status", false);
            retmap.put("api_errormsg", e.getMessage());
            e.printStackTrace();
        }
        return retmap;
    }
}
