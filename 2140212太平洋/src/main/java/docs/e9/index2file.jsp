<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page import="weaver.conn.*"%>
<%@ page import="weaver.general.*"%>
<%@ page import="weaver.hrm.*" %>
<%@ page import="java.util.Map,java.util.HashMap,java.util.Hashtable,java.util.List,java.util.ArrayList" %>
<%@ page import="weaver.docs.pdf.docpreview.ConvertPDFUtil"%>
<%@ page import="weaver.docs.pdf.docpreview.ConvertPDFTools" %>
<%@ page import="com.api.doc.detail.service.DocDetailService"%>
<%@ page import="weaver.docs.category.SecCategoryDocPropertiesComInfo"%>
<%@ page import="weaver.docs.category.SecCategoryComInfo"%>
<%@ page import="weaver.docs.mould.MouldManager"%>
<%@ page import="weaver.systeminfo.SystemEnv"%>
<%@ page import="com.api.doc.detail.service.DocViewPermission"%>
<%@ page import="java.util.Date"%>
<%@ page import="com.api.doc.detail.service.DocSaveService"%>
<%@ page import="com.api.doc.search.util.DocSptm"%>
<%@ page import="com.api.doc.detail.util.ImageConvertUtil"%>
<%@page import="com.engine.doc.util.IWebOfficeConf"%>
<%@ page import="com.engine.doc.util.DocPlugUtil" %>
<%@ page import="com.api.doc.detail.util.DocDetailUtil" %>
<%@ page import="com.api.doc.search.service.DocLogService" %>
<%@ page import="org.apache.bcel.generic.NEW" %>
<%@ page import="com.api.doc.detail.util.SendMsgForNewDocThread,com.cloudstore.dev.api.bean.MessageType" %>
<%@page import="java.net.*" %>
<%@ page import="com.api.doc.detail.util.DocDownloadCheckUtil" %>
<%@ page import="com.api.doc.yozo.weboffice.util.YozoWebOfficeUtil" %>
<%@ page import="org.apache.commons.lang.time.DateFormatUtils" %>
<%@ page import="weaver.docs.docs.util.DocumentDeleteSecurityUtil" %>
<%@ page import="weaver.docs.docs.util.DocumentDeleteStatusMould" %>
<jsp:useBean id="DocComInfo" class="weaver.docs.docs.DocComInfo" scope="page"/>
<jsp:useBean id="DocPreviewHtmlManager" class="weaver.docs.docpreview.DocPreviewHtmlManager" scope="page"/>
<jsp:useBean id="DepartmentComInfo" class="weaver.hrm.company.DepartmentComInfo" scope="page"/>
<jsp:useBean id="ResourceComInfo" class="weaver.hrm.resource.ResourceComInfo" scope="page"/>
<jsp:useBean id="CustomerInfoComInfo" class="weaver.crm.Maint.CustomerInfoComInfo" scope="page" />
<jsp:useBean id="LanguageComInfo" class="weaver.systeminfo.language.LanguageComInfo" scope="page" />
<jsp:useBean id="DocDsp" class="weaver.docs.docs.DocDsp" scope="page" />

<%
	//==zj
	boolean isOpenWps = false;	//根据分部来判断是否开启wps
	User user1 = HrmUserVarify.getUser(request,response);
	int uid = user1.getUID();
	String subcompanyName = "";		//分部id
	String subcompanyList = "";		//分部wps权限数组
	try{
		RecordSet wps = new RecordSet();
		wps.executeQuery("select isopen from wps_config where type='wps'");
		if (wps.next()){
			int isOpen = wps.getInt("isopen");
			if (isOpen == 1){
				String hrmsql = "select id from hrmsubcompany where id = (select subcompanyid1 from Hrmresource where id =" +  uid + ")";
				new BaseBean().writeLog("==zj==(附件--Hrmresource查询)" + hrmsql);
				wps.executeQuery(hrmsql);
				if (wps.next()){
					subcompanyName = Util.null2String(wps.getString("id"));
					new BaseBean().writeLog("==zj==（附件--获取账号所在分部）" + subcompanyName);
				}
				//查询建模表
				String subsql = "select subcompanyname from uf_wps";
				new BaseBean().writeLog("==zj==(附件--查询建模表分部信息)" + subsql);
				wps.executeQuery(subsql);
				if (wps.next()){
					subcompanyList = Util.null2String(wps.getString("subcompanyname"));
					String[] result = subcompanyList.split(",");
					for (int i = 0; i < result.length; i++) {
						if (subcompanyName.equals(result[i]) && !"".equals(subcompanyName)){
							isOpenWps = true;
							break;
						}
					}
				}
				new BaseBean().writeLog("==zj==(附件--isOpenWps)" + isOpenWps);
			}
		}
	}catch (Exception e){
		new BaseBean().writeLog("==zj==(Exception)" + e);
	}


	/*//brs
	boolean isOpenWps = false;	//是否开启WPS
	try{
		RecordSet wps = new RecordSet();
		wps.executeQuery("select isopen from wps_config where type='wps'");
		if (wps.next()){
			isOpenWps = wps.getInt("isopen")==1;
		}
	}catch (Exception e){
		new BaseBean().writeLog("brs -Exception- "+e);
	}*/
	//--
    String CONTEXTPATH = weaver.general.GCONST.getContextPath();//url，api请求都请加上此参数,二级路径处理
	boolean isEdit = "1".equals(request.getParameter("isEdit"));
	boolean useNewAccView = ImageConvertUtil.useNewAccView();
	String warmMessageStatus = Util.null2String(request.getParameter("warmMessageStatus"));
	if(!isEdit && useNewAccView){
		int imagefileId = Util.getIntValue(request.getParameter("imagefileId"),-1);
		int docid = Util.getIntValue(request.getParameter("id"),-1);
			RecordSet rs = new RecordSet();
		if(docid <= 0 && imagefileId > 0){
			rs.executeQuery("select docid from DocImageFile where imagefileid=?",imagefileId);
			if(rs.next()){
				docid = rs.getInt("docid");
			}
		}
		if(docid > 0){
			
			boolean fromRequest = !"1".equals(request.getParameter("fromVesionClick")); 
			if(fromRequest){  //查询最新版本的附件id
				rs.executeQuery("select imagefileid from docimagefile where id in (select id from docimagefile where imagefileid = ? and docid=?) order by versionid desc",imagefileId,docid);
				if(rs.next()){
					imagefileId = rs.getInt("imagefileid");
				}
			}
			
			String moudleParams = new DocViewPermission().getMoudleParams(request);
			if ("checkout".equals(warmMessageStatus)) {
			%>
				<script>
				location.href = "<%=DocSptm.DOC_DETAIL_LINK + "?imagefileId="+imagefileId+"&id=" + docid +"&warmMessageStatus=checkout" +DocSptm.DOC_ROOT_FLAG_VALUE + moudleParams + DocSptm.DOC_DETAIL_ROUT %>"
				</script>
			<%
			}else{
				%>
				<script>
				location.href = "<%=DocSptm.DOC_DETAIL_LINK + "?imagefileId="+imagefileId+"&id=" + docid + DocSptm.DOC_ROOT_FLAG_VALUE + moudleParams + DocSptm.DOC_DETAIL_ROUT %>"
				</script>
			<%
			}
			return;
		}
	}

	/*** 其他模块跳转过来的时候，不带路由，这里统一加上 ***/
	String router = request.getParameter(DocSptm.DOC_ROOT_FLAG);
	if(router == null){
	  %>
	 <script>
	 	var _href = location.href;
	 	if(_href.indexOf("#/main/document/fileView") == -1){
	 		location.href = _href + "&<%=DocSptm.DOC_ROOT_FLAG%>=1" + "#/main/document/fileView";
	 	}else{
	 		location.href = _href.replace("#/main/document/fileView","&<%=DocSptm.DOC_ROOT_FLAG%>=1#/main/document/fileView");
	 	}
	 </script>

	  <%
	  return;
	}
