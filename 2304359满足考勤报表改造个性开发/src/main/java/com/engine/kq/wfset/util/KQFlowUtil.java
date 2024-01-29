package com.engine.kq.wfset.util;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2304359.KqReportUtil;
import com.engine.kq.biz.KQExitRulesBiz;
import com.engine.kq.biz.KQFormatData;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.biz.KQTravelRulesBiz;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;
import weaver.workflow.workflow.WorkflowRequestComInfo;

public class KQFlowUtil {
  private KQLog kqLog = new KQLog();

  /**
   * 把通用的bean部分在这里进行填充
   * @param splitBean
   * @param rs1
   * @param requestId
   * @param rci
   * @param workflowId
   * @param durationTypeEnum
   * @param key
   * @param result
   * @param datetimeFormatter
   * @param uuid
   */
  public boolean fillSplitBean(SplitBean splitBean, RecordSet rs1, String requestId,
      ResourceComInfo rci, String workflowId, DurationTypeEnum durationTypeEnum, String key,
      Map<String, String> result, DateTimeFormatter datetimeFormatter, String uuid){

    boolean isFillRight = true;
    String concort = "###" ;
    int usedetail =  Util.getIntValue(key.split(concort)[2], 0);
    String tableDetailName=  key.split(concort)[1] ;
    String tableName=  key.split(concort)[0] ;
    String prefix = "";
    String id = "dataId";
    if(usedetail == 1){
      prefix = "detail_";
      id = "detailId";
    }

    boolean isLeaveBack = false;
    if(durationTypeEnum == DurationTypeEnum.LEAVEBACK){
      isLeaveBack = true;
    }
    boolean isProcessChange = false;
    boolean isProcessDrawBack = false;
    if(durationTypeEnum == DurationTypeEnum.PROCESSCHANGE){
      isProcessChange = true;
      String changetype = Util.null2s(rs1.getString("changetype"), "");
      if("1".equalsIgnoreCase(changetype)){
        //如果是撤销的话，没有开始日期时间和结束日期时间
        isProcessDrawBack = true;
      }
    }

    String resourceId = "";
    //查询到的requestid
    String requestId_rs = "";

    String idVal = Util.null2s(rs1.getString(id), "0");
    String fromDate = Util.null2s(rs1.getString(prefix+"fromDate"), "");
    String toDate = Util.null2s(rs1.getString(prefix+"toDate"), "");
    String fromTime = Util.null2s(rs1.getString(prefix+"fromTime"), "");
    String toTime = Util.null2s(rs1.getString(prefix+"toTime"), "");
    String durationDB = Util.null2s(rs1.getString(prefix+"duration"), "");
    if(isLeaveBack || isProcessChange){
      resourceId = Util.null2s(rs1.getString("resourceId"), "");
    }else{
      resourceId = Util.null2s(rs1.getString(prefix+"resourceId"), "");
    }
    if(Util.getIntValue(requestId,0) <= 0){
      requestId_rs = Util.null2s(rs1.getString("requestId"), "0");
    }

    boolean isVal = checkActionValidate(result, fromDate, toDate, fromTime, toTime, datetimeFormatter);
    if(isProcessDrawBack){
      result.clear();
      result.put("isProcessDrawBack", "1");
      isVal = true;
    }
    if(!isVal){
      isFillRight = false;
      return isFillRight;
    }
    if(isLeaveBack){
      LocalDateTime localFromDateTime = LocalDateTime.parse(fromDate+" "+fromTime,datetimeFormatter);
      LocalDateTime localToDateTime = LocalDateTime.parse(toDate+" "+toTime,datetimeFormatter);

      isFillRight = KQFlowLeaveBackUtil.leaveBackCheck(rs1 ,datetimeFormatter,prefix,localFromDateTime,localToDateTime,result);
      if(!isFillRight){
        return isFillRight;
      }
    }
    if(isProcessChange){
      isFillRight = KQFlowProcessChangeUtil.processChangeCheck(rs1 ,requestId_rs,result);
      if(!isFillRight){
        return isFillRight;
      }
    }

    if(usedetail == 1){
      splitBean.setDataId("0");
      splitBean.setDetailId(idVal);
      splitBean.setTablenamedb(tableDetailName);
    }else{
      splitBean.setDataId(idVal);
      splitBean.setDetailId("0");
      splitBean.setTablenamedb(tableName);
    }
    splitBean.setFromdatedb(fromDate);
    splitBean.setFromtimedb(fromTime);
    splitBean.setTodatedb(toDate);
    splitBean.setTotimedb(toTime);

    if(requestId_rs.length() > 0){
      WorkflowRequestComInfo workflowRequestComInfo = new WorkflowRequestComInfo();
      splitBean.setRequestId(requestId_rs);
      splitBean.setWorkflowId(workflowRequestComInfo.getWorkflowId(requestId_rs));
    }else{
      splitBean.setRequestId(requestId);
      splitBean.setWorkflowId(workflowId);
    }
    splitBean.setUsedetail(""+usedetail);
    splitBean.setResourceId(resourceId);
    splitBean.setSubcompanyid(Util.null2s(rci.getSubCompanyID(resourceId),"0"));
    splitBean.setDepartmentid(Util.null2s(rci.getDepartmentID(resourceId),"0"));
    splitBean.setJobtitle(Util.null2s(rci.getJobTitle(resourceId),"0"));
    splitBean.setFromDate(fromDate);
    splitBean.setFromTime(fromTime);
    splitBean.setToDate(toDate);
    splitBean.setToTime(toTime);
    splitBean.setDurationDB(durationDB);
    //默认记录的状态都为0
    splitBean.setStatus("0");
    splitBean.setDurationTypeEnum(durationTypeEnum);

    switch (durationTypeEnum){
      case LEAVE:
        KQFlowLeaveUtil.bean4Leave(prefix,rs1,splitBean);
        break;
      case EVECTION:
        KQFlowEvectionUtil.bean4Evection(prefix,rs1,splitBean);
        break;
      case OUT:
        KQFlowOutUtil.bean4Out(splitBean);
        break;
      case OVERTIME:
        KQFlowOvertimeUtil.bean4Overtime(prefix,rs1,splitBean);
        break;
      case LEAVEBACK:
        KQFlowLeaveBackUtil.bean4LeaveBack(prefix,rs1,splitBean);
        break;
      case OTHER:
        bean4Other(prefix,rs1,splitBean);
        break;
      case PROCESSCHANGE:
        KQFlowProcessChangeUtil.bean4ProcessChange(prefix,rs1,splitBean);
        break;
      default:
        break;
    }
    String computingMode = splitBean.getComputingMode();
    String newLeaveType = splitBean.getNewLeaveType();
    if("2".equalsIgnoreCase(computingMode)){
      if(durationTypeEnum == DurationTypeEnum.PROCESSCHANGE){
        double oneDayHour = getOneDayHour(splitBean.getDurationTypeEnum(),newLeaveType);
        splitBean.setOneDayHour(oneDayHour);
      }else{
        double oneDayHour = getOneDayHour(durationTypeEnum,newLeaveType);
        splitBean.setOneDayHour(oneDayHour);
      }
    }
    return isFillRight;

  }

