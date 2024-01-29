package com.api.cusomization.qc2728806.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQFlowDataBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.enums.FlowReportTypeEnum;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class KqFormatCustomUtil {
String tableName = new BaseBean().getPropValue("qc2728806","tableName");
String fieldName = new BaseBean().getPropValue("qc2728806","fieldName");
String fieldResource = new BaseBean().getPropValue("qc2728806","fieldResource");

 /*   public Boolean isAbsenteeism(int checkIn, int checkOut, int absenteeismMins, String serialId){
        KQShiftManagementComInfo shiftManagementComInfo = new KQShiftManagementComInfo();
        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

        ShiftInfoCominfoBean shiftInfoCominfoBean = shiftManagementComInfo.getShiftInfoCominfoBean(serialId);//获取班次基本信息


        int halfpointIndex = kqTimesArrayComInfo.getArrayindexByTimes("12:00");//先默认以12:00为分界线
        int absenteeismDays = 0;
        //如果缺卡或者旷工，就算缺勤一天




        return true;
    }*/

    /**
     * 判断当天是否缺勤
     * @param checkIn
     * @param checkOut
     * @param absenteeismMins
     * @param resourceId
     * @param kqDate
     * @return
     */
    public Boolean isAbsenteeism(int checkIn, int checkOut, int absenteeismMins,String resourceId,String kqDate){
        Boolean isAbsenteeism = true;
        if (checkIn == 1 && checkOut == 1){
            return false;
        }

        try {
            List<Object> workFlow = null;
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
            int halfpointIndex = kqTimesArrayComInfo.getArrayindexByTimes("12:00");//先默认以12:00为分界线，分为上下午

            //如果当天有出差公出流程就不需要计算缺勤
                workFlow =  getFlowData(resourceId,kqDate);
                new BaseBean().writeLog("==zj==(所有考勤流程)" + JSON.toJSONString(workFlow));
                if (workFlow != null){
                    for (int i = 0; i < workFlow.size(); i++) {
                        Map<String, Object> data = (Map<String, Object>) workFlow.get(i);
                        String flowType = Util.null2String(data.get("flowtype"));
                        String newLeaveType = Util.null2String(data.get("newleavetype"));
                        String signtype = Util.null2String(data.get("signtype"));
                        String serial = Util.null2String(data.get("serial"));
                        String requestId = Util.null2String(data.get("requestId"));

                        int beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("begintime")));
                        int endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("endtime")));


                        if (flowType.equalsIgnoreCase(FlowReportTypeEnum.EVECTION.getFlowType())){
                            //如果有出差当天就不算缺勤
                            isAbsenteeism = false;
                            break;
                        }

                        if (flowType.equalsIgnoreCase(FlowReportTypeEnum.OUT.getFlowType())){
                            //如果没有出差再看有没有公出
                            if (checkIn == 0 && checkOut == 0){
                                isAbsenteeism = !((beginIdx <= halfpointIndex) && (endIdx > halfpointIndex));
                            }else if (checkIn == 0 && beginIdx <= halfpointIndex){
                                isAbsenteeism = false;
                            }else  if (checkOut == 0 && beginIdx >= halfpointIndex){
                                isAbsenteeism = false;
                            }
                        }
                    }
                }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取请假流程报错)" + JSON.toJSONString(e));
        }
        return isAbsenteeism;
    }

    /**
     * 获取当天的考勤流程数据
     * @param userId 考勤人员
     * @param kqDate 考勤日期
     */
    public List<Object> getFlowData(String userId, String kqDate){
        List<Object> workFlow = null;
        String dateKey = userId + "|" + kqDate;
        String kqDateNext = DateUtil.addDate(kqDate, 1);
        KQFlowDataBiz kqFlowDataBiz = new KQFlowDataBiz.FlowDataParamBuilder().resourceidParam(userId).fromDateParam(kqDate).toDateParam(kqDateNext).build();
        Map<String, Object> workFlowInfo = new HashMap<>();
        kqFlowDataBiz.getAllFlowData(workFlowInfo, false);
        workFlow = (List<Object>) workFlowInfo.get(dateKey);
        return workFlow;
    }

    /**
     * 判断是否为大夜班
     * @param kqDate
     * @return
     */
    public Boolean isNightShiftBig(String kqDate,String userId){
        Boolean isHave = false;
        RecordSet rs = new RecordSet();
        String sql = "select * from "+tableName + " where "+fieldName+"='"+kqDate+"' and INSTR(',' || "+ fieldResource+" || ',', ',"+userId+",') > 0";
        new BaseBean().writeLog("==zj==(大夜班sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            isHave = true;
        }
        return isHave;
    }

    /**
     * 获取工作日之外的加班天数，最多算4天，且只计算周六
     * @return
     */
    public String getOverTimeDayExtras(String resourceId,String fromDate,String toDate){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        int count = 0;
        String sql = "";
        RecordSet rs = new RecordSet();

        try {
            sql = "select * from KQ_FLOW_OVERTIME where resourceid='"+resourceId+"' and belongdate between '"+fromDate+"' and '"+toDate+"' and changeType != 2 ";
            new BaseBean().writeLog("==zj==(获取工作日之外的加班天数)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            while (rs.next()){
                Date belongDate = format.parse(Util.null2String(rs.getString("belongdate")));
                calendar.setTime(belongDate);

                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY) {
                    count += 1;
                }
                if (count >= 4){
                    break;
                }
            }
        } catch (ParseException e) {
            new BaseBean().writeLog("获取工作日之外的加班天数" + e);
        }
return count + "";
    }

    /**
     * 设置自定义列
     */
    public void setCustomCol(List<Object> titleList){
        List<String> customCols = new ArrayList<>();
        Map<String, Object> title = null;

        customCols.add("reason");
        customCols.add("absenteeismDays");

        for (int i = 0; i < customCols.size(); i++) {
            title = new HashMap<>();
            if ("reason".equals(customCols.get(i))){
                title.put("rowSpan",3);
                title.put("width",7680);
                title.put("title","缺勤原因");
                titleList.add(title);
            }else if ("absenteeismDays".equals(customCols.get(i))){
                title.put("rowSpan",3);
                title.put("width",40*256);
                title.put("title","缺勤日期");
                titleList.add(title);
            }
        }
    }

    /**
     *获取缺勤日期
     * @return
     */
    public String getAbsenteeismDates(String resourceId,String fromDate,String toDate){
        String absenteeismDates = "";
        RecordSet rs = new RecordSet();
        String sql = "select * from kq_format_total where resourceid='"+resourceId+"' and kqdate between '"+fromDate+"' and '"+toDate+"' and absenteeismDays=1 order by kqdate asc";
        rs.executeQuery(sql);
        while (rs.next()){
            String kqDate = Util.null2String(rs.getString("kqdate"));
            absenteeismDates += kqDate+"\n";
        }
        if (absenteeismDates.length() > 0){
            absenteeismDates = absenteeismDates.substring(0,absenteeismDates.length() -1);
        }

        return absenteeismDates;
    }
}
