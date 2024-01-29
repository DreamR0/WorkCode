package com.api.customization.qc20230816.workingreport.util;

public class WorkingCheckUtil {

    public Boolean isShow(String fieldName){
        Boolean isShow = false;
        //人员id
        if ("resourceId".equals(fieldName)){
            isShow = true;
        }
        //姓名
        if ("lastname".equals(fieldName)){
            isShow = true;
        }
        //分部名称
        if ("subcompany".equals(fieldName)){
            isShow = true;
        }
        //分部id
        if ("subcompanyId".equals(fieldName)){
            isShow = true;
        }
        //部门名称
        if ("department".equals(fieldName)){
            isShow = true;
        }
        //部门id
        if ("departmentId".equals(fieldName)){
            isShow = true;
        }
        //员工编号
        if ("workcode".equals(fieldName)){
            isShow = true;
        }
        //上班工时
        if ("workhours".equals(fieldName)){
            isShow = true;
        }
        //工时报表加班工时
        if ("workingoverhours".equals(fieldName)){
            isShow = true;
        }


        return isShow;

    }
}
