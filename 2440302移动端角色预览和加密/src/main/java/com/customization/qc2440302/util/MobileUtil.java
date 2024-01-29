package com.customization.qc2440302.util;

import cn.net.drm.edi.client.DrmAgentInf;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

public class MobileUtil {

    /**
     * 用来查询是否是指定pdf角色
     * @param resourceId
     * @return
     */
    public Boolean PDFReadLine(int resourceId){
        Boolean isReadLine = false;
        RecordSet rs = new RecordSet();
        String mobileTableName = new BaseBean().getPropValue("qc2440302","mobileTableName");
        String mobileRoleName = new BaseBean().getPropValue("qc2440302","mobileRoleName");

        try{
           String sqlBase = "select * from hrmroles a left join hrmrolemembers b on a.id = b.roleid";
           String sqlWhere = "";
               sqlWhere =" where b.resourceid ="+resourceId+" and a.rolesmark=(select "+mobileRoleName+" from "+mobileTableName+")";
           String sql = sqlBase + sqlWhere;
           new BaseBean().writeLog("==zj==(查询移动端角色sql)" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                isReadLine = true;
            }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(pdf移动端预览报错)" +e);
        }
        return isReadLine;
    }

}
