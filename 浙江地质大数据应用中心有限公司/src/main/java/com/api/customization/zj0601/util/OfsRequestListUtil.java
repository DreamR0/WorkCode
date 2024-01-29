package com.api.customization.zj0601.util;

import com.alibaba.fastjson.JSON;
import com.api.workflow.bean.WfTreeNode;
import com.engine.workflow.biz.WorkflowCenterBiz;
import com.engine.workflow.biz.mobileCenter.MobileDimensionsBiz;
import com.engine.workflow.util.WorkflowDimensionUtils;
import weaver.conn.ConnectionPool;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.ofs.manager.utils.OfsTodoDataUtils;
import weaver.workflow.request.todo.OfsSettingObject;
import weaver.workflow.request.todo.RequestUtil;
import weaver.workflow.workflow.WorkflowVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfsRequestListUtil {

    private Map<String,Object> params = new HashMap<>();
    private User user;
    private boolean showDone = true;

    public OfsRequestListUtil(){}

    public OfsRequestListUtil(Map<String,Object> params,User user){
        this.params = params;
        if(this.params == null)
            this.params = new HashMap<>();
        //--------------------流程中心--more页面条件拼接------------------------
        if("wfcenter_todo".equals(Util.null2String(this.params.get("source")))){
            WorkflowCenterBiz.resetParams(this.params,"doing",user);
        }
        if(!this.params.containsKey("inornot")){
            this.params.put("inornot"," in ");
        }
        //--------------------流程中心--more页面条件拼接------------------------
        this.user = user;
    }

    private boolean supportOfs(User user){
        int usertype = "2".equals(user.getLogintype()) ? 1 : 0;
        OfsSettingObject ofsSetting = new RequestUtil().getOfsSetting();
        this.showDone = "1".equals(ofsSetting.getShowdone());
        if(ofsSetting.getIsuse() == 1 && usertype == 0)
            return true;
        else
            return false;
    }

    /**
     * 判断是内部用户还是外部用户的公共方法
     * @param user
     * @return
     */
    public boolean supportOfs4OtherCall(User user){
        return  "1".equals(user.getLogintype());
    }

    /**
     * 扩展树节点数据
     */
    public void extendsTreeData(String flag, List<WfTreeNode> tree, User user, List<String> typeidList, String resourceid){
        if(!this.supportOfs(user))
            return;
        Map<String,Object> base = this.generateOfsBase(flag, user,typeidList,resourceid);
        if(base == null)
            return;
        Map<String,Map<String,String>> ofsCounts = (Map<String,Map<String,String>>)base.get("ofsCounts");
        String wfidRangeStr = base.get("wfidRangeStr")+"";
        RequestUtil requestutil = new RequestUtil();
        OfsSettingObject ofso = requestutil.getOfsSetting();
        String showSysname = ofso.getShowsysname();
        boolean showname = ("1".equals(showSysname) || "2".equals(showSysname));
        RecordSet rs = new RecordSet();
        String searchName=Util.null2String(params.get("name"));
        String sql = "select a.workflowid,a.workflowname,b.sysid,b.sysshortname,b.sysfullname,b.showorder from ofs_workflow a, ofs_sysinfo b where a.sysid=b.sysid " +
                ("1".equals(params.get("ismobile"))&&!"".equals(searchName)?(" and ("+("2".equals(showSysname)?" b.sysfullname ":" b.sysshortname ")+" like '%"+searchName+"%' or a.workflowname like '%"+searchName+"%')"):"") +
                " and a.workflowid in ("+wfidRangeStr+") and a.Cancel=0 ";
        // 通过集成方法获取不需要显示的异构系统id
        int showType = "1".equals(params.get("ismobile")) ? 1 : 0;
        sql += getOfsConfigWhere(showType,"b.");
        sql += " order by a.sysid desc,a.workflowid desc";
        rs.executeQuery(sql);
        String preSysid = "";
        String preSysname = "";
        float preDsporder = 0;
        List<WfTreeNode> childs = new ArrayList<WfTreeNode>();
        while(rs.next()){
            String wfid = Util.null2String(rs.getString("workflowid"));
            String wfname = Util.null2String(rs.getString("workflowname"));
            String sysid = Util.null2String(rs.getString("sysid"));
            String sysname = "2".equals(showSysname) ? Util.null2String(rs.getString("sysfullname")) : Util.null2String(rs.getString("sysshortname"));
            float dsporder = Util.getFloatValue(rs.getString("showorder"),0);
            Map<String,String> count = ofsCounts.get(wfid);
            if(count == null)
                continue;
            int allcount = Util.getIntValue(count.get("allcount"), 0);
            if(allcount == 0)
                continue;
            if(!"".equals(preSysid) && !sysid.equals(preSysid)){
                WfTreeNode typenode = new WfTreeNode();
                typenode.setDomid("type_"+preSysid);
                typenode.setKey(preSysid);
                typenode.setName(preSysname);
                typenode.setIsopen(true);
                typenode.setHaschild(true);
                typenode.setChilds(childs);
                typenode.setDsporder(preDsporder);//设置排序
                if(childs.size() > 0)
                    tree.add(typenode);

                childs = new ArrayList<WfTreeNode>();
            }

            WfTreeNode wfnode = new WfTreeNode();
            wfnode.setDomid("wf_"+wfid);
            wfnode.setKey(wfid);
            wfnode.setName(wfname);
            childs.add(wfnode);

            preSysid = sysid;
            preSysname = sysname;
            preDsporder = dsporder;
        }
        //最后一条put
        WfTreeNode typenode = new WfTreeNode();
        typenode.setDomid("type_"+preSysid);
        typenode.setKey(preSysid);
        typenode.setName(preSysname);
        typenode.setIsopen(true);
        typenode.setHaschild(true);
        typenode.setChilds(childs);
        typenode.setDsporder(preDsporder);
        if(childs.size() > 0)
            tree.add(typenode);
    }

    /**
     * 扩展计数数据
     */
    public Map<String,Object> extendCountData(String flag, Map<String,Map<String,String>> countmap, User user,List<String> typeidList,String resourceid){
        if(!this.supportOfs(user))
            return null;
        Map<String,Object> base = this.generateOfsBase(flag, user,typeidList,resourceid);
        new BaseBean().writeLog("==zj==(异构计数base)" + JSON.toJSONString(base));
        if(base == null)
            return null;
        Map<String,Map<String,String>> ofsCounts = (Map<String,Map<String,String>>)base.get("ofsCounts");
        String wfidRangeStr = base.get("wfidRangeStr")+"";
        new BaseBean().writeLog("==zj==(wfidRangeStr)" + wfidRangeStr);
        StringBuilder sql_sb = new StringBuilder("select a.workflowid,a.workflowname,b.sysid,b.sysshortname from ofs_workflow a, ofs_sysinfo b");
        sql_sb.append(" where ").append(Util.getSubINClause(wfidRangeStr,"a.workflowid","in")).append(" and a.sysid=b.sysid and a.Cancel=0 ");
        // 调用集成方法获取不显示的异构系统id
        int showType = "1".equals(params.get("ismobile")) ? 1 : 0;
        sql_sb.append(getOfsConfigWhere(showType,"b."));
        sql_sb.append(" order by a.sysid desc,a.workflowid desc ");
        RecordSet rs = new RecordSet();
        rs.executeQuery(sql_sb.toString());
        int sysAllCount = 0;
        int sysNewCount = 0;
        int sysFlowDoingCount = 0;
        int sysFlowViewCount = 0;
        int sysFlowAttentionCount = 0;
        int sysFlowCSCount = 0;
        String preSysid = "";
        while(rs.next()){
            String wfid = Util.null2String(rs.getString("workflowid"));
            String sysid = Util.null2String(rs.getString("sysid"));
            Map<String,String> count = ofsCounts.get(wfid);
            if(count == null)
                continue;
            int newcount = Util.getIntValue(count.get("newcount"), 0);
            int allcount = Util.getIntValue(count.get("allcount"), 0);
            int flowdoingcount = Util.getIntValue(count.get("flowdoingcount"), 0);
            int flowviewcount = Util.getIntValue(count.get("flowviewcount"), 0);
            int flowAttentionCount = Util.getIntValue(count.get("flowAttentionCount"), 0);
            int flowCScount = Util.getIntValue(count.get("flowCScount"), 0);
            if(allcount == 0)
                continue;
            //两条记录不同的类型时，把上一类型数据put进去
            if(!"".equals(preSysid) && !sysid.equals(preSysid)){
                Map<String,String> typecountmap = new HashMap<String,String>();
                typecountmap.put("domid", "type_"+preSysid);
                typecountmap.put("keyid", preSysid);
                typecountmap.put("flowAll", sysAllCount+"");
                typecountmap.put("flowNew", sysNewCount+"");
                typecountmap.put("flowRes", "0");
                typecountmap.put("flowOver", "0");
                typecountmap.put("flowSup", "0");
                typecountmap.put("flowDoing",sysFlowDoingCount+"");
                typecountmap.put("flowView",sysFlowViewCount+"");
                typecountmap.put("flowCS",sysFlowCSCount+"");
                if(sysAllCount > 0)
                    countmap.put(typecountmap.get("domid"), typecountmap);
                //重置
                sysAllCount = 0;
                sysNewCount = 0;
                sysFlowDoingCount = 0;
                sysFlowViewCount = 0;
                sysFlowCSCount = 0;
                preSysid = "";
            }
            Map<String,String> wfcountmap = new HashMap<String,String>();
            wfcountmap.put("domid", "wf_"+wfid);
            wfcountmap.put("keyid", wfid);
            wfcountmap.put("flowAll", allcount + "");
            wfcountmap.put("flowNew", newcount + "");
            wfcountmap.put("flowDoing", flowdoingcount + "");
            wfcountmap.put("flowView", flowviewcount + "");
            wfcountmap.put("flowRes", "0");
            wfcountmap.put("flowOver", "0");
            wfcountmap.put("flowSup", "0");
            wfcountmap.put("flowAttention", flowAttentionCount + "");
            wfcountmap.put("flowCS", flowCScount + "");
            if(allcount > 0)
                countmap.put(wfcountmap.get("domid"), wfcountmap);

            sysAllCount += allcount;
            sysNewCount += newcount;
            sysFlowDoingCount += flowdoingcount;
            sysFlowViewCount += flowviewcount;
            sysFlowAttentionCount += flowAttentionCount;
            sysFlowCSCount += flowCScount;
            preSysid = sysid;
        }
        //最后一条put
        Map<String,String> typecountmap = new HashMap<String,String>();
        typecountmap.put("domid", "type_"+preSysid);
        typecountmap.put("keyid", preSysid);
        typecountmap.put("flowAll", sysAllCount+"");
        typecountmap.put("flowNew", sysNewCount+"");
        typecountmap.put("flowRes", "0");
        typecountmap.put("flowOver", "0");
        typecountmap.put("flowSup", "0");
        typecountmap.put("flowDoing",sysFlowDoingCount+"");
        typecountmap.put("flowView",sysFlowViewCount+"");
        typecountmap.put("flowAttention",sysFlowAttentionCount+"");
        typecountmap.put("flowCS",sysFlowCSCount+"");
        if(sysAllCount > 0)
            countmap.put(typecountmap.get("domid"), typecountmap);
        return base;
    }

    private Map<String,Object> generateOfsBase(String flag, User user,List<String> typeidList,String resourceid){
        Map<String,Map<String,String>> ofsCounts = new HashMap<String,Map<String,String>>();
        RecordSet rs = new RecordSet();
        boolean isMergeShow = false;	//是否主从账号统一显示
        String userID = user.getUID()+"";
        String currentUser = "".equals(resourceid) ? userID : resourceid;
        String userIDAll = "";
        if(currentUser.equals(userID)){	//只有看自己待办才做合并显示
            rs.executeQuery("select * from HrmUserSetting where resourceId=?", userID);
            if (rs.next() && "1".equals(rs.getString("belongtoshow")))
                isMergeShow = true;
            userIDAll = userID;
            String Belongtoids = user.getBelongtoids();
            if (!"".equals(Belongtoids))
                userIDAll = userID + "," + Belongtoids;
        }else{
            userIDAll = currentUser;
        }
        String user_sqlstr = isMergeShow ? userIDAll : currentUser;
        boolean superior = false; //是否为被查看者上级或者本身
        if (userID.equals(currentUser)) {
            superior = true;
        } else {
            rs.executeQuery("SELECT * FROM HrmResource WHERE ID = " + currentUser + " AND managerStr LIKE '%," + userID + ",%'");
            if (rs.next())
                superior = true;
        }

        String sql = "";
        // 处理 移动端 流程中心 -待办/已办/我的请求 条件
        String deftabsql_os = Util.null2String(params.get("deftabsql_os"));
        if(typeidList.isEmpty()){
            sql = this.generateSql(flag,deftabsql_os.replaceAll("ofs_todo.", ""),user_sqlstr,superior);
        }else{
            sql = this.generateSql(flag,typeidList,user,user_sqlstr,superior);
        }
        new BaseBean().writeLog("==zj==(异构计数sql)" + sql);
        int totalNewCount = 0;
        int totalAllCount = 0;  //全部的异构系统数据
        int totleFlowDoing = 0;//待处理的异构系统数据
        int totleFlowView = 0;//待阅的异构系统数据
        int totleFlowCS = 0;//待阅的异构系统数据
        int totleAttentionCount = 0;//我的关注
        StringBuilder wfidRange = new StringBuilder();
        rs.executeQuery(sql);
        while(rs.next()){
            String ofsWfid = Util.null2String(rs.getString("workflowid"));
            int ofsViewType = Util.getIntValue(rs.getString("viewtype"), 0);
            int ofsReqCount = Util.getIntValue(rs.getString("count"), 0);
            int ofsAttentionCount = Util.getIntValue(rs.getString("count1"), 0);
            int isremark = Util.getIntValue(rs.getString("isremark"));
            wfidRange.append(",").append(ofsWfid);

            //统计 我的关注 数量
            if("attention".equals(flag))ofsReqCount = ofsAttentionCount;

            Map<String,String> ofsBean = null;
            if(ofsCounts.containsKey(ofsWfid)){
                ofsBean = ofsCounts.get(ofsWfid);
            }else{
                ofsBean = new HashMap<String,String>();
                ofsBean.put("wfid", ofsWfid);
                ofsCounts.put(ofsWfid, ofsBean);
            }
            Map<String,String> ofsBeanTemp = ofsCounts.get(ofsWfid);
            //未读数
            if(ofsViewType == 0){
                if(ofsBeanTemp != null){
                    ofsBean.put("newcount", (ofsReqCount+(Util.getIntValue(ofsBeanTemp.get("newcount"),0)))+"");
                }else {
                    ofsBean.put("newcount", ofsReqCount + "");
                }
                totalNewCount += ofsReqCount;
            }
            //待处理计数
            if(isremark == 0){
                if(ofsBeanTemp != null){
                    ofsBean.put("flowdoingcount", (ofsReqCount+(Util.getIntValue(ofsBeanTemp.get("flowdoingcount"),0)))+"");
                }else {
                    ofsBean.put("flowdoingcount", ofsReqCount + "");
                }
                totleFlowDoing += ofsReqCount;
            }
            //待阅计数
            if(isremark == 8 || isremark == 9){
                if(ofsBeanTemp != null){
                    ofsBean.put("flowviewcount", (ofsReqCount+(Util.getIntValue(ofsBeanTemp.get("flowviewcount"),0)))+"");
                    ofsBean.put("flowCScount", (ofsReqCount+(Util.getIntValue(ofsBeanTemp.get("flowCScount"),0)))+"");
                }else {
                    ofsBean.put("flowviewcount", ofsReqCount + "");
                    ofsBean.put("flowCScount", ofsReqCount + "");
                }
                totleFlowView += ofsReqCount;
                totleFlowCS += ofsReqCount;
            }

            //我的关注数
            int _totleAttentionCount = Util.getIntValue(ofsBean.get("flowAttentionCount"), 0);
            ofsBean.put("flowAttentionCount", (_totleAttentionCount+ofsAttentionCount)+"");
            totleAttentionCount += ofsAttentionCount;

            //全部数
            int _allcount = Util.getIntValue(ofsBean.get("allcount"), 0);
            ofsBean.put("allcount", (_allcount+ofsReqCount)+"");
            totalAllCount += ofsReqCount;
        }
        if(totalAllCount == 0)
            return null;
        String wfidRangeStr = wfidRange.toString();
        if(wfidRangeStr.startsWith(","))
            wfidRangeStr = wfidRangeStr.substring(1);
        Map<String,Object> base = new HashMap<String,Object>();
        base.put("ofsCounts", ofsCounts);
        base.put("totalAllCount", totalAllCount);
        base.put("totalNewCount", totalNewCount);
        base.put("totleFlowDoing", totleFlowDoing);
        base.put("totleFlowView", totleFlowView);
        base.put("totleFlowCS", totleFlowCS);
        base.put("wfidRangeStr", wfidRangeStr);
        base.put("totleAttentionCount", totleAttentionCount);
        return base;
    }

    private String generateSql(String flag){ return this.generateSql(flag,""); }
    private String generateSql(String flag,String deftabsqlOs){ return this.generateSql(flag,deftabsqlOs,"",true); }

    /**
     * 获取左侧树sql
     * @param flag scope
     * @param deftabsqlOs 移动端流程中心  待办/已办/我的请求 异构系统条件
     * @param userIDAll 用户id串，resourceid或者当前登录用户id（根据设置区分是否含次账号id）
     * @param superior 是否是查看自己或直属下级人员数据
     * @return
     */
    private String generateSql(String flag,String deftabsqlOs,String userIDAll,boolean superior){
        String sql = "";
        String sqlDone = "";//集成分表--统一已办表
        String useridCondition = "".equals(userIDAll) ? "userid=?" : "userid in("+userIDAll+")";
        String createrCondition = "".equals(userIDAll) ? "creatorid=?" : "creatorid in("+userIDAll+")";
        String superiorCondition = superior ? "" : " and exists (select 1 from ofs_todo_data otd where otd.requestid=o.requestid and otd.workflowid=o.workflowid and otd.userid =" + user.getUID() + ")";//是否是查看非直接下属人员待办或自己查看自己待办
        if("doing".equals(flag) || "attention".equals(flag)){
            //增加 待办-我的关注 计数

            sql = ConnectionPool.getInstance().isNewDB()?
                    "select o.workflowid,o.viewtype,o.isremark,COUNT(o.requestid) count,sum(case when att.requestid<0 then 1 else 0 end) count1 " +
                            " from ofs_todo_data o left join (select * from workflow_attention wa where id in (select max(id) from workflow_attention att2 where wa.userid=att2.userid group by requestid)) att " +
                            " on att.requestid=o.requestid and att.userid="+user.getUID() +
                            " where o."+useridCondition+" and o.isremark in(0,8,9) and o.islasttimes=1 " + getExtendsSql()
                    :
                    "select o.workflowid,o.viewtype,o.isremark,COUNT(o.requestid) count,sum(case when att.requestid<0 then 1 else 0 end) count1 " +
                            " from ofs_todo_data o left join workflow_attention att on att.requestid=o.requestid and att.userid="+user.getUID()+" and att.id in (select max(id) from workflow_attention att2 where att.userid=att2.userid group by requestid) " +
                            " where o."+useridCondition+" and o.isremark in(0,8,9) and o.islasttimes=1 " + getExtendsSql() ;
            sql += getLimitSql() + superiorCondition;
            if(!"".equals(deftabsqlOs)){		// 移动端 流程中心 待办tab条件
                sql +=  deftabsqlOs;
            }
            // appurl/pcurl条件
            int showType = "1".equals(params.get("ismobile")) ? 1 : 0;
            sql += getOfsUrlWhere(showType,"o.");
            sql += " group by o.workflowid,o.viewtype,o.isremark";
        }else if("done".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_done_data where "+useridCondition+" and isremark in (2,4) and islasttimes=1 ";
            if(!"".equals(deftabsqlOs)){		// 移动端 流程中心 已办tab条件
                sql +=  deftabsqlOs;
            }
        }else if("mine".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+createrCondition+" and creatorid=userid and islasttimes=1 ";
            sqlDone = "select workflowid,viewtype,isremark,COUNT(distinct requestid) count from ofs_done_data where "+createrCondition+" and creatorid=userid and islasttimes=1 ";
            if(!"".equals(deftabsqlOs)){		// 移动端 流程中心 我的请求tab条件
                sql +=  deftabsqlOs;
                sqlDone +=  deftabsqlOs.replaceAll("ofs_todo_data","ofs_done_data");
            }
        } else if ("all".equals(flag)) {//可能已经废弃了，没找到还有哪里调用
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where ("+useridCondition+" or "+createrCondition+")  and islasttimes=1 ";
            sqlDone = "select workflowid,viewtype,isremark,COUNT(distinct requestid) count from ofs_done_data where ("+useridCondition+" or "+createrCondition+")  and islasttimes=1 ";
        }else if("flowNew".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+useridCondition+" and isremark in(0,8,9) and viewtype=0 and islasttimes=1 ";
        }else if("flowUnFinish".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_done_data where "+useridCondition+" and islasttimes=1 and isremark='2' and iscomplete=0 ";
        }else if("flowFinish".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_done_data where "+useridCondition+" and islasttimes=1 and isremark in ('2','4') and iscomplete=1 ";
        }else if("mineFlowUnFinish".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+createrCondition+" and creatorid=userid and islasttimes=1 and iscomplete=0 ";
            sqlDone = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_done_data where "+createrCondition+" and creatorid=userid and islasttimes=1 and iscomplete=0 ";
        }else if("mineFlowFinish".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+createrCondition+" and creatorid=userid and islasttimes=1 and iscomplete=1 ";
            sqlDone = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_done_data where "+createrCondition+" and creatorid=userid and islasttimes=1 and iscomplete=1 ";
        }else if("flowDoing".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+useridCondition+" and isremark = 0 and islasttimes=1 ";
        }else if("flowView".equals(flag)){
            sql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+useridCondition+" and isremark in(8,9) and islasttimes=1 ";
        }
        if("doing".equals(flag) || "attention".equals(flag)){
            return sql;
        }
        // appurl / pcurl 条件
        int showType = "1".equals(params.get("ismobile")) ? 1 : 0;
        sql += getOfsUrlWhere(showType,"");
        // 处理后台 移动端限制流程路径
        String limitSql = getLimitSql();
        String extendsSql = getExtendsSql().replaceAll("o\\.","");
        sql += limitSql + extendsSql + superiorCondition.replaceAll("o\\.","") + " group by workflowid,viewtype,isremark";
        if(!"".equals(sqlDone)){//拼接已办表数据集合
            // 处理 appurl / pcurl条件
            sqlDone += getOfsUrlWhere(showType,"");
            sqlDone += limitSql + extendsSql + superiorCondition.replaceAll("ofs_todo_data","ofs_done_data").replaceAll("o\\.","") + " group by workflowid,viewtype,isremark";
            sql += " union " + sqlDone;
        }
        return sql;
    }

    /**
     * 处理移动端  设置路径限制
     * @return sqlWhere
     */
    private String getLimitSql(){
        String menuid =  Util.null2String(params.get("menuid"));
        String ismobile = Util.null2String(params.get("ismobile"));
        String whereOfs = "";
        if( "1".equals(ismobile) && !"".equals(menuid)){  // 移动端 添加 左侧树限制 条件
            Map<String,String> mobileRange = new MobileDimensionsBiz().getMobileRangeSql(Util.getIntValue(menuid));
            whereOfs = mobileRange.get("whereclause_os").replaceAll("ofs.","");
			/*MobileDimensionsBiz mobileDimensionsBiz = new MobileDimensionsBiz();
			String sourcetype = Util.null2String(mobileDimensionsBiz.getSourcetype(Integer.parseInt(menuid)));
			if(!"".equals(sourcetype)){  // sourcetype == ""全部
				String getworkflowidsSql = " select workflowid from workflow_mobileconfigdetail where menuid = ? ";
				StringBuilder stringBuilder = new StringBuilder();
				String workflowids = "";
				RecordSet rs = new RecordSet();
				rs.executeQuery(getworkflowidsSql,menuid);
				while(rs.next()){
					stringBuilder.append(rs.getString("workflowid")).append(",");
				}
				if(stringBuilder.length() > 0){
					workflowids = stringBuilder.substring(0,stringBuilder.length()-1);
				}
				 whereOfs= " and workflowid " + sourcetype + " ( " + workflowids + " )";
			}*/
        }
        return whereOfs;
    }

    private String generateSql(String flag,List<String> typeList,User user){ return this.generateSql(flag, typeList, user,"",true); }

    /**
     * 根据指定tabid，获取所有tab下异构系统数据集合查询sql
     * @param flag 维度标识
     * @param typeList 指定的未读id数组
     * @param user 当前登录用户
     * @param userIDAll 用户id串，resourceid或者当前登录用户id（根据设置区分是否含次账号id）
     * @param superior 是否是查看自己或直属下级人员数据
     * @return
     */
    private String generateSql(String flag,List<String> typeList,User user,String userIDAll,boolean superior){
        String sql = "";
        String sqlDone = "";//集成分表--统一已办表
        String extendsSql = getExtendsSql().replaceAll("o\\.","");
        String useridCondition = "".equals(userIDAll) ? "userid=?" : "userid in("+userIDAll+")";
        String createrCondition = "".equals(userIDAll) ? "creatorid=?" : "creatorid in("+userIDAll+")";
        String superiorCondition = superior ? "" : " and exists (select 1 from ofs_todo_data otd where otd.requestid=o.requestid and otd.workflowid=o.workflowid and otd.userid =" + user.getUID() + ")";//是否是查看非直接下属人员待办或自己查看自己待办
        //增加 待办-我的关注 计数
        String aliasName = "";
        String doingBaseSql = ConnectionPool.getInstance().isNewDB()?
                "select o.workflowid,o.viewtype,o.isremark,COUNT(o.requestid) count,sum(case when att.requestid<0 then 1 else 0 end) count1 " +
                        " from ofs_todo_data o left join (select * from workflow_attention wa where id in (select max(id) from workflow_attention att2 where wa.userid=att2.userid group by requestid)) att " +
                        " on att.requestid=o.requestid and att.userid="+user.getUID() +
                        " where o."+useridCondition+"  and o.islasttimes=1 " + extendsSql + superiorCondition
                :
                "select o.workflowid,o.viewtype,o.isremark,COUNT(o.requestid) count,sum(case when att.requestid<0 then 1 else 0 end) count1 " +
                        " from ofs_todo_data o left join workflow_attention att on att.requestid=o.requestid and att.userid="+user.getUID()+" and att.id in (select max(id) from workflow_attention att2 where att.userid=att2.userid group by requestid) " +
                        " where o."+useridCondition+"  and o.islasttimes=1 " + extendsSql + superiorCondition;

        String doneBaseSql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_done_data where "+useridCondition+" and isremark in (2,4) and islasttimes=1 " + extendsSql + superiorCondition.replaceAll("ofs_todo_data","ofs_done_data").replaceAll("o\\.","");
        String mineBaseSql = "select workflowid,viewtype,isremark,COUNT(requestid) count from ofs_todo_data where "+createrCondition+" and creatorid=userid and islasttimes=1 " + extendsSql + superiorCondition.replaceAll("o\\.","");
        if("doing".equals(flag) || "flowNew".equals(flag) || "attention".equals(flag)){//待办输入tabkeys时，左侧树的计算方式
            sql = doingBaseSql;
            aliasName = "o.";
            if(!typeList.contains("0")){
                sql += " and (1=2 ";
                for(String typeid : typeList){
                    sql += WorkflowDimensionUtils.getOsSqlWhere(typeid,"or",user).replaceAll("ofs_todo_data.","o.");
                }
                sql += " ) ";
            }
        }else if("done".equals(flag) || "flowUnFinish".equals(flag) || "flowFinish".equals(flag)){//已办输入tabkeys时，左侧树的计算方式
            sql = doneBaseSql;
            aliasName = "ofs_done_data.";
            if(!typeList.contains("10")){
                sql += " and (1=2 ";
                for(String typeid : typeList){
                    sql += WorkflowDimensionUtils.getOsSqlWhere(typeid,"or",user).replaceAll("ofs_todo_data","ofs_done_data");
                }
                sql += " ) ";
            }
        }else if("mine".equals(flag) || "mineFlowUnFinish".equals(flag) || "mineFlowFinish".equals(flag)){//我的请求输入tabkeys时，左侧树的计算方式
            sql = mineBaseSql;
            aliasName = "ofs_todo_data.";
            sqlDone = mineBaseSql.replaceAll("ofs_todo_data","ofs_done_data");
            if(!typeList.contains("16")){
                sql += " and (1=2 ";
                sqlDone += " and (1=2 ";
                for(String typeid : typeList){
                    String osSqlWhere = WorkflowDimensionUtils.getOsSqlWhere(typeid,"or",user);
                    sql += osSqlWhere;
                    sqlDone += osSqlWhere.replaceAll("ofs_todo_data","ofs_done_data");
                }
                sql += " ) ";
                sqlDone += " ) ";
            }
        }
        if(!"".equals(sql)){
            String limitSql = getLimitSql();
            sql += limitSql;
            // appurl/pcurl条件
            int showType = "1".equals(params.get("ismobile")) ? 1 : 0;
            sql += getOfsUrlWhere(showType,aliasName);
            sql += getOfsConfigWhere(showType,aliasName);
            sql += " group by workflowid,viewtype,isremark ";
            if(!"".equals(sqlDone)){//拼接已办表数据集合
                sqlDone += limitSql;
                sqlDone += getOfsUrlWhere(showType,"ofs_done_data.");
                sqlDone += " group by workflowid,viewtype,isremark ";
                sql += " union " + sqlDone;
            }
        }
        return sql;
    }

    //获取扩展的sql条件
    private String getExtendsSql() {
        String extendsSql = "";
        String inornot = Util.null2String(params.get("inornot"));
        String wfids = WorkflowVersion.getAllVersionStringByWFIDs(Util.null2String(params.get("workflowid")));
        String wftypeids = Util.null2String(params.get("workflowtype"));
        if (!"".equals(wfids)) {
            //extendsSql = " and o.workflowid in (" + wfids + ") ";
            extendsSql += " and " + Util.getSubINClause(wfids,"o.workflowid",inornot);
        }
        if (!"".equals(wftypeids)) {
            //extendsSql = " and o.sysid in (" + wftypeids + ") ";
            extendsSql += " and " + Util.getSubINClause(wftypeids,"o.sysid",inornot);
        }

        String scope = Util.null2String(params.get("scope"));
        if("mine".equals(scope) && !showDone){
            extendsSql += " and o.isremark in(0,8,9)";
        }
        return extendsSql;
    }

    /**
     * 获取 排除掉集成ofs_sysinfo表 showapp，showpc 为0的数据条件
     * @param showtype  1 app  0 pc
     * @Param alias 别名 传入时末尾需要带.  如ofs_todo_data.
     * @return
     */
    public static String getOfsConfigWhere(int showtype, String alias){
        OfsTodoDataUtils ofsTodoDataUtils = new OfsTodoDataUtils();
        String ofsConfigSqlWhere = "";
        String unValidSysids = ofsTodoDataUtils.getNoShowOfsSysInfoIds(showtype);
        if(alias == null){
            alias = "";
        }
        if(!"".equals(unValidSysids)){
            ofsConfigSqlWhere = " and " + Util.getSubINClause(unValidSysids,alias+"sysid","not in") + " ";
        }
        return ofsConfigSqlWhere;
    }

    /**
     * 获取排除掉ofs_todo_data表 appurl/pcurl 为空的条件
     * @param showtype 1 app 0 pc
     * @Param alias  别名 传入时末尾需要带.  如ofs_todo_data.
     * @return
     */
    public static String getOfsUrlWhere(int showtype, String alias){
        String ofsUrlWhere = "";
        RecordSet rs = new RecordSet();
        if(alias == null){
            alias = "";
        }
        String urlField = 1 == showtype ? (alias + "appurl") : (alias + "pcurl");
        ofsUrlWhere = " and " + urlField + " is not null " + ("oracle".equals(rs.getDBType()) ? "" : " and " + urlField + " <> '' ");
        return ofsUrlWhere;
    }
}
