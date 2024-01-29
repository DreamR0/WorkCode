package com.api.customization.qc20230816.permission.util;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionItem;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.hrm.biz.HrmFieldManager;
import weaver.conn.RecordSet;
import weaver.docs.docs.CustomFieldManager;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.definedfield.HrmFieldGroupComInfo;
import weaver.hrm.resource.HrmListValidate;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.api.browser.util.ConditionType.INPUT;

public class CheckSetUtil {
    //公共查看表
    private String publicSelectTable = new BaseBean().getPropValue("qc20230816permission","publicSelectTable");
    //公共编辑表
    private String publicSetTable = new BaseBean().getPropValue("qc20230816permission","publicSetTable");
    //人员查看表
    private String resourceSelectTable = new BaseBean().getPropValue("qc20230816permission","resourceSelectTable");
    //人员编辑表
    private String resourceSetTable = new BaseBean().getPropValue("qc20230816permission","resourceSetTable");

    /**
     * 过滤基本信息编辑表单字段
     * @param resourceId    当前操作人员
     * @param searchConditionItem
     * @param tabType
     * @return
     */
    public void searchConditionItem(int resourceId,String fieldName,int tabType,SearchConditionItem searchConditionItem,int setUserId){
        try {
            if (!fieldSetCheck(resourceId,fieldName,tabType,setUserId)){
                Map<String,Object> otherParams = new HashMap<>();
                otherParams.put("hide",true);
                otherParams.put("inputType","multilang");
                otherParams.put("isBase64",true);
                searchConditionItem.setOtherParams(otherParams);
                searchConditionItem.setHide(true);

                //照片特殊字段处理
                if ("resourceimageid".equals(fieldName)){
                    searchConditionItem.setConditionType(INPUT);
                    searchConditionItem.setValue("0");
                    searchConditionItem.setLabel("");
                }

            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(过滤基本信息编辑表单字段报错)" + e);
        }
    }


    /**
     * 处理编辑字段权限
     * @param resourceId     操作人员id
     * @param fieldName     字段数据库名
     * @param setUserId     修改人员id
     * @param tabType       标签页
     * @return
     */
    public Boolean fieldSetCheck(int resourceId,String fieldName,int tabType,int setUserId){
        Boolean isHave = false;
        new BaseBean().writeLog("==zj==(修改人员是否为当前操作人员)" + resourceId + " | " + setUserId + "  | "  );
        //查看公共编辑表和人员编辑表
        for (int i = 0; i < 2; i++) {
            if (i == 0){
                //公共编辑表只限修改自己信息
                if (resourceId == setUserId){
                    isHave = publicFieldSet(fieldName,tabType);
                }
            }
            if (i == 1){
                isHave = FieldSet(resourceId,fieldName,tabType);
            }
            if (isHave){
                return isHave;
            }
        }

        return isHave;

    }


    /**
     * 公共字段编辑
     * @param fieldName 字段名
     * @return
     */
    public Boolean publicFieldSet(String fieldName,int tabType){
        Boolean isHave = false;
        String sql = "";
        RecordSet rs = new RecordSet();

        sql = "select * from "+publicSetTable+" where instr(',' || zd || ',', ',' || '"+fieldName+"'  || ',')>0 and ssbqy="+tabType;
        rs.executeQuery(sql);
        if (rs.next()){
            isHave = true;
        }

        return isHave;
    }

    /**
     * 编辑权限赋予
     * @param resourceId    人员id
     * @param fieldName    字段名
     * @return
     */
    public Boolean FieldSet(int resourceId,String fieldName,int tabType){
        Boolean isHave = false;
        String sql = "";
        RecordSet rs = new RecordSet();

        sql = "select * from "+resourceSetTable+" where instr(',' || ryxxzd || ',', ',' || '"+fieldName+"' || ',')>0 and "+
                "instr(',' || ry || ',', ',' ||"+resourceId+"  || ',')>0 and ssbqy="+tabType;
        rs.executeQuery(sql);
        if (rs.next()){
            isHave = true;
        }
        return isHave;
    }

    /**
     * 编辑权限赋予(导入)
     * @param resourceId    人员id
     * @return
     */
    public List<String> uploadCheck(String resourceId){
       List<String> uploadList = new ArrayList<>();
        String sql = "";
        String key = "";
        RecordSet rs = new RecordSet();

        try {
            //获取人员编辑权限表
            sql = "select * from "+resourceSetTable+" where "+
                    "instr(',' || ry || ',', ',' ||"+resourceId+"  || ',')>0";
            new BaseBean().writeLog("==zj==(导入人员编辑权限sql)" + sql);
            rs.executeQuery(sql);
            while (rs.next()){
                key += Util.null2String(rs.getString("ryxxzd"))+",";
            }
            //获取个人编辑权限表
            sql =  "select * from "+resourceSetTable;
            new BaseBean().writeLog("==zj==(导入公共编辑sql)" + sql);
            rs.executeQuery(sql);
            while (rs.next()){
                key += Util.null2String(rs.getString("zd"))+",";
            }
            new BaseBean().writeLog("==zj==(key)" + JSON.toJSONString(key));

            //封装权限key
            if (!"".equals(key)){
                key = key.substring(0,key.length()-1);
                String[] keys = key.split(",");
                for (int i = 0; i < keys.length; i++) {
                    uploadList.add(keys[i]);
                }
            }
            new BaseBean().writeLog("==zj==(uploadList)" + JSON.toJSONString(uploadList));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadList;

    }

    /**
     * 这里用来判断是否显示个人信息编辑按钮
     */
    public Boolean isSetButtonPerson(HttpServletRequest request, HttpServletResponse response, User user) {

        String id = Util.null2String(request.getParameter("id"));
        int setUserId = Util.getIntValue(id);
        int scopeId = 1;
        int viewAttr = 1;
        Boolean isHave = false;
        HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
        CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
        if (viewAttr != 1) hfm.isReturnDecryptData(true);
        hfm.getHrmData(Util.getIntValue(id));
        cfm.getCustomData(Util.getIntValue(id));
        CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        while (HrmFieldGroupComInfo.next()) {
            int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
            if (grouptype != scopeId) continue;
            int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
            int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
            hfm.getCustomFields(groupid);


            while (hfm.next()) {
                int tmpviewattr = viewAttr;
                String fieldName = hfm.getFieldname();
                Boolean flag = fieldSetCheck(user.getUID(), fieldName, 1, setUserId);
                if (flag){
                    isHave = true;
                    break;
                }
                new BaseBean().writeLog("==zj==(个人信息字段查询)" + JSON.toJSONString(fieldName));
            }
        }
        return isHave;
    }

    /**
     * 这里用来判断是否显示工作信息编辑按钮
     */
    public Boolean isSetButtonWork(HttpServletRequest request, HttpServletResponse response, User user) {

        String id = Util.null2String(request.getParameter("id"));
        int setUserId = Util.getIntValue(id);
        int scopeId = 3;
        int viewAttr = 1;
        Boolean isHave = false;
        HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
        CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
        if (viewAttr != 1) hfm.isReturnDecryptData(true);
        hfm.getHrmData(Util.getIntValue(id));
        cfm.getCustomData(Util.getIntValue(id));
        CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        while (HrmFieldGroupComInfo.next()) {
            int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
            if (grouptype != scopeId) continue;
            int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
            int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
            hfm.getCustomFields(groupid);


            while (hfm.next()) {
                int tmpviewattr = viewAttr;
                String fieldName = hfm.getFieldname();
                Boolean flag = fieldSetCheck(user.getUID(), fieldName, 2, setUserId);
                if (flag){
                    isHave = true;
                    break;
                }
                new BaseBean().writeLog("==zj==(工作信息字段查询)" + JSON.toJSONString(fieldName));
            }
        }
        return isHave;
    }
    /**
     * 这里用来判断是否显示基本信息编辑按钮
     */
    public Boolean isSetButtonBasic(HttpServletRequest request, HttpServletResponse response, User user) {

        String id = Util.null2String(request.getParameter("id"));
        int setUserId = Util.getIntValue(id);
        int scopeId = -1;
        int viewAttr = 1;
        Boolean isHave = false;
        HrmFieldGroupComInfo HrmFieldGroupComInfo = new HrmFieldGroupComInfo();
        HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
        HrmFieldManager hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
        CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
        if (viewAttr != 1) hfm.isReturnDecryptData(true);
        hfm.getHrmData(Util.getIntValue(id));
        cfm.getCustomData(Util.getIntValue(id));
        CheckSelectUtil checkSelectUtil = new CheckSelectUtil();
        CheckSetUtil checkSetUtil = new CheckSetUtil();
        while (HrmFieldGroupComInfo.next()) {
            int grouptype = Util.getIntValue(HrmFieldGroupComInfo.getType());
            if (grouptype != scopeId) continue;
            int grouplabel = Util.getIntValue(HrmFieldGroupComInfo.getLabel());
            int groupid = Util.getIntValue(HrmFieldGroupComInfo.getid());
            hfm.getCustomFields(groupid);


            while (hfm.next()) {
                int tmpviewattr = viewAttr;
                String fieldName = hfm.getFieldname();
                Boolean flag = fieldSetCheck(user.getUID(), fieldName, 0, setUserId);
                if (flag){
                    isHave = true;
                    break;
                }
                new BaseBean().writeLog("==zj==(基本信息字段查询)" + JSON.toJSONString(fieldName));
            }
        }
        return isHave;
    }

    /**
     * 查看该人员是否有系统信息维护权限
     * @return
     */
    public Boolean isSystemRight(User user){
        Boolean isHave = false;
        RecordSet rs = new RecordSet();
        int userId = user.getUID();
        String sql = "select * from uf_ryxxxtxxb where "+
                "instr(',' || ckry || ',', ',' || "+userId+" || ',')>0";
        new BaseBean().writeLog("==zj==(系统信息sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            isHave = true;
        }
        return isHave;

    }


}
