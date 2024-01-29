package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.engine.kq.enums.FlowReportTypeEnum;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.wfset.bean.SplitBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

/**
 * 考勤流程数据 相关类
 */
public class KQFlowDataBiz {

  private String resourceid;
  private String fromDate;
  private String toDate;
  private String fromTime;
  private String toTime;
  private String belongDate;
  private String newLeaveType;
  private String orderby_sql;

  public KQFlowDataBiz(FlowDataParamBuilder build){
    this.resourceid = build.resourceid;
    this.fromDate = build.fromDate;
    this.toDate = build.toDate;
    this.fromTime = build.fromTime;
    this.toTime = build.toTime;
    this.belongDate = build.belongDate;
    this.newLeaveType = build.newLeaveType;
    this.orderby_sql = build.orderby_sql;
  }

  /**
   * 获取所有的考勤数据
   * 请假，出差，公出，加班的
   * @param flowMaps
   * @param isAll true的时候也返回加班的，false的时候不返回加班的，只返回可以抵扣异常的
   * @return
   */
  public List<SplitBean> getAllFlowData(Map<String,Object> flowMaps,boolean isAll){
    List<SplitBean> allSplitBeans = new ArrayList<>();

    Map<String,String> flowDeductCard = getFlowDeductCard();

    allSplitBeans.addAll(getLeaveData(flowMaps,flowDeductCard));
    allSplitBeans.addAll(getEvectionData(flowMaps,flowDeductCard));
    allSplitBeans.addAll(getOutData(flowMaps,flowDeductCard));
    if(isAll){
      allSplitBeans.addAll(getOverTimeData(flowMaps));
    }
    allSplitBeans.addAll(getOtherData(flowMaps));

    return allSplitBeans;
  }

  /**
   * 流程抵扣考勤
   */
  public Map<String,String> getFlowDeductCard() {

    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    Map<String,String> flowDeductCard = new HashMap<>();

    String flowDeductCardSql = "select * from kq_flow_deduct_card t where 1=1 and (isclear is null or isclear<>1) ";
    String sqlWhere = sqlFlowCardParamWhere();
    if(sqlWhere.length() > 0){
      flowDeductCardSql += sqlWhere;
    }
    rs.execute(flowDeductCardSql);
    while(rs.next()){
      String requestId= rs.getString("requestId");
      String resourceid= rs.getString("resourceid");
      String signtype= rs.getString("signtype");
      String serialnumber= rs.getString("serialnumber");
      String flowtype= rs.getString("flowtype");
      String key = requestId+"_"+resourceid+"_"+flowtype;
      String serial_signtype = serialnumber+"_"+signtype;
      if(flowDeductCard.containsKey(key)){
        String tmpSignType = Util.null2String(flowDeductCard.get(key));
        flowDeductCard.put(key, tmpSignType+","+serial_signtype);
      }else{
        flowDeductCard.put(key, serial_signtype);
      }
    }
    return flowDeductCard;
  }

