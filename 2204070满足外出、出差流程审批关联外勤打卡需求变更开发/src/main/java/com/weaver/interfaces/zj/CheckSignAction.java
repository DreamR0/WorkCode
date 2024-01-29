package com.weaver.interfaces.zj;

import com.alibaba.fastjson.JSON;
import com.api.customization.kq.qc2241191.CheckSignActionUtil;
import com.engine.kq.wfset.util.SplitActionUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckSignAction implements Action {
    private RecordSet rs = new RecordSet();
    private List signNonList = new ArrayList(); //记录无效卡信息
    private String businessSchedule = new BaseBean().getPropValue("qc2204070","businessSchedule");  //获取出差表数据库名
    private String outSchedule = new BaseBean().getPropValue("qc2204070","outSchedule");            //获取公出表数据库名
    private String resourceIdSchedule = Util.null2String(new BaseBean().getPropValue("qc2204070","resourceId"));    //数据库表人员id字段

    private  String requestid="";       //流程id
    private String resourceid="";       //人员id

    private String ESD = Util.null2String(new BaseBean().getPropValue("qc2204070","ESD"));    //预计开始日期
    private String EST = Util.null2String(new BaseBean().getPropValue("qc2204070","EST"));    //预计开始时间
    private String EED = Util.null2String(new BaseBean().getPropValue("qc2204070","EED"));    //预计结束日期
    private String EET = Util.null2String(new BaseBean().getPropValue("qc2204070","EET"));    //预计结束时间

    private String ASD = Util.null2String(new BaseBean().getPropValue("qc2204070","ASD"));    //实际开始日期
    private String AST = Util.null2String(new BaseBean().getPropValue("qc2204070","AST"));    //实际开始时间
    private String AED = Util.null2String(new BaseBean().getPropValue("qc2204070","AED"));    //实际结束日期
    private String AET = Util.null2String(new BaseBean().getPropValue("qc2204070","AET"));    //实际结束时间


    private Map<String,String> timeMap= new HashMap();

    @Override
    public String execute(RequestInfo requestInfo) {
        requestid = requestInfo.getRequestid();    //获取当前流程id
        //==zj 先判断requestid是否来自公出表,还是出差表
        String sql1="select "+resourceIdSchedule+","+ESD+","+EST+","+EED+","+EET+","+ASD+","+AST+","+AED+","+AET+" from " + outSchedule +" where requestid="+requestid;
        String sql2=" union "+resourceIdSchedule+","+ESD+","+EST+","+EED+","+EET+","+ASD+","+AST+","+AED+","+AET+" from " + businessSchedule +" where requestid="+requestid;
        String sql = sql1+sql2;
        new BaseBean().writeLog("==zj==(出差表和公出表联合查询)" + sql);
        rs.executeQuery(sql);
        if (rs.next()){
            ESD = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","ESD")));    //获取预计开始日期
            EST = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","EST")));    //获取预计开始时间
            EED = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","EED")));    //获取预计结束日期
            EET = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","EET")));    //获取预计结束时间
            resourceid = Util.null2String(rs.getString(resourceIdSchedule));

            ASD = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","ASD")));    //获取实际开始日期
            AST = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","AST")));    //获取实际开始时间
            AED = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","AED")));    //获取实际结束日期
            AET = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2204070","AET")));    //获取实际结束时间
            timeMap.put("ASD",ASD);
            timeMap.put("AST",AST);
            timeMap.put("AED",AED);
            timeMap.put("AET",AET);

        }

        //==zj 获取流程预计时间段范围内的打卡数据
        sql = "select * from hrmschedulesign where userid="+resourceid+" and (signTime between '"+EST+"' and '"+EET+"') and (signDate between'"+
                ESD+"' and '"+EED+"') and signFrom='e9_mobile_out'";
        new BaseBean().writeLog("==zj==(获取流程预计时间范围内的打卡数据sql)" +sql);
        rs.executeQuery(sql);
        while (rs.next()){
            //开始循环检查打卡
            String userid = Util.null2String(rs.getString("userid"));   //获取人员id
            String signDate = Util.null2String(rs.getString("signDate"));   //获取签到日期
            String signTime = Util.null2String(rs.getString("signTime"));   //获取签到时间

            CheckSignActionUtil csau = new CheckSignActionUtil();
            Map signNonMap = csau.CheckSignTime(userid, signDate, signTime, timeMap);//检查打卡是否符合流程实际范围
            signNonList.add(signNonMap);
        }

        new BaseBean().writeLog("==zj==(打印一下删除的无效卡)" + JSON.toJSONString(signNonList));

        //处理加班规则
        SplitActionUtil.pushOverTimeTasksAll(ASD,AED,""+resourceid);
        return null;
    }
}
