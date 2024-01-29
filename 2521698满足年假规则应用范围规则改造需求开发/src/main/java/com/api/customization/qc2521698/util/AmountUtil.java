package com.api.customization.qc2521698.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.conn.StringUtil;
import weaver.general.BaseBean;
import weaver.general.Util;

public class AmountUtil {


    /**
     * 判断是否是年假,且有无设置界定日期
     * @param ruleId 假期类型id
     * @return
     */
    public Boolean isYearLeave(String ruleId){
    Boolean isYearLeave = false;    //是否年假
    String ruleName = "";           //规则名称
    String scopeDate = "";          //自定义字段 界定日期
    RecordSet rs = new RecordSet();
    String sql="";
     sql = "select * from  KQ_leaveRulesDetail where ruleId=" + ruleId;
    rs.executeQuery(sql);
    if (rs.next()){
        ruleName = Util.null2String(rs.getString("ruleName"));
        scopeDate = Util.null2String(rs.getString("scopeDate"));
        new BaseBean().writeLog("==zj==(是否是年假)" + ruleName +  "  |  " + scopeDate);
        if (ruleName.contains("年假") && !"".equals(scopeDate)){
            isYearLeave = true;
        }
    }
    return isYearLeave;
}

    /**
     * 判断是否为新老员工
     * @param resourceId
     * @param companyStartDate
     * @return
     */
    public Integer getReleaseRule(String resourceId, String companyStartDate,String ruleId ){
        String scopeDate = "";
        int releaseRule = 0;
        RecordSet rs = new RecordSet();
        String sql = "select * from KQ_leaveRulesDetail where ruleId=" + ruleId;
        rs.executeQuery(sql);
        if (rs.next()){
            scopeDate = Util.null2String(rs.getString("scopeDate"));
        }
        new BaseBean().writeLog("==zj==(是否新老员工)" + "入职日期:" + companyStartDate  + "--界定范围日期:" + scopeDate + "  |  " + companyStartDate.compareTo(scopeDate));
        if (companyStartDate.compareTo(scopeDate) > 0){
            //新员工
            releaseRule = 2;

        }else {
            //老员工
            releaseRule = 0;
        }
        return releaseRule;
    }
}
