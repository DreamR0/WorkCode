<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page import="weaver.conn.*"%>
<%@ page import="weaver.general.*"%>
<%@ page import="weaver.hrm.*" %>
<%@ page import="java.util.regex.Matcher,java.util.regex.Pattern" %>
<%@ page import="java.util.Map,java.util.HashMap,java.util.Hashtable,java.util.List,java.util.ArrayList" %>
<%@ page import="weaver.docs.pdf.docpreview.ConvertPDFUtil"%>
<%@ page import="com.api.doc.detail.service.DocDetailService"%>
<%@ page import="weaver.docs.category.SecCategoryDocPropertiesComInfo"%>
<%@ page import="weaver.docs.category.SecCategoryComInfo"%>
<%@ page import="weaver.docs.mould.MouldManager"%>
<%@ page import="weaver.systeminfo.SystemEnv"%>
<%@ page import="com.api.doc.detail.service.DocViewPermission"%>
<%@ page import="weaver.docs.category.security.MultiAclManager"%>
<%@ page import="java.util.*"%>
<%@ page import="java.net.*"%>
<%@ page import="com.alibaba.fastjson.JSONObject"%>
<%@ page import="com.engine.odoc.util.OdocUtil" %>
<%@ page import="weaver.file.Prop" %>

<%@ page import="com.api.doc.search.util.DocSptm"%>
<%@page import="com.api.doc.search.service.DocLogService"%>
<%@page import="com.api.doc.category.service.CategoryService"%>
<%@ page import="com.weaver.cssRenderHandler.JsonUtils" %>
<%@ page import="com.engine.doc.util.DocPlugUtil" %>
<%@ page import="com.engine.odoc.util.OdocTemplateSharedUtil" %>
<%@ page import="com.engine.odoc.web.OdocFileAction" %>
<%@ page import="weaver.common.StringUtil" %>
<%@ page import="com.engine.odoc.biz.odocSettings.QysOfficeSignSettingBiz" %>
<%@ page import="com.engine.odoc.biz.odocSettings.QysPdfSingleSignSettingBiz" %>
<%@ page import="com.engine.odoc.util.DocUtil" %>
<%@ page import="com.api.workflow.service.RequestAuthenticationService" %>
<%@ page import="weaver.workflow.agent.AgentManager" %>

