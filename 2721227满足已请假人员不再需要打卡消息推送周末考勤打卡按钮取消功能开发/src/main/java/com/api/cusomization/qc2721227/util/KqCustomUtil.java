package com.api.cusomization.qc2721227.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQFlowDataBiz;
import com.engine.kq.biz.KQTimesArrayComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.TimeScopeEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.engine.kq.enums.FlowReportTypeEnum;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KqCustomUtil {

    /**
     * 判断当天下班时间是否有请假
     * @param resourceId    考勤人员
     * @param kqDate        考勤日期
     * @return
     */
    public Boolean isLeave(String resourceId,String kqDate){
        Boolean isLeave = false;
        try {
            List<Object> workFlow = null;
            KQTimesArrayComInfo kqTimesArrayComInfo = new KQTimesArrayComInfo();

            //如果当天有请假流程就不需要打卡提醒
            workFlow =  getFlowData(resourceId,kqDate);
            new BaseBean().writeLog("==zj==(所有考勤流程)" + JSON.toJSONString(workFlow));
            if (workFlow != null){
                for (int i = 0; i < workFlow.size(); i++) {
                    Map<String, Object> data = (Map<String, Object>) workFlow.get(i);
                    String flowType = Util.null2String(data.get("flowtype"));
                    String newLeaveType = Util.null2String(data.get("newleavetype"));
                    String signtype = Util.null2String(data.get("signtype"));
                    String serial = Util.null2String(data.get("serial"));
                    String requestId = Util.null2String(data.get("requestId"));

                    int beginIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("begintime")));
                    int endIdx = kqTimesArrayComInfo.getArrayindexByTimes(Util.null2String(data.get("endtime")));

                    if (flowType.equalsIgnoreCase(FlowReportTypeEnum.LEAVE.getFlowType())){
                        isLeave = true;
                    }
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取请假流程报错)" + JSON.toJSONString(e));
        }
        return isLeave;
    }

    /**
     * 获取当天的考勤流程数据
     * @param userId 考勤人员
     * @param kqDate 考勤日期
     */
    public List<Object> getFlowData(String userId, String kqDate){
        List<Object> workFlow = null;
        String dateKey = userId + "|" + kqDate;
        String kqDateNext = DateUtil.addDate(kqDate, 1);
        KQFlowDataBiz kqFlowDataBiz = new KQFlowDataBiz.FlowDataParamBuilder().resourceidParam(userId).fromDateParam(kqDate).toDateParam(kqDateNext).build();
        Map<String, Object> workFlowInfo = new HashMap<>();
        kqFlowDataBiz.getAllFlowData(workFlowInfo, false);
        new BaseBean().writeLog("==zj==(查询当前人员所有考勤流程信息)" + dateKey);
        new BaseBean().writeLog("==zj==(workFlowInfo)" + JSON.toJSONString(workFlowInfo));
        workFlow = (List<Object>) workFlowInfo.get(dateKey);
        return workFlow;
    }

    /**
     * 检测非工作日是否打卡
     * @return
     */
    public boolean isOpen(){
        String sql = "";
        String isOpen="";
        RecordSet rs = new RecordSet();
        sql = "select * from kq_settings where main_key='isNonWorkShow'";
        rs.executeQuery(sql);
        if (rs.next()){
            isOpen = Util.null2String(rs.getString("main_val"));
        }
        if ("1".equals(isOpen)){
            return true;
        }else {
            return false;
        }
    }
}
