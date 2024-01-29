package com.api.doc.mobile.systemDoc.util;

import java.text.ParseException;

import com.alibaba.fastjson.JSON;
import com.api.doc.detail.util.DocSensitiveWordsUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import weaver.conn.RecordSet;
import weaver.docs.docs.DocComInfo;
import weaver.docs.docs.DocImageManager;
import weaver.docs.docs.util.DesUtils;
import weaver.docs.webservices.DocServiceImpl;
import weaver.favourite.SysFavouriteInfo;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.sm.SM4Utils;
import weaver.splitepage.transform.SptmForDoc;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.setting.HrmUserSettingComInfo;
import weaver.workflow.search.WfAdvanceSearchUtil;
import weaver.wps.sdk.WebOfficeSdkUtil;

import com.api.browser.service.impl.DocFullSearchUtil;
import com.api.doc.detail.service.DocDetailService;
import com.api.doc.detail.service.DocViewPermission;
import com.api.doc.detail.util.ImageConvertUtil;
import com.api.doc.search.bean.RightMenu;
import com.api.doc.search.util.ConditionUtil;
import com.api.doc.search.util.DocCondition;
import com.api.doc.search.util.DocListUtil;
import com.api.doc.search.util.DocTableType;
import com.api.doc.search.util.FullSearchUtil;
import com.api.doc.search.util.RightMenuType;
import com.api.networkdisk.cmd.AddHistorySearchCmd;
import com.api.networkdisk.util.DocIconUtil;
import com.engine.doc.util.DocApplySettingUtil;
import com.engine.doc.util.TimeZoneUtil;

public class SystemDocUtil {


    public static  final String TIMEOUT = "timeout"; //超时时间
    public static  final String TIMELIMIT = "timelimit"; //是否获取有时限的ddcode
    public static  final String ADMINRIGHT = "adminright"; //给管理员的下载权限
    public static  final String USERID = "userid"; //用户id
    public static  final String FILEID = "fileid"; //附件id
    public static  final String BEGINTIME = "begintime"; //ddcode生成的时间
    public static  final String ISOVERTIME = "isovertime"; //ddcode是否超时

    public static  final String Y = "y"; //ddcode是否超时
    public static  final String N = "n"; //ddcode是否超时

    /**
     * 将list中的文档数据转换为页面上展示的数据格式
     * @param results
     * @return
     */
    public static String secidsWhere = "";
    public static List<Map<String,Object>> getTranDocData(Map<String, Object> results){
        List<Map<String,Object>> docs = new ArrayList<Map<String,Object>>();
        //list 是getDocumentList2方法返回来的，存储查询出的文档属性的
        if(results != null && results.get("list") != null){
            List<Map<String, String>> list = (List<Map<String, String>>)results.get("list");
            //从list中循环取出所有的map
            for(Map<String,String> map : list){
                Map<String,Object> doc = new HashMap<String,Object>();
                docs.add(doc);
                doc.put("docid",map.get("docid"));
                //doc.put("docTitle",map.get("docsubject"));
				doc.put("docTitle",StringEscapeUtils.unescapeHtml(map.get("docsubject")));
                doc.put("extName",map.get("doctype"));

                doc.put("createTime",map.get("doccreatedate"));
                doc.put("doctype",map.get("doctype"));
                //无专用图片的用此图片
                Map<String, String> iconMap = DocIconUtil.getDocIconDetail(map.get("doctype"));
                doc.put("icon", iconMap);
                doc.put("userid",map.get("ownerid"));
                doc.put("username",map.get("owner"));
                doc.put("isnew",map.get("isnew"));
                doc.put("updateTime",map.get("docupdatedate"));
                doc.put("docstatus",map.get("docstatus"));
            }
        }
        return docs;
    }
    
