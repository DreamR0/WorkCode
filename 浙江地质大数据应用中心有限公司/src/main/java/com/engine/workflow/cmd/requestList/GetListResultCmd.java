package com.engine.workflow.cmd.requestList;

import com.api.workflow.service.RequestAuthenticationService;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.ParamUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.core.interceptor.CommandContext;
import com.engine.encrypt.biz.WfEncryptBiz;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.workflow.biz.RequestQuickSearchBiz;
import com.engine.workflow.biz.WorkflowCenterBiz;
import com.engine.workflow.biz.freeNode.FreeNodeBiz;
import com.engine.workflow.biz.requestList.GenerateDataInfoBiz;
import com.engine.workflow.biz.requestList.RequestAttentionBiz;
import com.engine.workflow.biz.requestList.OfsRequestListBiz;
import com.engine.workflow.biz.requestList.RequestListBiz;
import com.engine.workflow.biz.workflowCore.RequestBaseBiz;
import com.engine.workflow.constant.PageUidConst;
import com.engine.workflow.constant.SecondAuthType;
import com.engine.workflow.entity.RequestListDataInfoEntity;
import com.engine.workflow.entity.core.RequestInfoEntity;
import com.engine.workflow.entity.requestList.ListInfoEntity;
import com.engine.workflow.service.RequestSecondAuthService;
import com.engine.workflow.service.impl.RequestSecondAuthServiceImpl;
import com.engine.workflow.util.OrderByListUtil;
import weaver.conn.RecordSet;
import weaver.fullsearch.util.SearchBrowserUtils;
import weaver.general.BaseBean;
import weaver.general.PageIdConst;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.request.WFForwardManager;
import weaver.workflow.request.todo.OfsSettingObject;
import weaver.workflow.request.todo.RequestUtil;
import weaver.workflow.workflow.WorkflowConfigComInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PC端-流程待办/已办/我的请求分页列表数据
 * @author liuzy 2018-08-10
 */
public class GetListResultCmd extends AbstractCommonCommand<Map<String,Object>>{

	private HttpServletRequest request;

	/**
	 * 列表上一些可以个性化的信息， 供个性化使用（后续可继续完善）
	 */
	private ListInfoEntity listInfoEntity;

	public GetListResultCmd(HttpServletRequest request, User user){
		this.request = request;
		this.user = user;
		this.listInfoEntity = new ListInfoEntity();
		this.params = ParamUtil.request2Map(request);
	}

	public GetListResultCmd(){

	}

	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> result = new HashMap<String,Object>();
		try {
			RequestListDataInfoEntity bean = new GenerateDataInfoBiz().generateEntity(request, user);
			result = this.getResult(bean);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}

	private Map<String,Object> getResult(RequestListDataInfoEntity bean) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		RecordSet RecordSet = new RecordSet();
		RequestUtil requestutil = new RequestUtil();
		OfsSettingObject ofso = requestutil.getOfsSetting();
		boolean isopenos = ofso.getIsuse() == 1 && new OfsRequestListBiz().supportOfs4OtherCall(user);// 是否开启异构系统待办
		boolean showdone = "1".equals(ofso.getShowdone());//异构系统是否显示已办数据
		int userid = user.getUID();
		// 处理查看人员卡片 start
		int resourceid = Util.getIntValue(Util.null2String(params.get("resourceid")), -1);
		boolean isNeedHideBtn = resourceid != -1 && resourceid != userid;		// 查看别人卡片需要隐藏checkbox，操作按钮等

		WorkflowConfigComInfo wfconfig = new WorkflowConfigComInfo();
		int usequicksearch = Util.getIntValue(wfconfig.getValue("use_quicksearch_wflist"));//流程入口，是否使用微搜
		String usequicksearchUser = Util.null2String(wfconfig.getValue("use_quicksearch_user"));//流程入口，使用微搜人员id
		String scope = Util.null2String(params.get("viewScope"));
		if(!"true".equals(Util.null2String(params.get("doNotUseQuickSearch"))) && ((usequicksearch == 1 && "mine".equals(scope)) || (usequicksearch == 2 && (("," + usequicksearchUser + ",").contains("," + user.getUID() + ",") || "mine".equals(scope)))) && this.supportQuickSerach(isopenos,scope)){//满足微搜条件调用微搜，等于1只支持我的请求走微搜，等于2则待办已办也支持
			params.put("forWfreqlist",usequicksearch == 2 && ("," + usequicksearchUser + ",").contains("," + user.getUID() + ",") ? -1 : 0);
			return new RequestQuickSearchBiz().getRequestList4WfList(params, user,false);
		}
		//流程名称反射方法(兼容E8)
		String workflownamereflectmethod = "weaver.workflow.workflow.WorkflowComInfo.getWorkflowname";
		if(isopenos)
			workflownamereflectmethod = "weaver.general.WorkFlowTransMethod.getWorkflowname";
		String requestnamereflectclass = "com.api.workflow.util.WorkFlowSPATransMethod";
		Map<String,String> reqparams = bean.getReqparams();
		boolean showBatchSubmit = (!isNeedHideBtn && bean.isShowBatchSubmit());
		boolean isMergeShow = bean.isMergeShow();
		String CurrentUser = bean.getCurrentUser();
		String userIDAll = bean.getUserIDAll();
		String orderby = bean.getOrderclause();
		String orderbyos = bean.getOrderclause_os();
		String sqlwhere = bean.getWhereclause();
		String sqlwhereos = bean.getWhereclause_os();
		String sqlwhereosDone = bean.getWhereclause_osDone();//集成分表--统一已办表

		int usertype = "2".equals(user.getLogintype()) ? 1 : 0;
		int sysId = Util.getIntValue(reqparams.get("sysId"), 0);
		boolean isDoing = "doing".equals(scope);
		reqparams.put("isMergeShow",isMergeShow && !userIDAll.equals(String.valueOf(user.getUID()))?"1":"0");//设置开启并且有次账号
		boolean isQueryByNewTable = RequestListBiz.isQueryByNewTable(user,reqparams);
		apidatas.put("isQueryByNewTable",isQueryByNewTable);

		// 处理已办排序 start
		String operateDateTimeFieldSql0 = "";
		String operateDateTimeFieldSql = "";
		String operateDateTimeFieldSqlOs = "";
		String tableOrderStr = isopenos ? orderbyos : orderby;
		if (tableOrderStr.toLowerCase().indexOf("operatedate") != -1) {
			operateDateTimeFieldSql0 = ",operatedate";
			operateDateTimeFieldSql = ", (case  WHEN t2.operatedate IS NULL  THEN t2.receivedate ELSE t2.operatedate END) operatedate ";
			operateDateTimeFieldSqlOs = ", (case  WHEN operatedate IS NULL  THEN receivedate ELSE operatedate END) operatedate ";
		}

