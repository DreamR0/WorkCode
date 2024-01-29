package com.customization.qc2443677;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cloudstore.dev.api.util.EMManager;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQWorkTime;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.entity.WorkTimeEntity;
import com.google.common.collect.Maps;
import weaver.conn.RecordSet;
import weaver.dateformat.DateTransformer;
import weaver.dateformat.TimeZoneVar;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class signDataUtil {
    private Map<String,Object> workTimeEntityLogMap = Maps.newHashMap();
    private Map<String,Object> resultMap = new HashMap<>();
    private String isInCom;

    public Map<String,Object> getsignDate(User user, HttpServletRequest request, Map<String, Object> params) {

//    signSection: 2019-03-20 08:30#2019-03-20 18:30

        RecordSet rs = new RecordSet();
        RecordSet rs1 = new RecordSet();
        String serialid = Util.null2String(params.get("serialid"));
        //应上班 工作时间点
        String time = Util.null2String(params.get("time"));
        //应上班 工作时间 带日期
        String datetime = Util.null2String(params.get("datetime"));
        //允许打卡时段 带日期
        String signSectionTime = Util.null2String(params.get("signSectionTime"));
        //打卡所属worksection的对应的点
        String type = Util.null2String(params.get("type"));
        //所属打卡日期
        String belongdate = Util.null2String(params.get("belongdate"));
        boolean belongdateIsNull = belongdate.length()==0;
        String islastsign = Util.null2String(params.get("islastsign"));

        String isPunchOpen = Util.null2String(params.get("isPunchOpen"));

        String workmins = Util.null2String(params.get("workmins"));
        //针对非工作时段  签退的时候记录的签到数据 用于计算加班
        String signInTime4Out = Util.null2String(params.get("signInTime4Out"));
        //允许打卡的范围
        String signsection = Util.null2String(params.get("signSection"));

        //手机打卡部分
        String longitude = Util.null2String(params.get("longitude"));
        String latitude = Util.null2String(params.get("latitude"));
        double d_longitude = Util.getDoubleValue(longitude);
        double d_latitude = Util.getDoubleValue(latitude);
        if(d_latitude <= 0){
            latitude = "";
        }
        if(d_longitude <= 0){
            longitude = "";
        }
        String address = Util.null2String(params.get("address"));
        String ismobile = Util.null2String(params.get("ismobile"));
        String remark = Util.null2String(params.get("remark"));
        String attachment = Util.null2String(params.get("fileids"));
        //区分是来自于钉钉还是EM7
        String browser = Util.null2String(params.get("browser"));
        //客户
        String crm = Util.null2String(params.get("crm"));
        //是否开启外勤签到转考勤
        String outsidesign = "";
        KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
        KQGroupEntity kqGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(user.getUID()+"");
        String kqGroupEntityInfo = kqGroupEntity != null ? JSON.toJSONString(kqGroupEntity): "";
        if (kqGroupEntity != null) {
            outsidesign = kqGroupEntity.getOutsidesign();
        }

        int userId = user.getUID();
        String signfrom = "e9_mobile_out";
        DateTimeFormatter allFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime localTime = LocalDateTime.now();
        String signTime =localTime.format(dateTimeFormatter);
        String signDate = localTime.format(dateFormatter);

        KQWorkTime kqWorkTime = new KQWorkTime();
        WorkTimeEntity workTimeEntity = kqWorkTime.getWorkTime(user.getUID()+"", signDate);
        String userinfo = "#userid#"+user.getUID()+"#getUserSubCompany1#"+user.getUserSubCompany1()+"#getUserSubCompany1#"+user.getUserDepartment()
                +"#getJobtitle#"+user.getJobtitle();
        workTimeEntityLogMap.put("resourceid", userinfo);
        workTimeEntityLogMap.put("splitDate", signDate);
        workTimeEntityLogMap.put("workTimeEntity", workTimeEntity);

        //处理多时区
        String timeZoneConversion = Util.null2String(new weaver.general.BaseBean().getPropValue("weaver_timezone_conversion","timeZoneConversion")).trim();
        if("1".equals(timeZoneConversion)) {
            DateTransformer dateTransformer=new DateTransformer();
            String[] zone_localTime = dateTransformer.getLocaleDateAndTime(signDate,signTime);
            if(zone_localTime != null && zone_localTime.length == 2){
                signDate = zone_localTime[0];
                signTime = zone_localTime[1];
            }
        }
        String timeZone = Util.null2String(TimeZoneVar.getTimeZone(),"");

        if("1".equalsIgnoreCase(outsidesign)){

            JSONObject jsonObject = null;
            String deviceInfo = Util.null2String(params.get("deviceInfo"));
            if(deviceInfo.length() > 0){
                jsonObject = JSON.parseObject(deviceInfo);
                JSONObject jsonObject1 = new JSONObject();
                Set<Map.Entry<String, Object>> jsonSet =  jsonObject.entrySet();
                for(Map.Entry<String, Object> js : jsonSet){
                    String key = js.getKey();
                    String value = Util.null2String(js.getValue());
                    jsonObject1.put(key, value);
                }
                if(!jsonObject1.isEmpty()){
                    deviceInfo = jsonObject1.toJSONString();
                }
            }

            if("DingTalk".equalsIgnoreCase(browser)){
                signfrom = "DingTalk_out";
            }else if("Wechat".equalsIgnoreCase(browser)){
                signfrom = "Wechat_out";
                String weChat_deviceid = Util.null2String(request.getSession().getAttribute(EMManager.DeviceId));


                if(weChat_deviceid.length() > 0){
                    //微信打卡的设备号需要单独处理
                    if(jsonObject != null){
                        jsonObject.put("deviceId", weChat_deviceid);
                    }else{
                        jsonObject = new JSONObject();
                        jsonObject.put("deviceId", weChat_deviceid);
                    }
                    if(!jsonObject.isEmpty()){
                        deviceInfo = jsonObject.toJSONString();
                    }
                }
            }
            //自由班制处理
            String isfree = Util.null2String(params.get("isfree"));

            String userType = user.getLogintype();
            String signType = "on".equalsIgnoreCase(type) ? "1" : "2";
            String clientAddress = Util.getIpAddr(request);
            boolean isInIp = true;

            String isInCom = isInIp ? "1" : "0";

            String datetime_timezone = signDate+" "+signTime;
            LocalDateTime nowDateTime = LocalDateTime.parse(datetime_timezone,allFormatter);

            boolean isInScope = true;
            if(signsection != null && signsection.length() > 0){
                List<String> signsectionList = Util.TokenizerString(signsection, ",");
                for(int i = 0 ; i < signsectionList.size() ; i++){
                    String signsections = Util.null2String(signsectionList.get(i));
                    String[] signsection_arr = signsections.split("#");
                    if(signsection_arr != null && signsection_arr.length == 2){
                        String canStart = signsection_arr[0];
                        String canEnd = signsection_arr[1];
                        LocalDateTime startSignDateTime = LocalDateTime.parse(canStart,allFormatter);
                        LocalDateTime endSignDateTime = LocalDateTime.parse(canEnd,allFormatter);
                        if(nowDateTime.isBefore(startSignDateTime) || nowDateTime.isAfter(endSignDateTime)){
                            isInScope = false;
                        }else{
                            isInScope = true;
                            break;
                        }
                    }
                }
            }
            if(!isInScope){
                //外勤的不在范围内也不管，全部计入考勤表
//        retmap.put("status", "1");
//        retmap.put("message", SystemEnv.getHtmlLabelName(503597 , user.getLanguage()));
//        return ;
            }
            if(belongdate.length() == 0){
                belongdate = signDate;
            }
            resultMap.put("userId",userId);
            resultMap.put("userType", userType);
            resultMap.put("signType", signType);
            resultMap.put("signDate", signDate);
            resultMap.put("signTime", signTime);
            resultMap.put("clientAddress", clientAddress);
            resultMap.put("isInCom", isInCom);
            resultMap.put("timeZone", timeZone);
            resultMap.put("belongdate", belongdate);
            resultMap.put("signfrom", signfrom);
            resultMap.put("longitude", longitude);
            resultMap.put("latitude", latitude);
            resultMap.put("address", address);
            resultMap.put("deviceInfo", deviceInfo);
            resultMap.put("remark", remark);
            resultMap.put("attachment", attachment);
            resultMap.put("browser",browser);
        }

        new BaseBean().writeLog("==zj==(resultMap)" +JSON.toJSONString(resultMap));
        return resultMap;
    }
}
