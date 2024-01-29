package com.engine.kq.cmd.attendanceButton;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.api.hrm.service.HrmMobileSignInService;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.log.KQLog;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import weaver.conn.RecordSet;
import weaver.crm.Maint.CustomerInfoComInfo;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.mobile.sign.HrmSign;
import weaver.mobile.sign.MobileSign;
import weaver.systeminfo.SystemEnv;

/**
 * 外勤记录
 */
public class GetOutSearchListCmd extends AbstractCommonCommand<Map<String, Object>> {
  private KQLog kqLog = new KQLog();
  private HttpServletRequest request;
  private HttpServletResponse response;

	public GetOutSearchListCmd(HttpServletRequest request,Map<String, Object> params, User user,
      HttpServletResponse response) {
		this.request = request;
	  this.user = user;
		this.params = params;
		this.response = response;
	}

	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String, Object> retmap = new HashMap<String, Object>();
		try{
            CustomerInfoComInfo customerInfoComInfo = new CustomerInfoComInfo();
      List<Object> outLists = new ArrayList<>();
      Map<String,Object> outMap = new HashMap<>();
      RecordSet rs = new RecordSet();

      HrmMobileSignInService mobileSignInService = new HrmMobileSignInService();
      JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
      String fromDate = Util.null2String(jsonObj.get("fromDate"));
      String toDate = Util.null2String(jsonObj.get("toDate"));
      String typeselect =Util.null2String(jsonObj.get("typeselect"));
      String typeselectselect =Util.null2String(jsonObj.get("typeselectselect"));
      String typeselectfrom =Util.null2String(jsonObj.get("typeselectfrom"));
      String typeselectto =Util.null2String(jsonObj.get("typeselectto"));

      if (!typeselectselect.equals("") && !typeselectselect.equals("0") && !typeselectselect.equals("6")) {
        fromDate = TimeUtil.getDateByOption(typeselectselect, "0");
        toDate = TimeUtil.getDateByOption(typeselectselect, "1");
      }else{
        fromDate = typeselectfrom;
        toDate = typeselectto;
      }
      //0表示全部，1表示外勤，2表示移动端
      String signtypecondition = Util.null2String(jsonObj.get("signtypecondition"));
      String resourceId = Util.null2String(jsonObj.get("resourceId"));
      int pageIndex = Util.getIntValue(Util.null2String(jsonObj.get("pageIndex")), -1);
      int pageSize = Util.getIntValue(Util.null2String(jsonObj.get("pageSize")), -1);
      //order表示正序0还是倒序1(desc)
      String order = Util.null2String(jsonObj.get("order"));
      boolean isDesc = false;
      if("1".equalsIgnoreCase(order)){
        isDesc = true;
      }

      int count = 0;
      int pageCount = 0;
      int isHavePre = 0;
      int isHaveNext = 0;

      if(resourceId.length()==0){
        resourceId = ""+user.getUID();
      }

      if(!mobileSignInService.hasRight(request, response)) {
        retmap.put("status", "-1");
        retmap.put("hasRight", false);
        return retmap;
      }

      String beginQueryDate = "";
      String endQueryDate = "";
      if(fromDate.length()>0){
        beginQueryDate = fromDate+" 00:00:00";
      }
      if(toDate.length()>0){
        endQueryDate = toDate+" 23:59:59";
      }
      String resourceSql = "select id from HrmResource where status in (0,1,2,3,5)";
      if(!"".equals(resourceId)){
        resourceSql += " and id in ("+resourceId+")";
      }

      String backfields = " uniqueid,id,operater,operate_type,operate_date,operate_time,LONGITUDE,LATITUDE,address,remark,attachment,signtype,crm ";
      String fromSql  = " ";
      String sqlWhere = " where 1=1 ";
      String orderby = " order by operate_date "+(isDesc?"desc":"asc")+",operate_time "+(isDesc?"desc":"asc")+" " ;
      String descOrderBy = " order by operate_date "+(isDesc?"asc":"desc")+",operate_time "+(isDesc?"asc":"desc")+" " ;

      String hrmSignSql = HrmSign.CreateHrmSignSql4E9(resourceSql, beginQueryDate, endQueryDate);

      String mobileSignSql = MobileSign
          .CreateMobileSignSql(resourceSql, beginQueryDate, endQueryDate);
      String UNIONsql = "";
      if("1".equals(signtypecondition)){
        UNIONsql = hrmSignSql;
      }else if("2".equals(signtypecondition)){
        UNIONsql = mobileSignSql;
      }else{
        UNIONsql = hrmSignSql + " UNION "+ mobileSignSql;
      }

      fromSql = " from ( "+UNIONsql+" ) tmp  ";

      String sql = " select count(tmpcol) as c from ( select 1 as tmpcol "+fromSql+sqlWhere+") t";
      rs.execute(sql);
      if (rs.next()){
        count = rs.getInt("c");
      }

      if (count <= 0) {
        pageCount = 0;
      }

      pageCount = count / pageSize + ((count % pageSize > 0) ? 1 : 0);

      isHaveNext = (pageIndex + 1 <= pageCount) ? 1 : 0;

      isHavePre = (pageIndex - 1 >= 1) ? 1 : 0;

      sql = backfields + fromSql  + sqlWhere ;

      if (pageIndex > 0 && pageSize > 0) {
        if (rs.getDBType().equals("oracle")) {
          sql = " select * from (select " + sql+") t "+orderby;
          sql = "select * from ( select row_.*, rownum rownum_ from ( " + sql + " ) row_ where rownum <= "
              + (pageIndex * pageSize) + ") where rownum_ > " + ((pageIndex - 1) * pageSize);
        } else if (rs.getDBType().equals("mysql")) {
          sql = " select * from (select " + sql+") t "+orderby;
          sql = "select t1.* from (" + sql + ") t1 limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
        }
        else if (rs.getDBType().equals("postgresql")) {
            sql = " select * from (select " + sql+") t "+orderby;
            sql = "select t1.* from (" + sql + ") t1 limit " + pageSize + " offset " + ((pageIndex - 1) * pageSize);
        }
        else {
          if (pageIndex > 1) {
            int topSize = pageSize;
            if (pageSize * pageIndex > count) {
              topSize = count - (pageSize * (pageIndex - 1));
            }
            sql = " select top " + topSize + " * from ( select top  " + topSize + " * from ( select top "
                + (pageIndex * pageSize) + sql + orderby+" ) tbltemp1 " + descOrderBy + ") tbltemp2 " + orderby;
          } else {
            sql = " select top " + pageSize + sql+ orderby;
          }
        }
      } else {
        sql = " select " + sql+ orderby;
      }

      new BaseBean().writeLog("==zj==(打卡记录)" + JSON.toJSONString(sql));
      rs.executeQuery(sql);
      kqLog.info("pageIndex:"+pageIndex+":pageSize:"+pageSize+":sql:"+sql);
      while (rs.next()){
        String id = rs.getString("id");
        String operate_type = rs.getString("operate_type");
        String operate_date = rs.getString("operate_date");
        String operate_time = rs.getString("operate_time");
        String longitude = rs.getString("longitude");
        String latitude = rs.getString("latitude");
        String address = rs.getString("address");
        String remark = rs.getString("remark");
        String attachment = rs.getString("attachment");
        List<String> attachments = Util.TokenizerString(attachment, ",");
        String date = operate_date+" "+operate_time;
        outMap = new HashMap<>();
        outMap.put("id", id);
        outMap.put("operate_type", operate_type);
        outMap.put("date", date);
        Map<String,Object> positionMap = new HashMap<>();
        positionMap.put("longitude", longitude);
        positionMap.put("latitude", latitude);
        positionMap.put("address", address);
        outMap.put("position", positionMap);
        outMap.put("remark", remark);
        List<Object> attachmentList = new ArrayList<>();
        if(!attachments.isEmpty()){
          for(String attach : attachments) {
            attachmentList.add(DocDownloadCheckUtil.checkPermission(attach,null));
          }
        }
        outMap.put("attachment", attachmentList);
        List<String> crmList = new ArrayList<>();
        String crm = Util.null2String(rs.getString("crm"));
        if(!"".equals(crm)){
            List<String> ids = Util.splitString2List(crm, ",");
            for(String key : ids) {
                crmList.add(customerInfoComInfo.getCustomerInfoname(key));
            }
        }
        outMap.put("crm", crmList);
        outLists.add(outMap);
      }

      retmap.put("status", "1");
      retmap.put("datas", outLists);
      retmap.put("pagesize", pageSize);
      retmap.put("pageindex", pageIndex);
      retmap.put("count", count);
      retmap.put("pagecount", pageCount);
      retmap.put("ishavepre", isHavePre);
      retmap.put("ishavenext", isHaveNext);
    }catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
      kqLog.info("外勤打卡报错:GetOutSearchListCmd:");
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
		}
		return retmap;
	}

  @Override
	public BizLogContext getLogContext() {
		return null;
	}

}
