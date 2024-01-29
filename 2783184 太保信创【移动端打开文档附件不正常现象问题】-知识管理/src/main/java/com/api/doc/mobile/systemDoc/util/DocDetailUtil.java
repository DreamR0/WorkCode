package com.api.doc.mobile.systemDoc.util;

import com.api.doc.detail.service.DocDetailService;
import com.api.doc.detail.service.DocViewPermission;
import com.api.doc.detail.util.ImageConvertUtil;
import com.api.doc.edit.util.EditConfigUtil;
import com.api.doc.search.service.DocLogService;
import com.api.networkdisk.util.DocIconUtil;
import com.engine.doc.util.WaterMarkUtil;
import weaver.conn.RecordSet;
import weaver.docs.category.SecCategoryComInfo;
import weaver.docs.docs.DocImageManager;
import weaver.docs.docs.DocManager;
import weaver.docs.docs.reply.DocReplyManager;
import weaver.docs.docs.reply.PraiseInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.splitepage.operate.SpopForDoc;
import weaver.splitepage.transform.SptmForDoc;
import com.engine.msgcenter.util.ValveConfigManager;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author：tongh
 * date：2019-2-1 15:18
 * detail：
 */
public class DocDetailUtil extends BaseBean{



    public Map<String,Object> getDocDetail(int docid,User user,HttpServletRequest request){

        Map<String,Object> apidatas = new HashMap<String,Object>();
        boolean docisLock = false;//是否锁文档
            try { //获取文档详情
            Map<String,String> onlineParams = new HashMap<>();
            onlineParams.put("checkPic","1");
            //文档处理基本类
            DocManager docManager = new DocManager();
            //文档操作权限类
            SpopForDoc spopForDoc = new SpopForDoc();
            //文档信息日志类
            //DocDetailLog docDetailLog = new DocDetailLog();
            //人力资源基本信息缓存类
            ResourceComInfo resourceComInfo = new ResourceComInfo();
            //文档回复管理类
            DocReplyManager docReply = new DocReplyManager();
            //文档图片管理类
            DocImageManager docImageManager = new DocImageManager();
                //判断pdf是否走永中转换,默认不走
                String isPdfConvert = Util.null2String(getPropValue("doc_custom_for_weaver","pdfConvert"),"0");
            ImageConvertUtil icu = new ImageConvertUtil();
            if(docid <= 0){
                apidatas.put("api_status",true);
                apidatas.put("hasDoc",false);
                apidatas.put("canReader",true);
                String fileid = Util.null2String(request.getParameter("fileid"));
                RecordSet rs = new RecordSet();
                rs.executeQuery("select imagefileid,imagefilename from imagefile where imagefileid ="+fileid);
                if(rs.next()) {
                    Map<String,Object> fileInfo = new HashMap<String,Object>();
                    String imfid=rs.getString("imagefileid");
                    String fname=Util.null2String(rs.getString("imagefilename"));
                    long fSize = docImageManager.getImageFileSize(Util.getIntValue(imfid));
                    String ficon = "general_icon.png";

                    String docImagefileSizeStr = "";
                    if(fSize / (1024 * 1024) > 0) {
                        docImagefileSizeStr = (fSize / 1024 / 1024) + "M";
                    } else if(fSize / 1024 > 0) {
                        docImagefileSizeStr = (fSize / 1024) + "K";
                    } else {
                        docImagefileSizeStr = fSize + "B";
                    }
                    String extName = fname.contains(".")? fname.substring(fname.lastIndexOf(".") + 1) : "";
                    Map<String, String> iconMap = DocIconUtil.getDocIconDetail(extName);
                    boolean tooLarge = icu.isTooLarge(extName,fSize + "");
                    if(tooLarge && "pdf".equals(extName.toLowerCase())){
                    	tooLarge = false;
                    }
                    boolean readOnLine = ImageConvertUtil.readOnlineForMobile(extName,onlineParams);//是否支持在线查看
                    if(tooLarge){
                        readOnLine = false;
                    }
                    fileInfo.put("tooLarge",tooLarge ? "1" : "0");
                    fileInfo.put("imagefileid",imfid);
                    fileInfo.put("filename",fname);
                    fileInfo.put("ficon", iconMap);
                    fileInfo.put("fileSizeStr",docImagefileSizeStr);
                    fileInfo.put("readOnLine",readOnLine ? "1" : "0");
					boolean candownload = true;
					if("1".equals(ValveConfigManager.getTypeValve("prohibitDownload", "prohibitDownloadSwatch", "0"))){
                        candownload = false;
                    }
                    fileInfo.put("candownload",candownload);
                    fileInfo.put("doc_acc_isdownload",Util.null2s(getPropValue("doc_mobile_detail_prop","doc_acc_isdownload"),"1"));
                    //移动端是否直接下载，不提示弹框 0 不提示，1提示
                    fileInfo.put("doc_acc_download_noalert",Util.null2s(getPropValue("doc_mobile_detail_prop","doc_acc_download_noalert"),"1"));
                    fileInfo.put("doc_pdf_download",Util.null2s(getPropValue("doc_pdf_download_bl","doc_pdf_download"),"0"));
                    apidatas.put("fileInfo",fileInfo);
                }else{
                    apidatas.put("canReader",false);
                }
            }else{
                RecordSet rs = new RecordSet();
                String sql = "select DocSubject from Docdetail where id="+docid;
                rs.executeQuery(sql);
                if(!rs.next()){
                    apidatas.put("api_status",false);
                }else{

                    //0:查看
                    boolean canReader = false;
                    //1:编辑
                    boolean canEdit = false;
                    //2:删除
                    boolean canDel = false;
                    //3:共享
                    boolean canShare = false ;
                    //4:是否单附件打开
                    boolean canOpenSingleAttach = false;
                    //5:下载
                    boolean candownload = false;

                    BaseBean bb = new BaseBean();

                    if(isPdfConvert.equals("")){
                        isPdfConvert = "0";
                    }

                    int userid=user.getUID();
                    //获取用户类型 1 内部用户 2 外部用户
                    String logintype = user.getLogintype();
                    String userType = ""+user.getType();
                    String userdepartment = ""+user.getUserDepartment();
                    String usersubcomany = ""+user.getUserSubCompany1();
                    //获取安全级别
                    String userSeclevel = user.getSeclevel();
                    String userSeclevelCheck = userSeclevel;
                    //如果是外部用户
                    if("2".equals(logintype)){
                        userdepartment="0";
                        usersubcomany="0";
                        userSeclevel="0";
                    }

                    //重置各参数的值
                    docManager.resetParameter();
                    //设置文档id
                    docManager.setId(docid);
                    //通过文档ID得到相应的文档信息
                    docManager.getDocInfoById();
                    //返回文档子目录
                    int seccategory=docManager.getSeccategory();

                    SecCategoryComInfo scci = new SecCategoryComInfo();
                    String  seccategoryName = scci.getAllParentName("" + docManager.getSeccategory(),true);//文档所在目录
                    //返回文档内容
                    String doccontent=docManager.getDoccontent();
                    //返回 文档发布类型
                    String docpublishtype=docManager.getDocpublishtype();
                    //文档状态
                    String docstatus=docManager.getDocstatus();
                    
                    int doctype = docManager.getDocType();
                    //
                    // int ishistory = docManager.getIsHistory();

                    //子目录信息  执行存储过程
                    // rs.executeProc("Doc_SecCategory_SelectByID",seccategory+"");
                    // rs.next();
                    // String readerCanViewHistoryEdition=Util.null2String(rs.getString("readerCanViewHistoryEdition"));

                    // String userInfo=logintype+"_"+userid+"_"+userSeclevelCheck+"_"+userType+"_"+userdepartment+"_"+usersubcomany;
                    //返回对文档的各种操作权限，false为无权限，true为有权限
                    // List PdocList = spopForDoc.getDocOpratePopedom("" + docid,userInfo);

                    //判断当前文档是否开启单附件打开
                    canOpenSingleAttach = SystemDocUtil.canOpenSingleAttach(docid,doccontent);

                       /* if (((String)PdocList.get(0)).equals("true")) canReader = true ;//查看
                        if (((String)PdocList.get(1)).equals("true")) canEdit = true ;//编辑
                        if (((String)PdocList.get(2)).equals("true")) canDel = true ;//删除
                        if (((String)PdocList.get(3)).equals("true")) canShare = true ;//共享
                        if (((String)PdocList.get(5)).equals("true")) candownload = true;//下载


                        //7:失效 8:作废
                        if(canReader && ((!docstatus.equals("7")&&!docstatus.equals("8"))
                                ||(docstatus.equals("7")&&ishistory==1&&readerCanViewHistoryEdition.equals("1")))){
                            canReader = true;
                        }else{
                            canReader = false;
                        }
                        //是否可以查看历史版本
                        //具有编辑权限的用户，始终可见文档的历史版本；
                        //可以设置具有只读权限的操作人是否可见历史版本；
                        if(ishistory==1) {
                            if(readerCanViewHistoryEdition.equals("1")){
                                if(canReader && !canEdit) canReader = true;
                            } else {
                                if(canReader && !canEdit) canReader = false;
                            }
                        }

                        //编辑权限操作者可查看文档状态为：“审批”、“归档”、“待发布”或历史文档
                        if(canEdit && ((docstatus.equals("3") || docstatus.equals("5") || docstatus.equals("6") || docstatus.equals("7")) || ishistory==1)) {
                            canReader = true;
                        }
                        */
                    DocViewPermission dvp = new DocViewPermission();
                    if(dvp.hasRightForSecret(user,docid)){   //密级等级判断
                        Map<String,Boolean> levelMap = dvp.getShareLevel(docid,user,false);
                        canReader = levelMap.get(DocViewPermission.READ);
                        canEdit = levelMap.get(DocViewPermission.EDIT);
                        canDel = levelMap.get(DocViewPermission.DELETE);
                        canShare = levelMap.get(DocViewPermission.SHARE);
                        candownload = levelMap.get(DocViewPermission.DOWNLOAD);
                        if(!canReader){
                            levelMap.put(DocViewPermission.READ,dvp.hasRightFromOtherMould(docid,user,request));
                            canReader = levelMap.get(DocViewPermission.READ);
                            String sqldownload = "select nodownload from docseccategory where id = (select seccategory from docdetail where id = ?)";
                            RecordSet rsdown = new RecordSet();
                            rsdown.executeQuery(sqldownload,docid);
                            String nodownload = "0";
                            if(rsdown.next()){
                                nodownload = Util.null2s(rsdown.getString("nodownload"),"0");
                            }
                            if(canReader && "0".equals(nodownload)){
                                candownload = true;
                            }
                        }
                    }

					if("1".equals(ValveConfigManager.getTypeValve("prohibitDownload", "prohibitDownloadSwatch", "0"))){
                        candownload = false;
                    }
                    if(canReader){
                        apidatas.put("api_status",true);
                        apidatas.put("hasDoc",true);
                        if(docpublishtype.equals("2")){
                            int tmppos = doccontent.indexOf("!@#$%^&*");
                            if(tmppos!=-1) doccontent = doccontent.substring(tmppos+8,doccontent.length());
                        }
                        doccontent=doccontent.replaceAll("<meta.*?/>","");
                        Map<String,Object> docInfo = new HashMap<String,Object>();
                        //getDoccreaterid 返回文档创建者id
                        if( userid != docManager.getDoccreaterid() || !docManager.getUsertype().equals(logintype) ) {
                                /*char flag=Util.getSeparator() ;
                                rs.executeProc("docReadTag_AddByUser",""+docid+flag+userid+flag+logintype);
                                docDetailLog.resetParameter();
                                docDetailLog.setDocId(docid);
                                docDetailLog.setDocSubject(docManager.getDocsubject());
                                docDetailLog.setOperateType("0");
                                docDetailLog.setOperateUserid(user.getUID());
                                docDetailLog.setUsertype(user.getLogintype());
                                docDetailLog.setClientAddress(request.getRemoteAddr());
                                docDetailLog.setDocCreater(docManager.getDoccreaterid());
                                docDetailLog.setDocLogInfo();*/
                            DocLogService dls = new DocLogService();
                            dls.addReadLog(docid,user,request.getRemoteAddr());
                        }
                        doccontent = Util.replace(doccontent, "&amp;", "&", 0);
                        doccontent = doccontent.replace("<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"/>","");
                        docInfo.put("canDelete",canDel);  //是否可删除
                        docInfo.put("canShare",canShare); //是否可分享
                        docInfo.put("docTitle",docManager.getDocsubject()); // 文档标题
                        docInfo.put("doccontent",doccontent);  //文档内容
                        docInfo.put("owner",resourceComInfo.getLastname(docManager.getOwnerid()+""));  //所有者
                        docInfo.put("docCreater",resourceComInfo.getLastname(docManager.getDoccreaterid()+""));  //文档创建者
                        docInfo.put("docCreateTime",docManager.getDoccreatedate()+" "+docManager.getDoccreatetime());  //文档创建时间
                        docInfo.put("doccontent",doccontent);  //文档内容
                        docInfo.put("seccategory",seccategoryName);


                        boolean isContentEmpty = DocDetailService.ifContentEmpty(doccontent);
                        
                        docInfo.put("ownerHeaderUrl",resourceComInfo.getMessagerUrls(docManager.getOwnerid()+""));  //所有者头像地址
                        docInfo.put("ownerid",docManager.getOwnerid() + ""); //所有者id
                        docInfo.put("updateUser",resourceComInfo.getLastname(docManager.getDoclastmoduserid() + "")); //最后更新人
                        docInfo.put("updateTime",docManager.getDoclastmoddate() + " " + docManager.getDoclastmodtime());  //最后更新时间
                        docInfo.put("readCount","0");  //阅读数量
                        docInfo.put("canOpenSingleAttach",canOpenSingleAttach && isContentEmpty);//是否开启单附件打开
                        docInfo.put("candownload",candownload);

                        docInfo.put("isContentEmpty",isContentEmpty); //正文内容是否为空

                        //获取当前文档的状态
                        SptmForDoc sptmForDoc = new SptmForDoc();
                        //当前文档id
                        String docid1 = Util.null2String(docid);
                        String docstatusname = sptmForDoc.getDocStatus3(docid1, ""+user.getLanguage()+"+"+docstatus+"+"+seccategory);
                        docInfo.put("docstatus", docstatusname);
                        docInfo.put("docstatusnum", docstatus);


                        rs.executeSql("select count(1) num from DocDetailLog where docid="+docid+" and operatetype = 0");
                        if(rs.next()){
                            docInfo.put("readCount",rs.getString("num"));
                        }
                        docInfo.put("canReply",false);  //是否允许回复
                        rs.executeSql("select replyable,defaultlockeddoc from DocSecCategory where id=" + seccategory);
                        if(rs.next()){
                            docInfo.put("canReply","1".equals(rs.getString("replyable")));
                            docisLock = "1".equals(rs.getString("defaultlockeddoc"));//是否锁定
                        }
                        if(canEdit){
                            docisLock = false;
                        }
                        rs.executeSql("select count(1) num from DOC_REPLY where docid='" + docid + "'");
                        docInfo.put("replyCount","0"); //回复数
                        if(rs.next()){
                            docInfo.put("replyCount",rs.getString("num"));
                        }

                        PraiseInfo praiseInfo = docReply.getPraiseInfoByDocid(docid + "",user.getUID());
                        docInfo.put("praiseCount","0"); //数量
                        docInfo.put("isPraise",false); //是否过
                        if(praiseInfo.getUsers() != null){
                            docInfo.put("praiseCount",praiseInfo.getUsers().size() + "");
                            docInfo.put("isPraise",praiseInfo.getIsPraise() == 1 ? true : false);
                        }
                        docInfo.put("isCollute",false); //是否收藏过
                        rs.executeSql("select id from SysFavourite where favouriteObjId=" + docid + " and favouritetype=1 and Resourceid="+user.getUID());
                        if(rs.next()){
                            docInfo.put("isCollute",true);
                        }
                        //ConvertPDFUtil.isUsePDFViewer();
                        //String icon = resourceComInfo.getMessagerUrls(docManager.getOwnerid() + "");
                        String  icon = User.getUserIcon(docManager.getOwnerid() + "");
                        String sex = resourceComInfo.getSexs(docManager.getOwnerid() + "");
                        String errorIcon = icon;
                        if("1".equals(sex)){
                            // icon = weaver.hrm.User.getUserIcon(docManager.getOwnerid() + "");
                            //errorIcon = "/messager/images/icon_w_wev8.jpg";
                        }else{
                            //  icon = "/messager/images/icon_m_wev8.jpg";
                            //errorIcon = "/messager/images/icon_m_wev8.jpg";
                        }
                        docInfo.put("icon",icon);//所有者头像
                        docInfo.put("errorIcon",errorIcon); // 所有者头像加载错误时加载的图片
                        //rs.executeSql("select f.imagefileid,f.imagefilename,f.filesize,df.docid from DocImageFile df,ImageFile f where df.imagefileid=f.imagefileid and df.docid=" + docid);
                        //附件上传信息
                        Map<String,String> upload = EditConfigUtil.getFileUpload(user.getLanguage(),null,docid,seccategory);
                        docInfo.put("maxUploadSize",upload.get("maxUploadSize"));
                        docInfo.put("limitType",upload.get("limitType"));
                        int docImageId = 0;
                        //DocImageManager docImageManager = new DocImageManager();
                        docImageManager.resetParameter();
                        docImageManager.setDocid(docid);
                        docImageManager.selectDocImageInfo();
                        List<Map<String,Object>> docAttrs = new ArrayList<Map<String,Object>>();
                        docInfo.put("docAttrs",docAttrs); //附件列表

                        DocDetailService docService = new DocDetailService();
                        Map<String,Object> docParams = new HashMap<String,Object>();
                        docParams = docService.getDocParamInfo(docid,user,true);

                        docInfo.put("docParams",docParams.get("data"));//文档基本属性

                        weaver.docs.docs.util.DesUtils des = null;
                        try{
                            des = new weaver.docs.docs.util.DesUtils();

                        }catch(Exception e){
                        }

                        rs.executeQuery("select * from DocImageFile where docid="+docid+" and (isextfile <> '1' or isextfile is null) and docfiletype <> '1' order by versionId desc");
                        if(rs.next()) {
                            Map<String,Object> docAttr = new HashMap<String,Object>();
                            docAttrs.add(docAttr);
                            String imfid=rs.getString("imagefileid");
                            String fname=Util.null2String(rs.getString("imagefilename"));
                            long fSize = docImageManager.getImageFileSize(Util.getIntValue(imfid));
                            String ficon = "general_icon.png";


                            String docImagefileSizeStr = "";
                            if(fSize / (1024 * 1024) > 0) {
                                docImagefileSizeStr = (fSize / 1024 / 1024) + "M";
                            } else if(fSize / 1024 > 0) {
                                docImagefileSizeStr = (fSize / 1024) + "K";
                            } else {
                                docImagefileSizeStr = fSize + "B";
                            }
                            String extName = fname.contains(".")? fname.substring(fname.lastIndexOf(".") + 1) : "";


                            Map<String, String> iconMap = DocIconUtil.getDocIconDetail(extName);

                            boolean tooLarge = icu.isTooLarge(extName,fSize + "");
                            if(tooLarge && "pdf".equals(extName.toLowerCase())){
                            	tooLarge = false;
                            }
                            onlineParams.put("docid",docid+"");
                            boolean readOnLine = ImageConvertUtil.readOnlineForMobile(extName,onlineParams);//是否支持在线查看
                            if(tooLarge){
                                readOnLine = false;
                            }
                            docAttr.put("tooLarge",tooLarge ? "1" : "0");
                            docAttr.put("imagefileid",imfid);
                            docAttr.put("officeDoc","1");
                            docAttr.put("docid",docid+"");
                            docAttr.put("filename",fname);
                            docAttr.put("ficon", iconMap);
                            docAttr.put("fileSizeStr",docImagefileSizeStr);
                            docAttr.put("readOnLine",readOnLine ? "1" : "0");
                            if(SystemDocUtil.canEditForMobile(extName,user)){
                                //==zj==将流程正文编辑权限关闭
                    			docAttr.put("canEditDoc",true);
                                /*docAttr.put("canEditDoc",false);*/
                            }
                            String ddcode = SystemDocUtil.takeddcode(user,imfid,null);
                            docAttr.put("ddcode","");
                            request.getSession().setAttribute("docAttr_" + user.getUID() + "_" + docAttr.get("docid") + "_" + docAttr.get("imagefileid"),"1");
                        }
                        while (docImageManager.next()) {
                            int temdiid = docImageManager.getId();
                            if (temdiid == docImageId) {
                                continue;
                            }
                            docImageId = temdiid;
                            Map<String,Object> docAttr = new HashMap<String,Object>();
                            docAttrs.add(docAttr);
                            String filename = Util.null2String(docImageManager.getImagefilename());
                            int filesize = docImageManager.getImageFileSize(Util.getIntValue(docImageManager.getImagefileid()));
                            String extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
                            Map<String, String> iconMap = DocIconUtil.getDocIconDetail(extName);
                            String ficon = "general_icon.png";
                            boolean tooLarge = icu.isTooLarge(extName,filesize + "");
                            if(tooLarge && "pdf".equals(extName.toLowerCase())){
                            	tooLarge = false;
                            }
                            onlineParams.put("docid",docid+"");
                            boolean readOnLine = ImageConvertUtil.readOnlineForMobile(extName,onlineParams);//是否支持在线查看
                            if(tooLarge){
                                readOnLine = false;
                            }
                            String fileSizeStr = "";
                            if(filesize / (1024 * 1024) > 0) {
                                fileSizeStr = (filesize / 1024 / 1024) + "M";
                            } else if(filesize / 1024 > 0) {
                                fileSizeStr = (filesize / 1024) + "K";
                            } else {
                                fileSizeStr = filesize + "B";
                            }
                            docAttr.put("tooLarge",tooLarge ? "1" : "0");
                            docAttr.put("imagefileid",Util.null2String(docImageManager.getImagefileid()));
                            docAttr.put("docid",docid+"");
                            docAttr.put("filename",filename);
                            docAttr.put("ficon",iconMap);
                            docAttr.put("fileSizeStr",fileSizeStr);
                            docAttr.put("readOnLine",readOnLine ? "1" : "0");
                            
                            if(SystemDocUtil.canEditForMobile(extName,user)){
                    			docAttr.put("canEditAcc",true);
                    		}

                            String ddcode = SystemDocUtil.takeddcode(user,docImageManager.getImagefileid(),null);
                            docAttr.put("ddcode","");

                            request.getSession().setAttribute("docAttr_" + user.getUID() + "_" + docAttr.get("docid") + "_" + docAttr.get("imagefileid"),"1");
                        }
                        
                        if(canEdit && (Boolean)docInfo.get("canOpenSingleAttach") && (Boolean)docInfo.get("isContentEmpty")){
                        	if(docAttrs.size() > 0){
                        		Map<String,Object> docAttr = docAttrs.get(0);
                        		String filename = Util.null2String(docAttr.get("filename"));
                        		if(SystemDocUtil.canEditForMobile(filename,user)){
                        			if(doctype == 2){
                        			    //==zj==流程正文不给编辑权限
                        				docInfo.put("canEditDoc",true);
                                        /*docInfo.put("canEditDoc",false);*/
                        			}else{
                        				docInfo.put("canEditAcc",true);
                        			}
                        		}
                        	}
                        }

                        if(canEdit){
                            if(docAttrs.size() > 0){
                                Map<String,Object> docAttr = docAttrs.get(0);
                                String filename = Util.null2String(docAttr.get("filename"));
                                if(SystemDocUtil.canEditForMobile(filename,user)){
                                    int requestid =Util.getIntValue(request.getParameter("requestid"));
                                    int imagefileid =Util.getIntValue(request.getParameter("fileid"));
                                    if(requestid>0){
                                        RecordSet recordSet = new RecordSet();
                                        boolean canEditForZW = false;
                                        recordSet.executeQuery("select isextfile from docimagefile where docid =?",docid);
                                        if(recordSet.next()){
                                            String isextfile = recordSet.getString("isextfile");
                                            if(!"1".equals(isextfile)){
                                                canEditForZW = true;
                                            }
                                        }
                                        //==zj==流程正文编辑按钮控制
                                        /*docInfo.put("canEditForZW",canEditForZW);*/
                                        docInfo.put("canEditForZW",false);
                                        String extendName = filename.substring(filename.lastIndexOf("."));
                                        rs.executeQuery("select * from workflow_createdoc  where workflowId=(select workflowid from workflow_requestbase where requestid=?)",requestid);
                                        String isCompellentMark = "";
                                        String isCancelCheck = "";
                                        if(rs.next()){
                                            isCompellentMark=  rs.getString("isCompellentMark");
                                            isCancelCheck= rs.getString("isCancelCheck");
                                        }
                                        docInfo.put("canEditUrl","/docs/e9/weboffice_wps.jsp?mFileType="+ extendName + "&fromOdoc=1&isMobile=1&fileid=" + imagefileid + "&isCompellentMark="+isCompellentMark+"&isCancelCheck="+isCancelCheck);
                                    }
                                }
                            }
                        }

                        
                        //获取系统文档相关配置
                        //是否展示文档最后修改时间
                        docInfo.put("doc_final_edit",SystemDocUtil.getDefaultSet("doc_final_edit"));
                        //是否显示阅读的、评论的、点赞的、附件的数量
                        docInfo.put("doc_given_count",SystemDocUtil.getDefaultSet("doc_given_count"));
                        //不可在线预览的附件是否可以下载
                        docInfo.put("doc_acc_isdownload",Util.null2s(bb.getPropValue("doc_mobile_detail_prop","doc_acc_isdownload"),"1"));
                        //移动端是否直接下载，不提示弹框 0 不提示，1提示
                        docInfo.put("doc_acc_download_noalert",Util.null2s(bb.getPropValue("doc_mobile_detail_prop","doc_acc_download_noalert"),"1"));
                        String doc_pdf_download = Util.null2s(bb.getPropValue("doc_pdf_download_bl","doc_pdf_download"),"0");
                        docInfo.put("doc_pdf_download",doc_pdf_download);
                        apidatas.put("docInfo",docInfo);
                        apidatas.put("canReader",canReader);
                        apidatas.put("api_status",true);
                        apidatas.put("docisLock",docisLock);
                        apidatas.put("msg","success");
                        Map<String,Object> secWmSetMap = WaterMarkUtil.getCategoryWmSet(docid+"",false);
                        apidatas.put("secWmSet",secWmSetMap);
                    }else{
                        apidatas.put("api_status",true);
                        apidatas.put("canReader",canReader);
                        apidatas.put("docisLock",docisLock);
                        apidatas.put("msg","canReader is false");
                    }
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
            apidatas.put("api_status", false);
            apidatas.put("msg", "error");
            //记录异常日志
                BaseBean bb = new BaseBean();
            bb.writeLog("DocDetailUtil--->getDocDetail-->:"+ ex.getMessage());
        }
            return apidatas;
    }

    
    
    /**
     * mobile端附件是否支持预览(其他模块附件字段是否跳转到知识附件预览页面)
     * @author wangqs
     * */
    public static boolean canAttachView(String filename){
    	filename = Util.null2String(filename).toLowerCase();
        
        String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
        
        DocDetailUtil ddu = new DocDetailUtil();
        List<String> extnames = ddu.getAttachViewFileType();
        
        return extnames.contains(extname);
    }
    public List<String> getAttachViewFileType(){
        return getAttachViewFileType(false,false);
    }
    public List<String> getAttachViewFileType(Boolean pcView,Boolean isIE){
    	
    	List<String> extnames = new ArrayList<String>();
    	
    	extnames.add("pdf");
    	extnames.add("jpg");
    	extnames.add("jpeg");
    	extnames.add("png");
    	extnames.add("gif");
    	extnames.add("bmp");
        boolean isusepoi = ImageConvertUtil.isUsePoiForMobile();
        if(isusepoi || pcView){
            extnames.add("doc");
            extnames.add("docx");
            extnames.add("xls");
            extnames.add("xlsx");
        }
        if(pcView&&isIE){
            extnames.add("ppt");
            extnames.add("pptx");
            extnames.add("wps");
        }
    	boolean isUsePDFViewer =  isReadonline();
    	if(isUsePDFViewer){
    		extnames.add("doc");
    		extnames.add("docx");
    		extnames.add("xls");
    		extnames.add("xlsx");
    		extnames.add("ppt");
    		extnames.add("pptx");
    		extnames.add("wps");
    		extnames.add("txt");
    		extnames.add("zip");
    		extnames.add("rar");
    		extnames.addAll(ImageConvertUtil.getExtendConvertTypeFromProp());
    	}
    	if(ImageConvertUtil.shukeLightReaderPrew()){
            extnames.add("ofd");
        }
    	return extnames;
    }

	private boolean isReadonline(){
        BaseBean bb = new BaseBean();
        ImageConvertUtil icu = new ImageConvertUtil();
         boolean convert_client_open =  icu.convertForClient();
		String IsUseDocPreview = Util.null2s(bb.getPropValue("docpreview","IsUseDocPreview"),"0");
		String isUsePDFViewer = Util.null2s(bb.getPropValue("docpreview","isUsePDFViewer"),"0");
		if(convert_client_open && "1".equals(IsUseDocPreview) && "1".equals(isUsePDFViewer)){
			return true;
		}else{
			return false;
		}
    }
	
	public static String getMobileDocSubjectLabel(int secid,User user){
		String docTitleLabel = "";
        RecordSet rs0 = new RecordSet();
        rs0.executeQuery("select customName from DocSecCategoryDocProperty where secCategoryId=? and type=1",secid);
        if(rs0.next()){
        	docTitleLabel = Util.null2String(rs0.getString("customName"));
        }
        
        if(docTitleLabel.trim().isEmpty()){
        	docTitleLabel = SystemEnv.getHtmlLabelName(19541,user.getLanguage());
        }
        return docTitleLabel;
	}

	
}
