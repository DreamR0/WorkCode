package com.engine.kq.biz.chain;

import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.biz.KQSettingsBiz;
import com.engine.kq.biz.KQShiftRuleInfoBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.log.KQLog;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import weaver.general.Util;

/**
 * 加班链 基类
 */
public abstract class OvertimeChain {

  protected OvertimeChain success;	//定义下一个处理对象
  protected List<SplitBean> splitBeans;
  protected Map<String, String> splitMap;
  protected KQLog kqLog = new KQLog();

  public OvertimeChain(Map<String, String> splitMap,List<SplitBean> splitBeans) {
    this.splitMap = splitMap;
    this.splitBeans = splitBeans;
  }
  //创建链
  public void setOvertimeChain(OvertimeChain overtimeChain) {
    this.success=overtimeChain;
  }

  public abstract void handleOvertimeChain(SplitBean splitBean) throws Exception;

  /**
   * 非跨天情况下的 节假日加班
   * @param splitBean
   */
  public void doOverTimeSplit1(SplitBean splitBean){
    KQTimesArrayComInfo arrayComInfo = new KQTimesArrayComInfo();

    int[] initArrays = arrayComInfo.getInitArr();
    String resourceid = splitBean.getResourceId();
    String durationrule = splitBean.getDurationrule();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));

    int computingMode = KQOvertimeRulesBiz.getComputingMode(resourceid, splitDate);
    splitBean.setComputingMode(computingMode+"");

    kqLog.info("OvertimeChain doOverTimeSplit1:resourceid："+resourceid+":computingMode:"+computingMode);
    if(computingMode == 3){
//          无需审批，根据打卡时间计算加班时长
      splitBean.setD_Mins(0.0);
      kqLog.info("doOverTimeSplit1:resourceid："+resourceid+":splitDate:"+splitDate+"无需审批，根据打卡时间计算加班时长");
      return ;
    }

    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, splitDate);

    List<String[]> restTimeList = KQOvertimeRulesBiz.getRestTimeList(resourceid, splitDate);

    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);
    //再把休息时间填充上去
    if(!restTimeList.isEmpty()){
      for(int i =0 ; i < restTimeList.size() ; i++){
        String[] restTimes = restTimeList.get(i);
        if(restTimes.length == 2){
          int restStart = arrayComInfo.getArrayindexByTimes(restTimes[0]);
          int restEnd = arrayComInfo.getArrayindexByTimes(restTimes[1]);
          if(shouldAcross && restEnd == 1439){
            //针对跨天的休息时段单独处理排除掉23:59-00:00的时间
            restEnd = 1440;
          }
          Arrays.fill(initArrays, restStart, restEnd, 1);
        }
      }
    }

    //最后排除掉休息时间还剩多少加班时长就是最终的结果
    int curMins = arrayComInfo.getCnt(initArrays, startIndex, endIndex, 0);
    kqLog.info("doOverTimeSplit1:resourceid："+resourceid+":startIndex:"+startIndex+":endIndex:"+endIndex+":curMins:"+curMins);

    //如果人再当前指定日期下未开启加班，直接跳过
    if(overtimeEnable != 1){
      curMins = 0;
      kqLog.info("doOverTimeSplit1:resourceid："+resourceid+":splitDate:"+splitDate+"不允许加班");
    }
    if(curMins > 0){
      splitBean.setD_Mins(curMins);
      splitBean.setChangeType(1);
      splitBean.setComputingMode(""+computingMode);
      fillNewBean(splitBean);
    }

  }

  /**
   * 非跨天情况下的 工作日加班
   * @param splitBean
   */
  public void doOverTimeSplit2(SplitBean splitBean){
    KQTimesArrayComInfo arrayComInfo = new KQTimesArrayComInfo();

    int[] initArrays = arrayComInfo.getInitArr();
    String resourceid = splitBean.getResourceId();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));

    int computingMode = KQOvertimeRulesBiz.getComputingMode(resourceid, splitDate);
    splitBean.setComputingMode(computingMode+"");
    if(computingMode == 3){
//          无需审批，根据打卡时间计算加班时长
      splitBean.setD_Mins(0.0);
      kqLog.info("doOverTimeSplit2:resourceid："+resourceid+":splitDate:"+splitDate+"无需审批，根据打卡时间计算加班时长");
      return ;
    }
    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, splitDate);

    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);

    ShiftInfoBean shiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, splitDate,true);
    if(shiftInfoBean != null){
      List<int[]> workLongTimeIndex = shiftInfoBean.getWorkLongTimeIndex();
      List<int[]> real_workLongTimeIndex = Lists.newArrayList();

      //list带数组，这里要深拷贝
      for(int[] tmp : workLongTimeIndex){
        int[] real_tmp = new int[tmp.length];
        System.arraycopy(tmp, 0, real_tmp, 0, tmp.length);
        real_workLongTimeIndex.add(real_tmp);
      }
      if(real_workLongTimeIndex.size() == 1){
        //个性化设置只支持一次打卡的
        KQShiftRuleInfoBiz kqShiftRuleInfoBiz = new KQShiftRuleInfoBiz();
        kqShiftRuleInfoBiz.rest_workLongTimeIndex(shiftInfoBean,splitBean,real_workLongTimeIndex,arrayComInfo,null);
      }
      if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
        int[] workTimes = real_workLongTimeIndex.get(real_workLongTimeIndex.size()-1);
        for(int i = 0 ; i < real_workLongTimeIndex.size() ;i++){
          //工作日填充1
          Arrays.fill(initArrays, real_workLongTimeIndex.get(i)[0], real_workLongTimeIndex.get(i)[1], 1);
        }
        List<int[]> restIndex = shiftInfoBean.getRestIndex();
        for(int i = 0 ; i < restIndex.size() ; i++){
          //休息时段填充2
          Arrays.fill(initArrays, restIndex.get(i)[0], restIndex.get(i)[1], 2);
        }
        //加班起算时间是必填项
        int startTimeMin = KQOvertimeRulesBiz.getStartTime(resourceid,splitDate);
        startTimeMin = startTimeMin < 0 ? 0 : startTimeMin;
        int lastWorkTimeIndex = workTimes[1];
        //从下班时间到下班起算时间这段时间填充4
        Arrays.fill(initArrays, lastWorkTimeIndex, (lastWorkTimeIndex+startTimeMin), 4);

        int overMins = arrayComInfo.getCnt(initArrays, 0, 1440, 0);

        kqLog.info("OvertimeChain doOverTimeSplit2:resourceid："+resourceid+":overMins:"+overMins);
        //如果人再当前指定日期下未开启加班，直接跳过
        if(overtimeEnable != 1){
          overMins= 0;
          kqLog.info("doOverTimeSplit2:resourceid："+resourceid+":splitDate:"+splitDate+"不允许加班");
        }
        if(overMins > 0){
          splitBean.setD_Mins(overMins);
          splitBean.setChangeType(2);
          splitBean.setComputingMode(""+computingMode);
          fillNewBean(splitBean);
        }
      }else{
        kqLog.info("doOverTimeSplit2，当天工作日取不到当天工作时段:resourceid:"+resourceid+":splitDate:"+splitDate);
      }
    }else{
      kqLog.info("doOverTimeSplit2，工作日加班shiftInfoBean为null:resourceid:"+resourceid+":splitDate:"+splitDate);
    }
  }

  /**
   * 非跨天情况下的 休息日加班
   * @param splitBean
   */
  public void doOverTimeSplit3(SplitBean splitBean){

    KQTimesArrayComInfo arrayComInfo = new KQTimesArrayComInfo();

    int[] initArrays = arrayComInfo.getInitArr();
    String resourceid = splitBean.getResourceId();
    String durationrule = splitBean.getDurationrule();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));

    int computingMode = KQOvertimeRulesBiz.getComputingMode(resourceid, splitDate);
    splitBean.setComputingMode(computingMode+"");

    kqLog.info("OvertimeChain doOverTimeSplit3:resourceid："+resourceid+":computingMode:"+computingMode);
    if(computingMode == 3){
//          无需审批，根据打卡时间计算加班时长
      kqLog.info("OvertimeChain doOverTimeSplit3:resourceid："+resourceid+":splitDate:"+splitDate+"无需审批，根据打卡时间计算加班时长");
      splitBean.setD_Mins(0.0);
      return ;
    }

    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, splitDate);

    List<String[]> restTimeList = KQOvertimeRulesBiz.getRestTimeList(resourceid, splitDate);

    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);
    //再把休息时间填充上去
    if(!restTimeList.isEmpty()){
      for(int i =0 ; i < restTimeList.size() ; i++){
        String[] restTimes = restTimeList.get(i);
        if(restTimes.length == 2){
          int restStart = arrayComInfo.getArrayindexByTimes(restTimes[0]);
          int restEnd = arrayComInfo.getArrayindexByTimes(restTimes[1]);
          if(shouldAcross && restEnd == 1439){
            //针对跨天的休息时段单独处理排除掉23:59-00:00的时间
            restEnd = 1440;
          }
          Arrays.fill(initArrays, restStart, restEnd, 1);
        }
      }
    }
    //最后排除掉休息时间还剩多少加班时长就是最终的结果
    int curMins = arrayComInfo.getCnt(initArrays, startIndex, endIndex, 0);
    kqLog.info("OvertimeChain doOverTimeSplit3:resourceid："+resourceid+":startIndex:"+startIndex+":endIndex:"+endIndex+":curMins:"+curMins);

    //如果人再当前指定日期下未开启加班，直接跳过
    if(overtimeEnable != 1){
      curMins = 0;
      kqLog.info("doOverTimeSplit3:resourceid："+resourceid+":splitDate:"+splitDate+"不允许加班");
    }
    if(curMins > 0){
      splitBean.setD_Mins(curMins);
      splitBean.setChangeType(3);
      splitBean.setComputingMode(""+computingMode);
      fillNewBean(splitBean);
    }
  }

  /**
   * 针对工作日 前一天跨天的情况处理
   * @param splitBean
   * @param arrayComInfo
   * @param shiftInfoBean
   * @param real_workLongTimeIndex
   */
  public int handlePreWork(SplitBean splitBean, KQTimesArrayComInfo arrayComInfo,
      ShiftInfoBean shiftInfoBean, List<int[]> real_workLongTimeIndex){
    int curFirstTimeIndex = -1;
    String resourceid = splitBean.getResourceId();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));
    String preSplitDate = splitMap.get("preSplitDate");

    int[] initArrays = arrayComInfo.getInitArr();
    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);

    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }
    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, preSplitDate);

    //加班起算时间是必填项
    int startTimeMin = KQOvertimeRulesBiz.getStartTime(resourceid,preSplitDate);
    int preLastWorkTimeIndex = -1;
    List<String> allWorkTime = shiftInfoBean.getAllWorkTime();
    String curFirstTime = allWorkTime.get(0);
    curFirstTimeIndex = arrayComInfo.getArrayindexByTimes(curFirstTime);
    if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()){
      int[] cur_real_startIndex = real_workLongTimeIndex.get(0);
      if(curFirstTimeIndex > cur_real_startIndex[0]){
        curFirstTimeIndex = cur_real_startIndex[0];
      }
    }

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);

    List<int[]> preWorkIndex = shiftInfoBean.getPreWorkIndex();
    if(preWorkIndex != null && !preWorkIndex.isEmpty()){
      int[] preWorkIndexs = preWorkIndex.get(preWorkIndex.size()-1);
      for(int i = 0 ; i < preWorkIndex.size() ;i++){
        //跨天工作日填充1
        Arrays.fill(initArrays, preWorkIndex.get(i)[0], preWorkIndex.get(i)[1], 1);
      }
      preLastWorkTimeIndex = preWorkIndexs[1];
    }

    int shift_beginworktime_index = -1;
    int shift_endworktime_index = -1;
    if(preWorkIndex.size() == 1){
      boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
      if(is_flow_humanized){
        ShiftInfoBean preShiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, preSplitDate,false);
        Map<String,String> shifRuleMap = Maps.newHashMap();
        KQShiftRuleInfoBiz.getShiftRuleInfo(preShiftInfoBean,resourceid,shifRuleMap);
        if(!shifRuleMap.isEmpty()) {
          if (shifRuleMap.containsKey("shift_beginworktime")) {
            String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
            if (shift_beginworktime.length() > 0) {
              shift_beginworktime_index = arrayComInfo.getArrayindexByTimes(shift_beginworktime);
            }
          }
          if (shifRuleMap.containsKey("shift_endworktime")) {
            String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
            if (shift_endworktime.length() > 0) {
              shift_endworktime_index = arrayComInfo.getArrayindexByTimes(shift_endworktime);
            }
          }
        }
      }
    }
    if(shift_beginworktime_index > -1){
      curFirstTimeIndex = shift_beginworktime_index;
    }
    if(shift_endworktime_index > -1) {
      if (shift_endworktime_index > 1440) {
        //如果个性化之后的下班时间还是在第二天
        int tmp_shift_endworktime_index = shift_endworktime_index - 1440;
        if(preLastWorkTimeIndex > -1){
          if(preLastWorkTimeIndex > tmp_shift_endworktime_index){
            Arrays.fill(initArrays, tmp_shift_endworktime_index, preLastWorkTimeIndex, -1);
          }else{
            Arrays.fill(initArrays, preLastWorkTimeIndex, tmp_shift_endworktime_index, 1);
          }
        }else{
          Arrays.fill(initArrays, 0, tmp_shift_endworktime_index, 1);
        }
      }else{
        //如果个性化之后的下班时间不在第二天了，但是原始的下班时间还是在第二天，那么需要把这段时间给填补回来
        if(preLastWorkTimeIndex > -1){
          Arrays.fill(initArrays, 0, preLastWorkTimeIndex, -1);
        }
      }
    }

    List<int[]> preRestIndex = shiftInfoBean.getPreRestIndex();
    if(preRestIndex != null && !preRestIndex.isEmpty()){
      for(int i = 0 ; i < preRestIndex.size() ; i++){
        //跨天休息时段填充2
        Arrays.fill(initArrays, preRestIndex.get(i)[0], preRestIndex.get(i)[1], 2);
      }
    }
    //工作日还有一个加班起算时间的设置
    if(preLastWorkTimeIndex >= 0){
      if(shift_endworktime_index > -1) {
        if (shift_endworktime_index > 1440) {
          //如果个性化之后的下班时间还是在第二天
          preLastWorkTimeIndex = shift_endworktime_index - 1440;
        }else{
          preLastWorkTimeIndex = shift_endworktime_index;
        }
      }
      Arrays.fill(initArrays, preLastWorkTimeIndex, preLastWorkTimeIndex+startTimeMin, 4);
    }

    int preOverMins = arrayComInfo.getCnt(initArrays, startIndex, curFirstTimeIndex, 0);
    kqLog.info("handlePreWork:resourceid："+resourceid+":startIndex:"+startIndex+":curFirstTimeIndex:"+curFirstTimeIndex+"<br/>");
    kqLog.info("handlePreWork:resourceid："+resourceid+":preOverMins:"+preOverMins+"<br/>");

    //如果人再当前指定日期下未开启加班，直接跳过
    if(overtimeEnable != 1){
      preOverMins = 0;
      kqLog.info("handlePreWork:resourceid："+resourceid+":preSplitDate:"+preSplitDate+"不允许加班");
    }
    if(preOverMins > 0){
      SplitBean preSplitBean = new SplitBean();
      //然后把bean重新赋值下，根据拆分后的时间
      BeanUtils.copyProperties(splitBean, preSplitBean);
      preSplitBean.setD_Mins(preOverMins);
      preSplitBean.setBelongDate(preSplitDate);
      preSplitBean.setChangeType(2);
      int computingMode = KQOvertimeRulesBiz.getComputingMode(preSplitBean.getResourceId(), preSplitDate);
      preSplitBean.setComputingMode(""+computingMode);
      fillNewBean(preSplitBean);
    }
    return curFirstTimeIndex;
  }

  /**
   * 针对工作日 处理完前一天的数据处理当天情况
   * @param splitBean
   * @param arrayComInfo
   * @param curShiftInfoBean
   * @param real_workLongTimeIndex
   * @param firstWorkIndex 前一天
   */
  public void handleCurWork(SplitBean splitBean, KQTimesArrayComInfo arrayComInfo,
      ShiftInfoBean curShiftInfoBean, List<int[]> real_workLongTimeIndex, int firstWorkIndex) throws Exception{
    String resourceid = splitBean.getResourceId();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));
    String preSplitDate = splitMap.get("preSplitDate");

    int curComputingMode = KQOvertimeRulesBiz.getComputingMode(resourceid, splitDate);
    if(curComputingMode == 3) {
//          前一天是无需审批，根据打卡时间计算加班时长,那么只需要按照普通的工作日加班处理就可以
      kqLog.info("OvertimeChain handleCurWork:resourceid："+resourceid+":splitDate:"+splitDate+"前一天是无需审批，根据打卡时间计算加班时长,那么只需要按照普通的工作日加班处理就可以");
      return ;
    }

    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, splitDate);

    int[] initArrays = arrayComInfo.getInitArr();
    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);

    int[] workTimeIndexs = real_workLongTimeIndex.get(real_workLongTimeIndex.size()-1);
    if(real_workLongTimeIndex != null && !real_workLongTimeIndex.isEmpty()) {
      for (int i = 0; i < real_workLongTimeIndex.size(); i++) {
        //工作日填充1
        Arrays.fill(initArrays, real_workLongTimeIndex.get(i)[0], real_workLongTimeIndex.get(i)[1], 1);
      }
      List<int[]> restIndex = curShiftInfoBean.getRestIndex();
      for (int i = 0; i < restIndex.size(); i++) {
        //休息时段填充2
        Arrays.fill(initArrays, restIndex.get(i)[0], restIndex.get(i)[1], 2);
      }
    }
    //加班起算时间是必填项
    int startTimeMin = KQOvertimeRulesBiz.getStartTime(resourceid,splitDate);
    startTimeMin = startTimeMin < 0 ? 0 : startTimeMin;
    int lastWorkTimeIndex = workTimeIndexs[1];
    //从下班时间到下班起算时间这段时间填充4
    Arrays.fill(initArrays, lastWorkTimeIndex, (lastWorkTimeIndex+startTimeMin), 4);

    int need_startIndex = startIndex;
    if(firstWorkIndex > startIndex){
      need_startIndex = firstWorkIndex;
    }
    int overMins = arrayComInfo.getCnt(initArrays, need_startIndex, 1440, 0);

    kqLog.info("OvertimeChain:resourceid："+resourceid+":startIndex:"+startIndex+":overMins:"+overMins);
    //如果人再当前指定日期下未开启加班，直接跳过
    if(overtimeEnable != 1){
      overMins= 0;
      kqLog.info("handleCurWork:resourceid："+resourceid+":splitDate:"+splitDate+"不允许加班");
    }
    if(overMins > 0){
      SplitBean curSplitBean = new SplitBean();
      //然后把bean重新赋值下，根据拆分后的时间
      BeanUtils.copyProperties(splitBean, curSplitBean);
      curSplitBean.setD_Mins(overMins);
      curSplitBean.setBelongDate(splitDate);
      curSplitBean.setChangeType(2);
      curSplitBean.setFromTime(arrayComInfo.getTimesByArrayindex(lastWorkTimeIndex));
      curSplitBean.setToTime(arrayComInfo.getTimesByArrayindex(endIndex));
      int computingMode = KQOvertimeRulesBiz.getComputingMode(curSplitBean.getResourceId(), splitDate);
      curSplitBean.setComputingMode(""+computingMode);
      fillNewBean(curSplitBean);
    }
  }

  /**
   * 针对节假日 处理完前一天的数据处理当天情况
   * @param splitBean
   * @param arrayComInfo
   * @param curFirstTimeIndex
   */
  public void handleCurHoliday(SplitBean splitBean,KQTimesArrayComInfo arrayComInfo,int curFirstTimeIndex) {
    String resourceid = splitBean.getResourceId();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));
    String preSplitDate = splitMap.get("preSplitDate");

    int computingMode = KQOvertimeRulesBiz.getComputingMode(resourceid, splitDate);
    splitBean.setComputingMode(computingMode+"");
    if(computingMode == 3){
//          无需审批，根据打卡时间计算加班时长
      splitBean.setD_Mins(0.0);
      kqLog.info("handleCurHoliday:resourceid："+resourceid+":splitDate:"+splitDate+"无需审批，根据打卡时间计算加班时长");
      return ;
    }

    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, splitDate);

    int[] initArrays = arrayComInfo.getInitArr();
    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }

    List<String[]> restTimeList = KQOvertimeRulesBiz.getRestTimeList(resourceid, splitDate);

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);
    //再把休息时间填充上去
    if(!restTimeList.isEmpty()){
      for(int i =0 ; i < restTimeList.size() ; i++){
        String[] restTimes = restTimeList.get(i);
        if(restTimes.length == 2){
          int restStart = arrayComInfo.getArrayindexByTimes(restTimes[0]);
          int restEnd = arrayComInfo.getArrayindexByTimes(restTimes[1]);
          if(shouldAcross && restEnd == 1439){
            //针对跨天的休息时段单独处理排除掉23:59-00:00的时间
            restEnd = 1440;
          }
          Arrays.fill(initArrays, restStart, restEnd, 1);
        }
      }
    }
    //最后排除掉休息时间还剩多少加班时长就是最终的结果
    int curMins = arrayComInfo.getCnt(initArrays, curFirstTimeIndex, endIndex, 0);
    curMins = curMins > 0 ? curMins : 0;

    kqLog.info("OvertimeChain handleCurHoliday:resourceid："+resourceid+":curFirstTimeIndex:"+curFirstTimeIndex+":endIndex:"+endIndex+":curMins:"+curMins);
    //如果人再当前指定日期下未开启加班，直接跳过
    if(overtimeEnable != 1){
      curMins = 0;
      kqLog.info("handleCurHoliday:resourceid："+resourceid+":splitDate:"+splitDate+"不允许加班");
    }
    if(curMins > 0){
      SplitBean curSplitBean = new SplitBean();
      //然后把bean重新赋值下，根据拆分后的时间
      BeanUtils.copyProperties(splitBean, curSplitBean);
      curSplitBean.setD_Mins(curMins);
      curSplitBean.setFromTime(arrayComInfo.getTimesByArrayindex(curFirstTimeIndex));
      curSplitBean.setToTime(arrayComInfo.getTimesByArrayindex(endIndex));
      curSplitBean.setBelongDate(splitDate);
      curSplitBean.setComputingMode(""+computingMode);
      curSplitBean.setChangeType(1);
      fillNewBean(curSplitBean);
    }
  }

  /**
   * 针对休息日 处理完前一天的数据处理当天情况
   * @param splitBean
   * @param arrayComInfo
   * @param curFirstTimeIndex
   */
  public void handleCurRestday(SplitBean splitBean,KQTimesArrayComInfo arrayComInfo,int curFirstTimeIndex) {
    String resourceid = splitBean.getResourceId();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));
    String preSplitDate = splitMap.get("preSplitDate");

    int computingMode = KQOvertimeRulesBiz.getComputingMode(resourceid, splitDate);

    kqLog.info("OvertimeChain handleCurRestday:resourceid："+resourceid+":computingMode:"+computingMode);
    splitBean.setComputingMode(computingMode+"");
    if(computingMode == 3){
//          无需审批，根据打卡时间计算加班时长
      splitBean.setD_Mins(0.0);
      return ;
    }
    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, splitDate);

    int[] initArrays = arrayComInfo.getInitArr();
    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }

    List<String[]> restTimeList = KQOvertimeRulesBiz.getRestTimeList(resourceid, splitDate);

    //先把要计算的时长填充上0
    Arrays.fill(initArrays, startIndex, endIndex, 0);
    //再把休息时间填充上去
    if(!restTimeList.isEmpty()){
      for(int i =0 ; i < restTimeList.size() ; i++){
        String[] restTimes = restTimeList.get(i);
        if(restTimes.length == 2){
          int restStart = arrayComInfo.getArrayindexByTimes(restTimes[0]);
          int restEnd = arrayComInfo.getArrayindexByTimes(restTimes[1]);
          if(shouldAcross && restEnd == 1439){
            //针对跨天的休息时段单独处理排除掉23:59-00:00的时间
            restEnd = 1440;
          }
          Arrays.fill(initArrays, restStart, restEnd, 1);
        }
      }
    }
    //最后排除掉休息时间还剩多少加班时长就是最终的结果
    int curMins = arrayComInfo.getCnt(initArrays, curFirstTimeIndex, endIndex, 0);
    curMins = curMins > 0 ? curMins : 0;
    kqLog.info("OvertimeChain handleCurRestday:resourceid："+resourceid+":curFirstTimeIndex:"+curFirstTimeIndex+":endIndex:"+endIndex+":curMins:"+curMins);
    //如果人再当前指定日期下未开启加班，直接跳过
    if(overtimeEnable != 1){
      curMins = 0;
      kqLog.info("handlePreWork:resourceid："+resourceid+":splitDate:"+splitDate+"不允许加班");
    }

    if(curMins > 0){
      SplitBean curSplitBean = new SplitBean();
      //然后把bean重新赋值下，根据拆分后的时间
      BeanUtils.copyProperties(splitBean, curSplitBean);
      curSplitBean.setBelongDate(splitDate);
      curSplitBean.setFromTime(arrayComInfo.getTimesByArrayindex(curFirstTimeIndex));
      curSplitBean.setToTime(arrayComInfo.getTimesByArrayindex(endIndex));
      curSplitBean.setD_Mins(curMins);
      curSplitBean.setBelongDate(splitDate);
      curSplitBean.setChangeType(3);
      curSplitBean.setComputingMode(""+computingMode);
      fillNewBean(curSplitBean);
    }
  }

  /**
   * 前一天是工作日，处理前一天工作日的跨天数据
   * @param splitBean
   * @param arrayComInfo
   * @param shiftInfoBean
   * @return
   */
  public int handlePreWorkday(SplitBean splitBean, KQTimesArrayComInfo arrayComInfo, ShiftInfoBean shiftInfoBean) {

    String resourceid = splitBean.getResourceId();
    String splitDate = splitBean.getBelongDate();
    String splitFromTime = splitBean.getFromTime();
    String splitToTime = splitBean.getToTime();
    boolean shouldAcross = "1".equalsIgnoreCase(Util.null2String(splitMap.get("shouldAcross")));
    String preSplitDate = splitMap.get("preSplitDate");

    int overtimeEnable = KQOvertimeRulesBiz.getOvertimeEnable(resourceid, preSplitDate);

    int[] initArrays = arrayComInfo.getInitArr();
    int startIndex = arrayComInfo.getArrayindexByTimes(splitFromTime);
    int endIndex = arrayComInfo.getArrayindexByTimes(splitToTime);
    if(shouldAcross){
      endIndex = arrayComInfo.getArrayindexByTimes("24:00");
    }
    List<int[]> preWorkAcrossIndex = shiftInfoBean.getPreWorkAcrossIndex();
    if(preWorkAcrossIndex != null && !preWorkAcrossIndex.isEmpty()){
      int shift_beginworktime_index = -1;
      int shift_endworktime_index = -1;
      if(preWorkAcrossIndex.size() == 1){
        boolean is_flow_humanized = KQSettingsBiz.is_flow_humanized();
        if(is_flow_humanized){
          ShiftInfoBean preShiftInfoBean = KQDurationCalculatorUtil.getWorkTime(resourceid, preSplitDate,false);
          Map<String,String> shifRuleMap = Maps.newHashMap();
          KQShiftRuleInfoBiz.getShiftRuleInfo(preShiftInfoBean,resourceid,shifRuleMap);
          if(!shifRuleMap.isEmpty()) {
            if (shifRuleMap.containsKey("shift_beginworktime")) {
              String shift_beginworktime = Util.null2String(shifRuleMap.get("shift_beginworktime"));
              if (shift_beginworktime.length() > 0) {
                shift_beginworktime_index = arrayComInfo.getArrayindexByTimes(shift_beginworktime);
              }
            }
            if (shifRuleMap.containsKey("shift_endworktime")) {
              String shift_endworktime = Util.null2String(shifRuleMap.get("shift_endworktime"));
              if (shift_endworktime.length() > 0) {
                shift_endworktime_index = arrayComInfo.getArrayindexByTimes(shift_endworktime);
              }
            }
          }
        }
      }

      int[] preWorkAcrossIndexs = preWorkAcrossIndex.get(0);
      //得到当天最早的上班时间，则从0-firstWorkIndex都属于前一天的加班
      int firstWorkIndex = preWorkAcrossIndexs != null ? preWorkAcrossIndexs[0] : -1;
      if(shift_beginworktime_index > -1){
        firstWorkIndex = shift_beginworktime_index;
      }

      //先把要计算的时长填充上0
      Arrays.fill(initArrays, startIndex, endIndex, 0);

      int last_preworkIndex = -1;
      List<int[]> preWorkIndex = shiftInfoBean.getPreWorkIndex();
      for(int i = 0 ; preWorkIndex != null && i < preWorkIndex.size() ;i++){
        //跨天工作日填充1
        Arrays.fill(initArrays, preWorkIndex.get(i)[0], preWorkIndex.get(i)[1], 1);
        last_preworkIndex = preWorkIndex.get(i)[1];
      }
      if(shift_endworktime_index > -1) {
        if (shift_endworktime_index > 1440) {
          //如果个性化之后的下班时间还是在第二天
          int tmp_shift_endworktime_index = shift_endworktime_index - 1440;
          if(last_preworkIndex > -1){
            if(last_preworkIndex > tmp_shift_endworktime_index){
              Arrays.fill(initArrays, tmp_shift_endworktime_index, last_preworkIndex, -1);
            }else{
              Arrays.fill(initArrays, last_preworkIndex, tmp_shift_endworktime_index, 1);
            }
          }else{
            Arrays.fill(initArrays, 0, tmp_shift_endworktime_index, 1);
          }
        }else{
          //如果个性化之后的下班时间不在第二天了，但是原始的下班时间还是在第二天，那么需要把这段时间给填补回来
          if(last_preworkIndex > -1){
            Arrays.fill(initArrays, 0, last_preworkIndex, -1);
          }
        }
      }

      List<int[]> preRestIndex = shiftInfoBean.getPreRestIndex();
      for(int i = 0 ;preRestIndex != null && i < preRestIndex.size() ; i++){
        //跨天休息时段填充2
        Arrays.fill(initArrays, preRestIndex.get(i)[0], preRestIndex.get(i)[1], 2);
      }
      //加班起算时间是必填项
      int startTimeMin = KQOvertimeRulesBiz.getStartTime(resourceid,preSplitDate);
      startTimeMin = startTimeMin < 0 ? 0 : startTimeMin;
      int lastWorkTimeIndex = preWorkAcrossIndexs[1];
      if(shift_endworktime_index > -1) {
        if (shift_endworktime_index > 1440) {
          //如果个性化之后的下班时间还是在第二天
          lastWorkTimeIndex = shift_endworktime_index - 1440;
        }else{
          lastWorkTimeIndex = shift_endworktime_index;
        }
      }
      //从下班时间到下班起算时间这段时间填充4
      Arrays.fill(initArrays, lastWorkTimeIndex, (lastWorkTimeIndex+startTimeMin), 4);

      int overMins = arrayComInfo.getCnt(initArrays, startIndex, firstWorkIndex, 0);
      kqLog.info("handlePreWorkday:resourceid："+resourceid+":startIndex:"+startIndex+":firstWorkIndex:"+firstWorkIndex+":overMins:"+overMins);
      overMins = overMins > 0 ? overMins : 0;
      //如果人再当前指定日期下未开启加班，直接跳过
      if(overtimeEnable != 1){
        overMins = 0;
        kqLog.info("handlePreWorkday:resourceid："+resourceid+":preSplitDate:"+preSplitDate+"不允许加班");
      }
      if(overMins > 0){
        SplitBean preSplitBean = new SplitBean();
        //然后把bean重新赋值下，根据拆分后的时间
        BeanUtils.copyProperties(splitBean, preSplitBean);
        preSplitBean.setD_Mins(overMins);
        preSplitBean.setBelongDate(preSplitDate);
        preSplitBean.setFromTime(arrayComInfo.getTimesByArrayindex(startIndex));
        preSplitBean.setToTime(arrayComInfo.getTimesByArrayindex(firstWorkIndex));
        preSplitBean.setChangeType(2);
        int computingMode = KQOvertimeRulesBiz.getComputingMode(preSplitBean.getResourceId(), preSplitDate);
        preSplitBean.setComputingMode(""+computingMode);
        fillNewBean(preSplitBean);
      }
      return firstWorkIndex;
    }else{
      return -1;
    }
  }

  public void fillNewBean(SplitBean splitBean){
    getDurationByRule(splitBean);
    splitBeans.add(splitBean);
    //客户有的时候加班流程选择几年十几年的，这个日志会很大，屏蔽了吧
//    kqLog.info("getDurationByRule:splitBeans:"+JSON.toJSON(splitBeans));
  }

  /**
   * 根据加班单位得到加班时长
   * @param splitBean
   */
  public void getDurationByRule(SplitBean splitBean) {
    double D_Mins = splitBean.getD_Mins();
    int workmins = splitBean.getWorkmins();
    String durationrule = splitBean.getDurationrule();
    if("3".equalsIgnoreCase(durationrule)){
      double d_hour = D_Mins/60.0;
      splitBean.setDuration(KQDurationCalculatorUtil.getDurationRound5(""+d_hour));
    }else if("1".equalsIgnoreCase(durationrule)){
      double d_day = D_Mins/workmins;
      splitBean.setDuration(KQDurationCalculatorUtil.getDurationRound5(""+d_day));
    }
  }
}
