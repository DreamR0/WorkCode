package com.api.cusomization.kq;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQFlowDataBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.FlowReportTypeEnum;
import com.engine.kq.wfset.bean.SplitBean;
import com.engine.kq.wfset.util.KQFlowLeaveBackUtil;
import com.engine.kq.wfset.util.KQFlowLeaveUtil;
import com.engine.kq.wfset.util.KQFlowUtil;
import org.eclipse.collections.api.block.function.primitive.BooleanLongToLongFunction;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.*;

public class OverTimeUtil {


    /**
     * 检查加班是否满足3次的打卡次数
     */
    public Map<String,Object> overTimeSignCardCheck(String resourceid,  String signinDate, String signoutDate){
        Map<String,Object> resultMap = new HashMap<String,Object>();
        boolean result = false;     //检查下班之后的打卡次数
        int over_countNon = 0;      //下班时间到加班开始卡之间的时间

        KQWorkTime kqWorkTime = new KQWorkTime();

        //==zj 获取系统当前时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(System.currentTimeMillis());
        String endDate = formatter.format(date);

        //zj(第二种加班方式)获取最晚班次时间
        String endTime = "";  //下班时间
        WorkTimeEntity workTime_end = kqWorkTime.getWorkTime(resourceid,endDate);
        List<TimeScopeEntity> IsWorkTime = workTime_end.getWorkTime();
        for (int i = 0; IsWorkTime!=null&&i<IsWorkTime.size(); i++) {
            //默认为第一个值
            if (endTime.equals("")){
                TimeScopeEntity workTimeAdmin = IsWorkTime.get(0);
                endTime = workTimeAdmin.getEndTime();
            }
            TimeScopeEntity workTimeScope = IsWorkTime.get(i);
            if (endTime.compareTo(workTimeScope.getEndTime())<0){
                endTime = workTimeScope.getEndTime();
            }
        }

        //==zj==获取下班之后的打卡次数，如果次数小于3则加班时长为0，如果不小于3则计算加班时长
        int countSign = 0;//打卡次数

        RecordSet ro = new RecordSet();
        String overTimesql = "select count(*) as count from hrmschedulesign where userid=" + resourceid + "and"
                + "(signTime > '" + endTime + "' or signTime < '04:00:00')" + " and "
                +"(signDate = '" + signinDate + "'" + " or " + "signDate = '" + signoutDate + "')";
        new BaseBean().writeLog("==zj==（第二种方式--查询下班后打卡次数sql）" + overTimesql);
        //查询下班之后打卡次数
        ro.executeQuery(overTimesql);
        if (ro.next()){
            countSign = ro.getInt("count");
        }
        //如果下班之后次数小于3，则不计算加班时长
        if (countSign < 3){
            resultMap.put("result",result);
        }else {
            result = true;
            over_countNon = overTimeCalculate(resourceid,endTime,signinDate,signoutDate);
            resultMap.put("result",result);
            resultMap.put("over_countNon",over_countNon);
        }

        return resultMap;
    }

    /**
     * 获取加班上班卡时间，跟下班时间进行相减
     */
    public int overTimeCalculate(String resourceid,String endTime,String signinDate,String signoutDate){
        RecordSet rs = new RecordSet();
        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
        String overTimeIn = "";
        //获取加班上班卡时间
        String overInSql = "select top 1 * from(" + "select top 2 signTime,signDate from hrmschedulesign "+
                "where userid=" + resourceid + " and signTime >'" + endTime + "' and ("+
                "signDate='" + signinDate +"' or signDate='" + signoutDate + "') order by signTime)"+
                "[table] order by signTime desc";
        new BaseBean().writeLog("==zj==(第二种方式--获取加班上班卡sql)"+ overInSql);
        rs.executeQuery(overInSql);
        if (rs.next()){
            overTimeIn = Util.null2String(rs.getString("signTime"));  //获取加班上班卡时间
        }
        int overTimeInIndex = kqTimesArrayComInfo.getArrayindexByTimes(overTimeIn);//加班上班卡转分钟

        int endTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(endTime);       //最晚班次下班时间转分钟
        int overTime = overTimeInIndex - endTimeIndex;

        return overTime;
    }

