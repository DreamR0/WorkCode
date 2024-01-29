package com.engine.hrm.cmd.organization;

import com.api.hrm.util.ServiceUtil;
import com.customization.qc2269712.CheckPermissionUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateAuditType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.entity.RuleCodeType;
import com.engine.hrm.util.CodeRuleManager;
import com.engine.hrm.util.HrmOrganizationUtil;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.Util;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.definedfield.HrmDeptFieldManagerE9;
import weaver.hrm.definedfield.HrmFieldComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.interfaces.email.CoreMailAPI;
import weaver.interfaces.hrm.HrmServiceManager;
import weaver.matrix.MatrixUtil;
import weaver.rtx.OrganisationCom;
import weaver.rtx.OrganisationComRunnable;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;

public class EditDepartmentCmd extends AbstractCommonCommand<Map<String, Object>> {

	private SimpleBizLogger logger;

	public EditDepartmentCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;

		this.logger = new SimpleBizLogger();
		String id = Util.null2String(params.get("id"));
		BizLogContext bizLogContext = new BizLogContext();
		bizLogContext.setLogType(BizLogType.HRM_ENGINE);//模块类型
		bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION);
		bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_DPEARTMENT);//当前小类型
		bizLogContext.setOperateAuditType(BizLogOperateAuditType.WARNING);//警告
		bizLogContext.setParams(params);//当前request请求参数
		logger.setUser(user);//当前操作人
		String mainSql = "select a.* from HrmDepartment a left join HrmDepartmentdefined b on a.id=b.deptid where a.id in(" + id + ") ";
		String tableColumns = new ServiceUtil().getTableColumns("select * from HrmDepartmentdefined", "b", "id");
		if (!tableColumns.equals("")) {
			mainSql = "select a.*," + tableColumns + " from HrmDepartment a left join HrmDepartmentdefined b on a.id=b.deptid where a.id in(" + id + ")";
		}
		logger.setMainSql(mainSql,"id");//主表sql
		logger.setMainPrimarykey("id");//主日志表唯一key
		logger.setMainTargetNameColumn("departmentname");//当前targetName对应的列（对应日志中的对象名）
		logger.before(bizLogContext);
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			String sql = "";
			RecordSet rs = null;
			RecordSet rs1 = null;

			//==zj 如果sap区域有设置值，多个判断
			CheckPermissionUtil cpu = new CheckPermissionUtil();
			int departmentId = Integer.parseInt(Util.null2String(params.get("id")));
			if (!HrmUserVarify.checkUserRight("HrmDepartmentEdit:Edit", user) && !cpu.checkDepartment(user,departmentId)) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
				return retmap;
			}
			
			DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			//OrganisationCom OrganisationCom = new OrganisationCom();
			HrmServiceManager HrmServiceManager = new HrmServiceManager();

			String departmentmark = Util.fromScreen((String) params.get("departmentmark"), user.getLanguage());
			String departmentname = Util.fromScreen((String) params.get("departmentname"), user.getLanguage());
			String subcompanyid1 = Util.fromScreen((String) params.get("subcompanyid1"), user.getLanguage());
			int supdepid = Util.getIntValue((String) params.get("supdepid"), 0);
			String showorder = Util.fromScreen(Util.null2s((String) params.get("showorder"), "0"), user.getLanguage());

			if (Util.getIntValue(subcompanyid1, 0) <= 0) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(382658,user.getLanguage()));
				return retmap;
			}

			int supdepartmentid = supdepid;
			if (supdepid > 0) {
				if (HrmOrganizationUtil.ifDeptLevelEquals10(supdepid)) {
					retmap.put("status", "-1");
					retmap.put("message", SystemEnv.getHtmlLabelName(382650,user.getLanguage()));
					return retmap;
				}
			}

			String allsupdepid = "0";
			String departmentcode = Util.fromScreen((String) params.get("departmentcode"), user.getLanguage());

			// 如果编码为空 自动生成编码
