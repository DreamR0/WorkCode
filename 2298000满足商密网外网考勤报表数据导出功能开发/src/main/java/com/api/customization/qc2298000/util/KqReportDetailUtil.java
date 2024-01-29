package com.api.customization.qc2298000.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.kq.biz.KQOvertimeRulesBiz;
import com.engine.kq.enums.DurationTypeEnum;
import com.engine.kq.util.ExcelUtil;
import com.engine.kq.util.KQDurationCalculatorUtil;
import com.engine.kq.wfset.util.KQFlowUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class KqReportDetailUtil {
    private String selectSql = "";
    private String whereSql = "";
    private String orderSql="";
    private String groupBySql = "";

    /**
     * 获取请假明细
     * @param params
     * @param user
     */
    public Map<String,Object> getKqReportDetail(Map<String,Object> params, HttpServletRequest request, HttpServletResponse response,User user){
        JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
        new BaseBean().writeLog("==zj==(jsonObj)" + JSON.toJSONString(jsonObj));
        RecordSet rs = new RecordSet();
        RecordSet rw = new RecordSet();
        LinkedHashMap<String,Object> workbook = new LinkedHashMap<String,Object>();
        Map<String,Object> retmap = new HashMap<String,Object>();
        Map<String,Object> sheet = new HashMap<String,Object>();//请假明细
        Map<String,Object> sheetLeaveBack = new HashMap<String,Object>();//销假明细
        Map<String,Object> title = null;
        List<Object> data = null;
        List<List<Object>> dataList = new ArrayList<List<Object>>();
        List<Object> titleList = new ArrayList<Object>();
        List<Object> lsSheet = new ArrayList<Object>();
try {

    //时间范围
    String fromDate = Util.null2String(jsonObj.get("fromDate"));
    String toDate = Util.null2String(jsonObj.get("toDate"));
    String typeselect = Util.null2String(jsonObj.get("typeselect"));
    if (typeselect.length() == 0) typeselect = "3";
    if (!typeselect.equals("") && !typeselect.equals("0") && !typeselect.equals("6")) {
        if (typeselect.equals("1")) {
            fromDate = TimeUtil.getCurrentDateString();
            toDate = TimeUtil.getCurrentDateString();
        } else {
            fromDate = TimeUtil.getDateByOption(typeselect, "0");
            toDate = TimeUtil.getDateByOption(typeselect, "1");
        }
    }

    //人员状态
    String status = Util.null2String(jsonObj.get("status"));   //人员岗位状态
    String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));    //人员分部
    String departmentId = Util.null2String(jsonObj.get("departmentId"));    //人员部门
    String resourceId = Util.null2String(jsonObj.get("resourceId"));        //人员id
    String allLevel = Util.null2String(jsonObj.get("allLevel"));            //所有等级
    String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));      //是否无账号
    String viewScope = Util.null2String(jsonObj.get("viewScope"));          //查看范围

    //qc2298000 导出数据销假数据处理
    selectSql = "select a.*,b.*,c.*,d.* from hrmresource a left join kq_flow_split_leave b on a.id = b.resourceid"+
                " left join hrmdepartment c on b.departmentid = c.id left join KQ_LeaveRules d on b.newleavetype = d.id ";

    whereSql = " where 1=1 and a.id = b.resourceid and b.belongDate between '"+fromDate+"' and '"+toDate+"'";

    //人员岗位状态
    if(status.length()>0){
        if (!status.equals("8") && !status.equals("9")) {
            whereSql += " and a.status = "+status+ "";
        }else if (status.equals("8")) {
            whereSql += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
        }
    }

    //如果有分部
    if (subCompanyId.length() > 0){
        whereSql += " and a.subCompanyid in(" + departmentId + ") ";
    }

    //如果有部门
    if (departmentId.length() > 0) {
        whereSql += " and a.departmentid in(" + departmentId + ") ";
    }

    //如果查询人员
    if (resourceId.length() > 0) {
        whereSql += " and a.id in(" + resourceId + ") ";
    }

    //查看范围--人员
    if (viewScope.equals("4")) {//我的下属
        if (allLevel.equals("1")) {//所有下属
            whereSql += " and a.managerstr like '%," + user.getUID() + ",%'";
        } else {
            whereSql += " and a.managerid=" + user.getUID();//直接下属
        }
    }

    //是否是无账号人员
    if (!"1".equals(isNoAccount)) {
        whereSql += " and a.loginid is not null " +  " and a.loginid<>'' ";
    }

    String sql = selectSql+whereSql;
    new BaseBean().writeLog("==zj==(请假明细导出sql)" + sql);
    rs.executeQuery(sql);
    sheet.put("sheetTitle","请假明细表");
    sheet.put("sheetName","请假明细表");
    while (rs.next()){
        //添加请假明细表数据
        data = new ArrayList<Object>();
        data.add(Util.null2String(rs.getString("requestid")));   //流程requestid
        data.add(Util.null2String(rs.getString("lastname")));   //添加人员姓名
        data.add(Util.null2String(rs.getString("mobile")));   //添加人员手机号（唯一标识）
        data.add(Util.null2String(rs.getString("fromDate")));   //添加拆分后请假开始日期
        data.add(Util.null2String(rs.getString("fromTime")));   //添加拆分后请假开始时间
        data.add(Util.null2String(rs.getString("toDate")));   //添加拆分后请假结束日期
        data.add(Util.null2String(rs.getString("toTime")));   //添加拆分后请假结束时间
        data.add(Util.null2String(rs.getString("duration")));   //添加拆分后时长
        data.add(Util.null2String(rs.getString("fromDatedb")));   //添加原始表单请假开始日期
        data.add(Util.null2String(rs.getString("fromTimedb")));   //添加原始表单请假原始表单开始时间
        data.add(Util.null2String(rs.getString("toDatedb")));   //添加原始表单请假结束日期
        data.add(Util.null2String(rs.getString("toTimedb")));   //添加原始表单请假结束时间
        data.add(Util.null2String(rs.getString("durationdb")));   //添加原始表单数据，时长
        data.add(Util.null2String(rs.getString("leavename")));   //添加请假类型名称
        data.add(Util.null2String(rs.getString("tablenamedb")));   //添加表单名称
        data.add(Util.null2String(rs.getString("belongdate")));   //归属日期
        data.add(Util.null2String(rs.getString("durationrule")));   //单位规则
        data.add(Util.null2String(rs.getString("leavebackrequestid")));   //销假requestid
        dataList.add(data);
    }
    new BaseBean().writeLog("==zj==(dataList--请假明细表)"+JSON.toJSONString(dataList));
    sheet.put("dataList",dataList);

    //添加请假明细列
    List<String> showCol = new ArrayList<String>();
    showCol = showLeaveCol();
    for (String colName : showCol){
        title = new HashMap<String,Object>();
        title.put("rowSpan",4);
        title.put("width",10240);
        title.put("title",colName);
        titleList.add(title);
    }

    sheet.put("titleList",titleList);
    sheet.put("createFile","1");
    lsSheet.add(sheet);     //添加请假明细表
    lsSheet  = getOverTimeDetail(jsonObj,lsSheet,user);  //添加加班明细表
    lsSheet = getLeaveBackDetail(jsonObj,lsSheet,user);  //添加销假明细表
    workbook.put("sheet",lsSheet);
    String fileName = "请假加班销假明细表 "+fromDate+" "+toDate;
    workbook.put("fileName",fileName);

    ExcelUtil ExcelUtil = new ExcelUtil();
    new BaseBean().writeLog("==zj==(自定义导出请假加班明细表)" + JSON.toJSONString(workbook));
    Map<String, Object> exportMap1 = ExcelUtil.export(workbook, request, response);
    retmap.putAll(exportMap1);
    retmap.put("code", "1");
    return retmap;

}catch (Exception e){
    retmap.put("code", "-1");
    retmap.put("message", e);
    new BaseBean().writeLog("==zj==(导出请假明细错误)" + e);

    return retmap;
}

    }

    /**
     * 导出请假明细表要展示的列名
     * @return
     */
    public List<String> showLeaveCol(){
        ArrayList<String> showCol = new ArrayList<String>();
        showCol.add("流程id");
        showCol.add("姓名");
        showCol.add("手机号");
        showCol.add("拆分开始日期");
        showCol.add("拆分开始时间");
        showCol.add("拆分结束日期");
        showCol.add("拆分结束时间");
        showCol.add("拆分时长");
        showCol.add("原始表单开始日期");
        showCol.add("原始表单开始时间");
        showCol.add("原始表单结束日期");
        showCol.add("原始表单结束时间");
        showCol.add("原始表单时长");
        showCol.add("请假类型");
        showCol.add("表单名称");
        showCol.add("归属日期");
        showCol.add("单位规则");
        showCol.add("销假流程id");

        return showCol;
    }
    /**
     * 销假明细表
     * @param lsSheet
     */
    public List<Object> getLeaveBackDetail(JSONObject jsonObj, List<Object> lsSheet, User user){
        String selectSql ="";
        String whereSql ="";

        RecordSet rs = new RecordSet();
        LinkedHashMap<String,Object> workbook = new LinkedHashMap<String,Object>();
        Map<String,Object> retmap = new HashMap<String,Object>();
        Map<String,Object> sheet = new HashMap<String,Object>();
        Map<String,Object> title = null;
        List<Object> data = null;
        List<List<Object>> dataList = new ArrayList<List<Object>>();
        List<Object> titleList = new ArrayList<Object>();
        try{
            //时间范围
            String fromDate = Util.null2String(jsonObj.get("fromDate"));
            String toDate = Util.null2String(jsonObj.get("toDate"));
            String typeselect = Util.null2String(jsonObj.get("typeselect"));
            if (typeselect.length() == 0) typeselect = "3";
            if (!typeselect.equals("") && !typeselect.equals("0") && !typeselect.equals("6")) {
                if (typeselect.equals("1")) {
                    fromDate = TimeUtil.getCurrentDateString();
                    toDate = TimeUtil.getCurrentDateString();
                } else {
                    fromDate = TimeUtil.getDateByOption(typeselect, "0");
                    toDate = TimeUtil.getDateByOption(typeselect, "1");
                }
            }

            //人员状态
            String status = Util.null2String(jsonObj.get("status"));   //人员岗位状态
            String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));    //人员分部
            String departmentId = Util.null2String(jsonObj.get("departmentId"));    //人员部门
            String resourceId = Util.null2String(jsonObj.get("resourceId"));        //人员id
            String allLevel = Util.null2String(jsonObj.get("allLevel"));            //所有等级
            String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));      //是否无账号
            String viewScope = Util.null2String(jsonObj.get("viewScope"));          //查看范围


            selectSql = "select a.*,b.*,c.*,d.* from hrmresource a left join kq_flow_split_leaveback b on a.id = b.resourceid"+
                    " left join hrmdepartment c on b.departmentid = c.id left join KQ_LeaveRules d on b.newleavetype = d.id ";

            whereSql = " where 1=1 and a.id = b.resourceid and b.belongDate between '"+fromDate+"' and '"+toDate+"'";

            //人员岗位状态
            if(status.length()>0){
                if (!status.equals("8") && !status.equals("9")) {
                    whereSql += " and a.status = "+status+ "";
                }else if (status.equals("8")) {
                    whereSql += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
                }
            }

            //如果有分部
            if (subCompanyId.length() > 0){
                whereSql += " and subCompanyId in(" + departmentId + ") ";
            }

            //如果有部门
            if (departmentId.length() > 0) {
                whereSql += " and departmentId in(" + departmentId + ") ";
            }

            //如果查询人员
            if (resourceId.length() > 0) {
                whereSql += " and resourceid in(" + resourceId + ") ";
            }

            //查看范围--人员
            if (viewScope.equals("4")) {//我的下属
                if (allLevel.equals("1")) {//所有下属
                    whereSql += " and a.managerstr like '%," + user.getUID() + ",%'";
                } else {
                    whereSql += " and a.managerid=" + user.getUID();//直接下属
                }
            }

            //是否是无账号人员
            if (!"1".equals(isNoAccount)) {
                whereSql += " and a.loginid is not null " +  " and a.loginid<>'' ";
            }

            sheet.put("sheetTitle","销假明细表");
            sheet.put("sheetName","销假明细表");
            String sql = selectSql + whereSql;
            new BaseBean().writeLog("==zj==(销假明细表导出sql)" + sql);
            rs.executeQuery(sql);
            while (rs.next()){
                //添加加班明细表数据
                data = new ArrayList<Object>();
                data.add(Util.null2String(rs.getString("requestid")));   //流程requestid
                data.add(Util.null2String(rs.getString("lastname")));   //添加人员姓名
                data.add(Util.null2String(rs.getString("mobile")));   //添加人员手机号（唯一标识）
                data.add(Util.null2String(rs.getString("fromDate")));   //添加拆分后请假开始日期
                data.add(Util.null2String(rs.getString("fromTime")));   //添加拆分后请假开始时间
                data.add(Util.null2String(rs.getString("toDate")));   //添加拆分后请假结束日期
                data.add(Util.null2String(rs.getString("toTime")));   //添加拆分后请假结束时间
                data.add(Util.null2String(rs.getString("duration")));   //添加拆分后时长
                data.add(Util.null2String(rs.getString("fromDatedb")));   //添加原始表单请假开始日期
                data.add(Util.null2String(rs.getString("fromTimedb")));   //添加原始表单请假原始表单开始时间
                data.add(Util.null2String(rs.getString("toDatedb")));   //添加原始表单请假结束日期
                data.add(Util.null2String(rs.getString("toTimedb")));   //添加原始表单请假结束时间
                data.add(Util.null2String(rs.getString("durationdb")));   //添加原始表单数据，时长
                data.add(Util.null2String(rs.getString("leavename")));   //添加请假类型名称
                data.add(Util.null2String(rs.getString("tablenamedb")));   //添加表单名称
                data.add(Util.null2String(rs.getString("belongdate")));   //归属日期
                data.add(Util.null2String(rs.getString("durationrule")));   //单位规则
                data.add(Util.null2String(rs.getString("leavebackrequestid")));   //单位规则
                dataList.add(data);

            }
            sheet.put("dataList",dataList);

            //添加列
            List<String> showCol = new ArrayList<String>();
            showCol = showLeaveBackCol();
            for (String colName : showCol){
                title = new HashMap<String,Object>();
                title.put("rowSpan",4);
                title.put("width",10240);
                title.put("title",colName);
                titleList.add(title);
            }
            sheet.put("titleList",titleList);
            sheet.put("createFile","1");

            lsSheet.add(sheet);

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(销假明细表--报错)" + e);
        }
        return lsSheet;
    }



    /**
     * 加班明细表
     * @param lsSheet
     */
    public List<Object> getOverTimeDetail(JSONObject jsonObj, List<Object> lsSheet, User user){
        String selectSql ="";
        String whereSql ="";

        RecordSet rs = new RecordSet();
        LinkedHashMap<String,Object> workbook = new LinkedHashMap<String,Object>();
        Map<String,Object> retmap = new HashMap<String,Object>();
        Map<String,Object> sheet = new HashMap<String,Object>();
        Map<String,Object> title = null;
        List<Object> data = null;
        List<List<Object>> dataList = new ArrayList<List<Object>>();
        List<Object> titleList = new ArrayList<Object>();
        try{
            //时间范围
            String fromDate = Util.null2String(jsonObj.get("fromDate"));
            String toDate = Util.null2String(jsonObj.get("toDate"));
            String typeselect = Util.null2String(jsonObj.get("typeselect"));
            if (typeselect.length() == 0) typeselect = "3";
            if (!typeselect.equals("") && !typeselect.equals("0") && !typeselect.equals("6")) {
                if (typeselect.equals("1")) {
                    fromDate = TimeUtil.getCurrentDateString();
                    toDate = TimeUtil.getCurrentDateString();
                } else {
                    fromDate = TimeUtil.getDateByOption(typeselect, "0");
                    toDate = TimeUtil.getDateByOption(typeselect, "1");
                }
            }

            //人员状态
            String status = Util.null2String(jsonObj.get("status"));   //人员岗位状态
            String subCompanyId = Util.null2String(jsonObj.get("subCompanyId"));    //人员分部
            String departmentId = Util.null2String(jsonObj.get("departmentId"));    //人员部门
            String resourceId = Util.null2String(jsonObj.get("resourceId"));        //人员id
            String allLevel = Util.null2String(jsonObj.get("allLevel"));            //所有等级
            String isNoAccount = Util.null2String(jsonObj.get("isNoAccount"));      //是否无账号
            String viewScope = Util.null2String(jsonObj.get("viewScope"));          //查看范围


            selectSql = "select a.lastname,a.mobile,c.fromdatedb,c.fromtimedb,c.todatedb,c.totimedb,c.duration_min,c.belongdate,c.paidleaveenable,c.computingmode,c.changetype,c.durationrule from hrmresource a left join hrmdepartment b on a.departmentid = b.id left join kq_flow_overtime c on a.id = c.resourceid";
            whereSql = " where 1=1 and a.id = c.resourceid and a.departmentid = b.id and c.belongdate between '"+fromDate+"' and '"+toDate+"'";

            //人员岗位状态
            if(status.length()>0){
                if (!status.equals("8") && !status.equals("9")) {
                    whereSql += " and a.status = "+status+ "";
                }else if (status.equals("8")) {
                    whereSql += " and (a.status = 0 or a.status = 1 or a.status = 2 or a.status = 3) ";
                }
            }

            //如果有分部
            if (subCompanyId.length() > 0){
                whereSql += " and a.departmentid in(" + departmentId + ") ";
            }

            //如果有部门
            if (departmentId.length() > 0) {
                whereSql += " and a.departmentid in(" + departmentId + ") ";
            }

            //如果查询人员
            if (resourceId.length() > 0) {
                whereSql += " and a.id in(" + resourceId + ") ";
            }

            //查看范围--人员
            if (viewScope.equals("4")) {//我的下属
                if (allLevel.equals("1")) {//所有下属
                    whereSql += " and a.managerstr like '%," + user.getUID() + ",%'";
                } else {
                    whereSql += " and a.managerid=" + user.getUID();//直接下属
                }
            }

            //是否是无账号人员
            if (!"1".equals(isNoAccount)) {
                whereSql += " and a.loginid is not null " +  " and a.loginid<>'' ";
            }

            sheet.put("sheetTitle","加班明细表");
            sheet.put("sheetName","加班明细表");
            String sql = selectSql + whereSql;
            new BaseBean().writeLog("==zj==(加班明细表导出sql)" + sql);
            rs.executeQuery(sql);
            while (rs.next()){
                //添加加班明细表数据
                data = new ArrayList<Object>();
                data.add(Util.null2String(rs.getString("lastname")));   //添加人员姓名
                data.add(Util.null2String(rs.getString("mobile")));   //添加人员手机号
                data.add(Util.null2String(rs.getString("fromDatedb")));   //添加加班开始日期
                data.add(Util.null2String(rs.getString("fromTimedb")));   //添加加班开始时间
                data.add(Util.null2String(rs.getString("toDatedb")));   //添加加班结束日期
                data.add(Util.null2String(rs.getString("toTimedb")));   //添加加班结束时间

                //将分钟数转加班时长
                    data.add(Util.null2String(rs.getString("duration_min")));

                    //加班归属日期
                data.add(Util.null2String(rs.getString("belongdate")));
                //关联调休转换--1关联调休，0不关联调休
                data.add(Util.null2String(rs.getString("paidleaveenable")));

                //加班规则--1-需审批，以审批单为准，2-需审批，以打卡为准，但是不能超过审批时长
                data.add(Util.null2String(rs.getString("computingmode")));
                //加班类型-- 1-节假日、2-工作日、3-休息日
                data.add(Util.null2String(rs.getString("changetype")));
                //加班单位
                data.add(Util.null2String(rs.getString("durationrule")));
                dataList.add(data);
            }
            new BaseBean().writeLog("==zj==(dataList--加班明细表)"+JSON.toJSONString(dataList));
            sheet.put("dataList",dataList);

            //添加列
            List<String> showCol = new ArrayList<String>();
            showCol = showOverTimeCol();
            for (String colName : showCol){
                title = new HashMap<String,Object>();
                title.put("rowSpan",4);
                title.put("width",10240);
                title.put("title",colName);
                titleList.add(title);
            }
            new BaseBean().writeLog("==zj==(titleList--加班明细表)" + JSON.toJSONString(titleList));
            sheet.put("titleList",titleList);
            sheet.put("createFile","1");

            lsSheet.add(sheet);

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(加班明细表--报错)" + e);
        }
        return lsSheet;
    }

    /**
     * 换算加班时长--暂时不用
     * @param duration_min
     * @return
     */
    public String durationCul(String duration_min){
        String durationrule = Util.null2String(KQOvertimeRulesBiz.getMinimumUnit());
        new BaseBean().writeLog("==zj==(加班计算单位)" + durationrule);
        Double  duration_minD = 0.00;
        String duration = "";
        try{
             duration_minD = Double.parseDouble(duration_min);
            if (!"".equals(duration_min)){
                //如果加班分钟数不为空

                if("3".equalsIgnoreCase(durationrule) || "5".equalsIgnoreCase(durationrule) || "6".equalsIgnoreCase(durationrule)){
                     duration =KQDurationCalculatorUtil.getDurationRound(""+duration_minD/60.0) ;
                }else {
                    double oneDayHour = KQFlowUtil.getOneDayHour(DurationTypeEnum.OVERTIME,"");
                    double d_day = duration_minD/(oneDayHour * 60);
                    duration = KQDurationCalculatorUtil.getDurationRound(""+d_day);
                }

                new BaseBean().writeLog("==zj==(加班时长转换)" + duration);
                return duration;
            }

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(加班时长转换错误)" + e);
        }

        return duration;
    }

    /**
     * 导出加班明细表显示列
     * @return
     */
    public List<String> showOverTimeCol(){
        ArrayList<String> showCol = new ArrayList<String>();
        showCol.add("姓名");
        showCol.add("手机号码");
        showCol.add("开始日期");
        showCol.add("开始时间");
        showCol.add("结束日期");
        showCol.add("结束时间");
        showCol.add("加班时长");
        showCol.add("归属日期");
        showCol.add("关联调休");
        showCol.add("加班规则");
        showCol.add("加班类型");
        showCol.add("加班单位");

        return showCol;
    }



    /**
     * 导出销假明细表显示列
     * @return
     */
    public List<String> showLeaveBackCol(){
        ArrayList<String> showCol = new ArrayList<String>();
        showCol.add("流程id");
        showCol.add("姓名");
        showCol.add("手机号");
        showCol.add("拆分开始日期");
        showCol.add("拆分开始时间");
        showCol.add("拆分结束日期");
        showCol.add("拆分结束时间");
        showCol.add("拆分时长");
        showCol.add("原始表单开始日期");
        showCol.add("原始表单开始时间");
        showCol.add("原始表单结束日期");
        showCol.add("原始表单结束时间");
        showCol.add("原始表单时长");
        showCol.add("请假类型");
        showCol.add("表单名称");
        showCol.add("归属日期");
        showCol.add("单位规则");
        showCol.add("销假流程id");

        return showCol;
    }

}
