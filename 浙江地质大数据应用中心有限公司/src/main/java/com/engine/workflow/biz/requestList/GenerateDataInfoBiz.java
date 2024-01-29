package com.engine.workflow.biz.requestList;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.api.customization.zj0601.util.OpenofsUtil;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.workflow.biz.WorkflowCenterBiz;
import com.engine.workflow.biz.mobileCenter.MobileDimensionsBiz;
import com.engine.workflow.entity.RequestListDataInfoEntity;
import com.engine.workflow.entity.WorkflowDimensionEntity;
import com.engine.workflow.entity.mobileCenter.Dimensions;
import com.engine.workflow.util.CommonUtil;

import com.engine.workflow.util.WorkflowDimensionUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.request.todo.RequestUtil;
import weaver.workflow.search.WfAdvanceSearchUtil;
import weaver.workflow.workflow.WorkflowComInfo;
import weaver.workflow.workflow.WorkflowDoingDimension;
import weaver.workflow.workflow.WorkflowVersion;

/**
 * PC端、移动端共有，生成待办/已办/我的请求列表取数信息
 * @author liuzy 2018-08-10
 */
public class GenerateDataInfoBiz {

	public RequestListDataInfoEntity generateEntity(HttpServletRequest request, User user) throws Exception {
		return generateEntity(request, user, new HashMap<>());
	}

