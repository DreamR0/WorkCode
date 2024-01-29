package com.engine.workflow.cmd.mobileCenter;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.Checkboxpopedom;
import com.api.browser.bean.SplitTableBean;
import com.api.browser.bean.SplitTableColBean;
import com.api.browser.util.*;
import com.cloudstore.dev.api.bean.SplitMobileDataBean;
import com.engine.common.util.ParamUtil;
import com.engine.workflow.biz.RequestQuickSearchBiz;
import com.engine.workflow.biz.mobileCenter.MobileDimensionsBiz;
import com.engine.workflow.biz.mobileCenter.WorkflowCenterTabBiz;
import com.engine.workflow.biz.requestList.RequestListBiz;
import com.engine.workflow.constant.PageUidConst;
import com.engine.workflow.entity.requestList.ListInfoEntity;
import com.engine.workflow.util.GetCustomLevelUtil;
import com.engine.workflow.util.OrderByListUtil;
import weaver.conn.RecordSet;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.fullsearch.util.SearchBrowserUtils;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.system.RequestDefaultComInfo;
import weaver.systeminfo.SystemEnv;

import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.workflow.biz.requestList.GenerateDataInfoBiz;
import com.engine.workflow.entity.RequestListDataInfoEntity;
import weaver.workflow.request.todo.OfsSettingObject;
import weaver.workflow.request.todo.RequestUtil;
import weaver.workflow.workflow.WorkflowConfigComInfo;

/**
 * 移动端-流程中心列表数据
 * @author liuzy 2018-08-10
 */
public class GetListResultCmd extends AbstractCommonCommand<Map<String,Object>>{

	private HttpServletRequest request;
	private CustomerInfoComInfo cci = null;
	private ResourceComInfo rc = null;
	private RequestDefaultComInfo requestDefaultComInfo = new RequestDefaultComInfo();


	/**ƒ
	 * 列表上一些可以个性化的信息， 供个性化使用（后续可继续完善）
	 */
	private ListInfoEntity listInfoEntity;

