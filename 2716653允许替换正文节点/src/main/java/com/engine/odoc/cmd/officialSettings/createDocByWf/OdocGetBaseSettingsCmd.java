/*
 * Copyright (c) 2001-2018 泛微软件.
 * 泛微协同商务系统,版权所有.
 *
 */
package com.engine.odoc.cmd.officialSettings.createDocByWf;

import com.api.browser.bean.BrowserBean;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.browser.util.ConditionFactory;
import com.api.browser.util.ConditionType;
import com.api.doc.detail.util.ImageConvertUtil;
import com.api.doc.yozo.weboffice.util.YozoWebOfficeUtil;
import com.engine.core.interceptor.Command;
import com.engine.core.interceptor.CommandContext;
import com.engine.odoc.biz.odocSettings.QysPdfSingleSignSettingBiz;
import com.engine.odoc.util.BrowserType;
import com.engine.odoc.util.OdocStandardUtil;
import com.engine.odoc.util.RecordSetToMapUtil;
import org.apache.commons.lang3.StringUtils;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.docs.category.SecCategoryComInfo;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.workflow.WfRightManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 流程创建文档-基础设置信息
 *
 * @author leiwb 2018/05/03
 *
 */
public class OdocGetBaseSettingsCmd implements Command<Map<String, Object>> {

	// 请求参数
	private Map<String, Object> params;
	// 用户信息
	private User user;
	// 字段项工厂
	private ConditionFactory conditionFactory;
	//页面字段的布局参数(字段)
    private final int FIELD_COL_VALUE = 16;
    //标签
    private final int LABEL_COL_VALUE = 8;

	/**
	 * 构造方法
	 *
	 * @param user
	 * @param params
	 */
	public OdocGetBaseSettingsCmd(Map<String, Object> params, User user) {
		this.params = params;
		this.user = user;
		this.conditionFactory = new ConditionFactory(user);
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		// 返回值
		Map<String, Object> apidata = new HashMap<String, Object>();
		int workflowId = Util.getIntValue(Util.null2String(params.get("workflowId")));
		boolean haspermission = new WfRightManager().hasPermission3(workflowId, 0, user, WfRightManager.OPERATION_CREATEDIR);
		if(!HrmUserVarify.checkUserRight("WorkflowManage:All", user) && !haspermission){
			apidata.put("sessionkey_state", "noright");
			return apidata;
		}
		// 页面显示项集合
		return getBaseSettingsList();
	}

	/**
	 * 获取基本设置
	 * @return List
	 */

