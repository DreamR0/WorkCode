package com.api.customization.qc2297322.web;

import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2297322.util.KqReportDetailUtil;
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
import java.util.HashMap;
import java.util.Map;

@Path("/kq/getKqReport")
public class KqDetailAction {

        @POST
        @Path("/putKqDetail")
        @Produces(MediaType.APPLICATION_JSON)
        public String getKqDetail(@Context HttpServletRequest request, @Context HttpServletResponse response){
            KqReportDetailUtil kqReportDetailUtil = new KqReportDetailUtil();
            Map<String,Object> retmap = new HashMap<String,Object>();
            try{
                Map<String, Object> params = ParamUtil.request2Map(request);
                retmap = kqReportDetailUtil.KqReportDetailIn(params);

            }catch (Exception e){
                retmap.put("code","-1");
                new BaseBean().writeLog("==zj==(请假加班明细导入接口报错)" + e);
            }
            return JSONObject.toJSONString(retmap);
        }

}
