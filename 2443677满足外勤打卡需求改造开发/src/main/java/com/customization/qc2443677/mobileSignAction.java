package com.customization.qc2443677;

import com.engine.kq.biz.KQFormatBiz;
import com.engine.kq.wfset.util.SplitActionUtil;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

import javax.swing.*;

public class mobileSignAction  implements Action {


    @Override
    public String execute(RequestInfo requestInfo) {
        Boolean isSucess = false;//是否成功
        RecordSet rs = new RecordSet();
        String sql ="";
        String requestid = requestInfo.getRequestid();
        String tableName = new BaseBean().getPropValue("qc2443677", "tableName");//获取流程表单名称

        //要插入到签到表中的数据
        String userId = "";//用户id
        String userType = "";//用户类型
        String signType = "";//考勤类型
        String signDate = "";//考勤日期
        String signTime = "";//考勤时间
        String isincom = "";//是否是有效考勤打卡
        String signfrom = "";//考勤来源
        String addr = "";//考勤地址
        String belongDate = "";//考勤归属日期
        String remark = "";//备注
        try{
            //将本流程的数据插入到签到表中
            sql = "select * from "+tableName+" where requestid = '"+requestid+"'";
            rs.executeQuery(sql);
            if (rs.next()){
                userId = rs.getString("dkr");//用户id
                userType = rs.getString("yhlx");//用户类型
                signType = rs.getString("kqlx");//考勤类型
                signDate = rs.getString("dkrq");//考勤日期
                signTime = rs.getString("dksj");//考勤时间
                isincom = rs.getString("sfsyxkqdk");//是否是有效考勤打卡
                signfrom = rs.getString("kqly");//考勤来源
                addr = rs.getString("dkdd");//考勤地址
                belongDate = rs.getString("gsrq");//考勤归属日期
                remark = rs.getString("bz");//备注

            }
                String punchSql = "insert into HrmScheduleSign(userId,userType,signType,signDate,signTime,isInCom,belongdate,signfrom,addr) "+
                        " values(?,?,?,?,?,?,?,?,?)";
          isSucess = rs.executeUpdate(punchSql,userId,userType,signType,signDate,signTime,isincom,belongDate,signfrom,addr);
          //这里后面看看要不要插入到外勤签到表中
            if (isSucess){
                String mobile_sign_sql = "insert into mobile_sign(operater,operate_type,operate_date,operate_time,longitude,latitude,address,remark,attachment,crm,timezone) "
                        + " values(?,?,?,?,?,?,?,?,?,?,?) ";
               isSucess =  rs.executeUpdate(mobile_sign_sql, userId,signfrom,signDate,signTime,"0","0",addr,remark,"","","");
            }


            if (isSucess){
                boolean belongdateIsNull = belongDate.length()==0;
                //同步更新考勤数据到考勤报表
                if(belongdateIsNull){
                    //外勤签到没有归属日期，遇到跨天班次打卡可能归属前一天，需要格式化前一天考勤
                    new KQFormatBiz().formatDate(""+userId,DateUtil.getYesterday());
                }
                if(belongDate.length()==0){
                    //外勤签到没有归属日期，遇到跨天班次打卡可能归属前一天，需要格式化前一天考勤
                    new KQFormatBiz().formatDate(""+userId,DateUtil.getYesterday());
                }
                new KQFormatBiz().formatDate(""+userId,(belongDate.length() == 0 ? DateUtil.getCurrentDate() : belongDate));
                //外勤签到转的考勤 处理加班规则
                SplitActionUtil.pushOverTimeTasksAll(belongDate,belongDate,""+userId);
                return Action.SUCCESS;
            }else {
                return Action.FAILURE_AND_CONTINUE;
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(打卡流程归档插入)异常：" + e);
        }
        return null;
    }
}