    /**
     * map强制类型转换判断
     */
    public <T> T mapChangeCheck(Object param){
        if (param instanceof Integer) {
            int value = ((Integer) param).intValue();
            return (T) new Integer(value);
        } else if (param instanceof String) {
            String s = (String) param;
            return (T) new String(s);
        } else if (param instanceof Double) {
            double d = ((Double) param).doubleValue();
            return (T) new Double(d);
        } else if (param instanceof Float) {
            float f = ((Float) param).floatValue();
            return (T) new Float(f);
        } else if (param instanceof Long) {
            long l = ((Long) param).longValue();
            return (T) new Long(l);
        } else if (param instanceof Boolean) {
            boolean b = ((Boolean) param).booleanValue();
            return (T) new Boolean(b);
        }

        return null;
    }

    /**
     * 判断当天下班之后是否有打满3次卡
     */

    public Boolean isThreeCard(String resourceId,String signinDate){
        Boolean isThreeCard = false;
        String cutPointTime = new BaseBean().getPropValue("qc2522811","cutPointTime");//自定义跨天时间点
        String endTime = "";
        int count = 0;
        RecordSet rs = new RecordSet();
        String sql = "";


        //获取班次2下班时间
        WorkTimeEntity workTime = new KQWorkTime().getWorkTime(resourceId, signinDate);
        List<TimeScopeEntity> workTimeList = workTime.getWorkTime();
        if (workTimeList.size() > 1){
            TimeScopeEntity timeScopeEntity = workTimeList.get(1);
            endTime = timeScopeEntity.getEndTime();
        }

        sql = "select count(*)as count from hrmschedulesign where userid = '"+resourceId+"' and (signDate = '"+signinDate+"'"
                +" and signTime > '"+endTime+"') or (signDate = DATEADD(day, 1, '"+signinDate+"') and signTime < '"+cutPointTime+"')";
        new BaseBean().writeLog("==zj==(检查是否满足三次打卡次数)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            count = Util.getIntValue(rs.getInt("count"));
        }
        if (count >= 3 ){
            isThreeCard = true;
        }
        return isThreeCard;
    }

    /**
     * 获取加班上班卡,并重新计算加班时长
     * @param resourceId
     * @param signinDate
     * @return
     */
    public int overTimeBeginCard(String resourceId,String signinDate,int[] initArrays){
        String endTime = "";
        String overTimeBeginCard = "";
        RecordSet rs = new RecordSet();
        int overTimeBeginCardIndex = 0;
        String sql = "";

        //获取班次2下班时间
        WorkTimeEntity workTime = new KQWorkTime().getWorkTime(resourceId, signinDate);
        List<TimeScopeEntity> workTimeList = workTime.getWorkTime();
        if (workTimeList.size() > 1){
            TimeScopeEntity timeScopeEntity = workTimeList.get(1);
            endTime = timeScopeEntity.getEndTime();
        }

        //获取加班上班卡
        sql = "select * from( select top 2 * from hrmschedulesign where userid = '"+resourceId+"'  and signDate = '"+signinDate+"' "+
                " and signTime > '"+endTime+"' order by signTime asc ) [table] order by signTime desc";
        new BaseBean().writeLog("==zj==(获取加班上班卡)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            overTimeBeginCard =  Util.null2String(rs.getString("signTime"));
        }
        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
         overTimeBeginCardIndex = kqTimesArrayComInfo.getArrayindexByTimes(overTimeBeginCard);

         return overTimeBeginCardIndex;
    }

