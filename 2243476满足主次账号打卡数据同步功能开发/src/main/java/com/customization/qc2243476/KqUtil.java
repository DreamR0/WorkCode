package com.customization.qc2243476;

import com.alibaba.fastjson.JSON;
import com.api.integration.Base;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.WorkTimeEntity;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KqUtil {

    /**
     * 判断子账号是否有生效考勤组
     * @param userId
     * @param today
     * @return
     */
    public List<Map> childAccountCheck(String userId, String today){
        RecordSet rs = new RecordSet();
        ArrayList<Map> childList = new ArrayList<>();
        String sql = "";
        try{
             //查询子账号以及考勤组
            sql="select * from hrmresource where belongto = " + userId;
             rs.executeQuery(sql);
             while (rs.next()){
                  userId = Util.null2String(rs.getString("id"));
                 String groupId =Util.null2String( new KQWorkTime().getWorkTime(userId, today).getGroupId());
                 new BaseBean().writeLog("==zj==(子账号考勤组id)" + groupId);
                 if (!"".equals(groupId)){
                     Map<String,String> result = new HashMap<String,String>();
                     result.put("userid",userId);
                     result.put("groupid",groupId);
                     new BaseBean().writeLog("==zj==(获取生效考勤组id和userid)" + JSON.toJSONString(result));
                     childList.add(result);
                 }
             }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==判断主账号是否有生效考勤组错误" + e);
        }
        return childList;

    }

    /**
     * 判断主账号是否有生效考勤组
     * @param today
     * @param userId
     * @return
     */
    public Boolean mainAccountCheck(String userId,String today){
        Boolean flag = false;


        WorkTimeEntity workTimeEntity= new KQWorkTime().getWorkTime(userId, today);
        String groupId = Util.null2String(workTimeEntity.getGroupId());
        new BaseBean().writeLog("==zj==(判断主账号考勤组)" + groupId);
        if (!"".equals(groupId)){
            //如果groupId有值，说明当前主账号有生效考勤组
            flag = true;
        }

        return flag;
    }

    /**
     * 判断流程类型是否是考勤流程
     * @param workFlowId
     * @return
     */
    public Boolean checkKqFlow(String workFlowId){
        RecordSet rs = new RecordSet();
        String sqlKqFlow = "select * from kq_ATT_PROC_SET where field001 ="+workFlowId;
        rs.executeQuery(sqlKqFlow);
        if (rs.next()){
           return true;
        }
        return false;
    }

    /**
     *
     * @param requestid
     * @return
     */
    public Boolean checkKqFlowReq(String requestid){
        RecordSet rs = new RecordSet();
        String workflowid = "";
        Boolean result = false;
        String sql = "select * from workflow_requestbase where requestid = "+requestid;
        rs.executeQuery(sql);
        if (rs.next()){
            workflowid = Util.null2String(rs.getString("workflowid"));
        }
        if (!"".equals(workflowid)){
            sql = "select * from kq_ATT_PROC_SET where field001 = " + workflowid;
            rs.executeQuery(sql);
            if (rs.next()){
                result = true;
            }
        }

        return result;
    }

    /**
     * 判断查询用户是否为当前用户的子账号
     * @param resourceid
     * @return
     */
    public Boolean isChildAccount(int userid,String resourceid){
        RecordSet rs = new RecordSet();
        String userId = String.valueOf(userid);
        String sqlChildAccount="select * from hrmresource where id = "+resourceid;
        new BaseBean().writeLog("==zj==(判断查询账号是否为当前账号的子账号)" + sqlChildAccount);
        rs.executeQuery(sqlChildAccount);
        if (rs.next()){
            String belongTo = Util.null2String(rs.getString("belongto"));
            new BaseBean().writeLog("==zj==(userId-belongto)" + userId + " | " + belongTo);
            if (userId.equals(belongTo)){
                //如果该查询账号为当前账号的子账号
                return true;
            }

        }
        return false;
    }

    /**
     * 获取考勤汇总报表账号的子账号id
     * @param userid
     * @return
     */
    public String getChildKq(int userid){
        RecordSet rs = new RecordSet();
        String childId = "";
        String sqlKqChile = "select * from hrmresource where belongto = "+userid;
        new BaseBean().writeLog("==zj==(获取子账号sql)" +sqlKqChile);
        rs.executeQuery(sqlKqChile);
        while (rs.next()){
           childId+=Util.null2String(rs.getString("id"))+",";
        }
        //将字符串末尾的逗号去掉
        new BaseBean().writeLog("==zj==(获取子账号)" + childId);
        if (!"".equals(childId)){
            int lastIndex = childId.lastIndexOf(",");
            childId = childId.substring(0,lastIndex);
        }
        return childId;
    }

    /**
     * 原始打卡记录表搜索框显示
     * @param userid
     * @param replaceDatas
     * @return
     */
    public List getChildKq(int userid, List<Map<String, Object>> replaceDatas){
        RecordSet rs = new RecordSet();
        String sqlKqChile = "select * from hrmresource where belongto = "+userid;
        rs.executeQuery(sqlKqChile);
        while (rs.next()){
            Map<String,Object> result = new HashMap<>();
            result.put("name",Util.null2String(rs.getString("lastname")));
            result.put("id",Util.null2String(rs.getString("id")));
            replaceDatas.add(result);
        }

        new BaseBean().writeLog("==zj==(原始打卡记录表搜索框显示)" + JSON.toJSONString(replaceDatas));
        return replaceDatas;
    }

    /**
     * 判断当前账号考勤组情况
     * @param userid
     * @return
     */
    public Map<String,Object> isMuchGroup(int userid,String workflowid){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String,Object> result=new HashMap<>();
        String todays = "";
        String userId = "";

        try{
             todays = sdf.format(System.currentTimeMillis());   //获取日期
             userId = Util.null2String(userid);                 //获取当前userId

            if (!checkKqFlow(workflowid)){
                //如果不来自考勤流程
                result.put("tips","该流程不是来自考勤流程");
                result.put("code","1");
                return result;
            }

             if (mainAccountCheck(userId,todays)){
                 //如果主账号有考勤组
                 result.put("tips","该主账号有生效考勤组");
                 result.put("code","1");
                 return result;
             }else {
                 List<Map> resultList = childAccountCheck(userId, todays);
                 if (resultList.size() > 1){
                     //说明子账号下多个考勤组
                     RecordSet rs = new RecordSet();
                     String tips = "";
                     String sql = "select * from "+new BaseBean().getPropValue("qc2280379","tipsTableName")+" where "+new BaseBean().getPropValue("qc2280379","tipsFrom")+"= '考勤流程'";
                     new BaseBean().writeLog("==zj==(考勤打卡提示信息)" + sql);
                     rs.executeQuery(sql);
                     if (rs.next()){
                         tips = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2280379","tipsMessage")));
                     }

                     result.put("tips","该主账号没有考勤组，且多个次账号有考勤组");
                     result.put("code","1");
                     result.put("message",tips);
                 }else {
                     result.put("tips","该主账号没有考勤组，且只有一个次账号有考勤组");
                     result.put("code","1");
                 }
             }
        }catch (Exception e){
            new BaseBean().writeLog("==zj获取考勤组情况报错信息=="+e);
        }

        return result;
    }
}
