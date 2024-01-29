package com.engine.kq.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.core.impl.Service;
import com.engine.kq.cmd.report.GetKQReportCmd;
import com.engine.kq.service.KQMyAttendanceService;
import com.engine.kq.cmd.myattendance.*;
import weaver.common.DateUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 前台--人事--我的考勤
 */
public class KQMyAttendanceServiceImpl extends Service implements KQMyAttendanceService {

    @Override
    public Map<String, Object> getHrmKQReportInfo(Map<String, Object> params, User user) {
        try{
            String type = Util.null2String(params.get("type"));
            String typevalue = Util.null2String(params.get("typevalue"));
            String loaddata = Util.null2String(params.get("loaddata"));
            String fromDate = Util.null2String(params.get("fromDate"));
            String toDate = Util.null2String(params.get("toDate"));
            int subCompanyId = Util.getIntValue((String) params.get("subCompanyId"),0);
            int departmentId = Util.getIntValue((String) params.get("departmentId"),0);
            String resourceId = Util.null2String(params.get("resourceId"));
            String status = Util.null2String(params.get("status"));

            if(resourceId.length()==0){
                resourceId = "" + user.getUID();
            }
            if(type.equals("1")){//年
                if(typevalue.length()==0 || typevalue.length()!=4){
                    typevalue = DateUtil.getYear();
                }
                fromDate = typevalue + "-01-01";
                toDate = DateUtil.getLastDayOfYear(DateUtil.parseToDate(fromDate));
            }else if(type.equals("2")){//月
                if(typevalue.length()==0){
                    typevalue = DateUtil.getYear()+"-"+DateUtil.getMonth();
                }
                fromDate = typevalue + "-01";
                toDate = DateUtil.getLastDayOfMonthToString(DateUtil.parseToDate(fromDate));
            }
            params.put("fromDate",fromDate);
            params.put("toDate",toDate);

            /**获取今天的日期*/
            Calendar today = Calendar.getInstance();
            String currentdate = Util.add0(today.get(Calendar.YEAR), 4) + "-"
                    + Util.add0(today.get(Calendar.MONTH) + 1, 2) + "-"
                    + Util.add0(today.get(Calendar.DAY_OF_MONTH), 2);

            Map<String, Object> paramsMap = new HashMap<String, Object>();
            paramsMap.put("pageIndex",1);
            paramsMap.put("pageSize",10);
            paramsMap.put("typeselect","6");
            paramsMap.put("fromDate",fromDate);
            paramsMap.put("toDate",toDate);
            paramsMap.put("viewScope","3");
            paramsMap.put("resourceId",resourceId);
            paramsMap.put("isNoAccount","1");
            paramsMap.put("attendanceSerial","");
            paramsMap.put("isFromMyAttendance","1");
            Map<String, Object> temp = new HashMap<String,Object>();
            temp.put("data",JSONObject.toJSONString(paramsMap));
            temp.put("reportType","month");

            params.put("datas",commandExecutor.execute(new GetKQReportCmd(temp, user)).get("datas"));
            new BaseBean().writeLog("==zj==(我的考勤--params)" + JSON.toJSONString(params));
        }catch (Exception e){
            new BaseBean().writeLog(e);
        }
        return commandExecutor.execute(new GetHrmKQReportInfoCmd(params, user));
    }

    @Override
    public Map<String, Object> getHrmKQMonthReportInfo(Map<String, Object> params, User user) {
        return commandExecutor.execute(new GetHrmKQMonthReportInfoCmd(params, user));
    }

    @Override
    public Map<String, Object> getHrmKQReportDetialInfo(Map<String, Object> params, User user) {
        return commandExecutor.execute(new GetHrmKQReportDetialInfoCmd(params, user));
    }

    @Override
    public Map<String, Object> getHrmKQSignInfo(Map<String, Object> params, User user) {
        return commandExecutor.execute(new GetHrmKQSignInfoCmd(params, user));
    }

    @Override
    public Map<String, Object> getDetailCondition4Mobile(Map<String, Object> params, User user) {
        return commandExecutor.execute(new GetDetailCondition4MobileCmd(params, user));
    }

    @Override
    public Map<String, Object> getDetailList4Mobile(Map<String, Object> params, User user) {
        return commandExecutor.execute(new GetDetailList4MobileCmd(params, user));
    }
}
