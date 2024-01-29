package com.engine.hrm.cmd.organization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.definedfield.HrmDeptFieldManagerE9;
import weaver.hrm.definedfield.HrmFieldComInfo;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.SystemEnv;

import com.api.browser.bean.SearchConditionItem;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.HrmOrganizationUtil;
import weaver.systeminfo.systemright.CheckSubCompanyRight;

public class GetDepartmentFormFieldViewCmd extends AbstractCommonCommand<Map<String, Object>> {

	public GetDepartmentFormFieldViewCmd(Map<String, Object> params, User user) {
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
			int viewattr = 1;
			
			HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
			HrmFieldComInfo HrmFieldComInfo = new HrmFieldComInfo();
			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			SearchConditionItem searchConditionItem = null;
			HrmFieldBean hrmFieldBean = null;
			HrmDeptFieldManagerE9 hfm = new HrmDeptFieldManagerE9(5);
			
			hfm.getCustomData(Util.getIntValue(id));
			List lsGroup = hfm.getLsGroup();
			for (int tmp = 0; lsGroup != null && tmp < lsGroup.size(); tmp++) {
				String groupid = (String) lsGroup.get(tmp);
				List lsField = hfm.getLsField(groupid);
				if (lsField.size() == 0)
					continue;
				if (hfm.getGroupCount(lsField) == 0)
					continue;
		  	if(!Util.null2String(HrmFieldGroupComInfo.getIsShow(groupid)).equals("1"))continue;
				String grouplabel = HrmFieldGroupComInfo.getLabel(groupid);
				itemlist = new ArrayList<Object>();
				groupitem = new HashMap<String, Object>();
				groupitem.put("title", SystemEnv.getHtmlLabelNames(grouplabel, user.getLanguage()));
				groupitem.put("defaultshow", true);
				for (int j = 0; lsField != null && j < lsField.size(); j++) {
					String fieldid = (String) lsField.get(j);
					String isuse = HrmFieldComInfo.getIsused(fieldid);
					if (!isuse.equals("1"))continue;
					String fieldname = HrmFieldComInfo.getFieldname(fieldid);
					String fieldlabel = HrmFieldComInfo.getLabel(fieldid);
					String fieldhtmltype = HrmFieldComInfo.getFieldhtmltype(fieldid);
					String type = HrmFieldComInfo.getFieldType(fieldid);
					String dmlurl = Util.null2String(HrmFieldComInfo.getFieldDmlurl(fieldid));
					String fieldValue = "";
					if(HrmFieldComInfo.getIssystem(fieldid).equals("1")){
						fieldValue = hfm.getData(fieldname);
					}else{
						fieldValue = hfm.getData("hrmdepartmentdefined",fieldname);
					}
					
					if(id.length()>0 && fieldname.equals("showid")){
						fieldValue = id;
					}
					
					hrmFieldBean = new HrmFieldBean();
					hrmFieldBean.setFieldid(fieldid);
					hrmFieldBean.setFieldname(fieldname);
					hrmFieldBean.setFieldlabel(fieldlabel);
					hrmFieldBean.setFieldhtmltype(fieldhtmltype);
					hrmFieldBean.setType(type);
					hrmFieldBean.setIsFormField(true);
					hrmFieldBean.setFieldvalue(fieldValue);
					hrmFieldBean.setDmlurl(dmlurl);
					hrmFieldBean.setIssystem("1");
					searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
					if (searchConditionItem.getBrowserConditionParam() != null) {
						searchConditionItem.getBrowserConditionParam().setViewAttr(viewattr);
					}
					if(fieldname.equals("showorder")){
						searchConditionItem.setPrecision(2);
					}

					if("6".equals(fieldhtmltype)){//附件
						Map<String, Object> otherParams1 = new HashMap<String, Object>();
						otherParams1.put("showOrder", false);
						searchConditionItem.setOtherParams(otherParams1);
					}

					searchConditionItem.setViewAttr(viewattr);
					itemlist.add(searchConditionItem);
				}

				int operatelevel = -1;
				ManageDetachComInfo manageDetachComInfo = new ManageDetachComInfo();
				boolean hrmdetachable = manageDetachComInfo.isUseHrmManageDetach();
				if (hrmdetachable) {
					CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
					DepartmentComInfo departmentComInfo = new DepartmentComInfo();
					operatelevel=checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"HrmDepartmentEdit:Edit",Integer.parseInt(departmentComInfo.getSubcompanyid1(id)));
				} else {
					operatelevel = 2;
				}
				
				if(tmp==0 && HrmUserVarify.checkUserRight("HrmDepartmentEdit:Edit", user) && operatelevel > 0){
					int resourceNum = 0;
					RecordSet rs = new RecordSet();
					
					String sql = "SELECT COUNT(*) FROM hrmresource WHERE departmentid = "+ id +" and ( status =0 or status = 1 or status = 2 or status = 3)";
					rs.executeSql(sql);
					if(rs.next()){
						resourceNum = rs.getInt(1);
					}
					
					//4:解聘 5:离职 6:退休 7:无效
					int resourceNum1 = 0;
					 sql = "SELECT COUNT(*) FROM hrmresource WHERE departmentid = "+ id +" and status in(4,5,6,7)";
					rs.executeSql(sql);
					if(rs.next()){
						resourceNum1 = rs.getInt(1);
					}
					hrmFieldBean = new HrmFieldBean();
					hrmFieldBean.setFieldname("deptResourceInfo");
					hrmFieldBean.setFieldlabel("382428");
					hrmFieldBean.setFieldhtmltype("1");
					hrmFieldBean.setType("1");
					hrmFieldBean.setIsFormField(true);
					hrmFieldBean.setFieldvalue(resourceNum+"/"+resourceNum1);
					hrmFieldBean.setDmlurl("");
					searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
					searchConditionItem.setViewAttr(viewattr);
					itemlist.add(searchConditionItem);
				} 
				groupitem.put("items", itemlist);
				grouplist.add(groupitem);
			}
			RecordSet rs = new RecordSet();
			String canceled = "";
			String sql = "select canceled from hrmdepartment where id=" + id;
			rs.executeSql(sql);
			if(rs.next()){
				canceled = Util.null2String(rs.getString("canceled"));
			}
			retmap.put("status", "1");
			retmap.put("id", id);
			retmap.put("titleInfo", HrmOrganizationUtil.getTitleInfo(id, "department", user));
			retmap.put("formField", grouplist);
			retmap.put("canceled", canceled.equals("1"));
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
