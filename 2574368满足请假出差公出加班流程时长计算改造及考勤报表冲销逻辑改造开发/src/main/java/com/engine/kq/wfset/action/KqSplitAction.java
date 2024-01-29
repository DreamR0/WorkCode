package com.engine.kq.wfset.action;

import com.engine.kq.biz.KQFlowActiontBiz;
import com.engine.kq.log.KQLog;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;
import weaver.workflow.workflow.WorkflowComInfo;

/**
 * 考勤流程数据拆分action
 */
public class KqSplitAction extends BaseBean implements Action {
  private KQLog kqLog = new KQLog();

  @Override
  public String execute(RequestInfo request) {
    this.writeLog("KqSplitAction", "do action on request:" + request.getRequestid());
    String requestid = request.getRequestid();
    kqLog.info("do KqSplitAction on requestid:"+requestid);
    int requestidInt = Util.getIntValue(requestid, 0);

    String workflowid = request.getWorkflowid();
    String formid = new WorkflowComInfo().getFormId(workflowid);

    try {
      KQFlowActiontBiz kqFlowActiontBiz = new KQFlowActiontBiz();
      RecordSet rs = new RecordSet();
      String proc_set_sql = "select * from kq_att_proc_set where field001 = ? and field002 = ? ";
      rs.executeQuery(proc_set_sql, workflowid,formid);
      if(rs.next()){
        String proc_set_id = rs.getString("id");
        //得到这个考勤流程设置是否使用明细
        String usedetails = rs.getString("usedetail");

        int kqtype = Util.getIntValue(rs.getString("field006"));
        kqLog.info("do action on kqtype:" + kqtype+":requestidInt:"+requestidInt);
        Map<String, String> map = new HashMap<String, String>();
        if(requestidInt > 0){
          map.put("requestId", "and t.requestId = " + requestidInt);
        }

        Map<String,String> result = kqFlowActiontBiz.handleKQFlowAction(proc_set_id, usedetails, requestidInt, kqtype, Util.getIntValue(workflowid), false,false,map);

        if(!result.isEmpty()){
          String error = Util.null2String(result.get("message"));
          request.getRequestManager().setMessageid("666" + request.getRequestid() + "999");
          request.getRequestManager().setMessagecontent(error);
          return Action.FAILURE_AND_CONTINUE;
        }
      }
    } catch (Exception e) {
      kqLog.info("流程数据报错:KqSplitAction:");
      StringWriter errorsWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(errorsWriter));
      kqLog.info(errorsWriter.toString());
      request.getRequestManager().setMessageid("11111" + request.getRequestid() + "22222");
      request.getRequestManager().setMessagecontent("【考勤报表统计action】报错，请联系管理员！");
      return Action.FAILURE_AND_CONTINUE;
    }

    return Action.SUCCESS;

  }

}
