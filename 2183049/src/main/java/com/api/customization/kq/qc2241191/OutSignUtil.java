package com.api.customization.kq.qc2241191;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.TimeSignScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutSignUtil {
    private String businessSchedule = new BaseBean().getPropValue("qc2183049","businessSchedule"); //出差表
    private String outSchedule = new BaseBean().getPropValue("qc2183049","outSchedule");           //公出表
    private SimpleDateFormat sdfh = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat sdfd = new SimpleDateFormat("yyyy-MM-dd");
    long l = System.currentTimeMillis();    //获取当前时间戳
    private String todaysTimes = sdfh.format(l);  //将时间戳转换为指定时间格式    时分
    private String todaysDate = sdfd.format(l);            //将时间戳转换为指定时间格式    年月日


    //==zj 检测是否满足外出流程
    public Map<String, String> OutSignCheck(String resourceId){
        Map<String, String> timeMap = new HashMap<>();
        boolean result = false;
        //查询对应人员是否有归档出差流程，且打卡时间在流程提交的时间范围之内
        RecordSet rs = new RecordSet();
        String businessSingBaseSql = "select  a.id,b.requestid,b.currentnodetype from (select top 1 * from " + businessSchedule + " where resourceId="+resourceId+" order by id desc) a left join workflow_requestbase b on b.requestid=a.requestId";
        String businessSingWhereSql = " where '" +todaysTimes+"' between a.fromTime and a.toTime and '"+todaysDate+"' between a.fromDate and a.toDate order by id desc";
        String businessSingSql = businessSingBaseSql +businessSingWhereSql;
        new BaseBean().writeLog("==zj==(出差考勤验证sql)" + businessSingSql);
        rs.executeQuery(businessSingSql);
        if (rs.next()){
            //改变需求，如果流程有就可以开始打卡，不用归档  0：创建，1：批准，2：提交，3：归档
                //满足流程归档和流程提交范围时间内，进入此方法，校验是否满足当天班次打卡时间范围
                timeMap = OutSignTimeCheck(resourceId, todaysDate, todaysTimes);
                if ("1".equals(timeMap.get("result"))){
                    result = true;
                }
        }else {
            timeMap.put("result","0");
            timeMap.put("msg","没有外出流程或不在打卡时间范围内");
        }
        new BaseBean().writeLog("==zj==(result)" +result);
        //如果出差匹配成功，则不需要执行公出流程，避免数据覆盖
        if (!result){
            String outSingBaseSql = "select  a.id,b.requestid,b.currentnodetype from (select top 1 * from " + outSchedule + " where resourceId="+resourceId+" order by id desc) a left join workflow_requestbase b on b.requestid=a.requestId";
            String outSingWhereSql = " where '" +todaysTimes+"' between a.fromTime and a.toTime and '"+todaysDate+"' between a.fromDate and a.toDate order by id desc";
            String outSingSql = outSingBaseSql +outSingWhereSql;
            new BaseBean().writeLog("==zj==(公出考勤验证sql)" + outSingSql);
            rs.executeQuery(outSingSql);
            if (rs.next()){
                    timeMap = OutSignTimeCheck(resourceId, todaysDate, todaysTimes);

            }else {
                timeMap.put("result","0");
                timeMap.put("msg","没有外出流程或不在打卡时间范围内");
            }
        }

        return  timeMap;
    }

    //判断打卡时间是否在规定时间范围内
    public Map<String, String> OutSignTimeCheck(String resourceId, String todaysDate, String todaysTimes){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Map<String,String> timeMap = new HashMap<>();
        String beginTime = "00:00";      //上班打卡开始时间初始化
        String beginTimeEnd = "24:00";   //上班打卡结束时间初始化
        String endTime = "24:00";       //下班打卡结束时间初始化
        String endTimeStart = "00:00";  //下班打卡开始时间初始化

        //根据人员id和年月日获取当前班次管理信息
        KQWorkTime kt = new KQWorkTime();
        WorkTimeEntity workTime = kt.getWorkTime(resourceId, todaysDate);
        new BaseBean().writeLog("==zj==(workTime)" + JSON.toJSONString(workTime));
        List<TimeScopeEntity> signTime = workTime.getSignTime();    //获取人员当前考勤list
        new BaseBean().writeLog("==zj==(signTime)" + JSON.toJSONString(signTime));
        //获取一天内几次上下班，然后开始判断
        for (int i = 0; i < signTime.size(); i++) {
            TimeScopeEntity timeScopeEntity = signTime.get(i);  //获取班次时间实体
            beginTime = timeScopeEntity.getBeginTime();         //获取上班打卡开始时间
            endTime = timeScopeEntity.getEndTime();      //获取下班打卡结束时间
            TimeSignScopeEntity timeSignScopeEntity = timeScopeEntity.getTimeSignScopeEntity();     //获取打卡时间具体范围

            //如果有具体打卡时间范围
            if (timeSignScopeEntity != null){
                beginTimeEnd = timeSignScopeEntity.getBeginTimeEnd();      //获取上班打卡结束时间
                endTimeStart = timeSignScopeEntity.getEndTimeStart();    //获取下班打卡开始时间

                try{
                    Date todaysTimesD = sdf.parse(todaysTimes);      //签到时间
                    Date beginTimeD = sdf.parse(beginTime);          //上班打卡开始时间
                    Date beginTimeEndD = sdf.parse(beginTimeEnd);    //上班打卡结束时间
                    Date endTimeD = sdf.parse(endTime);              //下班打卡结束时间
                    Date endTimeStartD = sdf.parse(endTimeStart);    //下班打卡开始时间

                    //如果匹配到打卡时间，则退出上下班打卡循环

                    new BaseBean().writeLog("==zj==(如果有打卡时间具体范围)" + todaysTimesD.after(beginTimeD) +" && " + todaysTimesD.before(beginTimeEndD) + " || " + todaysTimesD.after(endTimeStartD) +" && " + todaysTimesD.before(endTimeD));
                    if ((todaysTimesD.after(beginTimeD) && todaysTimesD.before(beginTimeEndD)) || (todaysTimesD.after(endTimeStartD) && todaysTimesD.before(endTimeD))) {
                        timeMap.put("result","1");
                        timeMap.put("msg","打卡成功");
                        break;
                    }else {
                        timeMap.put("result","0");
                        timeMap.put("msg","不在班次规定打卡时间范围内");
                    }

                }catch (Exception e){
                    new BaseBean().writeLog("==zj==(时间转换)" + e);
                }
            }else {
                //如果没有具体时间打卡范围
                try{
                    Date todaysTimesD = sdf.parse(todaysTimes);      //签到时间
                    Date beginTimeD = sdf.parse(beginTime);          //上班打卡开始时间
                    Date endTimeD = sdf.parse(endTime);              //下班打卡结束时间

                    new BaseBean().writeLog("==zj==(如果没有打卡时间具体范围)" + todaysTimesD.after(beginTimeD) +"&&" + todaysTimesD.before(endTimeD));
                    if (todaysTimesD.after(beginTimeD) && todaysTimesD.before(endTimeD)){
                        timeMap.put("reslut","1");
                        timeMap.put("msg","打卡成功");
                    }else {
                        timeMap.put("result","0");
                        timeMap.put("msg","不在班次规定打卡时间范围内");
                        break;
                    }
                }catch (Exception e){
                    new BaseBean().writeLog("==zj==(时间转换)" +e);
                }
            }
        }
        return timeMap;
    }
}
