package com.api.customization.qc2607932.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.hrm.User;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {
    private String departmentLevel = new BaseBean().getPropValue("qc2607932","departmentlevel");    //获取查看分部及下级分部安全级别

    /**
     * 获取当前人员分部以及所有下级分部
     * @param user
     * @return
     */
    public String getDepartmentChilds(User user){
        String sql = "";
        String userLevel = "";  //当前用户安全级别
        int userLevelD = 0;
        int departmentLevelD = 0;
        int subCompanyId = 0;
        String departmentChilds = "";
        RecordSet rs = new RecordSet();


        try {
             userLevel = user.getSeclevel();
             //这里把规定安全值拆分下
            String[] departmentLevels = departmentLevel.split(",");
            new BaseBean().writeLog("==zj==(departmentLevels)" + JSON.toJSONString(departmentLevels));
            //当安全级别大于规定值，才能查看本部门和下级部门
            if (!"".equals(userLevel)){
                userLevelD = Integer.parseInt(userLevel);
            }

            for (int i = 0; i < departmentLevels.length; i++) {

                departmentLevelD = Integer.parseInt(departmentLevels[i]);//获取配置安全值
                new BaseBean().writeLog("==zj==(安全值检查)" + userLevelD  +  " | " + departmentLevelD);
                //只要当前用户安全值满足任意一个规定值就能允许浏览
             if (userLevelD == departmentLevelD){
                 subCompanyId = user.getUserSubCompany1();    //获取当前人员的分部id
                 sql = "SELECT id, supsubcomid FROM hrmsubcompany START WITH id = "+subCompanyId+" CONNECT BY PRIOR id = supsubcomid";
                 new BaseBean().writeLog("==zj==(下属考勤浏览按钮sql)" + JSON.toJSONString(sql));
                 rs.executeQuery(sql);
                 while (rs.next()){

                     departmentChilds += rs.getString("id")+",";
                 }
                 if (departmentChilds.length() > 0){
                    departmentChilds = departmentChilds.substring(0,departmentChilds.length()-1);
                    return departmentChilds;
                 }
             }

            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(获取所有下级部门报错)" + e);
        }

        return "";
    }
}
