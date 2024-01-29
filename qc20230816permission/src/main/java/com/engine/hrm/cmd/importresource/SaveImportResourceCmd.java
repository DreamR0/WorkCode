package com.engine.hrm.cmd.importresource;

import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import weaver.file.FileUploadToPath;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.join.hrm.in.IHrmImportAdapt;
import weaver.join.hrm.in.IHrmImportProcessE9;
import weaver.matrix.MatrixUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SaveImportResourceCmd extends AbstractCommonCommand<Map<String, Object>>{
	public static ThreadLocal<String> threadLocal = new ThreadLocal<>();

	protected HttpServletRequest request;
	
	public SaveImportResourceCmd(Map<String, Object> params, HttpServletRequest request, User user) {
		this.request = request;
        this.user = user;
        this.params = params;
    }
	

	@Override
	public Map<String,Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			 /*综合考虑多数据源后，实现通过配置文件配置适配器和解析类*/
			List<Object> lsErrorInfo = new ArrayList<Object>();
	  	
			IHrmImportAdapt importAdapt=(IHrmImportAdapt)Class.forName("weaver.join.hrm.in.adaptImpl.HrmImportAdaptExcelE9").newInstance();
		  
		  FileUploadToPath fu = new FileUploadToPath(request) ;

			int language = this.user.getLanguage();
			writeLog("lxr2018>>>language1=" + language);
			importAdapt.setUserlanguage(language);
			new BaseBean().writeLog("==zj==(导入前校验)");
			threadLocal.set(user.getUID()+"");
			List errorInfo = importAdapt.creatImportMap(fu);
		  
		  //如果读取数据和验证模板没有发生错误
		  if(errorInfo.isEmpty()){
			  Map hrMap=importAdapt.getHrmImportMap();
			
			  IHrmImportProcessE9 importProcess=(IHrmImportProcessE9)Class.forName("weaver.join.hrm.in.processImpl.HrmImportProcessE9").newInstance();
			  importProcess.init(request);
			  importProcess.processMap(hrMap);
			  //同步部门数据到矩阵
			  MatrixUtil.sysDepartmentData();
			  //同步分部数据到矩阵
		    MatrixUtil.sysSubcompayData();
		  }else{
				if(errorInfo!=null&&!errorInfo.isEmpty()){
					Map<String,Object> error = null;
					for(int i=0;i<errorInfo.size();i++){
						error = new HashMap<String,Object>();
						error.put("message",Util.null2String(errorInfo.get(i)));
						lsErrorInfo.add(error);
					}
				}
		  }
		  retmap.put("errorInfo", lsErrorInfo);
		  retmap.put("status", "1");
		} catch (Exception e) {
			writeLog("人员导入失败：" + e);
			retmap.put("status", "-1");
			retmap.put("message", e.getMessage());
		}
		return retmap;
	}
	
	@Override
	public BizLogContext getLogContext() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
