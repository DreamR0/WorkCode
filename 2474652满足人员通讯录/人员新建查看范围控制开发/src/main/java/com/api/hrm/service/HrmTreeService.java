package com.api.hrm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2474652.Util.HrmVarifyCustom;
import com.api.hrm.bean.TreeNode;

import com.engine.common.service.HrmCommonService;
import com.engine.common.service.impl.HrmCommonServiceImpl;
import com.engine.hrm.util.HrmUtil;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.common.database.dialect.DialectUtil;
import weaver.hrm.company.CompanyComInfo;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.CompanyVirtualComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.companyvirtual.SubCompanyVirtualComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import weaver.systeminfo.systemright.OrganizationUtil;

public class HrmTreeService extends BaseBean{

	private AppDetachComInfo adci = null;
	
	private List<String> lsSubCom4Search = null;
	private List<String> lsDept4Search = null;
	private User user = null;
	private String virtualCompanyid = null;
	private boolean isLoadSubDepartment = false;
	private boolean isLoadAllSub = false;
	private boolean isLoadUser = false;
	private boolean isNoAccount = false;
	private boolean isDetach = false;//是否分权
	private boolean isBlogDetach = false;//是否微博分权
	private boolean isOdocDetach = false;//是否公文分权
	private boolean isExecutionDetach = false;//是否执行力分权
	private boolean showDepartmentFullName = false;//部门是否显示全称
	private boolean showSubCompanyFullName = false;//分部是否显示全称
	private boolean showCanceled = false;//是否加载封存数据
	private int[] allSubComIds = null;//有权限的机构，包含必要的构成树的上级
	private int[] subComIds = null;//有权限的机构，不包含必要的构成树的上级
    private int[] depIds= null;//角色级别是部门的，只能看到自己的部门
    private String rightStr= null;//权限字符串
	private String[] allChildSubComIds = null;//人员所属分部的下级分部
	private String[] allChildDeptIds = null;//人员所属部门的下级部门
	private Boolean sysadmin = false;//系统管理员--为了解决mysql环境系统管理员微博分权左侧树，查询速度超过7分钟。
	private Boolean isOrgTree = true;
	private String classification = ""; // 密级过滤 >=传入密级

	private String isCustom = "";//自定义组织架构

