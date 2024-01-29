package com.api.customization.qc20230816.permission.web;

import com.alibaba.fastjson.JSON;
import com.engine.common.util.ParamUtil;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/organizationcustom/check")
public class CheckPermission {
    /**
     * 检测是否是管理员角色
     * @return
     */
    @GET
    @Path("/checkIsHave")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkIsAdmin(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            User user = HrmUserVarify.getUser(request, response);
            Boolean isHave = false;
            if (user.getUID() != 1){
                apidatas.put("isHave",isHave);
            }
            if (user.getUID() == 1){
                isHave = true;
                apidatas.put("isHave",isHave);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return JSON.toJSONString(apidatas);

    }
}