	public RequestListDataInfoEntity generateEntity(HttpServletRequest request, User user, Map<String, Object> otherParams) throws Exception{
		Map<String,String> reqparams = new HashMap<String,String>();
		RecordSet rs = new RecordSet();
		//request参数封装到map中
		Enumeration<?> em = request.getParameterNames();
		while(em.hasMoreElements()){
			String paramName = (String) em.nextElement();
			reqparams.put(paramName, Util.null2String(request.getParameter(paramName)));
		}

		/************ 用户信息计算 begin **********/
		String userID = String.valueOf(user.getUID());
		int usertype = "2".equals(user.getLogintype()) ? 1 : 0;
		String resourceid = Util.null2String(reqparams.get("resourceid"));	//resourceid为人力卡片查看他人待办列表，被查看人的用户ID
		String currentUser = "".equals(resourceid) ? user.getUID()+"" : resourceid;
		boolean isMergeShow = false;	//是否主从账号统一显示
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
		/************ 用户信息计算 end **********/

		WorkflowDoingDimension wdd = new WorkflowDoingDimension();
		RequestUtil requestutil = new RequestUtil();
		String whereclause = "";
		String whereclause_os = "";
		String orderclause = "";
		String orderclause_os = "";
		String scope = Util.null2String(reqparams.get("viewScope"));
		int viewcondition = Util.getIntValue(reqparams.get("viewcondition"), 0);

		//==zj 临时写法，后续需要优化
		if (viewcondition >= 80 ){
			scope = "doShow";
		}

		new BaseBean().writeLog("viewcondition:"+viewcondition);
		int viewcondition_tmep = viewcondition;

		if (!"doShow".equals(scope)){
			//不等于doShow,走默认
			viewcondition = Util.getIntValue(wdd.getViewcondition(viewcondition+""),0);
		}

		int viewtype = Util.getIntValue(reqparams.get("viewtype"), -1);//点击流程树上面的数字，查看类型，0：未读，1：反馈
		//查询条件之待办状态
		String doingStatus = Util.null2String(reqparams.get("doingStatus"));
		//输入url传递typeids参数
		String typeids = Util.null2String(reqparams.get("tabkeys"));
		List<String> typeidList = new ArrayList<>();
		typeidList = Util.TokenizerString(typeids,",");

		//临时typeidlist
		List<String> typeidList_temp = new ArrayList<>();
		typeidList_temp.addAll(typeidList);
		typeidList.clear();
		//排除下不存在的id
		if("" == wdd.getTypeid("1")){
			wdd.removeCache();
		}

			for(String typeid_temp : typeidList_temp){
				if(typeid_temp.equals(Util.null2String(wdd.getTypeid(typeid_temp))) && "1".equals(Util.null2String(wdd.getIsShow(typeid_temp))) && !typeidList.contains(typeid_temp) && scope.equals(wdd.getScope(typeid_temp))){
					typeidList.add(typeid_temp);
				}
			}


		if(viewcondition_tmep == 0 && "".equals(typeids) && !"1".equals(reqparams.get("ismobile"))){//走全部tab或者是列表的第一个tab
				typeidList = WorkflowDimensionUtils.getTypeidList(scope);
		}
		String ismobile = request.getParameter("ismobile");
		if("1".equals(ismobile)){//处理移动端应用限制数据范围
			int menuid = Util.getIntValue(request.getParameter("menuid"),-1);
			if(menuid != -1){
				Map<String,String> mobileRange = new MobileDimensionsBiz().getMobileRangeSql(menuid);
				String appType = mobileRange.get("appType");
				String mobileDimensionScope = Util.null2String(request.getParameter("mobileDimensionScope"));
				if("".equals(mobileDimensionScope) && (null != appType && !"".equals(appType) && !"10".equals(appType))){		// 默认流程中心删除后 可能会有appType为空情况
					scope = "doing";
				}else if(!"".equals(mobileDimensionScope)){
					//==zj 临时写法，后续需要优化
					if (viewcondition_tmep >= 80){
						scope = "doShow";
					}else {
						scope = mobileDimensionScope;
					}

				}

				whereclause += mobileRange.get("whereclause");
				whereclause_os += mobileRange.get("whereclause_os").replaceAll("ofs.","");
			}
		}

		boolean isopenofs = requestutil.getOfsSetting().getIsuse() == 1;
		boolean showdone = requestutil.getOfsSetting().getShowdone().equals("1");
		boolean isDoing = "doing".equals(scope);
		boolean isWfCenter = "wfcenter_todo".equals(Util.null2String(reqparams.get("source")));

		//拼接sql条件
		if(rs.getDBType().equalsIgnoreCase("postgresql"))
			whereclause += " and (t1.deleted<>1 or t1.deleted is null ) ";
		else
		    whereclause += " and (t1.deleted<>1 or t1.deleted is null or t1.deleted='') ";
		String wfRange = "";

		//--------------------流程中心--more页面条件拼接------------------------
		if(isWfCenter){
			WorkflowCenterBiz.resetParams(reqparams,"doing",user);
		}
		if(!reqparams.containsKey("inornot")){
			reqparams.put("inornot"," in ");
		}
		String inornot = Util.null2String(reqparams.get("inornot"));
		String workflowid = Util.null2String(reqparams.get("workflowid"));
		String workflowtype = Util.null2String(reqparams.get("workflowtype"));
		//--------------------流程中心--more页面条件拼接------------------------

		if (!workflowid.equals("")) {
			wfRange = WorkflowVersion.getAllVersionStringByWFIDs(workflowid);
			//whereclause += " and t1.workflowid in (" + wfRange + ") ";
			//whereclause_os += " and workflowid in(" + wfRange + ") ";
			whereclause += " and " + Util.getSubINClause(wfRange,"t1.workflowid",inornot);
			whereclause_os += " and " + Util.getSubINClause(wfRange,"workflowid",inornot);
		}
		if (!"".equals(workflowtype)) {
			//whereclause += " and t3.workflowtype in(" + workflowtype + ") ";
			//whereclause_os += " and workflowid in (select workflowid from ofs_workflow where sysid in(" + workflowtype + ") and (cancel=0 or cancel is null))";
			whereclause += " and " + Util.getSubINClause(workflowtype,"t3.workflowtype",inornot);
			whereclause_os += " and workflowid in (select workflowid from ofs_workflow where " + Util.getSubINClause(workflowtype,"sysid",inornot)+ " and (cancel=0 or cancel is null))";
		}

		String timeSql = new RequestListBiz().getTimeSql(rs);
		//新页面共用查询条件
		new BaseBean().writeLog("==zj==(新页面)" + typeidList + "  | " + scope + " | " + viewcondition + " | " + ismobile);
		if("doing".equals(scope) || "doShow".equals(scope)){
			String nodeids = Util.null2String(reqparams.get("nodeids"));
			if (!"".equals(nodeids)) {
				whereclause += " and t1.currentnodeid in ("+ WorkflowVersion.getAllRelationNodeStringByNodeIDs(nodeids) + ") ";
				whereclause_os += " and 1=2 ";
			}
			if (viewcondition == 3) {
				whereclause += " and (t2.isremark = '5' or (t2.isremark = '0' and (t2.takisremark is null or t2.takisremark=0 ) and isprocessed is not null ))  and (t1.currentnodetype <> '3' or (t2.isremark in ('1', '8', '9','11') and t1.currentnodetype = '3')) and t2.islasttimes=1";
			} else if (viewcondition == 1 || viewcondition == 2 || viewcondition == 4) {
				whereclause += " and ((t2.isremark='0' and (t2.takisremark is null or t2.takisremark=0 )) or t2.isremark in('1','5','8','9','7','11')) and t2.islasttimes=1";
				if(viewcondition == 1 || viewcondition == 2)	//未读、反馈排除超时数据
					whereclause += " and t2.isremark not in('5') and t2.isprocessed is null ";
			} else {
				//如果是doShowt语句特殊处理
				if ("doShow".equals(scope) && viewcondition != 83 && viewcondition != 87){
					if (viewcondition == 82){
						//如果是全部或者已归档就不需要添加条件
						whereclause += "and t2.islasttimes=1 and isprocessed is null";
					}else if(viewcondition == 85){
						whereclause +="AND (t2.isremark IN('2','4') OR (t2.isremark='0' AND t2.takisremark =-2)) AND t2.islasttimes=1";
					}else {
						whereclause += " and ((t2.isremark='0' and (t2.takisremark is null or t2.takisremark=0 )) or t2.isremark in('1','2','5','8','9','7','11')) and t2.islasttimes=1";
					}
				}else {
					whereclause += " and ((t2.isremark='0' and (t2.takisremark is null or t2.takisremark=0 )) or t2.isremark in('1','5','8','9','7','11')) and t2.islasttimes=1";
				}
			}
			if (viewcondition == 1)		//未读
				whereclause += " and t2.viewtype = '0' and (t1.currentnodetype <> '3' or (t2.isremark in ('1', '8', '9','11') and t1.currentnodetype = '3'))";
			else if (viewcondition == 2)	//反馈
				//whereclause += " and t2.viewtype = '-1'";
				whereclause += " and (t2.viewtype = '-1' or (t1.lastFeedBackOperator <> t2.userid and t2.needwfback = '1' and t2.viewtype = '-2' and t1.lastFeedBackDate is not null and t1.lastFeedBackTime is not null and (("+timeSql+") or (t2.viewDate is null and t2.viewTime is null)))) ";
			else if (viewcondition == 4)	//被督办
				whereclause += " and t2.requestid in (select requestid from workflow_requestlog where logtype='s')";
				//else if (viewcondition == 5 || viewcondition == 6 || viewcondition == 7){
			else if(viewcondition == 10 || viewcondition == 88) //我的关注
				whereclause += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=t2.requestid and att.userid="+user.getUID()+" AND att.usertype="+usertype+")";
			else if(viewcondition == 84) //未归档
				whereclause += " and(t1.currentnodetype <> '3')";
			else if ((viewcondition > 4)){//特殊处理抄送viewcondition=9
				whereclause += WorkflowDimensionUtils.getToDoSqlWhere(viewcondition_tmep+"","and",user);
			}
			//这里是给条件查询用的
			if ("5".equals(doingStatus) || "6".equals(doingStatus) || "7".equals(doingStatus)){
				whereclause += WorkflowDimensionUtils.getDoingStatusSqlWhere(doingStatus+"","and");
			}
			if(!typeidList.contains("0") && typeidList.size() > 0){
				String whereclause_tmp = "";
				String whereclause_os_tmp = "";
				for(String typeid : typeidList){
					whereclause_tmp += WorkflowDimensionUtils.getToDoSqlWhere(typeid,"or",user);
					whereclause_os_tmp += WorkflowDimensionUtils.getOsSqlWhere(typeid,"or",user);
				}
				if(!"".equals(whereclause_tmp)){
					whereclause += " and (1=2 " + whereclause_tmp + ") ";
				}
				whereclause_os += " and (1=2 ";
				if(!"".equals(whereclause_os_tmp)){
					whereclause_os +=  whereclause_os_tmp;
				}
				whereclause_os += ") ";
			}
			whereclause += " and (isprocessing = '' or isprocessing is null) ";
			new BaseBean().writeLog("==zj==(isopenofs)" + isopenofs);
			if (isopenofs) {
				if ("doShow".equals(scope)) {
					OpenofsUtil openofsUtil = new OpenofsUtil();
					whereclause_os += openofsUtil.getopenofsDoingSql( whereclause_os, user_sqlstr,  viewcondition,  doingStatus,  user, usertype);
					new BaseBean().writeLog("==zj==(异构系统条件拼接)" + whereclause_os);
				} else {
					whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark in(0,8,9) ";
					String customOsSql = WorkflowDimensionUtils.getOsSqlWhere(viewcondition_tmep+"","and",user);
					if (viewcondition == 0) {//全部
						//whereclause_os += " and userid=" + user.getUID() + " and islasttimes=1 and isremark='0' ";
					} else if (viewcondition == 1) {//未读
						whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark in(0,8,9) and viewtype=0 ";
					}else if(viewcondition == 10){ //我的关注
						whereclause_os += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=ofs_todo_data.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ") ";
					} else {
						if(!"".equals(customOsSql)){
							whereclause_os += customOsSql;
						}else{
							whereclause_os += " and 1=2 ";
						}
					}
					if("5".equals(doingStatus) || "6".equals(doingStatus) || "7".equals(doingStatus)){
						whereclause_os += " and 1=2 ";
					}
				}

			}
			orderclause = "sqlserver".equalsIgnoreCase(rs.getDBType()) ? "t2.receivedate+t2.receivetime " : "t2.receivedate,t2.receivetime ";
			orderclause_os = " receivedate,receivetime ";
		} else if("done".equals(scope)){
			whereclause += " and (t2.isremark in('2','4') or (t2.isremark='0' and t2.takisremark =-2)) and t2.islasttimes=1";
			if (viewcondition == 1)			//未归档
				whereclause += " and (t2.isremark ='2' or (t2.isremark='0' and t2.takisremark =-2)) and t1.currentnodetype <> '3' ";
			else if (viewcondition == 2)	//已归档
				whereclause += "  and t1.currentnodetype = 3 ";
			else if (viewcondition == 5)	//待回复
				whereclause += " and t2.isremark='0' and t2.takisremark =-2";
			else if (viewcondition == 4)	//未读
				whereclause += " and t2.viewtype=0 ";
			else if (viewcondition == 3)	//反馈
				//whereclause += " and t2.viewtype=-1 ";
				whereclause += " and (t2.viewtype = '-1' or (t1.lastFeedBackOperator <> t2.userid and t2.needwfback = '1' and t2.viewtype = '-2' and t1.lastFeedBackDate is not null and t1.lastFeedBackTime is not null and (("+timeSql+") or (t2.viewDate is null and t2.viewTime is null)))) ";
			else if(viewcondition == 6) { //我的关注
				whereclause += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=t2.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ")";
			}else if ((viewcondition > 5)){
				//已办自定义维度类型
				whereclause += WorkflowDimensionUtils.getToDoSqlWhere(viewcondition_tmep+"","and",user);
			}
			if (isopenofs && showdone) {
				whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark in ('2','4') ";
				String customOsSql = WorkflowDimensionUtils.getOsSqlWhere(viewcondition_tmep+"","and",user);
				if (viewcondition == 0) {//全部
					//whereclause_os += " and userid=" + user.getUID() + " and islasttimes=1 and isremark in ('2','4') ";
				}else if (viewcondition == 1) {//未归档
					whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark='2' and iscomplete=0 ";
				} else if (viewcondition == 2) {//已归档
					whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark in ('2','4') and iscomplete=1";
				}else if(viewcondition == 6) { //我的关注
					whereclause_os += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=ofs_todo_data.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ") ";
				} else {
					if(!"".equals(customOsSql)){
						whereclause_os += customOsSql;
					}else{
						whereclause_os += " and 1=2 ";
					}
				}
			} else {
				whereclause_os += " and 1=2 ";
			}
			//指定维度的数据
			if(!typeidList.contains("10") && typeidList.size() > 0){
				String whereclause_tmp = "";
				String whereclause_os_tmp = "";
				for(String typeid : typeidList){
					whereclause_tmp += WorkflowDimensionUtils.getToDoSqlWhere(typeid,"or",user);
					whereclause_os_tmp += WorkflowDimensionUtils.getOsSqlWhere(typeid,"or",user);
				}
				if(!"".equals(whereclause_tmp)){
					whereclause += " and (1=2 " + whereclause_tmp + ") ";
				}
				whereclause_os += " and (1=2 ";
				if(!"".equals(whereclause_os_tmp)){
					whereclause_os += whereclause_os_tmp;
				}
				whereclause_os += ") ";
			}
			if(!showdone){
				whereclause_os += " and 1=2 ";
			}
			orderclause = "t2.operatedate,t2.operatetime ";
			orderclause_os = " operatedate, operatetime ";
		}else if ("mine".equals(scope)) {		//我的请求-全部流程
			whereclause += " and t1.creater in (" + user_sqlstr + ") and t1.creatertype = " + usertype;
			whereclause += " and t1.creater = t2.userid ";
			whereclause_os += " and creatorid in(" + user_sqlstr + ")";
			whereclause_os += " and creatorid=userid and islasttimes=1 ";
			if (viewcondition == 1) { //未归档
				whereclause += " and t1.currentnodetype <> '3' and t2.islasttimes=1 ";
				//whereclause_os += " and iscomplete=0 ";
			} else if (viewcondition == 2) { //已归档
				whereclause += " and (t2.isremark in('1', '2','4','5','8','9','11') or (t2.isremark='0' and t2.takisremark =-2)) and t1.currentnodetype = '3' and t2.islasttimes=1";
				//whereclause_os += " and iscomplete=1 ";
			} else if (viewcondition == 0 || viewcondition == 4 || viewcondition == 3) { //全部、未读、反馈
				whereclause += " and ((t1.currentnodetype <> '3') or (t2.isremark in('1','2','4','5','8','9','11') and t1.currentnodetype = '3' )) and t2.islasttimes=1 ";
				if (viewcondition == 4) {        //未读
					whereclause += " and t2.viewtype=0 ";
					//whereclause_os += " and 1=2 ";
				} else if (viewcondition == 3) {    //反馈
					//whereclause += " and t2.viewtype=-1 ";
					//whereclause_os += " and 1=2 ";
					whereclause += " and (t2.viewtype = '-1' or (t1.lastFeedBackOperator <> t2.userid and t2.needwfback = '1' and t2.viewtype = '-2' and t1.lastFeedBackDate is not null and t1.lastFeedBackTime is not null and (("+timeSql+") or (t2.viewDate is null and t2.viewTime is null)))) ";
				} else if (viewcondition == 0) {

				}
			}else if(viewcondition == 5) {//我的关注
				whereclause += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=t2.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ") and t2.islasttimes=1";
			}else if ((viewcondition > 5)){
				//原则：先做出这个维度的全部数据  然后自定义维度是在这个数据上加条件，自定义维度暂不考虑异构数据
				whereclause += " and ((t1.currentnodetype <> '3') or (t2.isremark in('1','2','4','5','8','9','11') and t1.currentnodetype = '3' )) and t2.islasttimes=1 ";
				//已办自定义维度类型
				whereclause += WorkflowDimensionUtils.getToDoSqlWhere(viewcondition_tmep+"","and",user);
			}

			if(isopenofs){//异构系统条件拼接抽出来
				String customOsSql = WorkflowDimensionUtils.getOsSqlWhere(viewcondition_tmep+"","and",user);
				if(viewcondition == 0){

				}else if(viewcondition == 1){
					whereclause_os += " and iscomplete=0 ";
				}else if(viewcondition == 2){
					whereclause_os += " and iscomplete=1 ";
				}else if(viewcondition == 5) {//我的关注
					whereclause_os += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=ofs_todo_data.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ") ";
				}else {
					if(!"".equals(customOsSql)){
						whereclause_os += customOsSql;
					}else{
						whereclause_os += " and 1=2 ";
					}
				}
			}

			//指定维度的数据
			if(!typeidList.contains("16") && typeidList.size() > 0){
				String whereclause_tmp = "";
				String whereclause_os_tmp = "";
				for(String typeid : typeidList){
					whereclause_tmp += WorkflowDimensionUtils.getToDoSqlWhere(typeid,"or",user);
					whereclause_os_tmp += WorkflowDimensionUtils.getOsSqlWhere(typeid,"or",user);
				}
				if(!"".equals(whereclause_tmp)){
					whereclause += " and (1=2 " + whereclause_tmp + ") ";
				}
				whereclause_os += " and (1=2 ";
				if(!"".equals(whereclause_os_tmp)){
					whereclause_os += whereclause_os_tmp;
				}
				whereclause_os += ") ";
			}
			if(!showdone){
				whereclause_os += " and isremark in(0,8,9) ";
			}
			orderclause = "sqlserver".equalsIgnoreCase(rs.getDBType()) ? "t1.createdate+t1.createtime " : "t1.createdate,t1.createtime ";
			orderclause_os = "createdate,createtime";
		}//不包括共享
		else if("all".equals(scope)){
			whereclause += " and t2.islasttimes=1 ";
			whereclause_os += " and  (userid in(" + user_sqlstr + ") or creatorid in(" + user_sqlstr + "))";
		}
		else
			throw new Exception("unknown request method");

