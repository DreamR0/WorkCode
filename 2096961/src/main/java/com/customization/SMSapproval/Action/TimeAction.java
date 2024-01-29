package com.customization.SMSapproval.Action;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.customization.SMSapproval.Util.smsUtil;
import com.engine.workflow.entity.publicApi.PAResponseEntity;
import org.apache.commons.codec.digest.DigestUtils;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.interfaces.schedule.BaseCronJob;

import java.util.HashMap;
import java.util.Map;

public class TimeAction extends BaseCronJob {
    @Override
    public void execute() {
        new BaseBean().writeLog("==zj==" + "定时任务开启");

        //url,账号,密码
        String url = "http://120.26.69.132:7891/api/v1/mo";
        String account = "922268";
        String pwd = "UdHpIVc4";

        //MD5加密
        String pwdMD5 = DigestUtils.md5Hex(pwd);
        new BaseBean().writeLog("==zj==" + "pwdMD5（接收消息）:"  +  pwdMD5);

        //接收消息
        MessageAccept(url,account,pwdMD5);

    }

    //接收消息方法
    public void  MessageAccept(String url,String account,String pwd){
        Map paramMap = new HashMap();
        paramMap.put("account",account);
        paramMap.put("pwd",pwd);
        String msmAccept = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, "application/x-www-form-urlencoded")//头信息，多个头信息多次调用此方法即可
                .form(paramMap)//表单内容
                .timeout(20000)//超时，毫秒
                .execute().body();
        new BaseBean().writeLog("==zj==" + "msmAccept:" + msmAccept);

        //将接收到的信息转换为json格式
        JSONObject parse =  JSONObject.parseObject(msmAccept);
        new BaseBean().writeLog("==zj==" + "parse(接收消息):" + JSON.toJSONString(parse));
        String Data = parse.getString("data");

        //判断Data数据是否为空
        Boolean flag = false;
        if (Data.equals("[]")){
            flag = false;
        }else {
            flag = true;
        }
        if (flag){
            new BaseBean().writeLog("==zj==" + "数组Data不为空:" + Data);
            //获取parse的data数组
            JSONArray jsonArray = parse.getJSONArray("data");
            new BaseBean().writeLog("==zj==" + "jsonArray(数组数据):" + JSON.toJSONString(jsonArray));
            //获取数组中content内容，extNo内容
            Object contentObject = jsonArray.getJSONObject(0).get("content");
            Object extNoObject = jsonArray.getJSONObject(0).get("extNo");
            String content = (String) contentObject;
            String extNo = (String) extNoObject;
            extNo = extNo.substring(1);
            new BaseBean().writeLog("==zj==extNo截取结果:" + extNo);

            //判断短信消息内容
            //如果同意
            if (content.equals("同意")){
                 new BaseBean().writeLog("==zj==短信内容“同意”：开始执行方法");

                //查询Custom01_Message表，获取requestid和userid
                int requestid = 0;
                int userid = 0;
                RecordSet rs = new RecordSet();
                String reIdsql = "select * from Custom01_Message where extNo= "+ extNo;
                new BaseBean().writeLog("==zj==短信内容（同意）查询语句：" + reIdsql);
                rs.executeQuery(reIdsql);
                if (rs.next()){
                    requestid = rs.getInt("requestid");
                }

                //创建审批者user类
                RecordSet ru = new RecordSet();
                String rusql = "select top 1 touserid from SMS_Message where requestid= " + requestid + "order by id desc";

                ru.executeQuery(rusql);
                if (ru.next()){
                    userid = ru.getInt("touserid");
                }
                new BaseBean().writeLog("==zj==创建审批者id:" + userid);
                User user = new User(userid);

                //提交流程
                new BaseBean().writeLog("==zj==提交流程参数信息=====" + "requestid:" + requestid + ",user:" + JSON.toJSONString(user));
                smsUtil util = new smsUtil();

                //接收返回结果
                PAResponseEntity result = null;
                result = util.submit(requestid,user);


                new BaseBean().writeLog("==zj==(提交流程返回结果)" + JSON.toJSONString(result));
                new BaseBean().writeLog("==zj==提交流程执行完毕（同意）");
            }

            //如果不同意
            if (content.equals("不同意")){
                new BaseBean().writeLog("==zj==" + "不同意方法开始执行");

                //查询Custom01_Message表，获取requestid和userid
                int requestid = 0;
                int userid = 0;
                RecordSet rs = new RecordSet();
                String reIdsql = "select * from Custom01_Message where extNo= "+ extNo;
                new BaseBean().writeLog("==zj==短信内容（不同意）查询语句：" + reIdsql);
                rs.executeQuery(reIdsql);
                if (rs.next()){
                    requestid = rs.getInt("requestid");
                }

                //创建审批者user类
                RecordSet ru = new RecordSet();
                String rusql = "select top 1 touserid from SMS_Message where requestid= " + requestid + "order by id desc";
                ru.executeQuery(rusql);
                if (ru.next()){
                    userid = ru.getInt("touserid");
                }
                new BaseBean().writeLog("==zj==创建审批者id:" + userid);
                User user = new User(userid);

                //回退流程
                new BaseBean().writeLog("==zj==回退流程参数信息===" + "requestid:" + requestid + ",user:" + JSON.toJSONString(user));
                smsUtil smsUtil = new smsUtil();

                //接收返回结果
                PAResponseEntity result = null;
                 result = smsUtil.reject(requestid,user);


                new BaseBean().writeLog("==zj==(回退流程返回结果):" + JSON.toJSONString(result));
                new BaseBean().writeLog("==zj==回退流程执行完毕（不同意）");


            }
        }
    }
}
