package com.api.doc.detail.web;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.alibaba.fastjson.JSON;
import com.api.doc.detail.util.DocSensitiveWordsUtil;
import com.api.doc.detail.util.WordProcessUtil;
import com.api.doc.detail.util.ooxml.SearchResult;
import com.api.doc.detail.util.ooxml.commons.CommonUtils;
import com.api.doc.detail.util.ooxml.commons.DocUtils;
import weaver.conn.RecordSet;
import weaver.docs.DocDetailLog;
import weaver.docs.docpreview.DocPreviewHtmlManager;
import weaver.docs.docs.DocComInfo;
import weaver.docs.docs.DocManager;
import weaver.docs.pdf.docpreview.ConvertPDFTools;
import weaver.docs.pdf.docpreview.ConvertPDFUtil;
import weaver.email.service.MailFilePreviewService;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;
import weaver.wps.WebOfficeUtil;
import weaver.wps.down.WPSViewUtils;

import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.util.ConditionType;
import com.api.doc.detail.service.DocAccService;
import com.api.doc.detail.service.DocSaveService;
import com.api.doc.detail.service.DocViewPermission;
import com.api.doc.detail.util.DocDetailUtil;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.api.doc.detail.util.ImageConvertUtil;
import com.api.doc.detail.util.WordToHtmlUtil;
import com.api.doc.yozo.weboffice.util.YozoWebOfficeUtil;
import com.engine.doc.util.DocPlugUtil;
import com.engine.doc.util.IWebOfficeConf;

/**
 * 文档附件 数据获取接口
 * @author wangqs
 * */
@Path("/doc/acc")
public class DocAcction {
	
	public Map<String,String> getRequestMap(HttpServletRequest request){
		
		Map<String,String> dataMap = new HashMap<String,String>();
		
		Enumeration paramNames = request.getParameterNames(); 
		while (paramNames.hasMoreElements()) { 
			String paramName = (String) paramNames.nextElement(); 
			String paramValue = Util.null2String(request.getParameter(paramName));
			dataMap.put(paramName,paramValue);
		}
		return dataMap;
	}

