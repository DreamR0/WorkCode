package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.api.hrm.util.PageUidFactory;
import com.cloudstore.dev.api.util.Util_TableMap;
import com.engine.hrm.util.HrmUtil;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.enums.OverTimeComputingModeEnum;
import com.engine.kq.log.KQLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.common.database.dialect.DbDialectFactory;
import weaver.systeminfo.SystemEnv;

/**
 * 考勤流程 设置类
 */
public class KQAttFlowSetBiz {

  private KQLog kqLog = new KQLog();

  public String getFieldInfo(Map<String, String> attSetMap){
    String unionSql = "";
    if(attSetMap.isEmpty()){
      return "";
    }
    String[][] fieldsRefect = KQAttFlowFieldsSetBiz.fieldsRefect;
    String[] leavebackMainFields = KQAttFlowFieldsSetBiz.leavebackMainFields;
    String[] cardMainFields = KQAttFlowFieldsSetBiz.cardMainFields;
    String[] processchangeMainFields = KQAttFlowFieldsSetBiz.processchangeMainFields;

    String prefiex = "detail_";
    String mainPrefix = "a";
    String detailPrefix = "b";
    String allPrefix = "c";

    String attId = Util.null2String(attSetMap.get("attId"));
    int kqtype = Util.getIntValue(Util.null2String(attSetMap.get("kqtype")),0);
    String usedetail = Util.null2String(attSetMap.get("usedetail"));
    String tablename = Util.null2String(attSetMap.get("tablename"));
    tablename = tablename+" "+mainPrefix;
    String detailtablename = Util.null2String(attSetMap.get("detailtablename"));
    detailtablename = detailtablename+" "+detailPrefix;

    String custome_sql = Util.null2String(attSetMap.get("custome_sql"));

    if(attId.length() > 0){
      String backfields = "";
      Map<String,String> fieldidMap = new HashMap<>();
      Map<String,String> fieldnameMap = new HashMap<>();
      Map<String,String> backfieldMap = new HashMap<>();
      RecordSet rs = new RecordSet();
      String dbtype = rs.getDBType();
      String sql = "select a.field003 fieldid,a.field004 fieldname,b.field002 wffieldname from kq_att_proc_relation a left join kq_att_proc_fields b on a.field002=b.id where a.field001 = ? ";
      rs.executeQuery(sql, attId);
      while(rs.next()){
        String fieldid = Util.null2String(rs.getString("fieldid"));
        String fieldname = Util.null2String(rs.getString("fieldname"));
        String wffieldname = Util.null2String(rs.getString("wffieldname"));
        if(Util.getIntValue(fieldid,0) <= 0){
          kqLog.info("getFieldInfo:考勤流程字段对应有误 :fieldid is null: :attId:"+attId);
          break;
        }
        if("1".equalsIgnoreCase(usedetail)){
          if(KqSplitFlowTypeEnum.CARD.getFlowtype() == kqtype || KqSplitFlowTypeEnum.LEAVEBACK.getFlowtype() == kqtype
              || KqSplitFlowTypeEnum.PROCESSCHANGE.getFlowtype() == kqtype){
          }else{
            if(wffieldname.indexOf(prefiex) > -1){
              wffieldname = wffieldname.substring(prefiex.length());
            }else{
              kqLog.info("getFieldInfo:考勤流程字段对应有误 :usedetail wffieldname is error: :attId:"+attId);
//              break;
            }
          }
        }
        String backfield = "";
        fieldidMap.put(wffieldname, fieldid);
        fieldnameMap.put(wffieldname, fieldname);
        if(KqSplitFlowTypeEnum.LEAVEBACK.getFlowtype() == kqtype){
          if(Arrays.asList(leavebackMainFields).contains(wffieldname)){
            backfield = mainPrefix+"."+fieldname +" as "+wffieldname;
          }else{
            backfield = detailPrefix+"."+fieldname +" as "+wffieldname;
          }
        }else if(KqSplitFlowTypeEnum.CARD.getFlowtype() == kqtype){
          if(Arrays.asList(cardMainFields).contains(wffieldname)){
            backfield = mainPrefix+"."+fieldname +" as "+wffieldname;
          }else{
            if("detail_signtype".equalsIgnoreCase(wffieldname)){
              if("mysql".equalsIgnoreCase(dbtype)){
                backfield = "CAST("+detailPrefix+"."+fieldname +" as SIGNED) as "+wffieldname;
              }else {
                backfield = "cast("+detailPrefix+"."+fieldname +" as int) as "+wffieldname;
              }
            }else{
              backfield = detailPrefix+"."+fieldname +" as "+wffieldname;
            }
          }
        }else if(KqSplitFlowTypeEnum.PROCESSCHANGE.getFlowtype() == kqtype){
          if(Arrays.asList(processchangeMainFields).contains(wffieldname)){
            backfield = mainPrefix+"."+fieldname +" as "+wffieldname;
          }else{
            backfield = detailPrefix+"."+fieldname +" as "+wffieldname;
          }
        }else{
          if("1".equalsIgnoreCase(usedetail)){
            if(KqSplitFlowTypeEnum.EVECTION.getFlowtype() == kqtype && "companion".equalsIgnoreCase(wffieldname)){
              if("oracle".equalsIgnoreCase(dbtype)||"postgresql".equalsIgnoreCase(dbtype)){
                backfield = "to_char("+detailPrefix+"."+fieldname +") as "+wffieldname;
              }else if("mysql".equalsIgnoreCase(dbtype)){
                backfield = "CONVERT("+detailPrefix+"."+fieldname +", char ) as "+wffieldname;
              }else {
                backfield = "cast("+detailPrefix+"."+fieldname +" as varchar(max) ) as "+wffieldname;
              }
            }else{
              backfield = detailPrefix+"."+fieldname +" as "+wffieldname;
            }
          }else{
            if(KqSplitFlowTypeEnum.EVECTION.getFlowtype() == kqtype && "companion".equalsIgnoreCase(wffieldname)){
              if("oracle".equalsIgnoreCase(dbtype)||"postgresql".equalsIgnoreCase(dbtype)){
                backfield = "to_char("+mainPrefix+"."+fieldname +") as "+wffieldname;
              }else if("mysql".equalsIgnoreCase(dbtype)){
                backfield = "CONVERT("+mainPrefix+"."+fieldname +", char ) as "+wffieldname;
              }else {
                backfield = "cast("+mainPrefix+"."+fieldname +" as varchar(max) ) as "+wffieldname;
              }
            }else{
              backfield = mainPrefix+"."+fieldname +" as "+wffieldname;
            }
          }
        }
        if(!"".equalsIgnoreCase(wffieldname)){
          backfieldMap.put(wffieldname, backfield);
        }
      }
      if(backfieldMap.isEmpty()){
        kqLog.info("getFieldInfo:考勤流程表字段对应為空 :attId:"+attId+":kqtype:"+kqtype);
        return "";
      }
      //TODO 这里需要改
      boolean isReflect = true;
      String[] fieldsType = fieldsRefect[kqtype];
      for(int i = 0 ;i < fieldsType.length ;i++){
        String backfield = Util.null2String(backfieldMap.get(fieldsType[i]));
        if(backfield.length() > 0){
          backfields += ","+backfieldMap.get(fieldsType[i]);
        }else{
          kqLog.info("getFieldInfo:考勤流程表字段对应未找到 :attId:"+attId+":kqtype:"+kqtype+":fieldsType[i]:"+fieldsType[i]);
          if(fieldsType[i].indexOf("duration") > -1){
            backfields += ", 0 as "+fieldsType[i];
          }else{
            backfields += ", '' as "+fieldsType[i];
          }
        }
      }
      if(backfields.length() > 0){
        backfields = backfields.substring(1);
        backfields += ","+mainPrefix+".requestid ";
      }
      if(KqSplitFlowTypeEnum.SHIFT.getFlowtype() == kqtype || KqSplitFlowTypeEnum.LEAVEBACK.getFlowtype() == kqtype
          || KqSplitFlowTypeEnum.CARD.getFlowtype() == kqtype || KqSplitFlowTypeEnum.PROCESSCHANGE.getFlowtype() == kqtype){
        if(KqSplitFlowTypeEnum.SHIFT.getFlowtype() == kqtype){
          unionSql = "select "+backfields+" from "+detailtablename;
        }else{
          unionSql = "select "+backfields+" from "+tablename+" left join "+detailtablename+" on "+mainPrefix+".id = "+detailPrefix+".mainid" ;
          if(KqSplitFlowTypeEnum.CARD.getFlowtype() == kqtype){
            unionSql = "select "+allPrefix+".* from ("+ "select "+backfields+" from "+tablename+" left join "+detailtablename+" on "+mainPrefix+".id = "+detailPrefix+".mainid" +") "+allPrefix+" where ("+custome_sql+")";
          }
        }
      }else{
        if("1".equalsIgnoreCase(usedetail)){
          unionSql = "select "+backfields+" from "+detailtablename+" left join "+tablename+" on "+mainPrefix+".id = "+detailPrefix+".mainid" ;
        }else{
          unionSql = "select "+backfields+" from "+tablename;
        }
      }
    }
    return unionSql;
  }