	//是否表单
	private String isBill="";
	//表单Id
	private int formId = 0;
	private Map getBaseSettingsList() {

		List conditioninfo = new ArrayList();

		String workflowId = (String) params.get("workflowId");

		Map<String, Object> BaseItem = new HashMap<String, Object>();
		conditioninfo.add(BaseItem);
		/** 基本设置  start*/
		List<SearchConditionItem> itemList1 = new ArrayList<SearchConditionItem>();
		BaseItem.put("defaultshow",true);
		BaseItem.put("items",itemList1);
		BaseItem.put("title",SystemEnv.getHtmlLabelName(82751,user.getLanguage()));

		// 定义返回给前端显示项的存放集合
		// 获取字段项的值
		Map<String, Object> baseSettingsMap = getBaseSettings();

		RecordSet rs = new RecordSet();
		rs.executeQuery("select formid,isbill from workflow_base where id="+workflowId);
		if(rs.next()){
			formId = Util.getIntValue(rs.getString("formid"), 0);
			isBill = rs.getString("isbill");
			if(!"1".equals(isBill)){
				isBill="0";
			}
		}

		// 是否公文流程
		int officalTypeValue = -1;
		rs.executeQuery("select officalType from workflow_base where id = ?",workflowId);
		if(rs.next()){
			officalTypeValue = rs.getInt("officalType");
		}
		SearchConditionItem isOdocWorkflowItem = conditionFactory.createCondition(ConditionType.SELECT, 522075, "isOdocWorkflow");
		isOdocWorkflowItem.setDetailtype(3);
		List<SearchConditionOption> isOdocWorkflowOptions = new ArrayList<SearchConditionOption>();
		String isOdocWorkflow = officalTypeValue > 0 ? "1" : "0";
		isOdocWorkflowOptions.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(163, user.getLanguage()), false));
		isOdocWorkflowOptions.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(30587, user.getLanguage()),false));
		isOdocWorkflowItem.setOptions(isOdocWorkflowOptions);
		isOdocWorkflowItem.setValue(isOdocWorkflow);
		String inOdocWorkflow = Util.null2String(params.get("inOdocWorkflow"));
		if(inOdocWorkflow.equals("false")){
			itemList1.add(isOdocWorkflowItem);
		}

		//公文类型
		SearchConditionItem officalTypeItem = getOfficalTypeItem(officalTypeValue);
		itemList1.add(officalTypeItem);

		//正文字段
		SearchConditionItem MainText = conditionFactory.createCondition(ConditionType.SELECT, "1265,261","flowdocfield");
		MainText.setFieldcol(FIELD_COL_VALUE);
		MainText.setLabelcol(LABEL_COL_VALUE);
		MainText.setOptions(getFieldOptions(formId,isBill,(String)baseSettingsMap.get("FLOWDOCFIELD"), false,"3","9"));
		MainText.setRules("required");
		MainText.setHelpfulTip(SystemEnv.getHtmlLabelName(387031,user.getLanguage()));
		MainText.setViewAttr(3);
		itemList1.add(MainText);

		//正文标题
		SearchConditionItem MainTextTitle = conditionFactory.createCondition(ConditionType.SELECT, "1265,229","documenttitlefield");
		MainTextTitle.setFieldcol(FIELD_COL_VALUE);
		MainTextTitle.setLabelcol(LABEL_COL_VALUE);
		MainTextTitle.setOptions(getFieldOptions(formId,isBill,(String)baseSettingsMap.get("DOCUMENTTITLEFIELD"),true,"1","1"));
		MainTextTitle.setHelpfulTip(SystemEnv.getHtmlLabelName(21101,user.getLanguage()));
		itemList1.add(MainTextTitle);

		// 正文默认存放目录
		//路径
		SearchConditionItem defaultDirectory =  conditionFactory.createCondition(ConditionType.BROWSER, 33319, "defaultView",BrowserType.CATEGORY);
		List<Map<String,Object>> replaceDatas = new ArrayList<Map<String,Object>>();
		Map<String,Object> defalueValue  = new HashMap<String,Object>();
		String directory = (String) baseSettingsMap.get("DEFAULTVIEW");
		directory = getDefaultDir(directory);
		//获取路径名称
		SecCategoryComInfo SecCCI = new SecCategoryComInfo();
		String docPath = SecCCI.getAllParentName(directory,true);
		defalueValue.put("id", directory);
		defalueValue.put("name", docPath);
		replaceDatas.add(defalueValue);
		defaultDirectory.getBrowserConditionParam().setReplaceDatas(replaceDatas);
		defaultDirectory.getBrowserConditionParam().setIcon("icon-coms-workflow");
		defaultDirectory.getBrowserConditionParam().setIconBgcolor("#0079DE");
		defaultDirectory.getBrowserConditionParam().setTitle(SystemEnv.getHtmlLabelName(33319, user.getLanguage()));
		defaultDirectory.setRules("required");
		defaultDirectory.setFieldcol(this.FIELD_COL_VALUE);
		defaultDirectory.setLabelcol(this.LABEL_COL_VALUE);
		defaultDirectory.setValue(docPath);
		defaultDirectory.setViewAttr(3);
		itemList1.add(defaultDirectory);

		// 目录区分字段
		SearchConditionItem selectItemDirectory = conditionFactory.createCondition(ConditionType.SELECT, 531709 ,"flowDocCatField");
		//设置选项值
		String fieldId = (String) baseSettingsMap.get("FLOWDOCCATFIELD");
		selectItemDirectory.setFieldcol(FIELD_COL_VALUE);
		selectItemDirectory.setLabelcol(LABEL_COL_VALUE);
		selectItemDirectory.setOptions(getSelectFieldOptions(workflowId,fieldId));
		selectItemDirectory.setHelpfulTip(SystemEnv.getHtmlLabelName(385292, user.getLanguage()));
		selectItemDirectory.setValue(fieldId + "");
		itemList1.add(selectItemDirectory);


		//默认打开正文的节点
		//SearchConditionItem defaultNodesOfOpenningText = this.getNodesByIds((String)baseSettingsMap.get("OPENTEXTDEFAULTNODE"), workflowId, 128306, "openTextDefaultNode");
		SearchConditionItem defaultNodesOfOpenningText = getBaseSettingSeleInfo(0,(String)baseSettingsMap.get("OPENTEXTDEFAULTSELECT"),128306,"openTextDefaultSelect",this.getNodesByIds((String)baseSettingsMap.get("OPENTEXTDEFAULTNODE"), workflowId, 128306, "openTextDefaultNode"));
		itemList1.add(defaultNodesOfOpenningText);

		//默认预览正文
		SearchConditionItem docPreviewType = conditionFactory.createCondition(ConditionType.SWITCH, 517013, "useyozoorwps");
		String useYozoOrWps = (String) baseSettingsMap.get("USEYOZOORWPS");
		docPreviewType.setValue("1".equals(useYozoOrWps) ? "1" : "0");
		docPreviewType.setFieldcol(this.FIELD_COL_VALUE);
		docPreviewType.setLabelcol(this.LABEL_COL_VALUE);
		docPreviewType.setHelpfulTip(SystemEnv.getHtmlLabelName(517014, user.getLanguage()));
		itemList1.add(docPreviewType);

		/** 基本设置  end*/

		/** 常用设置  start*/
		Map<String, Object> CommonInfoItem = new HashMap<String, Object>();
		conditioninfo.add(CommonInfoItem);
		List<SearchConditionItem> itemListCommon = new ArrayList<SearchConditionItem>();
		CommonInfoItem.put("defaultshow",true);
		CommonInfoItem.put("items",itemListCommon);
		CommonInfoItem.put("title",SystemEnv.getHtmlLabelName(528866,user.getLanguage()));


		//只能新建正文
		SearchConditionItem onlyCreateText = conditionFactory.createCondition(ConditionType.SWITCH, 33320, "newTextNodes");
		String onlyCreate = (String) baseSettingsMap.get("NEWTEXTNODES");
		onlyCreateText.setValue("1".equals(onlyCreate) ? "1" : "0");
		onlyCreateText.setFieldcol(this.FIELD_COL_VALUE);
		onlyCreateText.setLabelcol(this.LABEL_COL_VALUE);
		onlyCreateText.setHelpfulTip(SystemEnv.getHtmlLabelName(385293, user.getLanguage()));
		itemListCommon.add(onlyCreateText);
		//允许上传WORD正文
		SearchConditionItem canUploadWORDText = conditionFactory.createCondition(ConditionType.SWITCH, 533159, "uploadWORD");
		String uploadWORD = (String) baseSettingsMap.get("UPLOADWORD");
		canUploadWORDText.setValue("1".equals(uploadWORD) ? "1" : "0");
		canUploadWORDText.setFieldcol(this.FIELD_COL_VALUE);
		canUploadWORDText.setLabelcol(this.LABEL_COL_VALUE);
		canUploadWORDText.setHelpfulTip(SystemEnv.getHtmlLabelName(	533160, user.getLanguage()));
		itemListCommon.add(canUploadWORDText);
		//允许上传PDF正文
		SearchConditionItem canUploadPDFText = conditionFactory.createCondition(ConditionType.SWITCH, 382507, "uploadPDF");
		String uploadPDF = (String) baseSettingsMap.get("UPLOADPDF");
		canUploadPDFText.setValue("1".equals(uploadPDF) ? "1" : "0");
		canUploadPDFText.setFieldcol(this.FIELD_COL_VALUE);
		canUploadPDFText.setLabelcol(this.LABEL_COL_VALUE);
		canUploadPDFText.setHelpfulTip(SystemEnv.getHtmlLabelName(385294, user.getLanguage()));
		itemListCommon.add(canUploadPDFText);

		//允许上传OFD正文
		SearchConditionItem canUploadOFDText = conditionFactory.createCondition(ConditionType.SWITCH, 510447, "uploadOFD");
		String uploadOFD = (String) baseSettingsMap.get("UPLOADOFD");
		canUploadOFDText.setValue("1".equals(uploadOFD) ? "1" : "0");
		canUploadOFDText.setFieldcol(this.FIELD_COL_VALUE);
		canUploadOFDText.setLabelcol(this.LABEL_COL_VALUE);
		canUploadOFDText.setHelpfulTip(SystemEnv.getHtmlLabelName(510445, user.getLanguage()));
		itemListCommon.add(canUploadOFDText);


		//保留正文版本
		SearchConditionItem saveTextVersion = conditionFactory.createCondition(ConditionType.SWITCH, 33321, "ifVersion");
		String ifVersion = getIfVersion(workflowId);
		saveTextVersion.setValue("1".equals(ifVersion) ? "1" : "0");
		saveTextVersion.setFieldcol(this.FIELD_COL_VALUE);
		saveTextVersion.setLabelcol(this.LABEL_COL_VALUE);
		saveTextVersion.setHelpfulTip(SystemEnv.getHtmlLabelName(385295, user.getLanguage()));
		itemListCommon.add(saveTextVersion);

		//存为流程草稿
		SearchConditionItem saveTextAsDraf = conditionFactory.createCondition(ConditionType.SWITCH, 33322, "isWorkflowDraft");
		String isWorkflowDraft = (String) baseSettingsMap.get("ISWORKFLOWDRAFT");
		saveTextAsDraf.setValue("1".equals(isWorkflowDraft) ? "1" : "0");
		saveTextAsDraf.setFieldcol(this.FIELD_COL_VALUE);
		saveTextAsDraf.setLabelcol(this.LABEL_COL_VALUE);
		saveTextAsDraf.setHelpfulTip(SystemEnv.getHtmlLabelName(385296, user.getLanguage()));
		itemListCommon.add(saveTextAsDraf);

		//智能排版设置
		if((ImageConvertUtil.canFormatForWpsDocCenter() || ImageConvertUtil.canViewForWpsForLinux()) && ImageConvertUtil.canEditForWps("doc",null) && ImageConvertUtil.openSmartOfficial()){
			SearchConditionItem smartOfficeItem = conditionFactory.createCondition(ConditionType.SELECT, 533464, "smartOfficeSet");
			itemListCommon.add(smartOfficeItem);
		}
		//ofd阅读器节点
		SearchConditionItem ofdSelectNode = getOfdNodeSelect((String)baseSettingsMap.get("OFDREADERNODESELECT"),	535499,"ofdReaderNodeSelect",this.getNodesByIds((String)baseSettingsMap.get("OFDREADERDEFAULTNODE"), workflowId, 	535110, "ofdReaderDefaultNode"));
		ofdSelectNode.setHelpfulTip(SystemEnv.getHtmlLabelName(535121 , user.getLanguage()));
		itemListCommon.add(ofdSelectNode);
		/*
		if(ImageConvertUtil.canViewForWpsForLinux()){
			//自动排版的节点
			SearchConditionItem autoprettifyNodesItem = getBaseSettingSeleInfo(0,(String)baseSettingsMap.get("AUTOPRETTIFYNODESSELECT"),503834,"autoprettifyNodesSelect",this.getNodesByIds((String)baseSettingsMap.get("AUTOPRETTIFYNODES"), workflowId, 503834, "autoprettifyNodes"));
			itemListCommon.add(autoprettifyNodesItem);

			//手动排版的节点
			SearchConditionItem manualPrettifyNodesItem = getBaseSettingSeleInfo(0,(String)baseSettingsMap.get("MANUALPRETTIFYNODESSELECT"),531812,"manualPrettifyNodesSelect",this.getNodesByIds((String)baseSettingsMap.get("MANUALPRETTIFYNODES"), workflowId, 531812, "manualPrettifyNodes"));
			itemListCommon.add(manualPrettifyNodesItem);
		}
		*/
		/** 常用设置  end*/


		/** 正文显示设置  start*/
		Map<String, Object> contentSettingsItem = new HashMap<String, Object>();
		conditioninfo.add(contentSettingsItem);
		List<SearchConditionItem> itemList5 = new ArrayList<SearchConditionItem>();
		contentSettingsItem.put("defaultshow",true);
		contentSettingsItem.put("items",itemList5);
		contentSettingsItem.put("title",SystemEnv.getHtmlLabelName(522761,user.getLanguage()));

		//正文内容显示在流程表单
		SearchConditionItem textInForm = conditionFactory.createCondition(ConditionType.SWITCH, 382048, "isTextInForm");
		String isTextInForm = (String) baseSettingsMap.get("ISTEXTINFORM");
		textInForm.setValue("1".equals(isTextInForm) ? "1" : "0");
		textInForm.setFieldcol(this.FIELD_COL_VALUE);
		textInForm.setLabelcol(this.LABEL_COL_VALUE);
		textInForm.setHelpfulTip(SystemEnv.getHtmlLabelName(514658 , user.getLanguage()));
		itemList5.add(textInForm);

		//选择正文显示在表单的节点
		SearchConditionItem textInFormSeletItem = getBaseSettingSeleInfo(1,(String)baseSettingsMap.get("ISTEXTINFORMNODESELECT"),385180,"isTextInFormNodeSelect",this.getNodesByIds((String)baseSettingsMap.get("ISTEXTINFORMNODE"), workflowId, 385180, "isTextInformNode"));
		itemList5.add(textInFormSeletItem);

		//允许在表单中直接编辑正文
		SearchConditionItem textInFormCanEdit = conditionFactory.createCondition(ConditionType.SWITCH, 516725, "isTextInFormCanEdit");
		String isTextInFormCanEdit = (String) baseSettingsMap.get("ISTEXTINFORMCANEDIT");
		textInFormCanEdit.setValue("1".equals(isTextInFormCanEdit) ? "1" : "0");
		textInFormCanEdit.setFieldcol(this.FIELD_COL_VALUE);
		textInFormCanEdit.setLabelcol(this.LABEL_COL_VALUE);
		textInFormCanEdit.setHelpfulTip(SystemEnv.getHtmlLabelName(516717 , user.getLanguage()));
		itemList5.add(textInFormCanEdit);

		//正文分栏显示
		SearchConditionItem isColumnShowItem = conditionFactory.createCondition(ConditionType.SWITCH, 513101, "isColumnShow");
		String isColumnShow = (String) baseSettingsMap.get("ISCOLUMNSHOW");
		isColumnShowItem.setValue("1".equals(isColumnShow) ? "1" : "0");
		isColumnShowItem.setFieldcol(this.FIELD_COL_VALUE);
		isColumnShowItem.setLabelcol(this.LABEL_COL_VALUE);
		itemList5.add(isColumnShowItem);

		// 正文位置
		SearchConditionItem selectItemDocumentTextPosition = conditionFactory.createCondition(ConditionType.SELECT, 513158,"documentTextPosition");
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
		//设置选项值
		options.add(new SearchConditionOption("", ""));
		options.add(new SearchConditionOption("l", SystemEnv.getHtmlLabelName(510338 , user.getLanguage())));
		options.add(new SearchConditionOption("r", SystemEnv.getHtmlLabelName(510339 , user.getLanguage())));
		String documentTextPositionId = (String) baseSettingsMap.get("DOCUMENTTEXTPOSITION");
		selectItemDocumentTextPosition.setFieldcol(FIELD_COL_VALUE);
		selectItemDocumentTextPosition.setLabelcol(LABEL_COL_VALUE);
		selectItemDocumentTextPosition.setOptions(options);
		if(StringUtil.isNull(documentTextPositionId)){
			documentTextPositionId = "l";
		}
		selectItemDocumentTextPosition.setValue(documentTextPositionId);
		selectItemDocumentTextPosition.setDetailtype(3);
		itemList5.add(selectItemDocumentTextPosition);

		//正文显示相对屏的比例
		SearchConditionItem showDocumentTextProportionSetItem = conditionFactory.createCondition(ConditionType.INPUTNUMBER, 390817, "documentTextProportion");
		String documentTextProportionValue = (String) baseSettingsMap.get("DOCUMENTTEXTPROPORTION");
		showDocumentTextProportionSetItem.setValue(documentTextProportionValue);
		showDocumentTextProportionSetItem.setLabel(SystemEnv.getHtmlLabelName(390817 , user.getLanguage())+"（%）");
		showDocumentTextProportionSetItem.setMin("0");
		showDocumentTextProportionSetItem.setMax("100");
		showDocumentTextProportionSetItem.setViewAttr(3);
		itemList5.add(showDocumentTextProportionSetItem);

		//正文显示页签
		SearchConditionItem displayTab = conditionFactory.createCondition(ConditionType.SWITCH, 531118, "displayTab");
		String tabInfo = (String) baseSettingsMap.get("DISPLAYTAB");
		displayTab.setValue("1".equals(tabInfo) ? "1" : "0");
		displayTab.setFieldcol(this.FIELD_COL_VALUE);
		displayTab.setLabelcol(this.LABEL_COL_VALUE);
		displayTab.setHelpfulTip(SystemEnv.getHtmlLabelName(517850, user.getLanguage()));
		itemList5.add(displayTab);

		//按照节点显示不同的正文
		SearchConditionItem odocShowDifTextByNodeItem = conditionFactory.createCondition(ConditionType.SELECT, 530781, "odocShowDifTextByNode");
		boolean odocShowDif = "1".equals(rs.getPropValue("doc_html_for_weaver","ShowDifText"));
		if(odocShowDif){
			itemList5.add(odocShowDifTextByNodeItem);
		}

		//正文保存后允许重新上传
		SearchConditionItem reUploadTextItem = conditionFactory.createCondition(ConditionType.SWITCH, 517774, "reUploadText");
		String reUploadText = (String) baseSettingsMap.get("REUPLOADTEXT");
		reUploadTextItem.setValue("1".equals(reUploadText) ? "1" : "0");
		reUploadTextItem.setFieldcol(this.FIELD_COL_VALUE);
		reUploadTextItem.setLabelcol(this.LABEL_COL_VALUE);
		reUploadTextItem.setHelpfulTip(SystemEnv.getHtmlLabelName(515909 , user.getLanguage()));
		itemList5.add(reUploadTextItem);

		//==zj 允许替换正文的节点
		SearchConditionItem changeTextNode = getBaseSettingSeleInfo(0,(String)baseSettingsMap.get("changeTextNodeSelect"),-81472,"changeTextNodeSelect",this.getNodesByIds((String)baseSettingsMap.get("changeTextNodes"), workflowId, -81472, "changeTextNodes"));
		changeTextNode.setHelpfulTip(SystemEnv.getHtmlLabelName(-81472, user.getLanguage()));
		itemList5.add(changeTextNode);

		//编辑模板选定后允许更换
		SearchConditionItem reSelectMouldItem = conditionFactory.createCondition(ConditionType.SWITCH, 517775, "reSelectMould");
		String reSelectMould = (String) baseSettingsMap.get("RESELECTMOULD");
		reSelectMouldItem.setValue("1".equals(reSelectMould) ? "1" : "0");
		reSelectMouldItem.setFieldcol(this.FIELD_COL_VALUE);
		reSelectMouldItem.setLabelcol(this.LABEL_COL_VALUE);
		reSelectMouldItem.setHelpfulTip(SystemEnv.getHtmlLabelName(515912,user.getLanguage()));
		itemList5.add(reSelectMouldItem);

		/** 正文显示设置  end*/

		/** 痕迹设置  start*/
		Map<String, Object> TraceInfoItem = new HashMap<String, Object>();
		conditioninfo.add(TraceInfoItem);

		// 痕迹设置
		List<SearchConditionItem> itemList3 = new ArrayList<SearchConditionItem>();
		TraceInfoItem.put("defaultshow",true);
		TraceInfoItem.put("items",itemList3);
		TraceInfoItem.put("title",SystemEnv.getHtmlLabelName(33317,user.getLanguage()));