<%@ page import="org.apache.commons.lang.time.DateFormatUtils" %>
<%@ page import="com.api.doc.detail.util.ImageConvertUtil" %>
<%@ page import="com.api.doc.detail.util.DocDetailUtil" %>
<jsp:useBean id="DocComInfo" class="weaver.docs.docs.DocComInfo" scope="page"/>
<jsp:useBean id="DocPreviewHtmlManager" class="weaver.docs.docpreview.DocPreviewHtmlManager" scope="page"/>
<jsp:useBean id="DepartmentComInfo" class="weaver.hrm.company.DepartmentComInfo" scope="page"/>
<jsp:useBean id="SecCategoryComInfo" class="weaver.docs.category.SecCategoryComInfo" scope="page" />
<jsp:useBean id="ResourceComInfo" class="weaver.hrm.resource.ResourceComInfo" scope="page"/>
<jsp:useBean id="CustomerInfoComInfo" class="weaver.crm.Maint.CustomerInfoComInfo" scope="page" />
<jsp:useBean id="LanguageComInfo" class="weaver.systeminfo.language.LanguageComInfo" scope="page" />
<jsp:useBean id="DocMouldComInfo" class="weaver.docs.mouldfile.DocMouldComInfo" scope="page" />
<jsp:useBean id="BaseBean" class="weaver.general.BaseBean" scope="page" />
<jsp:useBean id="WorkflowBarCodeSetManager" class="weaver.workflow.workflow.WorkflowBarCodeSetManager" scope="page" />
<jsp:useBean id="TexttoPDFManager" class="weaver.workflow.request.TexttoPDFManager" scope="page" />
<jsp:useBean id="ic" class="com.api.doc.detail.util.ImageConvertUtil" scope="page" />
<%
	User user = HrmUserVarify.getUser (request , response) ;
	if(user == null ){
		return;
	}
	response.setHeader("Pragma", "no-cache");
	response.setHeader("Cache-Control", "no-cache");
	response.setDateHeader("Expires", 0);

	long time0 = 0;
	long time1 = 0;
	long time2 = 0;
	long time3 = 0;
	Date d0 = new Date();
	
    String jsVersion = DateFormatUtils.format(d0,"yyyyMMdd");
    String CONTEXTPATH = weaver.general.GCONST.getContextPath();//url、src、api请求都请加上此参数,统一处理二级路径
	// 文档ID
	int docid = Util.getIntValue(request.getParameter("id"),-1);
    int imagefileId = Util.getIntValue(request.getParameter("imagefileId"),-1);
	BaseBean.writeLog("===================================imagefileId:"+imagefileId);
    int versionId = Util.getIntValue(request.getParameter("versionId"),-1);
    String extendName =  (Util.null2String(request.getParameter("extendName"))).toLowerCase(); //后缀名
    boolean isEdit = "1".equals(request.getParameter("isEdit"));	//是否是新建或者编辑
    boolean isCheckedOut = "1".equals(request.getParameter("isCheckedOut"));	//是否签出
    boolean isEditOffice = "1".equals(request.getParameter("isOffice")); //是否是 office
    String officeType = Util.null2String(request.getParameter("officeType")); //新建office类型
    String odocfileType = Util.null2String(request.getParameter("odocfileType"));//公文正文类型
	boolean canViewForWpsForLinux = "true".equals(request.getParameter("canViewForWpsForLinux"));
	boolean canViewForWpsForDocCenter = "true".equals(request.getParameter("canViewForWpsForDocCenter"));
    String authStr = Util.null2String(request.getParameter("authStr"));//授权信息
    String authSignatureStr = Util.null2String(request.getParameter("authSignatureStr"));//授权信息
    String isColumnShow = Util.null2String(request.getParameter("isColumnShow"));//是否分栏显示
    String isTextInForm = Util.null2String(request.getParameter("isTextInForm"));//是否显示在表单
	//是否必须留痕
	String isCompellentMark = Util.null2String(request.getParameter("isCompellentMark"));
	//是否取消审阅
	String isCancelCheck = Util.null2String(request.getParameter("isCancelCheck"));
	//是否默认隐藏痕迹
	String isHideTheTraces = Util.null2String(request.getParameter("isHideTheTraces"));
    BaseBean.writeLog("==============================authStr="+authStr+"authSignatureStr="+authSignatureStr+"=========================");
    String f_weaver_belongto_userid = Util.null2String(request.getParameter("f_weaver_belongto_userid"));
    String f_weaver_belongto_usertype = Util.null2String(request.getParameter("f_weaver_belongto_usertype"));
    BaseBean.writeLog("0==============================f_weaver_belongto_userid="+f_weaver_belongto_userid+"f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"=========================");
    if(StringUtil.isNull(f_weaver_belongto_userid)){
        f_weaver_belongto_userid = user.getUID()+"";
    }
    if(StringUtil.isNull(f_weaver_belongto_usertype)){
        f_weaver_belongto_usertype = user.getType()+"";
    }
    BaseBean.writeLog("1==============================f_weaver_belongto_userid="+f_weaver_belongto_userid+"f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"=========================");
	int workflowid = Util.getIntValue(request.getParameter("workflowid"),-1);
	int requestid = Util.getIntValue(request.getParameter("requestid"),-1);
	int secFieldid = Util.getIntValue(request.getParameter("secFieldid"),-1);
	//编辑模板参数
	int editMouldFieldid = Util.getIntValue(request.getParameter("editMouldFieldid"),-1);
	String isSelectField = Util.null2String(request.getParameter("isSelectField"));
	//套红模板参数
	int showMouldFieldid = Util.getIntValue(request.getParameter("showMouldFieldid"),-1);
	String isShowSelectField = Util.null2String(request.getParameter("isShowSelectField"));
	String requestname = Util.null2String(request.getParameter("requestname"));
	int secid = Util.getIntValue(request.getParameter("secid"),-1);	//目录id

	int nodeid = Util.getIntValue(request.getParameter("nodeid"),-1);	//节点id
	int docFieldId = Util.getIntValue(request.getParameter("docFieldId"),-1);	//正文字段id
	int docNameField = Util.getIntValue(request.getParameter("documentTitleField"),-1);	//正文名称id
	int editMouldId = Util.getIntValue(request.getParameter("editMouldId"),-1);
	int showMouldId = Util.getIntValue(request.getParameter("showMouldId"),-1);
	int languageId = user.getLanguage();

	//打印模板ID
	int printMouldid = Util.getIntValue(request.getParameter("printMouldid"),-1);

	int documentTitleField = Util.getIntValue(request.getParameter("documentTitleField"),-1); //文档标题存放字段

	boolean isUseTempletNode = "true".equals(request.getParameter("isUseTempletNode"));
	String  hasUsedTemplet = "1".equals(request.getParameter("hasUsedTemplet")) ? "1" : "0";
	boolean isSignatureNodes = "true".equals(request.getParameter("isSignatureNodes"));
	boolean isCleanCopyNodes = "true".equals(request.getParameter("isCleanCopyNodes"));
	boolean isIwebOfficeNodes = "true".equals(request.getParameter("isIwebOfficeNodes"));
	
	RecordSet RecordSet = new RecordSet();
	boolean openyozo = false;
	boolean isAutoExtendInfo = false;  //有附件时展开文档附件属性
	int useyozo = 0;
	RecordSet.executeQuery("select useyozo from workflow_yozotopdfconfig");
	if(RecordSet.next())
	{
		useyozo = RecordSet.getInt("useyozo");
	}
	boolean openConverForClient = ic.convertForClient();
	String _clientAddress = ic.getConvertIp();

	boolean openYozoServer = false;
	if(openConverForClient && !_clientAddress.isEmpty())
	{
	    //单独服务开启
		openYozoServer = true;
		if(useyozo==1)
		{
			//转PDF开启单独服务
			openyozo = true;
		}
	}


	/*************************************获取参数开始***********************************/
	//文档目录
	int seccategory = -1;
	Map readOnlyMap = request.getParameterMap();
	String UrlParam = "";
	//遍历map中的键
	Iterator<Map.Entry<String, String[]>> entries = readOnlyMap.entrySet().iterator();

	//根据uuid从数据库获取参数
	String paramUuid = Util.null2String(request.getParameter("uuid"));
	String paramsString = "";
	if(!"".equals(paramUuid)){
		RecordSet rs =  new RecordSet();
		rs.executeQuery("select uuid,urlparams from odoc_urlparam where uuid = ? ", paramUuid);
		if(rs.next()){
			paramsString = rs.getString("urlparams");
			rs.executeUpdate("delete from odoc_urlparam where uuid = ? ", paramUuid);
		}
	}
	RecordSet.executeQuery("select max(label) cversion from ecologyuplist");
	String cversion = "";
	if(RecordSet.next()){
	    cversion =Util.null2String(RecordSet.getString("cversion"));
	}

	JSONObject paramMap = new JSONObject();
	if(paramsString.length() > 0){
		paramMap = JSONObject.parseObject(paramsString);
	}
	paramMap.put("requestId",""+requestid);
	paramMap.put("workflowId",""+workflowid);
	paramMap.put("languageId",""+languageId);
	paramMap.put("editMouldId",""+editMouldId);
	while (entries.hasNext()) {
		Map.Entry<String, String[]> entry = entries.next();
		if(entry.getKey() != null && !entry.getKey().equals("")){
			String requestValue = "";
			if(entry.getValue().length==1){
				requestValue = entry.getValue()[0];
			} else {
				String[] values = entry.getValue();
				for(int i=0; i<values.length; i++){
					requestValue = values[i] + ",";
				}
				requestValue = requestValue.substring(0, requestValue.length()-1);
			}
			//获取正文存放目录
			if("secid".equals(entry.getKey())){
				seccategory = Util.getIntValue(requestValue,0);
			}
			if(!UrlParam.equals("")) UrlParam += "&";
			UrlParam += entry.getKey() + "=" + requestValue;
			String url = requestValue.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
			String urlStr = URLEncoder.encode(url, "UTF-8");
			paramMap.put(entry.getKey(),urlStr);
		}
	}
	if(seccategory>0){
		RecordSet rs =  new RecordSet();
		rs.executeSql("select isOpenAttachment,isAutoExtendInfo from docseccategory where id=" + seccategory);
		if(rs.next()){
			isAutoExtendInfo = "1".equals(rs.getString("isAutoExtendInfo"));
		}
	}
		BaseBean.writeLog("=============paramMap:"+paramMap);
	/*************************************获取参数结束***********************************/

	//开启WPS转PDF与否
	boolean openWpsToPdf = "true".equals(request.getParameter("openWpsToPdf"));
	//是否使用iWebOfficeChina
	boolean isNewObject = "true".equals(request.getParameter("isNewObject"));
	//webwps
	boolean webwps = "true".equals(request.getParameter("webwps"));
	//webwps4Windows
	boolean webwps4Windows = "true".equals(request.getParameter("webwps4Windows"));
	//永中Office
	boolean yozoOffice = "true".equals(request.getParameter("yozoOffice"));
	//Office2015
	boolean office2015 = "1".equals(Util.null2String(DocPlugUtil.getoffice215Set().get("isopen")));
	//WPS服务端编辑器
	boolean wpsEditor = "true".equals(request.getParameter("wpsEditor"));
	boolean canEditForWpsDocCenter = "true".equals(request.getParameter("canEditForWpsDocCenter"));
	//永中在线编辑
	boolean yozoEditor = "true".equals(request.getParameter("yozoEditor"));
	//WPS预览
	boolean wpsView = "true".equals(request.getParameter("wpsView"));
	//永中预览
	boolean yozoView = "true".equals(request.getParameter("yozoView"));
	boolean useYozoOrWPS = "1".equals(Util.null2String(request.getParameter("useyozoorwps"))) && OdocUtil.createNodeEdit(requestid + "", nodeid + "");
    //是否开启html展示
    boolean htmlshow = "1".equals(BaseBean.getPropValue("doc_html_for_weaver", "htmlshow"));
	//金格iWebPDF
	boolean iWebPDF = "true".equals(request.getParameter("iWebPDF"));
	//金格iWebPDF2018
    boolean iWebPDF2018 = "true".equals(request.getParameter("iWebPDF2018"));
	//是否可以编辑PDF（正文可编辑节点）
	boolean canEditPDF = "true".equals(request.getParameter("canEditPDF"));
	boolean hideDownload = "true".equals(request.getParameter("hideDownload"));
	//是否打开了金格iwebpdf窗口
	boolean iWebPDFIframe = false;
	//隐藏保存按钮
	boolean hideSaveButton = false;
	//自开发无插件编辑器
	boolean noplugninText = "true".equals(request.getParameter("noplugninText"));
	//是否开启0309
	boolean isUse0309 = true;

	//自动智能排版
	boolean autoSmartOfficial = "true".equals(request.getParameter("autoSmartOfficial"));
	//手动智能排版
	boolean manualSmartOfficial = "true".equals(request.getParameter("manualSmartOfficial"));
	
	//是否预览不走2015
    boolean isNotUseView2015 = "1".equals(BaseBean.getPropValue("docpreview","isNotUseView2015"));
	
	String agent = (String)request.getHeader("user-agent");
	if (office2015 && agent.contains(" Chrome") && !isEdit && isNotUseView2015) {
		htmlshow = true;
		office2015 = false;
	}


	BaseBean.writeLog("=======zx=======htmlshow=="+htmlshow+"==office2015=="+office2015+"==Chrome=="+agent.contains(" Chrome")+"==isNotUseView2015=="+isNotUseView2015+"==isEdit=="+isEdit);
	if(yozoEditor && isIwebOfficeNodes){
		yozoEditor = false;
	}
	
	if(office2015 || webwps4Windows || wpsEditor || yozoEditor || noplugninText){
		isUse0309 = false;
	}
	//是否开启office2015
	if(webwps4Windows || wpsEditor || yozoEditor){
		office2015 = false;
	}
	//是否开启windows环境WPS插件
	if(wpsEditor || yozoEditor){
		webwps4Windows = false;
	}
	//开启契约锁签章与否
	boolean isOpenQysSign = "true".equals(request.getParameter("isOpenQysSign"));
	//是否契约锁签章节点
	boolean isQysSignNode = "true".equals(request.getParameter("isQysSignNode"));
	//是否流程创建节点
	boolean isStartNode = "true".equals(request.getParameter("isStartNode"));
	//选择编辑模板后是否保留痕迹
	boolean saveRevision = "true".equals(request.getParameter("saveRevision"));
	//是否保存清稿前痕迹版本
	boolean needSaveVersion = "true".equals(request.getParameter("needSaveVersion"));
	//是否是从表单上打开
	String requestdocInForm = Util.null2String(request.getParameter("requestdocInForm"));

	String agent1 = (String)request.getHeader("user-agent");
	int systemtype = 1;
	if(agent1.contains("Windows")){
		systemtype = 0;
	}
	//是否开启按权限选择套红模板 yjy add 20200219
	OdocTemplateSharedUtil odocTemplateSharedUtil = new OdocTemplateSharedUtil();
	boolean isPermission = odocTemplateSharedUtil.getisPermissionTemp(workflowid);
	String isPermissionTemp = "";
	if (isPermission){
		isPermissionTemp = "1";
	}
	if ("1".equals(isPermissionTemp)){
		//判断是否开启了按权限选择模板，流程操作者拿到对应的模板id
		showMouldId = odocTemplateSharedUtil.getTemplateSharedId(user,systemtype,workflowid,requestid);
	}

	boolean officeDocument = DocUtil.isOfficeDocument(extendName);

	//若开启了WPS服务端编辑器，则调用方法替换书签，并返回结果
	if(wpsEditor && wpsView && isEdit && officeDocument){
		Map<String,Object> paramsWpsMap = new HashMap<String,Object>();
		paramsWpsMap.put("billParam",paramMap);
		if(docid > 0){
			RecordSet rs = new RecordSet();
			rs.executeQuery("select imagefileId from docimagefile where docid=? and (isextfile is null or isextfile != '1') order by versionid desc",docid);
			if(rs.next()){
				imagefileId = rs.getInt(1);
			}
		}
		paramsWpsMap.put("imagefileId",imagefileId);
		//获取返回值
		Map<String,String> rtnMap = new OdocFileAction().getFile(paramsWpsMap,user);
		imagefileId =  Util.getIntValue(rtnMap.get("rtnImagefileId"));
		extendName = Util.null2String(rtnMap.get("extendName"));
	}else if(yozoEditor && isEdit && officeDocument){
		Map<String,Object> paramsWpsMap = new HashMap<String,Object>();
		paramsWpsMap.put("billParam",paramMap);
		if(docid > 0){
			RecordSet rs = new RecordSet();
			rs.executeQuery("select imagefileId from docimagefile where docid=? and (isextfile is null or isextfile != '1') order by versionid desc",docid);
			if(rs.next()){
				imagefileId = rs.getInt(1);
			}
		}
		paramsWpsMap.put("imagefileId",imagefileId);
		//获取返回值
		Map<String,String> rtnMap = null;
		try{
			rtnMap = new OdocFileAction().templateByYozo(paramsWpsMap,user);
		}catch (Exception e){
			rtnMap = new HashMap<String,String>();
			rtnMap.put("rtnImagefileId",imagefileId+"");
			rtnMap.put("extendName",extendName);
			e.printStackTrace();
			BaseBean.writeLog("==========index_odocNew.jsp exception:"+e);
		}
		imagefileId =  Util.getIntValue(rtnMap.get("rtnImagefileId"));
		extendName = Util.null2String(rtnMap.get("extendName"));
	}

	//自动排版
	String errorMsg = "";
	if(autoSmartOfficial){
		Map<String,String> tempParamMap = new HashMap<String,String>();
		tempParamMap.put("workflowId",workflowid+"");
		tempParamMap.put("requestId",requestid+"");
		tempParamMap.put("nodeId",nodeid+"");
		tempParamMap.put("docid",docid+"");
		tempParamMap.put("imagefileId",imagefileId+"");
		tempParamMap.put("hasUsedTemplet",hasUsedTemplet);
		tempParamMap.put("isUseTempletNode",isUseTempletNode+"");
		Map<String,String> rtnMap = new OdocFileAction().autoSmartOfficial(user,tempParamMap);
		if("1".equals(rtnMap.get("status"))){
			imagefileId = Util.getIntValue(rtnMap.get("rtnImagefileId"),-1);
			extendName = rtnMap.get("extendName");
		}else{
			errorMsg = rtnMap.get("message");
		}
	}
	//是否排版过
	boolean previewAfterSmarted = DocUtil.isSmarted(requestid, docid);
	if(previewAfterSmarted){
		isEdit = false;
	}

    //套红后的无插件正文显示地址(PDF)
    String templatedContent = "";
    //套红后生成的docx物理文件地址
    String templatedDocpath = "";
	//自开发无插件编辑器
    if(noplugninText){
		if("1".equals(hasUsedTemplet)){
			imagefileId = new OdocFileAction().convertToPDF(docid);
			BaseBean.writeLog("=======================convertedPDfId:"+imagefileId+ ",docid:"+docid);
			extendName = ".pdf";
			isEdit = false;
			isEditOffice = false;
			officeType = "12";
		}else{
			if(isUseTempletNode){
				request.setAttribute("showMouldId",showMouldId+"");
				request.setAttribute("docId",docid+"");
				//调用接口，生成套红后文档包括DOCX和PDF
				String returnData = new OdocFileAction().taohong(request,response);
				JSONObject json = JSONObject.parseObject(returnData);
				templatedContent = Util.null2String(json.get("useTempfilePreviewUrl"));
				templatedDocpath = Util.null2String(json.get("useTempfilePath"));
			}
		}
		BaseBean.writeLog("=======================isEdit:"+isEdit+"noplugninText");
	}

	String qysPdfViewerUrl = "";
	int qysDownloadid = Util.getIntValue(request.getParameter("qysDownloadid"),0);
	//契约锁签章
	if(isOpenQysSign){
	    //设置为签章节点和契约锁签署节点
	    if(isSignatureNodes && isQysSignNode){
			//调用契约锁接口返回的预览PDF地址
			qysPdfViewerUrl = Util.null2String(request.getParameter("qysPdfViewerUrl"));
		}else{
			RecordSet qysRs = new RecordSet();
			if(qysDownloadid > 0){
				qysRs.executeQuery("select imagefileid from docImagefile where docid=? and (isextfile is null or isextfile != '1') order by versionid desc",qysDownloadid);
				if(qysRs.next()){
					imagefileId = qysRs.getInt(1);
				}
				docid = qysDownloadid;
				extendName = ".pdf";
				isEdit = false;
				isEditOffice = false;
				officeType = "12";
		   }
		   qysRs.writeLog("--------index_odoc.jsp---qysRs.isEdit"+isEdit+"契约锁签章");
	    }

	}

	RecordSet recordSetKG = new RecordSet();
	/**金格PDF签章展示，如果中间表odoc_kinggrid_sign存在对应的数据，则展示中间表中对应的数据 QC624465*/
	int kgPdfDocId = -1;
	try{
		recordSetKG.writeLog("--------index_odoc.jsp-----替换前docid"+docid+"金格PDF签章展示开始！");
		if(docid>0){
			recordSetKG.executeQuery("select pdfdocid from odoc_kinggrid_sign where olddocid=?",docid);
			if(recordSetKG.next()){
				kgPdfDocId = recordSetKG.getInt(1);
				recordSetKG.writeLog("--------index_odoc.jsp-----kgPdfDocId"+kgPdfDocId+"===");
				if(kgPdfDocId>0){
					recordSetKG.executeQuery("select imagefileid,versionId from docimagefile where docid=? order by versionid desc",kgPdfDocId);
					if(recordSetKG.next()){
						imagefileId = recordSetKG.getInt(1);
						versionId = recordSetKG.getInt(2);
					}
					docid = kgPdfDocId;
					extendName = ".pdf";
					isEdit = false;
					isEditOffice = false;
					officeType = "12";
				}
			}
		}
	}catch (Exception e){
		recordSetKG.writeLog("Exception:",e);
	}
	recordSetKG.writeLog("--------index_odoc.jsp---isEdit"+isEdit+"金格PDF签章");
	recordSetKG.writeLog("--------index_odoc.jsp---替换后docid"+docid+"金格PDF签章展示结束！");

	//文档编辑状态，0:非修订状态,不保护文档；1，修订状态，不保护文档；2，保护文档，锁定状态
	String mEditType = "0";
	if(docid < 0){
		mEditType = "0";
	}else if(isEdit){
		mEditType = "1";
	}else{
		mEditType = "0";
	}

	//是否契约锁Office单体签章
	boolean isQysOfficeSign  = QysOfficeSignSettingBiz.isUseQysOfficeSign(extendName);
	//是否契约锁PDF单体签章
	boolean isQysPdfSingleSign  = QysPdfSingleSignSettingBiz.isUseQysPdfSingleSign(extendName);
	//获取公文督办所需参数
	int formId=0;
	String isBill="0";
	if(workflowid>0){
		recordSetKG.executeQuery("select formid,isbill from workflow_base where id="+workflowid);
		if(recordSetKG.next()){
			formId=Util.getIntValue(recordSetKG.getString("formid"), 0);
			isBill=recordSetKG.getString("isbill");
			if(!isBill.equals("1")){
				isBill="0";
			}
		}
	}

	//已办事宜，不允许编辑
    String logintype = f_weaver_belongto_usertype;
    int operatortype = 0;
    if (logintype.equals("1")) operatortype = 0;
    if (logintype.equals("2")) operatortype = 1;
    String isremark = "0";
	String beforwardid = "";	
	String takisremark = "";
    boolean istoManagePage = false;
    String nodetype = "";
    if(requestid>0&&user.getUID()>0){
        recordSetKG.executeProc("workflow_Requestbase_SByID", requestid + "");
        if (recordSetKG.next()) {
            nodetype = Util.null2String(recordSetKG.getString("currentnodetype"));
        }
        recordSetKG.executeSql("select id,isremark,takisremark from workflow_currentoperator where requestid=" + requestid + " and userid=" + f_weaver_belongto_userid + " and usertype=" + operatortype + " order by isremark");
        while (recordSetKG.next()) {
			beforwardid = Util.null2String(recordSetKG.getString("id"));	
            isremark = Util.null2String(recordSetKG.getString("isremark"));
			takisremark = Util.null2String(recordSetKG.getString("takisremark"));
            //0：未操作&&非归档节点 9：抄送(需提交) ||h: 转办||j: 转办提交 ||1 2：意见征询("1".equals(isremark)&&"2".equals(takisremark))
            if (("0".equals(isremark) && !nodetype.equals("3"))||("1".equals(isremark)&&"2".equals(takisremark))||"9".equals(isremark)||"h".equals(isremark)||"j".equals(isremark)) {
                istoManagePage = true;
                break;
            }
			//意见征询可编辑
			if(!beforwardid.equals("")){
				RecordSet recordSetlm = new RecordSet();
				recordSetlm.executeQuery("select 1 from  workflow_forward where requestid = ? and beforwardid=?",requestid,beforwardid);
				if(recordSetlm.next()){
					istoManagePage = true;
					break;					
				}
			}				
        }
    }

    //代理权限验证
    RequestAuthenticationService authService = new RequestAuthenticationService();
    authService.setUser(user);
    RecordSet recordSet1 = new RecordSet();
    recordSet1.writeLog("--------index_odoc_router.jsp---代理权限认证start");
    if (authService.verify(request, requestid)) {
        Map<String, Object> authInfo = authService.getAuthInfo();
        int agentType = Util.getIntValue(Util.null2String(authInfo.get("agentType")));
        int agentorByAgentId = Util.getIntValue(Util.null2String(authInfo.get("agentorByAgentId")));
        if (agentType == 1) {//如果流程被代理出去，被代理人也要能办理
            RecordSet recordSetInner = new RecordSet();
            String sql = AgentManager.getAgentorSql(agentorByAgentId, user.getUID(), requestid);
            recordSet1.writeLog("--------index_odoc_router.jsp---代理权限认证sql="+sql+"istoManagePage="+istoManagePage);
            recordSetInner.executeQuery(sql);
            if (recordSetInner.next()) {
                String tempIsremark = recordSetInner.getString("isremark");
                String tempNodeType = Util.null2String(authInfo.get("nodetype"));
                if (tempIsremark.equals("1") || tempIsremark.equals("5") || tempIsremark.equals("7") || tempIsremark.equals("9") || (tempIsremark.equals("0") && !tempNodeType.equals("3")) || tempIsremark.equals("11")) {
                    istoManagePage = true;
                }
            }
        }else if(agentType == 2&&!istoManagePage){
			//暂时调整逻辑为代理已办里打开公文流程正文，作为待办来打开
			istoManagePage = true;
		}
    }


    recordSetKG.writeLog("--------index_odoc.jsp---认证,istoManagePage="+istoManagePage+"isEdit="+isEdit);
    if(!istoManagePage&&requestid>0&&docid>0){
        isEdit = false;
    }
    recordSetKG.writeLog("--------index_odoc.jsp---认证1,istoManagePage="+istoManagePage+"isEdit="+isEdit);



	//流程新建完成，关闭当前页面
	String isIE = (String)session.getAttribute("browser_isie");
	if (isIE == null || "".equals(isIE)) {
		isIE = "true";
		session.setAttribute("browser_isie", "true");
	}
	
	if((agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") )|| agent.contains("Edge")){
		isIE = "false";
	}else{
		isIE = "true";
	}
	boolean isChrome = agent.contains(" Chrome");
	boolean isQysPdfSingleSinBrowser = QysPdfSingleSignSettingBiz.isQysPdfSingleSignBrowser(isIE,agent);
	int qysPdfSingleSignMaxSize = QysPdfSingleSignSettingBiz.getQysPdfSingleSignMaxSize();
	boolean isQysPdfSingleSignSize = true;
	//判断客户端是否Windows操作系统
	boolean isWindows = false;
	//判断客户端是否MacOS操作系统
	boolean isMac = false;
	if(agent.contains("Windows")){
		isWindows = true;
	}
	if(agent.contains("Mac")){
		isMac = true;
	}
%>
<script type="text/javascript">
	window.ecologyContentPath = "<%=CONTEXTPATH%>";
</script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/odoc/odoc/js/customLoadFunction.js?v=<%=jsVersion%>"></script>
<script>
    <%--var isNewObject = <%=isNewObject%>;--%>
    window.notThrowCss = true;
</script>
<%


	//客户端windows环境且非IE，则进查看页面
	//if(isIE.equals("false") && isWindows && !office2015) isEdit = false;
    new weaver.general.BaseBean().writeLog("docid================================" + docid );
    new weaver.general.BaseBean().writeLog("imagefileId================================" + imagefileId );
    new weaver.general.BaseBean().writeLog("isEdit================================" + isEdit );

    //Office 格式正文，且使用0309，未开启预览的情况
    if(isUse0309 && isEdit && isWindows && officeDocument && !"true".equals(isIE) && ((docid < 0 && imagefileId < 0) || (docid > 0 && !(wpsView || yozoView)))){
		response.sendRedirect(CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=27969");
	}
	
    // 附件版本ID
    if(docid < 0 && imagefileId < 0 && !isEdit)
    {
		response.sendRedirect(CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=20990") ;
        return ;
    }


    if(docid <= 0 && imagefileId > 0){
    	RecordSet rs = new RecordSet();
    	rs.executeQuery("select docid from DocImageFile where imagefileid=" + imagefileId + (versionId > 0 ? (" and versionid=" + versionId) : ""));
    	if(rs.next()){
    		docid = rs.getInt("docid");
    	}
    	if(docid < 0 && !wpsEditor && !yozoEditor){
    		response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
    	    return ;
    	}
    }

	//开启自动清稿
	boolean autoCleanCopy = false;
	boolean allowDisplayTab = false;
    //保存时返回流程表单
    boolean saveBackForm = false;
	//正文未保存离开时进行提醒
    boolean saveDocRemind = false;
	if(workflowid > 0){
		RecordSet rs = new RecordSet();
		rs.executeQuery("select autoCleanCopy,displayTab,isPermissionTemp,saveBackForm,saveDocRemind from workflow_createdoc where workflowid=?",workflowid);
		if(rs.next()){
			autoCleanCopy = "1".equals(rs.getString(1));
			allowDisplayTab = "1".equals(rs.getString(2));
			saveBackForm = "1".equals(rs.getString(4));
			saveDocRemind = "1".equals(rs.getString(5));
		}
	}

	String useNew = Util.null2String(request.getParameter("useNew")); //是否使用新的权限判断
	useNew = "1";
	Map<String,String> rightParams = new HashMap<String,String>();
	rightParams.put("useNew",useNew);

	boolean canEdit = false;
	DocViewPermission dvps = new DocViewPermission();

	//其他模块参数集
	String moudleParams = dvps.getMoudleParams(request);
    new weaver.general.BaseBean().writeLog("ghk==========user.getUID()=======================" + user.getUID());
	Map<String,Boolean> levelMap = dvps.getShareLevel(docid,user,false);
	if(!levelMap.get(DocViewPermission.READ)){ //知识没有权限，判断是否是其他模板
		levelMap.put(DocViewPermission.READ,dvps.hasRightFromOtherMould(docid,user,request));
	}
    new weaver.general.BaseBean().writeLog("ghk==========levelMap.get(DocViewPermission.READ)======================" + levelMap.get(DocViewPermission.READ));
    //正文属性为只读时，是否可以用编辑器打开
    boolean canIWebOffice = true;
    //开启正文默认预览后，是否显示编辑按钮
    boolean useIwebOfficeEdit = true;
    //开启wps无插件编辑器（wpsEditor==true），正文只读时强制预览
    //开启wps预览（wpsEditor==true），正文只读时强制预览
    if(wpsEditor
			|| yozoEditor || isMac
			|| (isWindows && webwps4Windows && !"true".equals(isIE))
			|| (isWindows && office2015 && !"true".equals(isIE) && !agent.contains("Firefox") && !agent.contains(" Chrome"))
			|| (isWindows && isUse0309 && !"true".equals(isIE))
			|| (!isWindows && !isNewObject && !webwps && !yozoOffice)
	){
		canIWebOffice = false;
		useIwebOfficeEdit = false;
	}
	//没有开启wps无插件编辑器（wpsEditor==false），只是单独开启WPS预览,且不是签章节点，正文只读时强制预览
	if(!wpsEditor && !yozoEditor && (wpsView || yozoView) && (!istoManagePage||isCheckedOut) && !isEdit){
	    canIWebOffice = false;
	}
	//PDF正文使用非金格控件方式打开
	if(StringUtil.isNotNull(extendName)&&".pdf".equalsIgnoreCase(extendName)){
	    canIWebOffice = false;
	}
	//获取保存后的编辑模板ID，提供给编辑页面使用
	int oldEditMouldId = -1;
	int editSelectValue = -1;
	if(isEdit){
		if(docid > 0){  //编辑
			RecordSet rs = new RecordSet();
			rs.executeQuery("select doctype,docextendname from DocDetail where id=" + docid);
			if(rs.next()){
				String doctype = rs.getString("doctype");
				String docextendname = "." + rs.getString("docextendname");
				if("2".equals(doctype)){

				}else{
					isEditOffice = false;
				}
			}

			canEdit = levelMap.get(DocViewPermission.EDIT);

			rs.executeQuery("select editMouldId,editSelectValue from odoc_docMouldInfo where requestId = ?",requestid);
			if (rs.next()){
				oldEditMouldId = Util.getIntValue(rs.getString(1),-1);
				editSelectValue = Util.getIntValue(rs.getString(2),-1);
			}
		}else if(secid > 0){//新增
			MultiAclManager am = new MultiAclManager();
			canEdit = am.hasPermission(secid, MultiAclManager.CATEGORYTYPE_SEC, user.getUID(), user.getType(),
					Integer.parseInt(user.getSeclevel()), MultiAclManager.OPERATION_CREATEDOC);
		}else{
			canEdit = true;
		}
		String requestDoc = Util.null2String(request.getParameter("requestDoc"));

		if(requestDoc.equals("1")){
			canEdit = true;
		}
		if(!canEdit){
			response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
			return ;
		}
		//增加到常用目录
		CategoryService categoryService = new CategoryService();
		categoryService.addCommonUse(secid,user);
	}

	boolean canRead = false;
	/**判断文档查看权限*/
	if(!isEdit && docid > 0){
		canRead = levelMap.get(DocViewPermission.READ);
	}


	if(!isEdit && !canRead){   //没有权限
		RecordSet rs = new RecordSet();
		rs.executeQuery("select ishistory,doceditionid from DocDetail where id=" + docid);
		int ishistory = 0;
		int doceditionid = 0;
		if(rs.next()){
			ishistory = rs.getInt("ishistory");
			doceditionid = rs.getInt("doceditionid");
		}
		if(ishistory != 1){  //不是历史文档
			response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
		}else{
			int newestid = 0;

			rs.executeQuery("select id from DocDetail where doceditionid = " + doceditionid + " and ishistory=0 and id<>"+docid+"  order by docedition desc ");
			if(rs.next()){
				newestid = rs.getInt("id");
			}

			if(newestid <= 0 || newestid == docid){
				response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
				return ;
			}

			String linkNewestDoc = DocSptm.DOC_DETAIL_LINK + "?id=" + newestid + moudleParams + DocSptm.DOC_ROOT_FLAG_VALUE+DocSptm.DOC_DETAIL_ROUT;
%>
<html>
<head>
	<script language=javascript>
        if(confirm("<%=SystemEnv.getHtmlLabelName(20300,user.getLanguage())%><%=SystemEnv.getHtmlLabelName(19986,user.getLanguage())%>")) {
            location="<%=linkNewestDoc%>";
        }else{
            location="<%=CONTEXTPATH%>/notice/noright.jsp";
        }
	</script>
</head>
<body></body>
</html>
<%
		}
		return;
	}

	Date d1 = new Date();
	time0 = d1.getTime() - d0.getTime();


	// 标题
	String docsubject = ""; //新建office类型
	//最后修改人id
	int doclastmoduserid = 0;
	// 最后修改人
	String username = "";
	// 修改日期+时间
	String modifyDate = "";
	// 修改日期
	String doclastmoddate = "";
	// 修改时间
	String doclastmodtime = "";
	// 加载内容
	String docContent = "";
	String docContentSrc = "";
	// 文档类型
	int doctype = -1;

	//文档状态
	String docstatus = "0";
	//失效日期
	String invalidationdate = "";
	//文档所属部门
	String docdepartmentid = "";
	//是否是历史文档
	int ishistory = -1;
	//版本号
	int doceditionid = -1;
	//最新版本id
	int newestid = -1;
	//新闻模板
	int selectedpubmouldid = -1;
	//创建人
	int doccreaterid = -1;
	//创建人类型
	String usertype = "";
	//创建日期
	String doccreatedate = "";
	// 文档语言
	int doclangurage = 7;
	//文档编号
	String docCode = "";
	//回复文档id
	int replydocid = 0;
	//审批日期
	String docapprovedate = "";
	//发布类型
	String docpublishtype = "";

	String prevname = SystemEnv.getHtmlLabelName(156,user.getLanguage());
	String imagefilename = "";

	boolean isOpenAcc = false; // 是否单附件打开
	String OpenAccOfFileid = "";   //单附件打开的附件id
	String OpenAccOfFilename = "";   //单附件打开的附件名称
	String OpenAccOfFiletype = "";   //单附件打开的附件类型
	String OpenAccOfVersionid = "";   //单附件打开的附件版本
	int OpenAccOfFileSize = 0;  //单附件打开的附件大小


	boolean showByHtml = false;  //是否是html模式展示
	String convertFile = "";  //转换文件  空-不转换，html-转成html，pdf-转成pdf


	boolean canPrint = dvps.getPrint(docid,user,levelMap,null);
	boolean canDownload = levelMap.get(DocViewPermission.DOWNLOAD);
	//选择显示模版的ID
	String selectedpubmould = Util.null2String(request.getParameter("selectedpubmould"));

    //针对html文档进行特殊处理，通过让imagefileid=-1，让其能通过接口正常展示html文档 lmaoc
    if(docid>0){
        RecordSet rs =  new RecordSet();
        rs.executeQuery("select doctype from docdetail where id=?",docid);
        if(rs.next()){
            doctype = Util.getIntValue(rs.getString("doctype"),-1);
        }
        if(doctype==1){
            imagefileId=-1;
			isEdit = false;
        }
    }

	if(isEdit){

	}else if(imagefileId > 0){

	}else{
		DocLogService docLogService = new DocLogService();
		String ipAddress = request.getRemoteAddr();
		//docLogService.addReadLog(docid,user,ipAddress);

		List<String> columns = new ArrayList<String>();
		columns.add("doctype");
		columns.add("seccategory");
		columns.add("docstatus");
		columns.add("invalidationdate");
		columns.add("docdepartmentid");
		columns.add("ishistory");
		columns.add("doceditionid");
		columns.add("selectedpubmouldid");
		columns.add("doccreaterid");
		columns.add("usertype");
		columns.add("doccreatedate");
		columns.add("doclangurage");
		columns.add("docCode");
		columns.add("replydocid");
		columns.add("docapprovedate");
		columns.add("docpublishtype");

		DocDetailService detailService = new DocDetailService();
		Map<String,Object> baseInfo = detailService.getBasicInfoNoRight(docid,user,columns);
		Date d2 = new Date();
		time1 = d2.getTime() - d1.getTime();
		if(baseInfo != null && baseInfo.get("data") != null){
			Map data = (Map)baseInfo.get("data");

			docsubject = Util.null2String(data.get("docSubject").toString(),"");
			doclastmoduserid = Util.getIntValue(data.get("doclastmoduserid").toString(),0);
			username = Util.null2String(data.get("doclastmoduser").toString(),"");
			modifyDate = Util.null2String(data.get("doclastmoddatetime").toString(),"");
			doclastmoddate = Util.null2String(data.get("doclastmoddate").toString(),"");
			doclastmodtime = Util.null2String(data.get("doclastmodtime").toString(),"");
			doctype = Util.getIntValue(data.get("doctype").toString(),-1);
			seccategory = Util.getIntValue(data.get("seccategory").toString(),-1);
			docstatus = Util.null2String(data.get("docstatus").toString(),"");
			invalidationdate = Util.null2String(data.get("invalidationdate").toString(),"");
			docdepartmentid = Util.null2String(data.get("docdepartmentid").toString(),"");
			ishistory = Util.getIntValue(data.get("ishistory").toString(),-1);
			doceditionid = Util.getIntValue(data.get("doceditionid").toString(),-1);
			selectedpubmouldid = Util.getIntValue(data.get("selectedpubmouldid").toString(),-1);
			doccreaterid = Util.getIntValue(data.get("doccreaterid").toString(),-1);
			usertype = Util.null2String(data.get("usertype").toString(),"");
			doccreatedate = Util.null2String(data.get("doccreatedate").toString(),"");
			doclangurage = Util.getIntValue(data.get("doclangurage").toString(),7);
			docCode = Util.null2String(data.get("docCode").toString(),"");
			doclangurage = Util.getIntValue(data.get("replydocid").toString(),0);
			docapprovedate = Util.null2String(data.get("docapprovedate").toString(),"");
			docpublishtype = Util.null2String(data.get("docpublishtype").toString(),"");
		}

		RecordSet rs = new RecordSet();
		rs.executeSql("select isOpenAttachment,isAutoExtendInfo from docseccategory where id=" + seccategory);
		if(rs.next()){
			if("1".equals(rs.getString("isOpenAttachment")) && doctype == 1){ //目录是否开启单附件打开
				isOpenAcc = true;
			}
			isAutoExtendInfo = "1".equals(rs.getString("isAutoExtendInfo"));
		}
		//html文档
		if(doctype == 1){
			docContent = detailService.getDocContent(docid,user);
			Date d3 = new Date();
			time2 = d3.getTime() - d2.getTime();
			//if(contentMap != null && contentMap.get("data") != null){
			//	Map data = (Map)contentMap.get("data");
			//	docContent = Util.null2String(data.get("doccontent"));
			//}
			String mainimagefileid = DocDetailUtil.getMainImagefile(docid+"");
			Map<String,String> mainFileMap = new HashMap<String,String>();
			if("".equals(mainimagefileid)){
				mainFileMap = DocDetailUtil.getDocFirstfile(docid+"");
			}else{
				mainFileMap = DocDetailUtil.getFirstfileInfo(mainimagefileid);
			}
			if(DocDetailService.ifContentEmpty(docContent)){
				//根据目录判断是否单附件直接打开,附件类型是不是word,excel,pdf，ppt

				if(isOpenAcc){  //目录是否开启单附件打开
					if(!"".equals(Util.null2s(mainFileMap.get("imagefileid"),""))){
						OpenAccOfFileid = mainFileMap.get("imagefileid");
						OpenAccOfFilename = mainFileMap.get("imagefilename");
						OpenAccOfFiletype = mainFileMap.get("docfiletype");
						OpenAccOfVersionid = mainFileMap.get("versionId");
						OpenAccOfFileSize = Util.getIntValue(mainFileMap.get("filesize"));
					}else{
						rs.executeSql("select a.id,a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from docimagefile a,imagefile b " +
								" where a.imagefileid=b.imagefileid and a.docid=" + docid + " order by versionid desc");
						String id = "";
						int fileCount = 0;
						while(rs.next()){   //是否是单附件
							fileCount++;
							if("".equals(id)){
								id = rs.getString("id");
								OpenAccOfFileid = rs.getString("imagefileid");
								OpenAccOfFilename = rs.getString("imagefilename");
								OpenAccOfFiletype = rs.getString("docfiletype");
								OpenAccOfVersionid = rs.getString("versionId");
								OpenAccOfFileSize = rs.getInt("filesize");
							}
							if(!id.equals(rs.getString("id"))){
								isOpenAcc = false;
							}

						}

						String extname = OpenAccOfFilename.indexOf(".") > -1 ? OpenAccOfFilename.substring(OpenAccOfFilename.lastIndexOf(".")+ 1) : "";

						if("html".equals(extname.toLowerCase())){
							isOpenAcc = true;
						}
						if(fileCount == 0){
							isOpenAcc = false;
						}
					}
				}
			}else{
				isOpenAcc = false;
			}


			if(!isOpenAcc){
				showByHtml = true;
				SecCategoryDocPropertiesComInfo scdpc = new SecCategoryDocPropertiesComInfo();

				int docmouldid = -1;

				if(scdpc.getDocProperties(""+seccategory,"10") && "1".equals(scdpc.getVisible())){
					rs.executeSql("select t1.* from DocSecCategoryMould t1 right join DocMould t2 on t1.mouldId = t2.id where t1.secCategoryId = "+seccategory+" and t1.mouldType=1 order by t1.id");
					int selectMouldType = 0;
					int selectDefaultMould = 0;
					while(rs.next()){
						String moduleid=rs.getString("mouldId");
						String modulebind = rs.getString("mouldBind");
						int isDefault = Util.getIntValue(rs.getString("isDefault"),0);

						if(invalidationdate !=null && !"".equals(invalidationdate)){

							if(Util.getIntValue(modulebind,1)==3){
								selectMouldType = 3;
								selectDefaultMould = Util.getIntValue(moduleid);
							} else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
								if(selectMouldType==0){
									selectMouldType = 1;
									selectDefaultMould = Util.getIntValue(moduleid);
								}
							}

						} else {

							if(Util.getIntValue(modulebind,1)==2){
								selectMouldType = 2;
								selectDefaultMould = Util.getIntValue(moduleid);
							} else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
								selectMouldType = 1;
								selectDefaultMould = Util.getIntValue(moduleid);
							}
						}
					}

					if(user != null && HrmUserVarify.checkUserRight("DocEdit:Publish",user,docdepartmentid) && docstatus.equals("6") && (ishistory!=1) && docstatus.equals("6")){
						if(Util.getIntValue(Util.null2String(selectedpubmould),0)<=0){
							if(selectMouldType>0){
								docmouldid = selectDefaultMould;
							}
						} else {
							docmouldid = Util.getIntValue(selectedpubmould);
						}
					} else {
						if(Util.getIntValue(Util.null2String(selectedpubmould),0)<=0){
							if(selectedpubmouldid<=0){
								if(selectMouldType>0)
									docmouldid = selectDefaultMould;
							} else {
								docmouldid = selectedpubmouldid;
							}
						} else {
							docmouldid = Util.getIntValue(selectedpubmould);
						}
					}
				}

				MouldManager mouldManager = new MouldManager();
				if(docmouldid<=0)
					docmouldid = mouldManager.getDefaultMouldId();
				mouldManager.setId(docmouldid);
				mouldManager.getMouldInfoById();
				String mouldtext = mouldManager.getMouldText();

				int languageid = user.getLanguage();
				Hashtable<String,String> hr = new Hashtable<String,String>();
				SecCategoryComInfo scci = new SecCategoryComInfo();
				hr.put("DOC_SecCategory",Util.null2String(scci.getSecCategoryname(""+seccategory)));
				hr.put("DOC_Department",Util.null2String("<a href= "+CONTEXTPATH+ "/hrm/company/HrmDepartmentDsp.jsp?id="+docdepartmentid+"'>"+Util.toScreen(DepartmentComInfo.getDepartmentname(""+docdepartmentid),languageid)+"</a>"));
				hr.put("DOC_Content",Util.null2String(docContent));

				if(usertype.equals("2"))  {
					hr.put("DOC_CreatedBy",Util.null2String(Util.toScreen(CustomerInfoComInfo.getCustomerInfoname(""+doccreaterid),languageid)));
					hr.put("DOC_CreatedByLink",Util.null2String("<a href= "+CONTEXTPATH+ "/CRM/data/ViewCustomer.jsp?CustomerID="+doccreaterid+"'>"+Util.toScreen(CustomerInfoComInfo.getCustomerInfoname(""+doccreaterid),languageid)+"</a>"));
					hr.put("DOC_CreatedByFull",Util.null2String(Util.toScreen(CustomerInfoComInfo.getCustomerInfoname(""+doccreaterid),languageid)));
				}else {
					hr.put("DOC_CreatedBy",Util.null2String(Util.toScreen(ResourceComInfo.getFirstname(""+doccreaterid),languageid)));
					hr.put("DOC_CreatedByLink",Util.null2String("<a href='javaScript:openhrm("+doccreaterid+");' onclick='pointerXY(event);'>"+Util.toScreen(ResourceComInfo.getResourcename(""+doccreaterid),languageid)+"</a>"));
					hr.put("DOC_CreatedByFull",Util.null2String(Util.toScreen(ResourceComInfo.getResourcename(""+doccreaterid),languageid)));
				}

				hr.put("DOC_CreatedDate",Util.null2String(doccreatedate));
				hr.put("DOC_DocId",Util.null2String(Util.add0(docid,12)));
				hr.put("DOC_ModifiedBy",Util.null2String(Util.toScreen(ResourceComInfo.getFirstname(""+doclastmoduserid),languageid)));
				hr.put("DOC_ModifiedDate",Util.null2String(doclastmoddate));
				hr.put("DOC_Language",Util.null2String(LanguageComInfo.getLanguagename(""+doclangurage)));
				hr.put("DOC_ParentId",Util.null2String(Util.add0(replydocid,12)));
				String docstatusname = DocComInfo.getStatusView(docid,languageid);
				hr.put("DOC_Status",Util.null2String(docstatusname));
				hr.put("DOC_Subject",Util.null2String(docsubject));
				String tmppublishtype="";
				if(docpublishtype.equals("2")) tmppublishtype=SystemEnv.getHtmlLabelName(227,languageid);
				else if(docpublishtype.equals("3")) tmppublishtype=SystemEnv.getHtmlLabelName(229,languageid);
				else tmppublishtype=SystemEnv.getHtmlLabelName(58,languageid);
				hr.put("DOC_Publish",Util.null2String(tmppublishtype));
				hr.put("DOC_ApproveDate",Util.null2String(docapprovedate));
				hr.put("DOC_NO", Util.null2String(docCode)) ;

				String doccontentbackgroud="";
				int strindex = docContent.indexOf("data-background=");
				if(strindex!=-1){
					strindex = docContent.indexOf("\"", docContent.indexOf("data-background="))+1;
					doccontentbackgroud=docContent.substring(strindex, docContent.indexOf("\"", strindex));
				}

				if("".equals(doccontentbackgroud)){
					int strindextemp=mouldtext.indexOf("data-background=");
					if(strindextemp!=-1){
						strindextemp=mouldtext.indexOf("\"", strindextemp)+1;
						doccontentbackgroud=mouldtext.substring(strindextemp, mouldtext.indexOf("\"", strindextemp));
					}
				}

				mouldManager.closeStatement();
				mouldtext = Util.fillValuesToString(mouldtext,hr);

				docContent = mouldtext != null ? mouldtext : docContent;
			}

		}
	}
	recordSetKG.writeLog("recordSetKG=========isEdit="+isEdit+"doctype="+doctype+"isOpenAcc="+isOpenAcc+"====isUse0309:"+isUse0309+"========");

	BaseBean.writeLog("=====================canIWebOffice:"+canIWebOffice+"====isSignatureNodes:"+isSignatureNodes+"====isUseTempletNode:"+isUseTempletNode+"======noplugninText:"+noplugninText);
	boolean isOfdFile = ".ofd".equals(request.getParameter("extendName"));
	// office文件：正文word、excel   或者是单附件打开

	new BaseBean().writeLog("==zj==(isEdit)"+isEdit);
	//==zj 如果是ie浏览器预览默认走poi
	new BaseBean().writeLog("==zj==(isIe)" + isIE);
	if ("true".equals(isIE)){
		htmlshow = true;
		canIWebOffice = false;
	}
	if((!isEdit && (doctype == 2 || doctype == 12 || doctype == 13 || isOpenAcc || imagefileId > 0)) || isOfdFile) {
		// 附件ID
		String imageFileid = "";
		// 模板ID
		String mTemplate = "";
		// 附件名称
		String imageFileName="";
		// 附件类型
		String fileType="";
		// 操作用户
		String mUserName = user == null ? "" : user.getLastname();
		// 编辑状态
		String editType = "0";
		// 附件类型标识
		int docFileType = 0;
		// 附件大小
		int filesize = 0;
		// 是否文件过大
		boolean isToLarge = false;
		//最大文件大小
		int maxSize = 20*1024*1024;
		//最大excle大小
		int maxExcleSize = 15*1024*1024;


		RecordSet rs = new RecordSet();
		String sql = "";

		if(imagefileId > 0){
			if(docid>0){
				sql = "select a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from DocImageFile a,Imagefile b " +
						" where a.imagefileid=b.imagefileid and a.docid="+docid+" and (a.isextfile <> '1' or a.isextfile is null)"+
						(versionId > 0 ? " and a.versionId=" + versionId : "")
						+" order by versionId desc";
			}else{
				sql = "select a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from DocImageFile a,Imagefile b " +
						" where a.imagefileid=b.imagefileid and a.imagefileid="+imagefileId+
						(versionId > 0 ? " and a.versionId=" + versionId : "")
						+" order by versionId desc";				
			}					
			rs.executeSql(sql);
			if(rs.next()){
				imageFileName = Util.null2String(rs.getString("imagefilename"),"");
				docFileType = Util.getIntValue(rs.getString("docfiletype"),0);
				filesize = Util.getIntValue(rs.getString("filesize"),0);
				versionId = Util.getIntValue(rs.getString("versionId"),0);
			}
			imageFileid = imagefileId + "";
			imagefilename = imageFileName;
			imagefilename.replaceAll("\"","＂");
		}else if(doctype == 2 || doctype == 12 || doctype == 13 || isOfdFile){  // office文件
			sql = "select a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from DocImageFile a,Imagefile b " +
					" where a.imagefileid=b.imagefileid and a.docid="+docid+" and (a.isextfile <> '1' or a.isextfile is null) order by a.versionId desc";
			rs.executeSql(sql);
			if(rs.next())
			{
				imageFileid = Util.null2String(rs.getString("imagefileid"),"");
				imageFileName = Util.null2String(rs.getString("imagefilename"),"");
				docFileType = Util.getIntValue(rs.getString("docfiletype"),0);
				versionId = Util.getIntValue(rs.getString("versionId"),0);
				filesize = Util.getIntValue(rs.getString("filesize"),0);
			}
			if(!wpsEditor && !yozoEditor) {
				imagefileId = Util.getIntValue(imageFileid);
			}
		}else if(isOpenAcc){  //单附件打开

			imageFileid = OpenAccOfFileid;
			imageFileName = OpenAccOfFilename;
			docFileType = Util.getIntValue(OpenAccOfFiletype,0);
			versionId = Util.getIntValue(OpenAccOfVersionid,0);
			filesize = OpenAccOfFileSize;
			imagefileId = Util.getIntValue(imageFileid);
		}

		String extname = imageFileName.indexOf(".") > -1 ? imageFileName.substring(imageFileName.lastIndexOf(".")+ 1) : "";
		extname = extname.toLowerCase();

		odocfileType = docFileType+"";

		isQysPdfSingleSignSize = QysPdfSingleSignSettingBiz.isQysPdfSingleSignSize(filesize,qysPdfSingleSignMaxSize);
		if(extname.equals("pdf")){
			//==zj 这里加个判断如果是ie浏览器，pdf就走默认预览方式-- !"true".equals(isIE)
			String pdfConvert = BaseBean.getPropValue("doc_custom_for_weaver", "pdfConvert");
			boolean convertForClient = ic.convertForClient();
			if(isQysPdfSingleSign&&isQysPdfSingleSinBrowser){//如果需要做契约锁的pdf单体签章，就直接用pdf的阅读器打开
				docContent = "<iframe id='webOffice'  src=\""+CONTEXTPATH+"/odoc/odoc/qysPdf/qyspdf_iframecontent.jsp?canPrint="+canPrint+"&requestid="+requestid+"&signNode="+isSignatureNodes+"&nodeid="+nodeid+"&authStr="+authStr+"&authSignatureStr="+authSignatureStr+"&canDownload="+canDownload+"&pdfimagefileid="+imageFileid+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			}else if(!"true".equals(isIE) && isWindows && ((iWebPDF && "true".equals(isIE)) || (iWebPDF2018 && ("true".equals(isIE) || isChrome)))){
				iWebPDFIframe = true;
				String sessionParaPDF=""+docid+"_"+imagefileId+"_"+user.getUID()+"_"+user.getLogintype();
				session.setAttribute("canView_"+sessionParaPDF,"1");
				session.setAttribute("canEdit_"+sessionParaPDF,canEditPDF ? "1" : "0");
				session.setAttribute("canPrint_"+sessionParaPDF,canPrint ? "1" : "0");
				docContentSrc = CONTEXTPATH+"/docs/e9/iwebpdf.jsp?docid="+docid +"&imagefileid="+imagefileId;
				docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/docs/e9/iwebpdf.jsp?requestid="+requestid+"&authSignatureStr="+authSignatureStr+"&authStr="+authStr+"&f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"&f_weaver_belongto_userid="+f_weaver_belongto_userid+"&docid="+docid +"&imagefileid="+imagefileId + "\" style=\"display: block;width: 100%;height: 100%; border:0\" scrolling=\"no\"></iframe>";
			}else if(DocPlugUtil.isopenbjca() && "true".equals(isIE)){
				new BaseBean().writeLog("==zj==(pdf默认方法)");
				docContentSrc = CONTEXTPATH+"/docs/pdfofca/pdfShow.jsp?docid="+docid+"&imagefileId="+imagefileId;
				docContent = "<iframe id='webOffice'  src='"+CONTEXTPATH+"/docs/pdfofca/pdfShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
			}else if("1".equals(pdfConvert) && convertForClient){	//移动端只判断pdfConvert，PC端需要同时判断单独转换服务器不为空时，pdf也会在单独服务器预览
				if(!(wpsView||yozoView)){//未开启预览服务
					convertFile = "pdf";
					docContentSrc = CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996";
					docContent = "<iframe id=\"webOffice\" src=\"/docs/pdfview/web/sysRemind.jsp?labelid=996\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
				}
			}else{
				docContentSrc = CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"&f_weaver_belongto_userid="+f_weaver_belongto_userid+"&canPrint="+canPrint+"&requestid="+requestid+"&authStr="+authStr+"&authSignatureStr="+authSignatureStr+"&canDownload="+canDownload+"&pdfimagefileid="+imageFileid;
				docContent = "<iframe id='webOffice'  src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"&f_weaver_belongto_userid="+f_weaver_belongto_userid+"&canPrint="+canPrint+"&requestid="+requestid+"&authStr="+authStr+"&authSignatureStr="+authSignatureStr+"&canDownload="+canDownload+"&pdfimagefileid="+imageFileid+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			}
		}else if (extname.equals("ofd")) { //yjy add uploadOFD
			//OFD阅读器类型
			String readerType = OdocUtil.getUseReaderType(nodeid,requestid,user);
			//是否使用插件阅读OFD文档
			boolean useOFDReader = false;
			if((readerType.equals("0") ) || (readerType.equals("1") ) || (readerType.equals("2")) || (readerType.equals("3"))){
				useOFDReader = true;
			}

			if(useOFDReader){
				docContentSrc = CONTEXTPATH+"/docs/ofd/OfdShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"&isEdit="+canEdit+"&requestid="+requestid+"&authStr="+authStr+"&authSignatureStr="+authSignatureStr;
				docContent = "<iframe id='webOffice' src=\""+CONTEXTPATH+"/docs/ofd/OfdShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"&isEdit="+canEdit+"&requestid="+requestid+"&authStr="+authStr+"&authSignatureStr="+authSignatureStr+"\" style=\"display: block;width: 100%;height: 100%; border:0\" scrolling=\"no\"></iframe>";
			}else if((canViewForWpsForLinux || canViewForWpsForDocCenter || ImageConvertUtil.canConvertType(extname)) && ImageConvertUtil.canConvertType(extendName.substring(1))){
				hideSaveButton = true;
				docContentSrc = CONTEXTPATH + "/odoc/odoc/index_odoc4formShow.jsp?id="+docid+"&useYozoOrWpsShow=true";
				docContent = "<iframe id='webOffice' src=\""+CONTEXTPATH+"/odoc/odoc/index_odoc4formShow.jsp?useYozoOrWpsShow=true&id="+docid+"\" style=\"display: block;width: 100%;height: 100%; border:0\" scrolling=\"no\"></iframe>";
			}else{
				String sysremindurl=CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=516247";
				docContentSrc = sysremindurl;
				docContent = "<iframe src=\""+sysremindurl+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			}
		}else if(extname.equals("jpg") || extname.equals("jpeg") || extname.equals("png") || extname.equals("gif")){
			docContent = "<table style='width:100%;height:100%;background-color:#ffffff'><tr><td style='height:100%; vertical-align:middle; text-align:center;'><img style='vertical-align:middle;' src='/weaver/weaver.file.FileDownload?fileid=" + imageFileid + "'/></td></tr></table>";
		}else if (isOpenQysSign && isSignatureNodes && isQysSignNode && !"".equals(qysPdfViewerUrl)) {
			docContentSrc = qysPdfViewerUrl;
			docContent = "<iframe id='webOffice' src='" + qysPdfViewerUrl + "' style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
		}else if(!canIWebOffice && ((wpsView || openYozoServer|| htmlshow) && (!isSignatureNodes||(isSignatureNodes&&!istoManagePage)) && (!isUseTempletNode||(isUseTempletNode&&!istoManagePage)))){//调整逻辑签章、套红只读权限采用预览方式
			new BaseBean().writeLog("==zj==(方法有进入吗)");
			//是否开启PDF转换
			boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
			// 采用转换PDF预览
			if(isUsePDFViewer &&!htmlshow &&
					(extname.equals("doc") ||
							extname.equals("docx") ||
							extname.equals("xls") ||
							extname.equals("xlsx") ||
							extname.equals("ppt") ||
							extname.equals("pptx") ||
							extname.equals("wps") ||
							extname.equals("txt") ||
							extname.equals("sql") ||
							extname.equals("json") ||
							extname.equals("js") ||
							extname.equals("css") ||
							extname.equals("log")
					))
			{

				if(wpsView){
					convertFile = "pdf";
					docContentSrc =CONTEXTPATH+ "/docs/pdfview/web/sysRemind.jsp?labelid=996";
					docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
				}else{
					int pdffileid = -1;
					sql = "select pdfimagefileid from pdf_imagefile where imagefileid = " + imageFileid;
					rs.executeSql(sql);
					if(rs.next()){
						pdffileid = Util.getIntValue(rs.getString("pdfimagefileid"),0);
					}
					else
					{
						if((extname.equals("xlsx") || extname.equals("xls")) && filesize > maxExcleSize){
							isToLarge = true;
						}else if(filesize > maxSize){
							isToLarge = true;
						}

						if(!isToLarge){
							convertFile = "pdf";
						}
					}

					if(isToLarge){
						docContentSrc = CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=999";
						//文件过大
						docContent = "<iframe src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=999\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}else if(pdffileid > 0){
						docContentSrc = CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"&f_weaver_belongto_userid="+f_weaver_belongto_userid+"&canPrint="+canPrint+"&canDownload=" + canDownload + moudleParams + "&pdfimagefileid="+pdffileid;
						docContent = "<iframe id='webOffice'  src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?f_weaver_belongto_usertype="+f_weaver_belongto_usertype+"&f_weaver_belongto_userid="+f_weaver_belongto_userid+"&canPrint="+canPrint+"&canDownload=" + canDownload + moudleParams + "&pdfimagefileid="+pdffileid+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}else{
						docContentSrc = CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996";
						docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}
	            }
            }// 采用转换html查看
            else if(htmlshow && (extname.equals("doc") ||
                    extname.equals("docx") ||
                    extname.equals("xls") ||
                    extname.equals("xlsx")))
            {
            	new BaseBean().writeLog("==zj==(html查看)");
                showByHtml = true;
                convertFile = "html";
				docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			} else{
				//不支持的格式
				String sysremindurl=CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=129757";
				if(levelMap.get(DocViewPermission.DOWNLOAD)){
					sysremindurl=CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=516247";
				}
				docContentSrc = sysremindurl;
				docContent = "<iframe src=\""+sysremindurl+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";

			}
		} else {
			mTemplate = selectedpubmouldid + "";
			String mFileType = "";
			if(docFileType == 3 || "doc".equals(extname))
			{
				mFileType = ".doc";
			}
			else if(docFileType == 7 || "docx".equals(extname))
			{
				mFileType = ".docx";
			}
			else if(docFileType == 4 || "xls".equals(extname))
			{
				mFileType = ".xls";
			}
			else if(docFileType == 6 || "wps".equals(extname))
			{
				mFileType = ".wps";
			}
			else if(docFileType == 8 || "xlsx".equals(extname))
			{
				mFileType = ".xlsx";
			}else if(docFileType == 14 || "uot".equals(extname))
			{
				mFileType = ".uot";
			}
 			//解决标题带特殊符号的时候，传递到控件加载页面时，url会被隔断的问题
			String docsubjects = "";
			docsubjects = URLEncoder.encode(docsubject, "UTF-8");
			String imageFileNames = "";
			imageFileNames = URLEncoder.encode(imageFileName, "UTF-8");
			if(!mFileType.isEmpty() || noplugninText) {
			    if (isOpenQysSign && (qysDownloadid > 0 || !"".equals(qysPdfViewerUrl))) {
					if (isSignatureNodes && isQysSignNode && !"".equals(qysPdfViewerUrl)) {
						docContentSrc = qysPdfViewerUrl;
						docContent = "<iframe id='webOffice' src='" + qysPdfViewerUrl + "' style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					} else if (qysDownloadid > 0) {
						docContentSrc = CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid=" + imagefileId;
						docContent = "<iframe id='webOffice' src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid=" + imagefileId + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}
				} else if (wpsEditor) {
			        //wps 在线
					docContentSrc =CONTEXTPATH+ "/docs/e9/weboffice_wps.jsp?mFileType=" + extendName + "&fromOdoc=1&fileid=" + imagefileId + "&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces;
					docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/docs/e9/weboffice_wps.jsp?mFileType=" + extendName + "&fromOdoc=1&fileid=" + imagefileId + "&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
			    } else if(yozoEditor){
					docContentSrc =CONTEXTPATH+ "/docs/e9/weboffice.jsp?mFileType="+extendName+"&fromOdoc=1&fileid="+imagefileId + "&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces;
					docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/docs/e9/weboffice.jsp?mFileType="+extendName+"&fromOdoc=1&fileid="+imagefileId+"&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
				}else if (noplugninText) {
					if(!"".equals(templatedContent)){
						docContentSrc = templatedContent;
						docContent = "<iframe id='webOffice' src='"+templatedContent+"' style='display: block;width: 100%;height: 100%; border:0;'></iframe>";
					}else if("1".equals(hasUsedTemplet)){
						docContentSrc =CONTEXTPATH+ "/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid="+imagefileId;
						docContent = "<iframe id='webOffice' src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid="+imagefileId+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}else{
						docContentSrc =CONTEXTPATH+ "/spa/odoc/static4OnlineEdit/index.html#/main/offical/onlineEdit";
						docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/spa/odoc/static4OnlineEdit/index.html#/main/offical/onlineEdit' style='display: block;width: 100%;height: 100%; border:0;'></iframe>";
					}
				}else if(isWindows){
			        if(webwps4Windows) {
						docContentSrc =CONTEXTPATH+ "/odoc/odoc/webwps/webwps_iframecontent.jsp?isWindows=1&nodetype="+nodetype;
						docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/webwps/webwps_iframecontent.jsp?isWindows=1&nodetype="+nodetype+"' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
					}else if(office2015){
						docContentSrc =CONTEXTPATH+ "/odoc/odoc/weboffice2015/weboffice2015_iframecontent.jsp?languageId="+user.getLanguage();
						docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/weboffice2015/weboffice2015_iframecontent.jsp?languageId="+user.getLanguage()+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
					}else if(isUse0309 && "true".equals(isIE)){
						docContentSrc =CONTEXTPATH+ "/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams;
						docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams + "' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
					}
				}else if(yozoOffice){
					docContentSrc =CONTEXTPATH+ "/odoc/odoc/yozoOffice/yozoOffice_iframecontent.jsp";
					docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/yozoOffice/yozoOffice_iframecontent.jsp' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
				}else if(webwps){
					docContentSrc =CONTEXTPATH+ "/odoc/odoc/webwps/webwps_iframecontent.jsp?nodetype="+nodetype;
					docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/webwps/webwps_iframecontent.jsp?nodetype="+nodetype+"' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
				}else if(isNewObject){
					docContentSrc =CONTEXTPATH+ "/odoc/odoc/iwebofficechina/OpenAndSave/OpenAndSave_Word3.jsp?isEdit=false&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames+"&FileType=" + officeType +"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams ;
					docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/iwebofficechina/OpenAndSave/OpenAndSave_Word3.jsp?isEdit=false&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames+"&FileType=" + officeType +"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams +"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
				}else if(isUse0309 && "true".equals(isIE)){
					docContentSrc =CONTEXTPATH+ "/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=false&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames+"&FileType=" + officeType +"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams;
					docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=false&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames+"&FileType=" + officeType +"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams + "' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
				}

			}else{
				//不支持的格式
				String sysremindurl=CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=129757";
				if(levelMap.get(DocViewPermission.DOWNLOAD)){
					sysremindurl=CONTEXTPATH+"/wui/common/page/sysRemind.jsp?labelid=516247";
				}
				docContentSrc = sysremindurl;
				docContent = "<iframe src=\""+sysremindurl+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
				//docContent = "<iframe src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=997\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";

			}
		}
	}

	//文件后缀名
	String suffix = "";

	//新建、编辑office
	if(isEdit && isEditOffice){
		// 模板ID
		String mTemplate = "";
		// 附件名称
		String imageFileName="";
		// 编辑状态
		String editType = "1";
		//文档类型
		String mFileType = "";

		RecordSet rs = new RecordSet();
		//rs.executeQuery("select ")
		if(docid > 0){
			String sql = "select a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from DocImageFile a,Imagefile b " +
					" where a.imagefileid=b.imagefileid and a.docid="+docid+" and (a.isextfile <> '1' or a.isextfile is null) order by a.versionId desc";
			rs.executeQuery(sql);
			if(!rs.next() && !noplugninText){
				response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
				return ;
			}
			int docFileType = rs.getInt("docfiletype");
			imageFileName = rs.getString("imagefilename");
			versionId = rs.getInt("versionId");
			if(!wpsEditor && !yozoEditor){
				imagefileId = rs.getInt("imagefileid");
			}
			if(docFileType == 3){
				mFileType = ".doc";
			} else if(docFileType == 7){
				mFileType = ".docx";
			} else if(docFileType == 4){
				mFileType = ".xls";
			} else if(docFileType == 8){
				mFileType = ".xlsx";
			} else if(docFileType == 5){
				mFileType = ".ppt";
			} else if(docFileType == 6){
				mFileType = ".wps";
			} else if(docFileType == 9){
				mFileType = ".pptx";
			} else if(docFileType == 12){
				mFileType = ".pdf";
			}else if(docFileType == 13){
				mFileType = ".ofd";
			}else if(docFileType == 14){
				mFileType = ".uot";
			}
			suffix = mFileType;
		}else{
			mFileType = officeType;
			if(mFileType.equals(".doc")||mFileType.equals(".xls")||mFileType.equals(".wps")||mFileType.equals(".et")){
				int  tempMouldType=4;//4：WORD编辑模版
				if(mFileType.equals(".xls")){
					tempMouldType=6;//6：EXCEL编辑模版
				}else if(mFileType.equals(".wps")){
					tempMouldType=8;//8：WPS文字编辑模版
				}else if(mFileType.equals(".et")){
					tempMouldType=10;//8：WPS表格编辑模版
				}
				int selectMouldType = 0;
				int mouldid = 0;
				rs.executeQuery("select * from DocSecCategoryMould where secCategoryId = "+secid+" and mouldType=" + tempMouldType + " order by id ");
				while(rs.next()){
					String moduleid=rs.getString("mouldId");
					String mType = DocMouldComInfo.getDocMouldType(moduleid);
					String modulebind = rs.getString("mouldBind");
					int isDefault = Util.getIntValue(rs.getString("isDefault"),0);
					if(Util.getIntValue(modulebind,1)==2){
						selectMouldType = 2;
						mouldid = Util.getIntValue(moduleid);
					} else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
						if(selectMouldType==0){
							selectMouldType = 1;
							mouldid = Util.getIntValue(moduleid);
						}
					}
				}
				mTemplate = mouldid > 0 ? (mouldid + "") : "";
			}
		}
        //解决标题带特殊符号的时候，传递到控件加载页面时，url会被隔断的问题
        String docsubjects = "";
        docsubjects = URLEncoder.encode(docsubject, "UTF-8");
        String imageFileNames = "";
        imageFileNames = URLEncoder.encode(imageFileName, "UTF-8");
		if (isOpenQysSign && (qysDownloadid > 0 || !"".equals(qysPdfViewerUrl))) {
			if (isSignatureNodes && isQysSignNode && !"".equals(qysPdfViewerUrl)) {
				docContentSrc = qysPdfViewerUrl;
				docContent = "<iframe id='webOffice' src='" + qysPdfViewerUrl + "' style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			} else if (qysDownloadid > 0) {
				docContentSrc =CONTEXTPATH+ "/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid=" + imagefileId;
				docContent = "<iframe id='webOffice' src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid=" + imagefileId + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			}
		}else if(wpsEditor){
			docContentSrc =CONTEXTPATH+ "/docs/e9/weboffice_wps.jsp?mFileType="+extendName+"&fromOdoc=1&fileid="+imagefileId + "&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces;
			docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/docs/e9/weboffice_wps.jsp?mFileType="+extendName+"&fromOdoc=1&fileid="+imagefileId+"&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
		}else if(yozoEditor){
			docContentSrc =CONTEXTPATH+ "/docs/e9/weboffice.jsp?mFileType="+extendName+"&fromOdoc=1&fileid="+imagefileId + "&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces;
			docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/docs/e9/weboffice.jsp?mFileType="+extendName+"&fromOdoc=1&fileid="+imagefileId+"&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck+"&isHideTheTraces="+isHideTheTraces+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
		}else if (noplugninText) {
			if(!"".equals(templatedContent)){
				docContentSrc = templatedContent;
				docContent = "<iframe id='webOffice' src='"+templatedContent+"' style='display: block;width: 100%;height: 100%; border:0;'></iframe>";
			}else if("1".equals(hasUsedTemplet)){
				docContentSrc =CONTEXTPATH+ "/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid="+imagefileId;
				docContent = "<iframe id='webOffice' src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?canPrint=true&canDownload=true&pdfimagefileid="+imagefileId+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
			}else{
				docContentSrc =CONTEXTPATH+ "/spa/odoc/static4OnlineEdit/index.html#/main/offical/onlineEdit";
				docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/spa/odoc/static4OnlineEdit/index.html#/main/offical/onlineEdit' style='display: block;width: 100%;height: 100%; border:0;'></iframe>";			}
		}else if(isWindows){
			if(webwps4Windows) {
				docContentSrc =CONTEXTPATH+ "/odoc/odoc/webwps/webwps_iframecontent.jsp?isWindows=1&nodetype="+nodetype;
				docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/webwps/webwps_iframecontent.jsp?isWindows=1&nodetype="+nodetype+"' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
			}else if(office2015){
				docContentSrc =CONTEXTPATH+ "/odoc/odoc/weboffice2015/weboffice2015_iframecontent.jsp?languageId="+user.getLanguage();
				docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/weboffice2015/weboffice2015_iframecontent.jsp?languageId="+user.getLanguage()+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
			}else if(isUse0309 && "true".equals(isIE)){
				docContentSrc =CONTEXTPATH+ "/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams;
				docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams + "' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
			}
		}else if(yozoOffice) {
			docContentSrc =CONTEXTPATH+ "/odoc/odoc/yozoOffice/yozoOffice_iframecontent.jsp?nodetype="+nodetype;
			docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/yozoOffice/yozoOffice_iframecontent.jsp?nodetype="+nodetype+"' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
		}else if(webwps){
			docContentSrc =CONTEXTPATH+ "/odoc/odoc/webwps/webwps_iframecontent.jsp?nodetype="+nodetype;
			docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/webwps/webwps_iframecontent.jsp?nodetype="+nodetype+"' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
		}else if(isNewObject){
			docContentSrc =CONTEXTPATH+ "/odoc/odoc/iwebofficechina/OpenAndSave/OpenAndSave_Word3.jsp?isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams;
			docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/iwebofficechina/OpenAndSave/OpenAndSave_Word3.jsp?isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams + "' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
		} else if(isUse0309 && "true".equals(isIE)){
			docContentSrc =CONTEXTPATH+ "/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams;
			docContent = "<iframe id='webOffice' src= '"+CONTEXTPATH+"/odoc/odoc/office_odoc_0309.jsp?docsubject="+docsubjects+"&doccreaterid="+doccreaterid+"&uid="+user.getUID()+"&isEdit=true&onRequestDoc=true&mRecordID="+versionId +"_"+docid+"&mTemplate=" + mTemplate +"&mFileName=" + imageFileNames +"&FileType=" + officeType+"&mFileType=" + officeType +"&mEditType=" + mEditType + moudleParams + "' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
		}else {
			if(!(useYozoOrWPS && imagefileId > 0)){
				response.sendRedirect(CONTEXTPATH + "/wui/common/page/sysRemind.jsp?labelid=534186");
				return;
			}
		}
	}


    if(StringUtil.isNotNull(extendName)&&".html".equalsIgnoreCase(extendName)){
        showByHtml = true;
    }
	if(showByHtml){
		if(docContent.contains("</body>")){
			docContent = docContent.replace("<body","<div ").replace("</body>","</div>");
		}
		docContent = "<div id=\"weaDocDetailHtmlContent\" style=\"height:100%\">" + docContent + "</div>";

		Pattern p = Pattern.compile("<link\\s[^>]+\\s?\\/>");
		Matcher m = p.matcher(docContent);

		while(m.find()){
			String link = m.group(0);
			docContent = docContent.replace(link, "");
		}
	}
	
	//特殊处理，若特殊节点，特殊页面docContent为空，则默认跳转预览页面
	if("".equals(docContent)){
		docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/index_odoc4formShow.jsp?id="+docid+"&useYozoOrWpsShow=true' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;'></iframe>";
		docContentSrc = CONTEXTPATH+"/odoc/odoc/index_odoc4formShow.jsp?id="+docid+"&useYozoOrWpsShow=true";
	}


	String reuqestUrl = "/odoc/odoc/index_odoc_router.jsp?"+request.getQueryString();
	//开启了默认预览且点击了编辑按钮后
	boolean fromOnEditDoc = "true".equals(request.getParameter("fromOnEditDoc"));
	boolean usePreview = false;

    //使用永中、wps预览正文。
    if(((useYozoOrWPS && docid>0 && (isEdit || isCheckedOut) && ConvertPDFUtil.isUsePDFViewer() && !fromOnEditDoc && (wpsView || yozoView) && !isSignatureNodes && !isUseTempletNode)||(((!isEdit||isCheckedOut)&&docid>0&&!isSignatureNodes&&!isUseTempletNode)&&(wpsView || yozoView) && ConvertPDFUtil.isUsePDFViewer() )) || (".pdf".equalsIgnoreCase(extendName) && useYozoOrWPS && !fromOnEditDoc && docid>0))//调整说明
	{
		if((StringUtil.isNotNull(extendName)&&!".html".equalsIgnoreCase(extendName) && !".ofd".equalsIgnoreCase(extendName) && !".pdf".equalsIgnoreCase(extendName))
		|| (".pdf".equalsIgnoreCase(extendName) && useYozoOrWPS)){
            docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/index_odoc4formShow.jsp?id="+docid+"&useYozoOrWpsShow=true' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;' scrolling='no'></iframe>";
            if(docContentSrc.indexOf("?")>-1)
            {
                docContentSrc += "&fromOnEditDoc=true";
            }else{
                docContentSrc += "?fromOnEditDoc=true";
            }
            usePreview = true;
            secFieldid = -1;
		}
	}

    if(previewAfterSmarted && officeDocument && "".equals(qysPdfViewerUrl) && qysDownloadid <= 0){
		docContent = "<iframe id='webOffice' src='"+CONTEXTPATH+"/odoc/odoc/index_odoc4formShow.jsp?imagefileId="+imagefileId+"&useYozoOrWpsShow=true' style='display: block;width: 100%;height: 100%; border:0;z-index:-10;' scrolling='no'></iframe>";
	}
	recordSetKG.writeLog("=previewAfterSmarted:"+previewAfterSmarted+",officeDocument:"+officeDocument+",qysPdfViewerUrl:"+qysPdfViewerUrl+",qysDownloadid:"+qysDownloadid);
    recordSetKG.writeLog("=useYozoOrWPS:"+useYozoOrWPS+"==isEdit:"+isEdit+"====docContent:"+docContent+"===wpsView:"+wpsView+"===yozoView:"+yozoView+"===extendName:"+extendName);	//    String htmlcontent = docContent;
	//    session.setAttribute("doccontent_" + user.getUID() + "_" + user.getLogintype() + "_" + docid,docContent);

	if(showByHtml){
		//   	docContent = "<iframe src=\""+CONTEXTPATH+"/spa/document/htmlcontent.jsp?docid=" + docid + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
	}

	Date d4 = new Date();
	time3 = d4.getTime()-d1.getTime();
	String linkNewestDoc = "";
	if(ishistory == 1){
		RecordSet rs = new RecordSet();
		rs.executeQuery("select id from DocDetail where doceditionid = " + doceditionid + " and ishistory=0 and id<>"+docid+"  order by docedition desc ");
		if(rs.next()){
			newestid = rs.getInt("id");
		}
		if(newestid > 0 && newestid != docid){
			linkNewestDoc = DocSptm.DOC_DETAIL_LINK + "?id=" + newestid + moudleParams + DocSptm.DOC_ROOT_FLAG_VALUE+DocSptm.DOC_DETAIL_ROUT;
		}else{
			linkNewestDoc =CONTEXTPATH+"/notice/noright.jsp";
		}
	}

	RecordSet rs1 = new RecordSet();
	String docFileName = "";
	rs1.executeQuery("select docSubject from docDetail where id=?",docid);
	if(rs1.next()){
		docFileName = rs1.getString("docSubject");
	}
	docFileName = docFileName.replaceAll("\"","＂");

%>
<!--<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">-->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">-->
<html>

<%@ include file="/docs/docs/PDF417ManagerConf.jsp" %>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="initial-scale=1.0, maximum-scale=2.0">
	<meta name="description" content="">
	<meta name="author" content="">
	<title><%= docsubject %></title>
	<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/moduleConfig.js"></script>
	<link rel="stylesheet" type="text/css" href="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/index.min.css?v=<%=jsVersion%>">
	<link rel="stylesheet" type="text/css" href="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/ecCom.min.css?v=<%=jsVersion%>">
	<link rel="stylesheet" type="text/css" href="<%=CONTEXTPATH%>/spa/document/static4Detail/index.css?v=<%=jsVersion%>">
	<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/jquery/jquery.min.js"></script>
	<script type="text/javascript" src="<%=CONTEXTPATH%>/odoc/odoc/docutils4Object.js?v=<%=jsVersion%>"></script>

    <%if(!allowDisplayTab){%>
		<style type="text/css">
			.wea-doc-detail .wea-new-top-req{
				display:none;
			}
			.wea-doc-detail .wea-right-menu-wrap .wea-new-top-req-wapper{
				padding-top:0px;
			}
			.wea-doc-detail-content{
				padding-bottom: 0px !important;
			}
			.wea-doc-detail-content .wea-doc-detail-content-text-sub{
				display: none;
			}
		</style>
		<script>
			$(function(){
				var tabKey = parent.window.wfform.getGlobalStore().tabKey;
				if(tabKey ==='odoc')
				{				
					parent.jQuery(".wf-req-odoc").height(parent.jQuery(".wea-new-top-req-content").height() - 20);
					parent.jQuery(".req-workflow-odoc").height(parent.jQuery(".wf-req-odoc").height());
				}
			})
		</script><%}%>
	<script>
        //var hasUsedTemplet = '0';
        var noSavePDF = "";
        var versionId = "";
        var imagefileId = "<%=imagefileId%>";
	</script>
	<%
		String mClientName=BaseBean.getPropValue("weaver_obj","iWebOfficeClientName");
		if(mClientName==null||mClientName.trim().equals("")){
			mClientName="iWebOffice2003.ocx#version=6,6,0,0";
		}
		String mClassId=BaseBean.getPropValue("weaver_obj","iWebOfficeClassId");
		if(mClassId==null||mClassId.trim().equals("")){
			mClassId="clsid:23739A7E-5741-4D1C-88D5-D50B18F7C347";
		}

		String isHandWriteForIWebOffice2009=BaseBean.getPropValue("weaver_obj","isHandWriteForIWebOffice2009");
		boolean isIWebOffice2006 = (mClientName.indexOf("iWebOffice2006")>-1||mClientName.indexOf("iWebOffice2009")>-1)?true:false;
		boolean isIWebOffice2003 = (mClientName.indexOf("iWebOffice2003")>-1)?true:false;
		String canPostil = "";
		if(isIWebOffice2006 == true){
			canPostil = ",0";
		}
		String isNoComment="";
		if(isIWebOffice2006){
			isNoComment="1".equals(isHandWriteForIWebOffice2009)?"false":"true";
		}

		//正文ImageFileId
		int fileId = 0;

		String bookmarkJson = "";
		//获取套红书签对应关系
		if(isUseTempletNode){
			JSONObject json = new JSONObject();
			RecordSet rs =  new RecordSet();
			rs.executeQuery("select b.name,a.bookMarkValue from DocMouldBookMark a,MouldBookMark b where" +
					" a.bookMarkId=b.id and a.docid=? and a.mouldId=?", docid,showMouldId);
			while(rs.next()){
				String key = rs.getString("name");
				String value = rs.getString("bookMarkValue");
				value = value.replaceAll("\"","＂");
				if(!"".equals(key)){
					json.put(key,value);
				}
			}
			bookmarkJson = json.toJSONString();
			bookmarkJson.replaceAll("\\'","\\\\'");

			rs.executeQuery("select imagefileId,imagefilename from docimagefile where docid=? and  ( isextfile is null or isextfile<>'1') order by versionid desc",docid);
			if(rs.next()){
				fileId = rs.getInt("imagefileId");
			}
		}

		//遍历map中的键
		entries = readOnlyMap.entrySet().iterator();

		//根据uuid从数据库获取参数
		if(!"".equals(paramUuid)){
			RecordSet rs =  new RecordSet();
			rs.executeQuery("select uuid,urlparams from odoc_urlparam where uuid = ? ", paramUuid);
			if(rs.next()){
				paramsString = rs.getString("urlparams");
				rs.executeUpdate("delete from odoc_urlparam where uuid = ? ", paramUuid);
			}
		}
		paramMap = new JSONObject();
		if(paramsString.length() > 0){
			paramMap = JSONObject.parseObject(paramsString);
		}
		while (entries.hasNext()) {
			Map.Entry<String, String[]> entry = entries.next();
			if(entry.getKey() != null && !entry.getKey().equals("")){
				String requestValue = "";
				if(entry.getValue().length==1){
					requestValue = entry.getValue()[0];
				} else {
					String[] values = entry.getValue();
					for(int i=0; i<values.length; i++){
						requestValue = values[i] + ",";
					}
					requestValue = requestValue.substring(0, requestValue.length()-1);
				}
				//获取正文存放目录
				if("secid".equals(entry.getKey())){
					seccategory = Util.getIntValue(requestValue,0);
				}
				if(!UrlParam.equals("")) UrlParam += "&";
				UrlParam += entry.getKey() + "=" + requestValue;
				String url = requestValue.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
				String urlStr = URLEncoder.encode(url, "UTF-8");
				paramMap.put(entry.getKey(),urlStr);
			}
		}


		String ifVersion= "";
		RecordSet.executeQuery("select useyozo from workflow_yozotopdfconfig");
		if(RecordSet.next())
		{
			useyozo = RecordSet.getInt("useyozo");
		}
		if(openConverForClient && !_clientAddress.isEmpty()&&useyozo==1)
		{ //开启单独服务转换
			openyozo = true;
		}
		RecordSet.executeQuery("select ifVersion from workflow_base where id="+workflowid);
		if(RecordSet.next()){
			ifVersion = Util.null2String(RecordSet.getString("ifVersion"));
			if(StringUtil.isNull(ifVersion)){
                ifVersion = "0";
            }
		}
		//正文转PDF相关内容
		boolean showSignatureAPI;
		boolean isSavePDF;
		boolean isSaveDecryptPDF;
		List attachmentList;
		int operationtype;
		Map  texttoPDFMap=TexttoPDFManager.getTexttoPDFMap(requestid, workflowid, nodeid,docid);
		showSignatureAPI="1".equals((String)texttoPDFMap.get("showSignatureAPI"))?true:false;
		isSavePDF="1".equals((String)texttoPDFMap.get("isSavePDF"))?true:false;
		isSaveDecryptPDF="1".equals((String)texttoPDFMap.get("isSaveDecryptPDF"))?true:false;
		attachmentList=(List)texttoPDFMap.get("attachmentList");
		operationtype=Util.getIntValue((String)texttoPDFMap.get("operationtype"),0);
		/* 子目录信息 START */
		//子目录信息
		RecordSet.executeProc("Doc_SecCategory_SelectByID",seccategory+"");
		RecordSet.next();
		String categoryname=Util.toScreenToEdit(RecordSet.getString("categoryname"),languageId);
		String subcategoryid=Util.null2String(""+RecordSet.getString("subcategoryid"));
		//String docmouldid=Util.null2String(""+RecordSet.getString("docmouldid"));
		String publishable=Util.null2String(""+RecordSet.getString("publishable"));
		String replyable=Util.null2String(""+RecordSet.getString("replyable"));
		String shareable=Util.null2String(""+RecordSet.getString("shareable"));
		String cusertype=Util.null2String(""+RecordSet.getString("cusertype"));
		cusertype = cusertype.trim();
		String cuserseclevel=Util.null2String(""+RecordSet.getString("cuserseclevel"));
		if(cuserseclevel.equals("255")) cuserseclevel="0";
		String cdepartmentid1=Util.null2String(""+RecordSet.getString("cdepartmentid1"));
		String cdepseclevel1=Util.null2String(""+RecordSet.getString("cdepseclevel1"));
		if(cdepseclevel1.equals("255")) cdepseclevel1="0";
		String cdepartmentid2=Util.null2String(""+RecordSet.getString("cdepartmentid2"));
		String cdepseclevel2=Util.null2String(""+RecordSet.getString("cdepseclevel2"));
		if(cdepseclevel2.equals("255")) cdepseclevel2="0";
		String croleid1=Util.null2String(""+RecordSet.getString("croleid1"));
		String crolelevel1=Util.null2String(""+RecordSet.getString("crolelevel1"));
		String croleid2=Util.null2String(""+RecordSet.getString("croleid2"));
		String crolelevel2=Util.null2String(""+RecordSet.getString("crolelevel2"));
		String croleid3=Util.null2String(""+RecordSet.getString("croleid3"));
		String crolelevel3=Util.null2String(""+RecordSet.getString("crolelevel3"));
		String approvewfid=RecordSet.getString("approveworkflowid");
		String needapprovecheck="";
		if(approvewfid.equals(""))  approvewfid="0";
		if(approvewfid.equals("0"))
			needapprovecheck="0";
		else
			needapprovecheck="1";

		String readoptercanprint = Util.null2String(""+RecordSet.getString("readoptercanprint"));

		/*现在把附件的添加从由文档管理员确定改成了由用户自定义的方式.*/
		// String hasaccessory =Util.toScreen(RecordSet.getString("hasaccessory"),languageId);
		// int accessorynum = Util.getIntValue(RecordSet.getString("accessorynum"),languageId);
		String hasasset=Util.toScreen(RecordSet.getString("hasasset"),languageId);
		String assetlabel=Util.toScreen(RecordSet.getString("assetlabel"),languageId);
		String hasitems =Util.toScreen(RecordSet.getString("hasitems"),languageId);
		String itemlabel =Util.toScreenToEdit(RecordSet.getString("itemlabel"),languageId);
		String hashrmres =Util.toScreen(RecordSet.getString("hashrmres"),languageId);
		String hrmreslabel =Util.toScreenToEdit(RecordSet.getString("hrmreslabel"),languageId);
		String hascrm =Util.toScreen(RecordSet.getString("hascrm"),languageId);
		String crmlabel =Util.toScreenToEdit(RecordSet.getString("crmlabel"),languageId);
		String hasproject =Util.toScreen(RecordSet.getString("hasproject"),languageId);
		String projectlabel =Util.toScreenToEdit(RecordSet.getString("projectlabel"),languageId);
		String hasfinance =Util.toScreen(RecordSet.getString("hasfinance"),languageId);
		String financelabel =Util.toScreenToEdit(RecordSet.getString("financelabel"),languageId);
		String approvercanedit=Util.toScreen(RecordSet.getString("approvercanedit"),languageId);

		int maxOfficeDocFileSize = Util.getIntValue(RecordSet.getString("maxOfficeDocFileSize"),8);

		boolean isEditionOpen = SecCategoryComInfo.isEditionOpen(seccategory);

		//打印申请
		boolean canPrintApply=false;
		String isagentOfprintApply="0";
		String isPrintControl=Util.null2String(RecordSet.getString("isPrintControl"));

		/* 子目录信息 END */
		/* 判断打印控制 START*/
		boolean canDoPrintByApply=false;
		boolean canDoPrintByDocDetail=false;
		boolean hasPrintNode=false;
		boolean isPrintNode=false;
		if(isPrintControl.equals("1")){
			//判断是否已经有申请成功的打印份数
			StringBuffer canDoPrintByApplySb=new StringBuffer();
			canDoPrintByApplySb.append(" select 1   ")
					.append(" from workflow_requestbase a,Bill_DocPrintApply b ")
					.append(" where a.requestId=b.requestid ")
					.append("   and a.currentNodeType='3' ")
					.append("   and b.resourceId=").append(user.getUID())
					.append("   and b.relatedDocId=").append(docid)
					.append("   and printNum>hasPrintNum ")
			;
			RecordSet.executeSql(canDoPrintByApplySb.toString());
			if(RecordSet.next()){
				canDoPrintByApply=true;
				canPrint = true;
			}
			//判断是否有默认的打印份数
			RecordSet.executeSql("select 1 from DocDetail where id="+docid+" and canPrintedNum>hasPrintedNum");
			if(RecordSet.next()){
				canDoPrintByDocDetail=true;
			}
			if(canDoPrintByApply || canDoPrintByDocDetail){
				canPrint = true;
			}else{
				canPrint = false;
			}
		}
		String nodeName = "";
		RecordSet.executeSql("select nodeName from workflow_nodebase where id="+nodeid);
		if(RecordSet.next()) {
			nodeName = Util.null2String(RecordSet.getString("nodeName"));
		}

		/* 判断打印控制 END*/
		RecordSet.writeLog("--------index_odoc.jsp-----docid"+docid);
		//新建正文
		if(docid <= 0){
	%>

	<jsp:include page="/odoc/odoc/index_odocaddNew.jsp">

		<jsp:param name="docid" value="<%=docid%>" />
		<jsp:param name="isIWebOffice2006" value="<%=(isIWebOffice2006?1:0)%>" />
		<jsp:param name="requestid" value="<%=requestid%>" />
		<jsp:param name="requestname" value="<%=requestname%>" />
		<jsp:param name="docstatus" value="<%=docstatus%>" />
		<jsp:param name="languageId" value="<%=languageId%>" />
		<jsp:param name="isEdit" value="<%=isEdit%>" />
		<jsp:param name="editMouldId" value="<%=editMouldId%>" />
		<jsp:param name="workflowid" value="<%=workflowid%>" />
		<jsp:param name="requestid" value="<%=requestid%>" />
		<jsp:param name="secid" value="<%=secid%>" />
		<jsp:param name="requestname" value="<%=requestname%>" />
		<jsp:param name="secFieldid" value="<%=secFieldid%>" />
		<jsp:param name="editMouldFieldid" value="<%=editMouldFieldid%>" />
		<jsp:param name="isSelectField" value="<%=isSelectField%>" />
		<jsp:param name="isNewObject" value="<%=isNewObject%>" />
		<jsp:param name="webwps" value="<%=webwps%>" />
		<jsp:param name="webwps4Windows" value="<%=webwps4Windows%>" />
		<jsp:param name="yozoOffice" value="<%=yozoOffice%>" />
		<jsp:param name="office2015" value="<%=office2015%>" />
		<jsp:param name="maxOfficeDocFileSize" value="<%=maxOfficeDocFileSize%>" />

	</jsp:include>

	<%
	}else if(isEdit){%>

	<jsp:include page="/odoc/odoc/index_odoceditNew.jsp">
		<jsp:param name="docid" value="<%=docid%>" />
		<jsp:param name="isIWebOffice2006" value="<%=(isIWebOffice2006?1:0)%>" />
		<jsp:param name="requestid" value="<%=requestid%>" />
		<jsp:param name="docstatus" value="<%=docstatus%>" />
		<jsp:param name="languageId" value="<%=languageId%>" />
		<jsp:param name="isEdit" value="<%=isEdit%>" />
		<jsp:param name="editMouldId" value="<%=editMouldId%>" />
		<jsp:param name="oldEditMouldId" value="<%=oldEditMouldId%>" />
		<jsp:param name="editSelectValue" value="<%=editSelectValue%>" />
		<jsp:param name="workflowid" value="<%=workflowid%>" />
		<jsp:param name="requestid" value="<%=requestid%>" />
		<jsp:param name="secid" value="<%=secid%>" />
		<jsp:param name="imagefileId" value="<%=imagefileId%>" />
		<jsp:param name="requestname" value="<%=requestname%>" />
		<jsp:param name="secFieldid" value="<%=secFieldid%>" />
		<jsp:param name="showMouldFieldid" value="<%=showMouldFieldid%>" />
		<jsp:param name="isShowSelectField" value="<%=isShowSelectField%>" />
		<jsp:param name="editMouldFieldid" value="<%=editMouldFieldid%>" />
		<jsp:param name="isSelectField" value="<%=isSelectField%>" />
		<jsp:param name="canPrint" value="<%=canPrint%>" />
		<jsp:param name="canDownload" value="<%=canDownload%>" />
		<jsp:param name="isNewObject" value="<%=isNewObject%>" />
		<jsp:param name="showMouldId" value="<%=showMouldId%>" />
		<jsp:param name="webwps" value="<%=webwps%>" />
		<jsp:param name="webwps4Windows" value="<%=webwps4Windows%>" />
		<jsp:param name="yozoOffice" value="<%=yozoOffice%>" />
		<jsp:param name="office2015" value="<%=office2015%>" />
		<jsp:param name="usePreview" value="<%=usePreview%>" />
		<jsp:param name="isUse0309" value="<%=isUse0309%>" />
		<jsp:param name="maxOfficeDocFileSize" value="<%=maxOfficeDocFileSize%>" />

	</jsp:include>
	<%
	}else{%>
	<jsp:include page="/odoc/odoc/index_odocdspNew.jsp">
		<jsp:param name="docid" value="<%=docid%>" />
		<jsp:param name="isIWebOffice2006" value="<%=(isIWebOffice2006?1:0)%>" />
		<jsp:param name="requestid" value="<%=requestid%>" />
		<jsp:param name="docstatus" value="<%=docstatus%>" />
		<jsp:param name="languageId" value="<%=languageId%>" />
		<jsp:param name="isEdit" value="<%=isEdit%>" />
		<jsp:param name="editMouldId" value="<%=editMouldId%>" />
		<jsp:param name="workflowid" value="<%=workflowid%>" />
		<jsp:param name="requestid" value="<%=requestid%>" />
		<jsp:param name="secid" value="<%=secid%>" />
		<jsp:param name="requestname" value="<%=requestname%>" />
		<jsp:param name="canPrint" value="<%=canPrint%>" />
		<jsp:param name="canDownload" value="<%=canDownload%>" />
		<jsp:param name="isNewObject" value="<%=isNewObject%>" />
		<jsp:param name="showMouldId" value="<%=showMouldId%>" />
		<jsp:param name="webwps" value="<%=webwps%>" />
		<jsp:param name="secFieldid" value="<%=secFieldid%>" />
		<jsp:param name="webwps4Windows" value="<%=webwps4Windows%>" />
		<jsp:param name="yozoOffice" value="<%=yozoOffice%>" />
		<jsp:param name="office2015" value="<%=office2015%>" />

	</jsp:include>
	<% }%>


	<script>
		//获取二级路径
		function getSecondPath(url){
			var ecologyContentPath = "<%=CONTEXTPATH%>";//二级路径
			if(ecologyContentPath!="" && url && url!=""){
			if(url.indexOf("/")==0 && url.indexOf(ecologyContentPath)!=0){
				url = ecologyContentPath+url;
			}else if(url.indexOf("http")==0){
				var origin = window.location.origin;
				if(!origin){
				origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
				}
				if(url.indexOf(origin)==0 && url.indexOf(origin+ecologyContentPath)!=0){
				url = url.replace(origin,origin+ecologyContentPath);
				}
			}
			}
			return url;
		}

        /**分享*/
        function onDocShare(){
            var message='[{"shareid":<%=docid%>,"sharetitle":"<%= docsubject %>","sharetype":"doc","objectname":"FW:CustomShareMsg"}]';
            socialshareToEmessage(message);
        }
        /**调用子层office保存*/
        function saveDocumentForFlowdoc(){
            try{
                var obj = document.getElementById("webOffice").contentWindow.SaveDocument();
                if(obj.off_status == 0){
                    obj.msg = "保存失败!";
                }
                return obj;
            }catch(e){
                return {off_status : 1};
            }
        }
        /**子层office调用保存事件是，保存office*/
        function __saveDocument(){
            var obj = document.getElementById("webOffice").contentWindow.SaveDocument();
        }
        /**调用子层office保存草稿*/
        function saveDocumentAsDraft(){
            return saveDocument();
        }
        /**子层office调用保存草稿事件是，保存office*/
        function __saveDocumentAsDraft(){

        }

        function changeOfficeMould(mouldid){
            //mTemplate

            try{
                var _src = WebOfficeObj.src;
                _src = _src.replace(/&mTemplate=-?\d*/g,"&mTemplate=" + mouldid);
                WebOfficeObj.src = _src;
            }catch(e){}

        }


        //异步加载完附件之后
        function afterConvertFile (data){
            if(!data)
                return;
            var _src = "";
            if(data.convert == "client"){
                if(data.result == "0"){
                    if(data.data[0].path){
                        _src = data.data[0].path;
                    }else{
                        if("<%=convertFile%>" == "pdf"){
                            _src ="<%=CONTEXTPATH%>/docs/pdfview/web/pdfViewer.jsp?canPrint=<%=canPrint%>&canDownload=<%=canDownload%><%=moudleParams%>&pdfimagefileid="+data.data[0].id;
                        }else if("<%=convertFile%>" == "html"){
                            _src ="<%=CONTEXTPATH%>/weaver/weaver.file.FileDownload?fileid="+data.data[0].id+ "<%=moudleParams%>&nolog=1" ;
                        }
                    }

                }else{
                    _src ="<%=CONTEXTPATH%>/wui/common/page/sysRemindDocpreview.jsp?labelid=" + data.message;
                }
            }else{
                if(data.status == "1"){
                    if("<%=convertFile%>" == "pdf"){
                        _src ="<%=CONTEXTPATH%>/docs/pdfview/web/pdfViewer.jsp?canPrint=<%=canPrint%>&canDownload=<%=canDownload%><%=moudleParams%>&pdfimagefileid="+data.convertId;
                    }else if("<%=convertFile%>" == "html"){
                        _src ="<%=CONTEXTPATH%>/weaver/weaver.file.FileDownload?fileid="+data.convertId+ "<%=moudleParams%>&nolog=1" ;
                    }
                } if(data.status == "-1"){
                    _src ="<%=CONTEXTPATH%>/docs/pdfview/web/sysRemind.jsp?labelid=998";
                }else if(data.status == "-2"){
                    _src ="<%=CONTEXTPATH%>/wui/common/page/sysRemindDocpreview.jsp?labelid=" + data.msg;
                }
            }
            if(_src != ""){
				setTimeout(function(){
					document.getElementById("webOffice").src = _src;	
				},500)                
            }
        }

        function initFlashVideo(){}

        //打开应用连接
        function openAppLink(obj,linkid){
            var linkType=jQuery(obj).attr("linkType");
            if(linkType=="doc")
                window.open("<%=CONTEXTPATH%>/docs/docs/DocDsp.jsp?id="+linkid);
            else if(linkType=="task")
                window.open("<%=CONTEXTPATH%>/proj/process/ViewTask.jsp?taskrecordid="+linkid);
            else if(linkType=="crm")
                window.open("<%=CONTEXTPATH%>/CRM/data/ViewCustomer.jsp?CustomerID="+linkid);
            else if(linkType=="workflow")
                window.open("<%=CONTEXTPATH%>/workflow/request/ViewRequest.jsp?requestid="+linkid);
            else if(linkType=="project")
                window.open("<%=CONTEXTPATH%>/proj/data/ViewProject.jsp?ProjID="+linkid);
            else if(linkType=="workplan")
                window.open("<%=CONTEXTPATH%>/workplan/data/WorkPlanDetail.jsp?workid="+linkid);
            return false;
        }
        //打开留言附件(回复)
        function openReplyAcc(url,fileid){
            window.open(url);
        }
        //下载附件(回复)
        function downloadFile(url,fileid){
            window.open(url);
        }
	</script>
	<style type="text/css">
		body{
			margin: 0;
			overflow: hidden;
		}
		.chatImgPagWrap  .carousel-fullpane .control-pane .ctrlbtn {
			margin: 0
		}
		.wea-doc-detail-content-main img{
			max-width:1024px;
		}
		.wea-new-top-req-main{
			height:38px !important;
		}
		.wea-new-top-req-main .ant-tabs-tab{
			height:36px !important;
		}
		.ant-tabs-bar{
			height:38px;
		}
		.ant-table-content table{
			font-size: inherit;
		}
		.wea-new-top-req-main:after{
			content: "";
			display: none;
		}
	</style>
</head>
<body id="mybody" onunload="checkInDoc();">

<input type="hidden" id="isOffice" value="<%=isEditOffice ? 1 : 0%>"/>
<input type="hidden" id="docid"  name="docid" value="<%=docid %>"/>
<input type="hidden" id="extendName" name="extendName" value="<%=extendName%>"/>
<input type="hidden" id="imagefileId" name="imagefileId" value="<%=imagefileId%>"/>
<input type="hidden" id="smartedImagefileId" name="smartedImagefileId" value="<%=imagefileId%>"/>
<input type="hidden" id="smartedExtendName" name="smartedExtendName" value="<%=extendName%>"/>

<div id="containerButton" style="height:<%if(requestdocInForm.equals("1")){%>0px<%}else{%>0px<%}%>;">
</div>
<div id="container" style="height:100%;">
</div>
<div id="docContentPre" style="display: none;" >
	<%= docContent %>
</div>
<div><input type="hidden" id="docxContent" name="docxContent" value="<%=templatedDocpath%>"/></div>
<script type="text/javascript">
	<%
	if(docid <= 0){
        paramMap.put(com.api.doc.detail.util.DocParamItem.SEC_CATEGORY.getName(),secid);
        paramMap.put(com.api.doc.detail.service.DocSaveService.DOC_CONTENT,"");
        BaseBean.writeLog("paramMap==================" + paramMap);
    }

    RecordSet rs = new RecordSet();
	String signatureType = Util.null2String(Prop.getPropValue("wps_office_signature","wps_office_signature"));
	rs.executeQuery("select officesigntype from odoc_signsetting");
	if(rs.next()){
	    String tmpSignatureType = Util.null2String(rs.getString("officesigntype"));
	    if("-1".equals(tmpSignatureType)||"3".equals(tmpSignatureType)){
	        signatureType = Util.null2String(Prop.getPropValue("wps_office_signature","wps_office_signature"));
	    }else{
	        signatureType = tmpSignatureType;
	    }
	}
    String wps_version=Util.null2String(Prop.getPropValue("wps_office_signature","wps_version"));

    String maxImageFieldId = "";
    rs.executeQuery("select versionid from docimagefile where docid=? and (isextfile is null or isextfile != '1')  and hasusedtemplet='0' order by versionid desc",docid);
	if(rs.next()){
		maxImageFieldId = rs.getString(1);
	}

    int useTempCancelVersionId = 0;
    rs.executeSql("select versionid from DocImageFile where docid="+docid+" and (isextfile is null or isextfile='0') and hasusedtemplet='0' order by versionId desc");
    if(rs.next()){
        useTempCancelVersionId = Util.getIntValue(rs.getString("versionid"),-1);
    }


    %>
	var js_docFieldId = '<%=docFieldId%>';
	var js_docNameField = '<%=docNameField%>';
	var js_documentTitleField = '<%=documentTitleField%>';

	//环境以及使用的控件参数类型
	var isChrome = <%=isChrome%>;  //是否谷歌浏览器
	var isIE = <%=isIE%>; //是否ie浏览器
	var isWindows = <%=isWindows%>; //是否window环境
	var yozoOffice = <%=yozoOffice%>; //是否永中环境
	var webwps = <%=webwps%>; //国产环境webwps软件是否启用
	var webwps4Windows = <%=webwps4Windows%>; //window环境webwps软件是否启用
	var isIWebOffice2003 = <%=isIWebOffice2003%>; //是否iweboffice2003
	var isUse0309 = <%=isUse0309%>; //是否开启0309
	var openyozo = <%=openyozo%>; //转PDF永中服务是否开启
	var isNewObject = <%=isNewObject%>;	//是否使用iWebOfficeChina
	var noplugninText = <%=noplugninText%>; //自研无插件
	var yozoView = "<%=yozoView%>"; //永中预览
	var wpsView = "<%=wpsView%>"; //金山预览
	var wpsEditor = <%=wpsEditor%>; //金山可编辑
	var canEditForWpsDocCenter = <%=canEditForWpsDocCenter%>;
	var office2015 = <%=office2015%>; //启用office2015插件

	//契约锁相关参数
	var isOpenQysSign = <%=isOpenQysSign%>; //是否打开契约锁签章
	var isQysSignNode = <%=isQysSignNode%>; //是否契约锁签章的节点
	var QysOfficeSignature = null;
	var webSocketUrl = '<%=QysOfficeSignSettingBiz.getWebSocketUrl()%>'; //契约锁签章url
	var webSocketUrl2 = '<%=QysOfficeSignSettingBiz.getWebSocketUrl2()%>'; //契约锁签章url2
	var isQysPdfSingleSignSize = <%=isQysPdfSingleSignSize%>; //是否契约锁pdf单体签章大小
	var isQysPdfSingleSinBrowser = <%=isQysPdfSingleSinBrowser%>; //是否启用契约锁单体签章阅读器
	var isIWebOffice2006 = <%=isIWebOffice2006%>; //是否iweboffice2006?2009
	var isQysOfficeSign = <%=isQysOfficeSign%>; //是否契约锁office签章
	var isQysPdfSingleSign = <%=isQysPdfSingleSign%>; //是否契约锁pdf单体签章
	var isremark = '<%=isremark%>';

	//公文流程设置参数
	var isEdit = "<%=isEdit%>"; //是否可编辑
	var autoCleanCopy = <%=autoCleanCopy%>; //自动清稿
	var isCleanCopyNodes = <%=isCleanCopyNodes%>; //是否清稿节点
	var useYozoOrWPS = <%=useYozoOrWPS%>; //开启默认使用永中或者金山服务进行预览
	var isTextInForm = <%=isTextInForm.equals("true")%>; //开启正文显示在表单
	var isColumnShow = <%=isColumnShow.equals("1")%>; //开启一文双屏
	var saveBackForm = <%=saveBackForm%>; //保存时返回流程表单
	var needSaveVersion = <%=needSaveVersion%>; //是否保存清稿前痕迹版本
	var isSignatureNodes = <%=isSignatureNodes%>; //是否签章节点
	var secFieldid = '<%=secFieldid%>'; //选择框目录对应的字段id
	var isSavePDF = <%=isSavePDF%>; //是否转PDF
	var isSaveDecryptPDF = <%=isSaveDecryptPDF%>; //是否存为脱密PDF
	var operationtype = <%=operationtype%>; //转PDF操作类型
	var openWpsToPdf = <%=openWpsToPdf%>; //开启金山服务转PDF
	var isPermissionTemp = "<%=isPermissionTemp%>";//是否开启按权限选择套红模板
	var saveRevision = '<%=saveRevision%>';	//选择编辑模板后是否保留痕迹
	var signatureType = "<%=signatureType%>"; //金格电子签章类型
	var documentTitleField = '<%=(documentTitleField == -3||documentTitleField == 0)?-1:documentTitleField%>'; //文档标题字段id
	var isShowSelectField = '<%=isShowSelectField%>'; //取编辑、套红模板区分字段是否为选择框
	var showMouldFieldid = '<%=showMouldFieldid%>'; //套红模板对应字段
	var isUseTempletNode = '<%=isUseTempletNode%>'; //是否套红节点
	var hasUsedTemplet = '<%=hasUsedTemplet%>'; //是否已经套红确认
	var isCompellentMark = '<%=isCompellentMark%>'; //是否必须留痕
	var isCancelCheck = '<%=isCancelCheck%>'; //是否取消审阅
	var isHideTheTraces = '<%=isHideTheTraces%>';
	var ifVersion = <%=ifVersion%>; //保存正文时保留正文版本
	var allowDisplayTab = <%=allowDisplayTab%>; //默认隐藏文档属性，附件等页签
	var hideDownload = <%=hideDownload%>;

	//文档自身属性参数
	var officeType = "<%=officeType%>"; //文档类型
	var docFileName = "<%=docFileName%>"; //文档名称
	var isCheckedOut = <%=isCheckedOut%>; //正文是否签出
	var maxOfficeDocFileSize = <%=maxOfficeDocFileSize%>; //正文保存的最大值
	var suffix = "<%=suffix%>"; //文档后缀名
	var extendName = '<%=extendName%>'; //后缀名
	var doccreaterid = '<%=doccreaterid%>'; //文档创建人id
	var docsubject = '<%=Util.stringReplace4DocDspExt(docsubject)%>'; //文档标题
	var odocfileType = '<%=odocfileType%>'; //文档类型

	//其他
	var workflowid = '<%=workflowid%>'; //工作流id
	var requestid = '<%=requestid%>'; //当前请求id
	var nodeid = '<%=nodeid%>'; //节点id
	var formId = '<%=formId%>'; //表单id
	var userid = '<%=user.getUID()%>'; //用户id
	var loginType = '<%=Util.getIntValue(user.getLogintype(),1)%>'; //登录类型
	var hasCleanTraceSave = false;
	var usePreview = "<%=usePreview%>"; //使用永中、wps预览正文。
	var fromOnEditDoc = <%=fromOnEditDoc%>; //开启了默认预览且点击了编辑按钮后
	var docContentSrc = "<%=docContentSrc%>"; //点击编辑按钮后跳转的页面
	var FLOWDOCADDPARAM = "<%=paramMap.toJSONString().replace("\"","\\\"")%>"; //正文参数
	var clickAcceptRevisions = false;
	var paramMapStr = '<%=JsonUtils.map2json(paramMap)%>'; //正文参数
	var canPostil = '<%=canPostil%>'; //iweboffice2006/2009会多添加一个 ,0 在EditType后面
	var bookmarkJson = "<%=bookmarkJson.replace("\"","\\\"")%>"; //书签对应关系json
	var isflowdoc = 1; //是否来自正文
	var languageId = <%=languageId%>; //多语言类型
	var isNoComment = '<%=isNoComment%>'; //iweboffice2006/2009 是否保存复合文档
	var MOULDSELECTVALUE = '<%=Util.getIntValue((String)paramMap.get("field" + paramMap.get("showMouldFieldid")),-101)%>';
	var userName = '<%=user.getUsername()%>'; //用户名
	var nodeName = '<%=nodeName%>'; //节点名
	var addr = '<%=request.getRemoteAddr()%>'; //得客户端的ip地址
	var OfficeIframeHeight; //控件所在iframe高度
	var isStartNode = <%=isStartNode%>;
	var requestdocInForm = '<%=requestdocInForm%>';//是从表单上打开
	var canDownload = <%=canDownload%>;
	//文档相关联的id
	var maxImageFieldId = '<%=maxImageFieldId%>'; //最大的imagefileid
	var wordmouldid = "<%=editMouldId%>"; //编辑模板id
	var secid = <%=secid%>; //目录id
	var oldEditMouldId = '<%=oldEditMouldId%>'; //原来的编辑模板id
	var fileId = "<%=fileId%>"; //文档附件id
	var imagefileid = <%=imagefileId%>; //文档附件id
	var versionId = "<%=(versionId==0?"":versionId+"")%>"; //版本id
	var RecordID = "<%=(versionId==0?"":versionId+"")%>_<%=docid%>"; //传递到后台用于解析的id，版本id+docid
	var editMouldId = "<%=editMouldId%>"; //编辑模板id
	var showMouldId = "<%=showMouldId%>"; //显示模板id
	var mouldparam = <%=paramMap.toJSONString()%>; //模板参数
	var docid = '<%=docid%>'; //文档id
	var printMouldid = "<%=printMouldid%>"; //打印模板id
	var CONTEXTPATH = "<%=CONTEXTPATH%>";  //二级路径地址
	var autoSmartOfficial = "<%=autoSmartOfficial%>";
	var manualSmartOfficial = "<%=manualSmartOfficial%>";
	var yozoEditor = <%=yozoEditor%>;
	var isHideTheTraces = "<%=isHideTheTraces%>";
	var previewAfterSmarted = <%=previewAfterSmarted%>;

	//标签类
	var label533792 = '<%=SystemEnv.getHtmlLabelName(533792,languageId)%>';
	var label18805 = '<%=SystemEnv.getHtmlLabelName(18805,languageId)%>';
	var label15586 = '<%=SystemEnv.getHtmlLabelName(15586,languageId)%>';
	var label21706 = '<%=SystemEnv.getHtmlLabelName(21706,languageId)%>';
	var label518405 = '<%=SystemEnv.getHtmlLabelName(518405,languageId)%>';
	var label83551 = '<%=SystemEnv.getHtmlLabelName(83551,user.getLanguage())%>';
	var label23030 = '<%=SystemEnv.getHtmlLabelName(23030,7)%>';
	var label21667 = '<%=SystemEnv.getHtmlLabelName(21667,user.getLanguage())%>';
	var label518120 = '<%=SystemEnv.getHtmlLabelName(518120,user.getLanguage())%>';
	var label515850 = '<%=SystemEnv.getHtmlLabelName(515850,user.getLanguage())%>';
	var label518124 = '<%=SystemEnv.getHtmlLabelName(518124,user.getLanguage())%>';
	var label516225 = '<%=SystemEnv.getHtmlLabelName(516225,user.getLanguage())%>';
	var label516701 = '<%=SystemEnv.getHtmlLabelName(516701,user.getLanguage())+(user.getLanguage() == 8?" ":"")+qysPdfSingleSignMaxSize+"M"%>';
	var label516226 = '<%=SystemEnv.getHtmlLabelName(516226,user.getLanguage())%>';
	var label516429 = '<%=SystemEnv.getHtmlLabelName(516429,user.getLanguage())%>';
	var __label19716 = "<%=SystemEnv.getHtmlLabelName(19716,languageId)%>";
	var __label19006 = "<%=SystemEnv.getHtmlLabelName(19006,languageId)%>";
	var label21471 = "<%=SystemEnv.getHtmlLabelName(21471,user.getLanguage())%>";
	var label21252 = "<%=SystemEnv.getHtmlLabelName(21252,user.getLanguage())%>";
	var label258 = '<%=SystemEnv.getHtmlLabelName(258,languageId)%>';
	var label513622 = '<%=SystemEnv.getHtmlLabelName(513622,languageId)%>'
	var label19007 = "<%=SystemEnv.getHtmlLabelName(19007,languageId)%>";
	var label19695 = "<%=SystemEnv.getHtmlLabelName(19695,languageId)%>";
	var label19690 = "<%=SystemEnv.getHtmlLabelName(19690,languageId)%>";
	var label507417 = "<%=SystemEnv.getHtmlLabelName(507417,7)%>";
	var label24028 = "<%=SystemEnv.getHtmlLabelName(24028,languageId)%>";
	var label24029 = "<%=SystemEnv.getHtmlLabelName(24029,languageId)%>";
	var label18758 = "<%=SystemEnv.getHtmlLabelName(18758,user.getLanguage())%>";
	var label84544 = "<%=SystemEnv.getHtmlLabelName(84544,user.getLanguage())%>";
	var label28121 = "<%=SystemEnv.getHtmlLabelName(28121,languageId)%>";
	var label24338 = "<%=SystemEnv.getHtmlLabelName(24338,user.getLanguage())%>";
	var label26090 = "<%=SystemEnv.getHtmlLabelName(26090,user.getLanguage())%>";
	var label19718 = "<%=SystemEnv.getHtmlLabelName(19718,user.getLanguage())%>(&S)";
	var label19719 = "<%=SystemEnv.getHtmlLabelName(19719,user.getLanguage())%>";
	var label221 = "<%=SystemEnv.getHtmlLabelName(221,user.getLanguage())%>";
	var label16386 = "<%=SystemEnv.getHtmlLabelName(16386,user.getLanguage())%>";
	var label18893 = "<%=SystemEnv.getHtmlLabelName(18893,user.getLanguage())%>";
	var label1205_26096 = '<%=SystemEnv.getHtmlLabelNames("1205,26096", user.getLanguage())%>';
	var label132199 = '<%=SystemEnv.getHtmlLabelName(132199,user.getLanguage())%>';
	var label132200 = '<%=SystemEnv.getHtmlLabelName(132200,user.getLanguage())%>';
	var label520526 = '<%=SystemEnv.getHtmlLabelName(520526,user.getLanguage())%>';
	var label389418 = '<%=SystemEnv.getHtmlLabelName(389418,user.getLanguage())%>';

	setTimeout(function(){
		mybody.onbeforeunload=null;
	},5000);


	<%
    String editMouldIdAndSecCategoryId = editMouldId + "," + secid;
    %>


	var minWidth ;
	var maxWidth;
	var minHeight;
	var maxHeight;
	var bestWidth;
	var bestHeight;
	var PDF417ManagerCopyRight;
	<%
    //判断是否启用二维条码
    RecordSet.executeSql("select * from Workflow_BarCodeSet where workflowId="+workflowid+" and isUse='1'");
    if(RecordSet.next()){
        String isUseBarCodeThisJsp="1";
        int barCodeSetId=Util.getIntValue(RecordSet.getString("id"),0);
        String measureUnit=Util.null2String(RecordSet.getString("measureUnit"));
        int printRatio = Util.getIntValue(RecordSet.getString("printRatio"),96);
        int minWidth = Util.getIntValue(RecordSet.getString("minWidth"),30);
        int maxWidth = Util.getIntValue(RecordSet.getString("maxWidth"),70);
        int minHeight = Util.getIntValue(RecordSet.getString("minHeight"),10);
        int maxHeight = Util.getIntValue(RecordSet.getString("maxHeight"),25);
        int bestWidth = Util.getIntValue(RecordSet.getString("bestWidth"),50);
        int bestHeight = Util.getIntValue(RecordSet.getString("bestHeight"),20);

        if(measureUnit.equals("1")){
            minWidth=(int)(0.5+minWidth*printRatio/25.4);
            maxWidth=(int)(0.5+maxWidth*printRatio/25.4);
            minHeight=(int)(0.5+minHeight*printRatio/25.4);
            maxHeight=(int)(0.5+maxHeight*printRatio/25.4);
            bestWidth=(int)(0.5+bestWidth*printRatio/25.4);
            bestHeight=(int)(0.5+bestHeight*printRatio/25.4);
        }

        String PDF417TextValue=WorkflowBarCodeSetManager.getPDF417TextValue(requestid,barCodeSetId,user.getLanguage());
    %>
	minWidth = <%=minWidth%>;
	maxWidth = <%=maxWidth%>;
	minHeight = <%=minHeight%>;
	maxHeight = <%=maxHeight%>;
	bestWidth = <%=bestWidth%>;
	bestHeight = <%=bestHeight%>;
	PDF417ManagerCopyRight = "<%=PDF417ManagerCopyRight%>";
	<%
    }
    %>

</script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/odoc/odoc/js/index_odocCommon.js?v=<%=cversion%>"></script>
<script type="text/javascript">
    window.__time0 = "判断权限耗时：<%=time0%>";
    window.__time1 = "获取属性耗时：<%=time1%>";
    window.__time2 = "获取正文耗时：<%=time2%>";
    window.__time3 = "业务逻辑总耗时：<%=time3%>";

    window.__imagefilename = "<%=(imagefilename.replaceAll("\"","＂"))%>";
    window.__showByHtml = "<%=showByHtml ? 1 : 0%>";	//文档类型是html
    window.__moudleParams = "<%=moudleParams %>";		//其他模块参数集
    window.__hasNewestWarm = "<%=ishistory == 1 ? 1 : 0%>";	//是否给出有最新文档的提示
    window.__linkNewestDoc = "<%=linkNewestDoc%>";		//跳转到最新版本文档的链接
	window.__isAutoExtendInfo = "<%=isAutoExtendInfo ? 1 : 0%>";//有附件时展开文档附件属性
	function convertFile(){
       jQuery.ajax({
            type: "GET",
            url: "<%=CONTEXTPATH%>/api/doc/acc/convertFile",
            data: {
                convertFile:'<%=convertFile%>',
                converSelf:'<%=convertFile%>',
                fileid:'<%=imagefileId%>',
                fromFlowDoc:'1',
                docid:'<%=docid%>',
                versionId:'<%=versionId%>',
				requestid:'<%=requestid%>',
				authStr:'<%=authStr%>',
				authSignatureStr:'<%=authSignatureStr%>'
            },
            cache: false,
            dataType: 'json',
            beforeSend: function(){
                jQuery("#messageArea").show();
            },
            success: function(data){  
					afterConvertFile(data);				
            },
        });		
	}	
    //html走该预览
    if("<%=convertFile%>" != "" && "<%=isEdit%>" == "false"){
		setTimeout(function(){convertFile();},1500)
        //window.__convertFile = "convertFile=<%=convertFile%>&fileid=<%=imagefileId%>&docid=<%=docid%>&versionId=<%=versionId%>";		//是否需要异步转换文档
    }

    if(jQuery("#frame1").length > 0){

    }else{
        jQuery("table[id='content']").show();
    }

    jQuery(document).ready(function(){

		var docInfo = parent.WfForm.getOdocConfig();
        var buttonList = docInfo.buttons;
        for(var cnt = 0 ; cnt < buttonList.length; cnt++)
		{
		    //调整说明;预览方式不显示清稿、保存等按钮
			if("<%=usePreview%>" == "true")
			{
				if(buttonList[cnt]["key"] == "selectTemplate" || buttonList[cnt]["key"] == "saveDoc" || buttonList[cnt]["key"] == "SaveNewVersion"|| buttonList[cnt]["key"] == "toggleTrace" || (buttonList[cnt]["key"] == "fullScreen" && "<%=isWindows%>" == "false")|| buttonList[cnt]["key"] == "openLocalFile" || buttonList[cnt]["key"] == "smartOfficial")
				{
					buttonList[cnt]["isshow"] = false;
				}
			}

            //调整说明:非插件方式不显示清稿按钮
			if("<%=canIWebOffice%>" == "false" && "<%=wpsEditor%>" != "true" && "<%=yozoEditor%>" != "true" && buttonList[cnt]["key"] == "clearTrace")
			{
                buttonList[cnt]["isshow"] = false;
			}
			//yjy add 20200530 套红改造后，如果没有模板则隐藏“套红确认”按钮
            if( docInfo.docParam.showmouldCount <= 0 && docInfo.docParam.showMouldId <= 0 && buttonList[cnt]["key"] == "saveTHTemplate")
            {
                buttonList[cnt]["isshow"] = false;
            } else if (docInfo.docParam.showmouldCount > 0 && docInfo.docParam.showMouldId > 0 && buttonList[cnt]["key"] == "saveTHTemplate" && buttonList[cnt]["canshow"] == true && docInfo.docParam.hasUsedTemplet != '1'){
                buttonList[cnt]["isshow"] = true;
			}
            //是否显示保存PDF的按钮
			if("<%=iWebPDFIframe%>" == "true" && "<%=canEditPDF%>" == "true" && "<%=useYozoOrWPS%>" != "true" && buttonList[cnt]["key"] == "saveDoc"){
				buttonList[cnt]["isshow"] = true;
			}
			if("<%=hideSaveButton%>" == "true" && buttonList[cnt]["key"] == "saveDoc"){
				buttonList[cnt]["isshow"] = false;
			}

			//chrome模式下，03插件，隐藏保存、显示隐藏痕迹、打印按钮。
			if("true" == "<%=isWindows%>" && "<%=isIE%>" == "false" && "<%=isUse0309%>" == "true" && ".pdf" != "<%=extendName%>"){
				if(buttonList[cnt]["key"] == "saveDoc" || buttonList[cnt]["key"] == "SaveNewVersion" || buttonList[cnt]["key"] == "clearTrace" || buttonList[cnt]["key"] == "saveTHTemplate" || buttonList[cnt]["key"] == "toggleTrace" 
				 || buttonList[cnt]["key"] == "selectTemplate" || buttonList[cnt]["key"] == "useTempletCancel" || buttonList[cnt]["key"] == "saveTHTemplateNoConfirm"){
					buttonList[cnt]["isshow"] = false;
                    docInfo.docParam.isShowSelectOtherMould = false;
				}
			}
            <%BaseBean.writeLog("=========================isWindows="+isWindows+"isIE="+isIE+"isUse0309="+isUse0309+"officeDocument="+officeDocument);%>
			if("true" == "<%=isWindows%>" && "<%=isIE%>" == "false" && "<%=isUse0309%>" == "true" && "<%=officeDocument%>" == "true"&&"<%=useYozoOrWPS%>" != "true"){
				if(buttonList[cnt]["key"] == "printDoc"){
					buttonList[cnt]["isshow"] = false;
				}
			}
			
			// 国产环境不展示签章、签章确认按钮
			if("<%=isWindows%>" == "false" && (buttonList[cnt]["key"] == "CreateSignature" || buttonList[cnt]["key"] == "saveIsignatureFun")){
				buttonList[cnt]["isshow"] = false;
			}

            //契约锁单体签章&签章节点 chrome显示打印按钮，其他浏览器不显示
			if("<%=isQysPdfSingleSign%>" == "true" && "<%=isQysPdfSingleSinBrowser%>" == "true" && "<%=usePreview%>" != "true" && buttonList[cnt]["key"] == "printDoc"){
				buttonList[cnt]["isshow"] = true;
			}

			//调整说明:非插件方式不显示打印\签章按钮
			if("<%=canIWebOffice%>" == "false"&& !<%=wpsEditor%> && !<%=yozoEditor%> && (buttonList[cnt]["key"] == "CreateSignature_Sign"|| buttonList[cnt]["key"] == "selectTemplate"|| buttonList[cnt]["key"] == "openLocalFile"|| buttonList[cnt]["key"] == "selectTemplate2")){
				buttonList[cnt]["isshow"] = false;
			}
			if("<%=extendName%>"==".html"){
			    buttonList[cnt]["isshow"] = false;
			}
			//自动排版错误提示
			<%if(!"".equals(Util.null2String(errorMsg))){%>
				parent.antd.message.error("<%=errorMsg%>");
			<%}%>

			<%if(previewAfterSmarted && officeDocument && "".equals(qysPdfViewerUrl) && qysDownloadid <= 0){%>
				btnControlAfterSmartOfficial();
				//处理范文tab
				docInfo.docParam.reTextToModel = "0";
				docInfo.docParam.showTextSearch = "0";
				//处理敏感词tab
				docInfo.docParam.textShowSenWords = "0";
				docInfo.docParam.senWordsdoType = "-1";
			<%}%>
            //调整说明:非插件方式不显示范文tab、敏感词tab
            if(("<%=canIWebOffice%>" == "false" && "<%=wpsEditor%>" != "true" && "<%=yozoEditor%>" != "true") || "<%=usePreview%>" == "true") {
                if (buttonList[cnt]["key"] == "reTextToModel" && ".docx" != "<%=extendName%>" && ".doc" != "<%=extendName%>") {
                    buttonList[cnt]["isshow"] = false;
				}
                //处理范文tab
                docInfo.docParam.reTextToModel = "0";
                docInfo.docParam.showTextSearch = "0";
                //处理敏感词tab
                docInfo.docParam.textShowSenWords = "0";
                docInfo.docParam.senWordsdoType = "-1";
            }

			if(window.console)
			{
				console.log("=====key:"+buttonList[cnt]["key"]+"===isshow:"+buttonList[cnt]["isshow"]+"====canshow:"+buttonList[cnt]["canshow"]);
			}
		}
        parent.WfForm.changeOdocConfig(docInfo);
        parent.jQuery(".req-workflow-odoc").height(parent.jQuery(".wf-req-odoc").height());
		<%if(requestdocInForm.equals("1")&&isColumnShow.equals("1")){%> 
			var	showModelAndSensitive = false;
			if(top.WfForm && top.WfForm.getOdocConfig() && top.WfForm.getOdocConfig().docParam.isTextInFormCanEdit){
				var docParam = top.WfForm.getOdocConfig().docParam;
				showModelAndSensitive = docParam.isTextInFormCanEdit ? (docParam.isEdit == '1' && (docParam.reTextToModel == '1' || docParam.textShowSenWords == '1' || docParam.showTextSearch == '1')) ? true : false : false;
			}
			if(showModelAndSensitive){
				parent.jQuery(".wf-req-form-odoc-iframe").width("calc(100% - 51px)");
			}else{
				parent.jQuery(".wf-req-form-odoc-iframe").width("99%");
				parent.jQuery(".wf-req-form-odoc-iframe").css("padding-left","12px");
			}
		<%}%>
        //不修改人力资源链接的target属性
        jQuery("a").each(function(){
            var _this=jQuery(this);
            var href=_this.attr("href");
            if(href){
                href = href.toLowerCase();
                if(-1 == href.indexOf("javascript:openhrm(")){
                    _this.attr("target","_blank");
                }else if(href.indexOf("openfullwindowforxtable(")>0){
                    _this.attr("target","_self");
                }else if(href.indexOf("#")==0){
                    _this.attr("target","_self");
                }
            }
        });
        //隐藏除返回表单、全屏显示、下载的其他按钮 gaohk
        <%if(kgPdfDocId >0 && docid > 0){%>
        setKgButton();
        <%} else{ %>
        //金格签章展示的PDF文档不能展示替换按钮 gaohk
        setReplacePDFShow();//调用控制PDF替换按钮显隐的方法
        <%} %>
		//需隐藏的按钮数组
		var needHideButtons = new Array();
        //隐藏上传本地文件按钮
		<%if((isWindows && webwps4Windows) || (!isWindows && yozoOffice)){%>
        	needHideButtons.push("openLocalFile");
		<%}%>
		//隐藏签章相关按钮(签章节点，且未开启契约锁在线签章时需隐藏;开启在线签章isQysSignNode=true无需此操作)
		<%if(isSignatureNodes && !isQysPdfSingleSign && !isQysSignNode && (!isWindows|| (isWindows && (wpsEditor || yozoEditor || webwps4Windows || (isUse0309 && !"true".equals(isIE)))))){%>
			<%if(!isWindows){%>
                needHideButtons.push("CreateSignature_Sign");
				needHideButtons.push("CreateSignatureSign_SignHandSig");
				needHideButtons.push("CreateSignature_SignPageSeal");
			<%} else {%>
				needHideButtons.push("CreateSignature");
				needHideButtons.push("CreateSignature_Sign");
				needHideButtons.push("CreateSignatureSign_SignHandSig");
				needHideButtons.push("CreateSignature_SignPageSeal");
				needHideButtons.push("saveIsignatureFun");
        	<%}%>

		<%}%>
		//隐藏按钮(执行操作)
        hideButton(needHideButtons);
	    //使用契约锁单体签章的时候按钮的隐藏逻辑
		hideQysSingleSignButton();
		try{
			if(parent.wfform.getGlobalStore().commonParam.requestType == 1){
				<%if(docid > 0){%>
				setSubmitButton();
				 <%}%>
			}
		}catch(e){}
		<%
		if((canEdit && (wpsEditor || yozoEditor || useIwebOfficeEdit || (!isWindows && (isNewObject || yozoOffice || webwps)) || (isWindows && (office2015 || webwps4Windows))))
		|| (isWindows && canEditPDF && iWebPDFIframe))
		{
		    //加载编辑按钮
		%>
			setOnEditButton();
		<%
		}
		%>
        //自定义高度问题。
		var __onresize = parent.window.onresize;
        parent.window.onresize = function()
		{
            var tabKey = parent.window.wfform.getGlobalStore().tabKey;
            if(tabKey ==='odoc')
            {
                parent.jQuery(".wf-req-odoc").height(parent.jQuery(".wea-new-top-req-content").height() - 20).width(parent.jQuery(".wea-new-top-req-content").width()-50 );
                parent.jQuery(".req-workflow-odoc").height(parent.jQuery(".wf-req-odoc").height()).width(parent.jQuery(".iframeDiv").width());
                return;
            }
            __onresize();
		}
        var __detachEvent = parent.window.detachEvent;
        parent.window.detachEvent = function(obj){
            var tabKey = parent.window.wfform.getGlobalStore().tabKey;
            if(obj === 'onresize' && tabKey ==='odoc')
			{
				var wrapperWidth = parent.jQuery(".wea-new-top-req-content").width()-100;
				if(wrapperWidth<1275)
					wrapperWidth = 1275
				parent.jQuery(".wf-req-wrapper").width(wrapperWidth);
				var maxW = parseInt(wrapperWidth);
				if(maxW < parseInt(parent.jQuery(".wea-new-top-req-content").width()-50))
				{
					maxW = parseInt(parent.jQuery(".wea-new-top-req-content").width()-50);
				}
                parent.jQuery(".wf-req-odoc").height(parent.jQuery(".wea-new-top-req-content").height() - 20).width(maxW);
                parent.jQuery(".req-workflow-odoc").height(parent.jQuery(".wf-req-odoc").height()).width(parent.jQuery(".wf-req-odoc").width());
			    return;
            }
            __detachEvent();
		}
		<%if(isQysOfficeSign){%>
		//如果是契约锁Office签章，加载完成以后定时任务链接Office
		setTimeout(function(){
			var idx = 0;
			var isready = false;
			var qysofficesignTnterval = setInterval(function(){
				idx++;
				if (isready == true || idx > 5) {
					clearInterval(qysofficesignTnterval);
				}
				try {
					isready = getQysOfficeSignatureAPI();
				}catch(e){
					clearInterval(qysofficesignTnterval);
				}
			},1000);
		},2000);
		<%}%>

		changeButton();
    });
    /*try{
        customerLoadFunction();
    }catch(e){
        if(window.console)console.log('customerLoadFunction error',e);
    }*/
    window.__docsubject = '<%=docsubject.replace("'","\\'")%>';
    //window.__preTitle = $('.wea-new-top-req-title-text').html();// 应该没用了，就先去掉好了 by caoyun 20180830
    window.__preContent = $('#docContentPre').html();
    $('#docContentPre').remove();
