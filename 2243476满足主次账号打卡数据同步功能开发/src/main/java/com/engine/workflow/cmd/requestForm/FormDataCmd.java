package com.engine.workflow.cmd.requestForm;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.BrowserValueInfo;
import com.api.browser.service.BrowserValueInfoService;
import com.api.fna.service.impl.FnaWorkflowService;
import com.cloudstore.dev.api.bean.LogType;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.BizLogContext;
import com.engine.common.entity.EncryptFieldEntity;
import com.engine.core.exception.ECException;
import com.engine.core.interceptor.CommandContext;
import com.engine.workflow.biz.FieldInfo.FieldInfoBiz;
import com.engine.workflow.biz.customizeBrowser.BrowserFieldUtil;
import com.engine.workflow.biz.detailFilter.DetailFilterBiz;
import com.engine.workflow.biz.detailFilter.DetailFilterSetBiz;
import com.engine.workflow.biz.freeNode.FreeNodeBiz;
import com.engine.workflow.biz.requestFlow.SaveFormDatasBiz;
import com.engine.workflow.biz.requestForm.FileBiz;
import com.engine.workflow.biz.requestForm.MobileHtmlConvertBiz;
import com.engine.workflow.biz.requestForm.RequestFormBiz;
import com.engine.workflow.entity.core.DetailRowInfoEntity;
import com.engine.workflow.entity.core.DetailTableInfoEntity;
import com.engine.workflow.entity.core.MainTableInfoEntity;
import com.engine.workflow.entity.core.RequestInfoEntity;
import com.engine.workflow.entity.requestForm.FieldInfo;
import com.engine.workflow.entity.requestForm.FieldValueBean;
import com.engine.workflow.entity.requestForm.SelectItem;
import com.engine.workflow.entity.requestForm.TableInfo;
import com.engine.workflow.util.CommonUtil;
import com.engine.workflow.util.GetCustomLevelUtil;
import com.google.common.base.Strings;
import com.jayway.jsonpath.internal.Utils;
import weaver.car.CarDateTimeUtil;
import weaver.conn.RecordSet;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.dateformat.DateTransformer;
import weaver.general.BaseBean;
import weaver.general.LocateUtil;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.workflow.exceldesign.DetailOrderManager;
import weaver.workflow.request.RequestPreAddinoperateManager;
import weaver.workflow.workflow.WorkflowConfigComInfo;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 表单数据信息
 * @author liuzy 2018/5/2
 */
public class FormDataCmd extends AbstractCommonCommand<Map<String,Object>>{
	
	private int requestid;
	private int workflowid;
	private int nodeid;
	private int formid;
	private int isbill;
	private int creater;
	private Map<String, Object> requestMap;
	private boolean belongMain = true;
	private Map<String,TableInfo> tableinfomap = new LinkedHashMap<String,TableInfo>();
	private Map<String,Object> customizeBrowserValueCache;

	private Map<String,FieldValueBean> maindata = new HashMap<String,FieldValueBean>();		//主表数据信息
	private List<Map<String,Object>> detailDelayConvertInfo = new ArrayList<>();	//延时转换浏览按钮信息

	//主表取数
	public FormDataCmd(Map<String, Object> requestMap, User user, Map<String,Object> params, Map<String,TableInfo> tableinfomap){
		Boolean result = false;
		if ("".equals(Util.null2String(requestMap.get("f_weaver_belongto_userid")))) {
			if (!user.isAdmin()) {
				//==zj 判断当前用户主账号是否有考勤组
				String userId = String.valueOf(user.getUID());
				KqUtil kqUtil = new KqUtil();
				//先判断该流程id是不是考勤流程
				new BaseBean().writeLog("==zj==(主表取数)" + JSON.toJSONString(requestMap));
				String requestid = Util.null2String(requestMap.get("requestid"));
				String workflowid = Util.null2String(requestMap.get("workflowid"));
				new BaseBean().writeLog("==zj==(loadForm-requestid)" + requestid);
				if (!"".equals(requestid)){
					//如果不是新建考勤流程会有requestid
					result =   kqUtil.checkKqFlowReq(requestid);
					new BaseBean().writeLog("==zj==(非新建流程是否来自考勤流程-主表)" +result);
				}
				if (!"".equals(workflowid)){
					//如果是新建流程有workflowid
					result = kqUtil.checkKqFlow(workflowid);
					new BaseBean().writeLog("==zj==(新建流程是否来自考勤流程-主表)" +result);
				}

				if (result) {
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					String today = format.format(System.currentTimeMillis());
					if (!kqUtil.mainAccountCheck(userId, today)) {
						//如果主账号没有，再查次账号
						List<Map> resultlist = kqUtil.childAccountCheck(userId, today);
						new BaseBean().writeLog("==zj==(主表取数)" + JSON.toJSONString(resultlist));
						if (resultlist.size() == 1) {
							Map<String, String> resultMap = resultlist.get(0);
							int userid = Integer.parseInt(resultMap.get("userid"));
							new BaseBean().writeLog("==zj==(主表取数)" + userid);
							user = new User(userid);
						}
					}
				}
			}
		}
		this.requestMap = requestMap;
		this.user = user;
		
		this.params = params;
		this.requestid = Util.getIntValue(getRequestParam("requestid"), -1);
		this.workflowid = Util.getIntValue(Util.null2String(this.params.get("workflowid")), 0);
		this.nodeid = Util.getIntValue(Util.null2String(this.params.get("nodeid")), 0);
		if(FreeNodeBiz.isFreeNode(this.nodeid)) {
			this.nodeid = Util.getIntValue(Util.null2String(this.params.get("freeNodeExtendNodeId")));
		}
		this.formid = Util.getIntValue(Util.null2String(this.params.get("formid")), 0);
		this.isbill = Util.getIntValue(Util.null2String(this.params.get("isbill")), -1);
		this.tableinfomap = tableinfomap;
	}
	
	//明细表取数
	public FormDataCmd(Map<String, Object> requestMap, User user){
		boolean result = false ;	//判断是否要执行次账号
		//==zj 明细表修改
		if ("".equals(Util.null2String(requestMap.get("f_weaver_belongto_userid")))){
			if (!user.isAdmin()) {
				//==zj 判断当前用户主账号是否有考勤组
				String userId = String.valueOf(user.getUID());
				KqUtil kqUtil = new KqUtil();
				//先判断该流程id是不是考勤流程
				new BaseBean().writeLog("==zj==(主表取数)" + JSON.toJSONString(requestMap));
				String requestid = Util.null2String(requestMap.get("requestid"));
				String workflowid = Util.null2String(requestMap.get("workflowid"));
				new BaseBean().writeLog("==zj==(loadForm-requestid)" + requestid);
				if (!"".equals(requestid)){
					//如果不是新建考勤流程会有requestid
					result =   kqUtil.checkKqFlowReq(requestid);
					new BaseBean().writeLog("==zj==(非新建流程是否来自考勤流程-明细表)" +result);
				}
				if (!"".equals(workflowid)){
					//如果是新建流程有workflowid
					result = kqUtil.checkKqFlow(workflowid);
					new BaseBean().writeLog("==zj==(新建流程是否来自考勤流程-明细表)" +result);
				}

				if (result){
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					String today = format.format(System.currentTimeMillis());
					if (!kqUtil.mainAccountCheck(userId,today)){
						//如果主账号没有，再查次账号
						List<Map> resultlist = kqUtil.childAccountCheck(userId, today);
						new BaseBean().writeLog("==zj==(明细表-生效考勤组)" + JSON.toJSONString(resultlist));
						if (resultlist.size()==1){
							Map<String,String> resultMap= resultlist.get(0);
							int userid =Integer.parseInt(resultMap.get("userid")) ;
							new BaseBean().writeLog("==zj==(明细表-生效考勤组人员id)" + userid);
							user = new User(userid);
						}
					}
				}
			}
		}


		new BaseBean().writeLog("==zj==(明细表user)" + JSON.toJSONString(user));
		this.requestMap = requestMap;
		this.user = user;
		this.belongMain = false;
		
		this.params = JSON.parseObject(getRequestParam("reqParams"), Map.class);
		this.requestid = Util.getIntValue(Util.null2String(getRequestParam("requestid")), -1);
		this.workflowid = Util.getIntValue(Util.null2String(this.params.get("workflowid")), 0);
		this.nodeid = Util.getIntValue(Util.null2String(this.params.get("nodeid")), 0);
		int isrequest = Util.getIntValue(Util.null2String(getRequestParam("isrequest")), 1);
		if(isrequest!=1){
			params.put("isrequest",isrequest);
		}
		if(FreeNodeBiz.isFreeNode(this.nodeid)) {
			this.nodeid = Util.getIntValue(Util.null2String(this.params.get("freeNodeExtendNodeId")));
		}
		this.creater = Util.getIntValue(Util.null2String(this.params.get("creater")), -1);
		this.formid = Util.getIntValue(Util.null2String(this.params.get("formid")), 0);
		this.isbill = Util.getIntValue(Util.null2String(this.params.get("isbill")), -1);
		boolean isDelayConvertName = "1".equals(getRequestParam("isDelayConvertName"));
		if(isDelayConvertName){
			//来源第一次加载明细数据存储的缓存，使用一次即清除
			String delayCacheKey = Util.null2String(getRequestParam("delayCacheKey"));
			this.detailDelayConvertInfo = (List<Map<String,Object>>)Util_TableMap.getObjValWithLog(LogType.NONE,"workflowcache","",delayCacheKey);
			Util_TableMap.clearVal(delayCacheKey);
		}else{
			String apiResultCacheKey = Util.null2String(params.get("apiResultCacheKey"));
			//字段信息缓存，使用一次即清除
			String sessionkey = "wfTableInfo_"+requestid+"_"+user.getUID()+"_"+apiResultCacheKey;
			this.tableinfomap = (Map<String,TableInfo>) Util_TableMap.getObjValWithLog(LogType.NONE,"workflowcache","",sessionkey);
			Util_TableMap.clearVal(sessionkey);
			//主表数据缓存，使用一次即清除
			String mainDataKey = "wfMainData_"+requestid+"_"+user.getUID()+"_"+apiResultCacheKey;
			this.maindata = (Map<String,FieldValueBean>) Util_TableMap.getObjValWithLog(LogType.NONE,"workflowcache","",mainDataKey);
			Util_TableMap.clearVal(mainDataKey);
		}
	}
	
