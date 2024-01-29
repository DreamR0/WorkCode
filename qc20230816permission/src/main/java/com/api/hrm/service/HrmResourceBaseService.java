package com.api.hrm.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.customization.qc20230816.permission.util.CheckSelectUtil;
import com.api.customization.qc20230816.permission.util.CheckSetUtil;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.bean.RightMenu;
import com.api.hrm.bean.RightMenuType;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.api.hrm.util.HrmFieldUtil;
import com.api.hrm.util.ServiceUtil;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.biz.HrmFieldManager;
import com.engine.hrm.entity.RuleCodeType;
import com.engine.hrm.util.CodeRuleManager;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import com.engine.hrm.util.face.ValidateFieldManager;
import com.engine.hrm.util.face.bean.CheckItemBean;
import com.engine.kq.wfset.util.KQ122Util;
import com.engine.portal.biz.constants.ModuleConstants;
import com.engine.portal.biz.nonstandardfunction.SysModuleInfoBiz;
import ln.LN;
import org.apache.commons.lang3.StringUtils;
import weaver.blog.BlogShareManager;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.conn.RecordSetTrans;
import weaver.cowork.CoworkShareManager;
import weaver.cpt.search.CptSearchComInfo;
import weaver.crm.CrmShareBase;
import weaver.docs.docs.CustomFieldManager;
import weaver.docs.search.DocSearchComInfo;
import weaver.encrypt.EncryptUtil;
import weaver.file.FileUpload;
import weaver.file.Prop;
import weaver.filter.XssUtil;
import weaver.formmode.cuspage.cpt.Cpt4modeUtil;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.StaticObj;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.definedfield.HrmFieldComInfo;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.finance.SalaryManager;
import weaver.hrm.job.JobActivitiesComInfo;
import weaver.hrm.job.JobCallComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.location.LocationComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.privacy.PrivacyBaseComInfo;
import weaver.hrm.privacy.PrivacyComInfo;
import weaver.hrm.privacy.UserPrivacyComInfo;
import weaver.hrm.resource.*;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.hrm.tools.HrmResourceFile;
import weaver.hrm.tools.HrmValidate;
import weaver.interfaces.hrm.HrmServiceManager;
import weaver.license.PluginUserCheck;
import weaver.login.Account;
import weaver.login.VerifyLogin;
import weaver.proj.search.SearchComInfo;
import weaver.rtx.OrganisationCom;
import weaver.rtx.OrganisationComRunnable;
import weaver.system.SysRemindWorkflow;
import weaver.systeminfo.MouldStatusCominfo;
import weaver.systeminfo.SysMaintenanceLog;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.language.LanguageComInfo;
import weaver.systeminfo.setting.HrmUserSettingComInfo;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import weaver.wechat.util.Utils;
import weaver.workflow.search.WorkflowRequestUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.*;

import static com.api.browser.util.ConditionType.INPUT;

/***
 * 人员卡片
 * @author lvyi
 *
 */
public class HrmResourceBaseService extends BaseBean {
    private static final char separator = Util.getSeparator();
    private String today = DateUtil.getCurrentDate();

    private boolean isMobx = false;
    private String id = "";
    //qc20230816 自定义修改权限
    private String isOpen = new BaseBean().getPropValue("qc20230816permission","isopen");

    /**
     * 人员卡片头部信息
     *
     * @param request
     * @param response
     * @return
     */
    public String getResourceBaseTitle(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            AppDetachComInfo adci = new AppDetachComInfo();
            HrmListValidate hrmListValidate = new HrmListValidate();
            boolean hasRight = false;
            String id = Util.null2String(request.getParameter("id"));
            if (id.equals("") || id.equals(Util.null2String(user.getUID() + ""))) {
                id = String.valueOf(user.getUID());
                hasRight = true;
            } else {
                hasRight = adci.checkUserAppDetach(id, "1", user) == 1;
            }
            retmap.put("hasRight", hasRight);
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            Map<String, Object> jsondata = new HashMap<String, Object>();

            RecordSet rs = new RecordSet();
            String sql = "select * from hrmresource where id=" + id;
            rs.executeSql(sql);
            if (rs.next()) {
                String lastname = Util.toScreen(rs.getString("lastname"), user.getLanguage());
                String workcode = Util.toScreen(rs.getString("workcode"), user.getLanguage());
                String sex = Util.toScreen(rs.getString("sex"), user.getLanguage());
                String departmentid = Util.toScreen(rs.getString("departmentid"), user.getLanguage());
                String subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
                lastname = lastname.endsWith("\\") && !lastname.endsWith("\\\\") == true ? lastname + "\\" : lastname;
                lastname = StringUtil.replace(lastname, "'", "\\\\'");

                String messagerUrls = Util.toScreen(rs.getString("messagerurl"), user.getLanguage());
                String resourceimageid = ResourceComInfo.getResourceimageid(id);
                if (messagerUrls.length() == 0 && !resourceimageid.equals("0") && resourceimageid.length() > 0) {
                    messagerUrls = ServiceUtil.saveMessagerUrl(Util.getIntValue(resourceimageid), id);
                } else {
                    messagerUrls = ResourceComInfo.getMessagerUrls(id);
                }
//                String filePath = GCONST.getRootPath() + messagerUrls;
//                File file = new File(filePath);
//                if (!file.exists() || !hrmListValidate.isValidate(36)) {
//                    if (resourceimageid > 0) {
//                        String mainControlHost = Util.null2String(Prop.getInstance().getPropValue("Others", "MAINCONTROLHOST"));
//                        messagerUrls = mainControlHost + "/weaver/weaver.file.FileDownload?fileid=" + resourceimageid;
//                    } else {
//                        if (sex.equals("0")) {
//                            messagerUrls = "/messager/images/icon_m_wev8.jpg";
//                        } else if (sex.equals("1")) {
//                            messagerUrls = "/messager/images/icon_w_wev8.jpg";
//                        }
//                    }
//                }
//
//                boolean USERICONLASTNAME = Util.null2String(new BaseBean().getPropValue("Others", "USERICONLASTNAME")).equals("1");
//                if (USERICONLASTNAME && (messagerUrls.indexOf("icon_w_wev8.jpg") > -1 || messagerUrls.indexOf("icon_m_wev8.jpg") > -1 || messagerUrls.indexOf("dummyContact.png") > -1)) {
//                    jsondata.put("shortname", User.getLastname(Util.null2String(Util.formatMultiLang(lastname, "" + user.getLanguage()))));
//                }
                jsondata.put("lastname", lastname);
                jsondata.put("messagerurl", messagerUrls);
                jsondata.put("requestParams", ResourceComInfo.getUserIconInfoStr(id,user));
                jsondata.put("workcode", workcode);
                HrmFieldComInfo hrmFieldComInfo = new HrmFieldComInfo();
                String isused = Util.null2String(hrmFieldComInfo.getBaseFieldMap("sex").getString("isused"));
                if ("1".equals(isused)) {
                    Map<String, Object> sexdata = new HashMap<String, Object>();
                    jsondata.put("sex", sexdata);//0：男性，1：女性
                    sexdata.put("value", sex);
                    if (sex.equals("0")) {
                        sexdata.put("name", SystemEnv.getHtmlLabelName(28473, user.getLanguage()));
                    } else if (sex.equals("1")) {
                        sexdata.put("name", SystemEnv.getHtmlLabelName(28474, user.getLanguage()));
                    }
                }
                if (Util.null2String(Prop.getInstance().getPropValue("Others", "showDepartmentFullName")).equals("1")) {
                    jsondata.put("orginfo", DepartmentComInfo.getAllParentDepartmentNames(departmentid, subcompanyid));
                } else {
                    jsondata.put("orginfo", DepartmentComInfo.getAllParentDepartmentMarks(departmentid, subcompanyid));
                }
            }
            retmap.put("result", jsondata);
            retmap.put("id", id);
        } catch (Exception e) {
            writeLog(e);
        }

        return JSONObject.toJSONString(retmap);
    }

    /**
     * 查询人员列表右键菜单
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public String getRightMenu(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();
            User user = HrmUserVarify.getUser(request, response);
            String id = Util.null2String(request.getParameter("id"));
            if (id.equals("")) id = String.valueOf(user.getUID());

            HrmListValidate HrmListValidate = new HrmListValidate();
            List<RightMenu> rightMenus = new ArrayList<RightMenu>();
            int language = user.getLanguage();
            int hrmdetachable = 0;
            HttpSession session = request.getSession(true);
            String status = "";
            String subcompanyid = "";
            String departmentid = "";

            String textMessageSql = "select dataValue from ecology_message_valve_config where dataKey = 'emSwitch'";
            RecordSet textRs = new RecordSet();
            textRs.execute(textMessageSql);
            textRs.next();
            //dataValue为0代表关闭发消息选项
            String dataValue = textRs.getString("dataValue");

            RecordSet rs = new RecordSet();
            rs.executeSql("select subcompanyid1,departmentid, status from hrmresource where id = " + id);
            if (rs.next()) {
                status = Util.toScreen(rs.getString("status"), user.getLanguage());
                subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
                departmentid = Util.toScreen(rs.getString("departmentid"), user.getLanguage());
                if (subcompanyid == null || subcompanyid.equals("") || subcompanyid.equalsIgnoreCase("null"))
                    subcompanyid = "-1";
                session.setAttribute("hrm_subCompanyId", subcompanyid);
            }

            boolean isSelf = false;
            if (id.equals("" + user.getUID())) {
                isSelf = true;
            }
            //人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
            if (session.getAttribute("hrmdetachable") != null) {
                hrmdetachable = Util.getIntValue(String.valueOf(session.getAttribute("hrmdetachable")), 0);
            } else {
                boolean isUseHrmManageDetach = new ManageDetachComInfo().isUseHrmManageDetach();
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
            int operatelevel = -1;
            if (hrmdetachable == 1) {
                operatelevel = new CheckSubCompanyRight().ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", Integer.parseInt(subcompanyid));
            } else {
                if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentid))
                    operatelevel = 2;
            }


            if (((isSelf && isHasModify(-1)) || operatelevel > 0) && !status.equals("10")) {//编辑
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_EDIT, "editCard"));
            }

            if (HrmListValidate.isValidate(33) && !isSelf && "1".equals(Utils.null2String(dataValue))) {//发送消息
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_SENDMESAGE, "sendEmessage"));
            }

            if (HrmListValidate.isValidate(31) && !isSelf) {//发送短信
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_SENDSMSMESSAGE, "sendSmsMessage"));
            }

            if (HrmListValidate.isValidate(19) && !isSelf) {//发送邮件
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_SENDMAIL, "sendMail"));
            }

            if (HrmListValidate.isValidate(32) && !isSelf) {//新建日程
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_DOADDWORKPLANBYHRM, "doAddWorkPlanByHrm"));
            }

            if (((isSelf && canEditUserIcon()) || operatelevel > 0) && !status.equals("10")) {//设置个人头像
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_SetUserIcon, "showPortraitSetting"));
            }

            Map<String, Object> params = new HashMap<>();
            params.put("logSmallType", BizLogSmallType4Hrm.HRM_RSOURCE_CARD_BASE.getCode());
            params.put("targetId", id);
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_log, "showLog", params));

            apidatas.put("rightMenus", rightMenus);
        } catch (Exception e) {
            writeLog(e);
            apidatas.put("api_status", false);
            apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    public String isAdmin(HttpServletRequest request, HttpServletResponse response) {
        User user = HrmUserVarify.getUser(request, response);
        Map<String, Object> result = new HashMap<String, Object>();
        String id = Util.null2String(request.getParameter("id"));
        if (id.length() == 0) {
            id = "" + user.getUID();
        }
        result.put("isAdmin", ServiceUtil.isAdmin(id));
        return JSONObject.toJSONString(result);
    }

    public String getQRCode(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> jsondata = new HashMap<String, Object>();
        Map<String, Object> options = new HashMap<String, Object>();
        User user = HrmUserVarify.getUser(request, response);
        String id = Util.null2String(request.getParameter("id"));
        if (id.equals("")) id = String.valueOf(user.getUID());

        try {
            //var data = { "lastname": "吕益", "mobile": "13899998888","telephone":"021-99998888","email":"lvyi@weaver.com","jobtitle":"开发工程师","department":"人力资源组","locationname":"上海"};
            RecordSet rs = new RecordSet();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
            LocationComInfo locationComInfo = new LocationComInfo();
            JobCallComInfo jobCallComInfo = new JobCallComInfo();
            HrmListValidate HrmListValidate = new HrmListValidate();

            String sql = "select * from hrmresource where id=" + id;
            rs.executeSql(sql);
            if (rs.next()) {
                String lastname = Util.toScreen(rs.getString("lastname"), user.getLanguage());
                String departmentid = Util.toScreen(rs.getString("departmentid"), user.getLanguage());
                String jobtitle = Util.toScreen(rs.getString("jobtitle"), user.getLanguage());
                String mobile = Util.toScreen(rs.getString("mobile"), user.getLanguage());
                String telephone = Util.toScreen(rs.getString("telephone"), user.getLanguage());
                String email = Util.toScreen(rs.getString("email"), user.getLanguage());
                String locationid = Util.toScreen(rs.getString("locationid"), user.getLanguage());
                String jobcallid = Util.toScreen(rs.getString("jobcall"), user.getLanguage());
                lastname = lastname.endsWith("\\") && !lastname.endsWith("\\\\") == true ? lastname + "\\" : lastname;
                lastname = StringUtil.replace(lastname, "'", "\\\\'");
                boolean hasQRCode = HrmListValidate.isValidate(37);

                jsondata.put("hasQRCode", hasQRCode);
                jsondata.put("options", options);
                if (hasQRCode) {
                    options.put("lastname", lastname);
                    options.put("mobile", mobile);
                    options.put("telephone", telephone);
                    options.put("email", email);
                    options.put("jobtitle", jobTitlesComInfo.getJobTitlesname(jobtitle));  //对应的应该是岗位
                    options.put("department", DepartmentComInfo.getDepartmentname(departmentid));  //对应的应该是职称
                    //options.put("locationname", locationComInfo.getLocationname(departmentid));
                    options.put("locationname", locationComInfo.getLocationname(locationid));  //对应的应该是工作地点
                }
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return JSONObject.toJSONString(jsondata);
    }

    public String getHrmResourceItem(HttpServletRequest request, HttpServletResponse response) {
        List<Map<String, Object>> tablist = new ArrayList<Map<String, Object>>();
        Map<String, Object> itemdata = new HashMap<String, Object>();
        User user = HrmUserVarify.getUser(request, response);
        String resourceid = request.getParameter("id");
        boolean noLoadData = Util.null2String(request.getParameter("noLoadData")).equals("1");//不加载数据
        if (resourceid.equals("")) resourceid = String.valueOf(user.getUID());
        String currentUserId = "" + user.getUID();
        try {
            boolean workflowshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Workflow);
            boolean docshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Doc);
            boolean customshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Crm);
            boolean projectshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Project);
            boolean cptshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Fa);
            boolean coworkshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Cowork);
            boolean weiboshow = SysModuleInfoBiz.checkModuleStatus(ModuleConstants.Blog);

            //流程
            if (workflowshow) {
                int wf_count = 0;
                if (!noLoadData) {
                    wf_count = new WorkflowRequestUtil().getRequestCount(user, resourceid);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_workflow");
                itemdata.put("label", SystemEnv.getHtmlLabelName(18015, user.getLanguage()));
                itemdata.put("num", wf_count);
                itemdata.put("icon", "/hrm/hrm_e9/image/workflow.png");
                itemdata.put("font-color", "#1ab7f4");
                itemdata.put("url", "/spa/workflow/static/index.html#/main/workflow/listDoing?resourceid=" + resourceid);
                tablist.add(itemdata);
            }

            //文档
            if (docshow) {
                Object[] docInfo = new Object[]{"/spa/document/static/index.html#/main/document/search?viewcondition=2&doccreaterid=" + resourceid, "0"};
                if (!noLoadData) {
                    docInfo = new DocSearchComInfo().getDocCount4Hrm(resourceid, user);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_doc");
                itemdata.put("label", SystemEnv.getHtmlLabelName(58, user.getLanguage()));
                itemdata.put("num", docInfo[1]);
                itemdata.put("icon", "/hrm/hrm_e9/image/doc.png");
                itemdata.put("font-color", "#68c12c");
                itemdata.put("url", "/spa/document/static/index.html#/main/document/search?viewcondition=2&doccreaterid=" + resourceid);
                tablist.add(itemdata);
            }

            //客户
            if (customshow) {
                String[] crmInfo = new String[]{"", "0"};
                if (!noLoadData) {
                    crmInfo = new CrmShareBase().getCrmCount4Hrm(resourceid, currentUserId);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_custom");
                itemdata.put("label", SystemEnv.getHtmlLabelName(136, user.getLanguage()));
                itemdata.put("num", crmInfo[1]);
                itemdata.put("icon", "/hrm/hrm_e9/image/custom.png");
                itemdata.put("font-color", "#558de4");
                itemdata.put("url", "/spa/crm/static/index.html#/main/crm/customer/hrmView?searchHrmId=" + resourceid);
                tablist.add(itemdata);
            }

            //项目
            if (projectshow) {
                String[] prjInfo = new String[]{"/spa/prj/index.html#/main/prj/mineProject?search_resourceid=" + resourceid, "0"};
                if (!noLoadData) {
                    prjInfo = new SearchComInfo().getPrjCount4Hrm(resourceid, user);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_project");
                itemdata.put("label", SystemEnv.getHtmlLabelName(101, user.getLanguage()));
                itemdata.put("num", prjInfo[1]);
                itemdata.put("icon", "/hrm/hrm_e9/image/project.png");
                itemdata.put("font-color", "#29cf87");
                itemdata.put("url", "/spa/prj/index.html#/main/prj/mineProject?search_resourceid=" + resourceid);
                tablist.add(itemdata);
            }

            //资产
            if (cptshow) {
                String url = "/spa/cpt/index.html#/main/cpt/mycapital?hrmid=" + resourceid;
                String[] cptInfo = new String[]{url, "0"};
                if (!noLoadData) {
                    cptInfo = new CptSearchComInfo().getCptCount4Hrm(resourceid, user);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_cpt");
                itemdata.put("label", SystemEnv.getHtmlLabelName(535, user.getLanguage()));
                itemdata.put("num", cptInfo[1]);
                itemdata.put("icon", "/hrm/hrm_e9/image/cpt.png");
                itemdata.put("font-color", "#f6ae40");
                itemdata.put("url", url);
                tablist.add(itemdata);
            }

            //协作
            if (coworkshow) {
                String[] coworkInfo = new String[]{"", "0"};
                if (!noLoadData) {
                    coworkInfo = new CoworkShareManager().getCoworkCount4Hrm(resourceid, currentUserId);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_cowork");
                itemdata.put("label", SystemEnv.getHtmlLabelName(17855, user.getLanguage()));
                itemdata.put("num", coworkInfo[1]);
                itemdata.put("icon", "/hrm/hrm_e9/image/cowork.png");
                itemdata.put("font-color", "#826efd");
                itemdata.put("url", "/spa/cowork/static/index.html#/main/cowork/hrmview?searchHrmid=" + resourceid);
                tablist.add(itemdata);
            }

            //微博
            if (weiboshow) {
                String[] weiboInfo = new String[]{"", "0"};
                if (!noLoadData) {
                    weiboInfo = new BlogShareManager().getBlogCount4Hrm(resourceid);
                }
                itemdata = new HashMap<String, Object>();
                itemdata.put("name", "item_weibo");
                itemdata.put("label", SystemEnv.getHtmlLabelName(26467, user.getLanguage()));
                itemdata.put("num", weiboInfo[1]);
                itemdata.put("icon", "/hrm/hrm_e9/image/weibo.png");
                itemdata.put("font-color", "#fb6f47");
                itemdata.put("url", "/spa/blog/static/index.html#/user/" + resourceid);
                tablist.add(itemdata);
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return JSONObject.toJSONString(tablist);
    }

    public String getHrmResourceTab(HttpServletRequest request, HttpServletResponse response) {
        int isgoveproj = 0;
        User user = HrmUserVarify.getUser(request, response);
        String id = Util.null2String(request.getParameter("id"));
        this.id = id;
        this.isMobx = Util.null2String(request.getParameter("isMobx")).equals("1");
        if (id.equals("")) id = String.valueOf(user.getUID());
        List<Map<String, Object>> tablist = new ArrayList<Map<String, Object>>();
        try {
            AppDetachComInfo AppDetachComInfo = new AppDetachComInfo();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            AllManagers AllManagers = new AllManagers();
            HrmListValidate HrmListValidate = new HrmListValidate();
            CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
            MouldStatusCominfo MouldStatusCominfo = new MouldStatusCominfo();
            HrmResourceBaseTabComInfo HrmResourceBaseTabComInfo = new HrmResourceBaseTabComInfo();
            RecordSet rs = new RecordSet();

            //人力资源模块是否开启了管理分权
            int hrmdetachable = 0;
            HttpSession session = request.getSession(true);
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
            if (AppDetachComInfo.isUseAppDetach()) {
                boolean isApp = true;
                if(!AppDetachComInfo.getScopeIds(user, "resource",id)) {
                    isApp = false;
                }
                if (!isApp) {
                    String errMsg = SystemEnv.getHtmlLabelName(2012, user.getLanguage());
                    HashMap<String, String> jsondata = new HashMap<String, String>();
                    jsondata.put("errMsg", errMsg);
                    jsondata.put("status", "-1");
                    return jsondata.toString();
                }
            }
            String departmentid = "";
            String subcompanyid = "";
            rs.executeProc("HrmResource_SelectByID", id);
            if (rs.next()) {
                departmentid = Util.toScreen(rs.getString("departmentid"), user.getLanguage());        /*所属部门*/
                subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
            }

            /*显示权限判断*/
            int userid = user.getUID();
            boolean isSelf = false;
            boolean isManager = false;
