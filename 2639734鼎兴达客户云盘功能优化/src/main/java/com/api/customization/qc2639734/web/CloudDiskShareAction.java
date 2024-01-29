package com.api.customization.qc2639734.web;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2639734.util.CloudDiskShareUtil;
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

@Path("/clouddiskcustom/share")
public class CloudDiskShareAction {

    /**
     * 保存云盘文件
     * @return
     */
    @POST
    @Path("/save")
    @Produces(MediaType.TEXT_PLAIN)
    public String save(@Context HttpServletRequest request, @Context HttpServletResponse response){
        Map<String, Object> apidatas = new HashMap<String, Object>();
        CloudDiskShareUtil cloudDiskShareUtil = new CloudDiskShareUtil();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = cloudDiskShareUtil.save(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            new BaseBean().writeLog("==zj==(云盘分享文件报错)" + JSON.toJSONString(e));
        }

        return JSON.toJSONString(apidatas);

    }

}
