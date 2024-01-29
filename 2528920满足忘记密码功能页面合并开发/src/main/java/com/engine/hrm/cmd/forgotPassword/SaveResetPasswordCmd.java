package com.engine.hrm.cmd.forgotPassword;

import com.api.hrm.util.PasswordReuseUtil;
import com.api.hrm.util.ServiceUtil;
import com.api.login.biz.LoginBiz;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.face.HrmFaceCheckManager;
import com.weaver.integration.ldap.sync.oa.OaSync;
import com.weaver.integration.ldap.util.AuthenticUtil;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.file.ValidateFromEnum;
import weaver.general.BaseBean;
import weaver.general.PasswordUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.common.AjaxManager;
import weaver.hrm.passwordprotection.manager.HrmPasswordProtectionSetManager;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.rsa.security.RSA;
import weaver.systeminfo.SystemEnv;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存修改密码
 *
 * @author pzy
 */
public class SaveResetPasswordCmd extends AbstractCommonCommand<Map<String, Object>> {

    private HttpServletRequest request;

    public SaveResetPasswordCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    public SaveResetPasswordCmd(Map<String, Object> params, User user, HttpServletRequest request) {
        this.user = user;
        this.params = params;
        this.request = request;
    }

    @Override
    public BizLogContext getLogContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> retmap = new HashMap<String, Object>();
        RecordSet rs = new RecordSet();
        int language = Util.getIntValue(Util.null2String(params.get("languageId")), 7);
        if (user == null) {
            user = new User();
            user.setLanguage(language);
        }
        try {
            StringBuffer result = new StringBuffer();
            String id = Util.null2String(params.get("id"));
            String loginid = Util.null2String(params.get("loginid"));
            loginid = LoginBiz.getLoginId(loginid);
            String validatecode = Util.null2String(params.get("validatecode"));
            String type = Util.null2String(params.get("type"));

            String validateRand = "";
            String original = ValidateFromEnum.ORIGINAL.getSource();
            String source = ValidateFromEnum.FORGORT_PASSWORD.getSource();
            validateRand = Util.null2String((String) request.getSession(true).getAttribute(original + source));

            //验证码每次使用完之后都要销毁
            request.getSession(true).removeAttribute(original + source);
            if (!validateRand.toLowerCase().equals(validatecode.trim().toLowerCase()) || "".equals(validatecode.trim().toLowerCase())) {
                retmap.put("status", "-1");
                retmap.put("message", SystemEnv.getHtmlLabelName(127829, user.getLanguage()));
                return retmap;
            }
            //QC273665 start  Ldap用户支持忘记密码功能 ，保存新密码
            String ret = "0";
            Map<String ,Object> authenticFromAD= new AuthenticUtil().isAuthenticFromAD(loginid);
             Boolean isUseLdap= (Boolean)authenticFromAD.get("result");
            if (isUseLdap) {
                    ret = "1";
            }
            //QC273665 end   Ldap用户支持忘记密码功能 ，保存新密码
            String validateLoginid = Util.null2String((String) request.getSession(true).getAttribute("validateLoginid"));
            request.getSession(true).removeAttribute("validateLoginid");
            if (validateLoginid.length() == 0 || !validateLoginid.toLowerCase().equals(loginid.trim().toLowerCase())) {
                retmap.put("status", "-1");
                retmap.put("message", "system error!");
                return retmap;
            }
            String newPassword = Util.null2String(params.get("newpswd"));
            //是否开启了RSA加密
            String openRSA = Util.null2String(Prop.getPropValue("openRSA", "isrsaopen"));
            List<String> passwordList = new ArrayList<String>();
            if ("1".equals(openRSA)) {
                passwordList.add(newPassword);

                RSA rsa = new RSA();
                List<String> resultList = rsa.decryptList(request, passwordList);
                newPassword = resultList.get(0);
            }
            boolean isAdmin = false;
            rs.executeQuery("select id from HrmResourceManager where loginid=?", loginid);
            if (rs.next()) {
                isAdmin = true;
            }
            rs.executeSql("select id from " + AjaxManager.getData(loginid, "getTResourceName;HrmResource") + " where loginid='" + loginid + "'");
            rs.next();
            String userid = StringUtil.vString(rs.getString("id"), "0");
            String qid = StringUtil.vString(params.get("question"), "0");
            if (!"-1".equals(type)) {
                if (ret.equals("1")) {

                        Map<String, String> map = new HashMap<>();
                        map.put("userid", userid);
                        map.put("loginid", loginid);
                        map.put("password", newPassword);
                        map.put("issysadmin", "true");
                        map.put("optype", "3");
                        Map<String,Object>  retInfo = new OaSync("", "").modifyADPWDNew(map);
                        if (!Util.null2String(retInfo.get("code")).equals("0")){
                            retmap.put("message", SystemEnv.getHtmlLabelNames(Util.null2String(retInfo.get("msg")), user.getLanguage()));
                            retmap.put("status", "-1");
                            return retmap;
                        } else {
                            /*如果关闭长时间未登录自动锁定功能，则自动解锁之前由于长时间未登录导致账号被锁定的账号*/
                            ChgPasswdReminder reminder = new ChgPasswdReminder();
                            RemindSettings settings = reminder.getRemindSettings();
                            String forgotpasswordrelieve = settings.getForgotpasswordrelieve();
                            if (forgotpasswordrelieve.equals("1")) {
                                if(isAdmin){
                                    String sql = " UPDATE HrmResourceManager SET passwordlock=0,passwordLockReason='' WHERE passwordlock=1 and id ="+id;
                                     rs.executeUpdate(sql);
                                }else{
                                    String sql = " UPDATE HrmResource SET passwordlock=0,passwordLockReason='' WHERE passwordlock=1 and id ="+id;
                                   rs.executeUpdate(sql);
                                }
                            }
                            retmap.put("status", "1");
                            retmap.put("message", SystemEnv.getHtmlLabelName(26713, user.getLanguage()));
                            return retmap;
                        }
                    
                } else {
                    new HrmPasswordProtectionSetManager().changePassword(null, loginid, newPassword);
                }
            } else {
                rs.executeSql("select 1 from hrm_protection_question where user_id=" + userid + " and id in(" + qid + ")");
                if (rs.next()) {
                    if (ret.equals("1")) {
                       
                            Map<String, String> map = new HashMap<>();
                            map.put("userid", userid);
                            map.put("loginid", loginid);
                            map.put("password", newPassword);
                            map.put("issysadmin", "true");
                            map.put("optype", "3");
                            Map<String,Object>  retInfo = new OaSync("", "").modifyADPWDNew(map);
                            if (!Util.null2String(retInfo.get("code")).equals("0")){
                                retmap.put("message", SystemEnv.getHtmlLabelNames(Util.null2String(retInfo.get("msg")), user.getLanguage()));
                                retmap.put("status", "-1");
                                return retmap;
                            } else {
                                /*如果关闭长时间未登录自动锁定功能，则自动解锁之前由于长时间未登录导致账号被锁定的账号*/
                                ChgPasswdReminder reminder = new ChgPasswdReminder();
                                RemindSettings settings = reminder.getRemindSettings();
                                String forgotpasswordrelieve = settings.getForgotpasswordrelieve();
                                if (forgotpasswordrelieve.equals("1")) {
                                    if(isAdmin){
                                        String sql = " UPDATE HrmResourceManager SET passwordlock=0,passwordLockReason='' WHERE passwordlock=1 and id ="+id;
                                        rs.executeUpdate(sql);
                                    }else{
                                        String sql = " UPDATE HrmResource SET passwordlock=0,passwordLockReason='' WHERE passwordlock=1 and id ="+id;
                                      rs.executeUpdate(sql);
                                    }
                                }
                                retmap.put("status", "1");
                                retmap.put("message", SystemEnv.getHtmlLabelName(26713, user.getLanguage()));
                                return retmap;
                            }
                        
                    } else {
                        new HrmPasswordProtectionSetManager().changePassword(id, loginid, newPassword);
                    }
                } else {
                    retmap.put("status", "-1");
                    retmap.put("message", "system error!");
                    return retmap;
                }
            }
            try{
                //登录信息签名
                PasswordUtil.saveSign(""+userid);
            }catch (Exception e){
                writeLog(e);
            }
            HrmFaceCheckManager.setUserPassowrd(userid, newPassword);
            String[] newPwdArr = PasswordUtil.encrypt(newPassword) ;
           String  newPasswordNew = newPwdArr[0] ;
            PasswordReuseUtil.saveHistoryPassword(loginid, newPasswordNew);
            HrmFaceCheckManager.sync(userid, HrmFaceCheckManager.getOptUpdate(), this.getClass().getName(), HrmFaceCheckManager.getOaResource());
            retmap.put("status", "1");
            retmap.put("message", SystemEnv.getHtmlLabelName(26713, user.getLanguage()));
            //密码修改成功调用人员下线接口，将PC、EM均下线
            ServiceUtil serviceUtil = new ServiceUtil();
            ServletContext servletContext = request.getSession(true).getServletContext();
            //下线EM
            serviceUtil.emOffline(id);
            //下线PC
            serviceUtil.offLine4PC(id, servletContext);
            /*如果关闭长时间未登录自动锁定功能，则自动解锁之前由于长时间未登录导致账号被锁定的账号*/
            ChgPasswdReminder reminder = new ChgPasswdReminder();
            RemindSettings settings = reminder.getRemindSettings();
            String forgotpasswordrelieve = settings.getForgotpasswordrelieve();
            if (forgotpasswordrelieve.equals("1")) {
                if(isAdmin){
                    String sql = " UPDATE HrmResourceManager SET passwordlock=0,passwordLockReason='' WHERE passwordlock=1 and id ="+id;
                    rs.executeUpdate(sql);
                }else{
                    String sql = " UPDATE HrmResource SET passwordlock=0,passwordLockReason='' WHERE passwordlock=1 and id ="+id;
                    rs.executeUpdate(sql);
                }
            }
        } catch (Exception e) {
			 e.printStackTrace();
            retmap.put("status", "-1");
            retmap.put("message", SystemEnv.getHtmlLabelName(84566, user.getLanguage()));
            writeLog(e);
        }
        return retmap;
    }

}
