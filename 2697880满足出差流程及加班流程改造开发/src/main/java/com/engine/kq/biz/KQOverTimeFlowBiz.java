package com.engine.kq.biz;

import com.alibaba.fastjson.JSON;
import com.api.qc2697880.util.CustomUtil;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.KQOvertimeRulesDetailEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.wfset.bean.OvertimeBalanceTimeBean;
import com.engine.kq.wfset.bean.SplitBean;
import com.engine.kq.wfset.util.KQFlowUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import weaver.general.BaseBean;
import weaver.general.Util;

/**
 * 加班流程 非工作时长计算 重新搞一把
 * 现在是根据归属来拆分流程
 */
public class KQOverTimeFlowBiz {
  private KQLog kqLog = new KQLog();

  /**
   * 之前的加班拆分是根据每一天进行拆分的，但是我们是有个默认归属的，就是上班前是属于前一天的，再加上这次标准改造，加了临界点，
   * 加班的拆分改造一下，改成和请假拆分一样，根据归属区间来拆分
   * 未设置加班归属的逻辑是
   * 工作日A-工作日B ，从工作日A的上班时间往后加24小时，都算工作日A的加班区间
   * 工作日A-非工作日，从工作日A的上班时间往后加24小时，都算工作日A的加班区间
   * 非工作日-工作日A，从非工作日的0点到工作日A的上班前，都属于非工作日的区间
   *
   * 计算加班逻辑很特殊，
   * 需要知道昨日，今日和明日的类型，是工作日还是非工作日
   * @param splitBean
   * @param splitBeans
   */
  public void getSplitDurationBean_new(SplitBean splitBean,List<SplitBean> splitBeans) {
    try{
      long a = System.currentTimeMillis();

      double oneDayHour = KQFlowUtil.getOneDayHour(splitBean.getDurationTypeEnum(),"");
      int workmins = (int)(oneDayHour * 60);
      String resourceid = splitBean.getResourceId();
      String fromDate = splitBean.getFromdatedb();
      String toDate = splitBean.getTodatedb();
      String overtime_type = splitBean.getOvertime_type();

      LocalDate localFromDate = LocalDate.parse(fromDate);
      LocalDate localToDate = LocalDate.parse(toDate);
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      LocalDate preFromDate = localFromDate.minusDays(1);
      KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

      KQOvertimeRulesBiz kqOvertimeRulesBiz = new KQOvertimeRulesBiz();
      KQOverTimeRuleCalBiz kqOverTimeRuleCalBiz = new KQOverTimeRuleCalBiz();
      Map<String,Integer> changeTypeMap = Maps.newHashMap();
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap = Maps.newHashMap();
      Map<String,List<String[]>> restTimeMap = Maps.newHashMap();
      Map<String,Integer> computingModeMap = Maps.newHashMap();
      kqOverTimeRuleCalBiz.getOverTimeDataMap(resourceid, fromDate, toDate, dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,computingModeMap);

      //==zj
      CustomUtil customUtil = new CustomUtil();

      if(overRulesDetailMap.isEmpty()){
        return;
      }
      String fromTime = splitBean.getFromtimedb();
      String toTime = splitBean.getTotimedb();
      long betweenDays = localToDate.toEpochDay() - preFromDate.toEpochDay();
      //默认是从加班开始日期的前一天开始计算 需要特殊处理的就三个情况，i=0的时候，i=1的时候就是加班流程开始日期那一天，i=最后一天就是加班流程结束日期那一天
      for (int i = 0; i <= betweenDays; i++) {
        SplitBean overSplitBean = new SplitBean();
        //然后把bean重新赋值下，根据拆分后的时间
        BeanUtils.copyProperties(splitBean, overSplitBean);

        //从加班流程开始的前一天开始算归属
        LocalDate curLocalDate = preFromDate.plusDays(i);
        String splitDate = curLocalDate.format(dateFormatter);
        String preSplitDate = LocalDate.parse(splitDate).minusDays(1).format(dateFormatter);
        LocalDate nextLocalDate = curLocalDate.plusDays(1);
        String nextSplitDate = nextLocalDate.format(dateFormatter);
        String change_key = splitDate+"_"+resourceid;
        String pre_change_key = preSplitDate+"_"+resourceid;
        String next_change_key = nextSplitDate+"_"+resourceid;
        int changeType = Util.getIntValue(""+changeTypeMap.get(change_key),-1);
        int preChangeType = Util.getIntValue(""+changeTypeMap.get(pre_change_key),-1);
        int next_changeType = Util.getIntValue(""+changeTypeMap.get(next_change_key),-1);

        boolean shouldAcross = false;
        String changeType_key = splitDate+"_"+changeType;
        String preChangeType_key = preSplitDate+"_"+preChangeType;
        String nextChangeType_key = nextSplitDate+"_"+next_changeType;
        if(!computingModeMap.containsKey(changeType_key)){
          continue;
        }
        int computingMode = computingModeMap.get(changeType_key);
        if(computingMode == 3){
          //如果是纯打卡为主的不生成加班
          continue;
        }

        int[] initArrays = kqTimesArrayComInfo.getInitArr();
        //当前日期的加班分割点 分割点都是次日的
        String overtime_cut_point = "";
        int before_startTime = 0;
        int startTime = 0;
        int curMins = 0 ;
        //排除休息类型
        int restTimeType = -1;
        String next_beginwork_time = "";
        String cur_beginwork_time = "";
        String serialid = "";
        //需要知道明日的类型:如果今天是工作日的话，那么今天的加班临界点可能和明日的上班时间冲突，需要知道明日的上班时间进行比较，
        // 如果今天是休息日，那么明天如果是工作日的话，默认规则下，明天的上班前都是属于今天的加班区间
        if(next_changeType == 2){
          ShiftInfoBean next_shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, nextSplitDate, false);
          if(next_shiftInfoBean != null){
            List<int[]> workLongTimeIndex = next_shiftInfoBean.getWorkLongTimeIndex();
            List<int[]> real_workLongTimeIndex = Lists.newArrayList();
            get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,next_shiftInfoBean,kqTimesArrayComInfo,splitBean);

            if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
              next_beginwork_time = kqTimesArrayComInfo.getTimesByArrayindex(real_workLongTimeIndex.get(0)[0]);
            }
          }
        }
        if(changeType == 2){
          ShiftInfoBean cur_shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, splitDate, false);
          if(cur_shiftInfoBean != null){
            List<int[]> workLongTimeIndex = cur_shiftInfoBean.getWorkLongTimeIndex();
            List<int[]> real_workLongTimeIndex = Lists.newArrayList();
            get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,cur_shiftInfoBean,kqTimesArrayComInfo,splitBean);

