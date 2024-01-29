package com.api.customization.qc2419148.web;

import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2419148.UserCheckUtil;
import com.engine.common.util.ParamUtil;
import weaver.general.BaseBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/workflow/submitcheck")
public class WorkFlowCheckWeb {


    @POST
    @Path("/getMobileCheck")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMobileCheck(@Context HttpServletRequest request, @Context HttpServletResponse response){
        UserCheckUtil userCheckUtil = new UserCheckUtil();
        Map<String,Object> result = new HashMap<String,Object>();
        try{
            Map<String, Object> params = ParamUtil.request2Map(request);
            result = userCheckUtil.checkMobile(params);

        }catch (Exception e){
            result.put("code","-1");
            new BaseBean().writeLog("==zj==(请假加班明细导入接口报错)" + e);
        }
        return JSONObject.toJSONString(result);
    }
}

