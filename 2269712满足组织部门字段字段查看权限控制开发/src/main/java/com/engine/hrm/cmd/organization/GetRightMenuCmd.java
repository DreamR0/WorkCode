package com.engine.hrm.cmd.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import com.api.hrm.bean.RightMenu;
import com.api.hrm.bean.RightMenuType;
import com.customization.qc2269712.CheckPermissionUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.HrmOrganizationUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

public class GetRightMenuCmd extends AbstractCommonCommand<Map<String, Object>>{
	
	public GetRightMenuCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public BizLogContext getLogContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		String type = Util.null2String((String)params.get("type"));
		int id = Util.getIntValue((String)params.get("id"));
		String pageid = Util.null2String(params.get("pageid"));//页面标识
		List<Object>  rightMenu = new ArrayList<Object>();
		try {
			if(pageid.equals("company")){//总部信息
				rightMenu = getCompanyMenu(type, id, pageid, user);
			}else if(pageid.equals("subcompany")){//分部信息
				rightMenu = getSubCompanyMenu(type, id, pageid, user);
			}else if(pageid.equals("department")){//部门信息
				rightMenu = getDepartmentMenu(type, id, pageid, user);
			}else if(pageid.equals("subcompanylist")){//分部列表
				rightMenu = getSubCompanyListMenu(type, id, pageid, user);
			}else if(pageid.equals("departmentlist")){//部门信息
				rightMenu = getDepartmentListMenu(type, id, pageid, user);
			}else if(pageid.equals("resourcelist")){//人员信息
				rightMenu = getResourceListMenu(type, id, pageid, user);
			}
		} catch (Exception e) {
			writeLog(e);
		}
		retmap.put("status", "1");
		retmap.put("rightMenu", rightMenu);
		retmap.put("hasDpIcon", true);
		return retmap;
	}
	
	private List<Object> getCompanyMenu(String type, int id, String pageid, User user){
		List<Object>  rightMenu = new ArrayList<Object>();
		
		if(HrmUserVarify.checkUserRight("HrmCompanyEdit:Edit", user)){
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_EDIT, "doEdit", true));
		}
		if(HrmUserVarify.checkUserRight("HrmSubCompanyAdd:Add", user)){
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddSubCompany, "addSubCompany", true));
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddVirtualCompany, "addCompany", true));
		}
		if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)){
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Import_Resource, "importResource", true));
		}
		if(HrmUserVarify.checkUserRight("HrmCompanyEdit:Edit", user)){
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Import_Org, "importOrg", true));
		}
//		if(HrmUserVarify.checkUserRight("FnaTransaction:All",user)){
//			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_FnaExpenseDetail, "fnaExpenseDetail"));
//		}
//		if(HrmUserVarify.checkUserRight("SubBudget:Maint", user)){
//			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_FnaBudgetView, "fnaBudgetView"));
//		}
		if(HrmUserVarify.checkUserRight("HrmCompany:Log", user)){
			Map<String,Object> params = new HashMap<>();
			params.put("logSmallType", BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_COMPANY.getCode());
			params.put("targetId",id);
			rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_log,"showLog",params));
		}
		
		return rightMenu;
	}
	
	private List<Object> getSubCompanyMenu(String type, int id, String pageid, User user){
		List<Object>  rightMenu = new ArrayList<Object>();
		CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
		HrmOrganizationUtil hrmOrganizationUtil = new HrmOrganizationUtil();
		
		int detachable= HrmOrganizationUtil.getDetachable();
		ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
		boolean hrmdetachable = ManageDetachComInfo.isUseHrmManageDetach();
		int sublevel=0;
		int deplevel=0;
		int reslevel=0;
		if (detachable == 1 && hrmdetachable) {
			sublevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmSubCompanyEdit:Edit", id);
			deplevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmDepartmentAdd:Add", id);
			reslevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", id);
		} else {
			if (HrmUserVarify.checkUserRight("HrmSubCompanyEdit:Edit", user)) {
				sublevel = 2;
			}
			if (HrmUserVarify.checkUserRight("HrmDepartmentAdd:Add", user)) {
				deplevel = 2;
			}
			if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
				reslevel = 2;
			}
		}
		
		RecordSet rs = new RecordSet();
		String sql = "";
		boolean canceled = false;
		sql = "select canceled from hrmsubcompany where id=" + id;
		rs.executeSql(sql);
		if(rs.next()){
			canceled = Util.null2String(rs.getString("canceled")).equals("1");
		}
		//将新建部门按钮单独取出
		if(deplevel>0){
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddDepartment, "addDepartment", true));
		}

		if(sublevel>0){
			if(canceled){
				rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_DoIsCanceled, "doISCanceled", true));
			}else{
	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_EDIT, "doEdit", true));

	    	if(hrmOrganizationUtil.canCancelSubCompany(id)){
	    		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Cancel, "doCancel", true));
	    	}
	    	 
	    	if(hrmOrganizationUtil.canDelSubCompany(id)){
	    		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Delete, "doDelete"));
	    	}
	    	
	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddSubCompanySibling, "addSubCompanySibling", true));
	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddSubCompanyChild, "addSubCompanyChild", true));

	    	if(reslevel>0){
	  			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Import_Resource, "importResource", true));
	  		}

