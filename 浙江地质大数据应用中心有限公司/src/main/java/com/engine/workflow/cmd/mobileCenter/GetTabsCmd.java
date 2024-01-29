package com.engine.workflow.cmd.mobileCenter;

import com.alibaba.fastjson.JSON;
import com.engine.core.interceptor.CommandContext;
import com.engine.workflow.biz.mobileCenter.MobileDimensionsBiz;
import com.engine.workflow.biz.mobileCenter.WorkflowCenterTabBiz;
import com.engine.workflow.cmd.mobileSetting.support.WfCenterOperateTypeEnum;
import com.engine.workflow.cmd.mobileSetting.support.WfCenterTabTypeEnum;
import com.engine.workflow.entity.mobileCenter.Dimensions;
import com.engine.workflow.entity.mobileCenter.Tabs;
import com.engine.workflow.util.CommandUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.ofs.util.OfsWorkflowShareUtils;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.workflow.WorkflowConfigComInfo;

import java.util.*;

/**
 * User: zzw
 * Date: 2018-12-26-11:16
 * Description: 应用门户使用
 */
public class GetTabsCmd extends CommandUtil<Map<String,Object>> {
    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        int menuid = Util.getIntValue((String) params.get("menuid"),-1);
        String typeId = new BaseBean().getPropValue("qc0106","typeId");
        //根据menuid获取tabs实体
        MobileDimensionsBiz mdb = new MobileDimensionsBiz();
        Dimensions di = mdb.getDimension(menuid);
        new BaseBean().writeLog("==zj==(Dimensions)" + JSON.toJSONString(di));
        //int id=-1;
        //all doing done mine
        List<Tabs> tabs=new ArrayList<>();
        Map<String,Object> map=new HashMap<>();
        RecordSet rs = new RecordSet();
        Tabs tab = null;
        boolean showTab = false;//应用是否显示tab
        String dataurl = "";
        String treeurl = "";
        String conditionUrl = "";
        String buttonMainifestly = "1";     // 是否操作菜单明显化
        WorkflowConfigComInfo workflowConfigComInfo = new WorkflowConfigComInfo();
        buttonMainifestly = Util.null2String(workflowConfigComInfo.getValue("mobilecenter_button_manifestly"));
        if(!"1".equals(buttonMainifestly)){
            buttonMainifestly = "0";
        }
        map.put("mobilecenter_button_manifestly",buttonMainifestly);
        String tabsSql = "select typeid,mobileappkeyid,typename,path,treeurl,dataurl,typetitle,conditionurl from workflow_dimension where isshow=1 and scope=? ";
        String typeids = mdb.getTypeids(menuid);
        if(!"".equals(typeids)){
            tabsSql += " and typeid in("+typeids+")";
        }else{
            tabsSql += " and isdeftab=1 ";
        }
        tabsSql += " order by dsporder ";
        if("1".equals(di.getApptype())){//待办的
            String defDoingConditionUrl = "/api/workflow/reqlist/doingBaseInfo";                 // 待办默认conditionUrl
            String defDoingDataUrl = "/api/workflow/mobile/getListResult";                      // 待办默认dataUrl
            String defDoingTreeUrl = "/api/workflow/reqlist/doingBaseInfo";                      // 待办默认treeUrl
            int resourceid = Util.getIntValue(Util.null2String(params.get("resourceid")),-1);             // 增加resourceid参数 从人员卡片查看流程
            boolean isNeedHide = resourceid != -1 && resourceid != user.getUID();
            rs.executeQuery(tabsSql,"emDoingApp");
            showTab = rs.getCounts() > 1;
            // 增加 操作菜单按钮数据
            Map<String,Object> doReadIt = new HashMap<String, Object>();
            doReadIt.put("key", WfCenterOperateTypeEnum.allread.getKey());
            doReadIt.put("buttonName", SystemEnv.getHtmlLabelName(503621,user.getLanguage()));
            Map<String,Object> batchSubmit = new HashMap<String, Object>();
            batchSubmit.put("key", WfCenterOperateTypeEnum.batchsubmit.getKey());
            batchSubmit.put("buttonName",SystemEnv.getHtmlLabelName(17598,user.getLanguage()));
            Map<String,Object>  batchAttion = new HashMap<String, Object>();
            batchAttion.put("key", WfCenterOperateTypeEnum.batchatten.getKey());
            batchAttion.put("buttonName", SystemEnv.getHtmlLabelName(503955,user.getLanguage()));
            Map<String,Object>  batchForward = new HashMap<String, Object>();
            batchForward.put("key", WfCenterOperateTypeEnum.batchforward.getKey());
            batchForward.put("buttonName", SystemEnv.getHtmlLabelName(514521,user.getLanguage()));
            while(rs.next()){
                List<Map<String, Object>> buttons = new ArrayList<Map<String,Object>>();
                if(!isNeedHide){
                    Map<String,Object> doReatItTemp = new HashMap<String, Object>(doReadIt);
                    Map<String,Object> batchSubmitTemp = new HashMap<String,Object>(batchSubmit);
                    Map<String,Object> batchAttionTemp = new HashMap<String,Object>(batchAttion);
                    Map<String,Object> batchForwardTemp = new HashMap<String,Object>(batchForward);
                    buttons.add(batchSubmitTemp);
                    buttons.add(batchAttionTemp);
                    buttons.add(doReatItTemp);
                    buttons.add(batchForwardTemp);
                }
                conditionUrl = Util.null2String(rs.getString("conditionurl")).trim();
                dataurl = Util.null2String(rs.getString("dataurl")).trim();
                treeurl = Util.null2String(rs.getString("treeurl")).trim();
                if("".equals(conditionUrl)){
                    conditionUrl = defDoingConditionUrl;
                }
                if("".equals(dataurl)){
                    dataurl = defDoingDataUrl;
                }
                if("".equals(treeurl)){
                    treeurl = defDoingTreeUrl;
                }
                tab = new Tabs(Util.getIntValue(rs.getString("mobileappkeyid"),1),rs.getString("typename"),rs.getString("path"),
                        treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition="+rs.getString("typeid")+"&resourceid="+(isNeedHide ? resourceid : ""),
                        dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition="+rs.getString("typeid")+"&resourceid="+(isNeedHide ? resourceid : ""),conditionUrl,
                        SystemEnv.getHtmlLabelName(Util.getIntValue(rs.getString("typetitle")),user.getLanguage()),buttons);
                tabs.add(tab);
            }
            //map.put("centerButtons", buttons);
            map.put("tabs",tabs);
            map.put("scope","doing");
            map.put("showTab",showTab);
            map.put("canSubmit",true);
            map.put("canRead",true);
            map.put("titleUrl","/api/workflow/reqlist/doingCountInfo");
        }else if("2".equals(di.getApptype())){//已办的
            String defDoneConditionUrl = "/api/workflow/reqlist/doneBaseInfo";                  // 已办默认conditionUrl
            String defDoneDataUrl = "/api/workflow/mobile/getListResult";                       // 已办默认dataUrl
            String defDoneTreeUrl = "/api/workflow/reqlist/doneBaseInfo";                       // 已办默认treeUrl
            rs.executeQuery(tabsSql,"emDoneApp");
            showTab = rs.getCounts() > 1;
            // 增加 操作菜单按钮数据
            Map<String,Object>  batchAttion = new HashMap<String, Object>();
            batchAttion.put("key", WfCenterOperateTypeEnum.batchatten.getKey());
            batchAttion.put("buttonName", SystemEnv.getHtmlLabelName(503955,user.getLanguage()));
            Map<String,Object>  batchForward = new HashMap<String, Object>();
            batchForward.put("key", WfCenterOperateTypeEnum.batchforward.getKey());
            batchForward.put("buttonName", SystemEnv.getHtmlLabelName(514521,user.getLanguage()));
            while(rs.next()){
                Map<String,Object> batchAttionTemp = new HashMap<String,Object>(batchAttion);
                Map<String,Object> batchForwardTemp = new HashMap<String,Object>(batchForward);
                List<Map<String, Object>> buttons = new ArrayList<Map<String,Object>>();
                buttons.add(batchAttionTemp);
                buttons.add(batchForwardTemp);
                dataurl = Util.null2String(rs.getString("dataurl")).trim();
                treeurl = Util.null2String(rs.getString("treeurl")).trim();
                conditionUrl = Util.null2String(rs.getString("conditionurl")).trim();
                if("".equals(conditionUrl)){
                    conditionUrl = defDoneConditionUrl;
                }
                if("".equals(dataurl)){
                    dataurl = defDoneDataUrl;
                }
                if("".equals(treeurl)){
                    treeurl = defDoneTreeUrl;
                }
                tab = new Tabs(Util.getIntValue(rs.getString("mobileappkeyid"),1),rs.getString("typename"),rs.getString("path"),
                        treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition="+rs.getString("typeid"),
                        dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=done&viewcondition="+rs.getString("typeid"),conditionUrl,
                        SystemEnv.getHtmlLabelName(Util.getIntValue(rs.getString("typetitle")),user.getLanguage()),buttons);
                tabs.add(tab);
            }
            map.put("tabs",tabs);
            //map.put("centerButtons",buttons);
            map.put("scope","done");
            map.put("showTab",showTab);
            map.put("canSubmit",false);
            map.put("canRead",false);
            map.put("titleUrl","/api/workflow/reqlist/doneCountInfo");
        }else if("3".equals(di.getApptype())){//我的请求
            String defMineConditionUrl = "/api/workflow/reqlist/mineBaseInfo";                  // 我的请求默认conditionUrl
            String defMineDataUrl = "/api/workflow/mobile/getListResult";                       // 我的请求默认 dataUrl
            String defMineTreeUrl = "/api/workflow/reqlist/mineBaseInfo";                        // 我的请求默认 treeUrl
            rs.executeQuery(tabsSql,"emMineApp");
            showTab = rs.getCounts() > 1;
            // 增加 操作菜单按钮数据
            Map<String,Object>  batchAttion = new HashMap<String, Object>();
            batchAttion.put("key", WfCenterOperateTypeEnum.batchatten.getKey());
            batchAttion.put("buttonName", SystemEnv.getHtmlLabelName(503955,user.getLanguage()));
            Map<String,Object>  batchForward = new HashMap<String, Object>();
            batchForward.put("key", WfCenterOperateTypeEnum.batchforward.getKey());
            batchForward.put("buttonName", SystemEnv.getHtmlLabelName(514521,user.getLanguage()));
            while(rs.next()){
                Map<String,Object> batchAttionTemp = new HashMap<String,Object>(batchAttion);
                Map<String,Object> batchForwardTemp = new HashMap<String,Object>(batchForward);
                List<Map<String, Object>> buttons = new ArrayList<Map<String,Object>>();
                buttons.add(batchAttionTemp);
                buttons.add(batchForwardTemp);
                dataurl = Util.null2String(rs.getString("dataurl")).trim();
                treeurl = Util.null2String(rs.getString("treeurl")).trim();
                conditionUrl = Util.null2String(rs.getString("conditionurl")).trim();
                if("".equals(conditionUrl)){
                    conditionUrl = defMineConditionUrl;
                }
                if("".equals(dataurl)){
                    dataurl = defMineDataUrl;
                }
                if("".equals(treeurl)){
                    treeurl = defMineTreeUrl;
                }
                tab = new Tabs(Util.getIntValue(rs.getString("mobileappkeyid"),1),rs.getString("typename"),rs.getString("path"),
                        treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition="+rs.getString("typeid"),
                        dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=mine&viewcondition="+rs.getString("typeid"),conditionUrl,
                        SystemEnv.getHtmlLabelName(Util.getIntValue(rs.getString("typetitle")),user.getLanguage()),buttons);
                tabs.add(tab);
            }
            map.put("tabs",tabs);
            //map.put("centerButtons",buttons);
            map.put("scope","mine");
            map.put("showTab",showTab);
            map.put("canSubmit",false);
            map.put("canRead",false);
            map.put("titleUrl","");
        }else if("4".equals(di.getApptype())){//办结
            String defFinConditionUrl = "/api/workflow/reqlist/doneBaseInfo";                    // 办结默认 conditionUrl
            String defFinDataUrl = "/api/workflow/mobile/getListResult";                         // 办结默认 dataUrl
            String defFinTreeUrl = "/api/workflow/reqlist/doneBaseInfo";                         // 办结默认 treeUrl
            rs.executeQuery(tabsSql,"emFinApp");
            showTab = rs.getCounts() > 1;
            Map<String,Object>  batchForward = new HashMap<String, Object>();
            batchForward.put("key", WfCenterOperateTypeEnum.batchforward.getKey());
            batchForward.put("buttonName", SystemEnv.getHtmlLabelName(514521,user.getLanguage()));
            while(rs.next()){
                Map<String,Object> batchForwardTemp = new HashMap<String,Object>(batchForward);
                List<Map<String, Object>> buttons = new ArrayList<Map<String,Object>>();
                buttons.add(batchForwardTemp);
                dataurl = Util.null2String(rs.getString("dataurl")).trim();
                treeurl = Util.null2String(rs.getString("treeurl")).trim();
                conditionUrl = Util.null2String(rs.getString("conditionurl")).trim();
                if("".equals(conditionUrl)){
                    conditionUrl = defFinConditionUrl;
                }
                if("".equals(dataurl)){
                    dataurl = defFinDataUrl;
                }
                if("".equals(treeurl)){
                    treeurl = defFinTreeUrl;
                }
                tab = new Tabs(Util.getIntValue(rs.getString("mobileappkeyid"),1),rs.getString("typename"),rs.getString("path"),
                        treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition="+rs.getString("typeid"),
                        dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=done&viewcondition="+rs.getString("typeid"),conditionUrl,
                        SystemEnv.getHtmlLabelName(Util.getIntValue(rs.getString("typetitle")),user.getLanguage()),buttons);
                tabs.add(tab);
            }

            map.put("tabs",tabs);
            map.put("scope","doneFinish");
            map.put("showTab",showTab);
            map.put("canSubmit",false);
            map.put("canRead",false);
            map.put("titleUrl","");
        }else if("5".equals(di.getApptype())){//抄送
            String defCopyConditionUrl = "/api/workflow/reqlist/doingBaseInfo";                   // 抄送默认 conditionUrl
            String defCopyDataUrl = "/api/workflow/mobile/getListResult";                        // 抄送默认 dataUrl
            String defCopyTreeUrl = "/api/workflow/reqlist/doingBaseInfo";                        // 抄送默认 treeUrl
            rs.executeQuery(tabsSql,"emCopyApp");
            showTab = rs.getCounts() > 1;
            // 增加操作菜单

            Map<String,Object> doReadIt = new HashMap<String, Object>();
            doReadIt.put("key", WfCenterOperateTypeEnum.allread.getKey());
            doReadIt.put("buttonName", SystemEnv.getHtmlLabelName(503621,user.getLanguage()));
            Map<String,Object> batchSubmit = new HashMap<String,Object>();
            batchSubmit.put("key", WfCenterOperateTypeEnum.batchsubmit.getKey());
            batchSubmit.put("buttonName",SystemEnv.getHtmlLabelName(17598, user.getLanguage()));
            Map<String,Object>  batchForward = new HashMap<String, Object>();
            batchForward.put("key", WfCenterOperateTypeEnum.batchforward.getKey());
            batchForward.put("buttonName", SystemEnv.getHtmlLabelName(514521,user.getLanguage()));

            while(rs.next()){
                Map<String,Object> doReadItTemp = new HashMap<String,Object>(doReadIt);
                Map<String,Object> batchSubmitTemp = new HashMap<String,Object>(batchSubmit);
                Map<String,Object> batchForwardTemp = new HashMap<String,Object>(batchForward);
                List<Map<String, Object>> buttons = new ArrayList<Map<String,Object>>();
                buttons.add(batchSubmitTemp);
                buttons.add(doReadItTemp);
                buttons.add(batchForwardTemp);
                dataurl = Util.null2String(rs.getString("dataurl")).trim();
                treeurl = Util.null2String(rs.getString("treeurl")).trim();
                conditionUrl = Util.null2String(rs.getString("conditionurl")).trim();
                if("".equals(conditionUrl)){
                    conditionUrl = defCopyConditionUrl;
                }
                if("".equals(dataurl)){
                    dataurl = defCopyDataUrl;
                }
                if("".equals(treeurl)){
                    treeurl = defCopyTreeUrl;
                }
                tab = new Tabs(Util.getIntValue(rs.getString("mobileappkeyid"),1),rs.getString("typename"),rs.getString("path"),
                        treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition="+rs.getString("typeid"),
                        dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition="+rs.getString("typeid"),conditionUrl,
                        SystemEnv.getHtmlLabelName(Util.getIntValue(rs.getString("typetitle")),user.getLanguage()),buttons);
                tabs.add(tab);
            }
            map.put("tabs",tabs);
            // map.put("centerButtons",buttons);
            map.put("scope","flowCS");
            map.put("showTab",showTab);
            map.put("canSubmit",false);
            map.put("canRead",true);
            map.put("titleUrl","");
        }else if("10".equals(di.getApptype()) || menuid == -1 || menuid == 10){//流程中心
            boolean isdeleted = false;  // 系统中无此应用
            if(null == di.getApptype() || "".equals(di.getApptype())){
                isdeleted = true;
            }
            if(menuid == -1){
                menuid = 10;        // 若没传menuid，默认的menuid
            }
            // 获取tab 信息
            List<Tabs> tabsList = this.getWfCenterTabs(menuid, isdeleted);
            //List<Map<String,Object>> buttons = this.getWfCenterButtons(menuid);
            showTab = tabsList.size() > 1;
            map.put("showTab", showTab);
            // map.put("centerButtons", buttons);
            map.put("tabs", tabsList);
            map.put("scope","wfCenter");
        }else if (typeId.equals(di.getApptype())){
            String defDoneConditionUrl = "/api/workflow/reqlist/doShowBaseInfo";
            String defDoneDataUrl = "/api/workflow/mobile/getListResult";
            String defDoneTreeUrl = "/api/workflow/reqlist/doShowdBaseInfo";


            if("".equals(conditionUrl)){
                conditionUrl = defDoneConditionUrl;
            }
            if("".equals(dataurl)){
                dataurl = defDoneDataUrl;
            }
            if("".equals(treeurl)){
                treeurl = defDoneTreeUrl;
            }
            tab = new Tabs(1,"flowAll","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=82",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=82",conditionUrl,
                    "全部");
            tabs.add(tab);
            tab = new Tabs(2,"flowDoing","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=83",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=83",conditionUrl,
                    "待处理");
            tabs.add(tab);
            tab = new Tabs(3,"flowUnFinish","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=84",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=84",conditionUrl,
                    "未归档");
            tabs.add(tab);
            tab = new Tabs(4,"flowFinish","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=85",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=85",conditionUrl,
                    "已归档");
            tabs.add(tab);
            tab = new Tabs(5,"flowNew","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=86",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=86",conditionUrl,
                    "未读");
            tabs.add(tab);
            tab = new Tabs(6,"flowReject","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=87",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=87",conditionUrl,
                    "退回");
            tabs.add(tab);
            tab = new Tabs(7,"flowAttention","/center/doing",
                    treeurl+(treeurl.contains("?") ? "" : "?") + "ismobile=1&menuid="+menuid+"&viewcondition=88",
                    dataurl+(dataurl.contains("?") ? "" : "?") + "ismobile=1&mobileDimensionScope=doing&viewcondition=88",conditionUrl,
                    "我的关注");
            tabs.add(tab);

            map.put("tabs",tabs);
            map.put("scope","doShow");
            map.put("showTab",true);
        }

