package com.api.customization.qc20230816.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.*;
import com.engine.kq.util.KQDurationCalculatorUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KqReportUtil {

    /**
     *日报表数据
     * @param resourceId
     * @param fromDate
     * @param toDate
     * @return
     */
    public Map<String,Object> getDetialDatas(String resourceId, String fromDate, String toDate, User user,Boolean isExport){
        KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
        Map<String,Object> datas = new HashMap<>();
        Map<String,Object> data = null;
        Map<String,Object> tmpdatas = new HashMap<>();
        Map<String,Object> tmpdata = null;
        Map<String,Object> tmpmap = null;
        RecordSet rs = new RecordSet();
        String sql = "";
        try {
            sql = " select resourceid, kqdate, workMins, belatemins, graveBeLateMins, leaveearlymins, graveLeaveEarlyMins, absenteeismmins, forgotcheckMins, forgotBeginWorkCheckMins, "+
                    " leaveMins,leaveInfo,evectionMins,outMins,serialid,kqstatusam,kqstatuspm" +
                    " from kq_format_detail " +
                    " where resourceid = ? and kqdate>=? and kqdate<=? "+
                    " order by resourceid, kqdate, serialnumber  ";
            new BaseBean().writeLog("==zj==(sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql,resourceId, fromDate,toDate);
            KqFormatUtil kqFormatUtil = new KqFormatUtil();
            while (rs.next()) {
                String key = rs.getString("resourceid") + "|" + rs.getString("kqdate");
                String kqdate = rs.getString("kqdate");
                int workMins = rs.getInt("workMins");
                int beLateMins = rs.getInt("beLateMins");
                int leaveEarlyMins = rs.getInt("leaveEarlyMins");
                int graveBeLateMins = rs.getInt("graveBeLateMins");
                int absenteeismMins = rs.getInt("absenteeismMins");
                int graveLeaveEarlyMins = rs.getInt("graveLeaveEarlyMins");
                int forgotCheckMins = rs.getInt("forgotCheckMins");
                int forgotBeginWorkCheckMins = rs.getInt("forgotBeginWorkCheckMins");
                int leaveMins = rs.getInt("leaveMins");
                String leaveInfo = rs.getString("leaveInfo");
                int evectionMins = rs.getInt("evectionMins");
                int outMins = rs.getInt("outMins");
                //qc20230816 获取班次id
                int serialid = rs.getInt("serialid");
                //qc20230816 获取上下午考勤状态
                String kqstatusAM = rs.getString("kqstatusam");
                String kqstatusPM = rs.getString("kqstatuspm");
                 List<String> kqStatusList = new ArrayList<String>();
                 kqStatusList.add(kqstatusAM);
                 kqStatusList.add(kqstatusPM);

                String text = "";
                String tmptext ="";
                String flag ="true";
                if(datas.get(key)==null){
                    data = new HashMap<>();
                }else{
                    data = (Map<String,Object>)datas.get(key);
                    tmptext = Util.null2String(data.get("text"));
                }
                tmpdata = new HashMap<>();
                if(tmpdatas.get(key)!=null){
                    tmpmap = (Map<String,Object>)tmpdatas.get(key);
                    flag = Util.null2String(tmpmap.get("text"));
                }

                if (workMins<=0) {
                   /* //qc 这里判断下是不是节假日
                    int changeType = getChangeType(resourceId, kqdate);
                    if (changeType == 1){
                        if(text.length()>0) text +=" ";
                       //qc 通过排班表获取班次id
                        String serialIds = kqFormatUtil.getSerialIds(resourceId, kqdate);
                        if (!"".equals(serialIds)){
                            int id = Integer.parseInt(serialIds);
                            new BaseBean().writeLog("==zj==(id)" + id  + "   |   " + kqdate + "   |   " +resourceId);
                            text += kqFormatUtil.getSerialName(id,text,isExport);
                        }else {
                            text += SystemEnv.getHtmlLabelName(26593, user.getLanguage());
                        }
                    }else {*/
                        if(text.length()>0) text +=" ";
                        text += SystemEnv.getHtmlLabelName(26593, user.getLanguage());
                 /*   }*/

                } else {
                    for (int i = 0; i < kqStatusList.size(); i++) {
                        //这里开始处理上下午日历显示信息
                        if ("belate".equals(kqStatusList.get(i))){
                            //迟到
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20081, user.getLanguage());
                        }
                        if ("leaveEarly".equals(kqStatusList.get(i))){
                            //早退
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20082, user.getLanguage());
                        }
                        if ("absenteeism".equals(kqStatusList.get(i))){
                            //旷工
                            if(text.length()>0) text+=" ";
                            text += SystemEnv.getHtmlLabelName(20085, user.getLanguage());
                        }
                        if ("leave".equals(kqStatusList.get(i))){
                            //请假
                            Map<String,Object> jsonObject = null;
                            if(leaveInfo.length()>0){
                                jsonObject = JSON.parseObject(leaveInfo);
                                for (Map.Entry<String,Object> entry : jsonObject.entrySet()) {
                                    String newLeaveType = entry.getKey();
                                    String tmpLeaveMins = Util.null2String(entry.getValue());

                                        if (text.length() > 0) text += " ";
                                        //text += kqLeaveRulesComInfo.getLeaveName(newLeaveType)+tmpLeaveMins+SystemEnv.getHtmlLabelName(15049, user.getLanguage());
                                        text += Util.formatMultiLang( kqLeaveRulesComInfo.getLeaveName(newLeaveType),""+user.getLanguage());

                                }
                            }else{
                                    if (text.length() > 0) text += " ";
                                    text += SystemEnv.getHtmlLabelName(670, user.getLanguage());
                            }
                        }
                        if ("evection".equals(kqStatusList.get(i))) {//出差
                            //看是否只显示一次
                            if(text.indexOf(SystemEnv.getHtmlLabelName(20084, user.getLanguage()))==-1) {
                                if (text.length() > 0) text += " ";
                                text += SystemEnv.getHtmlLabelName(20084, user.getLanguage());
                            }
                        }
                        if ("out".equals(kqStatusList.get(i))) {//公出
                            if(text.indexOf(SystemEnv.getHtmlLabelName(24058, user.getLanguage()))==-1) {
                                if (text.length() > 0) text += " ";
                                text += SystemEnv.getHtmlLabelName(24058, user.getLanguage());
                            }
                        }
                        if ("forgotCheck".equals(kqStatusList.get(i))){
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20086, user.getLanguage());
                        }
                        if ("normal".equals(kqStatusList.get(i))){
                            text += " √";
                        }
                    }
                }

                if(text.length()==0) {
                    text = "√";
                }else{
                    flag = "false";//有其他的异常状态,则表示为false，不需要处理直接全部显示即可
                }
                new BaseBean().writeLog("==zj==(日历显示)" + text);
                //qc20230816 获取班次名称
                if (serialid > 0){
                    text = kqFormatUtil.getSerialName(serialid,text,isExport);
                }

                text = tmptext.length()>0?tmptext+" "+text:text;//显示所有的状态
                data.put("text", text);
                datas.put(key, data);

                //add
                tmpdata.put("text", flag);
                tmpdatas.put(key, tmpdata);
                //end
            }
            //全部搞一遍
            if(tmpdatas != null){
//        writeLog(n+">>>tmpdatas="+JSONObject.toJSONString(tmpdatas));
                Map<String,Object> data1 = null;
                for(Map.Entry<String,Object> entry : tmpdatas.entrySet()){
                    String mapKey = Util.null2String(entry.getKey());
                    Map<String,Object> mapValue = (Map<String,Object>)entry.getValue();
                    String flag = Util.null2String(mapValue.get("text"));
                    if("true".equals(flag)){//需要加工的数据
                        data1 = new HashMap<>();
                        data1.put("text", "√");
                        datas.put(mapKey, data1);
                    }
                }
//        writeLog("datas="+JSONObject.toJSONString(datas));
            }
            new BaseBean().writeLog("==zj==(datas)" + JSON.toJSONString(datas));
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(日历数据获取报错)" + JSON.toJSONString(e));
        }
        return datas;
    }

    /**
     * 获取标准工时
     * @return
     */
    public Double getStandardHours(String resourceId,String fromDate,String toDate){
        Double workDays = 0.0;  //标准工时
        String workBegin ="";   //入职日期
        String workEnd ="";   //离职日期
        String fromDateS="";  //查询开始日期
        String toDateS="";  //查询结束日期
        String standardResourceid = new BaseBean().getPropValue("qc20230816","standardResourceid");
        String sql = "";
        RecordSet rs = new RecordSet();
        try {
            //先查询员工入离职状态
             sql = "select companystartdate,enddate from hrmresource where id ='" + resourceId+"'";
             new BaseBean().writeLog("==zj==(标准工时计算--获取员工入离职sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                workBegin = Util.null2String(rs.getString("companystartdate"));
                workEnd = Util.null2String(rs.getString("enddate"));
            }
            new BaseBean().writeLog("==zj==(workBegin)" + JSON.toJSONString(workBegin));
            new BaseBean().writeLog("==zj==(workEnd)" + JSON.toJSONString(workEnd));
            //初始查询条件
            fromDateS = fromDate;
            toDateS = toDate;
            //判断查询日期是否再入职之后
            if (!"".equals(workBegin)){
                if (workBegin.compareTo(fromDate) >= 0){
                    fromDateS = workBegin;
                }
            }

            //判断结束日期是否在离职之后
            if (!"".equals(workEnd)){
                if (workEnd.compareTo(toDate) < 0){
                    toDateS = workEnd;
                }
            }
            sql = "select sum(workdays)as workdays from KQ_FORMAT_TOTAL where resourceid = "+standardResourceid + " and kqdate between '"+fromDateS+"' and '"+toDateS+"'";
            new BaseBean().writeLog("==zj==(标准工时sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                workDays = rs.getDouble("workdays");
            }
            if (workDays > 0){
                workDays = workDays * 8.0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return workDays;
    }

    /**
     * 计算加班工时
     * @param resourceid
     * @param flowData
     * @param fromDate
     * @param toDate
     * @param workHours
     * @return
     */
    public String getOverHours(String resourceid,Map<String, Object> flowData,String fromDate,String toDate,String workHours,String absenteeismdaysHours){
        Double overHours =0.0;
        Double leaveHours = 0.0;    //请假汇总时长
        Double standardHours = 0.0; //标准工作时长
        Double workHoursD = 0.0;    //工作时长
        Double absenteeismdaysHoursD = 0.0; //旷工工作时长

        //获取日期范围所有请假时长
        leaveHours = getleaveCount(flowData,resourceid);
        if (leaveHours > 0){
           leaveHours =  leaveHours * 8;
        }
        //获取标准工时
        standardHours = getStandardHours(resourceid,fromDate,toDate);
        //获取工作时长
        if (!"0".equals(workHours) && !"".equals(workHours)){
            workHoursD  = Double.parseDouble(workHours) / 60.0;
        }
        //获取旷工工作时长
        if (!"0".equals(absenteeismdaysHours) && !"".equals(absenteeismdaysHours)){
            absenteeismdaysHoursD  = Double.parseDouble(absenteeismdaysHours)  * 8;
        }
        //加班工时 = 工作时长+请假时长+旷工天数*8-标准工时
        new BaseBean().writeLog("==zj==(查询日期)" + resourceid + "    |     " + fromDate + " - "+toDate);
        new BaseBean().writeLog("==zj==(加班工时)"+ workHoursD + "   +  " + leaveHours +"  +  "+absenteeismdaysHoursD+ "   -   " + standardHours);
        overHours = workHoursD+leaveHours+absenteeismdaysHoursD - standardHours;
       /* if (overHours < 0){
            overHours = 0.0;
        }*/
       String fieldValue = KQDurationCalculatorUtil.getDurationRound(("" + overHours));
       new BaseBean().writeLog("==zj==(fieldValue)" + fieldValue);
        return fieldValue;

    }

    /**
     * 获取所有的请假时长
     * @param flowData
     * @return
     */
    public Double getleaveCount(Map<String, Object> flowData, String id) {
        double leaveTotal = 0.00;
        KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
        List<Map<String, Object>> allLeaveRules = kqLeaveRulesBiz.getAllLeaveRules();
        Map<String, Object> leaveRule = null;
        for (int i = 0; allLeaveRules != null && i < allLeaveRules.size(); i++) {
            leaveRule = (Map<String, Object>) allLeaveRules.get(i);
            new BaseBean().writeLog("==zj==(计算扣减--leaveRule)" + JSON.toJSONString(leaveRule));
            String flowType = Util.null2String("leaveType_" + leaveRule.get("id"));
            String leaveData = Util.null2String(flowData.get(id + "|" + flowType));
            String flowLeaveBackType = Util.null2String("leavebackType_" + leaveRule.get("id"));
            String leavebackData = Util.null2s(Util.null2String(flowData.get(id + "|" + flowLeaveBackType)), "0.0");
            String b_flowLeaveData = "";
            String flowLeaveData = "";
            try {
                //以防止出现精度问题
                if (leaveData.length() == 0) {
                    leaveData = "0.0";
                }
                if (leavebackData.length() == 0) {
                    leavebackData = "0.0";
                }
                BigDecimal b_leaveData = new BigDecimal(leaveData);
                BigDecimal b_leavebackData = new BigDecimal(leavebackData);
                b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
                if (Util.getDoubleValue(b_flowLeaveData, -1) < 0) {
                    b_flowLeaveData = "0.0";
                }
            } catch (Exception e) {
                new BaseBean().writeLog("==zj==(调休时长报错)" +e);
            }

            //考虑下冻结的数据
            if (b_flowLeaveData.length() > 0) {
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
            } else {
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData, 0.0) - Util.getDoubleValue(leavebackData, 0.0)));
            }

            //如果给假期属于统计范围，统计到事假总计
            try{
                        leaveTotal += Double.parseDouble(flowLeaveData);
            }catch (Exception e){
                new BaseBean().writeLog("==zj==(计算扣减累加报错)" + e);
            }
        }
        return leaveTotal;
    }

    /**
     * 这里对标准加班时长各进行显示折算
     * @param fieldName 不调休加班类型
     * @param userId     人员id
     * @return
     */
    public String overtimeObversion(String userId,String fieldName,String fromDate,String toDate){
        String sql="";
        Double overTime = 0.0;
        Double overTimeTotal = 0.0;
        Double belatemins = 0.0;
        Double leaveearlymins = 0.0;
        Double abnormal = 0.0;

        String overDate = "";//加班日期
        RecordSet rs = new RecordSet();
        RecordSet rw = new RecordSet();
        ArrayList<Double> overTimeList = new ArrayList<>(); //保存时间段内，每天的加班时长
        ArrayList<String> overDateList = new ArrayList<>(); //保存时间段内，每天的加班日期
        //工作日规则：加班时长＜2.5，此处显示为0 加班时长≥2.5，按照显示实际小时数
        if ("workingDayOvertime_nonleave".equals(fieldName)){
            sql = "select belongdate,resourceid,sum(duration_min) as duration_min ,changetype,paidleaveenable from KQ_FLOW_OVERTIME group by belongdate,resourceid,duration_min,changetype,paidleaveenable"+
                    " having resourceid ="+userId+" and belongdate between '"+fromDate+"' and '"+toDate+"' and changetype=2 and paidleaveenable=0"+
                    " order by belongdate asc";
            new BaseBean().writeLog("==zj==(工作日sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            while (rs.next()){
                String durationHour = KQDurationCalculatorUtil.getDurationRound(rs.getDouble("duration_min") / 60.0 + "");
                overTime =Double.parseDouble(durationHour);
                overDate = Util.null2String(rs.getString("belongdate"));
                String kqsql = "select * from kq_format_total where resourceid="+userId+" and kqdate='"+overDate+"'";
                new BaseBean().writeLog("==zj==(查询对应工作加班日sql)" + JSON.toJSONString(sql));
                rw.executeQuery(kqsql);
                if (rw.next()){
                    belatemins = rw.getDouble("belatemins");
                    leaveearlymins = rw.getDouble("leaveearlymins");
                    //异常时长汇总
                    abnormal = belatemins+leaveearlymins;
                    if (abnormal > 30.0){
                        overTime = 0.0;
                    }else if (overTime < 2.0){
                        overTime = 0.0;
                    }else if (overTime >= 2.0 && overTime <2.5){
                        overTime = 2.0;
                    }else if (overTime >=2.5){
                        overTime = 2.5;
                    }
                }
                overTimeList.add(overTime);
            }


            //将每天工作日加班时长累加
            for (int i = 0; i < overTimeList.size(); i++) {
                overTimeTotal += overTimeList.get(i);
            }
            return overTimeTotal+"";
        }

        //节假日,休息日规则：加班时长＜4h，此处显示为0， 4h≤加班时长＜8h，此处显示4，加班时长>=8，此处显示8
        if("holidayOvertime_nonleave".equals(fieldName) || "restDayOvertime_nonleave".equals(fieldName)){
            int changeType = -1;    //加班类型 1:节假日  3:休息日
            if ("holidayOvertime_nonleave".equals(fieldName)){
                changeType = 1;
            }else {
                changeType = 3;
            }

            sql = "select * from KQ_FLOW_OVERTIME where resourceid='"+userId+"' and belongdate between '"+fromDate+"' and '"+toDate+"' and changetype="+changeType+" and paidleaveenable=0";
            new BaseBean().writeLog("==zj==(节假日sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            while (rs.next()){
                String durationHour = KQDurationCalculatorUtil.getDurationRound(rs.getDouble("duration_min") / 60.0 + "");
                overTime =Double.parseDouble(durationHour);

                if (overTime >=0 && overTime < 3.5){
                    overTime = 0.0;
                }
                if (overTime >= 3.5 && overTime < 4){
                    overTime = 3.5;
                }
                if (overTime >= 4 && overTime < 7.5){
                    overTime = 4.0;
                }
                if (overTime >= 7.5 && overTime < 8){
                    overTime = 7.5;
                }
                if (overTime >= 8){
                    overTime = 8.0;
                }
                overTimeList.add(overTime);
            }
            //将每天加班时长累加
            for (int i = 0; i < overTimeList.size(); i++) {
                overTimeTotal += overTimeList.get(i);
            }
            return overTimeTotal+"";
        }

        return "0.0";

    }

    /**
     * 考勤加班汇总显示折算
     * @param holidayOvertime
     * @param workdayOvertime
     * @param restdayOvertime
     * @return
     */
    public String overtimeTotal(String holidayOvertime,String workdayOvertime,String restdayOvertime){
        Double holidayHours = 0.0;
        Double workdayHours = 0.0;
        Double restdayHours = 0.0;

        if (!"0.0".equals(holidayOvertime)){
            holidayHours = Double.parseDouble(holidayOvertime);
        }
        if (!"0.0".equals(workdayOvertime)){
            workdayHours = Double.parseDouble(workdayOvertime);
        }
        if (!"0.0".equals(restdayOvertime)){
            restdayHours = Double.parseDouble(restdayOvertime);
        }

        return holidayHours + workdayHours + restdayHours+"";
    }

    /**
     * 这里获取当天类型
     * @param resourceId
     * @param date
     * @return
     */
    public  int getChangeType(String resourceId, String date) {
        int changeType = -1;
        //1-节假日、2-工作日、3-休息日
        /*获取考勤组的ID，因为考勤组有有效期，所以需要传入日期*/
        KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
        String groupId = kqGroupMemberComInfo.getKQGroupId(resourceId, date);

        /*该人员不存在于任意一个考勤组中，请为其设置考勤组*/
        if(groupId.equals("")){
            new BaseBean().writeLog("该人员不存在于任意一个考勤组中，请为其设置考勤组。resourceId=" + resourceId + ",date=" + date);
        }

        changeType = KQHolidaySetBiz.getChangeType(groupId, date);
        if (changeType != 1 && changeType != 2 && changeType != 3) {
            KQWorkTime kqWorkTime = new KQWorkTime();
            changeType = kqWorkTime.isWorkDay(resourceId, date) ? 2 : 3;
        }
        return changeType;
    }

}
