package com.engine.encrypt.cmd.secondAuthSetting;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionItem;
import com.api.browser.util.ConditionType;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.api.login.bean.QysLoginManagerBean;
import com.api.login.util.QysLoginManager;
import com.cloudstore.dev.api.util.EMManager;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.encrypt.util.EncryptUtil;
import com.engine.workflow.constant.SecondAuthType;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.resource.ResourceComInfo;
import weaver.hrm.settings.HrmSettingsComInfo;
import weaver.login.exception.CaCheckException;
import weaver.sm.SM4Utils;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class GetSecondAuthFormCmd extends AbstractCommonCommand<Map<String, Object>> {

  HttpServletRequest request = null;

  public GetSecondAuthFormCmd(Map<String, Object> params, HttpServletRequest request, User user) {
    this.params = params;
    this.user = user;
    this.request = request;
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String, Object> retmap = new HashMap<String, Object>();
    List<Object> itemlist = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      List<HrmFieldBean> lsField = new ArrayList<HrmFieldBean>();
      HrmFieldBean hrmFieldBean = null;

      HrmSettingsComInfo hrmSettingsComInfo = new HrmSettingsComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      String secondPassword = hrmSettingsComInfo.getSecondPassword();//密码允许作为二次身份校验
      String secondNeedDynapass = hrmSettingsComInfo.getSecondNeedDynapass();//动态密码允许作为二次身份校验
      String secondNeedusbDt = hrmSettingsComInfo.getSecondNeedusbDt();//动态令牌允许作为二次身份校验
      String secondCA = hrmSettingsComInfo.getSecondCA();//CA允许作为二次身份校验
      String secondCL = hrmSettingsComInfo.getSecondCL();//契约锁允许作为二次身份校验

      String userId = "" + user.getUID();
      boolean notSetting = true;
      new BaseBean().writeLog("==zj==(二次身份打卡params)" + JSON.toJSONString(params));
      Map<String, Object> secondAuthConfig = new EncryptUtil().getSecondAuthConfig(params, request, user);
      new BaseBean().writeLog("==zj==(二次身份打卡secondAuthConfig)" + JSON.toJSONString(secondAuthConfig));
      boolean isNeedSecondAuth = Util.str2bool(Util.null2String(secondAuthConfig.get("isNeedSecondAuth")));
      boolean isNeedDoubleAuth = Util.str2bool(Util.null2String(secondAuthConfig.get("isNeedDoubleAuth")));
      String secondAuthType = Util.null2String(secondAuthConfig.get("secondAuthType"));
      String doubleAuthType = Util.null2String(secondAuthConfig.get("doubleAuthType"));
      String secondAuthToken = Util.null2String(secondAuthConfig.get("secondAuthToken"));
      String verifier = Util.null2String(secondAuthConfig.get("verifier"));
      int authType = -1;
      if(isNeedSecondAuth){
        //需要二次认证
        authType = Util.getIntValue(secondAuthType);
        if(secondAuthToken.length()==0) {
          verifier = "";
        }else if (isNeedDoubleAuth){
          //进行双重认证
          authType = Util.getIntValue(doubleAuthType);
        }
      }else if(isNeedDoubleAuth){
        //进行双重认证
        authType = Util.getIntValue(doubleAuthType);
      }
      if (verifier.length() > 0) {
        //设置验证人员id
        userId = verifier;
        retmap.put("userid", verifier);
        retmap.put("username", resourceComInfo.getLastname(verifier));
      }

      if (authType == -1) {
        //参数异常情况
        retmap.put("status", "-1");
        retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
        return retmap;
      }

      if (secondPassword.equals("1") && authType == SecondAuthType.SecondAuthPassword.getId()) {
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("authCode");
        hrmFieldBean.setFieldlabel("388412");
        hrmFieldBean.setFieldhtmltype("1");
        hrmFieldBean.setType("1");
        hrmFieldBean.setViewAttr(3);
        hrmFieldBean.setRules("required|string");
        lsField.add(hrmFieldBean);
        sql = "select salt,secondarypwd,usesecondarypwd from hrmresourcemanager where id = ?";
        rs.executeQuery(sql, userId);
        int count = rs.getCounts();
        if (count <= 0) {
          sql = "select salt,secondarypwd,usesecondarypwd from hrmresource where id = ?";
          rs.executeQuery(sql, userId);
        }
        if (rs.next()) {
          if (Util.null2String(rs.getString("usesecondarypwd")).equals("1")) {
            notSetting = false;
          }
        }
      } else if (secondNeedDynapass.equals("1") && authType == SecondAuthType.DynamicPassword.getId()) {
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("authCode");
        hrmFieldBean.setFieldlabel("32511");
        hrmFieldBean.setFieldhtmltype("1");
        hrmFieldBean.setType("1");
        hrmFieldBean.setViewAttr(3);
        hrmFieldBean.setRules("required|string");
        lsField.add(hrmFieldBean);
        authType = SecondAuthType.DynamicPassword.getId();
      } else if (secondNeedusbDt.equals("1") && authType == SecondAuthType.DynamicToken.getId()) {
        hrmFieldBean = new HrmFieldBean();
        hrmFieldBean.setFieldname("authCode");
        hrmFieldBean.setFieldlabel("32896");
        hrmFieldBean.setFieldhtmltype("1");
        hrmFieldBean.setType("1");
        hrmFieldBean.setViewAttr(3);
        hrmFieldBean.setRules("required|string");
        lsField.add(hrmFieldBean);
        authType = SecondAuthType.DynamicToken.getId();
        sql = "select tokenkey from hrmresourcemanager where id = ?";
        rs.executeQuery(sql, userId);
        int count = rs.getCounts();

        if (count <= 0) {
          sql = "select tokenkey from hrmresource where id = ?";
          rs.executeQuery(sql, userId);
        }
        if (rs.next()) {
          notSetting = false;
        }
      } else if (secondCA.equals("1") && authType == SecondAuthType.CAAuth.getId()) {
        String uid = UUID.randomUUID().toString();
        String random = Integer.toString(uid.hashCode());
        String em_sys_id = "";
        String url = "/spa/hrm/static4mobile/index.html#/qrCACheck";
        try {
          EMManager manager = new EMManager();
          Map<String, String> data = manager.getEMData();
          em_sys_id = data.get(EMManager.ec_id);
        } catch (Exception e) {//未部署EM7

        }
        Map<String, String> params = new HashMap<String, String>();
        retmap.put("qrcode", String.format("openlink:{\"sysId\":\"%s\",\"url\":\"%s?uid=%s&randomNumber=%s\"}", em_sys_id, url, uid, random));
        retmap.put("loginkey", uid);
        authType = SecondAuthType.CAAuth.getId();
      } else if (secondCL.equals("1") && authType == SecondAuthType.QYS.getId()) {
        try {
          String mouldCode = Util.null2String(params.get("mouldCode"));
          String itemCode = Util.null2String(params.get("itemCode"));

          String userName = "";
          if(mouldCode.equals("CONSTRACT")&&itemCode.equals("ONLINE_EDIT")){
            userId = Util.null2String(params.get("bizId"));
            userName = Util.null2String(params.get("userName"));
            writeLog("接收>>>>>>>>>>>>>>>userId:"+userId+"userName:"+userName);
            SM4Utils sm4 = new SM4Utils();
            String key = Util.null2String(new BaseBean().getPropValue("weaver_client_pwd","key"));
            userId = sm4.decrypt(userId,key);
            userName = sm4.decrypt(userName,key);
            writeLog("解密后userId:"+userId+"userName:"+userName);
          }
          QysLoginManagerBean qysLoginManagerBean = new QysLoginManagerBean();
          qysLoginManagerBean.setUserid(userId);
          qysLoginManagerBean.setUsername(userName);
          qysLoginManagerBean.setLangid(""+user.getLanguage());
          qysLoginManagerBean.setSecondAuth(true);
          String[] authInfo = QysLoginManager.initAuth(qysLoginManagerBean);
          retmap.put("authId",authInfo[0]) ;
          retmap.put("authUrl",authInfo[1]) ;
          retmap.put("qysAuthType",authInfo[2]) ;
        }catch (CaCheckException e){
          retmap.put("qysflag","-100") ;
          retmap.put("qysmg",e.getDetail()) ;
        }
        authType = SecondAuthType.QYS.getId();
      }

      HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
      SearchConditionItem searchConditionItem = null;
      itemlist = new ArrayList<Object>();
      for (int i = 0; i < lsField.size(); i++) {
        hrmFieldBean = lsField.get(i);
        searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
        if (hrmFieldBean.getFieldlabel().equals("388412")) {
          searchConditionItem.setConditionType(ConditionType.PASSWORD);
          Map otherParam = new HashMap<>();
          otherParam.put("autocomplete", "off");
          searchConditionItem.setOtherParams(otherParam);
        }
        itemlist.add(searchConditionItem);
      }

      Map<String,String> labelInfo = new HashMap<>();
      labelInfo.put("qysTitle",	SystemEnv.getHtmlLabelName(534749, user.getLanguage()));
      labelInfo.put("qysTitle1",SystemEnv.getHtmlLabelName(534571, user.getLanguage()));
      retmap.put("notSetting", notSetting);
      retmap.put("status", "1");
      retmap.put("conditions", itemlist);
      retmap.put("authType", authType);
      retmap.put("token", secondAuthToken);
      retmap.put("labelInfo", labelInfo);
    } catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
      writeLog(e);
    }
    return retmap;
  }

  @Override
  public BizLogContext getLogContext() {
    return null;
  }

}
