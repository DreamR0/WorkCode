package com.api.hrm.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.browser.bean.SearchConditionItem;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;

import com.api.hrm.util.ServiceUtil;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.LogUtil;
import com.engine.common.util.ParamUtil;
import com.engine.workflow.biz.SecondAuthBiz;
import com.engine.workflow.constant.SecondAuthType;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.PasswordUtil;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.common.DbFunctionUtil;
import weaver.hrm.common.database.dialect.DialectUtil;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.finance.SalaryComInfo;
import weaver.hrm.resource.HrmListValidate;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.rsa.security.RSA;
import weaver.systeminfo.SysMaintenanceLog;
import weaver.systeminfo.SystemEnv;

/***
 * 薪资福利
 * @author lvyi
 *
 */
public class HrmFinanceService extends BaseBean {
	
	public String getTabInfo(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		User user = HrmUserVarify.getUser(request, response);
		try {
			List<Map<String,Object>> tabs = new ArrayList<Map<String,Object>>();
			Map<String,Object> tab = null;
			HrmListValidate HrmListValidate = new HrmListValidate();
			int idx = 0;
			
			idx++;
      if(HrmListValidate.isValidate(61)){
    		tab = new HashMap<String, Object>();
				tab.put("key", idx);
				tab.put("title", SystemEnv.getHtmlLabelName(32653,user.getLanguage()));
				tabs.add(tab);
      }
      
      idx++;
      if(HrmListValidate.isValidate(62)){
    		tab = new HashMap<String, Object>();
				tab.put("key", idx);
				tab.put("title", SystemEnv.getHtmlLabelName(19576,user.getLanguage()));
				tabs.add(tab);
      }
      
      idx++;
      if(HrmListValidate.isValidate(63)){
  			tab = new HashMap<String, Object>();
				tab.put("key", idx);
				tab.put("title", SystemEnv.getHtmlLabelName(32656,user.getLanguage()));
				tabs.add(tab);
      }
			
			retmap.put("status", "1");
			retmap.put("tabs", tabs);
		} catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
		return JSONObject.toJSONString(retmap);
	}
	
