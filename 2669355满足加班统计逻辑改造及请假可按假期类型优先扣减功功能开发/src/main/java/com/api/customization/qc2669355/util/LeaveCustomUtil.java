package com.api.customization.qc2669355.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.KQLeaveRulesBiz;
import com.engine.kq.biz.KQReportBiz;
import com.engine.kq.util.KQDurationCalculatorUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaveCustomUtil {
    private String BalanceOfLeaveTable = new BaseBean().getPropValue("qc2669355","BalanceOfLeaveTable");

    /**
     * 获取事假请假数据
     */
    public  Map<String,Object> getLeaveAbsence(String resourceid, User user){
        Map<String,Object> params = null;
        Map<String,Object> data = new HashMap<>();

        //获取所有流程数据
        params = getParams(resourceid);
        KQReportBiz kqReportBiz = new KQReportBiz();
        Map<String,Object> flowData = kqReportBiz.getFlowData(params,user);
        new BaseBean().writeLog("==zj==(flowData)" + JSON.toJSONString(flowData));
        //获取请假数据
        KQLeaveRulesBiz kqLeaveRulesBiz = new KQLeaveRulesBiz();
        List<Map<String, Object>> allLeaveRules = kqLeaveRulesBiz.getAllLeaveRules();
        Map<String, Object> leaveRule = null;
        for(int i=0;allLeaveRules!=null&&i<allLeaveRules.size();i++){
            leaveRule = (Map<String, Object>)allLeaveRules.get(i);
            String flowType = Util.null2String("leaveType_"+leaveRule.get("id"));
            String leaveData = Util.null2String(flowData.get(resourceid+"|"+flowType));
            String flowLeaveBackType = Util.null2String("leavebackType_"+leaveRule.get("id"));
            String leavebackData = Util.null2s(Util.null2String(flowData.get(resourceid+"|"+flowLeaveBackType)),"0.0");
            String b_flowLeaveData = "";
            String flowLeaveData = "";
            try{
                //以防止出现精度问题
                if(leaveData.length() == 0){
                    leaveData = "0.0";
                }
                if(leavebackData.length() == 0){
                    leavebackData = "0.0";
                }
                BigDecimal b_leaveData = new BigDecimal(leaveData);
                BigDecimal b_leavebackData = new BigDecimal(leavebackData);
                b_flowLeaveData = b_leaveData.subtract(b_leavebackData).toString();
                if(Util.getDoubleValue(b_flowLeaveData, -1) < 0){
                    b_flowLeaveData = "0.0";
                }
            }catch (Exception e){
                new BaseBean().writeLog("GetKQReportCmd:leaveData"+leaveData+":leavebackData:"+leavebackData+":"+e);

            }

            //考虑下冻结的数据
            if(b_flowLeaveData.length() > 0){
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(b_flowLeaveData);
            }else{
                flowLeaveData = KQDurationCalculatorUtil.getDurationRound(Util.null2String(Util.getDoubleValue(leaveData,0.0)-Util.getDoubleValue(leavebackData,0.0)));
            }
            data.put(flowType,flowLeaveData);
        }
       return data;
    }


    /**
     * 封装下请求流程类型参数
     * @param resourceId
     * @return
     */
    public Map<String,Object> getParams(String resourceId){
        String typeselect = "7";//时间范围--上个月
        String viewScope = "3";//查看范围--人员

        //封装数据
        JSONObject data = new JSONObject();
        data.put("typeselect",typeselect);
        data.put("viewScope",viewScope);
        data.put("resourceId",resourceId);

        Map<String,Object> params = new HashMap<>();
        params.put("data",data);

        return params;
    }

    /**
     * 获取指定月份的抵扣事假时长
     * @param resourceId
     * @param fromDate
     * @param toDate
     * @return
     */
    public String getLeave(String resourceId,String fromDate,String toDate){
        RecordSet rs = new RecordSet();
        String fromMonth = "";//开始月份
        String toMonth = "";//结束月份
        if (fromDate.length() >=7 || toDate.length() >=7){
            fromMonth = fromDate.substring(0,7);
            toMonth = toDate.substring(0,7);
        }
        String sql = "";
        String leaveAll = "";//总抵扣调休事假时长

        sql = "select SUM(dkdx) as leaveAll from "+BalanceOfLeaveTable+" where kqry="+resourceId+" and kqyf between '"+fromMonth+"' and '"+toMonth+"' ";
        new BaseBean().writeLog("==zj==(获取月份抵扣调休事假时长)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            leaveAll = Util.null2String(rs.getString("leaveAll"));
        }
        if (leaveAll.length() <= 0){
            leaveAll = "0.0";
        }

        return leaveAll;
    }
}
