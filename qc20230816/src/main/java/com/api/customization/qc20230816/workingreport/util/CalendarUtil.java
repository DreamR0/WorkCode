package com.api.customization.qc20230816.workingreport.util;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc20230816.util.KqFormatUtil;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import com.engine.kq.util.KQDurationCalculatorUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarUtil {
    /**
     *日历显示
     * @param resourceId
     * @param fromDate
     * @param toDate
     * @return
     */
    public Map<String,Object> getDetialDatas(String resourceId, String fromDate, String toDate, User user, Boolean isExport){
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
                    " leaveMins,leaveInfo,evectionMins,outMins,serialid,kqstatusam,kqstatuspm,workhours,holidaymins" +
                    " from kq_format_detail " +
                    " where resourceid = ? and kqdate>=? and kqdate<=? "+
                    " order by resourceid, kqdate, serialnumber  ";
            rs.executeQuery(sql,resourceId, fromDate,toDate);
            while (rs.next()) {
                String key = rs.getString("resourceid") + "|" + rs.getString("kqdate");
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
                //qc20230816 获取当天节假日加班工时
                String holidayMins = rs.getString("holidaymins");
                //qc20230816 获取上班工时
                int workHours = Util.getIntValue(rs.getString("workhours"),0);
                //qc20230816 获取考勤日期
                String kqdate = rs.getString("kqdate");
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
                    //显示休息
                    if(text.length()>0) text +=" ";
                    //如果有加班工时就显示加班工时
                   /* Double overTime = getDayOverTime(resourceId,kqdate);*/
                        Double overTime = getDayOverTime(resourceId,kqdate);
                        new BaseBean().writeLog("==zj==(工时报表非工作日加班)" +resourceId + " | " + kqdate + "  | "+overTime);
                        text += overTime;

                } else {
                    //计算工时
                    Double dayHours = 0.0;
                    if (workHours > 0){
                        dayHours = (Double) (workHours/60.0);
                    }
                    //这里再加上当日的加班工时
                    dayHours += getDayOverTime(resourceId,kqdate);
                    text += ""+dayHours;
                }

                if(text.length()==0) {
                    text = "√";
                }else{
                    flag = "false";//有其他的异常状态,则表示为false，不需要处理直接全部显示即可
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
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(日历数据获取报错)" + JSON.toJSONString(e));
        }
        return datas;
    }

    /**
     * 这里获取指定日期的加班工时
     * @param resourceId
     * @param kqdate
     * @return
     */
    public Double getDayOverTime(String resourceId,String kqdate){
        Double dayOverTime = 0.0;
        String sql = "";
        Double belatemins = 0.0;
        Double leaveearlymins = 0.0;
        Double abnormal = 0.0;
        RecordSet rs = new RecordSet();
        RecordSet rw = new RecordSet();

        try {
            sql = "select * from KQ_FLOW_OVERTIME where resourceid='"+resourceId+"' and belongdate='"+kqdate+"'";
            new BaseBean().writeLog("==zj==(工时报表非工作日加班sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            while (rs.next()){
                String durationHour = KQDurationCalculatorUtil.getDurationRound(rs.getDouble("duration_min") / 60.0 + "");
                double overtime = Double.parseDouble(durationHour); //获取加班小时
                String changetype = Util.null2String(rs.getString("changetype"));// 获取加班类型 1：节假 2：工作 3：休息
                String kqsql = "select * from kq_format_total where resourceid="+resourceId+" and kqdate='"+kqdate+"'";
                rw.executeQuery(kqsql);
                if (rw.next()){
                    belatemins = rw.getDouble("belatemins");
                    leaveearlymins = rw.getDouble("leaveearlymins");
                    //异常时长汇总
                    abnormal = belatemins+leaveearlymins;
                    if ("2".equals(changetype)){
                        //工作日加班折算  加班时长＜2.5，显示为0 加班时长≥2.5，按照显示实际小时数
                        if (abnormal > 30.0){
                            overtime = 0.0;
                        }else if (overtime < 2.0){
                            overtime = 0.0;
                        }else if (overtime >= 2.0 && overtime <2.5){
                            overtime = 2.0;
                        }else if (overtime >=2.5){
                            overtime = 2.5;
                        }
                    }
                    new BaseBean().writeLog("==zj==(加班工时)" + resourceId + " | " + kqdate + "  |  " + overtime);
                }


                if ("1".equals(changetype)  || "3".equals(changetype)){
                    //节假日、休息日加班折算 加班时长＜4h，此处显示为0， 4h≤加班时长＜8h，此处显示4，
                    //加班时长>=8，此处显示8
                    if (overtime >=0 && overtime < 3.5){
                        overtime = 0.0;
                    }
                    if (overtime >= 3.5 && overtime < 4){
                        overtime = 3.5;
                    }
                    if (overtime >= 4 && overtime < 7.5){
                        overtime = 4.0;
                    }
                    if (overtime >= 7.5 && overtime < 8){
                        overtime = 7.5;
                    }
                    if (overtime >= 8){
                        overtime = 8.0;
                    }
                }
                dayOverTime += overtime;
            }
        } catch (NumberFormatException e) {
            new BaseBean().writeLog("==zj==(工时报表加班时长折算错误)" + e);
        }

        return dayOverTime;
    }

}