	/**
	 * 薪资福利
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public String getHrmFinanceFields(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		List<Object> lsGroup = new ArrayList<Object>();
		Map<String,Object> groupitem = null;
		List<Object> itemlist = null;
		User user = HrmUserVarify.getUser(request, response);
		try {
			String id = Util.null2String(request.getParameter("id"));
			String viewAttr = Util.null2String(request.getParameter("viewAttr"));
			if(id.length()==0){
				id = ""+user.getUID();
			}
			
			HrmListValidate HrmListValidate = new HrmListValidate();
			boolean ishe = (user.getUID() == Util.getIntValue(id));
			boolean ishasF =HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit",user)||isHasRight(user);
		 	if(!ishasF && !ishe){
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
		 	}

			if (ishasF) {
				Map<String, Object> buttons = new Hashtable<String, Object>();
				buttons.put("hasEdit", true);
				buttons.put("hasSave", true);
				retmap.put("buttons", buttons);
			}
		 	
		  String sql = "";
		  RecordSet rs = new RecordSet();
		  boolean hasFieldvalue = false;
		  sql = "select * from HrmResource where id = "+id;  
		  rs.executeSql(sql);
		  if(viewAttr.equals("2")){
		  	rs.isReturnDecryptData(true);
			}
		  if(rs.next()){
		  	hasFieldvalue = true;
		  }
		  
			//工资账号户名、工资银行	、工资账号、公积金帐户
			String[] fields = new String[]{"accountname,83353,1,1","bankid1,15812,3,284",
																		 "accountid1,16016,1,1","accumfundaccount,16085,1,1"};
			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			SearchConditionItem searchConditionItem = null;
			HrmFieldBean hrmFieldBean = null;
			itemlist = new ArrayList<Object>();
			groupitem = new HashMap<String,Object>();
			groupitem.put("title", SystemEnv.getHtmlLabelName(15805, user.getLanguage()));
			groupitem.put("defaultshow", true);
			groupitem.put("isHide", !HrmListValidate.isValidate(59));
			for(int i=0;i<fields.length;i++){
				String[] fieldinfo = fields[i].split(",");
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname(fieldinfo[0]);
				hrmFieldBean.setFieldlabel(fieldinfo[1]);
				hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
				hrmFieldBean.setType(fieldinfo[3]);
				hrmFieldBean.setFieldvalue(hasFieldvalue?rs.getString(fieldinfo[0]):"");
				hrmFieldBean.setIsFormField(true);
				searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
				if(searchConditionItem.getBrowserConditionParam()!=null){
					searchConditionItem.getBrowserConditionParam().setViewAttr(1);
				}
				searchConditionItem.setViewAttr(1);
				itemlist.add(searchConditionItem);
			}
			groupitem.put("items", itemlist);
			lsGroup.add(groupitem);
			retmap.put("fieldgroup", lsGroup);
			retmap.put("hrmId", id);
		} catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
		}
		return JSONObject.toJSONString(retmap);
	}
	
  /**
	 * 编辑薪酬工资
	 * @param request
	 * @param response
	 * @return
	 */
	public String save(HttpServletRequest request, HttpServletResponse response){
		Map<String,Object> retmap = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser(request, response);
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			boolean canEdit =HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit",user);
			if(isHasRight(user))canEdit = true;
			if(!canEdit){
				retmap.put("status", "-1");
				retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
				return JSONObject.toJSONString(retmap);
			}

			String id = Util.null2String(request.getParameter("id"));
      int userid = user.getUID();
			if(id.length()==0){
				id = ""+userid;
			}

			SimpleBizLogger logger = new SimpleBizLogger();
			Map<String, Object> params = ParamUtil.request2Map(request);
			BizLogContext bizLogContext = new BizLogContext();
			bizLogContext.setLogType(BizLogType.HRM);//模块类型
			bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD);//所属大类型
			bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_RSOURCE_CARD_FINANCE);//当前小类型
			bizLogContext.setParams(params);//当前request请求参数
			logger.setUser(user);//当前操作人
			String mainSql = "select * from HrmResource where id="+id;
			logger.setMainSql(mainSql,"id");//主表sql
			logger.setMainPrimarykey("id");//主日志表唯一key
			logger.setMainTargetNameColumn("lastname");//当前targetName对应的列（对应日志中的对象名）
			logger.before(bizLogContext);//写入操作前日志

      String bankid1 = StringUtil.vString(Util.null2String(request.getParameter("bankid1")),"0");
      String accountid1 = Util.null2String(request.getParameter("accountid1"));
      String accumfundaccount = Util.null2String(request.getParameter("accumfundaccount"));
      String accountname = Util.null2String(request.getParameter("accountname"));

      String para = "";
      Calendar todaycal = Calendar.getInstance ();
      String today = Util.add0(todaycal.get(Calendar.YEAR), 4) +"-"+
                     Util.add0(todaycal.get(Calendar.MONTH) + 1, 2) +"-"+
                     Util.add0(todaycal.get(Calendar.DAY_OF_MONTH) , 2) ;
      char separator = Util.getSeparator() ;
      RecordSet rs = new RecordSet();
