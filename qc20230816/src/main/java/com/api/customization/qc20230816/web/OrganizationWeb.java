package com.api.customization.qc20230816.web;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc20230816.util.OrganizationUtil;
import com.engine.common.util.ParamUtil;
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

@Path("/organizationcustom/check")
public class OrganizationWeb {

    /**
     * 检测是否是管理员角色
     * @return
     */
    @GET
    @Path("/checkIsAdmin")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkIsAdmin(@Context HttpServletRequest request,@Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();
        OrganizationUtil organizationUtil = new OrganizationUtil();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = organizationUtil.check(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return JSON.toJSONString(apidatas);
    }

    /**
     * 检测是否是管理员角色
     * @return
     */
    @GET
    @Path("/checkIsHave")
    @Produces(MediaType.TEXT_PLAIN)
    public String checkIsHave(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();
        Boolean isHave = false;
        try {
            User user = HrmUserVarify.getUser(request, response);
            if (user.getUID() == 1){
                isHave = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        apidatas.put("isHave",isHave);
        return JSON.toJSONString(apidatas);
    }
}