		if(viewtype == 0 && typeidList.size()>0){
			whereclause += " and t2.viewtype=0 ";
		}else if(viewtype == 1 && typeidList.size()>0){
			//whereclause += " and t2.viewtype=-1 ";
			whereclause += " and t2.needwfback=1 and (t2.viewtype = '-1' or (t1.lastFeedBackOperator <> t2.userid and t2.viewtype = '-2' and t1.lastFeedBackDate is not null and t1.lastFeedBackTime is not null and (("+timeSql+") or (t2.viewDate is null and t2.viewTime is null)))) ";
		}
		int date2during = Util.getIntValue(Util.null2String(reqparams.get("date2during")), 0);
		if ("".equals(orderclause))
			orderclause = "t2.receivedate ,t2.receivetime";
		if ("".equals(orderclause_os))
			orderclause_os = "receivedate ,receivetime";

		whereclause += RequestListBiz.buildWfRangeWhereClause(request, "t1.workflowid");
		WorkflowComInfo WorkflowComInfo = new WorkflowComInfo();
		if (date2during > 0 && date2during < 37) {
			whereclause += WorkflowComInfo.getDateDuringSql(date2during);
			whereclause_os += WorkflowComInfo.getDateDuringSql(date2during);
		}
		/************高级搜索条件过滤 begin ********/
		String wfstatu = Util.null2String(reqparams.get("wfstatu"));
		String nodetype = Util.null2String(reqparams.get("nodetype"));
		String requestlevel = Util.null2String(reqparams.get("requestlevel"));
		String secLevel = Util.null2String(reqparams.get("secLevel"));
		WfAdvanceSearchUtil conditionutil = new WfAdvanceSearchUtil(request, rs);
		if (!nodetype.equals("")) {
			whereclause += " and t1.currentnodetype='" + nodetype + "'";
			whereclause_os += " and 1=2 ";
		}
		if (!requestlevel.equals("")) {
			whereclause += " and t1.requestlevel=" + requestlevel;
			whereclause_os += " and requestlevel= " + requestlevel;
		}
		/** 涉密系统条件过滤 start**/
		if (!secLevel.equals("")) {
			whereclause += " and t1.seclevel in (" + secLevel + ") ";
			whereclause_os += " and "+new HrmClassifiedProtectionBiz().getDefaultResourceSecLevel()+" in (" + secLevel + ") ";
		}
		if (HrmClassifiedProtectionBiz.isOpenClassification()) {
			whereclause += " and t1.seclevel >= " + new HrmClassifiedProtectionBiz().getMaxResourceSecLevelById2(user.getUID()+"");
		}
		/** 涉密系统条件过滤 end**/

