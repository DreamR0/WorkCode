package weaver.file;

/**
 * Title:
 * Description:  ,2004-6-25:增加了是否记录下载次数到数据库
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author     ,Charoes Huang
 * @version 1.0,2004-6-25
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.net.drm.edi.client.DrmAgent;
import com.api.doc.detail.service.DocViewPermission;
import com.api.doc.detail.service.impl.DocWaterServiceImpl;
import com.api.doc.detail.util.PdfConvertUtil;
import com.api.doc.mobile.systemDoc.util.SystemDocUtil;
import com.customization.qc2440302.util.MobileUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.doc.util.*;
import com.engine.ecme.biz.EcmeRightManager;
import com.engine.odoc.util.DocUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import weaver.WorkPlan.WorkPlanService;
import weaver.alioss.AliOSSObjectManager;
import weaver.blog.BlogDao;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.cowork.CoworkDAO;
import weaver.crm.CrmShareBase;
import weaver.docs.category.SecCategoryComInfo;
import weaver.docs.docs.DocManager;
import weaver.docs.docs.AddWater.DocAddWaterForSecond;
import weaver.email.service.MailFilePreviewService;
import weaver.file.util.FileDeleteUtil;
import weaver.file.util.FileManager;
import weaver.file.util.FileWaterManager;
import weaver.file.util.FiledownloadUtil;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.meeting.MeetingUtil;
import weaver.mobile.plugin.ecology.service.AuthService;
import weaver.rdeploy.doc.PrivateSeccategoryManager;
import weaver.social.service.SocialIMService;
import weaver.splitepage.operate.SpopForDoc;
import weaver.system.SystemComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.voting.VotingManager;
import weaver.voting.groupchartvote.ImageCompressUtil;
import DBstep.iMsgServer2000;

import com.api.doc.detail.service.DocDetailService;
import com.api.doc.detail.service.DocViewPermission;
import com.api.doc.detail.util.DocCoopereateUtil;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.api.doc.detail.util.ImageConvertUtil;
import com.api.doc.upload.web.util.Json2MapUtil;
import com.api.doc.wps.service.impl.WebOfficeServiceImpl;
import com.api.workflow.service.RequestAuthenticationService;
import com.engine.edc.biz.JoinCubeBiz;
import com.weaver.formmodel.util.StringHelper;

import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipOutputStream;
import weaver.wps.CommonUtil;
import weaver.wps.doccenter.utils.Tools;

public class FileDownload extends HttpServlet {
    /**
     * 是否需要记录下载次数
     */

    private boolean isCountDownloads = false;
    private String agent = "";
    private static final int BUFFER_SIZE = 1 *1024 * 1024;
    private BaseBean baseBean = new BaseBean();
    private String systemWaterwriteLog = Util.null2s(baseBean.getPropValue("doc_custom_for_weaver","systemWaterwriteLog"),"0");
    private String writeLogForDownload = Util.null2s(baseBean.getPropValue("doc_writelog_config","doc_download_log"),"1");

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	this.doGet(req,res);
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {    
        String type = Util.null2String(req.getParameter("type"));
        Boolean _ec_ismobile = Boolean.valueOf(req.getParameter("_ec_ismobile"));
        String fromdocmobile = Util.null2String(req.getParameter("from_doc_mobile"));
        Boolean from_doc_mobile = fromdocmobile.equals("1") ? true : false;
        writeLogs("download start!");
		int nolog = Util.getIntValue(Util.null2String(req.getParameter("nolog")),0);
		String res_content_disposition = Util.null2String(req.getParameter("response-content-disposition"));		
		boolean isMobileDown=false;
		if(_ec_ismobile || from_doc_mobile || !"".equals(res_content_disposition)){
			isMobileDown=true;
		}
		if("showMould".equals(type) || "editMould".equals(type) || "printMould".equals(type)){
            downloadMould(req,res,type);
        }else if("weboffice".equals(type)){
        	downloadForWebOffice(req,res);
        }else if("doccenter".equals(type)){
            downloadForDoccenterCoop(req,res);
        }else if("document".equals(type)){
            User user = HrmUserVarify.getUser (req , res) ;
		    downloadForDocument(req,res,user);
        }else if("ofd".equals(type)){
            User user = HrmUserVarify.getUser (req , res) ;
            downloadOfd(req,res,user);
        }else if("odocExchange".equals(type)){
            downloadForExchange(req,res);
        }else if("exportHtmlView".equals(type)){
        	ExportHtmlViewMould ehvm = new ExportHtmlViewMould();
        	int fileid = Util.getIntValue(req.getParameter("fileid"));
        	Map<String,String> dataMap = ehvm.toExport(fileid);
        	String zipPath = dataMap.get("zipPath");
        	String filename = dataMap.get("zipName");
        	if(zipPath != null && !zipPath.isEmpty()){
        		ServletOutputStream out = null;
        		InputStream imagefile = null;
        		try {
                    if((agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") )&& !agent.contains("Edge")){
                        res.setHeader("content-disposition", "attachment; filename*=UTF-8''" +  URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
                    }else{
                        res.setHeader("content-disposition", "attachment; filename=\"" +
                                URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")")+"\"");
                    }
                } catch (Exception ecode) {
                    ecode.printStackTrace();
                }
                try {
                	imagefile = new FileInputStream(zipPath);
                    out = res.getOutputStream();
                    res.setContentType("application/x-download");
                    byte []data = new byte[2048];
                    int byteread = 0;
                    while ((byteread = imagefile.read(data)) != -1) {
                        out.write(data, 0, byteread);
                        out.flush();
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }finally {
                    if(imagefile!=null) imagefile.close();
                    if(out!=null) out.close();
                    
                    ImportHtmlViewMould ihvm =  new ImportHtmlViewMould();
                    ihvm.delTempFile(zipPath);
                    ihvm.delTempFile(zipPath.replace(".zip",""));
                }
        	}
        }else if("Email".equals(type)){
        	downloadForEmaill(req,res);
        }else if(!"netimag".equals(type)){
            String clientEcoding = "GBK";
            try
            {
                String acceptlanguage = Util.null2String(req.getHeader("Accept-Language"));
                if(!"".equals(acceptlanguage))
                    acceptlanguage = acceptlanguage.toLowerCase();
                if(acceptlanguage.indexOf("zh-tw")>-1||acceptlanguage.indexOf("zh-hk")>-1)
                {
                    clientEcoding = "BIG5";
                }
                else
                {
                    clientEcoding = "GBK";
                }
            }
            catch(Exception e)
            {
                writeLogs(e);
            }

            agent = Util.null2String(req.getHeader("user-agent"));
            String frompdfview = Util.null2String(req.getParameter("frompdfview"));
            String downloadBatch = Util.null2String(req.getParameter("downloadBatch"));
            //只下载附件，而不是下载文档的附件
            String onlydownloadfj = Util.null2String(req.getParameter("onlydownloadfj"));
            /*==============td26423 流程中批量下载     start=========================================================*/
            if(downloadBatch!=null &&"1".equals(downloadBatch)){//批量下载---传入的是文档的ids
                //req.setCharacterEncoding("ISO8859-1");
                String ipstring = Util.getIpAddr(req);
                String currentDateTime= new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String docids = Util.null2String(req.getParameter("fieldvalue"));//docids 以逗号隔开的
                if(!"".equals(docids)){					
					String[] docidsarray=docids.split(",");
					if(docidsarray!=null && docidsarray.length>0){
						String newdocids="";
						for(String newdocid:docidsarray){
							if(Util.getIntValue(newdocid)>0){
								String secondAuthFileDownUrl=getSecondAuthFileDownUrl(req,isMobileDown,Util.getIntValue(newdocid));
								if(!"".equals(secondAuthFileDownUrl)){
									 res.sendRedirect(secondAuthFileDownUrl);
									 return;
								}
								if("".equals(newdocids)){
									newdocids=newdocid;
								}else{
									newdocids+=","+newdocid;
								}
							}
						}
						docids=newdocids;
					}
				}
				String displayUsage = Util.null2String(req.getParameter("displayUsage"));
                String tempFlag1="";
                if(docids!=null&&!"".equals(docids)){
                    tempFlag1=docids.substring(docids.length()-1);
                }
                if(tempFlag1.equals(",")) docids=docids.substring(0,docids.length()-1);
                String delImgIds=Util.null2String(req.getParameter("delImgIds"));//imgeFileids 以逗号隔开的
                String tempFlag="";
                if(delImgIds!=null &&!"".equals(delImgIds)){
                    tempFlag=delImgIds.substring(delImgIds.length()-1);
                }
                if(tempFlag.equals(",")) delImgIds=delImgIds.substring(0,delImgIds.length()-1);

                String docSearchFlag=Util.null2String(req.getParameter("docSearchFlag"));//docSearchFlag ='1-标记为从查询文档中来批量下载多文档的多附件'

                String requestname ="";
                //String requestname = Util.null2String(req.getParameter("requestname"));//requestname 流程标题
                //requestname =new String(requestname.getBytes("UTF-8"));
                RecordSet rsimagefileid =new RecordSet();
                String requestid = Util.null2String(req.getParameter("requestid"));//requestname 流程标题
                if(Util.getIntValue(requestid)>0){
					String sqlrequestname="select requestname from workflow_requestbase where requestid='"+requestid+"'";
					rsimagefileid.executeSql(sqlrequestname);
					if(rsimagefileid.next()){
						requestname=Util.null2String(rsimagefileid.getString("requestname"));
					}
                }
                requestname = Util.StringReplace(requestname,"/","／");
                String download = Util.null2String(req.getParameter("download"));

                User loginuser = (User)req.getSession(true).getAttribute("weaver_user@bean") ;
                String userid=  Util.null2String(req.getParameter("f_weaver_belongto_userid"));
                if("".equals(userid) || userid == null){
                 userid=loginuser.getUID()+"";
                }
                //String loginid=loginuser.getLoginid();
                //String[] docidsList = Util.TokenizerString2(docids, ",");
                //List docImagefileidList= new ArrayList();
                String docImagefileids="";
                String sqlimagefileid="select a.id,a.docsubject,b.imagefileid,b.imagefilename from DocDetail a,DocImageFile b where a.id=b.docid and a.id in ("+docids+") and b.versionId = (select MAX(c.versionId) from DocImageFile c where c.id=b.id ) order by a.id asc";

                /*
                String sqlimagefileid="";
                if("1".equals(docSearchFlag)){//处理从查询文档中 来的文档附件批量下载时只下载最新版本问题
                    sqlimagefileid="select a.id,a.docsubject,b.imagefileid,b.imagefilename from DocDetail a,DocImageFile b where a.id=b.docid and a.id in ("+docids+") and b.versionId = (select MAX(c.versionId) from DocImageFile c where c.id=b.id ) order by a.id asc";
                }else{
                    sqlimagefileid="select a.id,a.docsubject,b.imagefileid,b.imagefilename from DocDetail a,DocImageFile b where a.id=b.docid and a.id in ("+docids+") order by a.id asc";
                }
                */
                String docsubject="";
                rsimagefileid.executeSql(sqlimagefileid);
                int counts=rsimagefileid.getCounts();
                String urlType = Util.null2String(req.getParameter("urlType"));
                if(counts<=0){
                   if("1".equals(docSearchFlag)){
                       if(urlType.equals("10")){
                           res.sendRedirect(GCONST.getContextPath()+"/docs/search/DocCommonContent.jsp?urlType=10&displayUsage="+displayUsage);
                       }else{
                        res.sendRedirect(GCONST.getContextPath()+"/docs/search/DocCommonContent.jsp?urlType=6&fromUrlType=1&displayUsage="+displayUsage);
                       }
                    return;
                   }else{
                    res.sendRedirect(GCONST.getContextPath()+"/login/BatchDownloadsEror.jsp");
                    return;
                   }
                }
                int num=0;
                while(rsimagefileid.next()){
                    num++;
                    if(num==counts){
                        docImagefileids += Util.null2String(rsimagefileid.getString("imagefileid"));
                    }else{
                        docImagefileids += Util.null2String(rsimagefileid.getString("imagefileid"))+",";
                    }
                    docsubject=Util.null2String(rsimagefileid.getString("docsubject"));
                }
                List filenameList = new ArrayList();
                List filerealpathList = new ArrayList();
                List filerealpathTempList = new ArrayList();
                List docidList = new ArrayList();
                List imagefileidList = new ArrayList();
                //List filerealpathTempParentList = new ArrayList();
                File fileTemp=null;
                String sqlfilerealpath="";
                ImageFileManager imageFileManager=new ImageFileManager();
                if(delImgIds!=null &&!"".equals(delImgIds)){
                    //sqlfilerealpath="select imagefilename,filerealpath,iszip,isencrypt,imagefiletype , imagefileid, imagefile from ImageFile where imagefileid in ("+delImgIds+")";
                    sqlfilerealpath = "select t2.docid,t1.isaesencrypt,t1.comefrom,t1.aescode,t1.imagefilename,t1.filerealpath,t1.iszip,t1.isencrypt,t1.imagefiletype , t1.imagefileid, t1.imagefile,t2.imagefilename as realname from ImageFile t1 left join DocImageFile t2 on t1.imagefileid = t2.imagefileid where t1.imagefileid in ("+delImgIds+") "       +(docids.equals("")?"":" and docid in("+docids+") ");
                }else{
                    //sqlfilerealpath="select imagefilename,filerealpath,iszip,isencrypt,imagefiletype , imagefileid, imagefile from ImageFile where imagefileid in ("+docImagefileids+")";
                    sqlfilerealpath = "select t2.docid,t1.isaesencrypt,t1.comefrom,t1.aescode,t1.imagefilename,t1.filerealpath,t1.iszip,t1.isencrypt,t1.imagefiletype , t1.imagefileid, t1.imagefile,t2.imagefilename as realname from ImageFile t1 left join DocImageFile t2 on t1.imagefileid = t2.imagefileid where t1.imagefileid in ("+docImagefileids+") " +(docids.equals("")?"":" and docid in("+docids+") ");
                }

                rsimagefileid.executeSql(sqlfilerealpath);
                String fromrequest = req.getParameter("fromrequest");

                Map<String,String> filenameMap = new HashMap<String,String>();
                String imagefileid = "";
                String pdfimagefileid = ""; //office文档转换过来的PDFid
                Map<String,Boolean> rightDoc = new HashMap<String,Boolean>();
                while(rsimagefileid.next()){
                    try{
                         imagefileid = Util.null2String(rsimagefileid.getString("imagefileid"));
                        String filename = Util.null2String(rsimagefileid.getString("realname"));
                        if(filename.equals("")){
                            filename = Util.null2String(rsimagefileid.getString("imagefilename"));
                        }
                        String filerealpath = Util.null2String(rsimagefileid.getString("filerealpath"));
                        String iszip = Util.null2String(rsimagefileid.getString("iszip"));
                        String isencrypt = Util.null2String(rsimagefileid.getString("isencrypt"));
                        String isaesencrypt = Util.null2String(rsimagefileid.getString("isaesencrypt"));
                        String aescode = Util.null2String(rsimagefileid.getString("aescode"));
						String _comefrom= Util.null2String(rsimagefileid.getString("comefrom"));
                        String _docid = Util.null2String(rsimagefileid.getString("docid"));
                        if(!_docid.isEmpty() && rightDoc.get(_docid) == null){
                            boolean hasRight = getWhetherHasRight(imagefileid,req,res,Util.getIntValue(requestid),false,null);
                            if(!hasRight && !HrmUserVarify.checkUserRight("DocFileBatchDownLoad:ALL", loginuser))
                                continue;
                            rightDoc.put(_docid,true);
                        }

                        int docidans1 = -1;
                        /**流程下载附件，获取最新版本附件下载 start */
                        if("1".equals(fromrequest)){
                            RecordSet rs = new RecordSet();
                            rs.executeSql("select doceditionid,id from DocDetail where id=(select max(docid) from DocImageFile where imagefileid=" + imagefileid + ")");
                            if(rs.next()){
                               int doceditionid = rs.getInt("doceditionid");
                               int docid = rs.getInt("id");
                               docidans1 = docid;
                               if(doceditionid > 0){  //有多版本文档，取最新版本文档
                                   rs.executeSql("select id from DocDetail where doceditionid=" + doceditionid + " order by docedition desc");
                                   if(rs.next()){
                                       docid = rs.getInt("id");
                                   }
                               }

                               //在新版本文档下取最新附件
                               rs.executeSql("select f.imagefileid,f.imagefilename,f.filerealpath,d.imagefilename realname from DocImageFile d,imagefile f where d.imagefileid=f.imagefileid and d.docid=" + docid + " order by d.versionId desc");
                               if(rs.next()){
                                   imagefileid = rs.getString("imagefileid");
                                   filename = Util.null2String(rsimagefileid.getString("realname"));
                                   if(filename.equals("")){
                                       filename = Util.null2String(rsimagefileid.getString("imagefilename"));
                                   }
                                   filerealpath = Util.null2String(rsimagefileid.getString("filerealpath"));
                               }
                            }
                        }
                        /**流程下载附件，获取最新附件 end */

                        if(!"DocLogAction_DocLogFile".equals(_comefrom) && FiledownloadUtil.isdownloadpdf(filename) && download.equals("1")){
                            pdfimagefileid = FiledownloadUtil.getpdfidByOfficeid(imagefileid);
                            if(pdfimagefileid.isEmpty()){
                                pdfimagefileid = imagefileid;
                            }
                            String pdffileinfosql = "select imagefilename,filerealpath from imagefile  where imagefileid = ? ";
                            RecordSet pdffileinfors = new RecordSet();
                            pdffileinfors.executeQuery(pdffileinfosql,pdfimagefileid);
                            if(pdffileinfors.next()){
                                filename = Util.null2String(pdffileinfors.getString("imagefilename"));
                                filerealpath = Util.null2String(pdffileinfors.getString("filerealpath"));
                            }
                        }else{
                            pdfimagefileid = imagefileid;
                        }
						if(!"".equals(filename)){
							filename = filename.replace("&amp;","&");
						}
                        filenameList.add(filename);
                        filerealpathList.add(filerealpath);
                        docidList.add(docidans1);
                        imagefileidList.add(imagefileid);
                            //是否需要记录日志
                           if(addDownLoadLogByimageId(Util.getIntValue(imagefileid))){
                            // 记录下载日志 begin

                               String userType = loginuser.getLogintype();
                               if ("1".equals(userType)) { // 如果是内部用户　名称就是　lastName
                                                           // 外部则入在　firstName里面
                                   downloadLog(loginuser.getUID(), loginuser.getLastname(), Util.getIntValue(imagefileid),filename, ipstring);
                               } else {
                                   downloadLog(loginuser.getUID(), loginuser.getFirstname(), Util.getIntValue(imagefileid),filename, ipstring);
                               }

                            // 记录下载日志 end
                           }
                        String extName = "";
                        int byteread;
                        byte data[] = new byte[1024];
                        if(filename.indexOf(".") > -1){
                            int bx = filename.lastIndexOf(".");
                            if(bx>=0){
                                extName = filename.substring(bx+1, filename.length());
                            }
                        }

                        InputStream imagefile = null;
                        if(!"".equals(pdfimagefileid) && !imagefileid.equals(pdfimagefileid)){
                            imageFileManager.getImageFileInfoById(Util.getIntValue(pdfimagefileid));
                        }else{
                            imageFileManager.getImageFileInfoById(Util.getIntValue(imagefileid));
                        }


                        imagefile=imageFileManager.getInputStream();

                        if(download.equals("1") && (isOfficeToDocument(extName))&&isMsgObjToDocument()) {
                            //正文的处理
                            ByteArrayOutputStream bout = null;
                            try {
                                bout = new ByteArrayOutputStream() ;
                                while((byteread = imagefile.read(data)) != -1) {
                                    bout.write(data, 0, byteread) ;
                                    bout.flush() ;
                                }
                                byte[] fileBody = bout.toByteArray();
                                iMsgServer2000 MsgObj = new iMsgServer2000();
                                MsgObj.MsgFileBody(fileBody);           //将文件信息打包
                                fileBody = MsgObj.ToDocument(MsgObj.MsgFileBody());    //通过iMsgServer200 将pgf文件流转化为普通Office文件流
                                imagefile = new ByteArrayInputStream(fileBody);
                                bout.close();
                            }
                            catch(Exception e) {
                                writeLogs(e);
                                if(bout!=null) bout.close();
                            }
                        }

                        FileOutputStream out = null;
                        try {

                            SystemComInfo syscominfo = new SystemComInfo();
                            String fileFoder= syscominfo.getFilesystem();
                            String fileFoderCompare= syscominfo.getFilesystem();
                            if("".equals(fileFoder)){
                                fileFoder = GCONST.getRootPath();
                                fileFoder = fileFoder + "filesystem" + File.separatorChar+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar;
                            }else{
                                if(fileFoder.endsWith(File.separator) ){
                                    fileFoder += "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar;
                                }else{
                                    fileFoder += File.separator+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar;
                                }
                            }

                            String _fielname = UUID.randomUUID().toString();
                            if(filename.indexOf(".") > -1){
                                int bx = filename.lastIndexOf(".");
                                if(bx>=0){
                                    _fielname += filename.substring(bx, filename.length());
                                }
                            }

                            if("".equals(fileFoderCompare)){
                                fileFoderCompare = GCONST.getRootPath();
                                fileFoderCompare = fileFoderCompare + "filesystem" + File.separatorChar+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar+_fielname;
                            }else{
                                if(fileFoderCompare.endsWith(File.separator)){
                                    fileFoderCompare += "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar+_fielname;
                                }else{
                                    fileFoderCompare += File.separator+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar+_fielname;
                                }
                            }
                            //附件文件重名加数字后缀区别:因为不加以区别的话,压缩到压缩包时只取一个文件(压缩包中不能有同名文件) 加唯一标识'_imagefileid'
                            for (String m : filenameMap.keySet()) {
                                String keyValue = filenameMap.get(m);
                                if(keyValue.equals(filename)){
                                    String tepName= filename.contains(".")? filename.substring(0, filename.indexOf(".")) : "";
                                    if(tepName!=null&&!"".equals(tepName)){
                                        String extNameTemp = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                                        filename = tepName + "_"+imagefileid+"."+extNameTemp;
										
                                    }
                                   
                                }
                            }
                            filenameMap.put(_fielname, filename);

                            //String fileFoder=GCONST.getRootPath()+ "downloadBatchTemp"+File.separatorChar+userid+File.separatorChar;//获取系统的运行目录 如：d:\ecology\
                            //String fileFoderCompare=GCONST.getRootPath()+ "downloadBatchTemp"+File.separatorChar+userid+File.separatorChar+filename;
                            String newFileName=_fielname;
                            for(int j=0;j<filerealpathTempList.size();j++){
                                if(fileFoderCompare.equals(filerealpathTempList.get(j))){
                                    int lastindex=filename.lastIndexOf(".");
                                    if(lastindex>=0){
                                        if(!"".equals(pdfimagefileid) && !imagefileid.equals(pdfimagefileid)){
                                            newFileName=filename.substring(0, lastindex)+"_"+pdfimagefileid+filename.substring(lastindex);
                                        }else{
                                            newFileName=filename.substring(0, lastindex)+"_"+imagefileid+filename.substring(lastindex);
                                        }
                                    }else{
                                        if(!"".equals(pdfimagefileid) && !imagefileid.equals(pdfimagefileid)){
                                            newFileName=filename+"_"+pdfimagefileid;
                                        }else{
                                            newFileName=filename+"_"+imagefileid;
                                        }

                                    }
                                }
                            }
                            fileTemp=this.fileCreate(fileFoder, newFileName);//在系统的根目录下创建了一个文件夹(downloadBatchTemp)及附件文件
                            out = new FileOutputStream(fileTemp);

                            imagefile = FileManager.download(req,imagefileid,filerealpath,aescode,iszip,isaesencrypt,imagefile);
 //=======================
                            while ((byteread = imagefile.read(data)) != -1) {
                               out.write(data, 0, byteread);
                               out.flush();
                            }
                            String filerealpathTemp=fileTemp.getPath();
                            //String filerealpathTempParent=fileTemp.getParent();
                            //filerealpathTempParentList.add(filerealpathTempParent);
                            //filerealpathTempList.add(filerealpathTemp);
                            filerealpathTempList.add(fileFoder+newFileName);
                        }catch(Exception e) {
                            writeLogs(e);
                            //do nothing
                        }finally {
                            if(imagefile!=null) imagefile.close();
                            if(out!=null) out.flush();
                            if(out!=null) out.close();
                        }
                    }catch(Exception e){
                        BaseBean basebean = new BaseBean();
                        basebean.writeLog(e);
                        continue;
                    }
                }

                if(filerealpathList.size() == 0){
                    res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?v1");
                    return;
                }


                //存储下载的文件路径   把临时存放的附件 打压缩包 提供给用户下载
                File[] files=new File[filerealpathList.size()];
                for(int i=0;i<filerealpathTempList.size();i++){
                      String path=(String)filerealpathTempList.get(i);
                      files[i]=new File(path);
                }
                byte[] bt=new byte[8192];

                SystemComInfo syscominfo2 = new SystemComInfo();
                String fileFoder2= syscominfo2.getFilesystem();
                if("".equals(fileFoder2)){
                    fileFoder2 = GCONST.getRootPath();
                    fileFoder2 = fileFoder2 + "filesystem" + File.separatorChar+ "downloadBatch"+File.separatorChar;
                }else{
                    if(fileFoder2.endsWith(File.separator)){
                        fileFoder2 += "downloadBatch"+File.separatorChar;

                    }else{
                        fileFoder2 += File.separator+ "downloadBatch"+File.separatorChar;
                    }
                }

                //String fileFoder=GCONST.getRootPath()+ "downloadBatch"+File.separatorChar;//获取系统的运行目录 如：d:\ecology\
                //String strs=requestname+"_"+userid+".zip";
                String strs="";
                String tmptfilename=userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";

                if(requestname!=null&&!"".equals(requestname)){//附件从流程中来 文件名称：流程名称_用户userid.zip
                    strs=requestname+"_"+userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";
                }else if("1".equals(docSearchFlag)){//从查询文档中来批量下载多文档多附件 文件名称：用户userid.zip
                    strs=userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";
                }else{//附件从文档中来 文件名称：文档名称_用户userid.zip
                    //strs=docsubject+"_"+userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";
					strs=userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";
                }
                File file=this.fileCreate(fileFoder2, tmptfilename);//在系统的根目录下创建了一个文件夹(downloadBatch)及zip文件
                String strzipName=file.getPath();
                //String fileTempParent=fileTemp.getParent();
                String fileTempParent=null;
                if(fileTemp!=null){
                    fileTempParent=fileTemp.getParent();
                }
                ZipOutputStream zout =null;
                InputStream is=null;
                try {
                    DocAddWaterForSecond docAddWaterForSecond = new DocAddWaterForSecond();
                    zout = new ZipOutputStream(new FileOutputStream(strzipName), "UTF-8");
                    //循环下载每个文件并读取内容
                    //流程中附件批量下载
                    for(int j=0;j<files.length;j++){
                        try{
                             is=new FileInputStream(files[j]);
                             //System.out.println("第["+j+"]个文件的名称为："+files[j].getName());

                            String filename = files[j].getName();
                            String fileid = Util.null2s((String) imagefileidList.get(j),"");

                            String extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                            if( !frompdfview.equals("1") && download.equals("1")){
                                Map<String,Object> secWmSetMap = WaterMarkUtil.getCategoryWmSet(imagefileid);
                                if("1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYDOWNLOAD)) && "1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYWMISOPEN)) && "0".equals(secWmSetMap.get(WaterMarkUtil.WATERCONTENTISNULL))){
//                                    is = WaterMarkUtil.takefileWater(is,loginuser,filename,Util.getIntValue(fileid),extName,WaterMarkUtil.MOULDDOC);
                                    is = FileWaterManager.takewater(req,fileid,filename,extName,is, WaterMarkUtil.MOULDDOC,-1);
                                }
                            }

                             //ZipEntry ze=new ZipEntry(filenameMap.get(files[j].getAbsolutePath()));//设置文件编码格式
                             ZipEntry ze = new ZipEntry(filenameMap.get(files[j].getName()));//设置文件编码格式
                             zout.putNextEntry(ze);
                             int len;
                             //读取下载文件内容
                             while((len=is.read(bt))>0){
                              zout.write(bt, 0, len);
                             }
                             zout.closeEntry();
                             is.close();
                        }catch(Exception e){
                            BaseBean basebean = new BaseBean();
                            basebean.writeLog(e);
                            continue;
                        }
                    }
                    zout.close();
                    this.toUpload(res, strs, tmptfilename);//调用下载方法
                    //String fileTempParent=fileTemp.getParent();
                    //this.deleteFile(fileTempParent);//下载完成后调用删除文件的方法删除downloadBatchTemp文件夹下的子文件夹
                    /*
                    for(int i=0;i<filerealpathTempList.size();i++){
                        String delPathTemp=(String)filerealpathTempList.get(i);
                        this.deleteFile(delPathTemp);//下载完成后调用删除方法删除downloadBatchTemp临时文件夹中存放的附件文件
                    }*/
                    //this.deleteFile(strzipName);//下载完成后调用删除文件的方法删除downloadBatch文件夹中的.zip文件
             } catch (FileNotFoundException e) {
                    e.printStackTrace();
             }catch (IOException e) {
                    e.printStackTrace();
             }finally {
                 if(is!=null) {
                    is.close();
                 }
                 if(zout!=null){
                    zout.close();
                 }
                 this.deleteFile(strzipName);//下载完成后调用删除文件的方法删除downloadBatch文件夹中的.zip文件
                 //this.deleteFile(fileTempParent);//下载完成后调用删除文件的方法删除downloadBatchTemp文件夹下的子文件夹
                 if(fileTempParent!=null){
                     //new weaver.filter.XssUtil().writeLog("=====L402------"+fileTempParent);
                     this.deleteFile(fileTempParent);//下载完成后调用删除文件的方法删除downloadBatchTemp文件夹下的子文件夹
                 }
             }



           /*==============td26423 流程中批量下载     end============================================================*/

            }else if(onlydownloadfj!=null &&"1".equals(onlydownloadfj)){//批量下载--来自于证照，只批量下载附件，传入的是附件的ids
                 /*============== 证照批量下载附件     start============================================================*/
                String currentDateTime= new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                String fieldids = Util.null2String(DocDownloadCheckUtil.getDownloadfileid(req));//fieldids 以逗号隔开的
                int labelid = Util.getIntValue(Util.null2String(req.getParameter("labelid")),0);
                String download = Util.null2String(req.getParameter("download"));
                User loginuser = (User)req.getSession(true).getAttribute("weaver_user@bean") ;
                String userid=loginuser.getUID()+"";
                File fileTemp=null;
                if(!"".equals(fieldids)&&fieldids.length()>0){
                    fieldids = fieldids.substring(0,fieldids.length()-1);
                }
                toWriteLog("onlydownloadfj----1----批量下载--fieldids："+fieldids+"--labelid:"+labelid+"--userid:"+userid+"--download:"+download);
                if(!"".equals(fieldids)){
					String[] fieldidsarray=fieldids.split(",");
					String newfieldids="";
					for(String newfileid:fieldidsarray){
						if(Util.getIntValue(newfileid)>0){
							if("".equals(newfieldids)){
								newfieldids=newfileid;
							}else{
								newfieldids+=","+newfileid;
							}
						}
					}
					fieldids=newfieldids;
					String[] newfieldidsarray=fieldids.split(",");
					RecordSet rsprop = new RecordSet();							 
					String secondAuthsql = " select distinct docid from docImageFile where imagefileid in ("+fieldids+")";
					rsprop.executeSql(secondAuthsql);
					while(rsprop.next()){
						int docid=Util.getIntValue(rsprop.getString("docid"));
						if(docid>0){
							String secondAuthFileDownUrl=getSecondAuthFileDownUrl(req,isMobileDown,docid);
							if(!"".equals(secondAuthFileDownUrl)){
								 res.sendRedirect(secondAuthFileDownUrl);
								 return;
							}
						}
					}
					int maxDownFileCount = Util.getIntValue(rsprop.getPropValue("BatchDownFileControl","maxDownFileCount"),10);
					int maxDownFileTotalSize = Util.getIntValue(rsprop.getPropValue("BatchDownFileControl","maxDownFileTotalSize"),200);
					if(newfieldidsarray.length>maxDownFileCount){
						res.sendRedirect(GCONST.getContextPath()+"/wui/common/page/sysRemindDocpreview.jsp?labelid="+SystemEnv.getHtmlLabelName(534454,loginuser.getLanguage())+maxDownFileCount);
						return;
					}else{
						String dbType = rsprop.getDBType();
						String sql = " select sum(fileSize) totalfilesize from ImageFile where imagefileid in ("+fieldids+")";
						if("sqlserver".equals(dbType)){
							sql = " select SUM(cast(filesize as numeric(30,0))) totalfilesize from ImageFile where imagefileid in ("+fieldids+")";
						}					
						rsprop.executeSql(sql);
						if(rsprop.next()){
							double totalfilesize = Util.getDoubleValue(rsprop.getString("totalfilesize"),0);
							if(totalfilesize>maxDownFileTotalSize*1024*1024){
								res.sendRedirect(GCONST.getContextPath()+"/wui/common/page/sysRemindDocpreview.jsp?labelid="+SystemEnv.getHtmlLabelName(534453,loginuser.getLanguage())+maxDownFileTotalSize);
								return;
							}
						}
					}
				}else{
					return;
				}
                List filenameList = new ArrayList();
                List filerealpathList = new ArrayList();
                List filerealpathTempList = new ArrayList();
                List<Integer> fileids = new ArrayList<Integer>();
                String sqlfilerealpath="";
                ImageFileManager imageFileManager=new ImageFileManager();
                sqlfilerealpath = "select * from imagefile where imagefileid in("+fieldids+") ";
                RecordSet rsimagefileid =new RecordSet();
                rsimagefileid.executeSql(sqlfilerealpath);
                String imagefileid = "";
                String pdfimagefileid = "";
                Map<String,String> filenameMap = new HashMap<String,String>();
                while(rsimagefileid.next()){
                    String downloadpdfimagefileid = "";
                    try{
                         imagefileid = Util.null2String(rsimagefileid.getString("imagefileid"));
                        String filename = Util.null2String(rsimagefileid.getString("realname"));
                        if(filename.equals("")){
                            filename = Util.null2String(rsimagefileid.getString("imagefilename"));
                        }
                        String filerealpath = Util.null2String(rsimagefileid.getString("filerealpath"));
                        String iszip = Util.null2String(rsimagefileid.getString("iszip"));
                        String isencrypt = Util.null2String(rsimagefileid.getString("isencrypt"));
                        String isaesencrypt = Util.null2String(rsimagefileid.getString("isaesencrypt"));
                        String aescode = Util.null2String(rsimagefileid.getString("aescode"));
						String _comefrom = Util.null2String(rsimagefileid.getString("comefrom"));
                        boolean hasRight = getWhetherHasRight(imagefileid,req,res,Util.getIntValue(req.getParameter("requestid")),false,null);
                        toWriteLog("onlydownloadfj----2----批量下载--imagefileid："+imagefileid+"--filename:"
                                +filename+"--userid:"+userid+"--filerealpath:"+filerealpath+"--hasRight:"+hasRight);
                        if(!hasRight)
                            continue;

                        if(!"DocLogAction_DocLogFile".equals(_comefrom) && FiledownloadUtil.isdownloadpdf(filename) && download.equals("1")){
                            pdfimagefileid = FiledownloadUtil.getpdfidByOfficeid(imagefileid);
                            if(pdfimagefileid.isEmpty()){
                                pdfimagefileid = imagefileid;
                            }
                            String pdffileinfosql = "select imagefilename,filerealpath from imagefile  where imagefileid = ? ";
                            RecordSet pdffileinfors = new RecordSet();
                            pdffileinfors.executeQuery(pdffileinfosql,pdfimagefileid);
                            if(pdffileinfors.next()){
                                filename = Util.null2String(pdffileinfors.getString("imagefilename"));
                                filerealpath = Util.null2String(pdffileinfors.getString("filerealpath"));
                            }
                        }else{
                            pdfimagefileid = imagefileid;
                        }
                        String extName = "";
                        if(filename.indexOf(".") > -1) {
                            int bx = filename.lastIndexOf(".");
                            if (bx >= 0) {
                                extName = filename.substring(bx + 1, filename.length());
                            }
                        }
                        if(ImageConvertUtil.canConvertPdfForDownload(extName,imagefileid)&& !frompdfview.equals("1") && download.equals("1")) {
                            RecordSet rsrecord = new RecordSet();
                            PdfConvertUtil pdfConvertUtil = new PdfConvertUtil();
                            int pdfid = pdfConvertUtil.convertPdf(Util.getIntValue(imagefileid));
                            if (pdfid > 0) {
                                downloadpdfimagefileid = String.valueOf(pdfid);
                                String pdffileinfosql = "select imagefilename,filerealpath from imagefile  where imagefileid = ? ";
                                RecordSet pdffileinfors = new RecordSet();
                                pdffileinfors.executeQuery(pdffileinfosql, downloadpdfimagefileid);
                                if (pdffileinfors.next()) {
                                    filename = filename.substring(0, filename.lastIndexOf(".")) + ".pdf";
                                    filerealpath = Util.null2String(pdffileinfors.getString("filerealpath"));
                                }
                            }
                        }
                        if(downloadpdfimagefileid.isEmpty()){
                            downloadpdfimagefileid = imagefileid+"";
                        }
						if(!"".equals(filename)){
							filename = filename.replace("&amp;","&");
						}

                        filenameList.add(filename);
                        filerealpathList.add(filerealpath);
                        fileids.add(Util.getIntValue(downloadpdfimagefileid));
                        int byteread;
                        byte data[] = new byte[1024];
                        if(filename.indexOf(".") > -1){
                            int bx = filename.lastIndexOf(".");
                            if(bx>=0){
                                extName = filename.substring(bx+1, filename.length());
                            }
                        }

                        InputStream imagefile = null;
                        if(!"".equals(pdfimagefileid) && !imagefileid.equals(pdfimagefileid)){
                            imageFileManager.getImageFileInfoById(Util.getIntValue(pdfimagefileid));
                        }else{
                            imageFileManager.getImageFileInfoById(Util.getIntValue(imagefileid));
                        }


                        imagefile=imageFileManager.getInputStream();

                        if(download.equals("1") && (isOfficeToDocument(extName))&&isMsgObjToDocument()) {
                            //正文的处理
                            ByteArrayOutputStream bout = null;
                            try {
                                bout = new ByteArrayOutputStream() ;
                                while((byteread = imagefile.read(data)) != -1) {
                                    bout.write(data, 0, byteread) ;
                                    bout.flush() ;
                                }
                                byte[] fileBody = bout.toByteArray();
                                iMsgServer2000 MsgObj = new iMsgServer2000();
                                MsgObj.MsgFileBody(fileBody);           //将文件信息打包
                                fileBody = MsgObj.ToDocument(MsgObj.MsgFileBody());    //通过iMsgServer200 将pgf文件流转化为普通Office文件流
                                imagefile = new ByteArrayInputStream(fileBody);
                                bout.close();
                            }
                            catch(Exception e) {
                                writeLogs(e);
                                if(bout!=null) bout.close();
                            }
                        }

                        FileOutputStream out = null;
                        try {

                            SystemComInfo syscominfo = new SystemComInfo();
                            String fileFoder= syscominfo.getFilesystem();
                            String fileFoderCompare= syscominfo.getFilesystem();
                            if("".equals(fileFoder)){
                                fileFoder = GCONST.getRootPath();
                                fileFoder = fileFoder + "filesystem" + File.separatorChar+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar;
                            }else{
                                if(fileFoder.endsWith(File.separator)){
                                    fileFoder += "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar;
                                }else{
                                    fileFoder += File.separator+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar;
                                }
                            }
							
							
							String _fielname = UUID.randomUUID().toString();
                            if(filename.indexOf(".") > -1){
                                int bx = filename.lastIndexOf(".");
                                if(bx>=0){
                                    _fielname += filename.substring(bx, filename.length());
                                }
                            }
							
							

                            if("".equals(fileFoderCompare)){
                                fileFoderCompare = GCONST.getRootPath();
                                fileFoderCompare = fileFoderCompare + "filesystem" + File.separatorChar+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar+_fielname;
                            }else{
                                if(fileFoderCompare.endsWith(File.separator)){
                                    fileFoderCompare += "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar+_fielname;
                                }else{
                                    fileFoderCompare += File.separator+ "downloadBatchTemp"+File.separatorChar+userid+currentDateTime+File.separatorChar+_fielname;
                                }
                            }
                            toWriteLog("onlydownloadfj----3----批量下载--imagefileid："+imagefileid+"--filename:"
                                    +filename+"--userid:"+userid+"--fileFoder:"+fileFoder+"--fileFoderCompare:"+fileFoderCompare);
                            //String fileFoder=GCONST.getRootPath()+ "downloadBatchTemp"+File.separatorChar+userid+File.separatorChar;//获取系统的运行目录 如：d:\ecology\
                            //附件文件重名加数字后缀区别:因为不加以区别的话,压缩到压缩包时只取一个文件(压缩包中不能有同名文件) 加唯一标识'_imagefileid'
                            //String fileFoderCompare=GCONST.getRootPath()+ "downloadBatchTemp"+File.separatorChar+userid+File.separatorChar+filename;
                            
                            //附件文件重名加数字后缀区别:因为不加以区别的话,压缩到压缩包时只取一个文件(压缩包中不能有同名文件) 加唯一标识'_imagefileid'
                            for (String m : filenameMap.keySet()) {
                                String keyValue = filenameMap.get(m);
                                if(keyValue.equals(filename)){
                                    String tepName= filename.contains(".")? filename.substring(0, filename.indexOf(".")) : "";
                                    if(tepName!=null&&!"".equals(tepName)){
                                        String extNameTemp = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                                        filename = tepName + "_"+imagefileid+"."+extNameTemp;
										
                                    }
                                   
                                }
                            }
							filenameMap.put(_fielname, filename);
							String newFileName=_fielname;
                            toWriteLog("onlydownloadfj----4----批量下载--imagefileid："+imagefileid+"--filename:"
                                    +filename+"--userid:"+userid+"--fileFoder:"+fileFoder+"--fileFoderCompare:"+fileFoderCompare
                            +"--newFileName:"+newFileName+"--filerealpathTempList:"+filerealpathTempList+"--out:"+filerealpathTempList
                            +"--imagefile:"+imagefile);
                            fileTemp=this.fileCreate(fileFoder, newFileName);//在系统的根目录下创建了一个文件夹(downloadBatchTemp)及附件文件
                            out = new FileOutputStream(fileTemp);

                            imagefile = FileManager.download(req,imagefileid,filerealpath,aescode,iszip,isaesencrypt,imagefile);
                            Map<String,Object> secWmSetMap = WaterMarkUtilNew.getCategoryWmSet(imagefileid);
                            
                            if("1".equals(secWmSetMap.get(WaterMarkUtilNew.SECCATEGORYDOWNLOAD))&& "1".equals(secWmSetMap.get(WaterMarkUtilNew.SECCATEGORYWMISOPEN)) && "0".equals(secWmSetMap.get(WaterMarkUtilNew.WATERCONTENTISNULL))){
                                imagefile = FileWaterManager.takewater(req,downloadpdfimagefileid,filename,extName,imagefile, WaterMarkUtil.MOULDDOC,Util.getIntValue(imagefileid));
                            }
                            while ((byteread = imagefile.read(data)) != -1) {
                               out.write(data, 0, byteread);
                               out.flush();
                            }
                            String filerealpathTemp=fileTemp.getPath();
                            //String filerealpathTempParent=fileTemp.getParent();
                            //filerealpathTempParentList.add(filerealpathTempParent);
                            //filerealpathTempList.add(filerealpathTemp);
                            filerealpathTempList.add(fileFoder+newFileName);
                        }catch(Exception e) {
                            //do nothing
                            e.printStackTrace();
                        }finally {
                            if(imagefile!=null) imagefile.close();
                            if(out!=null) out.flush();
                            if(out!=null) out.close();
                        }
                    }catch(Exception e){
                        BaseBean basebean = new BaseBean();
                        basebean.writeLog(e);
                        continue;
                    }
                }

                if(filerealpathList.size() == 0){
                    res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?v2");
                    return;
                }
                toWriteLog("onlydownloadfj----5----批量下载--filerealpathList："+filerealpathList+"--filerealpathTempList:"
                        +filerealpathTempList+"--userid:"+userid);
                //存储下载的文件路径   把临时存放的附件 打压缩包 提供给用户下载
                File[] files=new File[filerealpathList.size()];
                for(int i=0;i<filerealpathTempList.size();i++){
                      String path=(String)filerealpathTempList.get(i);
                      files[i]=new File(path);
                }
                byte[] bt=new byte[8192];

                SystemComInfo syscominfo2 = new SystemComInfo();
                String fileFoder2= syscominfo2.getFilesystem();
                if("".equals(fileFoder2)){
                    fileFoder2 = GCONST.getRootPath();
                    fileFoder2 = fileFoder2 + "filesystem" + File.separatorChar+ "downloadBatch"+File.separatorChar;
                }else{
                    if(fileFoder2.endsWith(File.separator)){
                        fileFoder2 += "downloadBatch"+File.separatorChar;
                    }else{
                        fileFoder2 += File.separator+ "downloadBatch"+File.separatorChar;
                    }
                }
                toWriteLog("onlydownloadfj----5----批量下载--fileFoder2："+fileFoder2+"--filerealpathTempList:"
                        +filerealpathTempList+"--userid:"+userid);
                //String fileFoder=GCONST.getRootPath()+ "downloadBatch"+File.separatorChar;//获取系统的运行目录 如：d:\ecology\
                //String strs=requestname+"_"+userid+".zip";
                //附件的名字
                String strs=SystemEnv.getHtmlLabelName(labelid,loginuser.getLanguage())+"_"+userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";
                
                String tmptfilename=userid+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".zip";
                
                toWriteLog("onlydownloadfj----5----批量下载--strs："+strs+"--userid:"+userid+"--fileFoder2:"+fileFoder2);
                File file=this.fileCreate(fileFoder2, tmptfilename);//在系统的根目录下创建了一个文件夹(downloadBatch)及zip文件
                toWriteLog("onlydownloadfj----5----批量下载--strs："+strs+"--userid:"+userid+"--file:"+file);
                String strzipName=file.getPath();
                toWriteLog("onlydownloadfj----5----批量下载--strs："+strs+"--userid:"+userid+"--strzipName:"+strzipName);
                //String fileTempParent=fileTemp.getParent();
                String fileTempParent=null;
                if(fileTemp!=null){
                    fileTempParent=fileTemp.getParent();
                }
                ZipOutputStream zout =null;
                InputStream is=null;
                try {
                    DocAddWaterForSecond docAddWaterForSecond = new DocAddWaterForSecond();
                    zout = new ZipOutputStream(new FileOutputStream(strzipName), "UTF-8");
                    //循环下载每个文件并读取内容
                    //文档中附件批量下载
                    for(int j=0;j<files.length;j++){
                        try{
                         is=new FileInputStream(files[j]);
//                         System.out.println("第["+j+"]个文件的名称为："+files[j].getName());
                         String filename = files[j].getName();
                         String extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                         if(!frompdfview.equals("1") && download.equals("1")){
                             Map<String,Object> secWmSetMap = WaterMarkUtil.getCategoryWmSet(imagefileid);
                             if("1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYDOWNLOAD))&& "1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYWMISOPEN)) && "0".equals(secWmSetMap.get(WaterMarkUtil.WATERCONTENTISNULL))){
//                                 is = WaterMarkUtil.takefileWater(is,loginuser,filename,fileids.get(j),extName,WaterMarkUtil.MOULDDOC);
                                 is = FileWaterManager.takewater(req,fileids.get(j)+"",filename,extName,is, WaterMarkUtil.MOULDDOC,-1);
                             }
                         }
                         ZipEntry ze=new ZipEntry(filenameMap.get(files[j].getName()));//设置文件编码格式
                         zout.putNextEntry(ze);
                         int len;
                         //读取下载文件内容
                         while((len=is.read(bt))>0){
                          zout.write(bt, 0, len);
                         }
                         zout.closeEntry();
                         is.close();
                        }catch(Exception e){
                            BaseBean basebean = new BaseBean();
                            basebean.writeLog(e);
                            continue;
                        }
                    }
                    zout.close();
                    this.toUpload(res, strs,tmptfilename);//调用下载方法
                    //String fileTempParent=fileTemp.getParent();
                    //this.deleteFile(fileTempParent);//下载完成后调用删除文件的方法删除downloadBatchTemp文件夹下的子文件夹
                    /*
                    for(int i=0;i<filerealpathTempList.size();i++){
                        String delPathTemp=(String)filerealpathTempList.get(i);
                        this.deleteFile(delPathTemp);//下载完成后调用删除方法删除downloadBatchTemp临时文件夹中存放的附件文件
                    }*/
                    //this.deleteFile(strzipName);//下载完成后调用删除文件的方法删除downloadBatch文件夹中的.zip文件
             } catch (FileNotFoundException e) {
                    e.printStackTrace();
             }catch (IOException e) {
                    e.printStackTrace();
             }finally {
                 if(is!=null) {
                    is.close();
                 }
                 if(zout!=null){
                    zout.close();
                 }
                 this.deleteFile(strzipName);//下载完成后调用删除文件的方法删除downloadBatch文件夹中的.zip文件
                 //this.deleteFile(fileTempParent);//下载完成后调用删除文件的方法删除downloadBatchTemp文件夹下的子文件夹
                 if(fileTempParent!=null){
                    // new weaver.filter.XssUtil().writeLog("=====L622------"+strzipName);
                     this.deleteFile(fileTempParent);//下载完成后调用删除文件的方法删除downloadBatchTemp文件夹下的子文件夹
                 }
             }
                /*============== 证照批量下载附件     end============================================================*/
        } else{

              int fileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileid(req), -1);
			  toWriteLog("weaver---870----fileid:"+fileid);
            String coworkid = Util.getFileidIn(Util.null2String(req.getParameter("coworkid")));
            int requestid = Util.getIntValue(req.getParameter("requestid"));
            String ipstring = Util.getIpAddr(req);

            Map<String,Object> jsonParams = new HashMap<String,Object>();
            boolean fromMobile = false;
            int userid = 0;
            jsonParams = Json2MapUtil.requestJson2Map(req);
            if(fileid <=0 ){
                fileid = Util.getIntValue(Util.null2String(jsonParams.get("fileid")));
                fromMobile = true;
                userid = Util.getIntValue(Util.null2String(jsonParams.get("userid")));
                requestid = Util.getIntValue(Util.null2String(jsonParams.get("requestid")));
            }
            
            String ddcode = req.getParameter("ddcode");
            int codeuserid = 0;
                User userddcode = (User)req.getSession(true).getAttribute("weaver_user@bean");
            if(ddcode != null && !ddcode.isEmpty() && userddcode==null){
            	try{
            		Map<String,Object> ddcodeMap = SystemDocUtil.deDdcode(userddcode,ddcode);
                    String isovertime = Util.null2String(ddcodeMap.get(SystemDocUtil.ISOVERTIME));
                    if("1".equals(isovertime)){
                        res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?ddcodeTimeout=1");
                        return;
                    }else{
                        userid = Util.getIntValue((String) ddcodeMap.get(SystemDocUtil.USERID));
                        String fileidcode = Util.null2String(ddcodeMap.get(SystemDocUtil.FILEID));
                        fileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileidstr(fileidcode));
                        fromMobile = true;
                        //如果有次账号，userid给次账号的
                        String f_weaver_belongto_userid=Util.null2s(req.getParameter("f_weaver_belongto_userid"),"");
                        if(!"".equals(f_weaver_belongto_userid)){
                            userid = Util.getIntValue(f_weaver_belongto_userid);
                        }
                        jsonParams = Json2MapUtil.requestJson2Map(req);
                        jsonParams.put("userid",userid);
                        codeuserid = Util.getIntValue(Util.null2String(jsonParams.get("userid")));
                    }

            	}catch(Exception e){
                    writeLogs(e);
            	}
            }


            if(fileid <= 0){//转化为int型，防止SQL注入
                res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?v3");
                return;
            }
			RecordSet rsprop = new RecordSet();
			String secondAuthsql = " select distinct docid from docImageFile where imagefileid =?";
			rsprop.executeQuery(secondAuthsql,fileid);
			while(rsprop.next()){
				int newdocid=Util.getIntValue(rsprop.getString("docid"));
				if(newdocid>0){
					String secondAuthFileDownUrl=getSecondAuthFileDownUrl(req,isMobileDown,newdocid);
					if(!"".equals(secondAuthFileDownUrl)){
						 res.sendRedirect(secondAuthFileDownUrl);
						 return;
					}
				}
			}
            /**流程下载附件，获取最新版本附件下载 start */
            String fromrequest = req.getParameter("fromrequest");
            if("1".equals(fromrequest)){
                RecordSet rs = new RecordSet();
                rs.executeSql("select doceditionid,id from DocDetail where id=(select max(docid) from DocImageFile where imagefileid=" + fileid + ")");
                if(rs.next()){
                   int doceditionid = rs.getInt("doceditionid");
                   int docid = rs.getInt("id");

                   if(doceditionid > 0){  //有多版本文档，取最新版本文档
                       rs.executeSql("select id from DocDetail where doceditionid=" + doceditionid + " order by docedition desc");
                       if(rs.next()){
                           docid = rs.getInt("id");
                       }
                   }
                   
                   rs.executeQuery("select id from DocImageFile where imagefileid=? and (isextfile is null or isextfile <> '1' ) and docfiletype <> '1'",fileid);
                   
                   boolean isOfficeFile = false;
                   if(rs.next()){
                	   isOfficeFile = true;
                   }

                   //在新版本文档下取最新附件
                   String sql = "select id,imagefileid from DocImageFile where docid=? ";
                   
                   if(isOfficeFile){
                	   sql += " and (isextfile is null or isextfile <> '1' ) and docfiletype <> '1'"; 
                   }else{
                	   sql += " and isextfile='1'";
                   }
                   
                   sql += "order by versionId desc";
                   rs.executeQuery(sql,docid);
                   int firstId = 0;
                   int i = 0;
                   int _id = 0;
                   while(rs.next()){
                       if(i == 0){
                           firstId = rs.getInt("imagefileid");
                           _id = rs.getInt("id");
                           if(isOfficeFile)
                        	   break;
                       }else{
                           if(_id != rs.getInt("id")){   //一个文档下有多个附件，则不再下载最新，按照传的id下载
                               firstId = fileid;
                               break;
                           }
                       }
                       i++;
                   }
                   fileid = firstId;
                }
            }
            /**流程下载附件，获取最新附件 end */

            //String strSql="select docpublishtype from docdetail where id in (select docid from docimagefile where imagefileid="+fileid+") and ishistory <> 1";
            RecordSet statement = new RecordSet();   //by ben  开启连接后知道文件下载才关闭，数据库连接时间太长
            //RecordSet rs =new RecordSet();
            User user = (User)req.getSession(true).getAttribute("weaver_user@bean");
            try {
                //解决问题：一个附件或图片被一篇内部文档和外部新闻同时引用时，外部新闻可能查看不到附件或图片。 update by fanggsh fot TD5478  begin
                //调整为如下：
                //默认需要用户登录信息,不需要登录信息的情形如下：
                //1、非登录用户查看外部新闻
                //boolean needUser= false;
                boolean needUser= true;
                int docId=0;
                String docIdsForOuterNews="";
                //String strSql="select id from DocDetail where exists (select 1 from docimagefile where imagefileid="+fileid+" and docId=DocDetail.id) and ishistory <> 1 and (docPublishType='2' or docPublishType='3')";
                String strSql = "SELECT  t1.id  FROM DocDetail t1  INNER JOIN docimagefile t2    ON t1.id = t2.docId  " +
        			"WHERE t2.imagefileid = ?   AND t1.ishistory <> 1   AND (  t1.docPublishType = '2'     OR t1.docPublishType = '3')";
		        RecordSet rs = new RecordSet();
		        rs.executeQuery(strSql,fileid);
                while(rs.next()){
                    docId=rs.getInt("id");
                    if(docId>0){
                        docIdsForOuterNews+=","+docId;
                    }
                }

                if(!docIdsForOuterNews.equals("")){
                    docIdsForOuterNews=docIdsForOuterNews.substring(1);
                }

                if(!docIdsForOuterNews.equals("")){
                    String newsClause="";
                    String sqlDocExist=" select 1 from DocDetail where id in("+docIdsForOuterNews+") ";
                    String sqlNewsClauseOr="";
                    boolean hasOuterNews=false;

                    rs.executeSql("select newsClause from DocFrontPage where publishType='0'");
                    while(rs.next()){
                        hasOuterNews=true;
                        newsClause=Util.null2String(rs.getString("newsClause"));
                        if (newsClause.equals("0"))
                        {
                            //newsClause=" 1=1 ";
                            needUser=false;
                            break;
                        }
                        if(!newsClause.trim().equals("")){
                            sqlNewsClauseOr+=" ^_^ ("+newsClause+")";
                        }
                    }
                    ArrayList newsArr = new ArrayList();
                    if(!sqlNewsClauseOr.equals("")&&needUser){
                        //sqlNewsClauseOr=sqlNewsClauseOr.substring(sqlNewsClauseOr.indexOf("("));
                        //sqlDocExist+=" and ("+sqlNewsClauseOr+") ";
                        String[] newsPage = Util.TokenizerString2(sqlNewsClauseOr,"^_^");
                        int i = 0;
                        String newsWhere = "";
                        for(;i<newsPage.length;i++){
                            if(i%10==0){
                                newsArr.add(newsWhere);
                                newsWhere="";
                                newsWhere+=newsPage[i];
                            }else
                                newsWhere+=" or "+newsPage[i];
                        }
                        newsArr.add(newsWhere);
                    }
                    //System.out.print(sqlDocExist);
                    if(hasOuterNews&&needUser){
                        for(int j=1;j<newsArr.size();j++){
                            String newsp = newsArr.get(j).toString();
                            if(j==1)
                                newsp = newsp.substring(newsp.indexOf("or")+2);
                            sqlDocExist+="and("+newsp+")";
                            rs.executeSql(sqlDocExist);
                            sqlDocExist = " select 1 from DocDetail where id in("+docIdsForOuterNews+") ";
                            if(rs.next()){
                                needUser=false;
                                break;
                            }
                        }
                    }
                }


                //处理外网查看默认图片
                    rs.executeSql("SELECT * FROM DocPicUpload  WHERE  Imagefileid="+fileid);
                    if(rs.next()){
                            needUser=false;
                        }

                if(needUser){
                    boolean hasRight=false;
                    try{
                        ResourceComInfo  comInfo = new ResourceComInfo();
                        String fromhrmcontract = Util.null2String(req.getParameter("fromhrmcontract"));
                        String f_weaver_belongto_userid=Util.null2String(req.getParameter("f_weaver_belongto_userid"));//需要增加的代码
                        String f_weaver_belongto_usertype=Util.null2String(req.getParameter("f_weaver_belongto_usertype"));//需要增加的代码
                        if(fromMobile){
                            user = new User(userid);
                        }else{
                            user = HrmUserVarify.getUser(req, res, f_weaver_belongto_userid, f_weaver_belongto_usertype) ;//需要增加的代码
                        }

                        if(user==null){
                            user = (User)req.getSession(true).getAttribute("weaver_user@bean");
                        }
                        baseBean.writeLog("weaver-->1061-->fileid"+fileid+"--->user"+user+"=userid--");
                        if(user==null){
                            AuthService as = new AuthService();

                            String sessionkey = "";
                            String querystring = req.getQueryString();
                            String[] parameters = StringUtils.split(querystring, '&');
                            if(parameters != null) {
                                for(int i=0; i<parameters.length; i++) {
                                    String[] param = StringUtils.split(parameters[i], '=');
                                    if(param != null && param.length >= 2 && "sessionkey".equals(param[0])) {
                                        sessionkey = param[1];
                                        break;
                                    }
                                }
                            }
                            try {
                                user = as.getCurrUser(sessionkey);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if(user ==null && codeuserid>0){
                            user = new User(codeuserid);
                        }
                        baseBean.writeLog("weaver-->1084-->fileid"+fileid+"--->user"+user+"=userid--");
                        if(user == null){
                            res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp");
                            return;
                        }
                        int hrmid = user.getUID();
                        boolean ishr = (HrmUserVarify.checkUserRight("HrmContractAdd:Add",user));//人力资源管理员
						//应用中心-人事-员工关怀提醒-入职周年上传的图片（没有HrmContractAdd:Add合同维护权限时根据这个放行）
						boolean isOtherSettings = (HrmUserVarify.checkUserRight("OtherSettings:Edit",user));
                        baseBean.writeLog("weaver-->1091-->fileid"+fileid+"--->fromhrmcontract"+fromhrmcontract);
                        baseBean.writeLog("weaver-->1091-->fileid"+fileid+"--->ishr"+ishr);
                        baseBean.writeLog("weaver-->1091-->fileid"+fileid+"--->hrmid"+hrmid);
                        if(!"".equals(fromhrmcontract)){
                            String contractman ="";
                            String contractdocid ="";
                            String contSql = "select contractman,contractdocid from HrmContract where id ="+fromhrmcontract;
                            rs.executeSql(contSql);
                            if(rs.next()){
                                contractman = rs.getString("contractman");
                                contractdocid = rs.getString("contractdocid");
                            }

                            contSql = "select * from DocImageFile where docid="+contractdocid+" and imagefileid="+fileid;
                            rs.executeSql(contSql);
                            if(rs.next()){

                                boolean ism = comInfo.isManager(hrmid,contractman); //上级
                                boolean ishe = (hrmid == Util.getIntValue(contractman)); //本人

                                if(ism || ishe || ishr) hasRight = true;
                            }
                        }else{
                            if (isOtherSettings || ishr || hrmid == 1) {
                                hasRight = true;
                            } else {
                                hasRight = getWhetherHasRight("" + fileid, req, res, requestid,fromMobile,jsonParams);
                            }
                        }
                    }catch(Exception ex){
                        BaseBean basebean = new BaseBean();
                        basebean.writeLog(ex);
                        hasRight=false;
                    }
                    baseBean.writeLog("weaver-->1125-->fileid"+fileid+"--->hasRight"+hasRight);
                    if(!hasRight){
                        boolean isOdocSmartRegist = odocSmartRegist(fileid);
                        if(isOdocSmartRegist){
                            hasRight = true;
                        }
                        baseBean.writeLog("weaver-->1125-->fileid"+fileid+"--->hasRight"+hasRight+"odocSmartRegist(fileid)="+isOdocSmartRegist);
                    }
					if(!hasRight && isMobileDown && "1".equals(Util.null2String(baseBean.getPropValue("doc_mobile_detail_prop","doc_acc_isDownloadByProp"),"0"))){
						String isProhibitDownload="0";
                        rs.executeQuery("select dataValue from ECOLOGY_MESSAGE_VALVE_CONFIG where type='prohibitDownload' and dataKey ='prohibitDownloadSwatch'");
                        if (rs.next()){
                            isProhibitDownload=Util.null2String(rs.getString(1),"0");
                        }
						if(!"1".equals(isProhibitDownload)){
							hasRight=true;
						}
					}
                    if(!hasRight){//
                        res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?v5");
                        return;


                    }
                    //end by cyril on 2008-08-01 for td:9133
                }

                //解决问题：一个附件或图片被一篇内部文档和外部新闻同时引用时，外部新闻可能查看不到附件或图片。 update by fanggsh fot TD5478  end
                //statement.close();
                String download = Util.null2String(req.getParameter("download"));
                String contenttype = "";
                String filename = "";
                String filerealpath = "";
                String signInfo = "";
                String hashInfo = "";
                String iszip = "";
                String isencrypt = "";
                String isaesencrypt="";
                String aescode = "";
                String tokenKey="";
                String storageStatus = "";
                String comefrom="";
                String filesize ="";
                String pdfimagefileid = "";
                String downloadpdfimagefileid = "";
                if ("1".equals(req.getParameter("countdownloads"))) {
                    isCountDownloads = true;
                }
                int byteread;
                byte data[] = new byte[1024];



                //String sql = "select imagefilename,filerealpath,iszip,isencrypt,imagefiletype , imagefile from ImageFile where imagefileid = " + fileid;
                String sql = "select t1.imagefilename,t1.filerealpath,t1.signinfo,t1.hashinfo,t1.iszip,t1.isencrypt,t1.imagefiletype , t1.imagefileid, t1.imagefile,t1.isaesencrypt,t1.aescode,t2.imagefilename as realname,t1.TokenKey,t1.StorageStatus,t1.comefrom,t1.filesize from ImageFile t1 left join DocImageFile t2 on t1.imagefileid = t2.imagefileid where t1.imagefileid = "+fileid;
                boolean isoracle = (statement.getDBType()).equals("oracle");

                String extName = "";

                statement.execute(sql);
                //statement.executeQuery();
                if (statement.next()) {
                    filename = Util.null2String(statement.getString("realname"));
                    if(filename.equals("")){
                        filename = Util.null2String(statement.getString("imagefilename"));
                    }
                    if(filename.toLowerCase().endsWith(".pdf")){
                        int decryptPdfImageFileId=0;
                        rs.executeSql("select decryptPdfImageFileId from workflow_texttopdf where pdfImageFileId="+fileid);
                        if(rs.next()){
                            decryptPdfImageFileId=Util.getIntValue(rs.getString("decryptPdfImageFileId"),-1);
                        }
                        if(decryptPdfImageFileId>0){
                            sql = "select t1.imagefilename,t1.filerealpath,t1.signinfo,t1.hashinfo,t1.iszip,t1.isencrypt,t1.imagefiletype , t1.imagefileid, t1.imagefile,t1.isaesencrypt,t1.aescode,t2.imagefilename as realname,t1.TokenKey,t1.StorageStatus,t1.comefrom,t1.filesize from ImageFile t1 left join DocImageFile t2 on t1.imagefileid = t2.imagefileid where t1.imagefileid = "+decryptPdfImageFileId;
                            statement.execute(sql);
                            if(!statement.next()){
                                return ;
                            }
                        }
                    }
                    filerealpath = Util.null2String(statement.getString("filerealpath"));
                    iszip = Util.null2String(statement.getString("iszip"));
                    signInfo = Util.null2String(statement.getString("signinfo"));
                    hashInfo = Util.null2String(statement.getString("hashinfo"));
                    isencrypt = Util.null2String(statement.getString("isencrypt"));
                    isaesencrypt = Util.null2o(statement.getString("isaesencrypt"));
                    aescode = Util.null2String(statement.getString("aescode"));
                    tokenKey = Util.null2String(statement.getString("TokenKey"));
                    storageStatus = Util.null2String(statement.getString("StorageStatus"));
                    comefrom = Util.null2String(statement.getString("comefrom"));
                    filesize = Util.null2String(statement.getString("filesize"));


                    if(!"DocLogAction_DocLogFile".equals(comefrom) && FiledownloadUtil.isdownloadpdf(filename) && download.equals("1") ){
                        pdfimagefileid = FiledownloadUtil.getpdfidByOfficeid(fileid+"");
                        if(pdfimagefileid.isEmpty()){
                            pdfimagefileid = fileid+"";
                        }
                        String pdffileinfosql = "select imagefilename,filerealpath,aescode,iszip,isencrypt,isaesencrypt,TokenKey,StorageStatus,filesize,comefrom,hashinfo,signinfo from imagefile  where imagefileid = ? ";
                        RecordSet pdffileinfors = new RecordSet();
                        pdffileinfors.executeQuery(pdffileinfosql,pdfimagefileid);
                        if(pdffileinfors.next()){
                            filename = Util.null2String(pdffileinfors.getString("imagefilename"));
                            filerealpath = Util.null2String(pdffileinfors.getString("filerealpath"));
                            iszip = Util.null2String(pdffileinfors.getString("iszip"));
                            signInfo = Util.null2String(pdffileinfors.getString("signinfo"));
                            hashInfo = Util.null2String(pdffileinfors.getString("hashinfo"));
                            isencrypt = Util.null2String(pdffileinfors.getString("isencrypt"));
                            isaesencrypt = Util.null2String(pdffileinfors.getString("isaesencrypt"));
                            aescode = Util.null2String(pdffileinfors.getString("aescode"));
                            tokenKey = Util.null2String(pdffileinfors.getString("TokenKey"));
                            storageStatus = Util.null2String(pdffileinfors.getString("StorageStatus"));
                            comefrom = Util.null2String(pdffileinfors.getString("comefrom"));
                            filesize = Util.null2String(pdffileinfors.getString("filesize"));
                        }
                    }else {
                        pdfimagefileid = fileid+"";
                    }

                    if(filename.indexOf(".") > -1){
                        int bx = filename.lastIndexOf(".");
                        if(bx>=0){
                            extName = filename.substring(bx+1, filename.length());
                        }
                    }
                    if(ImageConvertUtil.canConvertPdfForDownload(extName,fileid+"")&& !frompdfview.equals("1") && download.equals("1")) {
                        RecordSet rsrecord = new RecordSet();
                        PdfConvertUtil pdfConvertUtil = new PdfConvertUtil();
                        int pdfid =  pdfConvertUtil.convertPdf(fileid);
                        if (pdfid > 0) {
                            downloadpdfimagefileid = String.valueOf(pdfid);
                            String pdffileinfosql = "select imagefilename,filerealpath from imagefile  where imagefileid = ? ";
                            RecordSet pdffileinfors = new RecordSet();
                            pdffileinfors.executeQuery(pdffileinfosql, downloadpdfimagefileid);
                            if (pdffileinfors.next()) {
                                filename = filename.substring(0, filename.lastIndexOf(".")) + ".pdf";
                                filerealpath = Util.null2String(pdffileinfors.getString("filerealpath"));
                            }
                        }
                    }
                    if(downloadpdfimagefileid.isEmpty()){
                        downloadpdfimagefileid = fileid+"";
                    }

                    if(filename.indexOf(".") > -1){
                        int bx = filename.lastIndexOf(".");
                        if(bx>=0){
                            extName = filename.substring(bx+1, filename.length());
                        }
                    }
                    boolean isInline=false;
                    String cacheContorl="";
                    boolean isEnableForDsp=false;
                    boolean canUseAliOSS = false;
                    if(!tokenKey.equals("")&&storageStatus.equals("1")&&AliOSSObjectManager.isEnableForDsp(req)){
                        isEnableForDsp=true;
                    }
                    if(!tokenKey.equals("")&&storageStatus.equals("1")&&AliOSSObjectManager.canUseAliOSS()){
                        canUseAliOSS=true;
                    }
                    boolean isPic=false;
                    String lowerfilename = filename!=null ? filename.toLowerCase() : "";
                    boolean ishtmlfile = false;
                    boolean needGbkCode = false;
                    if(lowerfilename.endsWith(".html")||lowerfilename.endsWith(".htm")){
                    	RecordSet rs_tmp = new RecordSet();
                        
                        String _sql = "select i.imagefilename from DocPreviewHtml a,imagefile i "
                            + "where a.imagefileid=i.imagefileid and a.htmlfileid = ?";
                        
                        if("email".equals(req.getParameter("model"))){
                        	_sql = "select filename as imagefilename from mailresourcefile where htmlcode=?";
                        }
                        
                        rs_tmp.executeQuery(_sql,fileid);
                        if(rs_tmp.next()){
                            ishtmlfile = true;
                            String _imagefilename = Util.null2String(rs_tmp.getString("imagefilename"));
                            needGbkCode = _imagefilename.toLowerCase().endsWith(".xls")
                                    || _imagefilename.toLowerCase().endsWith(".xlsx")
                                    || _imagefilename.toLowerCase().endsWith(".doc");
                        }
                        String worflowhtmlSql = " select comefrom from imagefile where imagefileid = ?";
                        rs_tmp.executeQuery(worflowhtmlSql,fileid);
                        if(rs_tmp.next()){
                            String htmlcomefrom = Util.null2String(rs_tmp.getString("comefrom"));
                            if("WorkflowToDoc".equals(htmlcomefrom)){
                                ishtmlfile = true;
                            }
                        }
                    }
                    filename = StringEscapeUtils.unescapeHtml(filename);
                    String extendName = lowerfilename.lastIndexOf(".") != -1 ? lowerfilename.substring(lowerfilename.lastIndexOf(".") + 1) : lowerfilename;
                    boolean inlineView = inlineViewFile(extendName);
                    if (download.equals("") && (inlineView
                            ||ishtmlfile)){
                        if(filename.toLowerCase().endsWith(".doc")) contenttype = "application/msword";
                        else if(filename.toLowerCase().endsWith(".xls")) contenttype = "application/vnd.ms-excel";
                        else if(filename.toLowerCase().endsWith(".gif")) {
                            contenttype = "image/gif";
                            res.addHeader("Cache-Control", "private, max-age=8640000");
                            isPic=true;
                        }else if(filename.toLowerCase().endsWith(".png")) {
                            contenttype = "image/png";
                            res.addHeader("Cache-Control", "private, max-age=8640000");
                            isPic=true;
                        }else if(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                            contenttype = "image/jpg";
                            res.addHeader("Cache-Control", "private, max-age=8640000");
                            isPic=true;
                        }else if(filename.toLowerCase().endsWith(".bmp")) {
                            contenttype = "image/bmp";
                            res.addHeader("Cache-Control", "private, max-age=8640000");
                            isPic=true;
                        }else if(filename.toLowerCase().endsWith(".svg")){
                            contenttype = "image/svg+xml";
                            res.addHeader("Cache-Control", "private, max-age=8640000");
                            isPic=true;
                        }
                        else if(filename.toLowerCase().endsWith(".txt")) contenttype = "text/plain";
                        else if(filename.toLowerCase().endsWith(".pdf")) contenttype = "application/pdf";
                        else if(filename.toLowerCase().endsWith(".html")||filename.toLowerCase().endsWith(".htm")) contenttype = "text/html";
                        else {
                            contenttype = statement.getString("imagefiletype");
                        }
                        try {
                        	
                        	String _ec_browser = Util.null2String(req.getParameter("_ec_browser"));
                        	String _ec_os = Util.null2String(req.getParameter("_ec_os"));
                        	
                        	if("Wechat".equals(_ec_browser) && "iOS".equals(_ec_os)){  //微信
                        		res.setHeader("content-disposition", "inline; filename=\"" + new String(filename.getBytes("UTF-8"), "ISO8859-1")+"\"");
                        	}else if(_ec_ismobile || from_doc_mobile || agent.toLowerCase().contains("emobile") || agent.toLowerCase().contains("android") || agent.toLowerCase().contains("iphone")){
                               // res.setHeader("content-disposition", "inline; filename=\"" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")")+"\"");
                                filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
                                res.setHeader("Content-disposition", "inline;filename=\"" + filename + "\"");
                            }else {
                                if ((agent.contains("Firefox") || agent.contains(" Chrome") || agent.contains("Safari")) && !agent.contains("Edge")) {
                                    res.setHeader("content-disposition", "inline; filename*=UTF-8''" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
                                } else {
                                    res.setHeader("content-disposition", "inline; filename=\"" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")") + "\"");
                                }
                            }
                        } catch (Exception ecode) {
                            writeLogs(ecode);
                        }
                        isInline=true;
                    }else {
                        contenttype = "application/octet-stream";
                        if (filename.toLowerCase().endsWith(".mp4") || filename.toLowerCase().endsWith(".mov")) {
                            contenttype = "video/mp4";
                        } else if (filename.toLowerCase().endsWith(".ogg")) {
                            contenttype = "video/ogg";
                        } else if (filename.toLowerCase().endsWith(".asf")) {
                            contenttype = "video/x-ms-asf";
                        } else if (filename.toLowerCase().endsWith(".mp3")) {
                            contenttype = "audio/mp3";
                        } else if (filename.toLowerCase().endsWith(".wav")) {
                            contenttype = "audio/wav";
                        } else if (filename.toLowerCase().endsWith(".wma")) {
                            contenttype = "audio/x-ms-wma";
                        } else if (filename.toLowerCase().endsWith(".au")) {
                            contenttype = "audio/basic";
                        }
                        
                        if(filename.toLowerCase().endsWith(".pdf")) contenttype = "application/pdf";
                        try {
//                            res.setHeader("Content-length", "" + filesize);
//                            res.setHeader("Content-Range", "bytes");
//                            res.addHeader("Cache-Control", "no-cache");

                            //System.out.println(new String(new String(filename.getBytes(clientEcoding), "ISO8859_1").getBytes("ISO8859_1"),"utf-8"));
                            //System.out.println(new String(filename.getBytes(clientEcoding)));
                        	String _ec_browser = Util.null2String(req.getParameter("_ec_browser"));
                        	String _ec_os = Util.null2String(req.getParameter("_ec_os"));
                        	if("Wechat".equals(_ec_browser) && ("iOS".equals(_ec_os) || "Mac OS".equals(_ec_os))){  //微信
                        		res.setHeader("content-disposition", "inline; filename=\"" + new String(filename.getBytes("UTF-8"), "ISO8859-1")+"\"");
                        	}else if(agent.contains("Wechat") && (agent.contains("iOS") || agent.contains("Mac OS"))){
                                filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
                                res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                            }else if(agent.contains("Mac OS") && !AliOSSObjectManager.isEnable()){
                                filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
                                res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                            } else if(_ec_ismobile || from_doc_mobile||agent.toLowerCase().contains("android")){
                                if(FiledownloadUtil.isIos(req)){
                                    filename = FiledownloadUtil.counthanzi(filename,extName);
                                }
                            	
                				res.setHeader("content-disposition", "attachment; filename=\"" +
                						URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("%", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")")+"\"");
                            }else {
                                if ( (agent.contains(" Chrome")) && !agent.contains("Edge")) {
//                                    res.setHeader("content-disposition", "attachment;");
                                     res.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(")
                                             .replaceAll("%29", ")").replaceAll("%7B","{").replaceAll("%7D","}").replaceAll("%5B","[").replaceAll("%5D","]").replaceAll("%40","@").replaceAll("%23","#").replaceAll("%25","%").replaceAll("%26","&")
                                             .replaceAll("%2B","+").replaceAll("%27","'").replaceAll("%20"," "));

//                                    res.setHeader("content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
//                                    res.setHeader("content-disposition", "attachment;filename="+ URLEncoder.encode(filename, "UTF-8"));
//                                    res.setContentType("application/vnd.ms-excel;charset=utf-8");
//                                    res.setCharacterEncoding("UTF-8");
                                }else if(agent.contains("Safari")){
                                    filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
                                    res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                                }else if(agent.contains("Firefox")){
                                   // filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
                                   // res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                                    
//                                  filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
//                                  res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                                  //  filename = "=?UTF-8?B?" + (new String(Base64.encodeBase64(filename.getBytes("UTF-8")))) + "?=";
                                  //  res.setHeader("Content-disposition", String.format("attachment; filename=\"%s\"", filename));
                                    
                                	res.setHeader("content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
                                } else{
                                    res.setHeader("content-disposition", "attachment; filename=\"" +
                                            URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")") + "\"");
                                }
                            }
                        } catch (Exception ecode) {
                            writeLogs(ecode);
                        }
                    }
                    String iscompress = Util.null2String(req.getParameter("iscompress"));
					String thumbnail = Util.null2String(req.getParameter("thumbnail"));
                    String targetFilePath="";
                    if(isEnableForDsp||canUseAliOSS){
                    	
                    	//解决安卓app预览黑屏的问题
                    	boolean isAndroid = agent.contains("Android") && agent.startsWith("E-Mobile7");
                    	if(isAndroid && download.equals("1")){
	                    	if(filename.toLowerCase().endsWith(".gif")) {
	                            contenttype = "image/gif";
	                            res.addHeader("Cache-Control", "private, max-age=8640000");
	                            isPic=true;
	                        }else if(filename.toLowerCase().endsWith(".png")) {
	                            contenttype = "image/png";
	                            res.addHeader("Cache-Control", "private, max-age=8640000");
	                            isPic=true;
	                        }else if(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
	                            contenttype = "image/jpg";
	                            res.addHeader("Cache-Control", "private, max-age=8640000");
	                            isPic=true;
	                        }else if(filename.toLowerCase().endsWith(".bmp")) {
	                            contenttype = "image/bmp";
	                            res.addHeader("Cache-Control", "private, max-age=8640000");
	                            isPic=true;
	                        }else if(filename.toLowerCase().endsWith(".svg")){
	                            contenttype = "image/svg+xml";
	                            res.addHeader("Cache-Control", "private, max-age=8640000");
	                            isPic=true;
	                        }
                    	}

                        boolean  isAliOSSToServer=AliOSSObjectManager.isAliOSSToServer(comefrom);
                        boolean isOpenWaterMark = WaterMarkUtil.isOpenWmSetting();
						boolean  isSafari=agent.contains("Mac OS");
						int imgDownByObs=Util.getIntValue(baseBean.getPropValue("FileDownload","imgDownByObs"),1); 
                        baseBean.writeLog("FileDownload---------1645---fileid="+fileid+";isPic="+isPic+";filename="+filename+";isAliOSSToServer="+isAliOSSToServer+";imgDownByObs="+imgDownByObs+";frompdfview="+frompdfview+";isOpenWaterMark="+isOpenWaterMark+";ishtmlfile="+ishtmlfile+";canUseAliOSS="+canUseAliOSS+";isEnableForDsp="+isEnableForDsp+";isSafari="+isSafari);
						//canUseAliOSS && !isEnableForDs ：判断是否开启了阿里云并且请求端是内网地址的情况下走此逻辑
                        if(isAliOSSToServer|| (isPic && imgDownByObs !=1) || !frompdfview.isEmpty() || isOpenWaterMark || ishtmlfile || (canUseAliOSS && !isEnableForDsp)||isSafari){
                            baseBean.writeLog("FileDownload---------1648---fileid="+fileid+";isPic="+isPic+";11111");
							InputStream imagefile = null;
                            ServletOutputStream out = null;
                            DocAddWaterForSecond docAddWaterForSecond = new DocAddWaterForSecond();
                            try {
                                imagefile=weaver.alioss.AliOSSObjectUtil.downloadFile(tokenKey);
                                if("1".equals(iscompress)){
                                    ImageCompressUtil imageCompressUtil=new ImageCompressUtil();
                                    targetFilePath = imageCompressUtil.getTargetFilePath();
                                    imagefile=imageCompressUtil.imageCompress(imagefile,targetFilePath);
                                }
                                out = res.getOutputStream();
                                
                                if(ishtmlfile && needGbkCode){
                                    boolean loadGBK = !"0".equals(rs.getPropValue("docpreview", "loadGBK"));
                                    if(loadGBK){
                                        res.setContentType(contenttype+";charset=GBK");
                                    }
                                }else{
                                	res.setContentType(contenttype);
                                }
//                                 extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                                if(!frompdfview.equals("1") && download.equals("1")){
                                    Map<String,Object> secWmSetMap = WaterMarkUtilNew.getCategoryWmSet(fileid+"");
                                    if("1".equals(secWmSetMap.get(WaterMarkUtilNew.SECCATEGORYDOWNLOAD))&& "1".equals(secWmSetMap.get(WaterMarkUtilNew.SECCATEGORYWMISOPEN)) && "0".equals(secWmSetMap.get(WaterMarkUtilNew.WATERCONTENTISNULL))){
//                                       imagefile = WaterMarkUtil.takefileWater(imagefile,user,filename,fileid,extName,WaterMarkUtil.MOULDDOC);
                                        imagefile = FileWaterManager.takewater(req,downloadpdfimagefileid,filename,extName,imagefile, WaterMarkUtil.MOULDDOC,fileid);
                                    }

                                }
								long filetotalsize=0;							
								String _ec_browser = Util.null2String(req.getParameter("_ec_browser"));
								boolean isWeChat=false;
								if(agent.contains("Wechat") || agent.contains("Android") || "Wechat".equalsIgnoreCase(_ec_browser)){
									isWeChat=true;
								}
								baseBean.writeLog("FileDownload---------1496---fileid="+fileid+";isWeChat="+isWeChat+";_ec_browser="+_ec_browser+";agent="+agent);
								String sourceFilePath="";	
								if(isWeChat){								
                                    SystemComInfo syscominfo = new SystemComInfo();
									String syspath= Util.null2String(syscominfo.getFilesystem());
									String rootPath = Util.null2String(GCONST.getRootPath());
									if("".equals(syspath)){												
									sourceFilePath = rootPath + "doctempfile";        
									}else{
										if(syspath.endsWith(File.separator)){										
											sourceFilePath =syspath+ "doctempfile";
										}else{										
											sourceFilePath =syspath+File.separator+ "doctempfile";
										}
									}											       
									File sfile = new File(sourceFilePath);							
									if(!sfile.exists()) sfile.mkdirs();
									sourceFilePath += File.separator + UUID.randomUUID().toString();
									OutputStream os = null;
									int read;
									byte[] buf = new byte[8 * 1024];
									try{
									   os = new FileOutputStream(sourceFilePath);
									   while((read = imagefile.read(buf)) != -1) {
										   os.write(buf, 0, read);
										   filetotalsize += read;
									   }
									}catch(Exception e){
									   
									}finally{
									   if(os != null)
										   os.close();
									}
									imagefile = new FileInputStream(sourceFilePath);
									res.setHeader("Content-length", "" + filetotalsize);
								}

                                if("1".equals(download)) {
                                    imagefile = FileManager.download(req,fileid+"",filerealpath,aescode,iszip,isaesencrypt,imagefile);
                                }

                                while ((byteread = imagefile.read(data)) != -1) {
                                    out.write(data, 0, byteread);
                                    out.flush();
                                }
								try{
									if(!"".equals(sourceFilePath)){
										File f = new File(sourceFilePath);
										if(f.exists()&&f.isFile()){
											FileSecurityUtil.deleteFile(f);
										}
									}								
								}catch(Exception e) {
									baseBean.writeLog("FileDownload---------1533---fileid="+fileid+";Exception="+e);
								}
                            }
                            catch(Exception e) {
                                writeLogs(e);
                                //do nothing
                            }
                            finally {
                                if(imagefile!=null) imagefile.close();
                                if(out!=null) out.flush();
                                if(out!=null) out.close();
                                if("1".equals(iscompress)&& StringUtils.isNotEmpty(targetFilePath)){
                                    File targetfile = new File(targetFilePath);
                                    if(targetfile.exists()){
                                        FileSecurityUtil.deleteFile(targetfile);
                                        //new FileDeleteUtil().deleteFile(targetfile);
                                    }
                                }
                            }

                            try{
                                if(needUser&&nolog==0) {
                                    //记录下载日志 begin
                                    HttpSession session = req.getSession(false);
                                    if (session != null) {
                                        user = (User) session.getAttribute("weaver_user@bean");
                                        if (user != null) {
                                            //董平修改　文档下载日志只记录了内部员工的名字，如果是客户门户来下载，则没有记录　for TD:1644
                                            String userType = user.getLogintype();
                                            if ("1".equals(userType)) {   //如果是内部用户　名称就是　lastName 外部则入在　firstName里面
                                                downloadLog(user.getUID(), user.getLastname(), fileid, filename,ipstring);
                                            } else {
                                                downloadLog(user.getUID(), user.getFirstname(), fileid, filename,ipstring);
                                            }

                                        }
                                    }
                                    //记录下载日志 end
                                }

                                countDownloads(""+fileid);
                            }catch(Exception ex){
                                writeLogs(ex);
                            }
                            return ;
                        }else{
							baseBean.writeLog("FileDownload---------1767---fileid="+fileid+";isPic="+isPic+";222222");
                            String urlString=weaver.alioss.AliOSSObjectUtil.generatePresignedUrl(tokenKey,filename,contenttype,isInline,cacheContorl,isSafari);
                            if(urlString!=null){
                                try{
                                    if(needUser&&nolog==0) {
                                        //记录下载日志 begin
                                        HttpSession session = req.getSession(false);
                                        if (session != null) {
                                            user = (User) session.getAttribute("weaver_user@bean");
                                            if (user != null) {
                                                //董平修改　文档下载日志只记录了内部员工的名字，如果是客户门户来下载，则没有记录　for TD:1644
                                                String userType = user.getLogintype();
                                                if ("1".equals(userType)) {   //如果是内部用户　名称就是　lastName 外部则入在　firstName里面
                                                    downloadLog(user.getUID(), user.getLastname(), fileid, filename,ipstring);
                                                } else {
                                                    downloadLog(user.getUID(), user.getFirstname(), fileid, filename,ipstring);
                                                }

                                            }
                                        }
                                        //记录下载日志 end
                                    }

                                    countDownloads(""+fileid);
                                }catch(Exception ex){
                                    writeLogs(ex);
                                }
                                //urlString=urlString+"&fileid="+fileid;
                                res.sendRedirect(urlString);
                                return;
                            }
                        }
                    }

                    InputStream imagefile = null;
                    ZipInputStream zin = null;
                    /*if (filerealpath.equals("")) {         // 旧的文件放在数据库中的方式
                        if (isoracle)
                            imagefile = new BufferedInputStream(statement.getBlobBinary("imagefile"));
                        else
                            imagefile = new BufferedInputStream(statement.getBinaryStream("imagefile"));
                    } else*/       //目前已经不可能将文件存放在数据库中了

                        File thefile = new File(filerealpath);
                        boolean signResult = false;
                        if (!filerealpath.isEmpty() && !signInfo.isEmpty() && !hashInfo.isEmpty()) {
                            signResult = com.api.doc.util.DocEncryptUtil.verifyFile(hashInfo, signInfo, filerealpath);
                            // 如果签名不通过，则跳转提醒页面
                            if (!signResult ) {
                                res.sendRedirect(GCONST.getContextPath()+"/wui/common/page/integrityFailure.jsp?line=4&user=null");
                                return;
                            }
                        }

                        if (iszip.equals("1")) {
                            zin = new ZipInputStream(new FileInputStream(thefile));
                            if (zin.getNextEntry()!=null) imagefile = new BufferedInputStream(zin);
                        } else{
                            imagefile = new BufferedInputStream(new FileInputStream(thefile));
                        }

                        if(isaesencrypt.equals("1")){
                            imagefile = AESCoder.decrypt(imagefile, aescode);
                        }

                        if (signResult) {
                            imagefile = com.api.doc.util.DocEncryptUtil.decryptInput(imagefile);
                            if (imagefile == null) {
                                res.sendRedirect(GCONST.getContextPath()+"/wui/common/page/integrityFailure.jsp?line=4&user=null");
                                return;
                            }
                        }
                        if(download.equals("1") && (isOfficeToDocument(extName))&&isMsgObjToDocument()) {
                            //正文的处理
                            ByteArrayOutputStream bout = null;
                            try {
                                bout = new ByteArrayOutputStream() ;
                                while((byteread = imagefile.read(data)) != -1) {
                                    bout.write(data, 0, byteread) ;
                                    bout.flush() ;
                                }
                                byte[] fileBody = bout.toByteArray();
                                iMsgServer2000 MsgObj = new iMsgServer2000();
                                MsgObj.MsgFileBody(fileBody);           //将文件信息打包
                                fileBody = MsgObj.ToDocument(MsgObj.MsgFileBody());    //通过iMsgServer200 将pgf文件流转化为普通Office文件流
                                imagefile = new ByteArrayInputStream(fileBody);
                                bout.close();
                            }
                            catch(Exception e) {
                                writeLogs(e);
                                if(bout!=null) bout.close();
                            }
                        }
                    ServletOutputStream out = null;
                    DocAddWaterForSecond docAddWaterForSecond = new DocAddWaterForSecond();
                    try {
                        out = res.getOutputStream();
                        res.setContentType(contenttype);

                        if(ishtmlfile && needGbkCode){
                            boolean loadGBK = !"0".equals(rs.getPropValue("docpreview", "loadGBK"));
                            if(loadGBK){
                                res.setContentType(contenttype+";charset=GBK");
                            }
                        }
                        if("1".equals(iscompress)){
                            ImageCompressUtil imageCompressUtil=new ImageCompressUtil();
                            targetFilePath = imageCompressUtil.getTargetFilePath();
                            imagefile=imageCompressUtil.imageCompress(imagefile,targetFilePath);
                            }else if ("1".equals(thumbnail)){
                                imagefile = getThumbnail(fileid, imagefile);
                            }
                        if(!frompdfview.equals("1") && download.equals("1")){
                            Map<String,Object> secWmSetMap = WaterMarkUtil.getCategoryWmSet(fileid+"");
                            if("1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYDOWNLOAD))&& "1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYWMISOPEN)) && "0".equals(secWmSetMap.get(WaterMarkUtil.WATERCONTENTISNULL))){
//                                imagefile = WaterMarkUtil.takefileWater(imagefile,user,filename,fileid,extName,WaterMarkUtil.MOULDDOC);
                                imagefile = FileWaterManager.takewater(req,downloadpdfimagefileid,filename,extName,imagefile, WaterMarkUtil.MOULDDOC,fileid);
                            }
                        }

                        new BaseBean().writeLog("==zj==(filerealpath)" +filerealpath);
                        if("1".equals(download)) {
                            imagefile = FileManager.download(req,fileid+"",filerealpath,aescode,iszip,isaesencrypt,imagefile);
                        }
                                //加密流程返回false
                        if(((contenttype.contains("video")&& agent.contains("iPhone")) || contenttype.contains("audio")) && !download.equals("1") ) {//ios播放视频文件，解析range字段
                            sendVideoToIOS(req, res, imagefile, filename,filerealpath);
                        }else {
							long filetotalsize=0;							
							String _ec_browser = Util.null2String(req.getParameter("_ec_browser"));
							boolean isWeChat=false;
                        	if(agent.contains("Wechat") || agent.contains("Android") || "Wechat".equalsIgnoreCase(_ec_browser)){
								isWeChat=true;
							}
							baseBean.writeLog("FileDownload---------1675---fileid="+fileid+";isWeChat="+isWeChat+";_ec_browser="+_ec_browser+";agent="+agent);
							String sourceFilePath="";	
                        	if(isWeChat){								
                                SystemComInfo syscominfo = new SystemComInfo();
								String syspath= Util.null2String(syscominfo.getFilesystem());
								String rootPath = Util.null2String(GCONST.getRootPath());
								if("".equals(syspath)){												
								sourceFilePath = rootPath + "doctempfile";        
								}else{
									if(syspath.endsWith(File.separator)){										
										sourceFilePath =syspath+ "doctempfile";
									}else{										
										sourceFilePath =syspath+File.separator+ "doctempfile";
									}
								}								      
								File sfile = new File(sourceFilePath);							
								if(!sfile.exists()) sfile.mkdirs();
								sourceFilePath += File.separator + UUID.randomUUID().toString();
								OutputStream os = null;
								int read;
								byte[] buf = new byte[8 * 1024];
								try{
								   os = new FileOutputStream(sourceFilePath);
								   while((read = imagefile.read(buf)) != -1) {
									   os.write(buf, 0, read);
									   filetotalsize += read;
								   }
								}catch(Exception e){
								   
								}finally{
								   if(os != null)
									   os.close();
								}
								imagefile = new FileInputStream(sourceFilePath);
								res.setHeader("Content-length", "" + filetotalsize);
							}							
                            // 文档/流程中单个附件下载
                            OutputStream outs = res.getOutputStream();
                        	//qc2440302 PC预览下载加密，移动端非指定角色下载加密
                            MobileUtil mobileUtil = new MobileUtil();
                            String isOpenEncrypt = rs.getPropValue("qc2440302", "isOpenEncrypt");
                            if ("1".equals(download) && filename.toLowerCase().endsWith(".pdf")){
                                new BaseBean().writeLog("==zj==(是否开启加密限制)" + isOpenEncrypt);
                                new BaseBean().writeLog("==zj==(权限查看)" + "是否为维护角色:"+mobileUtil.PDFReadLine(user.getUID())+"--是否为移动端下载:"+isMobileDown+"--是否开启加密限制："+"1".equals(isOpenEncrypt));
                                if(!mobileUtil.PDFReadLine(user.getUID()) && isMobileDown && "1".equals(isOpenEncrypt)){
                                    //移动端非指定角色下载加密
                                    outs  = DrmAgent.getInstance().encryptBasicStream(outs,"fwoa",1);
                                }
                                if (!isMobileDown && "1".equals(isOpenEncrypt)){
                                    //pc端下载加密
                                    outs  = DrmAgent.getInstance().encryptBasicStream(outs,"fwoa",1);
                                }
                            }
                        	while ((byteread = imagefile.read(data)) != -1) {
                                if("1".equals(download) && filename.toLowerCase().endsWith(".pdf")){
                                    //移动端下载，非指定角色，且开启加密限制走加密下载
                                    if (isMobileDown && !mobileUtil.PDFReadLine(user.getUID()) && "1".equals(isOpenEncrypt)){
                                        outs.write(data,0,byteread);
                                        outs.flush();
                                    } else if (!isMobileDown && "1".equals(isOpenEncrypt)){
                                        //pc端下载加密
                                        outs.write(data,0,byteread);
                                        outs.flush();
                                    }else {
                                        out.write(data, 0, byteread);
                                        out.flush();
                                    }

                                }else {
                                    out.write(data, 0, byteread);
                                    out.flush();
                                }

                            }
							try{
								if(!"".equals(sourceFilePath)){
									File f = new File(sourceFilePath);
									if(f.exists()&&f.isFile()){
										FileSecurityUtil.deleteFile(f);
									}
								}								
							}catch(Exception e) {
								baseBean.writeLog("FileDownload---------1707---fileid="+fileid+";Exception="+e);
							}
                        }


                    }
                    catch(Exception e) {
                        writeLogs(e);
                        //do nothing
                    }
                    finally {
                        if(imagefile!=null) imagefile.close();
                        if(zin!=null) zin.close();
                        if(out!=null) out.flush();
                        if(out!=null) out.close();
                        if("1".equals(iscompress)&& StringUtils.isNotEmpty(targetFilePath)){
                            File targetfile = new File(targetFilePath);
                            FileSecurityUtil.deleteFile(targetfile);
                            //new FileDeleteUtil().deleteFile(targetFilePath);
                        }

                    }

                    if(needUser&&nolog==0) {
                        //记录下载日志 begin
                        HttpSession session = req.getSession(false);
                        if (session != null) {
                            user = (User) session.getAttribute("weaver_user@bean");
                            if (user != null) {
                                //董平修改　文档下载日志只记录了内部员工的名字，如果是客户门户来下载，则没有记录　for TD:1644
                                String userType = user.getLogintype();
                                if ("1".equals(userType)) {   //如果是内部用户　名称就是　lastName 外部则入在　firstName里面
                                    downloadLog(user.getUID(), user.getLastname(), fileid, filename,ipstring);
                                } else {
                                    downloadLog(user.getUID(), user.getFirstname(), fileid, filename,ipstring);
                                }

                            }
                        }
                        //记录下载日志 end
                    }


                    countDownloads(""+fileid);
                }
            } catch (Exception e) {
                BaseBean basebean = new BaseBean();
                basebean.writeLog(e);
            } //错误处理
            }
        }else{
            String urlstr = req.getParameter("urlstr");
            res.addHeader("Cache-Control", "private, max-age=8640000");
			boolean isNotUse=true;
			if(isNotUse){
				return;
			}
            InputStream imagefile = null;
            URL url = new URL(urlstr);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5 * 1000);
            imagefile = conn.getInputStream();

            ServletOutputStream outputStream = res.getOutputStream();

            byte data[] = new byte[1024];
            int byteread;

            while ((byteread = imagefile.read(data)) != -1) {
                outputStream.write(data, 0, byteread);
                outputStream.flush();
            }

            outputStream.close();
            imagefile.close();
        }
    }

    /**
     * 获取缩略图的流对象
     * @param fileid
     * @param is
     * @return
     */
    private InputStream getThumbnail(int fileid,InputStream is){
        RecordSet rs = new RecordSet();


        int width = Util.getIntValue(rs.getPropValue("docthumbnailfile","width"),300);
        width = width <= 0 ? 0 : width;

        rs.executeQuery("select thumbnailid from ImageFileThumbnail where imgfileid=? and width=?",fileid,width);
        if(rs.next()){
            //System.out.println("id为："+fileid+",走的缩略图");
            return ImageFileManager.getInputStreamById(Util.getIntValue(rs.getString("thumbnailid")));
        }

        ImageCompressUtil imageCompressUtil=new ImageCompressUtil();

        String targetFilePath = imageCompressUtil.getTargetFilePath();

        InputStream imagefile=imageCompressUtil.imageCompressPercent(is,targetFilePath,width,0.7f);

        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] in2b = null;
        try{
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = imagefile.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            in2b = swapStream.toByteArray();
        }catch(Exception e){

        }finally{
            try{
                swapStream.close();
            }catch(Exception e){}
            try{
                if(imagefile != null)
                    imagefile.close();
            }catch(Exception e){}
        }

        ImageFileManager ifm = new ImageFileManager();
        ifm.setData(in2b);
        ifm.setImagFileName(UUID.randomUUID().toString()+ ".jpeg");
        int thumbnailid = ifm.saveImageFile();
        if(thumbnailid <= 0){
            return ImageFileManager.getInputStreamById(fileid);
        }

        rs.executeUpdate("insert into ImageFileThumbnail(imgfileid,thumbnailid,width) values(?,?,?)",fileid,thumbnailid,width);

        return ImageFileManager.getInputStreamById(thumbnailid);
    }
	
	public void sendVideoToIOSJSP(HttpServletRequest request, HttpServletResponse response, InputStream inputStream, String fileName,String filerealpath) throws FileNotFoundException, IOException {
        sendVideoToIOS(request,response,inputStream,fileName,filerealpath);
    }

    /**
     * ios传输视频，必须要解析range字段
     * @param request
     * @param response
     * @param inputStream
     * @param fileName
     * @param filerealpath
     */
    private void sendVideoToIOS(HttpServletRequest request, HttpServletResponse response, InputStream inputStream, String fileName,String filerealpath) throws FileNotFoundException, IOException {
        if(filerealpath.endsWith(".zip")){
            filerealpath = filerealpath.substring(0,filerealpath.length() - 4);
        }
        File tempfile = new File(filerealpath + "_tmp");
        FileOutputStream fos  = null;
        RandomAccessFile randomFile = null;
        ServletOutputStream out = null;
        try {
            byte[] buffer = new byte[4096];
            int byteread = 0;
            fos = new FileOutputStream(tempfile);
            while ((byteread = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, byteread);
            }
            fos.flush();

            randomFile = new RandomAccessFile(tempfile, "r");//只读模式
            long contentLength = randomFile.length();
            String range =  request.getHeader("Range");
            int start = 0, end = 0;
            if(range != null && range.startsWith("bytes=")){
                String[] values = range.split("=")[1].split("-");
                start = Integer.parseInt(values[0]);
                if(values.length > 1){
                    end = Integer.parseInt(values[1]);
                }
            }
            int requestSize = 0;
            if(end != 0 && end > start){
                requestSize = end - start + 1;
            } else {
                requestSize = Integer.MAX_VALUE;
            }
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("ETag", fileName);
            response.setHeader("Last-Modified", new Date().toString());
            //第一次请求只返回content length来让客户端请求多次实际数据
            if(range == null){
                response.setHeader("Content-length", contentLength + "");
            }else{
                //以后的多次以断点续传的方式来返回视频数据
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);//206
                long requestStart = 0, requestEnd = 0;
                String[] ranges = range.split("=");
                if(ranges.length > 1){
                    String[] rangeDatas = ranges[1].split("-");
                    requestStart = Integer.parseInt(rangeDatas[0]);
                    if(rangeDatas.length > 1){
                        requestEnd = Integer.parseInt(rangeDatas[1]);
                    }
                }
                long length = 0;
                if(requestEnd > 0){
                    length = requestEnd - requestStart + 1;
                    response.setHeader("Content-length", "" + length);
                    response.setHeader("Content-Range", "bytes " + requestStart + "-" + requestEnd + "/" + contentLength);
                }else{
                    length = contentLength - requestStart;
                    response.setHeader("Content-length", "" + length);
                    response.setHeader("Content-Range", "bytes "+ requestStart + "-" + (contentLength - 1) + "/" + contentLength);
                }
            }
            out = response.getOutputStream();
            int needSize = requestSize;
            randomFile.seek(start);
            while(needSize > 0){
                int len = randomFile.read(buffer);
                if(needSize < buffer.length){
                    out.write(buffer, 0, needSize);
                } else {
                    out.write(buffer, 0, len);
                    if(len < buffer.length){
                        break;
                    }
                }
                needSize -= buffer.length;
            }
        }catch (Exception e){
            BaseBean basebean = new BaseBean();
            basebean.writeLog(e);
        }finally {
            if(inputStream != null)inputStream.close();
            if(fos != null)fos.close();
            if(randomFile != null)randomFile.close();
            if(out != null)out.close();
            FileSecurityUtil.deleteFile(tempfile);
            //new FileDeleteUtil().deleteFile(tempfile);
        }
    }
    /**
     * ofd下载逻辑
     * @param req
     * @param res
     */
        //本例中传入的url是类似  http://ip:port/DownloadServlet?docId=2&imageFileId=5
    private void downloadOfd(HttpServletRequest req, HttpServletResponse res, User user) {
        //本例中传入的url是类似  http://ip:port/DownloadServlet?docId=2&imageFileId=5
        int docId= Util.getIntValue(req.getParameter("docId"),-1);
        int imageFileId= Util.getIntValue(req.getParameter("imageFileId"),-1);
        baseBean.writeLog("downloadOfd docId="+docId+"###imageFileId="+imageFileId);
        if (imageFileId <= 0) return;
        if(!DocUtil.hasPemissionDownload(user, imageFileId)){
            baseBean.writeLog("---------- downloadOfd 无下载权限---------imageFileId=" + imageFileId);
            return;
        }
        InputStream is = null;
        BufferedOutputStream bos = null;
        try{
            ImageFileManager imageFileManager=new ImageFileManager();
            imageFileManager.getImageFileInfoById(imageFileId);
            res.setStatus(200);
            res.setContentType("APPLICATION/OCTET-STREAM; charset=UTF-8");
            res.setHeader("Content-Disposition", "attachment; filename="+ new String(imageFileManager.getImageFileName().replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", "").getBytes("UTF-8"),"ISO-8859-1")+"");
            res.setContentType("application/ofd");
            is = imageFileManager.getInputStream();
            bos = new BufferedOutputStream(res.getOutputStream());
            byte[] buf = new byte[BUFFER_SIZE];
            int len = -1;
            while ((len = is.read(buf)) != -1){
                bos.write(buf, 0, len);
            }
            bos.flush();
        }catch(IOException exp){
            exp.printStackTrace();
        }finally{
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (bos != null)
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
    
    private void downloadForEmaill(HttpServletRequest req, HttpServletResponse res){
    	//int fileid = Util.getIntValue(req.getParameter("fileid"));
		  int fileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileid(req), -1);
    	MailFilePreviewService mfps = new MailFilePreviewService();
		InputStream imagefile = mfps.getInputStreamByMailFileId(fileid + "");
		
		String filename = Util.null2String(mfps.getFileNameOnly(fileid + ""));
		ServletOutputStream out = null;
		if(imagefile != null){
			try {
	            out = res.getOutputStream();
	            res.setContentType("application/octet-stream");
	            res.setHeader("content-disposition", "attachment; filename=\"" +
                        URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")") + "\"");
	            int byteread;
	            byte data[] = new byte[1024];
	            while ((byteread = imagefile.read(data)) != -1) {
	                out.write(data, 0, byteread);
	                out.flush();
	            }
	        } catch (Exception e) {
	            new BaseBean().writeLog(e);
	        } finally {
	        	if(out != null){
		        	try{
		        		out.close();
		        	}catch(Exception e){
		        		 new BaseBean().writeLog(e);
		        	}
	        	}
	            if(imagefile != null){
	                try{
	                    imagefile.close();
	                }catch(Exception e){
	                    new BaseBean().writeLog(e);
	                }
	            }
	        }
		}
    }

    private void downloadForWebOffice(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
    	User user = (User)req.getSession(true).getAttribute("weaver_user@bean");
    	String fileid = Util.null2String(req.getParameter("fileid"));
    	
    	if(user == null){
    		res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=4&user=null");
            return;
    	}
    	
    	RecordSet rs = new RecordSet();
		RecordSet rsdoc = new RecordSet();
    	rs.executeQuery("select a.id,b.docid,b.imagefilename from DocWebOffice a,DocImageFile b where a.fileid=? and a.imagefileid=b.imagefileid",fileid);
    	if(rs.next()){
    		
    		String docid = rs.getString("docid");
    		
    		 DocCoopereateUtil dcu = new DocCoopereateUtil();
        	 DocViewPermission dvps = new DocViewPermission();
        	 Map<String,Boolean> levelMap = dvps.getShareLevel(Util.getIntValue(docid),user,false);
        	 boolean canRead = levelMap.get(DocViewPermission.READ);
        	 boolean canCoope = dcu.jugeUserCoopeRight(docid,user,canRead);
        	if(!canCoope){
        		res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=3&fileid=" + fileid);
	             return;
        	}
    		
    		Map<String,String> dataMap = new HashMap<String,String>();
    		InputStream in = new WebOfficeServiceImpl().getInputStream(rs.getInt("id"),dataMap);
    		
    		 ServletOutputStream out = null;
			if(in != null){
				rsdoc.executeQuery("select docsubject from docdetail where id = "+docid);
				rsdoc.next();
				String docsubject = Util.null2String(rsdoc.getString("docsubject"));
				String imagefilename = Util.null2String(rs.getString("imagefilename"));
				String extname = imagefilename.contains(".")? imagefilename.substring(imagefilename.lastIndexOf(".") + 1) : "";
				String filename =docsubject+"."+extname;
				if ( (agent.contains(" Chrome") || agent.contains("Safari")) && !agent.contains("Edge")) {
//               res.setHeader("content-disposition", "attachment;");
					res.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(")
							.replaceAll("%29", ")").replaceAll("%7B","{").replaceAll("%7D","}").replaceAll("%5B","[").replaceAll("%5D","]").replaceAll("%40","@").replaceAll("%23","#").replaceAll("%25","%").replaceAll("%26","&")
							.replaceAll("%2B","+").replaceAll("%27","'").replaceAll("%20"," "));
				}else if(agent.contains("Firefox")){
					res.setHeader("content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
				} else{
					res.setHeader("content-disposition", "attachment; filename=\"" +
							URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")") + "\"");
				}
				try {
		            out = res.getOutputStream();
		            res.setContentType("application/octet-stream");
		            int byteread;
		            byte data[] = new byte[1024];
		            while ((byteread = in.read(data)) != -1) {
		                out.write(data, 0, byteread);
		                out.flush();
		            }
		        } catch (Exception e) {
		            new BaseBean().writeLog(e);
		        } finally {
		        	if(out != null){
			        	try{
			        		out.close();
			        	}catch(Exception e){
			        		 new BaseBean().writeLog(e);
			        	}
		        	}
		            if(in != null){
		                try{
		                    in.close();
		                }catch(Exception e){
		                    new BaseBean().writeLog(e);
		                }
		            }
		        }
			}else{
				 res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=2&fileid=" + fileid);
	             return;
			}
    	}else{
    		 res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=1&fileid=" + fileid);
             return;
    	}
    	
    }

    private void downloadForDoccenterCoop(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
        User user = (User)req.getSession(true).getAttribute("weaver_user@bean");
        String fileid = Util.null2String(req.getParameter("fileid"));
        String fromfileid = Util.null2String(req.getParameter("fromfileid"));
        String wpsFileid = Util.null2String(req.getParameter("wpsFileid"));

        if(user == null){
            res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=4&user=null");
            return;
        }

        if(fromfileid == null || fromfileid.isEmpty()){
            res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?fromid=null");
            return;
        }

        String docid = "";
        int fromecfileid = Util.getIntValue(fileid);
        int ecfileid = 0;
        String mould = "";
        String ecFileName = "";

        RecordSet rs = new RecordSet();
        String sql = "select * from wps_doccenter_fileinfo where wpsfileid = ? and fromecfileid = ? order by versionid desc";
        rs.executeQuery(sql, wpsFileid, fromfileid);
        if(rs.next()){
            fromecfileid = rs.getInt("fromecfileid");
            ecfileid = rs.getInt("ecfileid");
            mould = Util.null2String(rs.getString("mould"));
            ecFileName = Util.null2String(rs.getString("filename"));
        } else {
            ecfileid = fromecfileid;
            mould = "ecology";
        }

        sql = "select docid, imagefilename from docimagefile where imagefileid = ?";
        rs.executeQuery(sql,fromecfileid);
        if(!rs.next()) {
            res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=1&wpsFileid=" + wpsFileid);
            return;
        }
        docid = rs.getString("docid");
        ecFileName = Util.null2String(rs.getString("imagefilename"));

        DocCoopereateUtil dcu = new DocCoopereateUtil();
        DocViewPermission dvps = new DocViewPermission();
        Map<String,Boolean> levelMap = dvps.getShareLevel(Util.getIntValue(docid),user,false);
        boolean canRead = levelMap.get(DocViewPermission.READ);
        boolean canCoope = dcu.jugeUserCoopeRight(docid,user,canRead);
        if(!canCoope){
            res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=3&fileid=" + wpsFileid);
            return;
        }

        Map<String,String> dataMap = new HashMap<String,String>();
        InputStream in = new weaver.wps.doccenter.utils.FileInfoUtil().getInputStream(ecfileid, mould);

        ServletOutputStream out = null;
        if(null == in) {
            res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?line=2&fileid=" + wpsFileid);
            return;
        }

        rs.executeQuery("select docsubject from docdetail where id = "+docid);
        rs.next();
        String docsubject = Util.null2String(rs.getString("docsubject"));
        String extname = ecFileName.contains(".")? ecFileName.substring(ecFileName.lastIndexOf(".") + 1) : "";
        String filename =docsubject+"."+extname;
        if ( (agent.contains(" Chrome") || agent.contains("Safari")) && !agent.contains("Edge")) {
//               res.setHeader("content-disposition", "attachment;");
            res.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(")
                    .replaceAll("%29", ")").replaceAll("%7B","{").replaceAll("%7D","}").replaceAll("%5B","[").replaceAll("%5D","]").replaceAll("%40","@").replaceAll("%23","#").replaceAll("%25","%").replaceAll("%26","&")
                    .replaceAll("%2B","+").replaceAll("%27","'").replaceAll("%20"," "));
        }else if(agent.contains("Firefox")){
            res.setHeader("content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
        } else{
            res.setHeader("content-disposition", "attachment; filename=\"" +
                    URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")") + "\"");
        }
        try {
            out = res.getOutputStream();
            res.setContentType("application/octet-stream");
            int byteread;
            byte data[] = new byte[1024];
            while ((byteread = in.read(data)) != -1) {
                out.write(data, 0, byteread);
                out.flush();
            }
        } catch (Exception e) {
            rs.writeLog(FileDownload.class.getName(), e);
        } finally {
            Tools.cloze(out);
            Tools.cloze(in);
        }
    }
    /**
     * 文档下载
     * @param req
     * @param res
     */
    private void downloadForDocument(HttpServletRequest req, HttpServletResponse res,User user){
        //int fileid = Util.getIntValue(req.getParameter("fileid"));
        int fileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileid(req), -1);
        int isofficeview = Util.getIntValue(Util.null2String(req.getParameter("isofficeview")), -1);
        baseBean.writeLog("---------- downloadForDocument ---------fileid=" + fileid);
        if(fileid <= 0) return;
        if(!DocUtil.hasPemissionDownload(user, fileid)){
            baseBean.writeLog("---------- downloadForDocument 无下载权限---------fileid=" + fileid);
            return;
        }
        InputStream imagefile = null;
        try {
            ImageFileManager ifm = new ImageFileManager();
            ifm.getImageFileInfoById(fileid);
            imagefile = ifm.getInputStream();
            String filename = ifm.getImageFileName();
            String contenttype = "";
            if(filename.toLowerCase().endsWith(".gif")) {
                contenttype = "image/gif";
                res.addHeader("Cache-Control", "private, max-age=8640000");
            }else if(filename.toLowerCase().endsWith(".png")) {
                contenttype = "image/png";
                res.addHeader("Cache-Control", "private, max-age=8640000");
            }else if(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                contenttype = "image/jpg";
                res.addHeader("Cache-Control", "private, max-age=8640000");
            }else if(filename.toLowerCase().endsWith(".bmp")) {
                contenttype = "image/bmp";
                res.addHeader("Cache-Control", "private, max-age=8640000");
            }
            if(isofficeview==1){
                String extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                Map<String,Object> secWmSetMap = WaterMarkUtil.getCategoryWmSet(fileid+"");
                if("1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYVIEW))&& "1".equals(secWmSetMap.get(WaterMarkUtil.SECCATEGORYWMISOPEN)) && "0".equals(secWmSetMap.get(WaterMarkUtil.WATERCONTENTISNULL))){
//                    imagefile = WaterMarkUtil.takefileWater(imagefile,user,filename,fileid,extName,WaterMarkUtil.MOULDDOC);
                    imagefile = FileWaterManager.takewater(req,fileid+"",filename,extName,imagefile, WaterMarkUtil.MOULDDOC,-1);
                }
            }
            ServletOutputStream out = res.getOutputStream();
            res.setContentType(contenttype);
            res.setHeader("content-disposition", "attachment; filename=\"" +  new String(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", "").getBytes("UTF-8"),"ISO-8859-1")+"\"");
            int byteread;
            byte data[] = new byte[1024];
            while ((byteread = imagefile.read(data)) != -1) {
                out.write(data, 0, byteread);
                out.flush();
            }
            out.flush();
            out.close();
        } catch (Exception e) {
            new BaseBean().writeLog(e);
        } finally {
            if(imagefile != null){
                try{
                    imagefile.close();
                }catch(Exception e){
                    new BaseBean().writeLog(e);
                }
            }
        }
    }

    //模板下载
    private void downloadMould(HttpServletRequest req, HttpServletResponse res,String type){
        //int mouldid = Util.getIntValue(req.getParameter("fileid"));
        int mouldid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileid(req), -1);
        if(mouldid <= 0) return;
        String mouldName = "";
        String mouldPath = "";
        String tableName = "";
        String mouldtype = "";
        int imagefileid = 0;
        if("showMould".equals(type)){
            tableName = "docMould";
        } else if ("printMould".equals(type)){
            tableName = "OdocPrintMould";
        }else{
            tableName = "DocMouldFile";
        }
        RecordSet rs = new RecordSet();
        rs.executeQuery("select mouldName,mouldPath,imagefileid,mouldtype from "+ tableName +" where id=?",mouldid);
        if(rs.next()){
            mouldName = rs.getString("mouldName");
            mouldPath = rs.getString("mouldPath");
            imagefileid = rs.getInt("imagefileid");
            mouldtype = rs.getString("mouldtype");
        }else{
            return;
        }
        
        if ("printMould".equals(type) && "2".equals(mouldtype)){
            mouldName += ".docx";
        }else if("2".equals(mouldtype)){
        	mouldName += ".doc";
        }else if("3".equals(mouldtype)){
        	mouldName += ".xls";
        }else if("4".equals(mouldtype)){
        	mouldName += ".wps";
        }
        InputStream is = null;
        try {
        	if(imagefileid > 0){
        		is = ImageFileManager.getInputStreamById(imagefileid);
        	}else{
        		is = new BufferedInputStream(new FileInputStream(mouldPath));
        	}
            res.setContentType("application/octet-stream");
            ServletOutputStream out = res.getOutputStream();
            res.setHeader("content-disposition", "attachment; filename=\"" +  new String(mouldName.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", "").getBytes("UTF-8"),"ISO-8859-1")+"\"");
            int byteread;
            byte data[] = new byte[1024];
            while ((byteread = is.read(data)) != -1) {
                out.write(data, 0, byteread);
                out.flush();
            }
            out.flush();
            out.close();
        } catch (Exception e) {
            new BaseBean().writeLog(e);
        } finally {
            if(is != null){
                try{
                    is.close();
                }catch(Exception e){
                    new BaseBean().writeLog(e);
                }
            }
        }
    }

    /**
     * 交换平台下载逻辑
     * @param req
     * @param res
     */
    private void downloadForExchange(HttpServletRequest req, HttpServletResponse res) {
        String clientEcoding = "GBK";
        try {
            try{
                String acceptlanguage = req.getHeader("Accept-Language");
                if(!"".equals(acceptlanguage))
                    acceptlanguage = acceptlanguage.toLowerCase();
                if(acceptlanguage.indexOf("zh-tw")>-1||acceptlanguage.indexOf("zh-hk")>-1){
                    clientEcoding = "BIG5";
                }else{
                    clientEcoding = "GBK";
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            User user = HrmUserVarify.getUser (req , res) ;
            agent = req.getHeader("user-agent");
			  int fileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileid(req), -1);
            //int fileid = Util.getIntValue(req.getParameter("fileid"), -1);
            int requestid = Util.getIntValue(req.getParameter("requestid"));
            String ipstring = Util.getIpAddr(req);
            Map<String,Object> jsonParams = new HashMap<String,Object>();
            int userid = 0;
            if(fileid <=0 ){
                jsonParams = Json2MapUtil.requestJson2Map(req);
                fileid = Util.getIntValue(Util.null2String(jsonParams.get("fileid")));
                userid = Util.getIntValue(Util.null2String(jsonParams.get("userid")));
                requestid = Util.getIntValue(Util.null2String(jsonParams.get("requestid")));
            }
            if(fileid <= 0){//转化为int型，防止SQL注入
                res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?v6");
                return;
            }
            RecordSet statement = new RecordSet();
            try {
                boolean hasRight=false;
                try{
                    String companyId = getCompanyIdByUser(user);
                    hasRight = isHaveDownloadRight(String.valueOf(fileid),companyId);
                }catch(Exception ex){
                    BaseBean basebean = new BaseBean();
                    basebean.writeLog(ex);
                    hasRight=false;
                }
                if(!hasRight){
                    res.sendRedirect(GCONST.getContextPath()+"/notice/noright.jsp?v7");
                    return;
                }
                String contenttype = "application/octet-stream";
                String filename = "";
                String filerealpath = "";
                String signInfo = "";
                String hashInfo = "";
                String iszip = "";
                String isencrypt = "";
                String isaesencrypt="";
                String aescode = "";
                String tokenKey="";
                String storageStatus = "";
                String comefrom="";
                int byteread;
                byte data[] = new byte[1024];
                String sql = "select t1.imagefilename,t1.filerealpath,t1.signinfo,t1.hashinfo,t1.iszip,t1.isencrypt,t1.imagefiletype , t1.imagefileid, t1.imagefile,t1.isaesencrypt,t1.aescode,t2.imagefilename as realname,t1.TokenKey,t1.StorageStatus,t1.comefrom from ImageFile t1 left join DocImageFile t2 on t1.imagefileid = t2.imagefileid where t1.imagefileid = "+fileid;
                String extName = "";
                statement.execute(sql);
                if (statement.next()) {
                    filename = Util.null2String(statement.getString("imagefilename"));
                    filerealpath = Util.null2String(statement.getString("filerealpath"));
                    signInfo = Util.null2String(statement.getString("signinfo"));
                    hashInfo = Util.null2String(statement.getString("hashinfo"));
                    iszip = Util.null2String(statement.getString("iszip"));
                    isencrypt = Util.null2String(statement.getString("isencrypt"));
                    isaesencrypt = Util.null2o(statement.getString("isaesencrypt"));
                    aescode = Util.null2String(statement.getString("aescode"));
                    tokenKey = Util.null2String(statement.getString("TokenKey"));
                    storageStatus = Util.null2String(statement.getString("StorageStatus"));
                    comefrom = Util.null2String(statement.getString("comefrom"));
                    try {
                        if((agent.contains("Firefox")||agent.contains(" Chrome")||agent.contains("Safari") )&& !agent.contains("Edge")){
                            res.setHeader("content-disposition", "attachment; filename*=UTF-8''" +  URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
                        }else{
                            res.setHeader("content-disposition", "attachment; filename=\"" +
                                    URLEncoder.encode(filename.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")")+"\"");
                        }
                    } catch (Exception ecode) {
                        ecode.printStackTrace();
                    }
                    InputStream imagefile = null;
                    ZipInputStream zin = null;
                    File thefile = new File(filerealpath);
                    boolean signResult = false;
                    if (!filerealpath.isEmpty() && !signInfo.isEmpty() && !hashInfo.isEmpty()) {
                        signResult = com.api.doc.util.DocEncryptUtil.verifyFile(hashInfo, signInfo, filerealpath);
                        // 附件下载，开始调用密码机解密
                        if (!signResult ) {
                            res.sendRedirect(GCONST.getContextPath()+"/wui/common/page/integrityFailure.jsp?line=4&user=null");
                            return;
                        }
                    }
                    if (iszip.equals("1")) {
                        zin = new ZipInputStream(new FileInputStream(thefile));
                        if (zin.getNextEntry() != null) imagefile = new BufferedInputStream(zin);
                    } else{
                        imagefile = new BufferedInputStream(new FileInputStream(thefile));
                    }
                    ServletOutputStream out = null;
                    try {
                        out = res.getOutputStream();
                        res.setContentType(contenttype);
                        if(isaesencrypt.equals("1")){
                            imagefile = AESCoder.decrypt(imagefile, aescode);
                        }
                        // 签名值校验通过，开始进行解密操作
                        if (signResult) {
                            imagefile = com.api.doc.util.DocEncryptUtil.decryptInput(imagefile);
                            if (imagefile == null) {
                                res.sendRedirect(GCONST.getContextPath()+"/wui/common/page/integrityFailure.jsp?line=4&user=null");
                                return;
                            }
                        }
                        while ((byteread = imagefile.read(data)) != -1) {
                            out.write(data, 0, byteread);
                            out.flush();
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }finally {
                        if(imagefile!=null) imagefile.close();
                        if(zin!=null) zin.close();
                        if(out!=null) out.flush();
                        if(out!=null) out.close();
                    }
                    countDownloads(""+fileid);
                }
            } catch (Exception e) {
                BaseBean basebean = new BaseBean();
                basebean.writeLog(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 记录下载次数，根据传入的参数 countdownloads == 1
     *
     */
    private void countDownloads(String fileid) {
        if (this.isCountDownloads) {

            RecordSet rs = new RecordSet();
            String sqlStr = "UPDATE ImageFile Set downloads=downloads+1 WHERE imagefileid = " + fileid;
            //System.out.println("sqlStr ==" + sqlStr);
            rs.execute(sqlStr);
        }
    }

    /**
     * 记录下载日志
     * @param userid 下载的用户id
     * @param userName 下载的用户名称
     * @param imageid 下载的文档id
     * @param imageName 下载的文档文件名称
     */
    private void downloadLog(int userid, String userName, int imageid, String imageName,String ipstring) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(new Date());  
        User user = new User(userid);
        RecordSet rs = new RecordSet();
        String sql = "select t2.id,t2.docsubject,t1.imagefilename from DocImageFile t1, DocDetail t2 where t1.docid=t2.id and t1.docfiletype<>1 and t1.imagefileid = "+imageid;
        int docid = -1;
        String docName = "";
        rs.executeSql(sql);
        if(rs.next()){
            docid = rs.getInt(1);
            docName = rs.getString(2);
			imageName = Util.null2String(rs.getString("imagefilename"));
			userid = new DocViewPermission().getUserid(docid+"",user);
            if(userid<=0){
                userid = user.getUID();
            }
            sql = "insert into DownloadLog(userid, username, downloadtime, imageid, imagename, docid, docname,clientaddress) values(" + userid + ",'" + Util.toHtml100(userName) + "','" + time + "'," + imageid + ",'" + Util.toHtml100(imageName) + "',"+docid+",'"+Util.toHtml100(docName)+"','"+ipstring+"')";
            rs.executeSql(sql);
        }

        DocDetailService dbs = new DocDetailService();
		dbs.resizeCount(docid, DocDetailService.DOWNLOAD_COUNT);

    }
    
    /**
     * 判断是否有下载特定文件的权限
     * @param fileId 文件id
     * @param req 请求
     * @param res 响应
     * 
     * @return boolean  true:有下载的权限  false:没有下载的权限
     */
    private boolean getWhetherHasRight(String fileId,HttpServletRequest req, HttpServletResponse res,int requestid,boolean fromMobile,Map<String,Object> jsonParams) throws Exception  {
        toWriteLog("weaver-->2176-->fileid"+fileId+"--->ishr"+fileId);
        toWriteLog("weaver-->2176-->fileid"+fileId+"--->requestid"+requestid);
        toWriteLog("weaver-->2176-->fileid"+fileId+"--->fromMobile"+fromMobile);
        toWriteLog("weaver-->2176-->fileid"+fileId+"--->jsonParams"+jsonParams);
    	jsonParams = jsonParams == null ? new HashMap<String,Object>() : jsonParams;
        //0:查看
        boolean canReader = false;
        String fileid_head = fileId;
    	int _userid = 0;
    	String groupid = "";
    	if(fromMobile){
    		_userid = Util.getIntValue(Util.null2String(jsonParams.get("userid")));
    		groupid = Util.null2String(jsonParams.get("groupid"));
    	}
        String download = Util.null2String(jsonParams.get("download"));
    	User user = new User(_userid);
    	if(user==null){
    	    user = (User)req.getSession(true).getAttribute("weaver_user@bean") ;
        }
        //安全性检查
        if(fileId==null||fileId.trim().equals("")){
            return false;
        }

        RecordSet rs = new RecordSet();
        //是否必须授权     1：是   0或其他：否
        String mustAuth=Util.null2String(rs.getPropValue("FileDownload","mustAuth"));           
        boolean hasRight=false;
//        boolean hasRight=true;
        if(mustAuth.equals("1")){
            hasRight=false;
        }       
        boolean isDocFile=false;
        boolean isHtmlPreview = false;
        //文档模块  附件查看权限控制  开始
        String docId=null;
        boolean isimage=false;
        List docIdList=new ArrayList();
		RecordSet rs2 = new RecordSet();
		rs2.executeSql("select  imagefilename,comefrom from ImageFile where imageFileId="+fileId);
		String extName="";
		boolean isExtfile=false;
        boolean isPdf=false;
		if(rs2.next()){
    		 String  filename = Util.null2String(rs2.getString("imagefilename"));			       	    	
				if(filename.indexOf(".") > -1){
					int bx = filename.lastIndexOf(".");
					if(bx>=0){
						extName = filename.substring(bx+1, filename.length());						
					}
				}
				String htmlcomefrom = Util.null2String(rs2.getString("comefrom"));
               if("WorkflowToDoc".equals(htmlcomefrom)){
                  isHtmlPreview = true;
               }
			   
			    if (filename.toLowerCase().endsWith(".mp4")||filename.toLowerCase().endsWith(".ogg")||filename.toLowerCase().endsWith(".asf")||filename.toLowerCase().endsWith(".mp3")||filename.toLowerCase().endsWith(".wav")||filename.toLowerCase().endsWith(".au")||filename.toLowerCase().endsWith(".wma")) {
                  isHtmlPreview = true;      
                } 
        }

		if( "xls".equalsIgnoreCase(extName)||"xlsx".equalsIgnoreCase(extName) || "doc".equalsIgnoreCase(extName)|| "docx".equalsIgnoreCase(extName)||"wps".equalsIgnoreCase(extName)||"ppt".equalsIgnoreCase(extName)||"pptx".equalsIgnoreCase(extName)) {		
		//isExtfile=true;
		}
        if("pdf".equals(extName.toLowerCase())) isPdf = true;
        String imgExtStr = Util.null2s(baseBean.getPropValue("doc_download_img_ext","doc_img_ext"),"");
        List<String> imgExtList = Arrays.asList(imgExtStr.split(","));
        for(int i=0;i<imgExtList.size();i++){
            String imgExt = imgExtList.get(i).toLowerCase();
            if (imgExt.equals(extName.toLowerCase())){
                isimage = true;
                break;
            }
        }
//        if("gif".equalsIgnoreCase(extName)||"png".equalsIgnoreCase(extName)||"jpg".equalsIgnoreCase(extName)||"jpeg".equalsIgnoreCase(extName)||"bmp".equalsIgnoreCase(extName)||"svg".equalsIgnoreCase(extName)){
//            isimage=true;
//        }
        rs.executeSql("select  docId from docImageFile where imageFileId="+fileId);
        while(rs.next()){
            hasRight=false;
            docId=rs.getString(1);
            if(docId!=null&&!docId.equals("")){
                docIdList.add(docId);
            }
        }
        boolean previewSensitive = false;
        String comefrom="";     
        String originComeFrom="";
        if(docIdList.size()==0){
            int fileId_related=0;
            int docId_related=0;
            boolean hasDocId=false;
            rs.executeSql("select comefrom from ImageFile where imageFileId="+fileId);
            if(rs.next()){
                comefrom=Util.null2String(rs.getString("comefrom"));
            }   
            String comefrom_noNeedLogin=Util.null2String(rs.getPropValue("FileDownload","comefrom_noNeedLogin"));           
            if((","+comefrom_noNeedLogin+",").indexOf(","+comefrom+",")>=0){
                hasRight=true;
                return hasRight;
            }
            if(comefrom.equals("DocPreview")||comefrom.equals("DocPreviewHistory")){
                rs.executeSql("select imageFileId,docId from "+comefrom+"  where (pdfFileId="+fileId+" or swfFileId="+fileId+") order by id desc");
                if(rs.next()){
                    fileId_related=Util.getIntValue(rs.getString("imageFileId"),0);
                    //docId_related=Util.getIntValue(rs.getString("docId"),0);                
                }
                if(docId_related>0){
                    docIdList.add(""+docId_related);
                    hasDocId=true;
                }
            }else if(comefrom.equals("DocPreviewHtml")||comefrom.equals("DocPreviewHtmlHistory")){
                rs.executeSql("select imageFileId,docId from "+comefrom+"  where  htmlFileId="+fileId+" order by id desc");
                if(rs.next()){
                    fileId_related=Util.getIntValue(rs.getString("imageFileId"),0);
                   // docId_related=Util.getIntValue(rs.getString("docId"),0);                
                }
                if(docId_related>0){
                    docIdList.add(""+docId_related);
                    hasDocId=true;
                }               
                isHtmlPreview = true;
            }else if(comefrom.equals("DocPreviewHtmlImage")){
                rs.executeSql("select imageFileId,docId from DocPreviewHtmlImage  where  picFileId="+fileId+"  order by id desc");
                if(rs.next()){
                    fileId_related=Util.getIntValue(rs.getString("imageFileId"),0);
                    //docId_related=Util.getIntValue(rs.getString("docId"),0);                
                }
                if(docId_related>0){
                    docIdList.add(""+docId_related);
                    hasDocId=true;
                }                   
            }
			// 处理敏感词高亮
            if(isHtmlPreview && fileId_related > 0) {
                int originalFileId = -1;
                String querySql = "select original_fileid from imagefile where IMAGEFILEID = " + fileId_related +" and sensitive_wtype = 1";
                rs.executeQuery(querySql);
                if(rs.next()) {
                    originalFileId = rs.getInt(1);
                }
                if(originalFileId > 0) {
                    fileId_related = originalFileId;
                    previewSensitive = true;
                }
            }
            if(!hasDocId&&fileId_related>0){
                rs.executeSql("select distinct  docId from docImageFile where imageFileId="+fileId_related);
                while(rs.next()){
                    docId=rs.getString(1);
                    if(docId!=null&&!docId.equals("")){
                        docIdList.add(docId);
                    }
                }               
            }
            
            if(fileId_related > 0){
                originComeFrom = comefrom;
            	 rs.executeSql("select comefrom from ImageFile where imageFileId="+fileId_related);
                 if(rs.next()){
                     comefrom=Util.null2String(rs.getString("comefrom"));
                 }
                 fileId = fileId_related + "";
            }
            
            if(!hasDocId && fileId_related == 0){
            	extName = extName.toLowerCase();
            	if(extName.equals("jpg") || 
            			extName.equals("jpeg") || 
            			extName.equals("png") || 
            			extName.equals("gif") ||
            			extName.equals("bmp")){
            		
            		int _docid = Util.getIntValue(req.getParameter("docid"));
            		if(_docid > 0){
            			rs.executeQuery("select id from DocDetail where id=? and themeshowpic=?",_docid,fileId);
	            		if(rs.next()){
	            			docIdList.add(rs.getString("id"));
	            		}
            		}
            	}
            }

        }
        if(docIdList.size()>0){
            hasRight=false;         
        }
        String mustLogin=Util.null2String(rs.getPropValue("FileDownload","mustLogin"));
        int votingId=Util.getIntValue(req.getParameter("votingId"),0);
        if(fromMobile && votingId<=0){
        	votingId = Util.getIntValue(Util.null2String(jsonParams.get("votingId")),0);
        }

        if(user==null && fromMobile){
        	user = new User(_userid);
        }
        
        if(user==null){
            if(mustLogin.equals("1")){
                hasRight=false; 
            }
            return hasRight;
        }
        if(!fromMobile){
	        String f_weaver_belongto_userid=Util.null2String(req.getParameter("f_weaver_belongto_userid"));//需要增加的代码
	        String f_weaver_belongto_usertype=Util.null2String(req.getParameter("f_weaver_belongto_usertype"));//需要增加的代码
	        user = HrmUserVarify.getUser(req, res, f_weaver_belongto_userid, f_weaver_belongto_usertype) ;//需要增加的代码
        }
        String workplanid=Util.null2String(req.getParameter("workplanid"));
        if(fromMobile){
        	workplanid = Util.null2String(jsonParams.get("workplanid"));
        }
        
        if(user==null){
            user = (User)req.getSession(true).getAttribute("weaver_user@bean") ;
        }
        if(user==null){
            if(mustLogin.equals("1")){
                hasRight=false; 
            }           
            return hasRight;
        }
        String comefrom_noNeedAuth=Util.null2String(rs.getPropValue("FileDownload","comefrom_noNeedAuth"));
        if((","+comefrom_noNeedAuth+",").indexOf(","+comefrom+",")>=0){
            hasRight=true;
            return hasRight;
        }       
    	if(comefrom.equals("VotingAttachment")){  //
            VotingManager votingManager = new VotingManager();
            return votingManager.hasRightByFileid(votingId, Util.getIntValue(fileId,0), user);
        }
        
        DocManager docManager=new DocManager();
        String docStatus="";
        int isHistory=0;
        int secCategory=0;
        String docPublishType="";//文档发布类型  1:正常(不发布)  2:新闻  3:标题新闻
        boolean useNewRightCheck = 1 == Util.getIntValue(baseBean.getPropValue("FileDownload","useNewRightCheck"),1);
        DocViewPermission dvp = new DocViewPermission();
        for(int i=0;i<docIdList.size()&&!hasRight;i++){
            docId=(String)docIdList.get(i);

            isDocFile=true;

            if(docId==null||docId.trim().equals("")){
                continue;
            }
            docManager.resetParameter();
            docManager.setId(Integer.parseInt(docId));
            docManager.getDocInfoById();

            docStatus=docManager.getDocstatus();
            isHistory = docManager.getIsHistory();
            secCategory=docManager.getSeccategory();
            docPublishType=docManager.getDocpublishtype();

            if(docPublishType!=null&&(docPublishType.equals("2")||docPublishType.equals("3"))){
                String newsClause="";
                String sqlDocExist=" select 1 from DocDetail where id="+docId+" ";
                String sqlNewsClauseOr="";
                boolean hasOuterNews=false;

                rs.executeSql("select newsClause from DocFrontPage where publishType='0'");
                while(rs.next()){
                    hasOuterNews=true;
                    newsClause=Util.null2String(rs.getString("newsClause"));
                    if (newsClause.equals("0"))
                    {
                        //newsClause=" 1=1 ";
                        hasRight=true;
                        return hasRight;
                    }
                    if(!newsClause.trim().equals("")){
                        //sqlDocExist+=" and "+newsClause;
                        sqlNewsClauseOr+=" ^_^ ("+newsClause+")";
                    }
                }
                ArrayList newsArr = new ArrayList();
                if(!sqlNewsClauseOr.equals("")&&!hasRight){
                    //sqlNewsClauseOr=sqlNewsClauseOr.substring(sqlNewsClauseOr.indexOf("("));
                    //sqlDocExist+=" and ("+sqlNewsClauseOr+") ";
                    String[] newsPage = Util.TokenizerString2(sqlNewsClauseOr,"^_^");
                    int k = 0;
                    String newsWhere = "";
                    for(;k<newsPage.length;k++){
                        if(k%10==0){
                            newsArr.add(newsWhere);
                            newsWhere="";
                            newsWhere+=newsPage[k];
                        }else
                            newsWhere+=" or "+newsPage[k];
                    }
                    newsArr.add(newsWhere);
                }
                //System.out.print(sqlDocExist);
                if(hasOuterNews&&!hasRight){
                    for(int j=1;j<newsArr.size();j++){
                        String newsp = newsArr.get(j).toString();
                        if(j==1)
                            newsp = newsp.substring(newsp.indexOf("or")+2);
                        sqlDocExist+="and("+newsp+")";
                        rs.executeSql(sqlDocExist);
                        sqlDocExist = " select 1 from DocDetail where id="+docId+" ";
                        if(rs.next()){
                            hasRight=false;
                            break;
                        }
                    }
                }

            }
            if(user==null){
                continue;
            }
            String userId=""+user.getUID();
            String loginType = user.getLogintype();

            if(useNewRightCheck) {
                Map<String, Boolean> shareLevelMap = dvp.getShareLevel(Util.getIntValue(docId), user, false);
                canReader = shareLevelMap.get(DocViewPermission.READ);
                hasRight = shareLevelMap.get(DocViewPermission.DOWNLOAD);
            } else {
                String userSeclevel = user.getSeclevel();
                String userType = ""+user.getType();
                String userDepartment = ""+user.getUserDepartment();
                String userSubComany = ""+user.getUserSubCompany1();
                String userInfo=loginType+"_"+userId+"_"+userSeclevel+"_"+userType+"_"+userDepartment+"_"+userSubComany;

                ArrayList PdocList = null;

                SpopForDoc  spopForDoc=new SpopForDoc();
                PdocList = spopForDoc.getDocOpratePopedom(""+docId,userInfo);

                SecCategoryComInfo secCategoryComInfo=new SecCategoryComInfo();

                //1:编辑
                boolean canEdit = false;
                //5:下载
                if (((String)PdocList.get(0)).equals("true")) {canReader = true ;}
                if (((String)PdocList.get(1)).equals("true")) {canEdit = true ;}
                if (((String)PdocList.get(5)).equals("true")) {hasRight = true ;}//TD12005

                String readerCanViewHistoryEdition=secCategoryComInfo.isReaderCanViewHistoryEdition(secCategory)?"1":"0";

                if(canReader && ((!docStatus.equals("7")&&!docStatus.equals("8"))
                        ||(docStatus.equals("7")&&isHistory==1&&readerCanViewHistoryEdition.equals("1"))
                )){
                    canReader = true;
                }else{
                    canReader = false;
                }

                if(isHistory==1) {
                    if(secCategoryComInfo.isReaderCanViewHistoryEdition(secCategory)){
                        if(canReader && !canEdit) canReader = true;
                    } else {
                        if(canReader && !canEdit) canReader = false;
                    }
                }

                if(canEdit && ((docStatus.equals("3") || docStatus.equals("5") || docStatus.equals("6") || docStatus.equals("7")) || isHistory==1)) {
                    canEdit = false;
                    canReader = true;
                }

                if(canEdit && (docStatus.equals("0") || docStatus.equals("1") || docStatus.equals("2") || docStatus.equals("7")) && (isHistory!=1))
                    canEdit = true;
                else
                    canEdit = false;
            }

            if(previewSensitive && (canReader || hasRight)) {
                return true;
            }
            if((isPdf||isHtmlPreview) && !"1".equals(download) && !hasRight && canReader){
                hasRight = true;
            }
            if(canReader){
                //String referer = req.getHeader("Referer");
                //String ismobile=req.getHeader("User-Agent");
                if(isimage){
                    hasRight=true;
                }
            }
            if(hasRight) {
                return hasRight;
            }
            //E9流程判断
            String authStr = Util.null2String(req.getParameter("authStr"));
            String authSignatureStr = Util.null2String(req.getParameter("authSignatureStr"));
            toWriteLog("weaver-->2505-->fileid"+fileId+"--->hasRight"+hasRight);
            toWriteLog("weaver-->2505-->fileid"+fileId+"--->canReader"+canReader);
            toWriteLog("weaver-->2505-->fileid"+fileId+"--->isExtfile"+isExtfile);
            toWriteLog("weaver-->2505-->fileid"+fileId+"--->authStr"+authStr);
            toWriteLog("weaver-->2505-->fileid"+fileId+"--->authSignatureStr"+authSignatureStr);

			if(!canReader && ((!authStr.isEmpty() && !authSignatureStr.isEmpty()) || requestid>0)){
        		RequestAuthenticationService e9wf = new RequestAuthenticationService();
        		e9wf.setUser(user);
        		e9wf.setAuthResouceType(1);
        		e9wf.setAuthResouceId(docId);
        		hasRight = e9wf.verify(req, requestid);
        		if(!hasRight){
        			rs.writeLog("^^^^^^ E9流程判断附件下载没权限(" + fileId + ")("+docId+")^^^^^^^^requestid=" +
        					requestid + ",authStr=" + authStr + ",authSignatureStr=" + authSignatureStr);
					Map<String, String> otherParams = new HashMap<String, String>();
					otherParams.put("requestid", requestid + "");
					otherParams.put("ismonitor", 1 + "");
					hasRight = e9wf.getRequestMonitorRight(otherParams,requestid);
					if(!hasRight){
						rs.writeLog("^^^^^^ E9流程判断流程监控附件下载没权限(" + fileId + ")("+docId+")^^^^^^^^requestid=" + 
        					requestid + ",authStr=" + authStr + ",authSignatureStr=" + authSignatureStr);
					}
        		}
        	}

            if(!canReader&&!hasRight)  {//如果没有查看权限，判断是否通过协作区赋权
                int desrequestid = Util.getIntValue(req.getParameter("desrequestid"));
                int wfdesrequestid = Util.getIntValue(String.valueOf(req.getSession().getAttribute("desrequestid")),0);
                //System.out.println("wfdesrequestid = "+wfdesrequestid);
                int coworkid = Util.getIntValue(req.getParameter("coworkid"));
                if(fromMobile){
                	desrequestid = Util.getIntValue(Util.null2String(jsonParams.get("desrequestid")));
                	wfdesrequestid = Util.getIntValue( Util.null2String(jsonParams.get("desrequestid")));
                	coworkid = Util.getIntValue( Util.null2String(jsonParams.get("coworkid")));
                }
                CoworkDAO coworkDAO=new CoworkDAO(coworkid);
                VotingManager votingManager=new VotingManager();
                Map parameterMap=new HashMap();
                    parameterMap.put("docId",Util.getIntValue(docId));
                    parameterMap.put("votingId",votingId);
                    parameterMap.put("userId",user.getUID());
                //微博下载权限
                BlogDao blogDao=new BlogDao();
                int blogDiscussid = Util.getIntValue(req.getParameter("blogDiscussid"),0);
                if(fromMobile && blogDiscussid<=0){
                	blogDiscussid = Util.getIntValue( Util.null2String(jsonParams.get("blogDiscussid")));
                }
                /*WFUrgerManager wfum=new WFUrgerManager();
                RequestAnnexUpload rau=new RequestAnnexUpload();
                if (!wfum.OperHaveDocViewRight(requestid,user.getUID(),Util.getIntValue(loginType,1),""+docId)
                  &&!wfum.OperHaveDocViewRight(requestid,desrequestid,Util.getIntValue(userId),Util.getIntValue(loginType),""+docId)
                  &&!wfum.getWFShareDesRight(requestid,wfdesrequestid,user,Util.getIntValue(loginType),""+docId)
                  &&!wfum.getWFChatShareRight(requestid,Util.getIntValue(userId),Util.getIntValue(loginType),""+docId)
                  &&!wfum.UrgerHaveDocViewRight(requestid,Util.getIntValue(userId),Util.getIntValue(loginType),""+docId)
                  &&!wfum.getMonitorViewObjRight(requestid,Util.getIntValue(userId),""+docId,"0")
                  &&!wfum.getWFShareViewObjRight(requestid,user,""+docId,"0")
                  &&!rau.HaveAnnexDocViewRight(requestid,Util.getIntValue(userId),Util.getIntValue(loginType),Util.getIntValue(docId))
                  &&!coworkDAO.haveRightToViewDoc(userId,docId)&&!votingManager.haveViewVotingDocRight(parameterMap)
                  &&!blogDao.appViewRight("doc",userId,Util.getIntValue(docId,0),blogDiscussid)){
                    hasRight=false;
                } else {
                    hasRight = true ;
    			}*/
                if(!coworkDAO.haveRightToViewDoc(userId,docId)&&!votingManager.haveViewVotingDocRight(parameterMap)
                  &&!blogDao.appViewRight("doc",userId,Util.getIntValue(docId,0),blogDiscussid)){
                    hasRight=false;
                } else {
                    hasRight = true ;
    			}
    		}
            if(!canReader&&!hasRight)  {//如果没有查看权限，判断是否通过会议赋权
                int meetingid = Util.getIntValue(req.getParameter("meetingid"));
                MeetingUtil MeetingUtil = new MeetingUtil();
                if (meetingid>0){
                    hasRight=MeetingUtil.UrgerHaveMeetingDocViewRight(meetingid+"",user,Util.getIntValue(loginType),""+docId);
                } else{
                    hasRight =MeetingUtil.UrgerHaveMeetingDocViewRight(user,Util.getIntValue(loginType),""+docId);
    			}
    		}

            if(!canReader&&!hasRight && !workplanid.equals(""))  {//如果没有查看权限，判断是否通过日程赋权
                WorkPlanService workPlanService = new WorkPlanService();
                if (!workPlanService.UrgerHaveWorkplanDocViewRight(workplanid,user,Util.getIntValue(loginType),""+docId)){
                    hasRight=false;
                } else {
                    hasRight = true ;
    			}
    		}
            //判断是否计划任务赋权
            String fromworktask = Util.getFileidIn(Util.null2String(req.getParameter("fromworktask")));
            String operatorid = Util.getFileidIn(Util.null2String(req.getParameter("operatorid")));
            if(fromMobile){
            	fromworktask =  Util.null2String(jsonParams.get("blogDiscussid"));
            	operatorid =  Util.null2String(jsonParams.get("operatorid"));
            }
            if("1".equals(fromworktask)) {
               /* WTRequestUtil WTRequestUtil = new WTRequestUtil();
                if(!canReader&&!hasRight)  {
                    if(!WTRequestUtil.UrgerHaveWorktaskDocViewRight(requestid,Util.getIntValue(userId),Util.getIntValue(docId,0),Util.getIntValue(operatorid,0))) {
                        hasRight=false;
                    } else {
                        hasRight = true ;
                    }
                } else {
                    hasRight=true;
                }*/
            }
            //如果没有权限，看到是否具有客户联系查看权限
            if(!canReader&&!hasRight)  {//如果没有查看权限，判断是否通过会议赋权
                CrmShareBase crmshare = new CrmShareBase();
                String crmid = Util.null2String(req.getParameter("crmid"));
                if(fromMobile){
                	crmid =  Util.null2String(jsonParams.get("crmid"));
                }
                int sharetype = crmshare.getRightLevelForCRM(userId+"",crmid,loginType);
                String crmtype = Util.null2String(req.getParameter("crmtype"));
                if(fromMobile){
                	crmtype =  Util.null2String(jsonParams.get("crmtype"));
                }
                if(crmtype == null || "".equals(crmtype))crmtype = "0";
                if (sharetype > 0 && crmshare.checkCrmFileExist(crmid, fileId, crmtype)){
                    //CoworkDAO coworkDAO = new CoworkDAO();
                    //coworkDAO.shareCoworkRelateddoc(Util.getIntValue(loginType),Util.getIntValue(docId,0),Util.getIntValue(userId));
                    hasRight= true;
                } else {
                    hasRight = false;
    			}
    		}
            // 如果没有下载权限，查找是否协作区附件赋权
            if(!hasRight) {
                int coworkid = Util.getIntValue(req.getParameter("coworkid"));
                if(fromMobile){
                	coworkid =  Util.getIntValue(Util.null2String(jsonParams.get("coworkid")));
                }
                CoworkDAO coworkDAO = new CoworkDAO(coworkid);
                hasRight = coworkDAO.haveRightToViewDoc(userId,docId);
            }
            
            //财务模块 发票影像
            String cwid = req.getParameter("cwid");
            toWriteLog("weaver-->2679-->fileid"+fileId+"--->cwid"+cwid);
            toWriteLog("weaver-->2679-->fileid"+fileId+"--->hasRight"+hasRight);
            if(!hasRight && "fna".equals(cwid)){
            	hasRight = weaver.fna.invoice.utils.ImagePermissionUtil.checkImagePermission(Util.getIntValue(fileId),user);
            	if(!hasRight){
            		rs.writeLog("^^^^^^ 财务模块 发票影像 财务判断附件下载没权限(" + fileId + ")("+docId+")^^^^^^^^");
            	}
            }
            toWriteLog("weaver-->2687-->fileid"+fileId+"--->hasRight"+hasRight);

            //财务模块  微报账规则制度
            if(!hasRight && "fna".equals(cwid)){
                RecordSet rs_ruleSystem = new RecordSet();
                //获取规则制度
                int ruleSystem = 0;
                rs_ruleSystem.executeQuery("select ruleSystem from fnaInvoiceEnterWay ");
                if(rs.next()){
                    ruleSystem = Util.getIntValue(rs_ruleSystem.getString("ruleSystem"),0);
                }
                if(ruleSystem==Util.getIntValue(fileId) && ruleSystem!=0){
                    hasRight = true;
                }
            }
            toWriteLog("weaver-->2892-->fileid"+fileId+"--->hasRight"+hasRight);
            
            
            if(new DocViewPermission().hasRightFromOtherMould(Util.getIntValue(docId),user,req)){
            	hasRight = true;
				if("1".equals(download)){
					hasRight=new DocViewPermission().hasDownRightFromFormMode(Util.getIntValue(docId),user,req);
            }
            }

            if(hasRight){
                return hasRight;
            }
        }
        //文档模块  附件查看权限控制  结束     
        
        if("email".equals(req.getParameter("model"))){  //邮件附件转html

    		boolean emailFlag = new MailFilePreviewService().getViewRightByHtmlCode(user.getUID(),fileId); //fileId

    		if(emailFlag){
    			return true;
    		}else{
    			rs.writeLog("^^^^^^ email判断附件下载没权限(" + fileId + ")^^^^^^^^");
    			return false;
    		}
    	}
        
        
        if(docIdList.size()==0 && "EMforEC".equals(comefrom)){
        	hasRight = false;
        }
        //检查社交平台附加权限
        if(!hasRight && "EMforEC".equals(comefrom)){
        	//hasRight=SocialIMService.checkFileRight(user, fileId, isDocFile, hasRight);
        	if(!groupid.isEmpty()){
        		rs.executeQuery("select groupid from social_IMFileShareGroup where groupid=? and fileid=?",groupid,Util.getIntValue(fileId));
        		if(rs.next()){
        			hasRight = true;
        		}
        	}
        	if(!hasRight){
	        	rs.executeQuery("select id from social_IMFileShare where userid=? and fileid=?",user.getUID(),Util.getIntValue(fileId));
	        	if(rs.next()){
	        		hasRight = true;
	        	}
	        }
        	
        	
        }else if(!hasRight && "EMforECNoRight".equals(comefrom)){
        	hasRight = true;
        }else if(!hasRight){
        	hasRight=SocialIMService.checkFileRight(user, fileId, isDocFile, hasRight);
        }
        
    	if(!hasRight) {//查看是否有建模关联授权
    		//表单建模判断关联授权
    		String formmodeflag = StringHelper.null2String(req.getParameter("formmode_authorize"));
    		Map<String,String> formmodeAuthorizeInfo = new HashMap<String,String>();
    		//formmodeparas+="&formmode_authorize="+formmodeflag;
    		if(formmodeflag.equals("formmode_authorize")){
    			int modeId = 0;
    			int formmodebillId = 0;
    			int fieldid = 0;
    			int formModeReplyid = 0;
    			modeId = Util.getIntValue(req.getParameter("authorizemodeId"),0);
    			formmodebillId = Util.getIntValue(req.getParameter("authorizeformmodebillId"),0);
    			fieldid = Util.getIntValue(req.getParameter("authorizefieldid"),0);
    			formModeReplyid = Util.getIntValue(req.getParameter("authorizeformModeReplyid"),0);
    			String fMReplyFName = Util.null2String(req.getParameter("authorizefMReplyFName"));
    			if(fromMobile && modeId <= 0){
    				modeId =  Util.getIntValue(Util.null2String(jsonParams.get("authorizemodeId")));
    				formmodebillId =  Util.getIntValue(Util.null2String(jsonParams.get("authorizeformmodebillId")));
    				fieldid =  Util.getIntValue(Util.null2String(jsonParams.get("authorizefieldid")));
    				formModeReplyid =  Util.getIntValue(Util.null2String(jsonParams.get("authorizeformModeReplyid")));
    				fMReplyFName =  Util.null2String(jsonParams.get("fMReplyFName"));
                }
    			
    			ModeRightInfo modeRightInfo = new ModeRightInfo();
    			modeRightInfo.setUser(user);
    			if(formModeReplyid!=0){
    				formmodeAuthorizeInfo = modeRightInfo.isFormModeAuthorize(formmodeflag, modeId, formmodebillId, fieldid, Util.getIntValue(docId), formModeReplyid,fMReplyFName);
    			}else{
    				formmodeAuthorizeInfo = modeRightInfo.isFormModeAuthorize(formmodeflag, modeId, formmodebillId, fieldid, Util.getIntValue(docId));
    			}
    		}
    		
    		if("1".equals(formmodeAuthorizeInfo.get("AuthorizeFlag"))){//如果是表单建模的关联授权，那么直接有查看权限
    			hasRight = true;
    		}
    	}

        if (!hasRight) {
            //数据中心查看文档权限
            String edcFlag = StringHelper.null2String(req.getParameter("edc_authorize"));
            if (edcFlag.equals("edc_authorize")) {
                int formid = Util.getIntValue(req.getParameter("authorizeFormId"), 0);
                int billid = Util.getIntValue(req.getParameter("authorizeBillId"), 0);
                int fieldid = Util.getIntValue(req.getParameter("authorizeFieldId"), 0);
                hasRight = JoinCubeBiz.checkDocViewPermission(user, formid, billid, fieldid, Util.getIntValue(docId));
            }
        }

        //建模扩展查看文档权限
        if (!hasRight) {
            String ecmeflag = StringHelper.null2String(req.getParameter("ecme_authorize"));
            if (ecmeflag.equals("ecme_authorize")) {
                int billid = Util.getIntValue(req.getParameter("authorizebillId"), 0);
                int fieldid = Util.getIntValue(req.getParameter("authorizefieldid"), 0);
                int feaId = Util.getIntValue(req.getParameter("authorizefeaId"), 0);
                EcmeRightManager erm = new EcmeRightManager(user,feaId);
                hasRight = erm.ecmeAuthorize( billid, fieldid,Util.getIntValue(docId));
            }
        }

        //财务模块微报账记一笔
        String cwid = req.getParameter("cwid");
        if(!hasRight && "fnaRemember".equals(cwid)){
            int count=0;
            rs.executeQuery("select count(id) cnt from fnaTakeOneNote where imageid=?",fileId);
            if(rs.next()){
                count = Util.getIntValue(rs.getString("cnt"),0);
            }
            if(count>0){
                hasRight = true;
            }
        }
        if(hasRight) {
            return hasRight;
        }
        toWriteLog("weaver-->2702-->fileid"+fileId+"--->hasRight"+hasRight);
        String isHaveDocSql = "select 1 from docimagefile where imagefileid = ?";
        Boolean isHaveDoc = false;
        rs.executeQuery(isHaveDocSql,fileId);
        if(rs.next()) isHaveDoc = true;
       //如果附件无文档文档，且无权限看，
        if(!hasRight && !isHaveDoc){
            String fileidStr = Util.null2s(req.getParameter("fileid"),"");
            String jmFileidStr = Util.null2s(req.getParameter("jmFileid"),"");
            String fieldidsStr = Util.null2s(req.getParameter("fieldids"),"");
            int fileid = Util.getIntValue(DocDownloadCheckUtil.getDownloadfileid(req), -1);
            String fieldids = Util.null2String(DocDownloadCheckUtil.getDownloadfileid(req));
            //如果是加密串并且fileid存在，则可以下载            如果是批量下载，需要不含逗号，长度大于11（防止只有一个附件id的情况），并且附件id存在
            if((fileidStr.length()>11 && fileid>0) ||(jmFileidStr.length()>11 && fileid>0) || (!fieldidsStr.contains(",") && fileId!=null && !fileId.trim().equals("") && fieldidsStr.length()>11)){
                hasRight = true;
            }
        }
        /**
         * 判断是否来自云盘分享的下载
         */
       // if(!hasRight){  加密串导致hasRight为true，从而云盘文件不进此判断逻辑
            hasRight = FiledownloadUtil.netdiskHasright(hasRight,fileid_head,user);
        //}
        /**
         * 判断是否来自云盘的poi转换
         */
        if(comefrom != null && !comefrom.isEmpty() || !originComeFrom.isEmpty()) {
            String sql = "";
            if (comefrom.isEmpty()) {
                comefrom = originComeFrom;
            }
            if("DocPreviewHtmlImage".equals(comefrom)){
            	sql ="select imageFileId from DocPreviewHtmlImage  where  picFileId=?";
            }else if("DocPreviewHtml".equals(comefrom)){
            	sql = "select imagefileid from DocPreviewHtml  where htmlFileId=?" ;
            }else if("pdfconvert".equals(comefrom)){
            	sql = "select imagefileid from pdf_imagefile where pdfimagefileid=?";
            }
            if(!sql.isEmpty()){
            	rs.executeQuery(sql,fileid_head);
            	if(rs.next()){
            		String _fileid = rs.getString("imagefileid");
            		hasRight = FiledownloadUtil.netdiskHasright(hasRight,_fileid,user);
            	}
            }
        }
        //判断附件是否来源于回复
		String isFromReplySql = "select 1 from reply_imagefile where imagefileid=?";
        rs.executeQuery(isFromReplySql,fileId);
        if(rs.next()){
           hasRight = true;
        }
        
        //pdf附件判断user权限是否一致
        cwid = req.getParameter("cwid");
        if("compared".equals(cwid)){
            String sqlDetail = " select mainid from FnaDocCompareDetail where 1=1 "+
                    "and (leftSrc = ? or leftSrc2 = ?  or rightSrc = ? or rightSrc2 = ? ) ";
            rs.executeQuery(sqlDetail, fileId,fileId,fileId,fileId);
            int mainId = 0;
            if(rs.next()){
                mainId = Util.getIntValue(rs.getString("mainid"),0);
            }
            
            rs.executeQuery(" select userid from FnaDocCompare where id = ? ", mainId);
            if(rs.next()){
                int userIdMain = Util.getIntValue(rs.getString("userid"));
                if(userIdMain != user.getUID()){
                    hasRight = false;
                }else{
                    hasRight = true;
                }
            }
            
            if(!hasRight){
                String sqlMain = " select userid from FnaDocCompare where 1=1 and (left_entry_ids = ? or left_pdf_id = ? or right_entry_ids = ? or right_pdf_id = ?) ";
                rs.executeQuery(sqlMain, fileId,fileId,fileId,fileId);
                if(rs.next()){
                    int userIdMain = Util.getIntValue(rs.getString("userid"));
                    if(userIdMain != user.getUID()){
                        hasRight = false;
                    }else{
                        hasRight = true;
                    }
                }
            }
            
        }
        baseBean.writeLog("weaver-->2917-->fileid"+fileId+"--->hasRight:"+hasRight);
        
        return hasRight;
    }    
    
    //  下载公用方法 
  
  public void toUpload(HttpServletResponse response,String str){
    toUpload(response,str,str); 
  }  
    
    public void toUpload(HttpServletResponse response,String str, String tmptfilename){ 
       try { 
        
            SystemComInfo syscominfo = new SystemComInfo();
            String path= syscominfo.getFilesystem();
            if("".equals(path)){
                path = GCONST.getRootPath();
                path = path + "filesystem" + File.separatorChar+ "downloadBatch"+File.separatorChar+tmptfilename;
            }else{
                if(path.endsWith(File.separator)){
                    path += "downloadBatch"+File.separatorChar+File.separatorChar+tmptfilename;
                    
                }else{
                    path += File.separator+ "downloadBatch"+File.separatorChar+File.separatorChar+tmptfilename;
                }
            }       
        
        //String path=GCONST.getRootPath()+ "downloadBatch"+File.separatorChar+str; 
        //String path="E:/bjls_ecology4.1_5.0/ecology/downloadBatch/"+str; 
        if(!"".equals(path)){ 
         File file=new File(path); 
         if(file.exists()){ 
          InputStream ins=null;
          BufferedInputStream bins=null;
          OutputStream outs=null;
          BufferedOutputStream bouts=null;
          try
          {
            
          ins=new FileInputStream(path); 
          bins=new BufferedInputStream(ins);//放到缓冲流里面 
          outs=response.getOutputStream();//获取文件输出IO流 
          bouts=new BufferedOutputStream(outs); 
               response.setContentType("application/x-download");//设置response内容的类型 
               if((agent.contains(" Chrome")||agent.contains("Safari") )&& !agent.contains("Edge")){
                   response.setHeader("content-disposition", "attachment;filename=\"" + URLEncoder.encode(str.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", "").replaceAll("\\+", "%20").replaceAll("%28", "(")
                           .replaceAll("%29", ")").replaceAll("%7B","{").replaceAll("%7D","}").replaceAll("%5B","[").replaceAll("%5D","]").replaceAll("%40","@").replaceAll("%23","#").replaceAll("%25","%").replaceAll("%26","&")
                           .replaceAll("%2B","+")+ "\"");
//                   response.setHeader("content-disposition", "attachment; filename*=UTF-8''" +  URLEncoder.encode(str.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
               }else if(agent.contains("Firefox")){
                   // filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
                   // res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                    
//                  filename = new String(filename.getBytes("UTF-8"), "ISO-8859-1");
//                  res.setHeader("Content-disposition", "attachment;filename=\"" + filename + "\"");
                  //  filename = "=?UTF-8?B?" + (new String(Base64.encodeBase64(filename.getBytes("UTF-8")))) + "?=";
                  //  res.setHeader("Content-disposition", String.format("attachment; filename=\"%s\"", filename));
                    
            	   response.setHeader("content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(str.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""), "UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")"));
                }else{
                   response.setHeader("content-disposition", "attachment; filename=\"" + URLEncoder.encode(str.replaceAll("<", "").replaceAll(">", "").replaceAll("&lt;", "").replaceAll("&gt;", ""),"UTF-8").replaceAll("\\+", "%20").replaceAll("%28", "(").replaceAll("%29", ")")+"\"");
               }
               int bytesRead = 0; 
               byte[] buffer = new byte[8192]; 
               //开始向网络传输文件流 
               while ((bytesRead = bins.read(buffer, 0, 8192)) != -1) { 
                   bouts.write(buffer, 0, bytesRead); 
               } 
               bouts.flush();//这里一定要调用flush()方法 
               ins.close(); 
               bins.close(); 
               outs.close(); 
               bouts.close(); 
           }
          catch (Exception ef)
          {
              new weaver.filter.XssUtil().writeError(ef);
          }
          finally {
                        
                if(ins!=null) ins.close();
                if(bins!=null) bins.close();
                if(outs!=null) outs.close();
                if(bouts!=null) bouts.close();
                    }

         } 
         else{ 
          response.sendRedirect(GCONST.getContextPath()+"/login/BatchDownloadsEror.jsp");
         } 
        } 
        else{ 
         response.sendRedirect(GCONST.getContextPath()+"/login/BatchDownloadsEror.jsp");

    //注;这里面不要用到PrintWriter out=response.getWriter();这里调用了response对象，后面下载调用时就会出错。这里要是想都用，希望大家找到解决办法。 
        } 
       } catch (IOException e) { 
        e.printStackTrace(); 
       } 
    } 
    
    /**  
     *3：打包完成后删除原来的目中的文件 
     * @param 
     * @param   
     * @throws Exception  
     */ 
  	public  void deleteFile(String targetPath) throws IOException {   
		 File targetFile = new File(targetPath);   
		 if (targetFile.isDirectory()) { 
			if(targetPath.indexOf("downloadBatchTemp")>-1){
		     // FileUtils.deleteDirectory(targetFile);   
		      new FileDeleteUtil().deleteFolder(targetFile);
			}
		 } else if (targetFile.isFile()) {   
		   FileSecurityUtil.deleteFile(targetFile);
			 //new FileDeleteUtil().deleteFile(targetFile);
		 }   
		}  
    /*
     * 
     */
    public boolean addDownLoadLogByimageId( int fileid){
        boolean needUser = true;
        int docId = 0;
        String docIdsForOuterNews = "";
       // String strSql = "select id from DocDetail where exists (select 1 from docimagefile where imagefileid=" + fileid + " and docId=DocDetail.id) and ishistory <> 1 and (docPublishType='2' or docPublishType='3')";
        String strSql = "SELECT  t1.id  FROM DocDetail t1  INNER JOIN docimagefile t2    ON t1.id = t2.docId  " +
        		"WHERE t2.imagefileid = ?   AND t1.ishistory <> 1   AND (  t1.docPublishType = '2'     OR t1.docPublishType = '3')";
        RecordSet rs = new RecordSet();
        rs.executeQuery(strSql,fileid);
        while (rs.next()) {
            docId = rs.getInt("id");
            if (docId > 0) {
                docIdsForOuterNews += "," + docId;
            }
        }

        if (!docIdsForOuterNews.equals("")) {
            docIdsForOuterNews = docIdsForOuterNews.substring(1);
        }

        if (!docIdsForOuterNews.equals("")) {
            String newsClause = "";
            String sqlDocExist = " select 1 from DocDetail where id in(" + docIdsForOuterNews + ") ";
            String sqlNewsClauseOr = "";
            boolean hasOuterNews = false;

            rs.executeSql("select newsClause from DocFrontPage where publishType='0'");
            while (rs.next()) {
                hasOuterNews = true;
                newsClause = Util.null2String(rs.getString("newsClause"));
                if (newsClause.equals("")) {
                    // newsClause=" 1=1 ";
                    needUser = false;
                    break;
                }
                if (!newsClause.trim().equals("")) {
                    sqlNewsClauseOr += " ^_^ (" + newsClause + ")";
                }
            }
            ArrayList newsArr = new ArrayList();
            if (!sqlNewsClauseOr.equals("") && needUser) {
                // sqlNewsClauseOr=sqlNewsClauseOr.substring(sqlNewsClauseOr.indexOf("("));
                // sqlDocExist+=" and ("+sqlNewsClauseOr+") ";
                String[] newsPage = Util.TokenizerString2(sqlNewsClauseOr, "^_^");
                int i = 0;
                String newsWhere = "";
                for (; i < newsPage.length; i++) {
                    if (i % 10 == 0) {
                        newsArr.add(newsWhere);
                        newsWhere = "";
                        newsWhere += newsPage[i];
                    } else
                        newsWhere += " or " + newsPage[i];
                }
                newsArr.add(newsWhere);
            }
            // System.out.print(sqlDocExist);
            if (hasOuterNews && needUser) {
                for (int j = 1; j < newsArr.size(); j++) {
                    String newsp = newsArr.get(j).toString();
                    if (j == 1)
                        newsp = newsp.substring(newsp.indexOf("or") + 2);
                    sqlDocExist += "and(" + newsp + ")";
                    rs.executeSql(sqlDocExist);
                    sqlDocExist = " select 1 from DocDetail where id in(" + docIdsForOuterNews + ") ";
                    if (rs.next()) {
                        needUser = false;
                        break;
                    }
                }
            }
        }

        // 处理外网查看默认图片
        rs.executeSql("SELECT * FROM DocPicUpload  WHERE  Imagefileid=" + fileid);
        if (rs.next()) {
            needUser = false;
        }

        return needUser;
    }
	/**
     * creat Folder and File.
     * @param String fileFoder, String fileName
     * Copyright (c) 2010
     * Company weaver
     * Create Time 2010-11-25
     * @author caizhijun001
     * @return File
     */
    public File fileCreate(String fileFoder, String fileName){
          File foder = new File(fileFoder);//E:\dlxdq_ecology4.5\ecology\filesystem\201012\S  1450623393_.xls
          File file = new File(fileFoder+fileName);
                //如果文件夹不存在，则创建文件夹
          if(foder.exists()==false){
                 foder.mkdirs();//多级目录
                   //foder.mkdir();//只创建一级目录
           }
          
          if(file.exists()==true){//删除以前同名的txt文件
             try{
                 FileSecurityUtil.deleteFile(file);
                 //new FileDeleteUtil().deleteFile(file);
            }catch(Exception e){
                   e.printStackTrace();
           }
          }
          //如果文件不存在，则创建文件
          if(file.exists()==false){
             try{
                       file.createNewFile();
                }catch(IOException e){
                       e.printStackTrace();
               }
          }
          return file;
  }
    
    private boolean isMsgObjToDocument(){
        boolean isMsgObjToDocument=true;
        
        BaseBean basebean = new BaseBean();
        String mClientName=Util.null2String(basebean.getPropValue("weaver_obj","iWebOfficeClientName"));
        boolean isIWebOffice2003 = (mClientName.indexOf("iWebOffice2003")>-1)?true:false;
        String isHandWriteForIWebOffice2009=Util.null2String(basebean.getPropValue("weaver_obj","isHandWriteForIWebOffice2009"));
        if(isIWebOffice2003||isHandWriteForIWebOffice2009.equals("0")){
            isMsgObjToDocument=false;
        }
		if(mClientName.indexOf("iWebOffice2009")>-1)
		{
			isMsgObjToDocument=true;
		}
        
        return isMsgObjToDocument;
    }
    
    private boolean isOfficeToDocument(String extName){
    	boolean isOfficeForToDocument=false;
    	if("xls".equalsIgnoreCase(extName) || "doc".equalsIgnoreCase(extName)||"wps".equalsIgnoreCase(extName)||"ppt".equalsIgnoreCase(extName)||"docx".equalsIgnoreCase(extName)||"xlsx".equalsIgnoreCase(extName)||"pptx".equalsIgnoreCase(extName)){
    		isOfficeForToDocument=true;
    	}
    	return isOfficeForToDocument;
    }

    /**
     *通过文件编号和单位编号判断此单位是否对此文件有下载权限
     * @param fileId       文件编号对应imagefile表中的imagefileId
     * @param companyId    单位编号
     * @return true：有下载权限 false：无下载权限
     */
    private boolean isHaveDownloadRight(String fileId,String companyId){
        boolean isHaveDownloadRight = false;
        if(StringUtil.isNull(fileId,companyId)){
            baseBean.writeLog("方法：FileDownloadForOdocExchange.isHaveDownloadRight 参数：fileId="+fileId+" companyId="+companyId);
            return isHaveDownloadRight;
        }
        /**如何判断当前用户有下载权限*/
        RecordSet rs = new RecordSet();
        /**1:作废的文不允许下载 2:撤销的文不允许接收方下载*/
        /**
         * 1:判断是否是发送方
         * 2:判断是否是接收方
         */
        String getIsSendDepartmentSql = "";
        String dbType = rs.getDBType();
        if("sqlserver".equals(dbType)){
            getIsSendDepartmentSql = "select * from odoc_exchange_docbase where send_companyid=? and  (docimagefileid=?  or ',' + attachimagefileids + ',' like '%,' + ? + ',%') and status!=5";
        }else if("mysql".equals(dbType)){
            getIsSendDepartmentSql = "select * from odoc_exchange_docbase where send_companyid=? and  (docimagefileid=?  or CONCAT(',',attachimagefileids,',') like CONCAT('%,',?,',%')) and status!=5";
        }else{
            getIsSendDepartmentSql = "select * from odoc_exchange_docbase where send_companyid=? and  (docimagefileid=?  or ',' || attachimagefileids || ',' like '%,' || ? || ',%') and status!=5";
        }

        boolean isGetIsSendDepartmentSql = rs.executeQuery(getIsSendDepartmentSql,companyId,fileId,fileId);
        if(isGetIsSendDepartmentSql&&rs.next()){
            isHaveDownloadRight = true;
        }else{
            baseBean.writeLog("方法：FileDownloadForOdocExchange.isHaveDownloadRight 执行sql：getIsSendDepartmentSql="+getIsSendDepartmentSql+" 无数据/作废状态的文不允许下载");
        }
        String getIsReceiveDepartmentSql = "";
        if("sqlserver".equals(dbType)){
            getIsReceiveDepartmentSql = "select b.status from odoc_exchange_docbase a left join odoc_exchange_recieveinfo b on a.document_identifier=b.document_identifier where b.receive_companyid=? and (a.docimagefileid=?  or ',' + a.attachimagefileids + ',' like '%,' + ? + ',%') and a.status!=5 and b.status!=4";
        }else if("mysql".equals(dbType)){
            getIsReceiveDepartmentSql = "select b.status from odoc_exchange_docbase a left join odoc_exchange_recieveinfo b on a.document_identifier=b.document_identifier where b.receive_companyid=? and (a.docimagefileid=?  or CONCAT(',',attachimagefileids,',') like CONCAT('%,',?,',%')) and a.status!=5 and b.status!=4";
        }else{
            getIsReceiveDepartmentSql = "select b.status from odoc_exchange_docbase a left join odoc_exchange_recieveinfo b on a.document_identifier=b.document_identifier where b.receive_companyid=? and (a.docimagefileid=?  or ',' || a.attachimagefileids || ',' like '%,' || ? || ',%') and a.status!=5 and b.status!=4";
        }
        boolean isGetIsReceiveDepartmentSql = rs.executeQuery(getIsReceiveDepartmentSql,companyId,fileId,fileId);
        if(isGetIsReceiveDepartmentSql&&rs.next()){
            isHaveDownloadRight = true;
        }else{
            baseBean.writeLog("方法：FileDownloadForOdocExchange.isHaveDownloadRight 执行sql：getIsReceiveDepartmentSql="+getIsReceiveDepartmentSql+"无数据/作废/撤销状态的文不允许下载");
        }
        return isHaveDownloadRight;
    }

    /**
     * 通过当前登录用户获取当前登陆用户所在单位编号
     * @param uesr 当前登录用户
     * @return 单位编号
     */
    private String getCompanyIdByUser(User user){
        String companyId = "";
        if(null==user){
            baseBean.writeLog("FileDownloadForOdocExchange.getCompanyIdByUser 参数：user"+user);
            return companyId;
        }
        String sqlForReceiveOrSendUser="SELECT exchange_companyid FROM odoc_exchange_com_user WHERE userid=?";
        String sqlForCompanyAdminUser="SELECT exchange_companyid FROM odoc_exchange_com_admin WHERE admin_userid=?";
        RecordSet rs = new RecordSet();
        boolean isSqlForReceiveOrSendUser = rs.executeQuery(sqlForReceiveOrSendUser,user.getUID());
        if(isSqlForReceiveOrSendUser&&rs.next()){
            companyId = rs.getString("exchange_companyid");
            if(StringUtil.isNull(companyId)) {
                boolean isSqlForCompanyAdminUser = rs.executeQuery(sqlForCompanyAdminUser,user.getUID());
                if(isSqlForCompanyAdminUser&&rs.next()){
                    companyId = rs.getString("exchange_companyid");
                }else{
                    baseBean.writeLog("FileDownloadForOdocExchange.getCompanyIdByUser 执行sql：sqlForCompanyAdminUser"+sqlForCompanyAdminUser+"参数：user.getUID()="+user.getUID()+"失败或查询数据为空");
                }
            }else{
                baseBean.writeLog("FileDownloadForOdocExchange.getCompanyIdByUser 执行sql：sqlForReceiveOrSendUser"+sqlForReceiveOrSendUser+"参数：user.getUID()="+user.getUID()+"查询的单位编号为空");
            }
        }else{
            baseBean.writeLog("FileDownloadForOdocExchange.getCompanyIdByUser 执行sql：sqlForReceiveOrSendUser"+sqlForReceiveOrSendUser+"参数：user.getUID()="+user.getUID()+"失败或查询数据为空");
        }
        return companyId;
    }

    /**
     * 判断此后缀是否允许inline模式打开
     * @param extname
     * @return
     */
    private boolean inlineViewFile(String extname){
        if(extname.isEmpty()) return false;
        String whiteListstr = Util.null2s(baseBean.getPropValue("doc_upload_suffix_limit","inline_ext"),"pdf,jpg,jpeg,png,bmp,gif");
        String [] whiteList = whiteListstr.split(",");
        String ext = "";
        for(int i=0;i<whiteList.length;i++){
            ext = whiteList[i];
            if(extname.toLowerCase().equals(ext)){
                return true;
            }
        }
        return false;
    }
    private void toWriteLog(String log){
        systemWaterwriteLog = Util.null2s(baseBean.getPropValue("doc_custom_for_weaver","systemWaterwriteLog"),"0");
       if("1".equals(systemWaterwriteLog)){
            baseBean.writeLog(FileDownload.class.getName(),log);
        }
    }

    private void writeLogs(Object log){
        if("1".equals(writeLogForDownload)){
            baseBean.writeLog(FileDownload.class.getName(), log);
        }
    }

    private boolean odocSmartRegist(int fileid){
        boolean isOdocSmartRegist = false;
        RecordSet recordSet = new RecordSet();
        String sql = "select b.comefrom from pdf_imagefile a left join imagefile b on a.imagefileid=b.imagefileid where a.pdfimagefileid=?";
        recordSet.executeQuery(sql,fileid);
        if(recordSet.next()){
            if("smartRegistration".equals(recordSet.getString("comefrom"))){
                isOdocSmartRegist = true;
            }
        }
        if(!isOdocSmartRegist){
            String sql1 = "select comefrom from imagefile  where imagefileid=?";
            recordSet.executeQuery(sql1,fileid);
            if(recordSet.next()){
                if("smartRegistration".equals(recordSet.getString("comefrom"))){
                    isOdocSmartRegist = true;
                }
            }
        }
        return isOdocSmartRegist;
    }
	private String getSecondAuthFileDownUrl(HttpServletRequest req,boolean fromMobile,int docid){
		String secondAuthFileDownUrl="";
		try{
			String isSecondAuth = Util.null2String(req.getParameter("isSecondAuth"));
			if(!"1".equals(isSecondAuth) && docid>0){
				Map<String,String>  encryptInfo = DocEncryptUtil.EncryptInfo(docid);
				if(encryptInfo!=null){
					String isEnableSecondAuth=Util.null2String(encryptInfo.get(DocEncryptUtil.ISENABLESECONDAUTH),"0");
					if("1".equals(isEnableSecondAuth)){
						StringBuilder requParamStr = new StringBuilder();		
						Enumeration paramNames = req.getParameterNames();
						while (paramNames.hasMoreElements()) {
							String paramName = (String) paramNames.nextElement();
							String paramValue = Util.null2String(req.getParameter(paramName));
							if(paramName.contains("request_header_user_agent") || paramName.contains("param_ip")) continue;
							requParamStr.append("&").append(paramName).append("=").append(paramValue);
						}
						if(fromMobile){
							secondAuthFileDownUrl= GCONST.getContextPath()+"/spa/custom/static4mobile/index.html#/cs/app/2e1b09c0329c4e839002365027216f64_baseTable?isSecondAuth=1"+requParamStr;
						}else{
							secondAuthFileDownUrl= GCONST.getContextPath()+"/spa/custom/static/index.html#/main/cs/app/759eb4fa5ce742c2a0b96877972ceae0_baseTable?isSecondAuth=1"+requParamStr;
						}
						toWriteLog("FileDownload-------------getSecondAuthFileDownUrl------docid------"+docid+";fromMobile="+fromMobile+";secondAuthFileDownUrl="+secondAuthFileDownUrl);
					}
				}
			}
		}catch(Exception e){
			toWriteLog("FileDownload-------------getSecondAuthFileDownUrl------docid------"+docid+";fromMobile="+fromMobile+";Exception="+e);
		}		
		return secondAuthFileDownUrl;		
	}
}