//			departmentcode = CodeRuleManager.getCodeRuleManager().generateRuleCode(RuleCodeType.DEPARTMENT, subcompanyid1, departmentcode);
			if (StringUtils.isNotEmpty(departmentcode)) {
				CodeRuleManager.getCodeRuleManager().checkReservedIfDel(RuleCodeType.DEPARTMENT.getValue(), departmentcode);
			}

			int coadjutant = Util.getIntValue((String) params.get("coadjutant"), 0);

			int id = Util.getIntValue((String) params.get("id"), 0);
			HrmFieldComInfo hrmFieldComInfo = new HrmFieldComInfo();
			sql = "select * from HrmDepartment where id=" + id;
			rs = new RecordSet();
			rs.executeSql(sql);
			while (rs.next()) {
				if (!hrmFieldComInfo.getIsused("73").equals("1"))
					departmentmark = rs.getString("departmentmark");
				if (!hrmFieldComInfo.getIsused("74").equals("1"))
					departmentname = rs.getString("departmentname");
				if (!hrmFieldComInfo.getIsused("75").equals("1"))
					subcompanyid1 = rs.getString("subcompanyid1");
				if (!hrmFieldComInfo.getIsused("76").equals("1"))
					supdepid = rs.getInt("supdepid");
				if (!hrmFieldComInfo.getIsused("77").equals("1"))
					coadjutant = rs.getInt("coadjutant");
				if (!hrmFieldComInfo.getIsused("78").equals("1"))
					showorder = rs.getString("showorder");
				if (!hrmFieldComInfo.getIsused("80").equals("1"))
					departmentcode = rs.getString("departmentcode");
			}
			if(supdepid > 0){
        supdepartmentid = supdepid;
      }

			if (!"".equals(departmentcode)) {
				sql = "select id from hrmdepartment where departmentcode = '" + departmentcode + "' and  id !=" + id;
				rs = new RecordSet();
				rs.executeSql(sql);
				if (rs.next()) {
					retmap.put("status", "-1");
					retmap.put("message", SystemEnv.getHtmlLabelName(382653,user.getLanguage()));
					return retmap;
				}
			}

			char separator = Util.getSeparator();
			if (supdepartmentid > 0) {//当选择了上级部门的时候，需要更改当前部门的上级分部
				subcompanyid1 = DepartmentComInfo.getSubcompanyid1(supdepartmentid + "");
			}
			String para = "" + id + separator + departmentmark + separator + departmentname + separator + supdepartmentid + separator + allsupdepid + separator + subcompanyid1 + separator + showorder + separator + coadjutant;
			rs = new RecordSet();
			rs.executeProc("HrmDepartment_Update", para);

      int flag = rs.getFlag();
      if (flag == 2) {
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(382655,user.getLanguage()));
        return retmap;
      }
      if (flag == 3) {
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(382657,user.getLanguage()));
        return retmap;
      }

			weaver.hrm.company.OrgOperationUtil OrgOperationUtil = new weaver.hrm.company.OrgOperationUtil();
			OrgOperationUtil.updateDepartmentLevel("" + id, "0");

			HrmDeptFieldManagerE9 hrmDeptFieldManager = new HrmDeptFieldManagerE9(5);
			hrmDeptFieldManager.editCustomData(params,id);

			sql = "update hrmdepartment set departmentcode = '" + departmentcode + "' ";
			sql += ","+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID()) ;
			sql += " where id = " + id;
			rs = new RecordSet();
			rs.executeSql(sql);


			ArrayList departmentlist = new ArrayList();
			departmentlist = DepartmentComInfo.getAllChildDeptByDepId(departmentlist, id + "");
			departmentlist.add(id + "");

			for (int i = 0; i < departmentlist.size(); i++) {
				String listdepartmenttemp = (String) departmentlist.get(i);
				rs = new RecordSet();
				rs.execute("update HrmDepartment set subcompanyid1=" + subcompanyid1 + " where id=" + listdepartmenttemp);

				//TD16048改为逐条修改
				rs = new RecordSet();
				rs.execute("select id, subcompanyid1,managerid,seclevel,managerstr from hrmresource where departmentid=" + listdepartmenttemp);
				while (rs.next()) {
					int resourceid_tmp = Util.getIntValue(rs.getString(1), 0);
					int oldsubcompanyid1 = Util.getIntValue(rs.getString(2), 0);
					int oldmanagerid = Util.getIntValue(rs.getString(3), 0);
					int seclevel = Util.getIntValue(rs.getString(4), 0);
					String oldmanagerstr = Util.null2String(rs.getString(5));
					rs1 = new RecordSet();
					rs1.execute("update hrmresource set subcompanyid1=" + subcompanyid1 + " where id=" + resourceid_tmp);
					para = "" + resourceid_tmp + separator + listdepartmenttemp + separator + subcompanyid1 + separator + oldmanagerid + separator + seclevel + separator + oldmanagerstr + separator + listdepartmenttemp + separator + oldsubcompanyid1 + separator + oldmanagerid + separator + seclevel + separator + oldmanagerstr + separator + "1";
					rs1 = new RecordSet();
					rs1.executeProc("HrmResourceShare", para);
				}
				//add by wjy
				//同步RTX端部门信息
				//OrganisationCom.editDepartment(Util.getIntValue(listdepartmenttemp));//执行速度过慢，改成另起现成执行
				new Thread(new OrganisationComRunnable("department", "edit", String.valueOf(listdepartmenttemp))).start();
			}

			DepartmentComInfo.removeCompanyCache();
			ResourceComInfo.removeResourceCache();
			
			rs = new RecordSet();
			rs.executeSql("update orgchartstate set needupdate=1");

			//同步到CoreMail邮件系统开始
			if (supdepid == 0) {
				CoreMailAPI.synOrg("" + id, departmentname, "parent_org_unit_id=com_" + subcompanyid1 + "&org_unit_name=" + departmentname, "0");
				//testapi.synOrg(""+id, departmentname, "com_"+subcompanyid1, "0");
			} else {
				CoreMailAPI.synOrg("" + id, departmentname, "parent_org_unit_id=" + supdepid + "&org_unit_name=" + departmentname, "0");
				//testapi.synOrg(""+id, departmentname, ""+supdepid, "0");
			}
			//同步到CoreMail邮件系统结束

			//OA与第三方接口单条数据同步方法开始
			HrmServiceManager.SynInstantDepartment("" + id, "2");
			//OA与第三方接口单条数据同步方法结束
			HrmFaceCheckManager.sync(id+"",HrmFaceCheckManager.getOptUpdate(),this.getClass().getName(),HrmFaceCheckManager.getOaDepartment());

			//同步部门数据到矩阵
			MatrixUtil.updateDepartmentData("" + id);
			
			retmap.put("status", "1");
			retmap.put("message", SystemEnv.getHtmlLabelName(18758, user.getLanguage()));
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

	@Override
	public List<BizLogContext> getLogContexts() {
		return logger.getBizLogContexts();
	}
}
