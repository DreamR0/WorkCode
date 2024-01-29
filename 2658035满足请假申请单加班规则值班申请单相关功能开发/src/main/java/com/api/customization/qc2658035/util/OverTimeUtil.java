package com.api.customization.qc2658035.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQHolidaySetBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.weaver.formmodel.util.DateHelper;
import weaver.conn.RecordSet;
import weaver.formmode.data.ModeDataIdUpdate;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.ArrayList;

public class OverTimeUtil {
    String userLevelTable = new BaseBean().getPropValue("qc2658035","userLevelTable");//人员级别表
    String overLeaveRuleTable = new BaseBean().getPropValue("qc2658035","overLeaveRuleTable");//级别调休表
    String nonLeaveOverTable = new BaseBean().getPropValue("qc2658035","nonLeaveOverTable");//未调休加班时长
    String normalWorkFlowid = new BaseBean().getPropValue("qc2658035","normalWorkFlowid");//正常


    /**
     * 获取当天的加班数据需要调休的时长
     * @param resourceId 人员id
     * @param realSplitDate 加班日期
     */
    public Double getOverTimeLeaveMins( String resourceId,String realSplitDate,int overTimeMins,String requestid){
        ArrayList<Double> result = new ArrayList<>();
        String sql = "";
        String overTimeDate = "";   //加班日期
        String overTimeMonth = "";    //加班年月
        Double timeTotal = 0.0;//当月未调休汇总时长
        Double timeToTalMins = 0.0;
        Double nonOverTimeHours = 0.0;//未调休加班时长
        Double overTimeLeavesMins = 0.0;//调休加班时长

        RecordSet rs = new RecordSet();


        try {
            if (realSplitDate.length()>=7){
                overTimeMonth = realSplitDate.substring(0,7);  //获取加班月份
            }
            isDelete(resourceId, realSplitDate, requestid);
            sql = "select isNull(SUM(jbsc),0) as timetotal from "+nonLeaveOverTable+" where xm='"+resourceId+"' and jbyf='"+overTimeMonth+"'";
            new BaseBean().writeLog("==zj==(sql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                timeTotal = rs.getDouble("timetotal");
            }
            new BaseBean().writeLog("==zj==(当天流程时长)"+"人员id:"+resourceId + "--当月累计未调休时长:"+timeToTalMins+" -- 当天加班时长 " + overTimeMins );
            timeToTalMins = timeTotal * 60.0;
            //如果当月累计时长>36小时，全部时长转为调休时长
            if (timeToTalMins > 2160){
                return (double)overTimeMins;
            }else if (timeToTalMins + overTimeMins <= 2160){
                //将非调休时长转到建模表里
                nonOverTimeHours =(double) overTimeMins/60;
                setNonOverTime(resourceId,realSplitDate,nonOverTimeHours,overTimeMonth,requestid);
                return 0.0;
            }else if ((timeToTalMins + overTimeMins) > 2160){
                //如果当前加班时长+当月非调休加班时长>36h,把超出部分转为调休
                overTimeLeavesMins = (timeToTalMins+overTimeMins) - 2160;//超出部分转为调休时长
                nonOverTimeHours =(overTimeMins - overTimeLeavesMins)/60;//剩下部分转为非调休时长，并存入建模表
                //剩下部分转为非调休时长，并存入建模表
                if (nonOverTimeHours > 0){
                    setNonOverTime(resourceId,realSplitDate,nonOverTimeHours,overTimeMonth,requestid);
                }
            }
        } catch (Exception e) {
           new BaseBean().writeLog("==zj==(加班时长处理报错)" + JSON.toJSONString(e));
        }

        return overTimeLeavesMins;
    }

