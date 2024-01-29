package com.api.customization.qc2654238.util;

import com.alibaba.fastjson.JSON;
import com.api.doc.detail.util.DocDownloadCheckUtil;
import com.engine.odoc.util.DocUtil;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;

import java.util.HashMap;
import java.util.Map;

public class CheckUtil {
    String setRemindTable = new BaseBean().getPropValue("qc2654238","setRemindTable");

    /**
     * 这里更换背景图和祝福信息
     * @param user
     * @param yearNum
     * @return
     */
    public Map<String,Object> remindCheck(User user, int yearNum){
        Map<String,Object> resultMap = new HashMap<>();
        RecordSet rs = new RecordSet();
        String sql = "";
        String url="";  //背景图片地址
        String docid = "";
        String imagfileid = "";
        String entryCongrats = "";   //祝福词

        if (yearNum >= 20){
            yearNum = 20;
        }
        //获取该人员图片名和祝福词
        sql ="select rznx,txnr,bjt from "+setRemindTable+" where  FIND_IN_SET('"+yearNum+"',rznx) > 0 and "+
        "FIND_IN_SET('"+user.getUID()+"',txfw) > 0";
        new BaseBean().writeLog("==zj==(周年提醒sql)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            docid = Util.null2String(rs.getString("bjt"));
            entryCongrats = Util.null2String(rs.getString("txnr"));
            new BaseBean().writeLog("==zj==(获取提示语)" + JSON.toJSONString(entryCongrats));
        }
        entryCongrats = entryCongrats.replace("\n","<br>");

        new BaseBean().writeLog("==zj==(处理之后)" + entryCongrats);
        sql = "select * from docimagefile where docid='"+docid+"'";
        new BaseBean().writeLog("==zj==(==zj==(docidsql))" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            url = "/weaver/weaver.file.FileDownload?fileid=" + Util.null2String(rs.getString("imagefileid"));
        }

            resultMap.put("url",url);
            resultMap.put("entryCongrats",entryCongrats);

        return  resultMap;
    }

    /**
     * 检查当前人员以及入职周年是否有在维护表里
     * @param user
     * @return
     */
    public Boolean isSet(User user, int yearNum){
        Boolean isSet = false;
        RecordSet rs1 = new RecordSet();
        String sql = "";
        if (yearNum >= 20){
            yearNum = 20;
        }
        sql ="select rznx,txnr,bjt from "+setRemindTable+" where  FIND_IN_SET('"+user.getUID()+"',txfw) > 0 and  "+
        " FIND_IN_SET('"+yearNum+"',rznx) > 0 ";
        new BaseBean().writeLog("==zj==(周年提醒是否维护)" + JSON.toJSONString(sql));
        rs1.executeQuery(sql);
        if (rs1.next()){
            isSet = true;
        }
        return isSet;
    }

    /**
     * 判断周年庆图片是否有权限
     * @param fielid
     */
    public Boolean isHaseRight(int fielid){
        Boolean haseRight = false;
        String docid = "";
        //如果是周年提醒维护表的fieid的话就给权限
        RecordSet rs = new RecordSet();
        //先拿到该图片的docid
        String sql = "select * from docimagefile where imagefileid="+fielid;
        new BaseBean().writeLog("==zj==(docidsql)" + JSON.toJSONString(sql));

        rs.executeQuery(sql);
        if (rs.next()){
            docid = Util.null2String(rs.getString("docid"));
        }
        if (!"".equals(docid)){
            sql = "select * from "+setRemindTable+" where bjt="+docid;
            new BaseBean().writeLog("==zj==(bjtsql)" + JSON.toJSONString(sql));
            rs.executeQuery(sql);
            if (rs.next()){
                haseRight = true;
            }
        }
new BaseBean().writeLog("==zj==(haseRight)" + JSON.toJSONString(haseRight));
        return haseRight;
    }
}
