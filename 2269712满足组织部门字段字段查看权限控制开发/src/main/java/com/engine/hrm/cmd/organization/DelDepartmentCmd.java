package com.engine.hrm.cmd.organization;

import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogOperateAuditType;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import weaver.conn.RecordSet;
import org.apache.commons.lang3.StringUtils;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.company.DepartmentComInfo;
import weaver.integration.framework.data.record.SimpleRecordData;
import weaver.interfaces.email.CoreMailAPI;
import weaver.interfaces.hrm.HrmServiceManager;
import weaver.matrix.MatrixUtil;
import weaver.rtx.OrganisationCom;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelDepartmentCmd extends AbstractCommonCommand<Map<String, Object>>{

	private SimpleBizLogger logger;

	public DelDepartmentCmd(Map<String, Object> params, User user) {
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
		String mainSql = "select * from Hrmdepartment where id in("+id+")";
		logger.setMainSql(mainSql,"id");//主表sql
		logger.setMainPrimarykey("id");//主日志表唯一key
		logger.setMainTargetNameColumn("departmentname");//当前targetName对应的列（对应日志中的对象名）
		logger.before(bizLogContext);
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			if(!HrmUserVarify.checkUserRight("HrmDepartmentEdit:Delete", user)){
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
				return retmap;
			}
			
			String ids = Util.null2String((String)params.get("id"));
			
			retmap = checkCanDelelte(ids) ;
			if(!retmap.isEmpty()){
				return retmap;
			}
			
			String[] arrIds = Util.splitString(ids, ",");
			if(arrIds.length>1){
				for(int i=0; i<arrIds.length;i++){
					int id = Util.getIntValue(arrIds[i]);
					this.del(id);
				}
			}else{
				int id = Util.getIntValue(ids);
				retmap = this.del(id);
				if(!retmap.isEmpty()){
					return retmap;
				}
			}
			retmap.put("status", "1");
			retmap.put("message", SystemEnv.getHtmlLabelName(22155, user.getLanguage()));
		} catch (Exception e) {
			writeLog(e);
			retmap.put("status", "-1");
			retmap.put("message", SystemEnv.getHtmlLabelName(22155, user.getLanguage()));
		}
		return retmap;
	}
	
	private Map<String, Object> checkCanDelelte(String id)throws Exception{
		if(StringUtils.isBlank(id)) throw new Exception("删除部门错误：id为空！") ;
		Map<String, Object> retmap = new HashMap<String, Object>();
		RecordSet rs = new RecordSet();
		String sql = "select count(id) from HrmResource where departmentid  in ("+id+")";
		rs.execute(sql) ;
		rs.next();
		if(rs.getInt(1)>0){
			retmap.put("status", "-1");
			retmap.put("message", SystemEnv.getHtmlLabelName(33594,user.getLanguage()));
			return retmap;
		}
		return retmap;

	}
	
	private Map<String, Object> del(int id) throws Exception {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			String sql = "";
			RecordSet rs = new RecordSet();;
			
			DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
			OrganisationCom OrganisationCom = new OrganisationCom();
			HrmServiceManager HrmServiceManager = new HrmServiceManager();
			
			String departmentname = "";
			int supdepid = 0;
			String subcompanyid1 = "";
	  	
			rs.executeSql("select departmentname, supdepid, subcompanyid1 from HrmDepartment  where id = " + id);
			if(rs.next()) {
				departmentname = Util.null2String(rs.getString("departmentname"));
				supdepid = Util.getIntValue(rs.getString("supdepid"), 0);
				subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
			}
		  
			String para = ""+id;

			
			SimpleRecordData recordData = HrmFaceCheckManager.syncQuery(id+"",HrmFaceCheckManager.getOptDelete(),HrmFaceCheckManager.getOaDepartment());
	    rs.executeProc("HrmDepartment_Delete",para);
      //add by wjy
      //同步RTX端部门信息
      OrganisationCom.deleteDepartment(id);
				
      //同步到CoreMail邮件系统开始
    	if(supdepid == 0) {
    		CoreMailAPI.synOrg(""+id, departmentname, "parent_org_unit_id=com_"+subcompanyid1+"&org_unit_name="+departmentname, "1");
    		//testapi.synOrg(""+id, departmentname, "com_"+subcompanyid1, "1");
    	} else {
    		CoreMailAPI.synOrg(""+id, departmentname, "parent_org_unit_id="+supdepid+"&org_unit_name="+departmentname, "1");
    		//testapi.synOrg(""+id, departmentname, ""+supdepid, "1");
    	}
    	//同步到CoreMail邮件系统结束
		        
			//OA与第三方接口单条数据同步方法开始
	    HrmServiceManager.SynInstantDepartment(""+id,"3");
	    //OA与第三方接口单条数据同步方法结束

			HrmFaceCheckManager.sync(id+"",HrmFaceCheckManager.getOptDelete(),this.getClass().getName(),HrmFaceCheckManager.getOaDepartment(),recordData);
	    
			DepartmentComInfo.removeCompanyCache();
			
		  rs.executeSql("update orgchartstate set needupdate=1");
	    
		  //同步部门数据到矩阵
	    MatrixUtil.updateDepartmentData(""+id);
		} catch (Exception e) {
			writeLog(e);
			throw e;
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
