package com.api.customization.qc2758032.util;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQLeaveRulesComInfo;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import java.util.HashMap;
import java.util.Map;

public class KqReportBizCustomUtil {

    /**
     *日报表数据
     * @param resourceId
     * @param fromDate
     * @param toDate
     * @return
     */
    public Map<String,Object> getDetialDatas(String resourceId, String fromDate, String toDate, User user){
        return getDetialDatas(resourceId,fromDate,toDate,user,new HashMap<String,Object>(),false,0,"0");
    }
    public Map<String,Object> getDetialDatas(String resourceId,String fromDate, String toDate, User user,
                                             Map<String,Object> flowData,boolean isWrap,int uintType,String show_card_source){
        KQLeaveRulesComInfo kqLeaveRulesComInfo = new KQLeaveRulesComInfo();
        Map<String,Object> datas = new HashMap<>();
        Map<String,Object> data = null;
        Map<String,Object> tmpdatas = new HashMap<>();
        Map<String,Object> tmpdatass = new HashMap<>();
        Map<String,Object> tmpdata = null;
        Map<String,Object> tmpmap = null;

        Map<String,String> tmpstatusdata = new HashMap<>();
        Map<String,String> tmpstatus = null;
        RecordSet rs = new RecordSet();
        String sql = "";

        //add
        String unit = "小时";
        if(uintType==1 || uintType== 2 || uintType== 4){//按天计算
            unit = "天";
        }
        //kqLog.info("detail.flowdata="+JSONObject.toJSONString(flowData));

        try {
            sql = " select resourceid, kqdate,GROUPNAME, workMins,attendanceMins,signindate,signintime,signoutdate,signouttime,signinid,signoutid, belatemins, graveBeLateMins, leaveearlymins, graveLeaveEarlyMins, absenteeismmins, forgotcheckMins, forgotBeginWorkCheckMins, "+
                    " leaveMins,leaveInfo,evectionMins,outMins " +
                    " from kq_format_detail,KQ_GROUP " +
                    " where resourceid = ? and kqdate>=? and kqdate<=? and kq_format_detail.GROUPID = KQ_GROUP.ID "+
                    " order by resourceid, kqdate, serialnumber  ";
            rs.executeQuery(sql,resourceId, fromDate,toDate);
            while (rs.next()) {
                String key = rs.getString("resourceid") + "|" + rs.getString("kqdate");
                int workMins = rs.getInt("workMins");

                String attendanceMins = rs.getString("attendanceMins");
//        String chuqin = "出勤："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(attendanceMins) / 60.0)))+"小时";
                String overtimekey = key+"|overtime";
                String overtime = Util.null2String(flowData.get(overtimekey));
                boolean hasovertime = Util.getDoubleValue(overtime)>0;
                overtime = hasovertime?(SystemEnv.getHtmlLabelName(6151, user.getLanguage())+"："+overtime+unit):"";//显示加班

                String signinid = "";
                String signoutid = "";
                String signintime = "";
                String signouttime = "";
                if("1".equals(show_card_source)){
                    String nosign = SystemEnv.getHtmlLabelName(25994, user.getLanguage());//未打卡
                    signinid = Util.null2String(rs.getString("signinid")).trim();
                    signintime = Util.null2String(rs.getString("signintime")).trim();
                    String tmpin = SystemEnv.getHtmlLabelName(21974, user.getLanguage())+"：";
                    if(signinid.length()>0){
                        String signinfrom = Util.null2String(flowData.get(signinid));
                        signintime = tmpin+signintime+" "+signinfrom;
                    }else{
                        signintime = tmpin+nosign;
                    }
//          signintime = isWrap?"\r\n"+signintime:"<br/>"+signintime;

                    signoutid = Util.null2String(rs.getString("signoutid")).trim();
                    signouttime = Util.null2String(rs.getString("signouttime")).trim();
                    String tmpout = SystemEnv.getHtmlLabelName(21975, user.getLanguage())+"：";
                    if(signoutid.length()>0){
                        String signoutfrom = Util.null2String(flowData.get(signoutid));
                        signouttime = tmpout+signouttime+" "+signoutfrom;
                    }else{
                        signouttime = tmpout+nosign;
                    }
                    signouttime = isWrap?"\r\n"+signouttime:"<br/>"+signouttime;
                }

                int beLateMins = rs.getInt("beLateMins");
                int leaveEarlyMins = rs.getInt("leaveEarlyMins");
                int graveBeLateMins = rs.getInt("graveBeLateMins");
                int absenteeismMins = rs.getInt("absenteeismMins");
                int graveLeaveEarlyMins = rs.getInt("graveLeaveEarlyMins");
                int forgotCheckMins = rs.getInt("forgotCheckMins");
                int forgotBeginWorkCheckMins = rs.getInt("forgotBeginWorkCheckMins");
                int leaveMins = rs.getInt("leaveMins");
                String leaveInfo = rs.getString("leaveInfo");
                int evectionMins = rs.getInt("evectionMins");
                int outMins = rs.getInt("outMins");
                String groupName = rs.getString("GROUPNAME");//考勤组名称
                String text = "";
                String tmptext ="";
                String flag ="true";
                if(datas.get(key)==null){
                    data = new HashMap<>();
                }else{
                    data = (Map<String,Object>)datas.get(key);
                    tmptext = Util.null2String(data.get("text"));
                }
                tmpdata = new HashMap<>();
                if(tmpdatas.get(key)!=null){
                    tmpmap = (Map<String,Object>)tmpdatas.get(key);
                    flag = Util.null2String(tmpmap.get("text"));
                }

                String yichang ="";
                if(tmpstatusdata.get(key)!=null){
                    yichang = Util.null2String(tmpstatusdata.get(key));
                }
                String sign ="";
                String signkey = key+"|text";
                if(tmpstatusdata.get(signkey)!=null){
                    sign = Util.null2String(tmpstatusdata.get(signkey));
                }

                if (workMins<=0) {
                    if(text.length()>0) text +=" ";
                    text += SystemEnv.getHtmlLabelName(26593, user.getLanguage());
                    //休息日处理
                    if(signinid.length()>0){
                        text += (isWrap?"\r\n":"<br/>")+signintime;
                    }
                    if(signoutid.length()>0){
                        text += signouttime;
                    }
                    if(sign.length()>0) sign += isWrap?"\r\n":"<br/>";
                    sign += text;
                } else {
                    //处理打卡数据==================
                    if(text.length()>0) text+= isWrap?"\r\n":"<br/>";
                    text += signintime;
                    text += signouttime;
                    if(sign.length()>0) sign+= isWrap?"\r\n":"<br/>";
                    sign += text;
                    //处理打卡数据==================

                    if (absenteeismMins > 0) {//旷工
                        if(text.length()>0) text+=" ";
                        text += SystemEnv.getHtmlLabelName(20085, user.getLanguage());
//            text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+absenteeismMins) / 60.0)))+"小时";
                        if(yichang.indexOf(SystemEnv.getHtmlLabelName(20085, user.getLanguage()))==-1){
                            if(yichang.length()>0) yichang+= isWrap?"\r\n":"<br/>";
                            yichang += SystemEnv.getHtmlLabelName(20085, user.getLanguage());
                        }
                    }else {
                        if (beLateMins > 0) {//迟到
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20081, user.getLanguage());
//              text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+beLateMins) / 60.0)))+"小时";
                            if(yichang.indexOf(SystemEnv.getHtmlLabelName(20081, user.getLanguage()))==-1) {
                                if (yichang.length() > 0) yichang += isWrap?"\r\n":"<br/>";
                                yichang += SystemEnv.getHtmlLabelName(20081, user.getLanguage());
                            }
                        }
                        if (graveBeLateMins > 0) {//严重迟到
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(500546, user.getLanguage());
//              text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+graveBeLateMins) / 60.0)))+"小时";
                            if(yichang.indexOf(SystemEnv.getHtmlLabelName(500546, user.getLanguage()))==-1) {
                                if (yichang.length() > 0) yichang += isWrap?"\r\n":"<br/>";
                                yichang += SystemEnv.getHtmlLabelName(500546, user.getLanguage());
                            }
                        }
                        if (leaveEarlyMins > 0) {//早退
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20082, user.getLanguage());
//              text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+leaveEarlyMins) / 60.0)))+"小时";
                            if(yichang.indexOf(SystemEnv.getHtmlLabelName(20082, user.getLanguage()))==-1) {
                                if (yichang.length() > 0) yichang += isWrap?"\r\n":"<br/>";
                                yichang += SystemEnv.getHtmlLabelName(20082, user.getLanguage());
                            }
                        }
                        if (graveLeaveEarlyMins > 0) {//严重早退
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(500547, user.getLanguage());
//              text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+graveLeaveEarlyMins) / 60.0)))+"小时";
                            if(yichang.indexOf(SystemEnv.getHtmlLabelName(500547, user.getLanguage()))==-1) {
                                if (yichang.length() > 0) yichang += isWrap?"\r\n":"<br/>";
                                yichang += SystemEnv.getHtmlLabelName(500547, user.getLanguage());
                            }
                        }
                        if (forgotCheckMins > 0) {//漏签
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20086, user.getLanguage());
                            if(yichang.indexOf(SystemEnv.getHtmlLabelName(20086, user.getLanguage()))==-1) {
                                if (yichang.length() > 0) yichang += isWrap?"\r\n":"<br/>";
                                yichang += SystemEnv.getHtmlLabelName(20086, user.getLanguage());
                            }
                        }
                        if (forgotBeginWorkCheckMins > 0) {//漏签
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(20086, user.getLanguage());
                            if(yichang.indexOf(SystemEnv.getHtmlLabelName(20086, user.getLanguage()))==-1) {
                                if (yichang.length() > 0) yichang += isWrap?"\r\n":"<br/>";
                                yichang += SystemEnv.getHtmlLabelName(20086, user.getLanguage());
                            }
                        }
                    }
                }
                if (leaveMins > 0) {//请假
                    Map<String,Object> jsonObject = null;
                    if(leaveInfo.length()>0){
                        jsonObject = JSON.parseObject(leaveInfo);
                        for (Map.Entry<String,Object> entry : jsonObject.entrySet()) {
                            String newLeaveType = entry.getKey();
                            String tmpLeaveMins = Util.null2String(entry.getValue());
                            if(text.indexOf(kqLeaveRulesComInfo.getLeaveName(newLeaveType))==-1){
                                if (text.length() > 0) text += " ";
                                //text += kqLeaveRulesComInfo.getLeaveName(newLeaveType)+tmpLeaveMins+SystemEnv.getHtmlLabelName(15049, user.getLanguage());
                                text += Util.formatMultiLang( kqLeaveRulesComInfo.getLeaveName(newLeaveType),""+user.getLanguage());
//                text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+leaveMins) / 60.0)))+"小时";
                                if(yichang.length()>0) yichang+= isWrap?"\r\n":"<br/>";
                                yichang += Util.formatMultiLang( kqLeaveRulesComInfo.getLeaveName(newLeaveType),""+user.getLanguage());
                            }
                        }
                    }else{
                        if(text.indexOf(SystemEnv.getHtmlLabelName(670, user.getLanguage()))==-1) {
                            if (text.length() > 0) text += " ";
                            text += SystemEnv.getHtmlLabelName(670, user.getLanguage());
                        }
                    }
                }
                if (evectionMins > 0) {//出差
                    if(text.indexOf(SystemEnv.getHtmlLabelName(20084, user.getLanguage()))==-1) {
                        if (text.length() > 0) text += " ";
                        text += SystemEnv.getHtmlLabelName(20084, user.getLanguage());
//            text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+evectionMins) / 60.0)))+"小时";
                        if(yichang.length()>0) yichang+= isWrap?"\r\n":"<br/>";
                        yichang += SystemEnv.getHtmlLabelName(20084, user.getLanguage());
                    }
                }
                if (outMins > 0) {//公出
                    if(text.indexOf(SystemEnv.getHtmlLabelName(24058, user.getLanguage()))==-1) {
                        if (text.length() > 0) text += " ";
                        text += SystemEnv.getHtmlLabelName(24058, user.getLanguage());
//            text += "："+KQDurationCalculatorUtil.getDurationRound(("" + (Util.getDoubleValue(""+outMins) / 60.0)))+"小时";
                        if(yichang.length()>0) yichang+= isWrap?"\r\n":"<br/>";
                        yichang += SystemEnv.getHtmlLabelName(24058, user.getLanguage());
                    }
                }