//		//是否清稿时保存痕迹正文
//		SearchConditionItem saveTraceByCleanCopy = conditionFactory.createCondition(ConditionType.SWITCH, 507234, "isSaveTraceByClean");
//		String isSaveTraceByClean = (String) baseSettingsMap.get("ISSAVETRACEBYCLEAN");
//		saveTraceByCleanCopy.setValue("1".equals(isSaveTraceByClean) ? "1" : "0");
//		saveTraceByCleanCopy.setFieldcol(this.FIELD_COL_VALUE);
//		saveTraceByCleanCopy.setLabelcol(this.LABEL_COL_VALUE);
//		itemList3.add(saveTraceByCleanCopy);

        //必须保留痕迹
        SearchConditionItem holdMarks = conditionFactory.createCondition(ConditionType.SWITCH, 21631, "iscompellentmark");
        String iscompellentmark = (String) baseSettingsMap.get("ISCOMPELLENTMARK");
        holdMarks.setValue("1".equals(iscompellentmark) ? "1" : "0");
        holdMarks.setFieldcol(this.FIELD_COL_VALUE);
        holdMarks.setLabelcol(this.LABEL_COL_VALUE);
        holdMarks.setHelpfulTip(SystemEnv.getHtmlLabelName(385302 , user.getLanguage()));
        itemList3.add(holdMarks);

        //取消审阅
        SearchConditionItem cancelcheck = conditionFactory.createCondition(ConditionType.SWITCH, 21632, "iscancelcheck");
        String iscancelcheck = (String) baseSettingsMap.get("ISCANCELCHECK");
        cancelcheck.setValue("1".equals(iscancelcheck) ? "1" : "0");
        cancelcheck.setFieldcol(this.FIELD_COL_VALUE);
        cancelcheck.setLabelcol(this.LABEL_COL_VALUE);
        cancelcheck.setHelpfulTip(SystemEnv.getHtmlLabelName(385304 , user.getLanguage()));
        itemList3.add(cancelcheck);

        //编辑正文时默认隐藏痕迹
        SearchConditionItem hideTheTraces = conditionFactory.createCondition(ConditionType.SWITCH, 24443, "isHideTheTraces");
        String isHideTheTraces = (String) baseSettingsMap.get("ISHIDETHETRACES");
        hideTheTraces.setValue("1".equals(isHideTheTraces) ? "1" : "0");
        hideTheTraces.setFieldcol(this.FIELD_COL_VALUE);
        hideTheTraces.setLabelcol(this.LABEL_COL_VALUE);
        hideTheTraces.setHelpfulTip(SystemEnv.getHtmlLabelName(385305 , user.getLanguage()));
        itemList3.add(hideTheTraces);

		//清稿节点
		//SearchConditionItem cleanCopyNodes = this.getNodesByIds((String)baseSettingsMap.get("CLEANCOPYNODES"), workflowId, 129588, "cleanCopyNodes");
		SearchConditionItem cleanCopyNodes = getBaseSettingSeleInfo(0,(String)baseSettingsMap.get("CLEANCOPYNODESELECT"),129588,"cleanCopyNodeSelect",this.getNodesByIds((String)baseSettingsMap.get("CLEANCOPYNODES"), workflowId, 129588, "cleanCopyNodes"));
		cleanCopyNodes.setHelpfulTip(SystemEnv.getHtmlLabelName(129892 , user.getLanguage()));
		itemList3.add(cleanCopyNodes);

		//自动清稿
		SearchConditionItem autoCleanCopy = conditionFactory.createCondition(ConditionType.SWITCH, 503587, "autoCleanCopy");
		String isAutoCleanCopy = (String) baseSettingsMap.get("AUTOCLEANCOPY");
		autoCleanCopy.setValue("1".equals(isAutoCleanCopy) ? "1" : "0");
		autoCleanCopy.setFieldcol(this.FIELD_COL_VALUE);
		autoCleanCopy.setLabelcol(this.LABEL_COL_VALUE);
		itemList3.add(autoCleanCopy);

		/** 痕迹设置  end*/


		/** 流程创建节点相关设置  start*/
		/*//流程创建节点相关设置
		Map<String, Object> createNodeItem = new HashMap<String, Object>();
		conditioninfo.add(createNodeItem);

		List<SearchConditionItem> itemList4 = new ArrayList<SearchConditionItem>();
		createNodeItem.put("defaultshow",true);
		createNodeItem.put("items",itemList4);
		createNodeItem.put("title",SystemEnv.getHtmlLabelName(515890,user.getLanguage()));*/


		/** 流程创建节点相关设置  end*/

		/** 其他设置  start*/
		Map<String, Object> OtherInfoItem = new HashMap<String, Object>();
		conditioninfo.add(OtherInfoItem);
		List<SearchConditionItem> itemList2 = new ArrayList<SearchConditionItem>();
		OtherInfoItem.put("defaultshow",true);
		OtherInfoItem.put("items",itemList2);
		OtherInfoItem.put("title",SystemEnv.getHtmlLabelName(20824,user.getLanguage()));


		//流程附件转为正文附件
		SearchConditionItem wfAttachToTextAttach = conditionFactory.createCondition(ConditionType.SWITCH, 33324, "extfile2doc");
		String extfile2doc = (String) baseSettingsMap.get("EXTFILE2DOC");
		wfAttachToTextAttach.setValue("1".equals(extfile2doc) ? "1" : "0");
		wfAttachToTextAttach.setFieldcol(this.FIELD_COL_VALUE);
		wfAttachToTextAttach.setLabelcol(this.LABEL_COL_VALUE);
		wfAttachToTextAttach.setHelpfulTip(SystemEnv.getHtmlLabelName(385300, user.getLanguage()));
		itemList2.add(wfAttachToTextAttach);

		//流程附件转为正文附件，保留流程附件
		SearchConditionItem wfAttachKeep = conditionFactory.createCondition(ConditionType.SWITCH, 518232, "wfattachkeep");
		String AttachKeep = (String) baseSettingsMap.get("WFATTACHKEEP");
		wfAttachKeep.setValue("1".equals(AttachKeep) ? "1" : "0");
		wfAttachKeep.setFieldcol(this.FIELD_COL_VALUE);
		wfAttachKeep.setLabelcol(this.LABEL_COL_VALUE);
		wfAttachKeep.setHelpfulTip(""+ SystemEnv.getHtmlLabelName(10005435,weaver.general.ThreadVarLanguage.getLang())+"");
		itemList2.add(wfAttachKeep);
		//选择转为正文附件的字段
		SearchConditionItem attachmentBro = conditionFactory.createCondition(ConditionType.BROWSER, 514623, "flowattachfiled","fieldBrowserService");
		Map<String, Object> otherParams = new HashMap<String, Object>();
		otherParams.put("ref", "flowattachfiled");
		attachmentBro.setOtherParams(otherParams);
		attachmentBro.getBrowserConditionParam().setViewAttr(2);
		attachmentBro.getBrowserConditionParam().setTitle(SystemEnv.getHtmlLabelName(127230, user.getLanguage()));
		attachmentBro.getBrowserConditionParam().setIsSingle(false);
		attachmentBro.getBrowserConditionParam().setIcon("icon-coms-workflow");
		attachmentBro.getBrowserConditionParam().setIconBgcolor("#0079DE");
		attachmentBro.getBrowserConditionParam().getDataParams().put("billid", formId);
		attachmentBro.getBrowserConditionParam().getDataParams().put("isbill", isBill);
		attachmentBro.getBrowserConditionParam().getDataParams().put("workflowid", workflowId);
		attachmentBro.getBrowserConditionParam().getDataParams().put("flowattachfiled", "1");
		attachmentBro.getBrowserConditionParam().getCompleteParams().put("billid", formId);
		attachmentBro.getBrowserConditionParam().getCompleteParams().put("isbill", isBill);
		attachmentBro.getBrowserConditionParam().getCompleteParams().put("workflowid", workflowId);
		attachmentBro.getBrowserConditionParam().getCompleteParams().put("flowattachfiled", "1");
		attachmentBro.getBrowserConditionParam().getConditionDataParams().put("billid", formId);
		attachmentBro.getBrowserConditionParam().getConditionDataParams().put("isbill", isBill);
		attachmentBro.getBrowserConditionParam().getConditionDataParams().put("workflowid", workflowId);
		attachmentBro.getBrowserConditionParam().getConditionDataParams().put("flowattachfiled", "1");
		attachmentBro.getBrowserConditionParam().getDestDataParams().put("billid", formId);
		attachmentBro.getBrowserConditionParam().getDestDataParams().put("isbill", isBill);
		attachmentBro.getBrowserConditionParam().getDestDataParams().put("workflowid", workflowId);
		attachmentBro.getBrowserConditionParam().getDestDataParams().put("flowattachfiled", "1");
		attachmentBro.setFieldcol(this.FIELD_COL_VALUE);
		attachmentBro.setLabelcol(this.LABEL_COL_VALUE);
		attachmentBro.setHelpfulTip(SystemEnv.getHtmlLabelName(514622, user.getLanguage()));
		attachmentBro.getBrowserConditionParam().setReplaceDatas(this.getAttachReplaceDatas((String)baseSettingsMap.get("FLOWATTACHFILED")));
		itemList2.add(attachmentBro);

		//默认文档类型
		SearchConditionItem defaultTextType = getDefaultDocType((String)baseSettingsMap.get("DEFAULTDOCTYPE"));
		itemList2.add(defaultTextType);

		//打印节点
		//SearchConditionItem printTextNodes = this.getNodesByIds((String)baseSettingsMap.get("PRINTNODES"), workflowId, 21529, "printNodes");
		SearchConditionItem printTextNodes = getBaseSettingSeleInfo(0,(String)baseSettingsMap.get("PRINTNODESELECT"),524336,"printNodeSelect",this.getNodesByIds((String)baseSettingsMap.get("PRINTNODES"), workflowId, 524336, "printNodes"));
		boolean isQysPdfSingleSign = QysPdfSingleSignSettingBiz.isQysPdfSingleSign();
		if(isQysPdfSingleSign){
			printTextNodes.setHelpfulTip(SystemEnv.getHtmlLabelName(516493, user.getLanguage()));
		}
		itemList2.add(printTextNodes);
		//正文标题同步节点
		SearchConditionItem SyncMainTextNodes = this.getNodesByIds((String)baseSettingsMap.get("SYNCTITLENODES"), workflowId, 517758 , "SyncTitleNodes");
		itemList2.add(SyncMainTextNodes);

		//使用金格iWebOffice节点
		SearchConditionItem useIwebOfficeNodes = this.getNodesByIds((String)baseSettingsMap.get("USEIWEBOFFICENODES"), workflowId, 534597 , "useIwebOfficeNodes");



		//WPS服务端编辑器
		boolean wpsEditor = ImageConvertUtil.canEditForWps("doc",null);
		boolean yozoEditor = YozoWebOfficeUtil.canEditForYozoFormOdoc(false);
		if(wpsEditor || yozoEditor)
		{
			itemList2.add(useIwebOfficeNodes);
		}


		//保存正文返回表单
		SearchConditionItem saveBackFormItem = conditionFactory.createCondition(ConditionType.SWITCH, 517015, "savebackform");
		String saveBackForm = (String) baseSettingsMap.get("SAVEBACKFORM");
		saveBackFormItem.setValue("1".equals(saveBackForm) ? "1" : "0");
		saveBackFormItem.setFieldcol(this.FIELD_COL_VALUE);
		saveBackFormItem.setLabelcol(this.LABEL_COL_VALUE);
		saveBackFormItem.setHelpfulTip(SystemEnv.getHtmlLabelName(517016 , user.getLanguage()));
		itemList2.add(saveBackFormItem);


		//OFD版式文件公文元数据设置
		SearchConditionItem odocMetaDataSetItem = conditionFactory.createCondition(ConditionType.SELECT, 531438, "odocMetaDataSet");
		itemList2.add(odocMetaDataSetItem);


		if(OdocStandardUtil.isOpenDocumentCompare()){
			//文档比对
			SearchConditionItem isOpenDocumentCompareItem = conditionFactory.createCondition(ConditionType.SWITCH, 525878, "ISOPENDOCUMENTCOMPARE");
			String isOpenDocumentCompare = (String) baseSettingsMap.get("ISOPENDOCUMENTCOMPARE");
			isOpenDocumentCompareItem.setValue("1".equals(isOpenDocumentCompare) ? "1" : "0");
			isOpenDocumentCompareItem.setFieldcol(this.FIELD_COL_VALUE);
			isOpenDocumentCompareItem.setLabelcol(this.LABEL_COL_VALUE);
			itemList2.add(isOpenDocumentCompareItem);
		}

		//离开正文提醒保存
		SearchConditionItem saveDocRemindItem = conditionFactory.createCondition(ConditionType.SWITCH, 517017, "savedocremind");
		String saveDocRemind = (String) baseSettingsMap.get("SAVEDOCREMIND");
		saveDocRemindItem.setValue("1".equals(saveDocRemind) ? "1" : "0");
		saveDocRemindItem.setFieldcol(this.FIELD_COL_VALUE);
		saveDocRemindItem.setLabelcol(this.LABEL_COL_VALUE);
		saveDocRemindItem.setHelpfulTip(SystemEnv.getHtmlLabelName(517018 , user.getLanguage()));
		itemList2.add(saveDocRemindItem);
		//预归档节点
		SearchConditionItem preFileDocNodes = conditionFactory.createCondition(ConditionType.SELECT, 526924, "prefinishdocnodes",getNodeOptions(workflowId));
		preFileDocNodes.setValue(baseSettingsMap.get("PREFINISHDOCNODES"));
		preFileDocNodes.setHelpfulTip(SystemEnv.getHtmlLabelName(526926 , user.getLanguage()));
		itemList2.add(preFileDocNodes);

		//移动端正文附件是否显示在表单
		SearchConditionItem isShowAttachmentInFormItem = conditionFactory.createCondition(ConditionType.SWITCH, 531534, "isshowattachmentinform");
		String isShowAttachmentInForm = (String) baseSettingsMap.get("ISSHOWATTACHMENTINFORM");
		isShowAttachmentInFormItem.setValue("1".equals(isShowAttachmentInForm) ? "1" : "0");
		isShowAttachmentInFormItem.setFieldcol(this.FIELD_COL_VALUE);
		isShowAttachmentInFormItem.setLabelcol(this.LABEL_COL_VALUE);
		itemList2.add(isShowAttachmentInFormItem);

		/** 其他设置  end*/