    public static List<Map<String,Object>> getTranDocData(List<Object> list){
        List<Map<String,Object>> docs = new ArrayList<Map<String,Object>>();
        for(Object obj : list){
        	Map<String,String> map = (Map<String,String>)obj;
            Map<String,Object> doc = new HashMap<String,Object>();
            docs.add(doc);
            doc.put("docid",map.get("docid"));
            //doc.put("docTitle",map.get("docsubject"));
			doc.put("docTitle",StringEscapeUtils.unescapeHtml(map.get("docsubject")));
            doc.put("extName",map.get("doctype"));

            doc.put("createTime",map.get("doccreatedate"));
            doc.put("doctype",map.get("doctype"));
            //无专用图片的用此图片
            Map<String, String> iconMap = DocIconUtil.getDocIconDetail(map.get("docExtendName"));
            doc.put("icon", iconMap);
            doc.put("userid",map.get("ownerid"));
            doc.put("username",map.get("owner"));
            doc.put("isnew",map.get("isnew"));
            doc.put("updateTime",map.get("docupdatedate"));
            doc.put("docstatus",map.get("docstatus"));
        }
        return docs;
    }
    

    
    public static List<Map<String,Object>> getFullSearch(Map<String,Object> params,User user,int pageNum,int pageSize,DocTableType docTable) throws Exception{

        Map<String,String> _params = new HashMap<String,String>();
    	for(String key : params.keySet()){
            String value = Util.null2String(params.get(key));
            if(value.isEmpty()){
                continue;
            }
            _params.put(key,value);
        }
    	
    	_params.put("pageId",docTable == null ? "" : docTable.getPageUid());
    	
    	FullSearchUtil fullSearch = new FullSearchUtil();
    	Map<String,Object> searchParams = fullSearch.getParams(_params,user);
    	
    	Map<String,Object> sortMap = new LinkedHashMap<String,Object>();
    	sortMap.put("lastModDate","false");
        sortMap.put("lastModTime","false");
        // 必须包含
        searchParams.put("loginid",user.getLoginid());
        searchParams.put("currentUserObject",user);
        searchParams.put("page",pageNum);
        searchParams.put("pageSize",pageSize);
        searchParams.put("schemaType","DOCSEARCH");
        
        Map<String, Object> searchResult = DocFullSearchUtil.quickSearch(searchParams,sortMap,null);
        searchResult = searchResult == null ? new HashMap<String,Object>() : searchResult;
        // 查询结果集
        List<Object> docList = (List<Object>) searchResult.get("result");
        docList = docList == null ? new ArrayList<Object>() : docList;
        
        List<Map<String,Object>> docs = new ArrayList<Map<String,Object>>();
        ResourceComInfo rci = new ResourceComInfo();
        SptmForDoc sptmForDoc=new SptmForDoc();
        String ids = "";
        RecordSet rs = new RecordSet();
        for(Object obj : docList){
        	Map<String,String> map = (Map<String,String>)obj;
        	Map<String,Object> doc = new HashMap<String,Object>();
            docs.add(doc);
            
            
            if(Util.getIntValue(map.get("id")) > 0){
            	ids += "," + map.get("id");
            }
            String docid = map.get("id");
            doc.put("docid",docid);
            //doc.put("docTitle",map.get("title"));
			doc.put("docTitle",StringEscapeUtils.unescapeHtml(map.get("title")));
            doc.put("extName",Util.null2String(map.get("docExtendName")));

            doc.put("createTime",map.get("createDate") + " " + map.get("createTime"));
            String docextendname = "";
            String sql = "select docextendname from docdetail where id = ?";
            rs.executeQuery(sql,docid);
            if(rs.next()){
                docextendname = Util.null2String(rs.getString("docextendname"));
            }
            doc.put("doctype",docextendname);
            //无专用图片的用此图片
            Map<String, String> iconMap = DocIconUtil.getDocIconDetail(docextendname);
            doc.put("icon", iconMap);
//            Map<String, String> iconMap = getIconByExtname(map.get("docExtendName"));
//            doc.put("icon", iconMap);
            doc.put("userid",map.get("ownerid"));
            doc.put("username",rci.getResourcename(map.get("ownerid")));
            doc.put("isnew","0");
            doc.put("updateTime",map.get("lastModDate") + " " + map.get("lastModTime"));
        	String docstatusname = sptmForDoc.getDocStatus3(map.get("id"), ""+user.getLanguage()+"+"+map.get("docStatus")+"+"+map.get("directoryId"));//获取文档状态
            doc.put("docstatus",docstatusname);

            if(DocTableType.MY_DOC_TABLE != docTable){
                String belongtoshow = "";
                String belongtoids = "";
                String account_type = "";

                if(user != null) {
                    HrmUserSettingComInfo userSetting = new HrmUserSettingComInfo();
                    belongtoshow = userSetting.getBelongtoshowByUserId(user.getUID() + "");
                    belongtoids = user.getBelongtoids();
                    account_type = user.getAccount_type();
                }
                sql = "select count(0) as c from DocDetail t where t.id="+docid+" and t.doccreaterid<>"+user.getUID()+" and not exists (select 1 from docReadTag where userid="+user.getUID()+" and docid=t.id)";
                if(belongtoshow.equals("1")&&account_type.equals("0")&&!belongtoids.equals("")){
                    belongtoids += ","+user.getUID();
                    sql = "select count(0) as c from DocDetail t where t.id="+docid+" and t.doccreaterid not in ("+belongtoids+" ) and not exists (select 1 from docReadTag where userid in ("+belongtoids+") and docid=t.id)";
                }
                rs.execute(sql);
                if(rs.next()&&rs.getInt("c")>0) {
                    doc.put("isnew", "1");
                } else {
                    doc.put("isnew", "0");
                }
            }
        }

//        DocComInfo dci = new DocComInfo();
//        dci.getIsNewDoc()
//        List<Map<String,Object>> docsResult = new ArrayList<Map<String,Object>>();
//        if(!ids.isEmpty() && DocTableType.MY_DOC_TABLE != docTable){ //有文档，并且不是我的文档
//            bb.writeLog("th==systemDocUtil==157==ids==",ids);
//        	ids = ids.substring(1);
//        	RecordSet rs = new RecordSet();
//	        rs.executeQuery("select a.id from DocDetail a where exists(" +
//	        		"select 1 from docReadTag b where a.id=b.docid and b.userType=" + user.getLogintype()+
//        	        		" and b.docid in (" + ids + ") and b.userid=" + user.getUID() + ")");
//	        while(rs.next()){
//
//	        	for(Map<String,Object> data : docs){
//	        		if(rs.getString("id").equals(Util.null2String(data.get("id")))){
//	        			data.put("isnew", "1");
//	        			docsResult.add(data);
//	        			break;
////	        			continue;
//	        		}
//	        	}
//	        }
//        }
        
        
        return docs;
        
    }
    
    public static Map<String,String> getCreateDate(Map<String,Object> params,User user){
		String select = Util.null2String(params.get(DocCondition.DOC_CREATEDATE_SELECT + ConditionUtil.DATE_SELECT));
		String from = Util.null2String(params.get(DocCondition.DOC_CREATEDATE_SELECT + ConditionUtil.DATE_FROM));
		String to = Util.null2String(params.get(DocCondition.DOC_CREATEDATE_SELECT + ConditionUtil.DATE_TO));
		
		Map<String,String> dateMap = new HashMap<String,String>();
		if("0".equals(select)){
			return dateMap;
		}
		dateMap = DocListUtil.packDate(select,from,to);
		
		from = dateMap.get("from");
		to = dateMap.get("to");
		
		if(from != null && !from.isEmpty()){
			from = new TimeZoneUtil().getServerDate(from + " 00:00:00");
		}
		if(to != null && !to.isEmpty()){
			to = new TimeZoneUtil().getServerDate(to + " 23:59:59");
		}
		
		dateMap.put(DocCondition.DOC_CREATEDATE_SELECT.getName() + ConditionUtil.DATE_FROM,from);
		dateMap.put(DocCondition.DOC_CREATEDATE_SELECT.getName() + ConditionUtil.DATE_TO,to);
		
		return dateMap;
	}
    
