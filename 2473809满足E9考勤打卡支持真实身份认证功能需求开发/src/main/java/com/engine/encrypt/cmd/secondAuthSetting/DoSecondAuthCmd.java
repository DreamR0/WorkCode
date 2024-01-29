package com.engine.encrypt.cmd.secondAuthSetting;

import com.alibaba.fastjson.JSONObject;
import com.api.login.util.QysLoginManager;
import com.cloudstore.dev.api.util.Util_DataMap;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.BizLogContext;
import com.engine.common.entity.EncryptSecondAuthEntity;
import com.engine.common.entity.EncryptSpecialAuthEntity;
import com.engine.common.enums.EncryptMould;
import com.engine.core.interceptor.CommandContext;
import com.engine.encrypt.biz.EncryptFieldConfigComInfo;
import com.engine.encrypt.biz.WfEncryptBiz;
import com.engine.encrypt.util.EncryptUtil;
import com.engine.workflow.constant.SecondAuthType;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.filter.XssUtil;
import weaver.general.PasswordUtil;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.settings.ChgPasswdReminder;
import weaver.hrm.settings.RemindSettings;
import weaver.login.TokenJSCX;
import weaver.login.exception.CaCheckException;
import weaver.rsa.security.RSA;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

public class DoSecondAuthCmd extends AbstractCommonCommand<Map<String, Object>> {

  HttpServletRequest request = null;

  public DoSecondAuthCmd(Map<String, Object> params, HttpServletRequest request, User user) {
    this.params = params;
    this.request = request;
    this.user = user;
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String, Object> retmap = new HashMap<String, Object>();
    try {
      Map<String, Object> secondAuthConfig = new EncryptUtil().getSecondAuthConfig(params, request, user);
      String mouldCode = Util.null2String(secondAuthConfig.get("mouldCode"));
      String itemCode = Util.null2String(secondAuthConfig.get("itemCode"));
      String configId = Util.null2String(secondAuthConfig.get("configId"));
      boolean isNeedSecondAuth = Util.str2bool(Util.null2String(secondAuthConfig.get("isNeedSecondAuth")));
      boolean isNeedDoubleAuth = Util.str2bool(Util.null2String(secondAuthConfig.get("isNeedDoubleAuth")));
      String secondAuthType = Util.null2String(secondAuthConfig.get("secondAuthType"));
      String doubleAuthType = Util.null2String(secondAuthConfig.get("doubleAuthType"));
      String secondAuthToken = Util.null2String(params.get("token"));
      String verifier = Util.null2String(secondAuthConfig.get("verifier"));

      String token = "";
      int authType = -1;
      if(isNeedSecondAuth){
        //需要二次认证
        if(secondAuthToken.length()==0) {
          //进行二次认证
          authType = Util.getIntValue(secondAuthType);
          verifier = "";
        }else if (isNeedDoubleAuth){
          //进行双重认证
          authType = Util.getIntValue(doubleAuthType);
        }
      }else if(isNeedDoubleAuth){
        //进行双重认证
        authType = Util.getIntValue(doubleAuthType);
      }

      User tmpUser = user;
      if (verifier.length() > 0) {
        //设置验证人员id
        tmpUser = new User(Util.getIntValue(verifier));
      }

      String authCode = Util.null2String(params.get("authCode")).trim();
      String isrsaopen = Util.null2String(Prop.getPropValue("openRSA", "isrsaopen"));
      List<String> decriptList = new ArrayList<>();

      if ("1".equals(isrsaopen)) {
        RSA rsa = new RSA();
        decriptList.add(authCode);
        List<String> resultList = rsa.decryptList(request, decriptList);
        authCode = resultList.get(0);
        if (!rsa.getMessage().equals("0")) {
          writeLog("rsa.getMessage()", rsa.getMessage());
        }
        params.put("authCode", authCode);
      }

      if (authType == SecondAuthType.DynamicPassword.getId()) {       //动态密码验证
        retmap = checkDynamicPassword(tmpUser);
      } else if (authType == SecondAuthType.DynamicToken.getId()) {       //动态令牌验证
        retmap = checkDynamicToken(tmpUser);
      } else if (authType == SecondAuthType.SecondAuthPassword.getId()) {     //二次密码验证
        retmap = checkSecondPasword(tmpUser);
      } else if (authType == SecondAuthType.CAAuth.getId()) {     //CA验证
        retmap = checkCAAuth(tmpUser);
      } else if (authType == SecondAuthType.QYS.getId()) {     //契约锁验证
        retmap = checkQYSAuth(tmpUser);
      }
      writeLog("DoSecondAuthCmd checkmap:"+JSONObject.toJSONString(retmap));
      if (Util.null2String(retmap.get("checkStatus")).equals("1")) {
        //认证通过
        token = updateFreeSecretTime(mouldCode,itemCode,configId,authType,tmpUser);
        writeLog("DoSecondAuthCmd token:"+token);
        if(verifier.length()>0){
          token = secondAuthToken+"_"+token;
        }
      }
      retmap.put("token", token);
      retmap.put("status", "1");
      writeLog("DoSecondAuthCmd retmap:"+JSONObject.toJSONString(retmap));
    } catch (Exception e) {
      writeLog(e);
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
    }
    return retmap;
  }

