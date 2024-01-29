package com.api.customization.qc20230816.workingreport.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc20230816.popkqreport.util.ExportUtil;
import com.api.customization.qc20230816.popkqreport.util.PopKqReportUtil;
import com.api.customization.qc20230816.workingreport.util.WorkReportExport;
import com.api.customization.qc20230816.workingreport.util.WorkingKqReportUtil;
import com.engine.common.util.ParamUtil;
import weaver.general.BaseBean;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/workingkqcustom/report")
public class WorkingReportAction {

    /**
     * 获取研发工时报表
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/getKQReport")
    @Produces(MediaType.TEXT_PLAIN)
    public String getKqReport(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            WorkingKqReportUtil workingKqReportUtil = new WorkingKqReportUtil(ParamUtil.request2Map(request), user);
            apidatas = workingKqReportUtil.getKqReport();
        } catch (Exception e) {
            apidatas.put("status", "-1");
            new BaseBean().writeLog("==zj==(研发工时报表报错)" + JSON.toJSONString(e));
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 工时报表导出
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/export")
    @Produces(MediaType.TEXT_PLAIN)
    public String export(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            User user = HrmUserVarify.getUser(request, response);
            WorkReportExport workReportExport = new WorkReportExport(ParamUtil.request2Map(request), request, response, user);
            apidatas = workReportExport.Export();
        } catch (Exception e) {
            apidatas.put("status","-1");
            apidatas.put("messgae",e);
            new BaseBean().writeLog("==zj==(e)" + JSON.toJSONString(e));
        }

        return JSONObject.toJSONString(apidatas);
    }

}