//		    if(HrmUserVarify.checkUserRight("FnaTransaction:All",user)){
//					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_FnaExpenseDetail, "fnaExpenseDetail"));
//				}
//				if(HrmUserVarify.checkUserRight("SubBudget:Maint", user)){
//					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_FnaBudgetView, "fnaBudgetView"));
//				}
				if(HrmUserVarify.checkUserRight("HrmSubCompany:Log", user)){
					Map<String,Object> params = new HashMap<>();
					params.put("logSmallType", BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_SUBCOMPANY.getCode());
					params.put("targetId",id);
					rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_log,"showLog",params));
				}
			}
		}
		
		return rightMenu;
	}
	
	private List<Object> getDepartmentMenu(String type, int id, String pageid, User user){
		List<Object>  rightMenu = new ArrayList<Object>();
		CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
		HrmOrganizationUtil hrmOrganizationUtil = new HrmOrganizationUtil();
		
		RecordSet rs = new RecordSet();
		String sql = "";
		boolean canceled = false;
		int subcompanyid = 0;
		
		sql = "select * from hrmdepartment where id=" + id;
		rs.executeSql(sql);
		if(rs.next()){
			canceled = Util.null2String(rs.getString("canceled")).equals("1");
			subcompanyid = rs.getInt("subcompanyid1");
		}



		int detachable= HrmOrganizationUtil.getDetachable();
		ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
		boolean hrmdetachable = ManageDetachComInfo.isUseHrmManageDetach();
		int deplevel=0;
		int reslevel=0;
		new BaseBean().writeLog("==zj==(detachable和hrmdetachable)"  + detachable + " | " + hrmdetachable);
		if (detachable == 1 && hrmdetachable) {
			deplevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmDepartmentAdd:Add", subcompanyid);
			reslevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmResourceEdit:Edit", subcompanyid);
		} else {

			if (HrmUserVarify.checkUserRight("HrmDepartmentAdd:Add", user)) {
				deplevel = 2;
			}
			if (HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
				reslevel = 2;
			}
		}
		//==zj 如果该部门有填写spa区域就显示编辑按钮
		CheckPermissionUtil checkPermissionUtil = new CheckPermissionUtil();
		if (checkPermissionUtil.checkDepartment(user,id)){
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_EDIT, "doEdit", true));		//部门信息修改
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_DepartmentRoles, "departmentRoles"));			//部门人员
		}
		if(deplevel>0){
			if(canceled){
				rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_DoIsCanceled, "doISCanceled", true));
			}else{
	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_EDIT, "doEdit", true));

	    	if(hrmOrganizationUtil.canCancelDepartment(id)){
	    		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Cancel, "doCancel", true));
	    	}
	    	 
	    	if(hrmOrganizationUtil.canDelDepartment(id)){
	    		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Delete, "doDelete"));
	    	}


	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddDepartmentSibling, "addDepartmentSibling", true));
	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddDepartmentChild, "addDepartmentChild", true));

	    	if(reslevel>0){
	  			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_Import_Resource, "importResource", true));
	  			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_DepartmentRoles, "departmentRoles"));
	  		}
	    	
