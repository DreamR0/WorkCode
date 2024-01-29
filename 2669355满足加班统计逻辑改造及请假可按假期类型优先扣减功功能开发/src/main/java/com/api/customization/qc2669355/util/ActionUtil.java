package com.api.customization.qc2669355.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionUtil {
    /**
     * 获取上个月的第一天和最后一天
     * @return
     */
    public List<String> getLastMonthDay(){
        List<String> lastMonthDays = null;
        try {
            lastMonthDays = new ArrayList<>();
            // 获取当前日期
            LocalDate currentDate = LocalDate.now();

            // 获取上个月的第一天
            LocalDate firstDayOfLastMonth = currentDate.minusMonths(1).withDayOfMonth(1);

            // 获取上个月的最后一天
            LocalDate lastDayOfLastMonth = currentDate.minusMonths(1).withDayOfMonth(currentDate.minusMonths(1).lengthOfMonth());

            // 格式化日期为字符串
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String firstDay = firstDayOfLastMonth.format(formatter);
            String lastDay = lastDayOfLastMonth.format(formatter);

            lastMonthDays.add(firstDay);
            lastMonthDays.add(lastDay);
        } catch (Exception e) {
           new BaseBean().writeLog("==zj==(获取上月时长报错)" + JSON.toJSONString(e));
        }

        return lastMonthDays;
    }

    /**将超出部分转为调休时长，并生成调休加班记录
     * @param overTimeRs
     * @param duration_split_min 超出36小时部分
     */
    public void saveOverTimeLeave(RecordSet overTimeRs,int duration_split_min){
        RecordSet rs = new RecordSet();
        String  requestid = Util.null2String(overTimeRs.getString("requestid"));  //流程id
        String  resourceid = Util.null2String(overTimeRs.getString("resourceid"));  //人员id
        String  fromdate = Util.null2String(overTimeRs.getString("fromdate"));  //实际开始日期
        String  fromtime = Util.null2String(overTimeRs.getString("fromtime"));  //实际开始时间
        String  todate = Util.null2String(overTimeRs.getString("todate"));  //实际结束日期
        String  totime = Util.null2String(overTimeRs.getString("totime"));  //实际结束时间
        String  expiringdate = Util.null2String(overTimeRs.getString("expiringdate"));  //过期日期
        String  belongdate = Util.null2String(overTimeRs.getString("belongdate"));  //加班归属日期
        int  workmins = overTimeRs.getInt("workmins");  //工作时长
        int  changetype = overTimeRs.getInt("changetype");  //加班类型
        int  paidleaveenable = overTimeRs.getInt("paidleaveenable");  //是否调休
        int  duration_min = overTimeRs.getInt("duration_min");  //加班时长
        int  computingmode = overTimeRs.getInt("computingmode");  //加班规则
        int  durationrule = overTimeRs.getInt("durationrule");  //加班单位
        String  fromdatedb = Util.null2String(overTimeRs.getString("fromdatedb"));  //流程上加班开始日期
        String  fromtimedb = Util.null2String(overTimeRs.getString("fromtimedb"));  //流程上加班开始时间
        String  todatedb = Util.null2String(overTimeRs.getString("todatedb"));  //流程上加班结束日期
        String  totimedb = Util.null2String(overTimeRs.getString("totimedb"));  //流程上加班结束时间
        String  tiaoxiuId = Util.null2String(overTimeRs.getString("tiaoxiuId"));  //调休id
        String  uuid = Util.null2String(overTimeRs.getString("uuid"));  //uuid
        String  flow_mins = Util.null2String(overTimeRs.getString("flow_mins"));  //流程时长
        String  card_mins = Util.null2String(overTimeRs.getString("card_mins"));  //打卡时长
        String  ori_belongdate = Util.null2String(overTimeRs.getString("ori_belongdate"));
        String  flow_dataid = Util.null2String(overTimeRs.getString("flow_dataid"));


        //生成调休
        Map<String, Object> otherParam = new HashMap<>();
        otherParam.put("overtime_type",0);
        tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceid,belongdate,duration_split_min+""
                ,"0","",requestid,"1",belongdate,otherParam);

        //生成加班
        paidleaveenable = 1;    //默认为调休
        int issplit = 1;        //区分是否为自定义流程拆分
        String flow_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,card_mins,ori_belongdate,flow_dataid,issplit)"+
                " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        new BaseBean().writeLog("==zj==(将超出36小时部分转为可调休加班)" + flow_overtime_sql);
        rs.executeUpdate(flow_overtime_sql,requestid,resourceid,fromdate,fromtime,todate,totime,duration_split_min,expiringdate,
                belongdate,workmins, durationrule,changetype,paidleaveenable,computingmode,tiaoxiuId,uuid,fromdatedb,fromtimedb,
                todatedb,totimedb,flow_mins,card_mins,ori_belongdate,flow_dataid,issplit);
    }

    /**
     * 这里将拆分原加班流程的时长重新修改下
     */
    public void setOverTimeFlow(RecordSet overTimeRs,int duration_min){
        RecordSet updateRs = new RecordSet();
        String  requestid = Util.null2String(overTimeRs.getString("requestid"));  //流程id
        String  resourceid = Util.null2String(overTimeRs.getString("resourceid"));  //人员id
        String  belongdate = Util.null2String(overTimeRs.getString("belongdate"));  //加班归属日期

        String flow_overtime_sql = "update kq_flow_overtime set duration_min="+duration_min
                                    +" where resourceid="+resourceid+" and requestid="+requestid+" and belongdate='"+belongdate+"'"+
                " and issplit is null";
        new BaseBean().writeLog("(修改原流程加班时长sql)" + flow_overtime_sql);
        updateRs.executeUpdate(flow_overtime_sql);
    }

    /**
     * 这里将不调休加班直接转为调休加班
     * @param overTimeRs
     * @param duration_min
     */
    public void setOverTimeFlowLeave(RecordSet overTimeRs,int duration_min){
        RecordSet updateLeaveRs = new RecordSet();
        Map<String, Object> otherParam = new HashMap<>();
        otherParam.put("overtime_type",0);
        String  requestid = Util.null2String(overTimeRs.getString("requestid"));  //流程id
        String  resourceid = Util.null2String(overTimeRs.getString("resourceid"));  //人员id
        String  belongdate = Util.null2String(overTimeRs.getString("belongdate"));  //加班归属日期
        String  tiaoxiuId = "";//调休id

        tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(resourceid,belongdate,duration_min+""
                ,"0","",requestid,"1",belongdate,otherParam);


        String flow_overtime_sql = "update kq_flow_overtime set paidleaveenable="+1+", tiaoxiuId="+tiaoxiuId
                +" where resourceid="+resourceid+" and requestid="+requestid+" and belongdate='"+belongdate+"'"+
                " and issplit is null";
        new BaseBean().writeLog("(将原加班流程转为调休加班sql)" + flow_overtime_sql);
        updateLeaveRs.executeUpdate(flow_overtime_sql);

    }


}