	public Map<String,Object> execute(CommandContext commandContext) throws ECException{
		Map<String,Object> result = new HashMap<String,Object>();
		try{
			boolean isDelayConvertName = "1".equals(getRequestParam("isDelayConvertName"));
			if(tableinfomap == null && !isDelayConvertName)
				throw new Exception("generateData Get forminfo Empty Exception"+this.requestid);
			boolean iscreate = "1".equals(Util.null2String(params.get("iscreate")));
			if(this.requestid > 0) this.customizeBrowserValueCache  = BrowserFieldUtil.loadRequestCustomizeBrowserValues(this.requestid);
			if(belongMain){
				Map<String,FieldValueBean> mainDataResult = iscreate ? this.generateCreateDefaultValue() : this.generateMainData();
				//包含明细才加缓存，主表数据存缓存，明细自定义浏览转name依赖需要
				if(this.tableinfomap.containsKey("detail_1")) {
					String mainDataKey = "wfMainData_" + requestid + "_" + user.getUID() + "_" + Util.null2String(params.get("apiResultCacheKey"));
					Util_TableMap.setObjValWithLog(LogType.NONE, "workflowcache", "", mainDataKey, mainDataResult, 180);
				}
				result.put("datas", mainDataResult);
			}else{
				if(isDelayConvertName)
					result = this.generateDetailDelayData();
				else
					result = this.generateDetailData();
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new ECException(e.getMessage());
		}
		Util_TableMap.setObjValWithLog(LogType.NONE,"workflowcache","detailData"+user.getUID(),result);
		return result;
	}
	
	@Override
	public BizLogContext getLogContext() {
		return null;
	}

	private String getRequestParam(String key){
		return Util.null2String(this.requestMap.get(key));
	}
	
	/**
	 * 流程创建生成主表字段默认值
	 */
	private Map<String,FieldValueBean> generateCreateDefaultValue() throws Exception{
		/**********获取相关参数************/
 		String hrmid = getRequestParam("hrmid");
		String prjid = getRequestParam("prjid");
		String reqid = getRequestParam("reqid");
		String docid = getRequestParam("docid");
		String crmid = getRequestParam("crmid");
		if ("".equals(hrmid) && "1".equals(this.user.getLogintype())) {
			hrmid = "" + this.user.getUID();
		} else if ("".equals(crmid) && "2".equals(this.user.getLogintype())) {
			crmid = "" + this.user.getUID();
		}
		String defhrmid = Util.getIntValue(hrmid, 0)+"";
		if("1".equals(Util.null2String(params.get("isagent")))){	//代理判断
			defhrmid = Util.null2String(params.get("beagenter"));
		}
		/**********获取节点前附加操作************/
        RequestPreAddinoperateManager requestPreAddM = new RequestPreAddinoperateManager();
		requestPreAddM.setCreater(user.getUID());
		requestPreAddM.setOptor(user.getUID());
		requestPreAddM.setWorkflowid(workflowid);
		requestPreAddM.setNodeid(nodeid);
		requestPreAddM.setRequestid(requestid);
		Hashtable getPreAddRule_hs = requestPreAddM.getPreAddRule();
		Hashtable inoperatefield_hs = (Hashtable) getPreAddRule_hs.get("inoperatefield_hs");
		Hashtable fieldvalue_hs = (Hashtable) getPreAddRule_hs.get("inoperatevalue_hs");
		int nodecustomid = Util.getIntValue(getRequestParam("nodecustomid"),-1);
 		Map<String,String> targetfieldids=new HashMap<>();
		if(nodecustomid>0&&Util.getIntValue(reqid)>0){
			//如果此参数大于0
			//先获取所有数据
			RecordSet rsz=new RecordSet();
			rsz.executeQuery("select fieldid,targetfieldid from WORKFLOW_CREATEFLOWSET where targetfieldid!=0 and nodecustomid=?",nodecustomid);
			while (rsz.next()){
 				targetfieldids.put(rsz.getString(1),rsz.getString(2));
			}
		}
		
		/***********循环主表字段赋默认值************/
		Map<String,FieldValueBean> maindefdata = new HashMap<String,FieldValueBean>();
		TableInfo maininfo = this.tableinfomap.get("main");
		Map<String,FieldInfo> mainfields = maininfo.getFieldinfomap();
		RequestInfoEntity requestinfo=null;
		MainTableInfoEntity mainTableInfoEntity=null;
		if(!targetfieldids.isEmpty()){
			requestinfo = FieldInfoBiz.getRequestinfo(reqid);
			mainTableInfoEntity = requestinfo.getLazyMainTableInfoEntity().get();
		}



		for(Map.Entry<String,FieldInfo> entry : mainfields.entrySet()){
			FieldInfo fieldinfo = entry.getValue();
			int fieldid = fieldinfo.getFieldid();
			String defvalue = null;
			if("manager".equals(fieldinfo.getFieldname())){
				defvalue = this.getManagerFieldValue();
			}else{
				if("2".equals(this.user.getLogintype()))	//客户门户不赋默认值
					defhrmid = "";
				defvalue = this.getFieldDefValue(fieldinfo, inoperatefield_hs, fieldvalue_hs, defhrmid, crmid, reqid, docid, prjid);
			}

			if(!targetfieldids.isEmpty()&&mainTableInfoEntity!=null&&targetfieldids.containsKey(fieldid+"")){
				String value = Util.null2String(mainTableInfoEntity.getDatas().get(targetfieldids.get(fieldid+"") + ""));
				if(!"".equals(value)){
					defvalue=value;
				}
			}
			String isReturnFieldValue = new WorkflowConfigComInfo().getValue("isReturnFieldValue");	//当字段不在模板上时是否返回值。
			boolean isReturn = true;	//默认当字段不在也返回。
			if ("".equals(Util.null2String(isReturnFieldValue))) {
				String[] split = isReturnFieldValue.split(",");
				for (String s : split) {
					if (Util.getIntValue(s, -1) == workflowid) {
						isReturn = false;
					}
				}
			}
			boolean existLayout = fieldinfo.isExistLayout();		//判断字段是否在模板上。
			if (!existLayout && !isReturn) {	//如果不在模板上，而且这个流程配置的不在模板上不返回
				defvalue = null;
			}
			if(defvalue != null)	//空串允许(节点前清空值情况)，只能对null判断
				maindefdata.put("field"+fieldid, this.buildFieldValueBean(fieldinfo, defvalue));

		}
		
		//财务字段联动赋值
        FnaWorkflowService fnaWorkflowService = new FnaWorkflowService();
        fnaWorkflowService.getFnaMainDefValue(workflowid, maindefdata, user, defhrmid, inoperatefield_hs);

		//处理新建流程




		//判断是否为车辆流程
        if(isCarWorkflow()){
            //单独处理车辆多时区
            maindefdata =changeCarDateTime(maindefdata);
        }
		//系统字段赋默认值
		int defaultName = 0;
		boolean messageType = false;
		boolean mailmessageType = false;
		int smsAlertsType = -1;
		int mailmessageAlertType = -1;
		RecordSet rs = new RecordSet();
		rs.executeQuery("select * from workflow_base where id="+workflowid);
		if(rs.next()){
			defaultName = Util.getIntValue(rs.getString("defaultName"), 0);
			if(rs.getInt("messageType") == 1)	messageType = true;
			if(rs.getInt("mailmessagetype") == 1)		mailmessageType = true;
			smsAlertsType = Util.getIntValue(rs.getString("smsAlertsType"), -1);
			mailmessageAlertType = Util.getIntValue(rs.getString("mailmessagetype"), -1);
		}
		//流程标题-1
		int requestname_viewattr = mainfields.containsKey("-1") ? mainfields.get("-1").getViewattr() : 0;
		if(defaultName == 1 || requestname_viewattr == 0){	//标题默认规则或标题字段未放置在模板上
			String username = "";
			if (this.user.getLogintype().equals("1"))
				username = new ResourceComInfo().getLastname(defhrmid);
			if (this.user.getLogintype().equals("2"))
				username = new CustomerInfoComInfo().getCustomerInfoname(crmid);
			
			weaver.general.DateUtil DateUtil = new weaver.general.DateUtil();
			String deftitle = DateUtil.getWFTitleNew(""+workflowid, defhrmid, username, this.user.getLogintype());
			deftitle = Util.toScreenToEdit(deftitle, user.getLanguage());
			
			FieldValueBean requestnamebean = new FieldValueBean();
			requestnamebean.setValue(deftitle);
			maindefdata.put("field-1", requestnamebean);
		}
		// 紧急程度-2
		FieldValueBean requestlevelbean = new FieldValueBean();
		String value = new GetCustomLevelUtil().getFristLeveId();
		requestlevelbean.setValue(value);//不能等于0封存
		requestlevelbean.setSpecialobj(getCustomLevel(getViewattr(-2), value));
		maindefdata.put("field-2", requestlevelbean);
		// 短信提醒-3
		if(messageType){
			FieldValueBean messagetypebean = new FieldValueBean();
			messagetypebean.setValue(smsAlertsType+"");
			maindefdata.put("field-3", messagetypebean);
		}
		// 微信提醒-5
		if(mailmessageType){
			FieldValueBean chatstypebean = new FieldValueBean();
			chatstypebean.setValue(mailmessageAlertType+"");
			maindefdata.put("field-5", chatstypebean);
		}
		return maindefdata;
	}

	/**
	 * 获取紧急程度数据
	 * @param viewattr
	 * @param requestlevel
	 * @return
	 */
	private  List<Map<String, Object>> getCustomLevel(int viewattr, String requestlevel) {
		String isprint = Util.null2String(params.get("isprint"));
		if("1".equals(isprint.trim()) || viewattr == 1) {//只读
			return new GetCustomLevelUtil().getCustomLevel("0",requestlevel+"", this.user.getLanguage());
		} else{//可编辑-选择没有封存的数据
			return new GetCustomLevelUtil().getCustomLevel("0",null, this.user.getLanguage());
		}
	}

	/**
	 * 获取字段字段是否可编辑
	 * @param filedid
	 * @return
	 */
	private int getViewattr(int filedid) {
		TableInfo maininfo = this.tableinfomap.get("main");
		Map<String,FieldInfo> mainfields = maininfo.getFieldinfomap();
		int viewattr = -1;
		String filetype = "";
		for(Map.Entry<String,FieldInfo> entry : mainfields.entrySet()){
			FieldInfo fieldinfo = entry.getValue();
			if(fieldinfo.getFieldid()  == filedid) {
				filetype = fieldinfo.getFielddbtype();
				viewattr =  fieldinfo.getViewattr();
				break;
			}
		}
		//这里的判断依据src4js/pc4mobx/workflowForm/util/public/fieldUtil.js类中的判断得来
		if(this.params.containsKey("isviewonly") && "1".equals(this.params.get("isviewonly").toString())) {
			viewattr = 1;//1表示只读
		}
		if(this.params.containsKey("isaffirmance") && "1".equals(this.params.get("isaffirmance").toString()) && !"9-1".equals(filetype)) {
			viewattr = 1;//1表示只读
		}
		return viewattr;
	}

	/**
	 * 获取新的紧急程度id  如果给定的id被封存,并不是只读则直接返回新的紧急程度id，反之返回本身
	 * @return
	 */
	public String getNewLevel(int viewattr ,String requestlevel) {
		String isprint = Util.null2String(params.get("isprint"));
		GetCustomLevelUtil glu = new GetCustomLevelUtil();
		if("1".equals(isprint.trim()) || viewattr == 1) {//只读 返回本身
			return requestlevel;
		} else {
			if(glu.isfcByRequestlevel(requestlevel)) {
				return glu.getFristLeveId();
			} else {
				return requestlevel;
			}
		}
	}

	/**
	 * 流程编辑生成主表数据信息
	 */
	private Map<String,FieldValueBean> generateMainData() throws Exception{
		int currentNodeType = -1;
		String isremark = Util.null2String(params.get("isremark"));

		RecordSet rs = new RecordSet();
		//系统字段值
		rs.executeSql("select * from workflow_requestbase where requestid="+requestid);
		if(rs.next()){
			currentNodeType = Util.getIntValue(rs.getString("currentnodetype"));
			
			FieldValueBean requestnamebean = new FieldValueBean();
			requestnamebean.setValue(this.convertSpecialChar(CommonUtil.convertChar(rs.getString("requestname"))));
			maindata.put("field-1", requestnamebean);
			
			int requestlevel = Util.getIntValue(rs.getString("requestlevel"),0);
			FieldValueBean requestlevelbean = new FieldValueBean();
			//获取当前字段是否可编辑
			int viewattr = getViewattr(-2);
			requestlevel = Util.getIntValue(getNewLevel(viewattr, requestlevel+""), -1);
			requestlevelbean.setSpecialobj(getCustomLevel(viewattr, requestlevel+""));
			requestlevelbean.setValue(requestlevel+"");
			maindata.put("field-2", requestlevelbean);
			
			int messagetype = Util.getIntValue(rs.getString("messagetype"),-1);
			FieldValueBean messagetypebean = new FieldValueBean();
			messagetypebean.setValue(messagetype+"");
			maindata.put("field-3", messagetypebean);
			
			int mailmessagetype = Util.getIntValue(rs.getString("chatsType"),-1);
			FieldValueBean chatstypebean = new FieldValueBean();
			chatstypebean.setValue(mailmessagetype+"");
			maindata.put("field-5", chatstypebean);
		}
		//打印次数
		int printcount=0;
		rs.executeSql("select count(*) as printcount from workflow_viewlog where requestid='"+requestid+"'");
		if(rs.next()){
			printcount=Util.getIntValue(rs.getString("printcount"),0);
		}
		FieldValueBean printcountbean = new FieldValueBean();
		printcountbean.setValue(printcount+"");
		maindata.put("field-9", printcountbean);
		//自定义表单字段值
		TableInfo maininfo = this.tableinfomap.get("main");
		String tablename = maininfo.getTablename();
		String tablecolumn = maininfo.getTablecolumn();
		if(tablecolumn.endsWith(","))
			tablecolumn = tablecolumn.substring(0, tablecolumn.length()-1);
		String recordsql = "";
		if(isbill == 0){
			recordsql = "select "+tablecolumn+" from "+tablename+" where requestid="+requestid+" and billformid="+formid;
		}else{
			if("".equals(tablecolumn.trim())){		//再次验证是否主表没任何字段，此情况不抛异常
				boolean countFlag = rs.executeQuery("select count(*) from workflow_billfield where billid=? and (viewtype is null or viewtype='0')", formid);
				if(countFlag && rs.next() && rs.getInt(1) == 0)
					tablecolumn = "requestid";
			}
			recordsql = "select id,"+tablecolumn+" from "+tablename+" where requestid="+requestid;
		}
		boolean flag = rs.executeQuery(recordsql);
		if(!flag || rs.getCounts() == 0)
			throw new Exception("执行主表取数据SQL失败："+recordsql);
		if(rs.next()){
			//自定义字段值
			Map<String,FieldInfo> mainfields = maininfo.getFieldinfomap();
			for(Map.Entry<String, FieldInfo> entry : mainfields.entrySet()){
				FieldInfo fieldinfo = entry.getValue();
				int fieldid = fieldinfo.getFieldid();
				if(fieldid < 0)
					continue;
				//联动修改浏览框类型
				int detailtype_convert = 0;
				if(fieldinfo.getHtmltype() == 3 && fieldinfo.getBrowserattr() != null && fieldinfo.getBrowserattr().getTypeLinkageCfg() != null)
					detailtype_convert = this.changeBrowserDetailType(fieldinfo, mainfields, rs);
				String fieldvalue = "";
				if("manager".equals(fieldinfo.getFieldname()) && currentNodeType != 3
					&& ("0".equals(isremark) || "5".equals(isremark) || "7".equals(isremark))){
					fieldvalue = this.getManagerFieldValue();
				}else{
					fieldvalue = getFieldDBValue(rs, fieldinfo, tablename);
				}
				maindata.put("field"+fieldid, this.buildFieldValueBean(fieldinfo, fieldvalue, detailtype_convert, rs, new HashMap<String,Object>()));
			}
			//判断是否为车辆流程
            if(isCarWorkflow()){
                //单独处理车辆多时区
                maindata =changeCarDateTime(maindata);
            }
			maininfo.setRecordnum(rs.getInt("id"));
		}
		return maindata;
	}
	
	
	/**
	 * 生成明细表数据信息(可单个/多个明细请求)
	 */
	public Map<String,Object> generateDetailData() throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		RecordSet rs = new RecordSet();
		int billmainid = this.getBillMainId();
		int isviewonly = Util.getIntValue(this.params.get("isviewonly")+"");
		boolean isCreate = "1".equals(params.get("iscreate")+"");
		/**********获取相关参数************/
		String hrmid = getRequestParam("hrmid");
		String prjid = getRequestParam("prjid");
		String reqid = getRequestParam("reqid");
 		String docid = getRequestParam("docid");
		String crmid = getRequestParam("crmid");
		if ("".equals(hrmid) && "1".equals(this.user.getLogintype())) {
			hrmid = "" + this.user.getUID();
		} else if ("".equals(crmid) && "2".equals(this.user.getLogintype())) {
			crmid = "" + this.user.getUID();
		}
		String defhrmid = Util.getIntValue(hrmid, 0)+"";
		if("1".equals(Util.null2String(params.get("isagent")))){	//代理判断
			defhrmid = Util.null2String(params.get("beagenter"));
		}
		/**********获取节点前附加操作************/
		Hashtable inoperatefield_hs = new Hashtable();
		Hashtable fieldvalue_hs = new Hashtable();
		if(isviewonly != 1){
	        RequestPreAddinoperateManager requestPreAddM = new RequestPreAddinoperateManager();
			requestPreAddM.setCreater(isCreate?user.getUID():creater);
			requestPreAddM.setOptor(user.getUID());
			requestPreAddM.setWorkflowid(workflowid);
			requestPreAddM.setNodeid(nodeid);
			requestPreAddM.setRequestid(requestid);
			Hashtable getPreAddRule_hs = requestPreAddM.getPreAddRule();
			inoperatefield_hs = (Hashtable) getPreAddRule_hs.get("inoperatefield_hs");
			fieldvalue_hs = (Hashtable) getPreAddRule_hs.get("inoperatevalue_hs");
		}
		
		String detailmark = getRequestParam("detailmark");
		String[] detailmarkArr = detailmark.split(",");
		String billDetailKey = RequestFormBiz.getBillDetailKeyField(formid);

		//右键-新建流程赋值
		int nodecustomid = Util.getIntValue(getRequestParam("nodecustomid"));
		Map<String, String> targetfieldids = new HashMap<>();
		List<DetailTableInfoEntity> detailTableInfoEntities = null;
		if (nodecustomid > 0 && Util.getIntValue(reqid) > 0) {
			RecordSet rsz = new RecordSet();
			rsz.executeQuery("select fieldid,targetfieldid from WORKFLOW_CREATEFLOWSET where targetfieldid!=0 and nodecustomid=?", nodecustomid);
			while (rsz.next()) {
				targetfieldids.put(rsz.getString(1), rsz.getString(2));
			}
			RequestInfoEntity requestinfo = FieldInfoBiz.getRequestinfo(reqid);
			detailTableInfoEntities = requestinfo.getLazyDetailTableInfos().get();
		}

		DetailOrderManager detailOrderManager = new DetailOrderManager();
		for(String key : detailmarkArr){
			TableInfo detailinfo = this.tableinfomap.get(key);
			if(detailinfo == null)
				continue;
			Map<String,Object> detailmap = new HashMap<String,Object>();
			int tableindex = detailinfo.getTableindex();
			Map<String,FieldInfo> fieldinfomap = detailinfo.getFieldinfomap();
			String tablename = detailinfo.getTablename();
			String tablecolumn = detailinfo.getTablecolumn();
			if(tablecolumn.endsWith(","))
				tablecolumn = tablecolumn.substring(0, tablecolumn.length()-1);
			if( !Strings.isNullOrEmpty(tablecolumn) && !tablecolumn.startsWith(",") )
				tablecolumn = "," + tablecolumn;
			
			//获取明细表排序sql（老表单明细表序号从0开始计数，这里+1处理）
			String orderSql = detailOrderManager.getOrderSql(workflowid+"", nodeid+"", formid+"", isbill+"", user.getLanguage()+"", tableindex  + "", tablename, "t","id");

			/**********取明细已有行数据***********/
			Map<String,Object> rowdatas = new HashMap<String,Object>();
			boolean isprint = Util.null2String(params.get("isprint")).equalsIgnoreCase("1");
			DetailFilterBiz detailFilterBiz = new DetailFilterBiz(workflowid,requestid, nodeid, isprint ? DetailFilterSetBiz.PRINT_MODE_TYPE :  DetailFilterSetBiz.SHOW_MODE_TYPE, tableindex + "", user);
			int rowindex = 0;
			if(!isCreate){
				boolean isopenpaging = detailinfo.getDetailtableattr().getIsopenpaging() == 1;
				int pagesize = isopenpaging ? detailinfo.getDetailtableattr().getPagesize() : 1000;	//每页条数
				String primaryKey = RequestFormBiz.getDetailTablePrimaryKey(formid);
				String recordsql = "";
				if(isbill == 0){
					recordsql = "select "+primaryKey+tablecolumn+" from "+tablename+" t where requestid="+requestid+" and groupid="+(tableindex-1)+" order by " + orderSql;
				}else{
					recordsql = "select "+primaryKey+tablecolumn+" from "+tablename+" t where "+billDetailKey+"="+billmainid+" order by " + orderSql;
				}
				rs.executeQuery(recordsql);
				int rscount = 0;
				while(rs.next()){
					rscount++;
					Map<String,Object> fieldvaluemap = new HashMap<String,Object>();
					int detailRecordId = Util.getIntValue(rs.getString(primaryKey));
					Map<Integer, String> filterValueMap = new HashMap<>();
 					for(Map.Entry<String, FieldInfo> entry : fieldinfomap.entrySet()){
						FieldInfo fieldinfo = entry.getValue();
						int fieldid = fieldinfo.getFieldid();
						//联动修改浏览框类型
						int detailtype_convert = 0;
						if(fieldinfo.getHtmltype() == 3 && fieldinfo.getBrowserattr() != null && fieldinfo.getBrowserattr().getTypeLinkageCfg() != null)
							detailtype_convert = this.changeBrowserDetailType(fieldinfo, fieldinfomap, rs);
						String fieldvalue = getFieldDBValue(rs, fieldinfo, tablename);
						if(!"".equals(fieldvalue)){		//明细空值字段不生成，优化前端遍历字段*行性能
							Map<String,Object> otherparams = new HashMap<>();
							otherparams.put("dTableName", tablename);
							otherparams.put("detailRecordId", detailRecordId+"");
							otherparams.put("delay", rscount>pagesize ? "1" : "0");
							otherparams.put("rowMark", key+"|"+rowindex);
							fieldvaluemap.put("field"+fieldid, this.buildFieldValueBean(fieldinfo, fieldvalue, detailtype_convert, rs, otherparams));
						}
						filterValueMap.put(fieldid, fieldvalue);
					}
					//明细主键、顺序
					fieldvaluemap.put("keyid", detailRecordId);

					//明细过滤
					if(detailFilterBiz.needFilter(filterValueMap)) {
						continue;
					}
//					fieldvaluemap.put("orderid", rowindex+1);


					rowdatas.put("row_"+rowindex, fieldvaluemap);
					rowindex++;
				}
			}else{
				//TODO 明细赋值
				if (!targetfieldids.isEmpty()) {
					//获取当前明细中的所有需要赋值字段
					List<Integer> fieldIds = new ArrayList<>();
					if (!targetfieldids.isEmpty()) {
						for (Map.Entry<String, FieldInfo> entry : fieldinfomap.entrySet()) {
							FieldInfo fieldinfo = entry.getValue();
							int fieldid = fieldinfo.getFieldid();
							//如果存在此明细字段
							if (targetfieldids.containsKey(fieldid + "")) {
								fieldIds.add(fieldid);
							}
						}
						//获取原来流程明细表数据
						for (DetailTableInfoEntity table : detailTableInfoEntities) {

							for (DetailRowInfoEntity detailRowInfoEntity : table.getDetailRowInfos()) {
								Map<String, Object> fieldvaluemap = new HashMap<String, Object>();
								//每一个 datas是每一条数据
								Map<String, Object> datas = detailRowInfoEntity.getDatas();
								int detailRecordId = Util.getIntValue(Util.null2String(datas.get("id")));
								boolean isfiled = false;
								for (Map.Entry<String, FieldInfo> entry : fieldinfomap.entrySet()) {
									FieldInfo fieldinfo = entry.getValue();
									int fieldid = fieldinfo.getFieldid();
									String fieldKey = targetfieldids.get(fieldid + "");
									if (fieldIds.contains(fieldid) && datas.containsKey(fieldKey)) {
										//联动修改浏览框类型
										isfiled = true;
										int detailtype_convert = 0;
										String fieldvalue = Util.null2String(datas.get(fieldKey));
										fieldvaluemap.put("field" + fieldid, this.buildFieldValueBean(fieldinfo, fieldvalue, detailtype_convert, detailRecordId));
									}
								}
								if (isfiled) {
									fieldvaluemap.put("keyid", 0);
									rowdatas.put("row_" + rowindex, fieldvaluemap);
									rowindex++;
								}
							}

						}
					}
				}
			}
			detailmap.put("indexnum", rowindex);
			detailmap.put("rowDatas", rowdatas);
			/**********获取明细新增行字段默认值************/
			if(isviewonly != 1){
				Map<String,Object> addRowDefValue = new HashMap<String,Object>();
				for(Map.Entry<String, FieldInfo> entry : fieldinfomap.entrySet()){
					FieldInfo fieldinfo = entry.getValue();
					int fieldid = fieldinfo.getFieldid();
					String defvalue = this.getFieldDefValue(fieldinfo, inoperatefield_hs, fieldvalue_hs, defhrmid, crmid, reqid, docid, prjid);
					if(defvalue != null)	//空串允许(节点前清空值情况)，只能对null判断
						addRowDefValue.put("field"+fieldid, this.buildFieldValueBean(fieldinfo, defvalue));
				}
				
				FnaWorkflowService fnaWorkflowService = new FnaWorkflowService();
				fnaWorkflowService.getFnaDefValue(workflowid, addRowDefValue, user, defhrmid);
				detailmap.put("addRowDefValue", addRowDefValue);
			}
			
			apidatas.put(key, detailmap);
			//判断是否有延时转name的信息
			if(!isCreate && detailDelayConvertInfo.size() > 0){
				String delayCacheKey = "wfDetailDelayedConvert_"+this.requestid+"_"+user.getUID()+"_"+Util.null2String(params.get("apiResultCacheKey"));
				Util_TableMap.setObjValWithLog(LogType.NONE,"workflowcache","",delayCacheKey,detailDelayConvertInfo,180);
				apidatas.put("delayCacheKey", delayCacheKey);
			}
		}
		return apidatas;
	}

