package com.customization.SMSapproval.Action;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.sms.SmsService;

import java.util.HashMap;
import java.util.Map;

public class SmsAction implements SmsService {
    private String url;
    private String account;
    private String pwd;
    private String taskId;
    private String mobiles;
    private String content;
    private String extNo;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMobiles() {
        return mobiles;
    }

    public void setMobiles(String mobiles) {
        this.mobiles = mobiles;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExtNo() {
        return extNo;
    }

    public void setExtNo(String extNo) {
        this.extNo = extNo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean sendSMS(String smsId, String number, String msg) {
        new BaseBean().writeLog("==zj==" + "sendSMS(" + "smsId:" + smsId + "number:" + number + "msg:" + msg + ")");
        mobiles = number;
        content = msg;
        new BaseBean().writeLog("==zj==账号、密码、url" +"账号：" + account + ",密码:" + pwd +",url：" + url);

            //MD5加密前
            new BaseBean().writeLog("==zj==" + "pwd（加密前）:" + pwd);
            //MD5加密后
            String pwdMD5 = DigestUtils.md5Hex(pwd);
            new BaseBean().writeLog("==zj==" + "pwd（加密后）:" + pwdMD5);


        //生成递增拓展号
        extNo = "000";

       RecordSet rw = new RecordSet();
       String rwsql = "SELECT top 1 extNo FROM Custom01_Message order by id desc";
       rw.executeQuery(rwsql);
       if (rw.next()){
            new BaseBean().writeLog("==zj==rw语句开始执行");
            extNo = Util.null2String(rw.getString("extNo"));

           int i  = Util.getIntValue(extNo) + 1;
           new BaseBean().writeLog("==zj==(flag==0)" + "i:" + i + ",extNo:" + extNo);
           if (i<10){
               extNo = "00" + Util.null2String(i); //如果拓展号小于10前面添加“00”
           }else if (i<100){
               extNo = "0" + Util.null2String(i);//如果拓展号大于10小于99前面添加"0"
           } else if(i==1000){
               extNo = "000";
           } else {
               extNo = Util.null2String(i);
           }
       }



    new BaseBean().writeLog("==zj==extNo(最终发送结果)" + extNo);

        Map paramMap = new HashMap();
        paramMap.put("account",account);
        paramMap.put("pwd",pwdMD5);
        paramMap.put("mobiles",mobiles);
        paramMap.put("content",content);
        paramMap.put("extNo",extNo);


        new BaseBean().writeLog("==zj==" + "paramMap（数据）:" + JSON.toJSONString(paramMap));

        //获取requestId和userid
        RecordSet rs = new RecordSet();
        int  requestid = 0;
        int userid = 0;
        String requestIdsql = "select * from SMS_Message where id = " + smsId;
        rs.executeQuery(requestIdsql);
        if (rs.next()){
             requestid = rs.getInt("requestid");
             userid = rs.getInt("userid");
        }


        //短信发送
        String result2 = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, "application/x-www-form-urlencoded")//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute().body();
        JSONObject parse = (JSONObject) JSONObject.parse(result2);
        new BaseBean().writeLog("==zj==短信发送（内容）:" + Util.null2String(parse));
        String code =  parse.getString("code");
        //如果发送成功
        if (code.equals("00")){
            new BaseBean().writeLog("==zj==发送短息（获取requestid和userid）:" +"requestid:" + requestid + ",userid:" + userid);
            //查询拓展号是否到达上限999
            String extNoflag = "";  //创建一个extNo标识变量
            RecordSet re = new RecordSet();
            String extNosql = "SELECT top 1 extNo FROM Custom01_Message order by id desc";
            re.executeQuery(extNosql);
            if (re.next()){
                extNoflag = Util.null2String(re.getString("extNo"));
            }
            new BaseBean().writeLog("==zj==extNoflag" + extNoflag);



                RecordSet rd = new RecordSet();
                String rdsql = "delete from Custom01_Message where extNo = " + extNo;
                rd.executeUpdate(rdsql);


                String smsMessagesql = "insert into Custom01_Message(extNo,requestid,userid) values(" + "'" + extNo + "'," + requestid + "," + userid + ")";
                new BaseBean().writeLog("==zj==" + "短信发送成功执行sql语句:" + smsMessagesql);
               boolean sqlflag =  rd.executeUpdate(smsMessagesql);
               if (sqlflag){
                   new BaseBean().writeLog("==zj==sql执行成功");
               }else {
                   new BaseBean().writeLog("==zj==sql执行失败");
               }
            return true;
        }else {
            return false;
        }
    }

}
