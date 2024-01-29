package com.api.customization.qc2640833.web;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2640833.util.KqUtil;
import com.engine.common.util.ParamUtil;
import weaver.general.BaseBean;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/kqcardcustom")
public class CheckKQCardAction {

    /**
     * 检测打卡
     * @return
     */
    @GET
    @Path("/check")
    @Produces(MediaType.TEXT_PLAIN)
    public String check(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();
        KqUtil kqUtil = new KqUtil();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = kqUtil.check(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(打卡检测报错)" + JSON.toJSONString(e));
            apidatas.put("status","-1");
            apidatas.put("msg",e);
            return JSON.toJSONString(apidatas);
        }

        return JSON.toJSONString(apidatas);

    }
}
