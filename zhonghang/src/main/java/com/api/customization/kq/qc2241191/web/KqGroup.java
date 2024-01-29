package com.api.customization.kq.qc2241191.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.kq.qc2241191.KqGroupSet;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/kq/kqgroupset")
public class KqGroup {

    @POST
    @Path("/groupselect")
    @Produces(MediaType.APPLICATION_JSON)
    //==zj 获取考勤组列表
    public String groupSelect(@Context HttpServletRequest request, @Context HttpServletResponse response){
        List<Object> apidatas = new ArrayList<>();
        Map<String,Object> result = new HashMap<>();
        User user = HrmUserVarify.getUser(request,response);
        Map<String, Object> params = ParamUtil.request2Map(request);
        Map<String,Object> data = null;
        try{

                 KqGroupSet kqGroupSet = new KqGroupSet();
                 apidatas = kqGroupSet.kqGroupSelect(params,user);

                 result.put("code","1");
                 result.put("data","apidatas");

        }catch (Exception e){
            new BaseBean().writeLog(e);
           result.put("code","0");
        }

        return JSONObject.toJSONString(result);
    }

    @POST
    @Path("/groupmemberchange")
    @Produces(MediaType.APPLICATION_JSON)
    //==zj 考勤组成员变更
    public String groupMemberChange(@Context HttpServletRequest request, @Context HttpServletResponse response){
        User user = HrmUserVarify.getUser(request,response);
        Map<String, Object> params = ParamUtil.request2Map(request);
        Map<String,Object> retmap = null;
        try{

            KqGroupSet kqGroupSet = new KqGroupSet();
            retmap = kqGroupSet.KqGroupChange(params,user);
            new BaseBean().writeLog("==zj==(成员变更考勤组后，返回给前端的数据)" + JSON.toJSONString(retmap));

        }catch (Exception e){
            new BaseBean().writeLog(e);
        }

        return JSONObject.toJSONString(retmap);
    }



}
