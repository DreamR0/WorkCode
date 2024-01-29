package com.api.customization.qc2607931.util;

import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQHolidaySetBiz;
import com.engine.kq.biz.KQWorkTime;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class CheckUtil {

    /**获取当前人员查看范围
     *@param user
     * @param reportName
     * @return
     */
    public String selfCheck(User user,String reportName){
        String sql = "";
        String userLevel = "";  //安全级别
        String resourceType = "";   //对象类型
        String sharelevel = "";     //共享级别 0：分部  1：部门
        String subcomIdCustom = "";//分部id
        String deptIdCustom = "";//部门id
        int userId = 0;
        RecordSet rs = new RecordSet();

        String userSubId = Util.null2s(Util.null2String(user.getUserSubCompany1()),"-1");//用户分部id
        String userDepId = Util.null2s(Util.null2String(user.getUserDepartment()),"-1");//用户部门id
        //获取该考勤报表类型的报表类型
        sql = "select * from kq_reportshare where reportName="+reportName+" and resourcetype=1024";
        rs.executeQuery(sql);
        if (rs.next()){
            userLevel = Util.null2String(rs.getString("userLevel"));//安全级别
            resourceType = Util.null2String(rs.getString("resourcetype"));//对象类型
            sharelevel = Util.null2String(rs.getString("sharelevel"));//共享级别

            subcomIdCustom = Util.null2String(rs.getString("subcomIdCustom"));
            deptIdCustom = Util.null2String(rs.getString("deptIdCustom"));

        }
        //获取该人员的安全值
        String seclevel = user.getSeclevel();
        int seclevelD = 0;
        if (!"".equals(seclevel)){
            seclevelD = Integer.parseInt(seclevel);
        }

        if ("1024".equals(resourceType)){
            //对象类型为 安全级别
            if (userLevel.indexOf(",")>-1){
                String levelFrom = userLevel.substring(0,userLevel.indexOf(","));
                String levelTo = "";
                if (!userLevel.endsWith(",")){
                    levelTo = userLevel.substring(userLevel.indexOf(",") + 1);
                }
                //qczj 这里转为int比较
                int levelFromD = Integer.parseInt(levelFrom);
                int levelToD = Integer.parseInt(levelTo);
                new BaseBean().writeLog("==zj==(levelFromD和levelToD)" + levelFromD + " | " + levelToD);
                if (seclevelD >= levelFromD && seclevelD <= levelToD){
                    if ("0".equals(sharelevel) && subcomIdCustom.contains(userSubId)){
                        return "0";
                    }
                    if ("1".equals(sharelevel) && deptIdCustom.contains(userDepId)){
                        return "1";
                    }
                }
            }
        }
        return "-1";

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
     * 当天为周六周日就跳过排班
     * @param shiftdate
     * @return
     */
    public  Boolean dateToWeek(Date shiftdate) {
        int dayForWeek = 0;
        Boolean isHave = false;
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(shiftdate);
            /*format.parse(shiftdate)*/
            if(c.get(Calendar.DAY_OF_WEEK) == 1){
                dayForWeek = 7;
            }else{
                dayForWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
            }
            System.out.println(dayForWeek);
        } catch (Exception e) {
            e.printStackTrace();
        }
        switch (dayForWeek){
            case 6:
                isHave = true;
                break;
            case 7:
                isHave = true;
                break;
        }
        return isHave;
    }

    /**
     * 获取考勤共享报表的安全级别
     * @return
     */
    public String getSaveLevel(String reportName,String resourceType,String reportId){
        RecordSet rs = new RecordSet();
        String saveLevel = "";//获取该考勤报表安全级别
        String sql = "select * from kq_ReportShare where reportName ="+reportName+" and resourceType="+resourceType+" and id="+reportId;
        rs.executeQuery(sql);
        if (rs.next()){
            saveLevel = Util.null2String(rs.getString("userlevel"));
        }
        return saveLevel;

    }
}
