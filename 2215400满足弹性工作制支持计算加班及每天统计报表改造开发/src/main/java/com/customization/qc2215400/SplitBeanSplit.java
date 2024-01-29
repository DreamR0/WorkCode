package com.customization.qc2215400;

import com.api.integration.Base;
import com.engine.kq.wfset.bean.SplitBean;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SplitBeanSplit {
    /**
     * 因为考虑到加班跨天情况所以在这里做拆分
     */
    public List<SplitBean> split(SplitBean splitBean){
        //数据初始化
        String requestId = splitBean.getRequestId();    //对应流程的requestId
        String resourceId = splitBean.getResourceId();  //对应人员id
        String groupid = splitBean.getGroupid();        //获取考勤组id
        String fromdateDB = "";  //流程上加班开始日期
        String fromtimedb = "";  //流程上加班开始时间
        String todatedb = "";      //流程上加班结束日期
        String totimedb = "";      //流程上加班结束时间
        String fromdate = "";      //实际有效的加班开始日期
        String fromtime = "";      //实际有效的加班开始时间
        String todate = "";          //实际有效的加班结束日期
        String totime = "";          //实际有效的加班结束时间
        String belongDate = "";     //归属日期


        List<SplitBean> splitBeans = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        int count = dayCount(splitBean.getFromdatedb(),splitBean.getTodatedb());
        new BaseBean().writeLog("==zj==(count)"+count);
        try{
            //如果没有跨天
           if (count == 0){
               splitBean.setBelongDate(splitBean.getToDate());      //归属日期就为当前实际开始日期
               splitBeans.add(splitBean);
           }else {
               //如果有跨天
               for (int i = 0; i <= count; i++) {
                   //开始第一天
                   if (i==0){
                         fromdateDB = splitBean.getFromdatedb(); //第一天,流程开始日期为初始数据默认
                         fromtimedb = splitBean.getFromtimedb(); //第一天,流程开始时间为初始数据默认
                         todatedb =splitBean.getTodatedb();       //第一天，流程结束日期为初始数据默认
                         totimedb   =splitBean.getTotimedb();   //第一天，流程结束时间为初始数据默认

                        fromdate = fromdateDB;      //第一天，实际有效的加班开始日期为流程开始日期
                        fromtime = fromtimedb;      //第一天，实际有效的加班开始时间为流程开始时间
                        todate = fromdateDB;         //第一天，实际有效的加班结束日期为当天
                        totime = "24:00";            //第一天，实际结束时间默认为"24:00"

                        belongDate = todate;        //归属日期就为当前实际开始日期

                       SplitBean splitBeanChild = new SplitBean();
                       //流程和实际开始日期和时间
                       splitBeanChild.setFromdatedb(fromdateDB);
                       splitBeanChild.setFromtimedb(fromtimedb);
                       splitBeanChild.setFromDate(fromdate);
                       splitBeanChild.setFromTime(fromtime);
                        //流程和实际结束日期和时间
                       splitBeanChild.setTodatedb(todatedb);
                       splitBeanChild.setToDate(todate);
                       splitBeanChild.setTotimedb(totimedb);
                       splitBeanChild.setToTime(totime);
                       //归属日期
                       splitBeanChild.setBelongDate(belongDate);
                       //流程id,人员id,考勤组id
                       splitBeanChild.setRequestId(requestId);
                       splitBeanChild.setResourceId(resourceId);
                       splitBeanChild.setGroupid(groupid);

                       splitBeans.add(splitBeanChild);
                       continue;
                   }
                   //如果是最后一天
                   if (i == count){
                       fromdate = todatedb;
                       fromtime = "00:00";
                       todate = todatedb;
                       totime = totimedb;

                       belongDate = todate;        //归属日期就为当前实际开始日期

                       SplitBean splitBeanChild = new SplitBean();
                       //流程和实际开始日期和时间
                       splitBeanChild.setFromdatedb(fromdateDB);
                       splitBeanChild.setFromtimedb(fromtimedb);
                       splitBeanChild.setFromDate(fromdate);
                       splitBeanChild.setFromTime(fromtime);
                       //流程和实际结束日期和时间
                       splitBeanChild.setTodatedb(todatedb);
                       splitBeanChild.setToDate(todate);
                       splitBeanChild.setTotimedb(totimedb);
                       splitBeanChild.setToTime(totime);
                       //归属日期
                       splitBeanChild.setBelongDate(belongDate);
                       //流程id,人员id,考勤组id
                       splitBeanChild.setRequestId(requestId);
                       splitBeanChild.setResourceId(resourceId);
                       splitBeanChild.setGroupid(groupid);

                       splitBeans.add(splitBeanChild);
                       break;
                   }
                   //如果是中间天数，将实际开始日期和结束日期往后推一天，实际开始时间和结束时间为默认值
                   Date fromdateD = sdf.parse(fromdate);
                   Date todateD = sdf.parse(todate);
                   dayAdd(fromdateD);
                   dayAdd(todateD);
                   //再转为String类
                   fromdate = DateChange(fromdateD);
                   todate = DateChange(todateD);

                   fromtime = "00:00";
                   totime = "24:00";

                   belongDate = todate;        //归属日期就为当前实际开始日期

                   SplitBean splitBeanChild = new SplitBean();
                   //流程和实际开始日期和时间
                   splitBeanChild.setFromdatedb(fromdateDB);
                   splitBeanChild.setFromtimedb(fromtimedb);
                   splitBeanChild.setFromDate(fromdate);
                   splitBeanChild.setFromTime(fromtime);
                   //流程和实际结束日期和时间
                   splitBeanChild.setTodatedb(todatedb);
                   splitBeanChild.setToDate(todate);
                   splitBeanChild.setTotimedb(totimedb);
                   splitBeanChild.setToTime(totime);
                   //归属日期
                   splitBeanChild.setBelongDate(belongDate);
                   //流程id,人员id,考勤组id
                   splitBeanChild.setRequestId(requestId);
                   splitBeanChild.setResourceId(resourceId);
                   splitBeanChild.setGroupid(groupid);

                   splitBeans.add(splitBeanChild);
               }
           }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(流程拆分转换报错信息)" + e);
        }

        return splitBeans;
    }

    /**
     * 这里计算一共跨多少天
     */
    public int dayCount(String fromdate,String todate){
        int i = 0;
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fromDay = sdf.parse(fromdate);
            Date toDay = sdf.parse(todate);
            Date nextDay = fromDay;

            //比较日期相差多少天
            while (nextDay.before(toDay)) {
                Calendar cld = Calendar.getInstance();
                cld.setTime(fromDay);
                cld.add(Calendar.DATE, 1);
                fromDay = cld.getTime();
                nextDay = fromDay;
                i++;
            }

        }catch (Exception e){

        }
        return i;
    }

    /**
     *这里将天数+1
     */
    public void dayAdd(Date date){
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.DATE,1);
    }

    /**
     * 这里将Date类型转为字符串
     */

    public String DateChange(Date date){
        DateFormat dateformat= new SimpleDateFormat("yyyy-MM-dd");
        String dateS = dateformat.format(date);

        return dateS;
    }

}
