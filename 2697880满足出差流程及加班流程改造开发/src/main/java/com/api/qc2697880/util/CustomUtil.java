package com.api.qc2697880.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.*;
import com.engine.kq.biz.chain.shiftinfo.ShiftInfoBean;
import com.engine.kq.entity.KQOvertimeRulesDetailEntity;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.timer.KQOvertimeCardBean;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CustomUtil {
    public static KQLog kqLog = new KQLog();

    /**
     * 判断人员休息日是否包含出差流程，如果有出差流程就不需要打卡，以流程为准
     * @return
     */
    public Boolean isBusiness(String resourceId,String kqDate){
        Boolean isBusiness = false;
        int changeType = getChangeType(resourceId,kqDate);
        //判断是不是休息日，不是休息日就走系统标准
        if (changeType == 3){
            RecordSet rs = new RecordSet();
            String sql = "select * from kq_flow_split_evection where resourceid='"+resourceId+"' and belongdate='"+kqDate+"' ";
            new BaseBean().writeLog("休息日是否有出差流程" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                isBusiness = true;
            }
        }
        return isBusiness;
    }

    /**
     * 这里获取当天类型
     * @param resourceId    人员id
     * @param date          日期
     * @return
     */
    public  int getChangeType(String resourceId, String date) {
        int changeType = -1;
        //1-节假日、2-工作日、3-休息日
        /*获取考勤组的ID，因为考勤组有有效期，所以需要传入日期*/
        KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
        String groupId = kqGroupMemberComInfo.getKQGroupId(resourceId, date);

        /*该人员不存在于任意一个考勤组中，请为其设置考勤组*/
        if(groupId.equals("")){
            new BaseBean().writeLog("该人员不存在于任意一个考勤组中，请为其设置考勤组。resourceId=" + resourceId + ",date=" + date);
        }

        changeType = KQHolidaySetBiz.getChangeType(groupId, date);
        if (changeType != 1 && changeType != 2 && changeType != 3) {
            KQWorkTime kqWorkTime = new KQWorkTime();
            changeType = kqWorkTime.isWorkDay(resourceId, date) ? 2 : 3;
        }
        return changeType;
    }



    /**
     *  现在工作日，节假日和休息日都可以设置排除休息时间,如果开启统计休息时长就填为0
     * @param restTimeMap
     * @param changeType_key
     * @param kqTimesArrayComInfo
     * @param shouldAcross
     * @param initArrays
     */
    public void handle_resttime_remove(Map<String, List<String[]>> restTimeMap, String changeType_key, KQTimesArrayComInfo kqTimesArrayComInfo, boolean shouldAcross, int[] initArrays) {

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
                        Arrays.fill(initArrays, restStart, restEnd, 0);
                    }
                }
            }
        }
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
                            new BaseBean().writeLog("==zj==(打卡休息时长)" + restStart +  " | " + restEnd);
                            if(restStart < restEnd){
                                Arrays.fill(initArrays, restStart, restEnd, 1);
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
        clearWorkAndRestTime(resourceid, DateUtil.addDate(splitDate, 1),isNextDay, kqTimesArrayComInfo,
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
                                Arrays.fill(initArrays, begintimeIndex, endtimeIndex, 1);
                            }
                        }
                    }

                }
            }
        }
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
     * 加班方式是第一种，然后再流程归档的时候把加班数据写到加班中间表里
     * @param splitBean
     */
    public static void doComputingMode1_splitBean(SplitBean splitBean) {
        try{
            kqLog.info("doComputingMode1_splitBean:splitBean: "+ (splitBean != null ? JSON.toJSONString(splitBean) : "null"));
            RecordSet rs = new RecordSet();
            RecordSet rs1 = new RecordSet();
            int changeType = splitBean.getChangeType();
            String requestId = splitBean.getRequestId();
            String resourceId = splitBean.getResourceId();
            String belongDate = splitBean.getBelongDate();
            String duration = splitBean.getDuration();
            String durationrule = splitBean.getDurationrule();
            String fromdateDB = splitBean.getFromdatedb();
            String fromtimedb = splitBean.getFromtimedb();
            String todatedb = splitBean.getTodatedb();
            String totimedb = splitBean.getTotimedb();
            String fromdate = splitBean.getFromDate();
            String fromtime = splitBean.getFromTime();
            String todate = splitBean.getToDate();
            String totime = splitBean.getToTime();
            double D_Mins = splitBean.getD_Mins();
            Map<String,Integer> changeTypeMap = Maps.newHashMap();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
            KQOverTimeRuleCalBiz kqOverTimeRuleCalBiz = new KQOverTimeRuleCalBiz();
            Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap = Maps.newHashMap();
            Map<String, List<String[]>> restTimeMap = Maps.newHashMap();
            Map<String,Integer> computingModeMap = Maps.newHashMap();
            //先获取一些前提数据，加班規則和假期規則
            kqOverTimeRuleCalBiz.getOverTimeDataMap(resourceId, belongDate, belongDate, dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,computingModeMap);
            String overtime_type = splitBean.getOvertime_type();
            String changeType_key = belongDate + "_" + changeType;
            if(!overRulesDetailMap.containsKey(changeType_key)){
                return;
            }
            KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
            int paidLeaveEnable = kqOverTimeRuleCalBiz.getPaidLeaveEnable(kqOvertimeRulesDetailEntity,overtime_type);
            boolean needSplitByTime = false;//kqOverTimeRuleCalBiz.getNeedSplitByTime(kqOvertimeRulesDetailEntity,paidLeaveEnable);

            int unit = KQOvertimeRulesBiz.getMinimumUnit();
            if(needSplitByTime){
                List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = kqOverTimeRuleCalBiz.getOvertimeBalanceTimeBean(resourceId, fromdate, fromtime, todate, totime, changeTypeMap, kqOvertimeRulesDetailEntity, splitBean, restTimeMap,
                        overRulesDetailMap);
                if(overtimeBalanceTimeBeans != null && !overtimeBalanceTimeBeans.isEmpty()){
                    for(int i = 0 ; i < overtimeBalanceTimeBeans.size() ;i++){
                        OvertimeBalanceTimeBean overtimeBalanceTimeBean = overtimeBalanceTimeBeans.get(i);
                        String timepoint = overtimeBalanceTimeBean.getTimepoint();
                        boolean needTX = overtimeBalanceTimeBean.isNeedTX();
                        String tiaoxiu_id = "";
                        String overtime_tiaoxiu_id = "";
                        if(needTX){
                            String split_key = splitBean.getRequestId()+"_"+splitBean.getDataId()+"_"+splitBean.getDetailId()+"_"
                                    +splitBean.getFromDate()+"_"+splitBean.getFromTime()+"_"+splitBean.getToDate()+"_"+
                                    splitBean.getToTime()+"_"+splitBean.getD_Mins()+"_"+timepoint;;
                            String check_tiaoxiu_sql = "select * from kq_overtime_tiaoxiu where split_key=? ";
                            rs1.executeQuery(check_tiaoxiu_sql, split_key);
                            if(rs1.next()){
                                overtime_tiaoxiu_id = rs1.getString("id");
                                tiaoxiu_id = rs1.getString("tiaoxiu_id");
                            }
                        }
                        int timepoint_mins = overtimeBalanceTimeBean.getTimepoint_mins();
                        if(timepoint_mins == 0){
                            continue;
                        }
                        String timePointStart = overtimeBalanceTimeBean.getTimepoint_start();
                        String timePointEnd = overtimeBalanceTimeBean.getTimepoint_end();
                        int timePointStart_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointStart);
                        int timePointEnd_index = kqTimesArrayComInfo.getArrayindexByTimes(timePointEnd);
                        String pointFromtime = timePointStart+":00";
                        String pointTotime = timePointEnd + ":00";
                        String flow_overtime_sql = "insert into kq_flow_overtime(requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,workMins,durationrule,changetype,paidLeaveEnable,computingMode,fromdatedb,fromtimedb,todatedb,totimedb,tiaoxiuid)"+
                                " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                        boolean isUp = rs.executeUpdate(flow_overtime_sql, requestId,resourceId,fromdate,pointFromtime,todate,pointTotime,timepoint_mins,"",belongDate,"",
                                unit,changeType,paidLeaveEnable,"1",fromdateDB,fromtimedb,todatedb,totimedb,tiaoxiu_id);
                        if(!isUp){
                            kqLog.info("doComputingMode1 加班数据flow_overtime_sql记录失败！！！");
                        }else{
                            kqLog.info("doComputingMode1:flow_overtime_sql: "+flow_overtime_sql);
                            kqLog.info("doComputingMode1:requestId:"+requestId+":resourceId:"+resourceId+":fromdate:"+fromdate+":fromtime:"+fromtime
                                    +":todate:"+todate+":totime:"+totime+":D_Mins:"+D_Mins+":belongDate:"+belongDate+":unit:"+unit+":changeType:"+changeType
                                    +":paidLeaveEnable:"+paidLeaveEnable+":fromdateDB:"+fromdateDB+":fromtimedb:"+fromtimedb+":todatedb:"+todatedb+":totimedb:"+totimedb);
                        }
                        if(overtime_tiaoxiu_id.length() > 0 && Util.getIntValue(overtime_tiaoxiu_id) > 0){
                            String delSql = "delete from kq_overtime_tiaoxiu where id = ? ";
                            rs1.executeUpdate(delSql, overtime_tiaoxiu_id);
                        }
                    }
                }else{
                    String tiaoxiu_id = "";
                    String overtime_tiaoxiu_id = "";
                    String split_key = splitBean.getRequestId()+"_"+splitBean.getDataId()+"_"+splitBean.getDetailId()+"_"
                            +splitBean.getFromDate()+"_"+splitBean.getFromTime()+"_"+splitBean.getToDate()+"_"+
                            splitBean.getToTime()+"_"+splitBean.getD_Mins();
                    String check_tiaoxiu_sql = "select * from kq_overtime_tiaoxiu where split_key=? ";
                    rs1.executeQuery(check_tiaoxiu_sql, split_key);
                    if(rs1.next()){
                        overtime_tiaoxiu_id = rs1.getString("id");
                        tiaoxiu_id = rs1.getString("tiaoxiu_id");
                    }

                    fromtime = fromtime+":00";
                    totime = totime + ":00";
                    String flow_overtime_sql = "insert into kq_flow_overtime(requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,workMins,durationrule,changetype,paidLeaveEnable,computingMode,fromdatedb,fromtimedb,todatedb,totimedb,tiaoxiuid)"+
                            " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                    boolean isUp = rs.executeUpdate(flow_overtime_sql, requestId,resourceId,fromdate,fromtime,todate,totime,D_Mins,"",belongDate,"",
                            unit,changeType,paidLeaveEnable,"1",fromdateDB,fromtimedb,todatedb,totimedb,tiaoxiu_id);
                    if(!isUp){
                        kqLog.info("doComputingMode1 加班数据flow_overtime_sql记录失败！！！");
                    }else{
                        kqLog.info("doComputingMode1:flow_overtime_sql: "+flow_overtime_sql);
                        kqLog.info("doComputingMode1:requestId:"+requestId+":resourceId:"+resourceId+":fromdate:"+fromdate+":fromtime:"+fromtime
                                +":todate:"+todate+":totime:"+totime+":D_Mins:"+D_Mins+":belongDate:"+belongDate+":unit:"+unit+":changeType:"+changeType
                                +":paidLeaveEnable:"+paidLeaveEnable+":fromdateDB:"+fromdateDB+":fromtimedb:"+fromtimedb+":todatedb:"+todatedb+":totimedb:"+totimedb);
                    }
                    if(overtime_tiaoxiu_id.length() > 0 && Util.getIntValue(overtime_tiaoxiu_id) > 0){
                        String delSql = "delete from kq_overtime_tiaoxiu where id = ? ";
                        rs1.executeUpdate(delSql, overtime_tiaoxiu_id);
                    }
                }
            }else{

                String tiaoxiu_id = "";
                String overtime_tiaoxiu_id = "";

                String split_key = splitBean.getRequestId()+"_"+splitBean.getDataId()+"_"+splitBean.getDetailId()+"_"
                        +splitBean.getFromDate()+"_"+splitBean.getFromTime()+"_"+splitBean.getToDate()+"_"+
                        splitBean.getToTime()+"_"+splitBean.getD_Mins();
                String check_tiaoxiu_sql = "select * from kq_overtime_tiaoxiu where split_key=? ";
                rs1.executeQuery(check_tiaoxiu_sql, split_key);
                if(rs1.next()){
                    overtime_tiaoxiu_id = rs1.getString("id");
                    tiaoxiu_id = rs1.getString("tiaoxiu_id");
                }

                fromtime = fromtime+":00";
                totime = totime + ":00";
                String flow_overtime_sql = "insert into kq_flow_overtime(requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,workMins,durationrule,changetype,paidLeaveEnable,computingMode,fromdatedb,fromtimedb,todatedb,totimedb,tiaoxiuid)"+
                        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                boolean isUp = rs.executeUpdate(flow_overtime_sql, requestId,resourceId,fromdate,fromtime,todate,totime,D_Mins,"",belongDate,"",
                        unit,changeType,paidLeaveEnable,"1",fromdateDB,fromtimedb,todatedb,totimedb,tiaoxiu_id);
                if(!isUp){
                    kqLog.info("doComputingMode1 加班数据flow_overtime_sql记录失败！！！");
                }else{
                    kqLog.info("doComputingMode1:flow_overtime_sql: "+flow_overtime_sql);
                    kqLog.info("doComputingMode1:requestId:"+requestId+":resourceId:"+resourceId+":fromdate:"+fromdate+":fromtime:"+fromtime
                            +":todate:"+todate+":totime:"+totime+":D_Mins:"+D_Mins+":belongDate:"+belongDate+":unit:"+unit+":changeType:"+changeType
                            +":paidLeaveEnable:"+paidLeaveEnable+":fromdateDB:"+fromdateDB+":fromtimedb:"+fromtimedb+":todatedb:"+todatedb+":totimedb:"+totimedb);
                }
                if(overtime_tiaoxiu_id.length() > 0 && Util.getIntValue(overtime_tiaoxiu_id) > 0){
                    String delSql = "delete from kq_overtime_tiaoxiu where id = ? ";
                    rs1.executeUpdate(delSql, overtime_tiaoxiu_id);
                }
            }

        }catch (Exception e){
            kqLog.info("加班生成数据报错:doComputingMode1_splitBean:");
            StringWriter errorsWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(errorsWriter));
            kqLog.info(errorsWriter.toString());
        }

    }

    /**
     * 加班方式是第一种，如果是先生成调休，后面才生成加班，需要先把调休给按照加班规则给生成出来
     * @param splitBean
     */
    public  void doComputingMode1_4TX(SplitBean splitBean) {
        RecordSet rs = new RecordSet();
        int changeType = splitBean.getChangeType();
        String resourceId = splitBean.getResourceId();
        String belongDate = splitBean.getBelongDate();
        String duration = splitBean.getDuration();
        String fromdateDB = splitBean.getFromdatedb();
        String requestId = splitBean.getRequestId();
        String fromdate = splitBean.getFromDate();
        String fromtime = splitBean.getFromTime();
        String todate = splitBean.getToDate();
        String totime = splitBean.getToTime();
        String overtime_type = splitBean.getOvertime_type();
        double D_Mins = splitBean.getD_Mins();
        int workMins = splitBean.getWorkmins();
        String workingHours = Util.null2String(workMins/60.0);
        Map<String,Integer> changeTypeMap = Maps.newHashMap();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        KQOverTimeRuleCalBiz kqOverTimeRuleCalBiz = new KQOverTimeRuleCalBiz();
        Map<String, KQOvertimeRulesDetailEntity> overRulesDetailMap = Maps.newHashMap();
        Map<String, List<String[]>> restTimeMap = Maps.newHashMap();
        Map<String,Integer> computingModeMap = Maps.newHashMap();
        //先获取一些前提数据，加班規則和假期規則
        kqOverTimeRuleCalBiz.getOverTimeDataMap(resourceId, belongDate, belongDate, dateFormatter,changeTypeMap,overRulesDetailMap,restTimeMap,computingModeMap);

        String changeType_key = belongDate + "_" + changeType;
        if(!overRulesDetailMap.containsKey(changeType_key)){
            return;
        }
//   根据加班单位重新生成下加班分钟数
        D_Mins = kqOverTimeRuleCalBiz.getD_MinsByUnit(D_Mins);

        KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity = overRulesDetailMap.get(changeType_key);
        int paidLeaveEnable = kqOverTimeRuleCalBiz.getPaidLeaveEnable(kqOvertimeRulesDetailEntity, overtime_type);
        boolean needSplitByTime = kqOverTimeRuleCalBiz.getNeedSplitByTime(kqOvertimeRulesDetailEntity, paidLeaveEnable);
        if(needSplitByTime) {
            List<OvertimeBalanceTimeBean> overtimeBalanceTimeBeans = kqOverTimeRuleCalBiz
                    .getOvertimeBalanceTimeBean(resourceId, fromdate, fromtime, todate, totime, changeTypeMap,
                            kqOvertimeRulesDetailEntity, splitBean, restTimeMap,overRulesDetailMap);
            if (overtimeBalanceTimeBeans != null && !overtimeBalanceTimeBeans.isEmpty()) {
                for(int i = 0 ; i < overtimeBalanceTimeBeans.size() ;i++){
                    OvertimeBalanceTimeBean overtimeBalanceTimeBean = overtimeBalanceTimeBeans.get(i);
                    String timepoint = overtimeBalanceTimeBean.getTimepoint();
                    boolean needTX = overtimeBalanceTimeBean.isNeedTX();
                    if(needTX){
                        Map<String,Object> otherParam = Maps.newHashMap();
                        otherParam.put("overtime_type", overtime_type);
                        otherParam.put("OvertimeBalanceTimeBean", overtimeBalanceTimeBean);
                        int timepoint_mins = overtimeBalanceTimeBean.getTimepoint_mins();
                        if(timepoint_mins == 0){
                            continue;
                        }
                        String tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceId,belongDate,timepoint_mins+"","0",workingHours,requestId,"1",fromdateDB,otherParam);
                        if(Util.getIntValue(tiaoxiuId) > 0){
                            //为啥要用kq_overtime_tiaoxiu这个表呢，因为kqpaidleaveaction可能和splitaction不是在同一个归档前节点
                            String split_key = splitBean.getRequestId()+"_"+splitBean.getDataId()+"_"+splitBean.getDetailId()+"_"
                                    +splitBean.getFromDate()+"_"+splitBean.getFromTime()+"_"+splitBean.getToDate()+"_"+
                                    splitBean.getToTime()+"_"+splitBean.getD_Mins()+"_"+timepoint;
                            String tiaoxiuId_sql = "insert into kq_overtime_tiaoxiu(split_key,tiaoxiu_id) values(?,?) ";
                            rs.executeUpdate(tiaoxiuId_sql, split_key,tiaoxiuId);
                            kqLog.info("doComputingMode1 加班生成调休成功！！！");
                        }else{
                            kqLog.info("doComputingMode1 加班生成调休失败！！！");
                        }
                    }
                }
            }else{
                //设置了按照时间区间生成调休，但是没有时间区间
            }
        }else{
            Map<String,Object> otherParam = Maps.newHashMap();
            otherParam.put("overtime_type", overtime_type);

            String tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceId,belongDate,D_Mins+"","0",workingHours,requestId,"1",fromdateDB,otherParam);
            if(Util.getIntValue(tiaoxiuId) > 0){
                //为啥要用kq_overtime_tiaoxiu这个表呢，因为kqpaidleaveaction可能和splitaction不是在同一个归档前节点
                String split_key = splitBean.getRequestId()+"_"+splitBean.getDataId()+"_"+splitBean.getDetailId()+"_"
                        +splitBean.getFromDate()+"_"+splitBean.getFromTime()+"_"+splitBean.getToDate()+"_"+
                        splitBean.getToTime()+"_"+splitBean.getD_Mins();
                String tiaoxiuId_sql = "insert into kq_overtime_tiaoxiu(split_key,tiaoxiu_id) values(?,?) ";
                rs.executeUpdate(tiaoxiuId_sql, split_key,tiaoxiuId);
                kqLog.info("doComputingMode1 加班生成调休成功！！！");
            }else{
                kqLog.info("doComputingMode1 加班生成调休失败！！！");
            }
        }

    }

    public void logOvertimeMap(Map<String, Object> overtimeLogMap,Object params, String keys){
        if(overtimeLogMap != null){
            overtimeLogMap.put(keys, params);
        }
    }
}