	/**
	 * 获取明细延时转换字段信息
	 */
	public Map<String,Object> generateDetailDelayData(){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		Map<String,Map<String,FieldValueBean>> datas = new HashMap<>();
		for(Map<String,Object> delayInfo : this.detailDelayConvertInfo){
			String rowMark = Util.null2String(delayInfo.get("rowMark"));
			String[] rowMarkArr = rowMark.split("\\|");
			String detailMark = rowMarkArr[0];
			String rowIndex = rowMarkArr[1];
			Map<String,FieldValueBean> fieldvaluemap = null;
			if(datas.containsKey(detailMark))
				fieldvaluemap = datas.get(detailMark);
			else{
				fieldvaluemap = new HashMap<>();
				datas.put(detailMark, fieldvaluemap);
			}
			FieldInfo fieldinfo = (FieldInfo)delayInfo.get("fieldinfo");
			String fieldvalue = Util.null2String(delayInfo.get("fieldvalue"));
			int detailtype_convert = Util.getIntValue(delayInfo.get("detailtype_convert")+"", -1);
			int detailRecordId = Util.getIntValue(delayInfo.get("detailRecordId")+"", -1);
			Map<String,Object> otherparams = new HashMap<>();
			otherparams.put("detailRecordId", detailRecordId);
			otherparams.put("cusDependFieldValues", delayInfo.get("cusDependFieldValues"));
			FieldValueBean bean = this.buildFieldValueBean(fieldinfo, fieldvalue, detailtype_convert, null, otherparams);
			fieldvaluemap.put("field"+fieldinfo.getFieldid()+"_"+rowIndex, bean);
		}
		apidatas.put("datas", datas);
		return apidatas;
	}
	
