package com.api.customization.qc2658035.util;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

public class UserUtil {
    String userLevelTable = new BaseBean().getPropValue("qc2658035","userLevelTable");//人员级别表
    String overLeaveRuleTable = new BaseBean().getPropValue("qc2658035","overLeaveRuleTable");//级别调休表
    /**
     * 这里获取该用户职级规则
     * @param resourceId
     * @return
     */
    public String getUserRank(String resourceId){
        String sql = "";
        String userLevel = "";
        String paidleaveRule = "";
        RecordSet rs = new RecordSet();

        //先判断当前人员的职级
        sql = "select * from "+ userLevelTable +" where xm="+resourceId+";";
        rs.executeQuery(sql);
        if (rs.next()){
            userLevel = Util.null2String(rs.getString("zj"));
        }
        //查询对应职级的调休规则
        sql = "select zj,dxgz from "+overLeaveRuleTable+" where EXISTS( select 1 from STRING_SPLIT(zj,',') where value='"+userLevel+"')";
        rs.executeQuery(sql);
        if (rs.next()){
            paidleaveRule = Util.null2String(rs.getString("dxgz"));
        }
        return paidleaveRule;
    }
}
