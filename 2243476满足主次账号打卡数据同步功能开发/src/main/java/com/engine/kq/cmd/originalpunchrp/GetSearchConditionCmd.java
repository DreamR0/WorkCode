package com.engine.kq.cmd.originalpunchrp;

import com.api.browser.bean.BrowserBean;
import com.api.browser.bean.BrowserTabBean;
import com.api.browser.bean.SearchConditionItem;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.bean.SelectOption;
import com.api.hrm.bean.WeaRadioGroup;
import com.api.hrm.util.HrmAdvancedSearchUtil;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.google.common.collect.Lists;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原始打卡记录报表--获取查询条件
 */
public class GetSearchConditionCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetSearchConditionCmd(Map<String, Object> params, User user) {
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

        HrmAdvancedSearchUtil hrmAdvancedSearchUtil = new HrmAdvancedSearchUtil();
        /*展示列*/
        Map<String, Object> optionMap = new HashMap<String, Object>();
        List<Map<String, Object>> optionList = new ArrayList<Map<String, Object>>();

        optionMap = new HashMap<String, Object>();
        optionMap.put("key", "subcom");
        optionMap.put("showname", SystemEnv.getHtmlLabelName(33553, user.getLanguage()));
        optionList.add(optionMap);
        optionMap = new HashMap<String, Object>();
        optionMap.put("key", "dept");
        optionMap.put("showname", SystemEnv.getHtmlLabelName(27511, user.getLanguage()));
        optionList.add(optionMap);
        optionMap = new HashMap<String, Object>();
        optionMap.put("key", "jobtitle");
        optionMap.put("showname", SystemEnv.getHtmlLabelName(6086, user.getLanguage()));
        optionList.add(optionMap);
        optionMap = new HashMap<String, Object>();
        optionMap.put("key", "workcode");
        optionMap.put("showname", SystemEnv.getHtmlLabelName(714, user.getLanguage()));
        optionList.add(optionMap);

        String valueStr = "subcom,dept,jobtitle,workcode";
        resultMap.put("option", optionList);
        resultMap.put("value", valueStr);

        List<Map<String, Object>> groupList = new ArrayList<Map<String, Object>>();
        Map<String, Object> groupItem = new HashMap<String, Object>();
        List<Object> itemList = new ArrayList<Object>();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        SearchConditionItem searchConditionItem = null;
        HrmFieldBean hrmFieldBean = null;
        /*时间范围*/
        WeaRadioGroup weaRadioGroup = new WeaRadioGroup();//WeaRadioGroup组件
        List<Object> options = new ArrayList<Object>();//WeaRadioGroup组件内的options参数
        List<String> domkey = new ArrayList<String>();//WeaRadioGroup组件内的domkey参数
        Map<String, Object> selectLinkageDatas = new HashMap<String, Object>();//WeaRadioGroup组件内的selectLinkageDatas参数
        Map<String, Object> linkMap = new HashMap<String, Object>();//selectLinkageDatas参数内部Map数据

        weaRadioGroup.setLabel(SystemEnv.getHtmlLabelName(19482, user.getLanguage()));

        options.add(new SelectOption("1", SystemEnv.getHtmlLabelName(15537, user.getLanguage())));//今天
        options.add(new SelectOption("2", SystemEnv.getHtmlLabelName(15539, user.getLanguage())));//本周
        options.add(new SelectOption("3", SystemEnv.getHtmlLabelName(15541, user.getLanguage()), true));//本月
        options.add(new SelectOption("7", SystemEnv.getHtmlLabelName(27347, user.getLanguage())));//上个月
        options.add(new SelectOption("4", SystemEnv.getHtmlLabelName(21904, user.getLanguage())));//本季
        options.add(new SelectOption("5", SystemEnv.getHtmlLabelName(15384, user.getLanguage())));//本年
        options.add(new SelectOption("8", SystemEnv.getHtmlLabelName(81716, user.getLanguage())));//上一年
        options.add(new SelectOption("6", SystemEnv.getHtmlLabelName(32530, user.getLanguage())));//指定日期范围
        weaRadioGroup.setOptions(options);

        domkey.add("dateScope");
        weaRadioGroup.setDomkey(domkey);