//			boolean isSys = ResourceComInfo.isSysInfoView(userid,id);
//			boolean isCap = ResourceComInfo.isCapInfoView(userid,id);
//			boolean ishasF =HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit",user);
            boolean isSys = ResourceComInfo.isSysInfoView2(userid, id);
            boolean isCap = ResourceComInfo.isCapInfoView2(userid, id);
            boolean ishasF = HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit", user,departmentid) || ResourceComInfo.isFinInfoView2(userid, id);
            ;


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
            int idx = 0;
            //tabInfo.put(SystemEnv.getHtmlLabelName(,user.getLanguage()), "/hrm/resource/HrmResourceBase.jsp?isfromtab=true&id="+id);
            tablist.add(setTabInfo("HrmResourceBase", 1361, GCONST.getContextPath()+"/hrm/resource/HrmResourceBase.jsp?isfromtab=true&id=" + id, "/main/hrm/resource/HrmResourceBase/" + id, user));
            idx++;

            //原工作历程变为其他
            if (HrmListValidate.isValidate(29)) {
                //tabInfo.put(SystemEnv.getHtmlLabelName(30804,user.getLanguage()), "/hrm/resource/HrmResourceTotal.jsp?isfromtab=true&id="+id);
                tablist.add(setTabInfo("HrmResourceTotal", 30804, GCONST.getContextPath()+"/wui/index.html#/main/hrm/HrmResourceTotal" + id, "/main/hrm/HrmResourceTotal" + id, user));
                idx++;
            }

            //tabInfo.put(SystemEnv.getHtmlLabelName(81554,user.getLanguage()), "/hrm/resource/HrmResourceGroupView.jsp?id="+id);
            if (HrmListValidate.isValidate(64)) {
                tablist.add(setTabInfo("HrmResourceGroupView", 81554, GCONST.getContextPath()+"/hrm/resource/HrmResourceGroupView.jsp?id=" + id, "/main/hrm/resource/HrmResourceGroupView/" + id, user));
                idx++;
            }

            int operatelevel = -1;
            if (hrmdetachable == 1) {
                operatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", Integer.parseInt(subcompanyid));
            } else {
                if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentid)) {
                    operatelevel = 2;
                }
            }
            int operatelevelnew = -1;
            if (hrmdetachable == 1) {
                operatelevelnew = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "ResourcesInformationSystem:All", Integer.parseInt(subcompanyid));
            } else {
                String departmentidtmp = ResourceComInfo.getDepartmentID(id);
                if (HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user, departmentidtmp)) {
                    operatelevelnew = 2;
                }
            }

            StaticObj staticobj = StaticObj.getInstance();
            String software = (String) staticobj.getObject("software");
            if (software == null) software = "ALL";
            if (software.equals("ALL") || software.equals("HRM")) {
                //qc20230816 个人信息权限放开
                if ((isSelf || operatelevel >= 0 /*|| "1".equals(isOpen)*/) && HrmListValidate.isValidate(11)) {
                    //tabInfo.put(SystemEnv.getHtmlLabelName(15687,user.getLanguage()), "/hrm/resource/HrmResourcePersonalView.jsp?isfromtab=true&id="+id);
                    tablist.add(setTabInfo("HrmResourcePersonalView", 15687, GCONST.getContextPath()+"/hrm/resource/HrmResourcePersonalView.jsp?isfromtab=true&id=" + id, "/main/hrm/resource/HrmResourcePersonalView/" + id, user));
                    idx++;
                }
                //qc20230816 工作信息权限放开
                if ((isSelf || isManager || operatelevel >= 0/*|| "1".equals(isOpen)*/) && HrmListValidate.isValidate(12)) {
                    //tabInfo.put(SystemEnv.getHtmlLabelName(15688,user.getLanguage()), "/hrm/resource/HrmResourceWorkView.jsp?isfromtab=true&id="+id);
                    tablist.add(setTabInfo("HrmResourceWorkView", 15688, GCONST.getContextPath()+"/hrm/resource/HrmResourceWorkView.jsp?isfromtab=true&id=" + id, "/main/hrm/resource/HrmResourceWorkView/" + id, user));
                    idx++;
                }
            }

            if ((isSelf || operatelevelnew >= 0 || isSys) && HrmListValidate.isValidate(15)) {
                //tabInfo.put(SystemEnv.getHtmlLabelName(15804,user.getLanguage()), "/hrm/resource/HrmResourceSystemView.jsp?isfromtab=true&id="+id+"&isView=1");
                tablist.add(setTabInfo("HrmResourceSystemView", 15804, GCONST.getContextPath()+"/hrm/resource/HrmResourceSystemView1.jsp?isfromtab=true&id=" + id + "&isView=1", "/main/hrm/resource/HrmResourceSystemView/" + id, user));
                idx++;
            }

            if (software.equals("ALL") || software.equals("HRM")) {
                if (isgoveproj == 0) {
                    if ((isSelf || ishasF) && HrmListValidate.isValidate(13)) {
                        //tabInfo.put(SystemEnv.getHtmlLabelName(16480,user.getLanguage()), "/hrm/resource/HrmResourceFinanceView.jsp?isfromtab=true&id="+id+"&isView=1");
                        tablist.add(setTabInfo("HrmResourceFinanceView", 16480, GCONST.getContextPath()+"/hrm/resource/HrmResourceFinanceView.jsp?isfromtab=true&id=" + id + "&isView=1", "/main/hrm/resource/HrmResourceFinanceView/" + id, user));
                        idx++;
                    }
                }
                if ((isSelf || operatelevel >= 0 || isCap) && HrmListValidate.isValidate(14) && "1".equals(MouldStatusCominfo.getStatus("cpt"))) {
                    String cpturl = GCONST.getContextPath()+"/cpt/search/SearchOperation.jsp?resourceid=" + id + "&isdata=2&from=hrmResourceBase";
                    if (Cpt4modeUtil.isUse()) {
                        cpturl = GCONST.getContextPath()+"/formmode/search/CustomSearchBySimpleIframe.jsp?customid=" + Cpt4modeUtil.getSearchid("wdzc") + "&resourceid=" + id + "&from=hrmResourceBase&mymodetype=wdzc&sqlwhere=" + new XssUtil().put("where resourceid=" + id);
                    }
                    //tabInfo.put(SystemEnv.getHtmlLabelName(15806,user.getLanguage()), cpturl);
                    tablist.add(setTabInfo("iframe-" + (idx++) + "-cptsearch", 15806, GCONST.getContextPath()+"/spa/cpt/index.html#/main/cpt/mycapital?from=hrm&hrmid=" + id, "/spa/cpt/index.html#/main/cptcapital/mycapitalResult", user));
                }
            }

            if ((isSelf || isManager || HrmUserVarify.checkUserRight("HrmResource:Workflow", user, departmentid)) && HrmListValidate.isValidate(17)) {
                //tabInfo.put(SystemEnv.getHtmlLabelName(1207,user.getLanguage()), "/workflow/search/WFSearchTemp.jsp?method=all&viewScope=doing&complete=0&viewcondition=0&resourceid="+id);
                tablist.add(setTabInfo("iframe-" + (idx++) + "-workflowsearch", 1207, GCONST.getContextPath()+"/spa/workflow/static/index.html#/main/workflow/listDoing?needTop=false&needTree=false&resourceid=" + id, "/spa/workflow/index.html#/main/workflow/listDoing", user));
                //tablist.add(setTabInfo("iframe-"+(idx++)+"-workflowsearch",1207,"/workflow/search/WFSearchTemp.jsp?method=all&viewScope=doing&complete=0&viewcondition=0&resourceid="+id,"/spa/workflow/index.html#/main/workflow/listDoing",user));
            }
            if ((isSelf || isManager || HrmUserVarify.checkUserRight("HrmResource:Plan", user, departmentid)) && HrmListValidate.isValidate(18)) {
                //tabInfo.put(SystemEnv.getHtmlLabelName(2192,user.getLanguage()), "/workplan/data/WorkPlan.jsp?resourceid="+id);
                tablist.add(setTabInfo("iframe-" + (idx++) + "-workplansearch", 2192, GCONST.getContextPath()+"/spa/workplan/static/index.html#/main/wp/myWorkPlan?fromcard=true&selectedUser=" + id, "/main/workplan/data/WorkPlan", user));
            }

            if (isgoveproj == 0) {
                if (software.equals("ALL") || software.equals("HRM")) {
                    if (isSelf || isManager || HrmUserVarify.checkUserRight("HrmResource:Absense", user, departmentid)) {
                        if (HrmListValidate.isValidate(20)) {
                            //tabInfo.put(SystemEnv.getHtmlLabelName(20080,user.getLanguage()), "/hrm/resource/HrmResourceAbsense.jsp?resourceid="+id);
                            tablist.add(setTabInfo("HrmResourceAbsense", 20080, GCONST.getContextPath()+"/hrm/resource/HrmResourceAbsense.jsp?resourceid=" + id, "/main/hrm/HrmResourceAbsense", user));
                            idx++;
                        }
                    }
                    if (isSelf || isManager || HrmUserVarify.checkUserRight("HrmResource:TrainRecord", user)) {
                        if (HrmListValidate.isValidate(21)) {
                            //tabInfo.put(SystemEnv.getHtmlLabelName(816,user.getLanguage()), "/hrm/resource/HrmResourceTrainRecord.jsp?resourceid="+id);
                            tablist.add(setTabInfo("HrmResourceTrainRecord", 816, GCONST.getContextPath()+"/hrm/resource/HrmResourceTrainRecord.jsp?resourceid=" + id, "/main/hrm/resource/HrmResourceTrainRecord/" + id, user));
                            idx++;
                        }
                    }
                    if (isSelf || isManager || HrmUserVarify.checkUserRight("HrmResource:RewardsRecord", user)) {
                        if (HrmListValidate.isValidate(22)) {
                            //tabInfo.put(SystemEnv.getHtmlLabelName(16065,user.getLanguage()), "/hrm/resource/HrmResourceRewardsRecordView.jsp?id="+id);
                            tablist.add(setTabInfo("HrmResourceRewardsRecordView", 16065, GCONST.getContextPath()+"/hrm/resource/HrmResourceRewardsRecordView.jsp?id=" + id, "/main/hrm/resource/HrmResourceRewardsRecordView/" + id, user));
                            idx++;
                        }
                    }
                }
            }

            //判断该用户对编辑人员机构是否具有的角色维护权限(TD19119)
            boolean rolesmanage = false;
            int varifylevel = -1;
            if (hrmdetachable == 1) {
                varifylevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmRolesEdit:Edit", Integer.parseInt(subcompanyid));
                if (varifylevel > 0) {
                    if (HrmUserVarify.checkUserRight("HrmRolesEdit:Edit", user)) {
                        varifylevel = 2;
                    } else {
                        varifylevel = -1;
                    }
                }
            } else {
                if (HrmUserVarify.checkUserRight("HrmRolesEdit:Edit", user)) {
                    varifylevel = 2;
                }
            }
            if (varifylevel > 0) {
                rolesmanage = true;
            }

            if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentid) && rolesmanage) {
                //tabInfo.put(SystemEnv.getHtmlLabelName(16527,user.getLanguage()), "/hrm/roles/HrmResourceNewRoles.jsp?resourceid="+id );
                tablist.add(setTabInfo("HrmResourceNewRoles", 16527, GCONST.getContextPath()+"/hrm/roles/HrmResourceNewRoles.jsp?resourceid=" + id, "/main/hrm/resource/HrmResourceNewRoles/" + id, user));
                idx++;
            }

            //取自定义标签页
            HrmResourceBaseTabComInfo.setTofirstRow();
            while (HrmResourceBaseTabComInfo.next()) {
                if (!HrmResourceBaseTabComInfo.getIsopen().equals("1")) {
                    continue;
                }
                String tab_urlTemp = HrmResourceBaseTabComInfo.getLinkurl();
                String tab_urlTemp1 = tab_urlTemp.replaceAll("\\Q{#id}", "" + id);
                if (tab_urlTemp1.indexOf("?") >= 0) {
                    tab_urlTemp1 += "&hrmResourceID=" + id;
                } else {
                    tab_urlTemp1 += "?hrmResourceID=" + id;
                }
                String groupname = HrmResourceBaseTabComInfo.getGroupName();
                if (HrmResourceBaseTabComInfo.getTabnum().trim().length() != 0) {
                    try {
                        Class c = Class.forName(HrmResourceBaseTabComInfo.getTabnum().trim());
                        Object cObject = c.newInstance();
                        Method m = c.getMethod("execute");
                        Method method = c.getMethod("setOwnerid", String.class);
                        method.invoke(cObject, "" + id);
                        method = c.getMethod("setUserid", String.class);
                        method.invoke(cObject, "" + user.getUID());
                        int num = (Integer) m.invoke(cObject);
                        groupname += "(" + num + ")";
                    } catch (Exception e) {
                        e.printStackTrace();
                        groupname += "(error)";
                    }
                }
                tablist.add(setTabInfo("iframe-" + (idx++) + "-DefineTab" + id, groupname, tab_urlTemp1, "", user));
            }

            List<Map<String, Object>> tablist1 = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> obj : tablist) {
                if (!obj.isEmpty()) {
                    tablist1.add(obj);
                }
            }
            tablist = tablist1;
        } catch (Exception e) {
            writeLog(e);
        }
        return JSONObject.toJSONString(tablist);
    }

    /**
     * 设置tabInfo 后续可以考虑直接读取数据
     *
     * @param key
     * @param labelid
     * @param url
     * @param rotueurl
     * @param user
     * @return
     */
    private Map<String, Object> setTabInfo(String key, int labelid, String url, String rotueurl, User user) {
        return setTabInfo(key, "" + labelid, url, rotueurl, user);
    }

    /**
     * 设置tabInfo 后续可以考虑直接读取数据
     *
     * @param key
     * @param labelid
     * @param url
     * @param rotueurl
     * @param user
     * @return
     */
    private Map<String, Object> setTabInfo(String key, String labelid, String url, String rotueurl, User user) {
        Map<String, Object> tabObj = new HashMap<String, Object>();
        if (isMobx) {
            tabObj = getTabInfo4Mobx(key, labelid, url, rotueurl, user);
        } else {
            tabObj.put("key", key);
            if (key.indexOf("DefineTab") != -1) {
                tabObj.put("value", labelid);
            } else {
                tabObj.put("value", SystemEnv.getHtmlLabelNames(labelid, user.getLanguage()));
            }
            tabObj.put("url", url);
            tabObj.put("rotueurl", rotueurl);
        }
        return tabObj;
    }

    private Map<String, Object> getTabInfo4Mobx(String key, String labelid, String url, String rotueurl, User user) {
        Map<String, Object> tabObj = new HashMap<String, Object>();

        if (key.equals("HrmResourceBase")) {
            key = "cardInfo";
        } else if (key.equals("HrmResourcePersonalView")) {
            key = "cardPersonal";
        } else if (key.equals("HrmResourceWorkView")) {
            key = "cardWork";
        } else if (key.equals("HrmResourceSystemView")) {
            key = "cardSystemInfo";
        } else if (key.equals("HrmResourceFinanceView")) {
            key = "cardFinance";
        } else if (key.equals("HrmResourceAbsense")) {
            key = "cardChecking";
            /*判断是否开启了新考勤，如果开启了，那么人员卡片上面的考勤情况也需要切换成新版考勤的路由*/
            KQ122Util kq122Util = new KQ122Util();
            if (kq122Util.is122Open()) {
                key = "cardCheckingN";
            }
        } else if (key.equals("HrmResourceTrainRecord")) {
            key = "cardTrainRecord";
        } else if (key.equals("HrmResourceRewardsRecordView")) {
            key = "cardRewardsRecord";
        } else if (key.equals("HrmResourceNewRoles")) {
            key = "cardRoleSet";
        } else if (key.equals("HrmResourceTotal")) {
            key = "cardTotal";
        } else if (key.equals("HrmResourceGroupView")) {
            key = "addGroup";
        }

        if (key.indexOf("DefineTab") != -1) {
            tabObj.put("title", labelid);
        } else {
            tabObj.put("title", SystemEnv.getHtmlLabelNames(labelid, user.getLanguage()));
        }
        tabObj.put("key", key);
        tabObj.put("pathname", "main/hrm/card/" + key + "/" + this.id);
        tabObj.put("isIframe", key.startsWith("iframe-"));
        if (key.startsWith("iframe-")) {
            tabObj.put("url", url);
        }
        return tabObj;
    }


    /**
     * 普通人员只编辑后台开启允许个人修改的字段
     *
     * @param request
     * @param response
     * @return
     */
    public String getResourceContactForm(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<Object> lsGroup = new ArrayList<Object>();
        Map<String, Object> groupitem = null;
        List<Object> itemlist = null;
        try {
            //基本信息
            User user = HrmUserVarify.getUser(request, response);
            String id = Util.null2String(request.getParameter("id"));
            int viewattr = Util.getIntValue(request.getParameter("viewattr"), 1);
            if (id.length() == 0) {
                id = "" + user.getUID();
            }
            result.put("id", id);

            Map<String, Object> buttons = new Hashtable<String, Object>();
            if (isHasModify(-1)) {
                buttons.put("hasEdit", true);
                buttons.put("hasSave", true);
            }
            result.put("buttons", buttons);
            result.put("editcontact", "1");

            UserPrivacyComInfo upc = new UserPrivacyComInfo();
            PrivacyComInfo pc = new PrivacyComInfo();
            PrivacyBaseComInfo privacyBaseComInfo = new PrivacyBaseComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            Map<String, String> mapShowTypes = pc.getMapShowTypes();
            Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();
            //总体的大开关的默认设置
            Map<String, String> mapBaseShowTypeDefaults = privacyBaseComInfo.getMapShowTypeDefaults();

            StaticObj staticobj = StaticObj.getInstance();
            String multilanguage = (String) staticobj.getObject("multilanguage");
            if (multilanguage == null) multilanguage = "n";
            boolean isMultilanguageOK = false;
            if (multilanguage.equals("y")) isMultilanguageOK = true;
            boolean flagaccount = GCONST.getMOREACCOUNTLANDING();
            int scopeId = -1;
            HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
            hfm.isReturnDecryptData(true);
            CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
            hfm.getHrmData(Util.getIntValue(id));
            cfm.getCustomData(Util.getIntValue(id));
            CheckSetUtil checkSetUtil = new CheckSetUtil();

            while (HrmFieldGroupComInfo.next()) {
                int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
                if (grouptype != scopeId) continue;
                int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
                int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
                hfm.getCustomFields(groupid);

                groupitem = new HashMap<String, Object>();
                itemlist = new ArrayList<Object>();
                groupitem.put("title", SystemEnv.getHtmlLabelName(grouplabel, user.getLanguage()));
                groupitem.put("hide", hfm.getContactEditCount()  == 0 || !Util.null2String(HrmFieldGroupComInfo.getIsShow()).equals("1"));
                groupitem.put("defaultshow", true);
                groupitem.put("items", itemlist);
                groupitem.put("col", 1);
                lsGroup.add(groupitem);
                while (hfm.next()) {
                    int tmpviewattr = viewattr;
                    String fieldName = hfm.getFieldname();
                    String cusFieldname = "";
                    String fieldValue = "";
                    if (hfm.isBaseField(fieldName)) {
                        fieldValue = hfm.getHrmData(fieldName);
                    } else {
                        cusFieldname = "customfield" + hfm.getFieldid();
                        fieldValue = Util.null2String(new EncryptUtil().decrypt("cus_fielddata","field" + hfm.getFieldid(),"HrmCustomFieldByInfoType",""+scopeId,cfm.getData("field" + hfm.getFieldid()),true,true));
                    }

                    if (!hfm.isUse()||(!hfm.isModify())) {
                        HrmFieldBean hrmFieldBean = new HrmFieldBean();
                        hrmFieldBean.setFieldname(cusFieldname.length() > 0 ? cusFieldname : fieldName);
                        hrmFieldBean.setFieldhtmltype("1");
                        hrmFieldBean.setType("1");
                        hrmFieldBean.setFieldvalue(fieldValue);
                        hrmFieldBean.setIsFormField(true);
                        Map<String, Object> otherParams = new HashMap<String, Object>();
                        otherParams.put("hide", true);
                        hrmFieldBean.setOtherparam(otherParams);
                        SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);

                        itemlist.add(searchConditionItem);
                        continue;
                    }

                    if (fieldName.equals("systemlanguage") && !isMultilanguageOK) continue;
                    if (fieldName.equals("accounttype") && !flagaccount) continue;
                    if (fieldName.equalsIgnoreCase("jobGroupId")) {
                        continue;
                    }

                    if (fieldName.equals("departmentvirtualids")||fieldName.equals("messagerurl"))
                        continue;

                    if (fieldName.equals("resourceimageid")) {
                        groupitem.put("hasResourceImage", true);
                        if (fieldValue.equals("0")) fieldValue = "";
                        if(fieldValue.length()>0){
                            fieldValue = DocDownloadCheckUtil.checkPermission(fieldValue,user);
                        }
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
                    if (hrmFieldBean.getFieldname().equals("departmentid")) {
                        hrmFieldBean.setFieldvalue(Util.null2String(request.getParameter("departmentid")));
                    }
                    hrmFieldBean.setFieldvalue(fieldValue);
                    hrmFieldBean.setIsFormField(true);

                    if (fieldName.equals("mobile") || fieldName.equals("email") ||
                            fieldName.equals("fax") || fieldName.equals("telephone") || fieldName.equals("mobilecall")|| fieldName.equals("workcode")) {
                        hrmFieldBean.setMultilang(false);
                    }

                    if (((String) hrmFieldConf.get("ismand")).equals("1")) {
                        if (hrmFieldBean.getFieldname().equals("managerid")) {

                        } else {
                            tmpviewattr = 3;
                        }
                    }

                    if (hrmFieldBean.getFieldname().equals("status")) {//状态不能编辑
                        tmpviewattr = 1;
                    }

                    if (fieldName.equals("belongto") && flagaccount) {
                        tmpviewattr = 3;
                    }
                    String rules = "";
                    if(tmpviewattr==3){
                        rules = "required|string";
                    }
                    hrmFieldBean.setRules(rules);
                    SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                    if (searchConditionItem == null) continue;
//                    if (fieldName.equals("sex")) {
//                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
//                        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(28473, user.getLanguage()), true));
//                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(28474, user.getLanguage())));
//                        searchConditionItem.setOptions(options);
//                    }
//                    if (fieldName.equals("accounttype")) {
//                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
//                        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(17746, user.getLanguage()), true));
//                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(17747, user.getLanguage())));
//                        searchConditionItem.setOptions(options);
//                    }
                    if (fieldName.equals("belongto") && flagaccount) {
                        XssUtil xssUtil = new XssUtil();
                        String accountSql = "(accounttype=0 or accounttype=null or accounttype is null)";
                        searchConditionItem.getBrowserConditionParam().getDataParams().put("sqlwhere", xssUtil.put(accountSql));
                        searchConditionItem.getBrowserConditionParam().getCompleteParams().put("sqlwhere", xssUtil.put(accountSql));
                        searchConditionItem.getBrowserConditionParam().getDestDataParams().put("sqlwhere", xssUtil.put(accountSql));
                    }
                    if (searchConditionItem.getBrowserConditionParam() != null) {
                        searchConditionItem.getBrowserConditionParam().setViewAttr(tmpviewattr);
                        if (hrmFieldBean.getFieldname().equals("departmentid")) {
                            searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "HrmResourceAdd:Add");
                            searchConditionItem.getBrowserConditionParam().getCompleteParams().put("rightStr", "HrmResourceAdd:Add");
                        }
                    }
                    searchConditionItem.setViewAttr(tmpviewattr);
                    //qc20230816 字段修改权限按照建模表来决定
                    if (user.getUID()!=1 && "1".equals(isOpen)){
                        //这里如果修改的是非本人信息，就不检查公共编辑表
                        int setUserId = Integer.parseInt(id);
                            checkSetUtil.searchConditionItem(user.getUID(),fieldName,0,searchConditionItem,setUserId);
                    }
                    itemlist.add(searchConditionItem);

                    if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                        String mobileShowSet = Util.null2String(mapShowSets.get(fieldName));
                        String mobileShowType = Util.null2String(mapShowTypes.get(fieldName));
                        String mapShowTypeDefault = Util.null2String(mapShowTypeDefaults.get(fieldName));

                        String baseMobileShowTypeDefaults = Util.null2String(mapBaseShowTypeDefaults.get(fieldName));

                        if (mobileShowSet.equals("1")) {
                            String comPk = id + "__" + fieldName;
                            fieldValue = Util.null2String(upc.getPvalue(comPk));
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
                                if (fieldValue.length() > 0) {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage()), fieldValue.equals("1")));
                                } else if (baseMobileShowTypeDefaults.indexOf("1") != -1) {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage())));
                                }
                            }
                            if (mobileShowType.indexOf("2") != -1) {
                                if (fieldValue.length() > 0) {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage()), fieldValue.equals("2")));
                                } else if (baseMobileShowTypeDefaults.indexOf("2") != -1) {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage())));
                                }
                            }
                            if (mobileShowType.indexOf("3") != -1) {
                                if (fieldValue.length() > 0) {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage()), fieldValue.equals("3")));
                                } else if (baseMobileShowTypeDefaults.indexOf("3") != -1) {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage())));
                                }
                            }
                            searchConditionItem.setValue(fieldValue);
                            searchConditionItem.setOptions(statusOptions);
                            itemlist.add(searchConditionItem);
                        }
                    }
                }
                if (itemlist.size()== 0) lsGroup.remove(groupitem);
            }

            result.put("conditions", lsGroup);

        } catch (Exception e) {
            writeLog(e);
        }

        Map<String, Object> retmap = new HashMap<String, Object>();
        retmap.put("result", result);
        return JSONObject.toJSONString(retmap);
    }

    /**
     * 获取人员基本信息编辑表单
     *
     * @param request
     * @param response
     * @return
     */
    public String getResourceBaseForm(HttpServletRequest request, HttpServletResponse response) {
        User user = HrmUserVarify.getUser(request, response);

        Map<String, Object> result = new HashMap<String, Object>();
        try {
            //基本信息
            HrmListValidate HrmListValidate = new HrmListValidate();
            String id = Util.null2String(request.getParameter("id")).trim();
            if (id.length() == 0) {
                id = "" + user.getUID();
            }

            AppDetachComInfo AppDetachComInfo = new AppDetachComInfo();
            if (AppDetachComInfo.isUseAppDetach()) {
                boolean isApp = true;
                if(!AppDetachComInfo.getScopeIds(user, "resource",id)) {
                    isApp = false;
                }
                if (!isApp) {
                    String errMsg = SystemEnv.getHtmlLabelName(2012, user.getLanguage());
                    HashMap<String, String> jsondata = new HashMap<String, String>();
                    jsondata.put("errMsg", errMsg);
                    jsondata.put("status", "-1");
                    return JSONObject.toJSONString(jsondata);
                }
            }


            result.put("id", id);
            boolean isSelf = false;
            if (id.equals("" + user.getUID())) {
                isSelf = true;
            }
            HttpSession session = request.getSession(true);
            String subcompanyid = "";
            String departmentId = "";
            String status = "";
            RecordSet rs = new RecordSet();
            rs.executeSql("select subcompanyid1,departmentid, status from hrmresource where id = " + id);
            if (rs.next()) {
                subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
                departmentId = Util.toScreen(rs.getString("departmentid"), user.getLanguage());
                status = Util.toScreen(rs.getString("status"), user.getLanguage());
                if (subcompanyid == null || subcompanyid.equals("") || subcompanyid.equalsIgnoreCase("null")) {
                    subcompanyid = "-1";
                }
                session.setAttribute("hrm_subCompanyId", subcompanyid);
            }
            //人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
            int hrmdetachable = 0;
            if (session.getAttribute("hrmdetachable") != null) {
                hrmdetachable = Util.getIntValue(String.valueOf(session.getAttribute("hrmdetachable")), 0);
            } else {
                boolean isUseHrmManageDetach = new ManageDetachComInfo().isUseHrmManageDetach();
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
            int operatelevel = -1;
            if (hrmdetachable == 1) {
                operatelevel = new CheckSubCompanyRight().ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", Integer.parseInt(subcompanyid));
            } else {
                if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentId)) {
                    operatelevel = 2;
                }
            }
            if (operatelevel > 0) {
            } else if (isSelf) {
                return getResourceContactForm(request, response);
            }

            Map<String, Object> buttons = new Hashtable<String, Object>();
            if (((isSelf&&isHasModify(-1))|| operatelevel > 0) && !status.equals("10")) {//编辑
                buttons.put("hasEdit", true);
                buttons.put("hasSave", true);
            }
            result.put("hasJobTitlesAdd", HrmUserVarify.checkUserRight("HrmJobTitlesAdd:Add", user));
            result.put("buttons", buttons);
            result.put("conditions", getFormFields(request, response, false));
            result.put("validate", getValidate(user));
            if (user.getUID()!=1 && "1".equals(isOpen)){
                result.put("isSet","true");
            }

        } catch (Exception e) {
            writeLog(e);
        }

        Map<String, Object> retmap = new HashMap<String, Object>();
        retmap.put("result", result);
        return JSONObject.toJSONString(retmap);
    }

    /**
     * 人员基本信息表单字段
     *
     * @param request
     * @param response
     * @return
     */
    public List<Object> getFormFields(HttpServletRequest request, HttpServletResponse response, boolean isAdd) {
        //qc20230816
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        List<Object> lsGroup = new ArrayList<Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            Map<String, Object> groupitem = null;
            List<Object> itemlist = null;
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();

            String id = Util.null2String(request.getParameter("id"));
            if (id.length() == 0) {
                id = "" + user.getUID();
            }
            UserPrivacyComInfo upc = new UserPrivacyComInfo();
            PrivacyComInfo pc = new PrivacyComInfo();
            PrivacyBaseComInfo privacyBaseComInfo = new PrivacyBaseComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            Map<String, String> mapShowTypes = pc.getMapShowTypes();
            Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();
            //总体的大开关的默认设置
            Map<String, String> mapBaseShowTypeDefaults = privacyBaseComInfo.getMapShowTypeDefaults();

            StaticObj staticobj = StaticObj.getInstance();
            String multilanguage = (String) staticobj.getObject("multilanguage");
            if (multilanguage == null) multilanguage = "n";
            boolean isMultilanguageOK = false;
            if (multilanguage.equals("y")) isMultilanguageOK = true;
            boolean flagaccount = GCONST.getMOREACCOUNTLANDING();
            int viewattr = isAdd ? 2 : Util.getIntValue(request.getParameter("viewattr"), 1);
            int scopeId = -1;
            HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            hrmFieldSearchConditionComInfo.setIsMobile(Util.null2String(request.getParameter("ismobile")));
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
            hfm.isReturnDecryptData(true);
            CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
            hfm.getHrmData(Util.getIntValue(id));
            cfm.getCustomData(Util.getIntValue(id));

            while (HrmFieldGroupComInfo.next()) {
                int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
                if (grouptype != scopeId) continue;
                int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
                int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
                hfm.getCustomFields(groupid);
                //if(hfm.getGroupCount()==0)continue;

                groupitem = new HashMap<String, Object>();
                itemlist = new ArrayList<Object>();
                groupitem.put("title", SystemEnv.getHtmlLabelName(grouplabel, user.getLanguage()));
                groupitem.put("defaultshow", true);
                groupitem.put("hide", hfm.getGroupCount() == 0 || !Util.null2String(HrmFieldGroupComInfo.getIsShow()).equals("1"));
                groupitem.put("items", itemlist);
                lsGroup.add(groupitem);
                while (hfm.next()) {
                    int tmpviewattr = viewattr;
                    String fieldName = hfm.getFieldname();

                    String cusFieldname = "";
                    String fieldValue = "";
                    if (hfm.isBaseField(fieldName)) {
                        fieldValue = hfm.getHrmData(fieldName);
                    } else {
                        cusFieldname = "customfield" + hfm.getFieldid();
                        if (isAdd) cusFieldname = "customfield_0_" + hfm.getFieldid();
                        fieldValue = Util.null2String(new EncryptUtil().decrypt("cus_fielddata","field" + hfm.getFieldid(),"HrmCustomFieldByInfoType",""+scopeId,cfm.getData("field" + hfm.getFieldid()),true,true));
                    }

                    if (!hfm.isUse()) {
                        HrmFieldBean hrmFieldBean = new HrmFieldBean();
                        hrmFieldBean.setFieldname(cusFieldname.length() > 0 ? cusFieldname : fieldName);
                        hrmFieldBean.setFieldhtmltype("1");
                        hrmFieldBean.setType("1");
                        if (!isAdd) {
                        hrmFieldBean.setFieldvalue(fieldValue);
						}
                        hrmFieldBean.setIsFormField(true);
                        Map<String, Object> otherParams = new HashMap<String, Object>();
                        otherParams.put("hide", true);
                        hrmFieldBean.setOtherparam(otherParams);
                        SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
                        itemlist.add(searchConditionItem);
                        continue;
                    }
                    if (fieldName.equals("resourceimageid")) {
                        groupitem.put("hasResourceImage", true);
                        if (fieldValue.equals("0")) fieldValue = "";
                        if(fieldValue.length()>0){
                            fieldValue = DocDownloadCheckUtil.checkPermission(fieldValue,user);
                        }
                    }
                    if (fieldName.equals("loginid") || fieldName.equals("jobactivity") || fieldName.equals("departmentvirtualids")||fieldName.equals("messagerurl"))
                        continue;
                    if (fieldName.equalsIgnoreCase("jobGroupId")) {
                        continue;
                    }
                    if (fieldName.equals("systemlanguage") && !isMultilanguageOK) continue;
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
                    if (hrmFieldBean.getFieldname().equals("departmentid")) {
                        hrmFieldBean.setFieldvalue(Util.null2String(request.getParameter("departmentid")));
                    }
                    if (!isAdd) {
                        hrmFieldBean.setFieldvalue(fieldValue);
                    }
                    hrmFieldBean.setIsFormField(true);

                    if (fieldName.equals("mobile") || fieldName.equals("email") ||
                            fieldName.equals("fax") || fieldName.equals("telephone") || fieldName.equals("mobilecall")|| fieldName.equals("workcode")) {
                        hrmFieldBean.setMultilang(false);
                    }

                    if (((String) hrmFieldConf.get("ismand")).equals("1")) {
                        if (hrmFieldBean.getFieldname().equals("managerid")) {

                        } else {
                            tmpviewattr = 3;
                        }
                    }

                    if (!isAdd && hrmFieldBean.getFieldname().equals("status")) {//状态不能编辑
                        tmpviewattr = 1;
                    }

                    if (fieldName.equals("belongto") && flagaccount) {
                        tmpviewattr = 3;
                    }
                    String rules = "";
                    if(tmpviewattr==3){
                        rules = "required|string";
                    }
                    hrmFieldBean.setRules(rules);
                    SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);

                    if (searchConditionItem == null) continue;
//                    if (fieldName.equals("sex")) {
//                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
//                        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(28473, user.getLanguage()), true));
//                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(28474, user.getLanguage())));
//                        searchConditionItem.setOptions(options);
//                    }
//                    if (fieldName.equals("accounttype")) {
//                        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
//                        options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(17746, user.getLanguage()), true));
//                        options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(17747, user.getLanguage())));
//                        searchConditionItem.setOptions(options);
//                    }
                    if (fieldName.equals("belongto") && flagaccount) {
                        XssUtil xssUtil = new XssUtil();
                        String accountSql = "(accounttype=0 or accounttype=null or accounttype is null)";
                        searchConditionItem.getBrowserConditionParam().getDataParams().put("sqlwhere", xssUtil.put(accountSql));
                        searchConditionItem.getBrowserConditionParam().getCompleteParams().put("sqlwhere", xssUtil.put(accountSql));
                        searchConditionItem.getBrowserConditionParam().getDestDataParams().put("sqlwhere", xssUtil.put(accountSql));
                    }
                    if (searchConditionItem.getBrowserConditionParam() != null) {
                        searchConditionItem.getBrowserConditionParam().setViewAttr(tmpviewattr);
                        if (hrmFieldBean.getFieldname().equals("departmentid")) {
                            searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "HrmResourceAdd:Add");
                            searchConditionItem.getBrowserConditionParam().getCompleteParams().put("rightStr", "HrmResourceAdd:Add");
                        }
                    }
                    searchConditionItem.setViewAttr(tmpviewattr);
                    if (isAdd && fieldName.equals("status")) {
                        List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
                        statusOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(15710, user.getLanguage()), true));
                        statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(15711, user.getLanguage())));
                        statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(480, user.getLanguage())));
                        searchConditionItem.setOptions(statusOptions);
                    }
                    //qc20230816 字段修改权限按照建模表来决定
                    if (user.getUID()!=1 && "1".equals(isOpen) ){
                        int setUserId = Integer.parseInt(id);
                        checkSetUtil.searchConditionItem(user.getUID(),fieldName,0,searchConditionItem,setUserId);

                    }

                    itemlist.add(searchConditionItem);

                    if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                        String mobileShowSet = Util.null2String(mapShowSets.get(fieldName));
                        String mobileShowType = Util.null2String(mapShowTypes.get(fieldName));
                        String mapShowTypeDefault = Util.null2String(mapShowTypeDefaults.get(fieldName));

                        String baseMobileShowTypeDefaults = Util.null2String(mapBaseShowTypeDefaults.get(fieldName));

                        if (mobileShowSet.equals("1")) {
//								  userid+"__"+ptype
                            String comPk = id + "__" + fieldName;
                            fieldValue = Util.null2String(upc.getPvalue(comPk));
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
                                if (fieldValue.length() > 0) {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage()), fieldValue.equals("1")));
                                } else if (baseMobileShowTypeDefaults.indexOf("1") != -1) {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(2161, user.getLanguage())));
                                }
                            }
                            if (mobileShowType.indexOf("2") != -1) {
                                if (fieldValue.length() > 0) {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage()), fieldValue.equals("2")));
                                } else if (baseMobileShowTypeDefaults.indexOf("2") != -1) {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(32670, user.getLanguage())));
                                }
                            }
                            if (mobileShowType.indexOf("3") != -1) {
                                if (fieldValue.length() > 0) {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage()), fieldValue.equals("3")));
                                } else if (baseMobileShowTypeDefaults.indexOf("3") != -1) {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage()), true));
                                } else {
                                    statusOptions.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(32671, user.getLanguage())));
                                }
                            }
                            searchConditionItem.setValue(fieldValue);
                            searchConditionItem.setOptions(statusOptions);
                            itemlist.add(searchConditionItem);
                        }
                    }

