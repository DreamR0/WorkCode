package com.api.customization.qc2563653.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.HashMap;
import java.util.Map;

public class CheckUtil {


    /**
     * 获取分部加班单位设置
     * @param resourceId
     * @return
     */
    public Map<String,Object> getOverSet(String resourceId){
        Map<String,Object> reusltMap = new HashMap();
        RecordSet rs = new RecordSet();
        String subcompanyid1 = "";  //分部id
        int minimumUnit = -1;    //最小加班单位
        Double hoursToDay = -1.0;     //日折算时长
        int overtimeConversion = -1;     //加班时长折算方式
        String sql = "";

        //获取当前人员的分部id
        sql = "select * from hrmresource where id='"+resourceId+"'";
        rs.executeQuery(sql);
        if (rs.next()){
            subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
        }
        //如果没有分部，则按系统标准
        if (subcompanyid1.length() < 0){
            return reusltMap;
        }
        sql = "select * from uf_overtimeUnit where concat(',', subIds, ',') like '%,"+subcompanyid1+",%'";
        new BaseBean().writeLog("==zj==(加班设置维护sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            minimumUnit = Util.getIntValue(rs.getString("minimumUnit"));    //获取最小加班单位
            hoursToDay = Util.getDoubleValue(rs.getString("housToDay"));    //日折算时长
            overtimeConversion = Util.getIntValue(rs.getString("overtimeConversion"));    //加班时长折算方式
            reusltMap.put("minimumUnit",minimumUnit+1);
            reusltMap.put("hoursToDay",hoursToDay);
            reusltMap.put("overtimeConversion",overtimeConversion+1);
        }
        return reusltMap;
    }
}