		if (tableOrderStr.toLowerCase().indexOf("operatetime") != -1) {
			operateDateTimeFieldSql0 += ",operatetime";
			operateDateTimeFieldSql += ", (case  WHEN t2.operatetime IS NULL  THEN t2.receivetime ELSE t2.operatetime END) operatetime ";
			operateDateTimeFieldSqlOs += ", (case  WHEN operatetime IS NULL  THEN receivetime ELSE operatetime END) operatetime ";
		}
		// 处理已办排序 end
		// 最外层查询字段
		String backfields0 = " requestid,requestmark,createdate, createtime,creater, creatertype, workflowid, requestname, requestnamenew, " +
				"status,requestlevel,currentnodeid,viewtype,userid,receivedate,receivetime,isremark,nodeid,agentorbyagentid,agenttype,isprocessed "
				+ operateDateTimeFieldSql0 + ",systype,workflowtype,isbereject,takisremark,requestnamehtmlnew";
		// 原始查询字段
		String backfields = " t1.requestid,t1.requestmark,t1.createdate, t1.createtime,t1.creater, t1.creatertype, t1.workflowid, t1.requestname, t1.requestnamenew," +
				" t1.status,t1.requestlevel,t1.currentnodeid,t2.viewtype,t2.userid,t2.receivedate,t2.receivetime,t2.isremark,t2.nodeid,t2.agentorbyagentid,t2.agenttype,t2.isprocessed "
				+ operateDateTimeFieldSql + " ,t1.seclevel,'0' as systype,t2.workflowtype,t2.isbereject,t2.takisremark,t1.requestnamehtmlnew";
		// 异构系统查询字段
		String backfieldsOs = " requestid,'' as requestmark,createdate, createtime,creatorid as creater, 0 as creatertype, workflowid, requestname, requestname as requestnamenew, " +
				"'' as status,requestlevel,-1 as currentnodeid,viewtype,userid,receivedate,receivetime,isremark,0 as nodeid, -1 as agentorbyagentid,'0' as agenttype,'0' as isprocessed "
				+ operateDateTimeFieldSqlOs + " ,'"+new HrmClassifiedProtectionBiz().getDefaultResourceSecLevel()+"' as secLevel,'1' as systype, sysid as workflowtype,'' as isbereject,0 as takisremark,'' as requestnamehtmlnew";
		//反馈黄点提示字段
		backfields0 += ",viewDate,viewTime,lastFeedBackDate,lastFeedBackTime,needwfback,lastFeedBackOperator";
		backfields += ",t2.viewDate,t2.viewTime,t1.lastFeedBackDate,t1.lastFeedBackTime,t2.needwfback,t1.lastFeedBackOperator";
		backfieldsOs += ",'' as viewDate,'' as viewTime,'' as lastFeedBackDate,'' as lastFeedBackTime,'' as needwfback,0 as lastFeedBackOperator";
		//反馈黄点提示字段
		String fromSql = " from workflow_requestbase t1,workflow_currentoperator t2,workflow_base t3 ";
		if("done".equals(scope)){
			backfields0 += ",operatedateNew,operatetimeNew";
			backfields += ",t2.operatedate as operatedateNew,t2.operatetime as operatetimeNew";
			backfieldsOs += ",operatedate as operatedateNew,operatetime as operatetimeNew";
		}

		String para2 = "column:requestid+column:workflowid+column:viewtype+0+" + user.getLanguage()
				+ "+column:nodeid+column:isremark+" + user.getUID()
				+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+column:userid+0+column:creater+scope_"+scope+"_scope";
		String para4 = user.getLanguage() + "+" + user.getUID() + "+column:userid";

		para2 = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S+" + para2;
		String reqNameParams = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S"//反馈参数不动
				+ "+column:requestid+column:workflowid+column:viewtype+" + user.getLanguage()
				+ "+column:nodeid+column:isremark+" + user.getUID()
				+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+column:userid+column:creater+scope_"+scope+"_scope+column:isbereject+column:takisremark+column:requestnamehtmlnew";//流程标题解析新方法新参数
		String reqNameTransMethod = "com.api.workflow.util.WorkFlowSPATransMethod.commonReqNameTransMethod_AttentionTag";

		//处理紧急程度自定义排序问题
		String requestleve_str = "requestlevelorder";//统一使用这个紧急程度顺序排序，以前是紧急程度排序
		OrderByListUtil obu = new OrderByListUtil(this.user);
		String myorderby = "",colname="",isordertype="";
		if(isDoing) {
			myorderby = obu.getMyOrderByStr(this.user.getUID(), PageUidConst.WF_LIST_DOING);
			if("".equals(myorderby)) {//如果为空
				myorderby  += " receivedate desc, receivetime desc";
			}
		}
		if("wfcenter_todo".equals(params.get("source"))){//流程中心more页面不走默认排序设置
			myorderby = Util.null2String(WorkflowCenterBiz.getWfCenterSetting(userid+"",usertype+"","doing","").get("ordercolDoing"));
		}

		if(isQueryByNewTable){//走新表时，使用提前缓存的已办排序时间代替实时case when排序
			backfields0 += ",optorderdate,optordertime";
			backfields += ",t2.optorderdate,optordertime";
			backfieldsOs += ",operatedate as optorderdate,operatetime as optordertime";
			orderby = orderbyos.replaceAll("operatedate","optorderdate").replaceAll("operatetime","optordertime");
			orderbyos = orderbyos.replaceAll("operatedate","optorderdate").replaceAll("operatetime","optordertime");
			myorderby = orderbyos.replaceAll("operatedate","optorderdate").replaceAll("operatetime","optordertime");
		}

		String pageUid = "";
		String urlType = "";
		new BaseBean().writeLog("==zj==(getListResultCmd)" + scope);
		if (scope.equals("doing")) {
			urlType = "1";
			pageUid = PageUidConst.WF_LIST_DOING;
		} else if (scope.equals("done")) {
			urlType = "2";
			pageUid = PageUidConst.WF_LIST_DONE;
		} else if (scope.equals("complete")) {
			urlType = "3";
		} else if (scope.equals("mine")) {
			urlType = "4";
			pageUid = PageUidConst.WF_LIST_MINE;
		}else if (scope.equals("doShow")){
			urlType = "5";
			pageUid = PageUidConst.WF_LIST_DOING;
		}
		else {
			urlType = "0";
		}
		String pageId = PageIdConst.getWFPageId(urlType);
		String pageSize = PageIdConst.getPageSize(pageId, user.getUID());
		String operateString = "";
		String tableString = "";
		String temptableString = "";
		String temptablerowString = "";
		new BaseBean().writeLog("==zj==(isopenos)" + isopenos);
		if (isopenos) {
			para2 = "column:requestid+column:workflowid+column:viewtype+0+" + user.getLanguage()
					+ "+column:nodeid+column:isremark+" + user.getUID()
					+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+column:userid+0+column:creater+column:systype+column:workflowtype+scope_"+scope+"_scope";
			para2 = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S+" + para2;


			if("done".equals(scope)){//异构系统不显示已办可以直接不查已办表
				if(showdone){
					fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
							+ " union (select distinct " + backfieldsOs + " from ofs_done_data " + sqlwhereosDone + ") ) t1 ) t1 ";
				}else{
					fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere + " ) t1 ) t1 ";
				}
			}else if("mine".equals(scope) && showdone){//异构系统不显示已办时，我的请求sql和待办sql一致
				fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
						+ " union (select distinct " + backfieldsOs + " from ofs_todo_data " + sqlwhereos + ") union (select distinct " + backfieldsOs + " from ofs_done_data" + sqlwhereosDone + ") ) t1 ) t1 ";
			} else{
				fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
						+ " union (select distinct " + backfieldsOs + " from ofs_todo_data " + sqlwhereos + ") ) t1 ) t1 ";
			}

			//处理紧急程度自定义排序问题
			backfields0 =  obu.getOrderByFrom(this.user.getLanguage()) + backfields0;
			if(isDoing) {//需要特殊处理  待办
				orderbyos = myorderby;
			}
			temptableString = " <sql "+WfEncryptBiz.judgeListAutoDecrypt(pageUid)+" backfields=\"" + backfields0 + "\" sqlform=\"" + (isQueryByNewTable ? RequestListBiz.transNewTable(user,Util.toHtmlForSplitPage(fromSql)) : Util.toHtmlForSplitPage(fromSql))
					+ "\" sqlwhere=\"\"  sqlorderby=\"" + orderbyos
					+ "\"  sqlprimarykey=\"t1.requestid\" openprimarykeyorder=\"1\" sqlsortway=\"Desc\" sqlisdistinct=\"false\" />";
			String showname = ofso.getShowsysname();
			if (!showname.equals("0")) {
				temptablerowString = "<col width=\"8%\" text=\"" + SystemEnv.getHtmlLabelName(22677, user.getLanguage())
						+ "\" column=\"workflowtype\"  orderkey=\"workflowtype\" transmethod=\"weaver.workflow.request.todo.RequestUtil.getSysname\" otherpara=\""
						+ showname + "\" />";
			}
		} else {
			backfields =  obu.getOrderByFrom(this.user.getLanguage()) + backfields;
			if(orderby.toLowerCase().indexOf("operatedate") != -1 || orderby.toLowerCase().indexOf("operatetime") != -1){
				//已办含case when情况排序特殊处理
				if(isDoing) {//需要特殊处理  待办
					orderby = myorderby;
				}
				fromSql = " from (select " + backfields + " " + fromSql + "" + sqlwhere + ") t1 ";
				orderby = orderby.replace("t2.", "t1.");
				temptableString = " <sql "+WfEncryptBiz.judgeListAutoDecrypt(pageUid)+" backfields=\"" + backfields0 + "\" sqlform=\"" + (isQueryByNewTable ? RequestListBiz.transNewTable(user,Util.toHtmlForSplitPage(fromSql)) : Util.toHtmlForSplitPage(fromSql))
						+ "\" sqlwhere=\"\"  sqlorderby=\"" + orderby
						+ "\"  sqlprimarykey=\"t1.requestid\" openprimarykeyorder=\"1\" sqlsortway=\"Desc\" sqlisdistinct=\"false\" />";
			}else{
				//				if(myorderby.trim().length()>0) {//需要特殊处理
				if(isDoing) {//需要特殊处理  待办
					orderby = myorderby;
				}
				temptableString = " <sql "+WfEncryptBiz.judgeListAutoDecrypt(pageUid)+" backfields=\"" + backfields + "\" sqlform=\"" + (isQueryByNewTable ? RequestListBiz.transNewTable(user,Util.toHtmlForSplitPage(fromSql)) : Util.toHtmlForSplitPage(fromSql))
						+ "\" sqlwhere=\"" + Util.toHtmlForSplitPage(sqlwhere) + "\"  sqlorderby=\"" + orderby
						+ "\"  sqlprimarykey=\"t1.requestid\" openprimarykeyorder=\"1\" sqlsortway=\"Desc\" sqlisdistinct=\"false\" />";
			}
		}
//==zj para2添加下参数(pc端)
		int  viewcondition =Integer.parseInt(Util.null2String(params.get("viewcondition"))) ;
		new BaseBean().writeLog("==zj==(para2添加下参数)"+viewcondition);
		if (viewcondition >=82 && viewcondition <=88){
				para2 +="+"+viewcondition;
		}
			new BaseBean().writeLog("==zj==(pc端sql-from查询语句)" + fromSql);
			new BaseBean().writeLog("==zj==(pc端sql-where查询语句)" + sqlwhere);
			new BaseBean().writeLog("==zj==(搜索结果)" +temptableString);

