package com.engine.kq.biz.chain.cominfo;

import com.engine.kq.biz.KQTimesArrayComInfo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.general.Util;

/**
 * 针对时长计算的基类
 */
public abstract class ShiftComIndex {

  protected ShiftComIndex success;	//定义下一个处理对象
  protected String name ;	//定义下一个对象的名字
  protected Map<String,Object> workTimeMap ;	//每个地方都会用到的缓存类
  private LocalDateTime localDateTime = LocalDateTime.now();
  private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

  public ShiftComIndex(String name,Map<String,Object> workTimeMap) {
    this.name=name;
    this.workTimeMap = workTimeMap;
  }
  //创建链
  public void setDuration(ShiftComIndex durationIndex) {
    this.success=durationIndex;
  }

  public abstract void handleDuration(ShiftInfoCominfoBean shiftInfoCominfoBean)  throws Exception ;

  /**
   * 得到需要的上下班时间
   * @param workTimes
   * @param shiftInfoCominfoBean
   */
  public void setWorkDuration(List<Object> workTimes,ShiftInfoCominfoBean shiftInfoCominfoBean) throws Exception {

    String tmpDate = localDateTime.toLocalDate().format(dateFormatter);
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
    List<String[]> preWorkTime = shiftInfoCominfoBean.getPreWorkTime();
    List<String[]> workTime = shiftInfoCominfoBean.getWorkTime();
    List<String[]> workAcrossTime = shiftInfoCominfoBean.getWorkAcrossTime();

    List<int[]> preWorkTimeIndex = shiftInfoCominfoBean.getPreWorkTimeIndex();
    List<int[]> workTimeIndex = shiftInfoCominfoBean.getWorkTimeIndex();
    List<int[]> workAcrossTimeIndex = shiftInfoCominfoBean.getWorkAcrossTimeIndex();

    List<String[][]> workPunchMins =shiftInfoCominfoBean.getWorkPunchMins();
    List<String> allWorkTime = shiftInfoCominfoBean.getAllWorkTime();
    List<String> allAcrossWorkTime = shiftInfoCominfoBean.getAllAcrossWorkTime();
    List<Integer> eachWorkMins = shiftInfoCominfoBean.getEachWorkMins();

    List<Map<String, String>> workAcrossLongTime = shiftInfoCominfoBean.getWorkAcrossLongTime();
    List<Object> timelineList = shiftInfoCominfoBean.getTimelineList();
    List<String> allLongWorkTime = shiftInfoCominfoBean.getAllLongWorkTime();
    List<Map<String, String>> signWorkTime = shiftInfoCominfoBean.getSignWorkTime();
    List<String> allWorkTimeisAcross = shiftInfoCominfoBean.getAllWorkTimeisAcross();

    int workmins = shiftInfoCominfoBean.getWorkmins();

    List<int[]> workLongTimeIndex = shiftInfoCominfoBean.getWorkLongTimeIndex();

    for(int k = 0 ; k < workTimes.size() ; k++){
      List<Object> sectionList = (List<Object>)workTimes.get(k);
      if(sectionList == null || sectionList.isEmpty()){
        break;
      }
      String[][] workMinsAcrossArr = new String[2][];
      workMinsAcrossArr[0]=new String[2];
      workMinsAcrossArr[1]=new String[2];
      //sectionList一定是成对存在的，开始时间和结束时间
      if(sectionList.size() == 2) {
        Map<String, Object> onWorkMap = (Map<String, Object>) sectionList.get(0);
        buildWorkIndex(onWorkMap, workMinsAcrossArr, allAcrossWorkTime, allWorkTime,allLongWorkTime,0,kqTimesArrayComInfo,allWorkTimeisAcross);

        Map<String, Object> offWorkMap = (Map<String, Object>) sectionList.get(1);
        buildWorkIndex(offWorkMap, workMinsAcrossArr,allAcrossWorkTime, allWorkTime,allLongWorkTime,1,
            kqTimesArrayComInfo,allWorkTimeisAcross);
        workPunchMins.add(workMinsAcrossArr);
      }
    }

    int j = 0 ;
    for(int i = 0 ; i < allWorkTime.size() ; ){
      String[] preWorkArr = new String[2];
      String[] workArr = new String[2];
      String[] workAcrossArr = new String[2];

      int[] preWorkIndexArr = new int[2];
      int[] workIndexArr = new int[2];
      int[] workLongIndexArr = new int[2];
      int[] workAcrossIndexArr = new int[2];

      if(allWorkTime.size() % 2 != 0){
        break;
      }
      //是否是第一个打卡 针对多次打卡的情况来说的
      boolean isFirst = false;
      if(i == 0){
        isFirst = true;
      }
      String onWorkTime = Util.null2String(allWorkTime.get(i));
      String offWorkTime = Util.null2String(allWorkTime.get(i+1));
      String onWorkTimeIsAcross = Util.null2String(allWorkTimeisAcross.get(i));
      String offWorkTimeIsAcross = Util.null2String(allWorkTimeisAcross.get(i+1));

      String[][] workPunchMin = workPunchMins.get(j);
      //上班 允许最早打卡分钟数
      String punchMin00 = workPunchMin[0][0];
//      //上班 允许最晚打卡分钟数
      String punchMin01 = workPunchMin[0][1];
      //下班 允许最早打卡分钟数
      String punchMin10 = workPunchMin[1][0];
      //下班 允许最晚打卡分钟数
      String punchMin11 = workPunchMin[1][1];

      String onDateTime = tmpDate+" "+onWorkTime;
      String offDateTime = tmpDate+" "+offWorkTime;

      if(!allAcrossWorkTime.isEmpty()){
        //当前班次存在跨天
        shiftInfoCominfoBean.setIsAcross("1");

        //如果有跨天，分为几种情况 当前工作时段跨天，当前工作时段不跨天
        if(!"1".equalsIgnoreCase(onWorkTimeIsAcross) && !"1".equalsIgnoreCase(offWorkTimeIsAcross)){
          //当前工作时段不跨天的
          workArr[0] = onWorkTime;
          workArr[1] = offWorkTime;
          workTime.add(workArr);

          workIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
          workIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
          workTimeIndex.add(workIndexArr);

          workAcrossArr[0] = onWorkTime;
          workAcrossArr[1] = offWorkTime;
          workAcrossTime.add(workAcrossArr);

          workAcrossIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
          workAcrossIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
          workAcrossTimeIndex.add(workAcrossIndexArr);

          Map<String,String> longTimeMap = new HashMap<>();
          longTimeMap.put("bengintime", onWorkTime);
          longTimeMap.put("endtime", offWorkTime);
          longTimeMap.put("bengintime_across", "0");
          longTimeMap.put("endtime_across", "0");
          workAcrossLongTime.add(longTimeMap);

          eachWorkMins.add(kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime)-kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime));

          workLongIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
          workLongIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
          workLongTimeIndex.add(workLongIndexArr);

          Map<String,String> isSignAcrossMap = new HashMap<>();
          isSignAcrossMap.put("onsign", "0");
          isSignAcrossMap.put("offsign", "0");
          isSignAcrossMap.put("bengintime", onWorkTime);
          isSignAcrossMap.put("endtime", offWorkTime);

          buildSignList(signWorkTime,onDateTime,offDateTime,Util.getIntValue(punchMin00),Util.getIntValue(punchMin11),isSignAcrossMap,Util.getIntValue(punchMin01),Util.getIntValue(punchMin10),isFirst);

          buildTimeList(timelineList, "on", shiftInfoCominfoBean.getSerialid(), "0",  onWorkTime, punchMin00,workmins,isSignAcrossMap, punchMin01);
          buildTimeList(timelineList, "off", shiftInfoCominfoBean.getSerialid(), "0", offWorkTime, punchMin11,workmins,isSignAcrossMap, punchMin10);

        }else{
          //当前工作时段存在跨天的 ，只有两种情况 （上班不跨天，下班跨天）和（上班下班都跨天的）
          if(!"1".equalsIgnoreCase(onWorkTimeIsAcross) && "1".equalsIgnoreCase(offWorkTimeIsAcross)){
//            这是上班不跨天，下班跨天
            preWorkArr[0] = "00:00";
            preWorkArr[1] = offWorkTime;
            preWorkTime.add(preWorkArr);

            preWorkIndexArr[0] = 0;
            preWorkIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
            preWorkTimeIndex.add(preWorkIndexArr);

            workArr[0] = onWorkTime;
            workArr[1] = "24:00";
            workTime.add(workArr);

            workIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
            workIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes("24:00");
            workTimeIndex.add(workIndexArr);

            workAcrossArr[0] = onWorkTime;
            workAcrossArr[1] = offWorkTime;
            workAcrossTime.add(workAcrossArr);

            workAcrossIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
            workAcrossIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
            workAcrossTimeIndex.add(workAcrossIndexArr);

            String longofftime = kqTimesArrayComInfo.turn24to48Time(offWorkTime);

            Map<String,String> longTimeMap = new HashMap<>();
            longTimeMap.put("bengintime", onWorkTime);
            longTimeMap.put("endtime", longofftime);
            longTimeMap.put("bengintime_across", "0");
            longTimeMap.put("endtime_across", "1");
            workAcrossLongTime.add(longTimeMap);

            eachWorkMins.add(1440-kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime)+kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime));

            workLongIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
            workLongIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(longofftime);
            workLongTimeIndex.add(workLongIndexArr);

            Map<String,String> isSignAcrossMap = new HashMap<>();
            isSignAcrossMap.put("onsign", "0");
            isSignAcrossMap.put("offsign", "1");
            isSignAcrossMap.put("bengintime", onWorkTime);
            isSignAcrossMap.put("endtime", offWorkTime);

            buildSignList(signWorkTime,onDateTime,offDateTime,Util.getIntValue(punchMin00),Util.getIntValue(punchMin11),isSignAcrossMap,
                Util.getIntValue(punchMin01), Util.getIntValue(punchMin10),isFirst);

            buildTimeList(timelineList, "on", shiftInfoCominfoBean.getSerialid(), "0", onWorkTime, punchMin00,workmins,isSignAcrossMap,
                punchMin01);
            buildTimeList(timelineList, "off", shiftInfoCominfoBean.getSerialid(), "1", offWorkTime, punchMin11,workmins,isSignAcrossMap,
                punchMin10);

          }else if("1".equalsIgnoreCase(onWorkTimeIsAcross) && "1".equalsIgnoreCase(offWorkTimeIsAcross)){
//            这是上班下班都跨天的
            preWorkArr[0] = onWorkTime;
            preWorkArr[1] = offWorkTime;
            preWorkTime.add(preWorkArr);

            preWorkIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
            preWorkIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
            preWorkTimeIndex.add(preWorkIndexArr);

            workAcrossArr[0] = onWorkTime;
            workAcrossArr[1] = offWorkTime;
            workAcrossTime.add(workAcrossArr);

            workIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
            workIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes("24:00");
            workTimeIndex.add(workIndexArr);

            workAcrossIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
            workAcrossIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
            workAcrossTimeIndex.add(workAcrossIndexArr);

            String longontime =  kqTimesArrayComInfo.turn24to48Time(onWorkTime);
            String longofftime = kqTimesArrayComInfo.turn24to48Time(offWorkTime);

            Map<String,String> longTimeMap = new HashMap<>();
            longTimeMap.put("bengintime", longontime);
            longTimeMap.put("endtime", longofftime);
            longTimeMap.put("bengintime_across", "1");
            longTimeMap.put("endtime_across", "1");
            workAcrossLongTime.add(longTimeMap);
            eachWorkMins.add(kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime)-kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime));

            workLongIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(longontime);
            workLongIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(longofftime);
            workLongTimeIndex.add(workLongIndexArr);

            Map<String,String> isSignAcrossMap = new HashMap<>();
            isSignAcrossMap.put("onsign", "1");
            isSignAcrossMap.put("offsign", "1");
            isSignAcrossMap.put("bengintime", onWorkTime);
            isSignAcrossMap.put("endtime", offWorkTime);

            buildSignList(signWorkTime,onDateTime,offDateTime,Util.getIntValue(punchMin00),Util.getIntValue(punchMin11),isSignAcrossMap,
                Util.getIntValue(punchMin01), Util.getIntValue(punchMin10),isFirst);

            buildTimeList(timelineList, "on", shiftInfoCominfoBean.getSerialid(), "1", onWorkTime, punchMin00,workmins,isSignAcrossMap,
                punchMin01);
            buildTimeList(timelineList, "off", shiftInfoCominfoBean.getSerialid(), "1", offWorkTime, punchMin11,workmins,isSignAcrossMap,
                punchMin10);

          }
        }
      }else{
        //如果没有跨天
        workArr[0] = onWorkTime;
        workArr[1] = offWorkTime;
        workTime.add(workArr);

        workIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
        workIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
        workTimeIndex.add(workIndexArr);

        workAcrossArr[0] = onWorkTime;
        workAcrossArr[1] = offWorkTime;
        workAcrossTime.add(workAcrossArr);


        workAcrossIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
        workAcrossIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
        workAcrossTimeIndex.add(workAcrossIndexArr);

        Map<String,String> longTimeMap = new HashMap<>();
        longTimeMap.put("bengintime", onWorkTime);
        longTimeMap.put("endtime", offWorkTime);
        longTimeMap.put("bengintime_across", "0");
        longTimeMap.put("endtime_across", "0");
        workAcrossLongTime.add(longTimeMap);

        eachWorkMins.add(kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime)-kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime));

        workLongIndexArr[0] = kqTimesArrayComInfo.getArrayindexByTimes(onWorkTime);
        workLongIndexArr[1] = kqTimesArrayComInfo.getArrayindexByTimes(offWorkTime);
        workLongTimeIndex.add(workLongIndexArr);

        Map<String,String> isSignAcrossMap = new HashMap<>();
        isSignAcrossMap.put("onsign", "0");
        isSignAcrossMap.put("offsign", "0");
        isSignAcrossMap.put("bengintime", onWorkTime);
        isSignAcrossMap.put("endtime", offWorkTime);

        buildSignList(signWorkTime,onDateTime,offDateTime,Util.getIntValue(punchMin00),Util.getIntValue(punchMin11),isSignAcrossMap,
            Util.getIntValue(punchMin01), Util.getIntValue(punchMin10),isFirst);

        buildTimeList(timelineList, "on", shiftInfoCominfoBean.getSerialid(), "0", onWorkTime, punchMin00,workmins,isSignAcrossMap,
            punchMin01);
        buildTimeList(timelineList, "off", shiftInfoCominfoBean.getSerialid(), "0", offWorkTime, punchMin11,workmins,isSignAcrossMap,
            punchMin10);
      }

      i = i + 2;
      j++;
    }
  }

  /**
   * 填充允许打卡的范围list集合
   * @param signWorkTime 最终返回的集合
   * @param onDateTime 上班开始时间
   * @param offDateTime 下班结束时间
   * @param punchMin00 上班前多久开始打卡
   * @param punchMin11 下班后多久结束打卡
   * @param isSignAcrossMap 上班工作时间是否跨天
   * @param punchMin01 上班后多久停止打卡
   * @param punchMin10 上班前多久开始打卡
   * @param isFirst 多次打卡的时候是否是第一次打卡 只有第一次打卡的上班卡允许打卡才会跨到前一天
   */
  private void buildSignList(List<Map<String, String>> signWorkTime, String onDateTime,
      String offDateTime, int punchMin00, int punchMin11, Map<String, String> isSignAcrossMap,
      int punchMin01, int punchMin10,boolean isFirst){
    Map<String,String> signMap = new HashMap<>();

    //上班前 允许最早打卡是否跨天
    boolean isSignAcross00 = false;
    //上班 允许最晚打卡是否跨天
    boolean isSignAcross01 = false;
    //下班后 允许最早打卡是否跨天
    boolean isSignAcross10 = false;
    //下班后 允许最晚打卡是否跨天
    boolean isSignAcross11 = false;

    //工作时段是否跨天
    boolean workAcross0 = "1".equalsIgnoreCase(Util.null2String(isSignAcrossMap.get("onsign")));
    //工作时段是否跨天
    boolean workAcross1 = "1".equalsIgnoreCase(Util.null2String(isSignAcrossMap.get("offsign")));

    signMap.put("workbengintime", Util.null2String(isSignAcrossMap.get("bengintime")));
    signMap.put("workendtime", Util.null2String(isSignAcrossMap.get("endtime")));
    signMap.put("workbengintime_across", Util.null2String(isSignAcrossMap.get("onsign")));
    signMap.put("workendtime_across", Util.null2String(isSignAcrossMap.get("offsign")));

    LocalDateTime onLocalDateTime = LocalDateTime.parse(onDateTime,dateTimeFormatter);
    if(punchMin00 > 0){
      boolean is_pre_across = false;
      LocalDateTime canOnLocalDateTime = onLocalDateTime.minusMinutes(punchMin00);
      if(workAcross0){
        if(onLocalDateTime.toLocalDate().isEqual(canOnLocalDateTime.toLocalDate())){
          isSignAcross00 = true;
        }
      }else{
        //如果是上班前的话，只要当前工作时间不跨天，上班前多久都不会再跨天了，只会跨到前一天
        if(onLocalDateTime.toLocalDate().isAfter(canOnLocalDateTime.toLocalDate())){
          if(isFirst){
            is_pre_across = true;
          }
        }
      }
      buildSignTimeMap(canOnLocalDateTime,isSignAcross00,"on",signMap,"start");
      if(is_pre_across){
        signMap.put("bengintime_pre_across", "1");
        isSignAcrossMap.put("bengintime_pre_across", "1");
      }
    }
    if(punchMin01 > -1){
      LocalDateTime endOnLocalDateTime = onLocalDateTime.plusMinutes(punchMin01);
      if(workAcross0){
        if(onLocalDateTime.toLocalDate().isEqual(endOnLocalDateTime.toLocalDate())){
          isSignAcross01 = true;
        }
      }else{
        if(onLocalDateTime.toLocalDate().isBefore(endOnLocalDateTime.toLocalDate())){
          isSignAcross01 = true;
          isSignAcrossMap.put("onsign_end", "1");
        }else {
          isSignAcross01 = false;
        }
      }
      buildSignTimeMap(endOnLocalDateTime,isSignAcross01,"on",signMap,"end");
      if(punchMin10 < 0){
        LocalDateTime tmpStartOffLocalDateTime = endOnLocalDateTime.plusMinutes(1);
        if(tmpStartOffLocalDateTime.toLocalDate().isAfter(endOnLocalDateTime.toLocalDate())){
          buildSignTimeMap(tmpStartOffLocalDateTime,true,"off",signMap,"start");
        }else{
          buildSignTimeMap(tmpStartOffLocalDateTime,isSignAcross01,"off",signMap,"start");
        }
      }
    }
    LocalDateTime offLocalDateTime = LocalDateTime.parse(offDateTime,dateTimeFormatter);
    if(punchMin10 > -1){
      LocalDateTime startOffLocalDateTime = offLocalDateTime.minusMinutes(punchMin10);
      if(workAcross1){
        if(offLocalDateTime.toLocalDate().isEqual(startOffLocalDateTime.toLocalDate())){
          isSignAcross10 = true;
          isSignAcrossMap.put("offsign_start", "1");
        }
      }else{
        if(offLocalDateTime.toLocalDate().isEqual(startOffLocalDateTime.toLocalDate())){
        }else {
          isSignAcross10 = true;
          isSignAcrossMap.put("offsign_start", "1");
        }
      }
      buildSignTimeMap(startOffLocalDateTime,isSignAcross10,"off",signMap,"start");
      if(punchMin01 < 0){
        LocalDateTime tmpEndOnLocalDateTime = startOffLocalDateTime.minusMinutes(1);
        buildSignTimeMap(tmpEndOnLocalDateTime,isSignAcross10,"on",signMap,"end");
      }
    }
    if(punchMin11 > 0){
      LocalDateTime canOffLocalDateTime = offLocalDateTime.plusMinutes(punchMin11);
      if(workAcross1){
        isSignAcross11 = true;
      }else{
        if(offLocalDateTime.toLocalDate().isBefore(canOffLocalDateTime.toLocalDate())){
          isSignAcross11 = true;
          isSignAcrossMap.put("offsign", "1");
        }
      }
      buildSignTimeMap(canOffLocalDateTime,isSignAcross11,"off",signMap,"end");
    }
    signWorkTime.add(signMap);
  }

  /**
   * 填充允许打卡的map集合
   * @param canLocalDateTime
   * @param isSignAcross
   * @param type
   * @param signMap
   * @param punchMinsType 允许打卡的类型，是上班前开始还是上班后结束
   */
  private void buildSignTimeMap(LocalDateTime canLocalDateTime, boolean isSignAcross, String type,
      Map<String, String> signMap, String punchMinsType){
    String signTime = canLocalDateTime.toLocalTime().format(timeFormatter);
    String isAcross = isSignAcross?"1":"0";
    if("on".equalsIgnoreCase(type)){
      if("end".equalsIgnoreCase(punchMinsType)){
        signMap.put("bengintime_end", signTime);
        signMap.put("bengintime_end_across", isAcross);
      }else{
        signMap.put("bengintime", signTime);
        signMap.put("bengintime_across", isAcross);
      }
    }else{
      if("start".equalsIgnoreCase(punchMinsType)){
        signMap.put("endtime_start", signTime);
        signMap.put("endtime_start_across", isAcross);
      }else{
        signMap.put("endtime", signTime);
        signMap.put("endtime_across", isAcross);
      }
    }
  }



  /**
   * @param timelineList
   * @param type 签到签退标识
   * @param serialid 班次id
   * @param across 时间是否跨天
   * @param time 上下班时间
   * @param min 打卡时段控制
   * @param workmins
   * @param isSignAcrossMap 允许打卡时间是否跨天
   * @param min_next
   */
  private void buildTimeList(List<Object> timelineList, String type, String serialid, String across,
      String time, String min, int workmins, Map<String, String> isSignAcrossMap,
      String min_next){
    Map<String,Object> timelineMap = new HashMap<>();

    timelineMap = new HashMap<>();
    timelineMap.put("type", type);
    timelineMap.put("serialid", serialid);
    timelineMap.put("isacross", across);
    timelineMap.put("time", time);
    timelineMap.put("min", min);
    timelineMap.put("min_next", min_next);
    timelineMap.put("isPunchOpen", "1");
    timelineMap.put("workmins",workmins);
    if("on".equalsIgnoreCase(type)){
      timelineMap.put("signAcross","1".equalsIgnoreCase(Util.null2String(isSignAcrossMap.get("onsign")))?"1":"0");
      timelineMap.put("signAcross_next","1".equalsIgnoreCase(Util.null2String(isSignAcrossMap.get("onsign_end")))?"1":"0");
      if(isSignAcrossMap.containsKey("bengintime_pre_across")){
        timelineMap.put("sign_preAcross","1");
      }
    }else{
      timelineMap.put("signAcross","1".equalsIgnoreCase(Util.null2String(isSignAcrossMap.get("offsign")))?"1":"0");
      timelineMap.put("signAcross_next","1".equalsIgnoreCase(Util.null2String(isSignAcrossMap.get("offsign_start")))?"1":"0");
    }
    timelineList.add(timelineMap);
  }

  private void buildWorkIndex(Map<String, Object> workMap, String[][] workMinsAcrossArr,
      List<String> allAcrossWorkTime, List<String> allWorkTime, List<String> allLongWorkTime,
      int index, KQTimesArrayComInfo kqTimesArrayComInfo,
      List<String> allWorkTimeisAcross){

    if(workMap == null || workMap.isEmpty()){
      return ;
    }
    String across = Util.null2String(workMap.get("across"));
    String times = Util.null2String(workMap.get("times"));
    int mins = Util.getIntValue(Util.null2String(workMap.get("mins")), 0);
    int mins_next = Util.getIntValue(Util.null2String(workMap.get("mins_next")), -1);

    if(times.length() == 0){
      return ;
    }

    String longtime = times;

    allWorkTime.add(times);
    allWorkTimeisAcross.add(across);
    if("1".equalsIgnoreCase(across)){
      allAcrossWorkTime.add(times);
      longtime = kqTimesArrayComInfo.turn24to48Time(times);
    }
    allLongWorkTime.add(longtime);
    if(index ==0){
      //上班时间打卡区间
      if(workMinsAcrossArr != null){
        workMinsAcrossArr[index][0] = ""+mins;
        workMinsAcrossArr[index][1] = ""+mins_next;
      }
    }else{
      //下班时间打卡区间
      if(workMinsAcrossArr != null){
        workMinsAcrossArr[index][0] = ""+mins_next;
        workMinsAcrossArr[index][1] = ""+mins;
      }
    }
  }

  /**
   * 处理休息时间
   */
  public void setRestDuration(List<Object> restTimes,ShiftInfoCominfoBean shiftInfoCominfoBean) throws Exception {
    KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

    List<String> allRestTime = shiftInfoCominfoBean.getAllRestTime();
    List<String> allAcrossRestTime = shiftInfoCominfoBean.getAllAcrossRestTime();

    List<String[]> preRestTime = shiftInfoCominfoBean.getPreRestTime();
    List<String[]> restTime = shiftInfoCominfoBean.getRestTime();
    List<String[]> restAcrossTime = shiftInfoCominfoBean.getRestAcrossTime();

    List<int[]> preRestTimeIndex = shiftInfoCominfoBean.getPreRestTimeIndex();
    List<int[]> restTimeIndex = shiftInfoCominfoBean.getRestTimeIndex();
    List<int[]> restAcrossTimeIndex = shiftInfoCominfoBean.getRestAcrossTimeIndex();

    List<Map<String, String>> restAcrossLongTime = shiftInfoCominfoBean.getRestAcrossLongTime();
    List<int[]> restLongTimeIndex = shiftInfoCominfoBean.getRestLongTimeIndex();

    if (restTimes != null && !restTimes.isEmpty()) {
      for(int k = 0 ; k < restTimes.size() ; k++){
        List<Object> sectionList = (List<Object>)restTimes.get(k);
        if(sectionList == null || sectionList.isEmpty()){
          break;
        }
        if(sectionList.size() == 2) {
          Map<String, Object> restMap0 = (Map<String, Object>) sectionList.get(0);
          buildRestIndex(restMap0,allAcrossRestTime,allRestTime,0);
          Map<String, Object> restMap1 = (Map<String, Object>) sectionList.get(1);
          buildRestIndex(restMap1,allAcrossRestTime,allRestTime,1);
        }
      }
    }

    for(int i = 0 ; i < allRestTime.size() ; ){

      String[] preRestArr = new String[2];
      String[] restkArr = new String[2];
      String[] restAcrossArr = new String[2];

      int[] preRestArrIndex = new int[2];
      int[] restkArrIndex = new int[2];
      int[] restAcrossArrIndex = new int[2];
      int[] restLongArrIndex = new int[2];
      if(allRestTime.size() % 2 != 0){
        break;
      }

      String onRestTime = Util.null2String(allRestTime.get(i));
      String offRestTime = Util.null2String(allRestTime.get(i+1));

      if(!allAcrossRestTime.isEmpty()){

        //休息时段跨天只有两种可能 （休息开始时间不跨天，结束时间跨天）和（休息开始和结束时间都跨天）
        if(!allAcrossRestTime.contains(onRestTime) && allAcrossRestTime.contains(offRestTime)){
//            这是休息开始时间不跨天，结束时间跨天
          preRestArr[0] = "00:00";
          preRestArr[1] = offRestTime;
          preRestTime.add(preRestArr);

          preRestArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes("00:00");
          preRestArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
          preRestTimeIndex.add(preRestArrIndex);

          restkArr[0] = onRestTime;
          restkArr[1] = "24:00";
          restTime.add(restkArr);

          restkArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
          restkArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes("24:00");
          restTimeIndex.add(restkArrIndex);

          restAcrossArr[0] = onRestTime;
          restAcrossArr[1] = offRestTime;
          restAcrossTime.add(restAcrossArr);

          restAcrossArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
          restAcrossArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
          restAcrossTimeIndex.add(restAcrossArrIndex);

          String longofftime = offRestTime;
          String[] strTimes = offRestTime.split(":");
          String strTimesHead = ""+(Integer.valueOf(strTimes[0])+24);
          String strTimesBody = strTimes[1];
          longofftime = strTimesHead+":"+strTimesBody;

          Map<String,String> longTimeMap = new HashMap<>();
          longTimeMap.put("bengintime", onRestTime);
          longTimeMap.put("endtime", longofftime);
          longTimeMap.put("bengintime_across", "0");
          longTimeMap.put("endtime_across", "1");
          restAcrossLongTime.add(longTimeMap);

          restLongArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
          restLongArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(longofftime);
          restLongTimeIndex.add(restLongArrIndex);

        }else if(allAcrossRestTime.contains(onRestTime) && allAcrossRestTime.contains(offRestTime)){
//            这是休息开始和结束时间都是跨天的
          preRestArr[0] = onRestTime;
          preRestArr[1] = offRestTime;
          preRestTime.add(preRestArr);

          preRestArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
          preRestArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
          preRestTimeIndex.add(preRestArrIndex);

          restAcrossArr[0] = onRestTime;
          restAcrossArr[1] = offRestTime;
          restAcrossTime.add(restAcrossArr);

          restAcrossArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
          restAcrossArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
          restAcrossTimeIndex.add(restAcrossArrIndex);

          String longontime = onRestTime;
          String[] onstrTimes = onRestTime.split(":");
          String onstrTimesHead = ""+(Integer.valueOf(onstrTimes[0])+24);
          String onstrTimesBody = onstrTimes[1];
          longontime = onstrTimesHead+":"+onstrTimesBody;

          String longofftime = offRestTime;
          String[] offstrTimes = offRestTime.split(":");
          String offstrTimesHead = ""+(Integer.valueOf(offstrTimes[0])+24);
          String pffstrTimesBody = offstrTimes[1];
          longofftime = offstrTimesHead+":"+pffstrTimesBody;

          Map<String,String> longTimeMap = new HashMap<>();
          longTimeMap.put("bengintime", longontime);
          longTimeMap.put("endtime", longofftime);
          longTimeMap.put("bengintime_across", "1");
          longTimeMap.put("endtime_across", "1");
          restAcrossLongTime.add(longTimeMap);

          restLongArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(longontime);
          restLongArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(longofftime);
          restLongTimeIndex.add(restLongArrIndex);
        }
      }else{

        //如果没有跨天
        restkArr[0] = onRestTime;
        restkArr[1] = offRestTime;
        restTime.add(restkArr);

        restkArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
        restkArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
        restTimeIndex.add(restkArrIndex);

        restAcrossArr[0] = onRestTime;
        restAcrossArr[1] = offRestTime;
        restAcrossTime.add(restAcrossArr);

        restAcrossArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
        restAcrossArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
        restAcrossTimeIndex.add(restAcrossArrIndex);

        Map<String,String> longTimeMap = new HashMap<>();
        longTimeMap.put("bengintime", onRestTime);
        longTimeMap.put("endtime", offRestTime);
        longTimeMap.put("bengintime_across", "0");
        longTimeMap.put("endtime_across", "0");
        restAcrossLongTime.add(longTimeMap);

        restLongArrIndex[0] = kqTimesArrayComInfo.getArrayindexByTimes(onRestTime);
        restLongArrIndex[1] = kqTimesArrayComInfo.getArrayindexByTimes(offRestTime);
        restLongTimeIndex.add(restLongArrIndex);
      }

      i = i + 2;
    }
  }

  private void buildRestIndex(Map<String,Object> restMap,List<String> allAcrossRestTime,List<String> allRestTime,int index){

    if(restMap == null || restMap.isEmpty()){
      return ;
    }
    String across = Util.null2String(restMap.get("across"));
    String times = Util.null2String(restMap.get("times"));

    if(times.length() == 0){
      return ;
    }

    if("1".equalsIgnoreCase(across)){
      allAcrossRestTime.add(times);
    }
    allRestTime.add(times);
  }

}
