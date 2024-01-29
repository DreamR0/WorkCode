package com.engine.kq.cmd.reportshare;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.ConditionType;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤报表共享设置--新建的表单
 */
public class GetReportShareFormCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetReportShareFormCmd(Map<String, Object> params, User user) {
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

        String id = Util.null2String(params.get("id"));

        List<Map<String, Object>> groupList = new ArrayList<Map<String, Object>>();
        Map<String, Object> groupItem = new HashMap<String, Object>();
        List<Object> itemList = new ArrayList<Object>();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        SearchConditionItem searchConditionItem = null;
        HrmFieldBean hrmFieldBean = null;

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("reportName");//报表名称
        hrmFieldBean.setFieldlabel("15517");
        hrmFieldBean.setFieldhtmltype("5");
        hrmFieldBean.setType("1");
        hrmFieldBean.setIsFormField(true);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        List<SearchConditionOption> optionsList = new ArrayList<SearchConditionOption>();
        optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(332, user.getLanguage()), true));
        optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(390351, user.getLanguage())));
        optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(390352, user.getLanguage())));
        optionsList.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(390248, user.getLanguage())));
        optionsList.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(389441, user.getLanguage())));
        searchConditionItem.setOptions(optionsList);
        itemList.add(searchConditionItem);

        ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
        boolean isUseManageDetach = manageDetachComInfo.isUseManageDetach();//是否开启了管理分权

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("resourceType");//对象类型：1-人力资源、7-分权管理员、8-系统管理员、1024-安全级别
        hrmFieldBean.setFieldlabel("21956");
        hrmFieldBean.setFieldhtmltype("5");
        hrmFieldBean.setType("1");
        hrmFieldBean.setIsFormField(true);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        optionsList = new ArrayList<SearchConditionOption>();
        optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(179, user.getLanguage()), true));
        if (isUseManageDetach) {
            optionsList.add(new SearchConditionOption("7", SystemEnv.getHtmlLabelName(33495, user.getLanguage())));
        }
        if (user.getUID() == 1) {
            optionsList.add(new SearchConditionOption("8", SystemEnv.getHtmlLabelName(16139, user.getLanguage())));
        }
        //对象类型 1024 安全级别
        optionsList.add(new SearchConditionOption("1024", SystemEnv.getHtmlLabelName(523727, user.getLanguage())));

        searchConditionItem.setOptions(optionsList);
        itemList.add(searchConditionItem);

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("resourceIds");//对象--人力资源
        hrmFieldBean.setFieldlabel("106");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("17");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("resourceManagerIds");//对象--分权管理员
        hrmFieldBean.setFieldlabel("106");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("adminAccount");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        searchConditionItem.getBrowserConditionParam().setIsSingle(false);
        itemList.add(searchConditionItem);


        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("userLevel");
        hrmFieldBean.setFieldlabel("683");
        hrmFieldBean.setFieldhtmltype("1");
        hrmFieldBean.setType("scope");
        if(hrmFieldBean.getType().equals("scope")){
            hrmFieldBean.setIsScope(true);
        }
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);

        itemList.add(searchConditionItem);


        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("shareLevel");//共享级别
        hrmFieldBean.setFieldlabel("3005");
        hrmFieldBean.setFieldhtmltype("5");
        hrmFieldBean.setType("1");
        hrmFieldBean.setIsFormField(true);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        optionsList = new ArrayList<SearchConditionOption>();
        optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(33553, user.getLanguage()), true));//分部
        optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(27511, user.getLanguage())));//部门
        optionsList.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(179, user.getLanguage())));//人员
        optionsList.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(6086, user.getLanguage())));//岗位
        if(!new ManageDetachComInfo().appDetachDisableAll(user)) {
            optionsList.add(new SearchConditionOption("4", SystemEnv.getHtmlLabelName(1340, user.getLanguage())));//所有人
        }
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        //==z 这里增加一个对应安全级别的选项框
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("shareLevelCustom");//安全级别
        hrmFieldBean.setFieldlabel("3005");
        hrmFieldBean.setFieldhtmltype("5");
        hrmFieldBean.setType("1");
        hrmFieldBean.setIsFormField(true);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        optionsList = new ArrayList<SearchConditionOption>();
        optionsList.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(33553, user.getLanguage()), true));//分部
        optionsList.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(27511, user.getLanguage())));//部门
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        //==z 增加生效范围（分部）
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("subcomIdCustom");//分部
        hrmFieldBean.setFieldlabel("-20240101");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("194");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(2);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        //==z 增加生效范围（部门）
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("deptIdCustom");//部门
        hrmFieldBean.setFieldlabel("-20240102");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("57");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(2);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);


        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("subcomId");//分部
        hrmFieldBean.setFieldlabel("33553");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("194");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("deptId");//部门
        hrmFieldBean.setFieldlabel("27511");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("57");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("jobtitleId");//岗位
        hrmFieldBean.setFieldlabel("6086");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("278");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("userId");//人力资源
        hrmFieldBean.setFieldlabel("179");
        hrmFieldBean.setFieldhtmltype("3");
        hrmFieldBean.setType("17");
        hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        searchConditionItem.setOptions(optionsList);
        searchConditionItem.setRules("required|string");
        itemList.add(searchConditionItem);

        groupItem.put("items", itemList);
        groupList.add(groupItem);
        resultMap.put("condition", groupList);
        return resultMap;
    }
}
