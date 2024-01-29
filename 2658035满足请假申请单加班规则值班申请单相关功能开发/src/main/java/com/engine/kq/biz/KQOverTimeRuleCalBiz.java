package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.api.customization.qc2658035.util.OverTimeUtil;
import com.api.customization.qc2658035.util.UserUtil;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.KQOvertimeRulesDetailEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.timer.KQOvertimeCardBean;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.wfset.bean.OvertimeBalanceTimeBean;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 根据加班规则计算加班和调休数据
 * 主要是针对 流程+打卡 打卡 这几种方式
 */
public class KQOverTimeRuleCalBiz {
  private KQLog kqLog = new KQLog();

  /**
   * 生成加班数据
   * @param resourceid
   * @param fromDate
   * @param toDate
   * @param eventtype
   */
  public void buildOvertime(String resourceid, String fromDate, String toDate, String eventtype){

    try{
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
      Map<String,Integer> changeTypeMap = Maps.newHashMap();
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap = Maps.newHashMap();
      Map<String, List<String[]>> restTimeMap = Maps.newHashMap();
      Map<String,Integer> computingModeMap = Maps.newHashMap();
      //先获取一些前提数据，加班規則和假期規則
      getOverTimeDataMap(resourceid, fromDate, toDate, dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,computingModeMap);
      Map<String,Object> eventLogMap = Maps.newHashMap();
      eventLogMap.put("fromDate", fromDate);
      eventLogMap.put("toDate", toDate);
      eventLogMap.put("eventtype", eventtype);
      KQOvertimeLogBiz kqEventLogBiz = new KQOvertimeLogBiz();
      String logKey = "|key|"+resourceid+"_"+fromDate+"_"+toDate;
      String uuid = kqEventLogBiz.logEvent(resourceid,eventLogMap,"buildOvertime|生成加班调休"+logKey);

      LocalDate localFromDate = LocalDate.parse(fromDate);
      LocalDate localToDate = LocalDate.parse(toDate);
      LocalDate preFromDate = localFromDate.minusDays(1);
      LocalDate nextToDate = localToDate.plusDays(1);
      //之前是考虑外部考勤数据导入跨天打卡，判断归属的问题，向前算一天，现在不管了，都是默认只处理当天的
      if(eventtype.indexOf("#flow,") > -1 || eventtype.indexOf("punchcard") > -1 || true){
        //如果是正常走的加班流程，就是流程开始日期和结束日期，只有补卡，打卡，同步的时候才需要处理一下前一天和后一天的数据
        preFromDate = localFromDate;
        nextToDate = localToDate;
      }
      long betweenDays = nextToDate.toEpochDay() - preFromDate.toEpochDay();
      for (int i = 0; i <= betweenDays; i++) {
        LocalDate curLocalDate = preFromDate.plusDays(i);
        String splitDate = curLocalDate.format(dateFormatter);
        String key = resourceid + "_" + splitDate;
        String change_key = splitDate + "_" + resourceid;
        int changeType = Util.getIntValue("" + changeTypeMap.get(change_key), -1);
        String changeType_key = splitDate + "_" + changeType;
        int computingMode = Util.getIntValue(""+computingModeMap.get(changeType_key),-1);
        if(computingMode == 2){
//        需审批，以打卡为准，但是不能超过审批时长
          doComputingMode2(resourceid,splitDate,dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,
              computingModeMap,kqTimesArrayComInfo,uuid);
        }
        if(computingMode == 3){
//        无需审批，根据打卡时间计算加班时长
          doComputingMode3(resourceid,splitDate,dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,
              computingModeMap,kqTimesArrayComInfo,uuid);
        }
        if(computingMode == 4){
//        需审批，以打卡为准，取流程和打卡的交集
          doComputingMode4(resourceid,splitDate,dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,
              computingModeMap,kqTimesArrayComInfo,uuid);
        }
      }
	  fromDate=preFromDate.format(dateFormatter);
      updateTiaoXiu(resourceid, fromDate, toDate);	  
    }catch (Exception e){
      kqLog.info("加班生成数据报错:KQOverTimeRuleCalBiz:");
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
    }
  }


  /**
   * 更新调休
   * @param resourceid
   * @param fromDate
   * @param toDate
   */
  public void updateTiaoXiu(String resourceid, String fromDate, String toDate){
    //假期类型的缓存类
    KQLeaveRulesComInfo rulesComInfo = new KQLeaveRulesComInfo();
    //[调休]的假期类型的ID
    String leaveRulesId = "";
    //找到[调休]的假期类型ID
    rulesComInfo.setTofirstRow();
    while (rulesComInfo.next()) {
      if (KQLeaveRulesBiz.isTiaoXiu(rulesComInfo.getId())) {
        if("1".equals(rulesComInfo.getIsEnable())){
          leaveRulesId = rulesComInfo.getId();
          break;
        }
      }
    }
    RecordSet recordSet = new RecordSet();
    String  sql = " select * from kq_balanceOfLeave " +
            " where (isDelete is null or isDelete<>1) and resourceId=" + resourceid +
            " and leaveRulesId=" + leaveRulesId +
            " and  effectiveDate>='" + fromDate + "' and effectiveDate<='" +toDate+"'"+
            " order by belongYear asc,effectiveDate asc,id asc ";
    recordSet.executeQuery(sql);
    String all_tiaoxiuids = "";
    BigDecimal duration = new BigDecimal("0");
    while (recordSet.next()) {
      //额外
      BigDecimal extraAmount = new BigDecimal(Util.null2s(recordSet.getString("extraAmount"), "0"));
      //加班生成调休
      BigDecimal tiaoxiuAmount = new BigDecimal(Util.null2s(recordSet.getString("tiaoxiuamount"), "0"));
      //已休
      BigDecimal usedAmount = new BigDecimal(Util.null2s(recordSet.getString("usedAmount"), "0"));

      if (extraAmount.add(tiaoxiuAmount).subtract(usedAmount).doubleValue() >= 0) {
        continue;
      }
      String id = recordSet.getString("id");
      if(id.length() > 0 && Util.getIntValue(id) > 0){
        all_tiaoxiuids += ","+id;
        duration =duration.add(extraAmount).add(tiaoxiuAmount).subtract(usedAmount);
      }
    }
    duration = duration.abs();
    List<String> updateList = new ArrayList<String>();
    sql = " select * from kq_balanceOfLeave " +
              " where (isDelete is null or isDelete<>1) and resourceId=" + resourceid +
              " and leaveRulesId=" + leaveRulesId +
              " and  effectiveDate>='" + fromDate + "'"+
              " order by belongYear asc,effectiveDate asc,id asc ";
    recordSet.executeQuery(sql);
    int total = recordSet.getCounts();
    int index = 0;
    while (recordSet.next()) {
      index++;
      String id = recordSet.getString("id");
      //额外
      BigDecimal extraAmount = new BigDecimal(Util.null2s(recordSet.getString("extraAmount"), "0"));
      //加班生成调休
      BigDecimal tiaoxiuAmount = new BigDecimal(Util.null2s(recordSet.getString("tiaoxiuamount"), "0"));
      //已休
      BigDecimal usedAmount = new BigDecimal(Util.null2s(recordSet.getString("usedAmount"), "0"));

      if (extraAmount.add(tiaoxiuAmount).subtract(usedAmount).doubleValue() <= 0) {
          continue;
      }
        BigDecimal temp = extraAmount.add(tiaoxiuAmount).subtract(usedAmount).subtract(duration);
        /*该假期剩余假期余额不足以扣减，记录错误日志，退出方法*/
        if (index == total && temp.doubleValue() < 0) {
          kqLog.info("该人员的该假期所有的剩余假期余额都不足以扣减。" +
                  "resourceId=" + resourceid + ",date=" + fromDate + ",ruleId=" + leaveRulesId + ",duration=" + duration + ",extraAmount=" + extraAmount.doubleValue() + ",usedAmount=" + usedAmount.doubleValue());
          String newUsedAmount = usedAmount.add(duration).setScale(5, RoundingMode.HALF_UP).toPlainString();
          String updateSql = "update kq_balanceOfLeave set usedAmount=" + (newUsedAmount) + " where id=" + id;
          updateList.add(updateSql);
          break;
        }
        if (temp.doubleValue() >= 0) {
          String newUsedAmount = usedAmount.add(duration).setScale(5, RoundingMode.HALF_UP).toPlainString();
          String updateSql = "update kq_balanceOfLeave set usedAmount=" + (newUsedAmount) + " where id=" + id;
          updateList.add(updateSql);
          break;
        } else {
          duration = duration.add(usedAmount).subtract(extraAmount).subtract(tiaoxiuAmount);
          String newUsedAmount = extraAmount.add(tiaoxiuAmount).setScale(5, RoundingMode.HALF_UP).toPlainString();
          String updateSql = "update kq_balanceOfLeave set usedAmount=" + (newUsedAmount) + " where id=" + id;
          updateList.add(updateSql);
          continue;
        }
      }
    if(all_tiaoxiuids.length() > 0) {
      all_tiaoxiuids = all_tiaoxiuids.substring(1);
      updateList.add("update kq_balanceOfLeave set isDelete=1 where id in (" +all_tiaoxiuids+")");
    }
    kqLog.info("updateList:"+updateList);
    /*SQL操作批处理*/
    for (int i = 0; i < updateList.size(); i++) {
     boolean flag = recordSet.executeUpdate(updateList.get(i));
      if (!flag) {
        kqLog.info("刷新加班流程数据失败：数据库更新失败" );
        return;
      }
    }

  }

  /**
   * 需审批，以打卡为准，取流程和打卡的交集
   * @param resourceid
   * @param splitDate
   * @param dateFormatter
   * @param changeTypeMap
   * @param overRulesDetailMap
   * @param restTimeMap
   * @param computingModeMap
   * @param kqTimesArrayComInfo
   * @param main_uuid
   */
  private void doComputingMode4(String resourceid, String splitDate,
      DateTimeFormatter dateFormatter, Map<String, Integer> changeTypeMap,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      Map<String, List<String[]>> restTimeMap, Map<String, Integer> computingModeMap,
      KQTimesArrayComInfo kqTimesArrayComInfo, String main_uuid) throws Exception{
    String key = resourceid+"_"+splitDate;
    UserUtil userUtil = new UserUtil();
    OverTimeUtil overTimeUtil = new OverTimeUtil();
    //加班日志记录类
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(resourceid, splitDate);
    Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();
    workTimeEntityLogMap.put("resourceid", resourceid);
    workTimeEntityLogMap.put("splitDate", splitDate);
    workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);
    KQOvertimeLogBiz kqEventLogBiz = new KQOvertimeLogBiz();
    String uuid = kqEventLogBiz.logDetailWorkTimeEntity(resourceid,workTimeEntityLogMap,main_uuid,"doComputingMode4|加班计算,需审批,以打卡为准,取流程和打卡的交集|key|"+key);
    Map<String,Object> overtimeLogMap = Maps.newLinkedHashMap();

    Map<String,Object> eventMap = Maps.newLinkedHashMap();
    Map<String, KQOvertimeCardBean> lsCheckInfoMaps = Maps.newLinkedHashMap();
    //获取加班打卡数据
    getOverTimeCardDataMap(resourceid, splitDate, splitDate, dateFormatter,kqTimesArrayComInfo,overRulesDetailMap,changeTypeMap,lsCheckInfoMaps,eventMap);

    if(lsCheckInfoMaps.isEmpty()){
      logOvertimeMap(overtimeLogMap, "没有打卡数据", "打卡和上下班数据|KQOvertimeCardBean");
      kqEventLogBiz.logDetailOvertimeMap(resourceid,overtimeLogMap,uuid);
      kqEventLogBiz.logDetailEvent(resourceid,eventMap,uuid,"doComputingMode4|对应的加班流程数据|key|"+key);
      return;
    }
    RecordSet rs = new RecordSet();

    Iterator<Entry<String, KQOvertimeCardBean>> iterator = lsCheckInfoMaps.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, KQOvertimeCardBean> next = iterator.next();
      String mapKey = next.getKey();
      KQOvertimeCardBean kqOvertimeCardBean = next.getValue();
      String[] mapKeys = mapKey.split("_");
      if (mapKeys.length != 2) {
        continue;
      }
      String realSplitDate = mapKeys[1];

      Map<String,List<SplitBean>> splitBeanMaps = Maps.newHashMap();
      //获取加班流程数据
      getOverTimeFlowData(resourceid,realSplitDate,realSplitDate,splitBeanMaps,dateFormatter);

      String change_key = realSplitDate+"_"+resourceid;
      int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
      String changeType_key = realSplitDate+"_"+changeType;
      String changetypeName = 1==changeType ? "节假日" : (2 == changeType ? "工作日" : (3 == changeType ? "休息日" : "异常"));
      String changetypeLogInfo = change_key+"|changeType|"+changeType+"|"+changetypeName;
      logOvertimeMap(overtimeLogMap, changetypeLogInfo, mapKey+"|"+"加班日期属性|changetypeLogInfo");

      clearOvertimeTX(resourceid, realSplitDate,overtimeLogMap,splitDate);
      logOvertimeMap(overtimeLogMap, kqOvertimeCardBean, mapKey+"|"+"打卡和上下班数据|KQOvertimeCardBean");

      KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
      if(kqOvertimeRulesDetailEntity == null){
        String overRuleInfo = "changeType_key:"+changeType_key+":kqOvertimeRulesDetailEntity:"+kqOvertimeRulesDetailEntity;
        logOvertimeMap(overtimeLogMap, overRuleInfo, mapKey+"|"+"加班规则为null|kqOvertimeRulesDetailEntity");
        continue;
      }
      int overtimeEnable = kqOvertimeRulesDetailEntity.getOvertimeEnable();
      if(overtimeEnable != 1){
        String overtimeEnableInfo = "overtimeEnable:"+overtimeEnable;
        logOvertimeMap(overtimeLogMap, overtimeEnableInfo, mapKey+"|"+"未开启加班规则|overtimeEnable");
        continue;
      }

