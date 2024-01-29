package com.customization.qc2213471.Action;


import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.MainTableInfo;
import weaver.soa.workflow.request.RequestInfo;

public class changeClassification implements Action {
    private String tablename;
    private String classification;
    private String userid;

    private String ResourceId;
    private String classIficationM;

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    @Override
    public String execute(RequestInfo requestInfo) {

        try{
            RecordSet rs = new RecordSet();
            String requestid = requestInfo.getRequestid();

            String sql = "select * from "+tablename+" where requestid="+requestid;
            new BaseBean().writeLog("==zj==(查询提交表单信息sql)" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                 ResourceId = Util.null2String(rs.getString(userid));
                 classIficationM = Util.null2String(rs.getString(classification));
            }

            sql = "update hrmresource set classification="+classIficationM +" where id="+ResourceId;
            new BaseBean().writeLog("==zj==(修改人员密级sql)"+sql);
            boolean updateStatus  =  rs.executeUpdate(sql);
            new BaseBean().writeLog("==zj==(更新状态)" + updateStatus);

            // 更新人员缓存
            ResourceComInfo resourceComInfo = null;
            try {
                if (updateStatus) {
                    resourceComInfo = new ResourceComInfo();
                    resourceComInfo.updateResourceInfoCache(ResourceId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(报错)" + e);
        }

        return null;
    }
}
