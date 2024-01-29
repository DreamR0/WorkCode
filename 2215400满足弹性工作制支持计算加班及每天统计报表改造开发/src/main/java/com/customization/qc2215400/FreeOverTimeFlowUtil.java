package com.customization.qc2215400;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.*;
import com.engine.kq.wfset.bean.SplitBean;
import oracle.jdbc.babelfish.BabelfishStatement;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FreeOverTimeFlowUtil {

    /**
     * 自由班制的流程添加到kq_flow_overtime
     */
    public boolean Add(SplitBean splitBean,int Count){
        new BaseBean().writeLog("==zj==(每次拆分进来的数据)" + JSON.toJSONString(splitBean));
        boolean isUp = false;        //确认是否有插入到加班最终数据表
        RecordSet rs = new RecordSet();
            int changeType = 0;     //加班类型
            String requestId = splitBean.getRequestId();    //对应流程的requestId
            String resourceId = splitBean.getResourceId();  //对应人员id
            String groupid = splitBean.getGroupid();        //获取考勤组id

             String duration = splitBean.getDuration();     //加班流程时长
             String fromdateDB = splitBean.getFromdatedb();  //流程上加班开始日期
            String fromtimedb = splitBean.getFromtimedb();  //流程上加班开始时间
            String todatedb = splitBean.getTodatedb();      //流程上加班结束日期
            String totimedb = splitBean.getTotimedb();      //流程上加班结束时间
            String fromdate = splitBean.getFromDate();      //实际有效的加班开始日期
            String fromtime = splitBean.getFromTime();      //实际有效的加班开始时间
            String todate = splitBean.getToDate();          //实际有效的加班结束日期
            String totime = splitBean.getToTime();          //实际有效的加班结束时间
            String belongDate = splitBean.getBelongDate();  //加班归属日期

            int unit = KQOvertimeRulesBiz.getMinimumUnit(); //计算规则
            int paidLeaveEnable =0;             //加班补偿类型    1关联调休 2不关联调休

            //获取自由班制加班当天状态    1、节假日 2、工作日 3、休息日
            Date fromdateD = DateChange(fromdate);
            String week = getWeek(fromdateD);
            String daySql = "select id,value from kq_group CROSS APPLY STRING_SPLIT(kq_group.weekday, ',') where id="+groupid+" and value = '"+week+"'";
            new BaseBean().writeLog("==zj==(查询当天状态)" +daySql);
            rs.executeQuery(daySql);
            if (rs.next()){
                changeType = 2;
            }else {
                changeType = 3;
            }
            //如果是节假日设置，则优先与考勤组设置
            String holidaySql = "select * from kq_HolidaySet where groupid =" + groupid+" and holidayDate='" + fromdate+"'";
            new BaseBean().writeLog("==zj==(查询节假日设置)" + holidaySql);
            rs.executeQuery(holidaySql);
            if (rs.next()){
                changeType = rs.getInt("changeType");
            }
            splitBean.setChangeType(changeType);


                delete(requestId,splitBean);

            //格式化考勤
            new KQFormatData().formatKqDate(resourceId,fromdate);
        //最小加班单位：1-按天、2-按半天、3-按小时、4-按整天、5-按半小时加班、6-整小时
        int minimumUnitOfOvertime = -1;
        //日折算时长
        double hoursToDay = -1.00;
        String sql = "select * from kq_OvertimeUnit where (isDelete is null or isDelete !=1) and id=1";
        RecordSet recordSet = new RecordSet();
        recordSet.executeQuery(sql);
        if (recordSet.next()) {
            minimumUnitOfOvertime = weaver.general.Util.getIntValue(recordSet.getString("minimumUnit"), -1);
            hoursToDay = Util.getDoubleValue(recordSet.getString("hoursToDay"), 8.00);
        }

            DateUtil dateUtil = new DateUtil();
            int D_MinsCount = dateUtil.TimesTampChange(fromdate, fromtime, todate, totime);   //计算流程加班分钟数
            //将流程分钟数进行单位折算(测试)
            String D_Minss = dateUtil.duractionChange(D_MinsCount);
            new BaseBean().writeLog("==zj==（D_Minss）" +D_Minss);
            double D_Mins = dateUtil.duractionReturn(D_Minss,hoursToDay);
            new BaseBean().writeLog("==zj==(最终折算之后的流程分钟数)" + D_Mins);


        if (minimumUnitOfOvertime == 4){
            D_Mins = hoursToDay*60;
        }
        if (minimumUnitOfOvertime == 1){
            D_Mins = D_Mins*hoursToDay*60;
        }

        splitBean.setD_Mins(D_Mins);
            String flow_overtime_sql = "insert into kq_flow_overtime(requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,workMins,durationrule,changetype,paidLeaveEnable,computingMode,fromdatedb,fromtimedb,todatedb,totimedb)"+
                    " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
            new BaseBean().writeLog("==zj==(自由加班流程插入加班表)" + flow_overtime_sql);
             isUp = rs.executeUpdate(flow_overtime_sql, requestId,resourceId,fromdate,fromtime,todate,totime,D_Mins,"",belongDate,"",
                    unit,changeType,paidLeaveEnable,"1",fromdateDB,fromtimedb,todatedb,totimedb);


        return isUp;
    }

    /**
     * 自由班制流程先删除，再增加
     */
    public void delete(String requestId,SplitBean splitBean){
        String fromdate = splitBean.getFromDate();      //实际有效的加班开始日期
        String fromtime = splitBean.getFromTime();      //实际有效的加班开始时间
        String todate = splitBean.getToDate();          //实际有效的加班结束日期
        String totime = splitBean.getToTime();          //实际有效的加班结束时间

        RecordSet rs = new RecordSet();
        String flow_overtime_sql = "delete from kq_flow_overtime where requestid=" +requestId + "and fromdate='"+fromdate+"'"+" and todate='" +todate+"'"+
                " and fromtime='"+fromtime+"'"+"and totime='"+totime+"'";
        new BaseBean().writeLog("==zj==(删除流程加班数据)" + flow_overtime_sql);
        rs.executeUpdate(flow_overtime_sql);
    }

    /**
     * 获取当前日期为星期几
     */
    public  String getWeek(Date date){
        String[] weeks = {"6","0","1","2","3","4","5"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if(week_index<0){
            week_index = 0;
        }
        return weeks[week_index];
    }

    /**
     * 将字符串日期转为Date格式
     */
    public Date DateChange(String time){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = sdf.parse(time);
        } catch (Exception e) {
                new BaseBean().writeLog("==zj==(转换日期日期格式报错)" + e);
        }
        return date;
    }
}