  private Map<String, Object> checkDynamicPassword(User user) {
    Map<String, Object> apiDatas = new HashMap<String, Object>();
    String authCode = Util.null2String(params.get("authCode")).trim();
    RecordSet rs = new RecordSet();
    if (!"".equals(authCode) && user != null) {
      String currentTime = TimeUtil.getCurrentTimeString();
      int userId = user.getUID();
      int lanaguageid = user.getLanguage();
      String sql = "select salt,dyncmiaPassword,validTime from hrm_secondauth_password where userId = ? and userType = ? ";
      rs.executeQuery(sql, userId, 0);
      if (rs.next()) {
        String salt = Util.null2String(rs.getString("salt"));
        String dyncmiaPassword = Util.null2String(rs.getString("dyncmiaPassword"));
        String validTime = Util.null2String(rs.getString("validTime"));

        if (validTime.compareTo(currentTime) < 0) {   //密码过期
          apiDatas.put("checkStatus", "0");
          apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501129, lanaguageid));      //动态密码已过期，请点击重新发送
        } else {
          String[] pwdArr = PasswordUtil.encrypt(authCode, salt);     //将验证码使用相同的盐值加密
          String encryptPwd = Util.null2String(pwdArr[0]);

          if (dyncmiaPassword.equals(encryptPwd)) {     //说明输入的验证码正确
            apiDatas.put("checkStatus", "1");
            apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(22083, lanaguageid));       //验证通过
          } else {
            apiDatas.put("checkStatus", "0");
            apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501133, lanaguageid));      //动态密码不正确
          }
        }
      }
    }

    return apiDatas;
  }

  private Map<String, Object> checkDynamicToken(User user) {
    Map<String, Object> apiDatas = new HashMap<String, Object>();
    String authCode = Util.null2String(params.get("authCode")).trim();

    RecordSet rs = new RecordSet();
    if (!"".equals(authCode) && user != null) {
      int userId = user.getUID();
      int lanaguageid = user.getLanguage();
      String sql = "select tokenkey from hrmresourcemanager where id = ?";
      rs.executeQuery(sql, userId);
      int count = rs.getCounts();

      if (count <= 0) {
        sql = "select tokenkey from hrmresource where id = ?";
        rs.executeQuery(sql, userId);
      }

      String tokenKey = "";
      if (rs.next()) {
        tokenKey = Util.null2String(rs.getString("tokenkey"));
      }

      if ("".equals(tokenKey)) {
        apiDatas.put("checkStatus", "0");
        apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501134, lanaguageid));       //请绑定动态令牌
      } else {
        TokenJSCX token = new TokenJSCX();
        boolean isTokenAuthKeyPass = false;

        sql = "select * from tokenJscx WHERE tokenKey = ?";
        rs.executeQuery(sql, tokenKey);
        if (rs.next()) {
          if (tokenKey.startsWith("1")) {
            isTokenAuthKeyPass = token.checkDLKey(tokenKey, authCode);
          } else if (tokenKey.startsWith("2")) {
            isTokenAuthKeyPass = token.checkDLKey(tokenKey, authCode);
          } else if (tokenKey.startsWith("3")) {
            isTokenAuthKeyPass = token.checkKey(tokenKey, authCode);
          }

          if (!isTokenAuthKeyPass) {
            apiDatas.put("checkStatus", "0");
            apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501135, lanaguageid));     //动态令牌验证不通过
          } else {
            apiDatas.put("checkStatus", "1");
            apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(22083, lanaguageid));          //验证通过
          }
        } else {
          apiDatas.put("checkStatus", "0");
          apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501136, lanaguageid));      //动态令牌未初始化
        }
      }
    }

    return apiDatas;
  }

  private Map<String, Object> checkSecondPasword(User user) {
    Map<String, Object> apiDatas = new HashMap<String, Object>();
    String authCode = Util.null2String(params.get("authCode")).trim();

    RecordSet rs = new RecordSet();
    if (!"".equals(authCode) && user != null) {
      int userId = user.getUID();
      int lanaguageid = user.getLanguage();

      String sql = "select salt,secondarypwd,usesecondarypwd from hrmresourcemanager where id = ?";
      rs.executeQuery(sql, userId);
      int count = rs.getCounts();
      if (count <= 0) {
        sql = "select salt,secondarypwd,usesecondarypwd from hrmresource where id = ?";
        rs.executeQuery(sql, userId);
      }

      if (rs.next()) {
        String salt = Util.null2String(rs.getString("salt"));
        String secondarypwd = Util.null2String(rs.getString("secondarypwd"));
        int usesecondarypwd = Util.getIntValue(rs.getString("usesecondarypwd"), 0);

        if (usesecondarypwd != 1) {
          apiDatas.put("checkStatus", "0");
          apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501316, lanaguageid));       //请设置二次验证密码
          return apiDatas;
        }

        String[] pwdArr = PasswordUtil.encrypt(authCode, salt);
        String encryptPwd = Util.null2String(pwdArr[0]);

        if (encryptPwd.equals(secondarypwd)) {      //密码一致
          apiDatas.put("checkStatus", "1");
          apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(22083, lanaguageid));       //验证通过
        } else {
          apiDatas.put("checkStatus", "0");
          apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(501137, lanaguageid));      //您输入的二次验证密码不正确
        }
      }
    }
    return apiDatas;
  }

  private Map<String, Object> checkCAAuth(User user) {
    Map<String, Object> apiDatas = new HashMap<String, Object>();
    String authCode = Util.null2String(params.get("authCode")).trim();

    RecordSet rs = new RecordSet();
    String status = "";
    if (!"".equals(authCode) && user != null) {
      int lanaguageid = user.getLanguage();
      String dataKey = authCode+"_userid_"+user.getUID() + "_data";
      String statusKey = authCode +"_userid_"+user.getUID() + "_status";
      if (Util_DataMap.containsKey(statusKey)) {
        status = Util.null2String(Util_DataMap.getObjVal(statusKey));      //记录CA验证是否成功
      }

      if ("1".equals(status)) {     //说明验证成功了
        apiDatas.put("checkStatus", "1");
        apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(22083, lanaguageid));       //验证通过
        //那就把缓存里的数据清空掉，节省缓存空间
        try {
          if (Util_DataMap.containsKey(dataKey)) {
            Util_DataMap.clearVal(dataKey);
          }

          if (Util_DataMap.containsKey(statusKey)) {
            Util_DataMap.clearVal(statusKey);
          }
        } catch (IOException e) {
          writeLog(e);
        }
      } else {
        apiDatas.put("checkStatus", "0");
        apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(125979, lanaguageid));   //验证失败
      }
    }

    return apiDatas;
  }

  private Map<String, Object> checkQYSAuth(User user) {
    Map<String, Object> apiDatas = new HashMap<String, Object>();
    String authCode = Util.null2String(params.get("authId")).trim();//契约锁认证id

    String status = "";
    if (!"".equals(authCode) && user != null) {
      int lanaguageid = user.getLanguage();
      Map<String,Object> statusAuth = null;
      try{
        statusAuth = QysLoginManager.statusAuth(request);
        writeLog("checkQYSAuth JSONObject.toJSONString(statusAuth):"+JSONObject.toJSONString(statusAuth));
      }catch (CaCheckException e) {
        writeLog(e);
        e.printStackTrace();
      }
      if(statusAuth!=null) {
        status = Util.null2String(statusAuth.get("code"));
      }
      if ("1".equals(status)) {     //说明验证成功了
        apiDatas.put("checkStatus", "1");
        apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(22083, lanaguageid));       //验证通过
      } else {
        apiDatas.put("checkStatus", "0");
        apiDatas.put("checkMsg", SystemEnv.getHtmlLabelName(125979, lanaguageid));   //验证失败
      }
    }

    return apiDatas;
  }

  //更新免密时间
  private String updateFreeSecretTime(String mouldCode,String itemCode,String configId,int authType,User user) {
    String uuid = "";
    RecordSet rs = new RecordSet();
    if (user != null) {
      String userId = ""+user.getUID();
      if(mouldCode.equals("CONSTRACT")&&itemCode.equals("ONLINE_EDIT")){
        userId = Util.null2String(this.params.get("bizId"));
      }
      String sql = "delete from hrm_secondauth_freesecret where userId = ? and userType = ? and authType = ? ";
      List<Object> sqlParam = new ArrayList<>();
      sqlParam.add(userId);
      sqlParam.add(0);
      sqlParam.add(authType);
      if(Util.null2String(mouldCode).length()>0){
        sql += " and mouldCode= ? ";
        sqlParam.add(mouldCode);
      }
      if(Util.null2String(itemCode).length()>0){
        sql += " and itemCode= ? ";
        sqlParam.add(itemCode);
      }
      if(Util.null2String(configId).length()>0){
        sql += " and configId= ? ";
        sqlParam.add(configId);
      }
      rs.executeUpdate(sql, sqlParam);

      String currentTime = TimeUtil.getCurrentTimeString();

      int freeMins = EncryptUtil.getFreeMins(authType,user);
      if(freeMins<1){
        //未设置免密，允许1分钟
        freeMins = 1;
      }
      if (freeMins > 0) {
        String freeSecretTime = TimeUtil.timeAdd(currentTime, freeMins * 60);
        //保存免密时间
        uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        sql = "insert into hrm_secondauth_freesecret (userId, userType, authType,mouldCode,itemCode,configId,token,freeSecretTime) values (?,?,?,?,?,?,?,?)";
        sqlParam = new ArrayList<>();
        sqlParam.add(userId);
        sqlParam.add(0);
        sqlParam.add(authType);
        sqlParam.add(mouldCode);
        sqlParam.add(itemCode);
        sqlParam.add(configId);
        sqlParam.add(uuid);
        sqlParam.add(freeSecretTime);
        rs.executeUpdate(sql, sqlParam);
      }
    }
    return uuid;
  }

  @Override
  public BizLogContext getLogContext() {
    return null;
  }

}