	/**
	 * 获取文档附件列表
	 * @author wangqs
	 * */
	@GET
	@Path("/docAcc")
	@Produces(MediaType.TEXT_PLAIN)
	public String docAcc(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			int docid = Util.getIntValue(request.getParameter("docid"),0);
			if(docid <= 0){
				docid = Util.getIntValue(request.getParameter("id"),0);
			}
			String sql = "select bacthDownload from docseccategory where id = (select seccategory from docdetail where id=?)";
			RecordSet rs = new RecordSet();
			rs.executeQuery(sql,docid);
			String bacthDownload = "";
			while(rs.next()){
				bacthDownload = rs.getString("bacthDownload");
			}
			DocAccService accService = new DocAccService();
			
			Map<String,String> paramMap = getRequestMap(request);
			
			String agent = request.getHeader("user-agent");
			String isIE = "true";
		    if((agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") )|| agent.contains("Edge")){
		    	isIE = "false";
		    }
			paramMap.put("isIE", isIE);
			paramMap.put("agent", agent);
			
			accService.setRequest(request);
			apidatas = accService.getDocAcc(docid,user,paramMap);
			apidatas.put("bacthDownload",bacthDownload);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 获取文档附件列表
	 * @author wangqs
	 * */
	@GET
	@Path("/docAccList")
	@Produces(MediaType.TEXT_PLAIN)
	public String docAccList(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			int docid = Util.getIntValue(request.getParameter("docid"),0);
			if(docid <= 0){
				docid = Util.getIntValue(request.getParameter("id"),0);
			}
			String sql = "select bacthDownload from docseccategory where id = (select seccategory from docdetail where id=?)";
			RecordSet rs = new RecordSet();
			rs.executeQuery(sql,docid);
			String bacthDownload = "";
			while(rs.next()){
				bacthDownload = rs.getString("bacthDownload");
			}
			boolean accListVisible = !"0".equals(rs.getPropValue("docpreview","accListVisible"));
			DocAccService accService = new DocAccService();
			
			Map<String,String> paramMap = getRequestMap(request);
			
			String agent = request.getHeader("user-agent");
			String isIE = "true";
		    if((agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") )|| agent.contains("Edge")){
		    	isIE = "false";
		    }
			paramMap.put("isIE", isIE);
			paramMap.put("agent", agent);
			
			accService.setRequest(request);
			apidatas = accService.getDocAccList(docid,user,paramMap);
			apidatas.put("bacthDownload",bacthDownload);
			apidatas.put("accListVisible",accListVisible);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	
	/**
	 * 文档附件版本
	 * */
	@GET
	@Path("/docAccVersion")
	@Produces(MediaType.TEXT_PLAIN)
	public String docAccVersion(@Context HttpServletRequest request,@Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			int docid = Util.getIntValue(request.getParameter("docid"),0);
			if(docid <= 0){
				docid = Util.getIntValue(request.getParameter("id"),0);
			}
			
			int docImageFileId = Util.getIntValue(request.getParameter("docImageFileId"),0);
			
			int imagefileId = Util.getIntValue(request.getParameter("imagefileid"),0);
			int versionId = Util.getIntValue(request.getParameter("versionid"));
			Map<String,String> paramMap = getRequestMap(request);
			
			DocAccService accService = new DocAccService();
			accService.setRequest(request);
			apidatas = accService.getAccVersion(docid,imagefileId,versionId,docImageFileId,user,paramMap);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);	
	}
	
	/**
	 * 批量下载文件列表
	 * */
	@GET
	@Path("/downLoadBatch")
	@Produces(MediaType.TEXT_PLAIN)
	public String downLoadBatch(@Context HttpServletRequest request,@Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			String docids = Util.null2String(request.getParameter("docids"));
			String fileids = Util.null2String(request.getParameter("fileids"));
			
			
			
			DocAccService accService = new DocAccService();
			apidatas = accService.downLoadBatch(docids,fileids,user);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);	
	}
	
	/**
	 * 文档附件搜索
	 * @author wangqs 
	 * */
	@GET
	@Path("/docAccSearch")
	@Produces(MediaType.TEXT_PLAIN)
	public String docAccSearch(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			SearchConditionItem	searchConditionItem = new SearchConditionItem(ConditionType.INPUT, SystemEnv.getHtmlLabelNames("23752", 
					user.getLanguage()),new String[]{"imagefilename"}); 
			searchConditionItem.setIsQuickSearch(true);
			
			apidatas.put("condition",searchConditionItem);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 文档附件替换
	 * @author wangqs 
	 * */
	@GET
	@Path("/docAccReplace")
	@Produces(MediaType.TEXT_PLAIN)
	public String docAccReplace(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			int id = Util.getIntValue(request.getParameter("id"),0);
			if(id <= 0){
				id = Util.getIntValue(request.getParameter("docid"),0);
			}
			int newfileid = Util.getIntValue(request.getParameter("newfileid"),0);
			int oldfileid = Util.getIntValue(request.getParameter("oldfileid"),0);
			
			DocAccService accService = new DocAccService();
			Map<String,String> checkMap = accService.isCheckOut(id,oldfileid,user);
			if("1".equals(checkMap.get("checkOutStatus"))){
				
				accService.deleteAcc(newfileid,true);
				
				apidatas.put("status","0");
				apidatas.put("msg",checkMap.get("checkMsg") == null || checkMap.get("checkMsg").isEmpty() ? "文档已被签出!" : checkMap.get("checkMsg"));
				return JSONObject.toJSONString(apidatas); 
			}
			DocViewPermission dvp = new DocViewPermission();
			String moduleParams = dvp.getMoudleParams(request);
			String clientip = request.getRemoteAddr();

			try {
				DocDetailLog log = new DocDetailLog();
				DocManager dm = new DocManager();
				dm.setId(id);
				dm.getDocInfoById();
				log.resetParameter();
				log.setDocId(id);
				log.setDocSubject(dm.getDocsubject());
				log.setOperateType("2");
				log.setOperateUserid(user.getUID());
				log.setUsertype(user.getLogintype());
				log.setClientAddress(clientip);
				log.setDocCreater(user.getUID());
				log.setCreatertype(user.getLogintype());
				log.setDocLogInfo();
				DocComInfo dci = new DocComInfo();
				dci.updateDocInfoCache("" + id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			apidatas = accService.replaceAcc(newfileid,oldfileid,id,moduleParams,user);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}

	@GET
	@Path("/fileCheckIn")
	@Produces(MediaType.TEXT_PLAIN)
	public String fileCheckIn(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			int fileid = Util.getIntValue(request.getParameter("fileid"),0);
			int docid = Util.getIntValue(request.getParameter("docid"),0);
			DocAccService accService = new DocAccService();
			String ipAddress = request.getRemoteAddr();
			accService.checkIn(ipAddress,fileid,docid,user);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}	
	
	/**
	 * 文档附件编辑
	 * @author wangqs 
	 * */
	@GET
	@Path("/docAccSave")
	@Produces(MediaType.TEXT_PLAIN)
	public String docAccSave(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			int id = Util.getIntValue(request.getParameter("id"),0);
			if(id <= 0){
				id = Util.getIntValue(request.getParameter("docid"),0);
			}
			int newfileid = Util.getIntValue(request.getParameter("newfileid"),0);
			int oldfileid = Util.getIntValue(request.getParameter("oldfileid"),0);
			
			if(newfileid <= 0){  //wps,iwebchina等插件编辑附件
				String off_name = Util.null2String(request.getParameter("off_name"));
				if(!off_name.isEmpty()){
					ImageFileManager ifm = new ImageFileManager();
					ifm.resetParameter();
					ifm.getImageFileInfoById(oldfileid);
					DocSaveService dss = new DocSaveService();
					String filename = ifm.getImageFileName();
					
					filename = filename.contains(".") ? filename.substring(0,filename.lastIndexOf(".")) : filename;
					
					newfileid = dss.getFileIdByName(off_name,filename);
				}
			}
			
			DocAccService accService = new DocAccService();
			
			DocViewPermission dvp = new DocViewPermission();
			String moduleParams = dvp.getMoudleParams(request);
			try {
				DocDetailLog log = new DocDetailLog();
				DocManager dm = new DocManager();
				dm.setId(id);
				dm.getDocInfoById();
				log.resetParameter();
				log.setDocId(id);
				log.setDocSubject(dm.getDocsubject());
				log.setOperateType("2");
				log.setOperateUserid(user.getUID());
				log.setUsertype(user.getLogintype());
				log.setClientAddress(request.getRemoteAddr());
				log.setDocCreater(user.getUID());
				log.setCreatertype(user.getLogintype());
				log.setDocLogInfo();
				DocComInfo dci = new DocComInfo();
				dci.updateDocInfoCache("" + id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			DocSensitiveWordsUtil docSensitiveWordsUtil = new DocSensitiveWordsUtil();
			boolean enableOfficeSensitiveWordValidate =docSensitiveWordsUtil.enableOfficeSensitiveWordValidate();
			boolean enableAutoRemoveHighlightAfterValidateSensitiveWord = docSensitiveWordsUtil.enableAutoRemoveHighlightAfterValidateSensitiveWord();
			if(!enableOfficeSensitiveWordValidate) {
				enableAutoRemoveHighlightAfterValidateSensitiveWord = false;
			}
			boolean addExtraHighlight = "true".equals(request.getParameter("addExtraHighlight"));
			if(addExtraHighlight && enableAutoRemoveHighlightAfterValidateSensitiveWord && newfileid > 0) {
				WordProcessUtil wordProcessUtil = new WordProcessUtil();
				wordProcessUtil.removeHighlightInPlace(newfileid);
			}
			apidatas = accService.saveAcc(newfileid,oldfileid,id,moduleParams,user,false);
			String ipAddress = request.getRemoteAddr();
			accService.checkIn(ipAddress,oldfileid,id,user);
			apidatas.put("api_status", true);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}


	/**
	 * office转换
	 * @author wangqs
	 * */
	@GET
	@Path("/convertFileForMobile")
	@Produces(MediaType.TEXT_PLAIN)
	public String convertFileForMobile(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {

		int fileid = Util.getIntValue(request.getParameter("fileid"),0);
		int docid = Util.getIntValue(request.getParameter("docid"),0);
		int versionId = Util.getIntValue(request.getParameter("versionId"),0);
		String convertType = Util.null2String(request.getParameter("convertFile"));
		boolean isEmail = "email".equals(request.getParameter("model"));
		int  workflowid=Util.getIntValue(request.getParameter("workflowid"));

		Map<String,Object> apidatas = new HashMap<String,Object>();

		if(fileid > 0){

			RecordSet rs = new RecordSet();
			boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
			String isUsePoi = Util.null2String(rs.getPropValue("doc_mobile_isusepoi", "isUsePoi"));
			rs.writeLog("convertFileForMobile--->isUsePoi:"+isUsePoi);
			String filename = "";
			String comefrom = "";
			if(isEmail){
				MailFilePreviewService mfps = new MailFilePreviewService();
				filename = mfps.getFileNameOnly(fileid + "");
			}else{
				rs.executeQuery("select imagefilename,comefrom from ImageFile where imagefileid=?",fileid);
				if(rs.next()){
					filename = rs.getString("imagefilename").toLowerCase();
					comefrom =  rs.getString("comefrom");

				}
			}

			String noWpsView="false";
			RecordSet recordSet=new RecordSet();
			String  useKgPluginSql="select usekg from uf_useKgPlugin where concat(',',workflowid,',') like '%,"+workflowid+",%'";
			recordSet.writeLog("convertFileForMobile-->useKgPluginSql----->"+useKgPluginSql);
			recordSet.executeQuery(useKgPluginSql) ;
			while (recordSet.next()){
				if (recordSet.getInt(1)==1){
					noWpsView="true";
					break;
				}

			}

			String extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "";
			Boolean ispic = isPic(extName);
			rs.writeLog("convertFileForMobile--->extName:"+extName+"-->ispic:"+ispic);
			if((filename.endsWith(".html")||filename.endsWith(".htm"))&&"WorkflowToDoc".equals(comefrom)){
				Map<String,String> params = new HashMap<String,String>();
				List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
				params.put("ismobile","1");
				params.put("userAgent",request.getHeader("user-agent"));
				Map<String,String> data = new HashMap<String,String>();
				dataList.add(data);
				data.put("path",weaver.general.GCONST.getContextPath()+"/weaver/weaver.file.FileDownload?fileid=" + fileid +"&nolog=1");
				apidatas.put("data",dataList);
				apidatas.put("result",0);
				apidatas.put("convert","client");
				return JSONObject.toJSONString(apidatas);
			}
			rs.writeLog("convertFileForMobile--->isUsePDFViewer:"+isUsePDFViewer+"-->filename:"+filename);

			//==zj
			boolean isOpenWps = false;	//设置开关状态
			User user1 = HrmUserVarify.getUser (request , response);
			try{
				//查看开关状态
				RecordSet wps = new RecordSet();
				wps.executeQuery("select isopen from wps_config where type='wps'");
				if (wps.next()){
					int isOpen = wps.getInt("isopen");
					//如果开关是开启状态
					if (isOpen == 1){
						int uid = user1.getUID();		//用户id
						String subcompanyName = "";		//分部id
						String subcompanylist = "";		//分部wps权限数组
						String hrmsql = "select id from hrmsubcompany where id = (select subcompanyid1 from Hrmresource where id =" +  uid + ")";
						new BaseBean().writeLog("==zj==(页面--根据用户id查询分部id)" + hrmsql);
						wps.executeQuery(hrmsql);	//查询用户id对应分部id
						if (wps.next()){
							subcompanyName = Util.null2String(wps.getString("id"));		//如果有赋值
							new BaseBean().writeLog("==zj==(页面--获取账号所在分部)" + subcompanyName);
						}
						//将分部id作为条件在建模表进行查询
						RecordSet sub = new RecordSet();
						String Docsql = "select subcompanyname from uf_wps";
						new BaseBean().writeLog("==zj==(页面--查询建模表分部信息)" + Docsql);
						sub.executeQuery(Docsql);
						if (sub.next()){
							subcompanylist = Util.null2String(sub.getString("subcompanyname"));
							new BaseBean().writeLog("==zj==（页面--获取到wps权限分部数组）" + JSON.toJSONString(subcompanylist));
							String[] result = subcompanylist.split(",");
							new BaseBean().writeLog("==zj==（页面--截取后的数据）" + JSON.toJSONString(result));
							for (int i = 0; i < result.length; i++) {
								if (subcompanyName.equals(result[i]) && !"".equals(subcompanyName)){
									isOpenWps = true;
									break;
								}
							}
						}
					}
				}
				new BaseBean().writeLog("==zj==(页面--是否开启wps)" + isOpenWps);
			}catch (Exception e){
				new BaseBean().writeLog("==zj==(exception)" + e);
			}


			//==zj 是否来自流程正文
			Boolean flowDoc = true;
			if (workflowid == -1){
				flowDoc = false; //不是来自流程正文
			}

			//==zj 是否来自流程pdf
			String flowFile = "0";		//0:不是来自流程，1：来自流程
			if (flowDoc){
				//如果来自流程
				flowFile = "1";
			}
			
			//判断是否来自流程正文，如果是流程正文则不控制。
			new BaseBean().writeLog("==zj==(移动端文档判断条件)" +workflowid +"," +  flowDoc + "," + isOpenWps);
				if (flowDoc || (isOpenWps || ispic||(filename.endsWith(".pdf")&&!isOpenWps))) {
					if (isUsePDFViewer || filename.endsWith(".pdf") || ispic) {
						new BaseBean().writeLog("==zj==(文档wps预览)");
						ImageConvertUtil icu = new ImageConvertUtil();
						boolean openConverForClient = icu.convertForClient();
						if ("true".equals(noWpsView)) {
							openConverForClient = false;
						}
						rs.writeLog("convertFileForMobile--->openConverForClient:" + openConverForClient);
						if (openConverForClient || filename.endsWith(".pdf") || ispic) { //开启单独服务转换
							User user = HrmUserVarify.getUser(request, response);
							String clientAddress = request.getRequestURL().toString();
							clientAddress = clientAddress.substring(0, clientAddress.indexOf(weaver.general.GCONST.getContextPath() + "/api/doc/acc/convertFileForMobile"));
							String _convertType = icu.getConvertType();    //单独服务器转换时，转换类型以配置为准
							rs.writeLog("convertFileForMobile--->_convertType:" + _convertType);
							if (_convertType != null && !_convertType.isEmpty()) {
								convertType = _convertType;
							}

							convertType = "html";
							String agent = request.getHeader("user-agent");
							if ((agent.contains("Firefox") || agent.contains(" Chrome") || agent.contains("Safari")) || agent.contains("Edge")) {
								convertType = "pdf";
							}

							//String converSelf = Util.null2String(request.getParameter("converSelf"));

							Map<String, String> params = new HashMap<String, String>();
							//params.put("converSelf",converSelf);
							params.put("ismobile", "1");
							params.put("userAgent", request.getHeader("user-agent"));
							params.put("noWpsView", noWpsView);
							//==zj 添加一个值来判断是否来自流程
							params.put("flowFile",flowFile);

							new BaseBean().writeLog("==zj==(流程pdf)" +filename + " | " + flowFile);
							apidatas = icu.convertForPath(fileid, user, clientAddress, params, convertType,
									isEmail ? ImageConvertUtil.EMAIL_ACC_TABLE : ImageConvertUtil.DOC_ACC_TABLE);
							apidatas.put("convert", "client");

							new BaseBean().writeLog("==zj==(wps预览)" + filename);
							return JSONObject.toJSONString(apidatas);
						}
					}
				}

			apidatas.put("convert","system");	//转换方式：client-单独服务转换，system-本系统转换
			rs.writeLog("convertFileForMobile-22222-->isUsePDFViewer:"+isUsePDFViewer+"-->convertType:"+convertType);

			//==zj==pdf走默认预览
			new BaseBean().writeLog("==zj==（filename）" + filename);
			if(isUsePDFViewer && filename.endsWith(".pdf")/*"pdf".equals(convertType)*/){
				new BaseBean().writeLog("==zj==(pdf方法进入)");
				ConvertPDFTools convertPDFTools = new ConvertPDFTools();
				int pdffileid = -1;
				if(isEmail){
					pdffileid= convertPDFTools.conertToPdfForEmail(fileid,filename,0);
				}else{
					String isWeaverSystem = rs.getPropValue("doc_custom_for_weaver", "is_weaver_system");
					isWeaverSystem = "1";
					for(int i = 0 ;  i < 5 ; i ++){
						String sql = "select * from pdf_imagefile where imagefileid="+fileid;
						rs.executeQuery(sql);
						if(rs.next()){
							pdffileid = Util.getIntValue(rs.getString("pdfimagefileid"),0);
							if(pdffileid <= 0){
								Thread.sleep(2000);
							}else{
								break;
							}
						}else{
							if("1".equals(isWeaverSystem)){
								new BaseBean().writeLog("==zj==(pdffileid,fileid)" + pdffileid + "," + fileid);
								pdffileid= convertPDFTools.conertToPdf(fileid);

							}else{
								pdffileid= convertPDFTools.conertToPdf(fileid + "");
							}
							break;
						}
					}
				}
				//==zj==pdf的fileid赋值
				pdffileid = fileid;
				if(pdffileid > 0){
					apidatas.put("status", 1);
					apidatas.put("convertId", pdffileid);
				}else{
					apidatas.put("status", -1);
				}

			}else if("1".equals(isUsePoi)){
				new BaseBean().writeLog("==zj==（isUsePoi方法进入）");
				try{
					int htmlFileId = -1;
					DocPreviewHtmlManager dphm = new DocPreviewHtmlManager();
					if(isEmail){
						htmlFileId = dphm.doFileConvertForEmail(fileid,filename,0);
					}else{
						htmlFileId = dphm.doFileConvert(fileid,null,null,docid,versionId);
					}
					if(htmlFileId > 0){
						new BaseBean().writeLog("==zj==(Poi)" + filename);
						apidatas.put("status", 1);
						apidatas.put("convertId", htmlFileId);
					}else{
						apidatas.put("status", -1);
					}
				}catch(Exception e){
					apidatas.put("status", -2);
					apidatas.put("msg", e.getMessage());
				}
			}
		}else{
			apidatas.put("status", -3);
		}

		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * office转换
	 * @author wangqs
	 * */
	@GET
	@Path("/convertFile")
	@Produces(MediaType.TEXT_PLAIN)
	public String convertFile(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		
		int fileid = Util.getIntValue(request.getParameter("fileid"),0);
		int docid = Util.getIntValue(request.getParameter("docid"),0);
		int versionId = Util.getIntValue(request.getParameter("versionId"),0);
		String convertType = Util.null2String(request.getParameter("convertFile"));
		boolean existSensitiveWords = false;
		Map<String,Object> apidatas = new HashMap<String,Object>();
		int originalFileId  = -1;
		if(fileid > 0){
			
			RecordSet rs = new RecordSet();
			// oops
			DocSensitiveWordsUtil docSensitiveWordsUtil = new DocSensitiveWordsUtil();
			String sensitiveWordNotification = "";
			if(docSensitiveWordsUtil.enablePreviewValidateSensitiveWordHighlight()) {
				String fileName = DocUtils.getImageFilename(fileid);
				String fileExt = CommonUtils.getFileExt(fileName);
				if(docSensitiveWordsUtil.canPreviewCheckOfficeOfficeType(fileExt)) {
					try {
					WordProcessUtil wordProcessUtil = new WordProcessUtil();
					wordProcessUtil.setFromPreview(true);
					SearchResult searchResult = wordProcessUtil.searchAndHighlightSensitiveWords(fileid);
					if(searchResult != null && searchResult.getOccurenceCount() > 0) {
						int highlightfileId = searchResult.getHighlightFileId();
						existSensitiveWords = true;
						User user = HrmUserVarify.getUser (request , response) ;
						int languageId = user == null ? 7 : user.getLanguage();
						if(highlightfileId > 0) {
							originalFileId = fileid;
							fileid = highlightfileId;
						}
						int labelId = highlightfileId > 0 ? 531734 : 532320;
						sensitiveWordNotification = SystemEnv.getHtmlLabelName(labelId,languageId);
					}
					} catch (Exception e) {
						new BaseBean().writeLog(this.getClass().getName(),e);
					}
				}
			}
			boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
			if(isUsePDFViewer){
				
				boolean isViewForWps = ImageConvertUtil.canViewForWps();
				//金山wps
				if(isViewForWps){
					User user = HrmUserVarify.getUser (request , response) ;
					Map<String,String> params = new HashMap<String,String>();
					params.put("userAgent",request.getHeader("user-agent"));
					apidatas = WebOfficeUtil.convert(fileid,user,params);
					apidatas.put("convert","client");
					if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
						apidatas.put("existSensitiveWords",true);
						apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
					}
					if(originalFileId > 0) {
						apidatas.put("fileid",originalFileId);
					}
					return JSONObject.toJSONString(apidatas);
				}
				
				ImageConvertUtil icu = new ImageConvertUtil();
				boolean openConverForClient = icu.convertForClient();
				if(openConverForClient){ //开启单独服务转换
					User user = HrmUserVarify.getUser (request , response) ;
					String clientAddress = request.getRequestURL().toString();
					clientAddress = clientAddress.substring(0,clientAddress.indexOf(weaver.general.GCONST.getContextPath()+"/api/doc/acc/convertFile"));
					String _convertType = icu.getConvertType();	//单独服务器转换时，转换类型以配置为准
					if(_convertType != null && !_convertType.isEmpty()){
						convertType = _convertType;
					}
					
					convertType = "html";
					String agent = request.getHeader("user-agent");
				    if((agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") )|| agent.contains("Edge")){
				    	convertType = "pdf";
				    }
					
					//String converSelf = Util.null2String(request.getParameter("converSelf"));

					Map<String,String> params = new HashMap<String,String>();
					//params.put("converSelf",converSelf);

					apidatas = icu.convertForPath(fileid,user,clientAddress,params,convertType,ImageConvertUtil.DOC_ACC_TABLE);
					if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
						apidatas.put("existSensitiveWords",true);
						apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
					}
					if(originalFileId > 0) {
						apidatas.put("fileid",originalFileId);
					}
					apidatas.put("convert","client");
					return JSONObject.toJSONString(apidatas);
				}
			}
			
			apidatas.put("convert","system");	//转换方式：client-单独服务转换，system-本系统转换
			
			
			if("pdf".equals(convertType)){
				rs.executeQuery("select imagefilename from ImageFile where imagefileid=?",fileid);
				String filename = "";
				if(rs.next()){
					filename = rs.getString("imagefilename").toLowerCase();
				}
				if(filename.endsWith(".pdf")){
					apidatas.put("status", 1);
					apidatas.put("convertId", fileid);
					if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
						apidatas.put("existSensitiveWords",true);
						apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
					}
					if(originalFileId > 0) {
						apidatas.put("fileid",originalFileId);
					}
					return JSONObject.toJSONString(apidatas);
				}
				
				
				String isWeaverSystem = rs.getPropValue("doc_custom_for_weaver", "is_weaver_system");
				int pdffileid = -1;
				for(int i = 0 ;  i < 5 ; i ++){
					String sql = "select * from pdf_imagefile where imagefileid="+fileid;
					rs.executeQuery(sql);
					if(rs.next()){
					    pdffileid = Util.getIntValue(rs.getString("pdfimagefileid"),0);
						if(pdffileid <= 0){
							Thread.sleep(2000);
						}else{
							break;
						}
					}else{
					    ConvertPDFTools convertPDFTools = new ConvertPDFTools();
						if("1".equals(isWeaverSystem)){
							pdffileid= convertPDFTools.conertToPdf(fileid);
						}else{
							pdffileid= convertPDFTools.conertToPdf(fileid + "");
						}
						break;
					}
					
				}
				
				if(pdffileid > 0){
					if(existSensitiveWords) {
						apidatas.put("existSensitiveWords",true);
						apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
					}
					if(originalFileId > 0) {
						apidatas.put("fileid",originalFileId);
					}
					apidatas.put("status", 1);
					apidatas.put("convertId", pdffileid);
				}else{
					apidatas.put("status", -1);
				}
				
			}else if("html".equals(convertType)){
				try{
					DocPreviewHtmlManager dphm = new DocPreviewHtmlManager();
					User user = HrmUserVarify.getUser (request , response) ;
					dphm.setUser(user);
					int htmlFileId=dphm.doFileConvert(fileid,null,null,docid,versionId);
					if(htmlFileId > 0){
						if(existSensitiveWords) {
							apidatas.put("existSensitiveWords",true);
							apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
						}
						if(originalFileId > 0) {
							apidatas.put("fileid",originalFileId);
						}
						apidatas.put("status", 1);
						apidatas.put("convertId", htmlFileId);
					}else{
						apidatas.put("status", -1);
					}
				}catch(Exception e){
					apidatas.put("status", -2);
					apidatas.put("msg", e.getMessage());
				}
			}
		}else{
			apidatas.put("status", -3);
		}
		
		return JSONObject.toJSONString(apidatas);
	}
	
	@POST
	@Path("/convertFile")
	@Produces(MediaType.TEXT_PLAIN)
	public String convertFile2(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		int fileid = Util.getIntValue(request.getParameter("fileid"),0);
		String convertType = Util.null2String(request.getParameter("convertFile"));
		
		User user = HrmUserVarify.getUser (request , response) ;
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		if(fileid > 0){
			String clientAddress = request.getRequestURL().toString();
			clientAddress = clientAddress.substring(0,clientAddress.indexOf(weaver.general.GCONST.getContextPath()+"/api/doc/acc/convertFile"));
			ImageConvertUtil icu = new ImageConvertUtil();
			//String _convertType = icu.getConvertType();	//单独服务器转换时，转换类型以配置为准
			////if(_convertType != null && !_convertType.isEmpty()){
			//	convertType = _convertType;
			//}
			/*if("pdf".equals(convertType)){
				PdfConvertUtil pcu = new PdfConvertUtil();
				apidatas = pcu.convert(fileid,user,clientAddress,new HashMap<String,String>(),ImageConvertUtil.DOC_ACC_TABLE);
			}else if("jpg".equals(convertType) || "html".equals(convertType)){
				apidatas = icu.convert(fileid,user,clientAddress,new HashMap<String,String>(),convertType,ImageConvertUtil.DOC_ACC_TABLE);
			}*/
			convertType = "html";
			Map<String,String> map =  new HashMap<String,String>();
			map.put("ismobile","1");
			map.put("userAgent",request.getHeader("user-agent"));
			apidatas = icu.convertForPath(fileid,user,clientAddress,map,convertType,ImageConvertUtil.DOC_ACC_TABLE);
		}else{
			apidatas.put("result", -3);
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	@GET
	@Path("/getAccFileConvert")
	@Produces(MediaType.TEXT_PLAIN)
	public String getAccFileConvert(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		boolean existSensitiveWords = false;
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			int fileid = Util.getIntValue(request.getParameter("fileid"),0);
			int docid = Util.getIntValue(request.getParameter("docid"));
			
			RecordSet rs = new RecordSet();
			if(fileid <= 0){
				if(docid > 0){
					String mainfileid = DocDetailUtil.getMainImagefile(docid+"");
					if(!"".equals(Util.null2String(mainfileid))){
						fileid = Util.getIntValue(mainfileid);
					}else{
						Map<String,String> fileMap = DocDetailUtil.getDocFirstfile(docid+"");
						if(fileMap != null && fileMap.get("imagefileid") != null){
							fileid = Util.getIntValue(fileMap.get("imagefileid"));
						}
					}
				}
			}else{
				int getNewestVersion = Util.getIntValue(request.getParameter("getNewestVersion"));
				if(getNewestVersion == 1){
					rs.executeQuery("select imagefileid from docimagefile where id in (select id from docimagefile where imagefileid = ? and docid=?) order by versionid desc",fileid,docid);
					if(rs.next()){
						fileid = rs.getInt("imagefileid");
					}
				}
			}
			if(fileid <= 0){
				apidatas.put("result", 2);
				apidatas.put("status", 1);
				return JSONObject.toJSONString(apidatas);
			}
			
			apidatas.put("fileid",fileid);
			
			rs.executeQuery("select a.imagefilename,a.filesize,a.comefrom from imagefile a,DocImageFile b where a.imagefileid=? and b.docid=? and a.imagefileid=b.imagefileid",fileid,docid);
			String filename = "";
			String filesize = ""; 
			String comefrom = "";
			if(rs.next()){
				filename = Util.null2String(rs.getString("imagefilename"));
				filesize = Util.null2String(rs.getString("filesize"));
				comefrom = Util.null2String(rs.getString("comefrom"));
			}else{
				apidatas.put("result", 2);
				apidatas.put("status", 1);
				return JSONObject.toJSONString(apidatas);
			}
			int originalFileId = fileid;
			// oops
			DocSensitiveWordsUtil docSensitiveWordsUtil = new DocSensitiveWordsUtil();
			String sensitiveWordNotification = "";
			if(docSensitiveWordsUtil.enablePreviewValidateSensitiveWordHighlight()) {
				String fileName = DocUtils.getImageFilename(fileid);
				String fileExt = CommonUtils.getFileExt(fileName);
				if(docSensitiveWordsUtil.canPreviewCheckOfficeOfficeType(fileExt)) {
					try {
						WordProcessUtil wordProcessUtil = new WordProcessUtil();
						wordProcessUtil.setFromPreview(true);
						SearchResult searchResult = wordProcessUtil.searchAndHighlightSensitiveWords(fileid);
						if (searchResult != null && searchResult.getOccurenceCount() > 0) {
							existSensitiveWords = true;
							int highlightfileId = searchResult.getHighlightFileId();
							int languageId = user == null ? 7 : user.getLanguage();
							if (highlightfileId > 0) {
								fileid = highlightfileId;
							}
							int labelId = highlightfileId > 0 ? 531734 : 532320;
							sensitiveWordNotification = SystemEnv.getHtmlLabelName(labelId, languageId);
						}
					} catch (Exception e) {
						new BaseBean().writeLog(this.getClass().getName(),e);
					}
					
				}
			}
			String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "";
			boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
			String path = "";
			int result = -1;
			
			boolean ispic = ImageConvertUtil.isPic(extname);
			
			if(ispic || 
					((extname.equals("html") || extname.equals("html")) && "WorkflowToDoc".equals(comefrom))){
				
				List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
				Map<String,String> data = new HashMap<String,String>();
				dataList.add(data);
				data.put("path",weaver.general.GCONST.getContextPath()+"/weaver/weaver.file.FileDownload?fileid=" + fileid +"&nolog=1");
				apidatas.put("data",dataList);
				apidatas.put("result",0);
				apidatas.put("ispic",ispic ? 1 : 0);
				return JSONObject.toJSONString(apidatas);
			}
			
			
	        boolean IsUseDocPreview = "1".equals(new BaseBean().getPropValue("docpreview","IsUseDocPreview"));  //是否开启预览
	        boolean IsUseDocPreviewForIE = "1".equals(new BaseBean().getPropValue("docpreview","IsUseDocPreviewForIE"));
	        
	        String __agent = request.getHeader("user-agent");
	        //如果非IE
		    boolean isIE = true;
		    if((__agent.contains("Firefox")||__agent.contains(" Chrome")||__agent.contains("Safari") )|| __agent.contains("Edge")){
		    	isIE = false;
		    }
		    
		    if(IsUseDocPreview && isUsePDFViewer && (!isIE || IsUseDocPreviewForIE)){
				ImageConvertUtil icu = new ImageConvertUtil();
				boolean openConverForClient = icu.convertForClient();
				
				 if("pdf".equals(extname) && isIE && "1".equals(new BaseBean().getPropValue("weaver_iWebPDF", "isUseiWebPDF"))){
					 path = "/docs/e9/iwebpdf.jsp?imagefileid="+fileid;
				 }else if(openConverForClient){
					boolean isToLarge = icu.isTooLarge(extname,filesize);
					if(isToLarge){
						path = "/docs/pdfview/web/sysRemind.jsp?labelid=999";
					}else if(!extname.isEmpty() && ImageConvertUtil.canConvertType(extname)){
						Map<String,String> map =  new HashMap<String,String>();
						String clientAddress = request.getRequestURL().toString();
						clientAddress = clientAddress.substring(0,clientAddress.indexOf("/api/doc/acc/getAccFileConvert"));
						
						map.put("userAgent",request.getHeader("user-agent"));
						map.put("model","ecology");
						apidatas = icu.convertForPath(fileid,user,clientAddress,map,"pdf",ImageConvertUtil.DOC_ACC_TABLE);
						apidatas.put("fileid",originalFileId);
						if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
							apidatas.put("existSensitiveWords",true);
							apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
						}
						return JSONObject.toJSONString(apidatas);
					}else{
						path = "/wui/common/page/sysRemind.jsp?labelid=129755";
					}
				}else{ //永中dcc
					if("pdf".equals(extname)){
						path = "/docs/pdfview/web/pdfViewer.jsp?imagefilename="+URLEncoder.encode(filename,"utf-8")+"&pdfimagefileid="+fileid;
			    		path += "&requestid=0&canPrint=false&canDownload=false";
					}else if(ImageConvertUtil.canConvertType(extname) && 
							!"zip".equals(extname) && !"rar".equals(extname)){
						ConvertPDFTools convertPDFTools = new ConvertPDFTools();
						//filename
						int pdffileid= convertPDFTools.conertToPdf(fileid);
						if(pdffileid > 0){
							path = "/docs/pdfview/web/pdfViewer.jsp?imagefilename="+URLEncoder.encode(filename,"utf-8")+"&pdfimagefileid="+fileid;
				    		path += "&requestid=0&canPrint=false&canDownload=false";
							if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
								apidatas.put("existSensitiveWords",true);
								apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
							}
						}else{
							 path = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
						}
					}else{
						path = "/wui/common/page/sysRemind.jsp?labelid=129755";
					}
					
				}
				
			}else{
				
				boolean iweb2015 = IWebOfficeConf.canIwebOffice();
				
				if(iweb2015){
					boolean __iweb2015 = "1".equals(Util.null2String(DocPlugUtil.getoffice215Set().get("isopen")));
					boolean isChinaSystem = IWebOfficeConf.isChinaSystem(__agent);
					if(!isChinaSystem && !__iweb2015){   //不是国产操作系统，并且不是2015，则走03,09
						iweb2015 = false;
					}
				}
				
			    
			    if("pdf".equals(extname)){
			    	
			    	if(isIE && "1".equals(new BaseBean().getPropValue("weaver_iWebPDF", "isUseiWebPDF"))){
			    		path = "/docs/e9/iwebpdf.jsp?imagefileid="+fileid;
			    	}else{
						if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
							apidatas.put("existSensitiveWords",true);
							apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
						}
						
			    		path = "/docs/pdfview/web/pdfViewer.jsp?imagefilename="+URLEncoder.encode(filename,"utf-8")+"&pdfimagefileid="+fileid;
			    		path += "&requestid=0&canPrint=false&canDownload=false";
			    	}
			    	
		    	}else if((!isIE && !iweb2015) || (isIE && IsUseDocPreview && IsUseDocPreviewForIE)){
			    	if("doc".equals(extname) || "docx".equals(extname) || "xls".equals(extname) || "xlsx".equals(extname)){
			    		int maxFileSize = Util.getIntValue(new BaseBean().getPropValue("docpreview","maxFileSize"),5);
						int fileSize = Util.getIntValue(filesize,0);
						if(fileSize>maxFileSize*1024*1024){
							path = "/docs/pdfview/web/sysRemind.jsp?labelid=999";
						}
						try{
							 DocPreviewHtmlManager dphm = new DocPreviewHtmlManager();
							 dphm.setUser(user);
							 int htmlFileId = dphm.doFileConvert(fileid,null,null,0,0);
							 if(htmlFileId <= 0){
								 path = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
							}else{
								if(existSensitiveWords && Util.getIntValue("" + apidatas.get("result"),-1) == 0) {
									apidatas.put("existSensitiveWords",true);
									apidatas.put("sensitiveWordNotification",sensitiveWordNotification);
								}
								path = "/weaver/weaver.file.FileDownload?fileid="+htmlFileId;
								result = 0;
							}
						}catch(Exception fpe){	
						//	path = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
							path = "/wui/common/page/sysRemindDocpreview.jsp?labelid=" + URLEncoder.encode(fpe.getMessage(),"utf-8");
							new BaseBean().writeLog(fpe);
						}
			    	}else{
			    		path = "/wui/common/page/sysRemind.jsp?labelid=129755";
			    	}
			    }else{
			    	//path = "/docs/e9/email/office.jsp?fileid=" + fileid;
			    	//result = 0;
			    	path = "/wui/common/page/sysRemind.jsp?labelid=129755";   //插件查看
			    }
			    
			}
			
		    List<Map<String,String>> pathList = new ArrayList<Map<String,String>>();
			Map<String,String> pathMap = new HashMap<String,String>();
			pathMap.put("path",weaver.general.GCONST.getContextPath()+path);
			pathList.add(pathMap);
			apidatas.put("data",pathList);
			apidatas.put("result",result);
			apidatas.put("fileid",originalFileId);
			
			
			apidatas.put("status", 1);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
		
	}
	
	@POST
	@Path("/convertForEmail")
	@Produces(MediaType.TEXT_PLAIN)
	public String convertForEmail(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		User user = HrmUserVarify.getUser (request , response) ;
		int fileid = Util.getIntValue(request.getParameter("fileid"),0);
		
		MailFilePreviewService mfps = new MailFilePreviewService();
		Map<String, String> fileInfo = mfps.getFileInfoMap(user.getUID(),fileid + "");
		String filename = Util.null2String(fileInfo.get("filename"));
		String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "";
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
		String path = "";
		int result = -1;
		
        boolean IsUseDocPreview = "1".equals(new BaseBean().getPropValue("docpreview","IsUseDocPreview"));  //是否开启预览
        boolean IsUseDocPreviewForIE = "1".equals(new BaseBean().getPropValue("docpreview","IsUseDocPreviewForIE"));
        
        String __agent = request.getHeader("user-agent");
        //如果非IE
	    boolean isIE = true;
	    if((__agent.contains("Firefox")||__agent.contains(" Chrome")||__agent.contains("Safari") )|| __agent.contains("Edge")){
	    	isIE = false;
	    }
	    
	    
		
		if(IsUseDocPreview && isUsePDFViewer && (!isIE || IsUseDocPreviewForIE)){
			ImageConvertUtil icu = new ImageConvertUtil();
			boolean openConverForClient = icu.convertForClient();
			
			 if("pdf".equals(extname) && (isIE && "1".equals(new BaseBean().getPropValue("weaver_iWebPDF", "isUseiWebPDF"))|| IWebOfficeConf.canIwebPDF(request.getHeader("user-agent")))){
				 path = "/docs/e9/iwebpdf.jsp?imagefileid="+fileid+"&model=email";
			 }else if(openConverForClient){
				boolean isToLarge = icu.isTooLarge(extname,fileInfo.get("filesize"));
				if(isToLarge){
					path = "/docs/pdfview/web/sysRemind.jsp?labelid=999";
				}else if(ImageConvertUtil.canConvertType(extname)){
					Map<String,String> map =  new HashMap<String,String>();
					String clientAddress = request.getRequestURL().toString();
					clientAddress = clientAddress.substring(0,clientAddress.indexOf("/api/doc/acc/convertForEmail"));
					
					map.put("userAgent",request.getHeader("user-agent"));
					map.put("model","email");
					apidatas = icu.convertForPath(fileid,user,clientAddress,map,"pdf",ImageConvertUtil.EMAIL_ACC_TABLE);
					
					return JSONObject.toJSONString(apidatas);
				}else{
					path = "/wui/common/page/sysRemind.jsp?labelid=129755";
				}
			}else{ //永中dcc
				if("pdf".equals(extname)){
					path = "/docs/pdfview/web/pdfViewer.jsp?imagefilename="+URLEncoder.encode(filename,"utf-8")+"&pdfimagefileid="+fileid +"&model=email";
		    		path += "&requestid=0&canPrint=false&canDownload=false";
				}else if(ImageConvertUtil.canConvertType(extname) && 
						!"zip".equals(extname) && !"rar".equals(extname)){
					ConvertPDFTools convertPDFTools = new ConvertPDFTools();
					//filename
					int pdffileid= convertPDFTools.conertToPdfForEmail(fileid,filename,0);
					if(pdffileid > 0){
						path = "/docs/pdfview/web/pdfViewer.jsp?imagefilename="+URLEncoder.encode(filename,"utf-8")+"&pdfimagefileid="+fileid;
			    		path += "&requestid=0&canPrint=false&canDownload=false";
					}else{
						 path = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
					}
				}else{
					path = "/wui/common/page/sysRemind.jsp?labelid=129755";
				}
				
			}
			
		}else{
			
			boolean iweb2015 = IWebOfficeConf.canIwebOffice();
			
			if(iweb2015){
				boolean __iweb2015 = "1".equals(Util.null2String(DocPlugUtil.getoffice215Set().get("isopen")));
				boolean isChinaSystem = IWebOfficeConf.isChinaSystem(__agent);
				if(!isChinaSystem && !__iweb2015){   //不是国产操作系统，并且不是2015，则走03,09
					iweb2015 = false;
				}
			}
			
		    
		    if("pdf".equals(extname)){
		    	
		    	if(isIE && "1".equals(new BaseBean().getPropValue("weaver_iWebPDF", "isUseiWebPDF"))|| IWebOfficeConf.canIwebPDF(request.getHeader("user-agent"))){
		    		path = "/docs/e9/iwebpdf.jsp?imagefileid="+fileid+"&model=email";
		    	}else{
		    		path = "/docs/pdfview/web/pdfViewer.jsp?imagefilename="+URLEncoder.encode(filename,"utf-8")+"&pdfimagefileid="+fileid +"&model=email";
		    		path += "&requestid=0&canPrint=false&canDownload=false";
		    	}
		    	
	    	}else if((!isIE && !iweb2015) || (isIE && IsUseDocPreview && IsUseDocPreviewForIE)){
	    		boolean poiTohtml = ImageConvertUtil.isUsePoiForPC();  //是否开启POI转换预览
		    	if(poiTohtml && ("doc".equals(extname) || "docx".equals(extname) || "xls".equals(extname) || "xlsx".equals(extname))){
		    		int maxFileSize = Util.getIntValue(new BaseBean().getPropValue("docpreview","maxFileSize"),5);
					int fileSize = Util.getIntValue(fileInfo.get("filesize"),0);
					if(fileSize>maxFileSize*1024*1024){
						path = "/docs/pdfview/web/sysRemind.jsp?labelid=999";
					}
					try{
						 int htmlFileId = new DocPreviewHtmlManager().doFileConvertForEmail(fileid,filename,0);
						 if(htmlFileId == -1){
							 path = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
						}else{
							path = "/weaver/weaver.file.FileDownload?fileid="+htmlFileId+"&model=email";
							result = 0;
						}
					}catch(Exception fpe){	
					//	path = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
						path = "/wui/common/page/sysRemindDocpreview.jsp?labelid=" + URLEncoder.encode(fpe.getMessage(),"utf-8");
						new BaseBean().writeLog(fpe);
					}
		    	}else{
		    		path = "/wui/common/page/sysRemind.jsp?labelid=129755";
		    	}
		    }else{
		    	path = "/docs/e9/email/office_email.jsp?fileid=" + fileid;
		    	result = 0;
		    }
		    
		}
		
		List<Map<String,String>> pathList = new ArrayList<Map<String,String>>();
		Map<String,String> pathMap = new HashMap<String,String>();
		pathMap.put("path",weaver.general.GCONST.getContextPath()+path);
		pathList.add(pathMap);
		apidatas.put("data",pathList);
		apidatas.put("result",result);
		
		return JSONObject.toJSONString(apidatas);
		
	}
	
	/**
	 * 删除已转文档在数据库的记录，以便再次转换(用于转换有异常时调试使用)
	 * @author wangqs
	 * */
	@GET
	@Path("/deleteConvert")
	@Produces(MediaType.TEXT_PLAIN)
	public String deleteConvert(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			int fileid = Util.getIntValue(request.getParameter("fileid"),0);
			
			ImageConvertUtil icu = new ImageConvertUtil();
			icu.deleteConvert(fileid,user);
			apidatas.put("status", 1);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	

	/**
	 * 删除附件
	 * @author wangqs
	 * */
	@GET
	@Path("/delete")
	@Produces(MediaType.TEXT_PLAIN)
	public String delete(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			Map<String,String> params = getRequestMap(request);
			int fileid = Util.getIntValue(request.getParameter("fileid"),0);
			int docid = Util.getIntValue(request.getParameter("id"),0);
//			Map<String,String> params =

			DocAccService accService = new DocAccService();
			boolean flag = accService.deleteDocAcc(fileid,docid,user,params);
			apidatas.put("status", flag ? 1 : 0);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 重命名附件
	 * */
	@POST
	@Path("/rename")
	@Produces(MediaType.TEXT_PLAIN)
	public String rename(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			int fileid = Util.getIntValue(request.getParameter("imagefileid"),0);
			String filename = Util.null2String(request.getParameter("imagefilename"));
			
			DocAccService accService = new DocAccService();
			boolean flag = accService.renameFilename(fileid,filename,user);
			apidatas.put("status", flag ? 1 : 0);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	
	/**
	 * 从永中服务器下载文件
	 * */
	@POST
	@Path("/loadFromYoZo")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String loadFromYoZo(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		String filepath = Util.null2String(request.getParameter("filepath"));
		String filename = Util.null2String(request.getParameter("filename"));
		String loadFileId = Util.null2String(request.getParameter("fileId"));
		try{
			User user = HrmUserVarify.getUser(request, response);
			apidatas = YozoWebOfficeUtil.loadFromYoZo(filepath,filename, loadFileId, user);
			apidatas.put("api_status", true);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 从wps服务器下载文件
	 * */
	@POST
	@Path("/loadFromWps")
	@Produces(MediaType.TEXT_PLAIN)
	public String loadFromWps(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		String filename = Util.null2String(request.getParameter("filename"));
		String token = Util.null2String(request.getParameter("token"));
		String column = Util.null2String(request.getParameter("column"));
		String wpsFileid = Util.null2String(request.getParameter("wpsFileid"));
		
		
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			apidatas = WebOfficeUtil.downloadFromWps(column,wpsFileid,filename,token,user);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 获取wps预览签名
	 * */
	@GET
	@Path("/getWpsSign")
	@Produces(MediaType.TEXT_PLAIN)
	public String getWpsSign(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		String sign = Util.null2String(request.getParameter("sign"));
		
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			
			apidatas = new WPSViewUtils().getSign(user,sign);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	
	/**
	 * ftp方式附件生成文档
	 * */
	@POST
	@Path("/ftpAccUpload")
	@Produces(MediaType.TEXT_PLAIN)
	public String ftpAccUpload(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			DocAccService accService = new DocAccService();
			apidatas = accService.ftpAccUpload(request,response);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 下载附件到FTP服务器
	 * */
	@POST
	@Path("/ftpAccDown")
	@Produces(MediaType.TEXT_PLAIN)
	public String ftpAccDown(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			DocAccService accService = new DocAccService();
			apidatas = accService.ftpAccDown(request,response);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 删除ftp服务器文件
	 * */
	@POST
	@Path("/ftpAccDelete")
	@Produces(MediaType.TEXT_PLAIN)
	public String ftpAccDelete(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			DocAccService accService = new DocAccService();
			apidatas = accService.ftpAccDelete(request,response);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * word导入生成html
	 * */
	@POST
	@Path("/importToHtml")
	@Produces(MediaType.TEXT_PLAIN)
	public String importToHtml(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		int fileid = Util.getIntValue(request.getParameter("imagefileid"));
		
		apidatas = new WordToHtmlUtil().toHtml(fileid,request);
    	
    	return JSONObject.toJSONString(apidatas);
	}
	
	/**
	 * 获取附件base64
	 * */
	@GET
	@Path("/getFileBase64")
	@Produces(MediaType.TEXT_PLAIN)
	public String getFileBase64(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser (request , response) ;
			String fileid = Util.null2String(request.getParameter("fileid"));
			
			String _fileid = DocDownloadCheckUtil.getDownloadfileidstr(fileid);
			
			DocAccService accService = new DocAccService();
			apidatas = accService.getBase64(Util.getIntValue(_fileid),user,request);
			
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}

	private boolean isPic(String extName){
		BaseBean bb = new BaseBean();
		String isusepoi = Util.null2s(bb.getPropValue("doc_mobile_isusepoi","isUsePoi"),"0");
		if("jpg".equals(extName.toLowerCase()) ||
				"jpeg".equals(extName.toLowerCase()) ||
				"png".equals(extName.toLowerCase()) ||
				"gif".equals(extName.toLowerCase()) ||
				"bmp".equals(extName.toLowerCase())
				){
			return true;
		}
		return false;
	}

	/**
	 * 上传文件到永中服务中
	 * 移动端使用的接口
	 * */
	@GET
	@Path("/uploadFromYozo")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String uploadFromYozo(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String, Object> apidatas = new HashMap<String,Object>();
		int fileid = Util.getIntValue(request.getParameter("fileid"));
		String mFileType = Util.null2String(request.getParameter("mFileType"));
		try{
			User user = HrmUserVarify.getUser(request, response);
			Map<String, String> dataMap = YozoWebOfficeUtil.uploadToYozo(fileid, mFileType, user);
			apidatas.putAll(dataMap);
			apidatas.put("api_status", true);
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}

	/**
	 * 上传文件到永中服务中
	 * 移动端使用的接口
	 * */
	@POST
	@Path("/openFileFromYozo")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String openFileFromYozo(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String, Object> apidatas = new HashMap<String,Object>();
		Map<String,String> paramMap = getRequestMap(request);
		try{
			User user = HrmUserVarify.getUser(request, response);
			if(null == user) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", "no user");
				return JSONObject.toJSONString(apidatas);
			}

			Map<String, String> dataMap = YozoWebOfficeUtil.openFile2Yozo(paramMap, user);
			if(!"0".equalsIgnoreCase(dataMap.get("result"))) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", apidatas.get("message"));
			} else {
				apidatas.putAll(dataMap);
				apidatas.put("api_status", true);
			}
			apidatas.put("hostName", YozoWebOfficeUtil.getYozoProperties("yozo_hostname"));
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}

	/**
	 * 保存WebOffice服务中的文件
	 * 移动端使用的接口
	 * */
	@POST
	@Path("/saveFileFromYozo")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String saveFileFromYozo(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String, Object> apidatas = new HashMap<String,Object>();
		Map<String,String> paramMap = getRequestMap(request);
		try{
			User user = HrmUserVarify.getUser(request, response);
			if(null == user) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", "no user");
				return JSONObject.toJSONString(apidatas);
			}

			Map<String, String> dataMap = YozoWebOfficeUtil.saveFile2Yozo(paramMap, user);
			if(!"0".equalsIgnoreCase(dataMap.get("result"))) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", apidatas.get("message"));
			} else {
				apidatas.putAll(dataMap);
				apidatas.put("api_status", true);
			}
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}

	/**
	 * 关闭WebOffice的服务中的文件
	 * 移动端使用的接口
	 * */
	@POST
	@Path("/closeFileFromYozo")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String closeFileFromYozo(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String, Object> apidatas = new HashMap<String,Object>();
		Map<String,String> paramMap = getRequestMap(request);
		try{
			User user = HrmUserVarify.getUser(request, response);
			if(null == user) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", "no user");
				return JSONObject.toJSONString(apidatas);
			}

			Map<String, String> dataMap = YozoWebOfficeUtil.closeFile2Yozo(paramMap, user);
			if(!"0".equalsIgnoreCase(dataMap.get("result"))) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", apidatas.get("message"));
			} else {
				apidatas.putAll(dataMap);
				apidatas.put("api_status", true);
			}
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}

	/**
	 * 判断文件是否打开了
	 * 移动端使用的接口
	 * */
	@POST
	@Path("/isOpenFromYozo")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public String isOpenFromYozo(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
		Map<String, Object> apidatas = new HashMap<String,Object>();
		Map<String,String> paramMap = getRequestMap(request);
		try{
			User user = HrmUserVarify.getUser(request, response);
			if(null == user) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", "no user");
				return JSONObject.toJSONString(apidatas);
			}

			Map<String, String> dataMap = YozoWebOfficeUtil.isOpen2Yozo(paramMap, user);
			if(!"0".equalsIgnoreCase(dataMap.get("result"))) {
				apidatas.put("api_status", false);
				apidatas.put("api_errormsg", apidatas.get("message"));
			} else {
				apidatas.putAll(dataMap);
				apidatas.put("api_status", true);
			}
		}catch (Exception e) {
			e.printStackTrace();
			apidatas.put("api_status", false);
			apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
		}
		return JSONObject.toJSONString(apidatas);
	}
}
