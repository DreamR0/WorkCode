package com.customization.qc2260607;

import com.alibaba.fastjson.JSON;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.wbi.util.Util;
import org.springframework.util.CollectionUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;

import java.text.SimpleDateFormat;
import java.util.*;

public class AccountStateUtil {

    /**
     * 获取当前账号的考勤流程
     * @param resourceid
     * @return
     */
    public List<Map<String,String>> kqFlowGet(String resourceid){
        String nowDate = "";    //年月日
        String nowTime = "";    //时分
        SimpleDateFormat fdsD = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat fdsT = new SimpleDateFormat("HH:mm");

        List<Map<String,String>> resultList = new ArrayList<>();
        try{
            nowDate = fdsD.format(System.currentTimeMillis());   //获取当前年月日
            nowTime = fdsT.format(System.currentTimeMillis());   //获取当前时分
            //获取人员出差流程
           resultList = evectionFlowGet(nowDate,nowTime,resourceid,resultList);
           //获取人员公出流程
            resultList =outFlowGet(nowDate,nowTime,resourceid,resultList);
           //获取人员请假流程
            resultList = leaveFlowGet(nowDate,nowTime,resourceid,resultList);
            //获取人员加班流程
            resultList = overTimeFlowGet(nowDate,nowTime,resourceid,resultList);
            new BaseBean().writeLog("==zj==(获取当前时间范围内人员考勤流程)" + JSON.toJSONString(resultList));
            //按照归档时间进行排序
            new BaseBean().writeLog("==zj==(排序前resultList)"  + JSON.toJSONString(resultList));
            resultList = listSort(resultList);
            new BaseBean().writeLog("==zj==(排序后resultList)" + JSON.toJSONString(resultList));

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(获取考勤流程错误信息)" +e);
        }
        return resultList;
    }

    /**
     * 获取出差流程结果
     * @param nowDate
     * @param nowTime
     * @param resourceid
     * @return
     */
    public List<Map<String,String>> evectionFlowGet(String nowDate,String nowTime,String resourceid,List<Map<String,String>> resultList){
        RecordSet rs = new RecordSet();

        try {
            //获取人员出差流程
            String evection_table_name = KqSplitFlowTypeEnum.EVECTION.getTablename();   //获取出差流程拆分表
            String evectionSelect = "select * from "+evection_table_name+" a left join workflow_requestbase b on a.requestid = b.requestid " ;
            String evectionWhere = "where a.status <> 1 and '"+nowDate+"' between a.fromdate and a.todate and '"+nowTime+"' between a.fromtime and a.totime and a.resourceid = "+resourceid;
            String evectionSql = evectionSelect + evectionWhere;
            new BaseBean().writeLog("==zj==(出差归档流程获取sql)" + evectionSql);
            rs.executeQuery(evectionSql);
            while (rs.next()){
                Map<String,String> result = new HashMap<>();
               String lastDateTime = Util.null2String(rs.getString("lastoperatedate")) + " " + Util.null2String(rs.getString("lastoperatetime"));   //获取最后操作时间
                String fromTime = Util.null2String(rs.getString("fromtime"));   //获取拆分流程开始时间
                String toTime = Util.null2String(rs.getString("toTime"));   //获取拆分流程开始时间
                String kqStatus = "";       //考勤状态

                //获取考勤状态
                kqStatus = decideTime(fromTime, toTime, kqStatus);

                String kqType = "出差";
                result.put("人员",resourceid);
                result.put("考勤类型",kqType);
                result.put("归档时间",lastDateTime);
                result.put("考勤状态",kqStatus);
                resultList.add(result);
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取出差流程结果错误信息)" + e);
        }
        return resultList;
    }

