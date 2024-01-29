package com.engine.workflow.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.api.workflow.service.ScriptManagerService;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.customization.qc2243476.KqUtil;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.util.ParamUtil;
import com.engine.core.impl.Service;
import com.engine.fna.util.DateUtil;
import com.engine.msgcenter.util.ValveConfigManager;
import com.engine.encrypt.biz.WfEncryptBiz;
import com.engine.workflow.biz.excelDesign.DoSaveFreeExcelDesignBiz;
import com.engine.workflow.biz.excelDesign.ExcelSecurityBiz;
import com.engine.workflow.biz.freeNode.FreeNodeBiz;
import com.engine.workflow.biz.requestForm.LayoutInfoBiz;
import com.engine.workflow.cmd.requestForm.RequestDelVerifyCmd;
import com.engine.workflow.biz.requestForm.RequestFormBiz;
import com.engine.workflow.cmd.requestForm.*;
import com.engine.workflow.cmd.requestForm.freeNodeCustomForm.GetFreeNodeBrowserItemCmd;
import com.engine.workflow.cmd.requestForm.freeNodeCustomForm.SaveFreeNodeFormConfigCmd;
import com.engine.workflow.cmd.requestForm.remind.GetRemindDataCmd;
import com.engine.workflow.cmd.workflowSetting.GetDetailExpSetCmd;
import com.engine.workflow.cmd.workflowSetting.SaveDetailExpSetCmd;
import com.engine.workflow.cmd.workflowTemplate.GetThMouldListCmd;
import com.engine.workflow.cmd.workflowTemplate.GetThPreviewUrlCmd;
import com.engine.workflow.entity.requestForm.AutoApproveEntity;
import com.engine.workflow.entity.requestForm.TableInfo;
import com.engine.workflow.service.RequestFormService;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.request.RequestManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jhy on 2018/2/23.
 */