  /**
   *
   * @param kqtype
   * @param user
   * @param isMyKQ 是否来自我的考勤
   * @param params
   * @return
   */
  public String getFieldInfoByKQType(String kqtype, User user, String isMyKQ,
      Map<String, Object> params){
    String workflowid =Util.null2String(params.get("workflowid"));
    String custome_sql =Util.null2String(params.get("custome_sql"));
    RecordSet rs = new RecordSet();
    List<String> formids = new ArrayList<>();
    String unionSqls = "";
    String sql = "select * from kq_att_proc_set where field006= ?";
    if(workflowid.length() > 0 && Util.getIntValue(workflowid) > 0){
      sql += " and field001 = ? ";
      rs.executeQuery(sql,kqtype,workflowid);
    }else{
      rs.executeQuery(sql,kqtype);
    }
    while(rs.next()){
      Map<String,String> attSetMap = new HashMap<>();
      String formid = Util.null2s(rs.getString("field002"),"");
      //TODO 这里需要改 如果一个表单被多个表单使用的情况
//      if(formid.length() == 0){
//        continue;
//      }
//      if(formids.contains(formid)){
//        continue;
//      }else{
//        formids.add(formid);
//      }
      String attId = Util.null2String(rs.getString("id"));
      String usedetail = Util.null2String(rs.getString("usedetail"));
      String tablename = Util.null2String(rs.getString("tablename"));
      String detailtablename = Util.null2String(rs.getString("detailtablename"));
      attSetMap.put("attId", attId);
      attSetMap.put("kqtype", kqtype);
      attSetMap.put("usedetail", usedetail);
      attSetMap.put("tablename", tablename);
      attSetMap.put("detailtablename", detailtablename);
      attSetMap.put("custome_sql", custome_sql);
      if("1".equalsIgnoreCase(usedetail)){
        if("".equalsIgnoreCase(detailtablename)){
          kqLog.info("getFieldInfoByKQType:考勤流程明细表单不存在 :attId:"+attId+":kqtype:"+kqtype);
          continue;
        }
      }else {
        if("".equalsIgnoreCase(tablename)){
          kqLog.info("getFieldInfoByKQType:考勤流程主表表单不存在 :attId:"+attId+":kqtype:"+kqtype);
          continue;
        }
      }
      String fieldSql = getFieldInfo(attSetMap);

      if(fieldSql.length() > 0){
        if(unionSqls.length() > 0){
          unionSqls += " union all "+ fieldSql;
        }else{
          unionSqls += fieldSql;
        }
      }
    }

    if(unionSqls.length() > 0){

      String unionsql = "";
      if("1".equalsIgnoreCase(isMyKQ)){
        String hrmSql = " select * from hrmresource a where 1=1 ";
        unionSqls = " select distinct u.*,w.currentnodetype,w.status as flowstatus,w.workflowid ,w.requestid as req_requestid,w.requestname,a.managerid,a.managerstr,a.loginid,a.subcompanyid1 as asubcompanyid1,a.departmentid as adepartmentid,a.lastname from (" +unionSqls+") u left join Workflow_Requestbase w on u.requestid = w.requestid  left join ("+hrmSql+") a on u.resourceid = a.id ";
      }else{
        String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"a");
        String hrmSql = " select * from hrmresource a where 1=1 "+rightSql;
        unionSqls = " select distinct u.*,w.currentnodetype,w.status as flowstatus,w.workflowid ,w.requestid as req_requestid,w.requestname,a.managerid,a.managerstr,a.loginid,a.subcompanyid1 as asubcompanyid1,a.departmentid as adepartmentid,a.lastname from (" +unionSqls+") u left join Workflow_Requestbase w on u.requestid = w.requestid  left join ("+hrmSql+") a on u.resourceid = a.id ";
      }

    }
    return unionSqls;
  }

  /**
   * 拼接流程查询sql
   * @param params
   * @return
   */
  public Map<String,String> getFLowSql(Map<String, Object> params, User user){
    RecordSet rs = new RecordSet();
    Map<String,String> sqlMap = new HashMap<>();

    String tabkey = Util.null2String(params.get("tabKey"));
    int kqtype = Util.getIntValue(Util.null2String(params.get("kqtype")),-1);
    String resourceId = Util.null2String(params.get("resourceId"));
    String subCompanyId = Util.null2String(params.get("subCompanyId"));
    String departmentId = Util.null2String(params.get("departmentId"));

    String keyWord = Util.null2String(params.get("keyWord"));
    String fromDate = Util.null2String(params.get("fromDate"));
    String toDate = Util.null2String(params.get("toDate"));
    String typeselect =Util.null2String(params.get("typeselect"));
    String requestId =Util.null2String(params.get("requestId"));
    String viewScope = Util.null2String(params.get("viewScope"));
    String allLevel = Util.null2String(params.get("allLevel"));
    String isNoAccount = Util.null2String(params.get("isNoAccount"));

    String isMyKQ = Util.null2String(params.get("isMyKQ"));
    String not_start_node = Util.null2String(params.get("not_start_node"));
    String not_requestId =Util.null2String(params.get("not_requestId"));
    String custome_sql =Util.null2String(params.get("custome_sql"));
    String workflowid =Util.null2String(params.get("workflowid"));

    String fromSql  = " ";
    //req_requestid 表示只显示Workflow_Requestbase里有的数据
    String sqlWhere = " where 1=1 and req_requestid > 0 ";

    if(typeselect.length()==0){
      typeselect = "3";
    }
    if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
      if(typeselect.equals("1")){
        fromDate = TimeUtil.getCurrentDateString();
        toDate = TimeUtil.getCurrentDateString();
      }else{
        fromDate = TimeUtil.getDateByOption(typeselect,"0");
        toDate = TimeUtil.getDateByOption(typeselect,"1");
      }
    }

    boolean isFinished = false;
    if("1".equalsIgnoreCase(tabkey)){
      //已归档
      isFinished = true;
    }
    //不区分归档不归档 查所有
    boolean isAll = false;
    if("3".equalsIgnoreCase(tabkey)){
      isAll = true;
    }
    if(kqtype > -1){
      if(isFinished){
        fromSql = getFinishedByKQType(""+kqtype,fromDate,toDate,user,isMyKQ);
      }else {
        fromSql = getFieldInfoByKQType(""+kqtype,user,isMyKQ,params);
      }
    }
    if(fromSql.length() == 0){
      return sqlMap;
    }else{
      fromSql = "from ("+fromSql+") f ";
    }

    if(kqtype == 0){
      String newleavetype = Util.null2String(params.get("newleavetype"));
      if(newleavetype.length() > 0){
        sqlWhere += " and newleavetype= "+newleavetype;
      }
    }

    if (keyWord.length() > 0){
      sqlWhere += " and lastname = "+keyWord;
    }

    if(resourceId.length() > 0){
      sqlWhere += " and resourceId in("+resourceId+")";
    }

    if(subCompanyId.length()>0){
      sqlWhere +=" and asubcompanyid1 in("+subCompanyId+") ";
    }

    if(departmentId.length()>0){
      sqlWhere +=" and adepartmentid in("+departmentId+") ";
    }

    if(requestId.length() > 0){
      sqlWhere += " and requestId ="+requestId+"";
    }

    if(not_requestId.length() > 0){
      sqlWhere += " and requestId not in("+not_requestId+")";
    }

    if(viewScope.equals("4")){//我的下属
      if(allLevel.equals("1")){//所有下属
        sqlWhere+=" and managerstr like '%,"+user.getUID()+",%'";
      }else{
        sqlWhere+=" and managerid="+user.getUID();//直接下属
      }
    }
    if (!"1".equals(isNoAccount)) {
      sqlWhere += " and loginid is not null "+(rs.getDBType().equals("oracle")?"":" and loginid<>'' ");
    }

    if(!isFinished){
      if (fromDate.length() > 0 && toDate.length() > 0){
        sqlWhere += " and ( fromDate between '"+fromDate+"' and '"+toDate+"' or toDate between '"+fromDate+"' and '"+toDate+"' "
            + " or '"+fromDate+"' between fromDate and toDate or '"+toDate+"' between fromDate and toDate) ";
      }
    }

    if(isFinished){
      sqlWhere += " and (currentnodetype = '3') ";
    }else{
      if(!isAll){
        sqlWhere += " and (currentnodetype is null or currentnodetype != '3') ";
      }
    }
    if("1".equalsIgnoreCase(not_start_node)){
      sqlWhere += " and (currentnodetype != '0') ";
    }
    if(workflowid.length() > 0){
      sqlWhere += " and workflowid= "+workflowid;
    }
    if(custome_sql.length() > 0){
      sqlWhere += " and ("+custome_sql+") ";
    }
    if("1".equalsIgnoreCase(isMyKQ)){
    }else{
      String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"a");
      String hrmSql = " select id from hrmresource a where 1=1 "+rightSql;
      sqlWhere += " and resourceId in("+hrmSql+")";
    }
    kqLog.info("buildFlowSetTableString"+kqtype+":流程明细sql输出下:fromSql:"+fromSql);
    kqLog.info("buildFlowSetTableString"+kqtype+":流程明细sql输出下:sqlWhere:"+sqlWhere);
    sqlMap.put("from", fromSql);
    sqlMap.put("where", sqlWhere);
    return sqlMap;
  }


  /**
   * 获取归档后的考勤流程数据
   * @param kqtype
   * @param fromDate
   * @param toDate
   * @param user
   * @param isMyKQ 是否来自我的考勤
   * @return
   */
  private String getFinishedByKQType(String kqtype, String fromDate, String toDate, User user,
      String isMyKQ) {
    boolean isLeave = false;
    boolean isEvection = false;
    boolean isOut = false;
    String tableName = "";
    String fields = " distinct requestid,resourceid as resourceId,subcompanyid,departmentid,newleavetype,fromdatedb as fromDate,fromtimedb as fromTime,todatedb as toDate,totimedb as toTime,durationdb,durationrule,SUM(duration) as duration ";
    switch (kqtype){
      case "0":
        isLeave= true;
        tableName = KqSplitFlowTypeEnum.LEAVE.getTablename();
        break;
      case "1":
        isEvection= true;
        tableName = KqSplitFlowTypeEnum.EVECTION.getTablename();
        break;
      case "2":
        isOut= true;
        tableName = KqSplitFlowTypeEnum.OUT.getTablename();
        break;
      case "3":
        tableName = KqSplitFlowTypeEnum.OVERTIME.getTablename();
        break;
      default:
        break;
    }
    if(tableName.length() == 0){
      kqLog.info("getFinishedByKQType:获取归档的考勤流程出问题了:kqtype:"+kqtype);
      return "";
    }
    if(isLeave){
      String back_table_name= KqSplitFlowTypeEnum.LEAVEBACK.getTablename();
      String table_name= KqSplitFlowTypeEnum.LEAVE.getTablename();
      String backSql = "select sum(duration) from "+back_table_name+"  where "+back_table_name+".leavebackrequestid="+table_name+".requestid and "+back_table_name+".newleavetype="+table_name+".newleavetype and "+back_table_name+".resourceid="+table_name+".resourceid ";
      if(fromDate.length() > 0 && toDate.length() > 0){
        if("1".equalsIgnoreCase(isMyKQ)){
          backSql += " and fromDate between'"+fromDate+"' and '"+toDate+"'";
        }else{
          backSql += " and belongdate between'"+fromDate+"' and '"+toDate+"'";
        }

      }
      fields = " distinct requestid,resourceid as resourceId,subcompanyid,departmentid,newleavetype,fromdatedb as fromDate,fromtimedb as fromTime,todatedb as toDate,totimedb as toTime,durationdb,durationrule,SUM(duration) as duration,"
          + "("+backSql+") as backduraion ";
    }

    String baseSql = " select "+fields+" from "+tableName+" where tablenamedb is not null and (status is null or status != 1)  ";
    if(fromDate.length() > 0 && toDate.length() > 0){
      if("1".equalsIgnoreCase(isMyKQ)){
        baseSql += " and fromDate between'"+fromDate+"' and '"+toDate+"'";
      }else{
        baseSql += " and belongdate between'"+fromDate+"' and '"+toDate+"'";
      }
    }
    baseSql += " GROUP BY requestid,resourceid,subcompanyid,departmentid,newleavetype,fromdatedb,fromtimedb,todatedb,totimedb,durationdb,durationrule ";

    String unionsql = " select u.*,w.currentnodetype,w.requestname,w.requestid as req_requestid,w.status as flowstatus,w.workflowid,a.managerid,a.managerstr,a.loginid,a.subcompanyid1 as asubcompanyid1,a.departmentid as adepartmentid,a.lastname from ("+baseSql+") u left join Workflow_Requestbase w on u.requestid = w.requestid  left join hrmresource a on u.resourceid = a.id ";
    new BaseBean().writeLog("==zj==(外网考勤明细)" + JSON.toJSONString(unionsql));
    return unionsql;
  }

  /**
   * 考勤报表明细
   * @param params
   * @param user
   * @return
   */
  public String buildFlowSetTableString(Map<String, Object> params, User user){
    int kqtype = Util.getIntValue(Util.null2String(params.get("kqtype")),-1);
    String isMyKQ = Util.null2String(params.get("isMyKQ"));
    if(!"1".equalsIgnoreCase(isMyKQ)){
      if(kqtype == 3){
        String tabkey = Util.null2String(params.get("tabKey"));
        //加班的单独走一个列表显示
        if(OverTimeComputingModeEnum.FLOW.getComputingMode().equalsIgnoreCase(tabkey)){
          return buildTableString4OvertimeFlow(params,user);
        }else if(OverTimeComputingModeEnum.FLOW2CARD.getComputingMode().equalsIgnoreCase(tabkey)){
          return buildTableString4OvertimeFlow2Card(params,user);
        }else if(OverTimeComputingModeEnum.CARD.getComputingMode().equalsIgnoreCase(tabkey)){
          return buildTableString4OvertimeCard(params,user);
        }else if(OverTimeComputingModeEnum.FLOWINCARD.getComputingMode().equalsIgnoreCase(tabkey)){
          return buildTableString4OvertimeFlowInCard(params,user);
        }
      }
    }

    String backfields = " * ";
    String fromSql  = "";
    String sqlWhere = "";
    String tableString = "";
    String tabletype="none";
    String orderby = "";
    boolean isFinished = false;
    String tabkey = Util.null2String(params.get("tabKey"));
    if("1".equalsIgnoreCase(tabkey)){
      //已归档
      isFinished = true;
    }
    String typeselect =Util.null2String(params.get("typeselect"));
    String fromDate = Util.null2String(params.get("fromDate"));
    String toDate = Util.null2String(params.get("toDate"));

    //在这里生成sql语句
    Map<String,String> sqlMap = getFLowSql(params,user);
    if(sqlMap.isEmpty()){
      //给个默认表 比如请假表，因为表结构都是一样的
      String tableName = KqSplitFlowTypeEnum.LEAVE.getTablename();
      fromSql = tableName;
      sqlWhere = " where 1=2 ";
    }else{
      fromSql = Util.null2String(sqlMap.get("from"));
      sqlWhere = Util.null2String(sqlMap.get("where"));
    }

    String fromTimePram = kqtype+"+0+"+user.getLanguage()+"+column:newLeaveType";
    if(isFinished){
      fromTimePram = kqtype+"+0+"+user.getLanguage()+"+column:newLeaveType"+"+column:durationrule";
    }
    String toTimePram = kqtype+"+1+"+user.getLanguage()+"+column:newLeaveType";
    if(isFinished){
      toTimePram = kqtype+"+1+"+user.getLanguage()+"+column:newLeaveType"+"+column:durationrule";
    }

    String otherPram = kqtype+"+column:durationrule+"+user.getLanguage();
    if(kqtype == 0){
      otherPram +="+column:newLeaveType+column:requestid+column:backduraion";
    }
	
	int languageId = user.getLanguage();

    // #1475814-概述：满足考勤报分部部门显示及导出时显示全路径需求
    String transMethodString = HrmUtil.getKqDepartmentTransMethod();

    String pageUid = "cb9b9b02-a34c-4468-b871-08167bcaeb6c";

    tableString =" <table pageUid=\""+pageUid+"\" tabletype=\""+tabletype+"\" pagesize=\"10\" >"+
//        " <checkboxpopedom showmethod=\"com.engine.kq.cmd.shiftmanagement.toolkit.ShiftManagementToolKit.getCheckbox\"  id=\"checkbox\"  popedompara=\"column:id\" />"+
        "	   <sql backfields=\""+backfields+"\" sqlform=\""+fromSql+"\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"  sqlorderby=\""+orderby+"\"  sqlprimarykey=\"a.id\" sqlsortway=\"desc\" sqlisdistinct=\"false\"/>"+
        "			<head>";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(413,user.getLanguage()) +"\" column=\"lastname\" orderkey=\"lastname\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(714,user.getLanguage()) +"\" column=\"resourceId\" orderkey=\"resourceId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getWorkcode\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(124,user.getLanguage()) +"\" column=\"departmentId\" orderkey=\"departmentId\" transmethod=\"com.engine.hrm.util.HrmUtil.getKqDepartmentTransMethodnew\"  otherpara=\"column:resourceId\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(740,user.getLanguage()) +"\" column=\"fromDate\" orderkey=\"fromDate\" transmethod=\"com.engine.kq.util.TransMethod.getRequestLink\" otherpara=\"column:req_requestid\"  />";

    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(742,user.getLanguage()) +"\" column=\"fromTime\" orderkey=\"fromTime\" transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+fromTimePram+"\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(741,user.getLanguage()) +"\" column=\"toDate\" orderkey=\"toDate\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(743,user.getLanguage()) +"\" column=\"toTime\" orderkey=\"toTime\" transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+toTimePram+"\"  />";
    if(kqtype == 0){
      tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(1881,user.getLanguage()) +"\" column=\"newleavetype\" orderkey=\"newleavetype\" transmethod=\"com.engine.kq.util.TransMethod.getLeavetype\" otherpara=\""+languageId+"\" />";
    }
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(15378,user.getLanguage()) +"\" column=\"flowstatus\" orderkey=\"flowstatus\" />";
    if(isFinished){
      tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(21551,user.getLanguage()) +"\" column=\"duration\" orderkey=\"duration\" transmethod=\"com.engine.kq.util.TransMethod.getFlowDurationByUnit\" otherpara=\""+otherPram+"\"  />";
    }
    tableString +="			</head>"+
        " </table>";
    String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
    Util_TableMap.setVal(sessionkey, tableString);
    return sessionkey;
  }


  /**
   * 流程 生成加班明细
   * @param params
   * @param user
   * @return
   */
  public String buildTableString4OvertimeFlow(Map<String, Object> params, User user){
    String dbType = new RecordSet().getDBType();
    String orgindbtype = new RecordSet().getOrgindbtype();
    String backfields = " * ";
    String fromDate = Util.null2String(params.get("fromDate"));
    String toDate = Util.null2String(params.get("toDate"));
    String typeselect =Util.null2String(params.get("typeselect"));
    String belongdateWhere = "";
    if(typeselect.length()==0){
      typeselect = "3";
    }
    if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
      if(typeselect.equals("1")){
        fromDate = TimeUtil.getCurrentDateString();
        toDate = TimeUtil.getCurrentDateString();
      }else{
        fromDate = TimeUtil.getDateByOption(typeselect,"0");
        toDate = TimeUtil.getDateByOption(typeselect,"1");
      }
    }

    if (fromDate.length() > 0 && toDate.length() > 0){
      belongdateWhere += " and ( belongdate between '"+fromDate+"' and '"+toDate+"' or belongdate between '"+fromDate+"' and '"+toDate+"' "
          + " or '"+fromDate+"' between belongdate and belongdate or '"+toDate+"' between belongdate and belongdate) ";
    }

    String overtimeTable = "";

    if("oracle".equalsIgnoreCase(dbType)){
      overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,durationrule,to_char(sum(duration_min)) duration_min from kq_flow_overtime where computingmode=1 "+(belongdateWhere.length() == 0 ? "" : belongdateWhere)+" group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,durationrule ";
      if("jc".equalsIgnoreCase(orgindbtype)){
        overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,durationrule,to_char(sum(cast(duration_min as NUMERIC))) duration_min from kq_flow_overtime where computingmode=1 "+(belongdateWhere.length() == 0 ? "" : belongdateWhere)+" group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,durationrule ";
      }
    }else if("postgresql".equalsIgnoreCase(dbType)){
      overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,durationrule,sum(cast(duration_min as NUMERIC))  duration_min from kq_flow_overtime where computingmode=1 "+(belongdateWhere.length() == 0 ? "" : belongdateWhere)+" group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,durationrule ";
    }else if("mysql".equalsIgnoreCase(dbType)) {
      overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,durationrule,CONVERT (sum(duration_min), CHAR ) duration_min from kq_flow_overtime where computingmode=1 " + (belongdateWhere.length() == 0 ? "" : belongdateWhere) + " group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,durationrule ";
    }else{
      overtimeTable = " select requestid,resourceid,fromdatedb fromdate,fromtimedb fromtime,todatedb todate,totimedb totime,computingmode,paidLeaveEnable,expiringdate,durationrule,convert(varchar,sum(cast(duration_min as NUMERIC)))  duration_min  from kq_flow_overtime where computingmode=1 "+(belongdateWhere.length() == 0 ? "" : belongdateWhere)+" group by requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,durationrule ";
    }
    String fromSql  = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from ("+overtimeTable+") a left join hrmresource b on a.resourceid = b.id) f ";
    String sqlWhere = " 1=1 ";
    String tableString = "";
    String tabletype="none";
    String orderby = " computingmode ";

    String tabkey = Util.null2String(params.get("tabKey"));
    int kqtype = Util.getIntValue(Util.null2String(params.get("kqtype")),-1);
    String resourceId = Util.null2String(params.get("resourceId"));
    String subCompanyId = Util.null2String(params.get("subCompanyId"));
    String departmentId = Util.null2String(params.get("departmentId"));

	//来自我的考勤,resourceid为空时，默认取当前用户的id
	String source =Util.null2String(params.get("source"));
    if(source.equals("isMyKq")){
      if(resourceId.equals("")){
        resourceId = user.getUID()+"";
      }
    }
    kqLog.info(">>>>>>source:"+source+">>>>>resourceId:"+resourceId);
	
    String keyWord = Util.null2String(params.get("keyWord"));
    String requestId =Util.null2String(params.get("requestId"));
    String viewScope = Util.null2String(params.get("viewScope"));
    String allLevel = Util.null2String(params.get("allLevel"));
    String isNoAccount = Util.null2String(params.get("isNoAccount"));

    String isMyKQ = Util.null2String(params.get("isMyKQ"));

    boolean isFinished = false;
    if("1".equalsIgnoreCase(tabkey)){
      //已归档
      isFinished = true;
    }

    if (keyWord.length() > 0){
      sqlWhere += " and lastname = "+keyWord;
    }

    if(resourceId.length() > 0){
      sqlWhere += " and resourceId in("+resourceId+")";
    }

    if(subCompanyId.length()>0){
      sqlWhere +=" and subcompanyid1 in("+subCompanyId+") ";
    }

    if(departmentId.length()>0){
      sqlWhere +=" and departmentid in("+departmentId+") ";
    }

    if(viewScope.equals("4")){//我的下属
      if(allLevel.equals("1")){//所有下属
        sqlWhere+=" and managerstr like '%,"+user.getUID()+",%'";
      }else{
        sqlWhere+=" and managerid="+user.getUID();//直接下属
      }
    }
    if (!"1".equals(isNoAccount)) {
      sqlWhere += " and loginid is not null "+(new RecordSet().getDBType().equals("oracle")?"":" and loginid<>'' ");
    }

    String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"aa");
    String hrmSql = " select id from hrmresource aa where 1=1 "+rightSql;
    if (null != rightSql && rightSql.length()>0) {
      sqlWhere += " and resourceId in("+hrmSql+")";
    }

