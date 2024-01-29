package com.api.doc.detail.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.api.doc.detail.util.*;
import com.cloudstore.dev.api.bean.MessageType;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.EncryptFieldEntity;
import com.engine.common.service.HrmCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.doc.bean.DocMenuBean;
import com.engine.doc.util.*;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;

import weaver.conn.RecordSet;
import weaver.cpt.capital.CapitalComInfo;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.docs.DocDetailLog;
import weaver.docs.category.DocTreeDocFieldComInfo;
import weaver.docs.category.SecCategoryComInfo;
import weaver.docs.category.SecCategoryDocPropertiesComInfo;
import weaver.docs.category.security.MultiAclManager;
import weaver.docs.docs.CustomFieldManager;
import weaver.docs.docs.DocComInfo;
import weaver.docs.docs.DocManager;
import weaver.docs.mould.DocMouldComInfo;
import weaver.docs.mould.MouldManager;
import weaver.encrypt.EncryptUtil;
import weaver.general.StaticObj;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.UserManager;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.interfaces.workflow.browser.Browser;
import weaver.interfaces.workflow.browser.BrowserBean;
import weaver.lgc.asset.AssetComInfo;
import weaver.proj.Maint.ProjectInfoComInfo;
import weaver.share.ShareManager;
import weaver.splitepage.transform.SptmForDoc;
import weaver.system.systemmonitor.docs.DocMonitorManager;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.language.LanguageComInfo;
import weaver.workflow.field.BrowserComInfo;
import weaver.workflow.request.ResourceConditionManager;

import com.api.doc.detail.bean.DocParam;
import com.api.doc.detail.util.DocDetailUtil;
import com.api.doc.detail.util.DocParamItem;
import com.api.doc.detail.util.ImageConvertUtil;
import com.api.doc.search.bean.RightMenu;
import com.api.doc.search.util.DocSptm;
import com.api.doc.search.util.DocTableType;
import com.api.doc.search.util.PatternUtil;
import com.api.doc.search.util.RightMenuType;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.msgcenter.util.ValveConfigManager;

/**
 * 文档详情 相关业务数据
 * @author wangqs
 * */
public class DocDetailService {
	
	public final static String DOC_CONTENT = "content";  //正文
	public final static String DOC_PARAM = "param";  //属性
	public final static String DOC_SHARE = "share";  //共享
	public final static String DOC_ACC = "acc";  //附件
	public final static String DOC_VERSION = "version";  //版本
	public final static String DOC_REPLY = "reply";  //回复
	public final static String DOC_LOG = "log";  //日志
	public final static String DOC_SCORE = "score";  //打分
	public final static String DOC_REF = "ref";  //相关资源
	public final static String DOC_CHILD = "children";  //子文档
	
	
	public final static String IS_OPEN_ACC = "isOpenAcc";
	public final static String ACC_FILE_ID = "imagefileid";
	public final static String ACC_FILE_VERSION = "versionid";
	public final static String ACC_FILE_NAME = "filename";
	
	
	public static String READ_COUNT = "readCount"; //阅读总数
	public static String REPLY_COUNT = "replyCount"; //回复总数
	public static String DOWNLOAD_COUNT = "downloadCount"; //下载总数
	public static String SCORE_COUNT = "scoreCount";  //评分总数
	
	
	public final static boolean SHOW_EMPTY_PARAM = false;
	
	private HttpServletRequest request;
	
	public void setRequest(HttpServletRequest request){
		this.request = request;
	}
	