	/**
	 * 取新表单主表记录ID
	 */
	private int getBillMainId(){
		int billmainid = -1;
		if(isbill == 1){
			TableInfo maininfo = this.tableinfomap.get("main");
			RecordSet rs = new RecordSet();
			rs.executeSql("select id from "+maininfo.getTablename()+" where requestid="+this.requestid);
			if(rs.next())
				billmainid = Util.getIntValue(rs.getString(1));
		}
		return billmainid;
	}
	
	/**
	 * 获取字段默认值
	 */
	private String getFieldDefValue(FieldInfo fieldinfo, Hashtable inoperatefield_hs, Hashtable fieldvalue_hs,
			String defhrmid, String crmid, String reqid, String docid, String prjid) throws Exception{
		String defvalue = null;

		int fieldid = fieldinfo.getFieldid();
		String inoperatefield_tmp = Util.null2String((String) inoperatefield_hs.get("inoperatefield"+fieldid));
		if("1".equals(inoperatefield_tmp)){		//含节点前附加操作
			defvalue = Util.null2String((String) fieldvalue_hs.get("inoperatevalue"+fieldid));
			if(fieldinfo.getHtmltype() == 1 && fieldinfo.getDetailtype() == 5 && !"".equals(defvalue)){	// 金额 千分位
				int qfws = fieldinfo.getQfws();
				defvalue = this.milfloatFormat(defvalue, qfws);
			}
		}else{
			if (fieldinfo.getHtmltype() == 3) {
				int ismode = Util.getIntValue(Util.null2String(params.get("ismode")), 0);
				boolean existLayout = fieldinfo.isExistLayout();
				if(ismode == 0 && !existLayout ){
					defvalue = null;
				}else {
					//联动修改浏览框类型
					int detailtype_convert = 0;
					if(fieldinfo.getHtmltype() == 3 && fieldinfo.getBrowserattr() != null && fieldinfo.getBrowserattr().getTypeLinkageCfg() != null)
						detailtype_convert = this.changeBrowserDetailType4Create(fieldinfo);
					int detailtype = detailtype_convert > 0 ? detailtype_convert : fieldinfo.getDetailtype();
					if ((detailtype == 8 || detailtype == 135) && !prjid.equals("")) { // 浏览按钮为项目,从参数中获得项目默认值
						defvalue = "" + Util.getIntValue(prjid, 0);
					} else if ((detailtype == 9 || detailtype == 37) && !docid.equals("")) { // 浏览按钮为文档,从参数中获得文档默认值
						defvalue = "" + Util.getIntValue(docid, 0);
					} else if ((detailtype == 1 || detailtype == 17 || detailtype == 165 || detailtype == 166) && !defhrmid.equals("")) { 	// 浏览按钮为人,从参数中获得人默认值
						defvalue = "" + defhrmid;
					} else if ((detailtype == 7 || detailtype == 18) && !crmid.equals("")) { // 浏览按钮为CRM,从参数中获得CRM默认值
						defvalue = "" + Util.getIntValue(crmid, 0);
					} else if ((detailtype == 16 || detailtype == 152 || detailtype == 171) && !reqid.equals("")) { // 浏览按钮为REQ,从参数中获得REQ默认值
						defvalue = "" + Util.getIntValue(reqid, 0);
					} else if ((detailtype == 4 || detailtype == 57 || detailtype == 167 || detailtype == 168) && !defhrmid.equals("")) { // 浏览按钮为部门,从参数中获得人对应部门默认值
						defvalue = "" + Util.getIntValue(new ResourceComInfo().getDepartmentID(defhrmid), 0);
					} else if ((detailtype == 164 || detailtype == 169 || detailtype == 170 || detailtype == 194) && !defhrmid.equals("")) { // 浏览按钮为分部,从参数中获得人对应分部默认值
						defvalue = "" + Util.getIntValue(new ResourceComInfo().getSubCompanyID(defhrmid), 0);
					} else if ((detailtype == 24 || detailtype == 278) && !defhrmid.equals("")) { // 浏览按钮为职务,从参数中获得人对应职务默认值
						defvalue = "" + Util.getIntValue(new ResourceComInfo().getJobTitle(defhrmid), 0);
					} else if (detailtype == 32 && !defhrmid.equals("")) {
						defvalue = "" + Util.getIntValue(getRequestParam("TrainPlanId"), 0);
					} else if (detailtype == 2) {// 日期
						defvalue = TimeUtil.getCurrentDateString();
					} else if (detailtype == 19) {// 时间
						defvalue = TimeUtil.getCurrentTimeString().substring(11, 16);
					} else if (detailtype == 290) {// 日期时间
						defvalue = TimeUtil.getCurrentTimeString().substring(0, 16);
					} else if (detailtype == 178) {// 年份
						String currentdate_tmp = TimeUtil.getCurrentDateString();
						if (currentdate_tmp != null && currentdate_tmp.indexOf("-") >= 0)
							defvalue = currentdate_tmp.substring(0, currentdate_tmp.indexOf("-"));
					} else if (detailtype == 402) {// 年
						defvalue = TimeUtil.getCurrentDateString().substring(0, 4);
					} else if (detailtype == 403) {// 年月
						defvalue = TimeUtil.getCurrentDateString().substring(0, 7);
					}
				}


				if ("0".equals(defvalue) || "".equals(defvalue))
					defvalue = null;
			} else if(fieldinfo.getHtmltype() == 5) {	//选择框默认值
				defvalue = this.getSelectDefaultValue(fieldinfo);
			}
		}
		//流程创建URL传参赋值
		Map<String,String> urlfieldvalues = new HashMap<String,String>();
		if("1".equals(params.get("iscreate")+"")){
			if(fieldinfo.getIsdetail() == 0 && this.requestMap.containsKey(fieldinfo.getFieldname())){
				String urlDefault = this.getRequestParam(fieldinfo.getFieldname());
				if(!"".equals(urlDefault))
					defvalue = urlDefault;
			}
			if(fieldid > 0 && this.requestMap.containsKey("field"+fieldid)){
				String urlDefault = this.getRequestParam("field"+fieldid);
				if(!"".equals(urlDefault))
					defvalue = urlDefault;
			}
		}
		return defvalue;
	}

