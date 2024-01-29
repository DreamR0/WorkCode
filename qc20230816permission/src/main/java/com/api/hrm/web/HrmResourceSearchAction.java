package com.api.hrm.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.api.hrm.service.impl.HrmSearchServiceImpl;
import com.api.hrm.util.PageUidFactory;
import com.engine.common.util.ParamUtil;
import com.engine.common.util.ServiceUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.tools.HrmValidate;
import com.alibaba.fastjson.JSONObject;
import com.api.hrm.service.HrmSearchService;
import weaver.systeminfo.SysMaintenanceLog;

/**
 * 人力资源查询人员action
 */
@Path("/hrm/search")
public class HrmResourceSearchAction extends BaseBean {

    private BaseBean logger = new BaseBean();

    private HrmSearchService getService(User user) {
        return (HrmSearchServiceImpl) ServiceUtil.getService(HrmSearchServiceImpl.class, user);
    }

    /**
     * 判断是否具有权限
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getHasRight")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHasRight(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apiDatas = new HashMap<String, Object>();//传给前台的数据集合
        try {
            User user = HrmUserVarify.getUser(request, response);
            //是否具有【人员信息导出】权限
            boolean canExportExcel = HrmUserVarify.checkUserRight("HrmResourceInfo:Import", user);
            //是否具有【人力资源维护】权限
            boolean canEditBasicInfo = HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user);
            //是否具有【人力资源卡片系统信息维护】权限
            boolean canEditSystemInfo = HrmUserVarify.checkUserRight("ResourcesInformationSystem:All", user);
            //是否具有【薪酬福利维护】权限
            boolean canEditfinanceInfo = HrmUserVarify.checkUserRight("HrmResourceWelfareEdit:Edit", user);

            apiDatas.put("canExportExcel", canExportExcel);
            if (/*user.getUID() == 1*/true){
                apiDatas.put("canEditBasicInfo", canEditBasicInfo);
            }
            apiDatas.put("canEditSystemInfo", canEditSystemInfo);
            apiDatas.put("canEditfinanceInfo", canEditfinanceInfo);

            apiDatas.put("status", "1");
        } catch (Exception e) {
            apiDatas.put("status", "-1");
            logger.writeLog(e);
        }
        return JSONObject.toJSONString(apiDatas);
    }

    /**
     * 查询人员列表右键菜单
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getRightMenu")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRightMenu(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getRightMenu(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 保存查询条件定制
     *
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/saveHrmSearchUserDefine")
    @Produces(MediaType.TEXT_PLAIN)
    public String saveHrmSearchUserDefine(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).saveHrmSearchUserDefine(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 获取常用条件定制
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getHrmSearchUserDefine")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrmSearchUserDefine(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getHrmSearchUserDefine(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
    }

    /**
     * 查询人员模板列表
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getHrmSearchMoudleList")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrmSearchMoudleList(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getHrmSearchMoudleList(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
    }

    /**
     * 存为模板
     *
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/saveHrmSearchCondition")
    @Produces(MediaType.TEXT_PLAIN)
    public String saveHrmSearchCondition(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).saveHrmSearchCondition(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 查询人员查询条件
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getHrmSearchCondition")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrmSearchCondition(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getHrmSearchCondition(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
    }

    /**
     * 查询人员
     *
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/getHrmSearchResult")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrmSearchResult(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getHrmSearchResult(ParamUtil.request2Map(request), user, request);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
    }

    /**
     * 是否查询人员发起群聊
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/hasEmessage")
    @Produces(MediaType.TEXT_PLAIN)
    public String hasEmessage(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> data = new HashMap<String, Object>();
        User user = HrmUserVarify.getUser(request, response);
        if (HrmValidate.hasEmessage(user)) {
            data.put("hasEmessage", "true");
        } else {
            data.put("hasEmessage", "false");
        }
        return JSONObject.toJSONString(data);
    }

    @GET
    @Path("/getFields")
    @Produces(MediaType.TEXT_PLAIN)
    public String getFields(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getFields(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 同步显示列到所有人
     *
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/syncShowCol2All")
    @Produces(MediaType.TEXT_PLAIN)
    public String syncShowCol2All(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).syncShowCol2All(ParamUtil.request2Map(request), user, request);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 记录Excel导出的日志
     *
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/exportExcelLog")
    @Produces(MediaType.TEXT_PLAIN)
    public String exportExcelLog(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            String count = (String) request.getParameter("count");
            //导出Excel的语句
            String sql = (String) request.getSession(true).getAttribute("HrmResourceSearchResultExcelSql");
            //记录Excel导出的日志
            SysMaintenanceLog log = new SysMaintenanceLog();
            log.resetParameter();
            log.setRelatedId(-1);
            log.setRelatedName("excel");
            log.setOperateType("excel");
            log.setOperateDesc(count+"--HrmResourceSearchResultExcelSql");
            log.setOperateItem("507");
            log.setOperateUserid(user.getUID());
            log.setClientAddress(Util.getIpAddr(request));
            Map<String, String> kv = new HashMap<>();
            kv.put("other_operatedesc", count+"--"+sql);
            log.setKv(kv);
            log.setSysLogInfo();
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getSearchConditionOfLog")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSearchConditionOfLog(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getSearchConditionOfLog(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getHrmSearchLogList")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHrmSearchLogList(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getHrmSearchLogList(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getOrderBy4Search")
    @Produces(MediaType.TEXT_PLAIN)
    public String getOrderBy4Search(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getOrderBy4Search(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/saveOrderBy4Search")
    @Produces(MediaType.TEXT_PLAIN)
    public String saveOrderBy4Search(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).saveOrderBy4Search(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getGroupTree")
    @Produces(MediaType.TEXT_PLAIN)
    public String getGroupTree(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getGroupTree(ParamUtil.request2Map(request), user);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getShowCol")
    @Produces(MediaType.TEXT_PLAIN)
    public String getShowCol(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).getShowCol(ParamUtil.request2Map(request), user, request);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    @GET
    @Path("/getShowCol4Protal")
    @Produces(MediaType.TEXT_PLAIN)
    public String getShowCol4Protal(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);

            Map<String, Object> params = ParamUtil.request2Map(request);
            params.put("isProtal", true);

            apidatas = getService(user).getShowCol(params, user, request);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }

    /**
     * 同步默认排序设置到所有人
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/syncOrderBy2All")
    @Produces(MediaType.TEXT_PLAIN)
    public String syncOrderBy2All(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            apidatas = getService(user).syncOrderBy2All(ParamUtil.request2Map(request), user,request);
        } catch (Exception e) {
            logger.writeLog(e);
            apidatas.put("status", "-1");
            apidatas.put("message", e.getMessage());
        }
        return JSONObject.toJSONString(apidatas);
    }
}