//    if (fromDate.length() > 0 && toDate.length() > 0){
//      sqlWhere += " and ( fromDate between '"+fromDate+"' and '"+toDate+"' or toDate between '"+fromDate+"' and '"+toDate+"' "
//          + " or '"+fromDate+"' between fromDate and toDate or '"+toDate+"' between fromDate and toDate) ";
//    }
    String otherPram = user.getLanguage()+"";

    String fromTimePram = kqtype+"+0+"+user.getLanguage()+"++column:durationrule";
    String toTimePram = kqtype+"+1+"+user.getLanguage()+"++column:durationrule";

    // #1475814-概述：满足考勤报分部部门显示及导出时显示全路径需求
    String transMethodString = HrmUtil.getKqDepartmentTransMethod();

    String pageUid = PageUidFactory.getHrmPageUid("KQ_OvertimeTotal_Detail_Flow");
    String sql = "backfields>>>>"+backfields+">>>fromSql>>>"+fromSql+">>>sqlWhere>>>"+sqlWhere;
    new BaseBean().writeLog("sql>>>>>>>>>"+sql);
    tableString =" <table pageUid=\""+pageUid+"\" tabletype=\""+tabletype+"\" pagesize=\"10\" >"+
        "	   <sql backfields=\""+backfields+"\" sqlform=\""+fromSql+"\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"  sqlorderby=\""+orderby+"\"  sqlprimarykey=\"a.id\" sqlsortway=\"desc\" sqlisdistinct=\"false\"/>"+
        "			<head>";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(413,user.getLanguage()) +"\" column=\"lastname\" orderkey=\"lastname\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(714,user.getLanguage()) +"\" column=\"resourceId\" orderkey=\"resourceId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getWorkcode\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(124,user.getLanguage()) +"\" column=\"departmentId\" orderkey=\"departmentId\" transmethod=\""+transMethodString+"\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(740,user.getLanguage()) +"\" column=\"fromdate\" orderkey=\"fromdate\" transmethod=\"com.engine.kq.util.TransMethod.getRequestLink\" otherpara=\"column:requestid\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(742,user.getLanguage()) +"\" column=\"fromtime\" orderkey=\"fromtime\" transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+fromTimePram+"\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(741,user.getLanguage()) +"\" column=\"todate\" orderkey=\"todate\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(743,user.getLanguage()) +"\" column=\"totime\" orderkey=\"totime\"  transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+toTimePram+"\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(509604,user.getLanguage()) +"\" column=\"duration_min\" orderkey=\"duration_min\" transmethod=\"com.engine.kq.util.TransMethod.getDuration_minByUnit\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(125804,user.getLanguage()) +"\" column=\"paidLeaveEnable\" orderkey=\"paidLeaveEnable\" transmethod=\"com.engine.kq.util.TransMethod.getPaidLeaveEnable\" otherpara=\""+otherPram+"\"  />";
    tableString +="			</head>"+
        " </table>";
    String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
    Util_TableMap.setVal(sessionkey, tableString);
    return sessionkey;
  }

  /**
   * 打卡+流程 生成加班明细
   * @param params
   * @param user
   */
  private String buildTableString4OvertimeFlow2Card(Map<String, Object> params, User user) {
    String backfields = " * ";
    String fromDate = Util.null2String(params.get("fromDate"));
    String toDate = Util.null2String(params.get("toDate"));
    String typeselect =Util.null2String(params.get("typeselect"));
    String belongdateWhere = "";
    if(typeselect.length()==0){
      typeselect = "3";
    }
    if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
      if(typeselect.equals("1")){
        fromDate = TimeUtil.getCurrentDateString();
        toDate = TimeUtil.getCurrentDateString();
      }else{
        fromDate = TimeUtil.getDateByOption(typeselect,"0");
        toDate = TimeUtil.getDateByOption(typeselect,"1");
      }
    }

    if (fromDate.length() > 0 && toDate.length() > 0){
      belongdateWhere += " and ( belongdate between '"+fromDate+"' and '"+toDate+"' or belongdate between '"+fromDate+"' and '"+toDate+"' "
          + " or '"+fromDate+"' between belongdate and belongdate or '"+toDate+"' between belongdate and belongdate) ";
    }
    String overtimeTable = " select requestid,resourceid,fromdate,fromtime,todate,totime,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,duration_min,durationrule from kq_flow_overtime where computingmode = 2  "+(belongdateWhere.length() == 0 ? "" :belongdateWhere);

    String fromSql  = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from ("+overtimeTable+") a left join hrmresource b on a.resourceid = b.id) f ";
    String sqlWhere = " 1=1 ";
    String tableString = "";
    String tabletype="none";
    String orderby = " computingmode ";

    String tabkey = Util.null2String(params.get("tabKey"));
    int kqtype = Util.getIntValue(Util.null2String(params.get("kqtype")),-1);
    String resourceId = Util.null2String(params.get("resourceId"));
    String subCompanyId = Util.null2String(params.get("subCompanyId"));
    String departmentId = Util.null2String(params.get("departmentId"));

	//来自我的考勤,resourceid为空时，默认取当前用户的id
	String source =Util.null2String(params.get("source"));
    if(source.equals("isMyKq")){
      if(resourceId.equals("")){
        resourceId = user.getUID()+"";
      }
    }

    String keyWord = Util.null2String(params.get("keyWord"));
    String requestId =Util.null2String(params.get("requestId"));
    String viewScope = Util.null2String(params.get("viewScope"));
    String allLevel = Util.null2String(params.get("allLevel"));
    String isNoAccount = Util.null2String(params.get("isNoAccount"));

    String isMyKQ = Util.null2String(params.get("isMyKQ"));

    boolean isFinished = false;
    if("1".equalsIgnoreCase(tabkey)){
      //已归档
      isFinished = true;
    }

    if (keyWord.length() > 0){
      sqlWhere += " and lastname = "+keyWord;
    }

    if(resourceId.length() > 0){
      sqlWhere += " and resourceId in("+resourceId+")";
    }

    if(subCompanyId.length()>0){
      sqlWhere +=" and subcompanyid1 in("+subCompanyId+") ";
    }

    if(departmentId.length()>0){
      sqlWhere +=" and departmentid in("+departmentId+") ";
    }

    if(viewScope.equals("4")){//我的下属
      if(allLevel.equals("1")){//所有下属
        sqlWhere+=" and managerstr like '%,"+user.getUID()+",%'";
      }else{
        sqlWhere+=" and managerid="+user.getUID();//直接下属
      }
    }
    if (!"1".equals(isNoAccount)) {
      sqlWhere += " and loginid is not null "+(new RecordSet().getDBType().equals("oracle")?"":" and loginid<>'' ");
    }

    String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"aa");
    String hrmSql = " select id from hrmresource aa where 1=1 "+rightSql;
    if (null != rightSql && rightSql.length()>0) {
      sqlWhere += " and resourceId in("+hrmSql+")";
    }

    kqLog.info("buildTableString4OvertimeFlow2Card>>>>>>backfields:"+backfields+">>>>>fromSql:"+fromSql
        +">>>>>sqlWhere:"+sqlWhere);
    String otherPram = user.getLanguage()+"";
    String timePram = "column:fromtimedb+column:todatedb+column:totimedb";
    String fromTimePram = kqtype+"+0+"+user.getLanguage()+"++column:durationrule";
    String toTimePram = kqtype+"+1+"+user.getLanguage()+"++column:durationrule";

    // #1475814-概述：满足考勤报分部部门显示及导出时显示全路径需求
    String transMethodString = HrmUtil.getKqDepartmentTransMethod();

    String pageUid = PageUidFactory.getHrmPageUid("KQ_OvertimeTotal_Detail_Flow2Card");

    tableString =" <table pageUid=\""+pageUid+"\" tabletype=\""+tabletype+"\" pagesize=\"10\" >"+
        "	   <sql backfields=\""+backfields+"\" sqlform=\""+fromSql+"\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"  sqlorderby=\""+orderby+"\"  sqlprimarykey=\"a.id\" sqlsortway=\"desc\" sqlisdistinct=\"false\"/>"+
        "			<head>";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(413,user.getLanguage()) +"\" column=\"lastname\" orderkey=\"lastname\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(714,user.getLanguage()) +"\" column=\"resourceId\" orderkey=\"resourceId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getWorkcode\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(124,user.getLanguage()) +"\" column=\"departmentId\" orderkey=\"departmentId\" transmethod=\""+transMethodString+"\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(740,user.getLanguage()) +"\" column=\"fromdate\" orderkey=\"fromdate\"  transmethod=\"com.engine.kq.util.TransMethod.getRequestLink\" otherpara=\"column:requestid\"  />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(742,user.getLanguage()) +"\" column=\"fromtime\" orderkey=\"fromtime\" transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+fromTimePram+"\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(741,user.getLanguage()) +"\" column=\"todate\" orderkey=\"todate\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(743,user.getLanguage()) +"\" column=\"totime\" orderkey=\"totime\" transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+toTimePram+"\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(18949,user.getLanguage()) +"\" column=\"fromdatedb\" orderkey=\"fromdatedb\"  transmethod=\"com.engine.kq.util.TransMethod.getOvertimeCard\" otherpara=\""+timePram+"\"  />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(509604,user.getLanguage()) +"\" column=\"duration_min\" orderkey=\"duration_min\" transmethod=\"com.engine.kq.util.TransMethod.getDuration_minByUnit\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(125804,user.getLanguage()) +"\" column=\"paidLeaveEnable\" orderkey=\"paidLeaveEnable\" transmethod=\"com.engine.kq.util.TransMethod.getPaidLeaveEnable\" otherpara=\""+otherPram+"\"  />";
    tableString +="			</head>"+
        " </table>";
    String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
    Util_TableMap.setVal(sessionkey, tableString);
    return sessionkey;
  }

  /**
   * 打卡 生成加班明细
   * @param params
   * @param user
   */
  private String buildTableString4OvertimeCard(Map<String, Object> params, User user) {
    String backfields = " * ";
    String fromDate = Util.null2String(params.get("fromDate"));
    String toDate = Util.null2String(params.get("toDate"));
    String typeselect =Util.null2String(params.get("typeselect"));
    String belongdateWhere = "";
    if(typeselect.length()==0){
      typeselect = "3";
    }
    if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
      if(typeselect.equals("1")){
        fromDate = TimeUtil.getCurrentDateString();
        toDate = TimeUtil.getCurrentDateString();
      }else{
        fromDate = TimeUtil.getDateByOption(typeselect,"0");
        toDate = TimeUtil.getDateByOption(typeselect,"1");
      }
    }

    if (fromDate.length() > 0 && toDate.length() > 0){
      belongdateWhere += " and ( belongdate between '"+fromDate+"' and '"+toDate+"' or belongdate between '"+fromDate+"' and '"+toDate+"' "
          + " or '"+fromDate+"' between belongdate and belongdate or '"+toDate+"' between belongdate and belongdate) ";
    }
    String overtimeTable = " select requestid,resourceid,fromdate,fromtime,todate,totime,computingmode,paidLeaveEnable,expiringdate,duration_min from kq_flow_overtime where computingmode = 3  "+(belongdateWhere.length() == 0 ? "" : belongdateWhere);

    String fromSql  = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from ("+overtimeTable+") a left join hrmresource b on a.resourceid = b.id) f ";
    String sqlWhere = " 1=1 ";
    String tableString = "";
    String tabletype="none";
    String orderby = " computingmode ";

    String tabkey = Util.null2String(params.get("tabKey"));
    int kqtype = Util.getIntValue(Util.null2String(params.get("kqtype")),-1);
    String resourceId = Util.null2String(params.get("resourceId"));
    String subCompanyId = Util.null2String(params.get("subCompanyId"));
    String departmentId = Util.null2String(params.get("departmentId"));

	//来自我的考勤,resourceid为空时，默认取当前用户的id
	String source =Util.null2String(params.get("source"));
    if(source.equals("isMyKq")){
      if(resourceId.equals("")){
        resourceId = user.getUID()+"";
      }
    }
    kqLog.info(">>>>>>source:"+source+">>>>>resourceId:"+resourceId);
	
    String keyWord = Util.null2String(params.get("keyWord"));
    String requestId =Util.null2String(params.get("requestId"));
    String viewScope = Util.null2String(params.get("viewScope"));
    String allLevel = Util.null2String(params.get("allLevel"));
    String isNoAccount = Util.null2String(params.get("isNoAccount"));

    String isMyKQ = Util.null2String(params.get("isMyKQ"));

    boolean isFinished = false;
    if("1".equalsIgnoreCase(tabkey)){
      //已归档
      isFinished = true;
    }

    if (keyWord.length() > 0){
      sqlWhere += " and lastname = "+keyWord;
    }

    if(resourceId.length() > 0){
      sqlWhere += " and resourceId in("+resourceId+")";
    }

    if(subCompanyId.length()>0){
      sqlWhere +=" and subcompanyid1 in("+subCompanyId+") ";
    }

    if(departmentId.length()>0){
      sqlWhere +=" and departmentid in("+departmentId+") ";
    }

    if(viewScope.equals("4")){//我的下属
      if(allLevel.equals("1")){//所有下属
        sqlWhere+=" and managerstr like '%,"+user.getUID()+",%'";
      }else{
        sqlWhere+=" and managerid="+user.getUID();//直接下属
      }
    }
    if (!"1".equals(isNoAccount)) {
      sqlWhere += " and loginid is not null "+(new RecordSet().getDBType().equals("oracle")?"":" and loginid<>'' ");
    }
    String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"aa");
    String hrmSql = " select id from hrmresource aa where 1=1 "+rightSql;
    if (null != rightSql && rightSql.length()>0) {
      sqlWhere += " and resourceId in("+hrmSql+")";
    }