	private String getSelectDefaultValue(FieldInfo fieldinfo){
		if(fieldinfo.getHtmltype() != 5)
			return "";
		String defvalue = "";
		int detailtype = fieldinfo.getDetailtype();
		List<SelectItem> items = fieldinfo.getSelectattr().getSelectitemlist();
		for(SelectItem item : items){
			if(item.getIsdefault() == 1 && item.getCancel() == 0){
				if(detailtype == 2){	//复选框
					defvalue += "," + item.getSelectvalue();
				}else{
					defvalue = item.getSelectvalue()+"";
					break;
				}
			}
		}
		if(detailtype == 2 && defvalue.startsWith(","))
			defvalue = defvalue.substring(1);
		return defvalue;
	}

	//获取自定义浏览框转name依赖的其它表单字段值信息
	private Map<String,Object> generateCusDependFieldValues(RecordSet rs, FieldInfo fieldinfo, String dTableName){
		Map<String,Object> dependFieldValues = new HashMap<>();
		List<Map<String,String>> cusDependShip = fieldinfo.getBrowserattr().getCusDependShip();
		boolean isCalDetailField = fieldinfo.getIsdetail() == 1;
		if(cusDependShip != null && cusDependShip.size() > 0){
			for(Map<String,String> item: cusDependShip){
				boolean isDetail = "1".equals(item.get("isDetail"));
				String key = item.get("key");
				String fieldid = item.get("fieldid");
				String replaceStr = Util.null2String(isbill == 0 ? "detail" : dTableName);
				String fieldname = isDetail && !"".equals(replaceStr) ? key.replace(replaceStr+"_", "") : key;
				String fieldValue = "";
				if(isCalDetailField && !isDetail){	//明细字段取主字段依赖
					if(this.maindata.containsKey("field"+fieldid))
						fieldValue = this.maindata.get("field"+fieldid).getValue();
				}else{
					fieldValue = Util.null2String(rs.getString(fieldname));
				}
				dependFieldValues.put(key, fieldValue);
			}
		}
		return dependFieldValues;
	}
	