//						if(fieldName.equals("mobile")){
//						  String mobileShowSet = Util.null2String(settings.getMobileShowSet());
//						  String mobileShowType = Util.null2String(settings.getMobileShowType());
//						  if(mobileShowSet.equals("1")){
//						  	String sql = "";
//						  	RecordSet rs = new RecordSet();
//						  	sql = "select costcenterid,mobileshowtype from HrmResource where id = "+Util.getIntValue(id,-1);
//						  	rs.executeSql(sql);
//						  	if(rs.next()){
//						  		fieldValue = Util.null2String(rs.getString("mobileshowtype"));
//						  	}
//						  	if(isAdd){
//						  		fieldValue = Util.null2String(settings.getMobileShowTypeDefault());
//						  	}
//								hrmFieldBean = new HrmFieldBean();
//								hrmFieldBean.setFieldname("mobileshowtype");
//								hrmFieldBean.setFieldlabel("32684");
//								hrmFieldBean.setFieldhtmltype("5");
//								hrmFieldBean.setType("1");
//								hrmFieldBean.setIssystem("1");
//								hrmFieldBean.setIsFormField(true);
//								searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
//								if(searchConditionItem==null)continue;
//					  		List<SearchConditionOption> statusOptions = new ArrayList<SearchConditionOption>();
//					  		if(mobileShowType.indexOf("1")!=-1){
//					  			statusOptions.add(new SearchConditionOption("1",SystemEnv.getHtmlLabelName(2161,user.getLanguage()),fieldValue.equals("1")));
//							 	}if(mobileShowType.indexOf("2")!=-1){
//							 		statusOptions.add(new SearchConditionOption("2",SystemEnv.getHtmlLabelName(32670,user.getLanguage()),fieldValue.equals("2")));
//							 	}if(mobileShowType.indexOf("3")!=-1){
//							 		statusOptions.add(new SearchConditionOption("3",SystemEnv.getHtmlLabelName(32671,user.getLanguage()),fieldValue.equals("3")));
//							 	}
//								searchConditionItem.setOptions(statusOptions);
//								itemlist.add(searchConditionItem);
//						  }
//						}
                }
                if (itemlist.size() == 0) lsGroup.remove(groupitem);
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return lsGroup;
    }

    /**
     * 查看人员基本信息
     *
     * @param request
     * @param response
     * @return
     */
    public String getResourceBaseView(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        try {
            HrmListValidate HrmListValidate = new HrmListValidate();
            User user = HrmUserVarify.getUser(request, response);
            String id = Util.null2String(request.getParameter("id"));
            if (id.length() == 0) {
                id = "" + user.getUID();
            }
            AppDetachComInfo AppDetachComInfo = new AppDetachComInfo();
            if (AppDetachComInfo.isUseAppDetach()) {
                boolean isApp = true;
                if(!AppDetachComInfo.getScopeIds(user, "resource",id)) {
                    isApp = false;
                }
                if (!isApp) {
                    String errMsg = SystemEnv.getHtmlLabelName(2012, user.getLanguage());
                    HashMap<String, String> jsondata = new HashMap<String, String>();
                    jsondata.put("errMsg", errMsg);
                    jsondata.put("status", "-1");
                    return JSONObject.toJSONString(jsondata);
                }
            }

            if (id.equals("")) id = String.valueOf(user.getUID());
            String status = "";
            String subcompanyid = "", departmentId = "";
            int scopeId = -1;
            RecordSet rs = new RecordSet();
            HttpSession session = request.getSession(true);
            rs.executeSql("select subcompanyid1, status, departmentId from hrmresource where id = " + id);
            if (rs.next()) {
                status = Util.toScreen(rs.getString("status"), user.getLanguage());
                subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
                departmentId = StringUtil.vString(rs.getString("departmentId"));
                if (subcompanyid == null || subcompanyid.equals("") || subcompanyid.equalsIgnoreCase("null"))
                    subcompanyid = "-1";
                session.setAttribute("hrm_subCompanyId", subcompanyid);
            }

            boolean isSelf = false;
            if (id.equals("" + user.getUID())) {
                isSelf = true;
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
            Map<String, Object> buttons = new Hashtable<String, Object>();

            //qc20230816 这里放开修改按钮
            Boolean setButtonBasic = checkSetUtil.isSetButtonBasic(request, response, user);
            if (user.getUID() == 1){
                setButtonBasic = true;
            }
            if (((isSelf && isHasModify(-1)) || operatelevel > 0|| "1".equals(isOpen)) && !status.equals("10") && setButtonBasic) {
                buttons.put("hasEdit", true);
                buttons.put("hasSave", true);
            }
            retmap.put("buttons", buttons);

            RecordSet RecordSet = new RecordSet();
            SubCompanyComInfo SubCompanyComInfo = new SubCompanyComInfo();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
            JobActivitiesComInfo jobActivitiesComInfo = new JobActivitiesComInfo();
            LocationComInfo locationComInfo = new LocationComInfo();
            LanguageComInfo languageComInfo = new LanguageComInfo();
            HrmResourceFile hrmResourceFile = new HrmResourceFile();
            HrmFieldGroupComInfo hrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            ResourceBelongtoComInfo resourceBelongtoComInfo = new ResourceBelongtoComInfo();
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
            CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);

            String sql = "";

            List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
            Map<String, Object> jsondata = null;

            Map<String, Object> item1 = new HashMap<String, Object>();//姓名等头部信息lastname,workcode,sex,orginfo
            Map<String, Object> item2 = new HashMap<String, Object>();//账号信息accounttype,accounts,managerid,subordinatescount,status,lastlogindate
            Map<String, Object> item3 = new HashMap<String, Object>();//系统登录维护信息createrid,createdate,lastmoddate,lastmodid
            grouplist.add(item1);
            grouplist.add(item2);
            grouplist.add(item3);
            List<Object> itemlist1 = new ArrayList<Object>();
            List<Object> itemlist2 = new ArrayList<Object>();
            List<Object> itemlist3 = new ArrayList<Object>();
            item1.put("title", ""+ SystemEnv.getHtmlLabelName(10004508,weaver.general.ThreadVarLanguage.getLang())+"");
            item1.put("id", "item1");
            item1.put("defaultshow", true);
            item1.put("items", itemlist1);

            item2.put("title", ""+ SystemEnv.getHtmlLabelName(10004509,weaver.general.ThreadVarLanguage.getLang())+"");
            item2.put("id", "item2");
            item2.put("defaultshow", true);
            item2.put("items", itemlist2);

            item3.put("title", SystemEnv.getHtmlLabelName(2023, user.getLanguage()));
            item3.put("id", "item3");
            item3.put("defaultshow", true);
            item3.put("items", itemlist3);

            PrivacyComInfo pc = new PrivacyComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            Map<String, String> mapShowTypes = pc.getMapShowTypes();
            Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();

            UserPrivacyComInfo upc = new UserPrivacyComInfo();
            //基本信息
            sql = "select * from hrmresource where id=" + id;
            rs.executeSql(sql);
            if (rs.next()) {
                String lastname = Util.toScreen(rs.getString("lastname"), user.getLanguage());
                String workcode = Util.toScreen(rs.getString("workcode"), user.getLanguage());
                String sex = Util.toScreen(rs.getString("sex"), user.getLanguage());
                subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
                String departmentid = Util.toScreen(rs.getString("departmentid"), user.getLanguage());
                int accounttype = Util.getIntValue(rs.getString("accounttype"), 0);
                String managerid = Util.toScreen(rs.getString("managerid"), user.getLanguage());
                String assistantid = Util.toScreen(rs.getString("assistantid"), user.getLanguage());
                status = Util.toScreen(rs.getString("status"), user.getLanguage());
                String lastlogindate = Util.toScreen(rs.getString("lastlogindate"), user.getLanguage());
                String createrid = Util.toScreen(rs.getString("createrid"), user.getLanguage());
                String createdate = Util.toScreen(rs.getString("createdate"), user.getLanguage());
                String lastmoddate = Util.toScreen(rs.getString("lastmoddate"), user.getLanguage());
                String lastmodid = Util.toScreen(rs.getString("lastmodid"), user.getLanguage());
                String jobtitle = Util.toScreen(rs.getString("jobtitle"), user.getLanguage());
                String jobcall = Util.toScreen(rs.getString("jobcall"), user.getLanguage());
                String joblevel = Util.toScreen(rs.getString("joblevel"), user.getLanguage());
                String jobactivitydesc = Util.toScreen(rs.getString("jobactivitydesc"), user.getLanguage());
                String locationid = Util.toScreen(rs.getString("loactionid"), user.getLanguage());
                String systemlanguage = Util.toScreen(rs.getString("systemlanguage"), user.getLanguage());
                String mobile = resourceComInfo.getMobileShow(id, user);
                String fax = Util.toScreen(rs.getString("fax"), user.getLanguage());
                String telephone = Util.toScreen(rs.getString("telephone"), user.getLanguage());
                String email = Util.toScreen(rs.getString("email"), user.getLanguage());
                String mobilecall = Util.toScreen(rs.getString("mobilecall"), user.getLanguage());
                String workroom = Util.toScreen(rs.getString("workroom"), user.getLanguage());
                String resourceimageid = Util.getFileidOut(rs.getString("resourceimageid"));
                String messagerurl = Util.null2String(rs.getString("messagerurl"));
                String ismobile = Util.null2String(request.getParameter("ismobile"));
                /*最后登录时间已存储至另一张表*/
                String lastLoginDateSql = "select * from userlastlogindate where userId=?";
                RecordSet recordSet = new RecordSet();
                recordSet.executeQuery(lastLoginDateSql, id);
                if (recordSet.next()) {
                    lastlogindate = recordSet.getString("lastLoginDate");
                }

                lastname = lastname.endsWith("\\") && !lastname.endsWith("\\\\") == true ? lastname + "\\" : lastname;
                lastname = StringUtil.replace(lastname, "'", "\\\\'");

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "lastname");
                jsondata.put("label", SystemEnv.getHtmlLabelName(413, user.getLanguage()));
                jsondata.put("value", lastname);
                itemlist1.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "messagerurl");
                jsondata.put("label", SystemEnv.getHtmlLabelName(24513, user.getLanguage()));
                jsondata.put("value", resourceComInfo.getMessagerUrls(id));
                itemlist1.add(jsondata);


                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "workcode");
                jsondata.put("label", SystemEnv.getHtmlLabelName(714, user.getLanguage()));
                jsondata.put("value", workcode);
                itemlist1.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "sex");
                jsondata.put("label", SystemEnv.getHtmlLabelName(416, user.getLanguage()));
                jsondata.put("sex", sex);//0：男性，1：女性
                if (sex.equals("0")) {
                    jsondata.put("value", SystemEnv.getHtmlLabelName(28473, user.getLanguage()));
                } else if (sex.equals("1")) {
                    jsondata.put("value", SystemEnv.getHtmlLabelName(28474, user.getLanguage()));
                }
                itemlist1.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "orginfo");
                jsondata.put("label", SystemEnv.getHtmlLabelName(376, user.getLanguage()));
                jsondata.put("value", DepartmentComInfo.getAllParentDepartmentNames(departmentid, subcompanyid));
                itemlist1.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "resourceimageid");
                jsondata.put("label", SystemEnv.getHtmlLabelName(15707, user.getLanguage()));
                String url = "";
                if (!resourceimageid.equals("0") && resourceimageid.length() > 0 && HrmListValidate.isValidate(36)) {
                    String mainControlHost = Util.null2String(Prop.getInstance().getPropValue("Others", "MAINCONTROLHOST"));
                    url = mainControlHost + "/weaver/weaver.file.FileDownload?fileid=" + resourceimageid;
                } else {
                    if (sex.equals("") || sex.equals("0")) {
                        url = "/images/messageimages/temp/man_wev8.png";
                    } else if (sex.equals("1")) {
                        url = "/images/messageimages/temp/women_wev8.png";
                    }
                }
                jsondata.put("value", url);
                itemlist2.add(jsondata);

                jsondata = new HashMap<String, Object>();
                Map<String, Object> options = new HashMap<String, Object>();
                jsondata.put("options", options);
                itemlist2.add(jsondata);

                //var data = { "lastname": "吕益", "mobile": "13899998888","telephone":"021-99998888","email":"lvyi@weaver.com","jobtitle":"开发工程师","department":"人力资源组","locationname":"上海"};
                options.put("lastname", lastname);
                options.put("mobile", mobile);
                options.put("telephone", telephone);
                options.put("email", email);
                options.put("jobtitle", jobTitlesComInfo.getJobTitlesname(joblevel));
                options.put("department", DepartmentComInfo.getDepartmentname(departmentid));
                options.put("locationname", locationComInfo.getLocationname(departmentid));


                List<Object> lsOption = new ArrayList<Object>();
                jsondata = new HashMap<String, Object>();

                jsondata.put("options", lsOption);
                itemlist2.add(jsondata);
                Map<String, Object> option = null;

                option = new HashMap<String, Object>();//发消息
                option.put("id", id);
                option.put("validateId", "33");
                option.put("name", "sendEmessage");
                option.put("funname", "sendEmessage");
                option.put("title", SystemEnv.getHtmlLabelName(127379, user.getLanguage()));
                lsOption.add(option);

                option = new HashMap<String, Object>();//发送短信
                option.put("id", id);
                option.put("validateId", "31");
                option.put("name", "openmessage");
                option.put("funname", "sendSmsMessage");
                option.put("title", SystemEnv.getHtmlLabelName(16635, user.getLanguage()));
                lsOption.add(option);

                option = new HashMap<String, Object>();//发送邮件
                option.put("id", id);
                option.put("validateId", "19");
                option.put("name", "openemail");
                option.put("funname", "sendMail");
                option.put("title", SystemEnv.getHtmlLabelName(2051, user.getLanguage()));
                lsOption.add(option);

                option = new HashMap<String, Object>();//新建日程
                option.put("id", id);
                option.put("validateId", "32");
                option.put("name", "doAddWorkPlan");
                option.put("funname", "doAddWorkPlanByHrm");
                option.put("title", SystemEnv.getHtmlLabelName(18481, user.getLanguage()));
                lsOption.add(option);

                List<Map<String, Object>> ls = new ArrayList<Map<String, Object>>();
                jsondata = new HashMap<String, Object>();
                jsondata.put("accountinfo", ls);
                itemlist2.add(jsondata);
                String value = "";
                String showName = "";
                List accounts = new VerifyLogin().getAccountsById(Integer.valueOf(id).intValue());
                Iterator iter = null;
                if (accounts != null) iter = accounts.iterator();
                Account current = new Account();
                while (iter != null && iter.hasNext()) {
                    Account a = (Account) iter.next();
                    if (("" + a.getId()).equals(id))
                        current = a;
                }
                if (GCONST.getMOREACCOUNTLANDING()) {
                    jsondata = new HashMap<String, Object>();
                    jsondata.put("name", "accounttype");
                    jsondata.put("label", SystemEnv.getHtmlLabelName(17745, user.getLanguage()));
                    jsondata.put("value", accounttype == 0 ? SystemEnv.getHtmlLabelName(17746, user.getLanguage()) : SystemEnv.getHtmlLabelName(17747, user.getLanguage()));
                    ls.add(jsondata);

                    List lsUser = resourceBelongtoComInfo.getBelongtousers("" + id);
                    if (lsUser != null && lsUser.size() > 0 && current.getType() == 0) {
                        jsondata = new HashMap<String, Object>();
                        jsondata.put("name", "accounts");
                        jsondata.put("label", SystemEnv.getHtmlLabelName(17747, user.getLanguage()));
                        jsondata.put("value", "<a href='"+ GCONST.getContextPath()+"/spa/hrm/index_mobx.html#/main/hrm/query?superid="+id+"' target='_blank'>" + lsUser.size()+"</a>");
                        ls.add(jsondata);
                    }

                    if (current.getType() == 1) {
                        jsondata = new HashMap<String, Object>();
                        jsondata.put("name", "belongto");
                        jsondata.put("label", SystemEnv.getHtmlLabelName(17746, Util.getIntValue(user.getLanguage())));
                        jsondata.put("value", resourceComInfo.getBelongTo(id));
                        jsondata.put("showName", resourceComInfo.getLastname(resourceComInfo.getBelongTo(id)));
                        jsondata.put("isOpenHrm", true);
                        ls.add(jsondata);
                    }

//					if(current.getType()==1){
//						jsondata = new HashMap<String, Object>();
//						jsondata.put("name","accounts");
//						jsondata.put("label",SystemEnv.getHtmlLabelNames("17747,141,124",user.getLanguage()));
//						iter=accounts.iterator();
//						while(accounts.size()>1&&iter.hasNext()){
//							Account a=(Account)iter.next();
//							if(a.getType()==0){
//								String subcompanyname=SubCompanyComInfo.getSubCompanyname(""+a.getSubcompanyid());
//								String departmentname=DepartmentComInfo.getDepartmentname(""+a.getDepartmentid());
//								String jobtitlename=jobTitlesComInfo.getJobTitlesname(""+a.getJobtitleid());
//         				//<a href="/hrm/resource/HrmResource.jsp?id=<%=a.getId()%>" target="_blank"><%=subcompanyname+"/"+departmentname+"/"+jobtitlename%></a>
//         			}
//						}
//						jsondata.put("value",""+accounts.size());
//						ls.add(jsondata);
//					}
                }

                if (Util.null2String(managerid).length() > 0) {
                    value = managerid;
                    showName = resourceComInfo.getLastname(managerid);
                    if (showName.length() > 0) {
                        jsondata = new HashMap<String, Object>();
                        jsondata.put("name", "managerid");
                        jsondata.put("label", SystemEnv.getHtmlLabelName(15709, user.getLanguage()));
                        jsondata.put("value", value);
                        jsondata.put("showName", showName);
                        ls.add(jsondata);
                    }
                }

                RecordSet.executeProc("HrmResource_SCountBySubordinat", id);
                RecordSet.next();
                int subordinatescount = RecordSet.getInt(1);
                if (subordinatescount > 0) {
                    jsondata = new HashMap<String, Object>();
                    jsondata.put("name", "subordinatescount");
                    jsondata.put("label", SystemEnv.getHtmlLabelName(442, user.getLanguage()));
                    jsondata.put("value", "<A href="+ GCONST.getContextPath()+"/spa/hrm/index_mobx.html#/main/hrm/underling/" + id + " target=_blank>" + subordinatescount + "</a>");
                    ls.add(jsondata);
                }

                if (Util.null2String(assistantid).length() > 0 && hfm.isUse("assistantid")) {
                    value = assistantid;
                    showName = resourceComInfo.getLastname(assistantid);
                    if (showName.length() > 0) {
                        jsondata = new HashMap<String, Object>();
                        jsondata.put("name", "assistantid");
                        jsondata.put("label", SystemEnv.getHtmlLabelName(441, user.getLanguage()));
                        jsondata.put("value", value);
                        jsondata.put("showName", showName);
                        ls.add(jsondata);
                    }
                }

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "status");
                jsondata.put("label", SystemEnv.getHtmlLabelName(602, user.getLanguage()));
                jsondata.put("value", HrmFieldUtil.getResourceStatusName(status, user));
                ls.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "lastlogindate");
                jsondata.put("label", SystemEnv.getHtmlLabelName(16067, user.getLanguage()));
                jsondata.put("value", lastlogindate);
                ls.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "createrid");
                jsondata.put("label", SystemEnv.getHtmlLabelName(882, user.getLanguage()));
                value = "";
                showName = "";
                if (Util.null2String(createrid).length() > 0) {
                    value = createrid;
                    showName = resourceComInfo.getLastname(createrid);
                }
                jsondata.put("value", value);
                jsondata.put("showName", showName);
                ls.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "createdate");
                jsondata.put("label", SystemEnv.getHtmlLabelName(1339, user.getLanguage()));
                jsondata.put("value", createdate);
                ls.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "lastmodid");
                jsondata.put("label", SystemEnv.getHtmlLabelName(3002, user.getLanguage()));
                value = "";
                showName = "";
                if (Util.null2String(lastmodid).length() > 0) {
                    value = lastmodid;
                    showName = resourceComInfo.getLastname(lastmodid);
                }
                jsondata.put("value", value);
                jsondata.put("showName", showName);
                ls.add(jsondata);

                jsondata = new HashMap<String, Object>();
                jsondata.put("name", "lastmoddate");
                jsondata.put("label", SystemEnv.getHtmlLabelName(19521, user.getLanguage()));
                jsondata.put("value", lastmoddate);
                ls.add(jsondata);


                Map<String, Object> item = new HashMap<String, Object>();
                List<Object> itemlist = new ArrayList<Object>();
                grouplist.add(item);
                item.put("title", SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
                item.put("defaultshow", true);
                item.put("items", itemlist);

                //qc20230816 岗位字段特殊设置
                int selectUserId = Integer.parseInt(id);
                if ("1".equals(isOpen)&&checkSelectUtil.fieldShowCheck(user.getUID(),"jobtitle",0,selectUserId) || user.getUID() == 1){
                    jsondata = new HashMap<String, Object>();
                    jsondata.put("label", SystemEnv.getHtmlLabelName(6086, user.getLanguage()));
                    jsondata.put("type", "position");
                    jsondata.put("value", "<A href="+ GCONST.getContextPath()+"/spa/hrm/engine.html#/hrmengine/posts?id=" + jobtitle + " target=_blank>" + Util.toScreen(jobTitlesComInfo.getJobTitlesmark(jobtitle), user.getLanguage()) + "</a>");
                    itemlist.add(jsondata);
                }


                boolean hasbaseGroup = false;
                rs.executeSql(" select count(*) from hrm_formfield where fieldid = 0 and isuse =1 ");
                if (rs.next()) {
                    if (rs.getInt(1) > 0) hasbaseGroup = true;
                }
                if (hasbaseGroup) {
                    if ("1".equals(isOpen)&&checkSelectUtil.fieldShowCheck(user.getUID(),"jobactivity",0,selectUserId) || user.getUID() == 1) {
                        jsondata = new HashMap<String, Object>();
                        jsondata.put("label", SystemEnv.getHtmlLabelName(1915, user.getLanguage()));
                        jsondata.put("value", Util.toScreen(jobActivitiesComInfo.getJobActivitiesmarks(jobTitlesComInfo.getJobactivityid(jobtitle)), user.getLanguage()));
                        itemlist.add(jsondata);
                    }
                }

                //其他特殊字段
                hfm.getCustomFields(1);//取得基本信息
                hfm.getHrmData(Util.getIntValue(id));
                cfm.getCustomData(Util.getIntValue(id));
                while (hfm.next()) {
                    if (!hfm.isUse()) continue;
                    int fieldlabel = Util.getIntValue(hfm.getLable());
                    String fieldName = hfm.getFieldname();

                    //qc20230816 控制字段显示
                    if (user.getUID()!=1 && "1".equals(isOpen)&&!checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,0,selectUserId)){
                        continue;
                    }

                    int fieldId = hfm.getFieldid();
                    if (fieldName.equals("loginid") || fieldName.equals("workcode") || fieldName.equals("lastname") || fieldName.equals("sex") ||
                            fieldName.equals("accounttype") || fieldName.equals("belongto") || fieldName.equals("departmentid") ||
                            fieldName.equals("jobtitle") || (!ismobile.equals("1") && fieldName.equals("status")) || fieldName.equals("resourceimageid") || fieldName.equals("messagerurl") || fieldName.equals("jobactivity") || fieldName.equalsIgnoreCase("jobGroupId")) {
                        continue;
                    }
                    int type = hfm.getType();
                    String dmlurl = hfm.getDmrUrl();
                    int fieldhtmltype = Util.getIntValue(hfm.getHtmlType());
                    String fieldValue = "";
                    if (hfm.isBaseField(fieldName)) {
                        fieldValue = hfm.getHrmData(fieldName);
                    } else {
                        fieldValue = Util.null2String(new EncryptUtil().decrypt("cus_fielddata","field" + hfm.getFieldid(),"HrmCustomFieldByInfoType",""+scopeId,cfm.getData("field" + hfm.getFieldid()),false,true));
                    }

                    if (hfm.getHtmlType().equals("3")) {
                        fieldValue = hfm.getHtmlBrowserFieldvalue(user, dmlurl, fieldId, fieldhtmltype, type, fieldValue, "0");
                    } else {
                        fieldValue = hfm.getFieldvalue(user, dmlurl, fieldId, fieldhtmltype, type, fieldValue, Util.getIntValue(request.getParameter("ismobile")));
                    }

                    if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                        String mobileShowSet = Util.null2String(mapShowSets.get(fieldName));
                        String showTypeDefault = Util.null2String(mapShowTypeDefaults.get(fieldName));
                        if (mobileShowSet.equals("1")) {
                            String comPk = id + "__" + fieldName;
                            String comPkValue = Util.null2String(upc.getPvalue(comPk));
                            if (comPkValue.length() > 0) {
                                fieldValue = upc.getShow(id, user, fieldName, fieldValue, comPkValue);
                            } else {
                                fieldValue = pc.getShow(id, user, fieldName, fieldValue, showTypeDefault);
                            }
                        } else {
                            fieldValue = pc.getShow(id, user, fieldName, fieldValue, showTypeDefault);
                        }
                    }

                    if (fieldName.equals("mobile")) {//手机号码格式化
                        fieldValue = ResourceComInfo.formatMobile(fieldValue);
                    }

                    jsondata = new HashMap<String, Object>();
                    jsondata.put("label", SystemEnv.getHtmlLabelName(fieldlabel, user.getLanguage()));
                    jsondata.put("value", fieldValue);
                    itemlist.add(jsondata);
                }

                hrmFieldGroupComInfo.setTofirstRow();
                hfm.beforeFirst();
                cfm.beforeFirst();
                while (hrmFieldGroupComInfo.next()) {
                    int grouptype = Util.getIntValue(hrmFieldGroupComInfo.getType());
                    int groupid = Util.getIntValue(hrmFieldGroupComInfo.getid());
                    if (grouptype != scopeId || groupid == 1) continue;
                    int grouplabel = Util.getIntValue(hrmFieldGroupComInfo.getLabel());

                    hfm.getCustomFields(groupid);
                    if (hfm.getGroupCount() == 0) continue;

                    if (groupid == 3) {
                        boolean tmp_flag = false;
                        while (hfm.next()) {
                            if (!hfm.isUse()) continue;
                            String fieldName = hfm.getFieldname();
                            if (fieldName.equals("managerid") || fieldName.equals("assistantid")) {
                                continue;
                            }
                            tmp_flag = true;
                            break;
                        }
                        if (!tmp_flag) continue;
                        hfm.beforeFirst();
                    }
                    item = new HashMap<String, Object>();
                    itemlist = new ArrayList<Object>();
                    grouplist.add(item);
                    item.put("title", SystemEnv.getHtmlLabelName(grouplabel, user.getLanguage()));
                    item.put("defaultshow", true);
                    item.put("hide", !Util.null2String(hrmFieldGroupComInfo.getIsShow()).equals("1"));
                    item.put("items", itemlist);
                    if (grouplabel == 32946) {
                        item.put("SupSub", true);
                    }
                    while (hfm.next()) {
                        if (!hfm.isUse()) continue;
                        int fieldlabel = Util.getIntValue(hfm.getLable());
                        String fieldName = hfm.getFieldname();
                        //qc20230816 控制字段显示
                        if (user.getUID()!=1 && "1".equals(isOpen)&&!checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,0,selectUserId))continue;

                        int fieldId = hfm.getFieldid();
                        int type = hfm.getType();
                        String dmlurl = hfm.getDmrUrl();
                        int fieldhtmltype = Util.getIntValue(hfm.getHtmlType());
                        String fieldValue = "";

                        if (fieldName.equals("managerid") || fieldName.equals("assistantid")) {
                            continue;
                        }

                        if (hfm.isBaseField(fieldName)) {
                            fieldValue = hfm.getHrmData(fieldName);
                        } else {
                            fieldValue = Util.null2String(new EncryptUtil().decrypt("cus_fielddata","field" + hfm.getFieldid(),"HrmCustomFieldByInfoType",""+scopeId,cfm.getData("field" + hfm.getFieldid()),false,true));
                        }

                        if (mapShowSets != null && mapShowSets.get(fieldName) != null) {
                            String mobileShowSet = Util.null2String(mapShowSets.get(fieldName));
                            String showTypeDefault = Util.null2String(mapShowTypeDefaults.get(fieldName));
                            if (mobileShowSet.equals("1")) {
                                String comPk = id + "__" + fieldName;
                                String comPkValue = Util.null2String(upc.getPvalue(comPk));
                                if (comPkValue.length() > 0) {
                                    fieldValue = upc.getShow(id, user, fieldName, fieldValue, comPkValue);
                                } else {
                                    fieldValue = pc.getShow(id, user, fieldName, fieldValue, showTypeDefault);
                                }
                            } else {
                                fieldValue = pc.getShow(id, user, fieldName, fieldValue, showTypeDefault);
                            }
                        }

                        //一些特殊处理
                        if (fieldName.equals("mobile")) {//手机号码格式化
                            fieldValue = ResourceComInfo.formatMobile(fieldValue);
                        }

                        if (hfm.getHtmlType().equals("3")) {
                            fieldValue = hfm.getHtmlBrowserFieldvalue(user, dmlurl, fieldId, fieldhtmltype, type, fieldValue, "0");
                        } else {
                            fieldValue = hfm.getFieldvalue(user, dmlurl, fieldId, fieldhtmltype, type, fieldValue, Util.getIntValue(request.getParameter("ismobile")));
                        }

                        if (fieldName.equals("email")) {
                            fieldValue = "<A href=\"mailto:" + fieldValue + "\" target=\"_blank\">" + fieldValue + "</A>";
                        }
                        if (fieldName.equals("locationid")) {
                            fieldValue = "<A href=\""+ GCONST.getContextPath()+"/hrm/location/HrmLocationEdit.jsp?id=" + locationid + "\" target=\"_blank\">" + fieldValue + "</a>";
                        }

                        jsondata = new HashMap<String, Object>();
                        jsondata.put("label", SystemEnv.getHtmlLabelName(fieldlabel, user.getLanguage()));
                        jsondata.put("value", fieldValue);
                        if (fieldName.equals("email")) {
                            jsondata.put("type", "email");//邮件(手机端标识)
                        }
                        if (fieldName.equals("mobile")) {
                            jsondata.put("type", "mobile");//手机号码(手机端标识)
                        }
                        if (fieldName.equals("telephone")) {
                            jsondata.put("type", "telephone");//电话号码(手机端标识)
                        }
                        if (fieldName.equals("mobilecall")) {
                            jsondata.put("type", "mobilecall");//其他电话(手机端标识)
                        }
                        itemlist.add(jsondata);
                    }
                }

                retmap.put("validate", getValidate(user));
                retmap.put("result", grouplist);
                /*手机端需要用到的分部ID和部门ID*/
                retmap.put("subcompanyId", resourceComInfo.getSubCompanyID(id));//分部ID
                retmap.put("departmentId", resourceComInfo.getSubCompanyID(id));//部门ID
            }
        } catch (Exception e) {
            writeLog(e);
        }

        return JSONObject.toJSONString(retmap);
    }

    public Map<String, Object> getValidate(User user) {
        Map<String, Object> retmap = new LinkedHashMap<String, Object>();
        try {
            HrmListValidate hrmListValidate = new HrmListValidate();
            Map<String, Object> map = null;

            /*手机端显示的顺序必须是：发送消息、发送短信、发送邮件*/
            map = new HashMap<String, Object>();
            map.put("isValidate", hrmListValidate.isValidate(33) && HrmValidate.hasEmessage(user));
            map.put("validateName", SystemEnv.getHtmlLabelName(513224, user.getLanguage()));
            map.put("mobileUse", true);
            retmap.put("33", map);

            String[] validateIds = new String[]{"31", "19", "32", "36", "37", "38"};
            String[] validateTitles = new String[]{"16635", "2051", "18481", "15707", "30184", "83704"};
            for (int i = 0; i < validateIds.length; i++) {
                String validateId = validateIds[i];
                map = new HashMap<String, Object>();
                map.put("isValidate", hrmListValidate.isValidate(Util.getIntValue(validateIds[i])));
                map.put("validateName", SystemEnv.getHtmlLabelNames(validateTitles[i], user.getLanguage()));
                if (validateId.equals("19") || validateId.equals("31") || validateId.equals("32")) {
                    map.put("mobileUse", true);
                }
                retmap.put(validateIds[i], map);
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return retmap;
    }

    /**
     * 新建人员基本信息
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, String> addResourceBase(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> retmap = new HashMap<String, String>();
        try {
            retmap.put("id", "0");
            User user = HrmUserVarify.getUser(request, response);
            LN LN = new LN();
            RecordSet rs = new RecordSet();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            SalaryManager SalaryManager = new SalaryManager();
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();
            SysRemindWorkflow SysRemindWorkflow = new SysRemindWorkflow();
            CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();
            DepartmentVirtualComInfo DepartmentVirtualComInfo = new DepartmentVirtualComInfo();

            String para = "";
            FileUpload fu = new FileUpload(request);
            String cmd = Util.null2String(fu.getParameter("cmd"));
            String id = Util.null2String(fu.getParameter("id"));
            String workcode = Util.fromScreen3(fu.getParameter("workcode"), user.getLanguage());
            String lastname = Util.fromScreen3(fu.getParameter("lastname"), user.getLanguage()).trim();
            String sex = Util.fromScreen3(fu.getParameter("sex"), user.getLanguage());
            //String resourceimageid= Util.null2String(fu.uploadFiles("photoid"));
            String resourceimageid = Util.null2String(fu.getParameter("resourceimageid"));
            if (resourceimageid.length() > 0) {
                resourceimageid = "" + DocDownloadCheckUtil.getDownloadfileidstr(resourceimageid);
            }
            String departmentid = Util.fromScreen3(fu.getParameter("departmentid"), user.getLanguage());
            String costcenterid = Util.fromScreen3(fu.getParameter("costcenterid"), user.getLanguage());
            String jobtitle = Util.fromScreen3(fu.getParameter("jobtitle"), user.getLanguage());
            String joblevel = Util.fromScreen3(fu.getParameter("joblevel"), user.getLanguage());
            String jobactivitydesc = Util.fromScreen3(fu.getParameter("jobactivitydesc"), user.getLanguage());
            String managerid = Util.fromScreen3(fu.getParameter("managerid"), user.getLanguage());
            String assistantid = Util.fromScreen3(fu.getParameter("assistantid"), user.getLanguage());
            String status = Util.fromScreen3(fu.getParameter("status"), user.getLanguage());
            String locationid = Util.fromScreen3(fu.getParameter("locationid"), user.getLanguage());
            String workroom = Util.fromScreen3(fu.getParameter("workroom"), user.getLanguage());
            String telephone = Util.fromScreen3(fu.getParameter("telephone"), user.getLanguage());
            String mobile = Util.fromScreen3(fu.getParameter("mobile"), user.getLanguage());
            String mobileshowtype = Util.fromScreen3(fu.getParameter("mobileshowtype"), user.getLanguage());
            String mobilecall = Util.fromScreen3(fu.getParameter("mobilecall"), user.getLanguage());
            String fax = Util.fromScreen3(fu.getParameter("fax"), user.getLanguage());
            String jobcall = Util.fromScreen3(fu.getParameter("jobcall"), user.getLanguage());
            String email = Util.fromScreen3(fu.getParameter("email"), user.getLanguage());
            String dsporder = Util.fromScreen3(fu.getParameter("dsporder"), user.getLanguage());
            String accounttype = Util.fromScreen3(fu.getParameter("accounttype"), user.getLanguage());
            String systemlanguage = Util.null2String(fu.getParameter("systemlanguage"));
            if (systemlanguage.equals("") || systemlanguage.equals("0")) systemlanguage = "7";
            String belongto = Util.fromScreen3(fu.getParameter("belongto"), user.getLanguage());
			//应聘人员id
            String rcid = Util.null2String(fu.getParameter("rcId"));

            CheckItemBean mobileBean = new CheckItemBean("mobile", mobile, id);
            ValidateFieldManager.validate(mobileBean);
            if (!mobileBean.isPass()) {
                retmap.put("status", "-1");
                retmap.put("message", mobileBean.getCheckMsg());
                return retmap;
            }

            CheckItemBean telephoneBean = new CheckItemBean("telephone", telephone, id);
            ValidateFieldManager.validate(telephoneBean);
            if (!telephoneBean.isPass()) {
                retmap.put("status", "-1");
                retmap.put("message", telephoneBean.getCheckMsg());
                return retmap;
            }


            if (dsporder.length() == 0) dsporder = id;
            if (accounttype.equals("0")) {
                belongto = "-1";
            }
            String departmentvirtualids = Util.null2String(fu.getParameter("departmentvirtualids"));//虚拟部门id;

            //Td9325,解决多账号次账号没有登陆Id在浏览框组织结构中无法显示的问题。
            boolean falg = false;
            String loginid = "";
            if (accounttype.equals("1")) {
                rs.executeSql("select loginid from HrmResource where id =" + belongto);
                if (rs.next()) {
                    loginid = rs.getString("loginid");
                }
                if (!loginid.equals("")) {
                    String maxidsql = "select max(id) as id from HrmResource where loginid like '" + loginid + "%'";
                    rs.executeSql(maxidsql);
                    if (rs.next()) {
                        loginid = loginid + (rs.getInt("id") + 1);
                        falg = true;
                    }
                }
            }
            rs.executeProc("HrmResourceMaxId_Get", "");
            rs.next();
            id = "" + rs.getInt(1);
            if (!"".equals(rcid)) {
                id = rcid;
            }
            SimpleBizLogger logger = new SimpleBizLogger();
            Map<String, Object> params = ParamUtil.request2Map(request);
            BizLogContext bizLogContext = new BizLogContext();
            bizLogContext.setLogType(BizLogType.HRM);//模块类型
            bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
            bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
            bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_BASE);//当前小类型
            bizLogContext.setOperateType(BizLogOperateType.ADD);
            bizLogContext.setParams(params);//当前request请求参数
            logger.setUser(user);//当前操作人
            String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType", -1, "b");
            String mainSql = "select a.*" + (cusFieldNames.length() > 0 ? "," + cusFieldNames : "") + " from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=-1 where a.id=" + id;
            logger.setMainSql(mainSql, "id");//主表sql
            logger.setMainPrimarykey("id");//主日志表唯一key
            logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
            logger.before(bizLogContext);//写入操作前日志

            String sql = "select managerstr, seclevel from HrmResource where id = " + Util.getIntValue(managerid);
            rs.executeSql(sql);
            String managerstr = "";
            String seclevel = "";
            while (rs.next()) {
                String tmp_managerstr = rs.getString("managerstr");
                /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 begin***********/
                if (!tmp_managerstr.startsWith(",")) tmp_managerstr = "," + tmp_managerstr;
                if (!tmp_managerstr.endsWith(",")) tmp_managerstr = tmp_managerstr + ",";
                /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 end ***********/
                managerstr += tmp_managerstr;
                managerstr = "," + managerid + managerstr;
                managerstr = managerstr.endsWith(",") ? managerstr : (managerstr + ",");
                seclevel = rs.getString("seclevel");
            }

            String subcmpanyid1 = DepartmentComInfo.getSubcompanyid1(departmentid);
            RecordSetTrans rst = new RecordSetTrans();
            rst.setAutoCommit(false);
            try {
                if(resourceimageid.length()==0)resourceimageid="null";
                if(costcenterid.length()==0)costcenterid="null";
                if(managerid.length()==0)managerid="null";
                if(assistantid.length()==0)assistantid="null";
                if(accounttype.length()==0)accounttype="null";
                if(belongto.length()==0)belongto="null";
                if(jobcall.length()==0)jobcall="null";
                if(mobileshowtype.length()==0)mobileshowtype="null";
                if(rst.getDBType().equalsIgnoreCase("postgresql"))
                {
                    if(joblevel.length()==0)joblevel=null;
                    if(dsporder.length()==0)dsporder=null;
                }
                workcode = CodeRuleManager.getCodeRuleManager().generateRuleCode(RuleCodeType.USER, subcmpanyid1, departmentid, jobtitle, workcode);
                para = "" + id + separator + workcode + separator + lastname + separator + sex + separator + resourceimageid + separator +
                        departmentid + separator + costcenterid + separator + jobtitle + separator + joblevel + separator + jobactivitydesc + separator +
                        managerid + separator + assistantid + separator + status + separator + locationid + separator + workroom + separator + telephone +
                        separator + mobile + separator + mobilecall + separator + fax + separator + jobcall + separator + subcmpanyid1 + separator + managerstr +
                        separator + accounttype + separator + belongto + separator + systemlanguage + separator + email + separator + dsporder + separator + mobileshowtype;
                
                rst.executeProc("HrmResourceBasicInfo_Insert", para);
                if(Util.null2String(locationid).length()>0) {
                    rst.executeSql("update hrmresource set countryid=(select countryid from HrmLocations where id=" + locationid + "),"
                      + DbFunctionUtil.getInsertUpdateSetSql(rst.getDBType(), user.getUID()) + " where id=" + id);
                }
                String logidsql = "", quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(lastname);
                if (falg) {
                    logidsql = "update HrmResource set loginid = ?, pinyinlastname = ?, ecology_pinyin_search = ? where id = ?";
                    rst.executeUpdate(logidsql, loginid, quickSearchStr, quickSearchStr, id);
                } else {
                    logidsql = "update HrmResource set pinyinlastname = ?, ecology_pinyin_search = ? where id = ?";
                    rst.executeUpdate(logidsql, quickSearchStr, quickSearchStr, id);
                }
                if (seclevel == null || "".equals(seclevel)) {
                    seclevel = "0";
                }
                rst.commit();
            } catch (Exception e) {
                rst.rollback();
                e.printStackTrace();
            }

            boolean formdefined = false;
            weaver.system.CusFormSettingComInfo CusFormSettingComInfo = new weaver.system.CusFormSettingComInfo();
            weaver.system.CusFormSetting CusFormSetting = CusFormSettingComInfo.getCusFormSetting("hrm", "HrmResourceBase");
            if (CusFormSetting != null) {
                if (CusFormSetting.getStatus() == 2) {
                    //自定义布局页面
                    formdefined = true;
                }
            }
            int userid = user.getUID();
            String userpara = "" + userid + separator + today;
            para = "" + id;
            for (int i = 0; i < 5; i++) {
                int idx = i;
                if (formdefined) idx++;
                String datefield = Util.null2String(fu.getParameter("datefield" + idx));
                String numberfield = "" + Util.getDoubleValue(fu.getParameter("numberfield" + idx), 0);
                String textfield = Util.null2String(fu.getParameter("textfield" + idx));
                String tinyintfield = "" + Util.getIntValue(fu.getParameter("tinyintfield" + idx), 0);
                para += separator + datefield + separator + numberfield + separator + textfield + separator + tinyintfield;
            }
            rs.executeProc("HrmResourceDefine_Update", para);
            rs.executeProc("HrmResource_CreateInfo", "" + id + separator + userpara + separator + userpara);

            //421944 用户自定义隐私设置

            UserPrivacyComInfo upc = new UserPrivacyComInfo();
            PrivacyComInfo pc = new PrivacyComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            String insertSql = "";
            rs = new RecordSet();
            try {
                String deletePriSql = " delete from userprivacysetting where userid= '" + id + "'";
                rs.executeSql(deletePriSql);
                for (Map.Entry<String, String> me : mapShowSets.entrySet()) {
                    String fieldName = me.getKey();
                    String fieldVal = Util.null2String(mapShowSets.get(fieldName));
                    if (fieldVal.equals("1")) {
                        String tmpPK = id + "__" + fieldName;
                        String tmpPvalue = Util.null2String(fu.getParameter(fieldName + "showtype"));
                        insertSql = "insert into userprivacysetting (combinedid,userid,ptype,pvalue) values('" + tmpPK + "','" + id + "','" + fieldName + "','" + tmpPvalue + "')";
                        rs.executeSql(insertSql);
                    }
                }
                upc.removeUserPrivacyCache();
            } catch (Exception e) {
                e.printStackTrace();
            }


            // 改为只进行该人缓存信息的添加
            ResourceComInfo.addResourceInfoCache(id);
            SalaryManager.initResourceSalary(id);

            para = "" + id + separator + managerid + separator + departmentid + separator + subcmpanyid1 + separator + "0" + separator + managerstr;
            rs.executeProc("HrmResource_Trigger_Insert", para);

            String sql_1 = ("insert into HrmInfoStatus (itemid,hrmid) values(1," + id + ")");
            rs.executeSql(sql_1);
            String sql_2 = ("insert into HrmInfoStatus (itemid,hrmid) values(2," + id + ")");
            rs.executeSql(sql_2);
            String sql_3 = ("insert into HrmInfoStatus (itemid,hrmid) values(3," + id + ")");
            rs.executeSql(sql_3);
            String sql_10 = ("insert into HrmInfoStatus (itemid,hrmid) values(10," + id + ")");
            rs.executeSql(sql_10);

            String name = lastname;
            String CurrentUser = "" + user.getUID();
            String CurrentUserName = "" + user.getUsername();
            String SWFAccepter = "";
            String SWFTitle = "";
            String SWFRemark = "";
            String SWFSubmiter = "";
            String Subject = "";
            Subject = SystemEnv.getHtmlLabelName(15670, user.getLanguage());
            Subject += ":" + name;

            //modifier by lvyi 2013-12-31
            if (settings.getEntervalid().equals("1")) {//入职提醒
                String thesql = "select  hrmids from HrmInfoMaintenance where id<4 or id = 10";
                rs.executeSql(thesql);
                String members = "";
		    /*while(rs.next()){
				int hrmid_tmp = Util.getIntValue(rs.getString("hrmids"));//TD9392
				if(hrmid_tmp > 0 && user.getUID() != hrmid_tmp){
					members += ","+rs.getString("hrmids");
				}
		    }*/
                while (rs.next()) {
                    String hrmid_tmp = Util.null2String(rs.getString("hrmids"));//TD9392
                    if (hrmid_tmp.length() != 0) {
                        members += "," + rs.getString("hrmids");
                    }
                }
                if (!members.equals("")) {
                    members = members.substring(1);
                    members = duplicateRemoval(members, user.getUID() + "");
                    SWFAccepter = members;
                    SWFTitle = SystemEnv.getHtmlLabelName(15670, user.getLanguage());
                    SWFTitle += ":" + name;
                    SWFTitle += "-" + CurrentUserName;
                    SWFTitle += "-" + today;
//		        SWFRemark="<a href=/hrm/employee/EmployeeManage.jsp?hrmid="+id+">"+Util.fromScreen2(Subject,user.getLanguage())+"</a>";//支持E9弹窗
                    SWFRemark = "<a class='wea-hrm-new-employee-set'  onClick=\"openHrmNewEmployeeSetDialog(" + id + ")\"  style=\"cursor:pointer;\"  id = '" + id + "'>" + Util.fromScreen2(Subject, user.getLanguage()) + "</a>";
                    SWFSubmiter = CurrentUser;
                    SysRemindWorkflow.setPrjSysRemind(SWFTitle, 0, Util.getIntValue(SWFSubmiter), SWFAccepter, SWFRemark);
                }
            }

            CustomFieldTreeManager.editCustomDataE9Add("HrmCustomFieldByInfoType", -1, fu, Util.getIntValue(id, 0));
            //应聘人员的个人信息18条
            if (!"".equals(rcid)) {
                sql = "select * from HrmCareerApply where id = ?";
                rs.executeQuery(sql,id);
                if (rs.next()) {
                    String birthday = Util.null2String(rs.getString("birthday"));
                    String folk = Util.null2String(rs.getString("folk"));
                    String nativeplace = Util.null2String(rs.getString("nativeplace"));
                    String regresidentplace = Util.null2String(rs.getString("regresidentplace"));

                    String certificatenum = Util.null2String(rs.getString("certificatenum"));
                    String maritalstatus = Util.null2String(rs.getString("maritalstatus"));
                    String policy = Util.null2String(rs.getString("policy"));
                    String bememberdate = Util.null2String(rs.getString("bememberdate"));

                    String bepartydate = Util.null2String(rs.getString("bepartydate"));
                    String islabouunion = Util.null2String(rs.getString("islabouunion"));
                    String educationlevel = Util.null2String(rs.getString("educationlevel"));
                    String degree = Util.null2String(rs.getString("degree"));

                    String healthinfo = Util.null2String(rs.getString("healthinfo"));

                    String height = Util.null2String(rs.getString("height"));
                    if (height.indexOf(".") != -1) height = height.substring(0, height.indexOf("."));
                    String weight = Util.null2String(rs.getString("weight"));
                    if (weight.indexOf(".") != -1) weight = weight.substring(0, weight.indexOf("."));

                    String residentplace = Util.null2String(rs.getString("residentplace"));
                    String homeaddress = Util.null2String(rs.getString("homeaddress"));
                    String tempresidentnumber = Util.null2String(rs.getString("tempresidentnumber"));

                    para = "" + id + separator + birthday + separator + folk + separator + nativeplace + separator + regresidentplace + separator + maritalstatus + separator + policy + separator + bememberdate + separator + bepartydate + separator + islabouunion + separator + educationlevel + separator + degree + separator + healthinfo + separator + height + separator + weight + separator + residentplace + separator + homeaddress + separator + tempresidentnumber + separator + certificatenum;
                    RecordSet rs1 = new RecordSet();
                    rs1.executeProc("HrmResourcePersonalInfo_Insert", para);
                }
            }
            //更新虚拟组织部门id
            if (departmentvirtualids.length() > 0) {
                //保存前先删除需要删除的数据，因为有managerid 所以不能全部删除再保存
                sql = "delete from hrmresourcevirtual where resourceid=" + id + " and departmentid not in (" + departmentvirtualids + ")";
                rs.executeSql(sql);

                String[] departmentvirtualid = departmentvirtualids.split(",");
                for (int i = 0; departmentvirtualid != null && i < departmentvirtualid.length; i++) {
                    rs.executeSql(" select count(*) from HrmResourceVirtual where departmentid ='" + departmentvirtualid[i] + "' and resourceid = " + id);
                    if (rs.next()) {
                        //如果已存在 无需处理
                        if (rs.getInt(1) > 0) continue;
                    }

                    //写入
                    int tmpid = 0;
                    rs.executeSql("select max(id) from HrmResourceVirtual ");
                    if (rs.next()) {
                        tmpid = rs.getInt(1) + 1;
                    }
                    String subcompanyid = DepartmentVirtualComInfo.getSubcompanyid1(departmentvirtualid[i]);
                    sql = " insert into HrmResourceVirtual (id,resourceid,subcompanyid,departmentid ) " +
                            " values (" + tmpid + "," + id + "," + subcompanyid + "," + departmentvirtualid[i] + ")";
                    rs.executeSql(sql);
                }
            }

            LogUtil.writeBizLog(logger.getBizLogContexts());

            HrmFaceCheckManager.sync(id, HrmFaceCheckManager.getOptInsert(), "hrm_e9_HrmResourceBaseService_addResourceBase", HrmFaceCheckManager.getOaResource());
            //同步RTX端的用户信息.
            new OrganisationCom().checkUser(Util.getIntValue(id));
            new Thread(new OrganisationComRunnable("user", "add", "" + id)).start();
            ResourceComInfo.updateResourceInfoCache("" + id);
            new PluginUserCheck().clearPluginUserCache("messager");
            //OA与第三方接口单条数据同步方法开始
            new HrmServiceManager().SynInstantHrmResource("" + id, "1");
            //OA与第三方接口单条数据同步方法结束

            //新增人员实时同步到CoreMail邮件系统
            //CoreMailAPI.synUser(id);
            if (cmd.equals("SaveAndNew")) {
                retmap.put("status", "1");
            } else if (cmd.equals("SaveAndNext")) {
                retmap.put("status", "2");
            } else {
                retmap.put("status", "3");
            }
            retmap.put("id", id);
        } catch (Exception e) {
            writeLog("新建人员基本信息错误：" + e);
            retmap.put("status", "-1");
        }
        return retmap;
    }

    /**
     * 编辑人员基本信息
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> editContactInfo(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            RecordSet rs = new RecordSet();
            String sql = "";
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            SysMaintenanceLog SysMaintenanceLog = new SysMaintenanceLog();
            HrmServiceManager HrmServiceManager = new HrmServiceManager();
            String para = "";

            FileUpload fu = new FileUpload(request);
            String id = Util.null2String(fu.getParameter("id"));
            if (user.getUID() != Util.getIntValue(id)) {//本人才能修改
                retmap.put("status", "-1");
                retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
                return retmap;
            }


            LN LN = new LN();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();
            CrmShareBase CrmShareBase = new CrmShareBase();

            String mobile = Util.fromScreen3(fu.getParameter("mobile"), user.getLanguage());
            String telephone = Util.fromScreen3(fu.getParameter("telephone"), user.getLanguage());

            CheckItemBean mobileBean = new CheckItemBean("mobile", mobile, id);
            ValidateFieldManager.validate(mobileBean);
            if (!mobileBean.isPass()) {
                retmap.put("status", "-1");
                retmap.put("message", mobileBean.getCheckMsg());
                return retmap;
            }
            CheckItemBean telephoneBean = new CheckItemBean("telephone", telephone, id);
            ValidateFieldManager.validate(telephoneBean);
            if (!telephoneBean.isPass()) {
                retmap.put("status", "-1");
                retmap.put("message", telephoneBean.getCheckMsg());
                return retmap;
            }


            SimpleBizLogger logger = new SimpleBizLogger();
            Map<String, Object> params = ParamUtil.request2Map(request);
            BizLogContext bizLogContext = new BizLogContext();
            bizLogContext.setLogType(BizLogType.HRM);//模块类型
            bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
            bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
            bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_BASE);//当前小类型
            bizLogContext.setParams(params);//当前request请求参数
            logger.setUser(user);//当前操作人
            String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType", -1, "b");
            String mainSql = "select a.*" + (cusFieldNames.length() > 0 ? "," + cusFieldNames : "") + " from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=-1 where a.id=" + id;
            logger.setMainSql(mainSql, "id");//主表sql
            logger.setMainPrimarykey("id");//主日志表唯一key
            logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
            logger.before(bizLogContext);//吸入操作前日志

            String updateSql = "";
            List<String> sqlParams = new ArrayList<String>() ;
            RecordSetTrans rst = new RecordSetTrans();
            HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
            HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
            HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", -1);
            boolean flagaccount = GCONST.getMOREACCOUNTLANDING();
            String accounttype = "";
            String belongto = Util.fromScreen3(fu.getParameter("belongto"), user.getLanguage());
            String oldbelongto = "";
            String dsporder = Util.fromScreen3(fu.getParameter("dsporder"), user.getLanguage());;  //显示顺序为空需要和id保持一致
            if (dsporder.length() == 0) dsporder = id;
            String locationid = "";  //设置了办公地点需要更新人员的国家
            String loginid = "";  //次帐号的登录名需要和主账号+id保持一致
            boolean falg = false;
            while (HrmFieldGroupComInfo.next()) {
                int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
                if (grouptype != -1) continue;
                int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
                hfm.getCustomFields(groupid);
                while (hfm.next()) {
                    String fieldName = hfm.getFieldname();
                    if (fieldName.equals("accounttype") && !flagaccount) continue;
                    if (fieldName.equals("belongto")) continue;
                    if (fieldName.equals("loginid") || fieldName.equals("jobactivity") || fieldName.equals("status") || fieldName.equals("departmentvirtualids")) continue;
                    if (fieldName.equals("messagerurl") || fieldName.equals("jobGroupId") || fieldName.equals("managerid") || fieldName.equals("departmentid")) continue;
                    //没有开启多语言即使开启了系统语言允许个人编辑也不行
                    if (!hfm.isUse() || !hfm.isModify() || !hfm.isBaseField(fieldName)) {
                        continue;
                    }else{
                        String updateValue = Util.fromScreen3(fu.getParameter(fieldName), user.getLanguage());
                        if("accounttype".equals(fieldName)){
                            accounttype = updateValue;
                            if (accounttype.equals("0")) {
                                belongto = "-1";
                            }
                            //Td9325,解决多账号次账号没有登陆Id在浏览框组织结构中无法显示的问题。
                            rs.executeQuery("select * from HrmResource where id = ? " ,id);
                            if (rs.next()) {
                                loginid = rs.getString("loginid");
                                float idsporder = rs.getFloat("dsporder");
                                if (idsporder <= 0) {
                                    dsporder = rs.getString("id");
                                } else {
                                    dsporder = "" + idsporder;
                                }
                            }

                            String thisAccounttype = rs.getString("accounttype");
                            if (thisAccounttype.equals("1") && updateValue.equals("0")) {
                                oldbelongto = rs.getString("belongto");
                            }
                            if (accounttype.equals("1") && loginid.equalsIgnoreCase("")) {
                                rs.executeSql("select loginid from HrmResource where id =" + belongto);
                                if (rs.next()) {
                                    loginid = rs.getString(1);
                                }
                                if (LN.CkHrmnum() >= 0) {
                                    retmap.put("status", "-1");
                                    return retmap;
                                }
                                if (!loginid.equals("")) {
                                    loginid = loginid + (id + 1);
                                    falg = true;
                                }
                            }
                            continue;
                        }
                        if("resourceimageid".equals(fieldName)){
                            String resourceimageid = updateValue;
                            if (resourceimageid.length() > 0) {
                                resourceimageid = "" + DocDownloadCheckUtil.getDownloadfileidstr(resourceimageid);
                            }
                            String resourceimageBase64 = Util.null2String(fu.getParameter("resourceimageBase64"));
                            if (resourceimageBase64.length() > 0) {
                                resourceimageid = ServiceUtil.saveResourceImage(resourceimageBase64);
                            }
                            String oldresourceimageid = StringUtil.vString(fu.getParameter("oldresourceimage"),"0");
                            if (resourceimageid.equals("")) resourceimageid = oldresourceimageid;
                            updateValue = resourceimageid;
						}
                        if("locationid".equals(fieldName)){
                            locationid = updateValue;
                        }
                        if("dsporder".equals(fieldName)){
                            updateValue = dsporder;
                        }
						if("systemlanguage".equals(fieldName)){
                            if (updateValue.equals("") || updateValue.equals("0")) {
								updateValue = "7";
							}
                        }
                        if(updateSql.length()>0)updateSql+=",";
                        updateSql+=(fieldName+" = ? ");
                        sqlParams.add(updateValue);
                    }
                }
            }
            if(accounttype.length()>0){
                if(updateSql.length()>0)updateSql+=",";
                updateSql+="accounttype = ?,belongto = ?";
                sqlParams.add(accounttype);
                sqlParams.add(belongto);
            }
            if(updateSql.length()>0){
                rst.setAutoCommit(false);
                try {
                    String sqltemp = "update hrmresource set "+updateSql+" where id = ? ";
                    sqlParams.add(id);
                    rst.executeUpdate(sqltemp,sqlParams);
                    if(Util.null2String(locationid).length()>0) {
                        rst.executeSql("update hrmresource set countryid=(select countryid from HrmLocations where id=" + locationid + ") where id=" + id);
                    }
                    if (falg) {
                        String logidsql = "update HrmResource set loginid = '" + loginid + "' where id = " + id;
                        rst.executeSql(logidsql);
                    }
                    rst.commit();
                } catch (Exception e) {
                    rst.rollback();
                    e.printStackTrace();
                }
            }

            //421944 用户自定义隐私设置

            UserPrivacyComInfo upc = new UserPrivacyComInfo();
            PrivacyComInfo pc = new PrivacyComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            String insertSql = "";
            rs = new RecordSet();
            try {
                String deletePriSql = " delete from userprivacysetting where userid= '" + id + "'";
                rs.executeSql(deletePriSql);
                for (Map.Entry<String, String> me : mapShowSets.entrySet()) {
                    String fieldName = me.getKey();
                    String fieldVal = Util.null2String(mapShowSets.get(fieldName));
                    if (fieldVal.equals("1")) {
                        String tmpPK = id + "__" + fieldName;
                        String tmpPvalue = Util.null2String(fu.getParameter(fieldName + "showtype"));
                        insertSql = "insert into userprivacysetting (combinedid,userid,ptype,pvalue) values('" + tmpPK + "','" + id + "','" + fieldName + "','" + tmpPvalue + "')";
                        rs.executeSql(insertSql);
                    }
                }
                upc.removeUserPrivacyCache();
            } catch (Exception e) {
                e.printStackTrace();
            }

            int userid = user.getUID();
            String userpara = "" + userid + separator + today;
            rs.executeProc("HrmResource_ModInfo", "" + id + separator + userpara);
      
            boolean formdefined = false;
            weaver.system.CusFormSettingComInfo CusFormSettingComInfo = new weaver.system.CusFormSettingComInfo();
            weaver.system.CusFormSetting CusFormSetting = CusFormSettingComInfo.getCusFormSetting("hrm", "HrmResourceBase");
            if (CusFormSetting != null) {
                if (CusFormSetting.getStatus() == 2) {
                    //自定义布局页面
                    formdefined = true;
                }
            }
            para = "" + id;
            for (int i = 0; i < 5; i++) {
                int idx = i;
                if (formdefined) idx++;
                String datefield = Util.null2String(fu.getParameter("datefield" + idx));
                String numberfield = "" + Util.getDoubleValue(fu.getParameter("numberfield" + idx), 0);
                String textfield = Util.null2String(fu.getParameter("textfield" + idx));
                String tinyintfield = "" + Util.getIntValue(fu.getParameter("tinyintfield" + idx), 0);
                para += separator + datefield + separator + numberfield + separator + textfield + separator + tinyintfield;
            }
            rs.executeProc("HrmResourceDefine_Update", para);

            new Thread(new OrganisationComRunnable("user", "editbasicinfo", id + "-" + (ResourceComInfo.getStatus(id)))).start();
            // 改为自进行修正
            ResourceComInfo.updateResourceInfoCache(id);
            //OA与第三方接口单条数据同步方法开始
            HrmServiceManager.SynInstantHrmResource(id, "2");
            //OA与第三方接口单条数据同步方法结束

            //处理自定义字段 add by wjy
            CustomFieldTreeManager.editCustomData("HrmCustomFieldByInfoType", -1, fu, Util.getIntValue(id, 0));

            //处理次账号修改为主账号时，检查次账号所属 主账号的 其他设置是否需要修改 add by kzw QC159888
            try {
                if (!oldbelongto.equals("")) {
                    HrmUserSettingComInfo userSetting = new HrmUserSettingComInfo();
                    String belongtoshow = userSetting.getBelongtoshowByUserId(oldbelongto);
                    if (belongtoshow.equals("1")) {
                        rs.executeSql("select id from hrmresource where belongto = " + oldbelongto);
                        if (!rs.next()) {
                            String setId = userSetting.getId(oldbelongto);
                            rs.executeSql("update HrmUserSetting set belongtoshow=0  where id=" + setId);
                            userSetting.removeHrmUserSettingComInfoCache();
                        }
                    }
                }
            } catch (Exception e) {
                writeLog(e.getMessage());
            }

            rs.execute("update HrmResource set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);
            rs.execute("update HrmResourceManager set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);

            //写入操作后日志
            LogUtil.writeBizLog(logger.getBizLogContexts());

            //修改人员实时同步到CoreMail邮件系统
            //CoreMailAPI.synUser(id);

            HrmFaceCheckManager.sync(id, HrmFaceCheckManager.getOptUpdate(), "hrm_e9_HrmResourceBaseService_editResourceBase", HrmFaceCheckManager.getOaResource());

            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog("编辑人员信息错误：" + e);
            retmap.put("status", "-1");
        }
        return retmap;
    }

    /**
     * 编辑人员基本信息
     *
     * @param request
     * @param response
     * @return
     */
    public Map<String, Object> editResourceBase(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        try {
            User user = HrmUserVarify.getUser(request, response);
            if (Util.null2String(request.getParameter("editcontact")).equals("1")) {
                return editContactInfo(request, response);
            }

            boolean canEdit = HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user);
            //qc20230816 这里放开修改权限
            if ("1".equals(isOpen)){
                canEdit = true;
            }
            if (!canEdit) {
                retmap.put("status", "-1");
                retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
                return retmap;
            }
            LN LN = new LN();
            RecordSet rs = new RecordSet();
            DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
            ResourceComInfo ResourceComInfo = new ResourceComInfo();
            CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();
            SysMaintenanceLog SysMaintenanceLog = new SysMaintenanceLog();
            HrmServiceManager HrmServiceManager = new HrmServiceManager();
            CrmShareBase CrmShareBase = new CrmShareBase();

            String para = "";
            FileUpload fu = new FileUpload(request);
            String id = Util.null2String(fu.getParameter("id"));
            if (id.length()==0) {
                retmap.put("status", "-1");
                retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
                return retmap;
            }
            String workcode = Util.fromScreen3(fu.getParameter("workcode"), user.getLanguage());
            String lastname = Util.fromScreen3(fu.getParameter("lastname"), user.getLanguage()).trim();
            String sex = Util.fromScreen3(fu.getParameter("sex"), user.getLanguage());
            String resourceimageid = Util.null2String(fu.getParameter("resourceimageid"));
            if(resourceimageid.length()>0) {
                resourceimageid = "" + DocDownloadCheckUtil.getDownloadfileidstr(resourceimageid);
            }
            String resourceimageBase64 = Util.null2String(fu.getParameter("resourceimageBase64"));
            if(resourceimageBase64.length()>0){
                resourceimageid = ServiceUtil.saveResourceImage(resourceimageBase64);
            }
            String oldresourceimageid = Util.null2String(fu.getParameter("oldresourceimage"));
            if (resourceimageid.equals("")) resourceimageid = oldresourceimageid;
            String departmentid = Util.fromScreen3(fu.getParameter("departmentid"), user.getLanguage());
            String costcenterid = Util.fromScreen3(fu.getParameter("costcenterid"), user.getLanguage());
            String jobtitle = Util.fromScreen3(fu.getParameter("jobtitle"), user.getLanguage());
            String joblevel = Util.fromScreen3(fu.getParameter("joblevel"), user.getLanguage());
            String jobactivitydesc = Util.fromScreen3(fu.getParameter("jobactivitydesc"), user.getLanguage());
            String managerid = Util.fromScreen3(fu.getParameter("managerid"), user.getLanguage());
            String assistantid = Util.fromScreen3(fu.getParameter("assistantid"), user.getLanguage());
            String status = Util.fromScreen3(fu.getParameter("status"), user.getLanguage());
            String locationid = Util.fromScreen3(fu.getParameter("locationid"), user.getLanguage());
            String workroom = Util.fromScreen3(fu.getParameter("workroom"), user.getLanguage());
            String telephone = Util.fromScreen3(fu.getParameter("telephone"), user.getLanguage());
            String mobile = Util.fromScreen3(fu.getParameter("mobile"), user.getLanguage());
            String mobileshowtype = Util.fromScreen3(fu.getParameter("mobileshowtype"), user.getLanguage());
            String mobilecall = Util.fromScreen3(fu.getParameter("mobilecall"), user.getLanguage());
            String fax = Util.fromScreen3(fu.getParameter("fax"), user.getLanguage());
            String email = Util.fromScreen3(fu.getParameter("email"), user.getLanguage());
            String dsporder = Util.fromScreen3(fu.getParameter("dsporder"), user.getLanguage());
            String jobcall = Util.fromScreen3(fu.getParameter("jobcall"), user.getLanguage());
            String systemlanguage = Util.fromScreen3(fu.getParameter("systemlanguage"), user.getLanguage());
            if (systemlanguage.equals("") || systemlanguage.equals("0")) {
                systemlanguage = "7";
            }
            String accounttype = Util.fromScreen3(fu.getParameter("accounttype"), user.getLanguage());
            String belongto = Util.fromScreen3(fu.getParameter("belongto"), user.getLanguage());
            if (dsporder.length() == 0) dsporder = id;
            if (accounttype.equals("0")) {
                belongto = "-1";
            }


            CheckItemBean mobileBean = new CheckItemBean("mobile", mobile, id);
            ValidateFieldManager.validate(mobileBean);
            if (!mobileBean.isPass()) {
                retmap.put("status", "-1");
                retmap.put("message", mobileBean.getCheckMsg());
                return retmap;
            }
            CheckItemBean telephoneBean = new CheckItemBean("telephone", telephone, id);
            ValidateFieldManager.validate(telephoneBean);
            if (!telephoneBean.isPass()) {
                retmap.put("status", "-1");
                retmap.put("message", telephoneBean.getCheckMsg());
                return retmap;
            }


            SimpleBizLogger logger = new SimpleBizLogger();
            Map<String, Object> params = ParamUtil.request2Map(request);
            BizLogContext bizLogContext = new BizLogContext();
            bizLogContext.setLogType(BizLogType.HRM);//模块类型
            bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
            bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(1361, user.getLanguage()));
            bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_BASE);//当前小类型
            bizLogContext.setParams(params);//当前request请求参数
            logger.setUser(user);//当前操作人
            String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType", -1, "b");
            String mainSql = "select a.*" + (cusFieldNames.length() > 0 ? "," + cusFieldNames : "") + " from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=-1 where a.id=" + id;
            logger.setMainSql(mainSql, "id");//主表sql
            logger.setMainPrimarykey("id");//主日志表唯一key
            logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
            logger.before(bizLogContext);//吸入操作前日志

            //Td9325,解决多账号次账号没有登陆Id在浏览框组织结构中无法显示的问题。
            String oldbelongto = "";
            boolean falg = false;
            String loginid = "";
            rs.executeSql("select * from HrmResource where id =" + id);
            if (rs.next()) {
                loginid = rs.getString("loginid");
                float idsporder = rs.getFloat("dsporder");
                if (idsporder <= 0) {
                    dsporder = rs.getString("id");
                } else {
                    dsporder = "" + idsporder;
                }

                String thisAccounttype = rs.getString("accounttype");
                if (thisAccounttype.equals("1") && accounttype.equals("0")) {
                    oldbelongto = rs.getString("belongto");
                }
            }

            if (accounttype.equals("1") && loginid.equalsIgnoreCase("")) {
                rs.executeSql("select loginid from HrmResource where id =" + belongto);
                if (rs.next()) {
                    loginid = rs.getString(1);
                }
                
                if (!loginid.equals("")) {
                    loginid = loginid + (id + 1);
                    falg = true;
                }
            }
            String sql = "select * from HrmResource where id = " + Util.getIntValue(id);
            rs.executeSql(sql);
            String oldmanagerid = "";
            String oldmanagerstr = "";
            while (rs.next()) {
                oldmanagerid = rs.getString("managerid");
                oldmanagerstr = rs.getString("managerstr");
                /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 begin***********/
                if (!oldmanagerstr.startsWith(",")) oldmanagerstr = "," + oldmanagerstr;
                if (!oldmanagerstr.endsWith(",")) oldmanagerstr = oldmanagerstr + ",";
                /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 end ***********/
            }
            //mysql报错问题java.sql.SQLException: Incorrect integer value: '' for column 'COSTCENTERID' at row 1
            if(resourceimageid.length()==0)resourceimageid="null";
            if(costcenterid.length()==0)costcenterid="null";
            if(managerid.length()==0)managerid="null";
            if(assistantid.length()==0)assistantid="null";
            if(accounttype.length()==0)accounttype="null";
            if(belongto.length()==0)belongto="null";
            if(jobcall.length()==0)jobcall="null";
            if(mobileshowtype.length()==0)mobileshowtype="null";
            DepartmentComInfo departmentComInfo = new DepartmentComInfo();
            String subcompanyid1 = departmentComInfo.getSubcompanyid1(departmentid);
