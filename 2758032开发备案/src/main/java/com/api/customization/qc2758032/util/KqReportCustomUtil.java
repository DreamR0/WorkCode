package com.api.customization.qc2758032.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.util.KQDurationCalculatorUtil;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class KqReportCustomUtil {
   String travelWorkRestid = new BaseBean().getPropValue("qc2758032","travelWorkRestid");//路途工休id
   String noAllowanceAttendanceid = new BaseBean().getPropValue("qc2758032","noAllowanceAttendanceid");//无补助出勤id

    /**
     * 计算补助天数
     * @param id    人员id
     * @param flowData  流程数据
     * @param attendancedays    出勤天数
     * @return
     */
    public String getAllowanceDays(String id,Map<String,Object> flowData,String attendancedays){
        String allowanceDays ="";//补助天数
        String travelWorkRestDays ="";//路途工休班

        //获取路途工休班天数
        travelWorkRestDays = getLeaveDays(id,flowData,travelWorkRestid);
        //补助天数 = 打卡天数  + （路途工休班/2）
        allowanceDays = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(attendancedays,0.0)+Util.getDoubleValue(travelWorkRestDays,0.0)/2));
        return allowanceDays;
    }

    /**
     * 获取出勤总天数
     * @param id    人员id
     * @param flowData  流程数据
     * @param attendancedays  出勤天数
     * @return
     */
    public String getAttendanceCustomTotal(String id,Map<String,Object> flowData,String attendancedays){
        String attendanceCustomTotal = "";
        String travelWorkRestDays = "";//路途工休天数
        String noAllowanceAttendanceDays = "";//无补助出勤天数

        travelWorkRestDays = getLeaveDays(id,flowData,travelWorkRestid);//获取路途工休天数
        travelWorkRestDays = travelWorkRestDays.compareTo("0") > 0?travelWorkRestDays:"0.0";
        noAllowanceAttendanceDays = getLeaveDays(id,flowData,travelWorkRestDays);//无补助出勤天数
        noAllowanceAttendanceDays =  noAllowanceAttendanceDays.compareTo("0") > 0?noAllowanceAttendanceDays:"0.0";

        if ("21".equals(id)){
            new BaseBean().writeLog("==zj==(出勤总天数)" + travelWorkRestDays +  " | " + noAllowanceAttendanceDays + " | " + attendancedays);

        }
        //出勤总天数 = 路途工休天数/2 + 无补助出勤天数 + 应出勤天数
        attendanceCustomTotal = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(attendancedays,0.0)+Util.getDoubleValue(travelWorkRestDays,0.0)/2+Util.getDoubleValue(noAllowanceAttendanceDays,0.0)));

        return attendanceCustomTotal;

    }

    /**
     * 获取指定请假类型天数
     * @param id
     * @param flowData
     * @return
     */
    public String getLeaveDays(String id,Map<String,Object> flowData,String leaveid){
        String travelWorkRestDays ="";
        KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
        List<Map<String, Object>> allLeaveRules = kqLeaveRulesBiz.getAllLeaveRules();
        Map<String, Object> leaveRule = null;
        for(int i=0;allLeaveRules!=null&&i<allLeaveRules.size();i++){
            leaveRule = (Map<String, Object>)allLeaveRules.get(i);
            if (!leaveid.equals(leaveRule.get("id")))continue;
            String flowType = Util.null2String("leaveType_"+leaveRule.get("id"));
            String leaveData = Util.null2String(flowData.get(id+"|"+flowType));
            String flowLeaveBackType = Util.null2String("leavebackType_"+leaveRule.get("id"));
            String leavebackData = Util.null2s(Util.null2String(flowData.get(id+"|"+flowLeaveBackType)),"0.0");
            String b_flowLeaveData = "";
            String flowLeaveData = "";
            try{
                //以防止出现精度问题
                if(leaveData.length() == 0){
                    leaveData = "0.0";
                }
                if(leavebackData.length() == 0){
                    leavebackData = "0.0";
                }
                BigDecimal b_leaveData = new BigDecimal(leaveData);
                BigDecimal b_leavebackData = new BigDecimal(leavebackData);
                b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
                if(Util.getDoubleValue(b_flowLeaveData, -1) < 0){
                    b_flowLeaveData = "0.0";
                }
            }catch (Exception e){
                new BaseBean().writeLog(e);
            }

            //考虑下冻结的数据
            if(b_flowLeaveData.length() > 0){
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
            }else{
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData,0.0)-Util.getDoubleValue(leavebackData,0.0)));
            }
            travelWorkRestDays = flowLeaveData;
        }
        return travelWorkRestDays;
    }
}