    public static List<String> getConditions(Map<String,Object> params,User user) throws Exception{
    	 List<String> conditions = new ArrayList<String>();
    	 String bySearch = Util.null2String(params.get("bySearch"));
    	 
    	//如果是搜索,加一个拼接条件
         if("1".equals(bySearch)){
             String keyword = Util.null2String(params.get("docsubject"));
             // replaceAll 第一个参数为一个正则表达式，将所有匹配此正则表达式的替换为第二个参数
             if(!keyword.isEmpty()){
             	conditions.add("t1.docsubject like '%"+keyword.replaceAll("'","''")+"%'");
             	AddHistorySearchCmd ahs = new AddHistorySearchCmd(params,user);
             	ahs.addHistorySearch(keyword,5);
             }
         }else{ //高级搜索
        	 for(String key : params.keySet()){
        		 String value = Util.null2String(params.get(key));
        		 if(value.isEmpty()){
        			 continue;
        		 }
        		 
        		 if(DocCondition.DOC_SUBJECT.getName().equals(key)){
        			 conditions.add("t1.docsubject like '%"+value.replaceAll("'","''")+"%'");
        		 }else if(DocCondition.DOC_CREATER_ID.getName().equals(key)){
        			 conditions.add("t1.doccreaterid=" + Util.getIntValue(value,0));
        		 }else if(DocCondition.DEPARTMENT_ID.getName().equals(key)){
        			 conditions.add("exists(select 1 from HrmResource where departmentid=" + Util.getIntValue(value,0) + " and id=t1.doccreaterid)");
        		 }else if(DocCondition.OWNER_ID.getName().equals(key)){
        			 conditions.add("t1.ownerid=" + Util.getIntValue(value,0));
        		 }else if(DocCondition.OWNER_DEPARTMENT_ID.getName().equals(key)){
        			 conditions.add("exists(select 1 from HrmResource where departmentid=" + Util.getIntValue(value,0) + " and id=t1.ownerid)");
        		 }else if(DocCondition.DOC_NO.getName().equals(key)){
        			 conditions.add("t1.doccode like '%"+value.replaceAll("'","''")+"%'");
        		 }else if((DocCondition.DOC_CREATEDATE_SELECT.getName() + ConditionUtil.DATE_FROM).equals(key)){
        			 conditions.add("t1.doccreatedate >= '"+value+"'");
        		 }else if((DocCondition.DOC_CREATEDATE_SELECT.getName() + ConditionUtil.DATE_TO).equals(key)){
        			 conditions.add("t1.doccreatedate <= '"+value+"'");
        		 }else if((DocCondition.DOC_LAST_MODDATE.getName() + ConditionUtil.DATE_FROM).equals(key)){
        			 conditions.add("t1.doclastmoddate >= '"+value+"'");
        		 }else if((DocCondition.DOC_LAST_MODDATE.getName() + ConditionUtil.DATE_TO).equals(key)){
        			 conditions.add("t1.doclastmoddate <= '"+value+"'");
        		 }else if(DocCondition.SEC_CATEGORY.getName().equals(key)){
                     conditions.add("t1.seccategory = "+ Util.getIntValue(value,0));
                 }else if("doccreatedateselect".equals(key)){
                     conditions.add(handDateCondition("doccreatedateselect","doccreatedatefrom","doccreatedateto","t1.doccreatedate",params));
                 }else if("doclastmoddateselect".equals(key)){
                     conditions.add(handDateCondition("doclastmoddateselect","doclastmoddatefrom","doclastmoddateto","t1.doclastmoddate",params));
                 }
        	 }
        	 
        	 //密级等级
        	 String secretSql = DocListUtil.getSecretSql(user,"t1.secretLevel");
        	 if(!secretSql.isEmpty()){
        		 conditions.add(secretSql);
        	 }
         }
    	 return conditions;
    }

    public static String handDateCondition(String namefiled, String from, String to,
                                           String tname,Map<String, Object> params) throws  ParseException {

        String createdateselect = Util.null2String(params.get(namefiled));
        String condition = "";

        if (!createdateselect.equals("")) {
            if (WfAdvanceSearchUtil.TODAY.equals(createdateselect))
                condition = tname + ">='" + TimeUtil.getToday()
                        + "'  and  " + tname + "  <='" + TimeUtil.getToday()
                        + " 23:59:59'  ";
            else if (WfAdvanceSearchUtil.WEEK.equals(createdateselect))
                condition =  tname + ">='"
                        + TimeUtil.getFirstDayOfWeek() + "'  and  " + tname
                        + "<='" + TimeUtil.getLastDayOfWeek() + "'";
            else if (WfAdvanceSearchUtil.MONTH.equals(createdateselect))
                condition =  tname + ">='"
                        + TimeUtil.getFirstDayOfMonth() + "'  and  " + tname
                        + "<='" + TimeUtil.getLastDayOfMonth() + "'";
            else if (WfAdvanceSearchUtil.SEASON.equals(createdateselect))
                condition = tname + ">='"
                        + TimeUtil.getFirstDayOfSeason() + "'  and  " + tname
                        + "<='" + TimeUtil.getLastDayDayOfSeason() + "'";
            else if (WfAdvanceSearchUtil.YEAR.equals(createdateselect))
                condition = tname + ">='"
                        + TimeUtil.getFirstDayOfTheYear() + "' and " + tname
                        + "<='" + TimeUtil.getLastDayOfYear() + "'";
            else if (WfAdvanceSearchUtil.PREMONTH.equals(createdateselect))
                condition = tname + ">='"
                        + TimeUtil.getLastMonthBeginDay() + "' and " + tname
                        + "<='" + TimeUtil.getLastMonthEndDay() + "'";
            else if (WfAdvanceSearchUtil.PREYEAR.equals(createdateselect))
                condition = tname + ">='"
                        + TimeUtil.getFirstDayOfLastYear() + "' and " + tname
                        + "<='" + TimeUtil.getEndDayOfLastYear() + "'";


        }
        return condition;

    }


    /**
     *获取默认配置
     * @param param
     * @return param
     */
    public static String getDefaultSet(String param) {
        BaseBean bb = new BaseBean();

        param = Util.null2String(bb.getPropValue("mobile_doc_detail",param)) ;

        param = "0".equals(param) ? param : "1";

        return param;
    }

    /**
     *删除文档数据
     * @param documentid 文档id
     * @param user 用户对象
     * @return result
     */
    public Map<String, Object> deleteDoc(String documentid, User user)  {
        Map<String, Object> result = new HashMap<String, Object>();
        try{
            int docId=Util.getIntValue(documentid,-1);
            if(user==null){
                result.put("api_status",false);
                result.put("msg", ""+ SystemEnv.getHtmlLabelName(10003423,weaver.general.ThreadVarLanguage.getLang())+"");
                return result;
            }
            if(docId<=0){
                result.put("api_status",false);
                result.put("msg", ""+ SystemEnv.getHtmlLabelName(58,weaver.general.ThreadVarLanguage.getLang())+"id"+ SystemEnv.getHtmlLabelName(385284,weaver.general.ThreadVarLanguage.getLang())+"");
                return result;
            }
            DocServiceImpl docServiceImpl=new DocServiceImpl();
            int deleteReturn =docServiceImpl.deleteDocByUser(docId,user);
            if(deleteReturn==0){
                result.put("api_status",false);
                result.put("msg", ""+ SystemEnv.getHtmlLabelName(10003428,weaver.general.ThreadVarLanguage.getLang())+"");
                return result;
            }
        }catch(Exception ex){
            result.put("api_status",false);
            result.put("msg", ""+ SystemEnv.getHtmlLabelName(10003430,weaver.general.ThreadVarLanguage.getLang())+"");
            return result;
        }
        result.put("api_status",true);
        return result;
    }

