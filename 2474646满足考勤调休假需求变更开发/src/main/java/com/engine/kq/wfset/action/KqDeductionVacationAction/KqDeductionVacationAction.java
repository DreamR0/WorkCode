package com.engine.kq.wfset.action.KqDeductionVacationAction;

import com.engine.kq.biz.KQAttProcSetComInfo;
import com.engine.kq.biz.KQFlowActiontBiz;
import com.engine.kq.enums.KqSplitFlowTypeEnum;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import com.engine.kq.wfset.util.KQFlowLeaveUtil;
import com.engine.kq.wfset.util.SplitActionUtil;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;
import weaver.interfaces.workflow.action.Action;
import weaver.interfaces.workflow.action.RollBackAction;
import weaver.soa.workflow.request.RequestInfo;
import weaver.workflow.workflow.WorkflowComInfo;

/**
 * 兼容E8的请假扣减action
 */
public class KqDeductionVacationAction extends BaseBean implements Action, RollBackAction {
  private KQLog kqLog = new KQLog();

  @Override
  public String execute(RequestInfo request) {
    //e8 的状态标识
    int DEDUCTION = 0;
    int FREEZE = 1;
    int RELEASE = 2;
    this.writeLog("KqDeductionVacationAction", "do action on request:" + request.getRequestid());
    String requestid = request.getRequestid();
    kqLog.info("do KqDeductionVacationAction on requestid:"+requestid);
    int requestidInt = Util.getIntValue(requestid, 0);

    String workflowid = request.getWorkflowid();
    String formid = new WorkflowComInfo().getFormId(workflowid);
    KQAttProcSetComInfo kqAttProcSetComInfo = new KQAttProcSetComInfo();
    int cur_kqtype = Util.getIntValue(kqAttProcSetComInfo.getkqType(workflowid),-1);

    if(cur_kqtype != KqSplitFlowTypeEnum.LEAVE.getFlowtype()){
      return Action.SUCCESS;
    }

    try {

      KQFlowLeaveUtil kqFlowLeaveUtil = new KQFlowLeaveUtil();
      KQFlowActiontBiz kqFlowActiontBiz = new KQFlowActiontBiz();
      ResourceComInfo rci = new ResourceComInfo();

      List<SplitBean> splitBeans = new ArrayList<SplitBean>();
      DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      RecordSet rs = new RecordSet();
      String proc_set_sql = "select * from kq_att_proc_set where field001 = ? and field002 = ? ";
      rs.executeQuery(proc_set_sql, workflowid,formid);
      if(rs.next()){
        String proc_set_id = rs.getString("id");
        //得到这个考勤流程设置是否使用明细
        String usedetails = rs.getString("usedetail");
        int kqtype = Util.getIntValue(rs.getString("field006"));
        Map<String, String> map = new HashMap<>();
        if(requestidInt > 0){
          map.put("requestId", "and t.requestId = " + requestidInt);
        }

        Map<String, String> sqlMap = kqFlowActiontBiz.handleSql(proc_set_id, usedetails, requestidInt,kqtype,map);
        Map<String,String> result = kqFlowLeaveUtil.handleKQLeaveAction(sqlMap, splitBeans, datetimeFormatter, Util.getIntValue(workflowid), requestidInt, rci);
        if(!result.isEmpty()){
          String error = Util.null2String(result.get("message"));
          request.getRequestManager().setMessageid("666" + request.getRequestid() + "999");
          request.getRequestManager().setMessagecontent(error);
          return Action.FAILURE_AND_CONTINUE;
        }

      }

      //先在这里执行扣减动作
      SplitActionUtil.handleLeaveAction(splitBeans,requestid);
      //然后再把扣减的了数据更新下KQ_ATT_VACATION表
      String updateFreezeSql = "update KQ_ATT_VACATION set status=? where requestId=? and workflowId = ? ";
      boolean isUpdate = rs.executeUpdate(updateFreezeSql,DEDUCTION,requestid,workflowid);
      if(!isUpdate){
        request.getRequestManager().setMessageid("666" + request.getRequestid() + "999");
        request.getRequestManager().setMessagecontent(""+weaver.systeminfo.SystemEnv.getHtmlLabelName(82823,weaver.general.ThreadVarLanguage.getLang())+"action"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005361,weaver.general.ThreadVarLanguage.getLang())+""+updateFreezeSql+"("+DEDUCTION+","+requestid+","+workflowid+")");
        return Action.FAILURE_AND_CONTINUE;
      }
    } catch (Exception e) {
      writeLog(e);
      request.getRequestManager().setMessageid("11111" + request.getRequestid() + "22222");
      request.getRequestManager().setMessagecontent("请假流程【扣减action】报错，请联系管理员！");
      return Action.FAILURE_AND_CONTINUE;
    }

    return Action.SUCCESS;

  }

  @Override
  public String executeRollBack(RequestInfo request) {
    return null;
  }
}