    /**
     * 获取公出流程结果
     * @param nowDate
     * @param nowTime
     * @param resourceid
     * @param resultList
     * @return
     */
    public List<Map<String,String>> outFlowGet(String nowDate,String nowTime,String resourceid,List<Map<String,String>> resultList){
        RecordSet rs = new RecordSet();
        try {
            //获取人员公出流程
            String out_table_name = KqSplitFlowTypeEnum.OUT.getTablename();   //获取出差流程拆分表
            String outSelect = "select * from "+out_table_name+" a left join workflow_requestbase b on a.requestid = b.requestid " ;
            String outWhere = "where a.status <> 1 and '"+nowDate+"' between a.fromdate and a.todate and '"+nowTime+"' between a.fromtime and a.totime and a.resourceid = "+resourceid;
            String outSql = outSelect + outWhere;
            new BaseBean().writeLog("==zj==(公出归档流程获取sql)" + outSql);
            rs.executeQuery(outSql);
            while (rs.next()){
                Map<String,String> result = new HashMap<>();
                String lastDateTime = Util.null2String(rs.getString("lastoperatedate")) + " " + Util.null2String(rs.getString("lastoperatetime"));

                String fromTime = Util.null2String(rs.getString("fromtime"));   //获取拆分流程开始时间
                String toTime = Util.null2String(rs.getString("toTime"));   //获取拆分流程开始时间
                String kqStatus = "";       //考勤状态

                //获取考勤状态
                kqStatus = decideTime(fromTime, toTime, kqStatus);

                String kqType = "公出";
                result.put("人员",resourceid);
                result.put("考勤类型",kqType);
                result.put("归档时间",lastDateTime);
                result.put("考勤状态",kqStatus);
                resultList.add(result);
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取公出流程结果错误信息)" + e);
        }
        return resultList;
    }

    /**
     * 获取请假流程结果
     * @param nowDate
     * @param nowTime
     * @param resourceid
     * @param resultList
     * @return
     */
    public List<Map<String,String>> leaveFlowGet(String nowDate,String nowTime,String resourceid,List<Map<String,String>> resultList){
        RecordSet rs = new RecordSet();

        try {
            //获取人员请假流程
            String leave_table_name = KqSplitFlowTypeEnum.LEAVE.getTablename();   //获取出差流程拆分表
            String leaveSelect = "select * from "+leave_table_name+" a left join workflow_requestbase b on a.requestid = b.requestid " ;
            String leaveWhere = "where a.status <> 1 and '"+nowDate+"' between a.fromdate and a.todate and '"+nowTime+"' between a.fromtime and a.totime and a.resourceid = "+resourceid+" and leavebackrequestid is null";
            String leaveSql = leaveSelect + leaveWhere;
            new BaseBean().writeLog("==zj==(请假归档流程获取sql)" + leaveSql);
            rs.executeQuery(leaveSql);
            while (rs.next()){
                Map<String,String> result = new HashMap<>();
                String lastDateTime = Util.null2String(rs.getString("lastoperatedate")) + " " + Util.null2String(rs.getString("lastoperatetime"));

                String fromTime = Util.null2String(rs.getString("fromtime"));   //获取拆分流程开始时间
                String toTime = Util.null2String(rs.getString("toTime"));   //获取拆分流程开始时间
                String kqStatus = "";       //考勤状态

                //获取考勤状态
                kqStatus = decideTime(fromTime, toTime, kqStatus);
                String kqType = Util.null2String(rs.getString("newleavetype"));
                kqType = leaveTypeGet(kqType);
                result.put("人员",resourceid);
                result.put("考勤类型",kqType);
                result.put("归档时间",lastDateTime);
                result.put("考勤状态",kqStatus);
                resultList.add(result);
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取请假流程结果错误信息)" + e);
        }
        return resultList;
    }