//            workcode = CodeRuleManager.getCodeRuleManager().generateRuleCode(RuleCodeType.USER, subcompanyid1, departmentid, jobtitle, workcode);
            if (StringUtils.isNotEmpty(workcode)) {
                CodeRuleManager.getCodeRuleManager().checkReservedIfDel(RuleCodeType.USER.getValue(), workcode);
            }

                para = "" + id + separator + workcode + separator + lastname + separator + sex + separator + resourceimageid
                        + separator + departmentid + separator + costcenterid + separator + jobtitle + separator + joblevel
                        + separator + jobactivitydesc + separator + managerid + separator + assistantid + separator + status
                        + separator + locationid + separator + workroom + separator + telephone + separator + mobile
                        + separator + mobilecall + separator + fax + separator + jobcall + separator + systemlanguage
                        + separator + accounttype + separator + belongto + separator + email + separator + dsporder + separator + mobileshowtype;


            RecordSetTrans rst = new RecordSetTrans();
            rst.setAutoCommit(false);
            try {

                    rst.executeProc("HrmResourceBasicInfo_Update", para);
                if(Util.null2String(locationid).length()>0) {
                    rst.executeSql("update hrmresource set countryid=(select countryid from HrmLocations where id=" + locationid + ") where id=" + id);
                }
                String logidsql = "", quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(lastname);
                if (falg) {
                    logidsql = "update HrmResource set loginid = ?, pinyinlastname = ?, ecology_pinyin_search = ? where id = ?";
                    rst.executeUpdate(logidsql, loginid, quickSearchStr, quickSearchStr, id);
                } else {
                    logidsql = "update HrmResource set pinyinlastname = ?, ecology_pinyin_search = ? where id = ?";
                    rst.executeUpdate(logidsql, quickSearchStr, quickSearchStr, id);
                }
                rst.commit();
            } catch (Exception e) {
                rst.rollback();
                e.printStackTrace();
            }

            if(!"7".equals(systemlanguage) && StringUtils.isNotBlank(systemlanguage)){
                User.setUserLang(Util.getIntValue(id),Util.getIntValue(systemlanguage, 7));
            }

            //421944 用户自定义隐私设置

            UserPrivacyComInfo upc = new UserPrivacyComInfo();
            PrivacyComInfo pc = new PrivacyComInfo();
            Map<String, String> mapShowSets = pc.getMapShowSets();
            String insertSql = "";
            rs = new RecordSet();
            try {
                String deletePriSql = " delete from userprivacysetting where userid= '" + id + "'";
                rs.executeSql(deletePriSql);
                for (Map.Entry<String, String> me : mapShowSets.entrySet()) {
                    String fieldName = me.getKey();
                    String fieldVal = Util.null2String(mapShowSets.get(fieldName));
                    if (fieldVal.equals("1")) {
                        String tmpPK = id + "__" + fieldName;
                        String tmpPvalue = Util.null2String(fu.getParameter(fieldName + "showtype"));
                        insertSql = "insert into userprivacysetting (combinedid,userid,ptype,pvalue) values('" + tmpPK + "','" + id + "','" + fieldName + "','" + tmpPvalue + "')";
                        rs.executeSql(insertSql);
                    }
                }
                upc.removeUserPrivacyCache();
            } catch (Exception e) {
                e.printStackTrace();
            }

            int userid = user.getUID();
            String userpara = "" + userid + separator + today;
            rs.executeProc("HrmResource_ModInfo", "" + id + separator + userpara);
            String managerstr = "";
            if (!id.equals(managerid)) {
                sql = "select managerstr from HrmResource where id = " + Util.getIntValue(managerid);
                rs.executeSql(sql);
                while (rs.next()) {
                    managerstr = rs.getString("managerstr");
                    /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 begin***********/
                    if (!managerstr.startsWith(",")) managerstr = "," + managerstr;
                    if (!managerstr.endsWith(",")) managerstr = managerstr + ",";
                    /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 end ***********/
                    managerstr = "," + managerid + managerstr;
                    managerstr = managerstr.endsWith(",") ? managerstr : (managerstr + ",");
                }
            } else {
                managerstr = "," + managerid + ",";
            }

            rst = new RecordSetTrans();
            rst.setAutoCommit(false);
            try {
                para = "" + id + separator + managerstr;
                rst.executeProc("HrmResource_UpdateManagerStr", para);
                rst.commit();
            } catch (Exception e) {
                rst.rollback();
                e.printStackTrace();
            }

			managerid = Util.null2String(managerid).trim();
			oldmanagerid = Util.null2String(oldmanagerid).trim();
            if(!managerid.equals(oldmanagerid) && !(("".equals(managerid) || "0".equals(managerid)) && ("".equals(oldmanagerid) || "0".equals(oldmanagerid)))){
                String temOldmanagerstr = "," + id + oldmanagerstr;
                temOldmanagerstr = temOldmanagerstr.endsWith(",") ? temOldmanagerstr : (temOldmanagerstr + ",");
                sql = "select id,departmentid,subcompanyid1,managerid,seclevel,managerstr from HrmResource where managerstr like '%" + temOldmanagerstr + "'";
                rs.executeSql(sql);
                while (rs.next()) {
                    String nowmanagerstr = Util.null2String(rs.getString("managerstr"));
                    /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 begin***********/
                    if (!nowmanagerstr.startsWith(",")) nowmanagerstr = "," + nowmanagerstr;
                    if (!nowmanagerstr.endsWith(",")) nowmanagerstr = nowmanagerstr + ",";
                    /*********处理managerstr 不以逗号开始或者结束的情况 形如 managerstr：8 end ***********/
                    String resourceid = rs.getString("id");
                    //指定上级为自身的情况，不更新自身上级
                    if (id.equals(resourceid))
                        continue;
                    String nowmanagerstr2 = "";
                    int index = nowmanagerstr.lastIndexOf(oldmanagerstr);
                    if (index != -1) {
                        if (!"".equals(managerstr)) {
                            nowmanagerstr2 = nowmanagerstr.substring(0, index) + ("".equals(oldmanagerstr) ? managerstr.substring(1) : managerstr);
                        } else {
                            nowmanagerstr2 = nowmanagerstr.substring(0, index) + ("".equals(oldmanagerstr) ? "" : ",");
                        }
                    }
                    rst = new RecordSetTrans();
                    rst.setAutoCommit(false);
                    try {
                        para = resourceid + separator + nowmanagerstr2;
                        rst.executeProc("HrmResource_UpdateManagerStr", para);
                        rst.commit();
                        ResourceComInfo.updateResourceInfoCache(resourceid); //更新缓存
                    } catch (Exception e) {
                        rst.rollback();
                        e.printStackTrace();
                    }
                }
            }

            String subcmpanyid1 = DepartmentComInfo.getSubcompanyid1(departmentid);
            para = "" + id + separator + subcmpanyid1;
            rst = new RecordSetTrans();
            rst.setAutoCommit(false);
            try {
                rst.executeProc("HrmResource_UpdateSubCom", para);
                rst.commit();
            } catch (Exception e) {
                rst.rollback();
                e.printStackTrace();
            }

            if(!managerid.equals(oldmanagerid) && !(("".equals(managerid) || "0".equals(managerid)) && ("".equals(oldmanagerid) || "0".equals(oldmanagerid)))){//修改人力资源经理，对客户和日程共享重新计算
                CrmShareBase.setShareForNewManager(id);
            }
            boolean formdefined = false;
            weaver.system.CusFormSettingComInfo CusFormSettingComInfo = new weaver.system.CusFormSettingComInfo();
            weaver.system.CusFormSetting CusFormSetting = CusFormSettingComInfo.getCusFormSetting("hrm", "HrmResourceBase");
            if (CusFormSetting != null) {
                if (CusFormSetting.getStatus() == 2) {
                    //自定义布局页面
                    formdefined = true;
                }
            }
            para = "" + id;
            for (int i = 0; i < 5; i++) {
                int idx = i;
                if (formdefined) idx++;
                String datefield = Util.null2String(fu.getParameter("datefield" + idx));
                String numberfield = "" + Util.getDoubleValue(fu.getParameter("numberfield" + idx), 0);
                String textfield = Util.null2String(fu.getParameter("textfield" + idx));
                String tinyintfield = "" + Util.getIntValue(fu.getParameter("tinyintfield" + idx), 0);
                para += separator + datefield + separator + numberfield + separator + textfield + separator + tinyintfield;
            }
            rs.executeProc("HrmResourceDefine_Update", para);

            new Thread(new OrganisationComRunnable("user", "editbasicinfo", id + "-" + status)).start();
            // 改为自进行修正
            ResourceComInfo.updateResourceInfoCache(id);

            try {
                //OA与第三方接口单条数据同步方法开始
                HrmServiceManager.SynInstantHrmResource(id, "2");
                //OA与第三方接口单条数据同步方法结束
            } catch (Exception e) {
                this.writeLog("OA与第三方接口单条数据同步失败" + e);
            }

            //处理自定义字段 add by wjy
            CustomFieldTreeManager.editCustomData("HrmCustomFieldByInfoType", -1, fu, Util.getIntValue(id, 0));


            //处理次账号修改为主账号时，检查次账号所属 主账号的 其他设置是否需要修改 add by kzw QC159888
            try {
                if (!oldbelongto.equals("")) {
                    HrmUserSettingComInfo userSetting = new HrmUserSettingComInfo();
                    String belongtoshow = userSetting.getBelongtoshowByUserId(oldbelongto);
                    if (belongtoshow.equals("1")) {
                        rs.executeSql("select id from hrmresource where belongto = " + oldbelongto);
                        if (!rs.next()) {
                            String setId = userSetting.getId(oldbelongto);
                            rs.executeSql("update HrmUserSetting set belongtoshow=0  where id=" + setId);
                            userSetting.removeHrmUserSettingComInfoCache();
                        }
                    }
                }
            } catch (Exception e) {
                writeLog(e.getMessage());
            }

            rs.execute("update HrmResource set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);
            rs.execute("update HrmResourceManager set " + DbFunctionUtil.getUpdateSetSql(rs.getDBType(), user.getUID()) + " where id=" + id);

            //写入操作后日志
            LogUtil.writeBizLog(logger.getBizLogContexts());

            //修改人员实时同步到CoreMail邮件系统
            //CoreMailAPI.synUser(id);

            HrmFaceCheckManager.sync(id, HrmFaceCheckManager.getOptUpdate(), "hrm_e9_HrmResourceBaseService_editResourceBase", HrmFaceCheckManager.getOaResource());

            retmap.put("status", "1");
        } catch (Exception e) {
            writeLog(e);
            retmap.put("status", "-1");
            retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
        }
        return retmap;
    }

    /**
     * 校验数据合法性
     */
    public Map<String, Object> hrmResourceCheck(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            RecordSet rs = new RecordSet();
            String id = Util.null2String(request.getParameter("id"));
            String temName = Util.null2String(request.getParameter("lastname"));
            String temCode = Util.null2String(request.getParameter("workcode"));
            String temTelephone = Util.null2String(request.getParameter("telephone"));
            String temMobile = Util.null2String(request.getParameter("mobile"));

            String checkMessage = "";
            boolean isCheckHas = false;
            boolean isCheckHasName = false;
            boolean isCheck = false;

            String tempSql = "";

            if (temTelephone.equals("sysadmin") || temMobile.equals("sysadmin")) {
                checkMessage = SystemEnv.getHtmlLabelName(22305, user.getLanguage());
                isCheck = true;
            }

            if (!temCode.equals("")) {
                tempSql = "select workcode from HrmResource where workcode='" + temCode + "' ";
                if (temCode.equals("sysadmin")) {
                    checkMessage = SystemEnv.getHtmlLabelName(510739, user.getLanguage());  //判断workcode编号是不是sysadmin
                    isCheck = true;
                }
                if (id.length() > 0) {
                    tempSql += " and id !=" + id;
                }
                rs.executeSql(tempSql);
                if (rs.next()) {
                    String workcode = rs.getString("workcode");
                    if (workcode.equals(temCode)) {
                        checkMessage = SystemEnv.getHtmlLabelName(21447, user.getLanguage());
                        isCheckHas = true;
                        // 删除和编号重复的预留编号
                        String sql = "delete from hrm_coderulereserved where id in(select hcr.id from hrm_coderulereserved hcr left join hrm_coderule hc ON hcr.coderuleid = hc.id where hc.serialtype = ? and reservedcode = ?)";
                        rs.executeUpdate(sql, RuleCodeType.USER.getValue(), temCode);
                    }
                }
            }
            if (!temName.equals("")) {
                tempSql = "select lastname from HrmResource where lastname='" + temName + "' ";
                if (id.length() > 0) {
                    tempSql += " and id !=" + id;
                }
                rs.executeSql(tempSql);
                if (rs.next()) {
                    if (!isCheckHas) {
                        checkMessage = SystemEnv.getHtmlLabelName(21445, user.getLanguage());
                    } else {
                        checkMessage = SystemEnv.getHtmlLabelName(21446, user.getLanguage());
                    }
                    isCheckHasName = true;
                }
            }

            if ((isCheckHas && isCheckHasName) || (isCheckHas && !isCheckHasName) || (isCheck)) {
                retmap.put("status", "-1");
                retmap.put("message", checkMessage);
            } else if (!isCheckHas && isCheckHasName) {
                retmap.put("status", "-1");
                retmap.put("messagetype", "confirm");
                retmap.put("message", checkMessage);
            } else {
                retmap.put("status", "1");
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return retmap;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public String duplicateRemoval(String str, String userid) {
        String[] ss = str.split(",");
        List<String> list = new ArrayList<String>();
        for (String s : ss) {
            list.add(s.trim());
        }
        HashSet hs = new HashSet(list);
        list.clear();
        list.addAll(hs);
        if (list.contains(userid)) list.remove(userid);
        str = list.toString().replace("[", "").replace("]", "").replace(" ", "");
        return str;
    }

    /**
     * 获取允许个人编辑的字段数量
     * @param scopeid
     * @return
     */
    public boolean isHasModify(int scopeid) {
        int fieldNum = 0;
        HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeid);

        StaticObj staticobj = StaticObj.getInstance();
        String multilanguage = (String) staticobj.getObject("multilanguage");
        if (multilanguage == null) multilanguage = "n";
        boolean isMultilanguageOK = false;
        if (multilanguage.equals("y")) isMultilanguageOK = true;
        boolean flagaccount = GCONST.getMOREACCOUNTLANDING();

        while (HrmFieldGroupComInfo.next()) {
            int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
            if (grouptype != scopeid) continue;
            int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
            int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
            hfm.getCustomFields(groupid);
            while (hfm.next()) {
                String fieldName = hfm.getFieldname();
                //没有开启多语言即使开启了系统语言允许个人编辑也不行
                if (fieldName.equals("systemlanguage") && !isMultilanguageOK) continue;
                //没有开启主从账号，账号类型也要过滤
                if (fieldName.equals("accounttype") && !flagaccount) continue;
                if (fieldName.equals("belongto") && flagaccount) continue;
                if (fieldName.equals("loginid") || fieldName.equals("jobactivity") || fieldName.equals("status") || fieldName.equals("departmentvirtualids")) continue;
                if (fieldName.equals("messagerurl") || fieldName.equals("jobGroupId")) continue;
                if (!hfm.isUse() || !hfm.isModify()) {
                    continue;
                }
                fieldNum++;
            }

            //个人信息还需要检查自定义子信息是否有允许个人修改的字段
            if (scopeid == 1) {
                //自定义信息
                RecordSet RecordSet = new RecordSet();
                CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();
                LinkedHashMap<String, String> ht = new LinkedHashMap<String, String>();
                RecordSet.executeSql("select id, formlabel,viewtype from cus_treeform where parentid=" + scopeid + " order by scopeorder");
                while (RecordSet.next()) {
                    if (RecordSet.getInt("viewtype") != 1) continue;
                    int subId = RecordSet.getInt("id");
                    CustomFieldManager cfm2 = new CustomFieldManager("HrmCustomFieldByInfoType", subId);
                    cfm2.getCustomFields();
                    int colcount1 = cfm2.getSize();
                    if (colcount1 != 0) {
                        while (cfm2.next()) {
                            if (!cfm2.isUse() || (!cfm2.isModify())) continue;
                            fieldNum++;
                        }
                    }
                }
            }
        }
        if (fieldNum > 0) return true;
        return false;
    }

    public boolean canEditUserIcon() {
        RecordSet recordSet = new RecordSet();
        int isModify = 0;
        recordSet.executeQuery("select isModify from hrm_formfield where fieldname = ?", "messagerurl");
        if (recordSet.next()) {
            isModify = recordSet.getInt("isModify");
        }
        if (isModify == 1) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Object> refreshPinyin(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            if (user.isAdmin()) {
                upgradeSearchPinyin();
                retmap.put("status", "1");
            } else {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            writeLog(ex);
            retmap.put("status", "-1");
            retmap.put("message", "failed");
        }
        return retmap;
    }

    public void upgradeSearchPinyin() {
        try {
            RecordSet rs = new RecordSet();
            rs.executeQuery("select id, lastname from hrmresource");
            while (rs.next()) {
                RecordSet rs1 = new RecordSet();
                String id = Util.null2String(rs.getString("id"));
                String lastname = Util.null2String(rs.getString("lastname"));
                String quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(lastname);

                try {
                    rs1.executeUpdate("update hrmresource set PINYINLASTNAME = ?, ECOLOGY_PINYIN_SEARCH = ? where id = ?", quickSearchStr, quickSearchStr, id);
                } catch (Exception e) {
                    writeLog(e);
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            writeLog(ex);
            ex.printStackTrace();
        }
    }

    public void upgradeAreaSearchPinyin(){
        try{
            String id = "";
            String name = "";

            RecordSet rs = new RecordSet();
            RecordSet rs1 = new RecordSet();

            rs.executeQuery("select id, countryname as name from hrmcountry");
            while (rs.next()) {
                id = Util.null2String(rs.getString("id"));
                name = Util.null2String(rs.getString("name"));
                String quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(name);

                try {
                    rs1.executeUpdate("update hrmcountry set quicksearch = ? where id = ?", quickSearchStr, id);
                } catch (Exception e) {
                    writeLog(e);
                    e.printStackTrace();
                }
            }

            rs.executeQuery("select id, provincename as name from hrmprovince");
            while (rs.next()) {
                id = Util.null2String(rs.getString("id"));
                name = Util.null2String(rs.getString("name"));
                String quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(name);

                try {
                    rs1.executeUpdate("update hrmprovince set quicksearch = ? where id = ?", quickSearchStr, id);
                } catch (Exception e) {
                    writeLog(e);
                    e.printStackTrace();
                }
            }

            rs.executeQuery("select id, cityname as name from HRMCITY");
            while (rs.next()) {
                id = Util.null2String(rs.getString("id"));
                name = Util.null2String(rs.getString("name"));
                String quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(name);

                try {
                    rs1.executeUpdate("update HRMCITY set quicksearch = ? where id = ?", quickSearchStr, id);
                } catch (Exception e) {
                    writeLog(e);
                    e.printStackTrace();
                }
            }

            rs.executeQuery("select id, cityname as name  from HrmCityTwo");
            while (rs.next()) {
                id = Util.null2String(rs.getString("id"));
                name = Util.null2String(rs.getString("name"));
                String quickSearchStr = new HrmCommonServiceImpl().generateQuickSearchStr(name);

                try {
                    rs1.executeUpdate("update HrmCityTwo set quicksearch = ? where id = ?", quickSearchStr, id);
                } catch (Exception e) {
                    writeLog(e);
                    e.printStackTrace();
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
            writeLog(ex);
        }
    }
}