%>


<%
	response.setHeader("Pragma", "no-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expires", 0);
    User user = HrmUserVarify.getUser (request , response);
	  if(user==null){
		  response.sendRedirect(CONTEXTPATH+"/wui/index.html");
		  return ;
	  }

    long time1 = 0;
    long time2 = 0;
    long time3 = 0;

	Date d1 = new Date();
	String jsVersion = DateFormatUtils.format(d1,"yyyyMMdd");
	boolean onlyUrl4Content = false;
	boolean fix2015ForNotIE = false;
    //判断客户端操作系统
    boolean isWindows = false;
    boolean isLinux = false;
    // 文档ID
    int docid = Util.getIntValue(request.getParameter("id"),-1);

    String attachtype = request.getParameter("attachtype");

    int imagefileId = Util.getIntValue(request.getParameter("imagefileId"),-1);
    int versionId = Util.getIntValue(request.getParameter("versionId"),-1);
    String imagefiledesc = "";
    
    String warmMessage = "";

    String _convertFile = Util.null2String(request.getParameter("convertFile"));

    String checkOutStatus = "";
    int checkOutUserId = -1;

    String model = Util.null2String(request.getParameter("model"));
    boolean isReplyAcc = "reply".equals(model);

    if(isReplyAcc){
    	docid = Util.getIntValue(request.getParameter("docid"),-1);
    }

    Boolean docisLock = DocDetailUtil.isopendoclock(docid+"","doc");
    Boolean fromRequest = false;
    String isrequest = Util.null2s(request.getParameter("isrequest"),"");
	  if((Util.getIntValue(request.getParameter("requestid"),0) > 0) || "1".equals(isrequest)){
		  fromRequest = true;
	  }
    // 附件版本ID
    if(docid < 0 && imagefileId < 0)
    {
        response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
        return ;
    }

    if(docid > 0 && imagefileId >0){
    	RecordSet rs = new RecordSet();
    	if(isReplyAcc){
    		rs.executeQuery("select b.id from REPLY_IMAGEFILE a,DOC_REPLY b where a.REPLY_ID=b.id and a.IMAGEFILEID=? and b.docid=?",imagefileId,docid);
    	}else if(versionId > 0){
    		rs.executeQuery("select id from DocImageFile where imagefileid=? and docid=? and versionid=?",imagefileId,docid,versionId);
    	}else{
    		rs.executeQuery("select id from DocImageFile where imagefileid=? and docid=?",imagefileId,docid);
    	}
    	if(!rs.next()){//附件和文档不匹配
    		response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
    		return;
    	}
    }

    if(docid <= 0 && imagefileId > 0){

    	int oldimagefileId = imagefileId;
    	RecordSet rs = new RecordSet();
    	rs.executeQuery("select imagefileid from PDF_IMAGEFILE where pdfimagefileid=" + imagefileId);
    	if(rs.next()){
    		imagefileId = rs.getInt("imagefileid") > 0 ? rs.getInt("imagefileid") : imagefileId;
    		versionId = -1;
    	}else{
    		rs.executeQuery("select imagefileid from DocPreviewHtml where htmlfileid=" + imagefileId);
    		if(rs.next()){
        		imagefileId = rs.getInt("imagefileid") > 0 ? rs.getInt("imagefileid") : imagefileId;
        		versionId = -1;
        	}
    	}

    	rs.executeQuery("select docid,versionid from DocImageFile where imagefileid=" + imagefileId + (versionId > 0 ? (" and versionid=" + versionId) : ""));
    	if(rs.next()){
    		docid = rs.getInt("docid");
    		versionId = rs.getInt("versionid");
    	}
    	if(docid > 0){
	    	%>
	    		<script>
	    			location.href = location.href.replace("imagefileId=<%=oldimagefileId%>","imagefileId=<%=imagefileId%>&id=<%=docid%>&versionId=<%=versionId%>");
	    		</script>
	    	<%
	    	return;
    	}
    }else if(docid > 0 && imagefileId <= 0){
    	RecordSet rs = new RecordSet();
    	rs.executeQuery("select imagefileid,versionid from DocImageFile where docid=" + docid + (versionId > 0 ? (" and versionid=" + versionId) : "") + " order by versionid desc");
    	if(rs.next()){
    		imagefileId = rs.getInt("imagefileid");
    		versionId = rs.getInt("versionid");
    		%>
    			<script>
    			location.href = location.href.replace("&imagefileId=","&_imagefileId=").replace("&versionId","&_versionId").replace("#/main/document/fileView","&imagefileId=<%=imagefileId%>&versionId=<%=versionId%>#/main/document/fileView");
    			</script>
    		<%
    		return;
    	}else{
    		response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
            return ;
    	}
    }


    if(versionId <= 0 && docid > 0)
	{
		RecordSet rs = new RecordSet();
		rs.executeQuery("select versionid from DocImageFile where docid=" + docid + " and imagefileid=" + imagefileId + " order by versionid desc");
		if(rs.next()){
			versionId = rs.getInt("versionid");
		}

	}


    boolean canRead = false;
    boolean canEdit = false;
    /**判断文档查看权限*/

    String useNew = "1";//是否使用新的权限判断
    Map<String,String> rightParams = new HashMap<String,String>();
    rightParams.put("useNew",useNew);

    DocViewPermission dvps = new DocViewPermission();
    Map<String,Boolean> levelMap = new HashMap<String,Boolean>();
    //其他模块参数集
    String moudleParams = dvps.getMoudleParams(request);
    if(docid > 0){

    	 //密级判断
       	if(!dvps.hasRightForSecret(user,docid)){
       		response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp?secretNotEnough=1") ;
       		return;
       	}

    	levelMap = dvps.getShareLevel(docid,user,false);
		if(!levelMap.get(DocViewPermission.EDIT)){ //知识没有权限，判断是否是其他模块
			dvps.hasEditRightFromOtherMould(docid,levelMap,user,request);
		}

		canEdit = levelMap.get(DocViewPermission.EDIT);
		RecordSet rs = new RecordSet();
   		String checkOutUserType = "";
   		rs.executeQuery("select doctype,docextendname,checkOutStatus,checkOutUserId,checkOutUserType from DocDetail where id=" + docid);
       	if(rs.next()){
       		checkOutStatus = rs.getString("checkOutStatus");
			checkOutUserId = rs.getInt("checkOutUserId");
			checkOutUserType = rs.getString("checkOutUserType");
       	}
    	if(isEdit){
	        if(checkOutStatus!=null&&(checkOutStatus.equals("1")||checkOutStatus.equals("2"))&&!(checkOutUserId==user.getUID()&&checkOutUserType!=	null&&checkOutUserType.equals(user.getLogintype()))){
					//签出状态的文档，跳转详情页
	        		canEdit = false;
					
                    response.sendRedirect(DocSptm.ACC_DETAIL_LINK + "?id=" + docid + "&imagefileId=" + imagefileId + "&versionId=" + versionId + "&warmMessageStatus=checkout" + moudleParams + DocSptm.DOC_ROOT_FLAG_VALUE + DocSptm.ACC_DETAIL_ROUT);
                    		return;
			}
        	
            if (!canEdit) {
%>
<script>
    	location.href = location.href.replace("&isEdit","<%="1".equals(checkOutStatus) || "2".equals(checkOutStatus) ? "&warmMessageStatus=checkout&isEdits" : "&isEdits"%>").replace("?isEdit","<%="1".equals(checkOutStatus) || "2".equals(checkOutStatus) ? "?warmMessageStatus=checkout&isEdits" : "?isEdits"%>");
</script>
<%
                return;
            }
            String sql = "select imagefileid,versionid from DocImageFile where id=(select id from DocImageFile where imagefileid=? and docid=? and versionid=?) order by versionid desc";
            rs.executeQuery(sql, imagefileId, docid, versionId);
            if (rs.next()) {
                imagefileId = rs.getInt("imagefileid");
                versionId = rs.getInt("versionid");
            }

			canRead = true;
    	}else{
    		canRead = levelMap.get(DocViewPermission.READ);
    	}
    }else{
    	levelMap.put(DocViewPermission.READ,true);
		levelMap.put(DocViewPermission.DOWNLOAD,false);
		canRead = true;
    }
    RecordSet rsrq = new RecordSet();
    fromRequest = !"1".equals(request.getParameter("fromVesionClick")); 
	if(fromRequest){
		//rsrq.executeQuery("select docid,imagefileid,versionid from docimagefile where docid in (select docid from docimagefile where imagefileid = ?) order by versionid desc",imagefileId);
		
		if(docid > 0){
			rsrq.executeQuery("select docid,imagefileid,versionid from docimagefile where id in (select id from docimagefile where imagefileid = ? and docid=?) order by versionid desc",imagefileId,docid);
		}else{
			rsrq.executeQuery("select docid,imagefileid,versionid from docimagefile where id in (select id from docimagefile where imagefileid = ?) order by versionid desc",imagefileId);
		}
		
		int newimagefileid = imagefileId;
		int newversionId = versionId;
		if(rsrq.next()){
			newversionId = rsrq.getInt("versionid");
			newimagefileid = rsrq.getInt("imagefileid");
			if(newimagefileid>imagefileId){
				%>
					<script>
						location.href = location.href.replace("imagefileId=<%=imagefileId%>","imagefileId=<%=newimagefileid%>").replace("versionId=<%=versionId%>","versionId=<%=newversionId%>");
					</script>
				<%
				return;
			}
		}
	}
    if(!canRead){
    	levelMap.put(DocViewPermission.READ,dvps.hasRightFromOtherMould(docid,user,request));
    	canRead = levelMap.get(DocViewPermission.READ);
    }

    if(!canRead){
    	response.sendRedirect(CONTEXTPATH+"/notice/noright.jsp") ;
    }
	if(canRead){
		DocumentDeleteSecurityUtil dds = new DocumentDeleteSecurityUtil();
		dds.recoverDoc(docid+"",DocumentDeleteStatusMould.DOCUMENT.getMouldCode(),user,docid+"","","查看文档后对逻辑删除的文档进行恢复");
	}
	if(!isEdit && docid >0){
		DocLogService docLogService = new DocLogService();
	    String ipAddress = Util.getIpAddr(request);
	    docLogService.addReadLog(docid,user,ipAddress);
	}
	if(docisLock && levelMap.get(DocViewPermission.EDIT)){
		docisLock = false;
	}

    // 标题
    String docsubject = "";
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
    // 文档类型
    int doctype = -1;
    //文档目录
    int seccategory = -1;
    //文档状态
    String docstatus = "0";
    //失效日期
    String invalidationdate = "";
    //文档所属部门
    String docdepartmentid = "";
    //是否是历史文档
    int ishistory = -1;
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

    String prevname = SystemEnv.getHtmlLabelName(156,user.getLanguage()) + ": ";
    String imagefilename = "";

    boolean isOpenAcc = false; // 是否单附件打开
    String OpenAccOfFileid = "";   //单附件打开的附件id
    String OpenAccOfFilename = "";   //单附件打开的附件名称
    String OpenAccOfFiletype = "";   //单附件打开的附件类型
    String OpenAccOfVersionid = "";   //单附件打开的附件版本
    int OpenAccOfFileSize = 0;  //单附件打开的附件大小
	String OpenAccOfcomefrom = "";   //单附件打开的附件来源

    boolean canPrint = dvps.getPrint(docid,user,levelMap,null);
	boolean canDownload = levelMap.get(DocViewPermission.DOWNLOAD);

    String convertFile = "";  //转换文件  空-不转换，html-转成html，pdf-转成pdf

    boolean openWpsView = false;

    //是否开启PDF转换
    boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
	//判断浏览器类型，版本，多少位
	String agent = request.getHeader("user-agent");
	// 是否可以使用iWebPDF
	boolean canIwebPDF = IWebOfficeConf.canIwebPDF(agent);
	boolean canIwebPDF2018 = IWebOfficeConf.canIwebPDF2018();

	String _extname = "";

    //选择显示模版的ID
    String selectedpubmould = Util.null2String(request.getParameter("selectedpubmould"));

    if(imagefileId > 0){

    }else{

	    List<String> columns = new ArrayList<String>();
	    columns.add("doctype");
	    columns.add("seccategory");
	    columns.add("docstatus");
	    columns.add("invalidationdate");
	    columns.add("docdepartmentid");
	    columns.add("ishistory");
	    columns.add("selectedpubmouldid");
	    columns.add("doccreaterid");
	    columns.add("usertype");
	    columns.add("doccreatedate");
	    columns.add("doclangurage");
	    columns.add("docCode");
	    columns.add("replydocid");
	    columns.add("docapprovedate");
	    columns.add("docpublishtype");
            columns.add("checkOutUserId");
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
		    selectedpubmouldid = Util.getIntValue(data.get("selectedpubmouldid").toString(),-1);
		    doccreaterid = Util.getIntValue(data.get("doccreaterid").toString(),-1);
		    usertype = Util.null2String(data.get("usertype").toString(),"");
		    doccreatedate = Util.null2String(data.get("doccreatedate").toString(),"");
		    doclangurage = Util.getIntValue(data.get("doclangurage").toString(),7);
		    docCode = Util.null2String(data.get("docCode").toString(),"");
		    doclangurage = Util.getIntValue(data.get("replydocid").toString(),0);
		    docapprovedate = Util.null2String(data.get("docapprovedate").toString(),"");
		    docpublishtype = Util.null2String(data.get("docpublishtype").toString(),"");
                    checkOutUserId = Util.getIntValue(data.get("checkOutUserId").toString(), -1);
	    }


	    //html文档
	    if(doctype == 1){
	    	docContent = detailService.getDocContent(docid,user);
			//Date d3 = new Date();
			//time2 = d3.getTime() - d2.getTime();
	    	//if(contentMap != null && contentMap.get("data") != null){
	    	//	Map data = (Map)contentMap.get("data");
		    //	docContent = Util.null2String(data.get("doccontent"));
	    	//}
	        if(DocDetailService.ifContentEmpty(docContent))
	        {
	            //根据目录判断是否单附件直接打开,附件类型是不是word,excel,pdf，ppt
	            RecordSet rs = new RecordSet();

	            rs.executeSql("select isOpenAttachment from docseccategory where id=" + seccategory);
	            if(rs.next() && "1".equals(rs.getString("isOpenAttachment"))){  //目录是否开启单附件打开
	            	isOpenAcc = true;

		            rs.executeSql("select a.id,a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize,b.comefrom from docimagefile a,imagefile b " +
		            		" where a.imagefileid=b.imagefileid and a.docid=" + docid + " order by versionid desc");
		            String id = "";
		            isOpenAcc  = true;
		            while(rs.next()){   //是否是单附件
		            	if("".equals(id)){
		            		id = rs.getString("id");
		            		OpenAccOfFileid = rs.getString("imagefileid");
		            		OpenAccOfFilename = rs.getString("imagefilename");
		            		OpenAccOfFiletype = rs.getString("docfiletype");
		            		OpenAccOfVersionid = rs.getString("versionId");
		            		OpenAccOfFileSize = rs.getInt("filesize");
		            		OpenAccOfcomefrom = rs.getString("comefrom");
		            	}
		            	if(!id.equals(rs.getString("id"))){
		            		isOpenAcc = false;
		            	}
		            }
	            }
	        }else{
	        	SecCategoryDocPropertiesComInfo scdpc = new SecCategoryDocPropertiesComInfo();

	        	int docmouldid = -1;

	        	if(scdpc.getDocProperties(""+seccategory,"10") && "1".equals(scdpc.getVisible())){
	       			RecordSet rs = new RecordSet();
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

	       			if(HrmUserVarify.checkUserRight("DocEdit:Publish",user,docdepartmentid) && docstatus.equals("6") && (ishistory!=1) && docstatus.equals("6")){
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

	        	Hashtable<String,String> hr = new Hashtable<String,String>();
	        	SecCategoryComInfo scci = new SecCategoryComInfo();
	        	hr.put("DOC_SecCategory",Util.null2String(scci.getSecCategoryname(""+seccategory)));
	        	hr.put("DOC_Department",Util.null2String("<a href='"+CONTEXTPATH+"/hrm/company/HrmDepartmentDsp.jsp?id="+docdepartmentid+"'>"+Util.toScreen(DepartmentComInfo.getDepartmentname(""+docdepartmentid),user.getLanguage())+"</a>"));
	        	hr.put("DOC_Content",Util.null2String(docContent));

	        	if(usertype.equals("2"))  {
	        	    hr.put("DOC_CreatedBy",Util.null2String(Util.toScreen(CustomerInfoComInfo.getCustomerInfoname(""+doccreaterid),user.getLanguage())));
	        	    hr.put("DOC_CreatedByLink",Util.null2String("<a href='"+CONTEXTPATH+"/CRM/data/ViewCustomer.jsp?CustomerID="+doccreaterid+"'>"+Util.toScreen(CustomerInfoComInfo.getCustomerInfoname(""+doccreaterid),user.getLanguage())+"</a>"));
	        	    hr.put("DOC_CreatedByFull",Util.null2String(Util.toScreen(CustomerInfoComInfo.getCustomerInfoname(""+doccreaterid),user.getLanguage())));
	        	}else {
	        	    hr.put("DOC_CreatedBy",Util.null2String(Util.toScreen(ResourceComInfo.getFirstname(""+doccreaterid),user.getLanguage())));
	        	    hr.put("DOC_CreatedByLink",Util.null2String("<a href='javaScript:openhrm("+doccreaterid+");' onclick='pointerXY(event);'>"+Util.toScreen(ResourceComInfo.getResourcename(""+doccreaterid),user.getLanguage())+"</a>"));
	        	    hr.put("DOC_CreatedByFull",Util.null2String(Util.toScreen(ResourceComInfo.getResourcename(""+doccreaterid),user.getLanguage())));
	        	}

	        	hr.put("DOC_CreatedDate",Util.null2String(doccreatedate));
	        	hr.put("DOC_DocId",Util.null2String(Util.add0(docid,12)));
	        	hr.put("DOC_ModifiedBy",Util.null2String(Util.toScreen(ResourceComInfo.getFirstname(""+doclastmoduserid),user.getLanguage())));
	        	hr.put("DOC_ModifiedDate",Util.null2String(doclastmoddate));
	        	hr.put("DOC_Language",Util.null2String(LanguageComInfo.getLanguagename(""+doclangurage)));
	        	hr.put("DOC_ParentId",Util.null2String(Util.add0(replydocid,12)));
	        	String docstatusname = DocComInfo.getStatusView(docid,user);
	        	hr.put("DOC_Status",Util.null2String(docstatusname));
	        	hr.put("DOC_Subject",Util.null2String(docsubject));
	        	String tmppublishtype="";
	        	if(docpublishtype.equals("2")) tmppublishtype=SystemEnv.getHtmlLabelName(227,user.getLanguage());
	        	else if(docpublishtype.equals("3")) tmppublishtype=SystemEnv.getHtmlLabelName(229,user.getLanguage());
	        	else tmppublishtype=SystemEnv.getHtmlLabelName(58,user.getLanguage());
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

	    // office文件：正文word、excel   或者是单附件打开
	    if(doctype == 2  || isOpenAcc || imagefileId > 0)
	    {
	        // 附件ID
	        String imageFileid = "";
	        // 模板ID
	        String mTemplate = "";
	        // 附件名称
	    	String imageFileName="";
	        // 附件类型
	    	String fileType="";
	        // 操作用户
	    	String mUserName = user.getLastname();
	        // 编辑状态
	        String editType = isEdit ? "-1,0,1,1,0,0,1" : "4";
	        String isofficeview = isEdit ? "0" : "1";
	        // 附件类型标识
	    	int docFileType = 0;
	        // 附件大小
	        int filesize = 0;
	        // 是否文件过大
	        boolean isToLarge = false;
	        String comefrom = "";

	        RecordSet rs = new RecordSet();
	        String sql = "";

			 if(imagefileId > 0){
	        	 sql = "select a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize,b.comefrom from DocImageFile a,Imagefile b " +
	        	 " where a.imagefileid=b.imagefileid and a.imagefileid="+imagefileId+
	        	 (versionId > 0 ? " and a.versionId=" + versionId : "")
	        	 +" order by versionId desc";

	        	 if(isReplyAcc || docid <= 0){  //是回复里的文档、或者是
	        		 sql = "select b.imagefilename,b.filesize from Imagefile b where b.imagefileid=" + imagefileId;
	        	 }

			     rs.executeSql(sql);
	        	if(rs.next()){
	        		imageFileName = Util.null2String(rs.getString("imagefilename"),"");
					comefrom = Util.null2s(rs.getString("comefrom"),"");
		            docFileType = Util.getIntValue(rs.getString("docfiletype"),0);
		            filesize = Util.getIntValue(rs.getString("filesize"),0);
	        	}
	        	 imageFileid = imagefileId + "";
	        	 imagefilename = imageFileName;
	        }else if(doctype == 2){  // office文件
		        sql = "select a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize,b.comefrom from DocImageFile a,Imagefile b " +
		        	" where a.imagefileid=b.imagefileid and a.docid="+docid+" and (a.isextfile <> '1' or a.isextfile is null) order by a.versionId desc";
		        rs.executeSql(sql);
		        if(rs.next())
		        {
		            imageFileid = Util.null2String(rs.getString("imagefileid"),"");
		            imageFileName = Util.null2String(rs.getString("imagefilename"),"");
					comefrom = Util.null2String(rs.getString("comefrom"),"");
		            docFileType = Util.getIntValue(rs.getString("docfiletype"),0);
		            versionId = Util.getIntValue(rs.getString("versionId"),0);
		            filesize = Util.getIntValue(rs.getString("versionId"),0);
		        }
	        }else if(isOpenAcc){  //单附件打开

	            imageFileid = OpenAccOfFileid;
	            imageFileName = OpenAccOfFilename;
	            docFileType = Util.getIntValue(OpenAccOfFiletype,0);
	            versionId = Util.getIntValue(OpenAccOfVersionid,0);
	            filesize = OpenAccOfFileSize;
	            comefrom = OpenAccOfcomefrom;
	        }

	        String extname = imageFileName.indexOf(".") > -1 ? imageFileName.substring(imageFileName.lastIndexOf(".")+ 1) : "";
	        extname = extname.toLowerCase();
			imagefilename = imagefilename.replaceAll("[\\t\\n\\r]","");
			_extname = imagefilename;


	        boolean canIWebOffice = !(agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") || agent.contains("Edge"));
	        boolean isIE = canIWebOffice;

			
			if(agent.contains("Windows")){
				isWindows = true;
			}
			if(agent.contains("Linux")){
				isLinux = true;
			}
			//OFD阅读器类型
			String readerType = Util.null2String(rs.getPropValue("weaver_OFDReader","readerType"));
			//是否使用插件阅读OFD文档
			boolean useOFDReader = false;
			if((readerType.equals("0") && ((isWindows && isIE) || isLinux))
					|| (readerType.equals("1") && ((isWindows && isIE) || isLinux))
					|| (readerType.equals("2"))
					|| (readerType.equals("3"))){
				useOFDReader = true;
			}

			boolean isNotUseView2015 = (Util.getIntValue(rs.getPropValue("docpreview","isNotUseView2015"),1) == 1); //预览是否走2015 默认不走
			//brs 针对预览
			if (isOpenWps){
				isNotUseView2015=true; //不走金格
			}else {
				isNotUseView2015=false;
			}
	        boolean IsUseDocPreviewForIE = "1".equals(rs.getPropValue("docpreview","IsUseDocPreviewForIE"));
           	boolean IsUseDocPreview = "1".equals(rs.getPropValue("docpreview","IsUseDocPreview"));  //是否开启预览
			//brs 开启wps预览，但是后端应用设置WPS关闭，关闭wps预览
			if (IsUseDocPreview){
				if (!isOpenWps&&!extname.equals("pdf")){//使用金格时，pdf走pdfShow.jsp
					IsUseDocPreview=false;
				}
			}
			boolean editForPreviewForPc = "1".equals(rs.getPropValue("doc_yozo_for_weaver","previewForPc"));
			boolean useYozoEditForIE = "1".equals(rs.getPropValue("doc_yozo_for_weaver","useYozoEditForIE"));
	        boolean useYozoView = false;
	        boolean useWpsView = false;
			if(!canIWebOffice || (agent.indexOf(" like Gecko") > -1 || (agent.indexOf("Trident") > -1 && agent.indexOf("rv:11.0") > -1))){ //非IE或者ie11以上
				useYozoView = true;
			}
	        if(useYozoView){
	        	useYozoView = YozoWebOfficeUtil.canEditForYozo(extname, user, false);
	        	useWpsView = ImageConvertUtil.canEditForWps("doc",user);
	        }

            if(!canIWebOffice){
                canIWebOffice = IWebOfficeConf.canIwebOffice(agent,null);
            }
			if(canIWebOffice&&!isEdit&&(!IWebOfficeConf.isChinaSystem(agent)&&isNotUseView2015&&"1".equals(Util.null2String(DocPlugUtil.getoffice215Set().get("isopen"))))&&!isIE){
				canIWebOffice = false;
			}
			if(isIE && !useYozoEditForIE){
				useYozoView = false;
			}
	        if( isEdit && (!DocSptm.isOfficeDoc(_extname) || (!useYozoView && !canIWebOffice && !useWpsView))){
				%>
				<script>
					location.href = location.href.replace("&isEdit","").replace("?isEdit","?");
				</script>
				<%
				return;
			}

	        boolean tifReadOnline = false;
	        if(extname.equals("tif") || extname.equals("tiff")){
	        	tifReadOnline = "1".equals(rs.getPropValue("docpreview","tifReadOnline"));
	        }

	        ImageConvertUtil icu = new ImageConvertUtil();
	        if(extname.equals("jpg") || extname.equals("jpeg") || extname.equals("png") || extname.equals("gif") || extname.equals("bmp")){
				docContent = "<table style='width:100%;height:100%;background-color:#ffffff'><tr><td style='height:100%; vertical-align:middle; text-align:center;'><img style='vertical-align:middle;' src='"+CONTEXTPATH+"/weaver/weaver.file.FileDownload?fileid=" + imageFileid +moudleParams+ "&nolog=1'/></td></tr></table>";
			}else if(extname.equals("pdf") && IsUseDocPreview){
				String pdfConvert = rs.getPropValue("doc_custom_for_weaver", "pdfConvert");
				boolean convertForClient = icu.convertForClient();
				boolean isUseIwebPdf = false;
				if(isIE){
					isUseIwebPdf = "1".equals(rs.getPropValue("weaver_iWebPDF", "isUseiWebPDF"));
				}
				//zj 使用金格时，pdf走pdfShow.jsp
				if (!isOpenWps&&(canIwebPDF || convertForClient)){
					canIwebPDF =false;
					convertForClient =false;
				}
				//
				if(canIwebPDF){  // iwebpdf
					String sessionParaPDF=""+docid+"_"+imagefileId+"_"+user.getUID()+"_"+user.getLogintype();
					session.setAttribute("canView_"+sessionParaPDF,canRead ? "1" : "0");
					session.setAttribute("canEdit_"+sessionParaPDF,canEdit ? "1" : "0");
					session.setAttribute("canPrint_"+sessionParaPDF,canPrint ? "1" : "0");

					fix2015ForNotIE = canIwebPDF2018 && !isIE;
					if(fix2015ForNotIE) {
						onlyUrl4Content = true;
						docContent = CONTEXTPATH+"/docs/e9/iwebpdf.jsp?docid="+docid +"&imagefileid="+imagefileId + moudleParams ;
					} else {
						docContent = "<iframe id=\"ofdshow\" src=\""+CONTEXTPATH+"/docs/e9/iwebpdf.jsp?docid="+docid +"&imagefileid="+imagefileId + moudleParams + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}
				}else if("1".equals(pdfConvert) && convertForClient){	//移动端只判断pdfConvert，PC端需要同时判断单独转换服务器不为空时，pdf也会在单独服务器预览
					convertFile = "pdf";
					if(DocPlugUtil.isopenbjca()&&DocPlugUtil.isIE(request)){
						docContent = "<iframe id='pdfshow' src='"+CONTEXTPATH+"/docs/pdfofca/pdfShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"&isEdit="+isEdit+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
					}else{
						docContent = "<iframe id=\"officeShow\"  allowfullscreen allow=\"fullscreen\" src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}
				}else{
					if(DocPlugUtil.isopenbjca()&&DocPlugUtil.isIE(request)){
						docContent = "<iframe id='pdfshow' src='"+CONTEXTPATH+"/docs/pdfofca/pdfShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"&isEdit="+isEdit+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
					}else{
					    String filecode = DocDownloadCheckUtil.checkPermission(imageFileid,user);
						docContent = "<iframe id=\"officeShow\" src=\""+CONTEXTPATH+"/docs/pdfview/web/pdfViewer.jsp?canPrint="+canPrint+"&docisLock="+docisLock+"&canDownload="+canDownload+"&pdfimagefileid="+filecode+moudleParams+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
					}
				}
            }else if(extname.equals("ofd") && useOFDReader){
            	docContent = "<iframe id=\"ofdshow\" src=\""+CONTEXTPATH+"/docs/ofd/OfdShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"&isEdit="+canEdit+"\" style=\"display: block;width: 100%;height: 100%; border:0\" scrolling=\"no\"></iframe>";
            }else if(tifReadOnline || "WorkflowToDoc".equals(comefrom)){
            	docContent = "<iframe src=\""+CONTEXTPATH+"/weaver/weaver.file.FileDownload?fileid=" + imageFileid + moudleParams + "&nolog=1\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
            }else if(!isEdit && IsUseDocPreview && (isUsePDFViewer && !isIE || !canIWebOffice || IsUseDocPreviewForIE && isIE) && !editForPreviewForPc){ //开启预览，非ie浏览器或者ie下开启预览
	            //是否是支持转换的文档
	           	boolean canConvert = ImageConvertUtil.canConvertType(extname);

	           	if(extname.equals("zip") ||extname.equals("rar")){ //不开启单独转换，不支持压缩包
	           		if(!icu.convertForClient()){
	           			canConvert = false;
	           		}
	           	}

	           	// 采用转换PDF预览
	           	if(!IsUseDocPreview){  //未开启转换预览,且该浏览器不支持插件

		        	String sysremindurl="/wui/common/page/sysRemind.jsp?labelid=509650";
	            	if(levelMap.get(DocViewPermission.DOWNLOAD)){
	            		sysremindurl="/wui/common/page/sysRemind.jsp?labelid=129755&line=1";
	            	}
	            	docContent = "<iframe src=\""+CONTEXTPATH+sysremindurl+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
		        }else if(isUsePDFViewer && canConvert){

					isToLarge = icu.isTooLarge(extname,filesize + "");

					if(!isToLarge){
						convertFile = "pdf";
					}
					if(isToLarge){
	           			//文件过大
	           			docContent = "<iframe src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=999\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
	           		}else{
						if(DocPlugUtil.isopenbjca()&&DocPlugUtil.isIE(request)&&extname.equals("pdf")){
							docContent = "<iframe id='pdfshow' src='"+CONTEXTPATH+"/docs/pdfofca/pdfShow.jsp?docid="+docid+"&imagefileId="+imagefileId+"&isEdit="+isEdit+"' style='display: block;width: 100%;height: 100%; border:0'></iframe>";
						}else{
							docContent = "<iframe id=\"officeShow\" src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
						}
	           		}
	            }
	           	// 采用转换html查看
	           	else {
	           		boolean poiTohtml = ImageConvertUtil.isUsePoiForPC();  //是否开启POI转换预览
		           	// 采用转换html查看
		            if(poiTohtml && (extname.equals("doc") ||
		            		extname.equals("docx") ||
		            		extname.equals("xls") ||
		            		extname.equals("xlsx")))
		            {
	
		            	convertFile = "html";
		                ///weaver/weaver.file.FileDownload?fileid="+htmlFileId+ moudleParams +"
		                docContent = "<iframe id=\"officeShow\" src=\""+CONTEXTPATH+"/docs/pdfview/web/sysRemind.jsp?labelid=996\" onload=\"hideLoading();displayRightMenu(this);\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
		            }else{
		            	//不支持的格式
		            	int requestid = Util.getIntValue(request.getParameter("requestid"),0);
		            	if(requestid > 0){
		            		moudleParams += "&fromrequest=1"; 
		            	}
		            	String sysremindurl="/wui/common/page/sysRemind.jsp?labelid=509650";
		            	if(levelMap.get(DocViewPermission.DOWNLOAD)){
		            		sysremindurl="/wui/common/page/sysRemind.jsp?labelid=129755&line=2";
		            	}
		            	docContent = "<iframe src=\""+CONTEXTPATH+sysremindurl+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
		            	//docContent = "<iframe src=\"/docs/pdfview/web/sysRemind.jsp?labelid=997\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
		            }
	           	}
	        }
	        // IE 32 内核，直接加载控件
	        else
	        {
	            String mFileType = "";
	            if(docFileType == 3)
	            {
	                mFileType = ".doc";
	            }
	            else if(docFileType == 7)
	            {
	                mFileType = ".docx";
	            }
	            else if(docFileType == 4)
	            {
	                mFileType = ".xls";
	            }
	            else if(docFileType == 8)
	            {
	                mFileType = ".xlsx";
	            }else if(extname.equals("wps") || extname.equals("ppt") || extname.equals("pptx") || extname.equals("et")){
	            	mFileType = "." + extname;
	            }else if(extname.equals("uot")  &&IWebOfficeConf.isyozoOffice()){
	            	mFileType = ".doc";
	            }else if(extname.equals("uos")  &&IWebOfficeConf.isyozoOffice()){
	            	mFileType = ".xls";
	            }else if(isReplyAcc && (ImageConvertUtil.isOffice(extname.toLowerCase()) && !extname.toLowerCase().equals("wps") )){
					mFileType = "."+extname.toLowerCase();
				}else if(docFileType == 0){
					mFileType = "." + extname;
				}
				//brs 开启wps编辑，但是后端应用设置WPS关闭，关闭wps编辑
				if (useWpsView){
					if (!isOpenWps){
						useWpsView=false;
					}
				}
				//--
	            if((!isEdit && editForPreviewForPc || isEdit) && (useYozoView || useWpsView) &&
	            		(mFileType.equals(".doc") ||
	        				mFileType.equals(".docx") ||
	        				mFileType.equals(".xls") ||
	        				mFileType.equals(".xlsx") ||
	        				mFileType.equals(".ppt") ||
	        				mFileType.equals(".pptx"))){
	            	if(useWpsView){
	            		openWpsView = true;
		            	docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/docs/e9/weboffice_wps.jsp?fileid="+imageFileid+"&isEdit=1&mFileType="+mFileType+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
	            	}else{
						onlyUrl4Content = true;
						// docContent = "<iframe id=\"webOffice\" src=\"/spa/document/weboffice.jsp?fileid="+imageFileid+"&isEdit=" + (isEdit ? "1" : "0") + "&mFileType="+mFileType+"&docisLock=" + (docisLock ? "1" : "0" ) + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
						docContent = CONTEXTPATH+"/spa/document/weboffice.jsp?fileid="+imageFileid+"&isEdit=" + (isEdit ? "1" : "0") + "&mFileType="+mFileType+"&docisLock=" + (docisLock ? "1" : "0" );
	            	}
	            }else if(!mFileType.isEmpty() && canIWebOffice){
	                if(docisLock){
	                    editType = "0";
					}
	                
	                
	                if(!isEdit){
	                	selectedpubmouldid = new DocDetailService().getDocMouldId(docid,mFileType);
	                	if(selectedpubmouldid > 0){
	                		mTemplate = selectedpubmouldid + ""; 
	                	}
	                }
	                
	                int maxFileSize = 0;
		            rs.executeQuery("select a.maxofficedocfilesize from DocSecCategory a,DocDetail b where a.id=b.seccategory and b.id=?",docid);
		            if(rs.next()){
		            	maxFileSize = rs.getInt("maxofficedocfilesize");
		            }
					fix2015ForNotIE = "1".equals(Util.null2String(DocPlugUtil.getoffice215Set().get("isopen"))) && !isIE;
					if(isReplyAcc){
						if(fix2015ForNotIE) {
							onlyUrl4Content = true;
							docContent = CONTEXTPATH+"/spa/document/office.jsp?isofficeview="+ isofficeview +"&mRecordID="+versionId +"_"+docid+"&isReplyAcc=reply&imagefileid_reply="+imageFileid+"&maxFileSize="+maxFileSize+"&mTemplate=" + mTemplate +"&canPrint="+canPrint+"&mFileName=" + URLEncoder.encode(imageFileName.replaceAll("[#]+","_"),"utf-8") +"&mFileType=" + mFileType +"&mEditType=" + editType;
						} else {
							docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/spa/document/office.jsp?isofficeview="+ isofficeview +"&mRecordID="+versionId +"_"+docid+"&isReplyAcc=reply&imagefileid_reply="+imageFileid+"&maxFileSize="+maxFileSize+"&mTemplate=" + mTemplate +"&canPrint="+canPrint+"&mFileName=" + URLEncoder.encode(imageFileName.replaceAll("[#]+","_"),"utf-8") +"&mFileType=" + mFileType +"&mEditType=" + editType + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
						}
					}else{
						if(fix2015ForNotIE) {
							onlyUrl4Content = true;
							docContent = CONTEXTPATH+"/spa/document/office.jsp?isofficeview="+ isofficeview+"&imagefileid_reply="+imageFileid +"&mRecordID="+versionId +"_"+docid+"&maxFileSize="+maxFileSize+"&mTemplate=" + mTemplate +"&canPrint="+canPrint+"&mFileName=" + URLEncoder.encode(imageFileName.replaceAll("[#]+","_"),"utf-8") +"&mFileType=" + mFileType +"&mEditType=" + editType;
						} else{
							docContent = "<iframe id=\"webOffice\" src=\""+CONTEXTPATH+"/spa/document/office.jsp?isofficeview="+ isofficeview +"&imagefileid_reply="+imageFileid+"&mRecordID="+versionId +"_"+docid+"&maxFileSize="+maxFileSize+"&mTemplate=" + mTemplate +"&canPrint="+canPrint+"&mFileName=" + URLEncoder.encode(imageFileName.replaceAll("[#]+","_"),"utf-8") +"&mFileType=" + mFileType +"&mEditType=" + editType + "\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
						}
					}


	            }else{
	            	//不支持的格式
	            	String sysremindurl="/wui/common/page/sysRemind.jsp?labelid=509650";
	            	if(levelMap.get(DocViewPermission.DOWNLOAD)){
	            		sysremindurl="/wui/common/page/sysRemind.jsp?labelid=129755&line=3";
	            	}
	            	docContent = "<iframe src=\""+CONTEXTPATH+sysremindurl+"\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
	            	//docContent = "<iframe src=\"/docs/pdfview/web/sysRemind.jsp?labelid=997\" style=\"display: block;width: 100%;height: 100%; border:0\"></iframe>";
	            }
	       }
	    }

	    RecordSet rs = new RecordSet();
	    rs.executeQuery("select 1 from DocImageFile where docid="+docid+" and id in(select id from DocImageFile where docid="+docid+" and versionId="+versionId+" and isextfile=1) ");
		int versionCounts=rs.getCounts();

    	String versionSpan = "";
    	if(versionCounts > 1){
    		int versionNum = DocDsp.getVersionCounts(docid,versionId);
    		//versionSpan = "<span style='margin-left:3px'>(<span class='wea-new-top-req-title-version'>V" + versionNum + "</span>)</span>";
    	}

	    Date d4 = new Date();
	    time3 = d4.getTime()-d1.getTime();

		if(_convertFile != null && !_convertFile.isEmpty()){
			if("jpg".equals(_convertFile) || "pdf".equals(_convertFile) || "html".equals(_convertFile)){
				convertFile = _convertFile;
			}
		}
		if(isEdit && !"1".equals(checkOutStatus)&&!"2".equals(checkOutStatus)){
			DocSaveService dss = new DocSaveService();
           	dss.checkOut(docid,docsubject,doccreaterid,usertype,request.getRemoteAddr(),user);
    	}
	    

	    //文件签出
	    if ("checkout".equals(warmMessageStatus)) {
	        String checkOutUserName = ResourceComInfo.getLastname(checkOutUserId + "");
	        warmMessage = SystemEnv.getHtmlLabelName(19695, user.getLanguage()) + SystemEnv.getHtmlLabelName(19690, user.getLanguage()) + "：" + checkOutUserName;
	    }

		if(!isEdit) {
			int viewReplyFlag = Util.getIntValue(request.getParameter("viewReply"),-1);	//目录id
			int viewPraiseFlag = Util.getIntValue(request.getParameter("viewPraise"),-1);	//目录id
			int viewMessageFlag = Util.getIntValue(request.getParameter("viewMessage"),-1);	//目录id

            DocDetailService _ddService = new DocDetailService();
            _ddService.updateBizStateForDoc(docid, user, 1, 1, 1);
			// if(1 == viewReplyFlag) {
			//     SendMsgForNewDocThread.updateBizStateForCenter(user, docid, "138");
			// }
			// if(1 == viewPraiseFlag) {
			//     SendMsgForNewDocThread.updateBizStateForCenter(user, docid, "139");
			// }
			// if(1 == viewMessageFlag) {
			//     SendMsgForNewDocThread.updateBizStateForCenter(user, docid, Util.null2String(MessageType.DOC_NEW_DOC.getCode()));
			// }
		}
%>
<!--<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">-->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Frameset//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd">-->
<html>

	<head>
		<meta charset="utf-8">
		<meta name="renderer" content="webkit"/>
		<meta name="force-rendering" content="webkit"/>
		<meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1">
		<meta name="viewport" content="initial-scale=1.0, maximum-scale=2.0">
		<meta name="description" content="">
		<meta name="author" content="">
		<script>
          window.ecologyContentPath = "<%=CONTEXTPATH%>";
			var __href = location.href;
			if(__href.indexOf("#/main/document/fileView") == -1){
				//alert(location.href);

				if(__href.indexOf("#/") == -1){
					location.href = __href.replace("&router=1","");
				}else{
					__href = __href.replace("&router=1","");
					location.href = __href.substring(0,__href.indexOf("#/"));
				}

			}
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
		</script>
		<script type="text/javascript">
		  window.ecologyContentPath = "<%=CONTEXTPATH%>";
		</script>
		<title><%= imagefileId > 0 ? imagefilename : docsubject %></title>
		<link rel="stylesheet" type="text/css" href="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/index.min.css?v=<%=jsVersion%>">
		<link rel="stylesheet" type="text/css" href="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/ecCom.min.css?v=<%=jsVersion%>">
		<link rel="stylesheet" type="text/css" href="<%=CONTEXTPATH%>/spa/document/static4Detail/index.css?v=<%=jsVersion%>">
		<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/jquery/jquery.min.js"></script>
		<style type="text/css">
			body{margin: 0;overflow: hidden;}
			table#content{display:table !important;}
			.wea-new-top-req-title-version{
				color:#30B5FF;
				cursor:pointer;
			}
			.wea-new-top-req-title-label{
				color:gray;
			}
			.wea-new-top-req-wapper .wea-new-top-req-content{
				background-color:inherit;
			}
			.wea-doc-detail-content-main img{
				max-width:1024px;
			}
		   .wea-doc-detail-content-main {
			    max-height: calc(100vh - 70px);
	       } 
		</style>
	</head>
		<body>
		<div id="container" style="height:100%;">

		</div>
		<script type="text/javascript">
			window.pre_title = $('.wea-new-top-req-title-text').html();
			window.pre_subtitle = $('.wea-new-top-req-title-text-sub').html();
			window.pre_docDetail = $('.wea-doc-detail-content-main').html();
			//console.log('docContent: ', window.pre_docDetail)
			window.__time1 = "<%=SystemEnv.getHtmlLabelName(500850, user.getLanguage())%> <%=time1%>";// 获取属性耗时：
			window.__time2 = "<%=SystemEnv.getHtmlLabelName(500851, user.getLanguage())%> <%=time2%>";//获取正文耗时：
			window.__time3 = "<%=SystemEnv.getHtmlLabelName(500852, user.getLanguage())%> <%=time3%>";//业务逻辑总耗时：
			window.__extname = "<%=_extname%>";
            window.__warmMessage = "<%=warmMessage%>";//弹出提示语
			window.__moudleParams = "<%=moudleParams %>";
			window.__imagefilename = "<%=imagefilename%>";
			window.__docisLock = <%=docisLock%>;
			if("<%=convertFile%>" != ""){
				window.__convertFile = "convertFile=<%=convertFile%>&converSelf=<%=_convertFile%>&fileid=<%=imagefileId%>&docid=<%=docid%>&versionId=<%=versionId%>";		//是否需要异步转换文档
			}
            if(<%=docisLock%>){
                var ifmObj = window.document.body;
                ifmObj.style.cssText += ';-moz-user-select: none;-webkit-user-select: none;-ms-user-select: none;-khtml-user-select: none;user-select: none;unselectable:on;';
                ifmObj.onselectstart = function(){return false};
            }
			<%if(isEdit){%>
				/**调用子层office保存*/
				function saveDocument(){
					try{
						var obj = document.getElementById("webOffice").contentWindow.toSaveDocument();

						if(obj.off_status == 0){
							obj.msg = obj.off_msg ? obj.off_msg : "<%=SystemEnv.getHtmlLabelName(84544, user.getLanguage())%>";// 保存失败!
						}

						return obj;
					}catch(e){
						return {off_status : 0};
					}
				}

				window.__useWpsView = <%=openWpsView%>;
				function sendSaveMsg(btnkey){
					window.__savekey = btnkey;
					document.getElementById("webOffice").contentWindow.sendSaveMsg();
				}
				function acceptSaveMsg(){
					window.__wpsSaved = true;
					window.__onRightMenuClick(window.__savekey ? window.__savekey : "BTN_SUBMIT");
					window.__wpsSaved = false;
				}

			function validateSensitiveWordsNew(params) {
				var result;
				var fileId = "";
				var officeFileName = "";
				if(!params) {
					params = {};
				}
				jQuery.ajax({
					url : "<%=weaver.general.GCONST.getContextPath()%>/api/doc/sensitiveword/file/validate",
					data  : params,
					dataType : "json",
					type : "post",
					async: false,
					success: function(data) {
						result =  data;
					}
				});
				return result;
			}
			function validateSensitiveWords(params) {
				return document.getElementById("webOffice").contentWindow.validateSensitiveWords(params);
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
				function changeOfficeFile(fileid){
					try{
						var _src = document.getElementById("webOffice").src;
						_src = _src.replace(/&fileid=-?\d*/g,"&fileid=" + fileid);
						_src = _src.replace(/\?fileid=-?\d*/g,"?fileid=" + fileid);
						document.getElementById("webOffice").src = getSecondPath(_src);
					}catch(e){}
				}
			<%}%>


			function refreshSign(sign){
				jQuery.ajax({
					url : "<%=CONTEXTPATH%>/api/doc/acc/getWpsSign",
					type : "get",
					data : {sign : sign},
					dataType : "json",
					success : function(data){
						if(data && data.status == 1){
							var pdata = {};
							for(var k in data.params){
								pdata[k] = data.params[k];
							}
            				//document.getElementById("officeShow").contentWindow.postMessage("logic.setToken",pdata);

            				var sendData = JSON.stringify({"action":"logic.setToken", data:pdata});
            				document.getElementById("officeShow").contentWindow.postMessage(sendData, '*');

            				setTimeout(function(){
            					refreshSign(data.sign);
            				},data.timeout);
						}else{
						//	refreshSign(sign);
						}
					},
					error : function(){
						//refreshSign(sign);
					}

				})
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
							var _expIndex = _src.indexOf("?");
							if(_expIndex > -1 && _src.indexOf("&appid=") > -1){ //wps预览
								var _expires = _src.substring(_expIndex+1);
								_expires = decodeURIComponent(_expires).replace(/&/g,"^_^");
								setTimeout(function(){
		            				refreshSign(_expires);
		            			},10*1000);
							}
						}else{
							if("<%=convertFile%>" == "pdf"){
                                if(<%=DocPlugUtil.isopenbjca()&&DocPlugUtil.isIE(request)%>){
                                    _src = "/docs/pdfofca/pdfShow.jsp?docid=<%=docid%>&imagefileId=<%=imagefileId%>&isEdit=<%=isEdit%>";
                                }else{
                                    _src = "/docs/pdfview/web/pdfViewer.jsp?canPrint=<%=canPrint%>&docisLock=<%=docisLock%>&canDownload=<%=canDownload%><%=moudleParams%>&pdfimagefileid="+data.data[0].id;
                                }
							}else if("<%=convertFile%>" == "html"){
								_src = "/weaver/weaver.file.FileDownload?fileid="+data.data[0].id+ "<%=moudleParams%>&nolog=1" ;
							}
						}
					}else{
						_src = "/wui/common/page/sysRemindDocpreview.jsp?labelid=" + data.message;
					}
				}else{
					if(data.status == "1"){
						if("<%=convertFile%>" == "pdf"){
                            if(<%=DocPlugUtil.isopenbjca()&&DocPlugUtil.isIE(request)%>){
                                _src = "/docs/pdfofca/pdfShow.jsp?docid=<%=docid%>&imagefileId=<%=imagefileId%>&isEdit=<%=isEdit%>";
                            }else{
                                _src = "/docs/pdfview/web/pdfViewer.jsp?canPrint=<%=canPrint%>&docisLock=<%=docisLock%>&canDownload=<%=canDownload%><%=moudleParams%>&pdfimagefileid="+data.convertId;
                            }
						}else if("<%=convertFile%>" == "html"){
							_src = "/weaver/weaver.file.FileDownload?fileid="+data.convertId+ "<%=moudleParams%>&nolog=1" ;
						}
					} if(data.status == "-1"){
						_src = "/docs/pdfview/web/sysRemind.jsp?labelid=998";
					}else if(data.status == "-2"){
						_src = "/wui/common/page/sysRemindDocpreview.jsp?labelid=" + data.msg;
					}
				}
				if(_src != ""){
					document.getElementById("officeShow").src = getSecondPath(_src);
				}
                if(<%=docisLock%>){
                    var ifmObj = document.getElementById("officeShow");
                    ifmObj.style.cssText += ';-moz-user-select: none;-webkit-user-select: none;-ms-user-select: none;-khtml-user-select: none;user-select: none;unselectable:on;';
                    ifmObj.onselectstart = function(){return false};
                    setIframeBodyLock();
                }
			}

            function setIframeBodyLock(){
                window.setTimeout(function(){
                    try{
                        var  osframe = document.getElementById("webOffice");
                        if(!osframe || osframe.length == 0){
                            osframe = document.getElementById("officeShow");
                        }
                        if(!osframe || osframe.length == 0){
                            osframe = document.getElementById("previewDoc").contentWindow.document.getElementById("officeShow");
                        }
                        if(osframe) {
                            var osframebody = osframe.contentWindow.document.body;
                            if(osframebody){
                                osframebody.style.cssText += ';-moz-user-select: none;-webkit-user-select: none;-ms-user-select: none;-khtml-user-select: none;user-select: none;unselectable:on;';
                                osframebody.onselectstart = function () {
                                    return false
                                };
                            } else {
                                setIframeBodyLock();
                            }
                        }
                    }catch(e){}
                },1000);
            }
      //window.__preTitle = $('.wea-new-top-req-title-text').html();
	  window.__preTitleObj = {
		  prevname: '<%=prevname%>',
		  title: "<%= imagefileId > 0 ? imagefilename: docsubject%>",
		  versionname: "<%=versionSpan %>"
	  };
      window.__preContent = '<%= docContent.replace("'","\\'") %>';
	  window.__onlyUrl4Content = <%=onlyUrl4Content%>;
	  window.__fix2015ForNotIE = <%= fix2015ForNotIE && isWindows %>;
		</script>
		<script type="text/javascript">
            try{
                document.body.style.overflow="hidden";
            }catch(e){}
            function hideLoading(){
                try{
                    var $body = document.getElementById("officeShow").contentWindow.document.body;
                    top.window.__html = $body;
                    replaceSpace($body);
                }catch(e){

                }
                try{
                    parent.finalDo("view");
                }catch(e){
                    window.setTimeout(function(){
                        try{
                            parent.finalDo("view");
                        }catch(e){}
                    },1000);
                }
            }

            function replaceSpace(obj){
				if(obj.tagName=="IMG"){
					var newSrc = getSecondPath(obj.getAttribute("src"));
					if(newSrc.indexOf("?")){
						newSrc = newSrc + "<%=moudleParams%>";
					}else{
						newSrc = newSrc + "?<%=moudleParams%>";
					}
					obj.setAttribute("src", newSrc);
				}
                if(obj && !obj.tagName){
                    obj.nodeValue = obj.nodeValue.replace(/&nbsp;/g,"");
                }
                if(obj && obj.childNodes && obj.childNodes.length > 0){
                    for(var i = 0;i < obj.childNodes.length;i++){
                        replaceSpace(obj.childNodes[i]);
                    }
                }
            }
		</script>
    <script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/polyfill/polyfill.min.js"></script>
		<!--[if lt IE 10]>
    <script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/shim/shim.min.js"></script>
    <![endif]-->
    <script type="text/javascript">
      jQuery.browser.msie && parseInt(jQuery.browser.version, 10) < 9 && (
        window.location.href = '<%=CONTEXTPATH%>/login/Login.jsp'
      );
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
		<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/index.min.js?v=<%=jsVersion%>"></script>
		<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/com/v1/ecCom.min.js?v=<%=jsVersion%>"></script>
		<!-- mobx -->
		<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/mobx-3.1.16/mobx.umd.js"></script>
    	<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/mobx-react-4.2.1/index.js"></script>
    	<script type="text/javascript" src="<%=CONTEXTPATH%>/cloudstore/resource/pc/react-router/ReactRouter.min.js"></script>
		<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/coms/index.mobx.js?v=<%=jsVersion%>"></script>
		<!-- 二维码之前在门户 -->
		<script type="text/javascript" src="<%=CONTEXTPATH%>/wui/theme/ecology9/js/lib.js?v=<%=jsVersion%>"></script>
		<!-- spa -->
		<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/document/static4Detail/index.js?v=<%=jsVersion%>"></script>
		<script type="text/javascript" src="<%=CONTEXTPATH%>/spa/document/static4Detail/index4single.js?v=<%=jsVersion%>"></script>
	</body>

</html>