	public Map<String, Object> getCompanyTree(HttpServletRequest request, HttpServletResponse response) {
		User user = HrmUserVarify.getUser(request, response);
		String id = Util.null2String(request.getParameter("id"));
		//qc2474652 这里获取下是否是自定义组织架构
		isCustom = Util.null2String(request.getParameter("custom"));
		// 虚拟组织
		String virtualCompanyid = Util.null2String(request.getParameter("virtualCompanyid"));
		// 是否加载部门
		boolean isLoadSubDepartment = "1".equals(Util.null2String(request.getParameter("isLoadSubDepartment")));
		// 是否加载所有下级数据
		boolean isLoadAllSub = "1".equals(Util.null2String(request.getParameter("isLoadAllSub")));
		//是否加载人员
		boolean isLoadUser = "1".equals(Util.null2String(request.getParameter("isLoadUser")));
		//是否加载无账号人员
		boolean isNoAccount = "1".equals(Util.null2String(request.getParameter("isNoAccount")));
		isNoAccount = true;
		//是否加载封存数据
		boolean showCanceled = "1".equals(Util.null2String(request.getParameter("showCanceled")));
		//权限字符串
		String rightStr = Util.null2String(request.getParameter("rightStr"));
		//qc2474652 这里把权限清空写死
		if (user.getUID() != 1){
			if ("1".equals(isCustom)){
				if ("1".equals(isCustom)){
					rightStr = "";
				}
			}
		}
		//微博页面组织结构分部、部门需要将canclick置为false
		isOrgTree = !"1".equals(Util.null2String(request.getParameter("isOrgTree")));

		// 密级
		classification = Util.null2String(request.getParameter("classification"));

		//分部显示全称
		boolean showSubCompanyFullName = Util.null2String(Prop.getInstance().getPropValue("Others", "showSubCompanyFullName")).equals("1");
		//部门显示全称
		boolean showDepartmentFullName = Util.null2String(Prop.getInstance().getPropValue("Others", "showDepartmentFullName")).equals("1");

		if(isLoadUser)isLoadSubDepartment=true;
		
		this.user = user;
		this.virtualCompanyid = virtualCompanyid;
		this.isLoadSubDepartment = isLoadSubDepartment;
		this.isLoadAllSub = isLoadAllSub;
		this.isLoadUser = isLoadUser;
		this.isNoAccount = isNoAccount;
		this.showCanceled = showCanceled;
		this.showSubCompanyFullName = showSubCompanyFullName;
		this.showDepartmentFullName =showDepartmentFullName;
		this.rightStr = rightStr;

		if(user.isAdmin()&&user.getUID()==1)  sysadmin = true;
        if(!sysadmin&&rightStr.length()>0){
			//微博和执行力左侧树单独处理:开启管理分权总开关，但是未开启微博分权或者执行力分权开关。
			ManageDetachComInfo ma=new ManageDetachComInfo();
			this.isDetach =ma.getDetachable().equals("1");
			this.isBlogDetach = ma.isUseBlogManageDetach();
			this.isOdocDetach = ma.isUseOdocManageDetach();
			this.isExecutionDetach = ma.isUseExecutionManageDetach();
			if(rightStr.equals("blog:newappSetting")&&this.isDetach&&!this.isBlogDetach){
				this.isDetach = false;
			}else if((rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet"))&&this.isDetach&&!this.isExecutionDetach){
				this.isDetach = false;
			}else if((rightStr.equals("OdoSpecification:Edit"))&&!this.isOdocDetach){//开启公文分权树才做分权处理
				this.isDetach = false;
			}
			this.getSubCompanyTreeListByRight(user.getUID(), rightStr);
			checkBlogAppLoadSub(rightStr);
		}
		
		//查询左侧树
		String keyword = Util.null2String(request.getParameter("keyword")).trim().toLowerCase();
		if(keyword.length()>0){
			this.getSubComDept4Search(keyword);
		}
		
		if("".equals(id)){
			Map<String,Object> apidatas = new HashMap<String,Object>();
			
			CompanyComInfo companyComInfo = null;
			CompanyVirtualComInfo companyVirtualComInfo = null;
			// 加载顶级分部一级分部
			try {
				companyComInfo = new CompanyComInfo();
				companyVirtualComInfo = new CompanyVirtualComInfo();
				companyVirtualComInfo.setUser(user);
			} catch (Exception e) {
			}

			List<OrgBean> companys = new ArrayList<OrgBean>();

			OrgBean companyInfo = null;
			if (companyComInfo.getCompanyNum() > 0) {
				companyComInfo.setTofirstRow();
				while (companyComInfo.next()) {
					companyInfo = new OrgBean();
					companyInfo.setCompanyid(companyComInfo.getCompanyid());
					companyInfo.setName(companyComInfo.getCompanyname());
					companyInfo.setIsVirtual("0");
					companyInfo.setIcon("icon-coms-LargeArea");
					companys.add(companyInfo);
				}
			}
			// 加载虚拟组织列表
			if(companyVirtualComInfo.getCompanyNum() > 0) {
				companyVirtualComInfo.setTofirstRow();

				while (companyVirtualComInfo.next()) {
					// -20001=公文交换维度  -10000=客户维度
					if(HrmUtil.isHideDefaultDimension(Util.null2String(companyVirtualComInfo.getCompanyid()))){
						continue;
					}

					// 只控制默认2个维度，自定义维度不控制
					companyInfo = new OrgBean();
					companyInfo.setCompanyid(companyVirtualComInfo.getCompanyid());
					companyInfo.setName(companyVirtualComInfo.getVirtualType());
					companyInfo.setIsVirtual("1");
					companyInfo.setIcon("icon-coms-LargeArea");
					companys.add(companyInfo);
				}

			}
			Map<String, Object> resultDatas = new HashMap<String, Object>();
			apidatas.put("companys", companys);
			String companyname = companyComInfo.getCompanyname("1");
			OrgBean root = new OrgBean();
			if (("".equals(this.virtualCompanyid)||"1".equals(this.virtualCompanyid))) {
				new BaseBean().writeLog("==zj==(左侧树if)");
				root.setId("0");
				root.setCompanyid("1");
				root.setName(companyname);
				root.setType("0");
				root.setIsVirtual("0");
				root.setIcon("icon-coms-LargeArea");
				
				// 加载下级分部
				loadSubCompanys(root);
			} else {
				// 虚拟组织
				root.setId("0");
				root.setCompanyid(this.virtualCompanyid);
				root.setName(companyVirtualComInfo.getVirtualType(this.virtualCompanyid));
				root.setType("0");
				root.setIsVirtual("1");
				root.setIcon("icon-coms-LargeArea");
				loadVirtualSubCompanyInfo(root);
			}
			
			resultDatas.put("rootCompany", root);
			apidatas.put("datas", resultDatas);
			return apidatas;
		}else{
			return getTreeNodeData(request, response);
		}
	}

	/**
	 * 加载下级分部
	 * 
	 */
	private void loadSubCompanys(OrgBean parentOrg) {
		SubCompanyComInfo rs = null;
		try{
			rs = new SubCompanyComInfo();
		}catch(Exception e){
			e.printStackTrace();
		}
		rs.setTofirstRow();

		String subId = parentOrg.getId();
		boolean canview = this.checkDetach("com", subId);
		
		if (this.isLoadSubDepartment && canview) {
			// loadsubDepartments
			loadSubDepartments(parentOrg,null);
		}

		List<TreeNode> subOrgs = null;
		if (parentOrg.getSubs() != null) {
			subOrgs = parentOrg.getSubs();
		} else {
			subOrgs = new ArrayList<TreeNode>();
		}

		while (rs.next()) {
			String id = rs.getSubCompanyid();
			String name = rs.getSubCompanyname();
			String supsubcomid = rs.getSupsubcomid();
			if (!this.showCanceled && "1".equals(rs.getCompanyiscanceled())){
				continue;
			}
			if (supsubcomid.equals(""))
				supsubcomid = "0";
			if (!supsubcomid.equals(parentOrg.getId()))
				continue;


			if(this.showSubCompanyFullName){
				name = rs.getSubCompanydesc();
			}

			if(!this.checkDetach("com", id)){
				continue;
			}
			if(!sysadmin&&!this.checkBlogAppSetting("com",this.rightStr , id)){
				continue;
			}
			if(!sysadmin&&(rightStr.equals("Execution:PerformanceSet")||rightStr.equals("Execution:ReportSet"))&&!this.checkBlogAppSetAgain()){
				continue;
			}
			if (lsSubCom4Search!=null && !lsSubCom4Search.contains(id)){
				continue;
			}

			//qc2474652 这里将分部根据建模表进行过滤
			if (user.getUID() != 1) {
				if ("1".equals(isCustom)) {
					//先判断是否有权限
					HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
					Boolean isRight = hrmVarifyCustom.checkUserRight(user);
					if (!isRight){
						continue;
					}
					Boolean isHave = false;
					String[] subCompanyTree = hrmVarifyCustom.getSubCompanyTree(user);
					if (subCompanyTree.length <=0 || subCompanyTree == null){

					}else {
						for (int i = 0; i < subCompanyTree.length; i++) {
							if (id.equals(subCompanyTree[i])) {
								isHave = true;
								break;
							}
						}
						if (!isHave) {
							continue;
						}
					}
				}
			}
			OrgBean orgBean = new OrgBean();
			orgBean.setId(id);
			orgBean.setName(name);
			orgBean.setPid(parentOrg.getId());
			orgBean.setType("1");
			orgBean.setIsVirtual("0");
			orgBean.setIcon("icon-coms-LargeArea");
			orgBean.setCanceled("1".equals(rs.getCompanyiscanceled()));
			//orgBean.setCanClick(!this.isLoadSubDepartment);
			orgBean.setCanClick(isOrgTree);
			if(!this.isLoadSubDepartment){
				orgBean.setTitle(getAllParentsOrg(id,"0"));
			}
			subOrgs.add(orgBean);

			parentOrg.setIsParent(true);

			// 加载下级分部
			if (this.isLoadAllSub) {
				loadSubCompanys(orgBean);
			} else {
				// 更新当前节点isParent状态
				validOrgIsParent(orgBean);
			}
		}
		parentOrg.setSubs(subOrgs);
	}

	/**
	 * 不加载下级数据时，判断当前节点是否有下级分部
	 * 
	 * @param parentOrg
	 */
	private void validOrgIsParent(OrgBean parentOrg) {
		SubCompanyComInfo rs = null;
		try{
			rs = new SubCompanyComInfo();
		}catch(Exception e){
			e.printStackTrace();
		}
		rs.setTofirstRow();

		if (this.isLoadSubDepartment) {
			validOrgIsParent(parentOrg, null);
		}

		while (rs.next()) {
			String supsubcomid = rs.getSupsubcomid();
			if (supsubcomid.equals(""))
				supsubcomid = "0";
			if (!supsubcomid.equals(parentOrg.getId()))
				continue;

			String id = rs.getSubCompanyid();
			if (!this.showCanceled && "1".equals(rs.getCompanyiscanceled())){
				continue;
			}
			
			if (lsSubCom4Search!=null && !lsSubCom4Search.contains(id)){
				continue;
			}
			
			if(!this.checkDetach("com", id)){
				continue;
			}
			if(!sysadmin&&!this.checkBlogAppSetting("com",this.rightStr , id)){
				continue;
			}
			parentOrg.setIsParent(true);
			break;
		}
	}

	/**
	 * 不加载下级数据时,且开启加载部门，判断当前节点是否有下级部门
	 * 
	 * @param pSubCompany
	 * @param pDepartment
	 */
	private void validOrgIsParent(OrgBean pSubCompany, OrgBean pDepartment) {
		DepartmentComInfo rsDepartment = null;
		try{
			rsDepartment = new DepartmentComInfo();
		}catch(Exception e){
			e.printStackTrace();
		}
		rsDepartment.setTofirstRow();
		String pdepartmentId = pDepartment == null ? "0" : pDepartment.getId();
		String subcompanyid = Util.null2String(pSubCompany.getId());
		if("".equals(subcompanyid)){
			subcompanyid = pDepartment == null ? "0":rsDepartment.getSubcompanyid1(pdepartmentId);
		}
		
		while (rsDepartment.next()) {
			String id = rsDepartment.getDepartmentid();
			String supdepid = rsDepartment.getDepartmentsupdepid();
			
			if (pdepartmentId.equals("0") && supdepid.equals("")) {
				supdepid = "0";
			}
			if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(pdepartmentId) 
					|| (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && pdepartmentId.equals("0"))))){
				continue;
			}
			