//		    if(HrmUserVarify.checkUserRight("FnaTransaction:All",user)){
//					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_FnaExpenseDetail, "fnaExpenseDetail"));
//				}
//				if(HrmUserVarify.checkUserRight("SubBudget:Maint", user)){
//					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_FnaBudgetView, "fnaBudgetView"));
//				}
				if(HrmUserVarify.checkUserRight("HrmDepartment:Log", user)){
					Map<String,Object> params = new HashMap<>();
					params.put("logSmallType", BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_DPEARTMENT.getCode());
					params.put("targetId",id);
					rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_log,"showLog",params));
				}
			}
		}
		return rightMenu;
	}
	
	private List<Object> getSubCompanyListMenu(String type, int id, String pageid, User user){
		List<Object>  rightMenu = new ArrayList<Object>();
		CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
		
		RecordSet rs = new RecordSet();
		String sql = "";
		boolean canceled = false;
		sql = "select canceled from hrmsubcompany where id=" + id;
		rs.executeSql(sql);
		if(rs.next()){
			canceled = Util.null2String(rs.getString("canceled")).equals("1");
		}
		
		int detachable= HrmOrganizationUtil.getDetachable();
		ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
		boolean hrmdetachable = ManageDetachComInfo.isUseHrmManageDetach();
		int sublevel=0;
		if(detachable==1 && hrmdetachable){
	    sublevel=CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"HrmSubCompanyAdd:Add",id);
	    sublevel=CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"HrmSubCompanyEdit:Edit",id);
		} else {
			if (HrmUserVarify.checkUserRight("HrmSubCompanyAdd:Add", user)) {
				sublevel = 2;
			}
			if (HrmUserVarify.checkUserRight("HrmSubCompanyEdit:Edit", user)) {
				sublevel = 2;
			}
		}

		if(type.equals("company")){
			canceled = false;
			if (HrmUserVarify.checkUserRight("HrmSubCompanyAdd:Add", user)) {
				sublevel = 2;
			}
			if (HrmUserVarify.checkUserRight("HrmSubCompanyEdit:Edit", user)) {
				sublevel = 2;
			}
		}

		if(sublevel>0 ){
			if(canceled){
				//rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_DoIsCanceled, "doISCanceled", true));
			}else{
				if(type.equals("company")){
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddSubCompany, "addSubCompany", true));
				}else{
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddSubCompany, "addSubCompanyChild", true));
				}
	    	
				if(sublevel>1){
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchCancel, "batchCancel", true, true));
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchDoIsCanceled, "batchDoIsCanceled", true, true));
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchDelete, "batchDel", true, true));
					Map<String,Object> batchEditParams = new HashMap<>();
					batchEditParams.put("type", "subCompany");
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchEdit, "batchEdit", false, batchEditParams));
				}
			}
		}
		
		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_COLUMN, "definedColumn", false));
		Map<String,Object> params = new HashMap<>();
		params.put("logSmallType", BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_SUBCOMPANY.getCode());
		rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_log,"showLog",params));
    rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_Export_Excel,"export",true));