    /**
     *取消收藏文档
     * @param documentid 文档id
     * @param user 用户对象
     * @return result
     */
    public Map<String, Object> undoCollect(String documentid, User user)  {
        Map<String, Object> result = new HashMap<String, Object>();
        try{
            int docId=Util.getIntValue(documentid,-1);
            if(user==null){
                result.put("api_status",false);
                result.put("msg", SystemEnv.getHtmlLabelName(500584,user.getLanguage()));// "未登录或登录超时"
                return result;
            }
            if(docId<=0){
                result.put("api_status",false);
                result.put("msg", "docid is empty!");
                return result;
            }
            List sysfavouriteids=new ArrayList();
            int sysfavouriteid=0;
            RecordSet rs = new RecordSet();
            rs.executeQuery("select  id from SysFavourite where resourceId="+user.getUID()+" and favouritetype=1 and favouriteObjId="+docId);
            while(rs.next()){
                sysfavouriteid=Util.getIntValue(rs.getString("id"),0);
                if(sysfavouriteid>0){
                    sysfavouriteids.add(""+sysfavouriteid);
                }
            }
            SysFavouriteInfo sf=null;
            for(int i=0;i<sysfavouriteids.size();i++){
                sysfavouriteid=Util.getIntValue((String)sysfavouriteids.get(i));
                if(sysfavouriteid<=0){
                    continue;
                };
                sf=new SysFavouriteInfo();
                sf.setSysfavouriteid(""+sysfavouriteid);
                sf.setUserid(user.getUID());
                sf.deleteFavourites();
            }


        }catch(Exception ex){
            result.put("api_status",false);
            result.put("msg", SystemEnv.getHtmlLabelName(500545,user.getLanguage()));
            return result;
        }
        result.put("api_status",true);
        return result;
    }

    /**
     * 取消收藏文档或者流程
     * @param favobjid
     * @param favtype
     * @param user
     * @return
     */
    public Map<String, Object> undoCollect(String favobjid,String favtype, User user)  {
        String documentid = favobjid;
        Map<String, Object> result = new HashMap<String, Object>();
        try{
            int docId=Util.getIntValue(documentid,-1);
            if(user==null){
                result.put("api_status",false);
                result.put("msg", SystemEnv.getHtmlLabelName(500584,user.getLanguage()));// "未登录或登录超时"
                return result;
            }
            if(docId<=0){
                result.put("api_status",false);
                if("1".equals(favtype)){   //文档
                    result.put("msg", "docid is empty!");
                }else if("2".equals(favtype)){   //流程
                    result.put("msg", "workflow ID is empty!");
                }
                return result;
            }
            List sysfavouriteids=new ArrayList();
            int sysfavouriteid=0;
            RecordSet rs = new RecordSet();
            rs.executeQuery("select  id from SysFavourite where resourceId="+user.getUID()+" and favouritetype=" + favtype + " and favouriteObjId="+docId);
            while(rs.next()){
                sysfavouriteid=Util.getIntValue(rs.getString("id"),0);
                if(sysfavouriteid>0){
                    sysfavouriteids.add(""+sysfavouriteid);
                }
            }
            SysFavouriteInfo sf=null;
            for(int i=0;i<sysfavouriteids.size();i++){
                sysfavouriteid=Util.getIntValue((String)sysfavouriteids.get(i));
                if(sysfavouriteid<=0){
                    continue;
                };
                sf=new SysFavouriteInfo();
                sf.setSysfavouriteid(""+sysfavouriteid);
                sf.setUserid(user.getUID());
                sf.deleteFavourites();
            }


        }catch(Exception ex){
            result.put("api_status",false);
            if("1".equals(favtype)){
                result.put("msg", "Cancel Collection Document Exceptions");
            }else if("2".equals(favtype)){
                result.put("msg", "Cancel collection Workflow exception");
            }
            return result;
        }
        result.put("api_status",true);
        return result;
    }

    /**
     *收藏文档
     * @param documentid 文档id
     * @param user 用户对象
     * @return result
     */
    public Map<String, Object> doCollect(String documentid, User user)  {
        Map<String, Object> result = new HashMap<String, Object>();
        try{
            int docId=Util.getIntValue(documentid,-1);
            if(user==null){
                result.put("api_status",false);
                result.put("msg", SystemEnv.getHtmlLabelName(500584,user.getLanguage()));// "未登录或登录超时"
                return result;
            }
            if(docId<=0){
                result.put("api_status",false);
                result.put("msg", "docid id empty!");
                return result;
            }

            String docSubject="";
            RecordSet rs = new RecordSet();
            rs.executeQuery("select  docSubject from DocDetail where id="+docId);
            if(rs.next()){
                docSubject = Util.null2String(rs.getString("docSubject"));
            }

            SysFavouriteInfo sf=new SysFavouriteInfo();
            sf.setFavouriteid("-1");
            sf.setPagename(docSubject);
            sf.setUrl("/docs/docs/DocDsp.jsp?id="+docId);
            sf.setImportlevel("1");
            sf.setUserid(user.getUID());
            sf.setType("1");
            sf.setFavouriteObjId(docId);
            sf.saveFavouritesFromPage();

        }catch(Exception ex){
            result.put("api_status",false);
            result.put("msg", SystemEnv.getHtmlLabelName(500545,user.getLanguage()));
            return result;
        }
        result.put("api_status",true);
        return result;
    }