		//System.err.println("select "+backfields+fromSql+sqlwhere+" order by "+orderby);
		//================sql串拼装结束，拼接分页组件tablestring=========

		boolean hasrequestname = true;
		boolean hascreater = false;
		boolean hascreatedate = false;
		boolean hasworkflowname = false;
		boolean hasrequestlevel = false;
		boolean hasreceivetime = false;
		boolean hasstatus = false;
		boolean hasreceivedpersons = true;
		boolean hascurrentnode = false;
		boolean hasrequestmark = false;
		if (scope.equals("doing")) {
			hascreater = true;
			hascreatedate = true;
		} else if (scope.equals("done") || scope.equals("complete")) {
			hasworkflowname = true;
			hascreater = true;
			hasreceivetime = true;
			hascurrentnode = true;
		} else if (scope.equals("mine")) {
			hasworkflowname = true;
			hascreatedate = true;
			hascurrentnode = true;
		} else {
			hasworkflowname = true;
			hascreater = true;
			hascreatedate = true;
			hascurrentnode = true;
		}

		if (!userIDAll.equals(String.valueOf(user.getUID()))) {
			new BaseBean().writeLog("==zj==(自定义页面1)");
			//请求名称列串统一
			String requestNameColumnStr = "<col width=\"19%\" display=\""+ hasrequestname+ "\" text=\""+ SystemEnv.getHtmlLabelName(388770, user.getLanguage())
					+ "\" column=\"requestname\" orderkey=\"t1.requestname\" target=\"_fullwindow\" transmethod=\""+requestnamereflectclass+".getWfNewLinkWithTitle_AttentionTag"+(isMergeShow?"2":"")
					+"\"  otherpara=\""+ para2+ "\" otherpara2=\"column:requestnamenew\"/>";
			String currentUserpara = "column:userid";
			String currentUserpara2 = "column:userid+column:nodeid+column:workflowid+column:agentorbyagentid+column:agenttype+"+user.getUID();
			String popedomOtherpara = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S+" + "column:viewtype+column:isremark+column:isprocessed+column:nodeid+column:workflowid+"+ scope+"+column:userid";
			String popedomUserpara = userid + "_" + usertype;
			String popedomLogpara = "column:nodeid";
			String checkBoxParam = "column:requestid+column:userid+column:workflowid+"+usertype;
			if(!isNeedHideBtn){
				operateString = "<operates>";
				operateString += " <popedom async=\"false\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultOperation\" otherpara=\""
						+ popedomOtherpara + "\" otherpara2=\"" + popedomUserpara + "\" ></popedom> ";


				//标记为已读
				operateString += "     <operate href=\"javascript:reqListUtil.doReadIt();\" otherpara=\"" + currentUserpara + "\" text=\"" + SystemEnv.getHtmlLabelName(25419, user.getLanguage()) + "\" index=\"0\"/>";
				//转发
				if (!user.getLogintype().equals("2")) {
					operateString += "     <operate href=\"javascript:reqListUtil.doForward();\"  otherpara=\"" + currentUserpara2 + "\" text=\"" + SystemEnv.getHtmlLabelName(6011, user.getLanguage()) + "\" index=\"1\"/>";
				}
				//督办
				operateString += "<operate href=\"javascript:reqListUtil.handle();\" text=\""+SystemEnv.getHtmlLabelName(21223, user.getLanguage())+"\"  index=\"6\" linkvaluecolumn=\"requestid\" linkkey=\"requestid\" otherpara=\"column:userid\" target=\"_self\"/>";
				//短信催办
				operateString += "<operate href=\"javascript:reqListUtil.smsHandle();\" text=\""+SystemEnv.getHtmlLabelName(386971, user.getLanguage())+"\"  index=\"7\" linkvaluecolumn=\"requestid\" linkkey=\"requestid\" otherpara=\"column:userid\" target=\"_self\"/>";
				//邮件催办
				operateString += "<operate href=\"javascript:reqListUtil.emailHandle();\" text=\""+SystemEnv.getHtmlLabelName(386972, user.getLanguage())+"\"  index=\"8\" linkvaluecolumn=\"requestid\" linkkey=\"requestid\" otherpara=\"column:userid\" target=\"_self\"/>";
				//打印
				operateString += "     <operate href=\"javascript:reqListUtil.doPrint();\" otherpara=\"" + currentUserpara + "\" text=\"" + SystemEnv.getHtmlLabelName(257, user.getLanguage()) + "\" index=\"2\"/>";
				//表单日志
				operateString += "     <operate href=\"javascript:reqListUtil.seeFormLog();\" text=\"" + SystemEnv.getHtmlLabelName(21625, user.getLanguage()) + "\" otherpara=\"" + popedomLogpara+ "\" index=\"5\"/>";
				//关注
				operateString += "     <operate href=\"javascript:reqListUtil.doAttention();\" otherpara=\"" + user.getUID() + "\" text=\"" + SystemEnv.getHtmlLabelName(504445, user.getLanguage()) + "\" index=\"9\"/>";
				operateString += "</operates>";
			}
			if (showBatchSubmit && isDoing) {
				new BaseBean().writeLog("==zj==(自定义页面2)");

				if (isMergeShow) {
					tableString = " <table instanceid=\"workflowRequestListTable\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
							+ "\"   tabletype=\"checkbox\" pagesize=\""+ pageSize+ "\" >";
					tableString += temptableString;
					tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";
					tableString += requestNameColumnStr;
				} else {
					tableString = " <table instanceid=\"workflowRequestListTable\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
							+ "\"   tabletype=\"checkbox\" pagesize=\""+ pageSize+ "\" >";
					tableString += temptableString;
					tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "<head>";
					tableString += requestNameColumnStr;
				}
				tableString += temptablerowString;
				tableString += "<col width=\"10%\" display=\""+ hasworkflowname + "\"  text=\""+ SystemEnv.getHtmlLabelName(125749, user.getLanguage())
						+ "\" column=\"workflowid\" orderkey=\"t1.workflowid\" transmethod=\""+workflownamereflectmethod+"\" />";
				tableString += "<col width=\"6%\" display=\""+ hascreater + "\"  text=\""+ SystemEnv.getHtmlLabelName(882, user.getLanguage())
						+ "\" column=\"creater\" orderkey=\"t1.creater\"  otherpara=\"column:creatertype\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultName\" />";

				tableString += " <col width=\"10%\" display=\""+ hascreatedate + "\" id=\"createdate\" text=\""+ SystemEnv.getHtmlLabelName(722, user.getLanguage())
						+ "\" column=\"createdate\" orderkey=\"t1.createdate,t1.createtime\" otherpara=\"column:createtime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += "<col width=\"8%\" display=\""+ hasrequestlevel + "\"  id=\"quick\" text=\""+ SystemEnv.getHtmlLabelName(15534, user.getLanguage())
						+ "\" column=\"requestlevel\"  orderkey=\""+requestleve_str+"\"  transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultUrgencyDegree\" otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"10%\" display=\""+ hasreceivetime + "\"  text=\""+ SystemEnv.getHtmlLabelName(17994, user.getLanguage())
						+ "\" column=\"receivedate\" orderkey=\"receivedate,receivetime\" otherpara=\"column:receivetime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += "<col width=\"8%\" display=\""+ hascurrentnode + "\"  id=\"hurry\" text=\""+ SystemEnv.getHtmlLabelName(18564, user.getLanguage())
						+ "\" column=\"currentnodeid\" otherpara=\"column:requestid\" orderkey=\"t1.currentnodeid\" transmethod=\"weaver.general.WorkFlowTransMethod.getCurrentNode\"/>";

				tableString += "<col width=\"8%\" display=\"" + hasstatus + "\"  text=\""+ SystemEnv.getHtmlLabelName(1335, user.getLanguage())
						+ "\" column=\"status\" orderkey=\"t1.status\" />";
				tableString += "<col width=\"10%\" display=\"" + hasreceivedpersons + "\"  text=\""+ SystemEnv.getHtmlLabelName(16354, user.getLanguage())
						+ "\" _key=\"unoperators\" column=\"requestid\" otherpara=\""
						+ (userid+"+"+user.getLanguage()+"+column:userid") + "\" transmethod=\"weaver.general.WorkFlowTransMethod.getUnoperatorNew\"/>";
				tableString += "<col width=\"6%\" display=\"false\"  text=\""+ SystemEnv.getHtmlLabelName(19363, user.getLanguage())
						+ "\" _key=\"subwflink\" column=\"requestid\" orderkey=\"t1.requestid\"  linkkey=\"requestid\" linkvaluecolumn=\"requestid\" target=\"_self\" transmethod=\"weaver.general.WorkFlowTransMethod.getSubWFLink\"  otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"15%\" display=\"" + hasrequestmark
						+ "\" orderkey=\"t1.requestmark\"  text=\"" + SystemEnv.getHtmlLabelName(19502, user.getLanguage())
						+ "\" column=\"requestmark\"/>";
			} else if ("mine".equals(scope)) {
				if (isMergeShow) {
					sqlwhere += " and t1.creater = t2.userid";
					tableString = " <table instanceid=\"workflowRequestListTable\" tabletype=\"none\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
							+ "\" cssHandler=\"com.weaver.cssRenderHandler.request.CheckboxColorRender\" pagesize=\""+ pageSize + "\" >";
					tableString += temptableString;
					tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";
					tableString += requestNameColumnStr;
				} else {
					tableString = " <table instanceid=\"workflowRequestListTable\" tabletype=\"none\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
							+ "\" cssHandler=\"com.weaver.cssRenderHandler.request.CheckboxColorRender\" pagesize=\""+ pageSize + "\" >";
					tableString += temptableString;
					tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";
					tableString += requestNameColumnStr;
				}
				tableString += temptablerowString;
				tableString += " <col width=\"10%\"  display=\""+ hasworkflowname+ "\"  text=\""+ SystemEnv.getHtmlLabelName(125749, user.getLanguage())
						+ "\" column=\"workflowid\" orderkey=\"t1.workflowid\" transmethod=\""+workflownamereflectmethod+"\" />";

				tableString += " <col width=\"6%\" display=\""+ hascreater+ "\"  text=\""+ SystemEnv.getHtmlLabelName(882, user.getLanguage())
						+ "\" column=\"creater\" orderkey=\"t1.creater\"  otherpara=\"column:creatertype\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultName\" />";
				tableString += "<col width=\"10%\" display=\""+ hascreatedate+ "\" text=\""+ SystemEnv.getHtmlLabelName(722, user.getLanguage())
						+ "\" column=\"createdate\" orderkey=\"t1.createdate,t1.createtime\" otherpara=\"column:createtime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += " <col width=\"8%\" display=\""+ hasrequestlevel+ "\" text=\""+ SystemEnv.getHtmlLabelName(15534, user.getLanguage())
						+ "\" column=\"requestlevel\"  orderkey=\""+requestleve_str+"\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultUrgencyDegree\" otherpara=\""+ user.getLanguage() + "\"/>";
				tableString += " <col width=\"10%\" display=\""+ hasreceivetime+ "\" text=\""+ SystemEnv.getHtmlLabelName(17994, user.getLanguage())
						+ "\" column=\"receivedate\" orderkey=\"receivedate,receivetime\" otherpara=\"column:receivetime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";

				tableString += " <col width=\"8%\" display=\""+ hascurrentnode+ "\"  text=\""+ SystemEnv.getHtmlLabelName(18564, user.getLanguage())
						+ "\" column=\"currentnodeid\" otherpara=\"column:requestid\" orderkey=\"t1.currentnodeid\" transmethod=\"weaver.general.WorkFlowTransMethod.getCurrentNode\"/>";
				tableString += " <col width=\"8%\" display=\"" + hasstatus + "\" text=\""+ SystemEnv.getHtmlLabelName(1335, user.getLanguage())
						+ "\" column=\"status\" orderkey=\"t1.status\" />";
				tableString += "<col width=\"10%\" display=\"" + hasreceivedpersons + "\"  text=\""+ SystemEnv.getHtmlLabelName(16354, user.getLanguage())
						+ "\" _key=\"unoperators\" column=\"requestid\" otherpara=\""
						+ (userid+"+"+user.getLanguage()+"+column:userid") + "\" transmethod=\"weaver.general.WorkFlowTransMethod.getUnoperatorNew\"/>";
				tableString += "<col width=\"6%\" display=\"false\"  text=\""+ SystemEnv.getHtmlLabelName(19363, user.getLanguage())
						+ "\" _key=\"subwflink\" column=\"requestid\" orderkey=\"t1.requestid\"  linkkey=\"requestid\" linkvaluecolumn=\"requestid\" target=\"_self\" transmethod=\"weaver.general.WorkFlowTransMethod.getSubWFLink\"  otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"15%\" display=\"" + hasrequestmark
						+ "\" orderkey=\"t1.requestmark\" text=\"" + SystemEnv.getHtmlLabelName(19502, user.getLanguage())
						+ "\" column=\"requestmark\"/>";
			} else {
				new BaseBean().writeLog("==zj==(自定义页面3)");
				if (isMergeShow) {
					tableString = " <table instanceid=\"workflowRequestListTable\" tabletype=\"none\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
							+ "\" cssHandler=\"com.weaver.cssRenderHandler.request.CheckboxColorRender\" pagesize=\""+ pageSize + "\" >";
					tableString += temptableString;
					tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "	<head>";
					tableString += requestNameColumnStr;
				} else {
					tableString = " <table instanceid=\"workflowRequestListTable\" tabletype=\"none\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
							+ "\" cssHandler=\"com.weaver.cssRenderHandler.request.CheckboxColorRender\" pagesize=\""+ pageSize + "\" >";
					tableString += temptableString;
					tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";
					tableString += requestNameColumnStr;
				}
				tableString += temptablerowString;
				tableString += " <col width=\"10%\"  display=\""+ hasworkflowname + "\"  text=\""+ SystemEnv.getHtmlLabelName(125749, user.getLanguage())
						+ "\" column=\"workflowid\" orderkey=\"t1.workflowid\" transmethod=\""+workflownamereflectmethod+"\" />";

				tableString += " <col width=\"6%\" display=\""+ hascreater + "\"  text=\""+ SystemEnv.getHtmlLabelName(882, user.getLanguage())
						+ "\" column=\"creater\" orderkey=\"t1.creater\"  otherpara=\"column:creatertype\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultName\" />";
				tableString += "<col display=\""+ hascreatedate + "\" width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(722, user.getLanguage())
						+ "\" column=\"createdate\" orderkey=\"t1.createdate,t1.createtime\" otherpara=\"column:createtime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += " <col width=\"8%\" display=\""+ hasrequestlevel + "\"   text=\""+ SystemEnv.getHtmlLabelName(15534, user.getLanguage())
						+ "\" column=\"requestlevel\"  orderkey=\""+requestleve_str+"\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultUrgencyDegree\" otherpara=\""
						+ user.getLanguage() + "\"/>";

				if("done".equals(scope)){
					tableString += "<col width=\"10%\" display=\""+ hasreceivetime + "\"  text=\""+ SystemEnv.getHtmlLabelName(15502, user.getLanguage())
							+ "\" column=\"operatedateNew\" orderkey=\"operatedate,operatetime\" otherpara=\"column:operatetimeNew\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				}else{
					tableString += "<col width=\"10%\" display=\""+ hasreceivetime + "\"  text=\""+ SystemEnv.getHtmlLabelName(17994, user.getLanguage())
							+ "\" column=\"receivedate\" orderkey=\"receivedate,receivetime\" otherpara=\"column:receivetime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				}
				tableString += " <col width=\"8%\" display=\""+ hascurrentnode+ "\"   text=\""+ SystemEnv.getHtmlLabelName(18564, user.getLanguage())
						+ "\" column=\"currentnodeid\" otherpara=\"column:requestid\" orderkey=\"t1.currentnodeid\" transmethod=\"weaver.general.WorkFlowTransMethod.getCurrentNode\"/>";

				tableString += " <col width=\"8%\" display=\"" + hasstatus + "\"    text=\""+ SystemEnv.getHtmlLabelName(1335, user.getLanguage())
						+ "\" column=\"status\" orderkey=\"t1.status\" />";
				tableString += " <col width=\"10%\" display=\"" + hasreceivedpersons + "\"   text=\""+ SystemEnv.getHtmlLabelName(16354, user.getLanguage())
						+ "\" _key=\"unoperators\" column=\"requestid\"  otherpara=\""
						+ (userid+"+"+user.getLanguage()+"+column:userid") + "\" transmethod=\"weaver.general.WorkFlowTransMethod.getUnoperatorNew\"/>";
				tableString += "<col width=\"6%\" display=\"false\"  text=\""+ SystemEnv.getHtmlLabelName(19363, user.getLanguage())
						+ "\" _key=\"subwflink\" column=\"requestid\" orderkey=\"t1.requestid\"  linkkey=\"requestid\" linkvaluecolumn=\"requestid\" target=\"_self\" transmethod=\"weaver.general.WorkFlowTransMethod.getSubWFLink\"  otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"15%\" display=\"" + hasrequestmark
						+ "\" orderkey=\"t1.requestmark\" text=\"" + SystemEnv.getHtmlLabelName(19502, user.getLanguage())
						+ "\" column=\"requestmark\"/>";
			}
		} else {
			new BaseBean().writeLog("==zj==(自定义页面4)");
			//请求名称列串统一
			String requestNameColumnStr = "";
			new BaseBean().writeLog("==zj==(isQueryByNewTable)"+isQueryByNewTable);
			if(isQueryByNewTable){
				requestNameColumnStr = "<col width=\"19%\" display=\""+ hasrequestname+ "\" text=\""+ SystemEnv.getHtmlLabelName(388770, user.getLanguage())
						+ "\" column=\"requestname\" orderkey=\"t1.requestname\" target=\"_fullwindow\" transmethod=\""+reqNameTransMethod+"\" "
						+ " otherpara=\""+ reqNameParams+ "\" otherpara2=\"column:requestnamenew\" />";
			}else{
				requestNameColumnStr = "<col width=\"19%\" display=\""+ hasrequestname+ "\" text=\""+ SystemEnv.getHtmlLabelName(388770, user.getLanguage())
						+ "\" column=\"requestname\" orderkey=\"t1.requestname\" target=\"_fullwindow\" transmethod=\""+requestnamereflectclass+".getWfNewLinkWithTitle_AttentionTag\" "
						+ " otherpara=\""+ para2+ "\" otherpara2=\"column:requestnamenew\" />";
			}
			String currentUserpara = "column:userid";
			String currentUserpara2 = "column:userid+column:nodeid+column:workflowid+column:agentorbyagentid+column:agenttype+"+user.getUID();
			String popedomOtherpara = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S+" + "column:viewtype+column:isremark+column:isprocessed+column:nodeid+column:workflowid+" + scope +"+column:userid";
			String popedomUserpara = userid + "_" + usertype;
			String popedomLogpara = "column:nodeid";
			String checkBoxParam = "column:requestid+column:userid+column:workflowid+"+usertype;
			if(!isNeedHideBtn){
				operateString = "<operates>";
				operateString += " <popedom async=\"false\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultOperation\" otherpara=\""
						+ popedomOtherpara + "\" otherpara2=\"" + popedomUserpara + "\" ></popedom> ";

				//标记为已读
				operateString += "     <operate href=\"javascript:reqListUtil.doReadIt();\"  otherpara=\"" + currentUserpara + "\" text=\"" + SystemEnv.getHtmlLabelName(25419, user.getLanguage()) + "\" index=\"0\"/>";
				//转发
				if (!user.getLogintype().equals("2")) {
					operateString += "     <operate href=\"javascript:reqListUtil.doForward();\" otherpara=\"" + currentUserpara2 + "\" text=\"" + SystemEnv.getHtmlLabelName(6011, user.getLanguage()) + "\" index=\"1\"/>";
				}
				//督办
				operateString += "<operate href=\"javascript:reqListUtil.handle();\" text=\""+SystemEnv.getHtmlLabelName(21223, user.getLanguage())+"\"  index=\"6\" linkvaluecolumn=\"requestid\" linkkey=\"requestid\" otherpara=\"column:userid\" target=\"_self\"/>";
				//短信催办
				operateString += "<operate href=\"javascript:reqListUtil.smsHandle();\" text=\""+SystemEnv.getHtmlLabelName(386971, user.getLanguage())+"\"  index=\"7\" linkvaluecolumn=\"requestid\" linkkey=\"requestid\" otherpara=\"column:userid\" target=\"_self\"/>";
				//邮件催办
				operateString += "<operate href=\"javascript:reqListUtil.emailHandle();\" text=\""+SystemEnv.getHtmlLabelName(386972, user.getLanguage())+"\"  index=\"8\" linkvaluecolumn=\"requestid\" linkkey=\"requestid\" otherpara=\"column:userid\" target=\"_self\"/>";
				//打印
				operateString += "     <operate href=\"javascript:reqListUtil.doPrint();\" otherpara=\"" + currentUserpara + "\" text=\"" + SystemEnv.getHtmlLabelName(257, user.getLanguage()) + "\" index=\"2\"/>";
				//表单日志
				operateString += "     <operate href=\"javascript:reqListUtil.seeFormLog();\" text=\"" + SystemEnv.getHtmlLabelName(21625, user.getLanguage()) + "\" otherpara=\"" + popedomLogpara+ "\" index=\"5\"/>";
				//关注
				operateString += "     <operate href=\"javascript:reqListUtil.doAttention();\" otherpara=\"" + user.getUID() + "\" text=\"" + SystemEnv.getHtmlLabelName(504445, user.getLanguage()) + "\" index=\"9\"/>";
				operateString += "</operates>";
			}

			if (showBatchSubmit && isDoing) {
				tableString = " <table instanceid=\"workflowRequestListTable\" pageId=\""+ pageId + "\" pageUid=\"" + pageUid
						+ "\"   tabletype=\"checkbox\" pagesize=\""+ pageSize+ "\" >";

				tableString += temptableString;
				tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";

				tableString += requestNameColumnStr;
				tableString += temptablerowString;
				tableString += "<col width=\"10%\" display=\""+ hasworkflowname + "\"  text=\""+ SystemEnv.getHtmlLabelName(125749, user.getLanguage())
						+ "\" column=\"workflowid\" orderkey=\"t1.workflowid\" transmethod=\""+workflownamereflectmethod+"\" />";
				tableString += "<col width=\"6%\" display=\""+ hascreater + "\"   text=\""+ SystemEnv.getHtmlLabelName(882, user.getLanguage())
						+ "\" column=\"creater\" orderkey=\"t1.creater\"  otherpara=\"column:creatertype\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultName\" />";

				tableString += " <col width=\"10%\" display=\""+ hascreatedate + "\" id=\"createdate\" text=\""+ SystemEnv.getHtmlLabelName(722, user.getLanguage())
						+ "\" column=\"createdate\" orderkey=\"t1.createdate,t1.createtime\" otherpara=\"column:createtime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += "<col width=\"8%\" display=\""+ hasrequestlevel + "\"  id=\"quick\" text=\""+ SystemEnv.getHtmlLabelName(15534, user.getLanguage())
						+ "\" column=\"requestlevel\"  orderkey=\""+requestleve_str+"\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultUrgencyDegree\" otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"10%\" display=\""+ hasreceivetime + "\"  text=\""+ SystemEnv.getHtmlLabelName(17994, user.getLanguage())
						+ "\" column=\"receivedate\" orderkey=\"receivedate,receivetime\" otherpara=\"column:receivetime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += "<col width=\"8%\" display=\"" + hascurrentnode + "\"  id=\"hurry\" text=\"" + SystemEnv.getHtmlLabelName(18564, user.getLanguage())
						+ "\" column=\"currentnodeid\" otherpara=\"column:requestid\" orderkey=\"t1.currentnodeid\" transmethod=\"weaver.general.WorkFlowTransMethod.getCurrentNode\"/>";

				tableString += "<col width=\"8%\" display=\"" + hasstatus + "\"  text=\"" + SystemEnv.getHtmlLabelName(1335, user.getLanguage())
						+ "\" column=\"status\" orderkey=\"t1.status\" />";
				tableString += "<col width=\"10%\" display=\"" + hasreceivedpersons + "\"  text=\""+ SystemEnv.getHtmlLabelName(16354, user.getLanguage())
						+ "\" _key=\"unoperators\" column=\"requestid\" otherpara=\""
						+ (userid+"+"+user.getLanguage()+"+column:userid") + "\" transmethod=\"weaver.general.WorkFlowTransMethod.getUnoperatorNew\"/>";
				tableString += "<col width=\"6%\" display=\"false\"  text=\""+ SystemEnv.getHtmlLabelName(19363, user.getLanguage())
						+ "\" _key=\"subwflink\" column=\"requestid\" orderkey=\"t1.requestid\"  linkkey=\"requestid\" linkvaluecolumn=\"requestid\" target=\"_self\" transmethod=\"weaver.general.WorkFlowTransMethod.getSubWFLink\"  otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"15%\" display=\"" + hasrequestmark
						+ "\" orderkey=\"t1.requestmark\"  text=\"" + SystemEnv.getHtmlLabelName(19502, user.getLanguage())
						+ "\" column=\"requestmark\"/>";
			} else if ("mine".equals(scope)) {
				tableString = " <table instanceid=\"workflowRequestListTable\" tabletype=\"none\" pageId=\"" + pageId + "\" pageUid=\"" + pageUid
						+ "\" cssHandler=\"com.weaver.cssRenderHandler.request.CheckboxColorRender\" pagesize=\""+ pageSize + "\" >";
				tableString += temptableString;
				tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";

				tableString += temptablerowString;
				tableString += requestNameColumnStr;
				tableString += " <col width=\"10%\"  display=\""+ hasworkflowname + "\"  text=\""+ SystemEnv.getHtmlLabelName(125749, user.getLanguage())
						+ "\" column=\"workflowid\" orderkey=\"t1.workflowid\" transmethod=\""+workflownamereflectmethod+"\" />";

				tableString += " <col width=\"6%\" display=\""+ hascreater +"\" text=\""+ SystemEnv.getHtmlLabelName(882, user.getLanguage())
						+ "\" column=\"creater\" orderkey=\"t1.creater\"  otherpara=\"column:creatertype\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultName\" />";
				tableString += "<col width=\"10%\" display=\""+ hascreatedate +"\" text=\""+ SystemEnv.getHtmlLabelName(722, user.getLanguage())
						+ "\" column=\"createdate\" orderkey=\"t1.createdate,t1.createtime\" otherpara=\"column:createtime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += " <col width=\"8%\" display=\""+ hasrequestlevel +"\" text=\""+ SystemEnv.getHtmlLabelName(15534, user.getLanguage())
						+ "\" column=\"requestlevel\"  orderkey=\""+requestleve_str+"\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultUrgencyDegree\" otherpara=\""
						+ user.getLanguage() + "\"/>";

				tableString += " <col width=\"10%\" display=\""+ hasreceivetime + "\" text=\""+ SystemEnv.getHtmlLabelName(17994, user.getLanguage())
						+ "\" column=\"receivedate\" orderkey=\"receivedate,receivetime\" otherpara=\"column:receivetime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += " <col width=\"8%\" display=\""+ hascurrentnode + "\" text=\""+ SystemEnv.getHtmlLabelName(18564, user.getLanguage())
						+ "\" column=\"currentnodeid\" otherpara=\"column:requestid\" orderkey=\"t1.currentnodeid\" transmethod=\"weaver.general.WorkFlowTransMethod.getCurrentNode\"/>";

				tableString += " <col width=\"8%\" display=\"" + hasstatus + "\" text=\""+ SystemEnv.getHtmlLabelName(1335, user.getLanguage())
						+ "\" column=\"status\" orderkey=\"t1.status\" />";
				tableString += " <col width=\"10%\" display=\"" + hasreceivedpersons + "\" text=\""+ SystemEnv.getHtmlLabelName(16354, user.getLanguage())
						+ "\" _key=\"unoperators\" column=\"requestid\"  otherpara=\""+(userid+"+"+user.getLanguage()+"+column:userid")+"\" transmethod=\"weaver.general.WorkFlowTransMethod.getUnoperatorNew\"/>";
				tableString += "<col width=\"6%\" display=\"false\"  text=\""+ SystemEnv.getHtmlLabelName(19363, user.getLanguage())
						+ "\" _key=\"subwflink\" column=\"requestid\" orderkey=\"t1.requestid\"  linkkey=\"requestid\" linkvaluecolumn=\"requestid\" target=\"_self\" transmethod=\"weaver.general.WorkFlowTransMethod.getSubWFLink\"  otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"15%\" display=\"" + hasrequestmark + "\" text=\"" + SystemEnv.getHtmlLabelName(19502, user.getLanguage())
						+ "\" orderkey=\"t1.requestmark\" column=\"requestmark\"/>";
			} else {
				tableString = " <table instanceid=\"workflowRequestListTable\" tabletype=\"none\" pageId=\"" + pageId + "\" pageUid=\"" + pageUid
						+ "\" cssHandler=\"com.weaver.cssRenderHandler.request.CheckboxColorRender\" pagesize=\""+ pageSize + "\" >";
				tableString += temptableString;
				tableString += operateString + (isNeedHideBtn ? "" : getCheckBoxString(scope, checkBoxParam)) + "			<head>";
				tableString += temptablerowString;
				tableString += requestNameColumnStr;
				tableString += " <col width=\"10%\"  display=\""+ hasworkflowname + "\"  text=\""+ SystemEnv.getHtmlLabelName(125749, user.getLanguage())
						+ "\" column=\"workflowid\" orderkey=\"t1.workflowid\" transmethod=\""+workflownamereflectmethod+"\" />";

				tableString += " <col width=\"6%\" display=\""+ hascreater + "\"  text=\""+ SystemEnv.getHtmlLabelName(882, user.getLanguage())
						+ "\" column=\"creater\" orderkey=\"creater\"  otherpara=\"column:creatertype\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultName\" />";
				tableString += " <col width=\"10%\" display=\""+ hascreatedate+ "\" text=\""+ SystemEnv.getHtmlLabelName(722, user.getLanguage())
						+ "\" column=\"createdate\" orderkey=\"t1.createdate,t1.createtime\" otherpara=\"column:createtime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				tableString += " <col width=\"8%\" display=\""+ hasrequestlevel+ "\" text=\""+ SystemEnv.getHtmlLabelName(15534, user.getLanguage())
						+ "\" column=\"requestlevel\"  orderkey=\""+requestleve_str+"\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultUrgencyDegree\" otherpara=\""
						+ user.getLanguage() + "\"/>";

				if("done".equals(scope)){
					tableString += "<col width=\"10%\" display=\""+ hasreceivetime + "\"  text=\""+ SystemEnv.getHtmlLabelName(15502, user.getLanguage())
							+ "\" column=\"operatedateNew\" orderkey=\"operatedate,operatetime\" otherpara=\"column:operatetimeNew\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				}else{
					tableString += "<col width=\"10%\" display=\""+ hasreceivetime + "\"  text=\""+ SystemEnv.getHtmlLabelName(17994, user.getLanguage())
							+ "\" column=\"receivedate\" orderkey=\"receivedate,receivetime\" otherpara=\"column:receivetime\" transmethod=\"weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime\" />";
				}
				tableString += " <col width=\"8%\" display=\""+ hascurrentnode + "\"   text=\""+ SystemEnv.getHtmlLabelName(18564, user.getLanguage())
						+ "\" column=\"currentnodeid\" otherpara=\"column:requestid\" orderkey=\"t1.currentnodeid\" transmethod=\"weaver.general.WorkFlowTransMethod.getCurrentNode\"/>";

				tableString += " <col width=\"8%\" display=\"" + hasstatus + "\"    text=\""+ SystemEnv.getHtmlLabelName(1335, user.getLanguage())
						+ "\" column=\"status\" orderkey=\"t1.status\" />";
				tableString += " <col width=\"10%\" display=\"" + hasreceivedpersons + "\"   text=\""+ SystemEnv.getHtmlLabelName(16354, user.getLanguage())
						+ "\" _key=\"unoperators\" column=\"requestid\"  otherpara=\""
						+ (userid+"+"+user.getLanguage()+"+column:userid") + "\" transmethod=\"weaver.general.WorkFlowTransMethod.getUnoperatorNew\"/>";
				tableString += "<col width=\"6%\" display=\"false\"  text=\""+ SystemEnv.getHtmlLabelName(19363, user.getLanguage())
						+ "\" _key=\"subwflink\" column=\"requestid\" orderkey=\"t1.requestid\"  linkkey=\"requestid\" linkvaluecolumn=\"requestid\" target=\"_self\" transmethod=\"weaver.general.WorkFlowTransMethod.getSubWFLink\"  otherpara=\""
						+ user.getLanguage() + "\"/>";
				tableString += "<col width=\"15%\" display=\"" + hasrequestmark
						+ "\" orderkey=\"t1.requestmark\" text=\"" + SystemEnv.getHtmlLabelName(19502, user.getLanguage())
						+ "\" column=\"requestmark\"/>";
			}
		}
		if(!"".equals(tableString)){
			tableString += "<col hide=\"true\" column=\"userid\" />";
			tableString += "</head>" + "</table>";
		}

		String sessionkey = pageUid+"_"+Util.getEncrypt(Util.getRandom());
		Util_TableMap.setVal(sessionkey, tableString);

		//批量提交是否需要签字意见
		int multisubmitnotinputsign = 0;
		if(showBatchSubmit && isDoing){
			RecordSet.executeQuery("select multisubmitnotinputsign from workflow_RequestUserDefault where userId=?", userid);
			if(RecordSet.next())
				multisubmitnotinputsign = Util.getIntValue(Util.null2String(RecordSet.getString("multisubmitnotinputsign")), 0);
		}

		Map<String,String> sharearg = new HashMap<String,String>();
		sharearg.put("multisubmitnotinputsign", multisubmitnotinputsign+"");
		if(showBatchSubmit && isDoing && sysId != 5 && sysId != 8)
			sharearg.put("hasBatchSubmitBtn", "true");
		sharearg.put("hasBatchReadBtn", (!isNeedHideBtn && isDoing && CurrentUser.equals(userid+""))+"");	//自己待办才显示全部只读按钮

		boolean showBatchAttentionBtn = (!isNeedHideBtn && new RequestAttentionBiz().showBatchAttentionBtn(scope));
		sharearg.put("showBatchAttentionBtn", showBatchAttentionBtn+"");
		sharearg.put("showBatchForwardBtn", isNeedHideBtn ? "false" : "true");
		sharearg.put("showBtn", isNeedHideBtn ? "false" : "true");
		apidatas.put("sessionkey", sessionkey);
		apidatas.put("sharearg", sharearg);
		return apidatas;
	}

