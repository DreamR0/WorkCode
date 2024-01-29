package com.api.customization.kq.qc2241191.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.customization.kq.qc2241191.ReportUtil;
import com.engine.common.util.ParamUtil;
import com.engine.common.util.ServiceUtil;
import com.engine.kq.service.KQReportService;
import com.engine.kq.service.impl.KQReportServiceImpl;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/kq/reportsummary")
public class Report {

    private KQReportService getService(User user) {
        return (KQReportService) ServiceUtil.getService(KQReportServiceImpl.class, user);
    }

    @POST
    @Path("/saveReport")
    @Produces(MediaType.APPLICATION_JSON)
    public String saveReport(@Context HttpServletRequest request, @Context HttpServletResponse response){
       List apidatas = new ArrayList();
       //初始化数据表插入值
        int workdays = 0;
        String lastname = "";
        String subcompany = "";
        String department = "";
        String attendancedate = "";
        double attendancedays= 0;
        double leaveType_2 = 0;
        double leaveType_4 = 0;
        double leaveType_5 = 0;
        double leaveType_6 = 0;
        double leaveType_7 = 0;
        double leaveType_10 = 0;
        double leaveType_11 = 0;
        double leaveType_12 = 0;
        double leaveType_13 = 0;
        double leaveType_14 = 0;
        double leaveType_15 = 0;
        double leaveType_16 = 0;
        double leaveType_17 = 0;
        double otherdays = 0;
        double businessLeave = 0;
        double officialBusiness = 0;

        Map<String,String> message = new HashMap<String,String>();
        try{
            User user = HrmUserVarify.getUser(request,response);
            Map<String, Object> params = ParamUtil.request2Map(request);


            JSONObject jsonObj = JSON.parseObject(Util.null2String(params.get("data")));
            String fromDate = Util.null2String(jsonObj.get("fromDate"));
            String toDate = Util.null2String(jsonObj.get("toDate"));
            String typeselect =Util.null2String(jsonObj.get("typeselect"));
            if(typeselect.length()==0)typeselect = "3";
            if(!typeselect.equals("") && !typeselect.equals("0")&& !typeselect.equals("6")){
                if(typeselect.equals("1")){
                    fromDate = TimeUtil.getCurrentDateString();
                    toDate = TimeUtil.getCurrentDateString();
                }else{
                    fromDate = TimeUtil.getDateByOption(typeselect,"0");
                    toDate = TimeUtil.getDateByOption(typeselect,"1");
                }
            }

            //分割日期范围，按月划分
            List<String> listWeekOrMonth = new ArrayList<String>();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); //设置转换日期格式
            String startDate = fromDate;    //设置开始日期
            String endDate = toDate;        //设置结束日期

            Date sDate = dateFormat.parse(startDate);       //将“开始日期”转换为日期格式
            Calendar sCalendar = Calendar.getInstance();    //创建Calendar子类实例
            sCalendar.setFirstDayOfWeek(Calendar.MONDAY);   //将第一个星期的第一天设置为周一
            sCalendar.setTime(sDate);                       //设置开始时间

            Date eDate = dateFormat.parse(endDate);
            Calendar eCalendar = Calendar.getInstance();
            eCalendar.setFirstDayOfWeek(Calendar.MONDAY);
            eCalendar.setTime(eDate);
            boolean bool = true;

            //==zj==获取每个月开始时间，结束时间
                while(sCalendar.getTime().getTime()<eCalendar.getTime().getTime()){
                    if(bool||sCalendar.get(Calendar.DAY_OF_MONTH)==1||sCalendar.get(Calendar.DAY_OF_MONTH)==sCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)){
                        listWeekOrMonth.add(dateFormat.format(sCalendar.getTime()));
                        bool = false;
                    }
                    sCalendar.add(Calendar.DAY_OF_MONTH, 1);
                }
                listWeekOrMonth.add(dateFormat.format(eCalendar.getTime()));
                if(listWeekOrMonth.size()%2!=0){
                    listWeekOrMonth.add(dateFormat.format(eCalendar.getTime()));
                }
                new BaseBean().writeLog("==zj==(前端获取时间范围)" + listWeekOrMonth);

