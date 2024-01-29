package com.api.customization.zj0601.util;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorSetUtil {

    /**
     * 设置流程标题颜色
     * @param viewcondition
     * @return
     */
    public String FlowTitleColor(String viewcondition,Boolean isFlowAll) {
        String style = "style='color: #000000'";
        String color = "";
        try{
            if (!isFlowAll){
                switch (viewcondition){

                    case "82":  //全部
                        color = "#008000";
                        break;
                    case "83":  //待处理
                        color = "#008000";
                        break;
                    case "84":  //未归档
                        color = "#0000FF";
                        break;
                    case "85":  //已归档
                        color = "#000000";
                        break;
                    case "86":  //未读
                        color = "#FFA500";
                        break;
                    case "87":  //退回
                        color = "#FF0000";
                        break;
                    case "88":  //我的关注
                        color = "#000000";
                        break;
                }
            }else {
                switch (viewcondition){
                    case "flowDoing":  //待处理
                        color = "#008000";
                        break;
                    case "flowUnFinish":  //未归档
                        color = "#0000FF";
                        break;
                    case "flwoFinish":  //已归档
                        color = "#000000";
                        break;
                    case "flowNew":  //未读
                        color = "#FFA500";
                        break;
                    case "flowReject":  //退回
                        color = "#FF0000";
                        break;
                    case "flowAttention":  //我的关注
                        color = "#000000";
                        break;
                }
            }

            style = "style='color: "+color+"'";
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(流程标题颜色)");
        }

        return style;
    }

    /**
     * 当tab页为全部时，设置不同状态的流程标题颜色
     * @param requestid
     * @return
     */
    public String setflowAllColor(String requestid){
        RecordSet rs = new RecordSet();
        String style = "";
        List<Map<String,String>> sqlList = workFlowSelect();    //获取每个流程状态的sql
        for (int i = 0; i < sqlList.size(); i++) {
            Map<String,String> result = sqlList.get(i);
            String sql = result.get("sql");
            new BaseBean().writeLog("==zj==(颜色sql)" + sql);
            new BaseBean().writeLog("==zj==(requestid)" + requestid);
            rs.executeQuery(sql,requestid);
            if (rs.next()){
              String key = result.get("key");
              style = FlowTitleColor(key,true);
              return style;
            }
        }
        return style;
    }


    /**
     * 查询流程状态
     * @return
     */
    public List<Map<String,String>> workFlowSelect(){
        RecordSet rs = new RecordSet();
        List<Map<String,String>> sqlList = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            Map<String,String> result = new HashMap();
            String key = "";
            String sql = "";
            switch (i){
                case 0: //退回
                    key="flowReject";
                    sql = "select * from workflow_currentoperator where requestid = ? and islasttimes=1 and isbereject = 1";
                    break;
                case 4: //待办
                    key="flowDoing";
                    sql = "select  * from workflow_requestbase t1,workflow_currentoperator t2 where t2.requestid = t1.requestid and t2.isremark not in('1','8','9','11')  and  t2.islasttimes=1 and t1.currentnodetype <> '3' and t1.requestid = ?";
                    break;
                case 2: //未归档
                    key="flowUnFinish";
                    sql = "select * from workflow_currentoperator t2 left join workflow_requestbase t1 on t1.requestid = t2.requestid where ((t2.isremark =2 or (t2.isremark=0 and t2.takisremark =-2)) and t1.currentnodetype<> '3') and t1.requestid=?";
                    break;
                case 3: //已归档
                    key="flwoFinish";
                    sql = "select * from workflow_currentoperator t2 left join workflow_requestbase t1 on t1.requestid = t2.requestid where t1.currentnodetype = '3' and t1.requestid =?";
                    break;
                case 1: //未读
                    key="flowNew";
                    sql = "select * from workflow_currentoperator t2 left join workflow_requestbase t1 on t1.requestid = t2.requestid where  (t2.viewtype = '0' and (t1.currentnodetype <> '3' or (t2.isremark in ('1', '8', '9','11') and t1.currentnodetype = '3')) and t2.viewtype=0 and t2.isremark != '5' and t2.isprocessed is null) and t2.requestid =? ";
                    break;
                case 5: //我的关注
                    key="flowAttention";
                    sql = " select * from workflow_attention where requestid= ?";
                    break;
            }

            result.put("key",key);
            result.put("sql",sql);
            sqlList.add(result);
        }

        return sqlList;

    }
}
