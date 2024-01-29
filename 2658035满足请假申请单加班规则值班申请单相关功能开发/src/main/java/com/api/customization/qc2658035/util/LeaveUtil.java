package com.api.customization.qc2658035.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.wfset.bean.SplitBean;
import weaver.general.BaseBean;

public class LeaveUtil {


    /**
     * 判断当前拆分流程时长是否有覆盖到指定范围，覆盖到对其分钟数进行加减
     * @param splitBean
     */
    public void isHaveLeaveRule(SplitBean splitBean,String splitDate){
        try {
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
            int splitFromTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(splitBean.getFromTime());//获取开始时间分钟数
            int splitToTimeIndex =  kqTimesArrayComInfo.getArrayindexByTimes(splitBean.getToTime()); //获取结束时间分钟数
            int beginAMIndex =kqTimesArrayComInfo.getArrayindexByTimes("08:30");       //上午开始时间
            int endAMTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes("12:00");     //上午结束时间
            int beginPMIndex = kqTimesArrayComInfo.getArrayindexByTimes("12:45");       //下午开始时间
            int endPMIndex = kqTimesArrayComInfo.getArrayindexByTimes("13:00");       //下午结束时间
            Double Mins = 0.0;


            //先判断是否覆盖到上午时间段
            if (splitFromTimeIndex <= beginAMIndex && splitToTimeIndex >= endAMTimeIndex){
                Mins += 30;
            }
            //再判断是否覆盖到下午时间段
            if (splitFromTimeIndex <= beginPMIndex && splitToTimeIndex >= endPMIndex){
                Mins -= 15;
            }
            new BaseBean().writeLog("==zj==(userId)" + JSON.toJSONString(splitBean.getResourceId()));
            new BaseBean().writeLog("==zj==(拆分日期)" + splitDate);
            new BaseBean().writeLog("==zj==(Mins)" + JSON.toJSONString(Mins));
            double d_mins = splitBean.getD_Mins();
            splitBean.setD_Mins(d_mins + Mins);
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(拆分流程时长换算报错)" + JSON.toJSONString(e));
        }
    }
}
