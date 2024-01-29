package com.api.customization.qc2528920.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.login.biz.LoginBiz;
import weaver.common.DateUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.file.ValidateFromEnum;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.common.Constants;
import weaver.hrm.passwordprotection.manager.HrmPasswordProtectionSetManager;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.sms.SMSSaveAndSend;
import weaver.sms.SmsFromMouldEnum;
import weaver.sms.SmsTemplateModuleType;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ForgotPasswordSend {
    private HttpServletRequest request;
    private Map<String, Object> params ;
    private User user ;

    public ForgotPasswordSend(Map<String, Object> params,User user,HttpServletRequest request){
        this.params  = params;
        this.user = user;
        this.request = request;
    }


    public Map<String, Object> send() {
        new BaseBean().writeLog("==zj==(前端参数params)" + JSON.toJSONString(params));
        Map<String,Object> retmap = new HashMap<String,Object>();
        int language =  Util.getIntValue(Util.null2String(params.get("languageId")),7);
        if(user == null){
            user = new User();
            user.setLanguage(language);
        }
        try {
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();
            String forgotpasswordExpiration = Util.null2s(settings.getForgotpasswordExpiration(),"60");
            Map<String,String> resultMap = new HashMap<>();
            String type = Util.null2String(params.get("type"));
            switch (type){
                case "phoneCode":
                    retmap.put("forgotpasswordExpiration", forgotpasswordExpiration);
                    resultMap = confirmPhoneCode(forgotpasswordExpiration);
                    break;
            }

            if(resultMap.get("-1") != null){
                retmap.put("status", "-1");
                retmap.put("message", resultMap.get("-1"));
            }else{
                retmap.put("status", "1");
                retmap.put("id", resultMap.get("1"));
                //qc2528920
                retmap.put("userId",resultMap.get("userId"));
            }

        } catch (Exception e) {
            retmap.put("status", "-1");
            retmap.put("message", SystemEnv.getHtmlLabelName(84566, user.getLanguage()));
            new BaseBean().writeLog("==zj==" + JSON.toJSONString(e));
        }
        return retmap;
    }

    /**
     * 手机号码找回密码校验
     * @param forgotpasswordExpiration
     */
    private Map<String,String> confirmPhoneCode(String forgotpasswordExpiration) throws Exception{
        Map<String,String> resultMap = new HashMap<>();
        StringBuffer result = new StringBuffer();
        String id = Util.null2String(params.get("id"));
        String loginid = Util.null2String(params.get("loginid"));
        if (loginid == null || "".equals(loginid) || loginid.length() <= 0){
            resultMap.put("-1","请输入账号");
            return resultMap;
        }
        loginid = LoginBiz.getLoginId(loginid);
        String validatecode = Util.null2String(params.get("validatecode"));

        String content = "";
        String receiver = getReceiverByLoginid(loginid,"sendSMS");

        HttpSession session = this.request.getSession(true);
        Map<String,String> sessionMap = (Map<String,String>)session.getAttribute("phoneSessionMap");
        sessionMap = sessionMap==null? new HashMap<String,String>():sessionMap;
        if(sessionMap.get(loginid) != null ){
            String nowtime = DateUtil.getFullDate();
            String sixtyTime = sessionMap.get(loginid);
            if(nowtime.compareTo(sixtyTime) > 0 ){
                sessionMap.remove(loginid);
                request.getSession(true).removeAttribute("phoneCode");
            } else {
                resultMap.put("-1", SystemEnv.getHtmlLabelName(388285, user.getLanguage()));
                return resultMap;
            }
        }

        if(sessionMap.get(loginid) != null ){
            resultMap.put("-1", SystemEnv.getHtmlLabelName(125974, user.getLanguage()));
            return resultMap;
        }else{
            String newPassword = "";
            boolean isChange = false;
            String validateRand="";
            String original = ValidateFromEnum.ORIGINAL.getSource();
            String source = ValidateFromEnum.FORGORT_PASSWORD.getSource();
            validateRand=Util.null2String((String)request.getSession(true).getAttribute(original+source));

            //在ForgotPasswordCheckMsgCmd类里面已经校验了验证码了，二次检验，就会失败。
            //验证码每次使用完之后都要销毁
//      request.getSession(true).removeAttribute(original+source);
//      if(!validateRand.toLowerCase().equals(validatecode.trim().toLowerCase()) || "".equals(validatecode.trim().toLowerCase()) ){
//        resultMap.put("-1", SystemEnv.getHtmlLabelName(128878, user.getLanguage()));
//        return resultMap;
//      }else{
            HrmPasswordProtectionSetManager manager = new HrmPasswordProtectionSetManager();
            /*if(StringUtil.isNotNull(id) && !id.equals("0")){*/
                newPassword = StringUtil.randomString(6, 0);//很对get方法提交的时候特殊字符导致数据传输有误的问题，把验证码取值内容写死成6位随机数字
                content = StringUtil.replace(getPhoneCodeMessage(user), "{pswd}", newPassword);
                isChange = true;
            /*}*/
            String phone = "";
            //boolean bool = MessageUtil.sendSMS(receiver, content);
            SMSSaveAndSend sms=new SMSSaveAndSend();
            String msg = content ;
            sms.setMessage(msg);
            sms.setFrommould(SmsFromMouldEnum.HRM);
            sms.setSmsTemplateModuleType(SmsTemplateModuleType.COMMON_FORGOT_PSWD);
            sms.setCustomernumber(receiver);
            JSONObject jsonParams = new JSONObject() ;
            jsonParams.put("pswd",newPassword) ;
            sms.setSendParams(jsonParams);
            sms.setUserid(1);//系统发送
            boolean bool = sms.send();

            new BaseBean().writeLog("SMSSaveAndSend  send receiver["+receiver+"] content："+content);
            if(bool && isChange) {
                //manager.changePassword(id, loginid, newPassword);
                if(receiver.length() - 4 > 0){
                    phone = receiver.substring(0, receiver.length() - 4);
                }
                phone += "****";
            }else {
                new BaseBean().writeLog("短信发送失败，请检查短信服务器");
                resultMap.put("-1", SystemEnv.getHtmlLabelName(125974, user.getLanguage()));
                return resultMap;
            }
            if(!phone.equals("")){
                if(!"".equals(newPassword)){
                    // 将手机验证码存入session
                    session.setAttribute("phoneCode", newPassword);
                    Calendar cal = DateUtil.getCalendar(DateUtil.getFullDate());
                    int sessionSec = Constants.SessionSec;
                    if(forgotpasswordExpiration.length() > 0 && Util.getIntValue(forgotpasswordExpiration) > -1){
                        sessionSec = Util.getIntValue(forgotpasswordExpiration);
                    }
                    cal.add(Calendar.SECOND, sessionSec);
                    String sixtyTime = DateUtil.getFullDate(cal.getTime());
                    sessionMap.put(loginid,sixtyTime);
                    session.setAttribute("phoneSessionMap",sessionMap);
                }

            }
            result.append(phone);
            resultMap.put("1", phone);
//      }
        }

        //qc2528920
        String userId = "";
        RecordSet rs = new RecordSet();
        String sql = "select * from hrmresource where loginid = '"+loginid+"'";
        new BaseBean().writeLog("==zj==(人员id)" + JSON.toJSONString(sql));
        rs.executeQuery(sql);
        if (rs.next()){
            userId = Util.null2String(rs.getString("id"));
        }
        resultMap.put("userId",userId);
        return resultMap;
    }

    private static String getReceiverByLoginid(String loginid,String type){
        RecordSet RS = new RecordSet();
        String receiver = "";
        String sql = "select * from HrmResource where status in (0,1,2,3) and loginid='"+loginid+"' and (accounttype = 0 or accounttype is null)";
        RS.executeSql(sql);
        String mobile="",email="";
        if(RS.next()){
            mobile = RS.getString("mobile");
            email = RS.getString("email");
        }
        if(mobile.equals("")){
            sql = "select * from HrmResourceManager where loginid=?";
            RS.executeQuery(sql,loginid);
            if(RS.next()){
                mobile = RS.getString("mobile");
            }
        }
        if("sendSMS".equals(type)){
            receiver = mobile;
        }else if("sendEmail".equals(type)){
            receiver = email;
        }
        return receiver;
    }

    public String getPhoneCodeMessage(User user){
        return SystemEnv.getHtmlLabelName(389233,Util.getIntValue(user.getLanguage()));
    }

    public String getEmailCodeMessage(User user){
        return SystemEnv.getHtmlLabelName(389234,Util.getIntValue(user.getLanguage()));
    }
}
