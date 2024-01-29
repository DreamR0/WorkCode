package com.api.customization.qc2757479.util;

import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.entity.KQOvertimeRulesDetailEntity;
import com.engine.kq.timer.KQOvertimeCardBean;
import com.engine.kq.wfset.bean.SplitBean;
import com.google.common.collect.Maps;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KqUtil {

    /**
     * 判断人员休息日是否包含出差流程，如果有出差流程就不需要打卡，以流程为准
     * @return
     */
    public Boolean isBusiness(String resourceId,String kqDate){
        Boolean isBusiness = false;
        //判断是不是休息日，不是休息日就走系统标准
        int changeType = KQOvertimeRulesBiz.getChangeType(resourceId,kqDate);
        if (changeType != 2){
            return false;
        }
            RecordSet rs = new RecordSet();
            String sql = "select * from kq_flow_split_evection where resourceid='"+resourceId+"' and belongdate='"+kqDate+"' ";
            new BaseBean().writeLog("休息日是否有出差流程" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                isBusiness = true;
            }
        return isBusiness;
    }

    /**
     * 覆盖下打卡时长
     */
    public double buildsignCard(KQOvertimeCardBean kqOvertimeCardBean, String splitDate, KQTimesArrayComInfo kqTimesArrayComInfo){
        double signMins = 0;

        String signinDate = kqOvertimeCardBean.getSigninDate();
        String signinTime = kqOvertimeCardBean.getSigninTime();
        String signoutDate = kqOvertimeCardBean.getSignoutDate();
        String signoutTime = kqOvertimeCardBean.getSignoutTime();



        if(signinDate.compareTo(splitDate) > 0){
            signinTime = kqTimesArrayComInfo.turn24to48Time(signinTime);
            if(signinTime.length() > 0){
                signinTime = signinTime+ ":00";
            }
        }
        if(signoutDate.compareTo(splitDate) > 0){
            signoutTime = kqTimesArrayComInfo.turn24to48Time(signoutTime);
            if(signoutTime.length() > 0){
                signoutTime = signoutTime+ ":00";
            }
        }

        int signinTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signinTime);
        int signoutTimeIndex = kqTimesArrayComInfo.getArrayindexByTimes(signoutTime);

        if(signinTimeIndex < signoutTimeIndex){
            signMins = signoutTimeIndex - signinTimeIndex;
        }

        return signMins;
    }

    /**
     * 生成加班时长
     */
    public void buildCustomOverTime(String reosurceId, SplitBean splitBean, KQOvertimeCardBean kqOvertimeCardBean, String splitDate, String realSplitDate,KQTimesArrayComInfo kqTimesArrayComInfo, int changeType, KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity){
        RecordSet rs = new RecordSet();
        Map<String,Object> otherParam = Maps.newHashMap();
        double overTimesMins = 0.0;//实际加班时长
        double subMins = 7.5 * 60;      ///应减分钟数
        String tiaoxiuId = "";     //调休id
        String workingHours = "";  //工作时长
        double signMins = 0.0;//打卡时长
        String overtime_uuid = UUID.randomUUID().toString();
        int computingMode = 4;
        String flow_dataid = "";

        //打卡数据
        String signinDate = kqOvertimeCardBean.getSigninDate();
        String signinTime = kqOvertimeCardBean.getSigninTime();
        String signoutDate = kqOvertimeCardBean.getSignoutDate();
        String signoutTime = kqOvertimeCardBean.getSignoutTime();
        //工作日出差加班流程
        String dataid = splitBean.getDataId();
        String detailid = splitBean.getDetailId();
        String flow_fromdate = splitBean.getFromDate();
        String flow_fromtime = splitBean.getFromTime();
        String flow_todate = splitBean.getToDate();
        String flow_totime = splitBean.getToTime();
        String fromdatedb = splitBean.getFromdatedb();
        String fromtimedb = splitBean.getFromtimedb();
        String todatedb = splitBean.getTodatedb();
        String totimedb = splitBean.getTotimedb();
        String requestid = splitBean.getRequestId();
        String overtime_type = splitBean.getOvertime_type();
        double d_mins = splitBean.getD_Mins();  //加班流程时长
        int paidLeaveEnable = getPaidLeaveEnable(kqOvertimeRulesDetailEntity, overtime_type);//是否调休

        //加班规则
        int unit = KQOvertimeRulesBiz.getMinimumUnit();

        //计算当天打卡时长
        signMins = buildsignCard( kqOvertimeCardBean,splitDate,kqTimesArrayComInfo);

        //计算实际加班工时时长： 打卡时长 - 7.5
        overTimesMins = signMins - subMins;
        if (overTimesMins > 0){
            overTimesMins = overTimesMins > d_mins?d_mins:overTimesMins;//不超过流程时长
        }else {
            overTimesMins = 0.0;
        }
        otherParam.put("overtime_type", overtime_type);

        if (overTimesMins > 0){
            tiaoxiuId = KQBalanceOfLeaveBiz.addExtraAmountByDis5(reosurceId,realSplitDate,overTimesMins+"","0",workingHours,requestid,"1",realSplitDate,otherParam);

            String flow_overtime_sql = "insert into kq_flow_overtime (requestid,resourceid,fromdate,fromtime,todate,totime,duration_min,expiringdate,belongdate,"
                    + "workMins,durationrule,changetype,paidLeaveEnable,computingMode,tiaoxiuId,uuid,fromdatedb,fromtimedb,todatedb,totimedb,flow_mins,card_mins,ori_belongdate,flow_dataid)"+
                    " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";

            boolean isUp = rs.executeUpdate(flow_overtime_sql, requestid,reosurceId,signinDate,signinTime,signoutDate,signoutTime,overTimesMins,"",realSplitDate,
                    "",unit,changeType,paidLeaveEnable,computingMode,tiaoxiuId,overtime_uuid,fromdatedb,fromtimedb,todatedb,totimedb,d_mins,signMins,splitDate,flow_dataid);
        }

    }

    /**
     * 判断是否开启了调休
     * @param kqOvertimeRulesDetailEntity
     * @param overtime_type
     * @return
     */
    public int getPaidLeaveEnable(KQOvertimeRulesDetailEntity kqOvertimeRulesDetailEntity,
                                  String overtime_type) {
        int paidLeaveEnable = -1;
        if (kqOvertimeRulesDetailEntity != null){
            paidLeaveEnable = kqOvertimeRulesDetailEntity.getPaidLeaveEnable();
            paidLeaveEnable = paidLeaveEnable == 1?1:0;
        }
        int paidLeaveEnableType = kqOvertimeRulesDetailEntity.getPaidLeaveEnableType();
        if(2 == paidLeaveEnableType){
//      logOvertimeMap(overtimeLogMap, overtime_type, flow_cross_key+"|关联调休与否来自于流程选择,加班类型下拉框值|overtime_type");
            if("0".equalsIgnoreCase(overtime_type)){
                paidLeaveEnable = 1;
            }else if("1".equalsIgnoreCase(overtime_type)){
                paidLeaveEnable = 0;
            }else{
                paidLeaveEnable = 0;
            }
        }
        return paidLeaveEnable;
    }


}
