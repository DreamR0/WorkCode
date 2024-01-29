package com.api.customization.qc2287763.Util;

import com.alibaba.fastjson.JSON;
import com.wbi.util.Util;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.hrm.company.DepartmentComInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KqReportUtil {

    /**
     * 添加考勤报表自定义列
     * @param columns
     */
    public void getColumn(List<Object> columns){
        RecordSet rs = new RecordSet();
        String getColumnSql = "";
        String colName  =   "";
        String colKey = "";
        Map<String,Object> column = null;
        try{
            getColumnSql = "select * from "+new BaseBean().getPropValue("qc2287763","tableName");
            new BaseBean().writeLog("==zj==(考勤汇总报表自定义字段sql)" + getColumnSql);
            rs.executeQuery(getColumnSql);
            while (rs.next()){
                colName = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2287763","colName")));   //获取考勤报表字段展示名
                colKey  = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2287763","colKey")));    //获取考勤报表字段key
                column = new HashMap<>();

                column.put("dataIndex",colKey);
                column.put("isSystem","1");
                column.put("key",colKey);
                column.put("rowSpan",3);
                column.put("showDetial","0");
                column.put("title",colName);
                column.put("type",colKey);
                column.put("unit","");
                column.put("width",65);
                columns.add(column);
            }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(获取自定义列错误)" + e);
        }
    }

    /**
     * 获取自定义展示列
     * @param options
     */
    public void getTabs(List<Object> options){
        RecordSet rs = new RecordSet();
        String getTabsSql = "";
        Map<String,Object> option = null;
        String tabsName = "";
        String tabsKey = "";
        try{
            getTabsSql = "select * from "+new BaseBean().getPropValue("qc2287763","tableName");
            rs.executeQuery(getTabsSql);
            while (rs.next()){
                option = new HashMap<>();
                tabsName = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2287763","colName")));
                tabsKey = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2287763","colKey")));

                option.put("key",tabsKey);
                option.put("label","141");
                option.put("name",tabsKey);
                option.put("showname",tabsName);
                options.add(option);
            }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(获取展示列报错信息)" +e);
        }
    }

    /**
     * 获取该人员部门的所有上级部门
     * @param deptid
     */
    public List<Map<String,String>> getSupDepId(String deptid){
        String SupDePId = "";
        String depIds = "";
        List<Map<String,String>> depList = new ArrayList<Map<String,String>>();
        Map<String,String> resultMap = null;
        DepartmentComInfo departmentComInfo = new DepartmentComInfo();
        try{
             SupDePId = new DepartmentComInfo().getAllSupDepartment(deptid);    //获取该部门的所有上级部门
             depIds = SupDePId + deptid;    //把本部门id和上级部门id拼接
             String[] Arr = depIds.split(",");

             new BaseBean().writeLog("==zj==(depIds)" + depIds);
             for (String id : Arr){
                 resultMap = new HashMap<>();
                String name  = departmentComInfo.getDepartmentName(id);     //获取每个部门简称
                 resultMap.put("depName",name);
                 resultMap.put("depId",id);
                 depList.add(resultMap);
             }
        }catch (Exception e){
            new BaseBean().writeLog("==zj==(获取部门集合报错)" + e);
        }


        new BaseBean().writeLog("==zj==(depList)" + JSON.toJSONString(depList));
        return depList;
    }

    /**
     * 给汇总报表层级自定义字段赋值
     * @param data
     * @param depList
     */
    public void getDatas(Map<String,Object> data,List<Map<String,String>> depList){
        RecordSet rs = new RecordSet();
        Map<String,String> depMap = new HashMap<>();
        List<String> colList = new ArrayList<String>();
        String getTableSetSql = "";
        String tableDepId = new BaseBean().getPropValue("qc2287763","tableDepId");
        String tableKey = new BaseBean().getPropValue("qc2287763","tableKey");

        try{
            /**
             * 先获取考勤报表自定义层级字段
             */
             colList = getColList();
             new BaseBean().writeLog("==zj==(汇总报表赋值--colList)" + JSON.toJSONString(colList));
            /**
             * 再将每个层级字段进行对应显示(适用于mysql)
             */
            if (colList != null){
                for (int i = 0; i < colList.size(); i++) {
                    String fieldName = "";
                    String fieldValue = "";
                    String colKey = colList.get(i);
                    new BaseBean().writeLog("==zj==(depList)" + JSON.toJSONString(depList));
                    for (int j = 0; j < depList.size(); j++) {
                        depMap = depList.get(j);
                        String depId = depMap.get("depId");

                        getTableSetSql = "select * from uf_bbzsset where FIND_IN_SET("+depId+","+tableDepId+") and "+tableKey+"='"+colKey+"'";
                        new BaseBean().writeLog("==zj==(getTableSetSql)" + getTableSetSql);
                        rs.executeQuery(getTableSetSql);
                        if (rs.next()){
                            fieldName = colKey;
                            fieldValue = depMap.get("depName");
                            new BaseBean().writeLog("==zj==(该字段匹配到部门)" + fieldName + " | " + fieldValue);
                            data.put(fieldName,fieldValue);
                            break;
                        }
                    }
                    if ("".equals(fieldValue)){
                        fieldValue = "空";
                        fieldName = colKey;
                        new BaseBean().writeLog("==zj==(如果该字段匹配不到部门)" + fieldName + " | " + fieldValue);
                        data.put(fieldName,fieldValue);
                    }

                }
            }

        }catch (Exception e){

        }
    }

    /**
     * 获取汇总报表自定义层级字段list
     * @return
     */
    public List<String> getColList(){
        RecordSet rs = new RecordSet();
        List<String> colList = new ArrayList<String>();
        String getColSql = "";

        try{
            getColSql = "select * from "+new BaseBean().getPropValue("qc2287763","tableName");
            new BaseBean().writeLog("==zj==(获取层级自定义字段sql)"  + getColSql);
            rs.executeQuery(getColSql);
            while (rs.next()){
                colList.add(Util.null2String(rs.getString(new BaseBean().getPropValue("qc2287763","colKey"))));
            }
        }catch (Exception e){

        }
        return colList;
    }

    /**
     * 通过colKey获取字段显示名
     * @return
     */
    public String getcolName(String colKey){
        RecordSet rs = new RecordSet();
        String getColNameSql = "";
        String tableName = new BaseBean().getPropValue("qc2287763","tableName");    //考勤汇总报表层级字段表名
        String colKeyName = new BaseBean().getPropValue("qc2287763","colKey");          //考勤汇总报表层级字段key字段名
        String colName = "";
        try{
            getColNameSql = "select * from "+tableName+" where "+colKeyName+" = '"+colKey+"'";
            new BaseBean().writeLog("==zj==(根据key值获取该字段显示名sql)" + getColNameSql);
            rs.executeQuery(getColNameSql);
            if (rs.next()){
                colName = Util.null2String(rs.getString(new BaseBean().getPropValue("qc2287763","colName")));
            }
        }catch (Exception e){
                new BaseBean().writeLog("==zj==(通过colKey获取字段显示名-报错)" + e);
        }
        return colName;
    }

    /**
     * 给导出汇总报表层级自定义字段赋值
     * @param name
     * @param depList
     */
    public String getDatas(String name,List<Map<String,String>> depList){
        RecordSet rs = new RecordSet();
        Map<String,String> depMap = new HashMap<>();
        String getTableSetSql = "";
        String tableDepId = new BaseBean().getPropValue("qc2287763","tableDepId");
        String tableKey = new BaseBean().getPropValue("qc2287763","tableKey");

        try{

            /**
             * 再将每个层级字段进行对应显示(适用于mysql)
             */
                    String fieldValue = "";

                    new BaseBean().writeLog("==zj==(depList)" + JSON.toJSONString(depList));
                    for (int j = 0; j < depList.size(); j++) {
                        depMap = depList.get(j);
                        String depId = depMap.get("depId");

                        getTableSetSql = "select * from uf_bbzsset where FIND_IN_SET("+depId+","+tableDepId+") and "+tableKey+"='"+name+"'";
                        new BaseBean().writeLog("==zj==(getOutTableSql)" + getTableSetSql);
                        rs.executeQuery(getTableSetSql);
                        if (rs.next()){
                            fieldValue = depMap.get("depName");
                            break;
                        }
                    }
                    if ("".equals(fieldValue)){
                        fieldValue = "空";
                    }
                    return fieldValue;
        }catch (Exception e){

        }

        return "";
    }

}
