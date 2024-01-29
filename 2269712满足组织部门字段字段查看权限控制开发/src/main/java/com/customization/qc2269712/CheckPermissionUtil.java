package com.customization.qc2269712;

import org.apache.xpath.operations.Bool;
import org.docx4j.wml.U;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.HashMap;
import java.util.Map;

public class CheckPermissionUtil {

    /**
     * 判断该角色是否是总部角色
     * @param user
     * @return
     */
    public Boolean checkHead(User user){
        int userId = 0;
        int roleId = 0;
        String sql = "";
        Boolean isMainRole = false;
        RecordSet rs = new RecordSet();
        try{
            //先拿到总部角色的角色id
            userId  = user.getUID();
            sql = "select * from hrmroles where rolesmark = '" + new BaseBean().getPropValue("qc2269712","mainRoleName")+"'";
            new BaseBean().writeLog("==zj==(获取总部角色sql1)" + sql);
            rs.executeQuery(sql);
           if (rs.next()){
               roleId = Util.getIntValue(rs.getString("id"));
           }else {
               return isMainRole;
           }

           //在hrmrolemembers该人员是否为总部角色
            sql = "select * from hrmrolemembers where roleid="+roleId+" and resourceid="+userId;
            new BaseBean().writeLog("==zj==(该人员是否是总部角色)" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                isMainRole =true;
            }else {
                return isMainRole;
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(判断总部角色报错信息)" +e);
        }

        return isMainRole;
    }

    /**
     * 如果该部门有设置spa区域则判断该角色是否具有权限
     * @param user
     * @param departmentId
     * @return
     */
    public Boolean checkDepartment(User user,int departmentId){
        RecordSet rs = new RecordSet();
        String sapValue = "";       //默认spy区域值      0--新加坡  1--中国
        int userId = user.getUID();     //获取用户id
        int roleId = 0;
        String definedName =  new BaseBean().getPropValue("qc2269712","sapdefined");         //获取部门自定义数据库字段名
        String sql = "";
        try{
            //先查该部门的spa字段有没有设置值，如果没有就按系统标准走
            sql = "select "+definedName+" from hrmdepartmentdefined where deptid = " + departmentId;
            new BaseBean().writeLog("==zj==(部门获取sap区域值)" + sql);
            rs.executeQuery(sql);
            if (rs.next()){
                sapValue = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2269712","sapdefined")));
            }else {
                return false;
            }
            //如果该字段有值，继续判断该部门是哪个区域
            if (new BaseBean().getPropValue("qc2269712","singapore").equals(sapValue)){
                //这是新加坡区域,先查询是否有新加坡角色
                sql = "select * from hrmroles where rolesmark = '" + new BaseBean().getPropValue("qc2269712","singaporeName")+"'";
                new BaseBean().writeLog("==zj==(新加坡区域查询是否有中国角色)" + sql);
                rs.executeQuery(sql);
                if (rs.next()){
                    roleId = Util.getIntValue(rs.getString("id"));
                }else {
                    return false;
                }
                //再查询该角色下是否有该用户
                sql = "select * from hrmrolemembers where roleid="+roleId+" and resourceid="+userId;
                new BaseBean().writeLog("==zj==(该人员是否是新加坡角色)" + sql);
                rs.executeQuery(sql);
                if (rs.next()){
                   return true;
                }else {
                   return false;
                }
            }

            if (new BaseBean().getPropValue("qc2269712","china").equals(sapValue)){
                //这是中国区域,先查询是否有新加坡角色
                sql = "select * from hrmroles where rolesmark = '" + new BaseBean().getPropValue("qc2269712","chinaName")+"'";
                new BaseBean().writeLog("==zj==(中国区域查询是否有中国角色)" + sql);
                rs.executeQuery(sql);
                if (rs.next()){
                    roleId = Util.getIntValue(rs.getString("id"));
                }else {
                    return false;
                }
                //再查询该角色下是否有该用户
                sql = "select * from hrmrolemembers where roleid="+roleId+" and resourceid="+userId;
                new BaseBean().writeLog("==zj==(该人员是否是中国角色)" + sql);
                rs.executeQuery(sql);
                if (rs.next()){
                    return true;
                }else {
                    return false;
                }
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(spa部门保存信息)" +e);
        }

        return false;
    }

    /**
     * 这里判断是不是中国和新加坡两个特定角色
     * @param user
     * @return
     */
    public Boolean checkRole(User user){
        RecordSet rs = new RecordSet();
        int userId = user.getUID();
        boolean isCheckRole = false;
        String sqlSelect = "select RESOURCEID from hrmroles a left join hrmrolemembers b  on a.id = b.ROLEID";
        String sqlWhere =" where a.rolesmark='"+new BaseBean().getPropValue("qc2269712","chinaName")+"'" +
                " or a.rolesmark='"+new BaseBean().getPropValue("qc2269712","singaporeName")+"'";
        String sql = sqlSelect + sqlWhere;
        new BaseBean().writeLog("==zj==(判断特定角色)" + sql);
        rs.executeQuery(sql);
        while (rs.next()){
            int resourceId = Util.getIntValue(rs.getString("resourceid"));
            new BaseBean().writeLog("==zj==(该用户是否是特定角色)" + userId + " | " + resourceId);
            if (userId == resourceId){
                isCheckRole = true;
            }
        }

        return isCheckRole;
    }
}
