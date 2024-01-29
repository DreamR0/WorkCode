package com.customization.SMSapproval.Action;

import com.engine.core.interceptor.CommandContext;
import com.engine.workflow.biz.freeNode.FreeNodeBiz;
import com.engine.workflow.biz.publicApi.RequestOperateBiz;
import com.engine.workflow.biz.requestForm.RequestRemindBiz;
import com.engine.workflow.biz.workflowCore.WorkflowBaseBiz;
import com.engine.workflow.cmd.publicApi.PublicApiCommonCommand;
import com.engine.workflow.constant.PAResponseCode;
import com.engine.workflow.constant.ReqFlowFailMsgType;
import com.engine.workflow.entity.core.NodeInfoEntity;
import com.engine.workflow.entity.core.RequestInfoEntity;
import com.engine.workflow.entity.publicApi.PAResponseEntity;
import com.engine.workflow.entity.publicApi.ReqOperateRequestEntity;
import com.engine.workflow.entity.requestForm.ReqFlowFailMsgEntity;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.integration.entrance.utils.StringUtils;
import weaver.workflow.agent.AgentManager;
import weaver.workflow.request.RequestRejectManager;

import java.util.ArrayList;
import java.util.Map;

/**
 * description : 流程退回
 * author ：JHY
 * date : 2020/6/4
 * version : 1.0
 */
public class DoRejectRequestCmd extends PublicApiCommonCommand<PAResponseEntity> {

    public DoRejectRequestCmd(User user, ReqOperateRequestEntity requestParam) {
        this.user = user;
        this.requestParam = requestParam;
    }

    @Override
    public PAResponseEntity execute(CommandContext commandContext) {
        Map<String, Object> otherParams = requestParam.getOtherParams();
        PAResponseEntity pAEntity = RequestOperateBiz.verifyBefore(user, requestParam.getRequestId(), otherParams);
        ReqFlowFailMsgEntity reqFailMsg = new ReqFlowFailMsgEntity();
        pAEntity.setReqFailMsg(reqFailMsg);
        if (pAEntity.getCode() != null) return pAEntity;
        RequestInfoEntity reqInfo = RequestOperateBiz.initRequestInfo(requestParam, user);
        NodeInfoEntity nodeInfoEntity = WorkflowBaseBiz.getNodeInfo(Util.getIntValue(reqInfo.getCurrentNodeId()));

        //若有代理则收回代理
        int agentType = Util.getIntValue(Util.null2String(otherParams.get("agenttype")));
        int agentorByAgentId = Util.getIntValue(Util.null2String(otherParams.get("agentorbyagentid")));
        if (reqInfo.getIsremark() == 2 && agentType == 1 && agentorByAgentId > 0) {//流程代理出去，本人提交，需先收回代理
            AgentManager agentManager = new AgentManager(user);
            agentManager.agentBackRequest(agentorByAgentId, user.getUID(), reqInfo.getWorkflowId() + "", requestParam.getRequestId());
            reqInfo = RequestOperateBiz.initRequestInfo(requestParam, user);
        }

        int isremark = reqInfo.getIsremark();
        int takisremark = reqInfo.getTakisremark();
        if (!((isremark == 0 && (takisremark == 0 || takisremark == -1)) && nodeInfoEntity.getNodetype() == 1)) {
            pAEntity.setCode(PAResponseCode.NO_PERMISSION);
            pAEntity.getErrMsg().put("isremark", isremark);
            pAEntity.getErrMsg().put("takisremark", takisremark);
            pAEntity.getErrMsg().put("nodeType", nodeInfoEntity.getNodetype());
            pAEntity.getReqFailMsg().setMsgType(ReqFlowFailMsgType.NO_REQUEST_SUBMIT_PERMISSION);
            return pAEntity;
        }

        //自由退回
        int rejectToNodeId = Util.getIntValue(Util.null2String(otherParams.get("RejectToNodeid")));
        if((rejectToNodeId > 0 || FreeNodeBiz.isFreeNode(rejectToNodeId))) {
            if(rejectToNodeId == Util.getIntValue(reqInfo.getCurrentNodeId())) {
                pAEntity.setCode(PAResponseCode.PARAM_ERROR);
                pAEntity.getErrMsg().put("RejectToNodeid",rejectToNodeId);
                return pAEntity;
            }

            if(!RequestOperateBiz.judgeReqNode(rejectToNodeId, reqInfo)) {
                pAEntity.setCode(PAResponseCode.PARAM_ERROR);
                pAEntity.getErrMsg().put("Invalid_parameter_RejectToNodeid",otherParams.get("RejectToNodeid"));
                return pAEntity;
            } else {
                ArrayList<String> rejectnodeids = new ArrayList<String>();
                RequestRejectManager rejectManager = new RequestRejectManager();
                ArrayList<String>[] nodelist = rejectManager.getPathWayNodes(reqInfo.getWorkflowId(), requestParam.getRequestId(), Util.getIntValue(reqInfo.getCurrentNodeId()));
                if (nodelist != null && nodelist.length == 3) {
                    rejectnodeids = nodelist[0];
                }
                if(!(rejectnodeids.size() > 0 && rejectnodeids.contains(String.valueOf(rejectToNodeId)))) {
                    pAEntity.setCode(PAResponseCode.PARAM_ERROR);
                    pAEntity.getErrMsg().put("Invalid_parameter_RejectToNodeid",otherParams.get("RejectToNodeid"));
                    pAEntity.getErrMsg().put("Invalid_parameter_rejectRanage", StringUtils.join(rejectnodeids,","));
                    return pAEntity;
                }
            }
        }

        reqInfo.setSrc("reject");
        boolean flowFlag = RequestOperateBiz.flowNextNode(reqInfo, user, requestParam,reqFailMsg);
        if (flowFlag){
            RequestRemindBiz remindBiz = new RequestRemindBiz(user);
            remindBiz.requestSubmitRemind4WebService(Util.getIntValue(reqInfo.getRequestId()), reqInfo.getWorkflowId(),-1,-1);
        }
        pAEntity.setCode(flowFlag ? PAResponseCode.SUCCESS : PAResponseCode.SYSTEM_INNER_ERROR);
        return pAEntity;
    }
}
