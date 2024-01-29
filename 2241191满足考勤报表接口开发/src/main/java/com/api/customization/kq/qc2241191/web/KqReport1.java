package com.api.customization.kq.qc2241191.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.kq.qc2241191.util.KqReportUtil;
import weaver.general.BaseBean;
import weaver.hrm.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/kq/kqReport")
public class KqReport1 {

    @POST
    @Path("/getKqReport")
    @Consumes(MediaType.APPLICATION_JSON)
    public String getKqReport(JSONObject params){
        Map<String, Object> apidatas = new HashMap<String, Object>();

        try {
            User user =new User(1);
            new BaseBean().writeLog("==zj==(前端JSON格式获取数据)" + JSON.toJSONString(params));
            KqReportUtil kqReportUtil = new KqReportUtil(params, user);
            apidatas = kqReportUtil.getKqReport();


        }catch (Exception e){
                apidatas.put("code","0");
                apidatas.put("msg",e);
        }
        return JSONObject.toJSONString(apidatas);
    }

}