public class RequestFormServiceImpl extends Service implements RequestFormService {

	
	//流程测试选择人员后判断用户是否有创建权限
	public Map<String,Object> judgeCreateRight(HttpServletRequest request){
		return commandExecutor.execute(new LoadParamCmd(request, user));
	}
	//加载表单基础信息，权限参数及表单信息
    public String loadForm(HttpServletRequest request) {
	    Boolean result = false;
	    if("".equals(Util.null2String(request.getParameter("f_weaver_belongto_userid")))){
            if (!user.isAdmin()){
                //==zj 先判断是不是考勤流程表单
                KqUtil kqUtil = new KqUtil();
                String requestid = Util.null2String(request.getParameter("requestid"));
                String workflowid = Util.null2String(request.getParameter("workflowid"));
                new BaseBean().writeLog("==zj==(loadForm-requestid)" + requestid);
                if (!"".equals(requestid)){
                    //如果不是新建考勤流程会有requestid
                     result =   kqUtil.checkKqFlowReq(requestid);
                     new BaseBean().writeLog("==zj==(非新建流程是否来自考勤流程-loadForm)" +result);
                }
                if (!"".equals(workflowid)){
                    //如果是新建流程有workflowid
                    result = kqUtil.checkKqFlow(workflowid);
                    new BaseBean().writeLog("==zj==(新建流程是否来自考勤流程-loadForm)" +result);
                }
                if (result){
                    //==zj  判断主账号有没有考勤组
                    String userId = Util.null2String(user.getUID());  //获取主账号userid
                    String today = DateUtil.getCurrentDate();         //获取当前日期

                    Boolean flag = kqUtil.mainAccountCheck(userId, today);
                    new BaseBean().writeLog("==zj==(考勤流程-loadform)" + flag);

                    if (!flag){
                        //如果主账号没有考勤组，再看子账号是否有考勤组
                        List<Map> list = kqUtil.childAccountCheck(userId, today);
                        new BaseBean().writeLog("==zj==(loadform-考勤流程表单加载-maps)" + JSON.toJSONString(list));
                        if (list.size() == 1){
                            new BaseBean().writeLog("==zj==(loadfrom-检测考勤组)");
                            //如果只有一个生效考勤组
                            Map<String,String> map = list.get(0);
                            int userid = Integer.parseInt(map.get("userid"));
                            User user = new User(userid);
                            this.user = user;
                        }
                    }
                }
            }
        }


        Map<String, Object> apidatas = new HashMap<String, Object>();
        long start = System.currentTimeMillis();
        int userid = user.getUID();
        new BaseBean().writeLog("==zj==(加载考勤流程表单人员id)" + userid);
        String requestid = Util.null2String(request.getParameter("requestid"));
        try {
            //点击列表预加载，当表单请求到时直接返回
            boolean ispreload = Util.getIntValue(request.getParameter("ispreload")) == 1;
            boolean isEm = Util.null2String(request.getHeader("user-agent")).indexOf("E-Mobile") > -1;
            String preloadkey = isEm ? "" : Util.null2String(request.getParameter("preloadkey"));
            //获取预加载的内容的token
            String preloadValKey = preloadkey;
            if (!"".equals(preloadkey)) {
                preloadkey = userid + "_" + preloadkey;
                preloadValKey = preloadkey + "_val";
                if (ispreload) {
                    Util_TableMap.setVal(preloadkey, "loading");
                } else {
                    //-----------------------------------------------------------------------------
                    // 如果有预加载活动。
                    // 注：预加载的内容只能使用一次，当前线程获取预加载的内容后，其他线程需要按照正常逻辑加载。
                    //-----------------------------------------------------------------------------
                    if (Util_TableMap.containsKey(preloadkey)) {
                        //清除预加载标志
                        Util_TableMap.clearVal(preloadkey);
                        int i = 0;
                        //预加载的内容，如果还没有加载好， 线程等待5s，5s后还未加载完成， 则按照正常逻辑加载。
                        String cacheVal = null;
                        while (i < 100) {
                            cacheVal = Util_TableMap.getVal(preloadValKey);
                            if (cacheVal != null) {
                                Util_TableMap.clearVal(preloadValKey);
                                return cacheVal;
                            }
                            i++;
                            Thread.sleep(50);
                        }
                    }
                }
            }
            //第一步：权限加载判断、获取基础信息参数
            Map<String, Object> competenceInfo = commandExecutor.execute(new LoadParamCmd(request, user));
            apidatas.putAll(competenceInfo);
            Map<String, Object> params = (Map<String, Object>) competenceInfo.get("params");
            boolean iscreate = "1".equals(params.get("iscreate"));
            if(!iscreate){
                int creater = Util.getIntValue(Util.null2String(params.get("creater")));
                int currentnodetype = Util.getIntValue(Util.null2String(params.get("currentnodetype")));
                if(creater == user.getUID() && currentnodetype == 0 && this.isOnlyCreate(requestid)){
                    //创建人-创建节点不校验
                }else{
                    //校验节点打开表单是否需要二次身份认证
                    boolean needSecondAuth = new WfEncryptBiz().judgeNeedSecondAuth(params);
                    boolean hasVerifyAuth = "1".equals(request.getParameter("hasVerifyAuth"));
                    if(needSecondAuth && !hasVerifyAuth){
                        apidatas.put("needSecondAuth", true);
                        return JSON.toJSONString(apidatas);
                    }
                    boolean verifySecondAuthToken = false;
                    try{
                        //可能会异常，影响表单加载，做容错处理
                        verifySecondAuthToken = new EncryptConfigBiz().checkSecondAuthToken("WORKFLOW", "NODE", params, request);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    if(!verifySecondAuthToken){
                        apidatas.put("verifySecondAuthToken", false);
                        return JSON.toJSONString(apidatas);
                    }
                }
            }

            String retstr = "";
            if (!(Boolean) params.get("verifyRight")) {    //无权限直接返回
                retstr = JSON.toJSONString(apidatas);
            } else {
                int ismode = Util.getIntValue(Util.null2String(params.get("ismode")), 0);
                int layoutversion = Util.getIntValue(Util.null2String(params.get("layoutversion")), 0);
                //生成普通模式模板布局信息
                Map<String, Object> commonLayout = new HashMap<String, Object>();
                if (ismode == 0) {
                    commonLayout = commandExecutor.execute(new GenerateCommonLayoutCmd(params, user));
                }

                Map<String, Object>  requestMap = ParamUtil.request2Map(request);
                //第二步：加载表单信息，包括字段信息、明细信息等
                Map<String, Object> forminfo = commandExecutor.execute(new FormInfoCmd(requestMap, user, params, commonLayout));
                apidatas.putAll(forminfo);

                //第三步：加载联动配置信息
                apidatas.put("linkageCfg", commandExecutor.execute(new LinkageCfgCmd(params, user)));
                apidatas.put("propFileCfg", this.getFormPropFileCfg());

                //第四步：加载主表数据
                Map<String, Object> maindata = commandExecutor.execute(new FormDataCmd(requestMap, user, params, (Map<String,TableInfo>)forminfo.get("tableInfo")));
                apidatas.put("maindata", maindata.get("datas"));

                //第五步：模板布局datajson单独以字符串方式拼串，优化性能
                String apidatastr = JSON.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
                if (ismode == 0 || (ismode == 2 && layoutversion == 2)) {
                    String layoutStr = "";
                    if (ismode == 0) {    //普通模式
                        layoutStr = Util.null2String(commonLayout.get("layoutStr"));
                    } else if (ismode == 2) {    //html模式
                        int layoutid = Util.getIntValue(Util.null2String(params.get("modeid")), 0);
                        int nodeid = Util.getIntValue(Util.null2String(params.get("nodeid")), 0);
                        int workflowid = Util.getIntValue(Util.null2String(params.get("workflowid")), 0);
                        String configid = Util.null2String(params.get("layoutconfigid"));
                        boolean isnewFreelayout = DoSaveFreeExcelDesignBiz.isNewFreeLayout(configid);
                        boolean isSimpleFreeFlow = FreeNodeBiz.judgeNodeSimpleFreeFlow(workflowid, nodeid);
                        if (isnewFreelayout) {
                            DoSaveFreeExcelDesignBiz doSaveFreeExcelDesignBiz = new DoSaveFreeExcelDesignBiz(user);
                            if(isSimpleFreeFlow){
                                nodeid = FreeNodeBiz.getExtendNodeId(nodeid);
                                layoutStr = doSaveFreeExcelDesignBiz.getLayoutStr(layoutid, nodeid);
                            }else {
                                layoutStr = doSaveFreeExcelDesignBiz.getLayoutStr(layoutid, nodeid);
                            }

                        } else {
                            layoutStr = new LayoutInfoBiz().getLayoutStr(layoutid);
                        }
                    }
                    apidatastr = apidatastr.substring(0, apidatastr.lastIndexOf("}"));
                    retstr = apidatastr + ",\"datajson\":" + layoutStr + "}";
                } else {
                    retstr = apidatastr;
                }
            }
            //预加载结果放入容器
            if (ispreload && !"".equals(preloadkey)) {
                Util_TableMap.setVal(preloadValKey, retstr);
            }
            return retstr;
        } catch (Exception e) {
            new weaver.general.BaseBean().writeLog("requestid" + requestid + "---errorMsg----" + e.toString());
            e.printStackTrace();
            apidatas.put("api_status", false);
            apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
        }
        return JSON.toJSONString(apidatas);
    }

    //加载预览模板所需信息
    public String loadPreView(Map<String,Object> params) {
	    int nodeid = Util.getIntValue(params.get("nodeid")+"");
        Map<String, Object> apidatas = new HashMap<>();
        //第一步：模拟拼接生成commonParam参数
        Map<String,Object> reqParams = new HashMap<>();
        reqParams.put("isPreView", 1);
        Map<String,Object> commonParam = new HashMap<>();
        commonParam.put("workflowid", Util.getIntValue(params.get("wfid")+""));
        commonParam.put("nodeid", nodeid);
        commonParam.put("formid", Util.getIntValue(params.get("formid")+""));
        commonParam.put("isbill", Util.getIntValue(params.get("isbill")+""));
        commonParam.put("ismode", 2);
        commonParam.put("modeid", Util.getIntValue(params.get("layoutid")+""));
        commonParam.put("layoutversion", 2);
        commonParam.put("layouttype", Util.getIntValue(params.get("layouttype")+""));
        commonParam.put("margin", RequestFormBiz.generateLayoutMargin(nodeid));
        Map<String,Object> layoutInfo = new HashMap<>();
        String layoutStr = Util.null2String(params.get("datajson"));
        layoutInfo.put("layoutStr", layoutStr);
        //取代码块中的样式部分
        String scripts = Util.null2String(params.get("scripts"));
        String initscripts = Util.null2String(params.get("initscripts"));
        ExcelSecurityBiz excelSecurityBiz = new ExcelSecurityBiz();
        Map<String,String> splitResult = new ScriptManagerService().splitScript(excelSecurityBiz.decode(scripts));
        String styleStr = Util.null2String(splitResult.get("stylestr"));
        if(!"".equals(initscripts)){
            initscripts = excelSecurityBiz.decode(initscripts);
            String contextPath = Util.null2String(GCONST.getContextPath());
            if(!"".equals(contextPath))		//系统样式追加全路径
                initscripts = initscripts.replaceAll("(?<!"+contextPath+")/workflow/exceldesign/", contextPath+"/workflow/exceldesign/");
            styleStr += initscripts;
        }
        layoutInfo.put("styleStr", styleStr);
        apidatas.put("params", commonParam);
        //第二步：加载表单信息，包括字段信息、明细信息等
        Map<String, Object> forminfo = commandExecutor.execute(new FormInfoCmd(reqParams, user, commonParam, layoutInfo));
        apidatas.putAll(forminfo);
        //第三步：加载联动配置信息
        apidatas.put("linkageCfg", commandExecutor.execute(new LinkageCfgCmd(params, user)));
        apidatas.put("propFileCfg", this.getFormPropFileCfg());
        //返回string格式
        String apidatastr = JSON.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
        String result = apidatastr.substring(0, apidatastr.lastIndexOf("}")) + ",\"datajson\":" + layoutStr + "}";
        return result;
    }

    //获取相关配置文件配置项
    private Map<String, Object> getFormPropFileCfg() {
	    RecordSet rs = new RecordSet();
	    Map<String,Object> props = new HashMap<String,Object>();
	    rs.executeQuery("select name,value from workflow_config where type='form'");
	    while(rs.next()){
            props.put(rs.getString("name"), rs.getString("value"));
        }
        String prohibitDownload = ValveConfigManager.getTypeValve("prohibitDownload", "prohibitDownloadSwatch", "0");
        props.put("prohibitDownload", prohibitDownload);
        return props;
    }
	
	private boolean isOnlyCreate(String requestid){
        RecordSet rs = new RecordSet();
        rs.executeQuery("select count(*) as count from workflow_currentoperator where requestid=?", requestid);
        return rs.next() && Util.getIntValue(rs.getString("count")) <= 1;
    }

    public Map<String, Object> loadDetailData(Map<String,Object> params) {
        return commandExecutor.execute(new FormDataCmd(params, user));
    }

    public Map<String, Object> saveDetailPaging(Map<String,Object> params) {
	    return commandExecutor.execute(new SaveDetailPagingCmd(params, user));
    }

    public Map<String, Object> copyCustomPageFile(String custompage) {
        return commandExecutor.execute(new CopyCustomPageCmd(custompage));
    }

    public Map<String, Object> updateReqInfo(HttpServletRequest request) {
        return commandExecutor.execute(new UpdateReqInfoCmd(request, user));
    }

    public Map<String, Object> getLinkageResult(HttpServletRequest request, String type) {
        if ("dataInput".equals(type))
            return commandExecutor.execute(new LinkageDataInputCmd(request, user));
        else if ("fieldSql".equals(type))
            return commandExecutor.execute(new LinkageFieldSqlCmd(request, user));
        else if ("dateTime".equals(type))
            return commandExecutor.execute(new LinkageDateTimeCmd(request, user));
        else
            return null;
    }

    @Override
    public Map<String, Object> getRightMenu(HttpServletRequest request,Map<String, Object> params) {
        return commandExecutor.execute(new GetRightMenuCmd(request,params, user));
    }

    public Map<String, Object> getStatusData(Map<String, Object> params) {
        return commandExecutor.execute(new StatusDataCmd(params, user));
    }

    public Map<String, Object> getStatusCount(Map<String, Object> params) {
        return commandExecutor.execute(new StatusCountCmd(params, user));
    }

    public Map<String, Object> getResourcesKey(Map<String, Object> params) {
        return commandExecutor.execute(new ResourcesKeyCmd(params, user));
    }

    public Map<String, Object> getModifyLog(HttpServletRequest request) {
        return commandExecutor.execute(new ModifyLogCmd(request, user));
    }

    @Override
    public Map<String, Object> requestBatchSubmit(HttpServletRequest request) {
        return commandExecutor.execute(new BatchSubmitCmd(request, user));
    }

    public Map<String, Object> judgeRejectWay(HttpServletRequest request) {
        return commandExecutor.execute(new JudgeRejectWayCmd(request, user));
    }

    public Map<String, Object> getRejectOption(Map<String, Object> params) {
        return commandExecutor.execute(new GetRejectOptionCmd(params, user));
    }

    public Map<String, Object> requestSubmit(HttpServletRequest request) {
        return commandExecutor.execute(new RequestSubmitCmd(request, user));
    }

    public Map<String, Object> forwardSubmit(HttpServletRequest request) {
        return commandExecutor.execute(new ForwardSubmitCmd(request, user));
    }

    public Map<String, Object> requestWithdraw(HttpServletRequest request) {
        return commandExecutor.execute(new RequestWithdrawCmd(request, user));
    }

    public Map<String, Object> remarkSubmit(HttpServletRequest request) {
        return commandExecutor.execute(new RemarkSubmitCmd(request, user));
    }

    public Map<String, Object> functionManage(HttpServletRequest request, HttpServletResponse response) {
        return commandExecutor.execute(new FunctionManageCmd(request, response, user));
    }

    public Map<String, Object> triggerSubWf(HttpServletRequest request) {
        return commandExecutor.execute(new TriggerSubWfCmd(request, user));
    }

    public Map<String, Object> uploadFile(HttpServletRequest request, HttpServletResponse response) {
        return commandExecutor.execute(new FileUploadCmd(request, response, user));
    }

    public Map<String, Object> getFileFieldObj(Map<String, Object> params) {
        return commandExecutor.execute(new FileFieldObjCmd(params));

    }

    @Override
    public Map<String, Object> createWfCode(Map<String, Object> params) {
        return commandExecutor.execute(new CreateWfCodeCmd(params, user));
    }

    @Override
    public Map<String, Object> getWfCodeFieldValue(Map<String, Object> params) {
        return commandExecutor.execute(new GetWfCodeFieldValueCmd(params, user));
    }

    @Override
    public Map<String, Object> requestImport(Map<String, Object> params) {
        return commandExecutor.execute(new RequestImportCmd(params, user));
    }

    @Override
    public Map<String, Object> requestDetailImport(HttpServletRequest request) {
        return commandExecutor.execute(new RequestDetailImportCmd(request, user));
    }

    @Override
    public Map<String, Object> chooseExceptionOperator(Map<String, Object> params) {
        return commandExecutor.execute(new ChooseExceptionOperatorCmd(params, user));
    }

    @Override
    public Map<String, Object> overTimeSetting(HttpServletRequest request) {
        return commandExecutor.execute(new OverTimeSettingCmd(request, user));
    }
	
	@Override
	public Map<String,Object> getPrintLogBase(Map<String,Object> params){
		return commandExecutor.execute(new GetPrintLogBaseCmd(params,user));
	}
    
	@Override
    public Map<String,Object> getPrintLogData(Map<String,Object> params){
		return commandExecutor.execute(new GetPrintLogDataCmd(params,user));
    }
	
	@Override
	public Map<String,Object> freeFlowRead(Map<String,Object> params){
		return commandExecutor.execute(new FreeFlowReadCmd(params,user));
	}
    
	@Override
    public Map<String,Object> freeFlowSave(Map<String,Object> params){
    	return commandExecutor.execute(new FreeFlowSaveCmd(params,user));
    }
	
	@Override
	public Map<String,Object> editLockOper(Map<String,Object> params){
		return commandExecutor.execute(new EditLockOperCmd(params,user));
	}
	
	@Override
	public Map<String,Object> loadPrintTemplates(Map<String,Object> params){
		return commandExecutor.execute(new LoadPrintTemplatesCmd(params,user));
	}
	
	@Override
	public Map<String,Object> doEvalExpression(Map<String,Object> params){
		return commandExecutor.execute(new DoEvalExpressionCmd(params,user));
	}

	@Override
	public Map<String,Object> generatePrintLog(HttpServletRequest request){
		return commandExecutor.execute(new GeneratePrintLogCmd(request,user));
	}

	@Override
    public Map<String,Object> getForwardDatas(Map<String,Object> params){
	    return commandExecutor.execute(new GetForwardDatasCmd(params,user));
    }

    @Override
    public Map<String,Object> doBack(Map<String,Object> params){
        return commandExecutor.execute(new DoForwardBackCmd(params,user));
    }

    @Override
    public Map<String,Object> getFormTab(Map<String,Object> params){
        return commandExecutor.execute(new GetFormTabCmd(params,user));
    }

    /**
     * 获取指定下一节点流转的condition
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> getSelectNextFlowCondition(Map<String, Object> params) {
        return commandExecutor.execute(new GetSelectNextFlowConditionCmd(params,user));
    }

    /**
     * 获取指定下一节点流转的节点信息
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> getSelectNextFlowNodes(Map<String, Object> params) {
        return commandExecutor.execute(new GetSelectFlowNodesCmd(params,user));
    }

    @Override
    public Map<String,Object> getRemindData(Map<String,Object> params){
        return commandExecutor.execute(new GetRemindDataCmd(user,params));
    }

    @Override
    public Map<String,Object> judgeRequestIsValid(Map<String,Object> params){
        return commandExecutor.execute(new JudgeRequestIsValidCmd(params, user));
    }
	
	@Override
    public Map<String,Object> takeBack(Map<String,Object> params){
        return commandExecutor.execute(new DoTakeBackCmd(params, user));
    }

    @Override
    public Map<String, Object> getCustomOperation(Map<String,Object> params) {
        return commandExecutor.execute(new GetCustomOperationCmd(params, user));
    }

    @Override
    public Map<String, Object> runCustomOperationAction(Map<String,Object> params) {
        return commandExecutor.execute(new GetCustomOperationActionCmd(params, user));
    }

    @Override
    public Map <String, Object> JudgeAutoApprove(AutoApproveEntity autoApproveEntity) {
        return commandExecutor.execute(new JudgeAutoApproveCmd(autoApproveEntity,user));
    }

    @Override
    public Map <String, Object> delApproveLog(AutoApproveEntity autoApproveEntity) {
        return commandExecutor.execute(new DelApproveLogCmd(autoApproveEntity,user));
    }

    @Override
    public Map <String, Object> verifyRequestForView(Map <String, Object> params) {
        return commandExecutor.execute(new VerifyRequestForViewCmd(params,user));
    }

    /**
     * 结束意见征询
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> doTakEnd(HttpServletRequest request, Map<String, Object> params) {
        return commandExecutor.execute(new DoTakEndCmd(params,user));
    }

    @Override
    public Map<String, Object> importFieldLinkageCfg(Map<String, Object> params) {
        return commandExecutor.execute(new RequestDetailImportFieldLinkageCmd(params,user));
    }

    @Override
    public Map<String, Object> exportFieldSet(Map<String, Object> params) {
        return commandExecutor.execute(new RequestDetailExportFieldTypeCmd(params,user));
    }

    /**
     * 合规校验
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> conformCheck(Map<String, Object> params) {
        return commandExecutor.execute(new ConformCheckCmd(params,user));
    }

    @Override
    public Map<String, Object> getDetailByRequestId(Map<String, Object> params) {
        return commandExecutor.execute(new GetSubReuqestDetailCmd(params,user));
    }

    @Override
    public Map<String, Object> getFreeNodeBrowserItem(Map<String, Object> params) {
        return commandExecutor.execute(new GetFreeNodeBrowserItemCmd(params,user));
    }

    @Override
    public Map<String, Object> saveFreeNodeFormConfig(Map<String, Object> params) {
        return commandExecutor.execute(new SaveFreeNodeFormConfigCmd(params,user));
    }

    @Override
    public Map<String, Object> getNextNodeContent(Map<String, Object> params) {
        return commandExecutor.execute(new GetNextNodeContentCmd(params,user));
    }


    @Override
    public Map<String, Object> delRequestVerify(Map<String,Object> params) {
        return commandExecutor.execute(new RequestDelVerifyCmd(params,user));
    }

    @Override
    public Map<String, Object> importFormulaCfg(Map<String, Object> params) {
        return commandExecutor.execute(new RequestDetailImportFormulaCmd(params,user));
    }

    /**
     * 获取明细表数据keyid
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> getDetailDataKeyId(Map<String, Object> params) {
        return commandExecutor.execute(new GetDetailKeyIdCmd(params,user));
    }

    @Override
    public Map <String, Object> judgeWorkflowPenetrate(AutoApproveEntity autoApproveEntity, RequestManager requestManager) {
        return commandExecutor.execute(new JudgeWorkflowPenetrateCmd(autoApproveEntity,user, requestManager));
    }

	@Override
    public Map <String, Object> getThPreviewUrl(Map <String, Object> params) {
        return commandExecutor.execute(new GetThPreviewUrlCmd(params,user));
    }

    @Override
    public Map <String, Object> getThMouldList(Map <String, Object> params) {
        return commandExecutor.execute(new GetThMouldListCmd(params,user));
    }

    @Override
    public Map<String, Object> getDetailExpSet(Map<String, Object> params) {
        return commandExecutor.execute(new GetDetailExpSetCmd(params,user));
    }

    @Override
    public Map<String, Object> saveDetailExpSet(Map<String, Object> params) {
        return commandExecutor.execute(new SaveDetailExpSetCmd(params,user));
    }


}