		//流程状态：0表示无效，1表示有效，2表示全部
		if ("0".equals(wfstatu)) {//无效
			whereclause += " and t3.isvalid='0' ";
			whereclause_os += " and 1=2 ";
		} else if("".equals(wfstatu) || "1".equals(wfstatu)){//有效
			whereclause += " and t3.isvalid in('1','3') ";
		} else if("2".equals(wfstatu)){//全部
			whereclause += " and t3.isvalid<>'2' ";
		}
		String conditions = "";
		String conditionsos = "";
		String osTableAlias = "osTableAlias";
		if (isDoing) {
			conditions = conditionutil.getAdVanceSearch4PendingCondition();
			conditionsos = conditionutil.getAdVanceSearch4PendingConditionOs();
		} else {
			conditions = conditionutil.getAdVanceSearch4OtherCondition();
			conditionsos = conditionutil.getAdVanceSearch4OtherConditionOs(osTableAlias);
		}
		/*默认搜索框如果有参数  则去掉对应的标题或编号的参数--start*/
		String sqlWhere = Util.null2String(reqparams.get("sqlwhere"));
		if(!"".equals(sqlWhere)){
			whereclause += " "+sqlWhere+" ";
		}
		String sqlWhere_os = Util.null2String(reqparams.get("sqlwhere_os"));
		if(!"".equals(sqlWhere_os)){
			whereclause_os += " "+sqlWhere_os+" ";
		}
		/*默认搜索框如果有参数  则去掉对应的标题或编号的参数--start*/
		if (!"".equals(conditions))
			whereclause += conditions;
		if (!"".equals(conditionsos))
			whereclause_os += conditionsos;
		/************高级搜索条件过滤 end ********/

