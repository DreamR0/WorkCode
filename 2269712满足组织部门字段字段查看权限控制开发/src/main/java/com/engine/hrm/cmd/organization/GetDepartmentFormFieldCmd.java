package com.engine.hrm.cmd.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.customization.qc2269712.CheckPermissionUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.definedfield.HrmDeptFieldManager;
import weaver.hrm.definedfield.HrmDeptFieldManagerE9;
import weaver.hrm.definedfield.HrmFieldComInfo;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.systeminfo.SystemEnv;

import com.api.browser.bean.SearchConditionItem;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.HrmOrganizationUtil;

public class GetDepartmentFormFieldCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetDepartmentFormFieldCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {

		Map<String, Object> retmap = new HashMap<String, Object>();
		List<Map<String, Object>> grouplist = new ArrayList<Map<String, Object>>();
		Map<String, Object> groupitem = null;
		List<Object> itemlist = null;
		try {
			String id = Util.null2String(params.get("id"));
			int viewattr = Util.getIntValue((String)params.get("viewattr"),1);
			String nodeType = Util.null2String(params.get("type"));
			String addType = Util.null2String(params.get("addType"));
			DepartmentComInfo departmentComInfo = new DepartmentComInfo();
			String subcompanyid1 = "";
			String supdepid = "";
			
			if(addType.equals("normal")){
				if(nodeType.equals("subcompany")){
					subcompanyid1 = id;
				}else{
					subcompanyid1 = departmentComInfo.getSubcompanyid1(id);
				}
			}else if(addType.equals("sibling")){
				subcompanyid1 = departmentComInfo.getSubcompanyid1(id);
				supdepid = departmentComInfo.getDepartmentsupdepid(id);
			}else if(addType.equals("child")){
				subcompanyid1 = departmentComInfo.getSubcompanyid1(id);
				supdepid = id;
			}
			
			HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
			HrmFieldComInfo HrmFieldComInfo = new HrmFieldComInfo();
			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			SearchConditionItem searchConditionItem = null;
			HrmFieldBean hrmFieldBean = null;
			HrmDeptFieldManagerE9 hfm = new HrmDeptFieldManagerE9(5);
			hfm.isReturnDecryptData(true);
			hfm.getCustomData(Util.getIntValue(id));
			List lsGroup = hfm.getLsGroup();
			for (int tmp = 0; lsGroup != null && tmp < lsGroup.size(); tmp++) {
				String groupid = (String) lsGroup.get(tmp);
				List lsField = hfm.getLsField(groupid);
				//if (lsField.size() == 0) continue;
				//if (hfm.getGroupCount(lsField) == 0) continue;
		  	//if(!Util.null2String(HrmFieldGroupComInfo.getIsShow(groupid)).equals("1"))continue;

				boolean groupHide = lsField.size() == 0 || hfm.getGroupCount(lsField) == 0 || !Util.null2String(HrmFieldGroupComInfo.getIsShow(groupid)).equals("1");
				String grouplabel = HrmFieldGroupComInfo.getLabel(groupid);
				itemlist = new ArrayList<Object>();
				groupitem = new HashMap<String, Object>();
				groupitem.put("title", SystemEnv.getHtmlLabelNames(grouplabel, user.getLanguage()));
				groupitem.put("hide", groupHide);
				groupitem.put("defaultshow", true);
				for (int j = 0; lsField != null && j < lsField.size(); j++) {
					String fieldid = (String) lsField.get(j);
					String fieldname = HrmFieldComInfo.getFieldname(fieldid);
					String isuse = HrmFieldComInfo.getIsused(fieldid);
					if (!isuse.equals("1"))continue;
					int tmpViewattr = viewattr;
					String rules = "";
					String fieldlabel = HrmFieldComInfo.getLabel(fieldid);
					String fieldhtmltype = HrmFieldComInfo.getFieldhtmltype(fieldid);
					String type = HrmFieldComInfo.getFieldType(fieldid);
					String dmlurl = Util.null2String(HrmFieldComInfo.getFieldDmlurl(fieldid));
					String fieldValue = "";
					if(addType.length()>0) {
					}else {
						if (HrmFieldComInfo.getIssystem(fieldid).equals("1")) {
							fieldValue = hfm.getData(fieldname);
						} else {
							fieldValue = hfm.getData("hrmdepartmentdefined", fieldname);
						}
					}

					if(!groupHide && tmpViewattr==2&&HrmFieldComInfo.getIsmand(fieldid).equals("1")){
						tmpViewattr=3;
						if("1".equals(fieldhtmltype) && "2".equals(type)){
							rules = "required|integer";
						}else{
							rules = "required|string";
						}
					}
					
					if(subcompanyid1.length()>0 && fieldname.equals("subcompanyid1")){
						fieldValue = subcompanyid1;
					}

					if(supdepid.length()>0 && fieldname.equals("supdepid")){
						fieldValue = supdepid;
					}
					
					if(fieldname.equals("showid")){
						if(addType.length()>0){
							continue;
						}else{
							fieldValue = id;
							tmpViewattr = 1;
						}
					}
					
					hrmFieldBean = new HrmFieldBean();
					hrmFieldBean.setFieldid(fieldid);
					hrmFieldBean.setFieldname(fieldname);
					hrmFieldBean.setFieldlabel(fieldlabel);
					hrmFieldBean.setFieldhtmltype(fieldhtmltype);
					hrmFieldBean.setType(type);
					hrmFieldBean.setIsFormField(true);
					hrmFieldBean.setIssystem("1");
					hrmFieldBean.setFieldvalue(fieldValue);
					hrmFieldBean.setDmlurl(dmlurl);
					hrmFieldBean.setViewAttr(tmpViewattr);
					hrmFieldBean.setRules(rules);
					if(hrmFieldBean.getFieldname().equals("subcompanyid1")||hrmFieldBean.getFieldname().equals("supdepid")){
						hrmFieldBean.setHideVirtualOrg(true);
					}
					if(hrmFieldBean.getFieldname().equals("departmentcode")){
						hrmFieldBean.setMultilang(false);
					}
					searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
					if(searchConditionItem!=null){
						searchConditionItem.setLabelcol(8);
						searchConditionItem.setFieldcol(16);
					}
					if(fieldname.equals("showorder")){
						searchConditionItem.setPrecision(2);
					}
					if(fieldname.equals("showid")){
						Map<String, Object> otherParams = new HashMap<String, Object>();
						otherParams.put("hasBorder", true);
						searchConditionItem.setOtherParams(otherParams);
					}
					if("6".equals(fieldhtmltype)){//附件
						Map<String, Object> otherParams1 = new HashMap<String, Object>();
						otherParams1.put("showOrder", false);
						searchConditionItem.setOtherParams(otherParams1);
					}
					if (new BaseBean().getPropValue("qc2269712","sapdefined").equals(fieldname)){
						CheckPermissionUtil cpu = new CheckPermissionUtil();
						//==zj	sqp区域只有总部角色和系统管理员才能修改
						if (user.isAdmin() || cpu.checkHead(user)){
							searchConditionItem.setViewAttr(2);
						}else {
							searchConditionItem.setViewAttr(1);
						}
					}


					itemlist.add(searchConditionItem);
				}
				groupitem.put("items", itemlist);
				grouplist.add(groupitem);
			}
			retmap.put("status", "1");
			retmap.put("id", id);
			retmap.put("titleInfo", HrmOrganizationUtil.getTitleInfo(id, "department", user));
			retmap.put("formField", grouplist);
		} catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return retmap;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}
}
