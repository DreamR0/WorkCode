package com.api.customization.qc2528920.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2528920.util.ForgotPasswordCheck;
import com.api.customization.qc2528920.util.ForgotPasswordSend;
import com.engine.common.util.ParamUtil;
import com.sap.db.jdbc.Hash;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Path("/hrmcustom/passwordcustom")
public class ForgotPasswordWeb {

  /*获取手机验证码*/
    @POST
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public String next(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> resultMap = new HashMap<>();
        try {
            User user = HrmUserVarify.getUser(request,response);
            Map<String, Object> params = ParamUtil.request2Map(request);
            ForgotPasswordSend forgotPasswordSend = new ForgotPasswordSend(params, user, request);
            resultMap = forgotPasswordSend.send();
        } catch (Exception e) {
            resultMap.put("status","-1");
            resultMap.put("message",e);
        }
        return JSON.toJSONString(resultMap);
    }

    /*验证手机号码*/
    @POST
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public String check(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> resultMap = new HashMap<>();
        try {
            User user = HrmUserVarify.getUser(request,response);
            Map<String, Object> params = ParamUtil.request2Map(request);
            ForgotPasswordCheck forgotPasswordCheck = new ForgotPasswordCheck(params, user, request);
            resultMap = forgotPasswordCheck.check();
        } catch (Exception e) {
            resultMap.put("status","-1");
            resultMap.put("message",e);
        }

        return JSON.toJSONString(resultMap);
    }
}
