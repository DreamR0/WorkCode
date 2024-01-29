package com.api.customization.qc20230816.popkqreport.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc20230816.popkqreport.util.CheckUtil;
import com.api.customization.qc20230816.popkqreport.util.ExportUtil;
import com.api.customization.qc20230816.popkqreport.util.PopKqReportUtil;
import com.api.customization.qc20230816.popkqreport.util.SaveUtil;
import com.engine.common.util.ParamUtil;
import weaver.general.BaseBean;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/popkqcustom/report")
public class PopKqReportAction {
    /**
     * 获取上月考勤汇总报表
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
            PopKqReportUtil popKqReportUtil = new PopKqReportUtil(ParamUtil.request2Map(request), user);
            apidatas = popKqReportUtil.getKqReport();
        } catch (Exception e) {
            apidatas.put("status", "-1");
            new BaseBean().writeLog("==zj==(弹窗考勤报表报错)" + JSON.toJSONString(e));
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 是否需要弹窗提醒
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/getCheck")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCheck(@Context HttpServletRequest request, @Context HttpServletResponse response){
        CheckUtil checkUtil = new CheckUtil();
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            User user = HrmUserVarify.getUser(request, response);
            if (user.getUID() == 1){
                //管理员不弹窗
                apidatas.put("status","1");
                apidatas.put("isPop","false");
                return JSONObject.toJSONString(apidatas);
            }
            apidatas = checkUtil.isPop(ParamUtil.request2Map(request), user);

        } catch (Exception e) {
            apidatas.put("status","-1");
            apidatas.put("message",e);
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 弹窗提醒保存
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/save")
    @Produces(MediaType.TEXT_PLAIN)
    public String save(@Context HttpServletRequest request, @Context HttpServletResponse response){
        SaveUtil saveUtil = new SaveUtil();
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = saveUtil.SaveUtil(ParamUtil.request2Map(request), user);

        } catch (Exception e) {
            apidatas.put("status","-1");
            apidatas.put("messgae","保存失败");
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 弹窗报表内部确认
     * @params
     * @return
     */
    @POST
    @Path("/savesys")
    @Produces(MediaType.APPLICATION_JSON)
    public String saveSys(JSONObject params){
        SaveUtil saveUtil = new SaveUtil();
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {

            apidatas  = saveUtil.SaveSysUtil(params);

        } catch (Exception e) {
            apidatas.put("status","-1");
            apidatas.put("messgae","保存失败");
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 考勤报表导出
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
            ExportUtil exportUtil = new ExportUtil(ParamUtil.request2Map(request), request,response,user);
            apidatas = exportUtil.Export();
        } catch (Exception e) {
            apidatas.put("status","-1");
            apidatas.put("messgae",e);
            new BaseBean().writeLog("==zj==(e)" + JSON.toJSONString(e));
        }

        return JSONObject.toJSONString(apidatas);
    }
}