    /**
     * 获取加班流程结果
     * @param nowDate
     * @param nowTime
     * @param resourceid
     * @param resultList
     * @return
     */
    public List<Map<String,String>> overTimeFlowGet(String nowDate,String nowTime,String resourceid,List<Map<String,String>> resultList){
        RecordSet rs = new RecordSet();
        try {
            //获取人员加班流程
            String overTime_table_name = KqSplitFlowTypeEnum.OVERTIME.getTablename();   //获取加班流程拆分表
            String overTimeSelect = "select * from "+overTime_table_name+" a left join workflow_requestbase b on a.requestid = b.requestid " ;
            String overTimeWhere = "where a.status <> 1 and '"+nowDate+"' between a.fromdate and a.todate and '"+nowTime+"' between a.fromtime and a.totime and a.resourceid = "+resourceid;
            String overTimeSql = overTimeSelect + overTimeWhere;
            new BaseBean().writeLog("==zj==(加班归档流程获取sql)" + overTimeSql);
            rs.executeQuery(overTimeSql);
            while (rs.next()){
                Map<String,String> result = new HashMap<>();
                String lastDateTime = Util.null2String(rs.getString("lastoperatedate")) + " " + Util.null2String(rs.getString("lastoperatetime"));

                String fromTime = Util.null2String(rs.getString("fromtime"));   //获取拆分流程开始时间
                String toTime = Util.null2String(rs.getString("toTime"));   //获取拆分流程开始时间
                String kqStatus = "";       //考勤状态

                //获取考勤状态
                kqStatus = decideTime(fromTime, toTime, kqStatus);

                String kqType = "加班";
                result.put("人员",resourceid);
                result.put("考勤类型",kqType);
                result.put("归档时间",lastDateTime);
                result.put("考勤状态",kqStatus);
                resultList.add(result);
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取加班流程结果错误信息)" + e);
        }
        return resultList;
    }

    /**
     * 将list根据归档时间（lastDateTime）排序
     * @param resultList
     * @return
     */
    public List<Map<String,String>> listSort(List<Map<String,String>> resultList){
        new BaseBean().writeLog("==zj==(排序前)" + JSON.toJSONString(resultList));
        if (!CollectionUtils.isEmpty(resultList)){
            Collections.sort(resultList, new Comparator<Map<String, String>>() {
                @Override
                public int compare(Map<String, String> o1, Map<String, String> o2) {
                    return o1.get("归档时间").compareTo(o2.get("归档时间"));
                }
            });
        }
        return resultList;
    }

    /**
     * 考勤状态格式化
     * @param fromTime
     * @param toTime
     * @param kqStatus
     * @return
     */
    public String decideTime(String fromTime,String toTime,String kqStatus){
        new BaseBean().writeLog("==zj==(考勤状态格式化)");
        Calendar instance = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:ss");
        int hour = 0;
        int apm = 0;
        try{
            //这里判断开始时间是上午还是下午
            instance.clear();
            Date fromTimeD = sdf.parse(fromTime);
            instance.setTime(fromTimeD);
             hour = instance.get(Calendar.HOUR);
             apm = instance.get(Calendar.AM_PM);
            if (apm == 0){
                //如果为上午
                kqStatus +="(上午"+fromTime;
            }else{
                //如果为下午
                kqStatus +="(下午"+fromTime;
            }

            //这里判断结束时间是上午还是下午
            instance.clear();   //初始化清空
            Date toTimeD = sdf.parse(toTime);
            instance.setTime(toTimeD);
            hour = instance.get(Calendar.HOUR);
            apm = instance.get(Calendar.AM_PM);
            if (apm == 0){
                //如果为上午
                kqStatus += "-上午"+toTime+")";
            }else {
                //如果为下午
                kqStatus += "-下午"+toTime+")";
            }


        }catch (Exception e){
            new BaseBean().writeLog("==zj==(考勤格式化报错信息)" + e);
        }
        new BaseBean().writeLog("==zj==(拼接考勤状态)" + kqStatus);
        return kqStatus;

    }

    /**
     * 获取假期类型名字
     * @param kqType
     * @return
     */
    public String leaveTypeGet(String kqType){
        String leaveName = "";
        RecordSet rs = new RecordSet();
        String leaveTypeSql = "select * from KQ_LeaveRules where id=" + kqType;
        rs.executeQuery(leaveTypeSql);
        if (rs.next()){
            leaveName = Util.null2String(rs.getString("leaveName"));
        }

        return leaveName;
    }


}
