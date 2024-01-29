package com.engine.odoc.cmd.officialSettings.createDocByWf;


import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Odoc;
import com.engine.common.constant.BizLogType;
import com.engine.common.constant.ParamConstant;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.core.interceptor.CommandContext;
import com.engine.odoc.biz.odocSettings.OdocSettingBiz;
import com.engine.odoc.entity.createDoc.WorkflowCreateDoc;
import com.engine.odoc.util.OdocRequestdocUtil;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.BeanUtils;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.orm.util.OrmUtil;
import weaver.workflow.workflow.WfRightManager;
import weaver.workflow.workflow.WorkflowComInfo;

import java.util.Date;
import java.util.Map;

/**
 * @author suijp
 * @Date 2018/3/29
 * @Description: 保存SignatureNode逻辑
 */
public class OdocSaveBaseInfoSettingCmd  extends AbstractCommonCommand<Map<String, Object>> {



	private WorkflowCreateDoc wcd;

	public OdocSaveBaseInfoSettingCmd(WorkflowCreateDoc wcd) {
		this.wcd = wcd;
	}
	BizLogContext bizLogContext = new BizLogContext();

	@Override
	public BizLogContext getLogContext() {
		bizLogContext.setDateObject(new Date());
		bizLogContext.setUserid(user.getUID());
		bizLogContext.setUsertype(Util.getIntValue(user.getLogintype()));
		bizLogContext.setTargetId(params.get("workflowId") + "");
		bizLogContext.setLogType(BizLogType.ODOC_ENGINE);
		bizLogContext.setLogSmallType(BizLogSmallType4Odoc.ODOC_ENGINE_CREATEDOCBYWF_BASEINFO);
		if(wcd.getId() != null && wcd.getId() > 0){
			bizLogContext.setOperateType(BizLogOperateType.UPDATE);
		}else{
			bizLogContext.setOperateType(BizLogOperateType.ADD);
		}
		bizLogContext.setClientIp(Util.null2String(params.get(ParamConstant.PARAM_IP)));
		bizLogContext.setParams(params);
		return bizLogContext;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {


		Map<String, Object> apidatas = Maps.newHashMap();

		int workflowId = Util.getIntValue(Util.null2String(params.get("workflowId")));
		boolean haspermission = new WfRightManager().hasPermission3(workflowId, 0, user, WfRightManager.OPERATION_CREATEDIR);
		if (!HrmUserVarify.checkUserRight("WorkflowManage:All", user) && !haspermission) {
			// 无权限
			apidatas.put("sessionkey_state", "noright");
			return apidatas;
		}
		try {
			RecordSet rs = new RecordSet();
			rs.executeQuery("select id from workflow_Createdoc where workflowid = ?",wcd.getWorkflowid());
			if(rs.next()){
				wcd.setId(rs.getInt(1));
			}
			WorkflowCreateDoc oldWorkflowCreateDoc = new WorkflowCreateDoc();
			WorkflowComInfo WorkflowComInfo = new WorkflowComInfo();
			boolean flag = false;
			StringBuffer sb = new StringBuffer();
			
			if(wcd.getId() != null && wcd.getId() > 0){
                		oldWorkflowCreateDoc = (WorkflowCreateDoc) OrmUtil.selectObjByPrimaryKey(WorkflowCreateDoc.class, wcd.getId());
				
				sb.append("update workflow_Createdoc set ")
				.append(" flowDocField = ?,")
				.append(" documentTitleField = ?,")
				.append(" defaultView = ?,")
				.append(" flowDocCatField = ?,")
				.append(" newTextNodes = ?,")
				.append(" uploadPDF = ?,")
				.append(" uploadOFD = ?,")
				.append(" uploadWORD = ?,")
				.append(" isWorkflowDraft = ?,")
				.append(" extfile2doc = ?,")
				.append(" isTextInForm = ?,")
				.append(" isTextInFormNodeSelect = ?,")
				.append(" isTextInformNode = ?,")

				.append(" isTextInFormCanEdit = ?,")
				.append(" iscolumnshow = ?,")
				.append(" documentTextPosition = ?,")
				.append(" documentTextProportion = ?,")
				.append(" defaultDocType = ?,")
				.append(" openTextDefaultSelect = ?,")
				.append(" openTextDefaultNode = ?,")
				.append(" cleanCopyNodeSelect = ?,")
				.append(" cleanCopyNodes = ?,")
				.append(" printNodeSelect = ?,")
				.append(" printNodes = ?,")
				.append(" iscompellentmark = ?,")
				.append(" iscancelcheck = ?,")
				.append(" isHideTheTraces = ?,")
				.append(" status = ?,")
				.append(" autoCleanCopy = ?,")
				.append(" isSaveTraceByClean = ?,")
				.append(" flowattachfiled = ?,")
				.append(" SyncTitleNodes = ?,")
				.append(" reUploadText = ?,")
				.append(" reSelectMould = ?,")
				.append(" useYozoOrWPS = ?,")
				.append(" displayTab = ?,")
				.append(" saveBackForm = ?,")
				.append(" saveDocRemind = ?,")
				.append(" useIwebOfficeNodes = ?,")
				.append(" wfAttachKeep = ?,")
				.append(" preFinishDocNodes = ?,")
				.append(" isOpenDocumentCompare = ?,")
				.append(" oldDocumentType = ?,")
				.append(" oldDocumentValue = ?,")
				.append(" compareDocumentType = ?,")
				.append(" compareDocumentValue = ?,")
				.append(" isShowAttachmentInForm = ?,")
				.append(" ofdReaderNodeSelect = ?,")
				.append(" ofdReaderDefaultNode = ?,")
				.append(" changeTextNodeSelect = ?,")
				.append(" changeTextNodes = ?")
				.append(" where workflowid =  ? ");
			}else{
				sb.append("insert into workflow_Createdoc ( ")
				.append(" flowDocField,")
				.append(" documentTitleField,")
				.append(" defaultView,")
				.append(" flowDocCatField,")
				.append(" newTextNodes,")
				.append(" uploadPDF,")
				.append(" uploadOFD,")
				.append(" uploadWORD,")
				.append(" isWorkflowDraft,")
				.append(" extfile2doc,")
				.append(" isTextInForm,")
				.append(" isTextInFormNodeSelect,")
				.append(" isTextInformNode,")
				.append(" isTextInFormCanEdit,")
				.append(" iscolumnshow,")
				.append(" documentTextPosition,")
				.append(" documentTextProportion,")
				.append(" defaultDocType,")
				.append(" openTextDefaultSelect,")
				.append(" openTextDefaultNode,")
				.append(" cleanCopyNodeSelect,")
				.append(" cleanCopyNodes,")
				.append(" printNodeSelect,")
				.append(" printNodes,")
				.append(" iscompellentmark,")
				.append(" iscancelcheck,")
				.append(" isHideTheTraces,")
				.append(" status,")
				.append(" autoCleanCopy,")
				.append(" isSaveTraceByClean,")
				.append(" flowattachfiled,")
				.append(" SyncTitleNodes,")
				.append(" reUploadText,")
				.append(" reSelectMould,")
				.append(" useYozoOrWPS,")
				.append(" displayTab,")
				.append(" saveBackForm,")
				.append(" saveDocRemind,")
				.append(" useIwebOfficeNodes,")
				.append(" wfAttachKeep,")
				.append(" preFinishDocNodes,")
				.append(" isOpenDocumentCompare,")
				.append(" oldDocumentType,")
				.append(" oldDocumentValue,")
				.append(" compareDocumentType,")
				.append(" compareDocumentValue,")
				.append(" isShowAttachmentInForm,")
				.append(" ofdReaderNodeSelect,")
				.append(" ofdReaderDefaultNode,")
				.append(" changeTextNodeSelect,")
				.append(" changeTextNodes,")
				.append("workflowid) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			}
			
			flag = rs.executeUpdate(sb.toString(),
					Util.null2String(wcd.getFlowdocfield()).equals("")?null:wcd.getFlowdocfield(),
					Util.null2String(wcd.getDocumenttitlefield()).equals("")?null:wcd.getDocumenttitlefield(),
					Util.null2String(wcd.getDefaultview()).equals("")?null:wcd.getDefaultview(),
					Util.null2String(wcd.getFlowdoccatfield()).equals("")?null:wcd.getFlowdoccatfield(),
					Util.null2String(wcd.getNewtextnodes()).equals("")?null:wcd.getNewtextnodes(),
					Util.null2String(wcd.getUploadpdf()).equals("")?null:wcd.getUploadpdf(),
					Util.null2String(wcd.getUploadofd()).equals("")?null:wcd.getUploadofd(),
					Util.null2String(wcd.getUploadword()).equals("")?null:wcd.getUploadword(),
					Util.null2String(wcd.getIsworkflowdraft()).equals("")?null:wcd.getIsworkflowdraft(),
					Util.null2String(wcd.getExtfile2doc()).equals("")?null:wcd.getExtfile2doc(),
					Util.null2String(wcd.getIstextinform()).equals("")?null:wcd.getIstextinform(),
					Util.null2String(wcd.getIstextinformnodeselect()).equals("")?null:wcd.getIstextinformnodeselect(),
					Util.null2String(wcd.getIstextinformnode()).equals("")?null:wcd.getIstextinformnode(),
					Util.null2String(wcd.getIsTextInFormcanedit()).equals("")?null:wcd.getIsTextInFormcanedit(),
					Util.null2String(wcd.getIscolumnshow()).equals("")?null:wcd.getIscolumnshow(),
					Util.null2String(wcd.getDocumenttextposition()).equals("")?null:wcd.getDocumenttextposition(),
					Util.null2String(wcd.getDocumenttextproportion()).equals("")?null:wcd.getDocumenttextproportion(),
					Util.null2String(wcd.getDefaultdoctype()).equals("")?null:wcd.getDefaultdoctype(),
					Util.null2String(wcd.getOpentextdefaultselect()).equals("")?null:wcd.getOpentextdefaultselect(),
					Util.null2String(wcd.getOpentextdefaultnode()).equals("")?null:wcd.getOpentextdefaultnode(),
					Util.null2String(wcd.getCleancopynodeselect()).equals("")?null:wcd.getCleancopynodeselect(),
					Util.null2String(wcd.getCleancopynodes()).equals("")?null:wcd.getCleancopynodes(),
					Util.null2String(wcd.getPrintnodeselect()).equals("")?null:wcd.getPrintnodeselect(),
					Util.null2String(wcd.getPrintnodes()).equals("")?null:wcd.getPrintnodes(),
					Util.null2String(wcd.getIscompellentmark()).equals("")?null:wcd.getIscompellentmark(),
					Util.null2String(wcd.getIscancelcheck()).equals("")?null:wcd.getIscancelcheck(),
					Util.null2String(wcd.getIshidethetraces()).equals("")?null:wcd.getIshidethetraces(),

					Util.null2String(wcd.getStatus()).equals("")?null:wcd.getStatus(),
					Util.null2String(wcd.getAutocleancopy()).equals("")?null:wcd.getAutocleancopy(),
					Util.null2String(wcd.getIssavetracebyclean()).equals("")?null:wcd.getIssavetracebyclean(),
					Util.null2String(wcd.getFlowattachfiled()).equals("")?null:wcd.getFlowattachfiled(),
					Util.null2String(wcd.getSynctitlenodes()).equals("")?null:wcd.getSynctitlenodes(),
					
					Util.null2String(wcd.getReuploadtext()).equals("")?null:wcd.getReuploadtext(),
					Util.null2String(wcd.getReselectmould()).equals("")?null:wcd.getReselectmould(),
					Util.null2String(wcd.getUseyozoorwps()).equals("")?null:wcd.getUseyozoorwps(),
					Util.null2String(wcd.getDisplaytab()).equals("")?null:wcd.getDisplaytab(),
					Util.null2String(wcd.getSavebackform()).equals("")?null:wcd.getSavebackform(),
					Util.null2String(wcd.getSavedocremind()).equals("")?null:wcd.getSavedocremind(),
					Util.null2String(wcd.getUseiwebofficenodes()).equals("")?null:wcd.getUseiwebofficenodes(),
					Util.null2String(wcd.getWfattachkeep()).equals("")?null:wcd.getWfattachkeep(),
					Util.null2String(wcd.getPrefinishdocnodes()).equals("")?null:wcd.getPrefinishdocnodes(),
					Util.null2String(wcd.getIsopendocumentcompare()).equals("")?null:wcd.getIsopendocumentcompare(),
					Util.null2String(wcd.getOlddocumenttype()).equals("")?null:wcd.getOlddocumenttype(),
					Util.null2String(wcd.getOlddocumentvalue()).equals("")?null:wcd.getOlddocumentvalue(),
					Util.null2String(wcd.getComparedocumenttype()).equals("")?null:wcd.getComparedocumenttype(),
					Util.null2String(wcd.getComparedocumentvalue()).equals("")?null:wcd.getComparedocumentvalue(),
					Util.null2String(wcd.getIsshowattachmentinform()).equals("")?null:wcd.getIsshowattachmentinform(),
					Util.null2String(wcd.getOfdreadernodeselect()).equals("")?null:wcd.getOfdreadernodeselect(),
					Util.null2String(wcd.getOfdreaderdefaultnode()).equals("")?null:wcd.getOfdreaderdefaultnode(),
					//==zj
					Util.null2String(wcd.getChangeTextNodeSelect()).equals("")?null:wcd.getChangeTextNodeSelect(),
					Util.null2String(wcd.getChangeTextNodes()).equals("")?null:wcd.getChangeTextNodes(),

					Util.null2String(wcd.getWorkflowid()).equals("")?null:wcd.getWorkflowid());
			rs.writeLog("====wcd.getReuploadtext():"+wcd.getReuploadtext());

			/**构建日志需要内容 start gaohk*/
			Map oldMap = BeanUtils.describe(oldWorkflowCreateDoc);
			oldMap.put("ifversion",getIfVersion(String.valueOf(workflowId)));
			oldMap.put("officalType",getofficalType(String.valueOf(workflowId)));
			oldMap.put("workflow_base.isWorkflowDoc",oldWorkflowCreateDoc.getStatus());
			Map newMap = BeanUtils.describe(wcd);
			newMap.put("workflow_base.isWorkflowDoc",wcd.getStatus());
			newMap.put("ifversion",String.valueOf(ifversion));

			LogUtil.removeIntersectionEntry(oldMap, newMap);
			/**没有变动不做更新*/
			if(!(newMap.size()>0)){
				bizLogContext = null;
			}else{
				bizLogContext.setOldValues(oldMap);
				bizLogContext.setNewValues(newMap);
			}
			/**构建日志需要内容 end gaohk*/

			rs.executeUpdate("update workflow_base set ifversion = " + ifversion + ",officalType=" +officalType+ " where id=?",wcd.getWorkflowid());
			apidatas.put("api_status", flag);

			if(flag){
				OdocRequestdocUtil OdocRequestdocUtil = new OdocRequestdocUtil();
				//根据设置更新公文相关信息
				flag = OdocRequestdocUtil.OdocRequestdocInitByWfId(wcd.getWorkflowid());
			}
			int createdocStatus=Util.getIntValue(Util.null2String(params.get("createdocStatus")),0);
			OdocSettingBiz.updateWorkflowDocStatus(wcd.getWorkflowid(),createdocStatus);
		} catch (Exception e) {
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return apidatas;
	}




	private int workflowid;

	private int ifversion;
	private  int officalType;


	public int getWorkflowid() {
		return workflowid;
	}

	public void setWorkflowid(int workflowid) {
		this.workflowid = workflowid;
	}

	private String jsonStr;

	public String getJsonStr() {
		return jsonStr;
	}

	public void setJsonStr(String jsonStr) {
		this.jsonStr = jsonStr;
	}


	private User user;
	private Map<String, Object> params;
	public OdocSaveBaseInfoSettingCmd(int workflowid, User user) {
		this.workflowid = workflowid;
		this.user = user;
	}

	public OdocSaveBaseInfoSettingCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}


	public OdocSaveBaseInfoSettingCmd() {
	}

	public int getIfversion() {
		return ifversion;
	}

	public void setIfversion(int ifversion) {
		this.ifversion = ifversion;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public int getOfficalType() {
		return officalType;
	}

	public void setOfficalType(int officalType) {
		this.officalType = officalType;
	}

	/**
	 * 是否保存正文版本
	 * @param workflowId 流程路径ID
	 * @return ifVersion
	 */
	private String getIfVersion(String workflowId) {
		RecordSet rs = new RecordSet();
		rs.executeQuery("select ifVersion from workflow_base where id=?", workflowId);
		if (rs.next()) {
			return rs.getString("ifVersion");
		} else {
			return null;
		}
	}
	/**
	 * 公文种类日志
	 * @param workflowId 流程路径ID
	 * @return ifVersion
	 */
	private String getofficalType(String workflowId) {
		RecordSet rs = new RecordSet();
		rs.executeQuery("select officalType from workflow_base where id=?", workflowId);
		if (rs.next()) {
			return rs.getString("officalType");
		} else {
			return null;
		}
	}
}
