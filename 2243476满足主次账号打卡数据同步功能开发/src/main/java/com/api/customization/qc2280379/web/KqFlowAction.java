package com.api.customization.qc2280379.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.customization.qc2243476.KqUtil;
import com.engine.common.util.ParamUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
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

@Path("/kq/kqflowtips")
public class KqFlowAction {
    @POST
    @Path("/gettips")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTips(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            Map<String, Object> params = ParamUtil.request2Map(request);
            String workflowid = Util.null2String(params.get("workflowid"));
            User user = HrmUserVarify.getUser(request,response);
            if (user.isAdmin()){
                //如果账户为管理员
                apidatas.put("tips","该用户为管理员");
                apidatas.put("code","1");
                return JSONObject.toJSONString(apidatas);
            }
            KqUtil kqUtil = new KqUtil();
            apidatas = kqUtil.isMuchGroup(user.getUID(),workflowid);

        }catch (Exception e){
            apidatas.put("code","0");
            apidatas.put("msg",e);
        }
        new BaseBean().writeLog("==zj==endtime:"+System.currentTimeMillis());
        return JSONObject.toJSONString(apidatas);
    }

}