      if(kqOvertimeCardBean != null){

        int[] initArrays = kqTimesArrayComInfo.getInitArr();
        List<Map<String, String>> hasOverTime4SignList = Lists.newArrayList();
        getHasOverTimeData(resourceid,realSplitDate,hasOverTime4SignList);
        Map<String,String> signinoffMap = buildOvertimeCard(kqOvertimeCardBean, resourceid, realSplitDate, kqTimesArrayComInfo, restTimeMap, changeType_key,initArrays,hasOverTime4SignList,
            overRulesDetailMap,true,overtimeLogMap);
        logOvertimeMap(overtimeLogMap, signinoffMap, mapKey+"|"+"获取上下班打卡数据|signinoffMap");

        String signinTime = Util.null2String(signinoffMap.get("signinTime"));
        String signoutTime = Util.null2String(signinoffMap.get("signoutTime"));
        String signinDate = Util.null2String(signinoffMap.get("signinDate"));
        String signoutDate = Util.null2String(signinoffMap.get("signoutDate"));
        int signinTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signinTime);
        int signoutTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signoutTime);

        if(signinTimeIndex < signoutTimeIndex){
          //先覆盖打卡 打卡区间都是1
          int over_count = kqTimesArrayComInfo.getCnt(initArrays, signinTimeIndex, signoutTimeIndex, 1);
          String overCountLogInfo = "signinTimeIndex:"+signinTimeIndex+":signoutTimeIndex:"+signoutTimeIndex+":over_count:"+over_count;
          logOvertimeMap(overtimeLogMap, overCountLogInfo, mapKey+"|"+"打卡区间，得到打卡时长|over_count");
          if(over_count > 0){
            int restTimeType = 1;
            String kqOvertimeRulesDetailEntityLogInfo = kqOvertimeRulesDetailEntity==null ? "" :JSON.toJSONString(kqOvertimeRulesDetailEntity);
            logOvertimeMap(overtimeLogMap, kqOvertimeRulesDetailEntityLogInfo, mapKey+"|具体这个人这一天对应的加班规则|KQOvertimeRulesDetailEntity");
            int minimumLen = -1;
            if(kqOvertimeRulesDetailEntity != null){
              minimumLen = kqOvertimeRulesDetailEntity.getMinimumLen();
              if(over_count < minimumLen){
                String minInfo = "over_count:"+over_count+":minimumLen:"+minimumLen;
                logOvertimeMap(overtimeLogMap, minInfo, mapKey+"|打卡时长小于最小加班时长|over_count<minimumUnit");
                continue;
              }
            }
            if(splitBeanMaps.containsKey(mapKey)) {
              List<SplitBean> splitBeans = splitBeanMaps.get(mapKey);
              String flowinfo = "";
              if(splitBeans != null && !splitBeans.isEmpty()){
                flowinfo = JSON.toJSONString(splitBeans, SerializerFeature.DisableCheckSpecialChar,SerializerFeature.DisableCircularReferenceDetect);
              }
              eventMap.put(mapKey+"|"+"加班流程数据|flowinfo", flowinfo);

              if(splitBeans == null || splitBeans.isEmpty()){
                return;
              }

              for (int m = 0; m < splitBeans.size(); m++) {
                SplitBean splitBean = splitBeans.get(m);
                String dataid = splitBean.getDataId();
                String detailid = splitBean.getDetailId();
                String flow_fromdate = splitBean.getFromDate();
                String flow_fromtime = splitBean.getFromTime();
                String flow_todate = splitBean.getToDate();
                String flow_totime = splitBean.getToTime();
                String fromdatedb = splitBean.getFromdatedb();
                String fromtimedb = splitBean.getFromtimedb();
                String todatedb = splitBean.getTodatedb();
                String totimedb = splitBean.getTotimedb();
                String requestid = splitBean.getRequestId();
                double d_mins = splitBean.getD_Mins();
                if(d_mins <= 0){
                  continue;
                }
                String flow_key =  mapKey+"|"+"flow_fromdate|"+flow_fromdate+"|flow_todate|"+flow_todate
                    +"|flow_fromtime|"+flow_fromtime+"|flow_totime|"+flow_totime;
                //如果打卡数据有了，再拿流程数据去覆盖，得到有效的打卡区间，这个区间肯定已经是去除了上下班时间和休息时间还有重复打卡的部分
                List<Integer> cross_time_list = Lists.newArrayList();
//              cross_time_list里存的是排除了工作时间的打卡段，找到1表示找到打卡开始的点了，找到-2表示找到打卡结束的点了
                get_cross_time_list(cross_time_list,initArrays,signinTimeIndex,signoutTimeIndex,1,-2);
                logOvertimeMap(overtimeLogMap, cross_time_list, flow_key+"|cross_time_list");
                int[] initArrays_flow = Arrays.copyOfRange(initArrays,0,initArrays.length);
                if(flow_fromdate.compareTo(realSplitDate) > 0){
                  flow_fromtime = kqTimesArrayComInfo.turn24to48Time(flow_fromtime);
                }
                if(flow_todate.compareTo(realSplitDate) > 0){
                  flow_totime = kqTimesArrayComInfo.turn24to48Time(flow_totime);
                }
                int flow_fromIndex = kqTimesArrayComInfo.getArrayindexByTimes(flow_fromtime);
                int flow_toIndex = kqTimesArrayComInfo.getArrayindexByTimes(flow_totime);
                //在已经打卡的区间1上覆盖2，那么在有效打卡范围内是2的就是交集的部分
                Arrays.fill(initArrays_flow, flow_fromIndex, flow_toIndex,2);
//                本来下面的这个地方是处理重复流程的问题，比如提了两个流程19-20,19-21，那么这俩流程相互交叉的话也只能生成2个小时，但是后来讨论
//                    流程直接的重复校验就通过开那个校验规则了，每一个流程都是和打卡的比较，所以一个是1一个是2.总共3
//                List<Map<String,String>> hasOverTimeList = Lists.newArrayList();
//                getHasOverTimeData(resourceid,realSplitDate,hasOverTimeList);
//                logOvertimeMap(overtimeLogMap, hasOverTimeList, flow_key+"|是否已经生成过加班，返回已经生成过加班的区间|hasOverTimeList");
//                if(hasOverTimeList != null && !hasOverTimeList.isEmpty()){
//                  for(int p = 0 ; p < hasOverTimeList.size(); p++){
//                    Map<String,String> hasOverTimeMap = hasOverTimeList.get(p);
//
//                    String fromdate_flow = Util.null2String(hasOverTimeMap.get("fromdate_flow"));
//                    String fromtime_flow = Util.null2String(hasOverTimeMap.get("fromtime_flow"));
//                    String todate_flow = Util.null2String(hasOverTimeMap.get("todate_flow"));
//                    String totime_flow = Util.null2String(hasOverTimeMap.get("totime_flow"));
//                    int has_flow_fromIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromtime_flow);
//                    int has_totime_flow = kqTimesArrayComInfo.getArrayindexByTimes(totime_flow);
//                    //把重复的流程给去掉，覆盖成-1
//                    Arrays.fill(initArrays_flow, has_flow_fromIndex, has_totime_flow,-2);
//                  }
//                }

                int across_mins = 0;
                for(int i = 0 ; i < cross_time_list.size() ;) {
                  int cross_fromtime_index = cross_time_list.get(i);
                  int cross_totime_index = cross_time_list.get(i + 1);
                  //前面打卡区间段已经都被流程给覆盖了，所以取获取打卡区间段内有多少流程的标识2，就是交叉部分了
                  int flow_count = kqTimesArrayComInfo.getCnt(initArrays_flow, cross_fromtime_index, cross_totime_index, 2);
                  logOvertimeMap(overtimeLogMap, flow_count, flow_key+"|取打卡和流程相交的时长|flow_count");

                  if(flow_count > 0){
                    List<Integer> flow_cross_time_list = Lists.newArrayList();
                    // 找到2表示找到流程开始的点了，找到1表示找到流程结束的点了
                    get_cross_time_list(flow_cross_time_list,initArrays_flow,cross_fromtime_index,cross_totime_index,2,1);

                    logOvertimeMap(overtimeLogMap, flow_cross_time_list, flow_key+"|取打卡和流程相交的区间|flow_cross_time_list");
                    for(int j = 0 ; j < flow_cross_time_list.size() ;){
                      int flow_cross_fromtime_index = flow_cross_time_list.get(j);
                      int flow_cross_totime_index = flow_cross_time_list.get(j+1);

                      int mins = flow_cross_totime_index-flow_cross_fromtime_index;
                      if(mins <= 0){
                        String crossInfo = "flow_cross_fromtime_index:"+flow_cross_fromtime_index+":flow_cross_totime_index:"+flow_cross_totime_index+":mins:"+mins;
                        logOvertimeMap(overtimeLogMap, crossInfo, flow_key+"|打卡时长小于最小加班时长|crossInfo");
                        continue;
                      }
                      across_mins += mins;

                      String flow_cross_key = "加班计算区间|"+kqTimesArrayComInfo.getTimesByArrayindex(flow_cross_fromtime_index)+"-"+kqTimesArrayComInfo.getTimesByArrayindex(flow_cross_totime_index);
                      logOvertimeMap(overtimeLogMap, mins, flow_cross_key+"|原始加班区间生成的加班时长|mins");

                      String cross_fromtime = kqTimesArrayComInfo.getTimesByArrayindex(flow_cross_fromtime_index);
                      String cross_totime = kqTimesArrayComInfo.getTimesByArrayindex(flow_cross_totime_index);
                      String cross_fromdate = realSplitDate;
                      String cross_todate = realSplitDate;

                      boolean needSplitByTime = false;
                      if(needSplitByTime){
                        //        按照加班时长转调休的 时长设置 这个逻辑如果后面要开启来可以直接用的
                        List<String> timepointList = null;
                        if(kqOvertimeRulesDetailEntity != null){
                          timepointList = get_timepointList(kqOvertimeRulesDetailEntity);
                          logOvertimeMap(overtimeLogMap, timepointList, flow_cross_key+"|如果要生成调休且是根据时间区间来转调休，返回对应的时间区间|timepointList");
                        }

                        List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = Lists.newArrayList();
                        if(timepointList != null && !timepointList.isEmpty()){
                          int[] time_initArrays = kqTimesArrayComInfo.getInitArr();
                          for(int t = flow_cross_fromtime_index;t < flow_cross_totime_index; t++){
                            time_initArrays[t] = initArrays_flow[t];
                          }
                          get_overtimeBalanceTimeBeans(timepointList,overtimeBalanceTimeBeans,kqTimesArrayComInfo,time_initArrays,flow_cross_totime_index,flow_cross_fromtime_index,2);
                        }
                        String overtimeBalanceTimeBeansLogInfo = "";
                        if(overtimeBalanceTimeBeans == null || overtimeBalanceTimeBeans.isEmpty()){
                        }else{
                          overtimeBalanceTimeBeansLogInfo = JSON.toJSONString(overtimeBalanceTimeBeans);
                        }
                        logOvertimeMap(overtimeLogMap, overtimeBalanceTimeBeansLogInfo, flow_cross_key+"|如果要生成调休且是根据时间区间来转调休，返回对应的时间区间对应的时长|overtimeBalanceTimeBeans");

                        if(overtimeBalanceTimeBeans != null && !overtimeBalanceTimeBeans.isEmpty()){
                          String bean_cross_fromtime = cross_fromtime;
                          String bean_cross_totime = cross_totime;
                          for(int timeIndex = 0 ; timeIndex < overtimeBalanceTimeBeans.size() ;timeIndex++){
                            OvertimeBalanceTimeBean overtimeBalanceTimeBean = overtimeBalanceTimeBeans.get(timeIndex);
                            String timePointStart = overtimeBalanceTimeBean.getTimepoint_start();
                            String timePointEnd = overtimeBalanceTimeBean.getTimepoint_end();
                            boolean isNeedTX = overtimeBalanceTimeBean.isNeedTX();
                            int timePointStart_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointStart);
                            int timePointEnd_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointEnd);
                            if(timePointStart_index > flow_cross_fromtime_index){
                              bean_cross_fromtime = kqTimesArrayComInfo.getTimesByArrayindex(timePointStart_index);
                            }else{
                              bean_cross_fromtime = cross_fromtime;
                            }
                            if(timePointEnd_index < flow_cross_totime_index){
                              bean_cross_totime = kqTimesArrayComInfo.getTimesByArrayindex(timePointEnd_index);
                            }else{
                              bean_cross_totime = cross_totime;
                            }
                            int timepoint_mins = overtimeBalanceTimeBean.getTimepoint_mins();
                            if(timepoint_mins == 0){
                              continue;
                            }
                            mins = timepoint_mins;
                          }
                        }
                      }else{
                      }
                      j =j + 2;
                    } }else{

                  }
                  i = i +2;
                }

                if(kqOvertimeRulesDetailEntity != null){
                  //我这个方法是针对每次生成的加班数据做排除休息时长的处理
                  restTimeType = kqOvertimeRulesDetailEntity.getRestTimeType();
                  if(restTimeType == 2){
                    across_mins = new KQOverTimeFlowBiz().handle_restlength(across_mins,restTimeMap,changeType_key);
                  }
                }
                int card_mins = over_count;
                double double_mins = getD_MinsByUnit((1.0*across_mins));
                across_mins = (int)double_mins;
                if(across_mins <= 0){
                  logOvertimeMap(overtimeLogMap, across_mins, flow_key+"|经过单位换算之后时长为0|across_mins");
                  continue;
                }
                if(across_mins < minimumLen){
                  String minInfo = "across_mins:"+across_mins+":minimumLen:"+minimumLen;
                  logOvertimeMap(overtimeLogMap, minInfo, flow_key+"|打卡时长小于最小加班时长|over_count<minimumUnit");
                  continue;

                }

                String overtime_uuid = UUID.randomUUID().toString();
                String tiaoxiuId = "";
                String flow_dataid = "";
                int computingMode = 4;

                int unit = KQOvertimeRulesBiz.getMinimumUnit();
                String workingHours = "";
                String overtime_type = splitBean.getOvertime_type();
                Map<String,Object> otherParam = Maps.newHashMap();
                otherParam.put("overtime_type", overtime_type);
                int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
                if(2 == paidLeaveEnableType){
                  logOvertimeMap(overtimeLogMap, overtime_type, flow_key+"|关联调休与否来自于流程选择,加班类型下拉框值|overtime_type");
                }

                int paidLeaveEnable = getPaidLeaveEnable(kqOvertimeRulesDetailEntity, overtime_type);
                //目前不处理按照时间段生成调休
                boolean needSplitByTime = false;//getNeedSplitByTime(kqOvertimeRulesDetailEntity, paidLeaveEnable);
                if(needSplitByTime){

                }else{

                  //这个点
                  String startDate = Util.null2String(new BaseBean().getPropValue("qc2614039", "fromtime"));//建模配置的时间点1
                  int startDateIdx = kqTimesArrayComInfo.getArrayindexByTimes(startDate);
                  String endDate = Util.null2String(new BaseBean().getPropValue("qc2614039", "totime"));//建模配置的时间点2
                  int endDateIdx = kqTimesArrayComInfo.getArrayindexByTimes(endDate);
                  int extraMins = Util.getIntValue(Util.null2String(new BaseBean().getPropValue("qc2614039", "extraMins")),0);
                  //取交集
                  int intersection_from = Math.max(signinTimeIndex,flow_fromIndex);
                  int intersection_to = Math.min(signoutTimeIndex,flow_toIndex);
                  //获取打卡时间端 获取流程时间段
                  kqLog.info("signinTimeIndex:"+signinTimeIndex+","+"signoutTimeIndex:"+signoutTimeIndex+","+"flow_fromIndex:"+flow_fromIndex+","+"flow_toIndex:"+flow_toIndex+",MINS:"+extraMins);
                  kqLog.info(">>>>>交集："+intersection_from+"---"+intersection_to);
                  //判断是否包含配置时间段
                  if(intersection_from<=startDateIdx && intersection_to>=endDateIdx && changeType != 2){
                    double double_mins4temp = getD_MinsByUnit((1.0*extraMins));
                    across_mins += (int)double_mins4temp;
                  }

                  new BaseBean().writeLog("==zj==(across_mins1)" +resourceid+"  |  "  + splitDate + "  |  " + intersection_from + " | " + intersection_to +  "  |  " + across_mins);
                  //特殊加班流程
                  String specialWorkFlowid = new BaseBean().getPropValue("qc2658035","specialWorkFlowid");
                  //获取该加班流程的workflowid
                  String workFlowId = overTimeUtil.getWorkFlowId(requestid);


                  otherParam.put("overtimeLogMap", overtimeLogMap);
                  logOvertimeMap(overtimeLogMap, across_mins, flow_key+"|最终生成的加班分钟数|overtime_mins");

                  //如果是特殊加班流程还有值班时长转调休
                  int dutyOverTimes = 0;
                  String dutytiaoxiuId = "";
                  new BaseBean().writeLog("==zj==(是否是特殊流程)" + specialWorkFlowid.equals(workFlowId) + " | " + resourceid + " | " +splitDate);
                  if (specialWorkFlowid.equals(workFlowId)){
                    dutyOverTimes = overTimeUtil.getdutyOverTimes(intersection_from,intersection_to,realSplitDate,resourceid);
                  }

                    tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceid,realSplitDate,across_mins+"","0",workingHours,requestid,"1",realSplitDate,otherParam);
                  if(Util.getIntValue(tiaoxiuId) > 0){
                    kqLog.info("doComputingMode4 生成调休成功，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate);
                  }else{
                    kqLog.info("doComputingMode4 生成调休失败，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate);
                  }
                  logOvertimeMap(overtimeLogMap, tiaoxiuId, flow_key+"|最终生成的调休id|tiaoxiuId");

                  String flow_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                      + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,card_mins,ori_belongdate,flow_dataid)"+
                      " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                  signinTime = kqTimesArrayComInfo.turn48to24Time(signinTime);
                  signoutTime = kqTimesArrayComInfo.turn48to24Time(signoutTime);
                  if(signinTime.length() == 5){
                    signinTime = signinTime+":00";
                  }
                  if(signoutTime.length() == 5){
                    signoutTime = signoutTime+":00";
                  }

                  //默认走标准
                    boolean isUp = rs.executeUpdate(flow_overtime_sql, requestid,resourceid,signinDate,signinTime,signoutDate,signoutTime,across_mins,"",realSplitDate,
                            "",unit,changeType,paidLeaveEnable,computingMode,tiaoxiuId,overtime_uuid,fromdatedb,fromtimedb,todatedb,totimedb,d_mins,card_mins,splitDate,flow_dataid);


                  //如果是特殊流程，且有值班时长再增加一个值班时长。
                  new BaseBean().writeLog("==zj==(dutyOverTimes)" + resourceid + " | " + splitDate + " | " + dutyOverTimes);
                  if (dutyOverTimes > 0 && specialWorkFlowid.equals(workFlowId) ){
                    String duty_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                            + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,card_mins,ori_belongdate,flow_dataid,isdutywork)"+
                            " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                     rs.executeUpdate(duty_overtime_sql, requestid,resourceid,signinDate,signinTime,signoutDate,signoutTime,dutyOverTimes,"",realSplitDate,
                            "",unit,changeType,paidLeaveEnable,computingMode,dutytiaoxiuId,overtime_uuid,fromdatedb,fromtimedb,todatedb,totimedb,d_mins,card_mins,splitDate,flow_dataid,1);
                  }

                  String overtimeid = get_overtime_uuid(overtime_uuid);
                  kqEventLogBiz.updateOvertimeId(uuid, overtimeid);
                }
              }
            }else{
              //有打卡没有流程
              logOvertimeMap(overtimeLogMap, mapKey, mapKey+"|"+"加班流程为空");
            }
          }else{
            logOvertimeMap(overtimeLogMap, overCountLogInfo, mapKey+"|"+"打卡数据时长为0");
          }
        }else{
          String overCountLogInfo = "signinTimeIndex:"+signinTimeIndex+":signoutTimeIndex:"+signoutTimeIndex;
          logOvertimeMap(overtimeLogMap, overCountLogInfo, mapKey+"|"+"打卡数据异常");
        }
      }else{
        logOvertimeMap(overtimeLogMap, "打卡数据KQOvertimeCardBean为null", mapKey+"|"+"打卡和上下班数据|KQOvertimeCardBean");
      }
    }
    kqEventLogBiz.logDetailOvertimeMap(resourceid,overtimeLogMap,uuid);
    kqEventLogBiz.logDetailEvent(resourceid,eventMap,uuid,"doComputingMode4|对应的加班流程数据|key|"+key);

  }

  /**
   * 得到有效的打卡区间，这个区间肯定已经是去除了上下班时间和休息时间还有重复打卡的部分
   * @param cross_time_list
   * @param initArrays
   * @param fromIndex
   * @param toIndex
   * @param cross_from_index
   * @param cross_to_index
   */
  public void get_cross_time_list(List<Integer> cross_time_list, int[] initArrays,
      int fromIndex, int toIndex,int cross_from_index,int cross_to_index) {
    for(int i = fromIndex ; i < toIndex+1 ; i++){
      if(cross_time_list.isEmpty()){
        if(initArrays[i] == cross_from_index){
          cross_time_list.add(i);
        }
      }else{
        if(cross_time_list.size() % 2 != 0){
          if(initArrays[i] == cross_to_index){
            cross_time_list.add(i);
          }
        }else{
          if(initArrays[i] == cross_from_index){
            cross_time_list.add(i);
          }
        }
      }
    }
    if(cross_time_list.size() % 2 != 0){
      cross_time_list.add(toIndex);
    }
  }

  /**
   * 处理加班方式是 无需审批，根据打卡时间计算加班时长
   * @param resourceid
   * @param splitDate
   * @param dateFormatter
   * @param changeTypeMap
   * @param overRulesDetailMap
   * @param restTimeMap
   * @param computingModeMap
   * @param kqTimesArrayComInfo
   * @param uuid
   */
  private void doComputingMode3(String resourceid, String splitDate,
      DateTimeFormatter dateFormatter, Map<String, Integer> changeTypeMap,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      Map<String, List<String[]>> restTimeMap, Map<String, Integer> computingModeMap,
      KQTimesArrayComInfo kqTimesArrayComInfo, String main_uuid) throws Exception{
    String key = resourceid+"_"+splitDate;
    //加班日志记录类
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(resourceid, splitDate);
    Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();
    workTimeEntityLogMap.put("resourceid", resourceid);
    workTimeEntityLogMap.put("splitDate", splitDate);
    workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);
    KQOvertimeLogBiz kqEventLogBiz = new KQOvertimeLogBiz();
    String uuid = kqEventLogBiz.logDetailWorkTimeEntity(resourceid,workTimeEntityLogMap,main_uuid,"doComputingMode3|加班计算,无需审批,根据打卡时间计算加班时长|key|"+key);
    Map<String,Object> overtimeLogMap = Maps.newLinkedHashMap();

    Map<String,Object> eventMap = Maps.newHashMap();
    Map<String, KQOvertimeCardBean> lsCheckInfoMaps = Maps.newLinkedHashMap();
    //获取加班打卡数据
    getOverTimeCardDataMap(resourceid, splitDate, splitDate, dateFormatter,kqTimesArrayComInfo,overRulesDetailMap,changeTypeMap,lsCheckInfoMaps,
        eventMap);
    kqEventLogBiz.logDetailEvent(resourceid,eventMap,uuid,"doComputingMode3|对应的打卡数据|key|"+key);

    if(lsCheckInfoMaps.isEmpty()){
      logOvertimeMap(overtimeLogMap, "没有打卡数据", "打卡和上下班数据|KQOvertimeCardBean");
      kqEventLogBiz.logDetailOvertimeMap(resourceid,overtimeLogMap,uuid);
      return;
    }

    Iterator<Entry<String, KQOvertimeCardBean>> iterator = lsCheckInfoMaps.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, KQOvertimeCardBean> next = iterator.next();
      String mapKey = next.getKey();
      KQOvertimeCardBean kqOvertimeCardBean = next.getValue();
      String[] mapKeys = mapKey.split("_");
      if (mapKeys.length != 2) {
        continue;
      }
      String realSplitDate = mapKeys[1];
      String change_key = realSplitDate+"_"+resourceid;
      int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
      String changeType_key = realSplitDate+"_"+changeType;
      String changetypeName = 1==changeType ? "节假日" : (2 == changeType ? "工作日" : (3 == changeType ? "休息日" : "异常"));
      String changetypeLogInfo = change_key+"|changeType|"+changeType+"|"+changetypeName;
      logOvertimeMap(overtimeLogMap, changetypeLogInfo, "加班日期属性|changetypeLogInfo");

      clearOvertimeTX(resourceid, realSplitDate,overtimeLogMap, splitDate);

      logOvertimeMap(overtimeLogMap, kqOvertimeCardBean, "打卡和上下班数据|KQOvertimeCardBean");

      KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
      if(kqOvertimeRulesDetailEntity == null){
        String overRuleInfo = "changeType_key:"+changeType_key+":kqOvertimeRulesDetailEntity:"+kqOvertimeRulesDetailEntity;
        logOvertimeMap(overtimeLogMap, overRuleInfo, mapKey+"|"+"加班规则为null|kqOvertimeRulesDetailEntity");
        continue;
      }
      int overtimeEnable = kqOvertimeRulesDetailEntity.getOvertimeEnable();
      if(overtimeEnable != 1){
        String overtimeEnableInfo = "overtimeEnable:"+overtimeEnable;
        logOvertimeMap(overtimeLogMap, overtimeEnableInfo, mapKey+"|"+"未开启加班规则|overtimeEnable");
        continue;
      }
      if(kqOvertimeCardBean != null){

        int[] initArrays = kqTimesArrayComInfo.getInitArr();
        Map<String,String> signinoffMap = buildOvertimeCard(kqOvertimeCardBean, resourceid, realSplitDate, kqTimesArrayComInfo, restTimeMap, changeType_key,initArrays,Lists.newArrayList(),overRulesDetailMap,
            true, overtimeLogMap);
        logOvertimeMap(overtimeLogMap, signinoffMap, "获取上下班打卡数据|signinoffMap");

        String signinTime = Util.null2String(signinoffMap.get("signinTime"));
        String signoutTime = Util.null2String(signinoffMap.get("signoutTime"));
        String signinDate = Util.null2String(signinoffMap.get("signinDate"));
        String signoutDate = Util.null2String(signinoffMap.get("signoutDate"));
        int signinTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signinTime);
        int signoutTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signoutTime);
        String flow_cross_key = mapKey+"|"+"加班计算区间|"+signinTime+"-"+signoutTime;

        if(signinTimeIndex < signoutTimeIndex){
          int over_count = kqTimesArrayComInfo.getCnt(initArrays, signinTimeIndex, signoutTimeIndex, 1);
          String countLogInfo = "signinTimeIndex:"+signinTimeIndex+":signoutTimeIndex:"+signoutTimeIndex+":over_count:"+over_count;
          logOvertimeMap(overtimeLogMap, countLogInfo, mapKey+"|"+"打卡区间，得到打卡时长|over_count");
          if(over_count > 0){
            //表示加班打卡是存在的
            int restTimeType = 1;
            if(kqOvertimeRulesDetailEntity != null){
              int minimumLen = kqOvertimeRulesDetailEntity.getMinimumLen();
              if(over_count < minimumLen){
                continue;
              }
              logOvertimeMap(overtimeLogMap, JSON.toJSONString(kqOvertimeRulesDetailEntity), mapKey+"|"+"加班规则|KQOvertimeRulesDetailEntity");
              //我这个方法是针对每次生成的加班数据做排除休息时长的处理
              restTimeType = kqOvertimeRulesDetailEntity.getRestTimeType();
              if(restTimeType == 2){
                over_count = new KQOverTimeFlowBiz().handle_restlength(over_count,restTimeMap,changeType_key);
              }
            }
            if(over_count <= 0){
              logOvertimeMap(overtimeLogMap, "没有打卡数据", mapKey+"|"+"打卡时长|over_count");
              continue;
            }
            logOvertimeMap(overtimeLogMap, over_count, mapKey+"|"+"经历过休息时间之后的加班时长|over_rest_count");

            int mins = over_count;
            double double_mins = getD_MinsByUnit((1.0*mins));
            mins = (int)double_mins;

            RecordSet rs = new RecordSet();
            String overtime_uuid = UUID.randomUUID().toString();
            String tiaoxiuId = "";
            String workingHours = "";
            int computingMode = 3;

            Map<String,Object> otherParam = Maps.newHashMap();
            int unit = KQOvertimeRulesBiz.getMinimumUnit();

            int paidLeaveEnable = kqOvertimeRulesDetailEntity.getPaidLeaveEnable();
            int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
            boolean needSplitByTime = false;//getNeedSplitByTime(kqOvertimeRulesDetailEntity, paidLeaveEnable);
            if(needSplitByTime){
              //        按照加班时长转调休的 时长设置
              List<String> timepointList = null;
              if(kqOvertimeRulesDetailEntity != null){
                timepointList = get_timepointList(kqOvertimeRulesDetailEntity);
                logOvertimeMap(overtimeLogMap, timepointList, flow_cross_key+"|如果要生成调休且是根据时间区间来转调休，返回对应的时间区间|timepointList");
              }

              List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = Lists.newArrayList();
              if(timepointList != null && !timepointList.isEmpty()){
                int[] time_initArrays = kqTimesArrayComInfo.getInitArr();
                for(int t = signinTimeIndex;t < signoutTimeIndex; t++){
                  time_initArrays[t] = initArrays[t];
                }
                get_overtimeBalanceTimeBeans(timepointList,overtimeBalanceTimeBeans,kqTimesArrayComInfo,time_initArrays,signoutTimeIndex,signinTimeIndex,1);
              }
              String overtimeBalanceTimeBeansLogInfo = "";
              if(overtimeBalanceTimeBeans == null || overtimeBalanceTimeBeans.isEmpty()){
              }else{
                overtimeBalanceTimeBeansLogInfo = JSON.toJSONString(overtimeBalanceTimeBeans);
              }
              logOvertimeMap(overtimeLogMap, overtimeBalanceTimeBeansLogInfo, flow_cross_key+"|如果要生成调休且是根据时间区间来转调休，返回对应的时间区间对应的时长|overtimeBalanceTimeBeans");

              if(overtimeBalanceTimeBeans != null && !overtimeBalanceTimeBeans.isEmpty()){
                String bean_cross_fromtime = signinTime;
                String bean_cross_totime = signoutTime;
                for(int timeIndex = 0 ; timeIndex < overtimeBalanceTimeBeans.size() ;timeIndex++) {
                  OvertimeBalanceTimeBean overtimeBalanceTimeBean = overtimeBalanceTimeBeans.get(timeIndex);
                  String timePointStart = overtimeBalanceTimeBean.getTimepoint_start();
                  String timePointEnd = overtimeBalanceTimeBean.getTimepoint_end();
                  int timePointStart_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointStart);
                  int timePointEnd_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointEnd);
                  if(timePointStart_index > signinTimeIndex){
                    bean_cross_fromtime = kqTimesArrayComInfo.getTimesByArrayindex(timePointStart_index);
                  }else{
                    bean_cross_fromtime = signinTime;
                  }
                  if(timePointEnd_index < signoutTimeIndex){
                    bean_cross_totime = kqTimesArrayComInfo.getTimesByArrayindex(timePointEnd_index);
                  }else{
                    bean_cross_totime = signoutTime;
                  }
                  int timepoint_mins = overtimeBalanceTimeBean.getTimepoint_mins();
                  if(timepoint_mins == 0){
                    continue;
                  }
                  mins = timepoint_mins;
                  otherParam.put("OvertimeBalanceTimeBean", overtimeBalanceTimeBean);
                  otherParam.put("overtimeLogMap", overtimeLogMap);
                  String timepoint_key = flow_cross_key+"|调休按照分段计算加班时间("+signinTime+"-"+signoutTime+")";
                  otherParam.put("timepoint_key", timepoint_key);
                  logOvertimeMap(overtimeLogMap, mins, timepoint_key+"最终生成的加班分钟数|overtime_mins");

                  tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceid,realSplitDate,mins+"","0",workingHours,"","",realSplitDate,otherParam);
                  if(Util.getIntValue(tiaoxiuId) > 0){
                    kqLog.info("doComputingMode3 生成调休成功，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate);
                  }else{
                    kqLog.info("doComputingMode3 生成调休失败，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate);
                  }
                  overtime_uuid = UUID.randomUUID().toString();
                  String flow_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                      + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,ori_belongdate)"+
                      " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                  signinTime = kqTimesArrayComInfo.turn48to24Time(signinTime);
                  signoutTime = kqTimesArrayComInfo.turn48to24Time(signoutTime);
                  boolean isUp = rs.executeUpdate(flow_overtime_sql, "",resourceid,signinDate,signinTime,signoutDate,signoutTime,mins,"",realSplitDate,
                      "",unit,changeType,paidLeaveEnable,computingMode,tiaoxiuId,overtime_uuid,"","","","",0,splitDate);

                  String overtimeid = get_overtime_uuid(overtime_uuid);
                  kqEventLogBiz.updateOvertimeId(uuid, overtimeid);
                }
              }
            }else{

              logOvertimeMap(overtimeLogMap, mins, flow_cross_key+"|最终生成的加班分钟数|overtime_mins");
              tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceid,realSplitDate,mins+"","0",workingHours,"","",realSplitDate,otherParam);
              if(Util.getIntValue(tiaoxiuId) > 0){
                kqLog.info("doComputingMode3 生成调休成功，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate);
              }else{
                kqLog.info("doComputingMode3 生成调休失败，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate);
              }

              String flow_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                  + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,ori_belongdate)"+
                  " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
              signinTime = kqTimesArrayComInfo.turn48to24Time(signinTime);
              signoutTime = kqTimesArrayComInfo.turn48to24Time(signoutTime);
              boolean isUp = rs.executeUpdate(flow_overtime_sql, "",resourceid,signinDate,signinTime,signoutDate,signoutTime,mins,"",realSplitDate,
                  "",unit,changeType,paidLeaveEnable,computingMode,tiaoxiuId,overtime_uuid,"","","","",0,splitDate);

              String overtimeid = get_overtime_uuid(overtime_uuid);
              kqEventLogBiz.updateOvertimeId(uuid, overtimeid);
            }

          }
        }else{
          logOvertimeMap(overtimeLogMap, "打卡区间不正确|"+flow_cross_key, mapKey+"|"+"打卡区间，得到打卡时长|over_count");
        }
      }else{
        logOvertimeMap(overtimeLogMap, "打卡数据KQOvertimeCardBean为null", mapKey+"|"+"打卡和上下班数据|KQOvertimeCardBean");
      }
    }
    kqEventLogBiz.logDetailOvertimeMap(resourceid,overtimeLogMap,uuid);
    kqEventLogBiz.logDetailEvent(resourceid,eventMap,uuid,"doComputingMode3|对应的加班流程数据|key|"+key);


  }

  /**
   * 获取生成的加班id
   * @param overtime_uuid
   * @return
   */
  public String get_overtime_uuid(String overtime_uuid) {
    RecordSet rs = new RecordSet();
    String sql = "select * from kq_flow_overtime where uuid='"+overtime_uuid+"' ";
    rs.executeQuery(sql);
    if(rs.next()){
      return rs.getString("id");
    }
    return "";
  }

  /**
   * 按加班的时间段设置转调休时长
   * @param kqOvertimeRulesDetailEntity
   * @return
   */
  public List<String> get_timepointList(KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity) {
    List<String> timepointList = Lists.newArrayList();
    KQOvertimeRulesBiz kqOvertimeRulesBiz = new KQOvertimeRulesBiz();
    int ruleDetailid = kqOvertimeRulesDetailEntity.getId();
    int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
    if(1 == paidLeaveEnableType){
      int paidLeaveEnableDefaultType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableDefaultType();

      if(paidLeaveEnableDefaultType == 3){
        Map<String,List<String>> balanceTimethDetailMap = kqOvertimeRulesBiz.getBalanceTimeDetailMap(ruleDetailid);
        if(balanceTimethDetailMap != null && !balanceTimethDetailMap.isEmpty()){
          timepointList = balanceTimethDetailMap.get("timepointList");
        }
      }
    }else if(2 == paidLeaveEnableType){
      int paidLeaveEnableFlowType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableFlowType();

      if(paidLeaveEnableFlowType == 3){
        Map<String,List<String>> balanceTimethDetailMap = kqOvertimeRulesBiz.getBalanceTimeDetailMap(ruleDetailid);
        if(balanceTimethDetailMap != null && !balanceTimethDetailMap.isEmpty()){
          timepointList = balanceTimethDetailMap.get("timepointList");
        }
      }
    }
    return timepointList;
  }

  /**
   * 加班补偿规则里，按照加班时段补偿方式需要获取每一个时间区间内的加班时长
   * @param timepointList
   * @param overtimeBalanceTimeBeans
   * @param kqTimesArrayComInfo
   * @param initArrays
   * @param toTime_index
   * @param fromTime_index
   * @param arrayIndexValue
   */
  public void get_overtimeBalanceTimeBeans(List<String> timepointList,
      List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans,
      KQTimesArrayComInfo kqTimesArrayComInfo, int[] initArrays,
      int toTime_index,int fromTime_index,int arrayIndexValue) {
    //如果 [按加班的时间段设置转调休时长] 是这种方式，还需要根据时间点来判断时间区间内的加班时长，艹
    for(int k = 0 ; k < timepointList.size() ; k++){
      OvertimeBalanceTimeBean overtimeBalanceTimeBean = new OvertimeBalanceTimeBean();
      String start_pointtime = timepointList.get(k);
      int start_pointtime_index = kqTimesArrayComInfo.getArrayindexByTimes(start_pointtime);
      if(k == 0){
        if(start_pointtime_index > fromTime_index){
          int timepoint_curMins = kqTimesArrayComInfo.getCnt(initArrays, fromTime_index,start_pointtime_index,arrayIndexValue);
          OvertimeBalanceTimeBean ori_overtimeBalanceTimeBean = new OvertimeBalanceTimeBean();
          ori_overtimeBalanceTimeBean.setTimepoint(kqTimesArrayComInfo.getTimesByArrayindex(start_pointtime_index));
          ori_overtimeBalanceTimeBean.setTimepoint_start(kqTimesArrayComInfo.getTimesByArrayindex(fromTime_index));
          ori_overtimeBalanceTimeBean.setTimepoint_end(kqTimesArrayComInfo.getTimesByArrayindex(start_pointtime_index));
          ori_overtimeBalanceTimeBean.setTimepoint_mins(timepoint_curMins);
          ori_overtimeBalanceTimeBean.setNeedTX(false);
          overtimeBalanceTimeBeans.add(ori_overtimeBalanceTimeBean);
        }
      }
      if(start_pointtime_index > toTime_index){
        continue;
      }
      overtimeBalanceTimeBean.setList_index(k);
      int start_index = -1;
      int end_index = -1;
      if(k == timepointList.size()-1){
        start_index = start_pointtime_index;
        end_index = toTime_index;
      }else{
        if(k+1 < timepointList.size()){
          String end_pointtime = timepointList.get(k+1);
          start_index = start_pointtime_index;
          end_index = kqTimesArrayComInfo.getArrayindexByTimes(end_pointtime);
        }
      }
      if(start_index < end_index){
        int timepoint_curMins = kqTimesArrayComInfo.getCnt(initArrays, start_index,end_index,arrayIndexValue);
        overtimeBalanceTimeBean.setTimepoint(kqTimesArrayComInfo.getTimesByArrayindex(start_index));
        overtimeBalanceTimeBean.setTimepoint_start(kqTimesArrayComInfo.getTimesByArrayindex(start_index));
        overtimeBalanceTimeBean.setTimepoint_end(kqTimesArrayComInfo.getTimesByArrayindex(end_index));
        overtimeBalanceTimeBean.setTimepoint_mins(timepoint_curMins);
        overtimeBalanceTimeBean.setNeedTX(true);
        overtimeBalanceTimeBeans.add(overtimeBalanceTimeBean);
      }
    }
  }

  /**
   * 处理加班方式是 需审批，以打卡为准，但是不能超过审批时长的加班时长
   * @param resourceid
   * @param splitDate
   * @param dateFormatter
   * @param changeTypeMap
   * @param overRulesDetailMap
   * @param restTimeMap
   * @param computingModeMap
   * @param kqTimesArrayComInfo
   * @param uuid
   */
  private void doComputingMode2(String resourceid, String splitDate,
      DateTimeFormatter dateFormatter, Map<String, Integer> changeTypeMap,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      Map<String, List<String[]>> restTimeMap, Map<String, Integer> computingModeMap,
      KQTimesArrayComInfo kqTimesArrayComInfo, String main_uuid) throws Exception{
    String key = resourceid+"_"+splitDate;
    //加班日志记录类
    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(resourceid, splitDate);
    Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();
    workTimeEntityLogMap.put("resourceid", resourceid);
    workTimeEntityLogMap.put("splitDate", splitDate);
    workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);
    KQOvertimeLogBiz kqEventLogBiz = new KQOvertimeLogBiz();
    String uuid = kqEventLogBiz.logDetailWorkTimeEntity(resourceid,workTimeEntityLogMap,main_uuid,"doComputingMode2|加班计算,需审批,以打卡为准,但是不能超过审批时长的加班时长|key|"+key);
    Map<String,Object> overtimeLogMap = Maps.newLinkedHashMap();

    Map<String,Object> eventMap = Maps.newHashMap();
    Map<String, KQOvertimeCardBean> lsCheckInfoMaps = Maps.newLinkedHashMap();
    //获取加班打卡数据
    getOverTimeCardDataMap(resourceid, splitDate, splitDate, dateFormatter,kqTimesArrayComInfo,overRulesDetailMap,changeTypeMap,lsCheckInfoMaps,
        eventMap);

    Map<String,List<SplitBean>> splitBeanMaps = Maps.newHashMap();
    //获取加班流程数据
    getOverTimeFlowData(resourceid,splitDate,splitDate,splitBeanMaps,dateFormatter);

    if(lsCheckInfoMaps.isEmpty()){
      logOvertimeMap(overtimeLogMap, "没有打卡数据", "打卡和上下班数据|KQOvertimeCardBean");
      kqEventLogBiz.logDetailOvertimeMap(resourceid,overtimeLogMap,uuid);
      kqEventLogBiz.logDetailEvent(resourceid,eventMap,uuid,"doComputingMode2|对应的加班流程数据|key|"+key);
      return;
    }

    Iterator<Entry<String, KQOvertimeCardBean>> iterator = lsCheckInfoMaps.entrySet().iterator();
    while (iterator.hasNext()){
      Entry<String, KQOvertimeCardBean> next = iterator.next();
      String mapKey = next.getKey();
      KQOvertimeCardBean kqOvertimeCardBean = next.getValue();
      String[] mapKeys = mapKey.split("_");
      if(mapKeys.length != 2){
        continue;
      }
      String realSplitDate = mapKeys[1];

      String change_key = realSplitDate+"_"+resourceid;
      int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
      String changeType_key = realSplitDate+"_"+changeType;
      String changetypeName = 1==changeType ? "节假日" : (2 == changeType ? "工作日" : (3 == changeType ? "休息日" : "异常"));
      String changetypeLogInfo = change_key+"|changeType|"+changeType+"|"+changetypeName;
      logOvertimeMap(overtimeLogMap, changetypeLogInfo, mapKey+"|"+"加班日期属性|changetypeLogInfo");

      clearOvertimeTX(resourceid, realSplitDate,overtimeLogMap, splitDate);
      logOvertimeMap(overtimeLogMap, kqOvertimeCardBean, mapKey+"|"+"打卡和上下班数据|KQOvertimeCardBean");

      KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
      if(kqOvertimeRulesDetailEntity == null){
        String overRuleInfo = "changeType_key:"+changeType_key+":kqOvertimeRulesDetailEntity:"+kqOvertimeRulesDetailEntity;
        logOvertimeMap(overtimeLogMap, overRuleInfo, mapKey+"|"+"加班规则为null|kqOvertimeRulesDetailEntity");
        continue;
      }
      int overtimeEnable = kqOvertimeRulesDetailEntity.getOvertimeEnable();
      if(overtimeEnable != 1){
        String overtimeEnableInfo = "overtimeEnable:"+overtimeEnable;
        logOvertimeMap(overtimeLogMap, overtimeEnableInfo, mapKey+"|"+"未开启加班规则|overtimeEnable");
        continue;
      }

      if(kqOvertimeCardBean != null){
        if(splitBeanMaps.containsKey(mapKey)) {
          List<SplitBean> splitBeans = splitBeanMaps.get(mapKey);
          String flowinfo = "";
          if(splitBeans != null && !splitBeans.isEmpty()){
            flowinfo = JSON.toJSONString(splitBeans, SerializerFeature.DisableCheckSpecialChar,SerializerFeature.DisableCircularReferenceDetect);
          }else{
            logOvertimeMap(overtimeLogMap, "打卡数据KQOvertimeCardBean为null", mapKey+"|"+"打卡和上下班数据|KQOvertimeCardBean");
            continue;
          }
          eventMap.put(mapKey+"|"+"加班流程数据|flowinfo", flowinfo);
          for (int m = 0; m < splitBeans.size(); m++) {
            int[] initArrays_flow = kqTimesArrayComInfo.getInitArr();
            SplitBean splitBean = splitBeans.get(m);
            String dataid = splitBean.getDataId();
            String detailid = splitBean.getDetailId();
            String flow_fromdate = splitBean.getFromDate();
            String flow_fromtime = splitBean.getFromTime();
            String flow_todate = splitBean.getToDate();
            String flow_totime = splitBean.getToTime();
            String fromdatedb = splitBean.getFromdatedb();
            String fromtimedb = splitBean.getFromtimedb();
            String todatedb = splitBean.getTodatedb();
            String totimedb = splitBean.getTotimedb();
            String requestid = splitBean.getRequestId();
            double d_mins = splitBean.getD_Mins();
            if(d_mins <= 0){
              continue;
            }
            String flow_key = mapKey+"|"+"dataid|"+dataid+"|"+"detailid|"+detailid+"|"+"requestid|"+requestid+"|flow_fromdate|"+flow_fromdate
                +"|flow_fromtime|"+flow_fromtime+"|flow_todate|"+flow_todate+"|flow_totime|"+flow_totime;
            int flow_count = (int) d_mins;
            String flowLogInfo = "flow_fromdate:"+flow_fromdate+":flow_fromtime:"+flow_fromtime
                +":flow_todate:"+flow_todate+":flow_totime:"+flow_totime+":requestid:"+requestid
                +":d_mins:"+d_mins;
            logOvertimeMap(overtimeLogMap, flowLogInfo, flow_key+"|加班流程信息|flowLogInfo");

            List<Map<String,String>> hasOverTimeList = Lists.newArrayList();
            getHasOverTimeData(resourceid,realSplitDate,hasOverTimeList);
            logOvertimeMap(overtimeLogMap, hasOverTimeList, flow_key+"|是否已经生成过加班，返回已经生成过加班的区间|hasOverTimeList");

            if(flow_fromdate.compareTo(realSplitDate) > 0){
              flow_fromtime = kqTimesArrayComInfo.turn24to48Time(flow_fromtime);
            }
            if(flow_todate.compareTo(realSplitDate) > 0){
              flow_totime = kqTimesArrayComInfo.turn24to48Time(flow_totime);
            }
            int flow_fromIndex = kqTimesArrayComInfo.getArrayindexByTimes(flow_fromtime);
            int flow_toIndex = kqTimesArrayComInfo.getArrayindexByTimes(flow_totime);
            Arrays.fill(initArrays_flow, flow_fromIndex, flow_toIndex,1);
            int all_has_duration_min = 0;
            //因为存在下班后的打卡和上班前的打卡。这两块的打卡需要合在一起来和合在一起的流程比较时长
            Map<String,String> hasCardMap = Maps.newHashMap();
            if(hasOverTimeList != null && !hasOverTimeList.isEmpty()){
              for(int p = 0 ; p < hasOverTimeList.size(); p++){
                Map<String,String> hasOverTimeMap = hasOverTimeList.get(p);
                String duration_min = Util.null2String(hasOverTimeMap.get("duration_min"));
                String flow_dataid = Util.null2String(hasOverTimeMap.get("flow_dataid"));
                String ori_belongdate = Util.null2String(hasOverTimeMap.get("ori_belongdate"));
                String has_requestid = Util.null2String(hasOverTimeMap.get("requestid"));

                String card_fromdate = Util.null2String(hasOverTimeMap.get("fromdate_flow"));
                String card_fromtime = Util.null2String(hasOverTimeMap.get("fromtime_flow"));
                String card_todate = Util.null2String(hasOverTimeMap.get("todate_flow"));
                String card_totime = Util.null2String(hasOverTimeMap.get("totime_flow"));
                String card_key = card_fromdate+"_"+card_fromtime+"_"+card_todate+"_"+card_totime;

                int int_duration_min = Util.getIntValue(duration_min,0);
                String has_key = has_requestid;
                String cur_key = requestid;
                if(flow_dataid.length() > 0){
                  has_key += "_"+dataid+"_"+detailid;
                  cur_key += "_"+flow_dataid;
                }
                //为什么要加这个判断呢，因为有可能我请假3小时，打卡1小时，然后这个时候生成1小时加班，然后我后面又补卡了2小时，这时候，加班流程虽然已经
//                生成过加班，但是其实只用了1小时，还需要拿这个加班流程来，减去已经用的1小时，d_mins就是这个加班流程实际可用的加班流程时长
                if(has_key.equalsIgnoreCase(cur_key)){
                  if(int_duration_min > 0){
                    d_mins = d_mins-(int_duration_min*1.0);
                  }
                }
                all_has_duration_min += int_duration_min;
                if(hasCardMap.containsKey(card_key)){
                  int tmp_int_duration_min = Util.getIntValue(hasCardMap.get(card_key));
                  hasCardMap.put(card_key, ""+(int_duration_min+tmp_int_duration_min));
                }else{
                  hasCardMap.put(card_key, int_duration_min+"");
                }
              }
            }
            if(d_mins > 0){

              int[] initArrays = kqTimesArrayComInfo.getInitArr();
              Map<String,String> signinoffMap = buildOvertimeCard(kqOvertimeCardBean, resourceid, realSplitDate, kqTimesArrayComInfo, restTimeMap, changeType_key,initArrays,hasOverTimeList,
                  overRulesDetailMap, false, overtimeLogMap);

              String signinTime = Util.null2String(signinoffMap.get("signinTime"));
              String signoutTime = Util.null2String(signinoffMap.get("signoutTime"));
              String signinDate = Util.null2String(signinoffMap.get("signinDate"));
              String signoutDate = Util.null2String(signinoffMap.get("signoutDate"));
              int signinTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signinTime);
              int signoutTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signoutTime);

              if(signinTimeIndex < signoutTimeIndex){
                String nowCardKey = signinDate+"_"+kqTimesArrayComInfo.turn48to24Time(signinTime)
                    +"_"+signoutDate+"_"+kqTimesArrayComInfo.turn48to24Time(signoutTime);
                int over_count = kqTimesArrayComInfo.getCnt(initArrays, signinTimeIndex, signoutTimeIndex, 1);
                int card_mins = over_count;
                logOvertimeMap(overtimeLogMap, over_count, flow_key+"|加班打卡生成的时长|over_count");
                if(over_count > 0){
                  //表示加班打卡是存在的
                  String flow_cross_key = flow_key+"|加班计算区间|"+signinTime+"-"+signoutTime;

                  int restTimeType = 1;
                  if(kqOvertimeRulesDetailEntity != null){
                    int minimumLen = kqOvertimeRulesDetailEntity.getMinimumLen();
                    if(card_mins < minimumLen){
                      continue;
                    }
                    logOvertimeMap(overtimeLogMap, JSON.toJSONString(kqOvertimeRulesDetailEntity), flow_key+"|加班规则|KQOvertimeRulesDetailEntity");
                    //我这个方法是针对每次生成的加班数据做排除休息时长的处理 打卡数据也要去掉休息时长
                    restTimeType = kqOvertimeRulesDetailEntity.getRestTimeType();
                    if(restTimeType == 2){
                      over_count = new KQOverTimeFlowBiz().handle_restlength(over_count,restTimeMap,changeType_key);
                    }
                  }
                  if(all_has_duration_min > 0){
                    for(Entry<String,String> me : hasCardMap.entrySet()){
                      String cardKey  = me.getKey();
                      String cardValue = me.getValue();
                      if(cardKey.equalsIgnoreCase(nowCardKey)){
                      }else{
                        all_has_duration_min = all_has_duration_min - Util.getIntValue(cardValue);
                      }
                    }
                    over_count = over_count-all_has_duration_min;
                  }
                  if(over_count <= 0){
                    continue;
                  }
                  int mins = over_count < d_mins ? over_count : (int)d_mins;
                  double double_mins = getD_MinsByUnit((1.0*mins));
                  mins = (int)double_mins;

                  RecordSet rs = new RecordSet();
                  String overtime_uuid = UUID.randomUUID().toString();
                  String flow_dataid = dataid+"_"+detailid;
                  String tiaoxiuId = "";
                  int computingMode = 2;

                  int unit = KQOvertimeRulesBiz.getMinimumUnit();
                  String workingHours = "";
                  String overtime_type = splitBean.getOvertime_type();
                  Map<String,Object> otherParam = Maps.newHashMap();
                  otherParam.put("overtime_type", overtime_type);
                  int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
                  if(2 == paidLeaveEnableType){
                    logOvertimeMap(overtimeLogMap, overtime_type, flow_cross_key+"|关联调休与否来自于流程选择,加班类型下拉框值|overtime_type");
                  }

                  int paidLeaveEnable = getPaidLeaveEnable(kqOvertimeRulesDetailEntity, overtime_type);
                  boolean needSplitByTime = false;//getNeedSplitByTime(kqOvertimeRulesDetailEntity, paidLeaveEnable);
                  if(needSplitByTime){
                    //        按照加班时长转调休的 时长设置
                    List<String> timepointList = null;
                    if(kqOvertimeRulesDetailEntity != null){
                      timepointList = get_timepointList(kqOvertimeRulesDetailEntity);
                      logOvertimeMap(overtimeLogMap, timepointList, flow_cross_key+"|如果要生成调休且是根据时间区间来转调休，返回对应的时间区间|timepointList");
                    }

                    List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = Lists.newArrayList();
                    //需要分段根据设置的时间区间来计算加班，这个第二种加班方式有有点冲突，不能做
//                    if(timepointList != null && !timepointList.isEmpty()){
//                      int[] time_initArrays = kqTimesArrayComInfo.getInitArr();
//                      get_overtimeBalanceTimeBeans(timepointList,overtimeBalanceTimeBeans,kqTimesArrayComInfo,time_initArrays,flow_cross_totime_index,2);
//                    }
                  }else{
                    logOvertimeMap(overtimeLogMap, mins, flow_cross_key+"|最终生成的加班时长|mins");
                    otherParam.put("overtimeLogMap", overtimeLogMap);

                    tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceid,realSplitDate,mins+"","0",workingHours,requestid,"1",flow_fromdate,otherParam);
                    if(Util.getIntValue(tiaoxiuId) > 0){
                      kqLog.info("doComputingMode2 生成调休成功，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate+":requestid:"+requestid);
                    }else{
                      kqLog.info("doComputingMode2 生成调休失败，调休id:"+tiaoxiuId+":resourceid:"+resourceid+":realSplitDate:"+realSplitDate+":requestid:"+requestid);
                    }
                    logOvertimeMap(overtimeLogMap, tiaoxiuId, flow_cross_key+"|最终生成的调休id|tiaoxiuId");

                    String flow_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                        + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,card_mins,ori_belongdate,flow_dataid)"+
                        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                    signinTime = kqTimesArrayComInfo.turn48to24Time(signinTime);
                    signoutTime = kqTimesArrayComInfo.turn48to24Time(signoutTime);
                    if(fromtimedb.length() == 5){
                      fromtimedb = fromtimedb+":00";
                    }
                    if(totimedb.length() == 5){
                      totimedb = totimedb+":00";
                    }
                    boolean isUp = rs.executeUpdate(flow_overtime_sql, requestid,resourceid,fromdatedb,fromtimedb,todatedb,totimedb,mins,"",realSplitDate,
                        "",unit,changeType,paidLeaveEnable,computingMode,tiaoxiuId,overtime_uuid,signinDate,signinTime,signoutDate,signoutTime,flow_count,card_mins,splitDate,flow_dataid);
                    String overtimeid = get_overtime_uuid(overtime_uuid);
                    kqEventLogBiz.updateOvertimeId(uuid, overtimeid);
                  }

                }
              }
            }else{
              logOvertimeMap(overtimeLogMap, "流程时长d_mins为0，不生成加班", flow_key+"|加班流程信息|flowLogInfo");
            }
          }
        }

      }else{
        logOvertimeMap(overtimeLogMap, "打卡数据KQOvertimeCardBean为null", "打卡和上下班数据|KQOvertimeCardBean");
      }
    }
    kqEventLogBiz.logDetailOvertimeMap(resourceid,overtimeLogMap,uuid);
    kqEventLogBiz.logDetailEvent(resourceid,eventMap,uuid,"doComputingMode2|对应的加班流程数据|key|"+key);

  }

  /**
   * 打卡的时长计算，流程+打卡 和纯打卡的可以拿来共用
   * needHasOverTime 是否需要按照打卡区间排除重复打卡的数据，第二种流程和打卡比较时长的不需要这个方式
   */
  public Map<String,String> buildOvertimeCard(KQOvertimeCardBean kqOvertimeCardBean,
      String resourceid, String splitDate, KQTimesArrayComInfo kqTimesArrayComInfo,
      Map<String, List<String[]>> restTimeMap, String changeType_key, int[] initArrays,
      List<Map<String, String>> hasOverTimeList,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap, boolean needHasOverTime,
      Map<String, Object> overtimeLogMap) throws Exception{
    Map<String,String> signinoffMap = Maps.newHashMap();

    String signinDate = kqOvertimeCardBean.getSigninDate();
    String signinTime = kqOvertimeCardBean.getSigninTime();
    String signoutDate = kqOvertimeCardBean.getSignoutDate();
    String signoutTime = kqOvertimeCardBean.getSignoutTime();

    if(hasOverTimeList.isEmpty()){
      getHasOverTimeData(resourceid,splitDate,hasOverTimeList);
    }

    if(signinDate.compareTo(splitDate) > 0){
      signinTime = kqTimesArrayComInfo.turn24to48Time(signinTime);
      if(signinTime.length() > 0){
        signinTime = signinTime+ ":00";
      }
    }
    if(signoutDate.compareTo(splitDate) > 0){
      signoutTime = kqTimesArrayComInfo.turn24to48Time(signoutTime);
      if(signoutTime.length() > 0){
        signoutTime = signoutTime+ ":00";
      }
    }
    if(signinTime.length() == 0 || signoutTime.length() == 0){
      return signinoffMap;
    }

    int signinTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signinTime);
    int signoutTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signoutTime);

    signinoffMap.put("signinTime", signinTime);
    signinoffMap.put("signoutTime", signoutTime);
    signinoffMap.put("signinDate", signinDate);
    signinoffMap.put("signoutDate", signoutDate);
    //先把打卡数据都列出来 置位1
    if(signinTimeIndex < signoutTimeIndex){
      Arrays.fill(initArrays, signinTimeIndex, signoutTimeIndex,1);
    }
    KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
    if(restTimeMap.containsKey(changeType_key)){
      List<String[]> restTimeList = restTimeMap.get(changeType_key);
      logOvertimeMap(overtimeLogMap, restTimeList, "非工作时间设置的排除休息时间区间|restTimeList");
      //再把休息时间填充上去
      if(!restTimeList.isEmpty()){
        int restTimeType = 1;
        if(kqOvertimeRulesDetailEntity != null){
          restTimeType = kqOvertimeRulesDetailEntity.getRestTimeType();
        }
        if(restTimeType == 1){
          for(int j =0 ; j < restTimeList.size() ; j++){
            String[] restTimes = restTimeList.get(j);
            if(restTimes.length == 2){
              int restStart = kqTimesArrayComInfo.getArrayindexByTimes(restTimes[0]);
              int restEnd = kqTimesArrayComInfo.getArrayindexByTimes(restTimes[1]);
              if(restEnd == 1439){
                //针对跨天的休息时段单独处理排除掉23:59-00:00的时间
                restEnd = 1440;
              }
              int hasRestMins = kqTimesArrayComInfo.getCnt(initArrays, restStart,restEnd,1);
              if(hasRestMins == 0) {
                restStart = kqTimesArrayComInfo.turn24to48TimeIndex(restStart);
                restEnd = kqTimesArrayComInfo.turn24to48TimeIndex(restEnd);
              }
              if(restStart < restEnd){
                Arrays.fill(initArrays, restStart, restEnd, -2);
              }
            }
          }
        }else{
        }
      }
    }

    boolean isNextDay = false;
    clearWorkAndRestTime(resourceid,splitDate,isNextDay,kqTimesArrayComInfo,overtimeLogMap,kqOvertimeRulesDetailEntity,initArrays,signinTimeIndex);
    isNextDay = true;
    clearWorkAndRestTime(resourceid,DateUtil.addDate(splitDate, 1),isNextDay, kqTimesArrayComInfo,
        overtimeLogMap, kqOvertimeRulesDetailEntity, initArrays, signinTimeIndex);

    if(!hasOverTimeList.isEmpty() && needHasOverTime){
      if(hasOverTimeList != null && !hasOverTimeList.isEmpty()){
        for(int p = 0 ; p < hasOverTimeList.size(); p++){
          Map<String,String> hasOverTimeMap = hasOverTimeList.get(p);
          String duration_min = Util.null2String(hasOverTimeMap.get("duration_min"));
          String fromdate = Util.null2String(hasOverTimeMap.get("fromdate"));
          String fromtime = Util.null2String(hasOverTimeMap.get("fromtime"));
          String todate = Util.null2String(hasOverTimeMap.get("todate"));
          String totime = Util.null2String(hasOverTimeMap.get("totime"));
          if(fromdate.compareTo(splitDate) > 0){
            fromtime = kqTimesArrayComInfo.turn24to48Time(fromtime);
          }
          if(todate.compareTo(splitDate) > 0){
            totime = kqTimesArrayComInfo.turn24to48Time(totime);
          }
          int begintimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromtime);
          int endtimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(totime);
          if(begintimeIndex < endtimeIndex){
            Arrays.fill(initArrays, begintimeIndex, endtimeIndex, -2);
          }
        }
      }
    }
    return signinoffMap;
  }

  /**
   * 把指定的打卡区间内的上下班时间和休息时间去除
   * @param resourceid
   * @param splitDate
   * @param isNextDay 是否是处理处理明日的，如果是true的话，上下班时间和休息时间都要+1440
   * @param kqTimesArrayComInfo
   * @param overtimeLogMap
   * @param kqOvertimeRulesDetailEntity
   * @param initArrays
   * @param signinTimeIndex
   */
  public void clearWorkAndRestTime(String resourceid, String splitDate, boolean isNextDay,
      KQTimesArrayComInfo kqTimesArrayComInfo,
      Map<String, Object> overtimeLogMap,
      KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity, int[] initArrays,
      int signinTimeIndex) {

    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(resourceid, splitDate);
    if (workTime == null || workTime.getWorkMins() == 0) {
    }else{
      if (workTime.getKQType().equals("3")) {//自由工时
        //目前自由工时不加班
      } else {
        boolean oneSign = false;
        List<TimeScopeEntity> lsSignTime = new ArrayList<>();
        List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
        List<TimeScopeEntity> lsRestTime = new ArrayList<>();
        if (workTime != null) {
          lsSignTime = workTime.getSignTime();//允许打卡时间
          lsWorkTime = workTime.getWorkTime();//工作时间
          lsRestTime = workTime.getRestTime();//休息时段时间
          oneSign = lsWorkTime!=null&&lsWorkTime.size()==1;

          if(lsWorkTime != null && !lsWorkTime.isEmpty()){
            for (int i = 0; lsWorkTime != null && i < lsWorkTime.size(); i++) {
              TimeScopeEntity workTimeScope = lsWorkTime.get(i);
              if(oneSign){
                boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
                if(is_flow_humanized){
                  String workBeginTime = Util.null2String(workTimeScope.getBeginTime());
                  String ori_workBeginTime = workBeginTime;
                  int workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
                  boolean workBenginTimeAcross = workTimeScope.getBeginTimeAcross();
                  String workEndTime = Util.null2String(workTimeScope.getEndTime());
                  String ori_workEndTime = workEndTime;
                  int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
                  boolean workEndTimeAcross = workTimeScope.getEndTimeAcross();

                  Map<String, String> shifRuleMap = Maps.newHashMap();
                  //个性化设置只支持一天一次上下班
                  ShiftInfoBean shiftInfoBean = new ShiftInfoBean();
                  shiftInfoBean.setSplitDate(splitDate);
                  shiftInfoBean.setShiftRuleMap(workTime.getShiftRuleInfo());
                  shiftInfoBean.setSignTime(lsSignTime);
                  shiftInfoBean.setWorkTime(lsWorkTime);
                  List<String> logList = Lists.newArrayList();
                  KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean, resourceid, shifRuleMap,logList);
                  if(!shifRuleMap.isEmpty()){
                    if(shifRuleMap.containsKey("shift_beginworktime")){
                      String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
                      if(shift_beginworktime.length() > 0){
                        workBeginTime = Util.null2String(shift_beginworktime);
                        workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
                        workTimeScope.setBeginTime(workBeginTime);
                        workTimeScope.setBeginTimeAcross(workBeginIdx>=1440?true:false);
                      }
                    }
                    if(shifRuleMap.containsKey("shift_endworktime")){
                      String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
                      if(shift_endworktime.length() > 0){
                        workEndTime = Util.null2String(shift_endworktime);
                        workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
                        workTimeScope.setEndTime(workEndTime);
                        workTimeScope.setEndTimeAcross(workEndIdx>=1440?true:false);
                      }
                    }
                  }
                }
              }
            }
            //目前只处理上班前和下班后的加班数据，上班中间的数据不处理，所以从第一次上班时间到最后下班时间都是无效的加班打卡
            TimeScopeEntity first_TimeScopeEntity = lsWorkTime.get(0);
            TimeScopeEntity last_TimeScopeEntity = lsWorkTime.get(lsWorkTime.size()-1);

            String begintime = first_TimeScopeEntity.getBeginTime();
            String endtime = last_TimeScopeEntity.getEndTime();
            int begintimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(begintime);
            int endtimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(endtime);
            if(isNextDay){
              begintimeIndex = begintimeIndex + 1440;
              endtimeIndex = endtimeIndex + 1440;
              if(begintimeIndex >= initArrays.length){
                begintimeIndex = initArrays.length-1;
              }
              if(endtimeIndex >= initArrays.length){
                endtimeIndex = initArrays.length-1;
              }
            }
            String workTimeLogInfo = "begintime:"+begintime+":endtime:"+endtime+":isNextDay:"+isNextDay;
            logOvertimeMap(overtimeLogMap, workTimeLogInfo, "工作日的上下班时间|workTimeLogInfo");
            if(begintimeIndex < endtimeIndex){
              Arrays.fill(initArrays, begintimeIndex, endtimeIndex, -2);
            }
            if(!isNextDay){
              if(kqOvertimeRulesDetailEntity != null){
                int startTime = kqOvertimeRulesDetailEntity.getStartTime();
                if(startTime > -1){
                  int after_endtimeIndex = endtimeIndex + startTime;
                  if(after_endtimeIndex > endtimeIndex){
                    Arrays.fill(initArrays, endtimeIndex, after_endtimeIndex, -2);
                  }
                }
                int has_cut_point = kqOvertimeRulesDetailEntity.getHas_cut_point();
                if(has_cut_point == 1){
                  int before_startTime = kqOvertimeRulesDetailEntity.getBefore_startTime();
                  if(before_startTime > -1){
                    int before_begintimeIndex = begintimeIndex - before_startTime;
                    if(before_begintimeIndex > signinTimeIndex){
                      Arrays.fill(initArrays, signinTimeIndex, before_begintimeIndex, -2);
                    }
                  }
                }
              }
            }
          }
          if(lsRestTime != null && !lsRestTime.isEmpty()){
            String restTimeLogInfo = JSON.toJSONString(lsRestTime);
            logOvertimeMap(overtimeLogMap, restTimeLogInfo, "工作日的休息时间|restTimeLogInfo");
            for(int p = 0 ; p < lsRestTime.size(); p++){
              TimeScopeEntity rest_TimeScopeEntity = lsRestTime.get(p);
              String begintime = rest_TimeScopeEntity.getBeginTime();
              String endtime = rest_TimeScopeEntity.getEndTime();
              int begintimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(begintime);
              int endtimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(endtime);
              if(isNextDay){
                begintimeIndex = begintimeIndex + 1440;
                endtimeIndex = endtimeIndex + 1440;
                if(begintimeIndex >= initArrays.length){
                  begintimeIndex = initArrays.length-1;
                }
                if(endtimeIndex >= initArrays.length){
                  endtimeIndex = initArrays.length-1;
                }
              }
              if(begintimeIndex < endtimeIndex){
                Arrays.fill(initArrays, begintimeIndex, endtimeIndex, -2);
              }
            }
          }

        }
      }
    }
  }

  /**
   * 根据打卡数据和加班归属 拆分出来需要计算加班的区段
   * @param resourceid
   * @param fromDate
   * @param toDate
   * @param dateFormatter
   * @param kqTimesArrayComInfo
   * @param overRulesDetailMap
   * @param changeTypeMap
   * @param lsCheckInfoMaps
   * @param eventMap
   */
  public void getOverTimeCardDataMap(String resourceid, String fromDate, String toDate,
      DateTimeFormatter dateFormatter,
      KQTimesArrayComInfo kqTimesArrayComInfo,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      Map<String, Integer> changeTypeMap, Map<String, KQOvertimeCardBean> lsCheckInfoMaps,
      Map<String, Object> eventMap) throws Exception{

    KQOverTimeFlowBiz kqOverTimeFlowBiz = new KQOverTimeFlowBiz();

    List<Object> lsCheckInfos = Lists.newArrayList();
    Map<String,String> result = getSignData(resourceid, fromDate, dateFormatter,eventMap,lsCheckInfos,changeTypeMap,overRulesDetailMap);

    //获取一下当天的上班时间
    String firstworkDate = Util.null2String(result.get("firstworkDate"));
    String firstworkTime = Util.null2String(result.get("firstworkTime"));
    int pre_has_cut_point = Util.getIntValue(Util.null2String(result.get("pre_has_cut_point")));
    String pre_cut_point = Util.null2String(result.get("pre_cut_point"));
    int has_cut_point = Util.getIntValue(Util.null2String(result.get("has_cut_point")));
    String cut_point = Util.null2String(result.get("cut_point"));
    int before_startTime = Util.getIntValue(Util.null2String(result.get("before_startTime")));
    int preChangeType = Util.getIntValue(Util.null2String(result.get("preChangeType")));
    int changeType = Util.getIntValue(Util.null2String(result.get("changeType")));

    LocalDate localbelongDate = LocalDate.parse(fromDate);
    String splitDate = localbelongDate.format(dateFormatter);
    LocalDate preLocalDate = localbelongDate.minusDays(1);
    String preSplitDate = preLocalDate.format(dateFormatter);
    String nextSplitDate = localbelongDate.plusDays(1).format(dateFormatter);

    String preKey = resourceid+"_"+preSplitDate;
    String key = resourceid+"_"+splitDate;
    String nextkey = resourceid+"_"+nextSplitDate;

    eventMap.put("lsCheckInfos", lsCheckInfos);
    if(!lsCheckInfos.isEmpty()){
      Map<String, Object> signMap = (Map<String, Object>) lsCheckInfos.get(0);
      String signindate = "";
      String signintime = "";
      String signoutdate = "";
      String signouttime = "";

      if(signMap != null && !signMap.isEmpty()){
        signindate = Util.null2String(signMap.get("signindate"));
        signintime = Util.null2String(signMap.get("signintime"));
        signoutdate = Util.null2String(signMap.get("signoutdate"));
        signouttime = Util.null2String(signMap.get("signouttime"));
        if(lsCheckInfos.size() > 1){
          Map<String, Object> lastSignMap = (Map<String, Object>) lsCheckInfos.get(lsCheckInfos.size()-1);
          signoutdate = Util.null2String(lastSignMap.get("signoutdate"));
          signouttime = Util.null2String(lastSignMap.get("signouttime"));
        }
      }
      KQOvertimeCardBean kqOvertimeBean = new KQOvertimeCardBean();
      kqOvertimeBean.setSigninDate(signindate);
      kqOvertimeBean.setSigninTime(signintime);
      kqOvertimeBean.setSignoutDate(signoutdate);
      kqOvertimeBean.setSignoutTime(signouttime);
      kqOvertimeBean.setBelongDate(splitDate);
      eventMap.put("has_cut_point", has_cut_point);
      if(pre_has_cut_point == 0){
        //未设置打卡归属
        if(preChangeType == 2){
          eventMap.put("preChangeType", preChangeType);
          //如果前一天是工作日
          String pre_overtime_cut_point = "";
          ShiftInfoBean pre_shiftInfoBean = KQDurationCalculatorUtil
              .getWorkTime(resourceid, preSplitDate, false);
          if(pre_shiftInfoBean != null){
            List<int[]> workLongTimeIndex = pre_shiftInfoBean.getWorkLongTimeIndex();
            List<int[]> real_workLongTimeIndex = Lists.newArrayList();
            SplitBean splitBean = new SplitBean();
            splitBean.setResourceId(resourceid);
            kqOverTimeFlowBiz.get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,pre_shiftInfoBean,kqTimesArrayComInfo,splitBean);

            if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
              pre_overtime_cut_point = kqTimesArrayComInfo.getTimesByArrayindex(real_workLongTimeIndex.get(0)[0]);
            }
            eventMap.put("pre_overtime_cut_point", pre_overtime_cut_point);
            if(pre_overtime_cut_point.length() >= 0){
              rePutCheckInfoMap(lsCheckInfoMaps, kqOvertimeBean, preKey, key, pre_overtime_cut_point, splitDate,eventMap);
            }
          }else{
            String errorMsg = "前一天是工作日但是前一天的ShiftInfoBean班次获取不到信息";
            eventMap.put("errorMsg", errorMsg);
          }
        }else {
          eventMap.put("changeType", changeType);
          if(changeType == 2){
            eventMap.put("firstworkTime", firstworkTime);
            if(has_cut_point == 1 && before_startTime > 0){
              firstworkTime = kqTimesArrayComInfo.getTimesByArrayindex(kqTimesArrayComInfo.getArrayindexByTimes(firstworkTime)-before_startTime);
            }
            //如果前一天是非工作日，今天是工作日的话
            rePutCheckInfoMap(lsCheckInfoMaps, kqOvertimeBean, preKey, key, firstworkTime, splitDate,
                eventMap);
          }else{
            //如果前一天是非工作日，今天是非工作日的话，那就是打卡获取的是啥就是啥
            lsCheckInfoMaps.put(key, kqOvertimeBean);
          }
        }
      }else{
        String pre_splittime = "";
        List<Object> pre_lsCheckInfos = Lists.newArrayList();
        getSignData(resourceid, preSplitDate, dateFormatter, eventMap, pre_lsCheckInfos, changeTypeMap, overRulesDetailMap);
        if(!pre_lsCheckInfos.isEmpty()){
          Map<String, Object> preSignMap = (Map<String, Object>) pre_lsCheckInfos.get(0);
          String pre_signindate = "";
          String pre_signintime = "";
          String pre_signoutdate = "";
          String pre_signouttime = "";
          if(preSignMap != null && !preSignMap.isEmpty()){
            pre_signindate = Util.null2String(preSignMap.get("signindate"));
            pre_signintime = Util.null2String(preSignMap.get("signintime"));
            pre_signoutdate = Util.null2String(preSignMap.get("signoutdate"));
            pre_signouttime = Util.null2String(preSignMap.get("signouttime"));
            if(pre_signindate.length() > 0 && pre_signintime.length() > 0){
              pre_splittime = pre_signindate+" "+pre_signintime;
            }else if(pre_signoutdate.length() > 0 && pre_signouttime.length() > 0){
              pre_splittime = pre_signoutdate+" "+pre_signouttime;
            }
          }
        }
        eventMap.put("pre_cut_point", pre_cut_point);
        //设置了打卡归属 那么一天的打卡就可能被前一天给拆成两部分和后一天的打卡归属给拆分成两部分
        rePutCheckInfoCutPointMap(lsCheckInfoMaps, kqOvertimeBean, preKey, key, pre_cut_point, splitDate,
            eventMap,cut_point,nextSplitDate,nextkey,has_cut_point,pre_splittime);
      }
    }
  }

  /**
   * 获取打卡数据
   * @param resourceid
   * @param belongDate
   * @param dateFormatter
   * @param eventMap
   * @param lsCheckInfos
   * @param changeTypeMap
   * @param overRulesDetailMap
   */
  public Map<String, String> getSignData(String resourceid, String belongDate,
      DateTimeFormatter dateFormatter, Map<String, Object> eventMap,
      List<Object> lsCheckInfos, Map<String, Integer> changeTypeMap,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap) throws Exception{
    Map<String,String> result = Maps.newHashMap();
    String firstworkDate = "";
    String firstworkTime = "";

    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
    LocalDate localbelongDate = LocalDate.parse(belongDate);
    String splitDate = localbelongDate.format(dateFormatter);
    LocalDate preLocalDate = localbelongDate.minusDays(1);
    String preSplitDate = preLocalDate.format(dateFormatter);
    String nextSplitDate = localbelongDate.plusDays(1).format(dateFormatter);

    String change_key = splitDate+"_"+resourceid;
    String preChange_key = preSplitDate+"_"+resourceid;
    String nextChange_key = nextSplitDate+"_"+resourceid;
    int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
    int preChangeType = Util.getIntValue(""+changeTypeMap.get(preChange_key),-1);
    int nextChangeType = Util.getIntValue(""+changeTypeMap.get(nextChange_key),-1);
    String changeType_key = splitDate+"_"+changeType;
    String preChangeType_key = preSplitDate+"_"+preChangeType;
    String nextChangeType_key = nextSplitDate+"_"+nextChangeType;

    KQOvertimeRulesDetailEntity curKqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
    KQOvertimeRulesDetailEntity preKqOvertimeRulesDetailEntity = overRulesDetailMap.get(preChangeType_key);
    KQOvertimeRulesDetailEntity nextKqOvertimeRulesDetailEntity = overRulesDetailMap.get(preChangeType_key);

    int pre_has_cut_point = 0;
    String pre_cut_point = "";
    if(preKqOvertimeRulesDetailEntity != null){
      pre_has_cut_point = preKqOvertimeRulesDetailEntity.getHas_cut_point();
      pre_cut_point = preKqOvertimeRulesDetailEntity.getCut_point();
    }
    int has_cut_point = 0;
    String cut_point = "";
    int before_startTime = -1;
    if(curKqOvertimeRulesDetailEntity != null){
      has_cut_point = curKqOvertimeRulesDetailEntity.getHas_cut_point();
      cut_point = curKqOvertimeRulesDetailEntity.getCut_point();
      before_startTime =curKqOvertimeRulesDetailEntity.getBefore_startTime();
    }

    result.put("pre_has_cut_point", ""+pre_has_cut_point);
    result.put("pre_cut_point", pre_cut_point);
    result.put("has_cut_point", ""+has_cut_point);
    result.put("cut_point", cut_point);
    result.put("before_startTime", ""+before_startTime);
    result.put("preChangeType", ""+preChangeType);
    result.put("changeType", ""+changeType);

    KQWorkTime kqWorkTime = new KQWorkTime();
    WorkTimeEntity workTime = kqWorkTime.getWorkTime(resourceid, splitDate);

    LocalDate pre_curLocalDate = localbelongDate.minusDays(1);
    String preDate = pre_curLocalDate.format(dateFormatter);
    LocalDate next_curLocalDate = localbelongDate.plusDays(1);
    String nextDate = next_curLocalDate.format(dateFormatter);

    if (workTime == null || workTime.getWorkMins() == 0) {
      Map<String, Object> signMap = getRestSignInfo(resourceid,splitDate,preDate,nextDate,curKqOvertimeRulesDetailEntity,eventMap,preKqOvertimeRulesDetailEntity);
      if(signMap != null && !signMap.isEmpty()){
        lsCheckInfos.add(signMap);
      }
    }else{
      if (workTime.getKQType().equals("3")) {//自由工时
        //目前自由工时不加班
      } else {
        boolean oneSign = false;
        List<TimeScopeEntity> lsSignTime = new ArrayList<>();
        List<TimeScopeEntity> lsWorkTime = new ArrayList<>();
        List<TimeScopeEntity> lsRestTime = new ArrayList<>();
        oneSign = lsWorkTime!=null&&lsWorkTime.size()==1;
        boolean need_middle_time = false;

        if (workTime != null) {
          lsSignTime = workTime.getSignTime();//允许打卡时间
          lsWorkTime = workTime.getWorkTime();//工作时间
          result.put("lsWorkTime_size", ""+lsWorkTime.size());
          lsRestTime = workTime.getRestTime();//休息时段时间
          oneSign = lsWorkTime!=null&&lsWorkTime.size()==1;
          for (int i = 0; lsWorkTime != null && i < lsWorkTime.size(); i++) {
            TimeScopeEntity signTimeScope = lsSignTime.get(i);
            TimeScopeEntity workTimeScope = lsWorkTime.get(i);
            if(i == 0){
              firstworkDate = splitDate;
              firstworkTime = workTimeScope.getBeginTime();
            }
            if(!oneSign){
              if(!need_middle_time){
                //多次打卡的时候，中间打卡不算加班
                if(i != 0 && i != lsWorkTime.size()-1){
                  continue;
                }
              }
            }else {
              boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
              if(is_flow_humanized){
                String workBeginTime = Util.null2String(workTimeScope.getBeginTime());
                String ori_workBeginTime = workBeginTime;
                int workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
                boolean workBenginTimeAcross = workTimeScope.getBeginTimeAcross();
                String workEndTime = Util.null2String(workTimeScope.getEndTime());
                String ori_workEndTime = workEndTime;
                int workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
                boolean workEndTimeAcross = workTimeScope.getEndTimeAcross();

                Map<String, String> shifRuleMap = Maps.newHashMap();
                //个性化设置只支持一天一次上下班
                ShiftInfoBean shiftInfoBean = new ShiftInfoBean();
                shiftInfoBean.setSplitDate(splitDate);
                shiftInfoBean.setShiftRuleMap(workTime.getShiftRuleInfo());
                shiftInfoBean.setSignTime(lsSignTime);
                shiftInfoBean.setWorkTime(lsWorkTime);
                List<String> logList = Lists.newArrayList();
                KQShiftRuleInfoBiz.getShiftRuleInfo(shiftInfoBean, resourceid, shifRuleMap,logList);
                if(!shifRuleMap.isEmpty()){
                  if(shifRuleMap.containsKey("shift_beginworktime")){
                    String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
                    if(shift_beginworktime.length() > 0){
                      workBeginTime = Util.null2String(shift_beginworktime);
                      workBeginIdx = kqTimesArrayComInfo.getArrayindexByTimes(workBeginTime);
                      workTimeScope.setBeginTime(workBeginTime);
                      workTimeScope.setBeginTimeAcross(workBeginIdx>=1440?true:false);
                      if(i == 0){
                        firstworkDate = workBeginIdx>=1440 ? nextDate : splitDate;
                        firstworkTime = workBeginTime;
                      }
                    }
                  }
                  if(shifRuleMap.containsKey("shift_endworktime")){
                    String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
                    if(shift_endworktime.length() > 0){
                      workEndTime = Util.null2String(shift_endworktime);
                      workEndIdx = kqTimesArrayComInfo.getArrayindexByTimes(workEndTime);
                      workTimeScope.setEndTime(workEndTime);
                      workTimeScope.setEndTimeAcross(workEndIdx>=1440?true:false);
                    }
                  }
                }
              }
            }
            Map<String, Object> signMap = getSignInfo(resourceid,signTimeScope,workTimeScope,splitDate,preDate,nextDate,kqTimesArrayComInfo,eventMap,i,result);
            if(signMap != null && !signMap.isEmpty()){
              //目前一天多次打卡的话，只获取第一次和最后一次打卡
              lsCheckInfos.add(signMap);
            }
          }
        }
      }
    }

    if(firstworkDate.length() > 0 && firstworkTime.length() > 0){
      result.put("firstworkDate", firstworkDate);
      result.put("firstworkTime", firstworkTime);
    }
    return result;
  }

  public void rePutCheckInfoCutPointMap(Map<String, KQOvertimeCardBean> lsCheckInfoMaps,
      KQOvertimeCardBean kqOvertimeBean, String preKey, String key, String pre_cut_point,
      String splitDate, Map<String, Object> eventMap, String cut_point, String nextDate,
      String nextkey, int has_cut_point, String pre_splitdatetime) {
    String preDate = DateUtil.addDate(splitDate, -1);
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

    String pre_cut_points = pre_cut_point+":00";
    String pre_date_cut_point = splitDate+" "+pre_cut_points;

    String cut_points = cut_point+":00";
    String cur_date_cut_point = nextDate+" "+cut_points;

    String signindate = kqOvertimeBean.getSigninDate();
    String signoutdate = kqOvertimeBean.getSignoutDate();
    String signintime = kqOvertimeBean.getSigninTime();
    String signouttime = kqOvertimeBean.getSignoutTime();
    String signindatetime = kqOvertimeBean.getSigninDate()+" "+kqOvertimeBean.getSigninTime();
    String signoutdatetime = "";
    if(kqOvertimeBean.getSignoutDate().length() > 0 && kqOvertimeBean.getSignoutTime().length() > 0){
      //如果是休息日的话，可能存在没有上班时间的情况
      signoutdatetime = kqOvertimeBean.getSignoutDate()+" "+kqOvertimeBean.getSignoutTime();
    }
    String pre_splittime_date = "";
    String pre_splittime_time = "";
    if(pre_splitdatetime.length() > 0){
      String[] pre_splittimes = pre_splitdatetime.split(" ");
      if(pre_splittimes.length == 2){
        pre_splittime_date = pre_splittimes[0];
        pre_splittime_time = pre_splittimes[1];
      }
    }

    String timeInfo = "date_cut_point:"+pre_date_cut_point+":signoutdatetime:"+signoutdatetime+":signindatetime:"+signindatetime;
    String timeCompare1 = "date_cut_point.compareTo(signoutdatetime):"+pre_date_cut_point.compareTo(signoutdatetime);
    String timeCompare2 = "date_cut_point.compareTo(signindatetime):"+pre_date_cut_point.compareTo(signindatetime);
    eventMap.put("timeInfo", timeInfo);
    eventMap.put("timeCompare1", timeCompare1);
    eventMap.put("timeCompare2", timeCompare2);
    if(pre_date_cut_point.compareTo(signindatetime) > 0){
      //如果归属大于签到时间，小于签退时间，表示归属之前是属于前一天的，归属之后的是属于今天的
      //开启了归属之后，归属点之前的打卡就属于前一天的签退卡了，相当于是从前一天的下班时间到这个卡都算是前一天的加班卡，和不开归属的时候不一样
      KQOvertimeCardBean preKqOvertimeBean = new KQOvertimeCardBean();
      if(pre_splittime_date.length() > 0 && pre_splittime_time.length() > 0){
        preKqOvertimeBean.setSigninDate(pre_splittime_date);
        preKqOvertimeBean.setSigninTime(pre_splittime_time);
        preKqOvertimeBean.setSignoutDate(signindate);
        preKqOvertimeBean.setSignoutTime(signintime);
        preKqOvertimeBean.setBelongDate(preDate);
        lsCheckInfoMaps.put(preKey, preKqOvertimeBean);
      }
      if(signoutdatetime.length() > 0 && signoutdatetime.compareTo(pre_date_cut_point) > 0){
        KQOvertimeCardBean curOvertimeBean = new KQOvertimeCardBean();
        curOvertimeBean.setSigninDate(splitDate);
        curOvertimeBean.setSigninTime(pre_cut_points);
        curOvertimeBean.setSignoutDate(signoutdate);
        curOvertimeBean.setSignoutTime(signouttime);
        curOvertimeBean.setBelongDate(splitDate);
        lsCheckInfoMaps.put(key, curOvertimeBean);
      }

    }else if(signoutdatetime.length() > 0 && pre_date_cut_point.compareTo(signoutdatetime) > 0){
      //如果归属大于签退时间 表示这个时间都是属于前一天的
      lsCheckInfoMaps.put(preKey, kqOvertimeBean);
    }else {
      lsCheckInfoMaps.put(key, kqOvertimeBean);
    }
    if(signoutdate.length() > 0){
      if(signoutdate.compareTo(splitDate) > 0){
        if(1 == has_cut_point){
          //如果签退大于签到 表示打卡跨天
          if(cur_date_cut_point.compareTo(signoutdatetime) < 0){
            String hasSigninDate = signindate;
            String hasSigninTime = signintime;
            if(lsCheckInfoMaps.containsKey(key)){
              KQOvertimeCardBean hasOvertimeBean = lsCheckInfoMaps.get(key);
              hasSigninDate = hasOvertimeBean.getSigninDate();
              hasSigninTime = hasOvertimeBean.getSigninTime();
            }
            KQOvertimeCardBean curOvertimeBean = new KQOvertimeCardBean();
            curOvertimeBean.setSigninDate(hasSigninDate);
            curOvertimeBean.setSigninTime(hasSigninTime);
            curOvertimeBean.setSignoutDate(nextDate);
            curOvertimeBean.setSignoutTime(cut_points);
            curOvertimeBean.setBelongDate(splitDate);
            lsCheckInfoMaps.put(key, curOvertimeBean);

            KQOvertimeCardBean nextOvertimeBean = new KQOvertimeCardBean();
            nextOvertimeBean.setSigninDate(nextDate);
            nextOvertimeBean.setSigninTime(cut_points);
            nextOvertimeBean.setSignoutDate(signoutdate);
            nextOvertimeBean.setSignoutTime(signouttime);
            nextOvertimeBean.setBelongDate(nextDate);
            lsCheckInfoMaps.put(nextkey, nextOvertimeBean);
          }
        }
      }
    }
  }


  /**
   * 重新把打卡数据按照归属来赋值
   * @param lsCheckInfoMaps
   * @param kqOvertimeBean
   * @param preKey
   * @param key
   * @param overtime_cut_point
   * @param splitDate
   * @param eventMap
   */
  public void rePutCheckInfoMap(Map<String, KQOvertimeCardBean> lsCheckInfoMaps,
      KQOvertimeCardBean kqOvertimeBean,
      String preKey, String key, String overtime_cut_point, String splitDate,
      Map<String, Object> eventMap){

    String preDate = DateUtil.addDate(splitDate, -1);
    String overtime_cut_points = overtime_cut_point+":00";
    String date_cut_point = splitDate+" "+overtime_cut_points;
    String signindate = kqOvertimeBean.getSigninDate();
    String signoutdate = kqOvertimeBean.getSignoutDate();
    String signintime = kqOvertimeBean.getSigninTime();
    String signouttime = kqOvertimeBean.getSignoutTime();
    String signindatetime = kqOvertimeBean.getSigninDate()+" "+kqOvertimeBean.getSigninTime();
    String signoutdatetime = "";
    if(kqOvertimeBean.getSignoutDate().length() > 0 && kqOvertimeBean.getSignoutTime().length() > 0){
      //如果是休息日的话，可能存在没有上班时间的情况
      signoutdatetime = kqOvertimeBean.getSignoutDate()+" "+kqOvertimeBean.getSignoutTime();
    }

    String timeInfo = "date_cut_point:"+date_cut_point+":signoutdatetime:"+signoutdatetime+":signindatetime:"+signindatetime;
    String timeCompare1 = "date_cut_point.compareTo(signoutdatetime):"+date_cut_point.compareTo(signoutdatetime);
    String timeCompare2 = "date_cut_point.compareTo(signindatetime):"+date_cut_point.compareTo(signindatetime);
    eventMap.put("timeInfo", timeInfo);
    eventMap.put("timeCompare1", timeCompare1);
    eventMap.put("timeCompare2", timeCompare2);
    if(date_cut_point.compareTo(signindatetime) > 0){
      //如果归属大于签到时间，小于签退时间，表示归属之前是属于前一天的，归属之后的是属于今天的
      //不开启了归属的时候，根据打卡的性质来判断，如果是签到卡，那么签到时间到前一天上班时间，这段区间内算是前一天的加班卡，和开启了归属不一样
      KQOvertimeCardBean preKqOvertimeBean = new KQOvertimeCardBean();
      preKqOvertimeBean.setSigninDate(signindate);
      preKqOvertimeBean.setSigninTime(signintime);
      preKqOvertimeBean.setSignoutDate(splitDate);
      preKqOvertimeBean.setSignoutTime(overtime_cut_points);
      preKqOvertimeBean.setBelongDate(preDate);
      preKqOvertimeBean.setHas_cut_point("0");
      lsCheckInfoMaps.put(preKey, preKqOvertimeBean);

      if(signoutdatetime.length() > 0 && signoutdatetime.compareTo(date_cut_point) > 0){
        KQOvertimeCardBean curOvertimeBean = new KQOvertimeCardBean();
        curOvertimeBean.setSigninDate(splitDate);
        curOvertimeBean.setSigninTime(overtime_cut_points);
        curOvertimeBean.setSignoutDate(signoutdate);
        curOvertimeBean.setSignoutTime(signouttime);
        curOvertimeBean.setBelongDate(splitDate);
        curOvertimeBean.setHas_cut_point("0");
        lsCheckInfoMaps.put(key, curOvertimeBean);
      }
    }else if(signoutdatetime.length() > 0 && date_cut_point.compareTo(signoutdatetime) > 0){
      //如果归属大于签退时间 表示这个时间都是属于前一天的
      lsCheckInfoMaps.put(preKey, kqOvertimeBean);
    }else{
      //如果归属 小于签到时间，则都属于今天
      lsCheckInfoMaps.put(key, kqOvertimeBean);
    }
  }


  /**
   * 获取加班流程数据
   * @param resourceid
   * @param fromDate
   * @param toDate
   * @param splitBeanMaps
   * @param dateFormatter
   */
  private void getOverTimeFlowData(String resourceid, String fromDate, String toDate,
      Map<String, List<SplitBean>> splitBeanMaps,
      DateTimeFormatter dateFormatter) {
    LocalDate localFromDate = LocalDate.parse(fromDate);
    LocalDate preFromDate = localFromDate.minusDays(1);
    LocalDate localToDate = LocalDate.parse(toDate);
    long betweenDays = localToDate.toEpochDay() - preFromDate.toEpochDay();
    for (int k = 0; k <= betweenDays; k++) {
      LocalDate curLocalDate = preFromDate.plusDays(k);
      String splitDate = curLocalDate.format(dateFormatter);
      String key = resourceid+"_"+splitDate;
      String order_sql = " order by belongdate,fromtime ";
      KQFlowDataBiz kqFlowDataBiz = new KQFlowDataBiz.FlowDataParamBuilder().belongDateParam(splitDate).resourceidParam(resourceid).orderby_sqlParam(order_sql).build();
      Map<String,Object> flowMaps = Maps.newHashMap();
      List<SplitBean> splitBeans = kqFlowDataBiz.getOverTimeData(flowMaps);
      if(!splitBeans.isEmpty()){
        splitBeanMaps.put(key, splitBeans);
      }
    }
  }

  /**
   * 获取非工作日的打卡数据
   * @param resourceid
   * @param splitDate
   * @param preDate
   * @param nextDate
   * @param curKqOvertimeRulesDetailEntity
   * @param eventMap
   * @param preKqOvertimeRulesDetailEntity
   * @return
   */
  private Map<String, Object> getRestSignInfo(String resourceid, String splitDate, String preDate,
      String nextDate,
      KQOvertimeRulesDetailEntity curKqOvertimeRulesDetailEntity,
      Map<String, Object> eventMap,
      KQOvertimeRulesDetailEntity preKqOvertimeRulesDetailEntity) {
    Map<String, Object> signMap = Maps.newHashMap();
    WorkTimeEntity pre_workTime = new KQWorkTime().getWorkTime(resourceid, preDate);
    List<TimeScopeEntity> pre_lsSignTime = new ArrayList<>();

    if (pre_workTime != null) {
      pre_lsSignTime = pre_workTime.getSignTime();//允许打卡时间
      pre_lsSignTime = pre_lsSignTime != null ? pre_lsSignTime : new ArrayList<>();
    }
    WorkTimeEntity next_workTime = new KQWorkTime().getWorkTime(resourceid, nextDate);
    List<TimeScopeEntity> next_lsSignTime = new ArrayList<>();

    if (next_workTime != null) {
      next_lsSignTime = next_workTime.getSignTime();//允许打卡时间
      next_lsSignTime = next_lsSignTime != null ? next_lsSignTime : new ArrayList<>();
    }

    signMap = getNonWorkSignInfo(resourceid,nextDate,splitDate,pre_lsSignTime,next_lsSignTime,curKqOvertimeRulesDetailEntity,eventMap,preKqOvertimeRulesDetailEntity);

    return signMap;
  }


  /**
   * 获取非工作日的打卡数据 封装处理下
   * @param resourceid
   * @param nextDate
   * @param kqDate
   * @param pre_lsSignTime
   * @param next_lsSignTime
   * @param curKqOvertimeRulesDetailEntity
   * @param eventMap
   * @param preKqOvertimeRulesDetailEntity
   * @return
   */
  public Map<String, Object> getNonWorkSignInfo(String resourceid, String nextDate, String kqDate,
      List<TimeScopeEntity> pre_lsSignTime,
      List<TimeScopeEntity> next_lsSignTime,
      KQOvertimeRulesDetailEntity curKqOvertimeRulesDetailEntity,
      Map<String, Object> eventMap,
      KQOvertimeRulesDetailEntity preKqOvertimeRulesDetailEntity) {
    Map<String, Object> signMap = Maps.newHashMap();
    KQFormatSignData kqFormatSignData = new KQFormatSignData();
    List<Object> lsCheckInfo = new ArrayList<>();
    Map<String, Object> checkInfo = null;
    RecordSet rs = new RecordSet();
    String pre_Worktime4Today = "";
    if(!pre_lsSignTime.isEmpty()){
      TimeScopeEntity pre_signTimeScope = pre_lsSignTime.get(pre_lsSignTime.size()-1);
      if(pre_signTimeScope.getEndTimeAcross()){
        pre_Worktime4Today = pre_signTimeScope.getEndTime();
      }
    }
    String next_Worktime4Today = "";
    if(!next_lsSignTime.isEmpty()){
      TimeScopeEntity next_signTimeScope = next_lsSignTime.get(next_lsSignTime.size()-1);
      if(next_signTimeScope.isBeginTimePreAcross()){
        next_Worktime4Today = next_signTimeScope.getBeginTime();
      }
    }
    String signBeginDateTime = kqDate+" 00:00:00";
    String signEndDateTime = kqDate+" 23:59:59";
    //有这么一种情况，比如前一天是工作日，最晚签退是2点，然后工作日的归属是设置的5点，那么如果4点打一个卡的话，其实是需要生成加班的，因为4带你这个卡是昨日的签退
    String ore_signBeginDateTime = signBeginDateTime;
    String base_sql = kqFormatSignData.signSignSql(rs);

    int pre_has_cut_point = preKqOvertimeRulesDetailEntity != null ? preKqOvertimeRulesDetailEntity.getHas_cut_point() : 0;

    if(pre_Worktime4Today.length() > 0){
      if(pre_has_cut_point == 1){
        String cut_point = Util.null2String(preKqOvertimeRulesDetailEntity.getCut_point());
        if(cut_point.length() > 0){
          if(cut_point.compareTo(pre_Worktime4Today) > 0){
            pre_Worktime4Today = cut_point;
          }else{
            if(pre_Worktime4Today.length() == 5){
              pre_Worktime4Today += ":00";
            }
            ore_signBeginDateTime = kqDate+" "+pre_Worktime4Today;
          }
          /**
           打卡数据：2022/04/29 09:00:00----2022/04/29 23:59:59---2022/04/30 00:00:00----2022/04/30 02:00:00
           加班流程：2022-04-29 20:00---2022-04-30 03:00
           加班规则：2022-04-29周五工作日自定义跨天归属设置为00:00，取交集，然后30号的00:00-02:00两个小时加班无法生成
           问题原因：当29的下班卡结束打卡时间在自定义跨天00:00之后，那么读卡范围是：29号结束打卡时间（pre_Worktime4Today）开始的
           */
          pre_Worktime4Today = cut_point+":01";
        }
      }
      if(pre_Worktime4Today.length() == 5){
        pre_Worktime4Today += ":00";
      }
      signBeginDateTime = kqDate+" "+pre_Worktime4Today;
    }else{
      if(pre_has_cut_point == 1){
        String cut_point = Util.null2String(preKqOvertimeRulesDetailEntity.getCut_point());
        if(cut_point.length() > 0){
          String cut_point_time = kqDate+" "+cut_point+":00";
          signBeginDateTime = cut_point_time;
        }
      }
    }
    if(next_Worktime4Today.length() > 0){
      if(next_Worktime4Today.length() == 5){
        next_Worktime4Today += ":00";
      }
      signEndDateTime = kqDate+" "+next_Worktime4Today;
    }else{
      if(curKqOvertimeRulesDetailEntity != null){
        int cur_has_cut_point = curKqOvertimeRulesDetailEntity.getHas_cut_point();
        if(cur_has_cut_point == 1){
          String cut_point = Util.null2String(curKqOvertimeRulesDetailEntity.getCut_point());
          if(cut_point.length() > 0){
            String cut_point_time = nextDate+" "+cut_point+":00";
            signEndDateTime = cut_point_time;
          }
        }
      }
    }
    String sql = "select * from ("+base_sql+") a "+" order by signdate asc,signtime asc ";
    rs.executeQuery(sql,resourceid,signBeginDateTime,signEndDateTime);
    String nonwork_card_sql = "sql:"+sql+"|resourceid|"+resourceid+"|signBeginDateTime|"+signBeginDateTime+"|signEndDateTime|"+signEndDateTime;
    eventMap.put("非工作日打卡sql|nonwork_card_sql", nonwork_card_sql);
    eventMap.put("非工作日打卡sql结果|nonwork_card_sql_getCounts", rs.getCounts());
    int idx = 0;
    while (rs.next()){
      String signId = Util.null2String(rs.getString("id"));
      String signdate = Util.null2String(rs.getString("signdate"));
      String signtime = Util.null2String(rs.getString("signtime"));

      checkInfo = new HashMap<>();
      checkInfo.put("signId", signId);//签到签退标识
      checkInfo.put("signDate", signdate);//签到签退日期
      checkInfo.put("signTime", signtime);//签到签退时间
      idx++;
      if(idx==1){//第一条算签到
        checkInfo.put("signType", "1");
        lsCheckInfo.add(checkInfo);
      }else if(idx==rs.getCounts()){//最后一条算签退
        checkInfo.put("signType", "2");
        lsCheckInfo.add(checkInfo);
      }
    }
    if(lsCheckInfo != null && !lsCheckInfo.isEmpty()){
      if(lsCheckInfo.size() == 2){
        Map<String, Object> begin_signMap = (Map<String, Object>) lsCheckInfo.get(0);
        Map<String, Object> end_signMap = (Map<String, Object>) lsCheckInfo.get(lsCheckInfo.size()-1);
        signMap.put("signindate", begin_signMap.get("signDate"));
        signMap.put("signintime", begin_signMap.get("signTime"));
        signMap.put("signoutdate", end_signMap.get("signDate"));
        signMap.put("signouttime", end_signMap.get("signTime"));
      }else if(lsCheckInfo.size() == 1){
        Map<String, Object> begin_signMap = (Map<String, Object>) lsCheckInfo.get(0);
        signMap.put("signindate", begin_signMap.get("signDate"));
        signMap.put("signintime", begin_signMap.get("signTime"));
      }
    }else{
      if(pre_has_cut_point == 1){
        sql = "select * from ("+base_sql+") a "+" order by signdate asc,signtime asc ";
        rs.executeQuery(sql,resourceid,ore_signBeginDateTime,signEndDateTime);
        nonwork_card_sql = "sql:"+sql+"|resourceid|"+resourceid+"|ore_signBeginDateTime|"+ore_signBeginDateTime+"|signEndDateTime|"+signEndDateTime;
        eventMap.put("昨日开启了打卡归属，非工作日打卡sql|nonwork_card_sql", nonwork_card_sql);
        eventMap.put("昨日开启了打卡归属，非工作日打卡sql结果|nonwork_card_sql_getCounts", rs.getCounts());
        idx = 0;
        while (rs.next()){
          String signId = Util.null2String(rs.getString("id"));
          String signdate = Util.null2String(rs.getString("signdate"));
          String signtime = Util.null2String(rs.getString("signtime"));

          checkInfo = new HashMap<>();
          checkInfo.put("signId", signId);//签到签退标识
          checkInfo.put("signDate", signdate);//签到签退日期
          checkInfo.put("signTime", signtime);//签到签退时间
          idx++;
          if(idx==1){//第一条算签到
            checkInfo.put("signType", "1");
            lsCheckInfo.add(checkInfo);
          }else if(idx==rs.getCounts()){//最后一条算签退
            checkInfo.put("signType", "2");
            lsCheckInfo.add(checkInfo);
          }
        }
        if(lsCheckInfo != null && !lsCheckInfo.isEmpty()){
          //这种情况下，就只是把签到卡传回去作为前一天的签退了
          Map<String, Object> begin_signMap = (Map<String, Object>) lsCheckInfo.get(0);
          signMap.put("signindate", begin_signMap.get("signDate"));
          signMap.put("signintime", begin_signMap.get("signTime"));
        }
      }
    }
    return signMap;
  }
  /**
   * 获取工作日的打卡数据
   * @param userId
   * @param signTimeScope
   * @param workTimeScope
   * @param kqDate
   * @param preDate
   * @param nextDate
   * @param kqTimesArrayComInfo
   * @param eventMap
   * @param index
   * @param result
   * @return
   */
  public Map<String, Object> getSignInfo(String userId, TimeScopeEntity signTimeScope,
      TimeScopeEntity workTimeScope, String kqDate, String preDate,
      String nextDate, KQTimesArrayComInfo kqTimesArrayComInfo,Map<String, Object> eventMap,
      int index,Map<String,String> result) {
    Map<String, Object> signMap = Maps.newHashMap();
    KQFormatSignData kqFormatSignData = new KQFormatSignData();
    List<Object> lsCheckInfo = new ArrayList<>();
    Map<String, Object> checkInfo = null;
    String base_sql = "";
    RecordSet rs = new RecordSet();
    String dbtype = rs.getDBType();

    int has_cut_point = Util.getIntValue(Util.null2String(result.get("has_cut_point")));
    String cut_point = Util.null2String(result.get("cut_point"));

    int pre_has_cut_point = Util.getIntValue(Util.null2String(result.get("pre_has_cut_point")));
    String pre_cut_point = Util.null2String(result.get("pre_cut_point"));
    int lsWorkTime_size = Util.getIntValue(Util.null2String(result.get("lsWorkTime_size")));

    //流程抵扣打卡不处理
    Map<String,String> flow_deduct_card_map = Maps.newHashMap();

    List<Map<String,String>> sqlConditions = kqFormatSignData.getCanSignInfo(signTimeScope,kqDate,preDate,nextDate,kqTimesArrayComInfo);
    base_sql = kqFormatSignData.signSignSql(rs);

    if(sqlConditions != null && !sqlConditions.isEmpty()){
      for (int i = 0; i < sqlConditions.size(); i++) {
        Map<String,String> sqlMap = sqlConditions.get(i);
        String sql = "";
        String orderSql = "";
        int idx = 0;
        String signBeginDateTime = Util.null2String(sqlMap.get("signBeginDateTime"));
        String signEndDateTime = Util.null2String(sqlMap.get("signEndDateTime"));
        if(index == lsWorkTime_size-1 && i == sqlConditions.size()-1){
          //最后一次的打卡范围会被打卡临界点给修改
          if(has_cut_point == 1){
            String cut_point_datettime = nextDate+" "+cut_point+":59";
            signEndDateTime = cut_point_datettime;
          }
        }
        if(index == 0 && i == 0){
          //第一次的打卡范围会被打卡临界点给修改
          if(pre_has_cut_point == 1){
            String cut_point_datettime = kqDate+" "+pre_cut_point+":00";
            signBeginDateTime = cut_point_datettime;
          }
        }
        String type = Util.null2String(sqlMap.get("type"));
        if(type.length() > 0){
          if("signoff".equalsIgnoreCase(type)){
            orderSql = " order by signdate desc, signtime desc ";
          }else if("signin".equalsIgnoreCase(type)){
            orderSql = " order by signdate asc, signtime asc ";
          }
          if("oracle".equalsIgnoreCase(dbtype)){
            sql = "select * from ("+base_sql+" "+orderSql+") a where rownum=1";
          }else if("mysql".equalsIgnoreCase(dbtype)){
            sql = "select * from ("+base_sql+" "+orderSql+") a limit 0,1";
          }
          else if("postgresql".equalsIgnoreCase(dbtype)){
            sql = "select * from ("+base_sql+" "+orderSql+") a limit 1 offset 0";
          }
          else if("sqlserver".equalsIgnoreCase(dbtype)){
            sql = "select top 1 * from ("+base_sql+") a "+" "+orderSql;
          }else{
            sql = "select * from ("+base_sql+" "+orderSql+") a where rownum=1";
          }
        }else{
          orderSql = " order by signdate asc, signtime asc ";
          sql = base_sql+" "+orderSql;
        }
        rs.executeQuery(sql, userId, signBeginDateTime, signEndDateTime);
        String work_card_sql = "index:"+index+":idx:"+idx+"|sql:"+sql+"|resourceid|"+userId+"|signBeginDateTime|"+signBeginDateTime+"|signEndDateTime|"+signEndDateTime;

        eventMap.put("工作日打卡sql|work_card_sql_"+index, work_card_sql);
        eventMap.put("工作日打卡sql结果|work_card_sql_getCounts_"+index, rs.getCounts());
        while (rs.next()) {
          String signId = Util.null2String(rs.getString("id"));
          String signdate = Util.null2String(rs.getString("signdate"));
          String signtime = Util.null2String(rs.getString("signtime"));

          checkInfo = new HashMap<>();
          checkInfo.put("signId", signId);//签到签退标识
          checkInfo.put("signDate", signdate);//签到签退日期
          checkInfo.put("signTime", signtime);//签到签退时间
          checkInfo.put("type", type);//是否有打卡归属,type有值就是有打卡归属
          idx++;
          if(type.length() > 0){
            if("signin".equalsIgnoreCase(type)){
              checkInfo.put("signType", "1");
              lsCheckInfo.add(checkInfo);
            }else {
              checkInfo.put("signType", "2");
              lsCheckInfo.add(checkInfo);
            }
          }else{
            if(idx==1){//第一条算签到
              checkInfo.put("signType", "1");
              lsCheckInfo.add(checkInfo);
            }else if(idx==rs.getCounts()){//最后一条算签退
              checkInfo.put("signType", "2");
              lsCheckInfo.add(checkInfo);
            }
          }
        }
      }
    }
    if(lsCheckInfo != null && !lsCheckInfo.isEmpty()){
      if(lsCheckInfo.size() == 1){
        Map<String, Object> checkInfoMap = (Map<String, Object>) lsCheckInfo.get(0);
        String type = Util.null2String(checkInfoMap.get("type"));
        if("signin".equalsIgnoreCase(type)){
          signMap.put("signindate", checkInfoMap.get("signDate"));
          signMap.put("signintime", checkInfoMap.get("signTime"));
          signMap.put("signoutdate", workTimeScope.getBeginTimeAcross()?nextDate:kqDate);
          signMap.put("signouttime", workTimeScope.getBeginTime()+":00");
        }else if("signoff".equalsIgnoreCase(type)){
          signMap.put("signindate", workTimeScope.getEndTimeAcross()?nextDate:kqDate);
          signMap.put("signintime", workTimeScope.getEndTime()+":00");
          signMap.put("signoutdate", checkInfoMap.get("signDate"));
          signMap.put("signouttime", checkInfoMap.get("signTime"));
        }else{
          signMap.put("signindate", checkInfoMap.get("signDate"));
          signMap.put("signintime", checkInfoMap.get("signTime"));
          signMap.put("signoutdate", workTimeScope.getBeginTimeAcross()?nextDate:kqDate);
          signMap.put("signouttime", workTimeScope.getBeginTime()+":00");
        }
      }else{
        Map<String, Object> begin_signMap = (Map<String, Object>) lsCheckInfo.get(0);
        Map<String, Object> end_signMap = (Map<String, Object>) lsCheckInfo.get(lsCheckInfo.size()-1);
        signMap.put("signindate", begin_signMap.get("signDate"));
        signMap.put("signintime", begin_signMap.get("signTime"));
        signMap.put("signoutdate", end_signMap.get("signDate"));
        signMap.put("signouttime", end_signMap.get("signTime"));
      }
    }
    return signMap;
  }

  /**
   * 获取已经生成过的加班数据
   * @param resourceid
   * @param belongdate
   * @param hasOverTimeList
   */
  private void getHasOverTimeData(String resourceid, String belongdate,List<Map<String,String>> hasOverTimeList) {
    RecordSet rs = new RecordSet();

    String sql = " select * from kq_flow_overtime where resourceid = ? and belongdate = ? ";
    rs.executeQuery(sql,resourceid,belongdate);
    while (rs.next()){
      String requestid =rs.getString("requestid");
      String fromdate =rs.getString("fromdate");
      String fromtime =rs.getString("fromtime");
      String todate =rs.getString("todate");
      String totime =rs.getString("totime");
      String duration_min =Util.null2String(rs.getString("duration_min"));
      String flow_dataid =Util.null2String(rs.getString("flow_dataid"));
      String ori_belongdate =Util.null2String(rs.getString("ori_belongdate"));
      //流程+打卡的时候，存的对应的流程数据
      String fromdate_flow =rs.getString("fromdatedb");
      String fromtime_flow =rs.getString("fromtimedb");
      String todate_flow =rs.getString("todatedb");
      String totime_flow =rs.getString("totimedb");
      Map<String,String> hasOverTimeMap = Maps.newHashMap();
      hasOverTimeMap.put("resourceid", resourceid);
      hasOverTimeMap.put("belongdate", belongdate);
      hasOverTimeMap.put("requestid", requestid);
      hasOverTimeMap.put("fromdate", fromdate);
      hasOverTimeMap.put("fromtime", fromtime);
      hasOverTimeMap.put("todate", todate);
      hasOverTimeMap.put("totime", totime);
      hasOverTimeMap.put("fromdate_flow", fromdate_flow);
      hasOverTimeMap.put("fromtime_flow", fromtime_flow);
      hasOverTimeMap.put("todate_flow", todate_flow);
      hasOverTimeMap.put("totime_flow", totime_flow);
      hasOverTimeMap.put("duration_min", duration_min);
      hasOverTimeMap.put("flow_dataid", flow_dataid);
      hasOverTimeMap.put("ori_belongdate", ori_belongdate);

      hasOverTimeList.add(hasOverTimeMap);
    }
  }

  /**
   * 根据人和日期获取加班规则里的信息
   * @param resourceid
   * @param fromDate
   * @param toDate
   * @param dateFormatter
   * @param changeTypeMap
   * @param overRulesDetailMap
   * @param restTimeMap
   * @param computingModeMap
   */
  public void getOverTimeDataMap(String resourceid, String fromDate, String toDate,
      DateTimeFormatter dateFormatter, Map<String, Integer> changeTypeMap,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,Map<String,List<String[]>> restTimeMap,Map<String,Integer> computingModeMap){

    LocalDate localFromDate = LocalDate.parse(fromDate);
    LocalDate localToDate = LocalDate.parse(toDate);
    LocalDate preFromDate = localFromDate.minusDays(2);
    LocalDate nextToDate = localToDate.plusDays(1);
    long betweenDays = nextToDate.toEpochDay() - preFromDate.toEpochDay();
    for (int i = 0; i <= betweenDays; i++) {
      LocalDate curLocalDate = preFromDate.plusDays(i);
      String splitDate = curLocalDate.format(dateFormatter);
      KQOvertimeRulesBiz.getOverTimeData(resourceid, splitDate,changeTypeMap,overRulesDetailMap,restTimeMap,computingModeMap);
    }
  }

  public void logOvertimeMap(Map<String, Object> overtimeLogMap,Object params, String keys){
    if(overtimeLogMap != null){
      overtimeLogMap.put(keys, params);
    }
  }

  /**
   * 根据加班规则设置，分段获取每一段的加班时长
   * @param resourceId
   * @param fromdate
   * @param fromtime
   * @param todate
   * @param totime
   * @param changeTypeMap
   * @param kqOvertimeRulesDetailEntity
   * @param splitBean
   * @param restTimeMap
   * @param overRulesDetailMap
   */
  public List<OvertimeBalanceTimeBean> getOvertimeBalanceTimeBean(String resourceId,
      String fromdate, String fromtime, String todate, String totime,
      Map<String, Integer> changeTypeMap,
      KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity, SplitBean splitBean,
      Map<String, List<String[]>> restTimeMap,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap){
    KQOverTimeFlowBiz kqOverTimeFlowBiz = new KQOverTimeFlowBiz();
    KQTimesArrayComInfo kqTimesArrayComInfo= new KQTimesArrayComInfo();
    KQOverTimeRuleCalBiz kqOverTimeRuleCalBiz = new KQOverTimeRuleCalBiz();
    int[] initArrays = kqTimesArrayComInfo.getInitArr();
    boolean shouldAcross = false;
    int before_startTime = -1;
    int startTime = 0;
    String change_key = fromdate+"_"+resourceId;
    int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
    String changeType_key = fromdate+"_"+changeType;

    String nextSplitDate = DateUtil.addDate(fromdate,1);
    String next_change_key = nextSplitDate+"_"+resourceId;
    int next_changeType = Util.getIntValue(""+changeTypeMap.get(next_change_key),-1);
    String nextChangeType_key = nextSplitDate+"_"+next_changeType;

    String next_beginwork_time = "";
    String serialid = "";
    //需要知道明日的类型:如果今天是工作日的话，那么今天的加班临界点可能和明日的上班时间冲突，需要知道明日的上班时间进行比较，
    // 如果今天是休息日，那么明天如果是工作日的话，默认规则下，明天的上班前都是属于今天的加班区间
    if(next_changeType == 2){
      ShiftInfoBean next_shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceId, nextSplitDate, false);
      if(next_shiftInfoBean != null){
        List<int[]> workLongTimeIndex = next_shiftInfoBean.getWorkLongTimeIndex();
        List<int[]> real_workLongTimeIndex = Lists.newArrayList();
        kqOverTimeFlowBiz.get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,next_shiftInfoBean,kqTimesArrayComInfo,splitBean);

        if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
          next_beginwork_time = kqTimesArrayComInfo.getTimesByArrayindex(real_workLongTimeIndex.get(0)[0]);
        }
      }
    }

    int fromTime_index = kqTimesArrayComInfo.getArrayindexByTimes(fromtime);
    int toTime_index = kqTimesArrayComInfo.getArrayindexByTimes(totime);
    Arrays.fill(initArrays, fromTime_index, toTime_index, 0);
    if(changeType == 1){
      kqOverTimeFlowBiz.handle_changeType_1(initArrays, overRulesDetailMap, nextChangeType_key,
          next_changeType, next_beginwork_time);
    }else if(changeType == 2){
      if(kqOvertimeRulesDetailEntity != null){
        before_startTime = kqOvertimeRulesDetailEntity.getBefore_startTime();
        int has_cut_point = kqOvertimeRulesDetailEntity.getHas_cut_point();
        if(has_cut_point != 1){
          before_startTime = -1;
        }
        startTime = kqOvertimeRulesDetailEntity.getStartTime();
      }
      boolean isok = kqOverTimeFlowBiz.handle_changeType_2(initArrays, resourceId, fromdate, before_startTime, startTime, fromTime_index,
          kqTimesArrayComInfo, splitBean,toTime_index);
    }else if(changeType == 3){
      kqOverTimeFlowBiz.handle_changeType_3(initArrays, overRulesDetailMap, nextChangeType_key,
          next_changeType, next_beginwork_time);
    }
    int restTimeType = -1;
    if(kqOvertimeRulesDetailEntity != null){
      restTimeType = kqOvertimeRulesDetailEntity.getRestTimeType();
    }
    if(restTimeType == 1){
      //如果排除设置的休息时间
      kqOverTimeFlowBiz.handle_resttime(restTimeMap,changeType_key,kqTimesArrayComInfo,shouldAcross,initArrays);
    }
    //        按照加班时长转调休的 时长设置
    List<String> timepointList = null;
    List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = Lists.newArrayList();
    if(kqOvertimeRulesDetailEntity != null){
      timepointList = kqOverTimeRuleCalBiz.get_timepointList(kqOvertimeRulesDetailEntity);
    }

    if(timepointList != null && !timepointList.isEmpty()){
      kqOverTimeRuleCalBiz.get_overtimeBalanceTimeBeans(timepointList,overtimeBalanceTimeBeans,kqTimesArrayComInfo,initArrays,toTime_index,fromTime_index,0);
    }

    return overtimeBalanceTimeBeans;
  }

  /**
   * 判断是否开启了调休的 按照时间点分段生成调休
   * @param kqOvertimeRulesDetailEntity
   * @param paidLeaveEnable
   * @return
   */
  public boolean getNeedSplitByTime(KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity,
      int paidLeaveEnable) {
    //因为【按加班时长范围设置转调休时长问题】这个方式存在问题，暂不使用
    if(true){
      return false;
    }
    boolean needSplitByTime = false;
    if(paidLeaveEnable == 1){
      int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
      //如果开启了转调休，并且调休是分段折算的，可以按照分段来显示加班
      if(1 == paidLeaveEnableType){
        int paidLeaveEnableDefaultType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableDefaultType();
        if(paidLeaveEnableDefaultType == 3){
          needSplitByTime = true;
        }
      }else if(2 == paidLeaveEnableType){
        int paidLeaveEnableFlowType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableFlowType();
        if(paidLeaveEnableFlowType == 3){
          needSplitByTime = true;
        }
      }
    }
    return needSplitByTime;
  }

  /**
   * 判断是否开启了调休
   * @param kqOvertimeRulesDetailEntity
   * @param overtime_type
   * @return
   */
  public int getPaidLeaveEnable(KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity,
      String overtime_type) {
    int paidLeaveEnable = -1;
    if (kqOvertimeRulesDetailEntity != null){
      paidLeaveEnable = kqOvertimeRulesDetailEntity.getPaidLeaveEnable();
      paidLeaveEnable = paidLeaveEnable == 1?1:0;
    }
    int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
    if(2 == paidLeaveEnableType){
//      logOvertimeMap(overtimeLogMap, overtime_type, flow_cross_key+"|关联调休与否来自于流程选择,加班类型下拉框值|overtime_type");
      if("0".equalsIgnoreCase(overtime_type)){
        paidLeaveEnable = 1;
      }else if("1".equalsIgnoreCase(overtime_type)){
        paidLeaveEnable = 0;
      }else{
        paidLeaveEnable = 0;
      }
    }
    return paidLeaveEnable;
  }


  /**
   * 清掉之前的加班和调休，重新生成
   * @param resourceid
   * @param belongdate
   * @param overtimeLogMap
   * @param splitDate
   */
  public void clearOvertimeTX(String resourceid, String belongdate,
      Map<String, Object> overtimeLogMap, String splitDate) {
    List<String>  all_tiaoxiuidList = Lists.newArrayList();
    String all_tiaoxiuids = "";
    RecordSet rs = new RecordSet();
    String sql = "select * from kq_flow_overtime where resourceid = ? and belongdate=? and ori_belongdate=? ";
    rs.executeQuery(sql,resourceid, belongdate,splitDate);
    while (rs.next()){
      String tiaoxiuid = Util.null2String(rs.getString("tiaoxiuid"),"");
      if(tiaoxiuid.length() > 0 && Util.getIntValue(tiaoxiuid) > 0){
        all_tiaoxiuids += ","+tiaoxiuid;
        all_tiaoxiuidList.add(tiaoxiuid);
      }
    }
    logOvertimeMap(overtimeLogMap, all_tiaoxiuids, "需要重新生成的调休|all_tiaoxiuids");
    if(all_tiaoxiuids.length() > 0){
      all_tiaoxiuids = all_tiaoxiuids.substring(1);

      Map<String,String> tiaoxiuamountMap = Maps.newLinkedHashMap();
      String selSql = "select * from kq_balanceofleave where "+Util.getSubINClause(all_tiaoxiuids, "id", "in");
      rs.executeQuery(selSql);
      while (rs.next()){
        String id = rs.getString("id");
        String tiaoxiuamount = rs.getString("tiaoxiuamount");
        tiaoxiuamountMap.put(id, tiaoxiuamount);
      }
      if(!tiaoxiuamountMap.isEmpty()){
        logOvertimeMap(overtimeLogMap, tiaoxiuamountMap, "先记录下之前的加班生成的调休数据|tiaoxiuamountMap");
      }
      String tiaoxiuidis0 = "";
      String delSql0 = "select * from kq_balanceofleave where "+Util.getSubINClause(all_tiaoxiuids, "id", "in")+" and "
          + " baseamount =0 and extraamount=0 and usedamount=0 and baseamount2=0 and extraamount2=0 and usedamount2=0 ";
      if(rs.getDBType().equalsIgnoreCase("oracle")) {
        delSql0 = "select * from kq_balanceofleave where "+Util.getSubINClause(all_tiaoxiuids, "id", "in")+" and "
            + " nvl(baseamount,0) =0 and nvl(extraamount,0)=0 and nvl(usedamount,0)=0 and nvl(baseamount2,0)=0 "
            + " and nvl(extraamount2,0)=0 and nvl(usedamount2,0)=0 ";
      }else if((rs.getDBType()).equalsIgnoreCase("mysql")){
        delSql0 = "select * from kq_balanceofleave where "+Util.getSubINClause(all_tiaoxiuids, "id", "in")+" and "
            + " ifnull(baseamount,0) =0 and ifnull(extraamount,0)=0 and ifnull(usedamount,0)=0 and ifnull(baseamount2,0)=0 "
            + " and ifnull(extraamount2,0)=0 and ifnull(usedamount2,0)=0 ";
      }else {
        delSql0 = "select * from kq_balanceofleave where "+Util.getSubINClause(all_tiaoxiuids, "id", "in")+" and "
            + " isnull(baseamount,0) =0 and isnull(extraamount,0)=0 and isnull(usedamount,0)=0 and isnull(baseamount2,0)=0 "
            + " and isnull(extraamount2,0)=0 and isnull(usedamount2,0)=0 ";
      }
      rs.executeQuery(delSql0);
      while (rs.next()){
        String tiaoxiuid = Util.null2String(rs.getString("id"),"");
        if(tiaoxiuid.length() > 0 && Util.getIntValue(tiaoxiuid) > 0){
          tiaoxiuidis0 += ","+tiaoxiuid;
          all_tiaoxiuidList.remove(tiaoxiuid);
        }
      }
      String delSql = "";
      if(tiaoxiuidis0.length() > 0){
        delSql = "delete from kq_balanceofleave where "+Util.getSubINClause(tiaoxiuidis0, "id", "in");
        boolean flag= rs.executeUpdate(delSql);
        if (!flag) {
          kqLog.info("加班流程删除之前的调休数据失败：数据库更新失败" );
        }
      }
      String clearSql = "";
      boolean isclearOk = false;
      if(!all_tiaoxiuidList.isEmpty()){
        String clear_tiaoxiuids = all_tiaoxiuidList.stream().collect(Collectors.joining(","));
        clearSql = "update kq_balanceofleave set tiaoxiuamount=0.0 where "+Util.getSubINClause(clear_tiaoxiuids, "id", "in");
        isclearOk = rs.executeUpdate(clearSql);
      }

      String delUsageSql = "delete from kq_usagehistory where "+Util.getSubINClause(all_tiaoxiuids, "balanceofleaveid", "in");
      boolean isdelUsageOk = rs.executeUpdate(delUsageSql);
      Map<String,Object> logSqlMap = Maps.newLinkedHashMap();
      logSqlMap.put("tiaoxiuidis0",tiaoxiuidis0);
      logSqlMap.put("all_tiaoxiuidList",all_tiaoxiuidList);
      logSqlMap.put("delSql",delSql);
      logSqlMap.put("clearSql",clearSql);
      logSqlMap.put("isclearOk",isclearOk);
      logSqlMap.put("delUsageSql",delUsageSql);
      logSqlMap.put("isdelUsageOk",isdelUsageOk);
      logOvertimeMap(overtimeLogMap, logSqlMap, "需要重新生成的调休对应的信息|logSqlMap");
    }
    String delSql = "delete from kq_flow_overtime where resourceid = ? and belongdate=?  and ori_belongdate=? ";
    boolean isDelOk = rs.executeUpdate(delSql,resourceid, belongdate,splitDate);
    String delSqlLog = delSql+":resourceid:"+resourceid+":belongdate:"+belongdate+":splitDate:"+splitDate+":isDelOk:"+isDelOk;
    logOvertimeMap(overtimeLogMap, delSqlLog, "删除加班中间表数据|delSql");

  }
  /**
   * 加班单位
   * @param d_mins
   * @return
   */
  public double getD_MinsByUnit(double d_mins) {
    Map<String,String> map = KQOvertimeRulesBiz.getMinimumUnitAndConversion();
    if(!map.isEmpty()){
      double conversionMins = 0.0;
      int minimumUnit = Util.getIntValue(Util.null2String(map.get("minimumUnit")),-1);
      int overtimeConversion = Util.getIntValue(Util.null2String(map.get("overtimeConversion")),-1);
      if(5 == minimumUnit || 6 == minimumUnit){
        int halfHourInt = 30;
        int wholeHourInt = 60;
        if(5 == minimumUnit){
          conversionMins = getConversionMins(halfHourInt,d_mins,overtimeConversion);
        }else {
          conversionMins = getConversionMins(wholeHourInt,d_mins,overtimeConversion);
        }
        return conversionMins;
      }
    }
    return d_mins;
  }

  /**
   * 根据转换规则得到转换后的加班时长
   * @param halfHourInt
   * @param d_mins
   * @param overtimeConversion
   * @return
   */
  public double getConversionMins(int halfHourInt, double d_mins, int overtimeConversion) {
    double conversionMins = 0.0;
    int step = (int) (d_mins/halfHourInt);
    double leftMins = d_mins - halfHourInt*step;
    //半小时
    if(1 == overtimeConversion){
//          四舍五入
      if(leftMins >= halfHourInt/2){
        conversionMins = halfHourInt*step+halfHourInt;
      }else{
        conversionMins = halfHourInt*step;
      }
    }else if(2 == overtimeConversion){
//          向上取整
      if(leftMins > 0){
        conversionMins = halfHourInt*step+halfHourInt;
      }else{
        conversionMins = halfHourInt*step;
      }
    }else if(3 == overtimeConversion){
//          向下取整
      if(leftMins < halfHourInt){
        conversionMins = halfHourInt*step;
      }else{
        conversionMins = halfHourInt*step;
      }
    }
    return conversionMins;
  }
}
