package com.api.cusomization.kq;

import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverTimeUtli {


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
}
