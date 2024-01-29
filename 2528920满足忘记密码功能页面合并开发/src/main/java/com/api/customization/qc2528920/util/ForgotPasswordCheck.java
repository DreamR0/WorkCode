package com.api.customization.qc2528920.util;

import com.alibaba.fastjson.JSON;
import com.api.login.biz.LoginBiz;
import weaver.common.DateUtil;
import weaver.file.ValidateFromEnum;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ForgotPasswordCheck {
    private HttpServletRequest request;
    private Map<String, Object> params ;
    private User user ;

    public ForgotPasswordCheck(Map<String, Object> params, User user, HttpServletRequest request){
        this.params  = params;
        this.user = user;
        this.request = request;
    }

    public Map<String, Object> check(){
        Map<String,Object> retmap = new HashMap<String,Object>();
        int language =  Util.getIntValue(Util.null2String(params.get("languageId")));
        if(user == null){
            user = new User();
            user.setLanguage(language);
        }
        try {

            StringBuffer result = new StringBuffer();
            String id = Util.null2String(params.get("id"));
            String loginid = Util.null2String(params.get("loginid"));
            loginid = LoginBiz.getLoginId(loginid);
            String phoneCodeInp = Util.null2String(params.get("phoneCode"));

            HttpSession session = request.getSession(true);
            Map<String,String> sessionMap = (Map<String,String>)session.getAttribute("phoneSessionMap");
            sessionMap = sessionMap==null? new HashMap<String,String>():sessionMap;
            if(sessionMap.get(loginid) == null){
                result.append("");
            }else{
                String nowtime = DateUtil.getFullDate();
                String sixtyTime = sessionMap.get(loginid);
                if(nowtime.compareTo(sixtyTime) > 0 ){
                    result.append("");
                    sessionMap.remove(loginid);
                    request.getSession(true).removeAttribute("phoneCode");
                    retmap.put("status", "-1");
                    retmap.put("message", SystemEnv.getHtmlLabelName(501512, user.getLanguage()));
                    return retmap;
                }
                String validateRand="";
                String original = ValidateFromEnum.ORIGINAL.getSource();
                String source = ValidateFromEnum.FORGORT_PASSWORD.getSource();
                validateRand=Util.null2String((String)request.getSession(true).getAttribute(original+source));

                //验证码每次使用完之后都要销毁
                request.getSession(true).removeAttribute(original+source);
                String phoneCode="";
                phoneCode=Util.null2String((String)request.getSession(true).getAttribute("phoneCode"));
                    if("".equals(phoneCodeInp.trim().toLowerCase())){
                        result.append("");
                    }else{
                        if(!phoneCode.toLowerCase().equals(phoneCodeInp.trim().toLowerCase())){
                            result.append("");
                        }else{
                            request.getSession(true).setAttribute("validateLoginid",loginid);
                            result.append(new Random().nextInt()+"");
                        }
                    }

            }
            if("".equals(result.toString())){
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(501511, user.getLanguage()));
            }else{
                retmap.put("status", "1");
            }
        } catch (Exception e) {
            retmap.put("status", "-1");
            retmap.put("message", SystemEnv.getHtmlLabelName(84566, user.getLanguage()));
            new BaseBean().writeLog("==zj==(验证手机号码报错)" + JSON.toJSONString(e));
        }
        return retmap;
    }
}