	/**
	 * 获取checkbox的xml节点
	 * @param scope
	 * @param params
	 * @return
	 */
	private String getCheckBoxString(String scope,String params){
		StringBuffer result = new StringBuffer();

		if("done".equals(scope) || "mine".equals(scope) || "doing".equals(scope)){
			result.append("<checkboxList>");

			if("doing".equals(scope)){
				//批量提交
				//获取trancemethod信息
				String multSubmitParam = this.listInfoEntity.getListOperateInfoEntity().getMultSubmitParam();
				String multSubmitMethod = this.listInfoEntity.getListOperateInfoEntity().getMultSubmitMethod();

				result.append("<checkboxpopedom  id=\"batchSubmit\"");
				result.append(" popedompara=\"").append(multSubmitParam).append("\"");
				result.append(" showmethod=\"").append(multSubmitMethod).append("\" />");
			}

			//批量关注
			result.append("<checkboxpopedom  id=\"batchAttention\"");
			result.append(" popedompara=\"").append(params).append("\"");
			result.append(" showmethod=\"").append(getClass().getName()).append(".getBatchAttentionCheckbox").append("\" />");

			//批量督办
			result.append("<checkboxpopedom  id=\"batchSupervisor\"");
			result.append(" popedompara=\"").append(params).append("\"");
			result.append(" showmethod=\"").append(getClass().getName()).append(".getBatchSupervisorCheckbox").append("\" />");

			//批量转发
			result.append("<checkboxpopedom  id=\"batchForward\"");
			result.append(" popedompara=\"").append(params).append("\"");
			result.append(" showmethod=\"").append(getClass().getName()).append(".getBatchForwardCheckbox").append("\" />");

			result.append("</checkboxList>");
		}

		return result.toString();
	}