/*		//使用编辑模板后痕迹不保留
		SearchConditionItem needlessRevisionsItem = conditionFactory.createCondition(ConditionType.SWITCH, 515910, "needlessRevisions");
		String needlessRevisions = (String) baseSettingsMap.get("NEEDLESSREVISIONS");
		needlessRevisionsItem.setValue("1".equals(needlessRevisions) ? "1" : "0");
		needlessRevisionsItem.setFieldcol(this.FIELD_COL_VALUE);
		needlessRevisionsItem.setLabelcol(this.LABEL_COL_VALUE);
		needlessRevisionsItem.setHelpfulTip(SystemEnv.getHtmlLabelName(515913,user.getLanguage()));
		itemList4.add(needlessRevisionsItem);*/
		Map<String, Object> resDatas = new HashMap<String, Object>();
		resDatas.put("conditioninfo",conditioninfo );
		resDatas.put("isOdocWorkflow",isOdocWorkflow.equals("1") );
		return resDatas;
	}

	/**
	 *  获取附件浏览按钮数据
	 * @param
	 * @param flowattachfiled
	 * @return
	 */
	private List<Map<String, Object>> getAttachReplaceDatas(String flowattachfiled){
		List<Map<String, Object>> replaceDatas = new ArrayList<Map<String,Object>>();
		RecordSet rs = new RecordSet();
		String sql = "";
		if(StringUtil.isNotNull(flowattachfiled)){
            String[] nodeIds = flowattachfiled.split(",");
            for(int i = 0 ; i < nodeIds.length; i++){
                if ("1".equals(isBill)){
                    sql = "select distinct t.id fieldid,t2.labelname fieldlable from workflow_billfield t, HtmlLabelInfo t2 where billid = ? and   fieldhtmltype=6 and t.fieldlabel = t2.indexid and t2.languageid=? and t.id = ?";
                }else {
                    sql = "select fieldid,fieldlable from workflow_fieldlable t,workflow_formdict wf where formid= ? and langurageid=? and wf.id = t.fieldid and fieldid in (select id from workflow_formdict where  fieldhtmltype=6) and fieldid = ?";
                }
                rs.executeQuery(sql,formId,user.getLanguage(),nodeIds[i]);
                if( rs.next() ){
                    Map<String, Object> replaceData = new HashMap<String, Object>();
                    replaceData.put("id", nodeIds[i]);
                    replaceData.put("name", rs.getString("fieldlable"));
                    replaceDatas.add(replaceData);
                }
            }
        }
		return replaceDatas;
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
	 * 获取选择框字段(所有选项的目录不为空的字段才能被选择)
	 * @param workflowId 流程路径ID
	 * @param selectedFieldId  选中的选择框字段Id
	 * @return List
	 */
	private List<SearchConditionOption> getSelectFieldOptions(String workflowId, String selectedFieldId) {

        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
        options.add(new SearchConditionOption("-1",""));
        RecordSet rs = new RecordSet();
        String sql = "";
        if (isBill.equals("0")){
            sql = "SELECT formDict.ID,fieldLable.fieldLable FROM workflow_fieldLable fieldLable,workflow_formField formField,workflow_formdict formDict "
            		+ "WHERE fieldLable.formid = formField.formid AND fieldLable.fieldid = formField.fieldid AND formField.fieldid = formDict.ID "
            		+ "AND (formField.isdetail <> '1' OR formField.isdetail IS NULL) AND formField.formid = ? AND fieldLable.langurageid = ? "
            		+ "and formDict.fieldHtmlType = '5' and not exists ( select * from workflow_selectitem where (docCategory is null or docCategory = '' OR docCategory like '%-1') "
            		+ "AND (cancel is null or cancel ='0') and isAccordToSubCom='0' and formDict.ID = workflow_selectitem.fieldid and isBill='0') order by formField.fieldorder";
            rs.executeQuery(sql, formId, user.getLanguage());
        }else{
            //去除复选框的选择
            sql = "SELECT id,fieldlabel FROM workflow_billfield t WHERE t.billId = ? AND viewType = 0 AND fieldHtmlType = '5' and type IN (1,3) AND NOT EXISTS ( "
            		+ "SELECT 1 FROM workflow_selectitem WHERE ( docCategory IS NULL OR docCategory = '' OR docCategory like '%-1') AND (cancel is null or cancel ='0') AND isAccordToSubCom = '0' AND t.ID "
            		+ " = workflow_selectitem.fieldid AND isBill = '1' ) ORDER BY dspOrder ";
            rs.executeQuery(sql, formId);
        }

        RecordSet recordSet = new RecordSet();
        while (rs.next()){
            String fieldId = rs.getString(1);
			recordSet.executeQuery("select * from workflow_selectitem where FIELDID=?",fieldId);
			if(recordSet.next()){
				String fieldName = isBill.equals("0") ? rs.getString(2) : SystemEnv.getHtmlLabelName(rs.getInt(2),user.getLanguage());
				options.add(new SearchConditionOption(fieldId, fieldName, fieldId.equals(selectedFieldId)));

			}
        }
        return options;
	}

	/**
	 * 获取文本字段选项
	 * @param formId
	 * @param isBill
	 * @param fieldIdParm
	 * @param isTitle
	 * @param fieldHtmlType
	 * @param type
	 * @return
	 */
	private List<String> FieldTypeList = new ArrayList<String>();
	private List<String> fieldIdList = new ArrayList<String>();
	private List<String> fieldNameList = new ArrayList<String>();
	private List<SearchConditionOption> getFieldOptions(int formId, String isBill, String fieldIdParm,
														boolean isTitle,String fieldHtmlType,String type) {
		RecordSet rs = new RecordSet();
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();

		options.add(new SearchConditionOption("", ""));
		if(isTitle){
			options.add(new SearchConditionOption("-3", SystemEnv.getHtmlLabelName(1334,user.getLanguage()), "-3".equals(fieldIdParm)));
		}
		//通过list获取数据
		if(FieldTypeList.size() <= 0){

			String sql = "";
			if ("1".equals(isBill)) {
				sql = "select formField.id,fieldLable.labelName as fieldLable,formField.fieldHtmlType,formField.type from HtmlLabelInfo fieldLable ,"
						+ " workflow_billfield formField where fieldLable.indexId=formField.fieldLabel "
						+ " and formField.billId=? and formField.viewType=0 and fieldLable.languageid =?"
						+ " order by formField.dspOrder";
			} else {
				sql = "select formDict.ID, fieldLable.fieldLable,formDict.fieldHtmlType,formDict.type from workflow_fieldLable fieldLable,"
						+ " workflow_formField formField, workflow_formdict formDict where "
						+ " fieldLable.formid = formField.formid and fieldLable.fieldid = formField.fieldid "
						+ " and formField.fieldid = formDict.ID and (formField.isdetail<>'1' or formField.isdetail is null) "
						+ " and formField.formid =?  and fieldLable.langurageid =?  "
						+ "  order by formField.fieldorder";
			}
			rs.executeQuery(sql, formId, user.getLanguage());
			while(rs.next()){
				//避免重复执行SQL，通过list记录数据
				FieldTypeList.add(rs.getString("fieldHtmlType")+"_" + rs.getString("type"));
				fieldIdList.add(rs.getString(1));
				fieldNameList.add(rs.getString(2));
			}
		}

		for(int i =0; i < FieldTypeList.size(); i++){
			if(!(fieldHtmlType + "_" + type).equals(FieldTypeList.get(i))){
				continue;
			}
			String fieldId = fieldIdList.get(i);
			String fieldName = fieldNameList.get(i);
			options.add(new SearchConditionOption(fieldId, fieldName, fieldId.equals(fieldIdParm)));
		}
		return options;
	}
	/**
	 * 获取文档目录ID
	 * @param directory
	 * @return
	 */
	private String getDefaultDir(String directory) {
		if (StringUtils.isBlank(directory)) {
			return "";
		}
		String[] dirArr = directory.split("\\|\\|");
		if (dirArr.length == 3) {
			return dirArr[2];
		} else if (dirArr.length == 1) {
			return directory;
		} else {
			return "";
		}
	}

	/**
	 * 获取默认文档类型
	 * @param object
	 * @return
	 */
    private SearchConditionItem getDefaultDocType(String object) {
        SearchConditionItem defaultDocType = conditionFactory.createCondition(ConditionType.SELECT, 22358 , "defaultDocType");
        defaultDocType.setFieldcol(this.FIELD_COL_VALUE);
        defaultDocType.setLabelcol(this.LABEL_COL_VALUE);

        List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
        //Office Word
        options.add(new SearchConditionOption("1", "Office Word" , ("1".equals(object) || "".equals(object))));
        if("2".equals(object)){
			//WPS文字
			options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(22359,user.getLanguage()) , "2".equals(object)));
		}
        defaultDocType.setOptions(options);
        defaultDocType.setViewAttr(2);
        return defaultDocType;
    }

	/**
	 * 获取流程创建文档的基础设置
	 *
	 * @return
	 */
	private Map<String, Object> getBaseSettings() {
//==zj
		// 返回的数据集
		Map<String, Object> baseSettingsMap = new HashMap<String, Object>();
		String workflowId = (String) params.get("workflowId");
		RecordSet rs = new RecordSet();
		rs.executeQuery("select FLOWDOCFIELD,DOCUMENTTITLEFIELD,defaultView,flowDocCatField,newTextNodes,"
				+ "uploadPDF,uploadOFD,uploadWORD,"
				+ " isWorkflowDraft,"
				+ "extfile2doc,"
				+ "isTextInForm,"
				+ "isTextInFormCanEdit,"
                + "isColumnShow,"
                + "documentTextPosition,"
                + "documentTextProportion,"
				+ "isSaveTraceByClean,"
				+ "flowattachfiled,"
				+ "defaultDocType,openTextDefaultNode,cleanCopyNodes,isTextInFormNodeSelect,isTextInformNode,openTextDefaultSelect,cleanCopyNodeSelect,printNodeSelect,changeTextNodeSelect,changeTextNodes,"
				+ "printNodes,iscompellentmark,iscancelcheck,isHideTheTraces,autoCleanCopy,reuploadtext,reselectmould,useYozoOrWps,displayTab,saveBackForm,saveDocRemind,SyncTitleNodes,useIwebOfficeNodes,wfAttachKeep,preFinishDocNodes,"
                + "isOpenDocumentCompare,oldDocumentType,oldDocumentValue,compareDocumentType,compareDocumentValue,isshowattachmentinform,autoprettifyNodesSelect,autoprettifyNodes,manualPrettifyNodes,manualPrettifyNodesSelect, "
				+"ofdReaderNodeSelect,ofdReaderDefaultNode,replaceZW4Th"
				+ " from workflow_createdoc where workflowId=?" , workflowId);
		if (rs.next()) {
			baseSettingsMap = RecordSetToMapUtil.recordsToMap(rs);
		}
		return baseSettingsMap;
	}

	/**
	 * 根据节点Ids获取节点数据
	 * @param nodeIds 节点Ids
	 * @param workflowId 流程Id
	 * @param fieldLabel 字段标签
	 * @param fieldName 字段名
	 * @return SearchConditionItem
	 */
    private SearchConditionItem getNodesByIds(String nodeIds, String workflowId,int fieldLabel,String fieldName) {
        SearchConditionItem condition = conditionFactory.createCondition(ConditionType.BROWSER, fieldLabel ,fieldName , "workflowNode");
        condition.setFieldcol(this.FIELD_COL_VALUE);
        condition.setLabelcol(this.LABEL_COL_VALUE);
        condition.setViewAttr(2);

        BrowserBean nodeBrowserBean = condition.getBrowserConditionParam();
        nodeBrowserBean.setIsSingle(false);
        nodeBrowserBean.setViewAttr(2);
        nodeBrowserBean.setTitle(SystemEnv.getHtmlLabelName(fieldLabel, user.getLanguage()));
        nodeBrowserBean.setIcon("icon-coms-workflow");
        nodeBrowserBean.setIconBgcolor("#0079DE");
        nodeBrowserBean.getDataParams().put("workflowid", workflowId);
        nodeBrowserBean.getDataParams().put("noNeedActiveWfId", "1");
        nodeBrowserBean.getDataParams().put("notNeedFreeNode", 1);
        nodeBrowserBean.getDestDataParams().put("workflowid", workflowId);
        nodeBrowserBean.getDestDataParams().put("noNeedActiveWfId", "1");
        nodeBrowserBean.getCompleteParams().put("workflowid", workflowId);
        nodeBrowserBean.getCompleteParams().put("noNeedActiveWfId", "1");
        nodeBrowserBean.getCompleteParams().put("notNeedFreeNode", 1);
        if(nodeIds != null && nodeIds.trim().length() != 0) {
            List<Map<String , Object>> replaceDatas = nodeBrowserBean.getReplaceDatas();
            String[] nodeIdArray = nodeIds.split(",");
            for(String nodeId : nodeIdArray) {
                Map<String , Object> replaceData = new HashMap<String , Object>();
                replaceData.put("id", nodeId);
                replaceData.put("name", this.getNodeNameById(nodeId));
                replaceDatas.add(replaceData);
            }
        }
        return condition;
    }

    /**
     * 获取节点名称
     * @param nodeId
     * @return
     */
	private Object getNodeNameById(String nodeId) {
        String name = "";
        RecordSet rs = new RecordSet();
        String sql = "SELECT nodeName FROM workflow_nodebase WHERE id = ?";
        rs.executeQuery(sql , nodeId);
        if(rs.next()) {
            name = rs.getString("nodeName");
        }
        return name;
	}
	/**
	 * 公文过程
	 * */
	private SearchConditionItem getOfficalTypeItem(int value){
		SearchConditionItem officalTypeItem = conditionFactory.createCondition(ConditionType.SELECT, 23775 , "officalType" );
		RecordSet rs = new RecordSet();
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
		options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(26528, user.getLanguage()), false));
		options.add(new SearchConditionOption("3", SystemEnv.getHtmlLabelName(33683, user.getLanguage()), false));
		options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(33682, user.getLanguage()), false));
		//多选改成了单选
		if(value <= 0){
			value = 1;
		}else if(value > 3){
			value = 1;
		}
		officalTypeItem.setValue(Util.null2String(value));
		officalTypeItem.setOptions(options);
		officalTypeItem.setDetailtype(3);
		officalTypeItem.setFieldcol(this.FIELD_COL_VALUE);
		officalTypeItem.setLabelcol(this.LABEL_COL_VALUE);
		return officalTypeItem;
	}

	/**
	 *  获取 :1.默认打开正文节点，2.清稿节点，3.打印节点 设置项 4,自动排版的节点
	 * @param isTextInForm 是否为正文显示在表单的选项
	 * @param object 选择框选项
	 * @param itemName 选择标识
	 * @param searchConditionItem 节点浏览按钮
	 * @return
	 */
	private SearchConditionItem getBaseSettingSeleInfo(int isTextInForm,String object,int labelId,String itemName,SearchConditionItem searchConditionItem) {
		SearchConditionItem docTypeChangeNumberItem = conditionFactory.createCondition(ConditionType.SELECT_LINKAGE, labelId , itemName);
		docTypeChangeNumberItem.setFieldcol(this.FIELD_COL_VALUE);
		docTypeChangeNumberItem.setLabelcol(this.LABEL_COL_VALUE);

		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();

		if (isTextInForm == 0){
			//空选项
			options.add(new SearchConditionOption("0", "" , "0".equals(object)));
		}
		//全部
		options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(332,user.getLanguage()) , "1".equals(object)));
		//选择
		options.add(new SearchConditionOption("2", SystemEnv.getHtmlLabelName(172,user.getLanguage()) , "2".equals(object)));
		docTypeChangeNumberItem.setOptions(options);
		docTypeChangeNumberItem.setViewAttr(2);

		//联动节点浏览按钮
		Map<String, SearchConditionItem> selectLinkageDatas = new HashMap<String, SearchConditionItem>();
		selectLinkageDatas.put("2", searchConditionItem);
		docTypeChangeNumberItem.setSelectLinkageDatas(selectLinkageDatas);

		return docTypeChangeNumberItem;
	}


	private SearchConditionItem getOfdNodeSelect(String object,int labelId,String itemName,SearchConditionItem searchConditionItem) {
		SearchConditionItem docTypeChangeNumberItem = conditionFactory.createCondition(ConditionType.SELECT_LINKAGE, labelId , itemName);
		docTypeChangeNumberItem.setFieldcol(this.FIELD_COL_VALUE);
		docTypeChangeNumberItem.setLabelcol(this.LABEL_COL_VALUE);

		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();

		//空选项
		options.add(new SearchConditionOption("-1", "" , "-1".equals(object)));
		//数科阅读器
		options.add(new SearchConditionOption("0", SystemEnv.getHtmlLabelName(	535108,user.getLanguage()) , "0".equals(object)));
		//	福昕阅读器
		options.add(new SearchConditionOption("1", SystemEnv.getHtmlLabelName(	535109,user.getLanguage()) , "1".equals(object)));
		docTypeChangeNumberItem.setOptions(options);
		docTypeChangeNumberItem.setViewAttr(2);

		//联动节点浏览按钮
		Map<String, SearchConditionItem> selectLinkageDatas = new HashMap<String, SearchConditionItem>();
		selectLinkageDatas.put("0", searchConditionItem);
		selectLinkageDatas.put("1", searchConditionItem);
		docTypeChangeNumberItem.setSelectLinkageDatas(selectLinkageDatas);

		return docTypeChangeNumberItem;
	}
	private List<SearchConditionOption> getNodeOptions(String workflowid) {
		List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
		options.add(new SearchConditionOption("-1", ""));
		RecordSet rs = new RecordSet();
		StringBuffer printNodesSb2=new StringBuffer();
		printNodesSb2.append(" select a.nodeId,b.nodeName,a.nodeType ")
				.append(" from  workflow_flownode a,workflow_nodebase b ")
				.append(" where (b.IsFreeNode is null or b.IsFreeNode!='1') and a.nodeId=b.id ")
				.append("   and a.workflowId=?")
				.append(" and a.nodeType!=3 ")
				.append(" order by a.nodeType asc,a.nodeId asc ")
		;

		rs.executeQuery(printNodesSb2.toString(),workflowid);
		while(rs.next()) {
			String nodeid =rs.getString("nodeId");
			options.add(new SearchConditionOption(nodeid, rs.getString("nodeName")));
		}
		return options;
	}
}