        linkMap.put("conditionType", "RANGEPICKER");
        domkey = new ArrayList<String>();
        domkey.add("startDate");
        domkey.add("endDate");
        linkMap.put("domkey", domkey);
        selectLinkageDatas.put("6", linkMap);
        weaRadioGroup.setSelectLinkageDatas(selectLinkageDatas);

        weaRadioGroup.setLabelcol(6);
        weaRadioGroup.setFieldcol(18);
        itemList.add(weaRadioGroup);

        /**
         * 数据范围
         * 总部、分部、部门、人员、我的下属(管理员没有我的下属，没有下属的人员也不显示我的下属)
         * 如果人员是否在考勤报表权限共享设置里，则表示此人拥有权限，否则，没有权限
         */
        boolean hasRight = false;
        String sql = "select * from kq_ReportShare where resourceId=" + user.getUID() + " and reportName=3";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        hasRight = recordSet.getCounts() > 0;

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
            options.add(new SelectOption("0", SystemEnv.getHtmlLabelName(140, user.getLanguage()), hasRight && user.isAdmin()));//总部
            options.add(new SelectOption("1", SystemEnv.getHtmlLabelName(33553, user.getLanguage())));//分部
            options.add(new SelectOption("2", SystemEnv.getHtmlLabelName(27511, user.getLanguage())));//部门

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
                    browserTabBean.setShowOrder(0);
                } else {
                    browserTabBean.setShowOrder(1);
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
                    browserTabBean.setShowOrder(0);
                } else {
                    browserTabBean.setShowOrder(1);
                }
            }
            selectLinkageDatas.put("2", searchConditionItem);
        }

        options.add(new SelectOption("3", SystemEnv.getHtmlLabelName(30042, user.getLanguage()), !hasRight || !user.isAdmin()));//人员
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
        //==zj 搜索框显示id
        KqUtil kqUtil = new KqUtil();
        BrowserBean browserConditionParam = searchConditionItem.getBrowserConditionParam();
        List<Map<String, Object>> replaceDatas = browserConditionParam.getReplaceDatas();
        browserConditionParam.setReplaceDatas( kqUtil.getChildKq(user.getUID(),replaceDatas));

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

        WeaRadioGroup wrg1 = new WeaRadioGroup();

        ChgPasswdReminder reminder = new ChgPasswdReminder();
        RemindSettings settings = reminder.getRemindSettings();
        String checkUnJob = Util.null2String(settings.getCheckUnJob(), "0");
        List<String> statusList = Lists.newArrayList();
        if ("1".equals(checkUnJob)) {//启用后，只有有“离职人员查看”权限的用户才能检索非在职人员
          if (HrmUserVarify.checkUserRight("hrm:departureView", user)) {
            statusList.add("9,332,false");
          }
        } else {
          statusList.add("9,332,false");
        }
        statusList.add("0,15710,false");
        statusList.add("1,15711,false");
        statusList.add("2,480,false");
        statusList.add("3,15844,false");
        if ("1".equals(checkUnJob)) {//启用后，只有有“离职人员查看”权限的用户才能检索非在职人员
          if (HrmUserVarify.checkUserRight("hrm:departureView", user)) {
            statusList.add("4,6094,false");
            statusList.add("5,6091,false");
            statusList.add("6,6092,false");
            statusList.add("7,2245,false");
          }
        } else {
          statusList.add("4,6094,false");
          statusList.add("5,6091,false");
          statusList.add("6,6092,false");
          statusList.add("7,2245,false");
        }
        statusList.add("8,1831,true");
        String[] strOptions = new String[statusList.size()];
        for(int i = 0 ; i < statusList.size() ; i++){
          String statusStr = statusList.get(i);
          strOptions[i] = statusStr;
        }

        wrg1 = hrmAdvancedSearchUtil.getAdvanceCondition("status","602",strOptions,null,user);
        wrg1.setLabelcol(3);
        wrg1.setFieldcol(20);
        itemList.add(wrg1);

        groupItem.put("items", itemList);
        groupList.add(groupItem);
        resultMap.put("condition", groupList);
        return resultMap;
    }
}