	/**
	 * 是否有督办权限
	 * @param inStr("column:requestid+column:userid+column:workflowid)
	 * @return
	 */
	public String getBatchSupervisorCheckbox(String inStr){

		String[] tempStr = Util.TokenizerString2(inStr, "+");
		String requestid = Util.null2String(tempStr[0]);
		String userid = Util.null2String(tempStr[1]);//当前登录用户的userid

		boolean hasSupervisorRight = RequestListBiz.hasSupervisorRight(userid, requestid);

		return hasSupervisorRight ? "true" : "false";
	}

	/**
	 * 是否有转发权限
	 * @param inStr
	 * @return
	 */
	public String getBatchForwardCheckbox(String inStr){
		String[] tempStr = Util.TokenizerString2(inStr, "+");
		String requestid = Util.null2String(tempStr[0]);
		String userid = Util.null2String(tempStr[1]);//当前登录用户的userid
		String workflowid = Util.null2String(tempStr[2]);
		String usertype = Util.null2String(tempStr[3]);
		RecordSet rs = new RecordSet();
		rs.executeQuery("select prohibitBatchForward from workflow_base where id = ?",Util.getIntValue(workflowid));
		if(rs.next()) {
			if("1".equals(rs.getString("prohibitBatchForward"))) return "false";
		}

		int currentNodeType = 0;
		rs.executeQuery("select currentnodetype from workflow_requestbase where requestid  = ?",requestid);
		if(rs.next()) {
			currentNodeType = rs.getInt("currentnodetype");
		}

		RequestAuthenticationService authenticationService = new RequestAuthenticationService();
		User user = new User();
		user.setUid(Util.getIntValue(userid));
		user.setLogintype("0".equals(usertype) ? "1" : "2" );
		authenticationService.setUser(user);
		if(!authenticationService.getRequestUserRight(null, Util.getIntValue(requestid))) return  "false";
		Map<String, Object> authInfo = authenticationService.getAuthInfo();
		int currentOperateId = Util.getIntValue(Util.null2String(authInfo.get("wfcurrrid")));
		int nodeId = Util.getIntValue(Util.null2String(authInfo.get("nodeid")));
		int isremark = Util.getIntValue(Util.null2String(authInfo.get("isremarkForRM")));
		int takisremark = Util.getIntValue(Util.null2String(authInfo.get("takisremark")));
		int preisremark = Util.getIntValue(Util.null2String(authInfo.get("preisremark")));
		int agenttype = Util.getIntValue(Util.null2String(authInfo.get("agentType")));
		int nodetype = Util.getIntValue(Util.null2String(authInfo.get("nodetype")));

		int isFromWFRemark = -1;
		if (currentNodeType == 3) {
			isFromWFRemark = 2;
			if(isremark == 8 || isremark == 9) {
				isFromWFRemark = 0;
			}
		} else {
			if (isremark == 1 || isremark == 0 || isremark == 7 || isremark == 8 || isremark == 9 || isremark == 11) {
				//未回复意见征询人
				if(isremark == 0 && takisremark == -2) {
					isFromWFRemark = 1;
				} else {
					isFromWFRemark = 0;
				}
			} else if (isremark == 2) {
				isFromWFRemark = 1;
			}
		}

		int extendnodeid = nodeId;
		if(FreeNodeBiz.isFreeNode(extendnodeid)) {
			extendnodeid = FreeNodeBiz.getExtendNodeId(nodeId);
		}

		int wfid = Util.getIntValue(workflowid);

		//判断节点是否开启限制接收人范围
		rs.executeQuery("select isopen from workflow_FwLimitSet where fwtype='1' and nodeid= ? and wfid= ?",nodeId,wfid);
		if(rs.next()) {
			if("1".equals(rs.getString("isopen"))) {
				return "false";
			}
		}

		WFForwardManager wfForwardManager = new WFForwardManager();
		wfForwardManager.init();
		wfForwardManager.setWorkflowid(wfid);
		wfForwardManager.setNodeid(extendnodeid);
		wfForwardManager.setIsremark(Util.null2String(isremark));
		wfForwardManager.setTakIsremark(takisremark+"");
		wfForwardManager.setRequestid(Util.getIntValue(requestid));
		wfForwardManager.setBeForwardid(currentOperateId);
		wfForwardManager.getWFNodeInfo(preisremark);
		String IsPendingForward = wfForwardManager.getIsPendingForward();
		String IsTakingOpinions = wfForwardManager.getIsTakingOpinions();
		String IsHandleForward = wfForwardManager.getIsHandleForward();
		String IsBeForward = wfForwardManager.getIsBeForward();
		String IsSubmitedOpinion = wfForwardManager.getIsSubmitedOpinion();
		String IsSubmitForward = wfForwardManager.getIsSubmitForward();
		String IsAlreadyForward = wfForwardManager.getIsAlreadyForward();
		String IsWaitForwardOpinion = wfForwardManager.getIsWaitForwardOpinion();
		String IsBeForwardSubmit = wfForwardManager.getIsBeForwardSubmit();
		String IsBeForwardModify = wfForwardManager.getIsBeForwardModify();
		String IsBeForwardPending = wfForwardManager.getIsBeForwardPending();
		String IsBeForwardTodo = wfForwardManager.getIsBeForwardTodo();
		String IsBeForwardSubmitAlready = wfForwardManager.getIsBeForwardSubmitAlready();
		String IsBeForwardAlready = wfForwardManager.getIsBeForwardAlready();
		String IsBeForwardSubmitNotaries = wfForwardManager.getIsBeForwardSubmitNotaries();
		//转发记录
		if(preisremark == 1 && takisremark !=  2) {
			isFromWFRemark = Util.getIntValue(wfForwardManager.getIsFromWFRemark());
		}
		boolean canForwd = false;
		switch (isFromWFRemark) {
			case 0: //待办
				if (isremark == 0 || isremark == 7 || isremark == 8 || isremark == 9 || isremark == 11) {
					canForwd = "1".equals(IsPendingForward);
				} else if(isremark == 1) {
					if(takisremark == 2) {
						canForwd = "1".equals(IsPendingForward);
					} else {
						canForwd = "1".equals(IsBeForwardTodo);
					}
				}
				if(preisremark == 1 && takisremark !=  2) {
					canForwd = "1".equals(IsBeForwardTodo);
				}
				break;
			case 1: //已办
				if(preisremark == 0 || preisremark == 7 || preisremark == 8 || preisremark == 9 || preisremark == 11 || (preisremark == 2 && agenttype == 1)) {
					canForwd = "1".equals(IsAlreadyForward);
				} else if (preisremark == 1) {
					if(takisremark == 2) {
						canForwd = "1".equals(IsAlreadyForward);
					} else {
						canForwd = "1".equals(IsBeForwardAlready);
					}
				}
				break;
			case 2: //归档
				if(preisremark == 0 || preisremark == 7 || preisremark == 8 || preisremark == 9 || preisremark == 4 || (preisremark == 2 && agenttype == 1)) {
					canForwd = "1".equals(IsSubmitForward);
				} else if (preisremark == 1) {
					canForwd = "1".equals(IsBeForward);
				}
				break;
		}

		RequestSecondAuthService service = ServiceUtil.getService(RequestSecondAuthServiceImpl.class, user);
		Map<String, Object> params = new HashMap<String, Object>();

		params.put("workflowid", wfid);
		params.put("nodeid", extendnodeid + "");

		Map<String, Object> result = service.getSecondAuthConfig4Checkbox(params);
		String isEnableAuth = Util.null2String(result.get("isEnableAuth"));
		String isEnableProtect = Util.null2String(result.get("isEnableProtect"));
		int protectType = Util.getIntValue(Util.null2String(result.get("protectType")));    //数据保护的方式
		int secondAuthType = Util.getIntValue(Util.null2String(result.get("secondAuthType")));    //二次认证的方式
		int qysSignWay = Util.getIntValue(Util.null2String(result.get("qysSignWay")));	//2 契约锁服务 或者 1 契约锁单体

		if (("1".equals(isEnableAuth) && secondAuthType == SecondAuthType.RealIDAuth.getId()) || "1".equals(isEnableProtect)) {
			canForwd = false;
		}

		return canForwd ? "true" : "false";
	}