    /**
     * 收藏文档或者流程
     * @param user
     * @param params
     * @return
     */
    public Map<String, Object> doCollect(User user,Map<String,String> params)  {
        Map<String, Object> result = new HashMap<String, Object>();
        String objid = Util.null2String(params.get("favobjid"));
        String favtype = Util.null2String(params.get("favtype"));
        String favid = Util.null2String(params.get("favid"));
        try{
            int docId=Util.getIntValue(objid,-1);
            if(user==null){
                result.put("api_status",false);
                result.put("msg", SystemEnv.getHtmlLabelName(500584,user.getLanguage()));// "未登录或登录超时"
                return result;
            }
            if(docId<=0){
                if("1".equals(favtype)){   //文档
                    result.put("api_status",false);
                    result.put("msg", "Docid is empty!");
                }else if("2".equals(favtype)){   //流程
                    result.put("api_status",false);
                    result.put("msg", "Workflow ID is empty!");
                }
                return result;
            }

            String docSubject="";
            RecordSet rs = new RecordSet();
            if("1".equals(favtype)){
                rs.executeQuery("select  docSubject from DocDetail where id="+docId);
                if(rs.next()){
                    docSubject = Util.null2String(rs.getString("docSubject"));
                }
            }else if("2".equals(favtype)){
                rs.executeQuery("select  requestname from workflow_requestbase where requestid="+docId);
                if(rs.next()){
                    docSubject = Util.null2String(rs.getString("requestname"));
                }
            }

            SysFavouriteInfo sf=new SysFavouriteInfo();
            sf.setFavouriteid(favid);
            sf.setPagename(docSubject);
            if("1".equals(favtype)){
                sf.setUrl("/docs/docs/DocDsp.jsp?id="+docId);
            }else if("2".equals(favtype)){
                sf.setUrl("/workflow/request/ViewRequest.jsp?requestid="+docId);
            }
            sf.setImportlevel("1");
            sf.setUserid(user.getUID());
            sf.setType(favtype);
            sf.setFavouriteObjId(docId);
            sf.saveFavouritesFromPage();
        }catch(Exception ex){
            if("1".equals(favtype)){
                result.put("api_status",false);
                result.put("msg", "Cancel Collection Document Exceptions");
            }else if("2".equals(favtype)){
                result.put("api_status",false);
                result.put("msg", "Cancel Collection Workflow Exceptions");
            }
            return result;
        }
        result.put("api_status",true);
        return result;
    }
//    public static Boolean canOpenSingleAttach(int docid){
//        return canOpenSingleAttach(docid,"");
//    }
    //判断当前文档是否开启单附件打开,
    public static Boolean canOpenSingleAttach(int docid,String doccontent){
        //Map<String,Object> apidatas = new HashMap<String, Object>();
    	
//    	boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
//
//
//    	if(!isUsePDFViewer){
//    		return false;
//    	}
    	//BaseBean bb = new BaseBean();
        Boolean result = false;
        RecordSet rs = new RecordSet();
        String backFields = "c.isOpenAttachment,d.doctype";
        String from = " from DocDetail d,DocSecCategory c ";
        String where = " where d.seccategory=c.id and d.id=?";

        String sql = "";
       
        sql = "select " + backFields + from + where;
        rs.executeQuery(sql,docid);
        String isOpenAttachment = "";
       // String doccontent = "";
        int doctype = 0;
        while (rs.next()){
            isOpenAttachment = Util.null2String(rs.getString("isOpenAttachment"));
           // doccontent = Util.null2String(rs.getString("doccontent"));
            doctype = rs.getInt("doctype");
//            apidatas.put("canOpenSinglefile",isOpenAttachment);
        }
        //bb.writeLog(doctype);  //1
        if(doctype != 1 ){ //office文档
        	result = true;
        }else if(isOpenAttachment.equals("1")){
            DocDetailService docDetailService = new DocDetailService();
            Map<String,String> attMap = docDetailService.getMainAccInfo(doccontent,isOpenAttachment,docid);
            String isopenacc = Util.null2s(attMap.get(docDetailService.IS_OPEN_ACC),"0");
            if("1".equals(isopenacc)){
                return true;
            }
        	return false;
        }
        return result;

    }

