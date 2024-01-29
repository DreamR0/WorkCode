package com.api.customization.qc2640833.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class KqUtil {
    private String tableName = new BaseBean().getPropValue("qc2640833","tableName");//自有工人考勤流程主表
    private String tableName_dt = new BaseBean().getPropValue("qc2640833","tableName_dt");//自有工人考勤流程明细表
    /**
     * 检查考勤人员当前时间是否符合打卡条件
     * @param params
     * @param user
     * @return
     */
    public Map<String, Object> check(Map<String,Object> params, User user){
        Map<String,Object> error =null;
        String userId = Util.null2String(params.get("grxm"));  //获取考勤人员
        String kqTime =  Util.null2String(params.get("dksj"));  //打卡时间
        String kqDate =  Util.null2String(params.get("dkrq"));  //打卡日期
        String kqType = Util.null2String(params.get("dklx"));  //打卡类型
        String projectName = Util.null2String(params.get("xmmc"));  //项目名称



        LocalTime kqTimeL = LocalTime.parse(kqTime);//打卡时间
        LocalTime timeAML = LocalTime.parse("12:00");//上午进场规定时间
        LocalTime timePML = LocalTime.parse("18:00");//下午进场规定时间


        //进场打卡判断
        new BaseBean().writeLog("==zj==(打卡信息)"  + userId + " | " + kqDate +  " | "+kqType);

        if ("0".equals(kqType)){
            //上午进场
            error = new HashMap<>();
            if (timeAML.compareTo(kqTimeL) < 0){
                error = getResultMapin("0",0);//检查打卡时间
            }
            if (error.size() <= 0){
                error = checkrRepeat(userId,kqType,kqDate,projectName);//检查是否重复
            }
        }else if ("2".equals(kqType)){
            //下午进场
            error = new HashMap<>();

            if(timeAML.compareTo(kqTimeL) >0 || timePML.compareTo(kqTimeL) <= 0){  //检查打卡时间
                error = getResultMapin("2",0);
            }
            new BaseBean().writeLog("==zj==(下午进场打卡--检测上午考勤完整)" + error.size() + " | " + userId + " | " + kqDate +  " | "+kqType);
            if (error.size() <= 0){
                error = checkIn(userId,kqType,kqDate,projectName);//检查上午考勤记录是否完整
            }

            if (error.size() <= 0){
                error = checkrRepeat(userId,kqType,kqDate,projectName);//检查是否重复
            }
        }else if ("4".equals(kqType)){
            //晚上进场
            error = new HashMap<>();
            if (timePML.compareTo(kqTimeL) >0 ){//检查打卡时间
                error = getResultMapin("4",0);
            }
            new BaseBean().writeLog("==zj==(晚上进场打卡--检测上下午考勤完整)" + error.size() + " | " + userId + " | " + kqDate+  " | "+kqType);
            if (error.size() <= 0){
                error = checkIn(userId,kqType,kqDate,projectName);//检查上下午考勤记录是否完整
            }
            if (error.size() <= 0){
                error = checkrRepeat(userId,kqType,kqDate,projectName);//检查是否重复
            }
        }

        //出场打卡判断
        if ("1".equals(kqType)){
            //检查是否满足打卡条件
            error = new HashMap<>();
            error = checkOut(userId,kqType,kqDate,projectName);
            new BaseBean().writeLog("==zj==(error1)" + JSON.toJSONString(error));
            if (error.size() <= 0){
                //检查是否重复打卡
                error = checkrRepeat(userId,kqType,kqDate,projectName);
            }


        }else if ("3".equals(kqType)){
            //检查是否满足打卡条件
            error = new HashMap<>();
            error = checkOut(userId,kqType,kqDate,projectName);
            new BaseBean().writeLog("==zj==(error3)" + JSON.toJSONString(error));

            if (error.size() <= 0){
                error = checkrRepeat(userId,kqType,kqDate,projectName);
            }

        }else if ("5".equals(kqType)){
            //检查是否满足打卡条件
            error = new HashMap<>();
            error = checkOut(userId,kqType,kqDate,projectName);
            new BaseBean().writeLog("==zj==(error5)" + JSON.toJSONString(error));

            if (error.size() <= 0){
                error = checkrRepeat(userId,kqType,kqDate,projectName);
            }

        }
        new BaseBean().writeLog("(error)" + JSON.toJSONString(error));
        if (error.size() > 0){
            return error;
        }
        Map<String,Object> result = new HashMap<>();
        result.put("status","1");
        result.put("kqType",kqType);
        result.put("msg","无异常状态");
        return result;


    }

    //获取进场错误提示信息
    public Map<String, Object> getResultMapin(String kqType,int repeat){
        String type = "";
        String msg = "";
        switch (kqType){
            case "0":
                type = "上午进场";
                msg = "未在规定打卡时间内";
                break;
            case "2":
                type = "下午进场";
                msg = "未在规定打卡时间内";
                break;
            case "4":
                type = "晚上进场";
                msg = "未在规定打卡时间内";
                break;
        }
        if (repeat == 1){
            msg = "当天"+type+"已打卡，请勿重复打卡";
        }

        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("status","-1");
        resultMap.put("kqtype",type);
        resultMap.put("msg",msg);
        return resultMap;
    }

    //获取出场提示错误信息
    public Map<String, Object> getResultMapout(String kqType,int repeat,String error){
        String type = "";
        String msg = "";
        switch (kqType){
            case "1":
                type = "上午出场";
                msg = "不存在上午进场打卡记录，不能打卡";
                break;
            case "3":
                type = "下午出场";
                msg = "不存在下午进场打卡记录，不能打卡";
                break;
            case "5":
                type = "晚上出场";
                msg = "不存在晚上进场打卡记录，不能打卡";
                break;
        }
        if (repeat == 1){
            msg = "当天"+type+"已打卡，请勿重复打卡";
        }
        if (!"".equals(error)){
            msg = error;
        }


        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("status","-1");
        resultMap.put("kqtype",type);
        resultMap.put("msg",msg);
        return resultMap;
    }

    //检查是否已有打卡记录
    public Map<String, Object> checkrRepeat(String userId,String kqType,String kqDate,String projectName){
        Map<String,Object> resultMap = new HashMap<>();
        String sql = "";
        RecordSet rs = new RecordSet();

        /*sql = "select * from "+tableName+" where grxm='"+userId+"' and tbrq='"+kqDate+"' and dklx='"+kqType+"' and xmmc='"+projectName+"'";*/
        sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx='"+kqType+"' and a.xmmc='"+projectName+"'";
        new BaseBean().writeLog("==zj==(检测重复打卡sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            resultMap = getResultMapin(kqType,1);
        }
        return resultMap;
    }

    //出场打卡条件检测
    public Map<String, Object> checkOut(String userId,String kqType,String kqDate,String projectName){
        Map<String,Object> resultMap = new HashMap<>();
        String sql = "";
        RecordSet rs = new RecordSet();
        Boolean isHave = false;
        int count = 0;
        //上午出场
        if ("1".equals(kqType)){
            sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where  b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx='"+0+"' and a.xmmc='"+projectName+"'";//检测是否有上午进场打卡记录
            new BaseBean().writeLog("==zj==(检测是否有上午进场记录sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                isHave = true;
            }
            if (!isHave){
                //说明没有上午进场打卡记录
                resultMap = getResultMapout(kqType,0,"");
            }
            return resultMap;
        }else if ("3".equals(kqType)){
            //下午出场
            sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where  b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx='"+2+"' and a.xmmc='"+projectName+"'";//检测是否有下午进场打卡记录
            new BaseBean().writeLog("==zj==(检测是否有下午进场记录sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                isHave = true;
            }
            if (!isHave){
                //说明没有下午进场打卡记录
                resultMap = getResultMapout(kqType,0,"");
                return resultMap;
            }
            if (resultMap.size() <= 0){
                //再检查是否重复打卡
                resultMap = checkrRepeat(userId,kqType,kqDate,projectName);
            }
            return resultMap;
        }else if ("5".equals(kqType)){
            //晚上出场
            sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where  b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx='"+4+"' and a.xmmc='"+projectName+"'";//检测是否有晚上打卡记录
            new BaseBean().writeLog("==zj==(检测晚上是否有进场记录sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                isHave = true;
            }
            if (!isHave){
                //说明没有晚上进场打卡记录
                resultMap = getResultMapout(kqType,0,"");
            }else {
                if (resultMap.size() <= 0){
                    //检查是否重复打卡
                    resultMap = checkrRepeat(userId,kqType,kqDate,projectName);
                }
            }
            return resultMap;
        }
        return resultMap;
    }

    //进场打卡条件检测
    public  Map<String, Object> checkIn(String userId,String kqType,String kqDate,String projectName){
        String sql = "";
        RecordSet rs = new RecordSet();
        int count = 0;
        Map<String,Object> resultMap = new HashMap<>();
        if ("2".equals(kqType)){
            //如果是下午进场，判断上午出勤是否完整
            sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where  b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx in(0,1) and a.xmmc='"+projectName+"'";//检测上午打卡记录是否完整
            new BaseBean().writeLog("==zj==(下午进场上午出勤是否完整sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            while (rs.next()){
                count += 1;
            }
            if (count == 1 ){
                //说明上午打卡不完整
                resultMap = getResultMapout(kqType,0,"上午打卡记录不完整，不能打卡");
            }
        }
        if ("4".equals(kqType)){
            //如果是晚上进场，要判断下上下午出勤是否完整
            sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where  b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx in(0,1) and a.xmmc='"+projectName+"'";//检测上午打卡记录是否完整
            new BaseBean().writeLog("==zj==(晚上出场检查上午打卡记录是否完整)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            while (rs.next()){
                count += 1;
            }
            if (count == 1 ){
                //说明上午打卡不完整
                resultMap = getResultMapout(kqType,0,"上午打卡记录不完整，不能打卡");
            }else {
                count = 0;
                sql = "select a.*,b.* from "+tableName+" a left join "+tableName_dt+" b on a.id=b.mainid where  b.grxm='"+userId+"' and a.tbrq='"+kqDate+"' and a.dklx in(2,3) and a.xmmc='"+projectName+"'";//检测下午打卡记录是否完整
                new BaseBean().writeLog("==zj==(晚上出场检查下午打卡记录是否完整)" + JSON.toJSONString(sql));
                rs.executeQuery(sql);
                while (rs.next()){
                    count += 1;
                }
                if (count == 1){
                    resultMap = getResultMapout(kqType,0,"下午打卡记录不完整，不能打卡");
                }
            }
        }

        return resultMap;
    }


}