	//列表是否满足走微搜条件判断
	private boolean supportQuickSerach(boolean isopenos,String scope){
		int viewcondition = Util.getIntValue(Util.null2String(params.get("viewcondition")), 0);
		List<Integer> supportViewconList = Arrays.asList(0,10,16);
		if(!isopenos && "".equals(Util.null2String(params.get("resourceid"))) && "".equals(Util.null2String(params.get("tabkeys"))) && supportViewconList.contains(viewcondition)){//满足微搜条件调用微搜
			RecordSet rs = new RecordSet();
			rs.executeQuery("select * from HrmUserSetting where resourceId=?", user.getUID());//主次账号统一显示不走微搜，因为微搜暂时无法传给transmethod对应的userid
			String belongtoshow = "";
			if(rs.next()){
				belongtoshow = Util.null2String(rs.getString("belongtoshow"));
			}
			String Belongtoids = user.getBelongtoids();
			String doingStatus = Util.null2String(params.get("doingStatus"));
			String unophrmid = Util.null2String(params.get("unophrmid"));
			String creatertype = Util.null2String(params.get("creatertype"));
			String recievedateselect = Util.null2String(params.get("recievedateselect"));
			String operatedateselect = Util.null2String(params.get("operatedateselect"));
			if((!"1".equals(belongtoshow) || ("1".equals(belongtoshow) && "".equals(Belongtoids))) && ("0".equals(doingStatus) || "".equals(doingStatus)) && "".equals(unophrmid) && ("".equals(creatertype) || "0".equals(creatertype))){
				return (SearchBrowserUtils.quickSearchValidate("WFSEARCH",user.getLanguage() + "") && SearchBrowserUtils.isSupportWfRemarkStatus() &&
						(SearchBrowserUtils.isSupportWfReqList() || "mine".equals(scope)) && ("".equals(operatedateselect) || "0".equals(operatedateselect)) && ("".equals(recievedateselect) || "0".equals(recievedateselect)));
			}
		}
		return false;
	}

	/**
	 * 是否有批量关注权限
	 * @param inStr
	 * @return
	 */
	public String getBatchAttentionCheckbox(String inStr){
		return "true";
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public ListInfoEntity getListInfoEntity() {
		return listInfoEntity;
	}

	public void setListInfoEntity(ListInfoEntity listInfoEntity) {
		this.listInfoEntity = listInfoEntity;
	}
}