</script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/polyfill/polyfill.min.js"></script>
<!--[if lt IE 10]>
<script type="text/javascript" src="/cloudstore/resource/pc/shim/shim.min.js"></script>
<![endif]-->
<script type="text/javascript">
    jQuery.browser.msie && parseInt(jQuery.browser.version, 10) < 9 && (
        window.location.href = '<%=CONTEXTPATH%>/login/Login.jsp'
    );
    //jQuery.browser.msie && parseInt(jQuery.browser.version, 10) === 10 &&
    //document.write('<div><script src="<%=CONTEXTPATH%>/cloudstore/resource/pc/polyfill/polyfill.min.js"><\/script></div>');
</script>
<!-- 底层库 -->
<!-- <script type="text/javascript" src="/cloudstore/resource/pc/react16/react.development.js"></script>
<script type="text/javascript" src="/cloudstore/resource/pc/react16/react-dom.development.js"></script> -->
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react16/react.production.min.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react16/react-dom.production.min.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react16/prop-types.min.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react16/create-react-class.min.js"></script>
<script>
    //console.log(createReactClass);
    React.PropTypes = PropTypes;
    React.createClass = createReactClass;
</script>
<!-- <script type="text/javascript" src="/cloudstore/resource/pc/react/react-with-addons.min.js"></script>
<script type="text/javascript" src="/cloudstore/resource/pc/react/react-dom.min.js"></script> -->
<!-- 组件库 -->
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/promise/promise.min.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/fetch/fetch.min.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/ckeditor-4.6.2/ckeditor.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/plupload-2.3.1/js/plupload.full.min.js?v=<%=jsVersion%>"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/moduleConfig.js?v=<%=jsVersion%>"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/coms/ssoConfig/config.js?v=<%=jsVersion%>"></script>
<%-- <script type="text/javascript" src="<%=CONTEXTPATH%>/spa/moduleConfig.js?v=1560540617484"></script> --%>
<%-- <script type="text/javascript" src="<%=CONTEXTPATH%>/spa/coms/ssoConfig/config.js?v=<%=jsVersion%>"></script> --%>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/index.min.js?v=<%=jsVersion%>"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/ecCom.min.js?v=<%=jsVersion%>"></script>
<!-- mobx -->
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/mobx-3.1.16/mobx.umd.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/mobx-react-4.2.1/index.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react-router/ReactRouter.min.js"></script>
<%-- <script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/mobx-react-4.2.1/index.js"></script> --%>
<%-- <script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react-router/ReactRouter.min.js"></script> --%>
<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/coms/index.mobx.js?v=<%=jsVersion%>"></script>
<!-- 图片轮播 -->
<script type="text/javascript" src="<%=CONTEXTPATH%>/social/js/drageasy/drageasy.js"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/social/js/bootstrap/js/bootstrap.js?v=<%=jsVersion%>"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/social/js/imcarousel/imcarousel.js"></script>
<!-- spa -->
<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/document/static4Detail/index.js?v=<%=jsVersion%>"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/document/static4Detail/index4single.js?v=<%=jsVersion%>"></script>
<script type="text/javascript" src="<%=CONTEXTPATH%>/odoc/odoc/js/OFFICE.SDK.js"></script>
<div style="display:none;">
	<%
		//if(isSignatureNodes||isSaveDecryptPDF){
		if(isSignatureNodes){
			if("1".equals(signatureType)){%>
	<OBJECT id=SignatureAPI classid="clsid:A0689619-3E99-4012-A83F-E902CF3505BD"  codebase="iSignatureWPS_APIP.ocx#version=<%=wps_version%>"
			width=0 height=0 align=center hspace=0 vspace=0></OBJECT>
	<%}else if("2".equals(signatureType)){%>
	<object id="SignatureAPI" width="0" height="0" classid="clsid:857F9703-BE32-4BD4-92A4-D8079C10BD41"></object>
	<%}else{%>
	<OBJECT id=SignatureAPI classid="clsid:79F9A6F8-7DBE-4098-A040-E6E0C3CF2001"  codebase="iSignatureAPI.ocx#version=5,0,2,0" width=0 height=0 align=center hspace=0 vspace=0></OBJECT>
	<%}}%>
	<iframe id="DocCheckInOutUtilIframe" frameborder=0 scrolling=no src=""  style="display:none"></iframe>
</div>

</body>

</html>
