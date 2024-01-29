package com.engine.hrm.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.customization.qc2269712.CheckPermissionUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import com.alibaba.fastjson.JSONObject;
import com.engine.common.util.ParamUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.hrm.service.ImportResourceService;
import com.engine.hrm.service.impl.ImportResourceServiceImpl;
import weaver.systeminfo.SystemEnv;

/**
 * 人员导入
 *
 * @author lvyi
 */
public class ImportResourceAction {
    private BaseBean logger = new BaseBean();

    private ImportResourceService getService(User user) {
        return (ImportResourceServiceImpl) ServiceUtil.getService(ImportResourceServiceImpl.class, user);
    }

    /**
     * 是否有权限
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getHasRight")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHasRight(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            String type = Util.null2String(request.getParameter("type"));
            //任何人都能够看到
            boolean hasRight = true;
         
            apidatas.put("hasRight", hasRight);
            apidatas.put("status", "1");
        } catch (Exception e) {
            apidatas.put("status", "-1");
            logger.writeLog(e);
        }
        return JSONObject.toJSONString(apidatas);
    }


    /**
     * 是否有基础数据导入权限
     *
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getBasicDataImportHasRight")
    @Produces(MediaType.TEXT_PLAIN)
    public String getBasicDataImportHasRight(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            String type = Util.null2String(request.getParameter("type"));
            boolean hasRight = false;
            //增加权限控制，只有管理员才能查看
            hasRight = "sysadmin".equals(user.getLoginid().toLowerCase());
            hasRight = true;
            if (hasRight) {
                List<Map<String,Object>> cardConfigList = new ArrayList<>();
                //组织
                Map<String, Object> cardConfigMap1 = new HashMap<String, Object>();
                cardConfigMap1.put("icon","icon-coms-Department-number");
                cardConfigMap1.put("title",SystemEnv.getHtmlLabelName(376, user.getLanguage()));
                cardConfigMap1.put("linkName",SystemEnv.getHtmlLabelName(125432, user.getLanguage()));
                cardConfigMap1.put("subTitle",SystemEnv.getHtmlLabelName(33596, user.getLanguage()));
                cardConfigMap1.put("bgColor","#92B75B");
                cardConfigMap1.put("url","/spa/hrm/engine.html#/hrmengine/organization");
                cardConfigList.add(cardConfigMap1);

                //岗位
                Map<String, Object> cardConfigMap2 = new HashMap<String, Object>();
                cardConfigMap2.put("icon","icon-coms-hrm");
                cardConfigMap2.put("title",SystemEnv.getHtmlLabelName(6086, user.getLanguage()));
                cardConfigMap2.put("linkName",SystemEnv.getHtmlLabelName(125443, user.getLanguage()));
                cardConfigMap2.put("subTitle",SystemEnv.getHtmlLabelName(382806, user.getLanguage()));
                cardConfigMap2.put("bgColor","#49B2FE");
                cardConfigMap2.put("url","/spa/hrm/engine.html#/hrmengine/post");
                cardConfigList.add(cardConfigMap2);

                //人员
                Map<String, Object> cardConfigMap3 = new HashMap<String, Object>();
                cardConfigMap3.put("icon","icon-coms-crm");
                cardConfigMap3.put("title",SystemEnv.getHtmlLabelName(1867, user.getLanguage()));
                cardConfigMap3.put("linkName",SystemEnv.getHtmlLabelName(17887, user.getLanguage()));
                cardConfigMap3.put("subTitle",SystemEnv.getHtmlLabelName(33596, user.getLanguage()));
                cardConfigMap3.put("bgColor","#51A39A");
                cardConfigMap3.put("url","/spa/hrm/engine.html#/hrmengine/organization");
                cardConfigList.add(cardConfigMap3);

                //常用组
                Map<String, Object> cardConfigMap4 = new HashMap<String, Object>();
                cardConfigMap4.put("icon","icon-coms-meeting");
                cardConfigMap4.put("title",SystemEnv.getHtmlLabelName(81554, user.getLanguage()));
                cardConfigMap4.put("linkName",SystemEnv.getHtmlLabelName(125445, user.getLanguage()));
                cardConfigMap4.put("subTitle",SystemEnv.getHtmlLabelName(18166, user.getLanguage()));
                cardConfigMap4.put("bgColor","#E6A845");
                cardConfigMap4.put("url","/spa/hrm/index_mobx.html#/main/hrm/group");
                cardConfigList.add(cardConfigMap4);

                //个人数据
                Map<String, Object> cardConfigMap5 = new HashMap<String, Object>();
                cardConfigMap5.put("icon","icon-coms-Proportion");
                cardConfigMap5.put("title",SystemEnv.getHtmlLabelName(125429, user.getLanguage()));
                cardConfigMap5.put("linkName",SystemEnv.getHtmlLabelName(125678, user.getLanguage()));
                cardConfigMap5.put("subTitle",SystemEnv.getHtmlLabelName(33596, user.getLanguage()));
                cardConfigMap5.put("bgColor","#54CF81");
                cardConfigMap5.put("url","/spa/hrm/engine.html#/hrmengine/organization");
                cardConfigList.add(cardConfigMap5);

                //行政区域
                Map<String, Object> cardConfigMap6 = new HashMap<String, Object>();
                cardConfigMap6.put("icon","icon-coms-position");
                cardConfigMap6.put("title",SystemEnv.getHtmlLabelName(126167, user.getLanguage()));
                cardConfigMap6.put("linkName",SystemEnv.getHtmlLabelName(126168, user.getLanguage()));
                cardConfigMap6.put("subTitle",SystemEnv.getHtmlLabelName(126169, user.getLanguage()));
                cardConfigMap6.put("bgColor","#2B76FF");
                cardConfigMap6.put("url","/spa/hrm/engine.html#/hrmengine/adareaset");
                cardConfigList.add(cardConfigMap6);

                //办公地点
                Map<String, Object> cardConfigMap7 = new HashMap<String, Object>();
                cardConfigMap7.put("icon","icon-coms-Big-screen-sign");
                cardConfigMap7.put("title",SystemEnv.getHtmlLabelName(15712, user.getLanguage()));
                cardConfigMap7.put("linkName",SystemEnv.getHtmlLabelName(125447, user.getLanguage()));
                cardConfigMap7.put("subTitle",SystemEnv.getHtmlLabelName(125448, user.getLanguage()));
                cardConfigMap7.put("bgColor","#E6A845");
                cardConfigMap7.put("url","/spa/hrm/engine.html#/hrmengine/officeaddress");
                cardConfigList.add(cardConfigMap7);

                //专业
                Map<String, Object> cardConfigMap8 = new HashMap<String, Object>();
                cardConfigMap8.put("icon","icon-coms-Sign");
                cardConfigMap8.put("title",SystemEnv.getHtmlLabelName(803, user.getLanguage()));
                cardConfigMap8.put("linkName",SystemEnv.getHtmlLabelName(125449, user.getLanguage()));
                cardConfigMap8.put("subTitle",SystemEnv.getHtmlLabelName(16463, user.getLanguage()));
                cardConfigMap8.put("bgColor","#49B2FE");
                cardConfigMap8.put("url","/spa/hrm/engine.html#/hrmengine/major");
                cardConfigList.add(cardConfigMap8);

                apidatas.put("cardConfig", cardConfigList);
                apidatas.put("hasRight", hasRight);
                apidatas.put("status", "1");
            }else{
                apidatas.put("status", "-1");
                return JSONObject.toJSONString(apidatas);
            }

        } catch (Exception e) {
            apidatas.put("status", "-1");
            e.printStackTrace();
            logger.writeLog(e);
        }
        return JSONObject.toJSONString(apidatas);
    }

    /***
     * 获取导入人员表单
     * @param request
     * @param response
     * @return
     */
    @GET
    @Path("/getImportForm")
    @Produces(MediaType.TEXT_PLAIN)
    public String getImportForm(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            String importType = Util.null2String(request.getParameter("importType"));

            if (importType.equals("resource")) {
                    if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user) && HrmUserVarify.checkUserRight("HrmCompanyEdit:Edit", user) && HrmUserVarify.checkUserRight("HrmSubCompanyAdd:Add", user) && HrmUserVarify.checkUserRight("HrmDepartmentAdd:Add", user)) {
                    apidatas = getService(user).getImportResourceForm(ParamUtil.request2Map(request), user);
                }
            }else if (importType.equals("company")) {
                //分部部门导入
                if( HrmUserVarify.checkUserRight("HrmCompanyEdit:Edit", user) && HrmUserVarify.checkUserRight("HrmSubCompanyAdd:Add", user) && HrmUserVarify.checkUserRight("HrmDepartmentAdd:Add", user)) {
                    apidatas = getService(user).getImportCompanyForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("jobtitle")) {
                if(HrmUserVarify.checkUserRight("HrmJobTitlesAdd:Add", user)) {
                    apidatas = getService(user).getImportJobTitleForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("group")) {
                //常用组设置
                if(HrmUserVarify.checkUserRight("CustomGroup:Edit", user)) {
                    apidatas = getService(user).getImportGroupForm(ParamUtil.request2Map(request), user);
                    }
            } else if (importType.equals("resourcedetial")) {
                if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                    apidatas = getService(user).getImportResourceDetialForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("location")) {
                if(HrmUserVarify.checkUserRight("HrmLocationsEdit: Edit", user)) {
                    apidatas = getService(user).getImportLocationForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("special")) {
                if(HrmUserVarify.checkUserRight("HrmSpecialityEdit:Edit", user)) {
                    apidatas = getService(user).getImportSpecialForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("area")) {
                if(HrmUserVarify.checkUserRight("HrmCountriesAdd:Add", user) && HrmUserVarify.checkUserRight("HrmCityAdd:Add", user) && HrmUserVarify.checkUserRight("HrmProvinceAdd:Add", user)) {
                    apidatas = getService(user).getImportAreaForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("matrix")) {
                 // 维护者为完全控制时 也可以导入
                 apidatas = getService(user).getImportMatrixForm(ParamUtil.request2Map(request), user);

            } else if (importType.equals("photo")) {
                if(HrmUserVarify.checkUserRight("PersonnelOrganization:Batchmaintenance", user)) {
                    apidatas = getService(user).getImportPhotoForm(ParamUtil.request2Map(request), user);
                }
            } else if (importType.equals("groupMember")) {
                apidatas = getService(user).getImportGroupMemberForm(ParamUtil.request2Map(request), user);
            } else if (importType.equals("sensitiveword")) {
                apidatas = getService(user).getImportSensitiveWordForm(ParamUtil.request2Map(request), user);
            }
			if (apidatas.size() == 0) {
                apidatas.put("status", "-1");
                apidatas.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));
            }
        } catch (Exception e) {
            apidatas.put("status", "-1");
            logger.writeLog(e);
        }
        return JSONObject.toJSONString(apidatas, SerializerFeature.DisableCircularReferenceDetect);
    }

    /***
     * 导入人员
     * @param request
     * @param response
     * @return
     */
    @POST
    @Path("/saveImport")
    @Produces(MediaType.TEXT_PLAIN)
    public String saveImport(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        Map<String, Object> apidatas = new HashMap<String, Object>();
        try {
            User user = HrmUserVarify.getUser(request, response);
            String importType = Util.null2String(request.getParameter("importType"));
            String pIdName = "hrm" + importType + "PId";
            request.getSession(true).removeAttribute(pIdName);
            if (importType.equals("resource")) {
                if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                    apidatas = getService(user).saveImportResource(ParamUtil.request2Map(request), request, user);
                }
            }else if (importType.equals("matrix")) {
//                if(HrmUserVarify.checkUserRight("Matrix:Maint", user)) {
                    apidatas = getService(user).saveImportMatrix(ParamUtil.request2Map(request), request, user);
//                }
            } else if (importType.equals("photo")) {
                if(HrmUserVarify.checkUserRight("PersonnelOrganization:Batchmaintenance", user)) {
                    apidatas = getService(user).saveImportPhoto(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("company")) {
                if(HrmUserVarify.checkUserRight("HrmCompanyEdit:Edit", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("jobtitle")) {
                if(HrmUserVarify.checkUserRight("HrmJobTitlesAdd:Add", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("group")) {
                apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
            } else if (importType.equals("resourcedetial")) {
                if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("location")) {
                if(HrmUserVarify.checkUserRight("HrmLocationsEdit: Edit", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("special")) {
                if(HrmUserVarify.checkUserRight("HrmSpecialityEdit:Edit", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("area")) {
                if(HrmUserVarify.checkUserRight("HrmCountriesAdd:Add", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else if (importType.equals("groupMember")) {
                apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
            } else if (importType.equals("sensitiveword")) {
                if(HrmUserVarify.checkUserRight("SensitiveWord:Set", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            } else {
                if(HrmUserVarify.checkUserRight("HrmResourceEdit:Edit", user)) {
                    apidatas = getService(user).saveImport(ParamUtil.request2Map(request), request, user);
                }
            }

            apidatas.put("status", "1");
        } catch (Exception e) {
            apidatas.put("status", "-1");
            logger.writeLog(e);
            e.printStackTrace();
        }
        return JSONObject.toJSONString(apidatas);
    }

}
