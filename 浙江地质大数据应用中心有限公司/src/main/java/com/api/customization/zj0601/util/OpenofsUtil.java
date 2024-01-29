package com.api.customization.zj0601.util;

import com.api.integration.Base;
import com.engine.workflow.util.WorkflowDimensionUtils;
import weaver.general.BaseBean;
import weaver.hrm.User;

public class OpenofsUtil {
    public String getopenofsDoingSql(String whereclause_os, String user_sqlstr, int viewcondition, String doingStatus, User user,int usertype){
           int viewcondition_temp = 0;
            whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 ";
            //==zj 这里先设置初始条件
            if (viewcondition == 83){
                //待处理
              viewcondition_temp = 5;
            }else if(viewcondition == 86){
                //未读
                viewcondition_temp = 1;
            }
            String customOsSql = WorkflowDimensionUtils.getOsSqlWhere(viewcondition_temp+"","and",user);
            new BaseBean().writeLog("==zj==(customOsSql)" +customOsSql);
            if (viewcondition == 82) {//全部
                //whereclause_os += " and userid=" + user.getUID() + " and islasttimes=1 and isremark='0' ";
            } else if (viewcondition == 86) {//未读
                whereclause_os += " and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark in(0,8,9) and viewtype=0 ";
            }else if(viewcondition == 88){ //我的关注
                whereclause_os += " and exists (SELECT 1 FROM workflow_attention att WHERE att.requestid=ofs_todo_data.requestid and att.userid=" + user.getUID() + " AND att.usertype=" + usertype + ") ";
            }else if (viewcondition == 83){//待处理

            }else if (viewcondition == 84){//未归档
                whereclause_os += "and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark='2' and iscomplete=0";
            }else if (viewcondition == 85) {//已归档
                whereclause_os += "and userid in(" + user_sqlstr + ") and islasttimes=1 and isremark in ('2','4') and iscomplete=1";
            }else if(viewcondition == 87){ //退回
            }
            else {
                if(!"".equals(customOsSql)){
                    whereclause_os += customOsSql;
                }else{
                    whereclause_os += " and 1=2 ";
                }
            }
            if("5".equals(doingStatus) || "6".equals(doingStatus) || "7".equals(doingStatus)){
                whereclause_os += " and 1=2 ";
            }
        return whereclause_os;
    }
}
