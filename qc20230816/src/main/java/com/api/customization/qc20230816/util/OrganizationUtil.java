package com.api.customization.qc20230816.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.join.hrm.in.HrmResource;

import java.util.HashMap;
import java.util.Map;

public class OrganizationUtil {


    /**
     * 检测当前该操作用户是否为系统管理角色
     * @return
     */
    public Map<String, Object> check(Map<String,Object> params , User user){
        String roleIds = "";
        String sql = "";
        RecordSet rs = new RecordSet();
        String adminRoleIds = new BaseBean().getPropValue("qc20230816","adminIds");

        //系统管理员
        try {
            if (1 == user.getUID()){
                Map<String,Object> result = new HashMap<>();
                result.put("isadmin","true");
                result.put("status","1");
                return  result;
            }

            sql = "select roleid from hrmrolemembers where resourceid = "+user.getUID();
            rs.executeQuery(sql);
            while (rs.next()){
                 roleIds +=Util.null2String(rs.getString("roleid"))+",";
            }
            if (roleIds.length() > 0){
                roleIds = roleIds.substring(0,roleIds.length()-1);
            }
            new BaseBean().writeLog("==zj==(是否为管理员角色)" + adminRoleIds + " | " + roleIds);
            if (roleIds.contains(adminRoleIds)){
                Map<String,Object> result = new HashMap<>();
                result.put("isadmin","true");
                result.put("status","1");
                return  result;
            }

        } catch (Exception e) {
            new BaseBean().writeLog("==zj==" + JSON.toJSONString(e));
        }

        Map<String,Object> result = new HashMap<>();
        result.put("isadmin","false");
        result.put("status","1");
        return  result;
    }
}