                //根据月份开始循环
            for (int i = 0; i < listWeekOrMonth.size(); i+=2) {
                startDate = listWeekOrMonth.get(i);
                endDate = listWeekOrMonth.get(i+1);

                //获取报表数据
                ReportUtil reportUtil = new ReportUtil();
                apidatas = reportUtil.getReport(params,user,startDate,endDate);       //报表数据来源
                new BaseBean().writeLog("==zj==（每月报表数据）"+JSONObject.toJSONString(apidatas));

                //获取模块id信息
                String formmodeid = Util.null2String(new BaseBean().getPropValue("qc2118737","formmodeid"));    //获取formmodeid
                new BaseBean().writeLog("==zj==（模块id）" + formmodeid);

                //将报表数据循环插入到uf_ReportSummary（建模表）
                RecordSet rs = new RecordSet();
                //如果apidatas不为空遍历list
                if (apidatas != null){
                    for (int j = 0; j < apidatas.size(); j++) {
                        Map<String,Object> data = new HashMap<String,Object>();
                        data = (Map) apidatas.get(j);
                        new BaseBean().writeLog("==zj==(遍历list集合，取每个员工（Map）数据)" + data);
                        //如果data不为空遍历Map
                        if (data != null){
                            lastname=String.valueOf(data.get("lastname"));
                            subcompany=String.valueOf(data.get("subcompany"));
                            department=String.valueOf(data.get("department"));

                            workdays = Integer.parseInt(String.valueOf(data.get("workdays"))) ;
                            attendancedays = Double.parseDouble(String.valueOf(data.get("attendancedays")));
                            attendancedate=String.valueOf(data.get("attendancedate"));

                            //==zj==请假配置字段
                            String name2 = new BaseBean().getPropValue("qc2118737","leaveType_2");  //年休假
                            String name4 = new BaseBean().getPropValue("qc2118737","leaveType_4");  //带薪病假
                            String name5 = new BaseBean().getPropValue("qc2118737","leaveType_5");  //调休
                            String name6 = new BaseBean().getPropValue("qc2118737","leaveType_6");  //事假
                            String name7 = new BaseBean().getPropValue("qc2118737","leaveType_7");  //病假
                            String name10 = new BaseBean().getPropValue("qc2118737","leaveType_10");  //婚假
                            String name11 = new BaseBean().getPropValue("qc2118737","leaveType_11");  //丧假
                            String name12 = new BaseBean().getPropValue("qc2118737","leaveType_12");  //哺乳假
                            String name13 = new BaseBean().getPropValue("qc2118737","leaveType_13");  //探亲假
                            String name14 = new BaseBean().getPropValue("qc2118737","leaveType_14");  //产假及看护假
                            String name15 = new BaseBean().getPropValue("qc2118737","leaveType_15");  //育儿假
                            String name16 = new BaseBean().getPropValue("qc2118737","leaveType_16");  //产检假
                            String name17 = new BaseBean().getPropValue("qc2118737","leaveType_17");  //年假(陇原)
                            String name18 = new BaseBean().getPropValue("qc2118737","otherdays");     //其它
                            new BaseBean().writeLog("==zj==（字段测试）" + name2 + " | "+ name4 + " | "+ name5 + " | "+ name6 + " | "+ name7 + " | "+ name10 + " | "+ name11 + " | "+ name12+ " | "+ name13 + " | "+ name14 + " | "+ name15 + " | "+ name16 + " | "+ name17 + " | "+ name18);




                            leaveType_2=Double.parseDouble(Util.null2String(data.get(name2)));  //年休假
                            leaveType_4=Double.parseDouble(Util.null2String(data.get(name4)));  //带薪病假
                            leaveType_5=Double.parseDouble(Util.null2String(data.get(name5)));  //调休
                            leaveType_6=Double.parseDouble(Util.null2String(data.get(name6)));  //事假
                            leaveType_7=Double.parseDouble(Util.null2String(data.get(name7)));  //病假
                            leaveType_10=Double.parseDouble(Util.null2String(data.get(name10))); //婚假
                            leaveType_11=Double.parseDouble(Util.null2String(data.get(name11))); //丧假
                            leaveType_12=Double.parseDouble(Util.null2String(data.get(name12))); //哺乳假
                            leaveType_13=Double.parseDouble(Util.null2String(data.get(name13))); //探亲假
                            leaveType_14=Double.parseDouble(Util.null2String(data.get(name14))); //产假及看护假
                            leaveType_15=Double.parseDouble(Util.null2String(data.get(name15))); //育儿假
                            leaveType_16=Double.parseDouble(Util.null2String(data.get(name16))); //产检假
                            leaveType_17=Double.parseDouble(Util.null2String(data.get(name17))); //年假(陇原)
                            otherdays=Double.parseDouble(Util.null2String(data.get(name18)));    //其它
                            businessLeave=Double.parseDouble(Util.null2String(data.get("businessLeave")));
                            officialBusiness=Double.parseDouble(Util.null2String(data.get("officialBusiness")));

                            //先判断数据库是否已有该员工数据
                            String selectsql = "select lastname from uf_ReportSummary where lastname='"+lastname + "' and attendancedate='" + attendancedate + "'";
                            new BaseBean().writeLog("==zj==（查找该员工在数据库中是否已有本月数据）" + selectsql);
                            rs.executeQuery(selectsql);

                            if (rs.next()){
                                //如果有，创建修改语句
                                String setsql = "update uf_ReportSummary set lastname='" + lastname + "',subcompany='" + subcompany + "',department='"+department+
                                        "',attendancedate='"+attendancedate+"',attendancedays="+attendancedays+",leaveType_2="+leaveType_2+",leaveType_4="+leaveType_4+
                                        ",leaveType_5="+leaveType_5+",leaveType_6="+leaveType_6+",leaveType_7="+leaveType_7+",leaveType_10="+leaveType_10+
                                        ",leaveType_11="+leaveType_11+",leaveType_12="+leaveType_12+",leaveType_13="+leaveType_13+",leaveType_14="+leaveType_14+
                                        ",leaveType_15="+leaveType_15+",leaveType_16="+leaveType_16+",leaveType_17="+leaveType_17+",otherdays="+otherdays+
                                        ",businessLeave="+businessLeave+",officialBusiness="+officialBusiness+",workdays="+workdays+ ",formmodeid="+formmodeid + " where lastname='"+lastname+
                                        "' and attendancedate='"+attendancedate+"'";
                                new BaseBean().writeLog("==zj==(报表数据修改)" + setsql);
                                rs.executeUpdate(setsql);
                            }else {
                                //如果没有，创建插入语句
                                String insertsql="insert into uf_ReportSummary(lastname,subcompany,department,attendancedate,attendancedays,"+
                                        "leaveType_2,leaveType_4,leaveType_5,leaveType_6,leaveType_7,leaveType_10,leaveType_11,leaveType_12,"+
                                        "leaveType_13,leaveType_14,leaveType_15,leaveType_16,leaveType_17,otherdays,businessLeave,officialBusiness,workdays,formmodeid) values"+
                                        "('" +lastname + "'" +",'"+subcompany+"','"+department+"','"+attendancedate+"',"+attendancedays+","+
                                        leaveType_2+","+leaveType_4+","+leaveType_5+","+leaveType_6+","+leaveType_7+","+leaveType_10+","+
                                        leaveType_11+","+leaveType_12+","+leaveType_13+","+leaveType_14+","+leaveType_15+","+leaveType_16+
                                        ","+leaveType_17 +","+otherdays+","+businessLeave+","+officialBusiness+","+workdays+ "," + formmodeid +")";
                                new BaseBean().writeLog("==zj==(报表数据新增)" + insertsql);
                                rs.executeUpdate(insertsql);
                            }
                        }
                    }
                }
            }

            message.put("code","01");
            message.put("message","保存成功");
        }catch (Exception e){
            new BaseBean().writeLog(e);
            message.put("code","00");
            message.put("message",Util.null2String(e));
        }
        return JSONObject.toJSONString(message);
    }
}
