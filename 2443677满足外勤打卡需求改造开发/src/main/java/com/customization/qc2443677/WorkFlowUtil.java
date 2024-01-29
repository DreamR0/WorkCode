package com.customization.qc2443677;

import com.alibaba.fastjson.JSONObject;
import com.engine.workflow.biz.freeNode.FreeNodeBiz;
import com.engine.workflow.entity.publicApi.ReqOperateRequestEntity;
import com.engine.workflow.entity.publicApi.WorkflowDetailTableInfoEntity;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.workflow.webservices.WorkflowRequestTableField;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author Trump
 * @create 2021/11/9 14:21
 */
public class WorkFlowUtil {

    /**
     * 《通用》转为Entity对象
     * @param requestJSON
     * @return
     */
    public static ReqOperateRequestEntity parseRequestEntity(JSONObject requestJSON){
        ReqOperateRequestEntity entity = new ReqOperateRequestEntity();
        try {
            int workflowId = Util.getIntValue(requestJSON.getIntValue("workflowId"));
            int requestId = Util.getIntValue(requestJSON.getIntValue("requestId"));
            String requestName = Util.null2String(requestJSON.getString("requestName"));
            int userId = Util.getIntValue(requestJSON.getIntValue("userId"));
            int forwardFlag = Util.getIntValue(requestJSON.getIntValue("forwardFlag"));
            String forwardResourceIds = Util.null2String(requestJSON.getString("forwardResourceIds"));
            String mainData = Util.null2String(requestJSON.getString("mainData"));

            if (!"".equals(mainData)) {
                try {
                    List<WorkflowRequestTableField> mainDatas = JSONObject.parseArray(mainData, WorkflowRequestTableField.class);
                    entity.setMainData(mainDatas);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String detailData = Util.null2String(requestJSON.getString("detailData"));
            if (!"".equals(detailData)) {
                try {
                    List<WorkflowDetailTableInfoEntity> detailDatas = JSONObject.parseArray(detailData, WorkflowDetailTableInfoEntity.class);
                    entity.setDetailData(detailDatas);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String remark = Util.null2String(requestJSON.getString("remark"));
            String requestLevel = Util.null2String(requestJSON.getString("requestLevel"));
            String otherParams = Util.null2String(requestJSON.getString("otherParams"));
            if (!"".equals(otherParams)) {
                try {
                    Map<String,Object> otherParamsMap = (Map<String,Object>)JSONObject.parseObject(otherParams, Map.class);
                    entity.setOtherParams(otherParamsMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            entity.setWorkflowId(workflowId);
            if (requestId > 0){
                entity.setRequestId(requestId);
            }
            //entity.setRequestId(requestId);
            entity.setRequestName(requestName);
            entity.setUserId(userId);
            entity.setRemark(remark);
            entity.setRequestLevel(requestLevel);
            entity.setForwardFlag(forwardFlag);
            entity.setForwardResourceIds(forwardResourceIds);
            entity.setClientIp(requestJSON.getString("ip"));
            int submitNodeId = Util.getIntValue(Util.null2String(requestJSON.getString("submitNodeId")));
            if (submitNodeId > 0 || FreeNodeBiz.isFreeNode(submitNodeId)) {
                entity.setSubmitNodeId(submitNodeId);
                entity.setEnableIntervenor("1".equals(Util.null2s(requestJSON.getString("enableIntervenor"), "1")));
                entity.setSignType(Util.getIntValue(Util.null2String(requestJSON.getString("SignType")), 0));
                entity.setIntervenorid(Util.null2String(requestJSON.getString("Intervenorid")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            new BaseBean().writeLog("WorkFlowCmd>> 流程数据异常："+e);
        }
        return entity;
    }

}
