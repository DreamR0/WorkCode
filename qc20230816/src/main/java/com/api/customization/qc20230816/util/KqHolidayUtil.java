package com.api.customization.qc20230816.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import javax.ejb.Local;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

public class KqHolidayUtil {
    private String kqGroupList;     //考勤组
    private String holidayDateStart;//节假日开始日期
    private String holidayDateEnd;//节假日结束日期
    private int holidayType;//节假日类型
    private String holidayTimeStart1;//节假日开始时间1
    private String holidayTimeEnd1;//节假日结束时间1
    private String holidayTimeStart2;//节假日开始时间2
    private String holidayTimeEnd2;//节假日结束时间2

    public Integer holidayMinsCal(String userId,String groupId,String kqDate,String signInTime,String signOutTime){
        RecordSet rs = new RecordSet();
        int holidayMins = 0;
        //查询当天是否有对应节假日加班工时设置
       /* String sql = "select * from uf_ysw_kqzzdydx where kqzzdydx in ("+groupId+") and '"+kqDate+"' between JJRKSRQ and JJRJSRQ";*/
        String sql = "select * from uf_ysw_kqzzdydx where instr(',' || kqzzdydx || ',', ',' || "+groupId+" || ',')>0 and '"+kqDate+"' between JJRKSRQ and JJRJSRQ";
        new BaseBean().writeLog("==zj==(法定节假日建模表sql)" +JSON.toJSONString(sql));
        rs.executeQuery(sql);
       if (rs.next()){
           kqGroupList = Util.null2String(rs.getString("kqzzdydx"));   //设置考勤组
           holidayDateStart = Util.null2String(rs.getString("jjrksrq"));   //节假日开始日期
           holidayDateEnd = Util.null2String(rs.getString("jjrjsrq"));   //节假日结束日期
           holidayType = Integer.parseInt(rs.getString("jjrlx"));   //节假日类型
           holidayTimeStart1 = Util.null2String(rs.getString("jjrkssj1"));   //节假日开始时间1
           holidayTimeEnd1 = Util.null2String(rs.getString("jjrjssj1"));   //节假日结束时间1
           holidayTimeStart2 = Util.null2String(rs.getString("jjrkssj2"));   //节假日开始时间2
           holidayTimeEnd2 = Util.null2String(rs.getString("jjrjssj2"));   //节假日结束时间2

           new BaseBean().writeLog("==zj==(holidayType)" + JSON.toJSONString(holidayType));
           if (holidayType == 0){
               //节假日加班工时计算类型“当日内”
                holidayMins = kqHolidayType0(userId,kqDate,holidayTimeStart1, holidayTimeEnd1);
           }

           if (holidayType == 1){
               //节假日加班工时计算类型
               holidayMins = kqHolidayType1(userId,kqDate,holidayTimeStart1, holidayTimeEnd1, holidayTimeStart2, holidayTimeEnd2);
           }
       }

       return holidayMins;
    }