	/**
	 * 获取文档基本属性( 标题、修改人、修改时间)
	 * @return
	 * 	{
	 * 		status 	: 	1-成功，其他-失败
	 * 		msg 	: 	status不为1时，错误信息
	 * 		data 	:	{
	 * 						docSubject : 文档标题
	 * 						doclastmoduserid：最后修改人id
	 * 						doclastmoduser:最后修改人名称
	 * 						doclastmoddatetime：最后修改时间（日期+时间）
	 * 						accessorycount:附件数
	 * 						replaydoccount:回复数
	 * 					}
	 * 	}
	 * */
	public Map<String,Object> getBasicInfoNoRight(int docid,User user,List<String> columns) throws Exception{
		TimeZoneUtil timeZoneUtil = new TimeZoneUtil();
		int language = user == null ? DocSptm.getDefaultLanguage() : user.getLanguage();
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,language));
			return apidatas;
		}
		
		Map<String,String> columnMap = new HashMap<String,String>();
		columnMap.put("DOCSUBJECT","1");
		columnMap.put("DOCLASTMODUSERID","1");
		columnMap.put("DOCLASTMODUSERTYPE","1");
		columnMap.put("DOCLASTMODDATE","1");
		columnMap.put("DOCLASTMODTIME","1");
		
		List<String> docColumns = new ArrayList<String>();
		if(columns != null && columns.size() > 0){
			for(String column : columns){
				if(column == null || column.equals("")) continue;
				if(columnMap.get(column.toUpperCase()) == null){
					columnMap.put(column.toUpperCase(),"1");
					docColumns.add(column);
				}
				
			}
		}
		
		String backFields = "docSubject,doclastmoduserid,docLastModUserType,docpublishtype,doclastmoddate,doclastmodtime,docstatus";
		
		for(String column : docColumns){
			backFields += "," + column;
		}
		
		RecordSet rs = new RecordSet();
		rs.executeQuery("select " + backFields +
				" from Docdetail where id=?",docid);
		if(!rs.next()){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(23230,language));
			return apidatas;
		}
		
		Map<String,String> data = new HashMap<String,String>();
		//文档标题
		String docsubject_tmp = Util.null2String(rs.getString("docsubject"));
    	docsubject_tmp = docsubject_tmp.replaceAll("\n", "");
    	docsubject_tmp=docsubject_tmp.replaceAll("&lt;","<");
    	docsubject_tmp=docsubject_tmp.replaceAll("&gt;",">");
    	docsubject_tmp=docsubject_tmp.replaceAll("&quot;","\"");
    	docsubject_tmp=docsubject_tmp.replaceAll("&#8226;","·");
    	docsubject_tmp=docsubject_tmp.replaceAll("&amp;","&");

		data.put("docSubject",StringEscapeUtils.unescapeHtml(docsubject_tmp));
		//最后修改人id
		data.put("doclastmoduserid",Util.null2String(rs.getString("doclastmoduserid")));
		CustomerInfoComInfo cici = new CustomerInfoComInfo();
		ResourceComInfo rci = new ResourceComInfo();
		//最后修改人名称
		data.put("doclastmoduser","2".equals(rs.getString("docLastModUserType")) ? 
				cici.getCustomerInfoname(rs.getString("doclastmoduserid")) : rci.getResourcename(rs.getString("doclastmoduserid")));
		//最后修改时间（日期+时间）
		String time_doclastmoddatetime = TimeZoneUtil.getYmdLocaleDate1( Util.null2String(rs.getString("doclastmoddate"))
				+ " " + Util.null2String(rs.getString("doclastmodtime")));
		data.put("doclastmoddatetime",time_doclastmoddatetime);
		String doclastmoddate = Util.null2String(rs.getString("doclastmoddate"));
		String doclastmodtime = Util.null2String(rs.getString("doclastmodtime"));
		String doclastmoddateandtime = doclastmoddate + " " +doclastmodtime;
		doclastmoddateandtime = timeZoneUtil.getServerDate(doclastmoddateandtime);
		data.put("doclastmoddate",doclastmoddateandtime.substring(0,10));
		data.put("doclastmodtime",doclastmoddateandtime.substring(11));
		int docstatus = rs.getInt("docstatus");
		data.put("docstatus",docstatus <=0 ? "0" : (docstatus + ""));
		String doccode = rs.getString("doccode");
		data.put("doccode",doccode);
		String docapprovedate = Util.null2String(rs.getString("docapprovedate"));
		data.put("docapprovedate",docapprovedate);
		data.put("doceditionid",rs.getString("doceditionid"));
		data.put("ownerid",rs.getString("ownerid"));
		data.put("keyword",rs.getString("keyword"));
		data.put("docapproveuserid",rs.getString("docapproveuserid"));
		data.put("docarchiveuserid",rs.getString("docarchiveuserid"));
		data.put("doccanceluserid",rs.getString("doccanceluserid"));
		data.put("docinvaldate",rs.getString("docinvaldate"));
		data.put("maindoc",rs.getString("maindoc"));
		data.put("hrmresid",rs.getString("hrmresid"));
		data.put("assetid",rs.getString("assetid"));
		data.put("crmid",rs.getString("crmid"));
		data.put("projectid",rs.getString("projectid"));
		List<DocParam> params = new ArrayList<DocParam>();
		boolean canShowDocMain = false;
		params = getDocMain(rs.getString("DOCPUBLISHTYPE"),"",params,canShowDocMain,docid,user);

		if(params.size()>0) {
			data.put("docMain",StringEscapeUtils.unescapeHtml(Util.null2String(params.get(0).getValue())));//摘要
		}
		else {
			data.put("docMain","");//摘要
		}

		for(String column : docColumns){
			data.put(column,PatternUtil.formatJson2Js(Util.null2String(rs.getString(column))));
		}

		apidatas.put("status",1);
		apidatas.put("data",data);
		return apidatas;
	}

	/**
	 * 获取文档自定义属性
	 * @param docid
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public Map<String,String> getCusInfoNoRight(int docid,int seccategory, User user) throws Exception{
		Map<String,String> apidatas = new HashMap<String,String>();
		//List<DocParam> params = new ArrayList<DocParam>();
		SecCategoryDocPropertiesComInfo scdpc = new SecCategoryDocPropertiesComInfo();
//		scdpc.addDefaultDocProperties(seccategory);
//		scdpc.setTofirstRow();
		String sql1 = "select * from DocSecCategoryDocProperty where secCategoryId = ? order by viewindex";
		RecordSet rs1 = new RecordSet();
		rs1.executeQuery(sql1,seccategory);
		while(rs1.next()){
			
			if(Util.getIntValue(rs1.getString("secCategoryId")) != seccategory) continue;  //是否是该目录
			int docPropScopeId = Util.getIntValue(rs1.getString("scopeid"));
			int docPropFieldid =  Util.getIntValue(rs1.getString("fieldid"));

			RecordSet rs = new RecordSet();
			String value = "";
			String sql = "";
			String label = "";
			boolean isMobile = false;
			DepartmentComInfo dept = new DepartmentComInfo();
			CustomFieldManager cfm = new CustomFieldManager("DocCustomFieldBySecCategory",seccategory);
			cfm.getCustomFields(docPropFieldid);
			cfm.getCustomData(docid);
			if(cfm.next()){
				String fieldvalue = cfm.getData(cfm.getFieldName(""+cfm.getId()));
				label = cfm.getFieldName(""+cfm.getId());
				if(fieldvalue == null || fieldvalue.isEmpty()){
					if(!SHOW_EMPTY_PARAM) continue;
				}
				if(fieldvalue.startsWith(",")){
					fieldvalue = fieldvalue.substring(1);
				}
				fieldvalue=Util.StringReplace(fieldvalue,"\n","<br>");
				fieldvalue=Util.StringReplace(fieldvalue,"\r","");

				if(cfm.getHtmlType().equals("1")||cfm.getHtmlType().equals("2")){
					value = fieldvalue;
				}else if(cfm.getHtmlType().equals("3")){
					BrowserComInfo bci = new BrowserComInfo();
					String fieldtype = String.valueOf(cfm.getType());
					String url = bci.getBrowserurl(fieldtype); // 浏览按钮弹出页面的url
					String linkurl = bci.getLinkurl(fieldtype);// 浏览值点击的时候链接的url
                    if(!"".equals(linkurl)&&linkurl.startsWith("/")&&!linkurl.startsWith(weaver.general.GCONST.getContextPath())){
                        linkurl = weaver.general.GCONST.getContextPath()+linkurl;
                    }
					String showname = "";                               // 新建时候默认值显示的名称
					String showid = "";                                 // 新建时候默认值
					String fielddbtype=Util.null2String(cfm.getFieldDbType());
					if(fieldtype.equals("152") || fieldtype.equals("16")){
						linkurl = weaver.general.GCONST.getContextPath()+"/workflow/request/ViewRequest.jsp?requestid=";
					}
					if(fieldtype.equals("2") ||fieldtype.equals("19") || fieldtype.equals("402") ||fieldtype.equals("403")){
						showname=fieldvalue; // 日期时间
					}else if(fieldtype.equals("141")) {
						ResourceConditionManager rcm = new ResourceConditionManager();
						showname=rcm.getFormShowName(fieldvalue,user.getLanguage());
					}else if(fieldtype.equals("4")) {
						showname="<a href='javascript:void(0)' onclick=\"openFullWindowHaveBar('"+linkurl+fieldvalue+"');\">"+dept.getDepartmentname(fieldvalue)+"</a>";
					}else if(fieldtype.equals("161")){//自定义单选
						showname = "";                                   // 新建时候默认值显示的名称
						String showdesc="";
						showid =fieldvalue;                                     // 新建时候默认值
						try{
							fielddbtype = "browser." + fielddbtype.replace("browser.", "");
							Browser browser=(Browser)StaticObj.getServiceByFullname(fielddbtype, Browser.class);

							BrowserBean bb=browser.searchById(showid);

							String desc=Util.null2String(bb.getDescription());
							String name=Util.null2String(bb.getName());
							showname="<a title='"+desc+"'>"+name+"</a>&nbsp";
						}catch(Exception e){

						}
					}else if(fieldtype.equals("162")){//自定义多选
						showname = "";                                   // 新建时候默认值显示的名称
						showid =fieldvalue;                                     // 新建时候默认值
						try{
							fielddbtype = "browser." + fielddbtype.replace("browser.", "");
							Browser browser=(Browser)StaticObj.getServiceByFullname(fielddbtype, Browser.class);
							List l = Util.TokenizerString(showid,",");
							for(int jindex = 0;jindex < l.size();jindex++){
								String curid=(String)l.get(jindex);
								BrowserBean bb=browser.searchById(curid);
								String name=Util.null2String(bb.getName());
								String desc=Util.null2String(bb.getDescription());
								showname+="<a title='"+desc+"'>"+name+"</a>&nbsp";
							}
						}catch(Exception e){
						}
					} else if(!fieldvalue.equals("")) {
						String tablename=bci.getBrowsertablename(fieldtype); //浏览框对应的表,比如人力资源表
						String columname=bci.getBrowsercolumname(fieldtype); //浏览框对应的表名称字段
						String keycolumname=bci.getBrowserkeycolumname(fieldtype);   //浏览框对应的表值字段
						sql = "";
						Map<String,String> temRes = new HashMap<String,String>();
						if(fieldtype.equals("17")|| fieldtype.equals("18")||fieldtype.equals("27")||fieldtype.equals("37")||fieldtype.equals("56")||fieldtype.equals("57")||fieldtype.equals("65")||fieldtype.equals("142")||fieldtype.equals("152")||fieldtype.equals("168")||fieldtype.equals("171")||fieldtype.equals("166")||fieldtype.equals("135")) {    // 多人力资源,多客户,多会议，多文档，多流程
							sql= "select "+keycolumname+","+columname+" from "+tablename+" where "+keycolumname+" in( "+fieldvalue+")";
						} else if(fieldtype.equals("143")){
							//树状文档字段
							String tempFieldValue="0";
							int beginIndex=0;
							int endIndex=0;
							if(fieldvalue.startsWith(",")){
								beginIndex=1;
							}else{
								beginIndex=0;
							}
							if(fieldvalue.endsWith(",")){
								endIndex=fieldvalue.length()-1;
							}else{
								endIndex=fieldvalue.length();
							}
							if(fieldvalue.equals(",")){
								tempFieldValue="0";
							}else{
								tempFieldValue=fieldvalue.substring(beginIndex,endIndex);
							}
							sql= "select "+keycolumname+","+columname+" from "+tablename+" where "+keycolumname+" in( "+tempFieldValue+")";

						} else {
							sql= "select "+keycolumname+","+columname+" from "+tablename+" where "+keycolumname+" in(" + fieldvalue + ")";
						}

						rs.executeQuery(sql);
						while(rs.next()){
							showid = Util.null2String(rs.getString(1));
							String tempshowname= Util.toScreen(rs.getString(2),user.getLanguage());
							if(!linkurl.equals("")) {
								if(isMobile){
									temRes.put(String.valueOf(showid),tempshowname);
								}else{
									temRes.put(String.valueOf(showid),"<a href='javascript:void(0)' onclick=\"openFullWindowHaveBar('"+linkurl+showid+"');\">"+tempshowname+"</a> ");
								}
							} else {
								temRes.put(String.valueOf(showid),tempshowname);
							}
						}
						StringTokenizer temstk = new StringTokenizer(fieldvalue,",");
						String temstkvalue = "";
						while(temstk.hasMoreTokens()){
							temstkvalue = temstk.nextToken();
							if(temstkvalue.length()>0&&temRes.get(temstkvalue)!=null){
								showname += temRes.get(temstkvalue);
							}
						}

					}
					if(isMobile){
						value = Util.toScreen(showname,user.getLanguage());
					}else{
						value = "<span id='customfield" + cfm.getId() + "span'>" + Util.toScreen(showname,user.getLanguage()) + "</span>";
					}

				} else if(cfm.getHtmlType().equals("4")) {
					if(isMobile){
					}else {
						value = "<input type='checkbox' value='1' name='customfield" + cfm.getId() + "checkbox' " + (fieldvalue.equals("1")? "checked" : "") + " disabled >";
					}

				} else if(cfm.getHtmlType().equals("5")) {
					cfm.getSelectItem(cfm.getId());
					while(cfm.nextSelect()){
						if(cfm.getSelectValue().equals(fieldvalue)){
							value += cfm.getSelectName();
							break;
						}
					}
				}
			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				value = PatternUtil.formatJson2Js(value);
				apidatas.put(label.toUpperCase(),value);
			}
		}
		//apidatas.put("status",1);
		//apidatas.put("data",params);
		return apidatas;
	}
	
	
	public int getDocMouldId(int docid,String filetype){
		
		//取得模版设置
		int wordmouldid = 0;
		List<String> selectMouldList = new ArrayList<String>();
		int selectMouldType = 0;
		int selectDefaultMould = 0;
		
		
		RecordSet rs = new RecordSet();
		if(docid<=0)
			return 0;
		rs.executeQuery("select seccategory,selectedpubmouldid,invalidationdate from docdetail where id=?",docid);
		if(!rs.next())
			return 0;
		int seccategory = Util.getIntValue(rs.getString("seccategory"));
		int selectedpubmouldid = Util.getIntValue(rs.getString("selectedpubmouldid"));
		String invalidationdate = rs.getString("invalidationdate");

		if(filetype.equals(".doc")||filetype.equals(".wps")){
			String mouldType=".doc".equals(filetype)?"3":"7";
			
			boolean isTemporaryDoc = false;
			if(invalidationdate!=null&&!"".equals(invalidationdate))
			    isTemporaryDoc = true;
			
			rs.executeQuery("select * from DocSecCategoryMould where secCategoryId = ? and mouldType=? order by id ",seccategory,mouldType);
			while(rs.next()){
				String moduleid=rs.getString("mouldId");
				String modulebind = rs.getString("mouldBind");
				int isDefault = Util.getIntValue(rs.getString("isDefault"),0);
		
				if(isTemporaryDoc){
					if(Util.getIntValue(modulebind,1)==3){
					    selectMouldType = 3;
					    selectDefaultMould = Util.getIntValue(moduleid);
					    selectMouldList.add(moduleid);
				    } else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
				        if(selectMouldType==0){
					        selectMouldType = 1;
						    selectDefaultMould = Util.getIntValue(moduleid);
				        }
						selectMouldList.add(moduleid);
				    } else {
				        if(Util.getIntValue(modulebind,1)!=2)
							selectMouldList.add(moduleid);
				    }
				} else {
					if(Util.getIntValue(modulebind,1)==2){
					    selectMouldType = 2;
					    selectDefaultMould = Util.getIntValue(moduleid);
					    selectMouldList.add(moduleid);
				    } else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
				        selectMouldType = 1;
					    selectDefaultMould = Util.getIntValue(moduleid);
						selectMouldList.add(moduleid);
				    } else {
				        if(Util.getIntValue(modulebind,1)!=3)
							selectMouldList.add(moduleid);
				    }
				}
				
			}
			
	    	if(selectedpubmouldid<=0){
	    	    if(selectMouldType>0)
	    	        wordmouldid = selectDefaultMould;
	    	} else {
	    	    wordmouldid = selectedpubmouldid;
	    	}
			
		}
		if((wordmouldid<=0)){
			try{
				wordmouldid = new MouldManager().getDefaultWordMouldId();
			}catch(Exception e){
				
			}
		}
		
		
		return wordmouldid > 0 ? wordmouldid : 0;
		
	}

	/**
	 * 获取文档基本属性( 标题、修改人、修改时间)
	 * @return
	 * 	{
	 * 		status 	: 	1-成功，其他-失败
	 * 		msg 	: 	status不为1时，错误信息
	 * 		data 	:	{
	 * 						docSubject : 文档标题
	 * 						doclastmoduserid：最后修改人id
	 * 						doclastmoduser:最后修改人名称
	 * 						doclastmoddatetime：最后修改时间（日期+时间）
	 * 						accessorycount:附件数
	 * 						replaydoccount:回复数
	 * 					}
	 * 	}
	 * */
	public Map<String,Object> getBasicInfo(int docid,User user,Map<String,String> params) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		
		int imagefileid = Util.getIntValue(params.get("imagefileId"));
		if(imagefileid  > 0){  //是附件
			int versionId = Util.getIntValue(params.get("versionId"),0); 
			
			DocAccService accService = new DocAccService();
			accService.setRequest(request);
			
			apidatas = accService.getBasicInfo(docid,imagefileid,versionId,user,params);
			
			return apidatas;
		}
		
		apidatas = getBasicInfoNoRight(docid,user,null);
		
		if("-1".equals(apidatas.get("status"))){
			return apidatas;
		}
		
		List<RightMenu> rightMenus = user == null ? new ArrayList<RightMenu>() : getRightMenus(docid,user,params);
		
		apidatas.put("rightMenus",rightMenus);
		
		apidatas.put("status",1);
		return apidatas;
	}
	
	/**
	 *  获取文档内容
	 *  @author wangqs
	 *  * @return
	 * 	{
	 * 		status 	: 	1-成功，其他-失败
	 * 		msg 	: 	status不为1时，错误信息
	 * 		data 	:	{
	 * 						doccontent : 文档内容
	 * 					}
	 * 	}
	 * */
	public String getDocContent(int docid,User user){
		//Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			//apidatas.put("status",-1);
			//apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			//return apidatas;
			return "";
		}
		
		RecordSet rs = new RecordSet();

		String dbtype = rs.getDBType();
		
		String sql = "";
		if("oracle".equals(dbtype) || "mysql".equals(dbtype)){
			sql = "select c.doccontent,d.docpublishtype from docdetailcontent c,DocDetail d where d.id=c.docid and docid=" + docid;
		}else{
			sql = "select doccontent,docpublishtype from Docdetail where id=" + docid;
		}
		rs.executeQuery(sql);
		if(!rs.next()){
			//apidatas.put("status",-1);
			//apidatas.put("msg",SystemEnv.getHtmlLabelName(23230,user.getLanguage()));
			return "";
		}
		
		String docpublishtype = rs.getString("docpublishtype");
		String doccontent = rs.getString("doccontent");
		doccontent = this.formatDocContent(doccontent,docpublishtype);
		

		doccontent = PatternUtil.formatJson2Js(doccontent);
		return doccontent;
		//apidatas.put("status",1);
		//Map<String,String> data = new HashMap<String,String>();
		//data.put("doccontent",doccontent);
		//apidatas.put("data",data);
		//return apidatas;
	}
	
	/**
	 * 获取文档tab
	 * */
	public Map<String,Object> getDocDetailTab(int docid,User user){
		Map<String,Object> apidatas = new HashMap<String,Object>();

		int languageid = user == null ? DocSptm.getDefaultLanguage() : user.getLanguage();
		
		List<Map<String,String>> dataList =  new ArrayList<Map<String,String>>();
		Map<String,String> content = new HashMap<String,String>();

		content.put("key",DOC_CONTENT);
		content.put("selected","true");
		content.put("value",SystemEnv.getHtmlLabelName(500566,languageid));// 文档正文
		dataList.add(content);
		
		Map<String,String> acc = new HashMap<String,String>();
		acc.put("key",DOC_ACC);
		acc.put("value",SystemEnv.getHtmlLabelName(31208,languageid)); //31208 文档附件
		dataList.add(acc);
		
		if(user == null){
			apidatas.put("tabInfo",dataList);
			return apidatas;
		}
		
		RecordSet rs1 = new RecordSet();
		Map<String,String> userAllRole = getUserAllRoles(user.getUID());
		String sql = "";
		sql="SELECT roleid,isopen from HRMDOCROLE";
		rs1.executeSql(sql);
		Boolean isshowtab = false;
		int roleidnum = 0;
		while(rs1.next()){
			roleidnum ++;
			String roleid = Util.null2s(rs1.getString("roleid"),"");
			int int_roleid = Util.getIntValue(roleid,-1);
			String isopen = Util.null2s(rs1.getString("isopen"),"");
			if ((null != userAllRole.get(roleid) && !"".equals(roleid) && int_roleid > 0)||isopen.equals("0")){
				isshowtab = true;
			}
		}
		if(roleidnum<=0){
			//未设置开关时，默认展示tab
			isshowtab = true;
		}

		if(isshowtab){
			Map<String,String> param = new HashMap<String,String>();
			param.put("key",DOC_PARAM);
			param.put("value",SystemEnv.getHtmlLabelName(33197,languageid)); //33197 文档属性
			dataList.add(param);
		}
		
		RecordSet rs = new RecordSet();
		rs.executeQuery("select c.logviewtype,c.editionIsOpen,c.markable,c.relationable,c.replyable,d.docstatus " +
				"from DocSecCategory c,DocDetail d where c.id=d.seccategory and d.id=" + docid);
		rs.next();
		
		String docstatus = rs.getString("docstatus");

		if(isshowtab){
			Map<String,String> share = new HashMap<String,String>();
			share.put("key",DOC_SHARE);
			share.put("value",SystemEnv.getHtmlLabelName(1985,languageid));//1985 文档共享
			dataList.add(share);
		}

		if("1".equals(rs.getString("editionIsOpen"))){
			Map<String,String> version = new HashMap<String,String>();
			version.put("key",DOC_VERSION);
			version.put("value",SystemEnv.getHtmlLabelName(19543,languageid)); //19543文档版本
			dataList.add(version);
		}
		if(isshowtab){
			UserManager um = new UserManager();
			User userTmp = um.getUserByUserIdAndLoginType(user.getUID(), user.getLogintype());
			DocViewPermission docViewPermission = new DocViewPermission();
			Map<String,Boolean> rightMap = docViewPermission.getShareLevel(docid, user, false);
			Boolean canEdit = rightMap.get(DocViewPermission.EDIT);
			if (!"2".equals(user.getLogintype()) &&
					(!"1".equals(rs.getString("logviewtype")) || (canEdit || HrmUserVarify.checkUserRight("FileLogView:View", userTmp)))){
				Map<String,String> log = new HashMap<String,String>();
				log.put("key",DOC_LOG);
				log.put("value",SystemEnv.getHtmlLabelName(21990,languageid)); //21990 文档日志
				dataList.add(log);
			}
		}


		if("1".equals(rs.getString("markable"))){
			Map<String,String> score = new HashMap<String,String>();
			score.put("key",DOC_SCORE);
			score.put("value",SystemEnv.getHtmlLabelName(125667,languageid)); //125667 文档打分
			dataList.add(score);
		}
		
		if("1".equals(rs.getString("replyable"))  && ("5".equals(docstatus) || "2".equals(docstatus) || "1".equals(docstatus))){
			Map<String,String> reply = new HashMap<String,String>();
			reply.put("key",DOC_REPLY);
			reply.put("value",SystemEnv.getHtmlLabelName(125666,languageid));//文档回复
			dataList.add(reply);
		}
		
		if("1".equals(rs.getString("relationable")) && false){
			Map<String,String> ref = new HashMap<String,String>();
			ref.put("key",DOC_REF);
			ref.put("value",SystemEnv.getHtmlLabelName(22672,languageid));//相关资源
			dataList.add(ref);
		}
		
		if(false){
			Map<String,String> childrens = new HashMap<String,String>();
			childrens.put("key",DOC_CHILD);
			childrens.put("value",SystemEnv.getHtmlLabelName(500571,languageid));  //131481-子列表,131470-子文档列表 500571-子文档
			dataList.add(childrens);
		}

//		if(isshowtab){
//			Map<String,String> param = new HashMap<String,String>();
//			param.put("key",DOC_PARAM);
//			param.put("value",SystemEnv.getHtmlLabelName(33197,languageid)); //33197 文档属性
//			dataList.add(param);
//
//			Map<String,String> share = new HashMap<String,String>();
//			share.put("key",DOC_SHARE);
//			share.put("value",SystemEnv.getHtmlLabelName(1985,languageid));//1985 文档共享
//			dataList.add(share);
//
//			UserManager um = new UserManager();
//			User userTmp = um.getUserByUserIdAndLoginType(user.getUID(), user.getLogintype());
//			if (!"2".equals(user.getLogintype()) &&
//					(!"1".equals(rs.getString("logviewtype")) || HrmUserVarify.checkUserRight("FileLogView:View", userTmp))){
//				Map<String,String> log = new HashMap<String,String>();
//				log.put("key",DOC_LOG);
//				log.put("value",SystemEnv.getHtmlLabelName(21990,languageid)); //21990 文档日志
//				dataList.add(log);
//			}
//		}
		
		apidatas.put("tabInfo",dataList);
		
		return apidatas;
	}

	/**
	 * @param userid 用户ID
	 * @return 用户所具有的角色
	 */
	public String getUserAllRole(int userid){
		String returnStr="";
//    	RecordSet rs = new RecordSet();
//    	rs.executeSql("select roleid,rolelevel from hrmrolemembers where resourceid="+userid);

		HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
		List<Object> roleInfos = hrmCommonService.getRoleInfo(userid);

		for(Object ro : roleInfos) {
			Map<String, Object> roleInfo  = (Map<String, Object>) ro;
			returnStr+=Util.null2String(roleInfo.get("roleid"))+",";
		}
//    	while(rs.next()){
//    		returnStr+=Util.null2String(rs.getString("roleid"))+Util.null2String(rs.getString("rolelevel"))+",";
//    	}
		//if(!"".equals(returnStr)) returnStr=returnStr.substring(0,returnStr.length()-1); //形成(123,213,421)这种形式
		if(returnStr.equals("")){
			returnStr="-10";
		}
		return returnStr;
	}
	
	/**
	 * @param userid 用户ID
	 * @return 用户所具有的角色
	 */
	public Map<String,String> getUserAllRoles(int userid){
		String returnStr="";
		Map<String,String> result =  new HashMap<String,String>();

		HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
		List<Object> roleInfos = hrmCommonService.getRoleInfo(userid);

		for(Object ro : roleInfos) {
			Map<String, Object> roleInfo  = (Map<String, Object>) ro;
			result.put(roleInfo.get("roleid")+"",roleInfo.get("roleid")+"");
		}

		return result;
	}
	
	
	/**
	 * 获取文档属性
	 * @author wangqs
	 * @return 
	 * {
	 * 		status 	: 	1-成功，其他-失败
	 * 		msg 	: 	status不为1时，错误信息
	 * 		data 	:	DocParam
	 * 	}
	 * */
	public Map<String,Object> getDocParamInfo(int docid,User user) throws Exception{
		return getDocParamInfo(docid,user,false);
	}
	public Map<String,Object> getDocParamInfo(int docid,User user,boolean isMobile) throws Exception{
	    String ecologyContentPath = weaver.general.GCONST.getContextPath();
		Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			return apidatas;
		}
		Date d0 = new Date();
		String sql = "select *from DocDetail where id=?";
		RecordSet rs = new RecordSet();
		rs.executeQuery(sql,docid);
		if(!rs.next()){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(23230,user.getLanguage()));
			return apidatas;
		}
		
		Map<String,String> detail = new HashMap<String,String>();
		for(String column : rs.getColumnName()){
			detail.put(column.toUpperCase(),Util.null2String(rs.getString(column)));
		}
		Date d1 = new Date();
		/** 文档属性 start **/
		int seccategory = Util.getIntValue(detail.get("SECCATEGORY"),0);
		SecCategoryDocPropertiesComInfo scdpc = new SecCategoryDocPropertiesComInfo();
		RecordSet docPropCacheRs = new RecordSet();
		docPropCacheRs.executeSql("select * from DocSecCategoryDocProperty where secCategoryId= "+seccategory+"  order by viewindex ");
//		scdpc.addDefaultDocProperties(seccategory);
		Date d2 = new Date();
