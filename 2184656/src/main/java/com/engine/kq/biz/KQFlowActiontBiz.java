package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.chain.duration.WorkDayUnitSplitChain;
import com.engine.kq.biz.chain.duration.WorkDurationChain;
import com.engine.kq.biz.chain.duration.WorkHalfUnitSplitChain;
import com.engine.kq.biz.chain.duration.WorkHourUnitSplitChain;
import com.engine.kq.biz.chain.duration.WorkWholeUnitSplitChain;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.attendance.manager.HrmAttProcSetManager;
import com.engine.kq.wfset.bean.SplitBean;
import com.engine.kq.wfset.util.KQFlowCardUtil;
import com.engine.kq.wfset.util.KQFlowEvectionUtil;
import com.engine.kq.wfset.util.KQFlowLeaveBackUtil;
import com.engine.kq.wfset.util.KQFlowLeaveUtil;
import com.engine.kq.wfset.util.KQFlowOtherUtil;
import com.engine.kq.wfset.util.KQFlowOutUtil;
import com.engine.kq.wfset.util.KQFlowOvertimeUtil;
import com.engine.kq.wfset.util.KQFlowProcessChangeUtil;
import com.engine.kq.wfset.util.KQFlowShiftUtil;
import com.engine.kq.wfset.util.KQFlowUtil;
import com.engine.kq.wfset.util.SplitActionUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;

/**
 * 考勤流程action相关数据处理类
 */
public class KQFlowActiontBiz {
  public KQLog kqLog = new KQLog();

  /**
   * 强制收回操作
   * @param requestId
   */
  public void handleDel(int requestId) {
    RecordSet rs = new RecordSet();
    try {
      String updateFreezeSql = "update KQ_ATT_VACATION set status=2 where requestId=?  ";
      boolean isUpdate = rs.executeUpdate(updateFreezeSql,requestId);
    }catch (Exception e){
      e.printStackTrace();
      rs.writeLog("handleDrawBack:删除action报错:"+e.getMessage());
    }
  }
  /**
   * 强制收回操作
   * @param requestId
   * @param workflowId
   */
  public void handleDrawBack(int requestId, int workflowId) {
    kqLog.writeLog("handleDrawBack:进入 强制收回:requestId:"+requestId+":workflowId:"+workflowId);

    RecordSet rs = new RecordSet();
    try {
      String updateFreezeSql = "update KQ_ATT_VACATION set status=2 where requestId=? and workflowId = ? ";
      boolean isUpdate = rs.executeUpdate(updateFreezeSql,requestId,workflowId);
      kqLog.writeLog("handleDrawBack:强制收回isUpdate:"+isUpdate+":updateFreezeSql:"+updateFreezeSql+":requestId:"+requestId+":workflowId:"+workflowId);
    }catch (Exception e){
      e.printStackTrace();
      kqLog.writeLog("handleDrawBack:强制收回action报错:"+e.getMessage());
    }
  }

  /**
   * 强制归档操作
   * @param requestId
   * @param workflowId
   */
  public void handleforceOver(int requestId, int workflowId) {
    RecordSet rs = new RecordSet();
    try {
      if(!KQSettingsBiz.isforceflow_attend()){
        return ;
      }
      String proc_set_sql = "select * from kq_att_proc_set where field001 = ? ";
      rs.executeQuery(proc_set_sql, workflowId);
      if(rs.next()) {
        String proc_set_id = rs.getString("id");
        //得到这个考勤流程设置是否使用明细
        String usedetails = rs.getString("usedetail");
        int kqtype = Util.getIntValue(rs.getString("field006"));
        kqLog.info("handleforceOver:proc_set_id:"+proc_set_id+":kqtype:"+kqtype+":requestId:"+requestId);
        Map<String, String> map = new HashMap<String, String>();
        if(requestId > 0){
          map.put("requestId", "and t.requestId = " + requestId);
        }
        Map<String,String> result = handleKQFlowAction(proc_set_id,usedetails,requestId,kqtype,workflowId,true,false,map);
        if(!result.isEmpty()){
          kqLog.info("handleforceOver:强制归档action失败:requestId："+requestId+":workflowId:"+workflowId+":proc_set_id:"+proc_set_id);
        }
      }
    }catch (Exception e){
      e.printStackTrace();
      kqLog.info("handleforceOver:强制归档action报错:"+e.getMessage());
    }
  }