            if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
              cur_beginwork_time = kqTimesArrayComInfo.getTimesByArrayindex(real_workLongTimeIndex.get(0)[0]);
            }
          }
        }
        boolean needSplitByTime = false;
//        按照加班时长转调休的 时长设置
        List<String> timepointList = null;
        List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = Lists.newArrayList();
        KQOvertimeRulesDetailEntity curKqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
        if(curKqOvertimeRulesDetailEntity != null){
          int has_cut_point = curKqOvertimeRulesDetailEntity.getHas_cut_point();
          before_startTime = curKqOvertimeRulesDetailEntity.getBefore_startTime();
          int overtimeEnable = curKqOvertimeRulesDetailEntity.getOvertimeEnable();
          if(overtimeEnable != 1){
            continue;
          }
          if(has_cut_point != 1){
            before_startTime = -1;
          }
          startTime = curKqOvertimeRulesDetailEntity.getStartTime();
          restTimeType = curKqOvertimeRulesDetailEntity.getRestTimeType();
          int paidLeaveEnable = kqOverTimeRuleCalBiz.getPaidLeaveEnable(curKqOvertimeRulesDetailEntity,overtime_type);
          needSplitByTime = kqOverTimeRuleCalBiz.getNeedSplitByTime(curKqOvertimeRulesDetailEntity,paidLeaveEnable);
          if(needSplitByTime){
            int ruleDetailid = curKqOvertimeRulesDetailEntity.getId();
            Map<String,List<String>> balanceTimethDetailMap = kqOvertimeRulesBiz.getBalanceTimeDetailMap(ruleDetailid);
            if(balanceTimethDetailMap != null && !balanceTimethDetailMap.isEmpty()){
              timepointList = balanceTimethDetailMap.get("timepointList");
            }
          }
          if(has_cut_point == 0){
            if(changeType == 2){
              overtime_cut_point = cur_beginwork_time;
            }else {
              if(next_beginwork_time.length() > 0){
                overtime_cut_point = next_beginwork_time;
              }
            }
          }else{
            overtime_cut_point = curKqOvertimeRulesDetailEntity.getCut_point();
            if(next_beginwork_time.length() > 0){
              int next_beginwork_time_index = kqTimesArrayComInfo.getArrayindexByTimes(next_beginwork_time);
              int overtime_cut_point_index = kqTimesArrayComInfo.getArrayindexByTimes(overtime_cut_point);
              if(overtime_cut_point_index > next_beginwork_time_index){
                overtime_cut_point = next_beginwork_time;
              }
            }
          }
          if(overtime_cut_point.length() == 0){
            overtime_cut_point = "00:00";
          }
        }else{
          continue;
        }
        int fromTime_index = 0;
        int toTime_index = 0;
        if(i == 0){
          //i=0就是加班开始日期的前一天，只有当加班临界点超过了加班流程开始时间的话，i=0才会有可能计算出时长
          if(overtime_cut_point.compareTo(fromTime) > 0){
            fromTime_index = kqTimesArrayComInfo.getArrayindexByTimes(kqTimesArrayComInfo.turn24to48Time(fromTime));
            toTime_index = kqTimesArrayComInfo.getArrayindexByTimes(kqTimesArrayComInfo.turn24to48Time(overtime_cut_point));
            if(fromDate.equalsIgnoreCase(toDate)){
              //如果开始日期和结束日期是同一天，还需要比较流程的结束时间和归属点的大小
              int oriTotime_index = kqTimesArrayComInfo.getArrayindexByTimes(kqTimesArrayComInfo.turn24to48Time(toTime));
              if(toTime_index > oriTotime_index){
                toTime_index = oriTotime_index;
              }
            }
            Arrays.fill(initArrays, fromTime_index, toTime_index, 0);
//        1-节假日、2-工作日、3-休息日
            if(changeType == 1){
              handle_changeType_1(initArrays,overRulesDetailMap,nextChangeType_key,next_changeType,next_beginwork_time);
            }else if(changeType == 2){
              boolean isok = handle_changeType_2(initArrays, resourceid, splitDate, before_startTime, startTime, fromTime_index,kqTimesArrayComInfo,splitBean,
                  toTime_index);
              serialid = splitBean.getSerialid();
              if(!isok){
                continue;
              }
            }else if(changeType == 3){
              handle_changeType_3(initArrays,overRulesDetailMap,nextChangeType_key,next_changeType,next_beginwork_time);
            }
            if(restTimeType == 1){
              //==zj 如果排除设置的休息时间,如果“是否统计休息时长”开启，则统计休息时长，否则不统计
              if ("1".equals(overSplitBean.getIsOpenRest())){
                customUtil.handle_resttime_remove(restTimeMap,changeType_key,kqTimesArrayComInfo,shouldAcross,initArrays);
              }else {
                handle_resttime(restTimeMap,changeType_key,kqTimesArrayComInfo,shouldAcross,initArrays);
              }

            }
            curMins = kqTimesArrayComInfo.getCnt(initArrays, fromTime_index,toTime_index,0);
            if(restTimeType == 2){
              //如果排除休息时间是扣除时长
              curMins = handle_restlength(curMins,restTimeMap,changeType_key);
            }
          }else{
            continue;
          }
        }else{
          //除了i=0的情况，其他的每一天都是要获取一下昨日的临界点的
          String pre_overtime_cut_point = get_pre_overtime_cut_point(overRulesDetailMap,preChangeType_key,resourceid,preSplitDate,splitDate,preChangeType,kqTimesArrayComInfo,splitBean,changeType);
          if(changeType == 2){
            //如果今天是工作日，昨日的打卡归属会受到今日的上班前开始加班分钟数的影响
            int cur_beginwork_time_index = kqTimesArrayComInfo.getArrayindexByTimes(cur_beginwork_time);
            if(before_startTime > -1){
              int pre_overtime_cut_point_index = kqTimesArrayComInfo.getArrayindexByTimes(pre_overtime_cut_point);
              int before_cur_beginwork_time_index = cur_beginwork_time_index - before_startTime;
            }
          }
          //计算区间加班开始日期和加班结束日期这两天都是要特殊处理的
          fromTime_index = kqTimesArrayComInfo.getArrayindexByTimes(pre_overtime_cut_point);
          if(i == 1){
            if(fromTime.compareTo(pre_overtime_cut_point) > 0){
              fromTime_index = kqTimesArrayComInfo.getArrayindexByTimes(fromTime);
            }
          }
          if(i == betweenDays){
            toTime_index = kqTimesArrayComInfo.getArrayindexByTimes(toTime);
          }else{
            toTime_index = kqTimesArrayComInfo.turn24to48TimeIndex(kqTimesArrayComInfo.getArrayindexByTimes(overtime_cut_point));
            if(next_beginwork_time.length() > 0){
              int overtime_cut_point_index = kqTimesArrayComInfo.getArrayindexByTimes(overtime_cut_point);
              int next_beginwork_time_index = kqTimesArrayComInfo.getArrayindexByTimes(next_beginwork_time);
              //如果临界点都已经超过第二天上班的开始时间了，要相应的缩短成第二天上班时间
              if(overtime_cut_point_index > next_beginwork_time_index){
                toTime_index = kqTimesArrayComInfo.turn24to48TimeIndex(next_beginwork_time_index);
              }
            }
            if(i == betweenDays-1){
              int ori_totime_index = kqTimesArrayComInfo.turn48to24TimeIndex(toTime_index);
              int last_toTime_index = kqTimesArrayComInfo.getArrayindexByTimes(toTime);
              if(ori_totime_index > last_toTime_index){
                toTime_index = kqTimesArrayComInfo.turn24to48TimeIndex(last_toTime_index);
              }
            }
          }
          System.out.println(i+":betweenDays:"+betweenDays+":fromTime_index:"+fromTime_index+":toTime_index:"+toTime_index+":changeType:"+changeType);

          if(fromTime_index > toTime_index){
            continue;
          }
          Arrays.fill(initArrays, fromTime_index, toTime_index, 0);

          if(changeType == 1){
            handle_changeType_1(initArrays, overRulesDetailMap, nextChangeType_key, next_changeType,
                next_beginwork_time);
          }else if(changeType == 2){
            serialid = splitBean.getSerialid();
            boolean isok = handle_changeType_2(initArrays, resourceid, splitDate, before_startTime, startTime, fromTime_index,
                kqTimesArrayComInfo, splitBean, toTime_index);
            if(!isok){
              continue;
            }
          }else if(changeType == 3){
            handle_changeType_3(initArrays, overRulesDetailMap, nextChangeType_key, next_changeType,
                overtime_cut_point);
          }
          if(restTimeType == 1) {
            //==zj 如果排除设置的休息时间,如果“是否统计休息时长”开启，则统计休息时长，否则不统计
            if ("1".equals(overSplitBean.getIsOpenRest())){
              customUtil.handle_resttime_remove(restTimeMap,changeType_key,kqTimesArrayComInfo,shouldAcross,initArrays);
            }else {
              handle_resttime(restTimeMap,changeType_key,kqTimesArrayComInfo,shouldAcross,initArrays);
            }
          }
          curMins = kqTimesArrayComInfo.getCnt(initArrays, fromTime_index,toTime_index,0);

          if(restTimeType == 2){
            //如果排除休息时间是扣除时长
            curMins = handle_restlength(curMins,restTimeMap,changeType_key);
          }
        }
        int minimumUnit = curKqOvertimeRulesDetailEntity.getMinimumLen();
        if(curMins < minimumUnit){
          continue;
        }
        if(needSplitByTime){
          kqOverTimeRuleCalBiz.get_overtimeBalanceTimeBeans(timepointList,overtimeBalanceTimeBeans,kqTimesArrayComInfo,initArrays,toTime_index,fromTime_index,0);

          if(overtimeBalanceTimeBeans != null && !overtimeBalanceTimeBeans.isEmpty()){
            String bean_cross_fromtime = kqTimesArrayComInfo.getTimesByArrayindex(fromTime_index);
            String bean_cross_totime = kqTimesArrayComInfo.getTimesByArrayindex(toTime_index);
            for(int timeIndex = 0 ; timeIndex < overtimeBalanceTimeBeans.size() ;timeIndex++) {
              OvertimeBalanceTimeBean overtimeBalanceTimeBean = overtimeBalanceTimeBeans.get(timeIndex);
              String timePointStart = overtimeBalanceTimeBean.getTimepoint_start();
              String timePointEnd = overtimeBalanceTimeBean.getTimepoint_end();
              boolean isNeedTX = overtimeBalanceTimeBean.isNeedTX();
              int timePointStart_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointStart);
              int timePointEnd_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointEnd);
              if(timePointStart_index > fromTime_index){
                bean_cross_fromtime = kqTimesArrayComInfo.getTimesByArrayindex(timePointStart_index);
              }else{
                bean_cross_fromtime = kqTimesArrayComInfo.getTimesByArrayindex(fromTime_index);
              }
              if(timePointEnd_index < toTime_index){
                bean_cross_totime = kqTimesArrayComInfo.getTimesByArrayindex(timePointEnd_index);
              }else{
                bean_cross_totime = kqTimesArrayComInfo.getTimesByArrayindex(toTime_index);
              }
              int timepoint_mins = overtimeBalanceTimeBean.getTimepoint_mins();
              if(isNeedTX){
                if(timepoint_mins > 0){
                  overSplitBean = new SplitBean();
                  //然后把bean重新赋值下，根据拆分后的时间
                  BeanUtils.copyProperties(splitBean, overSplitBean);
                  overSplitBean.setChangeType(changeType);
                  overSplitBean.setPreChangeType(preChangeType);
                  overSplitBean.setOneDayHour(oneDayHour);
                  overSplitBean.setWorkmins(workmins);
                  overSplitBean.setComputingMode(computingMode+"");
                  overSplitBean.setChangeType(changeType);
                  overSplitBean.setFromDate(splitDate);
                  overSplitBean.setFromTime(bean_cross_fromtime);
                  overSplitBean.setToDate(splitDate);
                  overSplitBean.setToTime(bean_cross_totime);
                  overSplitBean.setBelongDate(splitDate);
                  overSplitBean.setD_Mins(timepoint_mins);
                  overSplitBean.setOvertimeBalanceTimeBeans(overtimeBalanceTimeBeans);
                  overSplitBean.setSerialid(serialid);
                  getDurationByRule(overSplitBean);
                  splitBeans.add(overSplitBean);
                }
              }
            }
          }
        }else{
          curMins = (int) kqOverTimeRuleCalBiz.getD_MinsByUnit(curMins);
          overSplitBean.setChangeType(changeType);
          overSplitBean.setPreChangeType(preChangeType);
          overSplitBean.setOneDayHour(oneDayHour);
          overSplitBean.setWorkmins(workmins);
          overSplitBean.setComputingMode(computingMode+"");
          overSplitBean.setChangeType(changeType);
          overSplitBean.setFromDate(splitDate);
          overSplitBean.setFromTime(kqTimesArrayComInfo.getTimesByArrayindex(fromTime_index));
          overSplitBean.setToDate(splitDate);
          overSplitBean.setToTime(kqTimesArrayComInfo.getTimesByArrayindex(toTime_index));
          overSplitBean.setBelongDate(splitDate);
          overSplitBean.setD_Mins(curMins);
          overSplitBean.setOvertimeBalanceTimeBeans(overtimeBalanceTimeBeans);
          overSplitBean.setSerialid(serialid);
          getDurationByRule(overSplitBean);
          splitBeans.add(overSplitBean);
        }
      }

      long b = System.currentTimeMillis();
      System.out.println("::"+(b-a));

    }catch (Exception e){
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
    }
  }


  /**
   * 被个性化设置处理下 得到实际的上下班时间
   * @param workLongTimeIndex
   * @param real_workLongTimeIndex
   * @param shiftInfoBean
   * @param kqTimesArrayComInfo
   * @param splitBean
   */
  public void get_real_workLongTimeIndex(List<int[]> workLongTimeIndex,
      List<int[]> real_workLongTimeIndex,
      ShiftInfoBean shiftInfoBean, KQTimesArrayComInfo kqTimesArrayComInfo,
      SplitBean splitBean) {

    //list带数组，这里要深拷贝
    for(int[] tmp : workLongTimeIndex){
      int[] real_tmp = new int[tmp.length];
      System.arraycopy(tmp, 0, real_tmp, 0, tmp.length);
      real_workLongTimeIndex.add(real_tmp);
    }
    if(real_workLongTimeIndex.size() == 1){
      //个性化设置只支持一次打卡的
      KQShiftRuleInfoBiz kqShiftRuleInfoBiz = new KQShiftRuleInfoBiz();
      kqShiftRuleInfoBiz.rest_workLongTimeIndex(shiftInfoBean,splitBean,real_workLongTimeIndex,kqTimesArrayComInfo,null);
    }

  }

  /**
   * 获取一下昨日的临界点
   * @param overRulesDetailMap
   * @param preChangeType_key
   * @param resourceid
   * @param preSplitDate
   * @param splitDate
   * @param preChangeType
   * @param kqTimesArrayComInfo
   * @param splitBean
   * @param changeType
   * @return
   */
  private String get_pre_overtime_cut_point(
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      String preChangeType_key, String resourceid, String preSplitDate, String splitDate,
      int preChangeType, KQTimesArrayComInfo kqTimesArrayComInfo,
      SplitBean splitBean, int changeType) {

    String pre_overtime_cut_point = "";
    KQOvertimeRulesDetailEntity preKqOvertimeRulesDetailEntity = overRulesDetailMap.get(preChangeType_key);
    if(preKqOvertimeRulesDetailEntity != null){
      int has_cut_point = preKqOvertimeRulesDetailEntity.getHas_cut_point();
      if(has_cut_point == 0){
        if(preChangeType == 2){
          ShiftInfoBean pre_shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, preSplitDate, false);
          if(pre_shiftInfoBean != null){
            List<int[]> workLongTimeIndex = pre_shiftInfoBean.getWorkLongTimeIndex();
            List<int[]> real_workLongTimeIndex = Lists.newArrayList();
            get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,pre_shiftInfoBean,kqTimesArrayComInfo,splitBean);

            if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
              pre_overtime_cut_point = kqTimesArrayComInfo.getTimesByArrayindex(real_workLongTimeIndex.get(0)[0]);
            }
          }
        }else {
          String next_beginwork_time = "";
          if(changeType == 2){
            ShiftInfoBean next_shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, splitDate, false);
            if(next_shiftInfoBean != null){
              List<int[]> workLongTimeIndex = next_shiftInfoBean.getWorkLongTimeIndex();
              List<int[]> real_workLongTimeIndex = Lists.newArrayList();
              get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,next_shiftInfoBean,kqTimesArrayComInfo,splitBean);

              if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
                next_beginwork_time = kqTimesArrayComInfo.getTimesByArrayindex(real_workLongTimeIndex.get(0)[0]);
              }
            }
          }
          if(next_beginwork_time.length() > 0){
            pre_overtime_cut_point = next_beginwork_time;
          }
        }
      }else{
        pre_overtime_cut_point = preKqOvertimeRulesDetailEntity.getCut_point();
      }
      if(pre_overtime_cut_point.length() == 0){
        pre_overtime_cut_point = "00:00";
      }
    }
    return pre_overtime_cut_point;
  }

  /**
   * 处理节假日的加班
   * @param initArrays
   * @param overRulesDetailMap
   * @param nextChangeType_key
   * @param next_changeType
   * @param next_beginwork_time
   */
  public void handle_changeType_1(int[] initArrays,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      String nextChangeType_key, int next_changeType, String next_beginwork_time){
    KQOvertimeRulesDetailEntity nextKqOvertimeRulesDetailEntity = overRulesDetailMap.get(nextChangeType_key);
//    if(nextKqOvertimeRulesDetailEntity != null){
//      if(next_changeType == 2){
//        //如果明日是工作日 工作日如果设置了上班前分钟，会导致加班归属被设置的分钟数给切断，上班前某些部分属于今天不属于昨日
//        int overtimeEnable = nextKqOvertimeRulesDetailEntity.getOvertimeEnable();
//        if(overtimeEnable == 1){
//          KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
//          int before_startTime = nextKqOvertimeRulesDetailEntity.getBefore_startTime();
//          int has_cut_point = nextKqOvertimeRulesDetailEntity.getHas_cut_point();
//          if(has_cut_point != 1){
//            before_startTime = -1;
//          }
//          int next_beginwork_time_index = kqTimesArrayComInfo.getArrayindexByTimes(next_beginwork_time);
//          if(before_startTime > -1){
//            int before_next_beginwork_time_index = next_beginwork_time_index - before_startTime;
//            if(before_next_beginwork_time_index > 0 && before_next_beginwork_time_index < next_beginwork_time_index){
//              before_next_beginwork_time_index = kqTimesArrayComInfo.turn24to48TimeIndex(before_next_beginwork_time_index);
//              next_beginwork_time_index = kqTimesArrayComInfo.turn24to48TimeIndex(next_beginwork_time_index);
//              Arrays.fill(initArrays, before_next_beginwork_time_index,next_beginwork_time_index,-1);
//            }
//          }
//        }
//      }
//    }
  }

  /**
   * 排除休息时间 现在工作日，节假日和休息日都可以设置排除休息时间
   * @param restTimeMap
   * @param changeType_key
   * @param kqTimesArrayComInfo
   * @param shouldAcross
   * @param initArrays
   */
  public void handle_resttime(Map<String, List<String[]>> restTimeMap, String changeType_key, KQTimesArrayComInfo kqTimesArrayComInfo, boolean shouldAcross, int[] initArrays) {

    //==zj
    if(restTimeMap.containsKey(changeType_key)){
      List<String[]> restTimeList = restTimeMap.get(changeType_key);
      //再把休息时间填充上去
      if(!restTimeList.isEmpty()){
        for(int k =0 ; k < restTimeList.size() ; k++){
          String[] restTimes = restTimeList.get(k);
          if(restTimes.length == 2){
            int restStart = kqTimesArrayComInfo.getArrayindexByTimes(restTimes[0]);
            int restEnd = kqTimesArrayComInfo.getArrayindexByTimes(restTimes[1]);
            if(shouldAcross && restEnd == 1439){
              //针对跨天的休息时段单独处理排除掉23:59-00:00的时间
              restEnd = 1440;
            }
            Arrays.fill(initArrays, restStart, restEnd, 1);
          }
        }
      }
    }
  }

  /**
   * 排除休息时长 根据加班时长排除指定时长
   * @param curMins
   * @param restTimeMap
   * @param changeType_key
   * @return
   */
  public int handle_restlength(int curMins, Map<String, List<String[]>> restTimeMap, String changeType_key) {
    if(restTimeMap.containsKey(changeType_key)) {
      List<String[]> restTimeList = restTimeMap.get(changeType_key);
      //再把休息时间填充上去
      if (!restTimeList.isEmpty()) {
        for(int k = restTimeList.size()-1 ; k >= 0 ; k--) {
          String[] restTimes = restTimeList.get(k);
          if (restTimes.length == 2) {
            //overlength 是满多少小时 cutlength是减去多少小时
            double overlength = Util.getDoubleValue(restTimes[0],-1);
            double cutlength = Util.getDoubleValue(restTimes[1],-1);
            if(overlength > -1 && cutlength > -1){
              double min_overlength = overlength * 60;
              double min_cutlength = cutlength * 60;
              if(curMins >= min_overlength){
                curMins = (int) (curMins-min_cutlength);
                break;
              }
            }
          }
        }

      }
    }
    return curMins;
  }

  /**
   * 处理工作日的加班
   * @param initArrays
   * @param resourceid
   * @param splitDate
   * @param before_startTime
   * @param startTime
   * @param fromTime_index
   * @param kqTimesArrayComInfo
   * @param splitBean
   * @param toTime_index
   * @return
   */
  public boolean handle_changeType_2(int[] initArrays, String resourceid, String splitDate,
      int before_startTime, int startTime, int fromTime_index,
      KQTimesArrayComInfo kqTimesArrayComInfo, SplitBean splitBean, int toTime_index){

    boolean isok = true;
    ShiftInfoBean cur_shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, splitDate, false);
    if(cur_shiftInfoBean != null){
      splitBean.setSerialid(cur_shiftInfoBean.getSerialid());
      List<int[]> workLongTimeIndex = cur_shiftInfoBean.getWorkLongTimeIndex();
      List<int[]> real_workLongTimeIndex = Lists.newArrayList();
      get_real_workLongTimeIndex(workLongTimeIndex,real_workLongTimeIndex,cur_shiftInfoBean,kqTimesArrayComInfo,splitBean);
      if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
        int all_firstworktime = 0;
        int all_lastworktime = 0;
        boolean need_middle_time = false;

        //==zj 是否统计班次休息时长
        new BaseBean().writeLog("==zj==(是否开启开关)" + splitBean.getResourceId() + " | " +splitBean.getBelongDate() + " | " + splitBean.getIsOpenRest());
        if ("1".equals(splitBean.getIsOpenRest())){
          need_middle_time = true;
        }
        for(int k = 0 ; k < real_workLongTimeIndex.size() ; k++){
          int workLongTimeStartIndex = real_workLongTimeIndex.get(k)[0];
          int workLongTimeEndIndex = real_workLongTimeIndex.get(k)[1];
          if(k == 0){
            if(before_startTime > -1){
              int before_workLongTimeStartIndex = workLongTimeStartIndex-before_startTime;
              if(before_workLongTimeStartIndex > 0){
                //从前一天的加班归属点到今天的上班前开始加班点，这段时间属于两不靠。需要排除
                if(fromTime_index < before_workLongTimeStartIndex){
                  Arrays.fill(initArrays, fromTime_index,before_workLongTimeStartIndex,-1);
                }
              }
            }
            all_firstworktime = workLongTimeStartIndex;
          }
          if(k == real_workLongTimeIndex.size()-1){
            if(startTime > -1){
              int after_workLongTimeEndIndex = workLongTimeEndIndex+startTime;
              if(workLongTimeEndIndex < after_workLongTimeEndIndex){
                Arrays.fill(initArrays, workLongTimeEndIndex,after_workLongTimeEndIndex,-1);
              }
            }
            all_lastworktime = workLongTimeEndIndex;
          }
          if(!need_middle_time){
            //目前标准加班，一天多次打卡的话是不算中间时间的，只算上班前和下班后的加班
          }else{
            //这个里面是可以算一天多次打卡的话是中间时间的
            Arrays.fill(initArrays, workLongTimeStartIndex,workLongTimeEndIndex,1);
          }
        }
        if(!need_middle_time){
          Arrays.fill(initArrays, all_firstworktime,all_lastworktime,1);
        }
        List<int[]> restLongTimeIndex = cur_shiftInfoBean.getRestLongTimeIndex();
        if(restLongTimeIndex != null && !restLongTimeIndex.isEmpty()){
          for (int k = 0; k < restLongTimeIndex.size(); k++) {
            //休息时段填充2
            //==zj 如果开启"是否统计休息时间"，则计算休息时长
            if ("1".equals(splitBean.getIsOpenRest())){
              Arrays.fill(initArrays, restLongTimeIndex.get(k)[0], restLongTimeIndex.get(k)[1], 0);
            }else {
              Arrays.fill(initArrays, restLongTimeIndex.get(k)[0], restLongTimeIndex.get(k)[1], 2);
            }

          }
        }
      }else {
        System.out.println("error");
        isok = false;
      }
    }else {
      System.out.println("error");
      isok = false;
    }

    return isok;
  }
  /**
   * 处理休息日的加班
   * @param restTimeMap
   * @param initArrays
   * @param overRulesDetailMap
   * @param nextChangeType_key
   * @param next_changeType
   * @param next_beginwork_time
   */
  public void handle_changeType_3(int[] initArrays,
      Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap,
      String nextChangeType_key, int next_changeType, String next_beginwork_time){
    KQOvertimeRulesDetailEntity nextKqOvertimeRulesDetailEntity = overRulesDetailMap.get(nextChangeType_key);
//    if(nextKqOvertimeRulesDetailEntity != null){
//      if(next_changeType == 2){
//        //如果明日是工作日 工作日如果设置了上班前分钟，会导致加班归属被设置的分钟数给切断，上班前某些部分属于今天不属于昨日
//        int overtimeEnable = nextKqOvertimeRulesDetailEntity.getOvertimeEnable();
//        if(overtimeEnable == 1){
//          KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
//          int before_startTime = nextKqOvertimeRulesDetailEntity.getBefore_startTime();
//          int has_cut_point = nextKqOvertimeRulesDetailEntity.getHas_cut_point();
//          if(has_cut_point != 1){
//            before_startTime = -1;
//          }
//          int next_beginwork_time_index = kqTimesArrayComInfo.getArrayindexByTimes(next_beginwork_time);
//          if(before_startTime > -1){
//            int before_next_beginwork_time_index = next_beginwork_time_index - before_startTime;
//            if(before_next_beginwork_time_index > 0 && before_next_beginwork_time_index < next_beginwork_time_index){
//              before_next_beginwork_time_index = kqTimesArrayComInfo.turn24to48TimeIndex(before_next_beginwork_time_index);
//              next_beginwork_time_index = kqTimesArrayComInfo.turn24to48TimeIndex(next_beginwork_time_index);
//              Arrays.fill(initArrays, before_next_beginwork_time_index,next_beginwork_time_index,-1);
//            }
//          }
//        }
//      }
//    }
  }

  public void getDurationByRule(SplitBean splitBean) {
    double D_Mins = splitBean.getD_Mins();
    int workmins = splitBean.getWorkmins();
    String durationrule = splitBean.getDurationrule();
    if("3".equalsIgnoreCase(durationrule) || "5".equalsIgnoreCase(durationrule)
        || "6".equalsIgnoreCase(durationrule)){
      double d_hour = D_Mins/60.0;
      splitBean.setDuration(KQDurationCalculatorUtil.getDurationRound5(""+d_hour));
    }else if("1".equalsIgnoreCase(durationrule)){
      double d_day = D_Mins/workmins;
      splitBean.setDuration(KQDurationCalculatorUtil.getDurationRound5(""+d_day));
    }
  }
}
