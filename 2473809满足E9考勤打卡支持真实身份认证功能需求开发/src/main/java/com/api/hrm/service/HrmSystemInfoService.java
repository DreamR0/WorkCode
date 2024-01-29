package com.api.hrm.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.api.hrm.util.ServiceUtil;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateAuditType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.hrm.util.HrmWeakPasswordUtil;
import com.weaver.integration.ldap.sync.oa.OaSync;
import com.weaver.integration.ldap.util.AuthenticUtil;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import ln.LN;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.ConditionType;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.conn.RecordSetTrans;
import weaver.encrypt.EncryptUtil;
import weaver.file.Prop;
import weaver.general.*;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.autotask.domain.HrmUsbAutoDate;
import weaver.hrm.autotask.manager.HrmUsbAutoDateManager;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.passwordprotection.manager.HrmResourceManager;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.schedule.manager.HrmScheduleManager;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.interfaces.hrm.HrmServiceManager;
import weaver.license.PluginUserCheck;
import weaver.login.VerifyPasswdCheck;
import weaver.meeting.MeetingUtil;
import weaver.rsa.security.RSA;
import weaver.rtx.OrganisationCom;
import weaver.rtx.OrganisationComRunnable;
import weaver.systeminfo.SysMaintenanceLog;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.sysadmin.HrmResourceManagerDAO;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import weaver.hrm.common.pattern.PatternUtil4Hrm;

import weaver.hrm.loginstrategy.LoginStrategyComInfo;
import weaver.hrm.loginstrategy.style.LoginStrategy;

/***
 * 系统信息
 * @author lvyi
 *
 */
public class HrmSystemInfoService extends BaseBean {

