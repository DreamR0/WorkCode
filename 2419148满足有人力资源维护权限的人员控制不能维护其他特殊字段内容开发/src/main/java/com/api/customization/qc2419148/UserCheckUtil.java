package com.api.customization.qc2419148;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.hrm.bean.HrmFieldBean;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.Map;

public class UserCheckUtil {

    /**
     * 判断该用户是否具有修改权限
     * @param user
     * @return
     */
    public boolean getHasright( User user) {
        RecordSet rs = new RecordSet();
        Boolean result = false;
        String sql = "";
        try{
             sql =  "select a.* from hrmrolemembers a left join systemrightroles b on a.roleid = b.roleid left join SystemRights c on b.rightid = c.id"+
                     " where a.resourceid =  "+ user.getUID() + " and c.rightdesc = '人力资源维护(不包含手机和邮箱)'";
             new BaseBean().writeLog("==zj==(判断用户是否有修改人员卡片权限)" + sql);
             rs.executeQuery(sql);
             if (rs.next()){

                     result = true;
             }
        }catch (Exception e) {
            new BaseBean().writeLog("==zj==(判断用户是否有修改人员卡片权限报错)" + sql);
        }
        return result;
    }

    /**
     *判断字段是否需要禁止修改
     * @param hrmFieldBean
     * @return
     */
    public boolean viewAtrrSet(HrmFieldBean hrmFieldBean,User user){
        RecordSet rs = new RecordSet();
        Boolean result = false;
        String sql = "";
        String lableName = SystemEnv.getHtmlLabelName(Integer.parseInt(hrmFieldBean.getFieldlabel()), user.getLanguage());  //字段名
        try{
            String tableName = new BaseBean().getPropValue("qc2419148", "tableName");
            String fieldName = new BaseBean().getPropValue("qc2419148", "fieldName");
            sql = "select * from " + tableName+" where "+fieldName+" = '" + lableName+"'";
            rs.executeQuery(sql);
            if (rs.next()){
                result = true;
            }
            new BaseBean().writeLog("==zj==(判断字段是否需要禁止修改)" + sql);
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(权限修改报错)" +e);
        }
            return result;
    }

    /**
     * 流程手机号码校验
     * @param params
     * @return
     */
    public Map<String,Object> checkMobile( Map<String, Object> params){
        new BaseBean().writeLog("==zj==(手机流程校验params)" + JSON.toJSONString(params));
        RecordSet rs = new RecordSet();
        Map<String,Object> result;
        String workFlowName = "";       //流程名称
        String[] mobiles;             //手机号码
        try{
            workFlowName = Util.null2String(params.get("workflowName")); //获取流程名称
            mobiles = Util.null2String(params.get("mobile")).split(",");             //获取手机号码
            new BaseBean().writeLog("==zj==(手机号码mobiles)" + mobiles);
            if ("邮箱账号申请".equals(workFlowName)){
                for (int i = 0; i < mobiles.length; i++) {
                    String sql = "select * from hrmresource where mobile = '"+mobiles[i]+"'";
                    new BaseBean().writeLog("==zj==(流程手机号码校验sql)" + sql);
                    rs.executeQuery(sql);
                    if (rs.next()){
                        //说明手机号码已经存在
                        result = new HashMap<>();
                        result.put("code",-1);
                        result.put("msg","手机号码已经存在，请重新填写");
                        return result;
                    }
                }
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(流程手机号码校验报错)" +e);
        }

        result = new HashMap<>();
        result.put("code",1);
        result.put("msg","提交成功");
        return result;
    }

    /**
     * 先判断该角色是否同具有相同级别
     * @return
     */
    public Boolean getUserRightLevel(){

    }
}