		String sqlwhere = "where t1.requestid = t2.requestid and t1.workflowid=t3.id and t2.userid in (" + user_sqlstr + " ) and t2.usertype=" + usertype;
		String sqlwhereos = " where 1=1 ";
		String nullKeyword= CommonUtil.getDBJudgeNullFun(rs.getDBType());
		sqlwhere += " and ("+nullKeyword+"(t1.currentstatus,-1) = -1 or ("+nullKeyword+"(t1.currentstatus,-1)=0 and t1.creater in ("+ user_sqlstr + "))) ";
		if (!superior) {
			sqlwhere += " and EXISTS (SELECT 1 FROM workFlow_CurrentOperator workFlowCurrentOperator WHERE t2.workflowid = workFlowCurrentOperator.workflowid AND t2.requestid = workFlowCurrentOperator.requestid AND workFlowCurrentOperator.userid in ("
					+ (isMergeShow ? userIDAll : user.getUID()) + " ) and workFlowCurrentOperator.usertype = " + usertype + ") ";
			sqlwhereos += " and exists (select 1 from ofs_todo_data otd where otd.requestid=ofs_todo_data.requestid and otd.workflowid=ofs_todo_data.workflowid and otd.userid in (" + (isMergeShow ? userIDAll : user.getUID()) + "))";
		}