  private void bean4Other(String prefix, RecordSet rs1, SplitBean splitBean) {
    String minimumUnit = "1";
    String computingMode = "1";
    splitBean.setDurationrule(minimumUnit);
    splitBean.setComputingMode(computingMode);
  }


  /**
   * 获取按照自然日计算的时候，按天/半天计算的时候一天对应的小时数
   * @param durationTypeEnum
   * @param newLeaveType
   * @return
   */
  public static double getOneDayHour(DurationTypeEnum durationTypeEnum,String newLeaveType){
    double oneDayHour = 0.0;
    //TODO KQLeaveRulesBiz.getHoursToDay如果单位是小时的时候取不到日折算时长
    switch (durationTypeEnum){
      case LEAVE:
        oneDayHour = Util.getDoubleValue(KQLeaveRulesBiz.getHoursToDay(newLeaveType), 0.0);
        break;
      case EVECTION:
        oneDayHour = Util.getDoubleValue(KQTravelRulesBiz.getHoursToDay(), 0.0);
        break;
      case OUT:
        oneDayHour = Util.getDoubleValue(KQExitRulesBiz.getHoursToDay(), 0.0);
        break;
      case OVERTIME:
        oneDayHour = KQOvertimeRulesBiz.getHoursToDay();
        break;
      case LEAVEBACK:
        oneDayHour = Util.getDoubleValue(KQLeaveRulesBiz.getHoursToDay(newLeaveType), 0.0);
        break;
      default:
    }
    return oneDayHour;
  }

