package com.api.cusomization.qc2721235.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import com.engine.kq.biz.KQSettingsBiz;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Map;

public class KqUtil {
   public String tiaoxiuTypeId = new BaseBean().getPropValue("qc2721235","paidLeaveId");

    /**
     * 获取当前人员的调休余额
     * @param resourceId    人员id
     * @return
     */
    public Double getRestAmount(String resourceId,String fromDate,String toDate){
        Double allRestAmountD=0.0;
        try {

            Calendar today = Calendar.getInstance();
            /**今年*/
            String currentDate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            String allRestAmount = KQBalanceOfLeaveBiz.getRestAmount("" + resourceId, tiaoxiuTypeId, currentDate, true, true/*,fromDate,toDate,1*/);
            if (!"".equals(allRestAmount)){
                allRestAmountD = Double.parseDouble(allRestAmount);
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取调休余额错误)" + e);
        }
        return allRestAmountD;
    }

    /**
     * 获取查询日期范围内的夜班班次次数
     * @param resourceId
     * @param fromDate
     * @param toDate
     * @return
     */
    public int getSerialCount(String resourceId, String fromDate, String toDate){
        RecordSet rs = new RecordSet();
        String sql = "";
        int serialCount = 0;
        try{
            sql = "select count(1) from hrmresource a, kq_format_total b where a.id= b.resourceid and b.resourceid = ? and b.kqdate >=? and b.kqdate <=? and b.serialId in(select id from kq_ShiftManagement where isNight = 1 and isdelete is null) ";
            rs.executeQuery(sql,resourceId,fromDate,toDate);
            if(rs.next()){
                serialCount = rs.getInt(1);
            }
        }catch (Exception e){
            new BaseBean().writeLog("夜班班次查询" + e);
        }
        return serialCount;
    }

    /**
     * 获取指定查询范围加班抵扣调休后的加班时长
     *
     * @param resourceId        人员ID
     * @return
     */
    public  String getRestAmount(String resourceId, String fromDate, String toDate, Map<String,Object> flowData,int changeType) {
        BigDecimal allTiaoxiuamount = new BigDecimal("0");
        String tiaoxiuAmount = "";
        try {

            String sql = "";
            RecordSet recordSet = new RecordSet();
            if (recordSet.getDBType().equalsIgnoreCase("sqlserver")
                    || recordSet.getDBType().equalsIgnoreCase("mysql")) {
                sql = " select sum(tiaoxiuamount) as allTiaoxiuamount,sum(baseAmount) as allBaseAmount,sum(extraAmount) as allExtraAmount,sum(usedAmount) as allUsedAmount from KQ_BalanceOfLeave " +
                        " where (isDelete is null or isDelete<>1) and resourceId=" + resourceId + " and leaveRulesId=" + tiaoxiuTypeId + " and (effectiveDate between '"+fromDate+"' and '"+toDate+"'"+") ";
            } else {
                sql = " select sum(tiaoxiuamount) as allTiaoxiuamount,sum(baseAmount) as allBaseAmount,sum(extraAmount) as allExtraAmount,sum(usedAmount) as allUsedAmount from KQ_BalanceOfLeave " +
                        " where (isDelete is null or isDelete<>1) and resourceId=" + resourceId + " and leaveRulesId=" + tiaoxiuTypeId + " and (effectiveDate between '"+fromDate+"' and '"+toDate+"'"+") ";
            }
            sql += " and changeType = "+changeType;

            new BaseBean().writeLog("==zj==(自定义获取列)" + JSON.toJSONString(sql));
            recordSet.executeQuery(sql);
            while (recordSet.next()) {
                allTiaoxiuamount = new BigDecimal(Util.null2s(recordSet.getString("allTiaoxiuamount"), "0"));
                BigDecimal _usedAmount = new BigDecimal(Util.null2s(recordSet.getString("allUsedAmount"), "0"));
                allTiaoxiuamount = allTiaoxiuamount.subtract(_usedAmount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (allTiaoxiuamount.doubleValue() <= 0){
            tiaoxiuAmount = "0.00";
        }else {
            tiaoxiuAmount = allTiaoxiuamount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (changeType == 3){
            new BaseBean().writeLog("==zj==(休息日抵扣调休之后的加班时长)" + resourceId + " | " + fromDate+"-"+toDate+" | "+allTiaoxiuamount);
        }
        if (changeType == 2){
            new BaseBean().writeLog("==zj==(工作日抵扣调休之后的加班时长)" + resourceId + " | " + fromDate+"-"+toDate+" | "+allTiaoxiuamount);
        }

        return tiaoxiuAmount;

    }

    /**
     * 获取指定类型的加班时长
     * @param flowData
     * @param changeType
     * @return
     */
    public BigDecimal getoverTime(String id,Map<String,Object> flowData,int changeType){
        double overTimeTotal = 0.0;
        if (changeType == 3){
            //休息日
            overTimeTotal = Util.getDoubleValue(Util.null2String(flowData.get(id+"|restDayOvertime_4leave")));
            overTimeTotal = overTimeTotal<0?0:overTimeTotal;
        }

        if (changeType == 2){
            //工作日
            overTimeTotal = Util.getDoubleValue(Util.null2String(flowData.get(id+"|workingDayOvertime_4leave")));
            overTimeTotal = overTimeTotal<0?0:overTimeTotal;
        }
        BigDecimal bigDecimal = new BigDecimal(Double.toString(overTimeTotal));
        return bigDecimal;
    }



}