		//流程连续处理的sqlwhere
		String continnuationProcessSqlWhere = Util.null2String(otherParams.get("continnuationProcessSqlWhere"));
		if(!"".equals(continnuationProcessSqlWhere)) {
			sqlwhere += " AND (( 1 = 1 " + whereclause + " ) OR ( " + continnuationProcessSqlWhere + ") ) ";
		} else {
			sqlwhere += " " + whereclause;
		}

		// 调用集成方法获取不需要展示的异构系统id
		int showType = "1".equals(ismobile) ? 1 : 0;
		whereclause_os += OfsRequestListBiz.getOfsConfigWhere(showType,"ofs_todo_data.");
		// ofs_todo_data表 appurl 和 pcurl 条件
		whereclause_os += OfsRequestListBiz.getOfsUrlWhere(showType,"ofs_todo_data.");

		sqlwhereos += " " + whereclause_os;

		boolean showBatchSubmit = false;		//是否显示批量提交
		if(isDoing && currentUser.equals(userID)){	//待办&&当前用户查看
			String batchSql = "select count(id) as count from workflow_base where multiSubmit=1";
			if(!"".equals(wfRange))
				batchSql += " and id in (" + wfRange + ")";
			rs.executeQuery(batchSql);
			if (rs.next() && rs.getInt("count") > 0)
				showBatchSubmit = true;
		}
		RequestListDataInfoEntity bean = new RequestListDataInfoEntity();
		bean.setReqparams(reqparams);
		bean.setMergeShow(isMergeShow);
		bean.setCurrentUser(currentUser);
		bean.setUserIDAll(userIDAll);
		bean.setWhereclause(sqlwhere);
		bean.setWhereclause_os(sqlwhereos.replaceAll(osTableAlias,""));
		//流程门户待办元素more页面处理自定义排序
		bean.setOrderclause(orderclause);
		bean.setOrderclause_os(orderclause_os);
		bean.setShowBatchSubmit(showBatchSubmit);
		bean.setWhereclause_osDone(sqlwhereos.replaceAll("ofs_todo_data","ofs_done_data").replaceAll(osTableAlias,""));//集成分表--统一已办表
		return bean;
	}

}