  /**
   * 获取请假相关的数据
   */
  public List<SplitBean> getLeaveData(Map<String, Object> flowMaps,
      Map<String, String> flowDeductCard){

    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();

    //qc2635317 这里把限制条件改下
    String tablename = "select a.* from "+KqSplitFlowTypeEnum.LEAVE.getTablename()+"  a left join workflow_requestbase b on a.requestid = b.requestid where a.requestidcustom > 0 ";
    String leaveSql = "select * from ("+tablename+") t where 1=1 and (status is null or status <> '1') ";
    String sqlWhere = sqlParamWhere();

    if(sqlWhere.length() > 0){
      leaveSql += sqlWhere;
    }
    List<SplitBean> splitBeans = new ArrayList<>();
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

    int[] initArrays = kqTimesArrayComInfo.getInitArr();

    new BaseBean().writeLog("==zj==(考勤计算获取请假流程sql)" + leaveSql);

    rs.execute(leaveSql);
    while(rs.next()){
      SplitBean splitBean = new SplitBean();
      String requestId= rs.getString("requestidcustom");
      String resourceid= rs.getString("resourceid");
      String fromdate= rs.getString("fromdate");
      String belongdate= rs.getString("belongdate");
      String fromtime= rs.getString("fromtime");
      String todate= rs.getString("todate");
      String totime= rs.getString("totime");
      String newleavetype= rs.getString("newleavetype");
      String duration= rs.getString("duration");
      String durationrule= rs.getString("durationrule");
      String leavebackrequestid= Util.null2String(rs.getString("leavebackrequestid"));

      if(Util.getDoubleValue(duration) <= 0){
        continue;
      }
      //计算规则 1-按天请假 2-按半天请假 3-按小时请假 4-按整天请假
      String unitType = "4".equalsIgnoreCase(durationrule)?"1":"2";
      String card_key = requestId+"_"+resourceid+"_"+KqSplitFlowTypeEnum.LEAVE.getFlowtype();
      String serial_signtype = "";
      String serial = "";
      String signtype = "";
      if(!flowDeductCard.isEmpty() && flowDeductCard.containsKey(card_key)){
        serial_signtype = Util.null2String(flowDeductCard.get(card_key));
        if(serial_signtype.split("_") != null && serial_signtype.split("_").length == 2){
          serial = serial_signtype.split("_")[0];
          signtype = serial_signtype.split("_")[1];
        }
      }

      Map<String,String> infoMap = new HashMap<>();
      infoMap.put("requestId", requestId);
      infoMap.put(newleavetype, duration);
      infoMap.put("begintime", fromtime);
      infoMap.put("endtime", totime);
      infoMap.put("unitType", unitType);
      infoMap.put("flowtype", FlowReportTypeEnum.LEAVE.getFlowType());
      infoMap.put("newleavetype", newleavetype);
      infoMap.put("signtype", signtype);
      infoMap.put("serial", serial);

      String key = resourceid+"|"+belongdate;
      if(flowMaps != null){
        if(flowMaps.get(key) != null){
          if(leavebackrequestid.length() > 0 && leavebackrequestid.startsWith(",")){
            initArrays = kqTimesArrayComInfo.getInitArr();
            int fromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromtime);
            int toTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(totime);
            Arrays.fill(initArrays, fromTimeIndex, toTimeIndex+1, 1);
            leavebackrequestid = leavebackrequestid.substring(1);
            //qc2635317 这里把销假流程条件改下
            String backSql = "select * from "+KqSplitFlowTypeEnum.LEAVEBACK.getTablename()+" where "+Util.getSubINClause(leavebackrequestid, "requestid", "in")+" and fromdate= '"+fromdate+"'";
            rs1.executeQuery(backSql);

            while (rs1.next()){
              String back_fromtime = rs1.getString("fromtime");
              String back_totime = rs1.getString("totime");
              if(back_fromtime.equalsIgnoreCase(fromtime)){
                Arrays.fill(initArrays, kqTimesArrayComInfo.getArrayindexByTimes(back_fromtime), kqTimesArrayComInfo.getArrayindexByTimes(back_totime), -1);
              }else{
                Arrays.fill(initArrays, kqTimesArrayComInfo.getArrayindexByTimes(back_fromtime)+1, kqTimesArrayComInfo.getArrayindexByTimes(back_totime), -1);
              }
            }
            List<List<String>> backLists = new ArrayList<>();
            List<String> backList = new ArrayList<>();
            for(int i = fromTimeIndex ; i <= toTimeIndex ; i ++){
              if(initArrays[i] == 1){
                backList.add(kqTimesArrayComInfo.getTimesByArrayindex(i));
              }else{
                if(!backList.isEmpty()){
                  backLists.add(backList);
                  backList = new ArrayList<>();
                }else{
                  continue;
                }
              }
            }
            if(!backList.isEmpty()){
              backLists.add(backList);
            }
            if(backLists != null && !backLists.isEmpty()){
              List<Map<String,String>> time_list_tmp = (List<Map<String,String>>)flowMaps.get(key);
              for(int j = 0 ; j < backLists.size() ;j++){
                List<String> backListTmp = backLists.get(j);
                String back_tmp_fromtime = backListTmp.get(0);
                String back_tmp_totime = backListTmp.get(backListTmp.size()-1);
                infoMap = new HashMap<>();
                infoMap.put("requestId", requestId);
                infoMap.put(newleavetype, duration);
                infoMap.put("begintime", back_tmp_fromtime);
                infoMap.put("endtime", back_tmp_totime);
                infoMap.put("unitType", unitType);
                infoMap.put("flowtype", FlowReportTypeEnum.LEAVE.getFlowType());
                infoMap.put("newleavetype", newleavetype);
                time_list_tmp.add(infoMap);
              }
            }
          }else{
            List<Map<String,String>> time_list_tmp = (List<Map<String,String>>)flowMaps.get(key);
            time_list_tmp.add(infoMap);
          }
        }else{
          if(leavebackrequestid.length() > 0 && leavebackrequestid.startsWith(",")){
            initArrays = kqTimesArrayComInfo.getInitArr();
            int fromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromtime);
            int toTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(totime);
            Arrays.fill(initArrays, fromTimeIndex, toTimeIndex+1, 1);
            leavebackrequestid = leavebackrequestid.substring(1);
            String backSql = "select * from "+KqSplitFlowTypeEnum.LEAVEBACK.getTablename()+" where "+Util.getSubINClause(leavebackrequestid, "requestidcustom", "in")+" and fromdate= '"+fromdate+"'";
            new BaseBean().writeLog("==zj==(考勤销假backsql)" + backSql);
            rs1.executeQuery(backSql);

            while (rs1.next()){
              String back_fromtime = rs1.getString("fromtime");
              String back_totime = rs1.getString("totime");
              if(back_fromtime.equalsIgnoreCase(fromtime)){
                Arrays.fill(initArrays, kqTimesArrayComInfo.getArrayindexByTimes(back_fromtime), kqTimesArrayComInfo.getArrayindexByTimes(back_totime), -1);
              }else{
                if(back_fromtime.compareTo(back_totime) < 0){
                  Arrays.fill(initArrays, kqTimesArrayComInfo.getArrayindexByTimes(back_fromtime)+1, kqTimesArrayComInfo.getArrayindexByTimes(back_totime), -1);
                }
              }
            }
            List<List<String>> backLists = new ArrayList<>();
            List<String> backList = new ArrayList<>();
            for(int i = fromTimeIndex ; i <= toTimeIndex ; i ++){
              if(initArrays[i] == 1){
                backList.add(kqTimesArrayComInfo.getTimesByArrayindex(i));
              }else{
                if(!backList.isEmpty()){
                  backLists.add(backList);
                  backList = new ArrayList<>();
                }else{
                  continue;
                }
              }
            }
            if(!backList.isEmpty()){
              backLists.add(backList);
            }
            if(backLists != null && !backLists.isEmpty()){
              List<Map<String,String>> time_list = new ArrayList<>();
              for(int j = 0 ; j < backLists.size() ;j++){
                List<String> backListTmp = backLists.get(j);
                String back_tmp_fromtime = backListTmp.get(0);
                String back_tmp_totime = backListTmp.get(backListTmp.size()-1);
                infoMap = new HashMap<>();
                infoMap.put("requestId", requestId);
                infoMap.put(newleavetype, duration);
                infoMap.put("begintime", back_tmp_fromtime);
                infoMap.put("endtime", back_tmp_totime);
                infoMap.put("unitType", unitType);
                infoMap.put("flowtype", FlowReportTypeEnum.LEAVE.getFlowType());
                infoMap.put("newleavetype", newleavetype);
                time_list.add(infoMap);
              }
              flowMaps.put(key, time_list);
            }
          }else{
            List<Map<String,String>> time_list = new ArrayList<>();
            time_list.add(infoMap);
            flowMaps.put(key, time_list);
          }
        }
      }

    }
    return splitBeans;
  }

  /**
   * 获取出差相关的数据
   */
  public List<SplitBean> getEvectionData(Map<String, Object> flowMaps,
      Map<String, String> flowDeductCard){

    RecordSet rs = new RecordSet();
    String tablename = "select a.* from "+KqSplitFlowTypeEnum.EVECTION.getTablename()+"  a left join workflow_requestbase b on a.requestid = b.requestid where  b.requestid > 0 ";
    String leaveSql = "select * from ("+tablename+") t where 1=1 and (status is null or status <> '1') ";
    String sqlWhere = sqlParamWhere();

    if(sqlWhere.length() > 0){
      leaveSql += sqlWhere;
    }

    List<SplitBean> splitBeans = new ArrayList<>();
    rs.execute(leaveSql);
    while(rs.next()){
      SplitBean splitBean = new SplitBean();
      String requestId= rs.getString("requestId");
      String resourceid= rs.getString("resourceid");
      String fromdate= rs.getString("fromdate");
      String belongdate= rs.getString("belongdate");
      String fromtime= rs.getString("fromtime");
      String todate= rs.getString("todate");
      String totime= rs.getString("totime");
      String newleavetype= rs.getString("newleavetype");
      String duration= rs.getString("duration");
      String durationrule= rs.getString("durationrule");

      splitBean.setRequestId(requestId);
      splitBean.setResourceId(resourceid);
      splitBean.setFromDate(fromdate);
      splitBean.setFromTime(fromtime);
      splitBean.setToDate(todate);
      splitBean.setToTime(totime);
      splitBean.setNewLeaveType(newleavetype);
      splitBean.setDuration(duration);
      splitBean.setDurationrule(durationrule);
      splitBeans.add(splitBean);

      if(Util.getDoubleValue(duration) <= 0){
        continue;
      }
      //计算规则 1-按天请假 2-按半天请假 3-按小时请假 4-按整天请假
      String unitType = "4".equalsIgnoreCase(durationrule)?"1":"2";
      String card_key = requestId+"_"+resourceid+"_"+KqSplitFlowTypeEnum.EVECTION.getFlowtype();
      String serial_signtype = "";
      String serial = "";
      String signtype = "";
      if(!flowDeductCard.isEmpty() && flowDeductCard.containsKey(card_key)){
        serial_signtype = Util.null2String(flowDeductCard.get(card_key));
        if(serial_signtype.split("_") != null && serial_signtype.split("_").length == 2){
          serial = serial_signtype.split("_")[0];
          signtype = serial_signtype.split("_")[1];
        }
      }

      Map<String,String> infoMap = new HashMap<>();
      infoMap.put(FlowReportTypeEnum.businessLeave.getFlowType(), duration);
      infoMap.put("requestId", requestId);
      infoMap.put("begintime", fromtime);
      infoMap.put("endtime", totime);
      infoMap.put("unitType", unitType);
      infoMap.put("duration", duration);
      infoMap.put("flowtype", FlowReportTypeEnum.EVECTION.getFlowType());
      infoMap.put("signtype", signtype);
      infoMap.put("serial", serial);

      String key = resourceid+"|"+belongdate;
      if(flowMaps != null){
        if(flowMaps.get(key) != null){
          List<Map<String,String>> time_list_tmp = (List<Map<String,String>>)flowMaps.get(key);
          time_list_tmp.add(infoMap);
        }else{
          List<Map<String,String>> time_list = new ArrayList<>();
          time_list.add(infoMap);
          flowMaps.put(key, time_list);
        }
      }

    }
    return splitBeans;
  }

  /**
   * 获取公出相关的数据
   */
  public List<SplitBean> getOutData(Map<String, Object> flowMaps,
      Map<String, String> flowDeductCard){

    RecordSet rs = new RecordSet();
    String tablename = "select a.* from "+KqSplitFlowTypeEnum.OUT.getTablename()+"  a left join workflow_requestbase b on a.requestid = b.requestid where  b.requestid > 0 ";
    String leaveSql = "select * from ("+tablename+") t where 1=1 and (status is null or status <> '1') ";
    String sqlWhere = sqlParamWhere();

    if(sqlWhere.length() > 0){
      leaveSql += sqlWhere;
    }
    List<SplitBean> splitBeans = new ArrayList<>();
    rs.execute(leaveSql);
    while(rs.next()){
      SplitBean splitBean = new SplitBean();
      String requestId= rs.getString("requestId");
      String resourceid= rs.getString("resourceid");
      String fromdate= rs.getString("fromdate");
      String belongdate= rs.getString("belongdate");
      String fromtime= rs.getString("fromtime");
      String todate= rs.getString("todate");
      String totime= rs.getString("totime");
      String newleavetype= rs.getString("newleavetype");
      String duration= rs.getString("duration");
      String durationrule= rs.getString("durationrule");

      splitBean.setRequestId(requestId);
      splitBean.setResourceId(resourceid);
      splitBean.setFromDate(fromdate);
      splitBean.setFromTime(fromtime);
      splitBean.setToDate(todate);
      splitBean.setToTime(totime);
      splitBean.setNewLeaveType(newleavetype);
      splitBean.setDuration(duration);
      splitBean.setDurationrule(durationrule);
      splitBeans.add(splitBean);

      if(Util.getDoubleValue(duration) <= 0){
        continue;
      }
      //计算规则 1-按天请假 2-按半天请假 3-按小时请假 4-按整天请假
      String unitType = "4".equalsIgnoreCase(durationrule)?"1":"2";
      String card_key = requestId+"_"+resourceid+"_"+KqSplitFlowTypeEnum.OUT.getFlowtype();
      String serial_signtype = "";
      String serial = "";
      String signtype = "";
      if(!flowDeductCard.isEmpty() && flowDeductCard.containsKey(card_key)){
        serial_signtype = Util.null2String(flowDeductCard.get(card_key));
        if(serial_signtype.split("_") != null && serial_signtype.split("_").length == 2){
          serial = serial_signtype.split("_")[0];
          signtype = serial_signtype.split("_")[1];
        }
      }

      Map<String,String> infoMap = new HashMap<>();
      infoMap.put(FlowReportTypeEnum.officialBusiness.getFlowType(), duration);
      infoMap.put("requestId", requestId);
      infoMap.put("begintime", fromtime);
      infoMap.put("endtime", totime);
      infoMap.put("unitType", unitType);
      infoMap.put("duration", duration);
      infoMap.put("flowtype", FlowReportTypeEnum.OUT.getFlowType());
      infoMap.put("signtype", signtype);
      infoMap.put("serial", serial);

      String key = resourceid+"|"+belongdate;
      if(flowMaps != null){
        if(flowMaps.get(key) != null){
          List<Map<String,String>> time_list_tmp = (List<Map<String,String>>)flowMaps.get(key);
          time_list_tmp.add(infoMap);
        }else{
          List<Map<String,String>> time_list = new ArrayList<>();
          time_list.add(infoMap);
          flowMaps.put(key, time_list);
        }
      }

    }
    return splitBeans;
  }

  /**
   * 获取加班相关的数据
   */
  public List<SplitBean> getOverTimeData(Map<String,Object> flowMaps){

    RecordSet rs = new RecordSet();
    String tablename = "select a.* from "+KqSplitFlowTypeEnum.OVERTIME.getTablename()+"  a left join workflow_requestbase b on a.requestid = b.requestid where  b.requestid > 0 ";
    String leaveSql = "select * from ("+tablename+") t where 1=1 ";
    String sqlWhere = sqlParamWhere();

    if(sqlWhere.length() > 0){
      leaveSql += sqlWhere;
    }
    List<SplitBean> splitBeans = new ArrayList<>();
    if(orderby_sql.length() > 0){
      leaveSql = leaveSql+orderby_sql;
    }
    rs.execute(leaveSql);
    while(rs.next()){
      SplitBean splitBean = new SplitBean();
      String dataid= rs.getString("dataid");
      String detailid= rs.getString("detailid");
      String requestId= rs.getString("requestId");
      String resourceid= rs.getString("resourceid");
      String fromdate= rs.getString("fromdate");
      String belongdate= rs.getString("belongdate");
      String fromtime= rs.getString("fromtime");
      String todate= rs.getString("todate");
      String totime= rs.getString("totime");
      String newleavetype= rs.getString("newleavetype");
      String duration= rs.getString("duration");
      String durationrule= rs.getString("durationrule");
      String changetype= rs.getString("changetype");
      String d_mins= rs.getString("d_mins");
      String overtime_type= rs.getString("overtime_type");

      String fromdatedb= rs.getString("fromdatedb");
      String fromtimedb= rs.getString("fromtimedb");
      String todatedb= rs.getString("todatedb");
      String totimedb= rs.getString("totimedb");

      splitBean.setDataId(dataid);
      splitBean.setDetailId(detailid);
      splitBean.setRequestId(requestId);
      splitBean.setResourceId(resourceid);
      splitBean.setFromDate(fromdate);
      splitBean.setFromTime(fromtime);
      splitBean.setToDate(todate);
      splitBean.setToTime(totime);
      splitBean.setNewLeaveType(newleavetype);
      splitBean.setDuration(duration);
      splitBean.setDurationrule(durationrule);
      splitBean.setChangeType(Util.getIntValue(changetype));
      splitBean.setD_Mins(Util.getDoubleValue(d_mins));
      splitBean.setOvertime_type(overtime_type);
      splitBean.setFromdatedb(fromdatedb);
      splitBean.setFromtimedb(fromtimedb);
      splitBean.setTodatedb(todatedb);
      splitBean.setTotimedb(totimedb);
      splitBeans.add(splitBean);

      //计算规则 1-按天请假 2-按半天请假 3-按小时请假 4-按整天请假
      String unitType = "4".equalsIgnoreCase(durationrule)?"1":"2";
      unitType = "2".equalsIgnoreCase(durationrule)?"1":"2";

      Map<String,String> infoMap = new HashMap<>();
      infoMap.put(FlowReportTypeEnum.businessLeave.getFlowType(), duration);
      infoMap.put("begintime", fromtime);
      infoMap.put("endtime", totime);
      infoMap.put("unitType", unitType);
      infoMap.put("duration", duration);
      infoMap.put("flowtype", FlowReportTypeEnum.OVERTIME.getFlowType());

      String key = resourceid+"|"+belongdate;
      if(flowMaps != null){
        if(flowMaps.get(key) != null){
          List<Map<String,String>> time_list_tmp = (List<Map<String,String>>)flowMaps.get(key);
          time_list_tmp.add(infoMap);
        }else{
          List<Map<String,String>> time_list = new ArrayList<>();
          time_list.add(infoMap);
          flowMaps.put(key, time_list);
        }
      }
    }
    return splitBeans;
  }

  /**
   * 获取异常流程的数据
   */
  public List<SplitBean> getOtherData(Map<String,Object> flowMaps){

    RecordSet rs = new RecordSet();
    String tablename = "select a.* from "+KqSplitFlowTypeEnum.OTHER.getTablename()+"  a left join workflow_requestbase b on a.requestid = b.requestid where  b.requestid > 0 ";
    String leaveSql = "select * from ("+tablename+") t where 1=1 ";
    String sqlWhere = sqlParamWhere();

    if(sqlWhere.length() > 0){
      leaveSql += sqlWhere;
    }
    List<SplitBean> splitBeans = new ArrayList<>();
    rs.execute(leaveSql);
    while(rs.next()){
      SplitBean splitBean = new SplitBean();
      String requestId= rs.getString("requestId");
      String resourceid= rs.getString("resourceid");
      String fromdate= rs.getString("fromdate");
      String belongdate= rs.getString("belongdate");
      String fromtime= rs.getString("fromtime");
      String todate= rs.getString("todate");
      String totime= rs.getString("totime");
      String newleavetype= rs.getString("newleavetype");
      String duration= rs.getString("duration");
      String durationrule= rs.getString("durationrule");

      splitBean.setRequestId(requestId);
      splitBean.setResourceId(resourceid);
      splitBean.setFromDate(fromdate);
      splitBean.setFromTime(fromtime);
      splitBean.setToDate(todate);
      splitBean.setToTime(totime);
      splitBean.setNewLeaveType(newleavetype);
      splitBean.setDuration(duration);
      splitBean.setDurationrule(durationrule);
      splitBeans.add(splitBean);

      //计算规则 1-按天请假 2-按半天请假 3-按小时请假 4-按整天请假
      String unitType = "4".equalsIgnoreCase(durationrule)?"1":"2";

      Map<String,String> infoMap = new HashMap<>();
      infoMap.put(FlowReportTypeEnum.businessLeave.getFlowType(), duration);
      infoMap.put("begintime", fromtime);
      infoMap.put("endtime", totime);
      infoMap.put("unitType", unitType);

      String key = resourceid+"|"+belongdate;
      if(flowMaps != null){
        if(flowMaps.get(key) != null){
          List<Map<String,String>> time_list_tmp = (List<Map<String,String>>)flowMaps.get(key);
          time_list_tmp.add(infoMap);
        }else{
          List<Map<String,String>> time_list = new ArrayList<>();
          time_list.add(infoMap);
          flowMaps.put(key, time_list);
        }
      }
    }
    return splitBeans;
  }

  /**
   * 根据请假类型判断是否被流程引用
   * @param ruleid
   * @return true表示被引用
   */
  public static boolean leaveTypeUsed(String ruleid){
    KQFlowDataBiz kqFlowDataBiz = new FlowDataParamBuilder().newLeaveTypeParam(ruleid).build();
    List<SplitBean> splitBeans = kqFlowDataBiz.getLeaveData(null, new HashMap<>());
    if(!splitBeans.isEmpty()){
      return true;
    }else{
      return false;
    }
  }

  /**
   * 生成相应的查询条件
   * @return
   */
  private String sqlParamWhere() {
    String sqlWhere = "";
    if(resourceid.length() > 0){
      sqlWhere += " and resourceid in ( "+resourceid+" )";
    }
    if(fromDate.length() > 0 && toDate.length() > 0){
      sqlWhere += " and ( fromdate between '"+fromDate+"' and '"+toDate+"' or todate between '"+fromDate+"' and '"+toDate+"' )";
    }else{
      if(fromDate.length() > 0){
        sqlWhere += " and fromdate between '"+fromDate+"' and '"+fromDate+"' ";
      }
      if(toDate.length() > 0){
        sqlWhere += " and todate between '"+toDate+"' and '"+toDate+"' ";
      }
    }
    if(belongDate.length() > 0){
      sqlWhere += " and belongdate = '"+belongDate+"' ";
    }
    if(fromTime.length() > 0){
      sqlWhere += " and fromtime >= '"+fromTime+"' ";
    }
    if(toTime.length() > 0){
      sqlWhere += " and totime <= '"+toTime+"' ";
    }
    if(newLeaveType.length() > 0){
      sqlWhere += " and newleavetype in ( "+newLeaveType+" )";
    }
    return sqlWhere;
  }

  private String sqlFlowCardParamWhere() {
    String sqlWhere = "";
    if(resourceid.length() > 0){
      sqlWhere += " and resourceid in ( "+resourceid+" )";
    }
    if(fromDate.length() > 0 && toDate.length() > 0){
      sqlWhere += " and ( fromdate between '"+fromDate+"' and '"+toDate+"' or todate between '"+fromDate+"' and '"+toDate+"' )";
    }else{
      if(fromDate.length() > 0){
        sqlWhere += " and fromdate between '"+fromDate+"' and '"+fromDate+"' ";
      }
      if(toDate.length() > 0){
        sqlWhere += " and todate between '"+toDate+"' and '"+toDate+"' ";
      }
    }
    if(belongDate.length() > 0){
      sqlWhere += " and belongdate = '"+belongDate+"' ";
    }
    if(fromTime.length() > 0){
      sqlWhere += " and fromtime >= '"+fromTime+"' ";
    }
    if(toTime.length() > 0){
      sqlWhere += " and totime <= '"+toTime+"' ";
    }
    return sqlWhere;
  }

  /**
   * 针对可能存在的多种参数类型 创建参数静态内部类Builder
   */
  public static class FlowDataParamBuilder {

    private String resourceid = "";
    private String fromDate = "";
    private String toDate = "";
    private String fromTime = "";
    private String toTime = "";
    private String belongDate = "";
    /**
     * 请假用的请假类型
     */
    private String newLeaveType = "";
    private String orderby_sql = "";

    public FlowDataParamBuilder() {
      this.resourceid = "";
      //初始化的时候需要把其他参数先清空下
      this.fromDate = "";
      this.toDate = "";
      this.fromTime = "";
      this.toTime = "";
      this.newLeaveType = "";
      this.belongDate = "";
      this.orderby_sql = "";
    }

    //成员方法返回其自身，所以可以链式调用
    public FlowDataParamBuilder resourceidParam(final String resourceid) {
      this.resourceid = resourceid;
      return this;
    }

    public FlowDataParamBuilder fromDateParam(final String fromDate) {
      this.fromDate = fromDate;
      return this;
    }

    public FlowDataParamBuilder toDateParam(final String toDate) {
      this.toDate = toDate;
      return this;
    }

    public FlowDataParamBuilder fromTimeParam(final String fromTime) {
      this.fromTime = fromTime;
      return this;
    }

    public FlowDataParamBuilder toTimeParam(final String toTime) {
      this.toTime = toTime;
      return this;
    }

    public FlowDataParamBuilder newLeaveTypeParam(final String newLeaveType) {
      this.newLeaveType = newLeaveType;
      return this;
    }

    public FlowDataParamBuilder belongDateParam(final String belongDate) {
      this.belongDate = belongDate;
      return this;
    }

    public FlowDataParamBuilder orderby_sqlParam(final String orderby_sql) {
      this.orderby_sql = orderby_sql;
      return this;
    }

    //Builder的build方法，返回外部类的实例
    public KQFlowDataBiz build() {
      return new KQFlowDataBiz(this);
    }
  }
}