//      para = ""+id+	separator+bankid1+separator+accountid1+separator+accumfundaccount+separator+accountname;
//      String userpara = ""+userid+separator+today;
			//rs.executeProc("HrmResourceFinanceInfo_Insert",para);
			List<Object> paramsObj = new ArrayList<>();
			paramsObj.add(bankid1);
			paramsObj.add(accountid1);
			paramsObj.add(accumfundaccount);
			paramsObj.add(accountname);
			paramsObj.add(id);
			rs.executeUpdate("UPDATE HrmResource SET bankid1= ? , accountid1= ? , accumfundaccount= ? , accountname= ? WHERE id= ?",paramsObj);
			rs.executeUpdate("update HrmResource set "+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID())+" where id=?",id) ;
			rs.executeUpdate("update HrmResourceManager set "+ DbFunctionUtil.getUpdateSetSql(rs.getDBType(),user.getUID())+" where id=?",id) ;
			rs.executeUpdate("update HrmInfoStatus set status = 1 where itemid = 2 and hrmid = ?",id);
			LogUtil.writeBizLog(logger.getBizLogContexts());

			retmap.put("status", "1");
		}catch (Exception e) {
			writeLog("编辑编辑薪酬工资错误："+e);
			retmap.put("status", "-1");
			retmap.put("message", ""+ SystemEnv.getHtmlLabelName(22620,weaver.general.ThreadVarLanguage.getLang())+"");
		}
		return JSONObject.toJSONString(retmap);
	}
	
	/***
	 * 最新工资单
	 * @param request
	 * @param response
	 * @return
	 */
	public String getHrmResourceSalaryLog(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			User user = HrmUserVarify.getUser(request, response);
			HrmListValidate HrmListValidate = new HrmListValidate();
			if (!HrmListValidate.isValidate(61)) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
			}
			String id = Util.null2String(request.getParameter("id"));
			if(id.length()==0){
				id = ""+user.getUID();
			}
			DecimalFormat df = new DecimalFormat("0.00");
			boolean ishe = (user.getUID() == Util.getIntValue(id));
			boolean ishasF =HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit",user)||isHasRight(user);
		 	if(!ishasF && !ishe){
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
		 	}
			DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			RecordSet rs = new RecordSet();
			SalaryComInfo SalaryComInfo = new SalaryComInfo();
			ArrayList itemlist = new ArrayList();
			ArrayList salaryitems = new ArrayList();
			ArrayList salarys = new ArrayList();
			int payid = 0;
			String yearmonth = "";
			String yearmonthsql = "select distinct(max(p.paydate)) yearmonth from Hrmsalarypaydetail d left join Hrmsalarypay p on d.payid=p.id  where hrmid='" + id + "' and sent=1 ";
			rs.executeSql(yearmonthsql);
			if (rs.next()) {
				yearmonth = Util.null2String(rs.getString("yearmonth"));
			}

			String sql = "select a.id,b.itemid ,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b,hrmsalaryitem c where a.id=b.payid and REPLACE(REPLACE(b.itemid,'_1',''),'_2','')=convert(varchar,c.id) and c.isshow='1' and b.sent=1 and a.paydate='" + yearmonth + "' and b.hrmid = " + id + " order by c.showorder,b.itemid";
			if ("oracle".equalsIgnoreCase(rs.getDBType())) {
				sql = "select a.id,b.itemid ,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b,hrmsalaryitem c where a.id=b.payid and to_number(REPLACE(REPLACE(b.itemid,'_1',''),'_2',''))=c.id and c.isshow='1' and b.sent=1 and a.paydate='" + yearmonth + "' and b.hrmid = " + id + " order by c.showorder,b.itemid";
			} else if (DialectUtil.isMySql(rs.getDBType())) {
				sql = "select a.id,b.itemid ,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b,hrmsalaryitem c where a.id=b.payid and REPLACE(REPLACE(b.itemid,'_1',''),'_2','')=convert(c.id,char) and c.isshow='1' and b.sent=1 and a.paydate='" + yearmonth + "' and b.hrmid = " + id + " order by c.showorder,b.itemid";
			}
			rs.executeSql(sql);
			rs.writeLog("HrmFinanceService:sql1:"+sql);
			while (rs.next()) {
				if (payid == 0)
					payid = rs.getInt("id");
				String itemid = rs.getString("itemid");
				String salary = rs.getString("salary");
				if (salaryitems.indexOf(itemid) < 0) {
					salaryitems.add(itemid);
					salarys.add(salary);
				}
			}
			sql = "select a.id,b.itemid ,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b where a.id=b.payid and b.sent=1 and a.paydate='" + yearmonth + "'and b.hrmid = " + id + " and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') not in(select convert(varchar,id) from hrmsalaryitem) order by b.itemid";
			if ("oracle".equalsIgnoreCase(rs.getDBType())) {
				sql = "select a.id,b.itemid ,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b where a.id=b.payid and b.sent=1 and a.paydate='" + yearmonth + "'and b.hrmid = " + id + " and to_number(REPLACE(REPLACE(b.itemid,'_1',''),'_2','')) not in(select id from hrmsalaryitem) order by b.itemid";
			} else if (DialectUtil.isMySql(rs.getDBType())) {
				sql = "select a.id,b.itemid ,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b where a.id=b.payid and b.sent=1 and a.paydate='" + yearmonth + "'and b.hrmid = " + id + " and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') not in(select convert(id,char) from hrmsalaryitem) order by b.itemid";
			}
			rs.executeSql(sql);
			rs.writeLog("HrmFinanceService:sql2:"+sql);
			while (rs.next()) {
				if (payid == 0)
					payid = rs.getInt("id");
				String itemid = rs.getString("itemid");
				String salary = rs.getString("salary");
				if (salaryitems.indexOf(itemid) < 0) {
					salaryitems.add(itemid);
					salarys.add(salary);
				}
			}
			if (salaryitems.size() < 1) {
				itemlist = SalaryComInfo.getSubCompanySalary(Util.getIntValue(DepartmentComInfo.getSubcompanyid1(ResourceComInfo.getDepartmentID(id))));
			} else {
				itemlist = salaryitems;
			}
			Map<String, Object> table = new HashMap<String, Object>();
			List<Object> columns = new ArrayList<Object>();
			Map<String, Object> column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(97, user.getLanguage()));
			column.put("dataIndex", "date");
			columns.add(column);
			for (int i = 0; i < itemlist.size(); i++) {
				String itemid = (String) itemlist.get(i);
				if (itemid.indexOf("_") > -1)
					itemid = itemid.substring(0, itemid.indexOf("_"));
				String itemname = SalaryComInfo.getSalaryname(itemid);
				String itemtype = SalaryComInfo.getSalaryItemtype(itemid);
				if (!itemtype.equals("9")) {
					column = new HashMap<String, Object>();
					column.put("title", itemname);
					column.put("dataIndex", "column_"+i);
					columns.add(column);
				} else {
					column = new HashMap<String, Object>();
					column.put("title", itemname + "(" + SystemEnv.getHtmlLabelName(6087, user.getLanguage()) + ")");
					column.put("dataIndex", "column_"+i);
					columns.add(column);
					i++;
					column = new HashMap<String, Object>();
					column.put("title", itemname + "(" + SystemEnv.getHtmlLabelName(1851, user.getLanguage()) + ")");
					column.put("dataIndex", "column_"+i);
					columns.add(column);
				}
			}
			List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
			if (salaryitems.size() > 0) {
				Map<String, Object> data = new HashMap<String, Object>();
				Map<String, Object> value = null;
				data.put("date", yearmonth);
				for (int j = 0; j < itemlist.size(); j++) {
					String itemid = (String) itemlist.get(j);
					String titles = "";
					boolean iscaltype = false;

					int salaryindex = salaryitems.indexOf(itemid);
					String salary = "0.00";
					if (salaryindex > -1) {
						salary = (String) salarys.get(salaryindex);
					}
					if (itemid.indexOf("_") < 0) {
						if (SalaryComInfo.getSalaryItemtype(itemid).equals("4")) {
							iscaltype = true;
						}
					} else {
						iscaltype = true;
					}
					if (iscaltype) {
						titles = SalaryComInfo.getTitles(Util.getIntValue(id), itemid, payid, user.getLanguage(), yearmonth);
					}
					value = new HashMap<String, Object>();
					value.put("titles", titles);
					value.put("value", ""+df.format(Util.getFloatValue(salary, 0)));
					data.put("column_"+j, value);
				}
				datas.add(data);
			}
			writeLog("Finance:columns:"+ JSON.toJSONString(columns));
			table.put("columns", columns);
			table.put("datas", datas);
			retmap.put("table", table);
		} catch (Exception e) {
			retmap.put("api_status", false);
			retmap.put("api_errormsg", e.getMessage());
			e.printStackTrace();
		}
		return JSONObject.toJSONString(retmap);
	}
	
	/**
	 * 历史工资单
	 * @param request
	 * @param response
	 * @return
	 */
	public String HrmResourceSalaryList(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			User user = HrmUserVarify.getUser(request, response);
			Map<String, Object> table = new HashMap<String, Object>();
			List<Object> columns = new ArrayList<Object>();
			Map<String, Object> column = null;
			List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
			Map<String, Object> data = null;
			Map<String, Object> value = null;
			DecimalFormat df = new DecimalFormat("0.00");

			HrmListValidate HrmListValidate = new HrmListValidate();
			if (!HrmListValidate.isValidate(62)) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
			}
			String id = Util.null2String(request.getParameter("id"));
			if (id.length() == 0) {
				id = "" + user.getUID();
			}
			boolean ishe = (user.getUID() == Util.getIntValue(id));
			boolean ishasF =HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit",user)||isHasRight(user);
		 	if(!ishasF && !ishe){
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
		 	}
			DepartmentComInfo DepartmentComInfo = new DepartmentComInfo();
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			RecordSet rs = new RecordSet();
			SalaryComInfo SalaryComInfo = new SalaryComInfo();
			RecordSet rs1 = new RecordSet();
			ArrayList itemlist = new ArrayList();
			ArrayList paydatelist = new ArrayList();
			ArrayList payidlist = new ArrayList();
			String sql = "select a.id,a.paydate,b.itemid,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b,hrmsalaryitem c where a.id=b.payid and REPLACE(REPLACE(b.itemid,'_1',''),'_2','')=convert(varchar,c.id) and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') in(select convert(varchar,id) from HrmSalaryItem) and c.isshow='1' and b.sent=1 and b.hrmid = " + id + " order by a.paydate desc,c.showorder,b.itemid";
			if ("oracle".equalsIgnoreCase(rs.getDBType())) {
				sql = "select a.id,a.paydate,b.itemid,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b,hrmsalaryitem c where a.id=b.payid and to_number(REPLACE(REPLACE(b.itemid,'_1',''),'_2',''))=c.id and to_number(REPLACE(REPLACE(b.itemid,'_1',''),'_2','')) in(select id from HrmSalaryItem) and c.isshow='1' and b.sent=1 and b.hrmid = " + id + " order by a.paydate desc,c.showorder,b.itemid";
			} else if (DialectUtil.isMySql(rs.getDBType())) {
				sql = "select a.id,a.paydate,b.itemid,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b,hrmsalaryitem c where a.id=b.payid and REPLACE(REPLACE(b.itemid,'_1',''),'_2','')=convert(c.id,char) and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') in(select convert(id,char) from HrmSalaryItem) and c.isshow='1' and b.sent=1 and b.hrmid = " + id + " order by a.paydate desc,c.showorder,b.itemid";
			}
			rs.executeSql(sql);
			while (rs.next()) {
				String payid = rs.getString("id");
				String paydate = rs.getString("paydate");
				String tmpitemid = rs.getString("itemid");
				if (paydatelist.indexOf(paydate) < 0) {
					paydatelist.add(paydate);
					payidlist.add(payid);
				}
				if (itemlist.indexOf(tmpitemid) == -1) {
					itemlist.add(tmpitemid);
				}
			}
			sql = "select a.id,a.paydate,b.itemid,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b where a.id=b.payid and b.sent=1 and b.hrmid = " + id + " and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') not in(select convert(varchar,id) from hrmsalaryitem) and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') in(select convert(varchar,id) from HrmSalaryItem)  order by a.paydate desc,b.itemid";
			if ("oracle".equalsIgnoreCase(rs.getDBType())) {
				sql = "select a.id,a.paydate,b.itemid,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b where a.id=b.payid and b.sent=1 and b.hrmid = " + id + " and to_number(REPLACE(REPLACE(b.itemid,'_1',''),'_2','')) not in(select id from hrmsalaryitem) and to_number(REPLACE(REPLACE(b.itemid,'_1',''),'_2','')) in(select id from HrmSalaryItem) order by a.paydate desc,b.itemid";
			} else if (DialectUtil.isMySql(rs.getDBType())) {
				sql = "select a.id,a.paydate,b.itemid,b.salary from HrmSalaryPay a,HrmSalaryPaydetail b where a.id=b.payid and b.sent=1 and b.hrmid = " + id + " and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') not in(select convert(id,char) from hrmsalaryitem) and REPLACE(REPLACE(b.itemid,'_1',''),'_2','') in(select convert(id,char) from HrmSalaryItem)  order by a.paydate desc,b.itemid";
			}
			rs1.executeSql(sql);
			while (rs1.next()) {
				String payid = rs1.getString("id");
				String paydate = rs1.getString("paydate");
				String tmpitemid = rs1.getString("itemid");
				if (paydatelist.indexOf(paydate) < 0) {
					paydatelist.add(paydate);
					payidlist.add(payid);
				}
				if (itemlist.indexOf(tmpitemid) == -1) {
					itemlist.add(tmpitemid);
				}
			}
			if (itemlist.size() < 1) {
				itemlist = SalaryComInfo.getSubCompanySalary(Util.getIntValue(DepartmentComInfo.getSubcompanyid1(ResourceComInfo.getDepartmentID(id))));
			}
			int itemnums = itemlist.size();
			ArrayList[] itemsalarylist = new ArrayList[itemnums];
			for (int i = 0; i < itemnums; i++) {
				itemsalarylist[i] = new ArrayList();
				for (int j = 0; j < paydatelist.size(); j++) {
					itemsalarylist[i].add(j, "");
				}
			}
			rs.beforFirst();
			while (rs.next()) {
				String paydate = rs.getString("paydate");
				String tmpitemid = rs.getString("itemid");
				String salary = rs.getString("salary");
				int paydateindx = paydatelist.indexOf(paydate);
				if (paydateindx > -1) {
					if (itemlist.indexOf(tmpitemid) > -1) {
						itemsalarylist[itemlist.indexOf(tmpitemid)].set(paydateindx, salary);
					}
				}
			}
			rs1.beforFirst();
			while (rs1.next()) {
				String paydate = rs1.getString("paydate");
				String tmpitemid = rs1.getString("itemid");
				String salary = rs1.getString("salary");
				int paydateindx = paydatelist.indexOf(paydate);
				if (paydateindx > -1) {
					if (itemlist.indexOf(tmpitemid) > -1) {
						itemsalarylist[itemlist.indexOf(tmpitemid)].set(paydateindx, salary);
					}
				}
			}
			column = new HashMap<String, Object>();
			column.put("title", SystemEnv.getHtmlLabelName(97, user.getLanguage()));
			column.put("dataIndex", "column_" + 0);
			columns.add(column);
			for (int i = 0; i < itemlist.size(); i++) {
				String itemid = (String) itemlist.get(i);
				if (itemid.indexOf("_") > -1)
					itemid = itemid.substring(0, itemid.indexOf("_"));
				String itemname = SalaryComInfo.getSalaryname(itemid);
				String itemtype = SalaryComInfo.getSalaryItemtype(itemid);
				if (!itemtype.equals("9")) {
					column = new HashMap<String, Object>();
					column.put("title", itemname);
					column.put("dataIndex", "column_" + (i + 1));
					columns.add(column);
				} else {
					column = new HashMap<String, Object>();
					column.put("title", itemname + "(" + SystemEnv.getHtmlLabelName(6087, user.getLanguage()) + ")");
					column.put("dataIndex", "column_" + (i + 1));
					columns.add(column);
					i++;
					column = new HashMap<String, Object>();
					column.put("title", itemname + "(" + SystemEnv.getHtmlLabelName(1851, user.getLanguage()) + ")");
					column.put("dataIndex", "column_" + (i + 1));
					columns.add(column);
				}
			}
			
			for (int i = 0; i < paydatelist.size(); i++) {
				data = new HashMap<String, Object>();
				data.put("column_0", paydatelist.get(i));
				for (int j = 0; j < itemlist.size(); j++) {
					String itemid = (String) itemlist.get(j);
					String titles = "";
					boolean iscaltype = false;
					String salary = (String) itemsalarylist[j].get(i);
					if (itemid.indexOf("_") < 0) {
						if (SalaryComInfo.getSalaryItemtype(itemid).equals("4")) {
							iscaltype = true;
						}
					} else {
						iscaltype = true;
					}
					if (iscaltype) {
						titles = SalaryComInfo.getTitles(Util.getIntValue(id), itemid, Util.getIntValue((String) payidlist.get(i)), user.getLanguage(), (String) paydatelist.get(i));
					}
					value = new HashMap<String, Object>();
					value.put("titles", titles);
					value.put("value", ""+df.format(Util.getFloatValue(salary, 0)));
					data.put("column_" + (j + 1), value);
				}
				datas.add(data);
			}

			table.put("columns", columns);
			table.put("datas", datas);
			retmap.put("table", table);
		} catch (Exception e) {
			retmap.put("api_status", false);
			retmap.put("api_errormsg", e.getMessage());
			e.printStackTrace();
		}
		return JSONObject.toJSONString(retmap);
	}	
	
	/**
	 * 工资调整记录
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public String HrmResourceChangeLog(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try {
			User user = HrmUserVarify.getUser(request, response);
			HrmListValidate HrmListValidate = new HrmListValidate();
			if (!HrmListValidate.isValidate(63)) {
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
			}
			String id = Util.null2String(request.getParameter("id"));
			if(id.length()==0){
				id = ""+user.getUID();
			}
			boolean ishe = (user.getUID() == Util.getIntValue(id));
			boolean ishasF =HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit",user)||isHasRight(user);
		 	if(!ishasF && !ishe){
				retmap.put("status", "-1");
				retmap.put("message", SystemEnv.getHtmlLabelName(2012,user.getLanguage()));
				return JSONObject.toJSONString(retmap);
		 	}
			RecordSet rs = new RecordSet();
			SalaryComInfo SalaryComInfo = new SalaryComInfo();
			ResourceComInfo ResourceComInfo = new ResourceComInfo();
			Map<String, Object> table = new HashMap<String, Object>();
			List<Object> columns = new ArrayList<Object>();
			Map<String, Object> column = null;
			List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
			Map<String, Object> data = null;

			String[] titles = new String[] { "15819", "15820", "19603", "15821", "15822", "19604", "1897", "15823" };
			for (int i = 0; i < titles.length; i++) {
				column = new HashMap<String, Object>();
				column.put("title", SystemEnv.getHtmlLabelNames(titles[i], user.getLanguage()));
				column.put("dataIndex", "column_" + i);
				columns.add(column);
			}

			rs.executeSql("select * from HrmSalaryChange where multresourceid like '%," + id + ",%' order by id desc");
			while (rs.next()) {
				String itemid = Util.null2String(rs.getString("itemid"));
				String changedate = Util.null2String(rs.getString("changedate"));
				String changetype = Util.null2String(rs.getString("changetype"));
				String salary = Util.null2String(rs.getString("salary"));
				String changeresion = Util.toScreen(rs.getString("changeresion"), user.getLanguage());
				String changeuser = Util.null2String(rs.getString("changeuser"));
				String oldsalary = Util.null2String(rs.getString("oldvalue"));
				String newsalary = Util.null2String(rs.getString("newvalue"));
					
				int i = 0;
				data = new HashMap<String, Object>();
				data.put("column_" + i++, Util.toScreen(SalaryComInfo.getSalaryname(itemid), user.getLanguage()));
				data.put("column_" + i++, changedate);
				data.put("column_" + i++, oldsalary);
				String changetypename = "";
				if (changetype.equals("1")) {
					changetypename = SystemEnv.getHtmlLabelName(456, user.getLanguage());
				} else if (changetype.equals("2")) {
					changetypename = SystemEnv.getHtmlLabelName(457, user.getLanguage());
				} else if (changetype.equals("3")) {
					changetypename = SystemEnv.getHtmlLabelName(15816, user.getLanguage());
				}
				data.put("column_" + i++, changetypename);
				data.put("column_" + i++, salary);
				data.put("column_" + i++, newsalary);
				data.put("column_" + i++, changeresion);
				data.put("column_" + i++, Util.toScreen(ResourceComInfo.getResourcename(changeuser), user.getLanguage()));
				datas.add(data);
			}
			table.put("columns", columns);
			table.put("datas", datas);
			retmap.put("table", table);
		} catch (Exception e) {
			retmap.put("api_status", false);
			retmap.put("api_errormsg", e.getMessage());
			e.printStackTrace();
		}
		return JSONObject.toJSONString(retmap);
	}	
	
	
	
	@SuppressWarnings("deprecation")
	private boolean isHasRight(User user) {
		//入职维护人维护权限。---工资福利
		RecordSet rs1 = new RecordSet();
		rs1.executeSql("  select hrmids from HrmInfoMaintenance where id = 2");
		rs1.next();	 
		String[] hrmids = rs1.getString("hrmids").split(",");
		boolean flog = false;
		for(String s1 :hrmids){
			if(s1.equals(user.getUID()+"")){
				flog=true;
				break;
			}
		}
		return flog;
	}

	/**
	 * 查看工资时是否需要二次密码验证
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String, Object> isNeedSecondPwdVerify(HttpServletRequest request, HttpServletResponse response) {
		//是否需要二次密码验证
		Map<String,Object> retmap = new HashMap<String,Object>();
		User user = HrmUserVarify.getUser(request, response);
		RecordSet rs = new RecordSet();
		try{
			boolean isNeedSecondPwdVerify = false;
			ChgPasswdReminder reminder = new ChgPasswdReminder();
			RemindSettings settings = reminder.getRemindSettings();
			String isOpenSecondaryPwd = settings.getSecondPassword();//二次验证密码是否允许作为身份校验  后台-安全设置-高级设置处开启二次密码允许作为身份校验
			if(isOpenSecondaryPwd.equals("1")){			//总开关开了
				rs.executeQuery("select isenable from enc_secondauth_config_info where itemcode=?", "SALARY");
				if(rs.next()&&rs.getString("isenable").equals("1")){   //工资应用设置开了
					isNeedSecondPwdVerify = true;
					//是否在免密时间内
					String currentTime = TimeUtil.getCurrentTimeString();
					String freeSecretTime = SecondAuthBiz.getFreeSecretTime(user,SecondAuthType.SecondAuthPassword.getId());
					if (freeSecretTime.compareTo(currentTime) > 0) {    //说明仍然在免密时间内,免密时间内，不需要二次验证
						isNeedSecondPwdVerify = false;
					}
				}
			}
			retmap.put("status", "1");
			retmap.put("isNeedSecondPwdVerify", isNeedSecondPwdVerify);
		}catch (Exception e){
			retmap.put("status", "-1");
			retmap.put("message", e.getMessage());
			e.printStackTrace();
		}
		return retmap;
	}

	/**
	 * 查看工资时验证二次密码表单
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String, Object> getSecondaryPasswordForm(HttpServletRequest request, HttpServletResponse response) {
		Map<String,Object> resultMap = new HashMap<String,Object>();
		try{
			User user = HrmUserVarify.getUser(request, response);
			List<Map<String,Object>> groupList = new ArrayList<Map<String,Object>>();//表单的group的集合
			Map<String, Object> groupItem = new HashMap<String, Object>();//表单group的属性
            Map<String, Object> otherParams = new HashMap<String, Object>();
			List<Object> itemList = new ArrayList<Object>();//表单每个group下面的item集合
			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			SearchConditionItem searchConditionItem = null;//表单每个的item项
			HrmFieldBean hrmFieldBean = null;
			groupItem.put("defaultshow",true);
			hrmFieldBean = new HrmFieldBean();
			hrmFieldBean.setFieldname("secondpassword");//二次密码
			hrmFieldBean.setFieldlabel("388412");
			hrmFieldBean.setFieldhtmltype("1");
			hrmFieldBean.setType("1");
			hrmFieldBean.setViewAttr(3);
			hrmFieldBean.setIsFormField(true);
			hrmFieldBean.setMultilang(false);
			otherParams.put("tip","");
			otherParams.put("tipLength","100");
			otherParams.put("type","password");
			hrmFieldBean.setOtherparam(otherParams);
			searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
			searchConditionItem.setRules("required|string");
			itemList.add(searchConditionItem);

			groupItem.put("items", itemList);
			groupList.add(groupItem);
			resultMap.put("conditions", groupList);
			boolean settedSecondPassword = isSettedSecondPassword(user);
			resultMap.put("settedSecondPassword", settedSecondPassword);
			resultMap.put("status", "1");
		}catch (Exception e){
			resultMap.put("status", "-1");
			resultMap.put("message", e.getMessage());
			e.printStackTrace();
		}
		return resultMap;
	}

	/**
	 * 二次密码校验
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String, Object> checkSecondPasword(HttpServletRequest request, HttpServletResponse response) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		User user = HrmUserVarify.getUser(request, response);
		try{
			RecordSet rs = new RecordSet();
			RSA rsa = new RSA();
			String secondpassword = Util.null2String(request.getParameter("secondpassword")).trim();
			String isrsaopen = Util.null2String(rs.getPropValue("openRSA", "isrsaopen"));
			List<String> decriptList = new ArrayList<>() ;

			if("1".equals(isrsaopen)){
				decriptList.add(secondpassword) ;
				List<String> resultList = rsa.decryptList(request,decriptList) ;
				secondpassword = resultList.get(0) ;

				if(!rsa.getMessage().equals("0")){
					resultMap.put("status", "-1");
					resultMap.put("message", SystemEnv.getHtmlLabelName(501137, user.getLanguage()));      //您输入的二次验证密码不正确
					writeLog("rsa.getMessage()", rsa.getMessage());
					return resultMap;
				}
			}
			if (!"".equals(secondpassword) && user != null) {
				int userId = user.getUID();
				String sql = "select salt,secondarypwd,usesecondarypwd from hrmresourcemanager where id = ?";
				rs.executeQuery(sql, userId);
				int count = rs.getCounts();
				if (count <= 0) {
					sql = "select salt,secondarypwd,usesecondarypwd from hrmresource where id = ?";
					rs.executeQuery(sql, userId);
				}

				if (rs.next()) {
					String salt = Util.null2String(rs.getString("salt"));
					String secondarypwd = Util.null2String(rs.getString("secondarypwd"));
					int usesecondarypwd = Util.getIntValue(rs.getString("usesecondarypwd"), 0);

					if (usesecondarypwd != 1) {
						resultMap.put("status", "-1");
						resultMap.put("message", SystemEnv.getHtmlLabelName(501316, user.getLanguage()));       //请设置二次验证密码
						return resultMap;
					}

					String[] pwdArr = PasswordUtil.encrypt(secondpassword, salt);
					String encryptPwd = Util.null2String(pwdArr[0]);

					if (encryptPwd.equals(secondarypwd)) {      //密码一致
						SecondAuthBiz.updateFreeSecretTime(user, SecondAuthType.SecondAuthPassword.getId());
						resultMap.put("status", "1");
						resultMap.put("message", SystemEnv.getHtmlLabelName(22083, user.getLanguage()));       //验证通过
					} else {
						resultMap.put("status", "-1");
						resultMap.put("message", SystemEnv.getHtmlLabelName(501137, user.getLanguage()));      //您输入的二次验证密码不正确
					}
				}
			}
		}catch (Exception e){
				resultMap.put("status", "-1");
				resultMap.put("message", SystemEnv.getHtmlLabelName(501137, user.getLanguage()));      //您输入的二次验证密码不正确
				writeLog(e);
				e.printStackTrace();
			}
		return resultMap;
	}


	/**
	 * 是否设置二次密码
	 * @param user
	 * @return
	 */
	private boolean isSettedSecondPassword(User user){
		RecordSet rs = new RecordSet();
		if (user != null) {
			int userId = user.getUID();
			String sql = "select salt,secondarypwd,usesecondarypwd from hrmresourcemanager where id = ?";
			rs.executeQuery(sql, userId);
			int count = rs.getCounts();
			if (count <= 0) {
				sql = "select salt,secondarypwd,usesecondarypwd from hrmresource where id = ?";
				rs.executeQuery(sql, userId);
			}
			if (rs.next()) {
				int usesecondarypwd = Util.getIntValue(rs.getString("usesecondarypwd"), 0);
				if(usesecondarypwd==1){
					return true;
				}
			}
		}
		return false;
	}
}