    public static Map<String, String> getRequestMap(HttpServletRequest request) {

        Map<String, String> dataMap = new HashMap<String, String>();

        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String paramValue = Util.null2String(request.getParameter(paramName));
            dataMap.put(paramName, paramValue);
        }
        return dataMap;
    }

    public static Boolean isPic(String fileid){
        String filename = "";
        String extName = "";
        try {
            String sql = "select * from imagefile where imagefileid = ?";
            RecordSet rs = new RecordSet();
            rs.executeQuery(sql,fileid);
            while (rs.next()){
                 filename = Util.null2String(rs.getString("imagefilename"));
                 extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
            }
            extName = extName.toLowerCase();
            if("jpg".equals(extName) || "jpeg".equals(extName) || "png".equals(extName) || "gif".equals(extName) || "bmp".equals(extName)){
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Map<String,Object> getNetdiskInfo(String fileid){
        Map<String,Object> result = new HashMap<>();
        BaseBean bb = new BaseBean();
        ImageConvertUtil icu = new ImageConvertUtil();
        Map<String,String> onlineParams = new HashMap<>();
        onlineParams.put("checkPic","1");
        DocImageManager docImageManager = new DocImageManager();
        long fSize = docImageManager.getImageFileSize(Util.getIntValue(fileid));
        String filename = "";
        String extName = "";
        String sql = "select * from imagefile where imagefileid = ?";
        RecordSet rs = new RecordSet();
        rs.executeQuery(sql,fileid);
        while (rs.next()){
            filename = Util.null2String(rs.getString("imagefilename"));
            extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
        }
        boolean readOnLine = ImageConvertUtil.readOnlineForMobile(extName,onlineParams);//是否支持在线查看
        boolean tooLarge = icu.isTooLarge(extName,fSize + "");
        if(tooLarge && "pdf".equals(extName.toLowerCase())){
        	tooLarge = false;
        }
        result.put("ispic","0");
        result.put("readOnLine","0");
        result.put("downloadurl","/weaver/weaver.file.FileDownload?fileid="+fileid+"&download=1");
        if(ImageConvertUtil.isPic(extName)){
            result.put("ispic","1");
        }
        if(tooLarge){
            readOnLine = false;
        }
        if(readOnLine){
            result.put("readOnLine","1");
        }
        //# 移动端是否直接下载，不提示弹框 0 不提示，1提示
        result.put("doc_acc_download_noalert",Util.null2s(bb.getPropValue("doc_mobile_detail_prop","doc_acc_download_noalert"),"1"));
        result.put("api_status", true);
        result.put("msg", "success");
        return result;
    }

    private static boolean getReadLine(String extName) {
        boolean readOnLine = false;  //是否支持在线查看

        readOnLine = ImageConvertUtil.canConvertType(extName);


        if("pdf".equals(extName)){
            readOnLine = true;
        }

        return readOnLine;

    }
    //根据 文件id判断当前文件是否是pdf
    public static Boolean isPDF(String fileid){
        String filename = "";
        String extName = "";
        try {
            String sql = "select * from imagefile where imagefileid = ?";
            RecordSet rs = new RecordSet();
            rs.executeQuery(sql,fileid);
            while (rs.next()){
                filename = Util.null2String(rs.getString("imagefilename"));
                extName = filename.contains(".")? filename.substring(filename.lastIndexOf(".") + 1) : "";
            }
            if("pdf".equals(extName)){
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean canEditForMobile(String filetype,User user){
    	filetype = Util.null2String(filetype);
    	//new BaseBean().writeLog("^^^^^^^^^^^^filetype1=" + filetype);
    	if(filetype.contains(".")){
    		filetype = filetype.substring(filetype.lastIndexOf(".")+1);
    	}
    	
    	//new BaseBean().writeLog("^^^^^^^^^^^^filetype2=" + filetype);
    	
    	boolean canEdit = ImageConvertUtil.canEditForWps(filetype,user);
    	
    	if(canEdit){
    		canEdit = "1".equals(new RecordSet().getPropValue("doc_wps_for_weaver","webOfficeNew")) || WebOfficeSdkUtil.isSDK();
    	}
    	
    	//new BaseBean().writeLog("^^^^^^^^^^^^canEdit=" + filetype);
    	
    	return canEdit;
    }
    
    public static Map<String,Object> getDocFileEdit(Map<String, Object> params,User user){
    	Map<String, Object> apidatas = new HashMap<String, Object>();
    	int id = Util.getIntValue(Util.null2String(params.get("id")));
    	int imagefileid = Util.getIntValue(Util.null2String(params.get("imagefileid")));
    	RecordSet rs = new RecordSet();
    	rs.executeQuery("select doctype from DocDetail where id=?",id);
    	int doctype = 0;
    	if(rs.next()){
    		doctype = rs.getInt("doctype");
    	}
    	
    	String imagefilename = "";
    	if(doctype == 2 && imagefileid <= 0){   //编辑正文
    		rs.executeQuery("select imagefileid,imagefilename from DocImageFile where docid=? and (isextfile <> '1' or isextfile is null) and docfiletype <> '1' order by versionId desc",id);
    		if(rs.next()){
    			imagefileid = rs.getInt("imagefileid");
    			imagefilename = Util.null2String(rs.getString("imagefilename"));
    		}
    	}else if(imagefileid > 0){
    		rs.executeQuery("select imagefilename from DocImageFile where docid=? and imagefileid=?",id,imagefileid);
    		if(rs.next()){
    			imagefilename = Util.null2String(rs.getString("imagefilename"));
    		}
    	}
    	
    	if(imagefilename.isEmpty()){
    		apidatas.put("api_status", true);
    		apidatas.put("canEdit",false);
    		return apidatas;
    	}
    	
    	boolean canEdit = false;
    	
    	 DocViewPermission dvp = new DocViewPermission();
         if(dvp.hasRightForSecret(user,id)) {   //密级等级判断
             Map<String, Boolean> levelMap = dvp.getShareLevel(id, user, false);
             canEdit = levelMap.get(DocViewPermission.EDIT);
         }
    	
    	String mFileType = imagefilename.contains(".") ? imagefilename.substring(imagefilename.lastIndexOf(".")).toLowerCase() : "";
    	
    	apidatas.put("api_status", true);
    	apidatas.put("canEdit",canEdit);
    	apidatas.put("id",id);
    	apidatas.put("mFileType",mFileType);
    	apidatas.put("imagefileid",imagefileid);
    	boolean enableOfficeSensitiveWordValidate = false;
        DocSensitiveWordsUtil sensitiveWordsUtil = new DocSensitiveWordsUtil();
        if(sensitiveWordsUtil.enableOfficeSensitiveWordValidate() && sensitiveWordsUtil.canProcessOfficeType(mFileType)) {
            enableOfficeSensitiveWordValidate = true;
        }
    	apidatas.put("enableOfficeSensitiveWordValidate",enableOfficeSensitiveWordValidate);
    	
    	return apidatas;
    }
    
    public static Map<String, Object> getCreateDocType(String currentType,User user){
    	Map<String, Object> apidatas = new HashMap<String, Object>();
    	
    	apidatas.put("editOffice",0);
    	apidatas.put("enableOfficeSensitiveWordValidate",false);
    	//ImageConvertUtil icu = new ImageConvertUtil();
    	boolean canEdit = canEditForMobile("doc",user);//ImageConvertUtil.canEditForWps("doc",user);
    	if(canEdit){
            //==zj
            boolean isOpenWps = false;	//设置开关状态
            try{
                //查看开关状态
                RecordSet wps = new RecordSet();
                wps.executeQuery("select isopen from wps_config where type='wps'");
                if (wps.next()){
                    int isOpen = wps.getInt("isopen");
                    //如果开关是开启状态
                    if (isOpen == 1){
                        int uid = user.getUID();		//用户id
                        String subcompanyName = "";		//分部id
                        String subcompanylist = "";		//分部wps权限数组
                        String hrmsql = "select id from hrmsubcompany where id = (select subcompanyid1 from Hrmresource where id =" +  uid + ")";
                        new BaseBean().writeLog("==zj==(移动新建--根据用户id查询分部id)" + hrmsql);
                        wps.executeQuery(hrmsql);	//查询用户id对应分部id
                        if (wps.next()){
                            subcompanyName = Util.null2String(wps.getString("id"));		//如果有赋值
                            new BaseBean().writeLog("==zj==(移动新建--获取账号所在分部)" + subcompanyName);
                        }
                        //将分部id作为条件在建模表进行查询
                        RecordSet sub = new RecordSet();
                        String Docsql = "select subcompanyname from uf_wps";
                        new BaseBean().writeLog("==zj==(移动新建--查询建模表分部信息)" + Docsql);
                        sub.executeQuery(Docsql);
                        if (sub.next()){
                            subcompanylist = Util.null2String(sub.getString("subcompanyname"));
                            new BaseBean().writeLog("==zj==（移动新建--获取到wps权限分部数组）" + JSON.toJSONString(subcompanylist));
                            String[] result = subcompanylist.split(",");
                            new BaseBean().writeLog("==zj==（移动新建--截取后的数据）" + JSON.toJSONString(result));
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
            if (isOpenWps){
                apidatas.put("editOffice",1);

            }else {
                apidatas.put("editOffice",0);
            }

    		int language = user.getLanguage();
    		
    		 List<RightMenu> rightMenus = new ArrayList<RightMenu>();
    		
    		String newDoc = SystemEnv.getHtmlLabelName(82, user.getLanguage());
    		
    		RightMenu htmlMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_HTML, "", false);
               
    		htmlMenu.setMenuName(newDoc + "HTML" + htmlMenu.getMenuName());
            rightMenus.add(htmlMenu); // HTML文档
    		
            RightMenu wordMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_DOCX, "", false);
            wordMenu.setMenuName(newDoc + "WORD" + wordMenu.getMenuName());
            rightMenus.add(wordMenu);
            
        	RightMenu excelMenu = new RightMenu(language, RightMenuType.BTN_CREATE_DOC_XLSX, "", false);
        	excelMenu.setMenuName(newDoc + "EXCEL" + excelMenu.getMenuName());
        	rightMenus.add(excelMenu);
        	
        	apidatas.put("typelist",rightMenus);
            DocSensitiveWordsUtil sensitiveWordsUtil = new DocSensitiveWordsUtil();
            if(sensitiveWordsUtil.enableOfficeSensitiveWordValidate() && sensitiveWordsUtil.canProcessOfficeType("doc")) {
                apidatas.put("enableOfficeSensitiveWordValidate",true);
            }
    		
    	}
    	apidatas.put("api_status", true);
    	
    	return apidatas;
    }

    public static String systemDocWhereSql(User user,String scope,String alias,String tablefield){
        String whereSql="1=1";
        if(scope.isEmpty()){
            return whereSql;
        }
        String selectFilterTypeSql = "select FilterType from MobileDocNewFileSetting where scope=? and  docappsettingtype=?";
        RecordSet selectFilterTypers = new RecordSet();
        selectFilterTypers.executeQuery(selectFilterTypeSql,scope,DocApplySettingUtil.DOCSYSTEMAPP);
        String filtertype = "";
        while (selectFilterTypers.next()){
            filtertype = Util.null2String(selectFilterTypers.getString("filtertype"));
        }

        if(!"1".equals(filtertype)){
            String secids = "";
            String selectcategorySql = "SELECT id,categoryname name FROM docseccategory WHERE id IN ( SELECT categoryid FROM MobileDocNewFileCategory WHERE scope = ? and docappsettingtype=? )";
            RecordSet selectcategoryrs = new RecordSet();
            selectcategoryrs.executeQuery(selectcategorySql,scope,DocApplySettingUtil.DOCSYSTEMAPP);
            while (selectcategoryrs.next()){
                int seccategory = selectcategoryrs.getInt("id");
                secids = secids+","+seccategory;
            }
           if(!secids.isEmpty()){secids = secids.substring(1);}
           if("2".equals(filtertype)){
                if(secids.contains(",")){
                    whereSql= "("+Util.getSubINClause(secids,(alias.isEmpty()?"":alias+".")+tablefield,"in")+")";
                }else{
                    whereSql=" "+(alias.isEmpty()?"":alias+".")+tablefield+" = "+secids;
                }
           }else if("3".equals(filtertype)){
                if(secids.contains(",")){
                    whereSql= "("+Util.getSubINClause(secids,(alias.isEmpty()?"":alias+".")+tablefield,"not in")+")";
                }else {
                    whereSql=" "+(alias.isEmpty()?"":alias+".")+tablefield+" != "+secids;
                }

           }
        }
        return whereSql;
    }
    public static String systemDocFilter(String scope){
        if(scope.isEmpty()){
            return "";
        }
        String selectFilterTypeSql = "select FilterType from MobileDocNewFileSetting where scope=? and  docappsettingtype=?";
        RecordSet selectFilterTypers = new RecordSet();
        selectFilterTypers.executeQuery(selectFilterTypeSql,scope,DocApplySettingUtil.DOCSYSTEMAPP);
        String filtertype = "";
        while (selectFilterTypers.next()){
            filtertype = Util.null2String(selectFilterTypers.getString("filtertype"));
        }
        if(!"1".equals(filtertype)){
            String secids = "";
            String selectcategorySql = "SELECT id,categoryname name FROM docseccategory WHERE id IN ( SELECT categoryid FROM MobileDocNewFileCategory WHERE scope = ? and docappsettingtype=? )";
            RecordSet selectcategoryrs = new RecordSet();
            selectcategoryrs.executeQuery(selectcategorySql,scope,DocApplySettingUtil.DOCSYSTEMAPP);
            while (selectcategoryrs.next()){
                int seccategory = selectcategoryrs.getInt("id");
                secids = secids+","+seccategory;
            }
            if(!secids.isEmpty()){secids = secids.substring(1);}
            if("2".equals(filtertype)){
                return "2_"+secids;
            }else if("3".equals(filtertype)){
                return "3_"+secids;
            }
        }
        return "";
    }
    public static String takeFileNamenoExt(String filename){
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                filename = filename.substring(0, dot);
            }
            return filename;
        }
        return "";
    }

    /***
     * 判断文档未读状态，含主次账号
     * @param user
     * @return
     */
    public static boolean takeDocReadStatus(User user,String docid){
        Boolean result = false;
        HrmUserSettingComInfo userSetting = new HrmUserSettingComInfo();
        User belongsuser = new User();
        String belongtoshow = userSetting.getBelongtoshowByUserId(user.getUID()+"");
        String belongtoids = belongsuser.getBelongtoidsByUserId(user.getUID()+"");
        String account_type = belongsuser.getAccount_type();
        DocComInfo dc = new DocComInfo();
        String sql = "select count(0) as c from DocDetail t where t.id="+docid+" and t.doccreaterid<>"+user.getUID()+" and not exists (select 1 from docReadTag where userid="+user.getUID()+" and docid=t.id)";
        if(belongtoshow.equals("1")&&account_type.equals("0")&&!belongtoids.equals("")){
            belongtoids+=","+user.getUID();
            dc.setHasbelongcreater(belongtoids);
            sql = "select count(0) as c from DocDetail t where t.id="+docid+" and t.doccreaterid not in ("+belongtoids+" ) and not exists (select 1 from docReadTag where userid in ("+belongtoids+") and docid=t.id)";
        }
       RecordSet rs1 = new RecordSet();
        rs1.execute(sql);
        if(rs1.next()&&rs1.getInt("c")>0) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    public static String takeddcode(User user,String fileid,Map<String,String> params){
        String ddcode = "";
        SM4Utils sm4 = new SM4Utils();
        String secretKey = sm4.createSecretKey();
        int timeout = Util.getIntValue(new BaseBean().getPropValue("doc_writelog_config","overtime"),5);
        String timelimit = "1";
        String adminright = "0";
        RecordSet rs = new RecordSet();
        String sql = "insert into docdesutil_secretkey(desencode,secretkey,createdate,createtime,codetimeout,timelimit) values(?,?,?,?,?,?)";
        if(params!=null){
            timeout = Util.getIntValue(params.get(TIMEOUT),5);
            timelimit = Util.null2s(params.get(TIMELIMIT),"1");
            adminright = Util.null2s(params.get(ADMINRIGHT),"1");
        }
        String begin = TimeUtil.getCurrentTimeString();
        try{
            if("1".equals(adminright)){
                ddcode = 1 + "_" + fileid;
            }else{
                ddcode = user.getUID() + "_" + fileid;
            }
            if("1".equals(timelimit)){
                ddcode = ddcode+"_"+begin+"_"+timeout;
            }
            ddcode = sm4.encrypt(ddcode,secretKey);
            rs.executeUpdate(sql,ddcode,secretKey,TimeUtil.getCurrentDateString(),TimeUtil.getOnlyCurrentTimeString(),timeout,(timelimit.equals("1")?Y:N));
        }catch(Exception e){
        }
        return ddcode;
    }

    /**
     *
     *  因为更改了加密方法，解密时需要注意历史数据
     *  先查密钥表
     *
     *  1.查不到数据再用老的解密方法再解一遍，若也解不出来，认为是超时的加密串，直接提示超时
     *
     *  2.如果密钥表查到了数据 1.先检查是否是设置了超时的密钥，是的话检查是否超时，超时了直接删除表数据并提示删除
     *                                2.若是永久的密钥，则不能删除表数据
     *
     * @param user
     * @param ddcode
     * @return
     */
    public static Map<String,Object> deDdcode(User user,String ddcode){
        Map<String,Object> result = new HashMap<>();
        SM4Utils sm4 = new SM4Utils();
        String userid = "";
        String fileid = "";
        String begin = "";
        String isovertime = "";
        String secretkey = "";
        String secretkeycreatedate = "";
        String secretkeycreatetime = "";
        int secretkeycodetimeout = 0;
        String secretkeytimelimit = "";
        int timeout = 0;
        String decode = "";
        String endtime = TimeUtil.getCurrentTimeString();

        RecordSet rs = new RecordSet();
        String sql = "select desencode,secretkey,createdate,createtime,codetimeout,timelimit from docdesutil_secretkey where desencode=?";
        rs.executeQuery(sql,ddcode);
        if(rs.next()){
            secretkey = Util.null2s(rs.getString("secretkey"),"");
            secretkeycreatedate = Util.null2s(rs.getString("createdate"),"");
            secretkeycreatetime = Util.null2s(rs.getString("createtime"),"");
            secretkeycodetimeout = Util.getIntValue(rs.getString("codetimeout"));
            secretkeytimelimit = Util.null2s(rs.getString("timelimit"),N);
            if(secretkeytimelimit.equals(Y)){//设置了超时
                SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date fromDate3 = null;
                try {
                    fromDate3 = simpleFormat.parse(secretkeycreatedate+" "+secretkeycreatetime);
                    Date toDate3 = simpleFormat.parse(endtime);
                    long from3 = fromDate3.getTime();
                    long to3 = toDate3.getTime();
                    int minutes = (int) ((to3 - from3) / (1000 * 60))+1; //计算出来的时间，一分钟内结果是0，所以要加1
                    result.put("to3",to3);
                    result.put("from3",from3);
                    result.put("minutes",minutes);
                    if(minutes>secretkeycodetimeout){ //超时了，不能下载，同时删除超时数据
                        isovertime = "1";
                        result.put("msg","secretkey timeout");
                        result.put(USERID,userid);
                        result.put(FILEID,fileid);
                        result.put(BEGINTIME,begin);
                        result.put("endtime",endtime);
                        result.put(TIMEOUT,timeout);
                        result.put(ISOVERTIME,"1");
                        rs.writeLog(">>>>>>>> [secretkey timeout] ddcode:"+ddcode);
                        sql = "delete from docdesutil_secretkey where desencode=?";
                        rs.executeUpdate(sql,ddcode);
                        return result;
                    }else{ //未超时，做解密
                        decode = sm4.decrypt(ddcode,secretkey);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else{//未设置超时，直接解密
                decode = sm4.decrypt(ddcode,secretkey);
            }
        }else{ //查不到数据用老的解密方法解密
            try{
                DesUtils des = new DesUtils();
                decode = des.decrypt(ddcode);
            }catch (Exception e){
                //解密失败，认为是超时的加密串被删除了表数据
                result.put(USERID,userid);
                result.put(FILEID,fileid);
                result.put(BEGINTIME,begin);
                result.put("endtime",endtime);
                result.put(TIMEOUT,timeout);
                result.put(ISOVERTIME,"1");
                result.put("msg","desutils encode fail ---catch");
                rs.writeLog(">>>>>>>> [DesUtils 解密方法失败] ddcode:"+ddcode);
                e.printStackTrace();
                return  result;
            }
        }

        try {
            String[] codeArr = decode.split("_");
            int len = codeArr.length;
            if(len<=2){
                userid = Util.null2s(codeArr[0],"");
                fileid = Util.null2s(codeArr[1],"");
            }else{
                userid = Util.null2s(codeArr[0],"");
                fileid = Util.null2s(codeArr[1],"");
                begin = Util.null2s(codeArr[2],"");
                timeout = Util.getIntValue(codeArr[3]);
                SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date fromDate3 = simpleFormat.parse(begin);
                Date toDate3 = simpleFormat.parse(endtime);
                long from3 = fromDate3.getTime();
                long to3 = toDate3.getTime();
                int minutes = (int) ((to3 - from3) / (1000 * 60))+1; //计算出来的时间，一分钟内结果是0，所以要加1
                result.put("to3",to3);
                result.put("from3",from3);
                result.put("minutes",minutes);
                if(minutes>timeout){ //超时了，不能下载
                    isovertime = "1";
                }
            }
            result.put(USERID,userid);
            result.put(FILEID,fileid);
            result.put(BEGINTIME,begin);
            result.put("endtime",endtime);
            result.put(TIMEOUT,timeout);
            result.put(ISOVERTIME,isovertime);
        } catch (Exception e) {
            rs.writeLog(">>>>>>>> [解密失败] ddcode:"+ddcode);
            e.printStackTrace();
        }
        return result;
    }

}