    /**
     * 获取加班上班卡,并重新计算加班时长(请假流程)
     * @param resourceId
     * @param signinDate
     * @return
     */
    public void overTimeBeginCardLeave(String resourceId,String signinDate,int[] initArrays){
        String endTime = "";
        String overTimeBeginCard = "";
        RecordSet rs = new RecordSet();
        String sql = "";

        //获取班次2下班时间
        WorkTimeEntity workTime = new KQWorkTime().getWorkTime(resourceId, signinDate);
        List<TimeScopeEntity> workTimeList = workTime.getWorkTime();
        if (workTimeList.size() > 1){
            TimeScopeEntity timeScopeEntity = workTimeList.get(1);
            endTime = timeScopeEntity.getEndTime();
        }

        //获取加班上班卡
        sql = "select top 2 * from hrmschedulesign where userid = '"+resourceId+"'  and signDate = '"+signinDate+"' "+
                " and signTime > '"+endTime+"' order by signTime asc";
        new BaseBean().writeLog("==zj==(获取加班上班卡请假流程)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            overTimeBeginCard =  Util.null2String(rs.getString("signTime"));
        }
        //把下班时间到加班上班卡之前的时间覆盖掉
        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
        int overTimeBeginCardIndex = kqTimesArrayComInfo.getArrayindexByTimes(overTimeBeginCard);
        int endTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(endTime);
        Arrays.fill(initArrays,endTimeIndex,overTimeBeginCardIndex,-5);

    }


    /**
     * 判断当天下班时间是否有请假
     * @param resourceId    考勤人员
     * @param kqDate        考勤日期
     * @param serial_type    是否夜班
     * @return
     */
    public Boolean isLeave(String resourceId,String kqDate,String serial_type){
        if ("1".equals(serial_type)){
            return false;
        }

        Boolean isLeave = null;
        try {
            List<Object> workFlow = null;
            RecordSet rs = new RecordSet();
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
            isLeave = false;
            String sql = "select * from kq_flow_split_leave where resourceid="+resourceId+" and belongdate='"+kqDate+"'"+
                    " and leavebackrequestid = null";
            int endTime = 0;//班次下班时间

            //获取班次2下班时间
            WorkTimeEntity workTime = new KQWorkTime().getWorkTime(resourceId, kqDate);
            List<TimeScopeEntity> workTimeList = workTime.getWorkTime();
            if (workTimeList.size() > 1){
                TimeScopeEntity timeScopeEntity = workTimeList.get(1);
                endTime = kqTimesArrayComInfo.getArrayindexByTimes( timeScopeEntity.getEndTime());
            }

            //对当天每条请假流程进行判断如果请假结束时间大于等于下班时间，则只需两次卡就行
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

                    if (flowType.equalsIgnoreCase(FlowReportTypeEnum.LEAVE.getFlowType())){
                        //如果是请假流程进行判断
                        new BaseBean().writeLog("==zj==(时长对比)" + endIdx + "  |  " + endTime);
                        if (endIdx >= endTime){
                            isLeave = true;
                        }
                    }
                    if (isLeave){
                        break;
                    }
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取请假流程报错)" + JSON.toJSONString(e));
        }

        return isLeave;

}

    /**
     * 获取当天的考勤流程数据
     * @param userId 考勤人员
     * @param kqDate 考勤日期
     */
    public List<Object> getFlowData(String userId,String kqDate){
        List<Object> workFlow = null;
        String dateKey = userId + "|" + kqDate;
        String kqDateNext = DateUtil.addDate(kqDate, 1);
        KQFlowDataBiz kqFlowDataBiz = new KQFlowDataBiz.FlowDataParamBuilder().resourceidParam(userId).fromDateParam(kqDate).toDateParam(kqDateNext).build();
        Map<String, Object> workFlowInfo = new HashMap<>();
        kqFlowDataBiz.getAllFlowData(workFlowInfo, false);
        new BaseBean().writeLog("==zj==(查询当前人员所有考勤流程信息)" + dateKey);
        new BaseBean().writeLog("==zj==(workFlowInfo)" + JSON.toJSONString(workFlowInfo));
        workFlow = (List<Object>) workFlowInfo.get(dateKey);

       /* for (int i = 0; i < workFlow.size(); i++) {
            Map<String, Object> data = (Map<String, Object>) workFlow.get(i);
            String flowType = Util.null2String(data.get("flowtype"));
            String newLeaveType = Util.null2String(data.get("newleavetype"));
            String signtype = Util.null2String(data.get("signtype"));
            String serial = Util.null2String(data.get("serial"));
            String requestId = Util.null2String(data.get("requestId"));

            int beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("begintime")));
            int endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("endtime")));

            if (flowType.equalsIgnoreCase(FlowReportTypeEnum.LEAVE.getFlowType())){
                //如果是请假流程进行判断
            }

        }*/

        return workFlow;

    }







}