//    if (fromDate.length() > 0 && toDate.length() > 0){
//      sqlWhere += " and ( fromDate between '"+fromDate+"' and '"+toDate+"' or toDate between '"+fromDate+"' and '"+toDate+"' "
//          + " or '"+fromDate+"' between fromDate and toDate or '"+toDate+"' between fromDate and toDate) ";
//    }
    String otherPram = user.getLanguage()+"";

    // #1475814-概述：满足考勤报分部部门显示及导出时显示全路径需求
    String transMethodString = HrmUtil.getKqDepartmentTransMethod();

    String pageUid = PageUidFactory.getHrmPageUid("KQ_OvertimeTotal_Detail_Card");

    tableString =" <table pageUid=\""+pageUid+"\" tabletype=\""+tabletype+"\" pagesize=\"10\" >"+
        "	   <sql backfields=\""+backfields+"\" sqlform=\""+fromSql+"\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"  sqlorderby=\""+orderby+"\"  sqlprimarykey=\"a.id\" sqlsortway=\"desc\" sqlisdistinct=\"false\"/>"+
        "			<head>";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(413,user.getLanguage()) +"\" column=\"lastname\" orderkey=\"lastname\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(714,user.getLanguage()) +"\" column=\"resourceId\" orderkey=\"resourceId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getWorkcode\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(124,user.getLanguage()) +"\" column=\"departmentId\" orderkey=\"departmentId\" transmethod=\""+transMethodString+"\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(740,user.getLanguage()) +"\" column=\"fromdate\" orderkey=\"fromdate\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(742,user.getLanguage()) +"\" column=\"fromtime\" orderkey=\"fromtime\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(741,user.getLanguage()) +"\" column=\"todate\" orderkey=\"todate\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(743,user.getLanguage()) +"\" column=\"totime\" orderkey=\"totime\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(509604,user.getLanguage()) +"\" column=\"duration_min\" orderkey=\"duration_min\" transmethod=\"com.engine.kq.util.TransMethod.getDuration_minByUnit\" />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(125804,user.getLanguage()) +"\" column=\"paidLeaveEnable\" orderkey=\"paidLeaveEnable\" transmethod=\"com.engine.kq.util.TransMethod.getPaidLeaveEnable\" otherpara=\""+otherPram+"\"  />";
    tableString +="			</head>"+
        " </table>";
    String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
    Util_TableMap.setVal(sessionkey, tableString);
    return sessionkey;
  }

  /**
   * 打卡+流程 取交集 生成加班明细
   * @param params
   * @param user
   */
  private String buildTableString4OvertimeFlowInCard(Map<String, Object> params, User user) {
    String backfields = " * ";
    String fromDate = Util.null2String(params.get("fromDate"));
    String toDate = Util.null2String(params.get("toDate"));
    String typeselect =Util.null2String(params.get("typeselect"));
    String belongdateWhere = "";
    if(typeselect.length()==0){
      typeselect = "3";
    }
    if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
      if(typeselect.equals("1")){
        fromDate = TimeUtil.getCurrentDateString();
        toDate = TimeUtil.getCurrentDateString();
      }else{
        fromDate = TimeUtil.getDateByOption(typeselect,"0");
        toDate = TimeUtil.getDateByOption(typeselect,"1");
      }
    }

    if (fromDate.length() > 0 && toDate.length() > 0){
      belongdateWhere += " and ( belongdate between '"+fromDate+"' and '"+toDate+"' or belongdate between '"+fromDate+"' and '"+toDate+"' "
          + " or '"+fromDate+"' between belongdate and belongdate or '"+toDate+"' between belongdate and belongdate) ";
    }
    String overtimeTable = " select requestid,resourceid,fromdate,fromtime,todate,totime,fromdatedb,fromtimedb,todatedb,totimedb,computingmode,paidLeaveEnable,expiringdate,duration_min,durationrule from kq_flow_overtime where computingmode = 4  "+(belongdateWhere.length() == 0 ? "" :belongdateWhere);

    String fromSql  = " (select b.lastname,b.loginid,b.subcompanyid1,b.departmentid,b.jobtitle,b.managerid,b.managerstr,a.* from ("+overtimeTable+") a left join hrmresource b on a.resourceid = b.id) f ";
    String sqlWhere = " 1=1 ";
    String tableString = "";
    String tabletype="none";
    String orderby = " computingmode ";

    String tabkey = Util.null2String(params.get("tabKey"));
    int kqtype = Util.getIntValue(Util.null2String(params.get("kqtype")),-1);
    String resourceId = Util.null2String(params.get("resourceId"));
    String subCompanyId = Util.null2String(params.get("subCompanyId"));
    String departmentId = Util.null2String(params.get("departmentId"));

    //来自我的考勤,resourceid为空时，默认取当前用户的id
    String source =Util.null2String(params.get("source"));
    if(source.equals("isMyKq")){
      if(resourceId.equals("")){
        resourceId = user.getUID()+"";
      }
    }

    String keyWord = Util.null2String(params.get("keyWord"));
    String requestId =Util.null2String(params.get("requestId"));
    String viewScope = Util.null2String(params.get("viewScope"));
    String allLevel = Util.null2String(params.get("allLevel"));
    String isNoAccount = Util.null2String(params.get("isNoAccount"));

    String isMyKQ = Util.null2String(params.get("isMyKQ"));

    boolean isFinished = false;
    if("1".equalsIgnoreCase(tabkey)){
      //已归档
      isFinished = true;
    }

    if (keyWord.length() > 0){
      sqlWhere += " and lastname = "+keyWord;
    }

    if(resourceId.length() > 0){
      sqlWhere += " and resourceId in("+resourceId+")";
    }

    if(subCompanyId.length()>0){
      sqlWhere +=" and subcompanyid1 in("+subCompanyId+") ";
    }

    if(departmentId.length()>0){
      sqlWhere +=" and departmentid in("+departmentId+") ";
    }

    if(viewScope.equals("4")){//我的下属
      if(allLevel.equals("1")){//所有下属
        sqlWhere+=" and managerstr like '%,"+user.getUID()+",%'";
      }else{
        sqlWhere+=" and managerid="+user.getUID();//直接下属
      }
    }
    if (!"1".equals(isNoAccount)) {
      sqlWhere += " and loginid is not null "+(new RecordSet().getDBType().equals("oracle")?"":" and loginid<>'' ");
    }

    String rightSql = new KQReportBiz().getReportRight("1",""+user.getUID(),"aa");
    String hrmSql = " select id from hrmresource aa where 1=1 "+rightSql;
    if (null != rightSql && rightSql.length()>0) {
      sqlWhere += " and resourceId in("+hrmSql+")";
    }

    kqLog.info("KQ_OvertimeTotal_Detail_FlowInCard>>>>>>backfields:"+backfields+">>>>>fromSql:"+fromSql
        +">>>>>sqlWhere:"+sqlWhere);
    String otherPram = user.getLanguage()+"";
    String timePram = "column:fromtime+column:todate+column:totime";
    String fromTimePram = kqtype+"+0+"+user.getLanguage()+"++column:durationrule";
    String toTimePram = kqtype+"+1+"+user.getLanguage()+"++column:durationrule";

    // #1475814-概述：满足考勤报分部部门显示及导出时显示全路径需求
    String transMethodString = HrmUtil.getKqDepartmentTransMethod();

    String pageUid = PageUidFactory.getHrmPageUid("KQ_OvertimeTotal_Detail_FlowInCard");

    tableString =" <table pageUid=\""+pageUid+"\" tabletype=\""+tabletype+"\" pagesize=\"10\" >"+
        "	   <sql backfields=\""+backfields+"\" sqlform=\""+fromSql+"\" sqlwhere=\""+Util.toHtmlForSplitPage(sqlWhere)+"\"  sqlorderby=\""+orderby+"\"  sqlprimarykey=\"a.id\" sqlsortway=\"desc\" sqlisdistinct=\"false\"/>"+
        "			<head>";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(413,user.getLanguage()) +"\" column=\"lastname\" orderkey=\"lastname\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(714,user.getLanguage()) +"\" column=\"resourceId\" orderkey=\"resourceId\" transmethod=\"weaver.hrm.resource.ResourceComInfo.getWorkcode\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(124,user.getLanguage()) +"\" column=\"departmentId\" orderkey=\"departmentId\" transmethod=\""+transMethodString+"\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(740,user.getLanguage()) +"\" column=\"fromdatedb\" orderkey=\"fromdatedb\"  transmethod=\"com.engine.kq.util.TransMethod.getRequestLink\" otherpara=\"column:requestid\"  />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(742,user.getLanguage()) +"\" column=\"fromtimedb\" orderkey=\"fromtimedb\" transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+fromTimePram+"\"  />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(741,user.getLanguage()) +"\" column=\"todatedb\" orderkey=\"todatedb\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(743,user.getLanguage()) +"\" column=\"totimedb\" orderkey=\"totimedb\"  transmethod=\"com.engine.kq.util.TransMethod.getFlowTimeByUnit\" otherpara=\""+toTimePram+"\"  />";
    tableString += "				<col width=\"20%\"  text=\""+ SystemEnv.getHtmlLabelName(18949,user.getLanguage()) +"\" column=\"fromdate\" orderkey=\"fromdate\"  transmethod=\"com.engine.kq.util.TransMethod.getOvertimeCard\" otherpara=\""+timePram+"\"  />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(509604,user.getLanguage()) +"\" column=\"duration_min\" orderkey=\"duration_min\" transmethod=\"com.engine.kq.util.TransMethod.getDuration_minByUnit\" />";
    tableString += "				<col width=\"10%\"  text=\""+ SystemEnv.getHtmlLabelName(125804,user.getLanguage()) +"\" column=\"paidLeaveEnable\" orderkey=\"paidLeaveEnable\" transmethod=\"com.engine.kq.util.TransMethod.getPaidLeaveEnable\" otherpara=\""+otherPram+"\"  />";
    tableString +="			</head>"+
        " </table>";
    String sessionkey = pageUid + "_" + Util.getEncrypt(Util.getRandom());
    Util_TableMap.setVal(sessionkey, tableString);
    return sessionkey;
  }
}