    /**
     * 将未调休时长保存到建模表
     */
    public void setNonOverTime(String resourceId,String realSplitDate,Double nonOverTimeHours,String overTimeMonth,String requestid){
        String sql = "";
        RecordSet rs = new RecordSet();
        User user = new User(1);
            int modeDataId = getModeDataId(nonLeaveOverTable, user);
            new BaseBean().writeLog("==zj==(modeDataId)" + JSON.toJSONString(modeDataId));
            if (modeDataId > 0){
                sql = "update "+nonLeaveOverTable+" set xm='"+resourceId+"',jbrq='"+realSplitDate+"',jbyf='"+overTimeMonth+"',jbsc="+nonOverTimeHours+",lcid='"+requestid+"' where id='"+modeDataId+"'";
                new BaseBean().writeLog("==zj==(未调休sql)" + JSON.toJSONString(sql));
                rs.executeUpdate(sql);
            }


    }

    /**
     * 获取该流程所属工作流
     * @param requestId
     * @return
     */
    public String getWorkFlowId(String requestId){
        String sql = "";
        String workFlowId ="";
        RecordSet rs = new RecordSet();

        sql = "select workflowid from workflow_requestbase where requestid="+requestId;
        rs.executeQuery(sql);
        if (rs.next()){
            workFlowId = Util.null2String(rs.getString("workflowid"));
        }
        return workFlowId;
    }


    /**
     * 构建建模表数据
     * @param tableName
     * @param user
     * @return
     */
    public int getModeDataId(String tableName, User user){
        int modeId = getModeId(tableName);
        if (modeId==-1){
            return -1;
        }
        int id = ModeDataIdUpdate.getInstance().getModeDataNewIdByUUID(tableName, modeId, user.getUID(), (user.getLogintype()).equals("1") ? 0 : 1,
                DateHelper.getCurrentDate(), DateHelper.getCurrentTime());
        ModeRightInfo ModeRightInfo = new ModeRightInfo();
        ModeRightInfo.setNewRight(true);
        ModeRightInfo.editModeDataShare(user.getUID(),Integer.parseInt(modeId+""),id);//新建的时候添加共享
        ModeRightInfo.addDocShare(user.getUID(),Integer.parseInt(modeId+""),id);//新建的时候添加文档共享
        return id;
    }

    /**
     * 获取建模id
     * @param tablename
     * @return
     */
    public int getModeId(String tablename){
        RecordSet rs = new RecordSet();
        int modeid = -1;
        String sql = "select id as modeid,formid from modeinfo where FORMID = (select id from workflow_bill where TABLENAME = ?)";
        rs.executeQuery(sql,tablename);
        if (rs.next()){
            modeid = rs.getInt("modeid");
        }
        return modeid;
    }

    /**
     * 这里判断当天值班时长
     * @param intersection_from 交集开始时间
     * @param intersection_to 交集结束时间
     * @return
     */
    public int getdutyOverTimes(int intersection_from, int intersection_to,String realSplitDate,String resourceId){
        KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();
        int halfpoint = kqTimesArrayComInfo.getArrayindexByTimes("12:00"); //设置一个半天分界点
        int halfpointEnd =  kqTimesArrayComInfo.getArrayindexByTimes("12:45");
        int dutyOverTime = 0;

        //有可能出现当天没有交集时长
        if (intersection_from == 0 && intersection_to == 0){
            dutyOverTime = 480;
            return dutyOverTime;
        }
        //如果结束时间在中午12点之前或者开始时间在12点之后
        if (intersection_to <= halfpoint || intersection_from >= halfpoint) {
            dutyOverTime = 240;
        }
         if ( intersection_from < halfpoint && intersection_to >= halfpointEnd ){
            //如果开始时间在中午12点之前，结束时间在12点45之后
            dutyOverTime = 0;
        }

        return dutyOverTime;


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


    /**
     * 删除相同建模数据
     * @param resourceId
     * @param realSplitDate
     * @param requestid
     * @return
     */
    public Boolean isDelete(String resourceId,String realSplitDate,String requestid){
        Boolean isDelete = false;
        RecordSet rsRepeat = new RecordSet();
        String sql = "delete  from "+nonLeaveOverTable+" where xm="+resourceId+" and jbrq='"+realSplitDate+"' and lcid="+requestid;
        new BaseBean().writeLog("==zj==(删除重复数据)" + JSON.toJSONString(sql));
        isDelete =  rsRepeat.executeUpdate(sql);

        return isDelete;

    }
}