			if (!this.showCanceled && "1".equals(rsDepartment.getDeparmentcanceled())){
				continue;
			}
			
			if (lsDept4Search!=null && !lsDept4Search.contains(id)){
				continue;
			}
			
			if(!this.checkDetach("dept", id)){
				continue;
			}
			
			pSubCompany.setIsParent(true);
			if (pDepartment != null) {
				pDepartment.setIsParent(true);
			}

			break;
		}
		
		//判断部门下是否有人员
		if(this.isLoadUser && pDepartment != null && !pDepartment.getIsParent()){
			ResourceComInfo rsResource = null;
			try{
				rsResource = new ResourceComInfo();
				rsResource.setTofirstRow();
				while (rsResource.next()) {
					String str = rsResource.getDepartmentID();
					if (str.equals(pdepartmentId)){
						pDepartment.setIsParent(true);
						break;
					}
				}
			}catch (Exception e) {
				writeLog(e);
			}
		}
	}

	/**
	 * 加载下级部门
	 * 
	 */
	private void loadSubDepartments(OrgBean pSubCompany, OrgBean pDepartment) {
		DepartmentComInfo rsDepartment = null;
		try{
			rsDepartment = new DepartmentComInfo();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(this.isLoadUser && pDepartment!=null){//加载人员
			loadDeptUser(pDepartment);
		}
		
		String pdepartmentId = pDepartment == null ? "0" : pDepartment.getId();
		String subcompanyid = Util.null2String(pSubCompany.getId());
		if("".equals(subcompanyid)){
			subcompanyid = rsDepartment.getSubcompanyid1(pdepartmentId);
		}
		String pId = pDepartment == null ? pSubCompany.getId() : pDepartment.getId();

		List<TreeNode> subOrgs = null;
		if (pDepartment == null && pSubCompany.getSubs() != null) {
			subOrgs = pSubCompany.getSubs();
		} else {
			if(pDepartment != null){
				subOrgs = pDepartment.getSubs();
			}
			if(subOrgs==null){
				subOrgs = new ArrayList<TreeNode>();
			}
		}
		rsDepartment.setTofirstRow();
		while (rsDepartment.next()) {
			String supdepid = rsDepartment.getDepartmentsupdepid();
			if(!this.showCanceled && "1".equals(rsDepartment.getDeparmentcanceled())){
				continue;
			}
			if (pdepartmentId.equals("0") && supdepid.equals("")) {
				supdepid = "0";
			}
			if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(pdepartmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && pdepartmentId
					.equals("0")))))
				continue;

			String id = rsDepartment.getDepartmentid();
			String name = rsDepartment.getDepartmentmark();
			if(this.showDepartmentFullName){
				name = rsDepartment.getDepartmentname();
			}

			if(!this.checkDetach("dept", id)){
				continue;
			}
			if(!sysadmin&&!this.checkBlogAppSetting("dept",this.rightStr,id)){
				continue;
			}
			if (lsDept4Search!=null && !lsDept4Search.contains(id)){
				continue;
			}
			//qc2474652 这里根据建模表对部门进行过滤
			if (user.getUID() != 1){
				if ("1".equals(isCustom)){
					//先判断是否有权限
					HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
					Boolean isRight = hrmVarifyCustom.checkUserRight(user);
					if (!isRight){
						continue;
					}
					Boolean isHave = false;
					ArrayList<String> departmentList = hrmVarifyCustom.getDepartment(user);
					if (departmentList == null || departmentList.size() <= 0){

					}else {
						for (int i = 0; i < departmentList.size(); i++) {
							if (id.equals(departmentList.get(i))){
								isHave = true;
								break;
							}
						}
						if (!isHave){
							continue;
						}
					}
				}
			}
			OrgBean orgBean = new OrgBean();
			orgBean.setId(id);
			orgBean.setPid(pId);
			orgBean.setName(name);
			orgBean.setType("2");
			orgBean.setIsVirtual("0");
			orgBean.setCanClick(isOrgTree);
			orgBean.setCanceled("1".equals(rsDepartment.getDeparmentcanceled()));
			orgBean.setTitle(getAllParentsOrg(id,"1"));
			orgBean.setPsubcompanyid(pSubCompany.getId());
			orgBean.setIcon("icon-coms-Branch");


			pSubCompany.setIsParent(true);
			if (pDepartment != null) {
				pDepartment.setIsParent(true);
			}


			subOrgs.add(orgBean);

			if (this.isLoadAllSub) {
				loadSubDepartments(pSubCompany, orgBean);
			} else {
				validOrgIsParent(pSubCompany, orgBean);
			}

			//qc2474652底层部门不显示图标
			if (user.getUID() != 1){
				if("1".equals(isCustom)){
					Boolean isParent = false;
					HrmVarifyCustom hrmVarifyCustom = new HrmVarifyCustom();
					String departments = hrmVarifyCustom.getDepartment(user, false);
					if (departments.contains(id)){
						//判断是否有子部门
						if (hrmVarifyCustom.isParent(user,id,true)){
							orgBean.setIsParent(true);
						}else {
							orgBean.setIsParent(false);
						}
					}
				}
			}

		}

		if (pDepartment == null) {
			pSubCompany.setSubs(subOrgs);
		} else {
			pDepartment.setSubs(subOrgs);
		}
	}
	
	/**
	 * 加载人员
	 * @param pDepartment
	 */
	@SuppressWarnings("deprecation")
	private void loadDeptUser(OrgBean pDepartment) {
		List<TreeNode> subOrgs = null;
		DepartmentComInfo rsDepartment = null;
    ResourceComInfo ResourceComInfo = null;
		RecordSet rs1 = new RecordSet();
		try{
			ResourceComInfo = new ResourceComInfo();
	    rsDepartment = new DepartmentComInfo();
	    rsDepartment.setTofirstRow();
	    
			if(pDepartment != null){
				subOrgs = pDepartment.getSubs();
			}
			if(subOrgs==null){
				subOrgs = new ArrayList<TreeNode>();
			}
			
	    String departmentId = pDepartment.getId();
	    String sqlwhere = "";
	    
			String sql = "select hr.id, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle, loginid " 
								 + "from hrmresource hr, hrmdepartment t2 " 
								 + "where hr.departmentid=t2.id and t2.id=" + departmentId ;

			if(!isNoAccount){
				sqlwhere += " and hr.loginid is not null "+(rs1.getDBType().equals("oracle")?"":" and hr.loginid<>'' ");
			}

		  AppDetachComInfo adci = new AppDetachComInfo();
			if(sqlwhere.length()>0)sqlwhere=" and " +sqlwhere;
			sqlwhere+=" and hr.status in (0,1,2,3)";

			// 筛选出比传入密级高或相等的人员
			if(!"".equals(classification)){
				sqlwhere += " and ( hr.classification in (select seclevel from UserClassification " +
						"where Optionalresourceseclevel like '%"+classification+"%') or hr.classification = "+classification +" ) ";
			}

			if(adci.isUseAppDetach()){
				String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
				String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
				sqlwhere+=tempstr;
			}
			if(sqlwhere.length()>0)sql+=sqlwhere;
			sql += " order by hr.dsporder ";
			writeLog("#1110 sql="+sql);
			rs1.executeSql(sql);
			while (rs1.next()) {
				String id = rs1.getString("id");
				String lastname = rs1.getString("lastname");

				OrgBean orgBean = new OrgBean();
				orgBean.setId(id);
				orgBean.setPid(departmentId);
				orgBean.setName(lastname);
				orgBean.setType("3");
				orgBean.setDisplayType("portrait");
				orgBean.setIsVirtual("0");
				orgBean.setCanClick(true);
				orgBean.setTitle("");
				orgBean.setIcon(ResourceComInfo.getMessagerUrls(id));
				orgBean.setRequestParams(ResourceComInfo.getUserIconInfoStr(id,user));
				orgBean.setPsubcompanyid(pDepartment.getPsubcompanyid());
				pDepartment.setIsParent(true);

				subOrgs.add(orgBean);
			}
			pDepartment.setSubs(subOrgs);
		}catch (Exception e) {
			writeLog(e);
		}
	}

	/**
	 * 加载虚拟分部
	 * 
	 */
	private void loadVirtualSubCompanyInfo(OrgBean parentOrg) {
		SubCompanyVirtualComInfo rs = null;
		try{
			rs = new SubCompanyVirtualComInfo();
			if("".equals(Util.null2String(parentOrg.getCompanyid()))){
				parentOrg.setCompanyid(rs.getCompanyid(parentOrg.getId()));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		rs.setTofirstRow();

		// 加载虚拟部门
		if (this.isLoadSubDepartment) {
			loadVirtualSubDepartments(parentOrg,null);
		}
		
		List<TreeNode> subOrgs = null;
		if (parentOrg.getSubs() != null) {
			subOrgs = parentOrg.getSubs();
		} else {
			subOrgs = new ArrayList<TreeNode>();
		}
		
		while (rs.next()) {
			String id = rs.getSubCompanyid();
			String supsubcomid = rs.getSupsubcomid();
			String comid = rs.getCompanyid();
			
			if(!this.showCanceled && "1".equals(rs.getCompanyiscanceled())){
				continue;
			}
			if (!comid.equals(parentOrg.getCompanyid()))
				continue;
			if (supsubcomid.equals(""))
				supsubcomid = "0";
			if (!supsubcomid.equals(parentOrg.getId()))
				continue;

			if (lsSubCom4Search!=null && !lsSubCom4Search.contains(id)){
				continue;
			}

			String name = rs.getSubCompanyname();
			if(this.showSubCompanyFullName){
				name = rs.getSubCompanydesc();
			}
			OrgBean virtualOrgBean = new OrgBean();

			virtualOrgBean.setId(id);
			virtualOrgBean.setPid(parentOrg.getId());
			virtualOrgBean.setName(name);
			virtualOrgBean.setCompanyid(parentOrg.getCompanyid());
			virtualOrgBean.setType("1");
			virtualOrgBean.setIsVirtual("1");
			//virtualOrgBean.setCanClick(!this.isLoadSubDepartment);
			virtualOrgBean.setCanceled("1".equals(rs.getCompanyiscanceled()));
			virtualOrgBean.setCanClick(isOrgTree);
			virtualOrgBean.setIcon("icon-coms-LargeArea");
			if(!this.isLoadSubDepartment){
				virtualOrgBean.setTitle(getAllParentsOrg(id,"1+"+parentOrg.getCompanyid()));
			}
			parentOrg.setIsParent(true);

			subOrgs.add(virtualOrgBean);

			if (this.isLoadAllSub) {
				loadVirtualSubCompanyInfo(virtualOrgBean);
			} else {
				validVirtualOrgIsParent(virtualOrgBean);
			}
		}
		parentOrg.setSubs(subOrgs);
	}

	/**
	 * 加载虚拟部门
	 * 
	 */
	private void loadVirtualSubDepartments(OrgBean pVCompany, OrgBean pVDepartment) {
		DepartmentVirtualComInfo rsDepartment = null;
		try{
			rsDepartment = new DepartmentVirtualComInfo();
		}catch(Exception e){
			e.printStackTrace();
		}

		if(this.isLoadUser && pVDepartment!=null){//加载人员
			loadVirtualDeptUser(pVDepartment);
		}
		
		String departmentId = pVDepartment == null ? "0" : pVDepartment.getId();
		String subcompanyid = Util.null2String(pVCompany.getId());
		if("".equals(subcompanyid)){
			subcompanyid = rsDepartment.getSubcompanyid1(departmentId);
		}

		String pId = pVDepartment == null ? pVCompany.getId() : pVDepartment.getId();
		List<TreeNode> subOrgs = null;
		if (pVDepartment == null && pVCompany.getSubs() != null) {
			subOrgs = pVCompany.getSubs();
		} else {
			if(pVDepartment != null){
				subOrgs = pVDepartment.getSubs();
			}
			if(subOrgs==null){
				subOrgs = new ArrayList<TreeNode>();
			}
		}
		rsDepartment.setTofirstRow();
		while (rsDepartment.next()) {
			if(!this.showCanceled && "1".equals(rsDepartment.getDeparmentcanceled())){
				continue;
			}
			if (departmentId.equals(rsDepartment.getDepartmentid()))
				continue;
			String supdepid = rsDepartment.getDepartmentsupdepid();
			if (departmentId.equals("0") && supdepid.equals(""))
				supdepid = "0";
			if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(departmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && departmentId.equals("0")))))
				continue;

			String id = rsDepartment.getDepartmentid();
			String name = rsDepartment.getDepartmentmark();
			if(this.showDepartmentFullName){
				name = rsDepartment.getDepartmentname();
			}
			if (lsDept4Search!=null && !lsDept4Search.contains(id)){
				continue;
			}
			
			OrgBean virtualOrgBean = new OrgBean();
			virtualOrgBean.setId(id);
			virtualOrgBean.setPid(pId);
			virtualOrgBean.setName(name);
			virtualOrgBean.setType("2");
			virtualOrgBean.setIsVirtual("1");
			virtualOrgBean.setCanClick(isOrgTree);
			virtualOrgBean.setCanceled("1".equals(rsDepartment.getDeparmentcanceled()));
			virtualOrgBean.setPsubcompanyid(pVCompany.getId());
			virtualOrgBean.setTitle(getAllParentsOrg(id,"1+"+pVCompany.getCompanyid()));
			virtualOrgBean.setIcon("icon-coms-Branch");
			pVCompany.setIsParent(true);
			if (pVDepartment != null) {
				pVDepartment.setIsParent(true);
			}

			subOrgs.add(virtualOrgBean);

			if (this.isLoadAllSub) {
				loadVirtualSubDepartments(pVCompany, virtualOrgBean);
			} else {
				validVirtualOrgIsParent(pVCompany, virtualOrgBean);
			}
		}

		if (pVDepartment == null) {
			pVCompany.setSubs(subOrgs);
		} else {
			pVDepartment.setSubs(subOrgs);
		}
	}

	/**
	 * 加载人员
	 * @param pDepartment
	 */
	private void loadVirtualDeptUser(OrgBean pDepartment) {
		List<TreeNode> subOrgs = null;
		DepartmentComInfo rsDepartment = null;
		ResourceComInfo resourceComInfo = null;
		try{
			resourceComInfo =  new ResourceComInfo();
	    rsDepartment = new DepartmentComInfo();
	    rsDepartment.setTofirstRow();
	    
			if(pDepartment != null){
				subOrgs = pDepartment.getSubs();
			}
			if(subOrgs==null){
				subOrgs = new ArrayList<TreeNode>();
			}
			
	    String departmentId = pDepartment.getId();
	    String sqlwhere = "";
	    
			String sql = " select hr.id, hr.loginid, hr.account, lastname, hr.pinyinlastname, hr.subcompanyid1, hr.jobtitle " 
								 + " from hrmresource hr " 
								 + "where 1=1 ";
			AppDetachComInfo adci = new AppDetachComInfo();
			
			if(sqlwhere.length()>0)sqlwhere=" and " +sqlwhere;
			sqlwhere+=" and hr.status in (0,1,2,3)";

			// 筛选出比传入密级高或相等的人员
			if(!"".equals(classification)){
				sqlwhere += " and ( hr.classification in (select seclevel from UserClassification " +
						"where Optionalresourceseclevel like '%"+classification+"%') or hr.classification = "+classification +" ) ";
			}

			if(adci.isUseAppDetach()){
			String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
			String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
			sqlwhere+=tempstr;
			}
			if(sqlwhere.length()>0)sql+=sqlwhere;
			sql += "and exists (select * from hrmresourcevirtual where hr.id = resourceid and departmentid=" + departmentId + ") order by hr.dsporder ";

			writeLog("#1101 vir sql="+sql);

			RecordSet rs1 = new RecordSet();
			rs1.executeSql(sql);
			while (rs1.next()) {
				String id = rs1.getString("id");
				String lastname = rs1.getString("lastname");

				OrgBean orgBean = new OrgBean();
				orgBean.setId(id);
				orgBean.setPid(departmentId);
				orgBean.setName(lastname);
				orgBean.setType("3");
				orgBean.setIsVirtual("0");
				orgBean.setCanClick(true);
				orgBean.setIcon(resourceComInfo.getMessagerUrls(id));
				orgBean.setRequestParams(resourceComInfo.getUserIconInfoStr(id,user));
				orgBean.setTitle("");
				orgBean.setPsubcompanyid(pDepartment.getPsubcompanyid());
				pDepartment.setIsParent(true);

				subOrgs.add(orgBean);
			}
			pDepartment.setSubs(subOrgs);
		}catch (Exception e) {
			writeLog(e);
		}
	}
	
	/**
	 * 
	 * @param parentOrg
	 */
	private void validVirtualOrgIsParent(OrgBean parentOrg) {
		SubCompanyVirtualComInfo rs = null;
		try{
			rs = new SubCompanyVirtualComInfo();
			if("".equals(Util.null2String(parentOrg.getCompanyid()))){
				parentOrg.setCompanyid(rs.getCompanyid(parentOrg.getId()));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		rs.setTofirstRow();

		// 加载虚拟部门
		if (this.isLoadSubDepartment) {
			validVirtualOrgIsParent(parentOrg, null);
		}
		while (rs.next()) {
			String id = rs.getSubCompanyid();
			String supsubcomid = rs.getSupsubcomid();
			String comid = rs.getCompanyid();

			if (!comid.equals(parentOrg.getCompanyid()))
				continue;
			if (supsubcomid.equals(""))
				supsubcomid = "0";
			if (!supsubcomid.equals(parentOrg.getId()))
				continue;
			
			if (!this.showCanceled && "1".equals(rs.getCompanyiscanceled())){
				continue;
			}
			
			if (lsSubCom4Search!=null && !lsSubCom4Search.contains(id)){
				continue;
			}
			
			parentOrg.setIsParent(true);
			break;
		}
	}

	/**
	 * 
	 * @param pVCompany
	 * @param pVDepartment
	 */
	private void validVirtualOrgIsParent(OrgBean pVCompany, OrgBean pVDepartment) {
		DepartmentVirtualComInfo rsDepartment = null;
		try{
			rsDepartment = new DepartmentVirtualComInfo();
		}catch(Exception e){
			e.printStackTrace();
		}
		rsDepartment.setTofirstRow();

		String departmentId = pVDepartment == null ? "0" : pVDepartment.getId();
		String subcompanyid = pVCompany.getId();
		if("".equals(subcompanyid)){
			subcompanyid = rsDepartment.getSubcompanyid1(departmentId);
		}

		while (rsDepartment.next()) {
			String id = rsDepartment.getDepartmentid();
			
			if (departmentId.equals(rsDepartment.getDepartmentid()))
				continue;
			String supdepid = rsDepartment.getDepartmentsupdepid();
			if (departmentId.equals("0") && supdepid.equals(""))
				supdepid = "0";
			if (!(rsDepartment.getSubcompanyid1().equals(subcompanyid) && (supdepid.equals(departmentId) || (!rsDepartment.getSubcompanyid1(supdepid).equals(subcompanyid) && departmentId.equals("0")))))
				continue;

			if (!this.showCanceled && "1".equals(rsDepartment.getDeparmentcanceled())){
				continue;
			}
			
			if (lsDept4Search!=null && !lsDept4Search.contains(id)){
				continue;
			}
			
			pVCompany.setIsParent(true);
			if (pVDepartment != null) {
				pVDepartment.setIsParent(true);
			}

			break;
		}
		

		//判断部门下是否有人员
		if(this.isLoadUser && pVDepartment != null && !pVDepartment.getIsParent()){
			ResourceComInfo rsResource = null;
			try{
				rsResource = new ResourceComInfo();
				rsResource.setTofirstRow();
				while (rsResource.next()) {
					String str = rsResource.getDepartmentID();
					if (str.equals(departmentId)){
						pVDepartment.setIsParent(true);
						break;
					}
				}
			}catch (Exception e) {
				writeLog(e);
			}
		}

	}

	/**
	 * 获取下级机构
	 * 废弃 TODO jhy
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String,Object> getTreeNodeData(HttpServletRequest request, HttpServletResponse response) {
		Map<String,Object> apidatas = new HashMap<String,Object>();
		
		String type = Util.null2String(request.getParameter("type"));
		String id = Util.null2String(request.getParameter("id"));
		String psubcompanyid = Util.null2String(request.getParameter("psubcompanyid"));
		String isVirtual = Util.null2String(request.getParameter("isVirtual"),"0");
		
		OrgBean psubOrgBean = new OrgBean();
		List<TreeNode> result = null;
		if ("0".equals(type)) {
			String companyid = Util.null2String(request.getParameter("companyid"));
			psubOrgBean.setId(id);
			psubOrgBean.setCompanyid(companyid);

			if ("0".equals(isVirtual)) {
				loadSubCompanys(psubOrgBean);
			} else {
				loadVirtualSubCompanyInfo(psubOrgBean);
			}
			result = psubOrgBean.getSubs();
		} else if ("1".equals(type)) {
			psubOrgBean.setId(id);
			psubOrgBean.setType(type);
			psubOrgBean.setIsVirtual(isVirtual);
			if ("0".equals(isVirtual)) {
				loadSubCompanys(psubOrgBean);
			} else {
				loadVirtualSubCompanyInfo(psubOrgBean);
			}
			result = psubOrgBean.getSubs();
		} else if ("2".equals(type)) {
			psubOrgBean.setId(psubcompanyid);

			OrgBean pdepOrgBean = new OrgBean();
			pdepOrgBean.setId(id);
			pdepOrgBean.setIsVirtual(isVirtual);
			if ("0".equals(isVirtual)) {
				loadSubDepartments(psubOrgBean, pdepOrgBean);
			} else {
				loadVirtualSubDepartments(psubOrgBean, pdepOrgBean);
			}
			result = pdepOrgBean.getSubs();
		}
		apidatas.put("datas", result);
		return apidatas;
	}
	
	public String getAllParentsOrg(String id,String para){
		String[] paraArr = para.split("\\+");
		boolean isDept = "1".equals(paraArr[0]);
		boolean isVirtual = !"".equals(paraArr.length > 1 ? Util.null2String(paraArr[1]): "");
		String pname = getPName("",id,isDept,isVirtual);
		if(isVirtual){
			CompanyVirtualComInfo companyVirtualComInfo = new CompanyVirtualComInfo();
			pname = ("".equals(pname)?"":pname+"/") + companyVirtualComInfo.getVirtualType(Util.null2String(paraArr[1]));
		}else{
			CompanyComInfo companyComInfo = new CompanyComInfo();
			pname = ("".equals(pname)?"":pname+"/") + companyComInfo.getCompanyname("1");
		}
		return pname;
	}
	
	
	public String getPName(String pname,String id,boolean isDept,boolean isVirtual){
		if("0".equals(id)||"".equals(id)) return pname;
		String _pname = "";
		String _pid = "";
		if(isDept){
			if(isVirtual){
				DepartmentVirtualComInfo deptVirComInfo  = new DepartmentVirtualComInfo();
				_pname = deptVirComInfo.getDepartmentmark(id);
				if(this.showDepartmentFullName){
					_pname = deptVirComInfo.getDepartmentname(id);
				}
				isDept = !"0".equals(Util.null2s(deptVirComInfo.getDepartmentsupdepid(id), "0"));
				_pid = isDept?deptVirComInfo.getDepartmentsupdepid(id):deptVirComInfo.getSubcompanyid1(id);
			}else{
				DepartmentComInfo deptComInfo =  new DepartmentComInfo();
				_pname = deptComInfo.getDepartmentmark(id);
				if(this.showDepartmentFullName){
					_pname = deptComInfo.getDepartmentname(id);
				}
				isDept = !"0".equals(Util.null2s(deptComInfo.getDepartmentsupdepid(id), "0"));
				_pid = isDept?deptComInfo.getDepartmentsupdepid(id):deptComInfo.getSubcompanyid1(id);
			}
		}else{
			if(isVirtual){
				SubCompanyVirtualComInfo subVirComInfo = new SubCompanyVirtualComInfo();
				_pname = subVirComInfo.getSubCompanyname(id);
				if(this.showSubCompanyFullName){
					_pname = subVirComInfo.getSubCompanydesc(id);
				}
				_pid = Util.null2String(subVirComInfo.getSupsubcomid(id));
			}else{
				SubCompanyComInfo subComInfo = new SubCompanyComInfo();
				_pname = subComInfo.getSubCompanyname(id);
				if(this.showSubCompanyFullName){
					_pname = subComInfo.getSubCompanydesc(id);
				}
				_pid = Util.null2String(subComInfo.getSupsubcomid(id));
			}
		}
		pname = ("".equals(pname)?"":pname+"/") + _pname;
		return getPName(pname,_pid,isDept,isVirtual);
	}

	/**
	 * 查询需要展示的分部部门
	 * @param keyword
	 */
	private void getSubComDept4Search(String keyword){
		try{
			RecordSet rs = new RecordSet();
			SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
			SubCompanyVirtualComInfo subCompanyVirtualComInfo = new SubCompanyVirtualComInfo();
			DepartmentComInfo departmentComInfo = new DepartmentComInfo();
			DepartmentVirtualComInfo departmentVirtualComInfo = new DepartmentVirtualComInfo();
			
			lsSubCom4Search = new ArrayList<String>();
			lsDept4Search = new ArrayList<String>();
			String sql = "";
			String sqlShowCanceled = this.showCanceled?"":"and (canceled is null or canceled !='1')";
			if(this.isLoadSubDepartment){
				if(rs.getDBType().equalsIgnoreCase("oracle") || DialectUtil.isMySql(rs.getDBType())||rs.getDBType().equalsIgnoreCase("postgresql")){
					sql = "select id,subcompanyid1 from hrmdepartmentallview where 1=1 "+sqlShowCanceled+" and lower(convtomultilang(departmentmark,"+user.getLanguage()+")) like '%"+keyword+"%'";
				}else{
					sql = "select id,subcompanyid1 from hrmdepartmentallview where 1=1 "+sqlShowCanceled+" and lower(dbo.convtomultilang(departmentmark,"+user.getLanguage()+")) like '%"+keyword+"%'";
				}
				rs.executeSql(sql);
				while(rs.next()){
					String departmentid = rs.getString("id");
					String subcompanyid1 = rs.getString("subcompanyid1");
					lsSubCom4Search.add(subcompanyid1);
					lsDept4Search.add(departmentid);
					String supdeptids = "";//获得必要的上级
					if(rs.getInt("id")<0){
						supdeptids = departmentVirtualComInfo.getAllSupDepartment(departmentid);
					}else{
						supdeptids = departmentComInfo.getAllSupDepartment(departmentid);
					}
					if(supdeptids.length()>0){
						String[] tmpsupdeptids = Util.splitString(supdeptids, ",");
						
						for(int i=0;tmpsupdeptids!=null&&i<tmpsupdeptids.length;i++){
							if(Util.null2String(tmpsupdeptids[i]).length()==0)continue;
							lsDept4Search.add(tmpsupdeptids[i]);
						}
					}
				}
			}
			
			if(rs.getDBType().equalsIgnoreCase("oracle") || DialectUtil.isMySql(rs.getDBType())||rs.getDBType().equalsIgnoreCase("postgresql")){
				sql = "select id from hrmsubcompanyallview where 1=1 "+sqlShowCanceled+" and lower(convtomultilang(subcompanyname,"+user.getLanguage()+")) like '%"+keyword+"%'";
			}else{
				sql = "select id from hrmsubcompanyallview where 1=1 "+sqlShowCanceled+"  and lower(dbo.convtomultilang(subcompanyname,"+user.getLanguage()+")) like '%"+keyword+"%'";
			}
			rs.executeSql(sql);
			while(rs.next()){
				String subcompanyid = rs.getString("id");
				lsSubCom4Search.add(subcompanyid);
			}
			
			for(int i=0;lsSubCom4Search!=null&&i<lsSubCom4Search.size();i++){
				if(Util.null2String(lsSubCom4Search.get(i)).length()==0)continue;
				String subcompanyid = lsSubCom4Search.get(i);
				String supsubcomids = "";//获得必要的上级
				if(Util.getIntValue(subcompanyid)<0){
					supsubcomids = subCompanyVirtualComInfo.getAllSupCompany(subcompanyid);
				}else{
					supsubcomids = subCompanyComInfo.getAllSupCompany(subcompanyid);
				}
				if(supsubcomids.length()>0){
					String[] tmpsupsubcomids = Util.splitString(supsubcomids, ",");
					for(int j=0;tmpsupsubcomids!=null&&j<tmpsupsubcomids.length;j++){
						if(tmpsupsubcomids[j].length()==0)continue;
						lsSubCom4Search.add(tmpsupsubcomids[j]);
					}
				}
			}
			
		}catch (Exception e) {
			writeLog(e);
		}
	}
	
	private class OrgBean extends TreeNode{
		private String companyid;
		private String isVirtual;
		private String psubcompanyid;
		private String displayType;
		private boolean isCanceled;
		private String requestParams;//头像信息

		public String getCompanyid() {
			return companyid;
		}

		public void setCompanyid(String companyid) {
			this.companyid = companyid;
		}


		public String getIsVirtual() {
			return isVirtual;
		}

		public void setIsVirtual(String isVirtual) {
			this.isVirtual = isVirtual;
		}

		public String getPsubcompanyid() {
			return psubcompanyid;
		}

		public void setPsubcompanyid(String psubcompanyid) {
			this.psubcompanyid = psubcompanyid;
		}
		
		public boolean isCanceled() {
			return isCanceled;
		}

		public void setCanceled(boolean isCanceled) {
			this.isCanceled = isCanceled;
		}

		public String getDisplayType() {
			return displayType;
		}

		public void setDisplayType(String displayType) {
			this.displayType = displayType;
		}

		public String getRequestParams() {
			return requestParams;
		}

		public void setRequestParams(String requestParams) {
			this.requestParams = requestParams;
		}
	}


  public void getSubCompanyTreeListByRight(int userId, String rightStr) {

  	String strbelongtoids = Util.null2String(User.getBelongtoidsByUserId(userId));
  	if(strbelongtoids.length()==0){
      CheckSubCompanyRight newCheck=new CheckSubCompanyRight();
      newCheck.setShowCanceled(showCanceled);
      allSubComIds=newCheck.getSubComPathByUserRightId(userId,rightStr,0);
      subComIds=newCheck.getSubComByUserRightId(userId,rightStr);
      depIds=newCheck.geDeptPathByUserRightId(userId,rightStr,0);
  	}else{
  		//如果有次账号 包含次账号数据
  		strbelongtoids += ","+userId;
  		String[] belongtoids = Util.TokenizerString2(strbelongtoids,",");
  		String tmp_subcomids = "";
  		String tmp_subcomids1 = "";
  		String tmp_deptids = "";
      CheckSubCompanyRight newCheck=new CheckSubCompanyRight();
      newCheck.setShowCanceled(showCanceled);
  		for(String tmpuserid:belongtoids){
  			int iUserid = Util.getIntValue(tmpuserid);
  			allSubComIds = newCheck.getSubComPathByUserRightId(iUserid,rightStr,0);
        subComIds=newCheck.getSubComByUserRightId(iUserid,rightStr);
        depIds=newCheck.geDeptPathByUserRightId(userId,rightStr,0);
        
        for(int i=0;allSubComIds!=null&&i<allSubComIds.length;i++){
        	if(tmp_subcomids.length()>0)tmp_subcomids+=",";
        	tmp_subcomids += allSubComIds[i];
        }
        
        for(int i=0;subComIds!=null&&i<subComIds.length;i++){
        	if(tmp_subcomids1.length()>0)tmp_subcomids1+=",";
        	tmp_subcomids1 += subComIds[i];
        }
        
        for(int i=0;depIds!=null&&i<depIds.length;i++){
        	if(tmp_deptids.length()>0)tmp_deptids+=",";
        	tmp_deptids += depIds[i];
        }
        
  		}
  		List ls = Util.TokenizerString(tmp_subcomids, ",");
  		List ls1 = Util.TokenizerString(tmp_subcomids1, ",");
  		List ls2 = Util.TokenizerString(tmp_deptids, ",");
  		
  		//剔除重复
  		List tmp_ls = new ArrayList(); 
  		for(Object userid :ls){
  			if(!tmp_ls.contains(userid))tmp_ls.add(userid);
  		}
  		
  		List tmp_ls1 = new ArrayList(); 
  		for(Object userid :ls1){
  			if(!tmp_ls1.contains(userid))tmp_ls1.add(userid);
  		}
  		
  		List tmp_ls2 = new ArrayList(); 
  		for(Object userid :ls2){
  			if(!tmp_ls2.contains(userid))tmp_ls2.add(userid);
  		}
  		
  		allSubComIds = new int[tmp_ls.size()];
  		for(int i=0;i<allSubComIds.length;i++){
  			allSubComIds[i]=Util.getIntValue((String)tmp_ls.get(i));
  		}
  		
  		subComIds = new int[tmp_ls1.size()];
  		for(int i=0;i<subComIds.length;i++){
  			subComIds[i]=Util.getIntValue((String)tmp_ls1.get(i));
  		}
  		
  		depIds = new int[tmp_ls2.size()];
  		for(int i=0;i<depIds.length;i++){
  			depIds[i]=Util.getIntValue((String)tmp_ls2.get(i));
  		}
  	}

	  writeLog("allSubComIds:"+JSONObject.toJSONString(allSubComIds));
	  writeLog("subComIds:"+JSONObject.toJSONString(subComIds));
	  writeLog("depIds:"+JSONObject.toJSONString(depIds));
  }
  
  /**
   * 检查机构分权和应用分权
   * @param type
   * @param id
   * @return
   */
  private boolean checkDetach(String type, String id){
  	if(sysadmin) return true ;

  	boolean hasRight = true;
  	DepartmentComInfo departmentComInfo = new DepartmentComInfo();
  	if(type.equals("com")){
  		if(this.isDetach){//检查机构分权
  			hasRight = false;
  			if(this.allSubComIds !=null && this.allSubComIds.length>0){
	  			for(int i=0; i<this.allSubComIds.length; i++){
	  				if(id.equals(String.valueOf(this.allSubComIds[i]))){
	  					hasRight=true;
	  					break;
	  				}
	  			}
  			}
			if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
				for (int i = 0; i < this.allChildSubComIds.length; i++) {
					if (id.equals(String.valueOf(this.allChildSubComIds[i]))) {
						hasRight = true;
						break;
					}
				}
			}
  		}else{//检查应用分权
  			if (adci == null){
  				adci = new AppDetachComInfo(user);
  			}
  			if (adci.isUseAppDetach()) {
  				if (adci.checkUserAppDetach(id, "2") == 0){
  					hasRight=false;
  				}else{
  					hasRight=true;
  				}
			}else if(Util.null2String(this.rightStr).length() > 0){
				hasRight = false;
				if (this.allSubComIds != null && this.allSubComIds.length > 0) {
					for (int i = 0; i < this.allSubComIds.length; i++) {
						if (id.equals(String.valueOf(this.allSubComIds[i]))) {
							hasRight = true;
							break;
						}
					}
				}
			}
  		}
  	}else if(type.equals("dept")){
  		String subcompanyid = departmentComInfo.getSubcompanyid1(id);
			if(this.isDetach){//检查机构分权
				hasRight = false;
				if(this.depIds!=null && this.depIds.length>0){
				 for(int i=0; i<depIds.length; i++){
					 if(id.equals(String.valueOf(depIds[i]))){
						 hasRight = true;
					 }
				 }
				}
                if (!hasRight && this.subComIds != null && this.subComIds.length > 0) {
                    for (int i = 0; i < subComIds.length; i++) {
                        if (subcompanyid.equals(String.valueOf(subComIds[i]))) {
                            hasRight = true;
                        }
                    }
                }
                if (this.allChildDeptIds != null && this.allChildDeptIds.length > 0) {
                    for (int i = 0; i < this.allChildDeptIds.length; i++) {
                        if (id.equals(String.valueOf(this.allChildDeptIds[i]))) {
                            hasRight = true;
                            break;
                        }
                    }
                }
			}else{// 检查应用分权
				if (adci == null){
					adci = new AppDetachComInfo(user);
				}
				if (adci.isUseAppDetach()) {
					if (adci.checkUserAppDetach(id, "3") == 0){
						hasRight = false;
					}else{
						hasRight=true;
					}
				}else if(Util.null2String(this.rightStr).length() > 0){

					hasRight = false;
					if (this.depIds != null && this.depIds.length > 0) {
						for (int i = 0; i < depIds.length; i++) {
							if (id.equals(String.valueOf(depIds[i]))) {
								hasRight = true;
								break ;
							}
						}
					}else if(this.subComIds != null && this.subComIds.length > 0){
						for (int i = 0; i < subComIds.length; i++) {
							if (subcompanyid.equals(String.valueOf(subComIds[i]))) {
								hasRight = true;
								break ;
							}
						}
					}
				}
			}
  	}
  	return hasRight;
  }
  /**
   * 检查执行力分权，控制，无权限的人只能看到总部
   * @return
   */
  private boolean checkBlogAppSetAgain() {
	if(this.isExecutionDetach){
		return  true;
	}
	if (HrmUserVarify.checkUserRight(rightStr, user)) {
		return  true;
	}
	  return false;
  }
  /**
   * 检查微博管理分权未开启时的角色成员权限：702505
   * @param type
   * @param id
   * @return
   */
  private boolean checkBlogAppSetting(String type, String rightStr, String id) {
	  boolean hasRight = true;
	  if (rightStr.equals("blog:newappSetting") && !this.isBlogDetach) {
		  hasRight = false;
		  HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
		  int rolelevel = hrmCommonService.getMaxRoleLevel(this.user.getUID(), rightStr);
		  switch (rolelevel) {
			  case 0:
				  if (this.allSubComIds != null && this.allSubComIds.length > 0) {
					  for (int i = 0; i < this.allSubComIds.length; i++) {
						  if (id.equals(String.valueOf(this.allSubComIds[i]))) {
							  hasRight = true;
							  break;
						  }
					  }
				  }
				  break;
			  case 1:
				  if (this.allSubComIds != null && this.allSubComIds.length > 0) {
					  for (int i = 0; i < this.allSubComIds.length; i++) {
						  if (id.equals(String.valueOf(this.allSubComIds[i]))) {
							  hasRight = true;
							  break;
						  }
					  }
				  }
				  if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
					  for (int i = 0; i < this.allChildSubComIds.length; i++) {
						  if (id.equals(this.allChildSubComIds[i])) {
							  hasRight = true;
							  break;
						  }
					  }
				  }
				  break;
			  case 2:
				  hasRight = true;
				  break;
			  default:
				  hasRight = true;
				  break;
		  }
	  } else if ((rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet")) && !this.isExecutionDetach) {
		  DepartmentComInfo departmentComInfo = new DepartmentComInfo();
		  hasRight = false;
		  HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
		  int rolelevel = hrmCommonService.getMaxRoleLevel(this.user.getUID(), rightStr);
		  switch (rolelevel) {
			  case 0:
				  if (type.equals("com")) {
					  if (!hasRight && this.allSubComIds != null && this.allSubComIds.length > 0) {
						  for (int i = 0; i < allSubComIds.length; i++) {
							  if (id.equals(String.valueOf(allSubComIds[i]))) {
								  hasRight = true;
							  }
						  }
					  }
				  } else {
					  if (this.allChildDeptIds != null && this.allChildDeptIds.length > 0) {
						  for (int i = 0; i < this.allChildDeptIds.length; i++) {
							  if (id.equals(this.allChildDeptIds[i])) {
								  hasRight = true;
								  break;
							  }
						  }
					  }
				  }
				  break;
			  case 1:
				  if (type.equals("com")) {
					  if (this.allSubComIds != null && this.allSubComIds.length > 0) {
						  for (int i = 0; i < this.allSubComIds.length; i++) {
							  if (id.equals(String.valueOf(this.allSubComIds[i]))) {
								  hasRight = true;
								  break;
							  }
						  }
					  }
					  if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
						  for (int i = 0; i < this.allChildSubComIds.length; i++) {
							  if (id.equals(this.allChildSubComIds[i])) {
								  hasRight = true;
								  break;
							  }
						  }
					  }
				  } else {
					  String subcompanyid = departmentComInfo.getSubcompanyid1(id);
					  if (this.allChildSubComIds != null && this.allChildSubComIds.length > 0) {
						  for (int i = 0; i < this.allChildSubComIds.length; i++) {
							  if (subcompanyid.equals(this.allChildSubComIds[i])) {
								  hasRight = true;
								  break;
							  }
						  }
					  }
				  }
				  break;
			  case 2:
				  hasRight = true;
				  break;
			  default:
				  hasRight = true;
				  break;
		  }
	  }
	  return hasRight;
  }
  /**
   * 检查微博管理分权未开启时的角色成员权限,
   * @param rightStr
   * @return
   */
  private void checkBlogAppLoadSub(String rightStr) {
		  if (!this.isBlogDetach &&rightStr.equals("blog:newappSetting")) {
				  HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
				  int rolelevel = hrmCommonService.getMaxRoleLevel(this.user.getUID(), rightStr);
				  switch (rolelevel) {
					  case 0:
						  this.isLoadAllSub = false;
						  break;
					  case 1:
						  this.isLoadAllSub = true;
						  int usersubcom = this.user.getUserSubCompany1();
						  String idss = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(usersubcom),String.valueOf(usersubcom));
						  String[] result1 = idss.split(",");
						  this.allChildSubComIds = result1;
						  break;
					  default:
						  break;
				  }
		  } else if (!this.isExecutionDetach && (rightStr.equals("Execution:PerformanceSet") || rightStr.equals("Execution:ReportSet"))) {
				  HrmCommonService hrmCommonService = new HrmCommonServiceImpl();
				  int rolelevel = hrmCommonService.getMaxRoleLevel(this.user.getUID(), rightStr);
			       writeLog("HrmTreeService>>>rolelevel:"+rolelevel);
				  switch (rolelevel) {
					  case 0:
						  this.isLoadAllSub = true;
						  int userdepart = this.user.getUserDepartment();
						  String detidss = null;
						  try {
							  detidss = DepartmentComInfo.getAllChildDepartId(String.valueOf(userdepart), String.valueOf(userdepart));
						  } catch (Exception e) {
							  writeLog(e);
						  }
						  String[] result0 = detidss.split(",");
						  this.allChildDeptIds = result0;
						  break;
					  case 1:
						  this.isLoadAllSub = true;
						  int usersubcom = this.user.getUserSubCompany1();
						  String idss = SubCompanyComInfo.getAllChildSubcompanyId(String.valueOf(usersubcom), String.valueOf(usersubcom));
						  String[] result1 = idss.split(",");
						  this.allChildSubComIds = result1;
						  break;
					  case 2:
					  	 ManageDetachComInfo ma=new ManageDetachComInfo();//解决QC935048问题
						 Boolean bool =ma.getDetachable().equals("1");
						 if(bool){
							 sysadmin = true;
						 }
					  default:
						  break;
				  }
		  }
	  }

}