	public GetListResultCmd(HttpServletRequest request, User user){
		this.request = request;
		this.user = user;
		this.listInfoEntity = new ListInfoEntity();

		try {
			this.cci = new CustomerInfoComInfo();
			this.rc = new ResourceComInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
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
		boolean isopenos = ofso.getIsuse() == 1;// 是否开启异构系统待办
		boolean showdone = "1".equals(ofso.getShowdone());//异构系统是否显示已办数据
		WorkflowConfigComInfo wfconfig = new WorkflowConfigComInfo();
		int usequicksearch = Util.getIntValue(wfconfig.getValue("use_quicksearch_wflist"));//流程入口，是否使用微搜
		if(usequicksearch == 1 && false && this.supportQuickSerach(isopenos)){//满足微搜条件调用微搜
			return new RequestQuickSearchBiz().getRequestList4WfList(ParamUtil.request2Map(request), user,true);
		}
		//流程名称反射方法(兼容E8)
		String workflownamereflectmethod = "weaver.workflow.workflow.WorkflowComInfo.getWorkflowname";
		if(isopenos)
			workflownamereflectmethod = "weaver.general.WorkFlowTransMethod.getWorkflowname";
		String requestnamereflectclass = "com.api.workflow.util.WorkFlowSPATransMethod";
		Map<String,String> reqparams = bean.getReqparams();
		boolean showBatchSubmit = bean.isShowBatchSubmit();
		boolean isMergeShow = bean.isMergeShow();
		String CurrentUser = bean.getCurrentUser();
		String userIDAll = bean.getUserIDAll();
		if(!isMergeShow){
			userIDAll=""+user.getUID();
		}
		reqparams.put("isMergeShow",isMergeShow && !userIDAll.equals(String.valueOf(user.getUID()))?"1":"0");//设置开启并且有次账号
		boolean isQueryByNewTable = RequestListBiz.isQueryByNewTable(user,reqparams);
		apidatas.put("isQueryByNewTable",isQueryByNewTable);
		String orderby = bean.getOrderclause();
		String orderbyos = bean.getOrderclause_os();
		String sqlwhere = bean.getWhereclause();
		String sqlwhereos = bean.getWhereclause_os();
		String sqlwhereosDone = bean.getWhereclause_osDone();
		/* 处理流程中心 待办-已办tab页*/
		String mobileTabId = Util.null2String(request.getParameter("mobileTabId"));
		boolean isFormWfCenter = false;
		boolean isTransWfCenterOrder = true;
		if(!"".equals(mobileTabId)){
			isFormWfCenter = true;
			Map<String,String> wfCenterInfo = null;
			HashMap<String,Object> wfCenterParams = new HashMap<String,Object>();
			wfCenterParams.put("mobileTabId", mobileTabId);
			wfCenterParams.put("viewType", Util.null2String(request.getParameter("viewType")));
			wfCenterParams.put("menuid", Util.null2String(request.getParameter("menuid")));
			String wfCenterJsonstr = WorkflowCenterTabBiz.getWfCenterTabWhere(wfCenterParams);
			if(wfCenterJsonstr != null && !"".equals(wfCenterJsonstr)){
				Map<String,Object> jsonparams = JSON.parseObject(wfCenterJsonstr, Map.class);
				wfCenterInfo = WorkflowCenterTabBiz.getDefTabWhere(jsonparams);
				if(wfCenterInfo!=null){
					String wfsqlwhere = Util.null2String(wfCenterInfo.get("whereclause"));
					String wfsqlwhere_os = Util.null2String(wfCenterInfo.get("whereclause_os"));
					String wforderby = Util.null2String(wfCenterInfo.get("orderby"));
					if(!"".equals(wfsqlwhere)){
						sqlwhere += wfsqlwhere;
					}
					if(!"".equals(wfsqlwhere_os)){
						sqlwhereos += wfsqlwhere_os.replaceAll("ofs_todo.", "");
						sqlwhereosDone += wfsqlwhere_os.replaceAll("ofs_todo.","");
					}
					if(!"".equals(wforderby)){
						isTransWfCenterOrder = false;
						orderby = wforderby;
						orderbyos = wforderby.replaceAll("t1.", "").replaceAll("t2.", "");
					}
				}
				new BaseBean().writeLog("--获取的 流程中心 参数:" + JSONObject.toJSONString(wfCenterInfo));
			}
		}
		/* 处理流程中心 待办-已办tab页*/
		int userid = user.getUID();
		int usertype = "2".equals(user.getLogintype()) ? 1 : 0;
		String scope = Util.null2String(reqparams.get("viewScope"));
		if(scope == null || "".equals(scope.trim())) {
			scope = Util.null2String(reqparams.get("mobileDimensionScope"));
		}
		int sysId = Util.getIntValue(reqparams.get("sysId"), 0);
		boolean isDoing = "doing".equals(scope);
		String myorderby = "",colname="",isordertype="";
		OrderByListUtil obu = new OrderByListUtil(this.user);
		if(isDoing) {
			myorderby =  obu.getMyOrderByStr(this.user.getUID(), PageUidConst.WF_LIST_DOING);
			if("".equals(myorderby)) {//如果为空，首选需要区分是初始未设置，还是用户清空数据了?
				myorderby  += " receivedate desc, receivetime desc";
			}
		}

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
		String backfields0 = " sysid,appurl,requestid,requestmark,createdate, createtime,creater, creatertype, workflowid, requestname, requestnamenew, " +
				"status,requestlevel,currentnodeid,viewtype,userid,receivedate,receivetime,isremark,nodeid,agentorbyagentid,agenttype,isprocessed "
				+ operateDateTimeFieldSql0 + ",systype,workflowtype";
		// 原始查询字段
		String backfields = " 0 as sysid,t1.requestid as appurl,t1.requestid,t1.requestmark,t1.createdate, t1.createtime,t1.creater, t1.creatertype, t1.workflowid, t1.requestname, t1.requestnamenew," +
				" t1.status,t1.requestlevel,t1.currentnodeid,t2.viewtype,t2.userid,t2.usertype,t2.receivedate,t2.receivetime,t2.isremark,t2.nodeid,t2.agentorbyagentid,t2.agenttype,t2.isprocessed "
				+ operateDateTimeFieldSql + " ,'0' as systype,t2.workflowtype";
		// 异构系统查询字段
		String backfieldsOs = " sysid,requestid as appurl,requestid,'' as requestmark,createdate, createtime,creatorid as creater, 0 as creatertype, workflowid, requestname, requestname as requestnamenew, " +
				"'' as status,requestlevel,-1 as currentnodeid,viewtype,userid,0 as usertype,receivedate,receivetime,isremark,0 as nodeid, -1 as agentorbyagentid,'0' as agenttype,'0' as isprocessed "
				+ operateDateTimeFieldSqlOs + ",'1' as systype, sysid as workflowtype";
		//反馈黄点提示字段
		backfields0 += ",viewDate,viewTime,lastFeedBackDate,lastFeedBackTime,needwfback,lastFeedBackOperator";
		backfields += ",t2.viewDate,t2.viewTime,t1.lastFeedBackDate,t1.lastFeedBackTime,t2.needwfback,t1.lastFeedBackOperator";
		backfieldsOs += ",'' as viewDate,'' as viewTime,'' as lastFeedBackDate,'' as lastFeedBackTime,'' as needwfback,0 as lastFeedBackOperator";
		//反馈黄点提示字段
		String fromSql = " from workflow_requestbase t1,workflow_currentoperator t2,workflow_base t3 ";

		String para2 = "column:requestid+column:workflowid+column:viewtype+0+" + user.getLanguage()
				+ "+column:nodeid+column:isremark+" + user.getUID()
				+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+column:userid+0+column:creater+" + userIDAll;
		String para4 = user.getLanguage() + "+" + user.getUID() + "+column:userid";

		para2 = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S+" + para2;

		//配置参数
		SplitTableBean tableBean = new SplitTableBean();
		tableBean.setPageID("");
		tableBean.setPageUID("");
		tableBean.setPagesize("");

		tableBean.setBackfields(backfields);
		tableBean.setSqlform(fromSql);
		tableBean.setSqlorderby(orderby);


		if (isopenos) {
			orderby = orderbyos;
			String orderyOsDone = "";
//			if ("done".equals(scope)) {
//				orderby = "";
//                orderby = " ORDER BY " +
//						"operatedate DESC," +
//						"operatetime DESC ";
//				//orderyOsDone="";
//			}

			para2 = "column:requestid+column:workflowid+column:viewtype+0+" + user.getLanguage()
					+ "+column:nodeid+column:isremark+" + user.getUID()
					+ "+column:agentorbyagentid+column:agenttype+column:isprocessed+" +
					"column:userid+0+column:creater+" + userIDAll + "+column:systype+column:workflowtype";
			para2 = "S+column:viewDate+column:viewTime+column:lastFeedBackDate+column:lastFeedBackTime+column:needwfback+column:lastFeedBackOperator+column:userid+S+" + para2;
			//==zj 这里接收type值，如果在范围之内就设置添加typeid(移动端，这个是用来区分流程标题需要显示什么颜色)
			try{
				int  viewcondition =Util.getIntValue(Util.null2String(request.getParameter("viewcondition")),-1);
				if (viewcondition != -1){
					viewcondition =Integer.parseInt(Util.null2String(request.getParameter("viewcondition"))) ;
					new BaseBean().writeLog("==zj==(移动端-para2添加下参数)"+viewcondition);
					if (viewcondition >=82 && viewcondition <=88){
						para2 +="+"+viewcondition;
					}
				}
			}catch (Exception e){
				new BaseBean().writeLog("==zj==(移动端listresultcmd报错)"+e);
			}

			if(isDoing) {//需要特殊处理
				backfields0 =  this.getOrderBy() + backfields0;
				if(!isFormWfCenter || isTransWfCenterOrder){
					orderby = myorderby;
				}
				if(orderby.contains("overtime")){		// 处理移动端流程中心优先显示 超时流程
					backfields0 = backfields0 + ",overtime ";
					backfields = backfields + ",case when ((t2.isremark='0' and (t2.isprocessed='0' or t2.isprocessed='3' or t2.isprocessed='2')) or t2.isremark='5') then '1' else '0' end as overtime ";
					backfieldsOs = backfieldsOs + ",'0' as overtime ";
				}
				fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
						+ " union (select distinct " + backfieldsOs + " from ofs_todo_data " + sqlwhereos + ") " + " ) t1 ) t1 ";
			} else if("done".equals(scope)){//异构系统不显示已办可以直接不查已办表
				if(showdone){
					fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
							+ " union (select distinct " + backfieldsOs + " from ofs_done_data " + sqlwhereosDone + ") " + " ) t1 ) t1 ";
				}else{
					fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere + " ) t1 ) t1 ";
				}
			} else if("mine".equals(scope)){		// 我的请求 默认排序条件 创建日期 创建时间
				if(showdone){//异构系统不显示已办时，我的请求sql和待办sql一致
					fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
							+ " union (select distinct " + backfieldsOs + " from ofs_todo_data " + sqlwhereos + ") union (select distinct " + backfieldsOs + " from ofs_done_data" + sqlwhereosDone + ") ) t1 ) t1 ";
				}else{
					fromSql = " from (select " + backfields0 + " from (select " + backfields + " " + fromSql + "" + sqlwhere
							+ " union (select distinct " + backfieldsOs + " from ofs_todo_data " + sqlwhereos + ") " + " ) t1 ) t1 ";
				}
				if(orderby==null || "".equals(orderby)){
					orderby = " receivedate,receivetime ";
				}
			} else{
				orderby = " receivedate,receivetime ";
			}
			//orderby = " receivedate,receivetime  ";

			tableBean.setBackfields(backfields0);
			tableBean.setSqlwhere("");
		} else {
			if((orderby.toLowerCase().indexOf("operatedate") != -1 || orderby.toLowerCase().indexOf("operatetime") != -1) && (isTransWfCenterOrder || !isFormWfCenter)){
				//已办含case when情况排序特殊处理
				fromSql = " from (select " + backfields + " " + fromSql + "" + sqlwhere + ") t1 ";
				orderby = orderby.replace("t2.", "t1.");
				tableBean.setBackfields(backfields0);
				tableBean.setSqlwhere("");
			}else{
				if(isDoing) {//需要特殊处理
					backfields = this.getOrderBy() + backfields;
					if(!isFormWfCenter || isTransWfCenterOrder){	// 移动端流程中心 -待办 走应用配置的排序
						orderby = myorderby;
					}else if(orderby.contains("overtime")){		// 处理 overtime 条件
						backfields = backfields + ",case when ((t2.isremark='0' and (t2.isprocessed='0' or t2.isprocessed='3' or t2.isprocessed='2')) or t2.isremark='5') then '1' else '0' end as overtime ";
					}
				}
				orderby = OrderByListUtil.appendRequestIdOrderBy(orderby,"t1");
				tableBean.setBackfields(backfields);
				tableBean.setSqlwhere((sqlwhere));
			}
		}
		tableBean.setSqlform(isQueryByNewTable ? RequestListBiz.transNewTable(user,fromSql) : fromSql);
		orderby = OrderByListUtil.appendRequestIdOrderBy(orderby);
		tableBean.setSqlorderby(orderby);
		tableBean.setSqlprimarykey("requestid");
		tableBean.setSqlsortway("Desc");
		tableBean.setSqlisdistinct("false");


		List<SplitTableColBean> cols=new ArrayList<>();
		//top
		SplitTableColBean topCol=new SplitTableColBean();
		topCol.setColumn("requestname");
		topCol.setText(SystemEnv.getHtmlLabelName(1334, user.getLanguage()));
		topCol.setMobiletransmethod(requestnamereflectclass + ".getTitle4Mobile_AttentionTag");
		topCol.setMobileotherpara(para2);
		topCol.setMobileviewtype(MobileViewTypeAttr.HIGHLIGHT);
		topCol.setBelong(BelongAttr.PCMOBILE);
		cols.add(topCol);

		SplitTableColBean leftCol=new SplitTableColBean();
		leftCol.setColumn("createdate");
		leftCol	.setText(SystemEnv.getHtmlLabelName(722, user.getLanguage()));
		//.setWidth("65%")
		leftCol.setMobiletransmethod("weaver.general.WorkFlowTransMethod.getWFSearchResultCreateTime");
		leftCol	.setMobileotherpara("column:createtime");
		leftCol	.setMobileviewtype(MobileViewTypeAttr.DETAIL);
		leftCol.setBelong(BelongAttr.PCMOBILE);
		//left
		cols.add(leftCol);
		//right
		SplitTableColBean rightCol=new SplitTableColBean();
		rightCol.setColumn("workflowid");
		rightCol.setText(SystemEnv.getHtmlLabelName(259, user.getLanguage()));
		rightCol.setMobiletransmethod(workflownamereflectmethod);
		if(isopenos){
			rightCol.setMobileotherpara("column:sysid");    // 20190906 wwp
		}

		//rightCol.setTransmethod(workflownamereflectmethod);
		rightCol.setBelong(BelongAttr.PCMOBILE);
		cols.add(rightCol);


		SplitTableColBean requestidCol=new SplitTableColBean();
		requestidCol.setColumn("requestid");
		//rightCol.setTransmethod(workflownamereflectmethod);
		requestidCol.setBelong(BelongAttr.PCMOBILE);
		cols.add(requestidCol);

		//zzw
		int menuid = Util.getIntValue(reqparams.get("menuid"),-1);
		MobileDimensionsBiz mdb = new MobileDimensionsBiz();
		if(!"mine".equals(mdb.getScope(menuid)) && !"mine".equals(scope)){
			SplitTableColBean createrCol = new SplitTableColBean();
			createrCol.setColumn("creater");
			createrCol.setMobileotherpara("column:creatertype");
			createrCol.setMobiletransmethod("com.engine.workflow.cmd.mobileCenter.GetListResultCmd.getWFSearchResultName");
			createrCol.setBelong(BelongAttr.PCMOBILE);
			cols.add(createrCol);
		}

		//appurl---start
		SplitTableColBean appurlCol = new SplitTableColBean();
		appurlCol.setColumn("appurl");
		appurlCol.setMobileotherpara("column:sysid+column:workflowid+column:userid+1");
		appurlCol.setMobiletransmethod("weaver.general.WorkFlowTransMethod.getAppUrl");
		appurlCol.setBelong(BelongAttr.PCMOBILE);
		cols.add(appurlCol);
		//appurl---end

		//钉钉、企业微信pc客户端以默认浏览器打开流程
		SplitTableColBean openByDefaultBrowserCol = new SplitTableColBean();
		openByDefaultBrowserCol.setColumn("requestid");
		openByDefaultBrowserCol.setKey("openByDefaultBrowser");
		openByDefaultBrowserCol.setMobiletransmethod(requestnamereflectclass+".getOpenByDefaultBrowserFlag");
		openByDefaultBrowserCol.setBelong(BelongAttr.MOBILE);
		cols.add(openByDefaultBrowserCol);

		//移动端打开异构系统流程，是否启动监听时间，当异构系统流程提交后自动刷新列表
		SplitTableColBean autoReloadWfListTimeCol = new SplitTableColBean();
		autoReloadWfListTimeCol.setColumn("requestid");
		autoReloadWfListTimeCol.setMobileotherpara("1");
		autoReloadWfListTimeCol.setKey("autoReloadWfListTime");
		autoReloadWfListTimeCol.setMobiletransmethod(RequestListBiz.class.getName()+".getAutoReloadWfListTime");
		autoReloadWfListTimeCol.setBelong(BelongAttr.MOBILE);
		cols.add(autoReloadWfListTimeCol);

		//userid
		SplitTableColBean useridCol = new SplitTableColBean();
		useridCol.setColumn("userid");
//        useridCol.setMobileotherpara("column:usertype");
//        useridCol.setMobiletransmethod(requestnamereflectclass+".getMobileUseridStr");
		useridCol.setBelong(BelongAttr.PCMOBILE);
		cols.add(useridCol);

		SplitTableColBean userType = new SplitTableColBean();
		userType.setColumn("usertype");
//        useridCol.setMobileotherpara("column:usertype");
//        useridCol.setMobiletransmethod(requestnamereflectclass+".getMobileUseridStr");
		userType.setBelong(BelongAttr.MOBILE);
		cols.add(userType);
		//userid
		SplitTableColBean primaryCol = new SplitTableColBean();
		primaryCol.setColumn("primarykey");
		//getprimaryKey
		primaryCol.setMobiletransmethod(requestnamereflectclass + ".getprimaryKey");
		primaryCol.setMobileotherpara("column:requestid+column:userid");
		primaryCol.setBelong(BelongAttr.PCMOBILE);
		primaryCol.setIsPrimarykey(BoolAttr.TRUE);
		primaryCol.setHide("true");
		cols.add(primaryCol);


		SplitTableColBean ciCol = new SplitTableColBean();
		ciCol.setColumn("primaryInfo");
		//getprimaryKey
		ciCol.setMobiletransmethod(requestnamereflectclass + ".getprimaryInfo");
		ciCol.setMobileotherpara("column:userid+" + userIDAll);
		ciCol.setBelong(BelongAttr.MOBILE);
		ciCol.setIsPrimarykey(BoolAttr.TRUE);
		ciCol.setHide("true");
		cols.add(ciCol);



		tableBean.setCols(cols);
		tableBean.setTableType("checkbox");
		List list1 = new ArrayList();
		if(!"mine".equals(mdb.getScope(menuid)) && !"mine".equals(scope)){
			list1 = JSON.parseArray(JSON_CONFIG2, SplitMobileDataBean.class);
		}else{
			list1 = JSON.parseArray(JSON_CONFIG2_MOBILE, SplitMobileDataBean.class);
		}
		tableBean.createMobileTemplate(list1);


		List<Checkboxpopedom> checkBoxList = new ArrayList<Checkboxpopedom>();



		//可提交
		if(isDoing ||(showBatchSubmit&&menuid>0)){
			Checkboxpopedom checkboxpopedom =new Checkboxpopedom();
			checkboxpopedom.setId("batchSubmit");
			String multSubmitParam = this.listInfoEntity.getListOperateInfoEntity().getMultSubmitParam();
			String multSubmitMethod = this.listInfoEntity.getListOperateInfoEntity().getMultSubmitMethod();
			checkboxpopedom.setShowmethod(multSubmitMethod);
			checkboxpopedom.setPopedompara(multSubmitParam);

			checkBoxList.add(checkboxpopedom);
		}

		//批量督办，批量关注
		if("done".equals(scope) || "mine".equals(scope) || isDoing){
			//批量督办
			Checkboxpopedom checkboxpopedom =new Checkboxpopedom();
			checkboxpopedom.setId("batchSupervise");
			String multSubmitParam = "column:requestid+column:userid+column:workflowid";
			String multSubmitMethod = "com.engine.workflow.cmd.requestList.GetListResultCmd.getBatchSupervisorCheckbox";
			checkboxpopedom.setShowmethod(multSubmitMethod);
			checkboxpopedom.setPopedompara(multSubmitParam);
			checkBoxList.add(checkboxpopedom);

			//批量关注
			Checkboxpopedom checkboxpopedom2 =new Checkboxpopedom();
			checkboxpopedom2.setId("batchAttention");
			String multSubmitParam2 = "column:requestid+column:userid+column:workflowid";
			String multSubmitMethod2 = "com.engine.workflow.cmd.requestList.GetListResultCmd.getBatchAttentionCheckbox";
			checkboxpopedom2.setShowmethod(multSubmitMethod2);
			checkboxpopedom2.setPopedompara(multSubmitParam2);
			checkBoxList.add(checkboxpopedom2);
		}

		//批量转发
		Checkboxpopedom checkboxpopedom3 =new Checkboxpopedom();
		checkboxpopedom3.setId("batchForward");
		String multSubmitParam = "column:requestid+column:userid+column:workflowid+"+usertype;
		String multSubmitMethod = "com.engine.workflow.cmd.requestList.GetListResultCmd.getBatchForwardCheckbox";
		checkboxpopedom3.setShowmethod(multSubmitMethod);
		checkboxpopedom3.setPopedompara(multSubmitParam);
		checkBoxList.add(checkboxpopedom3);

		tableBean.setCheckboxList(checkBoxList);


		//显示多列
		tableBean.setMobileshowtype(MobileShowTypeAttr.ListView);
		//String sessionkey = "workflow_"+scope+"_"+Util.getEncrypt(Util.getRandom());
		apidatas.putAll(SplitTableUtil.makeListDataResult(tableBean));

		//批量提交是否需要签字意见
		int multisubmitnotinputsign = 0;
		if(showBatchSubmit && isDoing){
			RecordSet.executeQuery("select multisubmitnotinputsign from workflow_RequestUserDefault where userId=?", userid);
			if(RecordSet.next())
				multisubmitnotinputsign = Util.getIntValue(Util.null2String(RecordSet.getString("multisubmitnotinputsign")), 0);
		}
		if(showBatchSubmit && isDoing && sysId != 5 && sysId != 8)
			apidatas.put("hasBatchSubmitBtn", "true");
		apidatas.put("multisubmitnotinputsign", multisubmitnotinputsign);
		RequestListBiz.removeRecord(user);//钉钉、企业微信以默认浏览器打开流程，刷新时清空客户端和浏览器交互数据
		//开启连续处理
		boolean isOpenContinuationProcess = "1".equals(requestDefaultComInfo.getIsOpenContinnuationProcess(userid+""));
		apidatas.put("isOpenContinuationProcess", "doing".equals(scope) && isOpenContinuationProcess);
		return apidatas;
	}

	/**
	 * 使用case...when的方式进行设置orderby
	 * @return
	 */
	private  String getOrderBy() {
		List<Map<String, Object>> list= GetCustomLevelUtil.getAllLevel(null, this.user.getLanguage());

		StringBuffer sb = new StringBuffer(" (case requestlevel  ");
		StringBuffer sb1 = new StringBuffer("");
		for(Map<String, Object> map : list) {
			sb1.append(" when "+map.get("id") +" then "+map.get("showorder")) ;
		}
		if("".equals(sb1.toString().trim())) {//判断有无数据没有数据则不拼接
			return "";
		}
		sb.append(sb1);
		sb.append(" else -1 end ) as requestlevelorder, ");
		return sb.toString();
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

	public String getWFSearchResultName(String id, String type) {
		String returnStr = "";
		if ("1".equals(type)) { //外部
			returnStr = cci.getCustomerInfoname(id) + " ";

		} else { //内部
			returnStr = rc.getResourcename(id) + " ";
		}
		return returnStr;
	}


	public GetListResultCmd() {
		try {
			this.cci = new CustomerInfoComInfo();
			this.rc = new ResourceComInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//列表是否满足走微搜条件判断
	private boolean supportQuickSerach(boolean isopenos){
		int viewcondition = Util.getIntValue(Util.null2String(request.getParameter("viewcondition")), 0);
		List<Integer> supportViewconList = Arrays.asList(41,46,51);
		if(!isopenos && "".equals(Util.null2String(request.getParameter("resourceid"))) && "".equals(Util.null2String(request.getParameter("tabkeys"))) && supportViewconList.contains(viewcondition)){//满足微搜条件调用微搜
			RecordSet rs = new RecordSet();
			rs.executeQuery("select * from HrmUserSetting where resourceId=?", user.getUID());//主次账号统一显示不走微搜，因为微搜暂时无法传给transmethod对应的userid
			String belongtoshow = "";
			if(rs.next()){
				belongtoshow = Util.null2String(rs.getString("belongtoshow"));
			}
			String Belongtoids = user.getBelongtoids();
			String scope = Util.null2String(request.getParameter("viewScope"));
			String recievedateselect = Util.null2String(request.getParameter("recievedateselect"));
			String operatedateselect = Util.null2String(request.getParameter("operatedateselect"));
			String hrmcreaterid = Util.null2String(request.getParameter("hrmcreaterid"));
			if((!"1".equals(belongtoshow) || ("1".equals(belongtoshow) && "".equals(Belongtoids))) && "mine".equals(scope) && ("".equals(recievedateselect) || "0".equals(recievedateselect))
					&& ("".equals(operatedateselect) || "0".equals(operatedateselect)) && "".equals(hrmcreaterid)){//暂时只放出我的请求，相当于支持我的请求高级搜索的标题，路径，归档状态和流程状态默认值
				return (SearchBrowserUtils.quickSearchValidate("WFSEARCH",user.getLanguage() + "") && SearchBrowserUtils.isSupportWfRemarkStatus());
			}
		}
		return false;
	}

	final String JSON_CONFIG = "[" +
			"    {" +
			"        \"configs\": [" +
			"            {" +
			"                \"configs\": [" +
			"                    {" +
			"                        \"key\": \"requestname\"" +
			"                    }" +
			"                ]," +
			"                \"key\": \"col1_row1\"" +
			"            }," +
			"            {" +
			"                \"configs\": [" +
			"                    {" +
			"                        \"key\": \"createdate\"" +
			"                    }," +
			"                    {" +
			"                        \"style\": {" +
			"                            \"float\": \"right\"" +
			"                        }," +
			"                        \"key\": \"workflowid,\"" +
			"                        \"class\": \"workflowid\"" +
			"                    }" +
			"                ]," +
			"                \"key\": \"col1_row2\"" +
			"            }" +
			"        ]," +
			"        \"key\": \"col1\"" +
			"    }" +
			"]";

	String JSON_CONFIG3 = "[\n" +
			"    {\n" +
			"        \"configs\": [\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                       \"key\": \"requestname\",\n" +
			"                        \"style\": {\n" +
			"                            \"fontWeight\": \"inherit\",\"color\": \"#000\",\"width\": \"96%\"" +
			"                        }\n" +
			"                    },{\"key\":\"primaryInfo\"}\n" +
			"                ],\n" +
			"                \"key\": \"col1_row1\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                        \"key\": \"creater\",\n" +
			"                        \"style\": {\n" +
			"                            \"marginRight\": \"5px\"\n" +
			"                        },\n" +
//			"                        \"className\": \"workflowid\"" +
			"                    },\n" +
			"                    {\n" +
			"                        \"key\": \"createdate\",\n" +
//			"                        \"className\": \"wf-center-list-createdate\"" +
			"                    },\n" +
			"                    {\n" +
//            "                        \"style\": {\n" +
//            "                            \"float\": \"right\"\n" +
//            "                        },\n" +
			"                        \"key\": \"workflowid\",\n" +
//			"                        \"className\": \"wf-center-list-workflowid\"" +
			"                    }\n" +
			"                ],\n" +
			"                \"key\": \"col1_row2\"\n" +
			"            }\n" +
			"        ],\n" +
			"        \"key\": \"col1\"\n" +
			"    }\n" +
			"]";

	public static final String JSON_CONFIG2 = "[\n" +

			"    {\n" +
			"        \"configs\": [\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                       \"key\": \"requestname\",\n" +
			"                        \"style\": {\n" +
			"                            \"fontWeight\": \"inherit\",\"color\": \"#000\",\"width\": \"96%\"" +
			"                        }\n" +
			"                    },{\"key\":\"primaryInfo\"}\n" +
			"                ],\n" +
			"                \"key\": \"col1_row1\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                        \"key\": \"workflowid\",\n" +
			"                        \"style\": {\n" +
			"                            \"textOverflow\": \"ellipsis\",\n" +
			"                            \"overflow\": \"hidden\",\n" +
			"                            \"whiteSpace\": \"nowrap\",\n" +
			"                            \"width\": \"90%\",\n" +
			"                        },\n" +
			"                    },\n" +
			"                ],\n" +
			"                \"key\": \"col1_row2\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                        \"key\": \"creater\",\n" +
			"                        \"style\": {\n" +
			"                            \"marginRight\": \"5px\"\n" +
			"                        },\n" +
			"                    },\n" +
			"                    {\n" +
			"                        \"key\": \"createdate\",\n" +
			"                        \"style\": {\n" +
			"                            \"marginRight\": \"5px\"\n" +
			"                        },\n" +
			"                    },\n" +
			"                ],\n" +
			"                \"key\": \"col1_row3\"\n" +
			"            },\n" +
			"        ],\n" +
			"        \"key\": \"col1\"\n" +
			"    }\n" +
			"]";

	public static final String JSON_CONFIG2_MOBILE = "[\n" +

			"    {\n" +
			"        \"configs\": [\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                       \"key\": \"requestname\",\n" +
			"                        \"style\": {\n" +
			"                            \"fontWeight\": \"inherit\",\"color\": \"#000\",\"width\": \"96%\"" +
			"                        }\n" +
			"                    },{\"key\":\"primaryInfo\"}\n" +
			"                ],\n" +
			"                \"key\": \"col1_row1\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                        \"key\": \"workflowid\",\n" +
			"                        \"style\": {\n" +
			"                            \"textOverflow\": \"ellipsis\",\n" +
			"                            \"overflow\": \"hidden\",\n" +
			"                            \"whiteSpace\": \"nowrap\",\n" +
			"                            \"width\": \"90%\",\n" +
			"                        },\n" +
			"                    },\n" +
			"                ],\n" +
			"                \"key\": \"col1_row2\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"configs\": [\n" +
			"                    {\n" +
			"                        \"key\":\"createdate\",\n" +
			"                        \"style\": {\n" +
			"                            \"marginRight\": \"5px\"\n" +
			"                        },\n" +
			"                    },\n" +
			"                ],\n" +
			"                \"key\": \"col1_row3\"\n" +
			"            },\n" +
			"        ],\n" +
			"        \"key\": \"col1\"\n" +
			"    }\n" +
			"]";
}