                if(text.length()==0) {
                    text = "√";
                }else{
                    flag = "false";//有其他的异常状态,则表示为false，不需要处理直接全部显示即可
                }
                text += overtime;



                //需要处理下打卡时间和异常状态显示的顺序--start
                tmpstatusdata.put(key, yichang);
                tmpstatusdata.put(signkey, sign);
                boolean hasyichang = tmpstatusdata.get(key).length()>0;
                if(tmptext.length()>0){
//          text = tmpstatusdata.get(signkey)+(isWrap?"\r\n":"<br/>")+tmpstatusdata.get(key)+(isWrap?"\r\n":"<br/>"+overtime);
                    text = tmpstatusdata.get(signkey);
                    if(hasyichang){
                        if(text.length()>0){
                            text += (isWrap?"\r\n":"<br/>")+tmpstatusdata.get(key);
                        }else{
                            text += tmpstatusdata.get(key);
                        }
                    }
                    if(hasovertime){
                        text += (isWrap?"\r\n":"<br/>")+overtime;
                    }
                }else{
                    text = tmpstatusdata.get(signkey);
                    if(hasyichang){
                        if(text.length()>0){
                            text += (isWrap?"\r\n":"<br/>")+tmpstatusdata.get(key);
                        }else{
                            text += tmpstatusdata.get(key);
                        }
                    }
                    if(hasovertime){
                        text += (isWrap?"\r\n":"<br/>")+overtime;
                    }
                }
                //需要处理下打卡时间和异常状态显示的顺序--end
                tmpdatass.put(key, (isWrap?"\r\n":"<br/>")+overtime);
//        text = tmptext.length()>0?tmptext+" "+text:text;//显示所有的状态

