package com.api.customization.kq.qc2241191;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CheckSignActionUtil {
    private RecordSet rs = new RecordSet();
    private Map timeMap= new HashMap();       //记录检查打卡信息
    private Map signNonMap = new HashMap();

    private SimpleDateFormat sdfH = new SimpleDateFormat("HH:mm");      //转时分
    private SimpleDateFormat sdfD = new SimpleDateFormat("yyyy-MM-dd");      //转年月日
    private String ASD ="";    //获取实际开始日期
    private String AST ="" ;    //获取实际开始时间
    private String AED ="" ;    //获取实际结束日期
    private String AET ="" ;    //获取实际结束时间
    //Action 对打卡数据进行整理，不符合流程实际时间范围的删除
    public Map CheckSignTime(String userid, String signDate, String signTime, Map<String,String>timeMap){
        //==zj 先把流程表里的实际时间范围给放出来，转成日期类型
        ASD = Util.null2String(timeMap.get("ASD"));     //获取实际开始日期
        AST = Util.null2String(timeMap.get("AST"));     //获取实际开始时间
        AED = Util.null2String(timeMap.get("AED"));     //获取实际结束日期
        AET = Util.null2String(timeMap.get("AET"));     //获取实际结束时间

        new BaseBean().writeLog("==zj==(日期转换)" +ASD+" | "+AST+" | "+AED+" | "+AET);

        try{
            Date ASDD = sdfD.parse(ASD);    //实际开始日期
            Date ASTD = sdfH.parse(AST);    //实际开始时间
            Date AEDD = sdfD.parse(AED);    //实际结束日期
            Date AETD = sdfH.parse(AET);    //实际结束时间

            Date signDateD = sdfD.parse(signDate);  //签到日期
            Date signTimeD = sdfH.parse(signTime);  //签到时间

            //==zj 先查看打卡日期时间范围是否在流程范围内
            new BaseBean().writeLog("==zj==(判断检查流程是否在实际范围内)" +signDateD.after(ASDD)+"&&"+signDateD.before(AETD)+"&&"+signTimeD.after(ASTD)+"&&"+signTimeD.before(AEDD));
            if ((signDateD.after(ASDD)&&signDateD.before(AETD))&&(signTimeD.after(ASTD)&&signTimeD.before(AETD))){
                //如果符合流程时间范围再查看是否在当天班次的打卡时间范围内
                OutSignUtil osu = new OutSignUtil();
                timeMap = osu.OutSignTimeCheck(userid,signDate,signTime);       //检查之后，返回结果
                new BaseBean().writeLog("==zj==(Action返回打卡检查结果)" + JSON.toJSONString(timeMap));

                if ("0".equals(timeMap.get("result"))){
                    //如果为0则代表该卡是无效卡，删除并记录一下
                    signNonMap.put("userId",userid);
                    signNonMap.put("signDate",signDate);
                    signNonMap.put("signTime",signTime);
                    signNonMap.put("msg","不符合班次时间范围");

                   String sql1="SET XACT_ABORT ON BEGIN TRANSACTION delete_sign delete mobile_sign where operater="+userid+" and operate_date='"+signDate+"' and operate_time='"+signTime+"'";
                   String sql2=" delete hrmschedulesign where userId="+userid+" and signDate='"+signDate+"' and signTime='"+signTime+"' and signFrom='e9_mobile_out' COMMIT TRANSACTION delete_sign";
                   String sql = sql1+sql2;
                   new BaseBean().writeLog("==zj==(Action无效卡--不符合班次范围)" + sql);
                   rs.executeUpdate(sql);
                }
            }else {
                //如果打卡数据不符合流程时间范围就从“签到表”和“外勤签到表”里删除。
                //记录一下无效卡
                signNonMap.put("userId",userid);
                signNonMap.put("signDate",signDate);
                signNonMap.put("signTime",signTime);
                signNonMap.put("msg","不符合流程时间范围");

                String sql1="SET XACT_ABORT ON BEGIN TRANSACTION delete_sign delete mobile_sign where operater="+userid+" and operate_date='"+signDate+"' and operate_time='"+signTime+"'";
                String sql2=" delete hrmschedulesign where userId="+userid+" and signDate='"+signDate+"' and signTime='"+signTime+"' and signFrom='e9_mobile_out'  COMMIT TRANSACTION delete_sign";
                String sql = sql1+sql2;
                new BaseBean().writeLog("==zj==(Action无效卡--不符合流程范围)" + sql);
                rs.executeUpdate(sql);
            }
        }catch (Exception e){
                new BaseBean().writeLog("==zj==(Action--e)" + e);
        }

        return signNonMap;
    }
}
