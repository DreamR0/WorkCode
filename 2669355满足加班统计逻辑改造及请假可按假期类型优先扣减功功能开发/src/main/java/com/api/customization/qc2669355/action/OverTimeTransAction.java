package com.api.customization.qc2669355.action;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2669355.util.ActionUtil;
import com.api.customization.qc2669355.util.BalanceOfLeaveUtil;
import com.api.customization.qc2669355.util.LeaveCustomUtil;
import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.interfaces.schedule.BaseCronJob;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class OverTimeTransAction extends BaseCronJob {

    @Override
    public void execute() {
        String typeId ="leaveType_" + new BaseBean().getPropValue("qc2669355","typeId");//获取事假类型id
        String newLeaveType = new BaseBean().getPropValue("qc2669355","typeId");//获取事假类型id
        RecordSet resSelect = new RecordSet();
        String resourceSql = "select id,lastname from hrmresource ";
        String resourceId = ""; //人员id
        String fromDate = "";   //开始日期
        String toDate = "";    //结束日期
        User user = new User(1);//这个用来获取流程数据
        ActionUtil actionUtil = new ActionUtil();
        LeaveCustomUtil leaveCustomUtil = new LeaveCustomUtil();
        BalanceOfLeaveUtil balanceOfLeaveUtil = new BalanceOfLeaveUtil();
        List<String> lastMonthDays = actionUtil.getLastMonthDay();
        //获取开始、结束日期
        for (int i = 0; i <lastMonthDays.size() ; i++) {
            if (i == 0){
                fromDate = lastMonthDays.get(i);
            }
            if (i == 1){
                toDate = lastMonthDays.get(i);
            }
        }

        //每个人都遍历一遍
        resSelect.executeQuery(resourceSql);
        while (resSelect.next()){
            RecordSet overTimeRs = new RecordSet();
            resourceId = Util.null2String(resSelect.getString("id"));
            int overTimeMinsTotal = 0;  //累计上月加班时长
            boolean isSplit = false;    //判断该条流程是否包含临界点，进行拆分
            String overTimeSql = "select *" +
                    " from kq_flow_overtime where resourceid="+resourceId+
                    " and belongdate between '"+fromDate+"' and '"+toDate+"' and paidLeaveEnable=0 order by"+
                    " case when changetype=1 then 1 when changetype=3 then 2 else 3 end ";
            new BaseBean().writeLog("查询当前人员上月加班情况:" + JSON.toJSONString(overTimeSql));
            overTimeRs.executeQuery(overTimeSql);
            //优先级 节假日加班 - 休息日加班 - 工作日加班
            while (overTimeRs.next()){
                int  duration_min = overTimeRs.getInt("duration_min");  //加班时长
                //如果小于2160(36小时)就累加
                if (overTimeMinsTotal < 2160){
                    //如果已累计时长+此次流程加班时长>2160(36小时),将该条流程进行拆分，超出部分转为调休时长
                    if ( (overTimeMinsTotal + duration_min) > 2160){
                        int duration_split_min =  (overTimeMinsTotal + duration_min)-2160;
                        actionUtil.saveOverTimeLeave(overTimeRs,duration_split_min);//超出部分转为调休时长,并生成对应加班记录

                        int duration_original_min = duration_min - duration_split_min;//剩下未转调休时长
                        actionUtil.setOverTimeFlow(overTimeRs,duration_original_min);//将原来加班时长调整为，未超出加班时长
                        overTimeMinsTotal += duration_original_min;
                        isSplit = true;
                    }
                    if (!isSplit){
                        //说明该流程时长没有包含临界点，所以直接累加就行，不用拆分
                        overTimeMinsTotal += duration_min;
                    }
                }else if (overTimeMinsTotal >= 2160){
                    //如果累计超过36小时就直接转为调休
                    actionUtil.setOverTimeFlowLeave(overTimeRs,duration_min);
                }
                new BaseBean().writeLog("==zj==(当月累计加班时长)" + resourceId + " | " + overTimeMinsTotal );
            }


            String kqMonth = "";    //考勤年月
            if (fromDate.length() >= 7){
                kqMonth = fromDate.substring(0,7);
            }

            //如果已经抵扣就不执行了
            if (!balanceOfLeaveUtil.selectRemainRestAmount(resourceId,kqMonth)){
                //再对事假进行调休扣减
                Map<String, Object> leaveAbsence = leaveCustomUtil.getLeaveAbsence(resourceId, user);
                Double absenceHoursD =Util.getDoubleValue(Util.null2String(leaveAbsence.get(typeId)),0) ;//获取上个月事假
                Double allRestAmountD = balanceOfLeaveUtil.getRestAmount(resourceId);//获取总共调休余额
                new BaseBean().writeLog("==zj==(事假扣减信息)" + resourceId + " | " + absenceHoursD + " | " + allRestAmountD + " | " + newLeaveType);
                balanceOfLeaveUtil.addUsedAmount(user,resourceId,toDate,absenceHoursD,allRestAmountD,newLeaveType);
            }







        }
    }
}