                new BaseBean().writeLog("==zj==(groupName)" + JSON.toJSONString(groupName));
                if (groupName.length() > 0){
                    if (text.indexOf("考勤组:"+groupName) == -1){
                        text +=  (isWrap?"\r\n":"<br/>")+"考勤组:"+groupName;
                    }
                }

                data.put("text", text);
                datas.put(key, data);

                //add
                tmpdata.put("text", flag);
                tmpdatas.put(key, tmpdata);
                //end
            }
            //全部搞一遍
            if(tmpdatas != null){
//        writeLog(n+">>>tmpdatas="+JSONObject.toJSONString(tmpdatas));
                Map<String,Object> data1 = null;
                for(Map.Entry<String,Object> entry : tmpdatas.entrySet()){
                    String mapKey = Util.null2String(entry.getKey());
                    Map<String,Object> mapValue = (Map<String,Object>)entry.getValue();
                    String flag = Util.null2String(mapValue.get("text"));
                    if("true".equals(flag)){//需要加工的数据
                        String  overtime = String.valueOf(tmpdatass.get(mapKey));
                        data1 = new HashMap<>();
                        data1.put("text", "√"+overtime);
                        datas.put(mapKey, data1);
                    }
                }
//        writeLog("datas="+JSONObject.toJSONString(datas));
            }
        }catch (Exception e){
            new BaseBean().writeLog(e);
        }
        // 最后针对数据再处理一遍，不然出现2023-01-02: "休息" 形式的错误，导致页面记载报错，应该是2023-01-01: {text: "休息"}格式
        boolean isEnd = false;
        for(String currentDate = fromDate; !isEnd;) {
            if (currentDate.equals(toDate)) isEnd = true;
            String dailyValue = Util.null2String(datas.get(currentDate));
            if(!"".equals(dailyValue) && !dailyValue.contains("text")) {
                Map<String,Object> innerMap2 = new HashMap<>();
                innerMap2.put("text", dailyValue);
                datas.put(currentDate, innerMap2);
            }
            currentDate = DateUtil.addDate(currentDate, 1);
        }

        return datas;
    }
}
