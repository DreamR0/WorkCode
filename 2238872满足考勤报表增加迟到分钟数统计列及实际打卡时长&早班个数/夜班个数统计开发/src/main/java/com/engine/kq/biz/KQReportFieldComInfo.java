package com.engine.kq.biz;

import weaver.cache.*;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;

/**
 * 报表自定义字段
 */
public class KQReportFieldComInfo extends CacheBase{
	protected static String TABLE_NAME = "kq_report_field";
	/** sql中的where信息，不要以where开始 */
	protected static String TABLE_WHERE = null;
	/** sql中的order by信息，不要以order by开始 */
	protected static String TABLE_ORDER = "showorder";

	@PKColumn(type = CacheColumnType.NUMBER)
	protected static String PK_NAME = "id";

	@CacheColumn(name = "fieldname")
	protected static int fieldname;

	@CacheColumn(name = "fieldlabel")
	protected static int fieldlabel;

	@CacheColumn(name = "unittype")
	protected static int unittype;

	@CacheColumn(name = "width")
	protected static int width;

	@CacheColumn(name = "parentid")
	protected static int parentid;

	@CacheColumn(name = "isdefinedcolumn")
	protected static int isdefinedcolumn;

	@CacheColumn(name = "defaultshow")
	protected static int defaultshow;

	@CacheColumn(name = "isleavetype")
	protected static int isleavetype;

	@CacheColumn(name = "reporttype")
	protected static int reporttype;

	@CacheColumn(name = "reporttype1")
	protected static int reporttype1;

	@CacheColumn(name = "isdataColumn")
	protected static int isdataColumn;

	@CacheColumn(name = "showDetial")
	protected static int showDetial;

	@CacheColumn(name = "groupid")
	protected static int groupid;

	@CacheColumn(name = "isenable")
	protected static int isenable;

	@CacheColumn(name = "formula")
	protected static int formula;

	@CacheColumn(name = "issystem")
	protected static int issystem;

	@CacheColumn(name = "cascadekey")
	protected static int cascadekey;

	@CacheColumn(name = "dailyShowOrder")
	protected static int dailyShowOrder;

	public static ConcurrentHashMap<String, String> cascadekey2fieldname = new ConcurrentHashMap<String, String>();
	public static ConcurrentHashMap<String, String> field2Id = new ConcurrentHashMap<String, String>();

	@Override
	protected CacheMap initCache() throws Exception {
		CacheMap localData = createCacheMap();
		RecordSet rs = new RecordSet();
		cascadekey2fieldname.clear();
		field2Id.clear();
//		String sql = " select a.id,fieldname,fieldlabel,width,unittype,parentid,isdefinedcolumn,defaultshow,isleavetype," +
//									"	reporttype,isdatacolumn,showdetial,a.showorder,groupid,isenable,formula,issystem,cascadekey " +
//								 " from kq_report_field a left join kq_report_field_group b on a.groupid = b.id " +
//							   " order by b.showorder asc, a.showorder asc ";

		String sql = " SELECT id, fieldname,fieldlabel, unittype,width,parentid,isdefinedcolumn,defaultshow,isleavetype,'month' as reporttype, reporttype as reporttype1,isdataColumn," +
							"		showDetial,groupid,isenable,formula,issystem,cascadekey,dailyShowOrder FROM kq_report_field where fieldname= 'lastname' ";
		new BaseBean().writeLog("==zj==(sql1)" + sql);
		rs.executeQuery(sql);
		while (rs.next()) {
			if(Util.null2String(rs.getString("cascadekey")).length()>0){
				String[] arrCascadekey = Util.splitString(rs.getString("cascadekey"),",");
				for(int i=0;i<arrCascadekey.length;i++){
					cascadekey2fieldname.put(arrCascadekey[i],rs.getString("fieldname"));
				}
			}
			String id = Util.null2String(rs.getString(PK_NAME))+""+Util.null2String(rs.getString("reporttype"));
			field2Id.put(Util.null2String(rs.getString("fieldname")),id);
			CacheItem cacheItem = createCacheItem();
			parseResultSetToCacheItem(rs, cacheItem);
			modifyCacheItem(id, cacheItem);
			localData.put(id, cacheItem);
		}

		sql = " SELECT id, fieldname,fieldlabel, unittype,width,parentid,isdefinedcolumn,defaultshow,isleavetype,'month' as reporttype, reporttype as reporttype1,isdataColumn," +
			          "		showDetial,groupid,isenable,formula,issystem,cascadekey,dailyShowOrder FROM (" +
								" SELECT CASE WHEN b.showorder IS NULL THEN 999 ELSE b.showorder END AS tmp_showorder," +
								" a.* " +
								" FROM kq_report_field a LEFT JOIN kq_report_field_group b ON a.groupid = b.id where reportType in('all','month')) a" +
								" ORDER BY a.tmp_showorder ASC, a.showorder ASC ";
		new BaseBean().writeLog("==zj==(sql2)" + sql);
		rs.executeQuery(sql);
		while (rs.next()) {
			if(Util.null2String(rs.getString("cascadekey")).length()>0){
				String[] arrCascadekey = Util.splitString(rs.getString("cascadekey"),",");
				for(int i=0;i<arrCascadekey.length;i++){
					cascadekey2fieldname.put(arrCascadekey[i],rs.getString("fieldname"));
				}
			}
			String id = Util.null2String(rs.getString(PK_NAME))+""+Util.null2String(rs.getString("reporttype"));
			field2Id.put(Util.null2String(rs.getString("fieldname")),id);
			CacheItem cacheItem = createCacheItem();
			parseResultSetToCacheItem(rs, cacheItem);
			modifyCacheItem(id, cacheItem);
			localData.put(id, cacheItem);
		}

		sql = " SELECT id, fieldname,fieldlabel, unittype,width,parentid,isdefinedcolumn,defaultshow,isleavetype,'daily' as reporttype,reporttype as reporttype1, isdataColumn," +
			" showDetial,groupid,isenable,formula,issystem,cascadekey,dailyShowOrder" +
			" FROM kq_report_field a where reportType in('all','daily') ORDER BY dailyShowOrder ASC ";
		new BaseBean().writeLog("==zj==(sql3)" + sql);
		rs.executeQuery(sql);
		while (rs.next()) {
			String id = Util.null2String(rs.getString(PK_NAME))+""+Util.null2String(rs.getString("reporttype"));
			CacheItem cacheItem = createCacheItem();
			parseResultSetToCacheItem(rs, cacheItem);
			modifyCacheItem(id, cacheItem);
			localData.put(id, cacheItem);
		}
		return localData;
	}

