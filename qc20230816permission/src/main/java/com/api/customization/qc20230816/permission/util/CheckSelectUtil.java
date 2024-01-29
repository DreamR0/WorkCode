package com.api.customization.qc20230816.permission.util;

import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.hrm.definedfield.HrmFieldManager;

import java.util.ArrayList;

public class CheckSelectUtil {
    //公共查看表
    private String publicSelectTable = new BaseBean().getPropValue("qc20230816permission","publicSelectTable");
    //公共编辑表
    private String publicSetTable = new BaseBean().getPropValue("qc20230816permission","publicSetTable");
    //人员查看表
    private String resourceSelectTable = new BaseBean().getPropValue("qc20230816permission","resourceSelectTable");
    //人员编辑表
    private String resourceSetTable = new BaseBean().getPropValue("qc20230816permission","resourceSetTable");
    //个人字段查看表
    private String selfSelectTable = new BaseBean().getPropValue("qc20230816permission","selfSelectTable");



    /**
     * 处理人员信息字段展示
     * @param resourceId
     * @param fieldName
     * @return
     */
    public Boolean fieldShowCheck(int resourceId,String fieldName,int tabType,int selectUserId){
        Boolean isHave = false;

        for (int i = 0; i < 4; i++) {
            if (i == 0){
                isHave =  publicFildSelect(fieldName,tabType);  //公共查看表
            }
            if (i == 1){
                isHave =  publicFieldSet(fieldName,tabType);    //公共编辑表
            }
            if (i == 2){
                isHave =  FieldSelect(resourceId,fieldName,tabType);    //人员查看表
            }
            if (i == 3){
                isHave =  FieldSet(resourceId,fieldName,tabType);       //人员编辑表
            }
            if (isHave){
                return isHave;
            }
        }
        //如果查看人员是自己再查个人字段查看表
        if (resourceId == selectUserId){
            isHave =selfFildSelect(fieldName,tabType);
        }

        return isHave;
    }
    public Boolean fieldShowCheck(int resourceId,String fieldName,int tabType){
        Boolean isHave = false;

        for (int i = 0; i < 4; i++) {
            if (i == 0){
                isHave =  publicFildSelect(fieldName,tabType);  //公共查看表
            }
            if (i == 1){
                isHave =  publicFieldSet(fieldName,tabType);    //公共编辑表
            }
            if (i == 2){
                isHave =  FieldSelect(resourceId,fieldName,tabType);    //人员查看表
            }
            if (i == 3){
                isHave =  FieldSet(resourceId,fieldName,tabType);       //人员编辑表
            }
            if (isHave){
                return isHave;
            }
        }
        return isHave;
    }

    /**
     * 判断字段是否在公共查看表
     * @param fieldName 字段名
     * @return
     */
    public Boolean publicFildSelect(String fieldName,int tabType){
        Boolean isHave = false;
        String sql = "";
        RecordSet rs = new RecordSet();


        sql = "select * from "+publicSelectTable+" where instr(',' || ryxxzd || ',', ',' || '"+fieldName+"'  || ',')>0 and ssbqy="+tabType;
        new BaseBean().writeLog("==zj==(公共查看表)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
           isHave = true;
        }

        return isHave;

    }

    /**
     * 个人字段查看
     * @param fieldName
     * @param tabType
     * @return
     */
    public Boolean selfFildSelect(String fieldName,int tabType){
        Boolean isHave = false;
        String sql = "";
        RecordSet rs = new RecordSet();


        sql = "select * from "+selfSelectTable+" where instr(',' || grxxzd || ',', ',' || '"+fieldName+"'  || ',')>0 and ssbqy="+tabType;
        new BaseBean().writeLog("==zj==(个人字段查看)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            isHave = true;
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
     * 查看权限赋予
     * @param resourceId    人员id
     * @param fieldName    字段名
     * @return
     */
    public Boolean FieldSelect(int resourceId,String fieldName,int tabType){
        Boolean isHave = false;
        String sql = "";
        RecordSet rs = new RecordSet();

        sql = "select * from "+resourceSelectTable+" where instr(',' || ryxxzd || ',', ',' || '"+fieldName+"' || ',')>0 and "+
                "instr(',' || ry || ',', ',' ||"+resourceId+"  || ',')>0 and ssbqy="+tabType;
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


}
