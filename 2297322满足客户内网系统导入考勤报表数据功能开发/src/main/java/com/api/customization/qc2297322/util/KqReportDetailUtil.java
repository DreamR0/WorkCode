package com.api.customization.qc2297322.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQFormatBiz;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import tebie.applib.api.X;
import weaver.conn.RecordSet;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class KqReportDetailUtil {


    /**
     * 请假加班明细表导入
     * @param params
     */
    public Map<String,Object> KqReportDetailIn(Map<String, Object> params){
        new BaseBean().writeLog("==zj==(params)" + JSON.toJSONString(params));
        RecordSet rs= new RecordSet();
        String fileName = Util.null2String(params.get("excelfile"));
        new BaseBean().writeLog("==zj==(fileName)" + fileName);
        Map<String,Object> retMap = new HashMap<>();
        List<Object> param = null;
        List<Object> paramNon = null;
        List<List> lsLeaveParams = new ArrayList<>();   //请假数据
        List<List> lsLeaveNonParams = new ArrayList<>();    //请假重复数据
        List<List> lsOverTimeParams = new ArrayList<>();//加班数据
        List<List> lsOverTimeNonParams = new ArrayList<>();//加班重复数据
        List<Object> errorLeave = new ArrayList<>();//请假错误数据
        List<Object> errorOverTime = new ArrayList<>();//加班错误数据
        List<Map> leaveInits = new ArrayList<>();//请假刷新报表
        List<Map> overTimeInits = new ArrayList<>();//加班刷新报表



        try{
            new BaseBean().writeLog("==zj==(标识)");
            int count = 2;      //两张明细表的标识   0:请假表   1：加班表
            ImageFileManager manager = new ImageFileManager();
            manager.getImageFileInfoById(Util.getIntValue(fileName));
            //
            Workbook workbook = WorkbookFactory.create(manager.getInputStream());

//            new POIFSFileSystem(manager.getInputStream()

            for (int i = 0; i < count; i++) {
                //循环插入两张表
                if (i == 0){
                    //当前为请假明细表
                    new BaseBean().writeLog("==zj==(i)" + i);
                    Sheet sheet = workbook.getSheetAt(i);
                    new BaseBean().writeLog("==zj==(请假明细表-sheetName)"+ sheet.getSheetName());
                    Row row = null;

                    String name = "";
                    String mobile = "";
                    String resourceId = "";
                    String fromDate = "";
                    String fromTime = "";
                    String toDate = "";
                    String toTime = "";
                    String leaveType = "";
                    String tableName = "";
                    String duration = "";
                    String belongto = "";
                    int rowsNum = sheet.getPhysicalNumberOfRows();      //获取该工作表的行数

                    new BaseBean().writeLog("==zj==(请假明细表--rowsNum)" + rowsNum);
                    for (int j = 1; j < rowsNum + 1; j++) {
                        //因为前三行为标题和列名，先写死跳过
                        if (j < 3)continue;
                        row = sheet.getRow(j);
                        if (row == null) continue;
                        for (int k = 0; k < row.getLastCellNum(); k++) {
                            if (k == 0){
                                 name = Util.null2String(getCellValue(row.getCell((short) k), row)).trim();     //获取名字
                            }else if (k == 1){
                                 mobile = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取手机号
                            }else if (k == 2){
                                fromDate = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取开始日期
                            }else if (k == 3){
                                fromTime = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取开始时间
                            }else if (k == 4){
                                toDate = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取结束日期
                            }else if (k == 5){
                                toTime = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取结束时间
                            }else if (k == 6){
                               String leaveName = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取请假类型
                               leaveType = getleaveType(leaveName); //这里要和内网的请假类型id做匹配
                            }else if (k == 7){
                                tableName = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取表单名称
                            }else if (k == 8){
                                duration = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取时长
                            }else if (k == 9){
                                belongto = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取归属日期
                            }
                        }
                        //如果获取到姓名和手机号之后，匹配内网人员id
                        new BaseBean().writeLog("==zj==(请假明细表)" + mobile);
                        if (!"".equals(mobile)){
                            resourceId = getResourceId(mobile);
                        }else {
                            //手机号无法匹配内网人员
                            resourceId = "";
                        }

                        if (resourceId.length() <= 0){
                            errorLeave.add(j + 1);
                            continue;
                        }
                        paramNon = new ArrayList<>();
                        paramNon.add(resourceId);
                        paramNon.add(fromDate);
                        paramNon.add(toDate);
                        paramNon.add(belongto);
                        lsLeaveNonParams.add(paramNon);

                        param = new ArrayList<Object>();
                        param.add(resourceId);      //sql导入--人员id
                        param.add(fromDate);        //sql导入--开始日期
                        param.add(fromTime);        //sql导入--开始时间
                        param.add(toDate);      //sql导入--结束日期
                        param.add(toTime);      //sql导入--结束时间
                        param.add(fromDate);      //sql导入--开始日期（流程）
                        param.add(fromTime);      //sql导入--开始时间（流程）
                        param.add(toDate);      //sql导入--结束日期（流程）
                        param.add(toTime);      //sql导入--结束时间（流程）
                        param.add(leaveType);       //sql导入--请假类型
                        param.add(duration);        //sql导入--时长
                        param.add(tableName);       //sql导入--表单名称
                        param.add(belongto);        //sql导入--归属日期
                        lsLeaveParams.add(param);

                        //这里添加下要刷新的报表日期
                        HashMap<String,String> map = new HashMap();
                        map.put("resourceId",resourceId);
                        map.put("fromDate",fromDate);
                        leaveInits.add(map);

                    }
                    //先删除数据
                    String leaveDelSql = "delete from kq_flow_split_leave where resourceid=? and fromdate=?  and todate=?  and belongdate=?";
                    new BaseBean().writeLog("==zj==(导入--请假删除数据)" + JSON.toJSONString(lsLeaveNonParams));
                    new BaseBean().writeLog("==zj==(导入--lsLeaveNonParams)" + JSON.toJSONString(lsLeaveNonParams));
                    boolean deleteLeave = rs.executeBatchSql(leaveDelSql, lsLeaveNonParams);
                    new BaseBean().writeLog("==zj==(导入--请假删除数据)" + deleteLeave);
                    //再插入数据
                    String leaveSql = "insert into kq_flow_split_leave(resourceid,fromdate,fromtime,todate,totime,fromdatedb,fromtimedb,todatedb,totimedb,newleavetype,duration,tablenamedb,belongdate)"+
                            " values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    new BaseBean().writeLog("==zj==(请假明细表插入sql)" + leaveSql);
                    new BaseBean().writeLog("==zj==(导入--请假插入数据)" + JSON.toJSONString(lsLeaveParams));
                    rs.executeBatchSql(leaveSql,lsLeaveParams);

                    //刷新报表
                    for (int j = 0; j < leaveInits.size(); j++) {
                        Map<String,String> map = leaveInits.get(i);
                        String userId = map.get("resourceId");
                        String fromDates = map.get("fromDate");
                        new KQFormatBiz().formatDate(userId,fromDates);
                    }
                }

                if (i == 1){
                    //当前为加班明细表
                    Sheet sheet = workbook.getSheetAt(i);
                    new BaseBean().writeLog("==zj==(加班明细表-sheetName)"+ sheet.getSheetName());
                    Row row = null;

                    String name = "";
                    String mobile = "";
                    String resourceId = "";
                    String fromDate = "";
                    String fromTime = "";
                    String toDate = "";
                    String toTime = "";
                    String duration_min = "";
                    String belongdate = "";
                    String paidleaveenable = "";
                    String computingmode = "";
                    String changetype = "";
                    String durationrule = "";

                    String fromDateDB = "";
                    String fromTimeDB = "";
                    String toDateDB = "";
                    String toTimeDB = "";

                    int rowsNum = sheet.getPhysicalNumberOfRows();

                    new BaseBean().writeLog("==zj==(加班明细表--rowsNum)" + rowsNum);
                    for (int j = 1; j < rowsNum + 1; j++) {
                        //因为前三行为标题和列名，先写死跳过
                        if (j < 3)continue;
                        row = sheet.getRow(j);
                        if (row == null) continue;
                        for (int k = 0; k < row.getLastCellNum(); k++) {
                            if (k == 0){
                                name = Util.null2String(getCellValue(row.getCell((short) k), row)).trim();     //获取名字
                            }else if (k == 1){
                                mobile = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取手机号
                            }else if (k == 2){
                                fromDate = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取开始日期
                            }else if (k == 3){
                                fromTime = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取开始时间
                            }else if (k == 4){
                                toDate = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取结束日期
                            }else if (k == 5){
                                toTime = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取结束时间
                            }else if (k == 6){
                                duration_min = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取加班时长
                            }else if (k == 7){
                                belongdate = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取归属日期
                            }else if (k == 8){
                                paidleaveenable = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取关联调休
                            }else if (k == 9){
                                computingmode = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取加班规则
                            }else if (k == 10){
                                changetype = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取加班类型
                            }else if (k == 11){
                                durationrule = Util.null2String(getCellValue(row.getCell((short) k), row)).trim(); //获取加班单位
                            }
                        }

                        //如果获取到姓名和手机号之后，匹配内网人员id
                        if (!"".equals(mobile)){
                            resourceId = getResourceId(mobile);
                        }else {
                            //手机号无法匹配内网人员
                            resourceId = "";
                        }

                        if (resourceId.length() <= 0){
                            errorOverTime.add(j + 1);
                            continue;
                        }
                        paramNon = new ArrayList<>();
                        paramNon.add(resourceId);
                        paramNon.add(fromDate);
                        paramNon.add(toDate);
                        paramNon.add(belongdate);
                        lsOverTimeNonParams.add(paramNon);




                        param = new ArrayList<Object>();
                        param.add(resourceId);          //sql导入--人员id
                        param.add(fromDate);             //sql导入--开始日期
                        param.add(fromTime); //sql导入--开始时间
                        param.add(toDate); //sql导入--结束日期
                        param.add(toTime); //sql导入--结束时间

                        if (fromTime.length()>6){
                             fromTimeDB =  fromTime.substring(0,5);
                        }else {
                            fromTimeDB = fromTime;
                        }
                        if (toTime.length()>6){
                            toTimeDB =  toTime.substring(0,5);
                        }else {
                            toTimeDB = toTime;
                        }
                        //流程日期就是实际日期
                        fromDateDB = fromDate;
                        toDateDB = toDate;
                        param.add(fromDateDB);
                        param.add(fromTimeDB);
                        param.add(toDateDB);
                        param.add(toTimeDB);
                        param.add(duration_min); //sql导入--加班分钟数
                        param.add(belongdate); //sql导入--归属日期
                        param.add(durationrule); //sql导入--加班单位
                        param.add(changetype); //sql导入--加班类型
                        param.add(paidleaveenable); //sql导入--关联调休
                        param.add(computingmode); //sql导入--加班规则

                        lsOverTimeParams.add(param);

                        //这里添加下要刷新的报表日期
                        HashMap<String,String> map = new HashMap();
                        map.put("resourceId",resourceId);
                        map.put("fromDate",fromDate);
                        overTimeInits.add(map);
                    }
                    //先删除
                    String overTimeDelSql = "delete from kq_flow_overtime where resourceid=? and fromdate=?  and todate=?  and belongdate=?";
                    new BaseBean().writeLog("==zj==(删除加班数据)" + overTimeDelSql);
                    new BaseBean().writeLog("==zj==(导入--加班删除数据)" + JSON.toJSONString(lsOverTimeNonParams));
                    boolean deleteOverTime = rs.executeBatchSql(overTimeDelSql, lsOverTimeNonParams);
                    new BaseBean().writeLog("==zj==(删除加班数据结果)" + deleteOverTime);
                    //再将获得到的加班数据插入
                    String overTimeSql = "insert into kq_flow_overtime(resourceid,fromdate,fromtime,todate,totime,fromdatedb,fromtimedb,todatedb,totimedb,duration_min,belongdate,durationrule,changetype,paidleaveenable,computingMode)"+
                     " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    new BaseBean().writeLog("==zj==(插入加班数据)" + overTimeSql);
                    new BaseBean().writeLog("==zj==(导入--加班插入数据)" + JSON.toJSONString(lsOverTimeParams));
                    rs.executeBatchSql(overTimeSql,lsOverTimeParams);

                    //刷新报表
                    for (int j = 0; j < overTimeInits.size(); j++) {
                        Map<String,String> map = overTimeInits.get(i);
                        String userId = map.get("resourceId");
                        String fromDates = map.get("fromDate");
                        new KQFormatBiz().formatDate(userId,fromDates);
                    }
                }
            }

            retMap.put("code","1");
            if (errorLeave.size() > 0){
                retMap.put("errorLeave",errorLeave);
            }
            if (errorOverTime.size() > 0){
                retMap.put("errorOverTime",errorOverTime);
            }
            retMap.put("msg","导入成功");

        }catch (Exception e){
            new BaseBean().writeLog("==zj==(导入报错信息)" + e);
            retMap.put("code","-1");
            retMap.put("msg",e);
        }
        return retMap;
}


    /**
     * 获取Excel表格对应表格中的数据
     * @param cell
     * @param row
     * @return
     */
    private String getCellValue(Cell cell, Row row) {
        if (cell == null) return "";
        String cellValue = "";
        switch (cell.getCellType()) {

            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = null;
                    if (cell.getCellStyle().getDataFormat() == HSSFDataFormat.getBuiltinFormat("h:mm:ss")) {
                        sdf = new SimpleDateFormat("HH:mm:ss");
                    } else {// 日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                    }
                    Date date = cell.getDateCellValue();
                    cellValue = sdf.format(date);
                } else {
                    cellValue = new java.text.DecimalFormat("0").format(cell.getNumericCellValue());
                }
                break;
            case STRING:
                cellValue = cell.getStringCellValue();
                break;
            case FORMULA:
                cellValue = (DateFormat.getDateInstance().format((cell.getDateCellValue()))).toString();

                break;
            default:

                break;
        }
        return cellValue;
    }

    /**
     * 导入人员两个系统可能不一致（唯一标识：手机号）
     * 在这里获取外网导入员的id
     * @return
     */
    public String getResourceId(String mobile){
        RecordSet rs = new RecordSet();
        String resourceId ="";

        //这里先判断如果大于1说明该系统存在不同人员有相同手机号
        String sqlUni = "select count(id)  from hrmresource where mobile = '"+mobile+"'";
        rs.executeQuery(sqlUni);
        if (rs.next()){
            int count = Util.getIntValue(rs.getString("count"));
            //如果大于1直接反空
            if (count > 1){
                return resourceId ;
            }
        }
        String sql = "select id from hrmresource where mobile = '"+mobile+"'";
        new BaseBean().writeLog("==zj==(获取内网人员id)" + sql);
        rs.executeQuery(sql);
        if (rs.next()){
            resourceId = Util.null2String(rs.getString("id"));
        }else {
            resourceId = "";
        }

        return resourceId;
    }

    /**
     *
     * @param leaveName
     * @return
     */
    public String getleaveType(String leaveName){
        String leaveType = "";
        RecordSet rs = new RecordSet();
        String sql = "select id from KQ_LeaveRules where leaveName = '"+leaveName+"'";
        rs.executeQuery(sql);
        if (rs.next()){
            leaveType = Util.null2String(rs.getString("id"));
        }

        return leaveType;
    }
}