//		scdpc.setTofirstRow();
		Date d3 = new Date();
		List<DocParam> params = new ArrayList<DocParam>();
		
		DocComInfo dci = new DocComInfo();
		DepartmentComInfo dept = new DepartmentComInfo();
		
		DocSptm ds = new DocSptm();
		boolean secretFlag = CheckPermission.isOpenSecret();
		boolean iscus_field = false;
		String cus_fieldname = "";
		while(docPropCacheRs.next()){
			if(Util.getIntValue(docPropCacheRs.getString("secCategoryId")) != seccategory) continue;  //是否是该目录
			if(Util.getIntValue(docPropCacheRs.getString("visible")) == 0) continue;  //是否显示
			int docPropLabelid = Util.getIntValue(docPropCacheRs.getString("labelid"));  //自定义属性 标签id
			int docPropType = Util.getIntValue(docPropCacheRs.getString("type")); //属性类型
			int docPropFieldid = Util.getIntValue(docPropCacheRs.getString("fieldid"));
			if(docPropLabelid == MultiAclManager.MAINCATEGORYLABEL 
					|| docPropLabelid == MultiAclManager.SUBCATEGORYLABEL) continue;
			if(docPropType == 1) continue;  //（1-文档标题）
			if(docPropType == 26 && !secretFlag){ //文档密级
				continue;
			}
			int docPropIsCustom = Util.getIntValue(docPropCacheRs.getString("isCustom"));  //是否自定义

			String docPropCustomName ="";
			if(user.getLanguage()==7){
				docPropCustomName = Util.null2String(docPropCacheRs.getString("customName"));
			}else if(user.getLanguage()==8){
				docPropCustomName = Util.null2String(docPropCacheRs.getString("customNameEng"));
			}else if(user.getLanguage()==9){
				docPropCustomName = Util.null2String(docPropCacheRs.getString("customNameTran"));
			}
			String label = "";
            if (!docPropCustomName.equals("")&&(!"0".equals(docPropCustomName))) {
                label = docPropCustomName;
            } else if (docPropIsCustom != 1) {
                label = SystemEnv.getHtmlLabelName(docPropLabelid, user
                        .getLanguage());
            }else if(docPropIsCustom == 1){
                rs.executeQuery("select fieldlabel from cus_formdict where id="+docPropFieldid);
                if(rs.next())
                    label = Util.null2String(rs.getString("fieldlabel"));
                if(label.equals(""))label = "field"+docPropFieldid;

            }
			iscus_field = false;
			cus_fieldname="";
			String value = "";
			boolean isFull = Util.getIntValue(docPropCacheRs.getString("columnWidth")) == 2;
			switch(docPropType){
				case 1 : //1 文档标题
					break;
				case 2 : //2 文档编号
					value = detail.get("DOCCODE");
					break;
				case 3	: //发布
					if("2".equals(detail.get("DOCPUBLISHTYPE"))){
						value =SystemEnv.getHtmlLabelName(227,user.getLanguage());
					}else if("3".equals(detail.get("DOCPUBLISHTYPE"))){
						value = SystemEnv.getHtmlLabelName(229,user.getLanguage());
					}else{
						value = SystemEnv.getHtmlLabelName(58,user.getLanguage());
					}
					break;
				case 4://4 文档版本
					value = dci.getEditionView(docid);
					break;
				case 5://5 文档状态
					value = dci.getStatusView(docid,user);
					break;
				case 6://6 主目录
					break;
				case 7://7 分目录
					break;
				case 8://8 子目录
					SecCategoryComInfo scci = new SecCategoryComInfo();
					value = scci.getAllParentName("" + seccategory,true);
       	           	value = Util.replace(value,"&amp;quot;","\"",0);
                    value = Util.replace(value,"&quot;","\"",0);
                    value = Util.replace(value,"&lt;","<",0);
                    value = Util.replace(value,"&gt;",">",0);
                    value = Util.replace(value,"&apos;","'",0);
					break;
				case 9://9 部门
					String deptname = dept.getDepartmentname(detail.get("DOCDEPARTMENTID"));
					if(deptname != null && !deptname.isEmpty()){
						if(isMobile){
							value = ds.getDepartmentLink(detail.get("DOCDEPARTMENTID"),deptname,isMobile);
						}else {
							value = ds.getDepartmentLink(detail.get("DOCDEPARTMENTID"),deptname);
						}
					}
					break;
				case 10://10 模版
					int docmouldid = 0;
					List<String> selectMouldList = new ArrayList<String>();
					int selectMouldType = 0;
					int selectDefaultMould = 0;
					
					boolean isTemporaryDoc = !"".equals(detail.get("INVALIDATIONDATE"));
					
					//if("1".equals(detail.get("DOCTYPE"))){
						DocMouldComInfo dmc = new DocMouldComInfo();
						rs.executeQuery("select t1.* from DocSecCategoryMould t1 right join DocMould t2 on t1.mouldId = t2.id where t1.secCategoryId = ? and t1.mouldType=1 order by t1.id",seccategory);
						while(rs.next()){
							String moduleid=rs.getString("mouldId");
							String modulebind = rs.getString("mouldBind");
							int isDefault = Util.getIntValue(rs.getString("isDefault"),0);

							if(isTemporaryDoc){
								if(Util.getIntValue(modulebind,1)==3){
								    selectMouldType = 3;
								    selectDefaultMould = Util.getIntValue(moduleid);
								    selectMouldList.add(moduleid);
							    } else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
							        if(selectMouldType==0){
								        selectMouldType = 1;
									    selectDefaultMould = Util.getIntValue(moduleid);
							        }
									selectMouldList.add(moduleid);
							    } else {
							        if(Util.getIntValue(modulebind,1)!=2)
										selectMouldList.add(moduleid);
							    }
								
							} else {
							    
								if(Util.getIntValue(modulebind,1)==2){
								    selectMouldType = 2;
								    selectDefaultMould = Util.getIntValue(moduleid);
								    selectMouldList.add(moduleid);
							    } else if(Util.getIntValue(modulebind,1)==1&&isDefault==1){
							        selectMouldType = 1;
								    selectDefaultMould = Util.getIntValue(moduleid);
									selectMouldList.add(moduleid);
							    } else {
							        if(Util.getIntValue(modulebind,1)!=3)
										selectMouldList.add(moduleid);
							    }
							}
						}
						
						if(docmouldid<=0){
							MouldManager mouldManager = new MouldManager();
							docmouldid = mouldManager.getDefaultMouldId();
						}
						
						int selectedpubmouldid = Util.getIntValue(detail.get("SELECTEDPUBMOULDID"),0);
						
						if(false && HrmUserVarify.checkUserRight("Docit:Publish",user,detail.get("DOCDEPARTMENTID")) 
								&& "6".equals(detail.get("DOCSTATUS"))){
							
							if(selectMouldType>0){
							    docmouldid = selectDefaultMould;
						    }

							value = "<select class='InputStyle' name='selectedpubmould' " +
									((selectMouldType==2||(isTemporaryDoc&&selectMouldType==3)) ? "disabled" : "") +
									" style='width:200' onchange='onChangeDocModule(this.value)'>";
							if(selectMouldType < 2){
								value += "<option value='-1'></option>";
							}
							for(int i = 0;i < selectMouldList.size();i++){
							  	String moduleid = selectMouldList.get(i);
								String modulename = dmc.getDocMouldname(moduleid);
							    String mType = dmc.getDocMouldType(moduleid);
							    String mouldTypeName = "";
							    if((mType.equals("")||mType.equals("0")||mType.equals("1")) && "1".equals(detail.get("DOCTYPE"))) {
							        mouldTypeName="HTML";
							    } else if(mType.equals("2") && "2".equals(detail.get("DOCTYPE"))) {
							        mouldTypeName="WORD";
							       // continue;
							    } else if(mType.equals("3") && "2".equals(detail.get("DOCTYPE"))){
							        mouldTypeName="EXCEL";
							       // continue;
							    } else if(mType.equals("4") && "2".equals(detail.get("DOCTYPE"))){
							    	mouldTypeName="WPS";
							    }else {
							        continue;
							    }
								String isselect ="" ;
								if(docmouldid==Util.getIntValue(moduleid)) isselect = " selected";
								value += "<option value='" + moduleid + "' " + isselect + " >" + modulename + "(" + mouldTypeName + ")</option>";
							}
							value += "</select>";
						}else{
					    	if(selectedpubmouldid<=0){
					    	    if(selectMouldType>0)
					    	        docmouldid = selectDefaultMould;
					    	} else {
					    	    docmouldid = selectedpubmouldid;
					    	}
					    	
					    	String mouldname = dmc.getDocMouldname(docmouldid+"");
					    	if(mouldname != null && !mouldname.isEmpty()){
					    		/*value = "<a href='/docs/mould/DocMouldDsp.jsp?id=" + docmouldid + "'    target='_blank'>" +
										dmc.getDocMouldname(docmouldid+"") +
									"</a>";*/
					    		value = ds.getMouldView(docmouldid + "",mouldname,isMobile);
					    	}
						}
					//}	
					break;
				case 11://11 语言
					LanguageComInfo lci = new LanguageComInfo();
					value = lci.getLanguagename(detail.get("DOCLANGURAGE"));
					break;
				case 12://12 关键字
					if(!detail.get("KEYWORD").isEmpty()){
						if(isMobile){
							value = detail.get("KEYWORD");
						}else{
							value = "<a target='_blank' href='" + DocSptm.DOC_LINK + DocSptm.DOC_SEARCH_LINK_ROUT + "?keyword=" + detail.get("KEYWORD") + "'>" + detail.get("KEYWORD") + "</a>";
						}

					}
					break;
				case 13:	//13 创建
					if(isMobile){
						value = getUserLink(detail.get("DOCCREATERID"),detail.get("DOCCREATERTYPE"),user,isMobile);
					}else{
						value = getUserLink(detail.get("DOCCREATERID"),detail.get("DOCCREATERTYPE"),user);
					}
					String time_DOCCREATEDATETime = detail.get("DOCCREATEDATE")+" "+ detail.get("DOCCREATETIME");
					if(time_DOCCREATEDATETime.length() > 11){
						time_DOCCREATEDATETime = TimeZoneUtil.getYmdLocaleDate1(time_DOCCREATEDATETime);
						String time_DOCCREATEDATE = time_DOCCREATEDATETime.substring(0,10);
						String time_DOCCREATETIME = time_DOCCREATEDATETime.substring(11) ;
						value += "&nbsp;" + time_DOCCREATEDATE + "&nbsp;" + time_DOCCREATETIME;
					}	
					break;
				case 14://14 修改
					if(isMobile){
						value = getUserLink(detail.get("DOCLASTMODUSERID"),detail.get("DOCLASTMODUSERTYPE"),user,isMobile);
					}else{
						value = getUserLink(detail.get("DOCLASTMODUSERID"),detail.get("DOCLASTMODUSERTYPE"),user);
					}
					String time_DOCLASTMODDATETime = detail.get("DOCLASTMODDATE")+" "+ detail.get("DOCLASTMODTIME");
					if(time_DOCLASTMODDATETime.length() > 11){
						time_DOCLASTMODDATETime = TimeZoneUtil.getYmdLocaleDate1(time_DOCLASTMODDATETime);
						String time_DOCLASTMODDATE = time_DOCLASTMODDATETime.substring(0,10);
						String time_DOCLASTMODTIME = time_DOCLASTMODDATETime.substring(11) ;
						value += "&nbsp;" + time_DOCLASTMODDATE + "&nbsp;" + time_DOCLASTMODTIME;
					}
					break;
				case 15://15 批准
					if(Util.getIntValue(detail.get("DOCAPPROVEUSERID"),0) > 0){
						if(isMobile){
							value = getUserLink(detail.get("DOCAPPROVEUSERID"),detail.get("DOCAPPROVEUSERTYPE"),user,isMobile);
						}else{
							value = getUserLink(detail.get("DOCAPPROVEUSERID"),detail.get("DOCAPPROVEUSERTYPE"),user);
						}
						String time_DOCAPPROVEDATETime = detail.get("DOCAPPROVEDATE")+" "+ detail.get("DOCAPPROVETIME");
						if(time_DOCAPPROVEDATETime.length() > 11){
							time_DOCAPPROVEDATETime = TimeZoneUtil.getYmdLocaleDate1(time_DOCAPPROVEDATETime);
							String time_DOCAPPROVEDATE = time_DOCAPPROVEDATETime.substring(0,10);
							String time_DOCAPPROVETIME = time_DOCAPPROVEDATETime.substring(11) ;
	
							value += "&nbsp;" + time_DOCAPPROVEDATE + "&nbsp;" + time_DOCAPPROVETIME;
						}
					}
					break;
				case 16://16 失效
					if(Util.getIntValue(detail.get("DOCINVALUSERID"),0) > 0){
						if(isMobile){
							value = getUserLink(detail.get("DOCINVALUSERID"),detail.get("DOCINVALUSERTYPE"),user,isMobile);
						}else{
							value = getUserLink(detail.get("DOCINVALUSERID"),detail.get("DOCINVALUSERTYPE"),user);
						}

						String time_DOCINVALDATETime = detail.get("DOCINVALDATE")+" "+ detail.get("DOCINVALTIME");
						if(time_DOCINVALDATETime.length() > 11){
							time_DOCINVALDATETime = TimeZoneUtil.getYmdLocaleDate1(time_DOCINVALDATETime);
							String time_DOCINVALDATE = time_DOCINVALDATETime.substring(0,10);
							String time_DOCINVALTIME = time_DOCINVALDATETime.substring(11) ;

							value += "&nbsp;" + time_DOCINVALDATE + "&nbsp;" + time_DOCINVALTIME;
						}
					}
					break;
				case 17://17 归档
					if(Util.getIntValue(detail.get("DOCARCHIVEUSERID"),0) > 0){
						if(isMobile){
							value = getUserLink(detail.get("DOCARCHIVEUSERID"),detail.get("DOCARCHIVEUSERTYPE"),user,isMobile);
						}else {
							value = getUserLink(detail.get("DOCARCHIVEUSERID"),detail.get("DOCARCHIVEUSERTYPE"),user);
						}

						String time_DOCARCHIVEDATETime = detail.get("DOCARCHIVEDATE")+" "+ detail.get("DOCARCHIVETIME");
						if(time_DOCARCHIVEDATETime.length() > 11){
							time_DOCARCHIVEDATETime = TimeZoneUtil.getYmdLocaleDate1(time_DOCARCHIVEDATETime);
							String time_DOCARCHIVEDATE = time_DOCARCHIVEDATETime.substring(0,10);
							String time_DOCARCHIVETIME = time_DOCARCHIVEDATETime.substring(11) ;
							value += "&nbsp;" + time_DOCARCHIVEDATE + "&nbsp;" + time_DOCARCHIVETIME;
						}
					}
					break;	
				case 18://18 作废
					if(Util.getIntValue(detail.get("DOCCANCELUSERID"),0) > 0){
						if(isMobile){
							value = getUserLink(detail.get("DOCCANCELUSERID"),detail.get("DOCCANCELUSERTYPE"),user,isMobile);
						}else{
							value = getUserLink(detail.get("DOCCANCELUSERID"),detail.get("DOCCANCELUSERTYPE"),user);
						}
						String time_DOCCANCELDATETime = detail.get("DOCCANCELDATE")+" "+ detail.get("DOCCANCELTIME");
						if(time_DOCCANCELDATETime.length() > 11){
							time_DOCCANCELDATETime = TimeZoneUtil.getYmdLocaleDate1(time_DOCCANCELDATETime);
							String time_DOCCANCELDATE = time_DOCCANCELDATETime.substring(0,10);
							String time_DOCCANCELTIME = time_DOCCANCELDATETime.substring(11) ;
	
							value += "&nbsp;" + time_DOCCANCELDATE + "&nbsp;" + time_DOCCANCELTIME;
						}
					}
					break;	
				case 19://19 主文档
					int maindoc = Util.getIntValue(detail.get("MAINDOC"));
					if(maindoc == docid){ 
						value = SystemEnv.getHtmlLabelName(390563,user.getLanguage());//390563 当前文档
					}else if(maindoc > 0){
						if(isMobile){
							value = dci.getDocname(maindoc + "");
						}else {
							value = "<a target='_blank' href='"+DocSptm.DOC_DETAIL_LINK + "?id=" + maindoc + DocSptm.DOC_ROOT_FLAG_VALUE + DocSptm.DOC_DETAIL_ROUT + "'>" + dci.getDocname(maindoc + "") + "</a>";
						}

					}
					break;
				case 20://20 被引用列表
					int newDocId = -1;
					String docSubject = "";
					if(Util.getIntValue(detail.get("DOCEDITIONID"),-1) > -1){
						rs.executeQuery(" select id,docSubject from DocDetail where doceditionid = " + detail.get("DOCEDITIONID") + " and doceditionid > 0  and (isHistory<>'1' or isHistory is null or isHistory='') order by docedition desc ");
			            rs.next();
			            newDocId = Util.getIntValue(rs.getString("id"));
			            docSubject = Util.null2String(rs.getString("docSubject"));
					} else {
						newDocId = docid;
						docSubject = detail.get("DOCSUBJECT");
					}
		            if(newDocId>0){
						if(isMobile){
							value = docSubject+"&nbsp;";
						}else{
							value = "<a href='" + DocSptm.DOC_DETAIL_LINK + "?id="+newDocId+DocSptm.DOC_ROOT_FLAG_VALUE+DocSptm.DOC_DETAIL_ROUT + "'  target='_blank'>" + docSubject + "</a>&nbsp;";
						}

				        int childDocId=-1;
						String childDocSubject = "";
						rs.executeQuery(" select id,docSubject from DocDetail where id <> " + newDocId + " and mainDoc = " + newDocId + " and (isHistory<>'1' or isHistory is null or isHistory='') order by id asc ");
						while(rs.next()){
							childDocId = Util.getIntValue(rs.getString("id"));;
							childDocSubject = Util.null2String(rs.getString("docSubject"));
							if(childDocId>0){
								if(isMobile){
									value += childDocSubject + "</a>&nbsp";
								}else {
									value += "<a href='" + DocSptm.DOC_DETAIL_LINK + "?id=" + childDocId + DocSptm.DOC_ROOT_FLAG_VALUE+DocSptm.DOC_DETAIL_ROUT + "'  target='_blank' >" + childDocSubject + "</a>&nbsp";
								}

				    		} 
				    	}
		            }
					break;
				case 21://21 文档所有者
					if(isMobile){
						value = getUserLink(detail.get("OWNERID"),detail.get("OWNERTYPE"),user,isMobile);
					}else{
						value = getUserLink(detail.get("OWNERID"),detail.get("OWNERTYPE"),user);
					}

					break;
				case 22://22 失效时间
					String invalidationdate = detail.get("INVALIDATIONDATE");
					if(invalidationdate.length() > 11){
                        invalidationdate = TimeZoneUtil.getYmdLocaleDate1(invalidationdate);
                    }
					value = invalidationdate;
					break;
				case 24://24 虚拟目录	
					String strSql="select catelogid from DocDummyDetail where docid="+docid;
					rs.executeQuery(strSql);
					String dummyIds="";
					while(rs.next()){
						dummyIds+=Util.null2String(rs.getString(1))+",";
					}
					if(!"".equals(dummyIds)) {
						dummyIds=dummyIds.substring(0,dummyIds.length()-1);
						DocTreeDocFieldComInfo dtdfc = new DocTreeDocFieldComInfo();
						
						for(String dummyId : dummyIds.split(",")){
							if(!dummyId.isEmpty()){
								String dummyName = dtdfc.getTreeDocFieldName(dummyId);
								String href = DocSptm.DOC_LINK + DocSptm.DOC_DUMMY_LINK_ROUT + "?dummyId=" + dummyId;
								if(isMobile){
									value += dummyName;
								}else{
									value += "&nbsp;" + "<a target='_blank' href='" + href + "' >" + dummyName + "</a>";
								}
							}
						}
					};
					break;
				case 25://可打印份数
					value = Util.getIntValue(detail.get("CANPRINTEDNUM"),0) + "";
					break;
				case 26 : //密级等级
					HrmClassifiedProtectionBiz hcpb = new HrmClassifiedProtectionBiz();
                	value = DocSecretLevelUtil.takeSecretLevelDefaultValue(detail.get(DocParamItem.SECRET_LEVEL.getName().toUpperCase()),user,docid+"",true);
					break;
				case 27 : //主题图片
					value = Util.null2String(detail.get("THEMESHOWPIC"));
					rs.writeLog(DocDetailService.class.getName(),">>>>>>>>>>>> DocDetailService docid="+docid+">>value="+value);
					break;
				case 0 : //自定义字段
//					int docPropFieldid = Util.getIntValue(scdpc.getFieldId());
					CustomFieldManager cfm = new CustomFieldManager("DocCustomFieldBySecCategory",seccategory);
					rs.writeLog(DocDetailService.class.getName()," docPropFieldid="+docPropFieldid+" seccategory="+seccategory+" docid="+docid);
				    cfm.getCustomFields(docPropFieldid);
					cfm.getCustomData(docid);
				    if(cfm.next()){
						rs.writeLog(DocDetailService.class.getName()," docPropFieldid="+docPropFieldid+" seccategory="+seccategory+" docid="+docid);
						iscus_field = true;
						cus_fieldname = DocEncryptUtil.getFieldName(docPropFieldid);
//						rs.writeLog(">>>>>>>>>>>> DocDetailService docid="+docid+">>docPropFieldid="+docPropFieldid+">>cus_fieldname="+cus_fieldname);
				        String fieldvalue = cfm.getData(cfm.getFieldName(""+cfm.getId()));
				        if(fieldvalue == null || fieldvalue.isEmpty()){
				        	if(!SHOW_EMPTY_PARAM) continue;
				        }
				        if(fieldvalue.startsWith(",")){
				        	fieldvalue = fieldvalue.substring(1);
				        }
				        fieldvalue=Util.StringReplace(fieldvalue,"\n","<br>");
				        fieldvalue=Util.StringReplace(fieldvalue,"\r","");
				        
				        if(cfm.getHtmlType().equals("1")||cfm.getHtmlType().equals("2")){
							rs.writeLog(DocDetailService.class.getName()," docPropFieldid="+docPropFieldid+" seccategory="+seccategory+" docid="+docid+" fieldvalue="+fieldvalue+"  getData"+cfm.getData(cus_fieldname)+" cus_fieldname="+cus_fieldname);
							fieldvalue = Util.null2String(new EncryptUtil().decrypt(DocEncryptUtil.CUS_FIELDDATA,cus_fieldname,DocEncryptUtil.SCOPE_DOCUMENTCUSTOM,""+seccategory,cfm.getData(cus_fieldname),false,true));
							rs.writeLog(DocDetailService.class.getName()," docPropFieldid="+docPropFieldid+" seccategory="+seccategory+" docid="+docid+" fieldvalue="+fieldvalue);
							value = fieldvalue;
				        }else if(cfm.getHtmlType().equals("3")){
				        	BrowserComInfo bci = new BrowserComInfo();
							String fieldtype = String.valueOf(cfm.getType());
							String url = bci.getBrowserurl(fieldtype); // 浏览按钮弹出页面的url
							String linkurl = bci.getLinkurl(fieldtype);// 浏览值点击的时候链接的url
                            if(!"".equals(linkurl)&&linkurl.startsWith("/")&&!linkurl.startsWith(ecologyContentPath)){
                                linkurl = ecologyContentPath+linkurl;
                            }
							String showname = "";                               // 新建时候默认值显示的名称
							String showid = "";                                 // 新建时候默认值
							String fielddbtype=Util.null2String(cfm.getFieldDbType());
							if(fieldtype.equals("152") || fieldtype.equals("16")){
								linkurl = ecologyContentPath+"/workflow/request/ViewRequest.jsp?requestid=";
							}
							/**
							 * 290 时间日期字段
							 * 2 日期字段
							 * 19 时间字段
							 */
							if(fieldtype.equals("2") ||fieldtype.equals("19") || fieldtype.equals("290") || fieldtype.equals("402") || fieldtype.equals("403")){
								showname = fieldvalue;
							}else if(fieldtype.equals("141")) {
								ResourceConditionManager rcm = new ResourceConditionManager();
								showname=rcm.getFormShowName(fieldvalue,user.getLanguage());
							}else if(fieldtype.equals("4")) {
								if(isMobile){
									showname = 	dept.getDepartmentname(fieldvalue);
								}else{
									showname="<a href='javascript:void(0)' onclick=\"openFullWindowHaveBar('"+linkurl+fieldvalue+"');\">"+dept.getDepartmentname(fieldvalue)+"</a>";
								}
							}else if(fieldtype.equals("161")){//自定义单选
							    showname = "";                                   // 新建时候默认值显示的名称
							    String showdesc="";
							    showid =fieldvalue;                                     // 新建时候默认值
							    try{
							    	fielddbtype = "browser." + fielddbtype.replace("browser.", "");
									Browser browser=(Browser)StaticObj.getServiceByFullname(fielddbtype, Browser.class);

									BrowserBean bb=browser.searchById(showid);

									String desc=Util.null2String(bb.getDescription());
									String name=Util.null2String(bb.getName());
									showname="<a title='"+desc+"'>"+name+"</a>&nbsp";
							    }catch(Exception e){

							    }
							}else if(fieldtype.equals("162")){//自定义多选
							    showname = "";                                   // 新建时候默认值显示的名称
							    showid =fieldvalue;                                     // 新建时候默认值
							    try{
							    	fielddbtype = "browser." + fielddbtype.replace("browser.", "");
									Browser browser=(Browser)StaticObj.getServiceByFullname(fielddbtype, Browser.class);
									List l = Util.TokenizerString(showid,",");
									for(int jindex = 0;jindex < l.size();jindex++){
										String curid=(String)l.get(jindex);
										BrowserBean bb=browser.searchById(curid);
										String name=Util.null2String(bb.getName());
										String desc=Util.null2String(bb.getDescription());
										showname+="<a title='"+desc+"'>"+name+"</a>&nbsp";
									}
							    }catch(Exception e){
							    }
							} else if(!fieldvalue.equals("")) {
								String tablename=bci.getBrowsertablename(fieldtype); //浏览框对应的表,比如人力资源表
								String columname=bci.getBrowsercolumname(fieldtype); //浏览框对应的表名称字段
								String keycolumname=bci.getBrowserkeycolumname(fieldtype);   //浏览框对应的表值字段
								sql = "";
								
								Map<String,String> temRes = new HashMap<String,String>();
								
								if(fieldtype.equals("17")|| fieldtype.equals("18")||fieldtype.equals("27")||fieldtype.equals("37")||fieldtype.equals("56")||fieldtype.equals("57")||fieldtype.equals("65")||fieldtype.equals("142")||fieldtype.equals("152")||fieldtype.equals("168")||fieldtype.equals("171")||fieldtype.equals("166")||fieldtype.equals("135")) {    // 多人力资源,多客户,多会议，多文档，多流程
									sql= "select "+keycolumname+","+columname+" from "+tablename+" where "+keycolumname+" in( "+fieldvalue+")";
								} else if(fieldtype.equals("143")){
									//树状文档字段
									String tempFieldValue="0";
									int beginIndex=0;
									int endIndex=0;
									if(fieldvalue.startsWith(",")){
										beginIndex=1;
									}else{
										beginIndex=0;
									}
									if(fieldvalue.endsWith(",")){
										endIndex=fieldvalue.length()-1;
									}else{
										endIndex=fieldvalue.length();
									}
									if(fieldvalue.equals(",")){
										tempFieldValue="0";			
									}else{
										tempFieldValue=fieldvalue.substring(beginIndex,endIndex);			
									}
									sql= "select "+keycolumname+","+columname+" from "+tablename+" where "+keycolumname+" in( "+tempFieldValue+")";

								} else {
									sql= "select "+keycolumname+","+columname+" from "+tablename+" where "+keycolumname+" in(" + fieldvalue + ")";
								}

								rs.executeQuery(sql);
								while(rs.next()){
									showid = Util.null2String(rs.getString(1));
									String tempshowname= Util.toScreen(rs.getString(2),user.getLanguage());
									if(!linkurl.equals("")) {
										if(isMobile){
											if("1".equals(fieldtype) || "165".equals(fieldtype) || "17".equals(fieldtype)){
												temRes.put(String.valueOf(showid),getUserLink(showid,new User(Util.getIntValue(showid)).getType()+"",user,true));
											}else if("9".equals(fieldtype) || "37".equals(fieldtype)) {
												tempshowname="<a contenteditable=\"false\" href=\"javascript:void(0);\" onclick=\"ecCom.WeaRichText.openAppLink(this,"+showid+",'37')\" style=\"cursor:pointer;text-decoration:underline;margin-right:8px\" unselectable=\"off\">"+tempshowname+"</a>";
												temRes.put(String.valueOf(showid),tempshowname);
											}else if("16".equals(fieldtype) || "152".equals(fieldtype)) {
												tempshowname="<a contenteditable=\"false\" href=\"javascript:void(0);\" onclick=\"ecCom.WeaRichText.openAppLink(this,"+showid+",'152')\" style=\"cursor:pointer;text-decoration:underline;margin-right:8px\" unselectable=\"off\">"+tempshowname+"</a>";
												temRes.put(String.valueOf(showid),tempshowname);
											}else{
												temRes.put(String.valueOf(showid),tempshowname);
											}

										}else{
											temRes.put(String.valueOf(showid),"<a href='javascript:void(0)' onclick=\"openFullWindowHaveBar('"+linkurl+showid+"');\">"+tempshowname+"</a> ");
										}
									} else {
										temRes.put(String.valueOf(showid),tempshowname);
									}
								}
								StringTokenizer temstk = new StringTokenizer(fieldvalue,",");
								String temstkvalue = "";
								while(temstk.hasMoreTokens()){
									temstkvalue = temstk.nextToken();
									if(temstkvalue.length()>0&&temRes.get(temstkvalue)!=null){
										showname += temRes.get(temstkvalue);
									}
								}

							}
							if(isMobile){
								value = Util.toScreen(showname,user.getLanguage());
							}else{
								value = "<span id='customfield" + cfm.getId() + "span'>" + Util.toScreen(showname,user.getLanguage()) + "</span>";
							}

						} else if(cfm.getHtmlType().equals("4")) {
				        	if(isMobile){
							}else {
								value = "<input type='checkbox' value='1' name='customfield" + cfm.getId() + "checkbox' " + (fieldvalue.equals("1")? "checked" : "") + " disabled >";
							}

						} else if(cfm.getHtmlType().equals("5")) {
							cfm.getSelectItem(cfm.getId());
							while(cfm.nextSelect()){
								if(cfm.getSelectValue().equals(fieldvalue)){
									value += cfm.getSelectName();
									break;
								}
							}
						}
					}
					
			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				value = PatternUtil.formatJson2Js(value);
				DocParam docParam = new DocParam(label,value,isFull);
				rs.writeLog(DocDetailService.class.getName(),">>>>>>>>>>>> DocDetailService>>docPropType="+docPropType+">>SHOW_EMPTY_PARAM="+SHOW_EMPTY_PARAM+" docid="+docid+">>value="+value);
				if(iscus_field && !"".equals(cus_fieldname)){
					Map<String,String> encryptSet = DocEncryptUtil.getEncryptSet(seccategory,cus_fieldname);
					docParam.setType("password");
					docParam.setShowblink(true);
					docParam.setIsencrypt(encryptSet.get(DocEncryptUtil.ISENCRYPT));
//					Map<String,String> encryptInfo = DocEncryptUtil.EncryptInfo(docid);
					docParam.setIsEnableSecondAuth(encryptSet.get(DocEncryptUtil.SECONDAUTH));
					docParam.setDesensitization(encryptSet.get(DocEncryptUtil.DESENSITIZATION));
				}
				if(docPropType==27){
					docParam.setType("27");
				}
				params.add(docParam);
			}
		}
		Date d4 = new Date();
		/** 文档属性 end **/
		
		 /** * 摘要 start ** */
		boolean canShowDocMain = false;
		params = getDocMain(detail.get("DOCPUBLISHTYPE"),detail.get("DOCCONTENT"),params,canShowDocMain,docid,user);

        /** * 摘要 end ** */
		
		
		/** 类型 start **/
		rs.executeQuery("select " +
				" hashrmres,hrmreslabel,hasasset,assetlabel,hascrm,crmlabel,hasitems,itemlabel,hasproject,projectlabel,hasfinance,financelabel,puboperation " +
				" from docseccategory where id=?",Util.getIntValue(detail.get("SECCATEGORY"),0));
		rs.next();
		String hashrmres = Util.toScreen(rs.getString("hashrmres"),user.getLanguage());
		String hrmreslabel = Util.toScreen(rs.getString("hrmreslabel"),user.getLanguage());
		String hasasset = Util.toScreen(rs.getString("hasasset"),user.getLanguage());
		String assetlabel = Util.toScreen(rs.getString("assetlabel"),user.getLanguage());
		String hascrm = Util.toScreen(rs.getString("hascrm"),user.getLanguage());
		String crmlabel = Util.toScreen(rs.getString("crmlabel"),user.getLanguage());
		String hasitems = Util.toScreen(rs.getString("hasitems"),user.getLanguage());
		String itemlabel = Util.toScreen(rs.getString("itemlabel"),user.getLanguage());
		String hasproject = Util.toScreen(rs.getString("hasproject"),user.getLanguage());
		String projectlabel = Util.toScreen(rs.getString("projectlabel"),user.getLanguage());
		String hasfinance = Util.toScreen(rs.getString("hasfinance"),user.getLanguage());
		String financelabel = Util.toScreen(rs.getString("financelabel"),user.getLanguage());
		boolean allowscheduledrelease="1".equals(Util.null2String(rs.getString("puboperation")));
		// 人力资源 
		if(!hashrmres.trim().equals("0")&&!hashrmres.trim().equals("")){
			String curlabel = SystemEnv.getHtmlLabelName(179,user.getLanguage());
			String value = "";
			if(!hrmreslabel.trim().equals("")) curlabel = hrmreslabel;
			if(!user.getLogintype().equals("2")){
				int hrmresid = Util.getIntValue(detail.get("HRMRESID"),0);
				if(hrmresid != 0){
					if(isMobile){
						value =getUserLink(hrmresid+"",new User(hrmresid).getType()+"",user,true);
					}else {
						value = ds.getHrmCard(hrmresid + "",isMobile);
					}
				}
			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				params.add(new DocParam(curlabel,value,false));
			}
		}
		// 资产 
		if(!hasasset.trim().equals("0")&&!hasasset.trim().equals("")){
			String curlabel = SystemEnv.getHtmlLabelName(535,user.getLanguage());
			String value = "";
			if(!assetlabel.trim().equals("")) curlabel = assetlabel;
			if(!user.getLogintype().equals("2")){
				int assetid = Util.getIntValue(detail.get("ASSETID"),0);
				if(assetid !=0 ){
					CapitalComInfo cci = new CapitalComInfo();
					if(isMobile){
						value = Util.toScreen(cci.getCapitalname(assetid+""),user.getLanguage());
					}else {
						value = "<a href='" + DocSptm.CAPITAL_LINK + DocSptm.CAPITAL_ROUT + assetid + "' target='_blank'>" +
								Util.toScreen(cci.getCapitalname(assetid+""),user.getLanguage()) + "</a>";
					}

				}
			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				params.add(new DocParam(curlabel,value,false));
			}
		}
		// CRM 
		if(!hascrm.trim().equals("0")&&!hascrm.trim().equals("")){
			String curlabel = SystemEnv.getHtmlLabelName(147,user.getLanguage());
			String value = "";
			if(!crmlabel.trim().equals("")) curlabel = crmlabel;
			int crmid = Util.getIntValue(detail.get("CRMID"),0);
			if(crmid !=0 ){
				CustomerInfoComInfo cici = new CustomerInfoComInfo();
				if(isMobile){
					value = Util.toScreen(cici.getCustomerInfoname(crmid+""),user.getLanguage());
				}else {
					value = "<a href='"+DocSptm.CUSTOM_LINK + DocSptm.CUSTOM_ROUT + crmid + "' target='_blank'>" +
							Util.toScreen(cici.getCustomerInfoname(crmid+""),user.getLanguage()) + "</a>";
				}

			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				params.add(new DocParam(curlabel,value,false));
			}
		}
		// 物品 
		if(!hasitems.trim().equals("0")&&!hasitems.trim().equals("")){
			String curlabel = SystemEnv.getHtmlLabelName(145,user.getLanguage());
			String value = "";
			if(!itemlabel.trim().equals("")) curlabel = itemlabel;
			int itemid = Util.getIntValue(detail.get("ITEMID"),0);
			if(itemid!=0){
				AssetComInfo aci = new AssetComInfo();
				if(isMobile){
					value = aci.getAssetName(""+itemid);
				}else {
					value = "<a href='"+ecologyContentPath+"/lgc/asset/LgcAsset.jsp?paraid=" + itemid + "' target='_blank'>" +
							aci.getAssetName(""+itemid) + "</a>";
				}

			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				params.add(new DocParam(curlabel,value,false));
			}
		}
		// 项目 
		if(!hasproject.trim().equals("0")&&!hasproject.trim().equals("")){
			String curlabel = SystemEnv.getHtmlLabelName(101,user.getLanguage());
			String value = "";
			if(!projectlabel.trim().equals("")) curlabel = projectlabel;
			int projectid = Util.getIntValue(detail.get("PROJECTID"),0);
			if(projectid!=0){
				ProjectInfoComInfo pici = new ProjectInfoComInfo();
				if(isMobile){
					value = Util.toScreen(pici.getProjectInfoname(""+projectid),user.getLanguage());
				}else {
					value = "<a href='"+DocSptm.PROJECT_LINK + DocSptm.PROJECT_ROUT + projectid + "' target='_blank'>" +
							Util.toScreen(pici.getProjectInfoname(""+projectid),user.getLanguage()) + "</a>";
				}

			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				params.add(new DocParam(curlabel,value,false));
			}
		}
		
		//  财务  
		if(!hasfinance.trim().equals("0")&&!hasfinance.trim().equals("")){
			String curlabel = SystemEnv.getHtmlLabelName(189,user.getLanguage());
			String value = "";
			if(!financelabel.trim().equals("")) curlabel = financelabel;
			int financeid = Util.getIntValue(detail.get("FINANCEID"),0);
			if(financeid!=0){
				value = financeid + "";
			}
			if(SHOW_EMPTY_PARAM || !value.isEmpty()){
				params.add(new DocParam(curlabel,value,false));
			}
		}
		
		Date d5 = new Date();
		/** 类型 end **/
		/** * 定时发布日期 start ** */
		if(allowscheduledrelease){
			String scheduledreleasedate = Util.null2String(detail.get("SCHEDULEDRELEASEDATE"));
			if(!"".equals(scheduledreleasedate)){
				if(scheduledreleasedate.length() > 11){
					scheduledreleasedate = TimeZoneUtil.getYmdLocaleDate1(scheduledreleasedate);
				}
				String curlabel = SystemEnv.getHtmlLabelName(524676,user.getLanguage());//定时发布日期			
				params.add(new DocParam(curlabel,scheduledreleasedate,false));
			}			
		}
		
		apidatas.put("status",1);
		apidatas.put("data",params);
		apidatas.put("time1",d1.getTime() - d0.getTime());
		apidatas.put("time2",d2.getTime() - d1.getTime());
		apidatas.put("time3",d3.getTime() - d2.getTime());
		apidatas.put("time4",d4.getTime() - d3.getTime());
		apidatas.put("time5",d5.getTime() - d4.getTime());
		
		return apidatas;
	}

	private List<DocParam> getDocMain(String docpublishtype,String doccontent, List<DocParam> params, boolean canShowDocMain, int docid,User user) throws Exception {
		RecordSet rs = new RecordSet();
		if("1".equals(docpublishtype) || "2".equals(docpublishtype)){
			canShowDocMain = true;
		}
		if (canShowDocMain) {

			String doccontents = "";
			if (docid > 0) {
				if (doccontent == null||doccontent.equals("")) {
					String dbtype = rs.getDBType();
					if("oracle".equals(dbtype) || "mysql".equals(dbtype)){
						rs.executeQuery("select doccontent from DocDetailContent where docid=?" , docid);
					}else{
						rs.executeQuery("select doccontent from DocDetail where id=?",docid);
					}
					if (rs.next()) {
						doccontents = rs.getString("doccontent");
					}
				} else {
					doccontents = doccontent;
				}
			}

			String docMain = "";
			int tmppos = doccontents.indexOf("!@#$%^&*");
			if (tmppos != -1) {
				docMain = doccontents.substring(0, tmppos);

				docMain = Util.replace(docMain, "&amp;quot;", "\"", 0);
				docMain = Util.replace(docMain, "&quot;", "\"", 0);
				docMain = Util.replace(docMain, "&lt;", "<", 0);
				docMain = Util.replace(docMain, "&gt;", ">", 0);
				docMain = Util.replace(docMain, "&apos;", "'", 0);
			}
			if(!docMain.isEmpty()){
				params.add(new DocParam(SystemEnv.getHtmlLabelName(341, user == null ? DocSptm.getDefaultLanguage() : user.getLanguage()),docMain,true));
			}

		}
		return params;
	}


	/**
	 * 获取文档版本
	 * @author wangqs
	 * */
	public Map<String,Object> getDocVersion(int docid,User user){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			return apidatas;
		}
		
		RecordSet rs = new RecordSet();
		rs.executeQuery("select doceditionid,seccategory from DocDetail where id=?",docid);
		
		String doceditionid = "";
		int seccategory = 0;
		if(rs.next()){
			doceditionid = rs.getString("doceditionid");
			seccategory = rs.getInt("seccategory");
		}
		String readerCanViewHistoryEdition = "";
		if(seccategory > 0){
			rs.executeQuery("select readerCanViewHistoryEdition from DocSecCategory where id=?",seccategory);
			if(rs.next()){
				readerCanViewHistoryEdition = rs.getString("readerCanViewHistoryEdition");
			}
		}
		
		DocViewPermission dvp = new DocViewPermission();
		Map<String,Boolean> levels = dvp.getShareLevel(docid, user, false);
		String canEditHis = levels.get(DocViewPermission.EDIT) ? "true" : "false";
		
		String tabletype = "none";
		DocTableType docTableType = DocTableType.DOC_VERSION;
		
		String sourceParams = "docid:"+docid+"+doceditionid:"+doceditionid+"+readerCanViewHistoryEdition:"+readerCanViewHistoryEdition+"+canEditHis:" + canEditHis;
		
		String tableString=""+
		 "<table datasource=\"com.api.doc.detail.service.DocDetailService.getDocVerList\" sourceparams=\""+sourceParams+"\" pagesize=\"" + docTableType.getPageSize() + "\" tabletype=\"" + tabletype + "\">"+
		 "<sql backfields=\"*\"  sqlform=\"temp\" sqlorderby=\"versionid\"  sqlprimarykey=\"versionid\" sqlsortway=\"desc\"  />"+
			   "<head>";
					tableString += "<col width=\"40%\" labelid=\"1341\"  text=\""+SystemEnv.getHtmlLabelName(1341,user.getLanguage())+"\" column=\"docsubject\"/>";
					tableString+=	 "<col width=\"10%\" labelid=\"22186\"  text=\""+SystemEnv.getHtmlLabelName(22186,user.getLanguage())+"\" column=\"versionid\" />";
					tableString += "<col width=\"20%\" labelid=\"882\"  text=\""+SystemEnv.getHtmlLabelName(882,user.getLanguage())+"\" column=\"creator\"/>";
					tableString += "<col width=\"30%\" labelid=\"19521\"  text=\""+SystemEnv.getHtmlLabelName(19521,user.getLanguage())+"\" column=\"doclastmoditime\"/>";
					tableString += "</head></table>";
					
		String sessionkey = docTableType.getPageUid() + "_" + Util.getEncrypt(Util.getRandom());
		Util_TableMap.setVal(sessionkey, tableString);			
		
		apidatas.put("sessionkey", sessionkey);
		
		return apidatas;
	}
	
	/**
     * 文档版本列表
     */
    public List<Map<String,String>> getDocVerList(User user,Map<String,String> otherparams,HttpServletRequest request,HttpServletResponse response) throws Exception{
        ArrayList<Map<String,String>> dataList=new ArrayList<Map<String,String>>();
        int docid = Util.getIntValue(otherparams.get("docid"),0);
        int doceditionid = Util.getIntValue(otherparams.get("doceditionid"),0);
        String readerCanViewHistoryEdition= Util.null2String(otherparams.get("readerCanViewHistoryEdition"));
        boolean canEditHis= Util.null2String(otherparams.get("canEditHis")).equals("true");
        
        if(user == null)  return dataList;
        DocComInfo docComInfo = new DocComInfo();
        RecordSet rs = new RecordSet();
        
        SptmForDoc sfd = new SptmForDoc();
        DocSptm ds = new DocSptm();
        
        if(doceditionid>-1){      
            rs.executeQuery("select a.id,a.isHistory,a.docsubject,a.usertype,a.doccreaterid,a.doclastmoddate,a.doclastmodtime,a.docedition,b.editionPrefix " +
            		"from DocDetail a,docseccategory b where a.seccategory = b.id and a.doceditionid = ? order by a.docedition desc,a.id desc ",doceditionid);
            while(rs.next()){
                JSONObject oJson= new JSONObject();
                int currDocId = Util.getIntValue(rs.getString("id"));
                int currIsHistory = Util.getIntValue(rs.getString("isHistory"));
                String docsubject = rs.getString("docsubject");
                String doccreaterid = rs.getString("doccreaterid");
                String usertype = rs.getString("usertype");
                String time_datetime = rs.getString("doclastmoddate") + " " + rs.getString("doclastmodtime");
//                time_datetime = TimeZoneUtil.getYmdLocaleDate1(time_datetime);
                String datetime = time_datetime;
                int docEdition = Util.getIntValue(rs.getString("docedition"), -1);
                String editionPrefix = Util.null2String(rs.getString("editionPrefix"));
                
                String versionid = "";
                if(docEdition < 1){
                	versionid = "";
                }else{
                	versionid = editionPrefix + docEdition + ".0";
                }
                
                String currCreater = "";
                if (Util.getIntValue(usertype) == 1) {
                    currCreater = sfd.getName(doccreaterid,"1");
                } else {
                    currCreater = sfd.getName(doccreaterid,"2");
                }
                String tempImg="";
                if(currDocId==docid){
                    tempImg="<i class='icon-document-doc' style='vertical-align: middle;color:#67AFF7;margin-right:5px;font-size: 14px'></i>";
                }
                if(currIsHistory==1&&!readerCanViewHistoryEdition.equals("1") && !canEditHis){
                    oJson.put("docsubject",tempImg+docsubject);
                }else{
                    oJson.put("docsubject",tempImg+ds.getDocNameByName(currDocId + "",docsubject));
                }

                
                oJson.put("versionid",versionid+"("+docComInfo.getStatusView(currDocId,user)+")");   
                oJson.put("creator",currCreater);
                oJson.put("doclastmoditime",datetime);

                oJson.put("expanded",true);
                oJson.put("leaf",true);
                oJson.put("uiProvider","col");  
                oJson.put("editionPrefix", editionPrefix);
                oJson.put("docEdition", docEdition);
                dataList.add(oJson);
            }
           
            
        } 
        return dataList;
    }
    
    /**
     * 获取文档版本号
     * */
    public String getEditionView(int docid,String number){
    	 RecordSet rs = new RecordSet();
         rs.executeQuery(
                 " select a.docedition as docedition,b.editionPrefix as editionPrefix " +
                 " from docdetail a, docseccategory b" +
                 " where a.seccategory = b.id" +
                 " and a.id = ?",docid);
         rs.next();
         int docEdition = Util.getIntValue(rs.getString("docedition"), -1);
         if(docEdition<1) return "";
         return Util.null2String(rs.getString("editionPrefix")) + "" +docEdition + ".0";
    }
	
	
	/**
	 * 子文档列表
	 * @author wangqs
	 * */
	public Map<String,Object> getDocChilds(int docid,User user){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			return apidatas;
		}
		
		RecordSet rs = new RecordSet();
		rs.executeQuery("select id from docdetail where doceditionid=(select doceditionid from docdetail where id=?) and (isHistory<>'1' or isHistory is null or isHistory='') order by doceditionid desc",docid);
		
		int newDocId = docid;
		if(rs.next()){
			newDocId = rs.getInt("id");
		}
		
		String sqlFrom = "DocDetail";
		String sqlWhere ="id <> " + newDocId + " and mainDoc = " + newDocId + " and (isHistory<>'1' or isHistory is null or isHistory='')";
		String backFields = "id,docsubject,ownerid,ownerType";
		String orderBy = "id";
		String tabletype = "none";
		
		DocTableType docTableType = DocTableType.DOC_CHILD;
		
		String tableString=""+
        "<table pageUid=\"" + docTableType.getPageUid() + "\" pageId=\"" + docTableType.getPageUid() + "\" pagesize=\"" + docTableType.getPageSize() + "\" tabletype=\""+tabletype+"\">"+
        "<sql backfields=\"" + backFields + "\" sqlform=\""+Util.toHtmlForSplitPage(sqlFrom)+"\" sqlorderby=\""+orderBy+"\"  sqlprimarykey=\"id\" sqlsortway=\"Desc\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\" sqldistinct=\"true\"  />"+
        "<head>"+
				"<col  width=\"70%\"  text=\""+SystemEnv.getHtmlLabelName(1341,user.getLanguage())+"\" column=\"docsubject\" orderkey=\"docsubject\" />"+
				"<col width=\"30%\"  text=\""+SystemEnv.getHtmlLabelName(79,user.getLanguage())+"\" column=\"ownerid\" orderkey=\"ownerid\"   transmethod=\"weaver.splitepage.transform.SptmForDoc.getName\" otherpara=\"column:ownerType\"/>"+		
        "</head>" +
        "</table>";  
		
		String sessionkey = docTableType.getPageUid() + "_" + Util.getEncrypt(Util.getRandom());
		Util_TableMap.setVal(sessionkey, tableString);
		
		
		apidatas.put("sessionkey", sessionkey);
		
		return apidatas;
	}
	
	
	/**
	 * 获取右键菜单及顶部按钮
	 * @author wangqs
	 * */
	public List<RightMenu> getRightMenus(int docid,User user,Map<String,String> paramMap) throws Exception{
		List<RightMenu> rightMenus = new ArrayList<RightMenu>();
//		Map<String,Object> showRightMenuMap = DocDetailUtil.isShowRightMenuMap();
		DocMenuManager docMenuManager = new DocMenuManager();
		Map<String,DocMenuBean> showRightMenuMap = docMenuManager.getDocMenuSet();
		paramMap = paramMap == null ? new HashMap<String,String>() : paramMap;
		
		int userid=user.getUID();
		String logintype = user.getLogintype();
		//String userSeclevel = user.getSeclevel();
		//String userType = "" + user.getType();
		//String userdepartment = ""+user.getUserDepartment();
		//String usersubcomany = ""+user.getUserSubCompany1();
		//String userInfo = logintype + "_" + userid + "_" + userSeclevel + "_" + userType + "_" + userdepartment + "_" + usersubcomany;
		//SpopForDoc spop = new SpopForDoc();
		//ArrayList PdocList = spop.getDocOpratePopedom("" + docid,userInfo);
		
		DocViewPermission docViewPermission = new DocViewPermission();
		Map<String,Boolean> rightMap = docViewPermission.getShareLevel(docid, user, false);
		
		if(!rightMap.get(DocViewPermission.READ) && request != null){
			boolean read = docViewPermission.hasRightFromOtherMould(docid,user,request);
			if(read){
				rightMap.put(DocViewPermission.READ,true);
				RecordSet rs = new RecordSet();
				rs.executeQuery("select c.nodownload  from DocDetail d,DocSecCategory c " +" where d.seccategory=c.id and d.id=" + docid);
				boolean caDownload =true;
				if(rs.next()){
					caDownload = Util.getIntValue(rs.getString("nodownload"),0)==0;
				}
				rightMap.put(DocViewPermission.DOWNLOAD,caDownload);
			}
		}
		
		
		int language = user.getLanguage();
		
		//0:查看  
		boolean canReader = false;
		//1:编辑
		boolean canEdit = false;
		//2:删除
		boolean canDel = false;
		//3:共享
		boolean canShare = false ;
		//4:日志
		boolean canViewLog = false;
		//5:可以回复
		boolean canReplay = false;
		//6:打印
		boolean canPrint = false;
		//7:发布
		boolean canPublish = false;
		//8:失效
		boolean canInvalidate = false;
		//9:归档
		boolean canArchive = false;
		//10:作废
		boolean canCancel = false;
		//11:重新打开
		boolean canReopen = false;

		//置顶
		boolean cantop = false;

		//签出
		boolean canCheckOut = false;
		//签入
		boolean canCheckIn = false;
		//强制签入
		boolean canCheckInCompellably =false ;
		//新建工作流
		boolean cannewworkflow = true;
		//TD12005不可下载
		boolean canDownloadFromShare = false;
		
		RecordSet rs = new RecordSet();
		
		String backFields = "d.docstatus,d.seccategory,d.docdepartmentid,d.docapprovable,d.ishistory,d.readOpterCanPrint,d.invalidationdate," +
				"d.doccreaterid,d.docCreaterType,d.ownerid,d.ownerType,d.doctype,d.selectedpubmouldid,d.docpublishtype," +
				"c.readerCanViewHistoryEdition,c.replyable,c.appointedWorkflowId,c.isOpenAttachment,c.maxUploadFileSize,c.minUploadFileSize,c.hideshare";
		String from = " from DocDetail d,DocSecCategory c ";
		String where = " where d.seccategory=c.id and d.id=?";
		
		String sql = "";
		
		String dbtype = rs.getDBType();
	
		if("oracle".equals(dbtype) || "mysql".equals(dbtype)){
			backFields += ",dc.doccontent";
			from += ",docdetailcontent dc";
			where += " and d.id=dc.docid";
		}else{
			backFields += ",d.doccontent";
		}
		sql = "select " + backFields + from + where;
		rs.executeQuery(sql,docid);
		
		String docstatus = "";	//文档状态
		int seccategoryid = 0;
		int docdepartmentid = 0;	//文档所属部门
		String docapprovable = ""; //是否审批
		int ishistory = -1;	//是否是历史文档
		String readerCanViewHistoryEdition = ""; //是否开启查看历史版本
		String docreplyable = "";	//目录是否开启回复
		int categoryreadoptercanprint = 0;	//目录是否开启打印
		int docreadoptercanprint = 0;	//文档是否开启打印
		int doccreaterid = -1;	// 文档创建人id
		String docCreaterType = "";	// 文档创建人类型
		int ownerid = -1;	//文档所有者id
		String ownerType = ""; //文档所有者类型
		int doctype = -1;	//文档类型
		int appointedWorkflowId = 0;   //审批流id
		int selectedpubmouldid = 0;  // 文档选中模板id
		String invalidationdate = ""; //文档失效时间
		String doccontent = "";  //文档内容
		String docpublishtype = ""; //发布类型
		String isOpenAttachment = ""; //是否开启单附件打开
		int maxUploadFileSize = 0;  //上传文件大小限制
		int minUploadFileSize = 0;  //上传文件大小限制
		int docfileType=-1;
		int hideshare=0;   //隐藏分享按钮
		Map<String,String> encryptShare = DocEncryptUtil.EncryptInfo(docid);
		if(rs.next()){
			docstatus = rs.getString("docstatus");
			seccategoryid = rs.getInt("seccategory");
			docdepartmentid = rs.getInt("docdepartmentid");
			docapprovable = rs.getString("docapprovable");
			ishistory = rs.getInt("ishistory");
			readerCanViewHistoryEdition = rs.getString("readerCanViewHistoryEdition");
			docreplyable = rs.getString("replyable");
			categoryreadoptercanprint = rs.getInt("readoptercanprint");
			docreadoptercanprint = rs.getInt("readOpterCanPrint");
			doccreaterid = rs.getInt("doccreaterid");
			docCreaterType = rs.getString("docCreaterType");
			ownerid = rs.getInt("ownerid");
			ownerType = rs.getString("ownerType");
			doctype = rs.getInt("doctype");
			appointedWorkflowId = rs.getInt("appointedWorkflowId");
			selectedpubmouldid = rs.getInt("selectedpubmouldid");
			invalidationdate = rs.getString("invalidationdate");
			doccontent = rs.getString("doccontent");
			docpublishtype = rs.getString("docpublishtype");
			isOpenAttachment = rs.getString("isOpenAttachment");
			maxUploadFileSize = rs.getInt("maxUploadFileSize");
			minUploadFileSize = rs.getInt("minUploadFileSize");
			hideshare=rs.getInt("hideshare");
		}
		
		doccontent = this.formatDocContent(doccontent,docpublishtype);
		doccontent = PatternUtil.formatJson2Js(doccontent);
		
		Map<String,String> accInfo = new HashMap<String,String>();
		//if(doctype == 1){
			accInfo = this.getMainAccInfo(doccontent, isOpenAttachment, docid);
		//}
		canReader = rightMap.get(DocViewPermission.READ);
		canEdit = rightMap.get(DocViewPermission.EDIT);
		canDel = rightMap.get(DocViewPermission.DELETE);
		canShare = rightMap.get(DocViewPermission.SHARE);
		canViewLog = rightMap.get(DocViewPermission.LOG);
		canDownloadFromShare = rightMap.get(DocViewPermission.DOWNLOAD);
		
		if(canReader && ((!docstatus.equals("7")&&!docstatus.equals("8")) 
                ||(docstatus.equals("7")&&ishistory==1&&readerCanViewHistoryEdition.equals("1")))){
		  canReader = true;
		}else{
		  canReader = false;
		}
		
		if(ishistory==1) {
			if(readerCanViewHistoryEdition.equals("1")){
		    	if(canReader && !canEdit) canReader = true;
			} else {
			    if(canReader && !canEdit) canReader = false;
			}
		}
		
		if(canEdit && ((docstatus.equals("3") || docstatus.equals("5") || docstatus.equals("6") || docstatus.equals("7")) || ishistory==1)) {
		    canReader = true;
		}
		
		if(canEdit && (Util.getIntValue(docstatus,3) < 3 || docstatus.equals("4") || docstatus.equals("7")) && (ishistory!=1))
		    canEdit = true;
		else
		    canEdit = false;
		
		if(docreplyable.equals("1") && (docstatus.equals("5") || docstatus.equals("2") || docstatus.equals("1")))
		    canReplay = true;
		
		
		if(canEdit){
			canPrint = true;
		}
		if(!canPrint){
			canPrint = docViewPermission.getPrint(docid,user,rightMap,paramMap);
		}
		
		if(HrmUserVarify.checkUserRight("DocEdit:Publish",user,docdepartmentid) && docstatus.equals("6") && (ishistory!=1)){
		    canPublish = true ;
		}
		
		if(HrmUserVarify.checkUserRight("DocEdit:Invalidate",user,docdepartmentid) && (docstatus.equals("1") || docstatus.equals("2")) && (ishistory!=1))
		    canInvalidate = true ;
		if(canEdit && (docstatus.equals("1") || docstatus.equals("2")) && (ishistory!=1)){
		    canInvalidate = true ;
		}
		
		if(HrmUserVarify.checkUserRight("DocEdit:Archive",user,docdepartmentid) && (docstatus.equals("1") || docstatus.equals("2")) && (ishistory!=1))
			canArchive = true ;
		
		if(HrmUserVarify.checkUserRight("DocEdit:Cancel",user,docdepartmentid) && (docstatus.equals("1") || docstatus.equals("2") || docstatus.equals("5") || docstatus.equals("7")) && (ishistory!=1))
			canCancel = true ;

		if(HrmUserVarify.checkUserRight("DocEdit:Reopen",user,docdepartmentid) && (docstatus.equals("5") || docstatus.equals("8")) && (ishistory!=1))
		    canReopen = true ;
		
		if(canEdit){
			canShare = true;
		    canViewLog = true;
		}
		
		if(canDel   || HrmUserVarify.checkUserRight("DocEdit:Delete",user,docdepartmentid) ){
			if(docstatus.equals("5")||docstatus.equals("3")){
		        canDel = false;
			}else{
				canDel = true;
			}
		}else{
		    canDel = false;
		}
		
		boolean onlyview=false;
		if(!canReader){
			if(
				  ((""+userid).equals(""+doccreaterid)&&logintype.equals(docCreaterType))
			    ||((""+userid).equals(""+ownerid)&&logintype.equals(ownerType))
			  ){
				canReader=true;
			}
		}
		
		String agent = paramMap == null ? "" : paramMap.get("agent");
		boolean canIwebOffice = IWebOfficeConf.canIwebOffice(agent,null);
		//OFD轻阅读预览
		String readType = Util.null2String(rs.getPropValue("weaver_OFDReader","readerType"));
		boolean ofdLightReader =  "2".equals(readType) || "3".equals(readType);
		if(!canEdit){
			docViewPermission.hasEditRightFromOtherMould(docid,rightMap,user,paramMap);
			canEdit = rightMap.get(DocViewPermission.EDIT);
		}

		DocCoopereateUtil dcu = new DocCoopereateUtil();
		if(dcu.jugeIsCoope(docid+"", user)) {
			canEdit = false;
			canDel = false;
			canShare = false;
			canViewLog = false;

			if(canDownloadFromShare && doctype != 1 && showRightMenuMap.get(DocMenuManager.DOWNLOAD).getIsopen()==1){
				RightMenu rightMenu = new RightMenu(language,RightMenuType.BTN_DOWNLOAD,"",showRightMenuMap.get(DocMenuManager.DOWNLOAD).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DOWNLOAD).getColumname());
				RecordSet fileRs = new RecordSet();
				fileRs.executeQuery("select imagefileid,imagefilename,docfiletype,versionId from DocImageFile where docid=? and (isextfile <> '1' or isextfile is null) order by versionId desc",docid);
				if(fileRs.next()){
					rightMenu.setParams("download=1&fileid=" + fileRs.getString("imagefileid"));
				}
				rightMenus.add(rightMenu);
			}

			// 如果是协作文档，只能查看下载
			return rightMenus;
		}
		
		
		if((canEdit && showRightMenuMap.get(DocMenuManager.EDIT).getIsopen()==1 && doctype!=12)||("-1".equals(docstatus)&&(""+userid).equals(""+doccreaterid))){
			if(doctype == 1 || 
					(canIwebOffice && !(ofdLightReader && doctype == 13)) ||
					(doctype == 2 && 
							(ImageConvertUtil.canEditForYozo("doc",user) || 
							ImageConvertUtil.canEditForWps("doc",user)))){  //是html文档，或者插件支持的浏览器    
				rightMenus.add(new RightMenu(language,RightMenuType.BTN_EDIT,"",showRightMenuMap.get(DocMenuManager.EDIT).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.EDIT).getColumname()));
			}
		}

		if(canEdit){
			String ext = "";
			if("1".equals(accInfo.get(IS_OPEN_ACC))){
				String filename = Util.null2String(accInfo.get(ACC_FILE_NAME));
				ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
			}else if(doctype == 12){
				ext = "pdf";
			}
			
			if(ext.toLowerCase().equals("pdf") && showRightMenuMap.get(DocMenuManager.SUBMIT).getIsopen()==1 &&
					paramMap != null &&
					("true".equals(paramMap.get("isIE")) &&
					"1".equals(rs.getPropValue("weaver_iWebPDF", "isUseiWebPDF"))|| IWebOfficeConf.canIwebPDF(request.getHeader("user-agent")))){
				rightMenus.add(new RightMenu(language,RightMenuType.BTN_SAVE,"",showRightMenuMap.get(DocMenuManager.SUBMIT).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.SUBMIT).getColumname()));
			}
			
		}

		
		if("1".equals(accInfo.get(IS_OPEN_ACC)) && canEdit){  //编辑附件
			String filename = Util.null2String(accInfo.get(ACC_FILE_NAME));
			
			String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
			
			//if(paramMap != null && "true".equals(paramMap.get("isIE")) && DocSptm.isOfficeDoc(ext)){
			
			
			boolean editAcc = false;
			if(paramMap != null &&  DocSptm.isOfficeDoc(ext)){
				if("true".equals(paramMap.get("isIE")) || 
						ImageConvertUtil.canEditForYozo(ext,user) || 
						ImageConvertUtil.canEditForWps("doc",user) || 
						canIwebOffice){
					editAcc = true;
				}
			}
			
			if(".uot".equals(ext) && canIwebOffice){
				String offcietype = IWebOfficeConf.getType();
				if("yozoOffice".equals(offcietype)){
					editAcc = true;
				}
			}
			
			if( showRightMenuMap.get(DocMenuManager.EDITACC).getIsopen()==1){
				RightMenu rmEdit = new RightMenu(language,RightMenuType.BTN_ACC_EDIT,"",showRightMenuMap.get(DocMenuManager.EDITACC).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.EDITACC).getColumname());
				if(ImageConvertUtil.useNewAccView()){
					rmEdit.setParams("id=" + docid  + "&isEdit=1" + DocSptm.DOC_ROOT_FLAG_VALUE);
				}else {
					rmEdit.setParams("id=" + docid + "&imagefileId=" + accInfo.get(ACC_FILE_ID) + "&versionId=" + accInfo.get(ACC_FILE_VERSION) + "&isEdit=1" + DocSptm.DOC_ROOT_FLAG_VALUE);
				}

				rightMenus.add(rmEdit);
			}

			if(showRightMenuMap.get(DocMenuManager.REPLACEACC).getIsopen()==1){
				RightMenu rm = new RightMenu(language,RightMenuType.BTN_ACC_REPLACE,"",showRightMenuMap.get(DocMenuManager.REPLACEACC).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.REPLACEACC).getColumname());
				Map<String,String> params = new HashMap<String,String>();
				params.put("maxUploadSize",maxUploadFileSize + "");
				params.put("mixUploadSize",minUploadFileSize + "");

				DocAccService accService = new DocAccService();
				params.put("limitType",accService.getReplaceType(ext));
				rm.setCustomData(params);
				rightMenus.add(rm);
			}

		}
		
		if((canEdit && Util.getIntValue(docstatus,0)<=0 && showRightMenuMap.get(DocMenuManager.SUBMIT).getIsopen()==1)||("-1".equals(docstatus)&&(""+userid).equals(""+doccreaterid)))
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_SUBMIT,"",showRightMenuMap.get(DocMenuManager.SUBMIT).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.SUBMIT).getColumname()));
		if(canShare && showRightMenuMap.get(DocMenuManager.SHAREGX).getIsopen()==1 ){
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_SHARE,"",showRightMenuMap.get(DocMenuManager.SHAREGX).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.SHAREGX).getColumname()));
		}
		if(canDownloadFromShare && doctype != 1 && showRightMenuMap.get(DocMenuManager.DOWNLOAD).getIsopen()==1){
			RightMenu rightMenu = new RightMenu(language,RightMenuType.BTN_DOWNLOAD,"",showRightMenuMap.get(DocMenuManager.DOWNLOAD).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DOWNLOAD).getColumname());
			RecordSet fileRs = new RecordSet();
			fileRs.executeQuery("select imagefileid,imagefilename,docfiletype,versionId from DocImageFile where docid=? and (isextfile <> '1' or isextfile is null) order by versionId desc",docid);
			if(fileRs.next()){
				rightMenu.setParams("download=1&fileid=" + fileRs.getString("imagefileid"));
			}
			rightMenus.add(rightMenu);
		}
		if("1".equals(accInfo.get(IS_OPEN_ACC)) && canDownloadFromShare && showRightMenuMap.get(DocMenuManager.DOWNLOAD).getIsopen()==1){
			RightMenu rightMenu = new RightMenu(language,RightMenuType.BTN_DOWNLOAD_ACC,"",showRightMenuMap.get(DocMenuManager.DOWNLOAD).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DOWNLOAD).getColumname());
			if(ImageConvertUtil.useNewAccView()){
				rightMenu.setParams("download=1");
			}else{
				rightMenu.setParams("download=1&fileid="+accInfo.get(ACC_FILE_ID));
			}
			rightMenus.add(rightMenu);
		}
		
		
		if(canReplay && showRightMenuMap.get(DocMenuManager.REPLY).getIsopen()==1)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_REPLY,"",showRightMenuMap.get(DocMenuManager.REPLY).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.REPLY).getColumname()));
		if(canDel && showRightMenuMap.get(DocMenuManager.DELETE).getIsopen()==1)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_DELETE,"",showRightMenuMap.get(DocMenuManager.DELETE).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DELETE).getColumname()));
		int isUseEMessager = 0;
		try {
			//isUseEMessager = Util.getIntValue(rs.getPropValue("Messager2", "IsUseEMessager"), 0);	
			
			//AppSettingService service = (AppSettingServiceImpl) ServiceUtil.getService(AppSettingServiceImpl.class, user);
			String shareDoc = ValveConfigManager.getTypeValve("share", "shareDoc");//service.get(new HashMap<String,Object>(), user);
			if("1".equals(shareDoc)){
				isUseEMessager = 1;
			}
			
		} catch(Exception e){}
		if(showRightMenuMap.get(DocMenuManager.SHAREFX).getIsopen()==1&&(docstatus.equals("1")||docstatus.equals("2")||docstatus.equals("5")) && "1".equals(user.getLogintype()) && isUseEMessager==1&&hideshare!=1){
			if("1".equals(encryptShare.get(DocEncryptUtil.ISENCRYPTSHARE)) && "1".equals(encryptShare.get(DocEncryptUtil.ENCRYPTRANGE))){ //开启加密分享，且允许个人设置
				rightMenus.add(new RightMenu(language,RightMenuType.BTN_SHARE_DOC,"",showRightMenuMap.get(DocMenuManager.SHAREFX).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.SHAREFX).getColumname()));
				rightMenus.add(new RightMenu(language,RightMenuType.BTN_ENCRYPT_SHARE,"",showRightMenuMap.get(DocMenuManager.SHAREFX).getQuickmenu()==1));
			}else if("1".equals(encryptShare.get(DocEncryptUtil.ISENCRYPTSHARE)) && "0".equals(encryptShare.get(DocEncryptUtil.ENCRYPTRANGE))){ //开启加密分享，且全部需要加密
				rightMenus.add(new RightMenu(language,RightMenuType.BTN_ENCRYPT_SHARE,"",showRightMenuMap.get(DocMenuManager.SHAREFX).getQuickmenu()==1));
			}else if(!"1".equals(encryptShare.get(DocEncryptUtil.ISENCRYPTSHARE))){ //未开启加密分享
				rightMenus.add(new RightMenu(language,RightMenuType.BTN_SHARE_DOC,"",showRightMenuMap.get(DocMenuManager.SHAREFX).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.SHAREFX).getColumname()));
			}

		}
		if(showRightMenuMap.get(DocMenuManager.CREATEWOREFLOW).getIsopen()==1&&!docstatus.equals("3")&&!docstatus.equals("7") && cannewworkflow ){
			RightMenu rightMenu= new RightMenu(language,RightMenuType.BTN_CREATE_WORKFLOW,"",showRightMenuMap.get(DocMenuManager.CREATEWOREFLOW).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.CREATEWOREFLOW).getColumname());
			if(appointedWorkflowId > 0){
				boolean hasNewRequestRight=false;
				String isagent="0";
			    //判断是否有流程创建权限
				ShareManager shareManager = new ShareManager();
				hasNewRequestRight = shareManager.hasWfCreatePermission(user, appointedWorkflowId);
				 if(!hasNewRequestRight){
					String begindate="";
					String begintime="";
					String enddate="";
					String endtime="";
					String CurrentDate = TimeUtil.getCurrentDateString();
					String CurrentTime = TimeUtil.getOnlyCurrentTimeString();
					int beagenterid=0;
					rs.executeQuery("select distinct workflowid,bagentuid,begindate,begintime,enddate,endtime from workflow_agentConditionSet where workflowid=? and agenttype>'0' and iscreateagenter=1 and agentuid=?",appointedWorkflowId,userid);
					while(rs.next()&&!hasNewRequestRight){
						begindate=Util.null2String(rs.getString("begindate"));
						begintime=Util.null2String(rs.getString("begintime"));
						enddate=Util.null2String(rs.getString("enddate"));
						endtime=Util.null2String(rs.getString("endtime"));
						beagenterid=Util.getIntValue(rs.getString("bagentuid"),0);

						if(!begindate.equals("")){
							if((begindate+" "+begintime).compareTo(CurrentDate+" "+CurrentTime)>0)
								continue;
						}
						if(!enddate.equals("")){
						    if((enddate+" "+endtime).compareTo(CurrentDate+" "+CurrentTime)<0)
						        continue;
						}
						
						hasNewRequestRight = shareManager.hasWfCreatePermission(beagenterid, appointedWorkflowId);
						
						if(hasNewRequestRight){
							isagent="1";
						}
					}
				}
				if(hasNewRequestRight){
					String params = "workflowid="+appointedWorkflowId+"&isagent="+isagent;
					rightMenu.setParams(params); 
				}
			}
			rightMenus.add(rightMenu);
		}
		if(showRightMenuMap.get(DocMenuManager.ABOUTFLOW).getIsopen()==1){
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_RELATE_WORKFLOW,"",showRightMenuMap.get(DocMenuManager.ABOUTFLOW).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.ABOUTFLOW).getColumname()));
		}

		if(!user.getLogintype().equals("2") && !docstatus.equals("7") && showRightMenuMap.get(DocMenuManager.CREATESCHEDULE).getIsopen()==1)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_CREATE_PLAN,"",showRightMenuMap.get(DocMenuManager.CREATESCHEDULE).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.CREATESCHEDULE).getColumname()));
		if(canPublish && showRightMenuMap.get(DocMenuManager.PUBLIC).getIsopen()==1)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_PUBLISH,"",showRightMenuMap.get(DocMenuManager.PUBLIC).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.PUBLIC).getColumname()));
		 if(canInvalidate && showRightMenuMap.get(DocMenuManager.INVALID).getIsopen()==1)
			 rightMenus.add(new RightMenu(language,RightMenuType.BTN_IN_VALIDATE,"",showRightMenuMap.get(DocMenuManager.INVALID).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.INVALID).getColumname()));
		if(canReopen)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_RE_OPEN,"",false));
		if(canCheckIn)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_RE_CHECKIN,"",false));
		if(canCheckInCompellably && showRightMenuMap.get(DocMenuManager.MUSTCHECKIN).getIsopen()==1)
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_RE_CHECKIN_M,"",showRightMenuMap.get(DocMenuManager.MUSTCHECKIN).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.MUSTCHECKIN).getColumname()));
		if(HrmUserVarify.checkUserRight("DocEdit:Reload",user,docdepartmentid) && docstatus.equals("5"))
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_RE_LOAD,"",false));
		if(canPrint && showRightMenuMap.get(DocMenuManager.PRINT).getIsopen()==1 && !(ofdLightReader && doctype == 13)){
			int mouldid = 0;
			if(selectedpubmouldid <= 0){
				int selectMouldType = 0;
				int selectDefaultMould = 0;
				boolean isTemporaryDoc = invalidationdate != null && !"".equals(invalidationdate);
				rs.executeQuery("select t1.* from DocSecCategoryMould t1 right join DocMould t2 on t1.mouldId = t2.id where t1.secCategoryId = ? and t1.mouldType=1 order by t1.id",seccategoryid);
				while(rs.next()){
					String moduleid=rs.getString("mouldId");
					String modulebind = rs.getString("mouldBind");
					int isDefault = Util.getIntValue(rs.getString("isDefault"),0);

					if(isTemporaryDoc){
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
				 if(selectMouldType > 0)
					 mouldid = selectDefaultMould;
				 
				 if(mouldid<=0){
					MouldManager mouldManager = new MouldManager();
					mouldid = mouldManager.getDefaultMouldId();
				 }
			}else{
				mouldid = selectedpubmouldid;
			}
			
			RightMenu  rightMenu = new RightMenu(language,RightMenuType.BTN_PRINT,"",showRightMenuMap.get(DocMenuManager.PRINT).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.PRINT).getColumname());
			if(mouldid > 0){
				rightMenu.setParams("docmouldid=" + mouldid);
			}
			rightMenus.add(rightMenu);
		}
		if(canEdit && docapprovable.equals("1") ) // || isremark==1 || hasright==1 )&&isbill==1&&formid==28
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_APPROVE_MSG,"",false));
		if(canViewLog&&logintype.equals("1") && showRightMenuMap.get(DocMenuManager.LOG).getIsopen()==1 && new DocDetailUtil().canShowdocTab(user))
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_LOG,"",showRightMenuMap.get(DocMenuManager.LOG).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.LOG).getColumname()));
		if(HrmUserVarify.checkUserRight("Document:Top", user) && !docstatus.equals("3") && showRightMenuMap.get(DocMenuManager.DOCTOP).getIsopen()==1){
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_TO_TOP,"",showRightMenuMap.get(DocMenuManager.DOCTOP).getQuickmenu()==1,showRightMenuMap.get(DocMenuManager.DOCTOP).getColumname()));
		}
		rs.executeQuery("select istop from docdetail d where d.id=?",docid);
		rs.next();
		int istop = rs.getInt(1);
		if(istop==1 && HrmUserVarify.checkUserRight("Document:Top", user))
			rightMenus.add(new RightMenu(language,RightMenuType.BTN_CANCLE_TOP,"",false));
		
		//rightMenus.add(new RightMenu(language,RightMenuType.BTN_STORE,"",false));
		//rightMenus.add(new RightMenu(language,RightMenuType.BTN_HELP,"",false));
		if(docid>0){
			RecordSet rss = new RecordSet();
			rss.executeQuery("select docfiletype from docimagefile where docid=? and docfiletype = 13",docid);
			if(rss.next()){
				docfileType = rss.getInt("docfiletype");

			}

		}
		if (docfileType==13){
			//if ((!"5".equals(docstatus)&&!"7".equals(docstatus)&&!"8".equals(docstatus)) && !ofdLightReader)
				//rightMenus.add(new RightMenu(language,RightMenuType.BTN_EDIT,"",true));
			//canEdit=true;
			RightMenu rm;
			//if (canEdit){
			if ((!"5".equals(docstatus)&&!"8".equals(docstatus)) && !ofdLightReader){
				int count = 0;
				for ( int i=0;i<rightMenus.size();i++){
					rm= rightMenus.get(i);
					if ("1".equals(rm.getIsTop())){
						//rm.setIsTop("0");
//						String labelids=rm.getType().getLabelids();
//						if (RightMenuType.BTN_DOWNLOAD.getLabelids().equals(labelids)){
//							rm.setIsTop("1");
//						}
						if (RightMenuType.BTN_EDIT.getLabelids().equals(rm.getType().getLabelids())){
							rightMenus.set(i,new RightMenu(language,RightMenuType.BTN_SAVE,"",true));
							count++;
						}
					}
				}
			}
		}


		return rightMenus;
	}
	
	/**
	 * 去除正文的摘要
	 * @author wangqs
	 * */
	public String formatDocContent(String doccontent,String docpublishtype){
		//Util.toBaseEncoding(rs.getString("doccontent"), user.getLanguage(), "1");
		if("2".equals(docpublishtype)){
			int tmppos = doccontent.indexOf("!@#$%^&*");
			if(tmppos!=-1){
				doccontent = doccontent.substring(tmppos+8,doccontent.length());
			}
		}
		return doccontent;
	}
	
	/**
	 * 获取单附件打开的附件id
	 * @author wangqs
	 * @return -1:不单附件打开，大于0：单附件打开的附件id
	 * */
	public Map<String,String> getOpenAccInfo(String doccontent,String isOpenAttachment,int docid){
		Map<String,String> accInfo = new HashMap<String,String>();
		boolean isOpenAcc = false;
		
		int OpenAccOfFileid = -1;
		String filename = "";
		int versionid = -1;
		
		if(ifContentEmpty(doccontent)){
			RecordSet rs = new RecordSet();
			
				if("1".equals(isOpenAttachment)){  //目录是否开启单附件打开
					isOpenAcc = true;
				    rs.executeQuery("select a.id,a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from docimagefile a,imagefile b " +
					" where a.imagefileid=b.imagefileid and a.isextfile ='1' and a.docid=? order by versionid desc",docid);
					String id = "";
					isOpenAcc  = true;;
					int fileCount = 0;
					while(rs.next()){   //是否是单附件
						fileCount++;
						if("".equals(id)){
							id = rs.getString("id");
							OpenAccOfFileid = rs.getInt("imagefileid");
							versionid = rs.getInt("versionId");
							filename = rs.getString("imagefilename");
						}
						if(!id.equals(rs.getString("id"))){
					        isOpenAcc = false;
				        }
			
			        }
					if(fileCount == 0){
						isOpenAcc = false;
					}
			  }
		}
		
		accInfo.put(IS_OPEN_ACC, isOpenAcc ? "1" : "0");
		accInfo.put(ACC_FILE_ID, OpenAccOfFileid + "");
		accInfo.put(ACC_FILE_VERSION, versionid + "");
		accInfo.put(ACC_FILE_NAME, filename);
		return accInfo;
	}

	/**
	 * 获取主附件的附件id
	 * @author wangqs
	 * @return -1:不单附件打开，大于0：单附件打开的附件id
	 * */
	public Map<String,String> getMainAccInfo(String doccontent,String isOpenAttachment,int docid){
		Map<String,String> accInfo = new HashMap<String,String>();
		boolean isOpenAcc = false;

		int OpenAccOfFileid = -1;
		String filename = "";
		int versionid = -1;

		if(ifContentEmpty(doccontent)){
			RecordSet rs = new RecordSet();
			if("1".equals(isOpenAttachment)){  //目录是否开启单附件打开
				isOpenAcc = true;
				String mainimagefileid = DocDetailUtil.getMainImagefile(docid+"");
				Map<String,String> mainFileMap = new HashMap<String,String>();
				if("".equals(mainimagefileid)){
					mainFileMap = DocDetailUtil.getDocFirstfile(docid+"");
				}else{
					mainFileMap = DocDetailUtil.getFirstfileInfo(mainimagefileid);
				}
				String id = "";
				if(!"".equals(Util.null2s(mainFileMap.get("imagefileid"),""))){
					id = mainFileMap.get("id");
					OpenAccOfFileid = Util.getIntValue(mainFileMap.get("imagefileid"));
					versionid = Util.getIntValue(mainFileMap.get("versionId"));
					filename = mainFileMap.get("imagefilename");
				}else{
					rs.executeQuery("select a.id,a.imagefileid,a.imagefilename,a.docfiletype,a.versionId,b.filesize from docimagefile a,imagefile b " +							
							" where a.imagefileid=b.imagefileid and a.isextfile ='1' and a.docid=? order by versionid desc",docid);
					int fileCount = 0;
					while(rs.next()){   //是否是单附件
						fileCount++;
						if("".equals(id)){
							id = rs.getString("id");
							OpenAccOfFileid = rs.getInt("imagefileid");
							versionid = rs.getInt("versionId");
							filename = rs.getString("imagefilename");
						}
						if(!id.equals(rs.getString("id"))){
							isOpenAcc = false;
						}

					}
					if(fileCount == 0){
						isOpenAcc = false;
					}
				}
			}
		}
		accInfo.put(IS_OPEN_ACC, isOpenAcc ? "1" : "0");
		accInfo.put(ACC_FILE_ID, OpenAccOfFileid + "");
		accInfo.put(ACC_FILE_VERSION, versionid + "");
		accInfo.put(ACC_FILE_NAME, filename);
		return accInfo;
	}

	/**
	 * 获取文档置顶信息
	 * @author wangqs
	 * */
	public Map<String,Object> getTopSet(int docid,User user){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			return apidatas;
		}
		
		RecordSet rs = new RecordSet();
		rs.executeQuery("select istop,topstartdate,topenddate from docdetail where id=?",docid);
		if(!rs.next()){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(23230,user.getLanguage()));
			return apidatas;
		}
		
		apidatas.put("status",1);
		apidatas.put("isTop",rs.getInt("istop") == 1 ? "1" : "0");
		apidatas.put("fromDate",rs.getString("topstartdate"));
		apidatas.put("toDate",rs.getString("topenddate"));
		return apidatas;
	}
	
	/**
	 * 文档置顶、取消置顶
	 * @author wangqs
	 * @param docid-文档id
	 * @param fromDate-置顶开始日期
	 * @param fromDate-置顶结束日期
	 * @param operate-（1、置顶，0、取消置顶）
	 * @param user
	 * 
	 * */
	public Map<String,Object> setTop(int docid,String fromDate,String toDate,String operate,User user){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			return apidatas;
		}
		
		RecordSet rs = new RecordSet();
		if("1".equals(operate)){ //置顶
			String reistop = "";
			String CurrentDate = TimeUtil.getCurrentDateString();
			String CurrentTime = TimeUtil.getOnlyCurrentTimeString();
			String sql = "";
			if(CurrentDate.compareTo(fromDate)>=0&&((CurrentDate.compareTo(toDate)<=0&&!toDate.equals(""))||toDate.equals(""))){
				sql = "update docdetail set istop=1,topdate=?,toptime=?,topstartdate=?,topenddate=? where id = ?";
				reistop = "1";
			}else{
				sql = "update docdetail set istop=0, topdate=?,toptime=?,topstartdate=?,topenddate=? where id = ?";
				if(CurrentDate.compareTo(toDate)>0&&!toDate.equals("")){
					reistop = "0";
				}else
					reistop = "1";
			}
			rs.executeUpdate(sql,CurrentDate,CurrentTime,fromDate,toDate,docid);
			
			if(reistop.equals("0")){
				apidatas.put("status",-1);
				apidatas.put("msg",SystemEnv.getHtmlLabelName(500573, user.getLanguage()));//文档置顶失败
			}else{
				apidatas.put("status",1);
				apidatas.put("msg",SystemEnv.getHtmlLabelName(500579, user.getLanguage()));//文档置顶成功
			}
			  
		}else if("0".equals(operate)){ //取消置顶
			rs.executeUpdate("update docdetail set istop=0,topdate=null,toptime=null,topstartdate=null,topenddate='' where id = ?",docid);
			apidatas.put("status",1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(500580,user.getLanguage()));//取消置顶成功
		}else{
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19327,user.getLanguage()));//	数据异常
		}
		return apidatas;
	}
	
	/**
	 * 文档失效
	 * @author wangqs
	 * @param docid 文档id
	 * @param clientip 用户id
	 * @param user
	 * */
	public Map<String,Object> invalidate(int docid,String clientip,User user) throws Exception{
		Map<String,Object> apidatas = new HashMap<String,Object>();
		if(docid == 0){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(19711,user.getLanguage()));
			return apidatas;
		}
		
		RecordSet rs = new RecordSet();
		rs.executeQuery("select d.seccategory,d.docstatus,d.docedition,d.doceditionid,d.docsubject" +
				" from DocDetail d,DocSecCategory c where d.seccategory=c.id and d.id=?",docid);
		
		if(!rs.next()){
			apidatas.put("status",-1);
			apidatas.put("msg",SystemEnv.getHtmlLabelName(23230,user.getLanguage()));
			return apidatas;
		}
		
        int seccategory = rs.getInt("seccategory");
        
        String oldstatus = rs.getString("docstatus");
        String docstatus = oldstatus;
        int docEdition = rs.getInt("docedition");
        int docEditionId = rs.getInt("doceditionid");

        String docsubject = rs.getString("docsubject");           
        int doccreaterid = rs.getInt("doccreaterid");
        String docCreaterType = rs.getString("docCreaterType");
        
        int docInvalUserId = user.getUID();
        String docInvalUserType = user.getLogintype();
        String docInvalDate = TimeUtil.getCurrentDateString();
        String docInvalTime = TimeUtil.getOnlyCurrentTimeString();
        int approveType = 0;
        if(Util.getIntValue(docstatus) == 1 || Util.getIntValue(docstatus) == 2){
        	SecCategoryComInfo scc = new SecCategoryComInfo();
            if(scc.needApprove(seccategory,2)) {//如果需要审批
                docstatus = "3";
                approveType = 2;
            } else {//失效
                docstatus = "7";
                    
                if(scc.isEditionOpen(seccategory)) {//如果版本管理开启
                    if(docEditionId == -1){
                         rs.executeProc("SequenceIndex_SelectNextID", "doceditionid");
                         if (rs.next())
                        	 docEditionId = rs.getInt(1);
                    }
                    DocComInfo dc = new DocComInfo();
                    docEdition = dc.getEdition(docEditionId);
                    rs.executeUpdate(" update docdetail set docstatus = 7,ishistory = 1 where id <> ? " + 
                    		" and docedition > 0 and docedition < ? " + 
                    		" and doceditionid > 0 and doceditionid = ?",docid,docEdition,docEditionId);
                }
            }

            rs.executeUpdate(
                    " UPDATE DocDetail SET " +
                    " docstatus = ?" +
                    ",approvetype = ?" +
                    ",docEdition = ?" +
                    ",docEditionId = ?" +
                    ",docinvaluserid = ?" +
                    ",docInvalUserType = ?" +                  
                    ",docinvaldate = ?" + 
                    ",docinvaltime = ?" + 
                    " WHERE ID = ?" 
                    ,docstatus,approveType,docEdition,docEditionId,docInvalUserId,docInvalUserType,docInvalDate,docInvalTime,docid);

            apidatas.put("status",1);
            apidatas.put("msg", SystemEnv.getHtmlLabelName(500581,user.getLanguage()) );// 文档失效成功！

            DocDetailLog log = new DocDetailLog();
            log.resetParameter();
            log.setDocId(docid);
            log.setDocSubject(docsubject);
            log.setOperateType("14");
            log.setOperateUserid(docInvalUserId);
            log.setUsertype(docInvalUserType);
            log.setClientAddress(clientip);
            log.setDocCreater(doccreaterid);
            log.setCreatertype(docCreaterType);             
            log.setDocLogInfo();
            
        }
        if(approveType == 2){
        	DocSaveService saveService = new DocSaveService();
        	saveService.approveWorkflow(docid,oldstatus,approveType + "",user);
        }
		return apidatas;
	} 
	
	/**
	 * 判断正文是否为空
	 * @author wangqs
	 * */
	public static boolean ifContentEmpty(String doccontent){
		if(doccontent == null || doccontent.isEmpty() ){
			return true;
		}
		if("initFlashVideo();".equals(doccontent) || doccontent.indexOf("<body></body>") > -1){
			return true;
		}
		if("<p></p>".equals(doccontent)){
			return true;
		}
		if(doccontent.length() <= 50){  
			doccontent = doccontent.replace("<p>", "").replace("</p>", "").replace(" ","").replace("&nbsp;","");
			if(doccontent.isEmpty()){//E8内容只有<p>标签，或者<p>标签内只有空格
				return true;
			}
			if(doccontent.length() <= 10 && doccontent.contains(SystemEnv.getHtmlLabelName(156,7)) ){//内容较少，并且含“附件”关键字，默认用户写的内容是“详见附件”等语言
				return true;
			}
		}else if(doccontent.indexOf("<body") > - 1 && doccontent.indexOf("</body>") > -1) {
			int bodyStyleIndex = doccontent.indexOf("<body");
			int key2 = doccontent.indexOf(">", bodyStyleIndex);
			doccontent = doccontent.substring(key2+1, doccontent.indexOf("</body>"));
			doccontent = doccontent.replace("<p>", "").replace("</p>", "").replace(" ", "").replace("&nbsp;", "");
			if (doccontent.isEmpty()) {  //E9内容只有<p>标签，或者<p>标签内只有空格
				return true;
			}
			if (doccontent.length() <= 10 && doccontent.contains(SystemEnv.getHtmlLabelName(156,7))) {  //内容较少，并且含“附件”关键字，默认用户写的内容是“详见附件”等语言
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 与人员相关的文档属性
	 * @author wangqs
	 * */
	public static String getUserLink(String userid,String userType,User user) throws Exception{
		return DocDetailService.getUserLink(userid,userType,user,false);
	}

	public static String getUserLink(String userid,String userType,User user,Boolean isMobile) throws Exception{
		String str = "";
		CustomerInfoComInfo cici = new CustomerInfoComInfo();
		ResourceComInfo rci = new ResourceComInfo();

		if("2".equals(userType)){
			String username = cici.getCustomerInfoname(userid);
			if(username != null || !username.isEmpty()){
				if(isMobile){
					if("1".equals(user.getLogintype())){
						str = "<a href='javascript:doc_openHrmCard(" + userid + ")' style='color:#55B1F9' >" +
								Util.toScreen(username,user.getLanguage()) +
								"</a>";
					}else {
						str = Util.toScreen(username,user.getLanguage());
					}
				}else{
					if("1".equals(user.getLogintype())){
						str = "<a href='"+weaver.general.GCONST.getContextPath()+"/CRM/data/ViewCustomer.jsp?CustomerID=" + userid + "'  target='_new'>" +
								Util.toScreen(username,user.getLanguage()) +
								"</a>";
					}else{
						str = Util.toScreen(username,user.getLanguage());
					}
				}
			}
		} else {
			String username = rci.getResourcename(userid);
			if(username != null || !username.isEmpty()){
				if(isMobile){
					if("1".equals(user.getLogintype())){
						str = "<a href='javascript:doc_openHrmCard(" + userid + ")' style='color:#55B1F9' >" +
								Util.toScreen(username,user.getLanguage()) +
								"</a>";
					}else {
						str = Util.toScreen(username,user.getLanguage());
					}
				}else{
					if("1".equals(user.getLogintype())){
						str = "<a href='javaScript:openhrm(" + userid + ");' onclick='pointerXY(event);'>" +
								Util.toScreen(username,user.getLanguage()) +
								"</a>";
					}else{
						str = Util.toScreen(username,user.getLanguage());
					}
				}

			}
		}


		return str;
	}
	
	
	public Map<String,Object> publishDoc(int docid,String ipAddress,User user){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		DocMonitorManager docMonitorManager=new DocMonitorManager();
		
		docMonitorManager.setClientAddress(ipAddress);
		try{
			docMonitorManager.executeDocMonitor(docid + "","publishDoc",user);
			apidatas.put("status", 1);
			apidatas.put("msg", SystemEnv.getHtmlLabelName(33179,user.getLanguage()) + "!");// 发布成功
		}catch(Exception e){
			e.printStackTrace();
			apidatas.put("status", -1);
			apidatas.put("msg", SystemEnv.getHtmlLabelName(129250,user.getLanguage()) + "!");// 发布失败
		}
		return apidatas;
	}
	
	public Map<String,Object> reOpen(int docid,String clientip,User user) throws Exception{
		RecordSet rs = new RecordSet();
		
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		rs.executeQuery("select docstatus,seccategory,docsubject,docedition,doceditionid from DocDetail where id=" + docid);
		if(!rs.next()){
			apidatas.put("status", 0);
			return apidatas;
		}
		
        int docEdition = Util.getIntValue(rs.getString("docedition"),-1);
        int docEditionId = Util.getIntValue(rs.getString("doceditionid"),-1);
        
        String docsubject = Util.null2String(rs.getString("docsubject"));        
        int doccreaterid = Util.getIntValue(rs.getString("doccreaterid"),0);
        String docCreaterType = Util.null2String(rs.getString("docCreaterType"));
        int docstatus = rs.getInt("docstatus");
        int seccategory = rs.getInt("seccategory");
        
        String formatdate = TimeUtil.getCurrentDateString();
        String formattime = TimeUtil.getOnlyCurrentTimeString();
        
        int userid = user.getUID();
        String usertype = user.getLogintype();
        
        if(docstatus == 5|| docstatus == 8){
            //重新打开
            if(docstatus == 5)
                docstatus = 2;
            if(docstatus == 8)
                docstatus = 7;
            
            SecCategoryComInfo scc = new SecCategoryComInfo();
            DocManager dm = new DocManager();
            DocComInfo dc = new DocComInfo();
            if(scc.isEditionOpen(seccategory)) {//如果版本管理开启
                if(docEditionId==-1)
                    docEditionId = dm.getNextEditionId(rs);
                docEdition = dc.getEdition(docEditionId);
                rs.execute(" update docdetail set docstatus = 7,ishistory = 1 where id <> " + docid + " and docedition > 0 and docedition < " + docEdition + " and doceditionid > 0 and doceditionid = " + docEditionId);
            }

            rs.execute(
                    " UPDATE DocDetail SET " +
                    " docstatus = " + docstatus +
                    ",docEdition = " + docEdition +
                    ",docEditionId = " + docEditionId +
                    ",docreopenuserid = " + userid +
                    ",docReopenUserType = '" + usertype +"'"+                    
                    ",docreopendate = '" + formatdate + "'" +
                    ",docreopentime = '" + formattime + "'" +
                    ",docinvaluserid = " + userid +
                    ",docInvalUserType = '" + usertype + "'" +                  
                    ",docinvaldate = '" + formatdate + "'" +
                    ",docinvaltime = '" + formattime + "'" +
                    " WHERE ID = " + docid
            );
            
            DocDetailLog log = new DocDetailLog();
            log.resetParameter();
            log.setDocId(docid);
            log.setDocSubject(docsubject);
            log.setOperateType("6");
            log.setOperateUserid(userid);
            log.setUsertype(usertype);           
            log.setClientAddress(clientip);
            log.setDocCreater(doccreaterid);
            log.setCreatertype(docCreaterType);
            log.setDocLogInfo();
        }
        apidatas.put("status", 1);
        return apidatas;
	}
	
	public Map<String,Object> reLoad(int docid,String clientip,User user) throws Exception{
		RecordSet rs = new RecordSet();
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		rs.executeQuery("select docstatus,seccategory,docsubject from DocDetail where id=" + docid);
		if(!rs.next()){
			apidatas.put("status", 0);
			return apidatas;
		}
		
		int docstatus = rs.getInt("docstatus");
        int seccategory = rs.getInt("seccategory");
        String docsubject = Util.null2String(rs.getString("docsubject"));        
        int doccreaterid = Util.getIntValue(rs.getString("doccreaterid"),0);
        
        SecCategoryComInfo scc = new SecCategoryComInfo();
		if(scc.needApprove(seccategory)) {//如果需要审批
            docstatus = 2;
        } else {
            docstatus = 1;
        }
		
		String sql = "update DocDetail set docarchiveuserid=?,docarchivedate=?,docarchivetime=?,docstatus=? where id=?";

        RecordSet statement = new RecordSet();
        try {

            statement.executeUpdate(sql,0,"","",docstatus,docid);
            
            DocDetailLog log = new DocDetailLog();
            log.resetParameter();
            log.setDocId(docid);
            log.setDocSubject(docsubject);
            log.setOperateType("8");
            log.setOperateUserid(user.getUID());
            log.setUsertype(user.getLogintype());
            log.setClientAddress(clientip);
            log.setDocCreater(doccreaterid);
            log.setDocLogInfo();
        } catch (Exception e) {
            rs.writeLog(e);
            throw e;
        } finally {
            try {
            } catch (Exception ex) {
            }
        }
        apidatas.put("status", 1);
        return apidatas;
	}
	
	
	public void resizeCount(int docid,String type){
		String sql = "";
		RecordSet rs = new RecordSet();
		
		if(READ_COUNT.equals(type)){
			/*sql = "update DocDetail set sumReadcount=(select count(1) from DocDetailLog " +
					" where operatetype = 0 and docid="+docid+") where id=" + docid;*/
			syncReadCount();
		}else if(REPLY_COUNT.equals(type)){
			sql = "update DocDetail set replaydoccount=(select count(1) from doc_reply where docid="+docid+") where id=" + docid;
		}else if(DOWNLOAD_COUNT.equals(type)){
			sql = "update DocDetail set sumDownload=(select count(1) from DownloadLog where docid="+docid+") where id=" + docid;
			String dbtype = rs.getDBType();
			if("mysql".equals(dbtype)){
				sql = "update DocDetail set sumDownload=sumDownload+1 where id=" + docid;
			}
		}else if(SCORE_COUNT.equals(type)){
			String dbtype = rs.getDBType();
			String isnull = "isnull";
			if("oracle".equals(dbtype)){  //isoracle
				isnull = "nvl";
			}else if("mysql".equals(dbtype)){
				isnull = "ifnull";
			}
			sql = "update DocDetail set sumMark=(select " + isnull + "(sum(mark),0) from DocMark " +
					" where docid="+docid+") where id=" + docid;
		}
		
		if(!sql.isEmpty()){
			//rs.execute(sql);
		}
	}
	
	public void syncReadCount(){
		StaticObj staticobj = StaticObj.getInstance();
		Object lock = new Object();
		synchronized (lock) {
			String rtime = "";
			if(staticobj.getObject("DocReadCountInfo") == null){
				rtime = TimeUtil.getCurrentTimeString();
				staticobj.putRecordToObj("DocReadCountInfo", "lasttime", rtime);
				staticobj.putRecordToObj("DocReadCountInfo", "locked", "1");
				DocReadCountThread thread = new DocReadCountThread();
				thread.start();
			}else{
				String lasttime = (String)(staticobj.getRecordFromObj("DocReadCountInfo", "lasttime"));
				String locked = (String)(staticobj.getRecordFromObj("DocReadCountInfo", "locked"));
				Calendar c = Calendar.getInstance();
				Calendar c2 = TimeUtil.getCalendar(lasttime);
				long t1 = c.getTimeInMillis();
				long t2 = c2.getTimeInMillis();
				if(t1 - t2 >= 10*60*1000 && "0".equals(locked)){ //10分钟
					rtime = TimeUtil.getTimeString(c);
					staticobj.putRecordToObj("DocReadCountInfo", "locked", "1");
					staticobj.putRecordToObj("DocReadCountInfo", "lasttime", rtime);
					DocReadCountThread thread = new DocReadCountThread();
					thread.start();
				}
				
			}
			      
		}
		
	}
	
    public Map<String,Object> updateBizStateForDoc(int docid, User user, int viewReplyFlag, int viewPraiseFlag, int viewMessageFlag) throws Exception {
        RecordSet rs = new RecordSet();
        Map<String, Object> apidatas = new HashMap<String, Object>();
        if(1 == viewReplyFlag) {
            SendMsgForNewDocThread.updateBizStateForCenter(user, docid, "138");
        }
        if(1 == viewPraiseFlag) {
            SendMsgForNewDocThread.updateBizStateForCenter(user, docid, "139");
        }
        if(1 == viewMessageFlag) {
            SendMsgForNewDocThread.updateBizStateForCenter(user, docid, Util.null2String(MessageType.DOC_NEW_DOC.getCode()));
        }

        // 返回成功
        apidatas.put("status", 1);
        return apidatas;
    }

}
