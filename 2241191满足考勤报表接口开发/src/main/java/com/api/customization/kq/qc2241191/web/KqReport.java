package com.api.customization.kq.qc2241191.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.kq.qc2241191.util.JsonUtil;
import com.api.customization.kq.qc2241191.util.KqReportUtil;
import weaver.general.BaseBean;
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

@Path("/kq/kqReport")
public class KqReport {

    @POST
    @Path("/getKqReport")
    @Produces(MediaType.APPLICATION_JSON)
    public String getKqReport(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            User user =new User(1);
            JsonUtil jsonUtil = new JsonUtil();
            JSONObject params = jsonUtil.getJson(request);
            new BaseBean().writeLog("==zj==(前端JSON格式获取数据新)" + JSON.toJSONString(params));

            KqReportUtil kqReportUtil = new KqReportUtil(params, user);
            apidatas = kqReportUtil.getKqReport();


        }catch (Exception e){
                apidatas.put("code","0");
                apidatas.put("msg",e);
        }
        return JSONObject.toJSONString(apidatas);
    }

}