  public Map<String,String> handleKQFlowAction(String proc_set_id, String usedetails, int requestId, int kqtype,
                                               int workflowId,boolean isForce,boolean isUpgrade,Map<String, String> map) throws Exception{
    Map<String,String> result = new HashMap<>();
    ResourceComInfo rci = new ResourceComInfo();
    List<SplitBean> splitBeans = new ArrayList<SplitBean>();

    DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    //生成处理的sql
    Map<String, String> sqlMap = handleSql(proc_set_id, usedetails, requestId,kqtype,map);
    if(sqlMap != null && !sqlMap.isEmpty()){
      KqSplitFlowTypeEnum flowTypeEnum = null;
      if(kqtype == KqSplitFlowTypeEnum.LEAVE.getFlowtype()){
        KQFlowLeaveUtil kqFlowLeaveUtil = new KQFlowLeaveUtil();
        result=kqFlowLeaveUtil.handleKQLeaveAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.LEAVE;
      }else if(kqtype == KqSplitFlowTypeEnum.EVECTION.getFlowtype()){
        KQFlowEvectionUtil kqFlowEvectionUtil = new KQFlowEvectionUtil();
        result=kqFlowEvectionUtil.handleKQEvectionAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.EVECTION;
      }else if(kqtype == KqSplitFlowTypeEnum.OUT.getFlowtype()){
        KQFlowOutUtil kqFlowOutUtil = new KQFlowOutUtil();
        result=kqFlowOutUtil.handleKQOutAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.OUT;
      }else if(kqtype == KqSplitFlowTypeEnum.OVERTIME.getFlowtype()){
        KQFlowOvertimeUtil kqFlowOvertimeUtil = new KQFlowOvertimeUtil();
        result=kqFlowOvertimeUtil.handleKQOvertimeAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci,"");
        flowTypeEnum = KqSplitFlowTypeEnum.OVERTIME;
      }else if(kqtype == KqSplitFlowTypeEnum.SHIFT.getFlowtype()){
        KQFlowShiftUtil kqFlowShiftUtil = new KQFlowShiftUtil();
        kqFlowShiftUtil.handleKQShiftAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.SHIFT;
      }else if(kqtype == KqSplitFlowTypeEnum.OTHER.getFlowtype()){
        KQFlowOtherUtil kqFlowOtherUtil = new KQFlowOtherUtil();
        result=kqFlowOtherUtil.handleKQOtherAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.OTHER;
      }else if(kqtype == KqSplitFlowTypeEnum.CARD.getFlowtype()){
        KQFlowCardUtil kqFlowCardUtil = new KQFlowCardUtil();
        result=kqFlowCardUtil.handleKQCardAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.CARD;
      }else if(kqtype == KqSplitFlowTypeEnum.LEAVEBACK.getFlowtype()){
        KQFlowLeaveBackUtil kqFlowLeaveBackUtil = new KQFlowLeaveBackUtil();
        result=kqFlowLeaveBackUtil.handleKQLeaveBackAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.LEAVEBACK;
      }else if(kqtype == KqSplitFlowTypeEnum.PROCESSCHANGE.getFlowtype()){
        KQFlowProcessChangeUtil kqFlowProcessChangeUtil = new KQFlowProcessChangeUtil();
        result=kqFlowProcessChangeUtil.handleKQProcessChangeAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
        flowTypeEnum = KqSplitFlowTypeEnum.PROCESSCHANGE;
      }else{
        new BaseBean().writeLog("考勤流程没有找到对应类型:proc_set_id:"+proc_set_id+":requestId:"+requestId+":kqtype:"+kqtype);
      }
      if(!result.isEmpty()){
        if(!isUpgrade){
          return result;
        }
      }
      if(!splitBeans.isEmpty() && flowTypeEnum != null){
        if(flowTypeEnum == KqSplitFlowTypeEnum.LEAVEBACK){
          KQFlowLeaveBackUtil kqFlowLeaveBackUtil = new KQFlowLeaveBackUtil();
          //销假流程需要再拆分下
          kqFlowLeaveBackUtil.handleSplitFLowActionData4LeaveBack(splitBeans,result,isUpgrade,requestId);
          clear_flow_deduct_card(splitBeans,KqSplitFlowTypeEnum.LEAVEBACK.getFlowtype()+"",requestId);
        }else if(flowTypeEnum == KqSplitFlowTypeEnum.PROCESSCHANGE){
          KQFlowProcessChangeUtil kqFlowProcessChangeUtil = new KQFlowProcessChangeUtil();
          kqFlowProcessChangeUtil.handleSplitFLowActionData4ProcessChange(splitBeans,result,isUpgrade,requestId);
        }else if(flowTypeEnum != KqSplitFlowTypeEnum.CARD && flowTypeEnum != KqSplitFlowTypeEnum.SHIFT){
          KQFlowUtil kqFlowUtil = new KQFlowUtil();
          kqFlowUtil.handleSplitFLowActionData(splitBeans,flowTypeEnum,rci,result,isForce,requestId,workflowId,isUpgrade);
          //流程抵扣打卡
          handle_flow_deduct_card(workflowId,splitBeans,requestId);
        }
      }
    }
    return result;
  }

  /**
   *  流程抵扣打卡逻辑
   * @param workflowId
   * @param splitBeans
   * @param requestId
   */
  public void handle_flow_deduct_card(int workflowId,
                                      List<SplitBean> splitBeans, int requestId) {
    KQAttProcSetComInfo kqAttProcSetComInfo = new KQAttProcSetComInfo();
    String flow_deduct_card = Util.null2String(kqAttProcSetComInfo.getFlow_deduct_card(""+workflowId));
    String kqType = Util.null2String(kqAttProcSetComInfo.getkqType(""+workflowId));
    kqLog.info("handle_flow_deduct_card:flow_deduct_card:"+ flow_deduct_card+":workflowId:"+workflowId+":requestId:"+requestId);
    if("1".equalsIgnoreCase(flow_deduct_card)){
      do_flow_deduct_card(splitBeans,kqType,requestId);
    }else{
//      这里扩展个操作，如果没有开启就先把对应的requestid删除掉
      if(requestId > 0){
        RecordSet rs = new RecordSet();
        String sql1 = "select * from kq_flow_deduct_card where requestid = ? ";
        rs.executeQuery(sql1,requestId);
        List<String> formateList = new ArrayList<>();
        while (rs.next()){
          String resourceid = rs.getString("resourceid");
          String belongdate = rs.getString("belongdate");
          String key = resourceid+"_"+belongdate;
          formateList.add(key);
        }
        String sql = "delete from kq_flow_deduct_card where requestid = ? ";
        rs.executeUpdate(sql, requestId);
        kqLog.info("clear_flow_deduct_card:formateList:"+formateList);
        for(String format: formateList){
          kqLog.info("clear_flow_deduct_card:format:"+ JSON.toJSONString(format));
          String[] formats = format.split("_");
          new KQFormatData().formatKqDate(formats[0],formats[1]);
        }
      }
    }
  }

  /**
   * 销假流程 消除流程抵扣打卡
   * @param splitBeans
   * @param kqType
   * @param requestId
   */
  public void clear_flow_deduct_card(List<SplitBean> splitBeans, String kqType, int requestId) {
    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    List<String> clear_ids = new ArrayList<>();
    List<String> formateList = new ArrayList<>();
    List updateParamList = new ArrayList();
    List updateList = new ArrayList();
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
    for(SplitBean bean : splitBeans){
      String leavebackrequestid = bean.getLeavebackrequestid();
      String resourceId = bean.getResourceId();
      String belongDate = bean.getBelongDate();
      String fromdate = bean.getFromDate();
      String fromtime = bean.getFromTime();
      String todate = bean.getToDate();
      String totime = bean.getToTime();
      String key = resourceId+"_"+belongDate;
      formateList.add(key);
      int fromtimeIdx = kqTimesArrayComInfo.getArrayindexByTimes(fromtime);
      int totimeIdx = kqTimesArrayComInfo.getArrayindexByTimes(totime);
      String deduct_sql = "select * from kq_flow_deduct_card where requestid = '"+leavebackrequestid+"' and resourceId='"+resourceId+"' and belongDate='"+belongDate+"'";
      rs.executeQuery(deduct_sql);
      while(rs.next()){
        String deduct_id = rs.getString("id");
        String signtype = rs.getString("signtype");
        String deduct_fromtime = rs.getString("fromtime");
        String deduct_totime = rs.getString("totime");
        int deduct_fromtimeIdx = kqTimesArrayComInfo.getArrayindexByTimes(deduct_fromtime);
        int deduct_totimeIdx = kqTimesArrayComInfo.getArrayindexByTimes(deduct_totime);
        updateList = new ArrayList();
        if("1".equalsIgnoreCase(signtype)){
          if(fromtimeIdx == deduct_fromtimeIdx && !clear_ids.contains(deduct_id)){
            clear_ids.add(deduct_id);
            updateList.add(deduct_id);
            updateParamList.add(updateList);
          }
        }else if("2".equalsIgnoreCase(signtype)){
          if(totimeIdx == deduct_totimeIdx && !clear_ids.contains(deduct_id)){
            clear_ids.add(deduct_id);
            updateList.add(deduct_id);
            updateParamList.add(updateList);
          }
        }
      }
    }
    kqLog.info("clear_flow_deduct_card:clear_ids:"+ clear_ids.size());
//    if(!clear_ids.isEmpty()){
//      for(String id : clear_ids){
//        String sql = "update kq_flow_deduct_card set isclear=1 where id = "+id;
//        rs1.executeUpdate(sql);
//      }
    /*更新抵扣打卡数据 start*/
    boolean isSuccess = true;
    String updateSql = "update kq_flow_deduct_card set isclear=1 where id=?";
    if (updateParamList.size() > 0) {
      isSuccess = rs1.executeBatchSql(updateSql, updateParamList);
      kqLog.info("clear_flow_deduct_card:formateList:"+formateList);
      for(String format: formateList){
        kqLog.info("clear_flow_deduct_card:format:"+ JSON.toJSONString(format));
        String[] formats = format.split("_");
        new KQFormatData().formatKqDate(formats[0],formats[1]);
      }
    }
    /*更新抵扣打卡数据 end*/

//  }
  }

  /**
   *  流程抵扣打卡逻辑
   * @param splitBeans
   * @param kqType
   * @param requestId
   */
  public void do_flow_deduct_card(List<SplitBean> splitBeans, String kqType, int requestId) {
    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    String batchSql = "insert into kq_flow_deduct_card(requestid,resourceid,belongDate,fromdate,fromtime,todate,totime,workBeginTime,workEndTime,signtype,flowtype,serialnumber)"+
            " values(?,?,?,?,?,?,?,?,?,?,?,?) ";
    List<List> params = new ArrayList<List>();
    List<String> formateList = new ArrayList<>();
    kqLog.info("handle_flow_deduct_card:splitBeans:"+ JSON.toJSONString(splitBeans));
    new BaseBean().writeLog("==zj==(splitBeans)" + JSON.toJSONString(splitBeans));
    for(SplitBean bean : splitBeans){
      do_flow_deduct_in_table(bean,formateList,params,kqType,requestId, "");

      if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.EVECTION.getFlowtype()){
        String companion = Util.null2s(bean.getCompanion(), "");
        if(companion.length() > 0){
          String[] companions = companion.split(",");
          if(companions != null && companions.length > 0 ) {
            for (int i = 0; i < companions.length; i++) {
              String compan_resid = companions[i];
              if(bean.getResourceId().equalsIgnoreCase(compan_resid)){
                //陪同人是自己的不要存到中间表里
                continue;
              }
              do_flow_deduct_in_table(bean,formateList,params,kqType,requestId,compan_resid);
            }
          }
        }
      }
    }
    if(!params.isEmpty()) {
      //先根据requestid删除中间表里的数据，再做啥插入操作
      String delSql = "delete from kq_flow_deduct_card where requestid = " + requestId;
      rs.executeUpdate(delSql);
      boolean isOk = rs1.executeBatchSql(batchSql, params);
      if(!isOk){
        kqLog.info("do_flow_deduct_card:requestId:"+requestId+":kq_flow_deduct_card error："+params);
        return ;
      }
      kqLog.info("do_flow_deduct_card:formateList:"+formateList);
      for(String format: formateList){
        kqLog.info("do_flow_deduct_card:format:"+ JSON.toJSONString(format));
        String[] formats = format.split("_");
        new KQFormatData().formatKqDate(formats[0],formats[1]);
      }
    }else{
      if(!splitBeans.isEmpty()){
        //有流程数据，但是签到签退都不包含的情况，支持多次归档，所以需要先删除再插入
        String delSql = "delete from kq_flow_deduct_card where requestid = " + requestId;
        rs.executeUpdate(delSql);
      }
    }
  }

  /**
   * 流程抵扣打卡数据整合到表里
   * @param bean
   * @param formateList
   * @param params
   * @param kqType
   * @param requestId
   * @param compan_resid 出差流程的陪同人
   */
  public void do_flow_deduct_in_table(SplitBean bean, List<String> formateList,
                                      List<List> params, String kqType, int requestId, String compan_resid) {
    KQWorkTime kqWorkTime = new KQWorkTime();
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
    String resourceId = bean.getResourceId();
    if(compan_resid.length() > 0 && Util.getIntValue(kqType) == KqSplitFlowTypeEnum.EVECTION.getFlowtype()){
      resourceId = compan_resid;
    }
    String belongDate = bean.getBelongDate();
    String fromdate = bean.getFromDate();
    String fromtime = bean.getFromTime();
    String todate = bean.getToDate();
    String totime = bean.getToTime();
    boolean oneSign = false;
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(resourceId, belongDate);
    String key = resourceId+"_"+belongDate;
    formateList.add(key);
    List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
    List<TimeScopeEntity> lsSignTime = new ArrayList<>();
    if (workTime != null) {
      lsWorkTime = workTime.getWorkTime();//工作时间
      new BaseBean().writeLog("==zj==(lsWorkTime)" + JSON.toJSONString(lsWorkTime));
      lsSignTime = workTime.getSignTime();//允许打卡时间
      oneSign = lsWorkTime!=null&&lsWorkTime.size()==1;
      for (int i = 0; lsWorkTime != null && i < lsWorkTime.size(); i++) {
        TimeScopeEntity workTimeScope = lsWorkTime.get(i);
        String workBeginTime = Util.null2String(workTimeScope.getBeginTime());
        int workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
        boolean workBenginTimeAcross = workTimeScope.getBeginTimeAcross();
        String workEndTime = Util.null2String(workTimeScope.getEndTime());
        int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
        boolean workEndTimeAcross = workTimeScope.getEndTimeAcross();
        int[] dayMins = new int[2880];//一天所有分钟数
        int beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(fromtime));
        int endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(totime));
        if(beginIdx > endIdx){
          continue;
        }
        if(oneSign){
          boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
          if(is_flow_humanized){
            //个性化设置只支持一天一次上下班
            ShiftInfoBean shiftInfoBean = new ShiftInfoBean();
            Map<String, String> shifRuleMap = Maps.newHashMap();
            shiftInfoBean.setSplitDate(belongDate);
            shiftInfoBean.setShiftRuleMap(workTime.getShiftRuleInfo());
            shiftInfoBean.setSignTime(lsSignTime);
            shiftInfoBean.setWorkTime(lsWorkTime);
            KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean, resourceId, shifRuleMap);
            if(!shifRuleMap.isEmpty()){
              if(shifRuleMap.containsKey("shift_beginworktime")){
                String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
                if(shift_beginworktime.length() > 0){
                  workBeginTime = Util.null2String(shift_beginworktime);
                  workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
                }
              }
              if(shifRuleMap.containsKey("shift_endworktime")){
                String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
                if(shift_endworktime.length() > 0){
                  workEndTime = Util.null2String(shift_endworktime);
                  workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
                }
              }
            }
          }
        }
        if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.EVECTION.getFlowtype()){
          Arrays.fill(dayMins, beginIdx, endIdx, 7);//出差抵扣时段标识 7
        }else if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.OUT.getFlowtype()){
          Arrays.fill(dayMins, beginIdx, endIdx, 8);//公出抵扣时段标识 8
        }else if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.LEAVE.getFlowtype()){
          if (endIdx > beginIdx) {
            Arrays.fill(dayMins, beginIdx, endIdx, 5);//流程抵扣时段标识 5
          }
        }else{
          if (endIdx > beginIdx) {
            Arrays.fill(dayMins, beginIdx, endIdx, 99);//异常流程抵扣时段标识99
          }
        }

        int cnt_7 = kqTimesArrayComInfo.getCnt(dayMins, workBeginIdx, workEndIdx, 7);
        int cnt_8 = kqTimesArrayComInfo.getCnt(dayMins, workBeginIdx, workEndIdx, 8);
        int cnt_5 = kqTimesArrayComInfo.getCnt(dayMins, workBeginIdx, workEndIdx, 5);
        int cnt_99 = kqTimesArrayComInfo.getCnt(dayMins, workBeginIdx, workEndIdx, 99);
        //流程时长大于0的才去做流程抵扣的处理
        if(cnt_7 > 0 || cnt_8 > 0 || cnt_5 > 0 || cnt_99 > 0){
          //如果流程时长大于0 结束时间因为是下标，需要补1分钟
          if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.EVECTION.getFlowtype()){
            Arrays.fill(dayMins, endIdx, endIdx+1, 7);//出差抵扣时段标识 7
          }else if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.OUT.getFlowtype()){
            Arrays.fill(dayMins, endIdx, endIdx+1, 8);//公出抵扣时段标识 8
          }else if(Util.getIntValue(kqType) == KqSplitFlowTypeEnum.LEAVE.getFlowtype()){
            Arrays.fill(dayMins, endIdx, endIdx+1, 5);//流程抵扣时段标识 5
          }else{
            Arrays.fill(dayMins, endIdx, endIdx+1, 99);//异常流程抵扣时段标识99
          }

          new BaseBean().writeLog("==zj==(FlowAction)" + JSON.toJSONString(dayMins));
          if(dayMins[workBeginIdx]>=5){//签到时间点有流程
            List<Object> beanParams =  new ArrayList<Object>();
            beanParams.add(requestId);
            beanParams.add(resourceId);
            beanParams.add(belongDate);
            beanParams.add(fromdate);
            beanParams.add(fromtime);
            beanParams.add(todate);
            beanParams.add(totime);
            beanParams.add(workBeginTime);
            beanParams.add(workEndTime);
            beanParams.add("1");
            beanParams.add(kqType);
            beanParams.add(i);
            params.add(beanParams);
          }

          if(dayMins[workEndIdx]>=5){//签退时间点有流程
            List<Object> beanParams =  new ArrayList<Object>();
            beanParams.add(requestId);
            beanParams.add(resourceId);
            beanParams.add(belongDate);
            beanParams.add(fromdate);
            beanParams.add(fromtime);
            beanParams.add(todate);
            beanParams.add(totime);
            beanParams.add(workBeginTime);
            beanParams.add(workEndTime);
            beanParams.add("2");
            beanParams.add(kqType);
            beanParams.add(i);
            params.add(beanParams);
          }
        }else{
          kqLog.info("handle_flow_deduct_card::i:"+i+":workBeginIdx:"+ workBeginIdx+":workEndIdx:"+ workEndIdx
                  +":cnt_7:"+cnt_7+":cnt_8:"+cnt_8+":cnt_5:"+cnt_5+":cnt_99:"+cnt_99);
        }
      }
    }else{
      kqLog.info("handle_flow_deduct_card:workTime is null :resourceId:"+ resourceId+":belongDate:"+belongDate);
    }
  }


  /**
   * 拆分请假数据 生成splitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @param durationTypeEnum
   * @return
   * @throws Exception
   */
  public Map<String,String> handleAction(Map<String, String> sqlMap,
                                         List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                         int requestId, ResourceComInfo rci,DurationTypeEnum durationTypeEnum) throws Exception{
    RecordSet rs1 = new RecordSet();
    KQFlowUtil kqFlowUtil = new KQFlowUtil();
    Map<String,String> result = new HashMap<>();

    if(!sqlMap.isEmpty()){
      for(Map.Entry<String,String> me : sqlMap.entrySet()){
        String key = me.getKey();
        String value = me.getValue();

        rs1.execute(value);
        while (rs1.next()) {
          SplitBean splitBean = new SplitBean();
          boolean isFillRight = kqFlowUtil.fillSplitBean(splitBean, rs1, ""+requestId, rci, ""+workflowId, durationTypeEnum,key,result,datetimeFormatter,
                  "");
          if(!isFillRight){
            new BaseBean().writeLog("升级失败："+requestId+";;result>>>"+JSONObject.toJSONString(result));
            continue ;
          }
          if(result.containsKey("isProcessDrawBack")){
            result.clear();
            splitBeans.add(splitBean);
          }else{
            doWorkSplitChain(splitBean, splitBeans);
          }

          if(durationTypeEnum == DurationTypeEnum.EVECTION){
            String companion = Util.null2s(splitBean.getCompanion(), "");
            if(companion.length() > 0){
              List<String> companionList = Util.splitString2List(companion,",");
              for(int i = 0 ; i < companionList.size() ; i++){
                String be_companion = Util.null2String(companionList.get(i));
                if(be_companion.length() > 0 && Util.getIntValue(be_companion) > 0){
                  SplitBean compSplitBean = new SplitBean();
                  boolean compFillRight = kqFlowUtil.fillSplitBean(compSplitBean, rs1, ""+requestId,
                          rci, ""+workflowId, durationTypeEnum,key,result,datetimeFormatter, "");
                  if(!compFillRight){
                    return result;
                  }
                  if(splitBean.getResourceId().equalsIgnoreCase(be_companion)){
                    //陪同人是自己的不要存到中间表里
                    continue;
                  }
                  compSplitBean.setResourceId(be_companion);
                  compSplitBean.setSubcompanyid(Util.null2s(rci.getSubCompanyID(be_companion),"0"));
                  compSplitBean.setDepartmentid(Util.null2s(rci.getDepartmentID(be_companion),"0"));
                  compSplitBean.setJobtitle(Util.null2s(rci.getJobTitle(be_companion),"0"));
                  compSplitBean.setIscompanion("1");
                  compSplitBean.setCompanion("");

                  if(result.containsKey("isProcessDrawBack")){
                    result.clear();
                    splitBeans.add(compSplitBean);
                  }else{
                    doWorkSplitChain(compSplitBean, splitBeans);
                  }
                }
              }
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * 生成处理的sql
   * @param proc_set_id
   * @param usedetails
   * @param requestidInt
   * @return
   */
  public Map<String,String> handleSql(String proc_set_id,String usedetails,int requestidInt,int kqtype,Map<String, String> map) {
    HrmAttProcSetManager hrmAttProcSetManager= new HrmAttProcSetManager();
    Map<String,String> sqlMap = new HashMap<>();
    sqlMap = hrmAttProcSetManager.getSQLByField006Map(kqtype, map, false, true, proc_set_id,usedetails);

    return sqlMap;
  }

  /**
   * 计算工作时长拆分
   * @param splitBean
   * @param splitBeans
   * @throws Exception
   */
  public void doWorkSplitChain(SplitBean splitBean,List<SplitBean> splitBeans) throws Exception{

    WorkDurationChain hourUnitSplitChain = new WorkHourUnitSplitChain(splitBeans);
    WorkDurationChain dayUnitSplitChain = new WorkDayUnitSplitChain(splitBeans);
    WorkDurationChain halfUnitSplitChain = new WorkHalfUnitSplitChain(splitBeans);
    WorkDurationChain wholeUnitSplitChain = new WorkWholeUnitSplitChain(splitBeans);

    //设置执行链
    hourUnitSplitChain.setDurationChain(dayUnitSplitChain);
    dayUnitSplitChain.setDurationChain(halfUnitSplitChain);
    halfUnitSplitChain.setDurationChain(wholeUnitSplitChain);
    //把初始数据设置进去
    hourUnitSplitChain.handleDuration(splitBean);
  }

  /**
   * 测试流程，删除测试数据的同时
   * 1、需要把冻结数据释放
   * 2、中间表数据清除
   * 3、格式化考勤报表
   * @param temprequestid
   * @param workflowid
   */
  public void delTest(int temprequestid, String workflowid,String from) {
    RecordSet rs = new RecordSet();
    RecordSet rs1 = new RecordSet();
    RecordSet rs2 = new RecordSet();
    RecordSet rs3 = new RecordSet();
    try {

      kqLog.info("KQFlowActiontBiz delTest:workflowid:"+workflowid+":from:"+from);
      if(Util.null2s(workflowid,"").length() == 0){
        return ;
      }

      String getkqType = "select * from kq_att_proc_set where field001=? ";
      rs.executeQuery(getkqType,workflowid);
      if(rs.next()){
        boolean isCard = false;
        boolean isOvertime = false;
        String tablename = "";
        int kqtype= Util.getIntValue(rs.getString("field006"),-1);
        if(kqtype == KqSplitFlowTypeEnum.LEAVE.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.LEAVE.getTablename();
          String updateFreezeSql = "update KQ_ATT_VACATION set status=2 where requestId=?  ";
          boolean isUpdate = rs1.executeUpdate(updateFreezeSql,temprequestid);
          kqLog.info("KQFlowActiontBiz delTest:updateFreezeSql:"+updateFreezeSql+":temprequestid:"+temprequestid+":isUpdate:"+isUpdate);
        }else if(kqtype == KqSplitFlowTypeEnum.EVECTION.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.EVECTION.getTablename();
        }else if(kqtype == KqSplitFlowTypeEnum.OUT.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.OUT.getTablename();
        }else if(kqtype == KqSplitFlowTypeEnum.OVERTIME.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.OVERTIME.getTablename();
          isOvertime = true;
        }else if(kqtype == KqSplitFlowTypeEnum.SHIFT.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.SHIFT.getTablename();
        }else if(kqtype == KqSplitFlowTypeEnum.OTHER.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.OTHER.getTablename();
        }else if(kqtype == KqSplitFlowTypeEnum.CARD.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.CARD.getTablename();
          isCard = true;
        }else if(kqtype == KqSplitFlowTypeEnum.LEAVEBACK.getFlowtype()){
          tablename = KqSplitFlowTypeEnum.LEAVEBACK.getTablename();
        }else{
          new BaseBean().writeLog("删除测试流程异常，未找到考勤流程类型:workflowid:"+workflowid);
        }
        if(tablename.length() > 0){
          KQFormatBiz kqFormatBiz = new KQFormatBiz();

          List<String> formateList = new ArrayList<>();
          if(isCard){
            String signfrom_param = "|requestid|"+temprequestid;
            String selTableSql = "select * from hrmschedulesign where signfrom like ? ";
            rs2.executeQuery(selTableSql,"%"+signfrom_param+"%");
            while (rs2.next()){
              String userid = rs2.getString("userid");
              String signdate = rs2.getString("signdate");
              String key = userid+"_"+signdate;
              formateList.add(key);
            }
            kqLog.info("KQFlowActiontBiz isCard delTest:formateList:"+formateList);

            String delTableSql = "delete from hrmschedulesign where signfrom like ? ";
            boolean isUpdate = rs3.executeUpdate(delTableSql,"%"+signfrom_param+"%");
            kqLog.info("KQFlowActiontBiz isCard delTest:delTableSql:"+delTableSql+":temprequestid:"+temprequestid+":isUpdate:"+isUpdate);
            if(isUpdate){
              for(String formateStr : formateList){
                String[] formateStrs = formateStr.split("_");
                new KQFormatData().formatKqDate(formateStrs[0],formateStrs[1]);
              }
            }
          }else if(isOvertime){
            SplitActionUtil.clearSameRequestTX(temprequestid+"");
            String delTableSql = "delete from "+tablename+"  where requestId= ?  ";
            boolean isUpdate = rs2.executeUpdate(delTableSql,temprequestid);
            kqLog.info("KQFlowActiontBiz delTest:delTableSql:"+delTableSql+":temprequestid:"+temprequestid+":isUpdate:"+isUpdate);
          }else{
            String sql = "select * from "+tablename+"  where requestId= ?  ";
            rs2.executeQuery(sql, temprequestid);
            while (rs2.next()){
              String resourceid = rs2.getString("resourceid");
              String belongdate = rs2.getString("belongdate");
              String key = resourceid+"_"+belongdate;
              formateList.add(key);
            }
            String delTableSql = "delete from "+tablename+"  where requestId= ?  ";
            boolean isUpdate = rs2.executeUpdate(delTableSql,temprequestid);
            kqLog.info("KQFlowActiontBiz delTest:delTableSql:"+delTableSql+":temprequestid:"+temprequestid+":isUpdate:"+isUpdate);
            //如果有流程抵扣，删除对应流程抵扣的数据
            String delSql = "delete from kq_flow_deduct_card where requestid = ? ";
            isUpdate = rs3.executeUpdate(delSql,temprequestid);
            kqLog.info("KQFlowActiontBiz delTest:delSql:"+delSql+":temprequestid:"+temprequestid+":isUpdate:"+isUpdate);

            kqLog.info("KQFlowActiontBiz delTest:formateList:"+formateList);
            for(String formateStr : formateList){
              String[] formateStrs = formateStr.split("_");
              new KQFormatData().formatKqDate(formateStrs[0],formateStrs[1]);
            }
          }
        }
      }
    }catch (Exception e){
      e.printStackTrace();
      rs.writeLog("KQFlowActiontBiz delTest:temprequestid:"+temprequestid+":workflowid:"+workflowid+":报错:"+e.getMessage());
    }
  }

  /**
   * 拆分请假数据 生成splitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQLeaveAction(Map<String, String> sqlMap,
                                                List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                                int requestId, ResourceComInfo rci) throws Exception{
    KQFlowLeaveUtil kqFlowLeaveUtil = new KQFlowLeaveUtil();
    return kqFlowLeaveUtil.handleKQLeaveAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
  }

  /**
   * 拆分出差数据 生成splitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQEvectionAction(Map<String, String> sqlMap,
                                                   List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                                   int requestId, ResourceComInfo rci) throws Exception{
    KQFlowEvectionUtil kqFlowEvectionUtil = new KQFlowEvectionUtil();
    return kqFlowEvectionUtil.handleKQEvectionAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
  }

  /**
   * 拆分公出数据 生成splitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQOutAction(Map<String, String> sqlMap,
                                              List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                              int requestId, ResourceComInfo rci) throws Exception{
    KQFlowOutUtil kqFlowOutUtil = new KQFlowOutUtil();
    return kqFlowOutUtil.handleKQOutAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
  }

  /**
   * 拆分加班数据 生成splitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQOvertimeAction(Map<String, String> sqlMap,
                                                   List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                                   int requestId, ResourceComInfo rci) throws Exception{
    KQFlowOvertimeUtil kqFlowOvertimeUtil = new KQFlowOvertimeUtil();
    return kqFlowOvertimeUtil.handleKQOvertimeAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci,"");
  }
  /**
   * 拆分排班数据
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public void handleKQShiftAction(Map<String, String> sqlMap,
                                  List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                  int requestId, ResourceComInfo rci) throws Exception{
    KQFlowShiftUtil kqFlowShiftUtil = new KQFlowShiftUtil();
    kqFlowShiftUtil.handleKQShiftAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
  }
  /**
   * 补卡流程数据 生成签到签退数据并更新考勤报表
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQCardAction(Map<String, String> sqlMap,
                                               List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                               int requestId, ResourceComInfo rci) throws Exception{
    KQFlowCardUtil kqFlowCardUtil = new KQFlowCardUtil();
    return kqFlowCardUtil.handleKQCardAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
  }
  /**
   * 拆分销假流程数据 生成数据到splitBeans和backsplitBeans
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public Map<String,String> handleKQLeaveBackAction(Map<String, String> sqlMap,
                                                    List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
                                                    int requestId, ResourceComInfo rci) throws Exception{
    KQFlowLeaveBackUtil kqFlowLeaveBackUtil = new KQFlowLeaveBackUtil();
    return kqFlowLeaveBackUtil.handleKQLeaveBackAction(sqlMap,splitBeans,datetimeFormatter,workflowId,requestId,rci);
  }
}
