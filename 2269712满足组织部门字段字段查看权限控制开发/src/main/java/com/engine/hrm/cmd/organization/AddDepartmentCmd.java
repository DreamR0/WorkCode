package com.engine.hrm.cmd.organization;

import com.api.hrm.util.ServiceUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateAuditType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.cmd.matrix.biz.MatrixMaintComInfo;
import com.engine.hrm.entity.RuleCodeType;
import com.engine.hrm.util.CodeRuleManager;
import com.engine.hrm.util.HrmOrganizationUtil;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.definedfield.HrmDeptFieldManagerE9;
import weaver.interfaces.email.CoreMailAPI;
import weaver.interfaces.hrm.HrmServiceManager;
import weaver.matrix.MatrixUtil;
import weaver.rtx.OrganisationCom;
import weaver.rtx.OrganisationComRunnable;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddDepartmentCmd extends AbstractCommonCommand<Map<String, Object>>{

	private SimpleBizLogger logger;

	public AddDepartmentCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;

		String departmentname = Util.fromScreen((String)params.get("departmentname"),user.getLanguage());

		this.logger = new SimpleBizLogger();
		BizLogContext bizLogContext = new BizLogContext();
		bizLogContext.setLogType(BizLogType.HRM_ENGINE);//模块类型
		bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION);
		bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_ORGANIZATION_DPEARTMENT);//当前小类型
		bizLogContext.setOperateAuditType(BizLogOperateAuditType.WARNING);//警告
		bizLogContext.setParams(params);//当前request请求参数
		logger.setUser(user);//当前操作人
		String mainSql = "select * from HrmDepartment where 1=2 ";
		logger.setMainSql(mainSql,"id");//主表sql
		logger.setMainPrimarykey("id");//主日志表唯一key
		logger.setMainTargetNameColumn("departmentname");//当前targetName对应的列（对应日志中的对象名）
		logger.before(bizLogContext);
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		try {

			if (!HrmUserVarify.checkUserRight("HrmDepartmentAdd:Add", user)) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
				return retmap;
			}
			
			DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
			//OrganisationCom OrganisationCom = new OrganisationCom();
			HrmServiceManager HrmServiceManager = new HrmServiceManager();
			
			String departmentmark = Util.fromScreen((String)params.get("departmentmark"),user.getLanguage());
			String departmentname = Util.fromScreen((String)params.get("departmentname"),user.getLanguage());
			String subcompanyid1 = Util.fromScreen((String)params.get("subcompanyid1"),user.getLanguage());
			int supdepid = Util.getIntValue((String)params.get("supdepid"),0);
			String showorder = Util.fromScreen(Util.null2s((String)params.get("showorder"),"0"),user.getLanguage());

			if(Util.getIntValue(subcompanyid1,0)<=0){
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(382647, user.getLanguage()));
				return retmap;
			}

			String allsupdepid = "0";
			String departmentcode = Util.fromScreen((String)params.get("departmentcode"),user.getLanguage());
			int coadjutant=Util.getIntValue((String)params.get("coadjutant"),0);  
			
			/*
			* Added by Charoes Huang
			* 判断是否10级部门
			*/
			int supdepartmentid = supdepid;
			if(supdepartmentid > 0){
				if(HrmOrganizationUtil.ifDeptLevelEquals10(supdepartmentid)){
					retmap.put("status", "-1");
					retmap.put("message", SystemEnv.getHtmlLabelName(26647, user.getLanguage()));
					return retmap;
				}
			}

			// 如果编码为空 自动生成编码
			departmentcode = CodeRuleManager.getCodeRuleManager().generateRuleCode(RuleCodeType.DEPARTMENT, subcompanyid1, departmentcode);

			if(!"".equals(departmentcode)){
				String sql2="select id from hrmdepartment where departmentcode = '" + departmentcode + "' ";
				rs.executeSql(sql2);
				if(rs.next()){
					retmap.put("status", "-1");
					retmap.put("message", SystemEnv.getHtmlLabelName(382653, user.getLanguage()));
					return retmap;
		    }
			}


		  char separator = Util.getSeparator() ;
			if(supdepid > 0){//当选择了上级部门的时候，需要更改当前部门的上级分部
				subcompanyid1 = DepartmentComInfo.getSubcompanyid1(supdepid+"");
			}
			
			String para =  departmentmark + separator + departmentname + separator +
			                supdepid+separator+allsupdepid+separator+subcompanyid1 + separator+ showorder+separator+coadjutant;
			rs.executeProc("HrmDepartment_Insert",para);
      int flag=rs.getFlag();
      if(flag==2){
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(382655, user.getLanguage()));
        return retmap;
      }else if(flag==3){
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(382657, user.getLanguage()));
        return retmap;
      }

			int id=0;
			if(rs.next()){
				id = rs.getInt(1);
			}
			
			weaver.hrm.company.OrgOperationUtil OrgOperationUtil = new weaver.hrm.company.OrgOperationUtil();
			OrgOperationUtil.updateDepartmentLevel(""+id,"0");
			String sql3="update hrmdepartment set departmentcode = '" + departmentcode + "' ";
			sql3 += ","+ DbFunctionUtil.getInsertUpdateSetSql(rs.getDBType(),user.getUID()) ;
			sql3+=" where id = "+id;
			rs.executeSql(sql3);
			HrmDeptFieldManagerE9 hrmDeptFieldManager = new HrmDeptFieldManagerE9(5);
			hrmDeptFieldManager.editCustomData(params,id);


			DepartmentComInfo.removeCompanyCache();
		  rs.executeSql("update orgchartstate set needupdate=1");

	    //add by wjy
	    //同步RTX端部门信息
	    //OrganisationCom.addDepartment(id);//执行速度过慢，改为另起线程执行
			new Thread(new OrganisationComRunnable("department", "add", String.valueOf(id))).start();
		    
		  //同步到CoreMail邮件系统开始
			if(supdepid == 0) {
				CoreMailAPI.synOrg(""+id, departmentname, "parent_org_unit_id=com_"+subcompanyid1+"&org_unit_name="+departmentname, "0");
				//testapi.synOrg(""+id, departmentname, "com_"+subcompanyid1, "0");
			} else {
				CoreMailAPI.synOrg(""+id, departmentname, "parent_org_unit_id="+supdepid+"&org_unit_name="+departmentname, "0");
				//testapi.synOrg(""+id, departmentname, ""+supdepid, "0");
			}
			//同步到CoreMail邮件系统结束
			
	    //OA与第三方接口单条数据同步方法开始
	    HrmServiceManager.SynInstantDepartment(""+id,"1"); 
	    //OA与第三方接口单条数据同步方法结束

			HrmFaceCheckManager.sync(id+"",HrmFaceCheckManager.getOptInsert(),this.getClass().getName(),HrmFaceCheckManager.getOaDepartment());
		    
		    
	    //同步部门数据到矩阵
	    MatrixUtil.updateDepartmentData(""+id);
	    //同步矩阵维护范围
		MatrixMaintComInfo maintComInfo = new MatrixMaintComInfo();
		maintComInfo.removeCache();
	    //初始化应用分权
	    new weaver.hrm.appdetach.AppDetachComInfo().initSubDepAppData();

			String mainSql = "select a.* from HrmDepartment a left join HrmDepartmentdefined b on a.id=b.deptid where a.id in(" + id + ")";
			String tableColumns = new ServiceUtil().getTableColumns("select * from HrmDepartmentdefined", "b", "id");
			if (!tableColumns.equals("")) {
				mainSql = "select a.*," + tableColumns + " from HrmDepartment a left join HrmDepartmentdefined b on a.id=b.deptid where a.id in(" + id + ")";
			}
			logger.setMainSql(mainSql, "id");//主表sql

	    retmap.put("id", id);
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
