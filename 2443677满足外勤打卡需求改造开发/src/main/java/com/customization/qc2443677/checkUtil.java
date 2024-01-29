package com.customization.qc2443677;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class checkUtil {
    private String tableName = new BaseBean().getPropValue("qc2443677", "tableName");//获取流程表单名称

    /**
     * 这里判断打卡的时候是否有打卡流程在审批中
     * @param user
     * @return
     */
    public boolean checkSignWf(User user){
        Boolean isHave = false;//是否有流程在审批中
        Boolean isAfter = false;//判断打卡时间是上午还是下午
        //处理打卡时间和日期
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime localTime = LocalTime.now();
        String signTime =localTime.format(dateTimeFormatter);
        String signDate = LocalDate.now().format(dateFormatter);

        //这里因为上午的外勤审批流程如未结束，不可影响下午的下班的外勤，所以做下判断
        if (signTime.compareTo("12:00") < 0) {
            //说明打卡时间是上午
            isAfter = false;
        }else {
            isAfter = true;
        }

        RecordSet rs = new RecordSet();
        String sqlBase ="";
        String sqlWhere = "";
        try{
            sqlBase = "select * from "+tableName + " a left join workflow_requestbase b on a.requestid = b.requestid ";
            if (isAfter){
                //下午
                sqlWhere = "where a.dkr='"+user.getUID()+"'and  b.currentnodetype !='" + 3+"' and '"+signDate+"' = a.sqrq and a.dksj >= '12:00' and  b.currentnodetype = 1";
            }else{
                //上午
                sqlWhere = "where a.dkr='"+user.getUID()+"'and b.currentnodetype !='" + 3+"' and '"+signDate+"' = a.sqrq and a.dksj < '12:00' and b.currentnodetype = 1";
            }

            String sql = sqlBase + sqlWhere;
            new BaseBean().writeLog("==zj==(检查当前人员是否有正在审批的外勤打卡流程)sql：" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                isHave = true;
            }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(查看当前人员是否有正在审批外勤打卡流程)异常：" + e);
        }
        return isHave;
    }

    /**
     * 这里是打卡的时候,看有没有退回的流程，有的话就更新该流程数据再提交
     * @param user
     * @return
     */
    public Map<String,String> checkReject(User user, Map<String,Object> otherParams, JSONArray mainTable){
        SimpleDateFormat times = new SimpleDateFormat("HH:mm");
        RecordSet rs = new RecordSet();
        int requestId = 0;
        Map<String,String> result = new HashMap<>();

        String signDate = Util.null2String(otherParams.get("signDate"));//打卡日期

        String signTime = Util.null2String(otherParams.get("signTime"));//打卡时间
        new BaseBean().writeLog("==zj==(退回打卡时间)" +signTime);
        if (signTime.length() > 5){
            signTime = signTime.substring(0,5);
        }
        String address = Util.null2String(otherParams.get("address"));//打卡地点
        String signType = Util.null2String(otherParams.get("signType"));//考勤类型
        String isInCom = Util.null2String(otherParams.get("isInCom"));//是否是有效打卡
        String signfrom = Util.null2String(otherParams.get("signfrom"));//打卡来源
        String belongdate = Util.null2String(otherParams.get("belongdate"));//打卡归属日期
        String remark = Util.null2String(otherParams.get("remark"));//备注

        //这里判断打卡时间是上午还是下午
        Boolean isAfter = true; //默认下午
        if (signTime.compareTo("12:00") < 0) {
            //说明打卡时间是在上午
            isAfter = false;
        }
        //这里判断是否有对应时间段退回的流程
        String sqlBase = "select * from "+tableName+" a left join workflow_currentoperator b on a.requestid = b.requestid ";
        String sqlWhere = "where  b.isbereject='1' and b.isremark in('0') and a.sqr="+user.getUID() + " and a.dkrq='"+signDate+"' and a.dksj >= '12:00'";
        if (!isAfter){
            sqlWhere = "where  b.isbereject='1' and b.isremark in('0') and a.sqr="+user.getUID() + " and a.dkrq='"+signDate+"'  and a.dksj < '12:00'";
        }
        String sqlSelect = sqlBase + sqlWhere;
        new BaseBean().writeLog("==zj==(查询是否有退回的流程)sql：" + sqlSelect);
        rs.executeQuery(sqlSelect);
        if (rs.next()){
            requestId = Util.getIntValue(rs.getString("requestid"),0);
        }


        if (requestId > 0){
            //说明有退回的流程，这里就更新流程数据，正常情况下，人员工作信息不会变化，所以只要更新打卡时间和打卡地点
           /* String sqlUpdate = "update "+ tableName+" set dkrq='"+signDate+"', dksj='"+signTime+"', dkdd='"+address+"', kqlx='"+signType+"',sfsyxkqdk='"+isInCom+"', kqly='"+signfrom+"', gsrq='"+belongdate+"', bz='"+remark+"' where requestid='"+requestId+"'";
            new BaseBean().writeLog("==zj==(更新退回流程数据)sql：" + sqlUpdate);
            boolean isUpdate = rs.executeUpdate(sqlUpdate);
            if (isUpdate){
                result.put("code","1");
                result.put("requestid",requestId+"");
            }else {
                //有对应流程但是更新失败
                result.put("code","-1");
                result.put("requestid",requestId+"");
            }
            return result;*/
            new BaseBean().writeLog("==zj==(退回的流程requestId)" +requestId);
            Boolean isUpset = signRequestUtil.submitRequest(user, requestId, mainTable);
            if (isUpset){
                result.put("code","1");
                result.put("requestid",requestId+"");
                return result;
            }else {
                result.put("code","-1");
                result.put("requestid",requestId+"");
                return result;
            }
        }
        result.put("code","0");
        return result;
    }

    /**
     * 判断是否在班次打卡范围
     * @return
     */
    public Boolean checkSignTime(User user){

        //处理打卡时间和日期
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime localTime = LocalTime.now();
        String signTime =localTime.format(dateTimeFormatter);
        String signDate = LocalDate.now().format(dateFormatter);

        //获取当前人员的班次信息
        KQWorkTime kt = new KQWorkTime();
        WorkTimeEntity workTime = kt.getWorkTime(user.getUID()+"", signDate);
        new BaseBean().writeLog("==zj==(workTime)" + JSON.toJSONString(workTime));
        List<TimeScopeEntity> signTimeList = workTime.getSignTime();    //获取人员当前考勤list
        new BaseBean().writeLog("==zj==(signTimeList)" +JSON.toJSONString(signTimeList));

        //先设置为不能打卡
        return false;

    }
}