//		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_STORE, "", false));
//		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_HELP, "", false));
		return rightMenu;
	}
	
	
	private List<Object> getDepartmentListMenu(String type, int id, String pageid, User user){
		List<Object>  rightMenu = new ArrayList<Object>();
		CheckSubCompanyRight CheckSubCompanyRight = new CheckSubCompanyRight();
		HrmOrganizationUtil hrmOrganizationUtil = new HrmOrganizationUtil();
		
		RecordSet rs = new RecordSet();
		String sql = "";
		boolean canceled = false;
		sql = "select canceled from hrmdepartment where id=" + id;
	
		if(type.equals("subcompany")){
			sql = "select canceled from hrmsubcompany where id=" + id;
		}
		rs.executeSql(sql);
		if(rs.next()){
			canceled = Util.null2String(rs.getString("canceled")).equals("1");
		}
		
		int detachable= HrmOrganizationUtil.getDetachable();
		ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
		boolean hrmdetachable = ManageDetachComInfo.isUseHrmManageDetach();
		int sublevel=0;
		if (detachable == 1 && hrmdetachable) {
			if(type.equals("subcompany")) {
				sublevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmDepartmentEdit:Edit", id);
			}else{
				int subcompanyid = Util.getIntValue(new DepartmentComInfo().getSubcompanyid1(""+id));
				sublevel = CheckSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(), "HrmDepartmentEdit:Edit",subcompanyid);
			}

		} else {
			if (HrmUserVarify.checkUserRight("HrmDepartmentEdit:Edit", user)){
				sublevel = 2;
			}
		}
		
		if(sublevel>0 ){
			if(canceled){
				//rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_DoIsCanceled, "doISCanceled", true));
			}else{
				if(type.equals("subcompany")){
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddDepartment, "addDepartment", true));
				}else{
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddDepartment, "addDepartmentChild", true));
				}
	    	
				if(sublevel>1){
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchCancel, "batchCancel", true, true));
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchDoIsCanceled, "batchDoIsCanceled", true, true));
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchDelete, "batchDel", true, true));
					Map<String,Object> batchEditParams = new HashMap<>();
					batchEditParams.put("type", "department");
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchEdit, "batchEdit", false, batchEditParams));
				}
			}
		}
		
		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_COLUMN, "definedColumn", false));
		Map<String,Object> params = new HashMap<>();
		params.put("logSmallType", BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_DPEARTMENT.getCode());
		rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_log,"showLog",params));
		rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_Export_Excel,"export",true));
//		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_STORE, "", false));
//		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_HELP, "", false));
		return rightMenu;
	}
	
	private List<Object> getResourceListMenu(String type, int id, String pageid, User user){
		List<Object>  rightMenu = new ArrayList<Object>();
		
		RecordSet rs = new RecordSet();
		String sql = "";
		boolean canceled = false;
		sql = "select canceled from hrmdepartment where id=" + id;
		rs.executeSql(sql);
		if(rs.next()){
			canceled = Util.null2String(rs.getString("canceled")).equals("1");
		}
		
		boolean hasHrmDeartmentVirtual = false;
		rs.executeSql(" select count(*) from hrmdepartmentvirtual");
		if(rs.next()){
			if(rs.getInt(1)>0)hasHrmDeartmentVirtual = true;
		}
		
		if(!canceled){
			if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user) ){
	    	rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddResource, "addResource", true));
				if(hasHrmDeartmentVirtual){ 
					rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddResourceVirtual, "addResourceVirtual", true, true));
				}
			}
			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_AddResourceToGroup, "addResourceToGroup", true, true));
			if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)){
				Map<String,Object> batchEditParams = new HashMap<>();
				batchEditParams.put("type", "resource");
				rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchEdit, "batchEdit", false, batchEditParams));
			}
		}
		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_COLUMN, "definedColumn", false));
		if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)){
			Map<String,Object> params = new HashMap<>();
			params.put("logSmallType", BizLogSmallType4Hrm.HRM_RSOURCE_CARD.getCode());
			rightMenu.add(new RightMenu(user.getLanguage(),RightMenuType.BTN_log,"showLog",params));

			rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_BatchResetPwd, "batchResetPwd", true,true));
		}
//		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_STORE, "", false));
//		rightMenu.add(new RightMenu(user.getLanguage(), RightMenuType.BTN_HELP, "", false));
		return rightMenu;
	}
}
