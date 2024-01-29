package com.api.customization.qc2474646.Util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.util.KQDurationCalculatorUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

public class BalanceOfLeaveUtil {

    /**
     * 获取时间范围内的假期余额
     *
     * @param resourceId
     * @param fromDate
     * @param toDate
     * @return
     */
    public String getBalanceOfLeave(String resourceId, String fromDate, String toDate,KQLeaveRulesBiz kqLeaveRulesBiz, Map<String, Object> flowData) {
        RecordSet rs = new RecordSet();
        String tiaoxiuLeave = "";
        String overTimeSum = "";
        Double overTimeSumD = 0.00;
        String mountSum = "";
        Double tiaoxiuLeaveD = 0.00;

        try {
            //获取当月调休假期时长
            tiaoxiuLeave = getLeaveMount(kqLeaveRulesBiz, flowData, resourceId);
            //获取当月加班时长
            overTimeSum = getOverTime(resourceId, flowData);
            if (!"".equals(tiaoxiuLeave)){
                tiaoxiuLeaveD = Double.parseDouble(tiaoxiuLeave);
            }

            if (!"".equals(overTimeSum)){
                overTimeSumD = Double.parseDouble(overTimeSum);
            }

            mountSum = String.format("%.2f", overTimeSumD - tiaoxiuLeaveD);
            if (mountSum.compareTo("0.00") <= 0){
                mountSum = "0.00";
            }
        } catch (NumberFormatException e) {
            new BaseBean().writeLog("==zj==(计算指定日期范围调休余额报错)" + JSON.toJSONString(e));
        }

        return mountSum;
    }

    /**
     * 设置假期余额--按月数时效的时间为次月2号
     *
     * @param expirationDate
     * @return
     */
    public String setExpirationDate(String expirationDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        GregorianCalendar gc = new GregorianCalendar();

        int days = 2;   //假期余额失效时间为次月3号
        try {
            gc.setTime(sdf.parse(expirationDate));
            gc.add(GregorianCalendar.DATE, days);

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(假期余额失效时间计算异常)" + e);
        }
        return sdf.format(gc.getTime());
    }


    public String getLeaveMount(KQLeaveRulesBiz kqLeaveRulesBiz, Map<String, Object> flowData, String id) {
        String tiaoxiuData = "0.00";
        //请假
        List<Map<String, Object>> allLeaveRules = kqLeaveRulesBiz.getAllLeaveRules();

        Map<String, Object> leaveRule = null;
        for (int i = 0; allLeaveRules != null && i < allLeaveRules.size(); i++) {
            leaveRule = (Map<String, Object>) allLeaveRules.get(i);
            String flowType = Util.null2String("leaveType_" + leaveRule.get("id"));
            String leaveData = Util.null2String(flowData.get(id + "|" + flowType));
            String flowLeaveBackType = Util.null2String("leavebackType_" + leaveRule.get("id"));
            String leavebackData = Util.null2s(Util.null2String(flowData.get(id + "|" + flowLeaveBackType)), "0.0");
            String b_flowLeaveData = "";
            String flowLeaveData = "";
            try {
                //以防止出现精度问题
                if (leaveData.length() == 0) {
                    leaveData = "0.0";
                }
                if (leavebackData.length() == 0) {
                    leavebackData = "0.0";
                }
                BigDecimal b_leaveData = new BigDecimal(leaveData);
                BigDecimal b_leavebackData = new BigDecimal(leavebackData);
                b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
                if (Util.getDoubleValue(b_flowLeaveData, -1) < 0) {
                    b_flowLeaveData = "0.0";
                }
            } catch (Exception e) {
            }

            //考虑下冻结的数据
            if (b_flowLeaveData.length() > 0) {
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
            } else {
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData, 0.0) - Util.getDoubleValue(leavebackData, 0.0)));
            }
            String isTiaoxiu = leaveRule.get("id").toString();
            new BaseBean().writeLog("==zj==(isTiaoxiu)" + isTiaoxiu);
            if (KQLeaveRulesBiz.isTiaoXiu(isTiaoxiu)) {
                tiaoxiuData = flowLeaveData;
            }
        }
        return tiaoxiuData;
    }

    /**
     * 这里获取加班时长
     * @param flowData
     * @return
     */
    public String getOverTime(String id,Map<String,Object> flowData){
            String overTimeSum = "";
        try {
            double workingDayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|workingDayOvertime_4leave")));
            workingDayOvertime_4leave = workingDayOvertime_4leave<0?0:workingDayOvertime_4leave;
            double restDayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|restDayOvertime_4leave")));
            restDayOvertime_4leave = restDayOvertime_4leave<0?0:restDayOvertime_4leave;
            double holidayOvertime_4leave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|holidayOvertime_4leave")));
            holidayOvertime_4leave = holidayOvertime_4leave<0?0:holidayOvertime_4leave;

            double workingDayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|workingDayOvertime_nonleave")));
            workingDayOvertime_nonleave = workingDayOvertime_nonleave<0?0:workingDayOvertime_nonleave;
            double restDayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|restDayOvertime_nonleave")));
            restDayOvertime_nonleave = restDayOvertime_nonleave<0?0:restDayOvertime_nonleave;
            double holidayOvertime_nonleave = Util.getDoubleValue(Util.null2String(flowData.get(id+"|holidayOvertime_nonleave")));
            holidayOvertime_nonleave = holidayOvertime_nonleave<0?0:holidayOvertime_nonleave;

            overTimeSum = KQDurationCalculatorUtil.getDurationRound(String.valueOf(workingDayOvertime_4leave+restDayOvertime_4leave+holidayOvertime_4leave+
                     workingDayOvertime_nonleave+restDayOvertime_nonleave+holidayOvertime_nonleave));
        } catch (Exception e) {
           new BaseBean().writeLog("==zj==(获取加班时长报错)" + JSON.toJSONString(e));
        }
        return overTimeSum;
    }
}
