package com.api.hrm.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.SearchConditionItem;
import com.api.customization.qc20230816.permission.util.CheckSelectUtil;
import com.api.customization.qc20230816.permission.util.CheckSetUtil;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.*;
import com.cloudstore.dev.api.util.TextUtil;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.hrm.biz.HrmFieldManager;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.docs.docs.CustomFieldManager;
import weaver.encrypt.EncryptUtil;
import weaver.file.FileUpload;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.AllManagers;
import weaver.hrm.resource.CustomFieldTreeManager;
import weaver.hrm.resource.HrmListValidate;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.settings.HrmSettingsComInfo;
import weaver.systeminfo.SysMaintenanceLog;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

/***
 * 人员卡片
 * @author lvyi
 *
 */
public class HrmResourcePersonalService extends BaseBean {
  private static final char separator = Util.getSeparator() ;	    
  private String today = DateUtil.getCurrentDate();

	//qc20230816 自定义修改权限
	private String isOpen = new BaseBean().getPropValue("qc20230816permission","isopen");

  /**
	 * 查看人员个人信息
	 * @param request
	 * @param response
	 * @return
	 */
	public String getResourcePesonalView(HttpServletRequest request, HttpServletResponse response){
		User user = HrmUserVarify.getUser(request, response);
		HrmSettingsComInfo  hrmSettingsComInfo = new HrmSettingsComInfo();
		HrmResourceBaseService hrmResourceBaseService = new HrmResourceBaseService();
		HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType",1);
		//这里看看能不能获取到字段
		CheckSetUtil checkSetUtil = new CheckSetUtil();
		Boolean setButtonPerson = checkSetUtil.isSetButtonPerson(request, response, user);
		new BaseBean().writeLog("==zj==(setButtonPerson)" + JSON.toJSONString(setButtonPerson));
		if (user.getUID() == 1){
			setButtonPerson = true;
		}

		String checkIsEdit = hrmSettingsComInfo.getCheckIsEdit();
		new BaseBean().writeLog("checkIsEdit==="+checkIsEdit);
		Map<String,Object> retmap = new HashMap<String,Object>();
		Map<String,Object> result = new HashMap<String,Object>();
		try{
			String id = Util.null2String(request.getParameter("id"));
			if(id.length()==0){
				id = ""+user.getUID();
			}
			boolean isSelf = false;
			if (id.equals("" + user.getUID())) {
				isSelf = true;
			}
			if (id.equals("")) id = String.valueOf(user.getUID());
			String status = "";
			String subcompanyid = "", departmentId = "";
			RecordSet rs = new RecordSet();
			HttpSession session = request.getSession(true);
			rs.executeSql("select subcompanyid1, status, departmentId from hrmresource where id = " + id);
			if (rs.next()) {
				status = Util.toScreen(rs.getString("status"), user.getLanguage());
				subcompanyid = Util.toScreen(rs.getString("subcompanyid1"), user.getLanguage());
				departmentId = StringUtil.vString(rs.getString("departmentId"));
				if (subcompanyid == null || subcompanyid.equals("") || subcompanyid.equalsIgnoreCase("null"))
					subcompanyid = "-1";
				
			}

			int operatelevel = -1;
			//人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
			int hrmdetachable = 0;
			ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
			boolean isUseHrmManageDetach = ManageDetachComInfo.isUseHrmManageDetach();
			if (isUseHrmManageDetach) {
				hrmdetachable = 1;
			} else {
				hrmdetachable = 0;
			}
			if (hrmdetachable == 1) {
				CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
				operatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", Util.getIntValue(subcompanyid));
			} else {
				if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentId)) {
					operatelevel = 2;
				}
			}
			Map<String, Object> buttons = new Hashtable<String, Object>();

			//qc20230816 显示对应编辑按钮
			if (((isSelf&&hrmResourceBaseService.isHasModify(1)) || operatelevel > 0) && !status.equals("10") && setButtonPerson) {
				buttons.put("hasEdit", true);
				buttons.put("hasSave", true);
			}

			HrmListValidate HrmListValidate = new HrmListValidate();
			AllManagers AllManagers = new AllManagers();
			boolean isManager = false;
			AllManagers.getAll(id);
			if (id.equals("" + user.getUID())) {
				isSelf = true;
			}
			while (AllManagers.next()) {
				String tempmanagerid = AllManagers.getManagerID();
				if (tempmanagerid.equals("" + user.getUID())) {
					isManager = true;
				}
			}
			//qc20230816 这里放开查看权限

