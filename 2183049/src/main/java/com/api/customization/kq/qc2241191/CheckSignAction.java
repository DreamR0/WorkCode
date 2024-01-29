package com.api.customization.kq.qc2241191;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

import java.text.SimpleDateFormat;

public class CheckSignAction implements Action {
    @Override
    public String execute(RequestInfo requestInfo) {
        RecordSet rs = new RecordSet();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long l = System.currentTimeMillis();    //获取当前时间戳
        String  fromDate=sdf.format(l);             //转为年月日
        String requestid = requestInfo.getRequestid();  //获取流程id
        String sql="";
        Boolean result = false;                         //流程
        String businessSchedule = new BaseBean().getPropValue("qc2204070","businessSchedule"); //出差表数据库名
        String outSchedule = new BaseBean().getPropValue("qc2204070","outSchedule");           //公出表数据库名
        String resourceId ="";      //人员id
        String fromTime = "";       //流程开始时间
        String toTime = "";         //流程结束时间



        //==zj 先查出差表看是否匹配
        sql="select * from " + businessSchedule + " where fromDate='" + fromDate + "' and requestId='" + requestid + "'";
        rs.executeQuery(sql);
        if (rs.next()){
            resourceId = Util.null2String(rs.getString("resourceId"));
            fromTime =  Util.null2String(rs.getString("fromTime"));
            toTime =  Util.null2String(rs.getString("toTime"));
            result = true;

            OutSignUtil osu = new OutSignUtil();

        }



        //==zj 如果出差表不匹配再看公出表
        if (!result){

        }




        return null;
    }
}
