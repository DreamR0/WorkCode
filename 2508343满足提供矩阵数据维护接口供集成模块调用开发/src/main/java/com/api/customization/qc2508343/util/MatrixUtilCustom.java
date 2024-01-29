package com.api.customization.qc2508343.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.taobao.api.internal.util.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.*;

public class MatrixUtilCustom {
    private String matrixTable = new BaseBean().getPropValue("qc2508343","matrixTable");//获取矩阵表表名
    private String[] matrixFields = Util.null2String(new BaseBean().getPropValue("qc2508343","matrixField")).split(",");//获取矩阵表的所有列字段
    String matrixWhereField = new BaseBean().getPropValue("qc2508343","matrixWhereField");//获取矩阵表的条件字段

    /**
     * 矩阵信息操作
     * @param user 部门id
     * @param params 集成传递数据
     * @return
     */
    public String operateMatrix(User user, JSONObject params){
        HashMap result = new HashMap();
        Boolean isSucess = false;
        new BaseBean().writeLog("==zj==(矩阵params)" + JSON.toJSONString(params));
        JSONArray jsonArray = JSONArray.parseArray(Util.null2String(params.get("data")));

        try {
            if (jsonArray.size() <= 0 || jsonArray == null){
                result.put("code", "-1");
                result.put("message", "传递数据为空");
                return JSON.toJSONString(result);
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                //获取前端数据key值
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                List<String> matrixList = new ArrayList<>();    //前端数值
                List<String> keyList = new ArrayList<>();       //修改字段列表
                Iterator keys = jsonObject.keySet().iterator();
                while (keys.hasNext()){
                    String key  = keys.next().toString();
                    keyList.add(key);   //获取修改字段
                    matrixList.add(Util.null2String(jsonObject.get(key)));  //获取修改字段值
                }
                //取出第一个数据，正常情况为“门店”的值
                String matrixWhereValue = Util.null2String(jsonObject.get(matrixWhereField));
                Boolean isHave = queryMatrix(matrixWhereValue);
                new BaseBean().writeLog("==zj==(当前矩阵是否存在已有行)" + JSON.toJSONString(isHave));
                if (isHave){
                    //如果存在，修改当前行
                     isSucess = setMatrix(matrixWhereValue, matrixList,keyList);
                }else {
                    //如果不存在，新增当前行
                    isSucess = addMatrix(matrixList,keyList);
                }
                if (!isSucess){
                    break;
                }
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(矩阵信息操作报错)" + JSON.toJSONString(e));
            result.put("code", "-1");
            result.put("message", e);
            return JSON.toJSONString(result);
        }

        //返回报错信息
        if (isSucess){
            result.put("code", "1");
            result.put("message", "更新成功");
        }else {
            result.put("code", "-1");
            result.put("message", "更新失败");
        }

        return JSON.toJSONString(result);
    }

    /**
     * 删除当前行
     * @param user
     * @param matrixWhereFieldValue
     * @return
     */
    public Boolean del(User user, String matrixWhereFieldValue){
        RecordSet rs = new RecordSet();
        String matrixWhereField = new BaseBean().getPropValue("qc2508343","matrixWhereField");
        Boolean isSucess = false;
        try {
            String sqlDel = "delete from "+matrixTable+" where " +matrixWhereField+" = '" + matrixWhereFieldValue+"'";
            new BaseBean().writeLog("==zj==(sql矩阵信息行删除)" + JSON.toJSONString(sqlDel));
            isSucess = rs.executeUpdate(sqlDel);
        } catch (Exception e) {
           new BaseBean().writeLog("==zj==(删除矩阵行报错)" + JSON.toJSONString(e));
        }
        return isSucess;
    }

    /**
     * 查询矩阵行信息
     * @param matrixWhereValue
     * @return
     */
    public Boolean queryMatrix(String matrixWhereValue){
        Boolean isHave = false;
        RecordSet rs = new RecordSet();
        try {
            String querySql = "select * from "+matrixTable+" where " +matrixWhereField+" = '" + matrixWhereValue+"'";
            new BaseBean().writeLog("==zj==(矩阵信息查询)" + JSON.toJSONString(querySql));
            rs.executeQuery(querySql);
            if (rs.next()){
                isHave = true;
            }
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(矩阵信息查询报错)" + JSON.toJSONString(e));
        }
        return isHave;
    }

    /**
     * 修改矩阵行信息
     * @param matrixList
     * @return
     */
    public Boolean setMatrix(String matrixWhereValue , List<String> matrixList,List<String> keyList){
        Boolean isSucess = false;
        RecordSet rs = new RecordSet();
        String selectKey = "";
        List<String> matrixListNew = new ArrayList<>();
        try {
            //这里获取更改字段名
            selectKey =  keyList.toString();
            String setSqlBase = "update "+matrixTable+" set ";
            for (int i = 0; i < keyList.size(); i++) {
                //这里对列字段进行拼接
                String sql = keyList.get(i) + " = ?,";
                setSqlBase += sql;
            }
            //去掉最后一个逗号
            setSqlBase = setSqlBase.substring(0, setSqlBase.length() - 1);
            //拼接条件
            String setSqlWhere = " where "+matrixWhereField+" = '" + matrixWhereValue+"'";
            String sql = setSqlBase + setSqlWhere;
            new BaseBean().writeLog("==zj==(矩阵信息修改)" + JSON.toJSONString(sql));

            //这里再进行对应字段值查询，避免被覆盖

               String keyValueSql = "select "+selectKey+" from "+ matrixTable+" where "+matrixWhereField+" ='"+matrixWhereValue+"'";
               rs.executeQuery(keyValueSql);
               if (rs.next()){
                   //这里把修改字段的值都取出来
                   for (int i = 0; i <keyList.size() ; i++) {
                       String value = Util.null2String(rs.getString(keyList.get(i)));

                   }
               }


            isSucess = rs.executeUpdate(sql, matrixList);
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(矩阵信息修改报错)" + JSON.toJSONString(e));
        }
        return isSucess;
    }

    /**
     * 新增矩阵行信息
     * @param matrixList
     * @return
     */
    public Boolean addMatrix(List<String> matrixList,List<String> keyList){
        Boolean isSucess = false;
        String uuid = UUID.randomUUID().toString();//唯一标识
        String dataorder = "1"; //排序
        RecordSet rs = new RecordSet();
        String matrixField = "";//新增行列表字段
        for (int i = 0; i < keyList.size(); i++) {
            matrixField += keyList.get(i)+",";
        }
        new BaseBean().writeLog("==zj==(keyList-新增)" + JSON.toJSONString(keyList));
        matrixField = matrixField.substring(0,matrixField.length() - 1);
        try {
            String sqlBase = "insert into "+matrixTable+" (uuid,dataorder,"+matrixField+") values ('"+uuid+"',"+dataorder+",";
            for (int i = 0; i < keyList.size(); i++) {
                String sql = "?,";
                sqlBase += sql;
            }
            sqlBase = sqlBase.substring(0, sqlBase.length() - 1)+")";
            new BaseBean().writeLog("==zj==(矩阵新增sql)" + JSON.toJSONString(sqlBase));
            isSucess = rs.executeUpdate(sqlBase, matrixList);
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(矩阵新增报错)" + JSON.toJSONString(e));
        }
        return isSucess;
    }

}
