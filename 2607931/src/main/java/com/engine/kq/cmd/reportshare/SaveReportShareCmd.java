package com.engine.kq.cmd.reportshare;

import com.alibaba.fastjson.JSON;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.SimpleBizLogger;
import com.engine.common.constant.BizLogSmallType4Hrm;
import com.engine.common.constant.BizLogType;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤报表共享设置--保存
 */
public class SaveReportShareCmd extends AbstractCommonCommand<Map<String, Object>> {

    private SimpleBizLogger logger;

    public SaveReportShareCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
        this.logger = new SimpleBizLogger();

        String resourceType = Util.null2String(params.get("resourceType"));//类型：1-人力资源、7-分权管理员、8-系统管理员
        String resourceIds = Util.null2String(params.get("resourceIds"));//对象(类型为人力资源时)
        String resourceManagerIds = Util.null2String(params.get("resourceManagerIds"));//对象(类型为分权管理员时)
        BizLogContext bizLogContext = new BizLogContext();
        bizLogContext.setLogType(BizLogType.HRM_ENGINE);//模块类型
        bizLogContext.setBelongType(BizLogSmallType4Hrm.HRM_ENGINE_KQ_REPORTSHARE);//所属大类型
        bizLogContext.setLogSmallType(BizLogSmallType4Hrm.HRM_ENGINE_KQ_REPORTSHARE);//当前小类型
        bizLogContext.setParams(params);//当前request请求参数
        logger.setUser(user);//当前操作人
        String mainSql = "select * from kq_ReportShare where resourceType="+resourceType;
        if(resourceType.equals("1")){
            mainSql += " and resourceId in ("+resourceIds+")";
        }else if(resourceType.equals("7")){
            mainSql += " and resourceId in ("+resourceManagerIds+")";
        }
        logger.setMainSql(mainSql, "id");//主表sql
        logger.setMainPrimarykey("id");//主日志表唯一key
        logger.setMainTargetNameMethod("com.engine.kq.util.KQTransMethod.getTargetName4ReportShare","column:reportName+column:resourceType+column:resourceId+"+user.getLanguage());
        logger.before(bizLogContext);
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public List<BizLogContext> getLogContexts() {
        return logger.getBizLogContexts();
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        boolean canSave = HrmUserVarify.checkUserRight("KQ:ReportShare", user);//是否具有保存的权限
        if (!canSave) {
            resultMap.put("status", "-1");
            resultMap.put("message", SystemEnv.getHtmlLabelName(2012, user.getLanguage()));//没有权限
            return resultMap;
        }
        try {
            String reportName = Util.null2String(params.get("reportName"));//报表名称：0-全部、1-考勤报表、2-月日历考勤报表、3-原始打卡记录、4-员工假期余额
            String resourceType = Util.null2String(params.get("resourceType"));//类型：1-人力资源、7-分权管理员、8-系统管理员
            String resourceIds = Util.null2String(params.get("resourceIds"));//对象(类型为人力资源时)
            String resourceManagerIds = Util.null2String(params.get("resourceManagerIds"));//对象(类型为分权管理员时)
            String shareLevel = Util.null2String(params.get("shareLevel"));//共享级别：分部、部门、人力资源、岗位、所有人
            String subcomId = Util.null2String(params.get("subcomId"));//分部ID
            String deptId = Util.null2String(params.get("deptId"));//部门ID
            String jobtitleId = Util.null2String(params.get("jobtitleId"));//岗位ID
            String userId = Util.null2String(params.get("userId"));//人力资源ID
            String forAllUser = shareLevel.equals("4") ? "1" : "0";
            String userLevel = Util.null2String(params.get("userLevel"));//安全级别范围
            String userLevelto = Util.null2String(params.get("userLevelto"));//安全级别范围

            String subcomIdCustom = Util.null2String(params.get("subcomIdCustom"));//自定义分部ID
            String deptIdCustom = Util.null2String(params.get("deptIdCustom"));//自定义部门ID

            boolean flag = true;//SQL语句是否执行成功
            RecordSet recordSet = new RecordSet();
            List<String> reportList = new ArrayList<String>();
            if (reportName.equals("0")) {
                reportList.add("1");
                reportList.add("2");
                reportList.add("3");
                reportList.add("4");
            } else {
                reportList.add(reportName);
            }

            if (resourceType.equals("7")) {
                resourceIds = resourceManagerIds;
            } else if (resourceType.equals("8")) {
                resourceIds = "1";
            } else {
                resourceIds = resourceIds;
            }

            for (int x = 0; x < reportList.size(); x++) {
                List<String> resourceIdList = Util.TokenizerString(resourceIds, ",");
                for (int i = 0; i < resourceIdList.size(); i++) {
                    //qczj 增加一个安全级别字段

                    String sql = "insert into kq_ReportShare(reportName,resourceType,resourceId,shareLevel,subcomId,deptId,jobtitleId,userId,forAllUser,userLevel) " +
                            "values(?,?,?,?,?,?,?,?,?,?)";

                    flag = recordSet.executeUpdate(sql, reportList.get(x), resourceType, resourceIdList.get(i), shareLevel, subcomId, deptId, jobtitleId, userId, forAllUser,userLevel+","+userLevelto);
                    if (!flag) {
                        resultMap.put("sign", "-1");
                        resultMap.put("message", SystemEnv.getHtmlLabelName(534532, user.getLanguage()));//保存失败
                        return resultMap;
                    }
                }
                if ("1024".equals(resourceType)){
                    //当对象类型为 安全级别
                    //z 增加两个字段，subcomIdCustom,deptIdCustom
                    String sql = "insert into kq_ReportShare(reportName,resourceType,resourceId,shareLevel,subcomId,deptId,jobtitleId,userId,forAllUser,userLevel,subcomIdCustom,deptIdCustom) " +
                            "values(?,?,?,?,?,?,?,?,?,?,?,?)";
                    new BaseBean().writeLog("==zj==(安全级别增加)" + JSON.toJSONString(sql)+ " | " +reportList.get(x) + " | " + resourceType+ " | " + " | " +shareLevel+ " | " +subcomId+ " | " +deptId+ " | " +jobtitleId+ " | " +userId+ " | " +forAllUser+ " | " +userLevel);
                    flag = recordSet.executeUpdate(sql, reportList.get(x), resourceType, "", shareLevel, subcomId, deptId, jobtitleId, userId, forAllUser,userLevel+","+userLevelto,subcomIdCustom,deptIdCustom);

                }
            }


            if (true) {
                resultMap.put("sign", "1");
                resultMap.put("message", SystemEnv.getHtmlLabelName(83551, user.getLanguage()));//保存成功
            }
        } catch (Exception e) {
            writeLog(e);
        }
        return resultMap;
    }
}
