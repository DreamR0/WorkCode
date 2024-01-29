package com.api.customization.qc20230816.popkqreport.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQReportFieldComInfo;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CheckUtil {

    /**
     * 弹窗考勤表是否显示字段
     *
     * @param fieldName
     * @return
     */
    public Boolean isShow(String fieldName) {
        Boolean isShow = false;
        if ("resourceId".equals(fieldName)) {
            isShow = true;
        }
        if ("lastname".equals(fieldName)) {
            isShow = true;
        }
        if ("subcompany".equals(fieldName)) {
            isShow = true;
        }
        if ("subcompanyId".equals(fieldName)) {
            isShow = true;
        }
        if ("department".equals(fieldName)) {
            isShow = true;
        }
        if ("departmentId".equals(fieldName)) {
            isShow = true;
        }
        if ("workdays".equals(fieldName)) {
            isShow = true;
        }
        if ("attendancedays".equals(fieldName)) {
            isShow = true;
        }
        if ("confirmType".equals(fieldName)) {
            isShow = true;
        }
        if ("confirmDate".equals(fieldName)) {
            isShow = true;
        }
        if ("popkqDate".equals(fieldName)) {
            isShow = true;
        }
        if ("overtime".equals(fieldName)){
            isShow = true;
        }
        if ("holidayOvertime_nonleave".equals(fieldName)){
            isShow = true;
        }
        if ("restDayOvertime_nonleave".equals(fieldName)){
            isShow = true;
        }
        if ("workingDayOvertime_nonleave".equals(fieldName)){
            isShow = true;
        }
        if ("overtimeTotal".equals(fieldName)){
            isShow = true;
        }
        return isShow;
    }

    /**
     * 检查当前人员是否需要弹窗提醒
     *
     * @return
     */
    public Map<String, Object> isPop(Map<String, Object> params, User user) {
        Map<String, Object> resultMap = new HashMap<>();
        RecordSet rs = new RecordSet();
        String sql = "";
        String resourceId = ""; //考勤人员id
        String confirmDate = ""; //当前日期
        String popSettableName = new BaseBean().getPropValue("qc20230816", "popSetTableName");//弹窗日期维护表
        String popNonTableName = new BaseBean().getPropValue("qc20230816", "popNonTableName");//弹窗免提醒人员表
        String poprecordTableName = new BaseBean().getPropValue("qc20230816", "poprecordTableName");//考勤弹窗记录表

        try {
            resourceId = Util.null2String(user.getUID());    //人员id
            confirmDate = Util.null2String(params.get("confirmDate"));  //当前日期

            //先查询人员是否在免提醒名单中
            sql = "select dldcmtxry from " + popNonTableName + " where dbms_lob.instr(dldcmtxry," + resourceId + ",1,1) > 0";
            new BaseBean().writeLog("==zj==(是否免提醒)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()) {
                resultMap.put("status", "1");
                resultMap.put("isPop", "false");
                return resultMap;
            }
            //再查询当前考勤月份是否有配置时间范围
            String needKqDate = ""; //考勤日期
            sql = "select * from " + popSettableName + " where '" + confirmDate + "' between mytxksrq and mytxjsrq";
            new BaseBean().writeLog("==zj==(是否在提醒日期)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()) {
                needKqDate = Util.null2String(rs.getString("kqyf"));
              /*  resultMap.put("status","1");
                resultMap.put("isPop","true");
                resultMap.put("kqdate",needKqDate);
                return resultMap;*/
            }
            //如果有确认考勤日期，再看看确认表里是否已经确认过了
            if (needKqDate.length() > 0) {
                sql = "select * from " + poprecordTableName + " where '" + needKqDate + "' = kqyf and ry='" + resourceId + "'";
                new BaseBean().writeLog("==zj==(是否已确认)" + JSON.toJSONString(sql));
                rs.executeQuery(sql);
                if (rs.next()) {
                    //说明已确认则不必弹窗
                    resultMap.put("status", "1");
                    resultMap.put("isPop", "false");
                    return resultMap;
                }
            }
            if (needKqDate.length() <= 0) {
                //当天没有要确认的考勤月份报表
                resultMap.put("status", "1");
                resultMap.put("isPop", "false");
                return resultMap;
            }
            if (needKqDate.length() > 0) {
                //说明有需要确认的考勤日期报表，且没有被确认过

                //获取年份
                int year =Integer.parseInt(needKqDate.substring(0,4));
                //获取月份
                int month =Integer.parseInt(needKqDate.substring(5,7));

                //获取当前月份的头尾
                String firstDay = getFirstDay(year, month, "yyyy-MM-dd");
                String lastDay = getLastDay(year, month, "yyyy-MM-dd");

                //返回结果
                resultMap.put("status", "1");
                resultMap.put("isPop", "true");
                resultMap.put("dateBegin",firstDay);
                resultMap.put("dateEnd",lastDay);
                resultMap.put("kqDate",needKqDate);
                return resultMap;
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(弹窗提醒报错)" + JSON.toJSONString(e));
        }
        resultMap.put("status", "1");
        resultMap.put("isPop", "false");
        return resultMap;
    }

    /**
     * 检查当前考勤报表信息是否需要显示确认
     * @return
     */
    public Boolean isConfirmButton(Map<String, Object> params, User user,String fromDate) {
        if (fromDate.length() > 7){
            fromDate = fromDate.substring(0,7);
        }
        //如果是系统管理员就都显示
        if (user.getUID() == 1){
            return true;
        }
        Boolean isConfirm = false;
        RecordSet rs = new RecordSet();
        String sql = "";
        String resourceId = ""; //考勤人员id
        String confirmDate = ""; //当前日期
        String popSettableName = new BaseBean().getPropValue("qc20230816", "popSetTableName");//弹窗日期维护表
        String popNonTableName = new BaseBean().getPropValue("qc20230816", "popNonTableName");//弹窗免提醒人员表
        String poprecordTableName = new BaseBean().getPropValue("qc20230816", "poprecordTableName");//考勤弹窗记录表

        try {
            LocalDate currentDate = LocalDate.now();
            resourceId = Util.null2String(user.getUID());    //人员id
            confirmDate = Util.null2String(currentDate);  //当前日期

            //先查询人员是否在免提醒名单中
            sql = "select dldcmtxry from " + popNonTableName + " where dbms_lob.instr(dldcmtxry," + resourceId + ",1,1) > 0";
            new BaseBean().writeLog("==zj==(是否免提醒-确认按钮)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()) {
              return false;
            }
            //再查询当前考勤月份是否有配置时间范围
            String needKqDate = ""; //考勤日期
            sql = "select * from " + popSettableName + " where '" + confirmDate + "' between mytxksrq and mytxjsrq";
            new BaseBean().writeLog("==zj==(是否在弹窗日期维护表-确认按钮)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()) {
                needKqDate = Util.null2String(rs.getString("kqyf"));
            }
            //如果有确认考勤日期，就和查询日期做对比，如果一致就显示考勤确认按钮
            if (needKqDate.compareTo(fromDate) == 0){
               return true;
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(考勤报表确认报错)" + JSON.toJSONString(e));
        }
      return isConfirm;
    }

    /**
     * 获取确认情况、确认时间、考勤月份
     *
     * @return
     */
    public String getConfirm(String resourceid, String Date, String confirmName) {
        RecordSet rs = new RecordSet();
        String poprecordTableName = new BaseBean().getPropValue("qc20230816", "poprecordTableName");
        String result = "";
        String kqDate = "";
        if (Date.length() > 7) {
            kqDate = Date.substring(0, 7);
        } else {
            kqDate = Date;
        }
        String sql = "select " + confirmName + " from " + poprecordTableName + " where ry='" + resourceid + "' and kqyf='" + kqDate + "'";
        new BaseBean().writeLog("==zj==(弹窗考勤报表自定义列sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()) {
            result = Util.null2String(rs.getString(confirmName));
            if ("qrqk".equals(confirmName)) {
                //如果是确认情况，0--系统确认  1--本人确认
                if ("0".equals(result)) {
                    result = "系统确认";
                }
                if ("1".equals(result)) {
                    result = "本人确认";
                }
            }
        }

        return result;
    }

    /**
     * 指定月考勤数据已确认人员
     *
     * @return
     */
    public String isConfirm(String Date) {
        RecordSet rs = new RecordSet();
        String resourcIds = "";
        String kqDate = "";
        String poprecordTableName = new BaseBean().getPropValue("qc20230816", "poprecordTableName");

        if (Date.length() > 7) {
            kqDate = Date.substring(0, 7);
        } else {
            kqDate = Date;
        }
        String sql = "select ry from " + poprecordTableName + " where  kqyf='" + kqDate + "'";
        rs.executeQuery(sql);
        while (rs.next()) {
            resourcIds += Util.null2String(rs.getString("ry")) + ",";
        }
        if (resourcIds.length() > 0) {
            resourcIds = resourcIds.substring(0, resourcIds.length() - 1);
        }
        return resourcIds;
    }

    /**
     * 指定月考勤数据已确认人员sql
     * @param Date 考勤月份
     * @return
     */
    public String getConfirmSql(String Date) {
        RecordSet rs = new RecordSet();
        String resourcIds = "";
        String kqDate = "";
        String poprecordTableName = new BaseBean().getPropValue("qc20230816", "poprecordTableName");

        if (Date.length() > 7) {
            kqDate = Date.substring(0, 7);
        } else {
            kqDate = Date;
        }
        String sql = "select ry from " + poprecordTableName + " where  kqyf='" + kqDate + "'";
       return sql;
    }

    /**
     * 根据年月获取月初第一天日期
     *
     * @param year
     * @param month
     * @return
     */
    public static String getFirstDay(int year, int month, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        Date startDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String formattedStartDate = sdf.format(startDate);

        return formattedStartDate;
    }


    /**
     * 根据年月获取月末最后一天日期
     *
     * @param year
     * @param month
     * @return
     */
    public static String getLastDay(int year, int month, String format) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);

        calendar.set(Calendar.DAY_OF_MONTH, 1);

        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date endDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String formattedEndDate = sdf.format(endDate);

        return formattedEndDate;
    }
}
