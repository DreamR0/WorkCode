package com.api.customization.qc2508343.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.qc2508343.util.JsonUtil;
import com.api.customization.qc2508343.util.MatrixUtilCustom;
import com.engine.common.util.ParamUtil;
import com.sap.db.jdbc.Hash;
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

@Path("/hrm/matrixcustom")
public class MatrixWeb {

    @POST
    @Path("/addmatrix")
    @Produces(MediaType.APPLICATION_JSON)
    //==zj 获取考勤组列表
    public String addmatrix(@Context HttpServletRequest request, @Context HttpServletResponse response){
        String data = "";
        HashMap result = new HashMap();
        User user = HrmUserVarify.getUser(request,response);
        try{
            JsonUtil jsonUtil = new JsonUtil();
            MatrixUtilCustom matrixUtilCustom = new MatrixUtilCustom();
            JSONObject params = jsonUtil.getJson(request);
            new BaseBean().writeLog("==zj==(前端数据params)" + JSON.toJSONString(params));
            data = matrixUtilCustom.operateMatrix(user, params);

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(矩阵操作接口报错)" + JSON.toJSONString(e));
             result = new HashMap();
            result.put("code", "-1");
            result.put("message", e);
            return JSON.toJSONString(result);
        }
        return JSON.toJSONString(data);
    }
}
