package com.customization.SMSapproval.Util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.common.util.ServiceUtil;
import com.engine.workflow.entity.publicApi.PAResponseEntity;
import com.engine.workflow.entity.publicApi.ReqOperateRequestEntity;
import com.engine.workflow.publicApi.WorkflowRequestOperatePA;
import com.engine.workflow.publicApi.impl.WorkflowRequestOperatePAImpl;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.webservices.WorkflowRequestTableField;

import java.util.List;
import java.util.Map;

public  class smsUtil {

    //消息提交流程方法
    public  PAResponseEntity submit(int requestid, User user){
        int workflowid = 0;         //工作流id
        String lastnodeid = "";         //最后操作节点id
        String currentnodeid = "";      //当前节点id
        String requestname = "";        //流程名称
        //获取requestname
        RecordSet rn = new RecordSet();
        String rnsql = "select requestname from workflow_requestbase where requestid = " + requestid;
        rn.executeQuery(rnsql);
        if (rn.next()){
            requestname = Util.null2String(rn.getString(requestname));
            new BaseBean().writeLog("==zj==requestname（同意）获取" +  requestname);
        }

        //查询workflow_requestbase表,获取workflowid，lastnodeid,currentnodeid
        RecordSet rw = new RecordSet();
        String rwsql = "select * from workflow_requestbase where requestid = " + requestid;
        rw.executeQuery(rwsql);
        if (rw.next()){
            workflowid = rw.getInt("workflowid");
            lastnodeid = Util.null2String(rw.getString("lastnodeid"));
            currentnodeid = Util.null2String(rw.getString("currentnodeid"));
            new BaseBean().writeLog("==zj==workflow数据表---" + "workflowid:" + workflowid + ",lastnodeid:" + lastnodeid + ",currentnodeid" + currentnodeid);
        }

        //提交流程
        JSONObject otherParams = new JSONObject();
        ReqOperateRequestEntity entity = null;
        otherParams.put("src","submit");
        otherParams.put("isnextflow","1");
        entity = createReqOperateRequestEntity(requestname,workflowid,"",otherParams.toJSONString(),user);
        entity.setRequestId(requestid);

        WorkflowRequestOperatePA workflowRequestOperatePA= ServiceUtil.getService(WorkflowRequestOperatePAImpl.class);
        PAResponseEntity resultEntity = workflowRequestOperatePA.submitRequest(user,entity);

        return resultEntity;
    }

    //消息流程回退方法
    public PAResponseEntity reject(int requestid,User user){
        WorkflowRequestOperatePA workflowRequestOperatePA= ServiceUtil.getService(WorkflowRequestOperatePAImpl.class);
        int workflowid = 0;
        String requestname = "";

        //获取requestname
        RecordSet rn = new RecordSet();
        String rnsql = "select requestname from workflow_requestbase where requestid = " + requestid;
        rn.executeQuery(rnsql);
        if (rn.next()){
            requestname = Util.null2String(rn.getString(requestname));
            new BaseBean().writeLog("==zj==requestname（不同意）获取" +  requestname);
        }

        //获取workflowid
        RecordSet rs = new RecordSet();
        String rwsql = "select * from workflow_requestbase where requestid = " + requestid;
        rs.executeQuery(rwsql);
        if (rs.next()){
            workflowid = rs.getInt("workflowid");
        }


        //回退流程
        JSONObject otherParams = new JSONObject();
        ReqOperateRequestEntity entity = null;
        otherParams.put("src","reject");
        otherParams.put("isnextflow","1");
        //设置回退节点（测试）
        otherParams.put("RejectToNodeid","4");

        entity = createReqOperateRequestEntity(requestname,workflowid,"",otherParams.toJSONString(),user);
        entity.setRequestId(requestid);

        PAResponseEntity resultEntity =  workflowRequestOperatePA.rejectRequest(user,entity);
        return resultEntity;
    }

    public static ReqOperateRequestEntity createReqOperateRequestEntity( String requestname , int workflowid, String mainData, String otherParams,User user){
        ReqOperateRequestEntity entity = new ReqOperateRequestEntity();
        entity.setWorkflowId(workflowid);
        entity.setUserId(user.getUID());
        entity.setRequestName(requestname);
        if (!"".equals(mainData)) {
            try {
                List<WorkflowRequestTableField> mainDatas = JSONObject.parseArray(mainData, WorkflowRequestTableField.class);
                entity.setMainData(mainDatas);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!"".equals(otherParams)) {
            try {
                Map<String,Object> otherParamsMap = (Map<String,Object>)JSONObject.parseObject(otherParams, Map.class);
                entity.setOtherParams(otherParamsMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // otherParams	Json	非必填	其他参数，比如src （动作类型 save表示只保存 ，submit 为提交 ，默认为提交）
        return entity;
    }
}
