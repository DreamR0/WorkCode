package com.customization.qc2215400;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.wfset.util.KQFlowUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
   private int mins = 0;
   private String overTimes = "";
   private Integer durationrule;


    /**
     * 提交加班流程表单时候的时间计算
     * @param fromDate
     * @param fromTime
     * @param toDate
     * @param toTime
     * @param resourceId
     * @return
     */
    public String TimesTampChange(String fromDate,String fromTime,String toDate,String toTime,String resourceId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sds = new SimpleDateFormat("HH:mm");

        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
        try {
            Date fromDay = sdf.parse(fromDate);     //开始日期
            Date toDay = sdf.parse(toDate);         //结束日期
            Date nextDay = fromDay;

            Date fromTimes = sds.parse(fromTime);   //开始时间
            Date toTimes = sds.parse(toTime);   //结束时间

            //最小加班单位：1-按天、2-按半天、3-按小时、4-按整天、5-按半小时加班、6-整小时
            int minimumUnitOfOvertime = -1;
            //日折算时长
            double hoursToDay = -1.00;
            String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
            RecordSet recordSet = new RecordSet();
            recordSet.executeQuery(sql);
            if (recordSet.next()) {
                minimumUnitOfOvertime = Util.getIntValue(recordSet.getString("minimumUnit"), -1);
                hoursToDay = Util.getDoubleValue(recordSet.getString("hoursToDay"), 8.00);
            }


            int i = 0;
            //比较日期相差多少天
            while (nextDay.before(toDay)) {
                Calendar cld = Calendar.getInstance();
                cld.setTime(fromDay);
                cld.add(Calendar.DATE, 1);
                fromDay = cld.getTime();
                nextDay = fromDay;
                i++;
            }

            //如果是按整天，则特殊处理
            if (minimumUnitOfOvertime == 4){
                int dayCount=0;
                for (int j = 0; j <= i; j++) {
                    dayCount++;
                    overTimes =  KQDurationCalculatorUtil.getDurationRound("" + dayCount);
                }
                return overTimes;
            }
            //计算加班时长多少分钟,如果开始日期大于结束日期则不计算加班时长
            new BaseBean().writeLog("==zj==(日期比较)" + JSON.toJSONString(fromDay) + " | " + JSON.toJSONString(toDay) + " | " + fromDay.before(toDay));
            if (fromDay.before(toDay) || fromDay.equals(toDay)) {
                //如果开始时间大于结束时间，则不计算加班时长。但如果是跨天则不需要
                new BaseBean().writeLog("==zj==(时间比较)" + JSON.toJSONString(fromTimes) + " | " + JSON.toJSONString(toTimes) + " | " + fromTimes.before(toTimes));
                if (fromTimes.before(toTimes) || i!=0) {
                    if (i == 0) {
                        int fromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromTime);
                        int toTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(toTime);
                        mins = toTimeIndex - fromTimeIndex;
                    } else {
                        int fromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromTime);
                        int toTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(toTime);
                        toTimeIndex += (i * 1440);
                        new BaseBean().writeLog("==zj==(如果是跨天，结束时间要+1440)" + toTimeIndex);

                        mins = toTimeIndex - fromTimeIndex;
                    }
                    new BaseBean().writeLog("==zj==(最终计算的分钟数)" + mins);
                    overTimes = duractionChange(mins);
                    new BaseBean().writeLog("==zj==(经过加班单位转换之后)" + overTimes);

                }else {
                    overTimes="0.0";
                }
            }else {
                overTimes="0.0";
            }

            }catch(Exception e){
                new BaseBean().writeLog("==zj==(时间转换工具错误)" + e);
            }
        return overTimes;
    }

    /**
     * 用来根据加班单位进行转换
     * @param mins
     * @return
     */
    public String duractionChange(int mins) {

        durationrule = KQOvertimeRulesBiz.getMinimumUnit();      //获取加班计算单位  1-按天计算 2-按半天计算 3-按小时计算 4-按整天计算
        new BaseBean().writeLog("==zj==(加班计算单位)" + durationrule);

        if(durationrule==3 || durationrule == 5 || durationrule == 6){
            double d_hour = mins/60.0;
            overTimes = KQDurationCalculatorUtil.getDurationRound("" + d_hour);
            if (durationrule == 5){
                //如果是按半小时计算,再计算折算方式
                overTimes = halfHour(overTimes);
            }
            if (durationrule == 6){
                //如果是按整小时计算，再计算折算方式
                overTimes = wholeHour(overTimes);
            }
        }else {
            double oneDayHour = KQFlowUtil.getOneDayHour(DurationTypeEnum.OVERTIME,"");
            double d_day = mins/(oneDayHour * 60);
            overTimes = KQDurationCalculatorUtil.getDurationRound("" + d_day);
        }
        return overTimes;
    }
    /**
     * 如果是半小时计算要计算三种折算方式
     */
    public String halfHour(String overTimes){
        new BaseBean().writeLog("==zj==(加班时间（小时单位）)" + overTimes);
        //最小加班单位：1-按天、2-按半天、3-按小时、4-按整天、5-按半小时加班、6-整小时
        int overtimeConversion = -1;
        //日折算时长
        double hoursToDay = -1.00;
        //半小时计算
        double minsHalfHour = 0.0;
        //小时计算
        Double minsHour = Double.parseDouble(overTimes);
        new BaseBean().writeLog("==zj==(加班时间转double类型)" + minsHour);


        String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        if (recordSet.next()) {
            overtimeConversion = Util.getIntValue(recordSet.getString("overtimeConversion"), -1);
            hoursToDay = Util.getDoubleValue(recordSet.getString("hoursToDay"), 8.00);
        }
        new BaseBean().writeLog("==zj==(半小时)" + overtimeConversion);
        if (overtimeConversion == 1){
            //四舍五入
            minsHalfHour = Math.round((minsHour*2));
            minsHalfHour = minsHalfHour/2;
        }else if (overtimeConversion == 2){
            //向上取整
            minsHalfHour = Math.ceil((minsHour*2));
            minsHalfHour = minsHalfHour/2;
        }else if (overtimeConversion == 3){
            //向下取整
            minsHalfHour = Math.floor((minsHour*2));
            minsHalfHour = minsHalfHour/2;
        }

        new BaseBean().writeLog("==zj==(折算半小时)" + minsHalfHour);
        return KQDurationCalculatorUtil.getDurationRound("" + minsHalfHour);
    }

    /**
     * 如果是整小时要计算三种计算方式
     */
    public String wholeHour(String overTimes){
        //以小时为单位的加班时长
        Double minsHour = Double.parseDouble(overTimes);
        new BaseBean().writeLog("==zj==(整小时转换)" + minsHour);
        //最小加班单位：1-按天、2-按半天、3-按小时、4-按整天、5-按半小时加班、6-整小时
        int overtimeConversion = -1;
        //日折算时长
        double hoursToDay = -1.00;
        //整小时计算
        double minsWholeHour = 0.0;


        String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        if (recordSet.next()) {
            overtimeConversion = Util.getIntValue(recordSet.getString("overtimeConversion"), -1);
            hoursToDay = Util.getDoubleValue(recordSet.getString("hoursToDay"), 8.00);
        }
        new BaseBean().writeLog("==zj==(整小时)" + overtimeConversion);
        if (overtimeConversion == 1){
            //四舍五入
            minsWholeHour = Math.round(minsHour);
        }else if (overtimeConversion == 2){
            //向上取整
            minsWholeHour = Math.ceil(minsHour);
        }else if (overtimeConversion == 3){
            //向下取整
            minsWholeHour = Math.floor(minsHour);
        }

        new BaseBean().writeLog("==zj==(折算整小时)" + minsHour);
        return KQDurationCalculatorUtil.getDurationRound("" + minsWholeHour);
    }
    /**
     * 折算单位转换分钟时长
     */

    public double duractionReturn(String mins,Double hoursToDay){
        durationrule = KQOvertimeRulesBiz.getMinimumUnit();      //获取加班计算单位  1-按天计算 2-按半天计算 3-按小时计算 4-按整天计算
        Double D_mins = Double.parseDouble(mins);
        if (durationrule == 3 || durationrule == 5 || durationrule == 6){
            //半小时
            BigDecimal b1 = new BigDecimal(D_mins);
            BigDecimal b2 = new BigDecimal(60);
            return b1.multiply(b2).doubleValue();
        }

        if (durationrule == 1){
           return D_mins;
        }
        //按天数
        BigDecimal b1 = new BigDecimal(D_mins);
        BigDecimal b2 = new BigDecimal(1440);
        return b1.multiply(b2).doubleValue();
    }
    /**
     * 这里是在流程插入加班表时的加班时长计算
     * @param fromDate
     * @param fromTime
     * @param toDate
     * @param toTime
     * @return
     */
    public int TimesTampChange(String fromDate,String fromTime,String toDate,String toTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sds = new SimpleDateFormat("HH:mm");

        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
        try {
            Date fromDay = sdf.parse(fromDate);     //开始日期
            Date toDay = sdf.parse(toDate);         //结束日期
            Date nextDay = fromDay;

            Date fromTimes = sds.parse(fromTime);   //开始时间
            Date toTimes = sds.parse(toTime);   //结束时间


            int i = 0;
            //比较日期相差多少天
            while (nextDay.before(toDay)) {
                Calendar cld = Calendar.getInstance();
                cld.setTime(fromDay);
                cld.add(Calendar.DATE, 1);
                fromDay = cld.getTime();
                nextDay = fromDay;
                i++;
            }

            //计算加班时长多少分钟,如果开始日期大于结束日期则不计算加班时长
            new BaseBean().writeLog("==zj==(日期比较)" + JSON.toJSONString(fromDay) + " | " + JSON.toJSONString(toDay) + " | " + fromDay.before(toDay));
            if (fromDay.before(toDay) || fromDay.equals(toDay)) {
                //如果开始时间大于结束时间，则不计算加班时长。但如果是跨天则不需要
                new BaseBean().writeLog("==zj==(时间比较)" + JSON.toJSONString(fromTimes) + " | " + JSON.toJSONString(toTimes) + " | " + fromTimes.before(toTimes));
                if (fromTimes.before(toTimes) || i!=0) {
                    if (i == 0) {
                        int fromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromTime);
                        int toTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(toTime);
                        mins = toTimeIndex - fromTimeIndex; //加班分钟数折算

                    } else {
                        int fromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(fromTime);
                        int toTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(toTime);
                        toTimeIndex += (i * 1440);
                        new BaseBean().writeLog("==zj==(如果是跨天，结束时间要+1440)" + toTimeIndex);
                        mins = toTimeIndex - fromTimeIndex;
                    }
                    new BaseBean().writeLog("==zj==(最终计算的分钟数)" + mins);

                }else {
                    mins=0;
                }
            }else {
                mins=0;
            }

        }catch(Exception e){
            new BaseBean().writeLog("==zj==(时间转换工具错误)" + e);
        }
        return mins;
    }
}