    /**
     * 当日内计算方式
     */
    public Integer kqHolidayType0(String userId,String kqDate,String holidayTimeStart1,String holidayTimeEnd1){
        int holidayMins = 0;    //节假日加班工时

        try {
            holidayMins = getTimeScope1(userId,kqDate,holidayTimeStart1,holidayTimeEnd1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return holidayMins;
    }

    public Integer kqHolidayType1(String userId,String kqDate,String holidayTimeStart1,String holidayTimeEnd1,String holidayTimeStart2,String holidayTimeEnd2){
        int holidayMins = 0;

        try {
            holidayMins =  getTimeScope2(userId,kqDate,holidayTimeStart1,holidayTimeEnd1,holidayTimeStart2,holidayTimeEnd2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return holidayMins;

    }

    /**
     * 这里计算节假日加班工作时长类型1（当日内）
     * @param holidayTimeStart
     * @param holidayTimeEnd
     * @return
     */
    public Integer getTimeScope1(String userId,String kqDate,String holidayTimeStart,String holidayTimeEnd){
        int sum = 0;
        Boolean startCard = false;  //开始卡
        Boolean endCard = false;    //结束卡
        String sql = "";
        RecordSet rs = new RecordSet();
        ArrayList<LocalTime> timeScopeList = new ArrayList();
        long holidayMins = 0;
        try {
            //这里特殊设置下23:59分问题，有几个23:59,就加几分钟
            sum += calIndex(holidayTimeStart);
            sum += calIndex(holidayTimeEnd);

            LocalTime holidayTimeS = LocalTime.parse(holidayTimeStart);     //节假日开始时间1
            LocalTime holidayTimeE = LocalTime.parse(holidayTimeEnd);       //节假日结束时间1

            //计算开始时间的前后范围
            LocalTime holidayBegin1 = holidayTimeS.minusHours(1);   //前1小时
            LocalTime holidayBegin2 = holidayTimeS.plusHours(1);   //后1小时
            //计算结束时间的前后范围
            LocalTime holidayEnd1 = holidayTimeE.minusHours(1);   //前1小时
            LocalTime holidayEnd2 = holidayTimeE.plusHours(1);   //后1小时
            //加到list
            timeScopeList.add(holidayBegin1);
            timeScopeList.add(holidayBegin2);
            timeScopeList.add(holidayEnd1);
            timeScopeList.add(holidayEnd2);

            //判断开始和结束时间前后范围内是否存在满足打卡
            for (int i = 0; i < timeScopeList.size(); i+=2) {
                int cycle = 1;
                sql = "select * from hrmschedulesign where userid='"+userId+"' and signdate='"+kqDate+"' and signtime between '"+timeScopeList.get(i)+"' and '"+timeScopeList.get(i+1)+"'";
                new BaseBean().writeLog("==zj==(节假日加班时长1)" + JSON.toJSONString(sql));
                rs.executeQuery(sql);
                if (rs.next()){
                    if (cycle == 1){
                        startCard = true;
                    }
                    if (cycle == 2){
                        endCard = true;
                    }
                }
                cycle++;
            }
            //如果满足打卡计算节假日加班时长
            new BaseBean().writeLog("==zj==(满足头尾打卡-当日)" + userId + "   |   " + startCard + " | " + endCard);
            if (startCard && endCard){
                holidayMins += Duration.between(holidayTimeS, holidayTimeE).getSeconds();
            }
            new BaseBean().writeLog("==zj==(holidayMins-当日)" + JSON.toJSONString(holidayMins));
            new BaseBean().writeLog("==zj==(sum当日)" + JSON.toJSONString(sum));
            if (holidayMins != 0 ){
                holidayMins = holidayMins/60 + sum;
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(节假日类型1计算错误)" + JSON.toJSONString(e));
        }

        return (int)holidayMins;

    }
    public Integer getTimeScope2(String userId,String kqDate,String holidayTimeStart1,String holidayTimeEnd1,String holidayTimeStart2,String holidayTimeEnd2){
        long holidayMins = 0;
        int sum = 0;
        Boolean startCard = false;  //开始卡
        Boolean endCard = false;    //结束卡
        String sql = "";
        RecordSet rs = new RecordSet();
        ArrayList<LocalTime> timeScopeList = new ArrayList();
        try {
            //这里特殊设置下23:59分问题，有几个23:59,就加几分钟
           sum += calIndex(holidayTimeStart1);
           sum += calIndex(holidayTimeEnd1);
           sum += calIndex(holidayTimeStart2);
           sum += calIndex(holidayTimeEnd2);

            LocalTime holidayTimeS1 = LocalTime.parse(holidayTimeStart1);     //节假日开始时间1
            LocalTime holidayTimeE1 = LocalTime.parse(holidayTimeEnd1);       //节假日结束时间1

            LocalTime holidayTimeS2 = LocalTime.parse(holidayTimeStart2);     //节假日开始时间2
            LocalTime holidayTimeE2 = LocalTime.parse(holidayTimeEnd2);       //节假日结束时间2




            //计算开始时间的前后范围
            LocalTime holidayBegin1 = holidayTimeE1.minusHours(1);   //前1小时
            LocalTime holidayBegin2 = holidayTimeE1.plusHours(1);   //后1小时
            //计算结束时间的前后范围
            LocalTime holidayEnd1 = holidayTimeS2.minusHours(1);   //前1小时
            LocalTime holidayEnd2 = holidayTimeS2.plusHours(1);   //后1小时
            timeScopeList.add(holidayBegin1);
            timeScopeList.add(holidayBegin2);
            timeScopeList.add(holidayEnd1);
            timeScopeList.add(holidayEnd2);

            //判断开始和结束时间前后范围内是否存在满足打卡
            for (int i = 0; i < timeScopeList.size(); i+=2) {

                sql = "select * from hrmschedulesign where userid='"+userId+"' and signdate='"+kqDate+"' and signtime between '"+timeScopeList.get(i)+"' and '"+timeScopeList.get(i+1)+"'";
                new BaseBean().writeLog("==zj==(节假日加班时长2)" + JSON.toJSONString(sql));
                rs.executeQuery(sql);
                if (rs.next()){
                    new BaseBean().writeLog("==zj==(i)" + JSON.toJSONString(i));
                    if (i == 0){
                        startCard = true;
                    }

                    if (i == 2){
                        endCard = true;
                    }
                }

            }
            //如果满足打卡计算节假日加班时长
            new BaseBean().writeLog("==zj==(满足头尾打卡)" + userId + "   |   " + startCard + " | " + endCard);
            if (startCard && endCard){
                holidayMins += Duration.between(holidayTimeS1, holidayTimeE1).getSeconds();
                holidayMins += Duration.between(holidayTimeS2, holidayTimeE2).getSeconds();
            }
            new BaseBean().writeLog("==zj==(holidayMins-跨日)" + JSON.toJSONString(holidayMins));
            new BaseBean().writeLog("==zj==(sum0跨日)" + JSON.toJSONString(sum));
            if (holidayMins != 0 ){
                holidayMins = holidayMins/60 + sum ;
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(节假日类型2计算错误)" + JSON.toJSONString(e));
        }
        new BaseBean().writeLog("==zj==(转换节假日时长)" + JSON.toJSONString((int)holidayMins));
        return (int)holidayMins;
    }

    /**
     * 这里计算23:59这个特殊时间
     * @return
     */
    public int calIndex(String time){

        if ("23:59".equals(time)){
          return 1;
        }

        return 0;
    }


}