	/**
	 * 根据字段信息生成字段值对象
	 */
	private FieldValueBean buildFieldValueBean(FieldInfo fieldinfo, String fieldvalue){
		return this.buildFieldValueBean(fieldinfo, fieldvalue, 0,0);
	}
	private FieldValueBean buildFieldValueBean(FieldInfo fieldinfo, String fieldvalue, int detailtype_convert,int detailRecordId){
		Map<String,Object> otherparams = new HashMap<>();
		otherparams.put("detailRecordId", detailRecordId);
		return this.buildFieldValueBean(fieldinfo, fieldvalue, detailtype_convert, null, otherparams);
	}
	private FieldValueBean buildFieldValueBean(FieldInfo fieldinfo, String fieldvalue, int detailtype_convert, RecordSet rs, Map<String,Object> otherparams){
		String isReturnFieldValue = new WorkflowConfigComInfo().getValue("isReturnFieldValue");	//当字段不在模板上时是否返回值。
		boolean isReturn = true;	//默认当字段不在也返回。
		if (!"".equals(Util.null2String(isReturnFieldValue))) {
			String[] split = isReturnFieldValue.split(",");
			for (String s : split) {
				if (Util.getIntValue(s, -1) == workflowid) {
					isReturn = false;
				}
			}
		}
		boolean existLayout = fieldinfo.isExistLayout();		//判断字段是否在模板上。
		if (!existLayout && !isReturn) {	//如果不在模板上，而且这个流程配置的不在模板上不返回
			return  null;
		}
		boolean isMobileForm = "1".equals(getRequestParam("isMobileForm"));
		int detailRecordId = Util.getIntValue(otherparams.get("detailRecordId")+"");
		boolean delay = "1".equals(otherparams.get("delay"));
		String rowMark = otherparams.get("rowMark")+"";
		FieldValueBean bean = new FieldValueBean();
		int htmltype = fieldinfo.getHtmltype();
		int detailtype = fieldinfo.getDetailtype();
		if((htmltype == 1 && detailtype == 1) || htmltype == 2){
			fieldvalue = fieldvalue.replace("\n", "<br>");
			fieldvalue = CommonUtil.convertChar(fieldvalue);
			if(htmltype == 2 && detailtype == 2){	//富文本
				fieldvalue = fieldvalue.replaceAll("'{30,}","&#39;");
				fieldvalue = fieldvalue.replaceAll("(&#39;){30,}","&#39;");
				int isviewonly = Util.getIntValue(this.params.get("isviewonly")+"");
				if(fieldinfo.getViewattr() == 1 || isviewonly == 1){
					int layouttype = Util.getIntValue(Util.null2String(this.params.get("layouttype")), 0);
					if(layouttype != 1)
						fieldvalue = RequestFormBiz.manageImgLazyLoad(fieldvalue);	//图片懒加载处理
					fieldvalue = RequestFormBiz.manageCssPollute(fieldvalue);	//css污染处理
					if(isMobileForm)	//移动端富文本内容处理
						fieldvalue = MobileHtmlConvertBiz.translateMarkup(fieldvalue);
				}
			}else{
				fieldvalue = this.convertSpecialChar(fieldvalue, htmltype, detailtype);
			}
		} else if (htmltype == 3 && detailtype == 290) {
            //多时区需要转换为客户端时间显示  防止别的模块配置白名单，所以值直接在此处转换掉
            DateTransformer dft = new DateTransformer();
            BaseBean bb = new BaseBean();
            String timeZoneConversion = Util.null2String(bb.getPropValue("weaver_timezone_conversion","timeZoneConversion")).trim();
            if("1".equals(timeZoneConversion)){
                fieldvalue = dft.getLocaleDateTime(fieldvalue);
            }
        }
		bean.setValue(fieldvalue);
		if(htmltype == 3){
			boolean isCustomizeBrow = (detailtype == 161 || detailtype == 162);
			boolean isCustomizeTree = (detailtype == 256 || detailtype == 257);
			Map<String,Object> cusDependFieldValues = null;
			if(isCustomizeBrow){
				if(otherparams.containsKey("cusDependFieldValues"))
					cusDependFieldValues = (Map<String,Object>)otherparams.get("cusDependFieldValues");
				else if(rs != null)
					cusDependFieldValues = this.generateCusDependFieldValues(rs, fieldinfo, Util.null2String(otherparams.get("dTableName")));
			}
			BrowserValueInfoService browserValueInfoService  = new BrowserValueInfoService();
			browserValueInfoService.setParams(this.customizeBrowserValueCache);
			//处理一下浏览框中 fieldvalue 存在脏数据的情况(逗号开头或者结尾) --> ,1,2,3,4 || 1,2,3,4,
			bean.setValue(checkFieldvalue(fieldvalue));
			try {
				boolean convertName = false;
				if((isCustomizeBrow || isCustomizeTree) && !fieldinfo.isExistLayout()){		//性能优化，自定义浏览框未放置模板上不转name
				}else if(delay && !"".equals(fieldvalue)){	//性能优化，明细浏览按钮超过当前页的延时转换name对象,存储此方法的入参信息
					Map<String,Object> delayInfo = new HashMap<>();
					delayInfo.put("fieldinfo", fieldinfo);
					delayInfo.put("fieldvalue", fieldvalue);
					delayInfo.put("detailtype_convert", detailtype_convert);
					delayInfo.put("detailRecordId", detailRecordId);
					delayInfo.put("rowMark", rowMark);
					delayInfo.put("cusDependFieldValues", cusDependFieldValues);
					detailDelayConvertInfo.add(delayInfo);
				}else {
					convertName = true;
					detailtype_convert = detailtype_convert <= 0 ? detailtype : detailtype_convert;
					Map<String,Object> browserOtherParams = new HashMap<>();
					browserOtherParams.put("cusDependFieldValues", cusDependFieldValues);
					browserOtherParams.put("detailRecordId", detailRecordId);
					browserOtherParams.put("moduleName4Org", "workflow");
					List<Object> browserInfo = browserValueInfoService.getBrowserValueInfo(detailtype_convert, fieldinfo.getFielddbtype(), fieldinfo.getFieldid(), fieldvalue, user.getLanguage(), requestid, browserOtherParams);
					if (browserInfo.size() > 0)
						bean.setSpecialobj(browserInfo);
				}
				if(!convertName){
					//返回id值作为name，延时第二次请求再转成name
					List <Object> specialobj = new ArrayList <Object>();
					String[] valueArr = fieldvalue.split(",");
					for(String singleValue : valueArr){
						if(!"".equals(singleValue))
							specialobj.add(new BrowserValueInfo(singleValue, singleValue));
					}
					bean.setSpecialobj(specialobj);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
/*			if(detailtype == 16 || detailtype == 152){	//流程，多流程
				ServiceUtil.addRelatedWfSession(this.request, this.requestid, fieldvalue);
			}*/
		}else if(htmltype == 6){	//附件上传
			this.params.put("requestid", requestid);
			user.setLoginip("");
			this.params.put("user", user);
			bean.setValue(SaveFormDatasBiz.checkfileField(fieldvalue));   //处理不正常的附件字段值格式  如 ，1，，2，3，， 变为1，2，3  解决批量下载无权限
			bean.setSpecialobj(FileBiz.getFileFieldSpecialObj(fieldvalue,fieldinfo.getDetailtype(),this.params, user));
		} else if(htmltype == 9) {//位置字段
			bean.setSpecialobj(LocateUtil.toList(bean.getValue()));
		}
		return bean;
	}

	private String checkFieldvalue(String fieldvalue){
		if(fieldvalue.startsWith(",")){
			fieldvalue = fieldvalue.substring(1);
		}
		if(fieldvalue.endsWith(",")){
			fieldvalue = fieldvalue.substring(0,fieldvalue.length()-1);
		}
		return fieldvalue;
	}
	
	/**
	 * 表单字段值特殊字符转换，与前端formUtil.convertSpecialChar方法对应
	 * 都是将数据库值特殊字符转换成Redux存储值，提交时直接取Redux数据不再做字符转换
	 */
	private String convertSpecialChar(String str){
		return this.convertSpecialChar(str, 1, 1);
	}
	private String convertSpecialChar(String str, int htmltype, int detailtype){
		str = Util.null2String(str);
		str = str.replace("<br>", ""+'\n');
		str = str.replace("&nbsp;", " ");
		str = str.replace("&lt;", "<");
		str = str.replace("&gt;", ">");
		str = str.replace("&quot;", ""+'"');
		str = str.replace("&amp;", "&");
		return str;
	}
	
	/**
	 * 获取字段数据库存储值
	 */
	private String getFieldDBValue(RecordSet rs, FieldInfo fieldinfo, String tablename){
		String fieldvalue = "";
		//自定义浏览框不能trim去空格
		if(fieldinfo.getHtmltype() == 3 && (fieldinfo.getDetailtype() == 161 || fieldinfo.getDetailtype() == 162)){
			fieldvalue = Util.null2String(rs.getStringNoTrim(fieldinfo.getFieldname()));
			if("".equals(fieldvalue.trim()))
				fieldvalue = "";
		}else{
			int encryptType = this.calEncryptType(fieldinfo);
			if(encryptType == 1){	//非参与人查看加密字段，不自动解密，返回密文
				fieldvalue = rs.getString(tablename, fieldinfo.getFieldname(), false);
			}else if(encryptType == 2){		//字段可编辑属性，对脱敏字段返回明文
				//fieldvalue = rs.getString(tablename, fieldinfo.getFieldname(), true, true);
				fieldvalue = rs.getString(tablename, fieldinfo.getFieldname());
				fieldvalue = EncryptConfigBiz.getDecryptData(fieldvalue);
			}else{	//默认情况，加密的返回明文，脱敏的返回脱敏格式
				if (fieldinfo.getHtmltype() == 1&&fieldinfo.getDetailtype()==1){  //单行文本不trim去空格
					fieldvalue = rs.getStringNoTrim(tablename, fieldinfo.getFieldname());
				} else{
					fieldvalue = rs.getString(tablename, fieldinfo.getFieldname());
				}
			}
			fieldvalue = Util.null2String(fieldvalue);
			//流程存文档处理
			if("1".equals(params.get("fromWfToDoc")))
				fieldvalue = EncryptConfigBiz.forceFormatDecryptData(fieldvalue);
		}
		return fieldvalue;
	}

	/**
	 * 表单加载时，后台数据返获取格式
	 * 0:默认获取（加密数据默认明文，脱敏数据默认脱敏格式）
	 * 1：加密数据获取密文
	 * 2：脱敏数据获取明文
	 */
	private int calEncryptType(FieldInfo fieldinfo){
		if(fieldinfo.getHtmltype() != 1 && fieldinfo.getHtmltype() != 2)
			return 0;
		if("1".equals(params.get("fromWfToDoc")))
			return 0;
		EncryptFieldEntity encryptattr = fieldinfo.getEncryptattr();
		if(encryptattr == null)
			return 0;
		int isviewonly = Util.getIntValue(params.get("isviewonly")+"");
		int isSelfAuth = Util.getIntValue(params.get("isSelfAuth")+"", 0);
		boolean isEncrypt = "1".equals(encryptattr.getIsEncrypt());
		boolean isDesFormat = "1".equals(encryptattr.getDesensitization());
		if(isEncrypt && isSelfAuth != 1)
			return 1;
		if(isDesFormat && (isviewonly == 0 && fieldinfo.getViewattr() > 1)){
			return 2;
		}
		return 0;
	}
	
	/**
	 * 获取manager字段值
	 */
	private String getManagerFieldValue(){
		String value = "";
		try {
			int beluserid = user.getUID();
			if("1".equals(Util.null2String(params.get("isagent")))){	//代理判断
				beluserid = Util.getIntValue(Util.null2String(params.get("beagenter")));
			}
			if(user.getLogintype().equals("2")){
				CustomerInfoComInfo customerInfoComInfo = new CustomerInfoComInfo();
				value = Util.getIntValue(customerInfoComInfo.getCustomerInfomanager("" + beluserid), 0) + "";
	        }else{
	        	ResourceComInfo resourceComInfo = new ResourceComInfo();
	        	value = Util.getIntValue(resourceComInfo.getManagerID("" + beluserid), 0) + "";
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}
	
	/**
	 * 联动浏览框类型情况，需先根据配置、数据库值修改浏览框类型
	 */
	private int changeBrowserDetailType4Create(FieldInfo fieldinfo){
		Map<String,String> typeLinkageCfg = fieldinfo.getBrowserattr().getTypeLinkageCfg();
		String dependFieldid = typeLinkageCfg.get("dependid");
		if(dependFieldid != null && !"".equals(dependFieldid)){
			FieldInfo dependFieldInfo = null;
			TableInfo tableInfo = this.tableinfomap.get(fieldinfo.getIsdetail() == 1 ? "detail_"+fieldinfo.getGroupid() : "main");
			if(tableInfo != null && tableInfo.getFieldinfomap().containsKey(dependFieldid))
				dependFieldInfo = tableInfo.getFieldinfomap().get(dependFieldid);
			if(dependFieldInfo != null && dependFieldInfo.getHtmltype() == 5){	//财务依赖的选择框类型，计算选择框默认值
				String dependFieldvalue = this.getSelectDefaultValue(dependFieldInfo);
				if (!"".equals(dependFieldvalue) && typeLinkageCfg.containsKey("value_" + dependFieldvalue)) {
					int changeDetailType = Util.getIntValue(typeLinkageCfg.get("value_" + dependFieldvalue));
					if (changeDetailType > 0 && changeDetailType != 161 && changeDetailType != 162)
						return changeDetailType;
				}
			}
		}
		return 0;
	}
	private int changeBrowserDetailType(FieldInfo fieldinfo, Map<String,FieldInfo> fieldinfomap, RecordSet dbrecord){
		Map<String,String> typeLinkageCfg = fieldinfo.getBrowserattr().getTypeLinkageCfg();
		String dependFieldid = typeLinkageCfg.get("dependid");
		if(fieldinfomap.get(dependFieldid) != null) {
			String dependFieldname = fieldinfomap.get(dependFieldid).getFieldname();
			String dependFieldvalue = Util.null2String(dbrecord.getString(dependFieldname));
			if (!"".equals(dependFieldvalue) && typeLinkageCfg.containsKey("value_" + dependFieldvalue)) {
				int changeDetailType = Util.getIntValue(typeLinkageCfg.get("value_" + dependFieldvalue));
				if (changeDetailType > 0 && changeDetailType != 161 && changeDetailType != 162)
					return changeDetailType;
			}
		}
		return 0;
	}
	/**
     * 判断是否为车辆流程
     */
    private boolean isCarWorkflow(){
        boolean flag=false;
        RecordSet rs = new RecordSet();
        if(this.formid==163){
            flag=true;
        }else{
            rs.executeSql("select * from carbasic b where b.workflowid="+this.workflowid);
            if(rs.next()){
                flag=true;
            }
        }
        return flag;
    }
    /**
     * 车辆流程对开始日期时间 进行多时区转换
     */
    private Map<String,FieldValueBean> changeCarDateTime(Map<String,FieldValueBean> maindata){
        Map<String,String> fieldMap=new HashMap<String,String>();
        RecordSet recordSet = new RecordSet();
        String startDate= TimeUtil.getCurrentDateString();
        String startTime=TimeUtil.getCurrentTimeString().substring(11, 16);
        String endDate= TimeUtil.getCurrentDateString();
        String endTime=TimeUtil.getCurrentTimeString().substring(11, 16);
        if(formid==163){
            fieldMap.put("startDate","634"); //开始日期
            fieldMap.put("startTime","635"); //开始时间
            fieldMap.put("endDate","636"); //结束日期
            fieldMap.put("endTime","637"); //结束时间
            try {
				startDate=Util.null2String(maindata.get("field"+634).getValue());
				startTime=Util.null2String(maindata.get("field"+635).getValue());
				endDate=Util.null2String(maindata.get("field"+636).getValue());
				endTime=Util.null2String(maindata.get("field"+637).getValue());
			} catch (Exception e) {
				writeLog(e);
			}
        }else{
            recordSet.executeSql("select a.*,c.fieldname from mode_carrelatemode a,carbasic b,workflow_billfield c where a.mainid=b.id and  a.modefieldid=c.id and  b.workflowid="+workflowid);
            if(recordSet.getCounts()>=7){
                while(recordSet.next()){
                    String carfieldid = recordSet.getString("carfieldid");
                    String modefieldid = recordSet.getString("modefieldid");
                    String fieldname = recordSet.getString("fieldname");
                    String key = "";
                    if(carfieldid.equals("634")){//开始日期
                        key = "startDate";
                        startDate=Util.null2String(maindata.get("field"+modefieldid).getValue());
                    }else if(carfieldid.equals("635")){//开始时间
                        key = "startTime";
                        startTime=Util.null2String(maindata.get("field"+modefieldid).getValue());
                    }else if(carfieldid.equals("636")){//结束日期
                        key = "endDate";
                        endDate=Util.null2String(maindata.get("field"+modefieldid).getValue());
                    }else if(carfieldid.equals("637")){//结束时间
                        key = "endTime";
                        endTime=Util.null2String(maindata.get("field"+modefieldid).getValue());
                    }
                    fieldMap.put(key,modefieldid); 
               } 
            }
        }    
        startDate= !"".equals(startDate) ? CarDateTimeUtil.getLocaleDate(startDate,startTime) : startDate;
        startTime= !"".equals(startTime) ? CarDateTimeUtil.getShortLocaleTime(startDate,startTime) : startTime;
        endDate= !"".equals(endDate) ? CarDateTimeUtil.getLocaleDate(endDate,endTime) : endDate;
        endTime= !"".equals(endTime) ? CarDateTimeUtil.getShortLocaleTime(endDate,endTime) : endTime;
        for(Map.Entry<String, String> entry : fieldMap.entrySet()){
            String fieldid=entry.getValue();
            String key = "field"+fieldid;
            if (maindata.get(key)==null) {
				continue;
			}
            if(entry.getKey().equals("startDate")){//开始日期
                maindata.get(key).setValue(startDate);
            }else if(entry.getKey().equals("startTime")){//开始时间
                maindata.get(key).setValue(startTime);
            }else if(entry.getKey().equals("endDate")){//结束日期
                maindata.get(key).setValue(endDate);
            }else if(entry.getKey().equals("endTime")){//结束时间
                maindata.get(key).setValue(endTime);
            }
        }
        return maindata;
    }

	/* 转化为金额千分位 ， value 转换前的 值 ， qfws 千分位保存 小数点位数  */
	private  String milfloatFormat(String value, int qfws){
		String result = value;
		if(Utils.isNumeric(value)){  // 此时为数字，转为千分位存储
			String pattern = ",###,##0.";
			if(qfws <=0)
				qfws = 2;
			for(int i=0;i<qfws;i++){
				pattern += "0";
			}
			BigDecimal tmpvalue =  new BigDecimal(value);
			DecimalFormat df = new DecimalFormat(pattern);
			result =  df.format(tmpvalue);
		}
		return result;
	}
}
