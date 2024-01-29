package com.api.hrm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.api.hrm.bean.TreeNode;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.resource.ResourceComInfo;

public class HrmResourceTreeService extends BaseBean{
	private AppDetachComInfo adci = null;
	private User user = null;
	private String alllevel = null;//是否显示所有下级
	private String isNoAccount = null;// 是否显示无账号人员
	public Map<String, Object> getHrmResourceTree(HttpServletRequest request, HttpServletResponse response) {
		User user = HrmUserVarify.getUser(request, response);
		TreeNode manager = new TreeNode();
		Map<String,Object> apidatas = new HashMap<String,Object>();
		Map<String, Object> resultDatas = new HashMap<String, Object>();
		try{
			adci = new AppDetachComInfo(user);
			this.user = user;
			String id = Util.null2String(request.getParameter("id"));
			String rootid = Util.null2String(request.getParameter("rootid"));
			if(id.length()>0){
				rootid = "";
			}
			alllevel = Util.null2String(request.getParameter("alllevel"));//是否显示所有下级
			isNoAccount = Util.null2String(request.getParameter("isNoAccount"));// 是否显示无账号人员
			if(id.length()==0||rootid.length()>0){
				String userid = ""+user.getUID();
				String username = user.getLastname();
				if(rootid.length()>0){
					ResourceComInfo rci = new ResourceComInfo();
					userid = rootid;
					username = rci.getLastname(userid);
				}
				manager.setId(userid);
				manager.setName(username);
				manager.setType("1");
				manager.setCanClick(true);
				List<TreeNode> result = getChildTreeNode(manager).getSubs();
				if(result!=null && result.size()>0){
					manager.setIsParent(true);
				}
				resultDatas.put("rootManager", manager);
				apidatas.put("datas", resultDatas);
			}else{
				manager.setId(id);
				List<TreeNode> result = getChildTreeNode(manager).getSubs();
				apidatas.put("datas", result);
			}
		}catch (Exception e) {
			writeLog(e);
		}
		
		return apidatas;
	}

	private TreeNode getChildTreeNode(TreeNode manager){
		List<TreeNode> childNode = null;
		RecordSet rs = new RecordSet();
		String sql = "";
		String sqlwhere = "";
		try{
			childNode = new ArrayList<TreeNode>();
			String id = manager.getId();
			
			if(id.length()>0){
				sql = "select id, lastname from hrmresource hr where managerid = "+id;
				if(sqlwhere.length()>0)sqlwhere=" and " +sqlwhere;
				sqlwhere+=" and hr.status in (0,1,2,3)";
				
			 	if(!isNoAccount.equals("1")){
			 		sqlwhere +=" and loginid is not null "+(rs.getDBType().equals("oracle")?"":" and loginid<>'' ");
			 	}
				if(adci.isUseAppDetach()){
					String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
					String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
					sqlwhere+=tempstr;
				}
				if(sqlwhere.length()>0)sql+=sqlwhere;
				sql += " order by hr.dsporder ";
				new BaseBean().writeLog("==zj==(考勤左侧树sql)" + sql);
				rs.executeSql(sql);
				while(rs.next()){
					TreeNode orgBean = new TreeNode();
					if(id.equals(rs.getString("id")))continue;
					orgBean.setId(rs.getString("id"));
					orgBean.setName(rs.getString("lastname"));
					orgBean.setType("1");
					orgBean.setCanClick(true);
					orgBean.setIsParent(hasChild(rs.getString("id")));
					childNode.add(orgBean);
				}

				//==zj 这里将子账号也添加进去
				String childSql = "select * from hrmresource where belongto = "+id;
				rs.executeQuery(childSql);
				while (rs.next()){
					TreeNode orgBean = new TreeNode();
					orgBean.setId(rs.getString("id"));
					orgBean.setName(rs.getString("lastname"));
					orgBean.setType("1");
					orgBean.setCanClick(true);
					childNode.add(orgBean);
				}
			}
			manager.setSubs(childNode);
		}catch (Exception e) {
			writeLog(e);
		}
		return manager; 
	}
	
	private boolean hasChild(String id){
		RecordSet rs = new RecordSet();
		String sql = "";
		String sqlwhere = "";
		boolean isParent = false;
		try{
			if(id.length()>0){
				sql = "select count(1) from hrmresource hr where managerid = "+id;
				if(sqlwhere.length()>0)sqlwhere=" and " +sqlwhere;
				sqlwhere+=" and hr.status in (0,1,2,3)";
				
			 	if(!isNoAccount.equals("1")){
			 		sqlwhere +=" and loginid is not null "+(rs.getDBType().equals("oracle")?"":" and loginid<>'' ");
			 	}
				if(adci.isUseAppDetach()){
					String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
					String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
					sqlwhere+=tempstr;
				}
				if(sqlwhere.length()>0)sql+=sqlwhere;
				//sql += " order by hr.dsporder ";
				rs.executeSql(sql);
				if(rs.next()){
					if(rs.getInt(1)>0){
						isParent = true;
					}
				}
			}
		}catch (Exception e) {
			writeLog(e);
		}
		return isParent; 
	}
}
