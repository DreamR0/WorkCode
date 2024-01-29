package com.api.hrm.cmd.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc20230816.permission.util.CheckSelectUtil;
import com.api.customization.qc20230816.permission.util.CheckSetUtil;
import com.cloudstore.dev.api.bean.SplitPageBean;
import com.cloudstore.dev.api.bean.UserDefCol;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.definedfield.HrmFieldManager;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 获取显示列定制的字段
 * 没有【人力资源维护】权限的人只能看到基本信息
 */
public class GetShowColCmd extends AbstractCommonCommand<Map<String, Object>> {
    //qc20230816 自定义修改权限
    private String isOpen = new BaseBean().getPropValue("qc20230816permission","isopen");

    private List<Map<String,Object>> optionList;

    private List<Map<String,Object>> groupInfoList;

    private HttpServletRequest request;

    public GetShowColCmd(Map<String, Object> params, User user, HttpServletRequest request) {
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
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {

            String dataKey = Util.null2String(params.get("dataKey"));
            if (dataKey.equals("")) {
                resultMap.put("status", "-1");
                resultMap.put("message", "dataKey is null");
            }
            SplitPageBean bean = new SplitPageBean(request, dataKey, "RootMap", "head");
            String pageUid = "";
            if (null != bean.getRootMap()) {
                pageUid = Util.null2String(bean.getRootMap().getString("pageUid"));
            }
            if (StringUtils.isBlank(pageUid)) {
                resultMap.put("status", "-1");
                resultMap.put("message", "dataKey is null");
            }
            // 获得当前用户的定制列
            List<UserDefCol> userDefCols = getUserDefColumns(pageUid, user.getUID());
            if(userDefCols.isEmpty()){
              //如果用户没有设置显示列定制，查看下是否存在同步到所有人的操作,同步到所有人之后的默认userid是0
              userDefCols = getUserDefColumns(pageUid, 0);

            }
            JSONArray unchoosedColumns = bean.getUnchoosedColumns();
            JSONArray choosedColumns = bean.getChoosedColumns();

            // 用户如果有定制列，则按用户的定义处理
            // 但如果用户配置的定制列已不存在，或者有不在配置列中的，则按当前配置的处理
            if (userDefCols != null && !userDefCols.isEmpty()) {
                JSONArray defUnchoose = new JSONArray();//用户自定义的未选列
                JSONArray defChoosed = new JSONArray();//用户自定义的已选列
                // 将默认的待选已选转为Map，增加orders属性
                Map<String, JSONObject> unchooseMap = Maps.newHashMap();
                Map<String, JSONObject> choosedMap = Maps.newHashMap();
                for (int i = 0; i < unchoosedColumns.size(); i++) {
                    JSONObject col = (JSONObject) unchoosedColumns.get(i);
                    col.put("orders", i);
                    unchooseMap.put(col.getString("dataIndex"), col);
                }
                for (int i = 0; i < choosedColumns.size(); i++) {
                    JSONObject col = (JSONObject) choosedColumns.get(i);
                    col.put("orders", i);
                    choosedMap.put(col.getString("dataIndex"), col);
                }
                // 按用户配置的定制列来组装数据
                for (UserDefCol defCol : userDefCols) {
                    String dataIndex = defCol.getDataIndex();
                    JSONObject col = unchooseMap.get(dataIndex);
                    unchooseMap.remove(dataIndex);
                    if (col == null) {
                        col = choosedMap.get(dataIndex);
                        choosedMap.remove(dataIndex);
                    }
                    if (col == null) {
                        continue;
                    }
                    col.put("display", defCol.getDisplay());
                    col.put("orders", defCol.getOrders());
                    if ("0".equals(defCol.getDisplay())) {
                        defChoosed.add(col);
                    } else {
                        defUnchoose.add(col);
                    }
                }
                // 对于不在用户配置内的，按它们的配置位置加入到列表中
                if (!unchooseMap.isEmpty()) {
                    addToListByOrders(defUnchoose, unchooseMap);
                }
                if (!choosedMap.isEmpty()) {
                    addToListByOrders(defChoosed, choosedMap);
                }
                unchoosedColumns = defUnchoose;
                choosedColumns = defChoosed;
            }

            boolean isProtal = Util.null2String(params.get("isProtal")).equals("true");
            if (isProtal) {
                unchoosedColumns.addAll(choosedColumns);
                choosedColumns.clear();
            }

            //显示列定制分组
            groupByColumns(choosedColumns, unchoosedColumns);

            getOptions();

            getGroupInfo();

            resultMap.put("destdatas", choosedColumns);
            resultMap.put("srcdatas", unchoosedColumns);
            resultMap.put("options", this.optionList);
            resultMap.put("groupInfo", this.groupInfoList);
            resultMap.put("currentPage", "1");
            resultMap.put("totalPage", "1");
            resultMap.put("status", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 获取
     */
    private void getOptions(){
        Map<String,Object> selectMap = new HashMap<>();
        List<Map<String,Object>> optionList = new ArrayList<>();

        //是否具有【人员信息导出】权限
        boolean canExportExcel = HrmUserVarify.checkUserRight("HrmResourceInfo:Import", user);
        //是否具有【人力资源维护】权限
        boolean canEditBasicInfo = HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user);
        //是否具有【人力资源卡片系统信息维护】权限
        boolean canEditSystemInfo = HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user);
        //是否具有【薪酬福利维护】权限
        boolean canEditfinanceInfo = HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit", user);

        if(canEditBasicInfo){
            selectMap = new HashMap<>();
            selectMap.put("key","all");
            selectMap.put("selected",true);
            selectMap.put("showname", SystemEnv.getHtmlLabelName(332, user.getLanguage()));
            optionList.add(selectMap);

            selectMap = new HashMap<>();
            selectMap.put("key","basic");
            selectMap.put("selected",false);
            selectMap.put("showname", SystemEnv.getHtmlLabelName(81711, user.getLanguage()));
            optionList.add(selectMap);

            selectMap = new HashMap<>();
            selectMap.put("key","personal");
            selectMap.put("selected",false);
            selectMap.put("showname", SystemEnv.getHtmlLabelName(15687, user.getLanguage()));
            optionList.add(selectMap);

            selectMap = new HashMap<>();
            selectMap.put("key","work");
            selectMap.put("selected",false);
            selectMap.put("showname", SystemEnv.getHtmlLabelName(15688, user.getLanguage()));
            optionList.add(selectMap);

            if(canEditfinanceInfo){
                selectMap = new HashMap<>();
                selectMap.put("key","finance");
                selectMap.put("selected",false);
                selectMap.put("showname", SystemEnv.getHtmlLabelName(15805, user.getLanguage()));
                optionList.add(selectMap);
            }

            if(canEditSystemInfo){
                selectMap = new HashMap<>();
                selectMap.put("key","system");
                selectMap.put("selected",false);
                selectMap.put("showname", SystemEnv.getHtmlLabelName(15804, user.getLanguage()));
                optionList.add(selectMap);
            }
        }

        this.optionList = optionList;
    }

    /**
     * 给字段增加分组属性
     */
    private void groupByColumns(JSONArray choosedColumns, JSONArray unchoosedColumns) {
        CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        //基本信息
        List<String> basicInfoList = new ArrayList<String>();
        HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", -1);
        hfm.getCustomFields();
        while (hfm.next()) {
            String fieldname = hfm.getFieldname();
            if (fieldname.equalsIgnoreCase("loginid") || fieldname.equalsIgnoreCase("systemlanguage")) {
                continue;
            }
            if (fieldname.indexOf("field") > -1) {
                fieldname = "t0_" + fieldname;
            }
            basicInfoList.add(fieldname);
        }
        basicInfoList.add("subcompanyid1");
        //个人信息
        List<String> personalInfoList = new ArrayList<String>();
        hfm = new HrmFieldManager("HrmCustomFieldByInfoType", 1);
        hfm.getCustomFields();
        while (hfm.next()) {
            String fieldname = hfm.getFieldname();
            if (fieldname.indexOf("field") > -1) {
                fieldname = "t1_" + fieldname;
            }
            personalInfoList.add(fieldname);
        }
        //工作信息
        List<String> workInfoList = new ArrayList<String>();
        hfm = new HrmFieldManager("HrmCustomFieldByInfoType", 3);
        hfm.getCustomFields();
        while (hfm.next()) {
            String fieldname = hfm.getFieldname();
            if (fieldname.indexOf("field") > -1) {
                fieldname = "t2_" + fieldname;
            }
            workInfoList.add(fieldname);
        }
        //财务信息
        List<String> financeInfoList = new ArrayList<String>();
        String[] financeInfoArr = new String[]{"accountname", "bankid1", "accountid1", "accumfundaccount"};
        financeInfoList = Arrays.asList(financeInfoArr);
        //系统信息
        List<String> systemInfoList = new ArrayList<String>();
        String[] systemInfoArr = new String[]{"seclevel", "classification", "loginid", "systemlanguage"};
        systemInfoList = Arrays.asList(systemInfoArr);

        for (int i = 0; i < choosedColumns.size(); i++) {
            JSONObject jsonObject = (JSONObject) choosedColumns.get(i);
            String dataIndex = jsonObject.getString("dataIndex");
            if (basicInfoList.contains(dataIndex)) {
                jsonObject.put("group", "basic");
            } else if (personalInfoList.contains(dataIndex)) {
                jsonObject.put("group", "personal");
            } else if (workInfoList.contains(dataIndex)) {
                jsonObject.put("group", "work");
            } else if (financeInfoList.contains(dataIndex)) {
                jsonObject.put("group", "finance");
            } else if (systemInfoList.contains(dataIndex)) {
                jsonObject.put("group", "system");
            } else {
                jsonObject.put("group", "");
            }
            choosedColumns.set(i, jsonObject);
        }

        //qc20230816
        JSONArray unchoosedColumnsNew = new JSONArray();    //用来保存权限过滤后的列表
        for (int i = 0; i < unchoosedColumns.size(); i++) {
            Boolean isHave =false;
            JSONObject jsonObject = (JSONObject) unchoosedColumns.get(i);
            String dataIndex = jsonObject.getString("dataIndex");
            if (basicInfoList.contains(dataIndex)) {
                //qc20230816 这里对基本信息列进行过滤
                String fieldName = dataIndex;
                if (dataIndex.indexOf("t0_field") > -1){
                     fieldName = dataIndex.substring(3,dataIndex.length());
                }

                if (user.getUID()!=1 && "1".equals(isOpen) && !"id".equals(fieldName)&&checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,0)){
                  isHave = true;
                }

                jsonObject.put("group", "basic");
            } else if (personalInfoList.contains(dataIndex)) {
                //qc20230816 这里对个人信息列进行过滤
                String fieldName = dataIndex;
                if (dataIndex.indexOf("t1_field") > -1){
                    fieldName = dataIndex.substring(3,dataIndex.length());
                }
                if (user.getUID()!=1 && "1".equals(isOpen) && !"id".equals(fieldName)&&checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,1)){
                    isHave = true;
                }
                jsonObject.put("group", "personal");
            } else if (workInfoList.contains(dataIndex)) {
                //qc20230816 这里对工作信息列进行过滤
                String fieldName = dataIndex;
                if (dataIndex.indexOf("t2_field") > -1){
                    fieldName = dataIndex.substring(3,dataIndex.length());
                }
                if (user.getUID()!=1 && "1".equals(isOpen) && !"id".equals(fieldName)&&checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,2)){
                    isHave = true;
                }
                jsonObject.put("group", "work");
            } else if (financeInfoList.contains(dataIndex)) {
                jsonObject.put("group", "finance");
            } else if (systemInfoList.contains(dataIndex)) {

                jsonObject.put("group", "system");
            } else {
                //特殊字段“是否在线”
                if ("id".equals(dataIndex)){
                    isHave = true;
                }
                jsonObject.put("group", "");
            }
            if (isHave){
                unchoosedColumnsNew.add(jsonObject);
            }
            //这里单独处理下，安全级别和登录名
            if ("loginid".equals(dataIndex) || "seclevel".equals(dataIndex)){
                new BaseBean().writeLog("==zj==(isSystem)" + checkSetUtil.isSystemRight(user));
                if (checkSetUtil.isSystemRight(user)){
                    unchoosedColumnsNew.add(jsonObject);
                }
            }
                unchoosedColumns.set(i, jsonObject);
        }
        //qc20230816 这里把列进行替换
        if (user.getUID()!=1 && "1".equals(isOpen)){
            unchoosedColumns.clear();
            unchoosedColumns.addAll(unchoosedColumnsNew);
        }
    }

    private List<UserDefCol> getUserDefColumns(String pageUid, int uid) {
        String sql = "select dataIndex, orders, display ,width from cloudstore_defcol where pageUid = ? and userid = ? order by orders";
        RecordSet rs = new RecordSet();
        rs.executeQuery(sql, pageUid, uid);
        List<UserDefCol> userDefCols = Lists.newArrayList();
        while (rs.next()) {
            UserDefCol col = new UserDefCol();
            col.setDataIndex(rs.getString("dataIndex"));
            col.setOrders(rs.getInt("orders"));
            col.setDisplay(rs.getString("display"));
            col.setWidth(rs.getString("width"));
            userDefCols.add(col);
        }
        return userDefCols;
    }

    /**
     * 将map中的value按顺序(orders属性)加入到前面的list中。
     *
     * @param array
     * @param map
     */
    private void addToListByOrders(JSONArray array, Map<String, JSONObject> map) {
        List<OrderCompare> list = Lists.newArrayList();
        for (JSONObject jo : map.values()) {
            list.add(new OrderCompare(jo));
        }
        Collections.sort(list);
        for (OrderCompare o : list) {
            int index = o.getOrders();
            if (index < array.size()) {
                array.add(index, o.jsonObject);
            } else {
                array.add(o.jsonObject);
            }
        }
    }

    private class OrderCompare implements Comparable<OrderCompare> {
        JSONObject jsonObject;

        public OrderCompare(JSONObject jsonObject) {
            this.jsonObject = jsonObject;
        }

        private int getOrders() {
            return jsonObject.getIntValue("orders");
        }

        @Override
        public int compareTo(OrderCompare o) {
            return Integer.valueOf(this.getOrders()).compareTo(Integer.valueOf(o.getOrders()));
        }
    }

    private void getGroupInfo() {
        Map<String, Object> groupItemMap = new HashMap<>();
        List<Map<String, Object>> groupInfoList = new ArrayList<>();

        groupItemMap = new HashMap<>();
        groupItemMap.put("key", "all");
        groupItemMap.put("showname", SystemEnv.getHtmlLabelName(332, user.getLanguage()));
        groupInfoList.add(groupItemMap);

        groupItemMap = new HashMap<>();
        groupItemMap.put("key", "basic");
        groupItemMap.put("showname", SystemEnv.getHtmlLabelName(81711, user.getLanguage()));
        groupInfoList.add(groupItemMap);

        groupItemMap = new HashMap<>();
        groupItemMap.put("key", "personal");
        groupItemMap.put("showname", SystemEnv.getHtmlLabelName(15687, user.getLanguage()));
        groupInfoList.add(groupItemMap);

        groupItemMap = new HashMap<>();
        groupItemMap.put("key", "work");
        groupItemMap.put("showname", SystemEnv.getHtmlLabelName(15688, user.getLanguage()));
        groupInfoList.add(groupItemMap);

        groupItemMap = new HashMap<>();
        groupItemMap.put("key", "finance");
        groupItemMap.put("showname", SystemEnv.getHtmlLabelName(15805, user.getLanguage()));
        groupInfoList.add(groupItemMap);

        groupItemMap = new HashMap<>();
        groupItemMap.put("key", "system");
        groupItemMap.put("showname", SystemEnv.getHtmlLabelName(15804, user.getLanguage()));
        groupInfoList.add(groupItemMap);

        this.groupInfoList = groupInfoList;
    }
}