        return map;
    }


    public GetTabsCmd() {
    }

    public GetTabsCmd(Map<String, Object> params, User user) {
        this.params = params;
        this.user = user;
        //super(params, user);
    }

    /**
     * 根据 menuid 获取 流程中心 元素 的tab页信息
     * @param menuid
     * @return isdeleted 应用是否已被删除
     */
    private List<Tabs> getWfCenterTabs(int menuid, boolean isdeleted) {
        String defCenterConditionUrl = "/api/workflow/customQuery/getFixedCondition";                 // 流程中心默认conditionUrl
        List<Tabs> tabsList = new ArrayList<Tabs>();
        String userlanguageStr = String.valueOf(user.getLanguage());
        RecordSet rs = new RecordSet();
        List<Map<String, Object>> wfCenterButtons = this.getWfCenterButtons(menuid,isdeleted);        // 获取所有可展示的操作菜单
        if (!isdeleted && WorkflowCenterTabBiz.isInitTab(menuid)) {
            rs.executeQuery("select wmt.id, wmt.tabtitle,wmt.viewtype,wmt.sourcetype,wd.conditionurl " +
                    "from workflow_mobilecenter_tabinfo wmt join workflow_dimension wd on wmt.viewtype = wd.keyid where menuid = ? order by orderno", menuid);
            while (rs.next()) {
                Tabs tabstemp = null;
                // 获取基本信息
                String tabId = rs.getString("id");
                String viewType = rs.getString("viewtype");
                String tabTitle = rs.getString("tabtitle");
                String conditionurl = Util.null2String(rs.getString("conditionurl")).trim();
                if("".equals(conditionurl)){
                    conditionurl = defCenterConditionUrl;
                }
                // 5 中默认类型走 原本的 treeUrl 和 dataUrl
                if ("1".equals(viewType)) {   // 待办事宜
                    List<Map<String, Object>> centerButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.todowf.getTypename(), WfCenterTabTypeEnum.todowf.getPath(),
                            WfCenterTabTypeEnum.todowf.getTreeUrl() + "?ismobile=1&menuid=" + menuid +"&mobileTabId="+tabId+"&belongPathTree=true&viewScope=doing",
                            WfCenterTabTypeEnum.todowf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=doing", Util.formatMultiLang(tabTitle, userlanguageStr), viewType,
                            tabId,conditionurl,this.getButtons(centerButtons, "1"));
                } else if ("10".equals(viewType)) {    // 全部事宜
                    List<Map<String, Object>> centerButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.allwf.getTypename(), WfCenterTabTypeEnum.allwf.getPath(),
                            WfCenterTabTypeEnum.allwf.getTreeUrl() + "?ismobile=1&menuid=" + menuid +"&mobileTabId="+tabId+"&belongPathTree=true&viewScope=all",
                            WfCenterTabTypeEnum.allwf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=all", Util.formatMultiLang(tabTitle, userlanguageStr), viewType,
                            tabId,conditionurl,this.getButtons(centerButtons, "10"));
                } else if ("2".equals(viewType)) {     // 已办事宜
                    List<Map<String, Object>> centerButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.donewf.getTypename(), WfCenterTabTypeEnum.donewf.getPath(),
                            WfCenterTabTypeEnum.donewf.getTreeUrl() + "?ismobile=1&menuid=" + menuid +"&mobileTabId=" + tabId+"&belongPathTree=true&viewScope=done",
                            WfCenterTabTypeEnum.donewf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=done", Util.formatMultiLang(tabTitle, userlanguageStr), viewType, tabId,
                            conditionurl,this.getButtons(centerButtons, "2"));
                } else if ("4".equals(viewType)) {     // 我的请求
                    List<Map<String, Object>> centerButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.minewf.getTypename(), WfCenterTabTypeEnum.minewf.getPath(),
                            WfCenterTabTypeEnum.minewf.getTreeUrl() + "?ismobile=1&menuid=" + menuid + "&mobileTabId=" + tabId+"&belongPathTree=true&viewScope=mine",
                            WfCenterTabTypeEnum.minewf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=mine", Util.formatMultiLang(tabTitle, userlanguageStr), viewType, tabId,
                            conditionurl,this.getButtons(centerButtons, "4"));
                } else if ("14".equals(viewType)) {    // 关注事宜
                    List<Map<String, Object>> centerButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.attenwf.getTypename(), WfCenterTabTypeEnum.attenwf.getPath(),
                            WfCenterTabTypeEnum.attenwf.getTreeUrl() + "?ismobile=1&menuid=" + menuid + "&mobileTabId="+tabId+"&belongPathTree=true&viewScope=attentionAll",
                            WfCenterTabTypeEnum.attenwf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=attentionAll", Util.formatMultiLang(tabTitle, userlanguageStr), viewType, tabId,
                            conditionurl,this.getButtons(centerButtons, "14"));
               /* } else if ("6".equals(viewType)) {     // 督办事宜
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.supervisewf.getTypename(), WfCenterTabTypeEnum.supervisewf.getPath(),
                            WfCenterTabTypeEnum.supervisewf.getTreeUrl() + "?ismobile=1&menuid=" + menuid +"&mobileTabId=" + tabId+"&belongPathTree=true&viewScope=all",
                            WfCenterTabTypeEnum.supervisewf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=all&_ec_device=true", tabTitle, viewType, tabId,
                            conditionurl);*/
                } else {  // 其他
                    List<Map<String, Object>> centerButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
                    tabstemp = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.allwf.getTypename(), WfCenterTabTypeEnum.allwf.getPath(),
                            WfCenterTabTypeEnum.allwf.getTreeUrl() + "?ismobile=1&menuid=" + menuid + "&mobileTabId="+tabId+"&belongPathTree=true&viewScope=all",
                            WfCenterTabTypeEnum.allwf.getDataUrl() + "?ismobile=1&menuid=" + menuid + "&viewScope=all", Util.formatMultiLang(tabTitle, userlanguageStr), viewType, tabId,
                            conditionurl,this.getButtons(centerButtons,viewType));
                }
                tabsList.add(tabstemp);
            }
        } else {    // 返回默认的5个tab页信息
            String viewType = "";
            // 待办  mobileTabId -1
            viewType = WfCenterTabTypeEnum.todowf.getTypeid();
            Tabs todoTabs = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.todowf.getTypename(),WfCenterTabTypeEnum.todowf.getPath(),
                    WfCenterTabTypeEnum.todowf.getTreeUrl()+"?ismobile=1&belongPathTree=true&viewScope=doing&menuid=" + menuid, WfCenterTabTypeEnum.todowf.getDataUrl()+"?ismobile=1&viewScope=doing&menuid=" + menuid, Util.formatMultiLang(WfCenterTabTypeEnum.todowf.getTabName(), userlanguageStr), viewType, "-1",
                    "/api/workflow/reqlist/doingBaseInfo",this.getButtons(wfCenterButtons,viewType));
            // 全部  mobileTabId -2
            viewType = WfCenterTabTypeEnum.allwf.getTypeid();
            List<Map<String, Object>> allCenterButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
            Tabs allTabs = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.allwf.getTypename(),WfCenterTabTypeEnum.allwf.getPath(),
                    WfCenterTabTypeEnum.allwf.getTreeUrl()+"?ismobile=1&belongPathTree=true&viewScope=all&menuid=" + menuid, WfCenterTabTypeEnum.allwf.getDataUrl()+"?ismobile=1&viewScope=all&menuid=" + menuid,Util.formatMultiLang(WfCenterTabTypeEnum.allwf.getTabName(), userlanguageStr), viewType, "-2",
                    "/api/workflow/customQuery/getFixedCondition",this.getButtons(allCenterButtons, viewType));
            // 已办  mobileTabId -3
            viewType = WfCenterTabTypeEnum.donewf.getTypeid();
            List<Map<String, Object>> doneCenterButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
            Tabs doneTabs = new Tabs(Util.getIntValue(viewType),WfCenterTabTypeEnum.donewf.getTypename(),WfCenterTabTypeEnum.donewf.getPath(),
                    WfCenterTabTypeEnum.donewf.getTreeUrl()+"?ismobile=1&belongPathTree=true&viewScope=done&menuid=" + menuid,WfCenterTabTypeEnum.donewf.getDataUrl()+"?ismobile=1&viewScope=done&menuid=" + menuid,Util.formatMultiLang(WfCenterTabTypeEnum.donewf.getTabName(), userlanguageStr),viewType,"-3",
                    "/api/workflow/reqlist/doneBaseInfo",this.getButtons(doneCenterButtons, viewType));
            // 我的请求  mobileTabId -4
            viewType = WfCenterTabTypeEnum.minewf.getTypeid();
            List<Map<String, Object>> mineCenterButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
            Tabs mineTabs = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.minewf.getTypename(),WfCenterTabTypeEnum.minewf.getPath(),
                    WfCenterTabTypeEnum.minewf.getTreeUrl()+"?ismobile=1&belongPathTree=true&viewScope=mine&menuid=" + menuid,WfCenterTabTypeEnum.minewf.getDataUrl()+"?ismobile=1&viewScope=mine&menuid=" + menuid , Util.formatMultiLang(WfCenterTabTypeEnum.minewf.getTabName(), userlanguageStr),viewType,"-4",
                    "/api/workflow/reqlist/mineBaseInfo",this.getButtons(mineCenterButtons, viewType));
            // 我的关注 mobileTabId -5
            viewType = WfCenterTabTypeEnum.attenwf.getTypeid();
            List<Map<String, Object>> attenCenterButtons = new ArrayList<Map<String, Object>>(wfCenterButtons);
            Tabs attenTabs = new Tabs(Util.getIntValue(viewType), WfCenterTabTypeEnum.attenwf.getTypename(),WfCenterTabTypeEnum.attenwf.getPath(),
                    WfCenterTabTypeEnum.attenwf.getTreeUrl()+"?ismobile=1&belongPathTree=true&viewScope=attentionAll&menuid=" + menuid, WfCenterTabTypeEnum.attenwf.getDataUrl()+"?ismobile=1&viewScope=attentionAll&menuid=" + menuid, Util.formatMultiLang(WfCenterTabTypeEnum.attenwf.getTabName(), userlanguageStr),viewType,"-5",
                    "/api/workflow/requestAttention/getCondition_post",this.getButtons(attenCenterButtons, viewType));
            tabsList.add(todoTabs);
            tabsList.add(allTabs);
            tabsList.add(doneTabs);
            tabsList.add(mineTabs);
            tabsList.add(attenTabs);
        }
        return tabsList;
    }

    // 根据 viewType 来过滤下 wfCenterButtons ，只有待办显示 全部已读 和 批量提交按钮
    private List<Map<String, Object>> getButtons(List<Map<String, Object>> wfCenterButtons, String viewType) {
        List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
        if(!wfCenterButtons.isEmpty()){
            for(Map<String,Object> map : wfCenterButtons){
                if(WfCenterOperateTypeEnum.allread.getKey().equals(map.get("key")) || WfCenterOperateTypeEnum.batchsubmit.getKey().equals(map.get("key"))){
                    if(!"1".equals(viewType) && !"11".equals(viewType) && !"12".equals(viewType) && !"5".equals(viewType) && !"15".equals(viewType) && !"7".equals(viewType)){   // 不i是待办/超时
                        continue;
                    }
                }
                Map<String,Object> temp = new HashMap<String,Object>(map);
                result.add(temp);
            }
        }
        return result;
    }

    /**
     * 根据 menuid 获取 流程中心 元素的操作按钮信息
     * @param menuid
     * @return
     */
    private List<Map<String,Object>> getWfCenterButtons(int menuid,boolean isdeleted){
        RecordSet rs = new RecordSet();
        List<Map<String,Object>> buttons = new ArrayList<Map<String,Object>>();
        if(!isdeleted && WorkflowCenterTabBiz.isInitTab(menuid)){
            // 获取 操作按钮信息
            String singleWfid = "";     // 路径来源只选择一个路径是 返回 该路径值
            String mobileurlos = "";//异构系统新建路径
            rs.executeQuery("select sourcetype,contentinfo from workflow_mobilecenter_newwf where menuid = ?",menuid);
            if(rs.next()){
                if(1 == Util.getIntValue(rs.getString("sourcetype"))){   // 选择路径
                    String contentinfo = Util.null2String(rs.getString("contentinfo"));
                    if(!contentinfo.contains(",")){
                        singleWfid = contentinfo;
                        mobileurlos = Util.getIntValue(singleWfid) > 0 ? "" : new OfsWorkflowShareUtils().getCreateUrl(singleWfid,true);
                    }
                }
            }
            Map<String, WfCenterOperateTypeEnum> operateTypes = WfCenterOperateTypeEnum.getOperateTypes();
            rs.executeQuery("select id,typeid,operatename,customname,enable from workflow_mobilecenter_operate where menuid = ? order by orderno",menuid);
            boolean needAddBatchForward = true;
            while(rs.next()){
                Map<String,Object> button = new HashMap<String,Object>();
                String typeid = rs.getString("typeid");
                boolean enable = "1".equals(rs.getString("enable"));
                if(WfCenterOperateTypeEnum.batchforward.getTypeid().equals(typeid)) {
                    needAddBatchForward = false;
                }
                if(!enable) {
                    continue;
                }
                String operatename = SystemEnv.getHtmlLabelName(Util.getIntValue(operateTypes.get(typeid).getOperatelabel()), user.getLanguage());
                String customname = Util.formatMultiLang(Util.null2String(rs.getString("customname")), String.valueOf(user.getLanguage()));
                String key = operateTypes.get(typeid).getKey();
                if("".equals(customname)){
                    customname = operatename;
                }

                if("0".equals(typeid) && !"".equals(singleWfid)){    // 只选择了一条路径
                    button.put("singleWfid",singleWfid);
                    button.put("mobileurlos",mobileurlos);
                }
                button.put("key", key);
                button.put("buttonName",customname);
                buttons.add(button);
            }

            //批量转发特殊处理，只是针对升级前初始化过的数据有效
            if(needAddBatchForward) {
                Map<String,Object> item = new HashMap<String,Object>();
                item.put("key",WfCenterOperateTypeEnum.batchforward.getKey());
                item.put("buttonName",SystemEnv.getHtmlLabelName(Util.getIntValue(WfCenterOperateTypeEnum.batchforward.getOperatelabel()),user.getLanguage()));
                buttons.add(item);
            }
        }else { // 默认的四个按钮
            for(WfCenterOperateTypeEnum operateType : WfCenterOperateTypeEnum.values()) {
                Map<String,Object> item = new HashMap<String,Object>();
                item.put("key",operateType.getKey());
                item.put("buttonName",SystemEnv.getHtmlLabelName(Util.getIntValue(operateType.getOperatelabel()),user.getLanguage()));
                buttons.add(item);
            }
        }
        return buttons;
    }


}
