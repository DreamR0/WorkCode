package com.api.customization.qc2452784.Web;

import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2452784.Util.RemindUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/meetingcustom/remindcustom")
public class RemindCustom {
    RemindUtil remindUtil = new RemindUtil();
    @POST
    @Path("/getremindlist")
    @Produces(MediaType.APPLICATION_JSON)
    //==zj 获取自定义消息列表
    public String getremindList(@Context HttpServletRequest request, @Context HttpServletResponse response){
        List<Object> apidatas = new ArrayList<>();
        User user = HrmUserVarify.getUser(request,response);
        Map<String, Object> params = ParamUtil.request2Map(request);
        Map<String,Object> data = new HashMap<>();
        try{
            apidatas = remindUtil.getRemindList(user);
            data.put("code","1");
            data.put("data",apidatas);

        }catch (Exception e){
            new BaseBean().writeLog(e);
            data.put("code","0");
            data.put("message",e);
        }

        return JSONObject.toJSONString(data);
    }

    @POST
    @Path("/deleteremindlist")
    @Produces(MediaType.APPLICATION_JSON)
    //==zj 删除自定义邮件消息列表
    public String deleteremindlist(@Context HttpServletRequest request, @Context HttpServletResponse response){
       Map<String,Object> result = new HashMap<>();
        User user = HrmUserVarify.getUser(request,response);
        Map<String, Object> params = ParamUtil.request2Map(request);
        try{
            result = remindUtil.deleteRemindCustom(user,params);
        }catch (Exception e){
            new BaseBean().writeLog(e);
            result.put("code","0");
            result.put("message","删除失败");
        }
        return JSONObject.toJSONString(result);
    }

    /**
     * 获取提醒模版
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @POST
    @Path("/getcustomfields")
    @Produces(MediaType.APPLICATION_JSON)
    public String getFields(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas.putAll(remindUtil.getremindField(user,ParamUtil.request2Map(request)));
            apidatas.put("api_status", true);
        } catch (Exception e) {
            e.printStackTrace();
            apidatas.put("api_status", false);
            apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 获取模板内容接口
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @POST
    @Path("/getcustomvariable")
    @Produces(MediaType.APPLICATION_JSON)
    public String getVariable(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas.putAll(remindUtil.getVariable(user,ParamUtil.request2Map(request)));
            apidatas.put("api_status", true);
        } catch (Exception e) {
            e.printStackTrace();
            apidatas.put("api_status", false);
            apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 新建、编辑提醒模版
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @POST
    @Path("/editcustomvariable")
    @Produces(MediaType.APPLICATION_JSON)
    public String editCustomVariable(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception{
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas.putAll(remindUtil.editRemindCustom(user,ParamUtil.request2Map(request)));
            apidatas.put("api_status", true);
        } catch (Exception e) {
            e.printStackTrace();
            apidatas.put("api_status", false);
            apidatas.put("api_errormsg", "catch exception : " + e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }
}
