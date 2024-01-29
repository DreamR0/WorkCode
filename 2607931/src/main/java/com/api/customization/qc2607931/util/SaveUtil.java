package com.api.customization.qc2607931.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.*;
import weaver.common.DateUtil;
import weaver.conn.BatchRecordSet;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.Reminder.KQAutoCardTask;
import weaver.systeminfo.SystemEnv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SaveUtil {

    /**
     * 调班流程 保存排班数据
     * @param userIds
     * @param fromDate
     * @param toDate
     * @param serialId
     * @param groupId
     */
    public void saveShiftschedule(String userIds, String fromDate, String toDate, String serialId, String groupId){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        BatchRecordSet bRs = new BatchRecordSet();
        RecordSet rs = new RecordSet();
        List<List<Object>> paramInsert = new ArrayList<>();
        List<Object> params = null;
        String sql = "";
        try {
            KQShiftScheduleComInfo kqShiftScheduleComInfo = new KQShiftScheduleComInfo();
            List sqlParams = new ArrayList();
            Object[] objects= DBUtil.transListIn(userIds,sqlParams);
            sql = "update kq_shiftschedule set isdelete = 1 where resourceid in("+objects[0]+") and kqdate>=? and kqdate<=? ";
            rs.executeUpdate(sql,sqlParams,fromDate,toDate);
            List<String> lsUserId = Util.splitString2List(userIds,",");
            CheckUtil checkUtil = new CheckUtil();
            for(String userId : lsUserId) {
                boolean isEnd = false;
                Calendar cal = DateUtil.getCalendar();
                for (String date = fromDate; !isEnd; ) {
                    if (date.equals(toDate)) isEnd = true;
                    params = new ArrayList<Object>();
                    params.add(date);
                    params.add(serialId);
                    params.add(userId);
                    params.add(groupId);
                    paramInsert.add(params);
                    cal.setTime(DateUtil.parseToDate(date));
                    date = DateUtil.getDate(cal.getTime(), 1);
                }
            }

            for (int i = 0; paramInsert != null && i < paramInsert.size(); i++) {
                params = paramInsert.get(i);
                String kqdate = Util.null2String(params.get(0));
                String serialid = Util.null2String(params.get(1));
                String resourceid = Util.null2String(params.get(2));
                //qczj 这里如果是周六周日就跳过排班
                Date kqDate = format.parse(kqdate);
                Boolean isHave = false;
                isHave = checkUtil.dateToWeek(kqDate);
                if (isHave){
                    continue;
                }


                String tmp_groupId = Util.null2String(params.get(3));
                sql = "insert into kq_shiftschedule (kqdate,serialid,resourceid,groupid,isdelete) values(?,?,?,?,0)";
                rs.executeUpdate(sql, kqdate,serialid,resourceid,tmp_groupId);
            }

            kqShiftScheduleComInfo.removeCache();
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(新建人员排班初始化报错)" + e);
        }
    }

    /**
     * 对应排班初始化人员也要添加到考勤组里
     */
    public void saveGroupMember(String resourceId,String groupId){
        RecordSet rs = new RecordSet();
        String sql = "";
        try {
            String[] arrObjIds = Util.splitString(resourceId, ",");
            for(int i=0;i<arrObjIds.length;i++){
                //人员类型,后面的需要替换前面的数据
                //TODO 现在有有效期，一个人是可以存在多个考勤组内的
    //				if(type.equals("1")){
    //					sql = "update kq_groupmember set isdelete=1 where type=1 and typevalue= ? ";
    //					rs.executeUpdate(sql,arrObjIds[i]);
    //				}

                sql = " INSERT INTO kq_groupmember ( groupid ,typevalue ,type ,alllevel ," +
                        " seclevel ,seclevelto ,jobtitlelevelvalue ,jobtitlelevel) " +
                        " VALUES  ( "+groupId+", "+arrObjIds[i]+" , 1 , null,null,null,null,null)";

                new BaseBean().writeLog("==zj==(新建人员添加考勤组sql)" + JSON.toJSONString(sql));
                rs.executeUpdate(sql);

                new KQGroupMemberComInfo().removeCache();
                //格式化考勤
                new KQFormatBiz().formatDateByGroupId(groupId, DateUtil.getCurrentDate());

                KQGroupComInfo kQGroupComInfo = new KQGroupComInfo();
                String auto_checkout = kQGroupComInfo.getAuto_checkout(groupId);
                String auto_checkin = kQGroupComInfo.getAuto_checkin(groupId);
                if("1".equalsIgnoreCase(auto_checkin) || "1".equalsIgnoreCase(auto_checkout)){
                    //如果开启了自动打卡，保存考勤组成员之后需要格式化下缓存
                    KQAutoCardTask kqAutoCardTask = new KQAutoCardTask();
                    kqAutoCardTask.initAutoCardTask(groupId);
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(考勤组初始化报错)" + e);
        }
    }
}