			if ((isSelf || operatelevel >= 0 /*|| "1".equals(isOpen)*/) && HrmListValidate.isValidate(11)) {
				result.put("buttons", buttons);
				Map<String,Object> tmp = getFormFields(request, response, false);
				result.put("conditions", tmp.get("conditions"));
				result.put("tables", tmp.get("tables"));
				result.put("id", id);
			}else{
				result.put("hasRight", false);
			}
//
//    	if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit",user)||(isSelf&&hrmResourceBaseService.isHasModify(1))) {
//    		buttons.put("hasEdit", true);
//  			buttons.put("hasSave", true);
//    	}

		}catch (Exception e) {
			writeLog(e);
		}
		
		retmap.put("result", result);
		return JSONObject.toJSONString(retmap);
	}
	
	/**
	 * 人员个人信息表单字段
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String,Object> getFormFields(HttpServletRequest request, HttpServletResponse response, boolean isAdd){
		User user = HrmUserVarify.getUser(request, response);
		Map<String,Object> result = new HashMap<String,Object>();
		List<Object> lsGroup = new ArrayList<Object>();
		Map<String,Object> groupitem = null;
		List<Object> itemlist = null;
		try{
			String id = Util.null2String(request.getParameter("id"));
			int viewAttr = Util.getIntValue(request.getParameter("viewAttr"),1);
			if(isAdd)viewAttr=2;
			if(id.length()==0){
				id = ""+user.getUID();
			}
			boolean isSelf = false;
			if (id.equals("" + user.getUID())) {
				isSelf = true;
			}
			String subcompanyid = "", departmentId = "";
			RecordSet recordSet = new RecordSet();
			//不能直接判断是否有人力资源维护权限，也需要根据分权进行判断
			boolean canEdit = false;
			recordSet.executeSql("select subcompanyid1, status, departmentId from hrmresource where id = " + id);
			if (recordSet.next()) {
				subcompanyid = Util.toScreen(recordSet.getString("subcompanyid1"), user.getLanguage());
				departmentId = StringUtil.vString(recordSet.getString("departmentId"));
				if (subcompanyid == null || subcompanyid.equals("") || subcompanyid.equalsIgnoreCase("null"))
					subcompanyid = "-1";
			}
			int operatelevel = -1;
			//人力资源模块是否开启了管理分权，如不是，则不显示框架，直接转向到列表页面(新的分权管理)
			int hrmdetachable = 0;
			ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
			boolean isUseHrmManageDetach = ManageDetachComInfo.isUseHrmManageDetach();
			if (isUseHrmManageDetach) {
				hrmdetachable = 1;
			} else {
				hrmdetachable = 0;
			}
			if (hrmdetachable == 1) {
				CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
				operatelevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", Util.getIntValue(subcompanyid));
			} else {
				if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user, departmentId)) {
					operatelevel = 2;
				}
			}
			if(operatelevel>0){
				canEdit = true;
			}
			int scopeId = 1;
			HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType",scopeId);
			CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType",scopeId);
			if(viewAttr!=1)hfm.isReturnDecryptData(true);
			hfm.getHrmData(Util.getIntValue(id));
			cfm.getCustomData(Util.getIntValue(id));
			HrmListValidate hrmListValidate = new HrmListValidate();
			CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
			CheckSetUtil checkSetUtil = new CheckSetUtil();
			while(HrmFieldGroupComInfo.next()){
				int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
				if(grouptype!=scopeId)continue;
				int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
				int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
				hfm.getCustomFields(groupid);

				groupitem = new HashMap<String,Object>();
				itemlist = new ArrayList<Object>();
				groupitem.put("title", SystemEnv.getHtmlLabelName(grouplabel, user.getLanguage()));
				groupitem.put("defaultshow", true);
				if(groupid==4){
					groupitem.put("hide", (!isAdd&&!canEdit&&isSelf&&viewAttr==2&&hfm.getContactEditCount()==0)||!hrmListValidate.isValidate(42));
				}else{
					groupitem.put("hide", (!isAdd&&!canEdit&&isSelf&&viewAttr==2&&hfm.getContactEditCount()==0)||!Util.null2String(HrmFieldGroupComInfo.getIsShow()).equals("1"));
				}
				groupitem.put("items", itemlist);
				lsGroup.add(groupitem);
				while(hfm.next()){
					int tmpviewattr = viewAttr;
					String fieldName=hfm.getFieldname();
					if (tmpviewattr == 1){
						//qc20230816 查看个人信息过滤
						int selectUserId = Integer.parseInt(id);
						if (user.getUID()!=1 && "1".equals(isOpen) && !checkSelectUtil.fieldShowCheck(user.getUID(),fieldName,1,selectUserId))continue;
					}


					String cusFieldname = "";
					String fieldValue="";
					if(hfm.isBaseField(fieldName)){
						fieldValue = hfm.getHrmData(fieldName);
					}else{
						fieldValue = Util.null2String(new EncryptUtil().decrypt("cus_fielddata","field" + hfm.getFieldid(),"HrmCustomFieldByInfoType",""+scopeId,cfm.getData("field" + hfm.getFieldid()),viewAttr==2,true));
						cusFieldname = "customfield"+hfm.getFieldid();
						if(isAdd) cusFieldname = "customfield_1_"+hfm.getFieldid();
					}
					
					if(!hfm.isUse()||(!isAdd&&viewAttr==2&&!canEdit&&isSelf&&!hfm.isModify())){
						HrmFieldBean hrmFieldBean = new HrmFieldBean();
						hrmFieldBean.setFieldname(cusFieldname.length() > 0 ? cusFieldname : fieldName);
						hrmFieldBean.setFieldhtmltype("1");
						hrmFieldBean.setType("1");
						if(!isAdd){
							hrmFieldBean.setFieldvalue(fieldValue);
						}
						hrmFieldBean.setIsFormField(true);
						SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
						Map<String, Object> otherParams = new HashMap<String, Object>();
						otherParams.put("hide", true);
						searchConditionItem.setOtherParams(otherParams);
						itemlist.add(searchConditionItem);
						continue;
					}
					 org.json.JSONObject hrmFieldConf = hfm.getHrmFieldConf(fieldName);
						HrmFieldBean hrmFieldBean = new HrmFieldBean();
						hrmFieldBean.setFieldid((String)hrmFieldConf.get("id"));
						hrmFieldBean.setFieldname(cusFieldname.length()>0?cusFieldname:fieldName);
						hrmFieldBean.setFieldlabel(hfm.getLable());
						hrmFieldBean.setFieldhtmltype((String)hrmFieldConf.get("fieldhtmltype"));
						hrmFieldBean.setType((String)hrmFieldConf.get("type"));
						hrmFieldBean.setDmlurl((String)hrmFieldConf.get("dmlurl"));
						hrmFieldBean.setIssystem(""+(Integer)hrmFieldConf.get("issystem"));
						if(!isAdd){
							hrmFieldBean.setFieldvalue(fieldValue);
						}
						hrmFieldBean.setIsFormField(true);
						if(viewAttr==2 && ((String)hrmFieldConf.get("ismand")).equals("1")){
							tmpviewattr=3;
							if(hrmFieldBean.getFieldhtmltype().equals("3")){
								hrmFieldBean.setRules("required|string");
//								if (hrmFieldBean.getType().equals("2")||hrmFieldBean.getType().equals("161")||hrmFieldBean.getType().equals("162")) {
//									hrmFieldBean.setRules("required|string");
//								}else{
//									hrmFieldBean.setRules("required|integer");
//								}
							}else if(hrmFieldBean.getFieldhtmltype().equals("4")||
									hrmFieldBean.getFieldhtmltype().equals("5")){
								hrmFieldBean.setRules("required|integer");
							} else if (hrmFieldBean.getFieldhtmltype().equals("1") && hrmFieldBean.getType().equals("2")) {
								hrmFieldBean.setRules("required|integer");
							}else{
								hrmFieldBean.setRules("required|string");
							}
						}
						
						SearchConditionItem searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
						if(searchConditionItem==null)continue;
						if(searchConditionItem.getBrowserConditionParam()!=null){
							searchConditionItem.getBrowserConditionParam().setViewAttr(tmpviewattr);
						}
						searchConditionItem.setViewAttr(tmpviewattr);
					if (tmpviewattr == 2){
						//qc20230816 编辑个人信息过滤
						if (user.getUID()!=1 && "1".equals(isOpen)){
							int setUserId = Integer.parseInt(id);
							checkSetUtil.searchConditionItem(user.getUID(),fieldName,1,searchConditionItem,setUserId);
						}
					}
						itemlist.add(searchConditionItem);
					}
					if(itemlist.size()==0)lsGroup.remove(groupitem);
			}
			result.put("conditions", lsGroup);
			
			//明细信息
			List<Object> lsTable = new ArrayList<Object>();
			List<HrmFieldBean> titles = null;
			Map<String,Object> table = null;
			Map<String,Object> maptab = null;
			HrmFieldBean hrmFieldBean = null;
			List<Map<String,Object>> columns = null;
			List<Map<String,Object>> datas = null;
			Map<String,Object> data = null;
			result.put("tables",lsTable);

			//标头信息--家庭信息
			HrmFieldDetailComInfo HrmFieldDetailComInfo = new HrmFieldDetailComInfo();
			LinkedHashMap<String, List<HrmFieldBean>>  detialTable = HrmFieldDetailComInfo.getDetialTable(""+scopeId, viewAttr, "80%");
			Iterator<Map.Entry<String, List<HrmFieldBean>>> entries = detialTable.entrySet().iterator();
			while(entries.hasNext()){
				Map.Entry<String, List<HrmFieldBean>> entry = entries.next();
				String tablename = entry.getKey();
				titles = entry.getValue();
				table = new HashMap<String,Object>();
				columns = HrmFieldUtil.getHrmDetailTable(titles, null, user);
				table.put("columns", columns);

				datas = new ArrayList<Map<String,Object>>();
				RecordSet rs = new  RecordSet();
				String sql = "select * from "+tablename+" where resourceid = "+id;
				rs.executeSql(sql);
				while(rs.next()){
					data = new HashMap<String,Object>();
					for(HrmFieldBean fieldInfo :titles){
						if (!isAdd){
							data.put(fieldInfo.getFieldname(), TextUtil.toBase64ForMultilang(Util.null2String(rs.getString(fieldInfo.getFieldname()))));
						}
					}
					datas.add(data);
				}
				table.put("datas", datas);
				table.put("rownum","rownum");
				maptab = new Hashtable<String, Object>();
				String tablabel = HrmResourceDetailTab.HrmResourceDetailTabInfo.get(tablename.toUpperCase());
				maptab.put("tabname", SystemEnv.getHtmlLabelNames(tablabel,user.getLanguage()));
				maptab.put("hide", (!hrmListValidate.isValidate(45)||(!isAdd&&!canEdit&&isSelf&&viewAttr==2)));
				maptab.put("tabinfo", table);
				lsTable.add(maptab);
			}

			//自定义信息
			RecordSet RecordSet = new RecordSet();
			CustomFieldTreeManager CustomFieldTreeManager = new CustomFieldTreeManager();
			LinkedHashMap<String,String> ht = new LinkedHashMap<String,String>();
			RecordSet.executeSql("select id, formlabel,viewtype from cus_treeform where parentid="+scopeId+" order by scopeorder");
			while(RecordSet.next()){
				if(RecordSet.getInt("viewtype")!=1)continue;
				titles = new ArrayList<HrmFieldBean>();
				int subId = RecordSet.getInt("id");
				CustomFieldManager cfm2 = new CustomFieldManager("HrmCustomFieldByInfoType",subId);
				cfm2.getCustomFields();
				CustomFieldTreeManager.getMutiCustomData("HrmCustomFieldByInfoType", subId, Util.getIntValue(id,0));
				int colcount1 = 0 ;
				int rowcount = 0;
				int col = 0;
				while(cfm2.next()){
					rowcount++;
					if(!cfm2.isUse()||(!isAdd&&viewAttr==2&&!canEdit&&isSelf&&!cfm2.isModify()))continue;
					col++;
				}
				if(rowcount==0)continue;
				cfm2.beforeFirst();
				ht.put("cus_list_" + subId, RecordSet.getString("formlabel"));
				cfm2.beforeFirst();
				while (cfm2.next()) {
					if(!cfm2.isUse()||(!isAdd&&viewAttr==2&&!canEdit&&isSelf&&!cfm2.isModify()))continue;
					int tmpviewattr = viewAttr;
					//创建表头
					String fieldname = "customfield" + cfm2.getId() + "_" + subId;
					if (isAdd) fieldname = "customfield_1_" + cfm2.getId() + "_" + subId;
					hrmFieldBean = new HrmFieldBean();
					hrmFieldBean.setFieldid("" + cfm2.getId());
					hrmFieldBean.setFieldname(fieldname);
					hrmFieldBean.setFieldlabel(cfm2.getLable());
					hrmFieldBean.setFieldhtmltype(cfm2.getHtmlType());
					hrmFieldBean.setType("" + cfm2.getType());
					hrmFieldBean.setDmlurl(cfm2.getDmrUrl());
					if (viewAttr == 2 && cfm2.isMand()) {
						tmpviewattr = 3;
						hrmFieldBean.setRules("required|string");
					}
					hrmFieldBean.setViewAttr(tmpviewattr);
					hrmFieldBean.setWidth("80%");
					titles.add(hrmFieldBean);
				}
				table = new HashMap<String, Object>();
				if(col>0){
					columns = HrmFieldUtil.getHrmDetailTable(titles, null, user);
					table.put("columns", columns);
				}
				datas = new ArrayList<Map<String, Object>>();
				cfm2.beforeFirst();
				while (CustomFieldTreeManager.nextMutiData()) {
					data = new HashMap<String, Object>();
					while (cfm2.next()) {
//							if(!cfm2.isUse()||(!isAdd&&viewAttr==2&&!canEdit&&isSelf&&!cfm2.isModify()))continue;
						int fieldid = cfm2.getId();  //字段id
						int type = cfm2.getType();
						String dmlurl = cfm2.getDmrUrl();
						int fieldhtmltype = Util.getIntValue(cfm2.getHtmlType());

						String fieldname = "customfield" + cfm2.getId() + "_" + subId;
						if (isAdd) fieldname = "customfield_1_" + cfm2.getId() + "_" + subId;
						String fieldvalue = "";
						if (!isAdd){
							fieldvalue = Util.null2String(CustomFieldTreeManager.getMutiData("field" + fieldid));
						}
						data.put(fieldname, fieldvalue);
						if (cfm2.getHtmlType().equals("1") && cfm2.getType() == 1) {
							data.put(fieldname, TextUtil.toBase64ForMultilang(Util.null2String(fieldvalue)));
						} else if (cfm2.getHtmlType().equals("3")) {
							String fieldshowname = hfm.getFieldvalue(user, dmlurl, fieldid, fieldhtmltype, type, fieldvalue, 0);
							data.put(fieldname, fieldvalue);
							data.put(fieldname + "span", fieldshowname);
						} else if (cfm2.getHtmlType().equals("4")) {
							data.put(fieldname, fieldvalue.equals("1"));
						} else if(cfm2.getHtmlType().equals("6")){
							List<Object> filedatas = new ArrayList<Object>();
							if(Util.null2String(fieldvalue).length()>0) {
								Map<String, Object> filedata = null;
								String[] tmpIds = Util.splitString(Util.null2String(fieldvalue), ",");
								for (int i = 0; i < tmpIds.length; i++) {
									String fileid = tmpIds[i];
									ImageFileManager manager = new ImageFileManager();
									manager.getImageFileInfoById(Util.getIntValue(fileid));
									String filename = manager.getImageFileName();
									String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
									filedata = new HashMap<String, Object>();
									filedata.put("acclink", "/weaver/weaver.file.FileDownload?fileid=" + fileid);
									filedata.put("fileExtendName", extname);
									filedata.put("fileid", fileid);
									filedata.put("filelink", "/spa/document/index2file.jsp?imagefileId=" + fileid + "#/main/document/fileView");
									filedata.put("filename", filename);
									filedata.put("filesize", manager.getImgsize());
									filedata.put("imgSrc", "");
									filedata.put("isImg", "");
									filedata.put("loadlink", "/weaver/weaver.file.FileDownload?fileid=" + fileid + "&download=1");
									filedata.put("showDelete", viewAttr==2);
									filedata.put("showLoad", "true");
									filedatas.add(filedata);
								}
							}
							data.put(fieldname, filedatas);
						}
						//只允许有权限的人删除明细行,没有权限的人只能修改不能删除
						if (canEdit) {
							data.put("viewAttr", 2);
						} else {
							data.put("viewAttr", 1);
						}
					}
					cfm2.beforeFirst();
					datas.add(data);
				}
				table.put("datas", datas);
				table.put("rownum", "nodesnum_" + subId);
				maptab = new Hashtable<String, Object>();
				RecordSet rs = new RecordSet();
				rs.executeSql("select id, formlabel from cus_treeform where parentid=" + scopeId + " and id=" + subId + " order by scopeorder");
				if (rs.next()) {
					maptab.put("tabname", rs.getString("id"));
					maptab.put("tabname", rs.getString("formlabel"));
				}
				maptab.put("hide", col == 0);
				maptab.put("tabinfo", table);
				lsTable.add(maptab);
			}
		}catch (Exception e) {
			writeLog(e);
		}
		return result;
	}
	
	/***
	 * 新建人员个人信息
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String,String> addResourcePersonal(String id, HttpServletRequest request, HttpServletResponse response){
		Map<String,String> retmap = new HashMap<String,String>();
		try{
			User user = HrmUserVarify.getUser(request, response);
			RecordSet rs = new RecordSet();
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			CustomFieldTreeManager CustomFieldTreeManager =  new CustomFieldTreeManager();

			String para  = "";
	    FileUpload fu = new FileUpload(request);
			//String id = Util.null2String(fu.getParameter("id"));
      String birthday = Util.fromScreen3(fu.getParameter("birthday"),user.getLanguage());
      String folk = Util.fromScreen3(fu.getParameter("folk"),user.getLanguage()) ;	 /*民族*/
      String nativeplace = Util.fromScreen3(fu.getParameter("nativeplace"),user.getLanguage()) ;	/*籍贯*/
      String regresidentplace = Util.fromScreen3(fu.getParameter("regresidentplace"),user.getLanguage()) ;	/*户口所在地*/
      String maritalstatus = Util.fromScreen3(fu.getParameter("maritalstatus"),user.getLanguage());
      String policy = Util.fromScreen3(fu.getParameter("policy"),user.getLanguage()) ; /*政治面貌*/
      String bememberdate = Util.fromScreen3(fu.getParameter("bememberdate"),user.getLanguage()) ;	/*入团日期*/
      String bepartydate = Util.fromScreen3(fu.getParameter("bepartydate"),user.getLanguage()) ;	/*入党日期*/
      String islabourunion = Util.fromScreen3(fu.getParameter("islabouunion"),user.getLanguage()) ;
      String educationlevel = Util.fromScreen3(fu.getParameter("educationlevel"),user.getLanguage()) ;/*学历*/
      String degree = Util.fromScreen3(fu.getParameter("degree"),user.getLanguage()) ; /*学位*/
      String healthinfo = Util.fromScreen3(fu.getParameter("healthinfo"),user.getLanguage()) ;/*健康状况*/
      String height = Util.null2o(fu.getParameter("height")) ;/*身高*/
      String weight = Util.null2o(fu.getParameter("weight")) ;
      String residentplace = Util.fromScreen3(fu.getParameter("residentplace"),user.getLanguage()) ;	/*现居住地*/
      String homeaddress = Util.fromScreen3(fu.getParameter("homeaddress"),user.getLanguage()) ;
      String tempresidentnumber = Util.fromScreen3(fu.getParameter("tempresidentnumber"),user.getLanguage()) ;
      String certificatenum = Util.fromScreen3(fu.getParameter("certificatenum"),user.getLanguage()) ;/*证件号码*/
      certificatenum=certificatenum.trim();
      String tempcertificatenum=certificatenum;

			SimpleBizLogger logger = new SimpleBizLogger();
			Map<String, Object> params = ParamUtil.request2Map(request);
			BizLogContext bizLogContext = new BizLogContext();
			bizLogContext.setLogType(BizLogType.HRM);//模块类型
			bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
			bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(15687, user.getLanguage()));
			bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_PERSONAL);//当前小类型
			bizLogContext.setOperateType(BizLogOperateType.ADD);
			bizLogContext.setParams(params);//当前request请求参数
			logger.setUser(user);//当前操作人
			String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType",1,"b");
			String mainSql = "select a.*"+(cusFieldNames.length()>0?","+cusFieldNames:"")+" from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=1 where a.id="+id;
			logger.setMainSql(mainSql,"id");//主表sql
			logger.setMainPrimarykey("id");//主日志表唯一key
			logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
			logger.before(bizLogContext);//写入操作前日志

      int msg=0;
      if(!certificatenum.equals("")){
			rs.executeSql("select accounttype,certificatenum from HrmResource where id="+id);
			String accountType = "", tempCertificatenum = "";
			if(rs.next()){
				accountType = Util.null2String(rs.getString("accounttype"));
				tempCertificatenum = Util.null2String(rs.getString("certificatenum"));
			}
			if(!accountType.equals("1")) {
				rs.executeSql("select id from HrmResource where id<>"+id+" and certificatenum='"+certificatenum+"' and accounttype != '1'");
				if(rs.next()){
					msg=1;
					tempcertificatenum = tempCertificatenum;
				}
			}			
        }
      para = ""+id+	separator+birthday+separator+folk+separator+nativeplace+separator+regresidentplace+separator+
             maritalstatus+	separator+policy+separator+bememberdate+separator+bepartydate+separator+islabourunion+
             separator+educationlevel+separator+degree+separator+healthinfo+separator+height+separator+weight+
             separator+residentplace+separator+homeaddress+separator+tempresidentnumber+separator+tempcertificatenum;
	    int userid = user.getUID();
	    String userpara = ""+userid+separator+today;
      rs.executeProc("HrmResourcePersonalInfo_Insert",para);
      rs.executeProc("HrmResource_ModInfo",""+id+separator+userpara);

      int rownum = Util.getIntValue(fu.getParameter("rownum"),user.getLanguage()) ;
	  String prefix = "" ;
      if(Boolean.TRUE.equals(HrmResourceAddService.saveStatusThreadLocal.get())){
      	prefix="person_" ;
	  }
	  
      for(int i = 0;i<rownum;i++){
        String member = Util.fromScreen3(fu.getParameter("member_"+i),user.getLanguage());
        String title = Util.fromScreen3(fu.getParameter("title_"+i),user.getLanguage());
        String company = Util.fromScreen3(fu.getParameter(prefix+"company_"+i),user.getLanguage());
        String jobtitle = Util.fromScreen3(fu.getParameter(prefix+"jobtitle_"+i),user.getLanguage());
        String address = Util.fromScreen3(fu.getParameter("address_"+i),user.getLanguage());
        String info = member+title+company+jobtitle+address;
        if(!(info.trim().equals(""))){
        para = ""+id+separator+member+separator+title+separator+company+separator+jobtitle+separator+address;
        rs.executeProc("HrmFamilyInfo_Insert",para);
        }
      }

			//处理自定义字段 add by wjy
			CustomFieldTreeManager.editCustomDataE9Add("HrmCustomFieldByInfoType", 1, fu, Util.getIntValue(id,0));
			CustomFieldTreeManager.editMutiCustomDataeE9Add("HrmCustomFieldByInfoType", 1, fu, Util.getIntValue(id,0));

			rs.execute("update HrmResource set "+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID())+" where id="+id) ;
			rs.execute("update HrmResourceManager set "+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID())+" where id="+id) ;

			LogUtil.writeBizLog(logger.getBizLogContexts());
			if(msg==1){
      	retmap.put("status", "-1");
      }else{
      	retmap.put("status", "1");
      }
		}catch (Exception e) {
			writeLog("新建人员个人信息错误："+e);
			retmap.put("status", "-1");
		}
		return retmap;
	}
	
	
  /**
	 * 编辑人员个人信息
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String,Object> editResourcePersonal(HttpServletRequest request, HttpServletResponse response){
		Map<String,Object> retmap = new HashMap<String,Object>();
		HrmSettingsComInfo  hrmSettingsComInfo = new HrmSettingsComInfo();

		try{
			User user = HrmUserVarify.getUser(request, response);
			RecordSet rs = new RecordSet();
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			CustomFieldTreeManager CustomFieldTreeManager =  new CustomFieldTreeManager();
			SysMaintenanceLog SysMaintenanceLog = new SysMaintenanceLog();
			
			String para  = "";
	    	FileUpload fu = new FileUpload(request);
      		String id = Util.null2String(fu.getParameter("id"));
      		//qc20230816 放开修改个人信息权限
            if (!HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user) && !"1".equals(isOpen)) {
                if (!id.equals(user.getUID() + "")) {
                    retmap.put("status", "-1");
                    retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
                    return retmap;
                }
            }
      String birthday = Util.fromScreen3(fu.getParameter("birthday"),user.getLanguage());
      String folk = Util.fromScreen3(fu.getParameter("folk"),user.getLanguage()) ;	 /*民族*/
      String nativeplace = Util.fromScreen3(fu.getParameter("nativeplace"),user.getLanguage()) ;	/*籍贯*/
      String regresidentplace = Util.fromScreen3(fu.getParameter("regresidentplace"),user.getLanguage()) ;	/*户口所在地*/
      String maritalstatus = Util.fromScreen3(fu.getParameter("maritalstatus"),user.getLanguage());
      String policy = Util.fromScreen3(fu.getParameter("policy"),user.getLanguage()) ; /*政治面貌*/
      String bememberdate = Util.fromScreen3(fu.getParameter("bememberdate"),user.getLanguage()) ;	/*入团日期*/
      String bepartydate = Util.fromScreen3(fu.getParameter("bepartydate"),user.getLanguage()) ;	/*入党日期*/
      String islabourunion = Util.fromScreen3(fu.getParameter("islabouunion"),user.getLanguage()) ;
      String educationlevel = Util.fromScreen3(fu.getParameter("educationlevel"),user.getLanguage()) ;/*学历*/
      String degree = Util.fromScreen3(fu.getParameter("degree"),user.getLanguage()) ; /*学位*/
      String healthinfo = Util.fromScreen3(fu.getParameter("healthinfo"),user.getLanguage()) ;/*健康状况*/
      String height = Util.null2o(fu.getParameter("height")) ;/*身高*/
      String weight = Util.null2o(fu.getParameter("weight")) ;
      String residentplace = Util.fromScreen3(fu.getParameter("residentplace"),user.getLanguage()) ;	/*现居住地*/
      String homeaddress = Util.fromScreen3(fu.getParameter("homeaddress"),user.getLanguage()) ;
      String tempresidentnumber = Util.fromScreen3(fu.getParameter("tempresidentnumber"),user.getLanguage()) ;
      String certificatenum = Util.fromScreen3(fu.getParameter("certificatenum"),user.getLanguage()) ;/*证件号码*/
      certificatenum=certificatenum.trim();
      String tempcertificatenum=certificatenum;

			SimpleBizLogger logger = new SimpleBizLogger();
			Map<String, Object> params = ParamUtil.request2Map(request);
			BizLogContext bizLogContext = new BizLogContext();
			bizLogContext.setLogType(BizLogType.HRM);//模块类型
			bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
			bizLogContext.setBelongTypeTargetName(SystemEnv.getHtmlLabelName(15687, user.getLanguage()));
			bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_PERSONAL);//当前小类型
			bizLogContext.setParams(params);//当前request请求参数
			logger.setUser(user);//当前操作人
			String cusFieldNames = ServiceUtil.getCusFieldNames("HrmCustomFieldByInfoType",1,"b");
			String mainSql = "select a.*"+(cusFieldNames.length()>0?","+cusFieldNames:"")+" from hrmresource a left join cus_fielddata b on a.id=b.id and b.scope='HrmCustomFieldByInfoType' and b.scopeid=1 where a.id="+id;
			logger.setMainSql(mainSql,"id");//主表sql
			logger.setMainPrimarykey("id");//主日志表唯一key
			logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
			logger.before(bizLogContext);//写入操作前日志

      int msg=0;
      if(!certificatenum.equals("")&&isNeedCheck(user,id,"certificatenum")){
				rs.executeSql("select accounttype,certificatenum from HrmResource where id="+id);
				String accountType = "", tempCertificatenum = "";
				if(rs.next()){
					accountType = Util.null2String(rs.getString("accounttype"));
					tempCertificatenum = Util.null2String(rs.getString("certificatenum"));
				}
				if(!accountType.equals("1")) {
					rs.executeSql("select id from HrmResource where id<>"+id+" and certificatenum='"+certificatenum+"' and (accounttype != '1' or accounttype is null)");
					if(rs.next()){
						msg=1;
						tempcertificatenum = tempCertificatenum;
						retmap.put("status", "-1");
						retmap.put("message", ""+ SystemEnv.getHtmlLabelName(83521,weaver.general.ThreadVarLanguage.getLang())+"");
						return retmap;
					}
				}			
      }
      para = ""+id+	separator+birthday+separator+folk+separator+nativeplace+separator+regresidentplace+separator+
           maritalstatus+	separator+policy+separator+bememberdate+separator+bepartydate+separator+islabourunion+
           separator+educationlevel+separator+degree+separator+healthinfo+separator+height+separator+weight+
           separator+residentplace+separator+homeaddress+separator+tempresidentnumber+separator+tempcertificatenum;

      rs.executeProc("HrmResourcePersonalInfo_Insert",para);
	    int userid = user.getUID();
	    String userpara = ""+userid+separator+today;
      rs.executeProc("HrmResource_ModInfo",""+id+separator+userpara);

			int rownum = Util.getIntValue(fu.getParameter("rownum"),user.getLanguage()) ;
			rs.executeProc("HrmFamilyInfo_Delete",""+id);
			for(int i = 0;i<rownum;i++){
				String member = Util.fromScreen3(fu.getParameter("member_"+i),user.getLanguage());
				String title = Util.fromScreen3(fu.getParameter("title_"+i),user.getLanguage());
				String company = Util.fromScreen3(fu.getParameter("company_"+i),user.getLanguage());
				String jobtitle = Util.fromScreen3(fu.getParameter("jobtitle_"+i),user.getLanguage());
				String address = Util.fromScreen3(fu.getParameter("address_"+i),user.getLanguage());
				String sonBirthday = Util.fromScreen3(fu.getParameter("birthday_"+i),user.getLanguage());
				String WhetherChildren = Util.fromScreen3(fu.getParameter("WhetherChildren_"+i),user.getLanguage());
				String info = member+title+company+jobtitle+address+sonBirthday+WhetherChildren;
				if(!info.trim().equals("")){
					para = "INSERT INTO HrmFamilyInfo ( resourceid, member, title, company, jobtitle, address,birthday,WhetherChildren) VALUES ( ?,?, ?,?, ?, ?,?,?  ) ";
					rs.executeUpdate(para,id,member,title,company,jobtitle,address,sonBirthday,WhetherChildren);
				}
			}

			ResourceComInfo.updateResourceInfoCache(id);
      //处理自定义字段 add by wjy
      CustomFieldTreeManager.editCustomData("HrmCustomFieldByInfoType", 1, fu, Util.getIntValue(id,0));
			CustomFieldTreeManager.setIsE9(true);
			CustomFieldTreeManager.editMutiCustomData("HrmCustomFieldByInfoType", 1, fu, Util.getIntValue(id,0));

			// 个人信息不需要清理人力资源缓存 ResourceComInfo.removeResourceCache();

			rs.execute("update HrmResource set "+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID())+" where id="+id) ;
			rs.execute("update HrmResourceManager set "+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID())+" where id="+id) ;

			HrmFaceCheckManager.sync(id,HrmFaceCheckManager.getOptUpdate(),
					"hrm_e9_HrmResourcePersonalService_editResourcePersonal",HrmFaceCheckManager.getOaResource());

			LogUtil.writeBizLog(logger.getBizLogContexts());

    	retmap.put("status", "1");
		}catch (Exception e) {
			writeLog("编辑人员个人信息错误："+e);
			retmap.put("status", "-1");
		}
		return retmap;
	}

	public boolean isNeedCheck(User user,String id,String fieldName){
		RecordSet rs = new RecordSet();
		boolean flag = true;
		try {
            if (!HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                if (id.equals(user.getUID() + "")) {
                    rs.executeQuery("select isModify from hrm_formfield where fieldName = ?", fieldName);
                    if (rs.next()) {
                        int isModify = rs.getInt("isModify");
                        if (isModify == 0) flag = false;
                    }
                }
            }
		}catch (Exception e){
			writeLog(e);
		}
		return flag;
	}
}
