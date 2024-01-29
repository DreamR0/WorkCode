package com.api.customization.qc20230816.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.HashMap;
import java.util.Map;

public class ResourceUtil {


    /**
     * 判断当前日期是否在入职日期之前，是显示为空
     * @param resoureid
     * @param date
     * @return
     */
    public Boolean getCompanyMessage(String resoureid,String date){
        Boolean isHave = false;
        String start = "";//入职日期
        String end = "";//合同结束日期
        String status = "";//人员状态
        RecordSet rs = new RecordSet();
        String sql = "select companystartdate,enddate,status from hrmresource where id ='" + resoureid+"'";
        new BaseBean().writeLog("==zj==(日历显示是否离职sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            start = Util.null2String(rs.getString("companystartdate"));
            end = Util.null2String(rs.getString("enddate"));
            status = Util.null2String(rs.getString("status"));
        }

        if (date.compareTo(start) < 0){
            //当前日期在入职日期之前
            isHave = true;
        }
        //状态为离职状态，才进行判断 4:解聘 5：离职 6：退休 7：无效
        if ("4".equals(status) || "5".equals(status) || "6".equals(status) || "7".equals(status)){
            if (!"".equals(end) && date.compareTo(end) > 0){
                isHave = true;
            }
        }
        new BaseBean().writeLog("==zj==(人员当前日期状态)"+resoureid+" | "+ status + " | " + date + " | " + start + " | " + end + " | " + isHave);

        return isHave;
    }
}