    /**
     * 系统信息
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> getHrmSystemInfoForm(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        List<Object> lsGroup = new ArrayList<Object>();
        Map<String, Object> groupitem = null;
        List<Object> itemlist = null;

        try {
            User user = HrmUserVarify.getUser(request, response);
            AuthenticUtil au = new AuthenticUtil();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
            ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();
            String passwordComplexity = settings.getPasswordComplexity();
            String openPasswordLock = settings.getOpenPasswordLock();
            int minpasslen = settings.getMinPasslen();
            int needdynapass = settings.getNeeddynapass();
            String needusbHt = settings.getNeedusbHt();
            needusbHt = "0";
            String needusbDt = settings.getNeedusbDt();
            String mobileScanCA = settings.getMobileScanCA();
            String qrCode = settings.getQRCode();

            String id = Util.null2String(request.getParameter("id"));
            int hrmid = user.getUID();
            if (id.length() == 0) {
                id = "" + user.getUID();
            }

            StaticObj staticobj = StaticObj.getInstance();
            String multilanguage = (String) staticobj.getObject("multilanguage");
            if (multilanguage == null) multilanguage = "n";
            boolean isMultilanguageOK = false;
            if (multilanguage.equals("y")) isMultilanguageOK = true;

						boolean iss = ResourceComInfo.isSysInfoView(hrmid, id);
            
            HttpSession session = request.getSession(true);
            //人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
            int hrmdetachable = 0;
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

            /**
             * 如果开启了管理分权，则判断的是是否对改人员有编辑权限，如果没有开启管理分权，则判断该人员是否有系统信息维护权限
             */
            int hrmoperatelevel = -1;
            boolean isright = false;
            if (hrmdetachable == 1) {
                String deptid = ResourceComInfo.getDepartmentID(id);
                String subcompanyid = DepartmentComInfo.getSubcompanyid1(deptid);
                hrmoperatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "ResourcesInformationSystem:All", Util.getIntValue(subcompanyid));
            } else {
                String departmentidtmp = ResourceComInfo.getDepartmentID(id);
                if (HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user, departmentidtmp)) {
                    hrmoperatelevel = 2;
                }
            }
            boolean isSelf = false;//查看的是否是自己的系统信息
            if (id.equals("" + user.getUID())) {
                isSelf = true;
            }
            /**
             * isSelf:查看的是否是自己的系统信息
             * iss：
             * hrmoperatelevel:0-只读、1-编辑、2-完全控制
             * isHasRight:是否是【入职维护人设置】中的【系统信息-负责人】
             */
            if (!(isSelf || iss || hrmoperatelevel >= 0 || isHasRight(user))) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return retmap;
            }

            if (iss || hrmoperatelevel > 0) {
                isright = true;
            }

            String loginid = "";
            String sql = "";
            RecordSet rs = new RecordSet();
            boolean hasFieldvalue = false;
            sql = "select * from HrmResource where id = " + id;
            rs.executeSql(sql);
            if (rs.next()) {
                hasFieldvalue = true;
                loginid = rs.getString("loginid");
            }

            boolean canSave = false;
            LN ln = new LN();
            int ckHrmnum = ln.CkHrmnum();
            if (ckHrmnum < 0) {//只有License检查人数小于规定人数，才能修改。防止客户直接修改数据库数据
                canSave = true;
            } else if (ckHrmnum == 0 && !loginid.trim().equals("")) {
                canSave = true;//如果正好license人数到头，而且当前的登录名是空，那么就不让修改登录名
            }


            if ((isright && canSave) || isHasRight(user)) {
                Map<String, Object> buttons = new Hashtable<String, Object>();
                buttons.put("hasEdit", true);
                retmap.put("buttons", buttons);
            }

            String userUsbType = "";
            //登录名、密码、密码确认、辅助校验方式、系统语言、电子邮件、安全级别
            String[] fields = new String[]{"loginid,412,1,1", "passwordlock,130547,4,2", "password,409,1,4",
                    "password1,501,1,4", "mobilecaflag,385000,4,2", "userUsbType,81629,5,1", "usbstate,602,5,1",
                    "tokenKey,32897,1,1", "serial,21597,1,1", "startUsing,131908,5,1",
                    "seclevel,683,1,2", "useSecondaryPwd,388412,4,2", "isADAccount,81932,4,2"};//"systemlanguage,16066,3,259",
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            SearchConditionItem searchConditionItem = null;
            HrmFieldBean hrmFieldBean = null;
            itemlist = new ArrayList<Object>();
            groupitem = new HashMap<String, Object>();
            groupitem.put("title", SystemEnv.getHtmlLabelName(15804, user.getLanguage()));
            groupitem.put("defaultshow", true);

            LoginStrategyComInfo loginStrategyComInfo = new LoginStrategyComInfo() ;

            for (int i = 0; i < fields.length; i++) {
                String[] fieldinfo = fields[i].split(",");
                if (!isMultilanguageOK && fieldinfo[0].equals("systemlanguage")) continue;
                //if (!"1".equals(openPasswordLock) && fieldinfo[0].equals("passwordlock")) continue;
                if (!"1".equals(mobileScanCA) && fieldinfo[0].equals("mobilecaflag")) continue;
                if (!au.checkType(loginid) && fieldinfo[0].equals("isADAccount")) continue;
                if (fieldinfo[0].equals("userUsbType")) {
                    if ((needdynapass == 1 || "1".equals(needusbHt) || "1".equals(needusbDt)) || loginStrategyComInfo.isStrategyOpen()|| "1".equals(qrCode)) {
                    } else {
                        continue;
                    }
                }

                hrmFieldBean = new HrmFieldBean();
                hrmFieldBean.setFieldname(fieldinfo[0]);
                hrmFieldBean.setFieldlabel(fieldinfo[1]);
                hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
                hrmFieldBean.setType(fieldinfo[3]);
                hrmFieldBean.setFieldvalue(hasFieldvalue ? Util.null2String(rs.getString(fieldinfo[0])) : "");
                hrmFieldBean.setIsFormField(true);
                if (hrmFieldBean.getFieldhtmltype().equals("1") || hrmFieldBean.getType().equals("1")) {
                    hrmFieldBean.setMultilang(false);
                }
                if (hrmFieldBean.getType().equals("4")) {
                    if (Util.null2String(rs.getString("loginid")).length() > 0
                            || Util.null2String(rs.getString("password")).length() > 0) {
                        hrmFieldBean.setFieldvalue("qwertyuiop");
                    }
                }

                if (hrmFieldBean.getFieldhtmltype().equals("4") && hrmFieldBean.getFieldvalue().equals("")) {
                    hrmFieldBean.setFieldvalue("0");
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
                        }else if (passwordComplexity.equals("3")) {
                            title = SystemEnv.getHtmlLabelName(512563, user.getLanguage());
                        }
                        otherParams.put("tip", title);
                        otherParams.put("tipLength", "100");
                        if (!passwordComplexity.equals("0")) {
                            otherParams.put("passwordStrengthIdx", 0);
                            otherParams.put("passwordStrength", true);
                        }
                    }
                    searchConditionItem.setOtherParams(otherParams);
                } else if (hrmFieldBean.getFieldhtmltype().equals("5")) {
                    List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
                    if (hrmFieldBean.getFieldname().equals("userUsbType")) {
                        userUsbType = "" + hrmFieldBean.getFieldvalue();
                        statusOptions.add(new SearchConditionOption("", "", hrmFieldBean.getFieldvalue().equals("") ? true : false));
                        if (needdynapass == 1) {
                            statusOptions.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(32511, user.getLanguage()), hrmFieldBean.getFieldvalue().equals("4") ? true : false));
                        }
                        if ("1".equals(needusbHt)) {
                            statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21589, user.getLanguage()), hrmFieldBean.getFieldvalue().equals("2") ? true : false));
                        }
                        if ("1".equals(needusbDt)) {
                            statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32896, user.getLanguage()), hrmFieldBean.getFieldvalue().equals("3") ? true : false));
                        }
                        if ("1".equals(qrCode)) {
                            statusOptions.add(new SearchConditionOption("6", SystemEnv.getHtmlLabelName(	526336, user.getLanguage()), hrmFieldBean.getFieldvalue().equals("6") ? true : false));
                        }
                        String fieldValue = Util.null2String(hrmFieldBean.getFieldvalue()) ;

                        for(LoginStrategy strategy : loginStrategyComInfo.listOpenStrategy()){
                            String userTypeStr = String.valueOf(strategy.getUserType()) ;
                            statusOptions.add(new SearchConditionOption(userTypeStr
                                    ,SystemEnv.getHtmlLabelName(strategy.getLabel(),user.getLanguage())
                                    ,userTypeStr.equals(fieldValue))) ;
                        }

                        searchConditionItem.setOptions(statusOptions);
                    } else if (hrmFieldBean.getFieldname().equals("usbstate")) {
                        String usbstatetemp = Util.null2String(hrmFieldBean.getFieldvalue());
                        statusOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(18095, user.getLanguage()), usbstatetemp.length() > 0 ? (usbstatetemp.equals("0") ? true : false):true));
                        statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(18096, user.getLanguage()), usbstatetemp.equals("1") ? true : false));
                        statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(21384, user.getLanguage()), usbstatetemp.equals("2") ? true : false));
                        searchConditionItem.setOptions(statusOptions);
                    }
                }
                if (!passwordComplexity.equals("0") && !searchConditionItem.getConditionType().equals(ConditionType.DATE)) {
                    searchConditionItem.setLabelcol(6);
                    searchConditionItem.setFieldcol(15);
                }

                if (!passwordComplexity.equals("0") && (fieldinfo[0].equals("password") || fieldinfo[0].equals("password1"))) {
                    searchConditionItem.setLabelcol(6);
                    searchConditionItem.setFieldcol(9);
                }
                if (searchConditionItem.getBrowserConditionParam() != null) {
                    searchConditionItem.getBrowserConditionParam().setViewAttr(1);
                }
                searchConditionItem.setViewAttr(1);
                itemlist.add(searchConditionItem);
            }

            /*验证码*/
            hrmFieldBean = new HrmFieldBean();
            hrmFieldBean.setFieldname("validatecode");//验证码
            hrmFieldBean.setFieldlabel("22910");
            hrmFieldBean.setFieldhtmltype("1");
            hrmFieldBean.setType("1");
            hrmFieldBean.setViewAttr(3);
            searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
            searchConditionItem.setRules("required|string");
            itemlist.add(searchConditionItem);

            /**
             * 人员密级
             * 具有系统信息维护权限的人可以编辑保存此字段
             * 自己不能编辑自己的人员密级字段
             */
            boolean isOpenClassification = HrmClassifiedProtectionBiz.isOpenClassification();//判断是否开启了分级保护
            if (isOpenClassification) {
                hrmFieldBean = new HrmFieldBean();
                hrmFieldBean.setFieldname("classification");//人员密级
                hrmFieldBean.setFieldlabel("130506");
                hrmFieldBean.setFieldhtmltype("5");
                hrmFieldBean.setType("1");
                hrmFieldBean.setIsFormField(true);
                hrmFieldBean.setViewAttr(1);
                searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                List statusOptions = new ArrayList<SearchConditionOption>();
                HrmClassifiedProtectionBiz hrmClassifiedProtectionBiz = new HrmClassifiedProtectionBiz();
                statusOptions = hrmClassifiedProtectionBiz.getOptionListByUserId(id,""+user.getLanguage());
                searchConditionItem.setOptions(statusOptions);
                itemlist.add(searchConditionItem);
            }

            groupitem.put("items", itemlist);
            lsGroup.add(groupitem);
            retmap.put("condition", lsGroup);
            retmap.put("hrmId", user.getUID());

            String defaultDate = TimeUtil.dateAdd(TimeUtil.getCurrentDateString(), 1);
            Map automap = new HashMap();
            automap.put("userId", id);
            HrmUsbAutoDate autoDate = new HrmUsbAutoDateManager().get(automap);
            String needauto = "1";
            String enableDate = "";
            String enableUsbType = userUsbType;
            if (autoDate != null) {
                needauto = String.valueOf(autoDate.getNeedAuto());
                enableDate = autoDate.getEnableDate();
                enableUsbType = String.valueOf(autoDate.getEnableUsbType());
            }
            retmap.put("needauto", needauto);
            retmap.put("enableDate", enableDate.length() == 0 ? defaultDate : enableDate);
            retmap.put("enableUsbType", enableUsbType);

            //密码设置相关
            Map<String, Object> setting = new HashMap<String, Object>();
            setting.put("passwordComplexity", passwordComplexity);
            setting.put("minpasslen", minpasslen);
            retmap.put("settings", setting);
            retmap.put("isSelef",isSelf);//自己不能编辑自己的人员密级
            //是否开启了RSA加密
            String openRSA = Util.null2String(Prop.getPropValue("openRSA","isrsaopen"));
            retmap.put("openRSA",openRSA);
        } catch (Exception e) {
            retmap.put("api_status", false);
            retmap.put("api_errormsg", e.getMessage());
            e.printStackTrace();
        }
        return retmap;
    }

    /**
     * 保存系统信息
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> save(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            RecordSet rs = new RecordSet();
            RecordSet RecordSetDB = new RecordSet();
            RecordSet rsdb2 = new RecordSet();
            SysMaintenanceLog SysMaintenanceLog = new SysMaintenanceLog();
            boolean canEdit = HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user);
            if (isHasRight(user)) canEdit = true;
            if (!canEdit) {
                retmap.put("status", "-1");
                retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620, ThreadVarLanguage.getLang())+"");
                return retmap;
            }

            //RSA加密 start
            String newPassword = Util.null2String(request.getParameter("password"));
            String newPassword1 = Util.null2String(request.getParameter("password1"));
            //是否开启了RSA加密
            boolean isOpenRSA = "1".equals(Prop.getPropValue("openRSA","isrsaopen"));
            List<String> passwordList = new ArrayList<String>();
            if (isOpenRSA) {
                passwordList.add(newPassword);
                passwordList.add(newPassword1);

                RSA rsa = new RSA();
                List<String> resultList = rsa.decryptList(request,passwordList) ;
                newPassword = resultList.get(0);
                newPassword1 = resultList.get(1);
            }
            //RSA加密 end

            /*验证码是否正确 start*/
            String validatecode = Util.null2String(request.getParameter("validatecode"));
            String validateRand = Util.null2String((String) request.getSession(true).getAttribute("validateRand"));
            request.getSession(true).removeAttribute("validateRand");
            if (!validateRand.toLowerCase().equals(validatecode.trim().toLowerCase()) || "".equals(validatecode.trim().toLowerCase())) {
                retmap.put("message", SystemEnv.getHtmlLabelName(10000304, Util.getIntValue(user.getLanguage())));
                retmap.put("status", "-1");
                return retmap;
            }
            /*验证码是否正确 end*/

            String id = Util.null2String(request.getParameter("id"));
            if (id.length() == 0) {
                id = "" + user.getUID();
            }

            SimpleBizLogger logger = new SimpleBizLogger();
            Map<String, Object> params = ParamUtil.request2Map(request);
            BizLogContext bizLogContext = new BizLogContext();
            bizLogContext.setLogType(BizLogType.HRM);//模块类型
            bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
            bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(15804, user.getLanguage()));//所属大类型
            bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_SYSTEM);//当前小类型
            bizLogContext.setOperateAuditType(BizLogOperateAuditType.WARNING);
            bizLogContext.setParams(params);//当前request请求参数
            logger.setUser(user);//当前操作人
            String mainSql = "select * from hrmresource where id=" + id;
            logger.setMainSql(mainSql, "id");//主表sql
            logger.setMainPrimarykey("id");//主日志表唯一key
            logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
            logger.before(bizLogContext);//写入操作前日志

            String subcompanyid = "";
            rs.executeSql("select Subcompanyid1 from hrmresource where id = " + id);
            if (rs.next()) {
                subcompanyid = Util.null2String(rs.getString("Subcompanyid1"));
            }

            if (!Util.null2String(request.getParameter("loginid")).equals("") && !subcompanyid.equals("0") && new HrmResourceManager().noMore(subcompanyid)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(81926, user.getLanguage()));
                return retmap;
            }
            //人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
            HttpSession session = request.getSession(true);
            ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
            int hrmdetachable = 0;
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
            int hrmoperatelevel = -1;
            if (hrmdetachable == 1) {
                String deptid = ResourceComInfo.getDepartmentID(id);
                subcompanyid = DepartmentComInfo.getSubcompanyid1(deptid);
                hrmoperatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "ResourcesInformationSystem:All", Util.getIntValue(subcompanyid));
            } else {
                String departmentidtmp = ResourceComInfo.getDepartmentID(id);
                if (HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user, departmentidtmp)) {
                    hrmoperatelevel = 2;
                }
            }
            if (isHasRight(user)) {
                hrmoperatelevel = 2;
            }
            if (!(hrmoperatelevel > 0)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return retmap;
            }
            String password = Util.null2String(newPassword);

            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings1 = reminder.getRemindSettings();

            //判断是否开启了【禁止弱密码保存】
            String weakPasswordDisable = Util.null2s(settings1.getWeakPasswordDisable(), "0");
            if (weakPasswordDisable.equals("1")) {
                if (password.equals("qwertyuiop")) {//未修改密码的情况

                } else {
                    //判断是否为弱密码
                    HrmWeakPasswordUtil hrmWeakPasswordUtil = new HrmWeakPasswordUtil();
                    if (hrmWeakPasswordUtil.isWeakPsd(password)) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(515420, user.getLanguage()));
                        return retmap;
                    }
                }
            }

            String passwordComplexity = settings1.getPasswordComplexity();
            int minpasslen = settings1.getMinPasslen();
            if (password.length() < minpasslen && !password.equals("qwertyuiop")) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(20172, user.getLanguage()) + minpasslen);
                return retmap;
            }

            if ("1".equals(passwordComplexity)) {
                if (password.equals("qwertyuiop")) {//未修改密码的情况

                } else {
                    if (!PatternUtil4Hrm.isPasswordComplexity1(password)) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(	512768, user.getLanguage()) + minpasslen);
                        return retmap;
                    }
                }
            } else if ("2".equals(passwordComplexity)) {
                if (password.equals("qwertyuiop")) {//未修改密码的情况

                } else {
                    if (!PatternUtil4Hrm.isPasswordComplexity2(password)) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(512769, user.getLanguage()) + minpasslen);
                        return retmap;
                    }
                }
            }
            else if ("3".equals(passwordComplexity)) {
                if (password.equals("qwertyuiop")) {//未修改密码的情况

                } else {
                    if (!PatternUtil4Hrm.isPasswordComplexity3(password)) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(512767, user.getLanguage()));
                        return retmap;
                    }
                }
            }
            String loginid = Util.null2String(request.getParameter("loginid"));
            String isADAccount = Util.null2String(request.getParameter("isADAccount"));
            if(isADAccount.equals("1")){
                if(!new AuthenticUtil().checkUserExistInAD(loginid)){
                    retmap.put("status", "-1");
                    retmap.put("message", SystemEnv.getHtmlLabelName(10000346, Util.getIntValue(user.getLanguage())));
                    return retmap;
                }

                if(!"".equals(loginid)&&!password.equals("qwertyuiop")){
                    Map<String,String> map = new HashMap<>();
                    map.put("userid",id);
                    map.put("loginid",loginid);
                    map.put("password",Util.null2String(newPassword));
                    map.put("issysadmin", "true");  //没有旧密码校验  直接返回true即可
                    if(!new OaSync("", "").modifyADPWD(map)){
                        retmap.put("message", SystemEnv.getHtmlLabelName(	31436, user.getLanguage()));
                        retmap.put("status", "-1");
                        return retmap;
                    }
                }
            }


            String logintype = user.getLogintype(); //当前用户类型  1: 类别用户  2:外部用户
            boolean iss = ResourceComInfo.isSysInfoView(user.getUID(), id);
            hrmdetachable = Util.getIntValue(String.valueOf(session.getAttribute("hrmdetachable")), 0);
            int operatelevel = 0;
            if (hrmdetachable == 1 && user.getUID() != 1) {
                String deptid = ResourceComInfo.getDepartmentID(id);
                subcompanyid = DepartmentComInfo.getSubcompanyid1(deptid);
                if (subcompanyid == null || "".equals(subcompanyid)) {
                    rs.executeSql("select Subcompanyid1 from hrmresource where id = " + id);
                    if (rs.next()) {
                        subcompanyid = Util.null2String(rs.getString("Subcompanyid1"));
                    }
                }
                if (subcompanyid != null && !"".equals(subcompanyid)) {
                    operatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "ResourcesInformationSystem:All", Integer.parseInt(subcompanyid));
                }
            } else {
                if (HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user)) {
                    operatelevel = 2;
                }
            }
            if (isHasRight(user)) operatelevel = 2;
            if ("2".equals(logintype) || !(iss || operatelevel > 0)) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return retmap;
            }

            int passwordlock = 0;
            if (Util.null2String(request.getParameter("passwordlock")).equals("true") ||
                    Util.null2String(request.getParameter("passwordlock")).equals("1")) {
                passwordlock = 1;
            }
            //int passwordlock = Util.getIntValue(Util.null2String(request.getParameter("passwordlock")), 0);
            if (logintype.equals("1") && !loginid.equalsIgnoreCase("")) {//如果是内部用户，且本次的loginid不为空
                String oldLoginId = "";
                rs.executeSql("select loginid from HrmResource where id =" + id);//查询这个用户之前的loginid
                if (rs.next()) {
                    oldLoginId = Util.null2String(rs.getString(1));
                }
                if ("".equals(oldLoginId)) {//如果旧loginid为空，检查license数量
                    LN ln = new LN() ;
                    ln.reloadLicenseInfo();
                    if (ln.CkHrmnum() >= 0) {
                        retmap.put("status", "-1");
                        retmap.put("message", SystemEnv.getHtmlLabelName(10000347, Util.getIntValue(user.getLanguage())));
                        return retmap;
                    }
                }
            }

            String enc_account = "";
            if (!loginid.equals("")) {
                enc_account = Util.getEncrypt(loginid);
            }
            if (!password.equals("qwertyuiop") && !isADAccount.equals("1")) {
				String orgpassword = password ;
                String[] passwordnewArr = PasswordUtil.encrypt(password);
                password = passwordnewArr[0];
                String newSalt = passwordnewArr[1];
                RecordSet passwordRecodset = new RecordSet() ;
                passwordRecodset.executeUpdate("update hrmresource set password=?,salt=? where id=?",password,newSalt,id) ;
                passwordRecodset.executeUpdate("update hrmresourcemanager set password=?,salt=? where id=?",password,newSalt,id) ;
                HrmFaceCheckManager.setUserPassowrd(id,orgpassword);
            } else {
                password = "0";
            }
            String systemlanguage = Util.null2String(request.getParameter("systemlanguage"));
            String old_needusb = "0";
            rs.executeQuery("select needusb,systemlanguage from HrmResource where id =" + id);
            if (rs.next()) {
                systemlanguage = Util.null2String(rs.getString(1));
                old_needusb = String.valueOf(rs.getInt("needusb"));
                if (!old_needusb.equals("1")) {
                    old_needusb = "0";
                }
            }

            if (systemlanguage.equals("") || systemlanguage.equals("0")) {
                systemlanguage = "7";
            }
            int seclevel = Util.getIntValue(request.getParameter("seclevel"), 0);
            String needdynapass = String.valueOf(Util.null2String(request.getParameter("needdynapass")));
            String passwordstate = String.valueOf(Util.null2String(request.getParameter("passwordstate")));
            if (passwordstate.equals("0") || passwordstate.equals("2")) {
                needdynapass = "1";
            } else {
                needdynapass = "0";
            }

            //Start 手机接口功能 by alan
            rs.executeSql("DELETE FROM workflow_mgmsusers WHERE userid=" + id);
            if (!Util.null2String(request.getParameter("isMgmsUser")).equals("")) {
                rs.executeSql("INSERT INTO workflow_mgmsusers(userid) VALUES (" + id + ")");
            }
            //End 手机接口功能 by alan

            //xiaofeng
            String needusb = Util.null2String(request.getParameter("needusb"));
            if (!needusb.equals("1"))
                needusb = "0";

            String serial = Util.null2String(request.getParameter("serial"));
            String userUsbType = Util.null2String(request.getParameter("userUsbType"));
            String mobilecaflag = Util.null2String(request.getParameter("mobilecaflag"), "0");
            if (!mobilecaflag.equals("1"))
                mobilecaflag = "0";
            String useSecondaryPwd = Util.null2String(request.getParameter("useSecondaryPwd"), "0");
            if (!useSecondaryPwd.equals("1"))
                useSecondaryPwd = "0";
            String tokenkey = Util.null2String(request.getParameter("tokenKey"));

            if ((needusb.equals("1") && old_needusb.equals("1") && serial.equals("")) || (!needusb.equals("1") && !old_needusb.equals("1")))
                serial = "0"; //如果该用户的序列号不做变更

            String usbstate = Util.null2String(request.getParameter("usbstate"));
            StringBuffer upSql = new StringBuffer("update HrmResource set");
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
                upSql.append(" needusb = ").append(needusb).append(" ,usbstate = ").append(usbstate).append(" ,needdynapass = ").append(needdynapass).append(" ,userUsbType = ").append(userUsbType).append(" where id = ").append(id);
            } else {
                upSql.append(" usbstate = ").append(usbstate.length() == 0 ? "null" : usbstate).append(" ,userUsbType = ").append(userUsbType.length() == 0 ? "null" : userUsbType).append(" where id = ").append(id);
            }
            rs.executeSql(upSql.toString());

            String oldLoginid = "";
            String oldPassword = "";
            if ("1".equals(isADAccount)) {//修改AD账户的密码不同步到人员信息表中,所以把旧密码赋给password
                String sql = "select loginid, password from hrmresource where id=" + id;
                rs.executeQuery(sql);
                if (rs.next()) {
                    oldLoginid = rs.getString("loginid");
                    password = rs.getString("password");
                }
            }
            String para = "";
            char separator = Util.getSeparator();
            para = "" + id + separator + loginid + separator + password + separator + systemlanguage + separator + seclevel + separator + needusb + separator + serial + separator + "" + separator + enc_account + separator + needdynapass + separator + passwordstate;
            ResourceComInfo.setTofirstRow();
            HrmResourceManagerDAO dao = new HrmResourceManagerDAO();
            if (!loginid.equals("") && dao.ifHaveSameLoginId(loginid, id)) { //检测loginid重名
                rs.executeQuery("select id from hrmresourceallview where loginid='" + loginid + "'");
                if (rs.next()) {
                    String theid = rs.getString("id");
                    retmap.put("status", "-1");
                    retmap.put("message", SystemEnv.getHtmlLabelName(10000348, Util.getIntValue(user.getLanguage())) + "(loginid:" + loginid + " lastname:" + ResourceComInfo.getLastname(theid) + ")");
                    return retmap;
                }

            } else {
                if ("".equals(oldLoginid) && !"".equals(loginid)) { //使用登陆名的情况下，将会在Rtx中新增一用户
                    if (!loginid.equals(oldLoginid)) {//修改用户名的情况下新添加用户
                        new Thread(new OrganisationComRunnable("user", "del2", id)).start();
                    }
                }
                //修改用户名的情况下Rtx中去除这一用户用户
                if (!"".equals(oldLoginid) && !"".equals(loginid) && !oldLoginid.equals(loginid)) {
                    new Thread(new OrganisationComRunnable("user", "del2", id)).start();
                }

                boolean ret = false;
                RecordSetTrans rst = new RecordSetTrans();
                rst.setAutoCommit(false);
                try {

                    ret = rst.executeProc("HrmResourceSystemInfo_Insert", para);
                    //保存指定的usbType和tokenkey
                    if (userUsbType.equals("2")) {
                        rst.execute("update hrmresource set userUsbType=" + userUsbType + ",serial='" + (serial.equals("0") ? "" : serial) + "' where id=" + id);
                    } else if (userUsbType.equals("3")) {
                        rst.execute("update hrmresource set userUsbType=" + userUsbType + ",tokenkey='" + tokenkey + "' where id=" + id);
                    }
                    rst.execute("update hrmresource set mobilecaflag=" + mobilecaflag + ",useSecondaryPwd=" + useSecondaryPwd + " where id=" + id);

                    if (user.getLoginid().equals(loginid)) {
                        String languid = String.valueOf(systemlanguage);
                        Cookie syslanid = new Cookie("Systemlanguid", languid);
                        syslanid.setMaxAge(-1);
                        syslanid.setPath("/");
                        response.addCookie(syslanid);
                    }
                    rst.commit();
                } catch (Exception e) {
                    rst.rollback();
                    e.printStackTrace();
                }
                VerifyPasswdCheck VerifyPasswdCheck = new VerifyPasswdCheck();
                VerifyPasswdCheck.unlockOrLockPassword(id, passwordlock);
                if (ret) {
                    if (needdynapass.equals("1")) {
                        rs.executeSql("select id from hrmpassword where id='" + id + "'");
                        if (rs.next())
                            ;
                        else {
                            rs.executeSql("insert into hrmpassword(id,loginid) values(" + id + ",'" + loginid + "')");
                        }
                    }

                } else {

                }
            }
            if (RecordSetDB.getDBType().equals("db2")) {
                rsdb2.executeProc("Tri_UMMInfo_ByHrmResource", "" + id); //主菜单
                String managerid = ResourceComInfo.getManagerID(id);
                String sql = "select managerstr from HrmResource where id = " + Util.getIntValue(managerid);
                rs.executeSql(sql);
            }
            int userid = user.getUID();
            Calendar todaycal = Calendar.getInstance();
            String today = Util.add0(todaycal.get(Calendar.YEAR), 4) + "-" + Util.add0(todaycal.get(Calendar.MONTH) + 1, 2) + "-" + Util.add0(todaycal.get(Calendar.DAY_OF_MONTH), 2);
            String userpara = "" + userid + separator + today;
            rs.executeProc("HrmResource_ModInfo", "" + id + separator + userpara);
            rs.executeProc("HrmInfoStatus_UpdateSystem", "" + id);

            para = "" + id + separator + loginid + separator + "1";
            rs.executeProc("Ycuser_Insert", para);

            para = "" + seclevel + separator + ResourceComInfo.getDepartmentID(id) + separator + DepartmentComInfo.getSubcompanyid1(ResourceComInfo.getDepartmentID(id)) + separator + id;
            rs.executeProc("MailShare_InsertByUser", para);
            if (!old_needusb.equals("1") && needusb.equals("1")) {
                SysMaintenanceLog.resetParameter();
                SysMaintenanceLog.setRelatedId(Util.getIntValue(id));
                SysMaintenanceLog.setRelatedName(ResourceComInfo.getResourcename(id));
                SysMaintenanceLog.setOperateItem("89");
                SysMaintenanceLog.setOperateUserid(user.getUID());
                SysMaintenanceLog.setClientAddress(request.getRemoteAddr());
                SysMaintenanceLog.setOperateType("7");
                SysMaintenanceLog.setOperateDesc("HrmResourceSystemInfo_USB");
                SysMaintenanceLog.setSysLogInfo();
            }
            if (old_needusb.equals("1") && !needusb.equals("1")) {
                SysMaintenanceLog.resetParameter();
                SysMaintenanceLog.setRelatedId(Util.getIntValue(id));
                SysMaintenanceLog.setRelatedName(ResourceComInfo.getResourcename(id));
                SysMaintenanceLog.setOperateItem("89");
                SysMaintenanceLog.setOperateUserid(user.getUID());
                SysMaintenanceLog.setClientAddress(request.getRemoteAddr());
                SysMaintenanceLog.setOperateType("8");
                SysMaintenanceLog.setOperateDesc("HrmResourceSystemInfo_USB");
                SysMaintenanceLog.setSysLogInfo();
            }

            //更新人员密级start
            String classification = Util.null2String(request.getParameter("classification"));//人员密级
            //自己不能更改自己的人员密级
            if (!id.equals("" + user.getUID())) {
                String classificationSql = "update HrmResource set classification=?,encKey=?, crc = ? where id=?";
                Map<String,String> crcInfo = new EncryptUtil().getLevelCRC(id,classification);
                String encKey = Util.null2String(crcInfo.get("encKey"));
                String crc = Util.null2String(crcInfo.get("crc"));
                rs.executeUpdate(classificationSql, classification,encKey,crc, id);
            }
            //更新人员密级end


            //add by wjy
            //同步RTX端的用户信息.
            boolean isAdd = false;
            //new OrganisationCom().checkUser(Integer.parseInt(id));//此方法此处调用没有实际意义，与集成模块沟通可以去除
            if (("".equals(oldLoginid) && !"".equals(loginid)) || (!loginid.equals(oldLoginid) && !"".equals(loginid))) { //使用登陆名的情况下，将会在Rtx中新增一用户
                if (!loginid.equals(oldLoginid)) {//修改用户名的情况下新添加用户
                    new Thread(new OrganisationComRunnable("user", "add", id)).start();
                    isAdd = true;
                }

            } else if (!"".equals(oldLoginid) && "".equals(loginid)) { //去除登陆名的情况下，将会在Rtx中去除这一用户
                new Thread(new OrganisationComRunnable("user", "del2", id)).start();
            }
            if (!isAdd && !"".equals(loginid)) {
                new Thread(new OrganisationComRunnable("user", "edit", id)).start();
            }
            // 改为自进行修正

            ResourceComInfo.updateResourceInfoCache(id);

            new PluginUserCheck().clearPluginUserCache("messager");

            //OA与第三方接口单条数据同步方法开始

            new HrmServiceManager().SynInstantHrmResource(id, "1");
            //OA与第三方接口单条数据同步方法结束
            HrmFaceCheckManager.sync(id,HrmFaceCheckManager.getOptUpdate(),
                    "hrm_e9_HrmSystemInfoService_save",HrmFaceCheckManager.getOaResource());

            LogUtil.writeBizLog(logger.getBizLogContexts());

            //更新账户的状态 是否为AD的账户
            if (new AuthenticUtil().checkType(loginid) && isADAccount.equals("1")) {
                rs.executeSql("update HrmResource set isADAccount='1' where id='" + id + "'");
            } else {
                rs.executeSql("update HrmResource set isADAccount=NULL where id='" + id + "'");
            }

            long arg0 = StringUtil.parseToLong(id);
            boolean arg1 = false;
            String arg2 = "";
            int arg3 = 0;
            if (usbstate.equals("1")) {
                arg1 = StringUtil.vString(request.getParameter("needauto")).equals("1");
                arg2 = StringUtil.getURLDecode(request.getParameter("enableDate"));
                arg3 = StringUtil.parseToInt(request.getParameter("enableUsbType"));
            }
            HrmUsbAutoDateManager manager = new HrmUsbAutoDateManager();
            Map<String, Comparable> map = new HashMap<String, Comparable>();
            map.put("userId", arg0);

            boolean isNew = false;
            HrmUsbAutoDate autoDate = manager.get(map);
            if (autoDate == null) {
                isNew = true;
                autoDate = new HrmUsbAutoDate();
                autoDate.setUserId(arg0);
            }
            autoDate.setNeedAuto(arg1 ? 1 : 0);
            autoDate.setEnableDate(arg2);
            autoDate.setEnableUsbType(arg3);
            if (isNew) manager.insert(autoDate);
            else manager.update(autoDate);

            //BBS集成相关
            if (!password.equals("0")) {
                String bbsLingUrl = new BaseBean().getPropValue(GCONST.getConfigFile(), "ecologybbs.linkUrl");
                if (!bbsLingUrl.equals("")) {
                    new Thread(new weaver.bbs.BBSRunnable(loginid, password)).start();
                }
            }


            rs.execute("update HrmResource set " + (useSecondaryPwd.equals("1") ? "" : " secondaryPwd=null,") + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);
            rs.execute("update HrmResourceManager set " + (useSecondaryPwd.equals("1") ? "" : " secondaryPwd=null,") + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);

            //修改人员实时同步到CoreMail邮件系统
            //CoreMailAPI coremailapi = CoreMailAPI.getInstance();
            //coremailapi.synUser(id);
            retmap.put("status", "1");
            //系统信息保存成功
            if(retmap.get("status").equals("1")){
                try{
                    if(password.equals("qwertyuiop")){  //没有修改密码
                    }else{

                        //更新修改密码时间
                        String passwdchgdate = Util.null2String(TimeUtil.getCurrentDateString());
                        RecordSet passwdchgdaters = new RecordSet();
                        passwdchgdaters.executeUpdate("update hrmresource set passwdchgdate=? where id=?",passwdchgdate,id) ;

                        //修改密码，且修改成功
                        ServiceUtil serviceUtil = new ServiceUtil();
                        ServletContext servletContext = session.getServletContext();
                        //下线EM
                        serviceUtil.emOffline(id);
                        //下线PC
                        serviceUtil.offLine4PC(id,servletContext);
                    }
                }catch (Exception e) {
                    writeLog("系统信息处修改密码之后调用人员下线出现异常：" + e);
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            writeLog(e);
            retmap.put("status", "-1");
            retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620, ThreadVarLanguage.getLang())+"");
        }
        return retmap;
    }

    /**
     * 获取一周信息
     *
     * @param request
     * @param response
     */
    public Map<String, Object> getWeekInfo(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            String resourceid = Util.null2String(request.getParameter("id"));
            if (resourceid.length() == 0) {
                resourceid = "" + user.getUID();
            }
            RecordSet rs = new RecordSet();
            boolean canEdit = HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user);
            if (resourceid.equals("" + user.getUID())) canEdit = true;
            if (!canEdit) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
                return retmap;
            }


            String sql = "";
            String datefrom = Util.null2String(request.getParameter("datefrom"));
            String dateto = Util.null2String(request.getParameter("dateto"));
            if (datefrom.length() == 0) {
                datefrom = DateUtil.getFirstWeekDateToString(DateUtil.getToday());
                dateto = DateUtil.getLastWeekDateToString(DateUtil.getToday());
            } else {
                java.util.Date today = DateUtil.parseToDate(datefrom);
                datefrom = DateUtil.getFirstWeekDateToString(today);
                dateto = DateUtil.getLastWeekDateToString(today);
            }
            String tmpdate = datefrom;
            HrmScheduleManager HrmScheduleManager = new HrmScheduleManager();

            Map<String, Object> kqinfo = new HashMap<String, Object>();

            //考勤天数：5	签到次数：5	签退次数：5	微博填写次数：5次	微博未填写次数：0次
            int subcompanyid1 = 0;
            rs.executeSql("select startdate,subcompanyid1 from hrmresource where id = " + resourceid);
            if (rs.next()) {
                subcompanyid1 = rs.getInt("subcompanyid1");
            }
            Map<String, Boolean> mapWorkday = HrmScheduleManager.isWorkday(datefrom, dateto, subcompanyid1);
            int workday = 0;
            for (Map.Entry<String, Boolean> entry : mapWorkday.entrySet()) {
                if (entry.getValue()) workday++;
            }
            kqinfo.put("weekworkdayslabel", ""+ SystemEnv.getHtmlLabelName(10004513, ThreadVarLanguage.getLang())+"");
            kqinfo.put("weekworkdays", workday);

            //本周签到次数
            sql = " SELECT count(distinct signdate) as weeksigninnum  FROM HrmScheduleSign WHERE signType=1 and signdate >= '" + datefrom + "' and signdate <= '" + dateto + "' and userid = " + resourceid;
            rs.executeSql(sql);
            if (rs.next()) {
                kqinfo.put("weeksigninnumlabel", ""+ SystemEnv.getHtmlLabelName(10003664, ThreadVarLanguage.getLang())+"");
                kqinfo.put("weeksigninnum", rs.getString("weeksigninnum"));
            } else {
                kqinfo.put("weeksigninnumlabel", ""+ SystemEnv.getHtmlLabelName(10003664, ThreadVarLanguage.getLang())+"");
                kqinfo.put("weeksigninnum", "0");
            }

            //本周签退次数
            sql = " SELECT count(distinct signdate) as weeksignoutnum  FROM HrmScheduleSign WHERE signType=2 and signdate >= '" + datefrom + "' and signdate <= '" + dateto + "' and userid = " + resourceid;
            rs.executeSql(sql);
            if (rs.next()) {
                kqinfo.put("weeksignoutnumlabel", ""+ SystemEnv.getHtmlLabelName(10004514, ThreadVarLanguage.getLang())+"");
                kqinfo.put("weeksignoutnum", rs.getString("weeksignoutnum"));
            } else {
                kqinfo.put("weeksignoutnumlabel", ""+ SystemEnv.getHtmlLabelName(10004514, ThreadVarLanguage.getLang())+"");
                kqinfo.put("weeksignoutnum", "0");
            }

            //本周微博填写次数
            int weekblogwritenum = 0;
            sql = " SELECT count(1) as weekblogwritenum  FROM blog_discuss WHERE workdate >= '" + datefrom + "' and workdate <= '" + dateto + "' and userid = " + resourceid;
            rs.executeSql(sql);
            if (rs.next()) {
                weekblogwritenum = rs.getInt("weekblogwritenum");
                kqinfo.put("weekblogwritenumlabel", ""+ SystemEnv.getHtmlLabelName(10004515, ThreadVarLanguage.getLang())+"");
                kqinfo.put("weekblogwritenum", weekblogwritenum + ""+ SystemEnv.getHtmlLabelName(18083, ThreadVarLanguage.getLang())+"");
            } else {
                kqinfo.put("weekblogwritenumlabel", ""+ SystemEnv.getHtmlLabelName(10004515, ThreadVarLanguage.getLang())+"");
                kqinfo.put("weekblogwritenum", "0"+ SystemEnv.getHtmlLabelName(18083, ThreadVarLanguage.getLang())+"");
            }

            //本周微博未填写次数
            kqinfo.put("weekblognotwritenumlabel", ""+ SystemEnv.getHtmlLabelName(10004516, ThreadVarLanguage.getLang())+"");
            kqinfo.put("weekblognotwritenum", workday - weekblogwritenum < 0 ? 0 : workday - weekblogwritenum + ""+ SystemEnv.getHtmlLabelName(18083, ThreadVarLanguage.getLang())+"");
            retmap.put("kqinfo", kqinfo);

            Map<String, String> signintime = new HashMap<String, String>();
            Map<String, String> signinip = new HashMap<String, String>();
            Map<String, String> signouttime = new HashMap<String, String>();
            Map<String, String> signoutip = new HashMap<String, String>();
            Map<String, String> mobilesignaddr = new HashMap<String, String>();
            Map<String, String> blogcreated = new HashMap<String, String>();
            sql = " SELECT signDate,signTime,signType,clientAddress,ADDR FROM HrmScheduleSign WHERE userid=" + resourceid + " and signDate>='" + datefrom + "' AND signDate<='" + dateto + "' ORDER BY signDate ASC ,signTime ASC";
            rs.executeSql(sql);
            while (rs.next()) {
                String signdate = Util.null2String(rs.getString("signDate"));
                String signtime = Util.null2String(rs.getString("signTime"));
                String signtype = Util.null2String(rs.getString("signType"));
                String clientaddress = Util.null2String(rs.getString("clientAddress"));
                String addr = Util.null2String(rs.getString("addr"));

                if (signtype.equals("1")) {
                    if (!signouttime.containsKey(signdate)) {//取最早一次
                        signintime.put(signdate, signtime);
                        signinip.put(signdate, clientaddress);
                        mobilesignaddr.put(signdate, addr);
                    }
                } else if (signtype.equals("2")) {//取最晚一次
                    signouttime.put(signdate, signtime);
                    signoutip.put(signdate, clientaddress);
                }
            }

            sql = " SELECT createdate, createtime from blog_discuss where userid = " + resourceid + " and workdate>='" + datefrom + "' AND workdate<='" + dateto + "'";
            rs.executeSql(sql);
            while (rs.next()) {
                String createdate = Util.null2String(rs.getString("createdate"));
                String createtime = Util.null2String(rs.getString("createtime"));
                blogcreated.put(createdate, createtime);
            }

            List<Map<String, String>> lsWeekInfo = new ArrayList<Map<String, String>>();
            Map<String, String> weekInfo = new HashMap<String, String>();
            for (int i = 0; i < 10; i++) {
                if (DateUtil.compDate(DateUtil.getDate(tmpdate), DateUtil.getDate(dateto)) < 0) break;
                weekInfo = new HashMap<String, String>();
                weekInfo.put("signintime", signintime.containsKey(tmpdate) ? signintime.get(tmpdate) : "");//签到时间
                weekInfo.put("signinip", signinip.containsKey(tmpdate) ? signinip.get(tmpdate) : "");//签到IP
                weekInfo.put("signouttime", signouttime.containsKey(tmpdate) ? signouttime.get(tmpdate) : "");//签退时间
                weekInfo.put("signoutip", signoutip.containsKey(tmpdate) ? signoutip.get(tmpdate) : "");//签退IP
                weekInfo.put("blogcreated", blogcreated.containsKey(tmpdate) ? blogcreated.get(tmpdate) : "");//微博填写

                String starttime = "";
                String endtime = "";
                sql = " SELECT t.hrmid, t.startdate, t.starttime, t.enddate, t.endtime, t.name from ( " +
                        " SELECT  a.resourceid AS hrmid , a.begindate AS startdate , a.begintime AS starttime , " +
                        " a.enddate AS enddate , a.endtime AS endtime, a.name " +
                        " FROM    WorkPlan a JOIN WorkPlanType b ON a.type_n = b.workPlanTypeID" +
                        " WHERE   a.status = 0 AND a.deleted <> 1 )  t " +
                        //" where t.startdate = '"+tmpdate+"' and t.enddate = '"+tmpdate+"' and t.name like '%泛微出差%' and ','+hrmid+',' like '%,"+ resourceid +",%'  order by starttime ";
                        " where t.startdate = '" + tmpdate + "' and t.enddate = '" + tmpdate + "' and t.name like '%"+ SystemEnv.getHtmlLabelName(10004517, ThreadVarLanguage.getLang())+"%' and " + MeetingUtil.getHrmLikeSql("hrmid", resourceid, rs) + "  order by starttime ";
                rs.executeSql(sql);
                if (rs.next()) {
                    starttime = rs.getString("startdate") + " " + rs.getString("starttime");
                    endtime = rs.getString("enddate") + " " + rs.getString("endtime");
                }

                weekInfo.put("businesstravelbegin", starttime);//出差开始时间
                weekInfo.put("businesstravelend", endtime);//出差结束时间
                weekInfo.put("date", "" + tmpdate);
                weekInfo.put("weekday", getWeekDay(i, user));
                weekInfo.put("mobilesignaddr", mobilesignaddr.containsKey(tmpdate) ? mobilesignaddr.get(tmpdate) : "");//移动签到
                lsWeekInfo.add(weekInfo);
                tmpdate = DateUtil.addDate(tmpdate, 1);
            }
            retmap.put("weekInfo", lsWeekInfo);
            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog("获取一周信息错误：" + e);
            retmap.put("status", "-1");
            retmap.put("message", ""+ SystemEnv.getHtmlLabelName(10004518, ThreadVarLanguage.getLang())+"");
        }
        return retmap;
    }

    public String getWeekDay(int day, User user) {
        String weekDay = "";
        if (day == 0) {
            weekDay = SystemEnv.getHtmlLabelName(16100, user.getLanguage());
        } else if (day == 1) {
            weekDay = SystemEnv.getHtmlLabelName(16101, user.getLanguage());
        } else if (day == 2) {
            weekDay = SystemEnv.getHtmlLabelName(16102, user.getLanguage());
        } else if (day == 3) {
            weekDay = SystemEnv.getHtmlLabelName(16103, user.getLanguage());
        } else if (day == 4) {
            weekDay = SystemEnv.getHtmlLabelName(16104, user.getLanguage());
        } else if (day == 5) {
            weekDay = SystemEnv.getHtmlLabelName(16105, user.getLanguage());
        } else if (day == 6) {
            weekDay = SystemEnv.getHtmlLabelName(16106, user.getLanguage());
        }
        return weekDay;
    }


    @SuppressWarnings("deprecation")
    private boolean isHasRight(User user) {

        //入职维护人维护权限。
        RecordSet rs1 = new RecordSet();
        rs1.executeSql("  select hrmids from HrmInfoMaintenance where id = 1");
        rs1.next();
        String[] hrmids = rs1.getString("hrmids").split(",");
        boolean flog = false;
        for (String s1 : hrmids) {
            if (s1.equals(user.getUID() + "")) {
                flog = true;
                break;
            }
        }
        return flog;
    }

}
