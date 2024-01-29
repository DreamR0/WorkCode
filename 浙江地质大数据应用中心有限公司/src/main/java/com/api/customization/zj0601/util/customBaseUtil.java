package com.api.customization.zj0601.util;

import com.alibaba.fastjson.JSON;
import com.api.workflow.bean.PageTabInfo;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.workflow.biz.WorkflowCenterBiz;
import com.engine.workflow.biz.mobileCenter.MobileDimensionsBiz;
import com.engine.workflow.biz.requestList.OfsRequestListBiz;
import com.engine.workflow.biz.requestList.RequestListBiz;
import com.engine.workflow.biz.requestList.SearchConditionBiz;
import com.engine.workflow.entity.WorkflowDimensionEntity;
import com.engine.workflow.entity.mobileCenter.Dimensions;
import com.engine.workflow.util.CommonUtil;
import com.engine.workflow.util.WorkflowDimensionUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.AllManagers;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.workflow.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class customBaseUtil {

    public Map<String, Object> getBaseInfo(HttpServletRequest request, User user) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            //高级查询条件信息
            String ismobile = Util.null2String(request.getParameter("ismobile"));
            int menuid = Util.getIntValue(request.getParameter("menuid"), -1);
            //如果是移动端抄送模块过来的请求，需要去掉搜索条件待办状态
            String viewScope = "doing";
            if ("1".equals(ismobile) && menuid != -1) {
                MobileDimensionsBiz mdb = new MobileDimensionsBiz();
                Dimensions di = mdb.getDimension(menuid);
                if ("5".equals(di.getApptype())) {
                    viewScope += "$CS";
                }
            }
            List<Map<String, Object>> conditioninfo = new SearchConditionBiz().getCondition(ismobile, viewScope, user);
            apidatas.put("pagetitle", SystemEnv.getHtmlLabelName(1207, user.getLanguage()));
            if ("1".equals(request.getParameter("ismobile"))) {
                apidatas.put("conditions", conditioninfo);
                apidatas.put("type", 3);
                return apidatas;
            } else {
                apidatas.put("conditioninfo", conditioninfo);
            }

        } catch (Exception e) {
            new BaseBean().writeLog("doingBaseInfo error:" + e);
        }
        return apidatas;
    }

    /**
     * 流程上面的统计信息选项
     * @param request
     * @param user
     * @return
     */
    public Map<String, Object> getCountInfo(HttpServletRequest request, User user) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        Map<String, Object> params = ParamUtil.request2Map(request);
        int allCount = 0;
        String typeids = request.getParameter("tabkeys");
        boolean isOpenSec = HrmClassifiedProtectionBiz.isOpenClassification();//是否开启涉密系统
        String userSecLevel = new HrmClassifiedProtectionBiz().getMaxResourceSecLevel(user);//用户最高资源密级
        List<String> typeidList = new ArrayList<>();
        typeidList = Util.TokenizerString(typeids, ",");
        //临时typeidlist
        List<String> typeidList_temp = new ArrayList<>();
        typeidList_temp.addAll(typeidList);
        typeidList.clear();
        //排除下不存在的id
        WorkflowDoingDimension wdd = new WorkflowDoingDimension();
        if ("" == wdd.getTypeid("1")) {
            wdd.removeCache();
        }
        for (String typeid_temp : typeidList_temp) {
            if (typeid_temp.equals(Util.null2String(wdd.getTypeid(typeid_temp))) && "1".equals(Util.null2String(wdd.getIsShow(typeid_temp))) && !typeidList.contains(typeid_temp) && "doing".equals(wdd.getScope(typeid_temp))) {
                typeidList.add(typeid_temp);
            }
        }
        String ismobile = request.getParameter("ismobile");
        String mobielRange = "";
        String csWhere = "";//抄送加上条件
        String tabName = Util.null2String(request.getParameter("name"));
        boolean defaultTips = false;
        if ("1".equals(ismobile)) {
            int menuid = Util.getIntValue(request.getParameter("menuid"), -1);
            if (menuid != -1) {
                MobileDimensionsBiz mdb = new MobileDimensionsBiz();
                //待办时，对于移动端，过来的可能是待办的全部，也可能是抄送--抄送不需要显示数量
                String sourcetype = mdb.getSourcetypeNew(menuid);    // 1,2 选择/排除路径  3,4选择/排除类型
                String sourceids = mdb.getRange(menuid, sourcetype);
                if (!"".equals(sourcetype) && !"".equals(sourceids)) {
                    String instr = "in";
                    if ("2".equals(sourcetype) || "4".equals(sourcetype)) {
                        instr = "not in";
                    }
                    if ("1".equals(sourcetype) || "2".equals(sourcetype)) {
                        mobielRange = " and " + Util.getSubINClause(sourceids, "t2.workflowid", instr);
                    } else if ("3".equals(sourcetype) || "4".equals(sourcetype)) {
                        mobielRange = " and " + Util.getSubINClause(sourceids, "t2.workflowtype", instr);
                    }
                }
                if ("".equals(tabName) && "1".equals(mdb.getDimension(menuid).getApptype())) {
                    defaultTips = true;
                }
                String scope = mdb.getScope(menuid);
                if ("csdoing".equals(scope)) {
                    typeidList.clear();
                    typeidList.add("9");
                }
                if ("flowAll".equals(tabName)) {
                    typeidList.clear();
                    typeidList.add("0");
                } else if ("flowDoing".equals(tabName)) {
                    typeidList.clear();
                    typeidList.add("5");
                } else if ("flowView".equals(tabName)) {
                    typeidList.clear();
                    typeidList.add("6");
                } else if ("flowReject".equals(tabName)) {
                    typeidList.clear();
                    typeidList.add("7");
                }
            }
        }
        //需要展示流程状态
        if (typeidList.size() == 0) {
           /* typeidList = WorkflowDimensionUtils.getTypeidList("doing");*/
            //固定显示
          /*  typeidList.add("0");
            typeidList.add("5");
            typeidList.add("11");
            typeidList.add("12");
            typeidList.add("1");
            typeidList.add("7");
            typeidList.add("37");*/
            typeidList = WorkflowDimensionUtils.getTypeidList("doShow");
        }
        new BaseBean().writeLog("==zj==(自定义-typeidList)" + typeidList);
        try {
            if (user == null)
                return null;
            RecordSet rs = new RecordSet();
            RecordSet RecordSet = new RecordSet();
            String nullKeyword = CommonUtil.getDBJudgeNullFun(rs.getDBType());
            WorkflowComInfo WorkflowComInfo = new WorkflowComInfo();
            WorkflowAllComInfo wfAllComInfo = new WorkflowAllComInfo();
            AllManagers AllManagers = new AllManagers();

            String resourceid = Util.null2String(request.getParameter("resourceid"));
            AllManagers.getAll(resourceid);
            if ("".equals(resourceid))
                resourceid = "" + user.getUID();
            boolean isSelf = false;
            if (resourceid.equals("" + user.getUID()))
                isSelf = true;
            int usertype = 0;
            if ("2".equals(user.getLogintype()))
                usertype = 1;
            String userID = String.valueOf(user.getUID());
            String userIDAll = String.valueOf(user.getUID());
            String belongtoshow = "";
            RecordSet.executeSql("select * from HrmUserSetting where resourceId = " + userID);
            if (RecordSet.next())
                belongtoshow = RecordSet.getString("belongtoshow");
            //QC235172,如果不是查看自己的代办，主从账号统一显示不需要判断
            if (!isSelf)
                belongtoshow = "";
            if (!"".equals(user.getBelongtoids()))
                userIDAll = userID + "," + user.getBelongtoids();

            boolean superior = false; //是否为被查看者上级或者本身
            if ("".equals(resourceid) || userID.equals(resourceid)) {
                resourceid = userID;
                superior = true;
            } else {
                rs.executeSql("SELECT * FROM HrmResource WHERE ID = " + resourceid + " AND managerStr LIKE '%," + userID + ",%'");
                if (rs.next())
                    superior = true;
            }
            params.put("isMergeShow", "1".equals(belongtoshow) && !userIDAll.equals(String.valueOf(user.getUID())) ? "1" : "0");//设置开启并且有次账号
            boolean isQueryByNewTable = RequestListBiz.isQueryByNewTable(user, params);

            String tworkflowNodeIDs = "";
            ArrayList<String> wftypeList = new ArrayList<String>();
            ArrayList<String> workflowList = new ArrayList<String>();
            Map<String, Integer> flowallmap = new Hashtable<String, Integer>();//待办数量
            Map<String, Integer> flownewmap = new Hashtable<String, Integer>();//待办数量


            StringBuffer sqlsb = new StringBuffer();
            sqlsb.append("select distinct t2.workflowtype, t2.workflowid from workflow_currentoperator t2,workflow_requestbase t1,workflow_base t3 ");
            sqlsb.append("	  where t1.requestid = t2.requestid and t1.workflowid = t2.workflowid and t2.workflowid = t3.id ");
            sqlsb.append("	  and ( (isremark=0 and (takisremark is null or takisremark=0)) or isremark in(1,5,7,8,9,11))");
            sqlsb.append(RequestListBiz.getCommonCondition(user, nullKeyword, ("1".equals(belongtoshow) ? userIDAll : resourceid)));//统一入口拼接通用条件
            sqlsb.append("   and islasttimes = 1 ");

            //--------------------流程中心--more页面条件拼接------------------------
            if ("wfcenter_todo".equals(Util.null2String(params.get("source")))) {
                WorkflowCenterBiz.resetParams(params, "doing", user);
            }
            if (!params.containsKey("inornot")) {
                params.put("inornot", " in ");
            }
            String inornot = Util.null2String(params.get("inornot"));
            //--------------------流程中心--more页面条件拼接------------------------

            //左侧树可以拼接url中输入的路径和类型参数
            String workflowidtmp = WorkflowVersion.getAllVersionStringByWFIDs(Util.null2String(params.get("workflowid")));
            String workflowtypetmp = Util.null2String(params.get("workflowtype"));
            if (!"".equals(workflowidtmp)) {
                //sqlsb.append(" and workflowid in("+workflowidtmp+") ");
                sqlsb.append(" and " + Util.getSubINClause(workflowidtmp, "t2.workflowid", inornot));
            }
            if (!"".equals(workflowtypetmp)) {
                //sqlsb.append(" and workflowtype in("+workflowtypetmp+") ");
                sqlsb.append(" and " + Util.getSubINClause(workflowtypetmp, "t2.workflowtype", inornot));
            }
            //左侧树可以拼接url中输入的路径和类型参数
            sqlsb.append(" and t2.userid in (").append("1".equals(belongtoshow) ? userIDAll : resourceid).append("  ) and usertype = ").append(usertype);
            sqlsb.append(" and t3.isvalid in('1','3') ");
            sqlsb.append(RequestListBiz.buildWfRangeWhereClause(request, "t2.workflowid"));
            if (!typeidList.contains("0") && typeidList.size() > 0) {
                String sqlwheretmp = "";
                for (String typeid : typeidList) {
                    sqlwheretmp += WorkflowDimensionUtils.getToDoSqlWhere(typeid, "or", user);
                }
                if (!"".equals(sqlwheretmp)) {
                    sqlsb.append(" and (1=2 " + sqlwheretmp + " ) ");
                }
            }
            if (!superior) {
                if ("1".equals(belongtoshow)) {
                    sqlsb.append(" AND EXISTS (SELECT NULL FROM workFlow_CurrentOperator b WHERE t2.workflowid = b.workflowid AND t2.requestid = b.requestid AND b.userid in ("
                            + userIDAll + ") and b.usertype= " + usertype + ") ");
                } else {
                    sqlsb.append(" AND EXISTS (SELECT NULL FROM workFlow_CurrentOperator b WHERE t2.workflowid = b.workflowid AND t2.requestid = b.requestid AND b.userid in ("
                            + user.getUID() + ") and b.usertype= " + usertype + ") ");
                }
            }
            sqlsb.append(mobielRange);//添加移动端限制的workflowid条件
            sqlsb.append(csWhere);
            sqlsb.append(" order by t2.workflowtype, t2.workflowid ");
            RecordSet.executeSql(isQueryByNewTable ? RequestListBiz.transNewTable(user, sqlsb.toString()) : sqlsb.toString());
            while (RecordSet.next()) {
                String theworkflowid = Util.null2String(RecordSet.getString("workflowid"));
                String theworkflowtype = Util.null2String(RecordSet.getString("workflowtype"));
                if (!"1".equals(wfAllComInfo.getIsValid(theworkflowid))) {        // 不是有效
                    if (!"3".equals(wfAllComInfo.getIsValid(theworkflowid))) {    // 不是有效，不是历史为无效状态，无效状态过滤掉
                        continue;
                    } else {
                        theworkflowid = wfAllComInfo.getActiveversionid(theworkflowid);
                    }
                }
                if (wftypeList.indexOf(theworkflowtype) == -1)
                    wftypeList.add(theworkflowtype);
                if (workflowList.indexOf(theworkflowid) == -1)
                    workflowList.add(theworkflowid);
            }
            StringBuffer wftypesb = new StringBuffer();
            StringBuffer wfsb = new StringBuffer();
            for (String _typeid : wftypeList) {
                wftypesb.append(",").append(_typeid);
            }
            for (String _wfid : workflowList) {
                wfsb.append(",").append(WorkflowVersion.getAllVersionStringByWFIDs(_wfid));
            }
            if (wfsb.length() > 0)
                wfsb = wfsb.delete(0, 1);
            //需要重新取一下workflowtype的值  因为currentoperator表中有些数据type存的不是wfid的实际type  如果以后解决这个问题  这段代码可以去掉--start
            if (wfsb.length() > 0) {
                rs.executeQuery("select distinct workflowtype from workflow_base where id in(" + wfsb.toString() + ")");
                wftypeList.clear();
                wftypesb.setLength(0);
                while (rs.next()) {
                    wftypeList.add(rs.getString("workflowtype"));
                    wftypesb.append(",").append(rs.getString("workflowtype"));
                }
            }
            //----end
            if (wftypesb.length() > 0)
                wftypesb = wftypesb.delete(0, 1);

            /******************未读流程、反馈流程计数*********************/
            sqlsb = new StringBuffer();
            //==zj 这里显示全部流程（不分已办或待办） ‘2’
            /*String sqlwhere = "((isremark='0' and (takisremark is null or takisremark=0)) or isremark in ('1','2','7','8','9','11')) and islasttimes=1 and isprocessed is null ";*/
            String sqlwhere = " islasttimes=1 and isprocessed is null ";
            if ("1".equals(belongtoshow)) {
                sqlsb.append("select t2.viewtype,t2.workflowtype, t2.workflowid,  t2.isremark,t1.currentnodetype, count(t2.requestid) workflowcount ");
            } else {
                sqlsb.append("select t2.viewtype,t2.workflowtype, t2.workflowid, t2.isremark,t1.currentnodetype, count(distinct t2.requestid) workflowcount ");
            }
            sqlsb.append("	  from workflow_currentoperator t2,workflow_requestbase t1 ");
            sqlsb.append("	  where t2.requestid = t1.requestid and ").append(sqlwhere);
            sqlsb.append(RequestListBiz.getCommonCondition(user, nullKeyword, ("1".equals(belongtoshow) ? userIDAll : resourceid)));//统一入口拼接通用条件
            sqlsb.append(mobielRange);//添加移动端限制的workflowid条件
            sqlsb.append(csWhere);//移动端抄送条件
            sqlsb.append(" and t2.userid in (").append("1".equals(belongtoshow) ? userIDAll : resourceid).append("  ) and usertype = ").append(usertype);
            if (!"".equals(wftypesb.toString())) {
                sqlsb.append("	    and t2.workflowtype in ( ").append(wftypesb).append(") ");
            }
            if (!"".equals(wfsb.toString())) {
                sqlsb.append("	    and t2.workflowid in (").append(wfsb).append(")");
            }

            if (!"".equals(tworkflowNodeIDs)) {
                sqlsb.append(" and t2.nodeid in (" + WorkflowVersion.getAllRelationNodeStringByNodeIDs(tworkflowNodeIDs) + ") ");
            }
            if (!superior) {
                if ("1".equals(belongtoshow)) {
                    sqlsb.append(" AND EXISTS (SELECT NULL FROM workFlow_CurrentOperator b WHERE t2.workflowid = b.workflowid AND t2.requestid = b.requestid AND b.userid in ("
                            + userIDAll + ") and b.usertype= " + usertype + ") ");
                } else {
                    sqlsb.append(" AND EXISTS (SELECT NULL FROM workFlow_CurrentOperator b WHERE t2.workflowid = b.workflowid AND t2.requestid = b.requestid AND b.userid in ("
                            + user.getUID() + ") and b.usertype= " + usertype + ") ");
                }
            }

            String sqlBack = sqlsb.toString();
            sqlsb.append(" group by t2.viewtype,t2.workflowtype, t2.workflowid, t2.isremark, t1.currentnodetype ");
            new BaseBean().writeLog("==zj==(待办未读反馈计数SQL)" + sqlsb.toString());
            //System.out.println("待办未读反馈计数SQL----" + sqlsb.toString());
            rs.executeSql(isQueryByNewTable ? RequestListBiz.transNewTable(user, sqlsb.toString()) : sqlsb.toString());
            while (rs.next()) {
                String _wfid = Util.null2String(rs.getString("workflowid"));
                if (!"1".equals(wfAllComInfo.getIsValid(_wfid))) {
                    if (!"3".equals(wfAllComInfo.getIsValid(_wfid))) {
                        continue;
                    } else {
                        _wfid = wfAllComInfo.getActiveversionid(_wfid);
                    }
                }
                int _count = Util.getIntValue(rs.getString("workflowcount"), 0);
                int viewtype = Util.getIntValue(rs.getString("viewtype"), 2);
                int wfindex = workflowList.indexOf(_wfid);
                String isremark = Util.null2String(rs.getString("isremark"));
                String currentnodetype = Util.null2String(rs.getString("currentnodetype"));

                if (wfindex != -1) {
                    int flowall_temp = flowallmap.containsKey(_wfid) ? flowallmap.get(_wfid) : 0;
                    flowall_temp += _count;
                    flowallmap.put(_wfid, flowall_temp);
                    if (viewtype == 0 && (!"3".equals(currentnodetype) || (("3").equals(currentnodetype) && "1,8,9,11".contains(isremark)))) {    // 未读计数
                        int flownew_temp = flownewmap.containsKey(_wfid) ? flownewmap.get(_wfid) : 0;
                        flownew_temp += _count;
                        flownewmap.put(_wfid, flownew_temp);
                    }
                }
            }
            new BaseBean().writeLog("==zj==(flowallmap)" + JSON.toJSONString(flowallmap));

            //统计待阅、待处理、退回数量--//统计基础五种维度之外的维度数据
            Map<String, Map<String, Integer>> newdimensionmap = new HashMap();
            for (String typeidlist : typeidList) {
                //我的关注
                boolean isAttention = "88".equals(typeidlist) ? true : false;

                sqlsb = new StringBuffer();
                //未归档、已归档--//单独处理
                if ("84".equals(typeidlist)){
                    sqlwhere = "((isremark='2' and (takisremark is null or takisremark=0)) or isremark in ('1','5','7','8','9','11')) and islasttimes=1";
                }else if("85".equals(typeidlist)){
                    sqlwhere = "(t2.isremark IN('2','4') OR (t2.isremark='0' AND t2.takisremark =-2)) AND t2.islasttimes=1";
                }
                else if("88".equals(typeidlist)){
                    sqlwhere = "((isremark='0' and (takisremark is null or takisremark=0)) or isremark in ('1','2','5','7','8','9','11')) and islasttimes=1";
                }else {
                    sqlwhere = "((isremark='0' and (takisremark is null or takisremark=0)) or isremark in ('1','5','7','8','9','11')) and islasttimes=1";
                }
                if ("1".equals(belongtoshow)) {
                    sqlsb.append("select t2.workflowtype, t2.workflowid, count(t2.requestid) workflowcount ");
                } else {
                    sqlsb.append("select t2.workflowtype, t2.workflowid, count(distinct t2.requestid) workflowcount ");
                }
                sqlsb.append("	  from workflow_currentoperator t2,workflow_requestbase t1 ");
                sqlsb.append("	  where t1.requestid = t2.requestid and ").append(sqlwhere);
                sqlsb.append(RequestListBiz.getCommonCondition(user, nullKeyword, ("1".equals(belongtoshow) ? userIDAll : resourceid)));//统一入口拼接通用条件
                sqlsb.append(" and t2.userid in (").append("1".equals(belongtoshow) ? userIDAll : resourceid).append("  ) and usertype = ").append(usertype);
                if (!"".equals(wftypesb.toString())) {
                    sqlsb.append("	    and t2.workflowtype in ( ").append(wftypesb).append(") ");
                    //sqlsb.append("   and exists(select 1 from workflow_type t where id in(").append(wftypesb).append(") and t.id=t2.workflowtype)  ");
                }
                if (!"".equals(wfsb.toString())) {
                    sqlsb.append("	    and t2.workflowid in (").append(wfsb).append(")");
                    //sqlsb.append("   and exists(select 1 from workflow_base b where id in(").append(wfsb).append(") and b.id=t2.workflowid)  ");
                }

                if (isAttention) {
                    sqlsb.append(" and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=t2.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ")");
                }

                if (Util.getIntValue(typeidlist, 0) > 4) {
                    sqlsb.append(WorkflowDimensionUtils.getToDoSqlWhere(typeidlist, "and", user));
                    if (!"".equals(tworkflowNodeIDs)) {
                        sqlsb.append(" and t2.nodeid in (" + WorkflowVersion.getAllRelationNodeStringByNodeIDs(tworkflowNodeIDs) + ") ");
                    }
                    if (!superior) {
                        if ("1".equals(belongtoshow)) {
                            sqlsb.append(" AND EXISTS (SELECT NULL FROM workFlow_CurrentOperator b WHERE t2.workflowid = b.workflowid AND t2.requestid = b.requestid AND b.userid in ("
                                    + userIDAll + ") and b.usertype= " + usertype + ") ");
                        } else {
                            sqlsb.append(" AND EXISTS (SELECT NULL FROM workFlow_CurrentOperator b WHERE t2.workflowid = b.workflowid AND t2.requestid = b.requestid AND b.userid in ("
                                    + user.getUID() + ") and b.usertype= " + usertype + ") ");
                        }
                    }
                    sqlsb.append(mobielRange);
                    sqlsb.append(" group by t2.workflowtype, t2.workflowid");
                    rs.executeSql(isQueryByNewTable ? RequestListBiz.transNewTable(user, sqlsb.toString()) : sqlsb.toString());
                    new BaseBean().writeLog("==zj==(自定义-基础维度之外-typelist)" + typeidlist);
                    new BaseBean().writeLog("==zj==(自定义-基础维度之外-sql)" + sqlsb.toString());
                    Map<String, Integer> dimensionmap = new HashMap<>();
                    while (rs.next()) {
                        String tworkflowid = Util.null2String(rs.getString("workflowid"));
                        if (!"1".equals(wfAllComInfo.getIsValid(tworkflowid))) {
                            if (!"3".equals(wfAllComInfo.getIsValid(tworkflowid))) {
                                continue;
                            } else {
                                tworkflowid = wfAllComInfo.getActiveversionid(tworkflowid);
                            }
                        }
                        int _count = rs.getInt(3);
                        int flowreject_temp = dimensionmap.containsKey(tworkflowid) ? dimensionmap.get(tworkflowid) : 0;
                        flowreject_temp += _count;
                        dimensionmap.put(tworkflowid, flowreject_temp);
                    }
                    newdimensionmap.put(typeidlist, dimensionmap);
                }
            }



            /**************生成计数对象*****************/
            Map<String, Map<String, String>> countmap = new HashMap<String, Map<String, String>>();
            int total_all = 0;
            int total_new = 0;

            List<String> workflowid_temp = new ArrayList<>();
            workflowid_temp.addAll(workflowList);
            List<String> workflowid_tempnew = new ArrayList<>();
            Map<String, Integer> totle_map = new HashMap<>();
            for (int i = 0; i < wftypeList.size(); i++) {
                String typeid = (String) wftypeList.get(i);
                int type_all = 0;
                int type_new = 0;

                Map<String, Integer> type_Map = new HashMap<>();
                new BaseBean().writeLog("==zj==(workflowList)"+workflowList);
                for (int j = 0; j < workflowList.size(); j++) {
                    String workflowid = (String) workflowList.get(j);
                    String curtypeid = wfAllComInfo.getWorkflowtype(workflowid);
                    if (!curtypeid.equals(typeid)) {
                        continue;
                    }
                    if (!workflowid_tempnew.contains(workflowid)) {
                        workflowid_tempnew.add(workflowid);
                    }
                    int wf_all = Util.getIntValue(flowallmap.get(workflowid) + "", 0);
                    int wf_new = Util.getIntValue(flownewmap.get(workflowid) + "", 0);

                    Map<String, String> wfcountmap = new HashMap<String, String>();
                    wfcountmap.put("domid", "wf_" + workflowid);
                    wfcountmap.put("keyid", workflowid);
                    wfcountmap.put("workflowname", WorkflowComInfo.getWorkflowname(workflowid));
                    wfcountmap.put("flowAll", wf_all + "");
                    wfcountmap.put("flowNew", wf_new + "");

                    int wf_newDimension = 0;
                    //统计基础五种维度之外的维度数据
                    for (String typeidlist : typeidList) {
                        if (Util.getIntValue(typeidlist, 0) > 4) {
                            if (newdimensionmap.get(typeidlist) != null) {
                                wf_newDimension = Util.getIntValue(newdimensionmap.get(typeidlist).get(workflowid) + "", 0);
                                int type_newDimension = type_Map.containsKey(typeidlist) ? type_Map.get(typeidlist) : 0;
                                type_newDimension += wf_newDimension;
                                type_Map.put(typeidlist, type_newDimension);
                                int totle_newDimension = totle_map.containsKey(typeidlist) ? totle_map.get(typeidlist) : 0;
                                totle_newDimension += wf_newDimension;
                                totle_map.put(typeidlist, totle_newDimension);
                                wfcountmap.put(wdd.getTypename(typeidlist), wf_newDimension + "");
                            }
                        }
                    }
                    countmap.put(wfcountmap.get("domid"), wfcountmap);
                    type_all += wf_all;

                    type_new += wf_new;

                    total_all += wf_all;
                    total_new += wf_new;

                }
                Map<String, String> typecountmap = new HashMap<String, String>();
                typecountmap.put("domid", "type_" + typeid);
                typecountmap.put("keyid", typeid);
                typecountmap.put("flowAll", type_all + "");
                typecountmap.put("flowNew", type_new + "");
                new BaseBean().writeLog("==zj==(flowAll)"+type_all);
                new BaseBean().writeLog("==zj==(flowNew)"+type_new);
                //统计基础五种维度之外的维度数据
                for (String typeidlist : typeidList) {
                    if (Util.getIntValue(typeidlist, 0) > 4 && Util.getIntValue(typeidlist, 0) != 82  && Util.getIntValue(typeidlist, 0) != 86) {
                        typecountmap.put(wdd.getTypename(typeidlist), type_Map.get(typeidlist) + "");
                    }
                }
                countmap.put(typecountmap.get("domid"), typecountmap);
            }

            workflowid_temp.removeAll(workflowid_tempnew);

            //集成异构系统数据
            //前台传参typeids时，如果不包含0，1则不返回异构系统数据
            Map<String, Object> ofsData = new HashMap<>();
            if (!typeidList.contains("82") && typeidList.contains("83")) {
                ofsData = new OfsRequestListUtil(params, user).extendCountData("flowNew", countmap, user, typeidList, resourceid);
            } else {
                ofsData = new OfsRequestListUtil(params, user).extendCountData("doing", countmap, user, typeidList, resourceid);
            }
            new BaseBean().writeLog("==zj==(ofsData)"+JSON.toJSONString(ofsData));
            if (ofsData != null) {
                total_all += Util.getIntValue(ofsData.get("totalAllCount") + "", 0);
                total_new += Util.getIntValue(ofsData.get("totalNewCount") + "", 0);
                for (String typeidlist : typeidList) {
                    String total_temp = totle_map.get(typeidlist) + "";
                    if ("83".equals(typeidlist)) {//待处理异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleFlowDoing") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    } else if ("84".equals(typeidlist)) {//未归档异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleUnFinish") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    }else if ("85".equals(typeidlist)){//已归档异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleFinish") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    }else if ("86".equals(typeidList)){//未读异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleNew") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    }else if ("87".equals(typeidlist)){//退回异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleReject") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    }
                    else if ("88".equals(typeidlist)) {//我的关注 异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleAttentionCount") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    } else if ("9".equals(typeidlist)) {//抄送 异构系统流程计数
                        totle_map.put(typeidlist, Util.getIntValue(ofsData.get("totleFlowCS") + "", 0) + Util.getIntValue("null".equals(total_temp) ? "0" : total_temp));
                    }
                }
            }
            //}
            //生成全部流程总计信息
            Map<String, String> totalcountmap = new HashMap<String, String>();
            totalcountmap.put("flowAll", total_all + "");
            totalcountmap.put("flowNew", total_new + "");
            new BaseBean().writeLog("==zj==(生成全部流程总计信息)totalcountmap:" + JSON.toJSONString(totalcountmap));
            for (String typeidlist : typeidList) {
                new BaseBean().writeLog("==zj==(生成全部流程总计信息--typeidlist)" + typeidlist);
                if (Util.getIntValue(typeidlist, 0) > 4 && Util.getIntValue(typeidlist, 0) != 82 && Util.getIntValue(typeidlist, 0) != 86) {
                    String totle_temp = totle_map.get(typeidlist) + "";     //这里将totle_map中的值取出,放到totalcountmap中
                    //这里单独处理下未归档和已归档的数据
                    if ("84".equals(typeidlist)) {
                        int flowUnFinish = "null".equals(totle_temp) ? 0 : Integer.parseInt(totle_temp);
                        new BaseBean().writeLog("==zj==(已处理未归档)" + flowUnFinish);
                        int flowDoing = totle_map.get("83");   //获取待处理的数据
                        new BaseBean().writeLog("==zj==(待处理未归档)" + flowDoing);
                        totle_temp = flowUnFinish + flowDoing + "";
                    }
                    totalcountmap.put(wdd.getTypename(typeidlist), "null".equals(totle_temp) ? "0" : totle_temp);
                }
            }
            if ("1".equals(ismobile)) {
                if ("flowAll".equals(tabName) || defaultTips) {
                    String tip = SystemEnv.getHtmlLabelName(23631, user.getLanguage()) + total_all + SystemEnv.getHtmlLabelName(18256, user.getLanguage())
                            + SystemEnv.getHtmlLabelName(1207, user.getLanguage());
                    apidatas.put("title", tip);
                } else if ("flowDoing".equals(tabName)) {
                    String tip = SystemEnv.getHtmlLabelName(23631, user.getLanguage()) + (totle_map.get("5") == null ? "0" : totle_map.get("5")) + SystemEnv.getHtmlLabelName(18256, user.getLanguage())
                            + SystemEnv.getHtmlLabelName(10000914, user.getLanguage());
                    apidatas.put("title", tip);
                } else if ("flowView".equals(tabName)) {
                    String tip = SystemEnv.getHtmlLabelName(23631, user.getLanguage()) + (totle_map.get("6") == null ? "0" : totle_map.get("6")) + SystemEnv.getHtmlLabelName(18256, user.getLanguage())
                            + SystemEnv.getHtmlLabelName(10000915, user.getLanguage());
                    apidatas.put("title", tip);
                } else if ("flowReject".equals(tabName)) {
                    String tip = SystemEnv.getHtmlLabelName(23631, user.getLanguage()) + (totle_map.get("7") == null ? "0" : totle_map.get("7")) + SystemEnv.getHtmlLabelName(18256, user.getLanguage())
                            + SystemEnv.getHtmlLabelName(10000916, user.getLanguage());
                    apidatas.put("title", tip);
                }
            }

            for (Map.Entry<String, String> entry : totalcountmap.entrySet()) {
                allCount += Util.getIntValue(entry.getValue(), 0);
            }
            new BaseBean().writeLog("==zj==(allCount)" + allCount);
            new BaseBean().writeLog("==zj==(totalcountmap)" + JSON.toJSONString(totalcountmap));
            apidatas.put("allCount", allCount);
            apidatas.put("totalcount", totalcountmap);
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(count报错)" +e);
            e.printStackTrace();
        }
        apidatas.put("topTab", getGroupInfo(typeidList, user));
        apidatas.put("hideNoDataTab", "1".equals(new WorkflowConfigComInfo().getValue("hideNoDataTab")) && allCount > 0);
        return apidatas;
    }

    /**
     * 获取列表tab信息
     * @param typeidList
     * @param user
     * @return
     */
    public List<PageTabInfo> getGroupInfo( List<String> typeidList,User user){
        List<PageTabInfo> groupinfo  = new ArrayList<PageTabInfo>();
        for(String typeid : typeidList){
            WorkflowDimensionEntity wde = new WorkflowDimensionEntity(Integer.parseInt(typeid));
            WorkflowDimensionUtils.getGroupInfo(wde,user.getLanguage());
            groupinfo.add(new PageTabInfo(wde));
        }

        return groupinfo;
    }
}