	public String getId(){
		return (String)getRowValue(PK_INDEX);
	}

	public String getFieldname() { return (String)getRowValue(fieldname); }

	public String getFieldname(String key)
	{
		return (String)getValue(fieldname,key);
	}

	public String getFieldlabel() { return (String)getRowValue(fieldlabel); }
	public String getFieldlabel(String key)
	{
		return (String)getValue(fieldlabel,key);
	}

	public String getUnittype() { return (String)getRowValue(unittype); }
	public String getUnittype(String key)
	{
		return (String)getValue(unittype,key);
	}

	public String getWidth() { return (String)getRowValue(width); }
	public String getWidth(String key)
	{
		return (String)getValue(width,key);
	}

	public String getParentid() { return (String)getRowValue(parentid); }
	public String getParentid(String key)
	{
		return (String)getValue(parentid,key);
	}

	public String getIsDefinedColumn() { return (String)getRowValue(isdefinedcolumn); }
	public String getIsDefinedColumn(String key)
	{
		return (String)getValue(isdefinedcolumn,key);
	}

	public String getDefaultShow() { return (String)getRowValue(defaultshow); }
	public String getDefaultShow(String key)
	{
		return (String)getValue(defaultshow,key);
	}

	public String getIsLeaveType() { return (String)getRowValue(isleavetype); }
	public String getIsLeaveType(String key) { return (String)getValue(isleavetype,key); }

	public String getReportType() { return (String)getRowValue(reporttype); }
	public String getReportType(String key) { return (String)getValue(reporttype,key); }

	public String getReportType1() { return (String)getRowValue(reporttype1); }
	public String getReportType1(String key) { return (String)getValue(reporttype1,key); }

	public String getIsdataColumn() { return (String)getRowValue(isdataColumn); }
	public String getIsdataColumn(String key) { return (String)getValue(isdataColumn,key); }

	public String getShowDetial() { return (String)getRowValue(showDetial); }
	public String getShowDetial(String key) { return (String)getValue(showDetial,key); }

	public String getGroupid() { return (String)getRowValue(groupid); }
	public String getGroupid(String key) { return (String)getValue(groupid,key); }

	public String getIsenable() { return (String)getRowValue(isenable); }
	public String getIsenable(String key) { return (String)getValue(isenable,key); }

	public String getFormula() { return (String)getRowValue(formula); }
	public String getFormula(String key) { return (String)getValue(formula,key); }

	public String getIsSystem() { return (String)getRowValue(issystem); }
	public String getIsSystem(String key) { return (String)getValue(issystem,key); }

	public String getCascadekey() { return (String)getRowValue(cascadekey); }
	public String getCascadekey(String key) { return (String)getValue(cascadekey,key); }

	public String getDailyShowOrder() { return (String)getRowValue(dailyShowOrder); }
	public String getDailyShowOrder(String key) { return (String)getValue(dailyShowOrder,key); }
}