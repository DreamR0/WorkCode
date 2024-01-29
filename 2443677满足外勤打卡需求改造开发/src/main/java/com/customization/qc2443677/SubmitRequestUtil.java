package com.customization.qc2443677;

import com.engine.workflow.biz.publicApi.RequestOperateBiz;
import com.engine.workflow.biz.requestForm.RequestRemindBiz;
import com.engine.workflow.constant.PAResponseCode;
import com.engine.workflow.constant.ReqFlowFailMsgType;
import com.engine.workflow.entity.core.RequestInfoEntity;
import com.engine.workflow.entity.publicApi.PAResponseEntity;
import com.engine.workflow.entity.publicApi.ReqOperateRequestEntity;
import com.engine.workflow.entity.requestForm.ReqFlowFailMsgEntity;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.agent.AgentManager;

import java.util.Map;

public class SubmitRequestUtil {

    private static User user;
    private static ReqOperateRequestEntity requestParam;

    public SubmitRequestUtil(User user, ReqOperateRequestEntity requestParam) {
       this.user = user;
        this.requestParam = requestParam;
    }

    /**
     * 提交流程
     */
    public PAResponseEntity submitRequest(int requestId) {
       //设置新建流程的requestid
        requestParam.setRequestId(requestId);

        ReqFlowFailMsgEntity reqFailMsg = new ReqFlowFailMsgEntity();
        Map<String, Object> otherParams = requestParam.getOtherParams();
        PAResponseEntity pAEntity = RequestOperateBiz.verifyBefore(user, requestParam.getRequestId(), otherParams);
        pAEntity.setReqFailMsg(reqFailMsg);
        if (pAEntity.getCode() != null) return pAEntity;
        RequestInfoEntity reqInfo = RequestOperateBiz.initRequestInfo(requestParam, user);
        int isremark = reqInfo.getIsremark();
        String src = Util.null2String(otherParams.get("src"));
        if(src.equals("")){
            src = "submit";
        }

        //若有代理则收回代理
        if(src.equals("submit")) {
            int agentType = Util.getIntValue(Util.null2String(otherParams.get("agenttype")));
            int agentorByAgentId = Util.getIntValue(Util.null2String(otherParams.get("agentorbyagentid")));
            if (isremark == 2 && agentType == 1 && agentorByAgentId > 0) {//流程代理出去，本人提交，需先收回代理
                AgentManager agentManager = new AgentManager(user);
                agentManager.agentBackRequest(agentorByAgentId, user.getUID(), reqInfo.getWorkflowId() + "", requestParam.getRequestId());
                reqInfo = RequestOperateBiz.initRequestInfo(requestParam, user);
                isremark = reqInfo.getIsremark();
            }
        }
        if(src.equals("save")) {
            pAEntity = save(reqInfo,isremark,pAEntity);
            return pAEntity;
        }else if(src.equals("submit")){
            pAEntity = save(reqInfo,isremark,pAEntity);
            if(pAEntity.getErrMsg().size()>0){
                return pAEntity;
            }

            if("1".equals(Util.null2String(otherParams.get(RequestOperateBiz.OTHERPARAM_JUDGEFORMMUSTINPUT)))) {
                try {
                    if (!RequestOperateBiz.judgeFormMustInput(reqInfo, reqFailMsg)) {
                        pAEntity.setCode(PAResponseCode.FAIL);
                        reqFailMsg.setMsgType(ReqFlowFailMsgType.JUDGE_MUST_INPUT);
                        return pAEntity;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    pAEntity.setCode(PAResponseCode.SYSTEM_INNER_ERROR);
                    return pAEntity;
                }
            }

            reqInfo.setSrc("submit");
            boolean flowFlag = RequestOperateBiz.flowNextNode(reqInfo, user, requestParam,reqFailMsg);
            boolean doAutoApprove = "1".equals(Util.null2String(reqFailMsg.getOtherParams().get("doAutoApprove")));
            if(flowFlag && !doAutoApprove){
                long startRemindTime = System.currentTimeMillis();
                RequestRemindBiz remindBiz = new RequestRemindBiz(user);
                remindBiz.requestSubmitRemind4WebService(Util.getIntValue(reqInfo.getRequestId()), reqInfo.getWorkflowId(),-1,-1);
                long endRemindTime = System.currentTimeMillis();
            }
            pAEntity.setCode(flowFlag ? PAResponseCode.SUCCESS : PAResponseCode.SYSTEM_INNER_ERROR);
        }
        return pAEntity;
    }

    private PAResponseEntity save(RequestInfoEntity reqInfo, int isremark, PAResponseEntity pAEntity) {
        int takisremark = reqInfo.getTakisremark();
        Map <String, Object> otherParams = requestParam.getOtherParams();
        String src = Util.null2String(otherParams.get("src"));
        String takEnd = String.valueOf(otherParams.get("takEnd"));
        String agenttype = Util.null2String(otherParams.get("agenttype"));
        if(src.equals("save")&&agenttype.equals("1")){
        }else if (!(isremark == 0 || isremark == 1 || isremark == 8 || isremark == 9 || isremark == 11) || (isremark==0&&takisremark==-2&&!takEnd.equals("1"))
                || takEnd.equals("1")&&!(takisremark == -2 && isremark == 0)) {
            pAEntity.setCode(PAResponseCode.NO_PERMISSION);
            pAEntity.getErrMsg().put("isremark", isremark);
            pAEntity.getReqFailMsg().setMsgType(ReqFlowFailMsgType.NO_REQUEST_SUBMIT_PERMISSION);
            return pAEntity;
        }
        try {
            if (isremark == 0 && (takisremark == 0 || takisremark == -1) || agenttype.equals("1")&&isremark==2) {
                Map<String, Object> errMsg = RequestOperateBiz.saveRequestInfo(reqInfo, user, requestParam,pAEntity.getReqFailMsg());
                //保存流程参数异常
                if (errMsg.size() > 0) {
                    pAEntity.setCode(PAResponseCode.PARAM_ERROR);
                    pAEntity.setErrMsg(errMsg);
                    return pAEntity;
                }else {
                    pAEntity.setCode(PAResponseCode.SUCCESS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            pAEntity.setCode(PAResponseCode.SYSTEM_INNER_ERROR);
            pAEntity.getErrMsg().put("save_req_err", "" + weaver.systeminfo.SystemEnv.getHtmlLabelName(126222, weaver.general.ThreadVarLanguage.getLang()) + "");
            return pAEntity;
        }
        return pAEntity;
    }
}