  public boolean checkActionValidate(Map<String,String> result,String fromDate,String toDate,String fromTime,String toTime, DateTimeFormatter datetimeFormatter){

    boolean isVal = true;

    if(Util.null2String(fromDate,"").length() == 0  ||
        Util.null2String(toDate,"").length() == 0 ||
        Util.null2String(fromTime,"").length() == 0 ||
        Util.null2String(toTime,"").length() == 0){
      result.put("status", "-1");
      result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005411,weaver.general.ThreadVarLanguage.getLang())+"");
      isVal = false;
      return isVal;
    }
    if((fromDate+" "+fromTime).length() != 16 || (toDate+" "+toTime).length() != 16){
      result.put("status", "-1");
      result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005412,weaver.general.ThreadVarLanguage.getLang())+""+(fromDate+" "+fromTime)+":"+(toDate+" "+toTime));
      isVal = false;
      return isVal;
    }
    LocalDate localFromDate = LocalDate.parse(fromDate);
    LocalDate localToDate = LocalDate.parse(toDate);
    LocalDateTime localFromDateTime = LocalDateTime.parse(fromDate+" "+fromTime,datetimeFormatter);
    LocalDateTime localToDateTime = LocalDateTime.parse(toDate+" "+toTime,datetimeFormatter);

    if(localFromDateTime.isAfter(localToDateTime)){
      result.put("status", "-1");
      result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(511480,weaver.general.ThreadVarLanguage.getLang())+"");
      isVal = false;
      return isVal;
    }

    if (localFromDate.isAfter(localToDate)) {
      result.put("status", "-1");
      result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005413,weaver.general.ThreadVarLanguage.getLang())+"");
      isVal = false;
      return isVal;
    }
    return isVal;
  }


  /**
   * 插入数据到中间表
   * @param splitBeans
   * @param flowTypeEnum
   * @param rci
   * @param result
   * @param isForce
   * @param requestId
   * @param workflowId
   * @param isUpgrade 是否是升级，升级的话不需要处理假期余额数据
   * @throws Exception
   */
  public void handleSplitFLowActionData(
      List<SplitBean> splitBeans, KqSplitFlowTypeEnum flowTypeEnum,
      ResourceComInfo rci, Map<String, String> result, boolean isForce, int requestId,
      int workflowId,boolean isUpgrade) throws Exception{

    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    Map<String,String> custome_map = Maps.newHashMap();
    List<String> custome_field = Lists.newArrayList();
    if(flowTypeEnum == KqSplitFlowTypeEnum.OVERTIME){
      custome_field.add("overtime_type");
    }

    //==zj 处理出差拆分时长取整（向下取整，0.5h最小单位）
    if (flowTypeEnum == KqSplitFlowTypeEnum.EVECTION){
      KqReportUtil kqReportUtil = new KqReportUtil();
        for (SplitBean bean : splitBeans){
          String d_mins =String.valueOf(bean.getD_Mins()) ;
          String duration = kqReportUtil.halfHourCal(d_mins,0);
          new BaseBean().writeLog("==zj==(出差流程拆分计算)" + "人员id:" + bean.getResourceId() + "--时长:" + duration + "--实际开始日期:" + bean.getFromDate());
          //qc2304359 这里再把日期类型设置一下，方便归类
          int changeType = kqReportUtil.getChangeType(bean.getResourceId(), bean.getFromDate());
          new BaseBean().writeLog("==zj==(设置后日期类型)" + changeType);
          bean.setChangeType(changeType);
          bean.setDuration(duration);

        }
    }

    //==zj 处理公出拆分时长取整（向上取整，0.5h最小单位）
    if (flowTypeEnum == KqSplitFlowTypeEnum.OUT){
      KqReportUtil kqReportUtil = new KqReportUtil();
      for (SplitBean bean : splitBeans){
        String d_mins =String.valueOf(bean.getD_Mins()) ;
        String duration = kqReportUtil.halfHourCal(d_mins,1);
        new BaseBean().writeLog("==zj==(公出流程拆分duration计算)" + duration);
        bean.setDuration(duration);

      }
    }

    String batchSql = "insert into "+flowTypeEnum.getTablename()+" ("
        + "requestid,workflowid,dataid,detailid,resourceid,fromdate,fromtime,"
        + "todate,totime,newleavetype,duration,usedetail,durationrule,tablenamedb,fromdatedb,"
        + "fromtimedb,todatedb,totimedb,durationdb,status,belongDate,D_Mins,serialid,"
        + "changeType,subcompanyid,departmentid,jobtitle,companion,iscompanion"+getCustomField(custome_field,"field",null,
        custome_map)+")"+
        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?"+getCustomField(custome_field,"param",null,
        custome_map)+") ";
    List<List> params = new ArrayList<List>();

    List<String> formateList = new ArrayList<>();
    for(SplitBean bean : splitBeans){
      List<Object> beanParams =  new ArrayList<Object>();
      beanParams.add(bean.getRequestId());
      beanParams.add(bean.getWorkflowId());
      beanParams.add(bean.getDataId());
      beanParams.add(bean.getDetailId());
      beanParams.add(bean.getResourceId());
      beanParams.add(bean.getFromDate());
      beanParams.add(bean.getFromTime());
      beanParams.add(bean.getToDate());
      beanParams.add(bean.getToTime());
      beanParams.add(bean.getNewLeaveType());
      beanParams.add(Util.null2s(bean.getDuration(),"0"));
      beanParams.add(bean.getUsedetail());
      beanParams.add(bean.getDurationrule());
      beanParams.add(bean.getTablenamedb());
      beanParams.add(bean.getFromdatedb());
      beanParams.add(bean.getFromtimedb());
      beanParams.add(bean.getTodatedb());
      beanParams.add(bean.getTotimedb());
      beanParams.add(bean.getDurationDB());
      beanParams.add(bean.getStatus());
      beanParams.add(bean.getBelongDate());
      beanParams.add(bean.getD_Mins());
      beanParams.add(bean.getSerialid());
      beanParams.add(bean.getChangeType());
      beanParams.add(bean.getSubcompanyid());
      beanParams.add(bean.getDepartmentid());
      beanParams.add(bean.getJobtitle());
      beanParams.add(bean.getCompanion());
      beanParams.add(bean.getIscompanion());
      if(!custome_field.isEmpty()){
        if(flowTypeEnum == KqSplitFlowTypeEnum.OVERTIME){
          custome_map.put("overtime_type", bean.getOvertime_type());
        }
        getCustomField(custome_field, "value",beanParams,custome_map);
      }

      String format = bean.getResourceId()+"_"+bean.getBelongDate();
      formateList.add(format);

      params.add(beanParams);

      if(flowTypeEnum == KqSplitFlowTypeEnum.EVECTION && false){
        //qc898997 已经改成根据每个人的班次来计算了，这里就屏蔽了
        KQFlowEvectionUtil kqFlowEvectionUtil = new KQFlowEvectionUtil();
        String companion = Util.null2s(bean.getCompanion(), "");
        if(companion.length() > 0){
          kqFlowEvectionUtil.splitEvectionCompanion(companion,bean,params,rci,formateList);
        }
      }
    }
    if(!params.isEmpty()){
      //先根据requestid删除中间表里的数据，再做啥插入操作
      String delSql = "delete from "+flowTypeEnum.getTablename()+" where requestid = "+requestId;
      rs.executeUpdate(delSql);

      for(int i = 0 ; i < params.size() ; i++){
        List<Object> beanParams = params.get(i);
        boolean isOk = rs1.executeUpdate(batchSql, beanParams);
        if(!isOk){
          delSql = "delete from "+flowTypeEnum.getTablename()+" where requestid = "+requestId;
          rs.executeUpdate(delSql);
          result.put("status", "-1");
          result.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005408,weaver.general.ThreadVarLanguage.getLang())+":"+flowTypeEnum.getTablename()+""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005414,weaver.general.ThreadVarLanguage.getLang())+"");
          kqLog.info("handleSplitFLowActionData:"+flowTypeEnum.getTablename()+"拆分保存失败："+params);
          return ;
        }
      }
      kqLog.info("handleSplitFLowActionData:formateList:"+formateList+":flowTypeEnum:"+flowTypeEnum);
      for(String format: formateList){
        kqLog.info("handleSplitFLowActionData:format:"+ JSON.toJSONString(format)+":flowTypeEnum:"+flowTypeEnum);
        String[] formats = format.split("_");
        if(!isUpgrade){
          //考勤设置升级的话 流程数据就不需要格式化考勤了
          new KQFormatData().formatKqDate(formats[0],formats[1]);
        }
      }
      if(!isUpgrade){
        if(isForce){
          if(flowTypeEnum == KqSplitFlowTypeEnum.LEAVE){
            //先在这里执行扣减动作
            SplitActionUtil.handleLeaveAction(splitBeans,""+requestId);
            //然后再把扣减的了数据更新下KQ_ATT_VACATION表
            String updateFreezeSql = "update KQ_ATT_VACATION set status=0 where requestId=? and workflowId = ? ";
            boolean isUpdate = rs.executeUpdate(updateFreezeSql,requestId,""+workflowId);
            if(!isUpdate){
              result.put("status", "-1");
              result.put("message", (""+weaver.systeminfo.SystemEnv.getHtmlLabelName(82823,weaver.general.ThreadVarLanguage.getLang())+"action"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005361,weaver.general.ThreadVarLanguage.getLang())+""+updateFreezeSql+"(0,"+requestId+","+workflowId+")"));
              kqLog.info("扣减action保存失败："+updateFreezeSql+"(0,"+requestId+","+workflowId+")");
              return ;
            }
          }else if(flowTypeEnum == KqSplitFlowTypeEnum.OVERTIME){
            // 强制归档，加班数据第一第二种规则都需要处理
            SplitActionUtil.handleOverTimeAction(splitBeans, ""+requestId,true, "");
          }
        }else{
          if(flowTypeEnum == KqSplitFlowTypeEnum.OVERTIME){
            //正常归档的时候 单独针对加班规则的第一 第二种模式 生成加班数据
            SplitActionUtil.handleOverTimeActionMode2(splitBeans, ""+requestId);
          }
        }
      }else{
        if(flowTypeEnum == KqSplitFlowTypeEnum.OVERTIME){
          if(!splitBeans.isEmpty()){
            for (SplitBean splitBean : splitBeans) {
              String sql = "delete from kq_flow_overtime where requestid=? ";
              rs.executeUpdate(sql, splitBean.getRequestId());
            }
          }
          SplitActionUtil.handleOverTimeActionMode2(splitBeans, ""+requestId);
        }
      }
    }else{
      rs1.writeLog(flowTypeEnum.getTablename()+"生成的params是空：");
      return ;
    }
  }

  /**
   * 考勤流程中间表针对某个流程扩展字段
   * @param custome_field
   * @param key
   * @param beanParams
   * @param custome_map
   * @return
   */
  public String getCustomField(List<String> custome_field, String key, List<Object> beanParams,
      Map<String, String> custome_map) {
    String fieldValue = "";
    if(!custome_field.isEmpty()){
      for(int i = 0 ; i < custome_field.size() ; i++){
        String tmp_value = Util.null2String(custome_field.get(i));
        if(tmp_value.length() > 0){
          if("field".equalsIgnoreCase(key)){
            fieldValue += ","+tmp_value;
          }else if("param".equalsIgnoreCase(key)){
            fieldValue += ",?";
          }else if("value".equalsIgnoreCase(key)){
            String value = custome_map.get(tmp_value);
            beanParams.add(value);
          }
        }
      }
    }
    return fieldValue;
  }

}
