package com.api.customization.qc2658035.action;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2658035.util.OverTimeUtil;
import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 值班流程归档，生成调休和加班时长
 */
public class DutyOverTimeAction implements Action {

    @Override
    public String execute(RequestInfo requestInfo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        RecordSet rs = new RecordSet();
        Map<String, String> mainDatas = new HashMap<>();
        OverTimeUtil overTimeUtil = new OverTimeUtil();
        Map<String,Object> otherParam = new HashMap<String,Object>();
        int dutyOverTimes = 0;//值班时长
        String requestId = requestInfo.getRequestid();//流程id
        String tiaoxiuId = "";//调休id
        String workingHours = "";//工作时长
        int unit = KQOvertimeRulesBiz.getMinimumUnit();//获取最小加班单位


        try {
            //获取主表数据
            Property[] properties = requestInfo.getMainTableInfo().getProperty();
            for (Property propertie : properties) {
                mainDatas.put(propertie.getName(), propertie.getValue());
            }

            //获取值班开始日期和结束日期
            LocalDate startDate = LocalDate.parse( Util.null2String(mainDatas.get("zbksrq")));
            LocalDate endDate = LocalDate.parse(Util.null2String(mainDatas.get("zbjsrq")));
            String startDateDB= startDate.format(formatter);//值班流程开始时间
            String endDateDB= endDate.format(formatter);//值班流程结束时间
            String resourceId = Util.null2String(mainDatas.get("sqr"));
            List<LocalDate> dateRange = new ArrayList<>();
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                dateRange.add(currentDate);
                currentDate = currentDate.plusDays(1);
            }


            for (LocalDate date : dateRange) {
                String kqDate = date.format(formatter);
                String sql  = "select * from kq_flow_overtime where resourceid="+resourceId+" and belongdate = '"+kqDate+"'";
                new BaseBean().writeLog("==zj==(值班日期当天是否有加班数据sql)" + JSON.toJSONString(sql));
                rs.executeQuery(sql);
                if (rs.next())continue;

                int changeType = overTimeUtil.getChangeType(resourceId,kqDate);//获取当天类型
                /*if (changeType == 1){
                    //如果是节假日就转为休息日调休
                    changeType = 3 ;
                }*/
                dutyOverTimes =  overTimeUtil.getdutyOverTimes(0,0,kqDate,resourceId);//获取值班调休时长


                //生成加班时长
                String flow_overtime_sql = "insert into kq_flow_overtime(requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,workMins,durationrule,changetype,paidLeaveEnable,computingMode,fromdatedb,fromtimedb,todatedb,totimedb,tiaoxiuId,isdutywork)"+
                        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";

                boolean isUp = rs.executeUpdate(flow_overtime_sql, requestId,resourceId,kqDate,"08:30:00",kqDate,"17:00:00",dutyOverTimes,"",kqDate,"",
                        unit,changeType,0,"4",startDateDB,"08:30",endDateDB,"17:00",tiaoxiuId,1);
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(Action)" + JSON.toJSONString(e));
        }


        return  Action.SUCCESS;
    }
}
